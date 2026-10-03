package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.search.exact.ExactSearch;
import com.ohinteractive.seedv6.search.tt.TTable;
import java.util.*;
import java.util.function.ToDoubleFunction;
import java.io.*;
import java.nio.file.*;

public final class BrnResearchDiagnostics {
    static final class Distribution {
        final ArrayList<Double> values = new ArrayList<>();
        double signedSum;
        void add(double value) { if (!Double.isFinite(value)) throw new ArithmeticException(); signedSum+=value;values.add(Math.abs(value)); }
        Map<String,Object> report() {
            values.sort(Double::compare); int n = values.size(); if (n == 0) return Map.of("count", 0);
            return Map.of("count", n, "median", values.get((n-1)/2), "p90", values.get((n-1)*90/100), "p99", values.get((n-1)*99/100),
                    "max", values.get(n-1), "above9", values.stream().filter(v -> v > 9).count(),"signedMean",signedSum/n);
        }
    }
    static int legal(long[] b, long[] moves) { return Gen.genAll(b[0], b[1], b[2], b[3], (int)b[4], b[5], true, moves, new long[6]); }
    static long[] child(long[] b, long move) { var c = new long[6]; Board.makeMoveInto(b[0], b[1], b[2], b[3], (int)b[4], b[5], move, c); return c; }
    static int pieces(long[] b) { return Long.bitCount(b[0] | b[1] | b[2]); }
    static int score(double pawns) { return (int) Math.copySign(Math.min(ExactSearch.MAX_STATIC_SCORE, Math.floor(Math.abs(pawns)*100+.5)), pawns); }
    public static void main(String[] args) throws Exception {
        var data = BrnResearchData.read(Path.of(args[0]), false); Path run = Path.of(args[1]);
        @SuppressWarnings("unchecked") var metadata = DataFiles.read(run.resolve("result.json"), Map.class);
        var manifest=DataFiles.read(Path.of(args[0]).resolve("manifest.json"),Map.class);
        if(!Objects.equals(((Map<?,?>)metadata.get("datasetManifest")).get("payloadSha256"),manifest.get("payloadSha256")))throw new IOException("Dataset mismatch");
        String candidate = (String)metadata.get("candidate");
        boolean production=args.length>4&&args[4].equals("production");
        boolean compact=args.length>4 && args[4].equals("compact");boolean cached=compact || args.length>4 && args[4].equals("cached");
        ToDoubleFunction<long[]> value; NnueNetwork nnue = null;
        com.ohinteractive.seedv6.core.brn3.Brn3Model productionModel=production?BrnResearchComparison.productionModel(run):null;
        try (var input = new BufferedInputStream(Files.newInputStream(run.resolve(candidate.equals("nnue") ? "selected.nnue" : "selected.state")))) {
            if(production){var workspace=productionModel.newWorkspace();value=workspace::evaluatePawns;}
            else if (candidate.startsWith("r0")) { var model = RelationalCandidate.read(input); value = model::predict; }
            else if (candidate.startsWith("r1")) { var model = PooledRelationCandidate.read(input); value = model::predict; }
            else if (candidate.startsWith("r2")) { var model = MessageRelationCandidate.read(input); value = model::predict; }
            else if (candidate.startsWith("r3")) { var model = AnchoredRelationCandidate.read(input); value = model::predict; }
            else if (candidate.startsWith("r4")) { var model = AbsoluteRelationCandidate.read(input).foldedInference();
                if(cached){var inference=new AbsoluteRelationInference(model,compact);value=inference::predict;}else value=model::predict; }
            else { nnue = NnueNetworkCodec.read(input); var evaluator = new NnueEvaluator(nnue); value = b -> { evaluator.evaluate(b); return evaluator.boundedValue(); }; }
        }
        var quiet = new Distribution(); var captures = new Distribution(); var recaptures = new Distribution(); var reversal = new Distribution(); var promotions = new Distribution();
        var relocation = new Distribution(); var removal = new Distribution();
        for (var example : data.validation().subList(0, Math.min(256, data.validation().size()))) {
            long[] b = example.board(); double base = value.applyAsDouble(b); long[] moves = new long[512]; int count = legal(b, moves);
            var swapped = b.clone(); swapped[4] ^= Board.PLAYER_BIT; reversal.add(base + value.applyAsDouble(swapped));
            // Geometry-only, off-manifold probes; never used as labels or searched.
            // Keep both kings and omit pawn rank/promotion complications.
            for(long occupied=b[0]|b[1]|b[2];occupied!=0;occupied&=occupied-1){
                int from=Long.numberOfTrailingZeros(occupied),code=Board.getSquare(b[0],b[1],b[2],b[3],from),type=code&7;
                if(type==1 || type==6)continue;
                var removed=b.clone();for(int word=0;word<4;word++)removed[word]&=~(1L<<from);removal.add(value.applyAsDouble(removed)-base);
                int file=from&7,rank=from>>>3;
                for(int delta:new int[]{1,-1,8,-8}){int to=from+delta;
                    if(to<0 || to>=64 || Math.abs((to&7)-file)+Math.abs((to>>>3)-rank)!=1 || ((b[0]|b[1]|b[2])&(1L<<to))!=0)continue;
                    var moved=removed.clone();for(int word=0;word<4;word++)if((code&(1<<word))!=0)moved[word]|=1L<<to;
                    relocation.add(value.applyAsDouble(moved)-base);break;
                }
                break;
            }
            for (int i = 0; i < count; i++) {
                long[] c = child(b, moves[i]); double difference = -value.applyAsDouble(c) - base;
                boolean promotion = ((moves[i] >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS) != 0;
                if (promotion) promotions.add(difference);
                if (pieces(c) < pieces(b)) {
                    captures.add(difference); var replies = new long[512]; int nc = legal(c, replies);
                    for (int j = 0; j < nc; j++) if (Move.toSquare(replies[j]) == Move.toSquare(moves[i])) {
                        var d = child(c, replies[j]); if (pieces(d) < pieces(c)) recaptures.add(value.applyAsDouble(d) - base);
                    }
                } else if (!promotion && !Board.isPlayerInCheck(c[0], c[1], c[2], c[3], Board.player((int)c[4]))) quiet.add(difference);
            }
        }
        double sink = 0; for (int repeat = 0; repeat < 4; repeat++) for (var e : data.validation()) sink += value.applyAsDouble(e.board());
        long start = System.nanoTime(); for (int repeat = 0; repeat < 4; repeat++) for (var e : data.validation()) sink += value.applyAsDouble(e.board());
        double ns = (System.nanoTime()-start) / (4.0 * data.validation().size());
        var searches = new ArrayList<Map<String,Object>>(); int roots = args.length > 2 ? Integer.parseInt(args[2]) : 16;
        var qsearches = new ArrayList<Map<String,Object>>();
        int depth = args.length > 3 ? Integer.parseInt(args[3]) : 3;
        for (int i = 0; i < roots; i++) {
            long[] b = data.validation().get(i).board(); long begun = System.nanoTime();
            var control = SearchControl.controlled(2_000_000, begun, 2_000_000_000L, TimeSource.SYSTEM);
            var adapter = production ? new ExactSearchAdapter(SearchEvaluation.brn3(productionModel),new TTable(4)) : nnue == null ? new ExactSearchAdapter((board, ply) -> score(value.applyAsDouble(board)), new TTable(4))
                    : new ExactSearchAdapter(SearchEvaluation.incremental(nnue), new TTable(4));
            try (var driver = new SearchDriver(adapter)) {
                var result = driver.search(new SearchRequest(b, GameHistory.initial(b), depth, SearchObserver.NONE, control, true));
                searches.add(Map.of("index", i, "completed", result.targetDepthCompleted(), "terminal", result.terminalRoot(), "nodes", control.nodes(),
                        "qnodes", result.diagnostics().worker().nodes().qNodes(), "seconds", (System.nanoTime()-begun)/1e9));
            }
            long qbegun = System.nanoTime();
            var q = ExactSearch.quiescenceResearch((board, ply) -> nnueScore(candidate, value.applyAsDouble(board)));
            var qr = q.search(b, GameHistory.initial(b), depth, () -> q.visitedNodes() >= 2_000_000 || System.nanoTime() - qbegun >= 2_000_000_000L);
            qsearches.add(Map.of("index", i, "completed", qr.completed(), "nodes", qr.nodes(), "qnodes", qr.qnodes(), "maximumQply", qr.maximumQply(), "seconds", (System.nanoTime()-qbegun)/1e9));
        }
        var report = new LinkedHashMap<String,Object>(); report.put("candidate", candidate); report.put("deltaUnits", nnue == null ? "pawns" : "bounded-outcome (not CP)");
        report.put("datasetPayloadSha256",manifest.get("payloadSha256"));
        report.put("checkpointSha256",BrnResearchComparison.digest(run.resolve(candidate.equals("nnue")?"selected.nnue":"selected.state")));
        report.put("quiet", quiet.report()); report.put("capture", captures.report()); report.put("recapture", recaptures.report()); report.put("stmReversalSum", reversal.report());
        report.put("promotion", promotions.report());
        report.put("geometryRelocationSameStm",relocation.report());report.put("pieceRemovalSameStm",removal.report());
        report.put("geometryProbeNote","Nonking nonpawn one-piece probes may be off the legal-position manifold; no labels or search use");
        if(!cached&&!production)report.put("fullRecomputeNs", ns);
        if(production)report.put("productionModelSha256",BrnResearchComparison.digest(run.resolve("production-preview/network.brn3")));
        report.put("evaluationNsOnUnrelatedPositions",ns);report.put("inferenceMode",production?"BRN-3 production model/workspace/search state":compact?"rolling cache with float32 weights":cached?"rolling placement cache":"full recomputation");report.put("sink", sink); report.put("searchDepth", depth); report.put("searches", searches);
        report.put("productionQsearch", "disabled in current production; qnodes zero is not a qsearch stability result");
        report.put("researchQsearch", qsearches);
        Path output=run.resolve(args.length>5?args[5]:compact?"diagnostics-compact.json":cached?"diagnostics-cached.json":"diagnostics.json");
        if(args.length>5&&Files.exists(output))throw new IOException("Use a new diagnostic output");
        DataFiles.write(output, report); System.out.println(DataFiles.JSON.toJson(report));
    }
    static int nnueScore(String candidate, double raw) {
        return candidate.equals("nnue") ? NnueScoreMapping.V1.map(raw) : score(raw);
    }
    private BrnResearchDiagnostics() {}
}
