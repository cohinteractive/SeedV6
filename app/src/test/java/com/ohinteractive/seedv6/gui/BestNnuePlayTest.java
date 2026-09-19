package com.ohinteractive.seedv6.gui;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.nnue.NnueEvaluator;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.alphabeta.SelectiveSearchPolicy;
import com.ohinteractive.seedv6.search.manage.*;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;
import com.ohinteractive.seedv6.training.checkpoint.*;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(60)
class BestNnuePlayTest {
    @TempDir Path root;
    private EngineSearchAdapter adapter;
    @AfterEach void close() throws Exception { if (adapter != null) edt(adapter::beginShutdown).run(); }

    private void open() throws Exception { adapter = edt(() -> new EngineSearchAdapter(1)); }
    private String select(PlayEvaluator.Mode mode) throws Exception {
        CompletableFuture<String> result = new CompletableFuture<>();
        edt(() -> adapter.changeEvaluator(mode, root, result::complete));
        return result.get(20, TimeUnit.SECONDS);
    }
    private ManagedSearchResult search(String fen, int depth, long nodes) throws Exception {
        long[] board = Board.fromFen(fen);
        CompletableFuture<ManagedSearchResult> done = new CompletableFuture<>();
        edt(() -> adapter.start(board, GameHistory.initial(board), new SearchLimits(depth, nodes, -1, false), new Object(),
                new SearchGateway.Listener() {
                    public void onIteration(Object token, IterationSnapshot snapshot) { assertTrue(SwingUtilities.isEventDispatchThread()); }
                    public void onComplete(Object token, ManagedSearchResult result) { assertTrue(SwingUtilities.isEventDispatchThread()); done.complete(result); }
                }));
        return done.get(20, TimeUnit.SECONDS);
    }

    @Tag("slow-nnue")
    @Test void onlyPersistedBestEntersImmutableSafeFullWindowPlayAndHandcraftedIsDefault() throws Exception {
        assertSame(com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping.V1, TrainingSettings.SCORE_MAPPING);
        String best = bootstrap(root), latest = candidate(root, false);
        open(); assertEquals(PlayEvaluator.Mode.HANDCRAFTED, adapter.evaluator().mode());
        assertNull(select(PlayEvaluator.Mode.BEST_NNUE));
        var binding = adapter.evaluator(); assertEquals(best, binding.checkpointId()); assertNotEquals(latest, binding.checkpointId());
        assertFalse(binding.evaluation().usesAspiration());
        assertEquals(SelectiveSearchPolicy.only(SelectiveSearchPolicy.Heuristic.MATE_DISTANCE), binding.evaluation().selectiveSearchPolicy());
        long[] board = Board.startingPosition();
        var state = binding.evaluation().newState(2); state.initialize(board, 0);
        int pinnedScore = state.evaluate(board, 0);
        try (var store = new CheckpointStore(root)) {
            var persisted = store.load(best);
            assertEquals(persisted.manifest().networkSha256(), binding.networkHash());
            var oracle = new NnueEvaluator(persisted.network()); oracle.evaluate(board);
            assertEquals(TrainingSettings.SCORE_MAPPING.map(oracle.boundedValue()), pinnedScore);
            var mutable = store.resume(best);
            mutable.trainBatch(new long[][] {board}, new double[] {1}, 1);
            assertEquals(pinnedScore, state.evaluate(board, 0), "Mutable training changes cannot alias the play network");
        }
        assertNotEquals(Eval.evaluate(board), pinnedScore);
        var result = search(Board.FEN_STARTING_POSITION, 2, -1);
        assertNull(result.failure()); assertTrue(result.hasMove()); assertEquals(2, result.lastCompletedResult().depth());
        GameSession session = GameSession.startingPosition();
        for (long move : result.lastCompletedResult().principalVariation()) session.applyGeneratedMove(move);
        assertFalse(session.moveHistory().isEmpty());
        assertNull(select(PlayEvaluator.Mode.HANDCRAFTED));
        assertEquals(PlayEvaluator.Mode.HANDCRAFTED, adapter.evaluator().mode());
        assertTrue(search(Board.FEN_STARTING_POSITION, 1, -1).hasMove());
    }

