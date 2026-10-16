package com.ohinteractive.seedv6.tools.search;

import java.util.ArrayList;
import java.util.List;

/** Fixed FENs, full status preserved; each search starts with singleton game history. */
final class Brn2DiagnosticCorpus {
    static final String ID = "seedv6-brn2-diagnostics-v1";
    static final List<SearchBenchmark.Position> POSITIONS = positions();

    private static List<SearchBenchmark.Position> positions() {
        var positions = new ArrayList<>(SearchBenchmark.corpus());
        // Existing BrnRemediationBenchmark opening fixture.
        positions.add(new SearchBenchmark.Position("opening-ruy-lopez",
                "r1bqkbnr/pppp1ppp/2n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3"));
        // Bounded additional quiet middlegame fixture, not a claimed pathology reproducer.
        positions.add(new SearchBenchmark.Position("quiet-fianchetto",
                "r1bq1rk1/ppp1bppp/2n2n2/3pp3/8/1P1P1NP1/PBP1PPBP/RN1Q1RK1 w - - 0 8"));
        // Existing bootstrap smoke fixture supplies non-capturing checking moves.
        positions.add(new SearchBenchmark.Position("queen-endgame",
                "7k/8/5K2/8/8/8/3Q4/8 w - - 0 1"));
        return List.copyOf(positions);
    }

    private Brn2DiagnosticCorpus() {}
}
