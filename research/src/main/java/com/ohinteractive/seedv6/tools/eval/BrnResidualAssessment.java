package com.ohinteractive.seedv6.tools.eval;

import com.google.gson.JsonParser;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brnnext.*;
import com.ohinteractive.seedv6.core.brnpair2.BrnPair2Features;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.training.model.NetworkTrainingState;
import java.nio.file.*;
import java.util.*;
import java.util.function.ToDoubleFunction;
import static com.ohinteractive.seedv6.core.brnnext.BrnNextArchitecture.*;

/** Reuses consumed development data: learning curves, raw parity, frozen-base-preserving ablations.
 * Constant gates fit only the first half of each source without labels; separate second-half metrics.
 */
public final class BrnResidualAssessment {
    record Row(long[] board, double target, String fen) {}
    record Predictor(String name, ToDoubleFunction<long[]> raw) {}
    public static void main(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("MODEL_PLAN BT4_JSONL CP_JSONL NEW_OUTPUT_DIRECTORY");
        var plan = DataFiles.read(Path.of(args[0]), BrnSuccessorAudit.Plan.class);
        Path output = Path.of(args[3]); Files.createDirectory(output);
        var datasets = new ArrayList<List<Row>>();
        var inputs = new TreeMap<String,String>();
        for (int s=1;s<=2;s++) {
            String text = Files.readString(Path.of(args[s])); inputs.put(args[s],DataFiles.hash(text));
            var rows = new ArrayList<Row>();
            for (String line : text.lines().toList()) {
                var j=JsonParser.parseString(line).getAsJsonObject();String fen=j.get("fen").getAsString();
                rows.add(new Row(Board.fromFen(fen),j.get("target").getAsDouble(),fen));
            }
            if(rows.size()!=2048)throw new IllegalArgumentException("Expected 2048 frozen rows/source");
            datasets.add(rows);
        }
        var predictors = new ArrayList<Predictor>();
        var bindings = new ArrayList<Object>();
        var parity = new LinkedHashMap<String,Double>();
        var means = new LinkedHashMap<String,double[]>();
        for (var spec : plan.models()) {
            Path root=Path.of(spec.root());
            var state=CheckpointStore.readTrainingSnapshot(root,spec.checkpoint());
            bindings.add(Map.of("name",spec.name(),"manifest",CheckpointStore.readSnapshot(root,spec.checkpoint()).manifest()));
            if(state instanceof NetworkTrainingState.BrnPair2 p) predictors.add(new Predictor(spec.name(),p.trainer()::predictPawns));
            else if(state instanceof NetworkTrainingState.BrnNext p) {
                var model=p.trainer().snapshot();var ws=model.newWorkspace();
                double error=0;
                for(var rows:datasets)for(var row:rows)
                    error=Math.max(error,Math.abs(p.trainer().predictPawns(row.board())-model.rawPawns(row.board(),ws)));
                parity.put(spec.name(),error);
                if(error>1e-9)throw new IllegalStateException("Raw trainer/runtime discrepancy");
                predictors.add(new Predictor(spec.name(),b->model.rawPawns(b,ws)));
                if(!Set.of("W3-RESGATE","W3-RESCUBIC").contains(spec.name()))continue;
                var kind=model.architecture();
                for(double alpha:new double[]{0,.25,.5,1.5})
                    add(predictors,spec.name()+"-correction"+alpha,BrnSuccessorVariants.correction(model,alpha,null,null));
                double[] w=weights(model);Arrays.fill(w,EMBEDDING,w.length,0);
                add(predictors,spec.name()+"-unary",new BrnNextModel(kind,w));
                if(kind.correctionKind()==CUBIC) {
                    w=weights(model);Arrays.fill(w,kind.head()+kind.width(),w.length,0);
                    add(predictors,spec.name()+"-quadratic",new BrnNextModel(kind,w));
                } else {
                    double[] mean=new double[kind.width()];
                    for(var rows:datasets)for(int i=0;i<1024;i++) {
                        double[] g=BrnGatedContextProbe.gates(model,rows.get(i).board());
                        for(int r=0;r<mean.length;r++)mean[r]+=g[r]/2048;
                    }
                    means.put(spec.name(),mean);
                    w=weights(model);Arrays.fill(w,EMBEDDING,kind.head(),0);
                    for(int r=0;r<mean.length;r++)w[kind.head()+r]=Math.log(Math.max(1e-300,mean[r]));
                    add(predictors,spec.name()+"-constant-gates",new BrnNextModel(kind,w));
                }
            } else throw new IllegalArgumentException("Unexpected model");
        }
        var reports=new ArrayList<Object>();
        for(int s=0;s<2;s++) {
            var rows=datasets.get(s);
            var metrics=new LinkedHashMap<String,BrnSuccessorDataProbe.Metrics>();
            try(var writer=Files.newBufferedWriter(output.resolve((s==0?"bt4":"cp")+"-predictions.jsonl"))) {
                for(int i=0;i<rows.size();i++) {
                    var row=rows.get(i);var values=new LinkedHashMap<String,Double>();
                    for(var p:predictors) {
                        double raw=p.raw().applyAsDouble(row.board()),material=BrnPair2Features.material(row.board());
                        if(!Double.isFinite(raw))throw new IllegalStateException("Nonfinite prediction");
                        values.put(p.name(),raw);
                        for(double gain:new double[]{.25,1}) {
                            String key=p.name()+"@"+gain+":"+(i<1024?"fit":"evaluation");
                            metrics.computeIfAbsent(key,k->new BrnSuccessorDataProbe.Metrics()).add(material+gain*(raw-material),row.board(),row.target());
                        }
                    }
                    writer.write(DataFiles.JSON.toJson(Map.of("fen",row.fen(),"target",row.target(),"predictions",values)).replace("\n",""));
                    writer.newLine();
                }
            }
            var values=new LinkedHashMap<String,Object>();metrics.forEach((k,v)->values.put(k,v.result()));
            reports.add(Map.of("source",s==0?"BT4":"CP","metrics",values));
        }
        DataFiles.write(output.resolve("summary.json"),Map.of("plan",plan.id(),"inputs",inputs,"bindings",bindings,
                "maxTrainerRuntimeRawError",parity,"meanGates",means,"sources",reports,
                "scope","Consumed development data, not independent strength evidence; no training"));
        System.out.println("Completed fixed-data residual assessment: "+output);
    }
    private static double[] weights(BrnNextModel model) {
        double[] w=new double[model.architecture().parameters()];
        for(int i=0;i<w.length;i++)w[i]=model.weight(i);return w;
    }
    private static void add(List<Predictor> list,String name,BrnNextModel model) {
        var ws=model.newWorkspace();list.add(new Predictor(name,b->model.rawPawns(b,ws)));
    }
    private BrnResidualAssessment() {}
}
