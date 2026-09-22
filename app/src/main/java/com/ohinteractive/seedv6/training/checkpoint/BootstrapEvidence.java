package com.ohinteractive.seedv6.training.checkpoint;

import java.io.*;
import com.ohinteractive.seedv6.training.validation.HeldOutLoss;

/** Self-contained audit evidence; lower prediction loss is not a game-strength claim. */
public record BootstrapEvidence(String generatorStore, String generatorId, String generatorHash,
                                String dataHash, long splitSeed, int trainingSamples, int trainingGames,
                                int heldOutGames, HeldOutLoss.Comparison comparison) {
    public BootstrapEvidence {
        if (generatorStore == null || generatorStore.isBlank() || trainingSamples < 2 || trainingGames < 2 || heldOutGames < 2)
            throw new IllegalArgumentException("Invalid bootstrap validation evidence.");
        CheckpointManifest.requireId(generatorId); SmallRecord.requireHash(generatorHash); SmallRecord.requireHash(dataHash);
        java.util.Objects.requireNonNull(comparison);
    }
    public static BootstrapEvidence create(BootstrapPlan plan, BootstrapData data, HeldOutLoss.Comparison comparison) throws IOException {
        if (!data.planHash().equals(plan.hash()) || comparison.samples() != data.partition().heldOut().size())
            throw new IOException("Bootstrap validation input mismatch.");
        return new BootstrapEvidence(plan.source().generatorStore(), plan.generatorId(), plan.generatorHash(), data.hash(),
                plan.splitSeed(), data.partition().training().size(), data.partition().trainingGames().size(),
                data.partition().heldOutGames().size(), comparison);
    }
    void write(DataOutputStream out) throws IOException {
        out.writeUTF(generatorStore); out.writeUTF(generatorId); out.writeUTF(generatorHash); out.writeUTF(dataHash);
        out.writeLong(splitSeed); out.writeInt(trainingSamples); out.writeInt(trainingGames); out.writeInt(heldOutGames);
        out.writeInt(comparison.samples()); out.writeDouble(comparison.candidateLoss()); out.writeDouble(comparison.bestLoss());
    }
    static BootstrapEvidence read(DataInputStream in) throws IOException {
        return new BootstrapEvidence(in.readUTF(), in.readUTF(), in.readUTF(), in.readUTF(), in.readLong(),
                in.readInt(), in.readInt(), in.readInt(), new HeldOutLoss.Comparison(in.readInt(), in.readDouble(), in.readDouble()));
    }
}
