package com.ohinteractive.seedv6.search.exact;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Fen;
import com.ohinteractive.seedv6.rules.DrawAdjudicator;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;
import com.ohinteractive.seedv6.tools.search.ExactSearchHarness;

/** SR-001E bounded experimental corpus. An interrupted candidate has no semantic score. */
public final class QuiescenceQuietCheckCorpus {
    static final int MODE = ExactSearch.QSEARCH_QUIET_CHECKS;
    record Fixture(String name, String fen, boolean oracle, String resource) {}
    static final List<Fixture> TARGETED = List.of(
        new Fixture("quiet-direct-mate", "7k/8/5KQ1/8/8/8/8/8 w - - 98 1", true, "g6g7"),
        new Fixture("quiet-double-discovery", "k7/8/B1K5/8/8/8/8/R7 w - - 98 1", true, "a6b7"),
        new Fixture("quiet-pawn-check", "8/8/3kp3/8/4PK2/8/8/8 w - - 0 1", true, "e4e5"),
        new Fixture("quiet-king-discovery", "k7/8/K7/8/8/8/8/R7 w - - 98 1", true, "a6b6"),
        new Fixture("quiet-castling-check", "5k2/8/8/8/8/8/8/4K2R w K - 98 1", true, "e1g1"),
        new Fixture("quiet-knight-fork", "4k3/5q2/8/5N2/8/8/8/4K3 w - - 0 1", false, "f5d6"),
        new Fixture("quiet-smother-sacrifice", "5r1k/6pp/4Q2N/8/8/8/8/4K3 w - - 0 1", false, "e6g8"),
        new Fixture("quiet-check-rule50-draw", "4k3/8/8/8/8/8/7q/R3K3 w - - 98 1", true, "a1a8"),
        new Fixture("quiet-rook-checks-standpat", "4k3/8/8/8/8/8/8/R3K3 w - - 98 1", true, "a1a8"),
        new Fixture("quiet-mate-unlimited", "7k/8/5KQ1/8/8/8/8/8 w - - 0 1", false, "g6g7"),
        new Fixture("endgame-pawn-displacement", "8/2p5/3p4/KPr4k/5R2/8/4P1P1/8 w - - 1 3", false, "g2g4")
    );

    static List<Fixture> fixtures() {
        List<Fixture> result = new ArrayList<>();
        for(var f : QuiescenceDepthCorpus.fixtures()) result.add(new Fixture(f.name(), f.fen(), false, ""));
        result.addAll(TARGETED);
        return result;
    }

    public static void main(String[] args) {
        if(args.length > 0 && args[0].equals("--safety")) safety(System.out);
        else if(args.length > 0 && args[0].equals("--witnesses")) benchmarkWitnesses(System.out);
        else run(System.out, args.length > 0);
    }

