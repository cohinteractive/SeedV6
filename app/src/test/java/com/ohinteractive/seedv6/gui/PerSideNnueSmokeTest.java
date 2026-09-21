package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointManifest;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

/** Native Swing actions and rendering at the normal Play window size. */
@Tag("slow-nnue")
@Timeout(120)
class PerSideNnueSmokeTest {
    @TempDir Path root;
    private ChessFrame frame;
    @AfterEach void close() throws Exception {
        if (frame != null) {
            edt(() -> frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING)));
            until(() -> !edt(frame::isDisplayable));
        }
    }

    @Test void selectorsPinDisplayedParticipantsRefreshWithoutFallbackAndStayWithinExistingGameCard() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        String best = bootstrap(root), other = candidate(root, false);
        frame = edt(() -> new ChessFrame(settings(root, 1, 1), new TrainingController.Backend(), ignored -> {}));
        edt(() -> {
            frame.setVisible(true); frame.setSize(SeedTheme.scale(1440), SeedTheme.scale(950)); frame.validate();
            assertFalse(combo("whiteNetwork").isShowing());
            combo("gameMode").setSelectedItem(GameController.GameMode.HUMAN_VS_HUMAN);
            spinner("playThreads").setValue(1); spinner("playDepth").setValue(1);
            combo("playEvaluator").setSelectedItem(PlayEvaluator.Mode.BEST_NNUE);
        });
        until(() -> edt(() -> button("newGame").isEnabled()));
        edt(() -> {
            assertFalse(combo("whiteNetwork").isShowing());
            combo("gameMode").setSelectedItem(GameController.GameMode.ENGINE_VS_ENGINE);
        });
        until(() -> edt(() -> combo("whiteNetwork").getItemCount() == 3));
        edt(() -> {
            assertEquals(PlayEvaluator.Choice.BEST, combo("whiteNetwork").getSelectedItem());
            assertEquals(PlayEvaluator.Choice.BEST, combo("blackNetwork").getSelectedItem());
            combo("whiteNetwork").setSelectedItem(new PlayEvaluator.Choice(other));
            combo("blackNetwork").setSelectedItem(new PlayEvaluator.Choice(best));
            button("newGame").doClick();
        });
        until(() -> edt(() -> button("newGame").isEnabled()
                && label("whitePlayerNetwork").getText().contains(PlayEvaluator.shortId(other))));
        edt(() -> button("stopSearch").doClick());
        until(() -> edt(() -> !button("stopSearch").isEnabled()));
        edt(() -> {
            assertTrue(label("blackPlayerNetwork").getText().contains(PlayEvaluator.shortId(best)));
            assertTrue(label("whitePlayerNetwork").getToolTipText().contains(other));
            assertTrue(label("blackPlayerNetwork").getToolTipText().contains(best));
            assertLayout(); capture("different-networks.png");
            // Changing next-game choices must not change either the board identities or score attribution.
            combo("whiteNetwork").setSelectedItem(new PlayEvaluator.Choice(best));
            combo("blackNetwork").setSelectedItem(new PlayEvaluator.Choice(other));
            assertTrue(label("whitePlayerNetwork").getText().contains(PlayEvaluator.shortId(other)));
            for (int side : new int[] {Value.WHITE, Value.BLACK}) {
                frame.showSearch(new GameController.SearchInfo("Idle", 2, "cp 15", 100, 1000, "e2e4", "DEPTH", 100, side));
                String expected = side == Value.WHITE ? other : best;
                assertTrue(label("searchTermination").getToolTipText().contains(expected));
                assertTrue(label("searchTermination").getText().contains(PlayEvaluator.shortId(expected)));
            }
            assertLayout(); capture("next-game-selections.png");
            frame.setSize(SeedTheme.scale(1100), SeedTheme.scale(760)); frame.validate(); assertLayout(); capture("minimum-size.png");
            frame.setSize(SeedTheme.scale(1440), SeedTheme.scale(950)); frame.validate();
        });
        String promoted = candidate(root, true);
        Files.delete(root.resolve("checkpoints").resolve(other).resolve(CheckpointManifest.NETWORK_FILE));
        edt(() -> combo("blackNetwork").setPopupVisible(true));
        until(() -> edt(() -> contains(combo("whiteNetwork"), promoted)));
        edt(() -> {
            combo("blackNetwork").setPopupVisible(false);
            assertFalse(contains(combo("blackNetwork"), other));
            assertEquals(new PlayEvaluator.Choice(other), combo("blackNetwork").getSelectedItem(), "Missing selection cannot silently become Best");
            assertTrue(label("blackPlayerNetwork").getText().contains(PlayEvaluator.shortId(best)), "Promotion does not replace active players");
            capture("unavailable-selection.png");
            combo("gameMode").setSelectedItem(GameController.GameMode.HUMAN_VS_ENGINE);
        });
        until(() -> edt(() -> button("newGame").isEnabled()));
        edt(() -> {
            assertFalse(combo("whiteNetwork").isShowing()); assertFalse(combo("blackNetwork").isShowing());
            assertTrue(named("pinnedBest", JLabel.class).isShowing());
            assertTrue(label("blackPlayerNetwork").getText().contains(PlayEvaluator.shortId(promoted)));
            combo("gameMode").setSelectedItem(GameController.GameMode.HUMAN_VS_HUMAN);
            combo("playEvaluator").setSelectedItem(PlayEvaluator.Mode.HANDCRAFTED);
        });
        until(() -> edt(() -> button("newGame").isEnabled()));
        edt(() -> { assertFalse(combo("whiteNetwork").isShowing()); assertLayout(); });
    }

    private void assertLayout() {
        for (String name : new String[] {"whiteNetwork", "blackNetwork", "gameMode", "playEvaluator"}) {
            var box = combo(name);
            if (!box.isShowing()) continue;
            assertTrue(box.getWidth() >= 130, name + " width " + box.getWidth());
            assertTrue(box.getVisibleRect().contains(new Rectangle(0, 0, box.getWidth(), box.getHeight())), name);
        }
        var board = named("chessBoard", BoardPanel.class);
        assertTrue(board.getVisibleRect().contains(board.boardBounds()));
    }
    private static boolean contains(JComboBox<?> box, String id) {
        for (int i = 0; i < box.getItemCount(); i++) if (box.getItemAt(i).equals(new PlayEvaluator.Choice(id))) return true;
        return false;
    }
    private void capture(String file) {
        try {
            Path directory = Path.of("build", "gui-smoke", "per-side", System.getProperty("flatlaf.uiScale", "100%"));
            Files.createDirectories(directory);
            BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics(); frame.paintAll(graphics); graphics.dispose();
            ImageIO.write(image, "png", directory.resolve(file).toFile());
        } catch (Exception failure) { throw new AssertionError(failure); }
    }
    private JComboBox<?> combo(String name) { return named(name, JComboBox.class); }
    private JSpinner spinner(String name) { return named(name, JSpinner.class); }
    private JButton button(String name) { return named(name, JButton.class); }
    private JLabel label(String name) { return named(name, JLabel.class); }
    private <T extends Component> T named(String name, Class<T> type) { return find(frame, name, type); }
    private static <T extends Component> T find(Container parent, String name, Class<T> type) {
        for (Component child : parent.getComponents()) {
            if (name.equals(child.getName())) return type.cast(child);
            if (child instanceof Container container) {
                T found = find(container, name, type); if (found != null) return found;
            }
        }
        return null;
    }
}
