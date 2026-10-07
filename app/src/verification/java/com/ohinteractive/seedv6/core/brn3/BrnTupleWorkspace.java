package com.ohinteractive.seedv6.core.brn3;

import com.ohinteractive.seedv6.core.Board;
import java.util.*;
import static com.ohinteractive.seedv6.core.brn3.BrnTupleTrainer.*;

/** Worker-local incremental categorical addressing over immutable folded tables. */
public final class BrnTupleWorkspace {
    private final float[] weights;
    private final int order,states;
    private final int[][] incident=new int[64][],codes=new int[2][64],addresses=new int[2][TEMPLATES];
    private final int[] oldCodes=new int[64],stamps=new int[TEMPLATES];
    private final double[][] sums=new double[2][2];
    private final long[] previous=new long[4];
    private int serial;private boolean initialized;private double whiteMaterial;
    public long rebuilds,updates,repeats;
    public BrnTupleWorkspace(BrnTupleTrainer.Model model) {
        order=model.order;states=model.states;weights=model.weights;
        for(int sq=0;sq<64;sq++) {
            var list=new ArrayList<Integer>();
            for(int t=0;t<TEMPLATES;t++)for(int j=0;j<order;j++)if(CELLS[t][j]==sq)list.add(t);
            incident[sq]=list.stream().mapToInt(Integer::intValue).toArray();
        }
    }
    private void setCode(long[] board,int sq) {
        int code=Board.getSquare(board[0],board[1],board[2],board[3],sq),old=oldCodes[sq];
        if(old!=0)whiteMaterial-=(old&8)==0?Brn3Features.pieceMaterial(old):-Brn3Features.pieceMaterial(old);
        if(code!=0)whiteMaterial+=(code&8)==0?Brn3Features.pieceMaterial(code):-Brn3Features.pieceMaterial(code);
        oldCodes[sq]=code;for(int p=0;p<2;p++)codes[p][sq^(p*56)]=category(code,p);
    }
    public double evaluatePawns(long[] board) {
        long changed=0;for(int i=0;i<4;i++)changed|=previous[i]^board[i];
        // Color-plane garbage on empty cells is not learned information.
        changed&=board[0]|board[1]|board[2]|previous[0]|previous[1]|previous[2];
        if(initialized&&changed==0)repeats++;
        else if(!initialized||Long.bitCount(changed)>8) {
            rebuilds++;Arrays.fill(oldCodes,0);whiteMaterial=0;
            for(int sq=0;sq<64;sq++)setCode(board,sq);
            for(int p=0;p<2;p++) {
                Arrays.fill(sums[p],0);
                for(int t=0;t<TEMPLATES;t++) {
                    int address=state(codes[p],t,order);addresses[p][t]=address;
                    for(int role=0;role<2;role++)sums[p][role]+=weights[(role*TEMPLATES+t)*states+address];
                }
            }
        } else {
            updates++;for(long bits=changed;bits!=0;bits&=bits-1)setCode(board,Long.numberOfTrailingZeros(bits));
            for(int p=0;p<2;p++) {
                if(++serial==0){Arrays.fill(stamps,0);serial=1;}
                for(long bits=changed;bits!=0;bits&=bits-1)for(int t:incident[Long.numberOfTrailingZeros(bits)^(p*56)]) {
                    if(stamps[t]==serial)continue;stamps[t]=serial;
                    int old=addresses[p][t],next=state(codes[p],t,order);addresses[p][t]=next;
                    for(int role=0;role<2;role++) {
                        int base=(role*TEMPLATES+t)*states;
                        sums[p][role]+=(double)weights[base+next]-(double)weights[base+old];
                    }
                }
            }
        }
        initialized=true;System.arraycopy(board,0,previous,0,4);int stm=Board.player((int)board[4]);
        double value=(stm==0?whiteMaterial:-whiteMaterial)+weights[weights.length-1]+NORM*(sums[stm][0]+sums[stm^1][1]);
        if(!Double.isFinite(value))throw new ArithmeticException("Nonfinite tuple inference");return value;
    }
    public double evaluatePawns(long[] board,double gain) {
        if(!Double.isFinite(gain)||gain<0||gain>1)throw new IllegalArgumentException("gain");
        double raw=evaluatePawns(board),material=Board.player((int)board[4])==0?whiteMaterial:-whiteMaterial;
        return material+gain*(raw-material);
    }
}
