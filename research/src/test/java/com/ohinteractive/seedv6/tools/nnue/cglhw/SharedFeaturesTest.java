package com.ohinteractive.seedv6.tools.nnue.cglhw;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.training.selfplay.HeadlessGame;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SharedFeaturesTest {
    private static void check(IncrementalModel.Worker work,long[] p,long[] c) {
        var original=p.clone();work.refresh(p,0);work.transition(p,c,0,1);work.refresh(c,2);
        assertEquals(work.raw(c,2),work.raw(c,1),1e-8);
        double value=work.raw(p,0);var flip=p.clone();flip[4]^=1;
        assertEquals(-value,work.raw(flip,0),0);assertArrayEquals(original,p);
        work.refresh(BootstrapAudit.symmetry(p,true),2);
        assertEquals(value,work.raw(BootstrapAudit.symmetry(p,true),2),1e-8);
    }
    @Test void sharedEmbeddingsRemainIncrementalAcrossLegalTrajectoriesAndSpecialMoves() {
        for(var kind:SharedFeatures.Kind.values()) {
            var work=new SharedFeatures(41001,kind).worker(3);
            for(var edge:BootstrapAudit.corpus(4,80))check(work,edge.parent(),edge.child());
            for(String fen:new String[]{"r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1","4k3/P7/8/8/8/8/7p/4K3 w - - 0 1"}) {
                var p=Board.fromFen(fen);var game=new HeadlessGame(p,100);
                for(long move:game.legalMoves()) {
                    var c=new long[6];Board.makeMoveInto(p[0],p[1],p[2],p[3],(int)p[4],p[5],move,c);check(work,p,c);
                }
            }
        }
    }
    @Test void countFeaturesDoNotInjectFloatingMovePreferenceAndMirrorFactorsTieReflections() {
        var count=new SharedFeatures(41002,SharedFeatures.Kind.COUNTS).worker(2);
        var mirror=new SharedFeatures(41002,SharedFeatures.Kind.MIRROR_SMOOTH).worker(2);
        for(var edge:BootstrapAudit.corpus(4,80)) {
            var p=edge.parent();var c=edge.child();
            if(edge.kind().equals("quiet")) {
                // This bounded opening corpus has no promotions; verify piece count invariance.
                count.refresh(p,0);count.transition(p,c,0,1);
                assertEquals(-count.raw(p,0),count.raw(c,1),0);
            }
            if((p[4]&(15L<<1))==0) {
                var reflected=BootstrapAudit.symmetry(p,false);mirror.refresh(p,0);mirror.refresh(reflected,1);
                assertEquals(mirror.raw(p,0),mirror.raw(reflected,1),1e-8);
            }
        }
    }
    @Test void balancedCountsAreExactlyZeroAndCountChangesArePathIndependent() {
        for(long seed=41001;seed<=41006;seed++) {
            var counts=new SharedFeatures(seed,SharedFeatures.Kind.COUNTS).worker(3);
            var root=Board.startingPosition();counts.refresh(root,0);
            assertEquals(0,counts.raw(root,0),0);assertEquals(0,counts.evaluate(root,0));
            for(var e:BootstrapAudit.corpus(2,80)) {
                counts.refresh(e.parent(),0);counts.transition(e.parent(),e.child(),0,1);counts.refresh(e.child(),2);
                assertEquals(counts.raw(e.child(),2),counts.raw(e.child(),1),0);
                assertEquals(counts.evaluate(e.child(),2),counts.evaluate(e.child(),1));
            }
        }
    }
}
