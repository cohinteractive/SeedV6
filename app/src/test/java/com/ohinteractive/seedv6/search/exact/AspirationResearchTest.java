package com.ohinteractive.seedv6.search.exact;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.SearchDriver;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;
import com.ohinteractive.seedv6.tools.search.ExactSearchHarness;

@Timeout(60)
class AspirationResearchTest {
    @Test void controlIsTheProductionPathAndAllPoliciesMatchBoundedTtOffOracle() {
        for(var p : ExactSearchHarness.orderingPositions()) {
            var b = Board.fromFen(p.fen());
            var expected = new ArrayList<SearchResult>();
            new SearchDriver().search(new SearchRequest(b, 4, observer(expected)));
            for(int width : new int[] {0, 512, 628, AspirationResearchSearch.ADAPT_172_628}) {
                var previous = new ArrayList<SearchResult>();
                for(int repeat = 0; repeat < 2; repeat++) {
                    var results = new ArrayList<SearchResult>();
                    var facility = new AspirationResearchSearch(width, (board, ply) -> Eval.evaluate(board), new TTable());
                    var outcome = new SearchDriver(facility).search(new SearchRequest(b, 4, observer(results)));
                    assertTrue(outcome.targetDepthCompleted());
                    for(int i = 0; i < results.size(); i++) {
                        var r = results.get(i);
                        assertEquals(expected.get(i).score(), r.score());
                        if(width == 0) assertEquals(expected.get(i), r);
                        if(repeat > 0) assertEquals(previous.get(i), r);
                        int oracle = new ExactSearch().search(b, i + 1).score();
                        assertEquals(oracle, r.score());
                        AspirationResearch.verifyPv(b, GameHistory.initial(b), r);
                    }
                    previous = results;
                }
            }
        }
    }

    @Test void exactSuccessAndBothFailureDirectionsPublishOnlyExactIterationsInOneGeneration() {
        // +/-width/2 also exercise equality at the original beta/alpha.
        for(int width : new int[] {512, 628}) for(int value : new int[] {0, 400, -400, width / 2, -width / 2}) {
            var board = Board.startingPosition();
            long key = SearchKey.key(board, SearchKey.rootHistory(board, GameHistory.initial(board)));
            var saves = new ArrayList<Integer>();
            int[] counts = new int[2];
            int[] invocation = {0}, retryBoundProbes = {0};
            var table = new TTable(1) {
                @Override public void advanceGeneration() { counts[0]++; super.advanceGeneration(); }
                @Override public void clear() { counts[1]++; super.clear(); }
                @Override public void save(long k, int d, int type, int score, long move) {
                    if(k == key && d == 2) saves.add(type);
                    super.save(k, d, type, score, move);
                }
                @Override public boolean probe(long k, TEntry entry) {
                    boolean hit = super.probe(k, entry);
                    if(value != 0 && invocation[0] == 3 && k == key && hit) {
                        assertEquals(2, entry.data & 255);
                        assertEquals(1, (entry.data >>> 10) & 255);
                        assertEquals(value > 0 ? TYPE_LOWER : TYPE_UPPER, (entry.data >>> 8) & 3);
                        retryBoundProbes[0]++;
                    }
                    return hit;
                }
            };
            var facility = new AspirationResearchSearch(width, new ExactEvaluator() {
                @Override public void initialize(long[] b) { invocation[0]++; }
                @Override public int evaluate(long[] b, int ply) { return value; }
            }, table);
            var published = new ArrayList<SearchResult>();
            var outcome = new SearchDriver(facility).search(new SearchRequest(board, 3, observer(published)));
            assertTrue(outcome.targetDepthCompleted());
            assertEquals(List.of(1, 2, 3), published.stream().map(SearchResult::depth).toList());
            assertEquals(List.of(-value, value, -value), published.stream().map(SearchResult::score).toList());
            var two = facility.iterations.get(1);
            assertEquals(value == 0 ? "exact" : value > 0 ? "fail-high" : "fail-low", two.narrowResult());
            assertEquals(value == 0, two.retry() == null);
            if(value != 0) {
                assertTrue(saves.contains(value > 0 ? TTable.TYPE_LOWER : TTable.TYPE_UPPER));
                assertEquals(TTable.TYPE_EXACT, saves.getLast());
                assertEquals(two.initial().result().nodes() + two.retry().result().nodes(), two.result().nodes());
                assertTrue(retryBoundProbes[0] > 0, "Retry sees retained same-depth/current-generation root bound");
            }
            assertEquals(1, counts[0]); assertEquals(0, counts[1]);
            var entry = new TTable.TEntry(); assertTrue(table.probe(key, entry));
            assertEquals(1, (entry.data >>> 10) & 255);
        }
    }

