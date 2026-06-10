package com.ohinteractive.seedv6.core.util;

public class Bitboard {
    
    public static final int FILE = 0;
    public static final int RANK = 1;
    public static final int FORWARD_DIAGONAL = 2;
    public static final int BACKWARD_DIAGONAL = 3;
    public static final int CASTLE = 4;
    public static final int CASTLE_PLAYER0 = 4;
    public static final int CASTLE_PLAYER1 = 5;
    public static final int FORWARD_RANKS = 6;
    public static final int FORWARD_RANKS_PLAYER0 = 6;
    public static final int FORWARD_RANKS_PLAYER1 = 7;
    public static final int KING_BACK_RANK_BLOCK = 8;
    public static final int KING_BACK_RANK_BLOCK_PLAYER0 = 8;
    public static final int KING_BACK_RANK_BLOCK_PLAYER1 = 9;
    public static final int ROOK_BACK_RANK_PROTECT = 10;
    public static final int ROOK_BACK_RANK_PROTECT_PLAYER0 = 10;
    public static final int ROOK_BACK_RANK_PROTECT_PLAYER1 = 11;
    public static final int PAWN_SHIELD_QUEENSIDE_CLOSE = 12;
    public static final int PAWN_SHIELD_QUEENSIDE_CLOSE_PLAYER0 = 12;
    public static final int PAWN_SHIELD_QUEENSIDE_CLOSE_PLAYER1 = 13;
    public static final int PAWN_SHIELD_KINGSIDE_CLOSE = 14;
    public static final int PAWN_SHIELD_KINGSIDE_CLOSE_PLAYER0 = 14;
    public static final int PAWN_SHIELD_KINGSIDE_CLOSE_PLAYER1 = 15;
    public static final int PAWN_SHIELD_QUEENSIDE_FAR = 16;
    public static final int PAWN_SHIELD_QUEENSIDE_FAR_PLAYER0 = 16;
    public static final int PAWN_SHIELD_QUEENSIDE_FAR_PLAYER1 = 17;
    public static final int PAWN_SHIELD_KINGSIDE_FAR = 18;
    public static final int PAWN_SHIELD_KINGSIDE_FAR_PLAYER0 = 18;
    public static final int PAWN_SHIELD_KINGSIDE_FAR_PLAYER1 = 19;
    public static final int PAWN_STORM_QUEENSIDE_CLOSE = 20;
    public static final int PAWN_STORM_QUEENSIDE_CLOSE_PLAYER0 = 20;
    public static final int PAWN_STORM_QUEENSIDE_CLOSE_PLAYER1 = 21;
    public static final int PAWN_STORM_KINGSIDE_CLOSE = 22;
    public static final int PAWN_STORM_KINGSIDE_CLOSE_PLAYER0 = 22;
    public static final int PAWN_STORM_KINGSIDE_CLOSE_PLAYER1 = 23;
    public static final int PAWN_STORM_QUEENSIDE_FAR = 24;
    public static final int PAWN_STORM_QUEENSIDE_FAR_PLAYER0 = 24;
    public static final int PAWN_STORM_QUEENSIDE_FAR_PLAYER1 = 25;
    public static final int PAWN_STORM_KINGSIDE_FAR = 26;
    public static final int PAWN_STORM_KINGSIDE_FAR_PLAYER0 = 26;
    public static final int PAWN_STORM_KINGSIDE_FAR_PLAYER1 = 27;
    public static final int KING_START_POSITION = 28;
    public static final int KING_START_POSITION_PLAYER0 = 28;
    public static final int KING_START_POSITION_PLAYER1 = 29;
    public static final int QUEEN_START_POSITION = 30;
    public static final int QUEEN_START_POSITION_PLAYER0 = 30;
    public static final int QUEEN_START_POSITION_PLAYER1 = 31;
    public static final int ROOK_START_POSITION = 32;
    public static final int ROOK_START_POSITION_PLAYER0 = 32;
    public static final int ROOK_START_POSITION_PLAYER1 = 33;
    public static final int BISHOP_START_POSITION = 34;
    public static final int BISHOP_START_POSITION_PLAYER0 = 34;
    public static final int BISHOP_START_POSITION_PLAYER1 = 35;
    public static final int KNIGHT_START_POSITION = 36;
    public static final int KNIGHT_START_POSITION_PLAYER0 = 36;
    public static final int KNIGHT_START_POSITION_PLAYER1 = 37;
    public static final int KING_RING = 38;
    public static final int KING_RING_PLAYER0 = 38;
    public static final int KING_RING_PLAYER1 = 39;
    public static final int SQUARE_COLOR_LIGHT = 40;
    public static final int SQUARE_COLOR_DARK = 41;
    public static final int ENPASSANT_SQUARES = 42;
    public static final int ENPASSANT_SQUARES_PLAYER0 = 42;
    public static final int ENPASSANT_SQUARES_PLAYER1 = 43;
    public static final int PASSED_PAWNS_FILES = 44;
    public static final int PASSED_PAWNS_FILES_PLAYER0 = 44;
    public static final int PASSED_PAWNS_FILES_PLAYER1 = 45;
    public static final int LEAP_ATTACKS = 46;
    public static final int KING_ATTACKS = 47;
    public static final int PAWN_ATTACKS = 48;
    public static final int PAWN_ATTACKS_PLAYER0 = 48;
    public static final int PAWN_ATTACKS_PLAYER1 = 49;
    public static final int PAWN_ADVANCE_1 = 50;
    public static final int PAWN_ADVANCE_1_PLAYER0 = 50;
    public static final int PAWN_ADVANCE_1_PLAYER1 = 51;
    public static final int PAWN_ADVANCE_2 = 52;
    public static final int PAWN_ADVANCE_2_PLAYER0 = 52;
    public static final int PAWN_ADVANCE_2_PLAYER1 = 53;
    public static final int RANK_FILE_ATTACKS = 54;
    public static final int DIAGONAL_ATTACKS = 55;

