package com.ohinteractive.seedv6.core.nnue.conventional;

import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.LongVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;
import jdk.incubator.vector.VectorShuffle;

/** Loader bounds every downstream weight to +/-127. With512 inputs and>=4
 * lanes each lane sum fits int; widen before the horizontal reduction. */
final class Phase2VectorHead implements Phase2Network.Head {
    private static final VectorSpecies<Integer> INTS=IntVector.SPECIES_PREFERRED;
    private static final VectorSpecies<Long> LONGS=LongVector.SPECIES_PREFERRED;
    // JDK21 convertShape(...,part=1) creates an iota shuffle at this call site.
    // Cache the half rotation and use part0 conversions to keep the hot path allocation-free.
    private static final VectorShuffle<Integer> HIGH_TO_LOW=VectorShuffle.fromOp(INTS,i->(i+LONGS.length())%INTS.length());
    static boolean supported() {return INTS.length()>=4 && INTS.length()==2*LONGS.length();}
    public long dot(int[] us,int[] them,int[] weights,int offset) {
        var sum=IntVector.zero(INTS);
        for(int i=0;i<256;i+=INTS.length()) {
            var a=IntVector.fromArray(INTS,us,i).max(0).min(255);
            var b=IntVector.fromArray(INTS,them,i).max(0).min(255);
            sum=sum.add(a.mul(a).mul(IntVector.fromArray(INTS,weights,offset+i)));
            sum=sum.add(b.mul(b).mul(IntVector.fromArray(INTS,weights,offset+256+i)));
        }
        var low=(LongVector)sum.convertShape(VectorOperators.I2L,LONGS,0);
        var high=(LongVector)sum.rearrange(HIGH_TO_LOW).convertShape(VectorOperators.I2L,LONGS,0);
        return low.add(high).reduceLanes(VectorOperators.ADD);
    }
    public long dense(int[] activated,int at,int[] weights,int offset) {
        var first=IntVector.zero(INTS);var second=IntVector.zero(INTS);
        for(int i=0;i<256;i+=INTS.length()) {
            first=first.add(IntVector.fromArray(INTS,activated,at+i).mul(IntVector.fromArray(INTS,weights,offset+i)));
            second=second.add(IntVector.fromArray(INTS,activated,at+256+i).mul(IntVector.fromArray(INTS,weights,offset+256+i)));
        }
        var sum=first.add(second);
        var low=(LongVector)sum.convertShape(VectorOperators.I2L,LONGS,0);
        var high=(LongVector)sum.rearrange(HIGH_TO_LOW).convertShape(VectorOperators.I2L,LONGS,0);
        return low.add(high).reduceLanes(VectorOperators.ADD);
    }
    public void prepare(int[] us,int[] them,int[] activated,int at) {
        for(int i=0;i<256;i+=INTS.length()) {
            var a=IntVector.fromArray(INTS,us,i).max(0).min(255);
            var b=IntVector.fromArray(INTS,them,i).max(0).min(255);
            a.mul(a).intoArray(activated,at+i);b.mul(b).intoArray(activated,at+256+i);
        }
    }
}
