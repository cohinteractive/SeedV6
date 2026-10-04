package com.ohinteractive.seedv6.training.validation;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.SplittableRandom;
import java.util.function.Consumer;
import java.util.function.BiFunction;
import com.ohinteractive.seedv6.training.model.NetworkModel;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.IterationSnapshot;
import com.ohinteractive.seedv6.search.common.TimeSource;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.driver.SearchDriver;
import com.ohinteractive.seedv6.search.driver.ProductionSearch;
import com.ohinteractive.seedv6.training.selfplay.GameTermination;
import com.ohinteractive.seedv6.training.selfplay.HeadlessGame;
import com.ohinteractive.seedv6.training.selfplay.SelfPlayRunner;

/** Bounded, sequential matches. No trainer or handcrafted positional evaluator is reachable here. */
public final class ValidationArena {
    public record Opening(long[] board, GameHistory history, int randomizedPlies) {
        public Opening {
            board = board.clone();
            history.requireCurrent(board);
            history = history.snapshot();
        }
        @Override public long[] board() { return board.clone(); }
        public String identity() { return stateHash(board, history); }
        public HeadlessGame newGame(int cap) { return new HeadlessGame(board, history, cap); }
    }

    /** Uniform authoritative legal moves, indexed RNG streams independent of both networks and game lengths. */
    public static Opening opening(long[] board, GameHistory history, ValidationConfig config, int index) {
        HeadlessGame game = new HeadlessGame(board, history, Integer.MAX_VALUE);
        SplittableRandom random = new SplittableRandom(SelfPlayRunner.gameSeed(config.seed(), index));
        int plies = (int) random.nextLong(config.minimumOpeningPlies(), (long) config.maximumOpeningPlies() + 1);
        while (game.active() && game.playedPlies() < plies) {
            long[] moves = game.legalMoves();
            game.play(moves[random.nextInt(moves.length)]);
        }
        return new Opening(game.boardSnapshot(), game.historySnapshot(), game.playedPlies());
    }

    public ValidationResult validate(NnueNetwork candidate, NnueNetwork incumbent, ValidationConfig config,
                                     ValidationControl control) {
        long[] board = Board.startingPosition();
        return validate(candidate, incumbent, config, board, GameHistory.initial(board), control);
    }

    public ValidationResult validate(NnueNetwork candidate, NnueNetwork incumbent, ValidationConfig config,
                                     long[] board, GameHistory history, ValidationControl control) {
        return validate(candidate, incumbent, config, board, history, control, ValidationArena::search);
    }

    /** Synchronous, move-boundary telemetry. Observer failures are contained; consumers must not block. */
    public ValidationResult validate(NnueNetwork candidate, NnueNetwork incumbent, ValidationConfig config,
                                     long[] board, GameHistory history, ValidationControl control,
                                     Consumer<ValidationProgress> observer) {
        return validate(candidate, incumbent, config, board, history, control, ValidationArena::search,
                Objects.requireNonNull(observer), TimeSource.SYSTEM);
    }

    public ValidationResult validate(NetworkModel candidate, NetworkModel incumbent, ValidationConfig config,
                                     long[] board, GameHistory history, ValidationControl control,
                                     Consumer<ValidationProgress> observer) {
        if (candidate.architecture() != incumbent.architecture()) throw new IllegalArgumentException("Architecture mismatch.");
        return validateModels(candidate, incumbent, config, board, history, control,
                (model, c) -> search(model.evaluation(c.scoreMapping()), c), Objects.requireNonNull(observer), TimeSource.SYSTEM);
    }

    // Test seam is deliberately package-private; the public lifecycle always uses actual network searches.
    interface Player extends AutoCloseable {
        long move(SearchRequest request);
        default com.ohinteractive.seedv6.search.common.SearchResult lastResult() { return null; }
        default ValidationProgress.MoveSearch lastSearch() { return null; }
        @Override default void close() {}
    }
    @FunctionalInterface interface PlayerFactory { Player create(NnueNetwork network, ValidationConfig config); }

    ValidationResult validate(NnueNetwork candidate, NnueNetwork incumbent, ValidationConfig config,
                              long[] board, GameHistory history, ValidationControl control, PlayerFactory factory) {
        return validate(candidate, incumbent, config, board, history, control, factory, null, TimeSource.SYSTEM);
    }

