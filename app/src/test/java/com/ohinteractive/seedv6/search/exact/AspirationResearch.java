package com.ohinteractive.seedv6.search.exact;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.SearchDriver;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;
import com.ohinteractive.seedv6.tools.perft.DefaultPerftPositionLibrary;
import com.ohinteractive.seedv6.tools.search.ExactSearchHarness;
import com.ohinteractive.seedv6.tools.search.SearchBenchmark;

/**
 * Explicit SR-006B experiment, excluded from routine tests and shipped classes.
 * Run :app:aspirationResearch -PresearchArgs="--output=build/sr006b".
 * Defaults: 33 repository-derived FENs, depth 8, eight extended to 10, three
 * repeats after two depth-5 warmup passes. Rotates policy order per position
 * and repetition. Every request owns a fresh default 64 MiB-requested TT.
 * Construction, file IO and exact PV validation are outside timed requests.
 * Raw CSVs in the requested build directory are measurement outputs, not canon.
 * Node totals count admitted children, as production SearchDriver does (PVS
 * re-entries are not separate child admissions). No Search behaviour is changed.
 */
public final class AspirationResearch {
    // Existing ExactSearchTest.unavoidableLossChoosesFourPlyMateOverTwoPlyMate fixture.
    static final String FORCED_LOSS = "6k1/8/5K2/8/8/8/8/Q7 b - - 0 1";
    record Position(String name, String fen, String source, boolean deep) {}
    record Run(List<AspirationResearchSearch.Iteration> iterations, List<SearchResult> published,
               long nodes, long nanos, int completedDepth) {}
    static final int[] WIDTHS = {0, 512, 628};

    static List<Position> positions() {
        var selected = new LinkedHashMap<String, Position>();
        for(var p : ExactSearchHarness.orderingPositions())
            selected.putIfAbsent(p.fen(), new Position(p.name(), p.fen(), "orderingPositions", true));
        for(var p : ExactSearchHarness.positions()) if(p.name().equals("endgame") || p.name().equals("mate"))
            selected.putIfAbsent(p.fen(), new Position(p.name(), p.fen(), "ExactSearchHarness.positions", p.name().equals("endgame")));
        for(var p : SearchBenchmark.corpus())
            selected.putIfAbsent(p.fen(), new Position(p.name(), p.fen(), "SearchBenchmark.corpus", p.name().equals("quiet-endgame")));
        selected.putIfAbsent(FORCED_LOSS, new Position("forced-loss", FORCED_LOSS, "ExactSearchTest", false));
        for(var p : new DefaultPerftPositionLibrary().positions())
            selected.putIfAbsent(p.fen(), new Position(p.id(), p.fen(), "DefaultPerftPositionLibrary", false));
        require(selected.size() == 33, "Representative fixture shape changed");
        require(selected.values().stream().filter(Position::deep).count() == 8, "Deeper fixture shape changed");
        return List.copyOf(selected.values());
    }

    static Run run(Position position, int width, int depth) {
        var facility = new AspirationResearchSearch(width, (b, ply) -> Eval.evaluate(b), new TTable());
        var driver = new SearchDriver(facility);
        var published = new ArrayList<SearchResult>();
        var request = new SearchRequest(Board.fromFen(position.fen()), depth, new SearchObserver() {
            @Override public void onIterationCompleted(IterationSnapshot snapshot) { published.add(snapshot.result()); }
        });
        long started = System.nanoTime();
        var outcome = driver.search(request);
        long elapsed = System.nanoTime() - started;
        require(outcome.targetDepthCompleted(), "Incomplete request " + position.name());
        require(published.size() == facility.iterations.size(), "Failed attempt was published");
        require(outcome.nodes() == facility.iterations.stream().mapToLong(i -> i.result().nodes()).sum(), "Node accounting");
        return new Run(List.copyOf(facility.iterations), List.copyOf(published), outcome.nodes(), elapsed,
                outcome.lastCompletedResult().depth());
    }

