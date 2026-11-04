package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.ApplicationVersion;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.manage.ClockPolicy;
import com.ohinteractive.seedv6.search.tt.TTable;

/** Integrated TC-P1/current-generation H1 dependency test under MW-R001.
 * Historical TT campaigns stay closed. An extra monitor preserves score-producer
 * provenance; this is deliberately not a speed/strength probe. A consumed score
 * is a guarded selective estimate, never an ordinary exact proof. */
public final class P4MultiworkerMemoryStudy {
    enum Schedule { ROOT_REUSE, EAGER_ROOT, WHOLE_ROOT_RACE }
    enum Delivery { HINTS_ONLY, SHARED_SCORES, PRIVATE_SCORES }
    static final class ScoreTable extends TTable {
        // Score, request generation, bound and depth; the retained hint has its own producer.
        static final long SCORE_FIELDS=0xffffffff0003ffffL;
        record Producer(int worker,long fields) {}
        final Delivery delivery;
        final ThreadLocal<Integer> worker=new ThreadLocal<>();
        final ThreadLocal<Producer> lastProbe=new ThreadLocal<>();
        final Map<Long,Producer> producers=new HashMap<>();
        final long[][] availableFrom,consumedFrom;
        long stores,acceptedStores,probes,hits,suppressed,cutoffs;
        ScoreTable(int workers,int hashMiB,Delivery delivery) {
            super(hashMiB);this.delivery=delivery;
            availableFrom=new long[workers][workers];consumedFrom=new long[workers][workers];
        }
        void bind(int id) {
            if(id<0 || id>=availableFrom.length) throw new IllegalArgumentException("Worker identity");
            worker.set(id);
        }
        int caller() { var id=worker.get();if(id==null)throw new AssertionError("Unbound score caller");return id; }
        @Override public synchronized boolean saveHeuristic(long key,int depth,int type,int score,long move) {
            int id=caller();stores++;
            if(delivery==Delivery.HINTS_ONLY) throw new AssertionError("Unexpected score producer");
            boolean saved=super.saveHeuristic(key,depth,type,score,move);
            if(saved) {
                acceptedStores++;var entry=new TEntry();
                if(!super.probeHeuristic(key,entry)) throw new AssertionError("Published score missing");
                producers.put(key,new Producer(id,entry.data&SCORE_FIELDS));
            }
            return saved;
        }
        @Override public synchronized boolean probeHeuristic(long key,TEntry entry) {
            int id=caller();probes++;lastProbe.remove();
            if(!super.probeHeuristic(key,entry)) return false;
            hits++;var producer=producers.get(key);
            if(producer==null || producer.fields()!=(entry.data&SCORE_FIELDS)) throw new AssertionError("Score provenance mismatch");
            availableFrom[producer.worker()][id]++;
            if(delivery==Delivery.PRIVATE_SCORES && producer.worker()!=id) { suppressed++;return false; }
            lastProbe.set(producer);return true;
        }
        @Override public synchronized void heuristicCutoff() {
            int id=caller();var producer=lastProbe.get();lastProbe.remove();
            if(producer==null) throw new AssertionError("Cutoff without delivered score");
            cutoffs++;consumedFrom[producer.worker()][id]++;
        }
        @Override public synchronized boolean saveHint(long key,int depth,long move) {
            return super.saveHint(key,depth,move);
        }
        @Override public void save(long key,int depth,int type,int score,long move) {
            throw new AssertionError("Unexpected ordinary proof in TC-P1 extension domain");
        }
        @Override public synchronized void clear() {
            super.clear();if(producers!=null)producers.clear();if(lastProbe!=null)lastProbe.remove();
        }
        synchronized Map<String,Object> snapshot() {
            var r=new LinkedHashMap<String,Object>();
            r.put("stores",stores);r.put("acceptedStores",acceptedStores);r.put("probes",probes);r.put("hits",hits);
            r.put("suppressedOtherWorkerScores",suppressed);r.put("guardedHeuristicCutoffs",cutoffs);
            r.put("availableProducerConsumer",availableFrom);r.put("consumedProducerConsumer",consumedFrom);
            r.put("provenanceKeys",producers.size());return r;
        }
    }

