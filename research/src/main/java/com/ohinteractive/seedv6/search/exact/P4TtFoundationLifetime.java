package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.manage.ClockPolicy;

/** Persistent real-history request streams; fresh owners only at actor boundaries.
 * Actual evaluation avoidance and full-request accounting are observed separately. */
public final class P4TtFoundationLifetime {
    public static void main(String[] args) throws Exception {
        if(args.length!=4)throw new IllegalArgumentException("model corpus output workers");
        var model=ConventionalNnueEngine.load(Path.of(args[0]));int workers=Integer.parseInt(args[3]);
        var roots=P3HistoryStudy.roots(Path.of(args[1]));var groups=new LinkedHashMap<String,List<P3HistoryStudy.Root>>();
        for(var root:roots)groups.computeIfAbsent(root.id().split(":")[0],unused->new ArrayList<>()).add(root);
        var gson=new GsonBuilder().create();int count=0;
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            for(var group:groups.entrySet()) {
                var drivers=new ArrayList<SearchDriver>();var tables=new ArrayList<P4TtFoundationStudy.Observed>();
                var profiles=List.of("h2","foundation","retain");
                try {
                    for(var profile:profiles) {
                        var table=new P4TtFoundationStudy.Observed(!profile.equals("h2"),profile.equals("retain"));tables.add(table);
                        var flags=P4TtFoundationStudy.flags(profile,workers);
                        var search=workers==1?new AspirationSearch(ExactSearchAdapter.constructionResearch(model.evaluation(),table,flags),AspirationSearch.Policy.ADAPTIVE16):
                                new AspirationSearch(ParallelSearch.constructionResearch(workers,model.evaluation(),table,flags),AspirationSearch.Policy.ADAPTIVE16);
                        drivers.add(new SearchDriver(ClockPolicy.BOUNDARY2.apply(search)));
                    }
                    for(var root:group.getValue()) {
                        var outcomes=new ArrayList<SearchDriverOutcome>();
                        for(int side=0;side<3;side++) {
                            var before=tables.get(side).snapshot();var saved=root.board().clone();
                            var result=ConstructionTtStudy.request(drivers.get(side),root,40,100000);
                            var outcome=(SearchDriverOutcome)result.get("outcome");outcomes.add(outcome);
                            if(outcome.nodes()!=100000 || !Arrays.equals(saved,root.board()))throw new AssertionError("Work/state");
                            var after=tables.get(side).snapshot();var delta=new LinkedHashMap<String,Long>();
                            for(var key:before.keySet())delta.put(key,((Number)after.get(key)).longValue()-((Number)before.get(key)).longValue());
                            out.write(gson.toJson(Map.of("type","request","profile",profiles.get(side),"root",root.id(),
                                    "workers",workers,"historySize",root.history().size(),"measurement",result,"table",delta)));
                            out.newLine();out.flush();count++;
                        }
                        if(workers==1) {
                            var a=outcomes.get(0);
                            for(var b:outcomes.subList(1,3))if(a.attemptedDepth()!=b.attemptedDepth()
                                    ||a.lastCompletedResult().score()!=b.lastCompletedResult().score()
                                    ||!Arrays.equals(a.lastCompletedResult().principalVariation(),b.lastCompletedResult().principalVariation()))
                                throw new AssertionError("Persistent W1 cache-only result mismatch: "+root.id());
                            long base=a.diagnostics().worker().nodes().evaluationCalls();
                            long retained=outcomes.get(2).diagnostics().worker().nodes().evaluationCalls();
                            if(base!=retained)throw new AssertionError("Retention-only unexpectedly avoided inference");
                        }
                    }
                } finally {for(var driver:drivers)driver.close();}
            }
            out.write(gson.toJson(Map.of("status","passed","requests",count)));out.newLine();
        }
    }
    private P4TtFoundationLifetime() {}
}