    @Test void boundaryEndpointsIncludeEveryOrdinaryScoreAndExcludeEveryMate() {
        int max = TranspositionScores.MAX_NORMAL_SCORE;
        for(int width : new int[] {172, 512, 628}) for(int center : new int[] {-max, -max + 1, 0, max - 1, max}) {
            int a = AspirationResearchSearch.alpha(center, width), b = AspirationResearchSearch.beta(center, width);
            assertTrue(a < center && center < b);
            assertTrue(a >= -TranspositionScores.MATE_THRESHOLD && b <= TranspositionScores.MATE_THRESHOLD);
            if(center == max) assertEquals(max + 1, b);
            if(center == -max) assertEquals(-max - 1, a);
            var root = Board.startingPosition();
            var search = new ExactSearch((board, ply) -> -center, new TTable(1));
            assertEquals(center, search.searchWindow(root, GameHistory.initial(root), 1, a, b, ExactSearch.NEVER_CANCELLED).score());
        }
    }

    @Test void depthOneNoPreviousAndPreviousMateBypassAndNewMateRetries() {
        for(int width : new int[] {512, 628, AspirationResearchSearch.ADAPT_172_628}) {
            var b = Board.fromFen(ExactSearchHarness.positions().stream().filter(p -> p.name().equals("mate")).findFirst().orElseThrow().fen());
            var facility = new AspirationResearchSearch(width, (board, ply) -> Eval.evaluate(board), new TTable(1));
            new SearchDriver(facility).search(new SearchRequest(b, 4));
            assertEquals("depth-one", facility.iterations.getFirst().eligibility());
            assertTrue(facility.iterations.subList(1, 4).stream().allMatch(i -> i.eligibility().equals("previous-mate") && i.retry() == null));
            facility.beginRequest();
            try {
                facility.search(new SearchRequest(Board.startingPosition(), 2));
                assertEquals("no-previous", facility.iterations.getFirst().eligibility());
            } finally { facility.endRequest(); }
            b = Board.fromFen(AspirationResearch.FORCED_LOSS);
            var published = new ArrayList<SearchResult>();
            new SearchDriver(facility).search(new SearchRequest(b, 5, observer(published)));
            var discovery = facility.iterations.stream().filter(i -> i.retry() != null && TranspositionScores.isMateScore(i.result().score())).findFirst().orElseThrow();
            assertEquals("fail-low", discovery.narrowResult());
            assertEquals(-ExactSearch.MATE_SCORE + 4, discovery.result().score());
            assertEquals(5, published.size());
        }
    }

    @Test void cancellationInNarrowOrRetryAndBetweenAttemptsRetainsEarlierCompletion() {
        for(int width : new int[] {512, 628, AspirationResearchSearch.ADAPT_172_628}) for(int abortInvocation : new int[] {2, 3}) {
            var control = SearchControl.controlled(-1, System.nanoTime(), -1, TimeSource.SYSTEM);
            int[] initialized = {0};
            var evaluator = new ExactEvaluator() {
                @Override public void initialize(long[] b) { initialized[0]++; }
                @Override public int evaluate(long[] b, int ply) {
                    if(initialized[0] == abortInvocation) control.request(SearchTermination.STOPPED);
                    return 400;
                }
            };
            var facility = new AspirationResearchSearch(width, evaluator, new TTable(1));
            var published = new ArrayList<SearchResult>();
            var outcome = new SearchDriver(facility).search(request(3, observer(published), control));
            assertFalse(outcome.targetDepthCompleted()); assertTrue(outcome.iterationIncomplete());
            assertEquals(1, outcome.lastCompletedResult().depth()); assertEquals(1, published.size());
            var two = facility.iterations.get(1);
            assertFalse(two.result().completed()); assertEquals(0, two.result().principalVariationLength());
            assertEquals(abortInvocation == 3, two.retry() != null);
            assertEquals(abortInvocation, initialized[0]);
        }
        var measured = new AspirationResearchSearch(512, (b, ply) -> 400, new TTable(1));
        new SearchDriver(measured).search(new SearchRequest(Board.startingPosition(), 2));
        long budget = measured.iterations.get(0).result().nodes() + measured.iterations.get(1).initial().result().nodes();
        var facility = new AspirationResearchSearch(512, (b, ply) -> 400, new TTable(1));
        var result = new SearchDriver(facility).search(request(2, SearchObserver.NONE,
                SearchControl.controlled(budget, System.nanoTime(), -1, TimeSource.SYSTEM)));
        assertEquals(1, result.lastCompletedResult().depth());
        assertTrue(result.iterationIncomplete()); assertNull(facility.iterations.get(1).retry());
        assertEquals(budget, result.nodes());
    }