    ValidationResult validate(NnueNetwork candidate, NnueNetwork incumbent, ValidationConfig config,
                              long[] board, GameHistory history, ValidationControl control, PlayerFactory factory,
                              Consumer<ValidationProgress> observer, TimeSource clock) {
        if (candidate.schemaVersion() != incumbent.schemaVersion()) throw new IllegalArgumentException("Schema mismatch.");
        return validateModels(candidate, incumbent, config, board, history, control, factory::create, observer, clock);
    }

    private <T> ValidationResult validateModels(T candidate, T incumbent, ValidationConfig config,
                              long[] board, GameHistory history, ValidationControl control,
                              BiFunction<T, ValidationConfig, Player> factory, Consumer<ValidationProgress> observer, TimeSource clock) {
        return validateModels(candidate, incumbent, config, board, history, control, factory, observer, clock, -1, null);
    }

    /** Architecture-neutral paired match, with no acceptance/promotion decision. Each game is committed
     * synchronously before the next starts; persistence failures propagate to the campaign owner.
     */
    public ValidationResult match(NetworkModel a, NetworkModel b, ValidationConfig config,
            long[] board, GameHistory history, ValidationControl control, long millis,
            Consumer<ValidationProgress> observer, Consumer<List<ValidationResult.Pair>> commit) {
        if (millis != -1 && millis < 1) throw new IllegalArgumentException("Invalid move time");
        return validateModels(a, b, config, board, history, control,
                (model, c) -> search(model.evaluation(c.scoreMapping()), c, millis >= 0),
                observer, TimeSource.SYSTEM, millis, Objects.requireNonNull(commit));
    }

    private <T> ValidationResult validateModels(T candidate, T incumbent, ValidationConfig config,
                              long[] board, GameHistory history, ValidationControl control,
                              BiFunction<T, ValidationConfig, Player> factory, Consumer<ValidationProgress> observer,
                              TimeSource clock, long millis, Consumer<List<ValidationResult.Pair>> commit) {
        Objects.requireNonNull(candidate);
        Objects.requireNonNull(incumbent);
        Objects.requireNonNull(control);
        long[] root = board.clone();
        history.requireCurrent(root);
        List<ValidationResult.Pair> pairs = new ArrayList<>();
        var savedPairs = control.takeSavedPairs();
        if (savedPairs.size() > config.openingPairs()) throw new IllegalArgumentException("Too many saved validation pairs.");
        var progress = new ValidationProgressTracker(config.openingPairs(), observer, clock);
        for (int i = 0; i < config.openingPairs(); i++) {
            progress.startPair(i + 1);
            var prior = i < savedPairs.size() ? savedPairs.get(i) : null;
            if (control.cancelled()) {
                var cancelled = new ValidationResult.Game(GameTermination.CANCELLED, 0);
                var pair = prior == null ? new ValidationResult.Pair("", cancelled, cancelled) : prior;
                pairs.add(pair);
                progress.endPair(pair, true);
                continue;
            }
            Opening opening = opening(root, history, config, i);
            if (prior != null && !prior.openingHash().isEmpty() && !prior.openingHash().equals(opening.identity()))
                throw new IllegalArgumentException("Incompatible validation continuation.");
            progress.startGame(1);
            var a = prior != null && prior.candidateWhite().termination() != GameTermination.CANCELLED
                    ? prior.candidateWhite() : play(opening, candidate, incumbent, config, control, factory, progress, i * 2 + 1, 1, millis);
            progress.endGame(a);
            if (commit != null) commitGames(commit, savedPairs, pairs, new ValidationResult.Pair(opening.identity(), a,
                    prior == null ? new ValidationResult.Game(GameTermination.CANCELLED, 0) : prior.candidateBlack()));
            progress.startGame(2);
            var b = prior != null && prior.candidateBlack().termination() != GameTermination.CANCELLED
                    ? prior.candidateBlack() : play(opening, incumbent, candidate, config, control, factory, progress, i * 2 + 2, 2, millis);
            progress.endGame(b);
            var pair = new ValidationResult.Pair(opening.identity(), a, b);
            if (commit != null) commitGames(commit, savedPairs, pairs, pair);
            pairs.add(pair);
            progress.endPair(pair, false);
        }
        progress.finish();
        control.recordPairs(pairs);
        return new ValidationResult(config, stateHash(root, history), pairs);
    }

    private static Player search(NnueNetwork network, ValidationConfig config) {
        return search(SearchEvaluation.incremental(network, config.scoreMapping()), config);
    }

