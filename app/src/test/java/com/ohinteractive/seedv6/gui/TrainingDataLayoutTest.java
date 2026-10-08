package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.training.service.*;
import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import java.awt.*;
import java.nio.file.*;
import java.util.List;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;

class TrainingDataLayoutTest {
    @TempDir Path temporary;
    @Test void configurationLifetimePlacementVisibilityAndTerminologyForBothArchitectures() throws Exception {
        Path data = temporary.resolve("source.jsonl"); Files.writeString(data, com.ohinteractive.seedv6.training.data.SourceReadersTest.line(1));
        for (var architecture : List.of(NetworkArchitecture.NNUE, NetworkArchitecture.BRN2)) {
            Path root = temporary.resolve(architecture.name());
            try (var store = new CheckpointStore(root, architecture.trainingArchitecture())) {
                new DataSources(1, List.of(DataSource.register("Source", data, 1)), false).save(DataSources.directory(root));
            }
            var settings = TrainingSettings.defaults(root, architecture).withSource(TrainingSource.dataSources(DataSources.directory(root)))
                    .withCorpus(DataSources.directory(root).toString(), new CorpusTrainingConfig(10)).withValidationMethod(ValidationMethod.HELD_OUT);
            var panel = edt(() -> new TrainingPanel(settings));
            until(() -> edt(() -> named(panel, "trainingDataSourceTable", JTable.class).getRowCount() == 1));
            edt(() -> {
                var tabs = named(panel, "trainingViews", JTabbedPane.class);
                Component configuration = tabs.getComponentAt(tabs.indexOfTab("Validation Settings"));
                Component network = tabs.getComponentAt(tabs.indexOfTab("Lineage Settings"));
                Component sources = tabs.getComponentAt(tabs.indexOfTab("Training Settings"));
                assertTrue(SwingUtilities.isDescendingFrom(named(panel, "trainingValidationMethod", JComboBox.class), configuration));
                assertTrue(SwingUtilities.isDescendingFrom(named(panel, "brnCorpusPositions", JSpinner.class), sources));
                assertTrue(SwingUtilities.isDescendingFrom(named(panel, "architectureConfiguration", JPanel.class), network));
                assertTrue(SwingUtilities.isDescendingFrom(named(panel, "brnTrainingSource", JComboBox.class), sources));
                panel.setSize(800, 850); tabs.setSelectedIndex(tabs.indexOfTab("Training Settings")); layout(panel); layout(panel);
                var networkScroll = (JScrollPane) sources;
                assertEquals(networkScroll.getViewport().getWidth(), networkScroll.getViewport().getView().getWidth());
                var control = named(panel, architecture == NetworkArchitecture.NNUE ? "trainingEpochs" : "recipeLearningRate", JSpinner.class);
                Rectangle bounds = SwingUtilities.convertRectangle(control.getParent(), control.getBounds(), networkScroll.getViewport());
                assertTrue(bounds.width > 75 && bounds.height > 0, bounds.toString());
                assertTrue(bounds.x >= 0 && bounds.x + bounds.width <= networkScroll.getViewport().getWidth(), bounds.toString());
                if (architecture == NetworkArchitecture.BRN2) assertFalse(named(panel, "brn2DataSeed", JTextField.class).isVisible());
                assertTrue(SwingUtilities.isDescendingFrom(named(panel, "trainingDataSourceTable", JTable.class), sources));
                assertFalse(named(panel, "trainingGames", JSpinner.class).isVisible());
                assertFalse(named(panel, "trainingDepth", JSpinner.class).isVisible());
                assertFalse(named(panel, "trainingPairs", JSpinner.class).isVisible());
                named(panel, "trainingValidationMethod", JComboBox.class).setSelectedItem(ValidationMethod.GAME_PAIRS);
                assertTrue(named(panel, "trainingDepth", JSpinner.class).isVisible());
                assertTrue(named(panel, "trainingPairs", JSpinner.class).isVisible());
                named(panel, "brnTrainingSource", JComboBox.class).setSelectedItem(TrainingSource.Mode.SELF_PLAY);
                assertTrue(named(panel, "trainingGames", JSpinner.class).isVisible());
                assertFalse(named(panel, "brnCorpusPositions", JSpinner.class).isVisible());
                assertNoTrainingDepthLabel(panel);
            });
        }
    }
    @Test void storageIsAnApplicationMenuAction() throws Exception {
        var invoked = new java.util.concurrent.atomic.AtomicBoolean();
        edt(() -> {
            JMenuBar menu = ApplicationMenu.create(() -> {}, () -> {}, () -> invoked.set(true));
            JMenuItem storage = menu.getMenu(0).getItem(0); assertEquals("trainingStorageSettings", storage.getName()); storage.doClick();
        });
        assertTrue(invoked.get());
    }
    private static void layout(Container parent) { parent.doLayout(); for (Component child : parent.getComponents()) if (child instanceof Container nested) layout(nested); }
    private static void assertNoTrainingDepthLabel(Component component) {
        String text = component instanceof JLabel label ? label.getText() : component instanceof AbstractButton button ? button.getText()
                : component instanceof JTextArea area ? area.getText() : "";
        assertFalse(text != null && text.equals("Training depth"), () -> "Visible terminology: " + text);
        if (component instanceof Container container) for (Component child : container.getComponents()) assertNoTrainingDepthLabel(child);
    }
}
