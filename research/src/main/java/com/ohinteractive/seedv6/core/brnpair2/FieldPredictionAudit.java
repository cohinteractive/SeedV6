package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import com.ohinteractive.seedv6.core.nnue.conventional.ConventionalNetwork;
import java.nio.file.*;
import java.util.*;

/** Read-only fixed-endpoint neural predictions on a prospectively frozen development panel. */
public final class FieldPredictionAudit {
    static double mse(double[] logits,double[] targets) {
        if(logits.length==0||logits.length!=targets.length)throw new IllegalArgumentException("Panel length");
        double sum=0;for(int i=0;i<logits.length;i++){double d=PairObjectiveDiagnostic.sigmoid(logits[i])-targets[i];sum+=d*d;}return sum/logits.length;
    }
    public static void main(String[] args)throws Exception {
        PairObjectiveDiagnostic.permission();Path configPath=Path.of(args[0]);var config=JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();
        Path cases=Path.of(config.get("cases").getAsString()),teacherPath=Path.of(config.get("teacher").getAsString()),out=Path.of(config.get("output").getAsString());
        if(!PairCoverageDiagnostic.sha(cases).equals(config.get("cases_sha256").getAsString())||!PairCoverageDiagnostic.sha(teacherPath).equals(config.get("teacher_sha256").getAsString()))throw new IllegalArgumentException("Input identity");
        var data=PairCoverageDiagnostic.Data.read(cases,config.get("rows").getAsInt());var teacher=ConventionalNetwork.load(teacherPath);long[] board=new long[PairCoverageDiagnostic.Data.STRIDE];double[] target=new double[data.rows];
        long started=System.nanoTime();for(int i=0;i<data.rows;i++){if(i%512==0)PairObjectiveDiagnostic.permission();data.board(i,board);target[i]=PairObjectiveDiagnostic.sigmoid(teacher.fullRebuild(board)/400.0);}
        int[] pieces=new int[data.rows];for(int i=0;i<data.rows;i++){data.board(i,board);pieces[i]=Long.bitCount(board[0]|board[1]|board[2]);}
        var models=new ArrayList<Object>();
        for(var element:config.getAsJsonArray("models")) {
            PairObjectiveDiagnostic.permission();var spec=element.getAsJsonObject();Path state=Path.of(spec.get("state").getAsString());if(!PairCoverageDiagnostic.sha(state).equals(spec.get("state_sha256").getAsString()))throw new IllegalArgumentException("Model identity");
            var predictor=load(state,spec.get("kind").getAsString());double[] logits=new double[data.rows];
            for(int i=0;i<data.rows;i++){data.board(i,board);logits[i]=predictor.applyAsDouble(board);}
            models.add(Map.of("id",spec.get("id").getAsString(),"kind",spec.get("kind").getAsString(),"state_sha256",spec.get("state_sha256").getAsString(),"bt4_mse",mse(logits,data.targets),"teacher_mse",mse(logits,target),"logits",logits));
        }
        var result=Map.of("config_sha256",PairCoverageDiagnostic.sha(configPath),"rows",data.rows,"bt4_targets",data.targets,"teacher_targets",target,"models",models,"pieces",pieces,"seconds",(System.nanoTime()-started)/1e9,
            "scope","Frozen endpoint consumed BRN development cross-check;source pool used for CP64 selection,unknown game correlation,no strength or final-validation claim");
        Files.writeString(out,new GsonBuilder().setPrettyPrinting().create().toJson(result)+"\n",StandardOpenOption.CREATE_NEW);System.out.println("Completed "+models.size()+" models on "+data.rows+" development rows");
    }
    static java.util.function.ToDoubleFunction<long[]> load(Path state,String kind)throws Exception {
        try(var input=Files.newInputStream(state)) {
            return switch(kind) {
                case "ALL" -> {var m=AttackFieldNetwork.read(input);if(!m.allSquares)throw new IllegalArgumentException("ALL mode required");var w=new AttackFieldNetwork.Workspace();yield b->m.logit(w.encode(b,true),w);}
                case "COMPOSE8" -> {var m=ComposedAttackFieldNetwork.read(input);if(!m.compose)throw new IllegalArgumentException("COMPOSE mode required");var w=new ComposedAttackFieldNetwork.Workspace();yield b->m.logit(w.encode(b,true),w);}
                case "COMPOSE16" -> {var m=WideComposedAttackFieldNetwork.read(input);if(!m.compose)throw new IllegalArgumentException("COMPOSE mode required");var w=new WideComposedAttackFieldNetwork.Workspace();yield b->m.logit(w.encode(b,true),w);}
                case "PAIR" -> {var m=BrnPair2Codec.readModel(input);var w=m.newWorkspace();yield b->w.evaluatePawns(b,.25)/4;}
                default -> throw new IllegalArgumentException("Unknown neural/reference kind");
            };
        }
    }
    private FieldPredictionAudit(){}
}
