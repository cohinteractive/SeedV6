package com.ohinteractive.seedv6.search.exact;

import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;
import static com.ohinteractive.seedv6.search.exact.FlatSearchConfig.Feature.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(180)
class Cp64SelectivityLazyTest {
    static FlatSearchConfig lazy() {return FlatSearchConfig.candidate().with(LAZY_IMPROVING,true);}
    static ExactSearch exact(ExactSearchAdapter adapter) throws Exception {
        var field=ExactSearchAdapter.class.getDeclaredField("exact");field.setAccessible(true);return (ExactSearch)field.get(adapter);
    }
    @Test void demandEvaluationRetainsFullRequestTraversalIncludingAbortsAndRetries() throws Exception {
        var model=ConventionalNnueEngine.load();
        var roots=P3HistoryStudy.roots(Path.of("../docs/research/search/p3-construction-2026-10-20/diagnostic-corpus.tsv"));
        long[] evaluations={0,0},events={0,0};
        for(var root:roots.subList(0,12)) {
            var eager=ExactSearchAdapter.constructionResearch(model.evaluation(),new TTable(8),FlatSearchConfig.candidate());
            var demand=ExactSearchAdapter.constructionResearch(model.evaluation(),new TTable(8),lazy());
            var x=exact(eager);var y=exact(demand);
            y.withSelectivityTrace(new SelectivityTrace() {
                @Override public void survivor(long[] b,long[] path,int ply,int depth,int alpha,int beta,boolean scout,boolean checked,int raw){events[0]++;}
                @Override public void reduction(long[] b,long move,long[] path,int ply,int depth,int ordinal,int quiets,
                        int alpha,int beta,int history,int continuation,int raw,int prior,int base,int assigned,boolean caution) {
                    events[1]++;assertTrue(assigned>=0 && assigned<=base);assertTrue(base-assigned<=1);
                }
            });
            try(var a=new SearchDriver(new AspirationSearch(eager,AspirationSearch.Policy.ADAPTIVE16,
                        (d,l,u,r,n)->evaluations[0]+=x.constructionCounters().get("evaluations")));
                var b=new SearchDriver(new AspirationSearch(demand,AspirationSearch.Policy.ADAPTIVE16,
                        (d,l,u,r,n)->evaluations[1]+=y.constructionCounters().get("evaluations")))) {
                var left=ConstructionTtAdaptiveStudy.request(a,root,8,250000);
                var right=ConstructionTtAdaptiveStudy.request(b,root,8,250000);
                ConstructionTtStudy.same((SearchDriverOutcome)left.get("outcome"),(SearchDriverOutcome)right.get("outcome"));
            }
        }
        assertTrue(events[0]>0 && events[1]>0);assertTrue(evaluations[1]<evaluations[0]);
        System.out.printf("lazy improving identical request traversal; eager evaluations=%d lazy=%d%n",evaluations[0],evaluations[1]);
    }
    @Test void ancestorReadsMatchIndependentRebuildAndPreserveDefaultIdentity() throws Exception {
        assertEquals(3407871,BaselineEngine.configuration(1,64).mechanisms().flags());
        assertEquals(262143,Cp64SelectivityLab.configuration("baseline",1,64).mechanisms().flags());
        assertFalse(BaselineEngine.configuration(1,64).mechanisms().has(LAZY_IMPROVING));
        assertThrows(IllegalArgumentException.class,()->FlatSearchConfig.exact().with(LAZY_IMPROVING,true));
        var model=ConventionalNnueEngine.load();
        for(String fen:List.of(Board.FEN_STARTING_POSITION,SeePruningTest.MIDDLE,
                "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1","7k/P7/8/8/8/8/8/7K w - - 0 1")) {
            var board=Board.fromFen(fen);var root=new P3HistoryStudy.Root(fen,board,GameHistory.initial(board));
            var config=new ConstructionSearch(lazy(),1,8,AspirationSearch.Policy.ADAPTIVE16);
            try(var a=new SearchDriver(config.create(model.evaluation()));
                var b=new SearchDriver(config.create(SearchEvaluation.conventionalFullRebuild(model.model())))) {
                ConstructionTtStudy.same((SearchDriverOutcome)ConstructionTtAdaptiveStudy.request(a,root,6,50000).get("outcome"),
                        (SearchDriverOutcome)ConstructionTtAdaptiveStudy.request(b,root,6,50000).get("outcome"));
            }
        }
    }
    @Test void lazyOwnersRespectEveryWorkerBudgetExpiryDrainAndReuse() throws Exception {
        var e=BaselineEngine.load().evaluation();var board=Board.startingPosition();var saved=board.clone();
        for(int workers=1;workers<=16;workers++) try(var driver=new SearchDriver(
                new ConstructionSearch(lazy(),workers,1,AspirationSearch.Policy.ADAPTIVE16).create(e))) {
            var budget=SearchControl.controlled(2000,System.nanoTime(),30_000_000_000L,TimeSource.SYSTEM);
            var result=driver.search(new SearchRequest(board,GameHistory.initial(board),40,budget));
            assertEquals(2000,result.nodes());assertEquals(0,driver.activeSearchThreads());assertArrayEquals(saved,board);
            if(result.lastCompletedResult()!=null) Phase2Oracle.legalPv(board,result.lastCompletedResult().principalVariation());
            var expired=SearchControl.controlled(2000,System.nanoTime()-10000,1,TimeSource.SYSTEM);
            assertNull(driver.search(new SearchRequest(board,GameHistory.initial(board),40,expired)).lastCompletedResult());
            assertEquals(SearchTermination.TIME_LIMIT,expired.termination());assertEquals(0,driver.activeSearchThreads());
            driver.newGame();assertTrue(driver.search(new SearchRequest(board,2)).targetDepthCompleted());
        }
    }
}
