package com.ohinteractive.seedv6.tools.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.alphabeta.AlphaBetaPvsSearch;
import com.ohinteractive.seedv6.search.common.SearchControl;
import com.ohinteractive.seedv6.search.common.SearchObserver;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.common.TimeSource;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot;
import com.ohinteractive.seedv6.search.iterative.IterativeDeepeningSearch;
import com.ohinteractive.seedv6.search.iterative.IterativeSearchOutcome;
import com.ohinteractive.seedv6.search.tt.TranspositionTable;

/**
 * Deterministic single-thread production-search benchmark. Search result and
 * counter reproducibility are enforced; elapsed time and NPS are observations.
 */
public final class SearchBenchmark {

    private static final int THREADS = 1;
    private static final int DEFAULT_DEPTH = 3;
    private static final int DEFAULT_WARMUPS = 1;
    private static final int DEFAULT_REPETITIONS = 3;
    private static final int TT_ENTRIES = 1 << 18;

    public static void main(String[] args) {
        final Options options = Options.parse(args);
        printEnvironment(options);
        warmup(options);

        final List<Long> disabledSuiteTimes = new ArrayList<>();
        final List<Long> enabledSuiteTimes = new ArrayList<>();
        final RunRecord[][] disabledBaseline = new RunRecord[CORPUS.size()][1];
        final RunRecord[][] enabledBaseline = new RunRecord[CORPUS.size()][1];
        long disabledExpectedNodes = -1L;
        long enabledExpectedNodes = -1L;

        for(int repetition = 1; repetition <= options.repetitions; repetition ++) {
            long disabledTotal = 0L;
            long enabledTotal = 0L;
            long disabledNodes = 0L;
            long enabledNodes = 0L;
            for(int index = 0; index < CORPUS.size(); index ++) {
                final Position position = CORPUS.get(index);
                RunRecord disabled = null;
                RunRecord enabled = null;
                for(boolean diagnostics : options.modes(repetition)) {
                    final RunRecord record = run(position, options, diagnostics);
                    printRecord(repetition, record, options);
                    if(diagnostics) {
                        enabled = record;
                        enabledTotal += record.elapsedNanos;
                        enabledNodes += record.result.nodes();
                        requireRepeatable(enabledBaseline[index], record);
                        if(enabledBaseline[index][0] == null) enabledBaseline[index][0] = record;
                    } else {
                        disabled = record;
                        disabledTotal += record.elapsedNanos;
                        disabledNodes += record.result.nodes();
                        requireRepeatable(disabledBaseline[index], record);
                        if(disabledBaseline[index][0] == null) disabledBaseline[index][0] = record;
                    }
                }
                if(disabled != null && enabled != null) requireDiagnosticIdentity(disabled, enabled);
            }
            if(options.includes(false)) {
                disabledSuiteTimes.add(disabledTotal);
                if(disabledExpectedNodes == -1L) disabledExpectedNodes = disabledNodes;
                else if(disabledExpectedNodes != disabledNodes) {
                    throw new AssertionError("Non-deterministic disabled suite node total.");
                }
            }
            if(options.includes(true)) {
                enabledSuiteTimes.add(enabledTotal);
                if(enabledExpectedNodes == -1L) enabledExpectedNodes = enabledNodes;
                else if(enabledExpectedNodes != enabledNodes) {
                    throw new AssertionError("Non-deterministic enabled suite node total.");
                }
            }
        }

        if(!disabledSuiteTimes.isEmpty()) {
            System.out.println("summary diagnostics=disabled medianNs=" + median(disabledSuiteTimes)
                + " nodesPerSuite=" + disabledExpectedNodes);
        }
        if(!enabledSuiteTimes.isEmpty()) {
            System.out.println("summary diagnostics=enabled medianNs=" + median(enabledSuiteTimes)
                + " nodesPerSuite=" + enabledExpectedNodes);
        }
        if(!disabledSuiteTimes.isEmpty() && !enabledSuiteTimes.isEmpty()) {
            if(disabledExpectedNodes != enabledExpectedNodes) {
                throw new AssertionError("Diagnostics changed suite node total.");
            }
            final long disabledMedian = median(disabledSuiteTimes);
            final long enabledMedian = median(enabledSuiteTimes);
            final double relative = disabledMedian == 0L ? 0.0
                : ((double) enabledMedian / disabledMedian - 1.0) * 100.0;
            System.out.printf(Locale.ROOT,
                "summary observedDiagnosticsDifferencePercent=%.3f caution=wall-time-observation-only%n",
                relative);
        }
        System.out.println("benchmark status=PASS deterministic-results-and-counters");
    }

