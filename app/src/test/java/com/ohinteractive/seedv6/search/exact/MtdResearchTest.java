package com.ohinteractive.seedv6.search.exact;

import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.search.exact.MtdResearchSearch.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.SearchDriver;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;

@Timeout(120)
class MtdResearchTest {
    @Test void representativeScoresAndEveryPvPrefixMatchIndependentReferenceAndUnprunedDriver() {
        for(var p : AspirationResearch.positions()) {
            int depth = p.deep() ? 4 : 3;
            var board = Board.fromFen(p.fen());
            var c = MtdResearch.run(p, Policy.CONTROL, depth, null, false);
            var production = new ArrayList<SearchResult>();
            // Preserve this exact-driver experiment's original unpruned control.
            new SearchDriver(SearchEvaluation.handcraftedIsolation()).search(new SearchRequest(board, depth, observer(production)));
            assertEquals(production, c.published(), p.name());
            for(var policy : Policy.values()) {
                var r = MtdResearch.run(p, policy, depth, c, false);
                MtdResearch.compare(c, r, p.name());
                for(var result : r.published()) {
                    assertEquals(new ExactSearch().search(board, result.depth()).score(), result.score(), p.name());
                    AspirationResearch.verifyPv(board, GameHistory.initial(board), result);
                }
                assertConvergence(r.iterations());
            }
        }
    }

    @Test void everyScoreHasSafeAdjacentThresholdsIncludingMateBandsAndSentinelsAreRejected() {
        for(int v = -ExactSearch.MATE_SCORE; v <= ExactSearch.MATE_SCORE; v++) {
            assertEquals(v - 1, predecessor(v)); assertEquals(v + 1, successor(v));
            assertTrue(predecessor(v) >= -ExactSearch.INFINITY && successor(v) <= ExactSearch.INFINITY);
        }
        for(int invalid : new int[] {Value.INVALID, Integer.MAX_VALUE, -ExactSearch.INFINITY, ExactSearch.INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> predecessor(invalid));
            assertThrows(IllegalArgumentException.class, () -> successor(invalid));
        }
        // Exercise actual root searches immediately around both ordinary/mate boundaries.
        int max = TranspositionScores.MAX_NORMAL_SCORE;
        for(int value : new int[] {-max, -max + 1, -1, 0, 1, max - 1, max})
            for(var policy : Policy.values()) {
                var facility = new MtdResearchSearch(policy, (b, ply) -> value, new TTable(1),
                        d -> (d & 1) == 0 ? value : -value);
                var result = new SearchDriver(facility).search(new SearchRequest(Board.startingPosition(), 3));
                assertTrue(result.targetDepthCompleted()); assertEquals(-value, result.lastCompletedResult().score());
                assertConvergence(facility.iterations);
            }
    }

    @Test void extremeGuessesAreNotBoundsAndMateDistancesAndTerminalResultsRemainExact() {
        for(String name : new String[] {"mate", "forced-loss", "start"}) {
            var p = AspirationResearch.positions().stream().filter(x -> x.name().equals(name)).findFirst().orElseThrow();
            var board = Board.fromFen(p.fen());
            for(int guess : new int[] {-ExactSearch.MATE_SCORE, -TranspositionScores.MATE_THRESHOLD,
                    TranspositionScores.MATE_THRESHOLD, ExactSearch.MATE_SCORE}) {
                var f = new MtdResearchSearch(Policy.MTD_ORACLE, (b, ply) -> Eval.evaluate(b), new TTable(1), d -> guess);
                new SearchDriver(f).search(new SearchRequest(board, name.equals("start") ? 3 : 5));
                for(var i : f.iterations) {
                    assertEquals(new ExactSearch().search(board, i.depth()).score(), i.result().score());
                    AspirationResearch.verifyPv(board, GameHistory.initial(board), i.result());
                }
                if(name.equals("mate")) assertEquals(32767, f.iterations.getLast().result().score());
                if(name.equals("forced-loss")) assertEquals(-32764, f.iterations.getLast().result().score());
                assertConvergence(f.iterations);
            }
        }
        for(String fen : new String[] {"7k/6Q1/5K2/8/8/8/8/8 b - - 100 1",
                "7k/5K2/6Q1/8/8/8/8/8 b - - 0 1", "4k3/8/8/8/8/8/8/R3K3 w - - 100 1"}) {
            var b = Board.fromFen(fen);
            var f = new MtdResearchSearch(Policy.MTD_PREV, (x, ply) -> { throw new AssertionError("Terminal leaf"); }, new TTable(1), null);
            var r = new SearchDriver(f).search(new SearchRequest(b, 5));
            assertTrue(r.targetDepthCompleted()); assertTrue(r.terminalRoot());
            assertEquals(1, r.lastCompletedResult().depth()); assertFalse(r.lastCompletedResult().hasMove());
            assertEquals(new ExactSearch().search(b, 1).score(), r.lastCompletedResult().score());
        }
    }

