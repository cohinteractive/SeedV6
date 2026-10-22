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

/** Research-only extraction from existing immutable prepared BT4 chunks.
 * No application or historical BRN training path is changed.
 */
public final class PrepareBt4 {
    static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    record Key(long a,long b,long c,long d,int stm) {
        Key(CorpusPosition p) {this(p.plane0(),p.plane1(),p.plane2(),p.plane3(),p.rules()&1);}
    }
    static void permission() throws Exception {
        var s=JsonParser.parseString(Files.readString(Path.of(System.getenv("SEED_NNUE_CONTROL")))).getAsJsonObject();
        var a=s.getAsJsonObject("active");
        if(s.get("schema").getAsInt()!=1 || !s.get("mode").getAsString().equals("NORMAL") || a==null
            || !a.get("id").getAsString().equals(System.getenv("SEED_NNUE_OPERATION"))
            || !a.get("stop_requested_at").isJsonNull()
            || (!s.get("expires_at").isJsonNull() && System.currentTimeMillis()/1000.0>=s.get("expires_at").getAsDouble()))
            throw new IOException("Compute permission withdrawn; partial extraction is evidence only");
    }
    static String sha(Path p) throws Exception {
        var md=MessageDigest.getInstance("SHA-256");
        try(var in=Files.newInputStream(p)) {byte[] b=new byte[1048576];for(int n;(n=in.read(b))>=0;)md.update(b,0,n);}
        return HexFormat.of().formatHex(md.digest());
    }
    static int link(int encoded) {
        double p=Math.max(.0001,Math.min(.9999,(1+Bt4Targets.q(encoded))*.5));
        return (int)Math.round(400*StrictMath.log(p/(1-p)));
    }
    static byte[] pack(CorpusPosition p,int link,int result) {
        int stm=p.rules()&1,idx=0;long occ=0;byte[] bytes=new byte[32];
        for(int square=0;square<64;square++) {
            int original=square^(stm*56),piece=Board.getSquare(p.plane0(),p.plane1(),p.plane2(),p.plane3(),original);
            if(piece==0)continue;
            int code=6-(piece&7)|((piece>>>3)^stm)<<3;
            if(idx==32)throw new IllegalArgumentException("More than32 pieces");
            bytes[8+idx/2]|=(byte)(code<<(4*(idx&1)));idx++;occ|=1L<<square;
            if(code==5)bytes[27]=(byte)square;
            if(code==13)bytes[28]=(byte)(square^56);
        }
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putLong(occ).putShort(24,(short)link);
        bytes[26]=(byte)(result+1);return bytes;
    }
    static String text(BinpackDecoder.Record r,int link) {
        int sign=(r.position().rules()&1)==0?1:-1;
        String fen=r.position().fen();if(!r.position().halfmoveKnown())fen+=" 0";
        return fen+" 1 | "+(sign*link)+" | "+((sign*r.result()+1)*.5);
    }
    static void add(Map<String,Long> counts,String name) {counts.merge(name,1L,Long::sum);}
    public static void main(String[] args) throws Exception {
        if(args.length!=6)throw new IllegalArgumentException("source.json output trainUnits validationUnits recordsPerUnit seed");
        permission();long start=System.nanoTime();
        var source=JSON.fromJson(Files.readString(Path.of(args[0])),DataSource.class);source.verify();
        if(source.labelProfile()!=LabelProfile.BT4_Q_V1)throw new IllegalArgumentException("BT4 only");
        var manifest=PreparedBinpack.ready(source);var directory=PreparedBinpack.directory(source);
        int trainUnits=Integer.parseInt(args[2]),valUnits=Integer.parseInt(args[3]),quota=Integer.parseInt(args[4]);
        long seed=Long.parseLong(args[5]);
        if(trainUnits<1||valUnits<1||quota<128||quota>65536||(long)trainUnits*quota>33554432)
            throw new IllegalArgumentException("Outside bounded extraction limits");
        var out=Path.of(args[1]);Files.createDirectory(out);
        var indices=new ArrayList<Integer>();for(int i=0;i<manifest.units().size();i++)indices.add(i);
        Collections.shuffle(indices,new Random(seed));
        if(trainUnits+valUnits>indices.size())throw new IllegalArgumentException("Too many units");
        // Validation units are processed first. Exact placement+STM dedup across
        // the entire sample excludes all identical evaluator inputs from training.
        var seen=new HashSet<Key>();var counts=new TreeMap<String,Long>();var unitEvidence=new ArrayList<Object>();
        byte[] train=new byte[Math.multiplyExact(Math.multiplyExact(trainUnits,quota),32)];
        byte[] provenance=new byte[Math.multiplyExact(Math.multiplyExact(trainUnits,quota),12)];
        int trainCount=0,valCount=0,proofCount=0;
        try(var validation=Files.newBufferedWriter(out.resolve("validation.txt"));
            var probes=Files.newBufferedWriter(out.resolve("probes.fens"));
            var proofText=Files.newBufferedWriter(out.resolve("proof.txt"));
            var proofBinary=Files.newOutputStream(out.resolve("proof.data"));
            var proofRaw=Files.newBufferedWriter(out.resolve("proof-labels.jsonl"))) {
            for(int selected=0;selected<trainUnits+valUnits;selected++) {
                permission();boolean held=selected<valUnits;int index=indices.get(selected),accepted=0,read=0;
                var unit=manifest.units().get(index);byte[] compressed=new byte[unit.compressed()];
                try(var channel=FileChannel.open(directory.resolve("shard-"+unit.shard()+".zst"))) {
                    channel.position(unit.offset());var buffer=ByteBuffer.wrap(compressed);
                    while(buffer.hasRemaining())if(channel.read(buffer)<0)throw new EOFException();
                }
                byte[] decoded=Zstd.decompress(compressed,unit.decoded());
                if(!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(decoded)).equals(unit.hash()))
                    throw new IOException("Prepared unit checksum mismatch");
                var decoder=new BinpackDecoder(BinpackDecoder.readChunk(new ByteArrayInputStream(decoded)));
                for(BinpackDecoder.Record r;(r=decoder.next())!=null && accepted<quota;) {
                    if((read++&4095)==0)permission();add(counts,"decoded");
                    if(r.target()==32002) {add(counts,"sentinel");continue;}
                    if(Math.abs(r.target())>32000) {add(counts,"invalid_target");continue;}
                    var p=r.position();int stm=p.rules()&1;
                    if(r.ply()<16) {add(counts,"early_ply");continue;}
                    if(Board.isPlayerInCheck(p.plane0(),p.plane1(),p.plane2(),p.plane3(),stm)) {add(counts,"check");continue;}
                    var move=r.move();
                    if(move.type()!=0 || Board.getSquare(p.plane0(),p.plane1(),p.plane2(),p.plane3(),move.to())!=0) {
                        add(counts,"capture_or_special");continue;
                    }
                    if(!seen.add(new Key(p))) {add(counts,"duplicate_input");continue;}
                    if(r.result() < -1 || r.result()>1)throw new IOException("Invalid result");
                    int score=link(r.target());byte[] packed=pack(p,score,r.result());
                    int pieces=Long.bitCount(p.plane0()|p.plane1()|p.plane2());
                    add(counts,(held?"validation_":"train_")+"pieces_"+(pieces/4*4));
                    add(counts,(held?"validation_":"train_")+"ply_"+(r.ply()/32*32));
                    add(counts,(held?"validation_":"train_")+"q_"+(int)Math.floor(Bt4Targets.q(r.target())*10));
                    if(held) {
                        validation.write(text(r,score));validation.newLine();
                        if(valCount<128) {probes.write(text(r,score).split(" \\|")[0]);probes.newLine();}
                        valCount++;
                    } else {
                        System.arraycopy(packed,0,train,32*trainCount,32);
                        ByteBuffer.wrap(provenance,12*trainCount,12).order(ByteOrder.LITTLE_ENDIAN)
                            .putInt(index).putInt(read-1).putShort((short)r.target()).put((byte)r.result()).put((byte)0);
                        trainCount++;
                    }
                    if(accepted%512==0) {
                        proofText.write(text(r,score));proofText.newLine();proofBinary.write(packed);
                        proofRaw.write(new Gson().toJson(Map.of("unit",index,"ordinal",unit.start()+read-1,
                            "encoded",r.target(),"stm_result",r.result(),"link",score,"q",Bt4Targets.q(r.target()))));proofRaw.newLine();proofCount++;
                    }
                    accepted++;
                }
                unitEvidence.add(Map.of("index",index,"unit",unit,"split",held?"validation":"train","decoded",read,"accepted",accepted));
                System.out.printf(Locale.ROOT,"unit=%d/%d shard=%d chunk=%d train=%d validation=%d seconds=%.1f%n",
                    selected+1,trainUnits+valUnits,unit.shard(),unit.chunk(),trainCount,valCount,(System.nanoTime()-start)/1e9);
            }
        }
        permission();source.verify();var random=new Random(seed^0x425434L);
        for(int i=trainCount-1;i>0;i--) {
            if((i&65535)==0)permission();int j=random.nextInt(i+1);
            for(int k=0;k<32;k++) {byte b=train[32*i+k];train[32*i+k]=train[32*j+k];train[32*j+k]=b;}
            for(int k=0;k<12;k++) {byte b=provenance[12*i+k];provenance[12*i+k]=provenance[12*j+k];provenance[12*j+k]=b;}
        }
        try(var stream=Files.newOutputStream(out.resolve("train.data"))) {stream.write(train,0,trainCount*32);}
        try(var stream=Files.newOutputStream(out.resolve("train-provenance.bin"))) {stream.write(provenance,0,trainCount*12);}
        var hashes=new TreeMap<String,String>();
        try(var paths=Files.list(out)) {for(var p:paths.toList()) {permission();hashes.put(p.getFileName().toString(),sha(p));}}
        var result=new LinkedHashMap<String,Object>();
        result.put("schema","seed-bt4-bullet-v1");result.put("source",source);result.put("prepared_identity",manifest.identity());
        result.put("source_positions",manifest.count());result.put("seed",seed);result.put("train",trainCount);result.put("validation",valCount);
        result.put("proof_records",proofCount);result.put("counts",counts);result.put("units",unitEvidence);result.put("hashes",hashes);
        result.put("target","BT4_Q_LOGIT400_V1: inverse publisher Q, p=clamp((Q+1)/2,.0001,.9999), round(400*logit(p)); stored STM; genuine STM WDL");
        result.put("selection","Seeded shuffle of prepared unit indices; validation units first; first qualifying quota per unit; exact global placement+STM dedup; Fisher-Yates training shuffle");
        result.put("provenance","12 LE bytes/record: unit index u32, unit raw ordinal u32, BT4 encoded i16, STM result i8, zero pad; aligned with shuffled train.data");
        result.put("limitations",List.of("Unit-disjoint diagnostic validation; no proof of game or symmetry independence","Bounded prefixes within globally sampled chunks, not a uniform sample of all positions","Logit scores are link values, not original centipawns","Source identity uses sampled fingerprints; consumed prepared units have full SHA256 verification"));
        result.put("seconds",(System.nanoTime()-start)/1e9);
        Files.writeString(out.resolve("manifest.json"),JSON.toJson(result)+"\n",StandardOpenOption.CREATE_NEW);
        System.out.println("Completed train="+trainCount+" validation="+valCount);
    }
}
