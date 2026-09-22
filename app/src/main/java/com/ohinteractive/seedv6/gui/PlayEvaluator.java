package com.ohinteractive.seedv6.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;

/** Immutable game binding to a validated persisted network. */
record PlayEvaluator(Mode mode, String checkpointId, String networkHash, SearchEvaluation evaluation) {
    enum Mode {
        HANDCRAFTED("Handcrafted"), BEST_NNUE("Best NNUE");
        private final String label;
        Mode(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    static PlayEvaluator handcrafted() {
        return new PlayEvaluator(Mode.HANDCRAFTED, "", "", SearchEvaluation.handcrafted());
    }

    static PlayEvaluator loadBest(Path root) throws IOException {
        if (root == null || !Files.isDirectory(root) || !Files.isDirectory(root.resolve("checkpoints"))) {
            throw new IOException("No checkpoint store at this folder. Start training to establish a bootstrap best before playing.");
        }
        try {
            var best = CheckpointStore.readBestSnapshot(root);
            requireNnue(best);
            return new PlayEvaluator(Mode.BEST_NNUE, best.manifest().id(), best.manifest().networkSha256(),
                    SearchEvaluation.incremental(best.network(), TrainingSettings.SCORE_MAPPING));
        } catch (IOException failure) {
            throw new IOException("Best checkpoint is unavailable, corrupt or incompatible. Store was preserved: "
                    + failure.getMessage(), failure);
        }
    }

    static PlayEvaluator load(Path root, String checkpointId) throws IOException {
        if (checkpointId.isEmpty()) return loadBest(root);
        var checkpoint = CheckpointStore.readSnapshot(root, checkpointId);
        requireNnue(checkpoint);
        return new PlayEvaluator(Mode.BEST_NNUE, checkpoint.manifest().id(), checkpoint.manifest().networkSha256(),
                SearchEvaluation.incremental(checkpoint.network(), TrainingSettings.SCORE_MAPPING));
    }

    private static void requireNnue(CheckpointStore.Checkpoint checkpoint) throws IOException {
        if (checkpoint.manifest().architecture() != com.ohinteractive.seedv6.training.model.TrainingArchitecture.NNUE)
            throw new IOException("Play requires an NNUE store; the selected store contains BRN.");
    }

    String description() {
        return mode == Mode.BEST_NNUE ? "NNUE · " + shortId(checkpointId) : "Handcrafted evaluator";
    }

    String identity() {
        return mode == Mode.BEST_NNUE ? "Checkpoint: " + checkpointId + " | Network SHA-256: " + networkHash : description();
    }

    record Choice(String checkpointId) {
        static final Choice BEST = new Choice("");
        @Override public String toString() {
            if (checkpointId.isEmpty()) return "Best NNUE";
            String generation = TrainingProgress.generation(java.util.OptionalLong.empty(), checkpointId);
            int hash = checkpointId.lastIndexOf('-') + 1;
            return "Gen " + generation + " · " + checkpointId.substring(hash, Math.min(hash + 6, checkpointId.length()));
        }
    }

    static String shortId(String id) {
        if (id == null || id.isEmpty()) return "—";
        return id.length() <= 25 ? id : id.substring(0, 25) + "…";
    }
}
