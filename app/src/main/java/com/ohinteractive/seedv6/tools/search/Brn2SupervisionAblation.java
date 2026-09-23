package com.ohinteractive.seedv6.tools.search;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.ToDoubleFunction;
import java.util.regex.Pattern;
import com.ohinteractive.seedv6.core.brn2.*;
import com.ohinteractive.seedv6.core.nnue.NnueEvaluator;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.history.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.selfplay.TrajectorySampler.Sample;
import com.ohinteractive.seedv6.training.validation.*;
import static com.ohinteractive.seedv6.tools.search.DiagnosticReport.*;

/** Narrow, offline canonical g128 experiment. Never opens a store writer or calls self-play/search.
 * Exported snapshots deliberately have no production store identity, promotion records or refs.
 */
public final class Brn2SupervisionAblation {
    static final String TEACHER_HASH = "3279ff72654f56c8f5ce3cc89cc55d1f253de6ae9df00bd61a41293f57dd16a9";
    static final String G0_MODEL_HASH = "195d4300ce1b90a30cb888d6990a872f33165bfadd66cf1f0212ba3f0c2f653f";
    static final String G0_TRAINING_HASH = "dec6eccdf4d9e59d4ea9f9843831548a045e07d050032f3c4ab62767d8c595bc";
    static final String ACCEPTED_REPLAY_HASH = "20ebb6aaf994d774301995a985ee832763c139ddd145c0769b925b8ee7fe9f4c";
    private static final Set<Integer> MILESTONES = Set.of(0, 32, 64, 96, 128);
    record Arm(String name, double teacherWeight) {
        static final Arm WDL = new Arm("WDL", 0), BLENDED = new Arm("BLENDED", .5), TEACHER = new Arm("TEACHER", 1);
        Arm {
            if (!Double.isFinite(teacherWeight) || teacherWeight < 0 || teacherWeight > 1)
                throw new IllegalArgumentException("Teacher weight must be finite and in [0, 1]");
        }
        static Arm weighted(double weight) {
            if (weight == 0) return WDL;
            if (weight == .5) return BLENDED;
            if (weight == 1) return TEACHER;
            return new Arm("WEIGHT" + Double.toString(weight).replace('.', 'p'), weight);
        }
        double target(double wdl, double teacher) {
            // Preserve the exact endpoint definitions, including signed zero; .5 retains its old arithmetic.
            return com.ohinteractive.seedv6.training.service.BrnSupervision.blended(teacherWeight).target(wdl, teacher);
        }
        @Override public String toString() { return name; }
    }
    record Batch(CheckpointManifest manifest, BootstrapPlan plan, BootstrapData data, GenerationRecord history) {}
    record Snapshot(int generation, byte[] model, byte[] training) {}

