package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Fen;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.training.validation.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Prospective opening geometry/freshness audit; contains no model or outcome selection. */
public final class BrnArchitectureOpeningAudit {
    static ValidationArena.Opening opening(long seed,int index) {
        var board=Board.startingPosition();var config=new ValidationConfig(1,seed,6,10,1,1,NnueScoreMapping.V1,1024);
        return ValidationArena.opening(board,GameHistory.initial(board),config,index);
    }
    static Map<String,Object> audit(long seed,int first,int count,Set<String> prior) {
        if(first<0||count<1||count>256||first>Integer.MAX_VALUE-count)throw new IllegalArgumentException("Opening bounds");
        var earlier=new HashSet<>(prior);for(int i=0;i<8;i++)earlier.add(BrnResearchData.groupKey(opening(190413,i).board()));
        var seen=new HashMap<String,Integer>();var rows=new LinkedHashMap<String,Object>();
        var overlaps=new ArrayList<Integer>();var duplicates=new ArrayList<List<Integer>>();var invalid=new ArrayList<Integer>();
        for(int i=first;i<first+count;i++) {
            var opening=opening(seed,i);var board=opening.board();String group=BrnResearchData.groupKey(board);
            if(earlier.contains(group))overlaps.add(i);Integer before=seen.putIfAbsent(group,i);if(before!=null)duplicates.add(List.of(before,i));
            boolean active=opening.newGame(1024).active();int plies=opening.randomizedPlies();if(!active||plies<6||plies>10)invalid.add(i);
            rows.put(Integer.toString(i),Map.of("identity",opening.identity(),"fen",Fen.fromBoard(board),"geometryGroup",group,"randomizedPlies",plies,"active",active));
        }
        var result=new LinkedHashMap<String,Object>();result.put("schema","brn-architecture-opening-audit-v1");
        result.put("seed",seed);result.put("firstIndex",first);result.put("count",count);result.put("priorGroupsIncludingWarmup",earlier.size());
        result.put("warmup","ValidationArena seed190413 indices0..7,6..10plies,matching BrnSuccessorMatch.warm");
        result.put("openings",rows);result.put("earlierOverlapIndices",overlaps);result.put("withinSampleDuplicateIndexPairs",duplicates);result.put("terminalOrWrongLengthIndices",invalid);
        result.put("fresh",overlaps.isEmpty()&&duplicates.isEmpty()&&invalid.isEmpty());
        result.put("scope","Geometry includes color/rank reflection,file reflection and STM variants; excludes listed earlier recorded openings and fixed warmup roots,not every historical game ever played");
        return result;
    }
    @SuppressWarnings("unchecked") public static void main(String[] a)throws Exception {
        if(a.length!=5)throw new IllegalArgumentException("NEW_OUT SEED FIRST COUNT PRIOR_FEN_INVENTORY");
        Path out=Path.of(a[0]),inventory=Path.of(a[4]);if(Files.exists(out))throw new IOException("Use a fresh opening audit directory");
        var prior=DataFiles.read(inventory,Map.class);if(!"brn-architecture-prior-opening-fens-v1".equals(prior.get("schema")))throw new IOException("Prior inventory schema");
        var sources=(Map<String,String>)prior.get("sourceSha256");for(var entry:sources.entrySet())
            if(!BrnResearchComparison.digest(Path.of(entry.getKey())).equals(entry.getValue()))throw new IOException("Prior opening evidence changed: "+entry.getKey());
        var groups=new HashSet<String>();for(String fen:(List<String>)prior.get("fens"))groups.add(BrnResearchData.groupKey(Board.fromFen(fen)));
        var result=audit(Long.parseLong(a[1]),Integer.parseInt(a[2]),Integer.parseInt(a[3]),groups);
        result.put("priorInventory",inventory.toString());result.put("priorInventorySha256",BrnResearchComparison.digest(inventory));result.put("priorSourceFiles",sources.size());
        Files.createDirectory(out);DataFiles.write(out.resolve("result.json"),result);System.out.println(DataFiles.JSON.toJson(result));
        if(!Boolean.TRUE.equals(result.get("fresh")))throw new IOException("Opening freshness failed; preserve evidence and declare a new prospective sample before games");
    }
    private BrnArchitectureOpeningAudit(){}
}
