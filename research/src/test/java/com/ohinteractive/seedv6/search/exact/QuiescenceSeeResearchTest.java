package com.ohinteractive.seedv6.search.exact;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.See;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.rules.DrawAdjudicator;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;
import com.ohinteractive.seedv6.tools.search.ExactSearchHarness;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(30)
class QuiescenceSeeResearchTest {
    @Test void baselineMatchesAllFourteenSr001aRecordedTreesExactly() {
        String[] expected = {
            "start|2|0|119|98|2|g1f3 b8c6",
            "kiwipete|2|61|18920|18871|23|e2a6 h3g2 f3g2 b4c3 d2c3 e6d5 e5g6 f7g6 g2g6 e7f7",
            "endgame|2|165|115|100|4|b4f4 h4g3",
            "tactical|2|1072|112|87|3|c4d5 c6d5 e4d5",
            "evasion|2|-410|18333|18326|28|g1h1 b2a1q d1a1 a3b4",
            "middlegame|2|111|14004|13957|19|c3d5 e7d8 g5f6 g7f6",
            "transposition-pawns|2|0|35|25|0|e1d2 e8d7",
            "start|3|38|1074|978|6|b1c3 d7d5 g1f3",
            "kiwipete|3|32|108281|108106|26|e2a6 e6d5 c3d5 e7e5",
            "endgame|3|167|396|352|4|b4f4 h4g3 f4f7",
            "tactical|3|1072|507|444|5|c4d5 c6d5 e4d5",
            "evasion|3|-503|30768|30656|27|c4c5 b6c5 d2d4 b2a1q d1a1 a3b4",
            "middlegame|3|111|62045|61721|24|c3d5 e7d8 g5f6 g7f6",
            "transposition-pawns|3|3|146|111|0|e1d2 e8d7 d2d3"
        };
        List<ExactSearchHarness.Position> positions = new ArrayList<>(ExactSearchHarness.positions());
        positions.addAll(ExactSearchHarness.orderingPositions());
        for(String row : expected) {
            String[] fields = row.split("\\|");
            long[] board = Board.fromFen(positions.stream().filter(p -> p.name().equals(fields[0])).findFirst().orElseThrow().fen());
            int depth = Integer.parseInt(fields[1]);
            ExactSearchResult result = owner(0).search(board, depth);
            assertTrue(result.completed());
            assertEquals(Integer.parseInt(fields[2]), result.score(), row);
            assertEquals(Long.parseLong(fields[3]), result.nodes(), row);
            assertEquals(Long.parseLong(fields[4]), result.qnodes(), row);
            assertEquals(Integer.parseInt(fields[5]), result.maximumQply(), row);
            assertEquals(fields[6], QuiescenceSeeCorpus.pv(result), row);
            same(result, ExactSearch.quiescenceResearch(QuiescenceSeeCorpus.HCE).search(board, depth));
        }
    }

    @Test void allCorpusValuesAndPvsMatchCompleteCandidateOraclesAndHaveCausalWitnesses() {
        try(PrintStream out = new PrintStream(OutputStream.nullOutputStream())) { QuiescenceSeeCorpus.run(out); }
    }

    @Test void rootMoveVisitsMatchEachEligibilityRuleIncludingPromotionsChecksAndEvasions() {
        int ordinaryPruned = 0, promotionsSaved = 0, checksSaved = 0, negativeEvasions = 0, epVisited = 0;
        for(var fixture : QuiescenceSeeCorpus.FIXTURES) {
            long[] board = Board.fromFen(fixture.fen());
            long[] legal = ExhaustiveOracle.legalMoves(board);
            boolean checked = QuiescenceOracle.inCheck(board);
            boolean draw = legal.length != 0 && DrawAdjudicator.adjudicateNonTerminal(board,
                    new SearchLineHistory(GameHistory.initial(board))) != DrawAdjudicator.RuleDraw.NONE;
            for(int mode = 0; mode < 4; mode++) {
                List<Long> visited = new ArrayList<>(), expected = new ArrayList<>();
                ExactSearch q = ExactSearch.quiescenceResearch(new ExactEvaluator() {
                    @Override public int evaluate(long[] b, int ply) {
                        assertFalse(QuiescenceOracle.inCheck(b));
                        return Eval.evaluate(b);
                    }
                    @Override public void child(long[] parent, long[] child, int parentPly) {
                        if(parentPly == 0) visited.add(child[Board.KEY]);
                    }
                }, mode);
                assertTrue(q.search(board, 0).completed());
                if(!draw) for(long move : legal) {
                    if(!checked && !QuiescenceOracle.tactical(board, move)) continue;
                    long[] child = ExhaustiveOracle.child(board, move);
                    boolean pruned = QuiescenceOracle.prunes(board, move, child, mode);
                    if(!pruned) expected.add(child[Board.KEY]);
                    if(!QuiescenceOracle.tactical(board, move)) continue;
                    boolean negative = !See.atLeastGeneratedLegal(board, move, 0);
                    if(negative && !checked && !QuiescenceOracle.promotion(move) && !QuiescenceOracle.inCheck(child) && mode > 0) {
                        assertTrue(pruned); ordinaryPruned++;
                    }
                    if(negative && QuiescenceOracle.promotion(move) && mode >= ExactSearch.SEE_PROMO_SAFE) {
                        assertFalse(pruned); promotionsSaved++;
                    }
                    if(negative && QuiescenceOracle.inCheck(child) && mode == ExactSearch.SEE_CHECK_PROMO_SAFE) {
                        assertFalse(pruned); checksSaved++;
                    }
                    if(negative && checked) { assertFalse(pruned); negativeEvasions++; }
                    if(fixture.name().startsWith("ep-") && !pruned) epVisited++;
                }
                expected.sort(Long::compare); visited.sort(Long::compare);
                assertEquals(expected, visited, fixture.name() + "/" + mode);
            }
        }
        assertTrue(ordinaryPruned > 0);
        assertTrue(promotionsSaved >= 8); // All four quiet-promotion types in both safe candidates.
        assertTrue(checksSaved > 0);
        assertTrue(epVisited > 0);
        assertTrue(negativeEvasions > 0);
    }

