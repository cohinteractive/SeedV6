package com.ohinteractive.seedv6.core.brn3;

import com.ohinteractive.seedv6.training.nnue.NnueCorpusTargets;

/** Supervision/validation adapter only. No WDL link is part of the runtime evaluator. */
public final class Brn3Objective {
    public static double outcome(double pawns,long[] board) {
        return NnueCorpusTargets.wdl(Math.round(pawns*100),NnueCorpusTargets.material(board)).target();
    }
    /** Differentiable supervision only; never a runtime feature or inference score mapping. */
    public static double[] smoothOutcome(double pawns, int material) {
        double m = Math.max(17, Math.min(78, material)) / 58.0;
        double a = ((-13.50030198 * m + 40.92780883) * m - 36.82753545) * m + 386.83004070;
        double b = ((96.53354896 * m - 165.79058388) * m + 90.89679019) * m + 49.29561889;
        double w = 1 / (1 + Math.exp((a - pawns * a) / b)), l = 1 / (1 + Math.exp((a + pawns * a) / b));
        return new double[]{w - l, a / b * (w * (1 - w) + l * (1 - l))};
    }
    /** Binary expected-score cross entropy through the SAME smooth WDL link.
     * Complement probabilities are evaluated directly to avoid cancellation in
     * confident material positions. Returns loss and derivative in pawn units. */
    public static double[] crossEntropy(double pawns, int material, double target) {
        double m = Math.max(17, Math.min(78, material)) / 58.0;
        double a = ((-13.50030198 * m + 40.92780883) * m - 36.82753545) * m + 386.83004070;
        double b = ((96.53354896 * m - 165.79058388) * m + 90.89679019) * m + 49.29561889;
        double k = a / b, z1 = k * (pawns - 1), z2 = k * (pawns + 1);
        double w = sigmoid(z1), s = sigmoid(z2), wc = sigmoid(-z1), sc = sigmoid(-z2);
        double denominator = (w + s) * (wc + sc);
        double ratio = denominator < 1e-200 ? k / 2 : k * (w * wc + s * sc) / denominator;
        double prediction = pawns >= 0 ? 1 - wc - sc : w + s - 1;
        double logP = logAdd(logSigmoid(z1), logSigmoid(z2)) - Math.log(2);
        double logQ = logAdd(logSigmoid(-z1), logSigmoid(-z2)) - Math.log(2);
        return new double[]{-.5 * ((1 + target) * logP + (1 - target) * logQ), (prediction - target) * ratio};
    }
    private static double sigmoid(double z) { double e = Math.exp(-Math.abs(z)); return z >= 0 ? 1 / (1 + e) : e / (1 + e); }
    private static double logSigmoid(double z) { return -Math.max(0, -z) - Math.log1p(Math.exp(-Math.abs(z))); }
    private static double logAdd(double a, double b) { double max = Math.max(a,b); return max + Math.log(Math.exp(a-max)+Math.exp(b-max)); }
    private Brn3Objective(){}
}
