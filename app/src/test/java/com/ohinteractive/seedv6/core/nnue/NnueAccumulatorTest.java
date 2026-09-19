package com.ohinteractive.seedv6.core.nnue;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;
import org.junit.jupiter.api.Test;

import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.*;

class NnueAccumulatorTest {

    private static final NnueNetwork NETWORK = NnueNetwork.initialized(20260918L);
    private static final String MIDDLEGAME =
            "r3k2r/pppq1ppp/2npbn2/3Np3/2B1P3/2N2Q1P/PPP2PP1/R3K2R w KQkq - 4 12";
    private static final String CASTLING = "r3k2r/8/8/8/8/8/8/R3K2R";

    // Absolute bounds for V1's seeded +/-1/64 float parameters, not arbitrary trained
    // weights or an unlimited number of plies. The 4,929-transition corpus measured
    // 8.75443220139e-8 raw-accumulator and 1.30967237055e-10 output drift. The accumulator
    // limit is four float ULPs at 0.25 (1.19e-7, about 1.36x observed); the output limit is
    // about 1.53x observed. clip01 and tanh are non-expansive; the small dense weights
    // further attenuate error, though float forward rounding is also measured separately.
    // Every component, including negative/clipped-away values, is checked. Each run prints
    // actual maxima into the JUnit XML report rather than assuming these historical values.
    private static final double ACCUMULATOR_TOLERANCE = 0x1.0p-23;
    private static final double OUTPUT_TOLERANCE = 2e-10;

    @Test
    void quietPawnKnightBishopRookAndQueenMovesForBothColours() {
        String[][] fixtures = {
                {Board.FEN_STARTING_POSITION, "e2e4"},
                {Board.FEN_STARTING_POSITION, "g1f3"},
                {"4k3/8/8/8/8/8/8/2B1K3 w - - 0 1", "c1g5"},
                {"4k3/8/8/8/8/8/8/R3K3 w - - 0 1", "a1a4"},
                {"4k3/8/8/8/8/8/8/3QK3 w - - 0 1", "d1d4"},
                {Board.FEN_STARTING_POSITION.replace(" w ", " b "), "e7e5"},
                {Board.FEN_STARTING_POSITION.replace(" w ", " b "), "g8f6"},
                {"2b1k3/8/8/8/8/8/8/4K3 b - - 0 1", "c8g4"},
                {"r3k3/8/8/8/8/8/8/4K3 b - - 0 1", "a8a5"},
                {"3qk3/8/8/8/8/8/8/4K3 b - - 0 1", "d8d5"}
        };
        for (String[] fixture : fixtures) checkFixture(fixture[0], fixture[1], 2);
    }

    @Test
    void capturesOfDifferentTypesIncludeSameTypeColourReplacement() {
        String[][] fixtures = {
                {"4k3/8/8/3n4/4P3/8/8/4K3 w - - 0 1", "e4d5"},
                {"4k3/8/3p4/8/4N3/8/8/4K3 w - - 0 1", "e4d6"},
                {"4k3/8/8/8/8/r7/8/R3K3 w - - 0 1", "a1a3"},
                {"4k3/8/8/8/4p3/3N4/8/4K3 b - - 0 1", "e4d3"},
                {"4k3/8/5b2/8/3R4/8/8/4K3 b - - 0 1", "f6d4"},
                {"4k2q/8/8/8/8/8/8/4K2B b - - 0 1", "h8h1"}
        };
        for (String[] fixture : fixtures) {
            long[] parent = Board.fromFen(fixture[0]);
            long move = legalMove(parent, fixture[1]);
            assertNotEquals(Value.NONE, pieceAt(parent, Move.toSquare(move)));
            checkFixture(fixture[0], fixture[1], 2);
        }
    }

    @Test
    void kingMovesAndCapturesRebuildOnlyTheirOwnPerspective() {
        String[][] fixtures = {
                {"4k3/8/8/8/8/8/8/R3K2R w - - 0 1", "e1f1"},
                {"r3k2r/8/8/8/8/8/8/4K3 b - - 0 1", "e8f8"},
                {"4k3/8/8/8/8/8/3n4/4K3 w - - 0 1", "e1d2"},
                {"4k3/3N4/8/8/8/8/8/4K3 b - - 0 1", "e8d7"}
        };
        for (String[] fixture : fixtures) checkKingPolicy(fixture[0], fixture[1], 2);
    }

