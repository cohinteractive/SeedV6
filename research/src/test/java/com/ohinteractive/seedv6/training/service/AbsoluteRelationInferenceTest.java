package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AbsoluteRelationInferenceTest {
    @Test void cachedInferenceAgreesOnWalksSiblingsSpecialMovesAndRuleChanges() {
        var trainable=new AbsoluteRelationCandidate(71,false,true,true,1);
        var rng=new Random(891);
        for(int i=trainable.HEAD;i<=trainable.OUTPUT_BIAS;i++)trainable.weights[i]=rng.nextDouble()-.5;
        for(int i=trainable.PARAMETERS;i<trainable.weights.length;i++)trainable.weights[i]=(rng.nextDouble()-.5)*.2;
        var model=trainable.foldedInference();var cache=new AbsoluteRelationInference(model);
        for(String fen:new String[]{Board.FEN_STARTING_POSITION,RelationalCandidateTest.FEN,
                "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1","4k3/P7/8/8/8/8/7p/4K3 w - - 0 1"}){
            var board=Board.fromFen(fen);check(model,cache,board);var moves=new long[512];int count=BrnResearchDiagnostics.legal(board,moves);
            for(int i=0;i<count;i++){check(model,cache,BrnResearchDiagnostics.child(board,moves[i]));check(model,cache,board);}
            var changed=board.clone();changed[4]^=Board.PLAYER_BIT;changed[5]^=12345;check(model,cache,changed);check(model,cache,board);
        }
        var board=Board.fromFen(Board.FEN_STARTING_POSITION);
        for(int ply=0;ply<2000;ply++){
            check(model,cache,board);var moves=new long[512];int count=BrnResearchDiagnostics.legal(board,moves);
            if(count==0 || ply%150==149)board=Board.fromFen(Board.FEN_STARTING_POSITION);
            else board=BrnResearchDiagnostics.child(board,moves[rng.nextInt(count)]);
        }
        assertTrue(cache.updates>100);assertTrue(cache.rebuilds>10);assertTrue(cache.repeats>0);
    }
    static void check(AbsoluteRelationCandidate model,AbsoluteRelationInference cache,long[] board){
        double expected=model.predict(board),actual=cache.predict(board);assertEquals(expected,actual,1e-9);
        assertEquals(BrnResearchDiagnostics.score(expected),BrnResearchDiagnostics.score(actual));
    }
    @Test void compactWeightsStayWithinDeclaredQuantizationBudget() {
        var model=new AbsoluteRelationCandidate(71,false,true);var rng=new Random(87);
        for(int i=model.HEAD;i<=model.OUTPUT_BIAS;i++)model.weights[i]=rng.nextDouble()-.5;
        var cache=new AbsoluteRelationInference(model,true);var board=Board.fromFen(Board.FEN_STARTING_POSITION);
        for(int n=0;n<1000;n++){
            assertEquals(model.predict(board),cache.predict(board),1e-4);
            var moves=new long[512];int count=BrnResearchDiagnostics.legal(board,moves);
            board=count==0||n%150==149?Board.fromFen(Board.FEN_STARTING_POSITION):BrnResearchDiagnostics.child(board,moves[rng.nextInt(count)]);
        }
    }
}
