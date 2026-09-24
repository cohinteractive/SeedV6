package com.ohinteractive.seedv6.tools.search;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.driver.ExactSearchAdapter;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.search.manage.*;
import com.ohinteractive.seedv6.tools.nnue.research.StrengthArena;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.validation.*;

import static com.ohinteractive.seedv6.tools.search.DiagnosticReport.*;

/** Offline immutable actors; no training/store writer, GUI, or promotion operation. */
public final class Brn2StrengthScreen {
    static final long MOVE_WATCHDOG_MS = 5000;
    static final String[] IDS = {"brn50", "brn75", "brn100", "nnue74"};
    static final int[][] MATCHES = {{0, 1}, {0, 2}, {0, 3}, {1, 2}, {1, 3}, {2, 3}};

    record Actor(String id, NetworkModel model, Map<String, Object> identity) {}
    record Config(int pairs, long seed, int openingPlies, long moveMillis, int maximumPlies) {
        Config {
            if (pairs < 1 || pairs > 64 || moveMillis < 1 || moveMillis > 1000)
                throw new IllegalArgumentException("Invalid screen resources.");
            new ValidationConfig(pairs, seed, openingPlies, openingPlies, 4, 1, NnueScoreMapping.V1, maximumPlies);
        }
        // Depth is required by the opening/statistics carrier, but never used for timed search.
        ValidationConfig validation(int count) {
            return new ValidationConfig(count, seed, openingPlies, openingPlies, 4, 1, NnueScoreMapping.V1, maximumPlies);
        }
        SearchLimits limits() { return new SearchLimits(0, -1, moveMillis, false); }
    }

