package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/** Same-panel component and full-cost comparison of equivalent neural computations. */
public final class SharedGeometryFiniteAudit {
    static Map<String,Object> profile(SharedGeometryPieceNetwork model,PairCoverageDiagnostic.Data data,SharedGeometryPieceNetwork.Features[] features,String kind,SharedGeometryPieceNetwork.Workspace w) {
        long[] board=new long[6];double checksum=0;long start=System.nanoTime();
        for(int i=0;i<10000;i++) {
            int row=i%data.rows;
            switch(kind) {
                case "encode" -> {data.board(row,board);var f=w.encode(board);checksum+=f.size+f.norm+f.material;}
                case "projection_preencoded" -> checksum+=model.projectionLogit(features[row],w);
                case "compiled_preencoded" -> checksum+=model.compiledLogit(features[row],w);
                case "projection_full" -> {data.board(row,board);checksum+=model.scoreCpProjection(board,w);}
                case "compiled_full" -> {data.board(row,board);checksum+=model.scoreCp(board,w);}
                default -> throw new IllegalArgumentException("Profile kind");
            }
        }
        return Map.of("kind",kind,"calls",10000,"nanoseconds",System.nanoTime()-start,"checksum",checksum);
    }
    public static void main(String[] args)throws Exception {
        PairObjectiveDiagnostic.permission();Path configPath=Path.of(args[0]);var config=JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();
        Path cases=Path.of(config.get("cases").getAsString()),out=Path.of(config.get("output").getAsString());
        if(!PairCoverageDiagnostic.sha(cases).equals(config.get("cases_sha256").getAsString()))throw new IllegalArgumentException("Input identity");
        var data=PairCoverageDiagnostic.Data.read(cases,4096);var features=new SharedGeometryPieceNetwork.Features[4096];var encoder=new SharedGeometryPieceNetwork.Workspace();long[] board=new long[6];
        for(int i=0;i<features.length;i++){data.board(i,board);features[i]=encoder.encode(board).frozen();}
        var kinds=List.of("encode","projection_preencoded","compiled_preencoded","projection_full","compiled_full");var models=new ArrayList<Object>();
        for(boolean context:new boolean[]{false,true}) {
            PairObjectiveDiagnostic.permission();var model=new SharedGeometryPieceNetwork(context,.01,71);var w=new SharedGeometryPieceNetwork.Workspace();var reference=new SharedGeometryPieceNetwork.Workspace();
            data.board(0,board);long cold=System.nanoTime();double coldScore=model.scoreCp(board,w);cold=System.nanoTime()-cold;double maximumError=0;
            for(int i=0;i<data.rows;i++){data.board(i,board);double expected=model.scoreCpUncached(board,reference);maximumError=Math.max(maximumError,Math.max(Math.abs(expected-model.scoreCp(board,w)),Math.abs(expected-model.scoreCpProjection(board,w))));}
            if(maximumError>1e-10)throw new ArithmeticException("Compiled forward parity");
            for(String kind:kinds)profile(model,data,features,kind,w);
            var blocks=new ArrayList<Map<String,Object>>();for(String kind:kinds)blocks.add(profile(model,data,features,kind,w));for(int i=kinds.size()-1;i>=0;i--)blocks.add(profile(model,data,features,kinds.get(i),w));
            var times=new LinkedHashMap<String,Double>();for(String kind:kinds){long total=0;for(var block:blocks)if(block.get("kind").equals(kind))total+=(Long)block.get("nanoseconds");times.put(kind,total/20000.0);}
            double speedup=times.get("projection_full")/times.get("compiled_full");var result=new LinkedHashMap<String,Object>();
            result.put("mode",context?"CONTEXT":"GEOMETRY");result.put("ns_per_call",times);result.put("full_speedup",speedup);result.put("maximum_forward_error_cp",maximumError);result.put("cold_compiled_call_ns",cold);result.put("cold_score",coldScore);result.put("blocks",blocks);
            result.put("shared_linear_bytes",w.projection.length*8);result.put("shared_gate_bytes",w.gates.length*8);result.put("passes",times.get("compiled_full")<=10000&&(!context||speedup>=1.25));models.add(result);
        }
        var result=Map.of("config_sha256",PairCoverageDiagnostic.sha(configPath),"models",models,"scope","Fixed component/full-cost blocks;derived caches shared per model;no fitting,clock budget or strength claim;components do not necessarily add because JIT/cache effects differ");
        Files.writeString(out,new GsonBuilder().setPrettyPrinting().create().toJson(result)+"\n",StandardOpenOption.CREATE_NEW);System.out.println("Completed finite neural compilation audit");
    }
    private SharedGeometryFiniteAudit(){}
}
