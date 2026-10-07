package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.brn3.*;
import com.ohinteractive.seedv6.training.data.DataFiles;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** C02 derives an inference-only view of existing order2 model bytes after parity. */
public final class BrnTupleCompilation {
    static BrnArchitectureControls.View view(BrnTupleCompiled model,double gain) {
        BrnArchitectureControls.requireGain(gain);var work=model.newWorkspace();
        return new BrnArchitectureControls.View(b->Brn3Objective.outcome(work.evaluatePawns(b),b),work::evaluatePawns,
                ()->{var cache=model.newWorkspace();return (b,ply)->Brn3Model.score(cache.evaluatePawns(b,gain));});
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] arguments)throws Exception {
        if(arguments.length!=3)throw new IllegalArgumentException("DATA ORIGINAL_TUPLE_RUN NEW_OUT");
        Path data=Path.of(arguments[0]),parent=Path.of(arguments[1]),out=Path.of(arguments[2]);
        if(Files.exists(out))throw new IOException("Use a new compilation directory");
        String metadataHash=BrnResearchComparison.digest(parent.resolve("result.json"));
        var metadata=DataFiles.read(parent.resolve("result.json"),Map.class);
        if(!"TUPLE".equals(metadata.get("kind"))||!Boolean.TRUE.equals(metadata.get("complete")))
            throw new IOException("Original complete tuple model required");
        String modelHash=BrnResearchComparison.digest(parent.resolve("selected.model"));
        if(!modelHash.equals(metadata.get("selectedModelSha256")))throw new IOException("Original model hash mismatch");
        BrnTupleTrainer.Model original;
        try(var in=new BufferedInputStream(Files.newInputStream(parent.resolve("selected.model")))){original=BrnTupleTrainer.Model.read(in);}
        long start=System.nanoTime();var compiled=new BrnTupleCompiled(original);double compilationSeconds=(System.nanoTime()-start)/1e9;
        Files.createDirectory(out);var report=new LinkedHashMap<String,Object>();
        report.put("schema","brn-architecture-tuple-compilation-v1");report.put("kind","TUPLE_COMPILED");
        report.put("arguments",arguments);report.put("sourceRun",parent.toString());report.put("sourceMetadataSha256",metadataHash);
        report.put("sourceModelSha256",modelHash);report.put("complete",false);
        report.put("compilationSeconds",compilationSeconds);report.put("uniquePhysicalPairs",compiled.pairs());
        report.put("immutablePrimitiveBytes",compiled.immutablePrimitiveBytes());
        report.put("workspacePrimitiveBytes",compiled.newWorkspace().primitiveBytes());
        report.put("memoryScope","Primitive model/cache arrays and scoring doubles; excludes JVM object headers, reference arrays, counters and temporary compiler/source allocations");
        report.put("storage","Original tuple model bytes retained unchanged; runtime compilation into double tables. No optimizer state is created or claimed.");
        report.put("parityScope","All validation boards, labels opaque; original and compiled rolling workspaces; five fixed gains; independent forward and legal/special transition tests are separate");
        DataFiles.write(out.resolve("result.json"),report);
        double[] gains={0,.125,.25,.5,1};double[] maximumError={0};int[] differences=new int[gains.length],maximumIntegerDifference=new int[gains.length],count={0};
        var reference=original.newWorkspace();var candidate=compiled.newWorkspace();
        var audit=BrnArchitectureDataAudit.scan(data,(board,partition,index)->{
            if(partition!=1)return;
            long[] before=board.clone();
            for(int i=0;i<gains.length;i++) {
                double expected=reference.evaluatePawns(board,gains[i]),actual=candidate.evaluatePawns(board,gains[i]);
                maximumError[0]=Math.max(maximumError[0],Math.abs(expected-actual));
                int delta=Math.abs(Brn3Model.score(expected)-Brn3Model.score(actual));
                if(delta!=0)differences[i]++;maximumIntegerDifference[i]=Math.max(maximumIntegerDifference[i],delta);
            }
            if(!Arrays.equals(before,board))throw new AssertionError("Input modified");count[0]++;
        });
        report.put("datasetAudit",audit);report.put("validationBoards",count[0]);report.put("gains",gains);
        report.put("maximumPawnDifference",maximumError[0]);report.put("integerDifferencesByGain",differences);
        report.put("maximumIntegerDifferenceByGain",maximumIntegerDifference);
        boolean parity=count[0]>0&&count[0]==((int[])audit.get("counts"))[1]&&maximumError[0]<=1e-8&&Arrays.stream(differences).sum()==0;
        report.put("parityPassed",parity);DataFiles.write(out.resolve("result.json"),report);
        if(!parity)throw new AssertionError("Compilation parity gate failed; evidence retained");
        if(!metadataHash.equals(BrnResearchComparison.digest(parent.resolve("result.json")))
                ||!modelHash.equals(BrnResearchComparison.digest(parent.resolve("selected.model"))))
            throw new IOException("Original evidence changed during compilation audit");
        Files.copy(parent.resolve("selected.model"),out.resolve("selected.model"));
        if(!modelHash.equals(BrnResearchComparison.digest(out.resolve("selected.model"))))throw new IOException("Model copy hash mismatch");
        report.put("selectedModelSha256",modelHash);report.put("complete",true);DataFiles.write(out.resolve("result.json"),report);
        System.out.println(DataFiles.JSON.toJson(report));
    }
    private BrnTupleCompilation(){}
}
