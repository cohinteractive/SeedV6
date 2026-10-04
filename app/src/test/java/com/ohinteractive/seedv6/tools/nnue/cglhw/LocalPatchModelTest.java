package com.ohinteractive.seedv6.tools.nnue.cglhw;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.training.selfplay.HeadlessGame;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LocalPatchModelTest {
    private static void check(IncrementalModel.Worker w,long[] p,long[] c) {
        w.refresh(p,0);double original=w.raw(p,0);w.transition(p,c,0,1);w.refresh(c,2);
        assertEquals(w.raw(c,2),w.raw(c,1),1e-12);assertEquals(original,w.raw(p,0),0);
        var flip=p.clone();flip[4]^=1;assertEquals(-original,w.raw(flip,0),0);
        var swapped=BootstrapAudit.symmetry(p,true);w.refresh(swapped,2);assertEquals(original,w.raw(swapped,2),1e-12);
    }
    @Test void localUpdatesMatchFullRebuildForOrdinarySpecialAndChainedMoves() {
        for(int radius:new int[]{1,2}) {
            var w=new LocalPatchModel(41001,radius).worker(3);
            for(var edge:BootstrapAudit.corpus(4,80))check(w,edge.parent(),edge.child());
            for(String fen:new String[]{"r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1","4k3/P7/8/8/8/8/7p/4K3 w - - 0 1"}) {
                var p=Board.fromFen(fen);
                for(long move:new HeadlessGame(p,100).legalMoves()) {
                    var c=new long[6];Board.makeMoveInto(p[0],p[1],p[2],p[3],(int)p[4],p[5],move,c);check(w,p,c);
                }
            }
            var game=new HeadlessGame(Board.startingPosition(),160);var random=new java.util.SplittableRandom(901);
            w.refresh(game.boardSnapshot(),0);int slot=0;
            while(game.active()) {
                var p=game.boardSnapshot();var moves=game.legalMoves();game.play(moves[random.nextInt(moves.length)]);var c=game.boardSnapshot();
                w.transition(p,c,slot,slot^1);slot^=1;w.refresh(c,2);assertEquals(w.raw(c,2),w.raw(c,slot),1e-12);
            }
        }
    }
    @Test void zeroReadoutRemainsExactlyZeroForIndependentInitializations() {
        for(long seed:new long[]{41001,41002,41003}) {
            var w=new HalfKpHead(seed,false,false,true).worker(2);
            for(var edge:BootstrapAudit.corpus(1,32)) {
                w.refresh(edge.parent(),0);w.transition(edge.parent(),edge.child(),0,1);
                assertEquals(0,w.raw(edge.parent(),0),0);assertEquals(0,w.evaluate(edge.child(),1));
            }
        }
    }
}
