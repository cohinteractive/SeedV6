package com.ohinteractive.seedv6.search.evaluation;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.brnnext.*;
import com.ohinteractive.seedv6.core.brnpair2.ProductionModel;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.exact.SeePolicy;
import com.ohinteractive.seedv6.search.tt.TTable;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResearchBlendTest {
    @Test void calibratedCombinationKeepsSeparateOwnersAndExplicitIdentity() throws Exception {
        var random=new SplittableRandom(71);
        var weights=new double[BrnNextArchitecture.CUBIC.parameters()];
        for(int i=0;i<weights.length;i++)weights[i]=random.nextDouble(-2,2);
        var reference=SearchEvaluation.brnPair2(ProductionModel.bundled());
        var candidate=SearchEvaluation.brnCubicIncrementalResearch(new BrnNextModel(BrnNextArchitecture.CUBIC,weights));
        var a=reference.newState(128);var b=candidate.newState(128);
        for(double alpha:new double[]{.25,.5,.75}) {
            var definition=SearchEvaluation.blendResearch(reference,candidate,alpha);
            assertFalse(definition.isBrnPair2(),"Composite must not masquerade as the production reference");
            var main=definition.newState(128);var leaf=definition.newState(128);
            var board=Board.startingPosition();
            long[] moves=new long[256],scratch=new long[Board.MAX_BITBOARDS];
            for(int i=0;i<1000;i++) {
                main.initialize(board,0);a.initialize(board,0);b.initialize(board,0);
                double expected=(1-alpha)*a.evaluate(board,0)+alpha*b.evaluate(board,0);
                int score=(int)Math.copySign(Math.floor(Math.abs(expected)+.5),expected);
                assertEquals(score,main.evaluate(board,0));
                leaf.initializeFrom(board,0,main);assertEquals(score,leaf.evaluate(board,0));
                int n=Gen.genAll(board[0],board[1],board[2],board[3],(int)board[4],board[5],true,moves,scratch);
                if(n==0||i%80==79){board=Board.startingPosition();continue;}
                var child=new long[Board.MAX_BITBOARDS];
                Board.makeMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],moves[random.nextInt(n)],child);
                main.child(board,child,0);main.evaluate(child,1);
                assertEquals(score,leaf.evaluate(board,0),"Other worker must retain its own state");
                assertEquals(score,main.evaluate(board,0),"Undo must reconstruct the same score");
                board=child;
            }
            assertThrows(IllegalArgumentException.class,()->main.initializeFrom(Board.startingPosition(),0,a));
        }
        assertSame(reference,SearchEvaluation.blendResearch(reference,candidate,0));
        assertSame(candidate,SearchEvaluation.blendResearch(reference,candidate,1));
        for(double alpha:new double[]{-.01,1.01,Double.NaN,Double.POSITIVE_INFINITY})
            assertThrows(IllegalArgumentException.class,()->SearchEvaluation.blendResearch(reference,candidate,alpha));
    }
    @Test void identicalComponentSearchIsUnchanged() throws Exception {
        var reference=SearchEvaluation.brnPair2(ProductionModel.bundled());
        var blend=SearchEvaluation.blendResearch(reference,reference,.5);
        for(String fen:List.of("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "r3k2r/8/8/3pP3/8/8/8/R3K2R w KQkq d6 0 1",
                "4k3/P7/8/8/8/8/7p/4K3 w - - 0 1")) {
            var board=Board.fromFen(fen);
            try(var a=new SearchDriver(ExactSearchAdapter.seeResearch(reference,SeePolicy.QXRAY,null,new TTable(1)).withCompletedHints(true,null));
                var b=new SearchDriver(ExactSearchAdapter.seeResearch(blend,SeePolicy.QXRAY,null,new TTable(1)).withCompletedHints(true,null))) {
                var expected=a.search(new SearchRequest(board,4));var actual=b.search(new SearchRequest(board,4));
                assertTrue(expected.targetDepthCompleted());assertTrue(actual.targetDepthCompleted());
                assertEquals(expected.lastCompletedResult(),actual.lastCompletedResult());
            }
        }
    }
}
