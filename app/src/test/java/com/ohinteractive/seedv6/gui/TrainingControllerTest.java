package com.ohinteractive.seedv6.gui;

import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.prefs.Preferences;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.service.*;
import com.ohinteractive.seedv6.training.selfplay.SelfPlayBatch;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(60)
class TrainingControllerTest {
    @TempDir Path temp;
    private TrainingController controller;

    private TrainingController create(TrainingSettings settings, TrainingController.Backend backend) throws Exception {
        controller = edt(() -> new TrainingController(settings, backend, ignored -> assertFalse(SwingUtilities.isEventDispatchThread()),
                s -> assertTrue(SwingUtilities.isEventDispatchThread())));
        return controller;
    }
    private TrainingController.ViewState poll() throws Exception { return edt(() -> { controller.poll(); return controller.state(); }); }
    private TrainingController.ViewState finished() throws Exception {
        until(() -> !poll().active()); return poll();
    }
    @AfterEach void close() throws Exception { if (controller != null) edt(controller::beginShutdown).run(); }

    @Test void defaultsPreferencesAndLaunchDoNotCreateCheckpointDirectory() throws Exception {
        var d = TrainingSettings.defaults();
        var config = d.config(TrainerConfig.DepthChange.REQUIRE_SAME);
        assertSame(com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping.V1, config.selfPlay().scoreMapping());
        assertSame(com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping.V1, config.validation().scoreMapping());
        assertEquals(4, d.depth()); assertEquals(1, d.threads()); assertEquals(64, d.games());
        assertEquals(0, d.openingMin()); assertEquals(8, d.openingMax()); assertEquals(32, d.samples());
        assertEquals(64, d.validationPairs()); assertEquals(0, d.maximumGenerations());
        assertEquals(Path.of("C:/user-data/SeedV6-NNUE/training"), TrainingSettings.defaultRoot("C:/user-data", "unused"));
        assertEquals(Path.of("C:/user/.seedv6-nnue/training"), TrainingSettings.defaultRoot(null, "C:/user"));
        var chosen = settings(temp.resolve("not-created"), 6, 0);
        create(chosen, new TrainingController.Backend());
        poll(); assertFalse(Files.exists(chosen.root()));
        Preferences prefs = Preferences.userRoot().node("seedv6-test-" + UUID.randomUUID());
        try {
            chosen.save(prefs); assertEquals(chosen, TrainingSettings.load(prefs));
            assertFalse(Files.exists(chosen.root()));
            assertFalse(Arrays.asList(prefs.keys()).contains("bestId"));
        } finally { prefs.removeNode(); }
    }

    @Tag("slow-nnue")
    @Test void realFreshBoundedGenerationThenResumeLatestAndExactAdamLineage() throws Exception {
        var backend = new ShortBackend();
        create(settings(temp.resolve("run"), 1, 1), backend);
        edt(controller::start);
        assertTrue(edt(() -> controller.state().active()));
        var first = finished();
        assertEquals(TrainingController.Phase.STOPPED, first.phase(), first.message());
        assertEquals(1, backend.fresh); assertEquals(0, backend.resumed);
        assertEquals(1, first.snapshot().generation()); assertTrue(first.snapshot().optimizerStep() > 0);
        assertNotEquals(first.snapshot().bestId(), first.snapshot().latestTrainingId());
        assertTrue(first.resume());
        byte[] original;
        Path bestFile = first.settings().root().resolve("checkpoints").resolve(first.snapshot().bestId()).resolve(CheckpointManifest.TRAINING_FILE);
        original = Files.readAllBytes(bestFile);
        // A new application/controller has no cached trainer or authoritative checkpoint identities.
        edt(controller::beginShutdown).run();
        create(first.settings(), backend);
        edt(controller::start);
        var second = finished();
        assertEquals(TrainingController.Phase.STOPPED, second.phase(), second.message());
        assertEquals(1, backend.fresh); assertEquals(1, backend.resumed);
        assertEquals(2, second.snapshot().generation());
        assertTrue(second.snapshot().optimizerStep() > first.snapshot().optimizerStep());
        try (var store = new CheckpointStore(first.settings().root())) {
            assertEquals(first.snapshot().latestTrainingId(), store.load(second.snapshot().latestTrainingId()).manifest().parentId());
        }
        assertArrayEquals(original, Files.readAllBytes(bestFile));
    }

