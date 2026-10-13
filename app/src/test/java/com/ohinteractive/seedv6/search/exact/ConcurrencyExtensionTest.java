package com.ohinteractive.seedv6.search.exact;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;

@Timeout(90)
class ConcurrencyExtensionTest {
    static final SearchEvaluation EVAL = SearchEvaluation.handcraftedIsolation();

    @Test void oneWorkerCumulativeHasExactAcceptedCompositionAcrossRequests() {
        var board=Board.startingPosition(); var history=GameHistory.initial(board);
        try(var expected=new SearchDriver(ExactSearchAdapter.iterationReuseResearch(EVAL,IterationReuse.EFFORT,null));
            var actual=new SearchDriver(new ParallelSearch(1,EVAL,new TTable(),ConcurrencyPolicy.COOPERATIVE))) {
            for(int i=0;i<3;i++) {
                var a=expected.search(new SearchRequest(board,history,5));
                var b=actual.search(new SearchRequest(board,history,5));
                assertEquals(a.nodes(),b.nodes());
                assertEquals(a.lastCompletedResult().score(),b.lastCompletedResult().score());
                assertArrayEquals(a.lastCompletedResult().principalVariation(),b.lastCompletedResult().principalVariation());
            }
        }
    }

    @Test void allPoliciesCountEveryAdmissionIncludingQsearchAndAbortedHelpers() {
        var board=Board.startingPosition();var history=GameHistory.initial(board);
        for(var policy:ConcurrencyPolicy.values()) if(policy!=ConcurrencyPolicy.OFF)
            for(int workers:new int[]{2,4,6}) try(var engine=new ParallelSearch(workers,EVAL,new TTable(1),policy);
                var driver=new SearchDriver(engine)) {
                for(long budget:new long[]{1,19,20,100,1000,10000}) {
                    var control=SearchControl.controlled(budget,0,-1,TimeSource.SYSTEM);
                    var result=driver.search(new SearchRequest(board,history,10,SearchObserver.NONE,control,true));
                    assertEquals(budget,result.nodes(),policy.toString());
                    assertEquals(control.nodes(),result.nodes());
                    assertEquals(result.nodes(),result.diagnostics().totalEnteredNodes());
                    assertEquals(0,engine.activeSearchThreads());
                    if(result.lastCompletedResult()!=null) Phase2QuiescenceTest.legalPv(board,result.lastCompletedResult().principalVariation());
                    long used=Arrays.stream(engine.workerWork()).mapToLong(ParallelSearch.WorkerWork::nodes).sum();
                    assertTrue(used<=result.nodes());
                    assertTrue(Arrays.stream(engine.workerWork()).filter(ParallelSearch.WorkerWork::winner).count()<=1);
                }
            }
    }

    @Test void transferredOrderingUsesCompletedFactsAndIsClearedAtRequestBoundary() {
        var board=Board.startingPosition();var history=GameHistory.initial(board);
        var source=new IterationReuse(IterationReuse.EFFORT,null);
        var target=new IterationReuse(IterationReuse.EFFORT,null);
        long[] moves=ExhaustiveOracle.legalMoves(board);
        source.prepare(board,history,1,true,-ExactSearch.INFINITY,ExactSearch.INFINITY);
        source.rootResult(moves[0],0,10);source.rootResult(moves[1],0,100);
        source.complete(1,new long[]{moves[0]},1);
        target.copyCompletedFrom(source);
        target.prepare(board,history,2,true,-ExactSearch.INFINITY,ExactSearch.INFINITY);
        assertEquals(moves[0],target.hint(0,0));assertTrue(target.rootRanking());
        source.clear(); // target arrays are independent of its source.
        assertEquals(moves[0],target.hint(0,0));
        target.clear();target.prepare(board,history,2,true,-ExactSearch.INFINITY,ExactSearch.INFINITY);
        assertEquals(0,target.hint(0,0));assertFalse(target.rootRanking());
    }

    @Test void unprunedSharedHorizonMatchesIndependentQsearchWithoutOrdinaryRootProof() {
        for(String fen:new String[]{"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1","7k/8/8/3pP3/8/8/8/K7 w - d6 0 1",
                "7k/1P6/8/8/8/8/6p1/K7 w - - 0 1","7k/6Q1/6K1/8/8/8/8/8 b - - 100 1"}) {
            var board=Board.fromFen(fen);var history=GameHistory.initial(board);
            // Nominal unpruned reference uses ordered alpha-beta with the same q horizon.
            var reference=ExactSearch.quiescenceResearch(ExactEvaluator.from(EVAL));
            int expected=reference.search(board,history,3,()->false).score();
            var table=new TTable(1);table.advanceGeneration();
            var worker=ExactSearch.sharedCumulativeWorker(ExactEvaluator.from(EVAL),table,new MoveReservations(),false);
            worker.beginSharedRequest(1);
            var actual=worker.search(board,history,3,()->false);worker.endRequest();
            assertTrue(actual.completed());assertEquals(expected,actual.score());
            Phase2QuiescenceTest.legalPv(board,actual.principalVariation());
            if(actual.selective())assertFalse(table.probe(SearchKey.key(board,SearchKey.rootHistory(board,history)),new TTable.TEntry()));
        }
    }

