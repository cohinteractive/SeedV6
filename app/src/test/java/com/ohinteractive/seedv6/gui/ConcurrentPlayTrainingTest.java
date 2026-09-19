package com.ohinteractive.seedv6.gui;

import java.nio.file.Path;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.manage.SearchLifecycleService;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

/** Controller isolation with a deterministic training handle and real asynchronous Play searches. */
@Timeout(60)
class ConcurrentPlayTrainingTest {
    @TempDir Path root;
    private EngineSearchAdapter play;
    private TrainingController training;

    @AfterEach void close() throws Exception {
        Runnable stopTraining = training == null ? () -> {} : edt(training::beginShutdown);
        Runnable stopPlay = play == null ? () -> {} : edt(play::beginShutdown);
        try { stopTraining.run(); } finally { stopPlay.run(); }
    }

    private TrainingControllerTest.DelayedBackend trainer() throws Exception {
        var backend = new TrainingControllerTest.DelayedBackend();
        backend.releaseInspect.countDown();
        training = edt(() -> new TrainingController(settings(root, 1, 0), backend, ignored -> {}, ignored -> {}));
        return backend;
    }

    private TrainingController.ViewState poll() throws Exception {
        return edt(() -> { training.poll(); return training.state(); });
    }

    @Test void bothStartOrdersStopsResetsSettingsAndTrainingFailureAreIndependent() throws Exception {
        play = edt(() -> new EngineSearchAdapter(2));
        var view = new View();
        var game = edt(() -> new GameController(play, view));
        var backend = trainer();
        edt(() -> {
            game.initialize();
            game.setSearchSettings(new GameController.SearchSettings(GameController.LimitKind.DEPTH, 256, 1000));
            training.start();
        });
        until(() -> poll().snapshot() != null);
        var trainingRun = poll().snapshot();
        edt(() -> {
            game.setGameMode(GameController.GameMode.HUMAN_VS_HUMAN);
            game.squarePressed(12); game.squareReleased(28); // Human e2-e4 remains available during Training.
            assertEquals(1, game.displayedMoves().size());
            game.newGame();
            game.setGameMode(GameController.GameMode.HUMAN_VS_ENGINE);
            game.setHumanSide(GameController.HumanSide.BLACK);
        });
        until(() -> view.search.nodes() > 0);
        assertTrue(poll().active()); assertFalse(backend.handle.stopping); assertFalse(backend.handle.closed);
        assertEquals(trainingRun.generation(), poll().snapshot().generation());
        edt(game::newGame);
        assertTrue(edt(play::isSearching)); assertFalse(backend.handle.stopping);
        edt(game::stopSearch);
        until(() -> edt(() -> !play.isBusy()));
        assertTrue(poll().active()); assertFalse(backend.handle.stopping);
        edt(() -> game.setWorkerCount(3));
        assertEquals(1, poll().settings().threads()); assertEquals(3, edt(play::workerCount));

        edt(game::newGame);
        until(() -> view.search.nodes() > 0);
        edt(training::stop);
        assertTrue(backend.handle.stopping); assertTrue(edt(play::isSearching));
        backend.handle.terminated = true;
        assertFalse(poll().active()); assertTrue(edt(play::isSearching));
        edt(() -> training.setSettings(new TrainingSettings(root, 1, 2, 2, 0, 0, 2, 2, 1, 2, 71, 8, 0)));
        assertEquals(3, edt(play::workerCount)); assertEquals(2, poll().settings().threads());

        // Start Training while Play is already searching. A fresh fake handle avoids reusing a stopped run.
        edt(training::beginShutdown).run();
        backend = trainer();
        edt(training::start);
        until(() -> poll().snapshot() != null);
        assertTrue(edt(play::isSearching));
        backend.handle.failed = true; backend.handle.terminated = true;
        assertEquals(TrainingController.Phase.FAILED, poll().phase());
        assertTrue(edt(play::isSearching)); assertNull(view.error);
    }

    @Test void playSearchFailureAndDisposalLeaveTrainingAlive() throws Exception {
        var backend = trainer();
        edt(training::start); until(() -> poll().snapshot() != null);
        play = edt(() -> new EngineSearchAdapter(1, SwingUtilities::invokeLater, threads ->
                new SearchLifecycleService(TimeSource.SYSTEM, () -> new SingleDepthSearch() {
                    public int maxSupportedDepth() { return 256; }
                    public SearchResult search(SearchRequest request) { throw new IllegalStateException("fixture Play failure"); }
                })));
        var view = new View(); var game = edt(() -> new GameController(play, view));
        edt(() -> game.setHumanSide(GameController.HumanSide.BLACK));
        until(() -> view.error != null);
        assertTrue(view.error.contains("fixture Play failure"));
        edt(game::beginShutdown).run();
        assertTrue(poll().active()); assertFalse(backend.handle.stopping); assertFalse(backend.handle.closed);
    }
}
