package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.training.selfplay.GameTermination;
import com.ohinteractive.seedv6.training.validation.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/** Atomic campaign journal. Every round has its own checkpoint identities, exposure and game receipts. */
public record LearningArenaState(int version, String id, String binding, LearningArenaConfig config,
        List<Round> history, Status status, String message) {
    public enum Status { READY, RUNNING, PAUSED, FAILED, COMPLETE }
    public enum Stage { INITIALIZE_A, INITIALIZE_B, SELECT_TRANCHE, TRAIN_A, TRAIN_B, ARENA, ROUND_COMPLETE }
    /** Per-trained-model round facts, independent of the Arena winner. Null time means
     * a legacy continuation lacked timing. Mean loss is the terminal optimizer mean,
     * sample-weighted over this round's visits, exactly as in live optimizer progress. */
    public record TrainingMetrics(long sampleVisits, Long elapsedNanos, double learningRate,
                                  int minibatch, int epochs, Double finalMeanLoss) {
        public TrainingMetrics {
            if (sampleVisits < 0 || elapsedNanos != null && elapsedNanos < 0
                    || !Double.isFinite(learningRate) || learningRate <= 0 || minibatch < 1 || epochs < 1
                    || finalMeanLoss != null && !Double.isFinite(finalMeanLoss))
                throw new IllegalArgumentException("Invalid round training metrics");
        }
        public Double averageSampleVisitsPerSecond() {
            return elapsedNanos == null || elapsedNanos == 0 ? null : sampleVisits * 1e9 / elapsedNanos;
        }
    }
    public record Endpoint(String checkpoint, long generation, long positions, long exposure, TrainingMetrics training) {
        public Endpoint(String checkpoint, long generation, long positions, long exposure) {
            this(checkpoint, generation, positions, exposure, null);
        }
        public Endpoint {
            if (checkpoint == null || !checkpoint.matches("g[0-9]{6,19}-s[0-9]{9,19}-[0-9a-f]{64}")
                    || generation < 0 || positions < 0 || exposure < positions)
                throw new IllegalArgumentException("Invalid campaign checkpoint/exposure");
        }
    }
    public record Round(int number, String trancheHash, long sourceEnd, Endpoint a, Endpoint b,
                        List<ValidationResult.Pair> pairs, boolean arenaComplete) {
        public Round {
            if (number < 0 || sourceEnd < 0 || trancheHash == null || !trancheHash.isEmpty() && !trancheHash.matches("[0-9a-f]{64}"))
                throw new IllegalArgumentException("Invalid campaign round");
            pairs = List.copyOf(pairs);
            if (b != null && a == null || !pairs.isEmpty() && b == null || arenaComplete && b == null)
                throw new IllegalArgumentException("Out-of-order campaign round");
            if (number == 0 && (!trancheHash.isEmpty() || sourceEnd != 0)
                    || number > 0 && a != null && trancheHash.isEmpty()) throw new IllegalArgumentException("Invalid tranche stage");
        }
        static Round empty(int number) { return new Round(number, "", 0, null, null, List.of(), false); }
        public Stage stage() {
            if (arenaComplete) return Stage.ROUND_COMPLETE;
            if (number > 0 && trancheHash.isEmpty()) return Stage.SELECT_TRANCHE;
            if (a == null) return number == 0 ? Stage.INITIALIZE_A : Stage.TRAIN_A;
            if (b == null) return number == 0 ? Stage.INITIALIZE_B : Stage.TRAIN_B;
            return Stage.ARENA;
        }
        Round tranche(LearningArenaTranche.Manifest m) { return new Round(number, m.hash(), m.end(), a, b, pairs, arenaComplete); }
        Round endpoint(boolean first, Endpoint value) { return new Round(number, trancheHash, sourceEnd, first ? value : a, first ? b : value, pairs, arenaComplete); }
        Round games(List<ValidationResult.Pair> value, boolean complete) { return new Round(number, trancheHash, sourceEnd, a, b, value, complete); }
        public ValidationResult result(LearningArenaConfig config) {
            if (pairs.size() != config.arena().games() / 2) return null;
            long[] board = Board.fromFen(config.arena().startingFen());
            return new ValidationResult(config.arena().matches(config.seed()), ValidationArena.stateHash(board, GameHistory.initial(board)), pairs);
        }
    }
    public LearningArenaState {
        if (version != 1 || id == null || config == null || !config.identity().equals(binding) || status == null || message == null)
            throw new IllegalArgumentException("Invalid/incompatible Learning Arena state");
        UUID.fromString(id); history = List.copyOf(history);
        if (history.isEmpty()) throw new IllegalArgumentException("Missing campaign Round 0");
        for (int i = 0; i < history.size(); i++) {
            var r = history.get(i);
            if (r.number() != i || i < history.size() - 1 && !r.arenaComplete()
                    || r.pairs().size() > config.arena().games() / 2
                    || r.arenaComplete() && (r.pairs().size() != config.arena().games() / 2 || !settled(r.pairs())))
                throw new IllegalArgumentException("Noncontiguous/incomplete campaign history");
            long positions = Math.multiplyExact((long) i, config.positionsPerRound());
            long exposure = Math.multiplyExact(positions, config.epochs());
            for (Endpoint e : new Endpoint[]{r.a(), r.b()}) if (e != null && (e.positions() != positions || e.exposure() != exposure))
                throw new IllegalArgumentException("Unequal campaign exposure");
        }
        if (status == Status.COMPLETE && (!history.getLast().arenaComplete() || config.rounds() == 0 || history.getLast().number() != config.rounds()))
            throw new IllegalArgumentException("Premature campaign completion");
    }
    public Round current() { return history.getLast(); }
    LearningArenaState withRound(Round round) {
        var rounds = new ArrayList<>(history); rounds.set(rounds.size() - 1, round);
        return new LearningArenaState(version, id, binding, config, rounds, status, message);
    }
    LearningArenaState next() {
        var rounds = new ArrayList<>(history); rounds.add(Round.empty(current().number() + 1));
        return new LearningArenaState(version, id, binding, config, rounds, status, message);
    }
    LearningArenaState status(Status value, String detail) { return new LearningArenaState(version, id, binding, config, history, value, detail); }
    static boolean settled(List<ValidationResult.Pair> pairs) {
        return pairs.stream().allMatch(p -> settled(p.candidateWhite()) && settled(p.candidateBlack()));
    }
    static boolean settled(ValidationResult.Game game) { return game.termination().completed() || game.termination() == GameTermination.PLY_CAP; }
    public static LearningArenaState read(Path root) throws IOException { return DataFiles.read(root.resolve("campaign.json"), LearningArenaState.class); }
}
