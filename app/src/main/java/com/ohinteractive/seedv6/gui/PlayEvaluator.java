package com.ohinteractive.seedv6.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;

/** Immutable game binding to a validated persisted network. */
record PlayEvaluator(Mode mode, String checkpointId, String networkHash, SearchEvaluation evaluation, com.ohinteractive.seedv6.training.model.TrainingArchitecture architecture, com.ohinteractive.seedv6.training.model.ModelLibrary.Binding binding) {
    PlayEvaluator(Mode mode, String id, String hash, SearchEvaluation evaluation,
                  com.ohinteractive.seedv6.training.model.TrainingArchitecture architecture) {
        this(mode, id, hash, evaluation, architecture, null);
    }
    PlayEvaluator(Mode mode, String id, String hash, SearchEvaluation evaluation) {
        this(mode, id, hash, evaluation, mode == Mode.HANDCRAFTED ? null
                : com.ohinteractive.seedv6.training.model.TrainingArchitecture.NNUE);
    }
    static PlayEvaluator fromCheckpoint(CheckpointStore.Checkpoint checkpoint) {
        return new PlayEvaluator(Mode.BEST_NNUE, checkpoint.manifest().id(), checkpoint.manifest().networkSha256(),
                checkpoint.model().evaluation(TrainingSettings.SCORE_MAPPING), checkpoint.manifest().architecture());
    }
    static PlayEvaluator fromCheckpoint(Path root, CheckpointStore.Checkpoint checkpoint) throws IOException {
        var manifest = checkpoint.manifest();
        var entry = com.ohinteractive.seedv6.training.model.ModelLibrary.entry(root, manifest.architecture());
        var binding = new com.ohinteractive.seedv6.training.model.ModelLibrary.Binding(root,
                entry.lineage().map(com.ohinteractive.seedv6.training.checkpoint.TrainingLineage::id),
                entry.name(), manifest.architecture(), manifest.id(), manifest.generation());
        return new PlayEvaluator(Mode.BEST_NNUE, manifest.id(), manifest.networkSha256(),
                checkpoint.model().evaluation(TrainingSettings.SCORE_MAPPING), manifest.architecture(), binding);
    }
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
            recognize(root, best);
            return fromCheckpoint(root, best);
        } catch (IOException failure) {
            throw new IOException("Best checkpoint is unavailable, corrupt or incompatible. Store was preserved: "
                    + failure.getMessage(), failure);
        }
    }

    static PlayEvaluator load(Path root, String checkpointId) throws IOException {
        if (checkpointId.isEmpty()) return loadBest(root);
        var checkpoint = CheckpointStore.readSnapshot(root, checkpointId);
        recognize(root, checkpoint);
        return fromCheckpoint(root, checkpoint);
    }

    static void recognize(Path root, CheckpointStore.Checkpoint best) throws IOException {
        if (com.ohinteractive.seedv6.training.checkpoint.CheckpointInspection.freshRoot(root, best.manifest().architecture()))
            throw new IOException("The selected folder has no initialized checkpoint store.");
    }

    String description() {
        if (binding != null) return binding.description();
        return mode == Mode.BEST_NNUE ? NetworkArchitecture.valueOf(architecture.name()) + " \u00b7 Gen " + TrainingProgress.generation(java.util.OptionalLong.empty(), checkpointId) : "Handcrafted evaluator";
    }

    String identity() {
        return mode == Mode.BEST_NNUE ? (binding == null ? "" : binding.description() + " | Lineage: " + binding.lineageId().map(Object::toString).orElse("not recorded") + " | ") + "Checkpoint: " + checkpointId + " | Network SHA-256: " + networkHash : description();
    }

    static String shortId(String id) {
        if (id == null || id.isEmpty()) return "—";
        return id.length() <= 25 ? id : id.substring(0, 25) + "…";
    }
}
