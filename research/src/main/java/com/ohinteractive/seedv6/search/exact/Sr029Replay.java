package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import com.google.gson.Gson;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Fen;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;

/** Outcome-blind stratified reservoir of actual LMR probes, then independent
 * TT-off, unreduced qsearch replays with complete legal history and real-ply correction. */
public final class Sr029Replay {
    record Sample(long[] child, GameHistory history, String childFen, List<String> path,
            int ply, int depth, int reduction, int ordinal, int quietOrdinal,
            int alpha, int beta, int observed, boolean verifiedNext) {}
    static final class Bucket {
        int population;
        final ArrayList<Sample> samples = new ArrayList<>();
    }
    static long moveBetween(long[] parent, long[] child) {
        for(long move : ExhaustiveOracle.legalMoves(parent))
            if(Arrays.equals(ExhaustiveOracle.child(parent, move), child)) return move;
        throw new AssertionError("Captured path contains a nonlegal transition");
    }

    public static void main(String[] args) throws Exception {
        if(args.length != 3) throw new IllegalArgumentException("store corpus.tsv new-output.jsonl");
        var snapshot = CheckpointStore.readSnapshot(Path.of(args[0]),
                "g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304");
        if(!snapshot.manifest().networkSha256().equals("5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7"))
            throw new AssertionError("Wrong network");
        var definition = snapshot.model().evaluation(NnueScoreMapping.V1);
        var gson = new Gson(); int rootIndex = 0, samples = 0, completedReplays = 0;
        try(var out = Files.newBufferedWriter(Path.of(args[2]), StandardOpenOption.CREATE_NEW)) {
            for(String line : Files.readAllLines(Path.of(args[1]))) {
                if(line.isBlank() || line.startsWith("#")) continue;
                var fields = line.split("\t", -1);
                var board = Board.fromFen(fields[1]); var builder = GameHistory.builder(board);
                if(fields.length > 2 && !fields[2].isBlank()) for(String uci : fields[2].split(" ")) {
                    long move = Arrays.stream(ExhaustiveOracle.legalMoves(board))
                            .filter(m -> Move.coordinate(m).equals(uci)).findFirst().orElseThrow();
                    board = ExhaustiveOracle.child(board, move); builder.appendPosition(board);
                }
                final var history = builder.snapshot();
                for(int policy : new int[] {ExactSearch.LMR_SCALED, ExactSearch.LMR_SCOUT}) {
                    var positions = new long[257][Board.MAX_BITBOARDS];
                    var delegate = ExactEvaluator.from(definition);
                    var evaluator = new ExactEvaluator() {
                        public void initialize(long[] b) { System.arraycopy(b, 0, positions[0], 0, b.length); delegate.initialize(b); }
                        public void child(long[] parent, long[] child, int p) {
                            System.arraycopy(child, 0, positions[p+1], 0, child.length); delegate.child(parent, child, p);
                        }
                        public int evaluate(long[] b, int p) { return delegate.evaluate(b, p); }
                    };
                    var buckets = new TreeMap<String, Bucket>();
                    var rng = new Random(290029L + rootIndex * 17L + policy);
                    var diag = new LmrDiagnostics() {
                        public void probe(long[] child, int ply, int depth, int r, int ordinal,
                                int alpha, int beta, int score, boolean verify) {
                            String key = Math.min(depth, 4) + ":" + (ordinal < 8 ? "4-7" : "8+");
                            var bucket = buckets.computeIfAbsent(key, k -> new Bucket());
                            int slot = rng.nextInt(++bucket.population);
                            if(bucket.samples.size() == 8 && slot >= 8) return;
                            var h = GameHistory.builder(history); var path = new ArrayList<String>();
                            for(int p=1; p<=ply; p++) {
                                h.appendPosition(positions[p]);
                                path.add(Move.coordinate(moveBetween(positions[p-1], positions[p])));
                            }
                            var parent = positions[ply-1]; int tacticals = 0;
                            for(long move : ExhaustiveOracle.legalMoves(parent))
                                if(CaptureHistory.isTactical(move, Board.enPassantSquare((int)parent[Board.STATUS]))) tacticals++;
                            var sample = new Sample(child.clone(), h.snapshot(), Fen.fromBoard(child), path,
                                    ply, depth, r, ordinal, ordinal - tacticals, alpha, beta, score, verify);
                            if(bucket.samples.size() < 8) bucket.samples.add(sample);
                            else bucket.samples.set(slot, sample);
                        }
                    };
                    var search = ExactSearch.lateMoveReductionResearch(evaluator, new TTable(), policy, diag);
                    var observed = search.search(board, history, 6, () -> search.visitedNodes() >= 5_000_000);
                    var shadow = ExactSearch.lateMoveReductionResearch(ExactEvaluator.from(definition), new TTable(), policy, null);
                    var control = shadow.search(board, history, 6, () -> shadow.visitedNodes() >= 5_000_000);
                    if(!observed.completed() || !control.completed() || observed.score() != control.score()
                            || observed.nodes() != control.nodes() || observed.qnodes() != control.qnodes()
                            || !Arrays.equals(observed.principalVariation(), control.principalVariation()))
                        throw new AssertionError("Observation changed search or exceeded capture guard: " + fields[0]);
                    out.write(gson.toJson(Map.of("kind", "capture", "root", fields[0], "policy", policy,
                            "nodes", observed.nodes(), "score", observed.score(), "lmr", diag,
                            "population", buckets.entrySet().stream().collect(java.util.stream.Collectors.toMap(
                                    Map.Entry::getKey, e -> e.getValue().population))))); out.newLine(); out.flush();
                    for(var entry : buckets.entrySet()) for(var sample : entry.getValue().samples) {
                        samples++;
                        var reference = ExactSearch.quiescenceResearch(ExactEvaluator.from(definition));
                        var replay = reference.search(sample.child(), sample.history(), sample.depth(),
                                () -> reference.visitedNodes() >= 2_000_000);
                        var row = new LinkedHashMap<String,Object>();
                        row.put("kind", "replay"); row.put("root", fields[0]); row.put("initialFen", fields[1]);
                        row.put("gameMoves", fields.length > 2 ? fields[2] : ""); row.put("policy", policy);
                        row.put("stratum", entry.getKey()); row.put("sample", sample); row.put("reference", replay);
                        if(replay.completed()) {
                            completedReplays++;
                            int value = replay.score();
                            if(value > ExactSearch.MAX_STATIC_SCORE) value -= sample.ply();
                            if(value < -ExactSearch.MAX_STATIC_SCORE) value += sample.ply();
                            row.put("nominalParentValue", -value);
                            row.put("falseDismissal", !sample.verifiedNext() && -value > sample.alpha());
                            Phase2Oracle.legalPv(sample.child(), replay.principalVariation());
                        }
                        out.write(gson.toJson(row)); out.newLine(); out.flush();
                    }
                }
                rootIndex++; System.out.println("replayed " + fields[0]);
            }
            out.write(gson.toJson(Map.of("status", "passed", "roots", rootIndex,
                    "samples", samples, "completedReplays", completedReplays))); out.newLine();
        }
    }
}
