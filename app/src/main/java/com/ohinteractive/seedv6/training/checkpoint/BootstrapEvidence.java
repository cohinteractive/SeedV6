package com.ohinteractive.seedv6.training.checkpoint;

import java.io.*;
import com.ohinteractive.seedv6.training.validation.HeldOutLoss;
import com.ohinteractive.seedv6.training.service.BrnSupervision;

/** Self-contained audit evidence; lower prediction loss is not a game-strength claim. */
public record BootstrapEvidence(String generatorStore, String generatorId, String generatorHash,
                                String dataHash, long splitSeed, int trainingSamples, int trainingGames,
                                int heldOutGames, HeldOutLoss.Comparison comparison, BrnSupervision supervision,
                                HeldOutLoss.Comparison wdlLoss, HeldOutLoss.Comparison teacherLoss,
                                com.ohinteractive.seedv6.training.service.TrainingSource.Mode generatorMode,
                                String teacherStore, String teacherId, String teacherHash, boolean separated) {
    public BootstrapEvidence {
        if (trainingSamples < 2 || trainingGames < 2 || heldOutGames < 2)
            throw new IllegalArgumentException("Invalid bootstrap validation evidence.");
        java.util.Objects.requireNonNull(generatorMode); java.util.Objects.requireNonNull(supervision);
        if (!separated && (generatorMode != com.ohinteractive.seedv6.training.service.TrainingSource.Mode.NNUE_BOOTSTRAP
                || supervision.blended() && (!teacherStore.equals(generatorStore) || !teacherId.equals(generatorId) || !teacherHash.equals(generatorHash))))
            throw new IllegalArgumentException("Legacy evidence requires the historical NNUE dual-role pin.");
        BootstrapPlan.requirePin(generatorMode == com.ohinteractive.seedv6.training.service.TrainingSource.Mode.NNUE_BOOTSTRAP,
                generatorStore, generatorId, generatorHash);
        BootstrapPlan.requirePin(supervision.blended(), teacherStore, teacherId, teacherHash);
        SmallRecord.requireHash(dataHash);
        java.util.Objects.requireNonNull(comparison); java.util.Objects.requireNonNull(supervision);
        if (supervision.blended() ? wdlLoss == null || teacherLoss == null
                || wdlLoss.samples() != comparison.samples() || teacherLoss.samples() != comparison.samples()
                : wdlLoss != null || teacherLoss != null) throw new IllegalArgumentException("Invalid component loss evidence.");
    }
    public BootstrapEvidence(String generatorStore, String generatorId, String generatorHash,
            String dataHash, long splitSeed, int trainingSamples, int trainingGames, int heldOutGames,
            HeldOutLoss.Comparison comparison, BrnSupervision supervision,
            HeldOutLoss.Comparison wdlLoss, HeldOutLoss.Comparison teacherLoss) {
        this(generatorStore, generatorId, generatorHash, dataHash, splitSeed, trainingSamples, trainingGames,
                heldOutGames, comparison, supervision, wdlLoss, teacherLoss,
                com.ohinteractive.seedv6.training.service.TrainingSource.Mode.NNUE_BOOTSTRAP,
                supervision.blended() ? generatorStore : "", supervision.blended() ? generatorId : "",
                supervision.blended() ? generatorHash : "", false);
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
                data.partition().heldOutGames().size(), comparison, plan.supervision(), wdlLoss, teacherLoss,
                plan.source().mode(), plan.teacherStore(), plan.teacherId(), plan.teacherHash(), plan.version() >= 3);
    }
    void write(DataOutputStream out) throws IOException {
        if (separated) { out.writeUTF(generatorMode.name()); supervision.write(out); }
        out.writeUTF(generatorStore); out.writeUTF(generatorId); out.writeUTF(generatorHash); out.writeUTF(dataHash);
        out.writeLong(splitSeed); out.writeInt(trainingSamples); out.writeInt(trainingGames); out.writeInt(heldOutGames);
        out.writeInt(comparison.samples()); out.writeDouble(comparison.candidateLoss()); out.writeDouble(comparison.bestLoss());
        if (supervision.blended()) {
            if (!separated) supervision.write(out);
            out.writeDouble(wdlLoss.candidateLoss()); out.writeDouble(wdlLoss.bestLoss());
            out.writeDouble(teacherLoss.candidateLoss()); out.writeDouble(teacherLoss.bestLoss());
        }
        if (separated) { out.writeUTF(teacherStore); out.writeUTF(teacherId); out.writeUTF(teacherHash); }
    }
    static BootstrapEvidence read(DataInputStream in, boolean blended) throws IOException { return read(in, blended, false); }
    static BootstrapEvidence read(DataInputStream in, boolean blended, boolean separated) throws IOException {
        var mode = separated ? com.ohinteractive.seedv6.training.service.TrainingSource.Mode.valueOf(in.readUTF())
                : com.ohinteractive.seedv6.training.service.TrainingSource.Mode.NNUE_BOOTSTRAP;
        BrnSupervision supervision = separated ? BrnSupervision.read(in) : BrnSupervision.WDL;
        String store = in.readUTF(), id = in.readUTF(), hash = in.readUTF(), data = in.readUTF();
        long seed = in.readLong(); int samples = in.readInt(), games = in.readInt(), held = in.readInt();
        var comparison = new HeldOutLoss.Comparison(in.readInt(), in.readDouble(), in.readDouble());
        if (!separated && blended) {
            supervision = BrnSupervision.read(in);
            if (!supervision.blended()) throw new IOException("Invalid blended validation objective.");
        }
        HeldOutLoss.Comparison wdl = null, teacher = null;
        if (supervision.blended()) {
            wdl = new HeldOutLoss.Comparison(comparison.samples(), in.readDouble(), in.readDouble());
            teacher = new HeldOutLoss.Comparison(comparison.samples(), in.readDouble(), in.readDouble());
        }
        if (!separated) return new BootstrapEvidence(store, id, hash, data, seed, samples, games, held,
                comparison, supervision, wdl, teacher);
        return new BootstrapEvidence(store, id, hash, data, seed, samples, games, held, comparison, supervision,
                wdl, teacher, mode, in.readUTF(), in.readUTF(), in.readUTF(), true);
    }
}
