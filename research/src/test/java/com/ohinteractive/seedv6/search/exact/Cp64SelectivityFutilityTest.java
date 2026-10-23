package com.ohinteractive.seedv6.search.exact;

import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.rules.*;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;
import static com.ohinteractive.seedv6.search.exact.FlatSearchConfig.Feature.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(180)
class Cp64SelectivityFutilityTest {
    static final List<String> PROFILES=List.of("nominal-futility","reduction-futility","reduction-no-improving","nominal-verified-null");
    @Test void omittedMovesAreRealUnadmittedAndObservationIsNeutral() throws Exception {
        var model=ConventionalNnueEngine.load();
        var roots=P3HistoryStudy.roots(Path.of("../docs/research/search/p3-construction-2026-10-20/diagnostic-corpus.tsv"));
        for(String profile:PROFILES) {
            long[] events={0};
            for(var root:roots.subList(0,3)) {
                var config=Cp64SelectivityLab.configuration(profile,1,8);
                var adapter=ExactSearchAdapter.constructionResearch(model.evaluation(),new TTable(8){
                    @Override public void save(long k,int d,int t,int s,long m){fail("Selective ordinary proof store");}
                },config.mechanisms());
                var exact=Cp64SelectivityLab.exact(adapter);
                exact.withSelectivityTrace(new SelectivityTrace(){
                    @Override public void omission(long[] b,long move,long[] path,int ply,int d,int a,int beta,
                            int raw,int h,int continuation,int assigned,int margin) {
                        events[0]++;assertTrue(d>=3 && d<=5 && ply>0);assertEquals(a+1,beta);
                        assertTrue(h<=0 && a>=0 && assigned>=1 && assigned<=2);assertTrue(raw+100*margin<=a);
                        assertEquals(config.mechanisms().has(NOMINAL_FUTILITY)?d:assigned,margin);
                        var replay=root.board();for(int i=1;i<=ply;i++)replay=ExhaustiveOracle.child(replay,path[i]);
                        assertArrayEquals(replay,b);assertFalse(QuiescenceOracle.inCheck(b));
                        assertFalse(QuiescenceOracle.tactical(b,move));assertFalse(QuiescenceOracle.inCheck(ExhaustiveOracle.child(b,move)));
                        try {
                            var history=(SearchLineHistory)Policy4ProductionTest.field(exact,"history");
                            assertEquals(root.history().size()+ply,history.size(),"No omitted child in formal history");
                        } catch(Exception failure) {throw new AssertionError(failure);}
                    }
                });
                try(var plain=new SearchDriver(config.create(model.evaluation()));
                    var traced=new SearchDriver(new AspirationSearch(adapter,AspirationSearch.Policy.ADAPTIVE16))) {
                    var a=ConstructionTtAdaptiveStudy.request(plain,root,8,250000);
                    var b=ConstructionTtAdaptiveStudy.request(traced,root,8,250000);
                    ConstructionTtStudy.same((SearchDriverOutcome)a.get("outcome"),(SearchDriverOutcome)b.get("outcome"));
                }
            }
            assertTrue(events[0]>0,profile);System.out.println(profile+" observed omissions="+events[0]);
        }
    }
    @Test void rebuildTerminalHistoryAndOwnerLifecycleRemainValid() throws Exception {
        var model=ConventionalNnueEngine.load();
        for(String profile:PROFILES) {
            var cfg=Cp64SelectivityLab.configuration(profile,1,4);
            for(String fen:List.of(SeePruningTest.MIDDLE,"4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1",
                    "7k/6Q1/5K2/8/8/8/8/8 b - - 100 1","7k/5Q2/6K1/8/8/8/8/8 b - - 100 1")) {
                var b=Board.fromFen(fen);var root=new P3HistoryStudy.Root(fen,b,GameHistory.initial(b));
                try(var rolling=new SearchDriver(cfg.create(model.evaluation()));
                    var rebuild=new SearchDriver(cfg.create(SearchEvaluation.conventionalFullRebuild(model.model())))) {
                    ConstructionTtStudy.same((SearchDriverOutcome)ConstructionTtAdaptiveStudy.request(rolling,root,7,40000).get("outcome"),
                            (SearchDriverOutcome)ConstructionTtAdaptiveStudy.request(rebuild,root,7,40000).get("outcome"));
                }
            }
            for(int workers:new int[]{1,4,16})try(var driver=new SearchDriver(Cp64SelectivityLab.configuration(profile,workers,1).create(model.evaluation()))) {
                var b=Board.startingPosition();var saved=b.clone();
                var control=SearchControl.controlled(20000,System.nanoTime(),30_000_000_000L,TimeSource.SYSTEM);
                var r=driver.search(new SearchRequest(b,GameHistory.initial(b),40,control));assertEquals(20000,r.nodes());
                assertEquals(0,driver.activeSearchThreads());assertArrayEquals(saved,b);
                if(r.lastCompletedResult()!=null)Phase2Oracle.legalPv(b,r.lastCompletedResult().principalVariation());
                var expired=SearchControl.controlled(2000,System.nanoTime()-10000,1,TimeSource.SYSTEM);
                assertNull(driver.search(new SearchRequest(b,GameHistory.initial(b),40,expired)).lastCompletedResult());
                assertEquals(SearchTermination.TIME_LIMIT,expired.termination());assertEquals(0,driver.activeSearchThreads());
                driver.newGame();assertTrue(driver.search(new SearchRequest(b,2)).targetDepthCompleted());
            }
        }
        assertEquals(3407871,BaselineEngine.configuration(1,64).mechanisms().flags());
        assertEquals(262143,Cp64SelectivityLab.configuration("baseline",1,64).mechanisms().flags());
        assertThrows(IllegalArgumentException.class,()->FlatSearchConfig.exact().with(REDUCTION_FUTILITY,true));
        assertThrows(IllegalArgumentException.class,()->FlatSearchConfig.candidate().with(REDUCTION_FUTILITY,true).with(NOMINAL_FUTILITY,true));
    }
}
