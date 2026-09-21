package com.ohinteractive.seedv6.core.brn;

import com.ohinteractive.seedv6.core.Board;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static com.ohinteractive.seedv6.core.brn.BrnFeatureSchema.*;
import static org.junit.jupiter.api.Assertions.*;

class BrnFeatureTest {
    @Test
    void authoritativeFenAndStartingBoardExposeExactPrimitiveCodes() {
        long[] board = Board.startingPosition();
        int[] backRank = {3, 5, 4, 2, 1, 4, 5, 3};
        for (int square = 0; square < 64; square++) {
            int expected = square < 8 ? backRank[square] : square < 16 ? 6
                    : square < 48 ? 0 : square < 56 ? 14 : backRank[square - 56] + 8;
            assertEquals(expected, squareCode(board[0], board[1], board[2], board[3], square));
        }
        board = Board.fromFen("7k/8/2n5/8/4P3/8/8/K7 b - e3 17 42");
        assertEquals(1, code(board, 0));
        assertEquals(9, code(board, 63));
        assertEquals(13, code(board, 42));
        assertEquals(6, code(board, 28));
        assertEquals(0, code(board, 20)); // En-passant is only in status, not a board entity.
        assertEquals(1L | (20L << 5) | (17L << 11) | (42L << 18), board[Board.STATUS]);
        assertEquals(6, board.length); // Five primitive inputs plus the separate excluded key.
    }

    @Test
    void allNodeSlotsAreContiguousUniqueAndDeterministic() {
        boolean[] seen = new boolean[PARAMETER_COUNT];
        for (int code = 1; code <= 15; code++) {
            for (int square = 0; square < 64; square++) {
                int index = nodeIndex(code, square);
                assertEquals(1 + (code - 1) * 64 + square, index);
                assertFalse(seen[index]);
                seen[index] = true;
                long bit = 1L << square;
                long[] board = { (code & 1) != 0 ? bit : 0, (code & 2) != 0 ? bit : 0,
                        (code & 4) != 0 ? bit : 0, (code & 8) != 0 ? bit : 0, 0 };
                BrnFeatures features = new BrnFeatures();
                features.extract(board);
                assertEquals(code, code(board, square));
                assertEquals(1, features.nodeCount());
                assertEquals(index, features.indexAt(1));
            }
        }
        assertEquals(26225, PARAMETER_COUNT);
    }

    @Test
    void everyEndpointCodeAndBoardPairMatchesIndependentCanonicalDisplacementOracle() {
        boolean[] seen = new boolean[RELATION_COUNT];
        for (int codeA = 1; codeA <= 15; codeA++) {
            for (int codeB = 1; codeB <= 15; codeB++) {
                for (int squareA = 0; squareA < 64; squareA++) {
                    for (int squareB = squareA + 1; squareB < 64; squareB++) {
                        int dx = squareB % 8 - squareA % 8;
                        int dy = squareB / 8 - squareA / 8;
                        int ordinal = 0;
                        // Deliberately enumerate the specification instead of using its flat formula.
                        outer: for (int y = 0; y <= 7; y++) {
                            for (int x = y == 0 ? 1 : -7; x <= 7; x++) {
                                if (x == dx && y == dy) break outer;
                                ordinal++;
                            }
                        }
                        int expected = RELATION_OFFSET + ((codeA - 1) * 15 + codeB - 1) * 112 + ordinal;
                        int actual = relationIndex(codeA, squareA, codeB, squareB);
                        assertEquals(expected, actual);
                        assertEquals(actual, relationIndex(codeB, squareB, codeA, squareA));
                        seen[actual - RELATION_OFFSET] = true;
                    }
                }
            }
        }
        for (boolean present : seen) assertTrue(present); // All 25,200 valid relation slots reached.
    }

