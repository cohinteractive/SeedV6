package com.ohinteractive.seedv6.search.exact;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BooleanSupplier;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.rules.DrawAdjudicator;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;

/**
 * R002 exact recursive negamax alpha-beta. Worker-confined, reusable, single
 * threaded. No TT, quiescence, selective search or iteration/time policy.
 *
 * Nodes count every entered position including the root, terminal positions and
 * static leaves; an entry refused by cancellation does not count. Input board
 * and immutable game history are preserved. Board.makeMoveInto writes six longs
 * into reusable child storage; no board copies/objects are created per node.
 */
public final class ExactSearch {
    // Shared numeric convention only: no table, probes, stores or TT score conversion.
    public static final int MATE_SCORE = TranspositionScores.MATE_SCORE;
    public static final int MAX_DEPTH = TranspositionScores.MAX_MATE_PLY;
    public static final int MAX_STATIC_SCORE = TranspositionScores.MAX_NORMAL_SCORE;
    public static final int INFINITY = MATE_SCORE + 1;
    public static final BooleanSupplier NEVER_CANCELLED = () -> false;
    private static final int MAX_MOVES = 512;

    private final ExactEvaluator evaluator;
    private final long[][] boards = new long[MAX_DEPTH + 1][Board.MAX_BITBOARDS];
    private final long[][] moves = new long[MAX_DEPTH + 1][MAX_MOVES];
    private final long[][] pv = new long[MAX_DEPTH + 1][MAX_DEPTH];
    private final int[] pvLength = new int[MAX_DEPTH + 1];
    // Generator and ordering scratch are used only before descending to children.
    private final long[] generatorScratch = new long[Board.MAX_BITBOARDS];
    private final long[] quietScratch = new long[MAX_MOVES];
    private SearchLineHistory history;
    private BooleanSupplier cancelled;
    private long nodes;
    private boolean active;

    public ExactSearch() { this(SearchEvaluation.handcrafted()); }

    public ExactSearch(SearchEvaluation evaluation) { this(ExactEvaluator.from(evaluation)); }

    public ExactSearch(ExactEvaluator evaluator) { this.evaluator = Objects.requireNonNull(evaluator, "evaluator"); }

    /** A FEN/standalone board has only its initial history; callers with game history must supply it. */
    public ExactSearchResult search(long[] board, int depth) {
        return search(board, GameHistory.initial(board), depth, NEVER_CANCELLED);
    }

    /**
     * Cancellation may come from any thread via a safely published supplier,
     * e.g. () -> !searchControl.checkpoint(). Thread interruption is also honored
     * without clearing the flag. No time-management policy is owned here.
     */
    public ExactSearchResult search(long[] board, GameHistory gameHistory, int depth, BooleanSupplier cancelled) {
        return searchWindow(board, gameHistory, depth, -INFINITY, INFINITY, cancelled);
    }

    public ExactSearchResult searchWindow(long[] board, GameHistory gameHistory, int depth,
                                          int alpha, int beta, BooleanSupplier cancelled) {
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(gameHistory, "gameHistory");
        Objects.requireNonNull(cancelled, "cancelled");
        if(board.length < Board.MAX_BITBOARDS) throw new IllegalArgumentException("Incomplete board.");
        if(depth < 0 || depth > MAX_DEPTH) throw new IllegalArgumentException("Depth must be 0.." + MAX_DEPTH);
        if(alpha < -INFINITY || beta > INFINITY || alpha >= beta) throw new IllegalArgumentException("Invalid window.");
        if(active) throw new IllegalStateException("ExactSearch cannot be re-entered.");
        gameHistory.requireCurrent(board);
        long start = System.nanoTime();
        active = true;
        nodes = 0;
        this.cancelled = cancelled;
        try {
            checkpoint();
            System.arraycopy(board, 0, boards[0], 0, Board.MAX_BITBOARDS);
            history = new SearchLineHistory(gameHistory, depth);
            evaluator.initialize(boards[0]);
            int score = negamax(depth, 0, alpha, beta);
            checkpoint();
            long[] line = Arrays.copyOf(pv[0], pvLength[0]);
            return new ExactSearchResult(depth, true, line.length == 0 ? 0 : line[0], score,
                    line, nodes, System.nanoTime() - start);
        } catch(Aborted ignored) {
            return new ExactSearchResult(depth, false, 0, Value.INVALID, new long[0],
                    nodes, System.nanoTime() - start);
        } finally {
            history = null;
            this.cancelled = null;
            active = false;
        }
    }

    private int negamax(int depth, int ply, int alpha, int beta) {
        checkpoint();
        nodes++;
        pvLength[ply] = 0;
        long[] board = boards[ply];
        int status = (int) board[Board.STATUS];
        long[] legalMoves = moves[ply];
        int count = Gen.genAll(board[0], board[1], board[2], board[3], status,
                board[Board.KEY], true, legalMoves, generatorScratch);
        // Mate/stalemate precede claimable rule draws and the nominal horizon.
        if(count == 0) {
            return Board.isPlayerInCheck(board[0], board[1], board[2], board[3], Board.player(status))
                    ? -MATE_SCORE + ply : 0;
        }
        if(DrawAdjudicator.adjudicateNonTerminal(board, history) != DrawAdjudicator.RuleDraw.NONE) return 0;
        if(depth <= 0) {
            int score = evaluator.evaluate(board, ply);
            if(score < -MAX_STATIC_SCORE || score > MAX_STATIC_SCORE) {
                throw new IllegalArgumentException("Static score enters the reserved mate band: " + score);
            }
            return score;
        }
        orderTacticalFirst(legalMoves, count, status);
        int best = -INFINITY;
        long[] child = boards[ply + 1];
        for(int i = 0; i < count; i++) {
            checkpoint();
            long move = legalMoves[i];
            Board.makeMoveInto(board[0], board[1], board[2], board[3], status, board[Board.KEY], move, child);
            evaluator.child(board, child, ply);
            history.pushRealPosition(child);
            int score;
            try {
                score = -negamax(depth - 1, ply + 1, -beta, -alpha);
            } finally {
                history.popRealPosition();
            }
            if(score > best) {
                best = score;
                pv[ply][0] = move;
                System.arraycopy(pv[ply + 1], 0, pv[ply], 1, pvLength[ply + 1]);
                pvLength[ply] = 1 + pvLength[ply + 1];
            }
            if(score >= beta) return score; // Fail-soft: retain the discovered value.
            if(score > alpha) alpha = score;
        }
        return best;
    }

    /** Stable two-way partition: captures (including EP) and promotions, then quiets. */
    private void orderTacticalFirst(long[] legalMoves, int count, int status) {
        int tactical = 0;
        int quiet = 0;
        int ep = Board.enPassantSquare(status);
        for(int i = 0; i < count; i++) {
            long move = legalMoves[i];
            boolean isTactical = ((move >>> Board.TARGET_PIECE_SHIFT) & Board.PIECE_BITS) != 0
                    || ((move >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS) != 0
                    || (((move >>> Board.START_PIECE_SHIFT) & Piece.TYPE) == Piece.PAWN && Move.toSquare(move) == ep);
            if(isTactical) legalMoves[tactical++] = move;
            else quietScratch[quiet++] = move;
        }
        System.arraycopy(quietScratch, 0, legalMoves, tactical, quiet);
    }

    private void checkpoint() {
        if(Thread.currentThread().isInterrupted() || cancelled.getAsBoolean()) throw ABORTED;
    }

    private static final Aborted ABORTED = new Aborted();
    private static final class Aborted extends RuntimeException {
        private Aborted() { super(null, null, false, false); }
    }
}
