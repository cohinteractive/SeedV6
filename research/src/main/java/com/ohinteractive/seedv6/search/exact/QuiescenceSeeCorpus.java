package com.ohinteractive.seedv6.search.exact;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.See;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;

/** Bounded SR-001B differential corpus and exhaustive witnesses. Test/research only. */
public final class QuiescenceSeeCorpus {
    record Fixture(String name, String fen) {}
    static final ExactEvaluator HCE = (b, p) -> Eval.evaluate(b);
    static final String[] MODES = {"QSEARCH_BASELINE", "SEE_ALL", "SEE_PROMO_SAFE", "SEE_CHECK_PROMO_SAFE"};
    static final List<Fixture> FIXTURES = List.of(
        new Fixture("winning", "4k3/8/8/3q4/4P3/8/8/4K3 w - - 0 1"),
        new Fixture("losing-defended", "3rk3/8/8/3p4/8/8/8/3QK3 w - - 0 1"),
        new Fixture("equal-defended", "rq2k3/8/8/8/8/8/8/R3K3 w - - 0 1"),
        new Fixture("bishop-xray", "4k3/6b1/8/8/3p4/8/3Q4/3RK3 w - - 0 1"),
        new Fixture("diagonal-xray", "4k3/8/6b1/8/8/3r4/2Q5/1B2K3 w - - 0 1"),
        new Fixture("successive-xrays", "7k/3r4/3q4/3r4/3p4/3R4/3Q4/K2R4 w - - 0 1"),
        new Fixture("pinned-defender", "4k3/3n4/8/1B2p3/5P2/8/8/4K3 w - - 0 1"),
        new Fixture("king-capture-evasion", "4k3/8/8/8/8/8/4r3/4K3 w - - 0 1"),
        new Fixture("unsafe-king-recapture", "8/8/4k3/3r4/8/1B6/8/3RK3 w - - 0 1"),
        new Fixture("ep-winning", "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1"),
        new Fixture("ep-defended", "4k3/8/8/3pP3/3r4/8/8/4K3 w - d6 0 1"),
        new Fixture("ep-black", "4k3/8/8/3R4/3Pp3/8/8/4K3 b - d3 0 1"),
        new Fixture("quiet-promotion", "7k/P7/8/8/8/8/8/7K w - - 0 1"),
        new Fixture("quiet-promotion-black", "k7/8/8/8/8/8/p7/7K b - - 0 1"),
        new Fixture("defended-quiet-promotion", "7k/P7/r7/8/8/8/8/7K w - - 0 1"),
        new Fixture("promotion-stalemate-resource", "3r4/1P6/8/8/8/1k6/p7/K7 w - - 0 1"),
        new Fixture("promotion-positional-resource", "3r4/1P6/8/7q/8/1k6/p7/K7 w - - 0 1"),
        new Fixture("capture-promotion", "1r5k/P7/8/8/8/8/8/7K w - - 0 1"),
        new Fixture("capture-promotion-black", "4k3/8/8/8/8/8/6p1/4K2R b - - 0 1"),
        new Fixture("promotion-recapture", "r6b/4k1P1/8/8/8/8/8/4K2R w - - 0 1"),
        new Fixture("rook-underpromotion", "8/k1P5/2K5/8/8/8/8/8 w - - 0 1"),
        new Fixture("checking-sacrifice", "5r1k/4Qpbp/4NN1N/8/8/8/8/4K3 w - - 0 1"),
        new Fixture("capture-mate", "7k/6p1/5KQ1/8/8/8/8/8 w - - 0 1"),
        new Fixture("quiet-evasions", "4r1k1/8/8/8/8/8/8/4K3 w - - 0 1"),
        new Fixture("negative-capture-evasion", "4k3/6b1/8/8/3b4/8/3Q4/6K1 w - - 0 1"),
        new Fixture("counterchecking-block", "4r3/8/8/8/8/8/8/2B1K1k1 w - - 0 1"),
        new Fixture("three-capture-xray", "7k/8/1p6/q7/8/Q7/8/R6K w - - 0 1"),
        new Fixture("nonchecking-clearance", "b1q4k/8/8/2p5/N7/8/8/R3K3 w - - 0 1"),
        new Fixture("tactical", "4k3/8/2p1p3/3q4/2P1P3/3Q4/8/4K3 w - - 0 1"),
        new Fixture("quiet-mate-excluded", "7k/8/5KQ1/8/8/8/8/8 w - - 0 1"),
        new Fixture("fifty-evasion", "4r1k1/8/8/8/8/8/8/4K3 w - - 99 1"),
        new Fixture("mate-before-fifty", "7k/6Q1/5K2/8/8/8/8/8 b - - 100 1"),
        new Fixture("stalemate", "7k/5K2/6Q1/8/8/8/8/8 b - - 0 1"),
        new Fixture("insufficient", "4k3/8/8/8/8/8/8/4K3 w - - 0 1")
    );

    record Witness(String path, String move, int qply, boolean promotion, boolean check,
                   int baselineNodeScore, int candidateNodeScore, int baselineMoveScore) {}

    public static void main(String[] args) { run(System.out); }

