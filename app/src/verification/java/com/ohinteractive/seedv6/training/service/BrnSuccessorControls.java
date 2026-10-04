package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.brn3.*;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.training.nnue.*;
import com.ohinteractive.seedv6.training.data.DataFiles;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.ToDoubleFunction;

/** Matched exposure/selection using unchanged production trainers, never a user store. */
public final class BrnSuccessorControls {
    @SuppressWarnings("unchecked")
    public static void main(String[] args)throws Exception {
        if(args.length!=6&&args.length!=7)throw new IllegalArgumentException("DATA NEW_OUT BRN3|NNUE COUNT EPOCHS SEED [NNUE_RESUME_DIR]");
        Path dataPath=Path.of(args[0]),out=Path.of(args[1]);if(Files.exists(out))throw new IllegalArgumentException("Use new output");
        if(!Set.of("BRN3","NNUE").contains(args[2]))throw new IllegalArgumentException("control");
        var data=BrnResearchData.read(dataPath,false);int count=Integer.parseInt(args[3]),epochs=Integer.parseInt(args[4]);long seed=Long.parseLong(args[5]);
        if(count<2||count>data.training().size()||epochs<1||epochs>8)throw new IllegalArgumentException("exposure");
        var train=data.training().subList(0,count);var validation=data.validation().subList(0,Math.min(4096,data.validation().size()));
        boolean brn=args[2].equals("BRN3");var trainer=brn?new Brn3Trainer(seed):null;
        if(brn&&args.length==7)throw new IllegalArgumentException("This bounded continuation is NNUE-only");
        NnueTrainer nnue=null;
        if(!brn) {
            if(args.length==7)try(var input=new BufferedInputStream(Files.newInputStream(Path.of(args[6]).resolve("selected.state")))){nnue=TrainingStateCodec.read(input);}
            else nnue=new NnueTrainer(TrainableNnue.initialized(seed));
        }
        ToDoubleFunction<long[]> prediction=brn?trainer::predictPawns:nnue::predict;
        Files.createDirectories(out);var report=new LinkedHashMap<String,Object>();var events=new ArrayList<Map<String,Object>>();
        report.put("arguments",List.of(args));report.put("datasetManifest",DataFiles.read(dataPath.resolve("manifest.json"),Map.class));
        report.put("trainingParameters",brn?Brn3Layout.TRAINING_PARAMETERS:NnueNetwork.PARAMETER_COUNT);
        report.put("recipe",brn?"production BRN3 maskedAdam .003/.9/.999/1e-8 pure CE":"unchanged production NNUE default Adam and outcome half-MSE");
        report.put("selection","first4096 validation; fresh initialization lineage,explicit exact resume if identified; same seed+epoch shuffle; batch128; no sealed test");
        report.put("vectorModule",ModuleLayer.boot().findModule("jdk.incubator.vector").isPresent());
        report.put("materialValidation",BrnResearchMain.metrics(validation,e->RelationalCandidate.material(e.board()),true));
        var hashes=new LinkedHashMap<String,String>();
        for(var type:new Class<?>[]{BrnSuccessorControls.class,Brn3Trainer.class,Class.forName("com.ohinteractive.seedv6.core.brn3.Brn3VectorKernels"),NnueTrainer.class,TrainableNnue.class}) {
            try(var input=new java.security.DigestInputStream(type.getResourceAsStream("/"+type.getName().replace('.','/')+".class"),DataFiles.digest())) {
                input.transferTo(OutputStream.nullOutputStream());hashes.put(type.getName(),HexFormat.of().formatHex(input.getMessageDigest().digest()));
            }
        }
        report.put("implementationClassesSha256",hashes);
        double best=Double.POSITIVE_INFINITY;long trainingNs=0;int startEpoch=0;
        if(args.length==7) {
            Path priorPath=Path.of(args[6]);var prior=DataFiles.read(priorPath.resolve("result.json"),Map.class);
            var priorArgs=(List<String>)prior.get("arguments");
            var manifest=(Map<?,?>)report.get("datasetManifest");var priorManifest=(Map<?,?>)prior.get("datasetManifest");
            if(!priorArgs.subList(2,6).equals(List.of(args[2],args[3],args[4],args[5]))
                    ||!Objects.equals(manifest.get("payloadSha256"),priorManifest.get("payloadSha256")))throw new IllegalArgumentException("Resume recipe/data mismatch");
            startEpoch=((Number)prior.get("selectedEpoch")).intValue();
            if(startEpoch<1||startEpoch>epochs||nnue.optimizer().step()!=(long)startEpoch*((count+127)/128)
                    ||!nnue.optimizer().hyperparameters().equals(AdamHyperparameters.DEFAULT))throw new IllegalArgumentException("Resume epoch/optimizer mismatch");
            byte[] original=Files.readAllBytes(priorPath.resolve("selected.state"));
            if(!Arrays.equals(original,TrainingStateCodec.encode(nnue)))throw new IllegalArgumentException("Resume state is not bit-exact");
            var metrics=BrnResearchMain.metrics(validation,e->prediction.applyAsDouble(e.board()),false);
            var selected=(Map<?,?>)prior.get("selectedValidation");
            if(!Objects.equals(metrics.get("outcomeHalfMse"),selected.get("outcomeHalfMse"))
                    ||!Objects.equals(metrics.get("balancedOutcomeHalfMse"),selected.get("balancedOutcomeHalfMse")))throw new IllegalArgumentException("Resume validation mismatch");
            var snapshot=new ByteArrayOutputStream();NnueNetworkCodec.write(nnue.model().snapshot(),snapshot);
            if(!Arrays.equals(snapshot.toByteArray(),Files.readAllBytes(priorPath.resolve("selected.nnue"))))throw new IllegalArgumentException("Resume network/state mismatch");
            var previousEvents=(List<Map<String,Object>>)prior.get("events");events.addAll(previousEvents.subList(0,startEpoch));
            trainingNs=Math.round(((Number)events.get(startEpoch-1).get("trainingSeconds")).doubleValue()*1e9);
            best=((Number)selected.get("outcomeHalfMse")).doubleValue();report.put("selectedEpoch",startEpoch);report.put("selectedValidation",metrics);
            Files.copy(priorPath.resolve("selected.state"),out.resolve("selected.state"));Files.copy(priorPath.resolve("selected.nnue"),out.resolve("selected.nnue"));
            report.put("resume",Map.of("source",priorPath.toString(),"checkpointEpoch",startEpoch,
                    "sourceStateSha256",BrnResearchComparison.digest(priorPath.resolve("selected.state")),
                    "sourceReportSha256",BrnResearchComparison.digest(priorPath.resolve("result.json")),
                    "previousCompletedEpochs",previousEvents.size(),"note","Original failed run preserved; replay starts at the selected complete epoch. trainingSeconds follows retained useful lineage; sum execution receipts for actual consumed wall time."));
        }
        long[][] boards=new long[128][];double[] targets=new double[128];
        for(int epoch=startEpoch;epoch<epochs;epoch++) {
            int[] order=BrnResearchMain.order(count,seed+epoch);long start=System.nanoTime();
            for(int offset=0;offset<count;offset+=128) {
                int batch=Math.min(128,count-offset);
                for(int i=0;i<batch;i++){var e=train.get(order[offset+i]);boards[i]=e.board();targets[i]=e.outcome();}
                if(brn)trainer.trainBatch(boards,targets,batch);else nnue.trainBatch(boards,targets,batch);
            }
            trainingNs+=System.nanoTime()-start;
            var metrics=BrnResearchMain.metrics(validation,e->prediction.applyAsDouble(e.board()),brn);
            var event=new LinkedHashMap<String,Object>();event.put("epoch",epoch+1);event.put("exposure",(long)count*(epoch+1));event.put("trainingSeconds",trainingNs/1e9);event.put("validation",metrics);events.add(event);
            double loss=(double)metrics.get("outcomeHalfMse");
            if(loss<best) {
                best=loss;report.put("selectedEpoch",epoch+1);report.put("selectedValidation",metrics);
                if(brn) {
                    var candidate=new BrnSuccessorCandidate(BrnSuccessorCandidate.Relations.ABSOLUTE,8,BrnSuccessorCandidate.Pool.SUM,seed);
                    for(int i=0;i<candidate.parameters;i++){candidate.weights[i]=trainer.weight(i);candidate.first[i]=trainer.firstMoment(i);candidate.second[i]=trainer.secondMoment(i);}candidate.updates=trainer.step();
                    Files.write(out.resolve("selected.successor"),candidate.encode());
                }else {
                    try(var stream=new BufferedOutputStream(Files.newOutputStream(out.resolve("selected.nnue")))){NnueNetworkCodec.write(nnue.model().snapshot(),stream);}
                    try(var stream=new BufferedOutputStream(Files.newOutputStream(out.resolve("selected.state")))){TrainingStateCodec.write(nnue,stream);}
                }
            }
            report.put("events",events);DataFiles.write(out.resolve("result.json"),report);System.out.println(DataFiles.JSON.toJson(event));
        }
        report.put("selectedStateSha256",BrnResearchComparison.digest(out.resolve(brn?"selected.successor":"selected.state")));
        DataFiles.write(out.resolve("result.json"),report);
    }
    private BrnSuccessorControls(){}
}
