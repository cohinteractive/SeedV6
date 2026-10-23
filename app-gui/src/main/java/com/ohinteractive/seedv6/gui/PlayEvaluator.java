package com.ohinteractive.seedv6.gui;

import java.io.IOException;
import com.ohinteractive.seedv6.search.driver.BaselineEngine;
import com.ohinteractive.seedv6.search.driver.ConventionalNnueEngine;
import com.ohinteractive.seedv6.search.driver.PinnedSearch;
import com.ohinteractive.seedv6.core.brnpair2.ProductionModel;
import java.nio.file.Files;
import java.nio.file.Path;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;

/** Immutable game binding to HCE or a validated persisted network. */
record PlayEvaluator(Mode mode, String checkpointId, String networkHash, SearchEvaluation evaluation, com.ohinteractive.seedv6.training.model.TrainingArchitecture architecture, com.ohinteractive.seedv6.training.model.ModelLibrary.Binding binding, PinnedSearch pinned, ConventionalNnueEngine conventional, BaselineEngine baseline) {
    PlayEvaluator {
        java.util.Objects.requireNonNull(mode);
        if ((mode == Mode.CONVENTIONAL_CP64) != (conventional != null)
                || conventional != null && (evaluation != conventional.evaluation()
                || !ConventionalNnueEngine.SHA256.equals(networkHash) || !"64".equals(checkpointId)))
            throw new IllegalArgumentException("Conventional gameplay requires its verified checkpoint-64 binding");
        if (baseline != null && (!mode.competitive() || evaluation != baseline.evaluation()))
            throw new IllegalArgumentException("Competitive gameplay requires its shared baseline evaluator");
        if (mode.pinned() != (pinned != null) || pinned != null && (pinned.policy() != mode.policy()
                || evaluation != pinned.evaluation() || !ProductionModel.SHA256.equals(networkHash)
                || !ProductionModel.CHECKPOINT.equals(checkpointId)
                || architecture != com.ohinteractive.seedv6.training.model.TrainingArchitecture.BRN_PAIR2))
            throw new IllegalArgumentException("Pinned gameplay requires its verified Pair-2 generation-32 Search binding");
    }
    PlayEvaluator(Mode mode, String id, String hash, SearchEvaluation evaluation,
                  com.ohinteractive.seedv6.training.model.TrainingArchitecture architecture,
                  com.ohinteractive.seedv6.training.model.ModelLibrary.Binding binding, PinnedSearch pinned) {
        this(mode, id, hash, evaluation, architecture, binding, pinned, null, null);
    }
    PlayEvaluator(Mode mode, String id, String hash, SearchEvaluation evaluation,
                  com.ohinteractive.seedv6.training.model.TrainingArchitecture architecture,
                  com.ohinteractive.seedv6.training.model.ModelLibrary.Binding binding) {
        this(mode, id, hash, evaluation, architecture, binding, null);
    }
    PlayEvaluator(Mode mode, String id, String hash, SearchEvaluation evaluation,
                  com.ohinteractive.seedv6.training.model.TrainingArchitecture architecture) {
        this(mode, id, hash, evaluation, architecture, null);
    }
    PlayEvaluator(Mode mode, String id, String hash, SearchEvaluation evaluation) {
        this(mode, id, hash, evaluation, mode == Mode.HANDCRAFTED ? null
                : com.ohinteractive.seedv6.training.model.TrainingArchitecture.NNUE);
    }
    static PlayEvaluator fromCheckpoint(CheckpointStore.Checkpoint checkpoint) {
        return new PlayEvaluator(Mode.BEST_NNUE, checkpoint.manifest().id(), checkpoint.manifest().networkSha256(),
                checkpoint.model().evaluation(TrainingSettings.SCORE_MAPPING), checkpoint.manifest().architecture());
    }
    static PlayEvaluator fromCheckpoint(Path root, CheckpointStore.Checkpoint checkpoint) throws IOException {
        var manifest = checkpoint.manifest();
        var entry = com.ohinteractive.seedv6.training.model.ModelLibrary.entry(root, manifest.architecture());
        var binding = new com.ohinteractive.seedv6.training.model.ModelLibrary.Binding(root,
                entry.lineage().map(com.ohinteractive.seedv6.training.checkpoint.TrainingLineage::id),
                entry.name(), manifest.architecture(), manifest.id(), manifest.generation());
        return new PlayEvaluator(Mode.BEST_NNUE, manifest.id(), manifest.networkSha256(),
                checkpoint.model().evaluation(TrainingSettings.SCORE_MAPPING), manifest.architecture(), binding);
    }
    enum Mode {
        HANDCRAFTED("HCE"), BEST_NNUE("Best NNUE"),
        POLICY4_PAIR2("Policy 4 / Pair-2 g32"), CONSTRUCTION_V1("construction-v1 / Pair-2 g32 (experimental)"),
        CONVENTIONAL_CP64("Conventional NNUE CP64"), BREP2_32("BREP2-32");
        static Mode[] competitiveChoices() { return new Mode[]{HANDCRAFTED, BREP2_32, CONVENTIONAL_CP64}; }
        boolean competitive() { return this == HANDCRAFTED || this == BREP2_32 || this == CONVENTIONAL_CP64; }
        boolean bundled() { return pinned() || this == CONVENTIONAL_CP64 || this == BREP2_32; }
        boolean pinned() { return this == POLICY4_PAIR2 || this == CONSTRUCTION_V1; }
        PinnedSearch.Policy policy() {
            return switch (this) {
                case POLICY4_PAIR2 -> PinnedSearch.Policy.POLICY4;
                case CONSTRUCTION_V1 -> PinnedSearch.Policy.CONSTRUCTION_V1;
                default -> throw new IllegalArgumentException("Not a pinned gameplay profile: " + this);
            };
        }
        private final String label;
        Mode(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    boolean neural() { return mode != Mode.HANDCRAFTED; }
    String scoreUnits() {
        if (baseline != null) return competitiveScoreUnits(baseline.evaluator());
        return conventional != null ? "NNUE score / 100" : neural() && baseline == null
                ? "uncalibrated network units" : "pawns (cp / 100)";
    }
    String scoreExplanation() {
        if (baseline != null) return competitiveScoreExplanation(baseline.evaluator());
        return conventional != null
                ? "Searched NNUE link score / 100, White perspective. CP64 uses a teacher expected-score scale, not material pawns or a calibrated win probability."
                : neural() && baseline == null
                    ? "Uncalibrated network units, White perspective; not centipawns or a winning probability."
                    : "Searched score in pawn units (centipawns / 100), White perspective; includes positional and tactical effects and is not a winning probability.";
    }
    static String competitiveScoreUnits(BaselineEngine.Evaluator choice) {
        return choice == BaselineEngine.Evaluator.CONVENTIONAL_CP64 ? "NNUE score / 100" : "pawns (cp / 100)";
    }
    static String competitiveScoreExplanation(BaselineEngine.Evaluator choice) {
        return choice == BaselineEngine.Evaluator.CONVENTIONAL_CP64
                ? "Searched NNUE link score / 100, White perspective. CP64 uses a teacher expected-score scale, not material pawns or a calibrated win probability."
                : "Searched score in pawn units (centipawns / 100), White perspective; includes positional and tactical effects and is not a winning probability.";
    }
    static PlayEvaluator bundled(Mode mode) throws IOException {
        if (!mode.competitive()) return pinned(mode);
        var engine = BaselineEngine.load(mode == Mode.BREP2_32 ? BaselineEngine.Evaluator.BREP2_32
                : BaselineEngine.Evaluator.CONVENTIONAL_CP64);
        return new PlayEvaluator(mode, mode == Mode.BREP2_32 ? ProductionModel.CHECKPOINT : "64",
                mode == Mode.BREP2_32 ? ProductionModel.SHA256 : ConventionalNnueEngine.SHA256, engine.evaluation(),
                mode == Mode.BREP2_32 ? com.ohinteractive.seedv6.training.model.TrainingArchitecture.BRN_PAIR2 : null,
                null, null, engine.conventional(), engine);
    }
    int effectiveWorkers(int requested) { return requested; }
    static PlayEvaluator pinned(Mode mode) throws IOException {
        var engine = PinnedSearch.load(mode.policy());
        return new PlayEvaluator(mode, ProductionModel.CHECKPOINT, ProductionModel.SHA256,
                engine.evaluation(), com.ohinteractive.seedv6.training.model.TrainingArchitecture.BRN_PAIR2, null, engine);
    }

    static PlayEvaluator handcrafted() {
        try {
            var engine = BaselineEngine.load(BaselineEngine.Evaluator.HCE);
            return new PlayEvaluator(Mode.HANDCRAFTED, "", "", engine.evaluation(), null, null, null, null, engine);
        } catch (IOException impossible) { throw new AssertionError(impossible); }
    }

    static PlayEvaluator loadBest(Path root) throws IOException {
        if (root == null || !Files.isDirectory(root) || !Files.isDirectory(root.resolve("checkpoints"))) {
            throw new IOException("No checkpoint store at this folder. Start training to establish a bootstrap best before playing.");
        }
        try {
            var best = CheckpointStore.readBestSnapshot(root);
            recognize(root, best);
            return fromCheckpoint(root, best);
        } catch (IOException failure) {
            throw new IOException("Best checkpoint is unavailable, corrupt or incompatible. Store was preserved: "
                    + failure.getMessage(), failure);
        }
    }

    static PlayEvaluator load(Path root, String checkpointId) throws IOException {
        if (checkpointId.isEmpty()) return loadBest(root);
        var checkpoint = CheckpointStore.readSnapshot(root, checkpointId);
        recognize(root, checkpoint);
        return fromCheckpoint(root, checkpoint);
    }

    static void recognize(Path root, CheckpointStore.Checkpoint best) throws IOException {
        if (com.ohinteractive.seedv6.training.checkpoint.CheckpointInspection.freshRoot(root, best.manifest().architecture()))
            throw new IOException("The selected folder has no initialized checkpoint store.");
    }

    String description() {
        if (mode.bundled()) return mode.toString();
        if (binding != null) return binding.description();
        return mode == Mode.BEST_NNUE ? NetworkArchitecture.valueOf(architecture.name()) + " \u00b7 Gen " + TrainingProgress.generation(java.util.OptionalLong.empty(), checkpointId) : "Handcrafted evaluator";
    }

    String identity() {
        if (baseline != null) return baseline.identity(1, 64);
        if (pinned != null) return mode + " | model.sha256=" + networkHash + " | Checkpoint: " + checkpointId;
        return mode == Mode.BEST_NNUE ? (binding == null ? "" : binding.description() + " | Lineage: " + binding.lineageId().map(Object::toString).orElse("not recorded") + " | ") + "Checkpoint: " + checkpointId + " | Network SHA-256: " + networkHash : description();
    }

    String identity(int workers) { return baseline != null ? baseline.identity(workers, 64) : pinned == null ? identity() : pinned.identity(workers, 64); }

    static String shortId(String id) {
        if (id == null || id.isEmpty()) return "—";
        return id.length() <= 25 ? id : id.substring(0, 25) + "…";
    }
}
