package com.ohinteractive.seedv6.training.nnue;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.NnueFeatureSchema;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

final class TrainingFixtures {
    static final long[] WHITE = Board.fromFen("4k3/8/8/8/8/8/P7/4K3 w - - 0 1");
    static final long[] BLACK = Board.fromFen("4k3/8/8/8/8/8/P7/4K3 b - - 0 1");
    static final long[] OTHER = Board.fromFen("7k/8/8/8/8/8/1P6/K7 w - - 0 1");
    static final int WHITE_PAWN = NnueFeatureSchema.featureIndex(Value.WHITE, 4, Value.WHITE, Piece.PAWN, 8);
    static final int BLACK_PAWN = NnueFeatureSchema.featureIndex(Value.BLACK, 60, Value.WHITE, Piece.PAWN, 8);
    static final int SHARED_KING = NnueFeatureSchema.featureIndex(Value.WHITE, 4, Value.WHITE, Piece.KING, 4);

    private TrainingFixtures() {}

    /** Dyadic, nonsaturated fixture: all raw accumulators and hidden units lie inside (0,1). */
    static TrainableNnue model() {
        Parameters p = new Parameters();
        Arrays.fill(p.groups[0], 0.25f);
        Arrays.fill(p.groups[2], 0.25f);
        p.groups[1][WHITE_PAWN * 64] = 0.125f;
        p.groups[1][BLACK_PAWN * 64] = 0.0625f;
        p.groups[3][0] = 0.25f;
        p.groups[3][64] = -0.125f;
        p.groups[4][0] = 0.0625f;
        p.groups[5][0] = 0.5f;
        return new TrainableNnue(p);
    }

    static double loss(NnueTrainer trainer, long[] board, double target) {
        double error = trainer.predict(board) - target;
        return 0.5 * error * error;
    }

    static double meanLoss(NnueTrainer trainer, long[][] boards, double[] targets) {
        double sum = 0;
        for (int i = 0; i < boards.length; i++) sum += loss(trainer, boards[i], targets[i]);
        return sum / boards.length;
    }

    static void assertBits(Parameters expected, Parameters actual) {
        for (int group = 0; group < expected.groups.length; group++) {
            assertArrayEquals(expected.groups[group], actual.groups[group], "Parameter group " + group);
        }
    }
}
