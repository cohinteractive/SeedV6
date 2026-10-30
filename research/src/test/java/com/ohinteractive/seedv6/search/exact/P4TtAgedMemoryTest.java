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
class P4TtAgedMemoryTest {
    static FlatSearchConfig config() {
        return P4TtMemoryTest.config().with(FlatSearchConfig.Feature.AGED_HEURISTIC_TT_MEMORY,true);
    }
    static ConstructionSearch construction(int workers,int hash) {
        return new ConstructionSearch(config(),workers,hash,AspirationSearch.Policy.ADAPTIVE16);
    }
    static final class Observed extends TTable {
        ExactSearch kernel;int generation,lastAge;long lastEpoch,priorHits,expiredHits,priorCutoffs;
        Observed() {super(8);}
        void safe() {
            assertEquals(false,P4TtMemoryTest.field(kernel,"nullProbeActive"));
            assertEquals(false,P4TtMemoryTest.field(kernel,"nullVerification"));
        }
        @Override public void advanceGeneration() {super.advanceGeneration();generation=(generation+1)&255;}
        @Override public boolean probeHeuristic(long key,TEntry entry) {
            safe();boolean found=super.probeHeuristic(key,entry);
            if(found) {
                lastAge=(generation-(int)(entry.data>>>10&255))&255;
                if(lastAge==1)priorHits++;if(lastAge>1)expiredHits++;
                lastEpoch=(long)P4TtMemoryTest.field(kernel,"selectiveEpoch");
            }
            return found;
        }
        @Override public boolean saveHeuristicAged(long key,int depth,int type,int score,long move) {
            safe();return super.saveHeuristicAged(key,depth,type,score,move);
        }
        @Override public boolean saveHintAged(long key,int depth,long move) {
            safe();return super.saveHintAged(key,depth,move);
        }
        @Override public void heuristicCutoff() {
            safe();assertTrue(lastAge<=1);
            assertEquals(lastEpoch+1,(long)P4TtMemoryTest.field(kernel,"selectiveEpoch"));
            if(lastAge==1)priorCutoffs++;
        }
    }

    @Test void priorRequestReuseIsReachedSelectiveAndExpires() throws Exception {
        assertEquals(3407871,BaselineEngine.configuration(1,64).mechanisms().flags());
        assertEquals(7602175,P4TtMemoryTest.config().flags());assertEquals(15990783,config().flags());
        assertThrows(IllegalArgumentException.class,
                ()->new FlatSearchConfig(1L<<FlatSearchConfig.Feature.AGED_HEURISTIC_TT_MEMORY.ordinal()));
        var model=ConventionalNnueEngine.load();var table=new Observed();
        var exact=ExactSearch.constructionResearch(ExactEvaluator.from(model.evaluation()),table,config());table.kernel=exact;
        var board=Board.startingPosition();var saved=board.clone();
        for(int request=0;request<2;request++) {
            exact.beginRequest();
            try {
                var result=exact.search(board,5);assertTrue(result.completed());assertTrue(result.selective());
                Phase2Oracle.legalPv(board,result.principalVariation());assertArrayEquals(saved,board);
            }finally {exact.endRequest();}
        }
        assertTrue(table.priorHits>0);assertTrue(table.priorCutoffs>0);
        exact.beginRequest();exact.endRequest(); // no scores produced in this intervening generation
        table.priorCutoffs=0;table.expiredHits=0;
        exact.beginRequest();
        try {assertTrue(exact.search(board,5).completed());}finally {exact.endRequest();}
        assertTrue(table.expiredHits>0);assertEquals(0,table.priorCutoffs);
        assertArrayEquals(saved,board);assertNull(P4TtMemoryTest.field(exact,"history"));
    }

