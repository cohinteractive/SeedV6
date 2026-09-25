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
import com.ohinteractive.seedv6.search.tt.TTable;

/**
 * R002 exact recursive negamax alpha-beta. Worker-confined, reusable, single
 * threaded. Optional R005 exact TT evidence; no selective search or time policy.
 *
 * Nodes count every entered position including the root, terminal positions and
 * static leaves; an entry refused by cancellation does not count. Input board
 * and immutable game history are preserved. Board.makeMoveInto writes six longs
 * into reusable child storage; no board copies/objects are created per node.
 */
public final class ExactSearch {
    public static final int MATE_SCORE = TranspositionScores.MATE_SCORE;
    public static final int MAX_DEPTH = TranspositionScores.MAX_MATE_PLY;
    public static final int MAX_STATIC_SCORE = TranspositionScores.MAX_NORMAL_SCORE;
    public static final int INFINITY = MATE_SCORE + 1;
    public static final BooleanSupplier NEVER_CANCELLED = () -> false;
    private static final int MAX_MOVES = 512;

    private final ExactEvaluator evaluator;
    private final TTable table;
    private final TTable.TEntry entry;
    private final long[] historyKeys;
    private int generation;
    private boolean requestActive;
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

    public ExactSearch(ExactEvaluator evaluator) { this(evaluator, null); }

    public ExactSearch(SearchEvaluation evaluation, TTable table) { this(ExactEvaluator.from(evaluation), table); }

    /**
     * Transfers exclusive ownership of a fresh table (generation zero), or null
     * for TT-off. The evaluator must give the same board the same static value;
     * ply selects its stack slot, not a different evaluation function.
     */
    public ExactSearch(ExactEvaluator evaluator, TTable table) {
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
        this.table = table;
        entry = table == null ? null : new TTable.TEntry();
        historyKeys = table == null ? null : new long[MAX_DEPTH + 1];
    }

    /** One driver request may contain several fixed-depth invocations. */
    public void beginRequest() {
        if(active || requestActive) throw new IllegalStateException("Search request already active.");
        advanceGeneration();
        requestActive = true;
    }

    public void endRequest() {
        if(active || !requestActive) throw new IllegalStateException("No idle Search request.");
        requestActive = false;
    }

    public void newGame() {
        if(active || requestActive) throw new IllegalStateException("Search request active.");
        if(table != null) table.clear();
    }

    private void advanceGeneration() {
        if(table != null) {
            table.advanceGeneration();
            generation = (generation + 1) & 255;
        }
    }

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
        if(!requestActive) advanceGeneration();
        long start = System.nanoTime();
        active = true;
        nodes = 0;
        this.cancelled = cancelled;
        try {
            checkpoint();
            System.arraycopy(board, 0, boards[0], 0, Board.MAX_BITBOARDS);
            history = new SearchLineHistory(gameHistory, depth);
            if(table != null) historyKeys[0] = SearchKey.rootHistory(board, gameHistory);
            evaluator.initialize(boards[0]);
            int score = negamax(depth, 0, alpha, beta);
            checkpoint();
            if(table != null) store(SearchKey.key(boards[0], historyKeys[0]), depth, 0,
                    alpha, beta, score, pvLength[0] == 0 ? 0 : pv[0][0]);
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
        final int originalAlpha = alpha;
        final int originalBeta = beta;
        long key = 0;
        long hashMove = 0;
        if(table != null) {
            key = SearchKey.key(board, historyKeys[ply]);
            if(table.probe(key, entry)) {
                hashMove = entry.hashMove; // Candidate only; validate when a PV or ordering uses it.
                long data = entry.data;
                if((data & 255) == depth && ((data >>> 10) & 255) == generation) {
                    int score = TranspositionScores.fromTableScore((int) (data >> 32), ply);
                    int type = (int) (data >>> 8) & 3;
                    if(type == TTable.TYPE_EXACT || (type == TTable.TYPE_LOWER && score >= beta)
                            || (type == TTable.TYPE_UPPER && score <= alpha)) {
                        // A validated one-move prefix is sufficient. A positive-depth
                        // nonterminal root must never be reported as having no move.
                        if(depth > 0 && hashMove != 0) {
                            for(int i = 0; i < count; i++) {
                                if(legalMoves[i] == hashMove) {
                                    pv[ply][0] = hashMove;
                                    pvLength[ply] = 1;
                                    break;
                                }
                            }
                        }
                        if(ply != 0 || depth == 0 || pvLength[ply] != 0) return score;
                        hashMove = 0; // The positive-depth root still needs a legal best move.
                    }
                }
            }
        }
        if(depth <= 0) {
            int score = evaluator.evaluate(board, ply);
            if(score < -MAX_STATIC_SCORE || score > MAX_STATIC_SCORE) {
                throw new IllegalArgumentException("Static score enters the reserved mate band: " + score);
            }
            return completed(key, depth, ply, originalAlpha, originalBeta, score, 0);
        }
        orderTacticalFirst(legalMoves, count, status);
        if(hashMove != 0) promoteHashMove(legalMoves, count, hashMove);
        int best = -INFINITY;
        long[] child = boards[ply + 1];
        for(int i = 0; i < count; i++) {
            checkpoint();
            long move = legalMoves[i];
            Board.makeMoveInto(board[0], board[1], board[2], board[3], status, board[Board.KEY], move, child);
            evaluator.child(board, child, ply);
            history.pushRealPosition(child);
            if(table != null) historyKeys[ply + 1] = SearchKey.childHistory(
                    historyKeys[ply], history.currentKey(), (int) child[Board.STATUS]);
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
            if(score >= beta) return completed(key, depth, ply, originalAlpha, originalBeta, score, move);
            if(score > alpha) alpha = score;
        }
        return completed(key, depth, ply, originalAlpha, originalBeta, best, pv[ply][0]);
    }

    private int completed(long key, int depth, int ply, int alpha, int beta, int score, long move) {
        if(table != null) {
            checkpoint(); // Includes cancellation raised by the final evaluated child.
            // R004 has eight depth bits; depth 256 must never alias depth zero.
            if(ply != 0) store(key, depth, ply, alpha, beta, score, move);
        }
        return score;
    }

    private void store(long key, int depth, int ply, int alpha, int beta, int score, long move) {
        if(depth > 255) return;
        int type = score <= alpha ? TTable.TYPE_UPPER : score >= beta ? TTable.TYPE_LOWER : TTable.TYPE_EXACT;
        table.save(key, depth, type, TranspositionScores.toTableScore(score, ply), move);
    }

    /** Validate against generated legal moves and stably promote in the same pass. */
    private static void promoteHashMove(long[] moves, int count, long hashMove) {
        for(int i = 0; i < count; i++) {
            if(moves[i] == hashMove) {
                System.arraycopy(moves, 0, moves, 1, i);
                moves[0] = hashMove;
                return;
            }
        }
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