    public static void main(String[] args) throws Exception {
        int depth = 8, deepDepth = 10, repetitions = 3, warmups = 2;
        Path output = Path.of("build/sr006b");
        String only = "all";
        for(String arg : args) {
            if(arg.startsWith("--depth=")) depth = Integer.parseInt(arg.substring(8));
            else if(arg.startsWith("--deep-depth=")) deepDepth = Integer.parseInt(arg.substring(13));
            else if(arg.startsWith("--repetitions=")) repetitions = Integer.parseInt(arg.substring(14));
            else if(arg.startsWith("--warmups=")) warmups = Integer.parseInt(arg.substring(10));
            else if(arg.startsWith("--output=")) output = Path.of(arg.substring(9));
            else if(arg.startsWith("--positions=")) only = arg.substring(12);
            else throw new IllegalArgumentException(arg);
        }
        require(depth > 0 && deepDepth >= depth && deepDepth <= 10 && repetitions > 0 && warmups >= 0, "Bounded research options");
        var positions = positions();
        if(only.equals("deep")) positions = positions.stream().filter(Position::deep).toList();
        else if(!only.equals("all")) {
            Set<String> names = Set.of(only.split(","));
            positions = positions.stream().filter(p -> names.contains(p.name())).toList();
            require(positions.size() == names.size(), "Unknown position selection");
        }
        Files.createDirectories(output);
        try(var manifest = writer(output, "positions.csv")) {
            manifest.println("position,fen,source,requested_depth");
            for(var p : positions) manifest.printf("%s,%s,%s,%d%n", p.name(), p.fen(), p.source(), p.deep() ? deepDepth : depth);
        }
        System.out.printf("SR-006B positions=%d depth=%d deepDepth=%d repeats=%d warmups=%d java=%s cpu=%s%n",
                positions.size(), depth, deepDepth, repetitions, warmups, System.getProperty("java.version"), System.getenv("PROCESSOR_IDENTIFIER"));
        for(int w = 0; w < warmups; w++) for(var p : positions().stream().filter(Position::deep).toList())
            for(int width : WIDTHS) run(p, width, Math.min(5, depth));
        System.out.println("Warmup complete");
        var baseline = new HashMap<String, Run>();
        int differences = 0, comparisons = 0;
        try(var rows = writer(output, "iterations.csv"); var requests = writer(output, "requests.csv");
            var alternatives = writer(output, "alternatives.csv")) {
            rows.println("repeat,policy,position,depth,previous,eligibility,alpha,beta,initial_score,narrow_result,initial_nodes,initial_ns,retry,retry_score,retry_nodes,retry_ns,final_score,total_nodes,total_ns,best_move,pv");
            requests.println("repeat,policy,position,requested_depth,completed_depth,nodes,elapsed_ns");
            alternatives.println("position,depth,policy,control_best,best,control_pv,pv,verification");
            for(int rep = 0; rep < repetitions; rep++) for(int index = 0; index < positions.size(); index++) {
                var p = positions.get(index);
                var current = new HashMap<Integer, Run>();
                for(int turn = 0; turn < 3; turn++) {
                    int width = WIDTHS[(rep + index + turn) % 3];
                    int requested = p.deep() ? deepDepth : depth;
                    Run result = run(p, width, requested);
                    current.put(width, result);
                    String key = p.name() + "/" + width;
                    if(rep == 0) baseline.put(key, result); else deterministic(baseline.get(key), result, key);
                    for(var i : result.iterations()) {
                        var a = i.initial(); var retry = i.retry(); var r = i.result();
                        rows.printf(Locale.ROOT, "%d,%s,%s,%d,%s,%s,%d,%d,%d,%s,%d,%d,%s,%s,%d,%d,%d,%d,%d,%s,%s%n",
                                rep + 1, policy(width), p.name(), i.depth(), i.previous() == null ? "" : i.previous(),
                                i.eligibility(), a.alpha(), a.beta(), a.result().score(), i.narrowResult(), a.result().nodes(), a.nanos(),
                                retry != null, retry == null ? "" : retry.result().score(), retry == null ? 0 : retry.result().nodes(),
                                retry == null ? 0 : retry.nanos(), r.score(), r.nodes(), i.nanos(), move(r.bestMove()), pv(r.principalVariation()));
                    }
                    requests.printf("%d,%s,%s,%d,%d,%d,%d%n", rep + 1, policy(width), p.name(), requested, result.completedDepth(), result.nodes(), result.nanos());
                    rows.flush(); requests.flush();
                    System.out.printf(Locale.ROOT, "repeat=%d %s %s depth=%d nodes=%d seconds=%.3f%n",
                            rep + 1, p.name(), policy(width), result.completedDepth(), result.nodes(), result.nanos() / 1e9);
                    // Stop immediately when a completed matching CONTROL is available.
                    if(current.containsKey(0)) for(var pair : current.entrySet()) compareScores(current.get(0), pair.getValue(), p.name());
                    else if(baseline.containsKey(p.name() + "/0")) compareScores(baseline.get(p.name() + "/0"), result, p.name());
                }
                Run control = current.get(0);
                long[] board = Board.fromFen(p.fen()); var history = GameHistory.initial(board);
                for(int width : WIDTHS) {
                    Run r = current.get(width);
                    compareScores(control, r, p.name());
                    for(int i = 0; i < r.published().size(); i++) {
                        var result = r.published().get(i); var reference = control.published().get(i);
                        comparisons++;
                        verifyLegalPv(board, result);
                        if(rep == 0) {
                            // Every supplied prefix is independently exact under its
                            // remaining depth/history; TT may legally shorten the line.
                            verifyPv(board, history, result);
                            if(result.depth() <= 3) require(new ExactSearch().search(board, result.depth()).score() == result.score(), "TT-off score " + p.name());
                            if(width != 0 && !Arrays.equals(reference.principalVariation(), result.principalVariation())) {
                                differences++;
                                alternatives.printf("%s,%d,%s,%s,%s,%s,%s,all-prefixes-exact%n", p.name(), result.depth(), policy(width),
                                        move(reference.bestMove()), move(result.bestMove()), pv(reference.principalVariation()), pv(result.principalVariation()));
                                alternatives.flush();
                            }
                        }
                    }
                }
            }
        }
        System.out.printf("VERIFIED comparisons=%d differing_legal_exact_PVs=%d deterministic_repetitions=%d%n", comparisons, differences, repetitions);
    }

