package com.ohinteractive.seedv6.gui;

import java.awt.Container;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.training.service.TrainerSnapshot;
import com.ohinteractive.seedv6.training.validation.*;
import static com.ohinteractive.seedv6.training.selfplay.GameTermination.*;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class ValidationPresentationTest {
    private static final String CANDIDATE = "g000025-s000000100-" + "a".repeat(64);
    private static final String BEST = "g000019-s000000090-" + "b".repeat(64);
    private static final ValidationConfig CONFIG = new ValidationConfig(64, 1, 0, 8, 6, 12,
            com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping.V1, 1024);
    private static final TrainerSnapshot.ValidationDetails DETAILS = new TrainerSnapshot.ValidationDetails(
            CANDIDATE, BEST, CONFIG, PromotionPolicy.DEFAULT, OptionalLong.of(25), OptionalLong.of(19));

    @Test void runningPresentationIsCompactAndUsesCandidatePerspective() {
        var p = progress(false, true, Map.of(WHITE_CHECKMATES_BLACK, 20, STALEMATE, 10, BLACK_CHECKMATES_WHITE, 2));
        var view = view(p, null, null, DETAILS, TrainerSnapshot.State.VALIDATING);
        String text = TrainingProgress.validation(view, 232_000_000_000L);
        assertTrue(text.startsWith("Validation in progress | Candidate score 78.1% | 16 valid pairs\n"), text);
        for (String expected : List.of("Candidate: Gen 25    Wins: 20", "Best:      Gen 19    Wins: 2", "Draws: 10",
                "Pair 17 / 64 | Game 1 / 2 | Overall 33 / 128", "Valid pairs: 16 | Incomplete: 0 | Capped: 0",
                "White: Candidate | Black: Best", "Current game: 83 plies | 03:42", "Validation elapsed: 03:52",
                "Last move: depth 6 | 3,518,672 nodes | 839 ms | 4.19M NPS", "Search: depth 6 | threads 12 | no node/time limit")) {
            assertTrue(text.contains(expected), expected + " in " + text);
        }
        assertCompact(text);
        assertFalse(text.contains("Cancelled")); assertFalse(text.contains("Failed"));
        assertFalse(text.contains("KEEP BEST")); assertFalse(text.contains("PROMOTE"));
        assertFalse(text.contains(CANDIDATE)); assertFalse(text.contains(BEST));
        assertTrue(TrainingProgress.validation(view, 242_000_000_000L).contains("83 plies | 03:52"));
        assertEquals(83, p.currentGamePlies());
        assertTrue(TrainingProgress.format(view).contains("Self-play / training"));
    }

    @Test void nonzeroHealthCountsAreConditionalAndDoNotInventAnEarlyDecision() {
        String text = TrainingProgress.validation(view(progress(false, true,
                Map.of(PLY_CAP, 4, CANCELLED, 2, SEARCH_FAILURE, 1, INFRASTRUCTURE_FAILURE, 1)),
                null, null, DETAILS, TrainerSnapshot.State.VALIDATING), 0);
        assertTrue(text.startsWith("Validation in progress"));
        assertTrue(text.contains("Capped: 4"));
        assertTrue(text.contains("Warning: Cancelled slots: 2"));
        assertTrue(text.contains("Warning: Failed slots: 2 | Promotion blocked"));
    }

    @Test void keepPromoteAndEqualityHeadlinesUseTheActualAssessment() {
        for (int wins : new int[] {41, 50}) {
            var stats = statistics(wins, 64 - wins, 0);
            var assessment = PromotionPolicy.DEFAULT.assess(64, stats.pairScoreSum());
            String text = TrainingProgress.validation(view(progress(true, false, stats.terminations()), stats, assessment,
                    DETAILS, TrainerSnapshot.State.STOPPED), 1_000_000_000_000L);
            String decision = wins == 41 ? "KEEP BEST" : "PROMOTE CANDIDATE";
            String relation = wins == 41 ? " < " : " > ";
            String headline = String.format(Locale.ROOT, "Validation finished - %s | Score %.1f%% | Lower %.1f%%%s50.0%%",
                    decision, assessment.mean() * 100, assessment.lowerBound() * 100, relation);
            assertEquals(headline, text.lines().findFirst().orElseThrow());
            assertTrue(text.contains("Games: 128 / 128 | Valid pairs: 64 | Incomplete: 0 | Capped: 0"));
            assertTrue(text.contains("Validation elapsed: 03:34"));
            assertNoActiveGame(text); assertCompact(text);
        }
        var equality = new PromotionPolicy.Assessment(64, 0.75, 0.25, 0.5, 0.5, PromotionPolicy.Decision.RETAIN_INCUMBENT);
        assertTrue(TrainingProgress.validation(view(null, statistics(48, 16, 0), equality, DETAILS,
                TrainerSnapshot.State.STOPPED), 0).startsWith("Validation finished - KEEP BEST | Score 75.0% | Lower 50.0% = 50.0%"));
    }

    @Test void inconclusiveAndFailureOverrideMisleadingPromotion() {
        var empty = new ValidationResult.Statistics(0, 64, new ValidationResult.ColourRecord(0, 0, 0),
                new ValidationResult.ColourRecord(0, 0, 0), 0, Map.of(CANCELLED, 128));
        String text = TrainingProgress.validation(view(null, empty, PromotionPolicy.DEFAULT.assess(0, 0), DETAILS,
                TrainerSnapshot.State.STOPPED), 0);
        assertTrue(text.startsWith("Validation finished - INCONCLUSIVE - KEEP BEST | Valid pairs: 0 < 64 required | Score pending"));
        assertTrue(text.contains("Cancelled slots: 128")); assertFalse(text.contains("NaN"));
        var passing = PromotionPolicy.DEFAULT.assess(64, 64);
        for (var reason : List.of(SEARCH_FAILURE, INFRASTRUCTURE_FAILURE)) {
            text = TrainingProgress.validation(view(progress(true, false, Map.of(reason, 1)), null, passing, DETAILS,
                    TrainerSnapshot.State.FAILED), 0);
            assertTrue(text.startsWith("Validation failed - PROMOTION BLOCKED | Search/infrastructure failure"));
            assertFalse(text.contains("PROMOTE CANDIDATE")); assertNoActiveGame(text);
        }
        text = TrainingProgress.validation(view(progress(false, true, Map.of()), null, null, DETAILS,
                TrainerSnapshot.State.FAILED), 0);
        assertTrue(text.startsWith("Validation failed - PROMOTION BLOCKED")); assertNoActiveGame(text);
        assertTrue(text.contains("Validation elapsed: 03:34 (last progress)"));
        assertEquals(text, TrainingProgress.validation(view(progress(false, true, Map.of()), null, null, DETAILS,
                TrainerSnapshot.State.FAILED), 999_000_000_000L));
        text = TrainingProgress.validation(view(progress(true, false, Map.of()), null, null, DETAILS,
                TrainerSnapshot.State.FAILED), 0);
        assertTrue(text.startsWith("Validation failed - PROMOTION BLOCKED"), "Failure before assessment must not stay pending");
        // Failure in the next training generation must not rewrite the previous validation decision.
        text = TrainingProgress.validation(view(progress(true, false, Map.of()), null, passing, DETAILS,
                TrainerSnapshot.State.FAILED), 0);
        assertTrue(text.startsWith("Validation finished - PROMOTE CANDIDATE"));
        // A failure recording the current candidate's decision blocks promotion too.
        text = TrainingProgress.validation(view(progress(true, false, Map.of()), null, passing,
                new TrainerSnapshot.ValidationDetails("candidate", BEST, CONFIG, PromotionPolicy.DEFAULT),
                TrainerSnapshot.State.FAILED), 0);
        assertTrue(text.startsWith("Validation failed - PROMOTION BLOCKED"));
    }

    @Test void pendingAndBetweenGamesDoNotInventScoresOrRetainMoveTelemetry() {
        String text = TrainingProgress.validation(view(ValidationProgress.initial(64, 0), null, null, DETAILS,
                TrainerSnapshot.State.VALIDATING), 0);
        assertTrue(text.startsWith("Validation in progress | Candidate score pending | 0 valid pairs"));
        assertFalse(text.contains("NaN")); assertFalse(text.contains("0.0%"));
        text = TrainingProgress.validation(view(progress(false, false, Map.of()), null, null, DETAILS,
                TrainerSnapshot.State.VALIDATING), 0);
        assertTrue(text.contains("Pair 17 / 64 | Game 1 / 2 | Overall 33 / 128")); assertNoActiveGame(text);
        text = TrainingProgress.validation(view(progress(true, true, Map.of()), null, null, DETAILS,
                TrainerSnapshot.State.VALIDATING), 0);
        assertTrue(text.startsWith("Validation finished - assessment pending")); assertNoActiveGame(text);
    }

    @Test void recoveredEvidenceUsesItsOwnGenerationsSettingsAndPolicy() {
        var stats = new ValidationResult.Statistics(1, 1, new ValidationResult.ColourRecord(1, 0, 0),
                new ValidationResult.ColourRecord(0, 1, 0), 12, Map.of(WHITE_CHECKMATES_BLACK, 1, STALEMATE, 1, PLY_CAP, 2));
        var rule = new PromotionPolicy(1, 0.1, 0.03);
        var config = new ValidationConfig(2, 1, 0, 0, 3, 2, CONFIG.scoreMapping(), 32);
        String text = TrainingProgress.validation(view(null, stats, rule.assess(1, stats.pairScoreSum()),
                new TrainerSnapshot.ValidationDetails(CANDIDATE, BEST, config, rule), TrainerSnapshot.State.STOPPED), 0);
        for (String expected : List.of("Candidate: Gen 25    Wins: 1", "Best:      Gen 19    Wins: 0", "Draws: 1",
                "Games: 4 / 4", "Search: depth 3 | threads 2", "< 53.0%", "Score 75.0%")) assertTrue(text.contains(expected), text);
        assertFalse(text.contains("Validation elapsed"), "Recovered records have no runtime elapsed telemetry");
        assertFalse(text.contains(CANDIDATE)); assertFalse(text.contains(BEST)); assertNoActiveGame(text);
    }

    @Test void generationMetadataWinsAndInvalidLegacyIdsAreNotGuessed() {
        for (String id : List.of("unexpected", "g25", "g9999999999999999999-s000000001-" + "a".repeat(64))) {
            var details = new TrainerSnapshot.ValidationDetails(id, id, CONFIG, PromotionPolicy.DEFAULT);
            String text = TrainingProgress.validation(view(ValidationProgress.initial(64, 0), null, null, details,
                    TrainerSnapshot.State.VALIDATING), 0);
            assertTrue(text.contains("Candidate: Gen unknown")); assertFalse(text.contains(id));
        }
        var details = new TrainerSnapshot.ValidationDetails(CANDIDATE, BEST, CONFIG, PromotionPolicy.DEFAULT,
                OptionalLong.of(101), OptionalLong.of(99));
        String text = TrainingProgress.validation(view(ValidationProgress.initial(64, 0), null, null, details,
                TrainerSnapshot.State.VALIDATING), 0);
        assertTrue(text.contains("Candidate: Gen 101")); assertTrue(text.contains("Best:      Gen 99"));
    }

    @Test void advancedContainsReadOnlyValidationExplanationAndExistingPanelConventionsSurvive() throws Exception {
        var view = view(progress(false, true, Map.of()), null, null, DETAILS, TrainerSnapshot.State.VALIDATING);
        TrainingPanel panel = edt(() -> new TrainingPanel(view.settings()));
        edt(() -> {
            panel.showState(view);
            var training = find(panel, "trainingProgress", JTextArea.class);
            var validation = find(panel, "validationProgress", JTextArea.class);
            assertEquals(java.awt.Font.MONOSPACED, training.getFont().getFamily());
            assertEquals(java.awt.Font.MONOSPACED, validation.getFont().getFamily());
            var split = find(panel, "trainingOutputs", JSplitPane.class);
            assertEquals(JSplitPane.VERTICAL_SPLIT, split.getOrientation());
            assertTrue(split.isContinuousLayout()); assertTrue(split.getDividerSize() > 0);
            assertTrue(split.getTopComponent().getMinimumSize().height >= 80);
            assertTrue(split.getBottomComponent().getMinimumSize().height >= 80);
            var info = find(TrainingPanel.validationInformation(), "validationInformation", JTextArea.class);
            assertFalse(info.isEditable());
            var searchInfo = find(panel, "nnueSearchInformation", JTextArea.class);
            assertFalse(searchInfo.isEditable());
            for (String concept : List.of("iterative deepening", "alpha-beta/PVS", "quiescence", "root parallelism",
                    "Incremental NNUE", "scale", "Full-window", "no aspiration", "mate-distance", "soft limit",
                    "absolute search ply limit", "private TT")) {
                assertTrue(searchInfo.getText().contains(concept), concept);
                assertFalse(info.getText().contains(concept), concept);
                assertFalse(validation.getText().contains(concept), concept);
            }
            for (String concept : List.of("reversed colours", "randomized opening", "game cap",
                    "Hoeffding", "sqrt", "minimum valid pairs", "all configured game slots", "no early threshold",
                    "inconclusive", "failure blocks promotion")) {
                assertTrue(info.getText().contains(concept), concept);
                assertFalse(validation.getText().contains(concept), concept);
            }
            panel.showState(view(progress(false, true, Map.of()), null, null, DETAILS, TrainerSnapshot.State.STOPPING));
            assertFalse(find(panel, "stopTraining", JButton.class).isEnabled());
            assertTrue(training.getText().contains("STOPPING"));
            panel.showState(view(progress(true, false, Map.of()), null, null, DETAILS, TrainerSnapshot.State.STOPPED));
            assertTrue(training.getText().contains("STOPPED"));
            assertFalse(training.getText().contains("State: VALIDATING"));
            assertFalse(training.getText().contains("State: STOPPING"));
            assertTrue(find(panel, "startTraining", JButton.class).isEnabled());
        });
        assertThrows(IllegalStateException.class, () -> panel.showState(view));
    }

    @Test void expandedValidationViewportRemovesScrollbarAndPollsPreserveDivider() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless());
        var view = view(progress(false, true, Map.of()), null, null, DETAILS, TrainerSnapshot.State.VALIDATING);
        JFrame frame = edt(() -> new JFrame("SeedV6 layout test"));
        try {
            edt(() -> {
                TrainingPanel panel = new TrainingPanel(view.settings());
                frame.setContentPane(panel); frame.setSize(1100, 1150); frame.setVisible(true);
                panel.showState(view); panel.showDiagnostics(); frame.validate();
                var split = find(panel, "trainingOutputs", JSplitPane.class);
                var lower = (JScrollPane) split.getBottomComponent();
                split.setDividerLocation(split.getHeight() - 130); frame.validate();
                assertTrue(lower.getVerticalScrollBar().isVisible());
                int small = lower.getHeight();
                split.setDividerLocation(90); frame.validate();
                assertTrue(lower.getHeight() > small + 300);
                assertFalse(lower.getVerticalScrollBar().isVisible());
                panel.showState(view); frame.validate();
                assertEquals(90, split.getDividerLocation());
            });
        } finally { edt(frame::dispose); }
    }

    @Test void configurationExposesTheReadOnlyValidationSectionWithoutAModalDialog() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless());
        JFrame frame = edt(() -> new JFrame("SeedV6 Configuration test"));
        try {
            edt(() -> {
                TrainingPanel panel = new TrainingPanel(settings(Path.of("build/test-store-unused"), 1, 1));
                frame.setContentPane(panel); frame.setSize(1000, 1100); frame.setVisible(true);
                find(panel, "trainingViews", JTabbedPane.class).setSelectedIndex(2); frame.validate();
                var info = find(panel, "validationInformation", JTextArea.class);
                assertNotNull(info); assertTrue(info.isShowing()); assertFalse(info.isEditable());
                assertTrue(info.getText().contains("Hoeffding"));
                assertTrue(find(panel, "trainingDepth", JSpinner.class).isShowing());
                assertTrue(find(panel, "applyTrainingSettings", JButton.class).isShowing());
                assertTrue(Arrays.stream(java.awt.Window.getWindows()).noneMatch(w -> w instanceof JDialog && w.isShowing()));
            });
        } finally { edt(frame::dispose); }
    }

    private static void assertCompact(String text) {
        for (String removed : List.of("Completed games", "Finished/capped", "Total plies", "Average", "Longest",
                "Checkmates", "50-move", "Repetition", "Stalemate", "Material", "Hoeffding", "sqrt", "Last progress"))
            assertFalse(text.contains(removed), removed + " in " + text);
    }
    private static void assertNoActiveGame(String text) {
        for (String removed : List.of("Current game:", "White:", "Black:", "Last move:", "Game elapsed", "No active game"))
            assertFalse(text.contains(removed), text);
    }
    private static ValidationProgress progress(boolean complete, boolean active, Map<com.ohinteractive.seedv6.training.selfplay.GameTermination, Integer> reasons) {
        return new ValidationProgress(64, 17, 1, 33, 83, ValidationProgress.CandidateColour.WHITE,
                active, complete, 16, 0, new ValidationResult.ColourRecord(10, 5, 1), new ValidationResult.ColourRecord(10, 5, 1),
                32, 2688, 312, reasons, 0, 10_000_000_000L, 214_000_000_000L, 214_000_000_000L,
                Optional.of(new ValidationProgress.MoveSearch(6, 3518672, 839, 4190000)));
    }
    private static ValidationResult.Statistics statistics(int wins, int losses, int draws) {
        var colour = new ValidationResult.ColourRecord(wins, draws, losses);
        return new ValidationResult.Statistics(64, 0, colour, colour, 100, Map.of(WHITE_CHECKMATES_BLACK, 128));
    }
    private static TrainingController.ViewState view(ValidationProgress progress, ValidationResult.Statistics stats,
            PromotionPolicy.Assessment assessment, TrainerSnapshot.ValidationDetails details, TrainerSnapshot.State state) {
        var base = new TrainingControllerTest.FakeHandle().snapshot();
        // Current generation and best deliberately differ from the retained match identities.
        var snapshot = new TrainerSnapshot(state, "failure", Duration.ZERO, 999, "current-best", "latest", "candidate", 1, 1,
                base.selfPlay(), Optional.empty(), 0, 0, 0, Optional.ofNullable(stats), Optional.ofNullable(assessment), base.totals(),
                Optional.ofNullable(progress), Optional.ofNullable(details));
        boolean active = state != TrainerSnapshot.State.STOPPED && state != TrainerSnapshot.State.FAILED;
        return new TrainingController.ViewState(settings(Path.of("build/test-store-unused"), 1, 1),
                state == TrainerSnapshot.State.STOPPING ? TrainingController.Phase.STOPPING
                        : active ? TrainingController.Phase.RUNNING : TrainingController.Phase.STOPPED,
                snapshot, "", active, !active, false, 1, "best");
    }
    private static <T> T find(Container parent, String name, Class<T> type) {
        for (var child : parent.getComponents()) {
            if (name.equals(child.getName())) return type.cast(child);
            if (child instanceof Container container) {
                T found = find(container, name, type); if (found != null) return found;
            }
        }
        return null;
    }
}
