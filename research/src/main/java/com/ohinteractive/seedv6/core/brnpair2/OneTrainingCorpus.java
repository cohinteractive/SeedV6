package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;

/** Exact logical replacement mapping and bounded-memory bijective epoch order. */
final class OneTrainingCorpus implements AutoCloseable {
    static final int ROWS=33_554_432,BLOCK=65_536;
    final ByteBuffer original,replacement,development;
    final byte[] mask;final int[] prefix;
    final List<FileChannel> channels=new ArrayList<>();
    OneTrainingCorpus(Path manifest)throws Exception {
        var m=JsonParser.parseString(Files.readString(manifest)).getAsJsonObject();
        if(!m.get("schema").getAsString().equals("r057-logical-corpus-v1")||m.get("samples").getAsInt()!=ROWS)throw new IOException("Corpus schema");
        var paths=m.getAsJsonObject("paths");var hashes=m.getAsJsonObject("hashes");
        for(String name:List.of("original","replacement","development","mask"))if(!PairCoverageDiagnostic.sha(Path.of(paths.get(name).getAsString())).equals(hashes.get(name).getAsString()))throw new IOException("Corpus hash "+name);
        original=map(Path.of(paths.get("original").getAsString()));replacement=map(Path.of(paths.get("replacement").getAsString()));development=map(Path.of(paths.get("development").getAsString()));
        var b=ByteBuffer.wrap(Files.readAllBytes(Path.of(paths.get("mask").getAsString())));
        if(b.getLong()!=0x5230353745584231L||b.getInt()!=ROWS)throw new IOException("Mask schema");int size=b.getInt();
        mask=expandMask(b,size,ROWS);prefix=prefix(mask);
        if(prefix[mask.length]!=39396||original.capacity()!=ROWS*32||replacement.capacity()!=39396*32||development.capacity()!=8192*32)throw new IOException("Corpus lengths");
    }
    static byte[] expandMask(ByteBuffer b,int size,int rows)throws IOException{
        // BitSet.toByteArray omits all zero bytes after the final set bit.
        if(size<0||size>(rows+7)/8||b.remaining()!=size)throw new IOException("Mask length");
        byte[] mask=new byte[(rows+7)/8];b.get(mask,0,size);return mask;
    }
    private ByteBuffer map(Path p)throws IOException {var c=FileChannel.open(p);channels.add(c);return c.map(FileChannel.MapMode.READ_ONLY,0,c.size()).order(ByteOrder.LITTLE_ENDIAN);}
    static int[] prefix(byte[] mask){int[] p=new int[mask.length+1];for(int i=0;i<mask.length;i++)p[i+1]=p[i]+Integer.bitCount(mask[i]&255);return p;}
    static int replacementIndex(byte[] mask,int[] prefix,int index){int b=index>>>3,bit=index&7;return (mask[b]&(1<<bit))==0?-1:prefix[b]+Integer.bitCount((mask[b]&255)&((1<<bit)-1));}
    double read(int index,TrainableOne m,TrainableOne.Features f){if(index<0||index>=ROWS)throw new IndexOutOfBoundsException();int r=replacementIndex(mask,prefix,index);return m.decode(r<0?original:replacement,(r<0?index:r)*32,f);}
    static final class Order {
        final int[] blocks;final int[] a=new int[512],b=new int[512];
        Order(int samples,int epoch,long dataSeed){
            if(samples!=1_048_576&&samples!=ROWS)throw new IllegalArgumentException("Declared sample count");
            int[] all=new int[512];for(int i=0;i<512;i++)all[i]=i;shuffle(all,new Random(dataSeed));blocks=Arrays.copyOf(all,samples/BLOCK);
            var random=new Random(dataSeed+epoch);shuffle(blocks,random);for(int i=0;i<512;i++){a[i]=random.nextInt(BLOCK)|1;b[i]=random.nextInt(BLOCK);}
        }
        int at(int cursor){int block=blocks[cursor/BLOCK];return block*BLOCK+((a[block]*(cursor&(BLOCK-1))+b[block])&(BLOCK-1));}
    }
    static void shuffle(int[] a,Random r){for(int i=a.length-1;i>0;i--){int j=r.nextInt(i+1),x=a[i];a[i]=a[j];a[j]=x;}}
    @Override public void close()throws IOException{for(var c:channels)c.close();}
}
