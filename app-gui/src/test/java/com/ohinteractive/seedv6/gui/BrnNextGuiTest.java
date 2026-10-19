package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.service.*;
import com.ohinteractive.seedv6.core.Board;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class BrnNextGuiTest {
    @TempDir Path temporary;
    private DataSource source() throws Exception {
        Path file = temporary.resolve("source.jsonl");
        Files.writeString(file, "{\"fen\":\"4k3/8/8/8/3pP3/8/8/4K3 w - - 0 1\",\"evals\":[{\"depth\":20,\"pvs\":[{\"cp\":50}]}]}\n");
        return DataSource.register("tiny", file, 1);
    }
    @Test void waveIsIdempotentAndPreservesExistingModelsAndEditedSettings() throws Exception {
        Path library = temporary.resolve("library"); Files.createDirectories(library);
        var old = TrainingLineages.create(library, NetworkArchitecture.BRN_PAIR2, "Existing Pair-2");
        byte[] metadata = Files.readAllBytes(old.root().resolve(TrainingLineage.FILE));
        var source = source(); var prepared = BrnSuccessorWave.prepare(library, source);
        assertEquals(3, prepared.size());
        for (var entry : prepared) {
            var architecture = NetworkArchitecture.valueOf(entry.architecture()); var root = Path.of(entry.root());
            var selected = TrainingLineages.read(new TrainingLineages.Entry(root, architecture, entry.name()));
            assertEquals(new TrainingRecipe(.003, 128, 8), selected.settings().recipe());
            assertEquals(ValidationMethod.HELD_OUT, selected.settings().validationMethod());
            assertEquals(entry.initialCheckpoint(), entry.currentCheckpoint()); assertTrue(selected.seedLocked());
            assertEquals(1, ModelLibrary.discover(library, architecture.trainingArchitecture(), List.of()).size());
            var actor = PlayEvaluator.load(root, entry.currentCheckpoint());
            assertEquals(architecture.trainingArchitecture(), actor.architecture());
            assertEquals(0, actor.evaluation().newState(2).evaluate(Board.startingPosition(), 0));
            var bytes = CheckpointStore.readTrainingSnapshot(root, entry.currentCheckpoint()).encode();
            TrainingLineages.save(selected, selected.settings().withLearningRate(.002));
            var replay = BrnSuccessorWave.prepare(library, source).stream().filter(e -> e.lineageId().equals(entry.lineageId())).findFirst().orElseThrow();
            assertEquals(.002, replay.recipe().learningRate()); assertEquals(entry.currentCheckpoint(), replay.currentCheckpoint());
            assertArrayEquals(bytes, CheckpointStore.readTrainingSnapshot(root, entry.currentCheckpoint()).encode());
        }
        assertArrayEquals(metadata, Files.readAllBytes(old.root().resolve(TrainingLineage.FILE)));
        assertTrue(CheckpointInspection.freshRoot(old.root(), TrainingArchitecture.BRN_PAIR2));
    }

    @Test void nativeWindowShowsEachCandidateRecipeLineageAndPlayBinding() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Native Swing required");
        String ownerReport = System.getProperty("seedv6.waveGuiReport");
        Path library = temporary.resolve("native-library"); Files.createDirectories(library);
        var entries = ownerReport == null ? BrnSuccessorWave.prepare(library, source())
                : DataFiles.read(Path.of(ownerReport), BrnSuccessorWave.Report.class).entries();
        for (var entry : entries) {
            var architecture = NetworkArchitecture.valueOf(entry.architecture());
            var selection = TrainingLineages.read(new TrainingLineages.Entry(Path.of(entry.root()), architecture, entry.name()));
            var frame = edt(() -> new ChessFrame(selection.settings(), new TrainingController.Backend(), ignored -> {}));
            try {
                edt(() -> { frame.setSize(1440, 950); frame.setVisible(true);
                    named(frame, null, TrainingPanel.class).loadInitialLineage(); return null; });
                long loadingEnd = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
                while (!edt(() -> named(frame, "trainingLineage", JComboBox.class).getSelectedItem()
                        instanceof TrainingLineages.Entry selected && selected.root().equals(Path.of(entry.root())))) {
                    if (System.nanoTime() > loadingEnd) fail("Lineage did not load: " + edt(() -> {
                        var chooser = named(frame, "trainingLineage", JComboBox.class);
                        return "selected=" + chooser.getSelectedItem() + ", count=" + chooser.getItemCount()
                                + ", wanted=" + entry.root() + ", summary=" + named(frame, "trainingLineageSummary", JTextArea.class).getText()
                                + ", dialogs=" + Arrays.stream(Window.getWindows()).filter(Window::isShowing)
                                    .map(w -> find(w, null, JOptionPane.class)).filter(Objects::nonNull).map(JOptionPane::getMessage).toList();
                    })); Thread.sleep(50);
                }
                while (!edt(() -> named(frame, "startTraining", JButton.class).isEnabled())) {
                    if (System.nanoTime() > loadingEnd) fail("Ready lineage did not enable Start Training"); Thread.sleep(50);
                }
                edt(() -> {
                    named(frame, "workspaces", JTabbedPane.class).setSelectedIndex(1);
                    named(frame, "trainingViews", JTabbedPane.class).setSelectedIndex(1);
                    assertTrue(named(frame, "architectureLibrary", JTextArea.class).getText().contains(architecture.toString()));
                    assertEquals(128, ((Number) named(frame, "trainingMinibatch", JSpinner.class).getValue()).intValue());
                    assertEquals(8, ((Number) named(frame, "trainingEpochs", JSpinner.class).getValue()).intValue());
                    assertTrue(named(frame, "trainingLineageSummary", JTextArea.class).getText().contains(entry.name()));
                    frame.validate();
                    var info = named(frame, "brnNextTrainingInformation-" + architecture.name(), JTextArea.class);
                    assertTrue(info.isShowing());
                    assertTrue(info.modelToView2D(info.getDocument().getLength() - 1).getMaxY() <= info.getHeight(), "Candidate explanation must fit");
                    Path output = Path.of("build/brn-successor-native"); Files.createDirectories(output);
                    var image = new java.awt.image.BufferedImage(frame.getWidth(), frame.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
                    var graphics = image.createGraphics(); frame.paintAll(graphics); graphics.dispose();
                    javax.imageio.ImageIO.write(image, "png", output.resolve(entry.candidate() + (ownerReport == null ? "-loaded.png" : "-owner.png")).toFile());
                    return null;
                });
            } finally {
                edt(() -> { frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING)); return null; });
                long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
                while (edt(frame::isDisplayable)) { if (System.nanoTime() > end) fail("Window did not close"); Thread.sleep(50); }
            }
        }
    }
    static <T extends Component> T named(Container root, String name, Class<T> type) {
        for (Component child : root.getComponents()) {
            if ((name == null || name.equals(child.getName())) && type.isInstance(child)) return type.cast(child);
            if (child instanceof Container c) { T result = find(c, name, type); if (result != null) return result; }
        }
        throw new AssertionError("Missing control " + name);
    }
    private static <T extends Component> T find(Container root, String name, Class<T> type) {
        try { return named(root, name, type); } catch (AssertionError missing) { return null; }
    }
    private static <T> T edt(Callable<T> call) throws Exception {
        var result = new AtomicReference<T>(); var failure = new AtomicReference<Throwable>();
        SwingUtilities.invokeAndWait(() -> { try { result.set(call.call()); } catch (Throwable t) { failure.set(t); } });
        if (failure.get() != null) throw new AssertionError(failure.get()); return result.get();
    }
}
