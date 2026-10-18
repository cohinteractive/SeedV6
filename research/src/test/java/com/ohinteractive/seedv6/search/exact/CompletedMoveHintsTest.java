package com.ohinteractive.seedv6.search.exact;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.tt.TTable;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(90)
class CompletedMoveHintsTest {
    @Test void selectiveCompletedRootPublishesOnlyLegalHintAndObservationIsNeutral() throws Exception {
        var e=P3InformationBaselineTest.evaluation();var board=Board.startingPosition();var game=GameHistory.initial(board);
        var table=new TTable(1);var d=new MoveHintDiagnostics();
        var candidate=P3InformationBaseline.engine(e,table,null).withCompletedHints(true,d);
        var plain=P3InformationBaseline.engine(e,new TTable(1),null);
        var observation=P3InformationBaseline.engine(e,new TTable(1),null).withCompletedHints(false,new MoveHintDiagnostics());
        candidate.beginRequest();plain.beginRequest();observation.beginRequest();long hits=0;
        try {for(int depth=1;depth<=5;depth++) {
            P3InformationBaseline.same(plain.search(board,game,depth,()->false),observation.search(board,game,depth,()->false));
            var result=candidate.search(board,game,depth,()->false);assertTrue(result.completed());assertTrue(result.selective());
            Phase2Oracle.legalPv(board,result.principalVariation());
            long key=SearchKey.key(board,SearchKey.rootHistory(board,game));
            assertEquals(result.bestMove(),table.probeHint(key));assertFalse(table.probe(key,new TTable.TEntry()));
            assertTrue(d.stored>0);assertEquals(0,d.illegal);hits+=d.hits;
        }} finally {candidate.endRequest();plain.endRequest();observation.endRequest();}
        assertTrue(hits>0);
    }

    @Test void conservativeIdentityAndCurrentLegalityRemainMandatory() throws Exception {
        var e=P3InformationBaselineTest.evaluation();var board=Board.startingPosition();var game=GameHistory.initial(board);
        var changed=GameHistory.builder(game).appendPosition(board).snapshot();
        long key=SearchKey.key(board,SearchKey.rootHistory(board,game));
        long other=SearchKey.key(board,SearchKey.rootHistory(board,changed));
        // With clock zero preceding history is immaterial; status still participates.
        long[] altered=board.clone();altered[Board.STATUS]^=1L<<40;
        assertNotEquals(key,SearchKey.key(altered,SearchKey.rootHistory(board,game)));
        var table=new TTable(1);table.saveHint(key,5,Long.MAX_VALUE);
        var d=new MoveHintDiagnostics();var search=P3InformationBaseline.engine(e,table,null).withCompletedHints(true,d);
        var result=search.search(board,2);assertTrue(result.completed());assertTrue(d.illegal>0);
        Phase2Oracle.legalPv(board,result.principalVariation());
        assertEquals(key,other);
    }

    @Test void ordinaryStaticExactValuesStillAgreeWithIndependentEnumeration() {
        ExactEvaluator hce=(b,p)->Eval.evaluate(b);
        for(String fen:CheckExtensionTest.FENS) {
            var board=Board.fromFen(fen);var game=GameHistory.initial(board);
            var candidate=new ExactSearch(hce,new TTable(1)).withCompletedHints(true,null);candidate.beginRequest();
            try {for(int depth=1;depth<=2;depth++) {
                int expected=new ExhaustiveOracle(hce).score(board,new com.ohinteractive.seedv6.rules.SearchLineHistory(game),depth,0);
                var actual=candidate.search(board,game,depth,()->false);
                assertEquals(expected,actual.score());assertTrue(actual.completed());
                Phase2Oracle.legalPv(board,actual.principalVariation());
            }} finally {candidate.endRequest();}
        }
    }

    @Test void cancelledRootPublishesNoNewRootHintAndCombinedHistoriesRemainSafe() throws Exception {
        var e=P3InformationBaselineTest.evaluation();var board=Board.startingPosition();var game=GameHistory.initial(board);
        for(boolean filter:new boolean[]{false,true}) {
            var table=new TTable(1);var search=P3InformationBaseline.engine(e,table,null)
                    .withHistoryResearch(true,filter,null).withCompletedHints(true,null);
            search.beginRequest();
            try {
                var aborted=search.search(board,game,6,()->search.visitedNodes()>200);
                assertFalse(aborted.completed());assertEquals(0,aborted.principalVariation().length);
                long key=SearchKey.key(board,SearchKey.rootHistory(board,game));
                assertEquals(0,table.probeHint(key));assertFalse(table.probe(key,new TTable.TEntry()));
                assertTrue(Arrays.stream(P3InformationBaseline.history(search)).allMatch(n->n==0));
                var result=search.search(board,game,3,()->false);assertTrue(result.completed());Phase2Oracle.legalPv(board,result.principalVariation());
            } finally {search.endRequest();}
        }
    }

    @Test void rootReuseSharesOnlyCoherentHintsAndDrainsBeforeReuse() throws Exception {
        var e=P3InformationBaselineTest.evaluation();var board=Board.startingPosition();var game=GameHistory.initial(board);
        var table=new TTable(1);
        try(var parallel=new ParallelSearch(4,e,table,ConcurrencyPolicy.ROOT_REUSE)
                .withSeeResearch(SeePolicy.QXRAY,null).withCompletedHints(true,null)) {
            parallel.beginRequest();
            try {
                var result=parallel.search(new com.ohinteractive.seedv6.search.common.SearchRequest(board,game,4));
                assertTrue(result.completed());assertTrue(result.selective());assertEquals(0,parallel.activeSearchThreads());
                Phase2Oracle.legalPv(board,result.principalVariation());
                long key=SearchKey.key(board,SearchKey.rootHistory(board,game));
                assertFalse(table.probe(key,new TTable.TEntry()));assertEquals(0,table.probeHint(key));
            } finally {parallel.endRequest();}
            parallel.newGame();parallel.beginRequest();
            try {
                var control=com.ohinteractive.seedv6.search.common.SearchControl.controlled(200,System.nanoTime(),
                        5_000_000_000L,com.ohinteractive.seedv6.search.common.TimeSource.SYSTEM);
                var stopped=parallel.search(new com.ohinteractive.seedv6.search.common.SearchRequest(board,game,8,control));
                assertFalse(stopped.completed());assertEquals(0,parallel.activeSearchThreads());
            } finally {parallel.endRequest();}
        }
    }
}