    @Test
    void allFourCastlesRefreshKingPerspectiveAndUpdateOppositeKingAndRookRows() {
        checkKingPolicy(CASTLING + " w KQkq - 0 1", "e1g1", 4);
        checkKingPolicy(CASTLING + " w KQkq - 0 1", "e1c1", 4);
        checkKingPolicy(CASTLING + " b KQkq - 0 1", "e8g8", 4);
        checkKingPolicy(CASTLING + " b KQkq - 0 1", "e8c8", 4);
    }

    @Test
    void enPassantBothColoursIncludesTheRemovedPawnsActualSquare() {
        String[][] fixtures = {
                {"4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 2", "e5d6", "d5"},
                {"4k3/8/8/8/3pP3/8/8/4K3 b - e3 0 2", "d4e3", "e4"}
        };
        for (String[] fixture : fixtures) {
            long[] parent = Board.fromFen(fixture[0]);
            long[] child = play(parent, fixture[1]);
            assertEquals(Piece.PAWN, pieceAt(parent, square(fixture[2])) & Piece.TYPE);
            assertEquals(Value.NONE, pieceAt(child, square(fixture[2])));
            checkFixture(fixture[0], fixture[1], 3);
        }
    }

    @Test
    void everyPromotionAndUnderpromotionBothColoursWithAndWithoutCapture() {
        String[] suffixes = {"q", "r", "b", "n"};
        int[] types = {Piece.QUEEN, Piece.ROOK, Piece.BISHOP, Piece.KNIGHT};
        String[][] fixtures = {
                {"7k/P7/8/8/8/8/8/7K w - - 0 1", "a7a8"},
                {"7k/8/8/8/8/8/p7/7K b - - 0 1", "a2a1"},
                {"1r5k/P7/8/8/8/8/8/7K w - - 0 1", "a7b8"},
                {"7k/8/8/8/8/8/p7/1R5K b - - 0 1", "a2b1"}
        };
        for (String[] fixture : fixtures) {
            for (int i = 0; i < suffixes.length; i++) {
                String coordinate = fixture[1] + suffixes[i];
                long[] child = play(Board.fromFen(fixture[0]), coordinate);
                assertEquals(types[i], pieceAt(child, square(coordinate.substring(2, 4))) & Piece.TYPE);
                checkFixture(fixture[0], coordinate, 2);
            }
        }
    }

    @Test
    void nullMovesCopyRawBitsAndUseTheSuppliedSideToMoveWithoutChangingParent() {
        String[] positions = {Board.FEN_STARTING_POSITION,
                "4k3/8/8/8/3pP3/8/8/4K3 b - e3 99 7"};
        for (String fen : positions) {
            long[] board = Board.fromFen(fen);
            long[] before = board.clone();
            long[] child = new long[Board.MAX_BITBOARDS];
            NnueAccumulator parent = rebuilt(board);
            float[] rawBefore = snapshot(parent);
            Board.nullMoveInto(board[0], board[1], board[2], board[3],
                    (int) board[Board.STATUS], board[Board.KEY], child);
            NnueAccumulator next = new NnueAccumulator(NETWORK);
            verifyTransition(board, child, parent, next, new Drift(), "null");
            assertRawBits(rawBefore, next);
            assertRawBits(rawBefore, parent);
            assertArrayEquals(before, board);
            assertEquals(0, changedSquares(board, child));
            assertEquals(Value.INVALID, Board.enPassantSquare((int) child[Board.STATUS]));
            NnueEvaluator evaluator = new NnueEvaluator(NETWORK);
            float original = evaluator.evaluate(board, parent);
            float reversed = evaluator.evaluate(child, next);
            assertNotEquals(original, reversed);
            // The original state is equally valid for the null board's placement.
            assertEquals(reversed, evaluator.evaluate(child, parent));
            assertRawBits(rawBefore, parent);
        }
    }

