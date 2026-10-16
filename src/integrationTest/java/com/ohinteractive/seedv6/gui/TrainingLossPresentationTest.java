package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.time.*;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.training.history.*;
import com.ohinteractive.seedv6.training.selfplay.SelfPlayTraining;
import com.ohinteractive.seedv6.training.service.*;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;
import static org.junit.jupiter.api.Assertions.*;

class TrainingLossPresentationTest {
    private static GenerationRecord row(int generation, String id, String incumbent, double loss, String objective) {
        var r = HistoryFixtures.record(generation, false, 4, Instant.EPOCH, 20_000_000_000L);
        return new GenerationRecord(generation, id, incumbent, incumbent, r.outcome(), r.decision(), r.wins(), r.draws(), r.losses(),
                r.validPairs(), r.incompletePairs(), r.score(), r.lowerBound(), r.threshold(),
                new GenerationRecord.Regime(4, 32, 32, 6, "TRAINING_DATA", "GAME_PAIRS", objective == null ? null : "recipe|finalLossObjective=" + objective),
                0, 0, 1024L, loss, Instant.EPOCH, Instant.EPOCH, 0L, 10L, 10L, 20_000_000_000L);
    }
    private static TrainingController.ViewState view(NetworkArchitecture architecture, TrainerSnapshot.State state, boolean completed, String finalObjective) {
        var settings = TrainingSettings.defaults(Path.of("build/unused-loss-presentation"), architecture).withValidationMethod(ValidationMethod.GAME_PAIRS);
        var base = TrainingDashboardTest.snapshot(state, false, false, false);
        var statistics = completed ? Optional.of(new SelfPlayTraining.Statistics(128, 1, 0, 1, .2, .120590, .623380, 0, 0, false))
                : Optional.<SelfPlayTraining.Statistics>empty();
        var s = new TrainerSnapshot(state, "", Duration.ofSeconds(488), 187, base.bestId(), base.latestTrainingId(), completed ? base.candidateId() : "",
                128, 4, base.selfPlay(), statistics, 1, 128, .623380, base.validation(), base.assessment(), base.totals());
        var run = new TrainerSnapshot.RunDetails(settings.config(TrainerConfig.DepthChange.REQUIRE_SAME),
                settings.source() == null ? TrainingSource.SELF_PLAY : settings.source(), BrnSupervision.WDL, 180, 200, "Resume", false, 256, true,
                new TrainerSnapshot.GenerationTiming(187, 487_000_000_000L, 0, false), null, .01, null, finalObjective);
        s = s.withRun(Optional.of(run), Optional.empty());
        var history = new HistoryRepository.Snapshot(List.of(row(181, base.bestId(), "bootstrap", .107815, null),
                row(186, "g000186-s000000001-" + "c".repeat(64), base.bestId(), .120590, null)), List.of());
        return new TrainingController.ViewState(settings, s.running() ? TrainingController.Phase.RUNNING : TrainingController.Phase.STOPPED,
                s, "Synthetic fixture", s.running(), !s.running(), true, 4, "", history, "");
    }
    @Test void objectivesFollowArchitectureAndRecordedRecipeWithoutGuessingMaterialNnueHistory() {
        var s = view(NetworkArchitecture.NNUE_MATERIAL, TrainerSnapshot.State.TRAINING, false, "cross-entropy (nats/example)").snapshot();
        assertEquals(TrainingLoss.Objective.CROSS_ENTROPY, TrainingLoss.runningObjective(s, NetworkArchitecture.NNUE_MATERIAL));
        assertEquals(TrainingLoss.Objective.CROSS_ENTROPY, TrainingLoss.currentFinalObjective(s, NetworkArchitecture.NNUE_MATERIAL));
        assertEquals(TrainingLoss.Objective.UNKNOWN, TrainingLoss.recordedObjective(null, NetworkArchitecture.NNUE_MATERIAL));
        assertEquals(TrainingLoss.Objective.HALF_SQUARED, TrainingLoss.recordedObjective(row(1, "one", "zero", .1,
                "half-squared target error (target units squared/example)"), NetworkArchitecture.NNUE_MATERIAL));
        assertEquals(TrainingLoss.Objective.UNKNOWN, TrainingLoss.recordedObjective(row(1, "one", "zero", .1, "future-objective"), NetworkArchitecture.BRN_PAIR2));
        for (var architecture : NetworkArchitecture.values()) if (architecture != NetworkArchitecture.NNUE_MATERIAL) {
            assertEquals(TrainingLoss.Objective.HALF_SQUARED, TrainingLoss.currentFinalObjective(null, architecture));
            assertEquals(architecture == NetworkArchitecture.BRN_PAIR2 ? TrainingLoss.Objective.CROSS_ENTROPY : TrainingLoss.Objective.HALF_SQUARED,
                    TrainingLoss.runningObjective(null, architecture));
        }
    }
    @Test void completedSnapshotFitCannotBeAttachedToAnotherGeneration() {
        var s = view(NetworkArchitecture.BRN_PAIR2, TrainerSnapshot.State.VALIDATING, true, null).snapshot();
        var wrong = new TrainerSnapshot(s.state(), "", s.elapsed(), s.generation() + 1, s.bestId(), s.latestTrainingId(), s.candidateId(),
                s.optimizerStep(), s.trainingDepth(), s.selfPlay(), s.training(), 0, 0, Double.NaN, s.validation(), s.assessment(), s.totals());
        var loss = new TrainingLoss(); loss.update(HistoryRepository.Snapshot.EMPTY);
        assertNull(loss.finalLoss(s.candidateId(), wrong)); assertEquals(.120590, loss.finalLoss(s.candidateId(), s));
    }
    @Test void liveAndCompletedObjectivesStaySeparatedAcrossTrainingPublicationAndNextGeneration() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SeedTheme.initialize(); var dashboard = new TrainingDashboard();
            dashboard.showState(view(NetworkArchitecture.BRN_PAIR2, TrainerSnapshot.State.TRAINING, false, null));
            assertEquals("Cross-entropy", named(dashboard, "candidateTrainingLossKind", JLabel.class).getText());
            assertEquals("0.623380", named(dashboard, "candidateTrainingLoss", JLabel.class).getText());
            for (String role : List.of("latestCompletedLoss", "bestTrainingLoss"))
                assertEquals("Half-squared error", named(dashboard, role + "Kind", JLabel.class).getText());
            assertFalse(SwingUtilities.isDescendingFrom(named(dashboard, "candidateTrainingLoss", JLabel.class),
                    named(dashboard, "completedModelFit", JPanel.class)));
            dashboard.showState(view(NetworkArchitecture.BRN_PAIR2, TrainerSnapshot.State.VALIDATING, true, null));
            assertEquals("Current model fit", named(dashboard, "currentLossHeading", JLabel.class).getText());
            assertEquals("Half-squared error", named(dashboard, "candidateTrainingLossKind", JLabel.class).getText());
            assertEquals("0.120590", named(dashboard, "candidateTrainingLoss", JLabel.class).getText());
            dashboard.showState(TrainingDashboardTest.view(TrainingDashboardTest.snapshot(TrainerSnapshot.State.GENERATING_SELF_PLAY, true, true, true)));
            assertEquals("Unavailable", named(dashboard, "candidateTrainingLoss", JLabel.class).getText());
            dashboard.showState(view(NetworkArchitecture.BRN3, TrainerSnapshot.State.TRAINING, false, null));
            assertEquals("Half-squared error", named(dashboard, "candidateTrainingLossKind", JLabel.class).getText(), "BRN-3 reports half-squared progress even though its optimizer uses CE");
            dashboard.showState(view(NetworkArchitecture.NNUE_MATERIAL, TrainerSnapshot.State.TRAINING, false, "cross-entropy (nats/example)"));
            assertEquals("Cross-entropy", named(dashboard, "candidateTrainingLossKind", JLabel.class).getText());
            assertEquals("Objective unavailable", named(dashboard, "bestTrainingLossKind", JLabel.class).getText());
        });
    }
    @Test void renderHeaderTimersAndObjectiveGroupsAtNormalAndHighDpiAndNarrowWidths() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SeedTheme.initialize();
            try {
                for (float scale : new float[]{1, 1.5f, 2}) {
                    com.formdev.flatlaf.util.UIScale.setZoomFactor(scale);
                    for (int width : new int[]{700, 980, 1500}) {
                        var dashboard = new TrainingDashboard();
                        dashboard.showState(view(NetworkArchitecture.BRN_PAIR2, TrainerSnapshot.State.TRAINING, false, null));
                        dashboard.setSize(SeedTheme.scale(width), dashboard.getPreferredSize().height);
                        TrainingRunPresentationTest.layout(dashboard);
                        for (String name : List.of("campaignElapsed", "generationElapsed")) {
                            var label = named(dashboard, name, JLabel.class);
                            var bounds = SwingUtilities.convertRectangle(label.getParent(), label.getBounds(), dashboard);
                            assertTrue(label.getFont().getSize() >= SeedTheme.scale(17)); assertTrue(label.getFont().isBold());
                            assertEquals(name.equals("campaignElapsed") ? "00:08:08" : "00:08:07", label.getText());
                            assertTrue(bounds.y + bounds.height < SeedTheme.scale(90), name + " must stay in the header");
                            assertTrue(bounds.x >= 0 && bounds.x + bounds.width <= dashboard.getWidth());
                            assertTrue(label.getWidth() >= label.getPreferredSize().width, name + " must not clip");
                        }
                        for (String name : List.of("candidateTrainingLossKind", "latestCompletedLossKind", "bestTrainingLossKind")) {
                            var label = named(dashboard, name, JLabel.class);
                            assertTrue(label.getFont().isBold());
                            assertTrue(label.getWidth() >= label.getPreferredSize().width, name + " must remain readable");
                        }
                        assertTrue(named(dashboard, "comparisonTrend", JComponent.class).getHeight() >= SeedTheme.scale(230));
                        capture(dashboard, "timers-loss-" + width + "-" + scale + ".png");
                    }
                }
            } finally { com.formdev.flatlaf.util.UIScale.setZoomFactor(1); }
        });
    }
    private static void capture(JComponent component, String name) {
        try {
            var image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_RGB);
            var g = image.createGraphics(); g.setColor(SeedTheme.BACKGROUND); g.fillRect(0, 0, image.getWidth(), image.getHeight()); component.paint(g); g.dispose();
            Path folder = Path.of("build/gui-smoke/training-timers-loss"); Files.createDirectories(folder); ImageIO.write(image, "png", folder.resolve(name).toFile());
        } catch (Exception error) { throw new AssertionError(error); }
    }
}
