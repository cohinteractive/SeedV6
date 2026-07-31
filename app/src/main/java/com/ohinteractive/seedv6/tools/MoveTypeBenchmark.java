package com.ohinteractive.seedv6.tools;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.BoardMoveType;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.GenMoveType;

/**
 * Lightweight, dependency-free comparison for the typed-move experiment.
 * Results are indicative only; use a stable machine and repeat runs before drawing conclusions.
 */
public final class MoveTypeBenchmark {

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
        productionPerft(board, perftDepth, new PerftWorkspace(perftDepth));
        experimentalPerft(typedBoard, perftDepth, new PerftWorkspace(perftDepth));

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
        PerftWorkspace workspace = new PerftWorkspace(depth);
        long start = System.nanoTime();
        long nodes = typed
            ? experimentalPerft(board, depth, workspace)
            : productionPerft(board, depth, workspace);
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

    public static long productionPerft(long[] board, int depth, PerftWorkspace workspace) {
        return productionPerft(board, depth, 0, workspace);
    }

    private static long productionPerft(long[] board, int depth, int ply, PerftWorkspace workspace) {
        if(depth == 0) return 1L;
        long[] moves = workspace.moves[ply];
        int count = Gen.genAll(
            board[0], board[1], board[2], board[3], (int) board[Board.STATUS], board[Board.KEY],
            true, moves, workspace.scratch
        );
        if(depth == 1) return count;
        long nodes = 0L;
        long[] nextBoard = workspace.boards[ply + 1];
        for(int i = 0; i < count; i ++) {
            Board.makeMoveInto(
                board[0], board[1], board[2], board[3], (int) board[Board.STATUS], board[Board.KEY],
                moves[i], nextBoard
            );
            nodes += productionPerft(nextBoard, depth - 1, ply + 1, workspace);
        }
        return nodes;
    }

    public static long experimentalPerft(long[] board, int depth, PerftWorkspace workspace) {
        return experimentalPerft(board, depth, 0, workspace);
    }

    private static long experimentalPerft(long[] board, int depth, int ply, PerftWorkspace workspace) {
        if(depth == 0) return 1L;
        long[] moves = workspace.moves[ply];
        int count = GenMoveType.genAll(
            board[0], board[1], board[2], board[3], (int) board[Board.STATUS], board[Board.KEY],
            true, moves, workspace.scratch
        );
        if(depth == 1) return count;
        long nodes = 0L;
        long[] nextBoard = workspace.boards[ply + 1];
        for(int i = 0; i < count; i ++) {
            BoardMoveType.makeMoveInto(
                board[0], board[1], board[2], board[3], (int) board[Board.STATUS], board[Board.KEY],
                moves[i], nextBoard
            );
            nodes += experimentalPerft(nextBoard, depth - 1, ply + 1, workspace);
        }
        return nodes;
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

    public static final class PerftWorkspace {
        final long[][] boards;
        final long[][] moves;
        final long[] scratch = new long[Board.MAX_BITBOARDS];

        public PerftWorkspace(int depth) {
            if(depth < 0 || depth > 64) throw new IllegalArgumentException("depth");
            boards = new long[depth + 1][Board.MAX_BITBOARDS];
            moves = new long[depth + 1][MAX_MOVES];
        }
    }

    private MoveTypeBenchmark() {}
}
