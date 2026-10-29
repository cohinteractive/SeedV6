package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import com.ohinteractive.seedv6.core.Board;
import java.nio.file.*;
import java.util.*;

/** Untrained representability and measured full-recomputation cost; no teacher fit. */
public final class TypedRelationCapacityAudit {
    public static void main(String[] args)throws Exception {
        PairObjectiveDiagnostic.permission();Path configPath=Path.of(args[0]);var config=JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();
        Path cases=Path.of(config.get("cases").getAsString()),validation=Path.of(config.get("validation").getAsString()),out=Path.of(config.get("output").getAsString());
        if(!PairCoverageDiagnostic.sha(cases).equals(config.get("cases_sha256").getAsString())||!PairCoverageDiagnostic.sha(validation).equals(config.get("validation_sha256").getAsString()))throw new IllegalArgumentException("Input identity");
        var quartets=JsonParser.parseString(Files.readString(cases)).getAsJsonArray();if(quartets.size()!=64)throw new IllegalArgumentException("Quartet count");
        Path train=Path.of(config.get("train").getAsString());if(!PairCoverageDiagnostic.sha(train).equals(config.get("train_sha256").getAsString()))throw new IllegalArgumentException("Training identity");
        var tiny=new PairCoverageDiagnostic.Data(128);try(var reader=Files.newBufferedReader(train)){for(int i=0;i<128;i++){String line=reader.readLine();if(line==null)throw new IllegalArgumentException("Training prefix");int split=line.indexOf('\t');long[] b=Board.fromFen(line.substring(0,split));System.arraycopy(b,0,tiny.boards,i*6,6);tiny.targets[i]=Double.parseDouble(line.substring(split+1));}}
        var data=PairCoverageDiagnostic.Data.read(validation,4096);var models=new ArrayList<Object>();long[] board=new long[6];
        for(boolean compose:new boolean[]{false,true}) {
            PairObjectiveDiagnostic.permission();var model=new TypedRelationPieceNetwork(compose,.01,71);var w=new TypedRelationPieceNetwork.Workspace();double[] contrasts=new double[64];int nonzero=0,index=0;
            for(var element:quartets) {
                var fens=element.getAsJsonObject().getAsJsonArray("fens");double contrast=0;
                for(int i=0;i<4;i++)contrast+=(i==0||i==3?1:-1)*model.scoreCp(Board.fromFen(fens.get(i).getAsString()),w);
                contrasts[index++]=contrast;if(Math.abs(contrast)>1e-9)nonzero++;
            }
            var stress=new TypedRelationPieceNetwork(compose,.01,97);var random=new Random(2026102952);for(int i=0;i<stress.weights.length;i++)stress.weights[i]=.99*(2*random.nextDouble()-1);stress.invalidateProjections();double maxError=0;var sw=new TypedRelationPieceNetwork.Workspace();var dw=new TypedRelationPieceNetwork.Workspace();
            for(int i=0;i<data.rows;i++){data.board(i,board);maxError=Math.max(maxError,Math.abs(stress.scoreCp(board,sw)-stress.scoreCpUncached(board,dw)));}
            if(maxError>.001)throw new ArithmeticException("Derived float gate error "+maxError);
            double checksum=0;for(int i=0;i<10000;i++){data.board(i%data.rows,board);checksum+=model.scoreCp(board,w);}
            long start=System.nanoTime();for(int i=0;i<20000;i++){data.board(i%data.rows,board);checksum+=model.scoreCp(board,w);}double cost=(System.nanoTime()-start)/20000.0;
            if(!Double.isFinite(checksum))throw new ArithmeticException("Cost checksum");
            var batch=new TypedRelationPieceNetwork.Features[128];for(int i=0;i<128;i++){tiny.board(i,board);batch[i]=w.encode(board).frozen();}
            for(int i=0;i<10;i++){PairObjectiveDiagnostic.permission();model.train(batch,tiny.targets);}
            long trainStart=System.nanoTime();for(int i=0;i<20;i++){PairObjectiveDiagnostic.permission();model.train(batch,tiny.targets);}double updateNs=(System.nanoTime()-trainStart)/20.0;
            var row=new LinkedHashMap<String,Object>();row.put("update_128_ns",updateNs);row.put("maximum_stressed_score_error_cp",maxError);row.put("derived_cache_bytes",compose?2700L*12*8*(8+7*4):2700L*8*8*8);row.put("microbenchmark_only_updates",30);row.putAll(Map.of("mode",compose?"TYPED":"CONTROL","seed",71,"parameters",TypedRelationPieceNetwork.PARAMETERS,"full_inference_ns",cost,"checksum",checksum,"nonzero_remote_contrasts",nonzero,"contrasts_cp",contrasts,"preliminary_cost_pass",cost<=10000,"practical_10us_pass",cost<=10000));models.add(row);
        }
        double control=((Number)((Map<?,?>)models.get(0)).get("update_128_ns")).doubleValue(),typed=((Number)((Map<?,?>)models.get(1)).get("update_128_ns")).doubleValue();
        var result=Map.of("typed_update_ratio",typed/control,"update_cost_pass",typed/control<=3,"config_sha256",PairCoverageDiagnostic.sha(configPath),"models",models,"scope","Untrained capacity is not correctness or strength;B initially zero;source transform witness separately tested;30 disposable microbenchmark updates per mode on original training prefix,not candidate fitting");
        Files.writeString(out,new GsonBuilder().setPrettyPrinting().create().toJson(result)+"\n",StandardOpenOption.CREATE_NEW);System.out.println("Completed shared neural geometry capacity and cost audit");
    }
    private TypedRelationCapacityAudit(){}
}
