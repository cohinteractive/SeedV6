package com.ohinteractive.seedv6.tools.eval;

import com.google.gson.JsonParser;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.training.model.NetworkModel;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.util.*;
import java.util.function.ToDoubleFunction;

/** Adjacent-checkpoint smoothing diagnostic. No optimization, source writes or trainable exports. */
public final class BrnCheckpointAverageProbe {
    record Source(String root,String checkpoint,String networkSha256) {}
    record Group(String name,List<Source> members) {}
    record Plan(String id,Source reference,List<Group> groups) {}
    record Predictor(String name,ToDoubleFunction<long[]> value) {}
    static NetworkModel load(Source source) throws Exception {
        var snapshot=CheckpointStore.readSnapshot(Path.of(source.root()),source.checkpoint());
        if(!snapshot.manifest().networkSha256().equals(source.networkSha256()))throw new IllegalArgumentException("Changed input");
        return snapshot.model();
    }
    static ToDoubleFunction<long[]> raw(NetworkModel model) {
        if(model instanceof NetworkModel.BrnNext n) {
            var workspace=n.model().newWorkspace();return b->n.model().rawPawns(b,workspace);
        }
        if(model instanceof NetworkModel.BrnPair2 p) {
            var workspace=p.model().newWorkspace();return workspace::evaluatePawns;
        }
        throw new IllegalArgumentException("Unsupported model");
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=4)throw new IllegalArgumentException("PLAN BT4_JSONL CP_JSONL NEW_OUTPUT_DIRECTORY");
        Path output=Path.of(args[3]);Files.createDirectory(output);
        var plan=DataFiles.read(Path.of(args[0]),Plan.class);
        var reference=raw(load(plan.reference()));
        var predictors=new ArrayList<Predictor>();predictors.add(new Predictor("Gen32",reference));
        var bindings=new ArrayList<Object>();var additive=new HashSet<String>();
        for(var group:plan.groups()) {
            var models=new ArrayList<NetworkModel>();var members=new ArrayList<Object>();
            String parent=null;long generation=-1,step=-1;
            for(var source:group.members()) {
                var snapshot=CheckpointStore.readSnapshot(Path.of(source.root()),source.checkpoint());
                if(parent!=null&&(!snapshot.manifest().parentId().equals(parent)
                        ||snapshot.manifest().generation()!=generation+1||snapshot.manifest().optimizerStep()!=step+8192))
                    throw new IllegalArgumentException("Consecutive same-lineage checkpoints required");
                parent=source.checkpoint();generation=snapshot.manifest().generation();step=snapshot.manifest().optimizerStep();
                models.add(load(source));members.add(snapshot.manifest());
            }
            if(models.size()!=8)throw new IllegalArgumentException("Frozen eight-checkpoint window required");
            var average=BrnSuccessorVariants.average(models);var mean=raw(average);var latest=raw(models.getLast());
            var functions=models.stream().map(BrnCheckpointAverageProbe::raw).toList();
            ToDoubleFunction<long[]> ensemble=b->{double sum=0;for(var f:functions)sum+=f.applyAsDouble(b)/functions.size();return sum;};
            for(var entry:Map.of("latest",latest,"average8",mean,"ensemble8",ensemble).entrySet()) {
                var f=entry.getValue();String name=group.name()+"-"+entry.getKey();
                predictors.add(new Predictor(name,f));
                predictors.add(new Predictor(name+"-half",b->{double base=reference.applyAsDouble(b);return base+.5*(f.applyAsDouble(b)-base);}));
            }
            if(average instanceof NetworkModel.BrnPair2)additive.add(group.name());
            var encoded=new ByteArrayOutputStream();average.write(encoded);
            bindings.add(Map.of("name",group.name(),"members",members,
                    "averagedNetworkSha256",HexFormat.of().formatHex(DataFiles.digest().digest(encoded.toByteArray())),
                    "payloadBytes",encoded.size(),"optimizer","None: inference-only diagnostic"));
        }
        var results=new ArrayList<Object>();var inputs=new TreeMap<String,String>();
        inputs.put(args[0],DataFiles.hash(Files.readString(Path.of(args[0]))));
        for(int source=1;source<=2;source++) {
            String text=Files.readString(Path.of(args[source]));inputs.put(args[source],DataFiles.hash(text));
            var lines=text.lines().toList();if(lines.size()!=2048)throw new IllegalArgumentException("Frozen2048 positions required");
            var metrics=new TreeMap<String,BrnSuccessorDataProbe.Metrics>();var maxError=new TreeMap<String,Double>();
            try(var writer=Files.newBufferedWriter(output.resolve((source==1?"bt4":"cp")+"-predictions.jsonl"))) {
                for(int i=0;i<lines.size();i++) {
                    var row=JsonParser.parseString(lines.get(i)).getAsJsonObject();String fen=row.get("fen").getAsString();
                    var board=Board.fromFen(fen);double target=row.get("target").getAsDouble();var values=new TreeMap<String,Double>();
                    for(var predictor:predictors) {
                        double value=predictor.value().applyAsDouble(board);if(!Double.isFinite(value))throw new ArithmeticException("Nonfinite prediction");
                        values.put(predictor.name(),value);
                        metrics.computeIfAbsent(predictor.name()+(i<1024?":fit":":evaluation"),k->new BrnSuccessorDataProbe.Metrics()).add(value,board,target);
                    }
                    for(var group:plan.groups())maxError.merge(group.name(),Math.abs(values.get(group.name()+"-average8")-values.get(group.name()+"-ensemble8")),Math::max);
                    writer.write(DataFiles.JSON.toJson(Map.of("fen",fen,"target",target,"predictions",values)).replace("\n",""));writer.newLine();
                }
            }
            for(String name:additive)if(maxError.get(name)>1e-9)throw new IllegalStateException("Additive function average mismatch");
            var measured=new TreeMap<String,Object>();metrics.forEach((k,v)->measured.put(k,v.result()));
            results.add(Map.of("source",source==1?"BT4":"CP","metrics",measured,"maximumParameterVsFunctionMeanError",maxError));
        }
        DataFiles.write(output.resolve("summary.json"),Map.of("id",plan.id(),"inputs",inputs,"bindings",bindings,"sources",results,
                "scope","Consumed development data. Uniform last-eight parameter mean, not training or Adam continuation; functional ensemble is diagnostic only."));
        System.out.println("Completed read-only checkpoint averaging diagnostic: "+output);
    }
    private BrnCheckpointAverageProbe() {}
}
