package com.ohinteractive.seedv6.gui;

import java.io.*;
import java.nio.file.Path;
import java.util.Base64;
import com.ohinteractive.seedv6.training.service.*;

/** Complete editable settings, independent of the immutable generation fingerprint. Root belongs to the lineage. */
final class LineageConfiguration {
    static String encode(TrainingSettings s) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bytes)) {
            out.writeInt(2);
            out.writeInt(s.depth()); out.writeInt(s.threads()); out.writeInt(s.games());
            out.writeInt(s.openingMin()); out.writeInt(s.openingMax()); out.writeInt(s.samples());
            out.writeInt(s.minibatch()); out.writeInt(s.epochs()); out.writeInt(s.validationPairs());
            out.writeLong(s.seed()); out.writeInt(s.maximumPlies()); out.writeLong(s.maximumGenerations());
            out.writeDouble(s.brnLearningRate()); out.writeDouble(s.brn1LearningRate()); out.writeDouble(s.brn2LearningRate());
            out.writeBoolean(s.source() != null);
            if (s.source() != null) { out.writeUTF(s.source().mode().name()); out.writeUTF(s.source().generatorStore()); }
            out.writeUTF(s.generatorStore());
            out.writeBoolean(s.supervision() != null);
            if (s.supervision() != null) { out.writeUTF(s.supervision().mode().name()); out.writeDouble(s.supervision().teacherWeight()); }
            out.writeBoolean(s.runSeeds() != null);
            if (s.runSeeds() != null) { out.writeLong(s.runSeeds().masterSeed()); out.writeLong(s.runSeeds().dataSeed()); }
            out.writeBoolean(s.teacherStore() != null); if (s.teacherStore() != null) out.writeUTF(s.teacherStore());
            out.writeLong(s.maximumRunMinutes());
            out.writeUTF(s.validationMethod() == null ? "" : s.validationMethod().name());
            out.writeBoolean(s.captureConsistency() != null);
            if (s.captureConsistency() != null) out.writeDouble(s.captureConsistency().lambda());
            out.writeUTF(s.corpusRoot());
            out.writeBoolean(s.corpusTraining() != null);
            if (s.corpusTraining() != null) {
                out.writeInt(s.corpusTraining().positionsPerGeneration()); out.writeUTF(s.corpusTraining().viewIdentity());
            }
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }

    static TrainingSettings decode(String encoded, Path root, NetworkArchitecture architecture) throws IOException {
        try (var in = new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(encoded)))) {
            int version = in.readInt();
            if (version != 1 && version != 2) throw new IOException("Unsupported lineage configuration version.");
            int depth = in.readInt(), threads = in.readInt(), games = in.readInt(), min = in.readInt(), max = in.readInt();
            int samples = in.readInt(), batch = in.readInt(), epochs = in.readInt(), pairs = in.readInt();
            long seed = in.readLong(); int plies = in.readInt(); long generations = in.readLong();
            double rate = in.readDouble(), rate1 = in.readDouble(), rate2 = in.readDouble();
            var source = in.readBoolean() ? new TrainingSource(TrainingSource.Mode.valueOf(in.readUTF()), in.readUTF()) : null;
            String generator = in.readUTF();
            var supervision = in.readBoolean() ? new BrnSupervision(BrnSupervision.Mode.valueOf(in.readUTF()), in.readDouble()) : null;
            var seeds = in.readBoolean() ? new BrnRunSeeds(in.readLong(), in.readLong()) : null;
            String teacher = in.readBoolean() ? in.readUTF() : null;
            long minutes = in.readLong(); String validation = in.readUTF();
            var capture = in.readBoolean() ? new BrnCaptureConsistency(in.readDouble()) : null;
            var result = new TrainingSettings(root, depth, threads, games, min, max, samples, batch, epochs, pairs,
                    seed, plies, generations, architecture, rate, rate1, rate2, source, generator, supervision, seeds,
                    teacher, minutes, validation.isEmpty() ? null : ValidationMethod.valueOf(validation), capture);
            if (version == 2) result = result.withCorpus(in.readUTF(),
                    in.readBoolean() ? new CorpusTrainingConfig(in.readInt(), in.readUTF()) : null);
            if (in.read() != -1) throw new IOException("Trailing lineage configuration data.");
            return result;
        } catch (RuntimeException invalid) { throw new IOException("Invalid saved lineage configuration.", invalid); }
    }
    private LineageConfiguration() {}
}
