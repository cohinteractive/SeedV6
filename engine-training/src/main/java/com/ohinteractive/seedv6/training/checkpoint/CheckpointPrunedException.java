package com.ohinteractive.seedv6.training.checkpoint;

import java.io.IOException;

/** An intact historical identity whose payload was intentionally retired; never resumable. */
public final class CheckpointPrunedException extends IOException {
    public CheckpointPrunedException(String id) {
        super("Checkpoint payload intentionally pruned/not materialized: " + id);
    }
}
