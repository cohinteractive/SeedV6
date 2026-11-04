package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.ApplicationVersion;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.manage.ClockPolicy;

/** Bounded independent-iteration/score-consumption study. Provenance monitor
 * changes scheduling; neither elapsed time nor depth here establishes strength. */
public final class P4IndependentStudy {
    enum Schedule { ROOT_REUSE, INDEPENDENT }
    static IterativeSearch owner(Schedule schedule,int workers,ConventionalNnueEngine model,
            P4MultiworkerMemoryStudy.ScoreTable table,P4MultiworkerStudy.Admissions[] counts) {
        if(schedule==Schedule.ROOT_REUSE) return new SearchDriver(ClockPolicy.BOUNDARY2.apply(new AspirationSearch(
                P4MultiworkerMemoryStudy.owner(P4MultiworkerMemoryStudy.Schedule.ROOT_REUSE,workers,model,table,counts),
                AspirationSearch.Policy.ADAPTIVE16)));
        var config=BaselineEngine.configuration(workers,64,BaselineEngine.SearchProfile.TC_P1).mechanisms()
                .with(FlatSearchConfig.Feature.HEURISTIC_TT_MEMORY,
                        table.delivery!=P4MultiworkerMemoryStudy.Delivery.HINTS_ONLY);
        return new IndependentSearch(workers,model.evaluation(),table,config,AspirationSearch.Policy.ADAPTIVE16,
                ClockPolicy.BOUNDARY2,id -> {
                    var delegate=ExactEvaluator.from(model.evaluation());
                    return counts[id]=new P4MultiworkerStudy.Admissions(new ExactEvaluator() {
                        @Override public void initialize(long[] b) { table.bind(id);delegate.initialize(b); }
                        @Override public void child(long[] p,long[] c,int ply,int qply) { delegate.child(p,c,ply,qply); }
                        @Override public void child(long[] p,long[] c,int ply) { delegate.child(p,c,ply); }
                        @Override public void reenter(int ply) { delegate.reenter(ply); }
                        @Override public int evaluate(long[] b,int ply) { return delegate.evaluate(b,ply); }
                    });
                });
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=9) throw new IllegalArgumentException("model corpus output workers rounds depth nodes schedule delivery");
        int workers=Integer.parseInt(args[3]),rounds=Integer.parseInt(args[4]),depth=Integer.parseInt(args[5]);
        long cap=Long.parseLong(args[6]);var schedule=Schedule.valueOf(args[7]);
        var delivery=P4MultiworkerMemoryStudy.Delivery.valueOf(args[8]);
        if(workers<2 || workers>4 || rounds<1 || rounds>4 || depth<3 || depth>16 || cap<1 || cap>1_000_000)
            throw new IllegalArgumentException("Unbounded diagnostic");
        var model=ConventionalNnueEngine.load(Path.of(args[0]));var roots=P3HistoryStudy.roots(Path.of(args[1]));
        if(roots.isEmpty() || roots.size()>64) throw new IllegalArgumentException("Root count");
        long nodes=0;int requests=0;var gson=new GsonBuilder().create();
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            out.write(gson.toJson(Map.of("type","identity","build",ApplicationVersion.load().build(),
                    "source",ApplicationVersion.load().sourceRevision(),"args",args,"schedule",schedule,"delivery",delivery,
                    "scope","Independent completed owner iterations, global admissions, actual score consumption; no speed/strength claim")));
            out.newLine();out.flush();
            for(int round=0;round<rounds;round++) for(var root:roots) {
                var table=new P4MultiworkerMemoryStudy.ScoreTable(workers,64,delivery);
                var counts=new P4MultiworkerStudy.Admissions[workers];var saved=root.board().clone();
                try(var driver=owner(schedule,workers,model,table,counts)) {
                    var measurement=ConstructionTtStudy.request(driver,root,depth,cap);
                    var result=(SearchDriverOutcome)measurement.get("outcome");
                    if(result.nodes()!=Arrays.stream(counts).mapToLong(a->a.nodes).sum()
                            || !Arrays.equals(saved,root.board()) || driver.activeSearchThreads()!=0)
                        throw new AssertionError("Callback/state/drain");
                    nodes+=result.nodes();requests++;
                    out.write(gson.toJson(Map.of("type","request","root",root.id(),"round",round,
                            "measurement",measurement,"scores",table.snapshot(),
                            "workers",Arrays.stream(counts).map(P4MultiworkerStudy.Admissions::snapshot).toList(),
                            "independentProgress",driver instanceof IndependentSearch independent
                                    ?Arrays.asList(independent.workerProgress()):List.of())));out.newLine();out.flush();
                }
            }
            long helpers=Thread.getAllStackTraces().keySet().stream().filter(t->t.isAlive()
                    && (t.getName().equals("seedv6-search-helper") || t.getName().equals("seedv6-independent-search"))).count();
            if(helpers!=0) throw new AssertionError("Live helper after close");
            out.write(gson.toJson(Map.of("type","summary","status","passed","requests",requests,
                    "nodes",nodes,"activeHelpersAfterClose",helpers)));out.newLine();
        }
    }
    private P4IndependentStudy() {}
}
