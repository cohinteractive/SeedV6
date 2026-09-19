package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import com.ohinteractive.seedv6.search.alphabeta.RootParallelSearch;
import com.ohinteractive.seedv6.search.quiescence.QuiescenceSearch;
import com.ohinteractive.seedv6.training.validation.PromotionPolicy;

/** Compact settings view; no filesystem, trainer lifecycle or mutable trainer state belongs here. */
final class TrainingPanel extends JPanel {
    private final JTextField root = new JTextField(20);
    private final JSpinner depth = spinner(4, 1, 256), threads = spinner(1, 1, RootParallelSearch.MAX_WORKERS);
    private final JSpinner games = spinner(64, 1, 100_000), pairs = spinner(64, 1, 100_000);
    private final JButton browse = new JButton("Browse…"), advanced = new JButton("Advanced…");
    private final JButton start = new JButton("Start / Resume Training"), stop = new JButton("Stop Training");
    private final JTextArea progress = new JTextArea(17, 32);
    private final JTextArea validation = new JTextArea(12, 32);
    private final JScrollPane validationBlock = new JScrollPane(validation);
    private final ScrollPreservingText validationText = new ScrollPreservingText(validation, validationBlock);
    private final JSplitPane outputs = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    private final List<JComponent> editors = new ArrayList<>();
    private TrainingSettings settings;
    private TrainingController controller;
    private boolean confirming;

