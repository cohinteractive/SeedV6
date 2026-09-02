package com.ohinteractive.seedv6.search.quiescence;

import java.util.Objects;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.rules.DrawAdjudicator;
import com.ohinteractive.seedv6.rules.DrawAdjudicator.RuleDraw;
import com.ohinteractive.seedv6.rules.SearchLineHistory;
import com.ohinteractive.seedv6.search.common.SearchControl;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnostics;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot;
import com.ohinteractive.seedv6.search.order.MoveOrdering;
import com.ohinteractive.seedv6.search.order.StagedMovePicker;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;

/**
 * Worker-owned, score-focused check-aware quiescence search.
 *
 * <p>Non-check tactical expansion stops at {@link #SOFT_QPLY_LIMIT}. Check is
 * always established first and checked nodes continue through the complete
 * legal evasion set even at that soft boundary. The absolute ply bound is the
 * existing SeedV6 mate-score band; attempting to enter a child beyond it is a
 * controlled search failure, never a fabricated chess score.</p>
 *
 * <p>The returned {@link Result} is reused by this worker and is overwritten by
 * the next invocation. One instance must not be used concurrently.</p>
 */
public final class QuiescenceSearch {

    /**
     * Sixteen tactical qplies cover ordinary exchange chains while bounding
     * unpruned non-check expansion well below the 256-ply mate/storage band.
     */
    public static final int SOFT_QPLY_LIMIT = 16;

    /** Highest root-relative ply representable by the established mate band. */
    public static final int MAX_ABSOLUTE_PLY = TranspositionScores.MAX_MATE_PLY;

    public QuiescenceSearch() {
        this(new MoveOrdering(MAX_ABSOLUTE_PLY + 1));
    }

    /** Use a worker-owned ordering instance that has the complete mate-band capacity. */
    public QuiescenceSearch(MoveOrdering ordering) {
        this.ordering = Objects.requireNonNull(ordering, "ordering");
        if(ordering.maxPly() <= MAX_ABSOLUTE_PLY) {
            throw new IllegalArgumentException(
                "Qsearch ordering must support absolute plies 0 through " + MAX_ABSOLUTE_PLY
            );
        }
        picker = ordering.picker();
    }

    /** Search an immutable request root at absolute/q-ply zero. */
    public Result search(SearchRequest request, int alpha, int beta) {
        Objects.requireNonNull(request, "request");
        final SearchLineHistory history = new SearchLineHistory(request.gameHistory());
        final SearchDiagnostics searchDiagnostics;
        if(request.diagnosticsEnabled()) {
            standaloneDiagnostics.reset();
            searchDiagnostics = standaloneDiagnostics;
        } else {
            searchDiagnostics = null;
        }
        try {
            request.copyBoardInto(boardStack[0]);
            final Result completed = run(
                boardStack[0], history, request.control(), 0, 0, alpha, beta,
                searchDiagnostics
            );
            lastDiagnostics = searchDiagnostics == null
                ? SearchDiagnosticsSnapshot.disabled() : searchDiagnostics.snapshot();
            return completed;
        } finally {
            history.restoreRoot();
        }
    }

    /**
     * Allocation-free leaf entry for the future main search. The supplied
     * private line history must already end at {@code board}; this method leaves
     * its size and top position unchanged on every normal or exceptional exit.
     */
    public Result searchLeaf(
        long[] board, SearchLineHistory history, SearchControl control,
        int absolutePly, int alpha, int beta
    ) {
        return run(board, history, control, absolutePly, 0, alpha, beta, null);
    }

    /** Main-search leaf entry sharing its top-level worker diagnostics scope. */
    public Result searchLeaf(
        long[] board, SearchLineHistory history, SearchControl control,
        int absolutePly, int alpha, int beta, SearchDiagnostics diagnostics
    ) {
        return run(board, history, control, absolutePly, 0, alpha, beta, diagnostics);
    }

    /** Package-visible boundary entry used to prove the named q-depth policy. */
    Result searchAtQply(
        long[] board, SearchLineHistory history, SearchControl control,
        int absolutePly, int qPly, int alpha, int beta
    ) {
        return run(board, history, control, absolutePly, qPly, alpha, beta, null);
    }

    Result searchAtQply(
        long[] board, SearchLineHistory history, SearchControl control,
        int absolutePly, int qPly, int alpha, int beta, SearchDiagnostics diagnostics
    ) {
        return run(board, history, control, absolutePly, qPly, alpha, beta, diagnostics);
    }

    public MoveOrdering ordering() {
        return ordering;
    }

    /** Final immutable snapshot from the most recent standalone request. */
    public SearchDiagnosticsSnapshot lastDiagnostics() {
        return lastDiagnostics;
    }

    private static final int MAX_MOVES = StagedMovePicker.MAX_MOVES;

