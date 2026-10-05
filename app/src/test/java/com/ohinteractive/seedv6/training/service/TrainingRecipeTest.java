package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn.*;
import com.ohinteractive.seedv6.core.brn1.*;
import com.ohinteractive.seedv6.core.brn2.*;
import com.ohinteractive.seedv6.core.brn3.*;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.nnue.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import java.io.*;
import java.nio.file.Path;
import java.security.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class TrainingRecipeTest {
    @TempDir Path root;
    static NetworkTrainingState initial(TrainingArchitecture architecture) {
        return switch (architecture) {
            case NNUE -> new NetworkTrainingState.Nnue(new NnueTrainer(TrainableNnue.initialized(71)));
            case NNUE_MATERIAL -> new NetworkTrainingState.NnueMaterial(NnueTrainer.materialParity(TrainableNnue.initialized(71)));
            case BRN -> new NetworkTrainingState.Brn(new BrnTrainer(.001));
            case BRN1 -> new NetworkTrainingState.Brn1(new Brn1Trainer(.001));
            case BRN2 -> new NetworkTrainingState.Brn2(new Brn2Trainer(.001));
            case BRN3 -> new NetworkTrainingState.Brn3(new Brn3Trainer(71));
        };
    }
    static void trainOne(NetworkTrainingState state) {
        long[] board = Board.fromFen(TrainerConfig.STANDARD_START);
        switch (state) {
            case NetworkTrainingState.Nnue n -> n.trainer().trainBatch(new long[][]{board}, new double[]{.8}, 1);
            case NetworkTrainingState.NnueMaterial n -> n.trainer().trainBatch(new long[][]{board}, new double[]{.8}, 1);
            case NetworkTrainingState.Brn b -> b.trainer().train(board, .8);
            case NetworkTrainingState.Brn1 b -> b.trainer().train(board, .8);
            case NetworkTrainingState.Brn2 b -> b.trainer().train(board, .8);
            case NetworkTrainingState.Brn3 b -> b.trainer().trainBatch(new long[][]{board}, new double[]{.8}, 1);
        }
    }
    static String digest(NetworkTrainingState state) throws Exception {
        var hash = MessageDigest.getInstance("SHA-256");
        state.write(new DigestOutputStream(OutputStream.nullOutputStream(), hash)); return HexFormat.of().formatHex(hash.digest());
    }
    @ParameterizedTest @EnumSource(TrainingArchitecture.class)
    void rateChangePreservesEveryOtherOptimizerByteAndRetainsTheCodec(TrainingArchitecture architecture) throws Exception {
        var state = initial(architecture); trainOne(state);
        long step = state.step(); var before = state.hyperparameters(); String bytes = digest(state);
        state.setLearningRate(.007);
        assertEquals(step, state.step()); assertEquals(.007, state.hyperparameters().learningRate());
        assertEquals(before.beta1(), state.hyperparameters().beta1()); assertEquals(before.beta2(), state.hyperparameters().beta2());
        assertEquals(before.epsilon(), state.hyperparameters().epsilon());
        state.setLearningRate(before.learningRate()); assertEquals(bytes, digest(state));
        state.setLearningRate(.007);
        var resumed = NetworkTrainingState.read(architecture, new ByteArrayInputStream(state.encode()));
        assertEquals(digest(state), digest(resumed));
        state.setLearningRate(before.learningRate()); trainOne(state); trainOne(resumed);
        resumed.setLearningRate(before.learningRate());
        assertNotEquals(digest(state), digest(resumed), "Different rates must affect weights, not just metadata");
        assertThrows(IllegalArgumentException.class, () -> state.setLearningRate(Double.NaN));
    }
    @ParameterizedTest @ValueSource(doubles = {.004, .007})
    void explicitRateParticipatesInPartialResumeIdentity(double resumedRate) throws Exception {
        var support = new TrainingRunControlTest(); support.temporary = root;
        var config = support.config(root, 1).withLearningRate(.004);
        var owner = new AtomicReference<TrainerService>();
        var stopping = new TrainerService.Operations() {
            @Override Optional<SelfPlayTraining.Statistics> trainBootstrap(NetworkTrainingState state, List<TrajectorySampler.Sample> samples,
                    SelfPlayTraining.Config cfg, SelfPlayControl control, Consumer<SelfPlayTraining.Progress> observer) {
                assertEquals(.004, state.hyperparameters().learningRate());
                return super.trainBootstrap(state, samples, cfg, control, p -> {
                    observer.accept(p); if (p.optimizerUpdates() == 1) owner.get().stop();
                });
            }
        };
        String parent;
        try (var s = TrainerService.fresh(config, initial(TrainingArchitecture.BRN2), stopping, v -> {})) {
            owner.set(s); var end = support.finish(s); parent = end.latestTrainingId(); assertEquals(0, end.totals().completedGenerations());
        }
        var partial = PartialGeneration.inspect(root).orElseThrow();
        var next = config.withLearningRate(resumedRate);
        assertEquals(resumedRate == .004, partial.attempt().matches(next, TrainingSource.HANDCRAFTED));
        var resumedWork = new TrainerService.Operations() {
            @Override Optional<SelfPlayTraining.Statistics> trainBootstrap(NetworkTrainingState state, List<TrajectorySampler.Sample> samples,
                    SelfPlayTraining.Config cfg, SelfPlayControl control, Consumer<SelfPlayTraining.Progress> observer) {
                assertEquals(resumedRate, state.hyperparameters().learningRate());
                assertEquals(resumedRate == .004 ? 1 : 0, state.step());
                return super.trainBootstrap(state, samples, cfg, control, observer);
            }
        };
        try (var s = TrainerService.resume(next, resumedWork, v -> {})) {
            var end = support.finish(s); assertEquals(1, end.totals().completedGenerations()); assertEquals(6, end.optimizerStep());
            var catalog = CheckpointStore.catalog(root);
            assertEquals(.004, catalog.checkpoints().stream().filter(c -> c.manifest().id().equals(parent)).findFirst().orElseThrow().hyperparameters().learningRate());
            assertEquals(resumedRate, catalog.checkpoints().stream().filter(c -> c.manifest().id().equals(end.candidateId())).findFirst().orElseThrow().hyperparameters().learningRate());
        }
    }
    @Test void oldConfigurationRetainsItsStoredRateAndFingerprint() throws Exception {
        var support = new TrainingRunControlTest(); support.temporary = root;
        var config = support.config(root, 1);
        assertEquals("Training[epochs=1, minibatchSize=1, shuffle=true]", config.training().toString());
        assertFalse(config.attemptSettings(1, TrainingSource.HANDCRAFTED).contains("learningRate"));
        var initial = initial(TrainingArchitecture.BRN2); initial.setLearningRate(.009);
        try (var service = TrainerService.fresh(config, initial, new TrainerService.Operations(), v -> {})) {
            var end = support.finish(service);
            assertEquals(.009, CheckpointStore.catalog(root).checkpoints().stream()
                    .filter(c -> c.manifest().id().equals(end.candidateId())).findFirst().orElseThrow().hyperparameters().learningRate());
        }
    }
}
