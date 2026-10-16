package com.ohinteractive.seedv6.search.exact;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.See;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Fen;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;
import com.ohinteractive.seedv6.tools.search.ExactSearchHarness;

/** SR-001C research only. Complete enumeration on compact fixtures; preserved AB reference on tails. */
public final class QuiescenceDepthCorpus {
    static final String MATE_FIVE = "7k/5prp/4QB2/8/8/7R/8/4K2R w - - 0 1";
    static final String DRAW_FIVE = "7k/2b4n/1p6/qK6/8/Q7/8/R7 w - - 0 1";
    static final int[] MODES = {0, 4, 8, 12};
    record Fixture(String name, String fen, boolean exhaustive) {}
    record Witness(String path, String fen, int qply, String continuation, String categories) {}

    static List<Fixture> fixtures() {
        List<Fixture> result = new ArrayList<>();
        for(var f : QuiescenceSeeCorpus.FIXTURES) result.add(new Fixture(f.name(), f.fen(), true));
        result.add(new Fixture("mate-five", MATE_FIVE, true));
        result.add(new Fixture("draw-five", DRAW_FIVE, true));
        List<ExactSearchHarness.Position> positions = new ArrayList<>(ExactSearchHarness.positions());
        positions.addAll(ExactSearchHarness.orderingPositions());
        for(String spec : new String[] {"kiwipete/e1d1", "kiwipete/f3h3", "evasion/", "evasion/f1f2",
                "evasion/f3d4", "middlegame/d3d4"}) {
            String[] fields = spec.split("/", -1);
            long[] b = Board.fromFen(positions.stream().filter(p -> p.name().equals(fields[0])).findFirst().orElseThrow().fen());
            if(!fields[1].isEmpty()) b = ExhaustiveOracle.child(b, move(b, fields[1]));
            result.add(new Fixture("tail-" + spec.replace('/', '-'), Fen.fromBoard(b), false));
        }
        return result;
    }

    public static void main(String[] args) { run(System.out); }

    static void run(PrintStream out) {
        out.println("fixture,fen,method,limit,baseline_score,score,baseline_best,best,baseline_pv,pv,baseline_max_qply,qnodes,max_qply,oracle_nodes,truncation_qply,truncation_path,truncation_fen,omitted_continuation,categories");
        for(Fixture f : fixtures()) {
            long[] b = Board.fromFen(f.fen());
            require(!Board.isPlayerInCheck(b[0], b[1], b[2], b[3], 1 ^ Board.player((int)b[Board.STATUS])), "Illegal fixture");
            GameHistory game = GameHistory.initial(b);
            ExactSearchResult baseline = search(b, game, 0, 0);
            for(int mode : MODES) {
                ExactSearchResult result = search(b, game, mode, 0);
                long oracleNodes = 0;
                if(f.exhaustive()) {
                    QuiescenceOracle oracle = new QuiescenceOracle(QuiescenceSeeCorpus.HCE, mode);
                    require(oracle.score(b, new SearchLineHistory(game), 0, 0) == result.score(), "Oracle mismatch " + f.name());
                    oracleNodes = oracle.nodes;
                }
                verifyPv(b, game, result, mode);
                Witness w = baseline.score() == result.score() ? null : witness(b, game, mode, 0, "", "");
                out.println(csv(f.name(), f.fen(), f.exhaustive() ? "exhaustive" : "alpha-beta-reference", mode,
                        baseline.score(), result.score(), best(baseline), best(result), QuiescenceSeeCorpus.pv(baseline),
                        QuiescenceSeeCorpus.pv(result), baseline.maximumQply(), result.qnodes(), result.maximumQply(), oracleNodes,
                        w == null ? "" : w.qply(), w == null ? "" : w.path(), w == null ? "" : w.fen(),
                        w == null ? "" : w.continuation(), w == null ? "" : w.categories()));
            }
        }
    }

