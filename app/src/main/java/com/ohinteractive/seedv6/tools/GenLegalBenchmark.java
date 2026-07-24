package com.ohinteractive.seedv6.tools;

import java.util.Arrays;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.GenLegal;

/*
 * Isolated move-generation microbenchmark. This class is intentionally not
 * connected to search or normal engine execution.
 */
public final class GenLegalBenchmark {

    private static final int MAX_MOVES = 256;
    private static final int PATH_ALL = 0;
    private static final int PATH_STAGED = 1;
    private static final int PATH_EVASION = 2;

    private static volatile long sink;

    public static void main(String[] args) {
        final int iterations = args.length > 0 ? Integer.parseInt(args[0]) : 200000;
        final int warmupRounds = args.length > 1 ? Integer.parseInt(args[1]) : 4;
        final int measurementRounds = args.length > 2 ? Integer.parseInt(args[2]) : 7;
        if(iterations <= 0 || warmupRounds < 0 || measurementRounds <= 0) {
            throw new IllegalArgumentException("iterations, warmupRounds, measurementRounds");
        }

        System.out.println(
            "GenLegal benchmark: " + iterations + " iterations, "
            + warmupRounds + " warmups, " + measurementRounds + " measurements"
        );
        benchmarkPosition(
            "no check (Kiwipete)",
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
            iterations, warmupRounds, measurementRounds
        );
        benchmarkPosition(
            "single rook check",
            "4r2k/8/8/8/8/8/8/R3K3 w - - 0 1",
            iterations, warmupRounds, measurementRounds
        );
        benchmarkPosition(
            "double check",
            "4r2k/8/8/8/1b6/8/8/4K3 w - - 0 1",
            iterations, warmupRounds, measurementRounds
        );
        System.out.println("sink: " + sink);
    }

    private static void benchmarkPosition(String name, String fen, int iterations, int warmupRounds, int measurementRounds) {
        final long[] board = Board.fromFen(fen);
        final long checkers = getCheckers(board);
        System.out.println();
        System.out.println(name + " (" + Long.bitCount(checkers) + " checker(s))");
        benchmarkPath("all", board, checkers, PATH_ALL, iterations, warmupRounds, measurementRounds);
        benchmarkPath("staged", board, checkers, PATH_STAGED, iterations, warmupRounds, measurementRounds);
        if(checkers != 0L) {
            benchmarkPath("evasion", board, checkers, PATH_EVASION, iterations, warmupRounds, measurementRounds);
        }
    }

    private static void benchmarkPath(
        String name, long[] board, long checkers, int path,
        int iterations, int warmupRounds, int measurementRounds
    ) {
        final long[] moves = new long[MAX_MOVES];
        final long[] quietMoves = new long[MAX_MOVES];
        final long[] boardBuffer = new long[Board.MAX_BITBOARDS];
        for(int round = 0; round < warmupRounds; round ++) {
            runGen(board, checkers, path, iterations, moves, quietMoves, boardBuffer);
            runGenLegal(board, checkers, path, iterations, moves, quietMoves, boardBuffer);
        }

        final long[] genTimes = new long[measurementRounds];
        final long[] legalTimes = new long[measurementRounds];
        for(int round = 0; round < measurementRounds; round ++) {
            if((round & 1) == 0) {
                genTimes[round] = timeGen(board, checkers, path, iterations, moves, quietMoves, boardBuffer);
                legalTimes[round] = timeGenLegal(board, checkers, path, iterations, moves, quietMoves, boardBuffer);
            } else {
                legalTimes[round] = timeGenLegal(board, checkers, path, iterations, moves, quietMoves, boardBuffer);
                genTimes[round] = timeGen(board, checkers, path, iterations, moves, quietMoves, boardBuffer);
            }
        }

        System.out.println("  " + name);
        for(int round = 0; round < measurementRounds; round ++) {
            System.out.printf(
                "    %d: Gen %.1f ns/op, GenLegal %.1f ns/op%n",
                round + 1,
                (double) genTimes[round] / iterations,
                (double) legalTimes[round] / iterations
            );
        }
        final double genMedian = (double) median(genTimes) / iterations;
        final double legalMedian = (double) median(legalTimes) / iterations;
        System.out.printf(
            "    median: Gen %.1f ns/op, GenLegal %.1f ns/op, ratio %.3f%n",
            genMedian, legalMedian, legalMedian / genMedian
        );
    }

