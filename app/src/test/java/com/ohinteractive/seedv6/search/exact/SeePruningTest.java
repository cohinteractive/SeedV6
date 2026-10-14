package com.ohinteractive.seedv6.search.exact;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;
import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.*;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;

@Timeout(120)
class SeePruningTest {
    static final SearchEvaluation EVAL = SearchEvaluation.handcraftedIsolation();
    static final String MIDDLE = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";

    @Test void qPoliciesMatchIndependentFullLegalEnumeration() {
        int comparisons = 0;
        for(var f : QuiescenceSeeCorpus.FIXTURES) for(var policy : new SeePolicy[]{SeePolicy.OFF,SeePolicy.QSAFE,SeePolicy.QDEEP,SeePolicy.ORDER}) {
            var board=Board.fromFen(f.fen()); var game=GameHistory.initial(board);
            for(int depth=0;depth<=1;depth++) {
                var oracle=new Oracle(policy);
                int expected=oracle.score(board,new SearchLineHistory(game),depth,0,0);
                var search=ExactSearch.quiescencePvsResearch(ExactEvaluator.from(EVAL),new TTable(1),ExactSearch.QSEARCH_BASELINE)
                        .withSeeResearch(policy,null);
                var result=search.search(board,game,depth,()->search.visitedNodes()>1_000_000);
                assertTrue(result.completed(),f.name()+policy);
                assertEquals(expected,result.score(),f.name()+policy+depth);
                Phase2QuiescenceTest.legalPv(board,result.principalVariation());
                comparisons++;
            }
        }
        System.out.println("SR024 independent qsearch oracle comparisons="+comparisons);
    }

    @Test void clearanceWitnessDistinguishesFirstLayerProtectionFromCheckSafety() {
        var board=Board.fromFen("b1q4k/8/8/2p5/N7/8/8/R3K3 w - - 0 1");
        int baseline=ExactSearch.quiescenceResearch(ExactEvaluator.from(EVAL)).search(board,0).score();
        for(var policy:new SeePolicy[]{SeePolicy.QSAFE,SeePolicy.QDEEP}) {
            var d=new SeePolicy.Diagnostics();
            var result=ExactSearch.quiescencePvsResearch(ExactEvaluator.from(EVAL),null,ExactSearch.QSEARCH_BASELINE)
                    .withSeeResearch(policy,d).search(board,0);
            if(policy==SeePolicy.QSAFE) { assertTrue(result.score()<baseline); assertTrue(d.qOmitted>0); }
            else assertEquals(baseline,result.score());
        }
    }

    @Test void omissionsRespectLegalTacticalAndActuallySearchedBoundaries() {
        for(var policy:new SeePolicy[]{SeePolicy.QSAFE,SeePolicy.QDEEP,SeePolicy.MAIN}) {
            var d=new SeePolicy.Diagnostics() {
                @Override public void omitted(long[] b,long[] c,long move,int ply,int depth,int qply,
                        int searched,int quiets,int alpha,int beta) {
                    assertFalse(QuiescenceOracle.inCheck(b));
                    assertFalse(QuiescenceOracle.inCheck(c));
                    assertFalse(QuiescenceOracle.promotion(move));
                    assertNotEquals(0,(move>>>Board.TARGET_PIECE_SHIFT)&Board.PIECE_BITS);
                    assertTrue(See.evaluate(b,move)<0);
                    assertTrue(Arrays.stream(ExhaustiveOracle.legalMoves(b)).anyMatch(m->m==move));
                    if(qply<0) { assertTrue(ply>0 && searched>0 && quiets<=searched); assertTrue(depth>=1 && depth<=3); assertEquals(alpha+1,beta); assertTrue(alpha>=0); }
                    if(policy==SeePolicy.QDEEP) assertTrue(qply>0);
                }
            };
            var table=new TTable(1);
            var search=ExactSearch.iterationReuseResearch(ExactEvaluator.from(EVAL),table,IterationReuse.EFFORT,true,null)
                    .withSeeResearch(policy,d);
            var board=Board.fromFen(MIDDLE);
            var result=search.search(board,GameHistory.initial(board),5,()->search.visitedNodes()>1_000_000);
            assertTrue(result.completed());assertTrue(result.selective());
            assertTrue(d.qOmitted+d.mainOmitted>0,policy.toString());
            Phase2QuiescenceTest.legalPv(board,result.principalVariation());
            // The affected root cannot publish ordinary nominal proof.
            var entry=new TTable.TEntry();
            assertFalse(table.probe(SearchKey.key(board,SearchKey.rootHistory(board,GameHistory.initial(board))),entry));
        }
    }

