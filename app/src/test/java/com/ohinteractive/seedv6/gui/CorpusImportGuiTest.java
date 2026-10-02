package com.ohinteractive.seedv6.gui;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.prefs.Preferences;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.github.luben.zstd.ZstdOutputStream;
import com.ohinteractive.seedv6.corpus.*;
import com.ohinteractive.seedv6.corpus.lichess.LichessImporter;
import com.ohinteractive.seedv6.training.service.*;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;
import static org.junit.jupiter.api.Assertions.*;

/** Components/controllers only: no frames, dialogs or chooser invocation, even on a desktop. */
@Timeout(25)
class CorpusImportGuiTest {
    @TempDir Path temp;
    private static final CorpusWriter.Stats STATS = new CorpusWriter.Stats(8, 7, 1, 3, 4, 0, 4, 1);
    TrainingSettings settings() {
        return new TrainingSettings(temp.resolve("training"), 1, 1, 1, 0, 0, 1, 32, 1, 1, 73, 1, 1,
                NetworkArchitecture.BRN2, .001, .001, .002).withSource(TrainingSource.HANDCRAFTED);
    }
    Path archive() throws IOException {
        Path input = temp.resolve("source.jsonl.zst");
        String first = "{\"fen\":\"4k3/8/8/8/8/8/8/4K3 w - - 0 1\",\"evals\":[{\"depth\":10,\"pvs\":[{\"cp\":7}]}]}\n";
        String second = first.replace(" - - 0 1", " - - 1 1");
        try (var out = new ZstdOutputStream(Files.newOutputStream(input))) {
            out.write((first + second + "invalid\n").getBytes(StandardCharsets.UTF_8));
        }
        return input;
    }
    static class FakeBackend implements CorpusImportController.Backend {
        final AtomicInteger imports = new AtomicInteger(), validations = new AtomicInteger();
        LichessImporter.Options options;
        public LichessImporter.Summary run(LichessImporter.Options options, Consumer<LichessImporter.Progress> progress, BooleanSupplier stop) throws Exception {
            assertFalse(SwingUtilities.isEventDispatchThread()); imports.incrementAndGet(); this.options = options;
            progress.accept(new LichessImporter.Progress(STATS, 11, 2));
            return new LichessImporter.Summary(STATS, 11, 2, stop.getAsBoolean());
        }
        public CorpusReader.Integrity validate(Path root) throws Exception {
            assertFalse(SwingUtilities.isEventDispatchThread()); validations.incrementAndGet();
            return new CorpusReader.Integrity(11, 14, 2);
        }
    }
    CorpusImportController controller(CorpusImportController.Backend backend, Consumer<String> refreshed) throws Exception {
        return edt(() -> new CorpusImportController(backend, state -> assertTrue(SwingUtilities.isEventDispatchThread()), refreshed));
    }
    void finished(CorpusImportController controller) throws Exception { until(() -> edt(() -> !controller.state().active())); }

    @Test void allAndBoundedUseExactProductionDefaults() {
        var all = new CorpusImportController.Request("a.jsonl.zst", temp.toString(), true, 9).options();
        var bounded = new CorpusImportController.Request("a.jsonl.zst", temp.toString(), false, 87).options();
        assertEquals(0, all.maxRecords()); assertEquals(87, bounded.maxRecords());
        assertEquals(100_000, all.shardSize()); assertEquals(100_000, all.progressEvery());
        assertThrows(IllegalArgumentException.class, () -> new CorpusImportController.Request("", temp.toString(), true, 9).options());
        assertThrows(IllegalArgumentException.class, () -> new CorpusImportController.Request("a", "", true, 9).options());
        assertThrows(IllegalArgumentException.class, () -> new CorpusImportController.Request("a", temp.toString(), false, 0).options());
    }

    @Test void missingAndUnsupportedSourcesAndInvalidRootsFailBeforeBackend() throws Exception {
        var backend = new FakeBackend(); var controller = controller(backend, root -> {});
        Path valid = archive(), fileRoot = Files.writeString(temp.resolve("not-dir"), "x");
        for (var request : List.of(
                new CorpusImportController.Request(temp.resolve("absent.jsonl.zst").toString(), temp.toString(), true, 1),
                new CorpusImportController.Request(fileRoot.toString(), temp.toString(), true, 1),
                new CorpusImportController.Request(temp.getRoot().toString(), temp.toString(), true, 1),
                new CorpusImportController.Request(valid.toString(), temp.resolve("absent").toString(), true, 1),
                new CorpusImportController.Request(valid.toString(), fileRoot.toString(), true, 1))) {
            edt(() -> controller.start(request)); finished(controller);
            var state = edt(controller::state); assertEquals(CorpusImportController.Phase.FAILED, state.phase());
            assertTrue(state.message().contains("existing"), state.message());
        }
        assertEquals(0, backend.imports.get());
    }

