package com.ohinteractive.seedv6.tools.search;

import java.util.*;
import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn2.Brn2Model;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.search.diagnostics.QsearchDecisionTrace;
import com.ohinteractive.seedv6.search.diagnostics.QsearchDecisionTrace.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;

import static org.junit.jupiter.api.Assertions.*;

class QsearchDecisionTraceTest {
    private static final SearchEvaluation BRN = SearchEvaluation.brn2(new Brn2Model());
    private static final SearchEvaluation NNUE = SearchEvaluation.incremental(NnueNetwork.initialized(1));

    @Test void boundariesAndLocalCounterfactualClassesAreExplicit() {
        assertEquals(Window.BELOW_ALPHA, QsearchDecisionTrace.window(-11, -10, 20));
        assertEquals(Window.AT_ALPHA, QsearchDecisionTrace.window(-10, -10, 20));
        assertEquals(Window.INSIDE_WINDOW, QsearchDecisionTrace.window(19, -10, 20));
        assertEquals(Window.AT_OR_ABOVE_BETA, QsearchDecisionTrace.window(20, -10, 20));
        assertEquals(Cutoff.BOTH, QsearchDecisionTrace.classify(20, 21, 20));
        assertEquals(Cutoff.DRIVER_ONLY, QsearchDecisionTrace.classify(20, 19, 20));
        assertEquals(Cutoff.SHADOW_ONLY, QsearchDecisionTrace.classify(19, 20, 20));
        assertEquals(Cutoff.NEITHER, QsearchDecisionTrace.classify(19, -30, 20));
    }

    @Test void hostileShadowScoresNeverChangeDriverResultMovePvNodesOrExistingCounters() {
        // Opposite near-boundary shadows would completely change stand-pat if accidentally used.
        for (var driver : List.of(BRN, NNUE)) {
            for (var position : Brn2DiagnosticCorpus.POSITIONS) {
                long[] board = Board.fromFen(position.fen()), before = board.clone();
                var baseline = Brn2Diagnostics.search(board, driver, 2, 1500, -1, true);
                assertNotEquals("FAILURE", baseline.status(), baseline.error());
                for (var shadow : List.of(constant(-2), constant(2))) {
                    var trace = new QsearchDecisionTrace(shadow, 1);
                    var traced = Brn2Diagnostics.search(board, driver, 2, 1500, -1, true, trace);
                    sameSearch(baseline, traced);
                    assertEquals(baseline.diagnostics(), traced.diagnostics());
                    assertArrayEquals(before, board);
                    var summary = trace.summary();
                    var all = object(summary.get("all"));
                    assertEquals(summary.get("observed"), all.get("nodes"));
                    assertEquals(traced.diagnostics().worker().qsearch().standPatCutoffs(), all.get("driverStandPatBetaCutoffs"));
                    assertEquals(traced.diagnostics().worker().nodes().qNodes(), summary.get("observedCountedQChildren"));
                    assertEquals((long) all.get("nodes"), object(all.get("reasons")).values().stream().mapToLong(v -> (long) v).sum());
                    assertEquals((long) all.get("standPatAllowed"), (long) all.get("both") + (long) all.get("driverOnly")
                            + (long) all.get("shadowOnly") + (long) all.get("neither"));
                }
            }
        }
    }

    @Test void bothRealDirectionsRepeatIncludingEveryNonTimingTraceFieldAndBoardPerspective() {
        for (var pair : List.of(List.of(BRN, NNUE), List.of(NNUE, BRN))) {
            for (String name : List.of("middlegame-kiwipete", "opening-ruy-lopez", "queen-endgame")) {
                var p = Brn2DiagnosticCorpus.POSITIONS.stream().filter(v -> v.name().equals(name)).findFirst().orElseThrow();
                var board = Board.fromFen(p.fen());
                var a = new QsearchDecisionTrace(pair.get(1), 1);
                var b = new QsearchDecisionTrace(pair.get(1), 1);
                var first = Brn2Diagnostics.search(board, pair.get(0), 2, 3000, -1, true, a);
                var second = Brn2Diagnostics.search(board, pair.get(0), 2, 3000, -1, true, b);
                sameSearch(first, second);
                assertEquals(a.summary(), b.summary()); assertEquals(a.samples(), b.samples());
                var state = pair.get(1).newState(1);
                boolean hasBlack = false;
                for (var sample : a.samples()) {
                    if (sample.get("shadowStatic") == null) continue;
                    @SuppressWarnings("unchecked") var hex = (List<String>) sample.get("boardLongsHex");
                    long[] observed = hex.stream().mapToLong(s -> Long.parseUnsignedLong(s, 16)).toArray();
                    state.initialize(observed, 0);
                    assertEquals(state.evaluate(observed, 0), sample.get("shadowStatic"));
                    boolean white = Board.player((int) observed[Board.STATUS]) == 0;
                    hasBlack |= !white;
                    assertEquals(white ? "white" : "black", sample.get("sideToMove"));
                    assertEquals((int) sample.get("driverStatic") - (int) sample.get("shadowStatic"), sample.get("driverMinusShadow"));
                    @SuppressWarnings("unchecked") var children = (List<Map<String, Object>>) sample.get("childExamples");
                    for (var child : children) assertEquals(-(int) child.get("childScoreSideToMove"), child.get("scoreParentPerspective"));
                }
                assertTrue(hasBlack);
            }
        }
    }

