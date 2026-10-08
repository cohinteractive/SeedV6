package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.training.validation.PromotionPolicy;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** GUI session budget, independent of optimizer/checkpoint identity. Writes require the store lock.
 * A safe explicit stop ends a session. A process failure leaves ACTIVE recoverable on explicit Start.
 * Settled checkpoint evidence closes the crash gap between a decision and a session-record write.
 */
public record TrainingSession(int version, UUID id, UUID lineageId, long baseline, long target,
                              long completed, State state) {
    public enum State { ACTIVE, STOPPED, COMPLETED }
    public static final String FILE = "training-session.json";
    public TrainingSession {
        Objects.requireNonNull(id); Objects.requireNonNull(lineageId); Objects.requireNonNull(state);
        if (version != 1 || baseline < 0 || target < 0 || completed < 0 || target > 0 && completed > target)
            throw new IllegalArgumentException("Invalid training session budget");
        Math.addExact(baseline, target);
    }
    public static Optional<TrainingSession> read(Path root) throws IOException {
        return Files.exists(root.resolve(FILE)) ? Optional.of(DataFiles.read(root.resolve(FILE), TrainingSession.class)) : Optional.empty();
    }
    public long remaining() { return target == 0 ? Long.MAX_VALUE : target - completed; }
    public boolean exhausted() { return target > 0 && completed == target; }
    public long finalGeneration() { return target == 0 ? 0 : baseline + target; }
    public String description() {
        return (target == 0 ? "Unlimited" : completed + " / " + target + " completed; " + remaining() + " remaining")
                + " | " + state;
    }
    public void save(Path root) throws IOException { DataFiles.write(root.resolve(FILE), this); }
    static TrainingSession open(CheckpointStore store, long target) throws IOException {
        var lineage = TrainingLineage.read(store.root()).orElseThrow(() -> new IOException("A session requires a stable lineage identity."));
        long settled = settledGeneration(store);
        var previous = read(store.root());
        if (previous.isPresent() && !previous.get().lineageId().equals(lineage.id()))
            throw new IOException("Session belongs to another lineage; preserved without replacement.");
        TrainingSession result;
        if (previous.isPresent() && previous.get().state() == State.ACTIVE) {
            result = previous.get().reconcile(settled);
        } else result = new TrainingSession(1, UUID.randomUUID(), lineage.id(), settled, target, 0, State.ACTIVE);
        result.save(store.root());
        return result;
    }
    static Optional<TrainingSession> stopPending(CheckpointStore store) throws IOException {
        var previous = read(store.root()).filter(s -> s.state() == State.ACTIVE);
        if (previous.isEmpty()) return Optional.empty();
        var lineage = TrainingLineage.read(store.root()).orElseThrow(() -> new IOException("Missing session lineage identity."));
        if (!previous.get().lineageId().equals(lineage.id())) throw new IOException("Session belongs to another lineage.");
        var stopped = previous.get().reconcile(settledGeneration(store)).end();
        stopped.save(store.root()); return Optional.of(stopped);
    }
    TrainingSession reconcile(long settled) throws IOException {
        long count = settled - baseline;
        if (count < completed || count < 0 || target > 0 && count > target)
            throw new IOException("Settled lineage history disagrees with the session budget.");
        return new TrainingSession(version, id, lineageId, baseline, target, count,
                target > 0 && count == target ? State.COMPLETED : state);
    }
    TrainingSession end() {
        return new TrainingSession(version, id, lineageId, baseline, target, completed,
                exhausted() ? State.COMPLETED : State.STOPPED);
    }
    static long settledGeneration(CheckpointStore store) throws IOException {
        String latest = CheckpointInspection.reference(store.root(), "latest-training");
        var manifest = CheckpointInspection.manifest(store.root().resolve("checkpoints").resolve(latest));
        if (manifest.parentId().isEmpty()) return manifest.generation();
        var decision = store.validationFor(latest);
        return decision.isPresent() && (decision.get().decision() != PromotionPolicy.Decision.PROMOTE
                || latest.equals(CheckpointInspection.reference(store.root(), "best")))
                ? manifest.generation() : manifest.generation() - 1;
    }
}
