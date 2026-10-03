package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.checkpoint.GenerationAttempt;
import com.ohinteractive.seedv6.corpus.CorpusPreparation;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Bounded generation acquisition behind the existing architecture trainer/validation interface. */
public final class SequentialTraining extends CorpusTraining {
    private final TrainerConfig configuration;
    private final TrainingSource source;
    private final DataSources sources;
    private final Path directory;
    private final CorpusPreparation control;
    private final TargetPolicy adapter;
    private final long maximumDecoded;
    public record Metrics(long decodedRecords, long seekRecords, long skippedRecords) {}
    private Metrics metrics = new Metrics(0, 0, 0);
    public Metrics metrics() { return metrics; }
    public static boolean legacyResume(TrainerConfig config, TrainingSource source) throws IOException {
        if (!source.corpus() || source.dataSources() || !Files.exists(config.checkpointRoot().resolve("corpus-training/campaign.json"))) return false;
        var attempt = GenerationAttempt.inspect(config.checkpointRoot());
        return attempt.isPresent() && attempt.get().source().equals(source)
                && Files.isRegularFile(config.checkpointRoot().resolve("corpus-training/generation-" + attempt.get().generation() + ".json"));
    }

    public static DataSources selection(TrainerConfig config, TrainingSource source) throws IOException {
        if (source.dataSources()) return DataSources.read(source.corpusRoot());
        // New legacy-source consumers get direct indexed access. Historical permutation campaigns
        // have no reliable sequential cursor; never invent one from generation * requested count.
        if (Files.exists(config.checkpointRoot().resolve("corpus-training/campaign.json")))
            throw new IOException("This lineage predates sequential Training Data cursors. In Training Data, register its source and acknowledge unknown previous usage before starting a new sequential campaign. Existing checkpoints, receipts and unfinished work are preserved.");
        Path registered = DataSources.directory(config.checkpointRoot()).resolve("sources.json");
        if (Files.exists(registered)) {
            var existing = DataSources.read(registered.getParent());
            if (existing.sources().size() == 1 && existing.sources().getFirst().path().toAbsolutePath().normalize().equals(source.corpusRoot().toAbsolutePath().normalize())) return existing;
        }
        return new DataSources(1, List.of(DataSource.register("Legacy Seed Training Data", source.requireCorpusRoot(config.checkpointRoot()), 1)), false);
    }
    public SequentialTraining(TrainerConfig config, TrainingSource source, CorpusPreparation control) throws IOException {
        this(config, source, control, Long.MAX_VALUE);
    }
    /** Additional explicit diagnostic bound; normal acquisition is bounded by requested positions and skip budget. */
    public SequentialTraining(TrainerConfig config, TrainingSource source, CorpusPreparation control, long maximumDecoded) throws IOException {
        if (maximumDecoded < 1) throw new IllegalArgumentException("Invalid source record limit");
        this.maximumDecoded = maximumDecoded;
        this.source = source; this.control = control;
        directory = DataSources.directory(config.checkpointRoot()); adapter = targetPolicy(config.architecture());
        sources = selection(config, source); sources.verify();
        if (config.corpusTraining() == null) {
            Path current = directory.resolve("configuration.json");
            if (!Files.exists(current)) throw new IOException("Set Positions / generation before training");
            config = config.withCorpusTraining(DataFiles.read(current, CorpusTrainingConfig.class));
        }
        configuration = config;
        if (!config.corpusTraining().viewIdentity().isEmpty() && !config.corpusTraining().viewIdentity().equals(sources.identity()))
            throw new IOException("Training Data source mix differs from saved generation configuration");
        if (Files.exists(config.checkpointRoot().resolve("corpus-training/campaign.json")) && !sources.legacyProgressAcknowledged())
            throw new IOException("Previous source usage is unknown. Acknowledge the sequential start in Training Data; no consumed counts were inferred.");
        new SourceLedger(config.checkpointRoot()).initialize();
        if (!source.dataSources()) sources.save(directory);
        sources.archiveSelection(directory);
        DataFiles.write(directory.resolve("configuration.json"), config());
    }
    @Override public CorpusTrainingConfig config() { return new CorpusTrainingConfig(configuration.corpusTraining().positionsPerGeneration(), sources.identity()).forArchitecture(configuration.architecture()); }
    @Override public Pin pin() { throw new UnsupportedOperationException("Sequential sources use source identities and range reservations"); }
    @Override public Batch batch(long generation) throws IOException { return acquire(generation, false); }
    @Override public Examples validation(long generation) throws IOException { return acquire(generation, true).validation(); }
    private Batch acquire(long generation, boolean validationOnly) throws IOException {
        sources.verify(); control.checkCancelled();
        var ledger = new SourceLedger(configuration.checkpointRoot());
        var saved = ledger.generation(generation).orElse(null);
        String attempt = DataFiles.hash(GenerationAttempt.inspect(configuration.checkpointRoot()).map(Object::toString)
                .orElse(configuration.withCorpusTraining(config()).attemptSettings(generation, source)));
        if (saved != null && saved.status() == SourceLedger.Status.COMPLETED) attempt = saved.attempt();
        if (saved != null && (!saved.mix().equals(sources.identity()) || !saved.attempt().equals(attempt)))
            throw new IOException("Saved source ranges belong to different generation settings; use the explicit restart lifecycle");
        if (validationOnly && saved == null) throw new IOException("Missing active-generation Training Data ranges");
        int count = config().positionsPerGeneration(), held = configuration.heldOut(source) ? (int) Math.max(2, (count + 3L) / 4) : 0;
        int[] trainingCounts = sources.allocate(count), validationCounts = sources.allocate(held);
        var training = new Examples(adapter); var validation = new Examples(adapter);
        MessageDigest trainHash = DataFiles.digest(), validationHash = DataFiles.digest();
        var ranges = new ArrayList<SourceLedger.Range>();
        long decoded = 0, seek = 0, skippedTotal = 0;
        ByteBuffer hashBuffer = ByteBuffer.allocate(60);
        for (int i = 0; i < sources.sources().size(); i++) {
            DataSource item = sources.sources().get(i);
            byte[] identityBytes = item.identity().getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            String progressStage = "Reading Training Data: " + item.name();
            int train = trainingCounts[i], valid = validationCounts[i];
            if (train + valid == 0) continue;
            var prior = saved == null ? null : saved.ranges().stream().filter(r -> r.source().equals(item.identity())).findFirst()
                    .orElseThrow(() -> new IOException("Missing source in saved reservation"));
            long start = prior == null ? ledger.next(item.identity()) : prior.start(), skipped = 0;
            long budget = Math.addExact(1000, Math.multiplyExact(100L, (long) train + valid));
            try (var reader = SourceReaders.open(item, directory.resolve("seek"), start)) {
                for (int retained = 0; retained < train + valid;) {
                    control.checkCancelled();
                    if (decoded + reader.nextPosition() - start >= maximumDecoded) throw new IOException("Training Data diagnostic source-record limit reached");
                    if (reader.nextPosition() - start >= budget) throw new IOException("Too many unusable records in Training Data source " + item.name() + "; bounded extraction stopped without a reservation");
                    if (prior != null && reader.nextPosition() >= prior.end()) throw new IOException("Saved Training Data range no longer supplies its examples");
                    var entry = reader.next();
                    if (entry == null) throw new EOFException("Training Data source exhausted: " + item.name() + " at position " + reader.nextPosition() + ". No wrapping or source substitution is permitted.");
                    if (entry.position() == null || adapter.rejection(entry.position()) != null) { skipped++; continue; }
                    boolean heldOut = retained >= train;
                    // No architecture-specific permanent records. Existing optimizers require a generation list
                    // for repeated NNUE epochs / safe batch resume; feature encoding stays in those trainers.
                    if (heldOut) validation.add(entry.position()); else if (!validationOnly) training.add(entry.position());
                    hash(heldOut ? validationHash : trainHash, identityBytes, hashBuffer, entry.ordinal(), entry.position());
                    retained++;
                    control.report(progressStage, retained, (long) train + valid);
                }
                var range = new SourceLedger.Range(item.identity(), start, reader.nextPosition(), train, valid, skipped);
                if (prior != null && !prior.equals(range)) throw new IOException("Training Data range changed since reservation");
                ranges.add(range); decoded += reader.decodedRecords(); seek += reader.seekRecords(); skippedTotal += skipped;
            }
        }
        String trainedHash = HexFormat.of().formatHex(trainHash.digest()), heldHash = HexFormat.of().formatHex(validationHash.digest());
        if (saved != null && (!saved.trainingHash().equals(trainedHash) || !saved.validationHash().equals(heldHash)))
            throw new IOException("Training Data contents differ from reserved generation");
        control.checkCancelled(); sources.verify();
        if (saved == null) ledger.reserve(generation, attempt, sources.identity(), ranges, trainedHash, heldHash);
        metrics = new Metrics(decoded, seek, skippedTotal);
        var evidence = new Evidence(source.generatorStore(), sources.identity(), configuration.masterSeed(), generation, count,
                (long) count + held, count, held, trainedHash, heldHash, decoded, Map.of("source-records-skipped", skippedTotal),
                adapter == TargetPolicy.BASIC_V1 ? null : adapter.identity, training.mates(), validation.mates());
        Path receipt = configuration.checkpointRoot().resolve("corpus-training/generation-" + generation + ".json");
        if (!Files.exists(receipt)) DataFiles.write(receipt, evidence);
        else {
            var prior = evidence(configuration.checkpointRoot(), generation);
            if (!prior.trainingHash().equals(trainedHash) || !prior.heldOutHash().equals(heldHash)) throw new IOException("Generation receipt differs from reserved positions");
            evidence = prior;
        }
        return new Batch(training, validation, evidence);
    }
    private static void hash(MessageDigest digest, byte[] source, ByteBuffer bytes, long ordinal, TrainingPosition position) {
        digest.update(source);
        bytes.clear().putLong(ordinal);
        var p = position.position();
        bytes.putLong(p.plane0()).putLong(p.plane1()).putLong(p.plane2()).putLong(p.plane3()).putInt(p.rules()).putInt(p.halfmove());
        bytes.putInt(position.targetKind()).putInt(position.target()).putInt(position.perspective()); digest.update(bytes.array());
    }
    @Override public void complete(long generation) throws IOException { new SourceLedger(configuration.checkpointRoot()).complete(generation); }
    @Override public void close() { }
}
