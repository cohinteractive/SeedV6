package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/** Frozen graph-energy pilot; feature preparation cost is separate from optimization. */
public final class GraphEnergyDiagnostic {
    static RelationalGraphEnergy.Features[] encode(PairObjectiveDiagnostic.Data data,RelationalGraphEnergy.Mode mode) {
        var out=new RelationalGraphEnergy.Features[data.boards().length];
        for(int i=0;i<out.length;i++){var f=new RelationalGraphEnergy.Features();f.encode(data.boards()[i],mode);out[i]=f;}
        return out;
    }
    static double mse(RelationalGraphEnergy model,RelationalGraphEnergy.Features[] data,double[] targets) {
        double sum=0;for(int i=0;i<data.length;i++){double d=PairObjectiveDiagnostic.sigmoid(model.score(data[i])/400)-targets[i];sum+=d*d;}return sum/data.length;
    }
    public static void main(String[] args)throws Exception {
        PairObjectiveDiagnostic.permission();var config=JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonObject();
        var mode=RelationalGraphEnergy.Mode.valueOf(config.get("mode").getAsString());long seed=config.get("seed").getAsLong();
        var model=new RelationalGraphEnergy(mode,config.get("rate").getAsDouble());Path out=Path.of(config.get("output").getAsString());Files.createDirectories(out);
        Path trainPath=Path.of(config.get("train").getAsString()),valPath=Path.of(config.get("validation").getAsString());
        if(!PairObjectiveDiagnostic.sha(trainPath).equals(config.get("train_sha256").getAsString())||!PairObjectiveDiagnostic.sha(valPath).equals(config.get("validation_sha256").getAsString()))throw new IllegalArgumentException("Data identity");
        String configSha=PairObjectiveDiagnostic.sha(Path.of(args[0]));Path binding=out.resolve("config.sha256");
        if(Files.exists(binding)){if(!Files.readString(binding).equals(configSha))throw new IllegalArgumentException("Config changed");}
        else Files.writeString(binding,configSha,StandardOpenOption.CREATE_NEW);
        long preparation=System.nanoTime();var trainData=PairObjectiveDiagnostic.read(trainPath);var valData=PairObjectiveDiagnostic.read(valPath);
        var train=encode(trainData,mode);var validation=encode(valData,mode);double prep=(System.nanoTime()-preparation)/1e9;
        var workspace=new RelationalGraphEnergy.Features();long checksum=0;
        for(int repeat=0;repeat<5;repeat++)for(int i=0;i<2000;i++)checksum+=(long)model.score(valData.boards()[i%validation.length],workspace);
        long began=System.nanoTime();for(int i=0;i<20000;i++)checksum+=(long)model.score(valData.boards()[i%validation.length],workspace);
        double inferenceNs=(System.nanoTime()-began)/20000.0;
        if(inferenceNs>10000) {
            Files.writeString(out.resolve("cost-gate.json"),"{\"full_graph_inference_ns\":"+inferenceNs+",\"status\":\"COST_GATE\"}\n",StandardOpenOption.CREATE_NEW);
            throw new IllegalStateException("Full graph exceeds prospective10us feasibility envelope; no training");
        }
        var metrics=new ArrayList<Object>();metrics.add(Map.of("epoch",0,"validation_mse",mse(model,validation,valData.targets())));
        var gson=new GsonBuilder().setPrettyPrinting().create();long start=System.nanoTime();int epochs=config.get("epochs").getAsInt();
        for(int epoch=1;epoch<=epochs;epoch++) {
            PairObjectiveDiagnostic.permission();Path receipt=out.resolve("epoch-"+epoch+".json"),state=out.resolve("epoch-"+epoch+".state");
            if(Files.exists(receipt)) {
                var r=JsonParser.parseString(Files.readString(receipt)).getAsJsonObject();
                if(!PairObjectiveDiagnostic.sha(state).equals(r.get("state_sha256").getAsString()))throw new IllegalArgumentException("Checkpoint hash");
                try(var in=Files.newInputStream(state)){model=RelationalGraphEnergy.read(in);}metrics.add(r);continue;
            }
            if(Files.exists(state))throw new IllegalArgumentException("Partial graph state preserved; reconcile before retry");
            int[] order=new int[train.length];for(int i=0;i<order.length;i++)order[i]=i;var random=new Random(seed+epoch);
            for(int i=order.length-1;i>0;i--){int j=random.nextInt(i+1),v=order[i];order[i]=order[j];order[j]=v;}
            for(int offset=0;offset<order.length;offset+=128) {
                int n=Math.min(128,order.length-offset);var batch=new RelationalGraphEnergy.Features[n];double[] targets=new double[n];
                for(int j=0;j<n;j++){int i=order[offset+j];batch[j]=train[i];targets[j]=trainData.targets()[i];}model.train(batch,targets);
            }
            try(var stream=Files.newOutputStream(state,StandardOpenOption.CREATE_NEW)){model.write(stream);}
            var r=Map.of("epoch",epoch,"train_mse",mse(model,train,trainData.targets()),"validation_mse",mse(model,validation,valData.targets()),
                "state_sha256",PairObjectiveDiagnostic.sha(state),"updates",model.step);
            Files.writeString(receipt,gson.toJson(r)+"\n",StandardOpenOption.CREATE_NEW);metrics.add(r);
        }
        var result=Map.of("mode",mode.toString(),"seed",seed,"parameters",model.parameters(),"metrics",metrics,
            "preparation_seconds",prep,"optimization_checkpoint_seconds",(System.nanoTime()-start)/1e9,
            "full_graph_inference_ns",inferenceNs,"checksum",checksum,"scope","development pilot; no incremental/search/strength qualification");
        Files.writeString(out.resolve("result.json"),gson.toJson(result)+"\n",StandardOpenOption.CREATE_NEW);System.out.println(gson.toJson(result));
    }
    private GraphEnergyDiagnostic(){}
}
