package com.ohinteractive.seedv6.search.flat;

import java.util.Objects;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.search.common.SearchObserver;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchResult;

public class FlatNegamax {

    private static final class Frame {
        int ply;
        int depth;
        int phase;
        int moveIndex;
        int moveCount;
        int searchedMoves;
        int bestScore;
        long bestMove;
        long currentMove;
        long board0;
        long board1;
        long board2;
        long board3;
        int status;
        long key;
        long checkers;
        boolean inCheck;
        long[] moves;
        long[] nextBoard;
    }

    public FlatNegamax() {
        for(int i = 0; i < frames.length; i ++) {
            frames[i] = new Frame();
        }
    }

    public SearchResult search(SearchRequest request) {
        Objects.requireNonNull(request, "request");
        final int requestedDepth = request.depth();
        if(requestedDepth > MAX_PLY) {
            throw new IllegalArgumentException("Unsupported search depth: " + requestedDepth);
        }
        request.copyBoardInto(boardStack[0]);
        
        nodes = 0L;
        initFrame(frames[0], 0, requestedDepth, boardStack[0]);

        final SearchObserver observer = request.observer();
        final long searchStartNanos = System.nanoTime();
        final Frame root = frames[0];
        final int rootEval = Eval.eval(root.board0, root.board1, root.board2, root.board3, root.status, root.key);
        final int rootMoveCount = countRootMoves(root);
        observer.onSearchStarted(requestedDepth, rootEval, rootMoveCount);
        int rootMoveIndex = 0;
        long rootMoveStartNodes = 0L;
        long rootMoveStartNanos = 0L;

        int top = 1;
        while(top > 0) {
            final Frame frame = frames[top - 1];
            if(frame.depth == 0) {
                final int score = evaluateFrontier(frame);
                top --;
                acceptChildScore(observer, frames[top - 1], score, rootMoveIndex, rootMoveCount, rootMoveStartNodes, rootMoveStartNanos);
                continue;
            }
            if(frame.phase == PHASE_UNGENERATED) {
                generateInitialPhase(frame);
                continue;
            }
            if(frame.moveIndex >= frame.moveCount) {
                if(advancePhaseOrComplete(frame)) {
                    final int score = completeFrameScore(frame);
                    top --;
                    if(top == 0) return finishSearch(observer, frame, requestedDepth, score, searchStartNanos);
                    acceptChildScore(observer, frames[top - 1], score, rootMoveIndex, rootMoveCount, rootMoveStartNodes, rootMoveStartNanos);
                }
                continue;
            }
            final long move = frame.moves[frame.moveIndex ++];
            frame.currentMove = move;
            frame.searchedMoves ++;
            if(frame.ply == 0) {
                rootMoveIndex ++;
                rootMoveStartNodes = nodes;
                rootMoveStartNanos = System.nanoTime();
                observer.onRootMoveStarted(rootMoveIndex, rootMoveCount, move);
            }
            final long[] childBoard = frame.nextBoard;
            Board.makeMoveInto(frame.board0, frame.board1, frame.board2, frame.board3, frame.status, frame.key, move, childBoard);
            nodes ++;
            initFrame(frames[top], frame.ply + 1, frame.depth - 1, childBoard);
            top ++;
        }
        throw new IllegalStateException("Flat negamax exited without producing a result.");
    }

    private static final int MAX_PLY = 64;
    private static final int MAX_MOVES = 256;
    private static final int MATE_SCORE = 32768;
    private static final int NEG_INF = -1_000_000_000;

    private static final int PHASE_UNGENERATED = 0;
    private static final int PHASE_EVASION = 1;
    private static final int PHASE_TACTICAL = 2;
    private static final int PHASE_QUIET = 3;
    private static final int[] LSB = Board.LSB;
    private static final long DB = Board.DB;

    private final Frame[] frames = new Frame[MAX_PLY + 1];
    private final long[][] boardStack = new long[MAX_PLY + 1][Board.MAX_BITBOARDS];
    private final long[][] moveStack = new long[MAX_PLY + 1][MAX_MOVES];
    private final long[] genScratch = new long[Board.MAX_BITBOARDS];
    
    private long nodes;

    private void generateInitialPhase(Frame frame) {
        frame.checkers = computeCheckers(frame);
        frame.inCheck = frame.checkers != 0L;
        frame.moveIndex = 0;
        if(frame.inCheck) {
            frame.moveCount = Gen.genEvasion(frame.board0, frame.board1, frame.board2, frame.board3, frame.status, frame.key, true, frame.checkers, frame.moves, genScratch);
            frame.phase = PHASE_EVASION;
            return;
        }
        frame.moveCount = Gen.genTactical(frame.board0, frame.board1, frame.board2, frame.board3, frame.status, frame.key, true, frame.moves, genScratch);
        frame.phase = PHASE_TACTICAL;
    }

