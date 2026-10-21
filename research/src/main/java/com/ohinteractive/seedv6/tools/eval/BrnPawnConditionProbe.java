package com.ohinteractive.seedv6.tools.eval;

import com.google.gson.JsonParser;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Fen;
import com.ohinteractive.seedv6.training.data.DataFiles;
import java.nio.file.*;
import java.util.*;

/** Read-only sparse information/cost feasibility. No model, optimizer or search integration. */
public final class BrnPawnConditionProbe {
    static final double NORM=1/Math.sqrt(8);
    static final long[][] AHEAD=new long[2][64];
    static {
        for(int color=0;color<2;color++)for(int square=0;square<64;square++)
            for(int other=0;other<64;other++)
                if(Math.abs((square&7)-(other&7))<=1&&
                        (color==0?(other>>>3)>(square>>>3):(other>>>3)<(square>>>3)))
                    AHEAD[color][square]|=1L<<other;
    }
    static long pawns(long[] b) { return ~b[0]&b[1]&b[2]; }
    static int progress(int color,int square) { return color==0?square>>>3:7-(square>>>3); }
    static void features(long[] b,boolean conditional,double[] out) {
        Arrays.fill(out,0);long all=pawns(b),white=all&~b[3],black=all&b[3];
        int stm=Board.player((int)b[Board.STATUS]);
        for(int color=0;color<2;color++)for(long bits=color==0?white:black;bits!=0;bits&=bits-1) {
            int square=Long.numberOfTrailingZeros(bits),rank=progress(color,square);
            if(rank<1||rank>6)throw new IllegalArgumentException("Nonstandard pawn rank");
            int index=(color==stm?0:6)+rank-1;
            long blockers=AHEAD[color][square]&(color==0?black:white);
            out[index]+=NORM;
            out[index+12]+=(conditional?(blockers==0?1:0):Long.bitCount(blockers)/4.0)*NORM;
        }
    }
    /** Independent coordinate/piece walker, deliberately avoiding pawn masks. */
    static double[] oracle(long[] b,boolean conditional) {
        double[] out=new double[24];int stm=Board.player((int)b[Board.STATUS]);
        for(int s=0;s<64;s++) {
            int p=Board.getSquare(b[0],b[1],b[2],b[3],s);if((p&7)!=6)continue;
            int color=(p>>>3)&1,rank=color==0?s>>>3:7-(s>>>3),count=0;
            for(int t=0;t<64;t++) {
                int q=Board.getSquare(b[0],b[1],b[2],b[3],t);
                if((q&7)==6&&((q>>>3)&1)!=color&&Math.abs((s&7)-(t&7))<=1&&
                        (color==0?(t>>>3)>(s>>>3):(t>>>3)<(s>>>3)))count++;
            }
            int index=(color==stm?0:6)+rank-1;out[index]+=NORM;
            out[index+12]+=(conditional?(count==0?1:0):count/4.0)*NORM;
        }
        return out;
    }
    static double dot(double[] w,double[] x) { double result=0;for(int i=0;i<24;i++)result+=w[i]*x[i];return result; }
    /** Both perspectives cached; all nonpawn moves and side changes reuse the condition. */
    static final class Cache {
        final boolean conditional;final double[] w;
        long white,black,refreshes;boolean valid;double whiteValue,blackValue;
        Cache(boolean conditional,double[] weights) {this.conditional=conditional;w=weights.clone();}
        double evaluate(long[] b) {
            long all=pawns(b),wp=all&~b[3],bp=all&b[3];
            if(!valid||wp!=white||bp!=black) {
                valid=true;white=wp;black=bp;refreshes++;whiteValue=blackValue=0;
                for(int color=0;color<2;color++)for(long bits=color==0?white:black;bits!=0;bits&=bits-1) {
                    int s=Long.numberOfTrailingZeros(bits),rank=progress(color,s)-1;
                    if(rank<0||rank>=6)throw new IllegalArgumentException("Nonstandard pawn rank");
                    long blockers=AHEAD[color][s]&(color==0?black:white);
                    double v=conditional?(blockers==0?1:0):Long.bitCount(blockers)/4.0;
                    double own=(w[rank]+v*w[12+rank])*NORM;
                    double enemy=(w[6+rank]+v*w[18+rank])*NORM;
                    whiteValue+=color==0?own:enemy;blackValue+=color==0?enemy:own;
                }
            }
            return Board.player((int)b[Board.STATUS])==0?whiteValue:blackValue;
        }
    }
    private static volatile double sink;
    static long run(List<long[]> boards,boolean conditional,boolean cached,double[] weights,int repetitions) {
        var c=new Cache(conditional,weights);double[] x=new double[24];double result=0;
        for(int i=0;i<repetitions;i++) {
            var b=boards.get(i%boards.size());
            if(cached)result+=c.evaluate(b);else {features(b,conditional,x);result+=dot(weights,x);}
        }
        sink=result;return c.refreshes;
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=2)throw new IllegalArgumentException("PRESERVED_GAME_RECEIPTS NEW_OUTPUT_DIRECTORY");
        Path input=Path.of(args[0]),output=Path.of(args[1]);Files.createDirectory(output);
        var boards=new ArrayList<long[]>();var bindings=new TreeMap<String,String>();
        try(var files=Files.list(input)) {
            for(var p:files.filter(p->p.getFileName().toString().matches("p\\d+-m\\d+-c\\d+\\.json")).sorted().toList()) {
                String text=Files.readString(p);bindings.put(p.toString(),DataFiles.hash(text));
                var row=JsonParser.parseString(text).getAsJsonObject();boards.add(Board.fromFen(row.get("openingFen").getAsString()));
                for(var move:row.getAsJsonArray("moves"))boards.add(Board.fromFen(move.getAsJsonObject().get("fenAfter").getAsString()));
            }
        }
        if(boards.isEmpty()||boards.size()>20000)throw new IllegalArgumentException("Bounded played positions required");
        var random=new SplittableRandom(2026102209);double[] weights=new double[24];
        for(int i=0;i<weights.length;i++)weights[i]=random.nextDouble(-.2,.2);
        double error=0;long[] refreshes=new long[2];
        var caches=new Cache[]{new Cache(false,weights),new Cache(true,weights)};
        try(var writer=Files.newBufferedWriter(output.resolve("features.jsonl"),StandardOpenOption.CREATE_NEW)) {
            for(var b:boards) {
                var row=new LinkedHashMap<String,Object>();row.put("fen",Fen.fromBoard(b));
                for(int mode=0;mode<2;mode++) {
                    double[] x=new double[24];features(b,mode==1,x);double[] expected=oracle(b,mode==1);
                    if(!Arrays.equals(x,expected))throw new IllegalStateException("Independent feature oracle differs");
                    error=Math.max(error,Math.abs(dot(weights,x)-caches[mode].evaluate(b)));
                    row.put(mode==0?"additive":"conditional",x);
                }
                writer.write(DataFiles.JSON.toJson(row).replace("\n","").replace("\r",""));writer.newLine();
            }
        }
        if(error>1e-12)throw new IllegalStateException("Pawn-cache value differs");
        for(int i=0;i<2;i++)refreshes[i]=caches[i].refreshes;
        for(int mode=0;mode<4;mode++)run(boards,mode/2==1,mode%2==1,weights,100000);
        long[][] times=new long[4][7];
        for(int pass=0;pass<7;pass++)for(int offset=0;offset<4;offset++) {
            int mode=(pass+offset)%4;long start=System.nanoTime();run(boards,mode/2==1,mode%2==1,weights,100000);times[mode][pass]=System.nanoTime()-start;
        }
        var latency=new ArrayList<Object>();
        for(int mode=0;mode<4;mode++) {
            long[] sorted=times[mode].clone();Arrays.sort(sorted);
            latency.add(Map.of("mode",(mode/2==1?"conditional":"additive")+(mode%2==1?"-cached":"-full"),
                    "medianNanos",sorted[3]/100000.0,"passNanos",times[mode],"evaluationsPerPass",100000));
        }
        DataFiles.write(output.resolve("summary.json"),Map.of("id","brn-pawn-condition-cost-feasibility-v1", "inputs",bindings,
                "positions",boards.size(),"maximumCacheError",error,"pawnStructureRefreshes",refreshes,"latency",latency,
                "parameters",24,"weights",weights,"limitations",List.of(
                    "Random weights; correction only, excludes existing evaluator, score rounding and search.",
                    "Played-game adjacency is not a recursive search transition trace; cache hit rates and costs need search integration measurement.",
                    "No training, checkpoint, architecture admission, GUI or playing-strength claim.")));
        System.out.println("Verified pawn-condition oracle/cache on "+boards.size()+" played positions: "+latency);
    }
    private BrnPawnConditionProbe() {}
}