    @Tag("slow-nnue")
    @Test void changedDepthDeclineThenApprovePreservesPriorDepthAndRecordsNewDepth() throws Exception {
        Path root = temp.resolve("depth");
        String id = bootstrap(root);
        var backend = new ShortBackend();
        create(settings(root, 2, 1), backend);
        edt(controller::start);
        until(() -> poll().phase() == TrainingController.Phase.CONFIRM_DEPTH);
        assertEquals(0, backend.resumed);
        edt(() -> controller.confirmDepth(false));
        assertFalse(poll().active()); assertTrue(poll().message().contains("declined"));
        edt(controller::start);
        until(() -> poll().phase() == TrainingController.Phase.CONFIRM_DEPTH);
        edt(() -> controller.confirmDepth(true));
        var end = finished();
        assertEquals(TrainingController.Phase.STOPPED, end.phase(), end.message());
        try (var store = new CheckpointStore(root)) {
            assertEquals(1, store.load(id).manifest().trainingDepth());
            assertEquals(2, store.load(end.snapshot().latestTrainingId()).manifest().trainingDepth());
        }
        assertEquals(1, backend.resumed); assertEquals(0, backend.fresh);
    }

    @Tag("slow-nnue")
    @Test void depthApprovalIsBoundToTheReviewedLineage() throws Exception {
        Path root = temp.resolve("depth-race"); bootstrap(root);
        var backend = new ShortBackend(); create(settings(root, 2, 1), backend);
        edt(controller::start);
        until(() -> poll().phase() == TrainingController.Phase.CONFIRM_DEPTH);
        candidate(root, false);
        edt(() -> controller.confirmDepth(true));
        var end = finished();
        assertEquals(TrainingController.Phase.FAILED, end.phase());
        assertTrue(end.message().contains("Store changed")); assertEquals(0, backend.resumed);
    }

    @Tag("slow-nnue")
    @Test void corruptOrForeignStoreIsNotResetAndLockedStoreFailsClearly() throws Exception {
        Path root = temp.resolve("corrupt"); String best = bootstrap(root);
        Path artifact = root.resolve("checkpoints").resolve(best).resolve(CheckpointManifest.NETWORK_FILE);
        Files.write(artifact, new byte[] {1, 2, 3});
        create(settings(root, 1, 1), new ShortBackend());
        edt(controller::start); var failed = finished();
        assertEquals(TrainingController.Phase.FAILED, failed.phase());
        assertTrue(failed.message().contains("corruption")); assertArrayEquals(new byte[] {1, 2, 3}, Files.readAllBytes(artifact));
        Path foreign = temp.resolve("foreign"); Files.createDirectory(foreign); Files.writeString(foreign.resolve("keep.txt"), "keep");
        edt(() -> controller.setSettings(settings(foreign, 1, 1))); edt(controller::start);
        assertTrue(finished().message().contains("non-empty folder"));
        assertFalse(Files.exists(foreign.resolve("store.lock"))); assertEquals("keep", Files.readString(foreign.resolve("keep.txt")));
        Path locked = temp.resolve("locked"); bootstrap(locked);
        try (var owner = new CheckpointStore(locked)) {
            edt(() -> controller.setSettings(settings(locked, 1, 1))); edt(controller::start);
            assertTrue(finished().message().contains("ownership"));
        }
    }

