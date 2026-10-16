package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.training.service.LearningArenaConfig;
import com.ohinteractive.seedv6.training.service.LearningArenaState;
import java.util.Locale;

/** Presentation over the existing paired-game evidence; never a promotion or inferred result. */
record ArenaRoundSummary(int round, String first, String second, int wins, int draws, int losses,
        int unscoredPairs, Double firstScore, String winner, boolean complete) {
    static ArenaRoundSummary from(LearningArenaConfig config, LearningArenaState.Round round) {
        var match = ArenaMatchSummary.from(config, round);
        int wins = match.wins(), draws = match.draws(), losses = match.losses();
        String first = ArenaMatchSummary.label(config, true), second = ArenaMatchSummary.label(config, false);
        int scored = wins + draws + losses;
        Double score = !round.arenaComplete() || scored == 0 ? null : (wins + .5 * draws) / scored;
        String winner = !round.arenaComplete() ? "Pending" : score == null ? "Unscored" : wins == losses ? "Tie (A / B)"
                : wins > losses ? first : second;
        return new ArenaRoundSummary(round.number(), first, second, wins, draws, losses,
                match.unscoredPairs(), score, winner, round.arenaComplete());
    }
    static String model(LearningArenaConfig.Competitor c, LearningArenaState.Endpoint endpoint) {
        if (c.isHandcrafted()) return "HCE (Handcrafted evaluator; no checkpoint)";
        return c.name() + " / " + c.architecture().displayName() + " / " + (endpoint == null ? "pending" : "Gen " + endpoint.generation());
    }
    String score(boolean first) { return firstScore == null ? complete ? "Unscored" : "Pending" : percent(first ? firstScore : 1 - firstScore); }
    String winningWdl() { return firstScore == null ? "\u2014" : ArenaMatchSummary.wdl(Math.max(wins, losses), draws, Math.min(wins, losses)); }
    String orderedScores() { return ArenaMatchSummary.scores(wins, draws, losses, losses > wins); }
    String description() { return "Round " + round + ": " + first + " " + score(true) + " vs " + second + " " + score(false)
            + "; W/D/L " + wins + "/" + draws + "/" + losses + "; winner " + winner + "; unscored pairs " + unscoredPairs; }
    private static String percent(double score) { return String.format(Locale.ROOT, "%.1f%%", score * 100); }
}
