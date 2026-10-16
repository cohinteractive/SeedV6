package com.ohinteractive.seedv6.tools.search;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn2.*;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.alphabeta.AlphaBetaPvsSearch;
import com.ohinteractive.seedv6.search.quiescence.QuiescenceSearch;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.function.IntToDoubleFunction;
import java.util.function.Supplier;

/** Evaluator-only probe of explicit checkpoints. No training, store writer or search.
 * Args: exact BRN-2 checkpoint, exact NNUE checkpoint, evaluations/round, measured rounds.
 * All operations visit the same prebuilt legal children; update operations start from
 * prebuilt root caches (one-ply siblings, not long paths or the periodic rebuild).
 */
public final class Brn2InferenceBenchmark {
    private static volatile double sink;
    private static volatile Object memorySink;

    record Workload(List<String> names, List<IntToDoubleFunction> operations, int size) {}

    static Workload workload(Brn2Model brn, NnueNetwork nnue) {
        List<long[]> parents = new ArrayList<>(), children = new ArrayList<>();
        List<Brn2Accumulator> brnParents = new ArrayList<>(), brnChildren = new ArrayList<>();
        List<NnueAccumulator> nnueParents = new ArrayList<>(), nnueChildren = new ArrayList<>();
        for (var position : Brn2DiagnosticCorpus.POSITIONS) {
            long[] parent = Board.fromFen(position.fen());
            var bp = new Brn2Accumulator(brn); bp.rebuild(parent);
            var np = new NnueAccumulator(nnue); np.rebuild(parent);
            for (long move : Brn2Diagnostics.legalMoves(parent)) {
                long[] child = Brn2Diagnostics.play(parent, move);
                var bc = new Brn2Accumulator(brn); bc.rebuild(child);
                var nc = new NnueAccumulator(nnue); nc.rebuild(child);
                parents.add(parent); children.add(child);
                brnParents.add(bp); brnChildren.add(bc);
                nnueParents.add(np); nnueChildren.add(nc);
            }
        }
        var workspace = new Brn2Workspace();
        var brnScratch = new Brn2Accumulator(brn);
        var nnueScratch = new NnueAccumulator(nnue);
        var nnueEvaluator = new NnueEvaluator(nnue);
        return new Workload(List.of("brn-full", "brn-cached-forward", "brn-update-forward",
                "brn-dual-rebuild-forward", "nnue-full", "nnue-cached-forward", "nnue-update-forward"), List.of(
                i -> brn.evaluate(children.get(i), workspace),
                i -> brnChildren.get(i).evaluate(children.get(i)),
                i -> {
                    brnScratch.update(parents.get(i), children.get(i), brnParents.get(i));
                    return brnScratch.evaluate(children.get(i));
                },
                i -> { brnScratch.rebuild(children.get(i)); return brnScratch.evaluate(children.get(i)); },
                i -> { nnueEvaluator.evaluate(children.get(i)); return nnueEvaluator.boundedValue(); },
                i -> {
                    nnueEvaluator.evaluate(children.get(i), nnueChildren.get(i));
                    return nnueEvaluator.boundedValue();
                },
                i -> {
                    nnueScratch.update(parents.get(i), children.get(i), nnueParents.get(i));
                    nnueEvaluator.evaluate(children.get(i), nnueScratch);
                    return nnueEvaluator.boundedValue();
                }), children.size());
    }

    private static double repeat(IntToDoubleFunction op, int count, int size) {
        double sum = 0;
        for (int i = 0; i < count; i++) sum += op.applyAsDouble(i % size);
        sink = sum;
        return sum;
    }

    static double[] verify(Workload workload) {
        double[] maximumError = new double[2];
        for (int i = 0; i < workload.size(); i++) {
            double full = workload.operations().get(0).applyAsDouble(i);
            for (int op = 1; op < 4; op++) maximumError[0] = Math.max(maximumError[0],
                    Math.abs(full - workload.operations().get(op).applyAsDouble(i)));
            double nnue = workload.operations().get(4).applyAsDouble(i);
            for (int op = 5; op < 7; op++) maximumError[1] = Math.max(maximumError[1],
                    Math.abs(nnue - workload.operations().get(op).applyAsDouble(i)));
        }
        if (!(maximumError[0] <= 1e-12 && maximumError[1] <= 1e-6))
            throw new IllegalStateException("Full/incremental mismatch: " + Arrays.toString(maximumError));
        return maximumError;
    }