    @Test void sameGenerationRootBoundsSurviveAndExactMaterializationReplacesThem() {
        var board = Board.startingPosition();
        long key = SearchKey.key(board, SearchKey.rootHistory(board, GameHistory.initial(board)));
        int[] generations = {0}, invocation = {0}, currentDepthRootHits = {0};
        var saves = new ArrayList<Integer>();
        var table = new TTable(1) {
            @Override public void advanceGeneration() { generations[0]++; super.advanceGeneration(); }
            @Override public boolean probe(long k, TEntry e) {
                boolean hit = super.probe(k, e);
                if(hit && k == key && invocation[0] > 2 && (e.data & 255) == 2) {
                    assertEquals(1, (e.data >>> 10) & 255); currentDepthRootHits[0]++;
                }
                return hit;
            }
            @Override public void save(long k, int depth, int type, int score, long move) {
                if(k == key && depth == 2) saves.add(type);
                super.save(k, depth, type, score, move);
            }
        };
        var f = new MtdResearchSearch(Policy.MTD_PREV, new ExactEvaluator() {
            @Override public void initialize(long[] b) { invocation[0]++; }
            @Override public int evaluate(long[] b, int ply) { return 400; }
        }, table, null);
        new SearchDriver(f).search(new SearchRequest(board, 2));
        assertEquals(1, generations[0]); assertTrue(currentDepthRootHits[0] >= 2);
        assertEquals(List.of(TTable.TYPE_LOWER, TTable.TYPE_UPPER, TTable.TYPE_EXACT), saves);
        var entry = new TTable.TEntry(); assertTrue(table.probe(key, entry));
        assertEquals(TTable.TYPE_EXACT, (entry.data >>> 8) & 3);
        assertEquals(400, (int) (entry.data >> 32));
        assertConvergence(f.iterations);
        // New top-level request resets the previous result, and advances once only.
        new SearchDriver(f).search(new SearchRequest(board, 1));
        assertEquals(2, generations[0]); assertEquals(1, f.iterations.size()); assertNull(f.iterations.getFirst().guess());
    }

    @Test void cancellationDuringEveryPassAndMaterializationPublishesOnlyPreviousDepth() {
        // Constant leaves give depth 2 exactly two zero passes plus materialization.
        for(var policy : new Policy[] {Policy.MTD_PREV, Policy.MTD_ORACLE, Policy.MTD_TWO_PASS, Policy.MTD_BRACKETED}) for(int abortAt : new int[] {2, 3, 4}) {
            var control = SearchControl.controlled(-1, System.nanoTime(), -1, TimeSource.SYSTEM);
            int[] invocation = {0};
            var f = new MtdResearchSearch(policy, new ExactEvaluator() {
                @Override public void initialize(long[] b) { if(++invocation[0] == abortAt) control.request(SearchTermination.STOPPED); }
                @Override public int evaluate(long[] b, int ply) { return 400; }
            }, new TTable(1), d -> 400);
            var published = new ArrayList<SearchResult>();
            var outcome = new SearchDriver(f).search(request(2, observer(published), control));
            assertTrue(outcome.iterationIncomplete()); assertFalse(outcome.targetDepthCompleted());
            assertEquals(1, published.size()); assertEquals(1, outcome.lastCompletedResult().depth());
            var last = f.iterations.getLast();
            assertFalse(last.result().completed()); assertEquals(Value.INVALID, last.result().score());
            assertFalse(last.result().hasMove()); assertEquals(0, last.result().principalVariationLength());
            assertEquals(abortAt, invocation[0]);
            assertEquals(last.attempts().stream().mapToLong(a -> a.result().nodes()).sum(), last.result().nodes());
        }
    }