    /** Follow the higher-valued side's line until the first causally relevant added quiet check. */
    static void benchmarkWitnesses(PrintStream out) {
        out.println("normal_depth,path,fen,qply,baseline_score,quiet_check_score,quiet_check,continuation");
        String fen = ExactSearchHarness.positions().get(2).fen();
        ExactSearch base = ExactSearch.quiescenceResearch(QuiescenceSeeCorpus.HCE);
        ExactSearch checks = ExactSearch.quiescenceResearch(QuiescenceSeeCorpus.HCE, MODE);
        for(int depth : new int[] {2, 3, 4}) {
            long[] b = Board.fromFen(fen); var h = GameHistory.builder(b);
            String path = "";
            for(int ply = 0; ; ply++) {
                int remaining = Math.max(0, depth - ply);
                var off = base.search(b, h.snapshot(), remaining, () -> base.visitedNodes() >= 1_000_000);
                var on = checks.search(b, h.snapshot(), remaining, () -> checks.visitedNodes() >= 1_000_000);
                require(off.completed() && on.completed(), "Witness budget");
                require(off.score() != on.score() && ply < 64, "Missing causal difference");
                var high = off.score() > on.score() ? off : on;
                require(high.hasMove(), "Missing higher-valued continuation");
                long m = high.bestMove();
                if(remaining == 0 && !QuiescenceOracle.inCheck(b) && !QuiescenceOracle.tactical(b, m)) {
                    require(on.score() > off.score(), "Quiet-check witness direction");
                    out.println(csv(depth, path, Fen.fromBoard(b), ply - depth, off.score(), on.score(),
                            Move.coordinate(m), QuiescenceSeeCorpus.pv(on)));
                    break;
                }
                b = ExhaustiveOracle.child(b, m); h.appendPosition(b);
                path = (path + " " + Move.coordinate(m)).trim();
            }
        }
    }
    static void run(PrintStream out, boolean targetedOnly) {
        out.println("fixture,fen,completed,completion_reason,baseline_score,score,baseline_best,best,baseline_pv,pv,baseline_qnodes,qnodes,baseline_max_qply,max_qply,nodes_with_checks,quiet_checks,quiet_cutoffs,max_chain,oracle,oracle_nodes,intended_resource,resource_result,first_pv_quiet_check,quiet_qply,classification");
        ExactSearch baseline = ExactSearch.quiescenceResearch(QuiescenceSeeCorpus.HCE);
        ExactSearch checks = ExactSearch.quiescenceResearch(QuiescenceSeeCorpus.HCE, MODE, true);
        for(Fixture f : targetedOnly ? TARGETED : fixtures()) {
            long[] b = Board.fromFen(f.fen()); var game = GameHistory.initial(b);
            require(!Board.isPlayerInCheck(b[0], b[1], b[2], b[3], 1 ^ Board.player((int)b[Board.STATUS])), "Invalid fixture " + f.name());
            var off = search(baseline, b, game);
            var on = search(checks, b, game);
            long oracleNodes = 0; String oracleStatus = "not-attempted";
            if(f.oracle()) {
                var oracle = new QuiescenceOracle(QuiescenceSeeCorpus.HCE, MODE);
                try {
                    int score = oracle.score(b, new SearchLineHistory(game), 0, 0);
                    require(on.completed() && score == on.score(), "Oracle mismatch " + f.name());
                    oracleStatus = "matched";
                } catch(AssertionError failure) {
                    if(!failure.getMessage().equals("Unbounded oracle fixture")) throw failure;
                    oracleStatus = "guard-incomplete";
                }
                oracleNodes = oracle.nodes;
            }
            String quietMove = ""; int quietPly = -1, ply = 0;
            long[] cursor = b;
            for(long m : on.principalVariation()) {
                require(Arrays.stream(ExhaustiveOracle.legalMoves(cursor)).anyMatch(x -> x == m), "Illegal qPV");
                long[] child = ExhaustiveOracle.child(cursor, m);
                if(!QuiescenceOracle.inCheck(cursor) && !QuiescenceOracle.tactical(cursor, m)) {
                    require(QuiescenceOracle.inCheck(child), "Nonchecking quiet searched");
                    if(quietMove.isEmpty()) { quietMove = Move.coordinate(m); quietPly = ply; }
                }
                cursor = child; ply++;
            }
            if(on.completed()) verifyPv(b, game, on);
            long[] d = checks.quietCheckDiagnostics();
            String classification = !on.completed() ? "no-completed-value" : off.score() == on.score() ? "same-value"
                    : switch(f.name()) {
                        case "quiet-direct-mate" -> "forced-mate";
                        case "quiet-knight-fork" -> "queen-gain;insufficient-material-draw";
                        case "quiet-smother-sacrifice" -> "quiet-queen-sacrifice;forced-mate";
                        case "quiet-check-rule50-draw" -> "rule50-draw";
                        case "endgame-pawn-displacement" -> "pawn-check;king-displacement;material-unchanged-positional";
                        default -> throw new AssertionError("Unclassified divergence: " + f.name());
                    };
            out.println(csv(f.name(), f.fen(), on.completed(), reason(on, 0), off.score(), on.completed() ? on.score() : "",
                    best(off), best(on), QuiescenceSeeCorpus.pv(off), QuiescenceSeeCorpus.pv(on), off.qnodes(), on.qnodes(),
                    off.maximumQply(), on.maximumQply(), d[0], d[1], d[2], d[3], oracleStatus, oracleNodes,
                    f.resource(), f.resource().isEmpty() ? "" : !on.completed() ? "incomplete-no-conclusion"
                            : best(on).equals(f.resource()) ? "best-resource" : "searched;stand-pat-best",
                    quietMove, quietPly < 0 ? "" : quietPly, classification));
        }
    }

    static String reason(ExactSearchResult r, int depth) {
        if(r.completed()) return "completed";
        if(r.nodes() >= 1_000_000) return "node-budget";
        require(r.maximumQply() + depth == 256, "Unexpected interruption");
        return "absolute-ply-256";
    }