    private static final int TABLE_COUNT = 56;

    public static final long[][] BB = new long[TABLE_COUNT][];

    static {
        initFixedTables();
        initGeneratedTables();
    }

    public static String toString(long bitboard) {
        StringBuilder string = new StringBuilder();
        for(int i = 0; i < 64; i ++) {
            long squareBit = 1L << (i ^ 0x38);
            string.append((bitboard & squareBit) != 0L ? "1 " : ". ");
            if((i & 7) == 7) string.append('\n');
        }
        return string.toString();
    }

    public static void drawBitboard(long bitboard) {
        String s = toString(bitboard);
        System.out.println(s);
    }

    private Bitboard() {}

    private static void initFixedTables() {

    }

    private static void initGeneratedTables() {
        BB[LEAP_ATTACKS] = buildLeapAttacks();
        BB[KING_ATTACKS] = buildKingAttacks();
        long[][] pawnAttacks = buildPawnAttacks();
        BB[PAWN_ATTACKS_PLAYER0] = pawnAttacks[0];
        BB[PAWN_ATTACKS_PLAYER1] = pawnAttacks[1];
        BB[RANK_FILE_ATTACKS] = buildRankFileAttacks();
        BB[DIAGONAL_ATTACKS] = buildDiagonalAttacks();
    }

    private static long[] buildLeapAttacks() {
        long[] attacks = new long[64];
        int[][] offsets = {
            {17, 33}, {10, 18}, {-6, -14}, {-15, -31},
            {-17, -33}, {-10, -18}, {6, 14}, {15, 31}
        };
        for(int square = 0; square < 64; square ++) {
            int x88 = ((square & 0b111000) << 1) | (square & 7);
            long bb = 0L;
            for(int d = 0; d < 8; d ++) {
                if(((x88 + offsets[d][1]) & 0x88) == 0) bb |= 1L << (square + offsets[d][0]);
            }
            attacks[square] = bb;
        }
        return attacks;
    }

    private static long[] buildKingAttacks() {
        long[] attacks = new long[64];
        int[][] offsets = {
            {8, 16}, {1, 1}, {-8, -16}, {-1, -1},
            {9, 17}, {-7, -15}, {-9, -17}, {7, 15}
        };
        for(int square = 0; square < 64; square ++) {
            int x88 = ((square & 0b111000) << 1) | (square & 7);
            long bb = 0L;
            for(int d = 0; d < 8; d ++) {
                if(((x88 + offsets[d][1]) & 0x88) == 0) bb |= (1L << (square + offsets[d][0]));
            }
            attacks[square] = bb;
        }
        return attacks;
    }

    private static long[][] buildPawnAttacks() {
        long[][] attacks = new long[2][64];
        int[][][] offsets = { 
            { {7, 15}, {9, 17} },
            { {-7, -15}, {-9, -17} }
        };
        for(int square = 0; square < 64; square ++) {
            int x88 = ((square & 0b111000) << 1) | (square & 7);
            for(int player = 0; player < 2; player ++) {
                long bb = 0L;
                for(int d = 0; d < 2; d ++) {
                    if(((x88 + offsets[player][d][1]) & 0x88) == 0) bb |= (1L << (square + offsets[player][d][0]));
                }
                attacks[player][square] = bb;
            }
        }
        return attacks;
    }

    private static long[] buildRankFileAttacks() {
        long[] attacks = new long[64 * 64];
        for(int square = 0; square < 64; square ++) {
            for(int target = 0; target < 64; target ++) {
                if(square == target) continue;
                int squareRank = square >>> 3;
                int squareFile = square & 7;
                int targetRank = target >>> 3;
                int targetFile = target & 7;
                long mask = 0L;
                if(squareRank == targetRank) {
                    int from = squareFile < targetFile ? squareFile : targetFile;
                    int to = squareFile > targetFile ? squareFile : targetFile;
                    for(int file = from; file <= to; file ++) {
                        mask |= 1L << ((squareRank << 3) | file);
                    }
                    attacks[square | (target << 6)] = mask;
                } else if (squareFile == targetFile) {
                    int from = squareRank < targetRank ? squareRank : targetRank;
                    int to = squareRank > targetRank ? squareRank : targetRank;
                    for(int rank = from; rank <= to; rank ++) {
                        mask |= 1L << ((rank << 3) | squareFile);
                    }
                    attacks[square | (target << 6)] = mask;
                }
            }
        }
        return attacks;
    }

    private static long[] buildDiagonalAttacks() {
        long[] attacks = new long[64 * 64];
        for(int square = 0; square < 64; square ++) {
            for(int target = 0; target < 64; target ++) {
                if(square == target) continue;
                int squareRank = square >>> 3;
                int squareFile = square & 7;
                int targetRank = target >>> 3;
                int targetFile = target & 7;
                int rankDistance = squareRank - targetRank;
                int fileDistance = squareFile = targetFile;
                if(rankDistance < 0) rankDistance = -rankDistance;
                if(fileDistance < 0) fileDistance = -fileDistance;
                if(rankDistance != fileDistance) continue;
                int rankStep = squareRank < targetRank ? 1 : -1;
                int fileStep = squareFile < targetFile ? 1 : -1;
                long mask = 0L;
                int rank = squareRank;
                int file = squareFile;
                while(true) {
                    mask |= 1L << ((rank << 3) | file);
                    if(rank == targetRank && file == targetFile) break;
                    rank += rankStep;
                    file += fileStep;
                }
                attacks[square | (target << 6)] = mask;
            }
        }
        return attacks;
    }

}