    private final MoveOrdering ordering;
    private final StagedMovePicker picker;
    private final long[][] boardStack =
        new long[MAX_ABSOLUTE_PLY + 1][Board.MAX_BITBOARDS];
    private final long[] legalAvailabilityMoves = new long[MAX_MOVES];
    private final long[] generatorScratch = new long[Board.MAX_BITBOARDS];
    private final Result result = new Result();
    private final SearchDiagnostics standaloneDiagnostics = new SearchDiagnostics();

    private SearchControl control;
    private long enteredNodes;
    private boolean aborted;
    private boolean active;
    private boolean pathDependent;
    private SearchDiagnostics diagnostics;
    private SearchDiagnosticsSnapshot lastDiagnostics = SearchDiagnosticsSnapshot.disabled();

    private Result run(
        long[] board, SearchLineHistory history, SearchControl control,
        int absolutePly, int qPly, int alpha, int beta, SearchDiagnostics diagnostics
    ) {
        requireBoard(board);
        Objects.requireNonNull(history, "history");
        Objects.requireNonNull(control, "control");
        requirePly(absolutePly);
        if(qPly < 0) throw new IllegalArgumentException("Q-ply must not be negative: " + qPly);
        if(alpha >= beta) {
            throw new IllegalArgumentException(
                "Qsearch requires alpha below beta: alpha=" + alpha + ", beta=" + beta
            );
        }
        if(alpha == Integer.MIN_VALUE || beta == Integer.MIN_VALUE) {
            throw new IllegalArgumentException(
                "Qsearch window endpoints must be safely negatable."
            );
        }
        if(active) throw new IllegalStateException("QuiescenceSearch is already active.");

        active = true;
        this.control = control;
        this.diagnostics = diagnostics;
        enteredNodes = 0L;
        aborted = false;
        pathDependent = false;
        result.reset();
        if(diagnostics != null) diagnostics.recordRoot();
        System.arraycopy(board, 0, boardStack[absolutePly], 0, Board.MAX_BITBOARDS);
        final int initialHistorySize = history.size();
        try {
            if(!control.checkpoint()) {
                aborted = true;
                result.abort(enteredNodes);
                return result;
            }
            final int score = searchNode(
                boardStack[absolutePly], history, absolutePly, qPly, alpha, beta, false
            );
            if(aborted) result.abort(enteredNodes);
            else result.complete(score, enteredNodes, pathDependent);
            return result;
        } finally {
            this.control = null;
            this.diagnostics = null;
            active = false;
            if(history.size() != initialHistorySize) {
                throw new IllegalStateException(
                    "Qsearch line-history imbalance: expected " + initialHistorySize
                        + ", actual " + history.size()
                );
            }
        }
    }

    private int searchNode(
        long[] board, SearchLineHistory history, int absolutePly, int qPly,
        int alpha, int beta, boolean countedQNode
    ) {
        if(!control.checkpoint()) {
            aborted = true;
            return 0;
        }

        if(diagnostics != null) diagnostics.recordQPosition(absolutePly, qPly);
        final long checkers = computeCheckers(board);
        final boolean inCheck = checkers != 0L;
        if(diagnostics != null && countedQNode && inCheck) diagnostics.recordCheckedQNode();

        if(!inCheck && qPly >= SOFT_QPLY_LIMIT) {
            if(diagnostics != null) diagnostics.recordSoftQdepthLimitEncounter();
            final int legalCount = Gen.genAll(
                board[0], board[1], board[2], board[3],
                Math.toIntExact(board[Board.STATUS]), board[Board.KEY], true,
                legalAvailabilityMoves, generatorScratch
            );
            if(legalCount == 0) return 0;
            if(isRuleDraw(board, history)) return 0;
            return Eval.evaluate(board);
        }

        boolean pickerTouched = false;
        try {
            pickerTouched = true;
            final int moveCount = picker.prepareQuiescence(
                board, absolutePly, StagedMovePicker.NO_MOVE, checkers
            );

            if(inCheck) {
                if(moveCount == 0) {
                    if(diagnostics != null) diagnostics.recordQmate();
                    return -TranspositionScores.MATE_SCORE + absolutePly;
                }
                if(isRuleDraw(board, history)) return 0;
                return searchMoves(
                    board, history, absolutePly, qPly, alpha, beta, Integer.MIN_VALUE, true
                );
            }

            if(moveCount == 0) {
                final int quietCount = Gen.genQuiet(
                    board[0], board[1], board[2], board[3],
                    Math.toIntExact(board[Board.STATUS]), board[Board.KEY], true,
                    legalAvailabilityMoves, generatorScratch
                );
                if(quietCount == 0) return 0;
            }
            if(isRuleDraw(board, history)) return 0;

            final int standPat = Eval.evaluate(board);
            if(standPat >= beta) {
                if(diagnostics != null) diagnostics.recordStandPatCutoff();
                return standPat;
            }
            final int raisedAlpha = Math.max(alpha, standPat);
            if(moveCount == 0) return standPat;
            return searchMoves(
                board, history, absolutePly, qPly, raisedAlpha, beta, standPat, false
            );
        } finally {
            if(pickerTouched) picker.clearPly(absolutePly);
        }
    }

