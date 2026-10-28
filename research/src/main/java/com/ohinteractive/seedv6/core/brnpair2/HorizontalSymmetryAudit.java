package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import com.ohinteractive.seedv6.core.nnue.conventional.ConventionalNetwork;
import java.nio.file.*;
import java.util.*;

/** Read-only file-reflection diagnostic; rule-state/standard-castling symmetry is not claimed. */
public final class HorizontalSymmetryAudit {
    public static void main(String[] args)throws Exception {
        PairObjectiveDiagnostic.permission();Path configPath=Path.of(args[0]);var config=JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();
        Path cases=Path.of(config.get("cases").getAsString()),teacherPath=Path.of(config.get("teacher").getAsString());
        if(!PairCoverageDiagnostic.sha(cases).equals(config.get("cases_sha256").getAsString())||!PairCoverageDiagnostic.sha(teacherPath).equals(config.get("teacher_sha256").getAsString()))throw new IllegalArgumentException("Input identity");
        var data=PairCoverageDiagnostic.Data.read(cases,config.get("rows").getAsInt());var teacher=ConventionalNetwork.load(teacherPath);long[] board=new long[6],reflected=new long[6];int[] teacherCp=new int[data.rows];int maxTeacher=0;
        for(int i=0;i<data.rows;i++){data.board(i,board);HorizontalScoreView.reflect(board,reflected);teacherCp[i]=teacher.fullRebuild(board);maxTeacher=Math.max(maxTeacher,Math.abs(teacherCp[i]-teacher.fullRebuild(reflected)));}
        var models=new ArrayList<Object>();
        for(var element:config.getAsJsonArray("models")) {
            PairObjectiveDiagnostic.permission();var spec=element.getAsJsonObject();Path path=Path.of(spec.get("state").getAsString());if(!PairCoverageDiagnostic.sha(path).equals(spec.get("state_sha256").getAsString()))throw new IllegalArgumentException("Model identity");
            AttackFieldNetwork model;try(var input=Files.newInputStream(path)){model=AttackFieldNetwork.read(input);}if(!model.allSquares)throw new IllegalArgumentException("Only frozen ALL controls");
            var w=new AttackFieldNetwork.Workspace();double[] original=new double[data.rows],mirror=new double[data.rows],loss=new double[3];double square=0,max=0;
            for(int i=0;i<data.rows;i++) {
                data.board(i,board);HorizontalScoreView.reflect(board,reflected);double a=model.scoreCp(board,w),b=model.scoreCp(reflected,w);original[i]=a;mirror[i]=b;square+=(a-b)*(a-b);max=Math.max(max,Math.abs(a-b));double target=PairObjectiveDiagnostic.sigmoid(teacherCp[i]/400.0);
                double[] predictions={a,b,(a+b)/2};for(int j=0;j<3;j++){double d=PairObjectiveDiagnostic.sigmoid(predictions[j]/400)-target;loss[j]+=d*d/data.rows;}
            }
            models.add(Map.of("id",spec.get("id").getAsString(),"state_sha256",spec.get("state_sha256").getAsString(),"original_cp",original,"reflected_cp",mirror,"rms_mirror_cp",Math.sqrt(square/data.rows),"maximum_mirror_cp",max,"original_teacher_mse",loss[0],"reflected_teacher_mse",loss[1],"averaged_score_teacher_mse",loss[2]));
        }
        var result=Map.of("config_sha256",PairCoverageDiagnostic.sha(configPath),"rows",data.rows,"maximum_teacher_mirror_cp",maxTeacher,"teacher_cp",teacherCp,"models",models,"scope","Placement/STM score-view symmetry only;not castling legality,BT4 truth,learned relational originality or strength evidence");
        Files.writeString(Path.of(config.get("output").getAsString()),new GsonBuilder().setPrettyPrinting().create().toJson(result)+"\n",StandardOpenOption.CREATE_NEW);System.out.println("Maximum teacher mirror discrepancy="+maxTeacher+"cp;models="+models.size());
    }
    private HorizontalSymmetryAudit(){}
}
