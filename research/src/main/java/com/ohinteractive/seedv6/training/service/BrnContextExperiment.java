package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.brn3.*;
import com.ohinteractive.seedv6.training.data.DataFiles;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** F01 training on the exact E001 population/order; never opens sealed test labels. */
public final class BrnContextExperiment {
    static BrnArchitectureControls.View view(BrnContextTrainer.Model model) {
        return view(model,.25);
    }
    static BrnArchitectureControls.View view(BrnContextTrainer.Model model,double gain) {
        BrnArchitectureControls.requireGain(gain);
        var work=model.newWorkspace();
        return new BrnArchitectureControls.View(b->Brn3Objective.outcome(work.evaluatePawns(b),b),work::evaluatePawns,
                ()->{var cache=model.newWorkspace();return (b,ply)->Brn3Model.score(cache.evaluatePawns(b,gain));});
    }
    static void save(BrnContextTrainer trainer,Path out,String prefix)throws IOException {
        try(var stream=new BufferedOutputStream(Files.newOutputStream(out.resolve(prefix+".state")))){trainer.write(stream);}
        try(var stream=new BufferedOutputStream(Files.newOutputStream(out.resolve(prefix+".model")))){trainer.snapshot().write(stream);}
    }
    static BrnContextTrainer load(Path file)throws IOException {
        try(var in=new BufferedInputStream(Files.newInputStream(file))){return BrnContextTrainer.read(in);}
    }
    public static void main(String[] a)throws Exception {
        if(a.length!=8)throw new IllegalArgumentException("DATA NEW_OUT SELF|CONTEXT COUNT EPOCHS SEED RATE frozen-v1");
        Path dataPath=Path.of(a[0]),out=Path.of(a[1]);boolean context=a[2].equals("CONTEXT");int count=Integer.parseInt(a[3]),epochs=Integer.parseInt(a[4]);long seed=Long.parseLong(a[5]);double rate=Double.parseDouble(a[6]);
        if((!context&&!a[2].equals("SELF"))||!a[7].equals("frozen-v1")||count<128||count>1048576||epochs<1||epochs>32)throw new IllegalArgumentException("protocol");
        var data=BrnResearchData.read(dataPath,false);var train=data.training().subList(0,count);var held=data.validation().subList(0,Math.min(4096,data.validation().size()));
        Files.createDirectory(out);var model=new BrnContextTrainer(context,seed,rate);var report=new LinkedHashMap<String,Object>();var events=new ArrayList<Object>();
        report.put("kind","CONTEXT_LINEAR");report.put("arguments",a);report.put("contextMode",a[2]);report.put("recipe","F01 zero linear head; same BRN3 encoder initialization; zero A/B/c identity context; M+context+linear CE; masked Adam "+rate+"; batch128; play quarter residual");
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
            var snapshot=model.snapshot();var metrics=BrnArchitectureControls.metrics(held,view(snapshot));double maxError=0;int changedScores=0;
            var cache=snapshot.newWorkspace();
            for(var e:held){double expected=model.predictPawns(e.board()),actual=cache.evaluatePawns(e.board());maxError=Math.max(maxError,Math.abs(expected-actual));if(Brn3Model.score(expected)!=Brn3Model.score(actual))changedScores++;}
            if(maxError>1e-4)throw new AssertionError("Float fold/cache budget: "+maxError);
            var event=new LinkedHashMap<String,Object>();event.put("epoch",epoch);event.put("samples",(long)count*epoch);event.put("updates",model.step());event.put("trainingSeconds",trainingNs/1e9);event.put("validation",metrics);event.put("maximumFloatCacheErrorPawns",maxError);event.put("floatIntegerScoreDifferences",changedScores);events.add(event);
            if(epoch==0){Path gen0=out.resolve("gen0");Files.createDirectory(gen0);save(model,gen0,"selected");DataFiles.write(gen0.resolve("result.json"),Map.of("kind","CONTEXT_LINEAR","complete",true,"selectedEpoch",0,"selectedModelSha256",BrnResearchComparison.digest(gen0.resolve("selected.model"))));}
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
    private BrnContextExperiment(){}
}
