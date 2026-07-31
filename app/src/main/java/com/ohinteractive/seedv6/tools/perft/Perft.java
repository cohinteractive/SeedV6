package com.ohinteractive.seedv6.tools.perft;

import java.util.List;
import java.util.Objects;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.BoardMoveType;

public final class Perft {

    private static final int MAX_DEPTH = 64;

    public enum Implementation {
        RECURSIVE,
        FLAT,
        MOVE_TYPE_RECURSIVE,
        MOVE_TYPE_FLAT
    }

    public enum Concurrency {
        SERIAL,
        CONCURRENT
    }

    public static final Implementation RECURSIVE = Implementation.RECURSIVE;
    public static final Implementation FLAT = Implementation.FLAT;
    public static final Implementation MOVE_TYPE_RECURSIVE = Implementation.MOVE_TYPE_RECURSIVE;
    public static final Implementation MOVE_TYPE_FLAT = Implementation.MOVE_TYPE_FLAT;

    public static final Concurrency USE_CONCURRENCY = Concurrency.CONCURRENT;
    public static final Concurrency NO_CONCURRENCY = Concurrency.SERIAL;

    public static final int MAX_CPUS = Math.max(1, Runtime.getRuntime().availableProcessors());

    public static void main(String[] args) {
        runPerft(PerftPositions.DEFAULT.all(), FLAT, USE_CONCURRENCY, MAX_CPUS);
    }

    public static void runPerft(
        PerftSuite suite,
        Implementation implementation,
        Concurrency concurrency,
        int cpuCount
    ) {
        Objects.requireNonNull(suite, "suite");
        Objects.requireNonNull(implementation, "implementation");
        Objects.requireNonNull(concurrency, "concurrency");
        validateCpuCount(cpuCount);
        List<PerftCase> cases = suite.cases();
        for(PerftCase perftCase : cases) validateDepth(perftCase.depth());

        int workerCount = concurrency == Concurrency.CONCURRENT ? cpuCount : 1;
        long totalElapsedMs = 0L;
        for(int i = 0; i < cases.size(); i ++) {
            PerftCase perftCase = cases.get(i);
            System.out.println(perftCase.position().name());
            long elapsedMs = runCase(perftCase, implementation, concurrency, workerCount);
            totalElapsedMs += elapsedMs;
            if(i + 1 < cases.size()) System.out.println();
        }
        System.out.println("All positions complete.");
        System.out.println("Total elapsed: " + format(totalElapsedMs) + " ms");
    }

    private static long runCase(
        PerftCase perftCase,
        Implementation implementation,
        Concurrency concurrency,
        int workerCount
    ) {
        String fen = perftCase.position().fen();
        int depth = perftCase.depth();
        System.out.println("\"" + fen + "\"");
        long[] board = usesMoveTypeBoard(implementation)
            ? BoardMoveType.fromFen(fen)
            : Board.fromFen(fen);
        long start = System.nanoTime();
        long nodes = runBoard(
            board,
            depth,
            implementation,
            concurrency,
            workerCount
        );
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
        validateResult(nodes, perftCase.expectedNodes());
        return elapsedMs;
    }

    public static long run(long[] board, int maxDepth) {
        return run(board, maxDepth, FLAT, USE_CONCURRENCY, MAX_CPUS);
    }

    public static long run(
        long[] board,
        int maxDepth,
        Implementation implementation,
        Concurrency concurrency,
        int cpuCount
    ) {
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(implementation, "implementation");
        Objects.requireNonNull(concurrency, "concurrency");
        validateDepth(maxDepth);
        validateCpuCount(cpuCount);
        return runBoard(
            board,
            maxDepth,
            implementation,
            concurrency,
            concurrency == Concurrency.CONCURRENT ? cpuCount : 1
        );
    }