    @Test void backgroundProgressIsCoalescedAndCompletionRefreshesTotal() throws Exception {
        var release = new CountDownLatch(1); var ready = new CountDownLatch(1);
        var refreshed = new AtomicReference<String>();
        var backend = new FakeBackend() {
            public LichessImporter.Summary run(LichessImporter.Options options, Consumer<LichessImporter.Progress> progress, BooleanSupplier stop) throws Exception {
                super.run(options, progress, stop);
                for (int i = 0; i < 1000; i++) progress.accept(new LichessImporter.Progress(STATS, 11, i));
                ready.countDown(); assertTrue(release.await(10, TimeUnit.SECONDS));
                return new LichessImporter.Summary(STATS, 11, 999, false);
            }
        };
        var controller = controller(backend, refreshed::set);
        Path input = archive();
        edt(() -> controller.start(new CorpusImportController.Request(input.toString(), temp.toString(), false, 8)));
        try {
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            edt(controller::poll);
            var state = edt(controller::state);
            assertEquals(STATS, state.progress().stats()); assertEquals(999, state.progress().elapsedSeconds());
            assertEquals(11, state.progress().corpusTotal()); assertEquals(8, backend.options.maxRecords());
            assertEquals(CorpusImportController.Phase.IMPORTING, state.phase());
        } finally { release.countDown(); }
        finished(controller);
        assertEquals(CorpusImportController.Phase.COMPLETE, edt(controller::state).phase());
        assertEquals(11, edt(controller::state).summary().corpusTotal()); assertEquals(temp.toString(), refreshed.get());
    }

