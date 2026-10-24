package com.ohinteractive.seedv6.search.exact;

import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.search.exact.Policy4ProductionTest.field;

@Timeout(180)
class Cp64ProductionTest {
    @Test void frozenResearchCompositionMatchesProductionAndWorkerStateIsPrivate() throws Exception {
        var engine = BaselineEngine.load();
        for (int workers = 1; workers <= 16; workers++) {
            for (int hash : new int[]{1, 64}) {
                var frozen = Cp64SelectivityLab.configuration("nominal-verified-null", workers, hash);
                assertEquals(frozen, BaselineEngine.configuration(engine.evaluation(), workers, hash));
                assertEquals(3407871, frozen.mechanisms().flags());
                assertEquals(262143, Cp64SelectivityLab.configuration("baseline", workers, hash).mechanisms().flags());
            }
            try (var production = engine.create(workers, 1); var candidate =
                    Cp64SelectivityLab.configuration("nominal-verified-null", workers, 1).create(engine.evaluation())) {
                var actual = kernels(production); var reference = kernels(candidate);
                var table = field(actual[0], "table");
                assertNotSame(table, field(reference[0], "table"));
                var mutable = Collections.newSetFromMap(new IdentityHashMap<>());
                for (var kernel : actual) {
                    assertSame(table, field(kernel, "table"));
                    assertEquals(field(reference[0], "construction"), field(kernel, "construction"));
                    for (String name : List.of("boards", "pv", "quietHistory", "constructionState", "constructionNullMoves", "evaluator"))
                        assertTrue(mutable.add(field(kernel, name)), name + " must be worker-private");
                }
            }
        }
        for (int processors : new int[]{1, 2, 8, 16, 32}) {
            int max = new SearchThreads(SearchThreads.MAX).resolve(processors);
            assertEquals(Math.min(processors, 16), max);
            assertEquals(3407871, BaselineEngine.configuration(max, 64).mechanisms().flags());
            assertEquals(16, new SearchThreads(16).resolve(processors));
        }
    }

    @Test void exactSingleWorkerTraversalMatchesFrozenCandidateWithRealHistoryAndHash() throws Exception {
        var engine = BaselineEngine.load();
        var roots = P3HistoryStudy.roots(Path.of("../docs/research/search/p3-construction-2026-10-20/diagnostic-corpus.tsv"));
        for (int hash : new int[]{1, 64}) try (var production = new SearchDriver(engine.create(1, hash));
                var candidate = new SearchDriver(Cp64SelectivityLab.configuration("nominal-verified-null", 1, hash).create(engine.evaluation()))) {
            for (var root : roots.subList(0, 4)) {
                for (int repeat = 0; repeat < 2; repeat++) {
                    var a = request(production, root.board(), root.history(), 8, 100000);
                    var b = request(candidate, root.board(), root.history(), 8, 100000);
                    ConstructionTtStudy.same(a, b);
                    assertNotNull(a.lastCompletedResult());
                    Phase2Oracle.legalPv(root.board(), a.lastCompletedResult().principalVariation());
                }
                production.newGame(); candidate.newGame();
            }
        }
    }