    @Test void duplicateStartStopAndCloseStayOffEdtAndDoNotClaimStoppedEarly() throws Exception {
        var backend = new DelayedBackend(); create(settings(temp, 1, 0), backend);
        edt(() -> { controller.start(); controller.start(); });
        assertTrue(backend.inspected.await(5, TimeUnit.SECONDS));
        assertTrue(edt(() -> controller.state().active())); // EDT stays available while I/O is blocked.
        assertThrows(ExecutionException.class, () -> edt(() -> controller.setSettings(settings(temp, 2, 0))));
        backend.releaseInspect.countDown();
        until(() -> backend.creates.get() == 1 && poll().snapshot() != null);
        edt(controller::stop);
        assertEquals(TrainingController.Phase.STOPPING, poll().phase());
        assertFalse(poll().canStart());
        assertEquals(1, backend.creates.get());
        backend.handle.stoppedPublished = true;
        assertTrue(poll().active(), "A published terminal snapshot cannot unlock settings before the owned worker terminates");
        backend.handle.terminated = true;
        assertEquals(TrainingController.Phase.STOPPED, finished().phase());
        Runnable cleanup = edt(controller::beginShutdown);
        cleanup.run(); assertTrue(backend.handle.closed); assertFalse(backend.handle.closedOnEdt);
    }

    @Test void stopAndWindowCloseDuringPendingStartupNeverStartsATrainer() throws Exception {
        var backend = new DelayedBackend(); create(settings(temp, 1, 0), backend);
        edt(controller::start); assertTrue(backend.inspected.await(5, TimeUnit.SECONDS));
        edt(controller::stop); assertEquals(TrainingController.Phase.STOPPING, poll().phase());
        Runnable cleanup = edt(controller::beginShutdown);
        backend.releaseInspect.countDown(); cleanup.run();
        assertEquals(0, backend.creates.get());
    }

    @Tag("slow-nnue")
    @Test void realIndefiniteTrainingStopsAndReleasesStoreOwnership() throws Exception {
        Path root = temp.resolve("indefinite"); create(settings(root, 1, 0), new ShortBackend());
        edt(controller::start);
        until(() -> { var s = poll().snapshot(); return s != null && s.totals().completedGenerations() >= 1; });
        edt(controller::stop);
        var end = finished(); assertEquals(TrainingController.Phase.STOPPED, end.phase(), end.message());
        try (var store = new CheckpointStore(root)) { assertTrue(store.recover().latestTraining().isPresent()); }
    }

    @Tag("slow-nnue")
    @Test void productionBackendBootstrapsDeterministicSeedAndRunsBoundedStandardChess() throws Exception {
        Path root = temp.resolve("standard");
        var chosen = new TrainingSettings(root, 1, 1, 1, 0, 0, 1, 1, 1, 1, 93, 2, 1);
        create(chosen, new TrainingController.Backend()); edt(controller::start);
        var end = finished(); assertEquals(TrainingController.Phase.STOPPED, end.phase(), end.message());
        assertEquals(1, end.snapshot().totals().completedGenerations()); assertEquals(1, end.snapshot().selfPlay().cappedGames());
        try (var store = new CheckpointStore(root)) {
            assertArrayEquals(com.ohinteractive.seedv6.core.nnue.NnueNetworkCodec.encode(com.ohinteractive.seedv6.core.nnue.NnueNetwork.initialized(93)),
                    com.ohinteractive.seedv6.core.nnue.NnueNetworkCodec.encode(store.recover().best().orElseThrow().network()));
        }
    }

