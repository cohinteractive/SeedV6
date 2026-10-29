package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/** Identical neural updates with batch-local encoding of a bounded flat dataset. */
public final class ContextualPieceDistributionDiagnostic {
    static void segment(ContextualPieceNetwork model,PairCoverageDiagnostic.Data data,long seed,int epoch,int begin,int end) {
        ContextualPieceFineTuneDiagnostic.segment(model,data,seed,epoch,begin,end);
    }
    static double[] mse(ContextualPieceNetwork model,PairCoverageDiagnostic.Data data) {
        return ContextualPieceFineTuneDiagnostic.mse(model,data);
    }
    static ContextualPieceNetwork loadParent(JsonObject config)throws Exception {
        Path manifest=Path.of(config.get("mixture_manifest").getAsString());
        if(!PairCoverageDiagnostic.sha(manifest).equals(config.get("mixture_manifest_sha256").getAsString()))throw new IllegalArgumentException("Mixture manifest identity");
        var mix=JsonParser.parseString(Files.readString(manifest)).getAsJsonObject();
        Path parentPath=Path.of(config.get("parent_config").getAsString());
        if(!PairCoverageDiagnostic.sha(parentPath).equals(config.get("parent_config_sha256").getAsString()))throw new IllegalArgumentException("Parent configuration identity");
        var parent=JsonParser.parseString(Files.readString(parentPath)).getAsJsonObject();
        int rows=config.get("train_positions").getAsInt();
        if(!mix.get("schema").getAsString().equals("root-grouped-neural-mixture-v1")||mix.get("rows").getAsInt()!=rows||rows%2!=0||mix.get("old_rows").getAsInt()!=rows/2||mix.get("new_rows").getAsInt()!=rows/2)throw new IllegalArgumentException("Mixture exposure");
        if(!mix.get("old_sha256").equals(parent.get("train_sha256"))||!mix.get("output_sha256").equals(config.get("train_sha256"))||!mix.get("output").equals(config.get("train"))||!mix.get("new_sha256").equals(config.get("new_train_sha256"))||!mix.get("validation_sha256").equals(config.get("new_validation_sha256")))throw new IllegalArgumentException("Mixture provenance");
        // Only the authorized data field changes. R031 retains every other parent guard.
        var compatible=config.deepCopy();compatible.add("train_sha256",parent.get("train_sha256"));
        return ContextualPieceFineTuneDiagnostic.loadParent(compatible);
    }
    public static void main(String[] args)throws Exception {
        PairObjectiveDiagnostic.permission();Path configPath=Path.of(args[0]);var config=JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();
        String mode=config.get("mode").getAsString();
        if(!mode.equals("ONE")||config.get("initial_epoch").getAsInt()!=1||config.get("epochs").getAsInt()!=2||config.get("train_positions").getAsInt()!=1048576||config.get("batch").getAsInt()!=128||config.get("width").getAsInt()!=8||config.get("decay_multiplier").getAsDouble()!=.9999||config.get("weight_bound").getAsDouble()!=.99)throw new IllegalArgumentException("Recipe");
        long seed=config.get("seed").getAsLong();double rate=config.get("rate").getAsDouble();var model=loadParent(config);
        Path trainPath=Path.of(config.get("train").getAsString()),valPath=Path.of(config.get("validation").getAsString()),out=Path.of(config.get("output").getAsString());
        if(!PairCoverageDiagnostic.sha(trainPath).equals(config.get("train_sha256").getAsString())||!PairCoverageDiagnostic.sha(valPath).equals(config.get("validation_sha256").getAsString()))throw new IllegalArgumentException("Data identity");
        Files.createDirectories(out);String configSha=PairCoverageDiagnostic.sha(configPath);Path bound=out.resolve("config.sha256");
        if(Files.exists(bound)){if(!Files.readString(bound).equals(configSha))throw new IllegalArgumentException("Config changed");}else Files.writeString(bound,configSha,StandardOpenOption.CREATE_NEW);
        long loading=System.nanoTime();var train=PairCoverageDiagnostic.Data.read(trainPath,config.get("train_positions").getAsInt());var val=PairCoverageDiagnostic.Data.read(valPath,4096);double loadSeconds=(System.nanoTime()-loading)/1e9;
        var workspace=new ContextualPieceNetwork.Workspace();long[] sample=new long[PairCoverageDiagnostic.Data.STRIDE];double checksum=0;
        for(int i=0;i<10000;i++){val.board(i%val.rows,sample);checksum+=model.scoreCp(sample,workspace);}
        long profiling=System.nanoTime();for(int i=0;i<20000;i++){val.board(i%val.rows,sample);checksum+=model.scoreCp(sample,workspace);}
        double cost=(System.nanoTime()-profiling)/20000.0;System.out.println("Full inference ns="+cost+" checksum="+checksum);
        if(cost>30000)throw new IllegalStateException("Full neural inference cost gate: "+cost);
        Path originalPath=Path.of(config.get("original_validation").getAsString());if(!PairCoverageDiagnostic.sha(originalPath).equals(config.get("original_validation_sha256").getAsString()))throw new IllegalArgumentException("Original target identity");
        var original=PairCoverageDiagnostic.Data.read(originalPath,4096);
        Path newTrainPath=Path.of(config.get("new_train").getAsString()),newValidationPath=Path.of(config.get("new_validation").getAsString());
        if(!PairCoverageDiagnostic.sha(newTrainPath).equals(config.get("new_train_sha256").getAsString())||!PairCoverageDiagnostic.sha(newValidationPath).equals(config.get("new_validation_sha256").getAsString()))throw new IllegalArgumentException("New corpus identity");
        var newTrain=PairCoverageDiagnostic.Data.read(newTrainPath,config.get("new_train_positions").getAsInt());
        var newValidation=PairCoverageDiagnostic.Data.read(newValidationPath,config.get("new_validation_positions").getAsInt());
        var gson=new GsonBuilder().setPrettyPrinting().create();var metrics=new ArrayList<Object>();metrics.add(Map.of("epoch",1,"validation_mse",mse(model,val)[0]));long start=System.nanoTime();
        for(int epoch=2;epoch<=config.get("epochs").getAsInt();epoch++) {
            PairObjectiveDiagnostic.permission();Path state=out.resolve("epoch-"+epoch+".state"),receipt=out.resolve("epoch-"+epoch+".json");
            if(Files.exists(receipt)) {
                var r=JsonParser.parseString(Files.readString(receipt)).getAsJsonObject();if(!PairCoverageDiagnostic.sha(state).equals(r.get("state_sha256").getAsString()))throw new IllegalArgumentException("Checkpoint identity");
                try(var input=Files.newInputStream(state)){model=ContextualPieceNetwork.read(input);}if(model.compose||model.rate!=rate)throw new IllegalArgumentException("Resume semantics");metrics.add(r);continue;
            }
            if(Files.exists(state))throw new IllegalArgumentException("Partial neural checkpoint preserved; reconcile before retry");
            for(int begin=0,segment=1;begin<train.rows;begin+=131072,segment++) {
                PairObjectiveDiagnostic.permission();int end=Math.min(begin+131072,train.rows);
                Path part=out.resolve("segment-"+segment+".state"),partReceipt=out.resolve("segment-"+segment+".json");
                if(Files.exists(partReceipt)) {
                    var saved=JsonParser.parseString(Files.readString(partReceipt)).getAsJsonObject();
                    if(saved.get("completed_positions").getAsInt()!=end||!saved.get("config_sha256").getAsString().equals(configSha)||!PairCoverageDiagnostic.sha(part).equals(saved.get("state_sha256").getAsString()))throw new IllegalArgumentException("Segment identity");
                    try(var input=Files.newInputStream(part)){model=ContextualPieceNetwork.read(input);}
                    if(model.compose||model.rate!=rate||model.step!=8192+(end+127)/128)throw new IllegalArgumentException("Segment semantics");
                    continue;
                }
                if(Files.exists(part)) {
                    try(var input=Files.newInputStream(part)){model=ContextualPieceNetwork.read(input);}
                    if(model.compose||model.rate!=rate||model.step!=8192+(end+127)/128)throw new IllegalArgumentException("Published segment semantics");
                } else {
                    segment(model,train,seed,epoch,begin,end);Path temporary=out.resolve("segment-"+segment+"-"+System.nanoTime()+".part");
                    try(var output=Files.newOutputStream(temporary,StandardOpenOption.CREATE_NEW)){model.write(output);}
                    Files.move(temporary,part,StandardCopyOption.ATOMIC_MOVE);
                }
                Files.writeString(partReceipt,gson.toJson(Map.of("completed_positions",end,"updates",model.step,"config_sha256",configSha,"state_sha256",PairCoverageDiagnostic.sha(part)))+"\n",StandardOpenOption.CREATE_NEW);
                System.out.println("Completed segment="+segment+" positions="+end);System.out.flush();
            }
            double[] validation=mse(model,val),training=mse(model,train);
            try(var output=Files.newOutputStream(state,StandardOpenOption.CREATE_NEW)){model.write(output);}
            var r=Map.of("epoch",epoch,"updates",model.step,"train_mse",training[0],"unary_only_train_mse",training[1],"validation_mse",validation[0],"unary_only_validation_mse",validation[1],"second_pass_masked_validation_mse",validation[2],"second_pass_masked_train_mse",training[2],"state_sha256",PairCoverageDiagnostic.sha(state));
            Files.writeString(receipt,gson.toJson(r)+"\n",StandardOpenOption.CREATE_NEW);metrics.add(r);
        }
        var result=new LinkedHashMap<String,Object>(Map.of("mode",mode,"seed",seed,"rate",rate,"train_positions",train.rows,"metrics",metrics,"load_seconds",loadSeconds,
            "optimization_checkpoint_seconds",(System.nanoTime()-start)/1e9,"scope","Matched root-grouped neural data intervention;no search or strength qualification"));result.put("full_inference_ns",cost);result.put("parameters",ContextualPieceNetwork.PARAMETERS);result.put("active_parameters",ContextualPieceNetwork.S);result.put("decay_multiplier",.9999);
        result.put("original_bt4_validation_mse",mse(model,original)[0]);
        result.put("new_train_mse",mse(model,newTrain)[0]);result.put("new_validation_mse",mse(model,newValidation)[0]);
        Files.writeString(out.resolve("result.json"),gson.toJson(result)+"\n",StandardOpenOption.CREATE_NEW);System.out.println(gson.toJson(result));
    }
    private ContextualPieceDistributionDiagnostic(){}
}