    @Test void restrictedJobsReadOnlyCompletedRootHintsAndCannotReplaceThem() {
        var board=Board.startingPosition();var history=GameHistory.initial(board);
        long[] moves=ExhaustiveOracle.legalMoves(board);
        var completed=new IterationReuse(IterationReuse.EFFORT,null);
        completed.prepare(board,history,1,true,-ExactSearch.INFINITY,ExactSearch.INFINITY);
        completed.rootResult(moves[0],0,10);completed.complete(1,new long[]{moves[0]},1);
        var table=new TTable(1);table.advanceGeneration();
        var worker=ExactSearch.sharedCumulativeWorker(ExactEvaluator.from(EVAL),table,null,false);
        worker.beginSharedRequest(1);worker.prepareRootIteration(completed);
        var first=worker.searchRootMove(board,history,2,moves[0],-ExactSearch.INFINITY,ExactSearch.INFINITY,false,()->false);
        assertTrue(first.completed());worker.seedRootHistoryFrom(worker);
        var scout=worker.searchRootMove(board,history,2,moves[1],first.score(),first.score()+1,true,()->false);
        assertTrue(scout.completed());
        var receiver=ExactSearch.sharedCumulativeWorker(ExactEvaluator.from(EVAL),new TTable(1),null,false);
        receiver.copyCompletedOrderingFrom(worker);
        // An ordinary full depth-two search must still consume the genuine depth-one
        // observation; the last restricted depth-two job cannot become its predecessor.
        var field=assertDoesNotThrow(()->ExactSearch.class.getDeclaredField("iterationReuse"));
        field.setAccessible(true);
        var observation=(IterationReuse)assertDoesNotThrow(()->field.get(receiver));
        observation.prepare(board,history,2,true,-ExactSearch.INFINITY,ExactSearch.INFINITY);
        assertEquals(moves[0],observation.hint(0,0));assertTrue(observation.rootRanking());
        worker.endRequest();worker.beginSharedRequest(2);receiver.copyCompletedOrderingFrom(worker);
        observation.prepare(board,history,2,true,-ExactSearch.INFINITY,ExactSearch.INFINITY);
        assertEquals(0,observation.hint(0,0));worker.endRequest();
    }

