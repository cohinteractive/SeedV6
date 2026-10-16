package com.ohinteractive.seedv6.core.brn2;

/** Trainer-owned moments and successful global step, in Brn2Model parameter order. */
public final class Brn2AdamState {
    final double[] first, second;
    long step;

    Brn2AdamState() { this(0, new double[Brn2Model.PARAMETER_COUNT], new double[Brn2Model.PARAMETER_COUNT]); }

    // Ownership transfers from codec only; public APIs never expose mutable arrays.
    Brn2AdamState(long step, double[] first, double[] second) {
        if (step < 0) throw new IllegalArgumentException("Negative Adam step.");
        Brn2Model.validate(first, false); Brn2Model.validate(second, true);
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
