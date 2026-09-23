package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.training.checkpoint.BootstrapEvidence;
import com.ohinteractive.seedv6.training.history.*;
import com.ohinteractive.seedv6.training.service.*;
import com.ohinteractive.seedv6.training.validation.*;
import static org.junit.jupiter.api.Assertions.*;

class TrainingRunPresentationTest {
    @org.junit.jupiter.api.io.TempDir Path temporary;

    @Test void realReadOnlyActionPreviewDistinguishesMutableChangesAndLineageLocksWithoutWriting() throws Exception {
        Path root = temporary.resolve("preview");
        var s = new TrainingSettings(root, 1, 1, 8, 0, 0, 4, 1, 1, 2, 1, 8, 1,
                NetworkArchitecture.BRN2, .001, .001, .001, TrainingSource.HANDCRAFTED, "", BrnSupervision.WDL,
                new BrnRunSeeds(1, 2));
        var backend = new TrainingController.Backend();
        assertEquals("Start Training", backend.preview(s)); assertFalse(Files.exists(root));
        var architecture = com.ohinteractive.seedv6.training.model.TrainingArchitecture.BRN2;
        try (var store = new com.ohinteractive.seedv6.training.checkpoint.CheckpointStore(root, architecture)) {
            store.initializeBrnRunSeeds(s.runSeeds()); store.initializeBrnSupervision(s.supervision()); store.writeTrainingSource(s.source());
            store.initialize(new com.ohinteractive.seedv6.training.model.NetworkTrainingState.Brn2(
                    new com.ohinteractive.seedv6.core.brn2.Brn2Trainer(.001)),
                    new com.ohinteractive.seedv6.training.checkpoint.CheckpointManifest.Metadata(3, 1, ""));
            assertEquals("Start Next Generation", backend.preview(s));
            String parent = store.recover().latestTraining().orElseThrow().manifest().id();
            store.writeGenerationAttempt(com.ohinteractive.seedv6.training.checkpoint.GenerationAttempt.create(parent, parent, 4,
                    s.config(TrainerConfig.DepthChange.REQUIRE_SAME), s.source()));
        }
        var before = hashes(root);
        assertEquals("Resume Generation 4", backend.preview(s.withTimeLimit(15)));
        var changed = new TrainingSettings(root, 1, 2, 8, 0, 0, 4, 1, 1, 2, 1, 8, 1,
                NetworkArchitecture.BRN2, .001, .001, .001, s.source(), "", s.supervision(), s.runSeeds());
        assertEquals("Restart Generation 4", backend.preview(changed));
        for (var locked : java.util.List.of(s.withSource(TrainingSource.SELF_PLAY), s.withRunSeeds(new BrnRunSeeds(2, 3)),
                s.withSupervision(BrnSupervision.blended(.75)))) {
            assertTrue(assertThrows(java.io.IOException.class, () -> backend.preview(locked)).getMessage().contains("fresh"));
        }
        assertEquals(before, hashes(root), "Action preview must not lock, repair, or write metadata");
    }

