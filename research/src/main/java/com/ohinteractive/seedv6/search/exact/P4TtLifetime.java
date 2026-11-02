package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.ApplicationVersion;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.manage.ClockPolicy;
import com.ohinteractive.seedv6.search.tt.TTable;

/** Unchanged H1 across successive real-history requests. Instrumented diagnostics,
 * never strength/cost evidence. Reads during Search use the table's own stripe
 * monitor; whole-table snapshots and counter merging happen only after draining. */
public final class P4TtLifetime {
    static final long VALID=1L<<18, HINT=1L<<19, HEURISTIC=1L<<20;
    static final class Counters {
        long probes,hits,rootHits,staleRootHits,cutoffs,attempts,admissions;
        long staleSameKeyRefresh,staleShallowerRefresh,staleCollisionReplacement,staleHintDemotion;
        long hintProbes,hintHits,hintAttempts,hintAdmissions;
        final long[] hitAge=new long[256];
        final long[][] hitDepthByAgeGroup=new long[3][256],hitBoundByAgeGroup=new long[3][3];
        final Map<Long,Long> staleNonroot=new HashMap<>();
    }
    static final class Observed extends TTable {
        final long[] keys,values,moves;final Object[] stripes;
        final ConcurrentLinkedQueue<Counters> all=new ConcurrentLinkedQueue<>();
        final ThreadLocal<Counters> local=ThreadLocal.withInitial(()->{var c=new Counters();all.add(c);return c;});
        int generation;long rootKey;
        Observed() throws Exception {
            super(64);keys=(long[])field("key");values=(long[])field("data");
            moves=(long[])field("hashMove");stripes=(Object[])field("locks");
            if(values.length!=2097152 || stripes.length!=32)throw new AssertionError("Unexpected frozen TT layout");
        }
        Object field(String name) throws Exception {var f=TTable.class.getDeclaredField(name);f.setAccessible(true);return f.get(this);}
        int age(long data) {return (generation-(int)(data>>>10&255))&255;}
        int index(long key) {return (int)key&(values.length-1);}
        Object stripe(int index) {return stripes[index&(stripes.length-1)];}
        @Override public void advanceGeneration() {super.advanceGeneration();generation=(generation+1)&255;}
        @Override public boolean probeHeuristic(long key,TEntry entry) {
            var c=local.get();c.probes++;
            boolean found=super.probeHeuristic(key,entry);
            if(found) {
                c.hits++;int age=age(entry.data),group=Math.min(age,2),depth=(int)(entry.data&255);
                c.hitAge[age]++;c.hitDepthByAgeGroup[group][depth]++;
                c.hitBoundByAgeGroup[group][(int)(entry.data>>>8&3)]++;
                if(key==rootKey) {c.rootHits++;if(age>0)c.staleRootHits++;}
                else if(age>0)c.staleNonroot.putIfAbsent(key,entry.data);
            }
            return found;
        }
        @Override public void heuristicCutoff() {local.get().cutoffs++;}
        @Override public long probeHint(long key) {
            var c=local.get();c.hintProbes++;long move=super.probeHint(key);if(move!=0)c.hintHits++;return move;
        }
        @Override public boolean saveHeuristic(long key,int depth,int type,int score,long move) {
            var c=local.get();c.attempts++;int index=index(key);
            synchronized(stripe(index)) {
                long old=values[index],oldKey=keys[index],oldMove=moves[index];
                boolean saved=super.saveHeuristic(key,depth,type,score,move);
                if(saved) {
                    c.admissions++;
                    if((old&(VALID|HEURISTIC))==(VALID|HEURISTIC) && age(old)>0) {
                        if(oldKey==key) {c.staleSameKeyRefresh++;if(depth<(old&255))c.staleShallowerRefresh++;}
                        else c.staleCollisionReplacement++;
                    }
                    int hintDepth=(int)(((old&HEURISTIC)!=0?old>>>21:old)&255);
                    if((old&VALID)!=0 && oldKey==key && age(old)==0 && hintDepth>depth
                            && oldMove!=0 && moves[index]!=oldMove)
                        throw new AssertionError("Deeper current completed hint displaced");
                }
                return saved;
            }
        }
        @Override public boolean saveHint(long key,int depth,long move) {
            var c=local.get();c.hintAttempts++;int index=index(key);
            synchronized(stripe(index)) {
                long old=values[index],oldKey=keys[index];boolean saved=super.saveHint(key,depth,move);
                if(saved) {
                    c.hintAdmissions++;
                    if(oldKey==key && (old&(VALID|HEURISTIC))==(VALID|HEURISTIC) && age(old)>0
                            && (values[index]&HEURISTIC)==0)c.staleHintDemotion++;
                }
                return saved;
            }
        }
        Map<String,Object> snapshot() {
            var sum=new Counters();var unique=new HashMap<Long,Long>();
            for(var c:all) {
                sum.probes+=c.probes;sum.hits+=c.hits;sum.rootHits+=c.rootHits;sum.staleRootHits+=c.staleRootHits;
                sum.cutoffs+=c.cutoffs;sum.attempts+=c.attempts;sum.admissions+=c.admissions;
                sum.staleSameKeyRefresh+=c.staleSameKeyRefresh;sum.staleShallowerRefresh+=c.staleShallowerRefresh;
                sum.staleCollisionReplacement+=c.staleCollisionReplacement;sum.staleHintDemotion+=c.staleHintDemotion;
                sum.hintProbes+=c.hintProbes;sum.hintHits+=c.hintHits;sum.hintAttempts+=c.hintAttempts;sum.hintAdmissions+=c.hintAdmissions;
                for(int a=0;a<256;a++)sum.hitAge[a]+=c.hitAge[a];
                for(int g=0;g<3;g++) {
                    for(int d=0;d<256;d++)sum.hitDepthByAgeGroup[g][d]+=c.hitDepthByAgeGroup[g][d];
                    for(int b=0;b<3;b++)sum.hitBoundByAgeGroup[g][b]+=c.hitBoundByAgeGroup[g][b];
                }
                c.staleNonroot.forEach(unique::putIfAbsent);
            }
            long[][] uniqueDepth=new long[3][256],uniqueBounds=new long[3][3];
            for(long data:unique.values()) {
                int group=Math.min(age(data),2);uniqueDepth[group][(int)(data&255)]++;
                uniqueBounds[group][(int)(data>>>8&3)]++;
            }
            long[][] occupiedDepth=new long[3][256],occupiedBounds=new long[3][3];long hintOnly=0,ordinary=0;
            for(long data:values)if((data&VALID)!=0) {
                if((data&HEURISTIC)!=0) {
                    int group=Math.min(age(data),2);occupiedDepth[group][(int)(data&255)]++;
                    occupiedBounds[group][(int)(data>>>8&3)]++;
                }else if((data&HINT)!=0)hintOnly++;else ordinary++;
            }
            var result=new LinkedHashMap<String,Object>();result.put("generation",generation);
            result.put("requestCounters",sum);result.put("uniqueStaleNonrootEncountered",unique.size());
            result.put("uniqueStaleNonrootDepthByCurrentAgeGroup",uniqueDepth);
            result.put("uniqueStaleNonrootBoundByCurrentAgeGroup",uniqueBounds);
            result.put("occupiedScoreDepthByAgeGroup",occupiedDepth);result.put("occupiedScoreBoundByAgeGroup",occupiedBounds);
            result.put("occupiedHintOnly",hintOnly);result.put("occupiedOrdinary",ordinary);
            result.put("entries",values.length);result.put("entryPayloadBytes",values.length*24L);
            return result;
        }
        void resetRequestCounters() {
            // Workers are drained. Retain ThreadLocal ownership while clearing
            // this request's observation counters, never the actual TT contents.
            for(var c:all) {
                c.probes=c.hits=c.rootHits=c.staleRootHits=c.cutoffs=c.attempts=c.admissions=0;
                c.staleSameKeyRefresh=c.staleShallowerRefresh=c.staleCollisionReplacement=c.staleHintDemotion=0;
                c.hintProbes=c.hintHits=c.hintAttempts=c.hintAdmissions=0;
                Arrays.fill(c.hitAge,0);for(var a:c.hitDepthByAgeGroup)Arrays.fill(a,0);
                for(var a:c.hitBoundByAgeGroup)Arrays.fill(a,0);c.staleNonroot.clear();
            }
        }
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=5)throw new IllegalArgumentException("model roots.tsv output.jsonl workers nodeBudget");
        var model=ConventionalNnueEngine.load(Path.of(args[0]));var roots=P3HistoryStudy.roots(Path.of(args[1]));
        int workers=Integer.parseInt(args[3]);long budget=Long.parseLong(args[4]);
        if((workers!=1&&workers!=4)||budget!=100000)throw new IllegalArgumentException("Frozen diagnostic domain");
        var base=BaselineEngine.legacyConfiguration(workers,64);
        var flags=base.mechanisms().with(FlatSearchConfig.Feature.HEURISTIC_TT_MEMORY,true);
        var config=new ConstructionSearch(flags,workers,64,base.aspiration());
        var groups=new LinkedHashMap<String,List<P3HistoryStudy.Root>>();
        for(var root:roots)groups.computeIfAbsent(root.id().split(":")[0],ignored->new ArrayList<>()).add(root);
        var gson=new GsonBuilder().create();
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            out.write(gson.toJson(Map.of("type","identity","source",ApplicationVersion.load().sourceRevision(),
                "build",ApplicationVersion.load().build(),"configuration",config.identity(),"workers",workers,
                "modelSha256",ConventionalNnueEngine.SHA256,"mapping",ConventionalNnueEngine.SCORE_MAPPING,
                "measurement","instrumented fixed-node successive real-history requests; no strength inference")));
            out.newLine();out.flush();
            int count=0;
            for(var group:groups.entrySet()) {
                var table=new Observed();
                var search=workers==1
                        ?new AspirationSearch(ExactSearchAdapter.constructionResearch(model.evaluation(),table,flags),base.aspiration())
                        :new AspirationSearch(ParallelSearch.constructionResearch(workers,model.evaluation(),table,flags),base.aspiration());
                try(var driver=new SearchDriver(ClockPolicy.BOUNDARY2.apply(search));
                    var plain=workers==1?new SearchDriver(ClockPolicy.BOUNDARY2.apply(config.create(model.evaluation()))):null) {
                    int previousHistorySize=-1;
                    for(var root:group.getValue()) {
                        if(previousHistorySize!=-1 && root.history().size()!=previousHistorySize+2)
                            throw new AssertionError("Expected each actor's consecutive real requests");
                        previousHistorySize=root.history().size();table.resetRequestCounters();
                        table.rootKey=SearchKey.key(root.board(),SearchKey.rootHistory(root.board(),root.history()));
                        var measurement=ConstructionTtStudy.request(driver,root,40,budget);
                        if(plain!=null) {
                            var clean=ConstructionTtStudy.request(plain,root,40,budget);
                            ConstructionTtStudy.same((SearchDriverOutcome)measurement.get("outcome"),(SearchDriverOutcome)clean.get("outcome"));
                        }
                        var row=new LinkedHashMap<String,Object>();row.put("type","request");row.put("root",root.id());
                        row.put("historySize",root.history().size());row.put("measurement",measurement);row.put("table",table.snapshot());
                        row.put("neutrality",workers==1?"same result/nodes/PV as clean persistent H1":"not asserted across parallel scheduling");
                        out.write(gson.toJson(row));out.newLine();out.flush();count++;
                    }
                }
                System.out.println("completed stream "+group.getKey()+" workers="+workers);
            }
            out.write(gson.toJson(Map.of("status","passed","requests",count)));out.newLine();
        }
    }
    private P4TtLifetime() {}
}
