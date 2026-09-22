package com.ohinteractive.seedv6.training.selfplay;

import java.util.Objects;
import java.util.SplittableRandom;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.training.model.NetworkModel;
import com.ohinteractive.seedv6.search.alphabeta.RootParallelSearch;
import com.ohinteractive.seedv6.search.common.SearchControl;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.iterative.IterativeDeepeningSearch;

/** Synchronous pinned-network self-play game. Fresh private TT/order state per game, reused only within that game. */
public final class SelfPlayRunner {
    /** Independent indexed seeds: no preceding game's length consumes another game's RNG stream. */
    public static long gameSeed(long masterSeed, int gameIndex) {
        if (gameIndex < 0) throw new IllegalArgumentException("Negative game index.");
        return new SplittableRandom(masterSeed + 0x9E3779B97F4A7C15L * gameIndex).nextLong();
    }

    public static GameTrajectory play(NnueNetwork network, SelfPlayConfig config, int gameIndex,
                                      long[] initialBoard, SelfPlayControl control) {
        return play(new NetworkModel.Nnue(network), config, gameIndex, initialBoard, control);
    }

    public static GameTrajectory play(NetworkModel network, SelfPlayConfig config, int gameIndex,
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
                new RootParallelSearch(config.threads(), network.evaluation(config.scoreMapping())))) {
            return drive(game, config, gameIndex, control, new MoveSelector() {
                private SearchResult completed;
                public SearchResult lastResult() { return completed; }
                public long select(SearchRequest request) {
                    var outcome = search.search(request);
                    SearchResult result = outcome.lastCompletedResult();
                    if (!outcome.targetDepthCompleted() || result == null || !result.completed() || !result.hasMove()) {
                        throw new IllegalStateException("Search did not complete the requested depth: "
                                + request.control().termination());
                    }
                    completed = result;
                    return result.bestMove();
                }
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
    interface MoveSelector {
        long select(SearchRequest request);
        default SearchResult lastResult() { return null; }
    }

    // Same loop used by real NNUE search; package seam avoids expensive searches for rule/failure tests.
    static GameTrajectory drive(HeadlessGame game, SelfPlayConfig config, int gameIndex,
                                SelfPlayControl control, MoveSelector selector) {
        SplittableRandom random = new SplittableRandom(gameSeed(config.seed(), gameIndex));
        int openingPlies = (int) random.nextLong(config.minimumOpeningPlies(), (long) config.maximumOpeningPlies() + 1);
        var presentation = control.presentation();
        if (presentation != null) presentation.start(game, gameIndex + 1, 0);
        try {
            while (game.active()) {
                if (control.cancelled()) { game.abort(GameTermination.CANCELLED, null); break; }
                long move;
                SearchResult completed = null;
                if (game.playedPlies() < openingPlies) {
                    long[] legal = game.legalMoves();
                    move = legal[random.nextInt(legal.length)];
                } else {
                    SearchControl searchControl = control.beginSearch(config);
                    try {
                        move = selector.select(new SearchRequest(game.boardSnapshot(), game.historySnapshot(),
                                config.depth(), searchControl));
                        completed = selector.lastResult();
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
                    if (presentation != null) presentation.moved(game, move, completed);
                } catch (IllegalArgumentException failure) {
                    game.abort(GameTermination.SEARCH_FAILURE, failure.toString());
                }
            }
            return game.trajectory();
        } finally { if (presentation != null) presentation.clear(); }
    }

    private SelfPlayRunner() {}
}
