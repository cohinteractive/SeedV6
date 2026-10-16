import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.data.SourceLedger;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.service.TrainingSource;

/** Bounded disk-continuation bridge: run the SAME probe against WU2 and WU3.
 * Only creates a new, explicitly supplied disposable directory. No user store. */
public final class UnifiedTrainingCompatibilityProbe {
    private static final List<TrajectorySampler.Sample> SAMPLES = List.of(
        new TrajectorySampler.Sample(Board.startingPosition(), 1),
        new TrajectorySampler.Sample(Board.fromFen("4k3/8/8/8/8/8/3P4/4K3 w - - 0 1"), 0),
        new TrajectorySampler.Sample(Board.fromFen("4k3/8/8/8/8/8/3P4/4K3 b - - 0 1"), -1));

    public static void main(String[] args) throws Exception {
        Path output = Path.of(args[0]).toAbsolutePath();
        Files.createDirectory(output); // Refuse reuse, including a historical store.
        if (args.length > 1) {
            // The driver supplies a private clone, including the original manifest/sidecar.
            Path checkpoint = Path.of(args[1]);
            Path root = checkpoint.getParent().getParent();
            String id = checkpoint.getFileName().toString();
            var saved = CheckpointStore.readTrainingSnapshot(root, id);
            var model = CheckpointStore.readSnapshot(root, id);
            require(saved.step() == 1753088 && model.manifest().generation() == 32, "Historical generation/step");
            var resolved = ModelLibrary.resolve(ModelLibrary.entry(root, TrainingArchitecture.BRN_PAIR2), id);
            require(resolved.binding().lineageId().orElseThrow().toString().equals("235d0688-e060-44e3-8f1e-119c39bc8017"), "Historical UUID");
            System.out.println("HISTORICAL\t" + id + "\t" + resolved.binding().lineageId().orElseThrow()
                + "\t" + model.manifest().networkSha256() + "\t" + model.manifest().trainingSha256());
        }
        for (var architecture : TrainingArchitecture.values()) verify(output, architecture);
    }

