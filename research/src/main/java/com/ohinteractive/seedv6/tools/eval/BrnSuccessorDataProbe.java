package com.ohinteractive.seedv6.tools.eval;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.Brn3Objective;
import com.ohinteractive.seedv6.core.brnnext.*;
import com.ohinteractive.seedv6.core.brnpair2.BrnPair2Features;
import com.ohinteractive.seedv6.core.util.Fen;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.model.NetworkTrainingState;
import com.ohinteractive.seedv6.training.nnue.NnueCorpusTargets;
import java.nio.file.*;
import java.util.*;
import java.util.function.ToDoubleFunction;

/** Frozen data diagnostics and destructive-in-memory-only ablations; never trains or writes a store. */
public final class BrnSuccessorDataProbe {
    record Source(String name, String selection, String identity, String seek, long start, int stride, boolean sealed) {}
    record Plan(String id, String modelPlan, int positions, Long maximumRawRecords, List<Source> sources) {
        Plan {
            if (positions < 1 || positions > 2048 || sources == null || sources.isEmpty() || sources.size() > 2)
                throw new IllegalArgumentException("Bounded probe requires 1..2048 positions and 1..2 sources");
            if (maximumRawRecords == null) maximumRawRecords = 1_000_000L;
            if (maximumRawRecords < 1 || maximumRawRecords > 1_000_000)
                throw new IllegalArgumentException("Raw record limit must be 1..1000000 per source");
            for (var source : sources)
                if (source.start() < 0 || source.stride() < 1 || !source.name().matches("[a-zA-Z0-9-]+"))
                    throw new IllegalArgumentException("Invalid source start, stride or output name");
            if (sources.stream().map(Source::name).distinct().count() != sources.size())
                throw new IllegalArgumentException("Duplicate source output name");
        }
    }
    record Predictor(String name, ToDoubleFunction<long[]> raw) {}
    static final class Metrics {
        long count; double ce, squared, residualSquared, rawMin=Double.POSITIVE_INFINITY, rawMax=Double.NEGATIVE_INFINITY;
        void add(double raw, long[] board, double target) {
            double error=Brn3Objective.smoothOutcome(raw,NnueCorpusTargets.material(board))[0]-target;
            ce+=Brn3Objective.crossEntropy(raw,NnueCorpusTargets.material(board),target)[0]; squared+=error*error;
            double residual=raw-BrnPair2Features.material(board); residualSquared+=residual*residual;
            rawMin=Math.min(rawMin,raw); rawMax=Math.max(rawMax,raw); count++;
        }
        Object result() { return Map.of("count",count,"crossEntropy",ce/count,"smoothOutcomeMse",squared/count,
                "residualRmsPawns",Math.sqrt(residualSquared/count),"rawMinPawns",rawMin,"rawMaxPawns",rawMax); }
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=2)throw new IllegalArgumentException("PLAN NEW_OUTPUT_DIRECTORY");
        var plan=DataFiles.read(Path.of(args[0]),Plan.class);
        var modelPlan=DataFiles.read(Path.of(plan.modelPlan()),BrnSuccessorAudit.Plan.class);
        Path output=Path.of(args[1]); Files.createDirectory(output);
        var predictors=new ArrayList<Predictor>(); predictors.add(new Predictor("material",BrnPair2Features::material));
        var parity=new LinkedHashMap<String,Double>();
        var parityNames=new ArrayList<String>();
        for(var spec:modelPlan.models()) {
            var state=CheckpointStore.readTrainingSnapshot(Path.of(spec.root()),spec.checkpoint());
            if(state instanceof NetworkTrainingState.BrnPair2 p) {
                predictors.add(new Predictor(spec.name(),p.trainer()::predictPawns));
            } else if(state instanceof NetworkTrainingState.BrnNext p) {
                parityNames.add(spec.name());
                var model=p.trainer().snapshot(); var workspace=model.newWorkspace();
                predictors.add(new Predictor(spec.name(),p.trainer()::predictPawns));
                predictors.add(new Predictor(spec.name()+"-runtime-raw",b->model.rawPawns(b,workspace)));
                var kind=model.architecture(); double[] weights=new double[kind.parameters()];
                for(int i=0;i<BrnNextArchitecture.EMBEDDING;i++)weights[i]=model.weight(i);
                var unary=new BrnNextModel(kind,weights); var unaryWorkspace=unary.newWorkspace();
                predictors.add(new Predictor(spec.name()+"-unary-only",b->unary.rawPawns(b,unaryWorkspace)));
                for(int i=0;i<weights.length;i++)weights[i]=model.weight(i);
                String label;
                if(kind==BrnNextArchitecture.GATED_PAIRS) {
                    Arrays.fill(weights,BrnNextArchitecture.EMBEDDING,kind.head()+kind.width(),0);
                    label="-uniform-gates";
                } else if(kind==BrnNextArchitecture.CUBIC) {
                    Arrays.fill(weights,kind.head()+kind.width(),weights.length,0); label="-quadratic-only";
                } else continue;
                var ablated=new BrnNextModel(kind,weights); var ws=ablated.newWorkspace();
                predictors.add(new Predictor(spec.name()+label,b->ablated.rawPawns(b,ws)));
            } else throw new IllegalArgumentException("Unexpected architecture");
        }
        var summaries=new ArrayList<Object>();
        long deadline=System.nanoTime()+300_000_000_000L;
        for(var request:plan.sources()) {
            var selection=DataFiles.read(Path.of(request.selection()),DataSources.class);
            var source=selection.sources().stream().filter(s->s.identity().equals(request.identity())).findFirst().orElseThrow();
            Path seek=output.resolve(request.name()+"-seek"); Files.createDirectory(seek);
            if(request.seek()!=null) {
                Path index=Path.of(request.seek()).resolve(source.identity()+".json");
                if(Files.exists(index))Files.copy(index,seek.resolve(index.getFileName()));
            }
            var metrics=new LinkedHashMap<String,Metrics>();
            for(var predictor:predictors)for(double gain:new double[]{.25,1})metrics.put(predictor.name()+"@"+gain,new Metrics());
            int retained=0; long examined=0,end=request.start();
            try(var reader=SourceReaders.open(source,seek,request.start());
                var rows=Files.newBufferedWriter(output.resolve(request.name()+"-positions.jsonl"))) {
                PositionReader.Entry entry;
                while(retained<plan.positions()) {
                    if(examined>=plan.maximumRawRecords())throw new IllegalStateException("Raw record budget exceeded; preserve partial output");
                    if(System.nanoTime()>deadline)throw new IllegalStateException("Data probe budget exceeded; preserve partial output");
                    entry=reader.next();
                    if(entry==null)break;
                    examined++;end=reader.nextPosition();
                    if((entry.ordinal()-request.start())%request.stride()!=0||entry.position()==null)continue;
                    var position=entry.position();
                    if(request.sealed()&&BrnNextPartition.part(position.position())!=BrnNextPartition.Part.SEALED)continue;
                    long[] board=position.position().toBoard(0);
                    double target;
                    if(source.labelProfile()==LabelProfile.BT4_Q_V1) {
                        if(position.target()==Bt4Targets.SKIP||Math.abs((long)position.target())>32000)continue;
                        target=Bt4Targets.q(position.target());
                    } else {
                        if(NnueCorpusTargets.rejection(position)!=null)continue;
                        target=NnueCorpusTargets.target(position,board);
                    }
                    var predictions=new LinkedHashMap<String,Double>(); double material=BrnPair2Features.material(board);
                    for(var predictor:predictors) {
                        double raw=predictor.raw().applyAsDouble(board);
                        if(!Double.isFinite(raw))throw new IllegalStateException("Nonfinite prediction");
                        predictions.put(predictor.name(),raw);
                        for(double gain:new double[]{.25,1}) metrics.get(predictor.name()+"@"+gain).add(material+gain*(raw-material),board,target);
                    }
                    for(String name:parityNames)
                        parity.merge(name,Math.abs(predictions.get(name)-predictions.get(name+"-runtime-raw")),Math::max);
                    rows.write(DataFiles.JSON.toJson(Map.of("ordinal",entry.ordinal(),"fen",Fen.fromBoard(board),
                            "target",target,"predictions",predictions)).replace("\n","")); rows.newLine();
                    retained++;
                }
            }
            if(retained!=plan.positions())throw new IllegalStateException("Insufficient data");
            var means=new LinkedHashMap<String,Object>();metrics.forEach((name,m)->means.put(name,m.result()));
            summaries.add(Map.of("source",source,"start",request.start(),"end",end,"examined",examined,
                    "retained",retained,"sealed",request.sealed(),"metrics",means));
            DataFiles.write(output.resolve("summary.json"),Map.of("plan",plan.id(),"sources",summaries,"maxTrainerRuntimeRawDifference",parity));
        }
        System.out.println("Completed fixed data/ablation probe without training: "+output);
    }
    private BrnSuccessorDataProbe() {}
}