    private static long allocated(com.sun.management.ThreadMXBean bean, Supplier<Object> constructor) {
        memorySink = constructor.get(); // Load/initialize constructors before measurement.
        long before = bean.getThreadAllocatedBytes(Thread.currentThread().threadId());
        memorySink = constructor.get();
        return bean.getThreadAllocatedBytes(Thread.currentThread().threadId()) - before;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("Expected BRN checkpoint, NNUE checkpoint, count, rounds.");
        int count = Integer.parseInt(args[2]), rounds = Integer.parseInt(args[3]);
        if (count < 1 || count > 1_000_000 || rounds < 1 || rounds > 10)
            throw new IllegalArgumentException("Use 1..1000000 evaluations and 1..10 rounds.");
        var brn = Brn2Diagnostics.load(args[0], TrainingArchitecture.BRN2);
        var nnue = Brn2Diagnostics.load(args[1], TrainingArchitecture.NNUE);
        var model = ((NetworkModel.Brn2) brn.model()).model();
        if (!model.boundedIntermediates()) throw new IllegalArgumentException("Checkpoint uses full fallback, not incremental production inference.");
        var workload = workload(model, ((NetworkModel.Nnue) nnue.model()).network());
        double[] errors = verify(workload);
        var out = new PrintWriter(System.out, true);
        var alloc = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!alloc.isThreadAllocatedMemorySupported()) throw new IllegalStateException("Allocation measurement unavailable.");
        alloc.setThreadAllocatedMemoryEnabled(true);
        long thread = Thread.currentThread().threadId();
        DiagnosticReport.write(out, "type", "micro_run", "brn2", brn.identity(), "nnue", nnue.identity(),
                "corpus", Brn2DiagnosticCorpus.ID, "children", workload.size(), "evaluationsPerRound", count,
                "warmupRounds", 2, "rounds", rounds, "java", System.getProperty("java.runtime.version"),
                "brnMaximumNormalizedError", errors[0], "nnueMaximumNormalizedError", errors[1],
                "jvmArgs", ManagementFactory.getRuntimeMXBean().getInputArguments());
        var definition = SearchEvaluation.brn2(model);
        DiagnosticReport.write(out, "type", "memory", "accumulatorAllocatedBytes",
                allocated(alloc, () -> new Brn2Accumulator(model)), "mainStateAllocatedBytes",
                allocated(alloc, () -> definition.newState(AlphaBetaPvsSearch.MAX_SUPPORTED_DEPTH + 1)),
                "qStateAllocatedBytes",
                allocated(alloc, () -> definition.newState(QuiescenceSearch.MAX_ABSOLUTE_PLY + 1)),
                "mainSlots", AlphaBetaPvsSearch.MAX_SUPPORTED_DEPTH + 1,
                "qSlots", QuiescenceSearch.MAX_ABSOLUTE_PLY + 1);
        memorySink = null;
        for (int r = -2; r < rounds; r++) for (int turn = 0; turn < workload.operations().size(); turn++) {
            int index = Math.floorMod(r + turn, workload.operations().size());
            long bytes = alloc.getThreadAllocatedBytes(thread), start = System.nanoTime();
            double checksum = repeat(workload.operations().get(index), count, workload.size());
            long nanos = System.nanoTime() - start;
            bytes = alloc.getThreadAllocatedBytes(thread) - bytes;
            DiagnosticReport.write(out, "type", "micro", "operation", workload.names().get(index),
                    "round", r, "warmup", r < 0, "count", count, "elapsedNs", nanos,
                    "nsPerEvaluation", nanos / (double) count, "evaluationsPerSecond", count * 1e9 / nanos,
                    "bytesPerEvaluation", bytes / (double) count, "checksum", checksum);
        }
    }

    private Brn2InferenceBenchmark() {}
}
