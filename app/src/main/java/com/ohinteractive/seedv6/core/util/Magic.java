package com.ohinteractive.seedv6.core.util;

public class Magic {
    
    public static long rookMoves(int square, long allOccupancy) {
		return ROOK_MOVES[square][(int) ((allOccupancy & ROOK_MOVEMENT[square]) * ROOK_MAGIC_NUMBER[square] >>> ROOK_SHIFT[square])];
	}

	public static long bishopMoves(int square, long allOccupancy) {
		return BISHOP_MOVES[square][(int) ((allOccupancy & BISHOP_MOVEMENT[square]) * BISHOP_MAGIC_NUMBER[square] >>> BISHOP_SHIFT[square])];
	}

	public static long queenMoves(int square, long allOccupancy) {
		return ROOK_MOVES[square][(int) ((allOccupancy & ROOK_MOVEMENT[square]) * ROOK_MAGIC_NUMBER[square] >>> ROOK_SHIFT[square])]
		| BISHOP_MOVES[square][(int) ((allOccupancy & BISHOP_MOVEMENT[square]) * BISHOP_MAGIC_NUMBER[square] >>> BISHOP_SHIFT[square])];
	}

	public static void init() {}

    private final static long OUTER = 0xff818181818181ffL;

	public final static long[] ROOK_MAGIC_NUMBER = { 0xa180022080400230L, 0x40100040022000L, 0x80088020001002L, 0x80080280841000L, 0x4200042010460008L,
			0x4800a0003040080L, 0x400110082041008L, 0x8000a041000880L, 0x10138001a080c010L, 0x804008200480L, 0x10011012000c0L, 0x22004128102200L,
			0x200081201200cL, 0x202a001048460004L, 0x81000100420004L, 0x4000800380004500L, 0x208002904001L, 0x90004040026008L, 0x208808010002001L,
			0x2002020020704940L, 0x8048010008110005L, 0x6820808004002200L, 0xa80040008023011L, 0xb1460000811044L, 0x4204400080008ea0L, 0xb002400180200184L,
			0x2020200080100380L, 0x10080080100080L, 0x2204080080800400L, 0xa40080360080L, 0x2040604002810b1L, 0x8c218600004104L, 0x8180004000402000L,
			0x488c402000401001L, 0x4018a00080801004L, 0x1230002105001008L, 0x8904800800800400L, 0x42000c42003810L, 0x8408110400b012L, 0x18086182000401L,
			0x2240088020c28000L, 0x1001201040c004L, 0xa02008010420020L, 0x10003009010060L, 0x4008008008014L, 0x80020004008080L, 0x282020001008080L,
			0x50000181204a0004L, 0x102042111804200L, 0x40002010004001c0L, 0x19220045508200L, 0x20030010060a900L, 0x8018028040080L, 0x88240002008080L,
			0x10301802830400L, 0x332a4081140200L, 0x8080010a601241L, 0x1008010400021L, 0x4082001007241L, 0x211009001200509L, 0x8015001002441801L,
			0x801000804000603L, 0xc0900220024a401L, 0x1000200608243L };
	public final static long[] BISHOP_MAGIC_NUMBER = { 0x2910054208004104L, 0x2100630a7020180L, 0x5822022042000000L, 0x2ca804a100200020L, 0x204042200000900L,
			0x2002121024000002L, 0x80404104202000e8L, 0x812a020205010840L, 0x8005181184080048L, 0x1001c20208010101L, 0x1001080204002100L, 0x1810080489021800L,
			0x62040420010a00L, 0x5028043004300020L, 0xc0080a4402605002L, 0x8a00a0104220200L, 0x940000410821212L, 0x1808024a280210L, 0x40c0422080a0598L,
			0x4228020082004050L, 0x200800400e00100L, 0x20b001230021040L, 0x90a0201900c00L, 0x4940120a0a0108L, 0x20208050a42180L, 0x1004804b280200L,
			0x2048020024040010L, 0x102c04004010200L, 0x20408204c002010L, 0x2411100020080c1L, 0x102a008084042100L, 0x941030000a09846L, 0x244100800400200L,
			0x4000901010080696L, 0x280404180020L, 0x800042008240100L, 0x220008400088020L, 0x4020182000904c9L, 0x23010400020600L, 0x41040020110302L,
			0x412101004020818L, 0x8022080a09404208L, 0x1401210240484800L, 0x22244208010080L, 0x1105040104000210L, 0x2040088800c40081L, 0x8184810252000400L,
			0x4004610041002200L, 0x40201a444400810L, 0x4611010802020008L, 0x80000b0401040402L, 0x20004821880a00L, 0x8200002022440100L, 0x9431801010068L,
			0x1040c20806108040L, 0x804901403022a40L, 0x2400202602104000L, 0x208520209440204L, 0x40c000022013020L, 0x2000104000420600L, 0x400000260142410L,
			0x800633408100500L, 0x2404080a1410L, 0x138200122002900L };

	public final static long[][] ROOK_MOVES = new long[64][];
	public final static long[] ROOK_MOVEMENT = new long[64];
	public final static int[] ROOK_SHIFT = new int[64];
	public final static long[][] BISHOP_MOVES = new long[64][];
	public final static long[] BISHOP_MOVEMENT = new long[64];
	public final static int[] BISHOP_SHIFT = new int[64];

