package com.ohinteractive.seedv6.core.brn1;

import java.util.Objects;
import com.ohinteractive.seedv6.core.brn.BrnAdamConfig;
import com.ohinteractive.seedv6.core.brn.BrnFeatureSchema;
import static com.ohinteractive.seedv6.core.brn1.Brn1Model.*;

/** Single-owner online Adam. Dense heads update every step; absent embedding rows freeze. */
public final class Brn1Trainer {
    final double[] weights;
    private final BrnAdamConfig config;
    private final Brn1AdamState optimizer;
    private final Brn1Workspace scratch = new Brn1Workspace();
    private final int[] multiplicities = new int[FEATURE_COUNT];
    private final int[] touched = new int[BrnFeatureSchema.MAX_ACTIVE_FEATURES];
    private final double[] hiddenGradient = new double[HIDDEN_WIDTH];
    private final double[] outputGradient = new double[HIDDEN_WIDTH];
    private int touchedCount;

    public Brn1Trainer(double learningRate) { this(new Brn1Model(), new BrnAdamConfig(learningRate)); }
    public Brn1Trainer(Brn1Model initial, BrnAdamConfig config) { this(initial.copyWeights(), config, new Brn1AdamState()); }
    Brn1Trainer(double[] weights, BrnAdamConfig config, Brn1AdamState optimizer) {
        Brn1Model.validate(weights, false);
        this.weights = weights; this.config = Objects.requireNonNull(config); this.optimizer = Objects.requireNonNull(optimizer);
    }

    public BrnAdamConfig config() { return config; }
    public Brn1AdamState optimizer() { return optimizer; }
    public Brn1Model snapshot() { return new Brn1Model(weights); }
    public double predict(long[] board) { return scratch.evaluate(board, weights); }

    /** Pre-update half squared error. ReLU derivative at zero is zero. No successful-step allocation. */
    public double train(long[] board, double target) {
        if (!Double.isFinite(target) || target < -1 || target > 1)
            throw new IllegalArgumentException("Target must be finite and in [-1,+1].");
        if (optimizer.step == Long.MAX_VALUE) throw new ArithmeticException("Adam step exhausted.");
        double prediction = predict(board), difference = prediction - target;
        double derivative = difference * (1 - prediction * prediction);
        // Cache all derivatives against PRE-update weights before publishing any parameter.
        for (int c = 0; c < HIDDEN_WIDTH; c++) {
            double h = scratch.hidden[c];
            outputGradient[c] = derivative * Math.max(0, h);
            hiddenGradient[c] = h > 0 ? derivative * weights[OUTPUT_WEIGHT_OFFSET + c] : 0;
        }
        for (int n = 0; n < touchedCount; n++) multiplicities[touched[n]] = 0;
        touchedCount = 0;
        for (int n = 1; n < scratch.features.size(); n++) {
            int row = scratch.features.indexAt(n) - 1;
            if (multiplicities[row]++ == 0) touched[touchedCount++] = row;
        }
        long next = optimizer.step + 1;
        double c1 = 1 - StrictMath.pow(config.beta1(), next), c2 = 1 - StrictMath.pow(config.beta2(), next);
        // Validate the entire sparse+dense candidate before mutating any persistent state.
        update(derivative, c1, c2, false);
        update(derivative, c1, c2, true);
        optimizer.step = next;
        return .5 * difference * difference;
    }

    private void update(double derivative, double c1, double c2, boolean publish) {
        adam(OUTPUT_BIAS, derivative, c1, c2, publish);
        for (int c = 0; c < HIDDEN_WIDTH; c++) {
            adam(OUTPUT_WEIGHT_OFFSET + c, outputGradient[c], c1, c2, publish);
            adam(HIDDEN_BIAS_OFFSET + c, hiddenGradient[c], c1, c2, publish);
        }
        for (int n = 0; n < touchedCount; n++) {
            int row = touched[n], count = multiplicities[row], offset = row * HIDDEN_WIDTH;
            for (int c = 0; c < HIDDEN_WIDTH; c++) adam(offset + c, count * hiddenGradient[c], c1, c2, publish);
        }
    }

    private void adam(int i, double gradient, double c1, double c2, boolean publish) {
        double m = config.beta1() * optimizer.first[i] + (1 - config.beta1()) * gradient;
        double v = config.beta2() * optimizer.second[i] + (1 - config.beta2()) * gradient * gradient;
        double weight = weights[i] - config.learningRate() * (m / c1) / (StrictMath.sqrt(v / c2) + config.epsilon());
        if (!Double.isFinite(m) || !Double.isFinite(v) || !Double.isFinite(weight))
            throw new ArithmeticException("Nonfinite BRN-1 Adam candidate; update not published.");
        if (publish) { weights[i] = weight; optimizer.first[i] = m; optimizer.second[i] = v; }
    }
}
