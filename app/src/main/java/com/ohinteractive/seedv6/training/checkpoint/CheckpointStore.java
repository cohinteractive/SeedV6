package com.ohinteractive.seedv6.training.checkpoint;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.*;
import java.util.*;
import com.ohinteractive.seedv6.core.nnue.NnueFeatureSchema;
import com.ohinteractive.seedv6.core.brn.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.training.nnue.NnueTrainer;
import com.ohinteractive.seedv6.training.nnue.AdamHyperparameters;
import com.ohinteractive.seedv6.training.validation.PromotionPolicy;
import com.ohinteractive.seedv6.training.validation.ValidationResult;
import com.ohinteractive.seedv6.training.service.TrainingSource;
import static com.ohinteractive.seedv6.training.checkpoint.CheckpointManifest.*;

/**
 * Explicit-path, single-owner checkpoint store. Holds an OS file lock until close, including validation.
 * Callers must exclusively own the trainer at an optimizer boundary. Never used by ordinary engine startup.
 * Files are forced, publication and reference replacement require ATOMIC_MOVE with no non-atomic fallback.
 * Directory force is attempted, but unavailable on some providers (notably Windows); atomic visibility and
 * corruption-detecting recovery are guaranteed by this protocol, universal power-loss durability is not.
 */
public final class CheckpointStore implements AutoCloseable {
    public record Checkpoint(CheckpointManifest manifest, NetworkModel model) {
        public Checkpoint(CheckpointManifest manifest, NnueNetwork network) { this(manifest, new NetworkModel.Nnue(network)); }
        public NnueNetwork network() { return model.nnue(); }
    }
    public record HistoricalCheckpoint(CheckpointManifest manifest, boolean materialized,
                                       AdamHyperparameters optimizer) {}
    public record Recovery(Optional<Checkpoint> best, Optional<Checkpoint> latestTraining,
                           Optional<PromotionRecord> bestEvidence, List<String> diagnostics) {
        public Recovery { diagnostics = List.copyOf(diagnostics); }
    }
    private record Loaded(Checkpoint checkpoint, NetworkTrainingState trainer) {}
    private record Reference(String checkpointId, String evidenceId) {}
    @FunctionalInterface interface AtomicMover { void move(Path source, Path destination, boolean replace) throws IOException; }
    @FunctionalInterface private interface ArtifactWriter { void write(OutputStream out) throws IOException; }

    private final Path root;
    private final AtomicMover mover;
    private final FileChannel lockChannel;
    private final FileLock lock;
    private TrainingArchitecture expectedArchitecture;
    private boolean closed;
    private boolean directoryForceSupported = true;

    public CheckpointStore(Path root) throws IOException { this(root, CheckpointStore::atomicMove); }

    public CheckpointStore(Path root, TrainingArchitecture architecture) throws IOException {
        this(root, CheckpointStore::atomicMove, Objects.requireNonNull(architecture), CheckpointInspection.freshRoot(root, architecture));
        try { requireArchitecture(Objects.requireNonNull(architecture)); }
        catch (IOException | RuntimeException failure) { close(); throw failure; }
    }

    /** Check identity before any recovery/reference mutation. Unreadable artifacts remain recovery diagnostics. */
    public void requireArchitecture(TrainingArchitecture requested) throws IOException {
        requireOpen();
        TrainingArchitecture found = expectedArchitecture;
        if (requested != null && found != null && requested != found) throw mismatch(requested, found);
        if (requested != null) found = requested;
        try (var paths = entries("checkpoints")) {
            for (Path path : paths) {
                CheckpointManifest manifest;
                try { manifest = CheckpointInspection.manifest(path); }
                catch (IOException invalid) { continue; } // Existing corruption/recovery handling is unchanged.
                if (found != null && found != manifest.architecture()) throw mismatch(found, manifest.architecture());
                found = manifest.architecture();
            }
        }
        expectedArchitecture = found;
    }

    private static IOException mismatch(TrainingArchitecture expected, TrainingArchitecture actual) {
        return new IOException("Checkpoint architecture mismatch: selected " + expected + ", store contains " + actual
                + ". Select a separate empty folder for a new lineage; existing data was preserved.");
    }

    // Failure-injection seam for atomic publication/reference crash tests.
    CheckpointStore(Path root, AtomicMover mover) throws IOException {
        this(root, mover, null, false);
    }

