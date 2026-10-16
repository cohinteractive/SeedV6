package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.training.data.DataFiles;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Strengthens a fresh opening audit against recorded corpus populations without decoding labels. */
public final class BrnArchitectureOpeningDataAudit {
    @SuppressWarnings("unchecked")
    static Map<String,Object> audit(Path openingAudit, String expectedSha, List<Path> datasets) throws Exception {
        if (!BrnResearchComparison.digest(openingAudit).equals(expectedSha)) throw new IOException("Base opening audit changed");
        var base = DataFiles.read(openingAudit, Map.class);
        if (!"brn-architecture-opening-audit-v1".equals(base.get("schema")) || !Boolean.TRUE.equals(base.get("fresh")))
            throw new IOException("A successful native opening preflight is required first");
        if (datasets.isEmpty()) throw new IllegalArgumentException("Recorded research datasets required");
        var openings = (Map<String,Map<String,Object>>) base.get("openings");
        var groups = new HashMap<String,String>();
        for (var entry : openings.entrySet()) {
            String group = BrnResearchData.groupKey(Board.fromFen((String) entry.getValue().get("fen")));
            if (!group.equals(entry.getValue().get("geometryGroup")) || groups.put(group, entry.getKey()) != null)
                throw new IOException("Base geometry mismatch or repeated opening");
        }
        var reports = new ArrayList<Object>(); var collisions = new ArrayList<Object>();
        for (Path dataset : datasets) {
            String manifestHash = BrnResearchComparison.digest(dataset.resolve("manifest.json"));
            var evidence = BrnArchitectureDataAudit.scan(dataset, (board, partition, index) -> {
                String opening = groups.get(BrnResearchData.groupKey(board));
                if (opening != null) collisions.add(Map.of("openingIndex", opening, "dataset", dataset.toString(),
                        "partition", partition, "partitionIndex", index));
            });
            if (!manifestHash.equals(BrnResearchComparison.digest(dataset.resolve("manifest.json"))))
                throw new IOException("Dataset manifest changed during opening audit");
            evidence.put("manifestSha256", manifestHash); reports.add(evidence);
        }
        if (!BrnResearchComparison.digest(openingAudit).equals(expectedSha)) throw new IOException("Base audit changed during dataset scan");
        var result = new LinkedHashMap<String,Object>(base);
        result.put("schema", "brn-architecture-opening-data-audit-v1");
        result.put("baseOpeningAudit", Map.of("path", openingAudit.toString(), "sha256", expectedSha));
        result.put("recordedDatasets", reports); result.put("recordedDatasetCollisions", collisions);
        result.put("fresh", collisions.isEmpty());
        result.put("scope", "Prior recorded openings and warmup checks inherited from base; additionally excludes geometry aliases in all explicitly listed research train/validation/test records. Labels skipped, not decoded. Does not establish absence from unknown native historical training data or all possible past games.");
        return result;
    }
    public static void main(String[] args) throws Exception {
        if (args.length < 4) throw new IllegalArgumentException("BASE_OPENING_AUDIT EXPECTED_SHA256 NEW_OUT DATASET...");
        Path out = Path.of(args[2]); if (Files.exists(out)) throw new IOException("Use a new audit directory");
        var datasets = new ArrayList<Path>(); for (int i = 3; i < args.length; i++) datasets.add(Path.of(args[i]));
        var result = audit(Path.of(args[0]), args[1], datasets);
        Files.createDirectory(out); DataFiles.write(out.resolve("result.json"), result);
        System.out.println(DataFiles.JSON.toJson(result));
        if (!Boolean.TRUE.equals(result.get("fresh"))) throw new IOException("Opening overlaps recorded research data; prospectively declare another sample before playing");
    }
    private BrnArchitectureOpeningDataAudit() {}
}
