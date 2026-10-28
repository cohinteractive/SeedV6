package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/** Identical neural updates with batch-local encoding of a bounded flat dataset. */
public final class ContextualPieceDiagnostic {
    static void segment(ContextualPieceNetwork model,PairCoverageDiagnostic.Data data,long seed,int epoch,int begin,int end) {
        if(begin<0||begin%128!=0||end>data.rows||end<=begin||(end!=data.rows&&end%128!=0))throw new IllegalArgumentException("Segment bounds");
        int[] order=new int[data.rows];for(int i=0;i<order.length;i++)order[i]=i;var random=new Random(seed+epoch);
        for(int i=order.length-1;i>0;i--){int j=random.nextInt(i+1),v=order[i];order[i]=order[j];order[j]=v;}
        var w=new ContextualPieceNetwork.Workspace();long[] board=new long[PairCoverageDiagnostic.Data.STRIDE];
        var buffer=new ContextualPieceNetwork.Features[128];for(int i=0;i<buffer.length;i++)buffer[i]=new ContextualPieceNetwork.Features();double[] targetBuffer=new double[128];
        for(int offset=begin;offset<end;offset+=128) {
            int n=Math.min(128,end-offset);var batch=n==128?buffer:Arrays.copyOf(buffer,n);var targets=n==128?targetBuffer:new double[n];
            for(int j=0;j<n;j++){int i=order[offset+j];data.board(i,board);batch[j].copyFrom(w.encode(board));targets[j]=data.targets[i];}
            model.train(batch,targets);
        }
    }
    static double[] mse(ContextualPieceNetwork model,PairCoverageDiagnostic.Data data) {
        var w=new ContextualPieceNetwork.Workspace();long[] board=new long[PairCoverageDiagnostic.Data.STRIDE];double full=0,masked=0,secondMasked=0;
        for(int i=0;i<data.rows;i++) {
            data.board(i,board);var f=w.encode(board);double d=PairObjectiveDiagnostic.sigmoid(model.logit(f,w))-data.targets[i],u=PairObjectiveDiagnostic.sigmoid(model.unary(f))-data.targets[i];
            full+=d*d;masked+=u*u;double base=PairObjectiveDiagnostic.sigmoid(w.baseLogit)-data.targets[i];secondMasked+=base*base;
        }
        return new double[]{full/data.rows,masked/data.rows,secondMasked/data.rows};
    }
    public static void main(String[] args)throws Exception {
        PairObjectiveDiagnostic.permission();Path configPath=Path.of(args[0]);var config=JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();
        String mode=config.get("mode").getAsString();if(!mode.equals("ONE")&&!mode.equals("TWO"))throw new IllegalArgumentException("Mode");
        if(config.get("epochs").getAsInt()!=1||config.get("batch").getAsInt()!=128||config.get("width").getAsInt()!=8||config.get("weight_decay").getAsDouble()!=.01||config.get("weight_bound").getAsDouble()!=.99)throw new IllegalArgumentException("Recipe");
        long seed=config.get("seed").getAsLong();double rate=config.get("rate").getAsDouble();var model=new ContextualPieceNetwork(mode.equals("TWO"),rate,seed);
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
        var gson=new GsonBuilder().setPrettyPrinting().create();var metrics=new ArrayList<Object>();metrics.add(Map.of("epoch",0,"validation_mse",mse(model,val)[0]));long start=System.nanoTime();
        for(int epoch=1;epoch<=config.get("epochs").getAsInt();epoch++) {
            PairObjectiveDiagnostic.permission();Path state=out.resolve("epoch-"+epoch+".state"),receipt=out.resolve("epoch-"+epoch+".json");
            if(Files.exists(receipt)) {
                var r=JsonParser.parseString(Files.readString(receipt)).getAsJsonObject();if(!PairCoverageDiagnostic.sha(state).equals(r.get("state_sha256").getAsString()))throw new IllegalArgumentException("Checkpoint identity");
                try(var input=Files.newInputStream(state)){model=ContextualPieceNetwork.read(input);}if(model.compose!=mode.equals("TWO")||model.rate!=rate)throw new IllegalArgumentException("Resume semantics");metrics.add(r);continue;
            }
            if(Files.exists(state))throw new IllegalArgumentException("Partial neural checkpoint preserved; reconcile before retry");
            for(int begin=0,segment=1;begin<train.rows;begin+=131072,segment++) {
                PairObjectiveDiagnostic.permission();int end=Math.min(begin+131072,train.rows);
                Path part=out.resolve("segment-"+segment+".state"),partReceipt=out.resolve("segment-"+segment+".json");
                if(Files.exists(partReceipt)) {
                    var saved=JsonParser.parseString(Files.readString(partReceipt)).getAsJsonObject();
                    if(saved.get("completed_positions").getAsInt()!=end||!saved.get("config_sha256").getAsString().equals(configSha)||!PairCoverageDiagnostic.sha(part).equals(saved.get("state_sha256").getAsString()))throw new IllegalArgumentException("Segment identity");
                    try(var input=Files.newInputStream(part)){model=ContextualPieceNetwork.read(input);}
                    if(model.compose!=mode.equals("TWO")||model.rate!=rate||model.step!=(end+127)/128)throw new IllegalArgumentException("Segment semantics");
                    continue;
                }
                if(Files.exists(part))throw new IllegalArgumentException("Partial segment checkpoint preserved;reconcile before retry");
                segment(model,train,seed,epoch,begin,end);
                try(var output=Files.newOutputStream(part,StandardOpenOption.CREATE_NEW)){model.write(output);}
                Files.writeString(partReceipt,gson.toJson(Map.of("completed_positions",end,"updates",model.step,"config_sha256",configSha,"state_sha256",PairCoverageDiagnostic.sha(part)))+"\n",StandardOpenOption.CREATE_NEW);
                System.out.println("Completed segment="+segment+" positions="+end);System.out.flush();
            }
            double[] validation=mse(model,val),training=mse(model,train);
            try(var output=Files.newOutputStream(state,StandardOpenOption.CREATE_NEW)){model.write(output);}
            var r=Map.of("epoch",epoch,"updates",model.step,"train_mse",training[0],"unary_only_train_mse",training[1],"validation_mse",validation[0],"unary_only_validation_mse",validation[1],"second_pass_masked_validation_mse",validation[2],"second_pass_masked_train_mse",training[2],"state_sha256",PairCoverageDiagnostic.sha(state));
            Files.writeString(receipt,gson.toJson(r)+"\n",StandardOpenOption.CREATE_NEW);metrics.add(r);
        }
        var result=new LinkedHashMap<String,Object>(Map.of("mode",mode,"seed",seed,"rate",rate,"train_positions",train.rows,"metrics",metrics,"load_seconds",loadSeconds,
            "optimization_checkpoint_seconds",(System.nanoTime()-start)/1e9,"scope","Contextual all-piece message diagnosis;no search or strength qualification"));result.put("full_inference_ns",cost);result.put("parameters",ContextualPieceNetwork.PARAMETERS);result.put("active_parameters",model.compose?ContextualPieceNetwork.PARAMETERS:ContextualPieceNetwork.S);
        result.put("original_bt4_validation_mse",mse(model,original)[0]);
        Files.writeString(out.resolve("result.json"),gson.toJson(result)+"\n",StandardOpenOption.CREATE_NEW);System.out.println(gson.toJson(result));
    }
    private ContextualPieceDiagnostic(){}
}
