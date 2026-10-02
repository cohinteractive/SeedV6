package com.ohinteractive.seedv6.training.service;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.*;
import java.sql.SQLException;
import java.util.*;
import java.util.function.ToDoubleFunction;
import com.google.gson.Gson;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn2.Brn2MaterialPrior;
import com.ohinteractive.seedv6.corpus.*;
import com.ohinteractive.seedv6.training.selfplay.TrajectorySampler.Sample;

/** Source adapter only. Features, forward/backprop, Adam and half-squared loss stay in the existing trainer. */
public final class BrnCorpusTraining implements AutoCloseable {
    public static final String POLICY = "basic-v1-cp/32511-stm-v1;skip-mate,raw-perspective,out-of-range;unknown-clock=0;feistel6-v1;heldout=max(2,ceil(n/5))";
    private static final Gson JSON = new Gson();
    private static final long SPLIT = 0xa54ff53a5f1d36f1L, VALIDATION = 0x3c6ef372fe94f82bL;
    public record Pin(String root, long seed, int positions, String identity, String policy, String checksum) {
        public Pin {
            if (positions < 2 || !hash(identity) || !POLICY.equals(policy)
                    || !pinHash(root, seed, positions, identity, policy).equals(checksum))
                throw new IllegalArgumentException("Invalid corpus campaign binding/checksum");
        }
        public Pin(String root, long seed, int positions, String identity, String policy) {
            this(root, seed, positions, identity, policy, pinHash(root, seed, positions, identity, policy));
        }
    }
    public record Evidence(String root, String viewIdentity, long seed, long generation, int requested,
            long recordsExamined, int usable, int heldOut, String trainingHash, String heldOutHash,
            long viewExamined, Map<String, Long> viewSkipped) {
        public Evidence {
            if (root == null || !hash(viewIdentity) || generation < 1 || requested < 2 || usable != requested
                    || recordsExamined != (long) usable + heldOut || heldOut < 2 || !hash(trainingHash) || !hash(heldOutHash)
                    || viewExamined < 4 || viewSkipped == null) throw new IllegalArgumentException("Invalid corpus evidence");
            if (viewSkipped.values().stream().anyMatch(n -> n == null || n < 0)) throw new IllegalArgumentException("Invalid corpus exclusions");
            viewSkipped = Collections.unmodifiableMap(new TreeMap<>(viewSkipped));
        }
        public String json() { return JSON.toJson(this); }
        public static Evidence read(String json) { return JSON.fromJson(json, Evidence.class); }
    }
    /** Samples retain their original exact-WDL invariant. The existing explicit bounded-target
     * selector supplies the CP label, just as it already supplies NNUE blended supervision.
     */
    public static final class Examples {
        private final List<Sample> samples = new ArrayList<>();
        private final IdentityHashMap<Sample, Double> targets = new IdentityHashMap<>();
        private void add(CorpusRecord record) {
            if (rejection(record) != null) throw new IllegalArgumentException("Pinned record is no longer eligible");
            Sample sample = new Sample(record.position().toBoard(0), 0);
            samples.add(sample); targets.put(sample, target(record));
        }
        public List<Sample> samples() { return List.copyOf(samples); }
        public ToDoubleFunction<Sample> targets() { return sample -> Objects.requireNonNull(targets.get(sample), "Unknown corpus example"); }
    }
    public record Batch(Examples training, Examples validation, Evidence evidence) {}
    private final Path directory;
    private final CorpusView view;
    private final Pin pin;

