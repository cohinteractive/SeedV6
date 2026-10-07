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
import com.ohinteractive.seedv6.training.data.TrainingPosition;
import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.model.TrainingArchitecture;

/** Source adapter only. Features, forward/backprop, optimizer and objective stay in the selected trainer. */
public class CorpusTraining implements AutoCloseable {
    public static final String POLICY = "basic-v1-cp/32511-stm-v1;skip-mate,raw-perspective,out-of-range;unknown-clock=0;feistel6-v1;heldout=max(2,ceil(n/5))";
    public static final String NNUE_POLICY = "STOCKFISH_WDL_V1;sf17.1=03e27488f3d21d8ff4dbf3065603afa21dbd0ef3;cp-bin-centre;material17..78/58;permille;mate-sign;unknown-clock=0;feistel6-v1;heldout=max(2,ceil(n/5))";
    public static final String BRN3_POLICY = "BRN3_CP_WDL_V1;STOCKFISH_WDL_V1;skip-mate,raw-perspective,out-of-range;unknown-clock=0;feistel6-v1;heldout=max(2,ceil(n/5))";
    // Historical BRN-3 recipe excludes CP mate labels; retain its durable identity.
    public static final String SOURCE_OUTCOME = "BRN3_SOURCE_OUTCOME_V1";
    public static final String SOURCE_OUTCOME_V1 = "SOURCE_OUTCOME_V1";
    public enum TargetPolicy {
        BASIC_V1(POLICY, "BASIC_V1_CP_STM"), STOCKFISH_WDL_V1(NNUE_POLICY, com.ohinteractive.seedv6.training.nnue.NnueCorpusTargets.ID, SOURCE_OUTCOME_V1),
        BRN3_CP_WDL_V1(BRN3_POLICY,"BRN3_CP_WDL_V1", SOURCE_OUTCOME),
        BT4_Q_V1("BT4_Q_V1;inverse-660.6-q/(1-.9751875*q^10);skip-32002;stm", "BT4_Q_V1");
        final String policy; public final String identity;
        // Non-null only when this corpus consumer accepts side-to-move outcome targets.
        final String sourceOutcomeAdapter;
        TargetPolicy(String policy, String identity) { this(policy, identity, null); }
        TargetPolicy(String policy, String identity, String sourceOutcomeAdapter) {
            this.policy = policy; this.identity = identity; this.sourceOutcomeAdapter = sourceOutcomeAdapter;
        }
        String rejection(TrainingPosition record) {
            if (this == BT4_Q_V1) return record.targetKind() != BinpackDecoder.ENCODED_SCORE || record.perspective() != CorpusRecord.SIDE_TO_MOVE
                    ? "unsupported-BT4-label" : record.target() == Bt4Targets.SKIP ? "BT4-skip-sentinel"
                    : Math.abs((long) record.target()) > 32000 ? "BT4-score-out-of-range" : null;
            return this != STOCKFISH_WDL_V1 ? CorpusTraining.rejection(record)
                : com.ohinteractive.seedv6.training.nnue.NnueCorpusTargets.rejection(record); }
        double target(TrainingPosition record, long[] board) { return this == BT4_Q_V1 ? Bt4Targets.q(record.target()) : this == BASIC_V1 ? CorpusTraining.target(record)
                : com.ohinteractive.seedv6.training.nnue.NnueCorpusTargets.target(record, board); }
    }
    public static TargetPolicy targetPolicy(TrainingArchitecture architecture, LabelProfile profile) {
        if (profile == LabelProfile.BT4_Q_V1) {
            sourceOutcomeAdapter(architecture);
            return TargetPolicy.BT4_Q_V1;
        }
        return targetPolicy(architecture);
    }
    /** Capability and receipt recipe belong to the target consumer, never the source picker. */
    public static String sourceOutcomeAdapter(TrainingArchitecture architecture) {
        String adapter = targetPolicy(architecture).sourceOutcomeAdapter;
        if (adapter == null) throw new IllegalArgumentException("BT4_Q_V1 supplies side-to-move outcome Q; "
                + architecture + " corpus training requires scaled centipawn targets. No Q-to-centipawn conversion is defined.");
        return adapter;
    }
    static boolean sourceOutcome(String adapter) {
        return SOURCE_OUTCOME.equals(adapter) || SOURCE_OUTCOME_V1.equals(adapter);
    }
    public static TargetPolicy targetPolicy(com.ohinteractive.seedv6.training.model.TrainingArchitecture architecture) {
        return switch (architecture) {
            case BRN2 -> TargetPolicy.BASIC_V1;
            case NNUE, NNUE_MATERIAL -> TargetPolicy.STOCKFISH_WDL_V1;
            case BRN3, BRN_PAIR2 -> TargetPolicy.BRN3_CP_WDL_V1;
            default -> throw new IllegalArgumentException("Corpus training supports NNUE, BRN-2 and BRN-3");
        };
    }
    private static boolean supportedPolicy(String policy) { return POLICY.equals(policy) || NNUE_POLICY.equals(policy) || BRN3_POLICY.equals(policy); }
    private static final Gson JSON = new Gson();
    private static final long SPLIT = 0xa54ff53a5f1d36f1L, VALIDATION = 0x3c6ef372fe94f82bL;
    public record Pin(String root, long seed, int positions, String identity, String policy, String checksum) {
        public Pin {
            if (positions < 2 || !hash(identity) || !supportedPolicy(policy)
                    || !pinHash(root, seed, positions, identity, policy).equals(checksum))
                throw new IllegalArgumentException("Invalid corpus campaign binding/checksum");
        }
        public Pin(String root, long seed, int positions, String identity, String policy) {
            this(root, seed, positions, identity, policy, pinHash(root, seed, positions, identity, policy));
        }
    }
    public record Evidence(String root, String viewIdentity, long seed, long generation, int requested,
            long recordsExamined, int usable, int heldOut, String trainingHash, String heldOutHash,
            long viewExamined, Map<String, Long> viewSkipped, String targetAdapter, int mateExamples, int heldOutMateExamples,
            Map<String, String> sourceLabels) {
        public Evidence {
            if (root == null || !hash(viewIdentity) || generation < 1 || requested < 2 || usable != requested
                    || recordsExamined != (long) usable + heldOut || heldOut < 0 || heldOut == 1 || !hash(trainingHash) || !hash(heldOutHash)
                    || viewExamined < 0 || viewSkipped == null) throw new IllegalArgumentException("Invalid Training Data evidence");
            if (viewSkipped.values().stream().anyMatch(n -> n == null || n < 0)) throw new IllegalArgumentException("Invalid corpus exclusions");
            if (targetAdapter != null && !targetAdapter.equals(TargetPolicy.STOCKFISH_WDL_V1.identity) && !targetAdapter.equals(TargetPolicy.BRN3_CP_WDL_V1.identity) && !sourceOutcome(targetAdapter)
                    || mateExamples < 0 || mateExamples > usable || heldOutMateExamples < 0 || heldOutMateExamples > heldOut
                    || (!TargetPolicy.STOCKFISH_WDL_V1.identity.equals(targetAdapter) && !SOURCE_OUTCOME_V1.equals(targetAdapter)) && (mateExamples != 0 || heldOutMateExamples != 0))
                throw new IllegalArgumentException("Invalid corpus target evidence");
            viewSkipped = Collections.unmodifiableMap(new TreeMap<>(viewSkipped));
            // Null preserves historical receipt JSON and checkpoint evidence bytes exactly.
            if (sourceLabels != null) sourceLabels = Collections.unmodifiableMap(new TreeMap<>(sourceLabels));
            if (sourceLabels != null && !sourceOutcome(targetAdapter)) throw new IllegalArgumentException("Unexpected source-specific target evidence");
            if (sourceOutcome(targetAdapter) && (sourceLabels == null || sourceLabels.isEmpty()
                    || sourceLabels.entrySet().stream().anyMatch(e -> !hash(e.getKey()) || e.getValue() == null
                    || !e.getValue().matches("STOCKFISH_CP_MATE_V1|BT4_Q_V1;prepared=[0-9a-f]{64}"))))
                throw new IllegalArgumentException("Missing/invalid source label evidence");
        }
        public Evidence(String root, String viewIdentity, long seed, long generation, int requested,
                long recordsExamined, int usable, int heldOut, String trainingHash, String heldOutHash,
                long viewExamined, Map<String, Long> viewSkipped, String targetAdapter, int mateExamples, int heldOutMateExamples) {
            this(root, viewIdentity, seed, generation, requested, recordsExamined, usable, heldOut, trainingHash, heldOutHash,
                    viewExamined, viewSkipped, targetAdapter, mateExamples, heldOutMateExamples, null);
        }
        public boolean supports(TrainingArchitecture architecture) {
            var consumer = targetPolicy(architecture);
            return adapterIdentity().equals(consumer.identity) || adapterIdentity().equals(consumer.sourceOutcomeAdapter);
        }
        public void verifySourceLabels(Path lineage) throws IOException {
            if (!sourceOutcome(targetAdapter)) return;
            DataSources selected = DataFiles.read(DataSources.directory(lineage).resolve("selections").resolve(viewIdentity + ".json"), DataSources.class);
            if (!selected.identity().equals(viewIdentity) || sourceLabels.size() != selected.sources().size()) throw new IOException("Receipt source selection mismatch");
            for (DataSource source : selected.sources()) {
                String value = sourceLabels.get(source.identity());
                String expected = source.labelProfile().name();
                if (value == null || (source.format() == DataSource.Format.STOCKFISH_BINPACK_ZSTD
                        ? !value.startsWith(expected + ";prepared=") : !value.equals(expected))) throw new IOException("Receipt source label profile mismatch");
            }
        }
        /** Legacy BRN receipts omit targetAdapter and retain their exact interpretation. */
        public Evidence(String root, String view, long seed, long generation, int requested, long examined,
                int usable, int heldOut, String trainingHash, String heldOutHash, long viewExamined, Map<String, Long> skipped) {
            this(root, view, seed, generation, requested, examined, usable, heldOut, trainingHash, heldOutHash, viewExamined, skipped, null, 0, 0);
        }
        public String adapterIdentity() { return targetAdapter == null ? TargetPolicy.BASIC_V1.identity : targetAdapter; }
        public String json() { return JSON.toJson(this); }
        public static Evidence read(String json) { return JSON.fromJson(json, Evidence.class); }
    }
    /** Draw placeholders retain the generated sample invariant; only the explicit target selector supplies corpus labels. */
    public static final class Examples {
        private final List<Sample> samples = new ArrayList<>();
        private final IdentityHashMap<Sample, Double> targets = new IdentityHashMap<>();
        private final TargetPolicy adapter;
        private int mates;
        Examples(TargetPolicy adapter) { this.adapter = adapter; }
        void add(TrainingPosition record) {
            add(record, adapter);
        }
        void add(TrainingPosition record, TargetPolicy sourceAdapter) {
            if (sourceAdapter.rejection(record) != null) throw new IllegalArgumentException("Pinned record is no longer eligible");
            long[] board = record.position().toBoard(0);
            Sample sample = new Sample(board, 0);
            samples.add(sample); targets.put(sample, sourceAdapter.target(record, board));
            if (record.targetKind() == CorpusRecord.MATE) mates++;
        }
        public List<Sample> samples() { return List.copyOf(samples); }
        int mates() { return mates; }
        public ToDoubleFunction<Sample> targets() { return sample -> Objects.requireNonNull(targets.get(sample), "Unknown corpus example"); }
    }
    public record Batch(Examples training, Examples validation, Evidence evidence) {}
    private final Path directory, checkpointRoot;
    private final TargetPolicy adapter;
    private final CorpusView view;
    private final Pin pin;
    private boolean legacyResume;
    public boolean legacyResume() { return legacyResume; }