    @Test void stopArrivingDuringServiceConstructionPreventsItsStart() throws Exception {
        CountDownLatch creating = new CountDownLatch(1), release = new CountDownLatch(1);
        AtomicInteger starts = new AtomicInteger();
        var backend = new DelayedBackend() {
            @Override TrainingController.Handle create(TrainingSettings s, boolean resume, TrainerConfig.DepthChange change) {
                creating.countDown();
                try { assertTrue(release.await(10, TimeUnit.SECONDS)); } catch (InterruptedException e) { throw new AssertionError(e); }
                return new FakeHandle() {
                    @Override public void start() { starts.incrementAndGet(); super.start(); }
                    @Override public void stop() { super.stop(); terminated = true; }
                };
            }
        };
        backend.releaseInspect.countDown(); create(settings(temp, 1, 0), backend); edt(controller::start);
        try { assertTrue(creating.await(5, TimeUnit.SECONDS)); edt(controller::stop); }
        finally { release.countDown(); }
        assertEquals(TrainingController.Phase.STOPPED, finished().phase()); assertEquals(0, starts.get());
    }

    @Test void queuedStartupCompletionCannotUnlockOrChangeASubsequentRun() throws Exception {
        CountDownLatch firstStarted = new CountDownLatch(1), returnFromFirstStart = new CountDownLatch(1);
        AtomicInteger created = new AtomicInteger();
        var backend = new DelayedBackend() {
            @Override TrainingController.Handle create(TrainingSettings s, boolean resume, TrainerConfig.DepthChange change) {
                if (created.incrementAndGet() != 1) return handle;
                return new FakeHandle() {
                    @Override public void start() {
                        terminated = true; firstStarted.countDown();
                        // Hold the startup callback back while EDT observes a very quickly completed service.
                        try { assertTrue(returnFromFirstStart.await(10, TimeUnit.SECONDS)); }
                        catch (InterruptedException e) { throw new AssertionError(e); }
                    }
                };
            }
        };
        backend.releaseInspect.countDown(); create(settings(temp, 1, 0), backend); edt(controller::start);
        try {
            assertTrue(firstStarted.await(5, TimeUnit.SECONDS)); assertFalse(poll().active());
            edt(controller::start);
        } finally { returnFromFirstStart.countDown(); }
        until(() -> created.get() == 2 && poll().snapshot() != null);
        assertTrue(poll().active()); assertFalse(poll().canStart());
        edt(controller::stop); backend.handle.terminated = true; finished();
    }

    @Test void runtimeFailureIsVisibleAndSnapshotIsAnImmutableValue() throws Exception {
        var backend = new DelayedBackend(); backend.releaseInspect.countDown();
        create(settings(temp, 1, 0), backend); edt(controller::start);
        until(() -> poll().snapshot() != null);
        var previous = poll().snapshot(); backend.handle.failed = true; backend.handle.terminated = true;
        var failed = finished(); assertEquals(TrainingController.Phase.FAILED, failed.phase());
        assertTrue(failed.message().contains("fixture failure"));
        assertEquals(TrainerSnapshot.State.GENERATING_SELF_PLAY, previous.state());
        String text = TrainingProgress.format(failed);
        for (String field : List.of("FAILED", "Best:", "Latest-training:", "Candidate:", "Elapsed:", "Generation:",
                "requested/completed/aborted/capped", "White wins / draws / Black wins", "Mean loss:", "Promotions:", "retains:")) assertTrue(text.contains(field), field);
    }

    @Test void stopChangeResumeShowsRestartNoticeInStatusAndDiagnosticsInsteadOfExactContinuation() throws Exception {
        var first = new FakeHandle();
        var restarted = new FakeHandle() {
            @Override public String lifecycleNotice() {
                return "Restarted unfinished generation 4 from settled checkpoint prior because generation settings changed. This is not an exact continuation.";
            }
        };
        var calls = new AtomicInteger();
        var backend = new TrainingController.Backend() {
            @Override TrainingController.Inspection inspect(TrainingSettings s) { return new TrainingController.Inspection(true, "prior", 1, ""); }
            @Override TrainingController.Handle create(TrainingSettings s, boolean resume, TrainerConfig.DepthChange change) {
                return calls.incrementAndGet() == 1 ? first : restarted;
            }
        };
        create(settings(temp, 1, 1), backend); edt(controller::start); until(() -> poll().snapshot() != null);
        edt(controller::stop); first.terminated = true; finished();
        edt(() -> controller.setSettings(settings(temp, 2, 1))); edt(controller::start);
        until(() -> poll().phase() == TrainingController.Phase.CONFIRM_DEPTH);
        edt(() -> controller.confirmDepth(true)); until(() -> calls.get() == 2 && poll().message().contains("Restarted unfinished"));
        assertTrue(TrainingProgress.format(poll()).contains("not an exact continuation"));
        restarted.terminated = true; assertTrue(finished().message().contains("Restarted unfinished generation 4"));
    }

