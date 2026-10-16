package com.ohinteractive.seedv6.search.alphabeta;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.LegalMoveResolver;
import com.ohinteractive.seedv6.core.move.MoveIntent;
import com.ohinteractive.seedv6.core.move.MoveIntent.Promotion;
import com.ohinteractive.seedv6.search.alphabeta.AlphaBetaPvsSearch.Configuration;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;
import com.ohinteractive.seedv6.search.tt.TranspositionTable;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.Bound;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.Cacheability;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlphaBetaPrincipalVariationTest {

    private record Special(String fen, String move, Promotion promotion) {}

    @Test
    void authoritativeHashHintCanProveEverySpecialRootMoveWithoutInventingContinuation() {
        final List<Special> specials = List.of(
            new Special(
                "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1",
                "e1g1", Promotion.NONE
            ),
            new Special(
                "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1",
                "e5d6", Promotion.NONE
            ),
            new Special(
                "7k/P7/8/8/8/8/8/7K w - - 0 1",
                "a7a8", Promotion.QUEEN
            ),
            new Special(
                "k6r/6P1/8/8/8/8/8/7K w - - 0 1",
                "g7h8", Promotion.QUEEN
            ),
            new Special(
                "7k/P7/8/8/8/8/8/7K w - - 0 1",
                "a7a8", Promotion.KNIGHT
            )
        );

        for(Special special : specials) {
            final long[] board = Board.fromFen(special.fen);
            final long move = resolve(board, special.move, special.promotion);
            final TranspositionTable table = new TranspositionTable(1024);
            table.store(
                board[Board.KEY], 0, Bound.EXACT, 0, 0, move,
                Cacheability.POSITION_ONLY
            );
            final AlphaBetaPvsSearch search = new AlphaBetaPvsSearch(
                table, new Configuration(true, true, true)
            );
            final SearchResult result = search.search(
                new SearchRequest(board, 1),
                -TranspositionScores.MATE_SCORE - 1,
                -TranspositionScores.MATE_SCORE
            );
            assertTrue(result.completed(), special.toString());
            assertEquals(move, result.bestMove(), special.toString());
            assertArrayEquals(new long[] {move}, result.principalVariation());
            AlphaBetaPvsSearchTest.assertLegalPv(board, result);
        }
    }

    @Test
    void matePvReplaysAndWarmTtTruncationRemainsLegal() {
        final long[] board = Board.fromFen(
            "7k/6p1/5KQ1/8/8/8/8/8 w - - 0 1"
        );
        final AlphaBetaPvsSearch search = new AlphaBetaPvsSearch(
            new TranspositionTable(4096)
        );
        final SearchResult cold = search.search(new SearchRequest(board, 3));
        final SearchResult warm = search.search(new SearchRequest(board, 3));
        assertEquals(TranspositionScores.MATE_SCORE - 1, cold.score());
        assertEquals(cold.score(), warm.score());
        AlphaBetaPvsSearchTest.assertLegalPv(board, cold);
        AlphaBetaPvsSearchTest.assertLegalPv(board, warm);
        assertTrue(warm.principalVariationLength() <= cold.principalVariationLength());
    }

    @Test
    void depthZeroReturnsLegalFallbackButNoFabricatedSearchedPv() {
        final long[] board = Board.startingPosition();
        final SearchResult result = new AlphaBetaPvsSearch().search(
            new SearchRequest(board, 0)
        );
        assertTrue(result.completed());
        assertTrue(result.hasMove());
        assertEquals(20, result.legalRootMoves());
        assertEquals(0, result.principalVariationLength());
    }

    @Test
    void pvCapacityAndSearchResultOwnershipAreExplicitlyBounded() {
        assertEquals(256, AlphaBetaPvsSearch.MAX_PV_MOVES);
        assertEquals(256, AlphaBetaPvsSearch.MAX_SUPPORTED_DEPTH);
        assertDoesNotThrow(() -> AlphaBetaPvsSearch.requireChildCapacity(255));
        assertThrows(
            AlphaBetaPvsSearch.MainSearchCapacityException.class,
            () -> AlphaBetaPvsSearch.requireChildCapacity(256)
        );

        final long[] maximum = new long[AlphaBetaPvsSearch.MAX_PV_MOVES];
        Arrays.fill(maximum, 7L);
        final SearchResult result = new SearchResult(
            7L, true, 0, 256, 0L, 1, true, maximum
        );
        maximum[0] = 9L;
        assertEquals(7L, result.principalVariationMove(0));

        final long[] tooLong = new long[AlphaBetaPvsSearch.MAX_PV_MOVES + 1];
        Arrays.fill(tooLong, 7L);
        assertThrows(IllegalArgumentException.class, () -> new SearchResult(
            7L, true, 0, 256, 0L, 1, true, tooLong
        ));
    }

    private static long resolve(
        long[] board, String coordinate, Promotion promotion
    ) {
        return new LegalMoveResolver().resolve(
            board, new MoveIntent(
                square(coordinate.substring(0, 2)),
                square(coordinate.substring(2, 4)), promotion
            )
        );
    }

    private static int square(String coordinate) {
        return (coordinate.charAt(1) - '1') * 8 + coordinate.charAt(0) - 'a';
    }
}
