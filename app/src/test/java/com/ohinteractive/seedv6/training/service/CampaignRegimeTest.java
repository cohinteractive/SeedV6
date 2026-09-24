package com.ohinteractive.seedv6.training.service;

import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.core.brn2.Brn2Trainer;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.history.HistoryRepository;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.nnue.*;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.validation.PromotionPolicy;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(180)
class CampaignRegimeTest {
    @TempDir Path temp;
    static TrainerConfig config(Path root, TrainingSource source, ValidationMethod method) {
        return new TrainerConfig(root, 71,
                new TrainerConfig.SelfPlay(1, 1, 4, 0, 0, 2, 4, NnueScoreMapping.V1),
                new TrainerConfig.Training(1, 1, true),
                new TrainerConfig.Validation(2, 0, 0, 1, 1, 4, NnueScoreMapping.V1, new PromotionPolicy(1, .9, 0)),
                1, TrainerConfig.DepthChange.REQUIRE_SAME, Brn2TrainerServiceTest.MATE,
                TrainingArchitecture.BRN2, .001, source).withValidationMethod(method);
    }
    static TrainerSnapshot finish(TrainerService service) throws Exception {
        service.start(); assertTrue(service.awaitTermination(Duration.ofSeconds(120)));
        assertFalse(service.snapshot().failed(), service.snapshot().failureSummary()); return service.snapshot();
    }
    @Test void oneLineageComposesAllSixRegimesAndPreservesHistoricalIdentity() throws Exception {
        Path generator = temp.resolve("generator"), root = temp.resolve("student");
        try (var store = new CheckpointStore(generator)) {
            store.initialize(new NnueTrainer(TrainableNnue.initialized(17)), new CheckpointManifest.Metadata(0, 1, ""));
        }
        var sources = List.of(TrainingSource.SELF_PLAY, TrainingSource.HANDCRAFTED, TrainingSource.bootstrap(generator),
                TrainingSource.SELF_PLAY, TrainingSource.HANDCRAFTED, TrainingSource.bootstrap(generator), TrainingSource.SELF_PLAY, TrainingSource.SELF_PLAY);
        var methods = List.of(ValidationMethod.GAME_PAIRS, ValidationMethod.HELD_OUT, ValidationMethod.GAME_PAIRS,
                ValidationMethod.HELD_OUT, ValidationMethod.GAME_PAIRS, ValidationMethod.HELD_OUT, ValidationMethod.GAME_PAIRS, ValidationMethod.GAME_PAIRS);
        String parent = null;
        for (int i = 0; i < sources.size(); i++) {
            var cfg = config(root, sources.get(i), methods.get(i));
            assertEquals(methods.get(i), cfg.withSource(TrainingSource.SELF_PLAY).validationMethod());
            assertEquals(sources.get(i), cfg.withValidationMethod(ValidationMethod.HELD_OUT).source());
            try (var service = i == 0 ? TrainerService.fresh(cfg, new Brn2Trainer(.001)) : TrainerService.resume(cfg)) {
                var end = finish(service); assertEquals(i + 1, end.generation());
                var manifest = CheckpointStore.readSnapshot(root, end.latestTrainingId()).manifest();
                if (parent != null) assertEquals(parent, manifest.parentId());
                parent = manifest.id();
                assertEquals(methods.get(i) == ValidationMethod.HELD_OUT, end.bootstrapValidation().isPresent());
                if (methods.get(i) == ValidationMethod.GAME_PAIRS)
                    assertEquals(end.selfPlay().sampledPositions(), end.generationSamplesTrained(), "Game-pair WDL training uses the full batch");
            }
            assertFalse(CheckpointInspection.freshRoot(root, TrainingArchitecture.BRN2));
            assertEquals(sources.get(i), CheckpointStore.readTrainingSource(root).orElseThrow());
        }
        var history = new HistoryRepository(root).refresh(); assertTrue(history.warnings().isEmpty());
        assertEquals(8, history.records().size());
        assertEquals(history.records().get(6).regime(), history.records().get(7).regime(), "Generation seeds must not split a campaign history span");
        for (int i = 0; i < 8; i++) {
            var regime = history.records().get(i).regime();
            assertEquals(sources.get(i).mode().name(), regime.positionSource());
            assertEquals(methods.get(i).name(), regime.validationMethod());
            assertNotNull(regime.effectiveSettings());
        }
        assertThrows(java.io.IOException.class, () -> CheckpointInspection.freshRoot(root, TrainingArchitecture.NNUE));
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = TrainingSource.Mode.class, names = {"SELF_PLAY", "HANDCRAFTED"})
    void changedValidatorAndSourceArchivePublishedUnfinishedWorkAndRestartFromSettledParent(TrainingSource.Mode mode) throws Exception {
        Path root = temp.resolve("restart"); var original = config(root, TrainingSource.SELF_PLAY, ValidationMethod.GAME_PAIRS);
        var owned = new AtomicReference<TrainerService>();
        String candidate, parent;
        try (var service = TrainerService.fresh(original, new NetworkTrainingState.Brn2(new Brn2Trainer(.001)),
                new TrainerService.Operations(), s -> { if (s.state() == TrainerSnapshot.State.PUBLISHING_CANDIDATE) owned.get().stop(); })) {
            owned.set(service); var stopped = finish(service); candidate = stopped.candidateId();
            assertFalse(candidate.isEmpty()); parent = GenerationAttempt.inspect(root).orElseThrow().parentId();
        }
        var changed = config(root, new TrainingSource(mode, ""), ValidationMethod.HELD_OUT);
        assertFalse(GenerationAttempt.inspect(root).orElseThrow().matches(changed, changed.source()));
        try (var service = TrainerService.resume(changed)) {
            var end = finish(service); assertEquals(1, end.generation());
            assertTrue(service.lifecycleNotice().contains("Restarted unfinished generation"));
            assertEquals(parent, CheckpointStore.readSnapshot(root, end.candidateId()).manifest().parentId());
        }
        try (var paths = Files.walk(root.resolve("restarted-generations"))) {
            assertTrue(paths.anyMatch(p -> p.getFileName().toString().equals(candidate)));
        }
    }
    @Test void nnueSelfPlaySupportsHeldOutWithoutChangingItsTrainingObjective() throws Exception {
        Path root = temp.resolve("nnue-held-out"); var brn = config(root, TrainingSource.SELF_PLAY, ValidationMethod.HELD_OUT);
        var cfg = new TrainerConfig(root, 71, brn.selfPlay(), brn.training(), brn.validation(), 1,
                brn.depthChange(), brn.startingFen()).withSource(TrainingSource.SELF_PLAY).withValidationMethod(ValidationMethod.HELD_OUT);
        try (var service = TrainerService.fresh(cfg, new NnueTrainer(TrainableNnue.initialized(41)))) {
            var end = finish(service); assertTrue(end.bootstrapValidation().isPresent());
            assertEquals(TrainingArchitecture.NNUE, CheckpointStore.readBestSnapshot(root).manifest().architecture());
        }
        assertEquals("HELD_OUT", new HistoryRepository(root).refresh().records().getFirst().regime().validationMethod());
    }
    @Test void legacyAttemptKeepsExactContinuationOnlyForItsRecordedRegime() {
        var cfg = config(temp, TrainingSource.SELF_PLAY, ValidationMethod.GAME_PAIRS);
        String id = "g000000-s000000000-" + "a".repeat(64);
        var legacy = new GenerationAttempt(id, id, 1, TrainingSource.SELF_PLAY,
                cfg.legacyAttemptSettings(1, TrainingSource.SELF_PLAY), GenerationAttempt.Format.CURRENT, "");
        assertTrue(legacy.matches(cfg, TrainingSource.SELF_PLAY));
        assertFalse(legacy.matches(cfg.withValidationMethod(ValidationMethod.HELD_OUT), TrainingSource.SELF_PLAY));
    }
}
