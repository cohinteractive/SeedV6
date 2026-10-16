package com.ohinteractive.seedv6.search.exact;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Piece;

/** Primitive mechanics for the fixed-depth capture-history experiment, not shared Search state. */
final class CaptureHistory {
    static final int LIMIT = 16_384;
    static final int MAX_BONUS_DEPTH = 64;
    static final int SIZE = 16 * 64 * 8;

    private CaptureHistory() {}

    /** Side/piece (four packed bits), destination, victim type; zero is the no-victim bucket.
     * Victim colour is implied by the moving side for generated legal captures. */
    static int index(long move, int ep) {
        int moving = (int) (move >>> Board.START_PIECE_SHIFT) & Board.PIECE_BITS;
        int victim = (int) (move >>> Board.TARGET_PIECE_SHIFT) & Piece.TYPE;
        int to = Move.toSquare(move);
        if(victim == 0 && (moving & Piece.TYPE) == Piece.PAWN && to == ep) victim = Piece.PAWN;
        return ((moving * 64 + to) * 8) + victim;
    }

    static boolean isTactical(long move, int ep) {
        return ((move >>> Board.TARGET_PIECE_SHIFT) & Board.PIECE_BITS) != 0
                || ((move >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS) != 0
                || (((move >>> Board.START_PIECE_SHIFT) & Piece.TYPE) == Piece.PAWN && Move.toSquare(move) == ep);
    }

    /** A searched tactical cutoff rewards its move and penalizes only earlier searched tacticals.
     * The existing node list survives child recursion, so no extra per-node storage is needed. */
    static void recordCutoff(int[] table, long[] moves, int cutoff, int ep, int depth) {
        long winner = moves[cutoff];
        if(!isTactical(winner, ep)) return;
        int cappedDepth = Math.min(depth, MAX_BONUS_DEPTH);
        int bonus = cappedDepth * cappedDepth;
        update(table, index(winner, ep), bonus);
        for(int i = 0; i < cutoff; i++) {
            long move = moves[i];
            if(isTactical(move, ep)) update(table, index(move, ep), -bonus);
        }
    }

    /** Signed gravity, with integer division toward zero. Bounded input and product fit in int. */
    static void update(int[] table, int index, int bonus) {
        bonus = Math.max(-LIMIT, Math.min(LIMIT, bonus));
        int current = table[index];
        table[index] = current + bonus - current * Math.abs(bonus) / LIMIT;
    }
}