    private static Map<Path, String> hashes(Path root) throws Exception {
        var result = new TreeMap<Path, String>();
        try (var files = Files.walk(root)) {
            for (var file : files.filter(Files::isRegularFile).toList()) result.put(root.relativize(file),
                    HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))));
        }
        return result;
    }
    static BootstrapEvidence evidence(BrnSupervision objective) {
        String hash = "a".repeat(64), teacher = objective.blended() ? "teacher" : "";
        var main = new HeldOutLoss.Comparison(64, .023456, .025678);
        return new BootstrapEvidence("", "", "", hash, 1, 256, 8, 2, main, objective,
                objective.blended() ? new HeldOutLoss.Comparison(64, .22, .21) : null,
                objective.blended() ? new HeldOutLoss.Comparison(64, .012, .014) : null,
                TrainingSource.Mode.HANDCRAFTED, teacher, objective.blended() ? TrainingDashboardTest.BEST : "", objective.blended() ? hash : "", true);
    }
    static GenerationRecord row(int generation, BrnSupervision objective) {
        return GenerationRecord.bootstrap(generation, "candidate"+generation, "incumbent", "candidate"+generation,
                new GenerationRecord.Regime(4, 10, 0, 6), 10, 0, 320L, .03,
                Instant.EPOCH, Instant.EPOCH.plusSeconds(generation * 60L), 10L, 20L, 10L, 60_000_000_000L, evidence(objective));
    }
    static TrainerSnapshot snapshot(BrnSupervision objective, long limit) {
        var fixture = TrainingDashboardTest.snapshot(TrainerSnapshot.State.VALIDATING, false, false, false);
        var c = TrainingDashboardTest.settings(Path.of("build/unused-presentation-store")).config(TrainerConfig.DepthChange.REQUIRE_SAME)
                .withTimeLimit(Duration.ofMinutes(60));
        c = new TrainerConfig(c.checkpointRoot(), c.masterSeed(), c.selfPlay(), new TrainerConfig.Training(1, 1, true), c.validation(), limit,
                c.depthChange(), c.startingFen(), com.ohinteractive.seedv6.training.model.TrainingArchitecture.BRN2, .001,
                TrainingSource.HANDCRAFTED, objective).withTimeLimit(Duration.ofMinutes(60));
        return fixture.withBootstrapValidation(Optional.of(new TrainerSnapshot.BootstrapValidation(fixture.candidateId(), fixture.bestId(), evidence(objective))))
                .withRun(Optional.of(new TrainerSnapshot.RunDetails(c, TrainingSource.HANDCRAFTED, objective, 184,
                        limit == 0 ? 0 : 193, "Resume", false, 256)), Optional.of(new TrainerSnapshot.LossProgress(128, 192, 64)));
    }
    @Test void summariesShowAbsoluteAndInvocationTargetsAndFreshTimeBudget() {
        var s = snapshot(BrnSupervision.WDL, 10);
        assertEquals("Generation 187 / 193", TrainingDashboardModel.generationTitle(s));
        assertTrue(TrainingDashboardModel.runLabel(s).startsWith("Run 4 / 10"));
        assertEquals("Remaining 00:27:19 · Budget 01:00:00", TrainingDashboardModel.timeLabel(s));
        assertTrue(TrainingDashboardModel.generationTitle(snapshot(BrnSupervision.WDL, 0)).contains("Continuous"));
        assertFalse(TrainingDashboardModel.runLabel(snapshot(BrnSupervision.WDL, 0)).contains(" / "));
        assertEquals(66, TrainingDashboardModel.validation(s, TrainingDashboardTest.view(s).settings()).percent());
        assertTrue(TrainingDashboardModel.validation(s, TrainingDashboardTest.view(s).settings()).value().contains("128 / 192"));
    }
    @Test void everyLossRegimeUsesBackendObjectiveAndRetainsComponentEvidence() {
        for (var objective : java.util.List.of(BrnSupervision.WDL, BrnSupervision.blended(1), BrnSupervision.blended(.75))) {
            var r = row(1, objective); var table = new TrainingHistory.Records(); table.show(java.util.List.of(r));
            String metric = table.getValueAt(0, 2).toString();
            assertEquals("Metric", table.getColumnName(2)); assertTrue(metric.contains("0.023456")); assertTrue(metric.contains("0.025678"));
            assertTrue(metric.contains("-0.002222")); assertEquals("—", table.getValueAt(0, 3)); assertEquals("PROMOTED", table.getValueAt(0, 4));
            assertEquals(.023456 - .025678, TrainingComparison.trend(r));
            if (objective.teacherWeight() == .75) { assertTrue(metric.contains("WDL ↓ C 0.220000")); assertTrue(metric.contains("NNUE ↓ C 0.012000")); }
        }
        var game = HistoryFixtures.record(3, false, 4, Instant.EPOCH, 1L);
        assertTrue(TrainingComparison.metric(game).contains("lower")); assertEquals("BEST RETAINED", TrainingComparison.outcome(game));
    }
    @Test void trendSeparatesScalesAndBlendWeightsAndDoesNotReconnectOldRegimes() {
        var a = row(1, BrnSupervision.WDL); var b = row(2, BrnSupervision.blended(1));
        var c = row(3, BrnSupervision.blended(.75)); var d = row(4, BrnSupervision.blended(.5));
        assertEquals(java.util.List.of(d), TrainingComparison.latestRegime(java.util.List.of(a, b, c, d)));
        assertEquals(java.util.List.of(row(5, BrnSupervision.WDL)).size(), TrainingComparison.latestRegime(java.util.List.of(a, b, a)).size());
    }
    @Test void settingsCopiesAndPreferencesKeepTimeLimitOutOfCompatibility() throws Exception {
        var s = TrainingDashboardTest.settings(Path.of("build/unused-presentation-store")).withTimeLimit(15);
        var c = s.config(TrainerConfig.DepthChange.REQUIRE_SAME);
        assertEquals(900_000, c.maximumRunMillis());
        assertEquals(c.attemptSettings(1, TrainingSource.SELF_PLAY), c.withTimeLimit(Duration.ofMinutes(5)).attemptSettings(1, TrainingSource.SELF_PLAY));
        assertEquals(15, s.withTeacherStore(null).withSource(null).withSupervision(null).withRunSeeds(null).maximumRunMinutes());
        var prefs = java.util.prefs.Preferences.userRoot().node("seedv6-run-limit-test-" + UUID.randomUUID());
        try { s.save(prefs); assertEquals(15, TrainingSettings.load(prefs).maximumRunMinutes()); }
        finally { prefs.removeNode(); }
    }
    @Test void dashboardAndHistoryRenderAtSupportedWidthsWithoutAnyStoreOrWindow() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SeedTheme.initialize();
            for (var objective : Arrays.asList(null, BrnSupervision.WDL, BrnSupervision.blended(1), BrnSupervision.blended(.75))) {
                var gameConfig = TrainingDashboardTest.settings(Path.of("build/unused-presentation-store")).config(TrainerConfig.DepthChange.REQUIRE_SAME);
                var snapshot = objective == null ? TrainingDashboardTest.snapshot(TrainerSnapshot.State.RECORDING_DECISION, true, true, false)
                        .withRun(Optional.of(new TrainerSnapshot.RunDetails(gameConfig, TrainingSource.SELF_PLAY, BrnSupervision.WDL,
                                184, 0, "Start", false, 768)), Optional.empty()) : snapshot(objective, 10);
                var base = TrainingDashboardTest.view(snapshot);
                var rows = objective == null ? java.util.List.of(HistoryFixtures.record(1, true, 4, Instant.EPOCH, 1L),
                        HistoryFixtures.record(2, false, 4, Instant.EPOCH, 1L))
                        : java.util.List.of(row(1, objective), row(2, objective), row(3, objective));
                var view = new TrainingController.ViewState(base.settings(), base.phase(), snapshot, "Synthetic presentation fixture", true,
                        false, true, 4, "", new HistoryRepository.Snapshot(rows, java.util.List.of()), "");
                for (int width : new int[]{980, 1500}) {
                    var dashboard = new TrainingDashboard(); dashboard.showState(view);
                    String name = objective == null ? "game" : Double.toString(objective.teacherWeight());
                    render(dashboard, width, name);
                    var table = find(dashboard, JTable.class);
                    assertEquals(SwingConstants.CENTER, ((JLabel)table.prepareRenderer(table.getCellRenderer(0, 2), 0, 2)).getHorizontalAlignment());
                    var history = (TrainingHistory) dashboard.historyView().getViewport().getView();
                    render(history, width, "history-" + name);
                    assertEquals(8, find(history, JTable.class).getColumnCount());
                }
            }
        });
    }
    static void render(JComponent component, int width, String name) {
        component.setSize(width, component.getPreferredSize().height); layout(component);
        var image = new BufferedImage(width, component.getHeight(), BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics(); graphics.setColor(SeedTheme.BACKGROUND); graphics.fillRect(0, 0, width, image.getHeight());
        component.printAll(graphics); graphics.dispose();
        try { Path output = Path.of("build/gui-smoke/run-control"); Files.createDirectories(output);
            ImageIO.write(image, "png", output.resolve(name+"-"+width+".png").toFile());
        } catch (java.io.IOException e) { throw new AssertionError(e); }
    }
    static void layout(Container c) { c.doLayout(); for (var child : c.getComponents()) if (child instanceof Container container) layout(container); }
    static <T> T find(Container c, Class<T> type) {
        for (var child : c.getComponents()) {
            if (type.isInstance(child)) return type.cast(child);
            if (child instanceof Container container) { T found = find(container, type); if (found != null) return found; }
        }
        return null;
    }
}
