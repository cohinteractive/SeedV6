package com.ohinteractive.seedv6.tools.eval;

import com.google.gson.JsonParser;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.Brn3Objective;
import com.ohinteractive.seedv6.core.brnnext.*;
import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.training.model.NetworkModel;
import com.ohinteractive.seedv6.training.nnue.NnueCorpusTargets;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.util.*;
import java.util.function.ToDoubleFunction;

/** Fixed-budget rank-removal diagnostic. Fit-only ordering, separate evaluation halves, no training. */
public final class BrnCubicRankProbe {
    record Row(String fen,long[] board,double target) {}
    record Importance(int rank,double removedMinusFullFitLoss) {}
    record Predictor(String name,ToDoubleFunction<long[]> raw) {}
    static BrnNextModel keep(BrnNextModel source,List<Integer> ranks) {
        if(source.architecture()!=BrnNextArchitecture.GEN32_CUBIC||ranks.isEmpty()
                ||new HashSet<>(ranks).size()!=ranks.size()||ranks.stream().anyMatch(r->r<0||r>=16))
            throw new IllegalArgumentException("Distinct cubic ranks0..15 required");
        var kind=source.architecture();double[] weights=new double[kind.parameters()];
        for(int i=0;i<weights.length;i++)weights[i]=source.weight(i);
        for(int r=0;r<kind.width();r++)if(!ranks.contains(r))weights[kind.head()+r]=weights[kind.head()+kind.width()+r]=0;
        return new BrnNextModel(kind,weights);
    }
    static double fitLoss(BrnNextModel model,List<List<Row>> sources) {
        var workspace=model.newWorkspace();double loss=0;
        for(var rows:sources)for(int i=0;i<1024;i++) {
            var row=rows.get(i);loss+=Brn3Objective.crossEntropy(model.rawPawns(row.board(),workspace),NnueCorpusTargets.material(row.board()),row.target())[0]/2048;
        }
        return loss;
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=5)throw new IllegalArgumentException("MODEL_PLAN MODEL_NAME BT4_JSONL CP_JSONL NEW_OUTPUT_DIRECTORY");
        Path output=Path.of(args[4]);Files.createDirectory(output);
        var plan=DataFiles.read(Path.of(args[0]),BrnGatedCacheProbe.ModelPlan.class);
        var spec=plan.models().stream().filter(m->m.name().equals(args[1])).findFirst().orElseThrow();
        var source=((NetworkModel.BrnNext)BrnSuccessorMatch.loadModel(spec)).model();
        if(source.architecture()!=BrnNextArchitecture.GEN32_CUBIC||spec.correctionScale()!=null||spec.correctionAblation()!=null)
            throw new IllegalArgumentException("Unscaled frozen-base cubic model required");
        var datasets=new ArrayList<List<Row>>();var inputs=new TreeMap<String,String>();
        inputs.put(args[0],DataFiles.hash(Files.readString(Path.of(args[0]))));
        for(int s=2;s<=3;s++) {
            String text=Files.readString(Path.of(args[s]));inputs.put(args[s],DataFiles.hash(text));var rows=new ArrayList<Row>();
            for(String line:text.lines().toList()) {
                var j=JsonParser.parseString(line).getAsJsonObject();String fen=j.get("fen").getAsString();
                rows.add(new Row(fen,Board.fromFen(fen),j.get("target").getAsDouble()));
            }
            if(rows.size()!=2048)throw new IllegalArgumentException("Frozen2048 positions/source required");datasets.add(rows);
        }
        double fullFit=fitLoss(source,datasets);var importance=new ArrayList<Importance>();var all=new ArrayList<Integer>();
        for(int r=0;r<16;r++)all.add(r);
        for(int r=0;r<16;r++) {
            var retained=new ArrayList<>(all);retained.remove(Integer.valueOf(r));
            importance.add(new Importance(r,fitLoss(keep(source,retained),datasets)-fullFit));
        }
        importance.sort(Comparator.comparingDouble(Importance::removedMinusFullFitLoss).reversed().thenComparingInt(Importance::rank));
        var predictors=new ArrayList<Predictor>();var variants=new ArrayList<Object>();
        predictors.add(new Predictor("Gen32",BrnNextBase.model().newWorkspace()::evaluatePawns));
        for(int count:new int[]{4,8,12,16}) {
            var ranks=importance.subList(0,count).stream().map(Importance::rank).sorted().toList();var retained=keep(source,ranks);
            var encoded=new ByteArrayOutputStream();new NetworkModel.BrnNext(retained).write(encoded);
            variants.add(Map.of("ranks",ranks,"count",count,"networkSha256",HexFormat.of().formatHex(DataFiles.digest().digest(encoded.toByteArray()))));
            for(double scale:new double[]{.5,1}) {
                var model=BrnSuccessorVariants.correction(retained,scale,null,null);var workspace=model.newWorkspace();
                predictors.add(new Predictor("ranks"+count+"-scale"+scale,b->model.rawPawns(b,workspace)));
            }
        }
        var results=new ArrayList<Object>();
        for(int s=0;s<2;s++) {
            var rows=datasets.get(s);var metrics=new TreeMap<String,BrnSuccessorDataProbe.Metrics>();
            try(var writer=Files.newBufferedWriter(output.resolve((s==0?"bt4":"cp")+"-predictions.jsonl"))) {
                for(int i=0;i<rows.size();i++) {
                    var row=rows.get(i);var values=new TreeMap<String,Double>();
                    for(var predictor:predictors) {
                        double raw=predictor.raw().applyAsDouble(row.board());if(!Double.isFinite(raw))throw new ArithmeticException("Nonfinite prediction");
                        values.put(predictor.name(),raw);
                        metrics.computeIfAbsent(predictor.name()+"@1.0:"+(i<1024?"fit":"evaluation"),k->new BrnSuccessorDataProbe.Metrics()).add(raw,row.board(),row.target());
                    }
                    writer.write(DataFiles.JSON.toJson(Map.of("fen",row.fen(),"target",row.target(),"predictions",values)).replace("\n",""));writer.newLine();
                }
            }
            var measured=new TreeMap<String,Object>();metrics.forEach((k,v)->measured.put(k,v.result()));
            results.add(Map.of("source",s==0?"BT4":"CP","metrics",measured));
        }
        DataFiles.write(output.resolve("summary.json"),Map.of("id","wave4-cubic-rank-diagnostic-v1","model",spec,"inputs",inputs,
                "fullFitLoss",fullFit,"fitOnlyRankImportance",importance,"variants",variants,"sources",results,
                "scope","Consumed development data. Ranking uses first1024/source only, full raw CE; second halves evaluated without reranking. No optimized sparse inference or strength claim."));
        System.out.println("Completed cubic rank-removal diagnostic: "+output);
    }
    private BrnCubicRankProbe() {}
}