    public static String rejection(CorpusRecord record) {
        if (record.targetKind() == CorpusRecord.MATE) return "mate";
        if (record.targetKind() != CorpusRecord.CP) return "unsupported-target";
        if (record.perspective() != CorpusRecord.WHITE && record.perspective() != CorpusRecord.SIDE_TO_MOVE) return "unsupported-perspective";
        if (Math.abs((long) record.target()) > Brn2MaterialPrior.SCORE_SCALE) return "cp-out-of-range";
        return null;
    }
    public static double target(CorpusRecord record) {
        if (rejection(record) != null) throw new IllegalArgumentException("Unsupported corpus target");
        // Canonical feature orientation already predicts STM: sign-transform the label ONCE.
        long cp = record.target();
        if (record.perspective() == CorpusRecord.WHITE && Board.player(record.position().rules()) == 1) cp = -cp;
        return cp / (double) Brn2MaterialPrior.SCORE_SCALE;
    }
    public BrnCorpusTraining(TrainerConfig config, TrainingSource source, boolean mayCreate) throws IOException {
        directory = config.checkpointRoot().resolve("corpus-training");
        String root = source.corpusRoot().toAbsolutePath().normalize().toString();
        var current = readPin(config.checkpointRoot());
        int count = config.corpusTraining() != null ? config.corpusTraining().positionsPerGeneration()
                : current.filter(p -> p.root().equals(root) && p.seed() == config.masterSeed())
                        .orElseThrow(() -> new IOException("Missing pinned corpus campaign/count; no fallback is permitted")).positions();
        Path binding = bindingDirectory(directory, root, config.masterSeed(), count);
        Path metadata = binding.resolve("campaign.json");
        try {
            if (!Files.exists(metadata)) {
                if (!mayCreate || config.corpusTraining() == null) throw new IOException("Missing pinned corpus campaign/count; no fallback is permitted");
                try (CorpusReader reader = new CorpusReader(source.corpusRoot())) {
                    if (!Files.exists(binding.resolve("view.json"))) CorpusView.create(reader, binding, POLICY, BrnCorpusTraining::rejection);
                }
            }
            view = new CorpusView(source.corpusRoot(), binding, POLICY);
            try {
                if (view.size() < 4) throw new IOException("Corpus sampling needs at least four eligible CP identities (two training and two reserved held out)");
                Pin requested = new Pin(root, config.masterSeed(), count, view.identity(), POLICY);
                if (Files.exists(metadata)) {
                    pin = JSON.fromJson(Files.readString(metadata), Pin.class);
                    if (pin == null || !pin.root().equals(requested.root()) || pin.seed() != requested.seed()
                            || pin.positions() != requested.positions()
                            || !pin.identity().equals(requested.identity()) || !pin.policy().equals(POLICY))
                        throw new IOException("Corpus configuration differs from its recorded binding");
                } else { pin = requested; writeNew(metadata, JSON.toJson(pin)); }
                if (config.corpusTraining() != null && !config.corpusTraining().viewIdentity().isEmpty()
                        && !config.corpusTraining().viewIdentity().equals(pin.identity())) throw new IOException("Configured corpus identity differs from campaign pin");
                // Only this small current-selection record changes. All prior bindings/views and
                // settled generation receipts remain intact; attempts/history record their identity.
                if (current.isEmpty() || !current.get().equals(pin)) writeCurrent(directory.resolve("current.json"), JSON.toJson(pin));
            } catch (Throwable failure) { view.close(); throw failure; }
        } catch (SQLException | RuntimeException invalid) { throw new IOException("Cannot open pinned BRN corpus: " + source.corpusRoot(), invalid); }
    }
    public CorpusTrainingConfig config() { return new CorpusTrainingConfig(pin.positions(), pin.identity()); }
    public Pin pin() { return pin; }

    /** Lightweight campaign configuration read; view/shard integrity is checked by normal startup. */
    public static Optional<Pin> readPin(Path checkpointRoot) throws IOException {
        Path directory = checkpointRoot.resolve("corpus-training");
        Path current = directory.resolve("current.json");
        return readBinding(Files.exists(current) ? current : directory.resolve("campaign.json"));
    }

    /** Resolve only the requested configuration; an unrelated prior pin is never a default identity. */
    public static Optional<Pin> readPin(Path checkpointRoot, TrainingSource source, CorpusTrainingConfig config, long seed) throws IOException {
        if (!source.corpus()) return Optional.empty();
        String root = source.corpusRoot().toAbsolutePath().normalize().toString();
        if (config == null) return readPin(checkpointRoot).filter(p -> p.root().equals(root) && p.seed() == seed);
        Path binding = bindingDirectory(checkpointRoot.resolve("corpus-training"), root, seed, config.positionsPerGeneration());
        return readBinding(binding.resolve("campaign.json"));
    }

    private static Path bindingDirectory(Path directory, String root, long seed, int count) throws IOException {
        var legacy = readBinding(directory.resolve("campaign.json"));
        if (legacy.isEmpty() && Files.exists(directory.resolve("current.json"))) throw new IOException("Missing original corpus binding; existing evidence was preserved");
        if (legacy.isEmpty() || legacy.get().root().equals(root) && legacy.get().seed() == seed && legacy.get().positions() == count)
            return directory;
        // Keep the original layout readable and immutable. Changed selection gets its own binding
        // and the same CorpusView implementation, without modifying corpus schema or sampling.
        return directory.resolve("configurations").resolve(pinHash(root, seed, count, "", POLICY));
    }

    private static Optional<Pin> readBinding(Path metadata) throws IOException {
        if (!Files.exists(metadata)) return Optional.empty();
        try {
            Pin pin = JSON.fromJson(Files.readString(metadata), Pin.class);
            if (pin == null) throw new IllegalArgumentException("Empty corpus campaign binding");
            return Optional.of(pin);
        } catch (RuntimeException invalid) { throw new IOException("Invalid pinned corpus campaign: " + metadata, invalid); }
    }

