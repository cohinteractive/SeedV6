package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Brn3IncrementalTransitionsTest {
    static Brn3Model nonzeroModel() {
        float[] weights=new float[Brn3Layout.MODEL_PARAMETERS];var rng=new Random(43091);
        for(int i=0;i<weights.length;i++)weights[i]=(float)((rng.nextDouble()-.5)*.25);
        return new Brn3Model(weights);
    }
    static void check(Brn3Model model,Brn3Workspace work,long[] board) {
        double expected=model.newWorkspace().evaluatePawns(board),actual=work.evaluatePawns(board);
        assertEquals(expected,actual,1e-10);
        assertEquals(Brn3Model.score(expected),Brn3Model.score(actual));
        assertEquals(model.newWorkspace().evaluatePawns(board,.25),work.evaluatePawns(board,.25),1e-10);
    }
    @Test void allSpecialMovesAndSiblingReturnsMatchFreshRebuildsAndReuseRelations() {
        var model=nonzeroModel();int captures=0,promotions=0;
        for(String fen:List.of(
                "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1",
                "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1",
                "1r2k3/P7/8/8/8/8/7p/4K3 w - - 0 1",
                "4k3/8/8/8/8/8/p6R/1R2K3 b - - 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")) {
            var root=Board.fromFen(fen);var work=model.newWorkspace();check(model,work,root);
            var moves=new long[512];int n=BrnResearchDiagnostics.legal(root,moves);
            for(int i=0;i<n;i++) {
                var child=BrnResearchDiagnostics.child(root,moves[i]);
                check(model,work,child);check(model,work,root);check(model,work,child);
                if(BrnResearchDiagnostics.pieces(child)<BrnResearchDiagnostics.pieces(root))captures++;
                if(((moves[i]>>>Board.PROMOTE_PIECE_SHIFT)&Board.PIECE_BITS)!=0)promotions++;
                var changed=child.clone();changed[4]^=Board.PLAYER_BIT;check(model,work,changed);
                changed[4]=(changed[4]&Board.PLAYER_BIT)|(~changed[4]&~Board.PLAYER_BIT);changed[5]=~changed[5];
                check(model,work,changed);
            }
            assertEquals(1,work.rebuilds,"Every small placement change, including reverse capture, should reuse raw sums");
            assertTrue(work.updates>0);assertTrue(work.repeats>0);
        }
        assertTrue(captures>=4);assertTrue(promotions>=8);
    }
    @Test void longRandomTraversalDistantJumpsAndSeparateWorkspacesStayIndependent() {
        var model=nonzeroModel();var work=model.newWorkspace();var independent=model.newWorkspace();
        var random=new Random(719);var board=Board.startingPosition();var other=Board.fromFen("4k3/8/8/8/8/8/4Q3/4K3 b - - 0 1");
        for(int i=0;i<5000;i++) {
            check(model,work,board);check(model,independent,other);
            if(i%61==0){check(model,work,other);check(model,work,board);}
            var moves=new long[512];int n=BrnResearchDiagnostics.legal(board,moves);
            board=n==0||i%200==199?Board.startingPosition():BrnResearchDiagnostics.child(board,moves[random.nextInt(n)]);
        }
        assertTrue(work.rebuilds>1);assertEquals(1,independent.rebuilds);
    }
}
