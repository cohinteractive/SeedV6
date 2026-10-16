package com.ohinteractive.seedv6.search.exact;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;

/** SR-001F deterministic semantic corpus; test/research allocations stay outside Search. */
public final class QuiescenceCheckLimitCorpus {
    static final int[] MODES = {0, 33, 34, 35, 32};
    static final String[] NAMES = {"QSEARCH_BASELINE", "QCHECK_INITIAL_ONLY", "QCHECK_MAX_ONE", "QCHECK_MAX_TWO", "QSEARCH_QUIET_CHECKS"};
    record Fixture(String name, String fen, String classification, boolean oracle, boolean unrestrictedOracle) {}
    static final List<Fixture> DEEPER = List.of(
        new Fixture("first-after-capture-mate", "5rRk/6pp/7N/8/8/8/8/4K3 b - - 0 1", "mate;first-check-qply-1", true, true),
        new Fixture("exchange-deeper-check", "5rrk/8/6KN/8/8/1Q6/8/8 w - - 0 1", "exchange;positional;first-check-qply-3", true, false),
        new Fixture("capture-allows-queen-fork", "4k3/8/8/8/5n2/1r6/P4Q2/4K3 w - - 0 1", "material-loss-avoidance;deeper-knight-fork", true, false),
        new Fixture("third-check-smother", "4k3/8/8/8/8/3q4/PPn5/1KR5 b - - 96 1", "mate;third-ordinary-check-required", true, false),
        new Fixture("repeated-check-no-extra-value", "4k3/8/8/8/8/8/8/R3K3 w - - 96 1", "second-checks-exist;no-extra-value", true, true)
    );

    static List<Fixture> fixtures() {
        List<Fixture> result = new ArrayList<>();
        for(var f : QuiescenceQuietCheckCorpus.fixtures()) result.add(new Fixture(f.name(), f.fen(), classify(f.name()),
                f.oracle() || f.name().equals("quiet-smother-sacrifice") || f.name().equals("quiet-promotion"), f.oracle()));
        result.addAll(DEEPER);
        return result;
    }

    public static void main(String[] args) { run(System.out, args.length > 0 && args[0].equals("--witnesses")); }

    static void run(PrintStream out, boolean witnesses) {
        if(witnesses) out.println("fixture,fen,candidate,reference,candidate_score,reference_score,classification,witness_path,excluded_quiet_check,qply,ordinal,higher_mode,witness_status");
        else out.println("fixture,fen,mode,completed,completion_reason,score,best,pv,qnodes,max_qply,first_quiet_qply,pv_quiet_checks,classification,oracle,oracle_nodes,nodes_with_checks,quiet_checks,cutoffs,max_chain,first_checks,second_checks,prevented_nodes,max_used,generated_checks");
        for(Fixture f : fixtures()) {
            long[] board = Board.fromFen(f.fen()); GameHistory game = GameHistory.initial(board);
            require(!Board.isPlayerInCheck(board[0], board[1], board[2], board[3], 1 ^ Board.player((int)board[Board.STATUS])), "Invalid " + f.name());
            ExactSearchResult[] results = new ExactSearchResult[MODES.length];
            for(int i = 0; i < MODES.length; i++) {
                int mode = MODES[i]; ExactSearch q = ExactSearch.quiescenceResearch(QuiescenceSeeCorpus.HCE, mode, mode >= 32);
                var r = search(q, board, game, 0, 0); results[i] = r;
                String oracleStatus = "not-attempted"; long oracleNodes = 0;
                if(!witnesses && f.oracle() && (mode != 32 || f.unrestrictedOracle())) {
                    QuiescenceOracle oracle = new QuiescenceOracle(QuiescenceSeeCorpus.HCE, mode);
                    int score = oracle.score(board, new SearchLineHistory(game), 0, 0);
                    require(r.completed() && r.score() == score, "Oracle mismatch " + f.name() + "/" + mode);
                    oracleStatus = "matched"; oracleNodes = oracle.nodes;
                }
                String checks = pvChecks(board, r, mode);
                if(r.completed() && !witnesses) verifyPv(board, game, r, mode);
                long[] d = mode >= 32 ? q.quietCheckDiagnostics() : new long[4];
                if(mode >= 33) require(d[7] <= (mode == 35 ? 2 : 1), "Allowance exceeded");
                if(!witnesses) out.println(csv(f.name(), f.fen(), NAMES[i], r.completed(), QuiescenceQuietCheckCorpus.reason(r, 0),
                        r.completed() ? r.score() : "", best(r), QuiescenceSeeCorpus.pv(r), r.qnodes(), r.maximumQply(),
                        checks.isEmpty() ? "" : checks.substring(0, checks.indexOf(':')), checks, f.classification(), oracleStatus, oracleNodes,
                        d[0], d[1], d[2], d[3], d.length > 4 ? d[4] : "", d.length > 4 ? d[5] : "", d.length > 4 ? d[6] : "",
                        d.length > 4 ? d[7] : "", d.length > 4 ? d[8] : ""));
            }
            if(witnesses) for(int i = 1; i <= 3; i++) for(int ref : new int[] {0, 4}) {
                var a = results[i]; var b = results[ref];
                if(a.completed() && b.completed() && a.score() != b.score()) {
                    Witness w = witness(board, game, MODES[i], MODES[ref]);
                    out.println(csv(f.name(), f.fen(), NAMES[i], NAMES[ref], a.score(), b.score(), f.classification(),
                            w.path(), w.move(), w.qply(), w.ordinal(), w.higherMode(), w.status()));
                }
            }
        }
    }

