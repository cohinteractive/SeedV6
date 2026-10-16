package com.ohinteractive.seedv6.core.brnpair2;

import static com.ohinteractive.seedv6.core.brnpair2.BrnPair2Geometry.*;

/** Immutable folded float32 compilation input; no optimizer or store state. */
public final class BrnPair2Weights {
    final int order,states;
    final float[] weights;

    public BrnPair2Weights(int order,float[] weights) {
        requireOrder(order);this.order=order;states=169;
        if(weights.length!=2*TEMPLATES*states+1)throw new IllegalArgumentException("shape");
        this.weights=weights.clone();
        for(float v:weights)if(!Float.isFinite(v))throw new IllegalArgumentException("Nonfinite tuple model");
    }
    public int parameters(){return weights.length;}
    public float weight(int index){return weights[index];}
}
