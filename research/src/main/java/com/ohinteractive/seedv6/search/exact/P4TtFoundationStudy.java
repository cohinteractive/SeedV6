package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.ApplicationVersion;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.manage.ClockPolicy;
import com.ohinteractive.seedv6.search.tt.TTable;

/** TTF-R001 causal diagnostics and clean costs. No result is playing-strength evidence.
 * Explicit profiles preserve old controls and independently ablate storage/use/admission. */
public final class P4TtFoundationStudy {
    public static FlatSearchConfig flags(String profile,int workers) {
        var base=BaselineEngine.legacyConfiguration(workers,64).mechanisms();
        if(profile.equals("production"))return base;
        if(!Set.of("h2","foundation","retain").contains(profile))
            throw new IllegalArgumentException("Unknown TTF profile: "+profile);
        return base.with(FlatSearchConfig.Feature.HEURISTIC_TT_MEMORY,true)
                .with(FlatSearchConfig.Feature.AGED_HEURISTIC_TT_MEMORY,true)
                .with(FlatSearchConfig.Feature.TT_INFORMATION,Set.of("foundation","retain").contains(profile));
    }
    public static final class RetentionOnly extends TTable {
        public RetentionOnly(int hash) {super(hash,true);}
        @Override public boolean probeInformation(long key,TEntry entry) {
            boolean hit=super.probeInformation(key,entry);entry.rawEvaluation=Integer.MIN_VALUE;return hit;
        }
    }
    static final class Observed extends TTable {
        final boolean retentionOnly;
        final AtomicLong rawProbes=new AtomicLong(),rawHits=new AtomicLong(),rawAttempts=new AtomicLong(),rawSaves=new AtomicLong();
        final AtomicLong scores=new AtomicLong(),scoreHits=new AtomicLong(),cutoffs=new AtomicLong(),consumed=new AtomicLong();
        Observed(boolean raw,boolean retentionOnly) {super(64,raw);this.retentionOnly=retentionOnly;}
        @Override public boolean probeInformation(long key,TEntry entry) {
            rawProbes.incrementAndGet();boolean hit=super.probeInformation(key,entry);
            if(entry.rawEvaluation!=Integer.MIN_VALUE)rawHits.incrementAndGet();
            if(entry.hasHeuristicScore())scoreHits.incrementAndGet();
            if(retentionOnly)entry.rawEvaluation=Integer.MIN_VALUE;return hit;
        }
        @Override public void rawEvaluationUsed() {consumed.incrementAndGet();}
        @Override public boolean saveRawEvaluation(long key,int value) {
            rawAttempts.incrementAndGet();boolean ok=super.saveRawEvaluation(key,value);if(ok)rawSaves.incrementAndGet();return ok;
        }
        @Override public boolean probeHeuristic(long key,TEntry entry) {
            boolean ok=super.probeHeuristic(key,entry);if(ok)scoreHits.incrementAndGet();return ok;
        }
        @Override public boolean saveHeuristicAged(long key,int depth,int type,int score,long move) {
            boolean ok=super.saveHeuristicAged(key,depth,type,score,move);if(ok)scores.incrementAndGet();return ok;
        }
        @Override public void heuristicCutoff() {cutoffs.incrementAndGet();}
        Map<String,Object> snapshot() {
            return Map.of("rawProbes",rawProbes.get(),"rawAvailable",rawHits.get(),"rawConsumed",consumed.get(),
                    "rawAttempts",rawAttempts.get(),"rawAdmitted",rawSaves.get(),"scoreAdmitted",scores.get(),
                    "scoreHits",scoreHits.get(),"scoreCutoffs",cutoffs.get());
        }
    }
    static Object field(Object object,String name) throws Exception {
        var f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);
    }
    static List<NullMovePolicy.Diagnostics> observeNull(SingleDepthSearch search,int workers) throws Exception {
        var result=new ArrayList<NullMovePolicy.Diagnostics>();
        if(workers==1) {
            var d=new NullMovePolicy.Diagnostics();((ExactSearch)field(search,"exact")).withFlatNullObservation(false,d);result.add(d);
        } else for(Object worker:(Object[])field(search,"workers")) {
            var d=new NullMovePolicy.Diagnostics();((ExactSearch)field(worker,"search")).withFlatNullObservation(false,d);result.add(d);
        }
        return result;
    }
    static Map<String,Long> nullSnapshot(List<NullMovePolicy.Diagnostics> values) {
        long attempts=0,probes=0,verify=0,verifyNodes=0,cutoffs=0,rejected=0;
        for(var d:values) {attempts+=d.attempts;probes+=d.probeNodes;verify+=d.verifications;
            verifyNodes+=d.verificationNodes;cutoffs+=d.cutoffs;rejected+=d.rejected;}
        return Map.of("attempts",attempts,"probeNodes",probes,"verifications",verify,"verificationNodes",verifyNodes,
                "cutoffs",cutoffs,"rejected",rejected);
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=9)throw new IllegalArgumentException("model corpus output workers observe|cost rounds depth budget profiles");
        var model=ConventionalNnueEngine.load(Path.of(args[0]));var roots=P3HistoryStudy.roots(Path.of(args[1]));
        int workers=Integer.parseInt(args[3]),rounds=Integer.parseInt(args[5]),depth=Integer.parseInt(args[6]);
        long budget=Long.parseLong(args[7]);boolean observe=args[4].equals("observe");
        if(!observe&&!args[4].equals("cost"))throw new IllegalArgumentException("mode");
        var profiles=List.of(args[8].split(","));var gson=new GsonBuilder().create();int requests=0;
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            out.write(gson.toJson(Map.of("type","identity","source",ApplicationVersion.load().sourceRevision(),
                    "build",ApplicationVersion.load().build(),"modelSha256",ConventionalNnueEngine.SHA256,
                    "head",model.model().headBackend(),"mapping",ConventionalNnueEngine.SCORE_MAPPING,"args",args)));
            out.newLine();out.flush();
            // One warmup round for clean costs; rotated order avoids a fixed first-profile disadvantage.
            for(int round=observe?0:-1;round<rounds;round++) {
                int rootIndex=0;
                for(var root:roots) {
                    var outcomes=new HashMap<String,SearchDriverOutcome>();
                    for(int offset=0;offset<profiles.size();offset++) {
                        String profile=profiles.get(Math.floorMod(round+rootIndex+offset,profiles.size()));
                        var config=flags(profile,workers);boolean raw=config.has(FlatSearchConfig.Feature.TT_INFORMATION);
                        TTable table=observe?new Observed(raw,profile.equals("retain")):
                                profile.equals("retain")?new RetentionOnly(64):new TTable(64,raw);
                        SingleDepthSearch owner=workers==1?ExactSearchAdapter.constructionResearch(model.evaluation(),table,config):
                                ParallelSearch.constructionResearch(workers,model.evaluation(),table,config);
                        var nulls=observe?observeNull(owner,workers):List.<NullMovePolicy.Diagnostics>of();
                        var attempts=new ArrayList<Map<String,Object>>();
                        AspirationSearch.AttemptObserver observer=observe?
                                (d,a,b,r,n)->attempts.add(Map.of("depth",d,"alpha",a,"beta",b,"result",r,"nanos",n)):null;
                        var aspiration=workers==1?new AspirationSearch((ExactSearchAdapter)owner,AspirationSearch.Policy.ADAPTIVE16,observer):
                                new AspirationSearch((ParallelSearch)owner,AspirationSearch.Policy.ADAPTIVE16,observer);
                        var saved=root.board().clone();
                        try(var driver=new SearchDriver(ClockPolicy.BOUNDARY2.apply(aspiration))) {
                            var measurement=ConstructionTtStudy.request(driver,root,depth,budget);
                            var outcome=(SearchDriverOutcome)measurement.get("outcome");outcomes.put(profile,outcome);
                            if(!Arrays.equals(saved,root.board()))throw new AssertionError("Board changed");
                            var row=new LinkedHashMap<String,Object>();row.put("type","request");row.put("profile",profile);
                            row.put("root",root.id());row.put("historySize",root.history().size());row.put("round",round);row.put("order",offset);
                            row.put("configuration",new ConstructionSearch(config,workers,64,AspirationSearch.Policy.ADAPTIVE16).identity());
                            row.put("entries",table.entryCapacity());row.put("payloadBytes",table.entryPayloadBytes());row.put("measurement",measurement);
                            if(observe) {row.put("table",((Observed)table).snapshot());row.put("null",nullSnapshot(nulls));row.put("attempts",attempts);}
                            out.write(gson.toJson(row));out.newLine();out.flush();requests++;
                        }
                    }
                    // Pure raw caching/retention must preserve W1 work and results. Diagnostics differ in evaluations.
                    if(workers==1&&outcomes.containsKey("h2"))for(String p:List.of("foundation","retain"))if(outcomes.containsKey(p)) {
                        var a=outcomes.get("h2");var b=outcomes.get(p);
                        if(a.nodes()!=b.nodes()||a.attemptedDepth()!=b.attemptedDepth()||a.iterationIncomplete()!=b.iterationIncomplete()
                                || a.lastCompletedResult().score()!=b.lastCompletedResult().score()
                                || !Arrays.equals(a.lastCompletedResult().principalVariation(),b.lastCompletedResult().principalVariation()))
                            throw new AssertionError("Raw-only/retention changed W1 Search: "+root.id()+" "+p);
                    }
                    rootIndex++;
                }
                System.out.println("completed "+args[4]+" W"+workers+" round="+round);
            }
            out.write(gson.toJson(Map.of("status","passed","requests",requests)));out.newLine();
        }
    }
    private P4TtFoundationStudy() {}
}
