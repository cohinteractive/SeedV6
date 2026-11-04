package com.ohinteractive.seedv6.search.exact;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.manage.ClockPolicy;
import com.ohinteractive.seedv6.search.tt.TTable;
import static org.junit.jupiter.api.Assertions.*;

/** MW-R001 bounded correctness tests, not competitive or scaling evidence. */
@Timeout(60)
class P4MultiworkerTest {
    @Test void exactTcP1LimitsRestoreAndReuseAcrossWorkerDomains() throws Exception {
        var model = ConventionalNnueEngine.load();
        var board = Board.startingPosition(); var saved = board.clone();
        for (int workers : new int[]{1, 2, 4, 8, 16}) {
            var config = BaselineEngine.configuration(workers, 1, BaselineEngine.SearchProfile.TC_P1);
            assertEquals(1177812991L, config.mechanisms().flags());
            try (var driver = new SearchDriver(ClockPolicy.BOUNDARY2.apply(config.create(model.evaluation())))) {
                for (int cap : new int[]{1, 100, 5000}) {
                    var control = SearchControl.controlled(cap, System.nanoTime(), 10_000_000_000L, TimeSource.SYSTEM);
                    var result = driver.search(new SearchRequest(board, GameHistory.initial(board), 20,
                            SearchObserver.NONE, control, true));
                    assertEquals(SearchTermination.NODE_LIMIT, control.termination());
                    assertEquals(cap, result.nodes());
                    assertEquals(cap, result.diagnostics().totalEnteredNodes());
                    assertFalse(result.targetDepthCompleted());
                    assertEquals(0, driver.activeSearchThreads());
                    assertArrayEquals(saved, board);
                    if (result.lastCompletedResult() != null)
                        Phase2Oracle.legalPv(board, result.lastCompletedResult().principalVariation());
                    driver.newGame();
                }
                var stopped = SearchControl.controlled(-1, 0, -1, TimeSource.SYSTEM);
                stopped.request(SearchTermination.STOPPED);
                assertEquals(0, driver.search(new SearchRequest(board, GameHistory.initial(board), 20, stopped)).nodes());
                assertTrue(driver.search(new SearchRequest(board, 3)).targetDepthCompleted());
            }
        }
    }

    @Test void observedTcP1HelperFailureDrainsThenReusesPrivateState() throws Exception {
        var model = ConventionalNnueEngine.load();
        var board = Board.startingPosition(); var saved = board.clone();
        for (int workers : new int[]{2, 4}) {
            var armed = new AtomicBoolean(true); var injected = new AtomicBoolean();
            var config = BaselineEngine.configuration(workers, 1, BaselineEngine.SearchProfile.TC_P1).mechanisms();
            var admissions = new P4MultiworkerStudy.Admissions[workers];
            var owner = ParallelSearch.constructionResearch(workers, model.evaluation(), new TTable(1), config, i -> {
                var delegate = ExactEvaluator.from(model.evaluation());
                return admissions[i] = new P4MultiworkerStudy.Admissions(new ExactEvaluator() {
                    public void initialize(long[] b) {
                        if (i > 0 && armed.compareAndSet(true, false)) {
                            injected.set(true); throw new IllegalStateException("MW injected helper failure");
                        }
                        delegate.initialize(b);
                    }
                    public void child(long[] p, long[] c, int ply) { delegate.child(p, c, ply); }
                    public void child(long[] p, long[] c, int ply, int qply) { delegate.child(p, c, ply, qply); }
                    public void reenter(int ply) { delegate.reenter(ply); }
                    public int evaluate(long[] b, int ply) { return delegate.evaluate(b, ply); }
                });
            });
            try (var driver = new SearchDriver(ClockPolicy.BOUNDARY2.apply(
                    new AspirationSearch(owner, AspirationSearch.Policy.ADAPTIVE16)))) {
                assertThrows(IllegalStateException.class, () -> driver.search(new SearchRequest(board, 6)));
                assertTrue(injected.get()); assertEquals(0, owner.activeSearchThreads());
                assertArrayEquals(saved, board); driver.newGame();
                for (var a : admissions) a.reset();
                var result = driver.search(new SearchRequest(board, 4));
                assertTrue(result.targetDepthCompleted());
                assertEquals(result.nodes(), java.util.Arrays.stream(admissions).mapToLong(a -> a.nodes).sum());
                Phase2Oracle.legalPv(board, result.lastCompletedResult().principalVariation());
                assertArrayEquals(saved, board); assertEquals(0, owner.activeSearchThreads());
            }
        }
        assertEquals(0, Thread.getAllStackTraces().keySet().stream()
                .filter(t -> t.isAlive() && t.getName().equals("seedv6-search-helper")).count());
    }
}