    @Test void checkedLosingCaptureIsNotSubjectToSeePruning() {
        long[] board = Board.fromFen(fixture("negative-capture-evasion"));
        long capture = move(board, "d2d4");
        assertTrue(QuiescenceOracle.inCheck(board));
        assertFalse(See.atLeastGeneratedLegal(board, capture, 0));
        final long key = ExhaustiveOracle.child(board, capture)[Board.KEY];
        for(int mode = 1; mode <= 3; mode++) {
            List<Long> visited = new ArrayList<>();
            ExactSearch q = ExactSearch.quiescenceResearch(new ExactEvaluator() {
                @Override public int evaluate(long[] b, int ply) { return Eval.evaluate(b); }
                @Override public void child(long[] parent, long[] child, int parentPly) {
                    if(parentPly == 0) visited.add(child[Board.KEY]);
                }
            }, mode);
            assertTrue(q.search(board, 0).completed());
            assertTrue(visited.contains(key));
        }
    }

    @Test void promotionAndCheckingSafeguardsPreserveDistinctTacticalResources() {
        long[] promotion = Board.fromFen(fixture("promotion-stalemate-resource"));
        assertEquals(0, owner(0).search(promotion, 0).score());
        assertEquals(-904, owner(1).search(promotion, 0).score());
        assertEquals(0, owner(2).search(promotion, 0).score());
        assertEquals(0, owner(3).search(promotion, 0).score());
        long[] sacrifice = Board.fromFen(fixture("checking-sacrifice"));
        assertFalse(See.atLeastGeneratedLegal(sacrifice, move(sacrifice, "e7f8"), 0));
        assertEquals(32765, owner(0).search(sacrifice, 0).score());
        for(int mode : new int[] {1, 2}) assertEquals(1076, owner(mode).search(sacrifice, 0).score());
        assertEquals(32765, owner(3).search(sacrifice, 0).score());
        long[] clearance = Board.fromFen(fixture("nonchecking-clearance"));
        assertEquals(-410, owner(0).search(clearance, 0).score());
        for(int mode = 1; mode <= 3; mode++) assertEquals(-691, owner(mode).search(clearance, 0).score());
    }

    @Test void middlegameBenchmarkDifferenceComesFromPruningTheNoncheckingBishopExchange() {
        // The depth-2 benchmark boundary after c3d5 e7d8. Too large for the bounded
        // exhaustive fixture oracle; use the preserved full-window SR-001A reference.
        long[] board = Board.fromFen("r2q1rk1/1pp2ppp/p1np1n2/2bNp1B1/2B1P1b1/P2P1N2/1PP1QPPP/R4RK1 w - - 2 11");
        long bishop = move(board, "g5f6");
        long[] child = ExhaustiveOracle.child(board, bishop);
        assertFalse(See.atLeastGeneratedLegal(board, bishop, 0));
        assertFalse(QuiescenceOracle.promotion(bishop));
        assertFalse(QuiescenceOracle.inCheck(child));
        ExactSearchResult baseline = owner(0).search(board, 0);
        assertEquals(111, baseline.score());
        assertEquals("g5f6 g7f6", QuiescenceSeeCorpus.pv(baseline));
        for(int mode = 1; mode <= 3; mode++) {
            ExactSearchResult candidate = owner(mode).search(board, 0);
            assertTrue(candidate.completed());
            assertEquals(84, candidate.score());
            assertEquals("d5f6 g7f6", QuiescenceSeeCorpus.pv(candidate));
            assertEquals(111, -owner(mode).search(child, 0).score());
        }
    }

