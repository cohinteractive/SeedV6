package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.conventional.ConventionalNetwork;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.function.ToIntFunction;

/** Streaming teacher labels, with atomic chunk directories and immutable input binding. */
public final class TeacherDataset {
    @FunctionalInterface interface Permission { void check()throws IOException; }
    static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    static void publish(Path source,Path target,Path root)throws IOException {
        Path boundary=root.toAbsolutePath().normalize();
        if(!source.toAbsolutePath().normalize().startsWith(boundary)||!target.toAbsolutePath().normalize().startsWith(boundary))throw new IOException("Publication outside dataset");
        if(Files.exists(target))throw new IOException("Preserve existing publication");
        Files.move(source,target,StandardCopyOption.ATOMIC_MOVE);
    }
    static Map<String,Object> process(Path source,Path out,int rows,int chunkSize,ToIntFunction<long[]> evaluator,Permission permission)throws Exception {
        Files.createDirectories(out);var chunks=new ArrayList<Path>();int index=0;
        try(var input=Files.newBufferedReader(source)) {
            for(int start=0;start<rows;start+=chunkSize,index++) {
                permission.check();int count=Math.min(chunkSize,rows-start);Path target=out.resolve(String.format("chunk-%05d",index));
                if(Files.exists(target)) {
                    var receipt=JsonParser.parseString(Files.readString(target.resolve("receipt.json"))).getAsJsonObject();
                    if(receipt.get("start").getAsInt()!=start||receipt.get("rows").getAsInt()!=count||!PairObjectiveDiagnostic.sha(target.resolve("data.tsv")).equals(receipt.get("sha256").getAsString()))throw new IOException("Teacher chunk identity");
                    for(int i=0;i<count;i++)if(input.readLine()==null)throw new EOFException("Source ended during resume");
                } else {
                    Path attempt=out.resolve(".attempt-"+index+"-"+System.nanoTime());Files.createDirectory(attempt);
                    try(var output=Files.newBufferedWriter(attempt.resolve("data.tsv"),StandardOpenOption.CREATE_NEW)) {
                        for(int i=0;i<count;i++) {
                            String line=input.readLine();if(line==null)throw new EOFException("Source ended during relabel");
                            int tab=line.indexOf('\t');if(tab<1)throw new IOException("Source row format");String fen=line.substring(0,tab);
                            double value=PairObjectiveDiagnostic.sigmoid(evaluator.applyAsInt(Board.fromFen(fen))/400.0);
                            if(!Double.isFinite(value)||value<0||value>1)throw new IOException("Teacher probability");
                            output.write(fen+"\t"+Double.toString(value)+"\n");
                        }
                    }
                    var receipt=Map.of("start",start,"rows",count,"sha256",PairObjectiveDiagnostic.sha(attempt.resolve("data.tsv")));
                    Files.writeString(attempt.resolve("receipt.json"),GSON.toJson(receipt)+"\n",StandardOpenOption.CREATE_NEW);
                    publish(attempt,target,out);
                }
                chunks.add(target.resolve("data.tsv"));
            }
            if(input.readLine()!=null)throw new IOException("Source has extra rows");
        }
        permission.check();Path finalData=out.resolve("data.tsv"),temporary=out.resolve(".concatenation-"+System.nanoTime()+".part");
        boolean existing=Files.exists(finalData);var digest=MessageDigest.getInstance("SHA-256");
        try(var output=new DigestOutputStream(existing?OutputStream.nullOutputStream():Files.newOutputStream(temporary,StandardOpenOption.CREATE_NEW),digest)) {
            for(Path chunk:chunks)try(var input=Files.newInputStream(chunk)){input.transferTo(output);}
        }
        String hash=HexFormat.of().formatHex(digest.digest());
        if(existing){if(!PairObjectiveDiagnostic.sha(finalData).equals(hash))throw new IOException("Teacher concatenation changed");}
        else publish(temporary,finalData,out);
        return Map.of("rows",rows,"chunks",chunks.size(),"data",finalData.toString(),"sha256",hash);
    }
    public static void main(String[] args)throws Exception {
        PairObjectiveDiagnostic.permission();Path configPath=Path.of(args[0]);var config=JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();
        Path out=Path.of(config.get("output").getAsString());Files.createDirectories(out);Path binding=out.resolve("config.sha256");String configSha=PairObjectiveDiagnostic.sha(configPath);
        if(Files.exists(binding)){if(!Files.readString(binding).equals(configSha))throw new IOException("Teacher config changed");}
        else Files.writeString(binding,configSha,StandardOpenOption.CREATE_NEW);
        Path model=Path.of(config.get("teacher").getAsString());
        if(!PairObjectiveDiagnostic.sha(model).equals(config.get("teacher_sha256").getAsString()))throw new IOException("Teacher identity");
        var network=ConventionalNetwork.load(model);var datasets=new LinkedHashMap<String,Object>();long began=System.nanoTime();
        for(String name:List.of("train","validation")) {
            var spec=config.getAsJsonObject(name);Path source=Path.of(spec.get("path").getAsString());
            if(!PairObjectiveDiagnostic.sha(source).equals(spec.get("sha256").getAsString()))throw new IOException("Source identity");
            datasets.put(name,process(source,out.resolve(name),spec.get("rows").getAsInt(),65536,network::fullRebuild,PairObjectiveDiagnostic::permission));
        }
        Path result=out.resolve("result.json");
        if(!Files.exists(result))Files.writeString(result,GSON.toJson(Map.of("config_sha256",configSha,"teacher_sha256",config.get("teacher_sha256").getAsString(),"datasets",datasets,"seconds",(System.nanoTime()-began)/1e9))+"\n",StandardOpenOption.CREATE_NEW);
        else {
            var prior=JsonParser.parseString(Files.readString(result)).getAsJsonObject();
            if(!prior.get("config_sha256").getAsString().equals(configSha)||!prior.get("datasets").equals(GSON.toJsonTree(datasets)))throw new IOException("Completed teacher manifest changed");
        }
        System.out.println(Files.readString(result));
    }
    private TeacherDataset(){}
}
