package com.ohinteractive.seedv6.search.exact;

import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.ApplicationVersion;
import com.ohinteractive.seedv6.search.common.SingleDepthSearch;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.manage.ClockPolicy;
import com.ohinteractive.seedv6.search.tt.TTable;

/** CLV fixed-work comparison using the shared kernel; no alternate Search.
 * Fresh request owners and TT. Read private lifetime observations only after drain.
 * Node-budget outcomes and wall costs are diagnostics, never playing-strength proof. */
public final class ClvStudy {
    static FlatSearchConfig configuration(String profile) {
        var base=BaselineEngine.configuration(4,64,BaselineEngine.SearchProfile.MW_C4).mechanisms();
        return switch(profile) {
            case "control" -> base;
            case "c1" -> base.with(FlatSearchConfig.Feature.TT_CRITICAL_RELIEF,true);
            default -> throw new IllegalArgumentException("Unknown CLV profile: "+profile);
        };
    }
    static long[] counters(SingleDepthSearch owner) throws ReflectiveOperationException {
        if(owner.activeSearchThreads()!=0) throw new AssertionError("Undrained observation");
        if(owner instanceof ExactSearchAdapter) {
            Field f=ExactSearchAdapter.class.getDeclaredField("exact");f.setAccessible(true);
            return ((ExactSearch)f.get(owner)).criticalLineCounters();
        }
        Field f=ParallelSearch.class.getDeclaredField("workers");f.setAccessible(true);
        long[] total=new long[2];
        for(Object w:(Object[])f.get(owner)) {
            Field s=w.getClass().getDeclaredField("search");s.setAccessible(true);
            long[] c=((ExactSearch)s.get(w)).criticalLineCounters();
            for(int i=0;i<2;i++) total[i]+=c[i];
        }
        return total;
    }
    /** model roots NEW-output workers rounds depth cap count profiles */
    public static void main(String[] args) throws Exception {
        if(args.length!=9) throw new IllegalArgumentException("model roots output workers rounds depth cap count profiles");
        int workers=Integer.parseInt(args[3]),rounds=Integer.parseInt(args[4]),depth=Integer.parseInt(args[5]),count=Integer.parseInt(args[7]);
        long cap=Long.parseLong(args[6]);var profiles=List.of(args[8].split(","));
        if((workers!=1 && workers!=4) || rounds<1 || rounds>3 || depth<4 || depth>16 || cap<1 || cap>1_000_000 || count<1 || count>40)
            throw new IllegalArgumentException("Bounded W1/W4 diagnostic domain");
        profiles.forEach(ClvStudy::configuration);
        var roots=P3HistoryStudy.roots(Path.of(args[1]));if(count>roots.size()) throw new IllegalArgumentException("Root count");
        var model=ConventionalNnueEngine.load(Path.of(args[0]));var gson=new GsonBuilder().create();
        long total=0;int requests=0;
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            out.write(gson.toJson(Map.of("type","identity","source",ApplicationVersion.load().sourceRevision(),
                    "build",ApplicationVersion.load().build(),"args",args,"model",model.identity(64),
                    "java",System.getProperty("java.runtime.version"),"scope","Fresh-owner fixed-work development, not strength or clean speed")));
            out.newLine();out.flush();
            for(int round=0;round<rounds;round++) for(int i=0;i<count;i++) {
                var root=roots.get(round%2==0?i:count-1-i);
                for(int j=0;j<profiles.size();j++) {
                    String profile=profiles.get((j+i+round)%profiles.size());var flags=configuration(profile);
                    var admissions=new P4MultiworkerStudy.Admissions[workers];
                    SingleDepthSearch owner=workers==1
                            ?ExactSearchAdapter.constructionResearch(model.evaluation(),new TTable(64),flags)
                            :ParallelSearch.constructionEagerRootResearch(workers,model.evaluation(),new TTable(64),flags,
                                w->admissions[w]=new P4MultiworkerStudy.Admissions(ExactEvaluator.from(model.evaluation())));
                    long[] audited={0};
                    AspirationSearch.AttemptObserver observer=(d,a,b,result,nanos)-> {
                        long nodes=0;
                        if(owner instanceof ParallelSearch parallel) {
                          var work=parallel.workerWork();
                          for(int w=0;w<workers;w++) {
                            if(admissions[w].nodes!=work[w].nodes() || admissions[w].qnodes!=work[w].qnodes()
                                    || admissions[w].evaluations!=work[w].evaluations()) throw new AssertionError("Private accounting");
                            nodes+=admissions[w].nodes;admissions[w].reset();
                          }
                        } else {
                            nodes=result.nodes();
                        }
                        if(nodes!=result.nodes() || nodes!=result.diagnostics().totalEnteredNodes()) throw new AssertionError("Pass accounting");
                        if(result.completed()) Phase2Oracle.legalPv(root.board(),result.principalVariation());
                        audited[0]+=nodes;
                    };
                    long[] saved=root.board().clone();
                    var search=owner instanceof ParallelSearch parallel
                            ?new AspirationSearch(parallel,AspirationSearch.Policy.ADAPTIVE16,observer)
                            :new AspirationSearch((ExactSearchAdapter)owner,AspirationSearch.Policy.ADAPTIVE16,observer);
                    try(var driver=new SearchDriver(ClockPolicy.BOUNDARY2.apply(search))) {
                        var measurement=ConstructionTtStudy.request(driver,root,depth,cap);
                        var result=(SearchDriverOutcome)measurement.get("outcome");
                        if(result.nodes()!=audited[0] || !Arrays.equals(saved,root.board())) throw new AssertionError("Request restoration/accounting");
                        long[] c=counters(owner);total+=result.nodes();requests++;
                        out.write(gson.toJson(Map.of("type","request","root",root.id(),"round",round,"profile",profile,
                                "flags",flags.flags(),"measurement",measurement,"criticalNodes",c[0],"criticalReliefs",c[1])));
                        out.newLine();out.flush();
                    }
                }
            }
            long helpers=Thread.getAllStackTraces().keySet().stream().filter(t->t.isAlive()&&t.getName().equals("seedv6-search-helper")).count();
            if(helpers!=0) throw new AssertionError("Helpers after close");
            out.write(gson.toJson(Map.of("type","summary","status","passed","requests",requests,"nodes",total,"activeHelpersAfterClose",helpers)));
            out.newLine();
        }
    }
    private ClvStudy() {}
}
