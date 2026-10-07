package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.training.model.TrainingArchitecture;
import com.ohinteractive.seedv6.training.telemetry.OptimizationSnapshot;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(90)
class ArenaOptimizationProgressTest {
    @TempDir Path temporary;
    @Test void progressTracksActualOptimizerLossAndResumedSegmentWithoutChangingResume() throws Exception {
        var fixture = new LearningArenaTest(); fixture.temporary = temporary;
        var config = fixture.config(TrainingArchitecture.BRN3, TrainingArchitecture.BRN3, 1, LearningArenaConfig.Limit.DEPTH);
        var values = new ArrayList<OptimizationSnapshot>(); var owner = new AtomicReference<LearningArenaService>();
        Path root = temporary.resolve("progress");
        long pausedNanos;
        try (var service = LearningArenaService.create(root, config, update -> {
            var p = update.optimization();
            if (p != null) { values.add(p); if (p.samples() == 1) owner.get().pause(); }
        })) {
            owner.set(service); service.run(); assertEquals(LearningArenaState.Status.PAUSED, service.state().status());
        }
        assertFalse(values.isEmpty()); var paused = values.getLast();
        assertEquals(1, paused.samples()); assertEquals(4, paused.targetSamples()); assertEquals(.003, paused.learningRate());
        assertEquals(1, paused.updates()); assertEquals(0, paused.initialStep()); assertEquals(1, paused.step());
        assertEquals(1, paused.totalExposure()); assertEquals(1, paused.invocationSamples()); assertTrue(Double.isFinite(paused.meanLoss()));
        var saved = com.ohinteractive.seedv6.training.data.DataFiles.read(root.resolve("rounds/000001/A/progress.json"), LearningArenaTraining.Progress.class);
        pausedNanos = saved.elapsedNanos(); assertTrue(pausedNanos > 0);
        values.clear();
        try (var service = LearningArenaService.resume(root, update -> { if (update.optimization() != null) values.add(update.optimization()); })) {
            service.run(); assertEquals(LearningArenaState.Status.COMPLETE, service.state().status());
        }
        var first = values.getFirst(); assertEquals(2, first.samples()); assertEquals(1, first.invocationSamples());
        assertEquals(2, first.updates()); assertEquals(2, first.step()); assertTrue(first.elapsedNanos() > 0);
        assertEquals(4, values.getLast().samples()); assertEquals(4, values.getLast().totalExposure());
        var state = LearningArenaState.read(root);
        assertNull(state.history().getFirst().a().training());
        var a = state.current().a().training(); var b = state.current().b().training();
        assertEquals(4, a.sampleVisits()); assertTrue(a.elapsedNanos() >= pausedNanos); assertEquals(1, a.minibatch()); assertEquals(2, b.minibatch());
        assertEquals(values.stream().filter(p -> p.identity().startsWith("A -") && p.samples() == 4).findFirst().orElseThrow().meanLoss(), a.finalMeanLoss());
        assertEquals(values.getLast().meanLoss(), b.finalMeanLoss());
        assertEquals(4e9 / a.elapsedNanos(), a.averageSampleVisitsPerSecond());
        assertEquals(4e9 / b.elapsedNanos(), b.averageSampleVisitsPerSecond());
    }
    @Test void legacyPartialProgressDoesNotInventTotalTrainingDuration() throws Exception {
        var fixture = new LearningArenaTest(); fixture.temporary = temporary;
        var config = fixture.config(TrainingArchitecture.BRN3, TrainingArchitecture.BRN3, 1, LearningArenaConfig.Limit.DEPTH);
        var owner = new AtomicReference<LearningArenaService>(); Path root = temporary.resolve("legacy");
        try (var service = LearningArenaService.create(root, config, u -> {
            if (u.optimization() != null && u.optimization().samples() == 1) owner.get().pause();
        })) { owner.set(service); service.run(); }
        var path = root.resolve("rounds/000001/A/progress.json");
        var json = com.ohinteractive.seedv6.training.data.DataFiles.JSON.fromJson(java.nio.file.Files.readString(path), com.google.gson.JsonObject.class);
        json.remove("elapsedNanos"); com.ohinteractive.seedv6.training.data.DataFiles.write(path, json);
        try (var service = LearningArenaService.resume(root, u -> {})) { service.run(); }
        var metrics = LearningArenaState.read(root).current().a().training();
        assertEquals(4, metrics.sampleVisits()); assertNull(metrics.elapsedNanos()); assertNull(metrics.averageSampleVisitsPerSecond());
        assertNotNull(metrics.finalMeanLoss());
    }
}
