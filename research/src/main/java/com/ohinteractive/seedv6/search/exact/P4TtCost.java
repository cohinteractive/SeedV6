package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.lang.management.ManagementFactory;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.manage.ClockPolicy;

/** Clean request-cost diagnostic using frozen engine artifacts; never strength evidence. */
public final class P4TtCost {
    public static void main(String[] args) throws Exception {
        if(args.length!=5)throw new IllegalArgumentException("CP64 roots.tsv new-output.jsonl workers rounds");
        var model=ConventionalNnueEngine.load(Path.of(args[0]));var roots=P3HistoryStudy.roots(Path.of(args[1]));
        int workers=Integer.parseInt(args[3]),rounds=Integer.parseInt(args[4]);
        if(rounds<1 || rounds>10)throw new IllegalArgumentException("Bounded 1..10 rounds required");
        var gson=new GsonBuilder().create();var os=(com.sun.management.OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
        SearchDriver[] drivers=new SearchDriver[2];
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            for(int i=0;i<2;i++) {
                var base=BaselineEngine.configuration(workers,64);
                var flags=i==0?base.mechanisms():base.mechanisms().with(FlatSearchConfig.Feature.HEURISTIC_TT_MEMORY,true);
                drivers[i]=new SearchDriver(ClockPolicy.BOUNDARY2.apply(new ConstructionSearch(flags,workers,64,base.aspiration()).create(model.evaluation())));
            }
            // One complete rotated pass is warmup, retained but excluded from measurement.
            for(int round=-1;round<rounds;round++)for(int index=0;index<roots.size();index++)for(int offset=0;offset<2;offset++) {
                int side=Math.floorMod(round+index+offset,2);var driver=drivers[side];var root=roots.get(index);
                long reset=System.nanoTime();driver.newGame();reset=System.nanoTime()-reset;
                var control=SearchControl.controlled(100000,System.nanoTime(),30_000_000_000L,TimeSource.SYSTEM);
                long cpu=os.getProcessCpuTime(),started=System.nanoTime();
                var result=driver.search(new SearchRequest(root.board(),root.history(),40,control));
                long elapsed=System.nanoTime()-started;cpu=os.getProcessCpuTime()-cpu;
                if(result.nodes()!=100000 || control.nodes()!=result.nodes() || driver.activeSearchThreads()!=0)
                    throw new IllegalStateException("Accounting/drain qualification");
                if(result.lastCompletedResult()!=null)Phase2Oracle.legalPv(root.board(),result.lastCompletedResult().principalVariation());
                var row=new LinkedHashMap<String,Object>();row.put("profile",side==0?"candidate-source-production-policy":"h1");
                row.put("root",root.id());row.put("round",round);row.put("workers",workers);row.put("resetNanos",reset);
                row.put("elapsedNanos",elapsed);row.put("processCpuNanos",cpu);row.put("outcome",result);
                row.put("interpretation","uninstrumented engine fixed-node diagnostic; not a strength comparison");
                out.write(gson.toJson(row));out.newLine();out.flush();
            }
            out.write(gson.toJson(Map.of("status","passed")));out.newLine();
        }finally {for(var driver:drivers)if(driver!=null)driver.close();}
    }
    private P4TtCost() {}
}