    private CheckpointStore(Path root, AtomicMover mover, TrainingArchitecture architecture, boolean fresh) throws IOException {
        this.root = Objects.requireNonNull(root, "explicit checkpoint root").toAbsolutePath().normalize();
        this.mover = Objects.requireNonNull(mover);
        Files.createDirectories(this.root);
        lockChannel = FileChannel.open(this.root.resolve("store.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock acquired;
        try {
            acquired = lockChannel.tryLock();
            if (acquired == null) throw new IOException("Checkpoint root is already open by another owner.");
        } catch (IOException | OverlappingFileLockException failure) {
            lockChannel.close();
            throw new IOException("Cannot acquire exclusive checkpoint-store ownership.", failure);
        }
        lock = acquired;
        try {
            if (fresh && Files.notExists(this.root.resolve(CheckpointInspection.BOOTSTRAP_IDENTITY))) {
                writeBytes(this.root.resolve(CheckpointInspection.BOOTSTRAP_IDENTITY),
                        SmallRecord.encode("bootstrap-identity-v1", out -> out.writeUTF(architecture.name())));
                forceDirectory(this.root);
            }
            for (String name : List.of("checkpoints", "staging", "validations", "promotions", "refs")) {
                Files.createDirectories(this.root.resolve(name));
            }
            try (var access = PayloadAccess.acquire(this.root)) { /* Establish reader coordination. */ }
            forceDirectory(this.root);
            GenerationRestart.recover(this);
        } catch (IOException | RuntimeException failure) {
            lock.release(); lockChannel.close(); throw failure;
        }
    }

    public Path root() { return root; }
    public Optional<GenerationAttempt> generationAttempt() throws IOException {
        requireOpen();
        Path path = root.resolve(GenerationAttempt.FILE);
        return Files.notExists(path) ? Optional.empty() : Optional.of(GenerationAttempt.read(path));
    }
    public void writeGenerationAttempt(GenerationAttempt attempt) throws IOException {
        requireOpen();
        Path temporary = root.resolve("staging").resolve("attempt-" + UUID.randomUUID());
        writeBytes(temporary, attempt.encode());
        mover.move(temporary, root.resolve(GenerationAttempt.FILE), true); forceDirectory(root);
    }
    public void restartGeneration(GenerationAttempt previous, GenerationAttempt replacement, String candidate) throws IOException {
        requireOpen(); GenerationRestart.start(this, previous, replacement, candidate);
    }
    void restoreGenerationParent(String parent) throws IOException {
        writeReference("latest-training", new Reference(parent, ""));
    }
    void moveGenerationArtifact(Path from, Path to) throws IOException {
        if (!from.normalize().startsWith(root) || !to.normalize().startsWith(root)) throw new IOException("Archive outside store.");
        mover.move(from, to, false); forceDirectory(from.getParent()); forceDirectory(to.getParent());
    }
    public static final String TRAINING_SOURCE_FILE = "training-source.bin";

    /** Absent selection is the legacy self-play regime, never an implicit conversion. */
    public static Optional<TrainingSource> readTrainingSource(Path root) throws IOException {
        Path file = root.resolve(TRAINING_SOURCE_FILE);
        if (Files.notExists(file)) return Optional.empty();
        return Optional.of(SmallRecord.read(file, "training-source-v1", in ->
                new TrainingSource(TrainingSource.Mode.valueOf(in.readUTF()), in.readUTF())));
    }
    public void writeTrainingSource(TrainingSource source) throws IOException {
        requireOpen();
        if (expectedArchitecture == TrainingArchitecture.NNUE && source.bootstrap()) throw new IOException("NNUE cannot be a bootstrap student.");
        byte[] bytes = SmallRecord.encode("training-source-v1", out -> { out.writeUTF(source.mode().name()); out.writeUTF(source.generatorStore()); });
        Path temporary = root.resolve("staging").resolve("source-" + UUID.randomUUID());
        writeBytes(temporary, bytes);
        mover.move(temporary, root.resolve(TRAINING_SOURCE_FILE), true);
        forceDirectory(root);
    }
    public Optional<BootstrapPlan> bootstrapPlan(String parent) throws IOException {
        requireOpen(); requireCheckpointId(parent);
        Path path = root.resolve("bootstrap").resolve(parent + ".plan");
        if (Files.notExists(path)) return Optional.empty();
        var plan = BootstrapPlan.read(path);
        if (!plan.parentId().equals(parent) || plan.generation() != historicalManifest(root, parent).generation() + 1)
            throw new IOException("Bootstrap plan parent/generation mismatch.");
        return Optional.of(plan);
    }
    public void writeBootstrapPlan(BootstrapPlan plan) throws IOException {
        requireOpen();
        if (expectedArchitecture == TrainingArchitecture.NNUE) throw new IOException("NNUE cannot be a bootstrap student.");
        Files.createDirectories(root.resolve("bootstrap"));
        publishRecord("bootstrap", plan.parentId() + ".plan", plan.encode());
    }
    public Optional<BootstrapData> bootstrapData(BootstrapPlan plan) throws IOException {
        requireOpen();
        Path path = root.resolve("bootstrap").resolve(plan.parentId() + ".data");
        if (Files.notExists(path)) return Optional.empty();
        var data = BootstrapData.read(path);
        if (!data.planHash().equals(plan.hash())) throw new IOException("Bootstrap data/plan mismatch.");
        return Optional.of(data);
    }
    public void writeBootstrapData(BootstrapPlan plan, BootstrapData data) throws IOException {
        requireOpen();
        if (!data.planHash().equals(plan.hash())) throw new IOException("Bootstrap data/plan mismatch.");
        publishRecord("bootstrap", plan.parentId() + ".data", data.encode());
    }
    /** A completed search with insufficient terminal data has no updates or Candidate to recover.
     * Preserve its pin as a failed attempt, atomically freeing the safe configuration boundary.
     */
    public void archiveInsufficientBootstrap(BootstrapPlan plan) throws IOException {
        requireOpen();
        if (!readReference("latest-training").checkpointId().equals(plan.parentId())
                || bootstrapData(plan).isPresent()) throw new IOException("Cannot archive a trained/published bootstrap attempt.");
        var active = bootstrapPlan(plan.parentId()).orElseThrow(() -> new IOException("Missing active bootstrap plan."));
        if (!active.equals(plan)) throw new IOException("Bootstrap plan changed.");
        Path directory = root.resolve("bootstrap");
        mover.move(directory.resolve(plan.parentId() + ".plan"),
                directory.resolve(plan.parentId() + "-" + UUID.randomUUID() + ".insufficient-plan"), false);
        forceDirectory(directory);
    }
    public boolean directoryForceSupported() { return directoryForceSupported; }

    /** Publish and move latest-training only. Generations must increase; duplicate identical content is idempotent. */
    public Checkpoint publish(NnueTrainer trainer, Metadata metadata) throws IOException {
        return publish(trainer.model().snapshot(), trainer, metadata);
    }

    public Checkpoint publish(NnueNetwork network, NnueTrainer trainer, Metadata metadata) throws IOException {
        return publish(new NetworkModel.Nnue(network), new NetworkTrainingState.Nnue(trainer), metadata);
    }

    public Checkpoint publish(NetworkTrainingState trainer, Metadata metadata) throws IOException {
        return publish(trainer.snapshot(), trainer, metadata);
    }

    private Checkpoint publish(NetworkModel network, NetworkTrainingState trainer, Metadata metadata) throws IOException {
        requireOpen();
        requireArchitecture(trainer.architecture());
        Objects.requireNonNull(metadata);
        requireSameModel(network, trainer.snapshot());
        String networkFile = network.architecture().networkFile();
        if (!metadata.parentId().isEmpty()
                && load(metadata.parentId()).manifest().generation() >= metadata.generation()) {
            throw new IOException("Parent must precede child generation.");
        }
        Path stage = Files.createTempDirectory(root.resolve("staging"), "checkpoint-");
        // Failed stages remain diagnostic evidence and are never scanned as checkpoints.
        writeArtifact(stage.resolve(networkFile), network::write);
        writeArtifact(stage.resolve(TRAINING_FILE), trainer::write);
        var manifest = CheckpointManifest.create(metadata, trainer.step(),
                SmallRecord.hash(stage.resolve(networkFile)), SmallRecord.hash(stage.resolve(TRAINING_FILE)), network.architecture());
        writeBytes(stage.resolve(MANIFEST_FILE), manifest.encode());
        writeBytes(stage.resolve(CheckpointPayload.CONFIG),
                CheckpointPayload.configuration(manifest, trainer.hyperparameters()));
        Loaded staged = readCheckpoint(stage, manifest.id());
        forceDirectory(stage);
        Path destination = root.resolve("checkpoints").resolve(manifest.id());
        Checkpoint highest = highestCheckpoint(new ArrayList<>());
        if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
            Checkpoint existing = load(manifest.id());
            if (!existing.manifest().equals(manifest)
                    || Files.mismatch(stage.resolve(networkFile), destination.resolve(networkFile)) != -1
                    || Files.mismatch(stage.resolve(TRAINING_FILE), destination.resolve(TRAINING_FILE)) != -1
                    || Files.mismatch(stage.resolve(MANIFEST_FILE), destination.resolve(MANIFEST_FILE)) != -1) {
                throw new IOException("Conflicting immutable checkpoint.");
            }
            // Only delete the exact temporary files created by this successful duplicate call.
            for (String file : List.of(networkFile, TRAINING_FILE, MANIFEST_FILE, CheckpointPayload.CONFIG)) Files.delete(stage.resolve(file));
            Files.delete(stage);
            if (highest != null && highest.manifest().id().equals(manifest.id())) {
                writeReference("latest-training", new Reference(manifest.id(), ""));
            }
            return existing;
        }
        if (metadata.generation() <= highestHistoricalGeneration()) {
            throw new IOException("New checkpoint generation must strictly exceed every valid completed generation.");
        }
        mover.move(stage, destination, false);
        forceDirectory(root.resolve("checkpoints"));
        forceDirectory(root.resolve("staging"));
        writeReference("latest-training", new Reference(manifest.id(), ""));
        return staged.checkpoint();
    }

    /** Explicit initial acceptance, not a promotion. Recovery can resume an interrupted identical bootstrap. */
    public Checkpoint initialize(NnueTrainer trainer, Metadata metadata) throws IOException {
        return initialize(new NetworkTrainingState.Nnue(trainer), metadata);
    }

    public Checkpoint initialize(NetworkTrainingState trainer, Metadata metadata) throws IOException {
        requireOpen();
        requireArchitecture(trainer.architecture());
        Recovery before = recover();
        if (before.best().isPresent()) throw new IOException("An incumbent is already accepted.");
        try (var files = Files.list(root.resolve("promotions"))) {
            if (files.findAny().isPresent()) throw new IOException("Existing acceptance history requires recovery, not rebootstrap.");
        }
        Checkpoint checkpoint = publish(trainer, metadata);
        PromotionRecord evidence = PromotionRecord.create(PromotionRecord.Kind.BOOTSTRAP, 0,
                metadata.generation(), checkpoint.manifest().id(), "", "", "");
        publishRecord("promotions", evidence.id(), evidence.encode());
        writeReference("best", new Reference(checkpoint.manifest().id(), evidence.id()));
        return checkpoint;
    }

    public Checkpoint load(String id) throws IOException {
        requireOpen();
        var checkpoint = load(root, id);
        if (expectedArchitecture != null && checkpoint.manifest().architecture() != expectedArchitecture)
            throw mismatch(expectedArchitecture, checkpoint.manifest().architecture());
        return checkpoint;
    }

    private static Checkpoint load(Path root, String id) throws IOException {
        requireCheckpointId(id);
        try (var access = PayloadAccess.acquire(root)) {
            return readCheckpoint(root.resolve("checkpoints").resolve(id), id).checkpoint();
        }
    }

    /** Fresh mutable exact Adam/model state. No optimizer moments or hyperparameters are reset. */
    public NnueTrainer resume(String id) throws IOException {
        var state = resumeState(id);
        if (state instanceof NetworkTrainingState.Nnue nnue) return nnue.trainer();
        throw mismatch(TrainingArchitecture.NNUE, state.architecture());
    }

    public NetworkTrainingState resumeState(String id) throws IOException {
        requireOpen();
        requireCheckpointId(id);
        try (var access = PayloadAccess.acquire(root)) {
            var state = readCheckpoint(root.resolve("checkpoints").resolve(id), id).trainer();
            if (expectedArchitecture != null && state.architecture() != expectedArchitecture)
                throw mismatch(expectedArchitecture, state.architecture());
            return state;
        }
    }

    /** Fresh service startup must not silently adopt or overwrite any earlier store artifacts. */
    public void requireEmptyForBootstrap() throws IOException {
        requireOpen();
        for (String category : List.of("checkpoints", "staging", "validations", "promotions", "refs")) {
            try (var paths = entries(category)) {
                if (paths.iterator().hasNext()) throw new IOException("Existing store requires resume, not fresh bootstrap.");
            }
        }
    }

    /**
     * Explicit service recovery of a directory publication whose latest reference replacement failed.
     * Ordinary recover() keeps its Workstream F reference preference. Only a verified descendant may
     * advance a still-valid latest reference here; staging is never eligible. Best remains evidence-only.
     */
    public Recovery recoverTrainingReferences() throws IOException {
        Recovery recovered = recover();
        if (recovered.best().isEmpty()) throw new IOException("No valid best/bootstrap evidence; cannot resume training.");
        Checkpoint highest = highestCheckpoint(new ArrayList<>());
        if (highest == null) throw new IOException("No durable training checkpoint.");
        if (recovered.latestTraining().isPresent()) {
            CheckpointManifest ancestor = highest.manifest();
            CheckpointManifest prior = recovered.latestTraining().get().manifest();
            while (ancestor.generation() > prior.generation()) {
                String parent = ancestor.parentId();
                if (parent.isEmpty()) throw new IOException("Disconnected training lineage.");
                CheckpointManifest next = historicalManifest(root, parent);
                if (next.generation() >= ancestor.generation()) throw new IOException("Invalid parent order.");
                ancestor = next;
            }
            if (!ancestor.id().equals(prior.id())) throw new IOException("Conflicting training lineage.");
        }
        writeReference("latest-training", new Reference(highest.manifest().id(), ""));
        return repairReferences();
    }

    /**
     * Find the one durable decision for an autonomous candidate without retaining history in memory.
     * Conflicting experiments or unreadable validation records fail closed: they cannot safely be
     * guessed to belong to some other candidate. Staging artifacts are deliberately excluded.
     */
    public Optional<ValidationRecord> validationFor(String candidateId) throws IOException {
        requireOpen(); requireCheckpointId(candidateId);
        ValidationRecord found = null;
        try (var paths = entries("validations")) {
            for (Path path : paths) {
                ValidationRecord record = readValidation(path.getFileName().toString());
                if (record.candidateId().equals(candidateId)) {
                    if (found != null && !found.id().equals(record.id())) throw new IOException("Ambiguous candidate validation history.");
                    found = record;
                }
            }
        }
        return Optional.ofNullable(found);
    }

    /**
     * Idempotent service boundary: finish existing promotion evidence before considering a new record.
     * A valid old best reference may survive a crash after evidence publication; ordinary recovery
     * intentionally keeps it. This operation verifies the exact previous acceptance before repairing it.
     */
    public PromotionRecord completePromotion(String validationId) throws IOException {
        requireOpen();
        ValidationRecord validation = readValidation(validationId);
        if (validation.decision() != PromotionPolicy.Decision.PROMOTE) throw new IOException("Promotion not authorized.");
        PromotionRecord found = null;
        try (var paths = entries("promotions")) {
            for (Path path : paths) {
                PromotionRecord record = validPromotion(path.getFileName().toString());
                if (record.validationId().equals(validationId)) {
                    if (found != null && !found.id().equals(record.id())) throw new IOException("Duplicate promotion evidence.");
                    found = record;
                }
            }
        }
        if (found == null) return promote(validationId);
        PromotionRecord current = recover().bestEvidence().orElseThrow(() -> new IOException("No accepted incumbent."));
        if (!current.id().equals(found.id()) && !current.id().equals(found.previousRecordId())) {
            throw new IOException("Promotion conflicts with current acceptance history.");
        }
        if (current.id().equals(found.id())) {
            try {
                Reference ref = readReference("best");
                if (ref.checkpointId().equals(found.checkpointId()) && ref.evidenceId().equals(found.id())) return found;
            } catch (IOException invalid) { /* Recovery found evidence; its missing/corrupt reference still needs repair. */ }
        }
        writeReference("best", new Reference(found.checkpointId(), found.id()));
        return found;
    }

    public ValidationRecord recordValidation(String candidateId, String incumbentId,
                                             ValidationResult result, PromotionPolicy policy) throws IOException {
        requireOpen();
        var candidate = load(candidateId); var incumbent = load(incumbentId);
        if (candidate.manifest().architecture() != incumbent.manifest().architecture())
            throw mismatch(candidate.manifest().architecture(), incumbent.manifest().architecture());
        var record = ValidationRecord.create(candidateId, incumbentId, result, policy);
        publishRecord("validations", record.id(), record.encode());
        return readValidation(record.id());
    }

    public ValidationRecord recordBootstrapValidation(String candidateId, BootstrapEvidence evidence) throws IOException {
        requireOpen();
        var candidate = load(candidateId);
        if (candidate.manifest().architecture() == TrainingArchitecture.NNUE) throw new IOException("NNUE cannot use held-out BRN promotion.");
        var plan = bootstrapPlan(candidate.manifest().parentId()).orElseThrow(() -> new IOException("Missing bootstrap plan."));
        var data = bootstrapData(plan).orElseThrow(() -> new IOException("Missing durable holdout."));
        if (!evidence.equals(BootstrapEvidence.create(plan, data, evidence.comparison()))) throw new IOException("Held-out evidence/input mismatch.");
        var incumbent = load(plan.incumbentId());
        if (incumbent.manifest().architecture() != candidate.manifest().architecture()) throw new IOException("Student architecture mismatch.");
        var record = ValidationRecord.create(candidateId, plan.incumbentId(), evidence);
        publishRecord("validations", record.id(), record.encode());
        return readValidation(record.id());
    }

    public ValidationRecord readValidation(String id) throws IOException {
        requireOpen();
        return readValidation(root, id);
    }

    private static ValidationRecord readValidation(Path root, String id) throws IOException {
        try { ValidationRecord.requireId(id); } catch (IllegalArgumentException invalid) { throw new IOException(invalid); }
        Path path = root.resolve("validations").resolve(id);
        ValidationRecord record = SmallRecord.read(path, "validation", in -> ValidationRecord.read(id, in));
        if (!id.equals("v-" + SmallRecord.hash(path))) throw new IOException("Validation identity mismatch.");
        return record;
    }

    /** Re-read durable evidence and verify current incumbent before advancing best. RETAIN/INCONCLUSIVE cannot enter. */
    public PromotionRecord promote(String validationId) throws IOException {
        requireOpen();
        ValidationRecord validation = readValidation(validationId);
        if (validation.decision() != PromotionPolicy.Decision.PROMOTE) {
            throw new IOException("Validation did not authorize promotion.");
        }
        Recovery current = recover();
        PromotionRecord prior = current.bestEvidence().orElseThrow(() -> new IOException("No accepted incumbent."));
        if (!prior.checkpointId().equals(validation.incumbentId())) throw new IOException("Stale incumbent validation.");
        Checkpoint candidate = load(validation.candidateId());
        if (candidate.manifest().generation() <= current.best().orElseThrow().manifest().generation()) {
            throw new IOException("Promotion must advance generation.");
        }
        long sequence = prior.sequence();
        try (var paths = entries("promotions")) {
            for (Path path : paths) {
                try { sequence = Math.max(sequence, validPromotion(path.getFileName().toString()).sequence()); }
                catch (IOException invalid) { /* Corrupt records remain untouched. */ }
            }
        }
        if (sequence == Long.MAX_VALUE) throw new IOException("Promotion sequence exhausted.");
        var record = PromotionRecord.create(PromotionRecord.Kind.PROMOTION, sequence + 1,
                candidate.manifest().generation(), validation.candidateId(), prior.checkpointId(), prior.id(), validation.id());
        publishRecord("promotions", record.id(), record.encode());
        validPromotion(record.id());
        writeReference("best", new Reference(record.checkpointId(), record.id()));
        return record;
    }

    /** Read-only recovery. A valid explicit reference wins, including the old best after an interrupted update. */
    public Recovery recover() throws IOException {
        requireOpen();
        requireArchitecture(expectedArchitecture);
        List<String> diagnostics = new ArrayList<>();
        Checkpoint latest = null, best = null;
        PromotionRecord bestRecord = null;
        try {
            Reference ref = readReference("latest-training");
            if (!ref.evidenceId().isEmpty()) throw new IOException("Unexpected latest-training evidence.");
            latest = load(ref.checkpointId());
        } catch (IOException invalid) { diagnostics.add("latest-training: " + invalid.getMessage()); }
        if (latest == null) latest = highestCheckpoint(diagnostics);
        try {
            Reference ref = readReference("best");
            bestRecord = validPromotion(ref.evidenceId());
            if (!ref.checkpointId().equals(bestRecord.checkpointId())) throw new IOException("Best reference/evidence mismatch.");
            best = load(ref.checkpointId());
        } catch (IOException invalid) {
            bestRecord = null;
            diagnostics.add("best: " + invalid.getMessage());
        }
        if (bestRecord == null) {
            try (var paths = entries("promotions")) {
                for (Path path : paths) {
                    try {
                        PromotionRecord record = validPromotion(path.getFileName().toString());
                        Checkpoint candidate = load(record.checkpointId());
                        if (bestRecord == null || comparePromotion(record, bestRecord) > 0) {
                            bestRecord = record; best = candidate;
                        }
                    } catch (CheckpointPrunedException retired) { /* History is not a recovery payload. */
                    } catch (IOException invalid) { diagnostic(diagnostics, path.getFileName() + ": " + invalid.getMessage()); }
                }
            }
        }
        return new Recovery(Optional.ofNullable(best), Optional.ofNullable(latest),
                Optional.ofNullable(bestRecord), diagnostics);
    }

    /** Explicit repair from validated in-memory recovery; never deletes corrupt checkpoints or records. */
    public Recovery repairReferences() throws IOException {
        Recovery recovered = recover();
        if (recovered.latestTraining().isPresent()) {
            writeReference("latest-training", new Reference(recovered.latestTraining().get().manifest().id(), ""));
        }
        if (recovered.bestEvidence().isPresent()) {
            PromotionRecord record = recovered.bestEvidence().get();
            writeReference("best", new Reference(record.checkpointId(), record.id()));
        }
        return recover();
    }

    private Checkpoint highestCheckpoint(List<String> diagnostics) throws IOException {
        List<CheckpointManifest> materialized = new ArrayList<>();
        try (var paths = entries("checkpoints")) {
            for (Path path : paths) {
                try {
                    CheckpointManifest manifest = historicalManifest(root, path.getFileName().toString());
                    if (!CheckpointPayload.pruned(path, manifest)) materialized.add(manifest);
                } catch (IOException invalid) { diagnostic(diagnostics, path.getFileName() + ": " + invalid.getMessage()); }
            }
        }
        materialized.sort(Comparator.comparingLong(CheckpointManifest::generation)
                .thenComparingLong(CheckpointManifest::optimizerStep).thenComparing(CheckpointManifest::id).reversed());
        for (var manifest : materialized) {
            try { return load(manifest.id()); }
            catch (IOException invalid) { diagnostic(diagnostics, manifest.id() + ": " + invalid.getMessage()); }
        }
        return null;
    }

    private long highestHistoricalGeneration() throws IOException {
        long highest = -1;
        try (var paths = entries("checkpoints")) {
            for (Path path : paths) {
                try { highest = Math.max(highest, historicalManifest(root, path.getFileName().toString()).generation()); }
                catch (IOException invalid) { /* Unknown/corrupt artifacts cannot establish identity. */ }
            }
        }
        return highest;
    }

    private PromotionRecord validPromotion(String id) throws IOException {
        return validPromotion(root, id);
    }

    private static PromotionRecord validPromotion(Path root, String id) throws IOException {
        PromotionRecord first = readPromotion(root, id);
        PromotionRecord current = first;
        // Strictly decreasing sequence/generation below already rules out cycles, in constant space.
        while (true) {
            CheckpointManifest accepted = historicalManifest(root, current.checkpointId());
            if (accepted.generation() != current.generation()) throw new IOException("Promotion generation mismatch.");
            if (current.kind() == PromotionRecord.Kind.BOOTSTRAP) return first;
            ValidationRecord validation = readValidation(root, current.validationId());
            if (validation.decision() != PromotionPolicy.Decision.PROMOTE
                    || !validation.candidateId().equals(current.checkpointId())
                    || !validation.incumbentId().equals(current.previousCheckpointId())) {
                throw new IOException("Promotion/validation mismatch.");
            }
            PromotionRecord previous = readPromotion(root, current.previousRecordId());
            var priorManifest = historicalManifest(root, previous.checkpointId());
            if (accepted.architecture() != priorManifest.architecture()) throw mismatch(accepted.architecture(), priorManifest.architecture());
            if (!previous.checkpointId().equals(current.previousCheckpointId())
                    || previous.sequence() >= current.sequence() || previous.generation() >= current.generation()) {
                throw new IOException("Invalid acceptance chain.");
            }
            current = previous;
        }
    }

    static PromotionRecord readPromotion(Path root, String id) throws IOException {
        try { PromotionRecord.requireId(id); } catch (IllegalArgumentException invalid) { throw new IOException(invalid); }
        Path path = root.resolve("promotions").resolve(id);
        PromotionRecord record = SmallRecord.read(path, "promotion", in -> PromotionRecord.read(id, in));
        if (!id.equals("p-" + SmallRecord.hash(path))) throw new IOException("Promotion identity mismatch.");
        return record;
    }

    /** Full payload integrity inspection under short-lived reader ownership; never opens a writer. */
    public static Checkpoint inspect(Path directory) throws IOException {
        try (var access = PayloadAccess.acquire(checkpointRoot(directory))) {
            return readCheckpoint(directory, directory.getFileName().toString()).checkpoint();
        }
    }

    public record AvailableCheckpoints(List<CheckpointManifest> checkpoints, List<String> diagnostics) {
        public AvailableCheckpoints {
            checkpoints = List.copyOf(checkpoints);
            diagnostics = List.copyOf(diagnostics);
        }
    }

    /**
     * All completed, loadable checkpoints, newest first, independent of acceptance/lineage.
     * Reuses full historical inspection and its reader/pruner lock for each entry; never owns
     * the trainer lock or retains payloads. This is a discovery snapshot, not a reservation:
     * callers must still load their selected identity under payload ownership before use.
     */
    public static AvailableCheckpoints availableCheckpoints(Path root) throws IOException {
        Objects.requireNonNull(root, "explicit checkpoint root");
        Path directory = root.resolve("checkpoints");
        if (Files.notExists(directory)) return new AvailableCheckpoints(List.of(), List.of());
        List<CheckpointManifest> available = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();
        try (var paths = Files.newDirectoryStream(directory)) {
            for (Path path : paths) {
                try {
                    HistoricalCheckpoint inspected;
                    try (var access = PayloadAccess.browse(root)) { inspected = inspectHistoricalOwned(path); }
                    if (inspected.materialized()) available.add(inspected.manifest());
                } catch (IOException invalid) {
                    diagnostics.add(path.getFileName() + ": " + invalid.getMessage());
                }
            }
        }
        available.sort(Comparator.comparingLong(CheckpointManifest::generation)
                .thenComparingLong(CheckpointManifest::optimizerStep).thenComparing(CheckpointManifest::id).reversed());
        diagnostics.sort(String::compareTo);
        return new AvailableCheckpoints(available, diagnostics);
    }

    /** Explicit immutable playing snapshot; no acceptance requirement and no fallback. */
    public static Checkpoint readSnapshot(Path root, String id) throws IOException {
        return load(root, id);
    }

    /** Benchmark/import access to a managed network participates in the same payload protocol. */
    public static byte[] readNetworkBytes(Path file) throws IOException {
        Path absolute = file.toAbsolutePath().normalize();
        // Checkpoint directories survive pruning, so resolve directory aliases without touching
        // the payload before acquiring ownership. Also recognize existing standalone file aliases.
        if (absolute.getParent() != null) absolute = absolute.getParent().toRealPath().resolve(absolute.getFileName());
        if (Files.isSymbolicLink(absolute)) absolute = absolute.toRealPath();
        Path directory = absolute.getParent();
        if ((absolute.getFileName().toString().equals(NETWORK_FILE)
                || absolute.getFileName().toString().equals(TrainingArchitecture.BRN.networkFile())
                || absolute.getFileName().toString().equals(TrainingArchitecture.BRN1.networkFile())
                || absolute.getFileName().toString().equals(TrainingArchitecture.BRN2.networkFile())) && directory != null
                && directory.getParent() != null && directory.getParent().getFileName() != null
                && directory.getParent().getFileName().toString().equals("checkpoints")) {
            try (var access = PayloadAccess.acquire(checkpointRoot(directory))) {
                readCheckpoint(directory, directory.getFileName().toString());
                return Files.readAllBytes(absolute);
            }
        }
        return Files.readAllBytes(absolute); // Standalone inputs are outside automatic store retention.
    }

    /** Full verification for materialized payloads; compact authenticated metadata for pruned history. */
    public static HistoricalCheckpoint inspectHistorical(Path directory) throws IOException {
        try (var access = PayloadAccess.acquire(checkpointRoot(directory))) {
            return inspectHistoricalOwned(directory);
        }
    }

    private static HistoricalCheckpoint inspectHistoricalOwned(Path directory) throws IOException {
        var manifest = CheckpointInspection.manifest(directory);
        if (CheckpointPayload.pruned(directory, manifest))
            return new HistoricalCheckpoint(manifest, false, CheckpointPayload.readConfiguration(directory, manifest));
        Loaded loaded = readCheckpoint(directory, manifest.id());
        return new HistoricalCheckpoint(manifest, true, loaded.trainer().hyperparameters());
    }

    private static Path checkpointRoot(Path directory) {
        Path parent = directory.toAbsolutePath().normalize().getParent();
        // Standalone checkpoint exports have no store pruner, but still coordinate their readers.
        return parent != null && parent.getFileName() != null && parent.getFileName().toString().equals("checkpoints")
                ? parent.getParent() : directory;
    }

    static CheckpointManifest historicalManifest(Path root, String id) throws IOException {
        requireCheckpointId(id);
        try (var access = PayloadAccess.acquire(root)) {
            Path directory = root.resolve("checkpoints").resolve(id);
            var manifest = CheckpointInspection.manifest(directory);
            if (!CheckpointPayload.pruned(directory, manifest)) CheckpointPayload.requireMaterialized(directory, manifest);
            return manifest;
        }
    }

    /**
     * Independent immutable playing snapshot, also available while a trainer owns the store lock.
     * Read the atomically replaced reference once, then validate only immutable completed artifacts
     * and their acceptance chain. A concurrent promotion may select the old or new best; it cannot
     * mix their identities. Never opens a writer, repairs references, or reads staging/candidate state.
     * Missing-reference recovery retains the existing evidence-only rule; a corrupt reference fails
     * closed. Payload ownership spans reference selection and loading, excluding concurrent pruning.
     */
    public static Checkpoint readBestSnapshot(Path root) throws IOException {
        Objects.requireNonNull(root, "explicit checkpoint root");
        try (var access = PayloadAccess.acquire(root)) { return readBestSnapshotOwned(root); }
    }

    private static Checkpoint readBestSnapshotOwned(Path root) throws IOException {
        final Reference ref;
        try {
            ref = readReference(root, "best");
        } catch (NoSuchFileException missing) {
            PromotionRecord recovered = null;
            try (var paths = Files.newDirectoryStream(root.resolve("promotions"))) {
                for (Path path : paths) {
                    try {
                        PromotionRecord record = validPromotion(root, path.getFileName().toString());
                        load(root, record.checkpointId());
                        if (recovered == null || comparePromotion(record, recovered) > 0) recovered = record;
                    } catch (IOException invalid) { /* Only completed, valid acceptance evidence is eligible. */ }
                }
            }
            if (recovered == null) throw new IOException("No recoverable accepted best network.", missing);
            return load(root, recovered.checkpointId());
        }
        PromotionRecord accepted = validPromotion(root, ref.evidenceId());
        if (!ref.checkpointId().equals(accepted.checkpointId())) throw new IOException("Best reference/evidence mismatch.");
        return load(root, ref.checkpointId());
    }

    private static Loaded readCheckpoint(Path directory, String id) throws IOException {
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Missing completed checkpoint: " + id);
        CheckpointPayload.regular(directory.resolve(MANIFEST_FILE));
        var manifest = SmallRecord.read(directory.resolve(MANIFEST_FILE), "manifest", CheckpointManifest::read);
        if (!manifest.id().equals(id)) throw new IOException("Checkpoint directory/manifest mismatch.");
        if (CheckpointPayload.pruned(directory, manifest)) throw new CheckpointPrunedException(id);
        for (String file : List.of(MANIFEST_FILE, manifest.networkFile(), TRAINING_FILE)) {
            if (!Files.isRegularFile(directory.resolve(file), LinkOption.NOFOLLOW_LINKS)) throw new IOException("Missing checkpoint artifact.");
        }
        Path networkPath = directory.resolve(manifest.networkFile()), trainingPath = directory.resolve(TRAINING_FILE);
        if (Files.size(networkPath) != manifest.networkBytes() || Files.size(trainingPath) != manifest.trainingBytes()
                || !SmallRecord.hash(networkPath).equals(manifest.networkSha256())
                || !SmallRecord.hash(trainingPath).equals(manifest.trainingSha256())) {
            throw new IOException("Checkpoint artifact length/SHA-256 mismatch.");
        }
        try (InputStream networkInput = new BufferedInputStream(Files.newInputStream(networkPath));
             InputStream trainingInput = new BufferedInputStream(Files.newInputStream(trainingPath))) {
            NetworkModel network = NetworkModel.read(manifest.architecture(), networkInput);
            NetworkTrainingState trainer = NetworkTrainingState.read(manifest.architecture(), trainingInput);
            if (trainer.step() != manifest.optimizerStep()) throw new IOException("Optimizer step mismatch.");
            if (Files.exists(directory.resolve(CheckpointPayload.CONFIG), LinkOption.NOFOLLOW_LINKS)
                    && !CheckpointPayload.readConfiguration(directory, manifest).equals(trainer.hyperparameters()))
                throw new IOException("Training configuration/payload mismatch.");
            requireSameModel(network, trainer.snapshot());
            return new Loaded(new Checkpoint(manifest, network), trainer);
        } catch (IllegalArgumentException invalid) { throw new IOException("Invalid checkpoint model.", invalid); }
    }

    private static void requireSameModel(NetworkModel a, NetworkModel b) throws IOException {
        if (a.architecture() != b.architecture()) throw mismatch(a.architecture(), b.architecture());
        if (a instanceof NetworkModel.Nnue n) { requireSameNetwork(n.network(), b.nnue()); return; }
        if (a instanceof NetworkModel.Brn1 left) {
            var right = ((NetworkModel.Brn1) b).model();
            for (int i = 0; i < com.ohinteractive.seedv6.core.brn1.Brn1Model.PARAMETER_COUNT; i++) {
                if (Double.doubleToRawLongBits(left.model().weight(i)) != Double.doubleToRawLongBits(right.weight(i)))
                    throw new IOException("BRN-1 network/model parameter mismatch.");
            }
            return;
        }
        if (a instanceof NetworkModel.Brn2 left) {
            var right = ((NetworkModel.Brn2) b).model();
            for (int i = 0; i < com.ohinteractive.seedv6.core.brn2.Brn2Model.PARAMETER_COUNT; i++) {
                if (Double.doubleToRawLongBits(left.model().weight(i)) != Double.doubleToRawLongBits(right.weight(i)))
                    throw new IOException("BRN-2 network/model parameter mismatch.");
            }
            return;
        }
        var left = ((NetworkModel.Brn) a).model(); var right = ((NetworkModel.Brn) b).model();
        for (int i = 0; i < BrnFeatureSchema.PARAMETER_COUNT; i++) {
            if (Double.doubleToRawLongBits(left.weight(i)) != Double.doubleToRawLongBits(right.weight(i)))
                throw new IOException("BRN network/model parameter mismatch.");
        }
    }

    /** Bit-for-bit comparison, including signed zero, over every parameter after independent codec validation. */
    private static void requireSameNetwork(NnueNetwork a, NnueNetwork b) throws IOException {
        if (a.schemaVersion() != b.schemaVersion()) throw new IOException("Network/model schema mismatch.");
        for (int i = 0; i < NnueNetwork.ACCUMULATOR_SIZE; i++) same(a.featureBias(i), b.featureBias(i));
        for (int row = 0; row < NnueFeatureSchema.FEATURE_COUNT; row++) {
            for (int i = 0; i < NnueNetwork.ACCUMULATOR_SIZE; i++) same(a.featureWeight(row, i), b.featureWeight(row, i));
        }
        for (int unit = 0; unit < NnueNetwork.HIDDEN_SIZE; unit++) {
            same(a.hiddenBias(unit), b.hiddenBias(unit));
            for (int i = 0; i < NnueNetwork.CONCATENATED_SIZE; i++) same(a.hiddenWeight(unit, i), b.hiddenWeight(unit, i));
        }
        same(a.outputBias(), b.outputBias());
        for (int i = 0; i < NnueNetwork.HIDDEN_SIZE; i++) same(a.outputWeight(i), b.outputWeight(i));
    }
    private static void same(float a, float b) throws IOException {
        if (Float.floatToRawIntBits(a) != Float.floatToRawIntBits(b)) throw new IOException("Network/model parameter mismatch.");
    }

    void publishRecord(String category, String id, byte[] bytes) throws IOException {
        Path target = root.resolve(category).resolve(id);
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            if (!Arrays.equals(Files.readAllBytes(target), bytes)) throw new IOException("Conflicting immutable record.");
            return;
        }
        Path temporary = root.resolve("staging").resolve("record-" + UUID.randomUUID());
        writeBytes(temporary, bytes);
        mover.move(temporary, target, false);
        forceDirectory(root.resolve(category));
        forceDirectory(root.resolve("staging"));
    }

