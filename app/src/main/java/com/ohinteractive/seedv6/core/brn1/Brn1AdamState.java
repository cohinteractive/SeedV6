package com.ohinteractive.seedv6.core.brn1;

/** Trainer-owned moments and successful global step, in Brn1Model parameter order. */
public final class Brn1AdamState {
    final double[] first, second;
    long step;

    Brn1AdamState() { this(0, new double[Brn1Model.PARAMETER_COUNT], new double[Brn1Model.PARAMETER_COUNT]); }

    // Ownership transfers from codec only; public APIs never expose mutable arrays.
    Brn1AdamState(long step, double[] first, double[] second) {
        if (step < 0) throw new IllegalArgumentException("Negative Adam step.");
        Brn1Model.validate(first, false); Brn1Model.validate(second, true);
        if (step == 0) {
            for (int i = 0; i < first.length; i++) {
                if (first[i] != 0 || second[i] != 0) throw new IllegalArgumentException("Nonzero moments at step zero.");
            }
        }
        this.step = step; this.first = first; this.second = second;
    }

    public long step() { return step; }
    public double firstMoment(int index) { return first[index]; }
    public double secondMoment(int index) { return second[index]; }
}
