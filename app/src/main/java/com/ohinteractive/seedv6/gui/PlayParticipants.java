package com.ohinteractive.seedv6.gui;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import com.ohinteractive.seedv6.core.util.Value;

/** Concrete game ownership, independent of the next game's requested network identities. */
record PlayParticipants(PlayEvaluator white, PlayEvaluator black, Selection selection) {
    PlayParticipants {
        Objects.requireNonNull(white); Objects.requireNonNull(black); Objects.requireNonNull(selection);
    }

    /** Empty identity means resolve current Best once when constructing the game. */
    record Selection(String whiteId, String blackId, Path whiteRoot, Path blackRoot) {
        Selection(String whiteId, String blackId) { this(whiteId, blackId, null, null); }
        boolean independentStores() { return whiteRoot != null || blackRoot != null; }
        static final Selection BEST = new Selection("", "");
        Selection { Objects.requireNonNull(whiteId); Objects.requireNonNull(blackId); }
    }

    static PlayParticipants shared(PlayEvaluator evaluator) { return new PlayParticipants(evaluator, evaluator, Selection.BEST); }
    PlayEvaluator forSide(int side) { return side == Value.WHITE ? white : black; }

    static PlayParticipants load(PlayEvaluator.Mode mode, Path root, Selection selection) throws IOException {
        if (mode == PlayEvaluator.Mode.HANDCRAFTED) return shared(PlayEvaluator.handcrafted());
        if (selection.independentStores()) return loadIndependent(selection);
        // Both Best choices resolve the same snapshot even if promotion occurs during construction.
        PlayEvaluator best = selection.whiteId().isEmpty() || selection.blackId().isEmpty()
                ? PlayEvaluator.loadBest(root) : null;
        PlayEvaluator white = loadSide(root, selection.whiteId(), best, "White");
        PlayEvaluator black = selection.blackId().equals(selection.whiteId()) ? white
                : loadSide(root, selection.blackId(), best, "Black");
        if (white.architecture() != com.ohinteractive.seedv6.training.model.TrainingArchitecture.NNUE
                || black.architecture() != com.ohinteractive.seedv6.training.model.TrainingArchitecture.NNUE)
            throw new IOException("Best NNUE requires an NNUE store. Engine vs Engine supports per-side NNUE or BRN stores.");
        return new PlayParticipants(white, black, selection);
    }

    private static PlayParticipants loadIndependent(Selection selection) throws IOException {
        // Resolve both fully before installing either participant. Each owns its evaluator definition
        // and search lifecycle; a common Best is read once even if promotion happens concurrently.
        var bests = new java.util.HashMap<Path, com.ohinteractive.seedv6.training.checkpoint.CheckpointStore.Checkpoint>();
        PlayEvaluator white = loadIndependentSide(selection.whiteRoot(), selection.whiteId(), "White", bests);
        PlayEvaluator black = loadIndependentSide(selection.blackRoot(), selection.blackId(), "Black", bests);
        return new PlayParticipants(white, black, selection);
    }
    private static PlayEvaluator loadIndependentSide(Path root, String id, String side,
            java.util.Map<Path, com.ohinteractive.seedv6.training.checkpoint.CheckpointStore.Checkpoint> bests) throws IOException {
        try {
            if (root == null) throw new IOException("Select a checkpoint store.");
            Path actual = root.toRealPath();
            var best = bests.get(actual);
            if (best == null) {
                best = com.ohinteractive.seedv6.training.checkpoint.CheckpointStore.readBestSnapshot(actual);
                PlayEvaluator.recognize(actual, best); bests.put(actual, best);
            }
            var checkpoint = id.isEmpty() || id.equals(best.manifest().id()) ? best
                    : com.ohinteractive.seedv6.training.checkpoint.CheckpointStore.readSnapshot(actual, id);
            if (checkpoint.manifest().architecture() != best.manifest().architecture())
                throw new IOException("Selected generation architecture differs from the store.");
            return PlayEvaluator.fromCheckpoint(checkpoint);
        } catch (IOException | RuntimeException failure) {
            throw new IOException(side + " network is unavailable, pruned, corrupt or incompatible: " + failure.getMessage(), failure);
        }
    }

    private static PlayEvaluator loadSide(Path root, String id, PlayEvaluator best, String side) throws IOException {
        if (id.isEmpty() || best != null && id.equals(best.checkpointId())) return best;
        try { return PlayEvaluator.load(root, id); }
        catch (IOException failure) {
            throw new IOException(side + " network " + PlayEvaluator.shortId(id)
                    + " is unavailable, pruned, corrupt or incompatible: " + failure.getMessage(), failure);
        }
    }
}
