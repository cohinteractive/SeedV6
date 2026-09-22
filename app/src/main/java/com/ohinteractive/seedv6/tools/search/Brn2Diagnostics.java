package com.ohinteractive.seedv6.tools.search;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.ToDoubleFunction;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.brn2.*;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.alphabeta.AlphaBetaPvsSearch;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.search.iterative.*;
import com.ohinteractive.seedv6.search.order.MoveOrdering;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.model.*;

import static com.ohinteractive.seedv6.tools.search.DiagnosticReport.*;

/** Explicit-input, bounded evaluator/search characterization. No training service or store writer. */
public final class Brn2Diagnostics {
    static final int TT_ENTRIES = 1 << 18;

    public static void main(String[] args) throws Exception {
        Options options = Options.parse(args);
        Loaded brn = load(options.brn2, TrainingArchitecture.BRN2);
        Loaded nnue = options.nnue == null ? null : load(options.nnue, TrainingArchitecture.NNUE);
        PrintWriter output = options.output == null
                ? new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8))
                : new PrintWriter(Files.newBufferedWriter(options.output, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE));
        try {
            run(options, brn, nnue, output);
        } finally {
            if (options.output != null) output.close();
            else output.flush();
        }
    }

    record Loaded(NetworkModel model, Map<String, Object> identity) {}

    /** A directory is an exact checkpoint, not a mutable Best/latest reference. */
    static Loaded load(String source, TrainingArchitecture expected) throws Exception {
        NetworkModel model;
        Map<String, Object> identity;
        if (source.equals("initialized")) {
            model = expected == TrainingArchitecture.BRN2 ? new NetworkModel.Brn2(new Brn2Model())
                    : new NetworkModel.Nnue(NnueNetwork.initialized(1));
            identity = fields("source", "deterministic-initialization-not-trained",
                    "seed", expected == TrainingArchitecture.BRN2 ? Brn2Model.INITIALIZATION_SEED : 1);
        } else {
            Path path = Path.of(source).toAbsolutePath().normalize();
            if (Files.isDirectory(path)) {
                // Reject the wrong architecture before reading the large optimizer payload.
                if (CheckpointInspection.manifest(path).architecture() != expected)
                    throw new IOException("Expected " + expected + " checkpoint: " + path);
                var checkpoint = CheckpointStore.inspect(path);
                model = checkpoint.model();
                var manifest = checkpoint.manifest();
                identity = fields("source", path.toString(), "checkpointId", manifest.id(),
                        "generation", manifest.generation(), "optimizerStep", manifest.optimizerStep(),
                        "trainingSha256", manifest.trainingSha256());
            } else {
                byte[] bytes = CheckpointStore.readNetworkBytes(path);
                model = NetworkModel.read(expected, new ByteArrayInputStream(bytes));
                identity = fields("source", path.toString(), "inputSha256", sha256(bytes));
            }
        }
        if (model.architecture() != expected) throw new IOException("Wrong model architecture.");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        model.write(new java.security.DigestOutputStream(OutputStream.nullOutputStream(), digest));
        identity.put("architecture", expected.name());
        identity.put("modelSha256", HexFormat.of().formatHex(digest.digest()));
        identity.put("scoreScale", BrnScoreMapping.SCALE);
        return new Loaded(model, identity);
    }

    record Value(double raw, double normalized, int score, Integer nnueScore) {}
    record Observation(String id, Value value) {}
    record Delta(String id, String parent, String move, List<String> categories,
                 Value parentValue, Value childValue) {
        int signedScoreDelta() { return -childValue.score() - parentValue.score(); }
        int absoluteScoreDelta() { return Math.abs(signedScoreDelta()); }
        double absoluteValueDelta() { return Math.abs(-childValue.normalized() - parentValue.normalized()); }
    }
    record Measurements(List<Observation> roots, List<Observation> children, List<Delta> deltas) {}

    /** Owns scratch only; follows the actual production initialization/child path. */
    static final class Evaluator {
        private final Brn2Model model;
        private final Brn2Accumulator parent, child;
        private final Brn2Workspace workspace = new Brn2Workspace();
        private final SearchEvaluation.State nnue;
        Evaluator(Brn2Model model, NetworkModel reference) {
            this.model = model;
            parent = model.boundedIntermediates() ? new Brn2Accumulator(model) : null;
            child = model.boundedIntermediates() ? new Brn2Accumulator(model) : null;
            nnue = reference == null ? null : reference.evaluation(NnueScoreMapping.V1).newState(2);
        }
        Value root(long[] board) {
            if (parent != null) parent.rebuild(board);
            if (nnue != null) nnue.initialize(board, 0);
            return value(board, parent, 0);
        }
        Value child(long[] board, long[] next) {
            if (child != null) child.update(board, next, parent);
            if (nnue != null) nnue.child(board, next, 0);
            return value(next, child, 1);
        }
        private Value value(long[] board, Brn2Accumulator state, int ply) {
            double normalized = state == null ? model.evaluateReference(board, workspace) : state.evaluate(board);
            return new Value(state == null ? workspace.raw() : state.raw(), normalized,
                    BrnScoreMapping.map(normalized), nnue == null ? null : nnue.evaluate(board, ply));
        }
    }

    static Measurements measure(Brn2Model model, NetworkModel reference, List<SearchBenchmark.Position> corpus) {
        var evaluator = new Evaluator(model, reference);
        List<Observation> roots = new ArrayList<>(), children = new ArrayList<>();
        List<Delta> deltas = new ArrayList<>();
        for (var position : corpus) {
            long[] board = Board.fromFen(position.fen());
            Value parent = evaluator.root(board);
            roots.add(new Observation(position.name(), parent));
            for (long move : legalMoves(board)) {
                long[] next = play(board, move);
                if (Board.player((int) board[Board.STATUS]) == Board.player((int) next[Board.STATUS]))
                    throw new IllegalStateException("Child did not change side to move.");
                Value child = evaluator.child(board, next);
                String coordinate = Move.coordinate(move), id = position.name() + "/" + coordinate;
                children.add(new Observation(id, child));
                deltas.add(new Delta(id, position.name(), coordinate, categories(board, next, move), parent, child));
            }
        }
        return new Measurements(List.copyOf(roots), List.copyOf(children), List.copyOf(deltas));
    }

    static long[] legalMoves(long[] board) {
        long[] moves = new long[256], scratch = new long[256];
        int count = Gen.genAll(board[0], board[1], board[2], board[3], (int) board[4], board[5], true, moves, scratch);
        // Stable identity order independent of generator staging changes.
        return Arrays.stream(Arrays.copyOf(moves, count)).boxed()
                .sorted(Comparator.comparing(Move::coordinate)).mapToLong(Long::longValue).toArray();
    }

    static long[] play(long[] board, long move) {
        long[] next = new long[Board.MAX_BITBOARDS];
        Board.makeMoveInto(board[0], board[1], board[2], board[3], (int) board[4], board[5], move, next);
        return next;
    }

    static List<String> categories(long[] board, long[] child, long move) {
        // Production Gen emits legacy move fields, not GenMoveType's experimental type bits.
        int target = (int) (move >>> Board.TARGET_PIECE_SHIFT) & Board.PIECE_BITS;
        int piece = ((int) (move >>> Board.START_PIECE_SHIFT) & Board.PIECE_BITS) & Piece.TYPE;
        boolean promotion = ((move >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS) != 0;
        boolean enPassant = !promotion && target == 0 && MoveOrdering.isTactical(board, move);
        boolean capture = target != 0 || enPassant;
        boolean check = inCheck(child);
        List<String> result = new ArrayList<>();
        if (capture) result.add("capture");
        if (promotion) result.add("promotion");
        if (check) result.add("gives-check");
        if (inCheck(board)) result.add("evasion");
        if (piece == Piece.KING && Math.abs(Move.toSquare(move) - Move.fromSquare(move)) == 2) result.add("castle");
        if (enPassant) result.add("en-passant");
        if (!capture && !promotion) result.add("non-capture-non-promotion");
        if (!capture && !promotion && !check) result.add("quiet");
        return List.copyOf(result);
    }

    private static boolean inCheck(long[] board) {
        int player = Board.player((int) board[Board.STATUS]);
        long color = ~(-(long) player ^ board[3]);
        int king = Long.numberOfTrailingZeros(board[0] & ~board[1] & ~board[2] & color);
        return Board.getCheckersPext(board[0], board[1], board[2], board[3], color, player, king,
                board[0] | board[1] | board[2]) != 0;
    }

    record SearchMeasurement(String status, SearchResult result, SearchDiagnosticsSnapshot diagnostics,
                             long nodes, long elapsedNanos, String error) {}

    static SearchMeasurement search(long[] board, SearchEvaluation definition, int depth,
                                    long nodeLimit, long timeMs, boolean diagnostics) {
        long[] before = board.clone();
        try (var search = new IterativeDeepeningSearch(new AlphaBetaPvsSearch(definition, TT_ENTRIES))) {
            long start = System.nanoTime();
            var control = SearchControl.controlled(nodeLimit, start,
                    timeMs == -1 ? -1 : Math.multiplyExact(timeMs, 1_000_000L), TimeSource.SYSTEM);
            try {
                var outcome = search.search(new SearchRequest(board, GameHistory.initial(board), depth,
                        SearchObserver.NONE, control, diagnostics));
                long elapsed = System.nanoTime() - start;
                if (diagnostics && control.nodes() != outcome.diagnostics().totalEnteredNodes())
                    throw new IllegalStateException("Incomplete main/q node accounting.");
                String status = outcome.terminalRoot() ? "TERMINAL" : outcome.targetDepthCompleted() ? "COMPLETED"
                        : control.termination() == SearchTermination.NONE ? "INCOMPLETE" : control.termination().name();
                return new SearchMeasurement(status, outcome.lastCompletedResult(), outcome.diagnostics(),
                        control.nodes(), elapsed, null);
            } catch (RuntimeException failure) {
                // A failed exact attempt may not have published counters; do not present stale counts as complete.
                return new SearchMeasurement("FAILURE", search.lastCompletedResult(), null, control.nodes(),
                        System.nanoTime() - start, failure.toString());
            } finally {
                if (!Arrays.equals(before, board)) throw new IllegalStateException("Search changed input board.");
            }
        }
    }

    static Double qRatio(long main, long q) {
        if (main < 0 || q < 0) throw new IllegalArgumentException("Negative node counts.");
        return main == 0 && q == 0 ? null : q / ((double) main + q);
    }

    private static void run(Options options, Loaded brn, Loaded nnue, PrintWriter out) throws Exception {
        var corpus = Brn2DiagnosticCorpus.POSITIONS;
        String corpusText = String.join("\n", corpus.stream().map(p -> p.name() + "\t" + p.fen()).toList()) + "\n";
        write(out, "type", "run", "schema", 1, "label", options.label,
                "corpus", Brn2DiagnosticCorpus.ID, "corpusSha256", sha256(corpusText.getBytes(StandardCharsets.UTF_8)),
                "brn2", brn.identity, "nnue", nnue == null ? null : nnue.identity,
                "depth", options.depth, "nodeLimit", options.nodes, "timeLimitMs", options.timeMs,
                "warmups", options.warmups, "repetitions", options.repetitions,
                "threads", 1, "ttEntries", TT_ENTRIES, "ttPolicy", "cold-per-search",
                "aspiration", false, "selectivity", "mate-distance-only", "history", "singleton-root",
                "java", System.getProperty("java.runtime.version"), "os", System.getProperty("os.name"),
                "arch", System.getProperty("os.arch"), "processors", Runtime.getRuntime().availableProcessors(),
                "jvmArgs", ManagementFactory.getRuntimeMXBean().getInputArguments());
        for (var p : corpus) write(out, "type", "position", "id", p.name(), "fen", p.fen());
        Measurements measured = measure(((NetworkModel.Brn2) brn.model).model(), nnue == null ? null : nnue.model, corpus);
        evaluations(out, "roots", measured.roots);
        evaluations(out, "children", measured.children);
        for (var d : measured.deltas) write(out, "type", "delta", "id", d.id, "parent", d.parent,
                "move", d.move, "categories", d.categories, "parentScore", d.parentValue.score,
                "childScoreSideToMove", d.childValue.score, "childScoreParentPerspective", -d.childValue.score,
                "signedScoreDelta", d.signedScoreDelta(), "absoluteScoreDelta", d.absoluteScoreDelta(),
                "absoluteNormalizedDelta", d.absoluteValueDelta());
        for (String category : List.of("all", "quiet", "capture", "gives-check", "evasion", "promotion",
                "castle", "en-passant", "non-capture-non-promotion")) {
            var group = measured.deltas.stream().filter(d -> category.equals("all") || d.categories.contains(category)).toList();
            write(out, "type", "delta_summary", "category", category,
                    "score", statistics(group.stream().map(d -> new Sample(d.id, d.absoluteScoreDelta())).toList()),
                    "normalized", statistics(group.stream().map(d -> new Sample(d.id, d.absoluteValueDelta())).toList()));
        }
        boolean failed = false, incomplete = false;
        if (options.depth > 0) {
            List<Loaded> models = nnue == null ? List.of(brn) : List.of(brn, nnue);
            for (var model : models) {
                List<Sample> nodes = new ArrayList<>(), latencies = new ArrayList<>();
                for (int round = -options.warmups; round < options.repetitions; round++) {
                    for (var position : corpus) {
                        String id = model.model.architecture() + "/" + round + "/" + position.name();
                        write(out, "type", "search_start", "id", id, "warmup", round < 0);
                        var result = search(Board.fromFen(position.fen()), model.model.evaluation(NnueScoreMapping.V1),
                                options.depth, options.nodes, options.timeMs, true);
                        searchRecord(out, id, round < 0, options.depth, result);
                        failed |= result.status.equals("FAILURE");
                        incomplete |= !Set.of("COMPLETED", "TERMINAL").contains(result.status);
                        if (round >= 0 && !result.status.equals("FAILURE")) {
                            nodes.add(new Sample(id, result.nodes));
                            latencies.add(new Sample(id, result.elapsedNanos / 1e6));
                        }
                    }
                }
                write(out, "type", "search_summary", "architecture", model.model.architecture(),
                        "nodes", statistics(nodes), "elapsedMs", statistics(latencies));
                peers(out, "nodes", nodes);
                peers(out, "elapsedMs", latencies);
            }
        }
        write(out, "type", "end", "status", failed ? "FAILURE" : incomplete ? "HAS_INCOMPLETE_SEARCHES" : "COMPLETE");
        if (failed) throw new IllegalStateException("One or more diagnostic searches failed; see FAILURE records.");
    }

    private static void evaluations(PrintWriter out, String population, List<Observation> observations) {
        for (var s : observations) write(out, "type", "evaluation", "population", population, "id", s.id,
                "rawPreTanh", s.value.raw, "normalized", s.value.normalized, "searchScore", s.value.score,
                "nnueSearchScore", s.value.nnueScore);
        summary(out, population, "rawPreTanh", observations, v -> v.raw);
        summary(out, population, "normalized", observations, v -> v.normalized);
        summary(out, population, "searchScore", observations, v -> v.score);
        if (!observations.isEmpty() && observations.getFirst().value.nnueScore != null)
            summary(out, population, "nnueSearchScore", observations, v -> v.nnueScore);
        for (double boundary : new double[]{.95, .99, .999, 1}) write(out, "type", "boundary_count",
                "population", population, "absoluteNormalizedAtLeast", boundary,
                "count", observations.stream().filter(s -> Math.abs(s.value.normalized) >= boundary).count());
        write(out, "type", "score_boundary_count", "population", population, "absoluteScoreEquals", BrnScoreMapping.SCALE,
                "count", observations.stream().filter(s -> Math.abs(s.value.score) == BrnScoreMapping.SCALE).count());
    }

    private static void summary(PrintWriter out, String population, String metric, List<Observation> values,
                                ToDoubleFunction<Value> field) {
        write(out, "type", "evaluation_summary", "population", population, "metric", metric,
                "statistics", statistics(values.stream().map(s -> new Sample(s.id, field.applyAsDouble(s.value))).toList()));
    }

    private static void searchRecord(PrintWriter out, String id, boolean warmup, int depth, SearchMeasurement m) {
        var d = m.diagnostics;
        var n = d == null ? null : d.worker().nodes();
        var r = m.result;
        Double ratio = n == null ? null : qRatio(n.mainNodes(), n.qNodes());
        write(out, "type", "search", "id", id, "warmup", warmup, "status", m.status, "error", m.error,
                "requestedDepth", depth, "completedDepth", r == null ? 0 : r.depth(),
                "elapsedNs", m.elapsedNanos, "nodes", m.nodes,
                "mainNodes", n == null ? null : n.mainNodes(), "qNodes", n == null ? null : n.qNodes(),
                "qRatio", ratio, "highQRatio", ratio != null && ratio >= .95,
                "nps", m.elapsedNanos <= 0 || m.nodes == 0 ? null : m.nodes * 1e9 / m.elapsedNanos,
                "evaluationCalls", n == null ? null : n.evaluationCalls(),
                "maximumPly", n == null ? null : n.maximumAbsolutePly(), "maximumQply", n == null ? null : n.maximumQply(),
                "softQDepthEncounters", d == null ? null : d.worker().qsearch().softDepthLimitEncounters(),
                "checkedQNodes", d == null ? null : d.worker().qsearch().checkedQNodes(),
                "standPatCutoffs", d == null ? null : d.worker().qsearch().standPatCutoffs(),
                "score", r == null ? null : r.score(),
                "move", r == null || !r.hasMove() ? null : Move.coordinate(r.bestMove()),
                "pv", r == null ? null : Arrays.stream(r.principalVariation()).mapToObj(Move::coordinate).toList());
    }

    private static void peers(PrintWriter out, String metric, List<Sample> values) {
        var nonzero = values.stream().filter(s -> s.value() > 0).sorted(Comparator.comparingDouble(Sample::value)).toList();
        if (nonzero.isEmpty()) return;
        double median = quantile(nonzero, .5);
        for (var s : values) write(out, "type", "peer_comparison", "metric", metric, "id", s.id(),
                "positivePeerMedian", median, "multiple", s.value() / median, "atLeast10xMedian", s.value() >= 10 * median);
    }

    static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    record Options(String brn2, String nnue, Path output, String label, int depth, long nodes,
                   long timeMs, int warmups, int repetitions) {
        static Options parse(String[] args) {
            Map<String, String> values = new HashMap<>();
            Set<String> allowed = Set.of("brn2", "nnue", "output", "label", "depth", "nodes", "time-ms", "warmup", "repetitions");
            for (String arg : args) {
                int separator = arg.indexOf('=');
                if (!arg.startsWith("--") || separator < 3) throw new IllegalArgumentException("Expected --name=value: " + arg);
                String key = arg.substring(2, separator), value = arg.substring(separator + 1);
                if (!allowed.contains(key) || value.isEmpty() || values.putIfAbsent(key, value) != null)
                    throw new IllegalArgumentException("Unknown, empty or duplicate option: " + arg);
            }
            int depth = Integer.parseInt(values.getOrDefault("depth", "4"));
            long nodes = Long.parseLong(values.getOrDefault("nodes", "1000000"));
            long timeMs = Long.parseLong(values.getOrDefault("time-ms", "10000"));
            int warmups = Integer.parseInt(values.getOrDefault("warmup", "0"));
            int repetitions = Integer.parseInt(values.getOrDefault("repetitions", "1"));
            if (depth < 0 || depth > AlphaBetaPvsSearch.MAX_SUPPORTED_DEPTH || nodes < -1 || timeMs < -1
                    || timeMs > Long.MAX_VALUE / 1_000_000 || warmups < 0 || repetitions < 1 || nodes == -1 && timeMs == -1)
                throw new IllegalArgumentException("Invalid options; retain at least one search limit. Depth 0 is evaluation only.");
            return new Options(values.getOrDefault("brn2", "initialized"), values.get("nnue"),
                    values.containsKey("output") ? Path.of(values.get("output")) : null,
                    values.getOrDefault("label", "unspecified"), depth, nodes, timeMs, warmups, repetitions);
        }
    }

    private Brn2Diagnostics() {}
}
