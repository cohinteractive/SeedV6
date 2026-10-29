package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.corpus.CorpusPosition;
import java.nio.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OneCorpusSupplementTest {
    @Test void supplementalPackingMatchesIndependentSquareEncoderForBothTurns() {
        for(String fen:IncrementalOneTest.SPECIAL){
            var board=Board.fromFen(fen);var position=CorpusPosition.fromFen(fen);
            byte[] actual=OneCorpusSupplement.pack(position,123,-1),expected=OneCorpusAuditTest.packed(board,false);
            for(int i=0;i<24;i++)assertEquals(expected[i],actual[i]);
            assertEquals(123,ByteBuffer.wrap(actual).order(ByteOrder.LITTLE_ENDIAN).getShort(24));assertEquals(0,actual[26]);
        }
    }
    @Test void exactKeyMembershipRejectsAbsentEndpointsAndHandlesUnsignedOrder() {
        var keys=ByteBuffer.allocate(3*24);long[][] rows={{0,1,2},{Long.MAX_VALUE,3,4},{-1L,5,6}};
        for(var row:rows)for(long value:row)keys.putLong(value);
        for(var row:rows)assertTrue(OneCorpusSupplement.contains(keys,0,3,row));
        for(var row:new long[][]{{0,0,0},{1,2,3},{-1L,5,7}})assertFalse(OneCorpusSupplement.contains(keys,0,3,row));
        assertFalse(OneCorpusSupplement.contains(keys,0,0,new long[3]));
        assertEquals(0,OneCorpusSupplement.link(0));
        for(int encoded:new int[]{1,100,1000,10000,32000}){
            assertTrue(OneCorpusSupplement.link(encoded)>0);assertTrue(OneCorpusSupplement.link(-encoded)<0);
            assertEquals(OneCorpusSupplement.link(encoded),-OneCorpusSupplement.link(-encoded),1);
        }
    }
}
