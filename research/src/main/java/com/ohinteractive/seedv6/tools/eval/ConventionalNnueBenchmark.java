package com.ohinteractive.seedv6.tools.eval;

import com.google.gson.Gson;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.brnpair2.ProductionModel;
import com.ohinteractive.seedv6.core.nnue.conventional.ConventionalNetwork;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.SplittableRandom;

/** Single-thread warmed primitive inference costs, never an engine-strength proxy. */
public final class ConventionalNnueBenchmark {
    private static volatile long sink;
    public static void main(String[] args) throws Exception {
        long start=System.nanoTime();var net=ConventionalNetwork.load(Path.of(args[0]));
        double loadMs=(System.nanoTime()-start)/1e6;
        var boards=new ArrayList<long[]>();boards.add(Board.startingPosition());
        var random=new SplittableRandom(873126);long[] moves=new long[256],scratch=new long[6];
        for(int i=0;i<255;i++) {
            long[] b=boards.getLast();int n=Gen.genAll(b[0],b[1],b[2],b[3],(int)b[4],b[5],true,moves,scratch);
            if(n==0)break;
            long[] next=new long[6];Board.makeMoveInto(b[0],b[1],b[2],b[3],(int)b[4],b[5],moves[random.nextInt(n)],next);
            boards.add(next);
        }
        // Cache each parent, as a search stack does; timed operations include copying
        // parent sums, updating all changed features and the complete output head.
        var parents=boards.stream().map(b->{var a=net.newAccumulator();a.rebuild(b);return a;}).toList();
        var child=net.newAccumulator();var full=net.newAccumulator();
        var pair=ProductionModel.bundled().newWorkspace();int transitions=boards.size()-1;
        Runnable incremental=()->{
            long sum=0;for(int i=0;i<20000;i++) {int j=i%transitions;child.update(boards.get(j),boards.get(j+1),parents.get(j));sum+=child.evaluate(boards.get(j+1));}sink=sum;
        };
        Runnable rebuild=()->{
            long sum=0;for(int i=0;i<20000;i++) {var b=boards.get(i%transitions+1);full.rebuild(b);sum+=full.evaluate(b);}sink=sum;
        };
        Runnable baseline=()->{
            double sum=0;for(int i=0;i<20000;i++)sum+=pair.evaluatePawns(boards.get(i%transitions+1),.25);sink=(long)sum;
        };
        for(int i=0;i<5;i++){incremental.run();rebuild.run();baseline.run();}
        var result=new LinkedHashMap<String,Object>();result.put("network",net.payloadSha256());
        result.put("load_ms",loadMs);result.put("legal_transitions",transitions);
        for(var entry:java.util.Map.of("incremental_plus_head",incremental,"full_refresh_plus_head",rebuild,"pair2_rolling_plus_head",baseline).entrySet()) {
            var times=new ArrayList<Double>();
            for(int repeat=0;repeat<3;repeat++){start=System.nanoTime();entry.getValue().run();times.add((System.nanoTime()-start)/20000.0);}
            result.put(entry.getKey()+"_ns_per_eval",times);
        }
        if(ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean bean && bean.isThreadAllocatedMemorySupported()) {
            bean.setThreadAllocatedMemoryEnabled(true);long id=Thread.currentThread().threadId();
            long before=bean.getThreadAllocatedBytes(id);incremental.run();
            result.put("incremental_allocated_bytes_20000",bean.getThreadAllocatedBytes(id)-before);
        }
        result.put("limitation","one deterministic legal walk; microbenchmark only; no NPS/Elo inference");
        System.out.println(new Gson().toJson(result));
    }
}
