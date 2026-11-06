package com.ohinteractive.seedv6.search.exact;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.manage.ClockPolicy;
import com.ohinteractive.seedv6.search.tt.TTable;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(60)
class ClvCriticalLineTest {
    static long entry(int depth,int bound,int score,int generation) {
        return ((long)score<<32)|((long)generation<<10)|((long)bound<<8)|depth;
    }
    @Test void reliefRequiresRecentNearDepthCompetitiveLowerEvidence() {
        for(int bound:new int[]{TTable.TYPE_EXACT,TTable.TYPE_LOWER})
            for(int depth:new int[]{4,5})
                assertTrue(ExactSearch.criticalTtRelief(entry(depth,bound,30,7),7,6,2,20,21));
        assertFalse(ExactSearch.criticalTtRelief(entry(4,TTable.TYPE_UPPER,30,7),7,6,2,20,21));
        assertFalse(ExactSearch.criticalTtRelief(entry(4,TTable.TYPE_LOWER,20,7),7,6,2,20,21));
        assertFalse(ExactSearch.criticalTtRelief(entry(4,TTable.TYPE_LOWER,30,6),7,6,2,20,21));
        for(int depth:new int[]{0,3,6,8})
            assertFalse(ExactSearch.criticalTtRelief(entry(depth,TTable.TYPE_LOWER,30,7),7,6,2,20,21));
    }
    @Test void rootWideWindowShallowAndDecisiveDomainsAreExcluded() {
        long data=entry(4,TTable.TYPE_LOWER,30,7);
        assertFalse(ExactSearch.criticalTtRelief(data,7,6,0,20,21));
        assertFalse(ExactSearch.criticalTtRelief(data,7,6,1,20,30));
        assertFalse(ExactSearch.criticalTtRelief(entry(2,TTable.TYPE_LOWER,30,7),7,3,1,20,21));
        assertFalse(ExactSearch.criticalTtRelief(entry(4,TTable.TYPE_LOWER,ExactSearch.MATE_SCORE,7),7,6,1,20,21));
        assertFalse(ExactSearch.criticalTtRelief(data,7,6,1,-ExactSearch.MATE_SCORE,-ExactSearch.MATE_SCORE+1));
    }
    @Test void explicitDependencyAndHistoricalIsolation() {
        assertThrows(IllegalArgumentException.class,()->FlatSearchConfig.exact().with(FlatSearchConfig.Feature.TT_CRITICAL_RELIEF,true));
        for(int workers=1;workers<=16;workers++) {
            var c=BaselineEngine.configuration(workers,64,BaselineEngine.SearchProfile.MW_C4);
            assertEquals(1182007295L,c.mechanisms().flags());
            assertFalse(c.mechanisms().has(FlatSearchConfig.Feature.TT_CRITICAL_RELIEF));
            assertEquals(3329490943L,c.mechanisms().with(FlatSearchConfig.Feature.TT_CRITICAL_RELIEF,true).flags());
            var adopted=BaselineEngine.configuration(workers,64);
            assertEquals(ClvStudy.configuration("c1"),adopted.mechanisms());
            assertEquals(c.rootScheduling(),adopted.rootScheduling());
            assertEquals(ClvStudy.configuration("control"),c.mechanisms());
            assertFalse(FlatSearchConfig.exact().has(FlatSearchConfig.Feature.TT_CRITICAL_RELIEF));
        }
    }
    @Test void normalFactoryActuallyConsumesReliefAndMatchesFrozenW1Traversal() throws Exception {
        var binding=BaselineEngine.load();
        var roots=P3HistoryStudy.roots(java.nio.file.Path.of("../docs/research/search/p3-construction-2026-10-20/diagnostic-corpus.tsv"));
        for(int workers:new int[]{1,2,4,8,16}) {
            var production=binding.create(workers,1);
            var kernel=(SingleDepthSearch)P4TtFoundationStudy.field(P4TtFoundationStudy.field(production,"owner"),"owner");
            var frozen=new ConstructionSearch(ClvStudy.configuration("c1"),workers,1,AspirationSearch.Policy.ADAPTIVE16,
                    workers==1?ConstructionSearch.RootScheduling.STANDARD:ConstructionSearch.RootScheduling.EAGER_ROOT);
            try(var driver=new SearchDriver(production);
                var reference=workers==1?new SearchDriver(ClockPolicy.BOUNDARY2.apply(frozen.create(binding.evaluation()))):null) {
                for(var root:roots.subList(0,4)) {
                    var saved=root.board().clone();
                    var control=SearchControl.controlled(250000,System.nanoTime(),20_000_000_000L,TimeSource.SYSTEM);
                    var result=driver.search(new SearchRequest(root.board(),root.history(),30,SearchObserver.NONE,control,true));
                    assertEquals(SearchTermination.NODE_LIMIT,control.termination());
                    assertEquals(250000,result.nodes());assertEquals(result.nodes(),result.diagnostics().totalEnteredNodes());
                    assertNotNull(result.lastCompletedResult());Phase2Oracle.legalPv(root.board(),result.lastCompletedResult().principalVariation());
                    assertArrayEquals(saved,root.board());assertEquals(0,driver.activeSearchThreads());
                    if(reference!=null) {
                        var referenceControl=SearchControl.controlled(250000,System.nanoTime(),20_000_000_000L,TimeSource.SYSTEM);
                        var expected=reference.search(new SearchRequest(root.board(),root.history(),30,SearchObserver.NONE,referenceControl,true));
                        ConstructionTtStudy.same(result,expected);
                    }
                }
                long[] counters=ClvStudy.counters(kernel);
                assertTrue(counters[0]>0,"No eligible TT evidence at W"+workers);
                assertTrue(counters[1]>0,"No actual reduction relief at W"+workers);
                System.out.println("CLV production W"+workers+" nodes=1000000 signals="+counters[0]+" reliefs="+counters[1]);
                driver.newGame();
                assertTrue(driver.search(new SearchRequest(Board.startingPosition(),3)).targetDepthCompleted());
                assertEquals(0,driver.activeSearchThreads());
            }
        }
    }
    @Test void candidateHonorsAdmissionRestorationDrainAndReuseAcrossWorkers() throws Exception {
        var model=ConventionalNnueEngine.load();var board=Board.startingPosition();var saved=board.clone();
        for(int workers:new int[]{1,2,4,8,16}) {
            var c=BaselineEngine.configuration(workers,1);
            var candidate=new ConstructionSearch(c.mechanisms().with(FlatSearchConfig.Feature.TT_CRITICAL_RELIEF,true),
                    workers,1,c.aspiration(),c.rootScheduling());
            try(var driver=new SearchDriver(ClockPolicy.BOUNDARY2.apply(candidate.create(model.evaluation())))) {
                for(int cap:new int[]{1,100,5000}) {
                    var control=SearchControl.controlled(cap,System.nanoTime(),10_000_000_000L,TimeSource.SYSTEM);
                    var r=driver.search(new SearchRequest(board,GameHistory.initial(board),20,SearchObserver.NONE,control,true));
                    assertEquals(SearchTermination.NODE_LIMIT,control.termination());assertEquals(cap,r.nodes());
                    assertEquals(cap,r.diagnostics().totalEnteredNodes());assertEquals(0,driver.activeSearchThreads());
                    assertTrue(Arrays.equals(saved,board));
                    if(r.lastCompletedResult()!=null) Phase2Oracle.legalPv(board,r.lastCompletedResult().principalVariation());
                    driver.newGame();
                }
                var r=driver.search(new SearchRequest(board,GameHistory.initial(board),2));
                assertTrue(r.targetDepthCompleted());Phase2Oracle.legalPv(board,r.lastCompletedResult().principalVariation());
                assertArrayEquals(saved,board);
            }
        }
        assertEquals(0,Thread.getAllStackTraces().keySet().stream().filter(t->t.isAlive()&&t.getName().equals("seedv6-search-helper")).count());
    }
}