    @Test void productionAtEveryWorkerCountAccountsDrainsRestartsAndExercisesNull() throws Exception {
        var engine = BaselineEngine.load(); var board = Board.startingPosition(); var saved = board.clone();
        for (int workers = 1; workers <= 16; workers++) {
            var search = engine.create(workers, 1);
            try (var driver = new SearchDriver(search)) {
                long budget = Set.of(1, 2, 4, 8, 16).contains(workers) ? 100000 : 10000;
                long[] completedAttempts = {0};
                var observer = new SearchObserver() {
                    @Override public void onIterationCompleted(IterationSnapshot snapshot) {
                        try {
                            for (var kernel : kernels(search))
                                completedAttempts[0] += kernel.constructionCounters().get("nullAttempts");
                        } catch (Exception failure) { throw new AssertionError(failure); }
                    }
                };
                var outcome = driver.search(new SearchRequest(board, GameHistory.initial(board), 40, observer,
                        SearchControl.controlled(budget, System.nanoTime(), 60_000_000_000L, TimeSource.SYSTEM), true));
                assertEquals(budget, outcome.nodes(), "workers=" + workers);
                assertEquals(0, driver.activeSearchThreads());
                assertArrayEquals(saved, board);
                assertNotNull(outcome.lastCompletedResult());
                Phase2Oracle.legalPv(board, outcome.lastCompletedResult().principalVariation());
                for (var kernel : kernels(search)) {
                    for (String flag : List.of("nullProbeActive", "nullVerification", "constructionNullProbe", "constructionNullVerify"))
                        assertEquals(false, field(kernel, flag));
                    assertNull(field(kernel, "history"));
                }
                // Cancellation clears invocation counters; observe completed iterations before that reset.
                if (budget == 100000) assertTrue(completedAttempts[0] > 0, "Actual verified-null work at W" + workers);
                driver.newGame();
                assertTrue(driver.search(new SearchRequest(board, 3)).targetDepthCompleted());
                assertEquals(0, driver.activeSearchThreads());
            }
        }
    }

    @Test void terminalHistoryMateAndPvSemanticsSurviveAtRepresentativeWorkers() throws Exception {
        var engine = BaselineEngine.load();
        for (int workers : new int[]{1, 4, 16}) try (var driver = new SearchDriver(engine.create(workers, 1))) {
            String[] fens = {"7k/6Q1/5K2/8/8/8/8/8 b - - 100 1", "7k/5Q2/6K1/8/8/8/8/8 b - - 100 1",
                    "7k/8/5KQ1/8/8/8/8/8 w - - 0 1"};
            int[] scores = {-ExactSearch.MATE_SCORE, 0, ExactSearch.MATE_SCORE - 1};
            for (int i = 0; i < fens.length; i++) {
                var board = Board.fromFen(fens[i]);
                var result = driver.search(new SearchRequest(board, 4)).lastCompletedResult();
                assertEquals(scores[i], result.score());
                Phase2Oracle.legalPv(board, result.principalVariation());
            }
            var board = Board.startingPosition();
            var history = GameHistory.builder(board);
            for (int cycle = 0; cycle < 2; cycle++) {
                for (String coordinate : List.of("g1f3", "g8f6", "f3g1", "f6g8")) {
                    long move = Arrays.stream(ExhaustiveOracle.legalMoves(board))
                            .filter(m -> com.ohinteractive.seedv6.core.move.Move.coordinate(m).equals(coordinate)).findFirst().orElseThrow();
                    board = ExhaustiveOracle.child(board, move); history.appendPosition(board);
                }
                if (cycle == 0) assertFalse(driver.search(new SearchRequest(board, history.snapshot(), 3)).terminalRoot());
            }
            var draw = driver.search(new SearchRequest(board, history.snapshot(), 3));
            assertTrue(draw.terminalRoot()); assertEquals(0, draw.lastCompletedResult().score());
            assertEquals(0, driver.activeSearchThreads());
        }
    }

    private static SearchDriverOutcome request(SearchDriver driver, long[] board, GameHistory history, int depth, long nodes) {
        return driver.search(new SearchRequest(board, history, depth, SearchObserver.NONE,
                SearchControl.controlled(nodes, System.nanoTime(), 60_000_000_000L, TimeSource.SYSTEM), true));
    }
    private static ExactSearch[] kernels(SingleDepthSearch search) throws Exception {
        if (search instanceof com.ohinteractive.seedv6.search.manage.Boundary2ClockPolicy)
            search = (SingleDepthSearch) field(search, "owner");
        var owner = field(search, "owner");
        if (owner instanceof ExactSearchAdapter) return new ExactSearch[]{(ExactSearch)field(owner, "exact")};
        var workers = (Object[])field(owner, "workers");
        var result = new ExactSearch[workers.length];
        for (int i = 0; i < result.length; i++) result[i] = (ExactSearch)field(workers[i], "search");
        return result;
    }
}
