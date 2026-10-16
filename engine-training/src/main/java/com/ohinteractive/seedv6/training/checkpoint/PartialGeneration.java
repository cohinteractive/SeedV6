package com.ohinteractive.seedv6.training.checkpoint;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.validation.ValidationResult;

/** A safe-stop continuation, never a checkpoint, decision, or Best candidate. */
public record PartialGeneration(GenerationAttempt attempt, String candidate,
        SelfPlayBatch.Saved games, SelfPlayBatch.Statistics statistics, SelfPlayControl.TrainingCursor training,
        List<ValidationResult.Pair> pairs, Instant started, long activeNanos,
        long selfPlayNanos, long trainingNanos, long validationNanos, boolean recoveryOnly, boolean generationSettingsKnown) {
    public static final String FILE = "partial-generation.bin";
    static final int MAX_BYTES = 128 * 1024 * 1024;
    public PartialGeneration(GenerationAttempt attempt, String candidate, SelfPlayBatch.Saved games,
            SelfPlayBatch.Statistics statistics, SelfPlayControl.TrainingCursor training, List<ValidationResult.Pair> pairs,
            Instant started, long activeNanos, long selfPlayNanos, long trainingNanos, long validationNanos, boolean recoveryOnly) {
        this(attempt, candidate, games, statistics, training, pairs, started, activeNanos, selfPlayNanos,
                trainingNanos, validationNanos, recoveryOnly, true);
    }
    public PartialGeneration {
        Objects.requireNonNull(attempt); Objects.requireNonNull(games); Objects.requireNonNull(training);
        Objects.requireNonNull(statistics);
        Objects.requireNonNull(started); pairs = List.copyOf(pairs);
        if (!candidate.isEmpty()) CheckpointManifest.requireId(candidate);
        if (activeNanos < 0 || selfPlayNanos < 0 || trainingNanos < 0 || validationNanos < 0)
            throw new IllegalArgumentException("Invalid partial duration.");
        int samples = 0;
        for (int i = 0; i < games.games().size(); i++) {
            var g = games.games().get(i);
            if (g.gameIndex() != i || g.termination() == GameTermination.CANCELLED || g.sampledPositions() < 0)
                throw new IllegalArgumentException("Invalid saved game.");
            samples = Math.addExact(samples, g.sampledPositions());
        }
        if (samples != games.samples().size()) throw new IllegalArgumentException("Partial sample accounting differs.");
    }
    record Stored(PartialGeneration progress, Path state, String stateHash) {}
    static void verifyState(Stored stored) throws IOException {
        if (stored.state() == null) return;
        CheckpointPayload.regular(stored.state());
        if (!SmallRecord.hash(stored.state()).equals(stored.stateHash())) throw new IOException("Partial optimizer hash mismatch.");
    }
    /** Metadata-only, no lock, repair, model load or directory creation. */
    public static Optional<PartialGeneration> inspect(Path root) throws IOException {
        return read(root).map(Stored::progress);
    }
    static Optional<Stored> read(Path root) throws IOException {
        Path ref = root.resolve(FILE);
        if (Files.notExists(ref)) return Optional.empty();
        return Optional.of(SmallRecord.read(ref, "partial-generation-reference-v1", in -> {
            String id = in.readUTF(); UUID.fromString(id);
            String metadataHash = SmallRecord.requireHash(in.readUTF()), stateHash = in.readUTF();
            Path directory = root.resolve("partial-generations").resolve(id);
            Path metadata = directory.resolve("progress.bin");
            CheckpointPayload.regular(metadata);
            if (!metadataHash.equals(SmallRecord.hash(metadata))) throw new IOException("Partial metadata hash mismatch.");
            var progress = SmallRecord.read(metadata, "partial-generation-v1", PartialGeneration::read, MAX_BYTES);
            if (!stateHash.isEmpty()) SmallRecord.requireHash(stateHash);
            if (progress.training().updates() > 0 && progress.candidate().isEmpty() && stateHash.isEmpty())
                throw new IOException("Missing partial optimizer state.");
            return new Stored(progress, stateHash.isEmpty() ? null : directory.resolve("training.state"), stateHash);
        }));
    }
    byte[] encode() throws IOException {
        return SmallRecord.encode("partial-generation-v1", out -> {
            attempt.write(out); out.writeUTF(candidate); out.writeUTF(started.toString());
            out.writeBoolean(recoveryOnly); out.writeBoolean(generationSettingsKnown);
            out.writeLong(activeNanos); out.writeLong(selfPlayNanos); out.writeLong(trainingNanos); out.writeLong(validationNanos);
            var s = statistics;
            out.writeInt(s.requestedGames()); out.writeInt(s.completedGames()); out.writeInt(s.abortedGames());
            out.writeInt(s.whiteWins()); out.writeInt(s.draws()); out.writeInt(s.blackWins()); out.writeInt(s.cappedGames());
            out.writeLong(s.totalPlayedPlies()); out.writeInt(s.minimumCompletedPlies()); out.writeInt(s.maximumCompletedPlies());
            out.writeDouble(s.meanCompletedPlies()); out.writeLong(s.rawTrajectoryPositions()); out.writeInt(s.sampledPositions());
            var t = training;
            out.writeLong(t.samples()); out.writeLong(t.updates()); out.writeLong(t.initialStep());
            out.writeDouble(t.initialLoss()); out.writeDouble(t.lossSum());
            out.writeInt(games.games().size());
            for (var g : games.games()) {
                out.writeInt(g.gameIndex()); out.writeLong(g.seed()); out.writeUTF(g.termination().name());
                out.writeInt(g.playedPlies()); out.writeInt(g.rawPositions()); out.writeInt(g.sampledPositions());
                out.writeUTF(g.failure() == null ? "" : g.failure());
            }
            out.writeInt(games.samples().size());
            for (var sample : games.samples()) { for (long b : sample.board()) out.writeLong(b); out.writeDouble(sample.target()); }
            out.writeInt(pairs.size());
            for (var p : pairs) { out.writeUTF(p.openingHash()); writeGame(out, p.candidateWhite()); writeGame(out, p.candidateBlack()); }
        }, MAX_BYTES);
    }
    private static PartialGeneration read(DataInputStream in) throws IOException {
        var attempt = GenerationAttempt.read(in); String candidate = in.readUTF(); Instant started = Instant.parse(in.readUTF());
        boolean recoveryOnly = in.readBoolean(), generationSettingsKnown = in.readBoolean();
        long active = in.readLong(), self = in.readLong(), train = in.readLong(), validation = in.readLong();
        var statistics = new SelfPlayBatch.Statistics(in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(),
                in.readInt(), in.readInt(), in.readLong(), in.readInt(), in.readInt(), in.readDouble(), in.readLong(), in.readInt());
        var cursor = new SelfPlayControl.TrainingCursor(in.readLong(), in.readLong(), in.readLong(), in.readDouble(), in.readDouble());
        var games = new ArrayList<SelfPlayBatch.GameSummary>();
        for (int n = count(in); n > 0; n--) games.add(new SelfPlayBatch.GameSummary(in.readInt(), in.readLong(),
                GameTermination.valueOf(in.readUTF()), in.readInt(), in.readInt(), in.readInt(), in.readUTF()));
        var samples = new ArrayList<TrajectorySampler.Sample>();
        for (int n = count(in); n > 0; n--) {
            long[] board = new long[Board.MAX_BITBOARDS]; for (int i = 0; i < board.length; i++) board[i] = in.readLong();
            samples.add(new TrajectorySampler.Sample(board, in.readDouble()));
        }
        var pairs = new ArrayList<ValidationResult.Pair>();
        for (int n = count(in); n > 0; n--) pairs.add(new ValidationResult.Pair(in.readUTF(), readGame(in), readGame(in)));
        return new PartialGeneration(attempt, candidate, new SelfPlayBatch.Saved(games, samples), statistics, cursor, pairs,
                started, active, self, train, validation, recoveryOnly, generationSettingsKnown);
    }
    private static int count(DataInputStream in) throws IOException {
        int n = in.readInt(); if (n < 0 || n > in.available()) throw new IOException("Invalid partial count."); return n;
    }
    private static void writeGame(DataOutputStream out, ValidationResult.Game game) throws IOException {
        out.writeUTF(game.termination().name()); out.writeInt(game.plies());
    }
    private static ValidationResult.Game readGame(DataInputStream in) throws IOException {
        return new ValidationResult.Game(GameTermination.valueOf(in.readUTF()), in.readInt());
    }
}
