package com.ohinteractive.seedv6.core.util;

public class Pext {

	public static long rookMoves(int square, long allOccupancy) {
		return ROOK_MOVES[square][(int) Long.compress(allOccupancy, ROOK_MOVEMENT[square])];
	}

	public static long bishopMoves(int square, long allOccupancy) {
		return BISHOP_MOVES[square][(int) Long.compress(allOccupancy, BISHOP_MOVEMENT[square])];
	}

	public static long queenMoves(int square, long allOccupancy) {
		return ROOK_MOVES[square][(int) Long.compress(allOccupancy, ROOK_MOVEMENT[square])]
		| BISHOP_MOVES[square][(int) Long.compress(allOccupancy, BISHOP_MOVEMENT[square])];
	}

	public static void init() {}

	private final static long OUTER = 0xff818181818181ffL;

	public final static long[][] ROOK_MOVES = new long[64][];
	public final static long[] ROOK_MOVEMENT = new long[64];
	public final static long[][] BISHOP_MOVES = new long[64][];
	public final static long[] BISHOP_MOVEMENT = new long[64];

	static {
		for (int square = 0; square < 64; square ++) {
			int rank = square >>> 3;
			int file = square & 7;
			ROOK_MOVEMENT[square] = (Bitboard.BB[Bitboard.FILE][file] | Bitboard.BB[Bitboard.RANK][rank]) & ~((file == 0 ? 0 : Bitboard.BB[Bitboard.FILE][0]) | (file == 7 ? 0 : Bitboard.BB[Bitboard.FILE][7]) | (rank == 0 ? 0 : Bitboard.BB[Bitboard.RANK][0]) | (rank == 7 ? 0 : Bitboard.BB[Bitboard.RANK][7]) | (1L << square));
			ROOK_MOVES[square] = generateRookMoves(square);
			BISHOP_MOVEMENT[square] = ((Bitboard.BB[Bitboard.FORWARD_DIAGONAL][square] | Bitboard.BB[Bitboard.BACKWARD_DIAGONAL][square]) ^ (1L << square)) & ~OUTER;
			BISHOP_MOVES[square] = generateBishopMoves(square);
		}
	}

	private Pext() {}

	private static long[] generateRookMoves(int square) {
		long movement = ROOK_MOVEMENT[square];
		int entryCount = 1 << Long.bitCount(movement);
		long[] rookMoves = new long[entryCount];
		for (int index = 0; index < entryCount; index ++) {
			long blockers = Long.expand(index, movement);
			rookMoves[index] = slowRookMoves(square, blockers);
		}
		return rookMoves;
	}

	private static long[] generateBishopMoves(int square) {
		long movement = BISHOP_MOVEMENT[square];
		int entryCount = 1 << Long.bitCount(movement);
		long[] bishopMoves = new long[entryCount];
		for (int index = 0; index < entryCount; index ++) {
			long blockers = Long.expand(index, movement);
			bishopMoves[index] = slowBishopMoves(square, blockers);
		}
		return bishopMoves;
	}

	/*
	 * Magic's reference ray generators are private. Keep equivalent traversal
	 * here so Magic's implementation and visibility remain unchanged.
	 */
	private static long slowRookMoves(int square, long occupancy) {
		long moves = 0L;
		for (int target = square + 8; target < 64; target += 8) {
			moves |= 1L << target;
			if ((occupancy & (1L << target)) != 0L) {
				break;
			}
		}
		for (int target = square - 8; target >= 0; target -= 8) {
			moves |= 1L << target;
			if ((occupancy & (1L << target)) != 0L) {
				break;
			}
		}
		for (int target = square + 1; target < 64 && target % 8 != 0; target ++) {
			moves |= 1L << target;
			if ((occupancy & (1L << target)) != 0L) {
				break;
			}
		}
		for (int target = square - 1; target >= 0 && target % 8 != 7; target --) {
			moves |= 1L << target;
			if ((occupancy & (1L << target)) != 0L) {
				break;
			}
		}
		return moves;
	}

	private static long slowBishopMoves(int square, long occupancy) {
		long moves = 0L;
		for (int target = square + 7; target < 64 && target % 8 != 7; target += 7) {
			moves |= 1L << target;
			if ((occupancy & (1L << target)) != 0L) {
				break;
			}
		}
		for (int target = square + 9; target < 64 && target % 8 != 0; target += 9) {
			moves |= 1L << target;
			if ((occupancy & (1L << target)) != 0L) {
				break;
			}
		}
		for (int target = square - 9; target >= 0 && target % 8 != 7; target -= 9) {
			moves |= 1L << target;
			if ((occupancy & (1L << target)) != 0L) {
				break;
			}
		}
		for (int target = square - 7; target >= 0 && target % 8 != 0; target -= 7) {
			moves |= 1L << target;
			if ((occupancy & (1L << target)) != 0L) {
				break;
			}
		}
		return moves;
	}
}
