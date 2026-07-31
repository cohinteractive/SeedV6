package com.ohinteractive.seedv6.tools;

import java.util.Arrays;
import java.util.Locale;

import com.ohinteractive.seedv6.core.Board;

public final class PerftCompare {

    private static final int DEFAULT_WARMUP_ROUNDS = 15;
    private static final int DEFAULT_MEASURED_ROUNDS = 30;
    private static final double TIE_THRESHOLD = 0.01;

    private static final BenchmarkCase[] DEFAULT_CASES = {
        new BenchmarkCase(
            "start",
            "Start position",
            Board.FEN_STARTING_POSITION,
            4,
            4,
            197281L
        ),
        new BenchmarkCase(
            "kiwipete",
            "Kiwipete",
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
            3,
            4,
            97862L
        ),
        new BenchmarkCase(
            "special",
            "Promotion/check evasions",
            "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8",
            3,
            8,
            62379L
        )
    };

    public static void main(String[] args) {
        Config config = Config.parse(args);
        if(config.help) {
            printUsage();
            return;
        }

        BenchmarkCase[] cases = selectCases(config);
        Result[] results = new Result[cases.length];
        System.out.printf(
            Locale.ROOT,
            "Perft comparison: %d case(s), %d warmup rounds, %d measured rounds%n",
            cases.length,
            config.warmupRounds,
            config.measuredRounds
        );

        for(int i = 0; i < cases.length; i ++) {
            results[i] = benchmark(cases[i], config.warmupRounds, config.measuredRounds);
        }

        printReport(results);
    }

    private static Result benchmark(BenchmarkCase benchmarkCase, int warmupRounds, int measuredRounds) {
        long[] board = Board.fromFen(benchmarkCase.fen);
        long[] originalBoard = board.clone();
        Perft.Workspace magicWorkspace = new Perft.Workspace(benchmarkCase.depth);
        PerftPext.Workspace pextWorkspace = new PerftPext.Workspace(benchmarkCase.depth);

        long magicNodes = Perft.perftFlat(board, benchmarkCase.depth, magicWorkspace);
        verifyBoardUnchanged(benchmarkCase, board, originalBoard);
        long pextNodes = PerftPext.perftFlat(board, benchmarkCase.depth, pextWorkspace);
        verifyBoardUnchanged(benchmarkCase, board, originalBoard);
        verifyCounts(benchmarkCase, "preflight", magicNodes, pextNodes);

        long expectedChecksum = Math.multiplyExact(magicNodes, benchmarkCase.invocations);
        for(int round = 0; round < warmupRounds; round ++) {
            long magicChecksum;
            long pextChecksum;
            if((round & 1) == 0) {
                magicChecksum = runMagic(board, benchmarkCase.depth, benchmarkCase.invocations, magicWorkspace);
                pextChecksum = runPext(board, benchmarkCase.depth, benchmarkCase.invocations, pextWorkspace);
            } else {
                pextChecksum = runPext(board, benchmarkCase.depth, benchmarkCase.invocations, pextWorkspace);
                magicChecksum = runMagic(board, benchmarkCase.depth, benchmarkCase.invocations, magicWorkspace);
            }
            verifyRound(benchmarkCase, "warmup", round, expectedChecksum, magicChecksum, pextChecksum);
            verifyBoardUnchanged(benchmarkCase, board, originalBoard);
            consume(magicChecksum, pextChecksum);
            printProgress("Warmup", benchmarkCase.name, round, warmupRounds);
        }

        long[] magicTimes = new long[measuredRounds];
        long[] pextTimes = new long[measuredRounds];
        for(int round = 0; round < measuredRounds; round ++) {
            long magicChecksum;
            long pextChecksum;
            long start;
            if((round & 1) == 0) {
                start = System.nanoTime();
                magicChecksum = runMagic(board, benchmarkCase.depth, benchmarkCase.invocations, magicWorkspace);
                magicTimes[round] = System.nanoTime() - start;

                start = System.nanoTime();
                pextChecksum = runPext(board, benchmarkCase.depth, benchmarkCase.invocations, pextWorkspace);
                pextTimes[round] = System.nanoTime() - start;
            } else {
                start = System.nanoTime();
                pextChecksum = runPext(board, benchmarkCase.depth, benchmarkCase.invocations, pextWorkspace);
                pextTimes[round] = System.nanoTime() - start;

                start = System.nanoTime();
                magicChecksum = runMagic(board, benchmarkCase.depth, benchmarkCase.invocations, magicWorkspace);
                magicTimes[round] = System.nanoTime() - start;
            }
            verifyRound(benchmarkCase, "measurement", round, expectedChecksum, magicChecksum, pextChecksum);
            verifyBoardUnchanged(benchmarkCase, board, originalBoard);
            consume(magicChecksum, pextChecksum);
            printProgress("Measurement", benchmarkCase.name, round, measuredRounds);
        }

        return new Result(
            benchmarkCase,
            magicNodes,
            warmupRounds,
            measuredRounds,
            magicTimes,
            pextTimes
        );
    }

