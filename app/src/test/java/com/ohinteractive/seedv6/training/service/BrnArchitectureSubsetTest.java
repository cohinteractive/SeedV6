package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.training.data.DataFiles;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class BrnArchitectureSubsetTest {
    @TempDir Path temp;
    Path fixture(String name, int label) throws Exception {
        var partitions = List.of(new ArrayList<long[]>(), new ArrayList<long[]>(), new ArrayList<long[]>());
        for (int square = 8; square < 56; square++) {
            var board = Board.fromFen("4k3/8/8/8/8/8/8/4K3 w - - 0 1"); board[0] |= 1L << square;
            partitions.get(BrnResearchData.split(board)).add(board);
        }
        Path path = temp.resolve(name); Files.createDirectory(path);
        try (var out = new DataOutputStream(Files.newOutputStream(path.resolve("positions.bin")))) {
            out.writeLong(BrnResearchData.MAGIC); out.writeInt(16); out.writeInt(1);
            // Deliberately interleave partitions: the copier must use training ordinal, not absolute row.
            for (int index = 0; index < 16; index++) {
                for (int partition = 0; partition < 3; partition++) if (partition == 0 || index == 3) {
                    var board = partitions.get(partition).get(partition == 0 ? index : 0);
                    for (long word : board) out.writeLong(word);
                    out.writeInt(label + index); out.writeInt(17); out.writeByte(partition);
                }
            }
        }
        DataFiles.write(path.resolve("manifest.json"), Map.of("payloadSha256", BrnResearchComparison.digest(path.resolve("positions.bin"))));
        return path;
    }
    @Test void nestedSelectionIsLabelIndependentAndHeldOutBytesAreUnchanged() throws Exception {
        var selected = new ArrayList<List<String>>();
        for (int label : new int[]{-1234, 5678}) {
            var parent = fixture("parent" + label, label); var out = temp.resolve("subset" + label);
            String original = BrnResearchComparison.digest(parent.resolve("positions.bin"));
            var report = BrnArchitectureSubset.create(parent, out, 8, 49217);
            var full = BrnResearchData.read(parent, true); var sub = BrnResearchData.read(out, true);
            assertEquals(8, sub.training().size()); assertEquals(1, sub.validation().size()); assertEquals(1, sub.test().size());
            assertArrayEquals(full.validation().get(0).board(), sub.validation().get(0).board());
            assertEquals(full.validation().get(0).cp(), sub.validation().get(0).cp());
            assertArrayEquals(full.test().get(0).board(), sub.test().get(0).board());
            assertEquals(full.test().get(0).cp(), sub.test().get(0).cp());
            int[] order = BrnResearchMain.order(16, 49217); var keep = new TreeSet<Integer>();
            for (int i = 0; i < 8; i++) keep.add(order[i]); int j = 0;
            for (int index : keep) {
                assertArrayEquals(full.training().get(index).board(), sub.training().get(j).board());
                assertEquals(full.training().get(index).cp(), sub.training().get(j++).cp());
            }
            selected.add(sub.training().stream().map(e -> BrnResearchData.groupKey(e.board())).toList());
            assertEquals(original, BrnResearchComparison.digest(parent.resolve("positions.bin")));
            var second = temp.resolve("replay" + label); var replay = BrnArchitectureSubset.create(parent, second, 8, 49217);
            assertEquals(report.get("payloadSha256"), replay.get("payloadSha256"));
            assertThrows(IOException.class, () -> BrnArchitectureSubset.create(parent, out, 8, 49217));
            assertThrows(IllegalArgumentException.class, () -> BrnArchitectureSubset.create(parent, temp.resolve("invalid" + label), 16, 49217));
        }
        assertEquals(selected.get(0), selected.get(1));
    }
}
