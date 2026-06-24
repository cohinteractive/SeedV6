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
    public static final int BETWEEN = 56;

    private static final int TABLE_COUNT = 57;

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
        // FILE 0
        BB[FILE] = new long[] {
            0x0101010101010101L, 0x0202020202020202L, 0x0404040404040404L, 0x0808080808080808L,
            0x1010101010101010L, 0x2020202020202020L, 0x4040404040404040L, 0x8080808080808080L
        };
        // RANK 1
		BB[RANK] = new long[] {
            0x00000000000000ffL, 0x000000000000ff00L, 0x0000000000ff0000L, 0x00000000ff000000L,
            0x000000ff00000000L, 0x0000ff0000000000L, 0x00ff000000000000L, 0xff00000000000000L
        };
        // FORWARD_DIAGONAL 2
		BB[FORWARD_DIAGONAL] = new long[] {
            0x8040201008040201L, 0x0080402010080402L, 0x0000804020100804L, 0x0000008040201008L, 0x0000000080402010L, 0x0000000000804020L, 0x0000000000008040L, 0x0000000000000080L,
            0x4020100804020100L, 0x8040201008040201L, 0x0080402010080402L, 0x0000804020100804L, 0x0000008040201008L, 0x0000000080402010L, 0x0000000000804020L, 0x0000000000008040L,
            0x2010080402010000L, 0x4020100804020100L, 0x8040201008040201L, 0x0080402010080402L, 0x0000804020100804L, 0x0000008040201008L, 0x0000000080402010L, 0x0000000000804020L,
            0x1008040201000000L, 0x2010080402010000L, 0x4020100804020100L, 0x8040201008040201L, 0x0080402010080402L, 0x0000804020100804L, 0x0000008040201008L, 0x0000000080402010L,
            0x0804020100000000L, 0x1008040201000000L, 0x2010080402010000L, 0x4020100804020100L, 0x8040201008040201L, 0x0080402010080402L, 0x0000804020100804L, 0x0000008040201008L,
            0x0402010000000000L, 0x0804020100000000L, 0x1008040201000000L, 0x2010080402010000L, 0x4020100804020100L, 0x8040201008040201L, 0x0080402010080402L, 0x0000804020100804L,
            0x0201000000000000L, 0x0402010000000000L, 0x0804020100000000L, 0x1008040201000000L, 0x2010080402010000L, 0x4020100804020100L, 0x8040201008040201L, 0x0080402010080402L,
            0x0100000000000000L, 0x0201000000000000L, 0x0402010000000000L, 0x0804020100000000L, 0x1008040201000000L, 0x2010080402010000L, 0x4020100804020100L, 0x8040201008040201L
        };
        // BACKWARD_DIAGONAL 3
		BB[BACKWARD_DIAGONAL] = new long[] {
            0x0000000000000001L, 0x0000000000000102L, 0x0000000000010204L, 0x0000000001020408L, 0x0000000102040810L, 0x0000010204081020L, 0x0001020408102040L, 0x0102040810204080L,
            0x0000000000000102L, 0x0000000000010204L, 0x0000000001020408L, 0x0000000102040810L, 0x0000010204081020L, 0x0001020408102040L, 0x0102040810204080L, 0x0204081020408000L,
            0x0000000000010204L, 0x0000000001020408L, 0x0000000102040810L, 0x0000010204081020L, 0x0001020408102040L, 0x0102040810204080L, 0x0204081020408000L, 0x0408102040800000L,
            0x0000000001020408L, 0x0000000102040810L, 0x0000010204081020L, 0x0001020408102040L, 0x0102040810204080L, 0x0204081020408000L, 0x0408102040800000L, 0x0810204080000000L,
            0x0000000102040810L, 0x0000010204081020L, 0x0001020408102040L, 0x0102040810204080L, 0x0204081020408000L, 0x0408102040800000L, 0x0810204080000000L, 0x1020408000000000L,
            0x0000010204081020L, 0x0001020408102040L, 0x0102040810204080L, 0x0204081020408000L, 0x0408102040800000L, 0x0810204080000000L, 0x1020408000000000L, 0x2040800000000000L,
            0x0001020408102040L, 0x0102040810204080L, 0x0204081020408000L, 0x0408102040800000L, 0x0810204080000000L, 0x1020408000000000L, 0x2040800000000000L, 0x4080000000000000L,
            0x0102040810204080L, 0x0204081020408000L, 0x0408102040800000L, 0x0810204080000000L, 0x1020408000000000L, 0x2040800000000000L, 0x4080000000000000L, 0x8000000000000000L
        };
        // CASTLE_PLAYER0 4
		BB[CASTLE_PLAYER0] = new long[]	{
            0x0000000000000060L, 0x000000000000000eL
        };
        // CASTLE_PLAYER1 5
		BB[CASTLE_PLAYER1] = new long[]	{
            0x6000000000000000L, 0x0e00000000000000L
        };
        // FORWARD_RANKS_PLAYER0 6
		BB[FORWARD_RANKS_PLAYER0] = new long[] {
            0xffffffffffffff00L, 0xffffffffffff0000L, 0xffffffffff000000L, 0xffffffff00000000L,
            0xffffff0000000000L, 0xffff000000000000L, 0xff00000000000000L, 0x0000000000000000L
        };
        // FORWARD_RANKS_PLAYER1 7
		BB[FORWARD_RANKS_PLAYER1] = new long[] {
            0x0000000000000000L, 0x00000000000000ffL, 0x000000000000ffffL, 0x0000000000ffffffL,
            0x00000000ffffffffL, 0x000000ffffffffffL, 0x0000ffffffffffffL,0x00ffffffffffffffL
        };
        // KING_BACK_RANK_BLOCK_PLAYER0 8
		BB[KING_BACK_RANK_BLOCK_PLAYER0] = new long[] {
            0x0000000000000000L, 0x0000000000000001L,
            0x0000000000000003L, 0x0000000000000007L,
            0x0000000000000000L, 0x00000000000000c0L,
            0x0000000000000080L, 0x0000000000000000L
        };
        // KING_BACK_RANK_BLOCK_PLAYER1 9
		BB[KING_BACK_RANK_BLOCK_PLAYER1] = new long[] {
            0x0000000000000000L, 0x0100000000000000L,
            0x0300000000000000L, 0x0700000000000000L,
            0x0000000000000000L, 0xc000000000000000L,
            0x8000000000000000L, 0x0000000000000000L
        };
        // ROOK_BACK_RANK_PROTECT_PLAYER0 10
		BB[ROOK_BACK_RANK_PROTECT_PLAYER0] = new long[] {
            0x000000000000000eL, 0x000000000000000cL,
            0x0000000000000008L, 0x0000000000000000L,
            0x0000000000000000L, 0x0000000000000000L,
            0x0000000000000020L, 0x0000000000000060L,
        };
        // ROOK_BACK_RANK_PROTECT_PLAYER1 11
		BB[ROOK_BACK_RANK_PROTECT_PLAYER1] = new long[] {
            0x0e00000000000000L, 0x0c00000000000000L,
            0x0800000000000000L, 0x0000000000000000L,
            0x0000000000000000L, 0x0000000000000000L,
            0x2000000000000000L, 0x6000000000000000L
        };
        // PAWN_SHIELD_QUEENSIDE_CLOSE_PLAYER0 12
		BB[PAWN_SHIELD_QUEENSIDE_CLOSE_PLAYER0] = new long[] {
            0x0000000000000700L
        };
        // PAWN_SHIELD_QUEENSIDE_CLOSE_PLAYER1 13
		BB[PAWN_SHIELD_QUEENSIDE_CLOSE_PLAYER1] = new long[] {
            0x0007000000000000L
        };
        // PAWN_SHIELD_KINGSIDE_CLOSE_PLAYER0 14
		BB[PAWN_SHIELD_KINGSIDE_CLOSE_PLAYER0] = new long[]	{
            0x000000000000e000L
        };
        // PAWN_SHIELD_KINGSIDE_CLOSE_PLAYER1 15
		BB[PAWN_SHIELD_KINGSIDE_CLOSE_PLAYER1] = new long[]	{
            0x00e0000000000000L
        };
        // PAWN_SHIELD_QUEENSIDE_FAR_PLAYER0 16
		BB[PAWN_SHIELD_QUEENSIDE_FAR_PLAYER0] = new long[] {
            0x0000000000070000L
        };
        // PAWN_SHIELD_QUEENSIDE_FAR_PLAYER1 17
		BB[PAWN_SHIELD_QUEENSIDE_FAR_PLAYER1] = new long[] {
            0x0000070000000000L
        };
        // PAWN_SHIELD_KINGSIDE_FAR_PLAYER0 18
		BB[PAWN_SHIELD_KINGSIDE_FAR_PLAYER0] = new long[] {
            0x0000000000e00000L
        };
        // PAWN_SHIELD_KINGSIDE_FAR_PLAYER1 19
		BB[PAWN_SHIELD_KINGSIDE_FAR_PLAYER1] = new long[] {
            0x0000e00000000000L
        };
        // PAWN_STORM_QUEENSIDE_CLOSE_PLAYER0 20
		BB[PAWN_STORM_QUEENSIDE_CLOSE_PLAYER0] = new long[] {
            0x0000000000070000L
        };
        // PAWN_STORM_QUEENSIDE_CLOSE_PLAYER1 21
		BB[PAWN_STORM_QUEENSIDE_CLOSE_PLAYER1] = new long[]	{
            0x0000070000000000L
        };
        // PAWN_STORM_KINGSIDE_CLOSE_PLAYER0 22
		BB[PAWN_STORM_KINGSIDE_CLOSE_PLAYER0] = new long[] {
            0x0000000000e00000L
        };
        // PAWN_STORM_KINGSIDE_CLOSE_PLAYER1 23
		BB[PAWN_STORM_KINGSIDE_CLOSE_PLAYER1] = new long[] {
            0x0000e00000000000L
        };
        // PAWN_STORM_QUEENSIDE_FAR_PLAYER0 24
		BB[PAWN_STORM_QUEENSIDE_FAR_PLAYER0] = new long[] {
            0x0000000007000000L
        };
        // PAWN_STORM_QUEENSIDE_FAR_PLAYER1 25
		BB[PAWN_STORM_QUEENSIDE_FAR_PLAYER1] = new long[] {
            0x0000000700000000L
        };
        // PAWN_STORM_KINGSIDE_FAR_PLAYER0 26
		BB[PAWN_STORM_KINGSIDE_FAR_PLAYER0] = new long[] {
            0x00000000e0000000L
        };
        // PAWN_STORM_KINGSIDE_FAR_PLAYER1 27
		BB[PAWN_STORM_KINGSIDE_FAR_PLAYER1] = new long[] {
            0x000000e000000000L
        };
        // KING_START_POSITION_PLAYER0 28
		BB[KING_START_POSITION_PLAYER0] = new long[] {
            0x0000000000000010L
        };
        // KING_START_POSITION_PLAYER1 29
		BB[KING_START_POSITION_PLAYER1] = new long[] {
            0x1000000000000000L
        };
        // QUEEN_START_POSITION_PLAYER0 30
		BB[QUEEN_START_POSITION_PLAYER0] = new long[] {
            0x0000000000000008L
        };
        // QUEEN_START_POSITION_PLAYER1 31
		BB[QUEEN_START_POSITION_PLAYER1] = new long[] {
            0x0800000000000000L
        };
        // ROOK_START_POSITION_PLAYER0 32
		BB[ROOK_START_POSITION_PLAYER0] = new long[] {
            0x0000000000000081L
        };
        // ROOK_START_POSITION_PLAYER1 33
		BB[ROOK_START_POSITION_PLAYER1] = new long[] {
            0x8100000000000000L
        };
        // BISHOP_START_POSITION_PLAYER0 34
		BB[BISHOP_START_POSITION_PLAYER0] = new long[] {
            0x0000000000000024L
        };
        // BISHOP_START_POSITION_PLAYER1 35
		BB[BISHOP_START_POSITION_PLAYER1] = new long[] {
            0x2400000000000000L
        };
        // KNIGHT_START_POSITION_PLAYER0 36
		BB[KNIGHT_START_POSITION_PLAYER0] = new long[] {
            0x0000000000000042L
        };
        // KNIGHT_START_POSITION_PLAYER1 37
		BB[KNIGHT_START_POSITION_PLAYER1] = new long[] {
            0x4200000000000000L
        };
        // KING_RING_PLAYER0 38
		BB[KING_RING_PLAYER0] = new long[] {
            0x0000000000030303L, 0x0000000000070707L, 0x00000000000e0e0eL, 0x00000000001c1c1cL, 0x0000000000383838L, 0x0000000000707070L, 0x0000000000e0e0e0L, 0x0000000000c0c0c0L,
            0x0000000003030303L, 0x0000000007070707L, 0x000000000e0e0e0eL, 0x000000001c1c1c1cL, 0x0000000038383838L, 0x0000000070707070L, 0x00000000e0e0e0e0L, 0x00000000c0c0c0c0L,
            0x0000000303030300L, 0x0000000707070700L, 0x0000000e0e0e0e00L, 0x0000001c1c1c1c00L, 0x0000003838383800L, 0x0000007070707000L, 0x000000e0e0e0e000L, 0x000000c0c0c0c000L,
            0x0000030303030000L, 0x0000070707070000L, 0x00000e0e0e0e0000L, 0x00001c1c1c1c0000L, 0x0000383838380000L, 0x0000707070700000L, 0x0000e0e0e0e00000L, 0x0000c0c0c0c00000L,
            0x0003030303000000L, 0x0007070707000000L, 0x000e0e0e0e000000L, 0x001c1c1c1c000000L, 0x0038383838000000L, 0x0070707070000000L, 0x00e0e0e0e0000000L, 0x00c0c0c0c0000000L,
            0x0303030300000000L, 0x0707070700000000L, 0x0e0e0e0e00000000L, 0x1c1c1c1c00000000L, 0x3838383800000000L, 0x7070707000000000L, 0xe0e0e0e000000000L, 0xc0c0c0c000000000L,
            0x0303030000000000L, 0x0707070000000000L, 0x0e0e0e0000000000L, 0x1c1c1c0000000000L, 0x3838380000000000L, 0x7070700000000000L, 0xe0e0e00000000000L, 0xc0c0c00000000000L,
            0x0303000000000000L, 0x0707000000000000L, 0x0e0e000000000000L, 0x1c1c000000000000L, 0x3838000000000000L, 0x7070000000000000L, 0xe0e0000000000000L, 0xc0c0000000000000L
        };
        // KING_RING_PLAYER1 39
		BB[KING_RING_PLAYER1] = new long[] {
            0x0000000000000303L, 0x0000000000000707L, 0x0000000000000e0eL, 0x0000000000001c1cL, 0x0000000000003838L, 0x0000000000007070L, 0x000000000000e0e0L, 0x000000000000c0c0L,
            0x0000000000030303L, 0x0000000000070707L, 0x00000000000e0e0eL, 0x00000000001c1c1cL, 0x0000000000383838L, 0x0000000000707070L, 0x0000000000e0e0e0L, 0x0000000000c0c0c0L,
            0x0000000003030303L, 0x0000000007070707L, 0x000000000e0e0e0eL, 0x000000001c1c1c1cL, 0x0000000038383838L, 0x0000000070707070L, 0x00000000e0e0e0e0L, 0x00000000c0c0c0c0L,
            0x0000000303030300L, 0x0000000707070700L, 0x0000000e0e0e0e00L, 0x0000001c1c1c1c00L, 0x0000003838383800L, 0x0000007070707000L, 0x000000e0e0e0e000L, 0x000000c0c0c0c000L,
            0x0000030303030000L, 0x0000070707070000L, 0x00000e0e0e0e0000L, 0x00001c1c1c1c0000L, 0x0000383838380000L, 0x0000707070700000L, 0x0000e0e0e0e00000L, 0x0000c0c0c0c00000L,
            0x0003030303000000L, 0x0007070707000000L, 0x000e0e0e0e000000L, 0x001c1c1c1c000000L, 0x0038383838000000L, 0x0070707070000000L, 0x00e0e0e0e0000000L, 0x00c0c0c0c0000000L,
            0x0303030300000000L, 0x0707070700000000L, 0x0e0e0e0e00000000L, 0x1c1c1c1c00000000L, 0x3838383800000000L, 0x7070707000000000L, 0xe0e0e0e000000000L, 0xc0c0c0c000000000L,
            0x0303030000000000L, 0x0707070000000000L, 0x0e0e0e0000000000L, 0x1c1c1c0000000000L, 0x3838380000000000L, 0x7070700000000000L, 0xe0e0e00000000000L, 0xc0c0c00000000000L
        };
        // SQUARE_COLOR_LIGHT 40
		BB[SQUARE_COLOR_LIGHT] = new long[]	{
            0x55aa55aa55aa55aaL
        };
        // SQUARE_COLOR_DARK 41
		BB[SQUARE_COLOR_DARK] = new long[] {
            0xaa55aa55aa55aa55L
        };
        // ENPASSANT_SQUARES_PLAYER0 42
		BB[ENPASSANT_SQUARES_PLAYER0] = new long[] {
            0x0000000000ff0000L
        };
        // ENPASSANT_SQUARES_PLAYER1 43
		BB[ENPASSANT_SQUARES_PLAYER1] = new long[] {
            0x0000ff0000000000L
        };
        // PASSED_PAWNS_FILES_PLAYER0 44
		BB[PASSED_PAWNS_FILES_PLAYER0] = new long[]	{
            0x0003030303030000L, 0x0007070707070000L, 0x000e0e0e0e0e0000L, 0x001c1c1c1c1c0000L, 0x0038383838380000L, 0x0070707070700000L, 0x00e0e0e0e0e00000L, 0x00c0c0c0c0c00000L
        };
        // PASSED_PAWNS_FILES_PLAYER1 45
		BB[PASSED_PAWNS_FILES_PLAYER1] = new long[]	{
            0x0000030303030300L, 0x0000070707070700L, 0x00000e0e0e0e0e00L, 0x00001c1c1c1c1c00L, 0x0000383838383800L, 0x0000707070707000L, 0x0000e0e0e0e0e000L, 0x0000c0c0c0c0c000L
        };
    }

    private static void initGeneratedTables() {
        BB[LEAP_ATTACKS] = buildLeapAttacks();
        BB[KING_ATTACKS] = buildKingAttacks();
        long[][] pawnAttacks = buildPawnAttacks();
        BB[PAWN_ATTACKS_PLAYER0] = pawnAttacks[0];
        BB[PAWN_ATTACKS_PLAYER1] = pawnAttacks[1];
        long[][] pawnAdvanceSingle = buildPawnAdvanceSingle();
        BB[PAWN_ADVANCE_1_PLAYER0] = pawnAdvanceSingle[0];
        BB[PAWN_ADVANCE_1_PLAYER1] = pawnAdvanceSingle[1];
        long[][] pawnAdvanceDouble = buildPawnAdvanceDouble();
        BB[PAWN_ADVANCE_2_PLAYER0] = pawnAdvanceDouble[0];
        BB[PAWN_ADVANCE_2_PLAYER1] = pawnAdvanceDouble[1];
        BB[RANK_FILE_ATTACKS] = buildRankFileAttacks();
        BB[DIAGONAL_ATTACKS] = buildDiagonalAttacks();
        BB[BETWEEN] = buildBetween();
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

    private static long[][] buildPawnAdvanceSingle() {
        long[][] advances = new long[2][64];
        for(int square = 0; square < 64; square ++) {
            int rank = square >>> 3;
            advances[0][square] = rank < 7 ? 1L << (square + 8) : 0L;
            advances[1][square] = rank > 0 ? 1L << (square - 8) : 0L;
        }
        return advances;
    }

    private static long[][] buildPawnAdvanceDouble() {
        long[][] advances = new long[2][64];
        for(int square = 0; square < 64; square ++) {
            int rank = square >>> 3;
            advances[0][square] = rank == 1 ? 1L << (square + 16) : 0L;
            advances[1][square] = rank == 6 ? 1L << (square - 16) : 0L;
        }
        return advances;
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
                int fileDistance = squareFile - targetFile;
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

    private static long[] buildBetween() {
        long[] attacks = new long[64 * 64];
            for(int square = 0; square < 64; square++) {
            for(int target = 0; target < 64; target++) {
                int index = square | (target << 6);
                long mask = BB[RANK_FILE_ATTACKS][index] | BB[DIAGONAL_ATTACKS][index];
                attacks[index] = mask & ~((1L << square) | (1L << target));
            }
        }
        return attacks;
    }

}
