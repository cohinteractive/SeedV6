package com.ohinteractive.seedv6.search.manage;

import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.driver.ExactSearchAdapter;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.driver.SearchDriver;
import com.ohinteractive.seedv6.training.validation.ValidationControl;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(30)
class ConcurrentSearchOwnershipTest {
    @Test void managedPlayAndSynchronousTrainingOwnDisjointMutableSearchGraphsAndCancelIndependently() throws Exception {
        var sharedDefinition = SearchEvaluation.incremental(NnueNetwork.initialized(71));
        var playRoot = new ExactSearchAdapter(sharedDefinition);
        var trainingRoot = new ExactSearchAdapter(sharedDefinition);
        var trainingControl = new ValidationControl();
        var caller = Executors.newSingleThreadExecutor();
        Thread playWorker;
        try (var play = new SearchLifecycleService(TimeSource.SYSTEM, () -> playRoot);
             var training = new SearchDriver(trainingRoot)) {
            try {
                playWorker = (Thread) field(play, "worker");
                assertDisjointFields(playRoot, trainingRoot, "exact", "root", "scratch", "rootMoves");
                assertDisjointFields(field(playRoot, "exact"), field(trainingRoot, "exact"),
                        "evaluator", "boards", "moves", "pv", "pvLength", "generatorScratch", "quietScratch",
                        "table", "entry", "historyKeys");
                assertDisjointFields(field(field(playRoot, "exact"), "table"), field(field(trainingRoot, "exact"), "table"),
                        "key", "data", "hashMove", "locks");
                assertDisjointFields(evaluatorState(playRoot), evaluatorState(trainingRoot), "accumulators", "inference");
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
                assertEquals(2, field(field(playRoot, "exact"), "generation"));
                assertEquals(1, field(field(trainingRoot, "exact"), "generation"));
                assertSame(playWorker, field(play, "worker"));
                trainingControl.cancel();
                assertFalse(trainingResult.get(10, TimeUnit.SECONDS).targetDepthCompleted());
                assertTrue(play.isWorking());
                assertEquals(SearchTermination.STOPPED, control.termination());
            } finally {
                trainingControl.cancel(); caller.shutdown();
                assertTrue(caller.awaitTermination(10, TimeUnit.SECONDS));
            }
        }
        assertFalse(playWorker.isAlive());
    }

    private static SearchObserver observer(CountDownLatch latch) {
        return new SearchObserver() {
            @Override public void onIterationCompleted(IterationSnapshot snapshot) { latch.countDown(); }
        };
    }

    // Structural assertions deliberately stay in tests: no production getters on mutable engine state.
    private static Object evaluatorState(ExactSearchAdapter adapter) throws Exception {
        Object wrapper = field(field(adapter, "exact"), "evaluator");
        Object delegate = captured(wrapper, com.ohinteractive.seedv6.search.exact.ExactEvaluator.class);
        return captured(delegate, SearchEvaluation.State.class);
    }
    private static Object captured(Object owner, Class<?> type) throws Exception {
        for(var field : owner.getClass().getDeclaredFields()) {
            if(type.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                return field.get(owner);
            }
        }
        throw new AssertionError("Missing captured " + type);
    }
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
