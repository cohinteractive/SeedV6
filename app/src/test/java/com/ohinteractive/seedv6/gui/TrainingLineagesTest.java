package com.ohinteractive.seedv6.gui;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.service.*;
import com.ohinteractive.seedv6.core.brn2.Brn2Trainer;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(60)
class TrainingLineagesTest {
    @TempDir Path temp;
    TrainingController controller;
    TrainingPanel panel;
    JFrame frame;
    final TrainingController.Backend backend = new TrainingController.Backend();

    @BeforeEach void theme() throws Exception {
        if (!java.awt.GraphicsEnvironment.isHeadless()) edt(SeedTheme::initialize);
    }

    @AfterEach void close() throws Exception {
        if (controller != null) edt(controller::beginShutdown).run();
        if (frame != null) edt(frame::dispose);
    }
    void window() throws Exception {
        if (java.awt.GraphicsEnvironment.isHeadless()) return;
        edt(() -> {
            SeedTheme.initialize(); frame = new JFrame("Training lineage smoke"); frame.setContentPane(panel);
            frame.setSize(1150, 900); frame.setVisible(true); frame.validate();
        });
    }
    void capture(String name) throws Exception {
        if (frame == null) return;
        edt(() -> {
            frame.validate();
            var image = new java.awt.image.BufferedImage(frame.getWidth(), frame.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
            var graphics = image.createGraphics(); frame.paint(graphics); graphics.dispose();
            Path output = Path.of("build", "gui-smoke", name + ".png"); Files.createDirectories(output.getParent());
            javax.imageio.ImageIO.write(image, "png", output.toFile()); return null;
        });
    }
    void controller() throws Exception {
        var initial = TrainingSettings.defaults(temp.resolve("unselected"), NetworkArchitecture.BRN2);
        panel = edt(() -> new TrainingPanel(initial));
        controller = edt(() -> new TrainingController(initial, backend, ignored -> {}, panel::showState));
        edt(() -> panel.bind(controller));
    }
    Exception select(TrainingLineages.Entry entry) throws Exception {
        var result = new CompletableFuture<Exception>();
        edt(() -> controller.selectLineage(() -> TrainingLineages.read(entry), null, result::complete));
        return result.get(10, TimeUnit.SECONDS);
    }
    TrainingLineages.Entry create(String name) throws Exception { return TrainingLineages.create(temp, NetworkArchitecture.BRN2, name); }
    TrainingLineages.Entry partial(String name, int settled) throws Exception {
        var entry = create(name); var settings = TrainingLineages.read(entry).settings();
        try (var store = new CheckpointStore(entry.root(), TrainingArchitecture.BRN2)) {
            store.writeTrainingSource(TrainingSource.HANDCRAFTED);
            store.initializeBrnSupervision(BrnSupervision.WDL);
            var initial = store.initialize(new NetworkTrainingState.Brn2(new Brn2Trainer(.001)),
                    new CheckpointManifest.Metadata(settled, settings.depth(), ""));
            var id = initial.manifest().id();
            store.writeGenerationAttempt(GenerationAttempt.create(id, id, settled + 1,
                    settings.config(TrainerConfig.DepthChange.REQUIRE_SAME), TrainingSource.HANDCRAFTED));
        }
        return entry;
    }

    @Test void switchesGenerationConfigurationAndEntireDashboardTogether() throws Exception {
        var a = partial("T1 Continuous", 43); var b = partial("T2", 143); var fresh = create("Fresh");
        TrainingLineages.save(TrainingLineages.read(a), TrainingLineages.read(a).settings().withTimeLimit(12));
        TrainingLineages.save(TrainingLineages.read(b), TrainingLineages.read(b).settings().withTimeLimit(37));
        controller(); window();
        for (var entry : List.of(a, b, a, fresh)) {
            assertNull(select(entry));
            edt(() -> {
                var state = controller.state();
                long generation = entry.equals(a) ? 44 : 144;
                assertEquals(entry.root(), state.settings().root());
                assertEquals(entry.equals(fresh) ? "Start Training" : "Resume Generation " + generation, state.startAction());
                assertEquals(state.startAction(), named(panel, "startTraining", JButton.class).getText());
                if (!entry.equals(fresh)) assertEquals(generation, state.snapshot().generation());
                assertEquals(entry.equals(a) ? 12L : entry.equals(b) ? 37L : 0L,
                        named(panel, "trainingRunMinutes", JSpinner.class).getValue());
                assertTrue(named(panel, "trainingLineageIdentity", JLabel.class).getText().contains(entry.name()));
                assertTrue(named(panel, "trainingHeadline", JLabel.class).getText().contains(entry.equals(fresh) ? "1" : "" + generation));
            });
            capture("lineage-" + entry.name().replace(' ', '-'));
        }
    }

    @Test void appliedEditsSurviveSwitchingAndReloadWithoutAnyGlobalConfiguration() throws Exception {
        var a = create("A"); var b = create("B"); controller(); assertNull(select(a));
        edt(() -> {
            named(panel, "trainingRunMinutes", JSpinner.class).setValue(23L);
            named(panel, "trainingDepth", JSpinner.class).setValue(7);
            assertTrue(panel.applySettings());
        });
        assertNull(select(b));
        assertEquals(4, edt(() -> controller.state().settings().depth()));
        assertNull(select(a));
        assertEquals(7, edt(() -> controller.state().settings().depth()));
        assertEquals(23, TrainingLineages.read(a).settings().maximumRunMinutes());
        assertEquals(0, TrainingLineages.read(b).settings().maximumRunMinutes());
        for (var architecture : NetworkArchitecture.values()) {
            var entry = TrainingLineages.create(temp, architecture, "New " + architecture);
            var settings = TrainingLineages.read(entry).settings();
            assertEquals(4, settings.depth()); assertEquals(0, settings.maximumRunMinutes());
            assertEquals(TrainerConfig.DEFAULT_BRN_LEARNING_RATE, settings.brn2LearningRate());
            assertEquals(architecture, settings.architecture());
            assertNotEquals(entry.name(), entry.root().getFileName().toString());
            UUID.fromString(entry.root().getFileName().toString());
        }
    }

    @Test void legacyAdoptionIsAdditiveAndDefaultsAreExplicitEvenAfterCustomizedLineage() throws Exception {
        Path external = temp.resolve("external");
        try (var store = new CheckpointStore(external, TrainingArchitecture.BRN2)) {
            store.writeTrainingSource(TrainingSource.HANDCRAFTED);
            store.initialize(new NetworkTrainingState.Brn2(new Brn2Trainer(.003)), new CheckpointManifest.Metadata(43, 4, ""));
        }
        var before = hashes(external); var a = create("A");
        TrainingLineages.save(TrainingLineages.read(a), TrainingLineages.read(a).settings().withTimeLimit(77));
        controller(); assertNull(select(a));
        var legacy = new TrainingLineages.Entry(external, NetworkArchitecture.BRN2, "external");
        assertNull(select(legacy));
        assertEquals(0, edt(() -> controller.state().settings().maximumRunMinutes()));
        assertTrue(edt(() -> controller.state().message().contains("not recorded")));
        var metadata = TrainingLineage.read(external).orElseThrow();
        assertEquals("external", metadata.name());
        assertFalse(CheckpointInspection.freshRoot(external, TrainingArchitecture.BRN2));
        var after = hashes(external);
        before.forEach((path, hash) -> assertEquals(hash, after.get(path), path.toString()));
        assertEquals(before.size() + 1, after.size());
        assertEquals(metadata.id(), TrainingLineages.read(legacy).lineage().id());
    }

    @Test void failedOrPendingLoadPreservesPreviousViewAndBlocksConflictingActions() throws Exception {
        var a = partial("A", 43); controller(); assertNull(select(a));
        var before = edt(controller::state);
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1); var result = new CompletableFuture<Exception>();
        edt(() -> controller.selectLineage(() -> { entered.countDown(); assertTrue(release.await(5, TimeUnit.SECONDS)); throw new IOException("Invalid store"); }, null, result::complete));
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        edt(() -> {
            assertFalse(controller.state().canStart());
            assertFalse(named(panel, "networkArchitecture", JComboBox.class).isEnabled());
            assertFalse(named(panel, "trainingLineage", JComboBox.class).isEnabled());
            assertEquals(before.settings(), controller.state().settings());
            assertSame(before.snapshot(), controller.state().snapshot());
        });
        release.countDown(); assertNotNull(result.get(5, TimeUnit.SECONDS));
        edt(() -> {
            var after = controller.state();
            assertEquals(before.settings(), after.settings()); assertSame(before.snapshot(), after.snapshot());
            assertSame(before.history(), after.history()); assertEquals(before.startAction(), after.startAction());
            assertEquals(before.lineage(), after.lineage()); assertTrue(after.canStart());
        });
        var broken = new TrainingLineages.Entry(temp.resolve("missing"), NetworkArchitecture.BRN2, "missing");
        assertNotNull(select(broken)); assertFalse(Files.exists(broken.root()));
    }