    @Test void adaptiveThresholdIncludes72AndExcludesMateArithmetic() {
        for(int movement : new int[] {-73, -72, -1, 0, 1, 72, 73})
            assertEquals(Math.abs(movement) <= 72 ? 172 : 628,
                    AspirationResearchSearch.adaptiveWidth(100, 100 + movement));
        assertEquals(628, AspirationResearchSearch.adaptiveWidth(null, 0));
        for(int mate : new int[] {-TranspositionScores.MATE_SCORE, -TranspositionScores.MATE_THRESHOLD,
                TranspositionScores.MATE_THRESHOLD, TranspositionScores.MATE_SCORE}) {
            assertEquals(0, AspirationResearchSearch.adaptiveWidth(null, mate));
            assertEquals(0, AspirationResearchSearch.adaptiveWidth(0, mate));
            assertEquals(0, AspirationResearchSearch.adaptiveWidth(mate, 0));
            assertEquals(0, AspirationResearchSearch.adaptiveWidth(mate, mate));
        }
        for(int value : new int[] {0, 36, 37}) {
            var facility = new AspirationResearchSearch(AspirationResearchSearch.ADAPT_172_628,
                    (b, ply) -> value, new TTable(1));
            // Reusing the owner must not leak either completed score into a new request.
            for(int request = 0; request < 2; request++) {
                new SearchDriver(facility).search(new SearchRequest(Board.startingPosition(), 3));
                var one = facility.iterations.get(0);
                var two = facility.iterations.get(1);
                var three = facility.iterations.get(2);
                assertNull(one.previous()); assertNull(one.earlier());
                assertEquals("insufficient-history", two.selectedClass());
                assertEquals(628, two.halfWidth()); assertNull(two.previousMovement());
                assertEquals(-value, three.earlier()); assertEquals(value, three.previous());
                assertEquals(2 * value, three.previousMovement());
                assertEquals(value <= 36 ? "stable" : "volatile", three.selectedClass());
                assertEquals(value <= 36 ? 172 : 628, three.halfWidth());
            }
        }
    }

    @Test void stableHitsAndFailuresPreserveBoundMeaningGenerationAndExactPublication() {
        String[] names = {"start", "endgame", "en-passant"};
        int[] depths = {3, 3, 4};
        String[] expected = {"exact", "fail-high", "fail-low"};
        for(int index = 0; index < names.length; index++) {
            var p = position(names[index]);
            var board = Board.fromFen(p.fen());
            int target = depths[index];
            long key = SearchKey.key(board, SearchKey.rootHistory(board, GameHistory.initial(board)));
            int[] generations = {0}, invocations = {0}, retryBounds = {0};
            var table = new TTable() {
                @Override public void advanceGeneration() { generations[0]++; super.advanceGeneration(); }
                @Override public boolean probe(long k, TEntry entry) {
                    boolean hit = super.probe(k, entry);
                    if(k == key && hit && invocations[0] == target + 1) {
                        assertEquals(target, entry.data & 255);
                        assertEquals(1, (entry.data >>> 10) & 255);
                        assertEquals(target == 3 ? TYPE_LOWER : TYPE_UPPER, (entry.data >>> 8) & 3);
                        retryBounds[0]++;
                    }
                    return hit;
                }
            };
            var facility = new AspirationResearchSearch(AspirationResearchSearch.ADAPT_172_628, new ExactEvaluator() {
                @Override public void initialize(long[] b) { invocations[0]++; }
                @Override public int evaluate(long[] b, int ply) { return Eval.evaluate(b); }
            }, table);
            var published = new ArrayList<SearchResult>();
            var outcome = new SearchDriver(facility).search(new SearchRequest(board, target, observer(published)));
            assertTrue(outcome.targetDepthCompleted()); assertEquals(target, published.size());
            var last = facility.iterations.getLast();
            assertEquals("stable", last.selectedClass()); assertEquals(172, last.halfWidth());
            assertEquals(expected[index], last.narrowResult());
            assertEquals(index != 0, last.retry() != null);
            assertEquals(1, generations[0]);
            if(index != 0) {
                assertTrue(retryBounds[0] > 0);
                assertEquals(-ExactSearch.INFINITY, last.retry().alpha());
                assertEquals(ExactSearch.INFINITY, last.retry().beta());
                assertEquals(last.initial().result().nodes() + last.retry().result().nodes(), last.result().nodes());
            }
            for(var r : published) {
                assertEquals(new ExactSearch().search(board, r.depth()).score(), r.score());
                AspirationResearch.verifyPv(board, GameHistory.initial(board), r);
            }
        }
    }

