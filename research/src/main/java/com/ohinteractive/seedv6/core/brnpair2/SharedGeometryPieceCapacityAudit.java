package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import com.ohinteractive.seedv6.core.Board;
import java.nio.file.*;
import java.util.*;

/** Untrained representability and measured full-recomputation cost; no teacher fit. */
public final class SharedGeometryPieceCapacityAudit {
    public static void main(String[] args)throws Exception {
        PairObjectiveDiagnostic.permission();Path configPath=Path.of(args[0]);var config=JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();
        Path cases=Path.of(config.get("cases").getAsString()),validation=Path.of(config.get("validation").getAsString()),out=Path.of(config.get("output").getAsString());
        if(!PairCoverageDiagnostic.sha(cases).equals(config.get("cases_sha256").getAsString())||!PairCoverageDiagnostic.sha(validation).equals(config.get("validation_sha256").getAsString()))throw new IllegalArgumentException("Input identity");
        var quartets=JsonParser.parseString(Files.readString(cases)).getAsJsonArray();if(quartets.size()!=64)throw new IllegalArgumentException("Quartet count");
        var data=PairCoverageDiagnostic.Data.read(validation,4096);var models=new ArrayList<Object>();long[] board=new long[6];
        for(boolean context:new boolean[]{false,true}) {
            PairObjectiveDiagnostic.permission();var model=new SharedGeometryPieceNetwork(context,.01,71);var w=new SharedGeometryPieceNetwork.Workspace();double[] contrasts=new double[64];int nonzero=0,index=0;
            for(var element:quartets) {
                var fens=element.getAsJsonObject().getAsJsonArray("fens");double contrast=0;
                for(int i=0;i<4;i++)contrast+=(i==0||i==3?1:-1)*model.scoreCp(Board.fromFen(fens.get(i).getAsString()),w);
                contrasts[index++]=contrast;if(Math.abs(contrast)>1e-9)nonzero++;
            }
            double checksum=0;for(int i=0;i<10000;i++){data.board(i%data.rows,board);checksum+=model.scoreCp(board,w);}
            long start=System.nanoTime();for(int i=0;i<20000;i++){data.board(i%data.rows,board);checksum+=model.scoreCp(board,w);}double cost=(System.nanoTime()-start)/20000.0;
            if(!Double.isFinite(checksum))throw new ArithmeticException("Cost checksum");
            models.add(Map.of("mode",context?"CONTEXT":"GEOMETRY","seed",71,"parameters",SharedGeometryPieceNetwork.PARAMETERS,"full_inference_ns",cost,"checksum",checksum,"nonzero_remote_contrasts",nonzero,"contrasts_cp",contrasts,"preliminary_cost_pass",cost<=30000,"practical_10us_pass",cost<=10000));
        }
        var result=Map.of("config_sha256",PairCoverageDiagnostic.sha(configPath),"models",models,"scope","Untrained capacity is not correctness or strength;context coefficients initially zero;fixed-endpoint occupancy witness separately tested;no fitting");
        Files.writeString(out,new GsonBuilder().setPrettyPrinting().create().toJson(result)+"\n",StandardOpenOption.CREATE_NEW);System.out.println("Completed shared neural geometry capacity and cost audit");
    }
    private SharedGeometryPieceCapacityAudit(){}
}
