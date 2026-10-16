package com.ohinteractive.seedv6.training.service;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.core.brn2.Brn2Trainer;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.nnue.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.validation.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120) @TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BrnRunSeedsTest {
    @TempDir static Path temp;
    Path generator;
    @BeforeAll void teacher() throws Exception {
        generator = temp.resolve("teacher");
        try (var store = new CheckpointStore(generator, TrainingArchitecture.NNUE)) {
            store.initialize(new NnueTrainer(TrainableNnue.initialized(17)), new CheckpointManifest.Metadata(0, 2, ""));
        }
    }
    TrainerConfig config(Path root) {
        return new TrainerConfig(root, 1, new TrainerConfig.SelfPlay(2, 1, 8, 0, 1, 8, 128, NnueScoreMapping.V1),
                new TrainerConfig.Training(1, 1, true), new TrainerConfig.Validation(2, 0, 0, 2, 1, 128,
                NnueScoreMapping.V1, new PromotionPolicy(1, .9, 0)), 1, TrainerConfig.DepthChange.REQUIRE_SAME,
                BrnBootstrapTest.MATE, TrainingArchitecture.BRN2, .001, TrainingSource.bootstrap(generator))
                .withSupervision(BrnSupervision.blended(.75));
    }
    TrainerSnapshot complete(TrainerService service) throws Exception { return BrnBootstrapTest.finish(service); }
    BootstrapPlan plan(Path root) throws Exception {
        var line = CheckpointInspection.lineage(root, CheckpointInspection.reference(root, "latest-training"));
        return CheckpointInspection.bootstrapPlan(root, line.getFirst().id());
    }
    String samples(Path root) throws Exception {
        var p = plan(root); var d = CheckpointInspection.bootstrapData(root, p);
        return new BootstrapData("0".repeat(64), d.partition(), d.statistics(), 0).hash();
    }
    @Test void defaultSeedsStayExactAndOnlySelfPlayChanges() {
        var old = config(temp.resolve("default")); var independent = old.withRunSeeds(new BrnRunSeeds(1, 2));
        assertEquals(7921502845091313457L, old.selfPlay(1).seed());
        assertEquals(-6540313355536843707L, old.training(1).shuffleSeed());
        assertEquals(8110949293515089404L, old.seed(1, TrainerConfig.SeedDomain.HOLDOUT));
        assertFalse(old.generationSettings(1).contains("brn-run-seeds"));
        for (long g : new long[]{1, 2, 128}) {
            assertNotEquals(old.selfPlay(g).seed(), independent.selfPlay(g).seed());
            for (var domain : TrainerConfig.SeedDomain.values()) if (domain != TrainerConfig.SeedDomain.SELF_PLAY)
                assertEquals(old.seed(g, domain), independent.seed(g, domain));
        }
        assertEquals(old.selfPlay(1), old.withRunSeeds(new BrnRunSeeds(1, 1)).selfPlay(1));
        var nnue = new TrainerConfig(temp.resolve("nnue"), 1, old.selfPlay(), old.training(), old.validation(), 1, old.depthChange());
        assertEquals(old.selfPlay(1), nnue.selfPlay(1));
        assertThrows(IllegalArgumentException.class, () -> nnue.withRunSeeds(new BrnRunSeeds(1, 2)));
    }
    @Test void realDataSameSeedReproducesAndDifferentSeedChangesSamples() throws Exception {
        Path a = temp.resolve("a"), b = temp.resolve("b"), c = temp.resolve("c");
        for (Path root : List.of(a,b,c)) try (var service = TrainerService.fresh(config(root)
                .withRunSeeds(new BrnRunSeeds(1, root.equals(c) ? 3 : 2)), new Brn2Trainer(.001))) {
            assertEquals(1, complete(service).totals().completedGenerations());
        }
        assertEquals(plan(a), plan(b)); assertEquals(samples(a), samples(b));
        assertEquals(CheckpointInspection.reference(a, "latest-training"), CheckpointInspection.reference(b, "latest-training"));
        assertNotEquals(plan(a).hash(), plan(c).hash()); assertNotEquals(samples(a), samples(c));
        assertEquals(new BrnRunSeeds(1,2), CheckpointStore.readBrnRunSeeds(a).orElseThrow());
    }
    @Test void partialUpdateResumeRestoresSeedsRejectsEditsAndReusesExactData() throws Exception {
        Path whole = temp.resolve("whole"), split = temp.resolve("split");
        TrainerSnapshot expected;
        try (var service = TrainerService.fresh(config(whole).withRunSeeds(new BrnRunSeeds(1,2)), new Brn2Trainer(.001))) {
            expected = complete(service);
        }
        var owner = new AtomicReference<TrainerService>();
        var stopping = new TrainerService.Operations() {
            @Override Optional<SelfPlayTraining.Statistics> trainBootstrap(NetworkTrainingState state,
                    List<TrajectorySampler.Sample> samples, SelfPlayTraining.Config cfg, SelfPlayControl control,
                    Consumer<SelfPlayTraining.Progress> observer, BrnSupervision supervision,
                    ToDoubleFunction<TrajectorySampler.Sample> target) {
                return super.trainBootstrap(state, samples, cfg, control, p -> { observer.accept(p); owner.get().stop(); }, supervision, target);
            }
        };
        try (var service = TrainerService.fresh(config(split).withRunSeeds(new BrnRunSeeds(1,2)),
                new NetworkTrainingState.Brn2(new Brn2Trainer(.001)), stopping, s -> {})) {
            owner.set(service); assertEquals(1, complete(service).optimizerStep());
        }
        byte[] attempt = Files.readAllBytes(split.resolve(GenerationAttempt.FILE));
        for (var changed : List.of(new BrnRunSeeds(1,3),new BrnRunSeeds(2,2))) {
            try (var service = TrainerService.resume(config(split).withRunSeeds(changed))) {
                service.start(); assertTrue(service.awaitTermination(java.time.Duration.ofSeconds(30)));
                assertEquals(TrainerSnapshot.State.FAILED, service.snapshot().state());
                assertTrue(service.snapshot().failureSummary().contains("Run seeds differ"));
            }
            assertArrayEquals(attempt, Files.readAllBytes(split.resolve(GenerationAttempt.FILE)));
            assertFalse(Files.exists(split.resolve("restarted-generations")));
        }
        var reuse = new TrainerService.Operations() {
            @Override SelfPlayBatch generate(NetworkModel actor, SelfPlayConfig cfg, long[] board, SelfPlayControl c, Consumer<SelfPlayBatch.Progress> o) {
                throw new AssertionError("Resume must reuse persisted data");
            }
        };
        var stale = config(split).withRunSeeds(new BrnRunSeeds(999,888)).withRunSeeds(null).withSource(null).withSupervision(null);
        try (var service = TrainerService.resume(stale, reuse, s -> {})) {
            assertEquals(expected.latestTrainingId(), complete(service).latestTrainingId());
            assertEquals(new BrnRunSeeds(1,2), service.config().runSeeds()); assertEquals(1, service.config().masterSeed());
        }
        assertArrayEquals(BrnBootstrapTest.state(whole, expected.latestTrainingId()), BrnBootstrapTest.state(split, expected.latestTrainingId()));
        var pin = plan(split); byte[] seedBytes = Files.readAllBytes(split.resolve(CheckpointStore.BRN_RUN_SEEDS_FILE));
        Files.delete(split.resolve(CheckpointStore.BRN_RUN_SEEDS_FILE));
        assertThrows(java.io.IOException.class, () -> CheckpointStore.readBrnRunSeeds(split));
        Files.write(split.resolve(CheckpointStore.BRN_RUN_SEEDS_FILE), seedBytes);
        seedBytes[seedBytes.length-1] ^= 1; Files.write(split.resolve(CheckpointStore.BRN_RUN_SEEDS_FILE), seedBytes);
        assertThrows(java.io.IOException.class, () -> CheckpointStore.readBrnRunSeeds(split));
    }
    @Test void legacyStoreRemainsUnmodifiedAndCannotAdoptNewSeedMetadata() throws Exception {
        Path root = temp.resolve("legacy");
        // A genuine legacy fixture predates automatic effective-seed persistence for new stores.
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN2)) {
            store.initializeBrnSupervision(BrnSupervision.blended(.75));
            store.writeTrainingSource(TrainingSource.bootstrap(generator));
            store.initialize(new NetworkTrainingState.Brn2(new Brn2Trainer(.001)), new CheckpointManifest.Metadata(0, 2, ""));
        }
        try (var service = TrainerService.resume(config(root))) { complete(service); }
        assertTrue(CheckpointStore.readBrnRunSeeds(root).isEmpty());
        byte[] attempt = Files.readAllBytes(root.resolve(GenerationAttempt.FILE));
        try (var service = TrainerService.resume(config(root).withRunSeeds(new BrnRunSeeds(1,2)))) {
            service.start(); assertTrue(service.awaitTermination(java.time.Duration.ofSeconds(30)));
            assertEquals(TrainerSnapshot.State.FAILED, service.snapshot().state());
        }
        assertArrayEquals(attempt, Files.readAllBytes(root.resolve(GenerationAttempt.FILE)));
        assertFalse(Files.exists(root.resolve(CheckpointStore.BRN_RUN_SEEDS_FILE)));
    }
    @Test void interruptedInitializationKeepsSeedIdentity() throws Exception {
        Path root = temp.resolve("initialization");
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN2)) {
            store.initializeBrnRunSeeds(new BrnRunSeeds(Long.MIN_VALUE, Long.MAX_VALUE));
        }
        assertTrue(CheckpointInspection.freshRoot(root, TrainingArchitecture.BRN2));
        try (var service = TrainerService.fresh(config(root), new Brn2Trainer(.001))) {
            complete(service); assertEquals(new BrnRunSeeds(Long.MIN_VALUE,Long.MAX_VALUE), service.config().runSeeds());
        }
    }
}
