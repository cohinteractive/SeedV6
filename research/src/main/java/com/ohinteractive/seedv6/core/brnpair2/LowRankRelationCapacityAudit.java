package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import com.ohinteractive.seedv6.core.Board;
import java.nio.file.*;
import java.util.*;

/** R056 untrained relational capacity and same-harness computational controls. */
public final class LowRankRelationCapacityAudit {
    public static void main(String[] args)throws Exception {
        PairObjectiveDiagnostic.permission();Path configPath=Path.of(args[0]);var config=JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();
        Path cases=Path.of(config.get("cases").getAsString()),validation=Path.of(config.get("validation").getAsString()),train=Path.of(config.get("train").getAsString()),out=Path.of(config.get("output").getAsString());
        for(String key:List.of("cases","validation","train"))if(!PairCoverageDiagnostic.sha(Path.of(config.get(key).getAsString())).equals(config.get(key+"_sha256").getAsString()))throw new IllegalArgumentException("Input identity "+key);
        var quartets=JsonParser.parseString(Files.readString(cases)).getAsJsonArray();if(quartets.size()!=64)throw new IllegalArgumentException("Quartet count");
        var tiny=new PairCoverageDiagnostic.Data(128);try(var reader=Files.newBufferedReader(train)){for(int i=0;i<128;i++){String line=reader.readLine();if(line==null)throw new IllegalArgumentException("Training prefix");int split=line.indexOf('\t');long[] b=Board.fromFen(line.substring(0,split));System.arraycopy(b,0,tiny.boards,i*6,6);tiny.targets[i]=Double.parseDouble(line.substring(split+1));}}
        var data=PairCoverageDiagnostic.Data.read(validation,4096);var models=new ArrayList<Map<String,Object>>();long[] board=new long[6];
        for(boolean compose:new boolean[]{false,true}) {
            PairObjectiveDiagnostic.permission();var model=new LowRankRelationNetwork(compose,.01,71);var w=new LowRankRelationNetwork.Workspace();double[] contrasts=new double[64];int nonzero=0,index=0;
            for(var element:quartets) {
                var fens=element.getAsJsonObject().getAsJsonArray("fens");double contrast=0;
                for(int i=0;i<4;i++)contrast+=(i==0||i==3?1:-1)*model.scoreCp(Board.fromFen(fens.get(i).getAsString()),w);
                contrasts[index++]=contrast;if(Math.abs(contrast)>1e-9)nonzero++;
            }
            double checksum=0;for(int i=0;i<10000;i++){data.board(i%data.rows,board);checksum+=model.scoreCp(board,w);}
            long start=System.nanoTime();for(int i=0;i<20000;i++){data.board(i%data.rows,board);checksum+=model.scoreCp(board,w);}double cost=(System.nanoTime()-start)/20000.0;
            if(!Double.isFinite(checksum))throw new ArithmeticException("Cost checksum");
            var batch=new LowRankRelationNetwork.Features[128];for(int i=0;i<128;i++){tiny.board(i,board);batch[i]=w.encode(board).frozen();}
            for(int i=0;i<10;i++){PairObjectiveDiagnostic.permission();model.train(batch,tiny.targets);}
            long trainStart=System.nanoTime();for(int i=0;i<20;i++){PairObjectiveDiagnostic.permission();model.train(batch,tiny.targets);}double updateNs=(System.nanoTime()-trainStart)/20.0;
            var row=new LinkedHashMap<String,Object>();row.put("mode",compose?"TWO":"ONE");row.put("parameters",LowRankRelationNetwork.PARAMETERS);row.put("inference_weight_bytes",8L*LowRankRelationNetwork.PARAMETERS);row.put("derived_cache_bytes",0);
            row.put("full_inference_ns",cost);row.put("update_128_ns",updateNs);row.put("nonzero_remote_contrasts",nonzero);row.put("contrasts_cp",contrasts);row.put("checksum",checksum);models.add(row);
        }
        var reference=new MixedTiedPieceNetwork(true,.01,71);var w=new MixedTiedPieceNetwork.Workspace();double checksum=0;
        for(int i=0;i<10000;i++){data.board(i%data.rows,board);checksum+=reference.scoreCp(board,w);}long start=System.nanoTime();
        for(int i=0;i<20000;i++){data.board(i%data.rows,board);checksum+=reference.scoreCp(board,w);}double referenceCost=(System.nanoTime()-start)/20000.0;
        var batch=new MixedTiedPieceNetwork.Features[128];for(int i=0;i<128;i++){tiny.board(i,board);batch[i]=w.encode(board).frozen();}
        for(int i=0;i<10;i++){PairObjectiveDiagnostic.permission();reference.train(batch,tiny.targets);}start=System.nanoTime();
        for(int i=0;i<20;i++){PairObjectiveDiagnostic.permission();reference.train(batch,tiny.targets);}double referenceUpdate=(System.nanoTime()-start)/20.0;
        if(!Double.isFinite(checksum))throw new ArithmeticException("Reference cost checksum");
        var referenceResult=Map.of("mode","R045_MIX","full_inference_ns",referenceCost,"update_128_ns",referenceUpdate,"checksum",checksum);
        for(var row:models) {
            double cost=((Number)row.get("full_inference_ns")).doubleValue(),update=((Number)row.get("update_128_ns")).doubleValue();
            row.put("inference_ratio_to_mix",cost/referenceCost);row.put("update_ratio_to_mix",update/referenceUpdate);
            row.put("passes",cost<=5000&&cost<=.75*referenceCost&&update<=2*referenceUpdate&&((Number)row.get("nonzero_remote_contrasts")).intValue()==64);
        }
        var result=Map.of("config_sha256",PairCoverageDiagnostic.sha(configPath),"models",models,"reference",referenceResult,"disposable_updates_per_mode",30,
            "scope","Untrained capacity,not teacher correctness;fixed-order microbenchmarks,not strength or learning speed. Original training-prefix disposable updates only;no checkpoint promotion.");
        Files.writeString(out,new GsonBuilder().setPrettyPrinting().create().toJson(result)+"\n",StandardOpenOption.CREATE_NEW);System.out.println("Completed factorized relation capacity and same-harness cost audit");
    }
    private LowRankRelationCapacityAudit(){}
}
