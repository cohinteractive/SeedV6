package com.ohinteractive.seedv6.gui;

import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import java.util.regex.Pattern;
import com.ohinteractive.seedv6.training.service.TrainerSnapshot;
import com.ohinteractive.seedv6.training.validation.PromotionPolicy;
import com.ohinteractive.seedv6.training.selfplay.GameTermination;
import com.ohinteractive.seedv6.training.validation.ValidationProgress.CandidateColour;

/** Pure constant-size presentation of the immutable service snapshot. */
final class TrainingProgress {
    private TrainingProgress() {}

    static String format(TrainingController.ViewState view) {
        var s = view.snapshot();
        StringBuilder text = new StringBuilder();
        text.append("State: ").append(view.active() && view.phase() != TrainingController.Phase.RUNNING
                ? view.phase() : s == null ? view.phase() : s.state()).append('\n');
        var c = view.settings();
        text.append("Network Architecture: ").append(c.architecture()).append('\n');
        if (c.source() != null && c.source().bootstrap()) text.append("Training Source: Bootstrap with NNUE\nBRN checkpoint store: ")
                .append(c.root()).append("\nNNUE generator store: ").append(c.source().generatorStore()).append('\n');
        text.append("Depth ").append(c.depth()).append(" · Threads ").append(c.threads())
                .append(" · Games ").append(c.games()).append(" · Validation pairs ").append(c.validationPairs()).append('\n');
        text.append(view.message()).append('\n');
        if (s == null) return text.toString();
        text.append("Elapsed: ").append(s.elapsed().toSeconds()).append("s · Generation: ").append(s.generation())
                .append(" · Training depth: ").append(s.trainingDepth()).append('\n');
        text.append("Best: ").append(PlayEvaluator.shortId(s.bestId()));
        if (!s.bestId().isEmpty()) text.append(s.bestId().equals(view.bootstrapId()) ? " (bootstrap, not a promotion)" : " (accepted)");
        text.append("\nLatest-training: ").append(PlayEvaluator.shortId(s.latestTrainingId()))
                .append("\nCandidate: ").append(PlayEvaluator.shortId(s.candidateId())).append('\n');
        text.append("Self-play / training\n");
        var g = s.selfPlay();
        text.append("Games requested/completed/aborted/capped: ").append(g.requestedGames()).append('/')
                .append(g.completedGames()).append('/').append(g.abortedGames()).append('/').append(g.cappedGames()).append('\n');
        text.append("White wins / draws / Black wins: ").append(g.whiteWins()).append(" / ").append(g.draws()).append(" / ").append(g.blackWins()).append('\n');
        text.append("Sampled positions: ").append(g.sampledPositions()).append(" · Updates: ").append(s.generationOptimizerUpdates())
                .append(" · ").append(c.architecture().optimizerName()).append(" step: ").append(s.optimizerStep())
                .append("\nMean loss: ").append(number(s.meanTrainingLoss())).append('\n');
        var t = s.totals();
        text.append("Run totals — generations: ").append(t.completedGenerations()).append(" · games: ").append(t.selfPlayGames())
                .append("\nSamples: ").append(t.sampledPositions()).append(" · updates: ").append(t.optimizerUpdates())
                .append("\nPromotions: ").append(t.promotions()).append(" · retains: ").append(t.retainedCandidates());
        if (s.failed()) text.append("\nFailure: ").append(s.failureSummary());
        return text.toString();
    }

    // Only recovered/legacy presentation snapshots lack explicit manifest generations.
    // Match the complete verified CheckpointManifest ID grammar; never infer from an arbitrary prefix.
    private static final Pattern CHECKPOINT_ID = Pattern.compile("g([0-9]{6,19})-s[0-9]{9,19}-[0-9a-f]{64}");

