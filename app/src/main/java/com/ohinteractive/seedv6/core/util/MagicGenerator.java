package com.ohinteractive.seedv6.core.util;

import java.util.Arrays;
import java.util.SplittableRandom;

public final class MagicGenerator {

    private static final long OUTER = 0xff818181818181ffL;

    /*
     * Fixed seed so generation is deterministic.
     *
     * Change this if you want a different set of magic numbers.
     */
    private static final long SEED = 0x0123456789abcdefL;

    /*
     * Some squares can be stubborn. If generation fails, increase this.
     */
    private static final int MAX_ATTEMPTS_PER_SQUARE = 100_000_000;

    private MagicGenerator() {}

    public static void main(String[] args) {
        System.out.println("Generating rook magics...");
        long[] rookMagics = generateRookMagics();

        System.out.println("Generating bishop magics...");
        long[] bishopMagics = generateBishopMagics();

        System.out.println();
        printArray("ROOK_MAGIC_NUMBER", rookMagics);
        System.out.println();
        printArray("BISHOP_MAGIC_NUMBER", bishopMagics);

        System.out.println();
        System.out.println("Validating generated magics...");
        validateRookMagics(rookMagics);
        validateBishopMagics(bishopMagics);
        System.out.println("Validation passed.");
    }

    private static long[] generateRookMagics() {
        long[] magics = new long[64];
        SplittableRandom random = new SplittableRandom(SEED ^ 0x9e3779b97f4a7c15L);

        for (int square = 0; square < 64; square++) {
            long movement = rookMovement(square);
            int shift = 64 - Long.bitCount(movement);
            magics[square] = findMagic(square, movement, shift, true, random);

            System.out.printf(
                "rook square %2d: 0x%016xL%n",
                square,
                magics[square]
            );
        }

        return magics;
    }

    private static long[] generateBishopMagics() {
        long[] magics = new long[64];
        SplittableRandom random = new SplittableRandom(SEED ^ 0xfedcba9876543210L);

        for (int square = 0; square < 64; square++) {
            long movement = bishopMovement(square);
            int shift = 64 - Long.bitCount(movement);
            magics[square] = findMagic(square, movement, shift, false, random);

            System.out.printf(
                "bishop square %2d: 0x%016xL%n",
                square,
                magics[square]
            );
        }

        return magics;
    }

    private static long findMagic(
        int square,
        long movement,
        int shift,
        boolean rook,
        SplittableRandom random
    ) {
        long[] occupancies = calculateVariations(movement);
        long[] attacks = new long[occupancies.length];

        for (int i = 0; i < occupancies.length; i++) {
            attacks[i] = rook
                ? slowRookMoves(square, occupancies[i])
                : slowBishopMoves(square, occupancies[i]);
        }

        int tableSize = 1 << Long.bitCount(movement);
        long[] used = new long[tableSize];
        boolean[] filled = new boolean[tableSize];

        for (int attempt = 0; attempt < MAX_ATTEMPTS_PER_SQUARE; attempt++) {
            long magic = randomSparseLong(random);

            /*
             * Cheap rejection heuristic.
             *
             * Good magics tend to spread the relevant occupancy bits into the
             * high bits. This avoids testing many obviously weak candidates.
             */
            if (Long.bitCount((movement * magic) & 0xff00000000000000L) < 6) {
                continue;
            }

            Arrays.fill(filled, false);

            boolean failed = false;

            for (int i = 0; i < occupancies.length; i++) {
                int index = (int) ((occupancies[i] * magic) >>> shift);

                if (!filled[index]) {
                    filled[index] = true;
                    used[index] = attacks[i];
                } else if (used[index] != attacks[i]) {
                    failed = true;
                    break;
                }
            }

            if (!failed) {
                return magic;
            }
        }

        throw new IllegalStateException(
            "Failed to find " + (rook ? "rook" : "bishop")
            + " magic for square " + square
            + " after " + MAX_ATTEMPTS_PER_SQUARE + " attempts"
        );
    }

    private static long randomSparseLong(SplittableRandom random) {
        return random.nextLong() & random.nextLong() & random.nextLong();
    }

