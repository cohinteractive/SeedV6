package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/** Fixed two-block alternating-order cost comparison of equivalent computations. */
public final class SharedGeometryCompilationAudit {
    static Map<String,Object> profile(SharedGeometryPieceNetwork model,PairCoverageDiagnostic.Data data,boolean compiled,SharedGeometryPieceNetwork.Workspace workspace) {
        long[] board=new long[6];double checksum=0;long start=System.nanoTime();
        for(int i=0;i<10000;i++){data.board(i%data.rows,board);checksum+=compiled?model.scoreCp(board,workspace):model.scoreCpUncached(board,workspace);}
        return Map.of("compiled",compiled,"calls",10000,"nanoseconds",System.nanoTime()-start,"checksum",checksum);
    }
    public static void main(String[] args)throws Exception {
        PairObjectiveDiagnostic.permission();Path configPath=Path.of(args[0]);var config=JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();
        Path cases=Path.of(config.get("cases").getAsString()),out=Path.of(config.get("output").getAsString());
        if(!PairCoverageDiagnostic.sha(cases).equals(config.get("cases_sha256").getAsString()))throw new IllegalArgumentException("Input identity");
        var data=PairCoverageDiagnostic.Data.read(cases,4096);var models=new ArrayList<Object>();
        for(boolean context:new boolean[]{false,true}) {
            PairObjectiveDiagnostic.permission();var model=new SharedGeometryPieceNetwork(context,.01,71);var cached=new SharedGeometryPieceNetwork.Workspace();var direct=new SharedGeometryPieceNetwork.Workspace();
            long[] board=new long[6];data.board(0,board);long cold=System.nanoTime();double coldScore=model.scoreCp(board,cached);cold=System.nanoTime()-cold;
            double maximumError=0;
            for(int i=0;i<data.rows;i++){data.board(i,board);maximumError=Math.max(maximumError,Math.abs(model.scoreCpUncached(board,direct)-model.scoreCp(board,cached)));}
            if(maximumError>1e-10)throw new ArithmeticException("Compiled forward parity");
            profile(model,data,false,direct);profile(model,data,true,cached); // fixed warmup, not observations
            var blocks=new ArrayList<Map<String,Object>>();blocks.add(profile(model,data,false,direct));blocks.add(profile(model,data,true,cached));blocks.add(profile(model,data,true,cached));blocks.add(profile(model,data,false,direct));
            long uncachedTime=0,cachedTime=0;
            for(var block:blocks){if((Boolean)block.get("compiled"))cachedTime+=(Long)block.get("nanoseconds");else uncachedTime+=(Long)block.get("nanoseconds");}
            double uncached=uncachedTime/20000.0,compiled=cachedTime/20000.0;
            models.add(Map.of("mode",context?"CONTEXT":"GEOMETRY","uncached_inference_ns",uncached,"compiled_inference_ns",compiled,"speedup",uncached/compiled,"maximum_forward_error_cp",maximumError,"cold_compiled_call_ns",cold,"cold_score",coldScore,"blocks",blocks,"passes",compiled<=10000&&uncached/compiled>=2));
        }
        var result=Map.of("config_sha256",PairCoverageDiagnostic.sha(configPath),"models",models,"projection_cache_bytes_per_workspace",2700*8*8,"scope","Equivalent neural computation;steady-state same-panel fixed profiling blocks,not clocks or strength;cold call separately reported;no large fitting");
        Files.writeString(out,new GsonBuilder().setPrettyPrinting().create().toJson(result)+"\n",StandardOpenOption.CREATE_NEW);System.out.println("Completed shared projection equivalence and fixed cost blocks");
    }
    private SharedGeometryCompilationAudit(){}
}
