package com.ohinteractive.seedv6.search.manage;

import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.alphabeta.RootParallelSearch;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.iterative.IterativeDeepeningSearch;
import com.ohinteractive.seedv6.training.validation.ValidationControl;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(30)
class ConcurrentSearchOwnershipTest {
    @Test void managedPlayAndSynchronousTrainingOwnDisjointMutableSearchGraphsAndCancelIndependently() throws Exception {
        var playRoot = new RootParallelSearch(2, SearchEvaluation.incremental(NnueNetwork.initialized(71)));
        var trainingRoot = new RootParallelSearch(2, SearchEvaluation.incremental(NnueNetwork.initialized(72)));
        var trainingControl = new ValidationControl();
        var caller = Executors.newSingleThreadExecutor();
        ExecutorService playPool = (ExecutorService) field(playRoot, "executor");
        ExecutorService trainingPool = (ExecutorService) field(trainingRoot, "executor");
        Thread playWorker;
        try (var play = new SearchLifecycleService(TimeSource.SYSTEM, () -> playRoot);
             var training = new IterativeDeepeningSearch(trainingRoot)) {
            try {
                playWorker = (Thread) field(play, "worker");
                assertDisjointFields(playRoot, trainingRoot, "table", "executor", "singleThread", "workers",
                        "rootProbe", "rootDiagnostics", "rootBoard", "rootMoves", "rootResults",
                        "rootElapsedNanos", "workerRootCounts", "workerNodeCounts");
                Object[] a = (Object[]) field(playRoot, "workers"), b = (Object[]) field(trainingRoot, "workers");
                for (Object left : new Object[] {field(playRoot, "singleThread"), a[0], a[1]}) {
                    for (Object right : new Object[] {field(trainingRoot, "singleThread"), b[0], b[1]}) {
                        assertDisjointFields(left, right, "table", "ordering", "picker", "quiescence", "evaluationState",
                                "diagnosticsAccumulator", "boardStack", "unorderedMoves", "unorderedIndices", "probes",
                                "pv", "pvLengths", "bestScores", "bestMoves", "pathDependent", "rootMoves", "generatorScratch");
                        assertDisjointFields(field(left, "ordering"), field(right, "ordering"), "history", "killers", "picker");
                        assertDisjointFields(field(left, "evaluationState"), field(right, "evaluationState"), "accumulators", "inference");
                        assertDisjointFields(field(left, "quiescence"), field(right, "quiescence"), "evaluationState",
                                "ordering", "picker", "boardStack", "legalAvailabilityMoves", "generatorScratch", "result", "standaloneDiagnostics");
                    }
                }
                long[] board = Board.startingPosition(); var history = GameHistory.initial(board);
                SearchControl control = trainingControl.beginSearch();
                var trainingStarted = new CountDownLatch(1);
                var trainingResult = caller.submit(() -> {
                    try {
                        return training.search(new SearchRequest(board, history, 256,
                                observer(trainingStarted), control, true));
                    } finally { trainingControl.endSearch(); }
                });
                var playStarted = new CountDownLatch(1);
                var playResult = new CompletableFuture<ManagedSearchResult>();
                play.start(board, history, new SearchLimits(0, -1, -1, true), observer(playStarted), true, playResult::complete);
                assertTrue(trainingStarted.await(10, TimeUnit.SECONDS)); assertTrue(playStarted.await(10, TimeUnit.SECONDS));
                synchronized (field(play, "lock")) {
                    assertNotSame(control, field(field(play, "current"), "control"));
                }
                assertEquals(SearchTermination.NONE, control.termination());
                play.stop();
                assertEquals(SearchTermination.STOPPED, playResult.get(10, TimeUnit.SECONDS).termination());
                assertFalse(trainingControl.cancelled()); assertFalse(trainingResult.isDone());
                assertEquals(SearchTermination.NONE, control.termination());

                var secondStarted = new CountDownLatch(1);
                play.start(board, history, new SearchLimits(0, -1, -1, true), observer(secondStarted), true, ignored -> {});
                assertTrue(secondStarted.await(10, TimeUnit.SECONDS));
                assertSame(playWorker, field(play, "worker")); assertSame(playPool, field(playRoot, "executor"));
                trainingControl.cancel();
                assertFalse(trainingResult.get(10, TimeUnit.SECONDS).targetDepthCompleted());
                assertTrue(play.isWorking());
                assertEquals(SearchTermination.STOPPED, control.termination());
            } finally {
                trainingControl.cancel(); caller.shutdown();
                assertTrue(caller.awaitTermination(10, TimeUnit.SECONDS));
            }
        }
        assertFalse(playWorker.isAlive()); assertTrue(playPool.isTerminated()); assertTrue(trainingPool.isTerminated());
    }

    private static SearchObserver observer(CountDownLatch latch) {
        return new SearchObserver() {
            @Override public void onIterationCompleted(IterationSnapshot snapshot) { latch.countDown(); }
        };
    }

    // Structural assertions deliberately stay in tests: no production getters on mutable engine state.
    private static Object field(Object owner, String name) throws Exception {
        var field = owner.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(owner);
    }
    private static void assertDisjointFields(Object a, Object b, String... names) throws Exception {
        assertNotSame(a, b);
        for (String name : names) {
            Object left = field(a, name), right = field(b, name);
            assertNotNull(left, name); assertNotNull(right, name); assertNotSame(left, right, name);
            if (left instanceof Object[] x && right instanceof Object[] y && x.length > 0 && y.length > 0
                    && x[0] != null && y[0] != null) assertNotSame(x[0], y[0], name + " first slot");
        }
    }
}
