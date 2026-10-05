package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.training.service.*;
import com.ohinteractive.seedv6.training.model.TrainingArchitecture;
import com.ohinteractive.seedv6.training.data.DataSource;
import com.ohinteractive.seedv6.search.exact.*;
import java.awt.*;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import javax.swing.*;
import static com.ohinteractive.seedv6.training.service.LearningArenaConfig.*;

/** Compact native workspace: campaign configuration, live state and longitudinal round receipts. */
final class LearningArenaPanel extends JPanel {
    private final TrainingFolders folders;
    private final LearningArenaController controller = new LearningArenaController(this::showState);
    private final CompetitorFields a = new CompetitorFields("A", TrainingArchitecture.NNUE_MATERIAL);
    private final CompetitorFields b = new CompetitorFields("B", TrainingArchitecture.BRN3);
    private final JTextField name = field("arenaName", "Learning campaign");
    private final TrainingDataSelector source;
    private final JTextField fen = field("arenaFen", TrainerConfig.STANDARD_START);
    private final JSpinner positions = number("arenaPositions", 131072, 1, 10000000);
    private final JSpinner epochs = number("arenaEpochs", 8, 1, 100000);
    private final JSpinner rounds = number("arenaRounds", 10, 0, 1000000);
    private final JTextField seed = field("arenaSeed", "71");
    private final JSpinner games = number("arenaGames", 8, 2, 100000);
    private final JComboBox<Limit> limit = new JComboBox<>(Limit.values());
    private final JSpinner depth = number("arenaDepth", 4, 1, ExactSearch.MAX_DEPTH);
    private final JSpinner millis = number("arenaMillis", 1000, 1, 3600000);
    private final JSpinner threads = ThreadSelection.spinner(1);
    private final JSpinner openingMin = number("arenaOpeningMin", 0, 0, 1000), openingMax = number("arenaOpeningMax", 8, 0, 1000);
    private final JSpinner cap = number("arenaPlyCap", 1024, 1, 100000);
    private final JButton start = button("arenaStart", "Start new"), resume = button("arenaResume", "Resume");
    private final JButton open = button("arenaOpen", "Open campaign..."), pause = button("arenaPause", "Pause safely");
    private final JLabel status = new JLabel("Ready"), storage = new JLabel();
    private final ArenaHistoryView history = new ArenaHistoryView();
    private final JPanel setup = new SetupPanel();
    private final JScrollPane setupScroll = new JScrollPane(setup);
    private final JTabbedPane views = new JTabbedPane();
    private final MatchView match = new MatchView("arenaMatch");
    private final TrainingProgressView optimization = new TrainingProgressView("arenaOptimization");
    private final JTextArea activity = new JTextArea(4, 30);
    private String loadedBinding = "";
    private final java.util.Set<Path> registeredLineages = new java.util.HashSet<>();
    LearningArenaPanel(TrainingFolders folders) {
        super(new BorderLayout(0, SeedTheme.scale(12))); this.folders = folders;
        source = new TrainingDataSelector("arenaSource", folders::base,
                () -> java.util.List.of((TrainingArchitecture) a.architecture.getSelectedItem(), (TrainingArchitecture) b.architecture.getSelectedItem()), this::sourceChanged);
        a.architecture.addActionListener(e -> source.compatibilityChanged()); b.architecture.addActionListener(e -> source.compatibilityChanged());
        threads.setName("arenaThreads");
        setName("learningArena"); setBackground(SeedTheme.BACKGROUND); SeedTheme.padding(this, 16, 20, 16, 20);
        var heading = new JPanel(new BorderLayout()); heading.setOpaque(false);
        heading.add(SeedTheme.label("Arena - learning campaign", 22, SeedTheme.TEXT), BorderLayout.WEST);
        var actions = new JPanel(new FlowLayout(FlowLayout.RIGHT)); actions.setOpaque(false);
        for (var button : new JButton[]{open, start, resume, pause}) actions.add(button);
        heading.add(actions, BorderLayout.EAST); add(heading, BorderLayout.NORTH);
        setup.setOpaque(false);
        var shared = fields(); row(shared, 0, "Campaign name", name); row(shared, 1, "Shared source", source);
        source.setMinimumSize(new Dimension(100, source.getPreferredSize().height));
        row(shared, 3, "Positions / round", positions); row(shared, 4, "Epochs / tranche (both)", epochs);
        row(shared, 5, "Training rounds (0 = until paused)", rounds); row(shared, 6, "Opening / shuffle seed", seed);
        var arena = fields(); limit.setName("arenaLimit"); row(arena, 0, "Games (even; colours reversed)", games);
        row(arena, 1, "Search limit", limit); row(arena, 2, "Depth (plies)", depth); row(arena, 3, "Time / move (ms)", millis);
        row(arena, 4, "Search threads", threads); row(arena, 5, "Opening plies minimum", openingMin);
        row(arena, 6, "Opening plies maximum", openingMax); row(arena, 7, "Game ply cap", cap);
        var c = new GridBagConstraints(); c.insets = new Insets(6, 6, 6, 6); c.fill = GridBagConstraints.BOTH; c.weightx = .5;
        c.gridx = 0; c.gridy = 0; setup.add(SeedTheme.card("Model lineage and recipe - first", null, a), c);
        c.gridx = 1; setup.add(SeedTheme.card("Model lineage and recipe - second", null, b), c);
        c.gridx = 0; c.gridy = 1; setup.add(SeedTheme.card("Shared campaign protocol and exposure", null, shared), c);
        c.gridx = 1; setup.add(SeedTheme.card("Shared match protocol", null, arena), c);
        var opening = fields(); row(opening, 0, "Starting FEN", fen);
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2; setup.add(opening, c);
        var explanation = new JTextArea("Round 0 compares the initial snapshots before campaign training. Both consume the same frozen records and targets, including the same epoch count.\nDepth is a controlled comparison; time includes evaluator cost. One search thread is most reproducible. Ply-capped pairs are unscored.\nResume restores the saved configuration. A crash may recompute work since the last optimizer save (about 30 seconds) or an unfinished game.");
        explanation.setEditable(false); explanation.setLineWrap(true); explanation.setWrapStyleWord(true); explanation.setOpaque(false); explanation.setRows(4);
        c.gridy = 3; setup.add(explanation, c);
        setupScroll.setName("arenaSetupScroll"); setupScroll.setBorder(null); setupScroll.getVerticalScrollBar().setUnitIncrement(20);
        views.setName("arenaViews"); views.addTab("Setup", setupScroll);
        var liveView = new JPanel(new BorderLayout(14, 8)); liveView.setOpaque(false); liveView.add(match);
        var liveDetails = new JPanel(new BorderLayout(0, 14)); liveDetails.setOpaque(false);
        liveDetails.setPreferredSize(new Dimension(SeedTheme.scale(350), 1));
        liveDetails.add(SeedTheme.card("Optimization", null, optimization), BorderLayout.NORTH);
        activity.setName("arenaActivity"); activity.setEditable(false); activity.setLineWrap(true); activity.setWrapStyleWord(true); activity.setOpaque(false);
        liveDetails.add(activity); liveView.add(liveDetails, BorderLayout.EAST); views.addTab("Live", liveView);
        views.addTab("History", history); add(views);
        var footer = new JPanel(new GridLayout(2, 1)); footer.setOpaque(false);
        status.setName("arenaStatus"); storage.setName("arenaStorage"); footer.add(status); footer.add(storage); add(footer, BorderLayout.SOUTH);
        limit.addActionListener(e -> limits());
        start.addActionListener(e -> start()); resume.addActionListener(e -> controller.resume()); pause.addActionListener(e -> controller.pause());
        open.addActionListener(e -> FilePickers.choose(this, FilePickers.Purpose.LEARNING_ARENA_RESUME, "Open Learning Arena campaign",
                controller.root() == null ? folders.base().resolve("learning-arena").toString() : controller.root().toString()).ifPresent(this::openCampaign));
        showState(controller);
    }
    private void start() {
        try {
            commit(setup);
            if (!source.ready()) throw new IllegalArgumentException("Select ready Training Data compatible with both competitors");
            var draft = new Draft(name.getText().trim(), a.read(), b.read(), source.selected(), value(positions), value(epochs), value(rounds), Long.parseLong(seed.getText().trim()),
                    new Arena(value(games), (Limit) limit.getSelectedItem(), value(depth), value(millis), value(threads), value(openingMin), value(openingMax), value(cap), fen.getText().trim()));
            String folder = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + "-" + UUID.randomUUID().toString().substring(0, 8);
            controller.startDraft(folders.base().resolve("learning-arena").resolve(folder), draft::resolve);
            views.setSelectedIndex(1);
        } catch (Exception error) { JOptionPane.showMessageDialog(this, error.getMessage(), "Learning Arena settings", JOptionPane.ERROR_MESSAGE); }
    }
    private record Draft(String name, Competitor a, Competitor b, DataSource source, int positions, int epochs, int rounds, long seed, Arena arena) {
        LearningArenaConfig resolve() throws Exception { source.requireReady(); return new LearningArenaConfig(name, a, b, source, positions, epochs, rounds, seed, arena); }
    }
    private void showState(LearningArenaController c) {
        enable(setup, !c.busy()); a.availability(!c.busy()); b.availability(!c.busy()); limits(); source.setEditable(!c.busy()); start.setEnabled(!c.busy() && source.ready()); open.setEnabled(!c.busy()); pause.setEnabled(c.busy());
        resume.setEnabled(!c.busy() && c.root() != null && c.update() != null && c.update().state().status() != LearningArenaState.Status.COMPLETE);
        storage.setText(c.root() == null ? "New campaigns: " + folders.base().resolve("learning-arena") : "Campaign: " + c.root());
        var u = c.update(); match.showGame(u == null ? null : u.liveGame(), u == null ? null : u.search());
        optimization.showProgress(u == null ? null : u.optimization());
        if (u == null) { status.setText(c.error().isBlank() ? c.busy() ? "Opening campaign..." : "Ready" : c.error()); return; }
        var state = u.state();
        activity.setText(state.config().name() + "\nRound " + state.current().number() + " - " + state.status() + "\n" + u.detail()
                + (u.optimization() != null && state.current().stage() != LearningArenaState.Stage.TRAIN_A && state.current().stage() != LearningArenaState.Stage.TRAIN_B ? "\nShowing the last training segment." : ""));
        if (c.root() != null) {
            if (state.history().getFirst().a() != null) registerLineage(c.root().resolve("A"), state.config().a().architecture());
            if (state.history().getFirst().b() != null) registerLineage(c.root().resolve("B"), state.config().b().architecture());
        }
        status.setText(c.error().isBlank() ? "Round " + state.current().number() + " | " + state.status() + " | " + u.detail() : c.error());
        status.setToolTipText(status.getText()); storage.setToolTipText(storage.getText());
        if (!c.busy() && !loadedBinding.equals(state.binding())) { load(state.config()); loadedBinding = state.binding(); }
        history.showState(state, c.root());
    }
    private void load(LearningArenaConfig c) {
        name.setText(c.name()); source.select(c.source()); a.load(c.a()); b.load(c.b()); positions.setValue(c.positionsPerRound()); epochs.setValue(c.epochs());
        rounds.setValue(c.rounds()); seed.setText(Long.toString(c.seed())); var v = c.arena(); games.setValue(v.games()); limit.setSelectedItem(v.limit()); depth.setValue(v.depth());
        millis.setValue((int) v.millis()); ThreadSelection.setChoice(threads, v.threads()); openingMin.setValue(v.openingMin()); openingMax.setValue(v.openingMax()); cap.setValue(v.maximumPlies()); fen.setText(v.startingFen());
    }
    private void registerLineage(Path root, TrainingArchitecture architecture) {
        if (registeredLineages.add(root)) folders.register(folders.base(), NetworkArchitecture.valueOf(architecture.name()), root);
    }
    private void sourceChanged() { if (source != null) start.setEnabled(!controller.busy() && source.ready()); }
    void poll() { controller.poll(); }
    void openCampaign(Path root) { controller.open(root); views.setSelectedIndex(2); }
    void showSetupTop() { setupScroll.getViewport().setViewPosition(new Point(0, 0)); }
    Runnable beginShutdown() {
        var campaign = controller.beginShutdown(); var data = source.beginShutdown();
        return () -> { try { campaign.run(); } finally { data.run(); } };
    }
    private void limits() { depth.setEnabled(!controller.busy() && limit.getSelectedItem() == Limit.DEPTH); millis.setEnabled(!controller.busy() && limit.getSelectedItem() == Limit.TIME); }
    private static JPanel fields() { var p = new JPanel(new GridBagLayout()); p.setOpaque(false); SeedTheme.padding(p, 12, 12, 12, 12); return p; }
    private static void row(JPanel panel, int y, String title, JComponent value) { TrainingPanel.row(panel, y, title, value); }
    private static JTextField field(String name, String text) { var f = new JTextField(text, 16); f.setName(name); return f; }
    private static JButton button(String name, String title) { var b = new JButton(title); b.setName(name); return b; }
    private static JSpinner number(String name, int value, int min, int max) { var s = new JSpinner(new SpinnerNumberModel(value, min, max, 1)); s.setName(name); return s; }
    private static int value(JSpinner field) { return ((Number) field.getValue()).intValue(); }
    private static void commit(Container c) throws java.text.ParseException { for (Component item : c.getComponents()) { if (item instanceof JSpinner s) s.commitEdit(); else if (item instanceof Container nested) commit(nested); } }
    private static void enable(Container c, boolean enabled) { for (Component item : c.getComponents()) { item.setEnabled(enabled); if (item instanceof Container nested) enable(nested, enabled); } }
    private static final class SetupPanel extends JPanel implements Scrollable {
        SetupPanel() { super(new GridBagLayout()); }
        public Dimension getPreferredScrollableViewportSize() { return new Dimension(1000, 700); }
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return SeedTheme.scale(20); }
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) { return Math.max(1, visible.height - SeedTheme.scale(30)); }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }
    private final class CompetitorFields extends JPanel {
        final JTextField name, seed;
        final JComboBox<TrainingArchitecture> architecture = new JComboBox<>(new TrainingArchitecture[]{TrainingArchitecture.NNUE_MATERIAL, TrainingArchitecture.NNUE, TrainingArchitecture.BRN3});
        final JSpinner batch;
        final JLabel recipe = new JLabel();
        final JSpinner rate = new JSpinner(new SpinnerNumberModel(.001, Double.MIN_VALUE, Double.MAX_VALUE, .0001));
        final JCheckBox inheritRate = new JCheckBox("Use checkpoint / architecture rate");
        final JButton model = new JButton("Select model..."), fresh = new JButton("Fresh");
        final JLabel initialDescription = new JLabel("Fresh initialization");
        InitialModel initialModel;
        boolean loading;
        final String side;
        CompetitorFields(String side, TrainingArchitecture initial) {
            super(new GridBagLayout()); setOpaque(false); SeedTheme.padding(this, 12, 12, 12, 12);
            this.side = side;
            name = field("arenaName" + side, initial.displayName() + " experiment"); seed = field("arenaSeed" + side, "71"); batch = number("arenaBatch" + side, 128, 1, 100000);
            architecture.setRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                    return super.getListCellRendererComponent(list, value instanceof TrainingArchitecture a ? a.displayName() : value, index, selected, focus);
                }
            });
            architecture.setName("arenaArchitecture" + side); architecture.setSelectedItem(initial);
            rate.setName("arenaLearningRate" + side); rate.setEditor(new JSpinner.NumberEditor(rate, "0.############"));
            rate.setValue(com.ohinteractive.seedv6.training.model.TrainingRecipe.defaults(initial).learningRate());
            inheritRate.setName("arenaInheritRate" + side); inheritRate.setOpaque(false);
            model.setName("arenaSelectModel" + side); fresh.setName("arenaFreshModel" + side);
            initialDescription.setName("arenaInitialModel" + side); initialDescription.setPreferredSize(new Dimension(200, 28));
            var start = new JPanel(new BorderLayout(4, 4)); start.setOpaque(false); start.add(initialDescription, BorderLayout.NORTH);
            var actions = new JPanel(new GridLayout(1, 2, 6, 0)); actions.setOpaque(false); actions.add(model); actions.add(fresh); start.add(actions);
            row(this, 0, "Lineage name", name); row(this, 1, "Architecture", architecture); row(this, 2, "Starting model", start);
            start.setMinimumSize(new Dimension(100, start.getPreferredSize().height));
            row(this, 3, "Initialization seed", seed); seed.setToolTipText("Used only for fresh initialization. Existing models retain their exact weights and optimizer state.");
            row(this, 4, "Minibatch size", batch); row(this, 5, "Learning rate", rate); row(this, 6, "", inheritRate); row(this, 7, "Optimizer", recipe);
            model.addActionListener(e -> selectModel());
            fresh.addActionListener(e -> { initialModel = null; initialDescription.setText("Fresh initialization"); initialDescription.setToolTipText(null); availability(!controller.busy()); });
            inheritRate.addActionListener(e -> availability(!controller.busy()));
            architecture.addActionListener(e -> {
                if (!loading) { initialModel = null; initialDescription.setText("Fresh initialization"); rate.setValue(com.ohinteractive.seedv6.training.model.TrainingRecipe.defaults((TrainingArchitecture) architecture.getSelectedItem()).learningRate()); }
                recipe(); availability(!controller.busy());
            }); recipe();
        }
        void availability(boolean editable) { seed.setEnabled(editable && initialModel == null); rate.setEnabled(editable && !inheritRate.isSelected()); }
        void selectModel() {
            var browser = new ModelSelectionPanel("arenaInitial" + side, "Copy an exact generation into this experiment", initialModel == null ? null : Path.of(initialModel.root()), folders, null, () -> {},
                    NetworkArchitecture.valueOf(((TrainingArchitecture) architecture.getSelectedItem()).name()), false);
            browser.setPreferredSize(new Dimension(620, 325));
            try {
                while (JOptionPane.showConfirmDialog(LearningArenaPanel.this, browser, "Starting model", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
                    if (!browser.validSelection()) { JOptionPane.showMessageDialog(LearningArenaPanel.this, "Choose an available generation."); continue; }
                    initialModel = InitialModel.from(browser.binding()); describeInitial(); availability(true); return;
                }
            } finally { browser.dispose(); }
        }
        void describeInitial() {
            initialDescription.setText(initialModel == null ? "Fresh initialization" : initialModel.name() + " - Gen " + initialModel.generation());
            initialDescription.setToolTipText(initialModel == null ? null : initialModel.root() + " / " + initialModel.checkpoint());
        }
        void recipe() { recipe.setText(((TrainingArchitecture) architecture.getSelectedItem()).nnueFamily() ? "Adam; moments and step preserved" : "BRN-3 masked Adam; moments preserved"); }
        Competitor read() {
            String lineageName = name.getText().trim();
            if (lineageName.length() > 120 || lineageName.chars().anyMatch(Character::isISOControl)) throw new IllegalArgumentException("Use a lineage name of at most 120 characters, without control characters");
            return new Competitor(lineageName, (TrainingArchitecture) architecture.getSelectedItem(), Long.parseLong(seed.getText().trim()), value(batch),
                    inheritRate.isSelected() ? null : ((Number) rate.getValue()).doubleValue(), initialModel);
        }
        void load(Competitor c) {
            loading = true;
            try { name.setText(c.name()); architecture.setSelectedItem(c.architecture()); seed.setText(Long.toString(c.seed())); batch.setValue(c.minibatch());
                initialModel = c.initialModel(); inheritRate.setSelected(c.learningRate() == null);
                rate.setValue(c.learningRate() == null ? com.ohinteractive.seedv6.training.model.TrainingRecipe.defaults(c.architecture()).learningRate() : c.learningRate()); describeInitial();
            } finally { loading = false; availability(!controller.busy()); }
        }
    }
}
