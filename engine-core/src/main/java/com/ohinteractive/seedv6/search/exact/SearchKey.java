package com.ohinteractive.seedv6.search.exact;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;

/** Conservative R005 value identity; no position-only fallback for hash moves. */
final class SearchKey {
    private static final long SEED = 0x243f6a8885a308d3L;

    static long rootHistory(long[] board, GameHistory game) {
        int clock = Board.halfMoveClock((int) board[Board.STATUS]);
        int first = clock == Board.MAX_HALF_MOVE_CLOCK ? 0 : Math.max(0, game.size() - 1 - clock);
        long fingerprint = SEED;
        for(int i = first; i < game.size(); i++) fingerprint = append(fingerprint, game.keyAt(i));
        return fingerprint;
    }

    static long childHistory(long parent, long repetitionKey, int status) {
        // Pawn moves/captures reset both the rule clock and the relevant history.
        return append(Board.halfMoveClock(status) == 0 ? SEED : parent, repetitionKey);
    }

    static long key(long[] board, long history) {
        // BRN reads the complete status long, including fullmove bits. Keeping it
        // also safely distinguishes raw EP context from canonical repetition EP.
        return mix(board[Board.KEY] ^ Long.rotateLeft(history, 23) ^ mix(board[Board.STATUS]));
    }

    private static long append(long history, long key) { return mix(history ^ mix(key)); }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }

    private SearchKey() {}
}
