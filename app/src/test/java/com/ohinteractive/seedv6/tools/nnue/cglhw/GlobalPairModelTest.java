package com.ohinteractive.seedv6.tools.nnue.cglhw;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.training.selfplay.HeadlessGame;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GlobalPairModelTest {
    @Test void twoMomentsAgreeWithRefreshAcrossChainedAndSpecialMoves() {
        for(boolean interactions:new boolean[]{false,true}) {
            var w=new GlobalPairModel(41001,interactions).worker(3);
            for(String fen:new String[]{Board.FEN_STARTING_POSITION,"r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1","4k3/P7/8/8/8/8/7p/4K3 w - - 0 1"}) {
                var p=Board.fromFen(fen);w.refresh(p,0);double original=w.raw(p,0);
                for(long move:new HeadlessGame(p,100).legalMoves()) {
                    var c=new long[6];Board.makeMoveInto(p[0],p[1],p[2],p[3],(int)p[4],p[5],move,c);
                    w.transition(p,c,0,1);w.refresh(c,2);assertEquals(w.raw(c,2),w.raw(c,1),1e-12);assertEquals(original,w.raw(p,0),0);
                }
            }
            var game=new HeadlessGame(Board.startingPosition(),200);var rng=new java.util.SplittableRandom(9091);int slot=0;
            w.refresh(game.boardSnapshot(),0);
            while(game.active()) {
                var p=game.boardSnapshot();var moves=game.legalMoves();game.play(moves[rng.nextInt(moves.length)]);var c=game.boardSnapshot();
                w.transition(p,c,slot,slot^1);slot^=1;w.refresh(c,2);assertEquals(w.raw(c,2),w.raw(c,slot),1e-12);
                var flip=c.clone();flip[4]^=1;assertEquals(-w.raw(c,slot),w.raw(flip,slot),0);
                var inverted=BootstrapAudit.symmetry(c,true);w.refresh(inverted,2);assertEquals(w.raw(c,slot),w.raw(inverted,2),1e-12);
            }
        }
    }
}
