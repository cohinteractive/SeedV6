package com.ohinteractive.seedv6.gui;

import java.util.Locale;
import java.util.OptionalLong;
import java.util.List;
import com.ohinteractive.seedv6.training.history.GenerationRecord;
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

    static double campaignFraction(long position, long total) {
        return total <= 0 ? 0 : Math.clamp((double) position / total, 0, 1);
    }
    static double campaignFraction(TrainerSnapshot s) {
        return s == null || s.run().isEmpty() ? 0 : campaignFraction(s.run().get().ordinal(s.generation()),
                s.run().get().effective().maximumGenerations());
    }
    /** Only N-1, never an older row silently substituted across a history gap. Records are generation sorted. */
    static GenerationRecord previousGeneration(List<GenerationRecord> records, long generation) {
        int low = 0, high = records.size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1; var r = records.get(mid);
            if (r.generation() == generation - 1) return r;
            if (r.generation() < generation - 1) low = mid + 1; else high = mid - 1;
        }
        return null;
    }
    record LiveComparison(long generation, Double score, Double threshold, int expectedPairs) {}
    static boolean liveValidation(TrainerSnapshot s) {
        return s != null && s.state() == TrainerSnapshot.State.VALIDATING && s.assessment().isEmpty()
                && s.validation().isEmpty() && s.validationProgress().isPresent() && s.validationDetails().isPresent()
                && !s.candidateId().isEmpty() && s.candidateId().equals(s.validationDetails().get().candidateId())
                && s.bootstrapValidation().isEmpty() && !s.run().map(r -> r.effective().heldOut(r.source())).orElse(false);
    }
    static LiveComparison liveComparison(TrainerSnapshot s) {
        if (!liveValidation(s)) return null;
        var p = s.validationProgress().orElseThrow(); var d = s.validationDetails().orElseThrow();
        int expected = d.config().openingPairs() - p.incompletePairs();
        return new LiveComparison(s.generation(), p.validPairs() == 0 ? null : candidateScore(s),
                d.policy().rawScoreThreshold(expected).stream().boxed().findFirst().orElse(null), expected);
    }
    private static double candidateScore(TrainerSnapshot s) {
        if (s.assessment().isPresent()) return s.assessment().get().mean();
        var v = s.validation().orElse(null); var p = s.validationProgress().orElse(null);
        return s.validationDetails().orElseThrow().policy().assess(v == null ? p.validPairs() : v.validPairs(),
                v == null ? (p.wins() + .5 * p.draws()) / 2 : v.pairScoreSum()).mean();
    }

    /** Baselines first observation/reopening/recovery. Only a later live publication can pulse. */
    static final class WinIncreases {
        private String candidate;
        private long started;
        private int wins, losses;
        record Change(boolean candidate, boolean best) {}
        void reset() { candidate = null; }
        Change update(TrainerSnapshot s, boolean visibleAndActive) {
            if (!visibleAndActive || !liveValidation(s)) { reset(); return new Change(false, false); }
            var p = s.validationProgress().orElseThrow();
            boolean same = s.candidateId().equals(candidate) && started == p.phaseStartedNanos()
                    && p.wins() >= wins && p.losses() >= losses;
            var d = s.validationDetails().orElseThrow();
            var change = new Change(same && p.wins() > Math.max(wins, d.restoredWins()),
                    same && p.losses() > Math.max(losses, d.restoredLosses()));
            candidate = s.candidateId(); started = p.phaseStartedNanos(); wins = p.wins(); losses = p.losses();
            return change;
        }
    }

    static String generationTitle(TrainerSnapshot s) {
        if (s == null) return "Training";
        var r = s.run().orElse(null);
        return "Generation " + s.generation() + (r == null ? "" : r.targetGeneration() == 0 ? " · Continuous" : " / " + r.targetGeneration());
    }
    static String runLabel(TrainerSnapshot s) {
        if (s == null || s.run().isEmpty()) return "Run begins on Start / Resume";
        var r = s.run().get();
        return r.targetGeneration() == 0 ? "Continuous run · " + s.totals().completedGenerations() + " finalized"
                : "Run " + Math.min(r.effective().maximumGenerations(), r.ordinal(s.generation())) + " / "
                + r.effective().maximumGenerations() + " · " + s.totals().completedGenerations() + " finalized";
    }
    static String timeLabel(TrainerSnapshot s) {
        if (s == null || s.run().isEmpty()) return "";
        var r = s.run().get(); long limit = r.effective().maximumRunMillis();
        return limit == 0 ? "No time limit" : (r.timeLimitReached() ? "Time limit reached · safe stop" : "Remaining "
                + timer(Math.max(0, limit - s.elapsed().toMillis() + 999) / 1000)) + " · Budget " + timer(limit / 1000);
    }

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
                count(s.generationSamplesTrained()) + " sample visits · Loss " + number(s.meanTrainingLoss()),
                s.run().map(r -> percent(s.generationSamplesTrained(), r.trainingSampleTarget())).orElse(-1));
    }

    static Progress validation(TrainerSnapshot s, TrainingSettings settings) {
        if (s != null && s.lossProgress().isPresent()) {
            var p = s.lossProgress().get();
            return new Progress(p.completed() + " / " + p.total() + " sample comparisons",
                    p.heldOutSamples() + " held-out samples · includes component passes", percent(p.completed(), p.total()));
        }
        if (s != null && s.bootstrapValidation().filter(b -> b.candidateId().equals(s.candidateId())).isPresent()) {
            var e = s.bootstrapValidation().get().evidence();
            return new Progress(e.comparison().samples() + " / " + e.comparison().samples() + " held-out samples", TrainingComparison.lossName(e.supervision()), 100);
        }
        if (s != null && s.run().map(r -> r.effective().heldOut(r.source())).orElse(false)
                || s == null && settings.selectedValidation() == com.ohinteractive.seedv6.training.service.ValidationMethod.HELD_OUT)
            return new Progress("Held-out loss", "Waiting for this Candidate", 0);
        Match m = match(s);
        int configuredPairs = s == null ? settings.validationPairs()
                : s.run().map(r -> r.effective().validation().openingPairs()).orElse(settings.validationPairs());
        if (m == null || !m.current()) return new Progress("0 / " + configuredPairs + " pairs", "Waiting for this Candidate", 0);
        if (s.assessment().isEmpty() && s.validation().isPresent()
                && s.validation().get().terminations().getOrDefault(CANCELLED, 0) > 0) {
            long done = s.validation().get().terminations().entrySet().stream().filter(e -> e.getKey() != CANCELLED).mapToLong(java.util.Map.Entry::getValue).sum();
            return new Progress(done + " / " + (2L * m.configuredPairs()) + " completed games",
                    "Paused · completed games saved for Resume", percent(done, 2L * m.configuredPairs()));
        }
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
        String decision = failed ? "PROMOTION BLOCKED" : a == null ? (v != null && reasons.getOrDefault(CANCELLED, 0) > 0
                ? "Validation paused" : finished ? "Assessment pending" : "Validation in progress")
                : switch (a.decision()) {
                    case PROMOTE -> s.bestId().equals(d.candidateId()) ? "PROMOTED" : "Promotion publication pending";
                    case RETAIN_INCUMBENT -> "BEST RETAINED";
                    case INCONCLUSIVE -> "INCONCLUSIVE";
                };
        String detail = failed ? "Search, infrastructure or publication failed"
                : a == null ? "Decision follows all configured game slots"
                : a.decision() == PromotionPolicy.Decision.INCONCLUSIVE ? valid + " / " + d.policy().minimumPairs() + " required valid pairs"
                : "Lower " + score(a.lowerBound()) + " · requires > " + score(a.threshold());
        String duration = p != null && p.complete() ? timer(p.validationElapsed(p.lastProgressNanos()).toSeconds()) : "—";
        return new Match(TrainingProgress.generation(d.candidateGeneration(), d.candidateId()),
                TrainingProgress.generation(d.bestGeneration(), d.bestId()), d.candidateId(), d.bestId(), wins, draws, losses,
                wins + draws + losses == 0 ? "—" : score(candidateScore(s)), decision, detail,
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
