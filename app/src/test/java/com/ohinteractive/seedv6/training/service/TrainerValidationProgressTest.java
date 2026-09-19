package com.ohinteractive.seedv6.training.service;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.ohinteractive.seedv6.training.selfplay.GameTermination;
import static com.ohinteractive.seedv6.training.service.TrainerServiceTest.*;
import static com.ohinteractive.seedv6.training.service.TrainerSnapshot.State.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("slow-nnue")
@Timeout(120)
class TrainerValidationProgressTest {
    @TempDir Path root;

    @ParameterizedTest @ValueSource(ints = {1, 8})
    void realServicePublishesLiveImmutableProgressAndResetsForNextGeneration(int validationCap) throws Exception {
        List<TrainerSnapshot> seen = new ArrayList<>(), phases = new ArrayList<>();
        AtomicReference<TrainerService> owned = new AtomicReference<>();
        var operations = ValidationTelemetryFixture.operations(p -> seen.add(owned.get().snapshot()));
        var base = config(root, 2);
        var config = new TrainerConfig(base.checkpointRoot(), base.masterSeed(), base.selfPlay(), base.training(),
                new TrainerConfig.Validation(2, 0, 0, 1, 1, validationCap, MAPPING, GATE),
                2, base.depthChange(), START);
        int expectedValid = validationCap == 1 ? 0 : 2;
        try (var service = TrainerService.fresh(config, trainer(), operations, phases::add)) {
            owned.set(service);
            var end = finish(service);
            assertTrue(seen.stream().allMatch(s -> s.state() == VALIDATING));
            for (long generation = 1; generation <= 2; generation++) {
                long g = generation;
                var snapshots = seen.stream().filter(s -> s.generation() == g).toList();
                var details = snapshots.getFirst().validationDetails().orElseThrow();
                assertEquals(config.validation(g), details.config());
                assertEquals(GATE, details.policy());
                assertEquals(snapshots.getFirst().candidateId(), details.candidateId());
                assertEquals(snapshots.getFirst().bestId(), details.bestId());
                assertEquals(g, details.candidateGeneration().orElseThrow());
                assertEquals(0, details.bestGeneration().orElseThrow());
                assertTrue(snapshots.stream().allMatch(s -> s.validationDetails().orElseThrow().equals(details)));
                var first = snapshots.getFirst().validationProgress().orElseThrow();
                assertEquals(0, first.currentPair()); assertEquals(0, first.gameInPair()); assertFalse(first.gameActive());
                assertTrue(snapshots.stream().anyMatch(s -> s.validationProgress().orElseThrow().gameActive()));
                assertTrue(snapshots.stream().anyMatch(s -> s.validationProgress().orElseThrow().currentGamePlies() == 1));
                assertTrue(snapshots.stream().anyMatch(s -> s.validationProgress().orElseThrow().gameInPair() == 2));
                assertEquals(expectedValid, snapshots.getLast().validationProgress().orElseThrow().validPairs());
                assertEquals(2 - expectedValid, snapshots.getLast().validationProgress().orElseThrow().incompletePairs());
                assertTrue(snapshots.stream().anyMatch(s -> {
                    var p = s.validationProgress().orElseThrow();
                    return !p.complete() && (validationCap == 1 ? p.incompletePairs() == 1 : p.validPairs() == 1);
                }), "Pair counts advance while VALIDATING, before the final decision");
                assertEquals(0, first.validPairs(), "Old progress is immutable");
            }
            var nextGeneration = phases.stream().filter(s -> s.state() == GENERATING_SELF_PLAY && s.generation() == 2).findFirst().orElseThrow();
            var previous = nextGeneration.validationProgress().orElseThrow();
            assertTrue(previous.complete()); assertFalse(previous.gameActive());
            assertEquals(0, previous.currentPair()); assertEquals(0, previous.gameInPair());
            assertEquals(0, previous.gameOrdinal()); assertEquals(0, previous.currentGamePlies());
            assertEquals(config.validation(1), nextGeneration.validationDetails().orElseThrow().config());
            assertEquals(expectedValid, previous.validPairs()); assertTrue(nextGeneration.assessment().isPresent());
            assertTrue(phases.stream().filter(s -> s.state() == RECORDING_DECISION).allMatch(s -> s.assessment().isPresent()));
            var progress = end.validationProgress().orElseThrow();
            assertTrue(progress.complete()); assertFalse(progress.gameActive());
            assertEquals(end.validation().orElseThrow().draws(), progress.draws());
            assertEquals(end.validation().orElseThrow().terminations(), progress.terminations());
            assertTrue(end.assessment().isPresent());
            assertSame(progress, service.snapshot().validationProgress().orElseThrow(), "Polling reuses immutable telemetry");
            assertThrows(UnsupportedOperationException.class, () -> progress.terminations().clear());
        }
    }

    @Test void stopRetainsFinalPartialProgressAndCancelledSlotsWithoutEvidenceLeakage() throws Exception {
        var pause = new TrainerControlTest.Pause();
        try (var service = ValidationTelemetryFixture.fresh(config(root, 1), trainer(), p -> {
            if (p.gameActive() && p.gameOrdinal() == 1 && p.currentGamePlies() == 1) pause.block();
        })) {
            try {
                service.start(); pause.await();
                var before = service.snapshot();
                assertEquals(VALIDATING, before.state());
                assertEquals(1, before.validationProgress().orElseThrow().currentGamePlies());
                service.stop();
                assertEquals(STOPPING, service.snapshot().state());
                assertSame(before.validationProgress().orElseThrow(), service.snapshot().validationProgress().orElseThrow());
            } finally { pause.close(); }
            assertTrue(service.awaitTermination(WAIT));
            var end = service.snapshot(); assertEquals(STOPPED, end.state(), end.failureSummary());
            var progress = end.validationProgress().orElseThrow();
            assertTrue(progress.complete()); assertFalse(progress.gameActive());
            assertEquals(2, progress.incompletePairs()); assertEquals(0, progress.validPairs());
            assertEquals(0, progress.wins() + progress.draws() + progress.losses());
            assertEquals(4, progress.terminationCount(GameTermination.CANCELLED));
            assertEquals(end.validation().orElseThrow().terminations(), progress.terminations());
            assertTrue(end.assessment().isPresent());
        }
    }
}
