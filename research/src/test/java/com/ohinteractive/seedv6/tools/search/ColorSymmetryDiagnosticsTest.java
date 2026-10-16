package com.ohinteractive.seedv6.tools.search;

import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn2.Brn2Model;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.search.diagnostics.QsearchDecisionTrace;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.training.model.NetworkModel;

import static org.junit.jupiter.api.Assertions.*;

class ColorSymmetryDiagnosticsTest {
    static final Brn2Model BRN = new Brn2Model();
    static final NetworkModel.Nnue NNUE = new NetworkModel.Nnue(NnueNetwork.initialized(1));

    @Test void allCorpusRootsChildrenAndSpecialMovesAreInvolutiveAndChessEquivalent() {
        Set<String> covered = new HashSet<>();
        for (var p : Brn2DiagnosticCorpus.POSITIONS) {
            var board = Board.fromFen(p.fen());
            verifyPlacementAndState(board);
            verifyMoves(board, covered);
            verifyMoves(ColorReversal.transform(board), covered);
            for (long move : Brn2Diagnostics.legalMoves(board)) verifyPlacementAndState(Brn2Diagnostics.play(board, move));
        }
        assertTrue(covered.containsAll(List.of("quiet", "capture", "gives-check", "evasion", "promotion", "castle", "en-passant")));
    }

    private static void verifyPlacementAndState(long[] board) {
        long[] before = board.clone(), t = ColorReversal.transform(board);
        assertArrayEquals(board, ColorReversal.transform(t));
        assertArrayEquals(before, board);
        for (int sq = 0; sq < 64; sq++) {
            int code = Board.getSquare(board[0], board[1], board[2], board[3], sq);
            assertEquals(code == 0 ? 0 : code ^ 8, Board.getSquare(t[0], t[1], t[2], t[3], sq ^ 56));
        }
        int a = (int) board[Board.STATUS], b = (int) t[Board.STATUS];
        assertEquals(Board.player(a) ^ 1, Board.player(b));
        assertEquals(Board.halfMoveClock(a), Board.halfMoveClock(b));
        assertEquals(Board.fullMoveNumber(a), Board.fullMoveNumber(b));
        for (int color = 0; color < 2; color++) {
            assertEquals(Board.kingSide(a, color), Board.kingSide(b, color ^ 1));
            assertEquals(Board.queenSide(a, color), Board.queenSide(b, color ^ 1));
        }
        assertEquals(Board.enPassantSquare(a) < 0 ? Value.INVALID : Board.enPassantSquare(a) ^ 56, Board.enPassantSquare(b));
    }

    private static void verifyMoves(long[] board, Set<String> covered) {
        var t = ColorReversal.transform(board);
        Map<String, Long> moves = new TreeMap<>();
        for (long move : Brn2Diagnostics.legalMoves(t)) moves.put(Move.coordinate(move), move);
        var originalMoves = Brn2Diagnostics.legalMoves(board);
        assertEquals(originalMoves.length, moves.size());
        for (long move : originalMoves) {
            String coordinate = ColorReversal.coordinate(Move.coordinate(move));
            assertTrue(moves.containsKey(coordinate), coordinate);
            var c = Brn2Diagnostics.play(board, move);
            var tc = ColorReversal.transform(c);
            var played = Brn2Diagnostics.play(t, moves.get(coordinate));
            for (int i = 0; i < 4; i++) assertEquals(tc[i], played[i]);
            assertEquals(tc[Board.KEY], played[Board.KEY]);
            // All legality/draw state commutes; fullmove label follows Black's numbering convention.
            long lowStateMask = (1L << Board.FULL_MOVE_NUMBER_SHIFT) - 1;
            assertEquals(tc[Board.STATUS] & lowStateMask, played[Board.STATUS] & lowStateMask);
            int expectedOffset = Board.player((int) board[Board.STATUS]) == 0 ? 1 : -1;
            assertEquals(expectedOffset, Board.fullMoveNumber((int) played[Board.STATUS]) - Board.fullMoveNumber((int) tc[Board.STATUS]));
            var categories = Brn2Diagnostics.categories(board, c, move);
            assertEquals(categories, Brn2Diagnostics.categories(t, played, moves.get(coordinate)));
            covered.addAll(categories);
        }
    }

