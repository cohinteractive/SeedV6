package com.ohinteractive.seedv6.search.exact;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.SearchDriver;
import com.ohinteractive.seedv6.search.tt.TTable;
import static com.ohinteractive.seedv6.search.exact.AspirationResearch.*;
import static com.ohinteractive.seedv6.search.exact.MtdResearchSearch.*;

/** Opt-in SR-004 experiment. Reuses SR-006's 33-position manifest and exact
 * prefix validators, with a bounded default 6/8 depth budget. Correctness and
 * diagnostic runs precede all clean timings. Only scalar CONTROL scores enter
 * MTD-ORACLE; every request constructs fresh Search/evaluator/TT ownership.
 */
public final class MtdResearch {
    record Run(List<Iteration> iterations, List<SearchResult> published, long nodes, long nanos, int completedDepth) {}

    static Run run(Position p, Policy policy, int depth, Run control, boolean diagnostics) {
        var facility = new MtdResearchSearch(policy, (b, ply) -> Eval.evaluate(b),
                diagnostics ? new ObservedTable() : new TTable(),
                control == null ? null : d -> control.published().get(d - 1).score());
        var published = new ArrayList<SearchResult>();
        var request = new SearchRequest(Board.fromFen(p.fen()), depth, new SearchObserver() {
            @Override public void onIterationCompleted(IterationSnapshot s) { published.add(s.result()); }
        });
        long start = System.nanoTime();
        var outcome = new SearchDriver(facility).search(request);
        long elapsed = System.nanoTime() - start;
        require(outcome.targetDepthCompleted(), "Incomplete " + p.name() + " " + policy);
        require(outcome.nodes() == facility.iterations.stream().mapToLong(i -> i.result().nodes()).sum(), "Node accounting");
        require(published.size() == facility.iterations.size(), "Bound published");
        return new Run(List.copyOf(facility.iterations), List.copyOf(published), outcome.nodes(), elapsed,
                outcome.lastCompletedResult().depth());
    }

