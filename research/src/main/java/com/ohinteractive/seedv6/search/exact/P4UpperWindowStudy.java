package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.ApplicationVersion;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.manage.ClockPolicy;
import com.ohinteractive.seedv6.search.tt.TTable;

/** IC-R001: drained-window facts from the exact MW-C4 kernel. No production hooks.
 * Root-job bounds remain selective observations, not exact proof or independent votes.
 * The mechanical observation copy records job-private slots, read only after every worker joins. */
public final class P4UpperWindowStudy {
    /** model roots NEW-output rounds depth node-cap root-count aspiration-policy */
    public static void main(String[] args) throws Exception {
        if(args.length!=8) throw new IllegalArgumentException("model roots output rounds depth cap count policy");
        int rounds=Integer.parseInt(args[3]), depth=Integer.parseInt(args[4]), count=Integer.parseInt(args[6]);
        long cap=Long.parseLong(args[5]);
        String policy=args[7];
        boolean carryover=policy.equals("UPPER_CARRYOVER");
        if(!carryover) AspirationSearch.Policy.valueOf(policy);
        if(rounds<1 || rounds>4 || depth<3 || depth>16 || cap<1 || cap>1_000_000 || count<1 || count>40)
            throw new IllegalArgumentException("Bounded daytime study only");
        var roots=P3HistoryStudy.roots(Path.of(args[1]));
        if(count>roots.size()) throw new IllegalArgumentException("Root count");
        var model=ConventionalNnueEngine.load(Path.of(args[0]));
        var configuration=BaselineEngine.configuration(4,64,BaselineEngine.SearchProfile.MW_C4_W4);
        if(configuration.mechanisms().flags()!=1182007295) throw new AssertionError("Exact C4 required");
        var gson=new GsonBuilder().create(); long total=0; int requests=0;
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            out.write(gson.toJson(Map.of("type","identity","source",ApplicationVersion.load().sourceRevision(),
                    "build",ApplicationVersion.load().build(),"configuration",configuration.identity(),
                    "aspiration",policy,"args",args,"networkSha256",ConventionalNnueEngine.SHA256,
                    "java",System.getProperty("java.runtime.version"),"scope","Instrumented fixed-node facts; not playing strength or clean speed")));
            out.newLine();out.flush();
            for(int round=0;round<rounds;round++) for(int i=0;i<count;i++) {
                // Reverse corpus traversal each round; requests remain fresh TT/private state.
                var root=roots.get(round%2==0?i:count-1-i);
                var admissions=new P4MultiworkerStudy.Admissions[4];
                var owner=P4ObservedRootSearch.constructionEagerRootResearch(4,model.evaluation(),new TTable(64),
                        configuration.mechanisms(),w -> admissions[w]=new P4MultiworkerStudy.Admissions(ExactEvaluator.from(model.evaluation())));
                var attempts=new ArrayList<Map<String,Object>>();
                AspirationSearch.AttemptObserver observer=(d,a,b,result,nanos) -> {
                    var work=owner.workerWork();long nodes=0;
                    for(int w=0;w<4;w++) {
                        if(admissions[w].nodes!=work[w].nodes() || admissions[w].qnodes!=work[w].qnodes()
                                || admissions[w].evaluations!=work[w].evaluations()) throw new AssertionError("Private accounting");
                        nodes+=admissions[w].nodes; admissions[w].reset();
                    }
                    if(nodes!=result.nodes() || nodes!=result.diagnostics().totalEnteredNodes()) throw new AssertionError("Pass accounting");
                    if(result.completed()) Phase2Oracle.legalPv(root.board(),result.principalVariation());
                    var row=new LinkedHashMap<String,Object>();
                    row.put("depth",d);row.put("alpha",a);row.put("beta",b);row.put("result",result);
                    row.put("workers",work);row.put("elapsedNanos",nanos);var jobs=owner.rootJobObservations();
                    if(!jobs.isEmpty() && jobs.stream().mapToLong(P4ObservedRootSearch.RootJobObservation::nodes).sum()!=nodes)
                        throw new AssertionError("Root-job admission accounting");
                    row.put("rootJobs",jobs);
                    row.put("eager",owner.eagerRootScheduling());
                    row.put("classification",!result.completed()?"incomplete":result.score()<=a?"upper":result.score()>=b?"lower":"resolved-selective");
                    attempts.add(row);
                };
                long[] saved=root.board().clone();
                AspirationSearch.WindowSearch observedWindow=(request,alpha,beta)->{
                    long started=System.nanoTime();
                    var result=owner.searchWindow(request,alpha,beta);
                    observer.completed(request.depth(),alpha,beta,result,System.nanoTime()-started);
                    return result;
                };
                SingleDepthSearch search=carryover
                        ?new P4UpperWindowSearch(owner,observedWindow,null)
                        :AspirationSearch.researchWindows(owner,observedWindow,AspirationSearch.Policy.valueOf(policy));
                try(var driver=new SearchDriver(ClockPolicy.BOUNDARY2.apply(search))) {
                    var measurement=ConstructionTtStudy.request(driver,root,depth,cap);
                    var outcome=(SearchDriverOutcome)measurement.get("outcome");
                    if(attempts.stream().mapToLong(x -> ((SearchResult)x.get("result")).nodes()).sum()!=outcome.nodes()
                            || !Arrays.equals(saved,root.board())) throw new AssertionError("Restoration/request accounting");
                    total+=outcome.nodes();requests++;
                    out.write(gson.toJson(Map.of("type","request","root",root.id(),"round",round,
                            "measurement",measurement,"attempts",attempts)));out.newLine();out.flush();
                }
            }
            long helpers=Thread.getAllStackTraces().keySet().stream()
                    .filter(t->t.isAlive() && t.getName().equals("seedv6-search-helper")).count();
            if(helpers!=0) throw new AssertionError("Helpers after close");
            out.write(gson.toJson(Map.of("type","summary","status","passed","requests",requests,"nodes",total,
                    "activeHelpersAfterClose",helpers)));out.newLine();
        }
    }
    private P4UpperWindowStudy() {}
}