    private static long runMagic(long[] board, int depth, int invocations, Perft.Workspace workspace) {
        long checksum = 0L;
        for(int i = 0; i < invocations; i ++) {
            checksum += Perft.perftFlat(board, depth, workspace);
        }
        return checksum;
    }

    private static long runPext(long[] board, int depth, int invocations, PerftPext.Workspace workspace) {
        long checksum = 0L;
        for(int i = 0; i < invocations; i ++) {
            checksum += PerftPext.perftFlat(board, depth, workspace);
        }
        return checksum;
    }

    private static void verifyCounts(
        BenchmarkCase benchmarkCase,
        String phase,
        long magicNodes,
        long pextNodes
    ) {
        if(magicNodes != pextNodes || (benchmarkCase.expectedNodes > 0L && magicNodes != benchmarkCase.expectedNodes)) {
            throw new IllegalStateException(
                "Perft mismatch for " + benchmarkCase.name
                + " at depth " + benchmarkCase.depth
                + " during " + phase
                + ": Magic=" + magicNodes
                + ", PEXT=" + pextNodes
                + (benchmarkCase.expectedNodes > 0L ? ", expected=" + benchmarkCase.expectedNodes : "")
            );
        }
    }

    private static void verifyRound(
        BenchmarkCase benchmarkCase,
        String phase,
        int round,
        long expectedChecksum,
        long magicChecksum,
        long pextChecksum
    ) {
        if(magicChecksum != pextChecksum || magicChecksum != expectedChecksum) {
            throw new IllegalStateException(
                "Perft mismatch for " + benchmarkCase.name
                + " at depth " + benchmarkCase.depth
                + " during " + phase + " round " + (round + 1)
                + ": Magic=" + magicChecksum
                + ", PEXT=" + pextChecksum
                + ", expected=" + expectedChecksum
            );
        }
    }

    private static void verifyBoardUnchanged(BenchmarkCase benchmarkCase, long[] board, long[] originalBoard) {
        if(!Arrays.equals(board, originalBoard)) {
            throw new IllegalStateException(
                "Input board mutated for " + benchmarkCase.name + " at depth " + benchmarkCase.depth
            );
        }
    }

    private static void consume(long magicChecksum, long pextChecksum) {
        
    }

    private static void printProgress(String phase, String caseName, int round, int rounds) {
        int completed = round + 1;
        int interval = Math.max(1, rounds / 5);
        if(completed == 1 || completed == rounds || completed % interval == 0) {
            System.out.printf(
                Locale.ROOT,
                "%s %d/%d complete (%s)%n",
                phase,
                completed,
                rounds,
                caseName
            );
        }
    }