    private boolean advancePhaseOrComplete(Frame frame) {
        if(frame.phase == PHASE_EVASION) return true;
        if(frame.phase == PHASE_TACTICAL) {
            frame.moveIndex = 0;
            frame.moveCount = Gen.genQuiet(frame.board0, frame.board1, frame.board2, frame.board3, frame.status, frame.key, true, frame.moves, genScratch);
            frame.phase = PHASE_QUIET;
            return false;
        }
        if(frame.phase == PHASE_QUIET) return true;
        throw new IllegalStateException("Unexpected search phase: " + frame.phase);
    }

    private int completeFrameScore(Frame frame) {
        if(frame.searchedMoves == 0) {
            if(frame.inCheck) {
                return -MATE_SCORE + frame.ply;
            }
            return 0;
        }
        return frame.bestScore;
    }

    private int evaluateFrontier(Frame frame) {
        final long checkers = computeCheckers(frame);
        if(checkers != 0L) {
            final int moveCount = Gen.genEvasion(
                frame.board0, frame.board1, frame.board2, frame.board3,
                frame.status, frame.key, true, checkers, frame.moves, genScratch
            );
            if(moveCount == 0) return -MATE_SCORE + frame.ply;
        } else {
            final int tacticalCount = Gen.genTactical(
                frame.board0, frame.board1, frame.board2, frame.board3,
                frame.status, frame.key, true, frame.moves, genScratch
            );
            if(tacticalCount == 0) {
                final int quietCount = Gen.genQuiet(
                    frame.board0, frame.board1, frame.board2, frame.board3,
                    frame.status, frame.key, true, frame.moves, genScratch
                );
                if(quietCount == 0) return 0;
            }
        }
        return Eval.eval(frame.board0, frame.board1, frame.board2, frame.board3, frame.status, frame.key);
    }

    private void acceptChildScore(SearchObserver observer, Frame parent, int childScore, int rootMoveIndex, int rootMoveCount, long rootMoveStartNodes, long rootMoveStartNanos) {
        final int score = -childScore;
        final boolean best = score > parent.bestScore;
        if(parent.ply == 0) observer.onRootMoveFinished(rootMoveIndex, rootMoveCount, parent.currentMove, score, best, nodes - rootMoveStartNodes, System.nanoTime() - rootMoveStartNanos);
        if(best) {
            parent.bestScore = score;
            parent.bestMove = parent.currentMove;
        }
    }

    private static long computeCheckers(Frame frame) {
        final int player = frame.status & Board.PLAYER_BIT;
        final long allOccupancy = frame.board0 | frame.board1 | frame.board2;
        final long colorMask = ~(-(long) player ^ frame.board3);
        final long king = frame.board0 & ~frame.board1 & ~frame.board2 & colorMask;
        final int kingSquare = LSB[(int) (((king & -king) * DB) >>> 58)];
        return Board.getCheckers(frame.board0, frame.board1, frame.board2, frame.board3, colorMask, player, kingSquare, allOccupancy);
    }

    private void initFrame(Frame frame, int ply, int depth, long[] board) {
        frame.ply = ply;
        frame.depth = depth;
        frame.phase = PHASE_UNGENERATED;
        frame.moveIndex = 0;
        frame.moveCount = 0;
        frame.searchedMoves = 0;
        frame.bestScore = NEG_INF;
        frame.bestMove = 0L;
        frame.currentMove = 0L;
        frame.board0 = board[0];
        frame.board1 = board[1];
        frame.board2 = board[2];
        frame.board3 = board[3];
        frame.status = (int) board[Board.STATUS];
        frame.key = board[Board.KEY];
        frame.checkers = 0L;
        frame.inCheck = false;
        frame.moves = moveStack[ply];
        frame.nextBoard = ply + 1 < boardStack.length ? boardStack[ply + 1] : null;
    }

    private SearchResult buildResult(Frame root, int requestedDepth, int score) {
        final boolean hasMove = root.searchedMoves != 0;
        return new SearchResult(
            hasMove ? root.bestMove : 0L,
            hasMove,
            score,
            requestedDepth,
            nodes,
            root.searchedMoves,
            true
        );
    }

    private SearchResult finishSearch(SearchObserver observer, Frame root, int requestedDepth, int score, long searchStartNanos) {
        final SearchResult result = buildResult(root, requestedDepth, score);
        observer.onSearchFinished(result, System.nanoTime() - searchStartNanos);
        return result;
    }

    private int countRootMoves(Frame root) {
        final long checkers = computeCheckers(root);
        if(checkers != 0L) return Gen.genEvasion(root.board0, root.board1, root.board2, root.board3, root.status, root.key, true, checkers, moveStack[0], genScratch);
        final int tacticalCount = Gen.genTactical(root.board0, root.board1, root.board2, root.board3, root.status, root.key, true, moveStack[0], genScratch);
        final int quietCount = Gen.genQuiet(root.board0, root.board1, root.board2, root.board3, root.status, root.key, true, moveStack[0], genScratch);
        return tacticalCount + quietCount;
    }

}
