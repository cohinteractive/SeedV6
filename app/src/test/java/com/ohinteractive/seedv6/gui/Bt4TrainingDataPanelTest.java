package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.training.data.*;
import java.nio.file.*;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
    @ParameterizedTest @EnumSource(value = NetworkArchitecture.class, names = {"NNUE", "BRN3"})
    void backgroundPreparationPublishesStatusPersistsRegistrationAndSurvivesPanelRestart(NetworkArchitecture architecture) throws Exception {
        var source = BinpackFixtures.source(temporary.resolve("bt4")); Path root = temporary.resolve("lineage"); var panel = panel(root, architecture);
        edt(() -> {
            assertEquals("Add file...", named(panel, "addTrainingDataSource", JButton.class).getText());
            assertEquals("Add folder...", named(panel, "addTrainingDataFolder", JButton.class).getText());
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
        var loaded = panel(root, architecture); assertTrue(edt(loaded::ready));
        var reloaded = DataSources.read(DataSources.directory(root)).sources().getFirst();
        assertEquals(4, reloaded.weight()); assertEquals(source.shards(), reloaded.shards()); assertEquals(source.labelProfile(), reloaded.labelProfile());
        var incompatible = panel(root, NetworkArchitecture.BRN2); assertFalse(edt(incompatible::ready));
        assertTrue(edt(() -> named(incompatible, "trainingDataSourceTable", JTable.class).getValueAt(0, 5).toString()).contains("requires scaled centipawn targets"));
        var settings = TrainingSettings.defaults(root, architecture)
                .withSource(com.ohinteractive.seedv6.training.service.TrainingSource.dataSources(DataSources.directory(root)));
        var provider = edt(() -> new BrnTrainingSourcePanel(settings, () -> {}));
        until(() -> edt(provider::ready));
        assertEquals(com.ohinteractive.seedv6.training.service.CorpusTraining.sourceOutcomeAdapter(architecture.trainingArchitecture()),
                edt(provider::readCorpusConfig).targetAdapter());
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
    @Test void lichessPzstdAndIndividualBinpackBecomeReadyForNnue() throws Exception {
        byte[] compressed = com.github.luben.zstd.Zstd.compress(SourceReadersTest.line(100).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        byte[] framed = java.nio.ByteBuffer.allocate(12 + compressed.length).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                .putInt(0x184d2a50).putInt(4).putInt(compressed.length).put(compressed).array();
        var lichess = DataSource.register("Lichess", Files.write(temporary.resolve("lichess.jsonl.zst"), framed), 1);
        assertEquals(DataSource.Format.LICHESS_PZSTD, lichess.format());
        Path root = temporary.resolve("lineage"); var panel = panel(root, NetworkArchitecture.NNUE);
        edt(() -> {
            panel.acceptSource(lichess, -1); assertTrue(panel.ready());
            assertEquals("READY", named(panel, "trainingDataSourceTable", JTable.class).getValueAt(0, 5));
        });
        Path archive = BinpackFixtures.archive(temporary.resolve("individual.zst"), BinpackFixtures.chunk(100, -200));
        var binpack = DataSource.register("individual", archive, 1, LabelProfile.BT4_Q_V1);
        edt(() -> panel.acceptSource(binpack, -1));
        until(() -> edt(panel::ready));
        assertEquals(1, binpack.shards().size());
        assertEquals(2, PreparedBinpack.ready(binpack).count());
        assertEquals(java.util.List.of(lichess, binpack), DataSources.read(DataSources.directory(root)).sources());
        assertTrue(edt(panel(root, NetworkArchitecture.NNUE)::ready));
    }
}
