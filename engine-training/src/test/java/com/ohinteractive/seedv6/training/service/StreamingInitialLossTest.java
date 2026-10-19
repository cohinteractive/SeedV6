package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.nnue.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class StreamingInitialLossTest {
    @ParameterizedTest @EnumSource(value = TrainingArchitecture.class, names = {"NNUE", "NNUE_MATERIAL", "BRN2", "BRN3", "BRN_PAIR2", "BRN_GATED", "BRN_CUBIC", "BRN_POOLED"})
    void fusedInitialLossEqualsOriginalMetricPass(TrainingArchitecture architecture) {
        verify(NetworkTrainingState.initialized(architecture, 71, .001));
    }
    @Test void legacyMaterialObjectiveRetainsItsLoss() {
        verify(new NetworkTrainingState.NnueMaterial(NnueTrainer.materialParity(TrainableNnue.initialized(71))));
    }
    private void verify(NetworkTrainingState state) {
        var samples = new ArrayList<TrajectorySampler.Sample>();
        double sum = 0;
        for (int i = 0; i < 7; i++) {
            var sample = new TrajectorySampler.Sample(Board.startingPosition(), 0, (i - 3) / 4.0);
            samples.add(sample); sum += TrainerService.corpusInitialLoss(state, sample);
        }
        var control = new SelfPlayControl();
        var config = new SelfPlayTraining.Config(1, state.architecture() == TrainingArchitecture.BRN2 ? 1 : 3, true, 71);
        java.util.function.Consumer<SelfPlayTraining.Progress> stop = p -> control.cancel();
        SelfPlayTraining.Statistics result = switch (state) {
            case NetworkTrainingState.Nnue n -> SelfPlayTraining.trainSamples(n.trainer(), samples, config, control, stop, TrajectorySampler.Sample::trainingTarget).orElseThrow();
            case NetworkTrainingState.NnueMaterial n -> SelfPlayTraining.trainSamples(n.trainer(), samples, config, control, stop, TrajectorySampler.Sample::trainingTarget).orElseThrow();
            case NetworkTrainingState.Brn2 n -> Brn2SelfPlayTraining.trainSamples(n.trainer(), samples, config, control, stop, TrajectorySampler.Sample::trainingTarget).orElseThrow();
            case NetworkTrainingState.Brn3 n -> Brn3CorpusOptimization.trainSamples(n.trainer(), samples, config, control, stop, TrajectorySampler.Sample::trainingTarget).orElseThrow();
            case NetworkTrainingState.BrnPair2 n -> BrnPair2CorpusOptimization.trainSamples(n.trainer(), samples, config, control, stop, TrajectorySampler.Sample::trainingTarget).orElseThrow();
            case NetworkTrainingState.BrnNext n -> BatchedCorpusOptimization.trainSamples(n.trainer()::step, n.trainer()::trainBatch, n.trainer()::predictOutcome, samples, config, control, stop, TrajectorySampler.Sample::trainingTarget).orElseThrow();
            default -> throw new AssertionError();
        };
        assertEquals(Double.doubleToRawLongBits(sum / samples.size()), Double.doubleToRawLongBits(result.initialLoss()));
    }
}
