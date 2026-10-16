package com.ohinteractive.seedv6.gui;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import com.ohinteractive.seedv6.training.history.GenerationRecord;
import com.ohinteractive.seedv6.training.history.HistoryRepository;
import com.ohinteractive.seedv6.training.service.TrainerSnapshot;

/** Presentation only: no checkpoint reads, inferred losses, or cross-objective normalization. */
final class TrainingLoss {
    enum Objective {
        CROSS_ENTROPY("Cross-entropy", "nats per sample"),
        HALF_SQUARED("Half-squared error", "target units squared per sample"),
        UNKNOWN("Objective unavailable", "Historical objective/units were not recorded");
        final String label, units;
        Objective(String label, String units) { this.label = label; this.units = units; }
    }
    static final String UNAVAILABLE = "Unavailable";
    static final String EXPLANATION = "Recorded objective loss, not a percentage or playing strength. "
            + "Training and held-out measurements use different samples; objectives and scales can differ by architecture, recipe, "
            + "and between running mean and final fit. Compare only matching measurements.";
    record Measurement(Double value, long generation, String objective) {
        String description() { return format(value) + " (" + (generation > 0 ? "Gen " + generation : "generation unavailable") + ", " + objective + ")"; }
    }
    private HistoryRepository.Snapshot source;
    private final Map<String, GenerationRecord> trained = new HashMap<>();
    private final Map<String, Measurement> validated = new HashMap<>();

    void update(HistoryRepository.Snapshot history) {
        if (source == history) return;
        source = history; trained.clear(); validated.clear();
        for (var r : history.records()) {
            trained.put(r.candidate(), r);
            if (r.bootstrap() != null) {
                var b = r.bootstrap(); String objective = TrainingComparison.lossName(b);
                validated.put(r.candidate(), new Measurement(b.comparison().candidateLoss(), r.generation(), objective));
                validated.put(r.incumbent(), new Measurement(b.comparison().bestLoss(), r.generation(), objective));
            }
        }
    }
    GenerationRecord latest() { return source == null || source.records().isEmpty() ? null : source.records().getLast(); }
    Double finalLoss(String id, TrainerSnapshot s) {
        if (id == null || id.isEmpty()) return null;
        if (currentCandidate(id, s) && s.training().isPresent()) {
            var t = s.training().get();
            if (!t.cancelled() && valid(t.finalLoss())) return t.finalLoss();
        }
        var r = trained.get(id);
        return r == null ? null : r.loss();
    }
    private static boolean currentCandidate(String id, TrainerSnapshot s) {
        return s != null && id.equals(s.candidateId()) && checkpointGeneration(id) == s.generation();
    }
    Objective finalObjective(String id, TrainerSnapshot s, NetworkArchitecture architecture) {
        if (currentCandidate(id, s) && s.training().filter(t -> !t.cancelled() && valid(t.finalLoss())).isPresent())
            return currentFinalObjective(s, architecture);
        return recordedObjective(trained.get(id), architecture);
    }
    static Objective recordedObjective(GenerationRecord record, NetworkArchitecture architecture) {
        String settings = record == null ? null : record.regime().effectiveSettings();
        String marker = "|finalLossObjective=";
        if (settings != null && settings.contains(marker)) {
            String objective = settings.substring(settings.indexOf(marker) + marker.length()).split("\\|", 2)[0];
            return objective(objective); // An unrecognized recorded objective must never become a guessed one.
        }
        return invariantFinalObjective(architecture);
    }
    static Objective currentFinalObjective(TrainerSnapshot s, NetworkArchitecture architecture) {
        String recorded = s == null ? null : s.run().map(TrainerSnapshot.RunDetails::finalLossObjective).orElse(null);
        return recorded == null ? invariantFinalObjective(architecture) : objective(recorded);
    }
    static Objective runningObjective(TrainerSnapshot s, NetworkArchitecture architecture) {
        return architecture == NetworkArchitecture.BRN_PAIR2 ? Objective.CROSS_ENTROPY : currentFinalObjective(s, architecture);
    }
    private static Objective objective(String value) {
        return switch (value) {
            case "cross-entropy (nats/example)" -> Objective.CROSS_ENTROPY;
            case "half-squared target error (target units squared/example)" -> Objective.HALF_SQUARED;
            default -> Objective.UNKNOWN;
        };
    }
    private static Objective invariantFinalObjective(NetworkArchitecture architecture) {
        // Material NNUE has both legacy half-squared and calibrated CE checkpoint recipes.
        // Current editable settings cannot identify an older model's persisted objective.
        return architecture == NetworkArchitecture.NNUE_MATERIAL ? Objective.UNKNOWN : Objective.HALF_SQUARED;
    }
    Measurement validation(String id, TrainerSnapshot s) {
        if (id == null || id.isEmpty()) return null;
        if (s != null && s.bootstrapValidation().isPresent()) {
            var b = s.bootstrapValidation().get();
            Double value = null;
            if (id.equals(b.candidateId())) value = b.evidence().comparison().candidateLoss();
            else if (id.equals(b.incumbentId())) value = b.evidence().comparison().bestLoss();
            if (value != null) {
                // The publication may retain the previous generation's validation. Use its identity, never s.generation().
                var recorded = trained.get(b.candidateId());
                long generation = recorded != null ? recorded.generation() : b.evidence().corpus() != null
                        ? b.evidence().corpus().generation() : checkpointGeneration(b.candidateId());
                return new Measurement(value, generation, TrainingComparison.lossName(b.evidence()));
            }
        }
        return validated.get(id);
    }
    String summary(String id, TrainerSnapshot s) {
        var v = validation(id, s);
        return "Training loss (final): " + format(finalLoss(id, s)) + "; validation loss: "
                + (v == null ? UNAVAILABLE : v.description()) + ". " + EXPLANATION;
    }
    static long checkpointGeneration(String id) {
        String value = TrainingProgress.generation(java.util.OptionalLong.empty(), id);
        try { return Long.parseLong(value); } catch (NumberFormatException unknown) { return 0; }
    }
    static boolean valid(Double value) { return value != null && Double.isFinite(value) && value >= 0; }
    static String format(Double value) {
        if (!valid(value)) return UNAVAILABLE;
        return String.format(Locale.ROOT, value != 0 && value < .000001 || value >= 1000 ? "%.6g" : "%.6f", value);
    }
}
