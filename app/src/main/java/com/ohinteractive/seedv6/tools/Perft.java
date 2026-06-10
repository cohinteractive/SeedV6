package com.ohinteractive.seedv6.tools;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.util.Piece;

public final class Perft {
    
    private static final int MAX_DEPTH = 64;
    private static final int MAX_MOVES = 512;

    public static long run(long[] board, int maxDepth) {
        long lastNodes = 0;
        long lastElapsedNs = 0;

        for(int depth = 1; depth <= maxDepth; depth ++) {
            long start = System.nanoTime();

            long nodes = perft(board, depth);

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

    public static void runDebug(long[] board, int maxDepth) {
        if(maxDepth < 0 || maxDepth > MAX_DEPTH) throw new IllegalArgumentException("maxDepth");

        for(int depth = 1; depth < maxDepth; depth ++) {
            long start = System.nanoTime();

            long nodes = perft(board, depth);

            long elapsedNs = System.nanoTime() - start;
            long elapsedMs = elapsedNs / 1_000_000L;

            System.out.println(
                "Depth " + depth + "/" + maxDepth + ": " +
                format(nodes) + " nodes in " + elapsedMs + " ms"
            );
        }

        if(maxDepth == 0) {
            System.out.println("Depth 0/0: 1 nodes in 0 ms");
            System.out.println();
            System.out.println("Nodes per second: 1");
            return;
        }

        System.out.println("Depth " + maxDepth + "/" + maxDepth + ":");

        long[][] boardStack = new long[maxDepth + 1][Board.MAX_BITBOARDS];
        long[][] moveStack = new long[maxDepth + 1][MAX_MOVES];
        long[] genScratch = new long[Board.MAX_BITBOARDS];

        final long board0 = board[0];
        final long board1 = board[1];
        final long board2 = board[2];
        final long board3 = board[3];
        final int status = (int) board[Board.STATUS];
        final long key = board[Board.KEY];

        long[] moves = moveStack[0];

        int n = Gen.gen(
            board0, board1, board2, board3,
            status,
            key,
            true,
            false,
            moves,
            genScratch
        );

        long total = 0;
        long finalDepthStart = System.nanoTime();

        long[] nextBoard = boardStack[1];

        for(int i = 0; i < n; i ++) {
            Board.makeMoveInto(board0, board1, board2, board3, status, key, moves[i], nextBoard);

            long moveStart = System.nanoTime();

            long nodes = perft(
                nextBoard,
                maxDepth - 1,
                1,
                boardStack,
                moveStack,
                genScratch
            );

            long moveElapsedNs = System.nanoTime() - moveStart;
            long moveElapsedMs = moveElapsedNs / 1_000_000L;

            total += nodes;

            System.out.println(
                (i + 1) + "/" + n + "  " +
                moveToString(moves[i]) + ": " +
                format(nodes) +
                "  Elapsed: " + moveElapsedMs + " ms"
            );
        }

        long finalDepthElapsedNs = System.nanoTime() - finalDepthStart;
        long finalDepthElapsedMs = finalDepthElapsedNs / 1_000_000L;

        System.out.println(
            "Depth " + maxDepth + "/" + maxDepth + ": " +
            format(total) + " nodes in " + finalDepthElapsedMs + " ms"
        );

        long nps = finalDepthElapsedMs > 0
            ? total * 1_000_000_000L / finalDepthElapsedNs
            : total;

        System.out.println();
        System.out.println("Nodes per second: " + format(nps));
    }

    public static void runAll() {
        runRange(0, POSITION_NAMES.length - 1);
    }

    public static void runRange(int start, int end) {
        if(start < 0 || end >= POSITION_NAMES.length || start > end) {
            throw new IllegalArgumentException("Invalid range: " + start + " to " + end);
        }

        long totalElapsedMs = 0;

        for(int i = start; i <= end; i ++) {
            System.out.println(POSITION_NAMES[i]);

            long elapsedMs = runSinglePosition(
                POSITION_FENS[i],
                DEPTHS[i],
                EXPECTED_NODES[i]
            );

            totalElapsedMs += elapsedMs;

            if(i < end) {
                System.out.println();
            }
        }

        System.out.println("All positions complete.");
        System.out.println("Total elapsed: " + format(totalElapsedMs) + " ms");
    }

    public static void runFen(String fen, int depth) {
        long elapsedMs = runSinglePosition(fen, depth, 0L);
        System.out.println("Total elapsed: " + format(elapsedMs) + " ms");
    }

    public static long runSinglePosition(String fen, int depth, long expectedNodes) {
        System.out.println("\"" + fen + "\"");

        long[] board = Board.fromFen(fen);

        long start = System.nanoTime();

        long nodes = run(board, depth);

        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;

        if(expectedNodes > 0L) {
            if(nodes == expectedNodes) {
                System.out.println("Result: PASSED");
            } else {
                long diff = nodes - expectedNodes;

                System.out.println(
                    "Result: FAILED expected " + format(expectedNodes) +
                    ", got " + format(nodes) +
                    ", diff " + format(diff)
                );
            }
        }

        return elapsedMs;
    }

    public static long perft(long[] board, int depth) {
        if(depth < 0 || depth > MAX_DEPTH) throw new IllegalArgumentException("depth");
        long[][] boardStack = new long[depth + 1][Board.MAX_BITBOARDS];
        long[][] moveStack = new long[depth + 1][MAX_MOVES];
        long[] genScratch = new long[Board.MAX_BITBOARDS];
        return perft(board, depth, 0, boardStack, moveStack, genScratch);
    }

    private static long perft(long[] board, int depth, int ply, long[][] boardStack, long[][] moveStack, long[] genScratch) {
        if(depth == 0) return 1;

        final long board0 = board[0];
        final long board1 = board[1];
        final long board2 = board[2];
        final long board3 = board[3];
        final int status = (int) board[Board.STATUS];
        final long key = board[Board.KEY];

        long[] moves = moveStack[ply];

        int n = Gen.gen(
            board0, board1, board2, board3,
            status,
            key,
            true,
            false,
            moves,
            genScratch
        );

        if(depth == 1) return n;

        long nodes = 0;
        long[] nextBoard = boardStack[ply + 1];

        for(int i = 0; i < n; i ++) {
            Board.makeMoveInto(board0, board1, board2, board3, status, key, moves[i], nextBoard);
            nodes += perft(nextBoard, depth - 1, ply + 1, boardStack, moveStack, genScratch);
        }

        return nodes;
    }

    public static void divide(long[] board, int depth) {
        if(depth <= 0) {
            System.out.println(perft(board, depth));
            return;
        }

        long[][] boardStack = new long[depth + 1][Board.MAX_BITBOARDS];
        long[][] moveStack = new long[depth + 1][MAX_MOVES];
        long[] genScratch = new long[Board.MAX_BITBOARDS];

        final long board0 = board[0];
        final long board1 = board[1];
        final long board2 = board[2];
        final long board3 = board[3];
        final int status = (int) board[Board.STATUS];
        final long key = board[Board.KEY];

        long[] moves = moveStack[0];

        int n = Gen.gen(board0, board1, board2, board3, status, key, true, false, moves, genScratch);

        long total = 0;
        long[] nextBoard = boardStack[1];

        for(int i = 0; i < n; i ++) {
            Board.makeMoveInto(board0, board1, board2, board3, status, key, moves[i], nextBoard);
            long nodes = perft(nextBoard, depth - 1, 1, boardStack, moveStack, genScratch);
            total += nodes;
            System.out.println(moveToString(moves[i]) + ": " + nodes);
        }

        System.out.println("total: " + total);
    }

    private static String moveToString(long move) {
        int from = (int) move & Board.SQUARE_BITS;
        int to = (int) move >>> Board.TARGET_SQUARE_SHIFT & Board.SQUARE_BITS;
        int promote = (int) move >>> Board.PROMOTE_PIECE_SHIFT & Board.PIECE_BITS;
        return squareToString(from) + squareToString(to) + promotionToString(promote);
    }

    private static String squareToString(int square) {
        return "" + (char) ('a' + (square & 7)) + (char) ('1' + (square >>> 3));
    }

    private static String promotionToString(int piece) {
        return switch(piece & Piece.TYPE) {
            case Piece.QUEEN -> "q";
            case Piece.ROOK -> "r";
            case Piece.BISHOP -> "b";
            case Piece.KNIGHT -> "n";
            default -> "";
        };
    }

    private static String format(long value) {
        return String.format("%,d", value);
    }

    public static void main(String[] args) {
        runRange(10, 19);
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
        193690690L,
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
        6, 5, 7, 6, 3, 6, 6, 6, 6, 6, 4, 4, 6, 5, 6, 6, 6, 7, 4, 5
    };

    private Perft() {}

}
