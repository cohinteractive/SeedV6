package com.ohinteractive.seedv6.tools.eval;

import com.google.gson.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brnpair2.*;
import com.ohinteractive.seedv6.core.brnnext.*;
import com.ohinteractive.seedv6.core.util.Fen;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.search.exact.SeePolicy;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.history.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.selfplay.HeadlessGame;
import com.ohinteractive.seedv6.training.service.LearningArenaState;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Read-only store reconciliation and bounded evaluator/search isolation. No trainer updates. */
public final class BrnSuccessorAudit {
    record ModelSpec(String name, String root, String checkpoint) {}
    record Plan(String id, List<ModelSpec> models) {}
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting()
            .registerTypeAdapter(Instant.class, (JsonSerializer<Instant>) (v,t,c) -> new JsonPrimitive(v.toString())).create();

    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("PLAN NEW_OUTPUT_DIRECTORY ARENA_ROOT");
        var plan = DataFiles.read(Path.of(args[0]), Plan.class);
        Path output = Path.of(args[1]);
        Files.createDirectory(output); // Refuse to replace any completed or failed experiment.
        long deadline = System.nanoTime() + 300_000_000_000L;
        var boards = boards();
        var evidence = new ArrayList<Object>();
        var trained = new ArrayList<SearchEvaluation>();
        for (var spec : plan.models()) {
            Path root = Path.of(spec.root());
            var snapshot = CheckpointStore.readSnapshot(root, spec.checkpoint());
            var state = CheckpointStore.readTrainingSnapshot(root, spec.checkpoint());
            var history = new HistoryRepository(root).refresh();
            var chain = CheckpointInspection.lineage(root, spec.checkpoint());
            var provenance = LineageProvenance.read(root);
            var item = new LinkedHashMap<String,Object>();
            item.put("name", spec.name()); item.put("root", spec.root());
            item.put("manifest", snapshot.manifest()); item.put("optimizerStep", state.step());
            item.put("optimizer", state.hyperparameters()); item.put("chain", chain);
            item.put("history", history); item.put("ledger", new SourceLedger(root).state());
            item.put("best", CheckpointInspection.reference(root, "best"));
            item.put("latestTraining", CheckpointInspection.reference(root, "latest-training"));
            item.put("initializer", provenance.isPresent() ? JSON.toJsonTree(provenance.get()) : JsonNull.INSTANCE);
            var runtime = snapshot.model().evaluation(NnueScoreMapping.V1);
            var recovered = state.snapshot().evaluation(NnueScoreMapping.V1);
            var scores = scores(runtime, boards);
            if (!scores.equals(scores(recovered, boards))) throw new IllegalStateException("Optimizer/runtime mismatch: " + spec.name());
            item.put("runtimeScores", scores); item.put("optimizerRuntimeScoreParity", true);
            if (spec.name().equals("Gen32")) {
                if (!snapshot.manifest().networkSha256().equals(ProductionModel.SHA256) || state.step() != 1753088)
                    throw new IllegalStateException("Frozen baseline identity mismatch");
                var bundled = SearchEvaluation.brnPair2(ProductionModel.load(Path.of("build-assets/production/bre-pair2-g32.brn-pair2")));
                if (!scores.equals(scores(bundled, boards))) throw new IllegalStateException("Frozen runtime mismatch");
            }
            evidence.add(item); trained.add(runtime);
            write(output.resolve("stores.json"), evidence);
        }
        var campaigns = new ArrayList<Object>();
        try (var entries = Files.list(Path.of(args[2]))) {
            for (var path : entries.sorted().toList()) {
                if (!path.getFileName().toString().startsWith("20261020-") || !Files.isRegularFile(path.resolve("campaign.json"))) continue;
                var campaign = LearningArenaState.read(path);
                var item = new LinkedHashMap<String,Object>();
                item.put("path", path.toString()); item.put("binding", campaign.binding());
                item.put("config", campaign.config()); item.put("status", campaign.status()); item.put("message", campaign.message());
                var rounds = new ArrayList<Object>();
                for (var round : campaign.history()) {
                    var row = new LinkedHashMap<String,Object>();
                    row.put("round", round.number()); row.put("stage", round.stage()); row.put("trancheHash", round.trancheHash());
                    row.put("sourceEnd", round.sourceEnd()); row.put("a", round.a()); row.put("b", round.b());
                    var result = round.result(campaign.config());
                    if (result != null) row.put("statistics", result.statistics());
                    rounds.add(row);
                }
                item.put("rounds", rounds); campaigns.add(item);
            }
        }
        write(output.resolve("campaigns.json"), campaigns);
        var gen0 = new ArrayList<SearchEvaluation>();
        gen0.add(SearchEvaluation.brnPair2(new BrnPair2Model(new BrnPair2Trainer(.01).snapshot())));
        for (var kind : BrnNextArchitecture.values()) gen0.add(SearchEvaluation.brnNext(new BrnNextTrainer(kind, 1, .003).snapshot()));
        var expected = scores(gen0.getFirst(), boards);
        for (var evaluation : gen0) if (!expected.equals(scores(evaluation, boards))) throw new IllegalStateException("Gen0 material mismatch");
        var searches = new ArrayList<Object>();
        for (int i = 0; i < 8; i++) {
            SearchResult control = null;
            for (int model = 0; model < gen0.size(); model++) {
                budget(deadline);
                SearchResult result = search(gen0.get(model), boards.get(i), true);
                if (control != null && !control.equals(result)) throw new IllegalStateException("Common search Gen0 mismatch at " + i + "/" + model);
                control = result;
                searches.add(Map.of("position", i, "model", model, "stage", "Gen0-common", "result", result));
                searches.add(Map.of("position", i, "model", model, "stage", "Gen0-production", "result", search(gen0.get(model), boards.get(i), false)));
                searches.add(Map.of("position", i, "model", model, "stage", "trained-common", "result", search(trained.get(model), boards.get(i), true)));
            }
            write(output.resolve("searches.json"), searches);
        }
        var report = new LinkedHashMap<String,Object>();
        report.put("plan", plan.id()); report.put("completedUtc", Instant.now());
        report.put("java", System.getProperty("java.runtime.version")); report.put("activeProcessors", Runtime.getRuntime().availableProcessors());
        report.put("fenRoots", boards.stream().map(Fen::fromBoard).toList());
        report.put("gen0MaterialParity", true); report.put("gen0CommonSearchParity", true);
        report.put("trainedOptimizerRuntimeScoreParity", true);
        write(output.resolve("complete.json"), report);
        System.out.println("Verified store identities, campaign accounting, runtime parity and common search parity: " + output);
    }
    static ExactSearchAdapter common(SearchEvaluation evaluation) {
        return ExactSearchAdapter.seeResearch(evaluation, SeePolicy.QXRAY, null, new TTable(64)).withCompletedHints(true, null);
    }
    private static SearchResult search(SearchEvaluation evaluation, long[] board, boolean common) {
        try (var search = new SearchDriver(common ? common(evaluation) : ProductionSearch.create(1, evaluation, 64))) {
            var outcome = search.search(new SearchRequest(board, 3));
            if (!outcome.targetDepthCompleted()) throw new IllegalStateException("Incomplete depth3 diagnostic");
            return outcome.lastCompletedResult();
        }
    }
    private static List<Integer> scores(SearchEvaluation evaluation, List<long[]> boards) {
        var state = evaluation.newState(2); var result = new ArrayList<Integer>();
        for (var board : boards) { state.initialize(board, 0); result.add(state.evaluate(board, 0)); }
        return result;
    }
    private static List<long[]> boards() {
        var result = new ArrayList<long[]>(); var random = new SplittableRandom(2026102002L);
        var game = new HeadlessGame(Board.startingPosition(), 256);
        while (result.size() < 24) {
            result.add(game.boardSnapshot());
            for (int i=0; i<3; i++) {
                if (!game.active()) game = new HeadlessGame(Board.startingPosition(), 256);
                var moves = game.legalMoves(); game.play(moves[random.nextInt(moves.length)]);
            }
        }
        return result;
    }
    private static void budget(long deadline) { if (System.nanoTime() > deadline) throw new IllegalStateException("Audit time budget exceeded"); }
    private static void write(Path file, Object value) throws Exception { Files.writeString(file, JSON.toJson(value) + "\n"); }
    private BrnSuccessorAudit() {}
}
