package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class AnchoredRelationCandidateTest {
    @TempDir Path temporary;
    @Test void materialInputsSymmetryAndFiniteDifference() {
        var model = new AnchoredRelationCandidate(71); var board = Board.fromFen(RelationalCandidateTest.FEN);
        assertEquals(RelationalCandidate.material(board), model.predict(board), 0);
        assertEquals(21.5, model.predict(Board.fromFen("4k3/8/8/8/8/8/PNBRQ3/4K3 w - - 0 1")), 1e-12);
        var rng = new Random(19);
        for (int i = AnchoredRelationCandidate.HEAD; i < model.weights.length; i++) model.weights[i] = rng.nextDouble() - .5;
        double value = model.predict(board); model.resetGradient(); model.backward(1);
        int[] indices = Arrays.copyOf(model.touched, model.size); double[] expected = model.gradient.clone();
        for (int n = 0; n < indices.length; n += 37) {
            int i = indices[n]; double old = model.weights[i]; model.weights[i] = old + 1e-5; double plus = model.predict(board);
            model.weights[i] = old - 1e-5; double minus = model.predict(board); model.weights[i] = old;
            assertEquals(expected[i], (plus - minus) / 2e-5, 1e-8, "parameter " + i);
        }
        var forbidden = Board.fromFen(RelationalCandidateTest.FEN.replace("KQkq - 2 3", "- - 75 89")); forbidden[5] ^= 123456;
        assertEquals(value, model.predict(forbidden), 0);
        var reversed = Board.fromFen("rnbqkb1r/pppp1ppp/5n2/4p3/4P3/2N5/PPPP1PPP/R1BQKBNR b KQkq - 2 3");
        assertEquals(value, model.predict(reversed), 1e-12);
    }
    @Test void tinyDistinctPositionsFitAndCheckpointResumesExactly() throws Exception {
        var model = new AnchoredRelationCandidate(71);
        var examples = List.of(new BrnResearchData.Example(Board.fromFen(RelationalCandidateTest.FEN), 80, 78, 0),
                new BrnResearchData.Example(Board.fromFen("4k3/8/8/3p4/3P4/8/4N3/4K3 w - - 0 1"), 250, 5, 0),
                new BrnResearchData.Example(Board.fromFen("4k3/8/8/4q3/8/8/4R3/4K3 b - - 0 1"), 300, 14, 0));
        int[] order = {0,1,2};
        for (int pass = 0; pass < 1200; pass++) model.trainBatch(examples, order, 0, 3, .003, 1);
        for (var e : examples) assertEquals(e.cp()/100.0, model.predict(e.board()), 1e-3);
        var path = temporary.resolve("state"); try (var out = new BufferedOutputStream(Files.newOutputStream(path))) { model.write(out); }
        AnchoredRelationCandidate restored; try (var in = new BufferedInputStream(Files.newInputStream(path))) { restored = AnchoredRelationCandidate.read(in); }
        for (int n = 0; n < 5; n++) { model.trainBatch(examples, order, 0, 3, .001, .05); restored.trainBatch(examples, order, 0, 3, .001, .05); }
        assertArrayEquals(model.weights, restored.weights); assertArrayEquals(model.first, restored.first); assertArrayEquals(model.second, restored.second);
        assertEquals(model.updates, restored.updates);
    }
}
