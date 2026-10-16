package com.ohinteractive.seedv6.training.data;

/** BT4 publisher's integer Q encoding. No centipawn calibration is involved. */
public final class Bt4Targets {
    public static final int SKIP = 32002;
    private static final double[] POSITIVE = table();
    private static double[] table() {
        double[] values = new double[32001];
        for (int score = 1; score < values.length; score++) {
            double low = 0, high = 1;
            // Fixed work once per process; strict arithmetic and odd reflection thereafter.
            for (int step = 0; step < 56; step++) {
                double q = (low + high) * .5;
                double encoded = 660.6 * q / (1 - .9751875 * StrictMath.pow(q, 10));
                if (encoded < score) low = q; else high = q;
            }
            values[score] = (low + high) * .5;
        }
        return values;
    }
    public static double q(int score) {
        if (score < -32000 || score > 32000)
            throw new IllegalArgumentException(score == SKIP ? "BT4 skip sentinel" : "BT4 encoded score out of range");
        return score < 0 ? -POSITIVE[-score] : POSITIVE[score];
    }
    private Bt4Targets() {}
}
