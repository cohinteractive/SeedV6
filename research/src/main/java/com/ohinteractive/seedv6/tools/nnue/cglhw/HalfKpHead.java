package com.ohinteractive.seedv6.tools.nnue.cglhw;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;

/** H001: project a random head onto antisymmetric perspective functions.
 * H003: optional fixed extra five bits of score resolution, independently crossed.
 * No training, material terms, sign selection or altered random initialization. */
public final class HalfKpHead implements IncrementalModel {
    private final NnueNetwork network;
    private final boolean odd;
    private final double scale;
    public HalfKpHead(long seed,boolean odd,boolean highResolution) {
        this(seed,odd,highResolution,false);
    }
    public HalfKpHead(long seed,boolean odd,boolean highResolution,boolean zeroHead) {
        this.network=zeroHead?zeroReadout(seed):NnueNetwork.initialized(seed);this.odd=odd;
        scale=NnueScoreMapping.V1.scale()*(highResolution?32:1);
    }
    /** Pin an existing checkpoint for cross-path equivalence without reinitializing weights. */
    public HalfKpHead(NnueNetwork network) {
        this.network = java.util.Objects.requireNonNull(network); this.odd = false; this.scale = NnueScoreMapping.V1.scale();
    }
    private static NnueNetwork zeroReadout(long seed) {
        var original=NnueNetwork.initialized(seed);var weights=new float[NnueNetwork.FEATURE_WEIGHT_COUNT];
        for(int f=0;f<NnueFeatureSchema.FEATURE_COUNT;f++)for(int u=0;u<64;u++)weights[f*64+u]=original.featureWeight(f,u);
        var dense=new float[128*32];for(int h=0;h<32;h++)for(int i=0;i<128;i++)dense[h*128+i]=original.hiddenWeight(h,i);
        return NnueNetwork.of(NnueFeatureSchema.VERSION,new float[64],weights,new float[32],dense,0,new float[32]);
    }
    @Override public int parameters() { return NnueNetwork.PARAMETER_COUNT; }
    @Override public Worker worker(int capacity) {
        return new Worker(scale) {
            private final NnueEvaluator readout=new NnueEvaluator(network);
            private final NnueAccumulator[] states=new NnueAccumulator[capacity];
            private final long[] swapped=new long[Board.MAX_BITBOARDS];
            { for(int i=0;i<capacity;i++)states[i]=new NnueAccumulator(network); }
            @Override public void refresh(long[] b,int slot) { states[slot].rebuild(b); }
            @Override public void transition(long[] p,long[] c,int src,int dst) { states[dst].update(p,c,states[src]); }
            @Override public double raw(long[] b,int slot) {
                double forward=readout.evaluate(b,states[slot]);
                if(!odd)return forward;
                // This is only a readout perspective swap, not a chess move or search board.
                System.arraycopy(b,0,swapped,0,swapped.length);swapped[Board.STATUS]^=Board.PLAYER_BIT;
                double reverse=readout.evaluate(swapped,states[slot]);
                return .5*(forward-reverse);
            }
        };
    }
}