    /** Render from a single immutable snapshot and monotonic now; polling creates no progress events. */
    static String validation(TrainingController.ViewState view, long now) {
        var s = view.snapshot();
        if (s != null && s.bootstrapValidation().isPresent()) return bootstrap(s);
        if (s == null || (s.validationProgress().isEmpty() && s.validation().isEmpty())) return "";
        var p = s.validationProgress().orElse(null);
        var v = s.validation().orElse(null);
        var d = s.validationDetails().orElse(null);
        var a = s.assessment().orElse(null);
        int wins = v != null ? v.wins() : p.wins();
        int draws = v != null ? v.draws() : p.draws();
        int losses = v != null ? v.losses() : p.losses();
        int valid = v != null ? v.validPairs() : p.validPairs();
        int incomplete = v != null ? v.incompletePairs() : p.incompletePairs();
        Map<GameTermination, Integer> reasons = v != null ? v.terminations() : p.terminations();
        int capped = reasons.getOrDefault(GameTermination.PLY_CAP, 0);
        int cancelled = reasons.getOrDefault(GameTermination.CANCELLED, 0);
        int failed = reasons.getOrDefault(GameTermination.SEARCH_FAILURE, 0)
                + reasons.getOrDefault(GameTermination.INFRASTRUCTURE_FAILURE, 0);
        // A service failure can interrupt the arena before it publishes a final progress event.
        // A later training failure must not relabel an already finished validation.
        boolean interrupted = s.failed() && (a == null || p != null && !p.complete()
                || d != null && d.candidateId().equals(s.candidateId()));
        boolean finished = a != null || p == null || p.complete() || interrupted;
        int maximumGames = p != null ? p.maximumGames() : d != null ? d.config().openingPairs() * 2 : 2 * (valid + incomplete);
        int accountedGames = reasons.values().stream().mapToInt(Integer::intValue).sum();
        String score = wins + draws + losses == 0 ? "pending" : percent((wins + 0.5 * draws) / (wins + draws + losses));
        StringBuilder text = new StringBuilder();
        if (!finished) {
            text.append("Validation in progress | Candidate score ").append(score).append(" | ").append(valid).append(" valid pairs");
        } else if (failed > 0 || interrupted) {
            text.append("Validation failed - PROMOTION BLOCKED | Search/infrastructure failure");
        } else if (a == null) {
            text.append("Validation finished - assessment pending");
        } else {
            headline(text, a, d);
        }
        text.append("\n\nCandidate: Gen ").append(d == null ? "unknown" : generation(d.candidateGeneration(), d.candidateId()))
                .append("    Wins: ").append(wins);
        text.append("\nBest:      Gen ").append(d == null ? "unknown" : generation(d.bestGeneration(), d.bestId()))
                .append("    Wins: ").append(losses);
        text.append("\nDraws: ").append(draws).append("\n\n");
        if (finished) {
            text.append("Games: ").append(accountedGames).append(" / ").append(maximumGames).append(" | ");
        } else {
            text.append("Pair ").append(p.currentPair()).append(" / ").append(p.configuredPairs())
                    .append(" | Game ").append(p.gameInPair()).append(" / 2")
                    .append(" | Overall ").append(p.gameOrdinal() == 0 ? accountedGames : p.gameOrdinal())
                    .append(" / ").append(maximumGames).append('\n');
        }
        text.append("Valid pairs: ").append(valid).append(" | Incomplete: ").append(incomplete)
                .append(" | Capped: ").append(capped).append('\n');
        if (!finished && p.gameActive()) {
            boolean candidateWhite = p.candidateColour() == CandidateColour.WHITE;
            text.append("\nWhite: ").append(candidateWhite ? "Candidate" : "Best")
                    .append(" | Black: ").append(candidateWhite ? "Best" : "Candidate").append('\n');
            text.append("Current game: ").append(p.currentGamePlies()).append(" plies | ")
                    .append(timer(p.gameElapsed(now).toSeconds())).append('\n');
            p.lastMoveSearch().ifPresent(m -> text.append("\nLast move: depth ").append(m.depth())
                    .append(" | ").append(String.format(Locale.ROOT, "%,d", m.nodes())).append(" nodes | ")
                    .append(m.elapsedMillis()).append(" ms | ").append(nps(m.nps())).append(" NPS\n"));
        }
        if (finished && p != null) {
            text.append("Validation elapsed: ").append(timer(p.validationElapsed(p.lastProgressNanos()).toSeconds()));
            if (!p.complete()) text.append(" (last progress)");
            text.append('\n');
        }
        if (d != null) text.append("Search: depth ").append(d.config().depth()).append(" | threads ").append(d.config().threads())
                .append(" | no node/time limit\n");
        if (!finished) text.append("Validation elapsed: ").append(timer(p.validationElapsed(now).toSeconds())).append('\n');
        if (cancelled > 0) text.append("Warning: Cancelled slots: ").append(cancelled).append('\n');
        if (failed > 0) text.append("Warning: Failed slots: ").append(failed).append(" | Promotion blocked\n");
        return text.toString();
    }

