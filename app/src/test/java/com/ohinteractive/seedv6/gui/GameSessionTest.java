package com.ohinteractive.seedv6.gui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.move.MoveIntent.Promotion;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;

class GameSessionTest {

    @Test
    void ordinaryMoveUsesExactGeneratedMoveAndIllegalIntentIsTransactional() {
        final GameSession session = GameSession.startingPosition();
        final long[] before = session.boardSnapshot();
        final int historyBefore = session.historySize();
        assertThrows(IllegalArgumentException.class,
            () -> session.applyIntent(square("e2"), square("e5"), Promotion.NONE));
        assertArrayEquals(before, session.boardSnapshot());
        assertEquals(historyBefore, session.historySize());

        final long expected = exactMove(session, "e2e4");
        final long applied = session.applyIntent(square("e2"), square("e4"), Promotion.NONE);
        assertEquals(expected, applied);
        assertEquals(Piece.PAWN, pieceAt(session, "e4"));
        assertEquals(0, pieceAt(session, "e2"));
        assertEquals(2, session.historySize());
        assertEquals(List.of("e2e4"), session.moveHistory());
        session.historySnapshot().requireCurrent(session.boardSnapshot());
    }

    @Test
    void allFourCastlesResolveAndApplyAsGenerated() {
        assertCastle(Value.WHITE, "e1g1", "g1", "f1");
        assertCastle(Value.WHITE, "e1c1", "c1", "d1");
        assertCastle(Value.BLACK, "e8g8", "g8", "f8");
        assertCastle(Value.BLACK, "e8c8", "c8", "d8");
    }

    @Test
    void enPassantUsesExactMoveAndKeepsHistorySynchronized() {
        final GameSession session = GameSession.fromFen(
            "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1"
        );
        final long expected = exactMove(session, "e5d6");
        final long applied = session.applyIntent(square("e5"), square("d6"), Promotion.NONE);
        assertEquals(expected, applied);
        assertEquals(Piece.PAWN, pieceAt(session, "d6"));
        assertEquals(0, pieceAt(session, "d5"));
        assertEquals(2, session.historySize());
        session.historySnapshot().requireCurrent(session.boardSnapshot());
    }

    @Test
    void allPromotionChoicesWorkForBothColoursQuietAndCapturing() {
        for(Promotion promotion : List.of(
            Promotion.QUEEN, Promotion.ROOK, Promotion.BISHOP, Promotion.KNIGHT
        )) {
            assertPromotion("7k/P7/8/8/8/8/8/7K w - - 0 1", "a7a8", promotion, Value.WHITE);
            assertPromotion("1r5k/P7/8/8/8/8/8/7K w - - 0 1", "a7b8", promotion, Value.WHITE);
            assertPromotion("7k/8/8/8/8/8/p7/7K b - - 0 1", "a2a1", promotion, Value.BLACK);
            assertPromotion("7k/8/8/8/8/8/p7/1R5K b - - 0 1", "a2b1", promotion, Value.BLACK);
        }
    }

    @Test
    void checkMateStalemateAndAcceptedRuleDrawsUseAuthoritativeFacilities() {
        final GameSession check = GameSession.fromFen(
            "4k3/8/8/8/8/8/4r3/4K3 w - - 0 1"
        );
        assertTrue(check.status().inCheck());
        assertEquals(PositionStatus.Outcome.ACTIVE, check.status().outcome());

        assertEquals(PositionStatus.Outcome.CHECKMATE, GameSession.fromFen(
            "7k/6Q1/6K1/8/8/8/8/8 b - - 0 1"
        ).status().outcome());
        assertEquals(PositionStatus.Outcome.STALEMATE, GameSession.fromFen(
            "7k/5Q2/6K1/8/8/8/8/8 b - - 0 1"
        ).status().outcome());
        assertEquals(PositionStatus.Outcome.FIFTY_MOVE_DRAW, GameSession.fromFen(
            "4k3/8/8/8/8/8/8/R3K3 w - - 100 1"
        ).status().outcome());
        assertEquals(PositionStatus.Outcome.INSUFFICIENT_MATERIAL_DRAW, GameSession.fromFen(
            "4k3/8/8/8/8/8/8/4K3 w - - 0 1"
        ).status().outcome());
    }

