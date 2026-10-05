package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.model.TrainingArchitecture;
import com.ohinteractive.seedv6.training.selfplay.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.training.service.LearningArenaConfig.*;
import static com.ohinteractive.seedv6.training.service.LearningArenaState.*;

@Timeout(180)
class LearningArenaTest {
    @TempDir Path temporary;
    static final String MATE = "7k/5K2/6Q1/8/8/8/8/8 w - - 0 1";
    Path source() throws Exception {
        Path path = temporary.resolve("data.jsonl");
        if (!Files.exists(path)) {
            var text = new StringBuilder();
            // NNUE accepts mate labels in ordinary training; common eligibility must exclude this row.
            text.append(SourceReadersTest.line(0).replace("\"cp\":0", "\"mate\":3"));
            for (int i = 1; i < 30; i++) text.append(SourceReadersTest.line(i * 30));
            Files.writeString(path, text);
        }
        return path;
    }
    LearningArenaConfig config(TrainingArchitecture a, TrainingArchitecture b, int rounds, Limit mode) throws Exception {
        return new LearningArenaConfig("test", new Competitor("A", a, 71, 1), new Competitor("B", b, 97, 2),
                DataSource.register("shared", source(), 1), 2, 2, rounds, 123,
                new Arena(2, mode, 1, 100, 1, 0, 0, 4, MATE));
    }
    @Test void sharedTrancheUsesCommonEligibilityAndBitIdenticalTargetsAndSurvivesSourceRemoval() throws Exception {
        var config = config(TrainingArchitecture.NNUE, TrainingArchitecture.BRN3, 1, Limit.DEPTH);
        Path directory = temporary.resolve("tranche-round");
        var tranche = LearningArenaTranche.obtain(directory, temporary.resolve("seek"), config, 0, () -> false);
        assertEquals(List.of(1L, 2L), tranche.rows.stream().map(LearningArenaTranche.Row::ordinal).toList());
        var a = tranche.examples(config.a().architecture(), config.source().labelProfile());
        var b = tranche.examples(config.b().architecture(), config.source().labelProfile());
        for (int i = 0; i < a.samples().size(); i++) {
            assertArrayEquals(a.samples().get(i).board(), b.samples().get(i).board());
            assertEquals(Double.doubleToLongBits(a.targets().applyAsDouble(a.samples().get(i))), Double.doubleToLongBits(b.targets().applyAsDouble(b.samples().get(i))));
        }
        Files.delete(source());
        var restored = LearningArenaTranche.obtain(directory, temporary.resolve("seek"), config, 0, () -> false);
        assertEquals(tranche.manifest, restored.manifest);
        Path payload = directory.resolve("tranche/records.bin");
        byte[] bytes = Files.readAllBytes(payload); bytes[25] ^= 1; Files.write(payload, bytes);
        assertThrows(IOException.class, () -> LearningArenaTranche.read(directory.resolve("tranche"), config, 0));
    }
    @org.junit.jupiter.params.ParameterizedTest @org.junit.jupiter.params.provider.EnumSource(value = TrainingArchitecture.class, names = {"NNUE", "NNUE_MATERIAL"})
    void freshNnueVersusBrn3RoundZeroTrainingCheckpointsArenaAndCompletedResume(TrainingArchitecture architecture) throws Exception {
        var config = config(architecture, TrainingArchitecture.BRN3, 1, Limit.DEPTH);
        Path root = temporary.resolve("smoke"); var stages = new ArrayList<String>();
        try (var service = LearningArenaService.create(root, config, u -> stages.add(u.state().current().number() + ":" + u.state().current().stage()))) {
            service.run(); assertEquals(Status.COMPLETE, service.state().status());
        }
        var state = LearningArenaState.read(root);
        assertEquals(2, state.history().size());
        assertTrue(stages.indexOf("0:ROUND_COMPLETE") < stages.indexOf("1:SELECT_TRANCHE"));
        for (var round : state.history()) {
            assertTrue(round.arenaComplete()); assertEquals(round.a().exposure(), round.b().exposure());
            assertEquals(round.number() * 4, round.a().exposure());
            assertEquals(1, round.result(config).statistics().validPairs());
            assertEquals(1, round.result(config).statistics().wins()); assertEquals(1, round.result(config).statistics().losses());
        }
        for (String side : List.of("A", "B")) {
            assertFalse(Files.exists(root.resolve(side + "/refs/best")));
            try (var promotions = Files.list(root.resolve(side + "/promotions"))) { assertEquals(0, promotions.count()); }
            assertEquals(2, CheckpointStore.availableCheckpoints(root.resolve(side)).checkpoints().size());
        }
        String original = Files.readString(root.resolve("campaign.json"));
        try (var resumed = LearningArenaService.resume(root, u -> {})) { resumed.run(); }
        assertEquals(original, Files.readString(root.resolve("campaign.json")), "Completed Resume does no work");
    }
    @org.junit.jupiter.params.ParameterizedTest @org.junit.jupiter.params.provider.EnumSource(value = TrainingArchitecture.class, names = {"BRN3", "NNUE_MATERIAL"})
    void optimizerPauseResumeIsByteExactAndCompletedAIsNeverRetrained(TrainingArchitecture architecture) throws Exception {
        var config = config(architecture, TrainingArchitecture.BRN3, 1, Limit.DEPTH);
        Path continuous = temporary.resolve("continuous"), split = temporary.resolve("split");
        try (var service = LearningArenaService.create(continuous, config, u -> {})) { service.run(); }
        var owner = new AtomicReference<LearningArenaService>();
        try (var service = LearningArenaService.create(split, config, u -> {
            if (u.state().current().number() == 1 && u.state().current().stage() == Stage.TRAIN_A && u.detail().contains("consumed 1 /")) owner.get().pause();
        })) {
            owner.set(service); service.snapshotNanos = 0; service.run();
            assertEquals(Status.PAUSED, service.state().status()); assertNull(service.state().current().a());
        }
        var progress = DataFiles.read(split.resolve("rounds/000001/A/progress.json"), LearningArenaTraining.Progress.class);
        assertEquals(1, progress.cursor().samples());
        try (var service = LearningArenaService.resume(split, u -> {
            if (u.state().current().number() == 1 && u.state().current().stage() == Stage.TRAIN_B) owner.get().pause();
        })) { owner.set(service); service.run(); assertNotNull(service.state().current().a()); assertNull(service.state().current().b()); }
        var afterA = LearningArenaState.read(split).current().a();
        Files.delete(source()); // Frozen tranche remains sufficient for B and the arena.
        try (var service = LearningArenaService.resume(split, u -> {
            assertFalse(u.state().current().stage() == Stage.TRAIN_A, "Completed A must be skipped");
        })) { service.run(); assertEquals(Status.COMPLETE, service.state().status()); }
        var expected = LearningArenaState.read(continuous); var actual = LearningArenaState.read(split);
        assertEquals(afterA, actual.current().a()); assertEquals(expected.history(), actual.history());
        for (String side : List.of("A", "B")) {
            String checkpoint = side.equals("A") ? actual.current().a().checkpoint() : actual.current().b().checkpoint();
            Path relative = Path.of(side, "checkpoints", checkpoint, "training.state");
            assertEquals(-1, Files.mismatch(continuous.resolve(relative), split.resolve(relative)));
        }
    }
    @Test void campaignOwnershipAndCorruptFrozenTrancheFailClosed() throws Exception {
        var config = config(TrainingArchitecture.BRN3, TrainingArchitecture.BRN3, 1, Limit.DEPTH);
        Path root = temporary.resolve("owned"); var owner = new AtomicReference<LearningArenaService>();
        try (var service = LearningArenaService.create(root, config, u -> {
            if (u.state().current().stage() == Stage.TRAIN_A) owner.get().pause();
        })) {
            owner.set(service); assertThrows(IOException.class, () -> LearningArenaService.resume(root, u -> {})); service.run();
        }
        var before = LearningArenaState.read(root).current();
        Files.write(root.resolve("rounds/000001/tranche/records.bin"), new byte[]{1});
        try (var service = LearningArenaService.resume(root, u -> {})) {
            assertThrows(IOException.class, service::run); assertEquals(Status.FAILED, service.state().status());
            assertEquals(before.a(), service.state().current().a()); assertNull(service.state().current().b());
        }
    }
    @Test void timedArenaAndSuccessiveTranchesRemainSeparateFromGenerationNumbers() throws Exception {
        var config = config(TrainingArchitecture.BRN3, TrainingArchitecture.BRN3, 2, Limit.TIME);
        Path root = temporary.resolve("timed");
        try (var service = LearningArenaService.create(root, config, u -> {})) { service.run(); }
        var state = LearningArenaState.read(root); assertEquals(Status.COMPLETE, state.status());
        assertEquals(3, state.history().size());
        assertEquals(3, state.history().get(1).sourceEnd()); assertEquals(5, state.current().sourceEnd());
        assertNotEquals(state.history().get(1).trancheHash(), state.current().trancheHash());
        assertEquals(8, state.current().b().exposure());
        assertEquals(1, state.current().result(config).statistics().validPairs());
    }
    @Test void crashAfterCheckpointPublicationBeforeReceiptDoesNotRepeatCompletedTraining() throws Exception {
        var config = config(TrainingArchitecture.BRN3, TrainingArchitecture.BRN3, 1, Limit.DEPTH);
        Path root = temporary.resolve("publication-crash");
        try (var service = LearningArenaService.create(root, config, u -> {})) {
            service.checkpointPublished = checkpoint -> {
                if (checkpoint.manifest().generation() == 1) throw new IllegalStateException("simulated process boundary");
            };
            assertThrows(IllegalStateException.class, service::run);
            assertNull(service.state().current().a());
        }
        assertEquals(2, CheckpointStore.availableCheckpoints(root.resolve("A")).checkpoints().size());
        var progress = DataFiles.read(root.resolve("rounds/000001/A/progress.json"), LearningArenaTraining.Progress.class);
        assertEquals(4, progress.cursor().samples());
        try (var resumed = LearningArenaService.resume(root, u -> assertFalse(u.detail().startsWith("A consumed")))) {
            resumed.run(); assertEquals(Status.COMPLETE, resumed.state().status());
        }
        assertEquals(2, CheckpointStore.availableCheckpoints(root.resolve("A")).checkpoints().size());
    }
    @Test void crashBetweenReversedGamesPreservesTheCompletedGame() throws Exception {
        var config = config(TrainingArchitecture.BRN3, TrainingArchitecture.BRN3, 1, Limit.DEPTH);
        Path root = temporary.resolve("game-crash"); var crashed = new java.util.concurrent.atomic.AtomicBoolean();
        try (var service = LearningArenaService.create(root, config, u -> {
            var pairs = u.state().current().pairs();
            if (!pairs.isEmpty() && pairs.getFirst().candidateWhite().termination().completed()
                    && pairs.getFirst().candidateBlack().termination() == GameTermination.CANCELLED && !crashed.getAndSet(true))
                throw new IllegalStateException("simulated crash after first game commit");
        })) { assertThrows(IllegalStateException.class, service::run); }
        var first = LearningArenaState.read(root).current().pairs().getFirst().candidateWhite();
        try (var service = LearningArenaService.resume(root, u -> {
            if (u.state().current().number() == 0 && u.search() != null && u.search().gameInPair() == 1)
                assertTrue(u.search().lastMoveSearch().isEmpty(), "First game must never search again");
        })) { service.run(); assertEquals(Status.COMPLETE, service.state().status()); }
        assertEquals(first, LearningArenaState.read(root).history().getFirst().pairs().getFirst().candidateWhite());
    }
}