    public static void main(String[] args) throws Exception {
        if (args.length != 3 && args.length != 5) throw new IllegalArgumentException(
                "Expected SOURCE_STORE EXACT_NNUE_CHECKPOINT NEW_EXPERIMENT_DIRECTORY [TEACHER_WEIGHTS_CSV ACCEPTED_REPLAY_JSONL]");
        List<Arm> requested = args.length == 3 ? List.of() : parseWeights(args[3]);
        if (!requested.isEmpty()) require(hash(Files.readAllBytes(Path.of(args[4]))).equals(ACCEPTED_REPLAY_HASH),
                "Explicit weights require the exact accepted 128-generation WDL gate evidence");
        Path source = Path.of(args[0]).toRealPath(), teacherPath = Path.of(args[1]).toRealPath();
        Path output = newOutput(Path.of(args[2]), source, teacherPath.getParent().getParent());
        var lineage = CheckpointInspection.lineage(source, CheckpointInspection.reference(source, "latest-training"));
        require(lineage.size() == 129, "Expected g0 through g128, without a completed g129");
        var history = new HistoryRepository(source).refresh();
        require(history.warnings().isEmpty() && history.records().size() == 128, "Invalid canonical history");
        var validations = CheckpointInspection.validations(source);
        var accepted = CheckpointInspection.accepted(source);
        require(validations.size() == 128 && accepted.size() == 65, "Unexpected canonical decisions");
        var g0 = lineage.getFirst();
        require(g0.generation() == 0 && g0.optimizerStep() == 0 && g0.networkSha256().equals(G0_MODEL_HASH)
                && g0.trainingSha256().equals(G0_TRAINING_HASH), "Wrong g0 seed state");
        var teacher = CheckpointStore.inspect(teacherPath);
        require(teacher.manifest().generation() == 74 && teacher.model() instanceof NetworkModel.Nnue
                && teacher.manifest().networkSha256().equals(TEACHER_HASH), "Wrong pinned teacher");
        var nnue = new NnueEvaluator(teacher.model().nnue());
        var initial = CheckpointStore.inspect(source.resolve("checkpoints").resolve(g0.id()));
        byte[] initialTraining = Files.readAllBytes(source.resolve("checkpoints").resolve(g0.id()).resolve("training.state"));
        require(hash(initialTraining).equals(G0_TRAINING_HASH), "g0 changed during load");
        byte[] initialModel = Brn2Codec.encodeModel(((NetworkModel.Brn2) initial.model()).model());
        require(hash(initialModel).equals(G0_MODEL_HASH), "g0 model serialization differs");
        var seed = Brn2Codec.decodeTraining(initialTraining);
        require(seed.config().learningRate() == .001 && seed.config().beta1() == .9 && seed.config().beta2() == .999
                && seed.config().epsilon() == 1e-8, "Wrong Adam configuration");
        require(Arrays.equals(initialModel, Brn2Codec.encodeModel(seed.snapshot())), "g0 optimizer/model mismatch");

        List<Batch> batches = new ArrayList<>();
        for (int generation = 1; generation <= 128; generation++) {
            var manifest = lineage.get(generation);
            var row = history.records().get(generation - 1);
            var plan = CheckpointInspection.bootstrapPlan(source, manifest.parentId());
            var data = CheckpointInspection.bootstrapData(source, plan);
            require(manifest.architecture() == TrainingArchitecture.BRN2 && manifest.trainingDepth() == 2
                    && manifest.generation() == generation && row.generation() == generation
                    && row.candidate().equals(manifest.id()) && row.incumbent().equals(plan.incumbentId())
                    && manifest.parentId().equals(lineage.get(generation - 1).id()), "Lineage mismatch at " + generation);
            require(plan.generatorId().equals(teacher.manifest().id()) && plan.generatorHash().equals(TEACHER_HASH)
                    && Path.of(plan.source().generatorStore()).toRealPath().equals(teacherPath.getParent().getParent()),
                    "Teacher pin mismatch at " + generation);
            trainingConfig(plan.settings()); // Fail closed on unrecognized settings before any updates.
            var evidence = BootstrapEvidence.create(plan, data, row.bootstrap().comparison());
            require(evidence.equals(row.bootstrap()) && evidence.equals(validations.get(manifest.id()).bootstrap()),
                    "Data/history/validation mismatch at " + generation);
            batches.add(new Batch(manifest, plan, data, row));
        }
        require(history.records().getLast().resultingBest().equals(CheckpointInspection.reference(source, "best")),
                "Best/history mismatch");
        Files.createDirectory(output);
        try (var out = new PrintWriter(Files.newBufferedWriter(output.resolve("replay.jsonl"), StandardOpenOption.CREATE_NEW))) {
            write(out, "type", "inputs", "source", source.toString(), "g0", g0.id(), "g0ModelSha256", G0_MODEL_HASH,
                    "g0TrainingSha256", G0_TRAINING_HASH, "teacher", teacherPath.toString(), "teacherSha256", TEACHER_HASH,
                    "teacherTrainingSha256", teacher.manifest().trainingSha256(), "batches", 128,
                    "target", "STM native bounded static value; target = (1-w)*WDL + w*teacher; half squared error");
            if (!requested.isEmpty()) {
                write(out, "type", "gate", "passed", true, "generations", 128, "tolerance", 0,
                        "reused", true, "acceptedReplaySha256", ACCEPTED_REPLAY_HASH,
                        "evidence", Path.of(args[4]).toRealPath().toString());
                for (Arm arm : requested) runArm(arm, batches, initialModel, initialTraining, nnue, output, out);
            } else {
                // Original invocation still runs its own exact control gate before either alternative.
                runArm(Arm.WDL, batches, initialModel, initialTraining, nnue, output, out);
                write(out, "type", "gate", "passed", true, "generations", 128, "tolerance", 0,
                        "evidence", "Exact model/training SHA-256, optimizer steps, training/held-out losses, decisions and Best generations");
                System.out.println("WDL GATE PASSED: all 128 generations exact");
                runArm(Arm.BLENDED, batches, initialModel, initialTraining, nnue, output, out);
                runArm(Arm.TEACHER, batches, initialModel, initialTraining, nnue, output, out);
            }
            write(out, "type", "end", "completed", true);
        }
    }

    static List<Arm> parseWeights(String text) {
        List<Arm> arms = Arrays.stream(text.split(",", -1)).map(Double::parseDouble).map(Arm::weighted).toList();
        if (new HashSet<>(arms).size() != arms.size()) throw new IllegalArgumentException("Duplicate teacher weights");
        return arms;
    }

