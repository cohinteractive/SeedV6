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
class P4TtMemoryTest {
    static FlatSearchConfig config() {
        return BaselineEngine.configuration(1,64).mechanisms().with(FlatSearchConfig.Feature.HEURISTIC_TT_MEMORY,true);
    }
    static ConstructionSearch construction(int workers,int hash) {
        return new ConstructionSearch(config(),workers,hash,AspirationSearch.Policy.ADAPTIVE16);
    }
    static Object field(Object object,String name) {
        try { var f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object); }
        catch(ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    static class Observed extends TTable {
        ExactSearch kernel;long attempts,admitted,probes,hits,cutoffs;
        Observed() { super(8); }
        void safe() {
            if(kernel!=null) {
                assertEquals(false,field(kernel,"nullProbeActive"));
                assertEquals(false,field(kernel,"nullVerification"));
            }
        }
        public boolean saveHeuristic(long key,int depth,int type,int score,long move) {
            safe();attempts++;boolean r=super.saveHeuristic(key,depth,type,score,move);if(r)admitted++;return r;
        }
        public boolean probeHeuristic(long key,TEntry e) {
            safe();probes++;boolean r=super.probeHeuristic(key,e);if(r)hits++;return r;
        }
        public void heuristicCutoff() { safe();cutoffs++; }
    }

    @Test void researchIsOptInAndReuseIsSelectiveWithRealProducers() throws Exception {
        assertEquals(3407871,BaselineEngine.configuration(1,64).mechanisms().flags());
        assertEquals(7602175,config().flags());
        var model=ConventionalNnueEngine.load();var table=new Observed();
        var exact=ExactSearch.constructionResearch(ExactEvaluator.from(model.evaluation()),table,config());
        table.kernel=exact;var board=Board.startingPosition();var original=board.clone();
        exact.beginRequest();
        try {
            for(int i=0;i<2;i++) {
                var result=exact.search(board,5);
                assertTrue(result.completed());assertTrue(result.selective());
                Phase2Oracle.legalPv(board,result.principalVariation());
            }
        } finally {exact.endRequest();}
        assertArrayEquals(original,board);assertTrue(table.admitted>0);assertTrue(table.hits>0);assertTrue(table.cutoffs>0);
        assertNull(field(exact,"history"));assertEquals(false,field(exact,"nullProbeActive"));
    }

    @Test void incrementalAndRebuiltEvaluationAgreeAndPreserveChessBoundaries() throws Exception {
        var model=ConventionalNnueEngine.load();
        for(String fen:List.of(Board.FEN_STARTING_POSITION,
                "r3k2r/pppq1ppp/2n1bn2/3pp3/3PP3/2N1BN2/PPPQ1PPP/R3K2R w KQkq - 0 1",
                "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1","7k/P7/8/8/8/8/8/7K w - - 0 1",
                "7k/6Q1/5K2/8/8/8/8/8 b - - 100 1","7k/5Q2/6K1/8/8/8/8/8 b - - 100 1")) {
            var board=Board.fromFen(fen);var original=board.clone();
            try(var a=new SearchDriver(construction(1,8).create(model.evaluation()));
                var b=new SearchDriver(construction(1,8).create(SearchEvaluation.conventionalFullRebuild(model.model())))) {
                var first=a.search(new SearchRequest(board,5));var second=b.search(new SearchRequest(board,5));
                assertTrue(first.targetDepthCompleted());assertEquals(second.nodes(),first.nodes());
                assertEquals(second.lastCompletedResult().score(),first.lastCompletedResult().score());
                assertArrayEquals(second.lastCompletedResult().principalVariation(),first.lastCompletedResult().principalVariation());
                Phase2Oracle.legalPv(board,first.lastCompletedResult().principalVariation());
                assertArrayEquals(original,board);
            }
        }
    }

    @Test void reachedLegalNullVerifierCannotConsumeOrPublishHeuristicScores() throws Exception {
        var model=ConventionalNnueEngine.load();var table=new Observed();
        var exact=ExactSearch.constructionResearch(ExactEvaluator.from(model.evaluation()),table,config());
        table.kernel=exact;var diagnostics=new NullMovePolicy.Diagnostics();
        exact.withFlatNullObservation(false,diagnostics);
        var board=Board.startingPosition();var saved=board.clone();
        // Accepted accounting-regression witness, now under H1 rather than changing the oracle.
        var result=exact.searchRootMove(board,GameHistory.initial(board),4,
                ExhaustiveOracle.legalMoves(board)[0],1000,1001,true,()->false);
        assertTrue(result.completed());assertTrue(diagnostics.verifications>0);
        assertArrayEquals(saved,board);Phase2Oracle.legalPv(board,result.principalVariation());
        assertFalse((boolean)field(exact,"nullProbeActive"));assertFalse((boolean)field(exact,"nullVerification"));
        assertNull(field(exact,"history"));
        // Every heuristic table call also checks the actual live kernel's null flags in Observed.safe().
    }

    @Test void allSupportedWorkersAccountDrainCancelAndRestart() throws Exception {
        var model=ConventionalNnueEngine.load();var board=Board.startingPosition();var original=board.clone();
        for(int workers=1;workers<=16;workers++) try(var d=new SearchDriver(construction(workers,1).create(model.evaluation()))) {
            for(long budget:new long[]{1,100,4000}) {
                var c=SearchControl.controlled(budget,System.nanoTime(),30_000_000_000L,TimeSource.SYSTEM);
                var r=d.search(new SearchRequest(board,GameHistory.initial(board),40,c));
                assertEquals(budget,r.nodes());assertEquals(c.nodes(),r.nodes());assertEquals(0,d.activeSearchThreads());
                assertArrayEquals(original,board);
                if(r.lastCompletedResult()!=null)Phase2Oracle.legalPv(board,r.lastCompletedResult().principalVariation());
                d.newGame();
            }
            var c=SearchControl.controlled(10000,System.nanoTime()-10000,1,TimeSource.SYSTEM);
            assertNull(d.search(new SearchRequest(board,GameHistory.initial(board),10,c)).lastCompletedResult());
            assertEquals(SearchTermination.TIME_LIMIT,c.termination());assertEquals(0,d.activeSearchThreads());
            assertTrue(d.search(new SearchRequest(board,3)).targetDepthCompleted());
        }
    }
}
