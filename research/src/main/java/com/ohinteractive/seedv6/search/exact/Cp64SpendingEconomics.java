package com.ohinteractive.seedv6.search.exact;

import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;

/** Additional-spending W1 actual-clock economics and fixed-node CPU-delivery qualification.
 * Clock input is the base allocation; candidate hard caps are twice that allocation.
 * Reserve clipping and UCI modes are separately exercised by real protocol qualification. */
public final class Cp64SpendingEconomics {
    public static void main(String[] args) throws Exception {
        if(args.length!=4 || !Set.of("clock","delivery").contains(args[3]))
            throw new IllegalArgumentException("model corpus NEW-output clock|delivery");
        boolean clock=args[3].equals("clock");
        var cpu=ManagementFactory.getThreadMXBean();
        if(!cpu.isCurrentThreadCpuTimeSupported())throw new IllegalStateException("CPU unavailable");
        cpu.setThreadCpuTimeEnabled(true);
        var os=(com.sun.management.OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
        var model=ConventionalNnueEngine.load(Path.of(args[0]));
        var roots=P3HistoryStudy.roots(Path.of(args[1])).subList(0,12);
        var profiles=List.of("baseline","boundary2","unstable2");var drivers=new SearchDriver[3];
        var gson=new GsonBuilder().create();
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            out.write(gson.toJson(Map.of("type","identity","mode",args[3],"profiles",profiles,
                "configuration",BaselineEngine.configuration(model.evaluation(),1,64).identity(),
                "networkSha256",ConventionalNnueEngine.SHA256,"jvm",ManagementFactory.getRuntimeMXBean().getInputArguments())));
            out.newLine();out.flush();
            for(int p=0;p<3;p++) {
                var owner=BaselineEngine.create(model.evaluation(),1,64);
                drivers[p]=new SearchDriver(p==0?owner:new Cp64SpendingPolicy(owner,p==2));
            }
            for(int round=-1;round<2;round++) for(int i=0;i<12;i++)
                for(long budgetMillis:clock && round>=0?new long[]{50,100,400}:new long[]{-1})
                for(int offset=0;offset<3;offset++) {
                    int p=Math.floorMod(round+i+offset,3);var driver=drivers[p];var root=roots.get(i);
                    driver.newGame();long gc=P3HistoryStudy.gcCount();
                    long jit=ManagementFactory.getCompilationMXBean().getTotalCompilationTime();
                    long processStart=os.getProcessCpuTime(),threadStart=cpu.getCurrentThreadCpuTime(),start=System.nanoTime();
                    long nodeLimit=budgetMillis<0?1000000:-1;
                    long hardMillis=budgetMillis<0?-1:budgetMillis*(p==0?1:2);
                    var control=SearchControl.controlled(nodeLimit,start,budgetMillis<0?60_000_000_000L:hardMillis*1000000,
                            TimeSource.SYSTEM,budgetMillis>=0);
                    var result=driver.search(new SearchRequest(root.board(),root.history(),40,SearchObserver.NONE,control,false));
                    long elapsed=System.nanoTime()-start,threadCpu=cpu.getCurrentThreadCpuTime()-threadStart;
                    long processCpu=os.getProcessCpuTime()-processStart;
                    if(threadCpu<0 || result.nodes()!=control.nodes() || (nodeLimit>=0 && result.nodes()>nodeLimit)
                            || (budgetMillis<0 && control.termination()==SearchTermination.TIME_LIMIT)
                            || result.diagnostics().enabled() || driver.activeSearchThreads()!=0)
                        throw new AssertionError("Delivery/accounting/drain");
                    if(result.lastCompletedResult()!=null)Phase2Oracle.legalPv(root.board(),result.lastCompletedResult().principalVariation());
                    var row=new LinkedHashMap<String,Object>();row.put("type","request");row.put("id",root.id());
                    row.put("round",round);row.put("profile",profiles.get(p));row.put("budgetMillis",budgetMillis);row.put("hardBudgetMillis",hardMillis);
                    row.put("outcome",result);row.put("termination",control.termination());
                    row.put("elapsedNanos",elapsed);row.put("ownerThreadCpuNanos",threadCpu);row.put("processCpuNanos",processCpu);
                    row.put("ownerCpuToElapsed",(double)threadCpu/elapsed);row.put("gcCollections",P3HistoryStudy.gcCount()-gc);
                    row.put("compilationMillis",ManagementFactory.getCompilationMXBean().getTotalCompilationTime()-jit);
                    row.put("heapUsed",ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
                    out.write(gson.toJson(row));out.newLine();out.flush();
                }
            out.write(gson.toJson(Map.of("status","passed")));out.newLine();
        } finally {for(var driver:drivers)if(driver!=null)driver.close();}
    }
    private Cp64SpendingEconomics() {}
}
