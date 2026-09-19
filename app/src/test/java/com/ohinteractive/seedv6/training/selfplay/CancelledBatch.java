package com.ohinteractive.seedv6.training.selfplay;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.search.alphabeta.RootParallelSearch;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.iterative.IterativeDeepeningSearch;

/** Deterministically pause one incomplete game inside the real E batch/sampling boundary. */
public final class CancelledBatch {
    public static SelfPlayBatch generate(NnueNetwork actor, SelfPlayConfig config, SelfPlayControl control, Runnable pause) {
        return SelfPlayBatch.generate(actor, config, control, index -> {
            HeadlessGame game = new HeadlessGame(Board.startingPosition(), config.maximumPlies());
            game.play(game.legalMoves()[0]);
            try (var search = new IterativeDeepeningSearch(new RootParallelSearch(1,
                    SearchEvaluation.incremental(actor, config.scoreMapping())))) {
                return SelfPlayRunner.drive(game, config, index, control, request -> {
                    long[] board = new long[Board.MAX_BITBOARDS]; request.copyBoardInto(board);
                    var outcome = search.search(new SearchRequest(board, request.gameHistory(), 8, new SearchObserver() {
                        @Override public void onIterationCompleted(IterationSnapshot snapshot) { pause.run(); }
                    }, request.control()));
                    if (!control.cancelled() || request.control().termination() != SearchTermination.STOPPED) {
                        throw new AssertionError("Cancellation did not reach the active NNUE search.");
                    }
                    return outcome.lastCompletedResult().bestMove();
                });
            }
        });
    }
    private CancelledBatch() {}
}