    private static void verify(Path output, TrainingArchitecture architecture) throws Exception {
        Path root = output.resolve(architecture.name());
        var expected = NetworkTrainingState.initialized(architecture, 2402, .001);
        var statistics = train(expected, new SelfPlayControl(), p -> {});
        var split = NetworkTrainingState.initialized(architecture, 2402, .001);
        var stop = new SelfPlayControl();
        UUID uuid = UUID.nameUUIDFromBytes(architecture.name().getBytes(StandardCharsets.UTF_8));
        var lineage = new TrainingLineage(uuid, "WU3 " + architecture, architecture, Instant.EPOCH, "fixture", "WU3 probe");
        String parent;
        byte[] parentState;
        SourceLedger.Reservation reservation;
        try (var store = new CheckpointStore(root, architecture)) {
            parent = store.initialize(split, new CheckpointManifest.Metadata(0, 1, "")).manifest().id();
            store.writeLineage(lineage);
            parentState = store.resumeState(parent).encode();
            // Corpus-only architectures retain their capability; no generated data request.
            var source = architecture.corpusOnly() ? TrainingSource.dataSources(root.resolve("training-data")) : TrainingSource.SELF_PLAY;
            var attempt = new GenerationAttempt(parent, parent, 1, source, "fixed-three-position-probe", GenerationAttempt.Format.CURRENT, "");
            store.writeGenerationAttempt(attempt);
            reservation = new SourceLedger(root).reserve(1, "probe", "mix",
                List.of(new SourceLedger.Range("a".repeat(64), 0, 3, 3, 0, 0)), "b".repeat(64), "c".repeat(64));
            train(split, stop, p -> { if (p.optimizerUpdates() == 2) stop.cancel(); });
            require(stop.trainingCursor().updates() == 2, "Cancellation boundary " + architecture);
            var games = new SelfPlayBatch.Saved(List.of(new SelfPlayBatch.GameSummary(
                0, 2402, GameTermination.WHITE_CHECKMATES_BLACK, 3, 3, 3, null)), SAMPLES);
            store.savePartial(new PartialGeneration(attempt, "", games, SelfPlayBatch.statistics(games, 3),
                stop.trainingCursor(), List.of(), Instant.EPOCH, 1, 0, 1, 0, false), split);
        }
        var progress = PartialGeneration.inspect(root).orElseThrow();
        require(progress.training().equals(stop.trainingCursor()), "Persisted cursor " + architecture);
        require(TrainingLineage.read(root).orElseThrow().equals(lineage), "Lineage identity " + architecture);
        String candidate;
        try (var store = new CheckpointStore(root, architecture)) {
            var restored = store.resumePartialState(progress);
            require(Arrays.equals(split.encode(), restored.encode()), "Partial optimizer bytes " + architecture);
            var resume = new SelfPlayControl(); resume.trainingCursor(progress.training());
            require(statistics.equals(train(restored, resume, p -> {})), "Continuation statistics " + architecture);
            require(Arrays.equals(expected.encode(), restored.encode()), "Continued weights/moments/step " + architecture);
            candidate = store.publish(restored, new CheckpointManifest.Metadata(1, 1, parent)).manifest().id();
            require(Arrays.equals(expected.encode(), store.resumeState(candidate).encode()), "Checkpoint optimizer " + architecture);
            require(Arrays.equals(parentState, store.resumeState(parent).encode()), "Immutable parent " + architecture);
            require(store.load(candidate).manifest().parentId().equals(parent), "Parent identity " + architecture);
            require(store.load(candidate).manifest().optimizerStep() == expected.step(), "Step identity " + architecture);
            require(new SourceLedger(root).active().orElseThrow().equals(reservation), "Source reservation " + architecture);
            require(new SourceLedger(root).next("a".repeat(64)) == 3, "Source cursor " + architecture);
        }
        var entry = ModelLibrary.entry(root, architecture);
        var catalog = ModelLibrary.browse(entry);
        require(catalog.best().orElseThrow().id().equals(parent), "Best separation " + architecture);
        require(catalog.latest().orElseThrow().id().equals(candidate), "Latest separation " + architecture);
        var resolved = ModelLibrary.resolve(entry, candidate);
        require(resolved.binding().lineageId().orElseThrow().equals(uuid), "Catalog UUID " + architecture);
        var manifest = resolved.checkpoint().manifest();
        System.out.println(architecture + "\t" + parent + "\t" + candidate + "\t" + manifest.networkSha256()
            + "\t" + manifest.trainingSha256() + "\t" + expected.step() + "\t" + progress.training());
    }

    private static SelfPlayTraining.Statistics train(NetworkTrainingState state, SelfPlayControl control,
            Consumer<SelfPlayTraining.Progress> observer) {
        boolean batch = state.architecture().nnueFamily() || state.architecture().corpusOnly();
        var config = new SelfPlayTraining.Config(batch ? 3 : 1, batch ? 2 : 1, true, 871);
        return switch (state) {
            case NetworkTrainingState.Nnue n -> SelfPlayTraining.trainSamples(n.trainer(), SAMPLES, config, control, observer).orElseThrow();
            case NetworkTrainingState.NnueMaterial n -> SelfPlayTraining.trainSamples(n.trainer(), SAMPLES, config, control, observer).orElseThrow();
            case NetworkTrainingState.Brn n -> BrnSelfPlayTraining.trainSamples(n.trainer(), SAMPLES, config, control, observer).orElseThrow();
            case NetworkTrainingState.Brn1 n -> Brn1SelfPlayTraining.trainSamples(n.trainer(), SAMPLES, config, control, observer).orElseThrow();
            case NetworkTrainingState.Brn2 n -> Brn2SelfPlayTraining.trainSamples(n.trainer(), SAMPLES, config, control, observer).orElseThrow();
            case NetworkTrainingState.Brn3 n -> Brn3CorpusOptimization.trainSamples(n.trainer(), SAMPLES, config, control, observer, TrajectorySampler.Sample::target).orElseThrow();
            case NetworkTrainingState.BrnPair2 n -> BrnPair2CorpusOptimization.trainSamples(n.trainer(), SAMPLES, config, control, observer, TrajectorySampler.Sample::target).orElseThrow();
        };
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
