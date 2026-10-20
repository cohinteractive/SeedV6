package com.ohinteractive.seedv6.core.brnnext;

import java.io.*;
import java.util.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brnpair2.BrnPair2Features;
import com.ohinteractive.seedv6.core.brn3.Brn3Objective;
import com.ohinteractive.seedv6.training.nnue.NnueCorpusTargets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.core.brnnext.BrnNextArchitecture.*;

class BrnNextMathTest {
    static final long[][] BOARDS = {
        Board.fromFen("4k3/8/3p4/2n5/4B3/3P4/8/4K3 w - - 0 1"),
        Board.startingPosition(),
        Board.fromFen("4k3/8/8/8/3q4/2R5/8/4K3 b - - 0 1")
    };

    @ParameterizedTest @EnumSource(BrnNextArchitecture.class)
    void independentExplicitForwardAndEveryParameterGradient(BrnNextArchitecture kind) {
        var trainer = new BrnNextTrainer(kind, 71, .003);
        var random = new SplittableRandom(97);
        for (int i = 0; i < trainer.weights.length; i++) trainer.weights[i] = random.nextDouble(-.2, .2);
        for (long[] board : BOARDS) assertEquals(reference(kind, trainer.weights, board), trainer.predictPawns(board), 1e-11);
        trainer.predictPawns(BOARDS[0]); trainer.backward(1);
        double[] analytic = trainer.gradient.clone();
        // All addresses, including inactive features and every expert table, not just head weights.
        for (int i = 0; i < trainer.weights.length; i++) {
            double original = trainer.weights[i], epsilon = 1e-5;
            trainer.weights[i] = original + epsilon; double plus = trainer.predictPawns(BOARDS[0]);
            trainer.weights[i] = original - epsilon; double minus = trainer.predictPawns(BOARDS[0]);
            trainer.weights[i] = original;
            assertEquals((plus - minus) / (2 * epsilon), analytic[i], 2e-9, "parameter " + i);
        }
    }

    @ParameterizedTest @EnumSource(BrnNextArchitecture.class)
    void materialInitializationBoundedFitAndExactStateContinuation(BrnNextArchitecture kind) throws Exception {
        var trainer = new BrnNextTrainer(kind, 71, .003);
        for (var board : BOARDS) assertEquals(initial(kind, board), trainer.predictPawns(board));
        double[] targets = {.6, -.4, .2};
        double before = loss(trainer, targets);
        for (int update = 0; update < 512; update++) trainer.trainBatch(BOARDS, targets, targets.length);
        double after = loss(trainer, targets);
        System.out.println(kind + " tiny-fit squared error " + before + " -> " + after + " (512 updates, 3 positions)");
        assertTrue(after < before * .35, "tiny fixture must learn: " + before + " -> " + after);
        assertTrue(Arrays.stream(trainer.first, EMBEDDING, kind.head()).anyMatch(v -> v != 0), "embedding receives learning signal");
        var bytes = new ByteArrayOutputStream(); trainer.write(bytes);
        assertEquals(BrnNextCodec.trainingBytes(kind), bytes.size());
        var restored = BrnNextTrainer.read(kind, new ByteArrayInputStream(bytes.toByteArray()));
        var immutable = trainer.snapshot();
        trainer.trainBatch(BOARDS, targets, targets.length); restored.trainBatch(BOARDS, targets, targets.length);
        var left = new ByteArrayOutputStream(); var right = new ByteArrayOutputStream();
        trainer.write(left); restored.write(right); assertArrayEquals(left.toByteArray(), right.toByteArray());
        assertEquals(513, restored.step());
        var runtime = new ByteArrayOutputStream(); BrnNextCodec.writeModel(immutable, runtime);
        assertEquals(BrnNextCodec.modelBytes(kind), runtime.size());
        var loaded = BrnNextCodec.readModel(kind, new ByteArrayInputStream(runtime.toByteArray()));
        for (var board : BOARDS) assertEquals(immutable.rawPawns(board, immutable.newWorkspace()), loaded.rawPawns(board, loaded.newWorkspace()));
        assertThrows(IOException.class, () -> BrnNextTrainer.read(kind, new ByteArrayInputStream(runtime.toByteArray())));
        assertThrows(IOException.class, () -> BrnNextCodec.readModel(kind, new ByteArrayInputStream(bytes.toByteArray())));
        byte[] corrupt = bytes.toByteArray(); corrupt[corrupt.length / 2] ^= 1;
        assertThrows(IOException.class, () -> BrnNextTrainer.read(kind, new ByteArrayInputStream(corrupt)));
        for (var other : BrnNextArchitecture.values()) if (other != kind)
            assertThrows(IOException.class, () -> BrnNextCodec.readModel(other, new ByteArrayInputStream(runtime.toByteArray())));
    }

