package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/** R041 fixed one-epoch comparison with atomic segment states and held-out diagnostics. */
public final class TiedGeometryTrainingDiagnostic {
    static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    static boolean compose(String mode){return switch(mode){case "ONE"->false;case "TIED"->true;default->throw new IllegalArgumentException("Shared neural mode");};}
    static TiedGeometryPieceNetwork ablateComposition(TiedGeometryPieceNetwork model) {
        var ablation=new TiedGeometryPieceNetwork(false,model.rate,0);System.arraycopy(model.weights,0,ablation.weights,0,model.weights.length);return ablation;
    }
    static TiedGeometryPieceNetwork checkpoint(Path path,boolean compose,double rate,long step)throws Exception {
        TiedGeometryPieceNetwork model;try(var stream=Files.newInputStream(path)){model=TiedGeometryPieceNetwork.read(stream);}
        if(model.compose!=compose||model.rate!=rate||model.step!=step)throw new IllegalArgumentException("Tied checkpoint semantics");return model;
    }
    static void publishState(Path path,TiedGeometryPieceNetwork model)throws Exception {
        Path temporary=path.resolveSibling(path.getFileName()+"-"+System.nanoTime()+".part");
        try(var stream=Files.newOutputStream(temporary,StandardOpenOption.CREATE_NEW)){model.write(stream);}
        TeacherDataset.publish(temporary,path,path.getParent());
    }
    static void publishJson(Path path,Object value)throws Exception {
        Path temporary=path.resolveSibling(path.getFileName()+"-"+System.nanoTime()+".part");Files.writeString(temporary,GSON.toJson(value)+"\n",StandardOpenOption.CREATE_NEW);TeacherDataset.publish(temporary,path,path.getParent());
    }
    static double[] mse(TiedGeometryPieceNetwork model,PairCoverageDiagnostic.Data data)throws Exception {
        var w=new TiedGeometryPieceNetwork.Workspace();long[] board=new long[6];double full=0,unary=0;
        for(int i=0;i<data.rows;i++) {
            if(i%65536==0)PairObjectiveDiagnostic.permission();data.board(i,board);var f=w.encode(board);
            double d=PairObjectiveDiagnostic.sigmoid(model.compiledLogit(f,w))-data.targets[i],u=PairObjectiveDiagnostic.sigmoid(model.unary(f))-data.targets[i];full+=d*d;unary+=u*u;
        }
        return new double[]{full/data.rows,unary/data.rows};
    }
    static double cost(TiedGeometryPieceNetwork model,PairCoverageDiagnostic.Data data) {
        var w=new TiedGeometryPieceNetwork.Workspace();long[] board=new long[6];double checksum=0;
        for(int i=0;i<10000;i++){data.board(i%data.rows,board);checksum+=model.scoreCp(board,w);}
        long start=System.nanoTime();for(int i=0;i<20000;i++){data.board(i%data.rows,board);checksum+=model.scoreCp(board,w);}double cost=(System.nanoTime()-start)/20000.0;
        if(!Double.isFinite(checksum))throw new ArithmeticException("Cost checksum");return cost;
    }
    static PairCoverageDiagnostic.Data data(JsonObject config,String key,int rows)throws Exception {
        Path path=Path.of(config.get(key).getAsString());if(!PairCoverageDiagnostic.sha(path).equals(config.get(key+"_sha256").getAsString()))throw new IllegalArgumentException("Data identity: "+key);return PairCoverageDiagnostic.Data.read(path,rows);
    }
    public static void main(String[] args)throws Exception {
        TrainingRunGuard.check(args[0]);
        PairObjectiveDiagnostic.permission();Path configPath=Path.of(args[0]);var config=JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();String mode=config.get("mode").getAsString();boolean compose=compose(mode);
        if(config.get("train_positions").getAsInt()!=1048576||config.get("epochs").getAsInt()!=1||config.get("batch").getAsInt()!=128||config.get("width").getAsInt()!=8||config.get("weight_bound").getAsDouble()!=.99||config.get("rate").getAsDouble()!=.01)throw new IllegalArgumentException("Fixed shared recipe");
        long seed=config.get("seed").getAsLong();if(seed!=71&&seed!=97)throw new IllegalArgumentException("Seed");double rate=.01;
        Path out=Path.of(config.get("output").getAsString());Files.createDirectories(out);String configSha=PairCoverageDiagnostic.sha(configPath);Path bound=out.resolve("config.sha256");
        if(Files.exists(bound)){if(!Files.readString(bound).equals(configSha))throw new IllegalArgumentException("Config changed");}else Files.writeString(bound,configSha,StandardOpenOption.CREATE_NEW);
        long loading=System.nanoTime();var train=data(config,"train",1048576);var validation=data(config,"validation",4096);var original=data(config,"original_validation",4096);var played=data(config,"played_validation",1398);double loadSeconds=(System.nanoTime()-loading)/1e9;
        var model=new TiedGeometryPieceNetwork(compose,rate,seed);double preflight=cost(model,validation);if(preflight>30000)throw new IllegalStateException("Gross inference cost gate: "+preflight);
        double initialValidation=mse(model,validation)[0];long start=System.nanoTime();
        for(int segment=1;segment<=8;segment++) {
            PairObjectiveDiagnostic.permission();int begin=(segment-1)*131072,end=segment*131072;Path state=out.resolve("segment-"+segment+".state"),receipt=out.resolve("segment-"+segment+".json");
            if(Files.exists(receipt)) {
                var saved=JsonParser.parseString(Files.readString(receipt)).getAsJsonObject();if(saved.get("completed_positions").getAsInt()!=end||!saved.get("config_sha256").getAsString().equals(configSha)||!PairCoverageDiagnostic.sha(state).equals(saved.get("state_sha256").getAsString()))throw new IllegalArgumentException("Segment binding");
                model=checkpoint(state,compose,rate,end/128);continue;
            }
            if(Files.exists(state))model=checkpoint(state,compose,rate,end/128);
            else{TiedGeometryPieceDiagnostic.segment(model,train,seed,1,begin,end);publishState(state,model);}
            publishJson(receipt,Map.of("completed_positions",end,"updates",model.step,"config_sha256",configSha,"state_sha256",PairCoverageDiagnostic.sha(state)));
            System.out.println("Completed segment="+segment+" positions="+end);System.out.flush();
        }
        Path state=out.resolve("epoch-1.state"),receipt=out.resolve("epoch-1.json");if(Files.exists(state))model=checkpoint(state,compose,rate,8192);else publishState(state,model);
        double[] tr=mse(model,train),val=mse(model,validation);var epoch=Map.of("epoch",1,"updates",model.step,"train_mse",tr[0],"unary_only_train_mse",tr[1],"validation_mse",val[0],"unary_only_validation_mse",val[1],"state_sha256",PairCoverageDiagnostic.sha(state));
        if(Files.exists(receipt)){if(!JsonParser.parseString(Files.readString(receipt)).equals(GSON.toJsonTree(epoch)))throw new IllegalArgumentException("Epoch evidence changed");}else publishJson(receipt,epoch);
        var ablation=ablateComposition(model);double[] newMse=mse(model,played);var w=new TiedGeometryPieceNetwork.Workspace();long[] board=new long[6];double[] logits=new double[played.rows];
        for(int i=0;i<played.rows;i++){played.board(i,board);logits[i]=model.compiledLogit(w.encode(board),w);}
        var result=new LinkedHashMap<String,Object>();result.put("mode",mode);result.put("seed",seed);result.put("parameters",TiedGeometryPieceNetwork.PARAMETERS);result.put("metrics",List.of(Map.of("epoch",0,"validation_mse",initialValidation),epoch));
        result.put("load_seconds",loadSeconds);result.put("optimization_checkpoint_seconds",(System.nanoTime()-start)/1e9);result.put("preflight_inference_ns",preflight);result.put("full_inference_ns",cost(model,validation));
        result.put("original_bt4_validation_mse",mse(model,original)[0]);result.put("played_validation_mse",newMse[0]);result.put("played_unary_only_mse",newMse[1]);
        result.put("second_pass_disabled_validation_mse",mse(ablation,validation)[0]);result.put("second_pass_disabled_played_validation_mse",mse(ablation,played)[0]);result.put("played_validation_logits",logits);
        result.put("scope","Tied shared neural relation recurrence;fixed endpoint;played development held out;no strength claim");publishJson(out.resolve("result.json"),result);
        System.out.println("Completed mode="+mode+" seed="+seed+" oldTeacher="+val[0]+" playedTeacher="+newMse[0]);
    }
    private TiedGeometryTrainingDiagnostic(){}
}
