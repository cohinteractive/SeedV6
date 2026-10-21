package com.ohinteractive.seedv6.tools.eval;

import com.google.gson.JsonParser;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brnnext.*;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.SearchDriver;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.training.model.NetworkModel;
import java.nio.file.*;
import java.io.ByteArrayOutputStream;
import java.util.*;

/** Actual frozen GATE/CUBIC weights, preserved game transitions and same-node search oracle. */
public final class BrnGatedCacheProbe {
    record ModelPlan(String id,List<BrnSuccessorMatch.Model> models) {}
    private static volatile long sink;
    public static void main(String[] args) throws Exception {
        if(args.length<4||args.length>6)throw new IllegalArgumentException("MODEL_PLAN NODE_RECEIPTS CORPUS_JSONL NEW_OUTPUT [MODEL_NAME [BLEND_WEIGHT]]");
        String candidate=args.length>=5?args[4]:"W1-GATE";
        Path output=Path.of(args[3]);if(Files.exists(output))throw new IllegalArgumentException("Preserve prior evidence");
        var plan=DataFiles.read(Path.of(args[0]),ModelPlan.class);
        var spec=plan.models().stream().filter(m->m.name().equals(candidate)).findFirst().orElseThrow();
        var snapshot=CheckpointStore.readSnapshot(Path.of(spec.root()),spec.checkpoint());
        var model=((NetworkModel.BrnNext)BrnSuccessorMatch.loadModel(spec)).model();
        if(spec.correctionScale()!=null||spec.correctionAblation()!=null)
            model=BrnSuccessorVariants.correction(model,spec.correctionScale()==null?1:spec.correctionScale(),
                    spec.correctionAblation(),spec.constantGates());
        boolean quadratic=model.architecture().quadratic();
        boolean cubic=model.architecture().polynomial();
        if(!cubic&&model.architecture().correctionKind()!=BrnNextArchitecture.GATED_PAIRS)throw new IllegalArgumentException("Unsupported cache");
        var hybridBase=BrnSuccessorMatch.loadHybridBase(spec);
        boolean packed=spec.packedCubicCacheEnabled();
        var compiledUnary=spec.compiledUnaryEnabled()?com.ohinteractive.seedv6.core.brnpair2.BrnUnaryResearchCompiler.compile(
                hybridBase==null?BrnNextBase.model():hybridBase,model):null;
        var compiledConstant=spec.compiledConstantGatesEnabled()?com.ohinteractive.seedv6.core.brnpair2.BrnConstantGateResearchCompiler.compile(
                hybridBase==null?BrnNextBase.model():hybridBase,model):null;
        if(compiledConstant!=null)compiledUnary=compiledConstant.model();
        if(hybridBase!=null&&!(spec.cubicDeltaCache()||spec.gatedDeltaCache()||packed||compiledUnary!=null))
            throw new IllegalArgumentException("Hybrid probe requires guarded delta cache");
        var full=hybridBase==null?SearchEvaluation.brnNext(model):SearchEvaluation.brnResidualHybridFullResearch(model,hybridBase);
        var cached=compiledUnary!=null?SearchEvaluation.brnPair2(compiledUnary):
                packed?(hybridBase==null?SearchEvaluation.brnCubicPackedResearch(model):SearchEvaluation.brnCubicPackedHybridResearch(model,hybridBase)):
                hybridBase!=null?SearchEvaluation.brnResidualHybridResearch(model,hybridBase):spec.cubicDeltaCache()?SearchEvaluation.brnCubicDeltaResearch(model):spec.gatedDeltaCache()?SearchEvaluation.brnGatedDeltaResearch(model):
                quadratic?SearchEvaluation.brnGen32Residual(model):cubic?SearchEvaluation.brnCubicIncrementalResearch(model):SearchEvaluation.brnGatedIncrementalResearch(model);
        var base=plan.models().getFirst();
        var referenceModel=CheckpointStore.readSnapshot(Path.of(base.root()),base.checkpoint()).model();
        var reference=hybridBase==null?referenceModel.evaluation(NnueScoreMapping.V1):SearchEvaluation.brnPair2(hybridBase);
        var boards=new ArrayList<long[]>();var inputs=new TreeMap<String,String>();
        try(var files=Files.list(Path.of(args[1]))) {
            for(var file:files.filter(f->f.getFileName().toString().matches("p\\d+-m\\d+-c\\d+\\.json")).sorted().toList()) {
                String json=Files.readString(file);inputs.put(file.toString(),DataFiles.hash(json));
                var receipt=DataFiles.JSON.fromJson(json,BrnSuccessorMatch.Receipt.class);
                boards.add(Board.fromFen(receipt.openingFen()));
                for(var move:receipt.moves())boards.add(Board.fromFen(move.fenAfter()));
            }
        }
        var accumulator=cache(model,spec.gatedDeltaCache()||spec.cubicDeltaCache(),packed,hybridBase,compiledUnary);
        var workspace=hybridBase==null?model.newWorkspace():model.newResearchWorkspace(hybridBase);
        var ordinary=model.newWorkspace();var gen32=BrnNextBase.model().newWorkspace();
        var replacement=hybridBase==null?null:hybridBase.newWorkspace();
        double maximum=0,maximumFreshError=0,compositionError=0;int differences=0,freshIntegerDifferences=0;
        for(int i=0;i<boards.size();i++) {
            var board=boards.get(i);double expected=model.rawPawns(board,workspace),actual=accumulator.rawPawns(board);
            maximum=Math.max(maximum,Math.abs(expected-actual));
            if(replacement!=null)compositionError=Math.max(compositionError,Math.abs(expected-
                    (model.rawPawns(board,ordinary)-gen32.evaluatePawns(board)+replacement.evaluatePawns(board))));
            if(model.score(board,workspace)!=accumulator.score(board))differences++;
            if(i%97==0) {
                var fresh=cache(model,spec.gatedDeltaCache()||spec.cubicDeltaCache(),packed,hybridBase,compiledUnary);double independent=fresh.rawPawns(board);
                maximumFreshError=Math.max(maximumFreshError,Math.abs(actual-independent));
                if(accumulator.score(board)!=fresh.score(board))freshIntegerDifferences++;
                if(!model.architecture().frozenBase()&&Double.doubleToLongBits(actual)!=Double.doubleToLongBits(independent))
                    throw new IllegalStateException("History-dependent W1 correction cache value");
            }
        }
        var result=new LinkedHashMap<String,Object>();
        result.put("plan",model.architecture().frozenBase()?"brn-successor-wave3-cache-oracle-v2":
                "brn-successor-wave2-"+(cubic?"cubic":"gate")+"-cache-v1");result.put("manifest",snapshot.manifest());
        result.put("modelPlanId",plan.id());
        if(spec.averagedInputs()!=null)result.put("averagedInputs",spec.averagedInputs());
        if(spec.retainedCubicRanks()!=null)result.put("retainedCubicRanks",spec.retainedCubicRanks());
        if(packed)result.put("packedCubicCache","packed-polynomial-period64-canonical-rounding-guard-v1");
        if(compiledConstant!=null)result.put("compiledConstantGates",BrnSuccessorMatch.constantBinding(compiledConstant));
        if(compiledUnary!=null&&compiledConstant==null) {
            var bytes=new ByteArrayOutputStream();com.ohinteractive.seedv6.core.brnpair2.BrnPair2Codec.writeModel(compiledUnary,bytes);
            result.put("compiledUnary",Map.of("kind","brn-unary-compiled-v1",
                    "compiledNetworkSha256",HexFormat.of().formatHex(DataFiles.digest().digest(bytes.toByteArray())),
                    "payloadBytes",bytes.size(),"immutablePrimitiveBytes",compiledUnary.immutablePrimitiveBytes()));
        }
        var effectiveBytes=new ByteArrayOutputStream();BrnNextCodec.writeModel(model,effectiveBytes);
        result.put("effectiveNetworkSha256",HexFormat.of().formatHex(DataFiles.digest().digest(effectiveBytes.toByteArray())));
        if(hybridBase!=null)result.put("hybrid",BrnSuccessorMatch.hybridBinding(spec,hybridBase,(String)result.get("effectiveNetworkSha256")));
        result.put("maximumCompositionRawError",compositionError);
        if(spec.correctionScale()!=null||spec.correctionAblation()!=null) {
            result.put("correctionScale",spec.correctionScale()==null?1:spec.correctionScale());
            result.put("correctionAblation",spec.correctionAblation());result.put("constantGates",spec.constantGates());
        }
        result.put("candidateWeightBytes",8L*model.architecture().parameters());
        if(model.architecture().frozenBase()) {
            if(hybridBase==null)result.put("baseSha256",BrnNextBase.SHA256);
            result.put("combinedPayloadBytes",BrnNextCodec.modelBytes(model.architecture())+com.ohinteractive.seedv6.core.brnpair2.BrnPair2Codec.MODEL_BYTES);
            result.put("combinedImmutablePrimitiveBytes",8L*model.architecture().parameters()+BrnNextBase.model().immutablePrimitiveBytes());
        }
        result.put("modelPlanHash",DataFiles.hash(Files.readString(Path.of(args[0]))));result.put("candidateName",candidate);
        result.put("inputs",inputs);result.put("positions",boards.size());result.put("maximumRawError",maximum);
        result.put("maximumFreshRawError",maximumFreshError);result.put("freshIntegerDifferences",freshIntegerDifferences);
        result.put("integerDifferences",differences);result.put("accumulatorPrimitiveBytes",accumulator.primitiveBytes());
        result.put("cacheDetails",compiledConstant==null?accumulator.details():Map.of("kind","brn-constant-gates-compiled-v1"));
        DataFiles.write(output,result);
        if(maximum>1e-9||maximumFreshError>1e-9||compositionError>1e-9||differences!=0||freshIntegerDifferences!=0)throw new IllegalStateException("Trained oracle mismatch; evidence preserved");
        String label=quadratic?"QUADRATIC":cubic?"CUBIC":"GATE";
        var names=new ArrayList<>(List.of(label+"-full",label+"-cache",hybridBase==null?"Gen32":"HybridBase"));
        var states=new ArrayList<>(List.of(full.newState(2),cached.newState(2),reference.newState(2)));
        var dense=packed||compiledUnary!=null?(hybridBase==null?(quadratic?SearchEvaluation.brnGen32Residual(model):cubic?SearchEvaluation.brnCubicDeltaResearch(model):SearchEvaluation.brnGatedDeltaResearch(model)):
                SearchEvaluation.brnResidualHybridResearch(model,hybridBase)):null;
        if(packed||compiledUnary!=null) {
            names.add(label+"-same-base-dense");
            states.add(dense.newState(2));
        }
        if(hybridBase!=null) {
            names.add(label+"-original-base-cache");
            states.add((cubic?SearchEvaluation.brnCubicDeltaResearch(model):SearchEvaluation.brnGatedDeltaResearch(model)).newState(2));
        }
        if(args.length==6) {
            double alpha=Double.parseDouble(args[5]);
            names.add(label+"-blend-"+alpha);
            states.add(SearchEvaluation.blendResearch(reference,cached,alpha).newState(2));
            result.put("blendCandidateWeight",alpha);
            result.put("referenceManifest",CheckpointStore.readSnapshot(Path.of(base.root()),base.checkpoint()).manifest());
            if(referenceModel instanceof NetworkModel.BrnPair2 pair) {
                result.put("blendReferenceImmutablePrimitiveBytes",pair.model().immutablePrimitiveBytes());
                result.put("blendReferenceWorkspacePrimitiveBytes",pair.model().newWorkspace().primitiveBytes());
            }
            result.put("blendCandidateWeightBytes",8L*model.architecture().parameters());
        }
        for(var state:states)run(state,boards,50000);
        long[][] timings=new long[states.size()][7];
        for(int pass=0;pass<7;pass++)for(int offset=0;offset<states.size();offset++) {
            int i=(pass+offset)%states.size();long start=System.nanoTime();run(states.get(i),boards,50000);timings[i][pass]=System.nanoTime()-start;
        }
        var latency=new ArrayList<Object>();
        for(int i=0;i<states.size();i++) {
            long[] sorted=timings[i].clone();Arrays.sort(sorted);
            latency.add(Map.of("name",names.get(i),"passNanos",timings[i],"evaluationsPerPass",50000,"medianNanos",sorted[3]/50000.0));
        }
        result.put("transitionLatency",latency);DataFiles.write(output,result);
        var roots=new ArrayList<long[]>();
        try(var lines=Files.lines(Path.of(args[2]))) {
            var list=lines.toList();
            for(int i=0;i<16;i++)roots.add(Board.fromFen(JsonParser.parseString(list.get(i*128)).getAsJsonObject().get("fen").getAsString()));
        }
        boolean sameAsBase=model.architecture().frozenBase();
        int firstOutput=model.architecture().head()+(cubic?0:model.architecture().width());
        for(int i=0;i<model.architecture().parameters();i++)
            if((i<BrnNextArchitecture.EMBEDDING||i>=firstOutput)&&model.weight(i)!=0)sameAsBase=false;
        var searchEvaluations=new ArrayList<>(List.of(full,cached));
        var searchNames=new ArrayList<>(List.of(names.get(0),names.get(1)));
        if(packed||compiledUnary!=null) {searchEvaluations.add(dense);searchNames.add(label+"-same-base-dense");}
        if(sameAsBase) {searchEvaluations.add(reference);searchNames.add(hybridBase==null?"Gen32":"HybridBase");}
        int implementations=searchEvaluations.size();result.put("zeroCorrectionReferenceSearchOracle",sameAsBase);
        var searches=new ArrayList<Object>();
        for(int pass=0;pass<3;pass++)for(int i=0;i<roots.size();i++) {
            var board=roots.get(i);SearchResult expected=null;
            for(int offset=0;offset<implementations;offset++) {
                int implementation=(offset+pass+i)%implementations;
                try(var driver=new SearchDriver(BrnSuccessorAudit.common(searchEvaluations.get(implementation)))) {
                    var control=SearchControl.controlled(32768,System.nanoTime(),2_000_000_000L,TimeSource.SYSTEM);
                    var outcome=driver.search(new SearchRequest(board,GameHistory.initial(board),64,control));
                    if(outcome.lastCompletedResult()==null||control.termination()!=SearchTermination.NODE_LIMIT)
                        throw new IllegalStateException("Search probe failed");
                    if(expected!=null&&!expected.equals(outcome.lastCompletedResult()))throw new IllegalStateException("Search parity failed");
                    expected=outcome.lastCompletedResult();
                    searches.add(Map.of("pass",pass,"root",i,"implementation",searchNames.get(implementation),
                            "elapsedNanos",control.elapsedNanos(),"nodes",control.nodes(),"result",expected));
                }
            }
        }
        result.put("searches",searches);result.put("completed",true);
        result.put("java",System.getProperty("java.runtime.version"));result.put("activeProcessors",Runtime.getRuntime().availableProcessors());
        DataFiles.write(output,result);System.out.println(label+" checkpoint cache oracle and performance complete: "+output);
    }
    private interface Cache {
        double rawPawns(long[] board);
        int score(long[] board);
        long primitiveBytes();
        default Map<String,Object> details() { return Map.of(); }
    }
    private static Cache cache(BrnNextModel model,boolean delta,boolean packed,com.ohinteractive.seedv6.core.brnpair2.BrnPair2Model hybridBase,
                               com.ohinteractive.seedv6.core.brnpair2.BrnPair2Model compiledUnary) {
        if(compiledUnary!=null) {
            var value=compiledUnary.newWorkspace();
            return new Cache() {
                public double rawPawns(long[] board){return value.evaluatePawns(board);}
                public int score(long[] board){return com.ohinteractive.seedv6.core.brn3.Brn3Model.score(value.evaluatePawns(board,.25));}
                public long primitiveBytes(){return value.primitiveBytes();}
                public Map<String,Object> details(){return Map.of("kind","brn-unary-compiled-v1",
                        "rebuilds",value.rebuilds,"updates",value.updates,"repeats",value.repeats);}
            };
        }
        if(packed) {
            var value=hybridBase==null?new BrnCubicPackedAccumulator(model):new BrnCubicPackedAccumulator(model,hybridBase);
            return new Cache() {
                public double rawPawns(long[] board){return value.rawPawns(board);}
                public int score(long[] board){return value.score(board);}
                public long primitiveBytes(){return value.primitiveBytes();}
                public Map<String,Object> details(){return Map.of("kind","packed-polynomial-period64-canonical-rounding-guard-v1",
                        "activeComponents",value.activeComponents(),"correctionErrorBound",value.correctionErrorBound(),
                        "roundingFallbacks",value.roundingFallbacks,"rebuilds",value.rebuilds,"updates",value.updates);}
            };
        }
        if(model.architecture().quadratic()) {
            var value=hybridBase==null?new BrnQuadraticAccumulator(model):new BrnQuadraticAccumulator(model,hybridBase);
            return new Cache() {
                public double rawPawns(long[] board){return value.rawPawns(board);}
                public int score(long[] board){return value.score(board);}
                public long primitiveBytes(){return value.primitiveBytes();}
                public Map<String,Object> details(){return Map.of("kind","quadratic-period64-canonical-rounding-guard-v1",
                        "correctionErrorBound",value.correctionErrorBound(),"roundingFallbacks",value.roundingFallbacks,
                        "rebuilds",value.rebuilds,"updates",value.updates);}
            };
        }
        if(delta && model.architecture().frozenCubic()) {
            var value=hybridBase==null?new BrnCubicDeltaAccumulator(model):new BrnCubicDeltaAccumulator(model,hybridBase);
            return new Cache() {
                public double rawPawns(long[] board){return value.rawPawns(board);}
                public int score(long[] board){return value.score(board);}
                public long primitiveBytes(){return value.primitiveBytes();}
                public Map<String,Object> details(){return Map.of("kind","polynomial-period64-canonical-rounding-guard-v1",
                        "correctionErrorBound",value.correctionErrorBound(),"roundingFallbacks",value.roundingFallbacks,
                        "rebuilds",value.rebuilds,"updates",value.updates);}
            };
        }
        if(delta) {
            var value=hybridBase==null?new BrnGatedDeltaAccumulator(model):new BrnGatedDeltaAccumulator(model,hybridBase);
            return new Cache() {
                public double rawPawns(long[] board){return value.rawPawns(board);}
                public int score(long[] board){return value.score(board);}
                public long primitiveBytes(){return value.primitiveBytes();}
                public Map<String,Object> details(){return Map.of("kind","period64-canonical-rounding-guard-v1",
                        "correctionErrorBound",value.correctionErrorBound(),"roundingFallbacks",value.roundingFallbacks,
                        "rebuilds",value.rebuilds,"updates",value.updates,
                        "sharedGeometryPrimitiveBytes",BrnGatedDeltaAccumulator.sharedGeometryPrimitiveBytes());}
            };
        }
        if(model.architecture().correctionKind()==BrnNextArchitecture.CUBIC) {
            var value=new BrnCubicAccumulator(model);
            return new Cache() {
                public double rawPawns(long[] board){return value.rawPawns(board);}
                public int score(long[] board){return value.score(board);}
                public long primitiveBytes(){return value.primitiveBytes();}
            };
        }
        var value=new BrnGatedAccumulator(model);
        return new Cache() {
            public double rawPawns(long[] board){return value.rawPawns(board);}
            public int score(long[] board){return value.score(board);}
            public long primitiveBytes(){return value.primitiveBytes();}
        };
    }
    private static void run(SearchEvaluation.State state,List<long[]>boards,int count) {
        long sum=0;for(int i=0;i<count;i++)sum+=state.evaluate(boards.get(i%boards.size()),0);sink=sum;
    }
    private BrnGatedCacheProbe() {}
}