    private static void verifyPv(long[] b, GameHistory game, ExactSearchResult result) {
        var path = GameHistory.builder(game); int ply = 0;
        ExactSearch reference = ExactSearch.quiescenceResearch(QuiescenceSeeCorpus.HCE, MODE);
        for(long m : result.principalVariation()) {
            b = ExhaustiveOracle.child(b, m); path.appendPosition(b); ply++;
            var suffix = search(reference, b, path.snapshot()); require(suffix.completed(), "PV re-search budget");
            int s = suffix.score();
            if(s >= TranspositionScores.MATE_THRESHOLD) s -= ply; else if(s <= -TranspositionScores.MATE_THRESHOLD) s += ply;
            require((ply % 2 == 0 ? s : -s) == result.score(), "PV value mismatch");
        }
    }

    /** Independently validate the entire actual path of two absolute-safety interruptions. */
    static void safety(PrintStream out) {
        out.println("fixture,fen,normal_depth,total_nodes,qnodes,max_qply,actual_ply,final_halfmove,final_occurrences,final_checked,final_fen,clock_reset_plies,legal_path");
        var middle = ExactSearchHarness.orderingPositions().stream().filter(p -> p.name().equals("middlegame")).findFirst().orElseThrow();
        for(var f : List.of(new Fixture("middlegame", middle.fen(), false, ""),
                new Fixture("nonchecking-clearance", QuiescenceSeeCorpus.FIXTURES.get(27).fen(), false, ""))) {
            int depth = f.name().equals("middlegame") ? 2 : 0;
            long[] b = Board.fromFen(f.fen()); var game = GameHistory.initial(b);
            SafetyTrace trace = new SafetyTrace();
            ExactSearch q = ExactSearch.quiescenceResearch(trace, MODE);
            var r = q.search(b, game, depth, () -> q.visitedNodes() >= 1_000_000);
            require(reason(r, depth).equals("absolute-ply-256"), "Expected safety interruption");
            var history = new SearchLineHistory(game, 256);
            StringBuilder moves = new StringBuilder(), resets = new StringBuilder();
            for(int ply = 0; ply < 256; ply++) {
                long[] parent = trace.witness[ply], child = trace.witness[ply + 1];
                require(DrawAdjudicator.adjudicateNonTerminal(parent, history) == DrawAdjudicator.RuleDraw.NONE,
                        "Draw was expanded at " + ply);
                long selected = 0;
                for(long m : ExhaustiveOracle.legalMoves(parent)) if(Arrays.equals(ExhaustiveOracle.child(parent, m), child)) { selected = m; break; }
                require(selected != 0, "Illegal witness child");
                if(ply >= depth) require(QuiescenceOracle.inCheck(parent) || QuiescenceOracle.tactical(parent, selected)
                        || QuiescenceOracle.inCheck(child), "Non-checking quiet witness");
                if(moves.length() > 0) moves.append(' '); moves.append(Move.coordinate(selected));
                if(Board.halfMoveClock((int)child[Board.STATUS]) == 0) {
                    if(resets.length() > 0) resets.append(' '); resets.append(ply + 1);
                }
                history.pushRealPosition(child);
            }
            long[] end = trace.witness[256];
            require(ExhaustiveOracle.legalMoves(end).length > 0, "Terminal safety node");
            require(DrawAdjudicator.adjudicateNonTerminal(end, history) == DrawAdjudicator.RuleDraw.NONE, "Draw safety node");
            out.println(csv(f.name(), f.fen(), depth, r.nodes(), r.qnodes(), r.maximumQply(), 256,
                    Board.halfMoveClock((int)end[Board.STATUS]), history.currentOccurrences(end), QuiescenceOracle.inCheck(end),
                    Fen.fromBoard(end), resets, moves));
        }
    }

    private static final class SafetyTrace implements ExactEvaluator {
        final long[][] current = new long[257][6], witness = new long[257][6];
        public int evaluate(long[] b, int ply) { return Eval.evaluate(b); }
        public void initialize(long[] b) { System.arraycopy(b, 0, current[0], 0, 6); }
        public void child(long[] a, long[] b, int ply) {
            System.arraycopy(b, 0, current[ply + 1], 0, 6);
            if(ply == 255) for(int i = 0; i <= 256; i++) System.arraycopy(current[i], 0, witness[i], 0, 6);
        }
    }
    static ExactSearchResult search(ExactSearch q, long[] b, GameHistory h) {
        return q.search(b, h, 0, () -> q.visitedNodes() >= 1_000_000);
    }
    private static String best(ExactSearchResult r) { return r.hasMove() ? Move.coordinate(r.bestMove()) : "none"; }
    private static String csv(Object... values) {
        return String.join(",", Arrays.stream(values).map(v -> "\"" + v.toString().replace("\"", "\"\"") + "\"").toList());
    }
    private static void require(boolean ok, String message) { if(!ok) throw new AssertionError(message); }
}
