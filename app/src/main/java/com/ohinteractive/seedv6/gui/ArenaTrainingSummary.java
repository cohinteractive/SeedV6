package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.training.service.*;
import java.util.Locale;

/** Training columns use A / B; duration totals both active phases. Neither follows the winner. */
final class ArenaTrainingSummary {
    static String cell(LearningArenaConfig config, LearningArenaState.Round round, int field) {
        if (round.number() == 0) return "\u2014";
        if (field == 1) {
            var a = round.a() == null ? null : round.a().training();
            var b = round.b() == null ? null : round.b().training();
            return duration(a == null || b == null || a.elapsedNanos() == null || b.elapsedNanos() == null
                    ? null : Math.addExact(a.elapsedNanos(), b.elapsedNanos()));
        }
        return value(config, round, true, field) + " / " + value(config, round, false, field);
    }
    static String value(LearningArenaConfig config, LearningArenaState.Round round, boolean first, int field) {
        var endpoint = first ? round.a() : round.b(); var competitor = first ? config.a() : config.b();
        var metrics = endpoint == null ? null : endpoint.training();
        if (round.number() == 0) return "\u2014";
        return switch (field) {
            case 0 -> number(metrics == null ? null : metrics.averageSampleVisitsPerSecond(), "%.1f");
            case 1 -> duration(metrics == null ? null : metrics.elapsedNanos());
            case 2 -> number(metrics == null ? competitor.learningRate() : Double.valueOf(metrics.learningRate()), "%.4g");
            case 3 -> Integer.toString(metrics == null ? competitor.minibatch() : metrics.minibatch());
            case 4 -> Integer.toString(metrics == null ? config.epochs() : metrics.epochs());
            case 5 -> number(metrics == null ? null : metrics.finalMeanLoss(), "%.6g");
            default -> throw new IllegalArgumentException("Unknown training column");
        };
    }
    static String duration(Long nanos) {
        if (nanos == null) return "\u2014";
        long seconds = nanos / 1_000_000_000;
        if (seconds < 1) return String.format(Locale.ROOT, "%.2fs", nanos / 1e9);
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) return seconds / 60 + "m " + seconds % 60 + "s";
        return seconds / 3600 + "h " + seconds / 60 % 60 + "m";
    }
    private static String number(Double value, String format) { return value == null ? "\u2014" : String.format(Locale.ROOT, format, value); }
    private ArenaTrainingSummary() {}
}
