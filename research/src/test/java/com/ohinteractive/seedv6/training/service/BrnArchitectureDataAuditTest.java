package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.training.data.DataFiles;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class BrnArchitectureDataAuditTest {
    @TempDir Path temp;
    Path data(String name,List<long[]> boards,int ignoredLabel) throws Exception {
        Path path=temp.resolve(name);Files.createDirectory(path);int[] counts=new int[3];
        for(var board:boards)counts[BrnResearchData.split(board)]++;
        assertEquals(counts[1],counts[2]);
        try(var out=new DataOutputStream(Files.newOutputStream(path.resolve("positions.bin")))) {
            out.writeLong(BrnResearchData.MAGIC);out.writeInt(counts[0]);out.writeInt(counts[1]);
            for(var board:boards){for(long word:board)out.writeLong(word);out.writeInt(ignoredLabel);out.writeInt(~ignoredLabel);out.writeByte(BrnResearchData.split(board));}
        }
        DataFiles.write(path.resolve("manifest.json"),Map.of("payloadSha256",BrnResearchComparison.digest(path.resolve("positions.bin"))));return path;
    }
    @Test void detectsEquivalentHeldOutPlacementsWithoutReadingLabelsAndChecksIntegrity()throws Exception {
        var byPartition=List.of(new ArrayList<long[]>(),new ArrayList<long[]>(),new ArrayList<long[]>());
        for(int square=8;square<56;square++) {
            long[] board=Board.fromFen("4k3/8/8/8/8/8/8/4K3 w - - 0 1");board[0]|=1L<<square;
            byPartition.get(BrnResearchData.split(board)).add(board);
        }
        var originals=List.of(byPartition.get(0).get(0),byPartition.get(1).get(0),byPartition.get(2).get(0));
        var altered=new ArrayList<long[]>();for(var board:originals){var copy=board.clone();copy[4]^=Board.PLAYER_BIT;copy[5]^=123;altered.add(copy);}
        Path prior=data("prior",originals,123),fresh=data("fresh",altered,Integer.MIN_VALUE);
        var report=BrnArchitectureDataAudit.audit(fresh,List.of(prior));
        assertArrayEquals(new int[]{1,1,1},(int[])report.get("freshRowsSeenEarlier"));
        int[][] overlap=(int[][])report.get("oldPartitionToFreshPartitionOverlapRows");
        for(int old=0;old<3;old++)for(int next=0;next<3;next++)assertEquals(old==next?1:0,overlap[old][next]);
        assertEquals(3,report.get("freshUniqueGroups"));
        var bytes=Files.readAllBytes(fresh.resolve("positions.bin"));bytes[64]^=1;Files.write(fresh.resolve("positions.bin"),bytes);
        assertThrows(IOException.class,()->BrnArchitectureDataAudit.audit(fresh,List.of(prior)));
    }
}