    public static void main(String[] args) throws Exception {
        int depth = 6, deepDepth = 8, repetitions = 3, warmups = 2;
        Path output = Path.of("build/sr004");
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
        require(depth >= 1 && deepDepth >= depth && deepDepth <= 10 && repetitions > 0 && warmups >= 0, "Bounded options");
        var positions = positions();
        if(only.equals("deep")) positions = positions.stream().filter(Position::deep).toList();
        else if(!only.equals("all")) {
            var names = Set.of(only.split(","));
            positions = positions.stream().filter(p -> names.contains(p.name())).toList();
            require(positions.size() == names.size(), "Unknown positions");
        }
        Files.createDirectories(output);
        String config = String.format(Locale.ROOT,
                "SR-004 positions=%d depth=%d deepDepth=%d repeats=%d warmups=%d java=%s vm=%s os=%s cpu=%s%n",
                positions.size(), depth, deepDepth, repetitions, warmups, System.getProperty("java.version"),
                System.getProperty("java.vm.name"), System.getProperty("os.name"), System.getenv("PROCESSOR_IDENTIFIER"));
        System.out.print(config);
        Files.writeString(output.resolve("configuration.txt"), config);
        try(var manifest = writer(output, "positions.csv")) {
            manifest.println("position,fen,source,requested_depth");
            for(var p : positions) manifest.printf("%s,%s,%s,%d%n", p.name(), p.fen(), p.source(), p.deep() ? deepDepth : depth);
        }
        var baseline = new HashMap<String, Run>();
        int verified = 0;
        try(var checks = writer(output, "verification.csv"); var diag = writer(output, "diagnostic_passes.csv")) {
            checks.println("position,policy,depth,score,best_move,pv,verification,tt_off_root");
            diag.println("position,policy,depth,pass,phase,probes,hits,exact_hits,lower_hits,upper_hits,save_attempts");
            for(var p : positions) {
                int requested = p.deep() ? deepDepth : depth;
                Run control = null;
                for(var policy : Policy.values()) {
                    var r = run(p, policy, requested, control, true);
                    if(policy == Policy.CONTROL) control = r;
                    compare(control, r, p.name());
                    var board = Board.fromFen(p.fen()); var history = GameHistory.initial(board);
                    for(var result : r.published()) {
                        verifyPv(board, history, result);
                        if(result.depth() <= 3) require(new ExactSearch().search(board, result.depth()).score() == result.score(), "TT-off root");
                        checks.printf("%s,%s,%d,%d,%s,%s,all-prefixes-exact,%s%n", p.name(), policy, result.depth(),
                                result.score(), move(result.bestMove()), pv(result.principalVariation()), result.depth() <= 3);
                        verified++;
                    }
                    for(var i : r.iterations()) for(int n = 0; n < i.attempts().size(); n++) {
                        var a = i.attempts().get(n); var t = a.tt();
                        diag.printf("%s,%s,%d,%d,%s,%d,%d,%d,%d,%d,%d%n", p.name(), policy, i.depth(), n + 1,
                                a.phase(), t.probes(), t.hits(), t.exact(), t.lower(), t.upper(), t.saves());
                    }
                    baseline.put(p.name() + "/" + policy, r);
                }
                checks.flush(); diag.flush();
                System.out.println("CORRECT " + p.name() + " depth=" + control.completedDepth());
            }
        }
        System.out.println("Correctness gate passed: " + verified + " exact results; starting warmup");
        for(int w = 0; w < warmups; w++) for(var p : positions().stream().filter(Position::deep).toList()) {
            var c = run(p, Policy.CONTROL, Math.min(5, depth), null, false);
            run(p, Policy.MTD_PREV, Math.min(5, depth), null, false);
            run(p, Policy.MTD_ORACLE, Math.min(5, depth), c, false);
        }
        try(var rows = writer(output, "iterations.csv"); var passes = writer(output, "passes.csv");
            var requests = writer(output, "requests.csv")) {
            rows.println("repeat,policy,position,requested_depth,completed_depth,initial_guess,final_score,signed_guess_error,absolute_guess_error,zero_passes,nodes,elapsed_ns,nps,materialize_nodes,materialize_ns,best_move,pv,verification");
            passes.println("repeat,policy,position,depth,pass,phase,alpha,beta,classification,score,lower,upper,nodes,visited_nodes,elapsed_ns");
            requests.println("repeat,policy,position,requested_depth,completed_depth,nodes,elapsed_ns,nps");
            for(int rep = 0; rep < repetitions; rep++) for(int index = 0; index < positions.size(); index++) {
                var p = positions.get(index);
                var c = baseline.get(p.name() + "/" + Policy.CONTROL);
                for(int turn = 0; turn < 3; turn++) {
                    var policy = Policy.values()[(rep + index + turn) % 3];
                    int requested = p.deep() ? deepDepth : depth;
                    var r = run(p, policy, requested, c, false);
                    compare(c, r, p.name());
                    deterministic(baseline.get(p.name() + "/" + policy), r);
                    for(var result : r.published()) verifyLegalPv(Board.fromFen(p.fen()), result);
                    for(var i : r.iterations()) {
                        var result = i.result();
                        var material = i.attempts().stream().filter(a -> a.phase().equals("materialize")).findFirst().orElse(null);
                        Integer error = i.guess() == null ? null : i.guess() - result.score();
                        long zeros = i.attempts().stream().filter(a -> a.phase().equals("zero")).count();
                        rows.printf(Locale.ROOT, "%d,%s,%s,%d,%d,%s,%d,%s,%s,%d,%d,%d,%.1f,%d,%d,%s,%s,all-prefixes-exact%n",
                                rep + 1, policy, p.name(), i.depth(), result.depth(), nullable(i.guess()), result.score(),
                                nullable(error), error == null ? "" : Math.abs(error), zeros, result.nodes(), i.nanos(),
                                nps(result.nodes(), i.nanos()), material == null ? 0 : material.result().nodes(),
                                material == null ? 0 : material.nanos(), move(result.bestMove()), pv(result.principalVariation()));
                        for(int n = 0; n < i.attempts().size(); n++) {
                            var a = i.attempts().get(n);
                            passes.printf("%d,%s,%s,%d,%d,%s,%d,%d,%s,%d,%d,%d,%d,%d,%d%n", rep + 1, policy,
                                    p.name(), i.depth(), n + 1, a.phase(), a.alpha(), a.beta(), a.classification(), a.result().score(),
                                    a.lower(), a.upper(), a.result().nodes(), a.visitedNodes(), a.nanos());
                        }
                    }
                    requests.printf(Locale.ROOT, "%d,%s,%s,%d,%d,%d,%d,%.1f%n", rep + 1, policy, p.name(), requested,
                            r.completedDepth(), r.nodes(), r.nanos(), nps(r.nodes(), r.nanos()));
                    rows.flush(); passes.flush(); requests.flush();
                    System.out.printf(Locale.ROOT, "repeat=%d %s %s depth=%d nodes=%d seconds=%.3f%n", rep + 1,
                            p.name(), policy, r.completedDepth(), r.nodes(), r.nanos() / 1e9);
                }
            }
        }
        String verifiedText = "VERIFIED exact_results=" + verified + " timing_repetitions=" + repetitions
                + " deterministic_including_diagnostic_noninterference=true\n";
        Files.writeString(output.resolve("verified.txt"), verifiedText);
        System.out.print(verifiedText);
    }

    static void compare(Run control, Run candidate, String context) {
        require(control.completedDepth() == candidate.completedDepth() && control.published().size() == candidate.published().size(), "Depth " + context);
        for(int i = 0; i < control.published().size(); i++)
            require(control.published().get(i).score() == candidate.published().get(i).score(), "Score " + context + " depth=" + (i + 1));
    }
    static void deterministic(Run a, Run b) {
        require(a.nodes() == b.nodes() && a.published().equals(b.published()), "Result determinism/noninterference");
        for(int i = 0; i < a.iterations().size(); i++) {
            var x = a.iterations().get(i); var y = b.iterations().get(i);
            require(Objects.equals(x.guess(), y.guess()) && x.attempts().size() == y.attempts().size(), "Pass count determinism");
            for(int j = 0; j < x.attempts().size(); j++) {
                var u = x.attempts().get(j); var v = y.attempts().get(j);
                require(u.alpha() == v.alpha() && u.beta() == v.beta() && u.lower() == v.lower() && u.upper() == v.upper()
                        && u.result().equals(v.result()) && u.visitedNodes() == v.visitedNodes()
                        && u.phase().equals(v.phase()) && u.classification().equals(v.classification()), "Pass determinism/noninterference");
            }
        }
    }
    private static double nps(long nodes, long nanos) { return nanos == 0 ? 0 : nodes * 1e9 / nanos; }
    private MtdResearch() {}
}
