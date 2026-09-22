package com.ohinteractive.seedv6.tools.search;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn2.*;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.alphabeta.AlphaBetaPvsSearch;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.model.*;

import static org.junit.jupiter.api.Assertions.*;

class Brn2DiagnosticsTest {
    private static final Brn2Model MODEL = new Brn2Model();
    @TempDir Path temporary;

    @Test void rawInspectionRetainsActualPreTanhAndInvalidatesFailedEvaluation() {
        var workspace = new Brn2Workspace();
        var accumulator = new Brn2Accumulator(MODEL);
        assertThrows(IllegalStateException.class, workspace::raw);
        assertThrows(IllegalStateException.class, accumulator::raw);
        var board = Board.startingPosition();
        var before = board.clone();
        assertEquals(MODEL.evaluateReference(board, workspace), StrictMath.tanh(workspace.raw()));
        accumulator.rebuild(board);
        assertEquals(accumulator.evaluate(board), StrictMath.tanh(accumulator.raw()));
        assertEquals(workspace.raw(), accumulator.raw(), 1e-12);
        assertArrayEquals(before, board);
        assertThrows(IllegalArgumentException.class, () -> accumulator.evaluate(new long[0]));
        assertThrows(IllegalStateException.class, accumulator::raw);
        assertThrows(RuntimeException.class, () -> MODEL.evaluate(new long[0], workspace));
        assertThrows(IllegalStateException.class, workspace::raw);
    }

    @Test void deterministicCorpusAndEveryChildMatchProductionAndPreserveParents() {
        var reference = new NetworkModel.Nnue(NnueNetwork.initialized(1));
        var corpus = Brn2DiagnosticCorpus.POSITIONS;
        var first = Brn2Diagnostics.measure(MODEL, reference, corpus);
        assertEquals(first, Brn2Diagnostics.measure(MODEL, reference, corpus));
        assertEquals(15, first.roots().size());
        assertEquals(corpus.size(), corpus.stream().map(SearchBenchmark.Position::name).distinct().count());
        var evaluator = new Brn2Diagnostics.Evaluator(MODEL, reference);
        var state = SearchEvaluation.brn2(MODEL).newState(2);
        var nnue = reference.evaluation(NnueScoreMapping.V1).newState(2);
        for (var position : corpus) {
            var board = Board.fromFen(position.fen());
            var before = board.clone();
            var root = evaluator.root(board);
            state.initialize(board, 0); nnue.initialize(board, 0);
            assertEquals(state.evaluate(board, 0), root.score());
            assertEquals(nnue.evaluate(board, 0), root.nnueScore());
            for (long move : Brn2Diagnostics.legalMoves(board)) {
                var child = Brn2Diagnostics.play(board, move);
                var childBefore = child.clone();
                var value = evaluator.child(board, child);
                state.child(board, child, 0); nnue.child(board, child, 0);
                assertEquals(state.evaluate(child, 1), value.score());
                assertEquals(nnue.evaluate(child, 1), value.nnueScore());
                assertEquals(root.score(), state.evaluate(board, 0));
                assertArrayEquals(before, board); assertArrayEquals(childBefore, child);
            }
        }
    }

    @Test void childDeltasUseParentPerspectiveForBothSides() {
        var a = new Brn2Diagnostics.Value(.2, .1, 100, null);
        var b = new Brn2Diagnostics.Value(-.3, -.15, -150, null);
        var delta = new Brn2Diagnostics.Delta("id", "parent", "e2e4", List.of("quiet"), a, b);
        assertEquals(50, delta.signedScoreDelta());
        assertEquals(50, delta.absoluteScoreDelta());
        assertEquals(.05, delta.absoluteValueDelta(), 1e-15);
        var corpus = Brn2DiagnosticCorpus.POSITIONS;
        var measured = Brn2Diagnostics.measure(MODEL, null, corpus);
        assertTrue(measured.deltas().stream().anyMatch(d -> d.parent().equals("opening-ruy-lopez")));
        for (var d : measured.deltas()) {
            assertEquals(Math.abs(d.parentValue().score() + d.childValue().score()), d.absoluteScoreDelta());
            assertTrue(d.absoluteScoreDelta() <= 2 * BrnScoreMapping.SCALE);
        }
    }

    @Test void moveCategoriesCoverSpecialMovesAndCheckingQuietsSeparately() {
        var measured = Brn2Diagnostics.measure(MODEL, null, Brn2DiagnosticCorpus.POSITIONS);
        assertTrue(measured.deltas().stream().anyMatch(d -> d.categories().contains("en-passant") && d.categories().contains("capture")));
        for (String category : List.of("quiet", "capture", "gives-check", "evasion", "promotion", "castle"))
            assertTrue(measured.deltas().stream().anyMatch(d -> d.categories().contains(category)), category);
        assertTrue(measured.deltas().stream().anyMatch(d -> d.categories().contains("gives-check")
                && d.categories().contains("non-capture-non-promotion")));
        for (var d : measured.deltas()) if (d.categories().contains("quiet")) {
            assertFalse(d.categories().contains("capture"));
            assertFalse(d.categories().contains("gives-check"));
            assertFalse(d.categories().contains("promotion"));
        }
    }

