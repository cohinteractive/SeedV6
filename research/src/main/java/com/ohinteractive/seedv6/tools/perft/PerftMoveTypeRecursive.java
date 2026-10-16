package com.ohinteractive.seedv6.tools.perft;

import java.util.concurrent.atomic.AtomicInteger;

import com.ohinteractive.seedv6.core.BoardMoveType;
import com.ohinteractive.seedv6.core.GenMoveType;

final class PerftMoveTypeRecursive {

    private static final int MAX_DEPTH = 64;
    private static final int MAX_MOVES = 512;
    private static final int[] LSB = BoardMoveType.LSB;

    static final class Workspace {
        final long[][] boardStack;
        final long[][] moveStack;
        final long[] genScratch;

        Workspace(int depth) {
            boardStack = new long[depth + 1][BoardMoveType.MAX_BITBOARDS];
            moveStack = new long[depth + 1][MAX_MOVES];
            genScratch = new long[BoardMoveType.MAX_BITBOARDS];
        }
    }

    static long count(long[] board, int depth) {
        if(depth < 0 || depth > MAX_DEPTH) throw new IllegalArgumentException("depth");
        return count(board, depth, 0, new Workspace(depth));
    }

    static long count(long[] board, int depth, Workspace workspace) {
        if(depth < 0 || depth > MAX_DEPTH) throw new IllegalArgumentException("depth");
        return count(board, depth, 0, workspace);
    }

    static long countConcurrent(long[] board, int depth, int threads) {
        if(depth < 0 || depth > MAX_DEPTH) throw new IllegalArgumentException("depth");
        if(depth == 0) return 1L;
        if(depth == 1) return count(board, depth);

        final long board0 = board[0];
        final long board1 = board[1];
        final long board2 = board[2];
        final long board3 = board[3];
        final int status = (int) board[BoardMoveType.STATUS];
        final long key = board[BoardMoveType.KEY];
        final long[] rootMoves = new long[MAX_MOVES];
        final long[] rootQuietMoves = new long[MAX_MOVES];
        final long[] rootScratch = new long[BoardMoveType.MAX_BITBOARDS];
        final int player = status & BoardMoveType.PLAYER_BIT;
        final long colorMask = ~(-(player) ^ board3);
        final long kingBitboard = board0 & ~board1 & ~board2 & colorMask;
        final int kingSquare = LSB[(int) (((kingBitboard & -kingBitboard) * BoardMoveType.DB) >>> 58)];
        final long allOccupancy = board0 | board1 | board2;
        final long checkers = BoardMoveType.getCheckers(
            board0, board1, board2, board3, colorMask, player, kingSquare, allOccupancy
        );
        final int rootMoveCount;
        if(checkers != 0L) {
            rootMoveCount = GenMoveType.genEvasion(
                board0, board1, board2, board3, status, key, true, checkers, rootMoves, rootScratch
            );
        } else {
            final int tacticalCount = GenMoveType.genTactical(
                board0, board1, board2, board3, status, key, true, rootMoves, rootScratch
            );
            final int quietCount = GenMoveType.genQuiet(
                board0, board1, board2, board3, status, key, true, rootQuietMoves, rootScratch
            );
            System.arraycopy(rootQuietMoves, 0, rootMoves, tacticalCount, quietCount);
            rootMoveCount = tacticalCount + quietCount;
        }
        if(rootMoveCount == 0) return 0L;

        final int workerCount = Math.min(threads, rootMoveCount);
        final AtomicInteger nextMoveIndex = new AtomicInteger(0);
        final long[] totals = new long[workerCount];
        final Thread[] workers = new Thread[workerCount];
        for(int worker = 0; worker < workerCount; worker ++) {
            final int workerIndex = worker;
            workers[worker] = new Thread(() -> {
                Workspace workspace = new Workspace(depth);
                long[] nextBoard = new long[BoardMoveType.MAX_BITBOARDS];
                long localTotal = 0L;
                while(true) {
                    int moveIndex = nextMoveIndex.getAndIncrement();
                    if(moveIndex >= rootMoveCount) break;
                    BoardMoveType.makeMoveInto(
                        board0, board1, board2, board3, status, key, rootMoves[moveIndex], nextBoard
                    );
                    localTotal += count(nextBoard, depth - 1, 0, workspace);
                }
                totals[workerIndex] = localTotal;
            });
        }
        startAndJoin(workers);

        long total = 0L;
        for(int worker = 0; worker < workerCount; worker ++) total += totals[worker];
        return total;
    }

    private static long count(long[] board, int depth, int ply, Workspace workspace) {
        if(depth == 0) return 1L;
        final long board0 = board[0];
        final long board1 = board[1];
        final long board2 = board[2];
        final long board3 = board[3];
        final int status = (int) board[BoardMoveType.STATUS];
        final long key = board[BoardMoveType.KEY];
        final long[] moves = workspace.moveStack[ply];
        final int player = status & BoardMoveType.PLAYER_BIT;
        final long colorMask = ~(-(player) ^ board3);
        final long kingBitboard = board0 & ~board1 & ~board2 & colorMask;
        final int kingSquare = LSB[(int) (((kingBitboard & -kingBitboard) * BoardMoveType.DB) >>> 58)];
        final long allOccupancy = board0 | board1 | board2;
        final long checkers = BoardMoveType.getCheckers(
            board0, board1, board2, board3, colorMask, player, kingSquare, allOccupancy
        );
        final int moveCount = checkers != 0L
            ? GenMoveType.genEvasion(
                board0, board1, board2, board3, status, key, true, checkers, moves, workspace.genScratch
            )
            : GenMoveType.genAll(
                board0, board1, board2, board3, status, key, true, moves, workspace.genScratch
            );
        if(depth == 1) return moveCount;

        long nodes = 0L;
        final long[] nextBoard = workspace.boardStack[ply + 1];
        for(int i = 0; i < moveCount; i ++) {
            BoardMoveType.makeMoveInto(
                board0, board1, board2, board3, status, key, moves[i], nextBoard
            );
            nodes += count(nextBoard, depth - 1, ply + 1, workspace);
        }
        return nodes;
    }

    private static void startAndJoin(Thread[] workers) {
        for(Thread worker : workers) worker.start();
        for(Thread worker : workers) {
            try {
                worker.join();
            } catch(InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted during concurrent perft", e);
            }
        }
    }

    private PerftMoveTypeRecursive() {}
}
