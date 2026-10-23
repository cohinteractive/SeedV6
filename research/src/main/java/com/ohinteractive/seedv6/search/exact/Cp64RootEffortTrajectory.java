package com.ohinteractive.seedv6.search.exact;

import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;

/** Read existing request-private ordering data at resolved W1 iteration boundaries.
 * Deliberately research-only reflection: no production accessors, counters or policy.
 * The frozen source binding and explicit field/type checks define this diagnostic.
 * Root scores are fail-soft ordering observations, not exact alternative values.
 * Effort covers the final resolved attempt; aspiration retry totals are separate. */
public final class Cp64RootEffortTrajectory {
    static Field field(Class<?> type, String name) throws ReflectiveOperationException {
        var field = type.getDeclaredField(name); field.setAccessible(true); return field;
    }
    record RootEffort(long move, long nodes, int heuristicScore) {}
    record Boundary(int depth, long bestMove, boolean selective, long allAttemptNodes,
                    int cachedDepth, int legalRootMoves, List<RootEffort> finalAttemptEffort) {}

    static final class Reader implements SingleDepthSearch {
        final SingleDepthSearch owner;
        final IterationReuse reuse;
        final Field count, cachedDepth, pending, effort, scores;
        final List<Boundary> boundaries = new ArrayList<>();
        Reader(SingleDepthSearch owner) throws ReflectiveOperationException {
            if(!(owner instanceof AspirationSearch)) throw new AssertionError("Expected accepted aspiration composition");
            this.owner = owner;
            var adapter = field(AspirationSearch.class, "owner").get(owner);
            if(!(adapter instanceof ExactSearchAdapter)) throw new AssertionError("W1 owner required");
            var exact = field(ExactSearchAdapter.class, "exact").get(adapter);
            reuse = (IterationReuse)field(ExactSearch.class, "iterationReuse").get(exact);
            if(reuse == null || reuse.policy != IterationReuse.EFFORT) throw new AssertionError("Accepted effort ordering");
            count = field(IterationReuse.class, "pendingCount"); cachedDepth = field(IterationReuse.class, "completedDepth");
            pending = field(IterationReuse.class, "pending"); effort = field(IterationReuse.class, "effort");
            scores = field(IterationReuse.class, "scores");
        }
        @Override public SearchResult search(SearchRequest request) {
            var result = owner.search(request);
            if(result.completed()) {
                if(owner.activeSearchThreads()!=0) throw new AssertionError("Boundary must be drained");
                try {
                    int n = count.getInt(reuse), depth = cachedDepth.getInt(reuse);
                    long[] moves = (long[])pending.get(reuse), nodes = (long[])effort.get(reuse);
                    int[] values = (int[])scores.get(reuse);
                    if(n<0 || n>moves.length || (n>0 && depth!=result.depth()))
                        throw new AssertionError("Unavailable or stale cache cannot be labelled completed effort");
                    var entries = new ArrayList<RootEffort>(); var unique = new HashSet<Long>();
                    for(int i=0; i<n; i++) {
                        if(nodes[i]<0 || !unique.add(moves[i])) throw new AssertionError("Malformed root effort");
                        entries.add(new RootEffort(moves[i], nodes[i], values[i]));
                    }
                    boundaries.add(new Boundary(result.depth(), result.bestMove(), result.selective(),
                        result.nodes(), depth, result.legalRootMoves(), List.copyOf(entries)));
                } catch(IllegalAccessException failure) { throw new IllegalStateException(failure); }
            }
            return result;
        }
        @Override public int maxSupportedDepth() { return owner.maxSupportedDepth(); }
        @Override public int activeSearchThreads() { return owner.activeSearchThreads(); }
        @Override public void beginRequest() { boundaries.clear(); owner.beginRequest(); }
        @Override public void endRequest() { owner.endRequest(); }
        @Override public void newGame() { boundaries.clear(); owner.newGame(); }
        @Override public void close() { owner.close(); }
    }

    /** Same arguments as Cp64TimeTrajectory; deterministic neutrality is mandatory. */
    public static void main(String[] args) throws Exception {
        if(args.length!=7) throw new IllegalArgumentException("model corpus NEW-output depth budget rounds roots");
        var model = ConventionalNnueEngine.load(Path.of(args[0])); var roots = P3HistoryStudy.roots(Path.of(args[1]));
        int depth=Integer.parseInt(args[3]), rounds=Integer.parseInt(args[5]), count=Integer.parseInt(args[6]);
        long budget=Long.parseLong(args[4]);
        if(depth<2 || depth>64 || budget<1 || rounds<1 || count<1 || count>roots.size()) throw new IllegalArgumentException("Bounds");
        var config=BaselineEngine.configuration(model.evaluation(),1,64);
        var reader=new Reader(config.create(model.evaluation())); var gson=new GsonBuilder().create();
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW);
            var plain=new SearchDriver(config.create(model.evaluation())); var observed=new SearchDriver(reader)) {
            out.write(gson.toJson(Map.of("type","identity","arguments",args,"configuration",config.identity(),
                "networkSha256",ConventionalNnueEngine.SHA256,"payload",model.model().payloadSha256(),
                "scope","Read-only final-attempt effort; inclusive aspiration work remains in allAttemptNodes; no confidence/proof interpretation")));
            out.newLine();out.flush();
            for(int i=0;i<4;i++) {
                Cp64TimeTrajectory.run(plain,roots.get(i%count),40,100000,false);
                Cp64TimeTrajectory.run(observed,roots.get(i%count),40,100000,true);
            }
            for(int round=0;round<rounds;round++) for(int i=0;i<count;i++) {
                Cp64TimeTrajectory.Run a,b;
                if((round+i)%2==0) {
                    a=Cp64TimeTrajectory.run(plain,roots.get(i),depth,budget,false);
                    b=Cp64TimeTrajectory.run(observed,roots.get(i),depth,budget,true);
                } else {
                    b=Cp64TimeTrajectory.run(observed,roots.get(i),depth,budget,true);
                    a=Cp64TimeTrajectory.run(plain,roots.get(i),depth,budget,false);
                }
                Cp64TimeTrajectory.same(a,b);
                out.write(gson.toJson(Map.of("type","effort-trajectory","id",roots.get(i).id(),"round",round,
                    "neutral",true,"plain",a,"observed",b,"rootEffort",reader.boundaries)));
                out.newLine();out.flush();
            }
            out.write(gson.toJson(Map.of("status","passed","neutralComparisons",rounds*count)));out.newLine();
        }
    }
    private Cp64RootEffortTrajectory() {}
}
