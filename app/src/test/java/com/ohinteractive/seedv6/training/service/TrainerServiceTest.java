package com.ohinteractive.seedv6.training.service;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.history.*;
import com.ohinteractive.seedv6.training.nnue.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.validation.*;
import static com.ohinteractive.seedv6.training.service.TrainerSnapshot.State.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class TrainerServiceTest {
    @TempDir Path root;
    static final String START = "4k3/8/8/8/8/8/8/R3K3 w - - 98 1";
    static final NnueScoreMapping MAPPING = new NnueScoreMapping(1_000_000);
    static final PromotionPolicy GATE = new PromotionPolicy(1, 0.9, 0);
    static final Duration WAIT = Duration.ofSeconds(90);

    static TrainerConfig config(Path root, long generations) {
        return new TrainerConfig(root, 12091,
                new TrainerConfig.SelfPlay(1, 1, 2, 0, 0, 2, 8, MAPPING),
                new TrainerConfig.Training(1, 2, true),
                new TrainerConfig.Validation(2, 0, 0, 1, 1, 8, MAPPING, GATE),
                generations, TrainerConfig.DepthChange.REQUIRE_SAME, START);
    }
    static NnueTrainer trainer() { return new NnueTrainer(TrainableNnue.initialized(71)); }
    static CheckpointManifest.Metadata metadata(long generation, String parent) {
        return new CheckpointManifest.Metadata(generation, 1, parent);
    }
    static ValidationResult outcome(ValidationConfig config, long[] board, GameHistory history, boolean win) {
        var white = new ValidationResult.Game(win ? GameTermination.WHITE_CHECKMATES_BLACK : GameTermination.FIFTY_MOVE_RULE, 2);
        var black = new ValidationResult.Game(win ? GameTermination.BLACK_CHECKMATES_WHITE : GameTermination.FIFTY_MOVE_RULE, 2);
        return new ValidationResult(config, ValidationArena.stateHash(board, history),
                Collections.nCopies(config.openingPairs(), new ValidationResult.Pair("", white, black)));
    }
    static class Matches extends TrainerService.Operations {
        int calls;
        final boolean win;
        Matches() { this(false); }
        Matches(boolean win) { this.win = win; }
        @Override ValidationResult validate(NnueNetwork candidate, NnueNetwork incumbent, ValidationConfig config,
                long[] board, GameHistory history, ValidationControl control, Consumer<ValidationProgress> observer) {
            calls++;
            if (control.cancelled()) return super.validate(candidate, incumbent, config, board, history, control, observer);
            return outcome(config, board, history, win);
        }
    }
    static TrainerSnapshot finish(TrainerService service) throws Exception {
        service.start();
        assertTrue(service.awaitTermination(WAIT), service.snapshot().toString());
        assertEquals(STOPPED, service.snapshot().state(), service.failure().map(Throwable::toString).orElse(""));
        assertTrue(service.isTerminated());
        return service.snapshot();
    }
    static long count(Path path) throws IOException { try (var paths = Files.list(path)) { return paths.count(); } }
    static String initial(CheckpointStore store) throws IOException {
        return store.initialize(trainer(), metadata(0, "")).manifest().id();
    }
    static String candidate(CheckpointStore store, String parent) throws IOException {
        NnueTrainer next = store.resume(parent);
        next.trainBatch(new long[][] {Board.fromFen(START)}, new double[] {0}, 1);
        return store.publish(next, metadata(1, parent)).manifest().id();
    }
    static Consumer<TrainerSnapshot> ignore() { return snapshot -> {}; }

    @Test void configurationValidatesBoundsAndKeepsSeparateDeterministicGenerationStreams() {
        var cfg = config(root, 0);
        assertEquals(0, cfg.maximumGenerations());
        Set<Long> seeds = new HashSet<>();
        for (int g = 0; g < 4; g++) for (var domain : TrainerConfig.SeedDomain.values()) {
            assertEquals(cfg.seed(g, domain), config(root.resolve("other"), 2).seed(g, domain));
            assertTrue(seeds.add(cfg.seed(g, domain)));
        }
        assertThrows(IllegalArgumentException.class, () -> config(root, -1));
        assertThrows(IllegalArgumentException.class, () -> new TrainerConfig.SelfPlay(0, 1, 1, 0, 0, 1, 1, MAPPING));
        assertThrows(IllegalArgumentException.class, () -> new TrainerConfig.Training(0, 1, true));
        assertThrows(IllegalArgumentException.class, () -> new TrainerConfig.Validation(0, 0, 0, 1, 1, 8, MAPPING, GATE));
        long[] first = cfg.startingBoard(); first[0] = 0;
        assertArrayEquals(Board.fromFen(START), cfg.startingBoard());
    }

    @Tag("slow-nnue")
    @Test void twoRetainedCandidatesAdvanceExactLearningLineageAndPublishImmutableSnapshots() throws Exception {
        List<TrainerSnapshot> snapshots = new CopyOnWriteArrayList<>();
        List<byte[]> actors = new ArrayList<>();
        Matches work = new Matches() {
            @Override SelfPlayBatch generate(NnueNetwork actor, SelfPlayConfig cfg, long[] board, SelfPlayControl control, Consumer<SelfPlayBatch.Progress> observer) {
                try { actors.add(NnueNetworkCodec.encode(actor)); } catch (IOException failure) { throw new AssertionError(failure); }
                return super.generate(actor, cfg, board, control, observer);
            }
        };
        TrainerSnapshot end;
        try (var service = TrainerService.fresh(config(root, 2), trainer(), work, snapshots::add)) {
            assertEquals(IDLE, service.snapshot().state());
            end = finish(service);
            assertThrows(IllegalStateException.class, service::start);
            assertEquals(2, end.generation()); assertEquals(4, end.optimizerStep());
            assertEquals(2, end.totals().completedGenerations()); assertEquals(2, end.totals().retainedCandidates());
            assertEquals(0, end.totals().promotions()); assertEquals(4, end.totals().selfPlayGames());
            assertEquals(8, end.totals().sampledPositions()); assertEquals(4, end.totals().optimizerUpdates());
            assertEquals(2, end.generationOptimizerUpdates()); assertEquals(4, end.generationSamplesTrained());
            assertEquals(2, end.selfPlay().draws()); assertEquals(4, end.selfPlay().totalPlayedPlies());
            assertEquals(4, end.selfPlay().rawTrajectoryPositions()); assertEquals(4, end.selfPlay().sampledPositions());
            assertTrue(Double.isFinite(end.training().orElseThrow().finalLoss()));
            assertTrue(Double.isFinite(end.meanTrainingLoss()));
            assertEquals(2, end.validation().orElseThrow().validPairs());
            assertEquals(4, end.validation().orElseThrow().draws());
            assertEquals(0.5, end.assessment().orElseThrow().mean());
            assertEquals(PromotionPolicy.Decision.RETAIN_INCUMBENT, end.assessment().orElseThrow().decision());
            assertFalse(end.running()); assertFalse(end.failed()); assertFalse(end.stopping());
            assertTrue(end.elapsed().isPositive());
        }
        try (var store = new CheckpointStore(root)) {
            var c = store.load(end.latestTrainingId());
            var b = store.load(c.manifest().parentId());
            var a = store.load(b.manifest().parentId());
            assertEquals(0, a.manifest().generation()); assertEquals(1, b.manifest().generation());
            assertEquals(2, b.manifest().optimizerStep()); assertEquals(4, c.manifest().optimizerStep());
            assertEquals(a.manifest().id(), end.bestId()); assertEquals(c.manifest().id(), end.candidateId());
            assertArrayEquals(NnueNetworkCodec.encode(a.network()), actors.get(0));
            assertArrayEquals(NnueNetworkCodec.encode(b.network()), actors.get(1));
            assertNotEquals(a.manifest().networkSha256(), b.manifest().networkSha256());
            assertEquals(1, count(root.resolve("promotions"))); assertEquals(2, count(root.resolve("validations")));
        }
        var states = snapshots.stream().map(TrainerSnapshot::state).toList();
        assertTrue(states.containsAll(List.of(RECOVERING, GENERATING_SELF_PLAY, TRAINING,
                PUBLISHING_CANDIDATE, VALIDATING, RECORDING_DECISION)));
        TrainerSnapshot first = snapshots.stream().filter(s -> s.totals().completedGenerations() == 1).findFirst().orElseThrow();
        assertEquals(1, first.generation()); assertEquals(2, first.optimizerStep());
        assertEquals(1, first.totals().retainedCandidates());
        assertThrows(UnsupportedOperationException.class, () -> first.validation().orElseThrow().terminations().clear());
        assertNoOwnedThreads();
    }

    @Tag("slow-nnue")
    @Test void promotionChangesOnlyBestWhileFollowingCandidateRemainsTrainingParent() throws Exception {
        Matches work = new Matches(true) {
            @Override ValidationResult validate(NnueNetwork candidate, NnueNetwork incumbent, ValidationConfig cfg,
                    long[] board, GameHistory history, ValidationControl control, Consumer<ValidationProgress> observer) {
                return outcome(cfg, board, history, calls++ == 0);
            }
        };
        TrainerSnapshot end;
        try (var service = TrainerService.fresh(config(root, 2), trainer(), work, ignore())) { end = finish(service); }
        try (var store = new CheckpointStore(root)) {
            var c = store.load(end.latestTrainingId());
            assertEquals(c.manifest().parentId(), end.bestId());
            assertEquals(end.bestId(), store.validationFor(c.manifest().id()).orElseThrow().incumbentId());
            assertEquals(2, count(root.resolve("promotions")));
            assertEquals(1, end.totals().promotions()); assertEquals(1, end.totals().retainedCandidates());
            var records = new HistoryRepository(root).refresh().records();
            assertEquals(2, records.size()); assertTrue(records.getFirst().promoted());
            assertEquals(records.getFirst().candidate(), records.getLast().incumbent());
            assertEquals(records.getFirst().candidate(), records.getFirst().resultingBest());
            assertNotEquals(records.getFirst().incumbent(), records.getLast().incumbent());
            assertEquals(4, records.getFirst().wins()); assertEquals(1, records.getFirst().score());
        }
    }

    @Tag("slow-nnue")
    @Test void splitResumeMatchesUninterruptedExactAdamStateAndGenerationIdentity() throws Exception {
        Path continuous = root.resolve("continuous"), split = root.resolve("split");
        TrainerSnapshot a, b, c;
        try (var service = TrainerService.fresh(config(continuous, 2), trainer(), new Matches(), ignore())) { a = finish(service); }
        try (var service = TrainerService.fresh(config(split, 1), trainer(), new Matches(), ignore())) { b = finish(service); }
        byte[] prior = Files.readAllBytes(split.resolve("checkpoints").resolve(b.latestTrainingId()).resolve(CheckpointManifest.TRAINING_FILE));
        AtomicBoolean sawExactResume = new AtomicBoolean();
        Matches work = new Matches() {
            @Override Optional<SelfPlayTraining.Statistics> train(NnueTrainer trainer, SelfPlayBatch batch,
                    SelfPlayTraining.Config cfg, SelfPlayControl control, Consumer<SelfPlayTraining.Progress> observer) {
                try { assertArrayEquals(prior, TrainingStateCodec.encode(trainer)); }
                catch (IOException failure) { throw new AssertionError(failure); }
                sawExactResume.set(true);
                return super.train(trainer, batch, cfg, control, observer);
            }
        };
        try (var service = TrainerService.resume(config(split, 1), work, ignore())) { c = finish(service); }
        assertTrue(sawExactResume.get()); assertEquals(1, work.calls, "The persisted RETAIN is not revalidated.");
        assertEquals(a.bestId(), c.bestId()); assertEquals(a.latestTrainingId(), c.latestTrainingId());
        assertEquals(2, c.generation()); assertEquals(4, c.optimizerStep());
        assertEquals(-1, Files.mismatch(continuous.resolve("checkpoints").resolve(a.latestTrainingId()).resolve(CheckpointManifest.TRAINING_FILE),
                split.resolve("checkpoints").resolve(c.latestTrainingId()).resolve(CheckpointManifest.TRAINING_FILE)));
        assertEquals(1, count(split.resolve("promotions")));
        assertEquals(2, count(split.resolve("validations")));
        assertEquals(2, new HistoryRepository(split).refresh().records().size(), "Resume must not duplicate settled generations");
    }

    @Tag("slow-nnue")
    @Test void realTwoGenerationHeadlessSmokeUsesSelfPlayAdamPublisherAndArena() throws Exception {
        TrainerSnapshot end;
        try (var service = TrainerService.fresh(config(root, 2), trainer())) { end = finish(service); }
        var archive = new HistoryRepository(root).refresh();
        assertEquals(2, archive.records().size()); assertTrue(archive.warnings().isEmpty());
        for (var record : archive.records()) {
            assertEquals(end.bestId(), record.incumbent()); assertEquals(record.incumbent(), record.resultingBest());
            assertEquals(GenerationRecord.Outcome.RETAINED, record.outcome()); assertEquals(.5, record.score());
            assertEquals(0, record.wins()); assertEquals(4, record.draws()); assertEquals(0, record.losses());
            assertEquals(new GenerationRecord.Regime(1, 2, 2, 1), record.regime());
            assertEquals(4, record.samples()); assertNotNull(record.loss());
            assertNotNull(record.started()); assertFalse(record.completed().isBefore(record.started()));
            assertTrue(record.totalNanos() > 0); assertTrue(record.selfPlayNanos() > 0);
            assertTrue(record.trainingNanos() > 0); assertTrue(record.validationNanos() > 0);
        }
        assertEquals(end.latestTrainingId(), archive.records().getLast().candidate());
        assertEquals(2, end.totals().completedGenerations()); assertEquals(4, end.totals().completedGames());
        assertEquals(0, end.totals().abortedGames()); assertEquals(0, end.totals().cappedGames());
        assertEquals(8, end.totals().sampledPositions()); assertEquals(4, end.totals().optimizerUpdates());
        try (var store = new CheckpointStore(root)) {
            var c = store.load(end.latestTrainingId());
            var b = store.load(c.manifest().parentId());
            var a = store.load(b.manifest().parentId());
            var first = store.validationFor(b.manifest().id()).orElseThrow();
            var second = store.validationFor(c.manifest().id()).orElseThrow();
            assertEquals(2, first.statistics().validPairs()); assertEquals(2, second.statistics().validPairs());
            assertEquals(0, first.statistics().incompletePairs() + second.statistics().incompletePairs());
            assertEquals(8, first.statistics().draws() + second.statistics().draws());
            assertEquals(2, end.totals().retainedCandidates()); assertEquals(0, end.totals().promotions());
            System.out.println("G_REAL_AUTONOMOUS_SMOKE startBest=" + a.manifest().id()
                    + " startLatest=" + a.manifest().id() + " candidateB=" + b.manifest().id()
                    + " finalBest=" + end.bestId() + " finalLatest=" + end.latestTrainingId()
                    + " generations=2 gamesCompleted=4 aborted=0 capped=0 samples=8 optimizerUpdates=4"
                    + " validPairs=4 incompletePairs=0 candidateWDL=0/8/0 decisions="
                    + first.assessment().decision() + "," + second.assessment().decision()
                    + " promotions=0 retains=2 elapsed=" + end.elapsed() + " depth=1 threads=1 plumbingNotStrength=true");
        }
        assertNoOwnedThreads();
    }

    @Tag("slow-nnue")
    @Test void unpublishedValidationIsFinishedBeforeNextGeneration() throws Exception {
        String a, b;
        try (var store = new CheckpointStore(root)) { a = initial(store); b = candidate(store, a); }
        Matches work = new Matches() {
            @Override SelfPlayBatch generate(NnueNetwork actor, SelfPlayConfig cfg, long[] board, SelfPlayControl control, Consumer<SelfPlayBatch.Progress> observer) {
                assertEquals(1, calls, "Pending B must be resolved before generation C.");
                try { assertEquals(1, count(root.resolve("validations"))); } catch (IOException failure) { throw new AssertionError(failure); }
                return super.generate(actor, cfg, board, control, observer);
            }
        };
        TrainerSnapshot end;
        try (var service = TrainerService.resume(config(root, 1), work, ignore())) { end = finish(service); }
        assertEquals(2, end.generation()); assertEquals(3, end.optimizerStep());
        assertEquals(a, end.bestId()); assertEquals(2, work.calls); assertEquals(1, end.totals().recoveredLifecycles());
        try (var store = new CheckpointStore(root)) { assertEquals(b, store.load(end.latestTrainingId()).manifest().parentId()); }
    }

    @Tag("slow-nnue")
    @Test void persistedRetainIsReusedWithoutRerunningMatch() throws Exception {
        String b, record;
        var cfg = config(root, 1);
        try (var store = new CheckpointStore(root)) {
            String a = initial(store); b = candidate(store, a);
            record = store.recordValidation(b, a, outcome(cfg.validation(1), cfg.startingBoard(), GameHistory.initial(cfg.startingBoard()), false), GATE).id();
        }
        Matches work = new Matches();
        var changed = new TrainerConfig(cfg.checkpointRoot(), cfg.masterSeed(), cfg.selfPlay(), cfg.training(),
                new TrainerConfig.Validation(3, 0, 0, 2, 2, 8, MAPPING, PromotionPolicy.DEFAULT),
                cfg.maximumGenerations(), cfg.depthChange(), cfg.startingFen());
        List<TrainerSnapshot> recovered = new ArrayList<>();
        try (var service = TrainerService.resume(changed, work, recovered::add)) {
            var end = finish(service); assertEquals(0, end.totals().recoveredLifecycles());
        }
        var details = recovered.stream().filter(s -> s.state() == TrainerSnapshot.State.RECORDING_DECISION)
                .findFirst().orElseThrow().validationDetails().orElseThrow();
        assertEquals(cfg.validation(1), details.config(), "Recovery reports the stored experiment, not the new run settings");
        assertEquals(GATE, details.policy()); assertEquals(b, details.candidateId());
        assertEquals(1, work.calls); assertEquals(2, count(root.resolve("validations")));
        try (var store = new CheckpointStore(root)) { assertEquals(record, store.validationFor(b).orElseThrow().id()); }
    }

    @Tag("slow-nnue")
    @ParameterizedTest @EnumSource(PromotionCrash.class)
    void resumesPromotionWithoutDuplicatingEvidenceOrMatch(PromotionCrash crash) throws Exception {
        var cfg = config(root, 1);
        String a, b, evidence;
        AtomicBoolean armed = new AtomicBoolean();
        try (var store = StoreFaults.open(root, (source, target) -> armed.get() && target.endsWith("best"))) {
            a = initial(store); b = candidate(store, a);
            var record = store.recordValidation(b, a, outcome(cfg.validation(1), cfg.startingBoard(), GameHistory.initial(cfg.startingBoard()), true), GATE);
            evidence = record.id();
            if (crash != PromotionCrash.VALIDATION_ONLY) {
                armed.set(true);
                assertThrows(IOException.class, () -> store.promote(record.id()));
                assertEquals(a, store.recover().best().orElseThrow().manifest().id(), "Valid old best remains until explicit completion.");
                if (crash == PromotionCrash.MISSING_REFERENCE) Files.delete(root.resolve("refs/best"));
            }
        }
        Matches work = new Matches();
        try (var service = TrainerService.resume(cfg, work, ignore())) {
            var end = finish(service); assertEquals(b, end.bestId()); assertEquals(2, end.generation());
        }
        assertEquals(1, work.calls); assertEquals(2, count(root.resolve("promotions")));
        try (var store = new CheckpointStore(root)) {
            var current = store.recover().bestEvidence().orElseThrow();
            assertEquals(evidence, current.validationId());
            assertEquals(current, store.completePromotion(evidence));
            Files.delete(root.resolve("refs/best"));
            assertEquals(current, store.completePromotion(evidence));
            assertTrue(Files.isRegularFile(root.resolve("refs/best")), "Completion repairs a recovered missing reference.");
            assertEquals(2, count(root.resolve("promotions")));
        }
    }
    enum PromotionCrash { VALIDATION_ONLY, OLD_REFERENCE, MISSING_REFERENCE }

    @Tag("slow-nnue")
    @Test void stagingFragmentsNeverBecomeAGenerationOrGetDeleted() throws Exception {
        String a;
        try (var store = new CheckpointStore(root)) { a = initial(store); }
        Path partial = Files.createDirectories(root.resolve("staging/g999999-partial"));
        Files.writeString(partial.resolve("training.state"), "partial");
        Files.writeString(root.resolve("refs/.latest-training-interrupted.tmp"), "partial");
        try (var service = TrainerService.resume(config(root, 1), new Matches(), ignore())) {
            var end = finish(service); assertEquals(1, end.generation()); assertEquals(a, end.bestId());
        }
        assertEquals("partial", Files.readString(partial.resolve("training.state")));
        assertEquals(2, count(root.resolve("checkpoints")));
    }

    @Tag("slow-nnue")
    @Test void freshCannotOverwriteExistingLineageAndResumeCannotInventBest() throws Exception {
        String id;
        try (var store = new CheckpointStore(root)) { id = store.publish(trainer(), metadata(0, "")).manifest().id(); }
        for (boolean fresh : List.of(true, false)) {
            try (var service = fresh ? TrainerService.fresh(config(root, 1), trainer()) : TrainerService.resume(config(root, 1))) {
                service.start(); assertTrue(service.awaitTermination(WAIT)); assertEquals(FAILED, service.snapshot().state());
                assertTrue(service.failure().isPresent()); assertEquals(1, count(root.resolve("checkpoints")));
            }
        }
        assertEquals(0, count(root.resolve("promotions")));
        try (var store = new CheckpointStore(root)) { assertEquals(id, store.recover().latestTraining().orElseThrow().manifest().id()); }
    }

    @Tag("slow-nnue")
    @Test void freshCopiesCallerStateAndDepthChangeRequiresExplicitChoice() throws Exception {
        NnueTrainer input = trainer(); byte[] expected = TrainingStateCodec.encode(input);
        TrainerSnapshot end;
        try (var service = TrainerService.fresh(config(root, 1), input, new Matches(), ignore())) {
            input.trainBatch(new long[][] {Board.fromFen(START)}, new double[] {1}, 1);
            end = finish(service);
        }
        assertArrayEquals(expected, Files.readAllBytes(root.resolve("checkpoints").resolve(end.bestId()).resolve(CheckpointManifest.TRAINING_FILE)));
        var old = config(root, 1);
        var deeper = new TrainerConfig.SelfPlay(2, 1, 2, 0, 0, 2, 8, MAPPING);
        var denied = new TrainerConfig(root, old.masterSeed(), deeper, old.training(), old.validation(), 1,
                TrainerConfig.DepthChange.REQUIRE_SAME, START);
        try (var service = TrainerService.resume(denied)) {
            service.start(); assertTrue(service.awaitTermination(WAIT)); assertEquals(FAILED, service.snapshot().state());
            assertTrue(service.snapshot().failureSummary().contains("depth"));
        }
        var allowed = new TrainerConfig(root, old.masterSeed(), deeper, old.training(), old.validation(), 1,
                TrainerConfig.DepthChange.EXPLICITLY_ALLOW, START);
        try (var service = TrainerService.resume(allowed, new Matches(), ignore())) { end = finish(service); }
        try (var store = new CheckpointStore(root)) {
            var c = store.load(end.latestTrainingId()); assertEquals(2, c.manifest().trainingDepth());
            assertEquals(1, store.load(c.manifest().parentId()).manifest().trainingDepth());
            assertEquals(4, end.optimizerStep());
        }
        assertEquals(List.of(1, 2), new HistoryRepository(root).refresh().records().stream()
                .map(r -> r.regime().depth()).toList(), "Each generation keeps its own configuration across resume");
    }

    static void assertNoOwnedThreads() {
        assertTrue(Thread.getAllStackTraces().keySet().stream().noneMatch(t -> t.isAlive()
                && (t.getName().startsWith("seedv6-trainer-") || t.getName().startsWith("seedv6-root-"))),
                () -> Thread.getAllStackTraces().keySet().stream().map(Thread::getName).toList().toString());
    }
}
