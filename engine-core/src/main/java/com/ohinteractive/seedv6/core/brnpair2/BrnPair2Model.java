package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import java.util.*;

/** Experimental C02 compiled binary64 pair tables. Immutable and shared; caches are worker local. */
public final class BrnPair2Model {
    private final int[] firstSquare, secondSquare;
    private final int[][] incident;
    // Two side-to-move values are adjacent at each physical pair/category address.
    final double[] weights;
    final double bias;
    public static final double RESIDUAL_GAIN=.25;
    public static final String CALIBRATION_ID="BRN_PAIR2_MATERIAL_RESIDUAL_QUARTER_V1";
    public double weight(int index){return weights[index];}
    public double bias(){return bias;}

    public BrnPair2Model(BrnPair2Weights source) {
        this(Objects.requireNonNull(source),source.weights[source.weights.length-1]);
    }
    BrnPair2Model(BrnPair2Weights source,double bias) {
        if(source!=null&&source.order!=2)throw new IllegalArgumentException("C02 requires an order2 model");
        var keys=new TreeSet<Integer>();
        for(int perspective=0;perspective<2;perspective++)for(int t=0;t<BrnPair2Geometry.TEMPLATES;t++) {
            int a=BrnPair2Geometry.cell(t,0)^(perspective*56),b=BrnPair2Geometry.cell(t,1)^(perspective*56);
            keys.add(Math.min(a,b)*64+Math.max(a,b));
        }
        firstSquare=new int[keys.size()];secondSquare=new int[keys.size()];
        var indices=new HashMap<Integer,Integer>();int count=0;
        for(int key:keys){indices.put(key,count);firstSquare[count]=key/64;secondSquare[count++]=key%64;}
        if(keys.size()!=BrnPair2Codec.PAIRS)throw new IllegalStateException("BRE-Pair 2 relation layout changed");
        weights=new double[keys.size()*169*2];this.bias=bias;
        for(int stm=0;source!=null&&stm<2;stm++)for(int role=0;role<2;role++) {
            int perspective=stm^role;
            for(int t=0;t<BrnPair2Geometry.TEMPLATES;t++) {
                int a=BrnPair2Geometry.cell(t,0)^(perspective*56),b=BrnPair2Geometry.cell(t,1)^(perspective*56);
                int pair=indices.get(Math.min(a,b)*64+Math.max(a,b));
                int sourceBase=(role*BrnPair2Geometry.TEMPLATES+t)*169;
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
    /** Explicit research-only additive relation extension. It has no ordinary Pair2 codec identity.
     * Tables use physical categories and adjacent STM entries, in existing C02 normalization. */
    public static BrnPair2Model withResearchPairs(BrnPair2Model base,int[] first,int[] second,double[] tables) {
        return new BrnPair2Model(base,first,second,tables);
    }
    private BrnPair2Model(BrnPair2Model base,int[] first,int[] second,double[] tables) {
        Objects.requireNonNull(base);Objects.requireNonNull(first);Objects.requireNonNull(second);Objects.requireNonNull(tables);
        if(base.pairs()!=BrnPair2Codec.PAIRS||first.length<1||first.length>4||second.length!=first.length
                ||tables.length!=first.length*169*2)throw new IllegalArgumentException("Bounded ordinary-base relation extension required");
        var keys=new TreeMap<Integer,Integer>();
        for(int i=0;i<base.pairs();i++)keys.put(base.firstSquare[i]*64+base.secondSquare[i],i);
        for(int i=0;i<first.length;i++) {
            if(first[i]<0||second[i]>=64||first[i]>=second[i]||keys.putIfAbsent(first[i]*64+second[i],base.pairs()+i)!=null)
                throw new IllegalArgumentException("Distinct new canonical physical relations required");
        }
        for(double value:tables)if(!Double.isFinite(value))throw new IllegalArgumentException("Nonfinite supplemental table");
        firstSquare=new int[keys.size()];secondSquare=new int[keys.size()];weights=new double[keys.size()*169*2];bias=base.bias;
        int index=0;
        for(var entry:keys.entrySet()) {
            firstSquare[index]=entry.getKey()/64;secondSquare[index]=entry.getKey()%64;int source=entry.getValue();
            System.arraycopy(source<base.pairs()?base.weights:tables,(source<base.pairs()?source:source-base.pairs())*338,weights,index*338,338);
            index++;
        }
        incident=new int[64][];
        for(int square=0;square<64;square++) {
            var found=new ArrayList<Integer>();
            for(int pair=0;pair<keys.size();pair++)if(firstSquare[pair]==square||secondSquare[pair]==square)found.add(pair);
            incident[square]=found.stream().mapToInt(Integer::intValue).toArray();
        }
    }
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
            if(old!=0)whiteMaterial-=(old&8)==0?BrnPair2Features.pieceMaterial(old):-BrnPair2Features.pieceMaterial(old);
            if(code!=0)whiteMaterial+=(code&8)==0?BrnPair2Features.pieceMaterial(code):-BrnPair2Features.pieceMaterial(code);
            oldCodes[square]=code;codes[square]=BrnPair2Geometry.category(code,0);
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
            double value=(stm==0?whiteMaterial:-whiteMaterial)+bias+BrnPair2Geometry.NORM*(stm==0?whiteSum:blackSum);
            if(!Double.isFinite(value))throw new ArithmeticException("Nonfinite compiled tuple inference");
            return value;
        }
        public double evaluatePawns(long[] board,double gain) {
            if(!Double.isFinite(gain)||gain<0||gain>1)throw new IllegalArgumentException("gain");
            double raw=evaluatePawns(board),material=Board.player((int)board[4])==0?whiteMaterial:-whiteMaterial;
            return material+gain*(raw-material);
        }
        /** Raw value for the placement most recently evaluated by this workspace.
         * Like materialPawns, this performs no placement update. A composite
         * evaluator may reuse it after its private base has evaluated that board. */
        public double cachedPawns(int sideToMove) {
            if(!initialized||(sideToMove&~1)!=0)throw new IllegalStateException("Evaluate a placement and use side 0 or 1");
            double value=(sideToMove==0?whiteMaterial:-whiteMaterial)+bias
                    +BrnPair2Geometry.NORM*(sideToMove==0?whiteSum:blackSum);
            if(!Double.isFinite(value))throw new ArithmeticException("Nonfinite compiled tuple inference");
            return value;
        }
        /** Material of the placement most recently evaluated by this private workspace.
         * A residual evaluator can reuse it after evaluatePawns instead of rescanning all pieces. */
        public double materialPawns(int sideToMove) {
            if(!initialized||(sideToMove&~1)!=0)throw new IllegalStateException("Evaluate a placement and use side 0 or 1");
            return sideToMove==0?whiteMaterial:-whiteMaterial;
        }
    }
}
