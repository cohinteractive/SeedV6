package com.ohinteractive.seedv6.search.exact;

import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.ApplicationVersion;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.manage.ClockPolicy;
import com.ohinteractive.seedv6.search.tt.TTable;

/** IC-R001: drained-window facts from the exact MW-C4 kernel. No production hooks.
 * Root effort is admitted work, not confidence, and no per-move score is invented.
 * Reflection is read-only, source-bound and performed after every worker joins. */
public final class P4IterationConfidenceStudy {
    record Effort(long move, long nodes) {}
    static final class RootFacts {
        final ParallelSearch owner;
        final Field split, moves, effort, previousMoves, previousEffort;
        RootFacts(ParallelSearch owner) throws ReflectiveOperationException {
            this.owner=owner;
            split=field("splitActive"); moves=field("splitMoves"); effort=field("splitEffort");
            previousMoves=field("previousMoves"); previousEffort=field("previousEffort");
        }
        static Field field(String name) throws ReflectiveOperationException {
            var f=ParallelSearch.class.getDeclaredField(name); f.setAccessible(true); return f;
        }
        List<Effort> read(Field m,Field e) throws IllegalAccessException {
            long[] ms=(long[])m.get(owner), ns=(long[])e.get(owner);
            if(ms.length!=ns.length) throw new AssertionError("Root effort shape");
            var rows=new ArrayList<Effort>(); var unique=new HashSet<Long>();
            for(int i=0;i<ms.length;i++) {
                if(ns[i]<0 || !unique.add(ms[i])) throw new AssertionError("Invalid root effort");
                rows.add(new Effort(ms[i],ns[i]));
            }
            return List.copyOf(rows);
        }
        Map<String,Object> snapshot(long nodes) {
            if(owner.activeSearchThreads()!=0) throw new AssertionError("Undrained read");
            try {
                boolean s=split.getBoolean(owner);
                var current=s?read(moves,effort):List.<Effort>of();
                if(s && current.stream().mapToLong(Effort::nodes).sum()!=nodes)
                    throw new AssertionError("Root effort/request admissions");
                return Map.of("split",s,"currentPass",current,
                        "retainedResolvedPass",read(previousMoves,previousEffort));
            } catch(IllegalAccessException e) { throw new IllegalStateException(e); }
        }
    }

    /** model roots NEW-output rounds depth node-cap root-count aspiration-policy */
    public static void main(String[] args) throws Exception {
        if(args.length!=8) throw new IllegalArgumentException("model roots output rounds depth cap count policy");
        int rounds=Integer.parseInt(args[3]), depth=Integer.parseInt(args[4]), count=Integer.parseInt(args[6]);
        long cap=Long.parseLong(args[5]);
        String policy=args[7];
        boolean carryover=policy.equals("VOLATILITY_CARRYOVER");
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
                var owner=ParallelSearch.constructionEagerRootResearch(4,model.evaluation(),new TTable(64),
                        configuration.mechanisms(),w -> admissions[w]=new P4MultiworkerStudy.Admissions(ExactEvaluator.from(model.evaluation())));
                var facts=new RootFacts(owner);
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
                    row.put("workers",work);row.put("elapsedNanos",nanos);row.put("rootEffort",facts.snapshot(nodes));
                    row.put("eager",owner.eagerRootScheduling());
                    row.put("classification",!result.completed()?"incomplete":result.score()<=a?"upper":result.score()>=b?"lower":"resolved-selective");
                    attempts.add(row);
                };
                long[] saved=root.board().clone();
                SingleDepthSearch search=carryover
                        ?new P4IterationConfidenceSearch(owner,owner::searchWindow,observer)
                        :new AspirationSearch(owner,AspirationSearch.Policy.valueOf(policy),observer);
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
    private P4IterationConfidenceStudy() {}
}
