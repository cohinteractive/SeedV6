package com.ohinteractive.seedv6.search.exact;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.See;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.rules.DrawAdjudicator;
import com.ohinteractive.seedv6.rules.SearchLineHistory;

/** Test-only complete enumeration: full legal generation, no windows, sorting or cutoffs.
 * The default remains SR-001A without SEE; SR-001B can enumerate a candidate's restricted tree. */
final class QuiescenceOracle {
    private final ExactEvaluator evaluator;
    private final int pruning;
    long nodes;
    /** Optional SR-001H evidence, after complete enumeration; never changes admission/value. */
    interface NodeEvidence {
        void completed(long[] board, int ply, int qply, int stand, int best,
                       long[] moves, int[] values, int[] causes);
    }
    NodeEvidence evidence;
    int lastCause;

    QuiescenceOracle(ExactEvaluator evaluator) { this(evaluator, ExactSearch.QSEARCH_BASELINE); }

    QuiescenceOracle(ExactEvaluator evaluator, int pruning) {
        this.evaluator = evaluator;
        this.pruning = pruning;
    }

    int score(long[] board, SearchLineHistory history, int remaining, int ply) {
        return score(board, history, remaining, ply, 0);
    }

    int score(long[] board, SearchLineHistory history, int remaining, int ply, int qply) {
        return score(board, history, remaining, ply, qply, 0);
    }

    int score(long[] board, SearchLineHistory history, int remaining, int ply, int qply, int quietChecksUsed) {
        // A fixture guard rejects an unsuitable test; it never substitutes a chess value.
        if(++nodes > 200_000 || ply > 64) throw new AssertionError("Unbounded oracle fixture");
        long[] moves = ExhaustiveOracle.legalMoves(board);
        boolean check = Board.isPlayerInCheck(board[0], board[1], board[2], board[3],
                Board.player((int) board[Board.STATUS]));
        lastCause = 0;
        if(moves.length == 0) { lastCause=check ? 1 : 2; return check ? -32768 + ply : 0; }
        var draw = DrawAdjudicator.adjudicateNonTerminal(board, history);
        if(draw != DrawAdjudicator.RuleDraw.NONE) { lastCause=draw.ordinal()+2; return 0; }
        int best = remaining <= 0 && !check ? evaluator.evaluate(board, ply) : -32769;
        int stand=best, bestCause=0;
        int[] values=evidence==null ? null : new int[moves.length];
        int[] causes=evidence==null ? null : new int[moves.length];
        if(values!=null) java.util.Arrays.fill(values,Integer.MIN_VALUE);
        if(remaining <= 0 && !check && (pruning == ExactSearch.QDEPTH_4
                || pruning == ExactSearch.QDEPTH_8 || pruning == ExactSearch.QDEPTH_12) && qply >= pruning) return best;
        for(int index=0; index<moves.length; index++) {
            long move=moves[index];
            boolean quiet = remaining <= 0 && !check && !tactical(board, move);
            if(quiet && !(pruning == ExactSearch.QSEARCH_QUIET_CHECKS
                    || pruning == ExactSearch.QCHECK_INITIAL_ONLY && qply == 0
                    || pruning == ExactSearch.QCHECK_MAX_ONE && quietChecksUsed == 0
                    || pruning == ExactSearch.QCHECK_MAX_TWO && quietChecksUsed < 2
                    || pruning >= ExactSearch.QCHECK_FORCED_ONE && pruning <= ExactSearch.QCHECK_INITIAL_PLUS_TWO)) continue;
            long[] child = ExhaustiveOracle.child(board, move);
            if(quiet && !inCheck(child)) continue;
            if(quiet && pruning >= ExactSearch.QCHECK_FORCED_ONE
                    && (pruning == ExactSearch.QCHECK_FORCED_ONE || qply > 0)
                    && ExhaustiveOracle.legalMoves(child).length > (pruning == ExactSearch.QCHECK_INITIAL_PLUS_TWO ? 2 : 1)) continue;
            if(remaining <= 0 && !check && prunes(board, move, child, pruning)) continue;
            history.pushRealPosition(child);
            int value;
            try { value = -score(child, history, remaining - 1, ply + 1, remaining <= 0 ? qply + 1 : 0,
                    remaining <= 0 ? quietChecksUsed + (quiet ? 1 : 0) : 0); }
            finally { history.popRealPosition(); }
            if(values!=null) { values[index]=value; causes[index]=lastCause; }
            if(value>best) { best=value; bestCause=lastCause; }
        }
        lastCause=bestCause;
        if(evidence!=null && remaining<=0 && !check)
            evidence.completed(board,ply,qply,stand,best,moves,values,causes);
        return best;
    }

    /** Independent eligibility expression; deliberately does not consume production ordering keys. */
    static boolean prunes(long[] board, long move, long[] child, int policy) {
        if(policy == ExactSearch.QSEARCH_BASELINE || policy >= ExactSearch.QDEPTH_4 || inCheck(board)
                || See.atLeastGeneratedLegal(board, move, 0)) return false;
        if(policy >= ExactSearch.SEE_PROMO_SAFE && promotion(move)) return false;
        return policy != ExactSearch.SEE_CHECK_PROMO_SAFE || !inCheck(child);
    }

    static boolean promotion(long move) {
        return ((move >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS) != 0;
    }

    static boolean inCheck(long[] board) {
        return Board.isPlayerInCheck(board[0], board[1], board[2], board[3],
                Board.player((int) board[Board.STATUS]));
    }

    static boolean tactical(long[] board, long move) {
        return ((move >>> Board.TARGET_PIECE_SHIFT) & Board.PIECE_BITS) != 0
                || ((move >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS) != 0
                || (((move >>> Board.START_PIECE_SHIFT) & Piece.TYPE) == Piece.PAWN
                    && Move.toSquare(move) == Board.enPassantSquare((int) board[Board.STATUS]));
    }
}
