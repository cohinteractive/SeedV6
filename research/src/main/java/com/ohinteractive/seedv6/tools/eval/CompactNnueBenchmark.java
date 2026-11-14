package com.ohinteractive.seedv6.tools.eval;

import com.google.gson.Gson;
import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.core.nnue.conventional.*;
import java.nio.file.Path;
import java.lang.management.ManagementFactory;
import java.util.*;

/** Same transitions for each model; update/head timings separated, no Elo inference. */
public final class CompactNnueBenchmark {
    private static volatile long sink;
    public static void main(String[] args)throws Exception {
        var model=CompactNetwork.load(Path.of(args[0]));var boards=new ArrayList<long[]>();
        var random=new SplittableRandom(873126);long[] moves=new long[256],scratch=new long[6];
        // The same pawn-rich-to-sparse legal walk used by the earlier benchmark.
        boards.add(Board.startingPosition());
        for(int i=0;i<255;i++) {
            var b=boards.getLast();int n=Gen.genAll(b[0],b[1],b[2],b[3],(int)b[4],b[5],true,moves,scratch);
            if(n==0)break;long[] child=new long[6];Board.makeMoveInto(b[0],b[1],b[2],b[3],(int)b[4],b[5],moves[random.nextInt(n)],child);boards.add(child);
        }
        var parents=boards.stream().map(b->{var s=model.newAccumulator();s.rebuild(b);return s;}).toList();
        var child=model.newAccumulator();int n=boards.size()-1;int pawnChanges=0,refreshes=0;
        for(int j=0;j<n;j++) {
            var before=boards.get(j);var after=boards.get(j+1);child.update(before,after,parents.get(j));
            if(child.evaluate(after)!=model.fullRebuild(after))throw new AssertionError("Incremental parity "+j);
            for(int side=0;side<2;side++)for(int unit=0;unit<128;unit++)if(child.raw(side,unit)!=parents.get(j+1).raw(side,unit))throw new AssertionError("Raw parity");
            long p=Phase2Features.pawns(before),q=Phase2Features.pawns(after);
            if(p!=q||((before[3]^after[3])&(p|q))!=0)pawnChanges++;
            long oldKings=before[0]&~before[1]&~before[2],newKings=after[0]&~after[1]&~after[2];
            for(int side=0;side<2;side++){
                int a=Long.numberOfTrailingZeros(oldKings&(side==0?~before[3]:before[3]));
                int b=Long.numberOfTrailingZeros(newKings&(side==0?~after[3]:after[3]));
                if(ConventionalFeatures.context(side,a)!=ConventionalFeatures.context(side,b))refreshes++;
            }
        }
        int iterations=100000;
        Runnable update=()->{long s=0;for(int i=0;i<iterations;i++){int j=i%n;child.update(boards.get(j),boards.get(j+1),parents.get(j));s+=child.raw(0,0);}sink=s;};
        Runnable head=()->{long s=0;for(int i=0;i<iterations;i++){int j=i%n;s+=parents.get(j).evaluate(boards.get(j));}sink=s;};
        Runnable both=()->{long s=0;for(int i=0;i<iterations;i++){int j=i%n;child.update(boards.get(j),boards.get(j+1),parents.get(j));s+=child.evaluate(boards.get(j+1));}sink=s;};
        Runnable refresh=()->{long s=0;for(int i=0;i<iterations;i++){int j=i%n;child.rebuild(boards.get(j));s+=child.raw(0,0);}sink=s;};
        var operations=new LinkedHashMap<String,Runnable>();operations.put("update",update);operations.put("head",head);operations.put("update_plus_head",both);operations.put("refresh",refresh);
        var result=new LinkedHashMap<String,Object>();result.put("architecture",model.architecture());result.put("payload",model.payloadSha256());result.put("backend",model.backend());
        result.put("transitions",n);result.put("pawn_transitions",pawnChanges);result.put("perspective_refreshes",refreshes);result.put("incremental_parity","PASS");
        for(var entry:operations.entrySet()){
            var times=new ArrayList<Double>();for(int repeat=-5;repeat<5;repeat++){
                long start=System.nanoTime();entry.getValue().run();double cost=(System.nanoTime()-start)/(double)iterations;
                if(repeat>=0)times.add(cost);
            }result.put(entry.getKey()+"_ns",times);
        }
        if(ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean bean&&bean.isThreadAllocatedMemorySupported()){
            bean.setThreadAllocatedMemoryEnabled(true);long id=Thread.currentThread().threadId(),before=bean.getThreadAllocatedBytes(id);both.run();result.put("allocated_bytes_100000",bean.getThreadAllocatedBytes(id)-before);
        }
        result.put("limitation","255-transition deterministic walk; cache/branch counters not collected. Search probe and equal-time games are separate.");
        System.out.println(new Gson().toJson(result));
    }
}
