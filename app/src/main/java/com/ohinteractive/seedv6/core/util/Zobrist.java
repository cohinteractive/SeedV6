package com.ohinteractive.seedv6.core.util;

public class Zobrist {
    
    public static final long[][][] PIECE = new long[8][2][64];
    public static final long WHITEMOVE;
    public static final long[][] CASTLING = new long[2][2];
    public static final long[] ENPASSANT_FILE = new long[8];

    public static long getKey(int[] squares, int playerToMove, int castling, int eSquare) {
		long key = 0L;
		for(int i = 0; i < 64; i ++) {
			int piece = squares[i] & 0xf;
            key ^= PIECE[piece & 7][piece >>> 3][i];
		}
		return  key ^
                (CASTLING[0][0] & -((long) (castling & 1))) ^
                (CASTLING[0][1] & -((long) ((castling >>> 1) & 1))) ^
                (CASTLING[1][0] & -((long) ((castling >>> 2) & 1))) ^
                (CASTLING[1][1] & -((long) ((castling >>> 3) & 1))) ^
		        (ENPASSANT_FILE[eSquare & 7] & -((long) ((eSquare | -eSquare) >>> 31))) ^
		        (WHITEMOVE & ~(-((long) playerToMove)));
	}

    private static final long SEED = 0xA7F2C90E5B81D346L;

    static {
        SplitMix64 rng = new SplitMix64(SEED);
        for(int pieceOrFile = 0; pieceOrFile < 8; pieceOrFile ++) {
            for(int player = 0; player < 2; player ++) {
                for(int square = 0; square < 64; square ++) {
                    PIECE[pieceOrFile][player][square] = rng.nextLong();
                }
                for(int side = 0; side < 2; side ++) {
                    CASTLING[player][side] = rng.nextLong();
                }
            }
            ENPASSANT_FILE[pieceOrFile] = rng.nextLong();
        }
        WHITEMOVE = rng.nextLong();
    }

    private Zobrist() {

    }

    private static final class SplitMix64 {
        private long state;

        SplitMix64(long seed) {
            this.state = seed;
        }

        long nextLong() {
            long z = (state += 0x9E3779B97F4A7C15L);
            z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
            z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
            return z ^ (z >>> 31);
        }
    }

}