    private static long timeGen(
        long[] board, long checkers, int path, int iterations,
        long[] moves, long[] quietMoves, long[] boardBuffer
    ) {
        final long start = System.nanoTime();
        runGen(board, checkers, path, iterations, moves, quietMoves, boardBuffer);
        return System.nanoTime() - start;
    }

    private static long timeGenLegal(
        long[] board, long checkers, int path, int iterations,
        long[] moves, long[] quietMoves, long[] boardBuffer
    ) {
        final long start = System.nanoTime();
        runGenLegal(board, checkers, path, iterations, moves, quietMoves, boardBuffer);
        return System.nanoTime() - start;
    }

    private static void runGen(
        long[] board, long checkers, int path, int iterations,
        long[] moves, long[] quietMoves, long[] boardBuffer
    ) {
        final long board0 = board[0];
        final long board1 = board[1];
        final long board2 = board[2];
        final long board3 = board[3];
        final int status = (int) board[Board.STATUS];
        final long key = board[Board.KEY];
        long checksum = 0L;
        for(int i = 0; i < iterations; i ++) {
            final int count;
            if(path == PATH_ALL) {
                count = Gen.genAll(
                    board0, board1, board2, board3, status, key, true,
                    moves, boardBuffer
                );
            } else if(path == PATH_STAGED) {
                count = Gen.genTactical(
                    board0, board1, board2, board3, status, key, true,
                    moves, boardBuffer
                ) + Gen.genQuiet(
                    board0, board1, board2, board3, status, key, true,
                    quietMoves, boardBuffer
                );
            } else {
                count = Gen.genEvasion(
                    board0, board1, board2, board3, status, key, true,
                    checkers, moves, boardBuffer
                );
            }
            checksum += count;
            if(count != 0) checksum ^= moves[0];
        }
        sink += checksum;
    }

    private static void runGenLegal(
        long[] board, long checkers, int path, int iterations,
        long[] moves, long[] quietMoves, long[] boardBuffer
    ) {
        final long board0 = board[0];
        final long board1 = board[1];
        final long board2 = board[2];
        final long board3 = board[3];
        final int status = (int) board[Board.STATUS];
        final long key = board[Board.KEY];
        long checksum = 0L;
        for(int i = 0; i < iterations; i ++) {
            final int count;
            if(path == PATH_ALL) {
                count = GenLegal.genAll(
                    board0, board1, board2, board3, status, key, true,
                    moves, boardBuffer
                );
            } else if(path == PATH_STAGED) {
                count = GenLegal.genTactical(
                    board0, board1, board2, board3, status, key, true,
                    moves, boardBuffer
                ) + GenLegal.genQuiet(
                    board0, board1, board2, board3, status, key, true,
                    quietMoves, boardBuffer
                );
            } else {
                count = GenLegal.genEvasion(
                    board0, board1, board2, board3, status, key, true,
                    checkers, moves, boardBuffer
                );
            }
            checksum += count;
            if(count != 0) checksum ^= moves[0];
        }
        sink += checksum;
    }

    private static long getCheckers(long[] board) {
        final long board0 = board[0];
        final long board1 = board[1];
        final long board2 = board[2];
        final long board3 = board[3];
        final int player = (int) board[Board.STATUS] & Board.PLAYER_BIT;
        final long colorMask = ~(-(player) ^ board3);
        final long king = board0 & ~board1 & ~board2 & colorMask;
        final int kingSquare = Long.numberOfTrailingZeros(king);
        return Board.getCheckers(
            board0, board1, board2, board3,
            colorMask, player, kingSquare, board0 | board1 | board2
        );
    }

    private static long median(long[] values) {
        long[] sorted = Arrays.copyOf(values, values.length);
        Arrays.sort(sorted);
        return sorted[sorted.length >>> 1];
    }

    private GenLegalBenchmark() {}

}
