package com.ohinteractive.seedv6.gui;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.search.alphabeta.RootParallelSearch;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.manage.SearchLifecycleService;
import com.ohinteractive.seedv6.training.checkpoint.*;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class PerSideNnuePlayTest {
    @TempDir Path root;
    private EngineSearchAdapter adapter;
    @AfterEach void close() throws Exception { if (adapter != null) edt(adapter::beginShutdown).run(); }

    @Test void gameCreationAndAlternatingSearchesUseIndependentFixedParticipantEvaluators() throws Exception {
        String best = bootstrap(root), other = candidate(root, false);
        var uses = new CopyOnWriteArrayList<Use>();
        var callbacks = new ConcurrentLinkedQueue<Runnable>();
        adapter = edt(() -> new EngineSearchAdapter(1, callbacks::add, new EngineSearchAdapter.LifecycleFactory() {
            public SearchLifecycleService create(int workers) { return new SearchLifecycleService(workers); }
            public SearchLifecycleService create(int workers, PlayEvaluator binding) {
                return new SearchLifecycleService(TimeSource.SYSTEM, () -> new SingleDepthSearch() {
                    final RootParallelSearch search = new RootParallelSearch(workers, binding.evaluation());
                    public int maxSupportedDepth() { return search.maxSupportedDepth(); }
                    public SearchResult search(SearchRequest request) {
                        long[] board = new long[Board.MAX_BITBOARDS]; request.copyBoardInto(board);
                        uses.add(new Use(Board.player((int) board[Board.STATUS]), binding));
                        return search.search(request);
                    }
                    public void close() { search.close(); }
                });
            }
        }));
        var view = new View();
        var game = edt(() -> new GameController(adapter, view));
        var selection = new PlayParticipants.Selection(other, best);
        edt(() -> {
            game.setCheckpointRoot(root);
            game.setGameMode(GameController.GameMode.HUMAN_VS_HUMAN);
            game.loadFen("7k/6Q1/5K2/8/8/8/8/8 b - - 0 1");
            game.setSearchSettings(new GameController.SearchSettings(GameController.LimitKind.DEPTH, 1, 1000));
            game.setGameMode(GameController.GameMode.ENGINE_VS_ENGINE);
            game.changeEvaluator(PlayEvaluator.Mode.BEST_NNUE, false);
        });
        until(() -> !view.changing);
        assertEquals(best, adapter.participants().white().checkpointId());
        assertSame(adapter.participants().white(), adapter.participants().black(), "Default self-play resolves one Best for both sides");
        var defaultBindings = adapter.participants();
        edt(() -> {
            game.setGameMode(GameController.GameMode.HUMAN_VS_HUMAN);
            assertFalse(view.changing, "Default mode switching retains existing pinned-Best semantics");
            assertSame(defaultBindings, adapter.participants());
            game.setGameMode(GameController.GameMode.ENGINE_VS_ENGINE);
        });
        edt(() -> game.setNetworkSelection(selection));
        assertEquals(selection, edt(game::networkSelection));
        edt(game::newGame); until(() -> !view.changing);
        var pinned = adapter.participants();
        assertEquals(selection, pinned.selection());
        assertEquals(other, pinned.white().checkpointId()); assertEquals(best, pinned.black().checkpointId());
        assertNotSame(pinned.white().evaluation(), pinned.black().evaluation());
        for (int turn = 0; turn < 6; turn++) {
            until(() -> callbacks.size() >= 2);
            var use = uses.get(turn);
            int side = turn % 2 == 0 ? Value.WHITE : Value.BLACK;
            assertEquals(side, use.side()); assertSame(pinned.forSide(side), use.binding());
            if (turn == 1) {
                edt(() -> game.setNetworkSelection(new PlayParticipants.Selection(best, other)));
                candidate(root, true);
                // Loaded immutable weights survive loss of their persisted files as well as promotion.
                Files.delete(root.resolve("checkpoints").resolve(other).resolve(CheckpointManifest.NETWORK_FILE));
                assertSame(pinned, adapter.participants());
            }
            var ready = new ArrayList<Runnable>();
            for (Runnable callback; (callback = callbacks.poll()) != null;) ready.add(callback);
            edt(() -> ready.forEach(Runnable::run));
        }
        assertEquals(6, edt(game::displayedMoves).size());
        assertSame(pinned, adapter.participants()); assertNull(view.error);
        long[] board = edt(game::boardSnapshot);
        edt(game::newGame); until(() -> !view.changing);
        assertTrue(view.error.contains("Black network"));
        assertSame(pinned, adapter.participants(), "Failure after loading White must restore the whole previous pair");
        assertArrayEquals(board, edt(game::boardSnapshot));
        edt(() -> { for (Runnable stale; (stale = callbacks.poll()) != null;) stale.run(); });
        assertArrayEquals(board, edt(game::boardSnapshot), "Callbacks from retired participants cannot change the paused game");
        edt(() -> game.setWorkerCount(2));
        assertSame(pinned, adapter.participants(), "Changing threads preserves both participants");
        String promoted = CheckpointStore.readBestSnapshot(root).manifest().id();
        edt(() -> { game.setNetworkSelection(PlayParticipants.Selection.BEST); game.newGame(); });
        until(() -> !view.changing);
        assertEquals(promoted, adapter.participants().white().checkpointId());
        assertSame(adapter.participants().white(), adapter.participants().black());
    }

    @Test void changingConfiguredStoreResetsVisibleChoicesTogetherAndDoesNotMutatePinnedParticipants() throws Exception {
        String best = bootstrap(root);
        adapter = edt(() -> new EngineSearchAdapter(1));
        class CatalogView extends View {
            volatile PlayParticipants.Selection selection;
            volatile CheckpointStore.AvailableCheckpoints available;
            public void showNetworks(CheckpointStore.AvailableCheckpoints value, PlayParticipants.Selection requested, String error) {
                available = value; selection = requested;
            }
        }
        var view = new CatalogView(); var game = edt(() -> new GameController(adapter, view));
        edt(() -> { game.setCheckpointRoot(root); game.refreshNetworks(); });
        until(() -> view.available != null && view.available.checkpoints().size() == 1);
        var pinned = adapter.participants();
        edt(() -> {
            game.setNetworkSelection(new PlayParticipants.Selection(best, best));
            game.setCheckpointRoot(root.resolve("different-store"));
            assertEquals(PlayParticipants.Selection.BEST, game.networkSelection());
            assertEquals(PlayParticipants.Selection.BEST, view.selection);
            assertTrue(view.available.checkpoints().isEmpty());
        });
        assertSame(pinned, adapter.participants());
    }

    @Test void defaultSameAndMixedBestSelectionsResolveOnceAndHandcraftedIgnoresNetworkChoices() throws Exception {
        String best = bootstrap(root), other = candidate(root, false);
        try (var writer = new CheckpointStore(root)) {
            var defaults = PlayParticipants.load(PlayEvaluator.Mode.BEST_NNUE, root, PlayParticipants.Selection.BEST);
            assertEquals(best, defaults.white().checkpointId()); assertSame(defaults.white(), defaults.black());
            var same = PlayParticipants.load(PlayEvaluator.Mode.BEST_NNUE, root, new PlayParticipants.Selection(other, other));
            assertEquals(other, same.white().checkpointId()); assertSame(same.white(), same.black());
            var mixed = PlayParticipants.load(PlayEvaluator.Mode.BEST_NNUE, root, new PlayParticipants.Selection("", other));
            assertEquals(best, mixed.white().checkpointId()); assertEquals(other, mixed.black().checkpointId());
            var handcrafted = PlayParticipants.load(PlayEvaluator.Mode.HANDCRAFTED, root.resolve("absent"),
                    new PlayParticipants.Selection(other, "missing"));
            assertEquals(PlayEvaluator.Mode.HANDCRAFTED, handcrafted.white().mode());
            assertSame(handcrafted.white(), handcrafted.black());
        }
    }

    @Test void humanPlayUsesBestAndFailedNewGameKeepsPreviousBoardAndBothBindingsWithoutFallback() throws Exception {
        String best = bootstrap(root), other = candidate(root, false);
        adapter = edt(() -> new EngineSearchAdapter(1));
        var view = new View(); var game = edt(() -> new GameController(adapter, view));
        edt(() -> {
            game.initialize(); game.setCheckpointRoot(root);
            game.setNetworkSelection(new PlayParticipants.Selection(other, other));
            game.changeEvaluator(PlayEvaluator.Mode.BEST_NNUE, false);
        });
        until(() -> !view.changing);
        assertEquals(best, adapter.participants().white().checkpointId());
        assertSame(adapter.participants().white(), adapter.participants().black());
        edt(game::newGame); until(() -> !view.changing);
        assertEquals(best, adapter.evaluator().checkpointId(), "Human vs Engine ignores self-play configuration");
        edt(() -> {
            game.setGameMode(GameController.GameMode.HUMAN_VS_HUMAN);
            game.loadFen("7k/6Q1/5K2/8/8/8/8/8 b - - 0 1");
            game.setGameMode(GameController.GameMode.ENGINE_VS_ENGINE);
        });
        var previous = adapter.participants(); long[] board = edt(game::boardSnapshot);
        Files.delete(root.resolve("checkpoints").resolve(other).resolve(CheckpointManifest.NETWORK_FILE));
        edt(() -> { game.setNetworkSelection(new PlayParticipants.Selection(best, other)); game.newGame(); });
        until(() -> !view.changing);
        assertNotNull(view.error); assertTrue(view.error.contains("Black network"));
        assertSame(previous, adapter.participants()); assertArrayEquals(board, edt(game::boardSnapshot));
        assertFalse(edt(adapter::isSearching));
        edt(() -> game.setNetworkSelection(new PlayParticipants.Selection(other, best)));
        edt(game::newGame); until(() -> !view.changing);
        assertTrue(view.error.contains("White network")); assertSame(previous, adapter.participants());
        edt(() -> game.changeEvaluator(PlayEvaluator.Mode.HANDCRAFTED, false)); until(() -> !view.changing);
        assertEquals(PlayEvaluator.Mode.HANDCRAFTED, adapter.participants().black().mode());
    }

    private record Use(int side, PlayEvaluator binding) {}
}
