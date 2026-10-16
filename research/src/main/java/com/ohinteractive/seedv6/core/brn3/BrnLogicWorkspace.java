package com.ohinteractive.seedv6.core.brn3;

import com.ohinteractive.seedv6.core.Board;
import static com.ohinteractive.seedv6.core.brn3.BrnLogicTrainer.*;

/** Immutable hard-gate model, worker-local full bitboard propagation and placement/STM reuse. */
public final class BrnLogicWorkspace {
    private final BrnLogicTrainer.Model model;
    private final long[][] raw=new long[2][RAW],bits;
    private final double[][] features;
    private final long[] previous=new long[4];
    private boolean initialized;
    private double whiteMaterial;
    public long rebuilds,repeats;
    BrnLogicWorkspace(BrnLogicTrainer.Model model) {
        this.model=model;bits=new long[2][model.ops.length];features=new double[2][model.ops.length*REGIONS];
    }
    public double evaluatePawns(long[] board) {
        boolean same=initialized;for(int i=0;i<4;i++)same&=board[i]==previous[i];int stm=Board.player((int)board[4]);
        if(same)repeats++;
        else {
            rebuilds++;rawMaps(board,raw);hardMaps(raw,bits,model.topology,model.ops);
            for(int p=0;p<2;p++)for(int g=0;g<model.ops.length;g++)for(int r=0;r<REGIONS;r++)features[p][g*REGIONS+r]=NORM*Long.bitCount(bits[p][g]&MASKS[r]);
            whiteMaterial=Brn3Features.material(board)*(stm==0?1:-1);System.arraycopy(board,0,previous,0,4);initialized=true;
        }
        double value=(stm==0?whiteMaterial:-whiteMaterial)+model.weights[model.weights.length-1];int n=features[0].length;
        for(int role=0;role<2;role++)for(int i=0;i<n;i++)value+=model.weights[role*n+i]*features[stm^role][i];
        if(!Double.isFinite(value))throw new ArithmeticException("Nonfinite hard logic value");return value;
    }
    public double evaluatePawns(long[] board,double gain) {
        if(!Double.isFinite(gain)||gain<0||gain>1)throw new IllegalArgumentException("gain");
        double raw=evaluatePawns(board),material=Board.player((int)board[4])==0?whiteMaterial:-whiteMaterial;return material+gain*(raw-material);
    }
}
