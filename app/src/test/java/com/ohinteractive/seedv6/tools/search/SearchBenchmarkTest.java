package com.ohinteractive.seedv6.tools.search;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchBenchmarkTest {

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
