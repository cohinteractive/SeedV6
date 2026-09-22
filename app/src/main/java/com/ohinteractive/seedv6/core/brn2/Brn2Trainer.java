package com.ohinteractive.seedv6.core.brn2;

import java.util.Arrays;
import java.util.Objects;
import com.ohinteractive.seedv6.core.brn.BrnAdamConfig;
import com.ohinteractive.seedv6.core.brn.BrnFeatureSchema;
import static com.ohinteractive.seedv6.core.brn2.Brn2Model.*;

/** Online Adam; each touched sparse row receives one aggregated vector update per sample. */
public final class Brn2Trainer {
    final double[] weights;
    private final BrnAdamConfig config;
    private final Brn2AdamState optimizer;
    private final Brn2Workspace scratch = new Brn2Workspace();
    private static final int MAX_TOUCHED = 64 + 2 * (64 * 63 / 2) + 64;
    // slot+1, zero means absent. Only previous touched slots are cleared; no per-sample dense sweep.
    private final int[] slots = new int[EMBEDDING_ROWS], touched = new int[MAX_TOUCHED];
    private final double[] gradients = new double[MAX_TOUCHED * HIDDEN_WIDTH];
    private final double[] localGradient = new double[64 * HIDDEN_WIDTH];
    private final double[] contextGradient = new double[HIDDEN_WIDTH];
    private final double[] boardGradient = new double[HIDDEN_WIDTH], outputGradient = new double[HIDDEN_WIDTH];
    private int touchedCount;

    public Brn2Trainer(double learningRate) { this(new Brn2Model(), new BrnAdamConfig(learningRate)); }
    public Brn2Trainer(Brn2Model initial, BrnAdamConfig config) { this(initial.copyWeights(), config, new Brn2AdamState()); }
    Brn2Trainer(double[] weights, BrnAdamConfig config, Brn2AdamState optimizer) {
        Brn2Model.validate(weights, false);
        this.weights = weights; this.config = Objects.requireNonNull(config); this.optimizer = Objects.requireNonNull(optimizer);
    }
    public BrnAdamConfig config() { return config; }
    public Brn2AdamState optimizer() { return optimizer; }
    public Brn2Model snapshot() { return new Brn2Model(weights); }
    public double predict(long[] board) { return scratch.evaluate(board, weights); }

    /** Pre-update half squared error; ReLU derivative at zero is zero. */
    public double train(long[] board, double target) {
        if (!Double.isFinite(target) || target < -1 || target > 1)
            throw new IllegalArgumentException("Target must be finite and in [-1,+1].");
        if (optimizer.step == Long.MAX_VALUE) throw new ArithmeticException("Adam step exhausted.");
        double prediction = predict(board), difference = prediction - target;
        double derivative = difference * (1 - prediction * prediction);
        // All gradients use pre-update weights and both retained ReLU masks.
        for (int h = 0; h < HIDDEN_WIDTH; h++) {
            outputGradient[h] = derivative * Math.max(0, scratch.boardPre[h]);
            boardGradient[h] = scratch.boardPre[h] > 0 ? derivative * weights[OUTPUT_WEIGHT_OFFSET + h] : 0;
        }
        Arrays.fill(contextGradient, 0);
        int nodes = scratch.features.nodeCount();
        for (int i = 0; i < nodes; i++) for (int h = 0; h < HIDDEN_WIDTH; h++) {
            double gradient = scratch.localPre[i * HIDDEN_WIDTH + h] > 0 ? boardGradient[h] : 0;
            localGradient[i * HIDDEN_WIDTH + h] = gradient;
            contextGradient[h] += gradient;
        }
        for (int n = 0; n < touchedCount; n++) slots[touched[n]] = 0;
        touchedCount = 0;
        for (int i = 0; i < nodes; i++)
            add(scratch.features.indexAt(1 + i) - BrnFeatureSchema.NODE_OFFSET, localGradient, i * HIDDEN_WIDTH);
        int pair = 1 + nodes;
        for (int a = 0; a < nodes; a++) for (int b = a + 1; b < nodes; b++) {
            int relation = scratch.features.indexAt(pair++) - BrnFeatureSchema.RELATION_OFFSET;
            add(RELATION_A_ROW + relation, localGradient, a * HIDDEN_WIDTH);
            add(RELATION_B_ROW + relation, localGradient, b * HIDDEN_WIDTH);
        }
        for (int n = pair; n < scratch.features.size(); n++)
            add(STATUS_ROW + scratch.features.indexAt(n) - BrnFeatureSchema.STATUS_OFFSET, contextGradient, 0);
        long next = optimizer.step + 1;
        double c1 = 1 - StrictMath.pow(config.beta1(), next), c2 = 1 - StrictMath.pow(config.beta2(), next);
        // Validate the complete sparse+dense candidate before changing any persistent state.
        update(derivative, c1, c2, false);
        update(derivative, c1, c2, true);
        optimizer.step = next;
        return .5 * difference * difference;
    }

    private void add(int row, double[] vector, int offset) {
        int slot = slots[row] - 1;
        if (slot < 0) {
            slot = touchedCount++; touched[slot] = row; slots[row] = slot + 1;
            System.arraycopy(vector, offset, gradients, slot * HIDDEN_WIDTH, HIDDEN_WIDTH);
        } else for (int h = 0; h < HIDDEN_WIDTH; h++) gradients[slot * HIDDEN_WIDTH + h] += vector[offset + h];
    }

    private void update(double derivative, double c1, double c2, boolean publish) {
        adam(OUTPUT_BIAS, derivative, c1, c2, publish);
        for (int h = 0; h < HIDDEN_WIDTH; h++) {
            adam(OUTPUT_WEIGHT_OFFSET + h, outputGradient[h], c1, c2, publish);
            adam(BOARD_BIAS_OFFSET + h, boardGradient[h], c1, c2, publish);
            adam(LOCAL_BIAS_OFFSET + h, contextGradient[h], c1, c2, publish);
        }
        for (int n = 0; n < touchedCount; n++) for (int h = 0; h < HIDDEN_WIDTH; h++)
            adam(touched[n] * HIDDEN_WIDTH + h, gradients[n * HIDDEN_WIDTH + h], c1, c2, publish);
    }

    private void adam(int i, double gradient, double c1, double c2, boolean publish) {
        double m = config.beta1() * optimizer.first[i] + (1 - config.beta1()) * gradient;
        double v = config.beta2() * optimizer.second[i] + (1 - config.beta2()) * gradient * gradient;
        double weight = weights[i] - config.learningRate() * (m / c1) / (StrictMath.sqrt(v / c2) + config.epsilon());
        if (!Double.isFinite(m) || !Double.isFinite(v) || !Double.isFinite(weight))
            throw new ArithmeticException("Nonfinite BRN-2 Adam candidate; update not published.");
        if (publish) { weights[i] = weight; optimizer.first[i] = m; optimizer.second[i] = v; }
    }
}
