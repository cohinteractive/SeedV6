package com.ohinteractive.seedv6.search.evaluation;

import com.ohinteractive.seedv6.search.tt.TranspositionScores;

/**
 * Immutable, uncalibrated NNUE search units; never centipawns or model identity.
 * V1 uses the largest linear scale safe over the entire bounded output domain.
 * Magnitude rounding (ties away from zero) and the historical nonzero minimum
 * of one preserve exact odd symmetry and existing explicit/serialized mappings.
 * Thus V1 is zero at either signed zero, otherwise sign(v)*max(1,round(32511*abs(v))).
 * Unlike plain round(v*scale), tiny signed predictions remain distinguishable
 * from exact draws. Quantization ties are possible; strict ordering is not promised.
 */
public record NnueScoreMapping(double scale) {
    public static final String V1_ID = "seedv6.nnue.search.full-range-sign-preserving.v1";
    public static final NnueScoreMapping V1 = new NnueScoreMapping(TranspositionScores.MAX_NORMAL_SCORE);

    /** Larger explicit scales remain available for historical replay/research and can clip. */
    public NnueScoreMapping {
        if (!Double.isFinite(scale) || scale <= 0) {
            throw new IllegalArgumentException("NNUE score scale must be finite and positive.");
        }
    }

    /**
     * Maps bounded tanh(u). Round magnitude to nearest integer, preserve every nonzero
     * sign with a minimum magnitude of one, and clamp below the reserved mate band.
     * NaN/non-bounded inference is a failure, never a handcrafted fallback.
     */
    public int map(double boundedValue) {
        if (!Double.isFinite(boundedValue) || Math.abs(boundedValue) > 1) {
            throw new IllegalArgumentException("Expected finite NNUE tanh(u) in [-1, 1].");
        }
        if (boundedValue == 0) return 0;
        double magnitude = Math.min(TranspositionScores.MAX_NORMAL_SCORE,
                Math.abs(boundedValue) * scale);
        int score = (int) Math.max(1L, Math.round(magnitude));
        return boundedValue < 0 ? -score : score;
    }
}
