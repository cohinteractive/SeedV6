package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.training.service.LearningArenaConfig;
import com.ohinteractive.seedv6.training.service.LearningArenaState;
import com.ohinteractive.seedv6.training.selfplay.GameTermination;
import com.ohinteractive.seedv6.training.validation.ValidationResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Cheap projection of durable game receipts. All cumulative records use competitor A.
 * A pair enters the score only when its second game has completed successfully. */
record ArenaMatchSummary(int wins, int draws, int losses, int games, int pairs, int unscoredPairs,
                         List<GameRow> history) {
    record GameRow(int game, int pair, boolean firstIsWhite, String result, String winner,
                   int plies, String material, String cumulativeWdl, String cumulativeScore, String termination) {}
    static ArenaMatchSummary from(LearningArenaConfig config, LearningArenaState.Round round) {
        int wins = 0, draws = 0, losses = 0, games = 0, pairs = 0, unscored = 0;
        var rows = new ArrayList<GameRow>();
        for (int p = 0; p < round.pairs().size(); p++) {
            var pair = round.pairs().get(p);
            for (int side = 0; side < 2; side++) {
                var game = side == 0 ? pair.candidateWhite() : pair.candidateBlack();
                if (!settled(game)) continue;
                games++;
                if (side == 1 && settled(pair.candidateWhite())) {
                    pairs++;
                    if (pair.valid()) {
                        for (double score : new double[]{pair.candidateWhite().score(Value.WHITE), pair.candidateBlack().score(Value.BLACK)}) {
                            if (score == 1) wins++; else if (score == .5) draws++; else losses++;
                        }
                    } else unscored++;
                }
                boolean completed = game.termination().completed();
                double score = completed ? game.score(side == 0 ? Value.WHITE : Value.BLACK) : -1;
                String result = !completed ? "Unscored" : game.score(Value.WHITE) == 1 ? "1-0" : game.score(Value.WHITE) == 0 ? "0-1" : "1/2-1/2";
                String winner = !completed ? "\u2014" : score == .5 ? "Draw" : label(config, score == 1);
                String reason = game.termination().name().toLowerCase(Locale.ROOT).replace('_', ' ');
                if (side == 0) reason += " (pair pending)";
                rows.add(new GameRow(p * 2 + side + 1, p + 1, side == 0, result, winner, game.plies(),
                        game.finalMaterial() == null ? "\u2014" : game.finalMaterial().white() + " / " + game.finalMaterial().black(),
                        wdl(wins, draws, losses), scores(wins, draws, losses, false), reason));
            }
        }
        return new ArenaMatchSummary(wins, draws, losses, games, pairs, unscored, List.copyOf(rows));
    }
    static boolean settled(ValidationResult.Game game) {
        return game.termination().completed() || game.termination() == GameTermination.PLY_CAP;
    }
    static String label(LearningArenaConfig config, boolean first) {
        var c = first ? config.a() : config.b();
        // Architecture is concise; A/B disambiguate independent models of the same architecture.
        String name = c.architecture() == com.ohinteractive.seedv6.training.model.TrainingArchitecture.NNUE_MATERIAL
                ? "NNUE" : c.displayName();
        if (c.architecture() == com.ohinteractive.seedv6.training.model.TrainingArchitecture.NNUE) name = "NNUE legacy";
        return name + (config.a().architecture() == config.b().architecture() ? first ? " A" : " B" : "");
    }
    static String wdl(int wins, int draws, int losses) { return wins + "-" + draws + "-" + losses; }
    static String percent(double score) { return String.format(Locale.ROOT, "%.1f%%", score * 100); }
    static String scores(int wins, int draws, int losses, boolean reverse) {
        int games = wins + draws + losses;
        if (games == 0) return "\u2014 / \u2014";
        double score = (wins + .5 * draws) / games;
        return percent(reverse ? 1 - score : score) + " / " + percent(reverse ? score : 1 - score);
    }
    String score(boolean first) {
        int count = wins + draws + losses;
        return count == 0 ? "\u2014" : percent(((first ? wins : losses) + .5 * draws) / count);
    }
}