    static Actor load(String id, String path, TrainingArchitecture architecture, String expectedHash) throws Exception {
        if (path.equals("initialized")) throw new IllegalArgumentException("Trained checkpoint required.");
        var loaded = Brn2Diagnostics.load(path, architecture);
        if (!Objects.equals(expectedHash, loaded.identity().get("modelSha256")))
            throw new IOException("Accepted model hash mismatch: " + id);
        var identity = new LinkedHashMap<>(loaded.identity());
        identity.put("actor", id);
        return new Actor(id, loaded.model(), identity);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3 || !Set.of("verify", "smoke", "screen").contains(args[0]))
            throw new IllegalArgumentException("verify|smoke|screen config.properties NEW-output.jsonl");
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(Path.of(args[1]), StandardCharsets.UTF_8)) { properties.load(reader); }
        Config config = new Config(Integer.parseInt(properties.getProperty("pairs")),
                Long.parseLong(properties.getProperty("seed")), Integer.parseInt(properties.getProperty("openingPlies")),
                Long.parseLong(properties.getProperty("moveMillis")), Integer.parseInt(properties.getProperty("maximumPlies")));
        List<Actor> actors = new ArrayList<>();
        // All four models must load and match before the first search/game.
        for (String id : IDS) actors.add(load(id, properties.getProperty(id + ".path"),
                id.equals("nnue74") ? TrainingArchitecture.NNUE : TrainingArchitecture.BRN2,
                properties.getProperty(id + ".sha256")));
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(Path.of(args[2]), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE))) {
            write(out, "type", "configuration", "mode", args[0], "startedUtc", Instant.now(),
                    "pairs", config.pairs(), "seed", config.seed(), "openingPlies", config.openingPlies(),
                    "moveMillis", config.moveMillis(), "maximumPlies", config.maximumPlies(), "threads", 1,
                    "search", "R003-exact", "ttEntries", 0, "diagnostics", true, "moveWatchdogMillis", MOVE_WATCHDOG_MS,
                    "searchLimits", fields("depth", 0, "nodes", -1, "timeMillis", config.moveMillis()),
                    "java", System.getProperty("java.runtime.version"), "os", System.getProperty("os.name"));
            for (Actor actor : actors) write(out, "type", "identity", "identity", actor.identity());
            if (!args[0].equals("verify")) {
                warmup(actors, config, out);
                if (args[0].equals("smoke")) {
                    var opening = openings(config).getFirst();
                    var pair = playPair("smoke", 0, opening, actors.getFirst(), actors.getLast(), config, out, Brn2StrengthScreen::player);
                    if (!pair.valid()) throw new IllegalStateException("Smoke pair incomplete.");
                    summarize("smoke", List.of(pair), config, out);
                } else run(actors, config, out);
            }
            write(out, "type", "end", "completed", true, "finishedUtc", Instant.now());
        }
    }

    static List<ValidationArena.Opening> openings(Config config) {
        long[] board = Board.startingPosition();
        GameHistory history = GameHistory.initial(board);
        List<ValidationArena.Opening> result = new ArrayList<>();
        Set<String> unique = new HashSet<>();
        for (int i = 0; i < config.pairs(); i++) {
            var opening = ValidationArena.opening(board, history, config.validation(config.pairs()), i);
            var repeat = ValidationArena.opening(board, history, config.validation(config.pairs()), i);
            if (!opening.identity().equals(repeat.identity()) || !unique.add(opening.identity()))
                throw new IllegalStateException("Non-reproducible or duplicate opening: " + i);
            if (!opening.newGame(config.maximumPlies()).active()) throw new IllegalStateException("Terminal opening: " + i);
            result.add(opening);
        }
        return List.copyOf(result);
    }

    private static void run(List<Actor> actors, Config config, PrintWriter out) throws Exception {
        var openings = openings(config);
        List<List<ValidationResult.Pair>> results = new ArrayList<>();
        for (int i = 0; i < MATCHES.length; i++) results.add(new ArrayList<>());
        for (int i = 0; i < openings.size(); i++) {
            var opening = openings.get(i);
            List<Long> history = new ArrayList<>();
            for (int h = 0; h < opening.history().size(); h++) history.add(opening.history().keyAt(h));
            write(out, "type", "opening", "index", i, "hash", opening.identity(),
                    "seed", SelfPlayRunner.gameSeed(config.seed(), i), "plies", opening.randomizedPlies(),
                    "board", Arrays.stream(opening.board()).boxed().toList(), "history", history);
            // Rotate pairing order, and alternate the first colour, to distribute run-order effects.
            for (int offset = 0; offset < MATCHES.length; offset++) {
                int m = (i + offset) % MATCHES.length;
                Actor a = actors.get(MATCHES[m][0]), b = actors.get(MATCHES[m][1]);
                String name = a.id() + "-" + b.id();
                var pair = playPair(name, i, opening, a, b, config, out, Brn2StrengthScreen::player);
                results.get(m).add(pair);
                if (!pair.valid()) {
                    summarize(name, results.get(m), config, out);
                    throw new IllegalStateException("Invalid pairing; stopped without replacing or scoring incomplete games: " + name);
                }
                System.out.println(Instant.now() + " " + name + " opening=" + i + " pairScore=" + pair.score());
            }
        }
        for (int m = 0; m < MATCHES.length; m++)
            summarize(actors.get(MATCHES[m][0]).id() + "-" + actors.get(MATCHES[m][1]).id(), results.get(m), config, out);
    }

    interface Player extends AutoCloseable {
        ManagedSearchResult move(HeadlessGame game, Config config) throws Exception;
        @Override default void close() {}
    }
    @FunctionalInterface interface Factory { Player create(Actor actor); }

    static Player player(Actor actor) {
        var evaluation = actor.model().evaluation(NnueScoreMapping.V1);
        var service = new SearchLifecycleService(TimeSource.SYSTEM, () -> new ExactSearchAdapter(evaluation));
        return new Player() {
            public ManagedSearchResult move(HeadlessGame game, Config config) throws Exception {
                CompletableFuture<ManagedSearchResult> result = new CompletableFuture<>();
                service.start(game.boardSnapshot(), game.historySnapshot(), config.limits(), SearchObserver.NONE, true, result::complete);
                return result.get(MOVE_WATCHDOG_MS, TimeUnit.MILLISECONDS);
            }
            public void close() {
                service.close();
                if (!service.isTerminated()) throw new IllegalStateException("Search worker did not terminate.");
            }
        };
    }

    static ValidationResult.Pair playPair(String name, int index, ValidationArena.Opening opening,
            Actor a, Actor b, Config config, PrintWriter out, Factory factory) throws Exception {
        ValidationResult.Game white, black;
        if ((index & 1) == 0) {
            white = play(name, index, "A-white", opening, a, b, config, out, factory);
            black = play(name, index, "A-black", opening, b, a, config, out, factory);
        } else {
            black = play(name, index, "A-black", opening, b, a, config, out, factory);
            white = play(name, index, "A-white", opening, a, b, config, out, factory);
        }
        var pair = new ValidationResult.Pair(opening.identity(), white, black);
        write(out, "type", "pair", "match", name, "opening", index, "openingHash", opening.identity(),
                "aWhite", white.termination(), "aBlack", black.termination(), "valid", pair.valid(),
                "score", pair.valid() ? pair.score() : null);
        return pair;
    }

    static ValidationResult.Game play(String name, int index, String colour, ValidationArena.Opening opening,
            Actor white, Actor black, Config config, PrintWriter out, Factory factory) throws Exception {
        HeadlessGame game = opening.newGame(config.maximumPlies());
        String id = name + "/" + index + "/" + colour;
        write(out, "type", "game_start", "game", id, "match", name, "opening", index,
                "openingHash", opening.identity(), "white", white.id(), "black", black.id());
        String error = null;
        try (Player w = factory.create(white); Player b = factory.create(black)) {
            while (game.active()) {
                String actor = game.sideToMove() == Value.WHITE ? white.id() : black.id();
                write(out, "type", "move_start", "game", id, "ply", game.playedPlies(), "actor", actor);
                long start = System.nanoTime();
                ManagedSearchResult result = (game.sideToMove() == Value.WHITE ? w : b).move(game, config);
                long elapsed = System.nanoTime() - start;
                var nodes = result.diagnostics().worker().nodes();
                var completed = result.lastCompletedResult();
                write(out, "type", "move", "game", id, "ply", game.playedPlies(), "actor", actor,
                        "move", Move.string(result.bestMove()), "encodedMove", result.bestMove(),
                        "nodes", result.nodes(), "mainNodes", nodes.mainNodes(), "qNodes", nodes.qNodes(),
                        "evaluationCalls", nodes.evaluationCalls(), "elapsedNs", elapsed,
                        "completedDepth", completed == null ? 0 : completed.depth(),
                        "score", completed == null ? null : completed.score(), "fallback", false,
                        "noCompletedResult", completed == null,
                        "termination", result.termination(), "failure", result.failure() == null ? null : result.failure().toString());
                requireUsable(result);
                game.play(result.bestMove());
            }
        } catch (Exception failure) {
            error = failure.toString();
            game.abort(GameTermination.SEARCH_FAILURE, error);
        }
        // A close failure after mate still invalidates the evidence, as in ValidationArena.
        GameTermination termination = error == null ? game.termination() : GameTermination.SEARCH_FAILURE;
        write(out, "type", "game", "game", id, "match", name, "opening", index,
                "openingHash", opening.identity(), "white", white.id(), "black", black.id(),
                "termination", termination, "plies", game.playedPlies(), "error", error,
                "finalStateHash", ValidationArena.stateHash(game.boardSnapshot(), game.historySnapshot()));
        return new ValidationResult.Game(termination, game.playedPlies());
    }

    static void requireUsable(ManagedSearchResult result) {
        if (result.failure() != null || !result.hasMove() || result.lastCompletedResult() == null
                || !(result.termination() == SearchTermination.TIME_LIMIT || result.termination() == SearchTermination.COMPLETED))
            throw new IllegalStateException("Unusable managed search: " + result.termination(), result.failure());
        if (!result.diagnostics().enabled() || result.nodes() != result.diagnostics().totalEnteredNodes())
            throw new IllegalStateException("Search node accounting mismatch.");
    }

    private static void warmup(List<Actor> actors, Config config, PrintWriter out) throws Exception {
        for (int round = 0; round < 5; round++) for (Actor actor : actors) {
            try (Player player = player(actor)) {
                HeadlessGame game = new HeadlessGame(Board.startingPosition(), config.maximumPlies());
                var result = player.move(game, config);
                requireUsable(result);
                write(out, "type", "warmup", "round", round, "actor", actor.id(), "nodes", result.nodes(),
                        "completedDepth", result.lastCompletedResult() == null ? 0 : result.lastCompletedResult().depth());
            }
        }
    }

    static void summarize(String name, List<ValidationResult.Pair> pairs, Config config, PrintWriter out) {
        long[] board = Board.startingPosition();
        var result = new ValidationResult(config.validation(pairs.size()),
                ValidationArena.stateHash(board, GameHistory.initial(board)), pairs);
        var s = result.statistics();
        var e = StrengthArena.assess(s, 64, 0.05);
        var p = result.assess(PromotionPolicy.DEFAULT);
        write(out, "type", "summary", "match", name, "validPairs", s.validPairs(), "incompletePairs", s.incompletePairs(),
                "wins", s.wins(), "draws", s.draws(), "losses", s.losses(), "score", finite(e.mean()),
                "white", colour(s.white()), "black", colour(s.black()), "totalPlies", s.totalPlies(),
                "terminations", s.terminations(), "confidence", fields("alpha", 0.05, "radius", finite(e.radius()),
                        "lower", e.lower(), "upper", e.upper(), "conclusion", e.conclusion()),
                "informationalPromotionFormula", fields("radius", finite(p.radius()), "lower", finite(p.lowerBound()),
                        "threshold", p.threshold(), "decision", p.decision()));
    }
    private static Map<String, Object> colour(ValidationResult.ColourRecord c) {
        return fields("wins", c.wins(), "draws", c.draws(), "losses", c.losses());
    }
    private static Double finite(double number) { return Double.isFinite(number) ? number : null; }
    private Brn2StrengthScreen() {}
}
