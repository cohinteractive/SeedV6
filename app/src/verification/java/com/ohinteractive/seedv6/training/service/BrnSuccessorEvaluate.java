package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.brn3.Brn3Model;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.exact.ExactSearch;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.training.data.DataFiles;
import java.nio.file.*;
import java.util.*;

/** Survivor evaluation with folded float runtime, validation only and explicit qsearch. */
public final class BrnSuccessorEvaluate {
    static volatile double sink;
    public static void main(String[] args)throws Exception {
        if(args.length!=3)throw new IllegalArgumentException("DATA STATE NEW_JSON");
        Path dataPath=Path.of(args[0]),state=Path.of(args[1]),out=Path.of(args[2]);if(Files.exists(out))throw new IllegalArgumentException("Use new output");
        var data=BrnResearchData.read(dataPath,false);var candidate=BrnSuccessorCandidate.decode(Files.readAllBytes(state));
        var work=new BrnSuccessorInference(candidate);var report=new LinkedHashMap<String,Object>();
        report.put("stateSha256",BrnResearchComparison.digest(state));report.put("state",state.toString());
        report.put("datasetManifest",DataFiles.read(dataPath.resolve("manifest.json"),Map.class));
        report.put("rawValidation",BrnResearchMain.metrics(data.validation(),e->work.evaluatePawns(e.board()),true));
        report.put("quarterValidation",BrnResearchMain.metrics(data.validation(),e->work.evaluatePawns(e.board(),.25),true));
        double maxError=0,ce=0;int scoreDifferences=0;
        for(var e:data.validation()) {
            double expected=candidate.predict(e.board()),actual=work.evaluatePawns(e.board());maxError=Math.max(maxError,Math.abs(expected-actual));
            if(Brn3Model.score(expected)!=Brn3Model.score(actual))scoreDifferences++;
            ce+=RelationalCandidate.crossEntropy(actual,e.sfMaterial(),e.outcome())[0];
        }
        if(maxError>1e-4)throw new AssertionError("Folded error "+maxError);
        report.put("maximumFloatCacheErrorPawns",maxError);report.put("rawIntegerDifferencesVsDouble",scoreDifferences);report.put("meanRawCrossEntropy",ce/data.validation().size());
        var boards=data.validation().stream().map(BrnResearchData.Example::board).toList();
        for(int r=0;r<5;r++)for(var b:boards)sink=work.evaluatePawns(b,.25);
        double[] times=new double[7];
        for(int r=0;r<times.length;r++){long start=System.nanoTime();double sum=0;for(var b:boards)sum+=work.evaluatePawns(b,.25);times[r]=(System.nanoTime()-start)/(double)boards.size();sink=sum;}
        report.put("foldedUnrelatedInferenceNs",times);report.put("foldedParameterBytes",4L*(com.ohinteractive.seedv6.core.brn3.Brn3Layout.MODEL_PARAMETERS+candidate.parameters-candidate.context));
        report.put("runtimeNote","Shared relative messages folded into absolute float rows for comparable cache kernels; trained parameter saving is not a runtime payload saving in this experiment.");
        var searches=new ArrayList<Map<String,Object>>();var qsearches=new ArrayList<Map<String,Object>>();
        for(int i=0;i<64;i++) {
            var b=data.validation().get(i).board();long start=System.nanoTime();
            var control=SearchControl.controlled(2_000_000,start,3_000_000_000L,TimeSource.SYSTEM);
            try(var driver=new SearchDriver(new ExactSearchAdapter((board,ply)->Brn3Model.score(work.evaluatePawns(board,.25)),new TTable(4)))) {
                var result=driver.search(new SearchRequest(b,GameHistory.initial(b),4,SearchObserver.NONE,control,true));var last=result.lastCompletedResult();
                searches.add(Map.of("index",i,"complete",result.targetDepthCompleted(),"nodes",control.nodes(),"seconds",(System.nanoTime()-start)/1e9,"score",last.score(),"move",Long.toUnsignedString(last.bestMove())));
            }
            if(i<16) {
                long begun=System.nanoTime();var q=ExactSearch.quiescenceResearch((board,ply)->Brn3Model.score(work.evaluatePawns(board,.25)));
                var result=q.search(b,GameHistory.initial(b),3,()->q.visitedNodes()>=500_000||System.nanoTime()-begun>2_000_000_000L);
                qsearches.add(Map.of("index",i,"complete",result.completed(),"nodes",result.nodes(),"qnodes",result.qnodes(),"seconds",(System.nanoTime()-begun)/1e9));
            }
        }
        report.put("searchDepth4",searches);report.put("separateQsearchDepth3",qsearches);DataFiles.write(out,report);
    }
    private BrnSuccessorEvaluate(){}
}
