package com.ohinteractive.seedv6.search.exact;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.util.Fen;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;
import com.ohinteractive.seedv6.rules.PositionIdentity;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;
import com.ohinteractive.seedv6.tools.search.ExactSearchHarness;

/** SR-001D equivalence evidence. Counters enabled; this is not a timing benchmark. */
public final class QuiescenceTtCorpus {
    public static void main(String[] args) {
        if(args.length == 1 && args[0].equals("--identity")) identity(System.out);
        else run(System.out);
    }

    /** Allocation-heavy test instrumentation, never used for timing or as a reuse key. */
    static void identity(PrintStream out) {
        out.println("fixture,depth,qnodes,distinct_board_keys,distinct_board_rule_states,distinct_search_keys,board_duplicates,board_rule_duplicates,search_key_duplicates,first_repeated_path,second_repeated_path,repeated_fen");
        List<ExactSearchHarness.Position> positions = new ArrayList<>(ExactSearchHarness.orderingPositions());
        positions.add(ExactSearchHarness.positions().get(2));
        positions.add(new ExactSearchHarness.Position("controlled-tactical", QuiescenceSeeCorpus.FIXTURES.get(28).fen()));
        for(var p : positions) {
            int depth = p.name().equals("controlled-tactical") ? 0 : 2;
            long[] b = Board.fromFen(p.fen());
            IdentityAudit audit = new IdentityAudit(depth);
            ExactSearch q = ExactSearch.quiescenceResearch(audit);
            var r = q.search(b, GameHistory.initial(b), depth, () -> q.visitedNodes() >= 1_000_000);
            require(r.completed() && r.qnodes() == audit.nodes, "Identity audit budget/coverage");
            out.println(csv(p.name(), depth, r.qnodes(), audit.boards.size(), audit.rules.size(), audit.keys.size(),
                    audit.nodes - audit.boards.size(), audit.nodes - audit.rules.size(), audit.nodes - audit.keys.size(),
                    audit.first, audit.second, audit.fen));
        }
    }

    private record RuleKey(long board, long status) {}
    private static final class IdentityAudit implements ExactEvaluator {
        final int depth;
        final long[] histories = new long[257];
        final String[] paths = new String[257];
        final HashSet<Long> boards = new HashSet<>(), keys = new HashSet<>();
        final HashSet<RuleKey> rules = new HashSet<>();
        final HashMap<Long, String> witnesses = new HashMap<>();
        long nodes;
        String first = "", second = "", fen = "";
        IdentityAudit(int depth) { this.depth = depth; }
        public int evaluate(long[] b, int ply) { return Eval.evaluate(b); }
        public void initialize(long[] b) {
            histories[0] = SearchKey.rootHistory(b, GameHistory.initial(b)); paths[0] = "";
            if(depth == 0) record(b, 0);
        }
        public void child(long[] parent, long[] b, int ply) {
            histories[ply + 1] = SearchKey.childHistory(histories[ply], PositionIdentity.repetitionKey(b), (int)b[Board.STATUS]);
            if(depth == 0) for(long m : ExhaustiveOracle.legalMoves(parent)) {
                if(Arrays.equals(ExhaustiveOracle.child(parent, m), b)) {
                    paths[ply + 1] = (paths[ply] + " " + Move.coordinate(m)).trim(); break;
                }
            }
            if(ply + 1 >= depth) record(b, ply + 1);
        }
        void record(long[] b, int ply) {
            nodes++; boards.add(b[Board.KEY]); rules.add(new RuleKey(b[Board.KEY], b[Board.STATUS]));
            long key = SearchKey.key(b, histories[ply]); keys.add(key);
            if(depth == 0) {
                String old = witnesses.putIfAbsent(key, paths[ply]);
                if(old != null && first.isEmpty()) { first = old; second = paths[ply]; fen = Fen.fromBoard(b); }
            }
        }
    }

