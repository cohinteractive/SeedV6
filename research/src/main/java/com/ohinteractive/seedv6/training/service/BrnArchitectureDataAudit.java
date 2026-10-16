package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.training.data.DataFiles;
import java.io.*;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.util.*;

/** Geometry-only overlap audit. Label bytes are hashed/skipped, never decoded or scored. */
public final class BrnArchitectureDataAudit {
    interface Visitor { void accept(long[] board, int partition, int partitionIndex); }

    @SuppressWarnings("unchecked")
    static Map<String,Object> scan(Path directory, Visitor visitor) throws Exception {
        var manifest=DataFiles.read(directory.resolve("manifest.json"),Map.class);
        var digest=DataFiles.digest();int[] counts=new int[3];
        try(var in=new DataInputStream(new BufferedInputStream(new DigestInputStream(
                Files.newInputStream(directory.resolve("positions.bin")),digest)))) {
            if(in.readLong()!=BrnResearchData.MAGIC)throw new IOException("Dataset format");
            int training=in.readInt(),held=in.readInt();
            if(training<0||held<0)throw new IOException("Negative counts");
            int total=Math.addExact(training,Math.multiplyExact(2,held));byte[] ignoredLabels=new byte[8];
            for(int i=0;i<total;i++) {
                long[] board=new long[6];for(int j=0;j<6;j++)board[j]=in.readLong();
                in.readFully(ignoredLabels);int partition=in.readUnsignedByte();
                if(partition>2||BrnResearchData.split(board)!=partition)throw new IOException("Partition mismatch");
                visitor.accept(board,partition,counts[partition]++);
            }
            if(in.read()!=-1||counts[0]!=training||counts[1]!=held||counts[2]!=held)throw new IOException("Dataset length/counts");
        }
        String hash=HexFormat.of().formatHex(digest.digest());
        if(!hash.equals(manifest.get("payloadSha256")))throw new IOException("Dataset digest mismatch");
        var result=new LinkedHashMap<String,Object>();result.put("path",directory.toString());
        result.put("payloadSha256",hash);result.put("counts",counts);
        for(String key:List.of("source","rawStart","rawEndExclusive"))if(manifest.containsKey(key))result.put(key,manifest.get(key));
        return result;
    }

    static Map<String,Object> audit(Path fresh,List<Path> earlier) throws Exception {
        var prior=new HashMap<String,Integer>();var manifests=new ArrayList<Object>();
        for(Path path:earlier)manifests.add(scan(path,(board,partition,index)->
                prior.merge(BrnResearchData.groupKey(board),1<<partition,(a,b)->a|b)));
        var seen=new HashSet<String>();int[] repeatedWithinFresh=new int[3],overlapRows=new int[3];
        int[][] oldToNew=new int[3][3];
        var overlapIndices=List.of(new ArrayList<Integer>(),new ArrayList<Integer>(),new ArrayList<Integer>());
        var freshManifest=scan(fresh,(board,partition,index)-> {
            String key=BrnResearchData.groupKey(board);
            if(!seen.add(key))repeatedWithinFresh[partition]++;
            int mask=prior.getOrDefault(key,0);
            if(mask!=0){overlapRows[partition]++;overlapIndices.get(partition).add(index);}
            for(int old=0;old<3;old++)if((mask&(1<<old))!=0)oldToNew[old][partition]++;
        });
        var result=new LinkedHashMap<String,Object>();
        result.put("schema","brn-architecture-geometry-audit-v1");
        result.put("method","Placement groups include colour/rank reflection, file reflection and STM variants; CP/material labels are skipped, never decoded; all payloads verified");
        result.put("partitionOrder",List.of("training","validation","sealedTest"));
        result.put("earlierDatasets",manifests);result.put("freshDataset",freshManifest);
        result.put("earlierUniqueGroups",prior.size());result.put("freshUniqueGroups",seen.size());
        result.put("freshRepeatedGroupRows",repeatedWithinFresh);result.put("freshRowsSeenEarlier",overlapRows);
        result.put("oldPartitionToFreshPartitionOverlapRows",oldToNew);
        result.put("freshOverlapPartitionIndices",overlapIndices);
        return result;
    }

    public static void main(String[] args)throws Exception {
        if(args.length<3)throw new IllegalArgumentException("NEW_OUT FRESH_DATA EARLIER_DATA...");
        Path out=Path.of(args[0]);if(Files.exists(out))throw new IOException("Use a fresh audit directory");
        var prior=new ArrayList<Path>();for(int i=2;i<args.length;i++)prior.add(Path.of(args[i]));
        var result=audit(Path.of(args[1]),prior);Files.createDirectory(out);
        DataFiles.write(out.resolve("result.json"),result);
        System.out.println(DataFiles.JSON.toJson(result));
    }
    private BrnArchitectureDataAudit(){}
}
