package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.exact.ConstructionSearch;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IncrementalOneTest {
    static final String[] SPECIAL = {
        "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
        "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1",
        "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1",
        "4k3/P7/8/8/8/8/7p/4K3 b - - 0 1",
        "4k3/P7/8/8/8/8/7p/4K3 w - - 0 1",
        "6k1/8/8/3q4/4Q3/8/8/6K1 w - - 0 1"
    };
    static LowRankRelationNetwork model() {
        var m = new LowRankRelationNetwork(false, .01, 71);
        var r = new Random(97);
        for (int i = 0; i < m.weights.length; i++) m.weights[i] = .99 * (2 * r.nextDouble() - 1);
        return m;
    }
    static int rounded(double cp) { return (cp < 0 ? -1 : 1) * (int)Math.round(Math.min(Math.abs(cp), 32511)); }
    static void close(double expected, double actual) { assertEquals(expected, actual, 1e-8 + 1e-12 * Math.abs(expected)); }
    static long[] child(long[] b, long move) {
        var c = new long[6]; Board.makeMoveInto(b[0], b[1], b[2], b[3], (int)b[4], b[5], move, c); return c;
    }

    @Test void allSpecialMovesNullParentSiblingUndoColourAndQuadraticOracle() {
        var m = model(); var fast = new IncrementalOne(m); var w = new LowRankRelationNetwork.Workspace();
        var parent = fast.newAccumulator(); var next = fast.newAccumulator(); var mirrored = fast.newAccumulator();
        var moves = new long[256]; var scratch = new long[6]; int tested = 0;
        double max = 0;
        for (String fen : SPECIAL) {
            var b = Board.fromFen(fen); var saved = b.clone(); parent.rebuild(b); double root = parent.scoreCp(b);
            close(400 * LowRankRelationNetworkTest.oracle(m, b), root);
            int n = Gen.genAll(b[0], b[1], b[2], b[3], (int)b[4], b[5], true, moves, scratch);
            for (int i = -1; i < n; i++) {
                long[] c = new long[6];
                if (i < 0) Board.nullMoveInto(b[0], b[1], b[2], b[3], (int)b[4], b[5], c);
                else c = child(b, moves[i]);
                next.update(b, c, parent); double actual = next.scoreCp(c), expected = m.scoreCp(c, w);
                close(expected, actual); close(400 * LowRankRelationNetworkTest.oracle(m, c), actual);
                assertEquals(rounded(expected), rounded(actual)); max = Math.max(max, Math.abs(actual - expected));
                assertEquals(root, parent.scoreCp(b)); assertArrayEquals(saved, b); tested++;
                var mirror = c.clone(); long occupied = c[0] | c[1] | c[2];
                for (int k = 0; k < 3; k++) mirror[k] = Long.reverseBytes(c[k]);
                mirror[3] = Long.reverseBytes(occupied ^ c[3]); mirror[4] ^= 1;
                mirrored.rebuild(mirror); close(actual, mirrored.scoreCp(mirror));
            }
        }
        System.out.println("special/null transitions=" + tested + " max_cp_error=" + max);
    }

    @Test void longReversibleHistoryTranspositionsResetAndImmutableSnapshot() {
        var m = model(); var fast = new IncrementalOne(m); var a = fast.newAccumulator(); var b = fast.newAccumulator();
        var w = new LowRankRelationNetwork.Workspace(); var board = Board.startingPosition(); var rng = new Random(20261029);
        var moves = new long[256]; var scratch = new long[6]; a.rebuild(board); double max = 0; int roundedDifferences = 0;
        for (int i = 0; i < 10000; i++) {
            int n = Gen.genAll(board[0], board[1], board[2], board[3], (int)board[4], board[5], true, moves, scratch);
            if (n == 0 || i % 100 == 0) { board = Board.startingPosition(); a.rebuild(board); }
            else {
                var c = child(board, moves[rng.nextInt(n)]); b.update(board, c, a);
                double expected = m.scoreCp(c, w), actual = b.scoreCp(c);
                close(expected, actual); max = Math.max(max, Math.abs(expected - actual));
                if (rounded(expected) != rounded(actual)) roundedDifferences++;
                // Undo through changed squares instead of retaining a parent; reaches repeated input by another history.
                a.update(c, board, b); close(m.scoreCp(board, w), a.scoreCp(board));
                b.update(board, c, a); var swap = a; a = b; b = swap; board = c;
            }
        }
        assertEquals(0, roundedDifferences);
        board = Board.startingPosition(); a.rebuild(board); double frozen = a.scoreCp(board);
        Arrays.fill(m.weights, 0); assertEquals(frozen, a.scoreCp(board));
        System.out.println("random/make-undo transitions=10000 max_cp_error=" + max + " rounded_differences=" + roundedDifferences);
    }

    @Test void searchLifecycleQsearchCopyBoundsIsolationAndReset() {
        var m = model(); var fast = new IncrementalOne(m); var definition = fast.searchDefinition();
        assertTrue(definition.isResearchCentipawnsStateful()); assertFalse(definition.isResearchCentipawnsFullRecompute());
        var main = definition.newState(8); var q = definition.newState(8); var independent = definition.newState(8);
        var w = new LowRankRelationNetwork.Workspace(); var moves = new long[256]; var scratch = new long[6];
        for (String fen : SPECIAL) {
            var board = Board.fromFen(fen); main.initialize(board, 0); int parent = main.evaluate(board, 0);
            int n = Gen.genAll(board[0], board[1], board[2], board[3], (int)board[4], board[5], true, moves, scratch);
            for (int i = 0; i < n; i++) {
                var c = child(board, moves[i]); main.child(board, c, 0); q.initializeFrom(c, 1, main);
                independent.initialize(c, 7); int expected = rounded(m.scoreCp(c, w));
                assertEquals(expected, main.evaluate(c, 1)); assertEquals(expected, q.evaluate(c, 1));
                assertEquals(expected, independent.evaluate(c, 7)); assertEquals(parent, main.evaluate(board, 0));
                var nullChild = new long[6]; Board.nullMoveInto(c[0], c[1], c[2], c[3], (int)c[4], c[5], nullChild);
                q.child(c, nullChild, 1); assertEquals(rounded(m.scoreCp(nullChild, w)), q.evaluate(nullChild, 2));
                assertEquals(expected, main.evaluate(c, 1));
            }
        }
        var board = Board.startingPosition();
        assertThrows(IndexOutOfBoundsException.class, () -> main.initialize(board, 8));
        assertThrows(IllegalArgumentException.class, () -> main.initializeFrom(board, 0, main));
        assertThrows(IllegalArgumentException.class, () -> q.initializeFrom(board, 0, fast.searchDefinition().newState(8)));
        var fresh = fast.newAccumulator(); assertThrows(IllegalStateException.class, () -> fresh.scoreCp(board));
        assertThrows(IllegalArgumentException.class, () -> new IncrementalOne(new LowRankRelationNetwork(true, .01, 71)));
        var a = fast.newAccumulator(); a.rebuild(board);
        assertThrows(IllegalArgumentException.class, () -> a.update(board, board, a));
        assertThrows(IllegalArgumentException.class, () -> fresh.copyFrom(board, new IncrementalOne(model()).newAccumulator()));
    }

    @Test void realConstructionSearchMatchesFullReferenceAndRepeatsDeterministically() {
        var m = model(); var fast = new IncrementalOne(m).searchDefinition();
        var reference = SearchEvaluation.researchCentipawnsFullRecompute(() -> {
            var w = new LowRankRelationNetwork.Workspace(); return board -> m.scoreCp(board, w);
        });
        var configuration = ConstructionSearch.v1(1, 1, AspirationSearch.Policy.ADAPTIVE16);
        for (String fen : new String[]{SPECIAL[0], SPECIAL[2], SPECIAL[3], SPECIAL[5]}) {
            var board = Board.fromFen(fen);
            long expectedMove = -1; int expectedScore = 0; long expectedNodes = 0;
            for (var definition : new SearchEvaluation[]{reference, fast, fast}) {
                try (var driver = new SearchDriver(configuration.create(definition))) {
                    var outcome = driver.search(new SearchRequest(board, 3)); assertTrue(outcome.targetDepthCompleted());
                    var result = outcome.lastCompletedResult();
                    if (expectedMove == -1) { expectedMove = result.bestMove(); expectedScore = result.score(); expectedNodes = result.nodes(); }
                    else { assertEquals(expectedMove, result.bestMove()); assertEquals(expectedScore, result.score()); assertEquals(expectedNodes, result.nodes()); }
                    assertEquals(0, driver.activeSearchThreads());
                }
            }
        }
    }

    @Test void canonicalRebuildAndHalfCentipawnGuardMatchReferenceOperationOrder() {
        var model = new LowRankRelationNetwork(false, .01, 71);
        var w = new LowRankRelationNetwork.Workspace();
        for (String fen : SPECIAL) {
            var board = Board.fromFen(fen); double original = model.scoreCp(board, w);
            double savedBias = model.weights[768];
            for (double perturbation : new double[]{-1e-10, 0, 1e-10}) {
                double target = Math.floor(original) + .5 + perturbation;
                model.weights[768] = savedBias + (target - original) / 400;
                var fast = new IncrementalOne(model); var rebuilt = fast.newAccumulator(); rebuilt.rebuild(board);
                assertEquals(model.scoreCp(board, w), rebuilt.canonicalScoreCp(board));
                var definition = fast.searchDefinition(); var main = definition.newState(257); main.initialize(board, 0);
                var nullBoard = new long[6]; Board.nullMoveInto(board[0], board[1], board[2], board[3], (int)board[4], board[5], nullBoard);
                for (int ply = 0; ply < 256; ply++) {
                    var parent = (ply & 1) == 0 ? board : nullBoard; var child = (ply & 1) == 0 ? nullBoard : board;
                    main.child(parent, child, ply); assertEquals(rounded(model.scoreCp(child, w)), main.evaluate(child, ply+1));
                }
            }
            model.weights[768] = savedBias;
        }
    }

    @Test void differentLegalMoveOrdersReachEquivalentTransposedState() {
        var model = model(); var fast = new IncrementalOne(model); var definition = fast.searchDefinition();
        double[] scores = new double[2]; long[][] positions = new long[2][];
        String[][] paths = {{"g1f3","g8f6","b1c3","b8c6"},{"b1c3","b8c6","g1f3","g8f6"}};
        for (int path = 0; path < 2; path++) {
            var state = definition.newState(5); var board = Board.startingPosition(); state.initialize(board, 0);
            for (int ply = 0; ply < 4; ply++) {
                long[] moves = new long[256], scratch = new long[6];
                int count = Gen.genAll(board[0],board[1],board[2],board[3],(int)board[4],board[5],true,moves,scratch);
                long selected = 0;
                for (int i = 0; i < count; i++) if (com.ohinteractive.seedv6.core.move.Move.coordinate(moves[i]).equals(paths[path][ply])) selected=moves[i];
                assertNotEquals(0, selected); var next = child(board, selected); state.child(board, next, ply); board=next;
            }
            scores[path] = state.evaluate(board, 4); positions[path] = board;
        }
        assertArrayEquals(positions[0], positions[1]); assertEquals(scores[0],scores[1]);
    }
}