    record Witness(String path, String move, int qply, int ordinal, int higherMode, String status) {}
    static Witness witness(long[] b, GameHistory game, int a, int c) {
        var history = GameHistory.builder(game); int used = 0; String path = "";
        ExactSearch qa = ExactSearch.quiescenceResearch(QuiescenceSeeCorpus.HCE, a), qc = ExactSearch.quiescenceResearch(QuiescenceSeeCorpus.HCE, c);
        for(int ply = 0; ply < 64; ply++) {
            var ra = search(qa, b, history.snapshot(), ply, used); var rc = search(qc, b, history.snapshot(), ply, used);
            if(!ra.completed() || !rc.completed()) return new Witness(path, "", ply, used + 1, -1, "suffix-budget-incomplete");
            require(ra.score() != rc.score(), "Causal difference disappeared");
            boolean highA = ra.score() > rc.score(); var high = highA ? ra : rc;
            require(high.hasMove(), "Higher value has no continuation"); long move = high.bestMove();
            boolean quiet = ordinary(b, move); int lowerMode = highA ? c : a;
            if(quiet && !eligible(lowerMode, ply, used)) return new Witness(path, Move.coordinate(move), ply, used + 1, highA ? a : c, "first-excluded-check");
            b = ExhaustiveOracle.child(b, move); history.appendPosition(b);
            if(quiet) used++; path = (path + " " + Move.coordinate(move)).trim();
        }
        return new Witness(path, "", 64, used + 1, -1, "witness-length-guard");
    }

    static ExactSearchResult search(ExactSearch q, long[] b, GameHistory g, int qply, int used) {
        return q.searchQuiescenceSuffix(b, g, qply, used, () -> q.visitedNodes() >= 1_000_000);
    }

    static boolean eligible(int mode, int qply, int used) {
        return mode == 32 || mode == 33 && qply == 0 || mode == 34 && used == 0 || mode == 35 && used < 2;
    }
    static boolean ordinary(long[] board, long move) { return !QuiescenceOracle.inCheck(board) && !QuiescenceOracle.tactical(board, move); }

    static String pvChecks(long[] b, ExactSearchResult r, int mode) {
        int ply = 0, used = 0; String checks = "";
        for(long m : r.principalVariation()) {
            require(Arrays.stream(ExhaustiveOracle.legalMoves(b)).anyMatch(x -> x == m), "Illegal PV");
            boolean quiet = ordinary(b, m); long[] child = ExhaustiveOracle.child(b, m);
            if(quiet) {
                require(eligible(mode, ply, used) && QuiescenceOracle.inCheck(child), "Inadmissible quiet PV");
                checks = (checks + " " + ply + ":" + (++used) + ":" + Move.coordinate(m)).trim();
            }
            b = child; ply++;
        }
        return checks;
    }

    static void verifyPv(long[] b, GameHistory game, ExactSearchResult result, int mode) {
        var history = GameHistory.builder(game); int ply = 0, used = 0;
        ExactSearch reference = ExactSearch.quiescenceResearch(QuiescenceSeeCorpus.HCE, mode);
        for(long m : result.principalVariation()) {
            if(ordinary(b, m)) used++;
            b = ExhaustiveOracle.child(b, m); history.appendPosition(b); ply++;
            var r = search(reference, b, history.snapshot(), ply, used); require(r.completed(), "PV suffix budget");
            int score = r.score(); if(score >= TranspositionScores.MATE_THRESHOLD) score -= ply; else if(score <= -TranspositionScores.MATE_THRESHOLD) score += ply;
            require((ply % 2 == 0 ? score : -score) == result.score(), "PV value mismatch mode=" + mode);
        }
    }

    private static String classify(String name) {
        return switch(name) {
            case "quiet-direct-mate", "quiet-mate-unlimited", "quiet-mate-excluded", "mate-five" -> "mate";
            case "quiet-knight-fork" -> "queen-gain;insufficient-material-draw";
            case "quiet-smother-sacrifice" -> "quiet-sacrifice;mate;second-check-required";
            case "quiet-check-rule50-draw" -> "rule50-draw";
            case "diagonal-xray", "three-capture-xray" -> "material-exchange-recovery";
            case "endgame-pawn-displacement", "losing-defended", "bishop-xray", "nonchecking-clearance" -> "positional-evaluation;checking-continuation";
            case "quiet-promotion", "capture-promotion", "capture-promotion-black", "promotion-positional-resource" -> "promotion;deeper-check;positional-evaluation";
            case "successive-xrays", "tail-evasion-", "tail-evasion-f1f2" -> "exchange;deeper-checking-continuation";
            default -> "retained-corpus-coverage";
        };
    }
    static String best(ExactSearchResult r) { return r.hasMove() ? Move.coordinate(r.bestMove()) : "none"; }
    private static String csv(Object... fields) {
        StringBuilder s = new StringBuilder();
        for(Object f : fields) { if(!s.isEmpty()) s.append(','); s.append('"').append(f.toString().replace("\"", "\"\"")).append('"'); }
        return s.toString();
    }
    private static void require(boolean value, String message) { if(!value) throw new AssertionError(message); }
}
