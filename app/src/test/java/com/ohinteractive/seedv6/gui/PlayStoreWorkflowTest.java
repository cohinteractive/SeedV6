package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn2.Brn2Trainer;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.selfplay.GameTermination;
import com.ohinteractive.seedv6.training.validation.*;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(180)
class PlayStoreWorkflowTest {
    @TempDir Path temp;
    static String brnStore(Path root) throws Exception {
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN2)) {
            return store.initialize(new NetworkTrainingState.Brn2(new Brn2Trainer(.001)),
                    new CheckpointManifest.Metadata(0, 1, "")).manifest().id();
        }
    }
    static ValidationRecord publish(Path root, long generation, boolean promote, boolean complete) throws Exception {
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN2)) {
            var refs = store.recover(); var parent = refs.latestTraining().orElseThrow().manifest();
            var best = refs.best().orElseThrow().manifest(); var state = store.resumeState(parent.id());
            var candidate = store.publish(state, new CheckpointManifest.Metadata(generation, 1, parent.id()));
            var config = new ValidationConfig(2, 42, 0, 0, 1, 1, TrainingSettings.SCORE_MAPPING, 8);
            var white = new ValidationResult.Game(promote ? GameTermination.WHITE_CHECKMATES_BLACK : GameTermination.FIFTY_MOVE_RULE, 2);
            var black = new ValidationResult.Game(promote ? GameTermination.BLACK_CHECKMATES_WHITE : GameTermination.FIFTY_MOVE_RULE, 2);
            long[] board = Board.startingPosition();
            var result = new ValidationResult(config, ValidationArena.stateHash(board, GameHistory.initial(board)),
                    Collections.nCopies(2, new ValidationResult.Pair("fixture", white, black)));
            var record = store.recordValidation(candidate.manifest().id(), best.id(), result, new PromotionPolicy(1, .9, 0));
            if (complete) CandidateLifecycle.completeDecision(store, record);
            return record;
        }
    }
    @Test void independentArchitecturesGenerationsRetentionAndAtomicStartPinning() throws Exception {
        Path nnue = temp.resolve("NNUE A"), brn = temp.resolve("BRN2 D");
        String a = bootstrap(nnue), b = brnStore(brn);
        String old = publish(brn, 1, false, true).candidateId();
        var callbacks = new ConcurrentLinkedQueue<Runnable>();
        var adapter = edt(() -> new EngineSearchAdapter(1, callbacks::add, com.ohinteractive.seedv6.search.manage.SearchLifecycleService::new));
        var view = new View(); var game = edt(() -> new GameController(adapter, view));
        try {
            edt(() -> {
                game.initialize(); game.setSearchSettings(new GameController.SearchSettings(GameController.LimitKind.DEPTH, 1, 1000));
                game.setGameMode(GameController.GameMode.ENGINE_VS_ENGINE);
                assertFalse(adapter.isSearching()); assertFalse(view.running); assertTrue(game.displayedMoves().isEmpty());
                game.startEngineGame(new PlayParticipants.Selection("", old, nnue, brn));
            });
            until(() -> !view.changing); assertNull(view.error);
            var pinned = adapter.participants();
            assertEquals(a, pinned.white().checkpointId()); assertEquals(old, pinned.black().checkpointId());
            assertEquals(TrainingArchitecture.NNUE, pinned.white().architecture());
            assertEquals(TrainingArchitecture.BRN2, pinned.black().architecture());
            assertNotSame(pinned.white().evaluation(), pinned.black().evaluation());
            var promoted = publish(brn, 120, true, true);
            assertEquals(promoted.candidateId(), PlayEvaluator.loadBest(brn).checkpointId());
            assertFalse(CheckpointStore.availableCheckpoints(brn).checkpoints().stream().anyMatch(m -> m.id().equals(old)));
            assertTrue(Files.exists(brn.resolve("checkpoints").resolve(old).resolve(CheckpointManifest.MANIFEST_FILE)));
            assertSame(pinned, adapter.participants()); assertEquals(old, pinned.black().checkpointId());
            edt(() -> game.setCheckpointRoot(temp.resolve("other-training-store")));
            assertSame(pinned, adapter.participants());
            var whiteState = pinned.white().evaluation().newState(4); var blackState = pinned.black().evaluation().newState(4);
            long[] board = Board.startingPosition(), otherBoard = Board.fromFen("7k/6Q1/5K2/8/8/8/8/8 b - - 0 1");
            whiteState.initialize(board, 0); int expected = whiteState.evaluate(board, 0);
            blackState.initialize(otherBoard, 0); blackState.evaluate(otherBoard, 0); assertEquals(expected, whiteState.evaluate(board, 0));
            // Both Best defaults on one store get distinct evaluator definitions and independent state.
            var same = PlayParticipants.load(PlayEvaluator.Mode.BEST_NNUE, brn,
                    new PlayParticipants.Selection("", "", brn, brn));
            assertEquals(promoted.candidateId(), same.white().checkpointId());
            assertNotSame(same.white(), same.black()); assertNotSame(same.white().evaluation(), same.black().evaluation());
            assertNotSame(same.white().evaluation().newState(4), same.black().evaluation().newState(4));
            assertThrows(java.io.IOException.class, () -> PlayParticipants.load(PlayEvaluator.Mode.BEST_NNUE, brn, PlayParticipants.Selection.BEST),
                    "The legacy Human vs Engine Best NNUE choice remains NNUE-only");
            long[] before = edt(game::boardSnapshot);
            edt(() -> game.startEngineGame(new PlayParticipants.Selection("", old, nnue, brn)));
            until(() -> !view.changing); assertNotNull(view.error); assertTrue(view.error.contains("Black network"));
            assertSame(pinned, adapter.participants()); assertArrayEquals(before, edt(game::boardSnapshot));
            assertFalse(edt(adapter::isSearching));
            edt(() -> game.startEngineGame(new PlayParticipants.Selection("", "", temp.resolve("missing"), brn)));
            until(() -> !view.changing); assertTrue(view.error.contains("White network")); assertSame(pinned, adapter.participants());
        } finally { edt(game::beginShutdown).run(); }
        var panel = edt(() -> new PlayEnginePanel("white", brn, null, () -> {}));
        try {
            until(() -> edt(panel::validSelection)); assertEquals("", edt(panel::selectedId));
            edt(() -> {
                for (int i = 0; i < panel.generation.getItemCount(); i++) assertNotEquals(old, panel.generation.getItemAt(i).checkpointId());
                panel.generation.getModel().setSelectedItem(new PlayEvaluator.Choice(old)); assertFalse(panel.validSelection());
                panel.selectStore(nnue);
            });
            until(() -> edt(panel::validSelection)); assertEquals("", edt(panel::selectedId));
            assertEquals(2, edt(() -> panel.generation.getItemCount()));
            edt(() -> panel.selectStore(temp.resolve("gone"))); until(() -> edt(() -> !panel.error().isEmpty()));
            assertFalse(edt(panel::validSelection));
        } finally { edt(panel::dispose); }
    }
    @Test void oneNativeWindowVerifiesDashboardAndExplicitPerSideSetup() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Path a = temp.resolve("BRN2 A"), b = temp.resolve("BRN2 B");
        brnStore(a); brnStore(b); String generation = publish(b, 3, false, true).candidateId();
        var settings = TrainingDashboardTest.settings(temp.resolve("training"));
        var handle = new TrainingController.Handle() {
            boolean stopped;
            public void start() {}
            public void stop() { stopped = true; }
            public boolean terminated() { return stopped; }
            public void close() { stopped = true; }
            public com.ohinteractive.seedv6.training.service.TrainerSnapshot snapshot() { return WorkflowRefinementGuiTest.liveView().snapshot(); }
        };
        var backend = new TrainingController.Backend() {
            @Override TrainingController.Inspection inspect(TrainingSettings s) { return new TrainingController.Inspection(true, "", s.depth(), ""); }
            @Override TrainingController.Handle create(TrainingSettings s, boolean resume, com.ohinteractive.seedv6.training.service.TrainerConfig.DepthChange change) { return handle; }
        };
        ChessFrame frame = edt(() -> new ChessFrame(settings, backend, ignored -> {}));
        try {
            edt(() -> {
                frame.setSize(1440, 950); frame.setVisible(Boolean.getBoolean("seedv6.visibleWorkflowSmoke"));
                named(frame, "workspaces", JTabbedPane.class).setSelectedIndex(1);
                named(frame, "startTraining", JButton.class).doClick();
            });
            until(() -> edt(() -> named(frame, "trainingState", JLabel.class).getText().equals("VALIDATING")));
            edt(() -> {
                frame.validate(); var dashboard = named(frame, "trainingDashboard", TrainingDashboard.class);
                var scroll = named(frame, "trainingDashboardScroll", JScrollPane.class);
                var graphs = named(frame, "runtimeGraphs", JPanel.class);
                int bottom = SwingUtilities.convertPoint(graphs, 0, graphs.getHeight(), dashboard).y;
                assertEquals(0, scroll.getViewport().getViewPosition().y);
                assertTrue(bottom <= scroll.getViewport().getHeight(), "Native graphs bottom=" + bottom + " viewport=" + scroll.getViewport().getHeight());
                WorkflowRefinementGuiTest.capture(frame.getRootPane(), "native-dashboard-1440x950.png");
                System.out.println("NATIVE_DASHBOARD frame=" + frame.getSize() + " viewport=" + scroll.getViewport().getSize() + " graphsBottom=" + bottom);
                named(frame, "workspaces", JTabbedPane.class).setSelectedIndex(0);
                named(frame, "gameMode", JComboBox.class).setSelectedItem(GameController.GameMode.ENGINE_VS_ENGINE);
                assertFalse(named(frame, "startGame", JButton.class).isEnabled());
                assertFalse(named(frame, "stopSearch", JButton.class).isEnabled());
                named(frame, "whiteCheckpointStore", JTextField.class).setText(a.toString());
                named(frame, "blackCheckpointStore", JTextField.class).setText(b.toString());
            });
            until(() -> edt(() -> named(frame, "startGame", JButton.class).isEnabled()));
            edt(() -> {
                assertEquals(PlayEvaluator.Choice.BEST, named(frame, "whiteNetwork", JComboBox.class).getSelectedItem());
                named(frame, "blackNetwork", JComboBox.class).setSelectedItem(new PlayEvaluator.Choice(generation));
                assertFalse(named(frame, "stopSearch", JButton.class).isEnabled(), "Changing setup must not start search");
                assertFalse(named(frame, "playEvaluator", JComboBox.class).isVisible(), "Store determines architecture");
                named(frame, "playDepth", JSpinner.class).setValue(1); named(frame, "playThreads", JSpinner.class).setValue(1);
                frame.validate();
                for (String name : new String[]{"whiteCheckpointStore", "blackCheckpointStore", "whiteNetwork", "blackNetwork"}) {
                    JComponent field = named(frame, name, JComponent.class);
                    assertTrue(field.getVisibleRect().contains(new Rectangle(0, 0, field.getWidth(), field.getHeight())), name + " clipped");
                    assertTrue(field.getHeight() > 0);
                }
                WorkflowRefinementGuiTest.capture(frame.getRootPane(), "native-play-setup.png");
                named(frame, "startGame", JButton.class).doClick();
            });
            until(() -> edt(() -> named(frame, "stopSearch", JButton.class).isEnabled()));
            edt(() -> {
                named(frame, "stopSearch", JButton.class).doClick();
                assertTrue(named(frame, "blackPlayerNetwork", JLabel.class).getText().contains("Gen 3"));
                assertTrue(named(frame, "whitePlayerNetwork", JLabel.class).getText().contains("Gen 0"));
                WorkflowRefinementGuiTest.capture(frame.getRootPane(), "native-play-started.png");
            });
        } finally {
            edt(() -> frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING)));
            until(() -> !edt(frame::isDisplayable));
        }
    }
}
