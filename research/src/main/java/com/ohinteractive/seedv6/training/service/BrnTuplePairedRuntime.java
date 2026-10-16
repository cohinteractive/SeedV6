package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.search.exact.ExactEvaluator;
import com.ohinteractive.seedv6.training.data.DataFiles;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** C02 same-JVM alternating-order cost check; no training or search changes. */
public final class BrnTuplePairedRuntime {
    static volatile long sink;
    private static double median(double[] values) {
        var ordered=values.clone();Arrays.sort(ordered);
        return (ordered[(ordered.length-1)/2]+ordered[ordered.length/2])/2;
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] a) throws Exception {
        if(a.length!=5)throw new IllegalArgumentException("DATA ORIGINAL COMPILED NEW_OUT GAIN");
        Path data=Path.of(a[0]),original=Path.of(a[1]),compiled=Path.of(a[2]),out=Path.of(a[3]);
        double gain=Double.parseDouble(a[4]);BrnArchitectureControls.requireGain(gain);
        if(Files.exists(out))throw new IOException("Use a new paired runtime output");
        String originalMetadata=BrnResearchComparison.digest(original.resolve("result.json"));
        String compiledMetadata=BrnResearchComparison.digest(compiled.resolve("result.json"));
        var originalReport=DataFiles.read(original.resolve("result.json"),Map.class);
        var compiledReport=DataFiles.read(compiled.resolve("result.json"),Map.class);
        if(!"TUPLE".equals(originalReport.get("kind"))||!"TUPLE_COMPILED".equals(compiledReport.get("kind"))
                ||!Boolean.TRUE.equals(compiledReport.get("parityPassed"))
                ||!originalMetadata.equals(compiledReport.get("sourceMetadataSha256")))
            throw new IOException("Verified original/compiled lineage required");
        String model=BrnArchitectureControls.modelIdentity(original);
        if(!model.equals(BrnArchitectureControls.modelIdentity(compiled)))throw new IOException("Different model weights");
        var first=BrnArchitectureControls.loadView(original,gain);
        var second=BrnArchitectureControls.loadView(compiled,gain);
        var held=BrnResearchData.read(data,false).validation();
        if(held.size()<128)throw new IOException("128 validation roots required");
        var requests=BrnArchitectureTransitions.requests(held.subList(0,128).stream().map(BrnResearchData.Example::board).toList());
        String requestHash=BrnArchitectureTransitions.digest(requests);
        ExactEvaluator[] evaluators={first.evaluator().get(),second.evaluator().get()};
        var parity=BrnArchitectureTransitions.verify(requests,evaluators[1],first.evaluator());
        if(((Number)parity.get("nonzeroChildScoreDifferences")).intValue()!=0
                ||((Number)parity.get("nonzeroParentScoreDifferences")).intValue()!=0)
            throw new AssertionError("C02 requires exact integer parity on this request stream");
        String[] names={"all","nonking","king","capture","promotion","castle","enPassant"};
        int[] counts=new int[names.length];
        for(var request:requests)for(int group=0;group<names.length;group++)
            if(BrnArchitectureTransitions.inGroup(request,group))counts[group]++;
        double[][][] ns=new double[2][names.length][10];long total=0;
        for(int round=-64;round<10;round++) {
            // Whole passes alternate order: five original-first and five compiled-first timed pairs.
            for(int side=0;side<2;side++) {
                int which=(round&1)^side;var evaluator=evaluators[which];long[] sums=new long[names.length];
                for(var request:requests) {
                    evaluator.initialize(request.parent());total+=evaluator.evaluate(request.parent(),0);
                    long start=System.nanoTime();evaluator.child(request.parent(),request.child(),0);
                    int score=evaluator.evaluate(request.child(),1);long elapsed=System.nanoTime()-start;total+=score;
                    if(round>=0)for(int group=0;group<names.length;group++)
                        if(BrnArchitectureTransitions.inGroup(request,group))sums[group]+=elapsed;
                }
                if(round>=0)for(int group=0;group<names.length;group++)
                    ns[which][group][round]=counts[group]==0?0:sums[group]/(double)counts[group];
            }
        }
        sink=total;
        var groups=new LinkedHashMap<String,Object>();
        for(int group=0;group<names.length;group++)if(counts[group]>0) {
            double oldMedian=median(ns[0][group]);
            double newMedian=median(ns[1][group]);
            groups.put(names[group],Map.of("requestsPerPass",counts[group],"originalPassesNs",ns[0][group],
                    "compiledPassesNs",ns[1][group],"originalMedianNs",oldMedian,"compiledMedianNs",newMedian,
                    "compiledOverOriginalMedian",newMedian/oldMedian));
        }
        if(!originalMetadata.equals(BrnResearchComparison.digest(original.resolve("result.json")))
                ||!compiledMetadata.equals(BrnResearchComparison.digest(compiled.resolve("result.json")))
                ||!model.equals(BrnArchitectureControls.modelIdentity(original))
                ||!model.equals(BrnArchitectureControls.modelIdentity(compiled)))
            throw new IOException("Model or metadata changed during paired runtime check");
        if(!requestHash.equals(BrnArchitectureTransitions.digest(requests)))
            throw new AssertionError("Timed evaluators modified requests");
        var report=new LinkedHashMap<String,Object>();
        report.put("schema","brn-architecture-tuple-paired-runtime-v1");report.put("arguments",a);
        report.put("modelSha256",model);report.put("originalMetadataSha256",originalMetadata);
        report.put("compiledMetadataSha256",compiledMetadata);report.put("gain",gain);
        report.put("datasetManifest",DataFiles.read(data.resolve("manifest.json"),Map.class));
        report.put("requestSha256",requestHash);report.put("verification",parity);
        report.put("groups",groups);report.put("complete",true);
        report.put("method","Same common legal/special requests and model weights in one JVM; 64 warmup pass pairs, then 10 timed pass pairs with alternating first evaluator; initialize/parent score outside child+score interval; nanoTime overhead included; native worker-local caches, no zero-weight shortcut. Complements separate native-loader runtime/search probes; no playing-strength inference.");
        Files.createDirectory(out);DataFiles.write(out.resolve("result.json"),report);
        System.out.println(DataFiles.JSON.toJson(report));
    }
    private BrnTuplePairedRuntime(){}
}