    private static long runBoard(
        long[] board,
        int maxDepth,
        Implementation implementation,
        Concurrency concurrency,
        int workerCount
    ) {
        PerftRecursive.Workspace recursiveWorkspace = implementation == Implementation.RECURSIVE
            ? new PerftRecursive.Workspace(maxDepth)
            : null;
        PerftFlat.Workspace flatWorkspace = implementation == Implementation.FLAT
            ? new PerftFlat.Workspace(maxDepth)
            : null;
        PerftMoveTypeRecursive.Workspace moveTypeRecursiveWorkspace =
            implementation == Implementation.MOVE_TYPE_RECURSIVE
                ? new PerftMoveTypeRecursive.Workspace(maxDepth)
                : null;
        PerftMoveTypeFlat.Workspace moveTypeFlatWorkspace =
            implementation == Implementation.MOVE_TYPE_FLAT
                ? new PerftMoveTypeFlat.Workspace(maxDepth)
                : null;
        long lastNodes = 0L;
        long lastElapsedNs = 0L;
        for(int depth = 1; depth <= maxDepth; depth ++) {
            long start = System.nanoTime();
            boolean concurrentDepth =
                concurrency == Concurrency.CONCURRENT && depth == maxDepth && depth > 1;
            long nodes = count(
                board,
                depth,
                implementation,
                concurrentDepth,
                workerCount,
                recursiveWorkspace,
                flatWorkspace,
                moveTypeRecursiveWorkspace,
                moveTypeFlatWorkspace
            );
            long elapsedNs = System.nanoTime() - start;
            long elapsedMs = elapsedNs / 1_000_000L;
            lastNodes = nodes;
            lastElapsedNs = elapsedNs;
            System.out.println(
                "Depth " + depth + "/" + maxDepth + ": " +
                format(nodes) + " nodes in " + elapsedMs + " ms"
            );
        }
        long nps = lastElapsedNs > 0L
            ? lastNodes * 1_000_000_000L / lastElapsedNs
            : lastNodes;
        System.out.println("Nodes per second: " + format(nps));
        return lastNodes;
    }

    private static long count(
        long[] board,
        int depth,
        Implementation implementation,
        boolean concurrent,
        int workerCount,
        PerftRecursive.Workspace recursiveWorkspace,
        PerftFlat.Workspace flatWorkspace,
        PerftMoveTypeRecursive.Workspace moveTypeRecursiveWorkspace,
        PerftMoveTypeFlat.Workspace moveTypeFlatWorkspace
    ) {
        return switch(implementation) {
            case RECURSIVE -> concurrent
                ? PerftRecursive.countConcurrent(board, depth, workerCount)
                : PerftRecursive.count(board, depth, recursiveWorkspace);
            case FLAT -> concurrent
                ? PerftFlat.countConcurrent(board, depth, workerCount)
                : PerftFlat.count(board, depth, flatWorkspace);
            case MOVE_TYPE_RECURSIVE -> concurrent
                ? PerftMoveTypeRecursive.countConcurrent(board, depth, workerCount)
                : PerftMoveTypeRecursive.count(board, depth, moveTypeRecursiveWorkspace);
            case MOVE_TYPE_FLAT -> concurrent
                ? PerftMoveTypeFlat.countConcurrent(board, depth, workerCount)
                : PerftMoveTypeFlat.count(board, depth, moveTypeFlatWorkspace);
        };
    }

    private static boolean usesMoveTypeBoard(Implementation implementation) {
        return implementation == Implementation.MOVE_TYPE_RECURSIVE
            || implementation == Implementation.MOVE_TYPE_FLAT;
    }

    private static void validateResult(long nodes, long expectedNodes) {
        if(nodes == expectedNodes) {
            System.out.println("Result: PASSED");
            return;
        }
        long diff = nodes - expectedNodes;
        System.out.println(
            "Result: FAILED expected " + format(expectedNodes) +
            ", got " + format(nodes) +
            ", diff " + format(diff)
        );
    }

    private static void validateDepth(int depth) {
        if(depth < 0 || depth > MAX_DEPTH) throw new IllegalArgumentException("depth");
    }

    private static void validateCpuCount(int cpuCount) {
        if(cpuCount <= 0) {
            throw new IllegalArgumentException("cpuCount must be positive: " + cpuCount);
        }
    }

    private static String format(long value) {
        return String.format("%,d", value);
    }

    private Perft() {}
}
