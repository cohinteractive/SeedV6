package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.ApplicationVersion;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.manage.ClockPolicy;
import com.ohinteractive.seedv6.search.tt.TTable;

/** MW-R001 research-owned observation of successful admissions and root-job startup.
 * No production instrumentation or scheduling change. Timing is diagnostic only:
 * instrumentation and thread scheduling can alter an SMP search at equal nodes. */
public final class P4MultiworkerStudy {
    enum Architecture { ROOT_REUSE, WHOLE_ROOT_RACE, WHOLE_ROOT_SHARING }
    static final class Admissions implements ExactEvaluator {
        final ExactEvaluator delegate;
        long nodes, qnodes, reentries, evaluations, initializations;
        long firstInitialization, firstJobNodes = -1;
        Set<Long> boardKeys;
        private long[] keysAtPly;

        Admissions(ExactEvaluator delegate) { this.delegate = delegate; }
        Admissions observeBoardKeys() {
            boardKeys=new HashSet<>(); keysAtPly=new long[ExactSearch.MAX_DEPTH+1]; return this;
        }
        @Override public void initialize(long[] board) {
            if (initializations++ == 0) firstInitialization = System.nanoTime();
            else if (firstJobNodes < 0) firstJobNodes = nodes;
            if(boardKeys!=null) keysAtPly[0]=board[Board.KEY];
            delegate.initialize(board);
        }
        @Override public void child(long[] parent, long[] child, int ply, int qply) {
            nodes++;
            if(boardKeys!=null) { keysAtPly[ply+1]=child[Board.KEY]; boardKeys.add(child[Board.KEY]); }
            if (qply >= 0) qnodes++;
            delegate.child(parent, child, ply, qply);
        }
        @Override public void child(long[] parent, long[] child, int ply) {
            child(parent, child, ply, -1);
        }
        @Override public void reenter(int ply) {
            nodes++; reentries++; if(boardKeys!=null) boardKeys.add(keysAtPly[ply]); delegate.reenter(ply);
        }
        @Override public int evaluate(long[] board, int ply) { evaluations++; return delegate.evaluate(board, ply); }
        void reset() {
            nodes = qnodes = reentries = evaluations = initializations = firstInitialization = 0;
            firstJobNodes = -1;
            if(boardKeys!=null) boardKeys.clear();
        }
        Map<String, Object> snapshot() {
            return Map.of("nodes", nodes, "qnodes", qnodes, "reentries", reentries,
                    "evaluations", evaluations, "initializations", initializations,
                    "firstJobNodes", firstJobNodes < 0 ? nodes : firstJobNodes);
        }
    }

