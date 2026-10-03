package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.brn3.*;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.training.data.DataFiles;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.function.ToDoubleFunction;

/** Paired warmed latency and numerical parity on validation only; no training or tuning. */
public final class Brn3RuntimeMeasurement {
    static volatile double sink;
    static double time(ToDoubleFunction<long[]> evaluation,List<long[]> boards,int repeats) {
        double sum=0;long start=System.nanoTime();
        for(int r=0;r<repeats;r++)for(var b:boards)sum+=evaluation.applyAsDouble(b);
        double ns=(System.nanoTime()-start)/(double)(repeats*boards.size());sink=sum;return ns;
    }
    static double median(double[] values){var a=values.clone();Arrays.sort(a);return a[a.length/2];}
    public static void main(String[] args)throws Exception {
        Path dataPath=Path.of(args[0]),brnRun=Path.of(args[1]),nnueRun=Path.of(args[2]),output=Path.of(args[3]);
        if(Files.exists(output))throw new IOException("Use a new output");
        var data=BrnResearchData.read(dataPath,false);var boards=data.validation().stream().map(BrnResearchData.Example::board).toList();
        var model=BrnResearchComparison.productionModel(brnRun);var work=model.newWorkspace();
        NnueNetwork nnue;try(var in=new BufferedInputStream(Files.newInputStream(nnueRun.resolve("selected.nnue")))){nnue=NnueNetworkCodec.read(in);}
        var evaluator=new NnueEvaluator(nnue);ToDoubleFunction<long[]> n=b->{evaluator.evaluate(b);return evaluator.boundedValue();};
        ToDoubleFunction<long[]> b=work::evaluatePawns;
        time(b,boards,8);time(n,boards,8);double[] bt=new double[9],nt=new double[9],ratio=new double[9];
        for(int r=0;r<9;r++) {
            if(r%2==0){bt[r]=time(b,boards,4);nt[r]=time(n,boards,4);}else{nt[r]=time(n,boards,4);bt[r]=time(b,boards,4);}
            ratio[r]=nt[r]/bt[r];
        }
        var quiet=new ArrayList<long[]>();var capture=new ArrayList<long[]>();
        for(var board:boards.subList(0,1024)) {
            long[] moves=new long[512];int count=BrnResearchDiagnostics.legal(board,moves);boolean q=false,c=false;
            for(int i=0;i<count&&!(q&&c);i++) {
                var child=BrnResearchDiagnostics.child(board,moves[i]);boolean took=BrnResearchDiagnostics.pieces(child)<BrnResearchDiagnostics.pieces(board);
                if(took&&!c){capture.add(board);capture.add(child);c=true;}
                if(!took&&!q){quiet.add(board);quiet.add(child);q=true;}
            }
        }
        time(b,quiet,8);time(b,capture,8);
        double quietNs=time(b,quiet,32),captureNs=time(b,capture,32);
        AbsoluteRelationCandidate reference;try(var in=new BufferedInputStream(Files.newInputStream(brnRun.resolve("selected.state")))){reference=AbsoluteRelationCandidate.read(in).foldedInference();}
        double maximum=0;int scoreDifferences=0,saturated=0;var parity=model.newWorkspace();
        for(var board:boards){double expected=reference.predict(board),actual=parity.evaluatePawns(board);maximum=Math.max(maximum,Math.abs(expected-actual));
            if(BrnResearchDiagnostics.score(expected)!=Brn3Model.score(actual))scoreDifferences++;
            if(Math.abs(actual*100)>=com.ohinteractive.seedv6.search.exact.ExactSearch.MAX_STATIC_SCORE)saturated++;
        }
        if(maximum>1e-4)throw new IllegalStateException("Float/cache error budget exceeded: "+maximum);
        var report=new LinkedHashMap<String,Object>();report.put("method","9 alternating rounds after 8 warmups, 4 full validation passes/round; single JVM, unrelated positions; System.nanoTime");
        report.put("brnNanoseconds",bt);report.put("nnueNanoseconds",nt);report.put("pairedThroughputRatios",ratio);
        report.put("brnMedianNs",median(bt));report.put("nnueMedianNs",median(nt));report.put("medianThroughputRatio",median(ratio));report.put("throughputGatePassed",median(ratio)>=.25);
        report.put("quietParentChildMeanNsPerEvaluation",quietNs);report.put("captureParentChildMeanNsPerEvaluation",captureNs);
        report.put("updateCostNote","Alternating parent/child sequence includes unrelated parent rebuilds; capture changes piece count and rebuilds. Includes full evaluation, not a synthetic isolated edge operation.");
        report.put("cacheRebuilds",work.rebuilds);report.put("cacheUpdates",work.updates);report.put("cacheRepeats",work.repeats);
        report.put("maximumFloatCacheErrorPawns",maximum);report.put("integerScoreDifferences",scoreDifferences);report.put("normalBandSaturations",saturated);
        report.put("inferenceParameters",Brn3Layout.MODEL_PARAMETERS);report.put("trainingParameters",Brn3Layout.TRAINING_PARAMETERS);
        report.put("brnModelBytes",Brn3Codec.MODEL_BYTES);report.put("brnTrainingStateBytes",Brn3Codec.TRAINING_BYTES);
        report.put("nnueModelBytes",NnueNetworkCodec.ENCODED_BYTES);report.put("nnueTrainingStateBytes",com.ohinteractive.seedv6.training.nnue.TrainingStateCodec.ENCODED_BYTES);
        report.put("brnTrainerLargePrimitiveArraysBytes",40L*Brn3Layout.TRAINING_PARAMETERS+4L*Brn3Layout.EDGE_ROWS);
        report.put("memoryNote","Trainer: weights, two moments, gradient (double), stamps and touched (int), plus shared relative-row table. Excludes array headers, small activation workspaces, corpus records, JVM and transient snapshots/codecs. Inference model shared; worker workspace approximately 12 KiB primitive arrays.");
        report.put("modelSha256",BrnResearchComparison.digest(brnRun.resolve("production-preview/network.brn3")));
        DataFiles.write(output,report);System.out.println(DataFiles.JSON.toJson(report));
    }
    private Brn3RuntimeMeasurement(){}
}