    static void compareScores(Run control, Run candidate, String context) {
        require(control.completedDepth() == candidate.completedDepth(), "Completed-depth mismatch " + context);
        require(control.published().size() == candidate.published().size(), "Iteration mismatch " + context);
        for(int i = 0; i < control.published().size(); i++)
            require(control.published().get(i).score() == candidate.published().get(i).score(),
                    "CORRECTNESS FAILURE " + context + " depth=" + (i + 1) + " control=" + control.published().get(i).score()
                    + " candidate=" + candidate.published().get(i).score());
    }
    static void deterministic(Run a, Run b, String key) {
        require(a.nodes() == b.nodes() && a.published().equals(b.published()), "Determinism " + key);
        for(int i = 0; i < a.iterations().size(); i++) {
            var x = a.iterations().get(i); var y = b.iterations().get(i);
            require(x.narrowResult().equals(y.narrowResult()) && x.initial().result().equals(y.initial().result())
                    && (x.retry() == null ? y.retry() == null : y.retry() != null && x.retry().result().equals(y.retry().result())), "Attempt determinism " + key);
        }
    }
    static void verifyLegalPv(long[] board, SearchResult result) {
        require(result.completed(), "Incomplete published result");
        require(result.principalVariationLength() <= result.depth(), "PV beyond static horizon");
        require(result.hasMove() == (result.principalVariationLength() > 0), "PV/move mismatch");
        for(long move : result.principalVariation()) {
            require(Arrays.stream(ExhaustiveOracle.legalMoves(board)).anyMatch(m -> m == move), "Illegal PV move");
            board = ExhaustiveOracle.child(board, move);
        }
    }
    static void verifyPv(long[] board, GameHistory game, SearchResult result) {
        verifyLegalPv(board, result);
        var history = GameHistory.builder(game);
        // A single fresh TT per line; independent full-window suffix requests.
        var oracle = result.depth() <= 4 ? new ExactSearch() : new ExactSearch((b, ply) -> Eval.evaluate(b), new TTable());
        int ply = 0;
        for(long move : result.principalVariation()) {
            board = ExhaustiveOracle.child(board, move); history.appendPosition(board); ply++;
            int score = oracle.search(board, history.snapshot(), result.depth() - ply, ExactSearch.NEVER_CANCELLED).score();
            if(score >= TranspositionScores.MATE_THRESHOLD) score -= ply;
            else if(score <= -TranspositionScores.MATE_THRESHOLD) score += ply;
            int normalized = (ply & 1) == 0 ? score : -score;
            require(normalized == result.score(), "Non-exact PV prefix depth=" + result.depth() + " ply=" + ply
                    + " root=" + result.score() + " prefix=" + normalized + " pv=" + pv(result.principalVariation()));
        }
    }
    static PrintWriter writer(Path directory, String file) throws IOException {
        return new PrintWriter(Files.newBufferedWriter(directory.resolve(file)));
    }
    static String policy(int width) { return width == 0 ? "CONTROL" : "ASP-" + width; }
    static String move(long move) { return move == 0 ? "-" : Move.coordinate(move); }
    static String pv(long[] line) { return String.join(" ", Arrays.stream(line).mapToObj(Move::coordinate).toList()); }
    static void require(boolean condition, String message) { if(!condition) throw new AssertionError(message); }
    private AspirationResearch() {}
}
