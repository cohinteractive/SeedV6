package com.ohinteractive.seedv6.training.selfplay;

import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.NnueEvaluator;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.core.nnue.NnueNetworkCodec;
import com.ohinteractive.seedv6.training.nnue.NnueTrainer;
import com.ohinteractive.seedv6.training.nnue.TrainableNnue;
import com.ohinteractive.seedv6.training.nnue.TrainingStateCodec;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.training.selfplay.SelfPlayRunnerTest.*;

@Timeout(30)
class SelfPlayTrainingTest {
    @Test
    void realSeededSelfPlayThroughAdamIsBitExactOnRepeatedSingleThreadRuns() throws Exception {
        var config = config(3, 2103, 1, 1, 8);
        var training = SelfPlayTraining.Config.fromBatchSeed(2, 4, config.seed());
        NnueTrainer first = trainer();
        String original = networkDigest(NETWORK);
        long start = System.nanoTime();
        SelfPlayTraining.Result result = SelfPlayTraining.run(first, config, training,
                Board.fromFen(FIFTY_SOON), new SelfPlayControl());
        double elapsed = (System.nanoTime() - start) / 1e9;
        SelfPlayBatch.Statistics games = result.generation().statistics();
        SelfPlayTraining.Statistics trained = result.training().orElseThrow();
        assertEquals(3, games.requestedGames());
        assertEquals(3, games.completedGames());
        assertEquals(0, games.abortedGames());
        assertEquals(0, games.whiteWins());
        assertEquals(3, games.draws());
        assertEquals(0, games.blackWins());
        assertEquals(6, games.totalPlayedPlies());
        assertEquals(6, games.rawTrajectoryPositions());
        assertEquals(6, games.sampledPositions());
        assertEquals(2, games.minimumCompletedPlies());
        assertEquals(2, games.maximumCompletedPlies());
        assertEquals(2.0, games.meanCompletedPlies());
        assertEquals(12, trained.samplesTrained());
        assertEquals(4, trained.optimizerUpdates());
        assertEquals(0, trained.initialOptimizerStep());
        assertEquals(4, trained.finalOptimizerStep());
        assertEquals(4, first.optimizer().step());
        assertTrue(trained.initialLoss() > 0, "The initialized network has nonzero draw error.");
        assertTrue(Double.isFinite(trained.initialLoss()));
        assertTrue(Double.isFinite(trained.finalLoss()));
        assertTrue(Double.isFinite(trained.meanTrainingLoss()));
        assertTrue(Double.isFinite(trained.predictionMean()));
        assertEquals(0.0, trained.targetMean());
        assertFalse(trained.cancelled());
        assertEquals(original, networkDigest(result.generation().generationNetwork()), "Actor A is frozen.");
        assertEquals(original, networkDigest(NETWORK));
        assertNotEquals(original, networkDigest(result.snapshot()), "Nonzero signal updates parameters.");
        assertEquals(networkDigest(first.model().snapshot()), networkDigest(result.snapshot()));
        assertSnapshotPredictions(first, result);

        NnueTrainer second = trainer();
        SelfPlayTraining.Result repeat = SelfPlayTraining.run(second, config, training,
                Board.fromFen(FIFTY_SOON), new SelfPlayControl());
        assertEquals(games, repeat.generation().statistics());
        assertEquals(result.generation().games(), repeat.generation().games());
        assertEquals(trained, repeat.training().orElseThrow());
        assertEquals(networkDigest(result.snapshot()), networkDigest(repeat.snapshot()));
        assertEquals(trainingDigest(first), trainingDigest(second), "Parameters, moments, step and hyperparameters match.");
        for (int i = 0; i < result.generation().samples().size(); i++) {
            var sample = result.generation().samples().get(i);
            assertEquals(0.0, sample.target());
            assertArrayEquals(sample.board(), repeat.generation().samples().get(i).board());
            long[] exposed = sample.board();
            exposed[0] = 0;
            assertArrayEquals(repeat.generation().samples().get(i).board(), sample.board());
        }
        System.out.println("E_REAL_DRAW_RUN elapsedSeconds=" + elapsed + " generation=" + games
                + " training=" + trained + " immutableAUnchanged=true snapshotBMatches=true");
    }

    @Test
    void genuineDecisiveSelfPlayAlsoSuppliesNonzeroTerminalSupervision() throws Exception {
        NnueTrainer trainer = trainer();
        String original = networkDigest(NETWORK);
        long start = System.nanoTime();
        var result = SelfPlayTraining.run(trainer, config(3, 17, 0, 0, 8),
                SelfPlayTraining.Config.fromBatchSeed(2, 2, 17),
                Board.fromFen("7k/6p1/5KQ1/8/8/8/8/8 w - - 0 1"), new SelfPlayControl());
        double elapsed = (System.nanoTime() - start) / 1e9;
        var games = result.generation().statistics();
        var training = result.training().orElseThrow();
        assertEquals(3, games.whiteWins());
        assertEquals(0, games.draws());
        assertEquals(0, games.blackWins());
        assertEquals(0, games.abortedGames());
        assertEquals(3, games.totalPlayedPlies());
        assertEquals(3, games.sampledPositions());
        assertEquals(4, training.optimizerUpdates());
        assertEquals(6, training.samplesTrained());
        assertEquals(1.0, training.targetMean());
        assertTrue(training.initialLoss() > 0);
        assertTrue(Double.isFinite(training.finalLoss()));
        assertEquals(original, networkDigest(result.generation().generationNetwork()));
        assertNotEquals(original, networkDigest(result.snapshot()));
        assertEquals(networkDigest(trainer.model().snapshot()), networkDigest(result.snapshot()));
        assertSnapshotPredictions(trainer, result);
        System.out.println("E_REAL_DECISIVE_RUN elapsedSeconds=" + elapsed + " generation=" + games
                + " training=" + training + " immutableAUnchanged=true snapshotBMatches=true");
    }