    @Test
    void legalRookCycleChangesRightsAndClocksButNotPlacementFeatures() {
        long[] original = Board.fromFen("r3k3/8/8/8/8/8/8/4K2R w Kq - 3 8");
        long[] board = original;
        NnueAccumulator start = rebuilt(board);
        NnueAccumulator state = start;
        for (String coordinate : new String[] {"h1h2", "a8a7", "h2h1", "a7a8"}) {
            long[] child = play(board, coordinate);
            NnueAccumulator next = new NnueAccumulator(NETWORK);
            verifyTransition(board, child, state, next, new Drift(), coordinate);
            board = child;
            state = next;
        }
        assertEquals(0, changedSquares(original, board));
        assertFalse(Board.kingSide((int) board[Board.STATUS], Value.WHITE));
        assertFalse(Board.queenSide((int) board[Board.STATUS], Value.BLACK));
        assertEquals(7, Board.halfMoveClock((int) board[Board.STATUS]));
        assertEquals(10, Board.fullMoveNumber((int) board[Board.STATUS]));
        assertNotEquals(original[Board.KEY], board[Board.KEY]);
        assertRawBits(snapshot(start), rebuilt(board));
        // Compare the endpoints of that legal cycle; placement-only update is an exact copy.
        NnueAccumulator endpoint = new NnueAccumulator(NETWORK);
        endpoint.update(original, board, start);
        assertRawBits(snapshot(start), endpoint);
    }

    @Test
    void siblingAndRepeatedDerivationsPreserveParentAndOverwriteReusedDestination() {
        long[] board = Board.startingPosition();
        NnueAccumulator parent = rebuilt(board);
        NnueAccumulator child = new NnueAccumulator(NETWORK);
        float[] before = snapshot(parent);
        for (String move : new String[] {"e2e4", "g1f3", "d2d4", "e2e4"}) {
            verifyTransition(board, play(board, move), parent, child, new Drift(), move);
        }
        float[] first = snapshot(child);
        child.update(board, play(board, "e2e4"), parent);
        assertRawBits(first, child);
        assertRawBits(before, parent);
    }

    @Test
    void fullRebuildPopulatesReusableRawStateAndMatchesDoublePrecisionSums() {
        NnueAccumulator state = new NnueAccumulator(NETWORK);
        NnueEvaluator evaluator = new NnueEvaluator(NETWORK);
        for (String fen : new String[] {Board.FEN_STARTING_POSITION, MIDDLEGAME,
                "8/4k3/8/p7/8/8/8/4K3 b - - 0 1", Board.FEN_STARTING_POSITION}) {
            long[] board = Board.fromFen(fen);
            long[] before = board.clone();
            state.rebuild(board);
            boolean negative = false;
            for (int perspective = 0; perspective < 2; perspective++) {
                int king = kingSquare(board, perspective);
                for (int unit = 0; unit < 64; unit++) {
                    // Unit-outer double sum, all-square discovery, no production row loop.
                    double expected = NETWORK.featureBias(unit);
                    for (int square = 0; square < 64; square++) {
                        int piece = pieceAt(board, square);
                        if (piece != 0) expected += NETWORK.featureWeight(feature(perspective, king, piece, square), unit);
                    }
                    assertEquals(expected, state.raw(perspective, unit), 5e-8);
                    negative |= state.raw(perspective, unit) < 0;
                }
            }
            assertTrue(negative, "Raw oracle must retain negative components");
            float[] rawBefore = snapshot(state);
            assertEquals(evaluator.evaluate(board), evaluator.evaluate(board, state));
            assertRawBits(rawBefore, state);
            assertArrayEquals(before, board);
        }
    }

