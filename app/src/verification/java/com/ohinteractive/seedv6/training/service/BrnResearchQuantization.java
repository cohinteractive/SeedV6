package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.training.data.DataFiles;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Trained-checkpoint float32 budget, kept separate from training/checkpoint selection. */
public final class BrnResearchQuantization {
    public static void main(String[] args)throws Exception {
        var data=BrnResearchData.read(Path.of(args[0]),false);Path run=Path.of(args[1]),out=Path.of(args[2]);
        if(Files.exists(out))throw new IOException("Use a new output");
        AbsoluteRelationCandidate model;
        try(var in=new BufferedInputStream(Files.newInputStream(run.resolve("selected.state")))){model=AbsoluteRelationCandidate.read(in).foldedInference();}
        var compact=new AbsoluteRelationInference(model,true);var distribution=new BrnResearchDiagnostics.Distribution();
        int integerDifferences=0,maximumIntegerDifference=0;double loss=0,compactLoss=0;
        for(var example:data.validation()) {
            double a=model.predict(example.board()),b=compact.predict(example.board());distribution.add(b-a);
            int difference=Math.abs(BrnResearchDiagnostics.score(a)-BrnResearchDiagnostics.score(b));if(difference!=0)integerDifferences++;
            maximumIntegerDifference=Math.max(maximumIntegerDifference,difference);
            double da=RelationalCandidate.outcome(a,example.sfMaterial())[0]-example.outcome();
            double db=RelationalCandidate.outcome(b,example.sfMaterial())[0]-example.outcome();loss+=.5*da*da;compactLoss+=.5*db*db;
        }
        var report=new LinkedHashMap<String,Object>();report.put("stateSha256",BrnResearchComparison.digest(run.resolve("selected.state")));
        report.put("dataset",DataFiles.read(Path.of(args[0]).resolve("manifest.json"),Map.class));report.put("absoluteErrorPawns",distribution.report());
        report.put("integerScoreDifferences",integerDifferences);report.put("maximumIntegerScoreDifference",maximumIntegerDifference);
        report.put("smoothOutcomeLossDouble",loss/data.validation().size());report.put("smoothOutcomeLossFloat32",compactLoss/data.validation().size());
        report.put("declaredMaximumErrorPawns",1e-4);report.put("withinBudget",(double)distribution.report().get("max")<=1e-4);
        DataFiles.write(out,report);System.out.println(DataFiles.JSON.toJson(report));
    }
    private BrnResearchQuantization(){}
}
