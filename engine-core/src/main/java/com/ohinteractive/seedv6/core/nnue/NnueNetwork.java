package com.ohinteractive.seedv6.core.nnue;

import java.util.Objects;
import java.util.SplittableRandom;

/**
 * Immutable V1 float parameters, safely shareable between worker-owned evaluators.
 * Transformer layout: weights[feature * 64 + unit]. Hidden layout:
 * weights[hiddenUnit * 128 + input]. Output weights are indexed by hidden unit.
 * Every sum uses ascending input indices and ordinary Java float multiply/add (no FMA).
 * Inference interleaves independent hidden sums; the scalar/training oracle is retained.
 */
public final class NnueNetwork {

    public static final int ACCUMULATOR_SIZE = 64;
    public static final int CONCATENATED_SIZE = 2 * ACCUMULATOR_SIZE;
    public static final int HIDDEN_SIZE = 32;
    public static final int OUTPUT_SIZE = 1;
    public static final int FEATURE_WEIGHT_COUNT = NnueFeatureSchema.FEATURE_COUNT * ACCUMULATOR_SIZE;
    public static final int HIDDEN_WEIGHT_COUNT = HIDDEN_SIZE * CONCATENATED_SIZE;
    public static final int PARAMETER_COUNT = ACCUMULATOR_SIZE + FEATURE_WEIGHT_COUNT
            + HIDDEN_SIZE + HIDDEN_WEIGHT_COUNT + OUTPUT_SIZE + HIDDEN_SIZE;

    private final float[] featureBias;
    private final float[] featureWeights;
    private final float[] hiddenBias;
    private final float[] hiddenWeights;
    private final float outputBias;
    private final float[] outputWeights;

    // Only factories can transfer exclusively owned arrays to this constructor.
    private NnueNetwork(float[] featureBias, float[] featureWeights, float[] hiddenBias,
                        float[] hiddenWeights, float outputBias, float[] outputWeights) {
        requireFinite(featureBias);
        requireFinite(featureWeights);
        requireFinite(hiddenBias);
        requireFinite(hiddenWeights);
        requireFinite(outputWeights);
        if (!Float.isFinite(outputBias)) {
            throw new IllegalArgumentException("Output bias must be finite.");
        }
        this.featureBias = featureBias;
        this.featureWeights = featureWeights;
        this.hiddenBias = hiddenBias;
        this.hiddenWeights = hiddenWeights;
        this.outputBias = outputBias;
        this.outputWeights = outputWeights;
    }

    /**
     * Copies caller-owned parameters once. Subsequent caller mutation cannot change this network.
     * Rejects unsupported schemas, incorrect dimensions and non-finite parameters.
     */
    public static NnueNetwork of(int schemaVersion, float[] featureBias, float[] featureWeights,
                                 float[] hiddenBias, float[] hiddenWeights,
                                 float outputBias, float[] outputWeights) {
        if (schemaVersion != NnueFeatureSchema.VERSION) {
            throw new IllegalArgumentException("Unsupported NNUE schema version: " + schemaVersion);
        }
        requireLength(featureBias, ACCUMULATOR_SIZE);
        requireLength(featureWeights, FEATURE_WEIGHT_COUNT);
        requireLength(hiddenBias, HIDDEN_SIZE);
        requireLength(hiddenWeights, HIDDEN_WEIGHT_COUNT);
        requireLength(outputWeights, HIDDEN_SIZE);
        return new NnueNetwork(featureBias.clone(), featureWeights.clone(), hiddenBias.clone(),
                hiddenWeights.clone(), outputBias, outputWeights.clone());
    }

    /**
     * Deterministic small symmetry-breaking initialization: one SplittableRandom(seed),
     * consumed in transformer, hidden, then output array order. Each weight is
     * (float) nextDouble(-1/64, +1/64); rounding can include either endpoint.
     * Biases are zero. Even 32 maximally positive transformer rows sum to at most 0.5.
     * Newly allocated arrays transfer directly to the network, without a matrix copy.
     * This is an untrained initialization, not a calibrated chess evaluation.
     */
    public static NnueNetwork initialized(long seed) {
        SplittableRandom random = new SplittableRandom(seed);
        float[] featureWeights = randomWeights(random, FEATURE_WEIGHT_COUNT);
        float[] hiddenWeights = randomWeights(random, HIDDEN_WEIGHT_COUNT);
        float[] outputWeights = randomWeights(random, HIDDEN_SIZE);
        return new NnueNetwork(new float[ACCUMULATOR_SIZE], featureWeights,
                new float[HIDDEN_SIZE], hiddenWeights, 0.0f, outputWeights);
    }

