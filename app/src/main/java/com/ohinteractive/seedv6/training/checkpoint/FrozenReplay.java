package com.ohinteractive.seedv6.training.checkpoint;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import com.ohinteractive.seedv6.core.brn2.*;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.history.HistoryRepository;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.service.*;
import com.ohinteractive.seedv6.training.validation.PromotionPolicy;

/** Immutable read-only corpus pin. No source lock, recovery, writer, teacher or generator is used. */
public record FrozenReplay(String sourceStore, String initialId, String modelHash, String optimizerHash,
        BrnRunSeeds seeds, TrainerConfig.SelfPlay selfPlay, TrainerConfig.Training training,
        String startingFen, double learningRate, List<Entry> entries) {
    public static final String FILE = "frozen-replay.bin";
    public static final String SETTINGS_PREFIX = "|frozen-wdl-v1:";
    private static final String KIND = "brn-frozen-wdl-replay-v1";
    private static final int MAX_GENERATIONS = 128;
    public record Entry(long generation, String parent, String planHash, String dataHash, String contentHash) {
        public Entry {
            if (generation < 1) throw new IllegalArgumentException("Invalid frozen generation.");
            CheckpointManifest.requireId(parent);
            SmallRecord.requireHash(planHash); SmallRecord.requireHash(dataHash); SmallRecord.requireHash(contentHash);
        }
    }
    public FrozenReplay {
        Objects.requireNonNull(sourceStore); Objects.requireNonNull(seeds); Objects.requireNonNull(selfPlay);
        Objects.requireNonNull(training); Objects.requireNonNull(startingFen);
        CheckpointManifest.requireId(initialId); SmallRecord.requireHash(modelHash); SmallRecord.requireHash(optimizerHash);
        entries = List.copyOf(entries);
        if (!Path.of(sourceStore).isAbsolute() || entries.isEmpty() || entries.size() > MAX_GENERATIONS
                || training.epochs() != 1 || training.minibatchSize() != 1)
            throw new IllegalArgumentException("Invalid frozen replay controls.");
        for (int i = 0; i < entries.size(); i++) if (entries.get(i).generation() != i + 1)
            throw new IllegalArgumentException("Frozen generations must be contiguous from g1.");
        if (!entries.getFirst().parent().equals(initialId)) throw new IllegalArgumentException("Frozen g0 mismatch.");
        new com.ohinteractive.seedv6.core.brn.BrnAdamConfig(learningRate);
    }

    /** Capture a completed Handcrafted 75/25 control, using explicit known controls; never infer from labels. */
    public static FrozenReplay capture(Path source, TrainerConfig controls) throws IOException {
        Path actual = source.toRealPath();
        if (Files.exists(actual.resolve(FILE))) throw new IOException("A replay cannot be its own experimental control.");
        if (controls.architecture() != TrainingArchitecture.BRN2 || controls.runSeeds() == null
                || !controls.frozenReplayHash().isEmpty()) throw new IOException("Expected explicit BRN-2 control settings/seeds.");
        if (!CheckpointStore.readTrainingSource(actual).orElseThrow().equals(TrainingSource.HANDCRAFTED)
                || !CheckpointStore.readBrnSupervision(actual).orElseThrow().equals(BrnSupervision.blended(.75))
                || !CheckpointStore.readBrnRunSeeds(actual).orElseThrow().equals(controls.runSeeds()))
            throw new IOException("Frozen control must be Handcrafted 75/25 with the specified durable seeds.");
        var history = new HistoryRepository(actual).refresh();
        if (!history.warnings().isEmpty() || history.records().isEmpty()) throw new IOException("Invalid frozen control history.");
        var plans = new ArrayList<BootstrapPlan>();
        try (var files = Files.newDirectoryStream(actual.resolve("bootstrap"), "*.plan")) {
            for (Path file : files) plans.add(BootstrapPlan.read(file));
        }
        plans.sort(Comparator.comparingLong(BootstrapPlan::generation));
        if (plans.size() != history.records().size() || plans.size() > MAX_GENERATIONS)
            throw new IOException("Frozen control must contain only the settled contiguous corpus (at most 128 generations).");
        var pins = new ArrayList<Entry>();
        String parent = plans.getFirst().parentId();
        for (int i = 0; i < plans.size(); i++) {
            var p = plans.get(i); var row = history.records().get(i);
            if (p.generation() != i + 1 || row.generation() != i + 1 || !p.parentId().equals(parent)
                    || !p.incumbentId().equals(row.incumbent()) || !p.source().equals(TrainingSource.HANDCRAFTED)
                    || !p.supervision().equals(BrnSupervision.blended(.75))
                    || !p.settings().equals(controls.generationSettings(i + 1))
                    || p.splitSeed() != controls.seed(i + 1, TrainerConfig.SeedDomain.HOLDOUT))
                throw new IOException("Frozen control settings/ancestry differ at g" + (i + 1));
            var data = CheckpointInspection.bootstrapData(actual, p);
            requireFileHashes(actual, p, data);
            var manifest = CheckpointInspection.manifest(actual.resolve("checkpoints").resolve(row.candidate()));
            if (manifest.architecture() != TrainingArchitecture.BRN2 || manifest.generation() != i + 1
                    || !manifest.parentId().equals(parent) || row.bootstrap() == null
                    || !data.hash().equals(row.bootstrap().dataHash()))
                throw new IOException("Frozen history/data mismatch at g" + (i + 1));
            pins.add(new Entry(i + 1, parent, p.hash(), data.hash(), contentHash(data)));
            parent = row.candidate();
        }
        var initial = CheckpointInspection.manifest(actual.resolve("checkpoints").resolve(plans.getFirst().parentId()));
        if (initial.architecture() != TrainingArchitecture.BRN2 || initial.generation() != 0 || initial.optimizerStep() != 0)
            throw new IOException("Frozen replay requires BRN-2 g0 / step 0.");
        var result = new FrozenReplay(actual.toString(), initial.id(), initial.networkSha256(), initial.trainingSha256(),
                controls.runSeeds(), controls.selfPlay(), controls.training(), controls.startingFen(), controls.brnLearningRate(), pins);
        result.initialState(); // Complete codec, exact initialization and optimizer verification before a destination writer exists.
        return result;
    }

    private TrainerConfig controls(Path root, long maximumGenerations) {
        return new TrainerConfig(root, seeds.masterSeed(), selfPlay, training,
                new TrainerConfig.Validation(64, selfPlay.minimumOpeningPlies(), selfPlay.maximumOpeningPlies(),
                        selfPlay.depth(), selfPlay.threads(), selfPlay.maximumPlies(), NnueScoreMapping.V1, PromotionPolicy.DEFAULT),
                maximumGenerations, TrainerConfig.DepthChange.REQUIRE_SAME, startingFen, TrainingArchitecture.BRN2,
                learningRate, TrainingSource.FROZEN_REPLAY).withRunSeeds(seeds).withSupervision(BrnSupervision.WDL).withTeacherStore("");
    }
    public TrainerConfig config(Path root) throws IOException { return controls(root, 0).withFrozenReplayHash(hash()); }
    public void requireConfig(TrainerConfig config) throws IOException {
        if (config.architecture() != TrainingArchitecture.BRN2 || !TrainingSource.FROZEN_REPLAY.equals(config.source())
                || !BrnSupervision.WDL.equals(config.supervision()) || !"".equals(config.teacherStore())
                || !config.generationSettings(1).equals(config(config.checkpointRoot()).generationSettings(1))
                || config.brnLearningRate() != learningRate)
            throw new IOException("Frozen replay controls/objective cannot be reconfigured.");
    }
    public static String contentHash(BootstrapData data) throws IOException {
        return new BootstrapData("0".repeat(64), data.partition(), data.statistics(), 0).hash();
    }
    public Entry entry(long generation) throws IOException {
        if (generation < 1 || generation > entries.size()) throw new IOException("Frozen corpus exhausted at g" + generation);
        return entries.get((int) generation - 1);
    }
    /** Read and validate the exact bytes that will be consumed, not a separate unchecked reread. */
    public BootstrapData data(long generation) throws IOException {
        var e = entry(generation); Path source = Path.of(sourceStore);
        var p = CheckpointInspection.bootstrapPlan(source, e.parent());
        if (!p.hash().equals(e.planHash()) || p.generation() != generation
                || !p.source().equals(TrainingSource.HANDCRAFTED) || !p.supervision().equals(BrnSupervision.blended(.75))
                || !p.settings().equals(controls(source, 0).generationSettings(generation)))
            throw new IOException("Frozen source plan changed at g" + generation);
        var data = CheckpointInspection.bootstrapData(source, p);
        requireFileHashes(source, p, data);
        if (!data.hash().equals(e.dataHash()) || !contentHash(data).equals(e.contentHash()))
            throw new IOException("Frozen source data changed at g" + generation);
        return data;
    }
    private static void requireFileHashes(Path source, BootstrapPlan plan, BootstrapData data) throws IOException {
        Path directory = source.resolve("bootstrap");
        if (!SmallRecord.hash(directory.resolve(plan.parentId() + ".plan")).equals(plan.hash())
                || !SmallRecord.hash(directory.resolve(plan.parentId() + ".data")).equals(data.hash()))
            throw new IOException("Frozen source bytes are unstable or not canonical.");
    }
    public void requireData(long generation, BootstrapData local) throws IOException {
        if (!contentHash(local).equals(entry(generation).contentHash()))
            throw new IOException("Replay data differs from frozen corpus at g" + generation);
    }
    public byte[] initialState() throws IOException {
        Path directory = Path.of(sourceStore).resolve("checkpoints").resolve(initialId);
        byte[] state = Files.readAllBytes(directory.resolve("training.state"));
        byte[] model = Files.readAllBytes(directory.resolve("network.brn2"));
        if (!SmallRecord.hash(state).equals(optimizerHash) || !SmallRecord.hash(model).equals(modelHash))
            throw new IOException("Frozen initial payload changed.");
        var restored = Brn2Codec.decodeTraining(state); Brn2Codec.decodeModel(model);
        if (!Arrays.equals(Brn2Codec.encodeModel(restored.snapshot()), model)
                || !Arrays.equals(Brn2Codec.encodeTraining(new Brn2Trainer(learningRate)), state))
            throw new IOException("Frozen initial model/optimizer differs from canonical fresh initialization.");
        return state;
    }
    public void verify(Path student) throws IOException {
        requireSeparate(student);
        initialState();
        for (var entry : entries) data(entry.generation());
        Path checkpoints = student.resolve("checkpoints");
        if (Files.isDirectory(checkpoints)) try (var files = Files.list(checkpoints)) {
            if (files.findAny().isPresent()) {
                var initial = CheckpointInspection.manifest(checkpoints.resolve(initialId));
                if (initial.architecture() != TrainingArchitecture.BRN2 || initial.generation() != 0
                        || initial.optimizerStep() != 0 || initial.trainingDepth() != selfPlay.depth()
                        || !initial.networkSha256().equals(modelHash) || !initial.trainingSha256().equals(optimizerHash))
                    throw new IOException("Replay initial checkpoint differs from the frozen control.");
            }
        }
        // Bind provenance to existing attempts/plans too: replacing the root pin cannot silently restart work.
        String suffix = SETTINGS_PREFIX + hash();
        var attempt = GenerationAttempt.inspect(student);
        if (attempt.isPresent() && (!attempt.get().source().frozen() || !attempt.get().settings().endsWith(suffix)))
            throw new IOException("Frozen attempt identity differs; restart/reconfiguration is forbidden.");
        Path bootstrap = student.resolve("bootstrap");
        if (Files.isDirectory(bootstrap)) try (var files = Files.newDirectoryStream(bootstrap, "*.plan")) {
            for (var file : files) {
                var plan = BootstrapPlan.read(file);
                if (!plan.source().frozen() || !plan.supervision().equals(BrnSupervision.WDL)
                        || !plan.settings().equals(config(student).generationSettings(plan.generation())))
                    throw new IOException("Frozen destination plan identity differs.");
                entry(plan.generation());
                Path local = bootstrap.resolve(plan.parentId() + ".data");
                if (Files.exists(local)) {
                    var data = BootstrapData.read(local);
                    if (!data.planHash().equals(plan.hash())) throw new IOException("Frozen local plan/data mismatch.");
                    requireData(plan.generation(), data);
                }
            }
        }
    }
    public void requireSeparate(Path student) throws IOException {
        Path source = Path.of(sourceStore).toRealPath(), destination = realLocation(student.toAbsolutePath().normalize());
        if (!source.toString().equals(sourceStore) || source.startsWith(destination) || destination.startsWith(source))
            throw new IOException("Frozen source and replay student must be separate, non-nested canonical folders.");
    }
    private static Path realLocation(Path path) throws IOException {
        return Files.exists(path) ? path.toRealPath() : realLocation(path.getParent()).resolve(path.getFileName()).normalize();
    }
    public String hash() throws IOException { return SmallRecord.hash(encode()); }
    byte[] encode() throws IOException {
        return SmallRecord.encode(KIND, out -> {
            out.writeUTF(sourceStore); out.writeUTF(initialId); out.writeUTF(modelHash); out.writeUTF(optimizerHash);
            out.writeLong(seeds.masterSeed()); out.writeLong(seeds.dataSeed());
            out.writeInt(selfPlay.depth()); out.writeInt(selfPlay.threads()); out.writeInt(selfPlay.games());
            out.writeInt(selfPlay.minimumOpeningPlies()); out.writeInt(selfPlay.maximumOpeningPlies());
            out.writeInt(selfPlay.maximumSamplesPerGame()); out.writeInt(selfPlay.maximumPlies());
            out.writeBoolean(training.shuffle()); out.writeUTF(startingFen); out.writeDouble(learningRate);
            out.writeInt(entries.size());
            for (var e : entries) { out.writeLong(e.generation()); out.writeUTF(e.parent()); out.writeUTF(e.planHash()); out.writeUTF(e.dataHash()); out.writeUTF(e.contentHash()); }
        });
    }
    public static Optional<FrozenReplay> read(Path student) throws IOException {
        Path path = student.resolve(FILE);
        return Files.notExists(path) ? Optional.empty() : Optional.of(SmallRecord.read(path, KIND, in -> {
            String source = in.readUTF(), initial = in.readUTF(), model = in.readUTF(), state = in.readUTF();
            var seeds = new BrnRunSeeds(in.readLong(), in.readLong());
            var generation = new TrainerConfig.SelfPlay(in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), NnueScoreMapping.V1);
            var training = new TrainerConfig.Training(1, 1, in.readBoolean()); String fen = in.readUTF(); double rate = in.readDouble();
            int count = in.readInt(); if (count < 1 || count > MAX_GENERATIONS) throw new IOException("Invalid frozen corpus length.");
            var entries = new ArrayList<Entry>();
            for (int i = 0; i < count; i++) entries.add(new Entry(in.readLong(), in.readUTF(), in.readUTF(), in.readUTF(), in.readUTF()));
            return new FrozenReplay(source, initial, model, state, seeds, generation, training, fen, rate, entries);
        }));
    }
}
