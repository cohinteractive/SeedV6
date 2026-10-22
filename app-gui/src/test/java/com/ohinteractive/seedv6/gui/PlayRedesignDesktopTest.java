package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.nio.file.*;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.model.*;
import static org.junit.jupiter.api.Assertions.*;

/** Native Swing layout, persistence and bounded production-engine checks; no training. */
@Timeout(90)
class PlayRedesignDesktopTest {
    @TempDir Path temporary;

    @Test void settingsRoundTripViewsResizeAndActualEngineMove() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Native Swing required");
        Preferences prefs = Preferences.userRoot().node("seedv6-play-redesign-test-" + UUID.randomUUID());
        ChessFrame frame = null;
        try {
            frame = create(prefs); ChessFrame first = frame;
            edt(() -> {
                assertTrue(named(first, "playLiveView", JPanel.class).isShowing());
                assertFalse(named(first, "gameMode", JComboBox.class).isShowing());
                named(first, "gameSettings", JButton.class).doClick();
                assertTrue(named(first, "gameMode", JComboBox.class).isShowing());
                named(first, "gameMode", JComboBox.class).setSelectedItem(GameController.GameMode.ENGINE_VS_ENGINE);
                named(first, "whiteEvaluator", JComboBox.class).setSelectedItem(PlayEvaluator.Mode.CONSTRUCTION_V1);
                named(first, "blackEvaluator", JComboBox.class).setSelectedItem(PlayEvaluator.Mode.HANDCRAFTED);
                named(first, "gameMode", JComboBox.class).setSelectedItem(GameController.GameMode.HUMAN_VS_ENGINE);
                named(first, "humanSide", JComboBox.class).setSelectedItem(GameController.HumanSide.BLACK);
                named(first, "playEvaluator", JComboBox.class).setSelectedItem(PlayEvaluator.Mode.CONSTRUCTION_V1);
                named(first, "searchLimit", JComboBox.class).setSelectedItem(GameController.LimitKind.MOVETIME);
                named(first, "playMovetime", JSpinner.class).setValue(150L);
                named(first, "playDepth", JSpinner.class).setValue(2);
                named(first, "playThreads", JSpinner.class).setValue(1);
                assertEquals(JTextField.LEFT, ((JSpinner.DefaultEditor) named(first, "playDepth", JSpinner.class).getEditor()).getTextField().getHorizontalAlignment());
                capture(first, "settings");
                assertTrue(named(first, "closeGameSettings", JButton.class).getVisibleRect().width > 0);
                assertEquals(named(first, "playMovetime", JSpinner.class).getWidth(), named(first, "playMovetime", JSpinner.class).getVisibleRect().width);
                named(first, "gameMode", JComboBox.class).setSelectedItem(GameController.GameMode.ENGINE_VS_ENGINE);
                named(first, "closeGameSettings", JButton.class).doClick();
                assertTrue(named(first, "playLiveView", JPanel.class).isShowing());
                assertFalse(controller(first).gameStarted());
                assertTrue(controller(first).displayedMoves().isEmpty());
                return null;
            });
            close(frame); frame = create(prefs); ChessFrame restored = frame;
            edt(() -> {
                assertEquals(GameController.GameMode.ENGINE_VS_ENGINE, named(restored, "gameMode", JComboBox.class).getSelectedItem());
                assertEquals(GameController.HumanSide.BLACK, named(restored, "humanSide", JComboBox.class).getSelectedItem());
                assertEquals(PlayEvaluator.Mode.CONSTRUCTION_V1, named(restored, "playEvaluator", JComboBox.class).getSelectedItem());
                assertEquals(PlayEvaluator.Mode.CONSTRUCTION_V1, named(restored, "whiteEvaluator", JComboBox.class).getSelectedItem());
                assertEquals(PlayEvaluator.Mode.HANDCRAFTED, named(restored, "blackEvaluator", JComboBox.class).getSelectedItem());
                assertEquals(GameController.LimitKind.MOVETIME, named(restored, "searchLimit", JComboBox.class).getSelectedItem());
                assertEquals(150L, named(restored, "playMovetime", JSpinner.class).getValue());
                assertEquals(2, named(restored, "playDepth", JSpinner.class).getValue());
                assertEquals(1, named(restored, "playThreads", JSpinner.class).getValue());
                assertFalse(controller(restored).gameStarted());
                named(restored, "gameMode", JComboBox.class).setSelectedItem(GameController.GameMode.HUMAN_VS_ENGINE);
                assertTrue(controller(restored).loadFen("4k3/8/8/8/4P3/8/8/4K3 w - - 0 1"));
                named(restored, "startGame", JButton.class).doClick(); return null;
            });
            until(() -> edt(() -> controller(restored).displayedMoves().size() == 1));
            edt(() -> {
                assertEquals("2", named(restored, "playDepth", JSpinner.class).getValue().toString());
                assertFalse(named(restored, "startGame", JButton.class).isEnabled());
                assertFalse(named(restored, "gameMode", JComboBox.class).isEnabled());
                assertTrue(named(restored, "configuredSearchLimits", JLabel.class).getText().contains("Time 150 ms"));
                assertTrue(named(restored, "whitePlayerNetwork", JTextArea.class).getText().contains("construction-v1"));
                JTable history = named(restored, "moveHistory", JTable.class);
                assertFalse(history.getValueAt(0, 2).toString().isBlank(), "Actual completed engine search must have a score");
                assertNull(history.getTableHeader());
                capture(restored, "live");
                restored.setSize(1100, 760); restored.validate(); capture(restored, "compact");
                var board = named(restored, "chessBoard", BoardPanel.class);
                var white = named(restored, "whitePlayerNetwork", JTextArea.class);
                assertEquals(board.boardBounds().x, white.getX());
                assertTrue(white.modelToView2D(white.getDocument().getLength() - 1).getMaxY() <= white.getHeight());
                assertTrue(history.getVisibleRect().height > 150);
                named(restored, "stopGame", JButton.class).doClick();
                named(restored, "newGame", JButton.class).doClick();
                assertFalse(controller(restored).gameStarted()); assertTrue(controller(restored).displayedMoves().isEmpty());
                return null;
            });
        } finally { if (frame != null) close(frame); prefs.removeNode(); }
    }

    @Test void capturedStacksGraphicalSanAndIndependentHistoryScrollRender() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Native Swing required");
        Preferences prefs = Preferences.userRoot().node("seedv6-play-layout-test-" + UUID.randomUUID());
        ChessFrame frame = create(prefs);
        try {
            edt(() -> {
                named(frame, "gameMode", JComboBox.class).setSelectedItem(GameController.GameMode.HUMAN_VS_HUMAN);
                named(frame, "startGame", JButton.class).doClick();
                GameController game = controller(frame);
                String[] moves = {"e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "f8c5", "b2b4", "c5b4",
                        "c2c3", "b4a5", "d2d4", "e5d4", "e1g1", "d7d6", "c3d4", "a5b6", "d4d5",
                        "c6a5", "c4b5", "c7c6", "d5c6", "b7c6", "b5c6", "a5c6"};
                for (String move : moves) {
                    game.squarePressed(PlayRedesignTest.square(move.substring(0, 2)));
                    game.squareReleased(PlayRedesignTest.square(move.substring(2, 4)));
                }
                assertEquals(moves.length, game.displayedMoves().size());
                var board = named(frame, "chessBoard", BoardPanel.class);
                assertEquals(3, board.displayedCaptures().count(com.ohinteractive.seedv6.core.util.Piece.PAWN | 8));
                assertEquals(3, board.displayedCaptures().count(com.ohinteractive.seedv6.core.util.Piece.PAWN));
                assertEquals(-3, BoardMaterial.balance(game.boardSnapshot(), 0));
                capture(frame, "captures-san");
                frame.setSize(1100, 760); frame.validate();
                JTable table = named(frame, "moveHistory", JTable.class);
                JScrollPane scroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, table);
                assertTrue(scroll.getVerticalScrollBar().getMaximum() > scroll.getVerticalScrollBar().getVisibleAmount());
                scroll.getVerticalScrollBar().setValue(0);
                game.squarePressed(PlayRedesignTest.square("a1"));
                assertEquals(0, scroll.getVerticalScrollBar().getValue());
                capture(frame, "captures-compact");
                // Long network descriptions wrap without changing either identity's board alignment.
                board.showPlayers("SeedV6 (Engine) - construction-v1 / a deliberately long lineage name / Pair-2 g32 (experimental)",
                        "SeedV6 (Engine) - an independently selected evaluator and a long checkpoint description", "white", "black");
                frame.validate();
                for (String name : new String[]{"whitePlayerNetwork", "blackPlayerNetwork"}) {
                    JTextArea label = named(frame, name, JTextArea.class);
                    assertEquals(board.boardBounds().x, label.getX());
                    assertTrue(label.modelToView2D(label.getDocument().getLength() - 1).getMaxY() <= label.getHeight());
                }
                capture(frame, "long-identities");
                return null;
            });
        } finally { close(frame); prefs.removeNode(); }
    }

    @Test void checkpointSelectionsRestoreIndependentlyAndMissingCheckpointNeverFallsBack() throws Exception {
        var entry = TrainingLineages.create(temporary, NetworkArchitecture.BRN_PAIR2, "Persisted Play fixture");
        String checkpoint;
        try (var store = new CheckpointStore(entry.root(), TrainingArchitecture.BRN_PAIR2)) {
            checkpoint = store.initialize(NetworkTrainingState.initialized(TrainingArchitecture.BRN_PAIR2, 13, .003),
                    new CheckpointManifest.Metadata(0, 2, "")).manifest().id();
        }
        Preferences prefs = Preferences.userRoot().node("seedv6-play-checkpoint-test-" + UUID.randomUUID());
        PlayEnginePanel white = null, black = null;
        try {
            prefs.node("white").put("store", entry.root().toString()); prefs.node("white").put("generation", checkpoint);
            prefs.node("white").put("evaluator", PlayEvaluator.Mode.BEST_NNUE.name());
            prefs.node("black").put("evaluator", PlayEvaluator.Mode.CONSTRUCTION_V1.name());
            white = edt(() -> new PlayEnginePanel("white", temporary, prefs.node("white"), () -> {}));
            black = edt(() -> new PlayEnginePanel("black", temporary, prefs.node("black"), () -> {}));
            PlayEnginePanel w = white, b = black;
            until(() -> edt(w::validSelection));
            assertEquals(checkpoint, edt(w::selectedId)); assertEquals(entry.root(), edt(w::selectedRoot));
            assertEquals(PlayEvaluator.Mode.CONSTRUCTION_V1, edt(b::selectedMode));
            var selection = edt(() -> w.versus(b));
            var participants = PlayParticipants.load(PlayEvaluator.Mode.BEST_NNUE, temporary, selection);
            assertEquals(checkpoint, participants.white().checkpointId());
            assertEquals(PlayEvaluator.Mode.CONSTRUCTION_V1, participants.black().mode());
            edt(() -> { w.dispose(); return null; });
            // A saved generation absent from a valid store must stay selected and unavailable.
            prefs.node("white").put("generation", "deleted-checkpoint");
            white = edt(() -> new PlayEnginePanel("white", temporary, prefs.node("white"), () -> {}));
            PlayEnginePanel missing = white;
            until(() -> edt(() -> missing.generation.getSelectedItem() != null));
            assertFalse(edt(missing::validSelection)); assertEquals("deleted-checkpoint", edt(missing::selectedId));
            assertEquals("deleted-checkpoint", prefs.node("white").get("generation", ""));
            edt(() -> { missing.refresh(true); return null; });
            until(() -> edt(() -> missing.generation.isEnabled()));
            assertFalse(edt(missing::validSelection)); assertEquals("deleted-checkpoint", edt(missing::selectedId));
            edt(() -> { missing.dispose(); return null; });
            prefs.node("white").put("store", temporary.resolve("deleted-store").toString());
            white = edt(() -> new PlayEnginePanel("white", temporary, prefs.node("white"), () -> {}));
            PlayEnginePanel absent = white;
            until(() -> edt(() -> !absent.error().isEmpty()));
            edt(() -> { absent.refresh(true); return null; });
            until(() -> edt(() -> !absent.error().isEmpty()));
            assertFalse(edt(absent::validSelection)); assertNull(edt(absent::selectedRoot));
            assertEquals(temporary.resolve("deleted-store").toString(), prefs.node("white").get("store", ""));
        } finally {
            PlayEnginePanel w = white, b = black;
            edt(() -> { if (w != null) w.dispose(); if (b != null) b.dispose(); return null; }); prefs.removeNode();
        }
    }

    private ChessFrame create(Preferences prefs) throws Exception {
        return edt(() -> {
            var settings = TrainingSettings.defaults(temporary, NetworkArchitecture.NNUE);
            var frame = new ChessFrame(settings, new TrainingController.Backend(), ignored -> {}, new TrainingFolders(settings), prefs);
            frame.setSize(1440, 950); frame.setVisible(true); return frame;
        });
    }
    private static GameController controller(ChessFrame frame) throws Exception {
        var field = ChessFrame.class.getDeclaredField("controller"); field.setAccessible(true); return (GameController) field.get(frame);
    }
    private static void close(ChessFrame frame) throws Exception {
        if (!edt(frame::isDisplayable)) return;
        edt(() -> { frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING)); return null; });
        until(() -> !edt(frame::isDisplayable));
    }
    private static void capture(JFrame frame, String name) throws Exception {
        frame.validate(); Path output = Path.of("build/play-redesign"); Files.createDirectories(output);
        var image = new java.awt.image.BufferedImage(frame.getWidth(), frame.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics(); frame.paintAll(graphics); graphics.dispose();
        javax.imageio.ImageIO.write(image, "png", output.resolve(name + ".png").toFile());
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
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (!check.call()) { if (System.nanoTime() > end) fail("Timed out waiting for Play"); Thread.sleep(25); }
    }
    private static <T> T edt(Callable<T> call) throws Exception {
        var result = new AtomicReference<T>(); var failure = new AtomicReference<Throwable>();
        SwingUtilities.invokeAndWait(() -> { try { result.set(call.call()); } catch (Throwable t) { failure.set(t); } });
        if (failure.get() != null) throw new AssertionError(failure.get()); return result.get();
    }
}
