package com.ohinteractive.seedv6.tools.nnue.cglhw;

import com.ohinteractive.seedv6.core.Board;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MaterialRemovalProbeTest {
    @Test void probeRemovesExactlyOneNonKingAndKeepsKingSafetyAndMaterialDirection() {
        var positions=MaterialRemovalProbe.positions();assertEquals(30,positions.size());
        for(var removal:positions) {
            var b=removal.board();assertEquals(31,Long.bitCount(b[0]|b[1]|b[2]));
            for(int side=0;side<2;side++)assertFalse(Board.isPlayerInCheckPext(b[0],b[1],b[2],b[3],side));
            assertTrue(((removal.code()&8)==0?-1:1)*BootstrapAudit.materialOracle(b)>0);
            if(removal.square()==0)assertFalse(Board.queenSide((int)b[4],0));
            if(removal.square()==7)assertFalse(Board.kingSide((int)b[4],0));
            if(removal.square()==56)assertFalse(Board.queenSide((int)b[4],1));
            if(removal.square()==63)assertFalse(Board.kingSide((int)b[4],1));
        }
    }
}
