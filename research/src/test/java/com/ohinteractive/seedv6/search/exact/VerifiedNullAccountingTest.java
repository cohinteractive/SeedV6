package com.ohinteractive.seedv6.search.exact;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.tt.TTable;
import static org.junit.jupiter.api.Assertions.*;

/** Observe admissions outside the accounting wrapper: an omitted reenter cannot hide the event. */
@Timeout(120)
class VerifiedNullAccountingTest {
    private static Object field(Object object, String name) {
        try {
            var f = object.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(object);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static void set(Object object, String name, Object value) {
        try {
            var f = object.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(object, value);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static long count(Object owner, String name) { return (long) field(owner, name); }
    private static SearchControl budget(long nodes) { return SearchControl.controlled(nodes, 0, -1, TimeSource.SYSTEM); }

    /** Test-only interposition, installed before execution; the production wrapper is unchanged. */
    private static final class Boundary implements ExactEvaluator {
        final ExactEvaluator wrapper;
        final Object counters;
        long children, qchildren, attempts, admitted, firstBefore = -1;
        SearchControl control;
        boolean isolated = true;
        Runnable before = () -> {}, after = () -> {};

        Boundary(ExactSearch kernel, Object counters) {
            this.wrapper = (ExactEvaluator) field(kernel, "evaluator");
            this.counters = counters;
            set(kernel, "evaluator", this);
        }

        public void initialize(long[] board) { wrapper.initialize(board); }
        public int evaluate(long[] board, int ply) { return wrapper.evaluate(board, ply); }
        public void child(long[] parent, long[] child, int ply, int qply) {
            long prior = count(counters, "nodes");
            wrapper.child(parent, child, ply, qply);
            long delta = count(counters, "nodes") - prior;
            assertTrue(delta == 0 || delta == 1);
            children += delta;
            if (qply >= 0) qchildren += delta;
        }
        public void reenter(int ply) {
            attempts++;
            long prior = count(counters, "nodes"), qprior = count(counters, "qnodes");
            int maximumBefore = (int) field(counters, "maximumPly");
            if (firstBefore < 0) firstBefore = prior;
            before.run();
            // Exact shared-counter deltas are checked only with one isolated worker.
            long shared = control == null ? 0 : control.nodes();
            boolean refused = control != null && control.termination() != SearchTermination.NONE;
            wrapper.reenter(ply);
            long delta = count(counters, "nodes") - prior;
            assertTrue(delta == 0 || delta == 1);
            if (control != null) {
                if (isolated) assertEquals(delta, control.nodes() - shared, "shared/private admission delta");
                if (refused) assertEquals(0, delta, "no admission after stop");
                else if (control.termination() == SearchTermination.NONE)
                    assertEquals(1, delta, "explicit verifier admission");
                else if (isolated) assertEquals(0, delta, "refused verifier admission");
                // A sibling may stop the request during a successful admission. Check totals after drain.
            }
            assertEquals(qprior, count(counters, "qnodes"), "reentry is main work");
            assertEquals(delta == 1 ? Math.max(maximumBefore, ply) : maximumBefore,
                    field(counters, "maximumPly"));
            admitted += delta;
            after.run();
        }
    }

    private static final class Fixture implements AutoCloseable {
        final SingleDepthSearch owner;
        final Object counters;
        final ExactSearch kernel;
        final Boundary boundary;
        final NullMovePolicy.Diagnostics diagnostics = new NullMovePolicy.Diagnostics();
        long delegated, transitions;
        boolean reject;

        Fixture(boolean parallel, SearchControl control) throws Exception {
            var model = ConventionalNnueEngine.load();
            var delegate = ExactEvaluator.from(model.evaluation());
            var evaluation = new ExactEvaluator() {
                public void initialize(long[] board) { delegate.initialize(board); }
                public void child(long[] parent, long[] child, int ply, int qply) {
                    transitions++;
                    delegate.child(parent, child, ply, qply);
                }
                public void reenter(int ply) { delegated++; }
                public int evaluate(long[] board, int ply) {
                    // A controlled legal-verifier fail-low, not a synthetic fail-low.
                    if (reject && (boolean) field(kernel, "nullVerification"))
                        return (ply & 1) == 1 ? -2000 : 2000;
                    return delegate.evaluate(board, ply);
                }
            };
            var config = BaselineEngine.configuration(1, 1).mechanisms();
            if (parallel) {
                owner = ParallelSearch.constructionResearch(1, model.evaluation(), new TTable(1), config, i -> evaluation);
                counters = ((Object[]) field(owner, "workers"))[0];
                kernel = (ExactSearch) field(counters, "search");
            } else {
                var constructor = ExactSearchAdapter.class.getDeclaredConstructor(ExactEvaluator.class, TTable.class, FlatSearchConfig.class);
                constructor.setAccessible(true);
                owner = constructor.newInstance(evaluation, new TTable(1), config);
                counters = owner;
                kernel = (ExactSearch) field(owner, "exact");
            }
            set(owner, "control", control);
            boundary = new Boundary(kernel, counters);
            boundary.control = control;
            kernel.withFlatNullObservation(false, diagnostics);
        }

        ExactSearchResult run() {
            // Existing Cp64FlatNullTest accepted-verifier witness, using production composition.
            var board = Board.startingPosition();
            var saved = board.clone();
            var result = kernel.searchRootMove(board, GameHistory.initial(board), 4,
                    ExhaustiveOracle.legalMoves(board)[0], 1000, 1001, true,
                    () -> !boundary.control.checkpoint());
            assertArrayEquals(saved, board);
            restored(kernel);
            assertEquals(boundary.children, transitions, "no extra evaluator transition");
            assertEquals(boundary.admitted, delegated, "exactly one delegation per admission");
            assertEquals(boundary.children + boundary.admitted, count(counters, "nodes"));
            assertEquals(count(counters, "nodes"), boundary.control.nodes());
            assertEquals(boundary.qchildren, count(counters, "qnodes"));
            if (result.completed()) Phase2Oracle.legalPv(board, result.principalVariation());
            return result;
        }
        public void close() { set(owner, "control", null); owner.close(); }
    }

    private static void restored(ExactSearch kernel) {
        for (String name : List.of("nullProbeActive", "nullVerification", "constructionNullProbe", "constructionNullVerify"))
            assertEquals(false, field(kernel, name), name);
        assertNull(field(kernel, "history"));
    }

    @Test void isolatedWrappersChargeOnceAndRefuseWithoutDelegationOrTransition() throws Exception {
        for (boolean parallel : new boolean[]{false, true}) try (var f = new Fixture(parallel, budget(2))) {
            var b = Board.startingPosition();
            f.boundary.initialize(b);
            f.boundary.child(b, ExhaustiveOracle.child(b, ExhaustiveOracle.legalMoves(b)[0]), 0, -1);
            f.boundary.reenter(7); // Last successful admission is allowed to unwind.
            assertEquals(2, f.boundary.control.nodes());
            assertEquals(SearchTermination.NONE, f.boundary.control.termination());
            assertTrue(f.boundary.control.checkpoint());
            assertEquals(7, field(f.counters, "maximumPly"));
            f.boundary.reenter(9);
            assertEquals(SearchTermination.NODE_LIMIT, f.boundary.control.termination());
            assertEquals(2, count(f.counters, "nodes"));
            assertEquals(7, field(f.counters, "maximumPly"));
            assertEquals(1, f.delegated);
            assertEquals(1, f.transitions);
        }
    }

    @Test void reachedLegalVerifierIsChargedWhetherItAcceptsOrRejects() throws Exception {
        for (boolean parallel : new boolean[]{false, true}) for (boolean reject : new boolean[]{false, true})
            try (var f = new Fixture(parallel, budget(-1))) {
                f.reject = reject;
                assertTrue(f.run().completed());
                assertEquals(1, f.boundary.attempts);
                assertEquals(1, f.boundary.admitted);
                assertEquals(1, f.diagnostics.verifications);
                assertEquals(reject ? 1 : 0, f.diagnostics.rejected);
                assertEquals(reject ? 0 : 1, f.diagnostics.cutoffs);
            }
    }

    @Test void realVerifierBoundaryExhaustionRefusesOnlyTheAdditionalAdmission() throws Exception {
        for (boolean parallel : new boolean[]{false, true}) {
            long before, complete;
            try (var witness = new Fixture(parallel, budget(-1))) {
                assertTrue(witness.run().completed());
                before = witness.boundary.firstBefore;
                complete = witness.boundary.control.nodes();
                assertTrue(before > 0);
            }
            try (var f = new Fixture(parallel, budget(complete))) {
                assertTrue(f.run().completed(), "final admitted node may complete and unwind");
                assertEquals(complete, f.boundary.control.nodes());
                assertEquals(SearchTermination.NONE, f.boundary.control.termination());
            }
            for (int remaining : new int[]{0, 1}) try (var f = new Fixture(parallel, budget(before + remaining))) {
                long[] evaluationsBefore = {0};
                f.boundary.before = () -> evaluationsBefore[0] = count(f.counters, "evaluations");
                assertFalse(f.run().completed());
                assertEquals(1, f.boundary.attempts, "kernel reached the refused boundary");
                assertEquals(remaining, f.boundary.admitted);
                assertEquals(before + remaining, f.boundary.control.nodes());
                assertEquals(SearchTermination.NODE_LIMIT, f.boundary.control.termination());
                if (remaining == 0) assertEquals(evaluationsBefore[0], count(f.counters, "evaluations"),
                        "refused verifier never evaluates the position");
                f.boundary.control = budget(-1);
                set(f.owner, "control", f.boundary.control);
                assertTrue(f.kernel.search(Board.startingPosition(), 2).completed(), "restart after unwind");
            }
        }
    }

    private static SingleDepthSearch aspiration(SingleDepthSearch search) {
        if (search instanceof com.ohinteractive.seedv6.search.manage.Boundary2ClockPolicy)
            search = (SingleDepthSearch) field(search, "owner");
        assertInstanceOf(AspirationSearch.class, search);
        return search;
    }

    private static Object[] counters(SingleDepthSearch search) {
        var owner = field(aspiration(search), "owner");
        return owner instanceof ParallelSearch ? (Object[]) field(owner, "workers") : new Object[]{owner};
    }

    private static ExactSearch kernel(Object counters) {
        return (ExactSearch) field(counters, counters instanceof ExactSearchAdapter ? "exact" : "search");
    }

    private static Boundary[] observe(SingleDepthSearch search, SearchControl control) {
        var owners = counters(search);
        var boundaries = new Boundary[owners.length];
        for (int i = 0; i < owners.length; i++) {
            boundaries[i] = new Boundary(kernel(owners[i]), owners[i]);
            boundaries[i].control = control;
            boundaries[i].isolated = owners.length == 1;
        }
        return boundaries;
    }

    private static long admissions(Boundary[] boundaries) {
        return Arrays.stream(boundaries).mapToLong(b -> b.children + b.admitted).sum();
    }

    private static void drained(SingleDepthSearch search) {
        assertEquals(0, search.activeSearchThreads());
        var owner = field(aspiration(search), "owner");
        assertEquals(false, field(owner, "active"));
        assertNull(field(owner, "control"));
        if (owner instanceof ParallelSearch) {
            assertNull(field(owner, "request"));
            assertTrue(Arrays.stream((Object[]) field(owner, "futures")).allMatch(java.util.Objects::isNull));
        }
    }

    @Test void productionDispatchAggregatesEveryAdmissionAcrossWorkersAndAspirationPasses() throws Exception {
        var model = BaselineEngine.load();
        var board = Board.startingPosition();
        var saved = board.clone();
        for (int workers : new int[]{1, 2, 8, 16}) {
            var search = model.create(workers, 1);
            var control = budget(100000);
            var boundaries = observe(search, control);
            var prior = new long[workers];
            var priorQ = new long[workers];
            var participated = new boolean[workers];
            long[] passes = {0}, published = {0};
            set(aspiration(search), "observer", (AspirationSearch.AttemptObserver) (d, a, b, result, nanos) -> {
                long sum = 0, qsum = 0;
                int maximumPly = 0;
                for (int i = 0; i < boundaries.length; i++) {
                    var event = boundaries[i];
                    long total = event.children + event.admitted;
                    long privateNodes = count(event.counters, "nodes");
                    assertEquals(total - prior[i], privateNodes, "worker " + i + " invocation");
                    assertEquals(event.qchildren - priorQ[i], count(event.counters, "qnodes"));
                    prior[i] = total;
                    priorQ[i] = event.qchildren;
                    participated[i] |= privateNodes > 0;
                    sum += privateNodes;
                    qsum += count(event.counters, "qnodes");
                    maximumPly = Math.max(maximumPly, (int) field(event.counters, "maximumPly"));
                    restored(kernel(event.counters));
                }
                assertEquals(sum, result.nodes());
                assertEquals(sum, result.diagnostics().totalEnteredNodes());
                assertEquals(qsum, result.diagnostics().worker().nodes().qNodes());
                assertEquals(maximumPly, result.diagnostics().worker().nodes().maximumAbsolutePly());
                assertEquals(control.nodes(), admissions(boundaries));
                drained(search);
                if (workers > 1) {
                    var parallel = (ParallelSearch) field(aspiration(search), "owner");
                    assertEquals(sum, Arrays.stream(parallel.workerWork()).mapToLong(ParallelSearch.WorkerWork::nodes).sum());
                    if ((boolean) field(parallel, "splitActive"))
                        assertEquals(sum, Arrays.stream((long[]) field(parallel, "splitEffort")).sum(),
                                "ROOT_REUSE effort includes all admitted job work");
                }
                passes[0]++;
            });
            var observer = new SearchObserver() {
                public void onIterationCompleted(IterationSnapshot snapshot) {
                    assertEquals(control.nodes(), snapshot.nodes());
                    assertEquals(admissions(boundaries), snapshot.nodes());
                    published[0]++;
                }
            };
            try (var driver = new SearchDriver(search)) {
                var result = driver.search(new SearchRequest(board, GameHistory.initial(board), 40, observer, control, true));
                assertEquals(100000, result.nodes());
                assertEquals(result.nodes(), result.diagnostics().totalEnteredNodes());
                assertEquals(result.nodes(), admissions(boundaries));
                assertTrue(Arrays.stream(boundaries).mapToLong(b -> b.attempts).sum() > 0, "reached verifier W" + workers);
                assertTrue(Arrays.stream(boundaries).mapToLong(b -> b.admitted).sum() > 0, "admitted verifier W" + workers);
                assertTrue(passes[0] >= published[0]);
                assertTrue(published[0] > 0);
                assertEquals(SearchTermination.NODE_LIMIT, control.termination());
                drained(search);
                if (workers > 1) {
                    boolean helperWorked = false;
                    for (int i = 1; i < workers; i++) helperWorked |= participated[i];
                    assertTrue(helperWorked, "actual helper work W" + workers);
                }
                assertArrayEquals(saved, board);
                Phase2Oracle.legalPv(board, result.lastCompletedResult().principalVariation());
                // Remove only the test observer; fresh request control and normal production restart.
                set(aspiration(search), "observer", null);
                for (var boundary : boundaries) boundary.control = null;
                driver.newGame();
                assertTrue(driver.search(new SearchRequest(board, 3)).targetDepthCompleted());
                drained(search);
            }
        }
    }

    @Test void cancellationAndInterruptionAtVerifierBoundaryDoNotPublishIncompleteWork() throws Exception {
        var model = BaselineEngine.load();
        var board = Board.startingPosition();
        for (int workers : new int[]{1, 2, 8}) for (boolean interrupt : new boolean[]{false, true}) {
            var search = model.create(workers, 1);
            var control = budget(100000);
            var boundaries = observe(search, control);
            var fired = new java.util.concurrent.atomic.AtomicBoolean();
            var ownerThread = Thread.currentThread();
            // Stop the owner at its first actual verifier boundary; other workers still must drain.
            Runnable stop = () -> {
                if (fired.compareAndSet(false, true)) {
                    if (interrupt) ownerThread.interrupt();
                    else control.request(SearchTermination.STOPPED);
                }
            };
            if (interrupt) boundaries[0].after = stop; // Already-admitted work must survive abort.
            else boundaries[0].before = stop; // Cancellation refuses admission and delegation.
            var published = new java.util.ArrayList<IterationSnapshot>();
            var observer = new SearchObserver() {
                public void onIterationCompleted(IterationSnapshot snapshot) { published.add(snapshot); }
            };
            try (var driver = new SearchDriver(search)) {
                SearchDriverOutcome result;
                try {
                    result = driver.search(new SearchRequest(board, GameHistory.initial(board), 40, observer, control, true));
                    assertEquals(interrupt, Thread.currentThread().isInterrupted());
                } finally { Thread.interrupted(); }
                assertTrue(fired.get(), "owner verifier reached W" + workers);
                assertEquals(SearchTermination.STOPPED, control.termination());
                assertFalse(result.targetDepthCompleted());
                assertTrue(result.iterationIncomplete());
                assertEquals(control.nodes(), result.nodes());
                assertEquals(admissions(boundaries), result.nodes());
                assertEquals(result.nodes(), result.diagnostics().totalEnteredNodes());
                assertEquals(interrupt ? 1 : 0, boundaries[0].admitted);
                assertEquals(1, boundaries[0].attempts);
                drained(search);
                for (var event : boundaries) restored(kernel(event.counters));
                assertFalse(published.isEmpty());
                assertEquals(published.getLast().depth(), result.lastCompletedResult().depth());
                assertTrue(result.lastCompletedResult().depth() < result.attemptedDepth());
                Phase2Oracle.legalPv(board, result.lastCompletedResult().principalVariation());
                for (var event : boundaries) {
                    event.control = null;
                    event.before = event.after = () -> {};
                }
                driver.newGame();
                assertTrue(driver.search(new SearchRequest(board, 3)).targetDepthCompleted());
                drained(search);
            }
        }
    }
}