    public static String rejection(TrainingPosition record) {
        if (record.targetKind() == CorpusRecord.MATE) return "mate";
        if (record.targetKind() != CorpusRecord.CP) return "unsupported-target";
        if (record.perspective() != CorpusRecord.WHITE && record.perspective() != CorpusRecord.SIDE_TO_MOVE) return "unsupported-perspective";
        if (Math.abs((long) record.target()) > Brn2MaterialPrior.SCORE_SCALE) return "cp-out-of-range";
        return null;
    }
    public static double target(TrainingPosition record) {
        if (rejection(record) != null) throw new IllegalArgumentException("Unsupported corpus target");
        // Canonical feature orientation already predicts STM: sign-transform the label ONCE.
        long cp = record.target();
        if (record.perspective() == CorpusRecord.WHITE && Board.player(record.position().rules()) == 1) cp = -cp;
        return cp / (double) Brn2MaterialPrior.SCORE_SCALE;
    }
    protected CorpusTraining() { directory = checkpointRoot = null; adapter = null; view = null; pin = null; }
    public void complete(long generation) throws IOException { }
    public CorpusTraining(TrainerConfig config, TrainingSource source, boolean mayCreate) throws IOException {
        this(config, source, mayCreate, CorpusPreparation.NONE);
    }
    public CorpusTraining(TrainerConfig config, TrainingSource source, boolean mayCreate, CorpusPreparation preparation) throws IOException {
        this(config, source, mayCreate, preparation, false);
    }
    CorpusTraining(TrainerConfig config, TrainingSource source, boolean mayCreate, CorpusPreparation preparation, boolean legacyResume) throws IOException {
        this.legacyResume = legacyResume;
        preparation.checkCancelled();
        checkpointRoot = config.checkpointRoot();
        directory = checkpointRoot.resolve("corpus-training");
        adapter = targetPolicy(config.architecture());
        String root = source.corpusRoot().toAbsolutePath().normalize().toString();
        var current = readPin(config.checkpointRoot());
        int count = config.corpusTraining() != null ? config.corpusTraining().positionsPerGeneration()
                : current.filter(p -> p.root().equals(root) && p.seed() == config.masterSeed() && p.policy().equals(adapter.policy))
                        .orElseThrow(() -> new IOException("Missing pinned corpus campaign/count; no fallback is permitted")).positions();
        Path binding = bindingDirectory(directory, root, config.masterSeed(), count, adapter.policy);
        Path metadata = binding.resolve("campaign.json");
        try {
            if (!Files.exists(metadata)) {
                if (!mayCreate || config.corpusTraining() == null) throw new IOException("Missing pinned corpus campaign/count; no fallback is permitted");
                try (CorpusReader reader = new CorpusReader(source.corpusRoot())) {
                    if (!Files.exists(binding.resolve("view.json"))) CorpusView.create(reader, binding, adapter.policy,
                            adapter::rejection, Long.MAX_VALUE, preparation);
                }
            }
            view = new CorpusView(source.corpusRoot(), binding, adapter.policy, preparation, legacyResume);
            try {
                preparation.checkCancelled();
                if (view.size() < 4) throw new IOException("Corpus sampling needs at least four eligible " + (adapter == TargetPolicy.BASIC_V1 ? "CP" : "outcome") + " identities (two training and two reserved held out)");
                Pin requested = new Pin(root, config.masterSeed(), count, view.identity(), adapter.policy);
                if (Files.exists(metadata)) {
                    pin = JSON.fromJson(Files.readString(metadata), Pin.class);
                    if (pin == null || !pin.root().equals(requested.root()) || pin.seed() != requested.seed()
                            || pin.positions() != requested.positions()
                            || !pin.identity().equals(requested.identity()) || !pin.policy().equals(adapter.policy))
                        throw new IOException("Corpus configuration differs from its recorded binding");
                } else { pin = requested; writeNew(metadata, JSON.toJson(pin)); }
                if (config.corpusTraining() != null && !config.corpusTraining().viewIdentity().isEmpty()
                        && !config.corpusTraining().viewIdentity().equals(pin.identity())) throw new IOException("Configured corpus identity differs from campaign pin");
                // Only this small current-selection record changes. All prior bindings/views and
                // settled generation receipts remain intact; attempts/history record their identity.
                if (current.isEmpty() || !current.get().equals(pin)) writeCurrent(directory.resolve("current.json"), JSON.toJson(pin));
                if (legacyResume && !com.ohinteractive.seedv6.training.checkpoint.GenerationAttempt.inspect(checkpointRoot).orElseThrow()
                        .matches(config.withCorpusTraining(config()), source)) throw new IOException("Legacy partial-generation settings changed. Select Training Data sources to explicitly restart with sequential cursors.");
            } catch (Throwable failure) { view.close(); throw failure; }
        } catch (SQLException | RuntimeException invalid) { throw new IOException("Cannot open pinned Seed corpus: " + source.corpusRoot(), invalid); }
    }
    public CorpusTrainingConfig config() { return new CorpusTrainingConfig(pin.positions(), pin.identity()).forArchitecture(adapter == TargetPolicy.BASIC_V1
            ? com.ohinteractive.seedv6.training.model.TrainingArchitecture.BRN2 : adapter==TargetPolicy.BRN3_CP_WDL_V1
            ? com.ohinteractive.seedv6.training.model.TrainingArchitecture.BRN3 : com.ohinteractive.seedv6.training.model.TrainingArchitecture.NNUE); }
    public Pin pin() { return pin; }

