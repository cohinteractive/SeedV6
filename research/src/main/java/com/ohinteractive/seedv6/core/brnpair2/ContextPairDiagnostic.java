package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/** Bounded fixed-base, extra-local versus king-conditioned energy experiment. */
public final class ContextPairDiagnostic {
    static double mse(ContextPairEnergy model,PairObjectiveDiagnostic.Data data) {
        var workspace=model.new Workspace();double loss=0;
        for(int i=0;i<data.boards().length;i++){double d=PairObjectiveDiagnostic.sigmoid(model.logit(data.boards()[i],workspace))-data.targets()[i];loss+=d*d;}
        return loss/data.boards().length;
    }
    static void epoch(ContextPairEnergy model,PairObjectiveDiagnostic.Data data,long seed,int epoch) {
        int[] order=new int[data.boards().length];for(int i=0;i<order.length;i++)order[i]=i;var random=new Random(seed+epoch);
        for(int i=order.length-1;i>0;i--){int j=random.nextInt(i+1),v=order[i];order[i]=order[j];order[j]=v;}
        for(int offset=0;offset<order.length;offset+=128) {
            int n=Math.min(128,order.length-offset);long[][] boards=new long[n][];double[] targets=new double[n];
            for(int j=0;j<n;j++){int i=order[offset+j];boards[j]=data.boards()[i];targets[j]=data.targets()[i];}model.train(boards,targets);
        }
    }
    public static void main(String[] args)throws Exception {
        PairObjectiveDiagnostic.permission();Path configPath=Path.of(args[0]);var config=JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();
        String mode=config.get("mode").getAsString();if(!Set.of("SHARED","KING").contains(mode))throw new IllegalArgumentException("Mode");
        Path trainPath=Path.of(config.get("train").getAsString()),valPath=Path.of(config.get("validation").getAsString()),basePath=Path.of(config.get("base").getAsString());
        String baseSha=config.get("base_sha256").getAsString();
        if(!PairObjectiveDiagnostic.sha(trainPath).equals(config.get("train_sha256").getAsString())||!PairObjectiveDiagnostic.sha(valPath).equals(config.get("validation_sha256").getAsString())||!PairObjectiveDiagnostic.sha(basePath).equals(baseSha))throw new IllegalArgumentException("Input identity");
        BrnPair2Model base;try(var in=Files.newInputStream(basePath)){base=BrnPair2Codec.readModel(in);}
        var model=new ContextPairEnergy(mode.equals("KING"),config.get("rate").getAsDouble(),base,baseSha);
        var train=PairObjectiveDiagnostic.read(trainPath);var val=PairObjectiveDiagnostic.read(valPath);long seed=config.get("seed").getAsLong();
        Path out=Path.of(config.get("output").getAsString());Files.createDirectories(out);String configSha=PairObjectiveDiagnostic.sha(configPath);Path bound=out.resolve("config.sha256");
        if(Files.exists(bound)){if(!Files.readString(bound).equals(configSha))throw new IllegalArgumentException("Config changed");}
        else Files.writeString(bound,configSha,StandardOpenOption.CREATE_NEW);
        var workspace=model.new Workspace();double checksum=0;
        for(int i=0;i<10000;i++)checksum+=model.logit(val.boards()[i%val.boards().length],workspace);
        long start=System.nanoTime();for(int i=0;i<20000;i++)checksum+=model.logit(val.boards()[i%val.boards().length],workspace);
        double cost=(System.nanoTime()-start)/20000.0;
        if(cost>10000)throw new IllegalStateException("Context full inference cost gate: "+cost);
        var gson=new GsonBuilder().setPrettyPrinting().create();var metrics=new ArrayList<Object>();metrics.add(Map.of("epoch",0,"validation_mse",mse(model,val)));
        start=System.nanoTime();int epochs=config.get("epochs").getAsInt();
        for(int epoch=1;epoch<=epochs;epoch++) {
            PairObjectiveDiagnostic.permission();Path receipt=out.resolve("epoch-"+epoch+".json"),state=out.resolve("epoch-"+epoch+".state");
            if(Files.exists(receipt)) {
                var r=JsonParser.parseString(Files.readString(receipt)).getAsJsonObject();
                if(!PairObjectiveDiagnostic.sha(state).equals(r.get("state_sha256").getAsString()))throw new IllegalArgumentException("Checkpoint identity");
                try(var in=Files.newInputStream(state)){model=ContextPairEnergy.read(in,base,baseSha);}
                if(model.buckets!=(mode.equals("KING")?10:1)||model.rate!=config.get("rate").getAsDouble())throw new IllegalArgumentException("Resume mode/rate");
                metrics.add(r);continue;
            }
            if(Files.exists(state))throw new IllegalArgumentException("Partial context checkpoint preserved; reconcile before retry");
            epoch(model,train,seed,epoch);
            try(var stream=new java.io.BufferedOutputStream(Files.newOutputStream(state,StandardOpenOption.CREATE_NEW))){model.write(stream);}
            var r=Map.of("epoch",epoch,"train_mse",mse(model,train),"validation_mse",mse(model,val),"state_sha256",PairObjectiveDiagnostic.sha(state),"updates",model.step);
            Files.writeString(receipt,gson.toJson(r)+"\n",StandardOpenOption.CREATE_NEW);metrics.add(r);
        }
        var result=Map.of("mode",mode,"seed",seed,"parameters",model.weights.length,"metrics",metrics,"seconds",(System.nanoTime()-start)/1e9,"full_inference_ns",cost,"checksum",checksum,
            "scope","Fixed-base direct relational energy development screen; no incremental/search/strength qualification");
        Files.writeString(out.resolve("result.json"),gson.toJson(result)+"\n",StandardOpenOption.CREATE_NEW);System.out.println(gson.toJson(result));
    }
    private ContextPairDiagnostic(){}
}
