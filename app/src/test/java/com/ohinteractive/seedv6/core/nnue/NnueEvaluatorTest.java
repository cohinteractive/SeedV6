package com.ohinteractive.seedv6.core.nnue;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Value;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class NnueEvaluatorTest {

    private static final String KNOWN_WHITE = "7k/8/8/8/8/8/P7/K7 w - - 0 1";
    private static final String KNOWN_BLACK = "7k/8/8/8/8/8/P7/K7 b - - 0 1";
    private static final NnueNetwork KNOWN_NETWORK = knownNetwork();
    private static final NnueNetwork SEEDED_NETWORK = NnueNetwork.initialized(73L);
    private static final String[] POSITIONS = {
            Board.FEN_STARTING_POSITION,
            // Existing EvalTest middlegame FEN; no handcrafted score is used.
            "r3k2r/pppq1ppp/2npbn2/3Np3/2B1P3/2N2Q1P/PPP2PP1/R3K2R w KQkq - 4 12",
            "8/4k3/8/p7/8/8/8/4K3 b - - 0 1",
            // Two white queens require promoted material; Black is legally in check.
            "7k/8/8/8/8/8/1Q6/K6Q b - - 0 1"
    };

    @Test
    void knownAnswerChecksBothAccumulatorsHiddenLayerAndUnclippedOutput() {
        NnueEvaluator evaluator = new NnueEvaluator(KNOWN_NETWORK);
        float raw = evaluator.evaluate(Board.fromFen(KNOWN_WHITE));
        assertKnownAccumulators(evaluator);
        float[] expectedHidden = new float[32];
        expectedHidden[0] = 0.375f;
        expectedHidden[2] = 1;
        expectedHidden[4] = 0.34375f;
        expectedHidden[31] = 0.5625f;
        assertHidden(expectedHidden, evaluator);
        // -1/2 + 8*(3/8) + (1/4)*1 + (1/2)*(11/32) - (1/4)*(9/16).
        assertEquals(89.0f / 32, raw);
        assertEquals(raw, evaluator.raw());
        assertTrue(raw > 1, "Output must not use clip01");
        assertEquals(Math.tanh(89.0 / 32), evaluator.boundedValue(), 1e-15);
        assertOutputContract(evaluator);
    }

    @Test
    void asymmetricKnownAnswerProvesSideToMovePerspectiveComesFirst() {
        NnueEvaluator evaluator = new NnueEvaluator(KNOWN_NETWORK);
        evaluator.evaluate(Board.fromFen(KNOWN_WHITE));
        float whiteRaw = evaluator.raw();
        evaluator.evaluate(Board.fromFen(KNOWN_BLACK));
        assertKnownAccumulators(evaluator); // Side-to-move changes ordering, not feature sets.
        float[] expectedHidden = new float[32];
        expectedHidden[1] = 0.125f;
        expectedHidden[2] = 1;
        expectedHidden[3] = 0.125f;
        expectedHidden[4] = 0.40625f;
        expectedHidden[31] = 0.3125f;
        assertHidden(expectedHidden, evaluator);
        // -1/2 - (1/4)*(1/8) + 1/4 + (1/2)*(1/8) + (1/2)*(13/32) - (1/4)*(5/16).
        assertEquals(-3.0f / 32, evaluator.raw());
        assertNotEquals(whiteRaw, evaluator.raw());
        assertEquals(Math.tanh(-3.0 / 32), evaluator.boundedValue(), 1e-15);
        assertOutputContract(evaluator);
    }

    @Test
    void fullRecomputationMatchesIndependentFenAndDoublePrecisionOracle() {
        NnueEvaluator evaluator = new NnueEvaluator(SEEDED_NETWORK);
        for (String fen : POSITIONS) {
            long[] board = Board.fromFen(fen);
            long[] before = board.clone();
            Oracle expected = referenceFromFen(fen, SEEDED_NETWORK);
            evaluator.evaluate(board);
            assertArrayEquals(before, board, fen);
            for (int perspective = 0; perspective < 2; perspective++) {
                for (int unit = 0; unit < 64; unit++) {
                    assertEquals(expected.accumulators[perspective][unit],
                            evaluator.accumulator(perspective, unit), 1e-7, fen);
                    assertTrue(evaluator.accumulator(perspective, unit) < 1,
                            "Small initialization must not upper-saturate ordinary positions");
                }
            }
            for (int unit = 0; unit < 32; unit++) {
                assertEquals(expected.hidden[unit], evaluator.hidden(unit), 1e-7, fen);
            }
            assertEquals(expected.raw, evaluator.raw(), 1e-8, fen);
            assertEquals(Math.tanh(expected.raw), evaluator.boundedValue(), 1e-8, fen);
            assertOutputContract(evaluator);
        }
    }

    @Test
    void reusableScratchResetsAndSeparateEvaluatorsCanShareOneNetwork() {
        NnueEvaluator first = new NnueEvaluator(KNOWN_NETWORK);
        NnueEvaluator second = new NnueEvaluator(KNOWN_NETWORK);
        first.evaluate(Board.fromFen(KNOWN_WHITE));
        second.evaluate(Board.fromFen(KNOWN_BLACK));
        assertEquals(89.0f / 32, first.raw());
        assertEquals(-3.0f / 32, second.raw());
        for (String fen : POSITIONS) first.evaluate(Board.fromFen(fen));
        first.evaluate(Board.fromFen(KNOWN_WHITE));
        assertKnownAccumulators(first);
        assertEquals(89.0f / 32, first.raw());
        assertEquals(-3.0f / 32, second.raw());
    }

    @Test
    void everySuppliedBoardLongIsPreservedAndUnusedMetadataIsNotAFeature() {
        NnueEvaluator evaluator = new NnueEvaluator(KNOWN_NETWORK);
        long[] board = Arrays.copyOf(Board.fromFen(KNOWN_WHITE), Board.MAX_BITBOARDS + 2);
        // Castling, clocks, en-passant and key do not contribute primitive NNUE features.
        board[Board.STATUS] = 0x76543210L;
        board[Board.KEY] = Long.MIN_VALUE;
        board[Board.MAX_BITBOARDS] = 0x123456789abcdefL;
        board[Board.MAX_BITBOARDS + 1] = Long.MAX_VALUE;
        long[] before = board.clone();
        evaluator.evaluate(board);
        assertEquals(89.0f / 32, evaluator.raw());
        assertArrayEquals(before, board);
    }

    @Test
    void missingAndDuplicateKingsFailForEachColourWithoutMutatingBoards() {
        String[] invalid = {
                "7k/8/8/8/8/8/P7/8 w - - 0 1",
                "7k/8/8/8/8/8/P7/KK6 w - - 0 1",
                "8/8/8/8/8/8/P7/K7 w - - 0 1",
                "6kk/8/8/8/8/8/P7/K7 w - - 0 1"
        };
        NnueEvaluator evaluator = new NnueEvaluator(KNOWN_NETWORK);
        for (int i = 0; i < invalid.length; i++) {
            evaluator.evaluate(Board.fromFen(KNOWN_WHITE));
            long[] board = Board.fromFen(invalid[i]);
            long[] before = board.clone();
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> evaluator.evaluate(board));
            assertEquals("NNUE requires exactly one " + (i < 2 ? "White" : "Black") + " king.",
                    error.getMessage());
            assertArrayEquals(before, board);
            assertThrows(IllegalStateException.class, evaluator::raw);
        }
        evaluator.evaluate(Board.fromFen(KNOWN_WHITE));
        assertEquals(89.0f / 32, evaluator.raw());
    }

    @Test
    void invalidShapesEncodingsAndUnavailableResultsFailExplicitly() {
        assertThrows(NullPointerException.class, () -> new NnueEvaluator(null));
        NnueEvaluator evaluator = new NnueEvaluator(KNOWN_NETWORK);
        assertThrows(IllegalStateException.class, evaluator::raw);
        assertThrows(IllegalStateException.class, evaluator::boundedValue);
        assertThrows(IllegalStateException.class, () -> evaluator.accumulator(Value.WHITE, 0));
        assertThrows(IllegalStateException.class, () -> evaluator.hidden(0));
        assertThrows(NullPointerException.class, () -> evaluator.evaluate(null));
        for (int length = 0; length < Board.MAX_BITBOARDS; length++) {
            long[] shortBoard = new long[length];
            assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate(shortBoard));
        }
        long[] colourOnEmptySquare = Board.fromFen(KNOWN_WHITE);
        colourOnEmptySquare[3] |= 1L << 16;
        assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate(colourOnEmptySquare));
        long[] unknownPiece = Board.fromFen(KNOWN_WHITE);
        unknownPiece[0] |= 1L << 16;
        unknownPiece[1] |= 1L << 16;
        unknownPiece[2] |= 1L << 16;
        assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate(unknownPiece));
    }

    private static void assertKnownAccumulators(NnueEvaluator evaluator) {
        // Unclipped W: [1/2, -1/4, 1/4, 5/4, -1/2, ..., 3/8].
        // Unclipped B: [-1/4, 1/2, -1/8, 1/2, -1/2, ..., 5/8].
        float[] white = new float[64];
        float[] black = new float[64];
        white[0] = 0.5f;
        white[2] = 0.25f;
        white[3] = 1;
        white[63] = 0.375f;
        black[1] = 0.5f;
        black[3] = 0.5f;
        black[63] = 0.625f;
        for (int unit = 0; unit < 64; unit++) {
            assertEquals(white[unit], evaluator.accumulator(Value.WHITE, unit), "White unit " + unit);
            assertEquals(black[unit], evaluator.accumulator(Value.BLACK, unit), "Black unit " + unit);
        }
    }

    private static void assertHidden(float[] expected, NnueEvaluator evaluator) {
        for (int unit = 0; unit < 32; unit++) {
            assertEquals(expected[unit], evaluator.hidden(unit), "Hidden unit " + unit);
        }
    }

    private static void assertOutputContract(NnueEvaluator evaluator) {
        assertTrue(Float.isFinite(evaluator.raw()));
        assertTrue(Double.isFinite(evaluator.boundedValue()));
        assertTrue(evaluator.boundedValue() >= -1 && evaluator.boundedValue() <= 1);
    }

    private static NnueNetwork knownNetwork() {
        float[] bias = new float[64];
        bias[0] = 0.125f;
        bias[1] = 0.25f;
        bias[2] = -0.25f;
        bias[3] = 0.75f;
        bias[4] = -0.5f;
        bias[63] = 0.125f;
        float[] features = new float[49_152 * 64];
        // Literal rows for Ka1/Pa2/kh8, independent of the production schema API:
        // White: friendly king a1=320, friendly pawn a2=8, enemy king h8=767.
        // Black (king h1): enemy king a8=6136, enemy pawn a7=5808, friendly king h1=5703.
        features[320 * 64] = 0.25f;
        features[320 * 64 + 1] = 0.25f;
        features[8 * 64] = 0.125f;
        features[8 * 64 + 2] = 0.5f;
        features[8 * 64 + 3] = 0.5f;
        features[767 * 64 + 1] = -0.75f;
        features[767 * 64 + 63] = 0.25f;
        features[6136 * 64] = 0.125f;
        features[6136 * 64 + 1] = 0.25f;
        features[5808 * 64] = -0.5f;
        features[5808 * 64 + 2] = 0.125f;
        features[5703 * 64 + 3] = -0.25f;
        features[5703 * 64 + 63] = 0.5f;

        float[] hiddenBias = new float[32];
        hiddenBias[0] = 0.125f;
        hiddenBias[1] = -0.125f;
        hiddenBias[2] = -0.25f;
        hiddenBias[3] = 0.125f;
        hiddenBias[31] = 0.125f;
        float[] hidden = new float[32 * 128];
        hidden[0] = 0.5f;
        hidden[64] = -0.25f;
        hidden[128 + 1] = 0.5f;
        hidden[128 + 65] = 0.25f;
        hidden[2 * 128 + 3] = 1;
        hidden[2 * 128 + 67] = 1;
        hidden[3 * 128 + 2] = -2;
        hidden[3 * 128 + 65] = -0.5f;
        hidden[4 * 128 + 63] = 0.5f;
        hidden[4 * 128 + 127] = 0.25f;
        hidden[31 * 128] = 0.25f;
        hidden[31 * 128 + 127] = 0.5f;
        float[] output = new float[32];
        output[0] = 8;
        output[1] = -0.25f;
        output[2] = 0.25f;
        output[3] = 0.5f;
        output[4] = 0.5f;
        output[31] = -0.25f;
        return NnueNetwork.of(1, bias, features, hiddenBias, hidden, -0.5f, output);
    }

    /**
     * Independent oracle: FEN characters instead of Board bitplanes, rank/file arithmetic
     * instead of XOR orientation, character vocabulary instead of Piece/schema mapping,
     * unit-outer double sums instead of row additions, and no production activation call.
     */
    private static Oracle referenceFromFen(String fen, NnueNetwork network) {
        String[] fields = fen.split(" ");
        char[] pieces = new char[64];
        int rank = 7;
        int file = 0;
        int whiteKing = -1;
        int blackKing = -1;
        for (char c : fields[0].toCharArray()) {
            if (c == '/') {
                rank--;
                file = 0;
            } else if (c >= '1' && c <= '8') {
                file += c - '0';
            } else {
                int square = rank * 8 + file++;
                pieces[square] = c;
                if (c == 'K') whiteKing = square;
                if (c == 'k') blackKing = square;
            }
        }
        double[][] accumulators = new double[2][64];
        for (int perspective = 0; perspective < 2; perspective++) {
            int king = perspective == 0 ? whiteKing : blackKing;
            int kingFile = king % 8;
            int kingRank = perspective == 0 ? king / 8 : 7 - king / 8;
            for (int unit = 0; unit < 64; unit++) {
                double sum = network.featureBias(unit);
                for (int square = 0; square < 64; square++) {
                    char piece = pieces[square];
                    if (piece == 0) continue;
                    int channel = "PNBRQK".indexOf(Character.toUpperCase(piece));
                    boolean friendly = Character.isUpperCase(piece) == (perspective == 0);
                    if (!friendly) channel += 6;
                    int pieceRank = perspective == 0 ? square / 8 : 7 - square / 8;
                    int index = kingRank * 6144 + kingFile * 768
                            + channel * 64 + pieceRank * 8 + square % 8;
                    sum += network.featureWeight(index, unit);
                }
                accumulators[perspective][unit] = referenceClip(sum);
            }
        }
        int player = fields[1].equals("w") ? 0 : 1;
        double[] hidden = new double[32];
        for (int unit = 0; unit < 32; unit++) {
            double sum = network.hiddenBias(unit);
            for (int input = 0; input < 128; input++) {
                int perspective = input < 64 ? player : 1 - player;
                sum += network.hiddenWeight(unit, input) * accumulators[perspective][input % 64];
            }
            hidden[unit] = referenceClip(sum);
        }
        double raw = network.outputBias();
        for (int unit = 0; unit < 32; unit++) raw += network.outputWeight(unit) * hidden[unit];
        return new Oracle(accumulators, hidden, raw);
    }

    private static double referenceClip(double value) {
        if (value <= 0) return 0;
        if (value >= 1) return 1;
        return value;
    }

    private record Oracle(double[][] accumulators, double[] hidden, double raw) {}
}
