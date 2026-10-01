package com.ohinteractive.seedv6.tools.nnue;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.alphabeta.AlphaBetaPvsSearch;
import com.ohinteractive.seedv6.search.alphabeta.RootParallelSearch;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.iterative.IterativeDeepeningSearch;
import com.ohinteractive.seedv6.tools.eval.EvaluationCorpus;
import com.ohinteractive.seedv6.tools.search.SearchBenchmark;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.IntToDoubleFunction;

/**
 * Bounded, standalone engineering measurements, never a timing assertion in unit tests.
 * Example: nnuePerformanceBenchmark -PbenchmarkArgs="--network=path/network.nnue".
 * Create an explicitly untrained fixture once with --create-network=path/network.nnue.
 * Defaults: WS12 search corpus, depth 3, cold private TT, diagnostics off, 5 warmups,
 * 7 repetitions, threads 1/2/4. --mode=micro or --mode=search isolates either workload.
 * --diagnostics=true is a separate attribution run, not the performance gate.
 * --integer-probe=true adds prequantized arithmetic probes, not a runtime backend.
 * Kernel timings are focused workloads, not additive estimates of search time.
 */
public final class NnuePerformanceBenchmark {
    private static final int TT_ENTRIES = 1 << 18;
    private static final com.sun.management.ThreadMXBean ALLOCATION =
            ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean bean
                    && bean.isThreadAllocatedMemorySupported() ? bean : null;
    private static volatile double sink;
    private static volatile SearchEvaluation.State memoryState0, memoryState1;

