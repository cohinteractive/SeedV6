package com.ohinteractive.seedv6.search.exact;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;

/** Accepted SR-016 primitive main history; ownership/reset belongs to one fixed-depth invocation. */
final class QuietHistory {
    static final int LIMIT = 16_384;
    static final int MAX_BONUS_DEPTH = 64;
    static final int SIZE = 16 * 64 * 64;

    private QuietHistory() {}

    /** Packed moving piece includes side: white 1..6, black 9..14; spare codes stay unused.
     * Direct four/six/six-bit indexing avoids remapping the noncontiguous piece codes. */
    static int index(long move) {
        int moving = (int) (move >>> Board.START_PIECE_SHIFT) & Board.PIECE_BITS;
        return (moving * 64 + Move.fromSquare(move)) * 64 + Move.toSquare(move);
    }

    /** Called only after completion checking. The existing node list retains the searched
     * prefix across recursion, including a failed quiet hash but excluding unsearched moves. */
    static void recordCutoff(int[] table, long[] moves, int cutoff, int ep, int depth) {
        long winner = moves[cutoff];
        if(CaptureHistory.isTactical(winner, ep)) return;
        int cappedDepth = Math.min(depth, MAX_BONUS_DEPTH);
        int bonus = cappedDepth * cappedDepth;
        update(table, index(winner), bonus);
        for(int i = 0; i < cutoff; i++) {
            long move = moves[i];
            if(!CaptureHistory.isTactical(move, ep)) update(table, index(move), -bonus);
        }
    }

    /** Signed gravity, division toward zero. Even LIMIT * LIMIT fits in a signed int. */
    static void update(int[] table, int index, int bonus) {
        bonus = Math.max(-LIMIT, Math.min(LIMIT, bonus));
        int current = table[index];
        table[index] = current + bonus - current * Math.abs(bonus) / LIMIT;
    }
}
