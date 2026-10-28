package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/** Research-only matched linear/nonlinear two-pass relational neural pilot. */
public final class AnchorDiagnostic {
    static AnchorNeuralNetwork.Features[] encode(PairObjectiveDiagnostic.Data data) {
        var w=new AnchorNeuralNetwork.Workspace();var out=new AnchorNeuralNetwork.Features[data.boards().length];
        for(int i=0;i<out.length;i++)out[i]=w.encode(data.boards()[i]).frozen();return out;
    }
    static double mse(AnchorNeuralNetwork model,AnchorNeuralNetwork.Features[] data,double[] targets,boolean unaryOnly) {
        var w=new AnchorNeuralNetwork.Workspace();double sum=0;
        for(int i=0;i<data.length;i++){double p=PairObjectiveDiagnostic.sigmoid(unaryOnly?model.unary(data[i]):model.logit(data[i],w));double d=p-targets[i];sum+=d*d;}return sum/data.length;
    }
    public static void main(String[] args)throws Exception {
        PairObjectiveDiagnostic.permission();Path configPath=Path.of(args[0]);var config=JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();
        String mode=config.get("mode").getAsString();if(!Set.of("LINEAR","NONLINEAR").contains(mode))throw new IllegalArgumentException("Mode");
        long seed=config.get("seed").getAsLong();var model=new AnchorNeuralNetwork(mode.equals("NONLINEAR"),config.get("rate").getAsDouble(),seed);
        Path trainPath=Path.of(config.get("train").getAsString()),valPath=Path.of(config.get("validation").getAsString()),out=Path.of(config.get("output").getAsString());
        if(!PairObjectiveDiagnostic.sha(trainPath).equals(config.get("train_sha256").getAsString())||!PairObjectiveDiagnostic.sha(valPath).equals(config.get("validation_sha256").getAsString()))throw new IllegalArgumentException("Data identity");
        Files.createDirectories(out);String configSha=PairObjectiveDiagnostic.sha(configPath);Path binding=out.resolve("config.sha256");
        if(Files.exists(binding)){if(!Files.readString(binding).equals(configSha))throw new IllegalArgumentException("Config changed");}else Files.writeString(binding,configSha,StandardOpenOption.CREATE_NEW);
        long preparation=System.nanoTime();var trainData=PairObjectiveDiagnostic.read(trainPath);var valData=PairObjectiveDiagnostic.read(valPath);var train=encode(trainData);var val=encode(valData);double prep=(System.nanoTime()-preparation)/1e9;
        var w=new AnchorNeuralNetwork.Workspace();double checksum=0;for(int i=0;i<10000;i++)checksum+=model.scoreCp(valData.boards()[i%val.length],w);
        long began=System.nanoTime();for(int i=0;i<20000;i++)checksum+=model.scoreCp(valData.boards()[i%val.length],w);double cost=(System.nanoTime()-began)/20000.0;
        if(cost>10000)throw new IllegalStateException("Full neural inference cost gate: "+cost);
        var gson=new GsonBuilder().setPrettyPrinting().create();var metrics=new ArrayList<Object>();metrics.add(Map.of("epoch",0,"validation_mse",mse(model,val,valData.targets(),false)));
        long start=System.nanoTime();int epochs=config.get("epochs").getAsInt();
        for(int epoch=1;epoch<=epochs;epoch++) {
            PairObjectiveDiagnostic.permission();Path state=out.resolve("epoch-"+epoch+".state"),receipt=out.resolve("epoch-"+epoch+".json");
            if(Files.exists(receipt)) {
                var r=JsonParser.parseString(Files.readString(receipt)).getAsJsonObject();if(!PairObjectiveDiagnostic.sha(state).equals(r.get("state_sha256").getAsString()))throw new IllegalArgumentException("Checkpoint identity");
                try(var stream=Files.newInputStream(state)){model=AnchorNeuralNetwork.read(stream);}metrics.add(r);continue;
            }
            if(Files.exists(state))throw new IllegalArgumentException("Partial anchor state preserved; reconcile before retry");
            int[] order=new int[train.length];for(int i=0;i<order.length;i++)order[i]=i;var random=new Random(seed+epoch);
            for(int i=order.length-1;i>0;i--){int j=random.nextInt(i+1),v=order[i];order[i]=order[j];order[j]=v;}
            for(int offset=0;offset<order.length;offset+=128) {
                int n=Math.min(128,order.length-offset);var batch=new AnchorNeuralNetwork.Features[n];double[] targets=new double[n];
                for(int j=0;j<n;j++){int i=order[offset+j];batch[j]=train[i];targets[j]=trainData.targets()[i];}model.train(batch,targets);
            }
            try(var stream=Files.newOutputStream(state,StandardOpenOption.CREATE_NEW)){model.write(stream);}
            var r=Map.of("epoch",epoch,"train_mse",mse(model,train,trainData.targets(),false),"validation_mse",mse(model,val,valData.targets(),false),
                "unary_only_validation_mse",mse(model,val,valData.targets(),true),"state_sha256",PairObjectiveDiagnostic.sha(state),"updates",model.step);
            Files.writeString(receipt,gson.toJson(r)+"\n",StandardOpenOption.CREATE_NEW);metrics.add(r);
        }
        var result=Map.of("mode",mode,"seed",seed,"parameters",AnchorNeuralNetwork.PARAMETERS,"metrics",metrics,"preparation_seconds",prep,
            "optimization_checkpoint_seconds",(System.nanoTime()-start)/1e9,"full_inference_ns",cost,"checksum",checksum,"scope","Neural relational development pilot; no incremental/search/strength qualification");
        Files.writeString(out.resolve("result.json"),gson.toJson(result)+"\n",StandardOpenOption.CREATE_NEW);System.out.println(gson.toJson(result));
    }
    private AnchorDiagnostic(){}
}