    @Test void individualCastlingBitsBothEpDirectionsAndAllCounterBitsArePreserved() {
        for (String rights : List.of("-", "K", "Q", "k", "q", "Kq", "Qk", "KQkq")) {
            for (String side : List.of("w", "b")) {
                var board = Board.fromFen("r3k2r/8/8/8/8/8/8/R3K2R " + side + " " + rights + " - 127 1023");
                verifyPlacementAndState(board);
            }
        }
        var ep = Board.fromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 19");
        verifyPlacementAndState(ep); verifyPlacementAndState(ColorReversal.transform(ep));
        verifyMoves(ep, new HashSet<>()); verifyMoves(ColorReversal.transform(ep), new HashSet<>());
        var board = Board.startingPosition(); board[Board.STATUS] |= 0x1234567800000000L;
        verifyPlacementAndState(board);
        board[Board.STATUS] |= 1L << Board.ESQUARE_SHIFT;
        assertThrows(IllegalArgumentException.class, () -> ColorReversal.transform(board));
    }

    @Test void nnueFeaturesAreIdenticalMultisetsUnderTheCorrespondingPerspective() {
        for (var p : Brn2DiagnosticCorpus.POSITIONS) {
            var board = Board.fromFen(p.fen()); var t = ColorReversal.transform(board);
            for (int perspective = 0; perspective < 2; perspective++)
                assertEquals(features(board, perspective), features(t, perspective ^ 1));
        }
    }
    private static List<Integer> features(long[] board, int perspective) {
        int king = -1;
        for (int s = 0; s < 64; s++) if (Board.getSquare(board[0], board[1], board[2], board[3], s) == (1 | perspective << 3)) king = s;
        List<Integer> result = new ArrayList<>();
        for (int s = 0; s < 64; s++) {
            int code = Board.getSquare(board[0], board[1], board[2], board[3], s);
            if (code != 0) result.add(NnueFeatureSchema.featureIndex(perspective, king, code >>> 3, code & 7, s));
        }
        Collections.sort(result); return result;
    }

    @Test void independentEvaluatorsMatchExistingPathsAndDoNotMutateModels() throws Exception {
        String beforeBrn = digest(new NetworkModel.Brn2(BRN)), beforeNnue = digest(NNUE);
        var m = ColorSymmetryDiagnostics.measure(BRN, NNUE, Brn2DiagnosticCorpus.POSITIONS);
        assertEquals(m, ColorSymmetryDiagnostics.measure(BRN, NNUE, Brn2DiagnosticCorpus.POSITIONS));
        var old = Brn2Diagnostics.measure(BRN, NNUE, Brn2DiagnosticCorpus.POSITIONS);
        assertEquals(15, m.roots().size()); assertEquals(233, m.children().size());
        for (int i = 0; i < m.children().size(); i++) {
            var p = m.children().get(i);
            assertEquals(old.children().get(i).value().score(), p.original().brn2().score());
            assertEquals(old.children().get(i).value().nnueScore(), p.original().nnue().score());
            assertTrue(Math.abs(p.original().nnue().score() - p.transformed().nnue().score()) <= 1);
        }
        assertEquals(beforeBrn, digest(new NetworkModel.Brn2(BRN))); assertEquals(beforeNnue, digest(NNUE));
    }

    @Test void equalityResidualAndParentPerspectiveEdgeSignsAreDistinct() {
        var a = new ColorSymmetryDiagnostics.Prediction(.2, .1, 100);
        var b = new ColorSymmetryDiagnostics.Prediction(.3, .2, 200);
        assertEquals(0, ColorSymmetryDiagnostics.pair(a, a).get("scoreResidual"));
        var p = new ColorSymmetryDiagnostics.Pair("p", "white", new ColorSymmetryDiagnostics.Values(a, a), new ColorSymmetryDiagnostics.Values(b, b));
        var c = new ColorSymmetryDiagnostics.Pair("c", "black", new ColorSymmetryDiagnostics.Values(b, b), new ColorSymmetryDiagnostics.Values(a, a));
        var e = ColorSymmetryDiagnostics.edge(new ColorSymmetryDiagnostics.Edge("e", List.of(), p, c), false);
        assertEquals(-300, e.get("originalDelta")); assertEquals(-300, e.get("transformedDelta")); assertEquals(0, e.get("signedError"));
        for (var edge : ColorSymmetryDiagnostics.measure(BRN, NNUE, Brn2DiagnosticCorpus.POSITIONS).edges()) {
            int residualSum = edge.parent().original().brn2().score() - edge.parent().transformed().brn2().score()
                    + edge.child().original().brn2().score() - edge.child().transformed().brn2().score();
            assertEquals(-residualSum, ColorSymmetryDiagnostics.edge(edge, false).get("signedError"));
        }
    }