    @ParameterizedTest @EnumSource(BrnNextArchitecture.class)
    void informationBoundaryColorRankSymmetryAndNumericalDomain(BrnNextArchitecture kind) {
        var trainer = new BrnNextTrainer(kind, 7, .003);
        trainer.trainBatch(BOARDS, new double[]{.6, -.4, .2}, 3);
        var model = trainer.snapshot(); var workspace = model.newWorkspace();
        var a = Board.fromFen("4k3/8/3p4/2n5/4B3/3P4/8/4K3 w - - 0 1");
        var flipped = Board.fromFen("4k3/8/3p4/4b3/2N5/3P4/8/4K3 b - - 0 1");
        assertEquals(model.rawPawns(a, workspace), model.rawPawns(flipped, workspace), 1e-14);
        var status = Board.fromFen("4k3/8/3p4/2n5/4B3/3P4/8/4K3 w - - 87 42");
        assertEquals(model.rawPawns(a, workspace), model.rawPawns(status, workspace));
        var emptyOwnership = a.clone(); emptyOwnership[3] ^= ~(a[0] | a[1] | a[2]);
        assertEquals(model.rawPawns(a, workspace), model.rawPawns(emptyOwnership, workspace));
        int expected = model.score(a, workspace);
        for (int i = 0; i < 50; i++) { model.score(BOARDS[i % BOARDS.length], workspace); assertEquals(expected, model.score(a, workspace)); }
        var huge = new double[kind.parameters()]; huge[0] = 1e6;
        var positive = new BrnNextModel(kind, huge); assertEquals(32511, positive.score(a, positive.newWorkspace()));
        huge[0] = -1e6;
        var negative = new BrnNextModel(kind, huge); assertEquals(-32511, negative.score(a, negative.newWorkspace()));
        huge[0] = Double.NaN; assertThrows(IllegalArgumentException.class, () -> new BrnNextModel(kind, huge));
    }

    private static double initial(BrnNextArchitecture kind, long[] board) {
        return kind.frozenBase() ? BrnNextBase.model().newWorkspace().evaluatePawns(board) : BrnPair2Features.material(board);
    }

    private static double loss(BrnNextTrainer trainer, double[] targets) {
        double sum = 0;
        for (int i = 0; i < BOARDS.length; i++) {
            double error = Brn3Objective.smoothOutcome(trainer.predictPawns(BOARDS[i]), NnueCorpusTargets.material(BOARDS[i]))[0] - targets[i];
            sum += error * error;
        }
        return sum;
    }

    /** Deliberately direct O(n^3) combinations, with no workspace recurrence or cached addresses. */
    private static double reference(BrnNextArchitecture kind, double[] w, long[] board) {
        var active = new ArrayList<Integer>(); int[] categories = new int[64];
        int stm = Board.player((int) board[4]);
        for (int q = 0; q < 64; q++) {
            int code = Board.getSquare(board[0], board[1], board[2], board[3], q ^ (56 * stm));
            if ((code & 7) != 0) {
                int channel = (code & 7) - 1 + (((code >>> 3) & 1) == stm ? 0 : 6);
                categories[q] = channel + 1; active.add(channel * 64 + q);
            }
        }
        double sum = initial(kind, board) + w[0];
        for (int feature : active) sum += w[UNARY + feature] / Math.sqrt(32);
        int width = kind.width(), h = kind.head(); double[] gate = new double[width], expert = new double[width];
        for (int r = 0; r < width; r++) {
            if (kind.correctionKind() == CUBIC) {
                for (int i = 0; i < active.size(); i++) for (int j = i + 1; j < active.size(); j++) {
                    double pair = w[EMBEDDING + active.get(i) * width + r] * w[EMBEDDING + active.get(j) * width + r];
                    sum += w[h + r] * pair / Math.sqrt(496);
                    for (int k = j + 1; k < active.size(); k++)
                        sum += w[h + width + r] * pair * w[EMBEDDING + active.get(k) * width + r] / Math.sqrt(4960);
                }
            } else {
                double projection = w[h + r];
                for (int feature : active) projection += w[EMBEDDING + feature * width + r] / Math.sqrt(32);
                if (kind.correctionKind() == POOLED) sum += w[h + width + r] * Math.tanh(projection);
                else {
                    gate[r] = Math.exp(projection);
                    int[][] directions = {{1, 0}, {0, 1}, {1, 1}, {-1, 1}};
                    for (int s = 0; s < directions.length; s++) for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) {
                        int nx = x + directions[s][0], ny = y + directions[s][1];
                        if (nx < 0 || nx >= 8 || ny >= 8) continue;
                        expert[r] += w[h + width + r * 676 + s * 169 + categories[y * 8 + x] * 13 + categories[ny * 8 + nx]] / Math.sqrt(210);
                    }
                }
            }
        }
        if (kind.correctionKind() == GATED_PAIRS) {
            double total = Arrays.stream(gate).sum();
            for (int r = 0; r < width; r++) sum += gate[r] * expert[r] / total;
        }
        return sum;
    }
}