    static {
		for (int square = 0; square < 64; square ++) {
			int rank = square >>> 3;
			int file = square & 7;
			ROOK_MOVEMENT[square] = (Bitboard.BB[Bitboard.FILE][file] | Bitboard.BB[Bitboard.RANK][rank]) & ~((file == 0 ? 0 : Bitboard.BB[Bitboard.FILE][0]) | (file == 7 ? 0 : Bitboard.BB[Bitboard.FILE][7]) | (rank == 0 ? 0 : Bitboard.BB[Bitboard.RANK][0]) | (rank == 7 ? 0 : Bitboard.BB[Bitboard.RANK][7]) | (1L << square));
			ROOK_SHIFT[square] = 64 - Long.bitCount(ROOK_MOVEMENT[square]);
			ROOK_MOVES[square] = generateRookMoves(square, ROOK_SHIFT[square]);
			BISHOP_MOVEMENT[square] = ((Bitboard.BB[Bitboard.FORWARD_DIAGONAL][square] | Bitboard.BB[Bitboard.BACKWARD_DIAGONAL][square]) ^ (1L << square)) & ~OUTER;
			BISHOP_SHIFT[square] = 64 - Long.bitCount(BISHOP_MOVEMENT[square]);
			BISHOP_MOVES[square] = generateBishopMoves(square, BISHOP_SHIFT[square]);
		}
	}

    private Magic() {}

	private static long[] calculateVariations(long movement) {
		int variationCount = (int) (1L << Long.bitCount(movement));
		long[] occupancyVariations = new long[variationCount];
		for (int variationIndex = 1; variationIndex < variationCount; variationIndex++) {
			long currentMask = movement;
			for (int i = 0; i < 32 - Integer.numberOfLeadingZeros(variationIndex); i++) {
				if (((1L << i) & variationIndex) != 0) {
					occupancyVariations[variationIndex] |= currentMask & -currentMask;
				}
				currentMask &= currentMask - 1;
			}
		}
		return occupancyVariations;
	}

	private static long[] generateRookMoves(int square, int rookShift) {
		long[] rookOccupancyVariations = calculateVariations(ROOK_MOVEMENT[square]);
		long[] rookMoves = new long[rookOccupancyVariations.length];
		for (int variationIndex = 0; variationIndex < rookOccupancyVariations.length; variationIndex++) {
			long validMoves = 0;
			int magicIndex = (int) ((rookOccupancyVariations[variationIndex] * ROOK_MAGIC_NUMBER[square]) >>> rookShift);
			for (int j = square + 8; j < 64; j += 8) {
				validMoves |= (1L << j);
				if ((rookOccupancyVariations[variationIndex] & (1L << j)) != 0) {
					break;
				}
			}
			for (int j = square - 8; j >= 0; j -= 8) {
				validMoves |= (1L << j);
				if ((rookOccupancyVariations[variationIndex] & (1L << j)) != 0) {
					break;
				}
			}
			for (int j = square + 1; j % 8 != 0; j++) {
				validMoves |= (1L << j);
				if ((rookOccupancyVariations[variationIndex] & (1L << j)) != 0) {
					break;
				}
			}
			for (int j = square - 1; j % 8 != 7 && j >= 0; j--) {
				validMoves |= (1L << j);
				if ((rookOccupancyVariations[variationIndex] & (1L << j)) != 0) {
					break;
				}
			}
			rookMoves[magicIndex] = validMoves;
		}
		return rookMoves;
	}

	private static long[] generateBishopMoves(int square, int bishopShift) {
		long[] bishopOccupancyVariations = calculateVariations(BISHOP_MOVEMENT[square]);
		long[] bishopMoves = new long[bishopOccupancyVariations.length];
		for (int variationIndex = 0; variationIndex < bishopOccupancyVariations.length; variationIndex++) {
			long validMoves = 0;
			int magicIndex = (int) ((bishopOccupancyVariations[variationIndex] * BISHOP_MAGIC_NUMBER[square]) >>> bishopShift);
			for (int j = square + 7; j % 8 != 7 && j < 64; j += 7) {
				validMoves |= (1L << j);
				if ((bishopOccupancyVariations[variationIndex] & (1L << j)) != 0) {
					break;
				}
			}
			for (int j = square + 9; j % 8 != 0 && j < 64; j += 9) {
				validMoves |= (1L << j);
				if ((bishopOccupancyVariations[variationIndex] & (1L << j)) != 0) {
					break;
				}
			}
			for (int j = square - 9; j % 8 != 7 && j >= 0; j -= 9) {
				validMoves |= (1L << j);
				if ((bishopOccupancyVariations[variationIndex] & (1L << j)) != 0) {
					break;
				}
			}
			for (int j = square - 7; j % 8 != 0 && j >= 0; j -= 7) {
				validMoves |= (1L << j);
				if ((bishopOccupancyVariations[variationIndex] & (1L << j)) != 0) {
					break;
				}
			}
			bishopMoves[magicIndex] = validMoves;
		}
		return bishopMoves;
	}

}
