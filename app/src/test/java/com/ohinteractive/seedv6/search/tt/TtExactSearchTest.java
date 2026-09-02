package com.ohinteractive.seedv6.search.tt;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.LegalMoveResolver;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.move.MoveIntent;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.flat.FlatNegamax;
import com.ohinteractive.seedv6.search.tt.TtExactSearchHarness.Result;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TtExactSearchTest {

    @Test
    void ttOnOffAndPreservedFlatOracleAgreeAcrossShallowTransposingSearches() {
        final List<SearchCase> cases = List.of(
            new SearchCase("4k1n1/8/8/8/8/8/8/Q3K1N1 w - - 0 1", 4),
            new SearchCase("4k3/8/8/3p4/4P3/8/8/R3K2R w KQ - 0 1", 3),
            new SearchCase("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1", 3),
            new SearchCase("7k/8/5KQ1/8/8/8/8/8 w - - 0 1", 2),
            new SearchCase("7k/6Q1/5K2/8/8/8/8/8 b - - 0 1", 2),
            new SearchCase("7k/5K2/6Q1/8/8/8/8/8 b - - 0 1", 2)
        );
        long internalHits = 0L;
        long probes = 0L;

        for(SearchCase searchCase : cases) {
            final long[] board = Board.fromFen(searchCase.fen);
            final long[] original = board.clone();
            final GameHistory history = GameHistory.initial(board);
            final SearchRequest request = new SearchRequest(board, history, searchCase.depth);
            final SearchResult flat = new FlatNegamax().search(
                request
            );
            final Result disabled = TtExactSearchHarness.search(request, null);
            final TranspositionTable table = new TranspositionTable(1 << 14);
            table.advanceGeneration();
            final Result enabled = TtExactSearchHarness.search(request, table);
            final Result warm = TtExactSearchHarness.search(request, table);

            assertArrayEquals(original, board);
            final long[] requestBoardAfter = new long[Board.MAX_BITBOARDS];
            request.copyBoardInto(requestBoardAfter);
            assertArrayEquals(original, requestBoardAfter);
            assertEquals(flat.score(), disabled.score());
            assertEquals(disabled.score(), enabled.score());
            assertEquals(disabled.score(), warm.score());
            assertEquals(flat.hasMove(), enabled.hasMove());
            assertEquals(flat.legalRootMoves(), enabled.legalRootMoves());
            assertEquals(disabled.pathDependent(), enabled.pathDependent());
            assertTrue(enabled.nodes() <= disabled.nodes());
            if(enabled.hasMove()) {
                assertTrue(isLegalStoredMove(board, enabled.bestMove()));
                assertTrue(isLegalStoredMove(board, warm.bestMove()));
                assertEquals(
                    enabled.score(),
                    TtExactSearchHarness.scoreAfterRootMove(
                        board, history, searchCase.depth, enabled.bestMove()
                    )
                );
                assertEquals(
                    warm.score(),
                    TtExactSearchHarness.scoreAfterRootMove(
                        board, history, searchCase.depth, warm.bestMove()
                    )
                );
                assertTrue(warm.scoreHits() > 0);
                assertTrue(warm.nodes() < enabled.nodes());
            } else {
                assertEquals(0L, enabled.bestMove());
                assertEquals(0L, warm.bestMove());
                assertEquals(0, enabled.legalRootMoves());
            }

            internalHits += enabled.scoreHits();
            probes += enabled.probes();
        }

        assertTrue(probes > 100, "TT-enabled searches did not perform substantial probing.");
        assertTrue(internalHits > 0, "No within-search transposition was reused.");
    }

    @Test
    void repetitionDrawAndNonRepetitionContextsSharingRawKeyRemainIsolated() {
        final long[] board = Board.fromFen("4k3/8/8/8/8/8/8/Q3K3 w - - 2 1");
        final GameHistory repetition = GameHistory.builder(board)
            .appendPosition(board)
            .appendPosition(board)
            .snapshot();
        final GameHistory ordinary = GameHistory.initial(board);
        final TranspositionTable table = new TranspositionTable(1 << 10);
        table.advanceGeneration();

        final Result drawnFirst = TtExactSearchHarness.search(board, repetition, 2, table);
        final Result ordinaryAfter = TtExactSearchHarness.search(board, ordinary, 2, table);
        final Result ordinaryOracle = TtExactSearchHarness.search(board, ordinary, 2, null);

        assertEquals(0, drawnFirst.score());
        assertTrue(drawnFirst.pathDependent());
        assertEquals(0, drawnFirst.stores());
        assertTrue(drawnFirst.uncacheableStores() > 0);
        assertFalse(ordinaryAfter.pathDependent());
        assertEquals(ordinaryOracle.score(), ordinaryAfter.score());
        assertTrue(ordinaryAfter.score() != 0);

        final TranspositionTable reverseTable = new TranspositionTable(1 << 10);
        reverseTable.advanceGeneration();
        final Result ordinaryFirst = TtExactSearchHarness.search(board, ordinary, 2, reverseTable);
        final Result drawnAfter = TtExactSearchHarness.search(board, repetition, 2, reverseTable);
        assertEquals(ordinaryOracle.score(), ordinaryFirst.score());
        assertEquals(0, drawnAfter.score());
        assertTrue(drawnAfter.pathDependent());
        assertEquals(0, drawnAfter.probes(), "Rule draw must be adjudicated before position-only probing.");
    }

    @Test
    void fiftyMoveAndLowerHalfmoveContextsSharingRawKeyRemainIsolated() {
        final long[] ordinary = Board.fromFen("4k3/8/8/8/8/8/8/Q3K3 w - - 0 1");
        final long[] fiftyMove = Board.fromFen("4k3/8/8/8/8/8/8/Q3K3 w - - 100 1");
        assertEquals(ordinary[Board.KEY], fiftyMove[Board.KEY]);
        final TranspositionTable table = new TranspositionTable(1 << 10);
        table.advanceGeneration();

        final Result drawnFirst = TtExactSearchHarness.search(
            fiftyMove, GameHistory.initial(fiftyMove), 2, table
        );
        final Result ordinaryAfter = TtExactSearchHarness.search(
            ordinary, GameHistory.initial(ordinary), 2, table
        );
        final Result ordinaryOracle = TtExactSearchHarness.search(
            ordinary, GameHistory.initial(ordinary), 2, null
        );

        assertEquals(0, drawnFirst.score());
        assertTrue(drawnFirst.pathDependent());
        assertEquals(0, drawnFirst.stores());
        assertEquals(ordinaryOracle.score(), ordinaryAfter.score());
        assertTrue(ordinaryAfter.score() != 0);

        final TranspositionTable reverseTable = new TranspositionTable(1 << 10);
        reverseTable.advanceGeneration();
        final Result ordinaryFirst = TtExactSearchHarness.search(
            ordinary, GameHistory.initial(ordinary), 2, reverseTable
        );
        final Result drawnAfter = TtExactSearchHarness.search(
            fiftyMove, GameHistory.initial(fiftyMove), 2, reverseTable
        );
        assertEquals(ordinaryOracle.score(), ordinaryFirst.score());
        assertEquals(0, drawnAfter.score());
        assertTrue(drawnAfter.pathDependent());
        assertEquals(0, drawnAfter.probes(), "50-move draw must precede position-only probing.");
    }

    private record SearchCase(String fen, int depth) {}

    private static boolean isLegalStoredMove(long[] board, long storedMove) {
        if(storedMove == 0L) return false;
        final LegalMoveResolver resolver = new LegalMoveResolver();
        try {
            final long resolved = resolver.resolve(
                board,
                new MoveIntent(
                    Move.fromSquare(storedMove), Move.toSquare(storedMove),
                    Move.promotion(storedMove)
                )
            );
            return resolved == storedMove;
        } catch(IllegalArgumentException exception) {
            return false;
        }
    }
}
