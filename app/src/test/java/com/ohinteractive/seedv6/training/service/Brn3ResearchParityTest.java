package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Brn3ResearchParityTest {
    @Test void productionTrainerMatchesTheIndependentlyTestedResearchRecipeAndResumesExactly() throws Exception {
        var research=new AbsoluteRelationCandidate(71,false,true,true,1,8,false,true);
        var production=new Brn3Trainer(71);
        var examples=List.of(new BrnResearchData.Example(Board.fromFen(RelationalCandidateTest.FEN),80,78,0),
                new BrnResearchData.Example(Board.fromFen("4k3/8/8/3p4/3P4/8/4N3/4K3 w - - 0 1"),250,5,0),
                new BrnResearchData.Example(Board.fromFen("4k3/8/8/4q3/8/8/4R3/4K3 b - - 0 1"),-300,14,0));
        int[] order={2,0,1};long[][] boards=new long[3][];double[] targets=new double[3];
        for(int i=0;i<3;i++){boards[i]=examples.get(order[i]).board();targets[i]=examples.get(order[i]).outcome();}
        compare(research,production);
        for(int step=0;step<6;step++) {
            research.trainBatch(examples,order,0,3,.003,true,0);production.trainBatch(boards,targets,3);
        }
        compare(research,production);
        var bytes=Brn3Codec.encodeTraining(production);assertEquals(Brn3Codec.TRAINING_BYTES,bytes.length);
        var restored=Brn3Codec.decodeTraining(bytes);assertEquals(production.config(),restored.config());
        research.trainBatch(examples,order,0,3,.003,true,0);restored.trainBatch(boards,targets,3);compare(research,restored);
        var oracle=research.foldedInference();var workspace=restored.snapshot().newWorkspace();var rng=new Random(1487);
        var board=Board.startingPosition();
        for(int ply=0;ply<2000;ply++) {
            double value=workspace.evaluatePawns(board);
            assertEquals(oracle.predict(board),value,1e-4);
            if(ply%37==0) {
                var forbidden=board.clone();forbidden[4]=(board[4]&Board.PLAYER_BIT)|(~board[4]&~Board.PLAYER_BIT);forbidden[5]=~board[5];
                assertEquals(value,workspace.evaluatePawns(forbidden),0,"Trained residual must ignore rule/history fields");
                var color=new long[6];color[4]=board[4]^Board.PLAYER_BIT;
                for(int square=0;square<64;square++) {
                    int code=Board.getSquare(board[0],board[1],board[2],board[3],square);if(code==0)continue;code^=8;
                    for(int word=0;word<4;word++)if((code&(1<<word))!=0)color[word]|=1L<<(square^56);
                }
                assertEquals(value,workspace.evaluatePawns(color),1e-10,"Color/rank/STM equivalence");
            }
            var moves=new long[512];int count=BrnResearchDiagnostics.legal(board,moves);
            board=count==0||ply%150==149?Board.startingPosition():BrnResearchDiagnostics.child(board,moves[rng.nextInt(count)]);
        }
        bytes[bytes.length/2]^=1;assertThrows(java.io.IOException.class,()->Brn3Codec.decodeTraining(bytes));
    }
    static void compare(AbsoluteRelationCandidate expected,Brn3Trainer actual) {
        assertEquals(expected.weights.length,Brn3Layout.TRAINING_PARAMETERS);assertEquals(expected.updates,actual.step());
        for(int i=0;i<expected.weights.length;i++) {
            if(Double.doubleToLongBits(expected.weights[i])!=Double.doubleToLongBits(actual.weight(i)))fail("Weight differs at "+i+": "+expected.weights[i]+" / "+actual.weight(i));
            if(Double.doubleToLongBits(expected.first[i])!=Double.doubleToLongBits(actual.firstMoment(i)))fail("First moment differs at "+i);
            if(Double.doubleToLongBits(expected.second[i])!=Double.doubleToLongBits(actual.secondMoment(i)))fail("Second moment differs at "+i);
        }
    }
    @Test void materialModelIsImmutableForbiddenFieldsDoNotMatterAndCodecRejectsCorruption()throws Exception {
        var trainer=new Brn3Trainer(97);var model=trainer.snapshot();var workspace=model.newWorkspace();
        var board=Board.fromFen("4k3/8/8/8/8/8/PNBRQ3/4K3 w - - 0 1");
        assertEquals(21.5,workspace.evaluatePawns(board),1e-12);
        var changed=board.clone();changed[4]^=0x7ffffffe;changed[5]^=54321;
        // Rule bits can encode arbitrary statuses; only the actual STM bit is restored.
        changed[4]=(changed[4]&~Board.PLAYER_BIT)|(board[4]&Board.PLAYER_BIT);
        assertEquals(workspace.evaluatePawns(board),workspace.evaluatePawns(changed),0);
        var weights=new float[Brn3Model.PARAMETER_COUNT];var immutable=new Brn3Model(weights);weights[Brn3Layout.OUTPUT_BIAS]=100;
        assertEquals(0,immutable.weight(Brn3Layout.OUTPUT_BIAS));
        byte[] bytes=Brn3Codec.encodeModel(model);assertEquals(Brn3Codec.MODEL_BYTES,bytes.length);
        assertEquals(21.5,Brn3Codec.decodeModel(bytes).newWorkspace().evaluatePawns(board),1e-12);
        assertThrows(java.io.IOException.class,()->Brn3Codec.decodeModel(Arrays.copyOf(bytes,bytes.length-1)));
        byte[] trailing=Arrays.copyOf(bytes,bytes.length+1);assertThrows(java.io.IOException.class,()->Brn3Codec.decodeModel(trailing));
        bytes[bytes.length/2]^=1;assertThrows(java.io.IOException.class,()->Brn3Codec.decodeModel(bytes));
        assertEquals(1,Brn3Model.score(.005));assertEquals(-1,Brn3Model.score(-.005));assertEquals(0,Brn3Model.score(.0049));
        assertEquals(com.ohinteractive.seedv6.search.tt.TranspositionScores.MAX_NORMAL_SCORE,Brn3Model.score(1000));
        assertThrows(IllegalArgumentException.class,()->Brn3Model.score(Double.NaN));
    }
}
