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

	public final static long[] ROOK_MAGIC_NUMBER = {
		0x80008468400210L, 0x8200201480410200L, 0x1900100a41002000L, 0x880080004100080L, 
		0x280040008008022L, 0x200010200840810L, 0x200020000840108L, 0x200018822440201L, 
		0x4102800080c00022L, 0x1002040008101L, 0xc004808020001000L, 0x8801000080082L, 
		0x8008800400080081L, 0x5004400020900L, 0x44004210088104L, 0x201c800040800100L, 
		0x2004228000400085L, 0x40404000201000L, 0x22a0a0020804010L, 0x84020200100a2040L, 
		0x401010008001004L, 0x1024004002010040L, 0x1040010010802L, 0xd80a02000100488cL, 
		0xa0208480004000L, 0x100a400640201001L, 0x200080100080L, 0xc150040040080040L, 
		0x2014040080800800L, 0x1820040080800200L, 0x80400414210L, 0x400248200104104L, 
		0x802804004800420L, 0x10a0200040401000L, 0x802000801000L, 0x4020801000800800L, 
		0x800802803400L, 0x404020080800400L, 0x406a0b0804008630L, 0x41018a000344L, 
		0x2000804000208000L, 0x9a00810040030028L, 0xc400200010008080L, 0x2010040008004040L, 
		0x4a8880100050011L, 0xc000402008080L, 0x4080120831040010L, 0x201444500860004L, 
		0x3000244881020200L, 0xcc01020622904200L, 0x880914200aa00L, 0x800200a02411200L, 
		0x48080004008080L, 0x804002030380c01L, 0x9208821801304400L, 0x1000880510c0200L, 
		0x41048000102441L, 0x810c801021004001L, 0x104102941022001L, 0x800090020041001L, 
		0x180900c410020801L, 0x5000844004201L, 0x4080108104a0884L, 0x1006068b00440822L
	};

	public final static long[] BISHOP_MAGIC_NUMBER = {
		0x4141000410302L, 0x1114100c01112003L, 0x4d04440482000860L, 0xc404180011012L, 
		0x4102021000000485L, 0x1010840410042L, 0x1004009209210002L, 0x1010061200822L, 
		0x208410108104L, 0x80000410c2004300L, 0x154008a200420002L, 0x8080849001000L, 
		0x20210014408L, 0x48020210462208L, 0x10008840402c168L, 0x410a020201010888L, 
		0x206000c084018214L, 0x2202002414480220L, 0x408800100a6a6020L, 0x4028000082014020L, 
		0x120c005080e00060L, 0x28802808040200L, 0x11000200900400L, 0x2000400082109040L, 
		0x10080111600140L, 0x102902008010820L, 0x4040808842400L, 0x4004004010002L, 
		0x1001011004000L, 0x100808018080441L, 0x4002022400809000L, 0x20c005014250c00L, 
		0x404104400c00484L, 0x902028200200820L, 0xa02021c010800L, 0x2020080080080L, 
		0x540440240100L, 0x100e0200808888L, 0x40080a4400004900L, 0x8004040922124910L, 
		0x4010822020001020L, 0x4000421004005041L, 0x42020201000204L, 0xc0004200808801L, 
		0x222401009014220L, 0x202200424210100L, 0x2008024808400a00L, 0x410062140548501L, 
		0x1028c1008044041L, 0x2004c42080001L, 0x4004a08040000L, 0x20020084110298L, 
		0x80010c008220800L, 0x200043070024380L, 0x12203204004600L, 0x10444100420028L, 
		0x10808400a00408L, 0x18b842010488cc04L, 0x1000800080580810L, 0x20009011820a800L, 
		0x802000008030c04L, 0x804050060194L, 0x204882980440L, 0xc0045810450020L
	};

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
