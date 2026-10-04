package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.training.data.DataFiles;
import java.nio.file.*;
import java.util.*;

/** Explicit bounded empirical work, not a routine test or production architecture. */
public final class BrnSuccessorTrain {
    static volatile double sink;
    public static void main(String[] args)throws Exception {
        if(args.length!=8)throw new IllegalArgumentException("DATA NEW_OUT RELATIONS WIDTH POOL COUNT EPOCHS SEED");
        Path dataPath=Path.of(args[0]),out=Path.of(args[1]);if(Files.exists(out))throw new IllegalArgumentException("Use a new output");
        var data=BrnResearchData.read(dataPath,false);int count=Integer.parseInt(args[5]),epochs=Integer.parseInt(args[6]);long seed=Long.parseLong(args[7]);
        if(count<2||count>data.training().size()||epochs<1||epochs>16)throw new IllegalArgumentException("exposure");
        var train=data.training().subList(0,count);var validation=data.validation().subList(0,Math.min(4096,data.validation().size()));
        var candidate=new BrnSuccessorCandidate(BrnSuccessorCandidate.Relations.valueOf(args[2]),Integer.parseInt(args[3]),BrnSuccessorCandidate.Pool.valueOf(args[4]),seed);
        Files.createDirectories(out);var report=new LinkedHashMap<String,Object>();var events=new ArrayList<Map<String,Object>>();
        report.put("arguments",List.of(args));report.put("datasetManifest",DataFiles.read(dataPath.resolve("manifest.json"),Map.class));
        report.put("trainingParameters",candidate.parameters);report.put("recipe","fresh; maskedAdam .003/.9/.999/1e-8; batch128; pure CE; validation first4096; no sealed test");
        report.put("materialValidation",BrnResearchMain.metrics(validation,e->RelationalCandidate.material(e.board()),true));
        double best=Double.POSITIVE_INFINITY;long trainingNs=0;
        for(int epoch=0;epoch<epochs;epoch++) {
            int[] order=BrnResearchMain.order(count,seed+epoch);long start=System.nanoTime();
            for(int offset=0;offset<count;offset+=128)candidate.trainBatch(train,order,offset,Math.min(128,count-offset),.003);
            trainingNs+=System.nanoTime()-start;
            var metrics=BrnResearchMain.metrics(validation,e->candidate.predict(e.board()),true);
            var quarter=BrnResearchMain.metrics(validation,e->{double m=RelationalCandidate.material(e.board());return m+.25*(candidate.predict(e.board())-m);},true);
            var event=new LinkedHashMap<String,Object>();event.put("epoch",epoch+1);event.put("exposure",(long)count*(epoch+1));event.put("trainingSeconds",trainingNs/1e9);
            event.put("validation",metrics);event.put("quarterValidation",quarter);events.add(event);
            double loss=(double)metrics.get("outcomeHalfMse");if(loss<best){best=loss;report.put("selectedEpoch",epoch+1);report.put("selectedValidation",metrics);report.put("selectedQuarterValidation",quarter);Files.write(out.resolve("selected.successor"),candidate.encode());}
            report.put("events",events);DataFiles.write(out.resolve("result.json"),report);System.out.println(DataFiles.JSON.toJson(event));
        }
        var selected=BrnSuccessorCandidate.decode(Files.readAllBytes(out.resolve("selected.successor")));double sum=0;
        for(int r=0;r<4;r++)for(var e:validation)sum+=selected.predict(e.board());double[] timings=new double[5];
        for(int r=0;r<timings.length;r++){long start=System.nanoTime();for(var e:validation)sum+=selected.predict(e.board());timings[r]=(System.nanoTime()-start)/(double)validation.size();}sink=sum;
        report.put("uncachedDoubleInferenceNs",timings);report.put("selectedStateSha256",BrnResearchComparison.digest(out.resolve("selected.successor")));
        report.put("inferenceNote","Generic double research implementation; not comparable to optimized folded production BRN-3 latency without further work");
        DataFiles.write(out.resolve("result.json"),report);
    }
    private BrnSuccessorTrain(){}
}
