package com.ohinteractive.seedv6.search.exact;

import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brnpair2.ProductionModel;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.driver.ProductionSearch;
import com.ohinteractive.seedv6.search.driver.SearchDriver;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(90)
class P3InformationBaselineTest {
    static SearchEvaluation evaluation() throws Exception {
        return SearchEvaluation.brnPair2(ProductionModel.load(Path.of("../build-assets/production/bre-pair2-g32.brn-pair2")));
    }

    @Test void observedCumulativeCompositionMatchesActualProductionFactory() throws Exception {
        var evaluation=evaluation();var board=Board.startingPosition();var game=GameHistory.initial(board);
        var table=new P3InformationBaseline.ObservedTable(64);var d=new InternalSearchPolicy.Diagnostics();
        var admissions=new P3InformationBaseline.Admissions(evaluation);
        var observed=P3InformationBaseline.engine(admissions,table,d);observed.beginRequest();
        ExactSearchResult last=null;long entered=0;
        try { for(int depth=1;depth<=4;depth++) {last=observed.search(board,game,depth,()->false);entered+=admissions.nodes;} }
        finally {observed.endRequest();}
        try(var driver=new SearchDriver(ProductionSearch.create(1,evaluation))) {
            var result=driver.search(new SearchRequest(board,game,4));
            assertEquals(last.score(),result.lastCompletedResult().score());
            assertEquals(last.bestMove(),result.lastCompletedResult().bestMove());
            assertArrayEquals(last.principalVariation(),result.lastCompletedResult().principalVariation());
            assertEquals(entered,result.nodes());
            assertTrue(table.probes>0);assertTrue(d.entries>0);
        }
    }

    @Test void observationIsNeutralAndAbortClearsHistory() throws Exception {
        var evaluation=evaluation();var board=Board.startingPosition();var game=GameHistory.initial(board);
        var plain=P3InformationBaseline.engine(evaluation,new TTable(1),null);
        var observed=P3InformationBaseline.engine(evaluation,new P3InformationBaseline.ObservedTable(1),new InternalSearchPolicy.Diagnostics());
        plain.beginRequest();observed.beginRequest();
        try {
            for(int depth=1;depth<=4;depth++) P3InformationBaseline.same(
                    plain.search(board,game,depth,()->false),observed.search(board,game,depth,()->false));
            assertTrue(Arrays.stream(P3InformationBaseline.history(observed)).anyMatch(n->n!=0));
            var a=plain.search(board,game,6,()->plain.visitedNodes()>200);
            var b=observed.search(board,game,6,()->observed.visitedNodes()>200);
            P3InformationBaseline.same(a,b);assertFalse(b.completed());
            assertTrue(Arrays.stream(P3InformationBaseline.history(observed)).allMatch(n->n==0));
        } finally {plain.endRequest();observed.endRequest();}
    }
}