    /** model corpus new-output workers rounds depth node-cap; every request starts a fresh TT.
     * Use W2/W4 to study the actual production ROOT_REUSE path. W1 uses the independent
     * production adapter and is qualified separately, not relabelled ParallelSearch(1). */
    public static void main(String[] args) throws Exception {
        if (args.length < 7 || args.length > 9)
            throw new IllegalArgumentException("model corpus output workers rounds depth node-cap [architecture] [board-keys]");
        var architecture = args.length >= 8 ? Architecture.valueOf(args[7]) : Architecture.ROOT_REUSE;
        boolean coverage=args.length==9;
        if(coverage && !args[8].equals("board-keys")) throw new IllegalArgumentException("Unknown observation");
        int workers = Integer.parseInt(args[3]), rounds = Integer.parseInt(args[4]), depth = Integer.parseInt(args[5]);
        long cap = Long.parseLong(args[6]);
        if (workers < 2 || workers > 4 || rounds < 1 || rounds > 4 || depth < 3 || depth > 16
                || cap < 1 || cap > 1_000_000) throw new IllegalArgumentException("Unbounded diagnostic");
        var model = ConventionalNnueEngine.load(Path.of(args[0]));
        var roots = P3HistoryStudy.roots(Path.of(args[1]));
        if (roots.isEmpty() || roots.size() > 64) throw new IllegalArgumentException("Root count");
        var config = BaselineEngine.configuration(workers, 64, BaselineEngine.SearchProfile.TC_P1).mechanisms();
        var gson = new GsonBuilder().create();
        long totalNodes = 0;
        int requests = 0;
        try (var out = Files.newBufferedWriter(Path.of(args[2]), StandardOpenOption.CREATE_NEW)) {
            out.write(gson.toJson(Map.of("type", "identity", "source", ApplicationVersion.load().sourceRevision(),
                    "build", ApplicationVersion.load().build(), "flags", config.flags(), "args", args,
                    "java", System.getProperty("java.runtime.version"), "availableProcessors", Runtime.getRuntime().availableProcessors(),
                    "architecture", architecture)));
            out.newLine(); out.flush();
            for (int round = 0; round < rounds; round++) for (var root : roots) {
                var admissions = new Admissions[workers];
                var requestKeys=new ArrayList<Set<Long>>();
                if(coverage) for(int i=0;i<workers;i++) requestKeys.add(new HashSet<>());
                var table = new TTable(64);
                java.util.function.IntFunction<ExactEvaluator> evaluators = i -> {
                    var a=new Admissions(ExactEvaluator.from(model.evaluation()));
                    return admissions[i]=coverage?a.observeBoardKeys():a;
                };
                var owner = switch(architecture) {
                    case ROOT_REUSE -> ParallelSearch.constructionResearch(workers,model.evaluation(),table,config,evaluators);
                    case WHOLE_ROOT_RACE -> ParallelSearch.constructionWholeRootResearch(workers,model.evaluation(),table,config,evaluators);
                    case WHOLE_ROOT_SHARING -> ParallelSearch.constructionWholeRootSharingResearch(workers,model.evaluation(),table,config,evaluators);
                };
                var attempts = new ArrayList<Map<String, Object>>();
                AspirationSearch.AttemptObserver observer = (d, alpha, beta, result, elapsed) -> {
                    long nodes = 0, qnodes = 0, firstHelper = Long.MAX_VALUE;
                    var snapshots = new ArrayList<Map<String, Object>>();
                    var work = owner.workerWork();
                    for (int i = 0; i < workers; i++) {
                        var a = admissions[i];
                        if (a.nodes != work[i].nodes() || a.qnodes != work[i].qnodes()
                                || a.evaluations != work[i].evaluations()) throw new AssertionError("Callback/private accounting");
                        nodes += a.nodes; qnodes += a.qnodes; snapshots.add(a.snapshot());
                        if(coverage) requestKeys.get(i).addAll(a.boardKeys);
                        if (i > 0 && a.initializations > 0) firstHelper = Math.min(firstHelper, a.firstInitialization);
                    }
                    if (nodes != result.nodes() || nodes != result.diagnostics().totalEnteredNodes()
                            || owner.activeSearchThreads() != 0) throw new AssertionError("Accounting/drain");
                    var row = new LinkedHashMap<String, Object>();
                    row.put("depth", d); row.put("alpha", alpha); row.put("beta", beta);
                    row.put("result", result); row.put("elapsedNanos", elapsed); row.put("workers", snapshots);
                    row.put("workerCompletion",work);
                    row.put("completedLearningTransfers",owner.completedLearningTransfers());
                    row.put("firstHelperDelayNanos", firstHelper == Long.MAX_VALUE ? null : firstHelper - admissions[0].firstInitialization);
                    // ROOT_REUSE split depths: first restricted root job. Whole-root race
                    // and depth1/2: entire owner invocation, NOT serialized first-job work.
                    row.put("firstOwnerJobNodes", admissions[0].firstJobNodes < 0 ? admissions[0].nodes : admissions[0].firstJobNodes);
                    attempts.add(row);
                    for (var a : admissions) a.reset();
                };
                long[] saved = root.board().clone();
                try (var driver = new SearchDriver(ClockPolicy.BOUNDARY2.apply(
                        new AspirationSearch(owner, AspirationSearch.Policy.ADAPTIVE16, observer)))) {
                    var measurement = ConstructionTtStudy.request(driver, root, depth, cap);
                    var outcome = (SearchDriverOutcome) measurement.get("outcome");
                    long observed = attempts.stream().mapToLong(a -> ((SearchResult)a.get("result")).nodes()).sum();
                    if (observed != outcome.nodes() || !Arrays.equals(saved, root.board())) throw new AssertionError("Request/restoration");
                    totalNodes += outcome.nodes(); requests++;
                    var row=new LinkedHashMap<String,Object>();
                    row.put("type","request"); row.put("root",root.id()); row.put("round",round);
                    row.put("measurement",measurement); row.put("attempts",attempts);
                    if(coverage) row.put("boardKeyCoverage",boardKeyCoverage(requestKeys));
                    out.write(gson.toJson(row));
                    out.newLine(); out.flush();
                }
            }
            out.write(gson.toJson(Map.of("type", "summary", "status", "passed", "requests", requests,
                    "nodes", totalNodes, "activeHelpersAfterClose", Thread.getAllStackTraces().keySet().stream()
                            .filter(t -> t.isAlive() && t.getName().equals("seedv6-search-helper")).count())));
            out.newLine();
        }
    }
    /** Admitted callback board-key overlap, not unique history/depth/window domains
     * and not a count of useless work. Includes synthetic and qsearch transitions. */
    static Map<String,Object> boardKeyCoverage(List<Set<Long>> workers) {
        var union=new HashSet<Long>(); long sum=0;
        var shared=new int[workers.size()][workers.size()];
        for(int i=0;i<workers.size();i++) {
            var a=workers.get(i); sum+=a.size(); union.addAll(a);
            for(int j=0;j<=i;j++) {
                int count=0; for(long key:a) if(workers.get(j).contains(key)) count++;
                shared[i][j]=shared[j][i]=count;
            }
        }
        return Map.of("uniqueBoardKeys",union.size(),"sumWorkerUniqueBoardKeys",sum,
                "crossWorkerExcessMemberships",sum-union.size(),"pairwiseSharedBoardKeys",shared,
                "semantics","64-bit board Zobrist keys from admitted child/reentry callbacks; no history/depth/window identity; collisions possible; overlap is not wasted work");
    }
    private P4MultiworkerStudy() {}
}
