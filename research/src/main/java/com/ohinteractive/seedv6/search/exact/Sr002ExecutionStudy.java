package com.ohinteractive.seedv6.search.exact;

import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.core.brnpair2.ProductionModel;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;

/** SR-002 complete iterative requests; no playing-strength inference. */
public final class Sr002ExecutionStudy {
    public static void main(String[] args) throws Exception {
        if(args.length!=9 && args.length!=10) throw new IllegalArgumentException(
                "model corpus output workers warmups rounds depth node-limit roots [recursive|flat|both|full|full-both]");
        var evaluation=SearchEvaluation.brnPair2(ProductionModel.load(Path.of(args[0])));
        var all=P3HistoryStudy.roots(Path.of(args[1]));
        int workers=Integer.parseInt(args[3]),warmups=Integer.parseInt(args[4]),rounds=Integer.parseInt(args[5]);
        int depth=Integer.parseInt(args[6]),limit=Integer.parseInt(args[8]);
        long budget=Long.parseLong(args[7]);
        if(warmups<0 || rounds<1 || limit<1 || limit>all.size() || budget<1)
            throw new IllegalArgumentException("Invalid sampling bounds");
        var roots=all.subList(0,limit);
        var mx=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        var os=(com.sun.management.OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
        if(!mx.isThreadAllocatedMemorySupported() || !mx.isThreadCpuTimeSupported())
            throw new IllegalStateException("CPU/allocation counters unavailable");
        mx.setThreadAllocatedMemoryEnabled(true);mx.setThreadCpuTimeEnabled(true);
        var gson=new GsonBuilder().create();
        String selection=args.length==10?args[9]:"recursive";
        int[] modes=switch(selection) {case "recursive"->new int[]{0};case "flat"->new int[]{1};
            case "both"->new int[]{0,1};case "full"->new int[]{2};case "full-both"->new int[]{0,2};
            default->throw new IllegalArgumentException("Invalid mode");};
        SearchDriver[] drivers=new SearchDriver[modes.length];
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            for(int i=0;i<modes.length;i++) drivers[i]=new SearchDriver(engine(workers,evaluation,modes[i]));
            out.write(gson.toJson(Map.of("type","identity","java",System.getProperty("java.runtime.version"),
                    "vm",System.getProperty("java.vm.name"),"jvm",ManagementFactory.getRuntimeMXBean().getInputArguments(),
                    "workers",workers,"hashMiB",64,"configuration","Policy4-"+selection,
                    "arguments",args)));out.newLine();out.flush();
            for(int round=-warmups;round<rounds;round++) for(var root:roots) {
              String expected=null;
              for(int offset=0;offset<modes.length;offset++) {
                int index=Math.floorMod(round+roots.indexOf(root)+offset,modes.length),mode=modes[index];
                var driver=drivers[index];
                long reset=System.nanoTime();driver.newGame();reset=System.nanoTime()-reset;
                long[] ids=mx.getAllThreadIds();
                long allocated=allocation(mx,ids),gc=P3HistoryStudy.gcCount();
                long compiled=ManagementFactory.getCompilationMXBean().getTotalCompilationTime();
                long cpu=os.getProcessCpuTime(),threadCpu=mx.getCurrentThreadCpuTime();
                long start=System.nanoTime();
                var control=SearchControl.controlled(budget,start,60_000_000_000L,TimeSource.SYSTEM);
                var result=driver.search(new SearchRequest(root.board(),root.history(),depth,SearchObserver.NONE,control,true));
                long elapsed=System.nanoTime()-start,usedCpu=os.getProcessCpuTime()-cpu;
                long ownerCpu=mx.getCurrentThreadCpuTime()-threadCpu,bytes=allocation(mx,ids)-allocated;
                if(result.nodes()!=result.diagnostics().totalEnteredNodes() || result.nodes()>budget)
                    throw new AssertionError("Request accounting mismatch");
                if(control.termination()==SearchTermination.TIME_LIMIT) throw new AssertionError("Safety deadline");
                if(result.lastCompletedResult()!=null) Phase2Oracle.legalPv(root.board(),result.lastCompletedResult().principalVariation());
                if(driver.activeSearchThreads()!=0) throw new AssertionError("Undrained workers");
                if(workers==1) {
                    String actual=gson.toJson(result);
                    if(expected!=null && !expected.equals(actual)) throw new AssertionError("Traversal mismatch: "+root.id());
                    expected=actual;
                }
                var row=new LinkedHashMap<String,Object>();
                row.put("type","request");row.put("round",round);row.put("id",root.id());
                row.put("mode",mode);
                row.put("elapsedNanos",elapsed);row.put("processCpuNanos",usedCpu);row.put("ownerCpuNanos",ownerCpu);
                row.put("liveThreadAllocatedBytes",bytes);row.put("allocationThreadCount",ids.length);
                row.put("newGameNanos",reset);row.put("gcCollections",P3HistoryStudy.gcCount()-gc);
                row.put("compilationMillis",ManagementFactory.getCompilationMXBean().getTotalCompilationTime()-compiled);
                row.put("heapUsed",ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
                row.put("outcome",result);row.put("termination",control.termination());
                out.write(gson.toJson(row));out.newLine();out.flush();
              }
            }
            out.write(gson.toJson(Map.of("status","passed","measuredRequests",rounds*roots.size()*modes.length)));out.newLine();
        } finally {for(var driver:drivers) if(driver!=null) driver.close();}
    }

    static SingleDepthSearch engine(int workers,SearchEvaluation evaluation,boolean flat) {
        return engine(workers,evaluation,flat?1:0);
    }
    static SingleDepthSearch engine(int workers,SearchEvaluation evaluation,int mode) {
        var engine=ProductionSearch.create(workers,evaluation);
        if(mode==2) {
            if(engine instanceof ExactSearchAdapter owner) owner.withFlatSearchResearch(true);
            else ((ParallelSearch)engine).withFlatSearchResearch(true);
        } else if(mode==1) {
            if(engine instanceof ExactSearchAdapter owner) owner.withFlatQuiescenceResearch(true);
            else ((ParallelSearch)engine).withFlatQuiescenceResearch(true);
        }
        return engine;
    }

    // Counts already-live threads. First request may create helper threads; warmup
    // records are excluded, and process CPU still includes all helpers/JIT/GC.
    private static long allocation(com.sun.management.ThreadMXBean mx,long[] ids) {
        return Arrays.stream(mx.getThreadAllocatedBytes(ids)).filter(n->n>=0).sum();
    }
    private Sr002ExecutionStudy() {}
}