    @Test void offIsIdenticalAndCancellationLeavesOwnerReusable() {
        var board=Board.fromFen(MIDDLE);var game=GameHistory.initial(board);
        try(var old=new SearchDriver(ExactSearchAdapter.iterationReuseResearch(EVAL,IterationReuse.EFFORT,null));
            var off=new SearchDriver(ExactSearchAdapter.seeResearch(EVAL,SeePolicy.OFF,null))) {
            var a=old.search(new SearchRequest(board,game,5));var b=off.search(new SearchRequest(board,game,5));
            assertEquals(a.nodes(),b.nodes());assertEquals(a.lastCompletedResult().score(),b.lastCompletedResult().score());
            assertArrayEquals(a.lastCompletedResult().principalVariation(),b.lastCompletedResult().principalVariation());
        }
        for(var policy:SeePolicy.values()) try(var driver=new SearchDriver(ExactSearchAdapter.seeResearch(EVAL,policy,null))) {
            var limited=driver.search(new SearchRequest(board,game,12,SearchControl.controlled(1000,0,-1,TimeSource.SYSTEM)));
            assertEquals(1000,limited.nodes());
            if(limited.lastCompletedResult()!=null)
                Phase2QuiescenceTest.legalPv(board,limited.lastCompletedResult().principalVariation());
            driver.newGame();assertTrue(driver.search(new SearchRequest(board,game,3)).targetDepthCompleted());
        }
    }

    @Test void allSupportedRootReuseWorkersExecutePrivateSeePolicyAndDrain() {
        var board=Board.fromFen(MIDDLE);var game=GameHistory.initial(board);
        for(int workers=1;workers<=16;workers++) {
            var sinks=new SeePolicy.Diagnostics[workers];
            try(var engine=new ParallelSearch(workers,EVAL,new TTable(1),ConcurrencyPolicy.ROOT_REUSE)
                    .withSeeResearch(SeePolicy.QSAFE,i->sinks[i]=new SeePolicy.Diagnostics());
                var driver=new SearchDriver(engine)) {
                var result=driver.search(new SearchRequest(board,game,6,SearchControl.controlled(500_000,0,-1,TimeSource.SYSTEM)));
                assertNotNull(result.lastCompletedResult());assertEquals(0,engine.activeSearchThreads());
                Phase2QuiescenceTest.legalPv(board,result.lastCompletedResult().principalVariation());
                assertEquals(workers,new HashSet<>(Arrays.asList(sinks)).size());
                assertTrue(Arrays.stream(sinks).mapToLong(d->d.qOmitted).sum()>0);
                // A short dynamic cohort need not schedule a job on every helper.
                // Per-helper deep execution is measured separately; do not impose deterministic scheduling.
                if(workers>1)assertTrue(Arrays.stream(sinks).skip(1).mapToLong(d->d.qOmitted).sum()>0);
                driver.newGame();assertTrue(driver.search(new SearchRequest(board,game,2)).targetDepthCompleted());
            }
        }
    }

    static class Oracle {
        final SeePolicy policy; long nodes;
        Oracle(SeePolicy policy) { this.policy=policy; }
        int score(long[] b,SearchLineHistory history,int depth,int ply,int qply) {
            if(++nodes>2_000_000 || ply>64) throw new AssertionError("Independent oracle fixture guard");
            long[] moves=ExhaustiveOracle.legalMoves(b);boolean check=QuiescenceOracle.inCheck(b);
            if(moves.length==0)return check ? -ExactSearch.MATE_SCORE+ply:0;
            if(DrawAdjudicator.adjudicateNonTerminal(b,history)!=DrawAdjudicator.RuleDraw.NONE)return 0;
            boolean leaf=depth<=0;int best=leaf&&!check ? Eval.evaluate(b):-ExactSearch.INFINITY;
            for(long move:moves) {
                if(leaf&&!check&&!QuiescenceOracle.tactical(b,move))continue;
                var child=ExhaustiveOracle.child(b,move);
                if(leaf&&!check&&policy.prunesQuiescence(qply)&&!QuiescenceOracle.promotion(move)
                        && ((move>>>Board.TARGET_PIECE_SHIFT)&Board.PIECE_BITS)!=0
                        && See.evaluate(b,move)<0&&!QuiescenceOracle.inCheck(child))continue;
                history.pushRealPosition(child);
                try{best=Math.max(best,-score(child,history,depth-1,ply+1,leaf?qply+1:0));}
                finally{history.popRealPosition();}
            }
            return best;
        }
    }
}