    @Test void fullReferenceFallbackExposesSaturatedFiniteRawWithoutChangingMapping() {
        double[] weights = new double[Brn2Model.PARAMETER_COUNT];
        weights[Brn2Model.OUTPUT_BIAS] = 1e101;
        var model = new Brn2Model(weights);
        var evaluator = new Brn2Diagnostics.Evaluator(model, null);
        var value = evaluator.root(Board.startingPosition());
        assertFalse(model.boundedIntermediates());
        assertEquals(1e101, value.raw()); assertEquals(1, value.normalized());
        assertEquals(BrnScoreMapping.SCALE, value.score());
    }

    @Test void statisticsHavePopulationVarianceQuantilesAndTraceableExtremes() {
        var values = List.of(new DiagnosticReport.Sample("a", -2), new DiagnosticReport.Sample("b", 0),
                new DiagnosticReport.Sample("c", 2), new DiagnosticReport.Sample("d", 4));
        var stats = DiagnosticReport.statistics(values);
        assertEquals(4, stats.get("count")); assertEquals(1.0, stats.get("mean"));
        assertEquals(Math.sqrt(5), (double) stats.get("populationStdDev"), 1e-14);
        assertEquals(1.0, stats.get("p50")); assertEquals(3.7, (double) stats.get("p95"), 1e-14);
        assertEquals(List.of(DiagnosticReport.fields("id", "d", "value", 4.0),
                DiagnosticReport.fields("id", "c", "value", 2.0), DiagnosticReport.fields("id", "b", "value", 0.0),
                DiagnosticReport.fields("id", "a", "value", -2.0)), stats.get("highest"));
        assertEquals(Map.of("count", 0), DiagnosticReport.statistics(List.of()));
        var huge = DiagnosticReport.statistics(List.of(new DiagnosticReport.Sample("a", -1e300),
                new DiagnosticReport.Sample("b", 1e300)));
        assertEquals(1e300, huge.get("populationStdDev"));
    }

    @Test void ratioUsesAllEnteredNodesAndZeroDenominatorIsUnavailable() {
        assertEquals(.75, Brn2Diagnostics.qRatio(1, 3));
        assertEquals(.998962, Brn2Diagnostics.qRatio(1038, 998962));
        assertNull(Brn2Diagnostics.qRatio(0, 0));
        assertThrows(IllegalArgumentException.class, () -> Brn2Diagnostics.qRatio(-1, 0));
    }

    @Test void enabledDiagnosticsPreserveNeuralSearchMovesScoresNodesAndPvIncludingCappedWork() {
        for (var evaluation : List.of(SearchEvaluation.brn2(MODEL), SearchEvaluation.incremental(NnueNetwork.initialized(1)))) {
            for (int index : new int[]{0, 1, 3, 10}) {
                var board = Board.fromFen(Brn2DiagnosticCorpus.POSITIONS.get(index).fen());
                var before = board.clone();
                var plain = Brn2Diagnostics.search(board, evaluation, 2, 2000, -1, false);
                var measured = Brn2Diagnostics.search(board, evaluation, 2, 2000, -1, true);
                var repeated = Brn2Diagnostics.search(board, evaluation, 2, 2000, -1, true);
                assertNotEquals("FAILURE", measured.status(), measured.error());
                assertEquals(plain.status(), measured.status()); assertEquals(plain.nodes(), measured.nodes());
                assertEquals(measured.nodes(), measured.diagnostics().totalEnteredNodes());
                assertEquals(measured.diagnostics(), repeated.diagnostics());
                assertTrue(measured.diagnostics().worker().nodes().evaluationCalls() > 0);
                assertArrayEquals(before, board);
                if (plain.result() == null) assertNull(measured.result());
                else {
                    assertEquals(plain.result().score(), measured.result().score());
                    assertEquals(plain.result().bestMove(), measured.result().bestMove());
                    assertEquals(plain.result().depth(), measured.result().depth());
                    assertArrayEquals(plain.result().principalVariation(), measured.result().principalVariation());
                }
            }
        }
    }

    @Test void evaluationCountIncludesRootReportAndQuiescenceStandPatWithoutDoubleCountingNodes() {
        var board = Board.startingPosition();
        var history = GameHistory.initial(board);
        try (var search = new AlphaBetaPvsSearch(SearchEvaluation.brn2(MODEL), 1024)) {
            var result = search.search(new SearchRequest(board, history, 0, SearchObserver.NONE,
                    SearchControl.controlled(1000, 0, -1, TimeSource.SYSTEM), true));
            assertEquals(0, result.nodes());
            assertEquals(2, result.diagnostics().worker().nodes().evaluationCalls());
            assertEquals(1, history.size()); assertTrue(history.matchesCurrent(board));
        }
    }