    @Test void trainingStartNeedsOnlyItsOwnIdleState() throws Exception {
        var backend = new DelayedBackend(); backend.releaseInspect.countDown();
        create(settings(temp, 1, 0), backend);
        edt(() -> { controller.poll(); assertTrue(controller.state().canStart()); controller.start(); });
        until(() -> backend.creates.get() == 1 && poll().snapshot() != null);
        assertTrue(poll().active());
    }

    @Test void changingStoppedRootClearsOldSnapshotIdentitiesEvenOnLaterPolls() throws Exception {
        var backend = new DelayedBackend(); backend.releaseInspect.countDown();
        create(settings(temp, 1, 0), backend); edt(controller::start); until(() -> poll().snapshot() != null);
        edt(controller::stop); backend.handle.terminated = true; finished();
        edt(() -> controller.setSettings(settings(temp.resolve("other"), 1, 0)));
        var changed = poll();
        assertNull(changed.snapshot()); assertFalse(changed.resume());
        assertEquals("", changed.bootstrapId()); assertFalse(Files.exists(temp.resolve("other")));
    }

    @Test void startupFailureAndInvalidFileRootProduceConciseSafeFailures() throws Exception {
        Path file = temp.resolve("file"); Files.writeString(file, "keep");
        create(settings(file, 1, 1), new TrainingController.Backend());
        edt(controller::start); assertTrue(finished().message().contains("directory"));
        assertEquals("keep", Files.readString(file));
        edt(controller::beginShutdown).run();
        var backend = new DelayedBackend() {
            @Override TrainingController.Handle create(TrainingSettings s, boolean resume, TrainerConfig.DepthChange change) {
                throw new IllegalStateException("fixture startup failure");
            }
        };
        backend.releaseInspect.countDown(); create(settings(temp, 1, 0), backend); edt(controller::start);
        var end = finished(); assertEquals(TrainingController.Phase.FAILED, end.phase());
        assertEquals("fixture startup failure", end.message());
    }

    @Test void shutdownFailureRemainsVisibleAndRetryWaitsForActualTermination() throws Exception {
        var backend = new DelayedBackend(); backend.releaseInspect.countDown(); backend.handle.failClose = true;
        create(settings(temp, 1, 0), backend); edt(controller::start); until(() -> poll().snapshot() != null);
        Runnable cleanup = edt(controller::beginShutdown);
        var failure = assertThrows(IllegalStateException.class, cleanup::run);
        assertTrue(failure.getMessage().contains("shutdown has not completed"));
        assertTrue(edt(() -> controller.state().active()));
        assertEquals(TrainingController.Phase.CLOSING, edt(() -> controller.state().phase()));
        backend.handle.terminated = true;
        edt(controller::beginShutdown).run();
    }

