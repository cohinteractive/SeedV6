package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BrnSuccessorCandidateTest {
    static List<BrnResearchData.Example> examples() {
        return List.of(new BrnResearchData.Example(Board.fromFen(RelationalCandidateTest.FEN),80,78,0),
                new BrnResearchData.Example(Board.fromFen("4k3/8/8/3p4/3P4/8/4N3/4K3 w - - 0 1"),250,5,0),
                new BrnResearchData.Example(Board.fromFen("4k3/8/8/4q3/8/8/4R3/4K3 b - - 0 1"),-300,14,0));
    }
    @Test void absoluteControlMatchesIndependentOriginalIncludingAllAdamState() {
        var old=new AbsoluteRelationCandidate(71,false,true,true,1,8,false,true);
        var candidate=new BrnSuccessorCandidate(BrnSuccessorCandidate.Relations.ABSOLUTE,8,BrnSuccessorCandidate.Pool.SUM,71);
        assertArrayEquals(old.weights,candidate.weights);int[] order={2,0,1};var data=examples();
        for(int step=0;step<7;step++) {
            for(var e:data)assertEquals(old.predict(e.board()),candidate.predict(e.board()),0);
            old.trainBatch(data,order,0,3,.003,true,0);candidate.trainBatch(data,order,0,3,.003);
            assertArrayEquals(old.weights,candidate.weights);assertArrayEquals(old.first,candidate.first);assertArrayEquals(old.second,candidate.second);
        }
    }
    @Test void eachRelationAndPoolingFamilyHasCorrectGradientsAndExactResume()throws Exception {
        var data=examples();int[] order={2,0,1};
        for(var relation:BrnSuccessorCandidate.Relations.values())for(var pool:BrnSuccessorCandidate.Pool.values()) {
            var model=new BrnSuccessorCandidate(relation,8,pool,71);
            assertEquals(RelationalCandidate.material(data.get(0).board()),model.predict(data.get(0).board()),0);
            // Nonzero head makes edge, unary, pooling and hidden gradients observable.
            Arrays.fill(model.weights,model.head,model.output+1,.17);
            model.predict(data.get(0).board());model.resetGradient();model.backward(1);
            var indices=Arrays.copyOf(model.touched,model.size);var gradients=model.gradient.clone();
            for(int n=0;n<indices.length;n+=Math.max(1,indices.length/100)) {
                int i=indices[n];double value=model.weights[i];model.weights[i]=value+1e-6;double plus=model.predict(data.get(0).board());
                model.weights[i]=value-1e-6;double minus=model.predict(data.get(0).board());model.weights[i]=value;
                assertEquals(gradients[i],(plus-minus)/2e-6,2e-7,relation+" "+pool+" parameter "+i);
            }
            for(int step=0;step<3;step++)model.trainBatch(data,order,0,3,.003);
            byte[] bytes=model.encode();var restored=BrnSuccessorCandidate.decode(bytes);
            model.trainBatch(data,order,0,3,.003);restored.trainBatch(data,order,0,3,.003);
            assertArrayEquals(model.weights,restored.weights);assertArrayEquals(model.first,restored.first);assertArrayEquals(model.second,restored.second);
            bytes[bytes.length/2]^=1;assertThrows(java.io.IOException.class,()->BrnSuccessorCandidate.decode(bytes));
        }
    }
    @Test void sharingCandidatesLearnDistinctTinyLabelsAndRespectColorAndInformationBoundary() {
        var data=examples();int[] order={0,1,2};
        for(var relation:BrnSuccessorCandidate.Relations.values()) {
            var model=new BrnSuccessorCandidate(relation,8,BrnSuccessorCandidate.Pool.SUM,97);
            for(int step=0;step<1000;step++)model.trainBatch(data,order,0,3,.003);
            for(var e:data)assertEquals(e.outcome(),RelationalCandidate.outcome(model.predict(e.board()),e.sfMaterial())[0],.02);
            var board=data.get(0).board();double value=model.predict(board);
            var changed=board.clone();changed[4]=(board[4]&Board.PLAYER_BIT)|(~board[4]&~Board.PLAYER_BIT);changed[5]=~board[5];
            assertEquals(value,model.predict(changed),0);
            var reversed=Board.fromFen("rnbqkb1r/pppp1ppp/5n2/4p3/4P3/2N5/PPPP1PPP/R1BQKBNR b KQkq - 2 3");
            assertEquals(value,model.predict(reversed),1e-10);
        }
    }
    @Test void explicitRuleStateVariantUsesRightsAndEpButNotClocksOrHash() {
        var model=new BrnSuccessorCandidate(BrnSuccessorCandidate.Relations.RELATIVE,8,BrnSuccessorCandidate.Pool.SUM_STATE,71);
        Arrays.fill(model.weights,0);model.weights[model.head]=1;model.weights[model.dense+96]=1;
        var rights=Board.fromFen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        var none=Board.fromFen("r3k2r/8/8/8/8/8/8/R3K2R w - - 0 1");
        assertEquals(1,model.predict(rights)-model.predict(none),0);
        var clock=Board.fromFen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 87 111");clock[5]=0;
        assertEquals(model.predict(rights),model.predict(clock),0);
        model.weights[model.dense+96]=0;model.weights[model.dense+96+4+3]=1;
        var ep=Board.fromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1");
        var noEp=Board.fromFen("4k3/8/8/3pP3/8/8/8/4K3 w - - 0 1");
        assertEquals(1,model.predict(ep)-model.predict(noEp),0);
    }
    @Test void admittedFoldedInferenceMatchesTrainingOnTransitionsAndOwnsItsSnapshot() {
        var data=examples();int[] order={0,1,2};
        for(var relation:List.of(BrnSuccessorCandidate.Relations.ABSOLUTE,BrnSuccessorCandidate.Relations.RELATIVE))
            for(var pool:List.of(BrnSuccessorCandidate.Pool.SUM,BrnSuccessorCandidate.Pool.SUM_SELF,BrnSuccessorCandidate.Pool.SUM_CONTEXT)) {
                var candidate=new BrnSuccessorCandidate(relation,8,pool,71);
                for(int step=0;step<8;step++)candidate.trainBatch(data,order,0,3,.003);
                var work=new BrnSuccessorInference(candidate);var random=new Random(987);var board=Board.startingPosition();
                var production=pool==BrnSuccessorCandidate.Pool.SUM?work.compatibleModel().newWorkspace():null;
                for(int i=0;i<300;i++) {
                    assertEquals(candidate.predict(board),work.evaluatePawns(board),1e-4,relation+" "+pool);
                    if(production!=null)assertEquals(production.evaluatePawns(board),work.evaluatePawns(board),0);
                    var moves=new long[512];int n=BrnResearchDiagnostics.legal(board,moves);
                    board=n==0||i%99==98?Board.startingPosition():BrnResearchDiagnostics.child(board,moves[random.nextInt(n)]);
                }
                double value=work.evaluatePawns(board);Arrays.fill(candidate.weights,123);
                assertEquals(value,work.evaluatePawns(board),0);
            }
    }
}