    private Reference readReference(String name) throws IOException {
        return readReference(root, name);
    }

    private static Reference readReference(Path root, String name) throws IOException {
        return SmallRecord.read(root.resolve("refs").resolve(name), "reference-" + name, in -> {
            String checkpoint = in.readUTF(), evidence = in.readUTF();
            CheckpointManifest.requireId(checkpoint);
            if (!evidence.isEmpty()) PromotionRecord.requireId(evidence);
            return new Reference(checkpoint, evidence);
        });
    }
    private void writeReference(String name, Reference ref) throws IOException {
        byte[] bytes = SmallRecord.encode("reference-" + name, out -> {
            out.writeUTF(ref.checkpointId()); out.writeUTF(ref.evidenceId());
        });
        Path temporary = root.resolve("refs").resolve("." + name + "-" + UUID.randomUUID() + ".tmp");
        writeBytes(temporary, bytes);
        mover.move(temporary, root.resolve("refs").resolve(name), true);
        forceDirectory(root.resolve("refs"));
    }

    static void atomicMove(Path source, Path target, boolean replace) throws IOException {
        try {
            if (replace) Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            else Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException unsupported) {
            throw new IOException("Atomic checkpoint publication is unavailable; no non-atomic fallback.", unsupported);
        }
    }

    private void writeBytes(Path path, byte[] bytes) throws IOException { writeArtifact(path, out -> out.write(bytes)); }
    private void writeArtifact(Path path, ArtifactWriter writer) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            var output = new BufferedOutputStream(Channels.newOutputStream(channel), 65536);
            writer.write(output);
            output.flush();
            channel.force(true);
        }
    }
    private void forceDirectory(Path path) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) { channel.force(true); }
        catch (AccessDeniedException | UnsupportedOperationException unsupported) { directoryForceSupported = false; }
    }
    private DirectoryStream<Path> entries(String name) throws IOException { return Files.newDirectoryStream(root.resolve(name)); }
    private static void diagnostic(List<String> diagnostics, String message) {
        if (diagnostics.size() < 32) diagnostics.add(message);
        else if (diagnostics.size() == 32) diagnostics.add("Further corrupt-artifact diagnostics omitted.");
    }
    private static int comparePromotion(PromotionRecord a, PromotionRecord b) {
        int comparison = Long.compare(a.sequence(), b.sequence());
        return comparison == 0 ? a.id().compareTo(b.id()) : comparison;
    }
    private static void requireCheckpointId(String id) throws IOException {
        try { CheckpointManifest.requireId(id); } catch (IllegalArgumentException invalid) { throw new IOException(invalid); }
    }
    private void requireOpen() throws IOException { if (closed) throw new IOException("Checkpoint store is closed."); }
    @Override public void close() throws IOException {
        if (!closed) {
            closed = true;
            try { lock.release(); } finally { lockChannel.close(); }
        }
    }
}
