package com.ohinteractive.seedv6.search.exact;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.manage.*;
import com.ohinteractive.seedv6.search.tt.TTable;

@Timeout(90)
class P4IndependentSearchTest {
    private static void await(CountDownLatch latch) {
        try { assertTrue(latch.await(5,TimeUnit.SECONDS),"worker coordination witness timed out"); }
        catch(InterruptedException e) { Thread.currentThread().interrupt();throw new AssertionError(e); }
    }
    private static ExactEvaluator observed(ExactEvaluator delegate,Runnable initialize,Runnable evaluate) {
        return new ExactEvaluator() {
            @Override public void initialize(long[] b) { initialize.run();delegate.initialize(b); }
            @Override public void child(long[] p,long[] c,int ply,int qply) { delegate.child(p,c,ply,qply); }
            @Override public void child(long[] p,long[] c,int ply) { delegate.child(p,c,ply); }
            @Override public void reenter(int ply) { delegate.reenter(ply); }
            @Override public int evaluate(long[] b,int ply) { evaluate.run();return delegate.evaluate(b,ply); }
        };
    }

    @Test void helperAdvancesIndependentlyAndCannotMakeOwnersForcedMoveClockDecision() {
        var board=Board.fromFen("7k/4Q3/6K1/8/8/8/8/8 b - - 0 1");
        assertEquals(1,ExhaustiveOracle.legalMoves(board).length);
        var nextDepth=new CountDownLatch(1);var evaluation=SearchEvaluation.handcrafted();
        var control=SearchControl.controlled(-1,0,1_000_000_000L,()->0,true);
        try(var owner=new IndependentSearch(2,evaluation,new TTable(1),FlatSearchConfig.exact(),
                AspirationSearch.Policy.ADAPTIVE16,ClockPolicy.BOUNDARY2,id -> {
                    var calls=new AtomicInteger();
                    return observed(ExactEvaluator.from(evaluation),()-> {
                        int call=calls.incrementAndGet();
                        if(id==1 && call==2) nextDepth.countDown();
                        if(id==0 && call==1) { await(nextDepth);assertEquals(SearchTermination.NONE,control.termination()); }
                    },()->{});
                })) {
            var result=owner.search(new SearchRequest(board,GameHistory.initial(board),3,control));
            assertEquals(0,nextDepth.getCount());assertEquals(SearchTermination.TIME_ALLOCATION,control.termination());
            assertEquals(1,result.lastCompletedResult().depth());assertFalse(result.targetDepthCompleted());
            assertEquals(0,owner.activeSearchThreads());assertEquals(control.nodes(),result.nodes());
            assertTrue(owner.workerProgress()[1].attemptedDepth()>=2);
        }
    }

    @Test void independentExactIterationsRetainOracleRulesAndHonestOwnerDepth() {
        var evaluation=SearchEvaluation.handcrafted();
        for(String fen:List.of("4k3/8/8/3pP3/8/8/8/4K3 w - d6 98 1",
                "8/k1P5/2K5/8/8/8/8/8 w - - 98 1","7k/6Q1/5K2/8/8/8/8/8 b - - 100 1")) {
            var board=Board.fromFen(fen);var saved=board.clone();var history=GameHistory.initial(board);
            int expected=ExactSearch.quiescenceResearch(ExactEvaluator.from(evaluation))
                    .search(board,history,3,()->false).score();
            for(int workers:new int[]{2,4}) try(var owner=IndependentSearch.constructionResearch(workers,evaluation,
                    new TTable(1),FlatSearchConfig.exact(),AspirationSearch.Policy.ADAPTIVE16,ClockPolicy.BOUNDARY2)) {
                var control=SearchControl.controlled(-1,0,-1,TimeSource.SYSTEM);
                var result=owner.search(new SearchRequest(board,history,3,SearchObserver.NONE,control,true));
                assertTrue(result.targetDepthCompleted());assertEquals(expected,result.lastCompletedResult().score());
                assertEquals(SearchTermination.NONE,control.termination());assertEquals(control.nodes(),result.nodes());
                assertEquals(result.nodes(),result.diagnostics().totalEnteredNodes());
                assertEquals(result.lastCompletedResult().depth(),owner.workerProgress()[0].completedDepth());
                Phase2Oracle.legalPv(board,result.lastCompletedResult().principalVariation());
                assertArrayEquals(saved,board);assertEquals(0,owner.activeSearchThreads());
            }
        }
        var board=Board.startingPosition();var history=GameHistory.builder(board);
        for(int i=0;i<8;i++) {
            board=ExactSearchTTableTest.play(board,new String[]{"g1f3","g8f6","f3g1","f6g8"}[i%4]);
            history.appendPosition(board);
        }
        var real=history.snapshot();assertTrue(real.isFormalThreefold(board));
        try(var owner=IndependentSearch.constructionResearch(4,evaluation,new TTable(1),FlatSearchConfig.exact(),
                AspirationSearch.Policy.ADAPTIVE16,ClockPolicy.BOUNDARY2)) {
            var result=owner.search(new SearchRequest(board,real,3));
            assertTrue(result.terminalRoot());assertEquals(0,result.lastCompletedResult().score());
            assertEquals(0,result.nodes());assertFalse(result.lastCompletedResult().hasMove());
        }
    }

