package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn.BrnAdamConfig;
import com.ohinteractive.seedv6.core.brn3.*;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.search.exact.ExactEvaluator;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.training.model.NetworkModel;
import com.ohinteractive.seedv6.training.model.TrainingArchitecture;
import com.ohinteractive.seedv6.training.nnue.*;
import com.ohinteractive.seedv6.training.validation.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;

/** Frozen common-data controls for architecture research; never registers or promotes models. */
public final class BrnArchitectureControls {
    enum Kind { BRN3, NNUE_MATERIAL_V2, NNUE_LEGACY, NNUE_STRICT }
    static final class Control {
        final Kind kind;
        Brn3Trainer brn;
        NnueTrainer nnue;
        NnueMaterialResidualTrainer strict;
        Control(Kind kind,long seed) {
            this(kind,seed,kind==Kind.BRN3||kind==Kind.NNUE_STRICT?.003:.001);
        }
        Control(Kind kind,long seed,double rate) {
            if(!Double.isFinite(rate)||rate<=0||rate>.1)throw new IllegalArgumentException("Research learning rate");
            this.kind=kind;
            switch(kind) {
                case BRN3 -> brn=new Brn3Trainer(seed,new BrnAdamConfig(rate));
                case NNUE_LEGACY -> nnue=new NnueTrainer(TrainableNnue.initialized(seed));
                case NNUE_MATERIAL_V2 -> nnue=NnueTrainer.calibratedMaterialParity(TrainableNnue.initialized(seed));
                case NNUE_STRICT -> strict=new NnueMaterialResidualTrainer(seed,new BrnAdamConfig(rate));
            }
            if(nnue!=null)nnue.optimizer().setLearningRate(rate);
        }
        void train(long[][] boards,double[] targets,int count) {
            if(brn!=null)brn.trainBatch(boards,targets,count);
            else if(strict!=null)strict.trainBatch(boards,targets,count);
            else nnue.trainBatch(boards,targets,count);
        }
        long step(){return brn!=null?brn.step():strict!=null?strict.step():nnue.optimizer().step();}
        String recipe(){return switch(kind) {
            case BRN3 -> "production BRN3: zero head; M+R CE; masked Adam .003; play M+.25R";
            case NNUE_MATERIAL_V2 -> NnueMaterialBootstrap.CALIBRATED_TRAINING_ID+"; original random head; Adam .001; production full-range tanh residual";
            case NNUE_LEGACY -> "production legacy: original random head; no material; outcome half-MSE; Adam .001";
            case NNUE_STRICT -> NnueMaterialResidualTrainer.ID+"; masked Adam .003; play M+.25R";
        };}
        double outcome(long[] b){return brn!=null?brn.predictOutcome(b):strict!=null?Brn3Objective.outcome(strict.predictPawns(b),b):nnue.predict(b);}
        double pawns(long[] b) {
            if(brn!=null)return brn.predictPawns(b);
            if(strict!=null)return strict.predictPawns(b);
            throw new IllegalStateException("Use immutable material NNUE view; legacy has no CP prediction");
        }
        void save(Path out,String prefix)throws IOException {
            try(var stream=new BufferedOutputStream(Files.newOutputStream(out.resolve(prefix+".state")))) {
                if(brn!=null)Brn3Codec.writeTraining(brn,stream);
                else if(strict!=null)strict.write(stream);
                else if(kind==Kind.NNUE_MATERIAL_V2)TrainingStateCodec.writeMaterial(nnue,stream);
                else TrainingStateCodec.write(nnue,stream);
            }
            try(var stream=new BufferedOutputStream(Files.newOutputStream(out.resolve(prefix+".model")))) {
                if(brn!=null)Brn3Codec.writeModel(brn.snapshot(),stream);
                else if(strict!=null)NnueNetworkCodec.write(strict.snapshot(),stream);
                else if(kind==Kind.NNUE_MATERIAL_V2)NnueNetworkCodec.writeCalibratedMaterial(nnue.model().snapshot(),stream);
                else NnueNetworkCodec.write(nnue.model().snapshot(),stream);
            }
        }
        static Control load(Kind kind,Path state)throws IOException {
            var c=new Control(kind,0);
            try(var in=new BufferedInputStream(Files.newInputStream(state))) {
                if(c.brn!=null)c.brn=Brn3Codec.readTraining(in);
                else if(c.strict!=null)c.strict=NnueMaterialResidualTrainer.read(in);
                else c.nnue=kind==Kind.NNUE_MATERIAL_V2?TrainingStateCodec.readMaterial(in):TrainingStateCodec.read(in);
            }
            if(c.nnue!=null&&(c.nnue.calibratedOutcome()!=(kind==Kind.NNUE_MATERIAL_V2)
                    ||c.nnue.materialBootstrap()!=(kind==Kind.NNUE_MATERIAL_V2)))throw new IOException("Control objective mismatch");
            return c;
        }
        View view() {
            return brn!=null?brnView(brn.snapshot()):nnueView(kind,strict!=null?strict.snapshot():nnue.model().snapshot());
        }
    }
    record View(ToDoubleFunction<long[]> outcome,ToDoubleFunction<long[]> pawns,
                Supplier<ExactEvaluator> evaluator) {
        Supplier<SearchDriver> drivers(){return ()->new SearchDriver(new ExactSearchAdapter(evaluator.get(),new TTable(4)));}
    }
    static View brnView(Brn3Model model) {
        return brnView(model,.25);
    }
    static void requireGain(double gain) {
        if(!Double.isFinite(gain)||gain<=0||gain>1)throw new IllegalArgumentException("Research residual gain must be in (0,1]");
    }
    static View brnView(Brn3Model model,double gain) {
        requireGain(gain);
        var workspace=model.newWorkspace();
        return new View(b->Brn3Objective.outcome(workspace.evaluatePawns(b),b),workspace::evaluatePawns,
                ()->ExactEvaluator.from(SearchEvaluation.brn3Research(model,gain)));
    }
    static View nnueView(Kind kind,NnueNetwork network) {
        return nnueView(kind,network,kind==Kind.NNUE_STRICT?.25:1);
    }
    static View nnueView(Kind kind,NnueNetwork network,double gain) {
        requireGain(gain);
        var evaluator=new NnueEvaluator(network);
        ToDoubleFunction<long[]> pawns=kind==Kind.NNUE_LEGACY?null:b->{
            double raw=evaluator.evaluate(b),m=NnueMaterialBootstrap.forSideToMove(b,NnueMaterialBootstrap.whiteScore(b))/100.;
            return kind==Kind.NNUE_STRICT?m+raw:Math.max(-325.11,Math.min(325.11,m+325.11*StrictMath.tanh(raw)));
        };
        ToDoubleFunction<long[]> outcome=kind==Kind.NNUE_LEGACY?b->{evaluator.evaluate(b);return evaluator.boundedValue();}
                :b->Brn3Objective.smoothOutcome(pawns.applyAsDouble(b),NnueCorpusTargets.material(b))[0];
        // Strict BRN objective uses rounded CP for diagnostic outcome, as does the production BRN control.
        if(kind==Kind.NNUE_STRICT)outcome=b->Brn3Objective.outcome(pawns.applyAsDouble(b),b);
        return new View(outcome,pawns,()->kind==Kind.NNUE_STRICT?strictEvaluator(network,gain)
                :ExactEvaluator.from(kind==Kind.NNUE_LEGACY?SearchEvaluation.incremental(network,new NnueScoreMapping(32511*gain))
                :SearchEvaluation.incrementalWithMaterial(network,new NnueScoreMapping(32511*gain))));
    }
    /** Same native NNUE accumulator/material lifecycle and rounding; only residual gain varies. */
    static ExactEvaluator strictEvaluator(NnueNetwork network,double gain) {
        requireGain(gain);
        return new ExactEvaluator() {
            final NnueAccumulator[] sums=new NnueAccumulator[257];
            final int[] material=new int[257];
            final NnueEvaluator head=new NnueEvaluator(network);
            {for(int i=0;i<sums.length;i++)sums[i]=new NnueAccumulator(network);}
            @Override public void initialize(long[] b){sums[0].rebuild(b);material[0]=NnueMaterialBootstrap.whiteScore(b);}
            @Override public void child(long[] p,long[] c,int ply){sums[ply+1].update(p,c,sums[ply]);material[ply+1]=NnueMaterialBootstrap.update(p,c,material[ply]);}
            @Override public int evaluate(long[] b,int ply){return Brn3Model.score(NnueMaterialBootstrap.forSideToMove(b,material[ply])/100.+gain*head.evaluate(b,sums[ply]));}
        };
    }
    static View loadView(Path run)throws Exception {return loadView(run,null);}
    static String modelIdentity(Path run)throws Exception {
        if(run.toString().equals("material"))return "material-only";
        if(run.toString().equals("material-fast"))return BrnArchitectureMaterial.ID;
        return BrnResearchComparison.digest(run.resolve("selected.model"));
    }
    static String metadataIdentity(Path run)throws Exception {
        if(run.toString().equals("material")||run.toString().equals("material-fast"))return modelIdentity(run);
        return BrnResearchComparison.digest(run.resolve("result.json"));
    }
    static void requireIdentity(Path run,String model,String metadata)throws Exception {
        if(!model.equals(modelIdentity(run))||!metadata.equals(metadataIdentity(run)))
            throw new IOException("Model or evaluator metadata changed during execution");
    }
    @SuppressWarnings("unchecked")
    static View loadView(Path run,Double requestedGain)throws Exception {
        if(requestedGain!=null)requireGain(requestedGain);
        double brnGain=requestedGain==null?.25:requestedGain,nnueGain=requestedGain==null?1:requestedGain;
        if(run.toString().equals("material"))return brnView(new Brn3Trainer(71).snapshot(),brnGain);
        if(run.toString().equals("material-fast"))return BrnArchitectureMaterial.view();
        var report=DataFiles.read(run.resolve("result.json"),Map.class);
        if(!Boolean.TRUE.equals(report.get("complete")))throw new IOException("Control run is not complete");
        Path model=run.resolve("selected.model");
        if(!BrnResearchComparison.digest(model).equals(report.get("selectedModelSha256")))throw new IOException("Model hash mismatch");
        try(var in=new BufferedInputStream(Files.newInputStream(model))) {
            if(report.get("kind").equals("NATIVE")) {
                var nativeModel=NetworkModel.read(TrainingArchitecture.valueOf((String)report.get("architecture")),in);
                if(nativeModel instanceof NetworkModel.Brn3 brn)return brnView(brn.model(),brnGain);
                if(nativeModel instanceof NetworkModel.Nnue nnue)return nnueView(Kind.NNUE_LEGACY,nnue.network(),nnueGain);
                // Native material v1 and v2 have the same runtime mapping; their training identity stays in the capture report.
                if(nativeModel instanceof NetworkModel.NnueMaterial nnue)return nnueView(Kind.NNUE_MATERIAL_V2,nnue.network(),nnueGain);
                throw new IOException("Unsupported native reference architecture");
            }
            if(report.get("kind").equals("ENERGY2"))return BrnEnergyExperiment.view(BrnEnergyTrainer.Model.read(in),brnGain);
            if(report.get("kind").equals("ENERGY3"))return BrnCubicExperiment.view(BrnCubicTrainer.Model.read(in),brnGain);
            if(report.get("kind").equals("LOGIC"))return BrnLogicExperiment.view(BrnLogicTrainer.Model.read(in),brnGain);
            if(report.get("kind").equals("CONTEXT_LINEAR"))return BrnContextExperiment.view(BrnContextTrainer.Model.read(in),brnGain);
            if(report.get("kind").equals("EDGE_PWL"))return BrnSplineExperiment.view(BrnSplineTrainer.Model.read(in),brnGain);
            if(report.get("kind").equals("TUPLE_COMPILED")) {
                if(!Boolean.TRUE.equals(report.get("parityPassed"))
                        ||!report.get("selectedModelSha256").equals(report.get("sourceModelSha256")))
                    throw new IOException("Verified tuple compilation parity and source identity required");
                return BrnTupleCompilation.view(new BrnTupleCompiled(BrnTupleTrainer.Model.read(in)),brnGain);
            }
            if(report.get("kind").equals("TUPLE"))return BrnTupleExperiment.view(BrnTupleTrainer.Model.read(in),brnGain);
            Kind kind=Kind.valueOf((String)report.get("kind"));
            return kind==Kind.BRN3?brnView(Brn3Codec.readModel(in),brnGain):nnueView(kind,kind==Kind.NNUE_MATERIAL_V2
                    ?NnueNetworkCodec.readCalibratedMaterial(in):NnueNetworkCodec.read(in),kind==Kind.NNUE_STRICT?brnGain:nnueGain);
        }
    }
    static Map<String,Object> metrics(List<BrnResearchData.Example> data,View v) {
        if(data.isEmpty())throw new IllegalArgumentException("Empty quality population");
        double mse=0,nativeMse=0,balanced=0,searchMse=0,ce=0,mae=0,residual=0;int balancedN=0,saturated=0,signWrong=0,signN=0;
        var phase=new LinkedHashMap<String,double[]>();var material=new LinkedHashMap<String,double[]>();
        var evaluator=v.evaluator.get();
        for(var e:data) {
            var b=e.board();double target=e.outcome(),nativePrediction=v.outcome.applyAsDouble(b);
            double pawnPrediction=v.pawns==null?0:v.pawns.applyAsDouble(b);
            double prediction=v.pawns==null?nativePrediction:Brn3Objective.outcome(pawnPrediction,b),error=prediction-target;
            nativeMse+=.5*(nativePrediction-target)*(nativePrediction-target);
            mse+=.5*error*error;if(Math.abs(e.cp())<=200){balanced+=.5*error*error;balancedN++;}
            evaluator.initialize(b);int score=evaluator.evaluate(b,0);
            if(Math.abs(score)>=32511)saturated++;
            if(v.pawns!=null) {
                double p=pawnPrediction,r=p-Brn3Features.material(b);residual+=r*r;mae+=Math.abs(p-e.cp()/100.);
                ce+=Brn3Objective.crossEntropy(p,e.sfMaterial(),target)[0];
                double se=Brn3Objective.outcome(score/100.,b)-target;searchMse+=.5*se*se;
                if(Math.abs(e.cp())>=50){signN++;if(Math.signum(p)!=Math.signum(e.cp()))signWrong++;}
            }
            int pieces=Long.bitCount(b[0]|b[1]|b[2]);double prior=Brn3Features.material(b),abs=Math.abs(prior);
            String phaseKey=pieces>=24?"pieces_24_plus":pieces>=12?"pieces_12_to_23":"pieces_0_to_11";
            String materialKey=abs<=.5?"abs_fixed_prior_le_0_5":abs<=3?"abs_fixed_prior_gt_0_5_le_3":"abs_fixed_prior_gt_3";
            double searchError=v.pawns==null?0:Brn3Objective.outcome(score/100.,b)-target;
            for(double[] bucket:new double[][]{phase.computeIfAbsent(phaseKey,k->new double[5]),material.computeIfAbsent(materialKey,k->new double[5])}) {
                bucket[0]++;bucket[1]+=.5*error*error;bucket[2]+=.5*searchError*searchError;
                bucket[3]+=(pawnPrediction-prior)*(pawnPrediction-prior);bucket[4]+=Math.abs(pawnPrediction-e.cp()/100.);
            }
        }
        int n=data.size();var result=new LinkedHashMap<String,Object>();result.put("positions",n);
        result.put("outcomeHalfMse",mse/n);if(balancedN>0)result.put("balancedHalfMse",balanced/balancedN);
        result.put("nativeOutcomeHalfMse",nativeMse/n);
        result.put("balancedCount",balancedN);result.put("saturatedSearchScores",saturated);
        if(v.pawns!=null){result.put("rawCrossEntropy",ce/n);result.put("searchHalfMse",searchMse/n);result.put("cpMaePawns",mae/n);
            result.put("residualRmsPawns",Math.sqrt(residual/n));result.put("signMistakes",signWrong);result.put("signPopulation",signN);}
        result.put("diagnosticVersion","quality-strata-v2");result.put("phaseProxy","occupied piece count, not move number or inferred game identity");
        result.put("phaseStrata",strata(phase,v.pawns!=null));result.put("materialStrata",strata(material,v.pawns!=null));
        return result;
    }
    private static Map<String,Object> strata(Map<String,double[]> sums,boolean hasPawns) {
        var result=new LinkedHashMap<String,Object>();
        for(var entry:sums.entrySet()) {
            double[] b=entry.getValue();var row=new LinkedHashMap<String,Object>();row.put("positions",(int)b[0]);row.put("outcomeHalfMse",b[1]/b[0]);
            if(hasPawns){row.put("searchHalfMse",b[2]/b[0]);row.put("residualRmsPawns",Math.sqrt(b[3]/b[0]));row.put("cpMaePawns",b[4]/b[0]);}
            result.put(entry.getKey(),row);
        }
        return result;
    }
    static void train(String[] a)throws Exception {
        if(a.length!=8&&a.length!=9)throw new IllegalArgumentException("train DATA NEW_OUT KIND COUNT EPOCHS SEED frozen-v1 [RESEARCH_RATE]");
        Path dataPath=Path.of(a[1]),out=Path.of(a[2]);Kind kind=Kind.valueOf(a[3]);int count=Integer.parseInt(a[4]),epochs=Integer.parseInt(a[5]);long seed=Long.parseLong(a[6]);
        if(!a[7].equals("frozen-v1"))throw new IllegalArgumentException("Explicit frozen-v1 recipe required");
        if(count<128||count>1048576||epochs<1||epochs>32)throw new IllegalArgumentException("Exposure bounds");
        var data=BrnResearchData.read(dataPath,false);var training=data.training().subList(0,count);
        var held=data.validation().subList(0,Math.min(4096,data.validation().size()));Files.createDirectory(out);
        double rate=a.length==9?Double.parseDouble(a[8]):kind==Kind.BRN3||kind==Kind.NNUE_STRICT?.003:.001;
        var control=new Control(kind,seed,rate);var report=new LinkedHashMap<String,Object>();var events=new ArrayList<Object>();
        report.put("schema","brn-architecture-control-v1");report.put("kind",kind);report.put("arguments",a);report.put("recipe",control.recipe());
        report.put("actualLearningRate",rate);report.put("researchRateOverride",a.length==9);
        if(a.length==9)report.put("recipe",control.recipe()+"; explicit research learning-rate override="+rate);
        report.put("datasetManifest",DataFiles.read(dataPath.resolve("manifest.json"),Map.class));
        report.put("trainingParameters",kind==Kind.BRN3?Brn3Layout.TRAINING_PARAMETERS:NnueNetwork.PARAMETER_COUNT);
        report.put("modelParameters",kind==Kind.BRN3?Brn3Layout.MODEL_PARAMETERS:NnueNetwork.PARAMETER_COUNT);
        report.put("selection","minimum first4096 validation raw outcome half-MSE; Gen0 and all complete epochs eligible; no sealed test");
        report.put("qualityLink","same rounded-CP STOCKFISH_WDL_V1 for every pawn-output control; legacy uses its native outcome; native outcome error also reported separately");
        report.put("shuffle","seed+epoch-1 Fisher-Yates; same seed/order across architectures; batch128");report.put("events",events);
        long trainingNs=0;double best=Double.POSITIVE_INFINITY;
        long[][] boards=new long[128][];double[] targets=new double[128];
        for(int epoch=0;epoch<=epochs;epoch++) {
            if(epoch>0) {
                var order=BrnResearchMain.order(count,seed+epoch-1);long begun=System.nanoTime();
                for(int offset=0;offset<count;offset+=128){int size=Math.min(128,count-offset);
                    for(int j=0;j<size;j++){var e=training.get(order[offset+j]);boards[j]=e.board();targets[j]=e.outcome();}control.train(boards,targets,size);}
                trainingNs+=System.nanoTime()-begun;
            }
            if(control.step()!=(long)epoch*((count+127)/128))throw new AssertionError("Update count");
            var validation=metrics(held,control.view());var event=new LinkedHashMap<String,Object>();
            if(epoch==0) {
                Path gen0=out.resolve("gen0");Files.createDirectory(gen0);control.save(gen0,"selected");
                DataFiles.write(gen0.resolve("result.json"),Map.of("kind",kind,"selectedEpoch",0,"complete",true,
                        "selectedModelSha256",BrnResearchComparison.digest(gen0.resolve("selected.model"))));
            }
            event.put("epoch",epoch);event.put("samples",(long)epoch*count);event.put("updates",control.step());event.put("trainingSeconds",trainingNs/1e9);event.put("validation",validation);
            events.add(event);double loss=(double)validation.get("outcomeHalfMse");
            if(loss<best){best=loss;control.save(out,"selected");report.put("selectedEpoch",epoch);report.put("selectedValidation",validation);}
            control.save(out,"last");report.put("completedEpochs",epoch);
            report.put("selectedModelSha256",BrnResearchComparison.digest(out.resolve("selected.model")));
            report.put("selectedStateSha256",BrnResearchComparison.digest(out.resolve("selected.state")));
            report.put("lastStateSha256",BrnResearchComparison.digest(out.resolve("last.state")));
            retainEpoch(out,epoch,event,report);
            DataFiles.write(out.resolve("result.json"),report);System.out.println("epoch="+epoch+" loss="+loss+" trainSeconds="+trainingNs/1e9);
        }
        // Exact persisted continuation check on the next frozen batch; does not alter saved states.
        var restored=Control.load(kind,out.resolve("last.state"));
        for(int j=0;j<128;j++){boards[j]=training.get(j).board();targets[j]=training.get(j).outcome();}
        control.train(boards,targets,128);restored.train(boards,targets,128);
        control.save(out,"continuation-a");restored.save(out,"continuation-b");
        if(!BrnResearchComparison.digest(out.resolve("continuation-a.state")).equals(BrnResearchComparison.digest(out.resolve("continuation-b.state"))))throw new AssertionError("Resume mismatch");
        report.put("exactNextBatchResume",true);report.put("complete",true);DataFiles.write(out.resolve("result.json"),report);
    }
    /** Immutable inference endpoints; retain existing selected/last optimizer states separately. */
    static void retainEpoch(Path out,int epoch,Map<String,Object> event,Map<String,Object> report)throws IOException {
        if(epoch==0)return; // Existing gen0 already retains both model and state.
        Path endpoint=out.resolve("epochs").resolve(String.format("epoch-%03d",epoch));Files.createDirectories(endpoint.getParent());Files.createDirectory(endpoint);
        Files.copy(out.resolve("last.model"),endpoint.resolve("selected.model"));
        var row=new LinkedHashMap<String,Object>();row.put("kind",report.get("kind"));row.put("complete",true);
        row.put("sourceRun",out.toString());row.put("selectedEpoch",epoch);row.put("endpoint",event);
        row.put("selectedModelSha256",BrnResearchComparison.digest(endpoint.resolve("selected.model")));
        row.put("note","Complete epoch model only; this is not a claim that its parent run or next-batch verification completed");
        DataFiles.write(endpoint.resolve("result.json"),row);
    }
    static volatile long sink;
    static void measure(String[] a)throws Exception {
        if(a.length!=6&&a.length!=7)throw new IllegalArgumentException("measure DATA RUN OUT COUNT DEPTH [GAIN]");
        Path data=Path.of(a[1]),run=Path.of(a[2]),out=Path.of(a[3]);int count=Integer.parseInt(a[4]),depth=Integer.parseInt(a[5]);
        if(count<1||count>128||depth<1||depth>5)throw new IllegalArgumentException("Root bounds");Files.createDirectory(out);
        Double gain=a.length==7?Double.parseDouble(a[6]):null;
        String modelHash=modelIdentity(run),metadataHash=metadataIdentity(run);
        var held=BrnResearchData.read(data,false).validation();var v=loadView(run,gain);var report=new LinkedHashMap<String,Object>();
        report.put("gain",gain==null?"native-default":gain);
        report.put("arguments",a);report.put("modelSha256",modelHash);report.put("modelMetadataIdentity",metadataHash);
        report.put("validation",metrics(held.subList(0,Math.min(4096,held.size())),v));
        report.put("method","initialize+score requests, normal BRN rolling cache retained, System.nanoTime;64 warmup then9 measured passes over first1024 validation; production factories; separate fixed-depth roots with cold private4MiBTT;2M nodes/5s guards; one thread/static leaves");
        var evaluator=v.evaluator.get();long sum=0;int n=Math.min(1024,held.size());double[] ns=new double[9];
        for(int round=-64;round<9;round++){long start=System.nanoTime();for(int i=0;i<n;i++){var b=held.get(i).board();evaluator.initialize(b);sum+=evaluator.evaluate(b,0);}if(round>=0)ns[round]=(System.nanoTime()-start)/(double)n;}sink=sum;
        report.put("fullRefreshScoreNs",ns);report.put("medianNs",Brn3RuntimeMeasurement.median(ns));
        // Warm the shared search machinery separately; evaluator warmup alone did not compile it.
        BrnSuccessorMatch.warm(v.drivers(),v.drivers());report.put("searchWarmup","identical8 fixed depth4 openings per factory before timed roots");
        var roots=new ArrayList<Object>();report.put("roots",roots);
        for(int i=0;i<count;i++)try(var driver=v.drivers().get()) {
            var board=held.get(i).board();long start=System.nanoTime();var control=SearchControl.controlled(2_000_000,start,5_000_000_000L,TimeSource.SYSTEM);
            var result=driver.search(new SearchRequest(board,GameHistory.initial(board),depth,SearchObserver.NONE,control,false));
            var last=result.lastCompletedResult();var row=new LinkedHashMap<String,Object>();row.put("index",i);row.put("complete",result.targetDepthCompleted());row.put("nodes",control.nodes());row.put("seconds",(System.nanoTime()-start)/1e9);
            if(last!=null){row.put("score",last.score());row.put("bestMove",Long.toUnsignedString(last.bestMove(),16));row.put("depth",last.depth());}
            roots.add(row);DataFiles.write(out.resolve("result.json"),report);
        }
        requireIdentity(run,modelHash,metadataHash);report.put("modelIdentityStable",true);
        DataFiles.write(out.resolve("result.json"),report);
    }
    static void match(String[] a)throws Exception {
        if(a.length!=9&&a.length!=11)throw new IllegalArgumentException("match FIRST SECOND OUT PAIRS DEPTH MILLIS OFFSET SEED [FIRST_GAIN SECOND_GAIN]");
        Path first=Path.of(a[1]),second=Path.of(a[2]),out=Path.of(a[3]);int pairs=Integer.parseInt(a[4]),depth=Integer.parseInt(a[5]),millis=Integer.parseInt(a[6]),offset=Integer.parseInt(a[7]);long seed=Long.parseLong(a[8]);
        if(pairs<1||pairs>128||depth<0||depth>5||millis<1||millis>100||offset<0)throw new IllegalArgumentException("Match bounds");
        Double firstGain=a.length==11?Double.parseDouble(a[9]):null,secondGain=a.length==11?Double.parseDouble(a[10]):null;
        String firstModel=modelIdentity(first),firstMetadata=metadataIdentity(first);
        String secondModel=modelIdentity(second),secondMetadata=metadataIdentity(second);
        var candidate=loadView(first,firstGain).drivers();var opponent=loadView(second,secondGain).drivers();Files.createDirectory(out);
        var config=new ValidationConfig(pairs,seed,6,10,Math.max(1,depth),1,NnueScoreMapping.V1,1024);
        var report=new LinkedHashMap<String,Object>();report.put("arguments",a);report.put("candidate",first.toString());report.put("opponent",second.toString());
        report.put("candidateGain",firstGain==null?"native-default":firstGain);report.put("opponentGain",secondGain==null?"native-default":secondGain);
        report.put("candidateModelSha256",firstModel);report.put("candidateMetadataIdentity",firstMetadata);
        report.put("opponentModelSha256",secondModel);report.put("opponentMetadataIdentity",secondMetadata);
        report.put("protocol","paired colours; uniform6..10-ply openings; private4MiBTT; static leaves;1024-ply cap is unscored;10min batch guard; identical8-root warmup");
        report.put("maximumPlies",config.maximumPlies());report.put("protocolVersion","architecture-match-v2-explicit-cap");
        DataFiles.write(out.resolve("config.json"),report);BrnSuccessorMatch.warm(candidate,opponent);
        long deadline=System.nanoTime()+600_000_000_000L;var rows=new ArrayList<BrnSuccessorMatch.Pair>();var root=Board.startingPosition();
        for(int i=offset;i<offset+pairs;i++) {
            var opening=ValidationArena.opening(root,GameHistory.initial(root),config,i);
            var one=BrnSuccessorMatch.play(opening,candidate,opponent,i%2,depth,millis,deadline,config.maximumPlies());
            var two=BrnSuccessorMatch.play(opening,candidate,opponent,1-i%2,depth,millis,deadline,config.maximumPlies());
            var pair=new BrnSuccessorMatch.Pair(i,opening.identity(),com.ohinteractive.seedv6.core.util.Fen.fromBoard(opening.board()),i%2==0?one:two,i%2==0?two:one);
            DataFiles.write(out.resolve(String.format("pair-%05d.json",i)),pair);rows.add(pair);
            report.put("summary",BrnSuccessorMatch.summary(rows));report.put("plannedPairs",pairs);report.put("recordedPairs",rows.size());
            long completed=rows.stream().filter(BrnSuccessorMatch.Pair::complete).count();double scoreSum=rows.stream().filter(BrnSuccessorMatch.Pair::complete).mapToDouble(BrnSuccessorMatch.Pair::score).sum();
            report.put("allPlannedPairsScoreBounds",List.of(scoreSum/pairs,(scoreSum+pairs-completed)/pairs));
            DataFiles.write(out.resolve("result.json"),report);System.out.println("pair="+i+" complete="+pair.complete());
            if(System.nanoTime()>=deadline)break;
        }
        requireIdentity(first,firstModel,firstMetadata);requireIdentity(second,secondModel,secondMetadata);
        report.put("modelIdentitiesStable",true);DataFiles.write(out.resolve("result.json"),report);
    }
    public static void main(String[] a)throws Exception {
        switch(a[0]){case "train"->train(a);case "measure"->measure(a);case "match"->match(a);default->throw new IllegalArgumentException("train|measure|match");}
    }
    private BrnArchitectureControls(){}
}
