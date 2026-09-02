package com.ohinteractive.seedv6.search.flat;

import java.util.Objects;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.search.common.SearchObserver;
import com.ohinteractive.seedv6.search.common.SearchControl;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.common.SingleDepthSearch;
import com.ohinteractive.seedv6.rules.DrawAdjudicator;
import com.ohinteractive.seedv6.rules.DrawAdjudicator.RuleDraw;
import com.ohinteractive.seedv6.rules.SearchLineHistory;

public class FlatNegamax implements SingleDepthSearch {

    public static final int MAX_SUPPORTED_DEPTH = 64;

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
        boolean ruleDraw;
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
        if(requestedDepth < 1) {
            throw new IllegalArgumentException(
                "FlatNegamax supports depths 1 through " + MAX_SUPPORTED_DEPTH
                    + ": " + requestedDepth
            );
        }
        if(requestedDepth > MAX_SUPPORTED_DEPTH) {
            throw new IllegalArgumentException("Unsupported search depth: " + requestedDepth);
        }
        final SearchLineHistory history = new SearchLineHistory(request.gameHistory());
        try {
            request.copyBoardInto(boardStack[0]);

            nodes = 0L;
            initFrame(frames[0], 0, requestedDepth, boardStack[0]);

            final SearchObserver observer = request.observer();
            final long searchStartNanos = System.nanoTime();
            final Frame root = frames[0];
            final int rootEval = Eval.eval(root.board0, root.board1, root.board2, root.board3, root.status, root.key);
            final int rootMoveCount = countRootMoves(root);
            observer.onSearchStarted(requestedDepth, rootEval, rootMoveCount);
            if(rootMoveCount == 0) {
                root.checkers = computeCheckers(root);
                return finishSearch(
                    observer, root, requestedDepth,
                    root.checkers != 0L ? -MATE_SCORE : 0, searchStartNanos
                );
            }
            if(rootMoveCount > 0 && isRuleDraw(root, history)) {
                return finishRuleDrawAtRoot(
                    observer, moveStack[0][0], requestedDepth, rootMoveCount, searchStartNanos
                );
            }

            int rootMoveIndex = 0;
            long rootMoveStartNodes = 0L;
            long rootMoveStartNanos = 0L;

            final SearchControl control = request.control();
            final boolean controlled = !control.isUnlimited();
            int pollCountdown = 0;

            int top = 1;
            while(top > 0) {
                if(controlled && pollCountdown -- <= 0) {
                    if(!control.checkpoint()) {
                        return interruptSearch(
                            observer, root, requestedDepth, rootMoveCount, searchStartNanos
                        );
                    }
                    pollCountdown = CONTROL_POLL_STEPS - 1;
                }
                final Frame frame = frames[top - 1];
                if(frame.depth == 0) {
                    final int score = evaluateFrontier(frame, history);
                    top --;
                    history.popRealPosition();
                    acceptChildScore(observer, frames[top - 1], score, rootMoveIndex, rootMoveCount, rootMoveStartNodes, rootMoveStartNanos);
                    continue;
                }
                if(frame.phase == PHASE_UNGENERATED) {
                    generateInitialPhase(frame);
                    if(frame.moveCount > 0) frame.ruleDraw = isRuleDraw(frame, history);
                    continue;
                }
                if(frame.ruleDraw) {
                    top --;
                    if(top == 0) {
                        return finishSearch(observer, frame, requestedDepth, 0, searchStartNanos);
                    }
                    history.popRealPosition();
                    acceptChildScore(observer, frames[top - 1], 0, rootMoveIndex, rootMoveCount, rootMoveStartNodes, rootMoveStartNanos);
                    continue;
                }
                if(frame.moveIndex >= frame.moveCount) {
                    if(advancePhaseOrComplete(frame)) {
                        final int score = completeFrameScore(frame);
                        top --;
                        if(top == 0) return finishSearch(observer, frame, requestedDepth, score, searchStartNanos);
                        history.popRealPosition();
                        acceptChildScore(observer, frames[top - 1], score, rootMoveIndex, rootMoveCount, rootMoveStartNodes, rootMoveStartNanos);
                    } else if(frame.moveCount > 0) {
                        frame.ruleDraw = isRuleDraw(frame, history);
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
                if(controlled && !control.tryEnterNode()) {
                    return interruptSearch(
                        observer, root, requestedDepth, rootMoveCount, searchStartNanos
                    );
                }
                final long[] childBoard = frame.nextBoard;
                Board.makeMoveInto(frame.board0, frame.board1, frame.board2, frame.board3, frame.status, frame.key, move, childBoard);
                nodes ++;
                history.pushRealPosition(childBoard);
                initFrame(frames[top], frame.ply + 1, frame.depth - 1, childBoard);
                top ++;
            }
            throw new IllegalStateException("Flat negamax exited without producing a result.");
        } finally {
            history.restoreRoot();
        }
    }

    private static final int MAX_MOVES = 256;
    private static final int MATE_SCORE = 32768;
    private static final int NEG_INF = -1_000_000_000;
    private static final int CONTROL_POLL_STEPS = 256;

    private static final int PHASE_UNGENERATED = 0;
    private static final int PHASE_EVASION = 1;
    private static final int PHASE_TACTICAL = 2;
    private static final int PHASE_QUIET = 3;
    private static final int[] LSB = Board.LSB;
    private static final long DB = Board.DB;

    private final Frame[] frames = new Frame[MAX_SUPPORTED_DEPTH + 1];
    private final long[][] boardStack = new long[MAX_SUPPORTED_DEPTH + 1][Board.MAX_BITBOARDS];
    private final long[][] moveStack = new long[MAX_SUPPORTED_DEPTH + 1][MAX_MOVES];
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

    @Override
    public int maxSupportedDepth() {
        return MAX_SUPPORTED_DEPTH;
    }

    private int evaluateFrontier(Frame frame, SearchLineHistory history) {
        final int moveCount = Gen.genAll(
            frame.board0, frame.board1, frame.board2, frame.board3,
            frame.status, frame.key, true, frame.moves, genScratch
        );
        if(moveCount == 0) {
            return computeCheckers(frame) != 0L ? -MATE_SCORE + frame.ply : 0;
        }
        if(isRuleDraw(frame, history)) return 0;
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
        frame.ruleDraw = false;
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

    private SearchResult interruptSearch(
        SearchObserver observer, Frame root, int requestedDepth, int rootMoveCount,
        long searchStartNanos
    ) {
        final boolean hasMove = root.bestMove != 0L;
        final SearchResult result = new SearchResult(
            hasMove ? root.bestMove : 0L,
            hasMove,
            hasMove ? root.bestScore : 0,
            requestedDepth,
            nodes,
            rootMoveCount,
            false
        );
        observer.onSearchFinished(result, System.nanoTime() - searchStartNanos);
        return result;
    }

    private SearchResult finishRuleDrawAtRoot(
        SearchObserver observer, long firstLegalMove, int requestedDepth, int rootMoveCount,
        long searchStartNanos
    ) {
        final SearchResult result = new SearchResult(
            firstLegalMove, true, 0, requestedDepth, 0L, rootMoveCount, true
        );
        observer.onSearchFinished(result, System.nanoTime() - searchStartNanos);
        return result;
    }

    private int countRootMoves(Frame root) {
        return Gen.genAll(
            root.board0, root.board1, root.board2, root.board3,
            root.status, root.key, true, moveStack[0], genScratch
        );
    }

    private static boolean isRuleDraw(Frame frame, SearchLineHistory history) {
        final RuleDraw draw = DrawAdjudicator.adjudicateNonTerminal(
            frame.board0, frame.board1, frame.board2, frame.board3,
            frame.status, frame.key, history
        );
        return draw != RuleDraw.NONE;
    }

}
