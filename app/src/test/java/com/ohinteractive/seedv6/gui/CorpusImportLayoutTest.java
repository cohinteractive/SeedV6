package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.nio.file.Path;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.edt;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;
import static org.junit.jupiter.api.Assertions.*;

/** Sizes real Swing containers without constructing a frame or invoking any chooser. */
class CorpusImportLayoutTest {
    @TempDir Path temp;

    @Test void pathRowsAndOrdinaryControlsFitViewportAndFieldsFlex() throws Exception {
        edt(() -> {
            SeedTheme.initialize();
            try {
                for (float zoom : new float[]{1f, 1.5f}) {
                    com.formdev.flatlaf.util.UIScale.setZoomFactor(zoom);
                    checkPathRows();
                }
            } finally { com.formdev.flatlaf.util.UIScale.setZoomFactor(1); }
        });
    }
    private void checkPathRows() {
        var body = new CorpusImportPanel(new TrainingFolders(TrainingSettings.defaults()), path -> {});
        var archive = named(body, "corpusImportArchive", JTextField.class);
        var root = named(body, "corpusImportRoot", JTextField.class);
        String longPath = temp.resolve("long-path-segment-".repeat(20)).toString();
        archive.setText(longPath + ".jsonl.zst"); root.setText(longPath);
        var pane = TrainingDashboard.scroll(body);
        assertTrue(body.getMinimumSize().width <= SeedTheme.scale(480));
        assertTrue(body.getPreferredSize().width <= SeedTheme.scale(610), "Ordinary controls should prefer a normal content width");
        int previousFieldWidth = -1, previousRootWidth = -1, previousVisibleWidth = -1;
        for (int width : new int[]{480, 610, 760, 900, 610, 480}) {
            pane.setSize(SeedTheme.scale(width), SeedTheme.scale(500)); layout(pane);
            int visibleWidth = pane.getViewport().getExtentSize().width;
            assertEquals(visibleWidth, body.getWidth(), "Corpus content must track viewport width");
            assertFalse(pane.getHorizontalScrollBar().isVisible(), "Ordinary controls must not require horizontal scrolling");
            assertTrue(body.getMinimumSize().width <= visibleWidth, "Minimum width must permit normal contraction: content="
                    + width + " viewport=" + visibleWidth + " minimum=" + body.getMinimumSize().width);
            for (JTextField field : new JTextField[]{archive, root}) {
                var browse = (JButton) ((BorderLayout) field.getParent().getLayout()).getLayoutComponent(BorderLayout.EAST);
                assertNotNull(browse); assertTrue(browse.isEnabled()); assertEquals(browse.getPreferredSize().width, browse.getWidth());
                assertFits(body, field, visibleWidth); assertFits(body, browse, visibleWidth);
            }
            for (Component child : archive.getParent().getParent().getComponents()) if (child instanceof JLabel label) {
                assertFits(body, label, visibleWidth); assertEquals(label.getPreferredSize().width, label.getWidth());
            }
            for (String name : new String[]{"corpusImportAll", "corpusImportBounded", "corpusImportLimit",
                    "startCorpusImport", "stopCorpusImport", "validateCorpus"}) assertFits(body, named(body, name, JComponent.class), visibleWidth);
            if (previousVisibleWidth > 0) {
                assertTrue((archive.getWidth() - previousFieldWidth) * (visibleWidth - previousVisibleWidth) > 0,
                        "Archive field should expand and contract with the viewport");
                assertTrue((root.getWidth() - previousRootWidth) * (visibleWidth - previousVisibleWidth) > 0,
                        "Root field should expand and contract with the viewport");
            }
            previousFieldWidth = archive.getWidth(); previousRootWidth = root.getWidth(); previousVisibleWidth = visibleWidth;
        }
        assertEquals(longPath + ".jsonl.zst", archive.getText()); assertEquals(longPath, root.getText());
        pane.setSize(SeedTheme.scale(480), SeedTheme.scale(170)); layout(pane);
        assertTrue(pane.getVerticalScrollBar().isVisible(), "Short content area should retain vertical scrolling");
        assertFalse(pane.getHorizontalScrollBar().isVisible());
        assertEquals(pane.getViewport().getExtentSize().width, body.getWidth());
    }

    @Test void corpusFitsActualNetworkTrainingTabAtNormalWidths() throws Exception {
        edt(() -> {
            SeedTheme.initialize();
            var workspace = new TrainingPanel(TrainingSettings.defaults(temp.resolve("unused-training"), NetworkArchitecture.NNUE));
            var tabs = named(workspace, "trainingViews", JTabbedPane.class);
            int corpusTab = tabs.indexOfTab("Data & exposure"); assertTrue(corpusTab >= 0); tabs.setSelectedIndex(corpusTab);
            var pane = (JScrollPane) tabs.getSelectedComponent(); var body = (Container) pane.getViewport().getView();
            for (int width : new int[]{610, 760, 980, 610}) {
                workspace.setSize(SeedTheme.scale(width), SeedTheme.scale(700)); layout(workspace);
                int visibleWidth = pane.getViewport().getExtentSize().width;
                assertEquals(visibleWidth, body.getWidth()); assertFalse(pane.getHorizontalScrollBar().isVisible());
                assertFits(body, named(body, "brnTrainingSource", JComboBox.class), visibleWidth);
            }
        });
    }

    private static void assertFits(Container body, JComponent control, int width) {
        Rectangle bounds = SwingUtilities.convertRectangle(control.getParent(), control.getBounds(), body);
        assertTrue(bounds.width > 0 && bounds.height > 0 && bounds.x >= 0 && bounds.x + bounds.width <= width,
                control.getName() + " bounds=" + bounds + " viewport width=" + width);
    }
    private static void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) if (child instanceof Container nested) layout(nested);
    }
}
