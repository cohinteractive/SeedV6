package com.ohinteractive.seedv6.gui;

import java.util.*;
import com.ohinteractive.seedv6.training.checkpoint.BootstrapEvidence;
import com.ohinteractive.seedv6.training.history.GenerationRecord;
import com.ohinteractive.seedv6.training.service.*;
import com.ohinteractive.seedv6.training.validation.HeldOutLoss;

/** Formatting and graph coordinates only. All assessments and objective losses come from the backend. */
final class TrainingComparison {
    static String lossName(BrnSupervision s) {
        return !s.blended() || s.teacherWeight() == 0 ? "WDL loss"
                : s.teacherWeight() == 1 ? "NNUE loss" : "Blended WDL + NNUE loss";
    }
    static String method(TrainingSource source, BrnSupervision supervision) {
        return source.bootstrap() ? lossName(supervision) : "Game Pair Validation";
    }
    static String positionMethod(TrainerSnapshot.RunDetails run) {
        if (run.effective().architecture() == com.ohinteractive.seedv6.training.model.TrainingArchitecture.NNUE)
            return "NNUE self-play";
        return run.source().mode().toString();
    }
    static String outcome(GenerationRecord r) {
        return switch (r.outcome()) {
            case PROMOTED -> "PROMOTED";
            case RETAINED -> "BEST RETAINED";
            case INCONCLUSIVE -> "INCONCLUSIVE";
            case CANCELLED_VALIDATION -> "CANCELLED";
        };
    }
    static String loss(double value) { return String.format(Locale.ROOT, "%.6f", value); }
    static String delta(HeldOutLoss.Comparison c) { return String.format(Locale.ROOT, "%+.6f", c.candidateLoss() - c.bestLoss()); }
    static String pair(String label, HeldOutLoss.Comparison c) {
        return label + " ↓ C " + loss(c.candidateLoss()) + " / B " + loss(c.bestLoss()) + " · Δ " + delta(c);
    }
    static String metric(GenerationRecord r) {
        if (r.bootstrap() == null) return "Score ↑ " + TrainingHistory.percent(r.score()) + " · lower "
                + TrainingHistory.percent(r.lowerBound()) + " · requires > " + TrainingHistory.percent(r.threshold());
        return metric(r.bootstrap());
    }
    static String metric(BootstrapEvidence b) {
        String result = pair(b.supervision().teacherWeight() > 0 && b.supervision().teacherWeight() < 1
                ? "Blend loss" : lossName(b.supervision()), b.comparison());
        if (b.supervision().teacherWeight() > 0 && b.supervision().teacherWeight() < 1)
            result += "\n" + pair("WDL", b.wdlLoss()) + "\n" + pair("NNUE", b.teacherLoss());
        return result;
    }
    static String regime(GenerationRecord r) {
        return r.bootstrap() == null ? "Game Pair Validation" : lossName(r.bootstrap().supervision())
                + (r.bootstrap().supervision().blended() ? " · NNUE weight " + r.bootstrap().supervision().teacherWeight() : "");
    }
    static Double trend(GenerationRecord r) {
        return r.bootstrap() == null ? r.score() : r.bootstrap().comparison().candidateLoss() - r.bootstrap().comparison().bestLoss();
    }
    /** A single axis never mixes scales; the latest contiguous objective is explicitly labelled. */
    static List<GenerationRecord> latestRegime(List<GenerationRecord> records) {
        if (records.isEmpty()) return records;
        String selected = regime(records.getLast()); int first = records.size() - 1;
        while (first > 0 && regime(records.get(first - 1)).equals(selected)) first--;
        return records.subList(first, records.size());
    }
    private TrainingComparison() {}
}
