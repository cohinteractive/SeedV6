package com.ohinteractive.seedv6.search.exact;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.rules.*;
import com.ohinteractive.seedv6.search.tt.TTable;
import static com.ohinteractive.seedv6.search.exact.FlatSearchConfig.Feature.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class ConstructionOrderingTest {
    @Test void combinedOrderingMatchesIndependentUnprunedReferenceAndEachRemoval() {
        ExactEvaluator hce=(b,p)->Eval.evaluate(b);
        var config=FlatSearchConfig.ordered().with(QXRAY,false).with(LMR,false).with(LMP,false);
        var variants=new ArrayList<FlatSearchConfig>();variants.add(config);
        for(var f:List.of(CAPTURE_HISTORY,CONTINUATION,KILLERS,COUNTERMOVES,NOMINAL_HISTORY,BAD_TACTICAL_LAST))
            variants.add(config.with(f,false));
        for(var fixture:QuiescenceSeeCorpus.FIXTURES) {
            var b=Board.fromFen(fixture.fen());int expected=ExactSearch.quiescenceResearch(hce).search(b,2).score();
            for(var variant:variants) {
                var r=ExactSearch.constructionResearch(hce,new TTable(1),variant).search(b,2);
                assertTrue(r.completed());assertEquals(expected,r.score(),fixture.name()+variant.identity());
                Phase2Oracle.legalPv(b,r.principalVariation());
            }
        }
    }

    @Test void nominalLearningExcludesReducedAndUnsearchedQuiets() {
        var board=Board.startingPosition();var moves=new long[512];
        int count=Gen.genAll(board[0],board[1],board[2],board[3],(int)board[Board.STATUS],board[Board.KEY],true,moves,new long[Board.MAX_BITBOARDS]);
        assertTrue(count>=4);
        var state=new FlatSearchState(FlatSearchConfig.ordered());var history=new int[QuietHistory.SIZE];
        state.pathMoves[1]=moves[3];
        state.nominal.searched(1,0,false);state.nominal.searched(1,1,true);state.nominal.searched(1,2,true);
        state.cutoff(history,moves,2,-1,4,1);
        assertEquals(0,history[QuietHistory.index(moves[0])]);
        assertTrue(history[QuietHistory.index(moves[1])]<0);assertTrue(history[QuietHistory.index(moves[2])]>0);
        assertEquals(0,history[QuietHistory.index(moves[3])]);
        int context=ContinuationHistory.context(moves[3]);
        assertEquals(0,state.continuation[ContinuationHistory.index(context,moves[0])]);
        assertTrue(state.continuation[ContinuationHistory.index(context,moves[1])]<0);
        assertEquals(moves[2],state.killers[2]);assertEquals(moves[2],state.counters[Countermoves.context(moves[3])]);
        assertEquals(1,state.filtered);
    }

    @Test void richerOrderingExecutesAndCancellationClearsPrivateMemory() throws Exception {
        var e=P3InformationBaselineTest.evaluation();
        var b=Board.fromFen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1");
        var search=ExactSearch.constructionResearch(ExactEvaluator.from(e),new TTable(1),FlatSearchConfig.ordered());
        var r=search.search(b,4);assertTrue(r.completed());Phase2Oracle.legalPv(b,r.principalVariation());
        var state=(FlatSearchState)Policy4ProductionTest.field(search,"constructionState");
        assertTrue(state.captureUpdates>0);assertTrue(state.quietUpdates>0);assertTrue(state.orderingChanges>0);
        assertFalse(search.search(b,GameHistory.initial(b),8,()->search.visitedNodes()>200).completed());
        assertTrue(Arrays.stream(state.captures).allMatch(n->n==0));
        assertTrue(Arrays.stream(state.killers).allMatch(n->n==0));
        for(short value:state.continuation) assertEquals(0,value);
        assertTrue(search.search(b,2).completed());
        assertThrows(IllegalStateException.class,()->search.withFlatSearchResearch(false));
        assertThrows(IllegalStateException.class,()->search.withCompletedHints(false,null));
        assertThrows(IllegalStateException.class,()->search.withSeeResearch(SeePolicy.OFF,null));
    }
}
