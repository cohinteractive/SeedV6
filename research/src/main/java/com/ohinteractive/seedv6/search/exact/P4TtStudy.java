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

/** Instrumented information-lifetime diagnostics, never a clean strength run.
 * Atomics count shared operations; occupancy is inspected only after draining. */
public final class P4TtStudy {
    static final class Observed extends TTable {
        final AtomicLong ordinaryStores=new AtomicLong(),ordinaryHits=new AtomicLong();
        final AtomicLong attempts=new AtomicLong(),admissions=new AtomicLong(),probes=new AtomicLong(),hits=new AtomicLong();
        final AtomicLong cutoffs=new AtomicLong(),hintProbes=new AtomicLong(),hintHits=new AtomicLong();
        final AtomicLong hintAttempts=new AtomicLong(),hintAdmissions=new AtomicLong(),heuristicMoveHits=new AtomicLong();
        Observed() { super(64); }
        public void save(long key,int depth,int type,int score,long move) {
            ordinaryStores.incrementAndGet();super.save(key,depth,type,score,move);
        }
        public boolean probe(long key,TEntry e) {
            boolean r=super.probe(key,e);if(r)ordinaryHits.incrementAndGet();return r;
        }
        public boolean saveHeuristic(long key,int depth,int type,int score,long move) {
            attempts.incrementAndGet();boolean r=super.saveHeuristic(key,depth,type,score,move);
            if(r)admissions.incrementAndGet();return r;
        }
        public boolean probeHeuristic(long key,TEntry e) {
            probes.incrementAndGet();boolean r=super.probeHeuristic(key,e);
            if(r) { hits.incrementAndGet();if(e.hashMove!=0)heuristicMoveHits.incrementAndGet(); }return r;
        }
        public void heuristicCutoff() { cutoffs.incrementAndGet(); }
        public long probeHint(long key) {
            hintProbes.incrementAndGet();long move=super.probeHint(key);if(move!=0)hintHits.incrementAndGet();return move;
        }
        public boolean saveHint(long key,int depth,long move) {
            hintAttempts.incrementAndGet();boolean r=super.saveHint(key,depth,move);if(r)hintAdmissions.incrementAndGet();return r;
        }
        Map<String,Object> snapshot(boolean occupancy) throws Exception {
            var m=new LinkedHashMap<String,Object>();
            m.put("ordinaryStores",ordinaryStores.get());m.put("ordinaryHits",ordinaryHits.get());
            m.put("scoreAttempts",attempts.get());m.put("scoreAdmissions",admissions.get());
            m.put("scoreRejected",attempts.get()-admissions.get());m.put("scoreProbes",probes.get());
            m.put("scoreHits",hits.get());m.put("heuristicCutoffs",cutoffs.get());
            m.put("hintProbes",hintProbes.get());m.put("hintHits",hintHits.get());
            m.put("heuristicMoveHits",heuristicMoveHits.get());m.put("hintAttempts",hintAttempts.get());
            m.put("hintAdmissions",hintAdmissions.get());
            if(occupancy) {
                var f=TTable.class.getDeclaredField("data");f.setAccessible(true);var values=(long[])f.get(this);
                long heuristic=0,hintOnly=0,ordinary=0;long[] depths=new long[256];
                for(long v:values)if((v&(1L<<18))!=0) {
                    if((v&(1L<<20))!=0) {heuristic++;depths[(int)(v&255)]++;}
                    else if((v&(1L<<19))!=0)hintOnly++;else ordinary++;
                }
                m.put("occupiedHeuristic",heuristic);m.put("occupiedHintOnly",hintOnly);m.put("occupiedOrdinary",ordinary);
                m.put("retainedHeuristicDepth",depths);m.put("entries",values.length);m.put("entryPayloadBytes",values.length*24L);
            }
            return m;
        }
    }

    public static void main(String[] args) throws Exception {
        if(args.length!=5)throw new IllegalArgumentException("CP64 corpus.tsv new-output.jsonl workers nodeBudget");
        var model=ConventionalNnueEngine.load(Path.of(args[0]));
        var roots=P3HistoryStudy.roots(Path.of(args[1]));int workers=Integer.parseInt(args[3]);long budget=Long.parseLong(args[4]);
        var gson=new GsonBuilder().create();
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            out.write(gson.toJson(Map.of("type","identity","source",ApplicationVersion.load().sourceRevision(),
                    "build",ApplicationVersion.load().build(),"modelSha256",ConventionalNnueEngine.SHA256,
                    "mapping",ConventionalNnueEngine.SCORE_MAPPING,"workers",workers,"budget",budget,
                    "mode","instrumented nodes, BOUNDARY2 selected but not active for explicit node requests")));
            out.newLine();out.flush();
            for(var root:roots)for(String profile:List.of("control","h1")) {
                var base=BaselineEngine.legacyConfiguration(workers,64);
                var flags=base.mechanisms().with(FlatSearchConfig.Feature.HEURISTIC_TT_MEMORY,profile.equals("h1"));
                var config=new ConstructionSearch(flags,workers,64,base.aspiration());var table=new Observed();
                var attempts=new ArrayList<Map<String,Object>>();
                AspirationSearch.AttemptObserver observer=(d,a,b,r,n)-> {
                    try {
                        var row=new LinkedHashMap<String,Object>();row.put("depth",d);row.put("alpha",a);row.put("beta",b);
                        row.put("outcome",r);row.put("nanos",n);row.put("tableCumulative",table.snapshot(false));attempts.add(row);
                    }catch(Exception e){throw new IllegalStateException(e);}
                };
                var observed=workers==1
                        ?new AspirationSearch(ExactSearchAdapter.constructionResearch(model.evaluation(),table,flags),base.aspiration(),observer)
                        :new AspirationSearch(ParallelSearch.constructionResearch(workers,model.evaluation(),table,flags),base.aspiration(),observer);
                try(var driver=new SearchDriver(ClockPolicy.BOUNDARY2.apply(observed))) {
                    var result=ConstructionTtStudy.request(driver,root,40,budget);
                    if(workers==1)try(var plain=new SearchDriver(ClockPolicy.BOUNDARY2.apply(config.create(model.evaluation())))) {
                        var unobserved=ConstructionTtStudy.request(plain,root,40,budget);
                        ConstructionTtStudy.same((SearchDriverOutcome)result.get("outcome"),(SearchDriverOutcome)unobserved.get("outcome"));
                    }
                    var row=new LinkedHashMap<String,Object>();row.put("type","observation");row.put("id",root.id());
                    row.put("profile",profile);row.put("configuration",config.identity());row.put("historySize",root.history().size());
                    row.put("measurement",result);row.put("table",table.snapshot(true));row.put("attempts",attempts);
                    row.put("instrumentationNeutrality",workers==1?"matched clean nodes/result/PV":"not asserted for scheduling-dependent parallel Search");
                    out.write(gson.toJson(row));out.newLine();out.flush();
                }
            }
            out.write(gson.toJson(Map.of("status","passed")));out.newLine();
        }
    }
    private P4TtStudy() {}
}
