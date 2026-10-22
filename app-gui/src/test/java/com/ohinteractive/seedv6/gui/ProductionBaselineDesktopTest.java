package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Real Swing default binding and one bounded move; no model library or training. */
@Timeout(45)
class ProductionBaselineDesktopTest {
    @TempDir Path temporary;

    @Test void freshPlayLoadsCp64AndSearchesWithoutAStore() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Native desktop required");
        var frame = edt(() -> new ChessFrame(TrainingSettings.defaults(temporary, NetworkArchitecture.NNUE),
                new TrainingController.Backend(), ignored -> {}));
        try {
            edt(() -> { frame.setVisible(true); return null; });
            until(() -> edt(() -> named(frame, "blackPlayerNetwork", JTextArea.class).getText().contains("Conventional NNUE")));
            var controllerField = ChessFrame.class.getDeclaredField("controller");
            controllerField.setAccessible(true);
            var controller = (GameController) controllerField.get(frame);
            edt(() -> {
                assertEquals(PlayEvaluator.Mode.CONVENTIONAL_CP64, named(frame, "playEvaluator", JComboBox.class).getSelectedItem());
                assertEquals(PlayEvaluator.Mode.CONVENTIONAL_CP64, named(frame, "whiteEvaluator", JComboBox.class).getSelectedItem());
                assertEquals(PlayEvaluator.Mode.CONVENTIONAL_CP64, named(frame, "blackEvaluator", JComboBox.class).getSelectedItem());
                named(frame, "playThreads", JSpinner.class).setValue(1);
                named(frame, "playDepth", JSpinner.class).setValue(3);
                // Normal GUI enables its book; use a legal non-book position to require evaluation.
                assertTrue(controller.loadFen("4k3/8/8/8/4P3/8/8/4K3 w - - 0 1"));
                named(frame, "humanSide", JComboBox.class).setSelectedItem(GameController.HumanSide.BLACK);
                assertTrue(controller.displayedMoves().isEmpty());
                named(frame, "startGame", JButton.class).doClick();
                return null;
            });
            until(() -> edt(() -> named(frame, "engineDepth", JLabel.class).getText().equals("3")));
            assertFalse(edt(() -> named(frame, "principalVariation", JTextArea.class).getText()).isBlank());
            assertTrue(edt(() -> named(frame, "whitePlayerNetwork", JTextArea.class).getText()).contains("Conventional NNUE"));
            try (var files = Files.walk(temporary)) {
                assertEquals(0, files.filter(Files::isRegularFile).count(), "Play must not create training artifacts");
            }
        } finally {
            edt(() -> { frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING)); return null; });
            until(() -> !edt(frame::isDisplayable));
        }
    }

    @Test void legacyPlayChoiceRequiresExplicitSelectionAndArenaDefaultsToCp64() throws Exception {
        var preferences = java.util.prefs.Preferences.userRoot().node("seedv6-baseline-test-" + java.util.UUID.randomUUID());
        try {
            preferences.put("evaluator", PlayEvaluator.Mode.CONSTRUCTION_V1.name());
            var play = edt(() -> new PlayEnginePanel("white", temporary, preferences, () -> {}));
            var arena = edt(() -> new PlayEnginePanel("arenaFixedA", temporary, null, () -> {}));
            try {
                assertFalse(edt(play::validSelection));
                assertTrue(edt(play::error).contains("CONSTRUCTION_V1"));
                edt(() -> { play.evaluator.setSelectedItem(PlayEvaluator.Mode.BREP2_32); return null; });
                assertTrue(edt(play::validSelection));
                assertEquals(PlayEvaluator.Mode.CONVENTIONAL_CP64, edt(arena::selectedMode));
            } finally { edt(() -> { play.dispose(); arena.dispose(); return null; }); }
        } finally { preferences.removeNode(); }
    }

    private static <T extends Component> T named(Container root, String name, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (name.equals(child.getName()) && type.isInstance(child)) return type.cast(child);
            if (child instanceof Container container) {
                try { return named(container, name, type); } catch (IllegalArgumentException absent) { }
            }
        }
        throw new IllegalArgumentException("Missing control " + name);
    }
    private static void until(Callable<Boolean> check) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (!check.call()) { if (System.nanoTime() > end) fail("Timed out waiting for Play"); Thread.sleep(25); }
    }
    private static <T> T edt(Callable<T> call) throws Exception {
        var result = new AtomicReference<T>(); var failure = new AtomicReference<Throwable>();
        SwingUtilities.invokeAndWait(() -> { try { result.set(call.call()); } catch (Throwable t) { failure.set(t); } });
        if (failure.get() != null) throw new AssertionError(failure.get());
        return result.get();
    }
}
