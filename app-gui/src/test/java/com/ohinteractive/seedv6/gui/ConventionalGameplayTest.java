package com.ohinteractive.seedv6.gui;

import java.nio.file.Path;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.ConventionalNnueEngine;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.exact.*;
import com.ohinteractive.seedv6.search.manage.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.gui.ConventionalGuiProbe.*;

@Timeout(90)
class ConventionalGameplayTest {
    @TempDir Path temporary;

    @Test void selectorsPersistIndependentChoicesWithoutStoresAndKeepDefaults() throws Exception {
        var preferences = java.util.prefs.Preferences.userRoot().node("seedv6-cp64-test-" + java.util.UUID.randomUUID());
        var white = edt(() -> new PlayEnginePanel("white", temporary, preferences, () -> {}));
        var black = edt(() -> new PlayEnginePanel("black", temporary, null, () -> {}));
        try {
            assertEquals(PlayEvaluator.Mode.CONVENTIONAL_CP64, edt(white::selectedMode));
            edt(() -> { black.evaluator.setSelectedItem(PlayEvaluator.Mode.BREP2_32); return null; });
            assertEquals(3, edt(() -> white.evaluator.getItemCount()));
            edt(() -> { white.evaluator.setSelectedItem(PlayEvaluator.Mode.CONVENTIONAL_CP64); return null; });
            assertEquals("Conventional NNUE CP64", edt(white::selectionDescription));
            assertTrue(edt(white::validSelection)); assertNull(edt(white::selectedRoot));
            assertEquals(PlayEvaluator.Mode.CONVENTIONAL_CP64.name(), preferences.get("evaluator", ""));
            var restored = edt(() -> new PlayEnginePanel("white", temporary, preferences, () -> {}));
            try { assertEquals(PlayEvaluator.Mode.CONVENTIONAL_CP64, edt(restored::selectedMode)); }
            finally { edt(() -> { restored.dispose(); return null; }); }
            edt(() -> { white.swapWith(black); return null; });
            var selection = edt(() -> white.versus(black));
            assertEquals(PlayEvaluator.Mode.CONVENTIONAL_CP64, selection.blackMode());
            assertEquals(PlayEvaluator.Mode.BREP2_32, selection.whiteMode());
        } finally {
            edt(() -> { white.dispose(); black.dispose(); return null; }); preferences.removeNode();
        }
    }

    @Test void actualGuiServiceUsesQualifiedSearchAndFreshAccumulatorState() throws Exception {
        var adapter = edt(() -> new EngineSearchAdapter(1));
        try {
            var selection = new PlayParticipants.Selection("", "", null, null, null, null,
                    PlayEvaluator.Mode.CONVENTIONAL_CP64, PlayEvaluator.Mode.BREP2_32);
            assertNull(bind(adapter, PlayEvaluator.Mode.BEST_NNUE, selection));
            var binding = adapter.participants().white();
            assertTrue(binding.evaluation().isConventional());
            assertEquals("64", binding.checkpointId());
            assertEquals(ConventionalNnueEngine.SHA256, binding.networkHash());
            assertEquals("S6CNU002", binding.conventional().model().schema());
            assertEquals(8, binding.conventional().model().outputBuckets());
            assertEquals(1, adapter.workerCount()); assertEquals(4, binding.effectiveWorkers(4));
            assertNotSame(field(adapter, "service"), field(adapter, "blackService"));
            assertTrue(binding.identity(1).contains("flags=262143"));
            assertTrue(binding.identity(1).contains("aspiration=ADAPTIVE16"));
            for (String fen : new String[]{Board.FEN_STARTING_POSITION,
                    "r3k2r/pppq1ppp/2n1bn2/3pp3/3PP3/2N1BN2/PPPQ1PPP/R3K2R w KQkq - 0 1",
                    "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1", "7k/P7/8/8/8/8/8/7K w - - 0 1"}) {
                edt(() -> { adapter.invalidate(SearchTermination.NEW_GAME); return null; });
                var board = Board.fromFen(fen); var before = board.clone();
                var actual = search(adapter, board, new SearchLimits(5, -1, -1, false), false);
                assertNull(actual.failure()); assertTrue(actual.hasMove()); assertArrayEquals(before, board);
                assertTrue(java.util.Arrays.stream(GameSession.fromFen(fen).legalMoves()).anyMatch(m -> m == actual.bestMove()));
                try (var oracle = new SearchLifecycleService(TimeSource.SYSTEM,
                        () -> com.ohinteractive.seedv6.search.driver.BaselineEngine.create(SearchEvaluation.conventionalFullRebuild(binding.conventional().model()), 1, 64),
                        com.ohinteractive.seedv6.search.tablebase.RootTablebase.NONE, com.ohinteractive.seedv6.book.Book.bundled())) {
                    var result = new CompletableFuture<ManagedSearchResult>();
                    oracle.setBookEnabled(false);
                    oracle.start(board, GameHistory.initial(board), new SearchLimits(5, -1, -1, false), SearchObserver.NONE, result::complete);
                    var completed = result.get(20, TimeUnit.SECONDS);
                    assertNull(completed.failure(), fen + ": " + completed.failure());
                    assertNotNull(completed.lastCompletedResult(), fen + ": " + completed.termination());
                    var expected = completed.lastCompletedResult();
                    assertEquals(expected.score(), actual.lastCompletedResult().score(), fen);
                    assertEquals(expected.bestMove(), actual.bestMove(), fen);
                    assertEquals(expected.nodes(), actual.lastCompletedResult().nodes(), fen);
                    assertArrayEquals(expected.principalVariation(), actual.lastCompletedResult().principalVariation(), fen);
                }
            }
            assertNull(search(adapter, Board.startingPosition(), new SearchLimits(100, -1, -1, false), true).failure());
            var retired = (SearchLifecycleService) field(adapter, "service");
            assertNull(bind(adapter, PlayEvaluator.Mode.CONSTRUCTION_V1, PlayParticipants.Selection.BEST));
            assertTrue(retired.isTerminated());
            assertNull(bind(adapter, PlayEvaluator.Mode.CONVENTIONAL_CP64, PlayParticipants.Selection.BEST));
            edt(() -> { adapter.replaceWorkerCount(16); return null; });
            assertEquals(16, adapter.evaluator().effectiveWorkers(adapter.workerCount()));
            assertNull(search(adapter, Board.startingPosition(), new SearchLimits(100, -1, 150, false), false).failure());
            assertNull(search(adapter, Board.startingPosition(), new SearchLimits(3, -1, -1, false), false).failure());
        } finally { edt(adapter::beginShutdown).run(); }
        assertTrue(((SearchLifecycleService) field(adapter, "service")).isTerminated());
        assertTrue(((SearchLifecycleService) field(adapter, "blackService")).isTerminated());
    }
    private static Object field(Object object, String name) throws Exception {
        var field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object);
    }
}
