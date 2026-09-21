package com.ohinteractive.seedv6.gui;

import java.util.Locale;
import java.util.OptionalLong;
import com.ohinteractive.seedv6.training.service.TrainerSnapshot;
import com.ohinteractive.seedv6.training.validation.PromotionPolicy;
import static com.ohinteractive.seedv6.training.selfplay.GameTermination.*;

/** Pure presentation of one publication. Never infers a promotion from the live score. */
final class TrainingDashboardModel {
    record Progress(String value, String detail, int percent) {} // -1: no meaningful denominator
    record Match(String candidate, String incumbent, String candidateId, String incumbentId,
                 int wins, int draws, int losses, String score, String decision, String detail,
                 int pairs, int configuredPairs, int incomplete, int slots, int depth, int threads,
                 String duration, boolean current, boolean finished, boolean failed) {}

    static String phase(TrainingController.ViewState view) {
        if (view.phase() != TrainingController.Phase.RUNNING) return switch (view.phase()) {
            case CONFIRM_DEPTH -> "CONFIRM DEPTH";
            default -> view.phase().toString();
        };
        var s = view.snapshot();
        if (s == null) return "STARTING";
        return switch (s.state()) {
            case GENERATING_SELF_PLAY -> "SELF-PLAY";
            case PUBLISHING_CANDIDATE -> "PUBLISHING";
            case RECORDING_DECISION -> "RECORDING DECISION";
            default -> s.state().toString();
        };
    }

    static Progress selfPlay(TrainerSnapshot s, TrainingSettings settings) {
        if (s == null) return new Progress("0 / " + settings.games() + " games", "Waiting to start", 0);
        var g = s.selfPlay();
        int processed = g.completedGames() + g.abortedGames();
        return new Progress(processed + " / " + g.requestedGames() + " processed",
                g.completedGames() + " complete · " + g.abortedGames() + " aborted (" + g.cappedGames() + " capped)",
                percent(processed, g.requestedGames()));
    }

    static Progress training(TrainerSnapshot s) {
        if (s == null) return new Progress("No optimizer updates", "Samples and loss appear during training", -1);
        return new Progress(count(s.generationOptimizerUpdates()) + " updates · " + count(s.selfPlay().sampledPositions()) + " samples",
                count(s.generationSamplesTrained()) + " sample visits · Loss " + number(s.meanTrainingLoss()), -1);
    }

    static Progress validation(TrainerSnapshot s, TrainingSettings settings) {
        Match m = match(s);
        if (m == null || !m.current()) return new Progress("0 / " + settings.validationPairs() + " pairs", "Waiting for this Candidate", 0);
        return new Progress((m.pairs() + m.incomplete()) + " / " + m.configuredPairs() + " pairs",
                m.pairs() + " valid · " + m.incomplete() + " incomplete · " + m.slots() + " / " + (m.configuredPairs() * 2) + " slots",
                percent(m.pairs() + m.incomplete(), m.configuredPairs()));
    }

    static Match match(TrainerSnapshot s) {
        if (s == null || s.validationDetails().isEmpty() || s.validationProgress().isEmpty() && s.validation().isEmpty()) return null;
        var d = s.validationDetails().orElseThrow();
        var p = s.validationProgress().orElse(null);
        var v = s.validation().orElse(null);
        var a = s.assessment().orElse(null);
        int wins = v == null ? p.wins() : v.wins(), draws = v == null ? p.draws() : v.draws(), losses = v == null ? p.losses() : v.losses();
        int valid = v == null ? p.validPairs() : v.validPairs(), incomplete = v == null ? p.incompletePairs() : v.incompletePairs();
        var reasons = v == null ? p.terminations() : v.terminations();
        boolean current = !s.candidateId().isEmpty() && s.candidateId().equals(d.candidateId());
        boolean failed = reasons.getOrDefault(SEARCH_FAILURE, 0) + reasons.getOrDefault(INFRASTRUCTURE_FAILURE, 0) > 0
                || s.failed() && (current || a == null || p != null && !p.complete());
        boolean finished = a != null || v != null || p != null && p.complete() || failed;
        String decision = failed ? "PROMOTION BLOCKED" : a == null ? (finished ? "Assessment pending" : "Validation in progress")
                : switch (a.decision()) {
                    case PROMOTE -> s.bestId().equals(d.candidateId()) ? "PROMOTED" : "Promotion publication pending";
                    case RETAIN_INCUMBENT -> "KEEP BEST";
                    case INCONCLUSIVE -> "INCONCLUSIVE · KEEP BEST";
                };
        String detail = failed ? "Search, infrastructure or publication failed"
                : a == null ? "Decision follows all configured game slots"
                : a.decision() == PromotionPolicy.Decision.INCONCLUSIVE ? valid + " / " + d.policy().minimumPairs() + " required valid pairs"
                : "Lower " + score(a.lowerBound()) + " · requires > " + score(a.threshold());
        String duration = p != null && p.complete() ? timer(p.validationElapsed(p.lastProgressNanos()).toSeconds()) : "—";
        return new Match(TrainingProgress.generation(d.candidateGeneration(), d.candidateId()),
                TrainingProgress.generation(d.bestGeneration(), d.bestId()), d.candidateId(), d.bestId(), wins, draws, losses,
                wins + draws + losses == 0 ? "—" : score((wins + .5 * draws) / (wins + draws + losses)), decision, detail,
                valid, d.config().openingPairs(), incomplete, reasons.values().stream().mapToInt(Integer::intValue).sum(),
                d.config().depth(), d.config().threads(), duration, current, finished, failed);
    }

    static String network(String id) {
        if (id.isEmpty()) return "Not published";
        String generation = TrainingProgress.generation(OptionalLong.empty(), id);
        return generation.equals("unknown") ? PlayEvaluator.shortId(id) : "Gen " + generation;
    }
    static String score(double value) { return Double.isFinite(value) ? String.format(Locale.ROOT, "%.1f%%", value * 100) : "—"; }
    static String number(double value) { return Double.isFinite(value) ? String.format(Locale.ROOT, "%.4f", value) : "—"; }
    static String count(long value) { return String.format(Locale.ROOT, "%,d", value); }
    static String timer(long seconds) { return String.format(Locale.ROOT, "%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60); }
    private static int percent(long done, long total) { return total <= 0 ? -1 : (int) Math.min(100, 100L * done / total); }
    private TrainingDashboardModel() {}
}