    @Test void budgetsInsideAndExactlyBetweenPassesNeverPublishConvergedBoundAsExactResult() {
        var measured = new MtdResearchSearch(Policy.MTD_PREV, (b, ply) -> 400, new TTable(1), null);
        new SearchDriver(measured).search(new SearchRequest(Board.startingPosition(), 2));
        long base = measured.iterations.getFirst().result().nodes();
        var budgets = new ArrayList<Long>();
        for(var a : measured.iterations.getLast().attempts()) {
            budgets.add(base + 1); base += a.result().nodes();
            if(!a.phase().equals("materialize")) budgets.add(base);
        }
        for(long budget : budgets) {
            var f = new MtdResearchSearch(Policy.MTD_PREV, (b, ply) -> 400, new TTable(1), null);
            var published = new ArrayList<SearchResult>();
            var outcome = new SearchDriver(f).search(request(2, observer(published),
                    SearchControl.controlled(budget, System.nanoTime(), -1, TimeSource.SYSTEM)));
            assertEquals(budget, outcome.nodes()); assertTrue(outcome.iterationIncomplete());
            assertFalse(outcome.targetDepthCompleted()); assertEquals(1, published.size());
            assertEquals(1, outcome.lastCompletedResult().depth());
            assertFalse(f.iterations.getLast().result().completed());
        }
        var f = new MtdResearchSearch(Policy.MTD_PREV, (b, ply) -> 400, new TTable(1), null);
        var complete = new SearchDriver(f).search(request(2, SearchObserver.NONE,
                SearchControl.controlled(base, System.nanoTime(), -1, TimeSource.SYSTEM)));
        assertTrue(complete.targetDepthCompleted(), "Final admitted child may unwind exactly");
    }

