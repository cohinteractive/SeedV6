package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.checkpoint.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ArenaRecipeConfigurationTest {
    @TempDir Path temporary;
    static LearningArenaConfig legacy() {
        return new LearningArenaConfig("Legacy fixture", new LearningArenaConfig.Competitor("One", TrainingArchitecture.NNUE, 71, 32),
                new LearningArenaConfig.Competitor("Two", TrainingArchitecture.BRN3, 97, 128),
                new DataSource("Shared", DataSource.Format.LICHESS_JSONL, "/fixture.jsonl", "a".repeat(64), 1), 128, 8, 3, 71,
                new LearningArenaConfig.Arena(8, LearningArenaConfig.Limit.DEPTH, 4, 1000, 1, 0, 8, 1024, TrainerConfig.STANDARD_START));
    }
    @Test void historicalCampaignBindingRemainsByteCompatible() {
        assertEquals("6f10a2b78b6c3092292283cf4e3b7aa334bd898796653de4afc3e298f90d8262", legacy().identity());
        assertEquals(legacy(), DataFiles.JSON.fromJson(DataFiles.JSON.toJson(legacy()), LearningArenaConfig.class));
    }
    @Test void exactStartingModelCopiesOptimizerStateAndOptionalRateWithoutWritingItsSource() throws Exception {
        Path root = temporary.resolve("source"); var architecture = TrainingArchitecture.BRN3;
        var trainer = TrainingRecipeTest.initial(architecture); TrainingRecipeTest.trainOne(trainer);
        trainer.setLearningRate(.006); CheckpointStore.Checkpoint source;
        var lineage = new TrainingLineage(UUID.randomUUID(), "Interesting model", architecture, java.time.Instant.now(), "", "fixture");
        try (var store = new CheckpointStore(root, architecture)) { store.writeLineage(lineage); source = store.publish(trainer, new CheckpointManifest.Metadata(3, 1, "")); }
        var binding = ModelLibrary.resolve(ModelLibrary.identify(root), source.manifest().id()).binding();
        var selected = LearningArenaConfig.InitialModel.from(binding);
        var inherited = new LearningArenaConfig.Competitor("Experiment", architecture, 999, 2, null, selected);
        String expected = TrainingRecipeTest.digest(trainer);
        assertEquals(expected, TrainingRecipeTest.digest(LearningArenaTraining.initial(inherited)));
        var override = new LearningArenaConfig.Competitor("Experiment", architecture, 999, 2, .009, selected);
        var copied = LearningArenaTraining.initial(override); assertEquals(.009, copied.hyperparameters().learningRate()); assertEquals(trainer.step(), copied.step());
        copied.setLearningRate(.006); assertEquals(expected, TrainingRecipeTest.digest(copied));
        assertEquals(expected, TrainingRecipeTest.digest(CheckpointStore.readTrainingSnapshot(root, source.manifest().id())));
        assertEquals(lineage, TrainingLineage.read(root).orElseThrow()); assertFalse(Files.exists(root.resolve("refs/best")));
        var wrong = new LearningArenaConfig.InitialModel(selected.root(), UUID.randomUUID().toString(), selected.name(), selected.checkpoint(), selected.generation());
        assertThrows(java.io.IOException.class, () -> LearningArenaTraining.initial(new LearningArenaConfig.Competitor("Wrong", architecture, 1, 2, null, wrong)));
        assertNotEquals(legacy().identity(), new LearningArenaConfig(legacy().name(), override, legacy().b(), legacy().source(), legacy().positionsPerRound(), legacy().epochs(), legacy().rounds(), legacy().seed(), legacy().arena()).identity());
    }
    @Test void distinctRatesProduceDistinctModelsAndRetainNamedLineagesAndResumableCampaigns() throws Exception {
        var fixture = new LearningArenaTest(); fixture.temporary = temporary;
        var legacy = fixture.config(TrainingArchitecture.BRN3, TrainingArchitecture.BRN3, 1, LearningArenaConfig.Limit.DEPTH);
        var a = new LearningArenaConfig.Competitor("Faster learner", TrainingArchitecture.BRN3, 71, 1, .007, null);
        var b = new LearningArenaConfig.Competitor("Slower learner", TrainingArchitecture.BRN3, 71, 1, .001, null);
        var config = new LearningArenaConfig("Rates", a, b, legacy.source(), 2, 2, 1, 71, legacy.arena());
        Path root = temporary.resolve("campaign");
        try (var owner = LearningArenaService.create(root, config, u -> {})) { owner.run(); }
        var state = LearningArenaState.read(root); assertEquals(config, state.config());
        var first = CheckpointStore.readSnapshot(root.resolve("A"), state.current().a().checkpoint());
        var second = CheckpointStore.readSnapshot(root.resolve("B"), state.current().b().checkpoint());
        assertNotEquals(first.manifest().networkSha256(), second.manifest().networkSha256());
        assertEquals(.007, CheckpointStore.readTrainingSnapshot(root.resolve("A"), first.manifest().id()).hyperparameters().learningRate());
        assertEquals(.001, CheckpointStore.readTrainingSnapshot(root.resolve("B"), second.manifest().id()).hyperparameters().learningRate());
        assertEquals("Faster learner", ModelLibrary.identify(root.resolve("A")).name());
        assertNotEquals(TrainingLineage.read(root.resolve("A")).orElseThrow().id(), TrainingLineage.read(root.resolve("B")).orElseThrow().id());
        assertEquals(71, LineageProvenance.read(root.resolve("A")).orElseThrow().initializationSeed());
        String receipt = Files.readString(root.resolve("campaign.json"));
        try (var owner = LearningArenaService.resume(root, u -> {})) { owner.run(); }
        assertEquals(receipt, Files.readString(root.resolve("campaign.json")));
    }
}
