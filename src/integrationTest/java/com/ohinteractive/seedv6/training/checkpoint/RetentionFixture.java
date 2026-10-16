package com.ohinteractive.seedv6.training.checkpoint;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.nnue.*;
import com.ohinteractive.seedv6.training.selfplay.GameTermination;
import com.ohinteractive.seedv6.training.validation.*;
import static com.ohinteractive.seedv6.training.checkpoint.CheckpointManifest.*;

/** Real codecs/evidence, small synthetic generation history; payload hard links avoid large test copies. */
public final class RetentionFixture {
    public static final AdamHyperparameters HP = new AdamHyperparameters(.007, .8, .95, 1e-7);
    private final Path root, template;
    private final String networkHash, trainingHash;
    public RetentionFixture(Path root, Path template) throws IOException {
        this.root = root; this.template = template;
        networkHash = SmallRecord.hash(template.resolve(NETWORK_FILE));
        trainingHash = SmallRecord.hash(template.resolve(TRAINING_FILE));
    }
    public static void template(Path path) throws IOException {
        Files.createDirectories(path);
        var trainer = new NnueTrainer(TrainableNnue.initialized(71), HP);
        try (var out = new BufferedOutputStream(Files.newOutputStream(path.resolve(NETWORK_FILE)))) {
            NnueNetworkCodec.write(trainer.model().snapshot(), out);
        }
        try (var out = new BufferedOutputStream(Files.newOutputStream(path.resolve(TRAINING_FILE)))) { TrainingStateCodec.write(trainer, out); }
    }
    public CheckpointManifest add(long generation, String parent) throws IOException {
        var manifest = CheckpointManifest.create(new Metadata(generation, 1, parent), 0, networkHash, trainingHash);
        Path directory = Files.createDirectory(root.resolve("checkpoints").resolve(manifest.id()));
        Files.write(directory.resolve(MANIFEST_FILE), manifest.encode());
        for (String name : List.of(NETWORK_FILE, TRAINING_FILE)) {
            try { Files.createLink(directory.resolve(name), template.resolve(name)); }
            catch (IOException | UnsupportedOperationException unavailable) { Files.copy(template.resolve(name), directory.resolve(name)); }
        }
        latest(manifest.id());
        return manifest;
    }
    public PromotionRecord bootstrap(CheckpointManifest manifest) throws IOException {
        var record = PromotionRecord.create(PromotionRecord.Kind.BOOTSTRAP, 0, manifest.generation(), manifest.id(), "", "", "");
        promotion(record); best(record); return record;
    }
    public PromotionRecord accept(CheckpointManifest manifest, PromotionRecord previous) throws IOException {
        var validation = validation(manifest.id(), previous.checkpointId(), true);
        var record = PromotionRecord.create(PromotionRecord.Kind.PROMOTION, previous.sequence() + 1,
                manifest.generation(), manifest.id(), previous.checkpointId(), previous.id(), validation.id());
        promotion(record); best(record); return record;
    }
    public void promotion(PromotionRecord record) throws IOException { Files.write(root.resolve("promotions").resolve(record.id()), record.encode()); }
    public ValidationRecord validation(String candidate, String incumbent, boolean promote) throws IOException {
        var config = new ValidationConfig(1, 512, 0, 0, 1, 1, new NnueScoreMapping(1000000), 16);
        long[] board = Board.startingPosition();
        var result = new ValidationResult(config, ValidationArena.stateHash(board, GameHistory.initial(board)), List.of(
                new ValidationResult.Pair("fixture", new ValidationResult.Game(GameTermination.WHITE_CHECKMATES_BLACK, 7),
                        new ValidationResult.Game(promote ? GameTermination.BLACK_CHECKMATES_WHITE : GameTermination.WHITE_CHECKMATES_BLACK, 4))));
        var record = ValidationRecord.create(candidate, incumbent, result, new PromotionPolicy(1, .9, 0));
        Files.write(root.resolve("validations").resolve(record.id()), record.encode()); return record;
    }
    public void latest(String id) throws IOException { reference("latest-training", id, ""); }
    public void best(PromotionRecord record) throws IOException { reference("best", record.checkpointId(), record.id()); }
    private void reference(String name, String id, String evidence) throws IOException {
        Files.write(root.resolve("refs").resolve(name), SmallRecord.encode("reference-" + name, out -> { out.writeUTF(id); out.writeUTF(evidence); }));
    }
    public Path artifact(CheckpointManifest manifest, String name) { return root.resolve("checkpoints").resolve(manifest.id()).resolve(name); }
}
