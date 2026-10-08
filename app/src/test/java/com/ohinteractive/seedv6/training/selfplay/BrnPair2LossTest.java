package com.ohinteractive.seedv6.training.selfplay;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.Brn3Objective;
import com.ohinteractive.seedv6.core.brnpair2.BrnPair2Trainer;
import com.ohinteractive.seedv6.training.nnue.NnueCorpusTargets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BrnPair2LossTest {
    private static List<TrajectorySampler.Sample> samples() {
        return List.of(new TrajectorySampler.Sample(Board.startingPosition(), 1),
                new TrajectorySampler.Sample(Board.fromFen("4k3/8/8/8/8/8/3P4/4K3 w - - 0 1"), 0),
                new TrajectorySampler.Sample(Board.fromFen("4k3/8/8/8/8/8/3P4/4K3 b - - 0 1"), -1));
    }
    // Source adapters may supply soft targets; the sample's WDL placeholder is not used.
    private static double target(TrajectorySampler.Sample sample) { return sample.target() * .7; }

    @Test void runningCrossEntropyWeightsVisitsAcrossUnequalBatchesAndEpochsWhileFinalFitUsesHalfSquaredError() {
        var samples = samples(); var trainer = new BrnPair2Trainer(.01); var reference = new BrnPair2Trainer(.01);
        var expectedMeans = new ArrayList<Double>(); double sum = 0; int visits = 0;
        for (int epoch = 0; epoch < 2; epoch++) for (int start = 0; start < samples.size(); start += 2) {
            int count = Math.min(2, samples.size() - start); long[][] boards = new long[count][]; double[] targets = new double[count];
            for (int i = 0; i < count; i++) {
                var sample = samples.get(start + i); boards[i] = sample.board(); targets[i] = target(sample);
                double outcome = Brn3Objective.smoothOutcome(reference.predictPawns(boards[i]), NnueCorpusTargets.material(boards[i]))[0];
                double p = (1 + outcome) / 2, q = (1 + targets[i]) / 2;
                sum += -q * Math.log(p) - (1 - q) * Math.log1p(-p); visits++;
            }
            reference.trainBatch(boards, targets, count); expectedMeans.add(sum / visits);
        }
        var progress = new ArrayList<SelfPlayTraining.Progress>(); var control = new SelfPlayControl();
        var result = BrnPair2CorpusOptimization.trainSamples(trainer, samples,
                new SelfPlayTraining.Config(2, 2, false, 1), control, progress::add, BrnPair2LossTest::target).orElseThrow();
        assertEquals(6, result.samplesTrained()); assertEquals(4, result.optimizerUpdates());
        assertEquals(List.of(2L, 3L, 5L, 6L), progress.stream().map(SelfPlayTraining.Progress::samplesTrained).toList());
        for (int i = 0; i < progress.size(); i++) assertEquals(expectedMeans.get(i), progress.get(i).meanTrainingLoss(), 1e-11);
        assertEquals(sum / visits, result.meanTrainingLoss(), 1e-11);
        double finalError = 0;
        for (var sample : samples) {
            double error = trainer.predictOutcome(sample.board()) - target(sample); finalError += .5 * error * error;
        }
        assertEquals(finalError / samples.size(), result.finalLoss(), 1e-14);
        assertNotEquals(result.meanTrainingLoss(), result.finalLoss(), .01);
        assertEquals(result.meanTrainingLoss(), control.trainingCursor().lossSum() / control.trainingCursor().samples(), 1e-14);
    }

    @Test void safeResumeRetainsTheRunningObjectiveAndWeightsWithoutRepeatingVisits() {
        var all = new BrnPair2Trainer(.01); var split = new BrnPair2Trainer(.01);
        var config = new SelfPlayTraining.Config(2, 2, true, 13);
        var expected = BrnPair2CorpusOptimization.trainSamples(all, samples(), config, new SelfPlayControl(), p -> {}, BrnPair2LossTest::target).orElseThrow();
        var stop = new SelfPlayControl();
        var partial = BrnPair2CorpusOptimization.trainSamples(split, samples(), config, stop, p -> stop.cancel(), BrnPair2LossTest::target).orElseThrow();
        assertTrue(partial.cancelled()); assertTrue(Double.isNaN(partial.finalLoss()));
        var resume = new SelfPlayControl(); resume.trainingCursor(stop.trainingCursor());
        var actual = BrnPair2CorpusOptimization.trainSamples(split, samples(), config, resume, p -> {}, BrnPair2LossTest::target).orElseThrow();
        assertEquals(expected, actual);
    }
}
