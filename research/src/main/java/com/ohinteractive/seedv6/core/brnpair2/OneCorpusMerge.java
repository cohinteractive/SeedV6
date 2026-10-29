package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Exact, bounded-memory cross-shard uniqueness and reservation audit. No model fitting. */
public final class OneCorpusMerge {
    static final class Cursor implements AutoCloseable {
        final DataInputStream input; final int shard; int remaining,id; long a,b,c;
        Cursor(Path path,int shard)throws IOException {
            this.shard=shard;input=new DataInputStream(new BufferedInputStream(Files.newInputStream(path),65536));
            if(input.readLong()!=OneCorpusAudit.MAGIC)throw new IOException("Index schema");
            remaining=input.readInt();if(remaining!=OneCorpusAudit.CHUNK)throw new IOException("Shard count");
        }
        boolean next()throws IOException {
            if(remaining==0){if(input.read()!=-1)throw new IOException("Trailing index bytes");return false;}
            a=input.readLong();b=input.readLong();c=input.readLong();id=input.readInt();remaining--;
            if(id<shard*OneCorpusAudit.CHUNK||id>=(shard+1)*OneCorpusAudit.CHUNK)throw new IOException("Source index range");
            return true;
        }
        public void close()throws IOException{input.close();}
    }
    public static void main(String[] args)throws Exception {
        PairObjectiveDiagnostic.permission();long start=System.nanoTime();Path shards=Path.of(args[0]),reserved=Path.of(args[1]),out=Path.of(args[2]);
        var reservation=JsonParser.parseString(Files.readString(reserved.resolveSibling("manifest.json"))).getAsJsonObject();
        if(!PairCoverageDiagnostic.sha(reserved).equals(reservation.get("sha256").getAsString()))throw new IllegalArgumentException("Reservations changed");
        long[] ra,rb,rc;
        try(var in=new DataInputStream(new BufferedInputStream(Files.newInputStream(reserved)))){
            if(!Arrays.equals(in.readNBytes(8),"R057EX01".getBytes(java.nio.charset.StandardCharsets.US_ASCII)))throw new IOException("Reservation schema");
            int n=in.readInt();ra=new long[n];rb=new long[n];rc=new long[n];
            for(int i=0;i<n;i++){ra[i]=in.readLong();rb[i]=in.readLong();rc[i]=in.readLong();if(i>0&&OneCorpusAudit.compare(ra[i-1],rb[i-1],rc[i-1],ra[i],rb[i],rc[i])>=0)throw new IOException("Reservation sort");}
            if(in.read()!=-1)throw new IOException("Reservation trailing bytes");
        }
        Files.createDirectory(out);var cursors=new ArrayList<Cursor>();var bindings=new LinkedHashMap<String,String>();
        var queue=new PriorityQueue<Cursor>((x,y)->{int d=OneCorpusAudit.compare(x.a,x.b,x.c,y.a,y.b,y.c);return d!=0?d:Integer.compare(x.id,y.id);});
        var seen=new BitSet(33554432);var excluded=new BitSet(33554432);long rows=0,unique=0,uniqueExcluded=0;int r=0;long pa=0,pb=0,pc=0;boolean first=true,exclude=false;int group=0;
        var multiplicities=new TreeMap<Integer,Long>();long[] pieces=new long[33],remainingPieces=new long[33];
        Path keys=out.resolve("unique.keys.partial");
        try(var file=new FileOutputStream(keys.toFile());var output=new DataOutputStream(new BufferedOutputStream(file))){
            for(int shard=0;shard<32;shard++){
                PairObjectiveDiagnostic.permission();Path path=shards.resolve(String.format(Locale.ROOT,"keys-%03d.bin",shard));
                var receipt=JsonParser.parseString(Files.readString(path.resolveSibling(path.getFileName().toString().replace(".bin",".json")))).getAsJsonObject();
                String digest=PairCoverageDiagnostic.sha(path);
                if(!digest.equals(receipt.get("sha256").getAsString())||!receipt.get("source_sha256").getAsString().equals("b9306efe0675a0e0c0c3bea9d64e4bccda856f35ebafe79ab5a87472ca5b9e2c"))throw new IOException("Shard binding");
                bindings.put(path.toString(),digest);var cursor=new Cursor(path,shard);cursors.add(cursor);if(cursor.next())queue.add(cursor);
            }
            while(!queue.isEmpty()){
                var c=queue.remove();if((rows&65535)==0)PairObjectiveDiagnostic.permission();
                if(seen.get(c.id))throw new IllegalStateException("Duplicate source index");seen.set(c.id);rows++;
                boolean changed=first||c.a!=pa||c.b!=pb||c.c!=pc;
                if(changed){
                    if(!first){if(OneCorpusAudit.compare(pa,pb,pc,c.a,c.b,c.c)>=0)throw new IllegalStateException("Merge order");multiplicities.merge(group,1L,Long::sum);}
                    unique++;group=0;pa=c.a;pb=c.b;pc=c.c;first=false;
                    while(r<ra.length&&OneCorpusAudit.compare(ra[r],rb[r],rc[r],pa,pb,pc)<0)r++;
                    exclude=r<ra.length&&ra[r]==pa&&rb[r]==pb&&rc[r]==pc;if(exclude)uniqueExcluded++;
                    output.writeLong(pa);output.writeLong(pb);output.writeLong(pc);
                }
                group++;int count=Long.bitCount(c.a);pieces[count]++;
                if(exclude)excluded.set(c.id);else remainingPieces[count]++;
                if(c.next())queue.add(c);
            }
            multiplicities.merge(group,1L,Long::sum);output.flush();file.getFD().sync();
        } finally {for(var c:cursors)c.close();}
        if(rows!=33554432||seen.cardinality()!=33554432)throw new IllegalStateException("Incomplete source coverage");
        Path complete=out.resolve("unique.keys");Files.move(keys,complete,StandardCopyOption.ATOMIC_MOVE);
        Path mask=out.resolve("excluded.bitset");try(var stream=new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(mask,StandardOpenOption.CREATE_NEW)))){
            stream.writeLong(0x5230353745584231L);stream.writeInt(33554432);byte[] bytes=excluded.toByteArray();stream.writeInt(bytes.length);stream.write(bytes);
        }
        var result=new LinkedHashMap<String,Object>();result.put("experiment","R057");result.put("training_samples",rows);result.put("exact_canonical_unique",unique);result.put("canonical_duplicate_samples",rows-unique);
        result.put("excluded_samples",excluded.cardinality());result.put("excluded_unique",uniqueExcluded);result.put("remaining_samples",rows-excluded.cardinality());result.put("remaining_unique",unique-uniqueExcluded);
        result.put("alias_multiplicities",multiplicities);result.put("source_piece_counts",pieces);result.put("remaining_piece_counts",remainingPieces);
        result.put("shards",bindings);result.put("reserved_sha256",PairCoverageDiagnostic.sha(reserved));result.put("unique_keys_sha256",PairCoverageDiagnostic.sha(complete));result.put("excluded_bitset_sha256",PairCoverageDiagnostic.sha(mask));
        result.put("seconds",(System.nanoTime()-start)/1e9);result.put("scope","Exact normalized STM/color-rank/horizontal aliases; labels not merged. Existing CP64 rows retained for comparison except reserved aliases. This audit selects no replacements and trains no model.");
        Files.writeString(out.resolve("result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result)+"\n",StandardOpenOption.CREATE_NEW);
        System.out.println("R057 exact merge rows="+rows+" unique="+unique+" excluded="+excluded.cardinality());
    }
}
