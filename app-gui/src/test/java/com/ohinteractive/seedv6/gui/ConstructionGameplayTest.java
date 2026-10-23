package com.ohinteractive.seedv6.gui;

import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.exact.FlatSearchConfig;
import com.ohinteractive.seedv6.search.manage.*;
import com.ohinteractive.seedv6.training.service.LearningArenaConfig;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(60)
class ConstructionGameplayTest {
    @TempDir Path temporary;
    @Test void retainedHistoricalProfilesKeepIndependentOwnersAcrossThreadChanges() throws Exception {
        var white = edt(() -> new PlayEnginePanel("white", temporary, null, () -> {}));
        var black = edt(() -> new PlayEnginePanel("black", temporary, null, () -> {}));
        var adapter = edt(() -> new EngineSearchAdapter(1));
        try {
            edt(() -> { white.loadFixed(LearningArenaConfig.Competitor.pinned(com.ohinteractive.seedv6.search.driver.PinnedSearch.Policy.CONSTRUCTION_V1));
                black.loadFixed(LearningArenaConfig.Competitor.pinned(com.ohinteractive.seedv6.search.driver.PinnedSearch.Policy.POLICY4)); return null; });
            assertTrue(edt(white::validSelection)); assertTrue(edt(black::validSelection));
            assertEquals(LearningArenaConfig.Evaluator.CONSTRUCTION_V1, edt(white::fixedCompetitor).evaluator());
            var selection = edt(() -> white.versus(black));
            assertEquals(PlayEvaluator.Mode.CONSTRUCTION_V1, selection.whiteMode());
            var loaded = new CompletableFuture<String>();
            edt(() -> { adapter.changeParticipants(PlayEvaluator.Mode.BEST_NNUE, temporary, selection, loaded::complete); return null; });
            assertNull(loaded.get(20, TimeUnit.SECONDS));
            assertNotSame(adapter.participants().white(), adapter.participants().black());
            assertNotSame(field(adapter, "service"), field(adapter, "blackService"));
            for (int workers : new int[]{1, 4, 16}) {
                edt(() -> { adapter.replaceWorkerCount(workers); return null; });
                var search = field(field(field(adapter, "service"), "search"), "exact");
                Object owner = workers == 1 ? field(search, "exact") : search;
                assertEquals(262143L, ((FlatSearchConfig) field(owner, "construction")).flags());
                var production = field(field(field(adapter, "blackService"), "search"), "exact");
                assertNull(field(workers == 1 ? field(production, "exact") : production, "construction"));
                assertNull(search(adapter, 3, false).failure());
                edt(() -> { adapter.invalidate(SearchTermination.NEW_GAME); return null; });
            }
            assertNull(search(adapter, 100, true).failure());
            assertNull(search(adapter, 2, false).failure());
            assertFalse(edt(() -> white.evaluator.isEnabled()));
            assertFalse(edt(() -> black.evaluator.isEnabled()));
            var card = edt(EngineCard::new);
            edt(() -> { card.showSearch(new GameController.SearchInfo("Idle", 3, "cp 2", 1234, 1, "e2e4", "DEPTH"),
                    adapter.participants().white(), 16); return null; });
            assertTrue(adapter.participants().white().identity().contains("model.sha256=5ab54a"));
        } finally {
            edt(() -> { white.dispose(); black.dispose(); return null; });
            edt(adapter::beginShutdown).run();
        }
    }
    @Test void humanBindingAndPersistedArenaSelectionKeepTheExplicitProfile() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> new PlayEvaluator(PlayEvaluator.Mode.CONSTRUCTION_V1, "", "",
                com.ohinteractive.seedv6.search.evaluation.SearchEvaluation.handcrafted()));
        var participants = PlayParticipants.load(PlayEvaluator.Mode.CONSTRUCTION_V1, null, PlayParticipants.Selection.BEST);
        assertEquals(PlayEvaluator.Mode.CONSTRUCTION_V1, participants.white().mode());
        assertTrue(participants.white().description().contains("experimental"));
        var panel = edt(() -> new PlayEnginePanel("arenaFixedA", temporary, null, () -> {}));
        try {
            edt(() -> { panel.loadFixed(LearningArenaConfig.Competitor.pinned(com.ohinteractive.seedv6.search.driver.PinnedSearch.Policy.CONSTRUCTION_V1)); return null; });
            assertEquals(PlayEvaluator.Mode.CONSTRUCTION_V1, edt(panel::selectedMode));
            assertFalse(edt(() -> panel.evaluator.isEnabled()));
            assertEquals(3, edt(() -> panel.evaluator.getItemCount()));
            assertTrue(edt(() -> ((javax.swing.JLabel) field(panel, "description")).getText()).startsWith("Saved historical configuration:"));
            edt(() -> { panel.clearFixed(); return null; });
            assertEquals(PlayEvaluator.Mode.CONVENTIONAL_CP64, edt(panel::selectedMode));
            assertTrue(edt(() -> panel.evaluator.isEnabled()));
            edt(() -> { panel.loadFixed(LearningArenaConfig.Competitor.competitive(com.ohinteractive.seedv6.search.driver.BaselineEngine.Evaluator.CONVENTIONAL_CP64));
                panel.clearFixed(); panel.evaluator.setSelectedItem(PlayEvaluator.Mode.BREP2_32); return null; });
            assertEquals("BREP2-32", edt(() -> ((javax.swing.JLabel) field(panel, "description")).getText()));
        } finally { edt(() -> { panel.dispose(); return null; }); }
    }
    private static ManagedSearchResult search(EngineSearchAdapter adapter, int depth, boolean stop) throws Exception {
        var result = new CompletableFuture<ManagedSearchResult>(); var board = Board.startingPosition();
        edt(() -> { adapter.start(board, GameHistory.initial(board), new SearchLimits(depth, -1, -1, false), new Object(), new SearchGateway.Listener() {
            public void onIteration(Object token, IterationSnapshot iteration) {}
            public void onComplete(Object token, ManagedSearchResult value) { result.complete(value); }
        }); if (stop) adapter.stop(); return null; });
        var value = result.get(20, TimeUnit.SECONDS);
        if (!stop) assertTrue(value.hasMove());
        return value;
    }
    private static Object field(Object object, String name) throws Exception {
        var field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object);
    }
    private static <T> T edt(Callable<T> call) throws Exception {
        var result = new AtomicReference<T>(); var failure = new AtomicReference<Throwable>();
        SwingUtilities.invokeAndWait(() -> { try { result.set(call.call()); } catch (Throwable t) { failure.set(t); } });
        if (failure.get() != null) throw new AssertionError(failure.get()); return result.get();
    }
}
