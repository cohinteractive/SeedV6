package com.ohinteractive.seedv6.search.exact;

import java.util.*;
import org.junit.jupiter.api.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class P4TtInformationTest {
    static FlatSearchConfig config() {
        return P4TtAgedMemoryTest.config().with(FlatSearchConfig.Feature.TT_INFORMATION,true);
    }
    static class Observed extends TTable {
        ExactSearch kernel;long hits,saves;
        Observed() {super(8,true);}
        void safe() {
            if(kernel!=null) {
                assertEquals(false,P4TtMemoryTest.field(kernel,"nullProbeActive"));
                assertEquals(false,P4TtMemoryTest.field(kernel,"nullVerification"));
            }
        }
        @Override public boolean probeInformation(long key,TEntry entry) {
            safe();return super.probeInformation(key,entry);
        }
        @Override public void rawEvaluationUsed() {safe();hits++;}
        @Override public boolean saveRawEvaluation(long key,int value) {
            safe();saves++;return super.saveRawEvaluation(key,value);
        }
    }
    static class Counting implements ExactEvaluator {
        final ExactEvaluator delegate;long evaluations,children,reentries;
        Counting(ExactEvaluator delegate) {this.delegate=delegate;}
        public void initialize(long[] b) {delegate.initialize(b);}
        public void child(long[] b,long[] c,int ply,int qply) {children++;delegate.child(b,c,ply,qply);}
        public void reenter(int ply) {reentries++;delegate.reenter(ply);}
        public int evaluate(long[] b,int ply) {evaluations++;return delegate.evaluate(b,ply);}
    }

    @Test void optInRequiresStorage() {
        assertEquals(3407871,BaselineEngine.legacyConfiguration(1,64).mechanisms().flags());
        assertThrows(IllegalArgumentException.class,()->ExactSearch.constructionResearch((b,p)->0,new TTable(1),config()));
    }

    @Test void repeatedSelectiveSearchAvoidsInferenceWithoutChangingWorkOrTransitions() throws Exception {
        var model=ConventionalNnueEngine.load();var t=new Observed();
        var a=new Counting(ExactEvaluator.from(model.evaluation()));
        var b=new Counting(ExactEvaluator.from(SearchEvaluation.conventionalFullRebuild(model.model())));
        var cached=ExactSearch.constructionResearch(a,t,config());t.kernel=cached;
        var plain=ExactSearch.constructionResearch(b,new TTable(8),P4TtAgedMemoryTest.config());
        for(String fen:List.of(Board.FEN_STARTING_POSITION,
                "r3k2r/pppq1ppp/2n1bn2/3pp3/3PP3/2N1BN2/PPPQ1PPP/R3K2R w KQkq - 0 1",
                "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1","7k/P7/8/8/8/8/8/7K w - - 0 1")) {
            var board=Board.fromFen(fen);var saved=board.clone();cached.beginRequest();plain.beginRequest();
            try {
                for(int depth:new int[]{3,4,4}) {
                    var x=cached.search(board,depth);var y=plain.search(board,depth);
                    assertTrue(x.completed());assertTrue(y.completed());assertEquals(y.score(),x.score());
                    assertEquals(y.nodes(),x.nodes());assertArrayEquals(y.principalVariation(),x.principalVariation());
                    Phase2Oracle.legalPv(board,x.principalVariation());assertArrayEquals(saved,board);
                }
            } finally {cached.endRequest();plain.endRequest();}
        }
        assertTrue(t.hits>100);assertTrue(t.saves>100);
        assertEquals(b.evaluations-a.evaluations,t.hits);
        assertEquals(b.children,a.children);assertEquals(b.reentries,a.reentries);
    }

    @Test void cacheCannotBypassEvaluatorFailuresOrNullVerifierIsolation() throws Exception {
        var model=ConventionalNnueEngine.load();var table=new Observed();
        var exact=ExactSearch.constructionResearch(ExactEvaluator.from(model.evaluation()),table,config());table.kernel=exact;
        var diagnostics=new NullMovePolicy.Diagnostics();exact.withFlatNullObservation(false,diagnostics);
        var board=Board.startingPosition();var saved=board.clone();
        var result=exact.searchRootMove(board,GameHistory.initial(board),4,
                ExhaustiveOracle.legalMoves(board)[0],1000,1001,true,()->false);
        assertTrue(result.completed());assertTrue(diagnostics.verifications>0);
        assertArrayEquals(saved,board);Phase2Oracle.legalPv(board,result.principalVariation());
        assertNull(P4TtMemoryTest.field(exact,"history"));
        var bad=ExactSearch.constructionResearch((b,p)->32512,new TTable(1,true),config());
        assertThrows(IllegalArgumentException.class,()->bad.search(board,2));
        assertArrayEquals(saved,board);assertNull(P4TtMemoryTest.field(bad,"history"));
    }

    @Test void requestEpochDoesNotReuseAChangedEvaluatorDefinition() {
        int[] value={7};var table=new TTable(1,true);
        var exact=ExactSearch.constructionResearch((b,p)->value[0],table,config());
        var board=Board.startingPosition();
        for(int score:new int[]{7,-300,0,32511}) {
            value[0]=score;exact.beginRequest();
            try {
                assertTrue(exact.search(board,2).completed());
                var e=new TTable.TEntry();
                assertTrue(table.probeInformation(SearchKey.key(board,SearchKey.rootHistory(board,GameHistory.initial(board))),e));
                assertEquals(score,e.rawEvaluation);
            } finally {exact.endRequest();}
        }
    }

    @Test void terminalsRealHistoryAndRepeatedDriverRequestsMatchControl() throws Exception {
        var model=ConventionalNnueEngine.load();
        var roots=new ArrayList<P3HistoryStudy.Root>();
        for(String fen:List.of("7k/6Q1/5K2/8/8/8/8/8 b - - 100 1",
                "7k/5Q2/6K1/8/8/8/8/8 b - - 100 1",
                "4k3/8/8/8/8/8/4P3/4K3 w - - 99 1",
                "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1")) {
            var board=Board.fromFen(fen);roots.add(new P3HistoryStudy.Root(fen,board,GameHistory.initial(board)));
        }
        var board=Board.startingPosition();var history=GameHistory.builder(board);
        for(String uci:List.of("g1f3","g8f6","f3g1","f6g8","g1f3","g8f6","f3g1","f6g8")) {
            long move=Arrays.stream(ExhaustiveOracle.legalMoves(board))
                    .filter(m->com.ohinteractive.seedv6.core.move.Move.coordinate(m).equals(uci)).findFirst().orElseThrow();
            board=ExhaustiveOracle.child(board,move);history.appendPosition(board);
        }
        roots.add(new P3HistoryStudy.Root("real repetition",board,history.snapshot()));
        try(var cached=new SearchDriver(new ConstructionSearch(config(),1,8,AspirationSearch.Policy.ADAPTIVE16).create(model.evaluation()));
            var plain=new SearchDriver(P4TtAgedMemoryTest.construction(1,8).create(model.evaluation()))) {
            for(var root:roots)for(int request=0;request<2;request++) {
                var saved=root.board().clone();var a=cached.search(new SearchRequest(root.board(),root.history(),4));
                var b=plain.search(new SearchRequest(root.board(),root.history(),4));
                ConstructionTtStudy.same(a,b);assertArrayEquals(saved,root.board());
                Phase2Oracle.legalPv(root.board(),a.lastCompletedResult().principalVariation());
            }
        }
    }

    @Test void allWorkersAccountDrainCancelResetAndRestart() throws Exception {
        var model=ConventionalNnueEngine.load();var board=Board.startingPosition();var saved=board.clone();
        for(int workers=1;workers<=16;workers++)try(var driver=new SearchDriver(
                new ConstructionSearch(config(),workers,1,AspirationSearch.Policy.ADAPTIVE16).create(model.evaluation()))) {
            for(long budget:new long[]{1,100,4000}) {
                var control=SearchControl.controlled(budget,System.nanoTime(),30_000_000_000L,TimeSource.SYSTEM);
                var result=driver.search(new SearchRequest(board,GameHistory.initial(board),40,control));
                assertEquals(budget,result.nodes());assertEquals(control.nodes(),result.nodes());
                assertEquals(0,driver.activeSearchThreads());assertArrayEquals(saved,board);
                if(result.lastCompletedResult()!=null)Phase2Oracle.legalPv(board,result.lastCompletedResult().principalVariation());
            }
            var expired=SearchControl.controlled(10000,System.nanoTime()-10000,1,TimeSource.SYSTEM);
            assertNull(driver.search(new SearchRequest(board,GameHistory.initial(board),10,expired)).lastCompletedResult());
            assertEquals(SearchTermination.TIME_LIMIT,expired.termination());assertEquals(0,driver.activeSearchThreads());
            driver.newGame();assertTrue(driver.search(new SearchRequest(board,3)).targetDepthCompleted());
        }
    }
}
