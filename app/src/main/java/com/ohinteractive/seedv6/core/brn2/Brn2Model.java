package com.ohinteractive.seedv6.core.brn2;

import java.util.Objects;
import java.util.Random;
import com.ohinteractive.seedv6.core.brn.BrnFeatureSchema;

/** Immutable binary64 BRN-2: local endpoint composition, ReLU, sum, ReLU, value head. */
public final class Brn2Model {
    public static final int HIDDEN_WIDTH = 32;
    public static final int ENDPOINT_COUNT = 2;
    public static final int NODE_ROWS = BrnFeatureSchema.NODE_COUNT;
    public static final int RELATION_ROWS = BrnFeatureSchema.RELATION_COUNT;
    public static final int STATUS_ROWS = Long.SIZE;
    public static final int RELATION_A_ROW = NODE_ROWS;
    public static final int RELATION_B_ROW = RELATION_A_ROW + RELATION_ROWS;
    public static final int STATUS_ROW = RELATION_B_ROW + RELATION_ROWS;
    public static final int EMBEDDING_ROWS = STATUS_ROW + STATUS_ROWS;
    public static final int LOCAL_BIAS_OFFSET = EMBEDDING_ROWS * HIDDEN_WIDTH;
    public static final int BOARD_BIAS_OFFSET = LOCAL_BIAS_OFFSET + HIDDEN_WIDTH;
    public static final int OUTPUT_WEIGHT_OFFSET = BOARD_BIAS_OFFSET + HIDDEN_WIDTH;
    public static final int OUTPUT_BIAS = OUTPUT_WEIGHT_OFFSET + HIDDEN_WIDTH;
    public static final int PARAMETER_COUNT = OUTPUT_BIAS + 1;
    /** Fixed architecture seed, independent of self-play and shuffle streams. */
    public static final long INITIALIZATION_SEED = 0x533642524e320001L;
    private final double[] weights;

    public Brn2Model() {
        weights = new double[PARAMETER_COUNT];
        Random random = new Random(INITIALIZATION_SEED);
        for (int i = 0; i < LOCAL_BIAS_OFFSET; i++)
            weights[i] = (2 * random.nextDouble() - 1) * (i < NODE_ROWS * HIDDEN_WIDTH ? .01 : .005);
        double bound = StrictMath.sqrt(6.0 / (HIDDEN_WIDTH + 1));
        for (int h = 0; h < HIDDEN_WIDTH; h++) weights[OUTPUT_WEIGHT_OFFSET + h] = (2 * random.nextDouble() - 1) * bound;
    }

    public Brn2Model(double[] weights) { validate(weights, false); this.weights = weights.clone(); }

    public static int nodeIndex(int featureId, int channel) {
        return index(Objects.checkIndex(featureId - BrnFeatureSchema.NODE_OFFSET, NODE_ROWS), channel);
    }
    public static int relationAIndex(int featureId, int channel) {
        return index(RELATION_A_ROW + Objects.checkIndex(featureId - BrnFeatureSchema.RELATION_OFFSET, RELATION_ROWS), channel);
    }
    public static int relationBIndex(int featureId, int channel) {
        return index(RELATION_B_ROW + Objects.checkIndex(featureId - BrnFeatureSchema.RELATION_OFFSET, RELATION_ROWS), channel);
    }
    public static int statusIndex(int bit, int channel) {
        return index(STATUS_ROW + Objects.checkIndex(bit, STATUS_ROWS), channel);
    }
    private static int index(int row, int channel) { return row * HIDDEN_WIDTH + Objects.checkIndex(channel, HIDDEN_WIDTH); }

    /** Side-to-move normalized value. The caller owns and reuses scratch. */
    public double evaluate(long[] board, Brn2Workspace scratch) { return scratch.evaluate(board, weights); }
    public double weight(int index) { return weights[index]; }
    public double[] copyWeights() { return weights.clone(); }

    static void validate(double[] values, boolean nonnegative) {
        Objects.requireNonNull(values, "values");
        if (values.length != PARAMETER_COUNT) throw new IllegalArgumentException("Wrong BRN-2 parameter count.");
        for (double value : values) if (!Double.isFinite(value) || (nonnegative && value < 0))
            throw new IllegalArgumentException("Invalid BRN-2 parameter/moment.");
    }
}
