package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.brn3.Brn3Trainer;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.nnue.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

/** Small campaign-facing adapter. The existing initializers, codecs and optimization loops remain authoritative. */
final class LearningArenaTraining {
    static void requireSupported(TrainingArchitecture architecture) {
        if (!architecture.nnueFamily() && !architecture.corpusOnly())
            throw new IllegalArgumentException("Learning Arena supports NNUE, BRN-3 and experimental BRN Pair-2");
    }
    static NetworkTrainingState fresh(LearningArenaConfig.Competitor competitor) {
        requireSupported(competitor.architecture());
        if (competitor.architecture() == TrainingArchitecture.NNUE_MATERIAL && competitor.nnueObjective() == null) {
            var state = new NetworkTrainingState.NnueMaterial(NnueTrainer.materialParity(TrainableNnue.initialized(competitor.seed())));
            state.setLearningRate(competitor.learningRate() == null ? TrainingRecipe.defaults(competitor.architecture()).learningRate() : competitor.learningRate());
            return state;
        }
        return NetworkTrainingState.initialized(competitor.architecture(), competitor.seed(), competitor.learningRate() == null
                ? TrainingRecipe.defaults(competitor.architecture()).learningRate() : competitor.learningRate());
    }
    static NetworkTrainingState initial(LearningArenaConfig.Competitor competitor) throws IOException {
        var selected = competitor.initialModel(); if (selected == null) return fresh(competitor);
        Path root = Path.of(selected.root()); var entry = ModelLibrary.entry(root, competitor.architecture());
        if (selected.lineageId() != null && !entry.lineage().map(l -> l.id().toString()).orElse("").equals(selected.lineageId()))
            throw new IOException("Initial model lineage identity changed");
        var manifest = com.ohinteractive.seedv6.training.checkpoint.CheckpointInspection.manifest(root.resolve("checkpoints").resolve(selected.checkpoint()));
        if (manifest.architecture() != competitor.architecture() || manifest.generation() != selected.generation())
            throw new IOException("Initial model generation/architecture changed");
        var state = CheckpointStore.readTrainingSnapshot(root, selected.checkpoint());
        requireObjective(competitor, state);
        if (competitor.learningRate() != null) state.setLearningRate(competitor.learningRate());
        return state;
    }
    /** Exact selected-model copies inherit their persisted objective; never reinterpret old moments. */
    static LearningArenaConfig.Competitor pinObjective(LearningArenaConfig.Competitor competitor) throws IOException {
        if (competitor.architecture() != TrainingArchitecture.NNUE_MATERIAL || competitor.initialModel() == null) return competitor;
        var selected = competitor.initialModel();
        var state = (NetworkTrainingState.NnueMaterial)CheckpointStore.readTrainingSnapshot(Path.of(selected.root()), selected.checkpoint());
        return new LearningArenaConfig.Competitor(competitor.name(), competitor.architecture(), competitor.seed(), competitor.minibatch(),
                competitor.learningRate(), selected, state.trainer().calibratedOutcome()
                        ? com.ohinteractive.seedv6.core.nnue.NnueMaterialBootstrap.CALIBRATED_TRAINING_ID : null);
    }
    static void requireObjective(LearningArenaConfig.Competitor competitor, NetworkTrainingState state) throws IOException {
        if (state instanceof NetworkTrainingState.NnueMaterial n && n.trainer().calibratedOutcome() != (competitor.nnueObjective() != null))
            throw new IOException("NNUE optimizer objective differs from the campaign binding");
    }
    static String recipe(LearningArenaConfig.Competitor competitor) {
        return competitor.nnueObjective() == null ? recipe(competitor.architecture())
                : competitor.architecture().schemaId() + "/" + competitor.architecture().schemaVersion() + ":"
                + competitor.nnueObjective() + ":" + com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping.V1_ID;
    }
    static String recipe(TrainingArchitecture architecture) {
        requireSupported(architecture);
        return architecture.schemaId() + "/" + architecture.schemaVersion() + ":" + switch (architecture) {
            case NNUE -> "existing-nnue-initializer-adam-v1:" + com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping.V1_ID;
            case NNUE_MATERIAL -> com.ohinteractive.seedv6.core.nnue.NnueMaterialBootstrap.TRAINING_ID + ":" + com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping.V1_ID;
            case BRN_PAIR2 -> "c02-zero-initializer-masked-adam-v1:" + com.ohinteractive.seedv6.core.brnpair2.BrnPair2Model.CALIBRATION_ID;
            case BRN3 -> "existing-brn3-initializer-masked-adam-v1:" + com.ohinteractive.seedv6.core.brn3.Brn3SearchCalibration.ID;
            default -> throw new IllegalArgumentException("Unsupported architecture");
        };
    }
    record Progress(String binding, String file, String hash, SelfPlayControl.TrainingCursor cursor) {}

