package com.ohinteractive.seedv6.search.exact;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.manage.ClockPolicy;
import com.ohinteractive.seedv6.search.tt.TTable;

@Timeout(90)
class P4MultiworkerMemoryTest {
    @Test void scoreSuppressionRetainsSharedHintsAndRejectsInventedConsumption() {
        long[] moves=ExhaustiveOracle.legalMoves(Board.startingPosition());
        for(var mode:new P4MultiworkerMemoryStudy.Delivery[]{P4MultiworkerMemoryStudy.Delivery.SHARED_SCORES,
                P4MultiworkerMemoryStudy.Delivery.PRIVATE_SCORES}) {
            var table=new P4MultiworkerMemoryStudy.ScoreTable(2,1,mode);table.advanceGeneration();table.bind(0);
            assertTrue(table.saveHeuristic(1,6,TTable.TYPE_LOWER,12,moves[0]));
            table.bind(1);assertFalse(table.saveHeuristic(1,2,TTable.TYPE_UPPER,8,moves[1]));
            assertEquals(0,table.producers.get(1L).worker());
            // An independent newer hint must not falsely claim authorship of the score.
            assertTrue(table.saveHint(1,7,moves[1]));
            boolean shared=mode==P4MultiworkerMemoryStudy.Delivery.SHARED_SCORES;
            assertEquals(shared,table.probeHeuristic(1,new TTable.TEntry()));
            assertEquals(moves[1],table.probeHint(1));
            if(shared) { table.heuristicCutoff();assertEquals(1,table.consumedFrom[0][1]); }
            else assertThrows(AssertionError.class,table::heuristicCutoff);
            assertEquals(1,table.availableFrom[0][1]);
            assertThrows(AssertionError.class,table::heuristicCutoff);
            assertFalse(table.probeHeuristic(2,new TTable.TEntry()));
            table.clear();assertFalse(table.probeHeuristic(1,new TTable.TEntry()));assertTrue(table.producers.isEmpty());
        }
    }

    @Test void consumptionUsesDeliveredSnapshotEvenAfterAnotherPublication() throws Exception {
        long move=ExhaustiveOracle.legalMoves(Board.startingPosition())[0];
        var table=new P4MultiworkerMemoryStudy.ScoreTable(2,1,P4MultiworkerMemoryStudy.Delivery.SHARED_SCORES);
        table.advanceGeneration();table.bind(0);assertTrue(table.saveHeuristic(1,4,TTable.TYPE_EXACT,10,move));
        table.bind(1);assertTrue(table.probeHeuristic(1,new TTable.TEntry()));
        var other=new Thread(()->{table.bind(1);table.saveHeuristic(1,4,TTable.TYPE_EXACT,20,move);});
        other.start();other.join();assertEquals(1,table.producers.get(1L).worker());
        table.heuristicCutoff();assertEquals(1,table.consumedFrom[0][1]);assertEquals(0,table.consumedFrom[1][1]);
        assertThrows(AssertionError.class,()->table.save(1,1,0,0,move));
    }

    @Test void integratedMemoryMaintainsGlobalAccountingAndDrainAcrossWorkerDomains() throws Exception {
        var model=ConventionalNnueEngine.load();var board=Board.startingPosition();var saved=board.clone();
        for(var schedule:P4MultiworkerMemoryStudy.Schedule.values())for(int workers:new int[]{2,4,8,16}) {
            var table=new P4MultiworkerMemoryStudy.ScoreTable(workers,1,P4MultiworkerMemoryStudy.Delivery.SHARED_SCORES);
            var counts=new P4MultiworkerStudy.Admissions[workers];
            var owner=P4MultiworkerMemoryStudy.owner(schedule,workers,model,table,counts);
            try(var driver=new SearchDriver(ClockPolicy.BOUNDARY2.apply(new AspirationSearch(owner,AspirationSearch.Policy.ADAPTIVE16)))) {
                for(int cap:new int[]{1,100,5000}) {
                    for(var count:counts)count.reset();
                    var control=SearchControl.controlled(cap,0,-1,TimeSource.SYSTEM);
                    var result=driver.search(new SearchRequest(board,GameHistory.initial(board),20,SearchObserver.NONE,control,true));
                    assertEquals(cap,result.nodes());assertEquals(cap,control.nodes());
                    assertEquals(cap,Arrays.stream(counts).mapToLong(a->a.nodes).sum());
                    assertEquals(cap,result.diagnostics().totalEnteredNodes());assertEquals(0,owner.activeSearchThreads());
                    assertArrayEquals(saved,board);
                    if(result.lastCompletedResult()!=null)Phase2Oracle.legalPv(board,result.lastCompletedResult().principalVariation());
                }
                driver.newGame();var stop=SearchControl.controlled(-1,0,-1,TimeSource.SYSTEM);stop.request(SearchTermination.STOPPED);
                assertEquals(0,driver.search(new SearchRequest(board,GameHistory.initial(board),8,stop)).nodes());
                assertTrue(driver.search(new SearchRequest(board,3)).targetDepthCompleted());
            }
        }
    }

    @Test void tcP1TacticalVerificationCannotReadOrPublishHeuristicMemory() throws Exception {
        var model=ConventionalNnueEngine.load();var board=Board.startingPosition();var saved=board.clone();
        var config=BaselineEngine.configuration(1,8,BaselineEngine.SearchProfile.TC_P1).mechanisms()
                .with(FlatSearchConfig.Feature.HEURISTIC_TT_MEMORY,true);
        assertEquals(1182007295L,config.flags());
        // Existing observation checks the live kernel's synthetic/legal-verifier flags
        // on every score production, probe and cutoff, now with TC-P1 composition.
        var table=new P4TtMemoryTest.Observed();
        var exact=ExactSearch.constructionResearch(ExactEvaluator.from(model.evaluation()),table,config)
                .withTacticalObservation(true);table.kernel=exact;
        var result=exact.search(board,GameHistory.initial(board),8,()->exact.visitedNodes()>=1_000_000);
        assertTrue(exact.constructionCounters().get("tacticalCandidates")>0,"No real tactical verifier reached");
        assertTrue(table.probes>0);assertTrue(table.attempts>0);assertArrayEquals(saved,board);
        assertEquals(false,P4TtMemoryTest.field(exact,"tacticalProbeActive"));
        assertEquals(false,P4TtMemoryTest.field(exact,"nullVerification"));
        assertNull(P4TtMemoryTest.field(exact,"history"));
        if(result.completed())Phase2Oracle.legalPv(board,result.principalVariation());
        assertTrue(exact.search(board,2).completed());
    }
}
