package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

/** Exact compact alias index for normalized Bullet positions, with immutable shard receipts. */
public final class OneCorpusAudit {
    static final int CHUNK = 1 << 20;
    static final long MAGIC = 0x52303537414c3031L;
    static long horizontal(long x) {
        x = ((x >>> 1) & 0x5555555555555555L) | ((x & 0x5555555555555555L) << 1);
        x = ((x >>> 2) & 0x3333333333333333L) | ((x & 0x3333333333333333L) << 2);
        return ((x >>> 4) & 0x0f0f0f0f0f0f0f0fL) | ((x & 0x0f0f0f0f0f0f0f0fL) << 4);
    }
    /** Bullet normalizes STM to White by colour/rank reflection. Keys also fold horizontal reflection.
     * Three unsigned words encode exact occupancy and 32 ordered piece nibbles, without hashing. */
    static void key(ByteBuffer source, int offset, long[] result, int[] squares) {
        long occupied = source.getLong(offset), lo = 0, hi = 0; int count = 0;
        Arrays.fill(squares, -1);
        for (long bits = occupied; bits != 0; bits &= bits - 1) {
            int square = Long.numberOfTrailingZeros(bits);
            int code = (source.get(offset + 8 + count / 2) >>> (4 * (count % 2))) & 15;
            if ((code & 7) > 5 || count >= 32) throw new IllegalArgumentException("Bullet piece encoding");
            squares[square] = code;
            if (count < 16) lo |= (long)code << (4 * count); else hi |= (long)code << (4 * (count - 16));
            count++;
        }
        if (count < 2) throw new IllegalArgumentException("Bullet piece count");
        long mirrored = horizontal(occupied), ml = 0, mh = 0; count = 0;
        for (long bits = mirrored; bits != 0; bits &= bits - 1) {
            int code = squares[Long.numberOfTrailingZeros(bits) ^ 7];
            if (count < 16) ml |= (long)code << (4 * count); else mh |= (long)code << (4 * (count - 16));
            count++;
        }
        boolean mirror = compare(mirrored, ml, mh, occupied, lo, hi) < 0;
        result[0] = mirror ? mirrored : occupied; result[1] = mirror ? ml : lo; result[2] = mirror ? mh : hi;
    }
    static int compare(long a, long b, long c, long x, long y, long z) {
        int d = Long.compareUnsigned(a, x); if (d != 0) return d;
        d = Long.compareUnsigned(b, y); return d != 0 ? d : Long.compareUnsigned(c, z);
    }
    static void sort(long[] a, long[] b, long[] c, int[] ids, int left, int right) {
        // Recurse into the smaller partition; stack remains logarithmically bounded.
        while (left < right) {
            int i=left,j=right,m=(left+right)>>>1; long x=a[m],y=b[m],z=c[m];
            while(i<=j){
                while(compare(a[i],b[i],c[i],x,y,z)<0)i++;
                while(compare(a[j],b[j],c[j],x,y,z)>0)j--;
                if(i<=j){long t=a[i];a[i]=a[j];a[j]=t;t=b[i];b[i]=b[j];b[j]=t;t=c[i];c[i]=c[j];c[j]=t;int id=ids[i];ids[i]=ids[j];ids[j]=id;i++;j--;}
            }
            if(j-left<right-i){if(left<j)sort(a,b,c,ids,left,j);left=i;}
            else{if(i<right)sort(a,b,c,ids,i,right);right=j;}
        }
    }
    public static void main(String[] args) throws Exception {
        PairObjectiveDiagnostic.permission(); Path source=Path.of(args[0]),directory=Path.of(args[1]);
        int begin=Integer.parseInt(args[2]),end=Integer.parseInt(args[3]);
        if(begin<0||end<=begin||end-begin>8)throw new IllegalArgumentException("At most eight checkpointed shards");
        long records=Files.size(source)/32;
        if(Files.size(source)%32!=0||records!=33554432)throw new IllegalArgumentException("Expected original CP64 corpus");
        String sourceSha=PairCoverageDiagnostic.sha(source);
        if(!sourceSha.equals("b9306efe0675a0e0c0c3bea9d64e4bccda856f35ebafe79ab5a87472ca5b9e2c"))throw new IllegalArgumentException("Original data identity");
        Files.createDirectories(directory);
        try(var input=new RandomAccessFile(source.toFile(),"r")){
            for(int shard=begin;shard<end;shard++){
                PairObjectiveDiagnostic.permission();int first=shard*CHUNK,n=(int)Math.min(CHUNK,records-first);
                if(n<=0)throw new IllegalArgumentException("Shard outside corpus");
                String stem=String.format(Locale.ROOT,"keys-%03d",shard);Path receipt=directory.resolve(stem+".json"),output=directory.resolve(stem+".bin");
                if(Files.exists(receipt)){
                    var saved=JsonParser.parseString(Files.readString(receipt)).getAsJsonObject();
                    if(!sourceSha.equals(saved.get("source_sha256").getAsString())||!PairCoverageDiagnostic.sha(output).equals(saved.get("sha256").getAsString()))throw new IllegalStateException("Shard binding");
                    System.out.println("Verified completed "+stem);continue;
                }
                if(Files.exists(output))throw new IllegalStateException("Unreceipted completed file preserved: "+output);
                long started=System.nanoTime();byte[] raw=new byte[n*32];input.seek((long)first*32);input.readFully(raw);
                var buffer=ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);long[] a=new long[n],b=new long[n],c=new long[n],key=new long[3];int[] ids=new int[n],squares=new int[64];
                for(int i=0;i<n;i++){key(buffer,i*32,key,squares);a[i]=key[0];b[i]=key[1];c[i]=key[2];ids[i]=first+i;}
                sort(a,b,c,ids,0,n-1);long duplicates=0;
                Path partial=directory.resolve(stem+".partial-"+System.nanoTime());
                try(var file=new FileOutputStream(partial.toFile());var out=new DataOutputStream(new BufferedOutputStream(file))){
                    out.writeLong(MAGIC);out.writeInt(n);
                    for(int i=0;i<n;i++){
                        if(i>0&&compare(a[i-1],b[i-1],c[i-1],a[i],b[i],c[i])>0)throw new IllegalStateException("Unsorted keys");
                        if(i>0&&a[i-1]==a[i]&&b[i-1]==b[i]&&c[i-1]==c[i])duplicates++;
                        out.writeLong(a[i]);out.writeLong(b[i]);out.writeLong(c[i]);out.writeInt(ids[i]);
                    }
                    out.flush();file.getFD().sync();
                }
                Files.move(partial,output,StandardCopyOption.ATOMIC_MOVE);
                var result=Map.of("shard",shard,"first_source_index",first,"records",n,"within_shard_alias_duplicates",duplicates,
                    "source_sha256",sourceSha,"sha256",PairCoverageDiagnostic.sha(output),"seconds",(System.nanoTime()-started)/1e9);
                Path pending=directory.resolve(stem+".receipt-"+System.nanoTime());
                Files.writeString(pending,new GsonBuilder().setPrettyPrinting().create().toJson(result)+"\n",StandardOpenOption.CREATE_NEW);
                Files.move(pending,receipt,StandardCopyOption.ATOMIC_MOVE);System.out.println("Completed "+stem+" "+result);
            }
        }
    }
}