    @Test void statisticsUseAbsoluteQuantilesStrictThresholdsAndSignedMeans() {
        var s = ColorSymmetryDiagnostics.residualStats(new double[]{-200, 0, 100, 1000}, new double[]{0, 0, 0, 0}, new double[]{100, 1000});
        assertEquals(225.0, s.get("signedMean")); assertEquals(325.0, s.get("absoluteMean"));
        assertEquals(150.0, s.get("medianAbsolute")); assertEquals(880.0, (double) s.get("p95Absolute"), 1e-10);
        assertNull(s.get("correlation")); assertEquals(Map.of("100.0", 2L, "1000.0", 0L), s.get("countsStrictlyGreater"));
        assertEquals(Map.of("count", 0), ColorSymmetryDiagnostics.residualStats(new double[0], new double[0], new double[0]));
    }

    @Test void boundedQSnapshotsAreDetachedDeterministicAndNeverChangeEitherDriver() throws Exception {
        var board = Board.fromFen(Brn2DiagnosticCorpus.POSITIONS.get(1).fen()); var before = board.clone();
        String beforeBrn = digest(new NetworkModel.Brn2(BRN)), beforeNnue = digest(NNUE);
        for (boolean brnDriver : List.of(true, false)) {
            var driver = brnDriver ? SearchEvaluation.brn2(BRN) : NNUE.evaluation(NnueScoreMapping.V1);
            var shadow = brnDriver ? NNUE.evaluation(NnueScoreMapping.V1) : SearchEvaluation.brn2(BRN);
            var baseline = Brn2Diagnostics.search(board, driver, 3, 3000, -1, true);
            String previous = null;
            for (int repeat = 0; repeat < 2; repeat++) {
                var trace = new QsearchDecisionTrace(shadow, 1, 7, 50);
                var measured = Brn2Diagnostics.search(board, driver, 3, 3000, -1, true, trace);
                assertEquals(baseline.status(), measured.status()); assertEquals(baseline.nodes(), measured.nodes());
                assertEquals(baseline.diagnostics(), measured.diagnostics());
                assertEquals(baseline.result().score(), measured.result().score());
                assertEquals(baseline.result().bestMove(), measured.result().bestMove());
                assertArrayEquals(baseline.result().principalVariation(), measured.result().principalVariation());
                assertTrue(trace.positionSamples().size() <= 50);
                for (var s : trace.positionSamples()) { assertEquals(0, (s.ordinal() - 1) % 7); assertTrue(s.ordinal() <= 344); }
                StringWriter text = new StringWriter();
                ColorSymmetryDiagnostics.qReport(new PrintWriter(text), "test", brnDriver ? "BRN2" : "NNUE", BRN, NNUE, trace, 7, 50);
                if (previous != null) assertEquals(previous, text.toString());
                previous = text.toString();
                assertTrue(previous.contains("qsymmetry_summary"));
                trace.positionSamples().getFirst().board()[0] = 0;
                assertArrayEquals(before, board);
                trace.reset(); assertTrue(trace.positionSamples().isEmpty());
            }
        }
        assertEquals(beforeBrn, digest(new NetworkModel.Brn2(BRN))); assertEquals(beforeNnue, digest(NNUE));
    }

    @Test void symmetryOptionsAreExplicitAndBounded() {
        assertFalse(Brn2Diagnostics.Options.parse(new String[]{}).symmetry());
        assertTrue(Brn2Diagnostics.Options.parse(new String[]{"--symmetry=true", "--depth=0"}).symmetry());
        for (String option : List.of("--symmetry-stride=0", "--symmetry-limit=20001", "--symmetry=maybe", "--symmetry=true"))
            assertThrows(IllegalArgumentException.class, () -> Brn2Diagnostics.Options.parse(new String[]{option}));
    }

    private static String digest(NetworkModel model) throws Exception {
        var digest = java.security.MessageDigest.getInstance("SHA-256");
        model.write(new java.security.DigestOutputStream(OutputStream.nullOutputStream(), digest));
        return HexFormat.of().formatHex(digest.digest());
    }
}
