package com.ohinteractive.seedv6.tools.search;

import java.io.PrintWriter;
import java.util.*;
import java.util.function.ToDoubleFunction;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn2.Brn2Model;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.search.diagnostics.QsearchDecisionTrace;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.model.NetworkModel;

import static com.ohinteractive.seedv6.tools.search.DiagnosticReport.*;

/** Tool-only paired evaluator measurements; does not supply search scores or mutate models. */
final class ColorSymmetryDiagnostics {
    static final List<String> CATEGORIES = List.of("all", "quiet", "capture", "gives-check", "evasion",
            "promotion", "castle", "en-passant", "non-capture-non-promotion");
    static final double[] SCORE_THRESHOLDS = {100, 1000, 5000, 10000, 20000};
    record Prediction(double raw, double normalized, int score) {}
    record Values(Prediction brn2, Prediction nnue) {}
    record Pair(String id, String side, Values original, Values transformed) {}
    record Edge(String id, List<String> categories, Pair parent, Pair child) {}
    record Measured(List<Pair> roots, List<Pair> children, List<Edge> edges) {}

    static final class Evaluator {
        final Brn2Diagnostics.Evaluator brn;
        final NnueAccumulator nnueParent, nnueChild;
        final NnueEvaluator nnue;
        Evaluator(Brn2Model model, NetworkModel reference) {
            brn = new Brn2Diagnostics.Evaluator(model, null);
            var network = reference == null ? null : ((NetworkModel.Nnue) reference).network();
            nnueParent = network == null ? null : new NnueAccumulator(network);
            nnueChild = network == null ? null : new NnueAccumulator(network);
            nnue = network == null ? null : new NnueEvaluator(network);
        }
        Values root(long[] board) {
            long[] before = board.clone();
            var b = brn.root(board);
            if (nnue != null) nnueParent.rebuild(board);
            var result = values(b, board, nnueParent);
            unchanged(before, board);
            return result;
        }
        Values child(long[] board, long[] child) {
            long[] before = board.clone(), childBefore = child.clone();
            var b = brn.child(board, child);
            if (nnue != null) nnueChild.update(board, child, nnueParent);
            var result = values(b, child, nnueChild);
            unchanged(before, board); unchanged(childBefore, child);
            return result;
        }
        private Values values(Brn2Diagnostics.Value b, long[] board, NnueAccumulator accumulator) {
            Prediction n = null;
            if (nnue != null) {
                nnue.evaluate(board, accumulator);
                n = new Prediction(nnue.raw(), nnue.boundedValue(), NnueScoreMapping.V1.map(nnue.boundedValue()));
            }
            return new Values(new Prediction(b.raw(), b.normalized(), b.score()), n);
        }
    }

    static Measured measure(Brn2Model model, NetworkModel nnue, List<SearchBenchmark.Position> corpus) {
        var original = new Evaluator(model, nnue);
        var reversed = new Evaluator(model, nnue);
        List<Pair> roots = new ArrayList<>(), children = new ArrayList<>();
        List<Edge> edges = new ArrayList<>();
        for (var p : corpus) {
            var board = Board.fromFen(p.fen());
            var t = ColorReversal.transform(board);
            var root = new Pair(p.name(), side(board), original.root(board), reversed.root(t));
            roots.add(root);
            for (long move : Brn2Diagnostics.legalMoves(board)) {
                var child = Brn2Diagnostics.play(board, move);
                var tc = ColorReversal.transform(child);
                String id = p.name() + "/" + Move.coordinate(move);
                var pair = new Pair(id, side(child), original.child(board, child), reversed.child(t, tc));
                children.add(pair);
                edges.add(new Edge(id, Brn2Diagnostics.categories(board, child, move), root, pair));
            }
        }
        return new Measured(List.copyOf(roots), List.copyOf(children), List.copyOf(edges));
    }

