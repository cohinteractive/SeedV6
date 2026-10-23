package com.ohinteractive.seedv6.search.exact;

import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;

/** CP64 whole-request TT diagnostics and clean existing-control costs.
 * Observation reads existing table arrays only in a single-worker domain. No
 * runtime policy changes, alternate TT, or instrumented strength measurements. */
public final class ConstructionTtStudy {
    static final class ObservedTable extends TTable {
        final long[] keys, data, moves;
        int generation;
        long proofProbes, proofHits, proofStores, hintProbes, hintHits, missingEmpty, missingCollision;
        long writes, emptyWrites, refreshWrites, collisionWrites, sameMoveRefresh, proofBlocked, deeperBlocked;
        final long[] hitAge = new long[256], hitDepth = new long[256], storeDepth = new long[256];

        ObservedTable(int mib) throws Exception {
            super(mib);
            keys = P3HintLifetimeStudy.Observed.array(this, "key");
            data = P3HintLifetimeStudy.Observed.array(this, "data");
            moves = P3HintLifetimeStudy.Observed.array(this, "hashMove");
        }
        @Override public void advanceGeneration() { super.advanceGeneration(); generation = (generation + 1) & 255; }
        @Override public boolean probe(long key, TEntry entry) {
            proofProbes++;
            boolean found = super.probe(key, entry);
            if (found) proofHits++;
            return found;
        }
        @Override public void save(long key, int depth, int type, int score, long move) {
            proofStores++;
            super.save(key, depth, type, score, move);
        }
        @Override public long probeHint(long key) {
            hintProbes++;
            long move = super.probeHint(key);
            int index = (int)key & (data.length - 1);
            if (move != 0) {
                hintHits++;
                hitAge[(generation - (int)(data[index] >>> 10 & 255)) & 255]++;
                hitDepth[(int)(data[index] & 255)]++;
            } else if ((data[index] & (1L << 18)) == 0) missingEmpty++;
            else if (keys[index] != key) missingCollision++;
            return move;
        }
        @Override public boolean saveHint(long key, int depth, long move) {
            int index = (int)key & (data.length - 1);
            long old = data[index], oldKey = keys[index], oldMove = moves[index];
            boolean saved = super.saveHint(key, depth, move);
            if (saved) {
                writes++; storeDepth[depth]++;
                if ((old & (1L << 18)) == 0) emptyWrites++;
                else if (oldKey == key) { refreshWrites++; if (oldMove == move) sameMoveRefresh++; }
                else collisionWrites++;
            } else if ((old & (1L << 18)) != 0 && (old & (1L << 19)) == 0) proofBlocked++;
            else deeperBlocked++;
            return saved;
        }
        void resetCounters() {
            proofProbes=proofHits=proofStores=hintProbes=hintHits=missingEmpty=missingCollision=0;
            writes=emptyWrites=refreshWrites=collisionWrites=sameMoveRefresh=proofBlocked=deeperBlocked=0;
            Arrays.fill(hitAge,0); Arrays.fill(hitDepth,0); Arrays.fill(storeDepth,0);
        }
        Map<String,Object> snapshot(boolean occupancy) {
            var row = new LinkedHashMap<String,Object>();
            row.put("proofProbes",proofProbes); row.put("proofHits",proofHits); row.put("proofStores",proofStores);
            row.put("hintProbes",hintProbes); row.put("hintHits",hintHits);
            row.put("missingEmpty",missingEmpty); row.put("missingCollision",missingCollision);
            row.put("writes",writes); row.put("emptyWrites",emptyWrites); row.put("refreshWrites",refreshWrites);
            row.put("collisionWrites",collisionWrites); row.put("sameMoveRefresh",sameMoveRefresh);
            row.put("proofBlocked",proofBlocked); row.put("deeperBlocked",deeperBlocked);
            row.put("hitAge",hitAge.clone()); row.put("hitDepth",hitDepth.clone()); row.put("storeDepth",storeDepth.clone());
            if (occupancy) {
                long proof=0,hints=0,current=0; long[] depths=new long[256];
                for (long value:data) if ((value & (1L << 18)) != 0) {
                    if ((value & (1L << 19)) == 0) proof++;
                    else { hints++; depths[(int)(value & 255)]++; if ((value >>> 10 & 255) == generation) current++; }
                }
                row.put("entries",data.length); row.put("payloadBytes",24L*data.length);
                row.put("occupiedProof",proof); row.put("occupiedHints",hints);
                row.put("currentHints",current); row.put("occupiedDepth",depths);
            }
            return row;
        }
    }

