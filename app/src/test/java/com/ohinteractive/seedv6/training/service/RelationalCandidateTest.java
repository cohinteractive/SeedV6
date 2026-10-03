package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import java.util.*;
import java.io.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RelationalCandidateTest {
    static final String FEN = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3";
    @Test void permittedInformationAndExactMaterial() {
        var model = new RelationalCandidate(true);
        var board = Board.fromFen("4k3/8/8/8/8/8/PNBRQ3/4K3 w - - 0 1");
        assertEquals(21.5, model.predict(board), 1e-12);
        for (int i = 0; i < 100; i++) model.train(Board.fromFen(FEN), .8, .01);
        var a = Board.fromFen(FEN); var b = Board.fromFen(FEN.replace("KQkq - 2 3", "- - 75 89")); b[5] ^= 1234567;
        assertEquals(model.predict(a), model.predict(b), 0);
        b = a.clone(); b[4] ^= Board.PLAYER_BIT;
        assertEquals(-model.predict(a), model.predict(b), 1e-10);
        var reversed = Board.fromFen("rnbqkb1r/pppp1ppp/5n2/4p3/4P3/2N5/PPPP1PPP/R1BQKBNR b KQkq - 2 3");
        assertEquals(model.predict(a), model.predict(reversed), 1e-10);
        assertEquals(BrnResearchData.split(a), BrnResearchData.split(b));
        assertEquals(BrnResearchData.split(a), BrnResearchData.split(reversed));
    }
    @Test void finiteDifferencesAndTinyOverfit() {
        var model = new RelationalCandidate(true); var a = Board.fromFen(FEN);
        model.encode(a); int[] indices = Arrays.copyOf(model.touched, model.size);
        for (int i = 0; i < indices.length; i += 11) {
            int index = indices[i]; model.encode(a); double analytic = model.features[index];
            model.weights[index] = 1e-5; double plus = model.predict(a); model.weights[index] = -1e-5; double minus = model.predict(a); model.weights[index] = 0;
            assertEquals(analytic, (plus - minus) / 2e-5, 1e-8);
        }
        long[][] boards = {a, Board.fromFen("4k3/8/8/3p4/3P4/8/4N3/4K3 w - - 0 1"),
                Board.fromFen("4k3/8/8/4q3/8/8/4R3/4K3 b - - 0 1")};
        double[] targets = {.8, 2.5, 3.0};
        for (int pass = 0; pass < 1000; pass++) for (int i = 0; i < boards.length; i++) model.train(boards[i], targets[i], .03);
        for (int i = 0; i < boards.length; i++) assertEquals(targets[i], model.predict(boards[i]), 1e-5);
    }
    @Test void serializationPreservesInferenceAndContinuation() throws Exception {
        var a = new RelationalCandidate(true); var board = Board.fromFen(FEN);
        for (int i = 0; i < 50; i++) a.train(board, .45, .02);
        var bytes = new ByteArrayOutputStream(); a.write(bytes);
        var b = RelationalCandidate.read(new ByteArrayInputStream(bytes.toByteArray()));
        assertEquals(a.predict(board), b.predict(board), 0);
        for (int i = 0; i < 10; i++) { a.train(board, -.2, .02); b.train(board, -.2, .02); }
        assertArrayEquals(a.weights, b.weights); assertArrayEquals(a.squares, b.squares); assertEquals(a.updates, b.updates);
        byte[] corrupt = bytes.toByteArray(); corrupt[0] = 0;
        assertThrows(IOException.class, () -> RelationalCandidate.read(new ByteArrayInputStream(corrupt)));
    }
    @Test void outcomeDerivativeMatchesFiniteDifferenceAcrossValueAndMaterial() {
        for (int material : new int[]{4, 17, 58, 78, 90}) for (double value : new double[]{-5, -1.1, -.3, 0, .3, 1.1, 5}) {
            double finite = (RelationalCandidate.outcome(value + 1e-5, material)[0] - RelationalCandidate.outcome(value - 1e-5, material)[0]) / 2e-5;
            assertEquals(finite, RelationalCandidate.outcome(value, material)[1], 1e-8);
        }
    }
    @Test void crossEntropyDerivativeIsCorrectAndDoesNotVanishWhenConfidentlyWrong() {
        for (int material : new int[]{4, 58, 78}) for (double value : new double[]{-1000, -10, -2, -.3, 0, .3, 2, 10, 1000}) for (double target : new double[]{-1, -.3, 0, .3, 1}) {
            double finite = (RelationalCandidate.crossEntropy(value + 1e-4, material, target)[0] - RelationalCandidate.crossEntropy(value - 1e-4, material, target)[0]) / 2e-4;
            double analytic = RelationalCandidate.crossEntropy(value, material, target)[1];
            assertTrue(Double.isFinite(analytic)); assertEquals(finite, analytic, 1e-7);
        }
        assertTrue(RelationalCandidate.crossEntropy(10, 78, -1)[1] > 1);
        assertTrue(RelationalCandidate.crossEntropy(-10, 78, 1)[1] < -1);
    }
}
