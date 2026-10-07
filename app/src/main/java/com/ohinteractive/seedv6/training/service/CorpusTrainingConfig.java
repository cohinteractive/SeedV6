package com.ohinteractive.seedv6.training.service;

/** Corpus count/identity is durable campaign configuration; seed is TrainerConfig.masterSeed. */
public record CorpusTrainingConfig(int positionsPerGeneration, String viewIdentity, String targetAdapter) {
    public CorpusTrainingConfig {
        if (positionsPerGeneration < 2 || viewIdentity == null || !viewIdentity.isEmpty() && !viewIdentity.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("Invalid corpus training configuration");
        if (targetAdapter == null || !targetAdapter.isEmpty()
                && !targetAdapter.equals(com.ohinteractive.seedv6.training.nnue.NnueCorpusTargets.ID)
                && !targetAdapter.equals(CorpusTraining.TargetPolicy.BRN3_CP_WDL_V1.identity)
                && !CorpusTraining.sourceOutcome(targetAdapter))
            throw new IllegalArgumentException("Unsupported corpus target adapter");
    }
    public CorpusTrainingConfig(int positions, String identity) { this(positions, identity, ""); }
    public CorpusTrainingConfig(int positions) { this(positions, ""); }
    public CorpusTrainingConfig forArchitecture(com.ohinteractive.seedv6.training.model.TrainingArchitecture architecture) {
        String expected = architecture.nnueFamily()
                ? com.ohinteractive.seedv6.training.nnue.NnueCorpusTargets.ID : architecture.corpusOnly()
                ? CorpusTraining.TargetPolicy.BRN3_CP_WDL_V1.identity : "";
        if (CorpusTraining.sourceOutcome(targetAdapter)
                && targetAdapter.equals(CorpusTraining.sourceOutcomeAdapter(architecture))) expected = targetAdapter;
        if (!targetAdapter.isEmpty() && !targetAdapter.equals(expected)) throw new IllegalArgumentException("Corpus adapter/architecture mismatch");
        return new CorpusTrainingConfig(positionsPerGeneration, viewIdentity, expected);
    }
    public String settings() { return targetAdapter.isEmpty() ? "|corpus-cp-v1:" + positionsPerGeneration + ":" + viewIdentity
            : "|corpus-outcome-v1:" + positionsPerGeneration + ":" + viewIdentity + ":" + targetAdapter; }
}
