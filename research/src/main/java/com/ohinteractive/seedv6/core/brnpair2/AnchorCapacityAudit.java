package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import com.ohinteractive.seedv6.core.Board;
import java.nio.file.*;
import java.util.*;

/** Untrained deterministic parameter witnesses,never fit to teacher deltas. */
public final class AnchorCapacityAudit {
    public static void main(String[] args)throws Exception {
        PairObjectiveDiagnostic.permission();var config=JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonObject();Path cases=Path.of(config.get("cases").getAsString());
        if(!PairObjectiveDiagnostic.sha(cases).equals(config.get("cases_sha256").getAsString()))throw new IllegalArgumentException("Fixture identity");
        var linear=new AnchorNeuralNetwork(false,.01,2026102907);var nonlinear=new AnchorNeuralNetwork(true,.01,2026102907);var random=new Random(2026102907);
        for(int i=0;i<linear.weights.length;i++)linear.weights[i]=nonlinear.weights[i]=Math.max(-.99,Math.min(.99,.3*random.nextGaussian()));var result=new ArrayList<Object>();
        for(var item:JsonParser.parseString(Files.readString(cases)).getAsJsonArray()) {
            var row=item.getAsJsonObject();double a=0,b=0;
            for(int n=0;n<4;n++){long[] board=Board.fromFen(row.getAsJsonArray("fens").get(n).getAsString());a+=InteractionAudit.SIGNS[n]*linear.scoreCp(board,new AnchorNeuralNetwork.Workspace());b+=InteractionAudit.SIGNS[n]*nonlinear.scoreCp(board,new AnchorNeuralNetwork.Workspace());}
            result.add(Map.of("source_row",row.get("source_row").getAsInt(),"context_changed",row.get("context_changed").getAsBoolean(),"linear_mixed_cp",a,"nonlinear_mixed_cp",b));
        }
        Path out=Path.of(config.get("output").getAsString());Files.writeString(out,new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("cases",result,"seed",2026102907,
            "config_sha256",PairObjectiveDiagnostic.sha(Path.of(args[0])),"scope","Untrained parameter witnesses establish possible responses only; no teacher fit,chess quality or playing evidence"))+"\n",StandardOpenOption.CREATE_NEW);
        System.out.println("Completed "+result.size()+" neural anchor response witnesses");
    }
    private AnchorCapacityAudit(){}
}
