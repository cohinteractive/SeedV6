package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.brn3.*;
import com.ohinteractive.seedv6.training.data.DataFiles;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** G01 training on the exact E001 population/order; never opens sealed test labels. */
public final class BrnLogicExperiment {
    static BrnArchitectureControls.View view(BrnLogicTrainer.Model model) {
        return view(model,.25);
    }
    static BrnArchitectureControls.View view(BrnLogicTrainer.Model model,double gain) {
        BrnArchitectureControls.requireGain(gain);
        var work=model.newWorkspace();
        return new BrnArchitectureControls.View(b->Brn3Objective.outcome(work.evaluatePawns(b),b),work::evaluatePawns,
                ()->{var cache=model.newWorkspace();return (b,ply)->Brn3Model.score(cache.evaluatePawns(b,gain));});
    }
    static void save(BrnLogicTrainer trainer,Path out,String prefix)throws IOException {
        try(var stream=new BufferedOutputStream(Files.newOutputStream(out.resolve(prefix+".state")))){trainer.write(stream);}
        try(var stream=new BufferedOutputStream(Files.newOutputStream(out.resolve(prefix+".model")))){trainer.snapshot().write(stream);}
    }
    static BrnLogicTrainer load(Path file)throws IOException {
        try(var in=new BufferedInputStream(Files.newInputStream(file))){return BrnLogicTrainer.read(in);}
    }
    public static void main(String[] a)throws Exception {
        if(a.length!=10&&a.length!=11)throw new IllegalArgumentException("DATA NEW_OUT LAYERS COUNT SOFT_EPOCHS HARD_EPOCHS SEED RATE HARD_RATE frozen-v1 [SOFT_HEAD_RATE]");
        Path dataPath=Path.of(a[0]),out=Path.of(a[1]);int layers=Integer.parseInt(a[2]),count=Integer.parseInt(a[3]),softEpochs=Integer.parseInt(a[4]),hardEpochs=Integer.parseInt(a[5]),epochs=softEpochs+hardEpochs;
        long seed=Long.parseLong(a[6]);double rate=Double.parseDouble(a[7]),hardRate=Double.parseDouble(a[8]),headRate=a.length==11?Double.parseDouble(a[10]):rate;
        if(!a[9].equals("frozen-v1")||count<128||count>1048576||softEpochs<0||hardEpochs<1||epochs>32)throw new IllegalArgumentException("protocol");
        var data=BrnResearchData.read(dataPath,false);var train=data.training().subList(0,count);var held=data.validation().subList(0,Math.min(4096,data.validation().size()));
        Files.createDirectory(out);var model=new BrnLogicTrainer(layers,seed,rate,headRate);var report=new LinkedHashMap<String,Object>();var events=new ArrayList<Object>();
        report.put("kind","LOGIC");report.put("arguments",a);report.put("layers",layers);report.put("gates",layers*32);report.put("softHeadRate",headRate);report.put("hardPhaseRate",hardRate);
        report.put("recipe","G01 random fixed shifted graph; normal gate logits; zero rank/file head; "+(softEpochs==0?"initial modal gates FROZEN; head-only Adam "+headRate+" until final epoch at "+hardRate:"soft Boolean mixture then frozen modal gates/head-only refit; CE gate Adam "+rate+"; soft head Adam "+headRate)+"; batch128; play quarter residual");
        report.put("fixedInitialGates",softEpochs==0);report.put("optimizedParameters",softEpochs==0?model.inferenceParameters():model.parameters());
        report.put("datasetManifest",DataFiles.read(dataPath.resolve("manifest.json"),Map.class));report.put("trainingParameters",model.parameters());report.put("modelParameters",model.inferenceParameters());report.put("inferenceRepresentation","float head parameters plus hard byte operator IDs and six topology integers per gate; no soft logits in model");
        report.put("selection","minimum ACTUAL HARD model first4096 validation rounded-CP WDL half-MSE; Gen0 eligible; soft diagnostic never selects; no test labels");report.put("shuffle","seed+epoch-1; identical E001 order");report.put("events",events);
        long trainingNs=0;double best=Double.POSITIVE_INFINITY;long[][] boards=new long[128][];double[] targets=new double[128];
        if(softEpochs==0)model.harden(headRate);
        for(int epoch=0;epoch<=epochs;epoch++) {
            if(epoch>0) {
                if(softEpochs==0){if(epoch==epochs)model.refitRate(hardRate);}
                else if(epoch==softEpochs+1)model.harden(hardRate);
                var permutation=BrnResearchMain.order(count,seed+epoch-1);long start=System.nanoTime();
                for(int offset=0;offset<count;offset+=128){int size=Math.min(128,count-offset);for(int j=0;j<size;j++){var e=train.get(permutation[offset+j]);boards[j]=e.board();targets[j]=e.outcome();}model.trainBatch(boards,targets,size);}
                trainingNs+=System.nanoTime()-start;
            }
            if(model.step()!=(long)epoch*((count+127)/128))throw new AssertionError("Update count");
            var snapshot=model.snapshot();var metrics=BrnArchitectureControls.metrics(held,view(snapshot));double maxError=0;int changedScores=0;
            var cache=snapshot.newWorkspace();
            for(var e:held){double expected=model.predictHardPawns(e.board()),actual=cache.evaluatePawns(e.board());maxError=Math.max(maxError,Math.abs(expected-actual));if(Brn3Model.score(expected)!=Brn3Model.score(actual))changedScores++;}
            if(maxError>1e-4)throw new AssertionError("Float fold/cache budget: "+maxError);
            var event=new LinkedHashMap<String,Object>();event.put("epoch",epoch);event.put("samples",(long)count*epoch);event.put("updates",model.step());event.put("trainingSeconds",trainingNs/1e9);event.put("validation",metrics);event.put("gateDiagnostics",model.diagnostics());event.put("softValidation",softMetrics(held,model));event.put("phase",softEpochs==0?"fixed-gates-head-only":model.hardened()?"hard-head-refit":"soft-gates-and-head");event.put("activeHeadRate",model.hardened()?model.hardRate():model.headRate());event.put("maximumFloatCacheErrorPawns",maxError);event.put("floatIntegerScoreDifferences",changedScores);events.add(event);
            if(epoch==0){Path gen0=out.resolve("gen0");Files.createDirectory(gen0);save(model,gen0,"selected");DataFiles.write(gen0.resolve("result.json"),Map.of("kind","LOGIC","complete",true,"selectedEpoch",0,"selectedModelSha256",BrnResearchComparison.digest(gen0.resolve("selected.model"))));}
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
    static Map<String,Object> softMetrics(List<BrnResearchData.Example> held,BrnLogicTrainer model) {
        double mse=0,ce=0;
        for(var e:held){double value=model.predictSoftPawns(e.board()),error=Brn3Objective.outcome(value,e.board())-e.outcome();mse+=.5*error*error;ce+=Brn3Objective.crossEntropy(value,e.sfMaterial(),e.outcome())[0];}
        return Map.of("positions",held.size(),"outcomeHalfMse",mse/held.size(),"rawCrossEntropy",ce/held.size(),"diagnosticOnlyNotDeployed",true);
    }
    private BrnLogicExperiment(){}
}