    public static void main(String[] args) throws Exception {
        Options o = Options.parse(args);
        if (o.createNetwork != null) {
            if (Files.exists(o.createNetwork.toAbsolutePath().getParent().resolve(
                    com.ohinteractive.seedv6.training.checkpoint.CheckpointManifest.MANIFEST_FILE)))
                throw new IllegalArgumentException("Standalone fixtures cannot be created inside a published checkpoint.");
            Files.write(o.createNetwork, NnueNetworkCodec.encode(NnueNetwork.initialized(o.seed)),
                    StandardOpenOption.CREATE_NEW);
            System.out.println("createdUntrainedFixture=" + o.createNetwork + " seed=" + o.seed);
            return;
        }
        if (ALLOCATION != null) ALLOCATION.setThreadAllocatedMemoryEnabled(true);
        byte[] encoded = o.network == null ? NnueNetworkCodec.encode(NnueNetwork.initialized(o.seed))
                : com.ohinteractive.seedv6.training.checkpoint.CheckpointStore.readNetworkBytes(o.network);
        NnueNetwork network = NnueNetworkCodec.decode(encoded);
        System.out.println("benchmark=seedv6-nnue-performance-v1 backends=handcrafted,float-scalar-v1,float-four-v1 tanh=StrictMath"
                + " networkSha256=" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(encoded))
                + " networkSource=" + (o.network == null ? "untrained-seed-" + o.seed : o.network)
                + " depth=" + o.depth + " warmups=" + o.warmups + " repetitions=" + o.repetitions
                + " iterations=" + o.iterations + " threads=" + Arrays.toString(o.threads)
                + " scoreScale=" + o.scale + " diagnostics=" + o.diagnostics
                + " tt=cold-private ttEntries=" + TT_ENTRIES + " corpus=WS12-v1-12-positions"
                + " setupExcluded=true nnuePolicy=mate-only-full-window handcraftedPolicy=production");
        System.out.println("environment java=" + System.getProperty("java.runtime.version")
                + " vm=" + System.getProperty("java.vm.name") + " os=" + System.getProperty("os.name")
                + " arch=" + System.getProperty("os.arch") + " processors=" + Runtime.getRuntime().availableProcessors()
                + " maxHeap=" + Runtime.getRuntime().maxMemory()
                + " flags=" + ManagementFactory.getRuntimeMXBean().getInputArguments());
        System.out.println("memory networkFloatPayload=" + (4L * NnueNetwork.PARAMETER_COUNT)
                + " incrementalWorkerFloatPayload=265472 mainAndQsearchStates=516"
                + " parallelContexts=threads+1 headersAndSearchStorageExcluded=true");
        if (!o.mode.equals("search")) micro(network, o);
        if (!o.mode.equals("micro")) searches(network, o);
        System.out.println("benchmark status=PASS");
    }

    private static void micro(NnueNetwork network, Options o) {
        long[][] boards = EvaluationCorpus.entries().stream().map(EvaluationCorpus.Entry::board).toArray(long[][]::new);
        System.out.println("microCorpus=OPT5-all samples=" + boards.length + " sha256=" + EvaluationCorpus.rawSha256());
        NnueEvaluator evaluator = new NnueEvaluator(network), oracle = NnueEvaluator.scalarOracle(network);
        NnueAccumulator[] prepared = new NnueAccumulator[boards.length];
        double[] raw = new double[boards.length];
        double[] values = new double[boards.length];
        float[][] inputs = new float[boards.length][128];
        long fingerprint = 0;
        for (int i = 0; i < boards.length; i++) {
            prepared[i] = new NnueAccumulator(network);
            prepared[i].rebuild(boards[i]);
            raw[i] = evaluator.evaluate(boards[i], prepared[i]);
            values[i] = evaluator.boundedValue();
            if (Float.floatToIntBits((float) raw[i]) != Float.floatToIntBits(oracle.evaluate(boards[i], prepared[i]))
                    || Double.doubleToLongBits(values[i]) != Double.doubleToLongBits(oracle.boundedValue()))
                throw new AssertionError("Exact float inference differs from scalar oracle");
            fingerprint = Long.rotateLeft(fingerprint, 7) ^ Double.doubleToLongBits(values[i]);
            int player = Board.player((int) boards[i][Board.STATUS]);
            for (int j = 0; j < 64; j++) {
                inputs[i][j] = evaluator.accumulator(player, j);
                inputs[i][64 + j] = evaluator.accumulator(player ^ 1, j);
            }
        }
        System.out.println("numerical boundedValueFingerprint=" + Long.toUnsignedString(fingerprint)
                + " comparison=scalar-vs-four samples=" + boards.length
                + " meanAbsoluteError=0 median=0 p95=0 p99=0 max=0 signDisagreements=0 mappedScoreDifferences=0");
        measure("harness-floor", o, boards.length, i -> raw[i]);
        measure("handcrafted-evaluate", o, boards.length, i -> Eval.evaluate(boards[i]));
        measure("float-scalar-full", o, boards.length, i -> oracle.evaluate(boards[i]));
        measure("float-scalar-prepared", o, boards.length, i -> oracle.evaluate(boards[i], prepared[i]));
        measure("float-four-full", o, boards.length, i -> evaluator.evaluate(boards[i]));
        measure("float-four-prepared", o, boards.length, i -> evaluator.evaluate(boards[i], prepared[i]));
        float[] hb = new float[32], hw = new float[4096], ow = new float[32], hidden = new float[32];
        for (int j = 0; j < 32; j++) {
            hb[j] = network.hiddenBias(j);
            ow[j] = network.outputWeight(j);
            for (int i = 0; i < 128; i++) hw[j * 128 + i] = network.hiddenWeight(j, i);
        }
        measure("dense-scalar-oracle-including-hidden-clip-output", o, boards.length,
                i -> NnueMath.forward(hb, hw, network.outputBias(), ow, inputs[i], null, hidden));
        if (o.integerProbe) integerProbe(o, hw, inputs);
        measure("output-dot32", o, boards.length, i -> {
            float sum = network.outputBias();
            for (int j = 0; j < 32; j++) sum += ow[j] * inputs[i][j];
            return sum;
        });
        measure("strict-tanh-corpus", o, boards.length, i -> StrictMath.tanh(raw[i]));
        measure("strict-tanh-wide", o, 4096, i -> StrictMath.tanh((i - 2048) / 256.0));
        NnueScoreMapping mapping = new NnueScoreMapping(o.scale);
        measure("mapping-only", o, boards.length, i -> mapping.map(values[i]));
        float[] destination = new float[128];
        measure("copy128", o, boards.length, i -> {
            System.arraycopy(inputs[i], 0, destination, 0, 128);
            return destination[i & 127];
        });
        measure("clip128", o, boards.length, i -> {
            for (int j = 0; j < 128; j++) destination[j] = NnueNetwork.clip01(inputs[i][j]);
            return destination[i & 127];
        });
        measure("clip32", o, boards.length, i -> {
            for (int j = 0; j < 32; j++) destination[j] = NnueNetwork.clip01(inputs[i][j]);
            return destination[i & 31];
        });
        float[] weights = new float[NnueNetwork.FEATURE_WEIGHT_COUNT];
        for (int f = 0; f < NnueFeatureSchema.FEATURE_COUNT; f++)
            for (int j = 0; j < 64; j++) weights[f * 64 + j] = network.featureWeight(f, j);
        measure("row-add-sub-hot", o, 1, i -> rowPair(weights, 0, destination));
        measure("row-add-sub-scattered", o, 49152, i -> rowPair(weights, (i * 7919) % 49152, destination));
        transition(network, o, "ordinary", Board.FEN_STARTING_POSITION, "g1f3");
        transition(network, o, "capture", "4k3/8/8/3n4/4P3/8/8/4K3 w - - 0 1", "e4d5");
        transition(network, o, "king-refresh", "r3k2r/pppq1ppp/2npbn2/3Np3/2B1P3/2N2Q1P/PPP2PP1/R3K2R w KQkq - 4 12", "e1f1");
        SearchEvaluation definition = SearchEvaluation.incremental(network, mapping);
        // Warm class initialization first; retain the measured states so allocation cannot be elided.
        definition.newState(257);
        long beforeState = allocated();
        memoryState0 = definition.newState(257);
        memoryState1 = definition.newState(257);
        System.out.println("memory measuredMainAndQsearchStateAllocationBytes="
                + (ALLOCATION == null ? -1 : allocated() - beforeState)
                + " includesObjectsAndArrays=true excludesBoardSearchAndTT=true");
        SearchEvaluation.State main = definition.newState(2), qsearch = definition.newState(2);
        main.initialize(boards[0], 0);
        measure("float-prepared-hot", o, 1, i -> evaluator.evaluate(boards[0], prepared[0]));
        measure("state-dispatch-evaluate", o, 1, i -> main.evaluate(boards[0], 0));
        measure("qsearch-handoff", o, 1, i -> { qsearch.initializeFrom(boards[0], 0, main); return i; });
    }

    /**
     * Optional arithmetic feasibility probe only, NOT an inference backend. Inputs are
     * prequantized outside timing, so results exclude conversion, transformer, clipping,
     * output and search costs. No result from this experiment can select a play runtime.
     * Rounding is nearest/ties-even. |weight| <= 127, input in [0,32767], so every
     * partial dot has magnitude <= 128*127*32767 = 532660352, safely inside int32.
     */
    private static void integerProbe(Options o, float[] weights, float[][] inputs) {
        double maximum = 0;
        for (float weight : weights) maximum = Math.max(maximum, Math.abs(weight));
        double step = maximum == 0 ? 1.0 : maximum / 127;
        int[] ints = new int[4096];
        short[] shorts = new short[4096];
        byte[] bytes = new byte[4096];
        int[][] activations = new int[inputs.length][128];
        for (int i = 0; i < weights.length; i++) {
            ints[i] = (int) StrictMath.rint(weights[i] / step);
            shorts[i] = (short) ints[i];
            bytes[i] = (byte) ints[i];
        }
        for (int b = 0; b < inputs.length; b++)
            for (int i = 0; i < 128; i++) activations[b][i] = (int) StrictMath.rint(inputs[b][i] * 32767);
        System.out.println("integerProbe=arithmetic-only-not-runtime weightStep=" + step
                + " rounding=nearest-ties-even activationScale=32767 maxDotMagnitude=532660352");
        measure("integer-probe-int-storage-dot-only", o, inputs.length, b -> {
            long checksum = 0;
            for (int u = 0; u < 32; u++) {
                int dot = 0;
                for (int i = 0; i < 128; i++) dot += ints[u * 128 + i] * activations[b][i];
                checksum += dot;
            }
            return checksum;
        });
        measure("integer-probe-short-storage-dot-only", o, inputs.length, b -> {
            long checksum = 0;
            for (int u = 0; u < 32; u++) {
                int dot = 0;
                for (int i = 0; i < 128; i++) dot += shorts[u * 128 + i] * activations[b][i];
                checksum += dot;
            }
            return checksum;
        });
        measure("integer-probe-byte-storage-dot-only", o, inputs.length, b -> {
            long checksum = 0;
            for (int u = 0; u < 32; u++) {
                int dot = 0;
                for (int i = 0; i < 128; i++) dot += bytes[u * 128 + i] * activations[b][i];
                checksum += dot;
            }
            return checksum;
        });
    }

    private static double rowPair(float[] weights, int row, float[] accumulator) {
        NnueMath.addFeature(weights, row, accumulator);
        int offset = row * 64;
        for (int j = 0; j < 64; j++) accumulator[j] -= weights[offset + j];
        return accumulator[row & 63];
    }

    private static void transition(NnueNetwork network, Options o, String name, String fen, String coordinate) {
        long[] board = Board.fromFen(fen), child = play(board, findMove(board, coordinate));
        NnueAccumulator parent = new NnueAccumulator(network), next = new NnueAccumulator(network);
        parent.rebuild(board);
        measure("transition-" + name, o, 1, i -> { next.update(board, child, parent); return next.raw(0, 0); });
    }

    private static void measure(String name, Options o, int size, IntToDoubleFunction operation) {
        for (int w = 0; w < o.warmups; w++) loop(operation, size, o.iterations);
        long[] ns = new long[o.repetitions], bytes = new long[o.repetitions];
        long gc = gcCount();
        for (int r = 0; r < o.repetitions; r++) {
            long before = allocated();
            long start = System.nanoTime();
            loop(operation, size, o.iterations);
            ns[r] = System.nanoTime() - start;
            bytes[r] = allocated() - before;
        }
        System.out.printf(Locale.ROOT, "micro name=%s medianNsPerOp=%.3f min=%.3f max=%.3f opsPerSecond=%.0f allocatedBytesPerOp=%.3f gc=%d%n",
                name, median(ns) / (double) o.iterations, minimum(ns) / (double) o.iterations,
                maximum(ns) / (double) o.iterations, o.iterations * 1e9 / median(ns),
                ALLOCATION == null ? -1.0 : median(bytes) / (double) o.iterations, gcCount() - gc);
    }

    private static void loop(IntToDoubleFunction operation, int size, int count) {
        double sum = 0;
        int index = 0;
        for (int i = 0; i < count; i++) {
            sum += operation.applyAsDouble(index);
            if (++index == size) index = 0;
        }
        sink = sum;
    }

    private static void searches(NnueNetwork network, Options o) {
        SearchEvaluation[] definitions = {SearchEvaluation.handcrafted(),
                SearchEvaluation.incrementalScalarOracle(network, new NnueScoreMapping(o.scale)),
                SearchEvaluation.incremental(network, new NnueScoreMapping(o.scale))};
        String[] names = {"handcrafted", "float-scalar-v1", "float-four-v1"};
        for (int threads : o.threads) {
            SearchResult[][] expected = new SearchResult[3][SearchBenchmark.corpus().size()];
            long[][] times = new long[3][o.repetitions], nodes = new long[3][o.repetitions], bytes = new long[3][o.repetitions];
            long[][] collections = new long[3][o.repetitions];
            int comparisons = 0, pvDifferences = 0, nodeDifferences = 0;
            for (int r = -o.warmups; r < o.repetitions; r++) {
                SearchResult[][] current = new SearchResult[3][SearchBenchmark.corpus().size()];
                for (int turn = 0; turn < 3; turn++) {
                    int backend = Math.floorMod(r + turn, 3); // Rotate order; setup/printing excluded.
                    for (int p = 0; p < SearchBenchmark.corpus().size(); p++) {
                        SearchBenchmark.Position position = SearchBenchmark.corpus().get(p);
                        long[] board = Board.fromFen(position.fen());
                        SingleDepthSearch exact = threads == 1 ? new AlphaBetaPvsSearch(definitions[backend], TT_ENTRIES)
                                : new RootParallelSearch(threads, definitions[backend], TT_ENTRIES);
                        try (IterativeDeepeningSearch search = new IterativeDeepeningSearch(exact)) {
                            SearchRequest request = new SearchRequest(board, GameHistory.initial(board), o.depth,
                                    SearchObserver.NONE, SearchControl.controlled(-1, System.nanoTime(), -1, TimeSource.SYSTEM),
                                    o.diagnostics);
                            long gc = gcCount(), before = allocated(), start = System.nanoTime();
                            var outcome = search.search(request);
                            long elapsed = System.nanoTime() - start, allocated = allocated() - before;
                            long gcDelta = gcCount() - gc;
                            SearchResult result = outcome.lastCompletedResult();
                            if (!outcome.targetDepthCompleted() || result == null || !result.completed())
                                throw new AssertionError("Incomplete benchmark search");
                            requireLegalPv(board, result);
                            if (r < 0) continue;
                            current[backend][p] = result;
                            SearchResult prior = expected[backend][p];
                            if (prior == null) expected[backend][p] = result;
                            else if (threads == 1 ? !prior.equals(result) : !sameRoot(prior, result))
                                throw new AssertionError("Non-repeatable search " + position.name());
                            times[backend][r] += elapsed;
                            nodes[backend][r] += result.nodes();
                            bytes[backend][r] += allocated;
                            collections[backend][r] += gcDelta;
                            if (r == 0) System.out.println("result backend=" + names[backend] + " threads=" + threads
                                    + " position=" + position.name() + " score=" + result.score()
                                    + " move=" + (result.hasMove() ? Move.coordinate(result.bestMove()) : "0000")
                                    + " pv=" + pv(result) + " nodes=" + result.nodes()
                                    + " qnodes=" + result.diagnostics().worker().nodes().qNodes()
                                    + " terminal=" + !result.hasMove() + " legalPv=true");
                        }
                    }
                }
                if (r >= 0) {
                    for (int p = 0; p < SearchBenchmark.corpus().size(); p++) {
                        SearchResult scalar = current[1][p], four = current[2][p];
                        if (threads == 1 ? !scalar.equals(four) : !sameRoot(scalar, four))
                            throw new AssertionError("Exact optimization changed search result");
                        comparisons++;
                        if (!Arrays.equals(scalar.principalVariation(), four.principalVariation())) pvDifferences++;
                        if (scalar.nodes() != four.nodes()) nodeDifferences++;
                    }
                    for (int b = 0; b < 3; b++) System.out.println("sample backend=" + names[b]
                            + " threads=" + threads + " repetition=" + r + " ns=" + times[b][r] + " nodes=" + nodes[b][r]);
                }
            }
            System.out.println("equivalence threads=" + threads + " comparisons=" + comparisons
                    + " rootScoreMoveTerminalDifferences=0 pvDifferences=" + pvDifferences + " nodeDifferences=" + nodeDifferences);
            for (int b = 0; b < 3; b++) System.out.printf(Locale.ROOT,
                    "search backend=%s threads=%d medianMs=%.3f minMs=%.3f maxMs=%.3f medianNodes=%d nps=%.0f medianAllocatedBytes=%d timedGc=%d%n",
                    names[b], threads, median(times[b]) / 1e6, minimum(times[b]) / 1e6, maximum(times[b]) / 1e6,
                    median(nodes[b]), median(nodes[b]) * 1e9 / median(times[b]), ALLOCATION == null ? -1 : median(bytes[b]),
                    Arrays.stream(collections[b]).sum());
        }
    }

    private static boolean sameRoot(SearchResult a, SearchResult b) {
        return a.bestMove() == b.bestMove() && a.hasMove() == b.hasMove() && a.score() == b.score()
                && a.depth() == b.depth() && a.legalRootMoves() == b.legalRootMoves() && a.completed() == b.completed();
    }

    static void requireLegalPv(long[] board, SearchResult result) {
        if (result.hasMove() && result.principalVariationLength() == 0) throw new AssertionError("Missing PV");
        for (long move : result.principalVariation()) {
            long[] moves = new long[256];
            int count = generate(board, moves);
            boolean legal = false;
            for (int i = 0; i < count; i++) if (moves[i] == move) legal = true;
            if (!legal) throw new AssertionError("Illegal PV move " + Move.coordinate(move));
            board = play(board, move);
        }
    }

    private static String pv(SearchResult result) {
        return Arrays.stream(result.principalVariation()).mapToObj(Move::coordinate).reduce((a, b) -> a + "," + b).orElse("-");
    }

    private static long findMove(long[] board, String coordinate) {
        long[] moves = new long[256];
        int count = generate(board, moves);
        for (int i = 0; i < count; i++) if (Move.coordinate(moves[i]).equals(coordinate)) return moves[i];
        throw new IllegalArgumentException("Not a legal move: " + coordinate);
    }

    private static int generate(long[] b, long[] moves) {
        return Gen.genAll(b[0], b[1], b[2], b[3], (int) b[4], b[5], true, moves, new long[6]);
    }

    private static long[] play(long[] b, long move) {
        long[] child = new long[6];
        Board.makeMoveInto(b[0], b[1], b[2], b[3], (int) b[4], b[5], move, child);
        return child;
    }

    private static long allocated() { return ALLOCATION == null ? 0 : ALLOCATION.getTotalThreadAllocatedBytes(); }
    private static long gcCount() { return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(b -> Math.max(0, b.getCollectionCount())).sum(); }
    private static long median(long[] values) { long[] sorted = values.clone(); Arrays.sort(sorted); return sorted[sorted.length / 2]; }
    private static long minimum(long[] values) { return Arrays.stream(values).min().orElseThrow(); }
    private static long maximum(long[] values) { return Arrays.stream(values).max().orElseThrow(); }

    record Options(Path network, Path createNetwork, long seed, double scale, String mode, int depth,
                   int warmups, int repetitions, int iterations, int[] threads, boolean diagnostics, boolean integerProbe) {
        static Options parse(String[] args) {
            Path network = null, create = null;
            long seed = 73;
            double scale = NnueScoreMapping.V1.scale();
            String mode = "all";
            int depth = 3, warmups = 5, repetitions = 7, iterations = 100_000;
            int[] threads = {1, 2, 4};
            boolean diagnostics = false;
            boolean integerProbe = false;
            for (String arg : args) {
                int equals = arg.indexOf('=');
                if (equals < 0) throw new IllegalArgumentException("Expected --option=value");
                String key = arg.substring(0, equals), value = arg.substring(equals + 1);
                switch (key) {
                    case "--network" -> network = Path.of(value);
                    case "--create-network" -> create = Path.of(value);
                    case "--seed" -> seed = Long.parseLong(value);
                    case "--scale" -> scale = Double.parseDouble(value);
                    case "--mode" -> mode = value;
                    case "--depth" -> depth = Integer.parseInt(value);
                    case "--warmups" -> warmups = Integer.parseInt(value);
                    case "--repetitions" -> repetitions = Integer.parseInt(value);
                    case "--iterations" -> iterations = Integer.parseInt(value);
                    case "--threads" -> threads = Arrays.stream(value.split(",")).mapToInt(Integer::parseInt).toArray();
                    case "--diagnostics" -> {
                        if (!value.equals("true") && !value.equals("false")) throw new IllegalArgumentException("Invalid diagnostics");
                        diagnostics = Boolean.parseBoolean(value);
                    }
                    case "--integer-probe" -> {
                        if (!value.equals("true") && !value.equals("false")) throw new IllegalArgumentException("Invalid integer-probe");
                        integerProbe = Boolean.parseBoolean(value);
                    }
                    default -> throw new IllegalArgumentException("Unknown option " + key);
                }
            }
            if (network != null && create != null || !Set.of("all", "micro", "search").contains(mode)
                    || depth < 1 || depth > AlphaBetaPvsSearch.MAX_SUPPORTED_DEPTH || warmups < 0
                    || repetitions < 1 || iterations < 1 || threads.length == 0
                    || Arrays.stream(threads).anyMatch(t -> t < 1 || t > RootParallelSearch.MAX_WORKERS))
                throw new IllegalArgumentException("Invalid benchmark options");
            new NnueScoreMapping(scale);
            return new Options(network, create, seed, scale, mode, depth, warmups, repetitions, iterations, threads, diagnostics, integerProbe);
        }
    }
}
