package com.ohinteractive.seedv6.search.exact;

import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.LongAdder;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.core.brnpair2.ProductionModel;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.driver.MtdDriverResearch.Policy;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;

/** Rotated complete requests with optional separate pass/TT observations. No strength inference. */
public final class Sr004DriverStudy {
    static final class ObservedTable extends TTable {
        final LongAdder probes=new LongAdder(),hits=new LongAdder(),saves=new LongAdder(),hints=new LongAdder();
        ObservedTable(){super(64);}
        public boolean probe(long key,TEntry entry){probes.increment();boolean found=super.probe(key,entry);if(found)hits.increment();return found;}
        public void save(long key,int depth,int type,int score,long move){saves.increment();super.save(key,depth,type,score,move);}
        public long probeHint(long key){long move=super.probeHint(key);if(move!=0)hints.increment();return move;}
        void reset(){probes.reset();hits.reset();saves.reset();hints.reset();}
        Map<String,Long> snapshot(){return Map.of("probes",probes.sum(),"mechanicalHits",hits.sum(),"saveAttempts",saves.sum(),"hintHits",hints.sum());}
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=11)throw new IllegalArgumentException("model corpus output workers warmups rounds depth nodes roots policies trace");
        var e=SearchEvaluation.brnPair2(ProductionModel.load(Path.of(args[0])));
        int workers=Integer.parseInt(args[3]),warmups=Integer.parseInt(args[4]),rounds=Integer.parseInt(args[5]);
        int depth=Integer.parseInt(args[6]),count=Integer.parseInt(args[8]);long budget=Long.parseLong(args[7]);
        boolean trace=Boolean.parseBoolean(args[10]);
        var roots=P3HistoryStudy.roots(Path.of(args[1])).subList(0,count);
        var policies=Arrays.stream(args[9].split(",")).map(Policy::valueOf).toArray(Policy[]::new);
        var mx=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        var os=(com.sun.management.OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
        mx.setThreadAllocatedMemoryEnabled(true);mx.setThreadCpuTimeEnabled(true);
        var gson=new GsonBuilder().create();var attempts=new ArrayList<Object>();
        SearchDriver[] drivers=new SearchDriver[policies.length];ObservedTable[] tables=new ObservedTable[policies.length];
        long[] passes={0};
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            for(int i=0;i<policies.length;i++) {
                var table=trace?(tables[i]=new ObservedTable()):new TTable(64);
                var observer=(MtdDriverResearch.Observer)(d,p,a,b,l,u,r)->{
                    passes[0]++;if(trace)attempts.add(Map.of("depth",d,"phase",p,"alpha",a,"beta",b,"lowerEstimate",l,"upperEstimate",u,"result",r));
                };
                MtdDriverResearch facility;
                if(workers==1) facility=new MtdDriverResearch(ExactSearchAdapter.seeResearch(e,SeePolicy.QXRAY,null,table)
                        .withCompletedHints(true,null).withFlatSearchResearch(true),policies[i],observer);
                else facility=new MtdDriverResearch(new ParallelSearch(workers,e,table,ConcurrencyPolicy.ROOT_REUSE)
                        .withSeeResearch(SeePolicy.QXRAY,null).withCompletedHints(true,null).withFlatSearchResearch(true),policies[i],observer);
                drivers[i]=new SearchDriver(facility);
            }
            out.write(gson.toJson(Map.of("type","identity","java",System.getProperty("java.runtime.version"),
                    "jvm",ManagementFactory.getRuntimeMXBean().getInputArguments(),"workers",workers,"hashMiB",64,"arguments",args)));
            out.newLine();out.flush();
            for(int round=-warmups;round<rounds;round++) for(int index=0;index<roots.size();index++) {
                var root=roots.get(index);
                for(int offset=0;offset<policies.length;offset++) {
                    int arm=Math.floorMod(round+index+offset,policies.length);var driver=drivers[arm];
                    driver.newGame();attempts.clear();passes[0]=0;if(trace)tables[arm].reset();
                    long[] ids=mx.getAllThreadIds();long alloc=allocation(mx,ids),gc=P3HistoryStudy.gcCount();
                    long compiled=ManagementFactory.getCompilationMXBean().getTotalCompilationTime();
                    long cpu=os.getProcessCpuTime(),start=System.nanoTime();
                    var control=SearchControl.controlled(budget,start,60_000_000_000L,TimeSource.SYSTEM);
                    var result=driver.search(new SearchRequest(root.board(),root.history(),depth,SearchObserver.NONE,control,true));
                    long wall=System.nanoTime()-start,usedCpu=os.getProcessCpuTime()-cpu,bytes=allocation(mx,ids)-alloc;
                    if(result.nodes()!=result.diagnostics().totalEnteredNodes() || result.nodes()>budget)throw new AssertionError("All-pass accounting");
                    if(control.termination()==SearchTermination.TIME_LIMIT)throw new AssertionError("Safety deadline");
                    if(result.lastCompletedResult()!=null)Phase2Oracle.legalPv(root.board(),result.lastCompletedResult().principalVariation());
                    if(driver.activeSearchThreads()!=0)throw new AssertionError("Undrained pass");
                    var row=new LinkedHashMap<String,Object>();
                    row.put("type","request");row.put("round",round);row.put("id",root.id());row.put("policy",policies[arm]);
                    row.put("elapsedNanos",wall);row.put("processCpuNanos",usedCpu);row.put("allocatedBytes",bytes);
                    row.put("compilationMillis",ManagementFactory.getCompilationMXBean().getTotalCompilationTime()-compiled);
                    row.put("gcCollections",P3HistoryStudy.gcCount()-gc);row.put("heapUsed",ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
                    row.put("passes",passes[0]);row.put("outcome",result);row.put("termination",control.termination());
                    if(trace){row.put("attempts",attempts);row.put("tt",tables[arm].snapshot());}
                    out.write(gson.toJson(row));out.newLine();out.flush();
                }
            }
            out.write(gson.toJson(Map.of("status","passed","measuredRequests",rounds*count*policies.length)));out.newLine();
        } finally {for(var d:drivers)if(d!=null)d.close();}
    }
    private static long allocation(com.sun.management.ThreadMXBean mx,long[] ids) {
        return Arrays.stream(mx.getThreadAllocatedBytes(ids)).filter(n->n>=0).sum();
    }
    private Sr004DriverStudy(){}
}
