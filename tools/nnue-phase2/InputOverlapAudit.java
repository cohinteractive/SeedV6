import com.google.gson.*;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Probabilistic placement audit of the exact alternating4096-record mixture. */
public final class InputOverlapAudit {
    private static final long[][] Z=new long[16][64];
    static {var rng=new SplittableRandom(2026111504L);for(var row:Z)for(int i=0;i<64;i++)row[i]=rng.nextLong();}
    static void permission()throws Exception {
        var s=JsonParser.parseString(Files.readString(Path.of(System.getenv("SEED_NNUE_CONTROL")))).getAsJsonObject();
        var active=s.getAsJsonObject("active");
        if(!s.get("mode").getAsString().equals("NORMAL")||active==null
            ||!active.get("id").getAsString().equals(System.getenv("SEED_NNUE_OPERATION"))
            ||!active.get("stop_requested_at").isJsonNull()
            ||(!s.get("expires_at").isJsonNull()&&System.currentTimeMillis()/1000.0>=s.get("expires_at").getAsDouble()))
            throw new IOException("Compute permission withdrawn");
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=3)throw new IllegalArgumentException("mixture.data expected-sha256 new-output.json");
        permission();long started=System.nanoTime();Path input=Path.of(args[0]),output=Path.of(args[2]);
        long size=Files.size(input),records=size/32;
        if(size%32!=0||records==0||records>67108864||records%8192!=0)throw new IOException("Not a bounded equal mixture");
        if(Files.exists(output))throw new IOException("Preserve completed audit");
        long[] keys=new long[(int)records];var digest=MessageDigest.getInstance("SHA-256");
        byte[] bytes=new byte[1<<20];var buffer=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);int record=0;
        try(var in=Files.newInputStream(input)) {
            for(int n;(n=in.readNBytes(bytes,0,bytes.length))!=0;) {
                if(n%32!=0)throw new EOFException();digest.update(bytes,0,n);
                for(int at=0;at<n;at+=32) {
                    long occupied=buffer.getLong(at),normal=0,mirror=0;int pieces=Long.bitCount(occupied),i=0;
                    if(pieces<2||pieces>32)throw new IOException("Invalid placement");
                    while(occupied!=0){int sq=Long.numberOfTrailingZeros(occupied);occupied&=occupied-1;
                        int pc=(bytes[at+8+i/2]>>>(4*(i&1)))&15;i++;
                        if((pc&7)>5)throw new IOException("Invalid piece code");normal^=Z[pc][sq];mirror^=Z[pc][sq^7];}
                    long canonical=Long.compareUnsigned(normal,mirror)<0?normal:mirror;
                    keys[record]=(canonical&~1L)|((record/4096)&1);record++;
                }
                permission();if((System.nanoTime()-started)/1e9>300)throw new IOException("Audit budget exceeded");
            }
        }
        if(record!=records||!HexFormat.of().formatHex(digest.digest()).equals(args[1]))throw new IOException("Dataset identity mismatch");
        Arrays.parallelSort(keys);permission();
        long unique=0,oldUnique=0,newUnique=0,shared=0,newOnly=0,matchingNewRecords=0;
        var histogram=new TreeMap<Integer,Long>();
        for(int start=0;start<keys.length;) {
            int end=start;long key=keys[start]&~1L,oldCount=0,newCount=0;
            while(end<keys.length&&(keys[end]&~1L)==key){if((keys[end]&1)==0)oldCount++;else newCount++;end++;}
            unique++;if(oldCount>0)oldUnique++;if(newCount>0)newUnique++;
            if(oldCount>0&&newCount>0){shared++;matchingNewRecords+=newCount;}
            if(oldCount==0)newOnly++;
            int bucket=31-Integer.numberOfLeadingZeros(end-start);histogram.merge(bucket,1L,Long::sum);start=end;
        }
        double seconds=(System.nanoTime()-started)/1e9;if(seconds>300)throw new IOException("Audit budget exceeded");
        var result=new LinkedHashMap<String,Object>();result.put("schema","seed-phase2-input-overlap-v1");
        result.put("input",input.toString());result.put("sha256",args[1]);result.put("records",records);
        result.put("source_records_each",records/2);result.put("unique_fingerprints",unique);
        result.put("original_unique_fingerprints",oldUnique);result.put("additional_unique_fingerprints",newUnique);
        result.put("shared_fingerprints",shared);result.put("additional_only_fingerprints",newOnly);
        result.put("additional_records_matching_original",matchingNewRecords);result.put("duplicate_records",records-unique);
        result.put("multiplicity_log2_floor_histogram",histogram);result.put("seconds",seconds);
        result.put("method","Seed2026111504 Zobrist placement fingerprints, native STM view, canonical file mirror,63 hash bits+alternating4096-batch source bit; labels/history excluded.");
        result.put("collision_model_expected_pair_collisions_upper_approx",(double)records*(records-1)/(2.0*Math.pow(2,63))*2);
        result.put("limitations","Fingerprint matches are probabilistic, not exact uniqueness or game independence. Random-hash collision approximation is not a guarantee. No source data or experimental selection is changed.");
        Files.writeString(output,new GsonBuilder().setPrettyPrinting().create().toJson(result)+"\n",StandardOpenOption.CREATE_NEW);
        System.out.println(new Gson().toJson(result));
    }
}