    @Test void stopAndShutdownCooperateWithoutInterruptingBackgroundIo() throws Exception {
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        var backend = new FakeBackend() {
            public LichessImporter.Summary run(LichessImporter.Options options, Consumer<LichessImporter.Progress> progress, BooleanSupplier stop) throws Exception {
                super.run(options, progress, stop); entered.countDown();
                assertTrue(release.await(10, TimeUnit.SECONDS)); assertTrue(stop.getAsBoolean());
                assertFalse(Thread.currentThread().isInterrupted());
                return new LichessImporter.Summary(STATS, 11, 2, true);
            }
        };
        var controller = controller(backend, root -> {});
        Path input = archive();
        edt(() -> controller.start(new CorpusImportController.Request(input.toString(), temp.toString(), true, 1)));
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            edt(controller::stop); assertEquals(CorpusImportController.Phase.STOPPING, edt(controller::state).phase());
            edt(() -> assertThrows(IllegalStateException.class, () -> controller.validate(temp.toString())));
            var join = edt(controller::beginShutdown); release.countDown(); join.run();
        } finally { release.countDown(); }
        finished(controller); assertEquals(CorpusImportController.Phase.STOPPED, edt(controller::state).phase());
    }

    @Test void validationUsesBackgroundBackendAndReportsErrors() throws Exception {
        var backend = new FakeBackend(); var refreshed = new AtomicReference<String>();
        var controller = controller(backend, refreshed::set);
        edt(() -> controller.validate(temp.toString())); finished(controller);
        assertEquals(CorpusImportController.Phase.VALID, edt(controller::state).phase());
        assertEquals(11, edt(controller::state).integrity().positions()); assertEquals(1, backend.validations.get());
        assertEquals(temp.toString(), refreshed.get());
        edt(() -> controller.validate("")); finished(controller);
        assertEquals(CorpusImportController.Phase.FAILED, edt(controller::state).phase());
        assertTrue(edt(controller::state).message().contains("Select a Seed corpus root"));
        var real = controller(new CorpusImportController.ProductionBackend(), root -> {});
        edt(() -> real.validate(temp.toString())); finished(real);
        assertTrue(edt(real::state).message().contains("Missing corpus catalog"));
    }

    @Test void backendFailureRetainsProgressAndRefreshesCheckpointStatus() throws Exception {
        var refreshed = new AtomicReference<String>();
        var backend = new FakeBackend() {
            public LichessImporter.Summary run(LichessImporter.Options options, Consumer<LichessImporter.Progress> progress, BooleanSupplier stop) throws Exception {
                super.run(options, progress, stop); throw new IOException("Corpus already has a writer");
            }
        };
        var controller = controller(backend, refreshed::set);
        Path input = archive();
        edt(() -> controller.start(new CorpusImportController.Request(input.toString(), temp.toString(), true, 1)));
        finished(controller); var state = edt(controller::state);
        assertEquals(CorpusImportController.Phase.FAILED, state.phase()); assertEquals(STATS, state.progress().stats());
        assertTrue(state.message().contains("Corpus already has a writer")); assertEquals(temp.toString(), refreshed.get());
    }

    @Test void sourceAndSharedRootAndAmountSurvivePreferenceReloadWithoutChoosers() throws Exception {
        var prefs = Preferences.userRoot().node("seedv6-import-test-" + UUID.randomUUID());
        try {
            var folders = new TrainingFolders(prefs);
            var panel = edt(() -> new CorpusImportPanel(folders, path -> {}));
            String input = archive().toString();
            edt(() -> {
                named(panel, "corpusImportArchive", JTextField.class).setText(input);
                panel.chooseRoot(temp.toString());
                named(panel, "corpusImportBounded", JRadioButton.class).doClick();
                named(panel, "corpusImportLimit", JSpinner.class).setValue(321L);
            });
            prefs.flush(); var reloaded = new TrainingFolders(Preferences.userRoot().node(prefs.absolutePath()));
            var next = edt(() -> new CorpusImportPanel(reloaded, path -> {}));
            edt(() -> {
                assertEquals(input, named(next, "corpusImportArchive", JTextField.class).getText());
                assertEquals(temp.toString(), named(next, "corpusImportRoot", JTextField.class).getText());
                assertTrue(named(next, "corpusImportBounded", JRadioButton.class).isSelected());
                assertEquals(321L, named(next, "corpusImportLimit", JSpinner.class).getValue());
            });
            var source = edt(() -> new BrnTrainingSourcePanel(settings(), reloaded, () -> {}));
            edt(() -> { source.load(settings()); assertEquals(temp.toString(), source.corpusRoot()); });
            var own = settings().withSource(TrainingSource.corpus(temp.resolve("own")))
                    .withCorpus(temp.resolve("own").toString(), new CorpusTrainingConfig(17, "a".repeat(64)));
            edt(() -> { source.load(own); assertEquals(own.corpusRoot(), source.corpusRoot()); assertEquals(own.corpusTraining(), source.readCorpusConfig()); return null; });
            edt(() -> named(next, "corpusImportAll", JRadioButton.class).doClick()); prefs.flush();
            assertTrue(new TrainingFolders(prefs).corpusImportAll());
            assertEquals(321L, new TrainingFolders(prefs).corpusImportLimit());
        } finally { prefs.removeNode(); }
    }

    @Test void realBackendExpandsCorpusAndRefreshesTrainingCountWithoutChangingPin() throws Exception {
        Path input = archive(), root = Files.createDirectory(temp.resolve("corpus"));
        var folders = new TrainingFolders(settings());
        var config = new CorpusTrainingConfig(7, "a".repeat(64));
        var selected = settings().withSource(TrainingSource.corpus(root)).withCorpus(root.toString(), config);
        var source = edt(() -> new BrnTrainingSourcePanel(selected, folders, () -> {}));
        edt(() -> source.load(selected));
        var panel = edt(() -> new CorpusImportPanel(folders, source::corpusChanged));
        edt(() -> {
            panel.chooseRoot(root.toString()); named(panel, "corpusImportArchive", JTextField.class).setText(input.toString());
            named(panel, "corpusImportBounded", JRadioButton.class).doClick(); named(panel, "corpusImportLimit", JSpinner.class).setValue(1L);
            named(panel, "startCorpusImport", JButton.class).doClick();
            assertFalse(named(panel, "corpusImportRoot", JTextField.class).isEnabled());
            assertTrue(named(panel, "stopCorpusImport", JButton.class).isEnabled());
        });
        until(() -> edt(() -> named(panel, "corpusImportStatus", JTextArea.class).getText().startsWith("Import complete.")));
        until(() -> edt(() -> named(source, "brnCorpusStatus", JLabel.class).getText().startsWith("Valid corpus: 1 positions.")));
        edt(() -> { named(panel, "corpusImportAll", JRadioButton.class).doClick(); named(panel, "startCorpusImport", JButton.class).doClick(); });
        until(() -> edt(() -> named(panel, "corpusImportStatus", JTextArea.class).getText().startsWith("Import complete.")));
        until(() -> edt(() -> named(source, "brnCorpusStatus", JLabel.class).getText().startsWith("Valid corpus: 2 positions.")));
        edt(() -> {
            String result = named(panel, "corpusImportStatus", JTextArea.class).getText();
            assertTrue(result.contains("Source records examined: 3")); assertTrue(result.contains("Duplicates: 1"));
            assertTrue(result.contains("Rejected: 1")); assertTrue(result.contains("Committed corpus total: 2"));
            assertEquals(config, source.readCorpusConfig()); assertEquals(root.toString(), folders.lastCorpusRoot());
            named(panel, "validateCorpus", JButton.class).doClick();
            return null;
        });
        until(() -> edt(() -> named(panel, "corpusImportStatus", JTextArea.class).getText().startsWith("Corpus integrity valid.")));
        try (var reader = new CorpusReader(root)) { assertEquals(2, reader.validate().positions()); }
    }

    @Test void managementEntryUsesSelectedTrainingRootAndPreservesGenerationSettings() throws Exception {
        Path input = archive(), root = Files.createDirectory(temp.resolve("entry-corpus"));
        LichessImporter.run(new LichessImporter.Options(input, root, 1, 10, 10), new PrintStream(OutputStream.nullOutputStream()));
        var selected = settings().withSource(TrainingSource.corpus(root)).withCorpus(root.toString(), new CorpusTrainingConfig(17));
        var panel = edt(() -> new TrainingPanel(selected));
        until(() -> edt(() -> named(panel, "brnCorpusStatus", JLabel.class).getText().startsWith("Valid corpus:")));
        edt(() -> {
            named(panel, "manageSeedCorpus", JButton.class).doClick();
            var tabs = named(panel, "trainingViews", JTabbedPane.class);
            assertEquals("Corpus", tabs.getTitleAt(tabs.getSelectedIndex()));
            assertEquals(root.toString(), named(panel, "corpusImportRoot", JTextField.class).getText());
            assertEquals(17, named(panel, "brnCorpusPositions", JSpinner.class).getValue());
        });
        edt(panel::beginCorpusShutdown).run();
        edt(() -> assertFalse(named(panel, "startCorpusImport", JButton.class).isEnabled()));
    }

    @Test void boundedEditorRejectsFractionalAndOverflowAndKeepsExactLongCount() throws Exception {
        var backend = new FakeBackend(); Path input = archive();
        var panel = edt(() -> new CorpusImportPanel(new TrainingFolders(settings()), path -> {}, backend));
        edt(() -> {
            panel.chooseRoot(temp.toString()); named(panel, "corpusImportArchive", JTextField.class).setText(input.toString());
            named(panel, "corpusImportBounded", JRadioButton.class).doClick();
            var editor = ((JSpinner.DefaultEditor) named(panel, "corpusImportLimit", JSpinner.class).getEditor()).getTextField();
            for (String invalid : List.of("1.5", "0", "-1", "9223372036854775808")) {
                editor.setText(invalid); named(panel, "startCorpusImport", JButton.class).doClick();
                assertTrue(named(panel, "corpusImportStatus", JTextArea.class).getText().startsWith("Bounded import count"));
                assertEquals(0, backend.imports.get());
            }
            editor.setText("9007199254740993"); named(panel, "startCorpusImport", JButton.class).doClick();
        });
        until(() -> edt(() -> named(panel, "corpusImportStatus", JTextArea.class).getText().startsWith("Import complete.")));
        assertEquals(9007199254740993L, backend.options.maxRecords());
    }

    @Test void actualWriterConflictAndCorruptCorpusUseBackendDiagnostics() throws Exception {
        Path input = archive(), root = Files.createDirectory(temp.resolve("locked"));
        var controller = controller(new CorpusImportController.ProductionBackend(), path -> {});
        try (var writer = new CorpusWriter(root, 10, new CorpusWriter.SourceInfo("test", "lock", "{}", "test"))) {
            edt(() -> controller.start(new CorpusImportController.Request(input.toString(), root.toString(), true, 1)));
            finished(controller);
            assertEquals(CorpusImportController.Phase.FAILED, edt(controller::state).phase());
            assertTrue(edt(controller::state).message().contains("Corpus already has a writer"));
        }
        LichessImporter.run(new LichessImporter.Options(input, root, 1, 10, 10), new PrintStream(OutputStream.nullOutputStream()));
        Path shard;
        try (var reader = new CorpusReader(root)) { shard = root.resolve("shards").resolve(reader.shards().getFirst().file()); }
        byte[] bytes = Files.readAllBytes(shard); bytes[bytes.length - 1] ^= 1; Files.write(shard, bytes);
        edt(() -> controller.validate(root.toString())); finished(controller);
        assertEquals(CorpusImportController.Phase.FAILED, edt(controller::state).phase());
        assertTrue(edt(controller::state).message().contains("Shard checksum mismatch"));
    }
}
