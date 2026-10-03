package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.training.nnue.*;
import com.ohinteractive.seedv6.training.data.DataFiles;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Explicit sealed-test comparison of the two prospectively fixed BRN-3 replications. */
public final class Brn3FinalComparison {
    public static void main(String[] args)throws Exception {
        if(args.length!=8||!args[7].equals("sealed-test"))throw new IllegalArgumentException("DATA1 BRN1 NNUE1 DATA2 BRN2 NNUE2 NEW_OUT sealed-test");
        Path output=Path.of(args[6]);if(Files.exists(output))throw new IOException("Use a new final report");
        var pooled=new BrnResearchComparison.Losses();var groups=new TreeMap<String,BrnResearchComparison.Losses>();
        var runs=new ArrayList<Map<String,Object>>();double priorCpError=0,initialNnueLoss=0;
        for(int run=0;run<2;run++) {
            Path directory=Path.of(args[3*run]),brnRun=Path.of(args[3*run+1]),nnueRun=Path.of(args[3*run+2]);
            var bm=BrnResearchComparison.metadata(brnRun);var nm=BrnResearchComparison.metadata(nnueRun);
            var manifest=DataFiles.read(directory.resolve("manifest.json"),Map.class);
            for(var metadata:List.of(bm,nm)) {
                if(!Objects.equals(((Map<?,?>)metadata.get("datasetManifest")).get("payloadSha256"),manifest.get("payloadSha256")))throw new IOException("Dataset mismatch");
                if(((Number)metadata.get("distinctTraining")).intValue()<131072 || ((List<?>)metadata.get("events")).size()!=((Number)metadata.get("epochs")).intValue())throw new IOException("Incomplete training opportunity");
            }
            if(!Objects.equals(bm.get("distinctTraining"),nm.get("distinctTraining"))||!Objects.equals(bm.get("epochs"),nm.get("epochs"))||!Objects.equals(bm.get("seed"),nm.get("seed")))throw new IOException("Unmatched exposure or seed");
            var model=BrnResearchComparison.productionModel(brnRun);var brn=model.newWorkspace();NnueNetwork network;
            try(var input=new BufferedInputStream(Files.newInputStream(nnueRun.resolve("selected.nnue")))){network=NnueNetworkCodec.read(input);}
            var nnue=new NnueEvaluator(network);
            var initialNnue=new NnueEvaluator(TrainableNnue.initialized(((Number)nm.get("seed")).longValue()).snapshot());
            var records=BrnResearchData.read(directory,true).test();if(records.size()<16384)throw new IOException("Test population too small");
            var strata=new TreeMap<String,BrnResearchComparison.Losses>();var brnCal=new BrnResearchComparison.Calibration();var nnueCal=new BrnResearchComparison.Calibration();
            var brnErrors=new BrnResearchDiagnostics.Distribution();var nnueErrors=new BrnResearchDiagnostics.Distribution();double runInitialNnueLoss=0;
            for(var e:records) {
                double cp=brn.evaluatePawns(e.board());nnue.evaluate(e.board());double value=nnue.boundedValue();
                pooled.add(e,cp,value);groups.computeIfAbsent(BrnResearchData.groupKey(e.board()),k->new BrnResearchComparison.Losses()).add(e,cp,value);
                double material=Math.abs(RelationalCandidate.material(e.board()));
                String materialBin=material<.5?"material-under-.5":material<2?"material-.5-to-2":material<5?"material-2-to-5":"material-5-plus";
                String phase=e.sfMaterial()<=20?"material-total-up-to-20":e.sfMaterial()<=50?"material-total-21-to-50":"material-total-over-50";
                for(String key:List.of("all",Math.abs(e.cp())<=200?"balanced":"unbalanced",materialBin,phase))strata.computeIfAbsent(key,k->new BrnResearchComparison.Losses()).add(e,cp,value);
                double brnOutcome=NnueCorpusTargets.wdl(Math.round(cp*100),e.sfMaterial()).target();brnCal.add(brnOutcome,e.outcome());nnueCal.add(value,e.outcome());
                brnErrors.add(brnOutcome-e.outcome());nnueErrors.add(value-e.outcome());
                priorCpError+=Math.abs(RelationalCandidate.material(e.board())-e.cp()/100.0);
                initialNnue.evaluate(e.board());double initialError=initialNnue.boundedValue()-e.outcome();runInitialNnueLoss+=.5*initialError*initialError;
            }
            initialNnueLoss+=runInitialNnueLoss;
            var row=new LinkedHashMap<String,Object>();row.put("seed",bm.get("seed"));row.put("brnRun",brnRun.toString());row.put("nnueRun",nnueRun.toString());
            row.put("dataset",manifest);row.put("brnModelSha256",BrnResearchComparison.digest(brnRun.resolve("production-preview/network.brn3")));row.put("nnueModelSha256",BrnResearchComparison.digest(nnueRun.resolve("selected.nnue")));
            row.put("distinctTraining",bm.get("distinctTraining"));row.put("epochOpportunity",bm.get("epochs"));row.put("selectedBrnEpoch",bm.get("selectedEpoch"));row.put("selectedNnueEpoch",nm.get("selectedEpoch"));
            var tables=new TreeMap<String,Object>();strata.forEach((k,v)->tables.put(k,v.report()));row.put("strata",tables);
            row.put("brnMetrics",BrnResearchMain.metrics(records,e->brn.evaluatePawns(e.board()),true));row.put("materialMetrics",BrnResearchMain.metrics(records,e->RelationalCandidate.material(e.board()),true));
            row.put("brnCalibration",brnCal.report());row.put("nnueCalibration",nnueCal.report());row.put("brnOutcomeErrors",brnErrors.report());row.put("nnueOutcomeErrors",nnueErrors.report());
            row.put("initialNnueTestHalfMse",runInitialNnueLoss/records.size());runs.add(row);
        }
        var report=new LinkedHashMap<String,Object>();report.put("partition","sealed-test, first evaluation after recipe freeze");report.put("runs",runs);
        report.put("pooled",pooled.report());report.put("geometryGroups",groups.size());double[] interval=BrnResearchComparison.bootstrap(groups.values());report.put("ratioBootstrap95Percent",interval);
        report.put("materialCpMaePawns",priorCpError/pooled.count);report.put("initialNnueTestHalfMse",initialNnueLoss/pooled.count);
        report.put("qualityGatePassed",pooled.brn/pooled.nnue<=1.10&&interval[1]<=1.20&&pooled.brn<=.95*pooled.material&&pooled.absoluteCpError<priorCpError&&pooled.nnue<initialNnueLoss);
        report.put("uncertaintyScope","2000 paired geometry-group bootstrap replicates, seed8017; shared groups clustered across both runs; two seeds and source ranges do not establish game-disjoint or population-wide independence");
        DataFiles.write(output,report);System.out.println(DataFiles.JSON.toJson(report));
    }
    private Brn3FinalComparison(){}
}
