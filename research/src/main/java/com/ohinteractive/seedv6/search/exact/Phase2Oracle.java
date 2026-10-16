package com.ohinteractive.seedv6.search.exact;

import java.util.Arrays;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.*;

/** Shared independent oracle/PV checks, extracted unchanged from the JUnit fixture. */
final class Phase2Oracle {
    static void legalPv(long[] board, long[] pv) {
        for(long move : pv) {
            if (!Arrays.stream(ExhaustiveOracle.legalMoves(board)).anyMatch(m -> m == move))
                throw new AssertionError("Illegal independent-reference PV move: " + move);
            board = ExhaustiveOracle.child(board, move);
        }
    }

    /** No windows, ordering keys, production horizon helper or TT. */
    static final class RestrictedOracle {
        final ExactEvaluator evaluator; final int policy; long nodes;
        RestrictedOracle(ExactEvaluator evaluator, int policy) { this.evaluator = evaluator; this.policy = policy; }
        int score(long[] b, SearchLineHistory history, int depth, int ply, int qply, int previousTo) {
            if(++nodes > 2_000_000 || ply > 64) throw new AssertionError("Oracle fixture guard");
            long[] moves = ExhaustiveOracle.legalMoves(b);
            boolean checked = QuiescenceOracle.inCheck(b);
            if(moves.length == 0) return checked ? -32768 + ply : 0;
            if(DrawAdjudicator.adjudicateNonTerminal(b, history) != DrawAdjudicator.RuleDraw.NONE) return 0;
            boolean leaf = depth <= 0;
            int best = leaf && !checked ? evaluator.evaluate(b, ply) : -32769;
            int cap = policy == 66 ? 0 : policy == 67 ? 2 : policy == 4 || policy == 8 || policy == 12 ? policy : 256;
            if(leaf && !checked && qply >= cap) return best;
            for(long move : moves) {
                boolean tactical = QuiescenceOracle.tactical(b, move);
                if(leaf && !checked && !tactical && !((policy == 33 || policy == 65) && qply == 0)) continue;
                var child = ExhaustiveOracle.child(b, move);
                if(leaf && !checked && !tactical && !QuiescenceOracle.inCheck(child)) continue;
                if(leaf && !checked && policy == 64 && qply > 0 && !QuiescenceOracle.promotion(move)
                        && Move.toSquare(move) != previousTo) continue;
                if(leaf && tactical && QuiescenceOracle.prunes(b, move, child, policy == 65 ? 2 : policy)) continue;
                history.pushRealPosition(child);
                int value;
                try { value = -score(child, history, depth - 1, ply + 1, leaf ? qply + 1 : 0, Move.toSquare(move)); }
                finally { history.popRealPosition(); }
                best = Math.max(best, value);
            }
            return best;
        }
    }
}
