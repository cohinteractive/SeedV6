package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.training.checkpoint.GenerationAnnotation;
import com.ohinteractive.seedv6.training.model.ModelLibrary;
import java.awt.*;
import java.time.Instant;
import java.util.Arrays;
import javax.swing.*;

/** Editable annotations over factual, read-only checkpoint/lineage evidence. */
final class GenerationDetailsPanel extends JPanel {
    private final ModelLibrary.Generation generation;
    private final JTextArea notes = new JTextArea(6, 44);
    private final JTextField tags = new JTextField();
    GenerationDetailsPanel(ModelLibrary.Snapshot snapshot, ModelLibrary.Generation generation) {
        super(new BorderLayout(8, 8)); this.generation = generation;
        notes.setName("generationNotes"); tags.setName("generationTags");
        notes.setLineWrap(true); notes.setWrapStyleWord(true);
        generation.annotation().ifPresent(a -> { notes.setText(a.notes()); tags.setText(String.join(", ", a.tags())); });
        var metadata = new JTextArea(describe(snapshot, generation), 11, 44);
        metadata.setName("generationEvidence"); metadata.setEditable(false); metadata.setLineWrap(true); metadata.setWrapStyleWord(true);
        add(new JScrollPane(metadata), BorderLayout.NORTH);
        var annotation = new JPanel(new BorderLayout(4, 4)); annotation.add(new JLabel("Your notes"), BorderLayout.NORTH); annotation.add(new JScrollPane(notes)); add(annotation);
        var row = new JPanel(new BorderLayout(8, 0)); row.add(new JLabel("Tags (comma separated)"), BorderLayout.WEST); row.add(tags); add(row, BorderLayout.SOUTH);
    }
    GenerationAnnotation annotation() {
        return new GenerationAnnotation(generation.id(), notes.getText(), Arrays.stream(tags.getText().split(","))
                .map(String::strip).filter(s -> !s.isEmpty()).toList(), Instant.now());
    }
    static String describe(ModelLibrary.Snapshot snapshot, ModelLibrary.Generation g) {
        var losses = new TrainingLoss();
        losses.update(new com.ohinteractive.seedv6.training.history.HistoryRepository.Snapshot(snapshot.records(), java.util.List.of()));
        return snapshot.lineage().architecture().displayName() + " · " + snapshot.lineage().name() + " · " + g.label()
                + "\n" + losses.summary(g.id(), null)
                + "\nLineage ID: " + snapshot.lineage().lineage().map(l -> l.id().toString()).orElse("not recorded")
                + "\nLineage metadata created: " + snapshot.lineage().lineage().map(l -> l.created().toString()).orElse("not recorded")
                + "\nConfiguration origin: " + snapshot.lineage().lineage().map(l -> l.configurationOrigin()).orElse("not recorded")
                + "\nInitialization: " + snapshot.provenance().map(p -> p.initializer()
                    + (p.initializationSeed() == null ? " (initialization seed not recorded)" : ", model seed " + p.initializationSeed())
                    + "; first run seed " + p.firstRunSeed()).orElse("not recorded")
                + "\n" + snapshot.exposureDescription()
                + "\nGeneration completed: " + g.history().map(h -> h.completed().toString()).orElse("not recorded")
                + "\nOptimizer step: " + g.checkpoint().manifest().optimizerStep()
                + "\nLearning rate: " + (g.checkpoint().hyperparameters() == null ? "not recorded" : g.checkpoint().hyperparameters().learningRate())
                + "\nRecorded samples: " + g.history().map(h -> h.samples() == null ? "not recorded" : h.samples().toString()).orElse("not recorded")
                + "\nValidation: " + g.history().map(h -> h.validationKind() + " · " + h.outcome()).orElse("not recorded")
                + "\nEffective settings: " + g.history().map(h -> h.regime().effectiveSettings()).orElse("not recorded")
                + "\nLocation: " + snapshot.lineage().root()
                + configurationHistory(snapshot)
                + (snapshot.diagnostics().isEmpty() ? "" : "\nDiagnostics: " + String.join("; ", snapshot.diagnostics()));
    }
    private static String configurationHistory(ModelLibrary.Snapshot snapshot) {
        if (snapshot.revisions().isEmpty()) return "\nPrior configuration revisions: not recorded";
        var text = new StringBuilder("\nPrior configurations: ").append(snapshot.revisions().size());
        for (var revision : snapshot.revisions().subList(Math.max(0, snapshot.revisions().size() - 10), snapshot.revisions().size())) {
            text.append("\nArchived ").append(revision.archived()).append(": ");
            try {
                var s = LineageConfiguration.decode(revision.lineage().configuration(), snapshot.lineage().root(),
                        NetworkArchitecture.valueOf(revision.lineage().architecture().name()));
                text.append("LR ").append(s.recipeLearningRate() == null ? "inherited from checkpoint" : s.recipeLearningRate())
                        .append(", minibatch ").append(s.recipe().minibatchSize()).append(", epochs ").append(s.recipe().epochs())
                        .append(", run seed ").append(s.seed());
            } catch (Exception legacy) { text.append("settings not available in this version"); }
        }
        return text.toString();
    }
}
