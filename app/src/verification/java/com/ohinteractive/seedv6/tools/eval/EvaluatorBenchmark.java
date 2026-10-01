package com.ohinteractive.seedv6.tools.eval;

import com.ohinteractive.seedv6.core.Eval;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Evaluator-only OPT5 benchmark over prebuilt {@code long[6]} boards.
 * Corpus loading, board construction, stratum selection, and reporting all
 * occur outside the timed loop.
 */
public final class EvaluatorBenchmark {

    private static final int DEFAULT_WARMUPS = 8;
    private static final int DEFAULT_REPETITIONS = 15;
    private static final int DEFAULT_EVALUATIONS = 250_000;
    private static final long CHECKSUM_SEED = 0x9e3779b97f4a7c15L;
    private static final long CHECKSUM_MULTIPLIER = 0xbf58476d1ce4e5b9L;
    private static final com.sun.management.ThreadMXBean ALLOCATION_BEAN =
            ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean bean
                    ? bean : null;

    public static void main(String[] args) {
        Options options = Options.parse(args);
        enableMeasurementSupport();
        EvaluationCorpus.Metadata metadata = EvaluationCorpus.metadata();
        List<Stratum> strata = strata();

        System.out.println("benchmark name=seedv6-evaluator-opt5-m1 baseline="
                + EvaluationCorpus.BASELINE_COMMIT
                + " corpusSha256=" + EvaluationCorpus.rawSha256()
                + " sampleCount=" + metadata.sampleCount()
                + " weightedSampleCount=" + metadata.weightedSampleCount()
                + " evaluationCalls=" + metadata.totalCalls()
                + " mainSearchCalls="
                + metadata.calls(EvaluationCorpus.Source.MAIN_SEARCH)
                + " qsearchCalls=" + metadata.calls(EvaluationCorpus.Source.QSEARCH)
                + " warmups=" + options.warmups
                + " repetitions=" + options.repetitions
                + " evaluationsPerRepetition=" + options.evaluations);
        System.out.println("environment os=\"" + System.getProperty("os.name") + " "
                + System.getProperty("os.version") + "\" arch="
                + System.getProperty("os.arch") + " java=\""
                + System.getProperty("java.vendor") + " "
                + System.getProperty("java.version") + "\" vm=\""
                + System.getProperty("java.vm.name") + "\" availableProcessors="
                + Runtime.getRuntime().availableProcessors() + " maxHeapBytes="
                + Runtime.getRuntime().maxMemory() + " allocationCounterSupported="
                + allocationCounterSupported());

        for (Stratum stratum : strata) {
            for (int warmup = 0; warmup < options.warmups; warmup++) {
                evaluateLoop(stratum.boards, options.evaluations);
            }
            primeAllocationMeasurement(stratum.boards);

            long[] elapsed = new long[options.repetitions];
            long[] allocated = new long[options.repetitions];
            long expectedChecksum = 0L;
            for (int repetition = 0; repetition < options.repetitions; repetition++) {
                long allocatedBefore = allocatedBytes();
                long start = System.nanoTime();
                long checksum = evaluateLoop(stratum.boards, options.evaluations);
                elapsed[repetition] = System.nanoTime() - start;
                allocated[repetition] = allocatedBytes() - allocatedBefore;
                if (repetition == 0) {
                    expectedChecksum = checksum;
                } else if (checksum != expectedChecksum) {
                    throw new AssertionError("Non-deterministic checksum for " + stratum.name);
                }
            }
            printResult(stratum, options, elapsed, allocated, expectedChecksum);
        }
        System.out.println("benchmark status=PASS timedLoop=eval-only-prebuilt-boards");
    }

    /** Deterministic benchmark checksum helper used by corpus integrity tests. */
    public static long checksum(long[][] boards, int evaluations) {
        if (boards.length == 0) {
            throw new IllegalArgumentException("At least one benchmark board is required");
        }
        if (evaluations <= 0) {
            throw new IllegalArgumentException("Evaluations must be positive");
        }
        for (long[] board : boards) {
            if (board.length != 6) {
                throw new IllegalArgumentException("Every benchmark board must have six longs");
            }
        }
        return evaluateLoop(boards, evaluations);
    }

    public static long[][] productionWeightedBoards() {
        return boards(EvaluationCorpus.weightedEntries());
    }

    private static long evaluateLoop(long[][] boards, int evaluations) {
        long checksum = CHECKSUM_SEED;
        int boardIndex = 0;
        for (int evaluation = 0; evaluation < evaluations; evaluation++) {
            int score = Eval.evaluate(boards[boardIndex]);
            checksum = Long.rotateLeft(checksum, 11)
                    ^ ((long) score * CHECKSUM_MULTIPLIER + boardIndex);
            boardIndex++;
            if (boardIndex == boards.length) {
                boardIndex = 0;
            }
        }
        return checksum;
    }

    private static List<Stratum> strata() {
        List<EvaluationCorpus.Entry> all = EvaluationCorpus.entries();
        return List.of(
                stratum("production-weighted", EvaluationCorpus.weightedEntries()),
                stratum("pawn-heavy", all,
                        entry -> entry.whitePawnCount() + entry.blackPawnCount() >= 12),
                stratum("slider-heavy", all, entry -> entry.sliderCount() >= 6),
                stratum("promoted-material", all, EvaluationCorpus.Entry::promotedMaterial),
                stratum("endgame", all, EvaluationCorpus.Entry::endgame));
    }

