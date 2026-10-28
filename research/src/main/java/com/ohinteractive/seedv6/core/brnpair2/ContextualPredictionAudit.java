package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import com.ohinteractive.seedv6.core.nnue.conventional.ConventionalNetwork;
import java.nio.file.*;
import java.util.*;

/** Read-only fixed-endpoint neural predictions on a prospectively frozen development panel. */
public final class ContextualPredictionAudit {
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
        var models=new ArrayList<Object>();
        for(var element:config.getAsJsonArray("models")) {
            PairObjectiveDiagnostic.permission();var spec=element.getAsJsonObject();Path state=Path.of(spec.get("state").getAsString());if(!PairCoverageDiagnostic.sha(state).equals(spec.get("state_sha256").getAsString()))throw new IllegalArgumentException("Model identity");
            ContextualAttackNetwork model;try(var input=Files.newInputStream(state)){model=ContextualAttackNetwork.read(input);}var w=new ContextualAttackNetwork.Workspace();double[] logits=new double[data.rows];
            for(int i=0;i<data.rows;i++){data.board(i,board);logits[i]=model.logit(w.encode(board),w);}
            models.add(Map.of("id",spec.get("id").getAsString(),"state_sha256",spec.get("state_sha256").getAsString(),"global",model.global,"updates",model.step,"bt4_mse",mse(logits,data.targets),"teacher_mse",mse(logits,target),"logits",logits));
        }
        var result=Map.of("config_sha256",PairCoverageDiagnostic.sha(configPath),"rows",data.rows,"bt4_targets",data.targets,"teacher_targets",target,"models",models,"seconds",(System.nanoTime()-started)/1e9,
            "scope","Frozen endpoint fresh BRN development confirmation;source pool used for CP64 selection,unknown game correlation,no strength or final-validation claim");
        Files.writeString(out,new GsonBuilder().setPrettyPrinting().create().toJson(result)+"\n",StandardOpenOption.CREATE_NEW);System.out.println("Completed "+models.size()+" models on "+data.rows+" fresh development rows");
    }
    private ContextualPredictionAudit(){}
}