    @Test
    void formalThreefoldIsRecognizedFromTheCurrentGameHistory() {
        final GameSession session = GameSession.startingPosition();
        for(int cycle = 0; cycle < 2; cycle ++) {
            apply(session, "g1f3");
            apply(session, "g8f6");
            apply(session, "f3g1");
            apply(session, "f6g8");
        }
        assertEquals(PositionStatus.Outcome.THREEFOLD_DRAW, session.status().outcome());
        assertEquals(9, session.historySize());
        session.historySnapshot().requireCurrent(session.boardSnapshot());
    }

    @Test
    void generatedMoveApplicationRejectsForeignValuesBeforeMutation() {
        final GameSession session = GameSession.startingPosition();
        final long[] before = session.boardSnapshot();
        assertThrows(IllegalArgumentException.class, () -> session.applyGeneratedMove(0L));
        assertArrayEquals(before, session.boardSnapshot());
        assertFalse(session.status().terminal());
    }

    private static void assertCastle(int side, String coordinate, String kingSquare, String rookSquare) {
        final String fen = "r3k2r/8/8/8/8/8/8/R3K2R "
            + (side == Value.WHITE ? "w" : "b") + " KQkq - 0 1";
        final GameSession session = GameSession.fromFen(fen);
        final long expected = exactMove(session, coordinate);
        final long applied = session.applyIntent(
            square(coordinate.substring(0, 2)), square(coordinate.substring(2, 4)), Promotion.NONE
        );
        assertEquals(expected, applied);
        final int color = side << 3;
        assertEquals(Piece.KING | color, pieceAt(session, kingSquare));
        assertEquals(Piece.ROOK | color, pieceAt(session, rookSquare));
        session.historySnapshot().requireCurrent(session.boardSnapshot());
    }

    private static void assertPromotion(
        String fen, String prefix, Promotion promotion, int side
    ) {
        final GameSession session = GameSession.fromFen(fen);
        assertEquals(
            List.of(Promotion.QUEEN, Promotion.ROOK, Promotion.BISHOP, Promotion.KNIGHT),
            session.promotionChoices(square(prefix.substring(0, 2)), square(prefix.substring(2, 4)))
        );
        final String coordinate = prefix + switch(promotion) {
            case QUEEN -> "q";
            case ROOK -> "r";
            case BISHOP -> "b";
            case KNIGHT -> "n";
            case NONE -> throw new AssertionError();
        };
        final long expected = exactMove(session, coordinate);
        final long applied = session.applyIntent(
            square(prefix.substring(0, 2)), square(prefix.substring(2, 4)), promotion
        );
        assertEquals(expected, applied);
        assertEquals(promotionType(promotion) | (side << 3), pieceAt(session, prefix.substring(2, 4)));
        assertEquals(coordinate, session.moveHistory().getFirst());
    }

    private static int promotionType(Promotion promotion) {
        return switch(promotion) {
            case QUEEN -> Piece.QUEEN;
            case ROOK -> Piece.ROOK;
            case BISHOP -> Piece.BISHOP;
            case KNIGHT -> Piece.KNIGHT;
            case NONE -> throw new AssertionError();
        };
    }

    private static void apply(GameSession session, String coordinate) {
        session.applyIntent(
            square(coordinate.substring(0, 2)), square(coordinate.substring(2, 4)), Promotion.NONE
        );
    }

    private static long exactMove(GameSession session, String coordinate) {
        for(long move : session.legalMoves()) {
            if(Move.coordinate(move).equals(coordinate)) return move;
        }
        throw new AssertionError("Missing generated move " + coordinate);
    }

    private static int pieceAt(GameSession session, String coordinate) {
        final long[] board = session.boardSnapshot();
        return Board.getSquare(board[0], board[1], board[2], board[3], square(coordinate));
    }

    private static int square(String coordinate) {
        return (coordinate.charAt(1) - '1') * 8 + coordinate.charAt(0) - 'a';
    }
}
