package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.model.TrainingArchitecture;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(40)
class TrainingDataSelectorTest {
    @TempDir Path temporary;
    @Test void closingPickerCancelsAndDrainsDiscoveryWithoutLateCallbacks() throws Exception {
        var callbacks = new java.util.concurrent.atomic.AtomicInteger();
        var picker = edt(() -> new TrainingDataSelector("closing", () -> temporary, () -> List.of(TrainingArchitecture.BRN3), callbacks::incrementAndGet));
        Runnable drain = edt(picker::beginShutdown); int before = callbacks.get(); drain.run();
        edt(() -> { assertFalse(picker.ready()); assertFalse(named(picker, "closingRefresh", JButton.class).isEnabled()); });
        assertEquals(before, callbacks.get());
    }
    @Test void existingTrainingMixBecomesSelectableInArenaWithoutAnotherBrowse() throws Exception {
        Path root = temporary.resolve("lineage"); var folders = new TrainingFolders(TrainingSettings.defaults(root, NetworkArchitecture.BRN3));
        var source = DataSource.register("Reusable Lichess", Files.writeString(temporary.resolve("data.jsonl"), SourceReadersTest.line(100)), 4);
        new DataSources(1, List.of(source), false).save(DataSources.directory(root));
        var mix = edt(() -> new TrainingDataSourcesPanel(folders, () -> {})); edt(() -> mix.load(root, NetworkArchitecture.BRN3));
        until(() -> edt(mix::ready));
        var arena = edt(() -> new LearningArenaPanel(folders));
        var picker = edt(() -> named(arena, "arenaSourceLibrary", TrainingDataSelector.class));
        until(() -> edt(picker::ready));
        assertEquals(source.identity(), edt(picker::selected).identity()); assertEquals(1, edt(picker::selected).weight());
        assertTrue(edt(() -> named(arena, "arenaCreate", JButton.class).isEnabled()));
        edt(() -> { var table = named(mix, "trainingDataSourceTable", JTable.class); table.setRowSelectionInterval(0, 0); named(mix, "removeTrainingDataSource", JButton.class).doClick(); });
        assertFalse(edt(mix::ready)); assertTrue(edt(picker::ready));
        assertEquals(1, new TrainingDataLibrary(folders.base()).browse().sources().size());
        Files.delete(source.path()); edt(() -> named(picker, "arenaSourceRefresh", JButton.class).doClick());
        until(() -> edt(() -> named(picker, "arenaSourceRefresh", JButton.class).isEnabled()));
        assertFalse(edt(picker::ready)); assertFalse(edt(() -> named(arena, "arenaCreate", JButton.class).isEnabled()));
        assertTrue(edt(() -> named(picker, "arenaSourceDetails", JTextArea.class).getText()).contains("Not ready"));
        edt(() -> arena.beginShutdown().run());
    }
    @Test void bt4AppearsWithItsCompatibilityFailureInsteadOfBeingExcluded() throws Exception {
        String before = System.getProperty("seedv6.preparedDataRoot"); System.setProperty("seedv6.preparedDataRoot", temporary.resolve("cache").toString());
        try {
            var source = BinpackFixtures.source(temporary.resolve("bt4")); new TrainingDataLibrary(temporary).register(source);
            var architecture = new java.util.concurrent.atomic.AtomicReference<>(TrainingArchitecture.BRN2);
            var picker = edt(() -> new TrainingDataSelector("testData", () -> temporary, () -> List.of(architecture.get()), () -> {}));
            until(() -> edt(() -> named(picker, "testDataRefresh", JButton.class).isEnabled()));
            assertEquals(source.identity(), edt(picker::selected).identity()); assertFalse(edt(picker::ready));
            assertTrue(edt(() -> named(picker, "testDataDetails", JTextArea.class).getText()).contains("Unsupported"));
            edt(() -> { architecture.set(TrainingArchitecture.BRN3); picker.compatibilityChanged(); named(picker, "testDataPrepare", JButton.class).doClick(); });
            until(() -> edt(picker::ready)); assertEquals(16, PreparedBinpack.ready(source).count());
        } finally { if (before == null) System.clearProperty("seedv6.preparedDataRoot"); else System.setProperty("seedv6.preparedDataRoot", before); }
    }
}
