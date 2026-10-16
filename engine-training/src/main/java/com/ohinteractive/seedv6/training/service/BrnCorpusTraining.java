package com.ohinteractive.seedv6.training.service;

import java.io.IOException;

/** Compatibility name for existing BRN callers; selection/pinning now lives in the shared service. */
public final class BrnCorpusTraining extends CorpusTraining {
    public BrnCorpusTraining(TrainerConfig config, TrainingSource source, boolean mayCreate) throws IOException {
        super(config, source, mayCreate);
    }
}
