package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.training.service.*;
import com.ohinteractive.seedv6.training.history.*;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;
import static org.junit.jupiter.api.Assertions.*;

class WorkflowRefinementGuiTest {
    @TempDir Path temp;
    @Test void independentSelectorsAndPreferencesRemainEditableAtSafeBoundaries() throws Exception {
        var settings = new TrainingSettings(temp, 1, 1, 4, 0, 0, 2, 1, 1, 2, 71, 4, 1,
                NetworkArchitecture.BRN2).withSource(TrainingSource.HANDCRAFTED).withValidationMethod(ValidationMethod.GAME_PAIRS);
        var panel = edt(() -> new TrainingPanel(settings));
        until(() -> edt(() -> named(panel, "brnTrainingSource", JComboBox.class).isEnabled()));
        edt(() -> {
            var source = named(panel, "brnTrainingSource", JComboBox.class);
            var validator = named(panel, "trainingValidationMethod", JComboBox.class);
            assertEquals(TrainingSource.Mode.HANDCRAFTED, source.getSelectedItem());
            assertEquals(ValidationMethod.GAME_PAIRS, validator.getSelectedItem());
            validator.setSelectedItem(ValidationMethod.HELD_OUT);
            assertEquals(TrainingSource.Mode.HANDCRAFTED, source.getSelectedItem());
            source.setSelectedItem(TrainingSource.Mode.SELF_PLAY);
            assertEquals(ValidationMethod.HELD_OUT, validator.getSelectedItem());
            assertFalse(named(panel, "trainingPairs", JSpinner.class).isEnabled());
            validator.setSelectedItem(ValidationMethod.GAME_PAIRS);
            assertEquals(TrainingSource.Mode.SELF_PLAY, source.getSelectedItem());
            assertTrue(named(panel, "trainingPairs", JSpinner.class).isEnabled());
            var state = TrainingDashboardTest.view(TrainingDashboardTest.snapshot(TrainerSnapshot.State.TRAINING, false, false, false));
            panel.showState(state); assertFalse(source.isEnabled()); assertFalse(validator.isEnabled());
        });
        Preferences prefs = Preferences.userRoot().node("seedv6-test/independent-" + UUID.randomUUID());
        try {
            settings.save(prefs); var loaded = TrainingSettings.load(prefs);
            assertEquals(settings.source(), loaded.source()); assertEquals(ValidationMethod.GAME_PAIRS, loaded.validationMethod());
            assertEquals(ValidationMethod.GAME_PAIRS, loaded.withSource(TrainingSource.SELF_PLAY).validationMethod());
            prefs.remove("validationMethod"); assertNotNull(TrainingSettings.load(prefs));
        } finally { prefs.removeNode(); }
    }
    @Test void legacyStoreRestoresItsValidatorOnceWithoutCouplingLaterSourceEdits() throws Exception {
        Path root = temp.resolve("legacy"); PlayStoreWorkflowTest.brnStore(root);
        var settings = new TrainingSettings(root, 1, 1, 4, 0, 0, 2, 1, 1, 2, 71, 4, 1, NetworkArchitecture.BRN2);
        var panel = edt(() -> new TrainingPanel(settings));
        until(() -> edt(() -> named(panel, "trainingValidationMethod", JComboBox.class).getSelectedItem() == ValidationMethod.GAME_PAIRS
                && named(panel, "brnTrainingSource", JComboBox.class).isEnabled()));
        edt(() -> {
            var source = named(panel, "brnTrainingSource", JComboBox.class);
            assertEquals(TrainingSource.Mode.SELF_PLAY, source.getSelectedItem());
            source.setSelectedItem(TrainingSource.Mode.HANDCRAFTED);
            assertEquals(ValidationMethod.GAME_PAIRS, named(panel, "trainingValidationMethod", JComboBox.class).getSelectedItem());
        });
    }
    static TrainingController.ViewState liveView() {
        var snapshot = TrainingDashboardRefinementTest.live(14, 5, 13, 100);
        var settings = TrainingDashboardTest.settings(Path.of("build/unused-dashboard-store"));
        var config = settings.config(TrainerConfig.DepthChange.REQUIRE_SAME).withValidationMethod(ValidationMethod.GAME_PAIRS);
        snapshot = snapshot.withRun(Optional.of(new TrainerSnapshot.RunDetails(config, TrainingSource.SELF_PLAY,
                BrnSupervision.WDL, 140, 267, "Resume", false, 768, true,
                new TrainerSnapshot.GenerationTiming(187, 239_000_000_000L, 0, false))), Optional.empty());
        var view = TrainingDashboardTest.view(snapshot);
        var records = new ArrayList<GenerationRecord>();
        for (int g = 164; g < 187; g++) records.add(HistoryFixtures.record(g, false, 4, java.time.Instant.EPOCH, 258_000_000_000L));
        return new TrainingController.ViewState(settings, view.phase(), snapshot, "Representative active validation", true,
                false, true, 4, "", new HistoryRepository.Snapshot(records, List.of()), "");
    }
    @Test void compactDashboardShowsBothGraphsAtOrdinaryViewportAndRendersAt150Percent() throws Exception {
        edt(() -> {
            SeedTheme.initialize();
            try {
                for (float scale : new float[]{1, 1.5f}) {
                    com.formdev.flatlaf.util.UIScale.setZoomFactor(scale);
                    var dashboard = new TrainingDashboard(); dashboard.showState(liveView());
                    var scroll = new JScrollPane(dashboard); scroll.setBorder(null);
                    scroll.setSize(760, 649); layout(scroll); layout(scroll);
                    var graphs = named(dashboard, "runtimeGraphs", JPanel.class);
                    int bottom = SwingUtilities.convertPoint(graphs, 0, graphs.getHeight(), dashboard).y;
                    capture(scroll, "dashboard-viewport-" + scale + ".png");
                    if (scale == 1) assertTrue(bottom <= scroll.getViewport().getHeight(),
                            "Graphs bottom=" + bottom + " viewport=" + scroll.getViewport().getHeight());
                    assertEquals(0, scroll.getViewport().getViewPosition().y);
                    var previous = named(dashboard, "previousGenerationDuration", JLabel.class);
                    var comparisonCard = previous.getParent().getParent();
                    assertTrue(SwingUtilities.convertRectangle(previous.getParent(), previous.getBounds(), comparisonCard)
                            .getMaxY() <= comparisonCard.getHeight(), "Previous duration is clipped at scale " + scale);
                    assertTrue(previous.getHeight() >= previous.getPreferredSize().height);
                    assertEquals("28", named(dashboard, "candidateWins", JLabel.class).getText());
                    assertTrue(named(dashboard, "previousGenerationDuration", JLabel.class).getText().contains("00:04:18"));
                    capture(scroll, "dashboard-viewport-" + scale + ".png");
                    dashboard.setSize(760, dashboard.getPreferredSize().height); layout(dashboard);
                    capture(dashboard, "dashboard-full-" + scale + ".png");
                    System.out.println("DASHBOARD_LAYOUT scale=" + scale + " graphBottom=" + bottom + " viewport=649");
                }
            } finally { com.formdev.flatlaf.util.UIScale.setZoomFactor(1); }
        });
    }
    static void layout(Container c) { c.doLayout(); for (var child : c.getComponents()) if (child instanceof Container p) layout(p); }
    static void capture(JComponent component, String name) {
        try {
            var image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_RGB);
            var g = image.createGraphics(); g.setColor(SeedTheme.BACKGROUND); g.fillRect(0, 0, image.getWidth(), image.getHeight());
            component.paint(g); g.dispose(); Path folder = Path.of("build/gui-smoke/workflow-refinement"); Files.createDirectories(folder);
            ImageIO.write(image, "png", folder.resolve(name).toFile());
        } catch (Exception failure) { throw new AssertionError(failure); }
    }
}
