package com.ohinteractive.seedv6.search.driver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.exact.ConstructionSearch;
import com.ohinteractive.seedv6.search.exact.FlatSearchConfig;
import com.ohinteractive.seedv6.search.manage.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class BaselineEngineTest {
    @Test void unrelatedConventionalPayloadDoesNotInheritCp64Selectivity() throws Exception {
        byte[] bytes;
        try (var input = getClass().getResourceAsStream(ConventionalNnueEngine.NETWORK)) {
            bytes = input.readAllBytes();
        }
        // In-memory valid alternate weights, never a mutation of the frozen asset.
        bytes[bytes.length - 1] ^= 1;
        var payload = java.util.Arrays.copyOfRange(bytes,
                com.ohinteractive.seedv6.core.nnue.conventional.ConventionalNetwork.HEADER_BYTES, bytes.length);
        System.arraycopy(java.security.MessageDigest.getInstance("SHA-256").digest(payload), 0, bytes, 40, 32);
        var model = com.ohinteractive.seedv6.core.nnue.conventional.ConventionalNetwork.read(new java.io.ByteArrayInputStream(bytes));
        var evaluation = SearchEvaluation.conventional(model);
        for (int workers = 1; workers <= 16; workers++)
            assertEquals(262143, BaselineEngine.configuration(evaluation, workers, 1).mechanisms().flags());
        try (var driver = new SearchDriver(BaselineEngine.create(evaluation, 4, 1))) {
            assertTrue(driver.search(new SearchRequest(Board.startingPosition(), 3)).targetDepthCompleted());
        }
    }
    @Test void frozenCompositionAndAllCompetitiveEvaluatorsUseSameSettings() throws Exception {
        for (var choice : BaselineEngine.Evaluator.values()) {
            var engine = BaselineEngine.load(choice);
            long flags = choice == BaselineEngine.Evaluator.CONVENTIONAL_CP64 ? 3407871 : 262143;
            assertEquals(flags, BaselineEngine.configuration(engine.evaluation(), 4, 8).mechanisms().flags());
            assertTrue(engine.identity(4, 8).contains("flags=" + flags));
            assertTrue(engine.identity(4, 8).contains("aspiration=ADAPTIVE16;parallel=ROOT_REUSE"));
            assertTrue(engine.identity(4, 8).contains("workers=4;hashMiB=8"));
            if (choice == BaselineEngine.Evaluator.CONVENTIONAL_CP64)
                assertTrue(engine.identity(4, 8).contains("scoreMapping=BT4_Q_LOGIT400_V1"));
            try (var service = engine.service(4, 8)) {
                service.setBookEnabled(false);
                var result = run(service, Board.startingPosition(), new SearchLimits(4, -1, -1, false));
                assertNull(result.failure()); assertTrue(result.hasMove());
            }
        }
    }
    @Test void incrementalMatchesIndependentRebuildThroughFullConstruction() throws Exception {
        var engine = BaselineEngine.load();
        for (String fen : new String[]{Board.FEN_STARTING_POSITION,
                "r3k2r/pppq1ppp/2n1bn2/3pp3/3PP3/2N1BN2/PPPQ1PPP/R3K2R w KQkq - 0 1",
                "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1", "7k/P7/8/8/8/8/8/7K w - - 0 1"}) {
            var board = Board.fromFen(fen);
            try (var actual = engine.service(1, 8);
                 var oracle = new SearchLifecycleService(TimeSource.SYSTEM,
                         () -> BaselineEngine.create(SearchEvaluation.conventionalFullRebuild(engine.conventional().model()), 1, 8),
                         com.ohinteractive.seedv6.search.tablebase.RootTablebase.NONE, com.ohinteractive.seedv6.book.Book.EMPTY)) {
                actual.setBookEnabled(false);
                var a = run(actual, board, new SearchLimits(5, -1, -1, false)).lastCompletedResult();
                var b = run(oracle, board, new SearchLimits(5, -1, -1, false)).lastCompletedResult();
                assertNotNull(a); assertNotNull(b);
                assertEquals(b.score(), a.score(), fen); assertEquals(b.bestMove(), a.bestMove(), fen);
                assertEquals(b.nodes(), a.nodes(), fen); assertArrayEquals(b.principalVariation(), a.principalVariation(), fen);
            }
        }
    }
    @Test void multiworkerReuseStopAndClose() throws Exception {
        var engine = BaselineEngine.load();
        for (int workers : new int[]{1, 2, 4, 16}) {
            var service = engine.service(workers, 8);
            try {
                var driver = field(field(service, "search"), "exact");
                var owner = field(driver, "owner");
                if (workers > 1) {
                    var allocated = (Object[]) field(owner, "workers");
                    assertEquals(workers, allocated.length);
                    var privateSearches = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
                    for (Object worker : allocated) assertTrue(privateSearches.add(field(worker, "search")));
                }
                service.setBookEnabled(false);
                for (int game = 0; game < 3; game++) {
                    service.invalidate(SearchTermination.NEW_GAME);
                    var board = Board.startingPosition(); var before = board.clone();
                    var result = run(service, board, new SearchLimits(100, -1, 100, false));
                    assertNull(result.failure()); assertTrue(result.hasMove()); assertArrayEquals(before, board);
                }
                var completion = new CompletableFuture<ManagedSearchResult>();
                var board = Board.startingPosition();
                service.start(board, GameHistory.initial(board), new SearchLimits(100, -1, -1, false), SearchObserver.NONE, completion::complete);
                service.stop(); assertNull(completion.get(10, TimeUnit.SECONDS).failure());
            } finally { service.close(); }
            assertTrue(service.isTerminated());
        }
    }
    @Test void effectiveAdaptiveDefaultAtEverySupportedWorkerCount() throws Exception {
        var engine = BaselineEngine.load();
        var board = Board.startingPosition(); var before = board.clone();
        for (int workers = 1; workers <= 16; workers++) {
            var search = engine.create(workers, 1);
            try (var driver = new SearchDriver(search)) {
                assertEquals(AspirationSearch.Policy.ADAPTIVE16, field(search, "policy"));
                var owner = field(search, "owner");
                var kernel = workers == 1 ? field(owner, "exact") : owner;
                assertEquals(3407871L, ((FlatSearchConfig)
                        field(kernel, "construction")).flags());
                if (workers > 1) assertEquals(workers, ((Object[]) field(owner, "workers")).length);
                var completed = driver.search(new SearchRequest(board, 3));
                assertTrue(completed.targetDepthCompleted(), "workers=" + workers);
                assertTrue(completed.lastCompletedResult().hasMove());
                assertEquals(0, driver.activeSearchThreads());
                var control = SearchControl.controlled(1500, System.nanoTime(), 30_000_000_000L, TimeSource.SYSTEM);
                var stopped = driver.search(new SearchRequest(board, GameHistory.initial(board), 40, control));
                assertEquals(1500, stopped.nodes()); assertEquals(control.nodes(), stopped.nodes());
                assertEquals(0, driver.activeSearchThreads()); assertArrayEquals(before, board);
                driver.newGame();
                assertTrue(driver.search(new SearchRequest(board, 3)).targetDepthCompleted());
            }
        }
    }
    @Test void productionMatchesExplicitAdaptiveAndKeepsWidenControl() throws Exception {
        var engine = BaselineEngine.load();
        var board = Board.startingPosition();
        try (var actual = new SearchDriver(engine.create(1, 8));
             var explicit = new SearchDriver(new ConstructionSearch(FlatSearchConfig.candidate()
                             .with(FlatSearchConfig.Feature.NOMINAL_FUTILITY, true)
                             .with(FlatSearchConfig.Feature.VERIFIED_NULL, true),
                             1, 8, AspirationSearch.Policy.ADAPTIVE16).create(engine.evaluation()))) {
            var a = actual.search(new SearchRequest(board, 6)).lastCompletedResult();
            var b = explicit.search(new SearchRequest(board, 6)).lastCompletedResult();
            assertEquals(b.score(), a.score()); assertEquals(b.bestMove(), a.bestMove());
            assertEquals(b.nodes(), a.nodes()); assertArrayEquals(b.principalVariation(), a.principalVariation());
        }
        for (int workers : new int[]{1, 4, 16}) {
            var config = com.ohinteractive.seedv6.search.exact.ConstructionSearch.v1(workers, 1, AspirationSearch.Policy.WIDEN16);
            var search = config.create(engine.evaluation());
            try (var driver = new SearchDriver(search)) {
                assertEquals(AspirationSearch.Policy.WIDEN16, field(search, "policy"));
                assertTrue(config.identity().contains("aspiration=WIDEN16"));
                assertTrue(driver.search(new SearchRequest(board, 4)).targetDepthCompleted());
                assertEquals(0, driver.activeSearchThreads());
            }
        }
    }
    private static ManagedSearchResult run(SearchLifecycleService service, long[] board, SearchLimits limits) throws Exception {
        var result = new CompletableFuture<ManagedSearchResult>();
        service.start(board, GameHistory.initial(board), limits, SearchObserver.NONE, result::complete);
        var completed = result.get(45, TimeUnit.SECONDS);
        assertNull(completed.failure()); return completed;
    }
    private static Object field(Object object, String name) throws Exception {
        var field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object);
    }
}
