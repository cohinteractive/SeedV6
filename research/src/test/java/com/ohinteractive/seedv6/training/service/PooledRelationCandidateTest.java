package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PooledRelationCandidateTest {
    @Test void materialPerspectiveForbiddenFieldsAndNonlinearGradient() {
        var a = Board.fromFen(RelationalCandidateTest.FEN); var model = new PooledRelationCandidate(71);
        assertEquals(RelationalCandidate.material(a), model.predict(a), 0);
        var rng = new Random(11); for (int i = PooledRelationCandidate.HEAD; i < model.weights.length; i++) model.weights[i] = rng.nextDouble() - .5;
        double prediction = model.predict(a); model.backward(a, 1);
        int[] indices = Arrays.copyOf(model.touched, model.touchedCount); double[] expected = model.gradient.clone();
        for (int j = 0; j < indices.length; j += 23) {
            int index = indices[j]; double old = model.weights[index]; model.weights[index] = old + 1e-5; double plus = model.predict(a);
            model.weights[index] = old - 1e-5; double minus = model.predict(a); model.weights[index] = old;
            assertEquals(expected[index], (plus - minus) / 2e-5, 1e-8, "parameter " + index);
        }
        var b = a.clone(); b[4] ^= Board.PLAYER_BIT; assertEquals(-prediction, model.predict(b), 1e-12);
        b = Board.fromFen(RelationalCandidateTest.FEN.replace("KQkq - 2 3", "- - 75 89")); b[5] ^= 777;
        assertEquals(prediction, model.predict(b), 0);
        b = Board.fromFen("rnbqkb1r/pppp1ppp/5n2/4p3/4P3/2N5/PPPP1PPP/R1BQKBNR b KQkq - 2 3");
        assertEquals(prediction, model.predict(b), 1e-12);
    }
    @Test void tinyOverfitAndExactResume() throws Exception {
        var model = new PooledRelationCandidate(71);
        var example = new BrnResearchData.Example(Board.fromFen(RelationalCandidateTest.FEN), 80, 78, 0);
        for (int i = 0; i < 1000; i++) model.train(example, .001, false);
        assertEquals(.8, model.predict(example.board()), 1e-5);
        var bytes = new ByteArrayOutputStream(); model.write(bytes);
        var restored = PooledRelationCandidate.read(new ByteArrayInputStream(bytes.toByteArray()));
        for (int i = 0; i < 10; i++) { model.train(example, .001, true); restored.train(example, .001, true); }
        assertArrayEquals(model.weights, restored.weights); assertArrayEquals(model.first, restored.first); assertArrayEquals(model.second, restored.second);
    }
    @Test void stmSpecificVariantHasCorrectGradientAndColorSymmetry() throws Exception {
        var model = new PooledRelationCandidate(71, false); var board = Board.fromFen(RelationalCandidateTest.FEN);
        for (int i = PooledRelationCandidate.HEAD; i < model.weights.length; i++) model.weights[i] = .2;
        double expected = model.predict(board); model.backward(board, 1); double[] gradient = model.gradient.clone();
        int[] indices = Arrays.copyOf(model.touched, model.touchedCount);
        for (int n=0; n<indices.length; n+=31) { int i=indices[n]; double old=model.weights[i];model.weights[i]=old+1e-5;double plus=model.predict(board);
            model.weights[i]=old-1e-5;double minus=model.predict(board);model.weights[i]=old;assertEquals(gradient[i],(plus-minus)/2e-5,1e-8); }
        var reversed = Board.fromFen("rnbqkb1r/pppp1ppp/5n2/4p3/4P3/2N5/PPPP1PPP/R1BQKBNR b KQkq - 2 3"); assertEquals(expected,model.predict(reversed),1e-12);
        var bytes=new ByteArrayOutputStream();model.write(bytes);var restored=PooledRelationCandidate.read(new ByteArrayInputStream(bytes.toByteArray()));
        assertFalse(restored.antisymmetric);assertEquals(model.predict(board),restored.predict(board),0);
    }
}