    @Test void legalNullVerificationCannotReadOrPublishAgedMemory() throws Exception {
        var model=ConventionalNnueEngine.load();var table=new Observed();
        var exact=ExactSearch.constructionResearch(ExactEvaluator.from(model.evaluation()),table,config());table.kernel=exact;
        var diagnostics=new NullMovePolicy.Diagnostics();exact.withFlatNullObservation(false,diagnostics);
        var board=Board.startingPosition();var saved=board.clone();
        var result=exact.searchRootMove(board,GameHistory.initial(board),4,
                ExhaustiveOracle.legalMoves(board)[0],1000,1001,true,()->false);
        assertTrue(result.completed());assertTrue(diagnostics.verifications>0);
        assertArrayEquals(saved,board);Phase2Oracle.legalPv(board,result.principalVariation());
        assertNull(P4TtMemoryTest.field(exact,"history"));
        assertEquals(false,P4TtMemoryTest.field(exact,"nullProbeActive"));
        assertEquals(false,P4TtMemoryTest.field(exact,"nullVerification"));
    }

    @Test void incrementalAndRebuiltEvaluationAgreeAcrossRequestsAndChessBoundaries() throws Exception {
        var model=ConventionalNnueEngine.load();
        for(String fen:List.of(Board.FEN_STARTING_POSITION,
                "r3k2r/pppq1ppp/2n1bn2/3pp3/3PP3/2N1BN2/PPPQ1PPP/R3K2R w KQkq - 0 1",
                "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1","7k/P7/8/8/8/8/8/7K w - - 0 1",
                "7k/6Q1/5K2/8/8/8/8/8 b - - 100 1","7k/5Q2/6K1/8/8/8/8/8 b - - 100 1")) {
            var board=Board.fromFen(fen);var saved=board.clone();
            try(var a=new SearchDriver(construction(1,8).create(model.evaluation()));
                var b=new SearchDriver(construction(1,8).create(SearchEvaluation.conventionalFullRebuild(model.model())))) {
                for(int request=0;request<2;request++) {
                    var first=a.search(new SearchRequest(board,4));var second=b.search(new SearchRequest(board,4));
                    assertTrue(first.targetDepthCompleted());assertEquals(second.nodes(),first.nodes());
                    assertEquals(second.lastCompletedResult().score(),first.lastCompletedResult().score());
                    assertArrayEquals(second.lastCompletedResult().principalVariation(),first.lastCompletedResult().principalVariation());
                    Phase2Oracle.legalPv(board,first.lastCompletedResult().principalVariation());assertArrayEquals(saved,board);
                }
            }
        }
    }

    @Test void allSupportedWorkersAccountDrainCancelAndRestart() throws Exception {
        var model=ConventionalNnueEngine.load();var board=Board.startingPosition();var saved=board.clone();
        for(int workers=1;workers<=16;workers++)try(var driver=new SearchDriver(construction(workers,1).create(model.evaluation()))) {
            for(long budget:new long[]{1,100,4000}) {
                var control=SearchControl.controlled(budget,System.nanoTime(),30_000_000_000L,TimeSource.SYSTEM);
                var result=driver.search(new SearchRequest(board,GameHistory.initial(board),40,control));
                assertEquals(budget,result.nodes());assertEquals(control.nodes(),result.nodes());assertEquals(0,driver.activeSearchThreads());
                assertArrayEquals(saved,board);
                if(result.lastCompletedResult()!=null)Phase2Oracle.legalPv(board,result.lastCompletedResult().principalVariation());
                // Keep TT state between requests here, unlike the H1 reset fixture.
            }
            var expired=SearchControl.controlled(10000,System.nanoTime()-10000,1,TimeSource.SYSTEM);
            assertNull(driver.search(new SearchRequest(board,GameHistory.initial(board),10,expired)).lastCompletedResult());
            assertEquals(SearchTermination.TIME_LIMIT,expired.termination());assertEquals(0,driver.activeSearchThreads());
            assertTrue(driver.search(new SearchRequest(board,3)).targetDepthCompleted());
            driver.newGame();assertTrue(driver.search(new SearchRequest(board,3)).targetDepthCompleted());
        }
    }
}
