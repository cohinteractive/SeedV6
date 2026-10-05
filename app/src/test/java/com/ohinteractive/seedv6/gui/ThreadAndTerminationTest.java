package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.search.exact.*;
import com.ohinteractive.seedv6.training.service.*;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.UUID;
import java.util.prefs.Preferences;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;

class ThreadAndTerminationTest {
    @TempDir Path root;
    @Test void maxRemainsSemanticThroughLineageAndPreferenceRoundTrips() throws Exception {
        var max = new SearchThreads(0);
        assertEquals(2, max.resolve(2)); assertEquals(12, max.resolve(12));
        assertEquals(ParallelSearch.MAX_WORKERS, max.resolve(100)); assertEquals(1, max.resolve(0));
        assertEquals(2, new SearchThreads(8).resolve(2));
        assertThrows(IllegalArgumentException.class, () -> new SearchThreads(100));
        var settings = new TrainingSettings(root, 1, 0, 2, 0, 0, 1, 1, 1, 2, 1, 8, 4).withTimeLimit(9);
        var restored = LineageConfiguration.decode(LineageConfiguration.encode(settings), root, settings.architecture());
        assertEquals(settings, restored); assertEquals(0, restored.threads());
        assertEquals(SearchThreads.available(), restored.config(TrainerConfig.DepthChange.REQUIRE_SAME).selfPlay().threads());
        assertEquals(RunTermination.Kind.COMBINED, restored.termination().kind());
        var prefs = Preferences.userRoot().node("seedv6-tests/threads-" + UUID.randomUUID());
        try {
            settings.save(prefs); var fromPrefs = TrainingSettings.load(prefs);
            assertEquals(0, fromPrefs.threads()); assertEquals(settings.termination(), fromPrefs.termination());
        } finally { prefs.removeNode(); }
    }
    @Test void editorBoundsAndPortableMaxSurviveEditing() throws Exception {
        NnueGuiFixtures.edt(() -> {
            var spinner = ThreadSelection.spinner(0);
            var field = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
            assertTrue(field.getText().startsWith("Max"));
            assertEquals(SearchThreads.available(), ThreadSelection.resolved(spinner));
            field.setText("Auto"); assertDoesNotThrow(spinner::commitEdit); assertEquals(0, spinner.getValue());
            assertEquals(SearchThreads.available(), ((SpinnerNumberModel) spinner.getModel()).getMaximum());
            assertThrows(ParseException.class, () -> field.getFormatter().stringToValue("100"));
            field.setText("1"); assertDoesNotThrow(spinner::commitEdit); assertEquals(1, spinner.getValue());
        });
    }
    @Test void terminationSelectionRemovesInactiveBoundsButRetainsLegacyCombined() {
        assertEquals(RunTermination.Kind.UNLIMITED, new RunTermination(0, 0).kind());
        assertEquals(new RunTermination(0, 9000), RunTermination.selected(RunTermination.Kind.TIME_BUDGET, 3, 9000));
        assertEquals(new RunTermination(3, 0), RunTermination.selected(RunTermination.Kind.GENERATIONS, 3, 9000));
        assertEquals(new RunTermination(3, 9000), RunTermination.selected(RunTermination.Kind.COMBINED, 3, 9000));
        assertThrows(IllegalArgumentException.class, () -> RunTermination.selected(RunTermination.Kind.TIME_BUDGET, 1, 0));
        var protocol = new LearningArenaConfig.Arena(2, LearningArenaConfig.Limit.DEPTH, 1, 1000, 0, 0, 0, 8, TrainerConfig.STANDARD_START);
        assertEquals(0, protocol.threads()); assertEquals(SearchThreads.available(), protocol.matches(1).threads());
    }
    @Test @org.junit.jupiter.api.Timeout(60)
    void nativeRunPolicyShowsOnlyItsActiveBoundAndRendersMax() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless());
        var frame = edt(() -> new ChessFrame(TrainingSettings.defaults(root, NetworkArchitecture.NNUE),
                new TrainingController.Backend(), ignored -> {}));
        try {
            edt(() -> {
                frame.setSize(1440, 950);
                named(frame, "workspaces", JTabbedPane.class).setSelectedIndex(1);
                named(frame, "trainingViews", JTabbedPane.class).setSelectedIndex(2);
                var choice = named(frame, "trainingTermination", JComboBox.class);
                var generations = named(frame, "trainingGenerations", JSpinner.class);
                var minutes = named(frame, "trainingRunMinutes", JSpinner.class);
                assertFalse(generations.isVisible()); assertFalse(minutes.isVisible());
                choice.setSelectedItem(RunTermination.Kind.GENERATIONS);
                assertTrue(generations.isVisible()); assertFalse(minutes.isVisible());
                choice.setSelectedItem(RunTermination.Kind.TIME_BUDGET);
                assertFalse(generations.isVisible()); assertTrue(minutes.isVisible());
                assertEquals(60L, minutes.getValue());
                var threads = named(frame, "trainingThreads", JSpinner.class); threads.setValue(0);
                assertTrue(((JSpinner.DefaultEditor) threads.getEditor()).getTextField().getText().startsWith("Max"));
                frame.validate(); minutes.scrollRectToVisible(new java.awt.Rectangle(0, 0, minutes.getWidth(), minutes.getHeight()));
                assertTrue(minutes.getVisibleRect().contains(new java.awt.Rectangle(0, 0, minutes.getWidth(), minutes.getHeight())));
                WorkflowRefinementGuiTest.capture(frame.getRootPane(), "run-termination-max.png");
            });
        } finally {
            edt(() -> frame.dispatchEvent(new java.awt.event.WindowEvent(frame, java.awt.event.WindowEvent.WINDOW_CLOSING)));
            until(() -> !edt(frame::isDisplayable));
        }
    }
}
