package com.ohinteractive.seedv6.tools;

import java.util.concurrent.atomic.AtomicInteger;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.util.Piece;

public final class Perft {

    public static void main(String[] args) {
        runRange(0, 19);
        //runDebug(Board.fromFen(POSITION_FENS[1]), 5);
    }
    
    private static final int MAX_DEPTH = 64;
    private static final int MAX_MOVES = 512;

    private static final class Frame {
        int ply;
        int depth;
        int moveIndex;
        int moveCount;

        long board0;
        long board1;
        long board2;
        long board3;
        int status;
        long key;
        long[] moves;
        long[] nextBoard;
    }

    private static final class Workspace {
        final long[][] boardStack;
        final long[][] moveStack;
        final long[] genScratch;
        final Frame[] frames;

        Workspace(int depth) {
            boardStack = new long[depth + 1][Board.MAX_BITBOARDS];
            moveStack = new long[depth + 1][MAX_MOVES];
            genScratch = new long[Board.MAX_BITBOARDS];
            frames = new Frame[depth + 1];
            for(int i = 0; i < frames.length; i ++) frames[i] = new Frame();
        }
    }

    private static int threadCount() {
        int cpus = Runtime.getRuntime().availableProcessors();
        return Math.max(1, cpus);
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

    public static long run(long[] board, int maxDepth) {
        if(maxDepth < 0 || maxDepth > MAX_DEPTH) throw new IllegalArgumentException("maxDepth");
        long lastNodes = 0;
        long lastElapsedNs = 0;
        Workspace workspace = new Workspace(maxDepth);
        for(int depth = 1; depth <= maxDepth; depth ++) {
            long start = System.nanoTime();
            long nodes = depth == maxDepth && depth > 1
                ? perftFlatConcurrentRoot(board, depth, threadCount())
                : perftFlat(board, depth, workspace);
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
        Workspace workspace = new Workspace(maxDepth);
        for(int depth = 1; depth < maxDepth; depth ++) {
            long start = System.nanoTime();
            long nodes = perftFlat(board, depth, workspace);
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
        final int player = status & Board.PLAYER_BIT;
        final long colorMask = ~(-(player) ^ board3);
        final long kingBitboard = board0 & ~board1 & ~board2 & colorMask;
        final int kingSquare = LSB[(int) (((kingBitboard & -kingBitboard) * Board.DB) >>> 58)];
        final long allOccupancy = board0 | board1 | board2;
        long[] moves = moveStack[0];
        final long checkers = Board.getCheckers(board0, board1, board2, board3, colorMask, player, kingSquare, allOccupancy);
        int n = checkers != 0L ?
        Gen.genEvasion(
            board0, board1, board2, board3,
            status,
            key,
            true,
            checkers,
            moves,
            genScratch
        )
        :
        Gen.genAll(
            board0, board1, board2, board3,
            status,
            key,
            true,
            moves,
            genScratch
        );
        long total = 0;
        long finalDepthStart = System.nanoTime();
        long[] nextBoard = boardStack[1];
        for(int i = 0; i < n; i ++) {
            Board.makeMoveInto(board0, board1, board2, board3, status, key, moves[i], nextBoard);
            long moveStart = System.nanoTime();
            long nodes = perftRecursive(
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
        long nps = finalDepthElapsedNs > 0L
            ? total * 1_000_000_000L / finalDepthElapsedNs
            : total;
        System.out.println();
        System.out.println("Nodes per second: " + format(nps));
    }

    public static long perftFlatEntry(long[] board, int depth) {
        return perftFlat(board, depth);
    }

    public static long perftFlat(long[] board, int depth) {
        if(depth < 0 || depth > MAX_DEPTH) throw new IllegalArgumentException("depth");
        if(depth == 0) return 1L;
        if(depth == 1) return countLegalMoves(board, new long[MAX_MOVES], new long[Board.MAX_BITBOARDS]);
        return perftFlat(board, depth, new Workspace(depth));
    }

    private static long perftFlat(long[] board, int depth, Workspace workspace) {
        if(depth == 0) return 1L;
        if(depth == 1) return countLegalMoves(board, workspace.moveStack[0], workspace.genScratch);
        Frame[] frames = workspace.frames;
        int top = 0;
        pushFrame(
            frames[top ++],
            board,
            0,
            depth,
            workspace
        );
        long nodes = 0L;
        while(top > 0) {
            Frame frame = frames[top - 1];
            if(frame.moveIndex >= frame.moveCount) {
                top --;
                continue;
            }
            if(frame.depth == 1) {
                nodes += frame.moveCount - frame.moveIndex;
                top --;
                continue;
            }
            long move = frame.moves[frame.moveIndex ++];
            long[] nextBoard = frame.nextBoard;
            Board.makeMoveInto(
                frame.board0,
                frame.board1,
                frame.board2,
                frame.board3,
                frame.status,
                frame.key,
                move,
                nextBoard
            );
            // move legality check
            if(Board.isPlayerInCheck(nextBoard[0], nextBoard[1], nextBoard[2], nextBoard[3], frame.status & Board.PLAYER_BIT)) continue;
            if(frame.depth == 2) {
                nodes += countLegalMoves(
                    nextBoard,
                    workspace.moveStack[frame.ply + 1],
                    workspace.genScratch
                );
                continue;
            }
            pushFrame(
                frames[top ++],
                nextBoard,
                frame.ply + 1,
                frame.depth - 1,
                workspace
            );
        }
        return nodes;
    }

    private static long perftFlatConcurrentRoot(long[] board, int depth, int threads) {
        if(depth == 0) return 1L;
        if(depth == 1) {
            return countLegalMoves(
                board,
                new long[MAX_MOVES],
                new long[Board.MAX_BITBOARDS]
            );
        }
        final long board0 = board[0];
        final long board1 = board[1];
        final long board2 = board[2];
        final long board3 = board[3];
        final int status = (int) board[Board.STATUS];
        final long key = board[Board.KEY];
        /*
         * Root generation is single-threaded.
         * These are legal root moves, so each worker can safely make the move
         * and count the child subtree.
         */
        final long[] rootMoves = new long[MAX_MOVES];
        final long[] rootScratch = new long[Board.MAX_BITBOARDS];
        final int player = status & Board.PLAYER_BIT;
        final long colorMask = ~(-(player) ^ board3);
        final long kingBitboard = board0 & ~board1 & ~board2 & colorMask;
        final int kingSquare = LSB[(int) (((kingBitboard & -kingBitboard) * Board.DB) >>> 58)];
        final long allOccupancy = board0 | board1 | board2;
        final long checkers = Board.getCheckers(board0, board1, board2, board3, colorMask, player, kingSquare, allOccupancy);
        final int rootMoveCount = checkers != 0L ?
        Gen.genEvasion(
            board0,
            board1,
            board2,
            board3,
            status,
            key,
            true,
            checkers,
            rootMoves,
            rootScratch
        )
        :
        Gen.genAll(
            board0,
            board1,
            board2,
            board3,
            status,
            key,
            true,
            rootMoves,
            rootScratch
        );
        if(rootMoveCount == 0) return 0L;
        final int workerCount = Math.min(Math.max(1, threads), rootMoveCount);
        final AtomicInteger nextMoveIndex = new AtomicInteger(0);
        final long[] totals = new long[workerCount];
        final Thread[] workers = new Thread[workerCount];
        for(int worker = 0; worker < workerCount; worker ++) {
            final int workerIndex = worker;
            workers[worker] = new Thread(() -> {
                Workspace workspace = new Workspace(depth);
                /*
                 * Each worker owns this board buffer.
                 * Never share one nextBoard array between workers.
                 */
                long[] nextBoard = new long[Board.MAX_BITBOARDS];
                long localTotal = 0L;
                while(true) {
                    int moveIndex = nextMoveIndex.getAndIncrement();
                    if(moveIndex >= rootMoveCount) break;
                    long move = rootMoves[moveIndex];
                    Board.makeMoveInto(
                        board0,
                        board1,
                        board2,
                        board3,
                        status,
                        key,
                        move,
                        nextBoard
                    );
                    localTotal += perftFlat(
                        nextBoard,
                        depth - 1,
                        workspace
                    );
                }
                totals[workerIndex] = localTotal;
            });
        }
        for(int i = 0; i < workerCount; i ++) {
            workers[i].start();
        }
        for(int i = 0; i < workerCount; i ++) {
            try {
                workers[i].join();
            } catch(InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted during concurrent perft", e);
            }
        }
        long total = 0L;
        for(int i = 0; i < workerCount; i ++) {
            total += totals[i];
        }
        return total;
    }

    private static void pushFrame(Frame frame, long[] board, int ply, int depth, Workspace workspace) {
        final long board0 = board[0];
        final long board1 = board[1];
        final long board2 = board[2];
        final long board3 = board[3];
        final int status = (int) board[Board.STATUS];
        final long key = board[Board.KEY];
        final long[] moves = workspace.moveStack[ply];
        frame.ply = ply;
        frame.depth = depth;
        frame.moveIndex = 0;
        frame.moves = moves;
        final int player = status & Board.PLAYER_BIT;
        final long colorMask = ~(-(player) ^ board3);
        final long kingBitboard = board0 & ~board1 & ~board2 & colorMask;
        final int kingSquare = LSB[(int) (((kingBitboard & -kingBitboard) * Board.DB) >>> 58)];
        final long allOccupancy = board0 | board1 | board2;
        final long checkers = Board.getCheckers(board0, board1, board2, board3, colorMask, player, kingSquare, allOccupancy);
        if(checkers != 0L) {
            frame.moveCount = Gen.genEvasion(
                board0,
                board1,
                board2,
                board3,
                status,
                key,
                false,
                checkers,
                moves,
                workspace.genScratch
            );
        } else {
            frame.moveCount = Gen.genAll(
                board0,
                board1,
                board2,
                board3,
                status,
                key,
                false,
                moves,
                workspace.genScratch
            );
        }
        if(depth > 1) {
            frame.board0 = board0;
            frame.board1 = board1;
            frame.board2 = board2;
            frame.board3 = board3;
            frame.status = status;
            frame.key = key;
            frame.nextBoard = workspace.boardStack[ply + 1];
        }
    }

    private static int countLegalMoves(long[] board, long[] moves, long[] genScratch) {
        final long board0 = board[0];
        final long board1 = board[1];
        final long board2 = board[2];
        final long board3 = board[3];
        final int status = (int) board[Board.STATUS];
        final long key = board[Board.KEY];
        final int player = status & Board.PLAYER_BIT;
        final long colorMask = ~(-(player) ^ board3);
        final long kingBitboard = board0 & ~board1 & ~board2 & colorMask;
        final int kingSquare = LSB[(int) (((kingBitboard & -kingBitboard) * Board.DB) >>> 58)];
        final long allOccupancy = board0 | board1 | board2;
        final long checkers = Board.getCheckers(board0, board1, board2, board3, colorMask, player, kingSquare, allOccupancy);
        if(checkers != 0L) {
            return Gen.genEvasion(
                board0,
                board1,
                board2,
                board3,
                status,
                key,
                true,
                checkers,
                moves,
                genScratch
            );
        }
        return Gen.genAll(
            board0,
            board1,
            board2,
            board3,
            status,
            key,
            true,
            moves,
            genScratch
        );
    }

    public static long perftRecursiveEntry1(long[] board, int depth) {
        if(depth < 0 || depth > MAX_DEPTH) throw new IllegalArgumentException("depth");
        long[][] boardStack = new long[depth + 1][Board.MAX_BITBOARDS];
        long[][] moveStack = new long[depth + 1][MAX_MOVES];
        long[] genScratch = new long[Board.MAX_BITBOARDS];
        return perftRecursive(board, depth, 0, boardStack, moveStack, genScratch);
    }

    private static long perftRecursive(long[] board, int depth, int ply, long[][] boardStack, long[][] moveStack, long[] genScratch) {
        if(depth == 0) return 1;
        final long board0 = board[0];
        final long board1 = board[1];
        final long board2 = board[2];
        final long board3 = board[3];
        final int status = (int) board[Board.STATUS];
        final long key = board[Board.KEY];
        long[] moves = moveStack[ply];
        final int player = status & Board.PLAYER_BIT;
        final long colorMask = ~(-(player) ^ board3);
        final long kingBitboard = board0 & ~board1 & ~board2 & colorMask;
        final int kingSquare = LSB[(int) (((kingBitboard & -kingBitboard) * Board.DB) >>> 58)];
        final long allOccupancy = board0 | board1 | board2;
        final long checkers = Board.getCheckers(board0, board1, board2, board3, colorMask, player, kingSquare, allOccupancy);
        int n = checkers != 0L ?
        Gen.genEvasion(
            board0, board1, board2, board3,
            status,
            key,
            true,
            checkers,
            moves,
            genScratch
        )
        :
        Gen.genAll(
            board0, board1, board2, board3,
            status,
            key,
            true,
            moves,
            genScratch
        );
        if(depth == 1) return n;
        long nodes = 0;
        long[] nextBoard = boardStack[ply + 1];
        for(int i = 0; i < n; i ++) {
            Board.makeMoveInto(board0, board1, board2, board3, status, key, moves[i], nextBoard);
            nodes += perftRecursive(nextBoard, depth - 1, ply + 1, boardStack, moveStack, genScratch);
        }
        return nodes;
    }

    public static void divide(long[] board, int depth) {
        if(depth <= 0) {
            System.out.println(perftFlatEntry(board, depth));
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
        int n = Gen.genAll(board0, board1, board2, board3, status, key, true, moves, genScratch);
        long total = 0;
        long[] nextBoard = boardStack[1];
        for(int i = 0; i < n; i ++) {
            Board.makeMoveInto(board0, board1, board2, board3, status, key, moves[i], nextBoard);
            long nodes = perftRecursive(nextBoard, depth - 1, 1, boardStack, moveStack, genScratch);
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

    private static final int[] LSB = Board.LSB;

    private Perft() {}

}
