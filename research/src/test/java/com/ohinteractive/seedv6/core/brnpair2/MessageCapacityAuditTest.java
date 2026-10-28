package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MessageCapacityAuditTest {
    @Test void symbolicMonomialsReproduceBothActualNeuralFunctions() {
        for(boolean compose:new boolean[]{false,true}) {
            var model=new RelationalMessages(compose,.01,71);var random=new Random(97);
            for(int i=0;i<model.weights.length;i++)model.weights[i]=.3*random.nextGaussian();
            for(String fen:List.of("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1","r3k2r/pppq1ppp/2n1bn2/3pp3/3PP3/2N1BN2/PPPQ1PPP/R3K2R b KQkq - 0 1")) {
                var workspace=new RelationalMessages.Workspace();var f=workspace.encode(Board.fromFen(fen));
                assertEquals(model.logit(f,workspace),f.material+MessageCapacityAudit.evaluate(MessageCapacityAudit.terms(f,compose),model.weights),1e-12);
            }
        }
    }
    @Test void identicalCornersCancelAndDisconnectedKingKnightRemainAdditive() {
        var start=Board.startingPosition();assertEquals(0,MessageCapacityAudit.unmatched(new long[][]{start,start,start,start},true));
        long[][] boards={Board.fromFen("6k1/5ppp/8/8/8/8/5PPP/1N4K1 w - - 0 1"),Board.fromFen("6k1/5ppp/8/8/8/8/5PPP/1N3K2 w - - 0 1"),Board.fromFen("6k1/5ppp/8/8/8/2N5/5PPP/6K1 w - - 0 1"),Board.fromFen("6k1/5ppp/8/8/8/2N5/5PPP/5K2 w - - 0 1")};
        assertEquals(0,MessageCapacityAudit.unmatched(boards,false));assertEquals(0,MessageCapacityAudit.unmatched(boards,true));
    }
}
