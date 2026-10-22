import com.google.gson.Gson;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.conventional.ConventionalNetwork;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.SplittableRandom;
import jdk.incubator.vector.*;

/** Isolated feasibility experiment. Not part of any engine/application distribution. */
public final class SeedNnueVectorPrototype {
    static final VectorSpecies<Integer> IS=IntVector.SPECIES_PREFERRED;
    static final VectorSpecies<Long> LS=LongVector.SPECIES_PREFERRED;
    static volatile long sink;
    record Sample(int[] us,int[] them,int offset,int bias,int expected) {}
    static int scale(long sum,int bias) {return (int)Math.clamp((sum/255+bias)*400/(255*64),-32511L,32511L);}
    static long scalarDot(Sample s,int[] w) {
        long sum=0;
        for(int i=0;i<s.us.length;i++) {
            int a=Math.clamp(s.us[i],0,255),b=Math.clamp(s.them[i],0,255);
            sum+=(long)a*a*w[s.offset+i]+(long)b*b*w[s.offset+s.us.length+i];
        }
        return sum;
    }
    static int scalar(Sample s,int[] w) {return scale(scalarDot(s,w),s.bias);}
    static long vectorDot(Sample s,int[] w) {
        LongVector lo=LongVector.zero(LS),hi=LongVector.zero(LS);
        for(int i=0;i<s.us.length;i+=IS.length()) {
            var a=IntVector.fromArray(IS,s.us,i).max(0).min(255);
            var b=IntVector.fromArray(IS,s.them,i).max(0).min(255);
            // Each individual 255^2 * signed-short product fits signed int.
            // Widen before adding products; neither vector nor horizontal sums overflow int.
            var x=a.mul(a).mul(IntVector.fromArray(IS,w,s.offset+i));
            var y=b.mul(b).mul(IntVector.fromArray(IS,w,s.offset+s.us.length+i));
            lo=lo.add((LongVector)x.convertShape(VectorOperators.I2L,LS,0));
            hi=hi.add((LongVector)x.convertShape(VectorOperators.I2L,LS,1));
            lo=lo.add((LongVector)y.convertShape(VectorOperators.I2L,LS,0));
            hi=hi.add((LongVector)y.convertShape(VectorOperators.I2L,LS,1));
        }
        return lo.add(hi).reduceLanes(VectorOperators.ADD);
    }
    static int vector(Sample s,int[] w) {return scale(vectorDot(s,w),s.bias);}
    static void run(Sample[] samples,int[] w,boolean vector,int n) {
        long sum=0;for(int i=0;i<n;i++){var s=samples[i%samples.length];sum+=vector?vector(s,w):scalar(s,w);}sink=sum;
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=2)throw new IllegalArgumentException("NETWORK PROBES");
        if(System.getenv("SEED_NNUE_OPERATION")==null)throw new IllegalStateException("Use nnue-control.cmd run");
        if(IS.vectorBitSize()!=LS.vectorBitSize() || IS.length()!=2*LS.length() || 256%IS.length()!=0)
            throw new IllegalStateException("Unsupported species for this bounded prototype");
        var net=ConventionalNetwork.load(Path.of(args[0]));
        var data=ByteBuffer.wrap(Files.readAllBytes(Path.of(args[0]))).order(ByteOrder.LITTLE_ENDIAN);
        data.position(72+2*(7680*256+256));
        int[] w=new int[net.outputBuckets()*512],bias=new int[net.outputBuckets()];
        for(int i=0;i<w.length;i++)w[i]=data.getShort();for(int i=0;i<bias.length;i++)bias[i]=data.getShort();
        var list=new ArrayList<Sample>();var acc=net.newAccumulator();
        for(String fen:Files.readAllLines(Path.of(args[1]))) {
            long[] board=Board.fromFen(fen);acc.rebuild(board);int stm=Board.player((int)board[4]);
            int head=net.outputBuckets()==1?0:(Long.bitCount(board[0]|board[1]|board[2])-2)/4;
            int[] us=new int[256],them=new int[256];
            for(int i=0;i<256;i++){us[i]=acc.raw(stm,i);them[i]=acc.raw(stm^1,i);}
            var s=new Sample(us,them,head*512,bias[head],acc.evaluate(board));
            if(scalar(s,w)!=s.expected || vector(s,w)!=s.expected || net.fullRebuild(board)!=s.expected)
                throw new AssertionError("Network parity: "+fen);
            list.add(s);
        }
        var random=new SplittableRandom(20261022);int[] hostile=new int[512];
        for(int trial=0;trial<10000;trial++) {
            int[] us=new int[256],them=new int[256];
            for(int i=0;i<256;i++) {us[i]=random.nextInt(-1_000_000,1_000_000);them[i]=random.nextInt(-1_000_000,1_000_000);}
            for(int i=0;i<512;i++)hostile[i]=trial<3?(trial==0?32767:trial==1?-32768:(i%2==0?32767:-32768)):random.nextInt(-32768,32768);
            var s=new Sample(us,them,0,random.nextInt(-32768,32768),0);
            if(vectorDot(s,hostile)!=scalarDot(s,hostile))throw new AssertionError("Hostile raw-dot parity "+trial);
        }
        var samples=list.toArray(Sample[]::new);int n=200000;
        for(int i=0;i<5;i++){run(samples,w,false,n);run(samples,w,true,n);}
        var result=new LinkedHashMap<String,Object>();result.put("payload",net.payloadSha256());
        result.put("vector_bits",IS.vectorBitSize());result.put("network_positions",samples.length);result.put("hostile_trials",10000);
        var scalar=new ArrayList<Double>();var vector=new ArrayList<Double>();
        for(int i=0;i<5;i++) {
            long t=System.nanoTime();run(samples,w,false,n);scalar.add((System.nanoTime()-t)/(double)n);
            t=System.nanoTime();run(samples,w,true,n);vector.add((System.nanoTime()-t)/(double)n);
        }
        result.put("scalar_head_ns",scalar);result.put("vector_head_ns",vector);
        if(ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean bean && bean.isThreadAllocatedMemorySupported()) {
            bean.setThreadAllocatedMemoryEnabled(true);long id=Thread.currentThread().threadId();long before=bean.getThreadAllocatedBytes(id);
            run(samples,w,true,n);result.put("vector_allocated_bytes_200000",bean.getThreadAllocatedBytes(id)-before);
        }
        result.put("limits","Head-only prototype with preexpanded int output weights; no integrated speed or strength claim, no application adoption.");
        System.out.println(new Gson().toJson(result));
    }
}
