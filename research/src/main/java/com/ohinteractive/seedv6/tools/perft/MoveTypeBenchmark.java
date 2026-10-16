package com.ohinteractive.seedv6.tools.perft;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.BoardMoveType;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.GenMoveType;

/**
 * Lightweight, dependency-free comparison for the typed-move experiment.
 * Results are indicative only; use a stable machine and repeat runs before drawing conclusions.
 */
final class MoveTypeBenchmark {

    private static final int MAX_MOVES = 256;
    private static final int DEFAULT_ITERATIONS = 200_000;
    private static final int DEFAULT_PERFT_DEPTH = 4;
    private static final String FEN =
        "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";

    public static void main(String[] args) {
        int iterations = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_ITERATIONS;
        int perftDepth = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PERFT_DEPTH;
        if(iterations <= 0) throw new IllegalArgumentException("iterations");
        if(perftDepth < 1 || perftDepth > 6) throw new IllegalArgumentException("perftDepth");

        long[] board = Board.fromFen(FEN);
        long[] typedBoard = BoardMoveType.fromFen(FEN);
        GenerationWorkspace generation = new GenerationWorkspace();

        int warmupIterations = Math.max(10_000, iterations / 5);
        int applicationIterations = Math.max(1, iterations / 10);
        benchmarkGeneration(board, warmupIterations, false, generation);
        benchmarkGeneration(typedBoard, warmupIterations, true, generation);
        benchmarkGenerationAndApplication(board, warmupIterations / 10, false, generation);
        benchmarkGenerationAndApplication(typedBoard, warmupIterations / 10, true, generation);
        PerftRecursive.count(board, perftDepth, new PerftRecursive.Workspace(perftDepth));
        PerftMoveTypeRecursive.count(
            typedBoard,
            perftDepth,
            new PerftMoveTypeRecursive.Workspace(perftDepth)
        );

        Result productionGeneration = timedGeneration(board, iterations, false, generation);
        Result typedGeneration = timedGeneration(typedBoard, iterations, true, generation);
        Result productionApplication = timedGenerationAndApplication(board, applicationIterations, false, generation);
        Result typedApplication = timedGenerationAndApplication(typedBoard, applicationIterations, true, generation);
        Result productionPerft = timedPerft(board, perftDepth, false);
        Result typedPerft = timedPerft(typedBoard, perftDepth, true);

        print("Gen", productionGeneration);
        print("GenMoveType", typedGeneration);
        printRatio("generation typed/current", typedGeneration, productionGeneration);
        print("Gen + Board", productionApplication);
        print("GenMoveType + BoardMoveType", typedApplication);
        printRatio("generation+apply typed/current", typedApplication, productionApplication);
        print("production perft depth " + perftDepth, productionPerft);
        print("typed perft depth " + perftDepth, typedPerft);
        printRatio("perft typed/current", typedPerft, productionPerft);
        if(productionPerft.value != typedPerft.value) {
            throw new AssertionError("perft mismatch: " + productionPerft.value + " != " + typedPerft.value);
        }
    }

    private static Result timedGeneration(long[] board, int iterations, boolean typed, GenerationWorkspace workspace) {
        long start = System.nanoTime();
        long value = benchmarkGeneration(board, iterations, typed, workspace);
        return new Result(System.nanoTime() - start, iterations, value);
    }

    private static Result timedGenerationAndApplication(long[] board, int iterations, boolean typed, GenerationWorkspace workspace) {
        long start = System.nanoTime();
        long value = benchmarkGenerationAndApplication(board, iterations, typed, workspace);
        return new Result(System.nanoTime() - start, iterations, value);
    }

    private static Result timedPerft(long[] board, int depth, boolean typed) {
        if(typed) {
            PerftMoveTypeRecursive.Workspace workspace =
                new PerftMoveTypeRecursive.Workspace(depth);
            long start = System.nanoTime();
            long nodes = PerftMoveTypeRecursive.count(board, depth, workspace);
            return new Result(System.nanoTime() - start, nodes, nodes);
        }
        PerftRecursive.Workspace workspace = new PerftRecursive.Workspace(depth);
        long start = System.nanoTime();
        long nodes = PerftRecursive.count(board, depth, workspace);
        return new Result(System.nanoTime() - start, nodes, nodes);
    }

    private static long benchmarkGeneration(long[] board, int iterations, boolean typed, GenerationWorkspace workspace) {
        long checksum = 0L;
        for(int iteration = 0; iteration < iterations; iteration ++) {
            int count = typed
                ? GenMoveType.genAll(
                    board[0], board[1], board[2], board[3], (int) board[Board.STATUS], board[Board.KEY],
                    true, workspace.moves, workspace.scratch
                )
                : Gen.genAll(
                    board[0], board[1], board[2], board[3], (int) board[Board.STATUS], board[Board.KEY],
                    true, workspace.moves, workspace.scratch
                );
            checksum += count;
            checksum ^= workspace.moves[iteration % count];
        }
        return checksum;
    }

    private static long benchmarkGenerationAndApplication(long[] board, int iterations, boolean typed, GenerationWorkspace workspace) {
        long checksum = 0L;
        for(int iteration = 0; iteration < iterations; iteration ++) {
            int count = typed
                ? GenMoveType.genAll(
                    board[0], board[1], board[2], board[3], (int) board[Board.STATUS], board[Board.KEY],
                    true, workspace.moves, workspace.scratch
                )
                : Gen.genAll(
                    board[0], board[1], board[2], board[3], (int) board[Board.STATUS], board[Board.KEY],
                    true, workspace.moves, workspace.scratch
                );
            for(int i = 0; i < count; i ++) {
                if(typed) {
                    BoardMoveType.makeMoveInto(
                        board[0], board[1], board[2], board[3], (int) board[Board.STATUS], board[Board.KEY],
                        workspace.moves[i], workspace.nextBoard
                    );
                } else {
                    Board.makeMoveInto(
                        board[0], board[1], board[2], board[3], (int) board[Board.STATUS], board[Board.KEY],
                        workspace.moves[i], workspace.nextBoard
                    );
                }
                checksum ^= workspace.nextBoard[Board.KEY];
            }
            checksum += count;
        }
        return checksum;
    }

    private static void print(String name, Result result) {
        double milliseconds = result.nanoseconds / 1_000_000.0;
        double nsPerOperation = (double) result.nanoseconds / result.operations;
        System.out.printf("%-34s %10.3f ms  %10.2f ns/op  checksum=%d%n",
            name, milliseconds, nsPerOperation, result.value);
    }

    private static void printRatio(String name, Result candidate, Result baseline) {
        double ratio = (double) candidate.nanoseconds / candidate.operations
            / ((double) baseline.nanoseconds / baseline.operations);
        System.out.printf("%-34s %10.3fx%n", name, ratio);
    }

    private record Result(long nanoseconds, long operations, long value) {}

    private static final class GenerationWorkspace {
        final long[] moves = new long[MAX_MOVES];
        final long[] scratch = new long[Board.MAX_BITBOARDS];
        final long[] nextBoard = new long[Board.MAX_BITBOARDS];
    }

    private MoveTypeBenchmark() {}
}
