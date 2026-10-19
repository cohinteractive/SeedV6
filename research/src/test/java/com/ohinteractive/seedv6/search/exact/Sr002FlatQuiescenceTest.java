package com.ohinteractive.seedv6.search.exact;

import java.nio.file.Path;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.*;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.search.exact.ExactSearch.*;

@Timeout(180)
class Sr002FlatQuiescenceTest {
    private boolean full;
    private ExactSearch enable(ExactSearch search) {
        return full?search.withFlatSearchResearch(true):search.withFlatQuiescenceResearch(true);
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void unprunedIndependentOracleAndTacticalFixtures(boolean full) {
        this.full=full;
        ExactEvaluator hce=(b,p)->Eval.evaluate(b);
        for(var fixture:QuiescenceSeeCorpus.FIXTURES) {
            var board=Board.fromFen(fixture.fen());
            var recursive=ExactSearch.quiescencePvsResearch(hce,new TTable(1),0);
            var flat=enable(ExactSearch.quiescencePvsResearch(hce,new TTable(1),0));
            for(int depth=0;depth<=1;depth++) {
                var a=recursive.search(board,depth);var b=flat.search(board,depth);
                P3InformationBaseline.same(a,b);
                assertEquals(ExactSearch.quiescenceResearch(hce).search(board,depth).score(),b.score(),fixture.name());
                Phase2Oracle.legalPv(board,b.principalVariation());
            }
        }
        for(String fen:List.of("4k3/8/8/3q4/4P3/8/8/4K3 w - - 98 1",
                "4r1k1/8/8/8/8/8/8/4K3 w - - 99 1",
                "4k3/8/8/3pP3/8/8/8/4K3 w - d6 98 1",
                "8/k1P5/2K5/8/8/8/8/8 w - - 98 1",
                "7k/6Q1/5K2/8/8/8/8/8 b - - 100 1",
                "7k/5Q2/6K1/8/8/8/8/8 b - - 100 1")) {
            var board=Board.fromFen(fen);var game=GameHistory.initial(board);
            for(int depth=0;depth<=1;depth++) {
                int expected=new Phase2Oracle.RestrictedOracle(hce,0).score(board,new SearchLineHistory(game),depth,0,0,-1);
                var flat=enable(ExactSearch.quiescencePvsResearch(hce,null,0));
                assertEquals(expected,flat.search(board,depth).score(),fen);
            }
        }
    }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void policy4ExactEventsWindowsHistoryHintsAndProofMatch(boolean full) throws Exception {
        this.full=full;
        var evaluation=P3InformationBaselineTest.evaluation();
        var roots=P3HistoryStudy.roots(Path.of("../docs/research/search/sr005-phase2-2026-10-13/diagnostic-corpus.tsv"));
        for(var root:roots.subList(0,8)) {
            var left=new Trace(evaluation);var right=new Trace(evaluation);
            var lt=new TTable(1);var rt=new TTable(1);
            var recursive=policy(left,lt,false);var flat=policy(right,rt,true);
            recursive.beginRequest();flat.beginRequest();
            try {
                for(int depth=1;depth<=5;depth++) for(int[] w:new int[][]{{-INFINITY,INFINITY},{-1,0},{0,1},{10,60}}) {
                    left.events.clear();right.events.clear();
                    var a=recursive.searchWindow(root.board(),root.history(),depth,w[0],w[1],()->recursive.visitedNodes()>=100000);
                    var b=flat.searchWindow(root.board(),root.history(),depth,w[0],w[1],()->flat.visitedNodes()>=100000);
                    P3InformationBaseline.same(a,b);
                    assertEquals(left.events,right.events,root.id());
                    assertArrayEquals(P3InformationBaseline.history(recursive),P3InformationBaseline.history(flat));
                    for(String field:List.of("key","data","hashMove"))
                        assertArrayEquals((long[])Policy4ProductionTest.field(lt,field),(long[])Policy4ProductionTest.field(rt,field),field);
                    Phase2Oracle.legalPv(root.board(),b.principalVariation());
                }
            } finally {recursive.endRequest();flat.endRequest();}
        }
    }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void cancellationExceptionUnwindAndReusePreserveEntryHistory(boolean full) throws Exception {
        this.full=full;
        var evaluation=P3InformationBaselineTest.evaluation();var board=Board.fromFen(SeePruningTest.MIDDLE);
        var game=GameHistory.initial(board);var original=board.clone();
        for(int limit:new int[]{1,2,10,100,1000}) {
            var a=policy(ExactEvaluator.from(evaluation),new TTable(1),false);
            var b=policy(ExactEvaluator.from(evaluation),new TTable(1),true);
            P3InformationBaseline.same(a.search(board,game,8,()->a.visitedNodes()>=limit),
                    b.search(board,game,8,()->b.visitedNodes()>=limit));
            a.newGame();b.newGame();P3InformationBaseline.same(a.search(board,3),b.search(board,3));
            assertArrayEquals(original,board);
        }
        var fail=new AtomicBoolean(true);var delegate=ExactEvaluator.from(evaluation);
        ExactEvaluator throwing=new ExactEvaluator() {
            public void initialize(long[] b){delegate.initialize(b);}
            public void child(long[] p,long[] c,int ply){delegate.child(p,c,ply);}
            public int evaluate(long[] b,int ply){if(ply>4 && fail.get())throw new IllegalStateException("injected");return delegate.evaluate(b,ply);}
        };
        var flat=policy(throwing,new TTable(1),true);
        assertEquals("injected",assertThrows(IllegalStateException.class,()->flat.search(board,5)).getMessage());
        fail.set(false);flat.newGame();
        P3InformationBaseline.same(policy(ExactEvaluator.from(evaluation),new TTable(1),false).search(board,3),flat.search(board,3));
        var repeated=Board.startingPosition();var builder=GameHistory.builder(repeated);
        for(String uci:"g1f3 g8f6 f3g1 f6g8 g1f3 g8f6 f3g1 f6g8".split(" ")) {
            long move=Arrays.stream(ExhaustiveOracle.legalMoves(repeated))
                    .filter(m->Move.coordinate(m).equals(uci)).findFirst().orElseThrow();
            repeated=ExhaustiveOracle.child(repeated,move);builder.appendPosition(repeated);
        }
        assertEquals(0,flat.search(repeated,builder.snapshot(),5,NEVER_CANCELLED).score());
    }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void workerIsolationGlobalBudgetDrainingStopAndRecovery(boolean full) throws Exception {
        this.full=full;
        var e=P3InformationBaselineTest.evaluation();var board=Board.startingPosition();
        for(int workers:new int[]{1,2,4}) try(var engine=Sr002ExecutionStudy.engine(workers,e,full?2:1);
                var driver=new SearchDriver(engine)) {
            var frames=Collections.newSetFromMap(new IdentityHashMap<>());
            for(var exact:Policy4ProductionTest.searches(engine)) assertTrue(frames.add(Policy4ProductionTest.field(exact,"flatQFrames")));
            for(int pass=0;pass<3;pass++) {
                var control=SearchControl.controlled(3000,System.nanoTime(),30_000_000_000L,TimeSource.SYSTEM);
                var outcome=driver.search(new SearchRequest(board,GameHistory.initial(board),20,SearchObserver.NONE,control,true));
                assertEquals(3000,outcome.nodes());assertEquals(outcome.nodes(),outcome.diagnostics().totalEnteredNodes());
                assertEquals(0,driver.activeSearchThreads());
                assertNotNull(outcome.lastCompletedResult());Phase2Oracle.legalPv(board,outcome.lastCompletedResult().principalVariation());
                driver.newGame();
            }
            var stopped=SearchControl.controlled(100000,System.nanoTime(),30_000_000_000L,TimeSource.SYSTEM);
            stopped.request(SearchTermination.STOPPED);
            assertNull(driver.search(new SearchRequest(board,GameHistory.initial(board),20,stopped)).lastCompletedResult());
            assertTrue(driver.search(new SearchRequest(board,3)).targetDepthCompleted());
        }
    }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void productionDefaultAndUnsupportedDomainsStayExplicit(boolean full) throws Exception {
        this.full=full;
        var e=P3InformationBaselineTest.evaluation();
        try(var normal=ProductionSearch.create(1,e)) {
            assertNull(Policy4ProductionTest.field(Policy4ProductionTest.searches(normal)[0],"flatQFrames"));
        }
        assertThrows(IllegalStateException.class,()->enable(new ExactSearch()));
        assertThrows(IllegalStateException.class,()->enable(ExactSearch.quiescenceResearch((b,p)->0,QSEARCH_QTT)));
        var q=enable(ExactSearch.quiescencePvsResearch((b,p)->0,new TTable(1),0));
        q.withSeeResearch(SeePolicy.QSCOUT,null);
        assertThrows(IllegalStateException.class,()->q.search(Board.startingPosition(),1));
    }

    private ExactSearch policy(ExactEvaluator e,TTable table,boolean flat) {
        var search=ExactSearch.iterationReuseResearch(e,table,IterationReuse.EFFORT,true,null)
                .withSeeResearch(SeePolicy.QXRAY,null).withCompletedHints(true,null);
        return flat?enable(search):search;
    }
    private static final class Trace implements ExactEvaluator {
        final ExactEvaluator delegate;final List<String> events=new ArrayList<>();
        Trace(SearchEvaluation e){delegate=ExactEvaluator.from(e);}
        public void initialize(long[] b){events.add("I"+Arrays.toString(b));delegate.initialize(b);}
        public void child(long[] p,long[] c,int ply,int qply){events.add("C"+ply+":"+qply+":"+Arrays.toString(c));delegate.child(p,c,ply,qply);}
        public void reenter(int ply){events.add("R"+ply);delegate.reenter(ply);}
        public int evaluate(long[] b,int ply){int v=delegate.evaluate(b,ply);events.add("E"+ply+":"+v);return v;}
    }

    @Test void fullMainRetainsTtProofAdmissionAndTerminalPrecedence() {
        var board=Board.startingPosition();var game=GameHistory.initial(board);
        long legal=ExhaustiveOracle.legalMoves(board)[0],key=SearchKey.key(board,SearchKey.rootHistory(board,game));
        for(int storedDepth:new int[]{0,1,2}) for(int type=0;type<3;type++) for(boolean old:new boolean[]{false,true})
            for(long hint:new long[]{0,Long.MAX_VALUE,legal}) {
                var lt=new TTable(1);var rt=new TTable(1);
                var rec=ExactSearch.quiescencePvsResearch((b,p)->-37,lt,0);
                var flat=ExactSearch.quiescencePvsResearch((b,p)->-37,rt,0).withFlatSearchResearch(true);
                rec.beginRequest();flat.beginRequest();
                lt.save(key,storedDepth,type,type==2?-1000:1000,hint);rt.save(key,storedDepth,type,type==2?-1000:1000,hint);
                if(old) {rec.endRequest();flat.endRequest();rec.beginRequest();flat.beginRequest();}
                P3InformationBaseline.same(rec.searchWindow(board,game,1,-50,50,NEVER_CANCELLED),
                        flat.searchWindow(board,game,1,-50,50,NEVER_CANCELLED));
                rec.endRequest();flat.endRequest();
            }
        for(String fen:List.of("7k/6Q1/5K2/8/8/8/8/8 b - - 100 1","7k/5Q2/6K1/8/8/8/8/8 b - - 0 1",
                "4k3/8/8/8/8/8/P7/4K3 w - - 100 1")) {
            var terminal=Board.fromFen(fen);var history=GameHistory.initial(terminal);var table=new TTable(1);
            var flat=ExactSearch.quiescencePvsResearch((b,p)->{throw new AssertionError("terminal evaluated");},table,0).withFlatSearchResearch(true);
            flat.beginRequest();table.save(SearchKey.key(terminal,SearchKey.rootHistory(terminal,history)),3,0,12345,legal);
            assertEquals(new ExactSearch().search(terminal,3).score(),flat.search(terminal,3).score());flat.endRequest();
        }
    }

    @Test void finalQSlotAbortsUnresolvedWorkAndRestoresOnlyItsOwnPath() throws Exception {
        var board=Board.fromFen("4r1k1/8/8/8/8/8/8/4K3 w - - 0 1");
        var q=ExactSearch.quiescencePvsResearch((b,p)->{throw new AssertionError("checked evaluation");},null,0)
                .withFlatSearchResearch(true);
        var boards=(long[][])Policy4ProductionTest.field(q,"boards");
        System.arraycopy(board,0,boards[MAX_DEPTH],0,Board.MAX_BITBOARDS);
        var history=new SearchLineHistory(GameHistory.initial(board));
        set(q,"history",history);set(q,"cancelled",NEVER_CANCELLED);
        var call=ExactSearch.class.getDeclaredMethod("flatQuiescence",int.class,int.class,int.class,int.class);
        call.setAccessible(true);
        var error=assertThrows(InvocationTargetException.class,()->call.invoke(q,MAX_DEPTH,3,-INFINITY,INFINITY));
        assertEquals("Aborted",error.getCause().getClass().getSimpleName());assertEquals(history.rootSize(),history.size());
        var mate=Board.fromFen("7k/6Q1/5K2/8/8/8/8/8 b - - 0 1");
        System.arraycopy(mate,0,boards[MAX_DEPTH],0,Board.MAX_BITBOARDS);
        assertEquals(-MATE_SCORE+MAX_DEPTH,call.invoke(q,MAX_DEPTH,3,-INFINITY,INFINITY));
    }

    @Test void completedIterationStopAndIndependentOwnersRecover() throws Exception {
        var e=P3InformationBaselineTest.evaluation();var board=Board.startingPosition();
        try(var a=new SearchDriver(Sr002ExecutionStudy.engine(4,e,2));
            var b=new SearchDriver(Sr002ExecutionStudy.engine(2,e,2))) {
            var control=SearchControl.controlled(10_000_000,System.nanoTime(),30_000_000_000L,TimeSource.SYSTEM);
            var observer=new SearchObserver() {
                @Override public void onIterationCompleted(IterationSnapshot snapshot) {
                    if(snapshot.depth()==3) control.request(SearchTermination.STOPPED);
                }
            };
            var request=new SearchRequest(board,GameHistory.initial(board),30,observer,control,true);
            var outcome=a.search(request);assertEquals(SearchTermination.STOPPED,control.termination());
            assertNotNull(outcome.lastCompletedResult());assertEquals(0,a.activeSearchThreads());
            assertTrue(b.search(new SearchRequest(board,3)).targetDepthCompleted());
            a.newGame();assertTrue(a.search(new SearchRequest(board,4)).targetDepthCompleted());
        }
    }

    private static void set(Object owner,String name,Object value) throws Exception {
        var field=owner.getClass().getDeclaredField(name);field.setAccessible(true);field.set(owner,value);
    }
}
