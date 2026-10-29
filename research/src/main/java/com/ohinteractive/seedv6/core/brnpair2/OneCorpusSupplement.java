package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import com.github.luben.zstd.Zstd;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.corpus.CorpusPosition;
import com.ohinteractive.seedv6.training.data.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Same BT4 source/filter/target methodology for reserved-row replacements and fresh held-out data. */
public final class OneCorpusSupplement {
    static int link(int encoded) {
        double p=Math.max(.0001,Math.min(.9999,(1+Bt4Targets.q(encoded))*.5));
        return (int)Math.round(400*StrictMath.log(p/(1-p)));
    }
    static byte[] pack(CorpusPosition p,int link,int result) {
        int stm=p.rules()&1,idx=0;long occ=0;byte[] bytes=new byte[32];
        for(int square=0;square<64;square++){
            int original=square^(stm*56),piece=Board.getSquare(p.plane0(),p.plane1(),p.plane2(),p.plane3(),original);
            if(piece==0)continue;int code=6-(piece&7)|((piece>>>3)^stm)<<3;
            if(idx==32)throw new IllegalArgumentException("Piece count");
            bytes[8+idx/2]|=(byte)(code<<(4*(idx&1)));idx++;occ|=1L<<square;
            if(code==5)bytes[27]=(byte)square;if(code==13)bytes[28]=(byte)(square^56);
        }
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putLong(occ).putShort(24,(short)link);bytes[26]=(byte)(result+1);return bytes;
    }
    static boolean contains(ByteBuffer data,int start,int count,long[] key) {
        int lo=0,hi=count;
        while(lo<hi){int mid=(lo+hi)>>>1,offset=start+24*mid;
            int c=OneCorpusAudit.compare(data.getLong(offset),data.getLong(offset+8),data.getLong(offset+16),key[0],key[1],key[2]);
            if(c<0)lo=mid+1;else hi=mid;
        }
        int offset=start+24*lo;
        return lo<count&&data.getLong(offset)==key[0]&&data.getLong(offset+8)==key[1]&&data.getLong(offset+16)==key[2];
    }
    record Key(long a,long b,long c) {Key(long[] k){this(k[0],k[1],k[2]);}}
    public static void main(String[] args)throws Exception {
        PairObjectiveDiagnostic.permission();Path planPath=Path.of(args[0]);var plan=JsonParser.parseString(Files.readString(planPath)).getAsJsonObject();
        Path originalPath=Path.of(plan.get("original_manifest").getAsString()),mergePath=Path.of(plan.get("merge_result").getAsString());
        var original=JsonParser.parseString(Files.readString(originalPath)).getAsJsonObject();var merge=JsonParser.parseString(Files.readString(mergePath)).getAsJsonObject();
        int trainTarget=plan.get("training_replacements").getAsInt(),heldTarget=plan.get("fresh_validation").getAsInt();long seed=plan.get("seed").getAsLong();
        if(trainTarget!=merge.get("excluded_samples").getAsInt()||heldTarget!=8192||trainTarget>131072)throw new IllegalArgumentException("Allocation");
        var source=new Gson().fromJson(original.getAsJsonObject("source"),DataSource.class);source.verify();
        var manifest=PreparedBinpack.ready(source);if(!manifest.identity().equals(original.get("prepared_identity").getAsString()))throw new IOException("Prepared source identity");
        Path prepared=PreparedBinpack.directory(source),unique=mergePath.resolveSibling("unique.keys"),reserved=Path.of(plan.get("reserved_keys").getAsString());
        if(!PairCoverageDiagnostic.sha(unique).equals(merge.get("unique_keys_sha256").getAsString())||!PairCoverageDiagnostic.sha(reserved).equals(merge.get("reserved_sha256").getAsString()))throw new IOException("Exclusion identity");
        var used=new HashSet<Integer>();for(var entry:original.getAsJsonArray("units"))used.add(entry.getAsJsonObject().get("index").getAsInt());
        var indices=new ArrayList<Integer>();for(int i=0;i<manifest.units().size();i++)if(!used.contains(i))indices.add(i);Collections.shuffle(indices,new Random(seed));
        Path out=Path.of(plan.get("output").getAsString());Files.createDirectory(out);long started=System.nanoTime();
        var counters=new TreeMap<String,Long>();var evidence=new ArrayList<Object>();var seen=new HashSet<Key>();
        int training=0,held=0,next=0;long[] key=new long[3];int[] squares=new int[64];
        try(var uc=FileChannel.open(unique);var rc=FileChannel.open(reserved);
            var trainOut=new BufferedOutputStream(Files.newOutputStream(out.resolve("topup.data"),StandardOpenOption.CREATE_NEW));
            var heldOut=new BufferedOutputStream(Files.newOutputStream(out.resolve("fresh-validation.data"),StandardOpenOption.CREATE_NEW));
            var proof=Files.newBufferedWriter(out.resolve("proof.txt"),StandardOpenOption.CREATE_NEW);
            var labels=Files.newBufferedWriter(out.resolve("labels.jsonl"),StandardOpenOption.CREATE_NEW)){
            var all=uc.map(FileChannel.MapMode.READ_ONLY,0,uc.size());var reservations=rc.map(FileChannel.MapMode.READ_ONLY,0,rc.size());
            int uniqueCount=(int)(uc.size()/24),reservedCount=reservations.getInt(8);
            while(training<trainTarget||held<heldTarget){
                PairObjectiveDiagnostic.permission();if(next>=64)throw new IllegalStateException("Bounded supplementary unit allocation exhausted");
                boolean validation=held<heldTarget;int target=Math.min(4096,validation?heldTarget-held:trainTarget-training);
                int index=indices.get(next++),read=0,accepted=0;var unit=manifest.units().get(index);
                byte[] compressed=new byte[unit.compressed()];try(var channel=FileChannel.open(prepared.resolve("shard-"+unit.shard()+".zst"))){
                    channel.position(unit.offset());var buffer=ByteBuffer.wrap(compressed);while(buffer.hasRemaining())if(channel.read(buffer)<0)throw new EOFException();
                }
                byte[] decoded=Zstd.decompress(compressed,unit.decoded());
                if(!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(decoded)).equals(unit.hash()))throw new IOException("Prepared unit SHA256");
                var decoder=new BinpackDecoder(BinpackDecoder.readChunk(new ByteArrayInputStream(decoded)));
                for(BinpackDecoder.Record row;(row=decoder.next())!=null&&accepted<target;){
                    if((read++&4095)==0)PairObjectiveDiagnostic.permission();counters.merge("decoded",1L,Long::sum);
                    String reason=null;var p=row.position();int stm=p.rules()&1;
                    if(row.target()==32002)reason="sentinel";
                    else if(Math.abs(row.target())>32000)reason="invalid_target";
                    else if(row.ply()<16)reason="early_ply";
                    else if(Board.isPlayerInCheck(p.plane0(),p.plane1(),p.plane2(),p.plane3(),stm))reason="check";
                    else if(row.move().type()!=0||Board.getSquare(p.plane0(),p.plane1(),p.plane2(),p.plane3(),row.move().to())!=0)reason="capture_or_special";
                    if(reason!=null){counters.merge(reason,1L,Long::sum);continue;}
                    if(row.result() < -1||row.result()>1)throw new IOException("Result range");
                    int score=link(row.target());byte[] packed=pack(p,score,row.result());
                    OneCorpusAudit.key(ByteBuffer.wrap(packed).order(ByteOrder.LITTLE_ENDIAN),0,key,squares);
                    if(contains(all,0,uniqueCount,key)||contains(reservations,12,reservedCount,key)||!seen.add(new Key(key))){counters.merge("canonical_alias",1L,Long::sum);continue;}
                    (validation?heldOut:trainOut).write(packed);if(validation)held++;else training++;accepted++;
                    int sign=stm==0?1:-1;String fen=p.fen();if(!p.halfmoveKnown())fen+=" 0";fen+=" 1";
                    proof.write(fen+" | "+sign*score+" | "+((sign*row.result()+1)*.5));proof.newLine();
                    labels.write(new Gson().toJson(Map.of("split",validation?"fresh_validation":"training_replacement","unit",index,"ordinal",read-1,"encoded",row.target(),"stm_result",row.result(),"link",score,"ply",row.ply(),"pieces",Long.bitCount(p.plane0()|p.plane1()|p.plane2()))));labels.newLine();
                }
                evidence.add(Map.of("unit_index",index,"unit",unit,"split",validation?"fresh_validation":"training_replacement","decoded",read,"accepted",accepted));
                System.out.println("Supplement unit="+index+" train="+training+" fresh_validation="+held);
            }
        }
        source.verify();var hashes=new TreeMap<String,String>();for(String name:List.of("topup.data","fresh-validation.data","proof.txt","labels.jsonl"))hashes.put(name,PairCoverageDiagnostic.sha(out.resolve(name)));
        var result=new LinkedHashMap<String,Object>();result.put("experiment","R057");result.put("training_replacements",training);result.put("fresh_validation",held);result.put("seed",seed);result.put("plan_sha256",PairCoverageDiagnostic.sha(planPath));
        result.put("original_manifest_sha256",PairCoverageDiagnostic.sha(originalPath));result.put("prepared_identity",manifest.identity());result.put("units",evidence);result.put("counts",counters);result.put("hashes",hashes);result.put("seconds",(System.nanoTime()-started)/1e9);
        result.put("method","Unused original-source prepared units; same quiet/noncheck/ply>=16/BT4 inverse-Q logit400 methodology; quota4096 vs CP64's32768. All original training and validation/diagnostic aliases excluded, global new-sample dedup, validation units first and distinct. Game identity remains unknown. Fresh validation is reserved until selected model freeze.");
        Files.writeString(out.resolve("manifest.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result)+"\n",StandardOpenOption.CREATE_NEW);
    }
}