    private static void printReport(Result[] results) {
        System.out.println();
        System.out.println("Aggregate results");
        long overallMagicTime = 0L;
        long overallPextTime = 0L;
        long overallNodes = 0L;
        long overallInvocations = 0L;

        for(Result result : results) {
            long magicTotal = sum(result.magicTimes);
            long pextTotal = sum(result.pextTimes);
            long measuredInvocations = (long) result.measuredRounds * result.benchmarkCase.invocations;
            long measuredNodes = Math.multiplyExact(result.nodesPerInvocation, measuredInvocations);
            overallMagicTime += magicTotal;
            overallPextTime += pextTotal;
            overallNodes = Math.addExact(overallNodes, measuredNodes);
            overallInvocations += measuredInvocations;

            System.out.println();
            System.out.println(result.benchmarkCase.name);
            System.out.println("  Depth: " + result.benchmarkCase.depth);
            System.out.println("  Nodes per invocation: " + formatInteger(result.nodesPerInvocation));
            System.out.println("  Warmup rounds: " + result.warmupRounds);
            System.out.println("  Measured rounds: " + result.measuredRounds);
            System.out.println("  Invocations per round: " + result.benchmarkCase.invocations);
            System.out.println("  Total measured Magic time: " + formatDuration(magicTotal));
            System.out.println("  Total measured PEXT time: " + formatDuration(pextTotal));
            System.out.println("  Mean Magic time/invocation: " + formatDuration((double) magicTotal / measuredInvocations));
            System.out.println("  Mean PEXT time/invocation: " + formatDuration((double) pextTotal / measuredInvocations));
            System.out.println("  Median Magic time/invocation: " + formatDuration((double) median(result.magicTimes) / result.benchmarkCase.invocations));
            System.out.println("  Median PEXT time/invocation: " + formatDuration((double) median(result.pextTimes) / result.benchmarkCase.invocations));
            System.out.println("  Best/worst Magic round: " + formatDuration(min(result.magicTimes)) + " / " + formatDuration(max(result.magicTimes)));
            System.out.println("  Best/worst PEXT round: " + formatDuration(min(result.pextTimes)) + " / " + formatDuration(max(result.pextTimes)));
            System.out.printf(Locale.ROOT, "  Magic throughput: %.3f Mnps%n", mnps(measuredNodes, magicTotal));
            System.out.printf(Locale.ROOT, "  PEXT throughput: %.3f Mnps%n", mnps(measuredNodes, pextTotal));
            printComparison("  ", magicTotal, pextTotal);
        }

        System.out.println();
        System.out.println("Overall (weighted by total measured nodes)");
        System.out.println("  Total measured nodes per implementation: " + formatInteger(overallNodes));
        System.out.println("  Total measured invocations per implementation: " + formatInteger(overallInvocations));
        System.out.println("  Total measured Magic time: " + formatDuration(overallMagicTime));
        System.out.println("  Total measured PEXT time: " + formatDuration(overallPextTime));
        System.out.printf(Locale.ROOT, "  Magic throughput: %.3f Mnps%n", mnps(overallNodes, overallMagicTime));
        System.out.printf(Locale.ROOT, "  PEXT throughput: %.3f Mnps%n", mnps(overallNodes, overallPextTime));
        printComparison("  ", overallMagicTime, overallPextTime);
    }

    private static void printComparison(String indent, long magicTime, long pextTime) {
        long absoluteDifference = Math.abs(magicTime - pextTime);
        double percentageDifference = magicTime == 0L
            ? 0.0
            : ((double) pextTime - magicTime) * 100.0 / magicTime;
        double speedRatio = pextTime == 0L ? Double.POSITIVE_INFINITY : (double) magicTime / pextTime;
        double relativeGap = Math.max(magicTime, pextTime) == 0L
            ? 0.0
            : (double) absoluteDifference / Math.max(magicTime, pextTime);
        String faster = relativeGap < TIE_THRESHOLD
            ? "Effectively tied (<1%)"
            : magicTime < pextTime ? "Magic" : "PEXT";

        System.out.println(indent + "Absolute time difference: " + formatDuration(absoluteDifference));
        System.out.printf(Locale.ROOT, "%sPEXT time vs Magic: %+.3f%%%n", indent, percentageDifference);
        System.out.printf(Locale.ROOT, "%sSpeed ratio (PEXT/Magic throughput): %.3fx%n", indent, speedRatio);
        System.out.println(indent + "Faster implementation: " + faster);
    }

    private static double mnps(long nodes, long elapsedNs) {
        return elapsedNs == 0L ? 0.0 : (double) nodes * 1_000.0 / elapsedNs;
    }

    private static long sum(long[] values) {
        long total = 0L;
        for(long value : values) total += value;
        return total;
    }

    private static long min(long[] values) {
        long result = Long.MAX_VALUE;
        for(long value : values) result = Math.min(result, value);
        return result;
    }

    private static long max(long[] values) {
        long result = Long.MIN_VALUE;
        for(long value : values) result = Math.max(result, value);
        return result;
    }

    private static long median(long[] values) {
        long[] sorted = Arrays.copyOf(values, values.length);
        Arrays.sort(sorted);
        int middle = sorted.length >>> 1;
        if((sorted.length & 1) != 0) return sorted[middle];
        return sorted[middle - 1] + (sorted[middle] - sorted[middle - 1]) / 2L;
    }

