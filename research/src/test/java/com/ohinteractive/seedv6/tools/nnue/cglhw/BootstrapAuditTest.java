package com.ohinteractive.seedv6.tools.nnue.cglhw;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.Brn3Trainer;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BootstrapAuditTest {
    @Test void freshProductionModelsSatisfyMaterialIdentityAndIncrementalOracle() throws Exception {
        var edges=BootstrapAudit.corpus(2,32);
        assertEquals(64,edges.size());
        for(long seed:new long[]{41001,41002}) {
            var result=BootstrapAudit.diagnostics(seed,edges,NnueNetwork.initialized(seed),new Brn3Trainer(seed));
            assertEquals(0.0,result.get("brnReadoutMaxAbs"));
            assertEquals(0.0,result.get("brnMaterialMaxErrorPawns"));
            assertEquals(0L,result.get("optimizerUpdates"));
        }
        var another=BootstrapAudit.corpus(2,32);
        for(int i=0;i<edges.size();i++)assertArrayEquals(edges.get(i).child(),another.get(i).child());
    }
    @Test void independentOracleHasCorrectPerspectiveAndPieceValues() {
        assertEquals(21.5,BootstrapAudit.materialOracle(Board.fromFen("4k3/8/8/8/8/8/PNBRQ3/4K3 w - - 0 1")),1e-12);
        assertEquals(-21.5,BootstrapAudit.materialOracle(Board.fromFen("4k3/8/8/8/8/8/PNBRQ3/4K3 b - - 0 1")),1e-12);
    }
    @Test void symmetriesRoundTripCompleteBoardIncludingCastlingAndEnPassant() {
        for(String fen:new String[]{Board.FEN_STARTING_POSITION,"rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 3","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 3"}) {
            var b=Board.fromFen(fen);
            assertArrayEquals(b,BootstrapAudit.symmetry(BootstrapAudit.symmetry(b,true),true));
            if(fen.contains(" w - "))assertArrayEquals(b,BootstrapAudit.symmetry(BootstrapAudit.symmetry(b,false),false));
        }
        assertThrows(IllegalArgumentException.class,()->BootstrapAudit.symmetry(Board.startingPosition(),false));
    }
}
