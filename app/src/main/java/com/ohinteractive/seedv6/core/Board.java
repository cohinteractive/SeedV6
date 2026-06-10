package com.ohinteractive.seedv6.core;

import com.ohinteractive.seedv6.core.util.Fen;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.core.util.Zobrist;

public class Board {

    public static final int STATUS = 4;
    public static final int MAX_BITBOARDS = 6;
    public static final int KEY = 5;
    public static final int PLAYER_BIT = 1;

    public static int player(long[] board) {
        return (int) board[STATUS] & PLAYER_BIT;
    }

    public static final int WHITE_KINGSIDE_BIT = 0b1;
    public static final int BLACK_KINGSIDE_BIT = 0b100;
    public static final int WHITE_KINGSIDE_BIT_UNSHIFTED = 0b10;
    public static final int BLACK_KINGSIDE_BIT_UNSHIFTED = 0b1000;

    public static boolean kingSide(long[] board, int player) {
        return 
        (board[STATUS] & 
        ((WHITE_KINGSIDE_BIT_UNSHIFTED & ~(-player)) | (BLACK_KINGSIDE_BIT_UNSHIFTED & -player))
        ) != 0L;
    }

    public static final int WHITE_QUEENSIDE_BIT = 0b10;
    public static final int BLACK_QUEENSIDE_BIT = 0b1000;
    public static final int WHITE_QUEENSIDE_BIT_UNSHIFTED = 0b100;
    public static final int BLACK_QUEENSIDE_BIT_UNSHIFTED = 0b10000;

    public static boolean queenSide(long[] board, int player) {
        return 
        (board[STATUS] & 
        ((WHITE_QUEENSIDE_BIT_UNSHIFTED & ~(-player)) | (BLACK_QUEENSIDE_BIT_UNSHIFTED & -player))
        ) != 0L;
    }

    public static final int ESQUARE_SHIFT = 5;
    public static final int SQUARE_BITS = 0b111111;
    public static final long WHITE_ENPASSANT_SQUARES = 0x0000ff0000000000L;
    public static final long BLACK_ENPASSANT_SQUARES = 0x0000000000ff0000L;

    /* This is the branchless version, but the branched version is probably
     * faster overall because of the rarity of valid enpassant squares
     * making correct predictions very likely
     * branched is 1-2 cycles on correct predict
     * branchless is always 5-8 cycles
     * branched is 12-20 cycles on incorrect predict
    public static int enPassantSquare(long[] board) {
        int status = (int) board[STATUS];
        int player = status & PLAYER_BIT;
        int eSquare = status >>> ESQUARE_SHIFT & SQUARE_BITS;
        long validBit = (1L << eSquare) & ((WHITE_ENPASSANT_SQUARES & ~(-player)) | (BLACK_ENPASSANT_SQUARES & -player));
        int mask = -((int) ((validBit | -validBit) >>> 63));
        return (eSquare & mask) | (Value.INVALID & ~mask);
    }
    */

    public static int enPassantSquare(long[] board) {
        int status = (int) board[STATUS];
        int player = status & PLAYER_BIT;
        int eSquare = status >>> ESQUARE_SHIFT & SQUARE_BITS;
        return ((1L << eSquare) & ((WHITE_ENPASSANT_SQUARES & ~(-player)) | (BLACK_ENPASSANT_SQUARES & -player))) != 0L ? eSquare : Value.INVALID;
    }

    public static final int HALF_MOVE_CLOCK_SHIFT = 11;
    public static final int HALF_MOVE_CLOCK_BITS = 0b1111111;

    public static int halfMoveClock(long[] board) {
        return (int) board[STATUS] >>> HALF_MOVE_CLOCK_SHIFT & HALF_MOVE_CLOCK_BITS;
    }

    public static final int FULL_MOVE_NUMBER_SHIFT = 18;
    public static final int FULL_MOVE_NUMBER_BITS = 0b1111111111;

    public static int fullMoveNumber(long[] board) {
        return (int) board[STATUS] >>> FULL_MOVE_NUMBER_SHIFT & FULL_MOVE_NUMBER_BITS;
    }

    public static long key(long[] board) {
        return board[KEY];
    }

    public static final int IN_CHECK_SHIFT = 28;
    public static final int IN_CHECK_BIT = 0b1;
    public static final int IN_CHECK_BIT_UNSHIFTED = 0b10000000000000000000000000000;
    public static final int HAS_CHECKED_SHIFT = 29;
    public static final int HAS_CHECKED_BIT = 0b1;
    public static final int HAS_CHECKED_BIT_UNSHIFTED = 0b100000000000000000000000000000;

    public static final String FEN_STARTING_POSITION = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    public static long[] startingPosition() {
        return fromFen(FEN_STARTING_POSITION);
    }

    public static final int CASTLING_SHIFT = 1;
    public static final int CASTLING_BITS = 0b1111;
    public static final int OCCUPANCY_BIT = 0b1000;
    public static final int SQUARE_A1 = 0;
    public static final int SQUARE_A8 = 56;
    public static final int SQUARE_H1 = 7;
    public static final int SQUARE_H8 = 63;

    public static long[] fromFen(String fen) {
        long[] board = new long[MAX_BITBOARDS];
        int[] pieces = Fen.getPieces(fen);
        long board0 = 0;
        long board1 = 0;
        long board2 = 0;
        long board3 = 0;
        for(int square = SQUARE_A1; square <= SQUARE_H8; square ++) {
            int piece = pieces[square];
            if(piece != Value.NONE) {
                long squareBit = 1L << square;
                board0 |= -(piece & 1) & squareBit;
                board1 |= -(piece >>> 1 & 1) & squareBit;
                board2 |= -(piece >>> 2 & 1) & squareBit;
                board3 |= -(piece >>> 3 & 1) & squareBit;
            }
        }
        board[0] = board0;
        board[1] = board1;
        board[2] = board2;
        board[3] = board3;
        int playerToMove = Fen.getWhiteToMove(fen) ? 0 : 1;
        int castling = Fen.getCastling(fen);
        long status = playerToMove ^ castling << CASTLING_SHIFT;
        int eSquare = Fen.getEnPassantSquare(fen);
        if(eSquare != -1) {
            status ^= eSquare << ESQUARE_SHIFT;
        } else {
            eSquare = 0; // this must be 0 here for getKey to work, since getKey uses a bit trick to make the eSquare calculation branchless
        }
        board[STATUS] = status ^ Fen.getHalfMoveClock(fen) << HALF_MOVE_CLOCK_SHIFT ^ Fen.getFullMoveNumber(fen) << FULL_MOVE_NUMBER_SHIFT;
        board[KEY] = Zobrist.getKey(pieces, playerToMove, castling, eSquare);
        return board;
    }

    public static int getSquare(long[] board, int square) {
        return (int) (((board[3] >>> square & 1) << 3) |
                      ((board[2] >>> square & 1) << 2) |
                      ((board[1] >>> square & 1) << 1) |
                      (board[0] >>> square & 1));
    }

    public static String boardString(long[] board) {
        StringBuilder boardString = new StringBuilder();
        for(int i = Board.SQUARE_A1; i <= Board.SQUARE_H8; i ++) {
            int square = i ^ 0x38;
            int piece = Board.getSquare(board, square);
            boardString.append((piece != Value.NONE ? Piece.SHORT_STRING[piece] : ".")).append((i & 7) == 7 ? "\n" : " ");
        }
        return boardString.toString();
    }

}