    @Test
    void rawNegativeAndAboveOneValuesSurviveInferenceAndIncrementalUpdates() {
        float[] bias = new float[64];
        bias[0] = 1.25f;
        bias[1] = -0.25f;
        float[] weights = new float[NnueNetwork.FEATURE_WEIGHT_COUNT];
        int newPawn = NnueFeatureSchema.featureIndex(0, 0, 0, Piece.PAWN, square("a3"));
        weights[newPawn * 64] = -0.5f;
        weights[newPawn * 64 + 1] = 0.5f;
        float[] hidden = new float[NnueNetwork.HIDDEN_WEIGHT_COUNT];
        hidden[0] = 0.5f;
        hidden[1] = 0.25f;
        float[] output = new float[32];
        output[0] = 2;
        NnueNetwork network = NnueNetwork.of(1, bias, weights, new float[32], hidden, 0, output);
        long[] board = Board.fromFen("7k/8/8/8/8/8/P7/K7 w - - 0 1");
        long[] childBoard = play(board, "a2a3");
        NnueAccumulator parent = new NnueAccumulator(network);
        NnueAccumulator child = new NnueAccumulator(network);
        NnueAccumulator oracle = new NnueAccumulator(network);
        parent.rebuild(board);
        float[] before = snapshot(parent);
        NnueEvaluator evaluator = new NnueEvaluator(network);
        assertEquals(1, evaluator.evaluate(board, parent));
        assertEquals(1, evaluator.accumulator(0, 0));
        assertEquals(0, evaluator.accumulator(0, 1));
        assertRawBits(before, parent);
        child.update(board, childBoard, parent);
        assertEquals(0.75f, child.raw(0, 0));
        assertEquals(0.25f, child.raw(0, 1));
        oracle.rebuild(childBoard);
        assertRawBits(snapshot(oracle), child);
        assertEquals(evaluator.evaluate(childBoard), evaluator.evaluate(childBoard, child));
        // Reusing caller state cannot invalidate the evaluator's retained activation/result.
        float retained = evaluator.raw();
        child.rebuild(board);
        assertEquals(retained, evaluator.raw());
        assertEquals(0.75f, evaluator.accumulator(0, 0));
        assertRawBits(before, parent);
    }

    @Test
    void mirroredColourSwappedTransitionsExchangePerspectiveStates() {
        String[][] pairs = {
                {"4k3/8/8/8/8/8/4P3/4K3 w - - 0 1", "e2e4",
                 "4k3/4p3/8/8/8/8/8/4K3 b - - 0 1", "e7e5"},
                {"4k3/8/8/8/8/8/3n4/4K3 w - - 0 1", "e1d2",
                 "4k3/3N4/8/8/8/8/8/4K3 b - - 0 1", "e8d7"},
                {CASTLING + " w KQkq - 0 1", "e1g1",
                 CASTLING + " b KQkq - 0 1", "e8g8"}
        };
        for (String[] pair : pairs) {
            long[] board = Board.fromFen(pair[0]);
            long[] mirror = Board.fromFen(pair[2]);
            long[] child = play(board, pair[1]);
            long[] mirrorChild = play(mirror, pair[3]);
            NnueAccumulator first = new NnueAccumulator(NETWORK);
            NnueAccumulator second = new NnueAccumulator(NETWORK);
            verifyTransition(board, child, rebuilt(board), first, new Drift(), pair[1]);
            verifyTransition(mirror, mirrorChild, rebuilt(mirror), second, new Drift(), pair[3]);
            for (int perspective = 0; perspective < 2; perspective++) {
                for (int unit = 0; unit < 64; unit++) {
                    assertEquals(first.raw(perspective, unit), second.raw(1 ^ perspective, unit),
                            ACCUMULATOR_TOLERANCE);
                }
            }
            NnueEvaluator evaluator = new NnueEvaluator(NETWORK);
            assertEquals(evaluator.evaluate(child, first), evaluator.evaluate(mirrorChild, second),
                    OUTPUT_TOLERANCE); // Corresponding STM order, not a sign-negation claim.
        }
    }

