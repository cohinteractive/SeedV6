package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.nnue.conventional.*;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.exact.ConstructionSearch;
import com.ohinteractive.seedv6.rules.GameHistory;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;

/** R057 actual-search trace replay and rotated full-request measurements. No chess clocks. */
public final class OneLifecycleAudit {
    private static volatile double sink;
    private static final String[] ROOTS = {
        "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
        "r1bq1rk1/ppp2ppp/2np1n2/2b1p3/2B1P3/2NP1N2/PPP2PPP/R1BQ1RK1 w - - 4 7",
        "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1",
        "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1",
        "4k3/P7/8/8/8/8/7p/4K3 b - - 0 1",
        "6k1/8/8/3q4/4Q3/8/8/6K1 w - - 0 1"
    };
    private static final com.sun.management.ThreadMXBean ALLOC =
        (com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
    private static long allocated() { return ALLOC.getThreadAllocatedBytes(Thread.currentThread().threadId()); }
    private record Event(int kind, int state, int source, int ply, long[] parent, long[] board) {}
    private static final class Trace {
        final ArrayList<Event> events = new ArrayList<>();
        final ArrayList<Integer> capacities = new ArrayList<>();
        final SearchEvaluation base;
        Trace(SearchEvaluation base) { this.base = base; }
        final class Recorder implements SearchEvaluation.ResearchCpLifecycle {
            final int id; final SearchEvaluation.State delegate;
            Recorder(int capacity) { id = capacities.size(); capacities.add(capacity); delegate = base.newState(capacity); }
            void record(int kind, int source, int ply, long[] parent, long[] board) {
                if (events.size() >= 250000) throw new IllegalStateException("Trace bound");
                events.add(new Event(kind, id, source, ply, parent == null ? null : parent.clone(), board.clone()));
            }
            public void initialize(long[] board, int ply) { record(0, -1, ply, null, board); delegate.initialize(board, ply); }
            public void child(long[] parent, long[] child, int ply) { record(1, -1, ply, parent, child); delegate.child(parent, child, ply); }
            public void initializeFrom(long[] board, int ply, SearchEvaluation.ResearchCpLifecycle source) {
                var other = (Recorder)source; record(2, other.id, ply, null, board); delegate.initializeFrom(board, ply, other.delegate);
            }
            public double evaluate(long[] board, int ply) { record(3, -1, ply, null, board); return delegate.evaluate(board, ply); }
        }
        SearchEvaluation definition() { return SearchEvaluation.researchCentipawnsStateful(Recorder::new); }
    }
    private static SearchEvaluation.State[] states(SearchEvaluation definition, Trace trace) {
        var states = new SearchEvaluation.State[trace.capacities.size()];
        for (int i = 0; i < states.length; i++) states[i] = definition.newState(trace.capacities.get(i));
        return states;
    }
    private static long replay(Event[] events, SearchEvaluation.State[] states) {
        long sum = 0;
        for (var e : events) switch (e.kind) {
            case 0 -> states[e.state].initialize(e.board, e.ply);
            case 1 -> states[e.state].child(e.parent, e.board, e.ply);
            case 2 -> states[e.state].initializeFrom(e.board, e.ply, states[e.source]);
            case 3 -> sum += states[e.state].evaluate(e.board, e.ply);
            default -> throw new AssertionError();
        }
        sink = sum; return sum;
    }
    private static Map<String,Object> verifyEveryEvaluation(Trace trace, Event[] events,
            SearchEvaluation.State[] reference, SearchEvaluation.State[] optimized,
            IncrementalOne model, java.util.function.ToDoubleFunction<long[]> full) {
        var frames=new IncrementalOne.Accumulator[trace.capacities.size()][];
        for(int i=0;i<frames.length;i++){frames[i]=new IncrementalOne.Accumulator[trace.capacities.get(i)];for(int p=0;p<frames[i].length;p++)frames[i][p]=model.newAccumulator();}
        double maximum=0;int evaluated=0,rawRoundedDifferences=0;
        for(var e:events)switch(e.kind){
            case 0 -> {reference[e.state].initialize(e.board,e.ply);optimized[e.state].initialize(e.board,e.ply);frames[e.state][e.ply].rebuild(e.board);}
            case 1 -> {reference[e.state].child(e.parent,e.board,e.ply);optimized[e.state].child(e.parent,e.board,e.ply);frames[e.state][e.ply+1].update(e.parent,e.board,frames[e.state][e.ply]);}
            case 2 -> {reference[e.state].initializeFrom(e.board,e.ply,reference[e.source]);optimized[e.state].initializeFrom(e.board,e.ply,optimized[e.source]);frames[e.state][e.ply].copyFrom(e.board,frames[e.source][e.ply]);}
            case 3 -> {
                double expected=full.applyAsDouble(e.board),actual=frames[e.state][e.ply].scoreCp(e.board),error=Math.abs(expected-actual);
                if(!Double.isFinite(error)||error>1e-8+1e-12*Math.abs(expected))throw new IllegalStateException("Per-event numeric mismatch at evaluation "+evaluated);
                maximum=Math.max(maximum,error);int rounded=reference[e.state].evaluate(e.board,e.ply);
                int raw=(actual<0?-1:1)*(int)Math.round(Math.min(Math.abs(actual),32511));if(raw!=rounded)rawRoundedDifferences++;
                if(optimized[e.state].evaluate(e.board,e.ply)!=rounded)throw new IllegalStateException("Per-event rounded search mismatch at evaluation "+evaluated);
                evaluated++;
            }
            default -> throw new AssertionError();
        }
        return Map.of("evaluations",evaluated,"maximum_absolute_cp_error",maximum,"raw_rounded_differences",rawRoundedDifferences,
                "actual_search_rounded_differences",0,"absolute_tolerance_cp",1e-8,"relative_tolerance",1e-12);
    }
    private static Map<String,Object> measure(String phase, int operations, Runnable action) {
        long bytes = allocated(), start = System.nanoTime(); action.run(); long ns = System.nanoTime() - start;
        long allocation = allocated() - bytes;
        var result = new LinkedHashMap<String,Object>(); result.put("phase", phase); result.put("operations", operations);
        result.put("ns", ns); result.put("ns_per_operation", (double)ns / operations);
        result.put("allocated_bytes", allocation); return result;
    }
    private static Map<String,Object> search(SearchEvaluation definition, String fen, int nodes) throws Exception {
        var board = Board.fromFen(fen); long allStart = System.nanoTime(); long before = allocated();
        try (var driver = new SearchDriver(ConstructionSearch.v1(1, 16, AspirationSearch.Policy.ADAPTIVE16).create(definition))) {
            long start = System.nanoTime();
            var control = SearchControl.controlled(nodes, start, -1L, TimeSource.SYSTEM);
            var result = driver.search(new SearchRequest(board, GameHistory.initial(board), 64, control));
            long elapsed = System.nanoTime() - start;
            if (result.nodes() != control.nodes() || result.nodes() > nodes || driver.activeSearchThreads() != 0)
                throw new IllegalStateException("Node accounting/release");
            var completed = result.lastCompletedResult(); if (completed == null) throw new IllegalStateException("No completed iteration");
            long[] replay = board.clone(), legal = new long[256], scratch = new long[6];
            for (long move : completed.principalVariation()) {
                int count = Gen.genAll(replay[0], replay[1], replay[2], replay[3], (int)replay[4], replay[5], true, legal, scratch);
                boolean found = false; for (int i = 0; i < count; i++) if (legal[i] == move) found = true;
                if (!found) throw new IllegalStateException("Illegal PV");
                long[] child = new long[6]; Board.makeMoveInto(replay[0], replay[1], replay[2], replay[3], (int)replay[4], replay[5], move, child); replay = child;
            }
            var row = new LinkedHashMap<String,Object>(); row.put("fen", fen); row.put("nodes", result.nodes());
            row.put("search_ns", elapsed); row.put("nodes_per_second", result.nodes() * 1e9 / elapsed);
            row.put("including_construction_ns", System.nanoTime() - allStart); row.put("allocated_bytes", allocated() - before);
            row.put("best_move", completed.bestMove()); row.put("score", completed.score()); row.put("depth", completed.depth());
            row.put("termination", control.termination().name()); return row;
        }
    }
    private static void writeTrace(Path path, Trace trace) throws Exception {
        try (var out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path, StandardOpenOption.CREATE_NEW)))) {
            out.writeUTF("R057_SEARCH_TRACE_V1"); out.writeInt(trace.capacities.size());
            for (int capacity : trace.capacities) out.writeInt(capacity);
            out.writeInt(trace.events.size());
            for (var e : trace.events) {
                out.writeInt(e.kind); out.writeInt(e.state); out.writeInt(e.source); out.writeInt(e.ply);
                out.writeBoolean(e.parent != null); if (e.parent != null) for (long x : e.parent) out.writeLong(x);
                for (long x : e.board) out.writeLong(x);
            }
        }
    }
    public static void main(String[] args) throws Exception {
        PairObjectiveDiagnostic.permission();
        if (!ALLOC.isThreadAllocatedMemorySupported()) throw new IllegalStateException("Allocation measurement unsupported");
        ALLOC.setThreadAllocatedMemoryEnabled(true);
        Path directory = Path.of(args[0]); Files.createDirectory(directory);
        String[] roots = args.length > 1 ? Files.readAllLines(Path.of(args[1])).toArray(String[]::new) : ROOTS;
        if (roots.length < 2 || roots.length > 40) throw new IllegalArgumentException("Root allocation");
        boolean trained = args.length > 2;
        Path modelPath = trained ? Path.of(args[2]) : directory.resolve("untrained.state");
        java.util.function.Supplier<java.util.function.ToDoubleFunction<long[]>> fullFactory;
        IncrementalOne fast; long before, start, initializationNs, initializationBytes;
        int width, rank, parameters;
        if (trained) {
            TrainableOne model; try (var in = Files.newInputStream(modelPath)) { model = TrainableOne.read(in); }
            width=model.width; rank=model.rank; parameters=model.parameters;
            before=allocated(); start=System.nanoTime(); fast=new IncrementalOne(model);
            initializationNs=System.nanoTime()-start; initializationBytes=allocated()-before;
            fullFactory=() -> { var features=new TrainableOne.Features(); var workspace=model.new Workspace();
                return board -> 400*model.logit(model.encode(board,features),workspace,true); };
        } else {
            var model = new LowRankRelationNetwork(false, .01, 71);
            var random = new Random(97); for (int i = 0; i < model.weights.length; i++) model.weights[i] = .3 * (2 * random.nextDouble() - 1);
            try (var out = Files.newOutputStream(modelPath, StandardOpenOption.CREATE_NEW)) { model.write(out); }
            width=LowRankRelationNetwork.WIDTH; rank=LowRankRelationNetwork.RANK; parameters=model.weights.length;
            before=allocated(); start=System.nanoTime(); fast=new IncrementalOne(model);
            initializationNs=System.nanoTime()-start; initializationBytes=allocated()-before;
            fullFactory=() -> { var workspace=new LowRankRelationNetwork.Workspace(); return board -> model.scoreCp(board,workspace); };
        }
        var reference = SearchEvaluation.researchCentipawnsFullRecompute(fullFactory);
        var cp64 = ConventionalNnueEngine.load(Path.of("build-assets/nnue-candidate/conventional-cp64.cnnue"));
        SearchEvaluation[] definitions = {reference, fast.searchDefinition(), cp64.evaluation()};
        String[] names = {trained ? "R057_ONE_FULL" : "R056_ONE_FULL", "R057_ONE_INCREMENTAL", "CP64"};
        // Record genuine main/qsearch transitions once; freeze before any replay comparisons.
        var trace = new Trace(definitions[1]); var traced = trace.definition();
        for (String fen : roots) search(traced, fen, 1500);
        Path tracePath = directory.resolve("search-trace.bin"); writeTrace(tracePath, trace);
        Event[] events = trace.events.toArray(Event[]::new); int[] counts = new int[4];
        for (var e : events) counts[e.kind]++;
        var replayStates = new SearchEvaluation.State[3][]; var stateCosts = new ArrayList<Object>();
        for (int mode = 0; mode < 3; mode++) {
            before = allocated(); start = System.nanoTime(); replayStates[mode] = states(definitions[mode], trace);
            stateCosts.add(Map.of("mode", names[mode], "initialization_ns", System.nanoTime()-start, "allocated_bytes", allocated()-before));
        }
        for (int warm = 0; warm < 12; warm++) for (int mode = 0; mode < 3; mode++) replay(events, replayStates[(mode+warm)%3]);
        long referenceSum = replay(events, replayStates[0]);
        if (referenceSum != replay(events, replayStates[1])) throw new IllegalStateException("Rounded lifecycle checksum mismatch");
        var numerical=verifyEveryEvaluation(trace,events,replayStates[0],replayStates[1],fast,fullFactory.get());
        var lifecycle = new ArrayList<Object>();
        for (int round = 0; round < 9; round++) for (int order = 0; order < 3; order++) {
            PairObjectiveDiagnostic.permission(); int mode = (round+order)%3;
            var row = measure("complete_search_trace", events.length, () -> replay(events, replayStates[mode]));
            row.put("mode", names[mode]); row.put("round", round); row.put("evaluations", counts[3]);
            row.put("ns_per_evaluation_including_all_state_work", ((Number)row.get("ns")).doubleValue()/counts[3]); lifecycle.add(row);
        }
        // Same trace transitions for component costs; material/piece-count changes are included.
        Event[] allTransitions = trace.events.stream().filter(e -> e.kind == 1).toArray(Event[]::new);
        Event[] transitions = new Event[Math.min(4096, allTransitions.length)];
        for (int i = 0; i < transitions.length; i++) transitions[i] = allTransitions[(int)((long)i * (allTransitions.length-1) / (transitions.length-1))];
        int n = transitions.length;
        var oneParents = new IncrementalOne.Accumulator[n]; var oneChildren = new IncrementalOne.Accumulator[n];
        var cpParents = new ConventionalAccumulator[n]; var cpChildren = new ConventionalAccumulator[n];
        for (int i = 0; i < n; i++) {
            oneParents[i] = fast.newAccumulator(); oneParents[i].rebuild(transitions[i].parent);
            oneChildren[i] = fast.newAccumulator(); oneChildren[i].update(transitions[i].parent, transitions[i].board, oneParents[i]);
            cpParents[i] = cp64.model().newAccumulator(); cpParents[i].rebuild(transitions[i].parent);
            cpChildren[i] = cp64.model().newAccumulator(); cpChildren[i].update(transitions[i].parent, transitions[i].board, cpParents[i]);
        }
        var fullScorer = fullFactory.get(); var oneFull = fast.newAccumulator();
        var cpFull = cp64.model().newAccumulator();
        Runnable[][] actions = new Runnable[3][4];
        actions[0][0] = () -> { double sum=0; for (var e:transitions) sum+=fullScorer.applyAsDouble(e.board); sink=sum; };
        actions[1][0] = () -> { double sum=0; for (var e:transitions) { oneFull.rebuild(e.board); sum+=oneFull.scoreCp(e.board); } sink=sum; };
        actions[2][0] = () -> { double sum=0; for (var e:transitions) { cpFull.rebuild(e.board);sum+=cpFull.evaluate(e.board); } sink=sum; };
        actions[1][1] = () -> { for(int i=0;i<n;i++) oneChildren[i].update(transitions[i].parent,transitions[i].board,oneParents[i]); };
        actions[2][1] = () -> { for(int i=0;i<n;i++) cpChildren[i].update(transitions[i].parent,transitions[i].board,cpParents[i]); };
        actions[1][2] = () -> { double sum=0;for(int i=0;i<n;i++)sum+=oneChildren[i].scoreCp(transitions[i].board);sink=sum; };
        actions[2][2] = () -> { double sum=0;for(int i=0;i<n;i++)sum+=cpChildren[i].evaluate(transitions[i].board);sink=sum; };
        actions[1][3] = () -> { double sum=0;for(int i=0;i<n;i++){oneChildren[i].update(transitions[i].parent,transitions[i].board,oneParents[i]);sum+=oneChildren[i].scoreCp(transitions[i].board);}sink=sum; };
        actions[2][3] = () -> { double sum=0;for(int i=0;i<n;i++){cpChildren[i].update(transitions[i].parent,transitions[i].board,cpParents[i]);sum+=cpChildren[i].evaluate(transitions[i].board);}sink=sum; };
        for(int warm=0;warm<12;warm++)for(var mode:actions)for(var action:mode)if(action!=null)action.run();
        var components = new ArrayList<Object>(); String[] phases={"full_evaluation","update","readout","update_plus_readout"};
        for(int round=0;round<9;round++)for(int order=0;order<3;order++){
            int mode=(round+order)%3;
            for(int phase=0;phase<4;phase++)if(actions[mode][phase]!=null){
                var row=measure(phases[phase],n,actions[mode][phase]);row.put("mode",names[mode]);row.put("round",round);components.add(row);
            }
        }
        // Whole-search timing excludes instrumentation; matching node budgets, distinct evaluator trees.
        for(int mode=0;mode<3;mode++)search(definitions[mode],roots[1],3000);
        var searches=new ArrayList<Object>();
        for(int round=0;round<3;round++)for(int order=0;order<3;order++){
            int mode=(round+order)%3;
            for(String fen:roots){PairObjectiveDiagnostic.permission();var row=search(definitions[mode],fen,10000);row.put("mode",names[mode]);row.put("round",round);searches.add(row);}
        }
        var result=new LinkedHashMap<String,Object>();result.put("experiment","R057");result.put("untrained",!trained); result.put("width",width); result.put("rank",rank); result.put("parameters",parameters);
        result.put("model_sha256",PairCoverageDiagnostic.sha(modelPath));
        result.put("trace_sha256",PairCoverageDiagnostic.sha(tracePath));result.put("trace_events",events.length);result.put("trace_counts_initialize_child_copy_evaluate",counts);
        result.put("roots", roots); if(args.length>1) result.put("roots_sha256",PairCoverageDiagnostic.sha(Path.of(args[1])));
        result.put("cp64_head_backend",cp64.model().headBackend());result.put("jvm_arguments",ManagementFactory.getRuntimeMXBean().getInputArguments());
        result.put("state_initialization",stateCosts);result.put("immutable_initialization_ns",initializationNs);result.put("immutable_allocated_bytes",initializationBytes);
        result.put("immutable_payload_bytes",fast.immutablePayloadBytes());result.put("accumulator_payload_bytes",fast.newAccumulator().payloadBytes());
        result.put("lifecycle",lifecycle);result.put("components",components);result.put("search",searches);
        result.put("per_evaluation_numerical_equivalence",numerical);
        result.put("settings","one worker; TT16MiB; common flags262143/ADAPTIVE16; no clocks/ponder/book/tablebases; rotated orders; double ONE vs quantized CP64");
        result.put("limitations","One JVM; trace captured using the bound ONE checkpoint. This timing study makes no quality/strength or equal-time inference. Component parents prebuilt; full trace includes actual root rebuilds and main/qsearch transitions (explicit copies only if nonzero in event counts). Payload memory excludes JVM headers; allocated bytes include them. CP64 full uses its normal accumulator rebuild plus head.");
        Files.writeString(directory.resolve("result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result)+"\n",StandardOpenOption.CREATE_NEW);
        System.out.println("R057 lifecycle audit complete: "+events.length+" events; result="+directory.resolve("result.json"));
    }
}
