package com.ohinteractive.seedv6.core.nnue;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class NnueNetworkTest {

    @Test
    void seededInitializationIsBitIdenticalFiniteSmallAndNonzero() {
        NnueNetwork first = NnueNetwork.initialized(20260918L);
        NnueNetwork same = NnueNetwork.initialized(20260918L);
        NnueNetwork different = NnueNetwork.initialized(20260919L);
        assertEquals(1, first.schemaVersion());
        boolean featureChanged = false;
        boolean hiddenChanged = false;
        boolean outputChanged = false;
        boolean nonzero = false;
        for (int feature = 0; feature < 49_152; feature++) {
            for (int unit = 0; unit < 64; unit++) {
                float value = first.featureWeight(feature, unit);
                assertInitializedWeight(value, same.featureWeight(feature, unit), different.featureWeight(feature, unit));
                featureChanged |= value != different.featureWeight(feature, unit);
                nonzero |= value != 0;
            }
        }
        for (int unit = 0; unit < 64; unit++) {
            assertZeroBias(first.featureBias(unit), same.featureBias(unit), different.featureBias(unit));
        }
        for (int unit = 0; unit < 32; unit++) {
            assertZeroBias(first.hiddenBias(unit), same.hiddenBias(unit), different.hiddenBias(unit));
            for (int input = 0; input < 128; input++) {
                float value = first.hiddenWeight(unit, input);
                assertInitializedWeight(value, same.hiddenWeight(unit, input), different.hiddenWeight(unit, input));
                hiddenChanged |= value != different.hiddenWeight(unit, input);
            }
            float value = first.outputWeight(unit);
            assertInitializedWeight(value, same.outputWeight(unit), different.outputWeight(unit));
            outputChanged |= value != different.outputWeight(unit);
        }
        assertZeroBias(first.outputBias(), same.outputBias(), different.outputBias());
        assertTrue(featureChanged);
        assertTrue(hiddenChanged);
        assertTrue(outputChanged);
        assertTrue(nonzero);
    }

    @Test
    void factoryCopiesEveryCallerOwnedArrayAndUsesDocumentedLayouts() {
        float[] featureBias = new float[64];
        float[] featureWeights = new float[49_152 * 64];
        float[] hiddenBias = new float[32];
        float[] hiddenWeights = new float[32 * 128];
        float[] outputWeights = new float[32];
        featureBias[63] = 0.125f;
        featureWeights[49_151 * 64 + 63] = 0.25f;
        hiddenBias[31] = 0.375f;
        hiddenWeights[31 * 128 + 127] = 0.5f;
        outputWeights[31] = 0.625f;
        NnueNetwork network = NnueNetwork.of(1, featureBias, featureWeights,
                hiddenBias, hiddenWeights, 0.75f, outputWeights);
        for (float[] parameters : new float[][] {
                featureBias, featureWeights, hiddenBias, hiddenWeights, outputWeights}) {
            Arrays.fill(parameters, Float.NaN);
        }
        assertEquals(0.125f, network.featureBias(63));
        assertEquals(0.25f, network.featureWeight(49_151, 63));
        assertEquals(0.375f, network.hiddenBias(31));
        assertEquals(0.5f, network.hiddenWeight(31, 127));
        assertEquals(0.625f, network.outputWeight(31));
        assertEquals(0.75f, network.outputBias());
        assertEquals(0.0f, network.featureWeight(0, 0));
        assertEquals(0.0f, network.hiddenWeight(0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> network.featureWeight(0, 64));
        assertThrows(IndexOutOfBoundsException.class, () -> network.featureWeight(49_152, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> network.hiddenWeight(0, 128));
    }

    @Test
    void factoryRejectsUnsupportedSchemaWrongDimensionsAndNonFiniteParameters() {
        float[][] arrays = {new float[64], new float[49_152 * 64],
                new float[32], new float[32 * 128], new float[32]};
        assertThrows(IllegalArgumentException.class, () -> create(2, arrays, 0));
        for (int i = 0; i < arrays.length; i++) {
            float[] original = arrays[i];
            arrays[i] = new float[original.length - 1];
            assertThrows(IllegalArgumentException.class, () -> create(1, arrays, 0));
            arrays[i] = original;
        }
        for (float[] array : arrays) {
            array[array.length - 1] = Float.NaN;
            assertThrows(IllegalArgumentException.class, () -> create(1, arrays, 0));
            array[array.length - 1] = 0;
        }
        assertThrows(IllegalArgumentException.class, () -> create(1, arrays, Float.POSITIVE_INFINITY));
        arrays[1][0] = Float.NEGATIVE_INFINITY;
        assertThrows(IllegalArgumentException.class, () -> create(1, arrays, 0));
    }

    @Test
    void clip01HasTheSpecifiedEndpointsAndInterior() {
        assertEquals(0, NnueNetwork.clip01(-2.5f));
        assertEquals(0, NnueNetwork.clip01(0));
        assertEquals(0.375f, NnueNetwork.clip01(0.375f));
        assertEquals(1, NnueNetwork.clip01(1));
        assertEquals(1, NnueNetwork.clip01(2.5f));
    }

    private static NnueNetwork create(int version, float[][] arrays, float outputBias) {
        return NnueNetwork.of(version, arrays[0], arrays[1], arrays[2], arrays[3], outputBias, arrays[4]);
    }

    private static void assertInitializedWeight(float first, float same, float different) {
        assertEquals(Float.floatToRawIntBits(first), Float.floatToRawIntBits(same));
        assertTrue(Float.isFinite(first));
        assertTrue(Float.isFinite(different));
        assertTrue(Math.abs(first) <= 1.0f / 64);
        assertTrue(Math.abs(different) <= 1.0f / 64);
    }

    private static void assertZeroBias(float first, float same, float different) {
        assertEquals(0, Float.floatToRawIntBits(first));
        assertEquals(0, Float.floatToRawIntBits(same));
        assertEquals(0, Float.floatToRawIntBits(different));
    }
}