    static ConstructionSearch configuration(String name, int workers) {
        var flags=FlatSearchConfig.candidate();
        int hash=name.equals("hash1")?1:64;
        if (name.startsWith("hints-off")) flags=flags.with(FlatSearchConfig.Feature.COMPLETED_HINTS,false);
        var aspiration=name.endsWith("full")?null:AspirationSearch.Policy.WIDEN16;
        if (!Set.of("baseline","hints-off","hash1","full","hints-off-full").contains(name))
            throw new IllegalArgumentException("Unknown profile: "+name);
        return new ConstructionSearch(flags,workers,hash,aspiration);
    }

    static void same(SearchDriverOutcome a, SearchDriverOutcome b) {
        if (a.nodes()!=b.nodes() || a.targetDepthCompleted()!=b.targetDepthCompleted()
                || a.terminalRoot()!=b.terminalRoot() || a.attemptedDepth()!=b.attemptedDepth()
                || a.iterationIncomplete()!=b.iterationIncomplete()
                || !Objects.equals(a.lastCompletedResult(),b.lastCompletedResult())
                || !a.diagnostics().equals(b.diagnostics()))
            throw new AssertionError("Observation changed request: "+a+" / "+b);
    }

    static Map<String,Object> request(SearchDriver driver, P3HistoryStudy.Root root, int depth, long budget) {
        var os=(com.sun.management.OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
        var mx=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        mx.setThreadAllocatedMemoryEnabled(true);
        long[] ids=mx.getAllThreadIds();
        long allocation=allocation(mx,ids),cpu=os.getProcessCpuTime(),gc=P3HistoryStudy.gcCount();
        long jit=ManagementFactory.getCompilationMXBean().getTotalCompilationTime(),started=System.nanoTime();
        var control=SearchControl.controlled(budget,started,60_000_000_000L,TimeSource.SYSTEM);
        var result=driver.search(new SearchRequest(root.board(),root.history(),depth,SearchObserver.NONE,control,true));
        long elapsed=System.nanoTime()-started,usedCpu=os.getProcessCpuTime()-cpu,bytes=allocation(mx,ids)-allocation;
        if (control.termination()==SearchTermination.TIME_LIMIT) throw new AssertionError("Operational request deadline");
        if (result.nodes()>budget || result.nodes()!=result.diagnostics().totalEnteredNodes()) throw new AssertionError("Whole-request accounting");
        if (driver.activeSearchThreads()!=0) throw new AssertionError("Undrained workers");
        if (result.lastCompletedResult()!=null) Phase2Oracle.legalPv(root.board(),result.lastCompletedResult().principalVariation());
        var row=new LinkedHashMap<String,Object>();
        row.put("outcome",result); row.put("termination",control.termination());
        row.put("elapsedNanos",elapsed); row.put("processCpuNanos",usedCpu); row.put("liveThreadAllocatedBytes",bytes);
        row.put("gcCollections",P3HistoryStudy.gcCount()-gc);
        row.put("compilationMillis",ManagementFactory.getCompilationMXBean().getTotalCompilationTime()-jit);
        row.put("heapUsed",ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
        return row;
    }

    /** model corpus output workers warmups rounds depth nodeBudget roots observe|cost profiles.
     * Fresh tables for each root in observe; reusable owners + newGame for clean cost.
     * Every output is CREATE_NEW. All requests include retries and aborted work. */
    public static void main(String[] args) throws Exception {
        if (args.length!=11) throw new IllegalArgumentException("model corpus output workers warmups rounds depth budget roots observe|cost profiles");
        var model=ConventionalNnueEngine.load(Path.of(args[0]));
        var evaluation=model.evaluation();
        var all=P3HistoryStudy.roots(Path.of(args[1]));
        int workers=Integer.parseInt(args[3]),warmups=Integer.parseInt(args[4]),rounds=Integer.parseInt(args[5]);
        int depth=Integer.parseInt(args[6]),count=Integer.parseInt(args[8]); long budget=Long.parseLong(args[7]);
        if (warmups<0 || rounds<1 || count<1 || count>all.size() || depth<1 || depth>ExactSearch.MAX_DEPTH || budget<1)
            throw new IllegalArgumentException("Invalid bounds");
        var profiles=List.of(args[10].split(","));
        for (String profile:profiles) configuration(profile,workers);
        var roots=all.subList(0,count); var gson=new GsonBuilder().create();
        try (var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            out.write(gson.toJson(Map.of("type","identity","arguments",args,
                    "model",model.identity(64),"java",System.getProperty("java.runtime.version"),
                    "jvm",ManagementFactory.getRuntimeMXBean().getInputArguments(),
                    "configuration",configuration("baseline",workers).identity(),
                    "modelIdentityNote","model.identity describes historical E008; configuration is authoritative Search composition")));
            out.newLine();out.flush();
            if (args[9].equals("observe")) {
                if (workers!=1 || warmups!=0 || rounds!=1) throw new IllegalArgumentException("Observation requires one worker, no warmup, one round");
                for (var root:roots) for (String profile:profiles) {
                    var config=configuration(profile,1);var table=new ObservedTable(config.hashMiB());
                    var adapter=ExactSearchAdapter.constructionResearch(evaluation,table,config.mechanisms());
                    var field=ExactSearchAdapter.class.getDeclaredField("exact");field.setAccessible(true);
                    var exact=(ExactSearch)field.get(adapter);
                    var attempts=new ArrayList<Map<String,Object>>();
                    var observed=config.aspiration()==null?adapter:new AspirationSearch(adapter,config.aspiration(),(d,a,b,r,n)-> {
                        var attempt=new LinkedHashMap<String,Object>();
                        attempt.put("depth",d);attempt.put("alpha",a);attempt.put("beta",b);attempt.put("outcome",r);
                        attempt.put("tableCumulative",table.snapshot(false));attempt.put("mechanisms",exact.constructionCounters());
                        attempts.add(attempt);
                    });
                    // Preserve the frozen WIDEN16 baseline independently of current production.
                    var plain=config.create(evaluation);
                    try (var plainDriver=new SearchDriver(plain);var observedDriver=new SearchDriver(observed)) {
                        var a=request(plainDriver,root,depth,budget);var b=request(observedDriver,root,depth,budget);
                        same((SearchDriverOutcome)a.get("outcome"),(SearchDriverOutcome)b.get("outcome"));
                        var row=new LinkedHashMap<String,Object>();row.put("type","observation");row.put("id",root.id());
                        row.put("historySize",root.history().size());row.put("profile",profile);row.put("neutral",true);
                        row.put("plain",a);row.put("observed",b);row.put("table",table.snapshot(true));row.put("attempts",attempts);
                        out.write(gson.toJson(row));out.newLine();out.flush();
                    }
                }
            } else if (args[9].equals("cost")) {
                SearchDriver[] drivers=new SearchDriver[profiles.size()];
                try {
                    for (int i=0;i<drivers.length;i++) {
                        String profile=profiles.get(i);long begin=System.nanoTime();
                        drivers[i]=new SearchDriver(configuration(profile,workers).create(evaluation));
                        out.write(gson.toJson(Map.of("type","construction","profile",profile,
                                "config",configuration(profile,workers).identity(),"elapsedNanos",System.nanoTime()-begin)));
                        out.newLine();
                    }
                    for (int round=-warmups;round<rounds;round++) for (int index=0;index<roots.size();index++)
                        for (int offset=0;offset<drivers.length;offset++) {
                            int i=Math.floorMod(round+index+offset,drivers.length);var root=roots.get(index);
                            long reset=System.nanoTime();drivers[i].newGame();reset=System.nanoTime()-reset;
                            var row=request(drivers[i],root,depth,budget);
                            row.put("type","request");row.put("id",root.id());row.put("round",round);
                            row.put("profile",profiles.get(i));row.put("resetNanos",reset);
                            out.write(gson.toJson(row));out.newLine();out.flush();
                        }
                } finally { for (var driver:drivers) if (driver!=null) driver.close(); }
            } else throw new IllegalArgumentException("Unknown mode");
            out.write(gson.toJson(Map.of("status","passed")));out.newLine();
        }
    }
    private static long allocation(com.sun.management.ThreadMXBean mx,long[] ids) {
        return Arrays.stream(mx.getThreadAllocatedBytes(ids)).filter(n->n>=0).sum();
    }
    private ConstructionTtStudy() {}
}