    TrainingPanel(TrainingSettings settings) {
        super(new BorderLayout(4, 4));
        this.settings = settings;
        root.setName("trainingRoot"); depth.setName("trainingDepth"); threads.setName("trainingThreads");
        games.setName("trainingGames"); pairs.setName("trainingPairs"); progress.setName("trainingProgress");
        start.setName("startTraining"); stop.setName("stopTraining");
        setBorder(BorderFactory.createTitledBorder("NNUE Training"));
        JPanel form = new JPanel(new GridBagLayout());
        add(form, BorderLayout.NORTH);
        root.setText(settings.root().toString());
        root.setToolTipText(settings.root().toString());
        depth.setValue(settings.depth()); threads.setValue(settings.threads()); games.setValue(settings.games()); pairs.setValue(settings.validationPairs());
        row(form, 0, "Checkpoint folder", root);
        row(form, 1, "", browse);
        row(form, 2, "Training depth", depth);
        row(form, 3, "Search threads", threads);
        row(form, 4, "Games / generation", games);
        row(form, 5, "Validation pairs", pairs);
        row(form, 6, "", advanced);
        JPanel buttons = new JPanel(new GridLayout(2, 1, 2, 2));
        buttons.add(start); buttons.add(stop);
        row(form, 7, "", buttons);
        editors.addAll(List.of(root, browse, depth, threads, games, pairs, advanced));
        progress.setEditable(false); progress.setLineWrap(true); progress.setWrapStyleWord(true);
        progress.setMargin(new Insets(6, 6, 6, 6));
        progress.setFont(new Font(Font.MONOSPACED, Font.PLAIN, progress.getFont().getSize()));
        validation.setName("validationProgress");
        validation.setEditable(false); validation.setLineWrap(true); validation.setWrapStyleWord(true);
        validation.setMargin(new Insets(4, 6, 4, 6));
        validation.setFont(new Font(Font.MONOSPACED, Font.PLAIN, validation.getFont().getSize()));
        validation.setToolTipText("Scores count valid pairs only. Validation details are under Advanced.");
        validationBlock.setBorder(BorderFactory.createTitledBorder("Candidate validation"));
        validationBlock.setVisible(false);
        JScrollPane trainingBlock = new JScrollPane(progress);
        trainingBlock.setMinimumSize(new Dimension(100, 80));
        validationBlock.setMinimumSize(new Dimension(100, 100));
        outputs.setName("trainingOutputs");
        outputs.setTopComponent(trainingBlock);
        outputs.setBottomComponent(validationBlock);
        outputs.setBorder(BorderFactory.createEmptyBorder());
        outputs.setContinuousLayout(true);
        outputs.setResizeWeight(0.4);
        outputs.setDividerSize(0); // No empty validation pane before the first match.
        add(outputs, BorderLayout.CENTER);
        stop.setEnabled(false);
        browse.addActionListener(event -> {
            JFileChooser chooser = new JFileChooser(root.getText());
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                root.setText(chooser.getSelectedFile().toPath().toString()); applySettings();
            }
        });
        advanced.addActionListener(event -> advanced());
        start.addActionListener(event -> { if (applySettings()) controller.start(); });
        stop.addActionListener(event -> controller.stop());
        root.addActionListener(event -> applySettings());
        root.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent event) { if (root.isEnabled()) applySettings(); }
        });
        for (JSpinner spinner : List.of(depth, threads, games, pairs)) spinner.addChangeListener(event -> applySettings());
    }

    void bind(TrainingController controller) { this.controller = controller; showState(controller.state()); }

    boolean applySettings() {
        if (controller == null) return false;
        try {
            for (JSpinner spinner : List.of(depth, threads, games, pairs)) spinner.commitEdit();
            if (root.getText().isBlank()) throw new IllegalArgumentException("Select a checkpoint folder.");
            settings = new TrainingSettings(Path.of(root.getText()), value(depth), value(threads), value(games),
                    settings.openingMin(), settings.openingMax(), settings.samples(), settings.minibatch(), settings.epochs(),
                    value(pairs), settings.seed(), settings.maximumPlies(), settings.maximumGenerations());
            controller.setSettings(settings);
            root.setToolTipText(settings.root().toString());
            return true;
        } catch (Exception invalid) {
            JOptionPane.showMessageDialog(this, "Check training settings: " + TrainingController.concise(invalid), "Invalid training settings", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    void showState(TrainingController.ViewState state) {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Training view requires EDT.");
        boolean editable = !state.active() && state.phase() != TrainingController.Phase.CLOSING;
        editors.forEach(component -> component.setEnabled(editable));
        start.setEnabled(state.canStart());
        start.setText(state.resume() ? "Resume Training" : "Start / Resume Training");
        stop.setEnabled(state.active() && state.phase() != TrainingController.Phase.STOPPING && state.phase() != TrainingController.Phase.CLOSING);
        progress.setText(TrainingProgress.format(state));
        progress.setCaretPosition(0);
        String rendered = TrainingProgress.validation(state, System.nanoTime());
        validationText.setText(rendered);
        boolean visible = !rendered.isEmpty();
        if (validationBlock.isVisible() != visible) {
            validationBlock.setVisible(visible);
            outputs.setDividerSize(visible ? 8 : 0);
            revalidate();
            if (visible) outputs.setDividerLocation(0.4);
        }
        if (state.phase() == TrainingController.Phase.CONFIRM_DEPTH && !confirming) {
            confirming = true;
            // Defer the modal dialog until the controller's state publication has returned.
            SwingUtilities.invokeLater(() -> {
                try {
                    if (controller.state().phase() == TrainingController.Phase.CONFIRM_DEPTH) {
                        controller.confirmDepth(JOptionPane.showConfirmDialog(this, state.message(), "Confirm training depth change",
                                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION);
                    }
                } finally { confirming = false; }
            });
        }
    }

    private void advanced() {
        JSpinner min = spinner(settings.openingMin(), 0, 100_000), max = spinner(settings.openingMax(), 0, 100_000);
        JSpinner samples = spinner(settings.samples(), 1, 100_000), batch = spinner(settings.minibatch(), 1, 100_000);
        JSpinner epochs = spinner(settings.epochs(), 1, 100_000), plies = spinner(settings.maximumPlies(), 1, 100_000);
        JSpinner generations = new JSpinner(new SpinnerNumberModel(settings.maximumGenerations(), 0L, Long.MAX_VALUE, 1L));
        JTextField seed = new JTextField(Long.toString(settings.seed()));
        JPanel form = new JPanel(new GridBagLayout());
        row(form, 0, "Opening minimum plies", min); row(form, 1, "Opening maximum plies", max);
        row(form, 2, "Maximum samples / game", samples); row(form, 3, "Minibatch size", batch);
        row(form, 4, "Training epochs", epochs); row(form, 5, "Maximum game plies", plies);
        row(form, 6, "Generations (0 = unlimited)", generations); row(form, 7, "Model / run seed", seed);
        row(form, 8, "", new JLabel("Adam / promotion defaults; V1 NNUE units (uncalibrated)."));
        JPanel content = new JPanel(new BorderLayout(4, 8));
        content.add(form, BorderLayout.NORTH);
        content.add(validationInformation(), BorderLayout.CENTER);
        if (JOptionPane.showConfirmDialog(this, content, "Advanced training settings", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            for (JSpinner spinner : List.of(min, max, samples, batch, epochs, plies, generations)) spinner.commitEdit();
            TrainingSettings edited = new TrainingSettings(Path.of(root.getText()), value(depth), value(threads), value(games),
                    value(min), value(max), value(samples), value(batch), value(epochs), value(pairs), Long.parseLong(seed.getText().trim()),
                    value(plies), ((Number) generations.getValue()).longValue());
            controller.setSettings(edited); settings = edited;
        } catch (Exception invalid) {
            JOptionPane.showMessageDialog(this, "Check advanced settings: " + TrainingController.concise(invalid), "Invalid training settings", JOptionPane.ERROR_MESSAGE);
        }
    }

    static JScrollPane validationInformation() {
        JTextArea explanation = new JTextArea("""
                Search
                Fixed-depth iterative deepening with alpha-beta/PVS and quiescence; root parallelism uses the configured threads. No node/time limit.
                Incremental NNUE uses V1 units (uncalibrated), scale %s. Full-window search, no aspiration; selective policy is mate-distance only.
                Quiescence soft limit: %d plies (checks continue); absolute search ply limit: %d.
                Each network has a private TT per game, reused across moves; its root workers share that TT.

                Games and scoring
                Each pair uses the same randomized opening with reversed colours. Opening length is sampled within the configured range, using uniformly selected legal moves and seeded random streams.
                The game cap and current-game plies count moves after the opening. Only pairs with two chess-complete games count toward Candidate/Best wins, draws and score. Capped, cancelled or failed pairs are incomplete and excluded from scoring.
                Games/Overall track configured game slots; cancelled slots can include unplayed games. Capped counts games, not pairs.

                Promotion
                Pair score is the mean of its two game scores (win = 1, draw = 0.5, loss = 0). Candidate score is the mean over valid pairs.
                Hoeffding lower = mean - sqrt(-ln(alpha) / (2*n)), where n is valid pairs. Promotion requires the configured minimum valid pairs and lower > 0.5 + required margin; equality keeps Best.
                The GUI defaults require %d valid pairs, alpha %s and required margin %s. Assessment follows all configured game slots; there is no early threshold stopping.
                Too few valid pairs is inconclusive and keeps Best. Search/infrastructure failure blocks promotion regardless of score. A completed game run can briefly show assessment pending before its decision is available.
                """.formatted(TrainingSettings.SCORE_MAPPING.scale(), QuiescenceSearch.SOFT_QPLY_LIMIT, QuiescenceSearch.MAX_ABSOLUTE_PLY,
                        PromotionPolicy.DEFAULT.minimumPairs(), PromotionPolicy.DEFAULT.alpha(), PromotionPolicy.DEFAULT.requiredMargin()), 14, 58);
        explanation.setName("validationInformation");
        explanation.setEditable(false);
        explanation.setLineWrap(true); explanation.setWrapStyleWord(true);
        explanation.setFont(new Font(Font.MONOSPACED, Font.PLAIN, explanation.getFont().getSize()));
        explanation.setMargin(new Insets(6, 6, 6, 6));
        explanation.setCaretPosition(0);
        JScrollPane section = new JScrollPane(explanation);
        section.setBorder(BorderFactory.createTitledBorder("Validation"));
        return section;
    }

    private static JSpinner spinner(int value, int min, int max) { return new JSpinner(new SpinnerNumberModel(value, min, max, 1)); }
    private static int value(JSpinner spinner) { return ((Number) spinner.getValue()).intValue(); }
    private static void row(JPanel panel, int row, String title, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row; c.gridx = 0; c.anchor = GridBagConstraints.WEST; c.insets = new Insets(3, 4, 3, 4);
        panel.add(new JLabel(title), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, c);
    }
}