    @Test void tcP1MemoryAccountsAllWorkersAcrossCapsStopsFailuresAndReuse() throws Exception {
        var model=ConventionalNnueEngine.load();var board=Board.startingPosition();var saved=board.clone();
        var config=BaselineEngine.configuration(4,1,BaselineEngine.SearchProfile.TC_P1).mechanisms()
                .with(FlatSearchConfig.Feature.HEURISTIC_TT_MEMORY,true);
        for(int workers:new int[]{2,4,8,16}) {
            var counts=new P4MultiworkerStudy.Admissions[workers];
            try(var owner=new IndependentSearch(workers,model.evaluation(),new TTable(1),config,
                    AspirationSearch.Policy.ADAPTIVE16,ClockPolicy.BOUNDARY2,
                    id -> counts[id]=new P4MultiworkerStudy.Admissions(ExactEvaluator.from(model.evaluation())))) {
                for(int cap:new int[]{0,1,100,5000}) {
                    for(var count:counts) count.reset();
                    var control=SearchControl.controlled(cap,0,-1,TimeSource.SYSTEM);
                    var result=owner.search(new SearchRequest(board,GameHistory.initial(board),20,SearchObserver.NONE,control,true));
                    assertEquals(cap,result.nodes());assertEquals(cap,control.nodes());
                    assertEquals(cap,Arrays.stream(counts).mapToLong(a->a.nodes).sum());
                    assertEquals(cap,Arrays.stream(owner.workerProgress()).mapToLong(IndependentSearch.WorkerProgress::nodes).sum());
                    assertEquals(cap,result.diagnostics().totalEnteredNodes());assertEquals(0,owner.activeSearchThreads());
                    assertArrayEquals(saved,board);
                    if(result.lastCompletedResult()!=null) {
                        assertTrue(result.lastCompletedResult().nodes()<=result.nodes());
                        Phase2Oracle.legalPv(board,result.lastCompletedResult().principalVariation());
                    }
                }
                var injected=new AtomicBoolean();
                TimeSource broken=()-> {
                    if(Thread.currentThread().getName().equals("seedv6-independent-search")
                            && injected.compareAndSet(false,true)) throw new IllegalStateException("injected independent failure");
                    return 0;
                };
                assertThrows(IllegalStateException.class,()->owner.search(new SearchRequest(board,GameHistory.initial(board),12,
                        SearchControl.controlled(-1,0,Long.MAX_VALUE,broken))));
                assertTrue(injected.get());assertEquals(0,owner.activeSearchThreads());owner.newGame();
                var stopped=SearchControl.controlled(-1,0,-1,TimeSource.SYSTEM);stopped.request(SearchTermination.STOPPED);
                assertEquals(0,owner.search(new SearchRequest(board,GameHistory.initial(board),8,stopped)).nodes());
                var expired=SearchControl.controlled(-1,0,0,()->0);
                assertEquals(0,owner.search(new SearchRequest(board,GameHistory.initial(board),8,expired)).nodes());
                assertEquals(SearchTermination.TIME_LIMIT,expired.termination());
                assertTrue(owner.search(new SearchRequest(board,3)).targetDepthCompleted());
            }
        }
    }

