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
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.alphabeta.RootParallelSearch;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.IterationSnapshot;
import com.ohinteractive.seedv6.search.common.TimeSource;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.iterative.IterativeDeepeningSearch;
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

    // Test seam is deliberately package-private; the public lifecycle always uses actual NNUE searches.
    interface Player extends AutoCloseable {
        long move(SearchRequest request);
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
        Objects.requireNonNull(candidate);
        Objects.requireNonNull(incumbent);
        Objects.requireNonNull(control);
        if (candidate.schemaVersion() != incumbent.schemaVersion()) throw new IllegalArgumentException("Schema mismatch.");
        long[] root = board.clone();
        history.requireCurrent(root);
        List<ValidationResult.Pair> pairs = new ArrayList<>();
        var progress = new ValidationProgressTracker(config.openingPairs(), observer, clock);
        for (int i = 0; i < config.openingPairs(); i++) {
            progress.startPair(i + 1);
            if (control.cancelled()) {
                var cancelled = new ValidationResult.Game(GameTermination.CANCELLED, 0);
                var pair = new ValidationResult.Pair("", cancelled, cancelled);
                pairs.add(pair);
                progress.endPair(pair, true);
                continue;
            }
            Opening opening = opening(root, history, config, i);
            progress.startGame(1);
            var a = play(opening, candidate, incumbent, config, control, factory, progress);
            progress.endGame(a);
            progress.startGame(2);
            var b = play(opening, incumbent, candidate, config, control, factory, progress);
            progress.endGame(b);
            var pair = new ValidationResult.Pair(opening.identity(), a, b);
            pairs.add(pair);
            progress.endPair(pair, false);
        }
        progress.finish();
        return new ValidationResult(config, stateHash(root, history), pairs);
    }

    private static Player search(NnueNetwork network, ValidationConfig config) {
        // Each colour gets a fresh TT. Workers within that one network share only its private TT.
        var search = new IterativeDeepeningSearch(new RootParallelSearch(config.threads(),
                SearchEvaluation.incremental(network, config.scoreMapping())));
        return new Player() {
            private ValidationProgress.MoveSearch lastSearch;
            public long move(SearchRequest request) {
                var outcome = search.search(request);
                var result = outcome.lastCompletedResult();
                if (!outcome.targetDepthCompleted() || result == null || !result.completed() || !result.hasMove()) {
                    throw new IllegalStateException("NNUE search did not complete requested depth.");
                }
                lastSearch = ValidationProgress.MoveSearch.from(IterationSnapshot.from(result, request.control().elapsedNanos()));
                return result.bestMove();
            }
            public ValidationProgress.MoveSearch lastSearch() { return lastSearch; }
            public void close() { search.close(); }
        };
    }

    private static ValidationResult.Game play(Opening opening, NnueNetwork white, NnueNetwork black,
                                               ValidationConfig config, ValidationControl control, PlayerFactory factory,
                                               ValidationProgressTracker progress) {
        HeadlessGame game = opening.newGame(config.maximumPlies());
        if (control.cancelled()) return new ValidationResult.Game(GameTermination.CANCELLED, 0);
        if (!game.active()) return summary(game);
        try (Player whitePlayer = factory.create(white, config); Player blackPlayer = factory.create(black, config)) {
            while (game.active()) {
                if (control.cancelled()) { game.abort(GameTermination.CANCELLED, null); break; }
                var searchControl = control.beginSearch();
                try {
                    Player player = game.sideToMove() == Value.WHITE ? whitePlayer : blackPlayer;
                    long move = player.move(new SearchRequest(game.boardSnapshot(), game.historySnapshot(),
                            config.depth(), searchControl));
                    if (control.cancelled()) game.abort(GameTermination.CANCELLED, null);
                    else if (!searchControl.checkpoint()) game.abort(GameTermination.SEARCH_FAILURE, "Search stopped.");
                    else {
                        game.play(move);
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
        }
        return summary(game);
    }

    private static ValidationResult.Game summary(HeadlessGame game) {
        return new ValidationResult.Game(game.termination(), game.playedPlies());
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
