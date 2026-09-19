package com.ohinteractive.seedv6.training.validation;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.training.selfplay.GameTermination;

/** Immutable match evidence. Only complete pairs contribute to every W/D/L statistic. */
public record ValidationResult(ValidationConfig config, String startingStateHash, List<Pair> pairs) {
    public ValidationResult {
        Objects.requireNonNull(config);
        if (startingStateHash == null || !startingStateHash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Invalid starting-state identity.");
        }
        pairs = List.copyOf(pairs);
        if (pairs.size() != config.openingPairs()) throw new IllegalArgumentException("Incomplete pair accounting.");
    }

    public record Game(GameTermination termination, int plies) {
        public Game {
            Objects.requireNonNull(termination);
            if (termination == GameTermination.ACTIVE || plies < 0) throw new IllegalArgumentException("Unfinished game.");
        }
        public double score(int candidateColour) {
            return (termination.result().orElseThrow().target(candidateColour) + 1) / 2;
        }
    }
    public record Pair(String openingHash, Game candidateWhite, Game candidateBlack) {
        public Pair {
            Objects.requireNonNull(openingHash);
            Objects.requireNonNull(candidateWhite);
            Objects.requireNonNull(candidateBlack);
        }
        public boolean valid() { return candidateWhite.termination().completed() && candidateBlack.termination().completed(); }
        public double score() {
            if (!valid()) throw new IllegalStateException("Incomplete pair has no score.");
            return (candidateWhite.score(Value.WHITE) + candidateBlack.score(Value.BLACK)) / 2;
        }
    }
    public record ColourRecord(int wins, int draws, int losses) {
        public ColourRecord {
            if (wins < 0 || draws < 0 || losses < 0) throw new IllegalArgumentException("Negative W/D/L.");
        }
        public long games() { return (long) wins + draws + losses; }
    }
    public record Statistics(int validPairs, int incompletePairs, ColourRecord white, ColourRecord black,
                             long totalPlies, Map<GameTermination, Integer> terminations) {
        public Statistics {
            Objects.requireNonNull(white);
            Objects.requireNonNull(black);
            terminations = Map.copyOf(terminations);
            long requested = (long) validPairs + incompletePairs;
            if (validPairs < 0 || incompletePairs < 0 || requested > Integer.MAX_VALUE / 2
                    || white.games() != validPairs || black.games() != validPairs || totalPlies < 0) {
                throw new IllegalArgumentException("Invalid paired statistics.");
            }
            long stopped = 0, completed = 0;
            for (var entry : terminations.entrySet()) {
                if (entry.getKey() == GameTermination.ACTIVE || entry.getValue() < 0) {
                    throw new IllegalArgumentException("Invalid terminations.");
                }
                stopped += entry.getValue();
                if (entry.getKey().completed()) completed += entry.getValue();
            }
            if (stopped != 2 * requested || completed < 2L * validPairs
                    || completed > 2L * validPairs + incompletePairs) {
                throw new IllegalArgumentException("Termination counts do not match pairs.");
            }
        }
        public int wins() { return white.wins() + black.wins(); }
        public int draws() { return white.draws() + black.draws(); }
        public int losses() { return white.losses() + black.losses(); }
        public double pairScoreSum() { return (wins() + 0.5 * draws()) / 2; }
    }

    public Statistics statistics() {
        int valid = 0;
        int[] white = new int[3], black = new int[3];
        long plies = 0;
        Map<GameTermination, Integer> reasons = new EnumMap<>(GameTermination.class);
        for (Pair pair : pairs) {
            plies += (long) pair.candidateWhite().plies() + pair.candidateBlack().plies();
            reasons.merge(pair.candidateWhite().termination(), 1, Integer::sum);
            reasons.merge(pair.candidateBlack().termination(), 1, Integer::sum);
            if (pair.valid()) {
                valid++;
                count(white, pair.candidateWhite().score(Value.WHITE));
                count(black, pair.candidateBlack().score(Value.BLACK));
            }
        }
        return new Statistics(valid, pairs.size() - valid,
                new ColourRecord(white[0], white[1], white[2]), new ColourRecord(black[0], black[1], black[2]), plies, reasons);
    }

    public PromotionPolicy.Assessment assess(PromotionPolicy policy) {
        Statistics s = statistics();
        return policy.assess(s.validPairs(), s.pairScoreSum());
    }

    private static void count(int[] record, double score) { record[score == 1 ? 0 : score == 0.5 ? 1 : 2]++; }
}

