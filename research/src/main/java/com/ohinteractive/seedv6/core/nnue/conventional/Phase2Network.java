package com.ohinteractive.seedv6.core.nnue.conventional;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Explicit experimental envelope; never accepted by the production loader. */
public final class Phase2Network {
    public static final int H=256,QA=255,QB=64,SCALE=400,HEADER=76;
    private static final boolean COUNT=Boolean.getBoolean("seed.phase2.count");
    private static final ThreadLocal<long[]> COUNTERS=ThreadLocal.withInitial(()->new long[4]);
    public static void resetProbeCounters(){if(COUNT)Arrays.fill(COUNTERS.get(),0);}
    public static Map<String,Object> probeCounters(){var c=COUNTERS.get();return Map.of("enabled",COUNT,"evaluations",c[0],"children",c[1],"perspective_refreshes",c[2],"feature_additions",c[3]);}
    static void refreshCount(){if(COUNT)COUNTERS.get()[2]++;}
    private final int kind,features;
    private final short[] weights,bias,outputBias,deepBias,residualBias;
    private final int[] output,deep,residual;
    private final Head head;
    private final String hash;
    interface Head {
        long dot(int[] us,int[] them,int[] weights,int offset);
        default void prepare(int[] us,int[] them,int[] activated,int at) {
            for(int i=0;i<256;i++) {
                int a=Math.clamp(us[i],0,QA),b=Math.clamp(them[i],0,QA);
                activated[at+i]=a*a;activated[at+256+i]=b*b;
            }
        }
        default long dense(int[] activated,int at,int[] weights,int offset) {
            long sum=0;for(int i=0;i<512;i++)sum+=(long)activated[at+i]*weights[offset+i];return sum;
        }
    }
    private Phase2Network(int kind,int features,byte[] payload,String hash) throws IOException {
        this.kind=kind;this.features=features;this.hash=hash;
        var data=ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        weights=new short[features*H];bias=new short[H];output=new int[512*8];outputBias=new short[8];
        data.get(weights);data.get(bias);readHead(data,output);data.get(outputBias);
        deep=new int[kind==1?512*16:0];deepBias=new short[kind==1?16:0];
        residual=new int[kind==1?16*8:0];residualBias=new short[kind==1?8:0];
        readHead(data,deep);data.get(deepBias);readHead(data,residual);data.get(residualBias);
        head=!Boolean.getBoolean("seed.nnue.scalar")&&ModuleLayer.boot().findModule("jdk.incubator.vector").isPresent()
             &&Phase2VectorHead.supported()?new Phase2VectorHead():Phase2Network::scalarDot;
    }
    private static void readHead(ShortBuffer data,int[] dst) throws IOException {
        for(int i=0;i<dst.length;i++) {int w=data.get();if(Math.abs(w)>127)throw new IOException("Unbounded downstream weight");dst[i]=w;}
    }
    public static Phase2Network load(Path path)throws IOException {try(var in=Files.newInputStream(path)){return read(in);}}
    public static Phase2Network read(InputStream in)throws IOException {
        byte[] header=in.readNBytes(HEADER);if(header.length!=HEADER)throw new IOException("Truncated Phase2 header");
        var h=ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);byte[] magic=new byte[8];h.get(magic);
        if(!Arrays.equals(magic,"S6P2N001".getBytes(java.nio.charset.StandardCharsets.US_ASCII))||h.getInt()!=1)
            throw new IOException("Unknown Phase2 format");
        int kind=h.getInt();if(kind<0||kind>2)throw new IOException("Unknown Phase2 architecture");
        int features=7680+(kind==2?Phase2Features.PAIRS:0),depth=kind==1?16:0;
        int length=2*(features*H+H+512*8+8+depth*(512+1+8)+(kind==1?8:0));
        for(int expected:new int[]{features,H,depth,QA,QB,SCALE,length})
            if(h.getInt()!=expected)throw new IOException("Phase2 geometry/scaling mismatch");
        byte[] hash=new byte[32];h.get(hash);byte[] payload=in.readNBytes(length);
        if(payload.length!=length||in.read()!=-1)throw new IOException("Phase2 payload length");
        try {if(!MessageDigest.isEqual(hash,MessageDigest.getInstance("SHA-256").digest(payload)))throw new IOException("Phase2 checksum");}
        catch(NoSuchAlgorithmException e){throw new AssertionError(e);}
        return new Phase2Network(kind,features,payload,HexFormat.of().formatHex(hash));
    }
    public int kind(){return kind;}
    public int hidden(){return H;}
    public int outputBuckets(){return 8;}
    public String headBackend(){return backend();}
    public int featureCount(){return features;}
    public String payloadSha256(){return hash;}
    public String architecture(){return switch(kind){case 0->"base";case 1->"deep16";default->"pawnpairs";};}
    public String backend(){return head instanceof Phase2VectorHead?"vector-bounded-int":"scalar-long";}
    public Phase2Accumulator newAccumulator(){return new Phase2Accumulator(this);}
    void reset(int[] dst){for(int i=0;i<H;i++)dst[i]=bias[i];}
    void add(int feature,int sign,int[] dst){if(COUNT)COUNTERS.get()[3]++;int offset=feature*H;for(int i=0;i<H;i++)dst[i]+=sign*weights[offset+i];}
    private static long scalarDot(int[] us,int[] them,int[] weights,int offset) {
        long sum=0;for(int i=0;i<H;i++){
            long a=Math.clamp(us[i],0,QA),b=Math.clamp(them[i],0,QA);
            sum+=a*a*weights[offset+i]+b*b*weights[offset+H+i];
        }return sum;
    }
    int bucket(long[] board) {
        int count=Long.bitCount(board[0]|board[1]|board[2]);
        if(count<2||count>32)throw new IllegalArgumentException("Material outside2..32");
        return (count-2)/4;
    }
    int score(int[] us,int[] them,int bucket,int[] scratch,boolean scalar) {
        Head kernel=scalar?Phase2Network::scalarDot:head;
        long total;
        if(kind==1) {
            kernel.prepare(us,them,scratch,16);
            total=kernel.dense(scratch,16,output,bucket*512)/QA+outputBias[bucket];
            for(int j=0;j<16;j++)scratch[j]=(int)Math.clamp((kernel.dense(scratch,16,deep,j*512)/QA+deepBias[j])/QB,0L,(long)QA);
            long sum=0;for(int j=0;j<16;j++)sum+=(long)scratch[j]*scratch[j]*residual[bucket*16+j];
            total+=sum/QA+residualBias[bucket];
        }else total=kernel.dot(us,them,output,bucket*512)/QA+outputBias[bucket];
        return (int)Math.clamp(total*SCALE/(QA*QB),-32511L,32511L);
    }
    /** Unit-major recomputation, independent of the accumulator's update/refresh loops. */
    public int fullRebuild(long[] board) {
        ConventionalFeatures.requireBoard(board);int stm=Board.player((int)board[4]);int[][] sums=new int[2][H];
        for(int side=0;side<2;side++) {
            int[] indices=Phase2Features.all(board,stm^side,kind==2);
            for(int j=0;j<H;j++){int raw=bias[j];for(int index:indices)raw+=weights[index*H+j];sums[side][j]=raw;}
        }
        return score(sums[0],sums[1],bucket(board),new int[kind==1?528:16],true);
    }
    public SearchEvaluation evaluation(){return SearchEvaluation.researchCentipawnsStateful(capacity->new Lifecycle(this,capacity));}
    private static final class Lifecycle implements SearchEvaluation.ResearchCpLifecycle {
        final Phase2Network model;final Phase2Accumulator[] stack;
        Lifecycle(Phase2Network model,int capacity){this.model=model;stack=new Phase2Accumulator[capacity];for(int i=0;i<capacity;i++)stack[i]=model.newAccumulator();}
        public void initialize(long[] b,int ply){stack[ply].rebuild(b);}
        public void child(long[] parent,long[] child,int ply){if(COUNT)COUNTERS.get()[1]++;stack[ply+1].update(parent,child,stack[ply]);}
        public void initializeFrom(long[] b,int ply,SearchEvaluation.ResearchCpLifecycle source){
            if(source==this||!(source instanceof Lifecycle other)||other.model!=model)throw new IllegalArgumentException("Model/owner mismatch");
            stack[ply].copyFrom(b,other.stack[ply]);
        }
        public double evaluate(long[] b,int ply){if(COUNT)COUNTERS.get()[0]++;return stack[ply].evaluate(b);}
    }
}