    public static List<Position> corpus() {
        return CORPUS;
    }

    private static void warmup(Options options) {
        for(int repetition = 0; repetition < options.warmups; repetition ++) {
            for(Position position : CORPUS) {
                for(boolean diagnostics : options.modes(repetition + 1)) {
                    run(position, options, diagnostics);
                }
            }
        }
        System.out.println("warmup completed=" + options.warmups + " excludedFromMeasurements=true");
    }

    private static RunRecord run(Position position, Options options, boolean diagnostics) {
        final TranspositionTable table = new TranspositionTable(TT_ENTRIES);
        final AlphaBetaPvsSearch exact = new AlphaBetaPvsSearch(table);
        final IterativeDeepeningSearch iterative = new IterativeDeepeningSearch(exact);
        final long[] board = Board.fromFen(position.fen);
        if(options.ttPolicy == TtPolicy.WARM) {
            search(iterative, board, options.depth, diagnostics);
        }
        final long start = System.nanoTime();
        final IterativeSearchOutcome outcome = search(iterative, board, options.depth, diagnostics);
        final long elapsed = Math.max(0L, System.nanoTime() - start);
        final SearchResult result = outcome.lastCompletedResult();
        if(result == null) throw new AssertionError("No completed benchmark result: " + position.name);
        return new RunRecord(position, result, outcome.diagnostics(), elapsed);
    }

    private static IterativeSearchOutcome search(
        IterativeDeepeningSearch search, long[] board, int depth, boolean diagnostics
    ) {
        final SearchControl control = SearchControl.controlled(
            -1L, System.nanoTime(), -1L, TimeSource.SYSTEM
        );
        return search.search(new SearchRequest(
            board, GameHistory.initial(board), depth, SearchObserver.NONE, control, diagnostics
        ));
    }

    private static void printRecord(
        int repetition, RunRecord record, Options options
    ) {
        final SearchResult result = record.result;
        final SearchDiagnosticsSnapshot diagnostics = record.diagnostics;
        final long nps = record.elapsedNanos == 0L
            ? 0L : saturatedMultiplyDivide(result.nodes(), 1_000_000_000L, record.elapsedNanos);
        System.out.println("result repetition=" + repetition
            + " corpus=" + record.position.name
            + " fen=\"" + record.position.fen + "\""
            + " requestedDepth=" + options.depth
            + " threads=" + THREADS
            + " diagnostics=" + diagnostics.enabled()
            + " ttPolicy=" + options.ttPolicy.name().toLowerCase(Locale.ROOT)
            + " ttEntries=" + TT_ENTRIES
            + " score=" + result.score()
            + " bestMove=" + (result.hasMove() ? Move.coordinate(result.bestMove()) : "0000")
            + " pv=\"" + formatPv(result.principalVariation()) + "\""
            + " completedDepth=" + result.depth()
            + " nodes=" + result.nodes()
            + " mainNodes=" + diagnostics.worker().nodes().mainNodes()
            + " qNodes=" + diagnostics.worker().nodes().qNodes()
            + " maxPly=" + diagnostics.worker().nodes().maximumAbsolutePly()
            + " maxQply=" + diagnostics.worker().nodes().maximumQply()
            + " elapsedNs=" + record.elapsedNanos
            + " nps=" + (record.elapsedNanos == 0L ? "unavailable" : Long.toString(nps))
            + " ttProbes=" + diagnostics.worker().transpositionTable().probes()
            + " ttKeyMatches=" + diagnostics.worker().transpositionTable().keyMatches()
            + " ttInsufficientDepth="
            + diagnostics.worker().transpositionTable().insufficientDepthMatches()
            + " ttExactCutoffs=" + diagnostics.worker().transpositionTable().exactCutoffs()
            + " ttLowerCutoffs=" + diagnostics.worker().transpositionTable().lowerBoundCutoffs()
            + " ttUpperCutoffs=" + diagnostics.worker().transpositionTable().upperBoundCutoffs()
            + " hashMovesAvailable="
            + diagnostics.worker().transpositionTable().hashMovesAvailable()
            + " ttStores=" + diagnostics.worker().transpositionTable().stores()
            + " searchedMoves=" + diagnostics.worker().moveOrder().legalMovesSearched()
            + " betaCutoffs=" + diagnostics.worker().moveOrder().betaCutoffs()
            + " firstMoveCutoffs=" + diagnostics.worker().moveOrder().firstMoveBetaCutoffs()
            + " cutoffRankSum=" + diagnostics.worker().moveOrder().cutoffRankSum()
            + " maxCutoffRank=" + diagnostics.worker().moveOrder().maximumCutoffRank()
            + " rank1=" + diagnostics.worker().moveOrder().cutoffRank1()
            + " rank2=" + diagnostics.worker().moveOrder().cutoffRank2()
            + " rank3=" + diagnostics.worker().moveOrder().cutoffRank3()
            + " rank4=" + diagnostics.worker().moveOrder().cutoffRank4()
            + " rank5to8=" + diagnostics.worker().moveOrder().cutoffRank5To8()
            + " rank9plus=" + diagnostics.worker().moveOrder().cutoffRank9Plus()
            + " hashCutoffs=" + diagnostics.worker().moveOrder().hashMoveCutoffs()
            + " tacticalCutoffs=" + diagnostics.worker().moveOrder().tacticalCutoffs()
            + " quietCutoffs=" + diagnostics.worker().moveOrder().quietCutoffs()
            + " killerCutoffs=" + diagnostics.worker().moveOrder().killerCutoffs()
            + " historyCutoffs=" + diagnostics.worker().moveOrder().historyCutoffs()
            + " checkedQNodes=" + diagnostics.worker().qsearch().checkedQNodes()
            + " standPatCutoffs=" + diagnostics.worker().qsearch().standPatCutoffs()
            + " qTacticalSearched=" + diagnostics.worker().qsearch().tacticalMovesSearched()
            + " qEvasionsSearched=" + diagnostics.worker().qsearch().evasionMovesSearched()
            + " softQdepthLimits=" + diagnostics.worker().qsearch().softDepthLimitEncounters()
            + " qmates=" + diagnostics.worker().qsearch().qmateTerminals()
            + " completedIterations=" + diagnostics.iteration().completedIterations()
            + " aspirationAttempts=" + diagnostics.iteration().aspirationAttempts()
            + " failLow=" + diagnostics.iteration().failLowResearches()
            + " failHigh=" + diagnostics.iteration().failHighResearches()
            + " fullWindowFallbacks=" + diagnostics.iteration().fullWindowFallbacks()
            + " deepestCompletedDepth=" + diagnostics.iteration().deepestCompletedDepth());
    }

