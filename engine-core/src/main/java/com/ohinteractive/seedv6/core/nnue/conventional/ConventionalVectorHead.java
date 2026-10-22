package com.ohinteractive.seedv6.core.nnue.conventional;

import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.LongVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

/** Optional exact SIMD kernel. Loaded only when the Vector API module is resolved. */
final class ConventionalVectorHead implements ConventionalNetwork.Head {
    private static final VectorSpecies<Integer> INTS = IntVector.SPECIES_PREFERRED;
    private static final VectorSpecies<Long> LONGS = LongVector.SPECIES_PREFERRED;

    static boolean supported() {
        return INTS.vectorBitSize() == LONGS.vectorBitSize() && INTS.length() == 2 * LONGS.length()
                && ConventionalNetwork.HIDDEN % INTS.length() == 0;
    }

    public String name() { return "vector-" + INTS.vectorBitSize() + "-long"; }

    public long dot(int[] us, int[] them, int[] weights, int offset) {
        LongVector low = LongVector.zero(LONGS), high = LongVector.zero(LONGS);
        for (int i = 0; i < ConventionalNetwork.HIDDEN; i += INTS.length()) {
            var a = IntVector.fromArray(INTS, us, i).max(0).min(ConventionalNetwork.QA);
            var b = IntVector.fromArray(INTS, them, i).max(0).min(ConventionalNetwork.QA);
            // A single 255^2 * signed-short product fits int, including -32768.
            // Widen each product before adding; the complete dot is exact in long.
            var x = a.mul(a).mul(IntVector.fromArray(INTS, weights, offset + i));
            var y = b.mul(b).mul(IntVector.fromArray(INTS, weights, offset + ConventionalNetwork.HIDDEN + i));
            low = low.add((LongVector) x.convertShape(VectorOperators.I2L, LONGS, 0));
            high = high.add((LongVector) x.convertShape(VectorOperators.I2L, LONGS, 1));
            low = low.add((LongVector) y.convertShape(VectorOperators.I2L, LONGS, 0));
            high = high.add((LongVector) y.convertShape(VectorOperators.I2L, LONGS, 1));
        }
        return low.add(high).reduceLanes(VectorOperators.ADD);
    }
}
