package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.*;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.training.validation.*;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BrnLearningProbeTest {
    @Test void researchDriverMatchesProductionEvaluatorOnNonzeroLearnedResidual() {
        var trainer=new Brn3Trainer(71);var root=Board.startingPosition();
        for(int i=0;i<4;i++)trainer.trainBatch(new long[][]{root},new double[]{.4},1);
        var model=trainer.snapshot();var config=new ValidationConfig(4,2837,6,10,3,1,NnueScoreMapping.V1,512);
        for(int i=0;i<4;i++) {
            var opening=ValidationArena.opening(root,GameHistory.initial(root),config,i);
            try(var probe=BrnLearningProbe.driver(BrnLearningProbe.evaluator(model,Brn3SearchCalibration.RESIDUAL_GAIN));
                var production=new SearchDriver(new ExactSearchAdapter(SearchEvaluation.brn3(model),new TTable(4)))) {
                var request=new SearchRequest(opening.board(),opening.history(),3);
                var expected=production.search(request).lastCompletedResult();var actual=probe.search(request).lastCompletedResult();
                assertEquals(expected.score(),actual.score());assertEquals(expected.bestMove(),actual.bestMove());
                assertEquals(expected.nodes(),actual.nodes());assertArrayEquals(expected.principalVariation(),actual.principalVariation());
            }
            assertEquals(Brn3Model.score(new Brn3Trainer(97).snapshot().newWorkspace().evaluatePawns(opening.board())),
                    Brn3Model.score(BrnLearningProbe.evaluator(model,0).applyAsDouble(opening.board())));
        }
    }
    @Test void partialPairsAndCapsCannotBecomeScoredDraws() {
        var win=new BrnLearningProbe.Game("WHITE_CHECKMATES_BLACK",null,1.,List.of(),"");
        var loss=new BrnLearningProbe.Game("WHITE_CHECKMATES_BLACK",null,0.,List.of(),"");
        var cap=new BrnLearningProbe.Game("PLY_CAP",null,null,List.of(),"");
        var pairs=List.of(new BrnLearningProbe.Pair(0,"a","",win,loss),new BrnLearningProbe.Pair(1,"b","",win,cap));
        var summary=BrnLearningProbe.summary(pairs);
        assertEquals(1,summary.get("completedPairs"));assertEquals(1,summary.get("incompletePairs"));
        assertEquals(.5,summary.get("pointScore"));assertEquals(0,summary.get("draws"));
        assertEquals(1,summary.get("wins"));assertEquals(1,summary.get("losses"));
        assertEquals(List.of(.5,.75),summary.get("allGameScoreBounds"));
    }
}
