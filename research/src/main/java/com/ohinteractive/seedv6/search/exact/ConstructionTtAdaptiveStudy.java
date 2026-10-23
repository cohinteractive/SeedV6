package com.ohinteractive.seedv6.search.exact;

import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;

/** One existing adaptive consumer control; measured worker diagnostics are OFF. */
public final class ConstructionTtAdaptiveStudy {
    static ConstructionSearch configuration(String profile,int workers) {
        return ConstructionSearch.v1(workers,64,profile.equals("baseline")
                ?AspirationSearch.Policy.WIDEN16:AspirationSearch.Policy.ADAPTIVE16);
    }
    static Map<String,Object> request(SearchDriver driver,P3HistoryStudy.Root root,int depth,long budget) {
        var os=(com.sun.management.OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
        long reset=System.nanoTime();driver.newGame();reset=System.nanoTime()-reset;
        long cpu=os.getProcessCpuTime(),gc=P3HistoryStudy.gcCount();
        long jit=ManagementFactory.getCompilationMXBean().getTotalCompilationTime(),start=System.nanoTime();
        var control=SearchControl.controlled(budget,start,60_000_000_000L,TimeSource.SYSTEM);
        var result=driver.search(new SearchRequest(root.board(),root.history(),depth,SearchObserver.NONE,control,false));
        long elapsed=System.nanoTime()-start,usedCpu=os.getProcessCpuTime()-cpu;
        if(control.termination()==SearchTermination.TIME_LIMIT || result.nodes()!=control.nodes()
                || result.nodes()>budget || result.diagnostics().enabled() || driver.activeSearchThreads()!=0)
            throw new AssertionError("Request accounting/diagnostics/drain");
        if(result.lastCompletedResult()!=null) Phase2Oracle.legalPv(root.board(),result.lastCompletedResult().principalVariation());
        var value=new LinkedHashMap<String,Object>();value.put("outcome",result);value.put("termination",control.termination());
        value.put("elapsedNanos",elapsed);value.put("processCpuNanos",usedCpu);value.put("resetNanos",reset);
        value.put("gcCollections",P3HistoryStudy.gcCount()-gc);
        value.put("compilationMillis",ManagementFactory.getCompilationMXBean().getTotalCompilationTime()-jit);
        value.put("heapUsed",ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());return value;
    }
    /** model corpus new-output workers warmups rounds depth budget roots */
    public static void main(String[] args) throws Exception {
        if(args.length!=9) throw new IllegalArgumentException("model corpus new-output workers warmups rounds depth budget roots");
        var model=ConventionalNnueEngine.load(Path.of(args[0]));var all=P3HistoryStudy.roots(Path.of(args[1]));
        int workers=Integer.parseInt(args[3]),warmups=Integer.parseInt(args[4]),rounds=Integer.parseInt(args[5]);
        int depth=Integer.parseInt(args[6]),count=Integer.parseInt(args[8]);long budget=Long.parseLong(args[7]);
        if(warmups<0 || rounds<1 || count<1 || count>all.size() || depth<1 || depth>ExactSearch.MAX_DEPTH || budget<1)
            throw new IllegalArgumentException("Bounds");
        String[] names={"baseline","adaptive"};var drivers=new SearchDriver[2];var gson=new GsonBuilder().create();
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            out.write(gson.toJson(Map.of("type","identity","arguments",args,"networkSha256",ConventionalNnueEngine.SHA256,
                "diagnostics",false,"baseline",configuration(names[0],workers).identity(),"candidate",configuration(names[1],workers).identity())));
            out.newLine();out.flush();
            for(int p=0;p<2;p++) drivers[p]=new SearchDriver(configuration(names[p],workers).create(model.evaluation()));
            for(int round=-warmups;round<rounds;round++) for(int i=0;i<count;i++) for(int order=0;order<2;order++) {
                int profile=Math.floorMod(round+i+order,2);var root=all.get(i);
                var value=request(drivers[profile],root,depth,budget);value.put("type","request");
                value.put("id",root.id());value.put("round",round);value.put("profile",names[profile]);
                out.write(gson.toJson(value));out.newLine();out.flush();
            }
            out.write(gson.toJson(Map.of("status","passed")));out.newLine();
        } finally {for(var driver:drivers) if(driver!=null) driver.close();}
    }
    private ConstructionTtAdaptiveStudy() {}
}
