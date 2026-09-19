package com.ohinteractive.seedv6.tools.nnue.research;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.alphabeta.RootParallelSearch;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.iterative.IterativeDeepeningSearch;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.validation.*;

/** Fixed immutable actors, private per-colour TTs, W/D/L only; no checkpoint writer or promotion path. */
public final class StrengthArena {
    public enum Mode { EVALUATION_ISOLATION, PRACTICAL_ENGINE }
    public enum Conclusion { CLEAR_A_ADVANTAGE, CLEAR_B_ADVANTAGE, NO_CLEAR_ADVANTAGE, INSUFFICIENT_EVIDENCE }
    public record Actor(String id, NnueNetwork network) {
        public Actor { if (id == null || id.isBlank()) throw new IllegalArgumentException("Missing actor identity."); }
        public static Actor handcrafted() { return new Actor("handcrafted-seedv6", null); }
        public static Actor checkpoint(Path directory) throws IOException {
            var checkpoint = CheckpointStore.inspect(directory);
            return new Actor(checkpoint.manifest().id(), checkpoint.network());
        }
        public SearchEvaluation evaluation(Mode mode, ValidationConfig config) {
            Objects.requireNonNull(mode);
            return network != null ? SearchEvaluation.incremental(network, config.scoreMapping())
                    : mode == Mode.EVALUATION_ISOLATION ? SearchEvaluation.handcraftedIsolation() : SearchEvaluation.handcrafted();
        }
    }
    public record Config(ValidationConfig resources, Mode mode, int minimumPairs, double alpha) {
        public Config {
            Objects.requireNonNull(resources); Objects.requireNonNull(mode);
            if (minimumPairs < 1 || !Double.isFinite(alpha) || alpha <= 0 || alpha >= 1) {
                throw new IllegalArgumentException("Invalid evidence settings.");
            }
        }
    }
    public record Evidence(double mean, double percentage, double radius, double lower, double upper,
                           OptionalDouble eloEquivalent, Conclusion conclusion) {}
    public record Result(String actorA, String actorB, Config config, ValidationResult games) {
        public Evidence evidence() { return assess(games.statistics(), config.minimumPairs(), config.alpha()); }
    }
    public static Evidence assess(ValidationResult.Statistics s, int minimumPairs, double alpha) {
        if (minimumPairs < 1 || !Double.isFinite(alpha) || alpha <= 0 || alpha >= 1) throw new IllegalArgumentException();
        int n = s.validPairs();
        double mean = n == 0 ? Double.NaN : s.pairScoreSum() / n;
        double radius = n == 0 ? Double.POSITIVE_INFINITY : Math.sqrt((Math.log(2) - Math.log(alpha)) / (2 * n));
        double lower = n == 0 ? 0 : Math.max(0, mean - radius), upper = n == 0 ? 1 : Math.min(1, mean + radius);
        Conclusion decision = n < minimumPairs || s.incompletePairs() > 0 ? Conclusion.INSUFFICIENT_EVIDENCE
                : lower > 0.5 ? Conclusion.CLEAR_A_ADVANTAGE
                : upper < 0.5 ? Conclusion.CLEAR_B_ADVANTAGE : Conclusion.NO_CLEAR_ADVANTAGE;
        return new Evidence(mean, 100 * mean, radius, lower, upper,
                mean > 0 && mean < 1 ? OptionalDouble.of(400 * Math.log10(mean / (1 - mean))) : OptionalDouble.empty(), decision);
    }

    public Result match(Actor a, Actor b, Config config) {
        return match(a, b, config, new ValidationControl(), pair -> {});
    }
    /** Optional external cancellation; interrupted and unstarted pairs remain incomplete, never draws. */
    public Result match(Actor a, Actor b, Config config, ValidationControl control, Consumer<ValidationResult.Pair> observer) {
        long[] board = Board.startingPosition();
        return match(a, b, config, board, GameHistory.initial(board), control, StrengthArena::search, observer);
    }
    interface Player extends AutoCloseable {
        long move(SearchRequest request);
        @Override default void close() {}
    }
    @FunctionalInterface interface Factory { Player create(Actor actor, Config config); }

    Result match(Actor a, Actor b, Config config, long[] board, GameHistory history, ValidationControl control, Factory factory) {
        return match(a, b, config, board, history, control, factory, pair -> {});
    }
    private Result match(Actor a, Actor b, Config config, long[] board, GameHistory history, ValidationControl control,
                         Factory factory, Consumer<ValidationResult.Pair> observer) {
        Objects.requireNonNull(a); Objects.requireNonNull(b);
        List<ValidationResult.Pair> pairs = new ArrayList<>();
        var resources = config.resources();
        for (int i = 0; i < resources.openingPairs(); i++) {
            var opening = ValidationArena.opening(board, history, resources, i);
            var white = play(opening, a, b, config, control, factory);
            var black = play(opening, b, a, config, control, factory);
            var pair = new ValidationResult.Pair(opening.identity(), white, black);
            pairs.add(pair); observer.accept(pair);
        }
        return new Result(a.id(), b.id(), config,
                new ValidationResult(resources, ValidationArena.stateHash(board, history), pairs));
    }
    static Player search(Actor actor, Config config) {
        var search = new IterativeDeepeningSearch(new RootParallelSearch(config.resources().threads(),
                actor.evaluation(config.mode(), config.resources())));
        return new Player() {
            public long move(SearchRequest request) {
                var outcome = search.search(request);
                var result = outcome.lastCompletedResult();
                if (!outcome.targetDepthCompleted() || result == null || !result.completed() || !result.hasMove()) {
                    throw new IllegalStateException("Fixed-depth search incomplete.");
                }
                return result.bestMove();
            }
            public void close() { search.close(); }
        };
    }
    private static ValidationResult.Game play(ValidationArena.Opening opening, Actor white, Actor black, Config config,
                                               ValidationControl control, Factory factory) {
        HeadlessGame game = opening.newGame(config.resources().maximumPlies());
        if (control.cancelled()) return new ValidationResult.Game(GameTermination.CANCELLED, 0);
        if (!game.active()) return summary(game);
        try (Player w = factory.create(white, config); Player b = factory.create(black, config)) {
            while (game.active()) {
                if (control.cancelled()) { game.abort(GameTermination.CANCELLED, null); break; }
                var searchControl = control.beginSearch();
                try {
                    long move = (game.sideToMove() == Value.WHITE ? w : b).move(new SearchRequest(
                            game.boardSnapshot(), game.historySnapshot(), config.resources().depth(), searchControl));
                    if (control.cancelled()) game.abort(GameTermination.CANCELLED, null);
                    else if (!searchControl.checkpoint()) game.abort(GameTermination.SEARCH_FAILURE, "Search stopped.");
                    else game.play(move);
                } catch (RuntimeException failure) {
                    game.abort(control.cancelled() ? GameTermination.CANCELLED : GameTermination.SEARCH_FAILURE, failure.toString());
                } finally { control.endSearch(); }
            }
        } catch (RuntimeException failure) {
            return new ValidationResult.Game(GameTermination.INFRASTRUCTURE_FAILURE, game.playedPlies());
        }
        return summary(game);
    }
    private static ValidationResult.Game summary(HeadlessGame game) {
        return new ValidationResult.Game(game.termination(), game.playedPlies());
    }
}
