package com.ohinteractive.seedv6.core.brnpair2;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OneTrainingCorpusTest {
    @Test void bitSetTrailingZeroBytesExpandWithoutChangingOrdinals()throws Exception{
        byte[] mask=OneTrainingCorpus.expandMask(java.nio.ByteBuffer.wrap(new byte[]{(byte)129}),1,32);
        assertArrayEquals(new byte[]{(byte)129,0,0,0},mask);int[] prefix=OneTrainingCorpus.prefix(mask);
        assertEquals(0,OneTrainingCorpus.replacementIndex(mask,prefix,0));assertEquals(1,OneTrainingCorpus.replacementIndex(mask,prefix,7));
        for(int i=8;i<32;i++)assertEquals(-1,OneTrainingCorpus.replacementIndex(mask,prefix,i));
        assertThrows(java.io.IOException.class,()->OneTrainingCorpus.expandMask(java.nio.ByteBuffer.allocate(2),2,8));
    }
    @Test void everyMaskBitMapsOnceToItsOwnReplacementOrdinal(){var random=new Random(71);byte[] mask=new byte[4096];random.nextBytes(mask);int[] prefix=OneTrainingCorpus.prefix(mask);int count=0;
        for(int i=0;i<mask.length*8;i++){boolean set=(mask[i/8]&(1<<(i%8)))!=0;assertEquals(set?count++:-1,OneTrainingCorpus.replacementIndex(mask,prefix,i));}assertEquals(count,prefix[mask.length]);}
    @Test void fullEpochIsBijectivePilotSubsetStableAndSegmentCursorExact(){
        var full=new OneTrainingCorpus.Order(OneTrainingCorpus.ROWS,0,20261029059L);var seen=new BitSet(OneTrainingCorpus.ROWS);
        for(int i=0;i<OneTrainingCorpus.ROWS;i++){int index=full.at(i);assertFalse(seen.get(index));seen.set(index);}assertEquals(OneTrainingCorpus.ROWS,seen.cardinality());
        BitSet prior=null;for(int epoch=0;epoch<2;epoch++){var order=new OneTrainingCorpus.Order(1_048_576,epoch,20261029059L);var resumed=new OneTrainingCorpus.Order(1_048_576,epoch,20261029059L);var subset=new BitSet(OneTrainingCorpus.ROWS);
            for(int i=0;i<1_048_576;i++){int index=order.at(i);assertFalse(subset.get(index));subset.set(index);assertEquals(index,resumed.at(i));}assertEquals(1_048_576,subset.cardinality());if(prior!=null)assertEquals(prior,subset);prior=subset;}
    }
}
