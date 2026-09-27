package com.ohinteractive.seedv6.search.exact;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;

/** One recent quiet cutoff reply per immediate previous-move context; SR-016 experiment. */
final class Countermoves {
    static final int SIZE = 12 * 64;

    private Countermoves() {}

    /** Compact resulting-piece/destination key; no previous move at root means no context. */
    static int context(long previousMove) {
        if(previousMove == 0) return -1;
        int piece = (int) (previousMove >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS;
        if(piece == 0) piece = (int) (previousMove >>> Board.START_PIECE_SHIFT) & Board.PIECE_BITS;
        // Reuse only the proved primitive 1..6/9..14 -> 0..11 mapping, not its history table.
        return ContinuationHistory.compactPiece(piece) * 64 + Move.toSquare(previousMove);
    }

    /** Called after completion checking with the actual searched cutoff move. */
    static void recordCutoff(long[] table, int context, long move, int ep) {
        if(context < 0 || move == 0 || CaptureHistory.isTactical(move, ep)) return;
        table[context] = move; // Newest successful quiet reply replaces the previous full move.
    }
}
