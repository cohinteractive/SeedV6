package com.ohinteractive.seedv6.core.brn3;

/** Version-one geometry-only R4 layout, fixed by the BRN programme contract. */
public final class Brn3Layout {
    public static final int VERSION=1, WIDTH=8, POOL_WIDTH=12*WIDTH, HIDDEN_WIDTH=32;
    public static final int EDGE_ROWS=768*767/2;
    public static final int NODES=EDGE_ROWS*WIDTH;
    public static final int DENSE=NODES+768*WIDTH;
    public static final int BIAS=DENSE+2*POOL_WIDTH*HIDDEN_WIDTH;
    public static final int HEAD=BIAS+HIDDEN_WIDTH, OUTPUT_BIAS=HEAD+HIDDEN_WIDTH;
    public static final int MODEL_PARAMETERS=OUTPUT_BIAS+1;
    public static final int RELATIVE_PARAMETERS=12*12*225*WIDTH;
    public static final int TRAINING_PARAMETERS=MODEL_PARAMETERS+RELATIVE_PARAMETERS;
    static int edge(int a,int b) {
        int large=Math.max(a,b),small=Math.min(a,b);
        if(large==small)throw new IllegalArgumentException("A piece cannot relate to itself");
        return (large*(large-1)/2+small)*WIDTH;
    }
    private Brn3Layout(){}
}