    static Player search(SearchEvaluation evaluation, ValidationConfig config) {
        return search(evaluation, config, false);
    }
    private static Player search(SearchEvaluation evaluation, ValidationConfig config, boolean timed) {
        // Each colour owns its driver, TTable, board stack and evaluator state.
        var search = new SearchDriver(ProductionSearch.create(config.threads(), evaluation));
        return new Player() {
            private ValidationProgress.MoveSearch lastSearch;
            private com.ohinteractive.seedv6.search.common.SearchResult completed;
            public long move(SearchRequest request) {
                var outcome = search.search(request);
                var result = outcome.lastCompletedResult();
                boolean usable = outcome.targetDepthCompleted() || timed
                        && request.control().termination() == com.ohinteractive.seedv6.search.common.SearchTermination.TIME_LIMIT;
                if (!usable || result == null || !result.completed() || !result.hasMove()) {
                    throw new IllegalStateException(timed ? "Network search did not complete a usable iteration within its limit."
                            : "Network search did not complete requested depth.");
                }
                completed = result;
                lastSearch = ValidationProgress.MoveSearch.from(IterationSnapshot.from(result, request.control().elapsedNanos()));
                return result.bestMove();
            }
            public com.ohinteractive.seedv6.search.common.SearchResult lastResult() { return completed; }
            public ValidationProgress.MoveSearch lastSearch() { return lastSearch; }
            public void close() { search.close(); }
        };
    }

    private static <T> ValidationResult.Game play(Opening opening, T white, T black,
                                               ValidationConfig config, ValidationControl control, BiFunction<T, ValidationConfig, Player> factory,
                                               ValidationProgressTracker progress, int ordinal, int gameInPair, long millis) {
        HeadlessGame game = opening.newGame(config.maximumPlies());
        if (control.cancelled()) return new ValidationResult.Game(GameTermination.CANCELLED, 0);
        if (!game.active()) return summary(game);
        var presentation = control.presentation();
        if (presentation != null) presentation.start(game, ordinal, gameInPair);
        try (Player whitePlayer = factory.apply(white, config); Player blackPlayer = factory.apply(black, config)) {
            while (game.active()) {
                if (control.cancelled()) { game.abort(GameTermination.CANCELLED, null); break; }
                var searchControl = control.beginSearch(millis);
                try {
                    Player player = game.sideToMove() == Value.WHITE ? whitePlayer : blackPlayer;
                    long move = player.move(new SearchRequest(game.boardSnapshot(), game.historySnapshot(),
                            config.depth(), searchControl));
                    if (control.cancelled()) game.abort(GameTermination.CANCELLED, null);
                    else if (millis < 0 && !searchControl.checkpoint()) game.abort(GameTermination.SEARCH_FAILURE, "Search stopped.");
                    else {
                        game.play(move);
                        if (presentation != null) presentation.moved(game, move, player.lastResult());
                        progress.moved(game.playedPlies(), player.lastSearch());
                    }
                } catch (RuntimeException failure) {
                    game.abort(control.cancelled() ? GameTermination.CANCELLED : GameTermination.SEARCH_FAILURE,
                            failure.toString());
                } finally { control.endSearch(); }
            }
        } catch (RuntimeException failure) {
            // A failure even while closing an otherwise completed game is not usable evidence.
            return new ValidationResult.Game(GameTermination.INFRASTRUCTURE_FAILURE, game.playedPlies());
        } finally { if (presentation != null) presentation.clear(); }
        return summary(game);
    }

    private static ValidationResult.Game summary(HeadlessGame game) {
        return new ValidationResult.Game(game.termination(), game.playedPlies());
    }

    private static void commitGames(Consumer<List<ValidationResult.Pair>> commit, List<ValidationResult.Pair> saved,
                                    List<ValidationResult.Pair> completed, ValidationResult.Pair current) {
        var value = new ArrayList<>(completed); value.add(current);
        if (saved.size() > value.size()) value.addAll(saved.subList(value.size(), saved.size()));
        commit.accept(List.copyOf(value));
    }

    /** Includes all board bits (rights, EP, clocks) and the complete ordered repetition history. */
    public static String stateHash(long[] board, GameHistory history) {
        history.requireCurrent(board);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            ByteBuffer value = ByteBuffer.allocate(Long.BYTES);
            for (long part : board) { value.clear(); value.putLong(part); digest.update(value.array()); }
            value.clear(); value.putLong(history.size()); digest.update(value.array());
            for (int i = 0; i < history.size(); i++) {
                value.clear(); value.putLong(history.keyAt(i)); digest.update(value.array());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
}
