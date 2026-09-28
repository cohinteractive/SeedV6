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

/** SR-001G deterministic semantic corpus; test/research allocations stay outside Search. */
public final class QuiescenceForcingCorpus {
    static final int[] MODES = {0, 36, 37, 38, 33, 32};
    static final String[] NAMES = {"QSEARCH_BASELINE", "QCHECK_FORCED_ONE", "QCHECK_INITIAL_PLUS_ONE", "QCHECK_INITIAL_PLUS_TWO", "QCHECK_INITIAL_ONLY", "QSEARCH_QUIET_CHECKS"};
    record Fixture(String name, String fen, String classification, boolean oracle, boolean unrestrictedOracle) {}
    static final List<Fixture> THRESHOLDS = List.of(
        new Fixture("deeper-two-reply-fork", "4k3/8/8/8/5n2/1rp5/P4Qp1/4K3 w - - 0 1", "material;two-reply-knight-fork-after-a2b3", true, false),
        new Fixture("deeper-one-reply-fork", "4k3/8/8/8/5n2/1rp2p2/P4Qp1/4K3 w - - 0 1", "material;one-reply-knight-fork-after-a2b3", true, false)
    );
    static List<Fixture> fixtures() {
        List<Fixture> result = new ArrayList<>();
        for(var f : QuiescenceCheckLimitCorpus.fixtures()) result.add(new Fixture(f.name(), f.fen(), f.classification(), f.oracle(), f.unrestrictedOracle()));
        result.addAll(THRESHOLDS);
        return result;
    }

    public static void main(String[] args) {
        if(args.length > 0 && args[0].equals("--resources")) resources(System.out);
        else run(System.out, args.length > 0 && args[0].equals("--witnesses"));
    }

    /** Explicit legal mechanism witnesses, including off-PV resources masked by another root choice. */
    static void resources(PrintStream out) {
        out.println("fixture,fen,classification,path_before_check,quiet_check,qply,ordinal,evasions,legal_replies,forced_one,initial_plus_one,initial_plus_two");
        for(var f : fixtures()) {
            String line = switch(f.name()) {
                case "quiet-direct-mate", "quiet-mate-unlimited" -> "g6g7";
                case "quiet-double-discovery" -> "a6b7";
                case "quiet-pawn-check" -> "e4e5";
                case "quiet-king-discovery" -> "a6b6";
                case "quiet-castling-check" -> "e1g1";
                case "quiet-knight-fork" -> "f5d6 e8d7 d6f7";
                case "quiet-smother-sacrifice" -> "e6g8 f8g8 h6f7";
                case "quiet-check-rule50-draw" -> "a1a8 e8d7";
                case "quiet-rook-checks-standpat" -> "a1a8";
                case "endgame-pawn-displacement" -> "g2g4 h5h4";
                case "first-after-capture-mate" -> "f8g8 h6f7";
                case "exchange-deeper-check" -> "h6g8 f8g8 g6h6 g8g6 h6h5 g6h6";
                case "capture-allows-queen-fork" -> "a2b3 f4d3 e1d1 d3f2";
                case "deeper-two-reply-fork" -> "a2b3 f4d3 e1e2 d3f2";
                case "deeper-one-reply-fork" -> "a2b3 f4d3 e1d1 d3f2 d1c2";
                case "third-check-smother" -> "c2a3 b1a1 d3b1 c1b1 a3c2";
                case "repeated-check-no-extra-value" -> "a1a8 e8e7 a8a7";
                case "quiet-promotion" -> "a7a8q h8g7 a8g2 g7f8 g2f3";
                default -> "";
            };
            if(line.isEmpty()) continue;
            long[] b = Board.fromFen(f.fen()); int qply = 0, used = 0; String prefix = "";
            for(String coordinate : line.split(" ")) {
                long move = QuiescenceDepthCorpus.move(b, coordinate);
                boolean quiet = ordinary(b, move); long[] c = ExhaustiveOracle.child(b, move);
                if(quiet) {
                    require(QuiescenceOracle.inCheck(c), "Witness is not a check");
                    long[] replies = ExhaustiveOracle.legalMoves(c); String replyText = "";
                    for(long reply : replies) replyText = (replyText + " " + Move.coordinate(reply)).trim();
                    out.println(csv(f.name(), f.fen(), f.classification(), prefix, coordinate, qply, used + 1, replies.length,
                            replyText, eligible(36,qply,used,c), eligible(37,qply,used,c), eligible(38,qply,used,c)));
                    used++;
                }
                b = c; qply++; prefix = (prefix + " " + coordinate).trim();
            }
        }
    }

