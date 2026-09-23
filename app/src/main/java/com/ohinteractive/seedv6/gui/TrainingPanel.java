package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import com.ohinteractive.seedv6.search.alphabeta.RootParallelSearch;
import com.ohinteractive.seedv6.training.validation.PromotionPolicy;
import static com.ohinteractive.seedv6.gui.TrainingDashboard.*;

/** EDT-only workspace over the existing controller and immutable trainer publications. */
final class TrainingPanel extends JPanel {
    private final JTextField root = new JTextField(20), seed = new JTextField();
    private final JSpinner depth = spinner(4, 1, 256), threads = spinner(1, 1, RootParallelSearch.MAX_WORKERS);
    private final JSpinner games = spinner(64, 1, 100_000), pairs = spinner(64, 1, 100_000);
    private final JSpinner min, max, samples, plies, generations;
    private final JComboBox<NetworkArchitecture> architecture = new JComboBox<>(NetworkArchitecture.values());
    private final JPanel architectureCards = panel(new CardLayout());
    private final NnueConfigurationPanel nnue;
    private final BrnConfigurationPanel brn;
    private final Brn1ConfigurationPanel brn1;
    private final Brn2ConfigurationPanel brn2;
    private final BrnTrainingSourcePanel trainingSource;
    private final JLabel checkpointLabel = label("Checkpoint folder", 12, SeedTheme.SECONDARY);
    private final JButton browse = new JButton("Browse…"), apply = new JButton("Apply settings");
    private final JButton start = new JButton("Start / Resume Training"), stop = new JButton("Stop Training");
    private final JTextArea progress = new JTextArea(17, 32), validation = new JTextArea(12, 32);
    private final JScrollPane trainingBlock = new JScrollPane(progress), validationBlock = new JScrollPane(validation);
    private final ScrollPreservingText trainingText = new ScrollPreservingText(progress, trainingBlock);
    private final ScrollPreservingText validationText = new ScrollPreservingText(validation, validationBlock);
    private final JSplitPane outputs = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    private final List<JComponent> editors = new ArrayList<>();
    private final JTabbedPane tabs = new JTabbedPane();
    private final TrainingDashboard dashboard = new TrainingDashboard();
    private final JScrollPane dashboardScroll;
    private final JLabel status = label("IDLE", 11, SeedTheme.SECONDARY);
    private final JPanel legacySourceSlot = panel(new BorderLayout());
    private TrainingController controller;
    private boolean confirming, applying, wasActive;
    private final TrainingFolders folders;
    private NetworkArchitecture displayedArchitecture;

    TrainingPanel(TrainingSettings settings) {
        this(settings, new TrainingFolders(settings));
    }