    @Test void configurationCodecIncludesEveryEditableFieldAndRejectsCorruption() throws Exception {
        var s = new TrainingSettings(temp, 5, 2, 36, 2, 5, 13, 7, 3, 22, 77, 96, 51,
                NetworkArchitecture.BRN2, .002, .003, .004, TrainingSource.HANDCRAFTED, "generator",
                BrnSupervision.blended(.5), new BrnRunSeeds(77, 123), temp.resolve("teacher").toString(), 19,
                ValidationMethod.GAME_PAIRS, new BrnCaptureConsistency(3));
        assertEquals(s, LineageConfiguration.decode(LineageConfiguration.encode(s), temp, s.architecture()));
        assertThrows(IOException.class, () -> LineageConfiguration.decode("broken", temp, s.architecture()));
    }

    @Test void switchingRestoresSettledCandidateBestDecisionAndHistoryWithoutMixingFreshLineage() throws Exception {
        var entry = create("Completed"); var fresh = create("Empty"); String candidate, best;
        var config = TrainingLineages.read(entry).settings().config(TrainerConfig.DepthChange.REQUIRE_SAME);
        try (var store = new CheckpointStore(entry.root(), TrainingArchitecture.BRN2)) {
            var trainer = new NetworkTrainingState.Brn2(new Brn2Trainer(.001));
            best = store.initialize(trainer, new CheckpointManifest.Metadata(43, 4, "")).manifest().id();
            candidate = store.publish(trainer, new CheckpointManifest.Metadata(44, 4, best)).manifest().id();
            var draw = new com.ohinteractive.seedv6.training.validation.ValidationResult.Game(
                    com.ohinteractive.seedv6.training.selfplay.GameTermination.FIFTY_MOVE_RULE, 2);
            var result = new com.ohinteractive.seedv6.training.validation.ValidationResult(config.validation(44), "a".repeat(64),
                    java.util.Collections.nCopies(config.validation().openingPairs(),
                            new com.ohinteractive.seedv6.training.validation.ValidationResult.Pair("fixture", draw, draw)));
            var decision = CandidateLifecycle.recordDecision(store, candidate, best, result, config.validation().policy()).validation();
            var stats = decision.statistics(); var assessment = decision.assessment();
            var row = new com.ohinteractive.seedv6.training.history.GenerationRecord(44, candidate, best, best,
                    com.ohinteractive.seedv6.training.history.GenerationRecord.Outcome.RETAINED, decision.decision(),
                    stats.wins(), stats.draws(), stats.losses(), stats.validPairs(), stats.incompletePairs(),
                    assessment.mean(), assessment.lowerBound(), assessment.threshold(),
                    new com.ohinteractive.seedv6.training.history.GenerationRecord.Regime(4, 64, 64, 1),
                    0, 0, 0L, null, java.time.Instant.EPOCH, java.time.Instant.EPOCH.plusSeconds(1), 0L, 0L, 0L, 1L);
            new com.ohinteractive.seedv6.training.history.HistoryRepository(entry.root()).append(row);
        }
        controller(); assertNull(select(entry));
        var view = edt(controller::state);
        assertEquals("Start Training", view.startAction()); assertEquals(45, view.snapshot().generation());
        assertEquals(candidate, view.snapshot().candidateId()); assertEquals(best, view.snapshot().bestId());
        assertTrue(view.snapshot().validation().isPresent()); assertEquals(44, view.history().records().getFirst().generation());
        assertNull(select(fresh));
        assertNull(edt(() -> controller.state().snapshot())); assertTrue(edt(() -> controller.state().history().records().isEmpty()));
        assertNull(select(entry));
        assertEquals(view.snapshot(), edt(() -> controller.state().snapshot()));
        assertEquals(view.history().records(), edt(() -> controller.state().history().records()));
    }

