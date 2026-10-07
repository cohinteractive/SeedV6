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
    private final JComboBox<Mode> mode = new JComboBox<>(Mode.values());
    private final PlayEnginePanel fixedA, fixedB;
    private final JPanel participantA = new ParticipantSetup(), participantB = new ParticipantSetup();
    private final JPanel trainingProtocol = fields();
    private final JLabel matchNote = new JLabel("Fixed evaluators; no training data or training rounds required.");
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
    private final JButton create = button("arenaCreate", "Create and run");
    private final JButton open = button("arenaOpen", "Open campaign..."), pause = button("arenaPause", "Pause safely");
    private final JLabel status = new JLabel("Ready"), storage = new JLabel();
    private final ArenaHistoryView history = new ArenaHistoryView();
    private final JPanel setup = new SetupPanel();
    private final JScrollPane setupScroll = new JScrollPane(setup);
    private final JTabbedPane views = new JTabbedPane();
    private final ArenaLiveView live = new ArenaLiveView();
    private final Draft defaults;
    private long loadedAttachment = -1;
    private java.util.function.Consumer<WorkspaceActivity> activityListener = activity -> {};

    void onActivity(java.util.function.Consumer<WorkspaceActivity> listener) {
        activityListener = listener;
        publishActivity(controller);
    }

    private void publishActivity(LearningArenaController controller) {
        var update = controller.update();
        String detail = update == null ? "Opening campaign"
                : "Round " + update.state().current().number() + " · " + update.state().current().stage();
        if (update != null && update.liveGame() != null) {
            detail += " · Game " + update.liveGame().gameOrdinal() + "/" + update.state().config().arena().games();
        }
        activityListener.accept(controller.busy() ? new WorkspaceActivity(true, detail) : WorkspaceActivity.idle());
    }
    private final java.util.Set<Path> registeredLineages = new java.util.HashSet<>();
    LearningArenaPanel(TrainingFolders folders) {
        super(new BorderLayout(0, SeedTheme.scale(12))); this.folders = folders;
        fixedA = new PlayEnginePanel("arenaFixedA", "", null, folders, null, this::sourceChanged);
        fixedB = new PlayEnginePanel("arenaFixedB", "", null, folders, null, this::sourceChanged);
        SeedTheme.padding(fixedA, 12, 12, 12, 12); SeedTheme.padding(fixedB, 12, 12, 12, 12);
        participantA.setOpaque(false); participantB.setOpaque(false);
        participantA.add(a, "learning"); participantA.add(fixedA, "fixed");
        participantB.add(b, "learning"); participantB.add(fixedB, "fixed");
        source = new TrainingDataSelector("arenaSource", folders::base,
                () -> java.util.List.of((TrainingArchitecture) a.architecture.getSelectedItem(), (TrainingArchitecture) b.architecture.getSelectedItem()), this::sourceChanged);
        // Capture the existing constructor defaults before any campaign can be restored.
        defaults = draft(null);
        a.architecture.addActionListener(e -> source.compatibilityChanged()); b.architecture.addActionListener(e -> source.compatibilityChanged());
        threads.setName("arenaThreads");
        setName("learningArena"); setBackground(SeedTheme.BACKGROUND); SeedTheme.padding(this, 16, 20, 16, 20);
        var heading = new JPanel(new BorderLayout()); heading.setOpaque(false);
        var modeRow = SeedTheme.panel(new FlowLayout(FlowLayout.LEFT, SeedTheme.scale(10), 0));
        mode.setName("arenaMode"); modeRow.add(SeedTheme.label("Arena", 22, SeedTheme.TEXT));
        modeRow.add(new JLabel("Mode")); modeRow.add(mode); heading.add(modeRow, BorderLayout.WEST);
        var actions = new JPanel(new FlowLayout(FlowLayout.RIGHT)); actions.setOpaque(false);
        for (var button : new JButton[]{open, start, create, resume, pause}) actions.add(button);
        heading.add(actions, BorderLayout.EAST); add(heading, BorderLayout.NORTH);
        setup.setOpaque(false);
        var shared = SeedTheme.panel(new BorderLayout()); var common = fields();
        row(common, 0, "Campaign name", name); row(common, 1, "Opening / shuffle seed", seed); shared.add(common, BorderLayout.NORTH);
        row(trainingProtocol, 0, "Shared source", source);
        shared.add(trainingProtocol); shared.add(matchNote, BorderLayout.SOUTH);
        source.setMinimumSize(new Dimension(100, source.getPreferredSize().height));
        row(trainingProtocol, 1, "Positions / round", positions); row(trainingProtocol, 2, "Epochs / tranche (both)", epochs);
        row(trainingProtocol, 3, "Training rounds (0 = until paused)", rounds);
        var arena = fields(); limit.setName("arenaLimit"); row(arena, 0, "Games (even; colours reversed)", games);
        row(arena, 1, "Search limit", limit); row(arena, 2, "Depth (plies)", depth); row(arena, 3, "Time / move (ms)", millis);
        row(arena, 4, "Search threads", threads); row(arena, 5, "Opening plies minimum", openingMin);
        row(arena, 6, "Opening plies maximum", openingMax); row(arena, 7, "Game ply cap", cap);
        var c = new GridBagConstraints(); c.insets = new Insets(6, 6, 6, 6); c.fill = GridBagConstraints.BOTH; c.weightx = .5;
        c.gridx = 0; c.gridy = 0; setup.add(SeedTheme.card("Participant A", null, participantA), c);
        c.gridx = 1; setup.add(SeedTheme.card("Participant B", null, participantB), c);
        c.gridx = 0; c.gridy = 1; setup.add(SeedTheme.card("Shared campaign protocol and exposure", null, shared), c);
        c.gridx = 1; setup.add(SeedTheme.card("Shared match protocol", null, arena), c);
        var opening = fields(); row(opening, 0, "Starting FEN", fen);
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2; setup.add(opening, c);
        var explanation = new JTextArea("Learning campaigns train both models on the same frozen records and targets after Round 0. Match only compares fixed HCE or network evaluators once, without training.\nDepth is a controlled comparison; time includes evaluator cost. One search thread is most reproducible. Ply-capped pairs are unscored.\nResume restores the saved configuration and completed games. A crash may recompute an unfinished game or unsaved training work.");
        explanation.setEditable(false); explanation.setLineWrap(true); explanation.setWrapStyleWord(true); explanation.setOpaque(false); explanation.setRows(4);
        c.gridy = 3; setup.add(explanation, c);
        setupScroll.setName("arenaSetupScroll"); setupScroll.setBorder(null); setupScroll.getVerticalScrollBar().setUnitIncrement(20);
        views.setName("arenaViews"); views.addTab("Setup", setupScroll);
        views.addTab("Live", live);
        views.addTab("History", history); add(views);
        var footer = new JPanel(new GridLayout(2, 1)); footer.setOpaque(false);
        status.setName("arenaStatus"); storage.setName("arenaStorage"); footer.add(status); footer.add(storage); add(footer, BorderLayout.SOUTH);
        limit.addActionListener(e -> limits());
        mode.addActionListener(e -> { modeControls(); sourceChanged(); SwingUtilities.invokeLater(this::showSetupTop); });
        start.addActionListener(e -> newCampaign()); create.addActionListener(e -> start());
        resume.addActionListener(e -> { load(defaults); controller.resume(); }); pause.addActionListener(e -> controller.pause());
        open.addActionListener(e -> FilePickers.choose(this, FilePickers.Purpose.LEARNING_ARENA_RESUME, "Open Learning Arena campaign",
                controller.root() == null ? folders.base().resolve("learning-arena").toString() : controller.root().toString()).ifPresent(this::openCampaign));
        showState(controller);
        Path remembered = folders.arenaCampaign();
        if (remembered != null) openCampaign(remembered);
    }
    private void newCampaign() {
        folders.rememberArenaCampaign(null);
        controller.newCampaign();
        load(defaults); showState(controller); views.setSelectedIndex(0); showSetupTop();
    }
    private void start() {
        try {
            if (!editable()) throw new IllegalStateException("Use Start new to configure a new campaign");
            commit(setup);
            if (!ready()) throw new IllegalArgumentException("Select available participants and, for learning campaigns, compatible Training Data");
            var draft = draft(source.selected());
            String folder = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + "-" + UUID.randomUUID().toString().substring(0, 8);
            controller.startDraft(folders.base().resolve("learning-arena").resolve(folder), draft::resolve);
            views.setSelectedIndex(1);
        } catch (Exception error) { JOptionPane.showMessageDialog(this, error.getMessage(), "Learning Arena settings", JOptionPane.ERROR_MESSAGE); }
    }
    private Draft draft(DataSource selected) {
        if (matchOnly()) return new Draft(name.getText().trim(), fixedA.fixedCompetitor(), fixedB.fixedCompetitor(), null, 0, 0, 0, Long.parseLong(seed.getText().trim()),
                new Arena(value(games), (Limit) limit.getSelectedItem(), value(depth), value(millis), value(threads), value(openingMin), value(openingMax), value(cap), fen.getText().trim()), Mode.MATCH_ONLY);
        return new Draft(name.getText().trim(), a.read(), b.read(), selected, value(positions), value(epochs), value(rounds), Long.parseLong(seed.getText().trim()),
                new Arena(value(games), (Limit) limit.getSelectedItem(), value(depth), value(millis), value(threads), value(openingMin), value(openingMax), value(cap), fen.getText().trim()), null);
    }
    private record Draft(String name, Competitor a, Competitor b, DataSource source, int positions, int epochs, int rounds, long seed, Arena arena, Mode mode) {
        LearningArenaConfig resolve() throws Exception {
            if (mode != Mode.MATCH_ONLY) source.requireReady();
            return new LearningArenaConfig(name, a, b, source, positions, epochs, rounds, seed, arena, mode);
        }
    }
    private void showState(LearningArenaController c) {
        publishActivity(c);
        var u = c.update();
        if (u != null && loadedAttachment != c.attachment()) {
            loadedAttachment = c.attachment();
            var config = u.state().config();
            load(new Draft(config.name(), config.a(), config.b(), config.source(), config.positionsPerRound(), config.epochs(), config.rounds(), config.seed(), config.arena(), config.mode()));
            folders.rememberArenaCampaign(c.root());
        }
        boolean editable = editable();
        enable(setup, editable); mode.setEnabled(editable); a.availability(editable); b.availability(editable); limits(); source.setEditable(editable && !matchOnly());
        fixedA.setEditable(editable); fixedB.setEditable(editable); modeControls();
        start.setEnabled(!c.busy()); create.setVisible(c.root() == null); create.setEnabled(editable && ready()); open.setEnabled(!c.busy()); pause.setEnabled(c.busy());
        resume.setEnabled(!c.busy() && c.root() != null && c.update() != null && c.update().state().status() != LearningArenaState.Status.COMPLETE);
        storage.setText(c.root() == null ? "New campaigns: " + folders.base().resolve("learning-arena") : "Campaign: " + c.root());
        live.showUpdate(u);
        if (u == null) { history.showState(null, null); status.setText(c.error().isBlank() ? c.busy() ? "Opening campaign..." : "New campaign draft - configure Setup, then Create and run" : c.error()); return; }
        var state = u.state();
        if (c.root() != null) {
            if (state.history().getFirst().a() != null && !state.config().a().isHandcrafted()) registerLineage(c.root().resolve("A"), state.config().a().architecture());
            if (state.history().getFirst().b() != null && !state.config().b().isHandcrafted()) registerLineage(c.root().resolve("B"), state.config().b().architecture());
        }
        status.setText(c.error().isBlank() ? "Round " + state.current().number() + " | " + state.status() + " | " + u.detail() + " | Saved setup is read-only; Start new creates a fresh draft" : c.error());
        status.setToolTipText(status.getText()); storage.setToolTipText(storage.getText());
        history.showState(state, c.root());
    }
    private void load(Draft c) {
        mode.setSelectedItem(c.mode() == Mode.MATCH_ONLY ? Mode.MATCH_ONLY : Mode.LEARNING);
        name.setText(c.name());
        if (matchOnly()) { fixedA.loadFixed(c.a()); fixedB.loadFixed(c.b()); }
        else { fixedA.clearFixed(); fixedB.clearFixed(); a.load(c.a()); b.load(c.b()); positions.setValue(c.positions()); epochs.setValue(c.epochs()); rounds.setValue(c.rounds()); }
        seed.setText(Long.toString(c.seed())); var v = c.arena(); games.setValue(v.games()); limit.setSelectedItem(v.limit()); depth.setValue(v.depth());
        millis.setValue((int) v.millis()); threads.setValue(v.threads()); openingMin.setValue(v.openingMin()); openingMax.setValue(v.openingMax()); cap.setValue(v.maximumPlies()); fen.setText(v.startingFen());
        if (c.source() == null) source.resetSelection(); else source.select(c.source());
    }
    private void registerLineage(Path root, TrainingArchitecture architecture) {
        if (registeredLineages.add(root)) folders.register(folders.base(), NetworkArchitecture.valueOf(architecture.name()), root);
    }
    private boolean editable() { return !controller.busy() && controller.root() == null; }
    private boolean matchOnly() { return mode.getSelectedItem() == Mode.MATCH_ONLY; }
    private boolean ready() { return matchOnly() ? fixedA != null && fixedB != null && fixedA.validSelection() && fixedB.validSelection() : source != null && source.ready(); }
    private void sourceChanged() { create.setEnabled(editable() && ready()); }
    private void modeControls() {
        ((CardLayout) participantA.getLayout()).show(participantA, matchOnly() ? "fixed" : "learning");
        ((CardLayout) participantB.getLayout()).show(participantB, matchOnly() ? "fixed" : "learning");
        trainingProtocol.setVisible(!matchOnly()); matchNote.setVisible(matchOnly());
        revalidate();
    }
    void poll() { controller.poll(); }
    void openCampaign(Path root) {
        folders.rememberArenaCampaign(null);
        load(defaults); controller.open(root); views.setSelectedIndex(0);
    }
    void showSetupTop() { setupScroll.getViewport().setViewPosition(new Point(0, 0)); }
    Runnable beginShutdown() {
        fixedA.dispose(); fixedB.dispose();
        var campaign = controller.beginShutdown(); var data = source.beginShutdown();
        return () -> { try { campaign.run(); } finally { data.run(); } };
    }
    private void limits() { depth.setEnabled(editable() && limit.getSelectedItem() == Limit.DEPTH); millis.setEnabled(editable() && limit.getSelectedItem() == Limit.TIME); }
    private static JPanel fields() { var p = new JPanel(new GridBagLayout()); p.setOpaque(false); SeedTheme.padding(p, 12, 12, 12, 12); return p; }
    private static void row(JPanel panel, int y, String title, JComponent value) { TrainingPanel.row(panel, y, title, value); }
    private static JTextField field(String name, String text) { var f = new JTextField(text, 16); f.setName(name); return f; }
    private static JButton button(String name, String title) { var b = new JButton(title); b.setName(name); return b; }
    private static JSpinner number(String name, int value, int min, int max) { var s = new JSpinner(new SpinnerNumberModel(value, min, max, 1)); s.setName(name); return s; }
    private static int value(JSpinner field) { return ((Number) field.getValue()).intValue(); }
    private static void commit(Container c) throws java.text.ParseException { for (Component item : c.getComponents()) { if (!item.isVisible()) continue; if (item instanceof JSpinner s) s.commitEdit(); else if (item instanceof Container nested) commit(nested); } }
    private static void enable(Container c, boolean enabled) { for (Component item : c.getComponents()) { item.setEnabled(enabled); if (item instanceof Container nested) enable(nested, enabled); } }
    private static final class SetupPanel extends JPanel implements Scrollable {
        SetupPanel() { super(new GridBagLayout()); }
        public Dimension getPreferredScrollableViewportSize() { return new Dimension(1000, 700); }
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return SeedTheme.scale(20); }
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) { return Math.max(1, visible.height - SeedTheme.scale(30)); }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }
    /** Hidden training controls must not reserve empty space in match-only setup. */
    private static final class ParticipantSetup extends JPanel {
        ParticipantSetup() { super(new CardLayout()); }
        @Override public Dimension getPreferredSize() {
            for (Component c : getComponents()) if (c.isVisible()) return c.getPreferredSize();
            return super.getPreferredSize();
        }
        @Override public Dimension getMinimumSize() {
            for (Component c : getComponents()) if (c.isVisible()) return c.getMinimumSize();
            return super.getMinimumSize();
        }
    }
    private final class CompetitorFields extends JPanel {
        final JTextField name, seed;
        final JComboBox<TrainingArchitecture> architecture = new JComboBox<>(new TrainingArchitecture[]{TrainingArchitecture.NNUE_MATERIAL, TrainingArchitecture.NNUE, TrainingArchitecture.BRN3, TrainingArchitecture.BRN_PAIR2});
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
        void recipe() { recipe.setText(((TrainingArchitecture) architecture.getSelectedItem()).nnueFamily() ? "Adam; moments and step preserved" : ((TrainingArchitecture) architecture.getSelectedItem()).displayName() + "; masked Adam; moments preserved"); }
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
