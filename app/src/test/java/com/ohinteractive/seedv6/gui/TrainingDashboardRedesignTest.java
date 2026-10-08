package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.training.history.*;
import com.ohinteractive.seedv6.training.selfplay.SelfPlayTraining;
import com.ohinteractive.seedv6.training.service.*;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;
import static org.junit.jupiter.api.Assertions.*;

class TrainingDashboardRedesignTest {
    private static GenerationRecord loss(GenerationRecord r, Double value) {
        return new GenerationRecord(r.generation(), r.candidate(), r.incumbent(), r.resultingBest(), r.outcome(), r.decision(),
                r.wins(), r.draws(), r.losses(), r.validPairs(), r.incompletePairs(), r.score(), r.lowerBound(), r.threshold(),
                r.regime(), r.completedGames(), r.abortedGames(), r.samples(), value, r.started(), r.completed(), r.selfPlayNanos(),
                r.trainingNanos(), r.validationNanos(), r.totalNanos(), r.bootstrap(), r.rawPromotionThreshold());
    }
    private static TrainingController.ViewState view(TrainerSnapshot s, HistoryRepository.Snapshot history) {
        var base = TrainingDashboardTest.view(s);
        return new TrainingController.ViewState(base.settings(), base.phase(), s, s.failed() ? "Fixture failure: publication failed" : "Fixture",
                base.active(), base.canStart(), base.resume(), 4, "", history, "");
    }
    private static TrainerSnapshot statistics(TrainerSnapshot s, double finalLoss, boolean cancelled) {
        return new TrainerSnapshot(s.state(), s.failureSummary(), s.elapsed(), s.generation(), s.bestId(), s.latestTrainingId(), s.candidateId(),
                s.optimizerStep(), s.trainingDepth(), s.selfPlay(), Optional.of(new SelfPlayTraining.Statistics(768, 24, 0, 24,
                .3, finalLoss, .0412, 0, 0, cancelled)), s.generationOptimizerUpdates(), s.generationSamplesTrained(), s.meanTrainingLoss(),
                s.validation(), s.assessment(), s.totals(), s.validationProgress(), s.validationDetails(), s.activeGame(),
                s.bootstrapValidation(), s.run(), s.lossProgress());
    }
    @Test void numericalObjectivesNeverBecomePercentagesAndTinyLossesStayVisible() {
        assertEquals("0.023456", TrainingLoss.format(.023456));
        assertEquals("0.000000", TrainingLoss.format(0.));
        assertEquals("2.30000e-09", TrainingLoss.format(2.3e-9));
        for (Double value : new Double[]{null, Double.NaN, Double.POSITIVE_INFINITY, -1.})
            assertEquals("Unavailable", TrainingLoss.format(value));
        assertFalse(TrainingLoss.format(1.23).contains("%"));
    }
    @Test void latestCompletionIsIndependentOfBestAndMissingHistoryIsNeverBackfilled() {
        var first = loss(HistoryFixtures.record(1, true, 4, Instant.EPOCH, 1L), .12);
        var second = loss(HistoryFixtures.record(2, false, 4, Instant.EPOCH, 1L, first.candidate()), null);
        var losses = new TrainingLoss(); losses.update(new HistoryRepository.Snapshot(List.of(first, second), List.of()));
        assertSame(second, losses.latest()); assertEquals(.12, losses.finalLoss(first.candidate(), null));
        assertNull(losses.finalLoss(second.candidate(), null)); assertNull(losses.finalLoss("unknown", null));
        losses.update(HistoryRepository.Snapshot.EMPTY);
        assertNull(losses.latest()); assertNull(losses.finalLoss(first.candidate(), null));
    }
    @Test void snapshotFinalFitAndHeldOutEvidenceUseExactModelIdentities() {
        var s = statistics(TrainingRunPresentationTest.snapshot(BrnSupervision.WDL, 10), .012345, false);
        var losses = new TrainingLoss(); losses.update(HistoryRepository.Snapshot.EMPTY);
        assertEquals(.012345, losses.finalLoss(s.candidateId(), s));
        assertNull(losses.finalLoss(s.bestId(), s));
        assertEquals(.023456, losses.validation(s.candidateId(), s).value());
        assertEquals(.025678, losses.validation(s.bestId(), s).value());
        assertNull(losses.validation("unrelated", s));
        assertNull(losses.finalLoss(s.candidateId(), statistics(s, .0001, true)), "Cancelled fit is not a completed model fit");
        var next = TrainingDashboardTest.snapshot(TrainerSnapshot.State.GENERATING_SELF_PLAY, true, true, true)
                .withBootstrapValidation(s.bootstrapValidation());
        assertEquals(187, losses.validation(s.candidateId(), next).generation(), "Retained validation belongs to its original generation");
        assertNull(losses.finalLoss(next.candidateId(), next));
    }
    @Test void positionProgressFollowsActualPhaseAndEffectiveSourceAcrossStopResume() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var dashboard = new TrainingDashboard();
            for (var phase : TrainerSnapshot.State.values()) {
                var s = TrainingDashboardTest.snapshot(phase, false, false, false);
                var v = TrainingDashboardTest.view(s); dashboard.showState(v);
                assertEquals(phase == TrainerSnapshot.State.GENERATING_SELF_PLAY,
                        named(dashboard, "selfPlayProgress", JProgressBar.class).isVisible(), phase.toString());
            }
            var s = TrainingDashboardTest.snapshot(TrainerSnapshot.State.GENERATING_SELF_PLAY, false, false, false);
            var base = TrainingDashboardTest.view(s);
            for (var phase : List.of(TrainingController.Phase.STARTING, TrainingController.Phase.STOPPING, TrainingController.Phase.CLOSING)) {
                dashboard.showState(new TrainingController.ViewState(base.settings(), phase, s, "Stopping", true, false, true, 4, ""));
                assertFalse(named(dashboard, "selfPlayProgress", JProgressBar.class).isVisible());
            }
            for (var source : List.of(TrainingSource.FROZEN_REPLAY, TrainingSource.corpus(Path.of("unused")))) {
                var run = new TrainerSnapshot.RunDetails(base.settings().config(TrainerConfig.DepthChange.REQUIRE_SAME), source,
                        BrnSupervision.WDL, 187, 187, "Resume", false, 0, false);
                dashboard.showState(TrainingDashboardTest.view(s.withRun(Optional.of(run), Optional.empty())));
                assertFalse(named(dashboard, "selfPlayProgress", JProgressBar.class).isVisible());
            }
            dashboard.showState(base); assertTrue(named(dashboard, "selfPlayProgress", JProgressBar.class).isVisible());
        });
    }
    @Test void prominentValuesDistinguishFinalTrainingAndValidationAndClearOnLineageLoad() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var s = statistics(TrainingRunPresentationTest.snapshot(BrnSupervision.WDL, 10), .012345, false);
            var row = TrainingRunPresentationTest.row(186, BrnSupervision.WDL);
            var history = new HistoryRepository.Snapshot(List.of(row), List.of());
            var dashboard = new TrainingDashboard(); dashboard.showState(view(s, history));
            assertEquals("0.012345", named(dashboard, "candidateTrainingLoss", JLabel.class).getText());
            assertEquals("0.030000", named(dashboard, "latestCompletedLoss", JLabel.class).getText());
            assertEquals("0.025678", named(dashboard, "bestTrainingLoss", JLabel.class).getText());
            assertTrue(named(dashboard, "bestTrainingLossKind", JLabel.class).getText().contains("Validation"));
            assertTrue(named(dashboard, "candidateTrainingLossKind", JLabel.class).getText().contains("final"));
            var base = view(s, history);
            dashboard.showState(new TrainingController.ViewState(base.settings(), base.phase(), s, "Loading", false, false, true, 4, "",
                    history, "", "Start Training", null, true, 0));
            for (String name : List.of("candidateTrainingLoss", "latestCompletedLoss", "bestTrainingLoss"))
                assertEquals("Unavailable", named(dashboard, name, JLabel.class).getText());
            assertEquals(0, named(dashboard, "recentTrainingHistory", JTable.class).getRowCount());
        });
    }
    @Test void partialMeanIsNotDisplayedAsFinalCandidateLossAndErrorsStayVisible() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var dashboard = new TrainingDashboard();
            var s = TrainingDashboardTest.snapshot(TrainerSnapshot.State.TRAINING, false, false, true);
            dashboard.showState(TrainingDashboardTest.view(s));
            assertEquals("0.041200", named(dashboard, "candidateTrainingLoss", JLabel.class).getText());
            assertTrue(named(dashboard, "candidateTrainingLossKind", JLabel.class).getText().contains("mean so far"));
            s = TrainingDashboardTest.snapshot(TrainerSnapshot.State.GENERATING_SELF_PLAY, true, true, true);
            dashboard.showState(TrainingDashboardTest.view(s));
            assertEquals("Unavailable", named(dashboard, "candidateTrainingLoss", JLabel.class).getText());
            s = TrainingDashboardTest.snapshot(TrainerSnapshot.State.FAILED, false, false, false);
            dashboard.showState(view(s, HistoryRepository.Snapshot.EMPTY));
            assertTrue(named(dashboard, "trainingDashboardNotice", JTextArea.class).getText().contains("publication failed"));
            assertTrue(named(dashboard, "trainingDashboardNotice", JTextArea.class).isVisible());
        });
    }
    @Test void graphSpaceGrowsAndActivityIsRemovedWhileDiagnosticsRemain() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SeedTheme.initialize();
            var dashboard = new TrainingDashboard();
            dashboard.showState(TrainingDashboardTest.view(TrainingDashboardTest.snapshot(TrainerSnapshot.State.VALIDATING, false, false, false)));
            dashboard.setSize(700, dashboard.getPreferredSize().height); TrainingRunPresentationTest.layout(dashboard);
            var graph = named(dashboard, "comparisonTrend", JComponent.class); int initial = graph.getHeight();
            assertTrue(initial >= SeedTheme.scale(230));
            dashboard.setSize(700, dashboard.getHeight() + 200); TrainingRunPresentationTest.layout(dashboard);
            assertTrue(graph.getHeight() >= initial + 190, "Graphs receive spare vertical space");
            assertFalse(named(dashboard, "effectiveTrainingConfiguration", JPanel.class).getParent().isVisible());
            named(dashboard, "trainingConfigurationDetails", JToggleButton.class).doClick();
            assertTrue(named(dashboard, "effectiveTrainingConfiguration", JPanel.class).getParent().isVisible());
            boolean[] opened = {false}; var board = new TrainingBoard(() -> opened[0] = true);
            assertNull(named(board, "trainingActivity", JTextArea.class));
            named(board, "trainingBoardDiagnostics", JButton.class).doClick(); assertTrue(opened[0]);
        });
    }
    @Test void completedFitBeforePublicationAndRoleMergingDoNotLoseIdentity() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var dashboard = new TrainingDashboard();
            var unpublished = statistics(TrainingDashboardTest.snapshot(TrainerSnapshot.State.PUBLISHING_CANDIDATE, false, false, true), .123456, false);
            dashboard.showState(view(unpublished, HistoryRepository.Snapshot.EMPTY));
            assertEquals("0.123456", named(dashboard, "candidateTrainingLoss", JLabel.class).getText());
            assertTrue(named(dashboard, "candidateTrainingLossKind", JLabel.class).getText().contains("final"));
            var s = statistics(TrainingDashboardTest.snapshot(TrainerSnapshot.State.STOPPED, true, true, false), .14, false);
            var original = HistoryFixtures.record(187, true, 4, Instant.EPOCH, 1L);
            var row = new GenerationRecord(187, s.candidateId(), original.incumbent(), s.candidateId(), original.outcome(), original.decision(),
                    original.wins(), original.draws(), original.losses(), original.validPairs(), original.incompletePairs(), original.score(),
                    original.lowerBound(), original.threshold(), original.regime(), 32, 0, 1024L, .14, null, Instant.EPOCH,
                    null, null, null, 1L);
            dashboard.showState(view(s, new HistoryRepository.Snapshot(List.of(row), List.of())));
            assertTrue(named(dashboard, "latestCompletedLossIdentity", JLabel.class).getText().contains("Latest completed / Candidate / Best"));
            assertFalse(named(dashboard, "bestTrainingLoss", JLabel.class).getParent().isVisible());
            assertFalse(named(dashboard, "candidateTrainingLoss", JLabel.class).getParent().isVisible());
            dashboard.showState(view(s, HistoryRepository.Snapshot.EMPTY));
            assertTrue(named(dashboard, "candidateTrainingLoss", JLabel.class).getParent().isVisible());
            assertTrue(named(dashboard, "candidateTrainingLossIdentity", JLabel.class).getText().startsWith("Candidate / Best"));
        });
    }
}