    static Path newOutput(Path requested, Path source, Path teacherStore) throws IOException {
        Path absolute = requested.toAbsolutePath().normalize();
        require(Files.notExists(absolute, LinkOption.NOFOLLOW_LINKS), "Output must be new");
        // Resolve the existing parent so junctions/symlinks cannot hide overlap with either source store.
        Path resolved = absolute.getParent().toRealPath().resolve(absolute.getFileName());
        for (Path input : List.of(source.toRealPath(), teacherStore.toRealPath()))
            require(!resolved.startsWith(input) && !input.startsWith(resolved), "Output overlaps an input store");
        require(resolved.getFileName().toString().startsWith("experimental-supervision-ablation-"),
                "Use an explicit experimental-supervision-ablation-* directory");
        return resolved;
    }

    static SelfPlayTraining.Config trainingConfig(String settings) throws IOException {
        String[] parts = settings.split("\\|", -1);
        require(parts.length == 4, "Unknown saved generation settings");
        var matcher = Pattern.compile("Config\\[epochs=1, minibatchSize=1, shuffle=true, shuffleSeed=(-?[0-9]+)\\]")
                .matcher(parts[1]);
        require(matcher.matches(), "Replay requires the saved one-pass online shuffle contract");
        return new SelfPlayTraining.Config(1, 1, true, Long.parseLong(matcher.group(1)));
    }

    static double teacherValue(NnueEvaluator teacher, Sample sample) {
        return com.ohinteractive.seedv6.training.service.BrnSupervision.teacherValue(teacher, sample);
    }

    private static void runArm(Arm arm, List<Batch> batches, byte[] initialModel, byte[] initialTraining,
                               NnueEvaluator nnue, Path output, PrintWriter out) throws Exception {
        Path armPath = Files.createDirectory(output.resolve(arm.name().toLowerCase(Locale.ROOT)));
        byte[] latest = initialTraining;
        Snapshot best = new Snapshot(0, initialModel, initialTraining);
        var bestModel = new NetworkModel.Brn2(Brn2Codec.decodeModel(initialModel));
        save(armPath, 0, best, out, arm, batches, nnue);
        int promotions = 0;
        long start = System.nanoTime();
        for (int generation = 1; generation <= 128; generation++) {
            Batch batch = batches.get(generation - 1);
            // Like the service, restore the last Candidate, including Adam, even after non-promotion.
            var trainer = Brn2Codec.decodeTraining(latest);
            ToDoubleFunction<Sample> target = arm == Arm.WDL ? Sample::target
                    : sample -> arm.target(sample.target(), teacherValue(nnue, sample));
            var stats = Brn2SelfPlayTraining.trainSamples(trainer, batch.data().partition().training(),
                    trainingConfig(batch.plan().settings()), new SelfPlayControl(), ignored -> {}, target).orElseThrow();
            var candidate = new NetworkModel.Brn2(trainer.snapshot());
            byte[] modelBytes = Brn2Codec.encodeModel(candidate.model());
            latest = Brn2Codec.encodeTraining(trainer);
            String modelHash = hash(modelBytes), trainingHash = hash(latest);
            var comparison = HeldOutLoss.compare(candidate, bestModel, batch.data().partition().heldOut(), target);
            boolean promoted = comparison.decision() == PromotionPolicy.Decision.PROMOTE;
            if (arm == Arm.WDL) verifyControl(batch, modelHash, trainingHash, trainer.optimizer().step(),
                    stats.finalLoss(), comparison, best.generation());
            if (promoted) {
                promotions++;
                best = new Snapshot(generation, modelBytes, latest);
                bestModel = candidate;
            }
            write(out, "type", "generation", "arm", arm, "teacherWeight", arm.teacherWeight(), "generation", generation,
                    "sourceCandidate", batch.manifest().id(), "dataSha256", batch.data().hash(),
                    "planSha256", batch.plan().hash(), "shuffleSeed", trainingConfig(batch.plan().settings()).shuffleSeed(),
                    "trainingSamples", stats.samplesTrained(), "heldOutSamples", comparison.samples(),
                    "step", trainer.optimizer().step(), "modelSha256", modelHash, "trainingSha256", trainingHash,
                    "trainingLoss", stats.finalLoss(), "candidateLoss", comparison.candidateLoss(),
                    "incumbentLoss", comparison.bestLoss(), "promoted", promoted, "bestGeneration", best.generation());
            if (MILESTONES.contains(generation)) {
                save(armPath, generation, best, out, arm, batches, nnue);
                System.out.printf(Locale.ROOT, "%s g%d Best=g%d promotions=%d elapsed=%.1fs%n",
                        arm, generation, best.generation(), promotions, (System.nanoTime() - start) / 1e9);
            }
        }
        Path latestPath = Files.createDirectory(armPath.resolve("latest-training-g128"));
        Files.write(latestPath.resolve("training.state"), latest, StandardOpenOption.CREATE_NEW);
        Files.write(latestPath.resolve("network.brn2"), Brn2Codec.encodeModel(Brn2Codec.decodeTraining(latest).snapshot()), StandardOpenOption.CREATE_NEW);
        Files.writeString(armPath.resolve("best.txt"), "boundary-128/network.brn2\nBest generation " + best.generation() + "\n", StandardOpenOption.CREATE_NEW);
        var losses = pooledLosses(bestModel.model(), arm, batches, nnue);
        write(out, "type", "final", "arm", arm, "teacherWeight", arm.teacherWeight(), "bestGeneration", best.generation(), "promotions", promotions,
                "modelSha256", hash(best.model()), "trainingSha256", hash(best.training()), "heldOutSamples", losses.get("heldOutSamples"),
                "wdlLoss", losses.get("wdlLoss"), "blendedLoss", losses.get("blendedLoss"), "teacherLoss", losses.get("teacherLoss"),
                "ownLoss", losses.get("ownLoss"), "elapsedSeconds", (System.nanoTime() - start) / 1e9);
    }