    private static void requireRepeatable(RunRecord[] baseline, RunRecord actual) {
        if(baseline[0] == null) return;
        final RunRecord expected = baseline[0];
        if(!sameSemantics(expected.result, actual.result)
            || !expected.diagnostics.equals(actual.diagnostics)) {
            throw new AssertionError("Non-deterministic result/counters for " + actual.position.name);
        }
    }

    private static void requireDiagnosticIdentity(RunRecord disabled, RunRecord enabled) {
        if(!sameSemantics(disabled.result, enabled.result)) {
            throw new AssertionError("Diagnostics changed search semantics for " + enabled.position.name);
        }
    }

    private static boolean sameSemantics(SearchResult left, SearchResult right) {
        return left.bestMove() == right.bestMove()
            && left.hasMove() == right.hasMove()
            && left.score() == right.score()
            && left.depth() == right.depth()
            && left.nodes() == right.nodes()
            && left.legalRootMoves() == right.legalRootMoves()
            && left.completed() == right.completed()
            && Arrays.equals(left.principalVariation(), right.principalVariation());
    }

    private static String formatPv(long[] pv) {
        final StringBuilder value = new StringBuilder();
        for(long move : pv) {
            if(!value.isEmpty()) value.append(' ');
            value.append(Move.coordinate(move));
        }
        return value.toString();
    }

    private static long median(List<Long> values) {
        final long[] sorted = values.stream().mapToLong(Long::longValue).sorted().toArray();
        return sorted[sorted.length / 2];
    }

    private static long saturatedMultiplyDivide(long value, long multiplier, long divisor) {
        if(value == 0L) return 0L;
        if(value <= Long.MAX_VALUE / multiplier) return value * multiplier / divisor;
        return java.math.BigInteger.valueOf(value).multiply(java.math.BigInteger.valueOf(multiplier))
            .divide(java.math.BigInteger.valueOf(divisor))
            .min(java.math.BigInteger.valueOf(Long.MAX_VALUE)).longValue();
    }

    private static void printEnvironment(Options options) {
        System.out.println("benchmark name=seedv6-search-ws12 corpusVersion=1 threads=" + THREADS
            + " depth=" + options.depth + " warmups=" + options.warmups
            + " repetitions=" + options.repetitions + " ttPolicy="
            + options.ttPolicy.name().toLowerCase(Locale.ROOT));
        System.out.println("environment os=\"" + System.getProperty("os.name") + " "
            + System.getProperty("os.version") + "\" arch=" + System.getProperty("os.arch")
            + " java=\"" + System.getProperty("java.vendor") + " "
            + System.getProperty("java.version") + "\" vm=\""
            + System.getProperty("java.vm.name") + "\" availableProcessors="
            + Runtime.getRuntime().availableProcessors() + " maxHeapBytes="
            + Runtime.getRuntime().maxMemory() + " cpu=\""
            + System.getenv().getOrDefault("PROCESSOR_IDENTIFIER", "unavailable") + "\"");
        System.out.println("timing npsZeroDuration=unavailable assertionPolicy=wall-time-and-nps-excluded");
    }

