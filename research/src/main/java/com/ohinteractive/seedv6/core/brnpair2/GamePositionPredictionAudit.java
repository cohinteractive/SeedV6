package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.conventional.ConventionalNetwork;
import java.nio.file.*;
import java.util.*;

/** Frozen evaluator comparison on independently replayed legal played roots. */
public final class GamePositionPredictionAudit {
    public static void main(String[] args)throws Exception {
        PairObjectiveDiagnostic.permission();Path configPath=Path.of(args[0]);var config=JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();
        Path cases=Path.of(config.get("cases").getAsString()),teacherPath=Path.of(config.get("teacher").getAsString()),out=Path.of(config.get("output").getAsString());
        if(!PairCoverageDiagnostic.sha(cases).equals(config.get("cases_sha256").getAsString())||!PairCoverageDiagnostic.sha(teacherPath).equals(config.get("teacher_sha256").getAsString()))throw new IllegalArgumentException("Input identity");
        var positions=JsonParser.parseString(Files.readString(cases)).getAsJsonObject().getAsJsonArray("positions");int rows=positions.size();if(rows!=config.get("rows").getAsInt()||rows<1||rows>1024)throw new IllegalArgumentException("Rows");
        long[][] boards=new long[rows][];int[] pieces=new int[rows],cp=new int[rows];boolean[] check=new boolean[rows];double[] target=new double[rows];var teacher=ConventionalNetwork.load(teacherPath);long started=System.nanoTime();
        for(int i=0;i<rows;i++) {
            var position=positions.get(i).getAsJsonObject();boards[i]=Board.fromFen(position.get("fen").getAsString());pieces[i]=Long.bitCount(boards[i][0]|boards[i][1]|boards[i][2]);
            if(pieces[i]!=position.get("pieces").getAsInt())throw new IllegalArgumentException("Board interpretation");check[i]=position.get("in_check").getAsBoolean();cp[i]=teacher.fullRebuild(boards[i]);target[i]=PairObjectiveDiagnostic.sigmoid(cp[i]/400.0);
        }
        var models=new ArrayList<Object>();
        for(var element:config.getAsJsonArray("models")) {
            PairObjectiveDiagnostic.permission();var spec=element.getAsJsonObject();Path state=Path.of(spec.get("state").getAsString());if(!PairCoverageDiagnostic.sha(state).equals(spec.get("state_sha256").getAsString()))throw new IllegalArgumentException("Model identity");
            var predictor=FieldPredictionAudit.load(state,spec.get("kind").getAsString());double[] logits=new double[rows];for(int i=0;i<rows;i++)logits[i]=predictor.applyAsDouble(boards[i]);
            models.add(Map.of("id",spec.get("id").getAsString(),"kind",spec.get("kind").getAsString(),"state_sha256",spec.get("state_sha256").getAsString(),"teacher_mse",FieldPredictionAudit.mse(logits,target),"logits",logits));
        }
        var result=Map.of("config_sha256",PairCoverageDiagnostic.sha(configPath),"rows",rows,"teacher_cp",cp,"teacher_targets",target,"pieces",pieces,"in_check",check,"models",models,"seconds",(System.nanoTime()-started)/1e9,
                "scope","Consumed played-root diagnostic;CP64 is a reference,not ground truth;no fit,game or strength claim");
        Files.writeString(out,new GsonBuilder().setPrettyPrinting().create().toJson(result)+"\n",StandardOpenOption.CREATE_NEW);System.out.println("Completed "+models.size()+" models on "+rows+" played roots");
    }
    private GamePositionPredictionAudit(){}
}
