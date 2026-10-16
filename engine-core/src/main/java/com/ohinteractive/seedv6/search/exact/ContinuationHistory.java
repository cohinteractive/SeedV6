package com.ohinteractive.seedv6.search.exact;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Piece;

/** Primitive immediate continuation history for the bounded SR-016 experiment. */
final class ContinuationHistory {
    static final int PIECE_SQUARES = 12 * 64;
    static final int SIZE = PIECE_SQUARES * PIECE_SQUARES;

    private ContinuationHistory() {}

    /** Valid packed codes 1..6 / 9..14 map to 0..5 / 6..11 without a lookup table. */
    static int compactPiece(int piece) {
        return (piece & Piece.TYPE) - 1 + (piece >>> Board.PLAYER_SHIFT) * 6;
    }

    /** One row per previous resulting piece/destination; -1 means no previous move (root). */
    static int context(long previousMove) {
        if(previousMove == 0) return -1;
        int piece = (int) (previousMove >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS;
        if(piece == 0) piece = (int) (previousMove >>> Board.START_PIECE_SHIFT) & Board.PIECE_BITS;
        return (compactPiece(piece) * 64 + Move.toSquare(previousMove)) * PIECE_SQUARES;
    }

    /** Current move is a generated legal quiet, so its moving piece is also its resulting piece. */
    static int index(int context, long move) {
        int piece = (int) (move >>> Board.START_PIECE_SHIFT) & Board.PIECE_BITS;
        return context + compactPiece(piece) * 64 + Move.toSquare(move);
    }

    /** Same searched quiet prefix and bonus as main history; no root or tactical evidence. */
    static void recordCutoff(short[] table, int context, long[] moves, int cutoff, int ep, int depth) {
        if(context < 0 || CaptureHistory.isTactical(moves[cutoff], ep)) return;
        int cappedDepth = Math.min(depth, QuietHistory.MAX_BONUS_DEPTH);
        int bonus = cappedDepth * cappedDepth;
        update(table, index(context, moves[cutoff]), bonus);
        for(int i = 0; i < cutoff; i++) {
            long move = moves[i];
            if(!CaptureHistory.isTactical(move, ep)) update(table, index(context, move), -bonus);
        }
    }

    /** Sign-extend short to int, perform bounded gravity safely, then narrow the bounded result. */
    static void update(short[] table, int index, int bonus) {
        bonus = Math.max(-QuietHistory.LIMIT, Math.min(QuietHistory.LIMIT, bonus));
        int current = table[index];
        table[index] = (short) (current + bonus - current * Math.abs(bonus) / QuietHistory.LIMIT);
    }
}