    @Test void candidateMateDistanceFailSoftAndCancellationKeepExistingContracts() {
        long[] capture = Board.fromFen(fixture("winning"));
        GameHistory game = GameHistory.initial(capture);
        long[] mate = Board.fromFen(fixture("capture-mate"));
        for(int mode = 1; mode <= 3; mode++) {
            ExactSearch q = owner(mode);
            for(int depth : new int[] {0, 1}) assertEquals(32767, q.search(mate, depth).score());
            int stand = Eval.evaluate(capture);
            ExactSearchResult stop = q.searchWindow(capture, game, 0, stand - 20, stand - 10, ExactSearch.NEVER_CANCELLED);
            assertEquals(stand, stop.score()); assertEquals(1, stop.nodes()); assertFalse(stop.hasMove());
            int exact = new QuiescenceOracle(QuiescenceSeeCorpus.HCE, mode).score(capture, new SearchLineHistory(game), 0, 0);
            int high = q.searchWindow(capture, game, 0, stand, stand + 1, ExactSearch.NEVER_CANCELLED).score();
            int low = q.searchWindow(capture, game, 0, exact + 1, exact + 20, ExactSearch.NEVER_CANCELLED).score();
            assertTrue(high > stand + 1 && high <= exact);
            assertTrue(low >= exact && low <= exact + 1);
            aborted(q.search(capture, game, 0, () -> true));
            long[] branching = Board.fromFen(fixture("capture-promotion"));
            aborted(q.search(branching, GameHistory.initial(branching), 0, () -> q.visitedNodes() >= 3));
            AtomicBoolean cancelled = new AtomicBoolean();
            ExactSearch afterEval = ExactSearch.quiescenceResearch((b, ply) -> {
                if(ply > 0) cancelled.set(true);
                return Eval.evaluate(b);
            }, mode);
            aborted(afterEval.search(capture, game, 0, cancelled::get));
            try {
                Thread.currentThread().interrupt();
                aborted(q.search(capture, 0));
                assertTrue(Thread.currentThread().isInterrupted());
            } finally { Thread.interrupted(); }
            same(owner(mode).search(capture, 0), q.search(capture, 0));
        }
    }

    @Test void candidatesRetainRealHistoryRepetitionOnQuietQevasion() {
        long[] board = Board.fromFen("r6k/8/8/8/8/8/8/1K6 b - - 1 1");
        GameHistory.Builder history = GameHistory.builder(board);
        String[] cycle = {"a8b8", "b1a1", "b8a8", "a1b1"};
        for(int i = 0; i < 7; i++) {
            board = ExhaustiveOracle.child(board, move(board, cycle[i % 4]));
            history.appendPosition(board);
        }
        for(int mode = 1; mode <= 3; mode++) {
            ExactSearch q = ExactSearch.quiescenceResearch((b, p) -> 37, mode);
            ExactSearchResult draw = q.search(board, history.snapshot(), 0, ExactSearch.NEVER_CANCELLED);
            assertEquals(0, draw.score()); assertEquals("a1b1", Move.coordinate(draw.bestMove()));
            assertEquals(-37, q.search(board, 0).score());
        }
        assertThrows(IllegalArgumentException.class, () -> owner(-1));
        assertThrows(IllegalArgumentException.class, () -> owner(5));
    }

    @Test void positiveNormalDepthMatchesExhaustiveCandidateValueWithoutNormalPruning() {
        for(String name : new String[] {"losing-defended", "ep-winning", "capture-promotion", "checking-sacrifice"}) {
            long[] board = Board.fromFen(fixture(name));
            for(int mode = 1; mode <= 3; mode++) {
                int expected = new QuiescenceOracle(QuiescenceSeeCorpus.HCE, mode)
                        .score(board, new SearchLineHistory(GameHistory.initial(board)), 1, 0);
                ExactSearchResult result = owner(mode).search(board, 1);
                assertTrue(result.completed()); assertEquals(expected, result.score());
                assertEquals(1, result.normalNodes());
            }
        }
    }

    private static String fixture(String name) {
        return QuiescenceSeeCorpus.FIXTURES.stream().filter(f -> f.name().equals(name)).findFirst().orElseThrow().fen();
    }
    private static ExactSearch owner(int mode) { return ExactSearch.quiescenceResearch(QuiescenceSeeCorpus.HCE, mode); }
    private static long move(long[] board, String coordinate) {
        return Arrays.stream(ExhaustiveOracle.legalMoves(board)).filter(m -> Move.coordinate(m).equals(coordinate)).findFirst().orElseThrow();
    }
    private static void same(ExactSearchResult a, ExactSearchResult b) {
        assertTrue(b.completed()); assertEquals(a.score(), b.score()); assertEquals(a.bestMove(), b.bestMove());
        assertEquals(a.nodes(), b.nodes()); assertEquals(a.qnodes(), b.qnodes());
        assertEquals(a.maximumQply(), b.maximumQply()); assertArrayEquals(a.principalVariation(), b.principalVariation());
    }
    private static void aborted(ExactSearchResult result) {
        assertFalse(result.completed()); assertEquals(-1, result.completedDepth());
        assertEquals(Value.INVALID, result.score()); assertFalse(result.hasMove());
        assertEquals(0, result.principalVariation().length);
    }
}
