package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.telemetry.ActiveGameSnapshot;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static com.ohinteractive.seedv6.training.service.LearningArenaConfig.*;
import static com.ohinteractive.seedv6.training.service.LearningArenaState.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(180)
class MatchOnlyArenaTest {
    @TempDir Path temporary;
    static Arena protocol() { return new Arena(2, Limit.DEPTH, 1, 100, 1, 0, 0, 4, "4k3/8/8/8/8/8/8/R3K3 w - - 98 1"); }
    Competitor network(TrainingArchitecture architecture) throws Exception {
        Path root = temporary.resolve("source"); String id;
        try (var store = new CheckpointStore(root, architecture)) {
            id = store.publish(NetworkTrainingState.initialized(architecture, 1, .001), new CheckpointManifest.Metadata(7, 1, "")).manifest().id();
        }
        return Competitor.fixed(ModelLibrary.resolve(ModelLibrary.identify(root), id).binding());
    }
    @ParameterizedTest @EnumSource(TrainingArchitecture.class)
    void allSupportedNetworksPlayHceInEitherPositionWithoutTraining(TrainingArchitecture architecture) throws Exception {
        var neural = network(architecture); var hce = Competitor.handcrafted();
        var sourceManifest = CheckpointInspection.manifest(Path.of(neural.initialModel().root()).resolve("checkpoints")
                .resolve(neural.initialModel().checkpoint()));
        for (boolean first : new boolean[]{true, false}) {
            Path root = temporary.resolve(first ? "hce-first" : "hce-second");
            var config = LearningArenaConfig.match("fixed", first ? hce : neural, first ? neural : hce, 71, protocol());
            var seen = new HashSet<Integer>();
            try (var service = LearningArenaService.create(root, config, update -> {
                var game = update.liveGame(); if (game == null) return;
                boolean hceWhite = first == (game.gameInPair() == 1);
                assertEquals(hceWhite, game.white().role() == ActiveGameSnapshot.Role.HCE);
                assertEquals(!hceWhite, game.black().role() == ActiveGameSnapshot.Role.HCE);
                var network = hceWhite ? game.black() : game.white();
                assertEquals(architecture, network.model().architecture()); seen.add(game.gameInPair());
                assertNull(update.optimization());
            })) {
                service.run(); assertEquals(Status.COMPLETE, service.state().status());
                var round = service.state().current(); assertEquals(0, round.number());
                assertEquals(1, round.result(service.state().config()).pairs().size());
                assertTrue(round.result(service.state().config()).pairs().getFirst().valid());
                assertEquals(0, round.a().exposure()); assertEquals(0, round.b().exposure());
                assertNull(round.a().training()); assertNull(round.b().training());
                assertEquals(first, round.a().isHandcrafted());
                var copied = first ? round.b() : round.a();
                assertEquals(sourceManifest.networkSha256(), CheckpointInspection.manifest(root.resolve(first ? "B" : "A")
                        .resolve("checkpoints").resolve(copied.checkpoint())).networkSha256());
            }
            assertEquals(Set.of(1, 2), seen);
            assertFalse(Files.exists(root.resolve(first ? "A" : "B")), "HCE has no checkpoint store");
            assertFalse(Files.exists(root.resolve("rounds")), "No tranches or optimizer work");
            assertFalse(Files.exists(root.resolve(first ? "B/refs/best" : "A/refs/best")), "Match results do not promote");
            var saved = LearningArenaState.read(root);
            try (var service = LearningArenaService.resume(root, ignored -> {})) { service.run(); assertEquals(saved, service.state()); }
        }
    }
    @Test void hceVersusHceCompletesAndSavedGamesAreNotReplayedAfterPause() throws Exception {
        Path root = temporary.resolve("hce"); var owner = new AtomicReference<LearningArenaService>();
        var config = LearningArenaConfig.match("HCE pair", Competitor.handcrafted(), Competitor.handcrafted(), 4, protocol());
        try (var service = LearningArenaService.create(root, config, u -> {
            if (!u.state().current().pairs().isEmpty() && u.state().current().pairs().getFirst().candidateWhite().termination().completed()) owner.get().pause();
        })) {
            owner.set(service); service.run(); assertEquals(Status.PAUSED, service.state().status());
        }
        var first = LearningArenaState.read(root).current().pairs().getFirst().candidateWhite();
        var seen = new HashSet<Integer>();
        try (var service = LearningArenaService.resume(root, u -> { if (u.liveGame() != null) seen.add(u.liveGame().gameOrdinal()); })) {
            service.run(); assertEquals(Status.COMPLETE, service.state().status());
            assertEquals(first, service.state().current().pairs().getFirst().candidateWhite());
        }
        assertEquals(Set.of(2), seen); assertFalse(Files.exists(root.resolve("A"))); assertFalse(Files.exists(root.resolve("B")));
        var saved = LearningArenaState.read(root);
        assertEquals(config, saved.config()); assertEquals(saved, DataFiles.JSON.fromJson(DataFiles.JSON.toJson(saved), LearningArenaState.class));
    }
    @Test void fixedModelResumeUsesCampaignSnapshotAfterSourceDisappears() throws Exception {
        var network = network(TrainingArchitecture.BRN3); Path root = temporary.resolve("frozen");
        var owner = new AtomicReference<LearningArenaService>();
        try (var service = LearningArenaService.create(root, LearningArenaConfig.match("fixed", network, Competitor.handcrafted(), 5, protocol()), u -> {
            if (u.state().current().stage() == Stage.ARENA) owner.get().pause();
        })) { owner.set(service); service.run(); assertEquals(Status.PAUSED, service.state().status()); }
        Files.move(temporary.resolve("source"), temporary.resolve("moved-source"));
        try (var service = LearningArenaService.resume(root, ignored -> {})) { service.run(); assertEquals(Status.COMPLETE, service.state().status()); }
    }
    @Test void trainingContractStillRejectsHceAndMatchRequiresAnExactNetwork() {
        assertThrows(IllegalArgumentException.class, () -> LearningArenaConfig.match("invalid", new Competitor("fresh", TrainingArchitecture.BRN3, 1, 1), Competitor.handcrafted(), 1, protocol()));
        var legacy = ArenaRecipeConfigurationTest.legacy();
        assertThrows(IllegalArgumentException.class, () -> new LearningArenaConfig("invalid", Competitor.handcrafted(), legacy.b(), legacy.source(), 1, 1, 1, 1, protocol()));
    }
    @Test void timedHcePairUsesTheExistingParallelSearchAndCompletionProtocol() throws Exception {
        var p = protocol();
        var timed = new Arena(2, Limit.TIME, 1, 250, 2, 0, 0, 4, p.startingFen());
        var config = LearningArenaConfig.match("timed", Competitor.handcrafted(), Competitor.handcrafted(), 71, timed);
        try (var service = LearningArenaService.create(temporary.resolve("timed"), config, ignored -> {})) {
            service.run(); assertEquals(Status.COMPLETE, service.state().status());
            assertTrue(service.state().current().result(config).pairs().getFirst().valid());
            assertEquals(2, service.state().current().result(config).config().threads());
        }
    }
}