    @Test void nodeAndTimeLimitsReportNoInventedCompletedMove() {
        for (var limits : List.of(new long[]{0, -1}, new long[]{1000, 0})) {
            var result = Brn2Diagnostics.search(Board.startingPosition(), SearchEvaluation.brn2(MODEL),
                    4, limits[0], limits[1], true);
            assertEquals(limits[0] == 0 ? "NODE_LIMIT" : "TIME_LIMIT", result.status());
            assertNull(result.result()); assertEquals(0, result.nodes());
            assertEquals(0, result.diagnostics().totalEnteredNodes());
        }
    }

    @Test void inferenceFailureIsExplicitAndDoesNotPresentStaleCounters() {
        double[] weights = new double[Brn2Model.PARAMETER_COUNT];
        Arrays.fill(weights, Double.MAX_VALUE);
        var board = Board.startingPosition(); var before = board.clone();
        var result = Brn2Diagnostics.search(board, SearchEvaluation.brn2(new Brn2Model(weights)), 1, 100, -1, true);
        assertEquals("FAILURE", result.status()); assertNotNull(result.error());
        assertNull(result.diagnostics()); assertNull(result.result()); assertArrayEquals(before, board);
    }

    @Test void checkpointLoadingAndEvaluationLeaveModelOptimizerAndStoreBytesUnchanged() throws Exception {
        var state = new NetworkTrainingState.Brn2(new Brn2Trainer(.001));
        state.trainer().train(Board.startingPosition(), 1);
        String id;
        try (var store = new CheckpointStore(temporary, TrainingArchitecture.BRN2)) {
            id = store.initialize(state, new CheckpointManifest.Metadata(0, 1, "")).manifest().id();
        }
        var before = hashes(temporary);
        Path checkpoint = temporary.resolve("checkpoints").resolve(id);
        var loaded = Brn2Diagnostics.load(checkpoint.toString(), TrainingArchitecture.BRN2);
        assertEquals(id, loaded.identity().get("checkpointId"));
        assertEquals(1L, loaded.identity().get("optimizerStep"));
        var model = ((NetworkModel.Brn2) loaded.model()).model();
        Brn2Diagnostics.measure(model, null, Brn2DiagnosticCorpus.POSITIONS);
        assertNotEquals("FAILURE", Brn2Diagnostics.search(Board.startingPosition(), loaded.model().evaluation(NnueScoreMapping.V1),
                1, 100, -1, true).status());
        assertEquals(loaded.identity().get("modelSha256"), Brn2Diagnostics.load(
                checkpoint.resolve(TrainingArchitecture.BRN2.networkFile()).toString(), TrainingArchitecture.BRN2).identity().get("modelSha256"));
        assertThrows(IOException.class, () -> Brn2Diagnostics.load(checkpoint.toString(), TrainingArchitecture.NNUE));
        assertEquals(before, hashes(temporary));
    }

    @Test void explicitNnueModelLoadingIsReadOnlyAndDoesNotFallbackOnWrongBytes() throws Exception {
        Path file = temporary.resolve("reference.nnue");
        try (var out = Files.newOutputStream(file)) { new NetworkModel.Nnue(NnueNetwork.initialized(1)).write(out); }
        var before = hashes(temporary);
        var loaded = Brn2Diagnostics.load(file.toString(), TrainingArchitecture.NNUE);
        assertEquals(TrainingArchitecture.NNUE, loaded.model().architecture());
        assertThrows(IOException.class, () -> Brn2Diagnostics.load(file.toString(), TrainingArchitecture.BRN2));
        assertEquals(before, hashes(temporary));
    }

    @Test void cliOutputIsStructuredAndExistingOutputCannotBeOverwritten() throws Exception {
        Path output = temporary.resolve("report with spaces.jsonl");
        Brn2Diagnostics.main(new String[]{"--depth=0", "--output=" + output, "--label=test\"\\\n"});
        String text = Files.readString(output);
        assertTrue(text.startsWith("{\"type\":\"run\",\"schema\":2,"));
        assertTrue(text.contains("\"label\":\"test\\\"\\\\\\u000a\""));
        assertTrue(text.contains("\"type\":\"delta_summary\",\"category\":\"quiet\""));
        assertTrue(text.endsWith("{\"type\":\"end\",\"status\":\"COMPLETE\"}" + System.lineSeparator()));
        assertThrows(FileAlreadyExistsException.class,
                () -> Brn2Diagnostics.main(new String[]{"--depth=0", "--output=" + output}));
        assertEquals(text, Files.readString(output));
    }

    @Test void invalidCliInputsFailBeforeSearch() {
        for (String[] args : List.of(new String[]{"--unknown=1"}, new String[]{"--depth=-1"},
                new String[]{"--nodes=-1", "--time-ms=-1"}, new String[]{"--depth=1", "--depth=2"},
                new String[]{"--repetitions=0"}, new String[]{"--brn2="}))
            assertThrows(IllegalArgumentException.class, () -> Brn2Diagnostics.Options.parse(args));
    }

    private static Map<String, String> hashes(Path root) throws Exception {
        var result = new TreeMap<String, String>();
        try (var paths = Files.walk(root)) {
            for (var path : paths.filter(Files::isRegularFile).toList())
                result.put(root.relativize(path).toString(), Brn2Diagnostics.sha256(Files.readAllBytes(path)));
        }
        return result;
    }
}
