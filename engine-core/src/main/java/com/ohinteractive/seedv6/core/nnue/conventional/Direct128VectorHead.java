package com.ohinteractive.seedv6.core.nnue.conventional;

import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.LongVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorShuffle;
import jdk.incubator.vector.VectorSpecies;

/** Direct path of the validated Phase 2 CompactVectorHead, without experimental layers.
 * S6CNU003 bounds output weights to +/-127. At >=4 lanes, each lane accumulates
 * at most 64 * 255^2 * 127 = 528,523,200 in magnitude; widen before reduction. */
final class Direct128VectorHead implements ConventionalNetwork.Head {
    private static final VectorSpecies<Integer> INTS = IntVector.SPECIES_PREFERRED;
    private static final VectorSpecies<Long> LONGS = LongVector.SPECIES_PREFERRED;
    // JDK21 part=1 conversion allocates an iota shuffle; cache the half rotation.
    private static final VectorShuffle<Integer> HIGH_TO_LOW =
            VectorShuffle.fromOp(INTS, i -> (i + LONGS.length()) % INTS.length());

    static boolean supported() {
        return INTS.length() >= 4 && INTS.length() == 2 * LONGS.length() && 128 % INTS.length() == 0;
    }

    public String name() { return "vector-bounded-int"; }

    public long dot(int[] us, int[] them, int[] weights, int offset) {
        var sum = IntVector.zero(INTS);
        for (int i = 0; i < 128; i += INTS.length()) {
            var a = IntVector.fromArray(INTS, us, i).max(0).min(255);
            var b = IntVector.fromArray(INTS, them, i).max(0).min(255);
            sum = sum.add(a.mul(a).mul(IntVector.fromArray(INTS, weights, offset + i)));
            sum = sum.add(b.mul(b).mul(IntVector.fromArray(INTS, weights, offset + 128 + i)));
        }
        var low = (LongVector) sum.convertShape(VectorOperators.I2L, LONGS, 0);
        var high = (LongVector) sum.rearrange(HIGH_TO_LOW).convertShape(VectorOperators.I2L, LONGS, 0);
        return low.add(high).reduceLanes(VectorOperators.ADD);
    }
}
