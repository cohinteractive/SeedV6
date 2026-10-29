package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import java.nio.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OneCorpusAuditTest {
    static byte[] packed(long[] board, boolean mirror) {
        int stm=Board.player((int)board[4]);int[] squares=new int[64];Arrays.fill(squares,-1);
        for(int s=0;s<64;s++){
            int p=Board.getSquare(board[0],board[1],board[2],board[3],s);
            if(p!=0)squares[s^(stm*56)^(mirror?7:0)]=(6-(p&7))+8*((p>>>3)^stm);
        }
        byte[] bytes=new byte[32];var b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);long occupied=0;int n=0;
        for(int s=0;s<64;s++)if(squares[s]>=0){occupied|=1L<<s;bytes[8+n/2]|=(byte)(squares[s]<<(4*(n%2)));n++;}
        b.putLong(0,occupied);return bytes;
    }
    static long[] key(byte[] packed) {long[] out=new long[3];OneCorpusAudit.key(ByteBuffer.wrap(packed).order(ByteOrder.LITTLE_ENDIAN),0,out,new int[64]);return out;}
    @Test void exactPackedKeyMatchesIndependentSquareReflectionsAndDistinguishesTurn() {
        for(String fen:IncrementalOneTest.SPECIAL){
            var b=Board.fromFen(fen);assertArrayEquals(key(packed(b,false)),key(packed(b,true)));
            var mirrored=b.clone();long occ=b[0]|b[1]|b[2];
            for(int i=0;i<3;i++)mirrored[i]=Long.reverseBytes(b[i]);
            mirrored[3]=Long.reverseBytes(occ^b[3]);mirrored[4]^=1;
            assertArrayEquals(key(packed(b,false)),key(packed(mirrored,false)));
            assertArrayEquals(key(packed(b,false)),key(packed(mirrored,true)));
        }
        var b=Board.fromFen(IncrementalOneTest.SPECIAL[2]);var flip=b.clone();flip[4]^=1;
        assertFalse(Arrays.equals(key(packed(b,false)),key(packed(flip,false))));
        var r=new Random(57);
        for(int i=0;i<1000;i++){
            long value=r.nextLong(),oracle=0;for(int s=0;s<64;s++)if((value&(1L<<s))!=0)oracle|=1L<<(s^7);
            assertEquals(oracle,OneCorpusAudit.horizontal(value));
        }
    }
    @Test void unsignedTripleSortPreservesEverySourceIdentityAndDuplicate() {
        int n=10000;long[] a=new long[n],b=new long[n],c=new long[n];int[] ids=new int[n];var r=new Random(57);
        long[][] originals=new long[n][];
        for(int i=0;i<n;i++){a[i]=r.nextInt(5)-2;b[i]=r.nextInt(7)-3;c[i]=r.nextInt(3)-1;ids[i]=i;originals[i]=new long[]{a[i],b[i],c[i]};}
        OneCorpusAudit.sort(a,b,c,ids,0,n-1);boolean[] seen=new boolean[n];
        for(int i=0;i<n;i++){
            assertFalse(seen[ids[i]]);seen[ids[i]]=true;assertArrayEquals(originals[ids[i]],new long[]{a[i],b[i],c[i]});
            if(i>0)assertTrue(OneCorpusAudit.compare(a[i-1],b[i-1],c[i-1],a[i],b[i],c[i])<=0);
        }
    }
}