    static void staticReport(PrintWriter out, Brn2Model model, NetworkModel nnue, List<SearchBenchmark.Position> corpus) {
        write(out, "type", "symmetry_policy", "version", 1,
                "transform", "rank xor 56; swap piece colors and STM; KQ<->kq; EP xor 56; preserve counters/status remainder; rebuild key",
                "residual", "original minus transformed, no negation",
                "edgeDelta", "-childScore-parentScore; error=originalDelta-transformedDelta=-(parentResidual+childResidual)",
                "counterConvention", "preserve numeric fullmove label per snapshot; mapped legal move increments after the opposite color, so fullmove bookkeeping does not commute",
                "staticPath", "independent original/transformed root rebuild and parent-child production accumulator updates",
                "qPath", "detached bounded snapshots; both evaluators freshly rebuilt after driving search; check/terminal statics are diagnostic only",
                "scoreThresholdsStrictlyGreater", SCORE_THRESHOLDS_LIST,
                "units", "search units, not calibrated centipawns; normalized thresholds are score thresholds / 32511",
                "quantiles", "linear interpolation at (n-1)*p", "correlation", "Pearson, null for constant/empty populations");
        var m = measure(model, nnue, corpus);
        for (var p : m.roots) write(out, "type", "symmetry_position", "population", "roots", "id", p.id,
                "sideToMove", p.side, "brn2", pair(p.original.brn2, p.transformed.brn2),
                "nnue", pair(p.original.nnue, p.transformed.nnue));
        for (var p : m.children) write(out, "type", "symmetry_position", "population", "children", "id", p.id,
                "sideToMove", p.side, "brn2", pair(p.original.brn2, p.transformed.brn2),
                "nnue", pair(p.original.nnue, p.transformed.nnue));
        var all = new ArrayList<>(m.roots); all.addAll(m.children);
        for (String population : List.of("all", "roots", "children")) {
            var values = population.equals("all") ? all : population.equals("roots") ? m.roots : m.children;
            for (String side : List.of("all", "white", "black")) {
                var group = values.stream().filter(p -> side.equals("all") || side.equals(p.side)).toList();
                write(out, "type", "symmetry_summary", "population", population, "sideToMove", side, "metrics", summaries(group));
            }
        }
        for (var p : corpus) write(out, "type", "symmetry_root_family", "id", p.name(), "metrics",
                summaries(all.stream().filter(v -> v.id.equals(p.name()) || v.id.startsWith(p.name() + "/")).toList()));
        for (var e : m.edges) write(out, "type", "symmetry_edge", "id", e.id, "categories", e.categories,
                "brn2", edge(e, false), "nnue", edge(e, true));
        for (String category : CATEGORIES) write(out, "type", "symmetry_edge_summary", "category", category,
                "metrics", edgeSummaries(m.edges.stream().filter(e -> category.equals("all") || e.categories.contains(category)).toList()));
    }

