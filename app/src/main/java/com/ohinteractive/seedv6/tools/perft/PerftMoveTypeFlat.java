package com.ohinteractive.seedv6.tools.perft;

import java.util.concurrent.atomic.AtomicInteger;

import com.ohinteractive.seedv6.core.BoardMoveType;
import com.ohinteractive.seedv6.core.GenMoveType;

final class PerftMoveTypeFlat {

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

    static final class Workspace {
        final long[][] boardStack;
        final long[][] moveStack;
        final long[][] quietStack; // new second stack for temporary quiet move storage
        final long[] genScratch;
        final Frame[] frames;

        Workspace(int depth) {
            boardStack = new long[depth + 1][BoardMoveType.MAX_BITBOARDS];
            moveStack = new long[depth + 1][MAX_MOVES];
            quietStack = new long[depth + 1][MAX_MOVES];
            genScratch = new long[BoardMoveType.MAX_BITBOARDS];
            frames = new Frame[depth + 1];
            for(int i = 0; i < frames.length; i ++) frames[i] = new Frame();
        }
    }

    static long count(long[] board, int depth) {
        if(depth < 0 || depth > MAX_DEPTH) throw new IllegalArgumentException("depth");
        if(depth == 0) return 1L;
        //if(depth == 1) return countLegalMoves(board, new long[MAX_MOVES], new long[BoardMoveType.MAX_BITBOARDS]);
        if(depth == 1) return countLegalMoves(board, new long[MAX_MOVES], new long[MAX_MOVES], new long[BoardMoveType.MAX_BITBOARDS]);

        return count(board, depth, new Workspace(depth));
    }