    TrainingPanel(TrainingSettings settings, TrainingFolders folders) {
        super(new BorderLayout(0, SeedTheme.scale(10))); setOpaque(false);
        this.folders = folders;
        displayedArchitecture = settings.architecture();
        root.setName("trainingRoot"); depth.setName("trainingDepth"); threads.setName("trainingThreads");
        games.setName("trainingGames"); pairs.setName("trainingPairs"); progress.setName("trainingProgress");
        start.setName("startTraining"); stop.setName("stopTraining"); apply.setName("applyTrainingSettings");
        root.setText(folders.root(displayedArchitecture)); root.setToolTipText(root.getText());
        depth.setValue(settings.depth()); threads.setValue(settings.threads()); games.setValue(settings.games()); pairs.setValue(settings.validationPairs());
        min = spinner(settings.openingMin(), 0, 100_000); max = spinner(settings.openingMax(), 0, 100_000);
        samples = spinner(settings.samples(), 1, 100_000); plies = spinner(settings.maximumPlies(), 1, 100_000);
        generations = new JSpinner(new SpinnerNumberModel(settings.maximumGenerations(), 0L, Long.MAX_VALUE, 1L));
        seed.setText(Long.toString(settings.seed()));
        seed.setName("trainingSeed"); samples.setName("trainingSamples");
        seed.setToolTipText("One seed for deterministic run streams and fresh NNUE initialization. Resume restores the stored model and optimizer.");
        architecture.setName("networkArchitecture"); architecture.setSelectedItem(settings.architecture());
        architectureCards.setName("architectureConfiguration");
        nnue = new NnueConfigurationPanel(settings); architectureCards.add(nnue, NetworkArchitecture.NNUE.name());
        brn = new BrnConfigurationPanel(settings); architectureCards.add(brn, NetworkArchitecture.BRN.name());
        brn1 = new Brn1ConfigurationPanel(settings); architectureCards.add(brn1, NetworkArchitecture.BRN1.name());
        brn2 = new Brn2ConfigurationPanel(settings); architectureCards.add(brn2, NetworkArchitecture.BRN2.name());
        trainingSource = new BrnTrainingSourcePanel(settings, this::sourceChanged);
        brn2.onChange(() -> {
            if (brn2.storedRunSeeds() != null) seed.setText(Long.toString(brn2.storedRunSeeds().masterSeed()));
            sourceChanged();
        });
        architecture.addActionListener(event -> {
            folders.remember(displayedArchitecture, root.getText());
            displayedArchitecture = selectedArchitecture();
            placeSource();
            root.setText(folders.root(displayedArchitecture)); root.setToolTipText(root.getText());
            folders.select(displayedArchitecture);
            ((CardLayout) architectureCards.getLayout()).show(architectureCards, displayedArchitecture.name());
            trainingSource.selectRoot(root.getText(), displayedArchitecture); brn2.selectRoot(root.getText(), displayedArchitecture);
            checkpointLabel.setText(displayedArchitecture == NetworkArchitecture.NNUE ? "NNUE checkpoint store" : "BRN checkpoint store (student)");
        });
        ((CardLayout) architectureCards.getLayout()).show(architectureCards, settings.architecture().name());
        JPanel selection = padded(new BorderLayout(SeedTheme.scale(12), 0), 10);
        JLabel architectureLabel = label("Network Architecture", 12, SeedTheme.SECONDARY);
        architectureLabel.setLabelFor(architecture); selection.add(architectureLabel, BorderLayout.WEST); selection.add(architecture);
        add(selection, BorderLayout.NORTH);
        editors.addAll(List.of(root, browse, depth, threads, games, pairs, min, max, samples, plies, generations, seed, architecture, apply));
        tabs.setName("trainingViews"); tabs.putClientProperty("JTabbedPane.tabAreaAlignment", "leading");
        dashboardScroll = scroll(dashboard); dashboardScroll.setName("trainingDashboardScroll");
        tabs.addTab("Dashboard", dashboardScroll); tabs.addTab("History", dashboard.historyView());
        tabs.addTab("Configuration", configuration()); tabs.addTab("Diagnostics", diagnostics());
        add(tabs);
        JPanel actions = panel(new BorderLayout(SeedTheme.scale(8), 0)); actions.add(status);
        JPanel buttons = panel(new FlowLayout(FlowLayout.RIGHT, SeedTheme.scale(8), 0)); buttons.add(start); buttons.add(stop); actions.add(buttons, BorderLayout.EAST);
        start.putClientProperty("FlatLaf.style", "background: #218f59; foreground: #ffffff; hoverBackground: #29a568; pressedBackground: #187547");
        add(actions, BorderLayout.SOUTH); stop.setEnabled(false);
        root.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { changed(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { changed(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { changed(); }
            private void changed() { trainingSource.selectRoot(root.getText(), displayedArchitecture); brn2.selectRoot(root.getText(), displayedArchitecture); }
        });
        trainingSource.selectRoot(root.getText(), displayedArchitecture); brn2.selectRoot(root.getText(), displayedArchitecture);
        checkpointLabel.setText(displayedArchitecture == NetworkArchitecture.NNUE ? "NNUE checkpoint store" : "BRN checkpoint store (student)");
        browse.addActionListener(event -> {
            JFileChooser chooser = new JFileChooser(root.getText()); chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                root.setText(chooser.getSelectedFile().toPath().toString());
                folders.remember(displayedArchitecture, root.getText());
            }
        });
        apply.addActionListener(event -> applySettings());
        start.addActionListener(event -> { if (applySettings()) controller.start(); });
        stop.addActionListener(event -> controller.stop());
    }

    void showDiagnostics() { tabs.setSelectedIndex(3); }
    // First activation only: tab focus/layout may otherwise reveal a lower child on a short viewport.
    // Later polls and workspace switches preserve the user's chosen position.
    void showDashboardTop() { SwingUtilities.invokeLater(() -> dashboardScroll.getViewport().setViewPosition(new Point())); }
    void bind(TrainingController controller) { this.controller = controller; showState(controller.state()); }

    boolean applySettings() {
        if (controller == null || applying) return false;
        applying = true;
        try {
            for (JSpinner spinner : List.of(depth, threads, games, pairs, min, max, samples, plies, generations)) spinner.commitEdit();
            if (root.getText().isBlank()) throw new IllegalArgumentException("Select a checkpoint folder.");
            var previous = controller.state().settings();
            var options = selectedArchitecture() == NetworkArchitecture.NNUE ? nnue.read()
                    : new NnueConfigurationPanel.Values(previous.minibatch(), previous.epochs());
            double rate = selectedArchitecture() == NetworkArchitecture.BRN ? brn.read() : previous.brnLearningRate();
            double rate1 = selectedArchitecture() == NetworkArchitecture.BRN1 ? brn1.read() : previous.brn1LearningRate();
            double rate2 = selectedArchitecture() == NetworkArchitecture.BRN2 ? brn2.read() : previous.brn2LearningRate();
            TrainingSettings edited = new TrainingSettings(Path.of(root.getText()), value(depth), value(threads), value(games),
                    value(min), value(max), value(samples), options.minibatch(), options.epochs(), value(pairs), Long.parseLong(seed.getText().trim()),
                    value(plies), ((Number) generations.getValue()).longValue(), selectedArchitecture(), rate, rate1, rate2,
                    selectedArchitecture() == NetworkArchitecture.NNUE ? null : trainingSource.read(), trainingSource.generatorStore(),
                    selectedArchitecture() == NetworkArchitecture.BRN2 ? brn2.readSupervision() : null)
                    .withTeacherStore(selectedArchitecture() == NetworkArchitecture.BRN2 ? brn2.readTeacherStore() : null)
                    .withRunSeeds(selectedArchitecture() == NetworkArchitecture.BRN2 ? brn2.readRunSeeds(Long.parseLong(seed.getText().trim())) : null);
            controller.setSettings(edited); root.setToolTipText(edited.root().toString());
            folders.remember(displayedArchitecture, root.getText()); folders.select(displayedArchitecture);
            return true;
        } catch (Exception invalid) {
            tabs.setSelectedIndex(2);
            JOptionPane.showMessageDialog(this, "Check training settings: " + TrainingController.concise(invalid), "Invalid training settings", JOptionPane.ERROR_MESSAGE);
            return false;
        } finally { applying = false; }
    }

    void showState(TrainingController.ViewState state) {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Training view requires EDT.");
        boolean stopped = wasActive && !state.active(); wasActive = state.active();
        if (stopped) {
            brn2.selectRoot(root.getText(), displayedArchitecture);
            if (displayedArchitecture == NetworkArchitecture.BRN2) trainingSource.selectRoot(root.getText(), displayedArchitecture);
        }
        boolean editable = !state.active() && state.phase() != TrainingController.Phase.CLOSING;
        editors.forEach(component -> component.setEnabled(editable));
        nnue.setEditable(editable); brn.setEditable(editable); brn1.setEditable(editable); brn2.setEditable(editable);
        trainingSource.setEditable(editable);
        seed.setEnabled(editable && (displayedArchitecture != NetworkArchitecture.BRN2 || (brn2.ready() && brn2.storedRunSeeds() == null)));
        start.setEnabled(state.canStart() && trainingSource.ready() && brn2.ready()); start.setText(state.resume() ? "Resume Training" : "Start / Resume Training");
        pairs.setEnabled(editable && !trainingSource.bootstrap());
        stop.setEnabled(state.active() && state.phase() != TrainingController.Phase.STOPPING && state.phase() != TrainingController.Phase.CLOSING);
        dashboard.showState(state);
        status.setText(TrainingDashboardModel.phase(state) + (state.message().contains("Restarted unfinished generation")
                ? " - unfinished generation restarted from settled checkpoint (see Diagnostics)" : ""));
        status.setToolTipText(state.message());
        status.setForeground(state.phase() == TrainingController.Phase.FAILED ? SeedTheme.ERROR : SeedTheme.SECONDARY);
        // Both bounded documents retain all previous diagnostics. No full-log reconstruction or caret jump.
        trainingText.setText(TrainingProgress.format(state) + "\n\nHistory: " + state.settings().root().resolve(com.ohinteractive.seedv6.training.history.HistoryRepository.FILE)
                + "\n" + String.join("\n", state.history().warnings()) + "\n" + state.historyWarning());
        progress.setForeground(state.phase() == TrainingController.Phase.FAILED ? SeedTheme.ERROR : SeedTheme.TEXT);
        String rendered = TrainingProgress.validation(state, System.nanoTime());
        validationText.setText(rendered);
        validation.setForeground(rendered.startsWith("Validation failed") ? SeedTheme.ERROR
                : rendered.contains("INCONCLUSIVE") || rendered.contains("Warning:") ? SeedTheme.WARNING : SeedTheme.TEXT);
        if (state.phase() == TrainingController.Phase.CONFIRM_DEPTH && !confirming) {
            confirming = true;
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

    private void placeSource() {
        (displayedArchitecture == NetworkArchitecture.BRN2 ? brn2.sourceSlot : legacySourceSlot).add(trainingSource);
        revalidate();
    }
    private JScrollPane configuration() {
        JPanel content = new ConfigurationCards();
        JPanel store = padded(new BorderLayout(SeedTheme.scale(8), SeedTheme.scale(8)), 14);
        checkpointLabel.setLabelFor(root); store.add(checkpointLabel, BorderLayout.NORTH); store.add(root); store.add(browse, BorderLayout.EAST);
        store.add(legacySourceSlot, BorderLayout.SOUTH); placeSource();
        addCard(content, card("Checkpoint store", null, store), 0);
        JPanel regime = padded(new GridLayout(1, 2, SeedTheme.scale(20), 0), 14);
        JPanel left = panel(new GridBagLayout()), right = panel(new GridBagLayout());
        row(left, 0, "Training depth", depth); row(left, 1, "Search threads", threads);
        row(right, 0, "Games / generation", games); row(right, 1, "Validation pairs", pairs);
        regime.add(left); regime.add(right); addCard(content, card("Training regime · independent settings", null, regime), 1);
        JPanel advanced = padded(new GridLayout(1, 2, SeedTheme.scale(20), 0), 14);
        left = panel(new GridBagLayout()); right = panel(new GridBagLayout());
        row(left, 0, "Opening min. plies", min); row(left, 1, "Opening max. plies", max);
        row(left, 2, "Samples / game", samples);
        row(right, 0, "Maximum game plies", plies);
        row(right, 1, "Generations (0 = unlimited)", generations); row(right, 2, "Model / run seed", seed);
        advanced.add(left); advanced.add(right); addCard(content, card("Training bounds", null, advanced), 2);
        addCard(content, architectureCards, 3);
        JPanel commit = padded(new BorderLayout(SeedTheme.scale(10), 0), 12);
        JTextArea help = text("Stop before editing. Resume with unchanged settings continues exactly. Changed generation settings restart unfinished work from the last settled checkpoint. Best changes only through the existing promotion rules.", 12, SeedTheme.SECONDARY);
        help.setRows(3); commit.add(help); commit.add(apply, BorderLayout.EAST); addCard(content, commit, 4);
        JScrollPane information = validationInformation(); information.setPreferredSize(new Dimension(1, SeedTheme.scale(350)));
        addCard(content, information, 5);
        GridBagConstraints filler = new GridBagConstraints(); filler.gridy = 6; filler.weighty = 1; content.add(Box.createVerticalGlue(), filler);
        JScrollPane scroll = scroll(content); scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); return scroll;
    }

    private JPanel diagnostics() {
        for (JTextArea area : List.of(progress, validation)) {
            area.setEditable(false); area.setLineWrap(true); area.setWrapStyleWord(true);
            area.setMargin(new Insets(SeedTheme.scale(8), SeedTheme.scale(10), SeedTheme.scale(8), SeedTheme.scale(10)));
            area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, SeedTheme.scale(12))); area.setBackground(SeedTheme.INSET);
        }
        validation.setName("validationProgress"); validation.setToolTipText("Scores count valid pairs only. Search and promotion rules are in Configuration.");
        trainingBlock.setBorder(BorderFactory.createTitledBorder("Trainer snapshot")); validationBlock.setBorder(BorderFactory.createTitledBorder("Candidate validation"));
        trainingBlock.setMinimumSize(new Dimension(100, 80)); validationBlock.setMinimumSize(new Dimension(100, 100));
        outputs.setName("trainingOutputs"); outputs.setTopComponent(trainingBlock); outputs.setBottomComponent(validationBlock);
        outputs.setBorder(BorderFactory.createEmptyBorder()); outputs.setContinuousLayout(true); outputs.setResizeWeight(.4); outputs.setDividerSize(SeedTheme.scale(8));
        outputs.setDividerLocation(SeedTheme.scale(300));
        JPanel body = panel(new BorderLayout(0, SeedTheme.scale(8)));
        JTextArea note = text("Live diagnostic snapshots · refreshed every 500 ms. Scroll position is preserved; the bottom follows updates when already selected. No active-game telemetry is shown after its game ends.", 12, SeedTheme.SECONDARY);
        note.setRows(2); body.add(note, BorderLayout.NORTH); body.add(outputs); return body;
    }

