package com.ohinteractive.seedv6.training.validation;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.alphabeta.RootParallelSearch;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.iterative.IterativeDeepeningSearch;

/** Test-only pause inside a registered, real NNUE arena search. */
public final class CancelledArena {
    public static ValidationResult validate(NnueNetwork candidate, NnueNetwork incumbent, ValidationConfig config,
            long[] board, GameHistory history, ValidationControl control, Runnable pause, java.util.function.Consumer<ValidationProgress> observer) {
        return new ValidationArena().validate(candidate, incumbent, config, board, history, control, (network, settings) -> {
            var search = new IterativeDeepeningSearch(new RootParallelSearch(1,
                    SearchEvaluation.incremental(network, settings.scoreMapping())));
            return new ValidationArena.Player() {
                @Override public long move(SearchRequest request) {
                    long[] root = new long[Board.MAX_BITBOARDS]; request.copyBoardInto(root);
                    var result = search.search(new SearchRequest(root, request.gameHistory(), 8, new SearchObserver() {
                        @Override public void onIterationCompleted(IterationSnapshot snapshot) { pause.run(); }
                    }, request.control()));
                    if (request.control().termination() != SearchTermination.STOPPED) throw new AssertionError("Search was not cancelled.");
                    return result.lastCompletedResult().bestMove();
                }
                @Override public void close() { search.close(); }
            };
        }, observer, TimeSource.SYSTEM);
    }
    private CancelledArena() {}
}
