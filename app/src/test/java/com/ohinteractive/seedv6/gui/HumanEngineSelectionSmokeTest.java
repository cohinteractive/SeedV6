package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.training.model.TrainingArchitecture;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static com.ohinteractive.seedv6.gui.PlayStoreWorkflowTest.*;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class HumanEngineSelectionSmokeTest {
    @TempDir Path temp;
    private ChessFrame frame;
    @AfterEach void close() throws Exception {
        if (frame != null) {
            edt(() -> frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING)));
            until(() -> !edt(frame::isDisplayable));
        }
    }

    @Test void nativeEngineVsEngineStillPinsIndependentStoresAndExplicitGenerations() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Path a = temp.resolve("white lineage"), b = temp.resolve("black lineage");
        String best = brnStore(a); brnStore(b); String explicit = publish(b, 3, false, true).candidateId();
        frame = edt(() -> new ChessFrame(settings(a, 1, 1), new TrainingController.Backend(), ignored -> {}));
        edt(() -> {
            frame.setSize(1440, 950);
            combo("gameMode").setSelectedItem(GameController.GameMode.ENGINE_VS_ENGINE);
            named(frame, "blackEngineSetup", PlayEnginePanel.class).selectStore(b);
            named(frame, "playDepth", JSpinner.class).setValue(1);
            assertFalse(combo("playEvaluator").isVisible());
            assertFalse(opponentVisible());
            assertFalse(button("stopSearch").isEnabled());
        });
        until(() -> edt(() -> button("startGame").isEnabled()));
        edt(() -> {
            combo("blackNetwork").setSelectedItem(new ModelChoice(explicit));
            button("swapSides").doClick();
            assertEquals(b, named(frame, "whiteEngineSetup", PlayEnginePanel.class).selectedRoot());
            assertEquals(new ModelChoice(explicit), combo("whiteNetwork").getSelectedItem());
            assertEquals(new ModelChoice(best), combo("blackNetwork").getSelectedItem());
            button("swapSides").doClick();
            assertEquals(a, named(frame, "whiteEngineSetup", PlayEnginePanel.class).selectedRoot());
            assertEquals(new ModelChoice(explicit), combo("blackNetwork").getSelectedItem());
            assertFalse(button("stopSearch").isEnabled(), "Setup alone cannot start engine play");
            button("startGame").doClick();
        });
        until(() -> edt(() -> button("stopSearch").isEnabled()));
        edt(() -> button("stopSearch").doClick());
        until(() -> edt(() -> !button("stopSearch").isEnabled()));
        edt(() -> {
            assertTrue(label("whitePlayerNetwork").getToolTipText().contains(best));
            assertTrue(label("blackPlayerNetwork").getToolTipText().contains(explicit));
            combo("blackNetwork").setSelectedItem(ModelChoice.BEST);
            assertTrue(label("blackPlayerNetwork").getToolTipText().contains(explicit));
            frame.validate();
            WorkflowRefinementGuiTest.capture(frame.getRootPane(), "human-engine-parity-engine-mode.png");
        });
    }

    @Test void nativeOpponentControlsShareEngineCatalogPinLabelsAndRequireNewGame() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Path a = temp.resolve("BRN2 lineage A"), b = temp.resolve("BRN2 lineage B");
        brnStore(a); brnStore(b); String explicit = publish(b, 3, false, true).candidateId();
        frame = edt(() -> new ChessFrame(settings(a, 1, 1), new TrainingController.Backend(), ignored -> {}));
        edt(() -> {
            frame.setSize(1440, 950); frame.validate();
            assertTrue(button("newGame").isEnabled());
            assertFalse(opponentVisible());
            assertEquals("Handcrafted evaluator", label("blackPlayerNetwork").getText());
            named(frame, "playDepth", JSpinner.class).setValue(1);
            combo("playEvaluator").setSelectedItem(PlayEvaluator.Mode.BEST_NNUE);
            named(frame, "opponentEngineSetup", PlayEnginePanel.class).selectStore(b);
            named(frame, "whiteEngineSetup", PlayEnginePanel.class).selectStore(b);
        });
        until(() -> edt(() -> opponent().validSelection()
                && named(frame, "whiteNetwork", JComboBox.class).getItemCount() == 3));
        edt(() -> {
            assertEquals(choices(combo("whiteNetwork")), choices(combo("opponentNetwork")));
            assertTrue(opponentVisible());
            var renderer = combo("playEvaluator").getRenderer();
            assertEquals("Network", ((JLabel) renderer.getListCellRendererComponent(new JList(),
                    PlayEvaluator.Mode.BEST_NNUE, 1, false, false)).getText());
            combo("opponentNetwork").setSelectedItem(new ModelChoice(explicit));
            assertEquals("Handcrafted evaluator", label("blackPlayerNetwork").getText());
            assertFalse(button("stopSearch").isEnabled());
            button("newGame").doClick();
        });
        until(() -> edt(() -> label("blackPlayerNetwork").getText().equals("BRN-2 \u00b7 " + b.getFileName() + " \u00b7 Gen 3")));
        edt(() -> {
            assertTrue(label("blackPlayerNetwork").getToolTipText().contains(explicit));
            assertEquals("White pieces", label("whitePlayerNetwork").getText());
            assertTrue(label("pinnedBest").getToolTipText().contains(explicit));
            frame.showSearch(new GameController.SearchInfo("Idle", 1, "cp 15", 100, 1000, "e7e5", "DEPTH", 100, Value.BLACK));
            assertTrue(label("searchTermination").getToolTipText().contains(explicit));
            frame.validate(); assertLayout();
            WorkflowRefinementGuiTest.capture(frame.getRootPane(), "human-engine-network.png");
            frame.setSize(1100, 760); frame.validate(); assertLayout();
            WorkflowRefinementGuiTest.capture(frame.getRootPane(), "human-engine-minimum.png");
            frame.setSize(1440, 950); frame.validate();
            combo("opponentNetwork").setSelectedItem(ModelChoice.BEST);
            assertTrue(label("blackPlayerNetwork").getToolTipText().contains(explicit));
            combo("playEvaluator").setSelectedItem(PlayEvaluator.Mode.HANDCRAFTED);
            assertFalse(opponentVisible());
            assertTrue(label("blackPlayerNetwork").getToolTipText().contains(explicit), "Evaluator edits are also next-game only");
            combo("playEvaluator").setSelectedItem(PlayEvaluator.Mode.BEST_NNUE);
            named(frame, "opponentEngineSetup", PlayEnginePanel.class).selectStore(a);
            assertTrue(label("blackPlayerNetwork").getToolTipText().contains(explicit));
        });
        until(() -> edt(() -> button("newGame").isEnabled()));
        edt(() -> {
            combo("humanSide").setSelectedItem(GameController.HumanSide.BLACK);
            assertTrue(label("whitePlayerNetwork").getToolTipText().contains(explicit));
        });
        until(() -> edt(() -> !button("stopSearch").isEnabled()));
        edt(() -> {
            combo("humanSide").setSelectedItem(GameController.HumanSide.WHITE);
            button("newGame").doClick();
        });
        until(() -> edt(() -> label("blackPlayerNetwork").getText().equals("BRN-2 \u00b7 " + a.getFileName() + " \u00b7 Gen 0")));
        // A stale selection remains visible and disables New Game; HCE still needs no store.
        edt(() -> named(frame, "opponentEngineSetup", PlayEnginePanel.class).selectStore(b));
        until(() -> edt(() -> opponent().validSelection()));
        edt(() -> combo("opponentNetwork").setSelectedItem(new ModelChoice(explicit)));
        Files.delete(b.resolve("checkpoints").resolve(explicit).resolve(TrainingArchitecture.BRN2.networkFile()));
        edt(() -> button("opponentRefreshNetworks").doClick());
        until(() -> edt(() -> combo("opponentNetwork").getItemCount() == 2));
        edt(() -> {
            assertEquals(new ModelChoice(explicit), combo("opponentNetwork").getSelectedItem());
            assertFalse(button("newGame").isEnabled());
            combo("playEvaluator").setSelectedItem(PlayEvaluator.Mode.HANDCRAFTED);
            assertTrue(button("newGame").isEnabled()); button("newGame").doClick();
        });
        until(() -> edt(() -> label("blackPlayerNetwork").getText().equals("Handcrafted evaluator")));
        edt(() -> {
            assertFalse(opponentVisible());
            combo("gameMode").setSelectedItem(GameController.GameMode.ENGINE_VS_ENGINE);
            assertTrue(named(frame, "whiteNetwork", JComboBox.class).isVisible());
            assertFalse(combo("playEvaluator").isVisible());
        });
    }

    private void assertLayout() {
        for (String name : List.of("opponentArchitecture", "opponentLineage", "opponentNetwork", "opponentStoreIdentity", "opponentRegisterModel", "opponentGenerationDetails")) {
            var field = named(frame, name, JComponent.class);
            assertTrue(field.getWidth() > 130 && field.getHeight() > 0, name);
            assertTrue(field.getVisibleRect().contains(new Rectangle(0, 0, field.getWidth(), field.getHeight())), name + " clipped");
        }
    }
    private PlayEnginePanel opponent() { return named(frame, "opponentEngineSetup", PlayEnginePanel.class); }
    private boolean opponentVisible() { return opponent().isVisible() && opponent().getParent().isVisible(); }
    private JButton button(String name) { return named(frame, name, JButton.class); }
    private JLabel label(String name) { return named(frame, name, JLabel.class); }
    private JComboBox combo(String name) { return named(frame, name, JComboBox.class); }
    private static List<Object> choices(JComboBox<?> box) {
        var values = new ArrayList<Object>();
        for (int i = 0; i < box.getItemCount(); i++) values.add(box.getItemAt(i));
        return values;
    }
}