    @Test void activeRunLocksLineageArchitectureAndUsesUnifiedStopControls() throws Exception {
        var entry = create("Active"); var s = TrainingLineages.read(entry).settings();
        var handle = new TrainingController.Handle() {
            boolean terminated; long scheduled;
            public void start() {}
            public void stop() { terminated = true; scheduled = 0; }
            public TrainerSnapshot snapshot() { return TrainingDashboardTest.snapshot(
                    terminated ? TrainerSnapshot.State.STOPPED : TrainerSnapshot.State.GENERATING_SELF_PLAY, true, false, false); }
            public boolean terminated() { return terminated; }
            public void close() { terminated = true; }
            public long stopAfterGeneration() { return scheduled = 187; }
            public long scheduledStopGeneration() { return scheduled; }
            public boolean cancelScheduledStop() { scheduled = 0; return true; }
        };
        panel = edt(() -> new TrainingPanel(s));
        controller = edt(() -> new TrainingController(s, new TrainingController.Backend() {
            @Override TrainingController.Inspection inspect(TrainingSettings settings) { return new TrainingController.Inspection(false, "", settings.depth(), ""); }
            @Override TrainingController.Handle create(TrainingSettings settings, boolean resume, TrainerConfig.DepthChange change) { return handle; }
        }, ignored -> {}, panel::showState));
        edt(() -> panel.bind(controller)); assertNull(select(entry));
        window();
        edt(controller::start);
        until(() -> edt(() -> { controller.poll(); return controller.state().phase() == TrainingController.Phase.RUNNING; }));
        edt(() -> {
            for (String name : List.of("networkArchitecture", "trainingLineage", "newTrainingLineage", "importTrainingLineage", "applyTrainingSettings"))
                assertFalse(named(panel, name, JComponent.class).isEnabled(), name);
            assertFalse(named(panel, "startTraining", JButton.class).isVisible());
            assertTrue(named(panel, "stopTraining", JButton.class).isVisible());
            assertTrue(named(panel, "scheduleTrainingStop", JButton.class).isEnabled());
            assertThrows(IllegalStateException.class, () -> controller.selectLineage(() -> null, s, ignored -> {}));
            named(panel, "scheduleTrainingStop", JButton.class).doClick();
            assertEquals("Cancel Scheduled Stop", named(panel, "scheduleTrainingStop", JButton.class).getText());
            assertEquals("STOP SCHEDULED", named(panel, "trainingState", JLabel.class).getText());
            assertEquals("STOPPING AFTER GENERATION 187", named(panel, "trainingLifecycleStatus", JLabel.class).getText());
            assertTrue(named(panel, "stopTraining", JButton.class).isEnabled());
        });
        capture("lineage-scheduled-stop");
        if (frame != null) {
            edt(() -> { frame.setSize(650, 900); frame.validate(); });
            capture("lineage-scheduled-stop-compact");
        }
        edt(() -> {
            named(panel, "scheduleTrainingStop", JButton.class).doClick(); assertEquals(0, controller.state().scheduledStopGeneration());
            named(panel, "stopTraining", JButton.class).doClick(); controller.poll();
            assertFalse(controller.state().active()); assertTrue(named(panel, "networkArchitecture", JComboBox.class).isEnabled());
        });
        capture("lineage-stopped");
    }

