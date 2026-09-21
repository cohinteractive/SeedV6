package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

/** Native desktop runtime verification via Swing actions, never inferred from source or HTTP. */
@Tag("slow-nnue")
@Timeout(120)
class NnueGuiSmokeTest {
    @TempDir Path temp;
    private ChessFrame frame;

    @Test void nativeValidationShowsCompactLiveStatusPreservesScrollAndShowsFinalDecisionAtExistingPollCadence() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        var reached = new java.util.concurrent.CountDownLatch[3];
        var release = new java.util.concurrent.CountDownLatch[3];
        for (int i = 0; i < 3; i++) {
            reached[i] = new java.util.concurrent.CountDownLatch(1);
            release[i] = new java.util.concurrent.CountDownLatch(1);
        }
        var backend = new TrainingController.Backend() {
            @Override TrainingController.Handle create(TrainingSettings settings, boolean resume,
                    com.ohinteractive.seedv6.training.service.TrainerConfig.DepthChange change) throws java.io.IOException {
                var c = settings.config(change);
                // This fixture specifically verifies INCONCLUSIVE presentation, independent of GUI defaults.
                var v = c.validation();
                var inconclusive = new com.ohinteractive.seedv6.training.service.TrainerConfig.Validation(
                        v.openingPairs(), v.minimumOpeningPlies(), v.maximumOpeningPlies(), v.depth(), v.threads(),
                        v.maximumPlies(), v.scoreMapping(), new com.ohinteractive.seedv6.training.validation.PromotionPolicy(
                                v.openingPairs() + 1, v.policy().alpha(), v.policy().requiredMargin()));
                var config = new com.ohinteractive.seedv6.training.service.TrainerConfig(c.checkpointRoot(), c.masterSeed(),
                        c.selfPlay(), c.training(), inconclusive, 1, change, SHORT_GAME);
                return handle(com.ohinteractive.seedv6.training.service.ValidationTelemetryFixture.fresh(config,
                        new com.ohinteractive.seedv6.training.nnue.NnueTrainer(
                                com.ohinteractive.seedv6.training.nnue.TrainableNnue.initialized(settings.seed())), p -> {
                            int gate = p.gameActive() && p.gameOrdinal() == 1 && p.currentGamePlies() <= 1
                                    ? p.currentGamePlies() : p.gameActive() && p.gameOrdinal() == 2 && p.currentGamePlies() == 0 ? 2 : -1;
                            if (gate < 0) return;
                            reached[gate].countDown();
                            try {
                                if (!release[gate].await(30, java.util.concurrent.TimeUnit.SECONDS)) throw new AssertionError("GUI gate timed out");
                            } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new AssertionError(interrupted); }
                        }));
            }
        };
        frame = edt(() -> new ChessFrame(settings(temp.resolve("validation"), 1, 1), backend, ignored -> {}));
        try {
            edt(() -> { frame.setVisible(true); tabs().setSelectedIndex(1); button("startTraining").doClick(); });
            for (int i = 0; i < 3; i++) {
                assertTrue(reached[i].await(30, java.util.concurrent.TimeUnit.SECONDS));
                int gate = i;
                until(() -> edt(() -> component("validationProgress", JTextArea.class).getText()
                        .contains(gate < 2 ? "Current game: " + gate + " plies" : "Game 2 / 2")));
                edt(() -> {
                    String training = component("trainingProgress", JTextArea.class).getText();
                    String validation = component("validationProgress", JTextArea.class).getText();
                    assertTrue(training.contains("State: VALIDATING"));
                    assertFalse(training.contains("Candidate W/D/L"));
                    assertTrue(validation.contains("Pair 1 / 2"));
                    assertTrue(validation.contains("Overall " + (gate < 2 ? 1 : 2) + " / 4"));
                    assertTrue(validation.contains("White: " + (gate < 2 ? "Candidate | Black: Best" : "Best | Black: Candidate")));
                    assertTrue(validation.contains("Current game:"));
                    assertTrue(validation.contains("Search: depth 1 | threads 1 | no node/time limit"));
                    assertEquals(Font.MONOSPACED, component("trainingProgress", JTextArea.class).getFont().getFamily());
                    assertEquals(Font.MONOSPACED, component("validationProgress", JTextArea.class).getFont().getFamily());
                    assertEquals(JSplitPane.VERTICAL_SPLIT, component("trainingOutputs", JSplitPane.class).getOrientation());
                    if (gate == 1) assertTrue(validation.contains("Last move: depth 1"));
                    if (gate == 2) {
                        assertFalse(validation.contains("50-move"));
                        assertTrue(validation.contains("Candidate: Gen 1    Wins: 0"));
                        assertTrue(validation.contains("Best:      Gen 0    Wins: 0"));
                        assertTrue(validation.contains("Draws: 0"));
                    }
                    assertTrue(button("stopTraining").isEnabled(), "EDT remains responsive during validation");
                    if (gate == 1) {
                        component("trainingViews", JTabbedPane.class).setSelectedIndex(0); frame.validate();
                        capture("nnue-validation-live.png");
                        assertEquals("VALIDATING", component("trainingState", JLabel.class).getText());
                        assertTrue(component("trainingGameSides", JLabel.class).getText().contains("White: Candidate"));
                    }
                });
                if (gate == 1) {
                    for (int anchor = 0; anchor < 3; anchor++) {
                        int mode = anchor;
                        String before = edt(() -> {
                            component("trainingViews", JTabbedPane.class).setSelectedIndex(3); frame.validate();
                            var split = component("trainingOutputs", JSplitPane.class);
                            split.setDividerLocation(split.getHeight() - 130); frame.validate();
                            var bar = ((JScrollPane) split.getBottomComponent()).getVerticalScrollBar();
                            int bottom = bar.getMaximum() - bar.getVisibleAmount();
                            assertTrue(bottom > 30);
                            bar.setValue(mode == 0 ? 0 : mode == 1 ? bottom / 2 : bottom);
                            return component("validationProgress", JTextArea.class).getText();
                        });
                        int position = edt(() -> ((JScrollPane) component("trainingOutputs", JSplitPane.class)
                                .getBottomComponent()).getVerticalScrollBar().getValue());
                        until(() -> edt(() -> !before.equals(component("validationProgress", JTextArea.class).getText())));
                        edt(() -> {}); // Allow the deferred restore to complete after the real 500 ms poll.
                        edt(() -> {
                            var bar = ((JScrollPane) component("trainingOutputs", JSplitPane.class).getBottomComponent()).getVerticalScrollBar();
                            assertEquals(mode == 2 ? bar.getMaximum() - bar.getVisibleAmount() : position, bar.getValue(), 2);
                        });
                    }
                }
                release[i].countDown();
            }
            until(() -> edt(() -> component("validationProgress", JTextArea.class).getText().contains("Validation finished - INCONCLUSIVE")));
            // Durable completion follows the validation publication; the history read is asynchronous.
            until(() -> edt(() -> component("recentTrainingHistory", JTable.class).getRowCount() == 1));
            edt(() -> {
                String text = component("validationProgress", JTextArea.class).getText();
                assertTrue(text.contains("Valid pairs: 2"));
                assertTrue(text.contains("Candidate: Gen 1    Wins: 0"));
                assertTrue(text.contains("Best:      Gen 0    Wins: 0"));
                assertTrue(text.contains("Draws: 4"));
                assertFalse(text.contains("50-move"));
                assertFalse(text.contains("Current game:"));
                assertFalse(text.contains("Last move:"));
                component("trainingViews", JTabbedPane.class).setSelectedIndex(0); frame.validate();
                assertFalse(component("trainingGameSides", JLabel.class).getText().contains("White:"));
                assertEquals(1, component("recentTrainingHistory", JTable.class).getRowCount());
                capture("nnue-validation-final.png");
            });
        } finally { for (var gate : release) gate.countDown(); }
    }

    @AfterEach void close() throws Exception {
        if (frame != null && edt(frame::isDisplayable)) {
            edt(() -> frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING)));
            until(() -> !edt(frame::isDisplayable));
        }
        until(() -> ownedThreads().isEmpty());
    }

    @Test void nativeControlsRunConcurrentPlayTrainingBothEvaluatorsAndCloseWithoutWorkers() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Path root = temp.resolve("gui-store"); var backend = new ShortBackend();
        frame = edt(() -> new ChessFrame(settings(root, 1, 0), backend, ignored -> {}));
        edt(() -> frame.setVisible(true));
        assertFalse(Files.exists(root), "Launching the actual frame must not open/create the store");
        assertEquals(PlayEvaluator.Mode.HANDCRAFTED, edt(() -> combo("playEvaluator").getSelectedItem()));
        edt(() -> {
            tabs().setSelectedIndex(1);
            assertTrue(component("trainingDepth", JSpinner.class).isEnabled());
            button("startTraining").doClick();
            assertFalse(button("startTraining").isEnabled());
            assertFalse(component("trainingDepth", JSpinner.class).isEnabled());
        });
        until(() -> edt(() -> component("trainingProgress", JTextArea.class).getText().contains("generations: 1")));
        edt(() -> {
            assertTrue(combo("humanSide").isEnabled());
            capture("nnue-training.png");
        });
        edt(() -> {
            tabs().setSelectedIndex(0);
            component("playThreads", JSpinner.class).setValue(2);
            assertEquals(1, component("trainingThreads", JSpinner.class).getValue());
            component("playDepth", JSpinner.class).setValue(1);
            combo("playEvaluator").setSelectedItem(PlayEvaluator.Mode.BEST_NNUE);
        });
        until(() -> edt(() -> component("pinnedBest", JLabel.class).getText().startsWith("Pinned best: g")));
        edt(() -> {
            findButton(frame, "New Game").doClick();
        });
        until(() -> edt(() -> combo("humanSide").isEnabled()));
        edt(() -> combo("humanSide").setSelectedItem(GameController.HumanSide.BLACK));
        until(() -> edt(() -> component("moveHistory", JTable.class).getRowCount() > 0));
        assertTrue(edt(() -> component("engineScore", JLabel.class).getToolTipText().contains("NNUE V1 units")));
        edt(() -> {
            capture("nnue-play.png");
            combo("playEvaluator").setSelectedItem(PlayEvaluator.Mode.HANDCRAFTED);
        });
        until(() -> edt(() -> combo("humanSide").isEnabled()));
        edt(() -> findButton(frame, "New Game").doClick());
        until(() -> edt(() -> component("moveHistory", JTable.class).getRowCount() > 0));
        assertTrue(edt(() -> component("engineScore", JLabel.class).getToolTipText().contains("Pawns")));
        assertEquals(1, backend.fresh);
        String progress = edt(() -> component("trainingProgress", JTextArea.class).getText());
        // The timer continues to publish training progress even with Play selected.
        until(() -> edt(() -> !progress.equals(component("trainingProgress", JTextArea.class).getText())));
        edt(() -> {
            assertTrue(button("stopTraining").isEnabled());
            for (int i = 0; i < 4; i++) tabs().setSelectedIndex(i % 2);
            component("playDepth", JSpinner.class).setValue(256);
            findButton(frame, "New Game").doClick();
        });
        until(() -> edt(() -> component("engineState", JLabel.class).getText().contains("Thinking")));
        edt(() -> findButton(frame, "Stop Search").doClick());
        until(() -> edt(() -> component("searchTermination", JLabel.class).getText().contains("STOPPED")));
        assertTrue(edt(() -> button("stopTraining").isEnabled()));
        edt(() -> findButton(frame, "New Game").doClick());
        until(() -> edt(() -> component("engineState", JLabel.class).getText().contains("Thinking")));
        edt(() -> button("stopTraining").doClick());
        until(() -> edt(() -> component("trainingProgress", JTextArea.class).getText().contains("State: STOPPED")));
        assertTrue(edt(() -> component("engineState", JLabel.class).getText().contains("Thinking")));
        edt(() -> {
            component("trainingThreads", JSpinner.class).setValue(3);
            assertEquals(2, component("playThreads", JSpinner.class).getValue());
        });
        // Start Training during a real Play search, then close both active domains through the window.
        edt(() -> { tabs().setSelectedIndex(1); button("startTraining").doClick(); });
        until(() -> edt(() -> component("trainingProgress", JTextArea.class).getText().contains("Best: g")));
        assertTrue(edt(() -> component("engineState", JLabel.class).getText().contains("Thinking")));
        edt(() -> frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING)));
        until(() -> !edt(frame::isDisplayable));
        until(() -> ownedThreads().isEmpty());
        try (var store = new com.ohinteractive.seedv6.training.checkpoint.CheckpointStore(root)) {
            assertTrue(store.recover().best().isPresent());
        }
    }

    @Test void windowCloseDuringRealSelfPlaySafelyReleasesTheStore() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Path root = temp.resolve("closing");
        frame = edt(() -> new ChessFrame(settings(root, 1, 0), new ShortBackend(), ignored -> {}));
        edt(() -> { frame.setVisible(true); tabs().setSelectedIndex(1); button("startTraining").doClick(); });
        until(() -> edt(() -> component("trainingProgress", JTextArea.class).getText().contains("Best: g")));
        edt(() -> frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING)));
        until(() -> !edt(frame::isDisplayable));
        until(() -> ownedThreads().isEmpty());
        try (var store = new com.ohinteractive.seedv6.training.checkpoint.CheckpointStore(root)) {
            assertTrue(store.recover().best().isPresent());
        }
    }

    private void capture(String filename) {
        try {
            Path directory = Path.of("build", "gui-smoke"); Files.createDirectories(directory);
            BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics(); frame.paintAll(graphics); graphics.dispose();
            ImageIO.write(image, "png", directory.resolve(filename).toFile());
        } catch (Exception failure) { throw new AssertionError(failure); }
    }
    private JButton button(String name) { return component(name, JButton.class); }
    private JComboBox<?> combo(String name) { return component(name, JComboBox.class); }
    private JTabbedPane tabs() { return descendants(frame).stream().filter(JTabbedPane.class::isInstance).map(JTabbedPane.class::cast).findFirst().orElseThrow(); }
    private <T> T component(String name, Class<T> type) {
        return descendants(frame).stream().filter(c -> name.equals(c.getName())).map(type::cast).findFirst().orElseThrow();
    }
    private static JButton findButton(Container parent, String text) {
        return descendants(parent).stream().filter(JButton.class::isInstance).map(JButton.class::cast)
                .filter(button -> text.equals(button.getText())).findFirst().orElseThrow();
    }
    private static List<Component> descendants(Container parent) {
        java.util.ArrayList<Component> found = new java.util.ArrayList<>();
        for (Component child : parent.getComponents()) {
            found.add(child); if (child instanceof Container container) found.addAll(descendants(container));
        }
        return found;
    }
    private static List<String> ownedThreads() {
        return Thread.getAllStackTraces().keySet().stream().filter(Thread::isAlive).map(Thread::getName)
                .filter(n -> n.startsWith("seedv6-trainer-") || n.startsWith("seedv6-root-worker-")
                        || n.equals("seedv6-search-worker") || n.startsWith("seedv6-ui-")).toList();
    }
}
