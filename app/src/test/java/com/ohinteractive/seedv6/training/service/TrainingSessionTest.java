package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.brn2.Brn2Trainer;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.model.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class TrainingSessionTest {
    @TempDir Path temporary;
    TrainerConfig config(Path root, int target) { return new TrainingRunControlTest().config(root, target); }
    void lineage(Path root, long generation) throws Exception {
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN2)) {
            store.writeLineage(new TrainingLineage(UUID.randomUUID(), "l1", TrainingArchitecture.BRN2, Instant.now(), "", "fixture"));
            store.initializeBrnRunSeeds(new BrnRunSeeds(1, 2));
            store.initialize(new NetworkTrainingState.Brn2(new Brn2Trainer(.001)), new CheckpointManifest.Metadata(generation, 1, ""));
        }
    }
    TrainerSnapshot run(TrainerService service) throws Exception {
        service.withSession().start(); assertTrue(service.awaitTermination(Duration.ofSeconds(90)));
        assertTrue(service.failure().isEmpty(), () -> service.failure().toString()); return service.snapshot();
    }
    @Test void sixteenAdditionalCyclesFromTenSettleAtTwentySixIncludingRejectedGenerations() throws Exception {
        Path root = temporary.resolve("sixteen"); lineage(root, 10);
        try (var service = TrainerService.resume(config(root, 16))) {
            var end = run(service); assertEquals(26, end.generation());
            var session = end.run().orElseThrow().session(); assertNotNull(session);
            assertEquals(10, session.baseline()); assertEquals(16, session.completed()); assertEquals(0, session.remaining());
            assertEquals(TrainingSession.State.COMPLETED, session.state());
            assertEquals(16, CheckpointInspection.validations(root).size());
            assertEquals(16, end.totals().retainedCandidates() + end.totals().incompleteValidations() + end.totals().promotions());
            assertTrue(service.lifecycleNotice().contains("Session complete"));
        }
        assertEquals(26, GenerationAttempt.inspect(root).orElseThrow().generation());
        var id = TrainingSession.read(root).orElseThrow().id();
        try (var service = TrainerService.resume(config(root, 1))) { assertEquals(27, run(service).generation()); }
        assertNotEquals(id, TrainingSession.read(root).orElseThrow().id());
    }
    @Test void crashRecoversRemainingBudgetAndReconcilesDecisionBeforeSessionWrite() throws Exception {
        Path root = temporary.resolve("crash"); lineage(root, 0);
        try (var service = TrainerService.resume(config(root, 3), new TrainerService.Operations(), snapshot -> {
            if (snapshot.state() == TrainerSnapshot.State.RECORDING_DECISION && snapshot.generation() == 1)
                throw new IllegalStateException("Injected process boundary");
        })) {
            service.withSession().start(); assertTrue(service.awaitTermination(Duration.ofSeconds(30)));
            assertTrue(service.snapshot().failed());
        }
        var original = TrainingSession.read(root).orElseThrow();
        assertEquals(TrainingSession.State.ACTIVE, original.state());
        // Simulate a crash after the settled decision but before its session progress write.
        new TrainingSession(1, original.id(), original.lineageId(), 0, 3, 0, TrainingSession.State.ACTIVE).save(root);
        try (var service = TrainerService.resume(config(root, 99))) {
            var end = run(service); assertEquals(3, end.generation());
            assertEquals(2, end.totals().completedGenerations());
            assertEquals(original.id(), end.run().orElseThrow().session().id());
            assertEquals(3, end.run().orElseThrow().session().completed());
        }
    }
    @Test void explicitStopEndsBudgetButPreservesPartialGenerationForNewStart() throws Exception {
        Path root = temporary.resolve("stopped"); lineage(root, 0);
        var owner = new AtomicReference<TrainerService>();
        try (var service = TrainerService.resume(config(root, 16).withValidationMethod(ValidationMethod.GAME_PAIRS), new TrainerService.Operations(), snapshot -> {
            if (snapshot.state() == TrainerSnapshot.State.VALIDATING) owner.get().stop();
        })) {
            owner.set(service); run(service);
            assertEquals(0, TrainingSession.read(root).orElseThrow().completed());
            assertEquals(TrainingSession.State.STOPPED, TrainingSession.read(root).orElseThrow().state());
            assertTrue(PartialGeneration.inspect(root).isPresent());
        }
        var id = TrainingSession.read(root).orElseThrow().id();
        try (var service = TrainerService.resume(config(root, 2).withValidationMethod(ValidationMethod.GAME_PAIRS))) { assertEquals(2, run(service).generation()); }
        var next = TrainingSession.read(root).orElseThrow(); assertNotEquals(id, next.id()); assertEquals(2, next.completed());
    }
    @Test void stopBeforeGenerationAdmissionEndsRecoveredBudgetWithoutStartingWork() throws Exception {
        Path root = temporary.resolve("early-stop"); lineage(root, 0);
        TrainingSession previous;
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN2)) { previous = TrainingSession.open(store, 16); }
        var owner = new AtomicReference<TrainerService>();
        try (var service = TrainerService.resume(config(root, 99), new TrainerService.Operations(), snapshot -> {
            if (snapshot.state() == TrainerSnapshot.State.RECOVERING) owner.get().stop();
        })) {
            owner.set(service); run(service);
        }
        var stopped = TrainingSession.read(root).orElseThrow();
        assertEquals(previous.id(), stopped.id()); assertEquals(16, stopped.target());
        assertEquals(0, stopped.completed()); assertEquals(TrainingSession.State.STOPPED, stopped.state());
        assertTrue(GenerationAttempt.inspect(root).isEmpty());
    }
    @Test void foreignOrRegressedSessionCannotResetHistory() throws Exception {
        Path root = temporary.resolve("foreign"); lineage(root, 0);
        new TrainingSession(1, UUID.randomUUID(), UUID.randomUUID(), 0, 16, 0, TrainingSession.State.ACTIVE).save(root);
        try (var service = TrainerService.resume(config(root, 16))) {
            service.withSession().start(); assertTrue(service.awaitTermination(Duration.ofSeconds(30)));
            assertTrue(service.snapshot().failed()); assertTrue(service.snapshot().failureSummary().contains("another lineage"));
        }
        var s = new TrainingSession(1, UUID.randomUUID(), UUID.randomUUID(), 10, 16, 2, TrainingSession.State.ACTIVE);
        assertThrows(java.io.IOException.class, () -> s.reconcile(11));
        assertThrows(java.io.IOException.class, () -> s.reconcile(27));
    }
}
