package com.ohinteractive.seedv6.search.flat;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.Gen;
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
        final int requestedDepth = request.depth;
        if(requestedDepth < 0 || requestedDepth > MAX_PLY) {
            throw new IllegalArgumentException("Unsupported search depth: " + requestedDepth);
        }
        System.arraycopy(request.board, 0, boardStack[0], 0, Board.MAX_BITBOARDS);
        nodes = 0L;
        initFrame(frames[0], 0, requestedDepth, boardStack[0]);
        int top = 1;
        while(top > 0) {
            final Frame frame = frames[top - 1];
            if(frame.depth == 0) {
                final int score = Eval.eval(frame.board0, frame.board1, frame.board2, frame.board3, frame.status, frame.key);
                nodes ++;
                top --;
                if(top == 0) return buildResult(frame, requestedDepth, score);
                acceptChildScore(frames[top - 1], score);
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
                    if(top == 0) return buildResult(frame, requestedDepth, score);
                    acceptChildScore(frames[top - 1], score);
                }
                continue;
            }
            final long move = frame.moves[frame.moveIndex ++];
            frame.currentMove = move;
            frame.searchedMoves ++;
            final long[] childBoard = frame.nextBoard;
            Board.makeMoveInto(frame.board0, frame.board1, frame.board2, frame.board3, frame.status, frame.key, move, childBoard);
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
            nodes ++;
            if(frame.inCheck) {
                return -MATE_SCORE+ frame.ply;
            }
            return 0;
        }
        return frame.bestScore;
    }

    private static void acceptChildScore(Frame parent, int childScore) {
        final int score = -childScore;
        if(score > parent.bestScore) {
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
        final SearchResult result = new SearchResult();
        result.bestMove = root.bestMove;
        result.hasMove = root.searchedMoves != 0;
        result.score = score;
        result.depth = requestedDepth;
        result.nodes = nodes;
        result.legalRootMoves = root.searchedMoves;
        return result;
    }

}