    @Tag("slow-nnue")
    @Test void promotionsDoNotMutateCurrentGameAndNewGameReloadsBestWithFreshSearchState() throws Exception {
        String first = bootstrap(root);
        try (var trainingWriter = new CheckpointStore(root)) {
            open();
            var view = new View();
            GameController game = edt(() -> new GameController(adapter, view));
            edt(() -> { game.setCheckpointRoot(root); game.setGameMode(GameController.GameMode.HUMAN_VS_HUMAN); game.initialize();
                game.changeEvaluator(PlayEvaluator.Mode.BEST_NNUE, false); });
            until(() -> !view.changing);
            var pinned = adapter.evaluator(); assertEquals(first, pinned.checkpointId());
            var initialResult = search(Board.FEN_STARTING_POSITION, 1, -1);
            edt(() -> {
                game.setWorkerCount(2);
                assertSame(pinned, adapter.evaluator(), "Changing Play threads retains the game's network");
                game.setSearchSettings(new GameController.SearchSettings(GameController.LimitKind.DEPTH, 256, 1000));
                game.setGameMode(GameController.GameMode.HUMAN_VS_ENGINE);
                game.setHumanSide(GameController.HumanSide.BLACK);
            });
            until(() -> view.search.nodes() > 0);
            String promoted = candidate(trainingWriter, true);
            assertTrue(edt(adapter::isSearching), "Promotion must leave the active Play search running");
            assertSame(pinned, adapter.evaluator());
            edt(() -> game.changeEvaluator(PlayEvaluator.Mode.BEST_NNUE, false));
            assertSame(pinned, adapter.evaluator(), "Re-selecting the same mode cannot refresh the active game");
            edt(game::stopSearch); until(() -> edt(() -> !adapter.isBusy()));
            edt(() -> {
                game.setGameMode(GameController.GameMode.HUMAN_VS_HUMAN);
                game.setWorkerCount(1); // Cold TT for the score comparison, retaining the active game's network.
                assertSame(pinned, adapter.evaluator());
            });
            var sameGame = search(Board.FEN_STARTING_POSITION, 1, -1);
            assertEquals(initialResult.lastCompletedResult().score(), sameGame.lastCompletedResult().score());
            edt(game::newGame); until(() -> !view.changing);
            var refreshed = adapter.evaluator();
            assertNotSame(pinned, refreshed); assertNotSame(pinned.evaluation(), refreshed.evaluation());
            assertEquals(promoted, refreshed.checkpointId());
            var fresh = search(Board.FEN_STARTING_POSITION, 1, -1);
            EngineSearchAdapter second = edt(() -> new EngineSearchAdapter(1));
            EngineSearchAdapter previous = adapter;
            try {
                adapter = second; assertNull(select(PlayEvaluator.Mode.BEST_NNUE));
                var oracle = search(Board.FEN_STARTING_POSITION, 1, -1);
                assertEquals(oracle.lastCompletedResult(), fresh.lastCompletedResult(), "New best has a fresh TT, matching a cold independent service");
            } finally { edt(second::beginShutdown).run(); adapter = previous; }
            assertNull(view.error);
        }
    }

    @Test void checkmateStalemateAndRuleDrawsStayCorrectAndAllGuiDepthsReachNnueService() throws Exception {
        bootstrap(root); open(); assertNull(select(PlayEvaluator.Mode.BEST_NNUE));
        String[] fens = {"7k/6Q1/5K2/8/8/8/8/8 b - - 100 1", "7k/5Q2/6K1/8/8/8/8/8 b - - 0 1",
                "4k3/8/8/8/8/8/8/R3K3 w - - 100 1", "4k3/8/8/8/8/8/8/4K3 w - - 0 1"};
        for (int i = 0; i < fens.length; i++) {
            var result = search(fens[i], 4, -1);
            assertEquals(i == 0 ? -TranspositionScores.MATE_SCORE : 0, result.lastCompletedResult().score());
            assertEquals(i >= 2, result.hasMove());
        }
        for (int depth : new int[] {2, 4, 8, 12, 256}) {
            var limits = new GameController.SearchSettings(GameController.LimitKind.DEPTH, depth, 1000).limits();
            assertEquals(depth, limits.depth());
            // Rule draw is exact and cheap even at the maximum supported GUI depth.
            var result = search(fens[2], limits.depth(), -1);
            assertNull(result.failure()); assertEquals(depth, result.lastCompletedResult().depth());
        }
    }

