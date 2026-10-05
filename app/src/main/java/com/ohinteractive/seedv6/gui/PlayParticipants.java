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
    record Selection(String whiteId, String blackId, Path whiteRoot, Path blackRoot,
                     java.util.UUID whiteLineageId, java.util.UUID blackLineageId) {
        Selection(String whiteId, String blackId, Path whiteRoot, Path blackRoot) {
            this(whiteId, blackId, whiteRoot, blackRoot, null, null);
        }
        Selection swapped() { return new Selection(blackId, whiteId, blackRoot, whiteRoot, blackLineageId, whiteLineageId); }
        Selection(String whiteId, String blackId) { this(whiteId, blackId, null, null); }
        boolean independentStores() { return whiteRoot != null || blackRoot != null; }
        static final Selection BEST = new Selection("", "");
        // Bind the opponent for either colour, preserving Human side changes within the game.
        static Selection singleEngine(Path root, String id) { return new Selection(id, id, root, root); }
        static Selection singleEngine(Path root, String id, java.util.UUID lineageId) {
            return new Selection(id, id, root, root, lineageId, lineageId);
        }
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
        if (!white.architecture().nnueFamily()
                || !black.architecture().nnueFamily())
            throw new IOException("Best NNUE requires an NNUE store. Engine vs Engine supports per-side NNUE or BRN stores.");
        return new PlayParticipants(white, black, selection);
    }

    private static PlayParticipants loadIndependent(Selection selection) throws IOException {
        // Resolve both fully before installing either participant. Each owns its evaluator definition
        // and search lifecycle; a common Best is read once even if promotion happens concurrently.
        var bests = new java.util.HashMap<Path, java.util.Map<String, com.ohinteractive.seedv6.training.checkpoint.CheckpointStore.Checkpoint>>();
        PlayEvaluator white = loadIndependentSide(selection.whiteRoot(), selection.whiteId(), selection.whiteLineageId(), "White", bests);
        PlayEvaluator black = loadIndependentSide(selection.blackRoot(), selection.blackId(), selection.blackLineageId(), "Black", bests);
        return new PlayParticipants(white, black, selection);
    }
    private static PlayEvaluator loadIndependentSide(Path root, String id, java.util.UUID expectedLineage, String side,
            java.util.Map<Path, java.util.Map<String, com.ohinteractive.seedv6.training.checkpoint.CheckpointStore.Checkpoint>> cache) throws IOException {
        try {
            if (root == null) throw new IOException("Select a model lineage.");
            Path actual = root.toRealPath();
            var loaded = cache.computeIfAbsent(actual, ignored -> new java.util.HashMap<>());
            var checkpoint = loaded.get(id);
            if (checkpoint == null) {
                checkpoint = id.isEmpty()
                        ? com.ohinteractive.seedv6.training.checkpoint.CheckpointStore.readBestSnapshot(actual)
                        : com.ohinteractive.seedv6.training.checkpoint.CheckpointStore.readSnapshot(actual, id);
                PlayEvaluator.recognize(actual, checkpoint); loaded.put(id, checkpoint);
            }
            var evaluator = PlayEvaluator.fromCheckpoint(actual, checkpoint);
            if (expectedLineage != null && (evaluator.binding() == null
                    || !evaluator.binding().lineageId().filter(expectedLineage::equals).isPresent()))
                throw new IOException("Selected lineage identity changed. Refresh the library.");
            return evaluator;
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
