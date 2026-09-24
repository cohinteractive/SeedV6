package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.time.*;
import java.util.List;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.training.history.*;
import com.ohinteractive.seedv6.training.service.*;
import com.ohinteractive.seedv6.training.validation.*;
import static com.ohinteractive.seedv6.gui.TrainingDashboardModel.*;
import static com.ohinteractive.seedv6.training.history.HistoryFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class TrainingDashboardRefinementTest {
    @Test void campaignUsesDisplayedOrdinalWithoutWithinGenerationFractions() {
        assertEquals(.375, campaignFraction(48, 128));
        assertEquals(0, campaignFraction(0, 128)); assertEquals(0, campaignFraction(-1, 128));
        assertEquals(1, campaignFraction(128, 128)); assertEquals(1, campaignFraction(129, 128));
        assertEquals(0, campaignFraction(48, 0)); assertEquals(0, campaignFraction(null));
        var s = TrainingRunPresentationTest.snapshot(BrnSupervision.WDL, 10);
        assertEquals(.4, campaignFraction(s)); assertTrue(runLabel(s).startsWith("Run 4 / 10"));
    }
    @Test void previousDurationMustBelongToImmediatelyPrecedingGeneration() {
        var a = record(45, false, 4, Instant.EPOCH, 258_000_000_000L);
        var b = record(46, false, 4, Instant.EPOCH, null);
        assertSame(a, previousGeneration(List.of(a, b), 46));
        assertEquals(258_000_000_000L, previousGeneration(List.of(a), 46).totalNanos());
        assertNull(previousGeneration(List.of(a), 47)); assertNull(previousGeneration(List.of(), 1));
        assertSame(b, previousGeneration(List.of(a, b), 47)); assertNull(b.totalNanos());
    }
    @Test void publishedGenerationClockIncludesSavedActiveTimeAndFreezesAtSettlement() {
        var live = new TrainerSnapshot.GenerationTiming(46, 200, 1000, true);
        assertEquals(250, live.elapsed(1050).toNanos());
        var settled = new TrainerSnapshot.GenerationTiming(46, 250, 0, false);
        assertEquals(250, settled.elapsed(1_000_000).toNanos());
        assertTrue(TrainingDashboardTest.snapshot(TrainerSnapshot.State.RECOVERING, false, false, false)
                .generationElapsed(System.nanoTime()).isEmpty());
    }
    static TrainerSnapshot live(int wins, int draws, int losses, long started) {
        return live(wins, draws, losses, started, 0, 0);
    }
    static TrainerSnapshot live(int wins, int draws, int losses, long started, int restoredWins, int restoredLosses) {
        var s = TrainingDashboardTest.snapshot(TrainerSnapshot.State.VALIDATING, false, false, false);
        int pairs = wins + draws + losses;
        var colour = new ValidationResult.ColourRecord(wins, draws, losses);
        var p = new ValidationProgress(32, pairs + 1, 1, 2 * pairs + 1, 0, ValidationProgress.CandidateColour.WHITE,
                true, false, pairs, 0, colour, colour, 2 * pairs, 0, 0, Map.of(), started, started, 0, started + pairs);
        var d = s.validationDetails().orElseThrow();
        return new TrainerSnapshot(s.state(), "", s.elapsed(), s.generation(), s.bestId(), s.latestTrainingId(), s.candidateId(),
                s.optimizerStep(), s.trainingDepth(), s.selfPlay(), s.training(), s.generationOptimizerUpdates(),
                s.generationSamplesTrained(), s.meanTrainingLoss(), Optional.empty(), Optional.empty(), s.totals(), Optional.of(p),
                Optional.of(new TrainerSnapshot.ValidationDetails(d.candidateId(), d.bestId(), d.config(), d.policy(),
                        d.candidateGeneration(), d.bestGeneration(), restoredWins, restoredLosses)));
    }
    @Test void replayOfPersistedPairsNeverPulsesButNewResultsAfterReplayDo() {
        var tracker = new WinIncreases();
        tracker.update(live(0, 0, 0, 100, 6, 4), true);
        assertEquals(new WinIncreases.Change(false, false), tracker.update(live(2, 0, 1, 100, 6, 4), true));
        assertEquals(new WinIncreases.Change(false, false), tracker.update(live(3, 0, 2, 100, 6, 4), true));
        assertEquals(new WinIncreases.Change(true, true), tracker.update(live(4, 0, 3, 100, 6, 4), true));
    }
    @Test void livePointUsesDecisionMeanAndOnlyCurrentActiveGamePairValidation() {
        var s = live(6, 2, 2, 100); var point = liveComparison(s);
        assertEquals(187, point.generation()); assertEquals(.7, point.score()); assertEquals("70.0%", match(s).score());
        assertEquals(s.validationDetails().get().policy().rawScoreThreshold(32).orElseThrow(), point.threshold());
        assertNull(liveComparison(live(0, 0, 0, 100)).score());
        assertNull(liveComparison(TrainingDashboardTest.snapshot(TrainerSnapshot.State.RECORDING_DECISION, true, false, false)));
        assertNull(liveComparison(TrainingDashboardTest.snapshot(TrainerSnapshot.State.GENERATING_SELF_PLAY, true, false, true)));
        assertNull(liveComparison(TrainingRunPresentationTest.snapshot(BrnSupervision.WDL, 10)));
    }
    @Test void pulseRequiresLaterLiveIncreaseWithSameCandidateAndValidationSession() {
        var tracker = new WinIncreases(); var first = live(3, 1, 2, 100);
        assertEquals(new WinIncreases.Change(false, false), tracker.update(first, true));
        assertEquals(new WinIncreases.Change(false, false), tracker.update(first, true));
        assertEquals(new WinIncreases.Change(true, false), tracker.update(live(4, 1, 2, 100), true));
        assertEquals(new WinIncreases.Change(false, true), tracker.update(live(4, 1, 4, 100), true));
        assertEquals(new WinIncreases.Change(false, false), tracker.update(live(4, 3, 4, 100), true));
        assertEquals(new WinIncreases.Change(false, false), tracker.update(live(5, 3, 5, 200), true));
        tracker.update(live(5, 3, 5, 200), false);
        assertEquals(new WinIncreases.Change(false, false), tracker.update(live(6, 3, 6, 200), true));
        tracker.reset(); assertEquals(new WinIncreases.Change(false, false), tracker.update(live(7, 3, 7, 200), true));
        assertEquals(new WinIncreases.Change(false, false), tracker.update(
                TrainingDashboardTest.snapshot(TrainerSnapshot.State.STOPPED, true, true, false), true));
    }
    @Test void finalizedHistoryReplacesTheProvisionalPoint() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SeedTheme.initialize(); var chart = new HistoryChart(true, 170); chart.setSize(650, 170);
            chart.showRecords(List.of(threshold(186, .7))); chart.showLive(liveComparison(live(6, 2, 2, 100)));
            var hover = new java.awt.event.MouseEvent(chart, java.awt.event.MouseEvent.MOUSE_MOVED, 0, 0, 640, 80, 0, false);
            assertTrue(chart.getToolTipText(hover).contains("provisional score 70.0%"));
            chart.showRecords(List.of(threshold(186, .7), threshold(187, .7)));
            chart.showLive(liveComparison(live(6, 2, 2, 100))); // Delayed publication cannot resurrect it.
            assertFalse(chart.getToolTipText(hover).contains("provisional"));
            assertTrue(chart.getToolTipText(hover).contains("Generation 187"));
        });
    }
    @Test void thresholdsAreSeparateHorizontalSegmentsWithLegacyAndMissingGenerationGaps() {
        var rows = List.of(threshold(21, .65), threshold(22, .65), threshold(23, .72), threshold(24, .72),
                record(25, false, 4, Instant.EPOCH, null), threshold(26, .72), threshold(28, .72));
        assertEquals(List.of(new PromotionThresholdSegments.Segment(21, 22, .65),
                new PromotionThresholdSegments.Segment(23, 24, .72), new PromotionThresholdSegments.Segment(26, 26, .72),
                new PromotionThresholdSegments.Segment(28, 28, .72)), PromotionThresholdSegments.from(rows));
        assertTrue(PromotionThresholdSegments.from(List.of(record(1, false, 4, Instant.EPOCH, null))).isEmpty());
        var large = new ArrayList<GenerationRecord>();
        for (int i = 1; i <= 2000; i++) large.add(threshold(i, i % 2 == 0 ? .65 : .72));
        assertTrue(PromotionThresholdSegments.from(large).isEmpty(), "Mixed buckets must not average regimes");
    }
    private static GenerationRecord threshold(int g, double value) {
        return withThreshold(record(g, false, 4, Instant.EPOCH, 258_000_000_000L), value);
    }
    @Test void swingPulseChangesPaintWithoutReflowAndReturnsToNormal() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        var complete = new java.util.concurrent.CompletableFuture<Void>();
        var window = new java.util.concurrent.atomic.AtomicReference<JFrame>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                SeedTheme.initialize(); var frame = new JFrame(); window.set(frame);
                var metric = new ValidationWinMetric("pulseFixture", SeedTheme.GREEN);
                var body = SeedTheme.panel(new BorderLayout()); body.setOpaque(true); body.setBackground(SeedTheme.PANEL);
                body.add(metric); frame.setContentPane(body); frame.setSize(180, 120); frame.setVisible(true);
                metric.showCount(8, false); frame.validate();
                var bounds = metric.getBounds(); var preferred = metric.getPreferredSize();
                int[] normal = pixels(metric); capture(metric, "pulse-normal.png");
                metric.showCount(8, true); capture(metric, "pulse-peak.png");
                assertFalse(Arrays.equals(normal, pixels(metric)));
                assertEquals(bounds, metric.getBounds()); assertEquals(preferred, metric.getPreferredSize());
                var finish = new javax.swing.Timer(700, e -> {
                    try {
                        assertEquals(bounds, metric.getBounds()); assertEquals(preferred, metric.getPreferredSize());
                        assertArrayEquals(normal, pixels(metric)); capture(metric, "pulse-settled.png"); complete.complete(null);
                    } catch (Throwable failure) { complete.completeExceptionally(failure); }
                });
                finish.setRepeats(false); finish.start();
            });
            complete.get(5, java.util.concurrent.TimeUnit.SECONDS);
        } finally { SwingUtilities.invokeAndWait(() -> { if (window.get() != null) window.get().dispose(); }); }
    }
    private static int[] pixels(JComponent c) {
        var image = new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics(); c.paint(g); g.dispose();
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }
    @Test void renderLiveDashboardAndCheckContentAlignmentAtTwoScales() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SeedTheme.initialize();
            try {
                for (float scale : new float[]{1f, 1.5f}) {
                    com.formdev.flatlaf.util.UIScale.setZoomFactor(scale);
                    var fixture = live(14, 5, 13, 100);
                    var config = TrainingDashboardTest.settings(Path.of("build/unused-dashboard-store")).config(TrainerConfig.DepthChange.REQUIRE_SAME);
                    config = new TrainerConfig(config.checkpointRoot(), config.masterSeed(), config.selfPlay(), config.training(),
                            config.validation(), 128, config.depthChange(), config.startingFen());
                    var run = new TrainerSnapshot.RunDetails(config, TrainingSource.SELF_PLAY, BrnSupervision.WDL, 140, 267,
                            "Resume", false, 768, true, new TrainerSnapshot.GenerationTiming(187, 239_000_000_000L, 0, false));
                    fixture = fixture.withRun(Optional.of(run), Optional.empty());
                    var base = TrainingDashboardTest.view(fixture); var rows = new ArrayList<GenerationRecord>();
                    for (int gen = 163; gen <= 186; gen++) rows.add(threshold(gen, gen < 177 ? .65 : .72));
                    var view = new TrainingController.ViewState(base.settings(), base.phase(), fixture, "Synthetic live validation fixture",
                            true, false, true, 4, "", new HistoryRepository.Snapshot(rows, List.of()), "");
                    for (int width : new int[]{980, 1500}) {
                        var dashboard = new TrainingDashboard(); dashboard.showState(view);
                        dashboard.setSize(SeedTheme.scale(width), dashboard.getPreferredSize().height); layout(dashboard);
                        var divider = TrainingWorkspaceSmokeTest.named(dashboard, "campaignProgressDivider", JComponent.class);
                        var title = TrainingWorkspaceSmokeTest.named(dashboard, "trainingHeadline", JLabel.class);
                        assertEquals(SwingUtilities.convertPoint(title, 0, 0, dashboard).x,
                                SwingUtilities.convertPoint(divider, 0, 0, dashboard).x);
                        var content = divider.getParent();
                        assertEquals(content.getHeight() / 2.0, divider.getY() + divider.getHeight() / 2.0, 1);
                        var right = TrainingWorkspaceSmokeTest.named(dashboard, "effectiveValidationMethod", JLabel.class);
                        assertEquals(SwingUtilities.convertPoint(right, right.getWidth(), 0, dashboard).x,
                                SwingUtilities.convertPoint(divider, divider.getWidth(), 0, dashboard).x);
                        assertEquals(dashboard.getWidth() - 22, divider.getWidth(), SeedTheme.scale(12));
                        var wins = TrainingWorkspaceSmokeTest.named(dashboard, "candidateWins", JLabel.class);
                        assertEquals("28", wins.getText()); assertTrue(wins.getHeight() >= SeedTheme.scale(40));
                        assertEquals("00:03:59", TrainingWorkspaceSmokeTest.named(dashboard, "generationElapsed", JLabel.class).getText());
                        capture(dashboard, "dashboard-" + width + "-" + scale + ".png");
                    }
                    var chart = new HistoryChart(true, 170); chart.setSize(SeedTheme.scale(650), SeedTheme.scale(170));
                    chart.showRecords(List.of()); chart.showLive(liveComparison(fixture)); capture(chart, "live-only-" + scale + ".png");
                    chart.showRecords(List.of(TrainingRunPresentationTest.row(186, BrnSupervision.WDL)));
                    chart.showLive(liveComparison(fixture)); capture(chart, "live-after-loss-" + scale + ".png");
                }
            } finally { com.formdev.flatlaf.util.UIScale.setZoomFactor(1); }
        });
    }
    private static void layout(Container c) { c.doLayout(); for (var child : c.getComponents()) if (child instanceof Container p) layout(p); }
    private static void capture(JComponent component, String name) {
        try {
            var image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_RGB);
            var g = image.createGraphics(); g.setColor(SeedTheme.BACKGROUND); g.fillRect(0, 0, image.getWidth(), image.getHeight());
            component.paint(g); g.dispose(); var folder = Path.of("build/gui-smoke/dashboard-refinement"); Files.createDirectories(folder);
            ImageIO.write(image, "png", folder.resolve(name).toFile());
        } catch (Exception e) { throw new AssertionError(e); }
    }
}