    @Test
    void invalidStatesBoardsNetworksAndAliasingAreRejectedBeforeDestinationMutation() {
        long[] board = Board.startingPosition();
        long[] childBoard = play(board, "e2e4");
        NnueAccumulator parent = rebuilt(board);
        NnueAccumulator destination = rebuilt(childBoard);
        float[] parentBefore = snapshot(parent);
        float[] before = snapshot(destination);
        NnueAccumulator empty = new NnueAccumulator(NETWORK);
        assertThrows(NullPointerException.class, () -> new NnueAccumulator(null));
        assertThrows(IllegalStateException.class, () -> empty.raw(0, 0));
        assertThrows(IllegalStateException.class, () -> destination.update(board, childBoard, empty));
        assertThrows(IllegalArgumentException.class, () -> parent.update(board, childBoard, parent));
        assertThrows(IllegalArgumentException.class, () -> destination.update(childBoard, board, parent));
        assertThrows(IllegalArgumentException.class, () -> parent.raw(2, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> parent.raw(0, 64));
        assertThrows(NullPointerException.class, () -> destination.rebuild(null));
        assertThrows(NullPointerException.class, () -> destination.update(board, childBoard, null));
        for (int length = 0; length < Board.MAX_BITBOARDS; length++) {
            long[] shortBoard = new long[length];
            assertThrows(IllegalArgumentException.class, () -> destination.rebuild(shortBoard));
            assertThrows(IllegalArgumentException.class, () -> destination.update(shortBoard, childBoard, parent));
            assertThrows(IllegalArgumentException.class, () -> destination.update(board, shortBoard, parent));
        }
        String[] invalidKings = {
                "7k/8/8/8/8/8/8/8 w - - 0 1", "7k/8/8/8/8/8/8/KK6 w - - 0 1",
                "8/8/8/8/8/8/8/K7 w - - 0 1", "6kk/8/8/8/8/8/8/K7 w - - 0 1"
        };
        for (String fen : invalidKings) {
            long[] invalid = Board.fromFen(fen);
            assertThrows(IllegalArgumentException.class, () -> destination.rebuild(invalid));
            assertThrows(IllegalArgumentException.class, () -> destination.update(board, invalid, parent));
        }
        long[] colourOnEmpty = board.clone();
        colourOnEmpty[3] |= 1L << square("a4");
        long[] unknownType = board.clone();
        for (int plane = 0; plane < 3; plane++) unknownType[plane] |= 1L << square("a4");
        for (long[] invalid : new long[][] {colourOnEmpty, unknownType}) {
            assertThrows(IllegalArgumentException.class, () -> destination.rebuild(invalid));
            assertThrows(IllegalArgumentException.class, () -> destination.update(board, invalid, parent));
        }
        // Even equal parameters in a different immutable object are a different binding.
        NnueNetwork different = NnueNetwork.initialized(20260918L);
        NnueAccumulator other = new NnueAccumulator(different);
        other.rebuild(board);
        assertThrows(IllegalArgumentException.class, () -> destination.update(board, childBoard, other));
        NnueEvaluator evaluator = new NnueEvaluator(NETWORK);
        evaluator.evaluate(board, parent);
        assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate(board, other));
        assertThrows(IllegalStateException.class, evaluator::raw);
        assertThrows(IllegalStateException.class, () -> evaluator.evaluate(board, empty));
        assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate(childBoard, parent));
        assertThrows(NullPointerException.class, () -> evaluator.evaluate(board, null));
        assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate(new long[5], parent));
        assertRawBits(before, destination);
        assertRawBits(parentBefore, parent);
    }

    @Test
    void deterministicLongLegalSequencesMeasureCumulativeDriftAfterEveryTransition() {
        String[] starts = {Board.FEN_STARTING_POSITION, MIDDLEGAME,
                "8/4k3/2p2p2/3p4/3P4/2P2P2/4K3/8 w - - 0 1",
                "7k/P7/8/8/8/8/7p/K7 w - - 0 1"};
        Drift drift = new Drift();
        int captures = 0, checks = 0, kingMoves = 0, promotions = 0;
        for (int start = 0; start < starts.length; start++) {
            for (int run = 0; run < 4; run++) {
                SplittableRandom random = new SplittableRandom(0x5eedbL + 31L * start + run);
                long[] board = Board.fromFen(starts[start]);
                long[] child = new long[Board.MAX_BITBOARDS];
                long[] scratch = new long[Board.MAX_BITBOARDS];
                long[] moves = new long[256];
                NnueAccumulator parent = rebuilt(board);
                NnueAccumulator next = new NnueAccumulator(NETWORK);
                for (int ply = 0; ply < 384; ply++) {
                    int count = generate(board, moves, scratch);
                    if (count == 0) break;
                    long move = moves[random.nextInt(count)];
                    // Ensure available promotions are exercised; dedicated fixtures cover all types.
                    for (int i = 0; i < count; i++) {
                        if (((moves[i] >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS) != 0) {
                            move = moves[i];
                            break;
                        }
                    }
                    int type = pieceAt(board, Move.fromSquare(move)) & Piece.TYPE;
                    if (pieceAt(board, Move.toSquare(move)) != 0 || Move.moveType(move) == Move.EN_PASSANT) captures++;
                    if (type == Piece.KING) kingMoves++;
                    if (((move >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS) != 0) promotions++;
                    makeMove(board, move, child);
                    int player = Board.player((int) child[Board.STATUS]);
                    if (Board.isPlayerInCheckPext(child[0], child[1], child[2], child[3], player)) checks++;
                    verifyTransition(board, child, parent, next, drift,
                            "start=" + start + " run=" + run + " ply=" + ply);
                    long[] swapBoard = board;
                    board = child;
                    child = swapBoard;
                    NnueAccumulator swapState = parent;
                    parent = next;
                    next = swapState;
                }
            }
        }
        // Legal transition stress continues through draw-by-rule positions; it creates no
        // game history and is not a self-play/search policy. Stop only on no legal moves/cap.
        assertTrue(drift.transitions >= 4000, "Corpus unexpectedly shortened");
        assertTrue(captures > 100);
        assertTrue(checks > 100);
        assertTrue(kingMoves > 1000);
        assertTrue(promotions >= 8);
        System.out.printf(java.util.Locale.ROOT,
                "NNUE drift: transitions=%d accumulator=%.12g raw=%.12g bounded=%.12g "
                        + "captures=%d checks=%d kingMoves=%d promotions=%d%n",
                drift.transitions, drift.accumulator, drift.raw, drift.bounded,
                captures, checks, kingMoves, promotions);
    }

    private static void checkFixture(String fen, String coordinate, int changedCount) {
        long[] parent = Board.fromFen(fen);
        long[] child = play(parent, coordinate);
        assertEquals(changedCount, Long.bitCount(changedSquares(parent, child)), coordinate);
        verifyTransition(parent, child, rebuilt(parent), new NnueAccumulator(NETWORK), new Drift(), coordinate);
    }

    private static void checkKingPolicy(String fen, String coordinate, int changedCount) {
        checkFixture(fen, coordinate, changedCount);
        long[] parentBoard = Board.fromFen(fen);
        long[] childBoard = play(parentBoard, coordinate);
        int moving = Board.player((int) parentBoard[Board.STATUS]);
        int other = 1 ^ moving;
        NnueAccumulator parent = rebuilt(parentBoard);
        NnueAccumulator child = new NnueAccumulator(NETWORK);
        child.update(parentBoard, childBoard, parent);
        NnueAccumulator oracle = rebuilt(childBoard);
        boolean otherRetainedDeltaRounding = false;
        // Exact own-perspective oracle identity establishes refresh. For the opposite
        // perspective, calculate a scalar delta reference over all 64 squares (independent
        // discovery) and require rounding evidence distinguishing it from a full rebuild.
        int king = kingSquare(parentBoard, other);
        for (int unit = 0; unit < 64; unit++) {
            assertEquals(oracle.raw(moving, unit), child.raw(moving, unit));
            float delta = parent.raw(other, unit);
            for (int square = 0; square < 64; square++) {
                int oldPiece = pieceAt(parentBoard, square);
                int newPiece = pieceAt(childBoard, square);
                if (oldPiece == newPiece) continue;
                if (oldPiece != 0) delta -= NETWORK.featureWeight(feature(other, king, oldPiece, square), unit);
                if (newPiece != 0) delta += NETWORK.featureWeight(feature(other, king, newPiece, square), unit);
            }
            assertEquals(delta, child.raw(other, unit), coordinate);
            otherRetainedDeltaRounding |= Float.floatToRawIntBits(delta)
                    != Float.floatToRawIntBits(oracle.raw(other, unit));
        }
        assertTrue(otherRetainedDeltaRounding, "Fixture must distinguish opposite delta from rebuild: " + coordinate);
    }

    private static void verifyTransition(long[] parentBoard, long[] childBoard, NnueAccumulator parent,
                                         NnueAccumulator child, Drift drift, String label) {
        long[] parentBefore = parentBoard.clone();
        long[] childBefore = childBoard.clone();
        float[] rawBefore = snapshot(parent);
        child.update(parentBoard, childBoard, parent);
        NnueAccumulator oracle = rebuilt(childBoard);
        for (int perspective = 0; perspective < 2; perspective++) {
            for (int unit = 0; unit < 64; unit++) {
                double error = Math.abs((double) oracle.raw(perspective, unit) - child.raw(perspective, unit));
                drift.accumulator = Math.max(drift.accumulator, error);
                assertEquals(oracle.raw(perspective, unit), child.raw(perspective, unit),
                        ACCUMULATOR_TOLERANCE, label);
            }
        }
        float[] childRaw = snapshot(child);
        NnueEvaluator full = new NnueEvaluator(NETWORK);
        NnueEvaluator incremental = new NnueEvaluator(NETWORK);
        full.evaluate(childBoard);
        incremental.evaluate(childBoard, child);
        drift.raw = Math.max(drift.raw, Math.abs((double) full.raw() - incremental.raw()));
        drift.bounded = Math.max(drift.bounded, Math.abs(full.boundedValue() - incremental.boundedValue()));
        assertEquals(full.raw(), incremental.raw(), OUTPUT_TOLERANCE, label);
        assertEquals(full.boundedValue(), incremental.boundedValue(), OUTPUT_TOLERANCE, label);
        assertRawBits(childRaw, child);
        assertRawBits(rawBefore, parent);
        assertArrayEquals(parentBefore, parentBoard, label);
        assertArrayEquals(childBefore, childBoard, label);
        drift.transitions++;
    }

    private static NnueAccumulator rebuilt(long[] board) {
        NnueAccumulator state = new NnueAccumulator(NETWORK);
        state.rebuild(board);
        return state;
    }

    private static float[] snapshot(NnueAccumulator state) {
        float[] result = new float[128];
        for (int p = 0; p < 2; p++) {
            for (int i = 0; i < 64; i++) result[p * 64 + i] = state.raw(p, i);
        }
        return result;
    }

    private static void assertRawBits(float[] expected, NnueAccumulator actual) {
        for (int p = 0; p < 2; p++) {
            for (int i = 0; i < 64; i++) {
                assertEquals(Float.floatToRawIntBits(expected[p * 64 + i]),
                        Float.floatToRawIntBits(actual.raw(p, i)));
            }
        }
    }

    private static long[] play(long[] board, String coordinate) {
        long[] child = new long[Board.MAX_BITBOARDS];
        makeMove(board, legalMove(board, coordinate), child);
        return child;
    }

    private static long legalMove(long[] board, String coordinate) {
        long[] moves = new long[256];
        int count = generate(board, moves, new long[Board.MAX_BITBOARDS]);
        for (int i = 0; i < count; i++) {
            if (Move.coordinate(moves[i]).equals(coordinate)) return moves[i];
        }
        throw new AssertionError("No generated legal move " + coordinate);
    }

    private static int generate(long[] board, long[] moves, long[] scratch) {
        return Gen.genAll(board[0], board[1], board[2], board[3],
                (int) board[Board.STATUS], board[Board.KEY], true, moves, scratch);
    }

    private static void makeMove(long[] board, long move, long[] child) {
        long[] before = board.clone();
        Board.makeMoveInto(board[0], board[1], board[2], board[3],
                (int) board[Board.STATUS], board[Board.KEY], move, child);
        assertArrayEquals(before, board);
    }

    private static int square(String coordinate) {
        return (coordinate.charAt(1) - '1') * 8 + coordinate.charAt(0) - 'a';
    }

    private static int pieceAt(long[] board, int square) {
        return Board.getSquare(board[0], board[1], board[2], board[3], square);
    }

    private static long changedSquares(long[] parent, long[] child) {
        long mask = 0;
        for (int square = 0; square < 64; square++) {
            if (pieceAt(parent, square) != pieceAt(child, square)) mask |= 1L << square;
        }
        return mask;
    }

    private static int kingSquare(long[] board, int colour) {
        for (int square = 0; square < 64; square++) {
            if (pieceAt(board, square) == (Piece.KING | colour << Board.PLAYER_SHIFT)) return square;
        }
        throw new AssertionError("Missing king");
    }

    private static int feature(int perspective, int king, int piece, int square) {
        return NnueFeatureSchema.featureIndex(perspective, king,
                piece >>> Board.PLAYER_SHIFT, piece & Piece.TYPE, square);
    }

    private static final class Drift {
        int transitions;
        double accumulator, raw, bounded;
    }
}