    public int schemaVersion() { return NnueFeatureSchema.VERSION; }

    // Scalar inspection deliberately never exposes a mutable parameter array.
    public float featureBias(int unit) { return featureBias[unit]; }

    public float featureWeight(int feature, int unit) {
        Objects.checkIndex(feature, NnueFeatureSchema.FEATURE_COUNT);
        Objects.checkIndex(unit, ACCUMULATOR_SIZE);
        return featureWeights[feature * ACCUMULATOR_SIZE + unit];
    }

    public float hiddenBias(int unit) { return hiddenBias[unit]; }

    public float hiddenWeight(int unit, int input) {
        Objects.checkIndex(unit, HIDDEN_SIZE);
        Objects.checkIndex(input, CONCATENATED_SIZE);
        return hiddenWeights[unit * CONCATENATED_SIZE + input];
    }

    public float outputBias() { return outputBias; }
    public float outputWeight(int unit) { return outputWeights[unit]; }

    /** V1 activation for both transformer and hidden units; never applied to the output. */
    public static float clip01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    void resetAccumulator(float[] accumulator) {
        System.arraycopy(featureBias, 0, accumulator, 0, ACCUMULATOR_SIZE);
    }

    void addFeature(int feature, float[] accumulator) {
        NnueMath.addFeature(featureWeights, feature, accumulator);
    }

    void subtractFeature(int feature, float[] accumulator) {
        int offset = feature * ACCUMULATOR_SIZE;
        for (int unit = 0; unit < ACCUMULATOR_SIZE; unit++) {
            accumulator[unit] -= featureWeights[offset + unit];
        }
    }

    float forwardScalar(float[] input, float[] hidden) {
        return NnueMath.forward(hiddenBias, hiddenWeights, outputBias, outputWeights,
                input, null, hidden);
    }

    float forward(float[] input, float[] hidden) {
        // Four independent sums hide the scalar float dependency latency on HotSpot.
        // EACH sum still visits inputs 0..127 in exactly the oracle's order, without
        // reassociation or FMA. Keep NnueMath.forward as the scalar/training oracle.
        for (int unit = 0; unit < HIDDEN_SIZE; unit += 4) {
            int offset = unit * CONCATENATED_SIZE;
            float s0 = hiddenBias[unit];
            float s1 = hiddenBias[unit + 1];
            float s2 = hiddenBias[unit + 2];
            float s3 = hiddenBias[unit + 3];
            for (int i = 0; i < CONCATENATED_SIZE; i++) {
                float value = input[i];
                s0 += hiddenWeights[offset + i] * value;
                s1 += hiddenWeights[offset + CONCATENATED_SIZE + i] * value;
                s2 += hiddenWeights[offset + 2 * CONCATENATED_SIZE + i] * value;
                s3 += hiddenWeights[offset + 3 * CONCATENATED_SIZE + i] * value;
            }
            hidden[unit] = clip01(s0);
            hidden[unit + 1] = clip01(s1);
            hidden[unit + 2] = clip01(s2);
            hidden[unit + 3] = clip01(s3);
        }
        float raw = outputBias;
        for (int unit = 0; unit < HIDDEN_SIZE; unit++) {
            raw += outputWeights[unit] * hidden[unit];
        }
        return raw;
    }

    private static float[] randomWeights(SplittableRandom random, int count) {
        float[] weights = new float[count];
        for (int i = 0; i < count; i++) {
            weights[i] = (float) random.nextDouble(-1.0 / 64.0, 1.0 / 64.0);
        }
        return weights;
    }

    private static void requireLength(float[] values, int length) {
        Objects.requireNonNull(values, "parameters");
        if (values.length != length) {
            throw new IllegalArgumentException("Expected " + length + " parameters, got " + values.length);
        }
    }

    private static void requireFinite(float[] values) {
        for (float value : values) {
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("NNUE parameters must be finite.");
            }
        }
    }
}
