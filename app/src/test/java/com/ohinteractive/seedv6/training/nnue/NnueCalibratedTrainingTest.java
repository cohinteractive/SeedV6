package com.ohinteractive.seedv6.training.nnue;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.Brn3Objective;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.selfplay.TrajectorySampler;
import com.ohinteractive.seedv6.training.validation.HeldOutLoss;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.training.nnue.TrainingFixtures.*;

class NnueCalibratedTrainingTest {
    static NnueTrainer fixture() {
        var model=model();model.parameters.groups[4][0]=0;model.parameters.groups[5][0]=1f/512;
        return NnueTrainer.calibratedMaterialParity(model);
    }
    @Test void finiteDifferencesCoverEveryLayerAndBothPerspectives() {
        var t=fixture();int[][] chosen={{0,0},{1,WHITE_PAWN*64},{1,BLACK_PAWN*64},{1,SHARED_KING*64},{2,0},{3,0},{3,64},{4,0},{5,0}};
        for(var board:new long[][]{WHITE,BLACK}) {
            t.accumulate(new long[][]{board},new double[]{-.4},1);
            for(var p:chosen) {
                float[] w=t.model().parameters.groups[p[0]];float old=w[p[1]],epsilon=1f/65536;
                double analytic=t.gradients.values.groups[p[0]][p[1]];
                w[p[1]]=old+epsilon;double plus=t.loss(board,-.4);
                w[p[1]]=old-epsilon;double minus=t.loss(board,-.4);w[p[1]]=old;
                assertEquals((plus-minus)/(2*epsilon),analytic,2e-4+Math.abs(analytic)*.004,"group "+p[0]);
            }
        }
    }
    @Test void materialIsInPawnUnitsAndZeroResidualAlreadyExplainsMaterialOutcome() {
        var t=fixture();Arrays.fill(t.model().parameters.groups[5],0);
        assertEquals(Brn3Objective.smoothOutcome(1,NnueCorpusTargets.material(WHITE))[0],t.predict(WHITE),1e-15);
        assertEquals(-t.predict(WHITE),t.predict(BLACK),1e-15);
        double y=t.predict(WHITE);t.accumulate(new long[][]{WHITE},new double[]{y},1);
        assertEquals(0,t.gradients.values.groups[4][0],1e-5);
        var legacy=NnueTrainer.materialParity(TrainableNnue.fromNetwork(t.model().snapshot()));
        legacy.accumulate(new long[][]{WHITE},new double[]{y},1);
        assertTrue(Math.abs(legacy.gradients.values.groups[4][0])>.1,"old objective tries to relearn fixed material at the wrong scale");
    }
    @Test void oneStepMovesCorrectlyBatchClearsAndTinyTrainingDecreasesLoss() {
        var t=fixture();t.optimizer().setLearningRate(1e-5);
        double prediction=t.predict(WHITE),loss=t.loss(WHITE,.9);
        t.trainBatch(new long[][]{WHITE},new double[]{.9},1);
        assertTrue(t.predict(WHITE)>prediction);assertTrue(t.loss(WHITE,.9)<loss);
        for(int i=0;i<50;i++)t.trainBatch(new long[][]{WHITE},new double[]{.9},1);
        assertTrue(t.loss(WHITE,.9)<loss);
        var fresh=NnueTrainer.calibratedMaterialParity(TrainableNnue.fromNetwork(t.model().snapshot()));
        t.accumulate(new long[][]{BLACK,OTHER},new double[]{-.4,.2},2);
        fresh.accumulate(new long[][]{BLACK,OTHER},new double[]{-.4,.2},2);
        assertBits(fresh.gradients.values,t.gradients.values);
    }
    @Test void versionedModelAndOptimizerResumeExactlyWithoutReinterpretingV1() throws Exception {
        var t=fixture();t.trainBatch(new long[][]{WHITE,BLACK},new double[]{.7,-.7},2);
        var state=new NetworkTrainingState.NnueMaterial(t);byte[] saved=state.encode();
        var restored=(NetworkTrainingState.NnueMaterial)NetworkTrainingState.read(TrainingArchitecture.NNUE_MATERIAL,new ByteArrayInputStream(saved));
        assertTrue(restored.trainer().calibratedOutcome());assertArrayEquals(saved,restored.encode());
        assertThrows(IOException.class,()->TrainingStateCodec.read(new ByteArrayInputStream(saved)));
        var bytes=new ByteArrayOutputStream();state.snapshot().write(bytes);
        var model=(NetworkModel.NnueMaterial)NetworkModel.read(TrainingArchitecture.NNUE_MATERIAL,new ByteArrayInputStream(bytes.toByteArray()));
        assertTrue(model.calibratedOutcome());
        assertThrows(IOException.class,()->NnueNetworkCodec.readMaterial(new ByteArrayInputStream(bytes.toByteArray())));
        var samples=List.of(new TrajectorySampler.Sample(WHITE,1),new TrajectorySampler.Sample(BLACK,-1));
        double expected=(Math.pow(t.predict(WHITE)-1,2)+Math.pow(t.predict(BLACK)+1,2))/4;
        assertEquals(expected,HeldOutLoss.compare(model,model,samples).candidateLoss(),1e-15);
        for(var trainer:new NnueTrainer[]{t,restored.trainer()})trainer.trainBatch(new long[][]{OTHER},new double[]{.2},1);
        assertArrayEquals(state.encode(),restored.encode());
        var old=NnueTrainer.materialParity(TrainableNnue.initialized(1));byte[] oldBytes=TrainingStateCodec.encodeMaterial(old);
        var oldRestored=TrainingStateCodec.readMaterial(new ByteArrayInputStream(oldBytes));
        assertFalse(oldRestored.calibratedOutcome());assertArrayEquals(oldBytes,TrainingStateCodec.encodeMaterial(oldRestored));
        saved[saved.length-5]^=1;assertThrows(IOException.class,()->TrainingStateCodec.readMaterial(new ByteArrayInputStream(saved)));
    }
    @Test void newProductionInitializerUsesCalibratedObjectiveAndIdenticalGenZeroScores() {
        var fresh=(NetworkTrainingState.NnueMaterial)NetworkTrainingState.initialized(TrainingArchitecture.NNUE_MATERIAL,71,.001);
        assertTrue(fresh.trainer().calibratedOutcome());
        var old=new NetworkModel.NnueMaterial(NnueNetwork.initialized(71));
        var a=fresh.snapshot().evaluation(com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping.V1).newState(1);
        var b=old.evaluation(com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping.V1).newState(1);
        for(var board:new long[][]{WHITE,BLACK,OTHER,Board.startingPosition()}){a.initialize(board,0);b.initialize(board,0);assertEquals(b.evaluate(board,0),a.evaluate(board,0));}
    }
}
