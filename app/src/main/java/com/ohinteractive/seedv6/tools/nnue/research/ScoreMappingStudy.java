package com.ohinteractive.seedv6.tools.nnue.research;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.search.driver.SearchDriver;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.validation.*;

/**
 * Read-only fixed-network mapping experiments. Emits TSV to stdout; never opens a
 * store owner, repairs references, or publishes records. Redirect only to an experimental path.
 * Match nodes include all iterative depths, for both actors; diagnostics use fresh private TTs.
 */
public final class ScoreMappingStudy {
    static final NnueScoreMapping LEGACY = new NnueScoreMapping(1_000_000);
    static final List<NnueScoreMapping> MAPPINGS = List.of(LEGACY, new NnueScoreMapping(4096),
            new NnueScoreMapping(16384), NnueScoreMapping.V1);
    private static volatile long sink;

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);
        switch (args[0]) {
            case "distribution" -> {
                var checkpoint = inspect(Path.of(args[1]));
                distribution("corpus", checkpoint.network(), NetworkHealth.corpus(Long.parseLong(args[2]), 128));
                if (args.length == 4) distribution("holdout", checkpoint.network(), holdout(Path.of(args[3])));
            }
            case "diagnostics" -> diagnostics(inspect(Path.of(args[1])).network(), Long.parseLong(args[2]), Integer.parseInt(args[3]));
            case "match" -> match(args);
            case "store" -> store(Path.of(args[1]), Long.parseLong(args[2]));
            case "cost" -> cost(Long.parseLong(args[1]));
            default -> throw new IllegalArgumentException("distribution CHECKPOINT SEED [HOLDOUT] | diagnostics CHECKPOINT SEED COUNT | match A B MODE SEED PAIRS SCALE [B_SCALE] | store ROOT SEED | cost SEED");
        }
    }

    private static CheckpointStore.Checkpoint inspect(Path path) throws IOException {
        var checkpoint = CheckpointStore.inspect(path);
        ResearchTool.out("IDENTITY", checkpoint.manifest());
        return checkpoint;
    }

    static List<long[]> holdout(Path path) throws IOException {
        List<long[]> result = new ArrayList<>();
        try (var in = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            if (in.readInt() != 0x4A484F31) throw new IOException("Unknown holdout format.");
            while (in.available() > 0) {
                in.readLong(); in.readInt(); int count = in.readInt();
                if (count < 1 || count > 32) throw new IOException("Invalid holdout count.");
                for (int i = 0; i < count; i++) {
                    long[] board = new long[Board.MAX_BITBOARDS];
                    for (int b = 0; b < board.length; b++) board[b] = in.readLong();
                    in.readDouble(); result.add(board);
                }
            }
        }
        return List.copyOf(result);
    }

    static void distribution(String label, NnueNetwork network, List<long[]> boards) {
        var evaluator = new NnueEvaluator(network);
        double[] raw = new double[boards.size()];
        for (int i = 0; i < raw.length; i++) { evaluator.evaluate(boards.get(i)); raw[i] = evaluator.boundedValue(); }
        ResearchTool.out("RAW", label, "count", raw.length, "distinct", Arrays.stream(raw).distinct().count(),
                NetworkHealth.distribution(raw.length, i -> raw[i]));
        for (var mapping : MAPPINGS) {
            int[] scores = Arrays.stream(raw).mapToInt(mapping::map).toArray();
            int clips = 0, inversions = 0, tiedDistinct = 0;
            for (int i = 0; i < raw.length; i++) {
                if (Math.abs(raw[i]) * mapping.scale() > TranspositionScores.MAX_NORMAL_SCORE) clips++;
                for (int j = i + 1; j < raw.length; j++) {
                    if ((raw[i] - raw[j]) * (scores[i] - scores[j]) < 0) inversions++;
                    if (raw[i] != raw[j] && scores[i] == scores[j]) tiedDistinct++;
                }
            }
            Arrays.sort(scores);
            ResearchTool.out("DISTRIBUTION", label, "scale", mapping.scale(), "count", scores.length,
                    "clipPercent", 100.0 * clips / scores.length, "zeroPercent", 100.0 * Arrays.stream(scores).filter(s -> s == 0).count() / scores.length,
                    "distinct", Arrays.stream(scores).distinct().count(), "min", scores[0], "p05", quantile(scores, .05),
                    "median", quantile(scores, .5), "p95", quantile(scores, .95), "max", scores[scores.length - 1],
                    "floorHits", Arrays.stream(scores).filter(s -> s == -TranspositionScores.MAX_NORMAL_SCORE).count(),
                    "ceilingHits", Arrays.stream(scores).filter(s -> s == TranspositionScores.MAX_NORMAL_SCORE).count(),
                    "orderingInversions", inversions, "tiedDistinctPairs", tiedDistinct);
        }
        // Plain nearest rounding is a credible alternative near zero. It loses sign there;
        // symmetric magnitude rounding avoids Java round's negative half-tie asymmetry.
        ResearchTool.out("PLAIN_ROUND_ALTERNATIVE", label, "zeroCount", Arrays.stream(raw)
                .filter(v -> Math.round(Math.abs(v) * NnueScoreMapping.V1.scale()) == 0).count());
    }

    private static int quantile(int[] sorted, double p) { return sorted[Math.max(0, (int) Math.ceil(p * sorted.length) - 1)]; }

    private static void store(Path root, long seed) throws IOException {
        // Each reference is a snapshot, not a claim of an atomic multi-reference transaction.
        String latest = CheckpointInspection.reference(root, "latest-training"), best = CheckpointInspection.reference(root, "best");
        var lineage = CheckpointInspection.lineage(root, latest);
        ResearchTool.out("REFERENCES", "latest", latest, "best", best, "bootstrap", lineage.getFirst().id());
        for (String id : new LinkedHashSet<>(List.of(lineage.getFirst().id(), best, latest))) {
            var checkpoint = inspect(root.resolve("checkpoints").resolve(id));
            distribution(id, checkpoint.network(), NetworkHealth.corpus(seed, 128));
        }
        for (var record : CheckpointInspection.validations(root).values()) ResearchTool.out("STORED_VALIDATION", record.candidateId(), record.config(), record.statistics());
        if (!latest.equals(CheckpointInspection.reference(root, "latest-training")) || !best.equals(CheckpointInspection.reference(root, "best"))) {
            throw new IOException("References changed during read-only inspection; results are not a stable store snapshot.");
        }
    }

    private static SearchRequest request(long[] board, int depth) {
        return new SearchRequest(board, GameHistory.initial(board), depth, SearchObserver.NONE, SearchControl.unlimited(), true);
    }

    private static void diagnostics(NnueNetwork network, long seed, int count) {
        var boards = NetworkHealth.corpus(seed, count);
        int moves = 0, pvs = 0;
        Metrics[] totals = {new Metrics(), new Metrics()};
        int[][] rootScores = {new int[count], new int[count]};
        for (int i = 0; i < count; i++) {
            SearchResult[] results = new SearchResult[2];
            for (int m = 0; m < 2; m++) {
                var mapping = m == 0 ? LEGACY : NnueScoreMapping.V1;
                long start = System.nanoTime();
                try (var search = new SearchDriver(SearchEvaluation.incremental(network, mapping))) {
                    var outcome = search.search(request(boards.get(i), 2));
                    if (!outcome.targetDepthCompleted()) throw new IllegalStateException("Incomplete diagnostic search.");
                    results[m] = outcome.lastCompletedResult(); rootScores[m][i] = results[m].score();
                    totals[m].add(outcome.diagnostics());
                    ResearchTool.out("SEARCH", i, "scale", mapping.scale(), "score", results[m].score(),
                            "move", Move.coordinate(results[m].bestMove()), "pv", Arrays.toString(results[m].principalVariation()),
                            "metrics", outcome.diagnostics().worker().nodes(), "tt", outcome.diagnostics().worker().transpositionTable(),
                            "seconds", ResearchTool.seconds(start));
                }
                totals[m].nanos += System.nanoTime() - start;
            }
            if (results[0].bestMove() != results[1].bestMove()) moves++;
            if (!Arrays.equals(results[0].principalVariation(), results[1].principalVariation())) pvs++;
        }
        ResearchTool.out("SEARCH_CHANGES", "positions", count, "moves", moves, "pvs", pvs);
        for (int m = 0; m < 2; m++) {
            Arrays.sort(rootScores[m]);
            ResearchTool.out("SEARCH_TOTAL", m == 0 ? "legacy" : "v1", totals[m],
                    "rootDistinct", Arrays.stream(rootScores[m]).distinct().count(), "min", rootScores[m][0], "median", quantile(rootScores[m], .5),
                    "max", rootScores[m][count - 1], "ordinaryBoundaryHits", Arrays.stream(rootScores[m]).filter(s -> Math.abs(s) == TranspositionScores.MAX_NORMAL_SCORE).count());
        }
    }

    private static StrengthArena.Actor actor(String path) throws IOException {
        if (path.equals("handcrafted")) return StrengthArena.Actor.handcrafted();
        var checkpoint = inspect(Path.of(path));
        return new StrengthArena.Actor(checkpoint.manifest().id(), checkpoint.network());
    }

    private static void match(String[] args) throws Exception {
        var a = actor(args[1]); var b = actor(args[2]);
        var mode = StrengthArena.Mode.valueOf(args[3]);
        int pairs = Integer.parseInt(args[5]);
        var resources = new ValidationConfig(pairs, Long.parseLong(args[4]), 0, 8, 2, 1, new NnueScoreMapping(Double.parseDouble(args[6])), 1024);
        var config = new StrengthArena.Config(resources, mode, pairs, .05);
        var bResources = new ValidationConfig(pairs, resources.seed(), 0, 8, 2, 1,
                args.length == 8 ? new NnueScoreMapping(Double.parseDouble(args[7])) : resources.scoreMapping(), 1024);
        ResearchTool.out("MATCH_START", a.id(), b.id(), config, "bScale", bResources.scoreMapping().scale(), "safetySeconds", 600);
        Metrics metrics = new Metrics(); long start = System.nanoTime();
        var control = new ValidationControl(); var timer = Executors.newSingleThreadScheduledExecutor();
        timer.schedule(control::cancel, 600, TimeUnit.SECONDS);
        StrengthArena.Result result;
        try {
            long[] board = Board.startingPosition();
            result = new StrengthArena().match(a, b, config, board, GameHistory.initial(board), control, (actor, cfg) -> {
                var search = new SearchDriver(actor.evaluation(mode, actor == b ? bResources : resources));
                return new StrengthArena.Player() {
                    public long move(SearchRequest request) {
                        long[] board = new long[Board.MAX_BITBOARDS]; request.copyBoardInto(board);
                        var outcome = search.search(new SearchRequest(board, request.gameHistory(), request.depth(), SearchObserver.NONE, request.control(), true));
                        metrics.add(outcome.diagnostics());
                        var last = outcome.lastCompletedResult();
                        if (!outcome.targetDepthCompleted() || last == null || !last.completed() || !last.hasMove()) throw new IllegalStateException("Incomplete fixed-depth move.");
                        return last.bestMove();
                    }
                    public void close() { search.close(); }
                };
            });
        } finally { timer.shutdownNow(); timer.awaitTermination(10, TimeUnit.SECONDS); }
        int min = Integer.MAX_VALUE, max = 0;
        for (var pair : result.games().pairs()) {
            ResearchTool.out("PAIR", pair);
            min = Math.min(min, Math.min(pair.candidateWhite().plies(), pair.candidateBlack().plies()));
            max = Math.max(max, Math.max(pair.candidateWhite().plies(), pair.candidateBlack().plies()));
        }
        metrics.nanos = System.nanoTime() - start;
        ResearchTool.out("MATCH", result.games().statistics(), result.evidence(), "pliesMean/min/max",
                result.games().statistics().totalPlies() / (2.0 * pairs), min, max, "meanNodesPerGame", metrics.nodes / (2.0 * pairs), metrics);
    }

    private static final class Metrics {
        long nodes, qnodes, probes, hits, cutoffs, searches, nanos;
        void add(SearchDiagnosticsSnapshot d) {
            searches++; nodes += d.totalEnteredNodes(); qnodes += d.worker().nodes().qNodes();
            var tt = d.worker().transpositionTable(); probes += tt.probes(); hits += tt.keyMatches(); cutoffs += tt.usableBoundCutoffs();
        }
        public String toString() { return "Metrics[searches=" + searches + ", nodes=" + nodes + ", qnodes=" + qnodes + ", probes=" + probes + ", hits=" + hits + ", cutoffs=" + cutoffs + ", seconds=" + nanos / 1e9 + "]"; }
    }

    private static void cost(long seed) {
        double[] values = new double[4096]; var random = new SplittableRandom(seed);
        for (int i = 0; i < values.length; i++) values[i] = random.nextDouble(-1, 1);
        int repetitions = 2_000_000;
        for (int warm = 0; warm < 6; warm++) for (var mapping : List.of(LEGACY, NnueScoreMapping.V1)) sink = calls(mapping, values, repetitions);
        for (int round = 0; round < 8; round++) for (var mapping : round % 2 == 0 ? List.of(LEGACY, NnueScoreMapping.V1) : List.of(NnueScoreMapping.V1, LEGACY)) {
            long start = System.nanoTime(); sink = calls(mapping, values, repetitions);
            ResearchTool.out("MAPPING_COST", "round", round, "scale", mapping.scale(), "nsPerCall", (System.nanoTime() - start) / (double) repetitions, "checksum", sink);
        }
    }
    private static long calls(NnueScoreMapping mapping, double[] values, int count) {
        long sum = 0; for (int i = 0; i < count; i++) sum += mapping.map(values[i & (values.length - 1)]); return sum;
    }
    private ScoreMappingStudy() {}
}