    static void run(PrintStream out, boolean witnesses) {
        if(witnesses) out.println("fixture,fen,candidate,reference,candidate_score,reference_score,candidate_pv,reference_pv,classification,witness_path,excluded_quiet_check,qply,ordinal,evasions,candidate_admits,reference_admits,higher_mode,witness_status");
        else out.println("fixture,fen,mode,completed,completion_reason,score,best,pv,qnodes,max_qply,first_quiet_qply,pv_quiet_checks,classification,pv_validation,oracle,oracle_nodes,nodes_with_checks,quiet_checks,cutoffs,max_chain,classified,zero,one,two,three_plus,required_calls,required_moves,initial_diagnostic_calls,initial_diagnostic_moves,max_used");
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
                    try {
                        int score = oracle.score(board, new SearchLineHistory(game), 0, 0);
                        require(r.completed() && r.score() == score, "Oracle mismatch " + f.name() + "/" + mode);
                        oracleStatus = "matched";
                    } catch(AssertionError e) {
                        if(!"Unbounded oracle fixture".equals(e.getMessage())) throw e;
                        oracleStatus = "guard-rejected-not-exact";
                    }
                    oracleNodes = oracle.nodes;
                }
                String checks = pvChecks(board, r, mode);
                String pvStatus = r.completed() && !witnesses ? verifyPv(board, game, r, mode) : "not-applicable";
                long[] d = mode >= 32 ? q.quietCheckDiagnostics() : new long[4];
                long[] g = mode >= 36 ? q.forcingDiagnostics() : new long[10];
                if(!witnesses) out.println(csv(f.name(), f.fen(), NAMES[i], r.completed(), QuiescenceQuietCheckCorpus.reason(r, 0),
                        r.completed() ? r.score() : "", best(r), QuiescenceSeeCorpus.pv(r), r.qnodes(), r.maximumQply(),
                        checks.isEmpty() ? "" : checks.substring(0, checks.indexOf(':')), checks, f.classification(), pvStatus, oracleStatus, oracleNodes,
                        d[0], d[1], d[2], d[3], g[0], g[1], g[2], g[3], g[4], g[5], g[6], g[7], g[8], g[9]));
            }
            if(witnesses) for(int i = 1; i <= 3; i++) for(int ref : new int[] {0, 4, 5, 2}) {
                if(ref == 2 && i != 3) continue;
                var a = results[i]; var b = results[ref];
                if(a.completed() && b.completed() && a.score() != b.score()) {
                    Witness w = witness(board, game, MODES[i], MODES[ref]);
                    boolean found = w.status().equals("first-excluded-check");
                    out.println(csv(f.name(), f.fen(), NAMES[i], NAMES[ref], a.score(), b.score(),
                            QuiescenceSeeCorpus.pv(a), QuiescenceSeeCorpus.pv(b), f.classification(),
                            w.path(), w.move(), w.qply(), w.ordinal(), w.evasions(),
                            found ? w.higherMode() == MODES[i] : "", found ? w.higherMode() == MODES[ref] : "", w.higherMode(), w.status()));
                }
            }
        }
    }

    record Witness(String path, String move, int qply, int ordinal, int evasions, int higherMode, String status) {}
    static Witness witness(long[] b, GameHistory game, int a, int c) {
        var history = GameHistory.builder(game); int used = 0; String path = "";
        ExactSearch qa = ExactSearch.quiescenceResearch(QuiescenceSeeCorpus.HCE, a), qc = ExactSearch.quiescenceResearch(QuiescenceSeeCorpus.HCE, c);
        for(int ply = 0; ply < 64; ply++) {
            var ra = search(qa, b, history.snapshot(), ply, used); var rc = search(qc, b, history.snapshot(), ply, used);
            if(!ra.completed() || !rc.completed()) return new Witness(path, "", ply, used + 1, -1, -1, "suffix-budget-incomplete");
            require(ra.score() != rc.score(), "Causal difference disappeared");
            boolean highA = ra.score() > rc.score(); var high = highA ? ra : rc;
            require(high.hasMove(), "Higher value has no continuation"); long move = high.bestMove();
            boolean quiet = ordinary(b, move); int lowerMode = highA ? c : a;
            if(quiet && !eligible(lowerMode, ply, used, ExhaustiveOracle.child(b, move))) return new Witness(path, Move.coordinate(move), ply, used + 1, ExhaustiveOracle.legalMoves(ExhaustiveOracle.child(b, move)).length, highA ? a : c, "first-excluded-check");
            b = ExhaustiveOracle.child(b, move); history.appendPosition(b);
            if(quiet) used++; path = (path + " " + Move.coordinate(move)).trim();
        }
        return new Witness(path, "", 64, used + 1, -1, -1, "witness-length-guard");
    }

    static ExactSearchResult search(ExactSearch q, long[] b, GameHistory g, int qply, int used) {
        return q.searchQuiescenceSuffix(b, g, qply, used, () -> q.visitedNodes() >= 1_000_000);
    }

    static boolean eligible(int mode, int qply, int used, long[] checkedChild) {
        if(mode < 36) return QuiescenceCheckLimitCorpus.eligible(mode, qply, used);
        return mode != 36 && qply == 0 || ExhaustiveOracle.legalMoves(checkedChild).length <= (mode == 38 ? 2 : 1);
    }
    static boolean ordinary(long[] board, long move) { return !QuiescenceOracle.inCheck(board) && !QuiescenceOracle.tactical(board, move); }

    static String pvChecks(long[] b, ExactSearchResult r, int mode) {
        int ply = 0, used = 0; String checks = "";
        for(long m : r.principalVariation()) {
            require(Arrays.stream(ExhaustiveOracle.legalMoves(b)).anyMatch(x -> x == m), "Illegal PV");
            boolean quiet = ordinary(b, m); long[] child = ExhaustiveOracle.child(b, m);
            if(quiet) {
                require(eligible(mode, ply, used, child) && QuiescenceOracle.inCheck(child), "Inadmissible quiet PV");
                checks = (checks + " " + ply + ":" + (++used) + ":" + Move.coordinate(m) + ":" + ExhaustiveOracle.legalMoves(child).length).trim();
            }
            b = child; ply++;
        }
        return checks;
    }

    static String verifyPv(long[] b, GameHistory game, ExactSearchResult result, int mode) {
        var history = GameHistory.builder(game); int ply = 0, used = 0;
        ExactSearch reference = ExactSearch.quiescenceResearch(QuiescenceSeeCorpus.HCE, mode);
        for(long m : result.principalVariation()) {
            if(ordinary(b, m)) used++;
            b = ExhaustiveOracle.child(b, m); history.appendPosition(b); ply++;
            var r = search(reference, b, history.snapshot(), ply, used);
            if(!r.completed()) return "legal-prefix;suffix-" + QuiescenceQuietCheckCorpus.reason(r, 0) + "-at-" + ply;
            int score = r.score(); if(score >= TranspositionScores.MATE_THRESHOLD) score -= ply; else if(score <= -TranspositionScores.MATE_THRESHOLD) score += ply;
            require((ply % 2 == 0 ? score : -score) == result.score(), "PV value mismatch mode=" + mode);
        }
        return "legal;all-suffix-values-matched";
    }

    static String best(ExactSearchResult r) { return r.hasMove() ? Move.coordinate(r.bestMove()) : "none"; }
    private static String csv(Object... fields) {
        StringBuilder s = new StringBuilder();
        for(Object f : fields) { if(!s.isEmpty()) s.append(','); s.append('"').append(f.toString().replace("\"", "\"\"")).append('"'); }
        return s.toString();
    }
    private static void require(boolean value, String message) { if(!value) throw new AssertionError(message); }
}
