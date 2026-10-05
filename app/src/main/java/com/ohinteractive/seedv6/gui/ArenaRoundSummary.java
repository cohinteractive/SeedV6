package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.training.service.LearningArenaConfig;
import com.ohinteractive.seedv6.training.service.LearningArenaState;
import java.util.Locale;

/** Presentation over the existing paired-game evidence; never a promotion or inferred result. */
record ArenaRoundSummary(int round, String first, String second, int wins, int draws, int losses,
        int unscoredPairs, Double firstScore, String winner, boolean complete) {
    static ArenaRoundSummary from(LearningArenaConfig config, LearningArenaState.Round round) {
        var result = round.result(config); var stats = result == null ? null : result.statistics();
        int wins = stats == null ? 0 : stats.wins(), draws = stats == null ? 0 : stats.draws(), losses = stats == null ? 0 : stats.losses();
        String first = model(config.a(), round.a()), second = model(config.b(), round.b());
        Double score = !round.arenaComplete() || stats == null || stats.validPairs() == 0 ? null : (wins + .5 * draws) / (2 * stats.validPairs());
        String winner = !round.arenaComplete() ? "Pending" : score == null ? "Unscored" : wins == losses ? "Draw"
                : (wins > losses ? config.a() : config.b()).name();
        return new ArenaRoundSummary(round.number(), first, second, wins, draws, losses,
                stats == null ? 0 : stats.incompletePairs(), score, winner, round.arenaComplete());
    }
    static String model(LearningArenaConfig.Competitor c, LearningArenaState.Endpoint endpoint) {
        return c.name() + " / " + c.architecture().displayName() + " / " + (endpoint == null ? "pending" : "Gen " + endpoint.generation());
    }
    String score(boolean first) { return firstScore == null ? complete ? "Unscored" : "Pending" : percent(first ? firstScore : 1 - firstScore); }
    String description() { return "Round " + round + ": " + first + " " + score(true) + " vs " + second + " " + score(false)
            + "; W/D/L " + wins + "/" + draws + "/" + losses + "; winner " + winner + "; unscored pairs " + unscoredPairs; }
    private static String percent(double score) { return String.format(Locale.ROOT, "%.1f%%", score * 100); }
}
