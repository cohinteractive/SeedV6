package com.ohinteractive.seedv6.training.checkpoint;

import java.io.*;
import com.ohinteractive.seedv6.training.validation.HeldOutLoss;
import com.ohinteractive.seedv6.training.service.BrnSupervision;

/** Self-contained audit evidence; lower prediction loss is not a game-strength claim. */
public record BootstrapEvidence(String generatorStore, String generatorId, String generatorHash,
                                String dataHash, long splitSeed, int trainingSamples, int trainingGames,
                                int heldOutGames, HeldOutLoss.Comparison comparison, BrnSupervision supervision,
                                HeldOutLoss.Comparison wdlLoss, HeldOutLoss.Comparison teacherLoss) {
    public BootstrapEvidence {
        if (generatorStore == null || generatorStore.isBlank() || trainingSamples < 2 || trainingGames < 2 || heldOutGames < 2)
            throw new IllegalArgumentException("Invalid bootstrap validation evidence.");
        CheckpointManifest.requireId(generatorId); SmallRecord.requireHash(generatorHash); SmallRecord.requireHash(dataHash);
        java.util.Objects.requireNonNull(comparison); java.util.Objects.requireNonNull(supervision);
        if (supervision.blended() ? wdlLoss == null || teacherLoss == null
                || wdlLoss.samples() != comparison.samples() || teacherLoss.samples() != comparison.samples()
                : wdlLoss != null || teacherLoss != null) throw new IllegalArgumentException("Invalid component loss evidence.");
    }
    public BootstrapEvidence(String generatorStore, String generatorId, String generatorHash,
                             String dataHash, long splitSeed, int trainingSamples, int trainingGames,
                             int heldOutGames, HeldOutLoss.Comparison comparison) {
        this(generatorStore, generatorId, generatorHash, dataHash, splitSeed, trainingSamples, trainingGames,
                heldOutGames, comparison, BrnSupervision.WDL, null, null);
    }
    public static BootstrapEvidence create(BootstrapPlan plan, BootstrapData data, HeldOutLoss.Comparison comparison) throws IOException {
        return create(plan, data, comparison, null, null);
    }
    public static BootstrapEvidence create(BootstrapPlan plan, BootstrapData data, HeldOutLoss.Comparison comparison,
            HeldOutLoss.Comparison wdlLoss, HeldOutLoss.Comparison teacherLoss) throws IOException {
        if (!data.planHash().equals(plan.hash()) || comparison.samples() != data.partition().heldOut().size())
            throw new IOException("Bootstrap validation input mismatch.");
        return new BootstrapEvidence(plan.source().generatorStore(), plan.generatorId(), plan.generatorHash(), data.hash(),
                plan.splitSeed(), data.partition().training().size(), data.partition().trainingGames().size(),
                data.partition().heldOutGames().size(), comparison, plan.supervision(), wdlLoss, teacherLoss);
    }
    void write(DataOutputStream out) throws IOException {
        out.writeUTF(generatorStore); out.writeUTF(generatorId); out.writeUTF(generatorHash); out.writeUTF(dataHash);
        out.writeLong(splitSeed); out.writeInt(trainingSamples); out.writeInt(trainingGames); out.writeInt(heldOutGames);
        out.writeInt(comparison.samples()); out.writeDouble(comparison.candidateLoss()); out.writeDouble(comparison.bestLoss());
        if (supervision.blended()) {
            supervision.write(out);
            out.writeDouble(wdlLoss.candidateLoss()); out.writeDouble(wdlLoss.bestLoss());
            out.writeDouble(teacherLoss.candidateLoss()); out.writeDouble(teacherLoss.bestLoss());
        }
    }
    static BootstrapEvidence read(DataInputStream in, boolean blended) throws IOException {
        var legacy = new BootstrapEvidence(in.readUTF(), in.readUTF(), in.readUTF(), in.readUTF(), in.readLong(),
                in.readInt(), in.readInt(), in.readInt(), new HeldOutLoss.Comparison(in.readInt(), in.readDouble(), in.readDouble()));
        if (!blended) return legacy;
        var supervision = BrnSupervision.read(in);
        if (!supervision.blended()) throw new IOException("Invalid blended validation objective.");
        int samples = legacy.comparison().samples();
        return new BootstrapEvidence(legacy.generatorStore(), legacy.generatorId(), legacy.generatorHash(), legacy.dataHash(),
                legacy.splitSeed(), legacy.trainingSamples(), legacy.trainingGames(), legacy.heldOutGames(), legacy.comparison(),
                supervision, new HeldOutLoss.Comparison(samples, in.readDouble(), in.readDouble()),
                new HeldOutLoss.Comparison(samples, in.readDouble(), in.readDouble()));
    }
}
