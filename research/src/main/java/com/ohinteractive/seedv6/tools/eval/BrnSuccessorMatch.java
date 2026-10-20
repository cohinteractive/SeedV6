package com.ohinteractive.seedv6.tools.eval;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Fen;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.SearchDriver;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.training.validation.*;
import com.ohinteractive.seedv6.training.selfplay.HeadlessGame;
import com.ohinteractive.seedv6.core.move.LegalMoveResolver;
import com.ohinteractive.seedv6.core.move.MoveIntent;
import java.nio.file.*;
import java.util.*;

/** Single-worker paired diagnostic with immutable per-game receipts and explicit equal search. */
public final class BrnSuccessorMatch {
    record Model(String name, String root, String checkpoint, String networkSha256) {}
    record Plan(String id, List<Model> models, int pairs, long seed, long nodes, long moveMillis,
                int maximumPlies, int batchGames, int batchSeconds, List<List<String>> openingMoves, boolean legalFallback) {
        Plan {
            if (models == null || models.size() < 2 || pairs < 1 || batchGames < 1 || batchSeconds < 1
                    || (nodes > 0) == (moveMillis > 0) || maximumPlies < 1)
                throw new IllegalArgumentException("Invalid bounded match plan");
            if (openingMoves != null && openingMoves.size() != pairs) throw new IllegalArgumentException("Opening count");
        }
    }
    record Move(int player, long move, Integer score, int depth, long nodes, long elapsedNanos, String fenAfter, boolean fallback) {}
    record Receipt(String planHash, String candidate, String reference, int pair, int colour,
                   String openingHash, String openingFen, ValidationResult.Game result, List<Move> moves) {}
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("PLAN OUTPUT_DIRECTORY (same plan resumes completed games)");
        Path planFile = Path.of(args[0]), output = Path.of(args[1]);
        var plan = DataFiles.read(planFile, Plan.class);
        String hash = DataFiles.hash(Files.readString(planFile));
        Files.createDirectories(output);
        Path binding = output.resolve("plan.sha256");
        if (Files.exists(binding)) {
            if (!Files.readString(binding).equals(hash)) throw new IllegalStateException("Different resume plan");
        } else Files.writeString(binding, hash, StandardOpenOption.CREATE_NEW);
        var models = new ArrayList<SearchEvaluation>();
        for (var spec : plan.models()) {
            var snapshot = CheckpointStore.readSnapshot(Path.of(spec.root()), spec.checkpoint());
            if (!snapshot.manifest().networkSha256().equals(spec.networkSha256())) throw new IllegalStateException("Model hash changed");
            models.add(snapshot.model().evaluation(NnueScoreMapping.V1));
        }
        var config = new ValidationConfig(plan.pairs(), plan.seed(), 4, 12, 64, 1, NnueScoreMapping.V1, plan.maximumPlies());
        if (plan.moveMillis() > 0) for (var evaluation : models) for (int i=0;i<4;i++)
            try (var warmup = new SearchDriver(BrnSuccessorAudit.common(evaluation))) {
                var opening=opening(plan,config,i%plan.pairs());
                warmup.search(new SearchRequest(opening.board(), opening.history(), 64,
                        SearchControl.controlled(32768,System.nanoTime(),2_000_000_000L,TimeSource.SYSTEM)));
            }
        long start = System.nanoTime(); int played = 0;
        // Alternate candidates within an opening to reduce host/time-order confounding.
        outer: for (int pair = 0; pair < plan.pairs(); pair++) {
            var opening = opening(plan,config,pair);
            for (int candidate = 1; candidate < models.size(); candidate++) for (int colour = 0; colour < 2; colour++) {
                String name = String.format(Locale.ROOT, "p%03d-m%d-c%d.json", pair, candidate, colour);
                Path file = output.resolve(name);
                if (Files.exists(file)) {
                    var prior = DataFiles.read(file, Receipt.class);
                    if (!prior.planHash().equals(hash) || !prior.openingHash().equals(opening.identity())
                            || prior.pair() != pair || prior.colour() != colour
                            || !prior.candidate().equals(plan.models().get(candidate).checkpoint())
                            || !prior.reference().equals(plan.models().getFirst().checkpoint()))
                        throw new IllegalStateException("Receipt binding mismatch: " + file);
                    continue;
                }
                if (played >= plan.batchGames() || System.nanoTime() - start > plan.batchSeconds() * 1_000_000_000L) break outer;
                var game = opening.newGame(plan.maximumPlies());
                var trace = new ArrayList<Move>();
                try (var a = new SearchDriver(BrnSuccessorAudit.common(models.get(candidate)));
                     var b = new SearchDriver(BrnSuccessorAudit.common(models.getFirst()))) {
                    while (game.active()) {
                        int player = game.sideToMove();
                        var control = SearchControl.controlled(plan.nodes() > 0 ? plan.nodes() : -1, System.nanoTime(),
                                plan.moveMillis() > 0 ? plan.moveMillis() * 1_000_000 : 2_000_000_000L, TimeSource.SYSTEM);
                        var outcome = (player == colour ? a : b).search(new SearchRequest(
                                game.boardSnapshot(), game.historySnapshot(), 64, control));
                        var result = outcome.lastCompletedResult();
                        if (result == null && plan.legalFallback()
                                && (control.termination()==SearchTermination.TIME_LIMIT || control.termination()==SearchTermination.NODE_LIMIT)) {
                            long move=game.legalMoves()[0];game.play(move);
                            trace.add(new Move(player,move,null,0,control.nodes(),control.elapsedNanos(),Fen.fromBoard(game.boardSnapshot()),true));
                            continue; // Same legal-only fallback as UCI; never invent a searched score/depth.
                        }
                        if (result == null || !result.completed() || !result.hasMove()
                                || plan.nodes() > 0 && control.termination() == SearchTermination.TIME_LIMIT)
                            throw new IllegalStateException("Search did not produce a usable bounded iteration: termination="
                                    + control.termination() + ", nodes=" + control.nodes() + ", elapsedNanos="
                                    + control.elapsedNanos() + ", result=" + result + ", fen=" + Fen.fromBoard(game.boardSnapshot()));
                        game.play(result.bestMove());
                        trace.add(new Move(player, result.bestMove(), result.score(), result.depth(), control.nodes(),
                                control.elapsedNanos(), Fen.fromBoard(game.boardSnapshot()),false));
                    }
                } catch (RuntimeException failure) {
                    DataFiles.write(output.resolve(name + ".failed"), Map.of("failure", failure.toString(), "moves", trace,
                            "planHash", hash, "pair", pair, "colour", colour));
                    throw failure; // Never silently exclude a failure and continue selection.
                }
                var receipt = new Receipt(hash, plan.models().get(candidate).checkpoint(), plan.models().getFirst().checkpoint(),
                        pair, colour, opening.identity(), Fen.fromBoard(opening.board()),
                        new ValidationResult.Game(game.termination(), game.playedPlies()), List.copyOf(trace));
                DataFiles.write(file, receipt);
                System.out.println(name + " " + game.termination() + " plies=" + game.playedPlies());
                played++;
            }
        }
        var summaries = new ArrayList<Object>();
        for (int candidate = 1; candidate < models.size(); candidate++) {
            int wins=0, draws=0, losses=0, missing=0, incomplete=0;
            var pairScores = new ArrayList<Double>();
            for (int pair=0; pair<plan.pairs(); pair++) {
                var white = output.resolve(String.format(Locale.ROOT, "p%03d-m%d-c0.json", pair, candidate));
                var black = output.resolve(String.format(Locale.ROOT, "p%03d-m%d-c1.json", pair, candidate));
                if (!Files.exists(white) || !Files.exists(black)) { missing++; continue; }
                var w=DataFiles.read(white, Receipt.class); var b=DataFiles.read(black, Receipt.class);
                if (!w.result().termination().completed() || !b.result().termination().completed()) { incomplete++; continue; }
                double ws=w.result().score(Value.WHITE), bs=b.result().score(Value.BLACK);
                for (double score : new double[]{ws,bs}) { if (score==1) wins++; else if(score==0) losses++; else draws++; }
                pairScores.add((ws+bs)/2);
            }
            summaries.add(Map.of("candidate",plan.models().get(candidate).name(),"wins",wins,"draws",draws,"losses",losses,
                    "missingPairs",missing,"incompletePairs",incomplete,"pairScores",pairScores));
        }
        DataFiles.write(output.resolve("summary.json"), Map.of("planHash",hash,"models",summaries,
                "java",System.getProperty("java.runtime.version"),"activeProcessors",Runtime.getRuntime().availableProcessors()));
    }
    static ValidationArena.Opening opening(Plan plan,ValidationConfig config,int pair) {
        var board=Board.startingPosition();
        if(plan.openingMoves()==null)return ValidationArena.opening(board,GameHistory.initial(board),config,pair);
        var game=new HeadlessGame(board,1024);var resolver=new LegalMoveResolver();
        for(String uci:plan.openingMoves().get(pair)) {
            if(!uci.matches("[a-h][1-8][a-h][1-8][qrbn]?"))throw new IllegalArgumentException("Invalid opening UCI");
            var promotion=uci.length()==4?MoveIntent.Promotion.NONE:switch(uci.charAt(4)) {
                case 'q'->MoveIntent.Promotion.QUEEN;case 'r'->MoveIntent.Promotion.ROOK;
                case 'b'->MoveIntent.Promotion.BISHOP;case 'n'->MoveIntent.Promotion.KNIGHT;default->throw new IllegalArgumentException();};
            int from=uci.charAt(0)-'a'+8*(uci.charAt(1)-'1'),to=uci.charAt(2)-'a'+8*(uci.charAt(3)-'1');
            game.play(resolver.resolve(game.boardSnapshot(),new MoveIntent(from,to,promotion)));
        }
        if(!game.active())throw new IllegalArgumentException("Terminal opening");
        return new ValidationArena.Opening(game.boardSnapshot(),game.historySnapshot(),game.playedPlies());
    }
    private BrnSuccessorMatch() {}
}
