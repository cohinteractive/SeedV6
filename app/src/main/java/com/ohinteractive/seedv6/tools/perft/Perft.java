package com.ohinteractive.seedv6.tools.perft;

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

    public static final class PositionSelection {
        private final int startIndex;
        private final int endIndex;

        private PositionSelection(int startIndex, int endIndex) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }

        public int firstPosition() {
            return startIndex + 1;
        }

        public int lastPosition() {
            return endIndex + 1;
        }
    }

    public static final Implementation RECURSIVE = Implementation.RECURSIVE;
    public static final Implementation FLAT = Implementation.FLAT;
    public static final Implementation MOVE_TYPE_RECURSIVE = Implementation.MOVE_TYPE_RECURSIVE;
    public static final Implementation MOVE_TYPE_FLAT = Implementation.MOVE_TYPE_FLAT;

    public static final Concurrency USE_CONCURRENCY = Concurrency.CONCURRENT;
    public static final Concurrency NO_CONCURRENCY = Concurrency.SERIAL;

    public static final int MAX_CPUS = Math.max(1, Runtime.getRuntime().availableProcessors());

    public static void main(String[] args) {
        runPerft(ALL_POSITIONS, FLAT, USE_CONCURRENCY, MAX_CPUS);
    }

    public static PositionSelection position(int positionNumber) {
        return positions(positionNumber, positionNumber);
    }

    public static PositionSelection positions(int firstPosition, int lastPosition) {
        int startIndex = firstPosition - 1;
        int endIndex = lastPosition - 1;
        if(startIndex < 0 || endIndex >= POSITION_NAMES.length || startIndex > endIndex) {
            throw new IllegalArgumentException(
                "Invalid position range: " + firstPosition + " to " + lastPosition
            );
        }
        return new PositionSelection(startIndex, endIndex);
    }

    public static void runPerft(
        PositionSelection positions,
        Implementation implementation,
        Concurrency concurrency,
        int cpuCount
    ) {
        Objects.requireNonNull(positions, "positions");
        Objects.requireNonNull(implementation, "implementation");
        Objects.requireNonNull(concurrency, "concurrency");
        validateCpuCount(cpuCount);

        int workerCount = concurrency == Concurrency.CONCURRENT ? cpuCount : 1;
        long totalElapsedMs = 0L;
        for(int i = positions.startIndex; i <= positions.endIndex; i ++) {
            System.out.println(POSITION_NAMES[i]);
            long elapsedMs = runSinglePosition(
                POSITION_FENS[i],
                DEPTHS[i],
                EXPECTED_NODES[i],
                implementation,
                concurrency,
                workerCount
            );
            totalElapsedMs += elapsedMs;
            if(i < positions.endIndex) System.out.println();
        }
        System.out.println("All positions complete.");
        System.out.println("Total elapsed: " + format(totalElapsedMs) + " ms");
    }

    public static void runAll() {
        runPerft(ALL_POSITIONS, FLAT, USE_CONCURRENCY, MAX_CPUS);
    }

    public static void runRange(int start, int end) {
        if(start < 0 || end >= POSITION_NAMES.length || start > end) {
            throw new IllegalArgumentException("Invalid range: " + start + " to " + end);
        }
        runPerft(new PositionSelection(start, end), FLAT, USE_CONCURRENCY, MAX_CPUS);
    }

    public static void runFen(String fen, int depth) {
        runFen(fen, depth, FLAT, USE_CONCURRENCY, MAX_CPUS);
    }

    public static void runFen(
        String fen,
        int depth,
        Implementation implementation,
        Concurrency concurrency,
        int cpuCount
    ) {
        long elapsedMs = runSinglePosition(
            fen, depth, 0L, implementation, concurrency, cpuCount
        );
        System.out.println("Total elapsed: " + format(elapsedMs) + " ms");
    }

    public static long runSinglePosition(String fen, int depth, long expectedNodes) {
        return runSinglePosition(
            fen, depth, expectedNodes, FLAT, USE_CONCURRENCY, MAX_CPUS
        );
    }

    public static long runSinglePosition(
        String fen,
        int depth,
        long expectedNodes,
        Implementation implementation,
        Concurrency concurrency,
        int cpuCount
    ) {
        Objects.requireNonNull(fen, "fen");
        Objects.requireNonNull(implementation, "implementation");
        Objects.requireNonNull(concurrency, "concurrency");
        validateDepth(depth);
        validateCpuCount(cpuCount);

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
            concurrency == Concurrency.CONCURRENT ? cpuCount : 1
        );
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
        validateResult(nodes, expectedNodes);
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
        if(expectedNodes <= 0L) return;
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

    private static final String[] POSITION_NAMES = {
        "1. Initial position",
        "2. Kiwipete",
        "3.",
        "4.",
        "5.",
        "6.",
        "7.",
        "8. En passant capture gives check",
        "9. Short castling gives check",
        "10. Long castling gives check",
        "11. Castling",
        "12. Castling prevented",
        "13. Promote out of check",
        "14. Discovered check",
        "15. Promotion gives check",
        "16. Underpromotion gives check",
        "17. Self stalemate",
        "18. Stalemate/Checkmate",
        "19. Double check",
        "20. New position"
    };

    private static final String[] POSITION_FENS = {
        "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
        "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
        "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
        "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1",
        "rnbqkb1r/pp1p1ppp/2p5/4P3/2B5/8/PPP1NnPP/RNBQK2R w KQkq - 0 6",
        "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10",
        "8/5bk1/8/2Pp4/8/1K6/8/8 w - d6 0 1",
        "8/8/1k6/2b5/2pP4/8/5K2/8 b - d3 0 1",
        "5k2/8/8/8/8/8/8/4K2R w K - 0 1",
        "3k4/8/8/8/8/8/8/R3K3 w Q - 0 1",
        "r3k2r/1b4bq/8/8/8/8/7B/R3K2R w KQkq - 0 1",
        "r3k2r/8/3Q4/8/8/5q2/8/R3K2R b KQkq - 0 1",
        "2K2r2/4P3/8/8/8/8/8/3k4 w - - 0 1",
        "8/8/1P2K3/8/2n5/1q6/8/5k2 b - - 0 1",
        "4k3/1P6/8/8/8/8/K7/8 w - - 0 1",
        "8/P1k5/K7/8/8/8/8/8 w - - 0 1",
        "K1k5/8/P7/8/8/8/8/8 w - - 0 1",
        "8/k1P5/8/1K6/8/8/8/8 w - - 0 1",
        "8/8/2k5/5q2/5n2/8/5K2/8 b - - 0 1",
        "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8"
    };

    private static final long[] EXPECTED_NODES = {
        119060324L,
        8031647685L,
        178633661L,
        706045033L,
        53392L,
        6923051137L,
        824064L,
        1440467L,
        661072L,
        803711L,
        1274206L,
        1720476L,
        3821001L,
        1004658L,
        217342L,
        92683L,
        2217L,
        567584L,
        23527L,
        89941194L
    };

    private static final int[] DEPTHS = {
        6, 6, 7, 6, 3, 6, 6, 6, 6, 6, 4, 4, 6, 5, 6, 6, 6, 7, 4, 5
    };

    public static final PositionSelection ALL_POSITIONS =
        new PositionSelection(0, POSITION_NAMES.length - 1);

    private Perft() {}
}
