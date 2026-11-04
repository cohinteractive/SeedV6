package com.ohinteractive.seedv6.search.exact;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.driver.BaselineEngine;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(90)
class P4WholeRootSharingTest {
    private static final FlatSearchConfig CONFIG = BaselineEngine.configuration(
            4,1,BaselineEngine.SearchProfile.TC_P1).mechanisms();

    // Quiescent read-only inspection checks state content and distinct ownership,
    // without adding reflection or diagnostic branches to the production hot path.
    static Object field(Object owner, String name) throws Exception {
        var f=owner.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(owner);
    }
    static ExactSearch[] searches(ParallelSearch owner) throws Exception {
        Object[] workers=(Object[])field(owner,"workers");
        var result=new ExactSearch[workers.length];
        for(int i=0;i<result.length;i++) result[i]=(ExactSearch)field(workers[i],"search");
        return result;
    }

    @Test void resolvedWinnerTransfersPrivateLearningAndBoundsNeverDo() throws Exception {
        var evaluation=SearchEvaluation.handcrafted(); var board=Board.startingPosition();
        var history=GameHistory.initial(board);
        try(var owner=ParallelSearch.constructionWholeRootSharingResearch(4,evaluation,new TTable(1),CONFIG)) {
            owner.beginRequest();
            try {
                var result=owner.search(new SearchRequest(board,history,4));
                assertTrue(result.completed()); assertEquals(3,owner.completedLearningTransfers());
                var all=searches(owner);
                var q=(int[])field(all[0],"quietHistory");
                assertTrue(Arrays.stream(q).anyMatch(v->v!=0));
                var first=(FlatSearchState)field(all[0],"constructionState");
                for(int i=1;i<all.length;i++) {
                    var other=(int[])field(all[i],"quietHistory");
                    assertNotSame(q,other); assertArrayEquals(q,other);
                    var next=(FlatSearchState)field(all[i],"constructionState");
                    assertNotSame(first.captures,next.captures); assertArrayEquals(first.captures,next.captures);
                    assertNotSame(first.continuation,next.continuation); assertArrayEquals(first.continuation,next.continuation);
                    assertNotSame(first.killers,next.killers); assertArrayEquals(first.killers,next.killers);
                    assertNotSame(first.counters,next.counters); assertArrayEquals(first.counters,next.counters);
                    var order=field(all[i],"iterationReuse");
                    assertEquals(4,field(order,"completedDepth"));
                }
                var bound=owner.searchWindow(new SearchRequest(board,history,4),-20000,-19999);
                assertTrue(bound.completed()); assertTrue(bound.score()>=-19999);
                assertEquals(0,owner.completedLearningTransfers());
                assertEquals(0,owner.activeSearchThreads());
            } finally { owner.endRequest(); }
            for(var worker:searches(owner)) {
                assertTrue(Arrays.stream((int[])field(worker,"quietHistory")).allMatch(v->v==0));
                assertTrue(Arrays.stream(((FlatSearchState)field(worker,"constructionState")).captures).allMatch(v->v==0));
            }
        }
    }

    @Test void copiedLearningChangesRecipientEffortWithoutCopyingSearchScratch() throws Exception {
        var evaluation=SearchEvaluation.handcrafted();
        var source=ExactSearch.constructionResearch(ExactEvaluator.from(evaluation),new TTable(1),CONFIG);
        var target=ExactSearch.constructionResearch(ExactEvaluator.from(evaluation),new TTable(1),CONFIG);
        assertThrows(IllegalStateException.class,()->target.copyCompletedLearningFrom(source));
        source.beginRequest(); target.beginRequest();
        try {
            var board=Board.startingPosition();
            var result=source.search(board,GameHistory.initial(board),4,()->false);
            assertTrue(result.completed());
            var q=(int[])field(source,"quietHistory");
            var targetQ=(int[])field(target,"quietHistory");
            var state=(FlatSearchState)field(source,"constructionState");
            var targetState=(FlatSearchState)field(target,"constructionState");
            long witness=ExhaustiveOracle.legalMoves(board)[0];
            // Controlled bounded heuristic evidence makes the consumer witness
            // independent of incidental strength/search outcomes.
            q[QuietHistory.index(witness)]=128;
            state.pathMoves[1]=123; targetState.pathMoves[1]=456;
            state.credits[1]=1; targetState.credits[1]=0;
            int before=targetState.historyReduction(3,8,0,witness,targetQ);
            target.copyCompletedLearningFrom(source);
            assertEquals(before-2,targetState.historyReduction(3,8,0,witness,targetQ));
            assertEquals(456,targetState.pathMoves[1]); assertEquals(0,targetState.credits[1]);
            targetQ[QuietHistory.index(witness)]=0;
            assertEquals(128,q[QuietHistory.index(witness)]);
        } finally { source.endRequest(); target.endRequest(); }
    }

    @Test void unsupportedCorrectionSharingIsRejectedBeforeWorkersAreAllocated() {
        assertThrows(IllegalArgumentException.class,()->ParallelSearch.constructionWholeRootSharingResearch(
                4,SearchEvaluation.handcrafted(),new TTable(1),CONFIG.with(FlatSearchConfig.Feature.EVAL_CORRECTION,true)));
    }
}