    @Test void baseRootCatalogAndAdoptedLocationsPersistAndRemainIndependent() throws Exception {
        var prefs = java.util.prefs.Preferences.userRoot().node("seedv6-lineages-" + UUID.randomUUID());
        try {
            var a = create("A"); Path other = temp.resolve("other-base");
            var b = TrainingLineages.create(other, NetworkArchitecture.BRN2, "B");
            prefs.put(TrainingFolders.key(NetworkArchitecture.BRN2), a.root().toString());
            prefs.put("architecture", "BRN2"); prefs.putInt("depth", 99); prefs.putLong("maximumRunMinutes", 999);
            var startup = TrainingSettings.selectionDefaults(prefs);
            assertEquals(a.root(), startup.root()); assertEquals(4, startup.depth()); assertEquals(0, startup.maximumRunMinutes());
            var folders = new TrainingFolders(prefs);
            assertEquals(temp, folders.base(), "Naturally nested legacy selection establishes the base without relocation");
            Path external = temp.resolve("outside"); Files.createDirectory(external);
            folders.register(temp, NetworkArchitecture.BRN2, external);
            folders.base(other);
            var restarted = new TrainingFolders(prefs); assertEquals(other, restarted.base());
            assertEquals(List.of(b), TrainingLineages.discover(other, NetworkArchitecture.BRN2, restarted.adopted(other, NetworkArchitecture.BRN2)));
            var original = TrainingLineages.discover(temp, NetworkArchitecture.BRN2, restarted.adopted(temp, NetworkArchitecture.BRN2));
            assertTrue(original.contains(a)); assertTrue(original.stream().anyMatch(e -> e.root().equals(external)));
            var metadata = TrainingLineage.read(a.root()).orElseThrow();
            try (var store = new CheckpointStore(a.root(), TrainingArchitecture.BRN2)) {
                store.writeLineage(new TrainingLineage(metadata.id(), "Renamed", metadata.architecture(), metadata.created(),
                        metadata.configuration(), metadata.configurationOrigin()));
            }
            assertEquals(metadata.id(), TrainingLineages.read(a).lineage().id());
            assertEquals("Renamed", TrainingLineages.read(a).entry().name());
            assertTrue(CheckpointInspection.freshRoot(a.root(), TrainingArchitecture.BRN2));
        } finally { prefs.removeNode(); }
    }

    private static Map<Path, String> hashes(Path root) throws Exception {
        var result = new HashMap<Path, String>();
        try (var files = Files.walk(root)) {
            for (var path : files.filter(Files::isRegularFile).toList()) result.put(root.relativize(path), HexFormat.of().formatHex(
                    java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))));
        }
        return result;
    }
}
