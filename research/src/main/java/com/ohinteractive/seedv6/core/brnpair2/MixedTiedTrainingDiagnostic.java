package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/** R045 fixed two-stage neural comparison with atomic segment states and held-out diagnostics. */
public final class MixedTiedTrainingDiagnostic {
    static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    static boolean mixing(String mode){return switch(mode){case "CONTROL"->false;case "MIX"->true;default->throw new IllegalArgumentException("Shared neural mode");};}
    static MixedTiedPieceNetwork ablateMix(MixedTiedPieceNetwork model) {
        var ablation=new MixedTiedPieceNetwork(false,model.rate,0);System.arraycopy(model.weights,0,ablation.weights,0,model.weights.length);return ablation;
    }
    static MixedTiedPieceNetwork checkpoint(Path path,boolean compose,double rate,long step)throws Exception {
        MixedTiedPieceNetwork model;try(var stream=Files.newInputStream(path)){model=MixedTiedPieceNetwork.read(stream);}
        if(model.mix!=compose||model.rate!=rate||model.step!=step)throw new IllegalArgumentException("Tied checkpoint semantics");return model;
    }
    static void publishState(Path path,MixedTiedPieceNetwork model)throws Exception {
        Path temporary=path.resolveSibling(path.getFileName()+"-"+System.nanoTime()+".part");
        try(var stream=Files.newOutputStream(temporary,StandardOpenOption.CREATE_NEW)){model.write(stream);}
        TeacherDataset.publish(temporary,path,path.getParent());
    }
    static void publishJson(Path path,Object value)throws Exception {
        Path temporary=path.resolveSibling(path.getFileName()+"-"+System.nanoTime()+".part");Files.writeString(temporary,GSON.toJson(value)+"\n",StandardOpenOption.CREATE_NEW);TeacherDataset.publish(temporary,path,path.getParent());
    }
    static double[] mse(MixedTiedPieceNetwork model,PairCoverageDiagnostic.Data data)throws Exception {
        var w=new MixedTiedPieceNetwork.Workspace();long[] board=new long[6];double full=0,unary=0;
        for(int i=0;i<data.rows;i++) {
            if(i%65536==0)PairObjectiveDiagnostic.permission();data.board(i,board);var f=w.encode(board);
            double d=PairObjectiveDiagnostic.sigmoid(model.compiledLogit(f,w))-data.targets[i],u=PairObjectiveDiagnostic.sigmoid(model.unary(f))-data.targets[i];full+=d*d;unary+=u*u;
        }
        return new double[]{full/data.rows,unary/data.rows};
    }
    static double cost(MixedTiedPieceNetwork model,PairCoverageDiagnostic.Data data) {
        var w=new MixedTiedPieceNetwork.Workspace();long[] board=new long[6];double checksum=0;
        for(int i=0;i<10000;i++){data.board(i%data.rows,board);checksum+=model.scoreCp(board,w);}
        long start=System.nanoTime();for(int i=0;i<20000;i++){data.board(i%data.rows,board);checksum+=model.scoreCp(board,w);}double cost=(System.nanoTime()-start)/20000.0;
        if(!Double.isFinite(checksum))throw new ArithmeticException("Cost checksum");return cost;
    }
    static PairCoverageDiagnostic.Data data(JsonObject config,String key,int rows)throws Exception {
        Path path=Path.of(config.get(key).getAsString());if(!PairCoverageDiagnostic.sha(path).equals(config.get(key+"_sha256").getAsString()))throw new IllegalArgumentException("Data identity: "+key);return PairCoverageDiagnostic.Data.read(path,rows);
    }
    public static void main(String[] args)throws Exception {
        TrainingRunGuard.check(args[0]);
        PairObjectiveDiagnostic.permission();Path configPath=Path.of(args[0]);var config=JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();String mode=config.get("mode").getAsString();boolean compose=mixing(mode);
        if(config.get("train_positions").getAsInt()!=1048576||config.get("epochs").getAsInt()!=2||config.get("batch").getAsInt()!=128||config.get("width").getAsInt()!=8||config.get("weight_bound").getAsDouble()!=.99||config.get("rate").getAsDouble()!=.01)throw new IllegalArgumentException("Fixed shared recipe");
        long seed=config.get("seed").getAsLong();if(seed!=71&&seed!=97)throw new IllegalArgumentException("Seed");double rate=.01;
        Path out=Path.of(config.get("output").getAsString());Files.createDirectories(out);String configSha=PairCoverageDiagnostic.sha(configPath);Path bound=out.resolve("config.sha256");
        if(Files.exists(bound)){if(!Files.readString(bound).equals(configSha))throw new IllegalArgumentException("Config changed");}else Files.writeString(bound,configSha,StandardOpenOption.CREATE_NEW);
        long loading=System.nanoTime();var train=data(config,"train",1048576);var validation=data(config,"validation",4096);var original=data(config,"original_validation",4096);var played=data(config,"played_validation",1398);double loadSeconds=(System.nanoTime()-loading)/1e9;
        var model=new MixedTiedPieceNetwork(compose,rate,seed);double preflight=cost(model,validation);if(preflight>30000)throw new IllegalStateException("Gross inference cost gate: "+preflight);
        double initialValidation=mse(model,validation)[0];long start=System.nanoTime();
        var metrics=new ArrayList<Object>();metrics.add(Map.of("epoch",0,"validation_mse",initialValidation));
        for(int epochNumber=1;epochNumber<=2;epochNumber++) {
            double epochRate=epochNumber==1?.01:.001;if(epochNumber==2)model=MixedTiedPieceDiagnostic.fork(model,epochRate);
            Path endpoint=out.resolve("epoch-"+epochNumber+".state"),epochReceipt=out.resolve("epoch-"+epochNumber+".json");
            if(Files.exists(epochReceipt)) {
                var saved=JsonParser.parseString(Files.readString(epochReceipt)).getAsJsonObject();
                if(saved.get("epoch").getAsInt()!=epochNumber||saved.get("updates").getAsLong()!=epochNumber*8192L||!PairCoverageDiagnostic.sha(endpoint).equals(saved.get("state_sha256").getAsString()))throw new IllegalArgumentException("Epoch binding");
                model=checkpoint(endpoint,compose,epochRate,epochNumber*8192L);metrics.add(saved);continue;
            }
            for(int local=1;local<=8;local++) {
                PairObjectiveDiagnostic.permission();int begin=(local-1)*131072,end=local*131072,segment=(epochNumber-1)*8+local,completed=(epochNumber-1)*1048576+end;
                Path state=out.resolve("segment-"+segment+".state"),receipt=out.resolve("segment-"+segment+".json");
                if(Files.exists(receipt)) {
                    var saved=JsonParser.parseString(Files.readString(receipt)).getAsJsonObject();
                    if(saved.get("completed_positions").getAsInt()!=completed||!saved.get("config_sha256").getAsString().equals(configSha)||!PairCoverageDiagnostic.sha(state).equals(saved.get("state_sha256").getAsString()))throw new IllegalArgumentException("Segment binding");
                    model=checkpoint(state,compose,epochRate,completed/128);continue;
                }
                if(Files.exists(state))model=checkpoint(state,compose,epochRate,completed/128);
                else{MixedTiedPieceDiagnostic.segment(model,train,seed,epochNumber,begin,end);publishState(state,model);}
                publishJson(receipt,Map.of("completed_positions",completed,"epoch",epochNumber,"updates",model.step,"config_sha256",configSha,"state_sha256",PairCoverageDiagnostic.sha(state)));
                System.out.println("Completed segment="+segment+" positions="+completed);System.out.flush();
            }
            if(Files.exists(endpoint))model=checkpoint(endpoint,compose,epochRate,epochNumber*8192L);else publishState(endpoint,model);
            double[] tr=mse(model,train),val=mse(model,validation);var epoch=Map.of("epoch",epochNumber,"updates",model.step,"train_mse",tr[0],"unary_only_train_mse",tr[1],"validation_mse",val[0],"unary_only_validation_mse",val[1],"state_sha256",PairCoverageDiagnostic.sha(endpoint));
            publishJson(epochReceipt,epoch);metrics.add(epoch);
        }
        var ablation=ablateMix(model);double[] newMse=mse(model,played);var w=new MixedTiedPieceNetwork.Workspace();long[] board=new long[6];double[] logits=new double[played.rows];
        for(int i=0;i<played.rows;i++){played.board(i,board);logits[i]=model.compiledLogit(w.encode(board),w);}
        var result=new LinkedHashMap<String,Object>();result.put("mode",mode);result.put("seed",seed);result.put("parameters",MixedTiedPieceNetwork.PARAMETERS);result.put("metrics",metrics);
        result.put("load_seconds",loadSeconds);result.put("optimization_checkpoint_seconds",(System.nanoTime()-start)/1e9);result.put("preflight_inference_ns",preflight);result.put("full_inference_ns",cost(model,validation));
        result.put("original_bt4_validation_mse",mse(model,original)[0]);result.put("played_validation_mse",newMse[0]);result.put("played_unary_only_mse",newMse[1]);
        result.put("mix_disabled_validation_mse",mse(ablation,validation)[0]);result.put("mix_disabled_played_validation_mse",mse(ablation,played)[0]);result.put("played_validation_logits",logits);
        result.put("scope","Learned cross-channel tied recurrence;fixed epoch2;played development held out;no strength claim");publishJson(out.resolve("result.json"),result);
        System.out.println("Completed mode="+mode+" seed="+seed+" oldTeacher="+mse(model,validation)[0]+" playedTeacher="+newMse[0]);
    }
    private MixedTiedTrainingDiagnostic(){}
}
