package com.ohinteractive.seedv6.core.move;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.move.MoveIntent.Promotion;
import com.ohinteractive.seedv6.core.util.Piece;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegalMoveResolverTest {

    private final LegalMoveResolver resolver = new LegalMoveResolver();

    @Test
    void resolvesOrdinaryDoublePawnPushAndCapture() {
        final long[] start = Board.startingPosition();
        final long doublePush = assertRoundTrip(start, "e2e4");
        final long[] afterPush = apply(start, doublePush);
        assertEquals(Piece.PAWN, pieceAt(afterPush, "e4"));
        assertEquals(0, pieceAt(afterPush, "e2"));

        final long[] captureBoard = Board.fromFen("4k3/8/8/3p4/4P3/8/8/4K3 w - - 0 1");
        final long capture = assertRoundTrip(captureBoard, "e4d5");
        final long[] afterCapture = apply(captureBoard, capture);
        assertEquals(Piece.PAWN, pieceAt(afterCapture, "d5"));
        assertEquals(0, pieceAt(afterCapture, "e4"));
    }

    @Test
    void resolvesAndAppliesBothWhiteCastles() {
        final long[] board = Board.fromFen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");

        final long[] kingSide = apply(board, assertRoundTrip(board, "e1g1"));
        assertEquals(Piece.KING, pieceAt(kingSide, "g1"));
        assertEquals(Piece.ROOK, pieceAt(kingSide, "f1"));
        assertEquals(0, pieceAt(kingSide, "e1"));
        assertEquals(0, pieceAt(kingSide, "h1"));

        final long[] queenSide = apply(board, assertRoundTrip(board, "e1c1"));
        assertEquals(Piece.KING, pieceAt(queenSide, "c1"));
        assertEquals(Piece.ROOK, pieceAt(queenSide, "d1"));
        assertEquals(0, pieceAt(queenSide, "e1"));
        assertEquals(0, pieceAt(queenSide, "a1"));
    }

    @Test
    void resolvesAndAppliesEnPassant() {
        final long[] board = Board.fromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1");
        final long move = assertRoundTrip(board, "e5d6");
        final long[] child = apply(board, move);

        assertEquals(Piece.PAWN, pieceAt(child, "d6"));
        assertEquals(0, pieceAt(child, "e5"));
        assertEquals(0, pieceAt(child, "d5"));
    }

    @Test
    void resolvesFromTheCompleteEvasionListWhileInCheck() {
        final long[] board = Board.fromFen("4k3/8/8/8/8/8/4r3/4K3 w - - 0 1");
        assertRoundTrip(board, "e1e2");
    }

    @Test
    void resolvesAndAppliesWhiteQuietPromotions() {
        assertPromotions("7k/P7/8/8/8/8/8/7K w - - 0 1", "a7a8", false);
    }

    @Test
    void resolvesAndAppliesWhiteCapturePromotions() {
        assertPromotions("1r5k/P7/8/8/8/8/8/7K w - - 0 1", "a7b8", false);
    }

    @Test
    void resolvesAndAppliesBlackQuietPromotions() {
        assertPromotions("7k/8/8/8/8/8/p7/7K b - - 0 1", "a2a1", true);
    }

    @Test
    void resolvesAndAppliesBlackCapturePromotions() {
        assertPromotions("7k/8/8/8/8/8/p7/1R5K b - - 0 1", "a2b1", true);
    }

    @Test
    void rejectsMissingPromotionChoiceAndIllegalCoordinates() {
        final long[] promotionBoard = Board.fromFen("7k/P7/8/8/8/8/8/7K w - - 0 1");
        assertThrows(
            IllegalArgumentException.class,
            () -> resolver.resolve(promotionBoard, new MoveIntent(square("a7"), square("a8")))
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> resolver.resolve(Board.startingPosition(), new MoveIntent(square("e2"), square("e5")))
        );
    }

    @Test
    void moveIntentValidatesSquaresAndPromotion() {
        assertThrows(IllegalArgumentException.class, () -> new MoveIntent(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new MoveIntent(0, 64));
        assertThrows(NullPointerException.class, () -> new MoveIntent(0, 1, null));
    }

    @Test
    void everyGeneratedMoveInHighMobilityPositionRoundTripsExactly() {
        final long[] board = Board.fromFen(
            "r3k2r/p1ppqpb1/bn2pnp1/2pP4/1p2P3/2N2N2/PPQBBPPP/R3K2R w KQkq - 0 1"
        );
        final long[] legalMoves = legalMoves(board);

        assertTrue(legalMoves.length > 40);
        for(long move : legalMoves) {
            final MoveIntent intent = new MoveIntent(
                Move.fromSquare(move), Move.toSquare(move), Move.promotion(move)
            );
            assertEquals(move, resolver.resolve(board, intent), Move.coordinate(move));
        }
    }

    private long assertRoundTrip(long[] board, String coordinate) {
        final MoveIntent intent = intent(coordinate);
        final long move = resolver.resolve(board, intent);

        assertEquals(intent.fromSquare(), Move.fromSquare(move));
        assertEquals(intent.toSquare(), Move.toSquare(move));
        assertEquals(intent.promotion(), Move.promotion(move));
        assertEquals(coordinate, Move.coordinate(move));
        assertTrue(contains(legalMoves(board), move));
        assertEquals(
            move,
            resolver.resolve(board, new MoveIntent(Move.fromSquare(move), Move.toSquare(move), Move.promotion(move)))
        );
        return move;
    }

    private void assertPromotions(String fen, String coordinatePrefix, boolean black) {
        final long[] board = Board.fromFen(fen);
        assertPromotion(board, coordinatePrefix + "q", Promotion.QUEEN, Piece.QUEEN, black);
        assertPromotion(board, coordinatePrefix + "r", Promotion.ROOK, Piece.ROOK, black);
        assertPromotion(board, coordinatePrefix + "b", Promotion.BISHOP, Piece.BISHOP, black);
        assertPromotion(board, coordinatePrefix + "n", Promotion.KNIGHT, Piece.KNIGHT, black);
    }

    private void assertPromotion(
        long[] board,
        String coordinate,
        Promotion promotion,
        int pieceType,
        boolean black
    ) {
        final long move = assertRoundTrip(board, coordinate);
        assertEquals(promotion, Move.promotion(move));
        final long[] child = apply(board, move);
        assertEquals(pieceType | (black ? 8 : 0), pieceAt(child, coordinate.substring(2, 4)));
    }

    private static long[] apply(long[] board, long move) {
        final long[] child = new long[Board.MAX_BITBOARDS];
        Board.makeMoveInto(
            board[0], board[1], board[2], board[3],
            (int) board[Board.STATUS], board[Board.KEY], move, child
        );
        return child;
    }

    private static long[] legalMoves(long[] board) {
        final long[] moves = new long[256];
        final int count = Gen.genAll(
            board[0], board[1], board[2], board[3],
            (int) board[Board.STATUS], board[Board.KEY], true,
            moves, new long[Board.MAX_BITBOARDS]
        );
        return Arrays.copyOf(moves, count);
    }

    private static boolean contains(long[] moves, long expected) {
        for(long move : moves) {
            if(move == expected) return true;
        }
        return false;
    }

    private static int pieceAt(long[] board, String coordinate) {
        return Board.getSquare(board[0], board[1], board[2], board[3], square(coordinate));
    }

    private static MoveIntent intent(String coordinate) {
        final Promotion promotion = coordinate.length() == 4
            ? Promotion.NONE
            : switch (coordinate.charAt(4)) {
                case 'q' -> Promotion.QUEEN;
                case 'r' -> Promotion.ROOK;
                case 'b' -> Promotion.BISHOP;
                case 'n' -> Promotion.KNIGHT;
                default -> throw new IllegalArgumentException("Unknown test promotion: " + coordinate);
            };
        return new MoveIntent(
            square(coordinate.substring(0, 2)),
            square(coordinate.substring(2, 4)),
            promotion
        );
    }

    private static int square(String coordinate) {
        return (coordinate.charAt(1) - '1') * 8 + coordinate.charAt(0) - 'a';
    }

}
