package com.ohinteractive.seedv6.training.nnue;

import java.io.*;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.model.*;
import static com.ohinteractive.seedv6.training.nnue.TrainingFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class NnueMaterialTrainingTest {
    @Test void combinedForwardAndFiniteDifferencesIncludeMaterialInAllParameterGroups() {
        var trainer = NnueTrainer.materialParity(model());
        float epsilon = 1.0f / 1024;
        int[][] selected = {{0,0},{1,WHITE_PAWN*64},{1,BLACK_PAWN*64},{1,SHARED_KING*64},
                {2,0},{3,0},{3,64},{4,0},{5,0}};
        for (long[] board : new long[][]{WHITE, BLACK}) {
            double neural = new NnueTrainer(TrainableNnue.fromNetwork(trainer.model().snapshot())).predict(board);
            assertEquals(neural + (board == WHITE ? 100 : -100) / 32511.0, trainer.predict(board));
            trainer.accumulate(new long[][]{board}, new double[]{-.4}, 1);
            for (int[] p : selected) {
                float[] weights = trainer.model().parameters.groups[p[0]];
                float original = weights[p[1]], analytic = trainer.gradients.values.groups[p[0]][p[1]];
                weights[p[1]] = original + epsilon; double plus = loss(trainer, board, -.4);
                weights[p[1]] = original - epsilon; double minus = loss(trainer, board, -.4);
                weights[p[1]] = original;
                double numerical = (plus - minus) / (2 * epsilon);
                assertEquals(numerical, analytic, 2e-6 + 2e-4 * Math.abs(numerical));
            }
        }
        // Distinguish residual training from the old full-outcome objective on identical weights.
        var legacy = new NnueTrainer(model());
        legacy.accumulate(new long[][]{WHITE}, new double[]{.4}, 1);
        trainer.accumulate(new long[][]{WHITE}, new double[]{.4}, 1);
        assertNotEquals(legacy.gradients.values.groups[4][0], trainer.gradients.values.groups[4][0]);
    }

    @Test void saturationHasZeroGradientAndTrainingRetainsThePrior() {
        var trainer = NnueTrainer.materialParity(model());
        for (int sign : new int[]{-1,1}) {
            trainer.model().parameters.groups[4][0] = sign * 10;
            long[] board = sign == 1 ? WHITE : BLACK;
            trainer.accumulate(new long[][]{board}, new double[]{-sign}, 1);
            assertEquals(sign, trainer.predict(board));
            for (float[] group : trainer.gradients.values.groups) for (float gradient : group) assertEquals(0, gradient);
        }
        trainer = NnueTrainer.materialParity(model());
        var boards = new long[][]{WHITE, BLACK}; var targets = new double[]{.7,-.7};
        double before = meanLoss(trainer, boards, targets);
        for (int i=0;i<30;i++) trainer.trainBatch(boards, targets, 2);
        assertTrue(meanLoss(trainer, boards, targets) < before);
        var neural = new NnueTrainer(TrainableNnue.fromNetwork(trainer.model().snapshot()));
        assertEquals(neural.predict(WHITE)+100/32511.0, trainer.predict(WHITE));
        assertEquals(neural.predict(BLACK)-100/32511.0, trainer.predict(BLACK));
    }

    @Test void distinctCodecsRejectCrossLoadingAndResumeExactly() throws Exception {
        var trainer = NnueTrainer.materialParity(model());
        trainer.trainBatch(new long[][]{WHITE, BLACK}, new double[]{.7,-.7}, 2);
        var state = new NetworkTrainingState.NnueMaterial(trainer);
        byte[] saved = state.encode();
        assertThrows(IOException.class, () -> NetworkTrainingState.read(TrainingArchitecture.NNUE, new ByteArrayInputStream(saved)));
        assertThrows(IllegalArgumentException.class, () -> TrainingStateCodec.encode(trainer));
        assertThrows(IllegalArgumentException.class, () -> new NetworkTrainingState.Nnue(trainer));
        var legacy = TrainingStateCodec.encode(new NnueTrainer(model()));
        assertThrows(IOException.class, () -> NetworkTrainingState.read(TrainingArchitecture.NNUE_MATERIAL, new ByteArrayInputStream(legacy)));
        var restored = (NetworkTrainingState.NnueMaterial)NetworkTrainingState.read(TrainingArchitecture.NNUE_MATERIAL, new ByteArrayInputStream(saved));
        for (var t : new NnueTrainer[]{trainer, restored.trainer()}) t.trainBatch(new long[][]{BLACK}, new double[]{-.3}, 1);
        assertArrayEquals(state.encode(), restored.encode());
        var output = new ByteArrayOutputStream(); state.snapshot().write(output);
        assertThrows(IOException.class, () -> NetworkModel.read(TrainingArchitecture.NNUE, new ByteArrayInputStream(output.toByteArray())));
        assertInstanceOf(NetworkModel.NnueMaterial.class, NetworkModel.read(TrainingArchitecture.NNUE_MATERIAL, new ByteArrayInputStream(output.toByteArray())));
        assertThrows(IOException.class, () -> NetworkModel.read(TrainingArchitecture.NNUE_MATERIAL,
                new ByteArrayInputStream(NnueNetworkCodec.encode(trainer.model().snapshot()))));
        assertThrows(IllegalArgumentException.class, () -> state.snapshot().evaluation(new NnueScoreMapping(400)));
    }
}
