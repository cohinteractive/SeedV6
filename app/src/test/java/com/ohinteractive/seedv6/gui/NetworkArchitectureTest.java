package com.ohinteractive.seedv6.gui;

import java.awt.CardLayout;
import java.nio.file.Path;
import java.util.*;
import java.util.prefs.Preferences;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.service.TrainerConfig;
import com.ohinteractive.seedv6.training.validation.PromotionPolicy;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.edt;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;
import static org.junit.jupiter.api.Assertions.*;

class NetworkArchitectureTest {
    @TempDir Path temp;

    @Test void legacyPreferencesKeepTheirKeysValuesAndExactNnueConfiguration() throws Exception {
        Preferences prefs = Preferences.userRoot().node("seedv6-test-" + UUID.randomUUID());
        var legacy = Map.ofEntries(Map.entry("root", temp.toString()), Map.entry("depth", "3"),
                Map.entry("threads", "2"), Map.entry("games", "7"), Map.entry("openingMin", "1"),
                Map.entry("openingMax", "3"), Map.entry("samples", "9"), Map.entry("minibatch", "4"),
                Map.entry("epochs", "2"), Map.entry("validationPairs", "32"), Map.entry("seed", "73"),
                Map.entry("maximumPlies", "80"), Map.entry("maximumGenerations", "2"));
        try {

            legacy.forEach(prefs::put);
            var loaded = TrainingSettings.load(prefs);
            assertEquals(NetworkArchitecture.NNUE, loaded.architecture());
            assertEquals(new TrainingSettings(temp, 3, 2, 7, 1, 3, 9, 4, 2, 32, 73, 80, 2), loaded);
            for (var change : TrainerConfig.DepthChange.values()) {
                var expected = new TrainerConfig(temp, 73,
                        new TrainerConfig.SelfPlay(3, 2, 7, 1, 3, 9, 80, NnueScoreMapping.V1),
                        new TrainerConfig.Training(2, 4, true),
                        new TrainerConfig.Validation(32, 1, 3, 3, 2, 80, NnueScoreMapping.V1,
                                new PromotionPolicy(32, 0.05, 0)), 2, change);
                assertEquals(expected, loaded.config(change));
            }
            loaded.save(prefs);
            assertEquals(temp.toString(), prefs.get(TrainingFolders.key(NetworkArchitecture.NNUE), null));
            legacy.forEach((key, value) -> assertEquals(value, prefs.get(key, null), key));
            assertEquals(loaded, TrainingSettings.load(prefs));
        } finally { prefs.removeNode(); }
    }

    @Test void nnueOwnsItsEditorsAndSharesEveryLifecycleLock() throws Exception {
        assertArrayEquals(new NetworkArchitecture[]{NetworkArchitecture.NNUE, NetworkArchitecture.BRN, NetworkArchitecture.BRN1, NetworkArchitecture.BRN2}, NetworkArchitecture.values());
        var settings = new TrainingSettings(temp, 3, 2, 7, 1, 3, 9, 4, 2, 32, 73, 80, 2);
        TrainingPanel panel = edt(() -> new TrainingPanel(settings));
        TrainingController controller = edt(() -> new TrainingController(settings, new TrainingController.Backend(),
                ignored -> {}, panel::showState));
        try {
            edt(() -> {
                panel.bind(controller);
                var selector = named(panel, "networkArchitecture", JComboBox.class);
                assertEquals(4, selector.getItemCount()); assertEquals(NetworkArchitecture.NNUE, selector.getSelectedItem());
                var cards = named(panel, "architectureConfiguration", JPanel.class);
                assertInstanceOf(CardLayout.class, cards.getLayout()); assertEquals(4, cards.getComponentCount());
                var nnue = named(panel, "nnueConfiguration", NnueConfigurationPanel.class);
                var batch = named(nnue, "trainingMinibatch", JSpinner.class);
                var epochs = named(nnue, "trainingEpochs", JSpinner.class);
                assertNotNull(batch); assertNotNull(epochs);
                for (String common : List.of("trainingRoot", "trainingDepth", "trainingThreads", "trainingGames",
                        "trainingPairs", "trainingSamples", "trainingSeed")) {
                    assertNotNull(named(panel, common, JComponent.class));
                    assertNull(named(nnue, common, JComponent.class), common + " remains common");
                }
                for (var phase : TrainingController.Phase.values()) {
                    boolean active = switch (phase) {
                        case STARTING, CONFIRM_DEPTH, RUNNING, STOPPING -> true;
                        default -> false;
                    };
                    boolean editable = !active && phase != TrainingController.Phase.CLOSING;
                    panel.showState(new TrainingController.ViewState(settings, phase, null, "", active, editable, false, 3, ""));
                    assertEquals(editable, selector.isEnabled(), phase.toString());
                    assertEquals(editable, batch.isEnabled(), phase.toString());
                    assertEquals(editable, epochs.isEnabled(), phase.toString());
                    assertEquals(editable, named(panel, "trainingDepth", JSpinner.class).isEnabled(), phase.toString());
                }
                panel.showState(controller.state());
                ((JSpinner.DefaultEditor) batch.getEditor()).getTextField().setText("5");
                ((JSpinner.DefaultEditor) epochs.getEditor()).getTextField().setText("3");
                assertTrue(panel.applySettings());
                assertEquals(new TrainingSettings(temp, 3, 2, 7, 1, 3, 9, 5, 3, 32, 73, 80, 2), controller.state().settings());
                assertEquals(new TrainerConfig.Training(3, 5, true), controller.state().settings()
                        .config(TrainerConfig.DepthChange.REQUIRE_SAME).training());
                assertTrue(named(panel, "trainingProgress", JTextArea.class).getText().contains("Network Architecture: NNUE"));
            });
        } finally { edt(controller::beginShutdown).run(); }
    }
}
