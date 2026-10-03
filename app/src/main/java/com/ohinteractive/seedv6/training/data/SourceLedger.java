package com.ohinteractive.seedv6.training.data;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Single atomic reservation/cursor record under CheckpointStore's exclusive lineage lock.
 * Reserving advances next-unallocated and stores the ranges in the SAME forced replacement.
 * Pending reservations are replayed, never allocated again; all abandoned ranges remain inspectable.
 */
public final class SourceLedger {
    public enum Status { RESERVED, COMPLETED, ABANDONED }
    public record Range(String source, long start, long end, int training, int validation, long skipped) {
        public Range {
            if (source == null || !source.matches("[0-9a-f]{64}") || start < 0 || end < start || training < 0 || validation < 0
                    || skipped < 0 || end - start != (long) training + validation + skipped) throw new IllegalArgumentException("Invalid source range");
        }
    }
    public record Reservation(String id, long generation, String attempt, String mix, List<Range> ranges,
                              String trainingHash, String validationHash, Status status, String reason) {
        public Reservation {
            UUID.fromString(id);
            if (generation < 1 || attempt == null || mix == null || ranges == null || ranges.isEmpty() || status == null || reason == null
                    || !trainingHash.matches("[0-9a-f]{64}") || !validationHash.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Invalid reservation");
            ranges = List.copyOf(ranges);
        }
        Reservation with(Status value, String reason) { return new Reservation(id, generation, attempt, mix, ranges, trainingHash, validationHash, value, reason); }
    }
    public record State(int version, Map<String, Long> next, List<Reservation> reservations, String checksum) {
        public State {
            if (version != 1 || next == null || reservations == null || checksum == null
                    || !checksum.equals(checksum(next, reservations))) throw new IllegalArgumentException("Invalid Training Data cursor ledger");
            next = Collections.unmodifiableMap(new TreeMap<>(next)); reservations = List.copyOf(reservations);
            Map<String, Long> expected = new TreeMap<>(); int active = 0;
            for (var reservation : reservations) {
                if (reservation.status() == Status.RESERVED) active++;
                for (var range : reservation.ranges()) {
                    if (range.start() != expected.getOrDefault(range.source(), 0L)) throw new IllegalArgumentException("Noncontiguous source reservations");
                    expected.put(range.source(), range.end());
                }
            }
            if (!next.equals(expected) || active > 1) throw new IllegalArgumentException("Cursor/reservation mismatch");
        }
        private static String checksum(Map<String, Long> next, List<Reservation> records) {
            return DataFiles.hash("sequential-ledger-v1:" + new TreeMap<>(next) + records);
        }
        State(Map<String, Long> next, List<Reservation> records) { this(1, next, records, checksum(next, records)); }
    }
    private final Path file;
    private State state;
    public SourceLedger(Path lineage) throws IOException {
        file = DataSources.directory(lineage).resolve("cursors.json");
        if (Files.exists(file.getParent().resolve("configuration.json")) && !Files.isRegularFile(file))
            throw new IOException("Training Data cursor ledger is missing from this established campaign. Restore it before consumption; no cursor was reset.");
        state = Files.exists(file) ? DataFiles.read(file, State.class) : new State(Map.of(), List.of());
    }
    /** Establish the ledger before publishing any sequential campaign configuration. Caller holds the store lock. */
    public void initialize() throws IOException { if (!Files.exists(file)) save(state); }
    public State state() { return state; }
    public long next(String source) { return state.next().getOrDefault(source, 0L); }
    public Optional<Reservation> active() { return state.reservations().stream().filter(r -> r.status() == Status.RESERVED).findFirst(); }
    public Optional<Reservation> generation(long generation) {
        return state.reservations().reversed().stream().filter(r -> r.generation() == generation && r.status() != Status.ABANDONED).findFirst();
    }
    public Reservation reserve(long generation, String attempt, String mix, List<Range> ranges, String trainingHash, String validationHash) throws IOException {
        if (active().isPresent()) throw new IOException("An active Training Data reservation must be resumed or explicitly abandoned");
        var reservation = new Reservation(UUID.randomUUID().toString(), generation, attempt, mix, ranges, trainingHash, validationHash, Status.RESERVED, "");
        var next = new TreeMap<>(state.next());
        for (var range : ranges) {
            if (range.start() != next(range.source())) throw new IOException("Source cursor changed before reservation");
            next.put(range.source(), range.end());
        }
        var records = new ArrayList<>(state.reservations()); records.add(reservation);
        save(new State(next, records)); return reservation;
    }
    public void complete(long generation) throws IOException {
        var active = active();
        if (active.isEmpty()) return;
        if (active.get().generation() > generation) return; // Startup settles the previous candidate before resuming this attempt.
        if (active.get().generation() != generation) throw new IOException("Completion does not match Training Data reservation");
        replace(active.get(), active.get().with(Status.COMPLETED, "Candidate decision durably recorded"));
    }
    /** Called only after a forced GenerationRestart intent, including crash recovery. */
    public static void abandonForRestart(Path lineage, long generation, String restart) throws IOException {
        var ledger = new SourceLedger(lineage); var active = ledger.active();
        if (active.isEmpty()) return;
        if (active.get().generation() != generation) throw new IOException("Restart/reservation generation mismatch");
        ledger.replace(active.get(), active.get().with(Status.ABANDONED, "Explicit settings restart: " + restart));
    }
    private void replace(Reservation old, Reservation updated) throws IOException {
        var records = new ArrayList<>(state.reservations()); records.set(records.indexOf(old), updated);
        save(new State(state.next(), records));
    }
    private void save(State value) throws IOException { DataFiles.write(file, value); state = value; }
}