    @Test void exactHistogramAndSampledAccountingResetWithoutPretendingSampleIsPopulation() {
        var trace = new QsearchDecisionTrace(constant(0), 2);
        trace.beginAttempt(4);
        var board = Board.startingPosition(); var before = board.clone();
        for (int i = 0; i < 10; i++) {
            trace.enter(board, 0, 0, -1, 1, false, false);
            trace.prepared(0, 0);
            trace.staticScore(0, i - 4, true);
            trace.end(0, i - 4 >= 1 ? Reason.STAND_PAT_BETA : Reason.NO_TACTICAL_MOVES, i - 4);
            trace.leave(0);
        }
        assertArrayEquals(before, board);
        var all = object(trace.summary().get("all"));
        assertEquals(10L, all.get("nodes")); assertEquals(5L, all.get("shadowEvaluated"));
        assertEquals(5L, all.get("driverStandPatBetaCutoffs"));
        assertEquals(2L, all.get("driverOnly")); assertEquals(3L, all.get("neither"));
        var diff = object(all.get("absoluteDifference"));
        assertEquals(2.4, diff.get("mean")); assertEquals(2.0, diff.get("median"));
        assertEquals(4.0, diff.get("p95")); assertEquals(4, diff.get("max"));
        assertEquals(10, trace.samples().size());
        trace.reset(); assertEquals(0L, trace.summary().get("observed")); assertTrue(trace.samples().isEmpty());
        assertNull(object(object(trace.summary().get("all")).get("absoluteDifference")).get("mean"));
    }

    @Test void nestedShadowOnlyDescendantsCountOnceAndTraceRemainsBounded() {
        var trace = new QsearchDecisionTrace(constant(2), 1);
        trace.beginAttempt(4);
        var board = Board.startingPosition();
        trace.enter(board, 0, 0, -1, 0, false, false);
        trace.staticScore(0, -1, true);
        long move = Brn2Diagnostics.legalMoves(board)[0];
        trace.moveStarted(0, move, -1, false);
        trace.enter(board, 1, 1, -1, 0, false, true); trace.staticScore(1, -1, true);
        trace.moveStarted(1, move, -1, false);
        trace.enter(board, 2, 2, -1, 0, false, true); trace.staticScore(2, -1, true);
        trace.end(2, Reason.NO_TACTICAL_MOVES, -1); trace.leave(2);
        trace.moveReturned(1, -1); trace.end(1, Reason.CHILD_BETA, 1); trace.leave(1);
        trace.moveReturned(0, 1); trace.end(0, Reason.MOVES_EXHAUSTED, -1); trace.leave(0);
        assertEquals(2L, trace.summary().get("shadowOnlyAncestorDescendants"));
        var all = object(trace.summary().get("all"));
        assertEquals(1L, all.get("shadowOnlyFrontierNodes"));
        assertEquals(2L, all.get("shadowOnlyFrontierDescendants"));
        var adjacent = object(trace.summary().get("adjacentStaticAbsoluteDelta"));
        assertEquals(2L, object(adjacent.get("driver")).get("count"));
        assertEquals(2.0, object(adjacent.get("driver")).get("median"));
        assertEquals(2L, object(adjacent.get("shadowBelowShadowOnlyParent")).get("count"));
        for (int i = 0; i < 20000; i++) {
            trace.enter(board, 0, 0, -1, 0, false, false); trace.staticScore(0, -2, true);
            trace.end(0, Reason.NO_TACTICAL_MOVES, -2); trace.leave(0);
        }
        assertTrue(trace.samples().size() <= 8 + 128 + 16);
        assertEquals(20003L, trace.summary().get("observed"));
    }

    @Test void explicitOptionsRejectUnsafeOrAmbiguousSelection() {
        assertThrows(IllegalArgumentException.class, () -> Brn2Diagnostics.Options.parse(new String[]{"--qshadow=true"}));
        assertThrows(IllegalArgumentException.class, () -> Brn2Diagnostics.Options.parse(new String[]{"--positions=unknown"}));
        assertThrows(IllegalArgumentException.class, () -> Brn2Diagnostics.Options.parse(new String[]{"--shadow-stride=0"}));
        assertThrows(IllegalArgumentException.class, () -> Brn2Diagnostics.Options.parse(new String[]{"--qshadow=yes"}));
        var options = Brn2Diagnostics.Options.parse(new String[]{"--nnue=initialized", "--qshadow=true", "--drivers=nnue", "--positions=middlegame-kiwipete"});
        assertTrue(options.qshadow()); assertEquals(1, options.shadowStride());
    }

    private static SearchEvaluation constant(double raw) {
        double[] parameters = new double[Brn2Model.PARAMETER_COUNT];
        parameters[Brn2Model.OUTPUT_BIAS] = raw;
        return SearchEvaluation.brn2(new Brn2Model(parameters));
    }

    @SuppressWarnings("unchecked") private static Map<String, Object> object(Object value) { return (Map<String, Object>) value; }

    private static void sameSearch(Brn2Diagnostics.SearchMeasurement a, Brn2Diagnostics.SearchMeasurement b) {
        assertNotEquals("FAILURE", b.status(), b.error());
        assertEquals(a.status(), b.status()); assertEquals(a.nodes(), b.nodes());
        if (a.result() == null) assertNull(b.result());
        else {
            assertEquals(a.result().score(), b.result().score()); assertEquals(a.result().bestMove(), b.result().bestMove());
            assertEquals(a.result().depth(), b.result().depth()); assertEquals(a.result().hasMove(), b.result().hasMove());
            assertArrayEquals(a.result().principalVariation(), b.result().principalVariation());
        }
    }
}
