package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.brn3.*;
import com.ohinteractive.seedv6.training.data.DataFiles;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** E01 training on the exact E001 population/order; never opens sealed test labels. */
public final class BrnSplineExperiment {
    static BrnArchitectureControls.View view(BrnSplineTrainer.Model model) {
        return view(model,.25);
    }
    static BrnArchitectureControls.View view(BrnSplineTrainer.Model model,double gain) {
        BrnArchitectureControls.requireGain(gain);
        var work=model.newWorkspace();
        return new BrnArchitectureControls.View(b->Brn3Objective.outcome(work.evaluatePawns(b),b),work::evaluatePawns,
                ()->{var cache=model.newWorkspace();return (b,ply)->Brn3Model.score(cache.evaluatePawns(b,gain));});
    }
    static void save(BrnSplineTrainer trainer,Path out,String prefix)throws IOException {
        try(var stream=new BufferedOutputStream(Files.newOutputStream(out.resolve(prefix+".state")))){trainer.write(stream);}
        try(var stream=new BufferedOutputStream(Files.newOutputStream(out.resolve(prefix+".model")))){trainer.snapshot().write(stream);}
    }
    static BrnSplineTrainer load(Path file)throws IOException {
        try(var in=new BufferedInputStream(Files.newInputStream(file))){return BrnSplineTrainer.read(in);}
    }
    public static void main(String[] a)throws Exception {
        if(a.length==4&&a[0].equals("ablate")){ablate(Path.of(a[1]),Path.of(a[2]),Path.of(a[3]));return;}
        if(a.length!=8&&a.length!=9)throw new IllegalArgumentException("DATA NEW_OUT KNOTS COUNT EPOCHS SEED RATE frozen-v1 [SPACING]");
        Path dataPath=Path.of(a[0]),out=Path.of(a[1]);int knots=Integer.parseInt(a[2]),count=Integer.parseInt(a[3]),epochs=Integer.parseInt(a[4]);long seed=Long.parseLong(a[5]);double rate=Double.parseDouble(a[6]);
        if(!a[7].equals("frozen-v1")||count<128||count>1048576||epochs<1||epochs>32)throw new IllegalArgumentException("protocol");
        var data=BrnResearchData.read(dataPath,false);var train=data.training().subList(0,count);var held=data.validation().subList(0,Math.min(4096,data.validation().size()));
        Files.createDirectory(out);var model=new BrnSplineTrainer(knots,seed,rate,a.length==9?Double.parseDouble(a[8]):.5);var report=new LinkedHashMap<String,Object>();var events=new ArrayList<Object>();
        report.put("gridSpacing",model.spacing());report.put("gridMaximum",knots*model.spacing());
        report.put("kind","EDGE_PWL");report.put("arguments",a);report.put("knots",knots);report.put("recipe","E01 zero head; same BRN3 encoder initialization; M+sum learned16-knot edge functions CE; masked Adam "+rate+"; batch128; play quarter residual");
        report.put("datasetManifest",DataFiles.read(dataPath.resolve("manifest.json"),Map.class));report.put("trainingParameters",model.parameters());report.put("modelParameters",model.inferenceParameters());
        report.put("selection","minimum first4096 validation common rounded-CP WDL half-MSE; Gen0 eligible; no test labels");report.put("shuffle","seed+epoch-1; identical E001 order");report.put("events",events);
        long trainingNs=0;double best=Double.POSITIVE_INFINITY;long[][] boards=new long[128][];double[] targets=new double[128];
        for(int epoch=0;epoch<=epochs;epoch++) {
            if(epoch>0) {
                var order=BrnResearchMain.order(count,seed+epoch-1);long start=System.nanoTime();
                for(int offset=0;offset<count;offset+=128){int size=Math.min(128,count-offset);for(int j=0;j<size;j++){var e=train.get(order[offset+j]);boards[j]=e.board();targets[j]=e.outcome();}model.trainBatch(boards,targets,size);}
                trainingNs+=System.nanoTime()-start;
            }
            if(model.step()!=(long)epoch*((count+127)/128))throw new AssertionError("Update count");
            var snapshot=model.snapshot();var metrics=BrnArchitectureControls.metrics(held,view(snapshot));double maxError=0,maxPooled=0;long aboveGrid=0;int changedScores=0;
            var cache=snapshot.newWorkspace();
            for(var e:held){double expected=model.predictPawns(e.board()),actual=cache.evaluatePawns(e.board());maxError=Math.max(maxError,Math.abs(expected-actual));maxPooled=Math.max(maxPooled,model.maximumPooled());aboveGrid+=model.pooledAboveGrid();if(Brn3Model.score(expected)!=Brn3Model.score(actual))changedScores++;}
            if(maxError>1e-4)throw new AssertionError("Float fold/cache budget: "+maxError);
            var event=new LinkedHashMap<String,Object>();event.put("epoch",epoch);event.put("samples",(long)count*epoch);event.put("updates",model.step());event.put("trainingSeconds",trainingNs/1e9);event.put("validation",metrics);event.put("maximumPooled",maxPooled);event.put("pooledAboveGridFraction",aboveGrid/(held.size()*192.0));event.put("maximumFloatCacheErrorPawns",maxError);event.put("floatIntegerScoreDifferences",changedScores);events.add(event);
            if(epoch==0){Path gen0=out.resolve("gen0");Files.createDirectory(gen0);save(model,gen0,"selected");DataFiles.write(gen0.resolve("result.json"),Map.of("kind","EDGE_PWL","complete",true,"selectedEpoch",0,"selectedModelSha256",BrnResearchComparison.digest(gen0.resolve("selected.model"))));}
            double loss=(double)metrics.get("outcomeHalfMse");if(loss<best){best=loss;save(model,out,"selected");report.put("selectedEpoch",epoch);report.put("selectedValidation",metrics);}
            save(model,out,"last");report.put("completedEpochs",epoch);report.put("selectedModelSha256",BrnResearchComparison.digest(out.resolve("selected.model")));report.put("selectedStateSha256",BrnResearchComparison.digest(out.resolve("selected.state")));report.put("lastStateSha256",BrnResearchComparison.digest(out.resolve("last.state")));
            BrnArchitectureControls.retainEpoch(out,epoch,event,report);
            DataFiles.write(out.resolve("result.json"),report);System.out.println("epoch="+epoch+" loss="+loss+" trainingSeconds="+trainingNs/1e9);
        }
        var restored=load(out.resolve("last.state"));for(int i=0;i<128;i++){boards[i]=train.get(i).board();targets[i]=train.get(i).outcome();}
        model.trainBatch(boards,targets,128);restored.trainBatch(boards,targets,128);save(model,out,"continuation-a");save(restored,out,"continuation-b");
        if(Files.mismatch(out.resolve("continuation-a.state"),out.resolve("continuation-b.state"))!=-1)throw new AssertionError("Exact next update resume");
        report.put("exactNextBatchResume",true);report.put("complete",true);DataFiles.write(out.resolve("result.json"),report);
    }
    @SuppressWarnings("unchecked")
    static void ablate(Path dataPath,Path run,Path out)throws Exception {
        var source=DataFiles.read(run.resolve("result.json"),Map.class);Path modelPath=run.resolve("selected.model");String hash=BrnResearchComparison.digest(modelPath);
        if(!Boolean.TRUE.equals(source.get("complete"))||!"EDGE_PWL".equals(source.get("kind"))||!hash.equals(source.get("selectedModelSha256")))throw new IOException("Incomplete/changed edge-function source");
        BrnSplineTrainer.Model model;try(var in=new BufferedInputStream(Files.newInputStream(modelPath))){model=BrnSplineTrainer.Model.read(in);}
        var data=BrnResearchData.read(dataPath,false);var held=data.validation().subList(0,Math.min(4096,data.validation().size()));
        var ablated=model.linearized();var fullMetrics=BrnArchitectureControls.metrics(held,view(model));var ablatedMetrics=BrnArchitectureControls.metrics(held,view(ablated));
        Files.createDirectory(out);try(var stream=new BufferedOutputStream(Files.newOutputStream(out.resolve("linearized.model")))){ablated.write(stream);}
        var report=new LinkedHashMap<String,Object>();report.put("kind","EDGE_COMPONENT_AUDIT");report.put("sourceRun",run.toString());report.put("sourceModelSha256",hash);
        report.put("ablationModelSha256",BrnResearchComparison.digest(out.resolve("linearized.model")));report.put("datasetManifest",DataFiles.read(dataPath.resolve("manifest.json"),Map.class));
        report.put("method","Extend learned first segment: q_k=k*q_1; retain encoder and bias; first4096 validation; no test. This removes curvature but is not a best refitted linear head, independently optimized linear control, or cheaper kernel.");
        report.put("fullValidation",fullMetrics);report.put("linearizedValidation",ablatedMetrics);report.put("complete",true);DataFiles.write(out.resolve("result.json"),report);
    }
    private BrnSplineExperiment(){}
}
