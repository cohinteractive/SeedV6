package com.ohinteractive.seedv6.tools.search;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SearchBenchmarkTest {

    @Test
    void explicitNnueBenchmarkDefaultsToV1AndPreservesReferenceIdentity() {
        assertThrows(IllegalArgumentException.class, () -> SearchBenchmark.main(
            new String[] {"--evaluation=nnue-incremental", "--nnue-scale=1000000", "--heuristics=production"}
        ));
        final PrintStream original = System.out;
        final String[] outputs = new String[2];
        final String[] modes = {"nnue-incremental", "nnue-recompute"};
        try {
            for(int i = 0; i < modes.length; i++) {
                final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                System.setOut(new PrintStream(bytes, true, StandardCharsets.UTF_8));
                SearchBenchmark.main(new String[] {
                    "--evaluation=" + modes[i], "--nnue-seed=73",
                    "--depth=2", "--warmup=0", "--repetitions=1", "--diagnostics=enabled"
                });
                outputs[i] = bytes.toString(StandardCharsets.UTF_8);
                assertTrue(outputs[i].contains("evaluation=" + modes[i]));
                assertTrue(outputs[i].contains("boundedOutputScale=32511.0"));
                assertTrue(outputs[i].contains("calibration=none aspiration=full-window"));
                assertTrue(outputs[i].contains("heuristics=mate"));
                assertTrue(outputs[i].contains("benchmark status=PASS"));
            }
        } finally {
            System.setOut(original);
        }
        // Exclude timing/environment output: compare all corpus score/move/PV/node identities.
        var incremental = outputs[0].lines().filter(line -> line.startsWith("result "))
            .map(line -> line.substring(line.indexOf(" score="), line.indexOf(" elapsedNs="))).toList();
        var reference = outputs[1].lines().filter(line -> line.startsWith("result "))
            .map(line -> line.substring(line.indexOf(" score="), line.indexOf(" elapsedNs="))).toList();
        assertEquals(12, incremental.size());
        assertEquals(reference, incremental);
    }

    @Test
    void corpusHasStableUniqueNamesAndValidExactFens() {
        assertEquals(12, SearchBenchmark.corpus().size());
        final HashSet<String> names = new HashSet<>();
        for(SearchBenchmark.Position position : SearchBenchmark.corpus()) {
            assertTrue(names.add(position.name()), position.name());
            assertEquals(Board.MAX_BITBOARDS, Board.fromFen(position.fen()).length);
        }
    }

    @Test
    void repeatedColdBothModeRunEnforcesDeterministicContractAndContextOutput() {
        final PrintStream original = System.out;
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(bytes, true, StandardCharsets.UTF_8));
            SearchBenchmark.main(new String[] {
                "--depth=1", "--warmup=0", "--repetitions=2",
                "--diagnostics=both", "--tt=cold"
            });
        } finally {
            System.setOut(original);
        }
        final String output = bytes.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("corpusVersion=1 threads=1 depth=1"));
        assertTrue(output.contains("corpus=opening-start"));
        assertTrue(output.contains("fen=\"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR"));
        assertTrue(output.contains("diagnostics=false ttPolicy=cold"));
        assertTrue(output.contains("diagnostics=true ttPolicy=cold"));
        assertTrue(output.contains("summary observedDiagnosticsDifferencePercent="));
        assertTrue(output.contains("benchmark status=PASS deterministic-results-and-counters"));
    }

    @Test
    void allOffDepthThreeRetainsTheCommittedWs12BenchmarkIdentity() {
        final PrintStream original = System.out;
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(bytes, true, StandardCharsets.UTF_8));
            SearchBenchmark.main(new String[] {
                "--depth=3", "--warmup=0", "--repetitions=1",
                "--diagnostics=enabled", "--tt=cold", "--heuristics=all-off"
            });
        } finally {
            System.setOut(original);
        }
        final String output = bytes.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("heuristics=all-off"));
        assertTrue(output.contains(
            "corpus=opening-start fen=\"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR"
                + " w KQkq - 0 1\" requestedDepth=3"
        ));
        assertTrue(output.contains("score=38 bestMove=b1c3 pv=\"b1c3 e7e5 e2e4\""));
        assertTrue(output.contains("nodesPerSuite=15878"));
        assertTrue(output.contains("benchmark status=PASS deterministic-results-and-counters"));
    }
}