    private static void validateRookMagics(long[] magics) {
        for (int square = 0; square < 64; square++) {
            long movement = rookMovement(square);
            int shift = 64 - Long.bitCount(movement);

            validateMagic(square, movement, shift, magics[square], true);
        }
    }

    private static void validateBishopMagics(long[] magics) {
        for (int square = 0; square < 64; square++) {
            long movement = bishopMovement(square);
            int shift = 64 - Long.bitCount(movement);

            validateMagic(square, movement, shift, magics[square], false);
        }
    }

    private static void validateMagic(
        int square,
        long movement,
        int shift,
        long magic,
        boolean rook
    ) {
        long[] occupancies = calculateVariations(movement);
        int tableSize = 1 << Long.bitCount(movement);

        long[] table = new long[tableSize];
        boolean[] filled = new boolean[tableSize];

        for (long occupancy : occupancies) {
            long expected = rook
                ? slowRookMoves(square, occupancy)
                : slowBishopMoves(square, occupancy);

            int index = (int) ((occupancy * magic) >>> shift);

            if (!filled[index]) {
                filled[index] = true;
                table[index] = expected;
            } else if (table[index] != expected) {
                throw new IllegalStateException(
                    "Destructive collision for "
                    + (rook ? "rook" : "bishop")
                    + " square " + square
                    + " index " + index
                );
            }
        }

        for (long occupancy : occupancies) {
            long expected = rook
                ? slowRookMoves(square, occupancy)
                : slowBishopMoves(square, occupancy);

            int index = (int) ((occupancy * magic) >>> shift);
            long actual = table[index];

            if (actual != expected) {
                throw new IllegalStateException(
                    "Validation failed for "
                    + (rook ? "rook" : "bishop")
                    + " square " + square
                );
            }
        }
    }

    private static long[] calculateVariations(long movement) {
        int bitCount = Long.bitCount(movement);
        int variationCount = 1 << bitCount;

        long[] bits = new long[bitCount];
        long current = movement;

        for (int i = 0; i < bitCount; i++) {
            bits[i] = current & -current;
            current &= current - 1;
        }

        long[] variations = new long[variationCount];

        for (int variationIndex = 0; variationIndex < variationCount; variationIndex++) {
            long occupancy = 0L;

            for (int bitIndex = 0; bitIndex < bitCount; bitIndex++) {
                if ((variationIndex & (1 << bitIndex)) != 0) {
                    occupancy |= bits[bitIndex];
                }
            }

            variations[variationIndex] = occupancy;
        }

        return variations;
    }

    private static long rookMovement(int square) {
        int rank = square >>> 3;
        int file = square & 7;

        return (
            Bitboard.BB[Bitboard.FILE][file]
            | Bitboard.BB[Bitboard.RANK][rank]
        ) & ~(
            (file == 0 ? 0L : Bitboard.BB[Bitboard.FILE][0])
            | (file == 7 ? 0L : Bitboard.BB[Bitboard.FILE][7])
            | (rank == 0 ? 0L : Bitboard.BB[Bitboard.RANK][0])
            | (rank == 7 ? 0L : Bitboard.BB[Bitboard.RANK][7])
            | (1L << square)
        );
    }

    private static long bishopMovement(int square) {
        return (
            (
                Bitboard.BB[Bitboard.FORWARD_DIAGONAL][square]
                | Bitboard.BB[Bitboard.BACKWARD_DIAGONAL][square]
            ) ^ (1L << square)
        ) & ~OUTER;
    }

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

        for (int target = square + 1; target < 64 && target % 8 != 0; target++) {
            moves |= 1L << target;

            if ((occupancy & (1L << target)) != 0L) {
                break;
            }
        }

        for (int target = square - 1; target >= 0 && target % 8 != 7; target--) {
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

    private static void printArray(String name, long[] values) {
        System.out.println("public final static long[] " + name + " = {");

        for (int i = 0; i < values.length; i++) {
            if (i % 4 == 0) {
                System.out.print("    ");
            }

            System.out.print("0x" + Long.toHexString(values[i]) + "L");

            if (i != values.length - 1) {
                System.out.print(", ");
            }

            if (i % 4 == 3 || i == values.length - 1) {
                System.out.println();
            }
        }

        System.out.println("};");
    }
}