    static ExactSearchResult search(long[] b, GameHistory game, int mode, int qply) {
        ExactSearch q = ExactSearch.quiescenceResearch(QuiescenceSeeCorpus.HCE, mode);
        ExactSearchResult r = q.searchQuiescenceSuffix(b, game, qply, () -> q.visitedNodes() >= 1_000_000);
        require(r.completed(), "Reference budget at " + Fen.fromBoard(b));
        return r;
    }

    private static Witness witness(long[] b, GameHistory game, int limit, int qply, String path, String tags) {
        ExactSearchResult base = search(b, game, 0, qply), bounded = search(b, game, limit, qply);
        if(qply >= limit && !QuiescenceOracle.inCheck(b)) {
            require(base.score() > bounded.score() && base.hasMove(), "Noncausal boundary");
            String categories = add(tags, classify(b, base.bestMove()));
            if(qply > limit) categories = add(categories, "check-resolution-overrun");
            if(base.score() >= TranspositionScores.MATE_THRESHOLD) categories = add(categories, "mate-resource");
            if(base.score() == 0) categories = add(categories, "draw-resource");
            return new Witness(path, Fen.fromBoard(b), qply, QuiescenceSeeCorpus.pv(base), categories);
        }
        ExactSearchResult high = base.score() > bounded.score() ? base : bounded;
        require(high.hasMove() && qply < 64, "Missing divergence witness");
        long m = high.bestMove();
        long[] child = ExhaustiveOracle.child(b, m);
        var history = GameHistory.builder(game); history.appendPosition(child);
        return witness(child, history.snapshot(), limit, qply + 1, path.isEmpty() ? Move.coordinate(m) : path + " " + Move.coordinate(m),
                add(tags, classify(b, m)));
    }

    private static String classify(long[] b, long m) {
        if(QuiescenceOracle.promotion(m)) return "promotion-sequence";
        if(((m >>> Board.START_PIECE_SHIFT) & Piece.TYPE) == Piece.PAWN
                && Move.toSquare(m) == Board.enPassantSquare((int)b[Board.STATUS])) return "en-passant";
        if(QuiescenceOracle.tactical(b, m)) return See.atLeastGeneratedLegal(b, m, 0) ? "capture" : "sacrifice";
        return "quiet-evasion";
    }

    private static String add(String tags, String tag) {
        return Arrays.asList(tags.split(";")).contains(tag) ? tags : tags.isEmpty() ? tag : tags + ";" + tag;
    }

    private static void verifyPv(long[] b, GameHistory game, ExactSearchResult result, int mode) {
        var history = GameHistory.builder(game);
        int ply = 0;
        for(long m : result.principalVariation()) {
            require(Arrays.stream(ExhaustiveOracle.legalMoves(b)).anyMatch(x -> x == m), "Illegal PV");
            require(QuiescenceOracle.inCheck(b) || QuiescenceOracle.tactical(b, m), "Quiet check admitted");
            require(mode == 0 || ply < mode || QuiescenceOracle.inCheck(b), "PV crosses noncheck limit");
            b = ExhaustiveOracle.child(b, m); history.appendPosition(b); ply++;
            int score = search(b, history.snapshot(), mode, ply).score();
            if(score >= TranspositionScores.MATE_THRESHOLD) score -= ply;
            else if(score <= -TranspositionScores.MATE_THRESHOLD) score += ply;
            require((ply % 2 == 0 ? score : -score) == result.score(), "PV value/horizon mismatch");
        }
    }

    static long move(long[] b, String coordinate) {
        return Arrays.stream(ExhaustiveOracle.legalMoves(b)).filter(m -> Move.coordinate(m).equals(coordinate)).findFirst().orElseThrow();
    }
    private static String best(ExactSearchResult r) { return r.hasMove() ? Move.coordinate(r.bestMove()) : "none"; }
    private static String csv(Object... values) {
        return String.join(",", Arrays.stream(values).map(v -> "\"" + v.toString().replace("\"", "\"\"") + "\"").toList());
    }
    private static void require(boolean ok, String message) { if(!ok) throw new AssertionError(message); }
}
