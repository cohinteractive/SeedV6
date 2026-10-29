package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.search.driver.ConventionalNnueEngine;
import java.nio.file.*;
import java.util.*;

/** Frozen original-target prediction comparison; no relationship to playing strength implied. */
public final class OnePredictionAudit {
    public static void main(String[] args)throws Exception {
        PairObjectiveDiagnostic.permission();Path configPath=Path.of(args[0]);var config=JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();
        Path state=Path.of(config.get("state").getAsString());if(!PairCoverageDiagnostic.sha(state).equals(config.get("state_sha256").getAsString()))throw new IllegalArgumentException("Model identity");
        TrainableOne model;try(var stream=Files.newInputStream(state)){model=TrainableOne.read(stream);}var fast=new IncrementalOne(model);var accumulator=fast.newAccumulator();
        Path cpPath=Path.of("build-assets/nnue-candidate/conventional-cp64.cnnue");if(!PairCoverageDiagnostic.sha(cpPath).equals("8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b"))throw new IllegalArgumentException("CP64 identity");
        var cp64=ConventionalNnueEngine.load(cpPath);var cpAccumulator=cp64.model().newAccumulator();var feature=new TrainableOne.Features();
        Path out=Path.of(config.get("output").getAsString());Files.createDirectory(out);var results=new ArrayList<Object>();
        for(var entry:config.getAsJsonArray("panels")){
            var panel=entry.getAsJsonObject();Path path=Path.of(panel.get("path").getAsString());if(!PairCoverageDiagnostic.sha(path).equals(panel.get("sha256").getAsString()))throw new IllegalArgumentException("Panel identity");
            String name=panel.get("name").getAsString();int count=panel.get("rows").getAsInt(),rows=0;double one=0,cp=0,unary=0,difference=0,differenceSquared=0,maxError=0;int clipped=0;double[][] phases=new double[9][4];
            try(var input=Files.newBufferedReader(path);var predictions=Files.newBufferedWriter(out.resolve(name+".tsv"),StandardOpenOption.CREATE_NEW)){
                predictions.write("row\ttarget\tone_cp\tcp64_cp\tunary_cp\tpieces\n");String line;
                while(rows<count&&(line=input.readLine())!=null){if((rows&1023)==0)PairObjectiveDiagnostic.permission();String[] fields=line.split(" \\| ");var board=Board.fromFen(fields[0]);
                    double target=PairObjectiveDiagnostic.sigmoid(Double.parseDouble(fields[1])*(Board.player((int)board[4])==0?1:-1)/400.0);
                    model.encode(board,feature);double full=400*model.logit(feature,model.workspace,true),u=400*model.logit(feature,model.workspace,false);accumulator.rebuild(board);double a=accumulator.scoreCp(board);maxError=Math.max(maxError,Math.abs(full-a));if(Math.abs(full-a)>1e-8+1e-12*Math.abs(full))throw new IllegalStateException("Trained inference equivalence");
                    cpAccumulator.rebuild(board);double c=cpAccumulator.evaluate(board);double e=PairObjectiveDiagnostic.sigmoid(a/400)-target,ce=PairObjectiveDiagnostic.sigmoid(c/400)-target,ue=PairObjectiveDiagnostic.sigmoid(u/400)-target;
                    one+=e*e;cp+=ce*ce;unary+=ue*ue;double d=e*e-ce*ce;difference+=d;differenceSquared+=d*d;int pieces=Long.bitCount(board[0]|board[1]|board[2]),bucket=pieces/4;phases[bucket][0]++;phases[bucket][1]+=e*e;phases[bucket][2]+=ce*ce;phases[bucket][3]+=ue*ue;
                    predictions.write(rows+"\t"+target+"\t"+a+"\t"+c+"\t"+u+"\t"+pieces+"\n");rows++;
                }
            }
            if(rows!=count)throw new IllegalStateException("Panel count");for(double weight:model.weights)if(Math.abs(weight)>=.989999999)clipped++;
            double mean=difference/rows,se=Math.sqrt(Math.max(0,(differenceSquared-rows*mean*mean)/(rows-1))/rows);
            results.add(Map.ofEntries(Map.entry("panel",name),Map.entry("rows",rows),Map.entry("one_mse",one/rows),Map.entry("cp64_mse",cp/rows),Map.entry("one_unary_only_mse",unary/rows),Map.entry("paired_loss_difference",mean),Map.entry("iid_position_standard_error_descriptive",se),Map.entry("phase_counts_sse_one_cp64_unary",phases),Map.entry("max_full_incremental_error_cp",maxError),Map.entry("clipped_parameters",clipped),Map.entry("predictions_sha256",PairCoverageDiagnostic.sha(out.resolve(name+".tsv")))));
        }
        var result=Map.of("status","PASS","state_sha256",PairCoverageDiagnostic.sha(state),"config_sha256",PairCoverageDiagnostic.sha(configPath),"parameters",model.parameters,"optimizer_steps",model.step,"cp64_backend",cp64.model().headBackend(),"panels",results,"limitations","Original BT4 targets; CP64 validation history differs. Unknown source-game correlations make iid position standard errors descriptive. No playing or equal-time strength conclusion.");
        Files.writeString(out.resolve("result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result)+"\n",StandardOpenOption.CREATE_NEW);System.out.println(new Gson().toJson(result));
    }
}
