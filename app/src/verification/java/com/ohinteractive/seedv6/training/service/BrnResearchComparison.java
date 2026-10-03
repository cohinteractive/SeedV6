package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.training.nnue.NnueCorpusTargets;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.core.brn3.*;
import java.io.*;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.util.*;
import java.util.function.ToDoubleFunction;

/** Paired, geometry-group-aware outcome comparison. Test opening is explicit. */
public final class BrnResearchComparison {
    static final class Losses {
        int count, wrongBrn, wrongNnue, signEligible; double brn, nnue, material, absoluteCpError, residualSquared;
        void add(BrnResearchData.Example e,double cpPrediction,double nnuePrediction) {
            double target=e.outcome(), prediction=NnueCorpusTargets.wdl(Math.round(cpPrediction*100),e.sfMaterial()).target();
            double prior=RelationalCandidate.material(e.board()), materialPrediction=NnueCorpusTargets.wdl(Math.round(prior*100),e.sfMaterial()).target();
            count++;brn+=.5*Math.pow(prediction-target,2);nnue+=.5*Math.pow(nnuePrediction-target,2);material+=.5*Math.pow(materialPrediction-target,2);
            absoluteCpError+=Math.abs(cpPrediction-e.cp()/100.0);residualSquared+=Math.pow(cpPrediction-prior,2);
            if(Math.abs(e.cp())>=50){signEligible++;if(prediction*target<0)wrongBrn++;if(nnuePrediction*target<0)wrongNnue++;}
        }
        Map<String,Object> report(){return Map.of("count",count,"brnHalfMse",brn/count,"nnueHalfMse",nnue/count,"ratio",brn/nnue,
                "materialHalfMse",material/count,"brnCpMaePawns",absoluteCpError/count,"residualRmsPawns",Math.sqrt(residualSquared/count),
                "signEligible",signEligible,"brnWrongSigns",wrongBrn,"nnueWrongSigns",wrongNnue);}
    }
    static final class Calibration {
        final int[] count=new int[10];final double[] prediction=new double[10],target=new double[10];
        void add(double p,double t){int bin=Math.min(9,(int)((p+1)*5));count[bin]++;prediction[bin]+=p;target[bin]+=t;}
        Map<String,Object> report(){var rows=new ArrayList<Map<String,Object>>();double absolute=0;int total=0;
            for(int i=0;i<10;i++)if(count[i]>0){rows.add(Map.of("bin",i,"count",count[i],"meanPrediction",prediction[i]/count[i],"meanTarget",target[i]/count[i]));absolute+=Math.abs(prediction[i]-target[i]);total+=count[i];}
            return Map.of("bins",rows,"weightedAbsoluteCalibrationError",absolute/total);}
    }
    @SuppressWarnings("unchecked")
    static Map<String,Object> metadata(Path run)throws IOException{return DataFiles.read(run.resolve("result.json"),Map.class);}
    static ToDoubleFunction<long[]> brn(Path run)throws IOException{
        String candidate=(String)metadata(run).get("candidate");
        try(var in=new BufferedInputStream(Files.newInputStream(run.resolve("selected.state")))){
            if(candidate.startsWith("r4")){var model=AbsoluteRelationCandidate.read(in).foldedInference();return model::predict;}
            if(candidate.startsWith("r3")){var model=AnchoredRelationCandidate.read(in);return model::predict;}
            if(candidate.startsWith("r2")){var model=MessageRelationCandidate.read(in);return model::predict;}
            if(candidate.startsWith("r1")){var model=PooledRelationCandidate.read(in);return model::predict;}
            if(candidate.startsWith("r0")){var model=RelationalCandidate.read(in);return model::predict;}
            throw new IOException("Expected BRN candidate");
        }
    }
    static Brn3Model productionModel(Path run)throws IOException {
        Path directory=run.resolve("production-preview"),file=directory.resolve("network.brn3");
        var conversion=DataFiles.read(directory.resolve("conversion.json"),Map.class);
        if(!Objects.equals(conversion.get("sourceResearchStateSha256"),digest(run.resolve("selected.state")))
                || !Objects.equals(conversion.get("modelSha256"),digest(file)))throw new IOException("Stale BRN-3 export");
        try(var input=new BufferedInputStream(Files.newInputStream(file))){return Brn3Codec.readModel(input);}
    }
    static String digest(Path path)throws IOException{
        try(var input=new DigestInputStream(Files.newInputStream(path),DataFiles.digest())){input.transferTo(OutputStream.nullOutputStream());return HexFormat.of().formatHex(input.getMessageDigest().digest());}
    }
    static double[] bootstrap(Collection<Losses> grouped) {
        var groups=grouped.toArray(Losses[]::new);var rng=new SplittableRandom(8017);double[] ratios=new double[2000];
        for(int i=0;i<ratios.length;i++){double a=0,b=0;for(int n=0;n<groups.length;n++){var g=groups[rng.nextInt(groups.length)];a+=g.brn;b+=g.nnue;}ratios[i]=a/b;}
        Arrays.sort(ratios);return new double[]{ratios[49],ratios[1949]};
    }
    public static void main(String[] args)throws Exception{
        if(args.length<4 || args.length>6)throw new IllegalArgumentException("DATA BRN_RUN NNUE_RUN OUT [validation|sealed-test] [production]");
        boolean test=args.length>=5&&args[4].equals("sealed-test");
        if(args.length>=5&&!test&&!args[4].equals("validation"))throw new IllegalArgumentException("Unknown partition");
        boolean production=args.length==6;if(production&&!args[5].equals("production"))throw new IllegalArgumentException("Unknown inference mode");
        Path output=Path.of(args[3]);if(Files.exists(output))throw new IOException("Comparison output already exists");
        var data=BrnResearchData.read(Path.of(args[0]),test);var records=test?data.test():data.validation();
        Path brnRun=Path.of(args[1]),nnueRun=Path.of(args[2]);
        ToDoubleFunction<long[]> predict=production?productionModel(brnRun).newWorkspace()::evaluatePawns:brn(brnRun);NnueNetwork network;
        var brnMeta=metadata(brnRun);var nnueMeta=metadata(nnueRun);
        var brnManifest=(Map<?,?>)brnMeta.get("datasetManifest");var nnueManifest=(Map<?,?>)nnueMeta.get("datasetManifest");
        var currentManifest=DataFiles.read(Path.of(args[0]).resolve("manifest.json"),Map.class);
        if(!"nnue".equals(nnueMeta.get("candidate")) || !Objects.equals(brnManifest.get("payloadSha256"),nnueManifest.get("payloadSha256"))
                || !Objects.equals(brnManifest.get("payloadSha256"),currentManifest.get("payloadSha256")))throw new IOException("Unmatched data or wrong control");
        try(var in=new BufferedInputStream(Files.newInputStream(nnueRun.resolve("selected.nnue")))){network=NnueNetworkCodec.read(in);}
        var evaluator=new NnueEvaluator(network);var strata=new TreeMap<String,Losses>();var groups=new TreeMap<String,Losses>();
        var brnCalibration=new Calibration();var nnueCalibration=new Calibration();
        for(var e:records){double cp=predict.applyAsDouble(e.board());evaluator.evaluate(e.board());double nnue=evaluator.boundedValue();
            double material=Math.abs(RelationalCandidate.material(e.board()));
            String materialBin=material<.5?"material-under-.5":material<2?"material-.5-to-2":material<5?"material-2-to-5":"material-5-plus";
            String phase=e.sfMaterial()<=20?"material-total-up-to-20":e.sfMaterial()<=50?"material-total-21-to-50":"material-total-over-50";
            for(String key:List.of("all",Math.abs(e.cp())<=200?"balanced":"unbalanced",materialBin,phase))strata.computeIfAbsent(key,k->new Losses()).add(e,cp,nnue);
            groups.computeIfAbsent(BrnResearchData.groupKey(e.board()),k->new Losses()).add(e,cp,nnue);
            brnCalibration.add(NnueCorpusTargets.wdl(Math.round(cp*100),e.sfMaterial()).target(),e.outcome());nnueCalibration.add(nnue,e.outcome());
        }
        var report=new LinkedHashMap<String,Object>();report.put("partition",test?"sealed-test":"validation");report.put("brnRun",brnRun.toString());report.put("nnueRun",nnueRun.toString());
        report.put("brnStateSha256",digest(brnRun.resolve("selected.state")));report.put("nnueModelSha256",digest(nnueRun.resolve("selected.nnue")));
        if(production)report.put("brnProductionModelSha256",digest(brnRun.resolve("production-preview/network.brn3")));
        report.put("brnMetrics",BrnResearchMain.metrics(records,e->predict.applyAsDouble(e.board()),true));
        report.put("materialMetrics",BrnResearchMain.metrics(records,e->RelationalCandidate.material(e.board()),true));
        report.put("brnTrainingOpportunity",Map.of("distinct",brnMeta.get("distinctTraining"),"epochs",brnMeta.get("epochs"),"selectedEpoch",brnMeta.get("selectedEpoch"),"seed",brnMeta.get("seed")));
        report.put("nnueTrainingOpportunity",Map.of("distinct",nnueMeta.get("distinctTraining"),"epochs",nnueMeta.get("epochs"),"selectedEpoch",nnueMeta.get("selectedEpoch"),"seed",nnueMeta.get("seed")));
        report.put("geometryGroups",groups.size());report.put("ratioBootstrap95Percent",bootstrap(groups.values()));
        var tables=new TreeMap<String,Object>();strata.forEach((k,v)->tables.put(k,v.report()));report.put("strata",tables);
        report.put("brnCalibration",brnCalibration.report());report.put("nnueCalibration",nnueCalibration.report());
        DataFiles.write(output,report);System.out.println(DataFiles.JSON.toJson(report));
    }
}
