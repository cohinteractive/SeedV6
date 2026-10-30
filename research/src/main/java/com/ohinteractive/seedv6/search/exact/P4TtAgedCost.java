package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.lang.management.ManagementFactory;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.ApplicationVersion;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.manage.ClockPolicy;

/** Clean integrated H1/H2 costs across consecutive real requests. No TT observer,
 * engine counters, strength inference or changes to the frozen engine artifact. */
public final class P4TtAgedCost {
    public static void main(String[] args) throws Exception {
        if(args.length!=5)throw new IllegalArgumentException("model corpus output workers measuredRounds");
        var model=ConventionalNnueEngine.load(Path.of(args[0]));var roots=P3HistoryStudy.roots(Path.of(args[1]));
        int workers=Integer.parseInt(args[3]),rounds=Integer.parseInt(args[4]);
        if((workers!=1&&workers!=4)||rounds!=2)throw new IllegalArgumentException("Frozen diagnostic bounds");
        var groups=new LinkedHashMap<String,List<P3HistoryStudy.Root>>();
        for(var root:roots)groups.computeIfAbsent(root.id().split(":")[0],ignored->new ArrayList<>()).add(root);
        var os=(com.sun.management.OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
        var gson=new GsonBuilder().create();int count=0;
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            out.write(gson.toJson(Map.of("type","identity","source",ApplicationVersion.load().sourceRevision(),
                    "build",ApplicationVersion.load().build(),"workers",workers,"modelSha256",ConventionalNnueEngine.SHA256,
                    "mapping",ConventionalNnueEngine.SCORE_MAPPING,"head",model.model().headBackend(),
                    "measurement","clean integrated persistent H1/H2 fixed-node cost; no strength inference")));
            out.newLine();out.flush();
            for(int round=-1;round<rounds;round++) {
                int streamIndex=0;
                for(var group:groups.entrySet()) {
                    SearchDriver[] drivers=new SearchDriver[2];
                    try {
                        for(int side=0;side<2;side++) {
                            var base=BaselineEngine.configuration(workers,64);
                            var flags=base.mechanisms().with(FlatSearchConfig.Feature.HEURISTIC_TT_MEMORY,true)
                                    .with(FlatSearchConfig.Feature.AGED_HEURISTIC_TT_MEMORY,side==1);
                            drivers[side]=new SearchDriver(ClockPolicy.BOUNDARY2.apply(
                                    new ConstructionSearch(flags,workers,64,base.aspiration()).create(model.evaluation())));
                        }
                        int index=0;
                        for(var root:group.getValue()) {
                            for(int offset=0;offset<2;offset++) {
                                int side=Math.floorMod(round+streamIndex+index+offset,2);var driver=drivers[side];
                                var control=SearchControl.controlled(100000,System.nanoTime(),30_000_000_000L,TimeSource.SYSTEM);
                                long cpu=os.getProcessCpuTime(),started=System.nanoTime();
                                var result=driver.search(new SearchRequest(root.board(),root.history(),40,control));
                                long elapsed=System.nanoTime()-started;cpu=os.getProcessCpuTime()-cpu;
                                if(result.nodes()!=100000 || control.nodes()!=result.nodes() || driver.activeSearchThreads()!=0)
                                    throw new AssertionError("Node/accounting/drain qualification");
                                if(result.lastCompletedResult()!=null)Phase2Oracle.legalPv(root.board(),result.lastCompletedResult().principalVariation());
                                var row=new LinkedHashMap<String,Object>();row.put("type","request");row.put("profile",side==0?"h1":"h2");
                                row.put("root",root.id());row.put("round",round);row.put("workers",workers);row.put("order",offset);
                                row.put("elapsedNanos",elapsed);row.put("processCpuNanos",cpu);row.put("outcome",result);
                                out.write(gson.toJson(row));out.newLine();out.flush();count++;
                            }
                            index++;
                        }
                    }finally {for(var driver:drivers)if(driver!=null)driver.close();}
                    streamIndex++;
                }
                System.out.println("completed round "+round+" workers="+workers);
            }
            out.write(gson.toJson(Map.of("status","passed","requests",count)));out.newLine();
        }
    }
    private P4TtAgedCost() {}
}