    @Test void threadInterruptionAndPreCancelledRequestPublishNothing() {
        var f = new MtdResearchSearch(Policy.MTD_PREV, (b, ply) -> 400, new TTable(1), null);
        Thread.currentThread().interrupt();
        try {
            var r = new SearchDriver(f).search(new SearchRequest(Board.startingPosition(), 2));
            assertNull(r.lastCompletedResult()); assertFalse(r.targetDepthCompleted());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally { Thread.interrupted(); }
        var c = SearchControl.controlled(-1, System.nanoTime(), -1, TimeSource.SYSTEM);
        c.request(SearchTermination.STOPPED);
        assertNull(new SearchDriver(f).search(request(2, SearchObserver.NONE, c)).lastCompletedResult());
    }

    @Test void realRepetitionHistoryAndRule50ContextReachEveryPassUnchanged() {
        var board = Board.startingPosition();
        var history = GameHistory.builder(board);
        String[] cycle = {"g1f3", "g8f6", "f3g1", "f6g8"};
        for(int n = 0; n < 7; n++) {
            String coordinate = cycle[n % 4];
            long move = Arrays.stream(ExhaustiveOracle.legalMoves(board)).filter(m ->
                    com.ohinteractive.seedv6.core.move.Move.coordinate(m).equals(coordinate)).findFirst().orElseThrow();
            board = ExhaustiveOracle.child(board, move); history.appendPosition(board);
        }
        // The next reversible move can complete a real repetition. Compare each
        // depth using the actual supplied history, not a FEN-reset oracle.
        for(int fixture = 0; fixture < 2; fixture++) {
            if(fixture == 1) {
                board = Board.fromFen("4k3/8/8/8/8/8/P7/R3K3 w - - 99 1");
                history = GameHistory.builder(board);
            }
            var game = history.snapshot();
            int[] scores = new int[5];
            for(int d = 1; d <= 4; d++) scores[d] = new ExactSearch().search(board, game, d, ExactSearch.NEVER_CANCELLED).score();
            for(var policy : Policy.values()) {
                var f = new MtdResearchSearch(policy, (b, ply) -> Eval.evaluate(b), new TTable(1), d -> scores[d]);
                new SearchDriver(f).search(new SearchRequest(board, game, 4, SearchObserver.NONE, SearchControl.unlimited(), false));
                for(var i : f.iterations) {
                    assertEquals(scores[i.depth()], i.result().score());
                    AspirationResearch.verifyPv(board, game, i.result());
                }
                assertConvergence(f.iterations);
            }
        }
    }

    @Test void twoPassBudgetChoosesExactlyOneExactCompletionPathAndRetainsSameGeneration() {
        for(String name : new String[] {"start", "mate", "forced-loss"}) {
            var p = position(name);
            int[] generations = {0};
            var table = new TTable() {
                @Override public void advanceGeneration() { generations[0]++; super.advanceGeneration(); }
            };
            var f = new MtdResearchSearch(Policy.MTD_TWO_PASS, (b, ply) -> Eval.evaluate(b), table, null);
            var b = Board.fromFen(p.fen());
            new SearchDriver(f).search(new SearchRequest(b, 5));
            assertEquals(1, generations[0]);
            assertConvergence(f.iterations);
            for(var i : f.iterations.subList(1, f.iterations.size())) {
                assertEquals(3, i.attempts().size());
                assertEquals("zero", i.attempts().get(0).phase());
                assertEquals("zero", i.attempts().get(1).phase());
                var second = i.attempts().get(1);
                assertEquals(second.lower() == second.upper() ? "materialize" : "fallback", i.attempts().getLast().phase());
                assertEquals(new ExactSearch().search(b, i.depth()).score(), i.result().score());
                AspirationResearch.verifyPv(b, GameHistory.initial(b), i.result());
            }
            assertEquals(name.equals("mate") ? "materialize" : "fallback", f.iterations.get(1).attempts().getLast().phase());
        }
    }

    @Test void outwardAndMidpointArithmeticStayInsideProvenDomainIncludingMateBoundaries() {
        int m = ExactSearch.MATE_SCORE;
        for(int v = -m; v < m; v++) {
            assertEquals(v + 1, midpointBeta(v, v + 1));
            assertEquals(v + 1, outwardBeta(v, v + 1, 1, 1));
            assertEquals(v + 1, outwardBeta(v, v + 1, -1, 65536));
        }
        int[] edges = {-m, -32513, -32512, -32511, -1, 0, 1, 32511, 32512, 32513, m};
        for(int lower : edges) for(int upper : edges) if(lower < upper) {
            int beta = midpointBeta(lower, upper);
            assertTrue(lower < beta && beta <= upper);
            assertTrue(Math.abs((beta - lower) - (upper - beta)) <= 1);
            for(int distance = 1; distance <= 65536; distance *= 2) {
                int high = outwardBeta(lower, upper, 1, distance);
                int low = outwardBeta(lower, upper, -1, distance);
                assertTrue(lower < high && high <= upper && lower < low && low <= upper);
                assertEquals(Math.min(distance, upper - lower), high - lower);
                assertEquals(Math.min(distance, upper - lower), upper - low + 1);
            }
        }
        for(int invalid : new int[] {Value.INVALID, Integer.MAX_VALUE, -ExactSearch.INFINITY, ExactSearch.INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> midpointBeta(invalid, 0));
            assertThrows(IllegalArgumentException.class, () -> outwardBeta(0, invalid, 1, 1));
        }
        assertThrows(IllegalArgumentException.class, () -> midpointBeta(0, 0));
        assertThrows(IllegalArgumentException.class, () -> outwardBeta(-1, 1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> outwardBeta(-1, 1, 1, Integer.MAX_VALUE));
    }

    @Test void directionalSequenceUsesSearchProvenBoundsThenHalvesTheBracketInBothDirections() {
        boolean up = false, down = false, bisect = false, leap = false;
        for(String name : new String[] {"start", "endgame", "forced-loss", "mate"}) {
            var p = position(name);
            var c = MtdResearch.run(p, Policy.CONTROL, 5, null, false);
            var r = MtdResearch.run(p, Policy.MTD_BRACKETED, 5, c, false);
            MtdResearch.compare(c, r, name); assertConvergence(r.iterations());
            for(var i : r.iterations()) if(i.guess() != null) {
                var a = i.attempts();
                assertEquals("bracket", a.getFirst().phase());
                int direction = a.getFirst().classification().equals("fail-high") ? 1 : -1;
                up |= direction > 0; down |= direction < 0;
                int step = 1;
                boolean oppositeSeen = false;
                for(int n = 1; n < a.size() - 1; n++) {
                    var previous = a.get(n - 1); var current = a.get(n);
                    if(current.phase().equals("bisect")) {
                        assertTrue(oppositeSeen, "Domain extrema alone are not a discovered local bracket"); bisect = true;
                        assertEquals(midpointBeta(previous.lower(), previous.upper()), current.beta());
                        assertTrue(current.upper() - current.lower() <= (previous.upper() - previous.lower()) / 2);
                    } else {
                        assertFalse(oppositeSeen);
                        assertEquals(outwardBeta(previous.lower(), previous.upper(), direction, step), current.beta());
                        oppositeSeen = !current.classification().equals(a.getFirst().classification());
                        if(!oppositeSeen) step *= 2;
                    }
                    leap |= current.classification().equals("fail-high") ? current.result().score() > current.beta()
                            : current.result().score() < current.alpha();
                }
                // Full 65,537-score domain: one initial + <=17 outward + <=16
                // bisection probes, independent of evaluator score-step sizes.
                assertTrue(a.size() - 1 <= 34);
            }
        }
        assertTrue(up && down && bisect && leap);
    }

    @Test void newPoliciesAbortInsideEveryPhaseAndAtEveryInterPassBudgetBoundary() {
        for(var policy : new Policy[] {Policy.MTD_TWO_PASS, Policy.MTD_BRACKETED}) {
            var measured = MtdResearch.run(position("start"), policy, 5, null, false);
            var target = policy == Policy.MTD_TWO_PASS ? measured.iterations().get(1)
                    : measured.iterations().stream().filter(i -> i.attempts().stream()
                            .anyMatch(a -> a.phase().equals("bisect"))).findFirst().orElseThrow();
            int depth = target.depth();
            int priorInvocations = measured.iterations().stream().filter(i -> i.depth() < depth)
                    .mapToInt(i -> i.attempts().size()).sum();
            assertEquals(policy == Policy.MTD_TWO_PASS ? "fallback" : "materialize", target.attempts().getLast().phase());
            if(policy == Policy.MTD_BRACKETED) assertTrue(target.attempts().stream().anyMatch(a -> a.phase().equals("bisect")));
            // Cancel after a child has entered each invocation, including
            // outward bracketing, bisection, fallback and materialization.
            for(int n = 0; n < target.attempts().size(); n++) {
                int abortAt = priorInvocations + n + 1;
                int[] invocation = {0};
                var control = SearchControl.controlled(-1, System.nanoTime(), -1, TimeSource.SYSTEM);
                var f = new MtdResearchSearch(policy, new ExactEvaluator() {
                    @Override public void initialize(long[] b) { invocation[0]++; }
                    @Override public int evaluate(long[] b, int ply) { return Eval.evaluate(b); }
                    @Override public void child(long[] b, long[] child, int ply) {
                        if(invocation[0] == abortAt) control.request(SearchTermination.STOPPED);
                    }
                }, new TTable(), null);
                var published = new ArrayList<SearchResult>();
                var outcome = new SearchDriver(f).search(request(depth, observer(published), control));
                assertTrue(outcome.iterationIncomplete()); assertFalse(outcome.targetDepthCompleted());
                assertEquals(depth - 1, published.size()); assertEquals(depth - 1, outcome.lastCompletedResult().depth());
                assertEquals(target.attempts().get(n).phase(), f.iterations.getLast().attempts().getLast().phase());
                assertEquals(Value.INVALID, f.iterations.getLast().result().score());
                assertEquals(0, f.iterations.getLast().result().principalVariationLength());
            }
            long budget = measured.published().get(depth - 2).nodes();
            for(int n = 0; n < target.attempts().size(); n++) {
                budget += target.attempts().get(n).result().nodes();
                var f = new MtdResearchSearch(policy, (b, ply) -> Eval.evaluate(b), new TTable(), null);
                var outcome = new SearchDriver(f).search(request(depth, SearchObserver.NONE,
                        SearchControl.controlled(budget, System.nanoTime(), -1, TimeSource.SYSTEM)));
                boolean last = n == target.attempts().size() - 1;
                assertEquals(last, outcome.targetDepthCompleted());
                assertEquals(last ? depth : depth - 1, outcome.lastCompletedResult().depth());
                assertEquals(budget, outcome.nodes());
                assertEquals(!last, outcome.iterationIncomplete());
            }
        }
    }

    static AspirationResearch.Position position(String name) {
        return AspirationResearch.positions().stream().filter(p -> p.name().equals(name)).findFirst().orElseThrow();
    }

    static void assertConvergence(List<Iteration> iterations) {
        for(var i : iterations) {
            long nodes = 0; int lower = -ExactSearch.MATE_SCORE, upper = ExactSearch.MATE_SCORE;
            for(var a : i.attempts()) {
                nodes += a.result().nodes();
                if(zeroPhase(a.phase())) {
                    assertEquals(a.alpha() + 1, a.beta());
                    assertTrue(a.lower() >= lower && a.upper() <= upper && a.lower() <= a.upper());
                    assertTrue(a.lower() <= i.result().score() && a.upper() >= i.result().score());
                    assertTrue(a.lower() > lower || a.upper() < upper);
                    lower = a.lower(); upper = a.upper();
                } else if(a.phase().equals("materialize")) {
                    assertEquals(lower, upper); assertEquals(i.result().score(), lower);
                    assertEquals(lower - 1, a.alpha()); assertEquals(lower + 1, a.beta());
                    assertEquals("exact", a.classification());
                } else if(a.phase().equals("fallback")) {
                    assertTrue(lower < upper);
                    assertEquals(-ExactSearch.INFINITY, a.alpha()); assertEquals(ExactSearch.INFINITY, a.beta());
                    assertEquals("exact", a.classification());
                    assertTrue(lower <= a.result().score() && a.result().score() <= upper);
                }
            }
            assertEquals(nodes, i.result().nodes());
            if(i.guess() != null) assertTrue(Set.of("materialize", "fallback").contains(i.attempts().getLast().phase()));
        }
    }
    static SearchObserver observer(List<SearchResult> results) {
        return new SearchObserver() { @Override public void onIterationCompleted(IterationSnapshot s) { results.add(s.result()); } };
    }
    static SearchRequest request(int depth, SearchObserver observer, SearchControl control) {
        var b = Board.startingPosition(); return new SearchRequest(b, GameHistory.initial(b), depth, observer, control, false);
    }
}
