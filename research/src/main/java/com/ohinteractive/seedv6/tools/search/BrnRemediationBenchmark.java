package com.ohinteractive.seedv6.tools.search;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn.*;
import com.ohinteractive.seedv6.core.brn1.*;
import com.ohinteractive.seedv6.core.brn2.*;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.alphabeta.*;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.search.iterative.IterativeDeepeningSearch;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import java.util.function.IntToDoubleFunction;

/** Bounded depth-4 comparison. No timing assertions; cold private TT, identical neural search policy.
 * Args: micro|search, repetitions (default 3), threads (default 1), optional backend index (0..3).
 * Optional fifth argument: standalone BRN-2 model file (read only).
 * Search nodes include incomplete iterations and quiescence. Every position has a 10-second cap.
 */
public final class BrnRemediationBenchmark {
    private static final String[] NAMES = {"NNUE", "BRN-0", "BRN-1", "BRN-2"};
    private static final com.sun.management.ThreadMXBean ALLOC =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    private static volatile double sink;
    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "search";
        int rounds = args.length > 1 ? Integer.parseInt(args[1]) : 3;
        int threads = args.length > 2 ? Integer.parseInt(args[2]) : 1;
        int only = args.length > 3 ? Integer.parseInt(args[3]) : -1;
        var nnue = NnueNetwork.initialized(1); var brn = new BrnModel();
        var brn1 = new Brn1Model();
        final Brn2Model brn2;
        if (args.length > 4) {
            byte[] bytes = Files.readAllBytes(Path.of(args[4])); brn2 = Brn2Codec.decodeModel(bytes);
            System.out.println("brn2Model=" + args[4] + " sha256=" + HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes)));
        } else brn2 = new Brn2Model();
        SearchEvaluation[] defs = {SearchEvaluation.incremental(nnue), SearchEvaluation.brn(brn),
                SearchEvaluation.brn1(brn1), SearchEvaluation.brn2(brn2)};
        var corpus = new ArrayList<>(SearchBenchmark.corpus().subList(0, 3));
        if (!mode.equals("micro")) corpus.add(new SearchBenchmark.Position("opening-ruy-lopez",
                "r1bqkbnr/pppp1ppp/2n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3"));
        long[][] boards = corpus.stream().map(p -> Board.fromFen(p.fen())).toArray(long[][]::new);
        System.out.println("java=" + System.getProperty("java.runtime.version") + " mode=" + mode
                + " threads=" + threads + " models=deterministic-bootstrap depth=4 warmupRounds=1"
                + " positions=" + corpus.stream().map(SearchBenchmark.Position::name).toList() + " ttEntries=262144 timeCapSeconds=10"
                + " flags=" + ManagementFactory.getRuntimeMXBean().getInputArguments());
        if (mode.equals("micro")) {
            var ne = new NnueEvaluator(nnue); var bf = new BrnFeatures();
            var b1w = new Brn1Workspace(); var b2w = new Brn2Workspace();
            IntToDoubleFunction[] ops = {i -> ne.evaluate(boards[i]), i -> brn.evaluate(boards[i], bf),
                    i -> brn1.evaluate(boards[i], b1w), i -> brn2.evaluate(boards[i], b2w)};
            for (var op : ops) repeat(op, 600_000);
            for (int r = 0; r < rounds; r++) for (int turn = 0; turn < 4; turn++) {
                int b = (r + turn) % 4; if (only >= 0 && b != only) continue;
                long bytes = ALLOC.getThreadAllocatedBytes(Thread.currentThread().threadId()), start = System.nanoTime();
                repeat(ops[b], 300_000);
                long ns = System.nanoTime() - start;
                bytes = ALLOC.getThreadAllocatedBytes(Thread.currentThread().threadId()) - bytes;
                System.out.printf(Locale.ROOT, "micro backend=%s round=%d evalsPerSecond=%.0f nsPerEval=%.1f bytesPerEval=%.3f%n",
                        NAMES[b], r, 300_000e9 / ns, ns / 300_000.0, bytes / 300_000.0);
            }
            return;
        }
        for (int r = -1; r < rounds; r++) for (int turn = 0; turn < 4; turn++) {
            int b = Math.floorMod(r + turn, 4); if (only >= 0 && b != only) continue;
            long totalNs = 0, totalNodes = 0, totalBytes = 0;
            for (int p = 0; p < boards.length; p++) {
                var exact = threads == 1 ? new AlphaBetaPvsSearch(defs[b], 1 << 18)
                        : new RootParallelSearch(threads, defs[b], 1 << 18);
                try (var search = new IterativeDeepeningSearch(exact)) {
                    SearchObserver observer = SearchObserver.NONE;
                    var request = new SearchRequest(boards[p], GameHistory.initial(boards[p]), 4, observer,
                            SearchControl.controlled(-1, System.nanoTime(), 10_000_000_000L, TimeSource.SYSTEM), false);
                    long bytes = ALLOC.getTotalThreadAllocatedBytes(), start = System.nanoTime();
                    var outcome = search.search(request);
                    long ns = System.nanoTime() - start;
                    bytes = ALLOC.getTotalThreadAllocatedBytes() - bytes;
                    var result = outcome.lastCompletedResult();
                    totalNs += ns; totalNodes += request.control().nodes(); totalBytes += bytes;
                    System.out.printf(Locale.ROOT,
                            "position backend=%s round=%d name=%s depth=%d completed=%s nodes=%d ms=%.3f nps=%.0f bytes=%d score=%d move=%d%n",
                            NAMES[b], r, corpus.get(p).name(), result == null ? 0 : result.depth(), outcome.targetDepthCompleted(), request.control().nodes(), ns / 1e6,
                            request.control().nodes() * 1e9 / ns, bytes, result == null ? 0 : result.score(), result == null ? 0 : result.bestMove());
                }
            }
            System.out.printf(Locale.ROOT, "search backend=%s threads=%d round=%d nodes=%d ms=%.3f nps=%.0f bytes=%d%n",
                    NAMES[b], threads, r, totalNodes, totalNs / 1e6, totalNodes * 1e9 / totalNs, totalBytes);
        }
    }
    private static void repeat(IntToDoubleFunction op, int n) {
        double sum = 0; for (int i = 0; i < n; i++) sum += op.applyAsDouble(i % 3); sink = sum;
    }
}
