package com.ohinteractive.seedv6.gui;

import java.awt.*;
import javax.swing.*;

/** Common recipe editor. Architecture capabilities determine which parameters actually apply. */
final class TrainingRecipePanel extends JPanel {
    record Values(Double learningRate, int minibatch, int epochs) {}
    private final RecipeLearningRatePanel learningRate;
    private final JSpinner minibatch = new JSpinner(new SpinnerNumberModel(128, 1, 100000, 1));
    private final JSpinner epochs = new JSpinner(new SpinnerNumberModel(8, 1, 100000, 1));
    private final JPanel batches = new JPanel(new GridBagLayout());
    private final JLabel online = new JLabel("One shuffled online pass per generation (batch 1, epoch 1)");
    private boolean batched;
    TrainingRecipePanel(TrainingSettings settings) {
        super(new BorderLayout(0, 6)); setOpaque(false);
        learningRate = new RecipeLearningRatePanel(settings); add(learningRate, BorderLayout.NORTH);
        batches.setOpaque(false); SeedTheme.padding(batches, 0, 14, 12, 14);
        minibatch.setName("trainingMinibatch"); epochs.setName("trainingEpochs");
        TrainingPanel.row(batches, 0, "Batch Size", minibatch); TrainingPanel.row(batches, 1, "Training Epochs", epochs);
        add(batches); add(online, BorderLayout.SOUTH); load(settings);
    }
    void load(TrainingSettings settings) {
        batched = settings.architecture().usesMinibatches(); batches.setVisible(batched); online.setVisible(!batched);
        minibatch.setValue(settings.minibatch()); epochs.setValue(settings.epochs()); learningRate.load(settings);
    }
    Values read() throws java.text.ParseException {
        minibatch.commitEdit(); epochs.commitEdit();
        return new Values(learningRate.read(), batched ? ((Number) minibatch.getValue()).intValue() : 1,
                batched ? ((Number) epochs.getValue()).intValue() : 1);
    }
    void setEditable(boolean value) { learningRate.setEditable(value); minibatch.setEnabled(value && batched); epochs.setEnabled(value && batched); }
}
