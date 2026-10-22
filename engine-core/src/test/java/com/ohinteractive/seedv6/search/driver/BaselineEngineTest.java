package com.ohinteractive.seedv6.search.driver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.manage.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class BaselineEngineTest {
    @Test void frozenCompositionAndAllCompetitiveEvaluatorsUseSameSettings() throws Exception {
        for (var choice : BaselineEngine.Evaluator.values()) {
            var engine = BaselineEngine.load(choice);
            assertEquals(262143, BaselineEngine.configuration(4, 8).mechanisms().flags());
            assertTrue(engine.identity(4, 8).contains("aspiration=WIDEN16;parallel=ROOT_REUSE"));
            assertTrue(engine.identity(4, 8).contains("workers=4;hashMiB=8"));
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
        for (int workers : new int[]{1, 2, 4}) {
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