    @Test void adaptiveStableCancellationAndExhaustionNeverPublishFailedDepth() {
        var board = Board.fromFen(position("endgame").fen()); // Stable depth 3 fails high.
        for(int abortInvocation : new int[] {3, 4}) {
            var control = SearchControl.controlled(-1, System.nanoTime(), -1, TimeSource.SYSTEM);
            int[] invocation = {0};
            var facility = new AspirationResearchSearch(AspirationResearchSearch.ADAPT_172_628, new ExactEvaluator() {
                @Override public void initialize(long[] b) { invocation[0]++; }
                @Override public int evaluate(long[] b, int ply) {
                    if(invocation[0] == abortInvocation) control.request(SearchTermination.STOPPED);
                    return Eval.evaluate(b);
                }
            }, new TTable());
            var published = new ArrayList<SearchResult>();
            var outcome = new SearchDriver(facility).search(new SearchRequest(board, GameHistory.initial(board),
                    3, observer(published), control, false));
            assertFalse(outcome.targetDepthCompleted()); assertTrue(outcome.iterationIncomplete());
            assertEquals(2, outcome.lastCompletedResult().depth()); assertEquals(2, published.size());
            var last = facility.iterations.getLast();
            assertEquals("stable", last.selectedClass()); assertFalse(last.result().completed());
            assertEquals(0, last.result().principalVariationLength());
            assertEquals(abortInvocation == 4, last.retry() != null);
        }
        var measured = AspirationResearch.run(position("endgame"), AspirationResearchSearch.ADAPT_172_628, 3);
        long previousNodes = measured.published().get(1).nodes();
        long narrowNodes = measured.iterations().getLast().initial().result().nodes();
        // Exhaust inside the narrow attempt, precisely before retry, and inside retry.
        for(long budget : new long[] {previousNodes + 1, previousNodes + narrowNodes, previousNodes + narrowNodes + 1}) {
            var facility = new AspirationResearchSearch(AspirationResearchSearch.ADAPT_172_628,
                    (b, ply) -> Eval.evaluate(b), new TTable());
            var published = new ArrayList<SearchResult>();
            var outcome = new SearchDriver(facility).search(new SearchRequest(board, GameHistory.initial(board), 3,
                    observer(published), SearchControl.controlled(budget, System.nanoTime(), -1, TimeSource.SYSTEM), false));
            assertEquals(budget, outcome.nodes()); assertTrue(outcome.iterationIncomplete());
            assertFalse(outcome.targetDepthCompleted()); assertEquals(2, outcome.lastCompletedResult().depth());
            assertEquals(2, published.size());
            var last = facility.iterations.getLast();
            assertFalse(last.result().completed()); assertEquals(0, last.result().principalVariationLength());
            assertEquals(budget > previousNodes + narrowNodes, last.retry() != null);
        }
    }

    @Test void fixed628RetainsSr006BMeasuredIterationTrees() {
        // Frozen SR-006B measurements with default TT, not candidate-derived expectations.
        String[] names = {"start", "endgame", "en-passant"};
        long[][] nodes = {{20, 60, 634, 2596}, {14, 29, 279, 756}, {7, 17, 71, 248}};
        int[][] scores = {{42, 0, 42, -4}, {142, 165, 477, 14}, {434, 412, 454, 41}};
        for(int p = 0; p < names.length; p++) {
            var run = AspirationResearch.run(position(names[p]), 628, 4);
            for(int i = 0; i < 4; i++) {
                var iteration = run.iterations().get(i);
                assertEquals(nodes[p][i], iteration.result().nodes());
                assertEquals(scores[p][i], iteration.result().score());
                assertEquals(i == 0 ? "full" : "exact", iteration.narrowResult());
                assertNull(iteration.retry());
            }
        }
    }

    static AspirationResearch.Position position(String name) {
        return AspirationResearch.positions().stream().filter(p -> p.name().equals(name)).findFirst().orElseThrow();
    }

    static SearchObserver observer(List<SearchResult> results) {
        return new SearchObserver() { @Override public void onIterationCompleted(IterationSnapshot s) { results.add(s.result()); } };
    }
    static SearchRequest request(int depth, SearchObserver observer, SearchControl control) {
        var b = Board.startingPosition();
        return new SearchRequest(b, GameHistory.initial(b), depth, observer, control, false);
    }
}
