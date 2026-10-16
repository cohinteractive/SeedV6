package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.training.model.NetworkModel;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Read-only capture of an explicitly referenced user's model; no store access/repair/lock APIs. */
public final class BrnArchitectureNativeReference {
    public static void main(String[] args)throws Exception {
        if(args.length!=2)throw new IllegalArgumentException("STORE_ROOT NEW_OUT");
        Path root=Path.of(args[0]).toAbsolutePath().normalize(),out=Path.of(args[1]).toAbsolutePath().normalize();
        if(root.startsWith(out)||out.startsWith(root)||Files.exists(out))throw new IOException("Use a separate fresh research directory");
        Path ref=root.resolve("refs/best");byte[] reference=Files.readAllBytes(ref);
        String id=CheckpointInspection.reference(root,"best");
        Path checkpoint=root.resolve("checkpoints").resolve(id);
        var manifest=CheckpointInspection.manifest(checkpoint);
        Path source=checkpoint.resolve(manifest.networkFile());
        byte[] manifestBytes=Files.readAllBytes(checkpoint.resolve(CheckpointManifest.MANIFEST_FILE));
        if(Files.size(source)!=manifest.networkBytes()||!BrnResearchComparison.digest(source).equals(manifest.networkSha256()))
            throw new IOException("Native model does not match its checksummed manifest");
        NetworkModel network;
        try(var in=new BufferedInputStream(Files.newInputStream(source))){network=NetworkModel.read(manifest.architecture(),in);}
        var lineage=TrainingLineage.read(root).orElseThrow();
        if(lineage.architecture()!=network.architecture())throw new IOException("Native lineage architecture differs");
        Files.createDirectory(out);Files.copy(source,out.resolve("selected.model"));
        if(!BrnResearchComparison.digest(out.resolve("selected.model")).equals(manifest.networkSha256())
                ||!Arrays.equals(reference,Files.readAllBytes(ref))
                ||!Arrays.equals(manifestBytes,Files.readAllBytes(checkpoint.resolve(CheckpointManifest.MANIFEST_FILE))))
            throw new IOException("Native snapshot changed during capture; retained output is incomplete");
        Files.write(out.resolve("native-best.ref"),reference,StandardOpenOption.CREATE_NEW);
        Files.write(out.resolve("native-manifest.bin"),manifestBytes,StandardOpenOption.CREATE_NEW);
        var result=new LinkedHashMap<String,Object>();result.put("kind","NATIVE");
        result.put("architecture",network.architecture());result.put("sourceRoot",root.toString());
        result.put("nativeManifest",manifest);result.put("lineageId",lineage.id().toString());result.put("lineageName",lineage.name());
        result.put("lineageCreated",lineage.created().toString());result.put("currentLineageConfiguration",lineage.configuration());
        result.put("lineageConfigurationOrigin",lineage.configurationOrigin());
        if(network instanceof NetworkModel.NnueMaterial nnue)result.put("calibratedOutcome",nnue.calibratedOutcome());
        result.put("selectedModelSha256",manifest.networkSha256());result.put("complete",true);
        result.put("scope","Read-only best-reference/manifest/model CRC+SHA identity capture, stable reference rechecked; no store lock/recovery/promotion API, no optimizer or acceptance-chain audit; native training data/compute are not matched to experimental controls");
        DataFiles.write(out.resolve("result.json"),result);System.out.println(DataFiles.JSON.toJson(result));
    }
    private BrnArchitectureNativeReference(){}
}