    @Test void completedOwnerCannotReturnBeforeHelperTailDrains() throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var iteration=new CountDownLatch(1);
        var evaluation=SearchEvaluation.handcrafted();var board=Board.startingPosition();
        var outcome=new AtomicReference<SearchDriverOutcome>();var problem=new AtomicReference<Throwable>();
        try(var owner=new IndependentSearch(2,evaluation,new TTable(1),FlatSearchConfig.exact(),
                AspirationSearch.Policy.ADAPTIVE16,ClockPolicy.BOUNDARY2,id -> {
                    var first=new AtomicBoolean(true);
                    return observed(ExactEvaluator.from(evaluation),()-> { if(id==0) await(entered); },()-> {
                        if(id==1 && first.getAndSet(false)) { entered.countDown();await(release); }
                    });
                })) {
            var control=SearchControl.controlled(-1,0,-1,TimeSource.SYSTEM);
            var observer=new SearchObserver() {
                @Override public void onIterationCompleted(IterationSnapshot s) {
                    assertEquals(1,s.depth());assertFalse(s.diagnostics().enabled());iteration.countDown();
                }
            };
            var thread=new Thread(()-> {
                try { outcome.set(owner.search(new SearchRequest(board,GameHistory.initial(board),1,observer,control,true))); }
                catch(Throwable t) { problem.set(t); }
            });
            thread.start();
            try {
                await(iteration);assertNull(outcome.get());assertTrue(thread.isAlive());
                assertTrue(owner.activeSearchThreads()>0);
            } finally { release.countDown();thread.join(10_000); }
            assertFalse(thread.isAlive());assertNull(problem.get());assertNotNull(outcome.get());
            assertEquals(control.nodes(),outcome.get().nodes());
            assertEquals(control.nodes(),outcome.get().diagnostics().totalEnteredNodes());
            assertTrue(owner.workerProgress()[1].nodes()>0);assertEquals(0,owner.activeSearchThreads());
        } finally { release.countDown(); }
    }

    @Test void managedReplacementAndNewGameSuppressOldPublicationAfterFullDrain() throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var done=new CountDownLatch(1);
        var oldPublications=new AtomicInteger();var result=new AtomicReference<ManagedSearchResult>();
        var evaluation=SearchEvaluation.handcrafted();var first=new AtomicBoolean(true);
        var owner=new IndependentSearch(4,evaluation,new TTable(1),FlatSearchConfig.exact(),
                AspirationSearch.Policy.ADAPTIVE16,ClockPolicy.BOUNDARY2,id -> observed(ExactEvaluator.from(evaluation),()-> {
                    if(id==0 && first.getAndSet(false)) { entered.countDown();await(release); }
                },()->{}));
        var service=SearchLifecycleService.iterativeResearch(TimeSource.SYSTEM,()->owner);
        try {
            var board=Board.startingPosition();var history=GameHistory.initial(board);
            service.start(board,history,new SearchLimits(10,-1,-1,false),r->oldPublications.incrementAndGet());
            await(entered);service.invalidate(SearchTermination.NEW_GAME);
            long generation=service.start(board,history,new SearchLimits(2,-1,-1,false),r->{result.set(r);done.countDown();});
            release.countDown();await(done);
            assertEquals(0,oldPublications.get());assertEquals(generation,result.get().generation());
            assertEquals(SearchTermination.COMPLETED,result.get().termination());
            assertEquals(2,result.get().lastCompletedResult().depth());assertNull(service.lastFailure());
            assertEquals(0,owner.activeSearchThreads());
        } finally { release.countDown();service.close(); }
        assertTrue(service.isTerminated());
    }

    @Test void interruptedOwnerRestoresFlagAndManagedFailureCanBeReused() throws Exception {
        var evaluation=SearchEvaluation.handcrafted();var board=Board.startingPosition();
        try(var owner=IndependentSearch.constructionResearch(4,evaluation,new TTable(1),FlatSearchConfig.exact(),
                AspirationSearch.Policy.ADAPTIVE16,ClockPolicy.BOUNDARY2)) {
            var control=SearchControl.controlled(-1,0,-1,TimeSource.SYSTEM);
            Thread.currentThread().interrupt();
            try {
                var outcome=owner.search(new SearchRequest(board,GameHistory.initial(board),8,control));
                assertEquals(SearchTermination.STOPPED,control.termination());assertNull(outcome.lastCompletedResult());
                assertTrue(Thread.currentThread().isInterrupted());assertEquals(0,owner.activeSearchThreads());
            } finally { Thread.interrupted(); }
            assertTrue(owner.search(new SearchRequest(board,2)).targetDepthCompleted());
        }
        var injected=new AtomicBoolean();var completed=new CountDownLatch(1);var reused=new CountDownLatch(1);
        var published=new AtomicReference<ManagedSearchResult>();
        var owner=new IndependentSearch(4,evaluation,new TTable(1),FlatSearchConfig.exact(),
                AspirationSearch.Policy.ADAPTIVE16,ClockPolicy.BOUNDARY2,id -> observed(ExactEvaluator.from(evaluation),()-> {
                    if(id==1 && injected.compareAndSet(false,true)) throw new IllegalStateException("managed helper failure");
                },()->{}));
        try(var service=SearchLifecycleService.iterativeResearch(TimeSource.SYSTEM,()->owner)) {
            service.start(board,GameHistory.initial(board),new SearchLimits(8,-1,-1,false),r->{published.set(r);completed.countDown();});
            await(completed);assertTrue(injected.get());assertEquals(SearchTermination.FAILURE,published.get().termination());
            assertNotNull(published.get().failure());assertEquals(0,owner.activeSearchThreads());
            service.start(board,GameHistory.initial(board),new SearchLimits(2,-1,-1,false),r->{published.set(r);reused.countDown();});
            await(reused);assertEquals(SearchTermination.COMPLETED,published.get().termination());
            assertEquals(0,owner.activeSearchThreads());
        }
    }
}
