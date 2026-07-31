package com.ohinteractive.seedv6.core;

import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class MoveTypeExperimentTest {

    private static final int MODE_ALL = 0;
    private static final int MODE_TACTICAL = 1;
    private static final int MODE_QUIET = 2;
    private static final int MODE_EVASION = 3;

    private static final String START = Board.FEN_STARTING_POSITION;
    private static final String KIWIPETE =
        "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";
    private static final String EN_PASSANT =
        "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1";
    private static final String PROMOTIONS =
        "r3k2r/1P4P1/8/8/8/8/1p4p1/R3K2R w KQkq - 0 1";
    private static final String WHITE_IN_CHECK =
        "4k3/8/8/8/8/8/4r3/4K3 w - - 0 1";
    private static final String BLACK_IN_CHECK =
        "4k3/4R3/8/8/8/8/8/4K3 b - - 0 1";

    @Test
    void packedLayoutUsesTheRequestedLongMasks() {
        assertEquals(24, Move.MOVE_TYPE_SHIFT);
        assertEquals(0x0700_0000L, Move.MOVE_TYPE_MASK);
        assertEquals(0x0800_0000L, Move.UNUSED_MOVE_BIT_MASK);
        assertEquals(28, Move.CASTLING_CHANGE_SHIFT);
        assertEquals(0xf000_0000L, Move.CASTLING_CHANGE_MASK);
        assertEquals(0x1000_0000L, Move.WHITE_KINGSIDE_CHANGE_MASK);
        assertEquals(0x2000_0000L, Move.WHITE_QUEENSIDE_CHANGE_MASK);
        assertEquals(0x4000_0000L, Move.BLACK_KINGSIDE_CHANGE_MASK);
        assertEquals(0x8000_0000L, Move.BLACK_QUEENSIDE_CHANGE_MASK);
        assertEquals(0L, Move.MOVE_TYPE_MASK & Move.UNUSED_MOVE_BIT_MASK);
        assertEquals(0L, Move.CASTLING_CHANGE_MASK & Move.UNUSED_MOVE_BIT_MASK);

        assertEquals(0, Move.QUIET);
        assertEquals(1, Move.PAWN_PUSH);
        assertEquals(2, Move.PAWN_DOUBLE_PUSH);
        assertEquals(3, Move.CASTLE);
        assertEquals(4, Move.CAPTURE);
        assertEquals(5, Move.EN_PASSANT);
        assertEquals(6, Move.PROMOTION);
        assertEquals(7, Move.CAPTURE_PROMOTION);

        long scoreAndMove = 0x5a5a_5a5a_ffff_ffffL;
        assertEquals(0x5a5a_5a5a_08ff_ffffL, Move.withoutExperimentalMetadata(scoreAndMove));
    }

    @Test
    void generatedMovesMatchProductionInEveryGenerationMode() {
        String[] fens = { START, KIWIPETE, EN_PASSANT, PROMOTIONS, WHITE_IN_CHECK, BLACK_IN_CHECK };
        for(String fen : fens) {
            assertGeneratedEquivalent(fen, MODE_ALL);
            assertGeneratedEquivalent(fen, MODE_TACTICAL);
            assertGeneratedEquivalent(fen, MODE_QUIET);
        }
        assertGeneratedEquivalent(WHITE_IN_CHECK, MODE_EVASION);
        assertGeneratedEquivalent(BLACK_IN_CHECK, MODE_EVASION);
    }

    @Test
    void generatedMetadataMatchesBoardMechanics() {
        String[] fens = { START, KIWIPETE, EN_PASSANT, PROMOTIONS, WHITE_IN_CHECK, BLACK_IN_CHECK };
        int seenTypes = 0;
        for(String fen : fens) {
            long[] board = Board.fromFen(fen);
            long[] moves = new long[256];
            int count = generateExperimental(board, moves, MODE_ALL);
            for(int i = 0; i < count; i ++) {
                long move = moves[i];
                int expectedType = expectedMoveType(board, move);
                int expectedChange = expectedCastlingChange(move);
                assertEquals(expectedType, Move.moveType(move), fen + " " + Move.string(move));
                assertEquals(expectedChange, Move.castlingChange(move), fen + " " + Move.string(move));
                assertEquals(0L, move & Move.UNUSED_MOVE_BIT_MASK, fen + " " + Move.string(move));
                assertEquals(0L, move >>> 32, "metadata sign-extended into score: " + Move.string(move));
                seenTypes |= 1 << Move.moveType(move);
            }
        }
        assertEquals(0xff, seenTypes);
    }

    @Test
    void boardStateMatchesProductionForEveryGeneratedMove() {
        String[] fens = { START, KIWIPETE, EN_PASSANT, PROMOTIONS, WHITE_IN_CHECK, BLACK_IN_CHECK };
        for(String fen : fens) {
            long[] board = Board.fromFen(fen);
            long[] typedBoard = BoardMoveType.fromFen(fen);
            assertArrayEquals(board, typedBoard);
            long[] moves = new long[256];
            int count = generateExperimental(board, moves, MODE_ALL);
            for(int i = 0; i < count; i ++) {
                assertMoveApplicationEquals(board, typedBoard, moves[i], fen + " " + Move.string(moves[i]));
            }
        }
    }

    @Test
    void destinationBoardMayAliasTheSourceArray() {
        long[] source = BoardMoveType.startingPosition();
        long[] moves = new long[256];
        int count = generateExperimental(source, moves, MODE_ALL);
        long move = findMove(moves, count, "e2e4");
        long[] expected = new long[Board.MAX_BITBOARDS];
        applyExperimental(source, move, expected);
        applyExperimental(source, move, source);
        assertArrayEquals(expected, source);
    }

    @Test
    void targetedMoveTypesAndCastlingChangesAreEncoded() {
        assertTarget(START, "g1f3", Move.QUIET, 0);
        assertTarget(START, "e2e3", Move.PAWN_PUSH, 0);
        assertTarget(START, "e2e4", Move.PAWN_DOUBLE_PUSH, 0);
        assertTarget("4k3/8/3p4/8/4N3/8/8/4K3 w - - 0 1", "e4d6", Move.CAPTURE, 0);
        assertTarget(EN_PASSANT, "e5d6", Move.EN_PASSANT, 0);

        for(char promotion : new char[] { 'Q', 'R', 'B', 'N' }) {
            assertTarget("4k3/P7/8/8/8/8/8/4K3 w - - 0 1", "a7a8" + promotion, Move.PROMOTION, 0);
            assertTarget("1r2k3/P7/8/8/8/8/8/4K3 w - - 0 1", "a7b8" + promotion, Move.CAPTURE_PROMOTION, 0);
        }

        assertTarget("4k3/8/8/8/8/8/8/R3K2R w KQ - 0 1", "e1g1", Move.CASTLE, 0b0011);
        assertTarget("4k3/8/8/8/8/8/8/R3K2R w KQ - 0 1", "e1c1", Move.CASTLE, 0b0011);
        assertTarget("r3k2r/8/8/8/8/8/8/4K3 b kq - 0 1", "e8g8", Move.CASTLE, 0b1100);
        assertTarget("r3k2r/8/8/8/8/8/8/4K3 b kq - 0 1", "e8c8", Move.CASTLE, 0b1100);

        assertTarget("4k3/8/8/8/8/8/8/4K3 w K - 0 1", "e1e2", Move.QUIET, 0b0011);
        assertTarget("4k3/8/8/8/8/8/8/4K3 b k - 0 1", "e8e7", Move.QUIET, 0b1100);
        assertTarget("4k3/8/8/8/8/8/8/R3K2R w KQ - 0 1", "a1a2", Move.QUIET, 0b0010);
        assertTarget("4k3/8/8/8/8/8/8/R3K2R w KQ - 0 1", "h1h2", Move.QUIET, 0b0001);
        assertTarget("r3k2r/8/8/8/8/8/8/4K3 b kq - 0 1", "a8a7", Move.QUIET, 0b1000);
        assertTarget("r3k2r/8/8/8/8/8/8/4K3 b kq - 0 1", "h8h7", Move.QUIET, 0b0100);

        assertTarget("4k3/8/8/8/8/8/1b6/R3K3 b Q - 0 1", "b2a1", Move.CAPTURE, 0b0010);
        assertTarget("4k3/8/8/8/8/8/6b1/4K2R b K - 0 1", "g2h1", Move.CAPTURE, 0b0001);
        assertTarget("r3k3/1B6/8/8/8/8/8/4K3 w q - 0 1", "b7a8", Move.CAPTURE, 0b1000);
        assertTarget("4k2r/6B1/8/8/8/8/8/4K3 w k - 0 1", "g7h8", Move.CAPTURE, 0b0100);

        // The encoded white queenside removal is harmless here because that right is already absent.
        assertTarget("4k3/8/8/8/8/8/8/4K3 w K - 0 1", "e1e2", Move.QUIET, 0b0011);
        assertTarget("4k3/8/8/8/8/8/8/4K2R w - - 0 1", "h1h2", Move.QUIET, 0b0001);
    }

    @Test
    void deterministicSequencesStayIdenticalAndExerciseEveryType() {
        String[] fens = {
            START, START, START, KIWIPETE, KIWIPETE, EN_PASSANT,
            "4k3/P7/8/8/8/8/8/4K3 w - - 0 1",
            "1r2k3/P7/8/8/8/8/8/4K3 w - - 0 1"
        };
        int[] firstTypes = {
            Move.QUIET, Move.PAWN_PUSH, Move.PAWN_DOUBLE_PUSH, Move.CASTLE,
            Move.CAPTURE, Move.EN_PASSANT, Move.PROMOTION, Move.CAPTURE_PROMOTION
        };
        SplittableRandom random = new SplittableRandom(0x5eed_6d6f_7665_7479L);
        int seenTypes = 0;
        for(int scenario = 0; scenario < fens.length; scenario ++) {
            long[] board = Board.fromFen(fens[scenario]);
            long[] typedBoard = BoardMoveType.fromFen(fens[scenario]);
            for(int ply = 0; ply < 16; ply ++) {
                long[] moves = new long[256];
                int count = generateExperimental(typedBoard, moves, MODE_ALL);
                if(count == 0) break;
                int moveIndex = ply == 0
                    ? findMoveType(moves, count, firstTypes[scenario])
                    : random.nextInt(count);
                assertNotEquals(-1, moveIndex, "missing type " + firstTypes[scenario] + " in " + fens[scenario]);
                long move = moves[moveIndex];
                seenTypes |= 1 << Move.moveType(move);
                long[] nextBoard = new long[Board.MAX_BITBOARDS];
                long[] nextTypedBoard = new long[Board.MAX_BITBOARDS];
                applyProduction(board, Move.withoutExperimentalMetadata(move), nextBoard);
                applyExperimental(typedBoard, move, nextTypedBoard);
                assertArrayEquals(nextBoard, nextTypedBoard, "scenario " + scenario + " ply " + ply);
                board = nextBoard;
                typedBoard = nextTypedBoard;
            }
        }
        assertEquals(0xff, seenTypes);
    }

    @Test
    void experimentalPerftMatchesProduction() {
        assertPerft(START, 4, 197_281L);
        assertPerft(KIWIPETE, 3, 97_862L);
        assertPerft(EN_PASSANT, 4, -1L);
    }

    private static void assertGeneratedEquivalent(String fen, int mode) {
        long[] board = Board.fromFen(fen);
        long[] production = new long[256];
        long[] experimental = new long[256];
        int productionCount = generateProduction(board, production, mode);
        int experimentalCount = generateExperimental(board, experimental, mode);
        assertEquals(productionCount, experimentalCount, fen + " mode=" + mode);
        for(int i = 0; i < productionCount; i ++) {
            assertEquals(
                production[i], Move.withoutExperimentalMetadata(experimental[i]),
                fen + " mode=" + mode + " index=" + i
            );
            assertEquals(0L, experimental[i] & Move.UNUSED_MOVE_BIT_MASK);
        }
    }

    private static int generateProduction(long[] board, long[] moves, int mode) {
        int status = (int) board[Board.STATUS];
        long[] scratch = new long[Board.MAX_BITBOARDS];
        return switch(mode) {
            case MODE_ALL -> Gen.genAll(board[0], board[1], board[2], board[3], status, board[Board.KEY], true, moves, scratch);
            case MODE_TACTICAL -> Gen.genTactical(board[0], board[1], board[2], board[3], status, board[Board.KEY], true, moves, scratch);
            case MODE_QUIET -> Gen.genQuiet(board[0], board[1], board[2], board[3], status, board[Board.KEY], true, moves, scratch);
            case MODE_EVASION -> Gen.genEvasion(board[0], board[1], board[2], board[3], status, board[Board.KEY], true, checkers(board), moves, scratch);
            default -> throw new IllegalArgumentException("mode");
        };
    }

    private static int generateExperimental(long[] board, long[] moves, int mode) {
        int status = (int) board[Board.STATUS];
        long[] scratch = new long[Board.MAX_BITBOARDS];
        return switch(mode) {
            case MODE_ALL -> GenMoveType.genAll(board[0], board[1], board[2], board[3], status, board[Board.KEY], true, moves, scratch);
            case MODE_TACTICAL -> GenMoveType.genTactical(board[0], board[1], board[2], board[3], status, board[Board.KEY], true, moves, scratch);
            case MODE_QUIET -> GenMoveType.genQuiet(board[0], board[1], board[2], board[3], status, board[Board.KEY], true, moves, scratch);
            case MODE_EVASION -> GenMoveType.genEvasion(board[0], board[1], board[2], board[3], status, board[Board.KEY], true, checkers(board), moves, scratch);
            default -> throw new IllegalArgumentException("mode");
        };
    }

    private static long checkers(long[] board) {
        int status = (int) board[Board.STATUS];
        int player = status & Board.PLAYER_BIT;
        long colorMask = ~(-player ^ board[3]);
        long occupancy = board[0] | board[1] | board[2];
        long king = board[0] & ~board[1] & ~board[2] & colorMask;
        int kingSquare = Long.numberOfTrailingZeros(king);
        return Board.getCheckersPext(board[0], board[1], board[2], board[3], colorMask, player, kingSquare, occupancy);
    }

    private static int expectedMoveType(long[] board, long move) {
        int startSquare = (int) move & Board.SQUARE_BITS;
        int targetSquare = (int) move >>> Board.TARGET_SQUARE_SHIFT & Board.SQUARE_BITS;
        int startPiece = (int) move >>> Board.START_PIECE_SHIFT & Board.PIECE_BITS;
        int targetPiece = (int) move >>> Board.TARGET_PIECE_SHIFT & Board.PIECE_BITS;
        int promotePiece = (int) move >>> Board.PROMOTE_PIECE_SHIFT & Board.PIECE_BITS;
        int startType = startPiece & Piece.TYPE;
        if(startType == Piece.PAWN) {
            if(promotePiece != Value.NONE) {
                return targetPiece == Value.NONE ? Move.PROMOTION : Move.CAPTURE_PROMOTION;
            }
            if(targetPiece != Value.NONE) return Move.CAPTURE;
            if(targetSquare == Board.enPassantSquare((int) board[Board.STATUS])
                && (startSquare & Value.FILE) != (targetSquare & Value.FILE)) return Move.EN_PASSANT;
            return Math.abs(startSquare - targetSquare) == 16 ? Move.PAWN_DOUBLE_PUSH : Move.PAWN_PUSH;
        }
        if(startType == Piece.KING && Math.abs(startSquare - targetSquare) == 2) return Move.CASTLE;
        return targetPiece == Value.NONE ? Move.QUIET : Move.CAPTURE;
    }

    private static int expectedCastlingChange(long move) {
        int startSquare = (int) move & Board.SQUARE_BITS;
        int targetSquare = (int) move >>> Board.TARGET_SQUARE_SHIFT & Board.SQUARE_BITS;
        int startPiece = (int) move >>> Board.START_PIECE_SHIFT & Board.PIECE_BITS;
        int targetPiece = (int) move >>> Board.TARGET_PIECE_SHIFT & Board.PIECE_BITS;
        int change = 0;
        if((startPiece & Piece.TYPE) == Piece.KING) {
            change |= (startPiece >>> Board.PLAYER_SHIFT) == 0 ? 0b0011 : 0b1100;
        }
        if((startPiece & Piece.TYPE) == Piece.ROOK) change |= rookSquareChange(startSquare);
        if((targetPiece & Piece.TYPE) == Piece.ROOK) change |= rookSquareChange(targetSquare);
        return change;
    }

    private static int rookSquareChange(int square) {
        return switch(square) {
            case Board.SQUARE_H1 -> 0b0001;
            case Board.SQUARE_A1 -> 0b0010;
            case Board.SQUARE_H8 -> 0b0100;
            case Board.SQUARE_A8 -> 0b1000;
            default -> 0;
        };
    }

    private static void assertTarget(String fen, String moveString, int type, int castlingChange) {
        long[] board = Board.fromFen(fen);
        long[] typedBoard = BoardMoveType.fromFen(fen);
        long[] moves = new long[256];
        int count = generateExperimental(board, moves, MODE_ALL);
        long move = findMove(moves, count, moveString);
        assertNotEquals(Long.MIN_VALUE, move, "missing " + moveString + " in " + fen);
        assertEquals(type, Move.moveType(move), moveString);
        assertEquals(castlingChange, Move.castlingChange(move), moveString);
        assertEquals(0L, move & Move.UNUSED_MOVE_BIT_MASK, moveString);
        assertMoveApplicationEquals(board, typedBoard, move, fen + " " + moveString);
    }

    private static long findMove(long[] moves, int count, String moveString) {
        for(int i = 0; i < count; i ++) {
            if(Move.string(moves[i]).equals(moveString)) return moves[i];
        }
        return Long.MIN_VALUE;
    }

    private static int findMoveType(long[] moves, int count, int type) {
        for(int i = 0; i < count; i ++) {
            if(Move.moveType(moves[i]) == type) return i;
        }
        return -1;
    }

    private static void assertMoveApplicationEquals(long[] board, long[] typedBoard, long move, String message) {
        long[] expected = new long[Board.MAX_BITBOARDS];
        long[] actual = new long[Board.MAX_BITBOARDS];
        applyProduction(board, Move.withoutExperimentalMetadata(move), expected);
        applyExperimental(typedBoard, move, actual);
        assertArrayEquals(expected, actual, message);
    }

    private static void applyProduction(long[] board, long move, long[] nextBoard) {
        Board.makeMoveInto(
            board[0], board[1], board[2], board[3], (int) board[Board.STATUS], board[Board.KEY], move, nextBoard
        );
    }

    private static void applyExperimental(long[] board, long move, long[] nextBoard) {
        BoardMoveType.makeMoveInto(
            board[0], board[1], board[2], board[3], (int) board[Board.STATUS], board[Board.KEY], move, nextBoard
        );
    }

    private static void assertPerft(String fen, int depth, long knownNodes) {
        long[] productionBoard = Board.fromFen(fen);
        long[] experimentalBoard = BoardMoveType.fromFen(fen);
        long production = productionPerft(
            productionBoard, depth, 0,
            new long[depth + 1][Board.MAX_BITBOARDS], new long[depth + 1][256]
        );
        long experimental = experimentalPerft(
            experimentalBoard, depth, 0,
            new long[depth + 1][Board.MAX_BITBOARDS], new long[depth + 1][256]
        );
        if(knownNodes >= 0) assertEquals(knownNodes, production, fen);
        assertEquals(production, experimental, fen);
    }

    private static long productionPerft(long[] board, int depth, int ply, long[][] boards, long[][] moveStack) {
        if(depth == 0) return 1L;
        long[] moves = moveStack[ply];
        int count = generateProduction(board, moves, MODE_ALL);
        if(depth == 1) return count;
        long nodes = 0L;
        long[] nextBoard = boards[ply + 1];
        for(int i = 0; i < count; i ++) {
            applyProduction(board, moves[i], nextBoard);
            nodes += productionPerft(nextBoard, depth - 1, ply + 1, boards, moveStack);
        }
        return nodes;
    }

    private static long experimentalPerft(long[] board, int depth, int ply, long[][] boards, long[][] moveStack) {
        if(depth == 0) return 1L;
        long[] moves = moveStack[ply];
        int count = generateExperimental(board, moves, MODE_ALL);
        if(depth == 1) return count;
        long nodes = 0L;
        long[] nextBoard = boards[ply + 1];
        for(int i = 0; i < count; i ++) {
            applyExperimental(board, moves[i], nextBoard);
            nodes += experimentalPerft(nextBoard, depth - 1, ply + 1, boards, moveStack);
        }
        return nodes;
    }
}