    static void run(PrintStream out) {
        out.println("fixture,fen,mode,baseline_score,score,best,pv,qnodes,max_qply,oracle_nodes,baseline_pv,witness_path,pruned_move,see_sign,promotion,gives_check,qply,witness_baseline_score,witness_candidate_score,witness_move_score");
        for(Fixture fixture : FIXTURES) {
            long[] board = Board.fromFen(fixture.fen());
            int player = Board.player((int) board[Board.STATUS]);
            require(!Board.isPlayerInCheck(board[0], board[1], board[2], board[3], 1 ^ player),
                    "Side not to move is checked: " + fixture.name());
            GameHistory game = GameHistory.initial(board);
            ExactSearchResult baseline = ExactSearch.quiescenceResearch(HCE).search(board, 0);
            for(int mode = 0; mode < MODES.length; mode++) {
                QuiescenceOracle oracle = new QuiescenceOracle(HCE, mode);
                int expected = oracle.score(board, new SearchLineHistory(game), 0, 0);
                ExactSearch search = ExactSearch.quiescenceResearch(HCE, mode);
                ExactSearchResult result = search.search(board, game, 0, () -> search.visitedNodes() >= 200_000);
                require(result.completed() && expected == result.score(), fixture.name() + "/" + MODES[mode] + " oracle mismatch");
                verifyPv(board, game, result, mode);
                Witness witness = result.score() == baseline.score() ? null
                        : witness(board, new SearchLineHistory(game), 0, mode, "");
                out.println(csv(fixture.name(), fixture.fen(), MODES[mode], baseline.score(), result.score(),
                        result.hasMove() ? Move.coordinate(result.bestMove()) : "none", pv(result), result.qnodes(),
                        result.maximumQply(), oracle.nodes, pv(baseline), witness == null ? "" : witness.path(),
                        witness == null ? "" : witness.move(), witness == null ? "" : "negative",
                        witness == null ? "" : witness.promotion(), witness == null ? "" : witness.check(),
                        witness == null ? "" : witness.qply(), witness == null ? "" : witness.baselineNodeScore(),
                        witness == null ? "" : witness.candidateNodeScore(), witness == null ? "" : witness.baselineMoveScore()));
            }
        }
    }

    private static void verifyPv(long[] board, GameHistory game, ExactSearchResult result, int mode) {
        SearchLineHistory history = new SearchLineHistory(game);
        int ply = 0;
        for(long move : result.principalVariation()) {
            require(Arrays.stream(ExhaustiveOracle.legalMoves(board)).anyMatch(m -> m == move), "Illegal PV");
            require(QuiescenceOracle.inCheck(board) || QuiescenceOracle.tactical(board, move), "Quiet non-evasion PV");
            long[] child = ExhaustiveOracle.child(board, move);
            require(!QuiescenceOracle.prunes(board, move, child, mode), "Pruned PV move");
            board = child;
            history.pushRealPosition(board);
            int value = value(board, history, ++ply, mode);
            require((ply % 2 == 0 ? value : -value) == result.score(), "PV does not realize candidate value");
        }
    }

    /** Follow the higher-valued side's optimal continuation until the first causally relevant omission.
     * This handles optimistic scores caused by pruning an opponent reply, not just root/PV deletions. */
    private static Witness witness(long[] board, SearchLineHistory history, int ply, int mode, String path) {
        int baseline = value(board, history, ply, 0), candidate = value(board, history, ply, mode);
        boolean checked = QuiescenceOracle.inCheck(board);
        for(long move : ExhaustiveOracle.legalMoves(board)) {
            if(!checked && !QuiescenceOracle.tactical(board, move)) continue;
            long[] child = ExhaustiveOracle.child(board, move);
            boolean pruned = QuiescenceOracle.prunes(board, move, child, mode);
            history.pushRealPosition(child);
            try {
                int baseMove = -value(child, history, ply + 1, 0);
                if(baseline > candidate && baseMove == baseline && pruned) {
                    require(!See.atLeastGeneratedLegal(board, move, 0), "Witness not SEE-negative");
                    return new Witness(path, Move.coordinate(move), ply, QuiescenceOracle.promotion(move),
                            QuiescenceOracle.inCheck(child), baseline, candidate, baseMove);
                }
                if(pruned) continue;
                int candidateMove = -value(child, history, ply + 1, mode);
                if(baseMove != candidateMove && (baseline > candidate ? baseMove == baseline : candidateMove == candidate))
                    return witness(child, history, ply + 1, mode, path.isEmpty() ? Move.coordinate(move) : path + " " + Move.coordinate(move));
            } finally { history.popRealPosition(); }
        }
        throw new AssertionError("No causal pruning witness at " + path);
    }

    private static int value(long[] board, SearchLineHistory history, int ply, int mode) {
        return new QuiescenceOracle(HCE, mode).score(board, history, 0, ply);
    }

    static String pv(ExactSearchResult result) {
        StringJoiner text = new StringJoiner(" ");
        for(long move : result.principalVariation()) text.add(Move.coordinate(move));
        return text.toString();
    }

    private static String csv(Object... values) {
        StringJoiner text = new StringJoiner(",");
        for(Object value : values) text.add("\"" + value.toString().replace("\"", "\"\"") + "\"");
        return text.toString();
    }

    private static void require(boolean condition, String message) { if(!condition) throw new AssertionError(message); }
}
