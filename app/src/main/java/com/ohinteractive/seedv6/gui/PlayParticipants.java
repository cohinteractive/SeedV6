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
    record Selection(String whiteId, String blackId) {
        static final Selection BEST = new Selection("", "");
        Selection { Objects.requireNonNull(whiteId); Objects.requireNonNull(blackId); }
    }

    static PlayParticipants shared(PlayEvaluator evaluator) { return new PlayParticipants(evaluator, evaluator, Selection.BEST); }
    PlayEvaluator forSide(int side) { return side == Value.WHITE ? white : black; }

    static PlayParticipants load(PlayEvaluator.Mode mode, Path root, Selection selection) throws IOException {
        if (mode == PlayEvaluator.Mode.HANDCRAFTED) return shared(PlayEvaluator.handcrafted());
        // Both Best choices resolve the same snapshot even if promotion occurs during construction.
        PlayEvaluator best = selection.whiteId().isEmpty() || selection.blackId().isEmpty()
                ? PlayEvaluator.loadBest(root) : null;
        PlayEvaluator white = loadSide(root, selection.whiteId(), best, "White");
        PlayEvaluator black = selection.blackId().equals(selection.whiteId()) ? white
                : loadSide(root, selection.blackId(), best, "Black");
        return new PlayParticipants(white, black, selection);
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
