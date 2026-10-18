package com.ohinteractive.seedv6.training.selfplay;

import java.util.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.training.model.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class StreamingOptimizerTest {
    @ParameterizedTest @EnumSource(TrainingArchitecture.class)
    void diskSamplesMatchOriginalFisherYatesBatchMathAndExactResume(TrainingArchitecture architecture) throws Exception {
        var samples = new ArrayList<TrajectorySampler.Sample>();
        int count = architecture == TrainingArchitecture.BRN_PAIR2 ? 515 : 7;
        for (int i = 0; i < count; i++) samples.add(new TrajectorySampler.Sample(Board.startingPosition(), i % 3 - 1));
        boolean batches = architecture.nnueFamily() || architecture.corpusOnly();
        var config = new SelfPlayTraining.Config(batches ? 3 : 1, batches ? 3 : 1, true, 875);
        var reference = NetworkTrainingState.initialized(architecture, 71, .001);
        var expectedOrder = new int[samples.size()]; var random = new SplittableRandom(config.shuffleSeed());
        long expectedUpdates = 0;
        for (int epoch = 0; epoch < config.epochs(); epoch++) {
            for (int i = 0; i < expectedOrder.length; i++) expectedOrder[i] = i;
            for (int i = expectedOrder.length - 1; i > 0; i--) {
                int other = random.nextInt(i + 1), swap = expectedOrder[i]; expectedOrder[i] = expectedOrder[other]; expectedOrder[other] = swap;
            }
            for (int start = 0; start < samples.size(); start += config.minibatchSize()) {
                int batchCount = Math.min(config.minibatchSize(), samples.size() - start);
                long[][] boards = new long[batchCount][]; double[] targets = new double[batchCount];
                for (int i = 0; i < batchCount; i++) { var sample = samples.get(expectedOrder[start + i]); boards[i] = sample.board(); targets[i] = sample.target(); }
                update(reference, boards, targets, batchCount); expectedUpdates++;
            }
        }
        byte[] expected = reference.encode(); reference = null;
        var actual = NetworkTrainingState.initialized(architecture, 71, .001);
        try (var workspace = new SampleStore.Workspace()) {
            var disk = SampleStore.copyOf(samples); var stop = new SelfPlayControl();
            train(actual, disk, config, stop, p -> { if (p.optimizerUpdates() == 2) stop.cancel(); });
            assertEquals(2, stop.trainingCursor().updates());
            var restored = NetworkTrainingState.read(architecture, new java.io.ByteArrayInputStream(actual.encode())); actual = null;
            var resume = new SelfPlayControl(); resume.trainingCursor(stop.trainingCursor());
            var statistics = train(restored, disk, config, resume, p -> {}).orElseThrow();
            assertEquals(expectedUpdates, statistics.optimizerUpdates());
            assertEquals((long) samples.size() * config.epochs(), statistics.samplesTrained());
            assertArrayEquals(expected, restored.encode());
        }
    }
    private static void update(NetworkTrainingState state, long[][] boards, double[] targets, int count) {
        switch (state) {
            case NetworkTrainingState.Nnue n -> n.trainer().trainBatch(boards, targets, count);
            case NetworkTrainingState.NnueMaterial n -> n.trainer().trainBatch(boards, targets, count);
            case NetworkTrainingState.Brn3 n -> n.trainer().trainBatch(boards, targets, count);
            case NetworkTrainingState.BrnPair2 n -> n.trainer().trainBatch(boards, targets, count);
            case NetworkTrainingState.Brn n -> n.trainer().train(boards[0], targets[0]);
            case NetworkTrainingState.Brn1 n -> n.trainer().train(boards[0], targets[0]);
            case NetworkTrainingState.Brn2 n -> n.trainer().train(boards[0], targets[0]);
        }
    }
    private static Optional<SelfPlayTraining.Statistics> train(NetworkTrainingState state, List<TrajectorySampler.Sample> samples,
            SelfPlayTraining.Config config, SelfPlayControl control, java.util.function.Consumer<SelfPlayTraining.Progress> observer) {
        return switch (state) {
            case NetworkTrainingState.Nnue n -> SelfPlayTraining.trainSamples(n.trainer(), samples, config, control, observer);
            case NetworkTrainingState.NnueMaterial n -> SelfPlayTraining.trainSamples(n.trainer(), samples, config, control, observer);
            case NetworkTrainingState.Brn3 n -> Brn3CorpusOptimization.trainSamples(n.trainer(), samples, config, control, observer, TrajectorySampler.Sample::target);
            case NetworkTrainingState.BrnPair2 n -> BrnPair2CorpusOptimization.trainSamples(n.trainer(), samples, config, control, observer, TrajectorySampler.Sample::target);
            case NetworkTrainingState.Brn n -> BrnSelfPlayTraining.trainSamples(n.trainer(), samples, config, control, observer);
            case NetworkTrainingState.Brn1 n -> Brn1SelfPlayTraining.trainSamples(n.trainer(), samples, config, control, observer);
            case NetworkTrainingState.Brn2 n -> Brn2SelfPlayTraining.trainSamples(n.trainer(), samples, config, control, observer);
        };
    }
}
