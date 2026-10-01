package com.ohinteractive.seedv6.tools.search;

import java.nio.file.Path;
import java.util.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.model.TrainingArchitecture;
import com.ohinteractive.seedv6.training.service.*;
import com.ohinteractive.seedv6.training.validation.PromotionPolicy;

/** Strict CLI: unknown/duplicate options and unsupported combinations are errors. */
final class BrnDiagnosticOptions {
    static final String HELP = """
        SeedV6 headless BRN diagnostics (JSONL + summary.txt; fresh output under build/)
        brn-diagnostic replay|train --seed LONG [options]
          --architecture BRN-2          only supported architecture
          --engine production|legacy    replay only; default production
          --depth N                     default 2; fixed target, no time termination
          --workers N                   1..16, default 1
            production ignores workers (effective 1, no qsearch); legacy replay uses them
          --fen FEN                     repeatable replay positions, quote all six fields
          --fen-file PATH               UTF-8, one FEN/line; blank/# lines ignored
          --model PATH                  replay saved .brn2 model instead of virgin seed
          --output PATH                 NEW directory beneath repository build/;
                                        otherwise a unique build/brn-diagnostics/run-* directory
          --outlier-ms N                default 1000; reporting only
          --outlier-qratio X            qnodes/max(1,ordinary nodes), default 100
          --outlier-nodes N             total nodes, default 1000000; reporting only
        Training settings (ordinary TrainerService; never resumes/reads normal stores):
          --generations N               default 1
          --games N                     games/generation, default 256
          --source self-play|handcrafted default self-play (explicit BRN-generated workload)
          --validation game-pairs|held-out  default game-pairs; held-out needs >=4 sampled games
          --opening-min N --opening-max N  default 0,8
          --max-plies N --samples N      default 1024,32
          --learning-rate X --shuffle true|false  default 0.001,true
          --validation-pairs N          default 64
          --validation-depth N          default training depth
          --validation-opening-min N --validation-opening-max N  default training openings
          --validation-max-plies N      default training ply cap
          --promotion-min-pairs N       default validation pairs
          --promotion-alpha X --promotion-margin X  default 0.05,0
          --starting-fen FEN             default normal chess
        Fixed training settings: WDL targets, capture consistency OFF, epochs=1, minibatch=1,
        no teacher, no node/time safety limits, no resume. Outlier thresholds NEVER abort searches.
        Reports include generation seeds and indexed game seeds. No GUI is constructed.
        """;
    final Map<String, String> values = new LinkedHashMap<>();
    final List<String> fens = new ArrayList<>();
    final String mode;
    private static final Set<String> COMMON = Set.of("seed", "architecture", "depth", "workers", "output",
            "outlier-ms", "outlier-qratio", "outlier-nodes");
    private static final Set<String> REPLAY = Set.of("engine", "fen", "fen-file", "model");
    private static final Set<String> TRAIN = Set.of("generations", "games", "source", "validation", "opening-min",
            "opening-max", "max-plies", "samples", "learning-rate", "shuffle", "validation-pairs", "validation-depth",
            "validation-opening-min", "validation-opening-max", "validation-max-plies", "promotion-min-pairs",
            "promotion-alpha", "promotion-margin", "starting-fen");
    BrnDiagnosticOptions(String[] args) {
        if (args.length == 0 || !Set.of("replay", "train").contains(args[0]))
            throw new IllegalArgumentException("Expected replay or train; use --help.");
        mode = args[0];
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("--")) throw new IllegalArgumentException("Expected option: " + arg);
            int equal = arg.indexOf('=');
            String key = arg.substring(2, equal < 0 ? arg.length() : equal);
            if (!COMMON.contains(key) && !(mode.equals("train") ? TRAIN : REPLAY).contains(key))
                throw new IllegalArgumentException("Unknown or inapplicable option: " + key);
            String value;
            if (equal >= 0) value = arg.substring(equal + 1);
            else if (++i < args.length) value = args[i];
            else throw new IllegalArgumentException("Missing value: " + key);
            if (key.equals("fen")) fens.add(value);
            else if (values.putIfAbsent(key, value) != null) throw new IllegalArgumentException("Duplicate option: " + key);
        }
        if (!values.containsKey("seed")) throw new IllegalArgumentException("An explicit --seed is required.");
        seed();
        choice("architecture", "BRN-2", "BRN-2");
        choice("engine", "production", "production", "legacy");
        choice("source", "self-play", "self-play", "handcrafted");
        choice("validation", "game-pairs", "game-pairs", "held-out");
        choice("shuffle", "true", "true", "false");
        if (depth() < 1 || depth() > 64 || workers() < 1 || workers() > 16
                || integer("generations", 1) < 1 || decimal("outlier-ms", 1000) < 0
                || decimal("outlier-qratio", 100) < 0 || longValue("outlier-nodes", 1000000) < 1)
            throw new IllegalArgumentException("Invalid diagnostic bounds.");
        config(Path.of("build", "unused-diagnostic-validation")); // validate all production settings before output mutation
        fens.forEach(Board::fromFen);
    }
    String get(String key, String fallback) { return values.getOrDefault(key, fallback); }
    int integer(String key, int fallback) { return Integer.parseInt(get(key, "" + fallback)); }
    long longValue(String key, long fallback) { return Long.parseLong(get(key, "" + fallback)); }
    double decimal(String key, double fallback) {
        double value = Double.parseDouble(get(key, "" + fallback));
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Nonfinite " + key);
        return value;
    }
    String choice(String key, String fallback, String... choices) {
        String value = get(key, fallback);
        if (!List.of(choices).contains(value)) throw new IllegalArgumentException("Unsupported " + key + ": " + value);
        return value;
    }
    long seed() { return Long.parseLong(values.get("seed")); }
    int depth() { return integer("depth", 2); }
    int workers() { return integer("workers", 1); }
    boolean legacy() { return get("engine", "production").equals("legacy"); }
    TrainerConfig config(Path store) {
        int min = integer("opening-min", 0), max = integer("opening-max", 8), cap = integer("max-plies", 1024);
        int pairs = integer("validation-pairs", 64);
        return new TrainerConfig(store, seed(),
                new TrainerConfig.SelfPlay(depth(), workers(), integer("games", 256), min, max,
                        integer("samples", 32), cap, NnueScoreMapping.V1),
                new TrainerConfig.Training(1, 1, Boolean.parseBoolean(get("shuffle", "true"))),
                new TrainerConfig.Validation(pairs, integer("validation-opening-min", min), integer("validation-opening-max", max),
                        integer("validation-depth", depth()), workers(), integer("validation-max-plies", cap), NnueScoreMapping.V1,
                        new PromotionPolicy(integer("promotion-min-pairs", pairs), decimal("promotion-alpha", .05),
                                decimal("promotion-margin", 0))), integer("generations", 1), TrainerConfig.DepthChange.REQUIRE_SAME,
                get("starting-fen", Board.FEN_STARTING_POSITION), TrainingArchitecture.BRN2,
                decimal("learning-rate", .001), get("source", "self-play").equals("self-play")
                        ? TrainingSource.SELF_PLAY : TrainingSource.HANDCRAFTED,
                BrnSupervision.WDL, new BrnRunSeeds(seed(), seed()), "", 0, "",
                get("validation", "game-pairs").equals("game-pairs") ? ValidationMethod.GAME_PAIRS : ValidationMethod.HELD_OUT,
                BrnCaptureConsistency.OFF);
    }
}
