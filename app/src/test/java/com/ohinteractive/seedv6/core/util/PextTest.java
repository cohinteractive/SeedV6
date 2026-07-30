package com.ohinteractive.seedv6.core.util;

import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PextTest {

	private static final int RANDOM_OCCUPANCIES_PER_SQUARE = 256;

	@Test
	void relevantMasksAndEmptyBoardAttacksMatchMagicOnEverySquare() {
		Magic.init();
		Pext.init();

		assertArrayEquals(Magic.ROOK_MOVEMENT, Pext.ROOK_MOVEMENT);
		assertArrayEquals(Magic.BISHOP_MOVEMENT, Pext.BISHOP_MOVEMENT);

		for (int square = 0; square < 64; square ++) {
			assertEquals(Magic.rookMoves(square, 0L), Pext.rookMoves(square, 0L), "rook square " + square);
			assertEquals(Magic.bishopMoves(square, 0L), Pext.bishopMoves(square, 0L), "bishop square " + square);
			assertEquals(Magic.queenMoves(square, 0L), Pext.queenMoves(square, 0L), "queen square " + square);
			assertEquals(
				Pext.rookMoves(square, 0L) | Pext.bishopMoves(square, 0L),
				Pext.queenMoves(square, 0L),
				"queen union square " + square
			);
		}
	}

	@Test
	void representativeBlockersIncludeTheFirstOccupiedSquareOnEveryRay() {
		int square = 27;
		long rookBlockers = bits(43, 11, 29, 25);
		long bishopBlockers = bits(45, 41, 13, 9);
		long occupancy = rookBlockers | bishopBlockers;
		long expectedRook = bits(35, 43, 19, 11, 28, 29, 26, 25);
		long expectedBishop = bits(36, 45, 34, 41, 20, 13, 18, 9);

		assertEquals(expectedRook, Magic.rookMoves(square, occupancy));
		assertEquals(expectedRook, Pext.rookMoves(square, occupancy));
		assertEquals(expectedBishop, Magic.bishopMoves(square, occupancy));
		assertEquals(expectedBishop, Pext.bishopMoves(square, occupancy));
		assertEquals(expectedRook | expectedBishop, Magic.queenMoves(square, occupancy));
		assertEquals(expectedRook | expectedBishop, Pext.queenMoves(square, occupancy));
	}

	@Test
	void edgeAndCornerSquaresMatchWithRepresentativeOccupancies() {
		int[] squares = {0, 7, 56, 63, 1, 6, 8, 15, 48, 55, 57, 62};
		long[] occupancies = {
			0L,
			-1L,
			0x8100001818000081L,
			0x55aa55aa55aa55aaL
		};

		for (int square : squares) {
			for (long occupancy : occupancies) {
				assertEquals(Magic.rookMoves(square, occupancy), Pext.rookMoves(square, occupancy));
				assertEquals(Magic.bishopMoves(square, occupancy), Pext.bishopMoves(square, occupancy));
				assertEquals(Magic.queenMoves(square, occupancy), Pext.queenMoves(square, occupancy));
			}
		}
	}

	@Test
	void friendlyAndEnemyPiecesAreEquivalentBlockers() {
		int square = 27;
		long blockers = bits(43, 45, 11, 9, 29, 41);

		long rookWithFriendlyBlockers = attacksWithOccupancies(square, blockers, 0L, true);
		long rookWithEnemyBlockers = attacksWithOccupancies(square, 0L, blockers, true);
		long bishopWithFriendlyBlockers = attacksWithOccupancies(square, blockers, 0L, false);
		long bishopWithEnemyBlockers = attacksWithOccupancies(square, 0L, blockers, false);

		assertEquals(rookWithFriendlyBlockers, rookWithEnemyBlockers);
		assertEquals(bishopWithFriendlyBlockers, bishopWithEnemyBlockers);
		assertNotEquals(Pext.rookMoves(square, 0L), rookWithFriendlyBlockers);
		assertNotEquals(Pext.bishopMoves(square, 0L), bishopWithFriendlyBlockers);
	}

	@Test
	void everyRookRelevantSubsetHasAUniqueDenseIndexAndMatchesMagic() {
		int expectedTableEntries = 0;
		int actualTableEntries = 0;

		for (int square = 0; square < 64; square ++) {
			long mask = Pext.ROOK_MOVEMENT[square];
			int entryCount = 1 << Long.bitCount(mask);
			boolean[] seen = new boolean[entryCount];
			expectedTableEntries += entryCount;
			actualTableEntries += Pext.ROOK_MOVES[square].length;
			assertEquals(entryCount, Pext.ROOK_MOVES[square].length, "rook table size square " + square);

			for (int index = 0; index < entryCount; index ++) {
				long blockers = Long.expand(index, mask);
				int compressedIndex = (int) Long.compress(blockers, mask);
				String context = "rook square " + square + ", index " + index;

				assertTrue(compressedIndex >= 0 && compressedIndex < entryCount, context);
				assertFalse(seen[compressedIndex], context);
				seen[compressedIndex] = true;
				assertEquals(index, compressedIndex, context);
				assertNotEquals(0L, Pext.ROOK_MOVES[square][compressedIndex], context);
				assertEquals(Pext.ROOK_MOVES[square][compressedIndex], Pext.rookMoves(square, blockers), context);
				assertEquals(Magic.rookMoves(square, blockers), Pext.rookMoves(square, blockers), context);
			}
		}

		assertEquals(102400, expectedTableEntries);
		assertEquals(expectedTableEntries, actualTableEntries);
	}

	@Test
	void everyBishopRelevantSubsetHasAUniqueDenseIndexAndMatchesMagic() {
		int expectedTableEntries = 0;
		int actualTableEntries = 0;

		for (int square = 0; square < 64; square ++) {
			long mask = Pext.BISHOP_MOVEMENT[square];
			int entryCount = 1 << Long.bitCount(mask);
			boolean[] seen = new boolean[entryCount];
			expectedTableEntries += entryCount;
			actualTableEntries += Pext.BISHOP_MOVES[square].length;
			assertEquals(entryCount, Pext.BISHOP_MOVES[square].length, "bishop table size square " + square);

			for (int index = 0; index < entryCount; index ++) {
				long blockers = Long.expand(index, mask);
				int compressedIndex = (int) Long.compress(blockers, mask);
				String context = "bishop square " + square + ", index " + index;

				assertTrue(compressedIndex >= 0 && compressedIndex < entryCount, context);
				assertFalse(seen[compressedIndex], context);
				seen[compressedIndex] = true;
				assertEquals(index, compressedIndex, context);
				assertNotEquals(0L, Pext.BISHOP_MOVES[square][compressedIndex], context);
				assertEquals(Pext.BISHOP_MOVES[square][compressedIndex], Pext.bishopMoves(square, blockers), context);
				assertEquals(Magic.bishopMoves(square, blockers), Pext.bishopMoves(square, blockers), context);
			}
		}

		assertEquals(5248, expectedTableEntries);
		assertEquals(expectedTableEntries, actualTableEntries);
	}

	@Test
	void randomFullOccupanciesWithIrrelevantBitsMatchAllMagicAttackMethods() {
		Random random = new Random(0x5eed70657874L);

		for (int square = 0; square < 64; square ++) {
			long irrelevantMask = ~(Pext.ROOK_MOVEMENT[square] | Pext.BISHOP_MOVEMENT[square]);
			long forcedIrrelevantBit = Long.lowestOneBit(irrelevantMask);

			for (int sample = 0; sample < RANDOM_OCCUPANCIES_PER_SQUARE; sample ++) {
				long occupancy = random.nextLong() | forcedIrrelevantBit;
				String context = "square " + square + ", sample " + sample;

				assertNotEquals(0L, occupancy & ~Pext.ROOK_MOVEMENT[square], context);
				assertNotEquals(0L, occupancy & ~Pext.BISHOP_MOVEMENT[square], context);
				assertEquals(Magic.rookMoves(square, occupancy), Pext.rookMoves(square, occupancy), context);
				assertEquals(Magic.bishopMoves(square, occupancy), Pext.bishopMoves(square, occupancy), context);
				assertEquals(Magic.queenMoves(square, occupancy), Pext.queenMoves(square, occupancy), context);
			}
		}
	}

	private static long attacksWithOccupancies(int square, long friendlyOccupancy, long enemyOccupancy, boolean rook) {
		long allOccupancy = friendlyOccupancy | enemyOccupancy;
		return rook ? Pext.rookMoves(square, allOccupancy) : Pext.bishopMoves(square, allOccupancy);
	}

	private static long bits(int... squares) {
		long bitboard = 0L;
		for (int square : squares) {
			bitboard |= 1L << square;
		}
		return bitboard;
	}
}
