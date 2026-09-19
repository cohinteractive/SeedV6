package com.ohinteractive.seedv6.training.nnue;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.NnueEvaluator;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import org.junit.jupiter.api.Test;

import static com.ohinteractive.seedv6.training.nnue.TrainingFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class NnueLearningTest {
    @Test
    void deterministicPositiveNegativeAndDrawLearning() {
        for (double target : new double[] {1, -1, 0}) {
            TrainableNnue model = model();
            if (target == 0) model.parameters.groups[4][0] = 0.75f;
            NnueTrainer trainer = new NnueTrainer(model, new AdamHyperparameters(0.01, 0.9, 0.999, 1e-8));
            long[][] boards = {WHITE};
            double[] targets = {target};
            double before = trainer.predict(WHITE), lossBefore = loss(trainer, WHITE, target);
            int updates = 100;
            for (int i = 0; i < updates; i++) trainer.trainBatch(boards, targets, 1);
            double after = trainer.predict(WHITE), lossAfter = loss(trainer, WHITE, target);
            if (target == 1) assertTrue(after > before + 0.4);
            if (target == -1) assertTrue(after < before - 0.7);
            if (target == 0) assertTrue(Math.abs(after) < Math.abs(before) / 10);
            assertTrue(lossAfter < lossBefore / 10);
            assertEquals(updates, trainer.optimizer().step());
            System.out.printf("LEARNING target=%+.0f updates=%d prediction=%.9f->%.9f loss=%.12g->%.12g%n",
                    target, updates, before, after, lossBefore, lossAfter);
        }
    }

    @Test
    void mixedMinibatchReducesLossAndIsBitDeterministic() {
        // Smaller steps keep this intentionally low-rank fixture away from dead clip units.
        AdamHyperparameters hp = new AdamHyperparameters(0.001, 0.9, 0.999, 1e-8);
        NnueTrainer trainer = new NnueTrainer(model(), hp), same = new NnueTrainer(model(), hp);
        long[][] boards = {WHITE, BLACK, OTHER};
        double[] targets = {0.8, -0.8, 0};
        double[] before = {trainer.predict(WHITE), trainer.predict(BLACK), trainer.predict(OTHER)};
        double lossBefore = meanLoss(trainer, boards, targets);
        int updates = 300;
        for (int i = 0; i < updates; i++) {
            assertEquals(trainer.trainBatch(boards, targets, 3), same.trainBatch(boards, targets, 3));
        }
        double lossAfter = meanLoss(trainer, boards, targets);
        assertTrue(lossAfter < lossBefore / 5, "loss=" + lossBefore + "->" + lossAfter
                + " predictions=" + trainer.predict(WHITE) + "," + trainer.predict(BLACK) + "," + trainer.predict(OTHER));
        assertBits(trainer.model().parameters, same.model().parameters);
        assertBits(trainer.optimizer().firstMoment, same.optimizer().firstMoment);
        assertBits(trainer.optimizer().secondMoment, same.optimizer().secondMoment);
        System.out.printf("MIXED updates=%d prediction=[%.9f,%.9f,%.9f]->[%.9f,%.9f,%.9f] loss=%.12g->%.12g%n",
                updates, before[0], before[1], before[2], trainer.predict(WHITE), trainer.predict(BLACK),
                trainer.predict(OTHER), lossBefore, lossAfter);
    }

    @Test
    void initializationAndSnapshotsMatchInferenceBitExactlyBeforeAndAfterTraining() {
        long[][] boards = {WHITE, BLACK, OTHER, Board.startingPosition(),
                Board.fromFen("7k/8/8/8/8/8/1Q6/K6Q b - - 0 1"),
                Board.fromFen("r3k2r/pppq1ppp/2npbn2/3Np3/2B1P3/2N2Q1P/PPP2PP1/R3K2R w KQkq - 4 12")};
        TrainableNnue model = TrainableNnue.initialized(73);
        assertBits(Parameters.from(NnueNetwork.initialized(73)), model.parameters);
        NnueTrainer trainer = new NnueTrainer(model);
        NnueNetwork frozen = model.snapshot();
        NnueEvaluator inference = new NnueEvaluator(frozen);
        double[] original = new double[boards.length];
        for (int i = 0; i < boards.length; i++) {
            long[] untouched = boards[i].clone();
            original[i] = trainer.predict(boards[i]);
            assertEquals(inference.evaluate(boards[i]), trainer.scratch.raw);
            assertEquals(inference.boundedValue(), original[i]);
            assertArrayEquals(untouched, boards[i]);
        }
        trainer.trainBatch(boards, new double[] {1, -1, 0, 0.5, -0.5, 0.25}, boards.length);
        NnueEvaluator trained = new NnueEvaluator(model.snapshot());
        boolean changed = false;
        for (int i = 0; i < boards.length; i++) {
            double value = trainer.predict(boards[i]);
            assertEquals(trained.evaluate(boards[i]), trainer.scratch.raw);
            assertEquals(trained.boundedValue(), value);
            inference.evaluate(boards[i]);
            assertEquals(original[i], inference.boundedValue());
            changed |= original[i] != value;
        }
        assertTrue(changed);
    }

    @Test
    void samePlacementOppositeSideUsesOrderingWithoutImplicitNegation() {
        NnueTrainer trainer = new NnueTrainer(model());
        double white = trainer.predict(WHITE), black = trainer.predict(BLACK);
        assertEquals(StrictMath.tanh(0.0625f + 0.5f * (0.25f + 0.25f * 0.375f - 0.125f * 0.3125f)), white);
        assertEquals(StrictMath.tanh(0.0625f + 0.5f * (0.25f + 0.25f * 0.3125f - 0.125f * 0.375f)), black);
        assertNotEquals(white, black);
        assertNotEquals(-white, black);
        NnueTrainer.BatchStatistics stats = trainer.accumulate(new long[][] {WHITE, BLACK}, new double[] {0.7, -0.7}, 2);
        assertEquals(0, stats.meanTarget());
        assertEquals((0.5 * (white - 0.7) * (white - 0.7) + 0.5 * (black + 0.7) * (black + 0.7)) / 2,
                stats.meanLoss());
    }
}
