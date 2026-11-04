package com.ohinteractive.seedv6.search.exact;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.manage.ClockPolicy;
import com.ohinteractive.seedv6.search.tt.TTable;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(90)
class P4WholeRootTest {
    @Test void narrowRaceReportsBoundsForAspirationWithoutInventingExactResults() {
        var evaluation = SearchEvaluation.handcrafted(); var board = Board.startingPosition();
        var history = GameHistory.initial(board);
        int expected = ExactSearch.quiescenceResearch(ExactEvaluator.from(evaluation)).search(board, history, 3, () -> false).score();
        try (var owner = ParallelSearch.constructionWholeRootResearch(4, evaluation, new TTable(1), FlatSearchConfig.exact())) {
            for (int[] window : new int[][]{{expected - 1, expected + 1}, {expected + 50, expected + 51}, {expected - 51, expected - 50}}) {
                var r = owner.searchWindow(new SearchRequest(board, history, 3), window[0], window[1]);
                assertTrue(r.completed()); assertEquals(3, r.depth()); assertEquals(0, owner.activeSearchThreads());
                if (expected <= window[0]) assertTrue(r.score() <= window[0]);
                else if (expected >= window[1]) assertTrue(r.score() >= window[1]);
                else assertEquals(expected, r.score());
                Phase2Oracle.legalPv(board, r.principalVariation());
            }
        }
    }

    @Test void completeExactRaceAgreesWithIndependentTreeAndPreservesBoard() {
        var evaluation = SearchEvaluation.handcrafted();
        for (String fen : List.of("4k3/8/8/3pP3/8/8/8/4K3 w - d6 98 1",
                "8/k1P5/2K5/8/8/8/8/8 w - - 98 1", "7k/6Q1/5K2/8/8/8/8/8 b - - 100 1")) {
            var board = Board.fromFen(fen); var saved = board.clone(); var history = GameHistory.initial(board);
            int expected = ExactSearch.quiescenceResearch(ExactEvaluator.from(evaluation)).search(board, history, 3, () -> false).score();
            for (int workers : new int[]{2, 4}) try (var driver = new SearchDriver(
                    ParallelSearch.constructionWholeRootResearch(workers, evaluation, new TTable(1), FlatSearchConfig.exact()))) {
                var result = driver.search(new SearchRequest(board, history, 3));
                assertTrue(result.targetDepthCompleted()); assertEquals(expected, result.lastCompletedResult().score());
                Phase2Oracle.legalPv(board, result.lastCompletedResult().principalVariation());
                assertArrayEquals(saved, board); assertEquals(0, driver.activeSearchThreads());
            }
        }
    }

    @Test void tcP1RaceAccountsAbortedPeersAndReusesAfterFailureOrStop() throws Exception {
        var model = ConventionalNnueEngine.load(); var board = Board.startingPosition(); var saved = board.clone();
        var config = BaselineEngine.configuration(4, 1, BaselineEngine.SearchProfile.TC_P1).mechanisms();
        for (int workers : new int[]{2, 4, 8, 16}) {
            var counts = new P4MultiworkerStudy.Admissions[workers];
            var owner = ParallelSearch.constructionWholeRootResearch(workers, model.evaluation(), new TTable(1), config,
                    i -> counts[i] = new P4MultiworkerStudy.Admissions(ExactEvaluator.from(model.evaluation())));
            try (var driver = new SearchDriver(ClockPolicy.BOUNDARY2.apply(new AspirationSearch(owner, AspirationSearch.Policy.ADAPTIVE16)))) {
                for (int cap : new int[]{1, 100, 5000}) {
                    for (var a : counts) a.reset();
                    var control = SearchControl.controlled(cap, System.nanoTime(), 10_000_000_000L, TimeSource.SYSTEM);
                    var result = driver.search(new SearchRequest(board, GameHistory.initial(board), 20, SearchObserver.NONE, control, true));
                    assertEquals(cap, result.nodes()); assertEquals(cap, control.nodes());
                    assertEquals(cap, Arrays.stream(counts).mapToLong(a -> a.nodes).sum());
                    assertEquals(cap, result.diagnostics().totalEnteredNodes());
                    assertEquals(0, owner.activeSearchThreads()); assertArrayEquals(saved, board);
                    if (result.lastCompletedResult() != null) Phase2Oracle.legalPv(board, result.lastCompletedResult().principalVariation());
                }
                var injected = new AtomicBoolean();
                TimeSource broken = () -> {
                    if (Thread.currentThread().getName().equals("seedv6-search-helper") && injected.compareAndSet(false, true))
                        throw new IllegalStateException("MW whole-root injected failure");
                    return 0;
                };
                assertThrows(IllegalStateException.class, () -> driver.search(new SearchRequest(board, GameHistory.initial(board), 8,
                        SearchControl.controlled(-1, 0, Long.MAX_VALUE, broken))));
                assertTrue(injected.get()); assertEquals(0, owner.activeSearchThreads());
                driver.newGame();
                var stopped = SearchControl.controlled(-1, 0, -1, TimeSource.SYSTEM); stopped.request(SearchTermination.STOPPED);
                assertEquals(0, driver.search(new SearchRequest(board, GameHistory.initial(board), 8, stopped)).nodes());
                assertTrue(driver.search(new SearchRequest(board, 3)).targetDepthCompleted());
            }
        }
    }
}