    static ParallelSearch owner(Schedule schedule,int workers,ConventionalNnueEngine model,ScoreTable table,
            P4MultiworkerStudy.Admissions[] counts) {
        var config=BaselineEngine.configuration(workers,64,BaselineEngine.SearchProfile.TC_P1).mechanisms()
                .with(FlatSearchConfig.Feature.HEURISTIC_TT_MEMORY,table.delivery!=Delivery.HINTS_ONLY);
        java.util.function.IntFunction<ExactEvaluator> evaluators=i->{
            var delegate=ExactEvaluator.from(model.evaluation());
            return counts[i]=new P4MultiworkerStudy.Admissions(new ExactEvaluator() {
                public void initialize(long[] b) { table.bind(i);delegate.initialize(b); }
                public void child(long[] p,long[] c,int ply,int qply) { delegate.child(p,c,ply,qply); }
                public void child(long[] p,long[] c,int ply) { delegate.child(p,c,ply); }
                public void reenter(int ply) { delegate.reenter(ply); }
                public int evaluate(long[] b,int ply) { return delegate.evaluate(b,ply); }
            });
        };
        return switch(schedule) {
            case ROOT_REUSE -> ParallelSearch.constructionResearch(workers,model.evaluation(),table,config,evaluators);
            case EAGER_ROOT -> ParallelSearch.constructionEagerRootResearch(workers,model.evaluation(),table,config,evaluators);
            case WHOLE_ROOT_RACE -> ParallelSearch.constructionWholeRootResearch(workers,model.evaluation(),table,config,evaluators);
        };
    }

    public static void main(String[] args) throws Exception {
        if(args.length!=9) throw new IllegalArgumentException("model corpus output workers rounds depth nodes schedule delivery");
        int workers=Integer.parseInt(args[3]),rounds=Integer.parseInt(args[4]),depth=Integer.parseInt(args[5]);
        long cap=Long.parseLong(args[6]);var delivery=Delivery.valueOf(args[8]);
        var schedule=Schedule.valueOf(args[7]);
        if(workers<2 || workers>4 || rounds<1 || rounds>4
                || depth<3 || depth>16 || cap<1 || cap>1_000_000) throw new IllegalArgumentException("Unbounded diagnostic");
        var model=ConventionalNnueEngine.load(Path.of(args[0]));
        var roots=P3HistoryStudy.roots(Path.of(args[1]));if(roots.isEmpty() || roots.size()>64) throw new IllegalArgumentException("Roots");
        var gson=new GsonBuilder().create();long nodes=0;int requests=0;
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            out.write(gson.toJson(Map.of("type","identity","build",ApplicationVersion.load().build(),
                    "source",ApplicationVersion.load().sourceRevision(),"args",args,"delivery",delivery,
                    "flags",1177812991L|(delivery==Delivery.HINTS_ONLY?0:1L<<FlatSearchConfig.Feature.HEURISTIC_TT_MEMORY.ordinal()),
                    "scope","Instrumented selective-score provenance, availability and actual guarded cutoff consumption; no speed or strength claim")));
            out.newLine();out.flush();
            for(int round=0;round<rounds;round++)for(var root:roots) {
                var table=new ScoreTable(workers,64,delivery);var counts=new P4MultiworkerStudy.Admissions[workers];
                var owner=owner(schedule,workers,model,table,counts);var saved=root.board().clone();
                try(var driver=new SearchDriver(ClockPolicy.BOUNDARY2.apply(new AspirationSearch(owner,AspirationSearch.Policy.ADAPTIVE16)))) {
                    var measurement=ConstructionTtStudy.request(driver,root,depth,cap);
                    var result=(SearchDriverOutcome)measurement.get("outcome");
                    if(result.nodes()!=Arrays.stream(counts).mapToLong(a->a.nodes).sum() || !Arrays.equals(saved,root.board())
                            || owner.activeSearchThreads()!=0) throw new AssertionError("Accounting/state/drain");
                    nodes+=result.nodes();requests++;
                    out.write(gson.toJson(Map.of("type","request","root",root.id(),"round",round,
                            "measurement",measurement,"scores",table.snapshot())));out.newLine();out.flush();
                }
            }
            out.write(gson.toJson(Map.of("type","summary","status","passed","requests",requests,"nodes",nodes,
                    "activeHelpersAfterClose",Thread.getAllStackTraces().keySet().stream()
                            .filter(t->t.isAlive()&&t.getName().equals("seedv6-search-helper")).count())));out.newLine();
        }
    }
    private P4MultiworkerMemoryStudy() {}
}
