package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.training.data.DataFiles;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Uniform nested training population, with byte-identical held-out records and opaque labels. */
public final class BrnArchitectureSubset {
    @SuppressWarnings("unchecked")
    static Map<String,Object> create(Path parent, Path out, int count, long seed) throws Exception {
        if (Files.exists(out)) throw new IOException("Use a new subset directory");
        String manifestHash = BrnResearchComparison.digest(parent.resolve("manifest.json"));
        var parentManifest = DataFiles.read(parent.resolve("manifest.json"), Map.class);
        var evidence = BrnArchitectureDataAudit.scan(parent, (board, partition, index) -> {});
        int[] parentCounts = (int[]) evidence.get("counts");
        if (count < 1 || count >= parentCounts[0]) throw new IllegalArgumentException("Proper nonempty training subset required");
        var keep = new BitSet(parentCounts[0]);
        int[] permutation = BrnResearchMain.order(parentCounts[0], seed);
        for (int i = 0; i < count; i++) keep.set(permutation[i]);
        var indicesHash = DataFiles.digest();
        try (var indexBytes = new DataOutputStream(new DigestOutputStream(OutputStream.nullOutputStream(), indicesHash))) {
            for (int i = keep.nextSetBit(0); i >= 0; i = keep.nextSetBit(i + 1)) indexBytes.writeInt(i);
        }
        Files.createDirectory(out);
        var parentHash = DataFiles.digest(); var payloadHash = DataFiles.digest();
        var partitionHashes = new MessageDigest[]{DataFiles.digest(), DataFiles.digest(), DataFiles.digest()};
        var originalHeldHashes = new MessageDigest[]{DataFiles.digest(), DataFiles.digest()};
        int[] readCounts = new int[3], writeCounts = new int[3];
        try (var input = new DataInputStream(new BufferedInputStream(new DigestInputStream(
                    Files.newInputStream(parent.resolve("positions.bin")), parentHash)));
             var output = new DataOutputStream(new BufferedOutputStream(new DigestOutputStream(
                    Files.newOutputStream(out.resolve("positions.bin"), StandardOpenOption.CREATE_NEW), payloadHash)))) {
            if (input.readLong() != BrnResearchData.MAGIC || input.readInt() != parentCounts[0] || input.readInt() != parentCounts[1])
                throw new IOException("Parent header changed after audit");
            output.writeLong(BrnResearchData.MAGIC); output.writeInt(count); output.writeInt(parentCounts[1]);
            byte[] record = new byte[57];
            for (int row = 0; row < Arrays.stream(parentCounts).sum(); row++) {
                input.readFully(record); int partition = record[56] & 255;
                if (partition > 2) throw new IOException("Invalid partition byte");
                int index = readCounts[partition]++;
                if (partition != 0) originalHeldHashes[partition - 1].update(record);
                if (partition != 0 || keep.get(index)) {
                    output.write(record); partitionHashes[partition].update(record); writeCounts[partition]++;
                }
            }
            if (input.read() != -1 || !Arrays.equals(readCounts, parentCounts)) throw new IOException("Parent record counts changed");
        }
        String observedParent = HexFormat.of().formatHex(parentHash.digest());
        if (!observedParent.equals(evidence.get("payloadSha256"))) throw new IOException("Parent changed during subset copy");
        if (!Arrays.equals(writeCounts, new int[]{count, parentCounts[1], parentCounts[2]})) throw new IOException("Subset count mismatch");
        String[] hashes = Arrays.stream(partitionHashes).map(h -> HexFormat.of().formatHex(h.digest())).toArray(String[]::new);
        for (int i = 0; i < 2; i++)
            if (!hashes[i + 1].equals(HexFormat.of().formatHex(originalHeldHashes[i].digest()))) throw new IOException("Held-out bytes changed");
        if (!manifestHash.equals(BrnResearchComparison.digest(parent.resolve("manifest.json"))))
            throw new IOException("Parent manifest changed during subset copy");
        var report = new LinkedHashMap<String,Object>();
        report.put("schema", "seedv6-brn-research-nested-subset-v1"); report.put("parent", evidence);
        report.put("parentManifestSha256", manifestHash);
        report.put("counts", writeCounts); report.put("selectionSeed", seed);
        report.put("selection", "First COUNT entries of seeded Fisher-Yates over parent training ordinals; retain chosen records in original order; no label values decoded; all held-out records copied verbatim");
        report.put("selectedTrainingOrdinalsSha256", HexFormat.of().formatHex(indicesHash.digest()));
        report.put("scope", "Nested training-data ablation, intentionally overlapping parent; same validation/test populations, not independent replication");
        report.put("trainingSha256", hashes[0]); report.put("validationSha256", hashes[1]); report.put("testSha256", hashes[2]);
        report.put("payloadSha256", HexFormat.of().formatHex(payloadHash.digest()));
        for (String key : List.of("source", "rawStart", "rawEndExclusive", "split"))
            if (parentManifest.containsKey(key)) report.put(key, parentManifest.get(key));
        DataFiles.write(out.resolve("manifest.json"), report);
        // Independent reader validates the new payload and every partition without opening labels.
        BrnArchitectureDataAudit.scan(out, (board, partition, index) -> {});
        return report;
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("PARENT_DATA NEW_OUT TRAIN_COUNT SUBSET_SEED");
        System.out.println(DataFiles.JSON.toJson(create(Path.of(args[0]), Path.of(args[1]), Integer.parseInt(args[2]), Long.parseLong(args[3]))));
    }
    private BrnArchitectureSubset() {}
}