    @Test void cancellationAndHelperFailureDrainBeforeReuseForEveryPolicy() throws Exception {
        var board=Board.startingPosition();var history=GameHistory.initial(board);
        for(var policy:ConcurrencyPolicy.values()) if(policy!=ConcurrencyPolicy.OFF) {
            var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var once=new AtomicBoolean();
            TimeSource clock=()->{
                if(Thread.currentThread().getName().equals("seedv6-search-helper")&&once.compareAndSet(false,true)) {
                    entered.countDown();try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}
                }return 0;
            };
            try(var engine=new ParallelSearch(4,EVAL,new TTable(1),policy);var driver=new SearchDriver(engine)) {
                var control=SearchControl.controlled(-1,0,Long.MAX_VALUE,clock);var failure=new AtomicReference<Throwable>();
                var owner=new Thread(()->{try{driver.search(new SearchRequest(board,history,9,control));}catch(Throwable e){failure.set(e);}});
                owner.start();
                try{assertTrue(entered.await(10,TimeUnit.SECONDS));control.request(SearchTermination.STOPPED);assertTrue(owner.isAlive());}
                finally{release.countDown();owner.join(10000);}
                assertFalse(owner.isAlive());assertNull(failure.get());assertEquals(0,engine.activeSearchThreads());
                var injected=new AtomicBoolean();TimeSource broken=()->{
                    if(Thread.currentThread().getName().equals("seedv6-search-helper")&&injected.compareAndSet(false,true))throw new IllegalStateException("injected");return 0;
                };
                assertThrows(IllegalStateException.class,()->driver.search(new SearchRequest(board,history,9,SearchControl.controlled(-1,0,Long.MAX_VALUE,broken))));
                assertTrue(injected.get());driver.newGame();
                assertTrue(driver.search(new SearchRequest(board,history,4)).targetDepthCompleted());
            }
        }
    }

    @Test void restrictedRootJobsPreserveIndependentValueHistoryMatePlyAndExcludeRootProof() {
        for(String fen:new String[]{"4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 2",
                "7k/8/5KQ1/8/8/8/8/8 w - - 0 1","r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1"}) {
            var board=Board.fromFen(fen);var history=GameHistory.initial(board);
            int expected=ExactSearch.quiescenceResearch(ExactEvaluator.from(EVAL)).search(board,history,3,()->false).score();
            var table=new TTable(1);table.advanceGeneration();
            long rootKey=SearchKey.key(board,SearchKey.rootHistory(board,history));
            // Deliberately inapplicable unrestricted-root evidence must not answer a restricted job.
            table.save(rootKey,3,TTable.TYPE_EXACT,12345,ExhaustiveOracle.legalMoves(board)[0]);
            var worker=ExactSearch.sharedCumulativeWorker(ExactEvaluator.from(EVAL),table,null,false);
            worker.beginSharedRequest(1);int best=-ExactSearch.INFINITY;
            for(long move:ExhaustiveOracle.legalMoves(board)) {
                var r=worker.searchRootMove(board,history,3,move,-ExactSearch.INFINITY,ExactSearch.INFINITY,false,()->false);
                assertTrue(r.completed());assertEquals(move,r.bestMove());
                Phase2QuiescenceTest.legalPv(board,r.principalVariation());best=Math.max(best,r.score());
            }
            assertEquals(expected,best);worker.endRequest();
            var entry=new TTable.TEntry();
            if(table.probe(rootKey,entry))assertEquals(12345,(int)(entry.data>>32),"No restricted-root score publication");
        }
    }

    @Test void independentOwnersAndGenerationWrapKeepPrivateRequestState() throws Exception {
        var pool=Executors.newFixedThreadPool(2);
        try {
            var futures=new ArrayList<Future<?>>();
            for(var policy:new ConcurrencyPolicy[]{ConcurrencyPolicy.COOPERATIVE,ConcurrencyPolicy.ROOT_REUSE})
                futures.add(pool.submit(()->{
                    var board=Board.startingPosition();var history=GameHistory.initial(board);
                    try(var engine=new ParallelSearch(4,EVAL,new TTable(1),policy);var driver=new SearchDriver(engine)) {
                        for(int i=0;i<270;i++) {
                            var control=SearchControl.controlled(2000,0,-1,TimeSource.SYSTEM);
                            var r=driver.search(new SearchRequest(board,history,5,SearchObserver.NONE,control,true));
                            assertEquals(control.nodes(),r.nodes());assertEquals(2000,r.nodes());
                            assertEquals(0,engine.activeSearchThreads());
                            assertNotNull(r.lastCompletedResult());Phase2QuiescenceTest.legalPv(board,r.lastCompletedResult().principalVariation());
                            if(i%19==0)driver.newGame();
                        }
                    }
                }));
            for(var future:futures)future.get();
        }finally{pool.shutdownNow();assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS));}
    }

    @Test void completeRootCohortsAgreeWithIndependentUnprunedOracleInProtectedDomain() {
        // At depth three no non-root invocation reaches LMR depth three.
        // At most six pieces excludes WINDOW LMP. Qsearch remains the same horizon.
        for(String fen:new String[]{"4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 2",
                "7k/8/5KQ1/8/8/8/8/8 w - - 0 1","r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1"}) {
            var board=Board.fromFen(fen);var history=GameHistory.initial(board);
            int expected=ExactSearch.quiescenceResearch(ExactEvaluator.from(EVAL)).search(board,history,3,()->false).score();
            for(var policy:new ConcurrencyPolicy[]{ConcurrencyPolicy.ROOT_SPLIT,ConcurrencyPolicy.ROOT_FULL,ConcurrencyPolicy.ROOT_REUSE})
                for(int workers:new int[]{1,2,4,6}) try(var engine=new ParallelSearch(workers,EVAL,new TTable(1),policy);
                        var driver=new SearchDriver(engine)) {
                    for(int repeat=0;repeat<3;repeat++) {
                        var result=driver.search(new SearchRequest(board,history,3));
                        assertTrue(result.targetDepthCompleted());
                        assertEquals(expected,result.lastCompletedResult().score(),policy+" workers="+workers);
                        Phase2QuiescenceTest.legalPv(board,result.lastCompletedResult().principalVariation());
                        assertEquals(0,engine.activeSearchThreads());
                    }
                }
        }
    }
}
