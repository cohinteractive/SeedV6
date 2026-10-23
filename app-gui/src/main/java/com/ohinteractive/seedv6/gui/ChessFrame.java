package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.JTabbedPane;
import javax.swing.Timer;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.ImageIcon;
import javax.swing.JScrollPane;
import java.util.function.Consumer;

import com.ohinteractive.seedv6.core.move.MoveIntent.Promotion;
import com.ohinteractive.seedv6.search.exact.ParallelSearch;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.formdev.flatlaf.util.ScaledImageIcon;
import com.ohinteractive.seedv6.ApplicationVersion;

/** Desktop shell; controller lifecycles remain independent of workspace navigation. */
final class ChessFrame extends JFrame implements GameController.View {

    ChessFrame() {
        this(TrainingSettings.selectionDefaults(TrainingSettings.preferences()), new TrainingController.Backend(),
                settings -> {}, new TrainingFolders(TrainingSettings.preferences()),
                TrainingSettings.preferences().node("play"));
    }

    ChessFrame(TrainingSettings settings, TrainingController.Backend backend, Consumer<TrainingSettings> persist) {
        this(settings, backend, persist, new TrainingFolders(settings), null);
    }

    ChessFrame(TrainingSettings settings, TrainingController.Backend backend, Consumer<TrainingSettings> persist,
                       TrainingFolders folders, java.util.prefs.Preferences playPreferences) {
        super(SeedTheme.initialize());
        this.playPreferences = playPreferences;
        setIconImages(ApplicationIcons.windowImages());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setJMenuBar(ApplicationMenu.create(this::closeWindow, this::showAbout, this::showStorageSettings));
        Rectangle usable = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setMinimumSize(new Dimension(Math.min(SeedTheme.scale(1100), usable.width), Math.min(SeedTheme.scale(760), usable.height)));
        setLayout(new BorderLayout());
        getContentPane().setBackground(SeedTheme.BACKGROUND);
        depthSpinner.setName("playDepth"); threadsSpinner.setName("playThreads");
        threadsSpinner.setToolTipText("Configured participants, including the main worker. Max follows available processors up to the supported search limit. Construction V1 uses this setting for all three evaluators. Live Threads shows current participation.");
        if (playPreferences != null) ThreadSelection.setChoice(threadsSpinner,
                Math.max(0, Math.min(ParallelSearch.MAX_WORKERS, playPreferences.getInt("threads", ParallelSearch.DEFAULT_WORKERS))));
        restorePlaySettings();
        ((JSpinner.DefaultEditor) depthSpinner.getEditor()).getTextField().setHorizontalAlignment(javax.swing.JTextField.LEFT);
        evaluatorBox.setName("playEvaluator"); humanSideBox.setName("humanSide"); modeBox.setName("gameMode");
        pinnedLabel.setName("pinnedBest");
        limitKindBox.setName("searchLimit"); movetimeSpinner.setName("playMovetime");
        newGameButton.setName("newGame"); startGameButton.setName("startGame"); stopButton.setName("stopSearch"); loadFenButton.setName("loadFen");
        stopGameButton.setName("stopGame"); resignButton.setName("resign");

        whiteEngine = new PlayEnginePanel("white", "White Engine", settings.root(), folders, playPreferences == null ? null : playPreferences.node("white"), this::refreshPlaySetup);
        blackEngine = new PlayEnginePanel("black", "Black Engine", settings.root(), folders, playPreferences == null ? null : playPreferences.node("black"), this::refreshPlaySetup);
        opponentEngine = new PlayEnginePanel("opponent", "Engine Opponent", settings.root(), folders,
                playPreferences == null ? null : playPreferences.node("opponent"), this::refreshPlaySetup);
        evaluatorBox.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                    int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value == PlayEvaluator.Mode.BEST_NNUE && modeBox.getSelectedItem() == GameController.GameMode.HUMAN_VS_ENGINE)
                    setText("Network");
                return this;
            }
        });
        evaluatorBox.setToolTipText("Human vs Engine evaluator choices apply when Start Game is pressed. All three evaluators use Construction V1 and the configured workers. Frozen networks are bundled.");
        trainingPanel = new TrainingPanel(settings, folders);
        tabs.setName("workspaces");
        tabs.putClientProperty("JTabbedPane.leadingComponent", identity());
        tabs.putClientProperty("JTabbedPane.tabAreaAlignment", "leading");
        tabs.addTab("Play", createPlayWorkspace());
        JPanel trainingWorkspace = new JPanel(new BorderLayout());
        trainingWorkspace.setBackground(SeedTheme.BACKGROUND);
        SeedTheme.padding(trainingWorkspace, 16, 20, 16, 20);
        trainingBoard = new TrainingBoard(trainingPanel::showDiagnostics);
        trainingSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, trainingBoard, trainingPanel);
        trainingSplit.setName("trainingSplit");
        trainingSplit.setBorder(BorderFactory.createEmptyBorder()); trainingSplit.setOpaque(false);
        trainingSplit.setDividerSize(SeedTheme.scale(18)); trainingSplit.setContinuousLayout(true);
        trainingSplit.setResizeWeight(0.44); trainingSplit.setDividerLocation(SeedTheme.scale(610));
        trainingPanel.setMinimumSize(new Dimension(SeedTheme.scale(610), 0));
        trainingWorkspace.add(trainingSplit);
        tabs.addTab("Network Training", trainingWorkspace);
        learningArena = new LearningArenaPanel(folders);
        tabs.addTab("Arena", learningArena);
        activityTabs = new WorkspaceActivityTabs(tabs);
        learningArena.onActivity(activity -> {
            activityTabs.update(2, activity);
            refreshStatus();
        });
        add(tabs, BorderLayout.CENTER);
        JPanel status = new JPanel(new BorderLayout(12, 0));
        status.setBackground(SeedTheme.PANEL);
        status.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, SeedTheme.LINE),
                BorderFactory.createEmptyBorder(SeedTheme.scale(6), SeedTheme.scale(20), SeedTheme.scale(6), SeedTheme.scale(20))));
        status.add(SeedTheme.label("SeedV6   |   Java Chess Engine", 11, SeedTheme.SECONDARY), BorderLayout.WEST);
        statusLabel.setFont(SeedTheme.font(11, Font.PLAIN)); statusLabel.setForeground(SeedTheme.SECONDARY);
        statusLabel.setHorizontalAlignment(JLabel.RIGHT);
        statusLabel.setName("workspaceStatus");
        status.add(statusLabel, BorderLayout.CENTER); add(status, BorderLayout.SOUTH);

        controller = new GameController(
            new EngineSearchAdapter(ThreadSelection.resolved(threadsSpinner)), this
        );
        controller.setCheckpointRoot(settings.root());
        // Sibling owners: neither controller receives the other's stop/reset/search lifecycle.
        trainingController = new TrainingController(settings, backend, persist,
                this::showTraining);
        trainingPanel.bind(trainingController);
        if (playPreferences != null) trainingPanel.loadInitialLineage();
        trainingBoard.showState(trainingController.state());
        tabs.addChangeListener(event -> {
            trainingSelected = tabs.getSelectedIndex() == 1;
            learningArenaSelected = tabs.getSelectedIndex() == 2;
            if (learningArenaSelected && !learningArenaLayoutInitialized) {
                learningArenaLayoutInitialized = true;
                SwingUtilities.invokeLater(learningArena::showSetupTop);
            }
            if (tabs.getSelectedIndex() == 1 && !trainingLayoutInitialized) {
                trainingLayoutInitialized = true;
                SwingUtilities.invokeLater(() -> { trainingSplit.setDividerLocation(.445); trainingPanel.showDashboardTop(); });
            }
            refreshStatus();
        });
        trainingTimer = new Timer(500, event -> {
            controller.pollSearchTelemetry();
            trainingController.poll();
            learningArena.poll();
            refreshStatus();
        });
        trainingTimer.start();
        boardPanel.setInputListener(controller);
        controller.setGameMode((GameController.GameMode) modeBox.getSelectedItem());
        controller.setHumanSide((GameController.HumanSide) humanSideBox.getSelectedItem());
        applySearchSettings();
        installActions();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                closeWindow();
            }
        });
        setPreferredSize(new Dimension(SeedTheme.scale(1440), SeedTheme.scale(950)));
        pack();
        setSize(Math.min(getWidth(), usable.width), Math.min(getHeight(), usable.height));
        setLocationRelativeTo(null);
        controller.initialize();
        refreshPlaySetup();
    }

    @Override
    public void showPosition(GameController.PositionView position) {
        requireEdt();
        boardPanel.showPosition(position);
        refreshStatus();
        moves.showPosition(position);
        updatePlayers();
        if (!searchRunning && !evaluatorChanging) {
            String state = !controller.gameStarted() && !position.status().terminal() ? "Prepared" : position.status().displayText();
            controlState.setText("●  " + state); controlState.setToolTipText(state);
        }
        setControlsEnabled(!closing && !evaluatorChanging);
    }
    private void showStorageSettings() { trainingPanel.showStorageSettings(); }

    @Override
    public void showSearch(GameController.SearchInfo search) {
        requireEdt();
        var evaluator = participants.forSide(search.scoreSide());
        boardPanel.showScore(engineCard.showSearch(search, evaluator, controller.workerCount()), evaluator.scoreUnits());
        String state = controller.positionStatus().terminal() ? controller.positionStatus().displayText()
                : !controller.gameStarted() && !evaluatorChanging ? "Prepared"
                : search.state().equals("Idle") ? controller.positionStatus().displayText() : search.state();
        controlState.setText("●  " + state);
        controlState.setToolTipText(state);
        activityTabs.update(0, controller.activity());
        refreshStatus();
    }

    @Override
    public void setSearchRunning(boolean running) {
        requireEdt();
        searchRunning = running;
        setControlsEnabled(!closing && !evaluatorChanging);
    }

    @Override public void showEvaluator(PlayEvaluator evaluator, boolean changing) {
        showParticipants(PlayParticipants.shared(evaluator), changing);
    }

    @Override public void showParticipants(PlayParticipants bindings, boolean changing) {
        requireEdt();
        participants = bindings;
        evaluatorChanging = changing;
        updatePlayers();
        setControlsEnabled(!closing && !evaluatorChanging);
    }

    private void showTraining(TrainingController.ViewState state) {
        requireEdt();
        controller.setCheckpointRoot(state.settings().root());
        trainingPanel.showState(state);
        trainingBoard.showState(state);
        activityTabs.update(1, WorkspaceActivity.training(state));
        refreshStatus();
    }

    private void refreshStatus() {
        if (activityTabs == null || controller == null || trainingController == null) return;
        String idle = tabs.getSelectedIndex() == 2 ? "Arena"
                : tabs.getSelectedIndex() == 1 ? "Network Training · " + TrainingDashboardModel.phase(trainingController.state())
                : controller.positionStatus().displayText();
        String text = activityTabs.status(idle);
        statusLabel.setText(text);
        statusLabel.setToolTipText(text);
    }

    @Override
    public Promotion choosePromotion(List<Promotion> choices) {
        requireEdt();
        final Promotion[] options = choices.toArray(Promotion[]::new);
        return (Promotion) JOptionPane.showInputDialog(
            this, "Choose the promotion piece.", "Promotion",
            JOptionPane.QUESTION_MESSAGE, null, options, options[0]
        );
    }

    @Override
    public void showError(String title, String message) {
        requireEdt();
        JOptionPane.showMessageDialog(
            this, message == null ? title : message, title, JOptionPane.ERROR_MESSAGE
        );
    }

    private final BoardPanel boardPanel = new BoardPanel();
    private final JTabbedPane tabs = new JTabbedPane();
    private WorkspaceActivityTabs activityTabs;
    private final JLabel statusLabel = new JLabel(" ");
    private final MoveScoresheet moves = new MoveScoresheet();
    private final EngineCard engineCard = new EngineCard();
    private final JLabel controlState = SeedTheme.label("●  Ready", 12, SeedTheme.GREEN);
    private final JLabel limitLabel = SeedTheme.label("Depth", 12, SeedTheme.SECONDARY);
    private final JPanel limitEditor = SeedTheme.panel(new CardLayout());
    private PlayParticipants participants = PlayParticipants.shared(PlayEvaluator.handcrafted());
    private final PlayEnginePanel whiteEngine, blackEngine, opponentEngine;
    private final JPanel engineSetup = SeedTheme.panel(new BorderLayout(0, SeedTheme.scale(6)));
    private final JButton swapSides = new JButton("Swap Sides");
    private final JPanel participantSetup = SeedTheme.panel(new CardLayout());
    private final JLabel evaluatorLabel = SeedTheme.label("Evaluator", 12, SeedTheme.SECONDARY);
    private JSplitPane playSplit, trainingSplit;
    private final JPanel playViews = SeedTheme.panel(new CardLayout());
    private final JButton settingsButton = new JButton(controlIcon(false)), closeSettingsButton = new JButton(controlIcon(true));
    private final JButton startGameButton = new JButton("Start Game");
    private final JButton newGameButton = new JButton("New Game");
    private final JButton loadFenButton = new JButton("Load FEN");
    private final JButton stopButton = new JButton("Stop Search");
    private final JButton stopGameButton = new JButton("Stop Game");
    private final JButton resignButton = new JButton("Resign");
    private String legacyEvaluator;
    private final JComboBox<PlayEvaluator.Mode> evaluatorBox = new JComboBox<>(PlayEvaluator.Mode.competitiveChoices());
    private final JLabel pinnedLabel = new JLabel("Handcrafted evaluator");
    private final JComboBox<GameController.GameMode> modeBox = new JComboBox<>(GameController.GameMode.values());
    private final JComboBox<GameController.HumanSide> humanSideBox = new JComboBox<>(GameController.HumanSide.values());
    private final JComboBox<GameController.LimitKind> limitKindBox = new JComboBox<>(GameController.LimitKind.values());
    private final JSpinner depthSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 256, 1));
    private final JSpinner movetimeSpinner = new JSpinner(new SpinnerNumberModel(1_000L, 50L, 600_000L, 50L));
    private final JSpinner threadsSpinner = ThreadSelection.spinner(ParallelSearch.DEFAULT_WORKERS);
    private final java.util.prefs.Preferences playPreferences;
    private final GameController controller;
    private final TrainingController trainingController;
    private final TrainingPanel trainingPanel;
    private final LearningArenaPanel learningArena;
    private boolean learningArenaSelected;
    private boolean learningArenaLayoutInitialized;
    private final TrainingBoard trainingBoard;
    private final Timer trainingTimer;
    private boolean searchRunning;
    private boolean closing;
    private boolean evaluatorChanging, updatingEvaluator;
    private boolean trainingLayoutInitialized, trainingSelected;

    private JPanel createPlayWorkspace() {
        JPanel workspace = new JPanel(new BorderLayout()); workspace.setBackground(SeedTheme.BACKGROUND);
        SeedTheme.padding(workspace, 16, 20, 14, 20);
        JPanel board = new SeedTheme.Card(); board.setLayout(new BorderLayout());
        board.add(boardPanel, BorderLayout.CENTER);
        board.setMinimumSize(new Dimension(SeedTheme.scale(380), 0));
        JPanel live = SeedTheme.panel(new BorderLayout(0, SeedTheme.scale(12)));
        live.setName("playLiveView");
        live.add(engineCard, BorderLayout.NORTH); live.add(moves, BorderLayout.CENTER);
        JPanel settings = new SettingsView(); settings.add(createGameCard(), BorderLayout.NORTH);
        JScrollPane settingsScroll = new JScrollPane(settings, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        settingsScroll.setBorder(BorderFactory.createEmptyBorder());
        settingsScroll.getVerticalScrollBar().setUnitIncrement(SeedTheme.scale(24));
        playViews.add(live, "live"); playViews.add(SeedTheme.card("Game settings", closeSettingsButton, settingsScroll), "settings");
        playViews.setMinimumSize(new Dimension(SeedTheme.scale(510), 0));
        playViews.setPreferredSize(new Dimension(SeedTheme.scale(600), SeedTheme.scale(800)));
        workspace.add(createControls(), BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, board, playViews);
        playSplit = split;
        split.setName("playSplit"); split.setBorder(BorderFactory.createEmptyBorder());
        split.setOpaque(false); split.setDividerSize(SeedTheme.scale(18)); split.setContinuousLayout(true);
        split.setResizeWeight(0.52); split.setDividerLocation(SeedTheme.scale(720));
        workspace.add(split); return workspace;
    }

    private JPanel createGameCard() {
        JPanel form = SeedTheme.panel(new GridLayout(0, 1, 0, SeedTheme.scale(10)));
        SeedTheme.padding(form, 8, 16, 12, 16);
        JPanel left = SeedTheme.panel(new GridBagLayout()), right = SeedTheme.panel(new GridBagLayout());
        row(left, 0, "Mode", modeBox); row(left, 1, "Human side", humanSideBox); row(left, 2, evaluatorLabel, evaluatorBox);
        pinnedLabel.setFont(SeedTheme.font(11, Font.PLAIN)); pinnedLabel.setForeground(SeedTheme.SECONDARY);
        pinnedLabel.setMinimumSize(new Dimension(0, SeedTheme.scale(26)));
        row(left, 3, "Network", pinnedLabel);
        limitEditor.add(depthSpinner, "depth"); limitEditor.add(movetimeSpinner, "time");
        row(right, 0, "Search limit", limitKindBox); row(right, 1, limitLabel, limitEditor); row(right, 2, "Threads", threadsSpinner);
        form.add(left); form.add(right);
        startGameButton.putClientProperty("FlatLaf.style", "background: #218f59; foreground: #ffffff; hoverBackground: #29a568; pressedBackground: #187547");
        var models = SeedTheme.panel(new GridLayout(0, 1, 0, SeedTheme.scale(16)));
        models.add(whiteEngine); models.add(blackEngine); engineSetup.add(models);
        var swapRow = SeedTheme.panel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        swapSides.setName("swapSides"); swapSides.addActionListener(e -> whiteEngine.swapWith(blackEngine));
        swapRow.add(swapSides); engineSetup.add(swapRow, BorderLayout.SOUTH); engineSetup.setVisible(false);
        SeedTheme.padding(engineSetup, 0, 16, 12, 16);
        SeedTheme.padding(opponentEngine, 0, 16, 12, 16);
        participantSetup.add(engineSetup, "engines"); participantSetup.add(opponentEngine, "opponent");
        participantSetup.setVisible(false);
        var body = SeedTheme.panel(new BorderLayout()); body.add(form, BorderLayout.NORTH); body.add(participantSetup);
        JPanel card = new JPanel(new BorderLayout()) {
            @Override public Dimension getMinimumSize() { return new Dimension(0, getPreferredSize().height); }
        };
        card.setOpaque(false); card.add(body); return card;
    }

    /** Only settings scroll as a whole; its close control stays outside this viewport. */
    private static final class SettingsView extends JPanel implements javax.swing.Scrollable {
        SettingsView() { super(new BorderLayout()); setOpaque(false); }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) { return SeedTheme.scale(24); }
        public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) { return Math.max(1, r.height - SeedTheme.scale(24)); }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private static javax.swing.Icon controlIcon(boolean close) {
        return new javax.swing.Icon() {
            public int getIconWidth() { return SeedTheme.scale(18); }
            public int getIconHeight() { return SeedTheme.scale(18); }
            public void paintIcon(Component c, Graphics graphics, int x, int y) {
                Graphics2D g = (Graphics2D) graphics.create();
                try {
                    g.translate(x, y); g.scale(getIconWidth() / 18.0, getIconHeight() / 18.0);
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(c.isEnabled() ? SeedTheme.TEXT : SeedTheme.MUTED); g.setStroke(new BasicStroke(1.7f));
                    if (close) { g.drawLine(4, 4, 14, 14); g.drawLine(14, 4, 4, 14); }
                    else {
                        var path = new java.awt.geom.Path2D.Double();
                        for (int i = 0; i < 32; i++) {
                            double angle = i * Math.PI / 16, radius = i % 4 < 2 ? 8 : 6;
                            double px = 9 + radius * Math.cos(angle), py = 9 + radius * Math.sin(angle);
                            if (i == 0) path.moveTo(px, py); else path.lineTo(px, py);
                        }
                        path.closePath(); g.draw(path); g.drawOval(6, 6, 6, 6);
                    }
                } finally { g.dispose(); }
            }
        };
    }

    private JPanel createControls() {
        JPanel body = SeedTheme.panel(new BorderLayout(SeedTheme.scale(12), 0));
        SeedTheme.padding(body, 0, 0, 12, 0);
        JPanel buttons = SeedTheme.panel(new FlowLayout(FlowLayout.LEFT, SeedTheme.scale(8), 0));
        buttons.add(newGameButton); buttons.add(startGameButton); buttons.add(loadFenButton);
        buttons.add(stopButton); buttons.add(stopGameButton); buttons.add(resignButton);
        body.add(buttons, BorderLayout.WEST);
        JPanel status = SeedTheme.panel(new FlowLayout(FlowLayout.RIGHT, SeedTheme.scale(8), 0));
        status.add(controlState); status.add(settingsButton); body.add(status, BorderLayout.EAST);
        settingsButton.setName("gameSettings"); closeSettingsButton.setName("closeGameSettings");
        settingsButton.setToolTipText("Game settings"); closeSettingsButton.setToolTipText("Close game settings");
        settingsButton.getAccessibleContext().setAccessibleName("Game settings");
        closeSettingsButton.getAccessibleContext().setAccessibleName("Close game settings");
        settingsButton.addActionListener(e -> ((CardLayout) playViews.getLayout()).show(playViews, "settings"));
        closeSettingsButton.addActionListener(e -> ((CardLayout) playViews.getLayout()).show(playViews, "live"));
        stopButton.setEnabled(false);
        return body;
    }

    private static JPanel identity() {
        JPanel panel = SeedTheme.panel(new FlowLayout(FlowLayout.LEFT, SeedTheme.scale(10), SeedTheme.scale(8)));
        SeedTheme.padding(panel, 0, 12, 0, 20);
        panel.add(new JLabel(new ScaledImageIcon(new ImageIcon(ApplicationIcons.windowImages().getLast()), 32, 32)));
        JLabel name = SeedTheme.label("SeedV6", 17, SeedTheme.TEXT); name.setFont(SeedTheme.font(17, Font.BOLD)); panel.add(name);
        return panel;
    }

    private void updatePlayers() {
        GameController.GameMode mode = (GameController.GameMode) modeBox.getSelectedItem();
        boolean humanWhite = humanSideBox.getSelectedItem() == GameController.HumanSide.WHITE;
        boolean blackEngine = mode == GameController.GameMode.ENGINE_VS_ENGINE || mode == GameController.GameMode.HUMAN_VS_ENGINE && humanWhite;
        boolean whiteEngine = mode == GameController.GameMode.ENGINE_VS_ENGINE || mode == GameController.GameMode.HUMAN_VS_ENGINE && !humanWhite;
        // A failed mode change retains the previous bindings, which may still be distinct.
        PlayEvaluator displayed = mode == GameController.GameMode.HUMAN_VS_ENGINE && humanWhite ? participants.black() : participants.white();
        boolean nnueActive = displayed.neural();
        pinnedLabel.setText(evaluatorChanging ? "Loading evaluator…" : !controller.gameStarted() ? "Apply selection with Start Game" : nnueActive
                ? (participants.selection().equals(PlayParticipants.Selection.BEST) ? "Pinned best: " : "Pinned network: ")
                    + displayed.description() : "Handcrafted evaluator");
        pinnedLabel.setToolTipText(controller.gameStarted() && nnueActive ? displayed.identity(controller.workerCount()) : null);
        String black = blackEngine ? "SeedV6 (Engine) - " + playerDescription(false)
                : mode == GameController.GameMode.HUMAN_VS_HUMAN ? "Black (Human)" : "You (Human)";
        String white = whiteEngine ? "SeedV6 (Engine) - " + playerDescription(true)
                : mode == GameController.GameMode.HUMAN_VS_HUMAN ? "White (Human)" : "You (Human)";
        boardPanel.showPlayers(white, black, whiteEngine && controller.gameStarted() ? participants.white().identity(controller.workerCount()) : white,
                blackEngine && controller.gameStarted() ? participants.black().identity(controller.workerCount()) : black);
    }

    private String playerDescription(boolean white) {
        if (controller.gameStarted()) return (white ? participants.white() : participants.black()).description();
        if (modeBox.getSelectedItem() == GameController.GameMode.ENGINE_VS_ENGINE)
            return (white ? whiteEngine : blackEngine).selectionDescription();
        var selected = (PlayEvaluator.Mode) evaluatorBox.getSelectedItem();
        return selected == null ? "Saved evaluator unavailable: " + legacyEvaluator + "; select an evaluator" : selected == PlayEvaluator.Mode.BEST_NNUE ? opponentEngine.selectionDescription()
                : selected == PlayEvaluator.Mode.HANDCRAFTED ? "Handcrafted evaluator" : selected.toString();
    }

    private void installActions() {
        newGameButton.addActionListener(event -> controller.newGame());
        startGameButton.addActionListener(event -> {
            if (modeBox.getSelectedItem() == GameController.GameMode.ENGINE_VS_ENGINE) {
                if (whiteEngine.validSelection() && blackEngine.validSelection()) controller.startEngineGame(
                        whiteEngine.versus(blackEngine));
            } else if (modeBox.getSelectedItem() == GameController.GameMode.HUMAN_VS_ENGINE) {
                PlayEvaluator.Mode evaluator = (PlayEvaluator.Mode) evaluatorBox.getSelectedItem();
                if (evaluator != PlayEvaluator.Mode.BEST_NNUE || opponentEngine.validSelection())
                    controller.startGame(evaluator, evaluator != PlayEvaluator.Mode.BEST_NNUE
                            ? PlayParticipants.Selection.BEST
                            : PlayParticipants.Selection.singleEngine(opponentEngine.selectedRoot(), opponentEngine.selectedId(), opponentEngine.selectedLineageId()));
            } else controller.startGame();
        });
        evaluatorBox.addActionListener(event -> { if (!updatingEvaluator) { legacyEvaluator = null; rememberPlaySettings(); refreshPlaySetup(); } });
        loadFenButton.addActionListener(event -> {
            final String fen = JOptionPane.showInputDialog(
                this, "Enter a complete six-field FEN:", "Load FEN",
                JOptionPane.QUESTION_MESSAGE
            );
            if(fen != null) controller.loadFen(fen);
        });
        stopButton.addActionListener(event -> controller.stopSearch());
        stopGameButton.addActionListener(event -> controller.stopGame());
        resignButton.addActionListener(event -> controller.resign());
        modeBox.addActionListener(event -> {
            final GameController.GameMode mode = (GameController.GameMode) modeBox.getSelectedItem();
            humanSideBox.setEnabled(mode == GameController.GameMode.HUMAN_VS_ENGINE);
            controller.setGameMode(mode);
            rememberPlaySettings(); refreshPlaySetup();
        });
        humanSideBox.addActionListener(event -> {
            controller.setHumanSide((GameController.HumanSide) humanSideBox.getSelectedItem());
            rememberPlaySettings();
        });
        limitKindBox.addActionListener(event -> {
            updateLimitControlState();
            applySearchSettings();
        });
        depthSpinner.addChangeListener(event -> applySearchSettings());
        movetimeSpinner.addChangeListener(event -> applySearchSettings());
        threadsSpinner.addChangeListener(event -> {
            int resolved = ThreadSelection.resolved(threadsSpinner);
            if (resolved != controller.workerCount()) controller.setWorkerCount(resolved);
            rememberPlaySettings(); showConfiguredLimits();
        });
        updateLimitControlState();
    }

    private void applySearchSettings() {
        controller.setSearchSettings(new GameController.SearchSettings(
            (GameController.LimitKind) limitKindBox.getSelectedItem(),
            ((Number) depthSpinner.getValue()).intValue(),
            ((Number) movetimeSpinner.getValue()).longValue()
        ));
        rememberPlaySettings(); showConfiguredLimits();
    }

    private void restorePlaySettings() {
        evaluatorBox.setSelectedItem(PlayEvaluator.Mode.CONVENTIONAL_CP64);
        if (playPreferences == null) return;
        restoreEnum(modeBox, GameController.GameMode.class, "mode");
        restoreEnum(humanSideBox, GameController.HumanSide.class, "humanSide");
        String saved = playPreferences.get("evaluator", PlayEvaluator.Mode.CONVENTIONAL_CP64.name());
        try {
            var selection = PlayEvaluator.Mode.valueOf(saved);
            if (!selection.competitive()) throw new IllegalArgumentException();
            evaluatorBox.setSelectedItem(selection);
        } catch (IllegalArgumentException obsolete) {
            legacyEvaluator = saved;
            evaluatorBox.setSelectedIndex(-1);
        }
        restoreEnum(limitKindBox, GameController.LimitKind.class, "limitKind");
        depthSpinner.setValue(Math.max(1, Math.min(256, playPreferences.getInt("depth", 4))));
        movetimeSpinner.setValue(Math.max(50L, Math.min(600_000L, playPreferences.getLong("movetimeMillis", 1_000L))));
    }
    private <E extends Enum<E>> void restoreEnum(JComboBox<E> box, Class<E> type, String key) {
        try { box.setSelectedItem(Enum.valueOf(type, playPreferences.get(key, box.getItemAt(box.getSelectedIndex()).name()))); }
        catch (IllegalArgumentException obsolete) { /* Retain the supported default. */ }
    }
    private void rememberPlaySettings() {
        if (playPreferences == null) return;
        playPreferences.put("mode", ((GameController.GameMode) modeBox.getSelectedItem()).name());
        playPreferences.put("humanSide", ((GameController.HumanSide) humanSideBox.getSelectedItem()).name());
        if (evaluatorBox.getSelectedItem() != null) playPreferences.put("evaluator", ((PlayEvaluator.Mode) evaluatorBox.getSelectedItem()).name());
        playPreferences.put("limitKind", ((GameController.LimitKind) limitKindBox.getSelectedItem()).name());
        playPreferences.putInt("depth", ((Number) depthSpinner.getValue()).intValue());
        playPreferences.putLong("movetimeMillis", ((Number) movetimeSpinner.getValue()).longValue());
        playPreferences.putInt("threads", ((Number) threadsSpinner.getValue()).intValue());
    }
    private void showConfiguredLimits() {
        engineCard.showConfiguration((GameController.LimitKind) limitKindBox.getSelectedItem(),
                ((Number) depthSpinner.getValue()).intValue(), ((Number) movetimeSpinner.getValue()).longValue(),
                ((Number) threadsSpinner.getValue()).intValue(), controller.workerCount());
    }

    private void updateLimitControlState() {
        final boolean depth = limitKindBox.getSelectedItem() == GameController.LimitKind.DEPTH;
        limitLabel.setText(depth ? "Depth" : "Time (ms)");
        ((CardLayout) limitEditor.getLayout()).show(limitEditor, depth ? "depth" : "time");
        depthSpinner.setEnabled(!searchRunning && depth);
        movetimeSpinner.setEnabled(!searchRunning && !depth);
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this, ApplicationVersion.load().aboutText(), "About SeedV6",
                JOptionPane.INFORMATION_MESSAGE,
                new ScaledImageIcon(new ImageIcon(ApplicationIcons.windowImages().getLast()), 64, 64));
    }

    private void closeWindow() {
        if(closing) return;
        closing = true;
        trainingTimer.stop();
        activityTabs.close();
        whiteEngine.dispose(); blackEngine.dispose(); opponentEngine.dispose();
        setControlsEnabled(false);
        final Runnable trainingCleanup = trainingController.beginShutdown();
        final Runnable corpusCleanup = trainingPanel.beginCorpusShutdown();
        final Runnable arenaCleanup = learningArena.beginShutdown();
        final Runnable cleanup = controller.beginShutdown();
        final Thread shutdown = new Thread(() -> {
            Throwable failure = null;
            for (Runnable task : java.util.List.of(trainingCleanup, corpusCleanup, arenaCleanup, cleanup)) {
                try { task.run(); }
                catch (RuntimeException problem) { if (failure == null) failure = problem; }
            }
            final Throwable outcome = failure;
            SwingUtilities.invokeLater(() -> {
                if (outcome == null) dispose();
                else {
                    closing = false;
                    showError("Shutdown incomplete", TrainingController.concise(outcome));
                }
            });
        }, "seedv6-ui-shutdown");
        shutdown.start();
    }

    private void setControlsEnabled(boolean enabled) {
        newGameButton.setEnabled(enabled);
        loadFenButton.setEnabled(enabled);
        stopButton.setEnabled(enabled && searchRunning);
        stopGameButton.setEnabled(enabled && controller != null && controller.canStopGame());
        resignButton.setVisible(modeBox.getSelectedItem() == GameController.GameMode.HUMAN_VS_ENGINE);
        resignButton.setEnabled(enabled && controller != null && controller.canResign());
        boolean editable = enabled && (controller == null || controller.settingsEditable());
        modeBox.setEnabled(editable);
        humanSideBox.setEnabled(editable && modeBox.getSelectedItem() == GameController.GameMode.HUMAN_VS_ENGINE);
        limitKindBox.setEnabled(editable && !searchRunning);
        depthSpinner.setEnabled(editable && !searchRunning
            && limitKindBox.getSelectedItem() == GameController.LimitKind.DEPTH);
        movetimeSpinner.setEnabled(editable && !searchRunning
            && limitKindBox.getSelectedItem() == GameController.LimitKind.MOVETIME);
        threadsSpinner.setEnabled(editable && !searchRunning);
        boolean engines = modeBox.getSelectedItem() == GameController.GameMode.ENGINE_VS_ENGINE;
        boolean opponent = modeBox.getSelectedItem() == GameController.GameMode.HUMAN_VS_ENGINE
                && evaluatorBox.getSelectedItem() == PlayEvaluator.Mode.BEST_NNUE;
        evaluatorBox.setEnabled(editable && !engines); evaluatorBox.setVisible(!engines); evaluatorLabel.setVisible(!engines);
        participantSetup.setVisible(engines || opponent);
        ((CardLayout) participantSetup.getLayout()).show(participantSetup, engines ? "engines" : "opponent");
        whiteEngine.setEditable(editable); blackEngine.setEditable(editable); opponentEngine.setEditable(editable);
        swapSides.setEnabled(editable && engines && whiteEngine.validSelection() && blackEngine.validSelection());
        startGameButton.setToolTipText(legacyEvaluator == null ? null : "Saved evaluator " + legacyEvaluator + " is historical/unavailable. Choose an evaluator explicitly.");
        startGameButton.setEnabled((engines || evaluatorBox.getSelectedItem() != null) && enabled && controller != null && controller.canStartGame()
                && (!engines || whiteEngine.validSelection() && blackEngine.validSelection())
                && (!opponent || opponentEngine.validSelection()));
        if (controller != null && !controller.gameStarted() && !controller.positionStatus().terminal()) controlState.setText("Prepared");

    }

    private void refreshPlaySetup() {
        if (whiteEngine == null || blackEngine == null || opponentEngine == null) return;
        setControlsEnabled(!closing && !evaluatorChanging);
        if (controller != null) updatePlayers();
        revalidate();
    }

    private static void row(JPanel panel, int row, String title, JComponent field) {
        row(panel, row, SeedTheme.label(title, 12, SeedTheme.SECONDARY), field);
    }

    private static void row(JPanel panel, int row, JLabel title, JComponent field) {
        GridBagConstraints c = new GridBagConstraints(); c.gridy = row; c.gridx = 0;
        c.anchor = GridBagConstraints.WEST; c.insets = new Insets(SeedTheme.scale(3), 0, SeedTheme.scale(3), SeedTheme.scale(10));
        panel.add(title, c); title.setLabelFor(field);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(SeedTheme.scale(3), 0, SeedTheme.scale(3), 0);
        field.setMinimumSize(new Dimension(SeedTheme.scale(130), SeedTheme.scale(30)));
        panel.add(field, c);
    }

    private static void requireEdt() {
        if(!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Swing view mutation must occur on the EDT.");
        }
    }
}
