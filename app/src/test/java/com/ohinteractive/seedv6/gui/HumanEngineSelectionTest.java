package com.ohinteractive.seedv6.gui;

import java.nio.file.*;
import java.util.*;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn.BrnTrainer;
import com.ohinteractive.seedv6.core.brn1.Brn1Trainer;
import com.ohinteractive.seedv6.core.brn2.Brn2Trainer;
import com.ohinteractive.seedv6.core.brn3.Brn3Trainer;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.model.NetworkTrainingState;
import com.ohinteractive.seedv6.training.nnue.*;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static com.ohinteractive.seedv6.gui.PlayStoreWorkflowTest.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class HumanEngineSelectionTest {
    @TempDir Path temp;
    private EngineSearchAdapter adapter;
    private GameController game;
    private View view;

    @AfterEach void close() throws Exception { if (game != null) edt(game::beginShutdown).run(); }

    private void open() throws Exception {
        adapter = edt(() -> new EngineSearchAdapter(1));
        view = new View(); game = edt(() -> new GameController(adapter, view));
        edt(() -> {
            game.initialize();
            game.setSearchSettings(new GameController.SearchSettings(GameController.LimitKind.DEPTH, 1, 1000));
        });
    }

    @ParameterizedTest @EnumSource(NetworkArchitecture.class)
    void everyPlayableArchitectureUsesTheSameCatalogAndExplicitCheckpoint(NetworkArchitecture architecture) throws Exception {
        Path root = temp.resolve(architecture.name());
        String selected = initializeAndPublish(root, architecture);
        var side = edt(() -> new PlayEnginePanel("white", root, null, () -> {}));
        var opponent = edt(() -> new PlayEnginePanel("opponent", "Engine Opponent", root, null, () -> {}));
        try {
            until(() -> edt(() -> side.validSelection() && opponent.validSelection()));
            var expected = new ArrayList<String>(); expected.add("");
            CheckpointStore.availableCheckpoints(root).checkpoints().forEach(m -> expected.add(m.id()));
            assertEquals(expected, edt(() -> choices(side)));
            assertEquals(expected, edt(() -> choices(opponent)));
            assertEquals(selected, expected.get(1), "Newest explicit generation precedes Gen 0");
            edt(() -> opponent.generation.setSelectedItem(new PlayEvaluator.Choice(selected)));
            var selection = edt(() -> PlayParticipants.Selection.singleEngine(opponent.selectedRoot(), opponent.selectedId()));
            var reference = PlayParticipants.load(PlayEvaluator.Mode.BEST_NNUE, root, selection);
            open();
            edt(() -> game.startGame(PlayEvaluator.Mode.BEST_NNUE, selection));
            until(() -> !view.changing); assertNull(view.error);
            var pinned = adapter.participants();
            for (var binding : List.of(pinned.white(), pinned.black())) {
                assertEquals(architecture.trainingArchitecture(), binding.architecture());
                assertEquals(selected, binding.checkpointId());
                assertEquals(reference.white().networkHash(), binding.networkHash());
                assertTrue(binding.description().startsWith(architecture.toString()));
            }
            assertFalse(edt(adapter::isSearching), "White human still makes the first move");
            edt(() -> {
                opponent.generation.setSelectedItem(PlayEvaluator.Choice.BEST);
                opponent.selectStore(temp.resolve("another-lineage"));
                game.setCheckpointRoot(temp.resolve("training-is-independent"));
            });
            assertSame(pinned, adapter.participants(), "Setup edits cannot replace the running participant");
            edt(game::newGame); until(() -> !view.changing);
            assertEquals(selected, adapter.participants().black().checkpointId(), "A plain restart retains the explicit store/identity");
            assertNull(view.error);
        } finally { edt(() -> { side.dispose(); opponent.dispose(); }); }
    }

    @Test void lineageChangesBestResolutionAndHumanColourUsePinnedParticipantsUntilNewGame() throws Exception {
        Path first = temp.resolve("lineage-a"), second = temp.resolve("lineage-b");
        String best = brnStore(first); brnStore(second);
        String explicit = publish(second, 3, false, true).candidateId();
        open();
        edt(() -> game.startGame(PlayEvaluator.Mode.BEST_NNUE, PlayParticipants.Selection.singleEngine(first, "")));
        until(() -> !view.changing); assertEquals(best, adapter.evaluator().checkpointId());
        var pinned = adapter.participants();
        String promoted = publish(first, 12, true, true).candidateId();
        assertSame(pinned, adapter.participants());
        edt(() -> {
            game.setHumanSide(GameController.HumanSide.BLACK);
            assertSame(pinned, adapter.participants());
        });
        until(() -> edt(() -> game.displayedMoves().size() == 1 && !adapter.isBusy()));
        assertSame(pinned, adapter.participants()); assertNull(view.error);
        edt(() -> {
            game.setHumanSide(GameController.HumanSide.WHITE);
            game.startGame(PlayEvaluator.Mode.BEST_NNUE, PlayParticipants.Selection.singleEngine(second, explicit));
        });
        until(() -> !view.changing);
        assertEquals(explicit, adapter.evaluator().checkpointId());
        assertEquals(second, adapter.participants().selection().blackRoot());
        assertArrayEquals(Board.startingPosition(), edt(game::boardSnapshot));
        edt(() -> game.startGame(PlayEvaluator.Mode.BEST_NNUE, PlayParticipants.Selection.singleEngine(first, "")));
        until(() -> !view.changing);
        assertEquals(promoted, adapter.evaluator().checkpointId(), "Best is resolved when New Game loads, not at discovery");
        assertNull(view.error);
    }

    @Test void disappearedCheckpointFailsThroughTheSameLoaderAndRefreshNeverFallsBack() throws Exception {
        Path root = temp.resolve("lineage"); brnStore(root);
        String selected = publish(root, 3, false, true).candidateId();
        var opponent = edt(() -> new PlayEnginePanel("opponent", "Engine Opponent", root, null, () -> {}));
        try {
            until(() -> edt(opponent::validSelection));
            edt(() -> opponent.generation.setSelectedItem(new PlayEvaluator.Choice(selected)));
            var selection = PlayParticipants.Selection.singleEngine(root, selected);
            open();
            edt(() -> game.startGame(PlayEvaluator.Mode.BEST_NNUE, selection));
            until(() -> !view.changing); assertNull(view.error);
            var pinned = adapter.participants();
            edt(() -> game.loadFen(SHORT_GAME));
            long[] board = edt(game::boardSnapshot);
            Files.delete(root.resolve("checkpoints").resolve(selected).resolve(pinned.black().architecture().networkFile()));
            var referenceFailure = assertThrows(java.io.IOException.class,
                    () -> PlayParticipants.load(PlayEvaluator.Mode.BEST_NNUE, root, selection));
            edt(() -> game.startGame(PlayEvaluator.Mode.BEST_NNUE, selection));
            until(() -> !view.changing);
            assertTrue(view.error.contains(referenceFailure.getMessage()));
            assertTrue(view.error.contains("Previous game and networks are unchanged; game is paused"));
            assertSame(pinned, adapter.participants()); assertArrayEquals(board, edt(game::boardSnapshot));
            assertFalse(edt(adapter::isSearching));
            edt(() -> opponent.refresh(true));
            until(() -> edt(() -> choices(opponent).size() == 2));
            assertEquals(selected, edt(opponent::selectedId));
            assertFalse(edt(opponent::validSelection), "Refresh keeps the missing explicit selection instead of Best");
            view.error = null;
            edt(() -> game.startGame(PlayEvaluator.Mode.HANDCRAFTED, PlayParticipants.Selection.BEST));
            until(() -> !view.changing);
            assertEquals(PlayEvaluator.Mode.HANDCRAFTED, adapter.evaluator().mode());
            assertArrayEquals(Board.startingPosition(), edt(game::boardSnapshot)); assertNull(view.error);
        } finally { edt(opponent::dispose); }
    }

    @Test void handcraftedRequiresNoStoreAndModeSwitchToHumanRetainsAnExistingBrnBinding() throws Exception {
        open(); Path missing = temp.resolve("missing");
        edt(() -> game.startGame(PlayEvaluator.Mode.HANDCRAFTED, PlayParticipants.Selection.singleEngine(missing, "missing")));
        assertEquals(PlayEvaluator.Mode.HANDCRAFTED, adapter.evaluator().mode());
        assertFalse(Files.exists(missing)); assertNull(view.error);
        Path root = temp.resolve("brn"); brnStore(root);
        edt(() -> {
            game.setGameMode(GameController.GameMode.ENGINE_VS_ENGINE);
            game.startEngineGame(PlayParticipants.Selection.singleEngine(root, ""));
        });
        until(() -> !view.changing);
        edt(() -> {
            var pinned = adapter.participants();
            game.setGameMode(GameController.GameMode.HUMAN_VS_ENGINE);
            assertFalse(view.changing, "Switching mode must not attempt the old NNUE-only fallback");
            assertSame(pinned, adapter.participants());
        });
        assertNull(view.error);
    }

    private static List<String> choices(PlayEnginePanel panel) {
        var ids = new ArrayList<String>();
        for (int i = 0; i < panel.generation.getItemCount(); i++) ids.add(panel.generation.getItemAt(i).checkpointId());
        return ids;
    }

    private static String initializeAndPublish(Path root, NetworkArchitecture architecture) throws Exception {
        NetworkTrainingState state = switch (architecture) {
            case NNUE_MATERIAL -> new NetworkTrainingState.NnueMaterial(NnueTrainer.materialParity(TrainableNnue.initialized(1)));
            case NNUE -> new NetworkTrainingState.Nnue(new NnueTrainer(TrainableNnue.initialized(1)));
            case BRN -> new NetworkTrainingState.Brn(new BrnTrainer(.001));
            case BRN1 -> new NetworkTrainingState.Brn1(new Brn1Trainer(.001));
            case BRN2 -> new NetworkTrainingState.Brn2(new Brn2Trainer(.001));
            case BRN3 -> new NetworkTrainingState.Brn3(new Brn3Trainer(1));
        };
        try (var store = new CheckpointStore(root, architecture.trainingArchitecture())) {
            String best = store.initialize(state, new CheckpointManifest.Metadata(0, 1, "")).manifest().id();
            return store.publish(state, new CheckpointManifest.Metadata(7, 1, best)).manifest().id();
        }
    }
}