    @Test void validationProgressFieldsAreMappedFromTheSnapshotWithoutMutableAliases() {
        var base = new FakeHandle().snapshot();
        Map<com.ohinteractive.seedv6.training.selfplay.GameTermination, Integer> terms = new HashMap<>();
        terms.put(com.ohinteractive.seedv6.training.selfplay.GameTermination.WHITE_CHECKMATES_BLACK, 1);
        terms.put(com.ohinteractive.seedv6.training.selfplay.GameTermination.FIFTY_MOVE_RULE, 1);
        terms.put(com.ohinteractive.seedv6.training.selfplay.GameTermination.PLY_CAP, 2);
        var validation = new com.ohinteractive.seedv6.training.validation.ValidationResult.Statistics(1, 1,
                new com.ohinteractive.seedv6.training.validation.ValidationResult.ColourRecord(1, 0, 0),
                new com.ohinteractive.seedv6.training.validation.ValidationResult.ColourRecord(0, 1, 0), 4, terms);
        terms.clear(); assertEquals(3, validation.terminations().size());
        var assessment = new com.ohinteractive.seedv6.training.validation.PromotionPolicy(1, .9, 0).assess(1, .5);
        var snapshot = new TrainerSnapshot(base.state(), "", base.elapsed(), base.generation(), base.bestId(), base.latestTrainingId(),
                base.candidateId(), base.optimizerStep(), base.trainingDepth(), base.selfPlay(), base.training(),
                base.generationOptimizerUpdates(), base.generationSamplesTrained(), base.meanTrainingLoss(),
                Optional.of(validation), Optional.of(assessment), base.totals());
        var view = new TrainingController.ViewState(settings(temp, 1, 0), TrainingController.Phase.RUNNING,
                snapshot, "", true, false, true, 1, "best");
        String text = TrainingProgress.format(view) + TrainingProgress.validation(view, System.nanoTime());
        for (String expected : List.of("Valid pairs: 1 | Incomplete: 1", "Candidate: Gen unknown    Wins: 1", "Best:      Gen unknown    Wins: 0",
                "Draws: 1", "Validation finished - KEEP BEST | Score 50.0%", "bootstrap, not a promotion", "Mean loss: 0.250000")) assertTrue(text.contains(expected), expected);
    }

    static class DelayedBackend extends TrainingController.Backend {
        final CountDownLatch inspected = new CountDownLatch(1), releaseInspect = new CountDownLatch(1);
        final AtomicInteger creates = new AtomicInteger();
        final FakeHandle handle = new FakeHandle();
        @Override TrainingController.Inspection inspect(TrainingSettings settings) {
            assertFalse(SwingUtilities.isEventDispatchThread()); inspected.countDown();
            try { assertTrue(releaseInspect.await(10, TimeUnit.SECONDS)); } catch (InterruptedException e) { throw new AssertionError(e); }
            return new TrainingController.Inspection(false, "", 1, "");
        }
        @Override TrainingController.Handle create(TrainingSettings s, boolean resume, TrainerConfig.DepthChange change) {
            assertFalse(SwingUtilities.isEventDispatchThread()); creates.incrementAndGet(); return handle;
        }
    }
    static class FakeHandle implements TrainingController.Handle {
        volatile boolean stopping, terminated, stoppedPublished, closed, closedOnEdt, failed, failClose;
        public void start() { assertFalse(SwingUtilities.isEventDispatchThread()); }
        public void stop() { stopping = true; }
        public boolean terminated() { return terminated; }
        public void close() {
            closedOnEdt = SwingUtilities.isEventDispatchThread();
            if (failClose) throw new IllegalStateException("fixture durable I/O still draining");
            closed = true; stopping = true; terminated = true;
        }
        public TrainerSnapshot snapshot() {
            return new TrainerSnapshot(failed ? TrainerSnapshot.State.FAILED : terminated || stoppedPublished ? TrainerSnapshot.State.STOPPED
                    : stopping ? TrainerSnapshot.State.STOPPING : TrainerSnapshot.State.GENERATING_SELF_PLAY,
                    failed ? "fixture failure" : "", Duration.ofSeconds(2), 3, "best", "latest", "candidate", 5, 1,
                    new SelfPlayBatch.Statistics(2, 1, 1, 1, 0, 0, 1, 10, 2, 2, 2, 8, 2), Optional.empty(), 2, 4,
                    .25, Optional.empty(), Optional.empty(), new TrainerSnapshot.Totals(2, 4, 2, 2, 2, 4, 2, 1, 1, 0, 0));
        }
    }
}
