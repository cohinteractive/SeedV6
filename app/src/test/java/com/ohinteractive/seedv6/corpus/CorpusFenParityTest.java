package com.ohinteractive.seedv6.corpus;

import com.ohinteractive.seedv6.core.Board;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CorpusFenParityTest {
    @Test void bulkParsingMatchesExistingBoardFieldsAndKeyAcrossRuleAndCounterCombinations() {
        String pieces="rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR";
        for(int side=0;side<2;side++)for(int mask=0;mask<16;mask++)for(int ep=-1;ep<8;ep++) {
            var rights=new StringBuilder();for(int i=0;i<4;i++)if((mask&(1<<i))!=0)rights.append("KQkq".charAt(i));
            String first=pieces+" "+(side==0?"w":"b")+" "+(mask==0?"-":rights)+" "+(ep<0?"-":"abcdefgh".charAt(ep)+(side==0?"6":"3"));
            for(int clock:new int[]{0,127,200,Integer.MAX_VALUE}) {
                String fen=first+" "+clock+" 1";
                assertArrayEquals(Board.fromFen(fen),CorpusPosition.fromFen(fen.replace(" ","\t  ")).toBoard(0));
                assertEquals(clock,CorpusPosition.fromFen(first+" "+clock).halfmove());
                assertEquals(CorpusPosition.fromFen(fen),CorpusPosition.fromFen(first+" "+clock+" 2147483647"));
            }
            assertArrayEquals(Board.fromFen(first+" 17 1"),CorpusPosition.fromFen(first).toBoard(17));
        }
    }
}
