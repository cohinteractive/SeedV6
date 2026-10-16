package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.service.*;
import java.nio.file.*;
import java.nio.ByteBuffer;
import java.util.*;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;

class LineageRedesignTest {
    @TempDir Path temporary;
    /** Legacy fixture construction deliberately bypasses the new-lineage creation policy. */
    static TrainingLineages.Entry fixture(Path base, NetworkArchitecture architecture, String name) throws Exception {
        if (ArchitectureLibrary.describe(architecture.trainingArchitecture()).canCreate()) return TrainingLineages.create(base, architecture, name);
        UUID id = UUID.randomUUID(); Path root = base.resolve(architecture.folderName()).resolve(id.toString());
        var settings = new TrainingController.Backend().resolveSource(TrainingSettings.defaults(root, architecture));
        settings = settings.withLearningRate(TrainingRecipe.defaults(architecture.trainingArchitecture()).learningRate());
        try (var store = new CheckpointStore(root, architecture.trainingArchitecture())) {
            store.writeLineage(new TrainingLineage(id, name, architecture.trainingArchitecture(), java.time.Instant.now(), LineageConfiguration.encode(settings), "Legacy fixture"));
        }
        return new TrainingLineages.Entry(root, architecture, name);
    }
    @Test void stableIdentityRenameCreationEligibilityAndLegacyDiscovery() throws Exception {
        var entry = TrainingLineages.create(temporary, NetworkArchitecture.BRN_PAIR2, "l1");
        var selected = TrainingLineages.read(entry); var initial = selected.lineage();
        assertEquals(.01, selected.settings().recipe().learningRate());
        assertEquals(128, selected.settings().minibatch()); assertEquals(8, selected.settings().epochs());
        assertEquals(131072, selected.settings().corpusTraining().positionsPerGeneration()); assertEquals(1, selected.settings().seed());
        var renamed = TrainingLineages.read(TrainingLineages.rename(selected, "renamed"));
        assertEquals(initial.id(), renamed.lineage().id()); assertEquals(entry.root(), renamed.entry().root());
        assertEquals(initial.created(), renamed.lineage().created()); assertEquals(initial.architecture(), renamed.lineage().architecture());
        assertEquals(1, LineageRevision.read(entry.root()).size());
        for (var arch : List.of(NetworkArchitecture.BRN, NetworkArchitecture.BRN1, NetworkArchitecture.BRN2, NetworkArchitecture.NNUE)) {
            assertThrows(java.io.IOException.class, () -> TrainingLineages.create(temporary, arch, "new"));
            var legacy = fixture(temporary, arch, "old");
            assertEquals(legacy.root(), TrainingLineages.discover(temporary, arch, List.of()).getFirst().root());
            assertEquals(arch, TrainingLineages.read(legacy).settings().architecture());
        }
        assertTrue(ArchitectureLibrary.describe(TrainingArchitecture.BRN3).canCreate());
        var future = ArchitectureLibrary.entries().stream().filter(a -> a.name().equals("BRN-4")).findFirst().orElseThrow();
        assertNull(future.implementation()); assertFalse(future.canCreate());
    }
    @Test void versionFourSettingsRoundTripWithoutRetroactiveDefaultsAndVersionSixKeepsIndependentProtocol() throws Exception {
        var entry = TrainingLineages.create(temporary, NetworkArchitecture.BRN_PAIR2, "l1");
        var original = TrainingLineages.read(entry).settings().withLearningRate(.007);
        byte[] bytes = Base64.getDecoder().decode(LineageConfiguration.encode(original));
        bytes = Arrays.copyOf(bytes, bytes.length - 9); ByteBuffer.wrap(bytes).putInt(4);
        var legacy = LineageConfiguration.decode(Base64.getEncoder().encodeToString(bytes), entry.root(), entry.architecture());
        assertEquals(original, legacy); assertEquals(.007, legacy.recipe().learningRate());
        var generated = new TrainerConfig.SelfPlay(2, 1, 5, 0, 2, 3, 20, TrainingSettings.SCORE_MAPPING);
        var settings = original.withValidationMoveTime(250).withGenerationProtocol(generated);
        settings = settings.withLearningRate(.008).withCorpus(settings.corpusRoot(), new CorpusTrainingConfig(65536))
                .withValidationMethod(ValidationMethod.GAME_PAIRS).withTimeLimit(0);
        TrainingLineages.save(TrainingLineages.read(entry), settings);
        var restored = TrainingLineages.read(entry).settings(); assertEquals(settings, restored);
        assertEquals(generated, restored.config(TrainerConfig.DepthChange.EXPLICITLY_ALLOW).selfPlay());
        assertEquals(250, restored.config(TrainerConfig.DepthChange.EXPLICITLY_ALLOW).validation(1).moveMillis());
    }
    @Test void requestedWorkflowAndConditionalControlsPersistOnSelectedLineage() throws Exception {
        edt(SeedTheme::initialize);
        var entry = TrainingLineages.create(temporary, NetworkArchitecture.BRN_PAIR2, "l1");
        Path dataset = temporary.resolve("stockfish.jsonl"); Files.writeString(dataset, SourceReadersTest.line(1));
        var source = DataSource.register("Stockfish fixture", dataset, 1);
        new DataSources(1, List.of(source), false).save(DataSources.directory(entry.root()));
        var settings = TrainingLineages.read(entry).settings(); var folders = new TrainingFolders(settings); folders.base(temporary);
        var panel = edt(() -> new TrainingPanel(settings, folders));
        var started = new java.util.concurrent.atomic.AtomicReference<TrainingSettings>();
        var handle = new TrainingWorkspaceSmokeTest.PreviewHandle();
        var backend = new TrainingController.Backend() {
            @Override TrainingController.Handle create(TrainingSettings configured, boolean resume, TrainerConfig.DepthChange change) {
                started.set(configured); return handle;
            }
        };
        var controller = edt(() -> new TrainingController(settings, backend, ignored -> {}, panel::showState));
        JFrame frame = java.awt.GraphicsEnvironment.isHeadless() ? null : edt(() -> {
            var f = new JFrame("Lineage workflow fixture"); f.setContentPane(panel); f.setSize(1050, 850); f.setVisible(true); return f;
        });
        try {
            edt(() -> { panel.bind(controller); panel.loadInitialLineage(); });
            until(() -> edt(() -> !controller.state().loading() && named(panel, "trainingDataSourceTable", JTable.class).getRowCount() == 1));
            edt(() -> {
                var tabs = named(panel, "trainingViews", JTabbedPane.class);
                assertEquals(List.of("Session", "Lineage Settings", "Training Settings", "Validation Settings", "Dataset Library", "History", "Diagnostics"),
                        java.util.stream.IntStream.range(0, tabs.getTabCount()).mapToObj(tabs::getTitleAt).toList());
                assertNull(named(panel, "networkArchitecture", JComboBox.class));
                assertFalse(named(panel, "trainingDepth", JSpinner.class).isVisible());
                named(panel, "trainingValidationMethod", JComboBox.class).setSelectedItem(ValidationMethod.GAME_PAIRS);
                named(panel, "trainingPairs", JSpinner.class).setValue(128);
                named(panel, "trainingDepth", JSpinner.class).setValue(4);
                named(panel, "trainingThreads", JSpinner.class).setValue(Math.min(4, com.ohinteractive.seedv6.search.exact.SearchThreads.available()));
                named(panel, "brnCorpusPositions", JSpinner.class).setValue(65536);
                var mode = named(panel, "validationSearchLimit", JComboBox.class); mode.setSelectedItem("Time");
                assertFalse(named(panel, "trainingDepth", JSpinner.class).isVisible());
                assertTrue(named(panel, "validationSearchTime", JSpinner.class).isVisible());
                mode.setSelectedItem("Depth"); assertTrue(named(panel, "trainingDepth", JSpinner.class).isVisible());
                assertFalse(named(panel, "validationSearchTime", JSpinner.class).isVisible());
                named(panel, "trainingTermination", JComboBox.class).setSelectedItem(RunTermination.Kind.GENERATIONS);
                named(panel, "trainingGenerations", JSpinner.class).setValue(16L);
                assertTrue(panel.applySettings());
                assertTrue(named(panel, "startTraining", JButton.class).isEnabled());
                var current = controller.state().settings(); assertEquals(.01, current.recipeLearningRate());
                assertEquals(1, current.seed()); assertEquals(16, current.maximumGenerations());
                assertEquals(65536, current.corpusTraining().positionsPerGeneration());
                assertEquals(128, current.validationPairs()); assertEquals(0, current.validationMoveMillis());
                assertNotNull(named(panel, "startCorpusImport", JButton.class));
            });
            if (frame != null) edt(() -> {
                var tabs = named(panel, "trainingViews", JTabbedPane.class);
                for (int width : new int[]{650, 1050}) for (int tab : new int[]{0, 1, 2, 3, 4}) {
                    frame.setSize(width, 850); tabs.setSelectedIndex(tab); frame.validate();
                    var picture = new java.awt.image.BufferedImage(frame.getWidth(), frame.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
                    var graphics = picture.createGraphics(); frame.paint(graphics); graphics.dispose();
                    Path directory = Path.of("build/gui-smoke/lineage-workflow"); Files.createDirectories(directory);
                    javax.imageio.ImageIO.write(picture, "png", directory.resolve("view-" + tab + "-" + width + ".png").toFile());
                }
                tabs.setSelectedIndex(0); return null;
            });
            until(() -> edt(() -> !controller.state().loading()));
            edt(() -> named(panel, "startTraining", JButton.class).doClick());
            until(() -> started.get() != null);
            assertEquals(entry.root(), started.get().root()); assertEquals(16, started.get().maximumGenerations());
            assertEquals(4, started.get().depth()); assertEquals(128, started.get().validationPairs());
            assertEquals(65536, started.get().corpusTraining().positionsPerGeneration());
        } finally {
            edt(controller::beginShutdown).run(); edt(panel::beginCorpusShutdown).run();
            if (frame != null) edt(frame::dispose);
        }
        var restored = TrainingLineages.read(entry); assertEquals(16, restored.settings().maximumGenerations());
        assertEquals(1, DataSources.read(DataSources.directory(entry.root())).sources().getFirst().weight());
        assertEquals(128, restored.settings().validationPairs());
    }
}
