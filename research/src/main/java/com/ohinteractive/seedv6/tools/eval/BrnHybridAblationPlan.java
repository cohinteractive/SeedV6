package com.ohinteractive.seedv6.tools.eval;

import com.google.gson.JsonParser;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brnnext.BrnNextArchitecture;
import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.training.model.NetworkModel;
import java.nio.file.*;
import java.util.*;

/** Prepare explicit inference ablations; conditional gates use label-free fit rows only. */
public final class BrnHybridAblationPlan {
    public static void main(String[] args) throws Exception {
        if(args.length!=4)throw new IllegalArgumentException("HYBRID_PLAN BT4_JSONL CP_JSONL NEW_PLAN");
        Path output=Path.of(args[3]);if(Files.exists(output))throw new IllegalArgumentException("Preserve prior plan");
        var plan=DataFiles.read(Path.of(args[0]),BrnHybridProbe.Plan.class);
        var inputs=new TreeMap<String,String>();inputs.put(args[0],DataFiles.hash(Files.readString(Path.of(args[0]))));
        var fit=new ArrayList<long[]>();
        for(int s=1;s<=2;s++) {
            String text=Files.readString(Path.of(args[s]));inputs.put(args[s],DataFiles.hash(text));var rows=text.lines().toList();
            if(rows.size()!=2048)throw new IllegalArgumentException("Expected frozen2048-row source");
            for(int i=0;i<1024;i++)fit.add(Board.fromFen(JsonParser.parseString(rows.get(i)).getAsJsonObject().get("fen").getAsString()));
        }
        var models=new ArrayList<BrnSuccessorMatch.Model>();var fitted=new TreeMap<String,double[]>();
        for(var spec:plan.models()) {
            models.add(spec);if(spec.hybridBase()==null)continue;
            if(spec.correctionAblation()!=null)throw new IllegalArgumentException("Start from hybrid components without an existing ablation");
            var model=((NetworkModel.BrnNext)BrnSuccessorMatch.loadModel(spec)).model();
            boolean gated=model.architecture()==BrnNextArchitecture.GEN32_GATED;
            if(!gated&&model.architecture()!=BrnNextArchitecture.GEN32_CUBIC)throw new IllegalArgumentException("Frozen correction required");
            double[] mean=null;
            if(gated) {
                mean=new double[4];
                for(var board:fit) {
                    double[] gates=BrnGatedContextProbe.gates(model,board);
                    for(int r=0;r<4;r++)mean[r]+=gates[r]/fit.size();
                }
                fitted.put(spec.name(),mean);
            }
            for(String ablation:List.of("unary",gated?"constant-gates":"quadratic")) {
                var json=DataFiles.JSON.toJsonTree(spec).getAsJsonObject();
                json.addProperty("name",spec.name()+"-"+ablation);json.addProperty("correctionAblation",ablation);
                if(ablation.equals("constant-gates"))json.add("constantGates",DataFiles.JSON.toJsonTree(mean));
                var modified=DataFiles.JSON.fromJson(json,BrnSuccessorMatch.Model.class);
                // Validate the exact public transformation before recording a usable plan.
                BrnSuccessorVariants.correction(model,spec.correctionScale()==null?1:spec.correctionScale(),ablation,modified.constantGates());
                models.add(modified);
            }
        }
        DataFiles.write(output,Map.of("id",plan.id()+"-ablations-v1","models",models,"inputs",inputs,
                "fitPositions",fit.size(),"constantGates",fitted,
                "scope","First1024/source, label-independent constant gate means. Unary and conditional/cubic-term removal are inference ablations, not retrained competitors. All data already consumed development data; no training or strength admission."));
        System.out.println("Prepared explicit hybrid ablation plan: "+output);
    }
    private BrnHybridAblationPlan() {}
}