    static String bootstrap(TrainerSnapshot snapshot) {
        var detail = snapshot.bootstrapValidation().orElseThrow(); var e = detail.evidence(); var c = e.comparison();
        String verdict = c.decision() == PromotionPolicy.Decision.PROMOTE
                ? snapshot.bestId().equals(detail.candidateId()) ? "PROMOTED" : "Promotion publication pending" : "KEEP BEST";
        return "Bootstrap terminal W/D/L validation - " + verdict
                + "\nCandidate loss: " + Double.toString(c.candidateLoss()) + " | Best loss: " + Double.toString(c.bestLoss())
                + "\nMean half-squared error; strictly lower promotes, ties retain. Prediction accuracy, not game strength."
                + "\nTraining / held-out samples: " + e.trainingSamples() + " / " + c.samples()
                + " | games: " + e.trainingGames() + " / " + e.heldOutGames()
                + "\nCandidate: " + detail.candidateId() + "\nIncumbent: " + detail.incumbentId()
                + "\nNNUE generator: " + e.generatorId() + "\nGenerator store: " + e.generatorStore()
                + "\nSplit seed: " + e.splitSeed() + " | data SHA-256: " + e.dataHash();
    }
    static String bootstrapSummary(TrainerSnapshot snapshot) {
        var detail = snapshot.bootstrapValidation().orElseThrow(); var e = detail.evidence(); var c = e.comparison();
        String decision = c.decision() == PromotionPolicy.Decision.PROMOTE
                ? snapshot.bestId().equals(detail.candidateId()) ? "PROMOTED" : "Promotion publication pending" : "KEEP BEST";
        return decision + " | Candidate loss " + number(c.candidateLoss()) + " | Best loss " + number(c.bestLoss())
                + "\nTraining / held-out: " + e.trainingSamples() + " / " + c.samples() + " samples from "
                + e.trainingGames() + " / " + e.heldOutGames() + " games"
                + "\nCandidate " + PlayEvaluator.shortId(detail.candidateId()) + " | Incumbent " + PlayEvaluator.shortId(detail.incumbentId())
                + "\nNNUE generator " + PlayEvaluator.shortId(e.generatorId())
                + "\nStrictly lower mean half-squared error promotes; ties keep Best."
                + "\nPrediction accuracy, not game strength. Full evidence is in History / Diagnostics.";
    }

    private static void headline(StringBuilder text, PromotionPolicy.Assessment a, TrainerSnapshot.ValidationDetails details) {
        text.append("Validation finished - ");
        if (a.decision() == PromotionPolicy.Decision.INCONCLUSIVE) {
            text.append("INCONCLUSIVE - KEEP BEST | Valid pairs: ").append(a.validPairs());
            if (details != null) text.append(" < ").append(details.policy().minimumPairs()).append(" required");
            else text.append(" | Minimum valid pairs not met");
            text.append(" | Score ").append(percent(a.mean()));
        } else {
            text.append(a.decision() == PromotionPolicy.Decision.PROMOTE ? "PROMOTE CANDIDATE" : "KEEP BEST")
                    .append(" | Score ").append(percent(a.mean())).append(" | Lower ").append(percent(a.lowerBound()))
                    .append(a.lowerBound() > a.threshold() ? " > " : a.lowerBound() < a.threshold() ? " < " : " = ")
                    .append(percent(a.threshold()));
        }
    }

    static String generation(OptionalLong metadata, String id) {
        if (metadata.isPresent()) return Long.toString(metadata.getAsLong());
        var match = CHECKPOINT_ID.matcher(id);
        if (match.matches()) {
            try { return Long.toString(Long.parseLong(match.group(1))); }
            catch (NumberFormatException outOfRange) { /* Unknown is preferable to a fabricated generation. */ }
        }
        return "unknown";
    }

    private static String percent(double value) { return Double.isFinite(value) ? String.format(Locale.ROOT, "%.1f%%", 100 * value) : "pending"; }
    private static String nps(long value) {
        return value < 0 ? "unavailable" : value >= 1_000_000 ? String.format(Locale.ROOT, "%.2fM", value / 1_000_000.0)
                : String.format(Locale.ROOT, "%,d", value);
    }

    private static String timer(long seconds) { return String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60); }

    private static String number(double number) { return Double.isFinite(number) ? String.format(Locale.ROOT, "%.6f", number) : "—"; }
}