    static void qReport(PrintWriter out, String searchId, String driver, Brn2Model model, NetworkModel nnue,
                        QsearchDecisionTrace trace, int stride, int limit) {
        var original = new Evaluator(model, nnue);
        var reversed = new Evaluator(model, nnue);
        List<Pair> pairs = new ArrayList<>();
        List<Edge> edges = new ArrayList<>();
        Map<String, List<Pair>> groups = new TreeMap<>();
        for (var s : trace.positionSamples()) {
            String id = searchId + "/q" + s.ordinal();
            var t = ColorReversal.transform(s.board());
            var p = new Pair(id, side(s.board()), original.root(s.board()), reversed.root(t));
            pairs.add(p);
            for (String group : List.of("qply/" + s.qply(), "parity/" + s.qply() % 2, "side/" + p.side,
                    "qply-side/" + s.qply() + "/" + p.side, "check/" + s.check(),
                    "cutoff/" + (s.cutoff() == null ? "UNCLASSIFIED" : s.cutoff()),
                    "stand-pat/" + (!s.standPatAllowed() ? "INELIGIBLE" : s.driver() >= s.beta() ? "CUTOFF" : "CONTINUE")))
                groups.computeIfAbsent(group, k -> new ArrayList<>()).add(p);
            Edge edge = null;
            if (s.driver() != null && s.parentBoard() != null) {
                var parent = new Pair(id + "/parent", side(s.parentBoard()), original.root(s.parentBoard()),
                        reversed.root(ColorReversal.transform(s.parentBoard())));
                edge = new Edge(id, List.of(), parent, p);
                edges.add(edge);
            }
            int rebuiltDriver = driver.equals("BRN2") ? p.original.brn2.score : p.original.nnue.score;
            write(out, "type", "qsymmetry_position", "id", searchId, "sampleId", id,
                    "ordinal", s.ordinal(), "iteration", s.iteration(), "attempt", s.attempt(),
                    "ply", s.ply(), "qply", s.qply(), "sideToMove", p.side, "inCheck", s.check(),
                    "alpha", s.alpha(), "beta", s.beta(), "actualDriverStatic", s.driver(), "actualShadowStatic", s.shadow(),
                    "rebuiltMinusActualDriver", s.driver() == null ? null : rebuiltDriver - s.driver(),
                    "standPatAllowed", s.standPatAllowed(), "cutoffClass", s.cutoff(), "reason", s.reason(),
                    "boardLongsHex", hex(s.board()), "transformedBoardLongsHex", hex(t),
                    "parentBoardLongsHex", s.parentBoard() == null ? null : hex(s.parentBoard()),
                    "brn2", pair(p.original.brn2, p.transformed.brn2), "nnue", pair(p.original.nnue, p.transformed.nnue),
                    "brn2Edge", edge == null ? null : edge(edge, false), "nnueEdge", edge == null ? null : edge(edge, true));
        }
        write(out, "type", "qsymmetry_summary", "id", searchId, "selection",
                "(entryOrdinal-1) modulo stride == 0 and (entryOrdinal-1)/stride < limit; across all iterations; duplicates retained",
                "stride", stride, "limit", limit, "observed", trace.summary().get("observed"),
                "sampleCount", pairs.size(), "metrics", summaries(pairs), "edges", edgeSummaries(edges),
                "largestBrnScoreResidualSampleIds", pairs.stream().sorted(Comparator
                        .<Pair>comparingInt(p -> Math.abs(p.original.brn2.score - p.transformed.brn2.score)).reversed()
                        .thenComparing(Pair::id)).limit(16).map(Pair::id).toList());
        groups.forEach((group, values) -> write(out, "type", "qsymmetry_group", "id", searchId,
                "group", group, "metrics", summaries(values)));
    }

