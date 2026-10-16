package com.ohinteractive.seedv6.search.evaluation;

import com.ohinteractive.seedv6.search.tt.TranspositionScores;

/**
 * BRN-0 search units, not centipawns or calibrated probability. The normalized side-to-move
 * value uses the complete normal score range (32511), leaving the mate band reserved.
 * Matches the established bounded-value policy: magnitude rounding, ties away from zero,
 * and a minimum magnitude of one for nonzero values. Monotone, odd and deterministic.
 * Training uses terminal W/D/L directly; this mapping is never inverted to create targets.
 */
public final class BrnScoreMapping {
    public static final int SCALE = TranspositionScores.MAX_NORMAL_SCORE;
    private BrnScoreMapping() {}

    public static int map(double value) {
        if (!Double.isFinite(value) || Math.abs(value) > 1) {
            throw new IllegalArgumentException("Expected finite BRN value in [-1,+1].");
        }
        if (value == 0) return 0;
        int magnitude = (int) Math.max(1L, Math.round(Math.abs(value) * SCALE));
        return value < 0 ? -magnitude : magnitude;
    }
}