    public Batch batch(long generation) throws IOException {
        if (generation < 1) throw new IllegalArgumentException("Invalid corpus generation");
        int count = pin.positions(), held = (int) Math.max(2, (count + 3L) / 4);
        long reserved = Math.max(2, (view.size() + 4) / 5), trainingSize = view.size() - reserved;
        var training = new Examples(); var validation = new Examples();
        MessageDigest trainHash = digest(), heldHash = digest();
        long start = Math.multiplyExact(generation - 1, (long) count), validationStart = Math.multiplyExact(generation - 1, (long) held);
        // A fixed seed-dependent global partition prevents validation leakage even across epochs.
        // Each stratum then advances through its own deterministic epoch permutation.
        for (int i = 0; i < count; i++) {
            long rank = reserved + CorpusPermutation.at(trainingSize, pin.seed(), Math.addExact(start, i));
            add(training, trainHash, CorpusPermutation.index(view.size(), pin.seed() ^ SPLIT, 0, rank));
        }
        for (int i = 0; i < held; i++) {
            long rank = CorpusPermutation.at(reserved, pin.seed() ^ VALIDATION, Math.addExact(validationStart, i));
            add(validation, heldHash, CorpusPermutation.index(view.size(), pin.seed() ^ SPLIT, 0, rank));
        }
        var evidence = new Evidence(pin.root(), pin.identity(), pin.seed(), generation, count, (long) count + held, count, held,
                HexFormat.of().formatHex(trainHash.digest()), HexFormat.of().formatHex(heldHash.digest()),
                view.descriptor().manifest().positions(), view.descriptor().excluded());
        Path receipt = directory.resolve("generation-" + generation + ".json");
        if (Files.exists(receipt)) {
            if (!Evidence.read(Files.readString(receipt)).equals(evidence)) throw new IOException("Deterministic corpus replay differs from generation receipt");
        } else writeNew(receipt, evidence.json());
        return new Batch(training, validation, evidence);
    }
    /** Candidate recovery needs only the held-out stream, never a second training buffer. */
    public Examples validation(long generation) throws IOException {
        var receipt = evidence(directory.getParent(), generation);
        int held = (int) Math.max(2, (pin.positions() + 3L) / 4);
        if (!receipt.root().equals(pin.root()) || !receipt.viewIdentity().equals(pin.identity()) || receipt.seed() != pin.seed()
                || receipt.requested() != pin.positions() || receipt.generation() != generation || receipt.heldOut() != held)
            throw new IOException("Corpus holdout receipt differs from campaign pin");
        var result = new Examples(); var hash = digest();
        long reserved = Math.max(2, (view.size() + 4) / 5), start = Math.multiplyExact(generation - 1, (long) held);
        for (int i = 0; i < held; i++) {
            long rank = CorpusPermutation.at(reserved, pin.seed() ^ VALIDATION, Math.addExact(start, i));
            add(result, hash, CorpusPermutation.index(view.size(), pin.seed() ^ SPLIT, 0, rank));
        }
        if (!HexFormat.of().formatHex(hash.digest()).equals(receipt.heldOutHash())) throw new IOException("Pinned corpus holdout changed");
        return result;
    }
    private void add(Examples examples, MessageDigest hash, long index) throws IOException {
        CorpusRecord record = view.record(index);
        hash.update(ByteBuffer.allocate(8).putLong(index).array()); hash.update(record.encode());
        examples.add(record);
    }
    public static Evidence evidence(Path root, long generation) throws IOException {
        try { return Evidence.read(Files.readString(root.resolve("corpus-training").resolve("generation-" + generation + ".json"))); }
        catch (RuntimeException invalid) { throw new IOException("Invalid corpus generation receipt", invalid); }
    }
    private static boolean hash(String value) { return value != null && value.matches("[0-9a-f]{64}"); }
    private static String pinHash(String root, long seed, int positions, String identity, String policy) {
        return HexFormat.of().formatHex(digest().digest(JSON.toJson(List.of(root, seed, positions, identity, policy))
                .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private static void writeNew(Path target, String content) throws IOException {
        Path pending = target.resolveSibling(target.getFileName() + ".pending");
        Files.writeString(pending, content, StandardOpenOption.CREATE_NEW);
        try (FileChannel f = FileChannel.open(pending, StandardOpenOption.WRITE)) { f.force(true); }
        Files.move(pending, target, StandardCopyOption.ATOMIC_MOVE);
    }
    private static void writeCurrent(Path target, String content) throws IOException {
        Path pending = Files.createTempFile(target.getParent(), "current-", ".pending");
        try {
            Files.writeString(pending, content);
            try (FileChannel f = FileChannel.open(pending, StandardOpenOption.WRITE)) { f.force(true); }
            Files.move(pending, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(pending); }
    }
    @Override public void close() throws IOException { view.close(); }
}
