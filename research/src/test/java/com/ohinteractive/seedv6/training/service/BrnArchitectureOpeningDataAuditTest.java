package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Fen;
import com.ohinteractive.seedv6.training.data.DataFiles;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class BrnArchitectureOpeningDataAuditTest {
    @TempDir Path temp;
    @Test @SuppressWarnings("unchecked") void findsStmAliasesInSealedRecordsWithoutEvaluatingTheirLabels() throws Exception {
        var partitions = List.of(new ArrayList<long[]>(), new ArrayList<long[]>(), new ArrayList<long[]>());
        for (int square = 8; square < 56; square++) {
            var board = Board.fromFen("4k3/8/8/8/8/8/8/4K3 w - - 0 1"); board[0] |= 1L << square;
            partitions.get(BrnResearchData.split(board)).add(board);
        }
        Path data = temp.resolve("data"); Files.createDirectory(data);
        try (var out = new DataOutputStream(Files.newOutputStream(data.resolve("positions.bin")))) {
            out.writeLong(BrnResearchData.MAGIC); out.writeInt(1); out.writeInt(1);
            for (int partition = 0; partition < 3; partition++) {
                for (long word : partitions.get(partition).get(0)) out.writeLong(word);
                out.writeInt(Integer.MIN_VALUE); out.writeInt(Integer.MIN_VALUE); out.writeByte(partition);
            }
        }
        DataFiles.write(data.resolve("manifest.json"), Map.of("payloadSha256", BrnResearchComparison.digest(data.resolve("positions.bin"))));
        Path base = temp.resolve("base.json"); var board = partitions.get(2).get(0).clone(); board[4] ^= Board.PLAYER_BIT;
        DataFiles.write(base, Map.of("schema", "brn-architecture-opening-audit-v1", "fresh", true, "openings",
                Map.of("900", Map.of("fen", Fen.fromBoard(board), "geometryGroup", BrnResearchData.groupKey(board)))));
        String hash = BrnResearchComparison.digest(base);
        var report = BrnArchitectureOpeningDataAudit.audit(base, hash, List.of(data));
        assertEquals(false, report.get("fresh"));
        var collisions = (List<Map<String,Object>>) report.get("recordedDatasetCollisions");
        assertEquals(1, collisions.size()); assertEquals(2, collisions.get(0).get("partition"));
        assertEquals("900", collisions.get(0).get("openingIndex"));
        assertThrows(IOException.class, () -> BrnArchitectureOpeningDataAudit.audit(base, "changed", List.of(data)));
        var other = Board.startingPosition();
        DataFiles.write(base, Map.of("schema", "brn-architecture-opening-audit-v1", "fresh", true, "openings",
                Map.of("900", Map.of("fen", Fen.fromBoard(other), "geometryGroup", BrnResearchData.groupKey(other)))));
        report = BrnArchitectureOpeningDataAudit.audit(base, BrnResearchComparison.digest(base), List.of(data));
        assertEquals(true, report.get("fresh"));
    }
}
