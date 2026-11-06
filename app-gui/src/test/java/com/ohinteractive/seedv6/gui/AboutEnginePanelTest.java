package com.ohinteractive.seedv6.gui;

import java.awt.*;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.ApplicationVersion;
import com.ohinteractive.seedv6.search.driver.BaselineEngine;
import static org.junit.jupiter.api.Assertions.*;

class AboutEnginePanelTest {
    @Test void normalPlayReportsUnifiedFormulationAndActualWorkerSchedule() throws Exception {
        String previous = System.getProperty(BaselineEngine.SearchProfile.PROPERTY);
        try {
            System.clearProperty(BaselineEngine.SearchProfile.PROPERTY);
            var binding = PlayEvaluator.bundled(PlayEvaluator.Mode.CONVENTIONAL_CP64);
            for (int workers : new int[]{1, 2, 3, 4, 5, 8, 16}) {
                String text = AboutEnginePanel.play(PlayParticipants.shared(binding), true, false,
                        GameController.GameMode.ENGINE_VS_ENGINE, GameController.HumanSide.WHITE, workers, true);
                assertTrue(text.contains("CLV-C1"));
                assertTrue(text.contains("searchProfile=CLV_C1 "));
                assertTrue(text.contains("flags=3329490943"));
                assertTrue(text.contains("parallel=" + (workers == 1 ? "OWNER" : "EAGER_ROOT")));
                assertTrue(text.contains(binding.baseline().formulation(workers, 64).configurationId()));
            }
            assertTrue(BaselineEngine.productionFormulation().contains("W1-W16: CLV-C1"));
            assertFalse(BaselineEngine.productionFormulation().contains("W4:"));
        } finally {
            if (previous == null) System.clearProperty(BaselineEngine.SearchProfile.PROPERTY);
            else System.setProperty(BaselineEngine.SearchProfile.PROPERTY, previous);
        }
    }
    @Test void idleLoadingAndHumanGamesDoNotClaimAnActiveDefault() {
        var bindings = PlayParticipants.shared(PlayEvaluator.handcrafted());
        assertTrue(play(bindings, false, false).contains("not an active runtime"));
        assertTrue(play(bindings, true, true).contains("configuration is changing"));
        assertTrue(AboutEnginePanel.play(bindings, true, false, GameController.GameMode.HUMAN_VS_HUMAN,
                GameController.HumanSide.WHITE, 1, false).contains("no playing engine"));
    }

    @Test void loadedOverridesAndSeparateSidesUseTheirOwnBindings() throws Exception {
        String previous = System.getProperty(BaselineEngine.SearchProfile.PROPERTY);
        try {
            System.setProperty(BaselineEngine.SearchProfile.PROPERTY, "HL_G1");
            var cp = PlayEvaluator.bundled(PlayEvaluator.Mode.CONVENTIONAL_CP64);
            System.setProperty(BaselineEngine.SearchProfile.PROPERTY, "TC_P1");
            var bindings = new PlayParticipants(cp, PlayEvaluator.handcrafted(), PlayParticipants.Selection.BEST);
            String text = play(bindings, true, false);
            assertTrue(text.contains("White: HL-G1 — retained Search learning and history-guided reductions"));
            assertTrue(text.contains("Black: Construction V1"));
            assertTrue(text.contains(cp.baseline().formulation(4, 64).configurationId()));
            assertTrue(text.contains("flags=104071167"));
            assertFalse(text.contains("ProbCut"));
            assertTrue(AboutEnginePanel.play(bindings, true, false, GameController.GameMode.HUMAN_VS_ENGINE,
                    GameController.HumanSide.BLACK, 4, true).contains(cp.networkHash()));
            var pinned = PlayParticipants.shared(PlayEvaluator.pinned(PlayEvaluator.Mode.POLICY4_PAIR2));
            assertTrue(play(pinned, true, false).contains("recursive-pvs"));
            assertFalse(play(pinned, true, false).contains("TC-P1"));
        } finally {
            if (previous == null) System.clearProperty(BaselineEngine.SearchProfile.PROPERTY);
            else System.setProperty(BaselineEngine.SearchProfile.PROPERTY, previous);
        }
    }

    @Test void panelPreservesMetadataAndProvidesBoundedScrollableCopyableDetails() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var version = ApplicationVersion.load();
            var panel = new AboutEnginePanel(version, "Play test", "full identity");
            var tabs = (JTabbedPane) panel.getComponent(0);
            var summary = (JTextArea) ((JScrollPane) tabs.getComponentAt(0)).getViewport().getView();
            assertTrue(summary.getText().startsWith(version.aboutText()));
            for (String label : new String[]{"Version:", "Build:", "Source revision:", "Desktop build:", "Platform:",
                    "Production default (CP64):", "Arena selects its engines independently"})
                assertTrue(summary.getText().contains(label));
            assertFalse(summary.isEditable());
            assertTrue(summary.getLineWrap());
            assertEquals(new Dimension(660, 490), panel.getPreferredSize());
            var details = (JTextArea) ((JScrollPane) tabs.getComponentAt(1)).getViewport().getView();
            assertTrue(details.getText().contains("full identity"));
        });
    }
    private static String play(PlayParticipants bindings, boolean started, boolean changing) {
        return AboutEnginePanel.play(bindings, started, changing, GameController.GameMode.ENGINE_VS_ENGINE,
                GameController.HumanSide.WHITE, 4, false);
    }
}
