package com.ohinteractive.seedv6.training.selfplay;

import java.util.Objects;
import java.util.SplittableRandom;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.search.alphabeta.RootParallelSearch;
import com.ohinteractive.seedv6.search.common.SearchControl;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.iterative.IterativeDeepeningSearch;

/** Synchronous NNUE-vs-NNUE game. Fresh private TT/order state per game, reused only within that game. */
public final class SelfPlayRunner {
    /** Independent indexed seeds: no preceding game's length consumes another game's RNG stream. */
    public static long gameSeed(long masterSeed, int gameIndex) {
        if (gameIndex < 0) throw new IllegalArgumentException("Negative game index.");
        return new SplittableRandom(masterSeed + 0x9E3779B97F4A7C15L * gameIndex).nextLong();
    }

    public static GameTrajectory play(NnueNetwork network, SelfPlayConfig config, int gameIndex,
                                      long[] initialBoard, SelfPlayControl control) {
        Objects.requireNonNull(network, "network");
        Objects.requireNonNull(control, "control");
        HeadlessGame game = new HeadlessGame(initialBoard, config.maximumPlies());
        if (!game.active()) return game.trajectory();
        if (control.cancelled()) {
            game.abort(GameTermination.CANCELLED, null);
            return game.trajectory();
        }
        try (IterativeDeepeningSearch search = new IterativeDeepeningSearch(
                new RootParallelSearch(config.threads(), SearchEvaluation.incremental(network, config.scoreMapping())))) {
            return drive(game, config, gameIndex, control, request -> {
                var outcome = search.search(request);
                SearchResult result = outcome.lastCompletedResult();
                if (!outcome.targetDepthCompleted() || result == null || !result.completed() || !result.hasMove()) {
                    throw new IllegalStateException("Search did not complete the requested depth: "
                            + request.control().termination());
                }
                return result.bestMove();
            });
        } catch (RuntimeException failure) {
            return infrastructureFailure(game, failure);
        }
    }

    // A search-close failure must invalidate even an otherwise completed game's evidence.
    static GameTrajectory infrastructureFailure(HeadlessGame game, RuntimeException failure) {
        game.abort(GameTermination.INFRASTRUCTURE_FAILURE, failure.toString());
        GameTrajectory trajectory = game.trajectory();
        return new GameTrajectory(trajectory.positions(), trajectory.finalBoard(), trajectory.history(),
                GameTermination.INFRASTRUCTURE_FAILURE, failure.toString());
    }

    @FunctionalInterface
    interface MoveSelector { long select(SearchRequest request); }

    // Same loop used by real NNUE search; package seam avoids expensive searches for rule/failure tests.
    static GameTrajectory drive(HeadlessGame game, SelfPlayConfig config, int gameIndex,
                                SelfPlayControl control, MoveSelector selector) {
        SplittableRandom random = new SplittableRandom(gameSeed(config.seed(), gameIndex));
        int openingPlies = (int) random.nextLong(config.minimumOpeningPlies(), (long) config.maximumOpeningPlies() + 1);
        while (game.active()) {
            if (control.cancelled()) { game.abort(GameTermination.CANCELLED, null); break; }
            long move;
            if (game.playedPlies() < openingPlies) {
                long[] legal = game.legalMoves();
                move = legal[random.nextInt(legal.length)];
            } else {
                SearchControl searchControl = control.beginSearch(config);
                try {
                    move = selector.select(new SearchRequest(game.boardSnapshot(), game.historySnapshot(),
                            config.depth(), searchControl));
                    if (!searchControl.checkpoint()) {
                        game.abort(control.cancelled() ? GameTermination.CANCELLED : GameTermination.SEARCH_FAILURE,
                                searchControl.termination().toString());
                        break;
                    }
                } catch (RuntimeException failure) {
                    game.abort(control.cancelled() ? GameTermination.CANCELLED : GameTermination.SEARCH_FAILURE,
                            failure.toString());
                    break;
                } finally {
                    control.endSearch();
                }
            }
            if (control.cancelled()) { game.abort(GameTermination.CANCELLED, null); break; }
            try {
                game.play(move);
            } catch (IllegalArgumentException failure) {
                game.abort(GameTermination.SEARCH_FAILURE, failure.toString());
            }
        }
        return game.trajectory();
    }

    private SelfPlayRunner() {}
}
