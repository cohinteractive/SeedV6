package com.ohinteractive.seedv6.core.brn;

/** Trainer-owned persistent moments and global successful-example count; no scratch state. */
public final class BrnAdamState {
    final double[] first, second;
    long step;

    BrnAdamState() { this(0, new double[BrnFeatureSchema.PARAMETER_COUNT], new double[BrnFeatureSchema.PARAMETER_COUNT]); }

    // Takes ownership of codec-created arrays; never accepts public caller-owned arrays.
    BrnAdamState(long step, double[] first, double[] second) {
        if (step < 0) throw new IllegalArgumentException("Negative Adam step.");
        BrnModel.validate(first, false);
        BrnModel.validate(second, true);
        if (step == 0) {
            for (int i = 0; i < first.length; i++) {
                if (first[i] != 0 || second[i] != 0) throw new IllegalArgumentException("Nonzero moments at step zero.");
            }
        }
        this.step = step;
        this.first = first;
        this.second = second;
    }

    public long step() { return step; }
    public double firstMoment(int index) { return first[index]; }
    public double secondMoment(int index) { return second[index]; }
}
