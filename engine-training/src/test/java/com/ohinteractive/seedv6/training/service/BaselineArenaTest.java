package com.ohinteractive.seedv6.training.service;

import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.search.driver.BaselineEngine;
import static com.ohinteractive.seedv6.training.service.LearningArenaConfig.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(90)
class BaselineArenaTest {
    @TempDir Path temporary;
    @Test void threeEvaluatorsPersistAndPlayRepeatedColorPairsWithoutTrainingStores() throws Exception {
        for (int workers : new int[]{1, 4, 16}) for (var evaluator : BaselineEngine.Evaluator.values()) {
            var a = Competitor.competitive(evaluator);
            var b = Competitor.competitive(BaselineEngine.Evaluator.CONVENTIONAL_CP64);
            var config = LearningArenaConfig.match("baseline lifecycle", a, b, 19,
                    new Arena(4, Limit.DEPTH, 2, 1000, workers, 0, 0, 4, Board.FEN_STARTING_POSITION));
            Path path = temporary.resolve(evaluator.name() + "-w" + workers);
            var snapshots = new java.util.concurrent.ConcurrentLinkedQueue<com.ohinteractive.seedv6.training.telemetry.ActiveGameSnapshot>();
            try (var service = LearningArenaService.create(path, config, update -> {
                if (update.liveGame() != null && update.liveGame().evaluation() != null) snapshots.add(update.liveGame());
            })) {
                service.run();
                assertEquals(LearningArenaState.Status.COMPLETE, service.state().status());
                assertEquals(2, service.state().current().pairs().size());
                assertEquals(a.evaluator(), service.state().current().a().evaluator());
                assertEquals(Evaluator.CONVENTIONAL_CP64, service.state().current().b().evaluator());
                assertFalse(snapshots.isEmpty(), "Actual move telemetry is required");
                for (var game : snapshots) {
                    assertEquals(game.gameInPair() == 1 ? evaluator : BaselineEngine.Evaluator.CONVENTIONAL_CP64, game.white().competitiveEvaluator());
                    assertEquals(game.gameInPair() == 1 ? BaselineEngine.Evaluator.CONVENTIONAL_CP64 : evaluator, game.black().competitiveEvaluator());
                    for (var participant : java.util.List.of(game.white(), game.black())) {
                        long flags = participant.competitiveEvaluator() == BaselineEngine.Evaluator.CONVENTIONAL_CP64 ? 3407871 : 262143;
                        assertTrue(participant.searchProfile().contains("flags=" + flags));
                    }
                    assertTrue(game.white().searchProfile().contains("aspiration=ADAPTIVE16"));
                    assertTrue(game.black().searchProfile().contains("aspiration=ADAPTIVE16"));
                    assertTrue(game.black().searchProfile().contains("workers=" + workers + ";hashMiB=64"));
                }
            }
            assertFalse(Files.exists(path.resolve("A/checkpoints")));
            assertFalse(Files.exists(path.resolve("B/checkpoints")));
            try (var resumed = LearningArenaService.resume(path, ignored -> {})) {
                assertEquals(config.identity(), resumed.state().binding());
                resumed.run(); assertEquals(LearningArenaState.Status.COMPLETE, resumed.state().status());
            }
            assertThrows(IllegalArgumentException.class, () -> new LearningArenaConfig("no training", a, b,
                    null, 1, 1, 1, 19, config.arena(), Mode.LEARNING));
        }
    }
}
