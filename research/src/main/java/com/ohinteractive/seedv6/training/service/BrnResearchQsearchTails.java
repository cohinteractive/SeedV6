package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.core.util.Fen;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.exact.ExactSearch;
import com.ohinteractive.seedv6.training.data.DataFiles;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.ToDoubleFunction;

/** Follow-up of already recorded deadline tails; never replaces the original run. */
public final class BrnResearchQsearchTails {
    public static void main(String[] args) throws Exception {
        if(args.length<4)throw new IllegalArgumentException("DATA RUN NEW_OUTPUT ROOT_INDEX...");
        var data=BrnResearchData.read(Path.of(args[0]),false);var run=Path.of(args[1]);var out=Path.of(args[2]);
        if(Files.exists(out))throw new IOException("Use a new output");
        var meta=BrnResearchComparison.metadata(run);String candidate=(String)meta.get("candidate");
        var manifest=DataFiles.read(Path.of(args[0]).resolve("manifest.json"),Map.class);
        if(!Objects.equals(((Map<?,?>)meta.get("datasetManifest")).get("payloadSha256"),manifest.get("payloadSha256")))throw new IOException("Dataset mismatch");
        boolean production=args[3].equals("production");
        ToDoubleFunction<long[]> value;
        if(candidate.equals("nnue")) {
            try(var in=new BufferedInputStream(Files.newInputStream(run.resolve("selected.nnue")))) {
                var evaluator=new NnueEvaluator(NnueNetworkCodec.read(in));value=b->{evaluator.evaluate(b);return evaluator.boundedValue();};
            }
        } else if(production) {
            var workspace=BrnResearchComparison.productionModel(run).newWorkspace();value=workspace::evaluatePawns;
        } else {
            try(var in=new BufferedInputStream(Files.newInputStream(run.resolve("selected.state")))) {
                var inference=new AbsoluteRelationInference(AbsoluteRelationCandidate.read(in).foldedInference());value=inference::predict;
            }
        }
        for(int r=0;r<4;r++)for(int i=0;i<1024;i++)value.applyAsDouble(data.validation().get(i).board());
        var results=new ArrayList<Map<String,Object>>();
        for(int argument=production?4:3;argument<args.length;argument++) {
            int index=Integer.parseInt(args[argument]);long[] b=data.validation().get(index).board();long start=System.nanoTime();
            var search=ExactSearch.quiescenceResearch((board,ply)->BrnResearchDiagnostics.nnueScore(candidate,value.applyAsDouble(board)));
            var result=search.search(b,GameHistory.initial(b),4,()->search.visitedNodes()>=2_000_000 || System.nanoTime()-start>=10_000_000_000L);
            results.add(Map.of("index",index,"fen",Fen.fromBoard(b),"completed",result.completed(),"nodes",result.nodes(),"qnodes",result.qnodes(),"maximumQply",result.maximumQply(),"seconds",(System.nanoTime()-start)/1e9));
        }
        var report=new LinkedHashMap<String,Object>();report.put("candidate",candidate);report.put("dataset",manifest);report.put("productionModel",production);
        report.put("guard","2 million nodes / 10 seconds, depth4; follow-up only; original 2-second failures remain recorded");report.put("results",results);
        DataFiles.write(out,report);System.out.println(DataFiles.JSON.toJson(report));
    }
    private BrnResearchQsearchTails(){}
}