    /** Lightweight campaign configuration read; view/shard integrity is checked by normal startup. */
    public static Optional<Pin> readPin(Path checkpointRoot) throws IOException {
        Path directory = checkpointRoot.resolve("corpus-training");
        Path current = directory.resolve("current.json");
        return readBinding(Files.exists(current) ? current : directory.resolve("campaign.json"));
    }

    /** Resolve only the requested configuration; an unrelated prior pin is never a default identity. */
    public static Optional<Pin> readPin(Path checkpointRoot, TrainingSource source, CorpusTrainingConfig config, long seed) throws IOException {
        return readPin(checkpointRoot, source, config, seed, com.ohinteractive.seedv6.training.model.TrainingArchitecture.BRN2);
    }
    public static Optional<Pin> readPin(Path checkpointRoot, TrainingSource source, CorpusTrainingConfig config, long seed,
            com.ohinteractive.seedv6.training.model.TrainingArchitecture architecture) throws IOException {
        if (!source.corpus() || source.dataSources()) return Optional.empty();
        String policy = targetPolicy(architecture).policy;
        String root = source.corpusRoot().toAbsolutePath().normalize().toString();
        if (config == null) return readPin(checkpointRoot).filter(p -> p.root().equals(root) && p.seed() == seed && p.policy().equals(policy));
        Path binding = bindingDirectory(checkpointRoot.resolve("corpus-training"), root, seed, config.positionsPerGeneration(), policy);
        return readBinding(binding.resolve("campaign.json"));
    }

