package com.ohinteractive.seedv6.training.service;

/** Corpus count/identity is durable campaign configuration; seed is TrainerConfig.masterSeed. */
public record CorpusTrainingConfig(int positionsPerGeneration, String viewIdentity) {
    public CorpusTrainingConfig {
        if (positionsPerGeneration < 2 || viewIdentity == null || !viewIdentity.isEmpty() && !viewIdentity.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("Invalid corpus training configuration");
    }
    public CorpusTrainingConfig(int positions) { this(positions, ""); }
    public String settings() { return "|corpus-cp-v1:" + positionsPerGeneration + ":" + viewIdentity; }
}
