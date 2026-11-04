package com.ohinteractive.seedv6.search.exact;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;

@Timeout(30)
class P4ObservedRootTest {
    @Test void finiteWindowStartsTwoRootWorkersBeforeEitherCanFinish() {
        var evaluation=SearchEvaluation.handcrafted();var board=Board.startingPosition();var saved=board.clone();
        var entered=new CountDownLatch(2);
        int expected=ExactSearch.quiescenceResearch(ExactEvaluator.from(evaluation))
                .search(board,GameHistory.initial(board),3,()->false).score();
        try(var owner=P4ObservedRootSearch.constructionEagerRootResearch(2,evaluation,new TTable(1),FlatSearchConfig.exact(),
                id->new ExactEvaluator() {
                    final ExactEvaluator delegate=ExactEvaluator.from(evaluation); boolean first=true;
                    @Override public void initialize(long[] position) {
                        if(first) {
                            first=false;entered.countDown();
                            try { if(!entered.await(5,TimeUnit.SECONDS)) throw new IllegalStateException("First root still serialized"); }
                            catch(InterruptedException e) { Thread.currentThread().interrupt();throw new IllegalStateException(e); }
                        }
                        delegate.initialize(position);
                    }
                    @Override public void child(long[] parent,long[] child,int ply) { delegate.child(parent,child,ply); }
                    @Override public int evaluate(long[] position,int ply) { return delegate.evaluate(position,ply); }
                })) {
            var result=owner.searchWindow(new SearchRequest(board,3),-1000,1000);
            assertEquals(0,entered.getCount());assertTrue(owner.eagerRootScheduling());
            assertEquals(result.nodes(),owner.rootJobObservations().stream().mapToLong(P4ObservedRootSearch.RootJobObservation::nodes).sum());
            assertTrue(result.completed());assertEquals(expected,result.score());
            Phase2Oracle.legalPv(board,result.principalVariation());assertArrayEquals(saved,board);
            assertEquals(0,owner.activeSearchThreads());
        }
    }

    @Test void unboundedWindowsAndShallowDepthRetainOriginalSchedulingAndZeroBudgetPublishesNothing() {
        var board=Board.startingPosition();var saved=board.clone();
        try(var owner=P4ObservedRootSearch.constructionEagerRootResearch(4,SearchEvaluation.handcrafted(),new TTable(1),FlatSearchConfig.exact())) {
            for(int[] window:new int[][]{{-ExactSearch.INFINITY,ExactSearch.INFINITY},{-32,ExactSearch.INFINITY},{-ExactSearch.INFINITY,32}}) {
                assertTrue(owner.searchWindow(new SearchRequest(board,3),window[0],window[1]).completed());
                assertFalse(owner.eagerRootScheduling());
            }
            assertTrue(owner.searchWindow(new SearchRequest(board,2),-32,32).completed());
            assertFalse(owner.eagerRootScheduling());
            var control=SearchControl.controlled(0,0,-1,TimeSource.SYSTEM);
            var empty=owner.searchWindow(new SearchRequest(board,GameHistory.initial(board),3,control),-32,32);
            assertFalse(empty.completed());assertFalse(empty.hasMove());assertEquals(0,empty.nodes());
            assertEquals(0,control.nodes());assertEquals(0,owner.activeSearchThreads());assertArrayEquals(saved,board);
            owner.newGame();
            assertTrue(owner.rootJobObservations().isEmpty());
            assertTrue(owner.searchWindow(new SearchRequest(board,3),-1000,1000).completed());
            assertTrue(owner.eagerRootScheduling());
        }
    }

    @Test void realThreefoldHistoryPreventsSpeculativeRootJobs() {
        var board=Board.startingPosition();var history=GameHistory.builder(board);
        for(int i=0;i<8;i++) {
            board=ExactSearchTTableTest.play(board,new String[]{"g1f3","g8f6","f3g1","f6g8"}[i%4]);
            history.appendPosition(board);
        }
        var real=history.snapshot();assertTrue(real.isFormalThreefold(board));var saved=board.clone();
        try(var owner=P4ObservedRootSearch.constructionEagerRootResearch(4,SearchEvaluation.handcrafted(),new TTable(1),FlatSearchConfig.exact())) {
            var result=owner.searchWindow(new SearchRequest(board,real,3),-32,32);
            assertTrue(result.completed());assertEquals(0,result.score());assertFalse(result.hasMove());
            assertFalse(owner.eagerRootScheduling());assertEquals(0,result.nodes());assertTrue(owner.rootJobObservations().isEmpty());
            assertEquals(0,owner.activeSearchThreads());assertArrayEquals(saved,board);
        }
    }

    @Test void boundedIncompleteJobsAccountEveryAdmissionAndCannotLeakIntoNextRequest() {
        var board=Board.startingPosition();var saved=board.clone();
        try(var owner=P4ObservedRootSearch.constructionEagerRootResearch(4,SearchEvaluation.handcrafted(),new TTable(1),FlatSearchConfig.exact())) {
            var control=SearchControl.controlled(97,0,-1,TimeSource.SYSTEM);
            var result=owner.searchWindow(new SearchRequest(board,GameHistory.initial(board),6,control),-1000,1000);
            var facts=owner.rootJobObservations();
            assertFalse(result.completed());assertEquals(97,result.nodes());assertEquals(97,control.nodes());
            assertEquals(result.nodes(),facts.stream().mapToLong(P4ObservedRootSearch.RootJobObservation::nodes).sum());
            assertTrue(facts.stream().anyMatch(j->!j.completed()));
            for(var fact:facts) {
                assertTrue(fact.alpha()<fact.beta());assertTrue(fact.nodes()>=0);
                assertTrue(fact.worker()>=0 && fact.worker()<4);assertEquals(6,fact.depth());
                assertTrue(fact.invocation()==0 || !fact.scout());
            }
            assertArrayEquals(saved,board);assertEquals(0,owner.activeSearchThreads());
            owner.beginRequest();assertTrue(owner.rootJobObservations().isEmpty());owner.endRequest();
            assertThrows(UnsupportedOperationException.class,()->facts.clear());
        }
    }
}
