package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.brn3.Brn3Model;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.exact.ExactSearch;
import com.ohinteractive.seedv6.search.exact.ExactEvaluator;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.training.data.DataFiles;
import java.nio.file.*;
import java.util.*;
import java.io.*;
import java.util.function.ToDoubleFunction;

/** Explicitly selected held-out partition, actual runtime controls and separate qsearch. */
public final class BrnSuccessorEvaluate {
    static volatile double sink;
    public static void main(String[] args)throws Exception {
        if(args.length<3||args.length>4)throw new IllegalArgumentException("DATA STATE NEW_JSON [validation|test]");
        Path dataPath=Path.of(args[0]),state=Path.of(args[1]),out=Path.of(args[2]);if(Files.exists(out))throw new IllegalArgumentException("Use new output");
        String partition=args.length==4?args[3]:"validation";if(!Set.of("validation","test").contains(partition))throw new IllegalArgumentException("partition");
        var data=BrnResearchData.read(dataPath,partition.equals("test"));var records=partition.equals("test")?data.test():data.validation();
        boolean nnue=state.toString().endsWith(".nnue");
        var candidate=nnue?null:BrnSuccessorCandidate.decode(Files.readAllBytes(state));
        var work=nnue?null:new BrnSuccessorInference(candidate);
        NnueNetwork network=null;if(nnue)try(var in=new BufferedInputStream(Files.newInputStream(state))){network=NnueNetworkCodec.read(in);}
        var inference=nnue?new NnueEvaluator(network):null;
        var compatible=!nnue&&candidate.pool==BrnSuccessorCandidate.Pool.SUM?work.compatibleModel():null;
        var production=compatible==null?null:compatible.newWorkspace();
        ToDoubleFunction<long[]> raw=nnue?b->{inference.evaluate(b);return inference.boundedValue();}:production!=null?production::evaluatePawns:work::evaluatePawns;
        ToDoubleFunction<long[]> play=nnue?raw:production!=null?b->production.evaluatePawns(b,.25):b->work.evaluatePawns(b,.25);
        var factory=BrnSuccessorMatch.factory(state,.25);var report=new LinkedHashMap<String,Object>();
        report.put("stateSha256",BrnResearchComparison.digest(state));report.put("state",state.toString());
        report.put("partition",partition);report.put("runtimeImplementation",nnue?"production NNUE":production!=null?"production BRN3":"experimental folded context");
        report.put("datasetManifest",DataFiles.read(dataPath.resolve("manifest.json"),Map.class));
        report.put("rawValidation",BrnResearchMain.metrics(records,e->raw.applyAsDouble(e.board()),!nnue));
        report.put("quarterValidation",BrnResearchMain.metrics(records,e->play.applyAsDouble(e.board()),!nnue));
        report.put("materialValidation",BrnResearchMain.metrics(records,e->RelationalCandidate.material(e.board()),true));
        double maxError=0,ce=0;int scoreDifferences=0;
        for(var e:records) if(!nnue) {
            double expected=candidate.predict(e.board()),actual=raw.applyAsDouble(e.board());maxError=Math.max(maxError,Math.abs(expected-actual));
            if(Brn3Model.score(expected)!=Brn3Model.score(actual))scoreDifferences++;
            ce+=RelationalCandidate.crossEntropy(actual,e.sfMaterial(),e.outcome())[0];
        }
        if(maxError>1e-4)throw new AssertionError("Folded error "+maxError);
        if(!nnue){report.put("maximumFloatCacheErrorPawns",maxError);report.put("rawIntegerDifferencesVsDouble",scoreDifferences);report.put("meanRawCrossEntropy",ce/records.size());}
        var boards=records.stream().map(BrnResearchData.Example::board).toList();
        for(int r=0;r<5;r++)for(var b:boards)sink=play.applyAsDouble(b);
        double[] times=new double[7];
        for(int r=0;r<times.length;r++){long start=System.nanoTime();double sum=0;for(var b:boards)sum+=play.applyAsDouble(b);times[r]=(System.nanoTime()-start)/(double)boards.size();sink=sum;}
        report.put("foldedUnrelatedInferenceNs",times);report.put("foldedParameterBytes",4L*(nnue?NnueNetwork.PARAMETER_COUNT:com.ohinteractive.seedv6.core.brn3.Brn3Layout.MODEL_PARAMETERS+candidate.parameters-candidate.context));
        report.put("runtimeNote","Production compatible controls; experimental context folds relative rows into absolute float rows. NNUE retains V1 score mapping. Field names rawValidation/quarterValidation refer to the explicitly named partition; quarter equals raw for NNUE.");
        var searches=new ArrayList<Map<String,Object>>();var qsearches=new ArrayList<Map<String,Object>>();
        for(int i=0;i<64;i++) {
            var b=records.get(i).board();long start=System.nanoTime();
            var control=SearchControl.controlled(2_000_000,start,3_000_000_000L,TimeSource.SYSTEM);
            try(var driver=factory.get()) {
                var result=driver.search(new SearchRequest(b,GameHistory.initial(b),4,SearchObserver.NONE,control,true));var last=result.lastCompletedResult();
                searches.add(Map.of("index",i,"complete",result.targetDepthCompleted(),"nodes",control.nodes(),"seconds",(System.nanoTime()-start)/1e9,"score",last.score(),"move",Long.toUnsignedString(last.bestMove())));
            }
            if(i<16) {
                long begun=System.nanoTime();
                ExactEvaluator qe=nnue?ExactEvaluator.from(SearchEvaluation.incremental(network)):
                        compatible!=null?ExactEvaluator.from(SearchEvaluation.brn3Research(compatible,.25)):
                        (board,ply)->Brn3Model.score(play.applyAsDouble(board));
                var q=ExactSearch.quiescenceResearch(qe);
                var result=q.search(b,GameHistory.initial(b),3,()->q.visitedNodes()>=500_000||System.nanoTime()-begun>2_000_000_000L);
                qsearches.add(Map.of("index",i,"complete",result.completed(),"nodes",result.nodes(),"qnodes",result.qnodes(),"seconds",(System.nanoTime()-begun)/1e9));
            }
        }
        report.put("searchDepth4",searches);report.put("separateQsearchDepth3",qsearches);DataFiles.write(out,report);
    }
    private BrnSuccessorEvaluate(){}
}
