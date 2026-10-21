package com.ohinteractive.seedv6.tools.eval;

import com.google.gson.JsonParser;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brnnext.*;
import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.training.model.NetworkModel;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.util.*;
import java.util.function.ToDoubleFunction;

/** Read-only actual-component composition and prediction oracle. No optimizer or export. */
public final class BrnHybridProbe {
    record Plan(String id,List<BrnSuccessorMatch.Model> models) {}
    record Predictor(String name,ToDoubleFunction<long[]> actual,ToDoubleFunction<long[]> algebra) {}
    public static void main(String[] args) throws Exception {
        if(args.length!=4)throw new IllegalArgumentException("PLAN BT4_JSONL CP_JSONL NEW_OUTPUT");
        var plan=DataFiles.read(Path.of(args[0]),Plan.class);Path output=Path.of(args[3]);Files.createDirectory(output);
        var predictors=new ArrayList<Predictor>();var bindings=new ArrayList<Object>();
        for(var spec:plan.models()) {
            if(spec.hybridBase()==null)continue;
            var original=((NetworkModel.BrnNext)BrnSuccessorMatch.loadModel(spec)).model();
            var model=BrnSuccessorVariants.correction(original,spec.correctionScale()==null?1:spec.correctionScale(),
                    spec.correctionAblation(),spec.constantGates());
            var base=BrnSuccessorMatch.loadHybridBase(spec);var hybrid=model.newResearchWorkspace(base);
            var compiled=spec.compiledUnaryEnabled()?com.ohinteractive.seedv6.core.brnpair2.BrnUnaryResearchCompiler.compile(base,model):null;
            var compiledWorkspace=compiled==null?null:compiled.newWorkspace();
            var correction=model.newWorkspace();var gen32=BrnNextBase.model().newWorkspace();var replacement=base.newWorkspace();
            predictors.add(new Predictor(spec.name(),b->compiledWorkspace==null?model.rawPawns(b,hybrid):compiledWorkspace.evaluatePawns(b),
                    b->replacement.evaluatePawns(b)+model.rawPawns(b,correction)-gen32.evaluatePawns(b)));
            var bytes=new ByteArrayOutputStream();BrnNextCodec.writeModel(model,bytes);
            var binding=new LinkedHashMap<String,Object>();binding.put("spec",spec);
            binding.put("hybrid",BrnSuccessorMatch.hybridBinding(spec,base,HexFormat.of().formatHex(DataFiles.digest().digest(bytes.toByteArray()))));
            if(compiled!=null) {
                var encoded=new ByteArrayOutputStream();com.ohinteractive.seedv6.core.brnpair2.BrnPair2Codec.writeModel(compiled,encoded);
                binding.put("compiledUnary",Map.of("kind","brn-unary-compiled-v1",
                        "compiledNetworkSha256",HexFormat.of().formatHex(DataFiles.digest().digest(encoded.toByteArray()))));
            }
            bindings.add(binding);
        }
        if(predictors.isEmpty())throw new IllegalArgumentException("Explicit hybrid specifications required");
        var inputs=new TreeMap<String,String>();inputs.put(args[0],DataFiles.hash(Files.readString(Path.of(args[0]))));
        var sources=new ArrayList<Object>();double maximumError=0;
        for(int source=1;source<=2;source++) {
            String text=Files.readString(Path.of(args[source]));inputs.put(args[source],DataFiles.hash(text));var lines=text.lines().toList();
            if(lines.size()!=2048)throw new IllegalArgumentException("Expected frozen2048-row development source");
            var metrics=new TreeMap<String,BrnSuccessorDataProbe.Metrics>();
            try(var writer=Files.newBufferedWriter(output.resolve((source==1?"bt4":"cp")+"-predictions.jsonl"))) {
                for(int i=0;i<lines.size();i++) {
                    var row=JsonParser.parseString(lines.get(i)).getAsJsonObject();String fen=row.get("fen").getAsString();
                    var board=Board.fromFen(fen);double target=row.get("target").getAsDouble();var values=new TreeMap<String,Double>();
                    for(var p:predictors) {
                        double actual=p.actual().applyAsDouble(board),algebra=p.algebra().applyAsDouble(board);
                        if(!Double.isFinite(actual)||!Double.isFinite(algebra))throw new ArithmeticException("Nonfinite hybrid");
                        maximumError=Math.max(maximumError,Math.abs(actual-algebra));values.put(p.name(),actual);
                        metrics.computeIfAbsent(p.name()+(i<1024?":fit":":evaluation"),k->new BrnSuccessorDataProbe.Metrics()).add(actual,board,target);
                    }
                    writer.write(DataFiles.JSON.toJson(Map.of("fen",fen,"target",target,"predictions",values)).replace("\n",""));writer.newLine();
                }
            }
            var measured=new TreeMap<String,Object>();metrics.forEach((k,v)->measured.put(k,v.result()));
            sources.add(Map.of("source",source==1?"BT4":"CP","metrics",measured));
        }
        DataFiles.write(output.resolve("summary.json"),Map.of("id",plan.id(),"inputs",inputs,"bindings",bindings,
                "sources",sources,"maximumCompositionRawError",maximumError,"scope","Consumed development data; no training or combined checkpoint export"));
        if(maximumError>1e-9)throw new IllegalStateException("Hybrid composition mismatch; evidence preserved");
        System.out.println("Actual hybrid composition verified: "+output);
    }
    private BrnHybridProbe() {}
}