    private static Stratum stratum(String name, List<EvaluationCorpus.Entry> entries) {
        if (entries.isEmpty()) {
            throw new IllegalStateException("Empty evaluation benchmark stratum: " + name);
        }
        long[][] boards = boards(entries);
        return new Stratum(name, boards, distinctBoards(boards));
    }

    private static Stratum stratum(String name, List<EvaluationCorpus.Entry> entries,
                                   Predicate<EvaluationCorpus.Entry> predicate) {
        List<EvaluationCorpus.Entry> selected = new ArrayList<>();
        for (EvaluationCorpus.Entry entry : entries) {
            if (predicate.test(entry)) {
                selected.add(entry);
            }
        }
        return stratum(name, selected);
    }

    private static long[][] boards(List<EvaluationCorpus.Entry> entries) {
        long[][] boards = new long[entries.size()][];
        for (int index = 0; index < entries.size(); index++) {
            boards[index] = entries.get(index).board();
        }
        return boards;
    }

    private static int distinctBoards(long[][] boards) {
        Set<BoardState> distinct = new HashSet<>();
        for (long[] board : boards) {
            distinct.add(new BoardState(board[0], board[1], board[2], board[3],
                    board[4], board[5]));
        }
        return distinct.size();
    }

    private static void printResult(Stratum stratum, Options options, long[] elapsed,
                                    long[] allocated, long checksum) {
        long[] sorted = elapsed.clone();
        Arrays.sort(sorted);
        long median = sorted[sorted.length / 2];
        long min = sorted[0];
        long max = sorted[sorted.length - 1];
        long[] deviations = new long[sorted.length];
        for (int index = 0; index < sorted.length; index++) {
            deviations[index] = Math.abs(sorted[index] - median);
        }
        Arrays.sort(deviations);
        long mad = deviations[deviations.length / 2];
        long medianAllocated = median(allocated);
        long maximumAllocated = Arrays.stream(allocated).max().orElse(-1L);

        System.out.printf(Locale.ROOT,
                "result stratum=%s samples=%d distinctBoards=%d "
                        + "evaluationsPerRepetition=%d warmups=%d repetitions=%d "
                        + "medianNsPerEvaluation=%.3f minNsPerEvaluation=%.3f "
                        + "maxNsPerEvaluation=%.3f madNsPerEvaluation=%.3f "
                        + "medianAllocatedBytesPerRepetition=%d "
                        + "maxAllocatedBytesPerRepetition=%d "
                        + "checksum=%016x%n",
                stratum.name, stratum.boards.length, stratum.distinctBoards,
                options.evaluations, options.warmups, options.repetitions,
                (double) median / options.evaluations,
                (double) min / options.evaluations,
                (double) max / options.evaluations,
                (double) mad / options.evaluations,
                medianAllocated,
                maximumAllocated,
                checksum);
    }

    private static long median(long[] values) {
        long[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }

    private static void enableMeasurementSupport() {
        if (allocationCounterSupported()
                && !ALLOCATION_BEAN.isThreadAllocatedMemoryEnabled()) {
            ALLOCATION_BEAN.setThreadAllocatedMemoryEnabled(true);
        }
    }

    private static void primeAllocationMeasurement(long[][] boards) {
        long before = allocatedBytes();
        evaluateLoop(boards, 1);
        allocatedBytes();
        if (before < 0L) {
            throw new AssertionError("Negative thread allocation counter");
        }
    }

    private static long allocatedBytes() {
        return allocationCounterSupported()
                ? ALLOCATION_BEAN.getThreadAllocatedBytes(Thread.currentThread().threadId()) : 0L;
    }

    private static boolean allocationCounterSupported() {
        return ALLOCATION_BEAN != null && ALLOCATION_BEAN.isThreadAllocatedMemorySupported();
    }

    private record Stratum(String name, long[][] boards, int distinctBoards) { }

    private record BoardState(long board0, long board1, long board2, long board3,
                              long status, long key) { }

    private record Options(int warmups, int repetitions, int evaluations) {
        private static Options parse(String[] args) {
            int warmups = DEFAULT_WARMUPS;
            int repetitions = DEFAULT_REPETITIONS;
            int evaluations = DEFAULT_EVALUATIONS;
            for (String argument : args) {
                if (argument.startsWith("--warmups=")) {
                    warmups = parseNonNegative(argument, "--warmups=");
                } else if (argument.startsWith("--repetitions=")) {
                    repetitions = parsePositive(argument, "--repetitions=");
                } else if (argument.startsWith("--evaluations=")) {
                    evaluations = parsePositive(argument, "--evaluations=");
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + argument);
                }
            }
            return new Options(warmups, repetitions, evaluations);
        }

        private static int parseNonNegative(String argument, String prefix) {
            int value = Integer.parseInt(argument.substring(prefix.length()));
            if (value < 0) {
                throw new IllegalArgumentException(prefix + " must be non-negative");
            }
            return value;
        }

        private static int parsePositive(String argument, String prefix) {
            int value = Integer.parseInt(argument.substring(prefix.length()));
            if (value <= 0) {
                throw new IllegalArgumentException(prefix + " must be positive");
            }
            return value;
        }
    }

    private EvaluatorBenchmark() { }
}