    @Test void missingAndCorruptBestFailSafelyWithoutInventingNetworkOrChangingBoard() throws Exception {
        open();
        assertNotNull(select(PlayEvaluator.Mode.BEST_NNUE));
        assertEquals(PlayEvaluator.Mode.HANDCRAFTED, adapter.evaluator().mode());
        assertFalse(Files.exists(root.resolve("store.lock")));
        String best = bootstrap(root);
        Files.write(root.resolve("checkpoints").resolve(best).resolve(CheckpointManifest.NETWORK_FILE), new byte[] {0});
        var view = new View(); GameController game = edt(() -> new GameController(adapter, view));
        edt(() -> { game.initialize(); game.setCheckpointRoot(root); game.changeEvaluator(PlayEvaluator.Mode.BEST_NNUE, false); });
        until(() -> !view.changing);
        assertNotNull(view.error); assertTrue(view.error.contains("corrupt"));
        assertEquals(PlayEvaluator.Mode.HANDCRAFTED, adapter.evaluator().mode());
        assertArrayEquals(Board.startingPosition(), edt(game::boardSnapshot));
    }

    @Test void evaluatorChangeDuringSearchSuppressesStaleMovesPreservesBoardAndRetiresWorker() throws Exception {
        bootstrap(root); open(); var view = new View(); GameController game = edt(() -> new GameController(adapter, view));
        edt(() -> {
            game.initialize(); game.setCheckpointRoot(root);
            game.setSearchSettings(new GameController.SearchSettings(GameController.LimitKind.DEPTH, 256, 1000));
            game.setHumanSide(GameController.HumanSide.BLACK);
            assertTrue(adapter.isBusy());
            game.changeEvaluator(PlayEvaluator.Mode.BEST_NNUE, false);
            assertTrue(adapter.isBusy());
        });
        until(() -> !view.changing);
        // New evaluator may now search, but the retired handcrafted result must never make a move.
        edt(game::stopSearch);
        until(() -> edt(() -> !adapter.isBusy()));
        assertTrue(edt(game::displayedMoves).size() <= 1);
        assertEquals(PlayEvaluator.Mode.BEST_NNUE, adapter.evaluator().mode());
        assertNull(view.error);
    }

    @Test void invalidatedButStillExecutingSearchKeepsItsOwnAdapterBusy() throws Exception {
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        adapter = edt(() -> new EngineSearchAdapter(1, SwingUtilities::invokeLater, workers ->
                new SearchLifecycleService(TimeSource.SYSTEM, () -> new SingleDepthSearch() {
                    public int maxSupportedDepth() { return 256; }
                    public SearchResult search(SearchRequest request) {
                        entered.countDown();
                        try { assertTrue(release.await(10, TimeUnit.SECONDS)); }
                        catch (InterruptedException e) { throw new AssertionError(e); }
                        return new SearchResult(0, false, 0, request.depth(), 0, 0, true);
                    }
                })));
        var view = new View(); GameController game = edt(() -> new GameController(adapter, view));
        try {
            edt(() -> game.setHumanSide(GameController.HumanSide.BLACK));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            edt(() -> game.setGameMode(GameController.GameMode.HUMAN_VS_HUMAN));
            assertTrue(edt(adapter::isBusy));
        } finally { release.countDown(); }
        until(() -> edt(() -> !adapter.isBusy()));
    }

    @Test void resourceCloseFailureIsReportedEvenWhenTheMainSearchWorkerHasTerminated() throws Exception {
        adapter = edt(() -> new EngineSearchAdapter(1, SwingUtilities::invokeLater, workers ->
                new SearchLifecycleService(TimeSource.SYSTEM, () -> new SingleDepthSearch() {
                    public int maxSupportedDepth() { return 256; }
                    public SearchResult search(SearchRequest request) { throw new AssertionError("No search expected"); }
                    public void close() { throw new IllegalStateException("fixture root resource close failure"); }
                })));
        Runnable cleanup = edt(adapter::beginShutdown);
        try {
            var failure = assertThrows(IllegalStateException.class, cleanup::run);
            assertTrue(failure.getMessage().contains("root resource close failure"));
        } finally { adapter = null; } // The fixture owns no root resources; its lifecycle worker has exited.
    }
}