    @Test
    void generationCompletesBeforeTrainingAndExplicitPartialTrainingPreservesActor() throws Exception {
        NnueTrainer trainer = trainer();
        NnueNetwork actor = trainer.model().snapshot();
        String original = networkDigest(actor);
        SelfPlayControl cancelled = new SelfPlayControl();
        var batch = SelfPlayBatch.generate(actor, config(4, 21, 0, 0, 20), cancelled, index -> {
            assertEquals(0, trainer.optimizer().step());
            if (index == 0) return HeadlessGameTest.foolsMate().trajectory();
            HeadlessGame interrupted = new HeadlessGame(Board.startingPosition(), 20);
            interrupted.play(interrupted.legalMoves()[0]);
            cancelled.cancel();
            interrupted.abort(GameTermination.CANCELLED, null);
            return interrupted.trajectory();
        });
        assertEquals(0, trainer.optimizer().step());
        assertEquals(4, batch.samples().size());
        var config = new SelfPlayTraining.Config(2, 3, false, 99);
        assertTrue(SelfPlayTraining.train(trainer, batch, config, cancelled).isEmpty());
        assertEquals(0, trainer.optimizer().step());
        var trained = SelfPlayTraining.train(trainer, batch, config, new SelfPlayControl()).orElseThrow();
        assertEquals(4, trained.optimizerUpdates());
        assertEquals(8, trained.samplesTrained());
        assertEquals(0.0, trained.targetMean());
        assertTrue(Double.isFinite(trained.finalLoss()));
        assertEquals(original, networkDigest(actor));
        assertNotEquals(original, networkDigest(trainer.model().snapshot()));
    }

    @Test
    void cancellationBeforeGenerationAndEmptyCappedBatchNeverTrain() throws Exception {
        NnueTrainer trainer = trainer();
        String original = networkDigest(trainer.model().snapshot());
        SelfPlayControl control = new SelfPlayControl();
        control.cancel();
        var result = SelfPlayTraining.run(trainer, config(3, 2, 0, 0, 8),
                new SelfPlayTraining.Config(2, 2, true, 1), control);
        assertTrue(result.training().isEmpty());
        assertTrue(result.generation().cancelled());
        assertEquals(3, result.generation().unstartedGames());
        assertEquals(0, result.generation().statistics().abortedGames());
        assertEquals(0, trainer.optimizer().step());
        var capped = SelfPlayTraining.run(trainer, config(2, 2, 1, 1, 1),
                new SelfPlayTraining.Config(1, 2, true, 1), new SelfPlayControl());
        assertEquals(2, capped.generation().statistics().cappedGames());
        assertEquals(2, capped.generation().statistics().abortedGames());
        assertEquals(0, capped.generation().statistics().draws());
        assertTrue(capped.generation().samples().isEmpty());
        assertTrue(capped.training().isEmpty());
        assertEquals(0, trainer.optimizer().step());
        assertEquals(original, networkDigest(capped.snapshot()));
    }

    @Test
    void shuffledMixedTargetsAreDeterministicAndSeedCanChangeUpdateOrder() throws Exception {
        var batch = SelfPlayBatch.generate(NETWORK, config(1, 0, 0, 0, 20), new SelfPlayControl(),
                index -> HeadlessGameTest.foolsMate().trajectory());
        NnueTrainer first = trainer();
        NnueTrainer repeat = trainer();
        var config = new SelfPlayTraining.Config(2, 1, true, 43);
        assertEquals(SelfPlayTraining.train(first, batch, config, new SelfPlayControl()),
                SelfPlayTraining.train(repeat, batch, config, new SelfPlayControl()));
        assertEquals(trainingDigest(first), trainingDigest(repeat));
        NnueTrainer other = trainer();
        SelfPlayTraining.train(other, batch, new SelfPlayTraining.Config(2, 1, true, 99), new SelfPlayControl());
        assertNotEquals(networkDigest(first.model().snapshot()), networkDigest(other.model().snapshot()));
        assertEquals(8, first.optimizer().step());
        assertEquals(8, other.optimizer().step());
    }

    private static NnueTrainer trainer() { return new NnueTrainer(TrainableNnue.fromNetwork(NETWORK)); }

    private static void assertSnapshotPredictions(NnueTrainer trainer, SelfPlayTraining.Result result) {
        NnueEvaluator evaluator = new NnueEvaluator(result.snapshot());
        for (var sample : result.generation().samples()) {
            evaluator.evaluate(sample.board());
            assertEquals(trainer.predict(sample.board()), evaluator.boundedValue());
        }
    }

    private static String networkDigest(NnueNetwork network) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        NnueNetworkCodec.write(network, new DigestOutputStream(OutputStream.nullOutputStream(), digest));
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String trainingDigest(NnueTrainer trainer) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        TrainingStateCodec.write(trainer, new DigestOutputStream(OutputStream.nullOutputStream(), digest));
        return HexFormat.of().formatHex(digest.digest());
    }
}
