package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.ProductionSearch;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.manage.SearchLifecycleService;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.model.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class HceParticipantsTest {
    @TempDir Path temporary;
    static PlayParticipants.Selection selection(Path root, String id, boolean whiteHce, boolean blackHce) {
        return new PlayParticipants.Selection(id, id, root, root, null, null,
                whiteHce ? PlayEvaluator.Mode.HANDCRAFTED : PlayEvaluator.Mode.BEST_NNUE,
                blackHce ? PlayEvaluator.Mode.HANDCRAFTED : PlayEvaluator.Mode.BEST_NNUE);
    }
    String network(TrainingArchitecture architecture) throws Exception {
        try (var store = new CheckpointStore(temporary, architecture)) {
            var state = NetworkTrainingState.initialized(architecture, 1, .001);
            var best = store.initialize(state, new CheckpointManifest.Metadata(0, 1, ""));
            return store.publish(state, new CheckpointManifest.Metadata(3, 1, best.manifest().id())).manifest().id();
        }
    }
    @ParameterizedTest @EnumSource(TrainingArchitecture.class)
    void hceAndEveryNetworkArchitectureBindInBothOrientations(TrainingArchitecture architecture) throws Exception {
        String id = network(architecture);
        var selection = selection(temporary, id, true, false);
        assertNull(selection.whiteRoot()); assertEquals("", selection.whiteId());
        for (var requested : List.of(selection, selection.swapped())) {
            var players = PlayParticipants.load(PlayEvaluator.Mode.BEST_NNUE, temporary.resolve("unused"), requested);
            var hce = requested.whiteMode() == PlayEvaluator.Mode.HANDCRAFTED ? players.white() : players.black();
            var neural = hce == players.white() ? players.black() : players.white();
            assertSame(SearchEvaluation.handcrafted(), hce.evaluation()); assertNull(hce.architecture());
            assertEquals("", hce.checkpointId()); assertNull(hce.binding());
            assertEquals(id, neural.checkpointId()); assertEquals(architecture, neural.architecture());
            assertEquals(requested, players.selection());
        }
        var networks = PlayParticipants.load(PlayEvaluator.Mode.BEST_NNUE, temporary, selection(temporary, id, false, false));
        assertEquals(id, networks.white().checkpointId()); assertEquals(id, networks.black().checkpointId());
    }
    @Test void hceNeedsNoStoreAndMissingNetworkStillFailsOnItsOwnSide() throws Exception {
        var requested = selection(temporary.resolve("missing"), "invalid", true, true);
        var players = PlayParticipants.load(PlayEvaluator.Mode.BEST_NNUE, null, requested);
        assertEquals(requested, players.selection());
        assertEquals(PlayEvaluator.Mode.HANDCRAFTED, players.white().mode());
        assertEquals(PlayEvaluator.Mode.HANDCRAFTED, players.black().mode());
        assertNotSame(players.white().evaluation().newState(2), players.black().evaluation().newState(2));
        for (boolean whiteHce : new boolean[]{true, false}) {
            var error = assertThrows(java.io.IOException.class, () -> PlayParticipants.load(PlayEvaluator.Mode.BEST_NNUE, null,
                    selection(temporary.resolve("missing"), "invalid", whiteHce, !whiteHce)));
            assertTrue(error.getMessage().contains(whiteHce ? "Black network" : "White network"));
        }
    }
    @Test void selectorsDisableCheckpointsPreserveNetworkDraftsAndSwapHceWithoutAStore() throws Exception {
        String id = network(TrainingArchitecture.BRN2);
        var white = edt(() -> new PlayEnginePanel("white", temporary, null, () -> {}));
        var black = edt(() -> new PlayEnginePanel("black", temporary.resolve("missing"), null, () -> {}));
        try {
            until(() -> edt(white::validSelection));
            edt(() -> {
                white.generation.setSelectedItem(new ModelChoice(id));
                black.evaluator.setSelectedItem(PlayEvaluator.Mode.HANDCRAFTED);
                assertTrue(black.validSelection()); assertFalse(black.generation.isEnabled()); assertFalse(black.architecture.isEnabled());
                white.swapWith(black);
                assertTrue(white.handcrafted()); assertTrue(white.validSelection());
                assertFalse(white.generation.isEnabled()); assertEquals(id, black.selectedId()); assertTrue(black.validSelection());
                black.evaluator.setSelectedItem(PlayEvaluator.Mode.HANDCRAFTED);
                black.evaluator.setSelectedItem(PlayEvaluator.Mode.BEST_NNUE);
                assertEquals(id, black.selectedId()); assertTrue(black.generation.isEnabled());
            });
            until(() -> edt(() -> !white.architecture.isEnabled()));
            assertTrue(edt(white::validSelection), "Late network I/O cannot invalidate HCE");
        } finally { edt(() -> { white.dispose(); black.dispose(); }); }
    }
    @ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(ints = {0, 1, 2})
    void activeGameSearchesUsePinnedParticipantsAndHumanHceReplacesBothSides(int orientation) throws Exception {
        String id = network(TrainingArchitecture.BRN2);
        var callbacks = new ConcurrentLinkedQueue<Runnable>();
        record Use(int side, PlayEvaluator player) {}
        var uses = new CopyOnWriteArrayList<Use>();
        var adapter = edt(() -> new EngineSearchAdapter(1, callbacks::add, new EngineSearchAdapter.LifecycleFactory() {
            public SearchLifecycleService create(int workers) { return new SearchLifecycleService(workers); }
            public SearchLifecycleService create(int workers, PlayEvaluator binding) {
                return new SearchLifecycleService(TimeSource.SYSTEM, () -> new SingleDepthSearch() {
                    final SingleDepthSearch search = ProductionSearch.create(workers, binding.evaluation());
                    public int maxSupportedDepth() { return search.maxSupportedDepth(); }
                    public SearchResult search(SearchRequest request) {
                        long[] board = new long[Board.MAX_BITBOARDS]; request.copyBoardInto(board);
                        uses.add(new Use(Board.player((int) board[Board.STATUS]), binding)); return search.search(request);
                    }
                    public void close() { search.close(); }
                });
            }
        }));
        var view = new View(); var game = edt(() -> new GameController(adapter, view));
        try {
            edt(() -> {
                game.initialize(); game.setSearchSettings(new GameController.SearchSettings(GameController.LimitKind.DEPTH, 1, 1000));
                game.setGameMode(GameController.GameMode.ENGINE_VS_ENGINE);
                game.startEngineGame(selection(temporary, id, orientation != 1, orientation != 0));
            });
            until(() -> !view.changing); assertNull(view.error); var pinned = adapter.participants();
            edt(() -> game.setNetworkSelection(selection(temporary, id, false, true)));
            for (int i = 0; i < 4; i++) {
                until(() -> callbacks.size() >= 2);
                var use = uses.get(i); assertEquals(i % 2 == 0 ? Value.WHITE : Value.BLACK, use.side());
                assertSame(pinned.forSide(use.side()), use.player());
                var ready = new ArrayList<Runnable>(); for (Runnable r; (r = callbacks.poll()) != null;) ready.add(r);
                edt(() -> ready.forEach(Runnable::run));
            }
            assertSame(pinned, adapter.participants()); assertEquals(4, edt(game::displayedMoves).size());
            edt(game::newGame); until(() -> !view.changing);
            assertEquals(PlayEvaluator.Mode.HANDCRAFTED, adapter.participants().black().mode());
            edt(() -> game.startEngineGame(selection(temporary, id, true, false))); until(() -> !view.changing);
            edt(() -> { game.setGameMode(GameController.GameMode.HUMAN_VS_ENGINE); game.startGame(PlayEvaluator.Mode.HANDCRAFTED, PlayParticipants.Selection.BEST); });
            until(() -> !view.changing);
            assertEquals(PlayEvaluator.Mode.HANDCRAFTED, adapter.participants().black().mode()); assertNull(view.error);
        } finally { edt(game::beginShutdown).run(); }
    }
    @Test void engineStatusIdentifiesHceAndItsSearchingColour() throws Exception {
        edt(() -> {
            var card = new EngineCard();
            card.showSearch(new GameController.SearchInfo("Idle", 1, "cp 125", 10, 100, "e7e5", "DEPTH", 10, Value.BLACK), PlayEvaluator.handcrafted(), 1);
            assertEquals("-1.25", named(card, "engineScore", JLabel.class).getText());
            var identity = named(card, "searchTermination", JLabel.class);
            assertTrue(identity.getText().contains("Black · HCE")); assertEquals("Handcrafted evaluator", identity.getToolTipText());
        });
    }
}
