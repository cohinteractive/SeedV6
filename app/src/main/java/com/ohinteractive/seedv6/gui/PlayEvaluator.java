package com.ohinteractive.seedv6.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;

/** Immutable game binding. Only the accepted persisted network crosses into play search. */
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
            return new PlayEvaluator(Mode.BEST_NNUE, best.manifest().id(), best.manifest().networkSha256(),
                    SearchEvaluation.incremental(best.network(), TrainingSettings.SCORE_MAPPING));
        } catch (IOException failure) {
            throw new IOException("Best checkpoint is unavailable, corrupt or incompatible. Store was preserved: "
                    + failure.getMessage(), failure);
        }
    }

    static String shortId(String id) {
        if (id == null || id.isEmpty()) return "—";
        return id.length() <= 25 ? id : id.substring(0, 25) + "…";
    }
}
