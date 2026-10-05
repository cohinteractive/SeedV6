package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.validation.PromotionPolicy;
import java.nio.file.*;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class LineageProvenanceTest {
    @TempDir Path root;
    @ParameterizedTest @EnumSource(value = TrainingArchitecture.class, names = {"NNUE_MATERIAL", "BRN2"})
    void onlyActualInitializerRecordsImmutableProvenance(TrainingArchitecture architecture) throws Exception {
        var config = new TrainerConfig(root, 731, new TrainerConfig.SelfPlay(1, 1, 2, 0, 0, 1, 4, NnueScoreMapping.V1),
                new TrainerConfig.Training(1, 1, true), new TrainerConfig.Validation(1, 0, 0, 1, 1, 4,
                NnueScoreMapping.V1, new PromotionPolicy(1, .9, 0)), 1, TrainerConfig.DepthChange.REQUIRE_SAME,
                TrainerConfig.STANDARD_START, architecture, .001).withSource(TrainingSource.SELF_PLAY);
        var owner = new AtomicReference<TrainerService>();
        try (var service = TrainerService.freshInitialized(config, new TrainerService.Operations(), s -> {
            if (s.state() == TrainerSnapshot.State.RECOVERING && !s.latestTrainingId().isEmpty()) owner.get().stop();
        })) {
            owner.set(service); service.start(); assertTrue(service.awaitTermination(Duration.ofSeconds(90)));
            assertFalse(service.snapshot().failed(), service.snapshot().failureSummary());
            assertEquals(0, service.snapshot().totals().completedGenerations());
        }
        var provenance = LineageProvenance.read(root).orElseThrow();
        assertEquals(architecture, provenance.architecture()); assertEquals(731, provenance.firstRunSeed());
        assertEquals(architecture.nnueFamily() ? 731L : null, provenance.initializationSeed());
        byte[] evidence = Files.readAllBytes(root.resolve(LineageProvenance.FILE));
        try (var store = new CheckpointStore(root, architecture)) {
            store.writeProvenance(provenance); assertArrayEquals(evidence, Files.readAllBytes(root.resolve(LineageProvenance.FILE)));
            assertThrows(java.io.IOException.class, () -> store.writeProvenance(new LineageProvenance(provenance.initialCheckpoint(),
                    architecture, provenance.initializer(), provenance.initializationSeed(), 999, provenance.recorded())));
        }
        assertArrayEquals(evidence, Files.readAllBytes(root.resolve(LineageProvenance.FILE)));
        var view = ModelLibrary.browse(ModelLibrary.identify(root)); assertEquals(provenance, view.provenance().orElseThrow());
        assertTrue(view.exposureDescription().contains("not recorded"));
    }
    @Test void suppliedTrainerAndOldStoreDoNotAcquireInventedSeedFacts() throws Exception {
        var helper = new TrainingRunControlTest(); helper.temporary = root;
        try (var service = TrainerService.fresh(helper.config(root, 1), TrainingRecipeTest.initial(TrainingArchitecture.BRN2),
                new TrainerService.Operations(), s -> {})) { helper.finish(service); }
        assertTrue(LineageProvenance.read(root).isEmpty());
        var view = ModelLibrary.browse(ModelLibrary.identify(root)); assertTrue(view.provenance().isEmpty());
        assertTrue(view.exposureDescription().contains("across 1 completed generations"));
    }
}