    // The same pooled population at every boundary, including future batches: retrospective, not promotion input.
    private static Map<String, Object> pooledLosses(Brn2Model model, Arm arm, List<Batch> batches, NnueEvaluator nnue) {
        double[] sums = new double[4]; int count = 0;
        Arm[] objectives = {Arm.WDL, Arm.BLENDED, Arm.TEACHER, arm};
        var workspace = new Brn2Workspace();
        for (Batch batch : batches) for (Sample sample : batch.data().partition().heldOut()) {
            double prediction = model.evaluate(sample.board(), workspace), teacher = teacherValue(nnue, sample);
            for (int i = 0; i < objectives.length; i++) {
                double difference = prediction - objectives[i].target(sample.target(), teacher);
                sums[i] += .5 * difference * difference;
            }
            count++;
        }
        return fields("heldOutSamples", count,
                "wdlLoss", sums[0] / count, "blendedLoss", sums[1] / count, "teacherLoss", sums[2] / count,
                "ownLoss", sums[3] / count);
    }

    static void verifyControl(Batch batch, String modelHash, String trainingHash, long step, double trainingLoss,
                              HeldOutLoss.Comparison comparison, int incumbentGeneration) throws IOException {
        var expected = batch.manifest();
        require(modelHash.equals(expected.networkSha256()) && trainingHash.equals(expected.trainingSha256())
                && step == expected.optimizerStep(), "WDL payload divergence at g" + expected.generation()
                + ": model=" + modelHash + " training=" + trainingHash);
        require(Double.doubleToLongBits(trainingLoss) == Double.doubleToLongBits(batch.history().loss())
                && comparison.equals(batch.history().bootstrap().comparison())
                && comparison.decision() == batch.history().decision()
                && batch.history().incumbent().startsWith(String.format(Locale.ROOT, "g%06d-", incumbentGeneration)),
                "WDL loss/promotion divergence at g" + expected.generation());
    }

    private static void save(Path armPath, int boundary, Snapshot best, PrintWriter out, Arm arm,
                             List<Batch> batches, NnueEvaluator nnue) throws Exception {
        Path path = Files.createDirectory(armPath.resolve("boundary-" + boundary));
        Files.write(path.resolve("network.brn2"), best.model(), StandardOpenOption.CREATE_NEW);
        Files.write(path.resolve("training.state"), best.training(), StandardOpenOption.CREATE_NEW);
        var metadata = fields("arm", arm, "teacherWeight", arm.teacherWeight(), "boundary", boundary, "bestGeneration", best.generation(),
                "modelSha256", hash(best.model()), "trainingSha256", hash(best.training()));
        metadata.putAll(pooledLosses(Brn2Codec.decodeModel(best.model()), arm, batches, nnue));
        Files.writeString(path.resolve("experiment.json"), json(metadata) + "\n", StandardOpenOption.CREATE_NEW);
        write(out, "type", "milestone", "snapshot", metadata, "path", path.toString());
    }

    private static String hash(byte[] bytes) throws Exception { return Brn2Diagnostics.sha256(bytes); }
    private static void require(boolean condition, String message) throws IOException {
        if (!condition) throw new IOException(message);
    }
    private Brn2SupervisionAblation() {}
}
