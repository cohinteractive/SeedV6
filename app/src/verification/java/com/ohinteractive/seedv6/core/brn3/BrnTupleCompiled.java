package com.ohinteractive.seedv6.core.brn3;

import com.ohinteractive.seedv6.core.Board;
import java.util.*;

/** C02 inference-only compilation of the existing order2 scalar function. */
public final class BrnTupleCompiled {
    private final int[] firstSquare, secondSquare;
    private final int[][] incident;
    // Two side-to-move values are adjacent at each physical pair/category address.
    private final double[] weights;
    private final double bias;

    public BrnTupleCompiled(BrnTupleTrainer.Model source) {
        if(source.order!=2)throw new IllegalArgumentException("C02 requires an order2 model");
        var keys=new TreeSet<Integer>();
        for(int perspective=0;perspective<2;perspective++)for(int[] cells:BrnTupleTrainer.CELLS) {
            int a=cells[0]^(perspective*56),b=cells[1]^(perspective*56);
            keys.add(Math.min(a,b)*64+Math.max(a,b));
        }
        firstSquare=new int[keys.size()];secondSquare=new int[keys.size()];
        var indices=new HashMap<Integer,Integer>();int count=0;
        for(int key:keys){indices.put(key,count);firstSquare[count]=key/64;secondSquare[count++]=key%64;}
        weights=new double[keys.size()*169*2];bias=source.weights[source.weights.length-1];
        for(int stm=0;stm<2;stm++)for(int role=0;role<2;role++) {
            int perspective=stm^role;
            for(int t=0;t<BrnTupleTrainer.TEMPLATES;t++) {
                int a=BrnTupleTrainer.CELLS[t][0]^(perspective*56),b=BrnTupleTrainer.CELLS[t][1]^(perspective*56);
                int pair=indices.get(Math.min(a,b)*64+Math.max(a,b));
                int sourceBase=(role*BrnTupleTrainer.TEMPLATES+t)*169;
                for(int low=0;low<13;low++)for(int high=0;high<13;high++) {
                    int x=a<b?low:high,y=a<b?high:low;
                    if(perspective==1){x=oppositeCategory(x);y=oppositeCategory(y);}
                    weights[(pair*169+low+13*high)*2+stm]+=source.weights[sourceBase+x+13*y];
                }
            }
        }
        incident=new int[64][];
        for(int square=0;square<64;square++) {
            var found=new ArrayList<Integer>();
            for(int pair=0;pair<keys.size();pair++)if(firstSquare[pair]==square||secondSquare[pair]==square)found.add(pair);
            incident[square]=found.stream().mapToInt(Integer::intValue).toArray();
        }
        for(double value:weights)if(!Double.isFinite(value))throw new IllegalArgumentException("Nonfinite compiled table");
    }
    private static int oppositeCategory(int category){return category==0?0:category<=6?category+6:category-6;}
    public int pairs(){return firstSquare.length;}
    public long immutablePrimitiveBytes(){
        long bytes=8L*weights.length+8+4L*(firstSquare.length+secondSquare.length);
        for(int[] row:incident)bytes+=4L*row.length;
        return bytes;
    }
    public Workspace newWorkspace(){return new Workspace();}

    /** Worker-local rolling placement cache; no Gen0/zero-weight fast path. */
    public final class Workspace {
        private final int[] codes=new int[64],oldCodes=new int[64],addresses=new int[pairs()],stamps=new int[pairs()];
        private final long[] previous=new long[4];
        private double whiteMaterial,whiteSum,blackSum;
        private boolean initialized;
        private int serial;
        public long rebuilds,updates,repeats;
        public long primitiveBytes(){return 4L*(codes.length+oldCodes.length+addresses.length+stamps.length)+8L*previous.length+24;}
        private void setCode(long[] board,int square) {
            int code=Board.getSquare(board[0],board[1],board[2],board[3],square),old=oldCodes[square];
            // Empty-square ownership bits are outside the permitted information.
            if((code&7)==0)code=0;
            if(old!=0)whiteMaterial-=(old&8)==0?Brn3Features.pieceMaterial(old):-Brn3Features.pieceMaterial(old);
            if(code!=0)whiteMaterial+=(code&8)==0?Brn3Features.pieceMaterial(code):-Brn3Features.pieceMaterial(code);
            oldCodes[square]=code;codes[square]=BrnTupleTrainer.category(code,0);
        }
        private int address(int pair){return (pair*169+codes[firstSquare[pair]]+13*codes[secondSquare[pair]])*2;}
        public double evaluatePawns(long[] board) {
            long changed=0;for(int i=0;i<4;i++)changed|=previous[i]^board[i];
            changed&=board[0]|board[1]|board[2]|previous[0]|previous[1]|previous[2];
            if(initialized&&changed==0)repeats++;
            else if(!initialized||Long.bitCount(changed)>8) {
                rebuilds++;Arrays.fill(oldCodes,0);whiteMaterial=0;whiteSum=0;blackSum=0;
                for(int square=0;square<64;square++)setCode(board,square);
                for(int pair=0;pair<pairs();pair++) {
                    int next=address(pair);addresses[pair]=next;
                    whiteSum+=weights[next];blackSum+=weights[next+1];
                }
            } else {
                updates++;
                for(long bits=changed;bits!=0;bits&=bits-1)setCode(board,Long.numberOfTrailingZeros(bits));
                if(++serial==0){Arrays.fill(stamps,0);serial=1;}
                for(long bits=changed;bits!=0;bits&=bits-1)for(int pair:incident[Long.numberOfTrailingZeros(bits)]) {
                    if(stamps[pair]==serial)continue;stamps[pair]=serial;
                    int old=addresses[pair],next=address(pair);addresses[pair]=next;
                    whiteSum+=weights[next]-weights[old];blackSum+=weights[next+1]-weights[old+1];
                }
            }
            initialized=true;System.arraycopy(board,0,previous,0,4);int stm=Board.player((int)board[4]);
            double value=(stm==0?whiteMaterial:-whiteMaterial)+bias+BrnTupleTrainer.NORM*(stm==0?whiteSum:blackSum);
            if(!Double.isFinite(value))throw new ArithmeticException("Nonfinite compiled tuple inference");
            return value;
        }
        public double evaluatePawns(long[] board,double gain) {
            if(!Double.isFinite(gain)||gain<0||gain>1)throw new IllegalArgumentException("gain");
            double raw=evaluatePawns(board),material=Board.player((int)board[4])==0?whiteMaterial:-whiteMaterial;
            return material+gain*(raw-material);
        }
    }
}
