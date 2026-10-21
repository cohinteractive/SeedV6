package com.ohinteractive.seedv6.tools.eval;

import com.ohinteractive.seedv6.core.util.Fen;
import com.ohinteractive.seedv6.training.data.*;
import java.nio.file.*;
import java.util.*;

/** Bounded raw-source health sample. No acquisition, reservation, training or owner writes. */
public final class BrnTrainingDataHealth {
    record Source(String name,String selection,String identity,String seek,long start) {}
    record Plan(String id,int records,List<Source> sources) {}
    public static void main(String[] args) throws Exception {
        if(args.length!=2)throw new IllegalArgumentException("PLAN NEW_OUTPUT");
        var plan=DataFiles.read(Path.of(args[0]),Plan.class);
        if(plan.records()<1||plan.records()>16384||plan.sources().isEmpty()||plan.sources().size()>2)
            throw new IllegalArgumentException("Bounded1..16384 records,1..2 sources required");
        Path output=Path.of(args[1]);Files.createDirectory(output);
        var summaries=new ArrayList<Object>();long deadline=System.nanoTime()+120_000_000_000L;
        for(var request:plan.sources()) {
            if(!request.name().matches("[a-zA-Z0-9-]+")||request.start()<0)throw new IllegalArgumentException("Invalid source binding");
            var selection=DataFiles.read(Path.of(request.selection()),DataSources.class);selection.verify();
            var source=selection.sources().stream().filter(s->s.identity().equals(request.identity())).findFirst().orElseThrow();
            source.requireReady();Path seek=output.resolve(request.name()+"-seek");Files.createDirectory(seek);
            Path index=Path.of(request.seek()).resolve(source.identity()+".json");
            if(Files.exists(index))Files.copy(index,seek.resolve(index.getFileName()));
            int retained=0,rejected=0;long end=request.start();
            try(var reader=SourceReaders.open(source,seek,request.start());var rows=Files.newBufferedWriter(output.resolve(request.name()+".jsonl"))) {
                for(int n=0;n<plan.records();n++) {
                    if(System.nanoTime()>deadline)throw new IllegalStateException("Health probe deadline");
                    var entry=reader.next();if(entry==null)throw new IllegalStateException("Source exhausted");end=reader.nextPosition();
                    if(entry.position()==null){rejected++;continue;}
                    var p=entry.position();
                    rows.write(DataFiles.JSON.toJson(Map.of("ordinal",entry.ordinal(),"fen",Fen.fromBoard(p.position().toBoard(0)),
                        "partition",BrnNextPartition.part(p.position()),"targetKind",p.targetKind(),"target",p.target(),"perspective",p.perspective())).replace("\n",""));rows.newLine();retained++;
                }
                summaries.add(Map.of("name",request.name(),"source",source,"start",request.start(),"end",end,
                    "retained",retained,"decoderRejected",rejected,"decodedRecords",reader.decodedRecords(),"seekRecords",reader.seekRecords()));
            }
        }
        DataFiles.write(output.resolve("summary.json"),Map.of("plan",plan.id(),"sources",summaries));
        System.out.println("Completed bounded source health sample, no owner writes: "+output);
    }
    private BrnTrainingDataHealth() {}
}