    private int searchMoves(
        long[] board, SearchLineHistory history, int absolutePly, int qPly,
        int alpha, int beta, int initialBest, boolean evasion
    ) {
        int best = initialBest;
        long move;
        while((move = picker.next(absolutePly)) != StagedMovePicker.NO_MOVE) {
            if(absolutePly == MAX_ABSOLUTE_PLY) {
                throw new QuiescenceCapacityException(
                    "Qsearch cannot enter a child beyond absolute ply " + MAX_ABSOLUTE_PLY
                );
            }
            if(!control.tryEnterNode()) {
                aborted = true;
                return 0;
            }
            if(diagnostics != null) {
                diagnostics.recordQNode(absolutePly + 1, qPly + 1);
                diagnostics.recordQMoveSearched(evasion);
            }

            final long[] child = boardStack[absolutePly + 1];
            Board.makeMoveInto(
                board[0], board[1], board[2], board[3],
                Math.toIntExact(board[Board.STATUS]), board[Board.KEY], move, child
            );
            enteredNodes ++;
            history.pushRealPosition(child);
            final int childScore;
            try {
                childScore = searchNode(
                    child, history, absolutePly + 1, qPly + 1, -beta, -alpha, true
                );
            } finally {
                history.popRealPosition();
            }
            if(aborted) return 0;

            final int score = -childScore;
            if(score > best) best = score;
            if(score >= beta) return score;
            if(score > alpha) alpha = score;
        }
        return best;
    }

    private static long computeCheckers(long[] board) {
        final int status = Math.toIntExact(board[Board.STATUS]);
        final int player = status & Board.PLAYER_BIT;
        final long allOccupancy = board[0] | board[1] | board[2];
        final long colorMask = ~(-(long) player ^ board[3]);
        final long king = board[0] & ~board[1] & ~board[2] & colorMask;
        final int kingSquare = Long.numberOfTrailingZeros(king);
        return Board.getCheckersPext(
            board[0], board[1], board[2], board[3], colorMask, player,
            kingSquare, allOccupancy
        );
    }

    private boolean isRuleDraw(long[] board, SearchLineHistory history) {
        final RuleDraw draw = DrawAdjudicator.adjudicateNonTerminal(board, history);
        if(draw == RuleDraw.FIFTY_MOVE || draw == RuleDraw.FORMAL_THREEFOLD) {
            pathDependent = true;
        }
        return draw != RuleDraw.NONE;
    }

    private static void requireBoard(long[] board) {
        Objects.requireNonNull(board, "board");
        if(board.length < Board.MAX_BITBOARDS) {
            throw new IllegalArgumentException(
                "Board must contain at least " + Board.MAX_BITBOARDS + " longs."
            );
        }
    }

    private static void requirePly(int absolutePly) {
        if(absolutePly < 0 || absolutePly > MAX_ABSOLUTE_PLY) {
            throw new IllegalArgumentException(
                "Absolute ply must be between 0 and " + MAX_ABSOLUTE_PLY
                    + ": " + absolutePly
            );
        }
    }

    /** Reusable, allocation-free invocation result. */
    public static final class Result {
        public boolean completed() {
            return completed;
        }

        /** A chess score exists only for a completed invocation. */
        public int score() {
            if(!completed) {
                throw new IllegalStateException("An interrupted qsearch has no completed score.");
            }
            return score;
        }

        /** Counted qsearch child entries; the already-entered leaf root is excluded. */
        public long nodes() {
            return nodes;
        }

        /**
         * True when any completed branch used repetition or the 50-move rule.
         * Main search propagates this conservatively to its TT cacheability.
         */
        public boolean pathDependent() {
            if(!completed) {
                throw new IllegalStateException(
                    "An interrupted qsearch has no completed cacheability result."
                );
            }
            return pathDependent;
        }

        private boolean completed;
        private int score;
        private long nodes;
        private boolean pathDependent;

        private void reset() {
            completed = false;
            score = 0;
            nodes = 0L;
            pathDependent = false;
        }

        private void complete(int score, long nodes, boolean pathDependent) {
            this.completed = true;
            this.score = score;
            this.nodes = nodes;
            this.pathDependent = pathDependent;
        }

        private void abort(long nodes) {
            completed = false;
            score = 0;
            this.nodes = nodes;
            pathDependent = false;
        }
    }

    /** Absolute-ply exhaustion is a lifecycle failure, not a chess evaluation. */
    public static final class QuiescenceCapacityException extends IllegalStateException {
        public QuiescenceCapacityException(String message) {
            super(message);
        }
    }
}