    private static Path bindingDirectory(Path directory, String root, long seed, int count, String policy) throws IOException {
        var legacy = readBinding(directory.resolve("campaign.json"));
        if (legacy.isEmpty() && Files.exists(directory.resolve("current.json"))) throw new IOException("Missing original corpus binding; existing evidence was preserved");
        if (legacy.isEmpty() || legacy.get().root().equals(root) && legacy.get().seed() == seed && legacy.get().positions() == count && legacy.get().policy().equals(policy))
            return directory;
        // Keep the original layout readable and immutable. Changed selection gets its own binding
        // and the same CorpusView implementation, without modifying corpus schema or sampling.
        return directory.resolve("configurations").resolve(pinHash(root, seed, count, "", policy));
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
        var training = new Examples(adapter); var validation = new Examples(adapter);
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
                view.descriptor().manifest().positions() - view.descriptor().excluded().getOrDefault("diagnostic-unexamined", 0L), view.descriptor().excluded(),
                adapter == TargetPolicy.BASIC_V1 ? null : adapter.identity, training.mates, validation.mates);
        Path receipt = directory.resolve("generation-" + generation + ".json");
        if (Files.exists(receipt)) {
            if (!Evidence.read(Files.readString(receipt)).equals(evidence)) throw new IOException("Deterministic corpus replay differs from generation receipt");
        } else writeNew(receipt, evidence.json());
        return new Batch(training, validation, evidence);
    }
    /** Candidate recovery needs only the held-out stream, never a second training buffer. */
    public Examples validation(long generation) throws IOException {
        var receipt = evidence(checkpointRoot, generation);
        int held = (int) Math.max(2, (pin.positions() + 3L) / 4);
        if (!receipt.root().equals(pin.root()) || !receipt.viewIdentity().equals(pin.identity()) || receipt.seed() != pin.seed()
                || receipt.requested() != pin.positions() || receipt.generation() != generation || receipt.heldOut() != held || !receipt.adapterIdentity().equals(adapter.identity))
            throw new IOException("Corpus holdout receipt differs from campaign pin");
        var result = new Examples(adapter); var hash = digest();
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