    private static String formatInteger(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    private static String formatDuration(long nanoseconds) {
        return formatDuration((double) nanoseconds);
    }

    private static String formatDuration(double nanoseconds) {
        if(nanoseconds >= 1_000_000_000.0) {
            return String.format(Locale.ROOT, "%.3f s", nanoseconds / 1_000_000_000.0);
        }
        if(nanoseconds >= 1_000_000.0) {
            return String.format(Locale.ROOT, "%.3f ms", nanoseconds / 1_000_000.0);
        }
        if(nanoseconds >= 1_000.0) {
            return String.format(Locale.ROOT, "%.3f us", nanoseconds / 1_000.0);
        }
        return String.format(Locale.ROOT, "%.1f ns", nanoseconds);
    }

    private static BenchmarkCase[] selectCases(Config config) {
        if(config.caseName.equals("all")) {
            BenchmarkCase[] selected = new BenchmarkCase[DEFAULT_CASES.length];
            for(int i = 0; i < DEFAULT_CASES.length; i ++) {
                selected[i] = DEFAULT_CASES[i].withOverrides(config.depth, config.invocations);
            }
            return selected;
        }
        for(BenchmarkCase benchmarkCase : DEFAULT_CASES) {
            if(benchmarkCase.id.equals(config.caseName)) {
                return new BenchmarkCase[] {
                    benchmarkCase.withOverrides(config.depth, config.invocations)
                };
            }
        }
        throw new IllegalArgumentException(
            "Unknown case '" + config.caseName + "'. Expected all, start, kiwipete, or special."
        );
    }

    private static void printUsage() {
        System.out.println(
            "Usage: PerftCompare [--warmup=N] [--measure=N] "
            + "[--case=all|start|kiwipete|special] [--depth=N] [--invocations=N]"
        );
    }

    private record BenchmarkCase(
        String id,
        String name,
        String fen,
        int depth,
        int invocations,
        long expectedNodes
    ) {
        BenchmarkCase withOverrides(Integer depthOverride, Integer invocationOverride) {
            int selectedDepth = depthOverride == null ? depth : depthOverride;
            int selectedInvocations = invocationOverride == null ? invocations : invocationOverride;
            long selectedExpected = selectedDepth == depth ? expectedNodes : 0L;
            return new BenchmarkCase(id, name, fen, selectedDepth, selectedInvocations, selectedExpected);
        }
    }

    private record Result(
        BenchmarkCase benchmarkCase,
        long nodesPerInvocation,
        int warmupRounds,
        int measuredRounds,
        long[] magicTimes,
        long[] pextTimes
    ) {}

    private static final class Config {
        int warmupRounds = DEFAULT_WARMUP_ROUNDS;
        int measuredRounds = DEFAULT_MEASURED_ROUNDS;
        String caseName = "all";
        Integer depth;
        Integer invocations;
        boolean help;

        static Config parse(String[] args) {
            Config config = new Config();
            for(String arg : args) {
                if(arg.equals("--help") || arg.equals("-h")) {
                    config.help = true;
                } else if(arg.startsWith("--warmup=")) {
                    config.warmupRounds = parseInt(arg, "--warmup=");
                } else if(arg.startsWith("--measure=")) {
                    config.measuredRounds = parseInt(arg, "--measure=");
                } else if(arg.startsWith("--case=")) {
                    config.caseName = arg.substring("--case=".length()).toLowerCase(Locale.ROOT);
                } else if(arg.startsWith("--depth=")) {
                    config.depth = parseInt(arg, "--depth=");
                } else if(arg.startsWith("--invocations=")) {
                    config.invocations = parseInt(arg, "--invocations=");
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }
            if(config.warmupRounds < 0) throw new IllegalArgumentException("--warmup must be >= 0");
            if(config.measuredRounds <= 0) throw new IllegalArgumentException("--measure must be > 0");
            if(config.depth != null && (config.depth < 0 || config.depth > 64)) {
                throw new IllegalArgumentException("--depth must be between 0 and 64");
            }
            if(config.invocations != null && config.invocations <= 0) {
                throw new IllegalArgumentException("--invocations must be > 0");
            }
            return config;
        }

        private static int parseInt(String argument, String prefix) {
            try {
                return Integer.parseInt(argument.substring(prefix.length()));
            } catch(NumberFormatException e) {
                throw new IllegalArgumentException("Invalid integer argument: " + argument, e);
            }
        }
    }

    private PerftCompare() {}
}