    static void run(PrintStream out) {
        ExactSearch base = ExactSearch.quiescenceResearch(QuiescenceSeeCorpus.HCE);
        ExactSearch qtt = ExactSearch.quiescenceResearch(QuiescenceSeeCorpus.HCE, ExactSearch.QSEARCH_QTT, true);
        out.println("fixture,fen,method,window,alpha,beta,baseline_score,qtt_score,baseline_best,qtt_best,baseline_pv,qtt_pv,baseline_qnodes,qtt_qnodes,baseline_max_qply,qtt_max_qply,oracle_nodes,probes,hits,exact,lower,upper,save_attempts");
        for(var f : QuiescenceDepthCorpus.fixtures()) {
            long[] board = Board.fromFen(f.fen());
            GameHistory game = GameHistory.initial(board);
            ExactSearchResult full = search(base, board, game, -ExactSearch.INFINITY, ExactSearch.INFINITY);
            long oracleNodes = 0;
            if(f.exhaustive()) {
                QuiescenceOracle oracle = new QuiescenceOracle(QuiescenceSeeCorpus.HCE);
                require(oracle.score(board, new SearchLineHistory(game), 0, 0) == full.score(), "Oracle " + f.name());
                oracleNodes = oracle.nodes;
            }
            int s = full.score();
            int[][] windows = {{-ExactSearch.INFINITY, ExactSearch.INFINITY}, {s - 1, s + 1},
                    {s - 10, s}, {s, s + 10}};
            for(int w = 0; w < windows.length; w++) {
                int alpha = Math.max(-ExactSearch.INFINITY, windows[w][0]);
                int beta = Math.min(ExactSearch.INFINITY, windows[w][1]);
                ExactSearchResult off = search(base, board, game, alpha, beta);
                ExactSearchResult on = search(qtt, board, game, alpha, beta);
                bound(off.score(), s, alpha, beta); bound(on.score(), s, alpha, beta);
                if(s > alpha && s < beta) {
                    require(off.score() == on.score(), "Exact score " + f.name());
                    verifyPv(board, game, on, base);
                } else legalPv(board, on);
                long[] c = qtt.qttDiagnostics();
                out.println(csv(f.name(), f.fen(), f.exhaustive() ? "exhaustive" : "alpha-beta-reference",
                        new String[] {"full", "around-exact", "fail-high", "fail-low"}[w], alpha, beta,
                        off.score(), on.score(), best(off), best(on), QuiescenceSeeCorpus.pv(off), QuiescenceSeeCorpus.pv(on),
                        off.qnodes(), on.qnodes(), off.maximumQply(), on.maximumQply(), oracleNodes,
                        c[0], c[1], c[2], c[3], c[4], c[5]));
            }
        }
    }

    static ExactSearchResult search(ExactSearch search, long[] b, GameHistory h, int a, int beta) {
        ExactSearchResult r = search.searchWindow(b, h, 0, a, beta, () -> search.visitedNodes() >= 1_000_000);
        require(r.completed(), "Corpus budget");
        return r;
    }

    static void bound(int returned, int exact, int alpha, int beta) {
        if(returned <= alpha) require(exact <= returned, "Invalid upper bound");
        else if(returned >= beta) require(exact >= returned, "Invalid lower bound");
        else require(returned == exact, "Invalid exact value");
    }

    static void verifyPv(long[] b, GameHistory game, ExactSearchResult r, ExactSearch reference) {
        var path = GameHistory.builder(game);
        int ply = 0;
        for(long move : r.principalVariation()) {
            legalMove(b, move);
            b = ExhaustiveOracle.child(b, move); path.appendPosition(b); ply++;
            int s = search(reference, b, path.snapshot(), -ExactSearch.INFINITY, ExactSearch.INFINITY).score();
            if(s >= TranspositionScores.MATE_THRESHOLD) s -= ply;
            else if(s <= -TranspositionScores.MATE_THRESHOLD) s += ply;
            require((ply % 2 == 0 ? s : -s) == r.score(), "Unsupported PV value");
        }
    }

    private static void legalPv(long[] b, ExactSearchResult r) {
        for(long move : r.principalVariation()) { legalMove(b, move); b = ExhaustiveOracle.child(b, move); }
    }
    private static void legalMove(long[] b, long move) {
        require(Arrays.stream(ExhaustiveOracle.legalMoves(b)).anyMatch(m -> m == move), "Illegal qPV");
        require(QuiescenceOracle.inCheck(b) || QuiescenceOracle.tactical(b, move), "Quiet non-evasion qPV");
    }
    private static String best(ExactSearchResult r) { return r.hasMove() ? Move.coordinate(r.bestMove()) : "none"; }
    private static String csv(Object... values) {
        return String.join(",", Arrays.stream(values).map(v -> "\"" + v.toString().replace("\"", "\"\"") + "\"").toList());
    }
    private static void require(boolean ok, String message) { if(!ok) throw new AssertionError(message); }
}
