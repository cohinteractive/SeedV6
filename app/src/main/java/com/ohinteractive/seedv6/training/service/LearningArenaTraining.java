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
        if (architecture != TrainingArchitecture.NNUE && architecture != TrainingArchitecture.BRN3)
            throw new IllegalArgumentException("Learning Arena V1 supports NNUE and BRN-3");
    }
    static NetworkTrainingState fresh(LearningArenaConfig.Competitor competitor) {
        return switch (competitor.architecture()) {
            case NNUE -> new NetworkTrainingState.Nnue(new NnueTrainer(TrainableNnue.initialized(competitor.seed())));
            case BRN3 -> new NetworkTrainingState.Brn3(new Brn3Trainer(competitor.seed()));
            default -> throw new IllegalArgumentException("Unsupported Learning Arena architecture");
        };
    }
    static String recipe(TrainingArchitecture architecture) {
        requireSupported(architecture);
        return architecture.schemaId() + "/" + architecture.schemaVersion() + ":" + switch (architecture) {
            case NNUE -> "existing-nnue-initializer-adam-v1:" + com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping.V1_ID;
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
            SelfPlayControl control, Consumer<SelfPlayTraining.Progress> observer, long snapshotNanos) throws IOException {
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
        long total = Math.multiplyExact((long) examples.samples().size(), config.epochs());
        long[] lastSave = {System.nanoTime()};
        try {
            new TrainerService.Operations().trainCorpus(state, examples, config, control, progress -> {
                long now = System.nanoTime();
                if (progress.samplesTrained() == total || now - lastSave[0] >= snapshotNanos) {
                    try { save(directory, binding, state, control.trainingCursor()); }
                    catch (IOException e) { throw new UncheckedIOException(e); }
                    lastSave[0] = now;
                }
                observer.accept(progress);
            });
        } catch (UncheckedIOException failure) { throw failure.getCause(); }
        if (control.trainingCursor().samples() > 0) save(directory, binding, state, control.trainingCursor());
        if (!control.cancelled() && control.trainingCursor().samples() != total)
            throw new IOException("Trainer did not consume the contracted shared exposure");
        return state;
    }
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