    private static final List<Double> SCORE_THRESHOLDS_LIST = Arrays.stream(SCORE_THRESHOLDS).boxed().toList();
    static Map<String, Object> pair(Prediction a, Prediction b) {
        return a == null ? null : fields("original", prediction(a), "transformed", prediction(b),
                "scoreResidual", a.score - b.score, "absoluteScoreResidual", Math.abs(a.score - b.score),
                "normalizedResidual", a.normalized - b.normalized, "absoluteNormalizedResidual", Math.abs(a.normalized - b.normalized),
                "rawResidual", a.raw - b.raw, "absoluteRawResidual", Math.abs(a.raw - b.raw));
    }
    private static Map<String, Object> prediction(Prediction p) {
        return fields("rawPreTanh", p.raw, "normalized", p.normalized, "searchScore", p.score);
    }
    static Map<String, Object> summaries(List<Pair> pairs) {
        return fields("brn2", summary(pairs, false), "nnue", summary(pairs, true));
    }
    private static Map<String, Object> summary(List<Pair> pairs, boolean nnue) {
        if (!pairs.isEmpty() && nnue && pairs.getFirst().original.nnue == null) return null;
        List<Prediction> a = pairs.stream().map(p -> nnue ? p.original.nnue : p.original.brn2).toList();
        List<Prediction> b = pairs.stream().map(p -> nnue ? p.transformed.nnue : p.transformed.brn2).toList();
        return fields("score", stats(a, b, p -> p.score, SCORE_THRESHOLDS),
                "normalized", stats(a, b, p -> p.normalized, Arrays.stream(SCORE_THRESHOLDS).map(v -> v / 32511).toArray()),
                "rawPreTanh", stats(a, b, p -> p.raw, new double[0]));
    }
    private static Map<String, Object> stats(List<Prediction> a, List<Prediction> b,
                                            ToDoubleFunction<Prediction> value, double[] thresholds) {
        return residualStats(a.stream().mapToDouble(value).toArray(), b.stream().mapToDouble(value).toArray(), thresholds);
    }
    static Map<String, Object> residualStats(double[] a, double[] b, double[] thresholds) {
        if (a.length != b.length) throw new IllegalArgumentException("Unpaired values.");
        if (a.length == 0) return fields("count", 0);
        List<Sample> abs = new ArrayList<>();
        double sum = 0, sumAbs = 0, ma = Arrays.stream(a).average().orElseThrow(), mb = Arrays.stream(b).average().orElseThrow();
        double covariance = 0, va = 0, vb = 0;
        Map<String, Object> counts = new LinkedHashMap<>();
        for (int i = 0; i < a.length; i++) {
            double r = a[i] - b[i]; sum += r; sumAbs += Math.abs(r);
            abs.add(new Sample(Integer.toString(i), Math.abs(r)));
            covariance += (a[i] - ma) * (b[i] - mb); va += (a[i] - ma) * (a[i] - ma); vb += (b[i] - mb) * (b[i] - mb);
        }
        abs.sort(Comparator.comparingDouble(Sample::value));
        for (double t : thresholds) counts.put(Double.toString(t), abs.stream().filter(s -> s.value() > t).count());
        return fields("count", a.length, "signedMean", sum / a.length, "absoluteMean", sumAbs / a.length,
                "medianAbsolute", quantile(abs, .5), "p95Absolute", quantile(abs, .95), "p99Absolute", quantile(abs, .99),
                "maxAbsolute", abs.getLast().value(), "correlation", va == 0 || vb == 0 ? null : covariance / Math.sqrt(va * vb),
                "countsStrictlyGreater", counts);
    }
    static Map<String, Object> edge(Edge e, boolean nnue) {
        var p = nnue ? e.parent.original.nnue : e.parent.original.brn2;
        if (p == null) return null;
        var c = nnue ? e.child.original.nnue : e.child.original.brn2;
        var tp = nnue ? e.parent.transformed.nnue : e.parent.transformed.brn2;
        var tc = nnue ? e.child.transformed.nnue : e.child.transformed.brn2;
        int a = -c.score - p.score, b = -tc.score - tp.score;
        return fields("originalDelta", a, "transformedDelta", b, "signedError", a - b, "absoluteError", Math.abs(a - b),
                "symmetricComponentDelta", (a + b) / 2.0, "antisymmetricComponentDelta", (a - b) / 2.0);
    }
    static Map<String, Object> edgeSummaries(List<Edge> edges) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (boolean nnue : List.of(false, true)) {
            var values = edges.stream().map(e -> edge(e, nnue)).filter(Objects::nonNull).toList();
            var summary = residualStats(values.stream().mapToDouble(v -> (int) v.get("originalDelta")).toArray(),
                    values.stream().mapToDouble(v -> (int) v.get("transformedDelta")).toArray(), SCORE_THRESHOLDS);
            for (String key : List.of("originalDelta", "transformedDelta", "symmetricComponentDelta", "antisymmetricComponentDelta")) {
                List<Sample> samples = new ArrayList<>();
                for (int i = 0; i < values.size(); i++) samples.add(new Sample(Integer.toString(i), Math.abs(((Number) values.get(i).get(key)).doubleValue())));
                var s = statistics(samples); s.remove("highest"); s.remove("lowest"); summary.put("absolute" + key, s);
            }
            result.put(nnue ? "nnue" : "brn2", summary);
        }
        return result;
    }
    private static String side(long[] board) { return Board.player((int) board[Board.STATUS]) == 0 ? "white" : "black"; }
    private static List<String> hex(long[] board) { return Arrays.stream(board).mapToObj(Long::toHexString).toList(); }
    private static void unchanged(long[] a, long[] b) {
        if (!Arrays.equals(a, b)) throw new IllegalStateException("Diagnostic evaluation mutated board.");
    }
    private ColorSymmetryDiagnostics() {}
}
