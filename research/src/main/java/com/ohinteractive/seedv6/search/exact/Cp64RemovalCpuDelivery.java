package com.ohinteractive.seedv6.search.exact;

import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;

/** Same owner-thread measurement as Cp64CpuDelivery, for the frozen H1 runtime. */
public final class Cp64RemovalCpuDelivery {
    public static void main(String[] args) throws Exception {
        if(args.length!=3) throw new IllegalArgumentException("model corpus NEW-output");
        var cpu=ManagementFactory.getThreadMXBean();
        if(!cpu.isCurrentThreadCpuTimeSupported()) throw new IllegalStateException("Thread CPU unavailable");
        if(!cpu.isThreadCpuTimeEnabled()) cpu.setThreadCpuTimeEnabled(true);
        var os=(com.sun.management.OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
        var model=ConventionalNnueEngine.load(Path.of(args[0]));
        var roots=P3HistoryStudy.roots(Path.of(args[1])).subList(0,12);
        var profiles=List.of("baseline","no-improving");
        var drivers=new SearchDriver[2];var gson=new GsonBuilder().create();
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            out.write(gson.toJson(Map.of("type","identity","networkSha256",ConventionalNnueEngine.SHA256,
                    "profiles",profiles.stream().map(p->Cp64SelectivityLab.configuration(p,1,64).identity()).toList(),
                    "threadId",Thread.currentThread().threadId(),"workerModel","Calling-thread OWNER")));
            out.newLine();out.flush();
            for(int p=0;p<2;p++) drivers[p]=new SearchDriver(Cp64SelectivityLab.configuration(profiles.get(p),1,64).create(model.evaluation()));
            for(int round=-1;round<2;round++) for(int i=0;i<12;i++) for(int offset=0;offset<2;offset++) {
                int p=Math.floorMod(round+i+offset,2);var driver=drivers[p];var root=roots.get(i);
                driver.newGame();long gc=P3HistoryStudy.gcCount();
                long jit=ManagementFactory.getCompilationMXBean().getTotalCompilationTime();
                long processStart=os.getProcessCpuTime(),threadStart=cpu.getCurrentThreadCpuTime(),start=System.nanoTime();
                var control=SearchControl.controlled(1000000,start,60_000_000_000L,TimeSource.SYSTEM);
                var result=driver.search(new SearchRequest(root.board(),root.history(),40,SearchObserver.NONE,control,false));
                long elapsed=System.nanoTime()-start,threadCpu=cpu.getCurrentThreadCpuTime()-threadStart;
                long processCpu=os.getProcessCpuTime()-processStart;
                if(threadCpu<0 || result.nodes()!=control.nodes() || result.nodes()>1000000
                        || control.termination()==SearchTermination.TIME_LIMIT || result.diagnostics().enabled()
                        || driver.activeSearchThreads()!=0) throw new AssertionError("Delivery/accounting/drain");
                if(result.lastCompletedResult()!=null) Phase2Oracle.legalPv(root.board(),result.lastCompletedResult().principalVariation());
                var row=new LinkedHashMap<String,Object>();row.put("type","request");row.put("id",root.id());
                row.put("round",round);row.put("profile",profiles.get(p));row.put("outcome",result);
                row.put("elapsedNanos",elapsed);row.put("ownerThreadCpuNanos",threadCpu);row.put("processCpuNanos",processCpu);
                row.put("ownerCpuToElapsed",(double)threadCpu/elapsed);row.put("termination",control.termination());
                row.put("gcCollections",P3HistoryStudy.gcCount()-gc);
                row.put("compilationMillis",ManagementFactory.getCompilationMXBean().getTotalCompilationTime()-jit);
                out.write(gson.toJson(row));out.newLine();out.flush();
            }
            out.write(gson.toJson(Map.of("status","passed")));out.newLine();
        } finally {for(var driver:drivers) if(driver!=null) driver.close();}
    }
    private Cp64RemovalCpuDelivery() {}
}