    static JScrollPane validationInformation() {
        JTextArea explanation = new JTextArea("""
                BRN position generation and supervision
                BRN-2 defaults to Handcrafted position generation and WDL targets. NNUE generation and NNUE blended supervision independently pin accepted NNUE Best checkpoints per generation. Handcrafted scores never enter targets. BRN-2 can select NNUE blended supervision in its architecture configuration; mode and weight are fixed for the lineage. A seeded whole-game split holds out about 20%% of completed sampled games (at least two; at least two training games). Candidate and Best use the same held-out positions. Strictly lower mean half-squared error promotes; ties keep Best. This measures prediction loss, not game strength. Validation pairs apply only to ordinary self-play. BRN-2 source and supervision are fixed for the lineage. While stopped, changing other generation settings restarts unfinished work from the last settled checkpoint; unchanged settings preserve exact Resume.

                Games and scoring
                Each pair uses the same randomized opening with reversed colours. Opening length is sampled within the configured range, using uniformly selected legal moves and seeded random streams.
                The game cap and current-game plies count moves after the opening. Only pairs with two chess-complete games count toward Candidate/Best wins, draws and score. Capped, cancelled or failed pairs are incomplete and excluded from scoring.
                Games/Overall track configured game slots; cancelled slots can include unplayed games. Capped counts games, not pairs.

                Promotion
                Pair score is the mean of its two game scores (win = 1, draw = 0.5, loss = 0). Candidate score is the mean over valid pairs.
                Hoeffding lower = mean - sqrt(-ln(alpha) / (2*n)), where n is valid pairs. Promotion requires the configured minimum valid pairs and lower > 0.5 + required margin; equality keeps Best.
                The GUI requires all configured Validation pairs to be valid (default %d), with alpha %s and required margin %s. Assessment follows all configured game slots; there is no early threshold stopping.
                Too few valid pairs is inconclusive and keeps Best. Search/infrastructure failure blocks promotion regardless of score. A completed game run can briefly show assessment pending before its decision is available.
                """.formatted(TrainingSettings.defaults().validationPairs(), PromotionPolicy.DEFAULT.alpha(), PromotionPolicy.DEFAULT.requiredMargin()), 14, 58);
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
    private NetworkArchitecture selectedArchitecture() { return (NetworkArchitecture) architecture.getSelectedItem(); }
    private void sourceChanged() {
        boolean editable = controller == null || (!controller.state().active() && controller.state().phase() != TrainingController.Phase.CLOSING);
        seed.setEnabled(editable && (displayedArchitecture != NetworkArchitecture.BRN2 || (brn2.ready() && brn2.storedRunSeeds() == null)));
        pairs.setEnabled(editable && !trainingSource.bootstrap());
        start.setEnabled(trainingSource.ready() && brn2.ready() && (controller == null || controller.state().canStart()));
    }

    static void row(JPanel panel, int row, String title, JComponent field) {
        GridBagConstraints c = new GridBagConstraints(); c.gridy = row; c.gridx = 0; c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(SeedTheme.scale(5), 0, SeedTheme.scale(5), SeedTheme.scale(10));
        JLabel label = label(title, 12, SeedTheme.SECONDARY); label.setLabelFor(field); panel.add(label, c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(SeedTheme.scale(5), 0, SeedTheme.scale(5), 0);
        field.setMinimumSize(new Dimension(SeedTheme.scale(75), SeedTheme.scale(30))); panel.add(field, c);
    }
    private static void addCard(JPanel parent, JComponent card, int row) {
        card.setMinimumSize(new Dimension(0, card.getPreferredSize().height));
        GridBagConstraints c = new GridBagConstraints(); c.gridx = 0; c.gridy = row; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, SeedTheme.scale(10), 0); parent.add(card, c);
    }
    private static final class ConfigurationCards extends JPanel implements Scrollable {
        ConfigurationCards() { super(new GridBagLayout()); setOpaque(false); }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return SeedTheme.scale(24); }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height - SeedTheme.scale(24); }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
        @Override public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize(); size.height = Math.max(size.height, getMinimumSize().height); return size;
        }
    }
}
