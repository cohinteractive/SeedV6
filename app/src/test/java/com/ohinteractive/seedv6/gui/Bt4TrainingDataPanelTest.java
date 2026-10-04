package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.training.data.*;
import java.nio.file.*;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;

@Timeout(30)
class Bt4TrainingDataPanelTest {
    @TempDir Path temporary;
    String prior;
    @BeforeEach void cache() { prior = System.getProperty("seedv6.preparedDataRoot"); System.setProperty("seedv6.preparedDataRoot", temporary.resolve("cache").toString()); }
    @AfterEach void restore() { if (prior == null) System.clearProperty("seedv6.preparedDataRoot"); else System.setProperty("seedv6.preparedDataRoot", prior); }
    TrainingDataSourcesPanel panel(Path root, NetworkArchitecture architecture) throws Exception {
        var panel = edt(() -> new TrainingDataSourcesPanel(() -> {})); edt(() -> panel.load(root, architecture));
        until(() -> edt(() -> named(panel, "addTrainingDataSource", JButton.class).isEnabled())); return panel;
    }
    @Test void backgroundPreparationPublishesStatusPersistsRegistrationAndSurvivesPanelRestart() throws Exception {
        var source = BinpackFixtures.source(temporary.resolve("bt4")); Path root = temporary.resolve("lineage"); var panel = panel(root, NetworkArchitecture.BRN3);
        edt(() -> {
            panel.acceptSource(source, -1); assertFalse(panel.ready());
            var table = named(panel, "trainingDataSourceTable", JTable.class);
            assertEquals("PREPARING", table.getValueAt(0, 5)); assertEquals(LabelProfile.BT4_Q_V1, table.getValueAt(0, 4));
        });
        until(() -> edt(panel::ready));
        assertEquals(source, DataSources.read(DataSources.directory(root)).sources().getFirst());
        assertEquals(16, PreparedBinpack.ready(source).count());
        edt(() -> {
            var table = named(panel, "trainingDataSourceTable", JTable.class);
            assertEquals("READY", table.getValueAt(0, 5)); table.setValueAt("4", 0, 2); assertDoesNotThrow(panel::save);
        });
        var loaded = panel(root, NetworkArchitecture.BRN3); assertTrue(edt(loaded::ready));
        var reloaded = DataSources.read(DataSources.directory(root)).sources().getFirst();
        assertEquals(4, reloaded.weight()); assertEquals(source.shards(), reloaded.shards()); assertEquals(source.labelProfile(), reloaded.labelProfile());
        var incompatible = panel(root, NetworkArchitecture.BRN2); assertFalse(edt(incompatible::ready));
        assertTrue(edt(() -> named(incompatible, "trainingDataSourceTable", JTable.class).getValueAt(0, 5).toString()).contains("requires BRN-3"));
    }
    @Test void failedPreparationRemainsRegisteredAndHasActionableRetry() throws Exception {
        Path original = BinpackFixtures.archive(temporary.resolve("bad.zst"), BinpackFixtures.chunk(3), new byte[]{'B', 'I'});
        var source = DataSource.register("bad", original, 1, LabelProfile.BT4_Q_V1); Path root = temporary.resolve("lineage"); var panel = panel(root, NetworkArchitecture.BRN3);
        edt(() -> panel.acceptSource(source, -1));
        until(() -> edt(() -> "FAILED".equals(named(panel, "trainingDataSourceTable", JTable.class).getValueAt(0, 5))));
        assertFalse(edt(panel::ready)); assertTrue(Files.isRegularFile(DataSources.directory(root).resolve("sources.json")));
        assertTrue(edt(() -> named(panel, "trainingDataDetails", JTextArea.class).getText()).contains("Prepare / retry"));
        assertTrue(edt(() -> named(panel, "prepareTrainingDataSource", JButton.class).isEnabled()));
        var reopened = panel(root, NetworkArchitecture.BRN3); assertFalse(edt(reopened::ready));
    }
}