    /** Periodic optimizer-boundary snapshots and an unconditional final/safe-pause snapshot.
     * A crash may recompute only the uncommitted interval, always from the saved model AND cursor.
     */
    static NetworkTrainingState train(Path directory, String binding, CheckpointStore store, String parent,
            LearningArenaConfig.Competitor competitor, CorpusTraining.Examples examples, SelfPlayTraining.Config config,
            SelfPlayControl control, java.util.function.BiConsumer<SelfPlayTraining.Progress, Telemetry> observer, long snapshotNanos) throws IOException {
        Files.createDirectories(directory);
        Path reference = directory.resolve("progress.json");
        NetworkTrainingState state;
        if (Files.exists(reference)) {
            var saved = DataFiles.read(reference, Progress.class);
            if (!binding.equals(saved.binding()) || saved.file() == null || !saved.file().matches("optimizer-[0-9a-f-]{36}\\.state")
                    || !LearningArenaFiles.hash(directory.resolve(saved.file())).equals(saved.hash()))
                throw new IOException("Learning Arena optimizer continuation mismatch");
            try (var in = new BufferedInputStream(Files.newInputStream(directory.resolve(saved.file())))) {
                state = NetworkTrainingState.read(competitor.architecture(), in);
            }
            if (saved.cursor().initialStep() < 0 || state.step() != saved.cursor().initialStep() + saved.cursor().updates())
                throw new IOException("Optimizer state/cursor mismatch");
            control.trainingCursor(saved.cursor());
        } else state = store.resumeState(parent);
        requireObjective(competitor, state);
        long total = Math.multiplyExact((long) examples.samples().size(), config.epochs());
        long started = System.nanoTime(), resumedSamples = control.trainingCursor().samples();
        long[] lastSave = {System.nanoTime()};
        try {
            new TrainerService.Operations().trainCorpus(state, examples, config, control, progress -> {
                long now = System.nanoTime();
                if (progress.samplesTrained() == total || now - lastSave[0] >= snapshotNanos) {
                    try { save(directory, binding, state, control.trainingCursor()); }
                    catch (IOException e) { throw new UncheckedIOException(e); }
                    lastSave[0] = now;
                }
                observer.accept(progress, new Telemetry(state.hyperparameters().learningRate(), now - started,
                        Math.max(0, progress.samplesTrained() - resumedSamples)));
            });
        } catch (UncheckedIOException failure) { throw failure.getCause(); }
        if (control.trainingCursor().samples() > 0) save(directory, binding, state, control.trainingCursor());
        if (!control.cancelled() && control.trainingCursor().samples() != total)
            throw new IOException("Trainer did not consume the contracted shared exposure");
        return state;
    }
    record Telemetry(double learningRate, long elapsedNanos, long invocationSamples) {}
    static void save(Path directory, String binding, NetworkTrainingState state,
                     SelfPlayControl.TrainingCursor cursor) throws IOException {
        Path reference = directory.resolve("progress.json");
        Progress prior = Files.exists(reference) ? DataFiles.read(reference, Progress.class) : null;
        String file = "optimizer-" + UUID.randomUUID() + ".state";
        LearningArenaFiles.write(directory.resolve(file), state::write);
        DataFiles.write(reference, new Progress(binding, file, LearningArenaFiles.hash(directory.resolve(file)), cursor));
        // Only the exact previously referenced application-owned payload is obsolete after atomic publication.
        if (prior != null && prior.file().matches("optimizer-[0-9a-f-]{36}\\.state")) Files.deleteIfExists(directory.resolve(prior.file()));
    }
    static void clear(Path directory) throws IOException {
        Path reference = directory.resolve("progress.json");
        if (!Files.exists(reference)) return;
        var prior = DataFiles.read(reference, Progress.class);
        Files.delete(reference);
        if (prior.file().matches("optimizer-[0-9a-f-]{36}\\.state")) Files.deleteIfExists(directory.resolve(prior.file()));
    }
    private LearningArenaTraining() {}
}
