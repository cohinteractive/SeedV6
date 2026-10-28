package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/** Bounded exact continuation of the admitted neural target control; no model changes. */
public final class ContextualContinuationDiagnostic {
    static void check(ContextualAttackNetwork model,JsonObject config,int epoch) {
        long updates=(config.get("train_positions").getAsLong()+127)/128;
        if(model.global||model.rate!=config.get("rate").getAsDouble()||model.step!=epoch*updates)throw new IllegalArgumentException("Continuation model semantics");
    }
    static ContextualAttackNetwork loadParent(JsonObject config)throws Exception {
        if(!config.get("mode").getAsString().equals("LOCAL"))throw new IllegalArgumentException("Only the admitted LOCAL control");
        Path parent=Path.of(config.get("parent_config").getAsString()),state=Path.of(config.get("initial_state").getAsString()),receipt=Path.of(config.get("parent_receipt").getAsString());
        if(!PairCoverageDiagnostic.sha(parent).equals(config.get("parent_config_sha256").getAsString())||!PairCoverageDiagnostic.sha(state).equals(config.get("initial_state_sha256").getAsString())||!PairCoverageDiagnostic.sha(receipt).equals(config.get("parent_receipt_sha256").getAsString()))throw new IllegalArgumentException("Parent identity");
        var p=JsonParser.parseString(Files.readString(parent)).getAsJsonObject();
        for(String name:List.of("mode","seed","rate","train_positions","train_sha256","validation_sha256"))if(!p.get(name).equals(config.get(name)))throw new IllegalArgumentException("Parent configuration: "+name);
        var r=JsonParser.parseString(Files.readString(receipt)).getAsJsonObject();int epoch=config.get("initial_epoch").getAsInt();
        if(epoch<1||r.get("epoch").getAsInt()!=epoch||!r.get("state_sha256").equals(config.get("initial_state_sha256")))throw new IllegalArgumentException("Parent epoch receipt");
        ContextualAttackNetwork model;try(var input=Files.newInputStream(state)){model=ContextualAttackNetwork.read(input);}check(model,config,epoch);
        if(r.get("updates").getAsLong()!=model.step)throw new IllegalArgumentException("Parent optimizer step");return model;
    }
    public static void main(String[] args)throws Exception {
        PairObjectiveDiagnostic.permission();Path configPath=Path.of(args[0]);var config=JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();var model=loadParent(config);
        int initial=config.get("initial_epoch").getAsInt(),epochs=config.get("epochs").getAsInt();if(epochs<=initial||epochs>8)throw new IllegalArgumentException("Bounded continuation");
        Path trainPath=Path.of(config.get("train").getAsString()),valPath=Path.of(config.get("validation").getAsString()),out=Path.of(config.get("output").getAsString());
        if(!PairCoverageDiagnostic.sha(trainPath).equals(config.get("train_sha256").getAsString())||!PairCoverageDiagnostic.sha(valPath).equals(config.get("validation_sha256").getAsString()))throw new IllegalArgumentException("Data identity");
        Files.createDirectories(out);Path binding=out.resolve("config.sha256");String configSha=PairCoverageDiagnostic.sha(configPath);
        if(Files.exists(binding)){if(!Files.readString(binding).equals(configSha))throw new IllegalArgumentException("Configuration changed");}else Files.writeString(binding,configSha,StandardOpenOption.CREATE_NEW);
        var train=PairCoverageDiagnostic.Data.read(trainPath,config.get("train_positions").getAsInt());var validation=PairCoverageDiagnostic.Data.read(valPath,4096);var gson=new GsonBuilder().setPrettyPrinting().create();var metrics=new ArrayList<Object>();long start=System.nanoTime();
        for(int epoch=initial+1;epoch<=epochs;epoch++) {
            PairObjectiveDiagnostic.permission();Path state=out.resolve("epoch-"+epoch+".state"),receipt=out.resolve("epoch-"+epoch+".json");
            if(Files.exists(state)) {try(var input=Files.newInputStream(state)){model=ContextualAttackNetwork.read(input);}check(model,config,epoch);}
            else {
                if(Files.exists(receipt))throw new IllegalArgumentException("Missing recorded checkpoint");
                ContextualAttackDiagnostic.epoch(model,train,config.get("seed").getAsLong(),epoch);check(model,config,epoch);
                Path temporary=out.resolve("epoch-"+epoch+"-"+System.nanoTime()+".part");try(var output=Files.newOutputStream(temporary,StandardOpenOption.CREATE_NEW)){model.write(output);}
                Files.move(temporary,state,StandardCopyOption.ATOMIC_MOVE);
            }
            if(Files.exists(receipt)) {
                var r=JsonParser.parseString(Files.readString(receipt)).getAsJsonObject();if(!PairCoverageDiagnostic.sha(state).equals(r.get("state_sha256").getAsString())||r.get("epoch").getAsInt()!=epoch||r.get("updates").getAsLong()!=model.step)throw new IllegalArgumentException("Checkpoint receipt");metrics.add(r);continue;
            }
            // A fully published state without its receipt resumes metrics,never repeats its updates.
            double[] v=ContextualAttackDiagnostic.mse(model,validation),t=ContextualAttackDiagnostic.mse(model,train);
            var r=Map.of("epoch",epoch,"updates",model.step,"train_teacher_mse",t[0],"unary_only_train_teacher_mse",t[1],"validation_mse",v[0],"unary_only_validation_mse",v[1],"state_sha256",PairCoverageDiagnostic.sha(state));
            Files.writeString(receipt,gson.toJson(r)+"\n",StandardOpenOption.CREATE_NEW);metrics.add(r);
        }
        var result=Map.of("mode","LOCAL","seed",config.get("seed").getAsLong(),"initial_epoch",initial,"endpoint",epochs,"metrics",metrics,"optimization_checkpoint_seconds",(System.nanoTime()-start)/1e9,"scope","Fixed-endpoint neural optimization-depth diagnosis;no architectural gate reversal or strength qualification");
        Files.writeString(out.resolve("result.json"),gson.toJson(result)+"\n",StandardOpenOption.CREATE_NEW);System.out.println(gson.toJson(result));
    }
    private ContextualContinuationDiagnostic(){}
}
