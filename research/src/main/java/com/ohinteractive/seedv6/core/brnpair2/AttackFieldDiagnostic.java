package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/** Identical neural updates with batch-local encoding of a bounded flat dataset. */
public final class AttackFieldDiagnostic {
    static void epoch(AttackFieldNetwork model,PairCoverageDiagnostic.Data data,long seed,int epoch) {
        int[] order=new int[data.rows];for(int i=0;i<order.length;i++)order[i]=i;var random=new Random(seed+epoch);
        for(int i=order.length-1;i>0;i--){int j=random.nextInt(i+1),v=order[i];order[i]=order[j];order[j]=v;}
        var w=new AttackFieldNetwork.Workspace();long[] board=new long[PairCoverageDiagnostic.Data.STRIDE];
        var buffer=new AttackFieldNetwork.Features[128];for(int i=0;i<buffer.length;i++)buffer[i]=new AttackFieldNetwork.Features();double[] targetBuffer=new double[128];
        for(int offset=0;offset<order.length;offset+=128) {
            int n=Math.min(128,order.length-offset);var batch=n==128?buffer:Arrays.copyOf(buffer,n);var targets=n==128?targetBuffer:new double[n];
            for(int j=0;j<n;j++){int i=order[offset+j];data.board(i,board);batch[j].copyFrom(w.encode(board,model.allSquares));targets[j]=data.targets[i];}
            model.train(batch,targets);
        }
    }
    static double[] mse(AttackFieldNetwork model,PairCoverageDiagnostic.Data data) {
        var w=new AttackFieldNetwork.Workspace();long[] board=new long[PairCoverageDiagnostic.Data.STRIDE];double full=0,masked=0;
        for(int i=0;i<data.rows;i++) {
            data.board(i,board);var f=w.encode(board,model.allSquares);double d=PairObjectiveDiagnostic.sigmoid(model.logit(f,w))-data.targets[i],u=PairObjectiveDiagnostic.sigmoid(model.unary(f))-data.targets[i];
            full+=d*d;masked+=u*u;
        }
        return new double[]{full/data.rows,masked/data.rows};
    }
    public static void main(String[] args)throws Exception {
        PairObjectiveDiagnostic.permission();Path configPath=Path.of(args[0]);var config=JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();
        String mode=config.get("mode").getAsString();if(!mode.equals("OCCUPIED")&&!mode.equals("ALL"))throw new IllegalArgumentException("Mode");
        long seed=config.get("seed").getAsLong();double rate=config.get("rate").getAsDouble();var model=new AttackFieldNetwork(mode.equals("ALL"),rate,seed);
        Path trainPath=Path.of(config.get("train").getAsString()),valPath=Path.of(config.get("validation").getAsString()),out=Path.of(config.get("output").getAsString());
        if(!PairCoverageDiagnostic.sha(trainPath).equals(config.get("train_sha256").getAsString())||!PairCoverageDiagnostic.sha(valPath).equals(config.get("validation_sha256").getAsString()))throw new IllegalArgumentException("Data identity");
        Files.createDirectories(out);String configSha=PairCoverageDiagnostic.sha(configPath);Path bound=out.resolve("config.sha256");
        if(Files.exists(bound)){if(!Files.readString(bound).equals(configSha))throw new IllegalArgumentException("Config changed");}else Files.writeString(bound,configSha,StandardOpenOption.CREATE_NEW);
        long loading=System.nanoTime();var train=PairCoverageDiagnostic.Data.read(trainPath,config.get("train_positions").getAsInt());var val=PairCoverageDiagnostic.Data.read(valPath,4096);double loadSeconds=(System.nanoTime()-loading)/1e9;
        var workspace=new AttackFieldNetwork.Workspace();long[] sample=new long[PairCoverageDiagnostic.Data.STRIDE];double checksum=0;
        for(int i=0;i<10000;i++){val.board(i%val.rows,sample);checksum+=model.scoreCp(sample,workspace);}
        long profiling=System.nanoTime();for(int i=0;i<20000;i++){val.board(i%val.rows,sample);checksum+=model.scoreCp(sample,workspace);}
        double cost=(System.nanoTime()-profiling)/20000.0;System.out.println("Full inference ns="+cost+" checksum="+checksum);
        if(cost>10000)throw new IllegalStateException("Full neural inference cost gate: "+cost);
        Path originalPath=Path.of(config.get("original_validation").getAsString());if(!PairCoverageDiagnostic.sha(originalPath).equals(config.get("original_validation_sha256").getAsString()))throw new IllegalArgumentException("Original target identity");
        var original=PairCoverageDiagnostic.Data.read(originalPath,4096);
        var gson=new GsonBuilder().setPrettyPrinting().create();var metrics=new ArrayList<Object>();metrics.add(Map.of("epoch",0,"validation_mse",mse(model,val)[0]));long start=System.nanoTime();
        for(int epoch=1;epoch<=config.get("epochs").getAsInt();epoch++) {
            PairObjectiveDiagnostic.permission();Path state=out.resolve("epoch-"+epoch+".state"),receipt=out.resolve("epoch-"+epoch+".json");
            if(Files.exists(receipt)) {
                var r=JsonParser.parseString(Files.readString(receipt)).getAsJsonObject();if(!PairCoverageDiagnostic.sha(state).equals(r.get("state_sha256").getAsString()))throw new IllegalArgumentException("Checkpoint identity");
                try(var input=Files.newInputStream(state)){model=AttackFieldNetwork.read(input);}if(model.allSquares!=mode.equals("ALL")||model.rate!=rate)throw new IllegalArgumentException("Resume semantics");metrics.add(r);continue;
            }
            if(Files.exists(state))throw new IllegalArgumentException("Partial neural checkpoint preserved; reconcile before retry");
            epoch(model,train,seed,epoch);double[] validation=mse(model,val),training=mse(model,train);
            try(var output=Files.newOutputStream(state,StandardOpenOption.CREATE_NEW)){model.write(output);}
            var r=Map.of("epoch",epoch,"updates",model.step,"train_mse",training[0],"unary_only_train_mse",training[1],"validation_mse",validation[0],"unary_only_validation_mse",validation[1],"state_sha256",PairCoverageDiagnostic.sha(state));
            Files.writeString(receipt,gson.toJson(r)+"\n",StandardOpenOption.CREATE_NEW);metrics.add(r);
        }
        var result=new LinkedHashMap<String,Object>(Map.of("mode",mode,"seed",seed,"rate",rate,"train_positions",train.rows,"metrics",metrics,"load_seconds",loadSeconds,
            "optimization_checkpoint_seconds",(System.nanoTime()-start)/1e9,"scope","Matched occupied/all-square neural attack field diagnosis; no search or strength qualification"));result.put("full_inference_ns",cost);result.put("parameters",AttackFieldNetwork.PARAMETERS);result.put("active_parameters",model.allSquares?AttackFieldNetwork.PARAMETERS:AttackFieldNetwork.PARAMETERS-2328);
        result.put("original_bt4_validation_mse",mse(model,original)[0]);
        Files.writeString(out.resolve("result.json"),gson.toJson(result)+"\n",StandardOpenOption.CREATE_NEW);System.out.println(gson.toJson(result));
    }
    private AttackFieldDiagnostic(){}
}
