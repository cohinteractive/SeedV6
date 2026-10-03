package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.brn.BrnAdamConfig;
import com.ohinteractive.seedv6.core.brn3.*;
import com.ohinteractive.seedv6.training.data.DataFiles;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Explicit one-way evidence export; normal BRN-3 training initializes in production code. */
public final class Brn3ResearchBridge {
    public static void main(String[] args)throws Exception {
        Path run=Path.of(args[0]),out=Path.of(args[1]);if(Files.exists(out))throw new IOException("Use a new output directory");
        var metadata=BrnResearchComparison.metadata(run);
        if(!"r4-relu-relative-typed-ce".equals(metadata.get("candidate")) || ((Number)metadata.get("warmupAuxiliary")).doubleValue()!=0)
            throw new IOException("Not the fixed BRN-3 research recipe");
        AbsoluteRelationCandidate research;
        try(var in=new BufferedInputStream(Files.newInputStream(run.resolve("selected.state")))){research=AbsoluteRelationCandidate.read(in);}
        if(research.WIDTH!=8||!research.typedPooling||research.singlePerspective||!research.relu||!research.relative||research.relativeScale!=1||research.denseAdam)
            throw new IOException("Incompatible research architecture");
        var trainer=Brn3Trainer.restore(research.weights,research.first,research.second,research.updates,new BrnAdamConfig(((Number)metadata.get("rate")).doubleValue()));
        Files.createDirectories(out);
        try(var stream=new BufferedOutputStream(Files.newOutputStream(out.resolve("network.brn3")))){Brn3Codec.writeModel(trainer.snapshot(),stream);}
        try(var stream=new BufferedOutputStream(Files.newOutputStream(out.resolve("training.brn3")))){Brn3Codec.writeTraining(trainer,stream);}
        var report=new LinkedHashMap<String,Object>();report.put("sourceResearchStateSha256",BrnResearchComparison.digest(run.resolve("selected.state")));
        report.put("modelSha256",BrnResearchComparison.digest(out.resolve("network.brn3")));report.put("trainingSha256",BrnResearchComparison.digest(out.resolve("training.brn3")));
        report.put("optimizerStep",trainer.step());report.put("configuration",trainer.config());report.put("normalTrainingRequiresThisExport",false);
        DataFiles.write(out.resolve("conversion.json"),report);System.out.println(DataFiles.JSON.toJson(report));
    }
    private Brn3ResearchBridge(){}
}
