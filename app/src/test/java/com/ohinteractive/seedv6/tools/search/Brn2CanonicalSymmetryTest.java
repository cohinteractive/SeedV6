package com.ohinteractive.seedv6.tools.search;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn2.*;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.search.evaluation.*;
import static org.junit.jupiter.api.Assertions.*;

class Brn2CanonicalSymmetryTest {
    private static final Brn2Model MODEL = new Brn2Model();

    @Test void established248PositionCorpusHasIdenticalOrderedFeaturesFullOutputsAndIncrementalScores() {
        var measured = ColorSymmetryDiagnostics.measure(MODEL, null, Brn2DiagnosticCorpus.POSITIONS);
        var pairs = new ArrayList<>(measured.roots()); pairs.addAll(measured.children());
        assertEquals(248, pairs.size());
        for (var pair : pairs) assertEquals(pair.original().brn2(), pair.transformed().brn2(), pair.id());
        int positions = 0;
        Set<String> categories = new HashSet<>();
        for (var p : Brn2DiagnosticCorpus.POSITIONS) {
            var board = Board.fromFen(p.fen());
            equivalent(board, ColorReversal.transform(board)); positions++;
            for (long move : Brn2Diagnostics.legalMoves(board)) {
                var child = Brn2Diagnostics.play(board, move);
                equivalent(child, ColorReversal.transform(child)); positions++;
                categories.addAll(Brn2Diagnostics.categories(board, child, move));
            }
        }
        assertEquals(248, positions);
        assertTrue(categories.containsAll(List.of("quiet", "capture", "gives-check", "evasion", "castle", "promotion", "en-passant")));
        StringWriter report = new StringWriter();
        ColorSymmetryDiagnostics.staticReport(new PrintWriter(report), MODEL, null, Brn2DiagnosticCorpus.POSITIONS);
        System.out.print(report); // Same diagnostic statistics/definitions as the g315 investigation.
        System.out.println("BRN2_CANONICAL positions=248 orderedFeatureDifferences=0 fullRawDifferences=0 fullNormalizedDifferences=0 searchScoreDifferences=0");
    }

    @Test void bothPerspectivesRemainExactAcrossMappedMovesCopiesSiblingsAndRebuildBoundaries() {
        double maximumError = 0;
        int comparisons = 0;
        for (var p : Brn2DiagnosticCorpus.POSITIONS) for (int side = 0; side < 2; side++) {
            var board = Board.fromFen(p.fen());
            if (side == 1) board = ColorReversal.transform(board);
            var transformed = ColorReversal.transform(board);
            var parent = new Brn2Accumulator(MODEL); parent.rebuild(board);
            var reverseParent = new Brn2Accumulator(MODEL); reverseParent.rebuild(transformed);
            var childState = new Brn2Accumulator(MODEL);
            var reverseChild = new Brn2Accumulator(MODEL);
            Map<String, Long> reverseMoves = new HashMap<>();
            for (long move : Brn2Diagnostics.legalMoves(transformed)) reverseMoves.put(Move.coordinate(move), move);
            double originalParent = parent.evaluate(board);
            for (long move : Brn2Diagnostics.legalMoves(board)) {
                var child = Brn2Diagnostics.play(board, move);
                var reverse = Brn2Diagnostics.play(transformed, reverseMoves.get(ColorReversal.coordinate(Move.coordinate(move))));
                // Actual mapped moves differ in fullmove number: that absolute-color convention is excluded.
                equivalent(child, reverse);
                childState.update(board, child, parent); reverseChild.update(transformed, reverse, reverseParent);
                assertEquals(childState.evaluate(child), reverseChild.evaluate(reverse));
                maximumError = Math.max(maximumError, checkBoth(child, childState)); comparisons += 2;
                assertEquals(originalParent, parent.evaluate(board));
                var copied = new Brn2Accumulator(MODEL); copied.update(child, child, childState);
                assertEquals(childState.evaluate(child), copied.evaluate(child));
                assertEquals(originalParent, reverseParent.evaluate(transformed));
            }
        }
        // A long paired trajectory crosses the pre-existing periodic drift-rebuild boundary.
        var random = new SplittableRandom(6302);
        var board = Board.startingPosition(); var reverse = ColorReversal.transform(board);
        var state = new Brn2Accumulator(MODEL); state.rebuild(board);
        var reverseState = new Brn2Accumulator(MODEL); reverseState.rebuild(reverse);
        for (int ply = 0; ply < 160; ply++) {
            var moves = Brn2Diagnostics.legalMoves(board); if (moves.length == 0) break;
            var child = Brn2Diagnostics.play(board, moves[random.nextInt(moves.length)]);
            var tc = ColorReversal.transform(child);
            var next = new Brn2Accumulator(MODEL); next.update(board, child, state);
            var tn = new Brn2Accumulator(MODEL); tn.update(reverse, tc, reverseState);
            assertEquals(next.evaluate(child), tn.evaluate(tc));
            maximumError = Math.max(maximumError, checkBoth(child, next)); comparisons += 2;
            board = child; reverse = tc; state = next; reverseState = tn;
        }
        System.out.printf(Locale.ROOT, "BRN2_CANONICAL_INCREMENTAL comparisons=%d maxFullError=%.17g symmetryResidual=0%n", comparisons, maximumError);
    }

    @Test void equivalentTrainingExamplesProduceBitIdenticalWeightsAndMoments() throws Exception {
        var original = new Brn2Trainer(.001); var reversed = new Brn2Trainer(.001);
        int n = 0;
        for (var p : Brn2DiagnosticCorpus.POSITIONS) {
            var board = Board.fromFen(p.fen()); double target = (n++ % 3) - 1;
            assertEquals(original.train(board, target), reversed.train(ColorReversal.transform(board), target));
        }
        assertArrayEquals(Brn2Codec.encodeTraining(original), Brn2Codec.encodeTraining(reversed));
        for (var p : Brn2DiagnosticCorpus.POSITIONS) {
            var board = Board.fromFen(p.fen());
            assertEquals(original.predict(board), original.predict(ColorReversal.transform(board)));
        }
    }

    private static double checkBoth(long[] board, Brn2Accumulator state) {
        double maximum = 0;
        for (int flip = 0; flip < 2; flip++) {
            var probe = board.clone(); probe[Board.STATUS] ^= flip;
            var copy = new Brn2Accumulator(MODEL); copy.update(board, probe, state);
            double expected = MODEL.evaluateReference(probe, new Brn2Workspace()), actual = copy.evaluate(probe);
            maximum = Math.max(maximum, Math.abs(expected - actual));
            assertEquals(expected, actual, 2e-13);
            assertEquals(BrnScoreMapping.map(expected), BrnScoreMapping.map(actual));
        }
        return maximum;
    }

    private static void equivalent(long[] board, long[] reversed) {
        var before = board.clone();
        var a = new Brn2Features(); var b = new Brn2Features(); a.extract(board); b.extract(reversed);
        assertEquals(a.size(), b.size()); assertEquals(a.nodeCount(), b.nodeCount());
        for (int i = 0; i < a.size(); i++) assertEquals(a.indexAt(i), b.indexAt(i), "ordered occurrence " + i);
        var x = new Brn2Workspace(); var y = new Brn2Workspace();
        double value = MODEL.evaluate(board, x), transformed = MODEL.evaluate(reversed, y);
        assertEquals(value, transformed); assertEquals(x.raw(), y.raw());
        assertEquals(BrnScoreMapping.map(value), BrnScoreMapping.map(transformed));
        assertArrayEquals(before, board);
    }
}
