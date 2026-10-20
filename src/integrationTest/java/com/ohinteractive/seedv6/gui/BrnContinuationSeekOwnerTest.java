package com.ohinteractive.seedv6.gui;

import com.github.luben.zstd.ZstdInputStream;
import com.google.gson.*;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.service.*;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Explicit local-owner, read-only diagnostic. Navigation writes go only to temporary copies. */
@Timeout(120)
class BrnContinuationSeekOwnerTest {
    @TempDir Path temporary;
    @Test void ownerCheckpointsResumeAndRealSourceBoundaries() throws Exception {
        String output=System.getenv("SEED_W4_SEEK_AUDIT_REPORT");Assumptions.assumeTrue(output!=null);
        Path report=Path.of(output);assertFalse(Files.exists(report));
        Path plan=Path.of("../docs/research/brn-successor-cglhw/raw/wave4/owner-preparation-01.json");
        var entries=DataFiles.read(plan,BrnContinuationWave.Report.class).entries();
        var results=new ArrayList<Map<String,Object>>();
        for(var entry:entries) {
            Path root=Path.of(entry.root()),sourceRoot=Path.of(entry.origin().source().root());
            var result=new LinkedHashMap<String,Object>();result.put("candidate",entry.candidate());result.put("root",root.toString());
            String latest=CheckpointInspection.reference(root,"latest-training"),best=CheckpointInspection.reference(root,"best");
            assertEquals(entry.origin().bootstrap(),latest);assertEquals(latest,best);
            var manifest=CheckpointStore.readSnapshot(root,latest).manifest();
            assertEquals(32,manifest.generation());assertEquals(entry.origin().inheritedStep(),manifest.optimizerStep());
            assertEquals(entry.origin().source().networkSha256(),manifest.networkSha256());
            assertEquals(entry.origin().source().trainingSha256(),manifest.trainingSha256());
            assertEquals(manifest.trainingSha256(),BrnContinuationWave.hash(CheckpointStore.readTrainingSnapshot(root,latest).encode()));
            result.put("checkpoint",manifest);result.put("best",best);
            assertTrue(PartialGeneration.inspect(root).isEmpty());
            var ledger=new SourceLedger(root);assertEquals(32,ledger.state().reservations().size());assertTrue(ledger.active().isEmpty());
            assertEquals(entry.sourceOffsets(),ledger.state().next());
            var selected=TrainingLineages.read(new TrainingLineages.Entry(root,NetworkArchitecture.valueOf(entry.architecture()),entry.candidate()));
            assertEquals(entry.recipe(),selected.settings().recipe());assertEquals(32,selected.settings().maximumGenerations());
            var attempt=GenerationAttempt.inspect(root);var session=TrainingSession.read(root);
            if(entry.candidate().equals("W4-BRE-CONTROL-LR001")) {
                assertEquals(33,attempt.orElseThrow().generation());assertEquals(latest,attempt.orElseThrow().parentId());
                assertEquals(GenerationAttempt.Format.INCREMENTAL,attempt.orElseThrow().format());
                assertEquals(TrainingSession.State.ACTIVE,session.orElseThrow().state());assertEquals(64,session.orElseThrow().finalGeneration());
                assertEquals(0,session.orElseThrow().completed());
            } else {assertTrue(attempt.isEmpty());assertTrue(session.isEmpty());}
            String preview=new TrainingController.Backend().preview(selected.settings());
            assertEquals(attempt.isPresent()?"Resume Generation 33":"Start Training",preview);
            result.put("preview",preview);result.put("attempt",attempt.orElse(null));result.put("session",session.orElse(null));
            result.put("partial",false);result.put("recipe",selected.settings().recipe());
            var selection=DataSources.read(DataSources.directory(root));selection.verify();
            Path scratch=temporary.resolve(entry.candidate());
            var probes=new ArrayList<Map<String,Object>>();
            for(var source:selection.sources()) {
                long start=ledger.next(source.identity());var probe=new LinkedHashMap<String,Object>();
                probe.put("identity",source.identity());probe.put("start",start);source.requireReady();
                if(source.format()==DataSource.Format.LICHESS_PZSTD) {
                    Path name=Path.of(source.identity()+".json"),ownerIndex=DataSources.directory(root).resolve("seek").resolve(name);
                    boolean missing=Files.notExists(ownerIndex);probe.put("ownerIndexMissing",missing);
                    if(missing) {
                        var failure=assertThrows(IOException.class,()->SourceReaders.open(source,DataSources.directory(root).resolve("seek"),start));
                        assertTrue(failure.getMessage().contains("does not cover position "+start));probe.put("reproducedException",failure.getMessage());
                    }
                    Path donor=DataSources.directory(sourceRoot).resolve("seek").resolve(name);
                    Files.createDirectories(scratch);Files.copy(missing?donor:ownerIndex,scratch.resolve(name));
                    var index=DataFiles.read(donor,SourceReaders.Index.class);
                    var floor=index.points().stream().filter(p->p.record()<=start).reduce((a,b)->b).orElseThrow();
                    var anchor=index.points().stream().filter(p->p.record()<floor.record()).reduce((a,b)->b).orElseThrow();
                    probe.put("floor",floor);probe.put("oracleAnchor",anchor);
                    long boundary=(start/4096+1)*4096;
                    var wanted=new TreeSet<>(List.of(floor.record()-1,floor.record(),floor.record()+1,start-1,start,start+1,boundary,boundary+1));
                    var samples=new TreeMap<Long,PositionReader.Entry>();
                    try(var reader=SourceReaders.open(source,scratch,wanted.first())) {
                        assertTrue(reader.seekRecords()<=4096);probe.put("seekRecords",reader.seekRecords());
                        for(long n=wanted.first();n<=wanted.last();n++) {
                            var value=reader.next();assertNotNull(value);assertEquals(n,value.ordinal());
                            if(wanted.contains(n))samples.put(n,value);
                        }
                    }
                    // Independently frame-decompress from an earlier anchor; no SourceReaders line/seek implementation.
                    var raw=oracle(source.path(),anchor,wanted);
                    for(var sample:samples.entrySet()) {
                        var value=sample.getValue().position();assertNotNull(value,sample.getValue().rejection());
                        var json=JsonParser.parseString(raw.get(sample.getKey())).getAsJsonObject();
                        String[] fen=json.get("fen").getAsString().split(" ");
                        assertEquals(String.join(" ",Arrays.copyOf(fen,Math.min(5,fen.length))),value.position().fen());
                        JsonObject evaluation=null;
                        for(var element:json.getAsJsonArray("evals")) {
                            var next=element.getAsJsonObject();
                            if(evaluation==null||quality(next,evaluation)>0)evaluation=next;
                        }
                        var pv=evaluation.getAsJsonArray("pvs").get(0).getAsJsonObject();
                        assertEquals(pv.get(pv.has("cp")?"cp":"mate").getAsInt(),value.target());
                        assertEquals(pv.has("cp")?com.ohinteractive.seedv6.corpus.CorpusRecord.CP:com.ohinteractive.seedv6.corpus.CorpusRecord.MATE,value.targetKind());
                        assertEquals(com.ohinteractive.seedv6.corpus.CorpusRecord.WHITE,value.perspective());
                    }
                    try(var reader=SourceReaders.open(source,scratch,boundary+1)) {
                        assertEquals(samples.get(boundary+1),reader.next());assertTrue(reader.seekRecords()<=4096);
                    }
                    probe.put("samples",samples);probe.put("independentFrameOracle",true);
                    probe.put("indexSha256",BrnContinuationWave.hash(Files.readAllBytes(donor)));
                } else {
                    try(var reader=SourceReaders.open(source,scratch,start)) {
                        var samples=new ArrayList<PositionReader.Entry>();
                        for(int i=0;i<3;i++){var value=reader.next();assertNotNull(value);assertNotNull(value.position());assertEquals(start+i,value.ordinal());samples.add(value);}
                        probe.put("samples",samples);probe.put("seekRecords",reader.seekRecords());
                    }
                }
                probes.add(probe);
            }
            result.put("probes",probes);results.add(result);
        }
        DataFiles.write(report,Map.of("recorded",java.time.Instant.now().toString(),"entries",results,"trainingPerformed",false));
    }
    private static int quality(JsonObject a,JsonObject b) {
        int depth=Integer.compare(a.get("depth").getAsInt(),b.get("depth").getAsInt());
        return depth!=0?depth:Long.compare(a.has("knodes")?a.get("knodes").getAsLong():-1,b.has("knodes")?b.get("knodes").getAsLong():-1);
    }
    private static Map<Long,String> oracle(Path path,SourceReaders.Point anchor,Set<Long> wanted) throws Exception {
        var decoded=new ByteArrayOutputStream();
        try(var file=new RandomAccessFile(path.toFile(),"r")) {
            file.seek(anchor.offset());
            // Two adjacent frames at most; each is bounded by 128 MiB decoded/16 MiB compressed.
            for(int i=0;i<2;i++) {
                byte[] header=new byte[12];file.readFully(header);var h=ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
                assertEquals(0x184d2a50,h.getInt());assertEquals(4,h.getInt());int length=h.getInt();assertTrue(length>0&&length<=16*1024*1024);
                byte[] bytes=new byte[length];file.readFully(bytes);
                try(var zstd=new ZstdInputStream(new ByteArrayInputStream(bytes))) {
                    byte[] plain=zstd.readNBytes(128*1024*1024+1);assertTrue(plain.length<=128*1024*1024);decoded.write(plain);
                }
            }
        }
        var result=new TreeMap<Long,String>();
        var bytes=new ByteArrayInputStream(decoded.toByteArray());bytes.skipNBytes(anchor.within());
        try(var lines=new BufferedReader(new InputStreamReader(bytes,java.nio.charset.StandardCharsets.UTF_8))) {
            for(long n=anchor.record();n<=Collections.max(wanted);n++){String line=lines.readLine();assertNotNull(line);if(wanted.contains(n))result.put(n,line);}
        }
        return result;
    }
}
