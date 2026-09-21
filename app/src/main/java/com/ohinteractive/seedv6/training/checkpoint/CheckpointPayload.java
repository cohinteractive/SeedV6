package com.ohinteractive.seedv6.training.checkpoint;

import java.io.IOException;
import java.nio.file.*;
import com.ohinteractive.seedv6.training.nnue.AdamHyperparameters;
import static com.ohinteractive.seedv6.training.checkpoint.CheckpointManifest.*;

/** Compact V1 sidecars. Existing immutable manifests/identities/codecs are unchanged. */
final class CheckpointPayload {
    static final String CONFIG = "training-metadata.bin";
    static final String PRUNED = "payload-pruned.bin";

    static byte[] configuration(CheckpointManifest manifest, AdamHyperparameters hp) throws IOException {
        return SmallRecord.encode("training-metadata-v1", out -> {
            out.writeUTF(manifest.id()); out.writeLong(manifest.optimizerStep());
            out.writeDouble(hp.learningRate()); out.writeDouble(hp.beta1());
            out.writeDouble(hp.beta2()); out.writeDouble(hp.epsilon());
        });
    }

    static AdamHyperparameters readConfiguration(Path directory, CheckpointManifest manifest) throws IOException {
        regular(directory.resolve(CONFIG));
        return SmallRecord.read(directory.resolve(CONFIG), "training-metadata-v1", in -> {
            if (!in.readUTF().equals(manifest.id()) || in.readLong() != manifest.optimizerStep())
                throw new IOException("Training metadata/checkpoint mismatch.");
            return new AdamHyperparameters(in.readDouble(), in.readDouble(), in.readDouble(), in.readDouble());
        });
    }

    static byte[] pruningRecord(Path directory, CheckpointManifest manifest, long completed) throws IOException {
        return SmallRecord.encode("payload-pruned-v1", out -> {
            out.writeUTF(manifest.id()); out.writeLong(completed);
            out.writeUTF(SmallRecord.hash(directory.resolve(CONFIG)));
        });
    }

    /** A marker is authoritative only after its identity, checksum and configuration binding validate. */
    static boolean pruned(Path directory, CheckpointManifest manifest) throws IOException {
        Path marker = directory.resolve(PRUNED);
        if (!Files.exists(marker, LinkOption.NOFOLLOW_LINKS)) return false;
        regular(marker);
        readConfiguration(directory, manifest);
        SmallRecord.read(marker, "payload-pruned-v1", in -> {
            if (!in.readUTF().equals(manifest.id()) || in.readLong() < manifest.generation()
                    || !in.readUTF().equals(SmallRecord.hash(directory.resolve(CONFIG))))
                throw new IOException("Invalid payload-pruned identity/configuration binding.");
            return true;
        });
        return true;
    }

    static void regular(Path path) throws IOException {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Missing/non-regular checkpoint artifact: " + path);
    }

    /** Presence/length is required even for metadata ancestry; absence without a marker is corruption. */
    static void requireMaterialized(Path directory, CheckpointManifest manifest) throws IOException {
        regular(directory.resolve(NETWORK_FILE)); regular(directory.resolve(TRAINING_FILE));
        if (Files.size(directory.resolve(NETWORK_FILE)) != manifest.networkBytes()
                || Files.size(directory.resolve(TRAINING_FILE)) != manifest.trainingBytes())
            throw new IOException("Checkpoint artifact length mismatch.");
        if (Files.exists(directory.resolve(CONFIG), LinkOption.NOFOLLOW_LINKS)) readConfiguration(directory, manifest);
    }

    private CheckpointPayload() {}
}
