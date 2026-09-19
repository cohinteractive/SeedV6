package com.ohinteractive.seedv6.training.nnue;

import com.ohinteractive.seedv6.core.nnue.NnueFeatureSchema;

import java.util.Objects;

/**
 * Trainer-owned conventional Adam, with float moments and double update arithmetic.
 * m = float(beta1*m + (1-beta1)*g), s = float(beta2*s + (1-beta2)*g*g),
 * theta = float(theta - learningRate*(m/(1-beta1^t))/(sqrt(s/(1-beta2^t))+epsilon)).
 * StrictMath derives corrections from the persisted step; there is no hidden power cache.
 */
public final class AdamOptimizer {
    final Parameters firstMoment, secondMoment;
    // Derived cache, not additional optimizer mathematics: recoverable exactly from moment bits.
    private final boolean[] active = new boolean[NnueFeatureSchema.FEATURE_COUNT];
    private final AdamHyperparameters hyperparameters;
    private long step;

    AdamOptimizer(AdamHyperparameters hyperparameters) {
        this(hyperparameters, 0, new Parameters(), new Parameters());
    }

    AdamOptimizer(AdamHyperparameters hyperparameters, long step, Parameters first, Parameters second) {
        this.hyperparameters = Objects.requireNonNull(hyperparameters, "hyperparameters");
        if (step < 0) throw new IllegalArgumentException("Negative optimizer step.");
        first.validate(false);
        second.validate(true);
        if (step == 0) {
            for (int group = 0; group < first.groups.length; group++) {
                for (int i = 0; i < first.groups[group].length; i++) {
                    if (first.groups[group][i] != 0 || second.groups[group][i] != 0) {
                        throw new IllegalArgumentException("Nonzero moments at step zero.");
                    }
                }
            }
        }
        this.step = step;
        firstMoment = first;
        secondMoment = second;
        for (int row = 0; row < active.length; row++) active[row] = hasMoment(row);
    }

    public AdamHyperparameters hyperparameters() { return hyperparameters; }
    /** Number of successfully published minibatch updates; a failed update never increments it. */
    public long step() { return step; }

    void update(Parameters parameters, BatchGradients gradients) {
        if (step == Long.MAX_VALUE) throw new ArithmeticException("Adam step exhausted.");
        long next = step + 1;
        double correction1 = 1 - StrictMath.pow(hyperparameters.beta1(), next);
        double correction2 = 1 - StrictMath.pow(hyperparameters.beta2(), next);
        // Validate every candidate before changing ANY parameter, moment, active flag or step.
        pass(parameters, gradients, correction1, correction2, false);
        pass(parameters, gradients, correction1, correction2, true);
        step = next;
    }

    private void pass(Parameters p, BatchGradients g, double c1, double c2, boolean publish) {
        for (int group = 0; group < p.groups.length; group++) {
            if (group == Parameters.FEATURES) {
                // An inactive row with zero moments has exactly zero Adam update. Historical
                // moment rows STILL decay/update on every batch, even when their gradient is zero.
                for (int row = 0; row < active.length; row++) {
                    if (active[row] || g.touched[row]) {
                        updateRange(p, g, group, row * 64, row * 64 + 64, c1, c2, publish);
                        if (publish) active[row] = hasMoment(row);
                    }
                }
            } else updateRange(p, g, group, 0, p.groups[group].length, c1, c2, publish);
        }
    }

    private void updateRange(Parameters p, BatchGradients gradients, int group, int start, int end,
                             double c1, double c2, boolean publish) {
        float[] weights = p.groups[group], g = gradients.values.groups[group];
        float[] m = firstMoment.groups[group], s = secondMoment.groups[group];
        double b1 = hyperparameters.beta1(), b2 = hyperparameters.beta2();
        for (int i = start; i < end; i++) {
            double gradient = g[i];
            float nextM = (float) (b1 * m[i] + (1 - b1) * gradient);
            float nextS = (float) (b2 * s[i] + (1 - b2) * gradient * gradient);
            float nextWeight = (float) (weights[i] - hyperparameters.learningRate()
                    * (nextM / c1) / (StrictMath.sqrt(nextS / c2) + hyperparameters.epsilon()));
            if (!Double.isFinite(gradient) || !Float.isFinite(nextM)
                    || !Float.isFinite(nextS) || nextS < 0 || !Float.isFinite(nextWeight)) {
                throw new ArithmeticException("Nonfinite Adam candidate; update not published.");
            }
            if (publish) {
                weights[i] = nextWeight;
                m[i] = nextM;
                s[i] = nextS;
            }
        }
    }

    private boolean hasMoment(int row) {
        int end = (row + 1) * 64;
        for (int i = row * 64; i < end; i++) {
            if (Float.floatToRawIntBits(firstMoment.groups[1][i]) != 0
                    || Float.floatToRawIntBits(secondMoment.groups[1][i]) != 0) return true;
        }
        return false;
    }
}
