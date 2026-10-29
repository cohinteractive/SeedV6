package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import static com.ohinteractive.seedv6.core.brnpair2.TiedGeometryTrainingDiagnostic.*;

/** One immutable-parent continuation; the trained model and original updater are unchanged. */
public final class TiedGeometryContinuationDiagnostic {
    static TiedGeometryPieceNetwork fork(TiedGeometryPieceNetwork parent,double rate) {
        if(!parent.compose||(rate!=.01&&rate!=.001))throw new IllegalArgumentException("TIED continuation rate/mode");
        var model=new TiedGeometryPieceNetwork(true,rate,0);model.step=parent.step;
        System.arraycopy(parent.weights,0,model.weights,0,model.weights.length);
        System.arraycopy(parent.first,0,model.first,0,model.first.length);
        System.arraycopy(parent.second,0,model.second,0,model.second.length);return model;
    }
    static TiedGeometryPieceNetwork loadParent(JsonObject config)throws Exception {
        Path parent=Path.of(config.get("parent_config").getAsString()),state=Path.of(config.get("initial_state").getAsString()),receipt=Path.of(config.get("parent_receipt").getAsString());
        if(!PairCoverageDiagnostic.sha(parent).equals(config.get("parent_config_sha256").getAsString())||!PairCoverageDiagnostic.sha(state).equals(config.get("initial_state_sha256").getAsString())||!PairCoverageDiagnostic.sha(receipt).equals(config.get("parent_receipt_sha256").getAsString()))throw new IllegalArgumentException("Parent identity");
        var p=JsonParser.parseString(Files.readString(parent)).getAsJsonObject();
        for(String name:List.of("mode","seed","train_positions","train_sha256","validation_sha256","original_validation_sha256","played_validation_sha256","width","batch","weight_bound","weight_decay"))if(!p.get(name).equals(config.get(name)))throw new IllegalArgumentException("Parent configuration: "+name);
        if(!p.get("mode").getAsString().equals("TIED")||p.get("rate").getAsDouble()!=.01||p.get("epochs").getAsInt()!=1)throw new IllegalArgumentException("Parent recipe");
        var r=JsonParser.parseString(Files.readString(receipt)).getAsJsonObject();
        if(config.get("initial_epoch").getAsInt()!=1||r.get("epoch").getAsInt()!=1||!r.get("state_sha256").equals(config.get("initial_state_sha256"))||r.get("updates").getAsLong()!=8192)throw new IllegalArgumentException("Parent epoch receipt");
        return fork(checkpoint(state,true,.01,8192),config.get("rate").getAsDouble());
    }
    public static void main(String[] args)throws Exception {
        PairObjectiveDiagnostic.permission();Path configPath=Path.of(args[0]);var config=JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();String mode=config.get("mode").getAsString();boolean compose=compose(mode);
        if(config.get("train_positions").getAsInt()!=1048576||config.get("epochs").getAsInt()!=2||config.get("initial_epoch").getAsInt()!=1||config.get("batch").getAsInt()!=128||config.get("width").getAsInt()!=8||config.get("weight_bound").getAsDouble()!=.99||(config.get("rate").getAsDouble()!=.01&&config.get("rate").getAsDouble()!=.001)||config.get("weight_decay").getAsDouble()!=.01||config.get("decay_multiplier").getAsDouble()!=1-config.get("rate").getAsDouble()*.01||!compose)throw new IllegalArgumentException("Fixed shared recipe");
        long seed=config.get("seed").getAsLong();if(seed!=71&&seed!=97)throw new IllegalArgumentException("Seed");double rate=config.get("rate").getAsDouble();
        Path out=Path.of(config.get("output").getAsString());Files.createDirectories(out);String configSha=PairCoverageDiagnostic.sha(configPath);Path bound=out.resolve("config.sha256");
        if(Files.exists(bound)){if(!Files.readString(bound).equals(configSha))throw new IllegalArgumentException("Config changed");}else Files.writeString(bound,configSha,StandardOpenOption.CREATE_NEW);
        long loading=System.nanoTime();var train=data(config,"train",1048576);var validation=data(config,"validation",4096);var original=data(config,"original_validation",4096);var played=data(config,"played_validation",1398);double loadSeconds=(System.nanoTime()-loading)/1e9;
        var model=loadParent(config);double preflight=cost(model,validation);if(preflight>30000)throw new IllegalStateException("Gross inference cost gate: "+preflight);
        double initialValidation=mse(model,validation)[0];long start=System.nanoTime();
        for(int segment=1;segment<=8;segment++) {
            PairObjectiveDiagnostic.permission();int begin=(segment-1)*131072,end=segment*131072;Path state=out.resolve("segment-"+segment+".state"),receipt=out.resolve("segment-"+segment+".json");
            if(Files.exists(receipt)) {
                var saved=JsonParser.parseString(Files.readString(receipt)).getAsJsonObject();if(saved.get("completed_positions").getAsInt()!=end||!saved.get("config_sha256").getAsString().equals(configSha)||!PairCoverageDiagnostic.sha(state).equals(saved.get("state_sha256").getAsString()))throw new IllegalArgumentException("Segment binding");
                model=checkpoint(state,compose,rate,8192+end/128);continue;
            }
            if(Files.exists(state))model=checkpoint(state,compose,rate,8192+end/128);
            else{TiedGeometryPieceDiagnostic.segment(model,train,seed,2,begin,end);publishState(state,model);}
            publishJson(receipt,Map.of("completed_positions",end,"updates",model.step,"config_sha256",configSha,"state_sha256",PairCoverageDiagnostic.sha(state)));
            System.out.println("Completed segment="+segment+" positions="+end);System.out.flush();
        }
        Path state=out.resolve("epoch-2.state"),receipt=out.resolve("epoch-2.json");if(Files.exists(state))model=checkpoint(state,compose,rate,16384);else publishState(state,model);
        double[] tr=mse(model,train),val=mse(model,validation);var epoch=Map.of("epoch",2,"updates",model.step,"train_mse",tr[0],"unary_only_train_mse",tr[1],"validation_mse",val[0],"unary_only_validation_mse",val[1],"state_sha256",PairCoverageDiagnostic.sha(state));
        if(Files.exists(receipt)){if(!JsonParser.parseString(Files.readString(receipt)).equals(GSON.toJsonTree(epoch)))throw new IllegalArgumentException("Epoch evidence changed");}else publishJson(receipt,epoch);
        var ablation=ablateComposition(model);double[] newMse=mse(model,played);var w=new TiedGeometryPieceNetwork.Workspace();long[] board=new long[6];double[] logits=new double[played.rows];
        for(int i=0;i<played.rows;i++){played.board(i,board);logits[i]=model.compiledLogit(w.encode(board),w);}
        var result=new LinkedHashMap<String,Object>();result.put("mode",mode);result.put("rate",rate);result.put("decay_multiplier",1-rate*.01);result.put("initial_state_sha256",config.get("initial_state_sha256").getAsString());result.put("seed",seed);result.put("parameters",TiedGeometryPieceNetwork.PARAMETERS);result.put("metrics",List.of(Map.of("epoch",1,"validation_mse",initialValidation),epoch));
        result.put("load_seconds",loadSeconds);result.put("optimization_checkpoint_seconds",(System.nanoTime()-start)/1e9);result.put("preflight_inference_ns",preflight);result.put("full_inference_ns",cost(model,validation));
        result.put("original_bt4_validation_mse",mse(model,original)[0]);result.put("played_validation_mse",newMse[0]);result.put("played_unary_only_mse",newMse[1]);
        result.put("second_pass_disabled_validation_mse",mse(ablation,validation)[0]);result.put("second_pass_disabled_played_validation_mse",mse(ablation,played)[0]);result.put("played_validation_logits",logits);
        result.put("scope","Tied neural continuation with original proportional decay;fixed epoch2;no strength claim");publishJson(out.resolve("result.json"),result);
        System.out.println("Completed mode="+mode+" seed="+seed+" oldTeacher="+val[0]+" playedTeacher="+newMse[0]);
    }
    private TiedGeometryContinuationDiagnostic(){}
}
