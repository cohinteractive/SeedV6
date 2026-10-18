package com.ohinteractive.seedv6.search.exact;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.tt.TTable;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(90)
class HistoryLearningTest {
    @Test void censoredFailureIsNotAClaimedNominalLabelAndUnsearchedMovesNeverLearn() {
        var board=Board.startingPosition();
        long[] moves=Arrays.stream(ExhaustiveOracle.legalMoves(board))
                .filter(m->Move.coordinate(m).equals("e2e4") || Move.coordinate(m).equals("d2d4")
                        || Move.coordinate(m).equals("g1f3")).toArray();
        assertEquals(3,moves.length);
        int[] baseline=new int[QuietHistory.SIZE],filtered=baseline.clone(),observed=baseline.clone();
        var d=new HistoryLearning.Diagnostics();var labels=new HistoryLearning(true,d);
        labels.searched(3,0,false);labels.searched(3,1,true);
        labels.cutoff(filtered,moves,1,-1,4,3);
        assertEquals(0,filtered[QuietHistory.index(moves[0])]);
        assertEquals(16,filtered[QuietHistory.index(moves[1])]);
        assertEquals(0,filtered[QuietHistory.index(moves[2])]);
        assertEquals(1,d.filteredMaluses);assertEquals(1,d.reducedMaluses);
        QuietHistory.recordCutoff(baseline,moves,1,-1,4);
        var observation=new HistoryLearning(false,null);
        observation.searched(3,0,false);observation.searched(3,1,true);
        observation.cutoff(observed,moves,1,-1,4,3);assertArrayEquals(baseline,observed);
        labels.searched(3,0,true);labels.searched(3,64,true);labels.searched(4,0,false);
        assertTrue(labels.observed(3,0));assertTrue(labels.observed(3,64));assertFalse(labels.observed(4,0));
        labels.searched(3,64,false);assertFalse(labels.observed(3,64));
    }

    @Test void learningObservationPreservesCumulativeTreeAndSeesCensoredOutcomes() throws Exception {
        var evaluation=P3InformationBaselineTest.evaluation();
        var board=Board.startingPosition();var game=GameHistory.initial(board);
        var d=new HistoryLearning.Diagnostics();
        var baseline=P3InformationBaseline.engine(evaluation,new TTable(1),null);
        var observed=P3InformationBaseline.engine(evaluation,new TTable(1),null).withHistoryResearch(false,false,d);
        baseline.beginRequest();observed.beginRequest();long reduced=0,rewards=0;
        try {
            for(int depth=1;depth<=6;depth++) {
                P3InformationBaseline.same(baseline.search(board,game,depth,()->false),observed.search(board,game,depth,()->false));
                reduced+=d.reducedMaluses;rewards+=d.rewards;
            }
        } finally {baseline.endRequest();observed.endRequest();}
        assertTrue(rewards>0);assertTrue(reduced>0);assertEquals(0,d.filteredMaluses);
    }

    @Test void retentionAndFilteringPreserveUnprunedOracleValuesAndRuleTerminals() {
        ExactEvaluator hce=(b,p)->Eval.evaluate(b);
        for(String fen:Arrays.copyOf(CheckExtensionTest.FENS,4)) for(boolean retain:new boolean[]{false,true}) {
            var board=Board.fromFen(fen);var game=GameHistory.initial(board);
            var candidate=ExactSearch.iterationReuseResearch(hce,new TTable(1),IterationReuse.EFFORT,false,null)
                    .withHistoryResearch(retain,true,null);candidate.beginRequest();
            try { for(int depth=1;depth<=2;depth++) {
                int expected=new Phase2Oracle.RestrictedOracle(hce,0)
                        .score(board,new com.ohinteractive.seedv6.rules.SearchLineHistory(game),depth,0,0,-1);
                var result=candidate.search(board,game,depth,()->false);
                assertTrue(result.completed());assertEquals(expected,result.score());Phase2Oracle.legalPv(board,result.principalVariation());
            } } finally {candidate.endRequest();}
        }
    }

    @Test void cancelledLearningCannotSeedFurtherWorkAndOwnersStayPrivate() throws Exception {
        var evaluation=P3InformationBaselineTest.evaluation();var board=Board.startingPosition();var game=GameHistory.initial(board);
        var candidate=P3InformationBaseline.engine(evaluation,null,null).withHistoryResearch(true,true,null);
        var other=P3InformationBaseline.engine(evaluation,null,null).withHistoryResearch(true,true,null);
        candidate.beginRequest();other.beginRequest();
        try {
            for(int depth=1;depth<=4;depth++) candidate.search(board,game,depth,()->false);
            assertTrue(Arrays.stream(P3InformationBaseline.history(candidate)).anyMatch(v->v!=0));
            assertTrue(Arrays.stream(P3InformationBaseline.history(other)).allMatch(v->v==0));
            var aborted=candidate.search(board,game,5,()->candidate.visitedNodes()>200);
            assertFalse(aborted.completed());assertEquals(0,aborted.principalVariation().length);
            assertTrue(Arrays.stream(P3InformationBaseline.history(candidate)).allMatch(v->v==0));
            P3InformationBaseline.same(other.search(board,game,6,()->false),candidate.search(board,game,6,()->false));
        } finally {candidate.endRequest();other.endRequest();}
    }
}