    public record Position(String name, String fen) { }

    private record RunRecord(
        Position position,
        SearchResult result,
        SearchDiagnosticsSnapshot diagnostics,
        long elapsedNanos
    ) { }

    private enum DiagnosticMode { DISABLED, ENABLED, BOTH }
    private enum TtPolicy { COLD, WARM }

    private record Options(
        int depth,
        int warmups,
        int repetitions,
        DiagnosticMode diagnosticMode,
        TtPolicy ttPolicy
    ) {
        static Options parse(String[] args) {
            int depth = DEFAULT_DEPTH;
            int warmups = DEFAULT_WARMUPS;
            int repetitions = DEFAULT_REPETITIONS;
            DiagnosticMode mode = DiagnosticMode.BOTH;
            TtPolicy ttPolicy = TtPolicy.COLD;
            for(String argument : args) {
                if(argument.startsWith("--depth=")) depth = integer(argument, "--depth=");
                else if(argument.startsWith("--warmup=")) warmups = integer(argument, "--warmup=");
                else if(argument.startsWith("--repetitions=")) repetitions = integer(argument, "--repetitions=");
                else if(argument.startsWith("--diagnostics=")) {
                    mode = DiagnosticMode.valueOf(value(argument).toUpperCase(Locale.ROOT));
                } else if(argument.startsWith("--tt=")) {
                    ttPolicy = TtPolicy.valueOf(value(argument).toUpperCase(Locale.ROOT));
                } else {
                    throw new IllegalArgumentException("Unknown benchmark option: " + argument);
                }
            }
            if(depth < 1 || depth > AlphaBetaPvsSearch.MAX_SUPPORTED_DEPTH) {
                throw new IllegalArgumentException("Invalid depth: " + depth);
            }
            if(warmups < 0) throw new IllegalArgumentException("Warmup count must not be negative.");
            if(repetitions < 1) throw new IllegalArgumentException("Repetitions must be positive.");
            return new Options(depth, warmups, repetitions, mode, ttPolicy);
        }

        boolean includes(boolean diagnostics) {
            return diagnosticMode == DiagnosticMode.BOTH
                || diagnostics == (diagnosticMode == DiagnosticMode.ENABLED);
        }

        boolean[] modes(int repetition) {
            if(diagnosticMode == DiagnosticMode.DISABLED) return new boolean[] {false};
            if(diagnosticMode == DiagnosticMode.ENABLED) return new boolean[] {true};
            return repetition % 2 == 0
                ? new boolean[] {true, false} : new boolean[] {false, true};
        }

        private static int integer(String argument, String prefix) {
            return Integer.parseInt(argument.substring(prefix.length()));
        }

        private static String value(String argument) {
            return argument.substring(argument.indexOf('=') + 1);
        }
    }

    private static final List<Position> CORPUS = List.of(
        new Position("opening-start", "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"),
        new Position("middlegame-kiwipete", "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1"),
        new Position("quiet-endgame", "8/2p5/3p4/1P1Pp3/4P3/2K5/8/2k5 w - - 0 1"),
        new Position("tactical-queen", "4k3/8/8/3q4/4P3/8/8/4K3 w - - 0 1"),
        new Position("quiet-pawn", "4k3/8/8/8/8/8/3P4/4K3 w - - 0 1"),
        new Position("check-evasion", "4k3/8/8/8/8/8/4r3/4K3 w - - 0 1"),
        new Position("transposition-knights", "4k3/8/8/8/8/8/1N3N2/4K3 w - - 0 1"),
        new Position("qsearch-exchanges", "4k3/8/2p1p3/3q4/2P1P3/3Q4/8/4K3 w - - 0 1"),
        new Position("promotion-race", "4k3/P7/8/8/8/8/7p/4K3 w - - 0 1"),
        new Position("en-passant", "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1"),
        new Position("checkmate-terminal", "7k/6Q1/6K1/8/8/8/8/8 b - - 0 1"),
        new Position("stalemate-terminal", "7k/5Q2/6K1/8/8/8/8/8 b - - 0 1")
    );

    private SearchBenchmark() { }
}
