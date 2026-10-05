package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.training.service.TrainerSnapshot;
import com.ohinteractive.seedv6.training.telemetry.OptimizationSnapshot;
import java.awt.*;
import java.util.Locale;
import javax.swing.*;

/** Common rich progress view; adapters supply known facts instead of inferring historical recipes. */
final class TrainingProgressView extends JPanel {
    private final JLabel identity = new JLabel("Training progress");
    private final JProgressBar progress = new JProgressBar(0, 1000);
    private final JLabel[] metrics = new JLabel[8];
    private final JTextArea detail = new JTextArea(3, 24);
    private final boolean compact;
    TrainingProgressView(String prefix) {
        this(prefix, false);
    }
    TrainingProgressView(String prefix, boolean compact) {
        super(new BorderLayout(8, compact ? 4 : 8)); this.compact = compact; setName(prefix); setOpaque(false);
        SeedTheme.padding(this, compact ? 2 : 10, 12, compact ? 2 : 10, 12);
        var heading = new JPanel(new BorderLayout(0, 6)); heading.setOpaque(false);
        identity.setName(prefix + "Identity"); identity.setFont(SeedTheme.font(14, Font.BOLD));
        progress.setName(prefix + "Samples"); progress.setStringPainted(true);
        heading.add(identity, BorderLayout.NORTH); heading.add(progress); add(heading, BorderLayout.NORTH);
        var values = new JPanel(new GridLayout(compact ? 3 : 4, compact ? 3 : 2, 10, compact ? 3 : 5)); values.setOpaque(false);
        String[] names = {"LearningRate", "Minibatch", "Epochs", "Updates", "Step", "Loss", "Elapsed", "Exposure"};
        for (int i = 0; i < metrics.length; i++) {
            metrics[i] = new JLabel(); metrics[i].setName(prefix + names[i]); values.add(metrics[i]);
        }
        add(values); detail.setEditable(false); detail.setLineWrap(true); detail.setWrapStyleWord(true); detail.setOpaque(false);
        detail.setFont(SeedTheme.font(11, Font.PLAIN)); detail.setName(prefix + "Detail");
        detail.setPreferredSize(new Dimension(1, SeedTheme.scale(compact ? 32 : 70))); add(detail, BorderLayout.SOUTH);
        setMinimumSize(new Dimension(320, compact ? 150 : 210)); showProgress(null);
    }
    void showProgress(OptimizationSnapshot value) {
        if (value == null) {
            identity.setText("Training progress"); progress.setValue(0); progress.setString("Waiting for optimizer progress");
            for (var metric : metrics) metric.setText("\u2014"); detail.setText("Actual recipe, loss and optimizer state appear when training starts."); return;
        }
        identity.setText(value.identity()); identity.setToolTipText(value.identity());
        progress.setValue(value.targetSamples() <= 0 ? 0 : (int) Math.min(1000, 1000.0 * value.samples() / value.targetSamples()));
        progress.setString(count(value.samples()) + " / " + (value.targetSamples() > 0 ? count(value.targetSamples()) : "unknown") + " sample visits");
        metrics[0].setText("LR: " + (value.learningRate() == null ? "unknown / inherited" : Double.toString(value.learningRate())));
        metrics[1].setText("Minibatch: " + known(value.minibatch())); metrics[2].setText("Epochs: " + known(value.epochs()));
        metrics[3].setText("Updates: " + count(value.updates()));
        metrics[4].setText("Step: " + count(value.initialStep()) + " \u2192 " + count(value.step()));
        metrics[5].setText("Mean loss: " + (Double.isFinite(value.meanLoss()) ? String.format(Locale.ROOT, "%.6g", value.meanLoss()) : "unavailable"));
        metrics[6].setText("Elapsed: " + Math.max(0, value.elapsedNanos()) / 1_000_000_000L + "s");
        metrics[7].setText("Total exposure: " + (value.totalExposure() == null ? "unknown" : count(value.totalExposure())));
        String speed = value.invocationSamples() < 0 || value.elapsedNanos() <= 0 ? "" : String.format(Locale.ROOT, " \u00b7 %.1f visits/s", value.invocationSamples() * 1e9 / value.elapsedNanos());
        detail.setText(value.phase() + (compact ? " \u00b7 " : "\n") + value.elapsedScope() + speed
                + "\nSample visits include repeated epochs; exposure is not unique positions.");
        for (var metric : metrics) metric.setToolTipText(metric.getText());
    }
    void showTraining(TrainingController.ViewState view) {
        TrainerSnapshot s = view.snapshot();
        if (s == null) { showProgress(null); return; }
        var run = s.run().orElse(null); var config = run == null || !run.generationSettingsKnown() ? null : run.effective();
        boolean online = !view.settings().architecture().nnueFamily() && view.settings().architecture() != NetworkArchitecture.BRN3;
        showProgress(new OptimizationSnapshot(view.settings().architecture().trainingArchitecture().displayName() + " - Generation " + s.generation(), s.state().toString(),
                config == null ? 0 : online ? 1 : config.training().epochs(), config == null ? 0 : online ? 1 : config.training().minibatchSize(),
                run == null ? null : run.optimizerLearningRate(), s.generationSamplesTrained(), run == null ? 0 : run.trainingSampleTarget(),
                s.generationOptimizerUpdates(), Math.max(0, s.optimizerStep() - s.generationOptimizerUpdates()), s.optimizerStep(), s.meanTrainingLoss(),
                s.generationElapsed(System.nanoTime()).map(java.time.Duration::toNanos).orElse(0L), "Generation active time, including generation and validation", null, -1));
    }
    private static String known(int value) { return value > 0 ? Integer.toString(value) : "unknown"; }
    private static String count(long value) { return String.format(Locale.ROOT, "%,d", value); }
}