    @Test
    void namedOffsetsEdgesAndEndpointOrderRemainDistinct() {
        int[] ends = {1, 8, 9, 17, 26, 63}; // Horizontal, vertical, diagonal, knight, non-ray, edge.
        for (int b : ends) assertEquals(relationIndex(6, 0, 14, b), relationIndex(14, b, 6, 0));
        assertEquals(relationIndex(3, 7, 11, 56), relationIndex(11, 56, 3, 7));
        assertNotEquals(relationIndex(3, 7, 11, 56), relationIndex(11, 7, 3, 56));
        assertNotEquals(relationIndex(6, 0, 14, 26), relationIndex(6, 0, 14, 19));
        assertEquals(relationIndex(6, 0, 14, 26), relationIndex(6, 9, 14, 35)); // Translation shares weight.
    }

    @Test
    void extractionCountsEveryOccupiedPairAndNeverEmptySquares() {
        BrnFeatures features = new BrnFeatures();
        long[] board = Board.startingPosition();
        long[] unchanged = board.clone();
        features.extract(board);
        assertCounts(features, 32, Long.bitCount(board[Board.STATUS]));
        assertArrayEquals(unchanged, board);
        int[] original = indices(features);
        board[Board.KEY] = ~board[Board.KEY];
        features.extract(board);
        assertArrayEquals(original, indices(features));
        features.extract(Arrays.copyOf(board, 5));
        assertArrayEquals(original, indices(features));

        features.extract(Board.fromFen("7k/8/8/8/8/8/8/K7 w - - 0 1"));
        assertCounts(features, 2, 1);
        assertEquals(relationIndex(1, 0, 9, 63), features.indexAt(3));
        features.extract(new long[] {-1, -1, -1, -1, -1});
        assertCounts(features, 64, 64);
        assertEquals(MAX_ACTIVE_FEATURES, features.size());
        features.extract(new long[5]);
        assertCounts(features, 0, 0);
    }

    @Test
    void duplicateRelationsRetainPhysicalPairMultiplicity() {
        long[] board = Board.fromFen("7k/8/8/8/8/8/PPP5/7K w - - 0 1");
        BrnFeatures features = new BrnFeatures();
        features.extract(board);
        int adjacentPawns = relationIndex(6, 8, 6, 9);
        int count = 0;
        for (int i = 0; i < features.size(); i++) if (features.indexAt(i) == adjacentPawns) count++;
        assertEquals(2, count);
        assertCounts(features, 5, 1);
    }

    @Test
    void malformedIndicesAndShortBoardsAreRejected() {
        for (int bad : new int[] {-1, 0, 16}) assertThrows(IllegalArgumentException.class, () -> nodeIndex(bad, 0));
        for (int bad : new int[] {-1, 64}) {
            assertThrows(IllegalArgumentException.class, () -> nodeIndex(1, bad));
            assertThrows(IllegalArgumentException.class, () -> squareCode(0, 0, 0, 0, bad));
            assertThrows(IllegalArgumentException.class, () -> statusIndex(bad));
        }
        assertThrows(IllegalArgumentException.class, () -> relationIndex(1, 0, 2, 0));
        assertThrows(IllegalArgumentException.class, () -> relationIndex(0, 0, 2, 1));
        assertThrows(IllegalArgumentException.class, () -> new BrnFeatures().extract(new long[4]));
        assertThrows(NullPointerException.class, () -> new BrnFeatures().extract(null));
    }

    private static int code(long[] board, int square) { return squareCode(board[0], board[1], board[2], board[3], square); }
    private static int[] indices(BrnFeatures features) {
        int[] result = new int[features.size()];
        for (int i = 0; i < result.length; i++) result[i] = features.indexAt(i);
        return result;
    }
    private static void assertCounts(BrnFeatures features, int nodes, int status) {
        assertEquals(nodes, features.nodeCount());
        assertEquals(nodes * (nodes - 1) / 2, features.relationCount());
        assertEquals(status, features.statusCount());
        assertEquals(1 + nodes + nodes * (nodes - 1) / 2 + status, features.size());
    }
}
