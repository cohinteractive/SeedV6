import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.ohinteractive.seedv6.core.Board;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Research data audit only. Never edits source data or uses labels for selection.
 * A neighbor has identical piece placement after deleting at most one piece from
 * each board. All rank/color and file-mirror symmetries and STM are ignored,
 * conservatively excluding more than identical evaluator inputs.
 */
public final class ValidationSeparationAudit {
    static final long[][] Z = new long[16][64];
    static { var r=new SplittableRandom(2026111401L);for(var a:Z)for(int i=0;i<64;i++)a[i]=r.nextLong(); }
    static final Map<Long,ArrayList<Integer>> exact=new HashMap<>(),near=new HashMap<>();
    static void add(Map<Long,ArrayList<Integer>> map,long key,int id) {
        var ids=map.computeIfAbsent(key,k->new ArrayList<>());if(!ids.contains(id))ids.add(id);
    }
    static void hit(Map<Long,ArrayList<Integer>> map,long key,BitSet hits) {
        var ids=map.get(key);if(ids!=null)for(int id:ids)hits.set(id);
    }
    static void permission() throws Exception {
        var s=JsonParser.parseString(Files.readString(Path.of(System.getenv("SEED_NNUE_CONTROL")))).getAsJsonObject();
        var a=s.getAsJsonObject("active");
        if(!s.get("mode").getAsString().equals("NORMAL") || a==null
                || !a.get("id").getAsString().equals(System.getenv("SEED_NNUE_OPERATION"))
                || !a.get("stop_requested_at").isJsonNull()
                || (!s.get("expires_at").isJsonNull() && System.currentTimeMillis()/1000.0>=s.get("expires_at").getAsDouble()))
            throw new IOException("Compute permission withdrawn; preserve partial audit");
    }
    static String sha(Path path) throws Exception {
        var md=MessageDigest.getInstance("SHA-256");try(var in=Files.newInputStream(path)) {
            byte[] bytes=new byte[1048576];for(int n;(n=in.read(bytes))>0;)md.update(bytes,0,n);
        }return HexFormat.of().formatHex(md.digest());
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=3)throw new IllegalArgumentException("dataset new-output sample-count");
        permission();long start=System.nanoTime();var data=Path.of(args[0]);var out=Path.of(args[1]);
        int limit=Integer.parseInt(args[2]);if(limit<128||limit>16384)throw new IllegalArgumentException("bounded sample");
        Files.createDirectory(out);
        var manifest=JsonParser.parseString(Files.readString(data.resolve("manifest.json"))).getAsJsonObject();
        var hashes=new TreeMap<String,String>();
        for(String name:List.of("train.data","validation.txt","train-provenance.bin")) {
            permission();String hash=sha(data.resolve(name));
            if(!hash.equals(manifest.getAsJsonObject("hashes").get(name).getAsString()))throw new IOException("Changed data: "+name);
            hashes.put(name,hash);
        }
        var all=Files.readAllLines(data.resolve("validation.txt"));var selected=new ArrayList<String>();
        // Evenly spaced source ordinals, chosen without inspecting targets/predictions.
        for(int id=0;id<limit;id++) {
            String line=all.get((int)((long)id*all.size()/limit));selected.add(line);
            long[] b=Board.fromFen(line.split("\\|")[0].trim());
            for(int flip:new int[]{0,7,56,63}) {
                var atoms=new ArrayList<Long>();long hash=0;
                for(int sq=0;sq<64;sq++) {
                    int piece=Board.getSquare(b[0],b[1],b[2],b[3],sq);if(piece==0)continue;
                    int code=(6-(piece&7))|((piece>>>3)^((flip&56)==0?0:1))<<3;
                    long atom=Z[code][sq^flip];atoms.add(atom);hash^=atom;
                }
                add(exact,hash,id);for(long atom:atoms)add(near,hash^atom,id);
            }
        }
        var exactHits=new BitSet(limit);var nearHits=new BitSet(limit);long records=0;
        var atoms=new long[32];byte[] record=new byte[32];
        try(var in=new BufferedInputStream(Files.newInputStream(data.resolve("train.data")),1<<20)) {
            while(true) {
                int n=in.readNBytes(record,0,32);if(n==0)break;if(n!=32)throw new EOFException();
                long occ=ByteBuffer.wrap(record).order(ByteOrder.LITTLE_ENDIAN).getLong();
                int pieces=Long.bitCount(occ),i=0;if(pieces>32)throw new IOException("Invalid record");long hash=0;
                while(occ!=0) {
                    int sq=Long.numberOfTrailingZeros(occ);occ&=occ-1;int code=(record[8+i/2]>>>(4*(i&1)))&15;
                    if((code&7)>5)throw new IOException("Invalid piece");atoms[i++]=Z[code][sq];hash^=atoms[i-1];
                }
                hit(exact,hash,exactHits);for(i=0;i<pieces;i++)hit(near,hash^atoms[i],nearHits);
                if((++records&1048575)==0) {
                    permission();System.out.printf(Locale.ROOT,"records=%d exact=%d neighbors=%d seconds=%.1f%n",
                            records,exactHits.cardinality(),nearHits.cardinality(),(System.nanoTime()-start)/1e9);
                }
            }
        }
        if(records!=manifest.get("train").getAsLong())throw new IOException("Record count mismatch");
        var excluded=(BitSet)nearHits.clone();excluded.or(exactHits);
        try(var writer=Files.newBufferedWriter(out.resolve("validation.txt"),StandardOpenOption.CREATE_NEW)) {
            for(int i=0;i<limit;i++)if(!excluded.get(i)){writer.write(selected.get(i));writer.newLine();}
        }
        var result=new LinkedHashMap<String,Object>();result.put("source",data.toString());result.put("hashes",hashes);
        result.put("training_records",records);result.put("source_validation_records",all.size());result.put("sampled",limit);
        result.put("symmetry_exact_overlap",exactHits.cardinality());result.put("single_piece_neighbor_overlap",nearHits.cardinality());
        result.put("retained",limit-excluded.cardinality());result.put("validation_sha256",sha(out.resolve("validation.txt")));
        result.put("seconds",(System.nanoTime()-start)/1e9);
        result.put("method","Evenly spaced validation ordinals; label-blind. Exclude placement equality and equality after deleting one piece each, allowing rank+color flip and file mirror, ignoring STM. 64-bit Zobrist keys; collisions only conservatively reject additional samples.");
        result.put("limitation","No source-game IDs. Does not detect all multi-move relatives; cannot establish game-independent validation. Original validation was used historically and is diagnostic only.");
        var json=new GsonBuilder().setPrettyPrinting().create().toJson(result)+"\n";
        Files.writeString(out.resolve("summary.json"),json,StandardOpenOption.CREATE_NEW);System.out.println(json);
    }
}