    static long count(long[] board, int depth, Workspace workspace) {
        if(depth == 0) return 1L;
        //if(depth == 1) return countLegalMoves(board, workspace.moveStack[0], workspace.genScratch);
        if(depth == 1) return countLegalMoves(board, workspace.moveStack[0], workspace.quietStack[0], workspace.genScratch);
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
            BoardMoveType.makeMoveInto(
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
            if(BoardMoveType.isPlayerInCheck(nextBoard[0], nextBoard[1], nextBoard[2], nextBoard[3], frame.status & BoardMoveType.PLAYER_BIT)) continue;
            if(frame.depth == 2) {
                /*
                nodes += countLegalMoves(
                    nextBoard,
                    workspace.moveStack[frame.ply + 1],
                    workspace.genScratch
                );
                */
               nodes += countLegalMoves(nextBoard, workspace.moveStack[frame.ply + 1], workspace.quietStack[frame.ply + 1], workspace.genScratch);
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

    static long countConcurrent(long[] board, int depth, int threads) {
        if(depth == 0) return 1L;
        if(depth == 1) {
            /*
            return countLegalMoves(
                board,
                new long[MAX_MOVES],
                new long[BoardMoveType.MAX_BITBOARDS]
            );
            */
           return countLegalMoves(board, new long[MAX_MOVES], new long[MAX_MOVES], new long[BoardMoveType.MAX_BITBOARDS]);
        }
        final long board0 = board[0];
        final long board1 = board[1];
        final long board2 = board[2];
        final long board3 = board[3];
        final int status = (int) board[BoardMoveType.STATUS];
        final long key = board[BoardMoveType.KEY];
        /*
         * Root generation is single-threaded.
         * These are legal root moves, so each worker can safely make the move
         * and count the child subtree.
         */
        final long[] rootMoves = new long[MAX_MOVES];
        final long[] rootScratch = new long[BoardMoveType.MAX_BITBOARDS];
        final long[] rootQuietMoves = new long[MAX_MOVES];
        final int player = status & BoardMoveType.PLAYER_BIT;
        final long colorMask = ~(-(player) ^ board3);
        final long kingBitboard = board0 & ~board1 & ~board2 & colorMask;
        final int kingSquare = LSB[(int) (((kingBitboard & -kingBitboard) * BoardMoveType.DB) >>> 58)];
        final long allOccupancy = board0 | board1 | board2;
        final long checkers = BoardMoveType.getCheckers(board0, board1, board2, board3, colorMask, player, kingSquare, allOccupancy);
        /*
        final int rootMoveCount = checkers != 0L ?
        GenMoveType.genEvasion(
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
        GenMoveType.genAll(
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
        */
        final int rootMoveCount;
        if(checkers != 0L) {
            rootMoveCount = GenMoveType.genEvasion(board0, board1, board2, board3, status, key, true, checkers, rootMoves, rootScratch);
        } else {
            final int tacticalCount = GenMoveType.genTactical(board0, board1, board2, board3, status, key, true, rootMoves, rootScratch);
            final int quietCount = GenMoveType.genQuiet(board0, board1, board2, board3, status, key, true, rootQuietMoves, rootScratch);
            System.arraycopy(rootQuietMoves, 0, rootMoves, tacticalCount, quietCount);
            rootMoveCount = tacticalCount + quietCount;
        }
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
                long[] nextBoard = new long[BoardMoveType.MAX_BITBOARDS];
                long localTotal = 0L;
                while(true) {
                    int moveIndex = nextMoveIndex.getAndIncrement();
                    if(moveIndex >= rootMoveCount) break;
                    long move = rootMoves[moveIndex];
                    BoardMoveType.makeMoveInto(
                        board0,
                        board1,
                        board2,
                        board3,
                        status,
                        key,
                        move,
                        nextBoard
                    );
                    localTotal += count(
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
        final int status = (int) board[BoardMoveType.STATUS];
        final long key = board[BoardMoveType.KEY];
        final long[] moves = workspace.moveStack[ply];
        frame.ply = ply;
        frame.depth = depth;
        frame.moveIndex = 0;
        frame.moves = moves;
        final int player = status & BoardMoveType.PLAYER_BIT;
        final long colorMask = ~(-(player) ^ board3);
        final long kingBitboard = board0 & ~board1 & ~board2 & colorMask;
        final int kingSquare = LSB[(int) (((kingBitboard & -kingBitboard) * BoardMoveType.DB) >>> 58)];
        final long allOccupancy = board0 | board1 | board2;
        final long checkers = BoardMoveType.getCheckers(board0, board1, board2, board3, colorMask, player, kingSquare, allOccupancy);
        if(checkers != 0L) {
            frame.moveCount = GenMoveType.genEvasion(
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
            /*
            frame.moveCount = GenMoveType.genAll(
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
            */
           final long[] quietMoves = workspace.quietStack[ply];
           final int tacticalCount = GenMoveType.genTactical(board0, board1, board2, board3, status, key, false, moves, workspace.genScratch);
           final int quietCount = GenMoveType.genQuiet(board0, board1, board2, board3, status, key, false, quietMoves, workspace.genScratch);
           System.arraycopy(quietMoves, 0, moves, tacticalCount, quietCount);
           frame.moveCount = tacticalCount + quietCount;
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

    private static int countLegalMoves(long[] board, long[] moves, long[] quietMoves, long[] genScratch) {
        final long board0 = board[0];
        final long board1 = board[1];
        final long board2 = board[2];
        final long board3 = board[3];
        final int status = (int) board[BoardMoveType.STATUS];
        final long key = board[BoardMoveType.KEY];
        final int player = status & BoardMoveType.PLAYER_BIT;
        final long colorMask = ~(-(player) ^ board3);
        final long kingBitboard = board0 & ~board1 & ~board2 & colorMask;
        final int kingSquare = LSB[(int) (((kingBitboard & -kingBitboard) * BoardMoveType.DB) >>> 58)];
        final long allOccupancy = board0 | board1 | board2;
        final long checkers = BoardMoveType.getCheckers(board0, board1, board2, board3, colorMask, player, kingSquare, allOccupancy);
        if(checkers != 0L) {
            return GenMoveType.genEvasion(
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
        /*
        return GenMoveType.genAll(
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
        */
       final int tacticalCount = GenMoveType.genTactical(board0, board1, board2, board3, status, key, true, moves, genScratch);
       final int quietCount = GenMoveType.genQuiet(board0, board1, board2, board3, status, key, true, quietMoves, genScratch);
       return tacticalCount + quietCount;
    }

    private static final int[] LSB = BoardMoveType.LSB;

    private PerftMoveTypeFlat() {}

}

