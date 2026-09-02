package com.ohinteractive.seedv6.uci;

import com.ohinteractive.seedv6.search.tt.TranspositionScores;

/** Exact conversion from V6 side-to-move scores to UCI cp/mate fields. */
record UciScore(boolean mate, int value) {

    static UciScore fromInternal(int score) {
        if(!TranspositionScores.isMateScore(score)) {
            return new UciScore(false, score);
        }
        final int pliesToMate = TranspositionScores.MATE_SCORE - Math.abs(score);
        final int movesToMate = (pliesToMate + 1) / 2;
        return new UciScore(true, score < 0 ? -movesToMate : movesToMate);
    }

    String fields() {
        return (mate ? "mate " : "cp ") + value;
    }
}
