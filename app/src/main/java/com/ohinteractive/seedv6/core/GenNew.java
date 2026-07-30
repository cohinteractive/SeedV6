package com.ohinteractive.seedv6.core;

import com.ohinteractive.seedv6.core.util.Bitboard;

public class GenNew {
    
    public static int genEvasion(long board0, long board1, long board2, long board3, int status, long checkers, long[] movesBuffer, long[] boardBuffer) {
        final int player = status & Board.PLAYER_BIT;
        final int playerBit = player << Board.PLAYER_SHIFT;
        final long colorMask = ~(-(player) ^ board3);

        final long kingBitboard = board0 & ~board1 & ~board2 & colorMask;
        final int[] lsb = LSB;
        final int kingSquare = lsb[(int) (((kingBitboard & -kingBitboard) * DB) >>> 58)];
        int moveListLength = 0;

        return moveListLength;
    }

    private static int getKingEvasions(int piece, int moveListLength, int square, long otherOccupancy, int[] lsb) {
        final long kingAttacks = KING_ATTACKS[square];
        final int moveInfo = square | (piece << Board.START_PIECE_SHIFT);
        long moveBitboard = kingAttacks & otherOccupancy;
        while(moveBitboard != 0L) {
            final long b = moveBitboard & -moveBitboard;
            moveBitboard ^= b;
            final int targetSquare = lsb[(int) ((b * DB) >>> 58)];

        }
        return moveListLength;
    }

    private static final long[] KING_ATTACKS = new long[64];
    private static final long[] LEAP_ATTACKS = new long[64];
    private static final long[][] PAWN_ATTACKS = new long[2][64];
    private static final long[][] PAWN_ADVANCE_SINGLE = new long[2][64];
    private static final long[][] PAWN_ADVANCE_DOUBLE = new long[2][64];
    private static final long[] BETWEEN = new long[64 * 64];
    static {
        for(int square = 0; square < 64; square ++) {
            KING_ATTACKS[square] = Bitboard.BB[Bitboard.KING_ATTACKS][square];
            LEAP_ATTACKS[square] = Bitboard.BB[Bitboard.LEAP_ATTACKS][square];
            for(int player = 0; player < 2; player ++) {
                PAWN_ATTACKS[player][square] = Bitboard.BB[Bitboard.PAWN_ATTACKS_PLAYER0 + player][square];
                PAWN_ADVANCE_SINGLE[player][square] = Bitboard.BB[Bitboard.PAWN_ADVANCE_1_PLAYER0 + player][square];
                PAWN_ADVANCE_DOUBLE[player][square] = Bitboard.BB[Bitboard.PAWN_ADVANCE_2_PLAYER0 + player][square];
            }
            for(int target = 0; target < 64; target ++) {
                BETWEEN[square | (target << 6)] = Bitboard.BB[Bitboard.BETWEEN][square | (target << 6)];
            }
        }
    }

    /*
	 * To get the LSB from a long, use:
	 * int lsbIndex = LSB[(int) (((someLong & -someLong) * DB) >>> 58)];
	 */
	private static final int[] LSB = {
        0,  1, 48,  2, 57, 49, 28,  3,
		61, 58, 50, 42, 38, 29, 17,  4,
		62, 55, 59, 36, 53, 51, 43, 22,
		45, 39, 33, 30, 24, 18, 12,  5,
		63, 47, 56, 27, 60, 41, 37, 16,
		54, 35, 52, 21, 44, 32, 23, 11,
		46, 26, 40, 15, 34, 20, 31, 10,
		25, 14, 19,  9, 13,  8,  7,  6
    };

	private static final long DB = 0x03f79d71b4cb0a89L;

}
