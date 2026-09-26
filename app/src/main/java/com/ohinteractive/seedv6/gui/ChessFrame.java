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
import javax.swing.Scrollable;
import javax.swing.Icon;
import java.util.function.Consumer;

import com.ohinteractive.seedv6.core.move.MoveIntent.Promotion;
import com.ohinteractive.seedv6.search.alphabeta.RootParallelSearch;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.formdev.flatlaf.util.ScaledImageIcon;
import com.ohinteractive.seedv6.ApplicationVersion;

/** Desktop shell; controller lifecycles remain independent of workspace navigation. */
final class ChessFrame extends JFrame implements GameController.View {

    ChessFrame() {
        this(TrainingSettings.load(TrainingSettings.preferences()), new TrainingController.Backend(),
                settings -> settings.saveConfiguration(TrainingSettings.preferences()), new TrainingFolders(TrainingSettings.preferences()),
                TrainingSettings.preferences().node("play"));
    }

    ChessFrame(TrainingSettings settings, TrainingController.Backend backend, Consumer<TrainingSettings> persist) {
        this(settings, backend, persist, new TrainingFolders(settings), null);
    }

    private ChessFrame(TrainingSettings settings, TrainingController.Backend backend, Consumer<TrainingSettings> persist,
                       TrainingFolders folders, java.util.prefs.Preferences playPreferences) {
        super(SeedTheme.initialize());
        setIconImages(ApplicationIcons.windowImages());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setJMenuBar(ApplicationMenu.create(this::closeWindow, this::showAbout));
        Rectangle usable = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setMinimumSize(new Dimension(Math.min(SeedTheme.scale(1100), usable.width), Math.min(SeedTheme.scale(760), usable.height)));
        setLayout(new BorderLayout());
        getContentPane().setBackground(SeedTheme.BACKGROUND);
        depthSpinner.setName("playDepth"); threadsSpinner.setName("playThreads");
        threadsSpinner.setToolTipText("Search currently uses one thread; larger limits are retained for compatibility.");
        evaluatorBox.setName("playEvaluator"); humanSideBox.setName("humanSide"); modeBox.setName("gameMode");
        pinnedLabel.setName("pinnedBest");
        whiteDetail.setName("whitePlayerNetwork"); blackDetail.setName("blackPlayerNetwork");
        controlHint.setName("playControlHint");
        limitKindBox.setName("searchLimit"); movetimeSpinner.setName("playMovetime");
        newGameButton.setName("newGame"); stopButton.setName("stopSearch"); loadFenButton.setName("loadFen");

        whiteEngine = new PlayEnginePanel("white", settings.root(), playPreferences == null ? null : playPreferences.node("white"), this::refreshPlaySetup);
        blackEngine = new PlayEnginePanel("black", settings.root(), playPreferences == null ? null : playPreferences.node("black"), this::refreshPlaySetup);
        trainingPanel = new TrainingPanel(settings, folders);
        final JTabbedPane tabs = new JTabbedPane();
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
        add(tabs, BorderLayout.CENTER);
        JPanel status = new JPanel(new BorderLayout(12, 0));
        status.setBackground(SeedTheme.PANEL);
        status.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, SeedTheme.LINE),
                BorderFactory.createEmptyBorder(SeedTheme.scale(6), SeedTheme.scale(20), SeedTheme.scale(6), SeedTheme.scale(20))));
        status.add(SeedTheme.label("SeedV6   |   Java Chess Engine", 11, SeedTheme.SECONDARY), BorderLayout.WEST);
        statusLabel.setFont(SeedTheme.font(11, Font.PLAIN)); statusLabel.setForeground(SeedTheme.SECONDARY);
        status.add(statusLabel, BorderLayout.EAST); add(status, BorderLayout.SOUTH);

        controller = new GameController(
            new EngineSearchAdapter(RootParallelSearch.DEFAULT_WORKERS), this
        );
        controller.setCheckpointRoot(settings.root());
        // Sibling owners: neither controller receives the other's stop/reset/search lifecycle.
        trainingController = new TrainingController(settings, backend, persist,
                this::showTraining);
        trainingPanel.bind(trainingController);
        trainingBoard.showState(trainingController.state());
        tabs.addChangeListener(event -> {
            trainingSelected = tabs.getSelectedIndex() == 1;
            if (tabs.getSelectedIndex() == 1 && !trainingLayoutInitialized) {
                trainingLayoutInitialized = true;
                SwingUtilities.invokeLater(() -> { trainingSplit.setDividerLocation(.445); trainingPanel.showDashboardTop(); });
            }
            statusLabel.setText(tabs.getSelectedIndex() == 1
                    ? "Network Training · " + TrainingDashboardModel.phase(trainingController.state()) : controller.positionStatus().displayText());
        });
        trainingTimer = new Timer(500, event -> trainingController.poll());
        trainingTimer.start();
        boardPanel.setInputListener(controller);
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
    }

    @Override
    public void showPosition(GameController.PositionView position) {
        requireEdt();
        boardPanel.showPosition(position);
        if (!trainingSelected) statusLabel.setText(position.status().displayText());
        moves.showPosition(position);
        updatePlayers();
    }

    @Override
    public void showSearch(GameController.SearchInfo search) {
        requireEdt();
        boardPanel.showScore(engineCard.showSearch(search, participants.forSide(search.scoreSide())), nnueActive);
        controlState.setText(search.state().equals("Idle") ? "●  Ready" : "●  " + search.state());
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
        PlayEvaluator evaluator = bindings.white();
        evaluatorChanging = changing;
        nnueActive = evaluator.mode() == PlayEvaluator.Mode.BEST_NNUE;
        updatingEvaluator = true;
        evaluatorBox.setSelectedItem(evaluator.mode());
        updatingEvaluator = false;
        updatePlayers();
        setControlsEnabled(!closing && !evaluatorChanging);
    }

    private void showTraining(TrainingController.ViewState state) {
        requireEdt();
        controller.setCheckpointRoot(state.settings().root());
        trainingPanel.showState(state);
        trainingBoard.showState(state);
        if (trainingSelected) statusLabel.setText("Network Training · " + TrainingDashboardModel.phase(state));
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
    private final JLabel statusLabel = new JLabel(" ");
    private final MoveScoresheet moves = new MoveScoresheet();
    private final EngineCard engineCard = new EngineCard();
    private final JLabel blackIdentity = SeedTheme.label("SeedV6 (Engine)", 17, SeedTheme.TEXT);
    private final JLabel whiteIdentity = SeedTheme.label("You (Human)", 17, SeedTheme.TEXT);
    private final JLabel blackDetail = SeedTheme.label("Handcrafted evaluator", 12, SeedTheme.SECONDARY);
    private final JLabel whiteDetail = SeedTheme.label("White", 12, SeedTheme.SECONDARY);
    private final JLabel controlState = SeedTheme.label("●  Ready", 12, SeedTheme.GREEN);
    private final JLabel controlHint = SeedTheme.label("Engine turns start automatically", 11, SeedTheme.MUTED);
    private final JLabel blackBadge = new JLabel(), whiteBadge = new JLabel();
    private final JLabel limitLabel = SeedTheme.label("Depth", 12, SeedTheme.SECONDARY);
    private final JPanel limitEditor = SeedTheme.panel(new CardLayout());
    private PlayParticipants participants = PlayParticipants.shared(PlayEvaluator.handcrafted());
    private final PlayEnginePanel whiteEngine, blackEngine;
    private final JPanel engineSetup = SeedTheme.panel(new GridLayout(1, 2, SeedTheme.scale(16), 0));
    private final JLabel evaluatorLabel = SeedTheme.label("Evaluator", 12, SeedTheme.SECONDARY);
    private JPanel boardCard;
    private JSplitPane playSplit, trainingSplit;
    private final JButton newGameButton = new JButton("New Game");
    private final JButton loadFenButton = new JButton("Load FEN");
    private final JButton stopButton = new JButton("Stop Search");
    private final JComboBox<PlayEvaluator.Mode> evaluatorBox = new JComboBox<>(PlayEvaluator.Mode.values());
    private final JLabel pinnedLabel = new JLabel("Handcrafted evaluator");
    private final JComboBox<GameController.GameMode> modeBox = new JComboBox<>(GameController.GameMode.values());
    private final JComboBox<GameController.HumanSide> humanSideBox = new JComboBox<>(GameController.HumanSide.values());
    private final JComboBox<GameController.LimitKind> limitKindBox = new JComboBox<>(GameController.LimitKind.values());
    private final JSpinner depthSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 256, 1));
    private final JSpinner movetimeSpinner = new JSpinner(new SpinnerNumberModel(1_000L, 50L, 600_000L, 50L));
    private final JSpinner threadsSpinner = new JSpinner(new SpinnerNumberModel(
        RootParallelSearch.DEFAULT_WORKERS,
        RootParallelSearch.MIN_WORKERS,
        RootParallelSearch.MAX_WORKERS,
        1
    ));
    private final GameController controller;
    private final TrainingController trainingController;
    private final TrainingPanel trainingPanel;
    private final TrainingBoard trainingBoard;
    private final Timer trainingTimer;
    private boolean searchRunning;
    private boolean closing;
    private boolean evaluatorChanging, updatingEvaluator, nnueActive;
    private boolean trainingLayoutInitialized, trainingSelected;

    private JPanel createPlayWorkspace() {
        JPanel workspace = new JPanel(new BorderLayout()); workspace.setBackground(SeedTheme.BACKGROUND);
        SeedTheme.padding(workspace, 16, 20, 14, 20);
        JPanel board = new SeedTheme.Card(); board.setLayout(new BorderLayout());
        boardCard = board;
        board.add(player(blackBadge, blackIdentity, blackDetail, "BLACK"), BorderLayout.NORTH);
        board.add(boardPanel, BorderLayout.CENTER);
        board.add(player(whiteBadge, whiteIdentity, whiteDetail, "WHITE"), BorderLayout.SOUTH);
        board.setMinimumSize(new Dimension(SeedTheme.scale(400), 0));
        JPanel cards = new WorkspaceCards();
        GridBagConstraints c = new GridBagConstraints(); c.gridx = 0; c.weightx = 1; c.fill = GridBagConstraints.BOTH;
        JPanel game = createGameCard(), controls = createControls();
        engineCard.setMinimumSize(engineCard.getPreferredSize());
        controls.setMinimumSize(controls.getPreferredSize());
        c.gridy = 0; c.insets = new Insets(0, 0, SeedTheme.scale(12), 0); cards.add(game, c);
        c.gridy = 1; cards.add(engineCard, c);
        c.gridy = 2; c.weighty = 1; cards.add(moves, c);
        c.gridy = 3; c.weighty = 0; c.insets = new Insets(0, 0, 0, 0); cards.add(controls, c);
        cards.setPreferredSize(new Dimension(SeedTheme.scale(600), SeedTheme.scale(800)));
        JScrollPane cardScroll = new JScrollPane(cards, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        cardScroll.setBorder(BorderFactory.createEmptyBorder()); cardScroll.setOpaque(false); cardScroll.getViewport().setOpaque(false);
        cardScroll.setMinimumSize(new Dimension(SeedTheme.scale(560), 0));
        cardScroll.getVerticalScrollBar().setUnitIncrement(SeedTheme.scale(24));
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, board, cardScroll);
        playSplit = split;
        split.setName("playSplit"); split.setBorder(BorderFactory.createEmptyBorder());
        split.setOpaque(false); split.setDividerSize(SeedTheme.scale(18)); split.setContinuousLayout(true);
        split.setResizeWeight(0.52); split.setDividerLocation(SeedTheme.scale(720));
        workspace.add(split); return workspace;
    }

    private static final class WorkspaceCards extends JPanel implements Scrollable {
        WorkspaceCards() { super(new GridBagLayout()); setOpaque(false); }
        @Override public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize(); size.height = Math.max(size.height, getMinimumSize().height); return size;
        }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) { return SeedTheme.scale(24); }
        public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) { return r.height - SeedTheme.scale(24); }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return getParent() != null && getParent().getHeight() >= getMinimumSize().height; }
    }

    private JPanel createGameCard() {
        JPanel form = SeedTheme.panel(new GridLayout(1, 2, SeedTheme.scale(20), 0));
        SeedTheme.padding(form, 8, 16, 12, 16);
        JPanel left = SeedTheme.panel(new GridBagLayout()), right = SeedTheme.panel(new GridBagLayout());
        row(left, 0, "Mode", modeBox); row(left, 1, "Human side", humanSideBox); row(left, 2, evaluatorLabel, evaluatorBox);
        pinnedLabel.setFont(SeedTheme.font(11, Font.PLAIN)); pinnedLabel.setForeground(SeedTheme.SECONDARY);
        pinnedLabel.setMinimumSize(new Dimension(0, SeedTheme.scale(26)));
        row(left, 3, "Network", pinnedLabel);
        limitEditor.add(depthSpinner, "depth"); limitEditor.add(movetimeSpinner, "time");
        row(right, 0, "Search limit", limitKindBox); row(right, 1, limitLabel, limitEditor); row(right, 2, "Threads", threadsSpinner);
        form.add(left); form.add(right);
        newGameButton.putClientProperty("FlatLaf.style", "background: #218f59; foreground: #ffffff; hoverBackground: #29a568; pressedBackground: #187547");
        engineSetup.add(whiteEngine); engineSetup.add(blackEngine); engineSetup.setVisible(false);
        SeedTheme.padding(engineSetup, 0, 16, 12, 16);
        var body = SeedTheme.panel(new BorderLayout()); body.add(form, BorderLayout.NORTH); body.add(engineSetup);
        JPanel card = new JPanel(new BorderLayout()) {
            @Override public Dimension getMinimumSize() { return new Dimension(0, getPreferredSize().height); }
        };
        card.setOpaque(false); card.add(SeedTheme.card("Game", newGameButton, body)); return card;
    }

    private JPanel createControls() {
        JPanel body = SeedTheme.panel(new BorderLayout(SeedTheme.scale(12), 0)); SeedTheme.padding(body, 10, 16, 12, 16);
        JPanel buttons = SeedTheme.panel(new FlowLayout(FlowLayout.LEFT, SeedTheme.scale(8), 0));
        buttons.add(loadFenButton); buttons.add(stopButton); body.add(buttons, BorderLayout.WEST);
        body.add(controlState, BorderLayout.EAST);
        stopButton.setEnabled(false);
        return SeedTheme.card("Controls", controlHint, body);
    }

    private static JPanel identity() {
        JPanel panel = SeedTheme.panel(new FlowLayout(FlowLayout.LEFT, SeedTheme.scale(10), SeedTheme.scale(8)));
        SeedTheme.padding(panel, 0, 12, 0, 20);
        panel.add(new JLabel(new ScaledImageIcon(new ImageIcon(ApplicationIcons.windowImages().getLast()), 32, 32)));
        JLabel name = SeedTheme.label("SeedV6", 17, SeedTheme.TEXT); name.setFont(SeedTheme.font(17, Font.BOLD)); panel.add(name);
        return panel;
    }

    private static JPanel player(JLabel badge, JLabel name, JLabel detail, String side) {
        JPanel row = SeedTheme.panel(new BorderLayout(SeedTheme.scale(14), 0)); SeedTheme.padding(row, 8, 24, 8, 24);
        badge.setPreferredSize(new Dimension(SeedTheme.scale(40), SeedTheme.scale(40)));
        row.add(badge, BorderLayout.WEST);
        JPanel labels = SeedTheme.panel(new GridLayout(2, 1, 0, SeedTheme.scale(4)));
        name.setFont(SeedTheme.font(17, Font.BOLD)); labels.add(name); labels.add(detail); row.add(labels);
        row.add(SeedTheme.label(side, 11, SeedTheme.MUTED), BorderLayout.EAST); return row;
    }

    private void updatePlayers() {
        GameController.GameMode mode = (GameController.GameMode) modeBox.getSelectedItem();
        boolean humanWhite = humanSideBox.getSelectedItem() == GameController.HumanSide.WHITE;
        boolean blackEngine = mode == GameController.GameMode.ENGINE_VS_ENGINE || mode == GameController.GameMode.HUMAN_VS_ENGINE && humanWhite;
        boolean whiteEngine = mode == GameController.GameMode.ENGINE_VS_ENGINE || mode == GameController.GameMode.HUMAN_VS_ENGINE && !humanWhite;
        // A failed mode change retains the previous bindings, which may still be distinct.
        PlayEvaluator displayed = mode == GameController.GameMode.HUMAN_VS_ENGINE && humanWhite ? participants.black() : participants.white();
        pinnedLabel.setText(evaluatorChanging ? "Loading evaluator…" : nnueActive
                ? (participants.selection().equals(PlayParticipants.Selection.BEST) ? "Pinned best: " : "Pinned network: ")
                    + PlayEvaluator.shortId(displayed.checkpointId()) : "Handcrafted evaluator");
        pinnedLabel.setToolTipText(nnueActive ? displayed.identity() : null);
        blackIdentity.setText(blackEngine ? "SeedV6 (Engine)" : mode == GameController.GameMode.HUMAN_VS_HUMAN ? "Black (Human)" : "You (Human)");
        whiteIdentity.setText(whiteEngine ? "SeedV6 (Engine)" : mode == GameController.GameMode.HUMAN_VS_HUMAN ? "White (Human)" : "You (Human)");
        blackDetail.setText(blackEngine ? participants.black().description() : "Black pieces");
        whiteDetail.setText(whiteEngine ? participants.white().description() : "White pieces");
        blackDetail.setToolTipText(blackEngine ? participants.black().identity() : null);
        whiteDetail.setToolTipText(whiteEngine ? participants.white().identity() : null);
        blackBadge.setIcon(blackEngine ? ENGINE_BADGE : HUMAN_BADGE);
        whiteBadge.setIcon(whiteEngine ? ENGINE_BADGE : HUMAN_BADGE);
        controlHint.setText(mode == GameController.GameMode.HUMAN_VS_HUMAN ? "Move pieces on the board"
                : mode == GameController.GameMode.ENGINE_VS_ENGINE ? "Configure both engines, then press Start Game" : "Engine turns start automatically");
        controlHint.setToolTipText(null);
    }

    private static final Icon ENGINE_BADGE = new ScaledImageIcon(new ImageIcon(ApplicationIcons.windowImages().getLast()), 40, 40);
    private static final Icon HUMAN_BADGE = new Icon() {
        public int getIconWidth() { return SeedTheme.scale(40); }
        public int getIconHeight() { return SeedTheme.scale(40); }
        public void paintIcon(Component c, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create(); g.translate(x, y);
            g.scale(getIconWidth() / 40.0, getIconHeight() / 40.0);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(SeedTheme.INSET); g.fillRoundRect(0, 0, 39, 39, 8, 8);
            g.setColor(SeedTheme.LINE); g.drawRoundRect(0, 0, 39, 39, 8, 8);
            g.setColor(SeedTheme.SECONDARY); g.fillOval(14, 8, 12, 12); g.fillArc(8, 23, 24, 20, 0, 180); g.dispose();
        }
    };

    private void installActions() {
        newGameButton.addActionListener(event -> {
            if (modeBox.getSelectedItem() == GameController.GameMode.ENGINE_VS_ENGINE) {
                if (whiteEngine.validSelection() && blackEngine.validSelection()) controller.startEngineGame(
                        new PlayParticipants.Selection(whiteEngine.selectedId(), blackEngine.selectedId(),
                                whiteEngine.selectedRoot(), blackEngine.selectedRoot()));
            } else controller.newGame();
        });
        evaluatorBox.addActionListener(event -> {
            if (!updatingEvaluator && modeBox.getSelectedItem() != GameController.GameMode.ENGINE_VS_ENGINE) {
                PlayEvaluator.Mode selected = (PlayEvaluator.Mode) evaluatorBox.getSelectedItem();
                if (selected == PlayEvaluator.Mode.BEST_NNUE && !trainingController.state().active()
                        && !trainingPanel.applySettings()) {
                    showEvaluator(controller.evaluator(), false); return;
                }
                controller.changeEvaluator(selected, false);
            }
        });
        loadFenButton.addActionListener(event -> {
            final String fen = JOptionPane.showInputDialog(
                this, "Enter a complete six-field FEN:", "Load FEN",
                JOptionPane.QUESTION_MESSAGE
            );
            if(fen != null) controller.loadFen(fen);
        });
        stopButton.addActionListener(event -> controller.stopSearch());
        modeBox.addActionListener(event -> {
            final GameController.GameMode mode = (GameController.GameMode) modeBox.getSelectedItem();
            humanSideBox.setEnabled(mode == GameController.GameMode.HUMAN_VS_ENGINE);
            controller.setGameMode(mode);
            refreshPlaySetup();
        });
        humanSideBox.addActionListener(event -> controller.setHumanSide(
            (GameController.HumanSide) humanSideBox.getSelectedItem()
        ));
        limitKindBox.addActionListener(event -> {
            updateLimitControlState();
            applySearchSettings();
        });
        depthSpinner.addChangeListener(event -> applySearchSettings());
        movetimeSpinner.addChangeListener(event -> applySearchSettings());
        threadsSpinner.addChangeListener(event -> controller.setWorkerCount(
            ((Number) threadsSpinner.getValue()).intValue()
        ));
        updateLimitControlState();
    }

    private void applySearchSettings() {
        controller.setSearchSettings(new GameController.SearchSettings(
            (GameController.LimitKind) limitKindBox.getSelectedItem(),
            ((Number) depthSpinner.getValue()).intValue(),
            ((Number) movetimeSpinner.getValue()).longValue()
        ));
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
        whiteEngine.dispose(); blackEngine.dispose();
        setControlsEnabled(false);
        final Runnable trainingCleanup = trainingController.beginShutdown();
        final Runnable cleanup = controller.beginShutdown();
        final Thread shutdown = new Thread(() -> {
            Throwable failure = null;
            try {
                trainingCleanup.run();
            } catch (RuntimeException problem) {
                failure = problem;
            } finally {
                try { cleanup.run(); }
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
        modeBox.setEnabled(enabled);
        humanSideBox.setEnabled(enabled && modeBox.getSelectedItem() == GameController.GameMode.HUMAN_VS_ENGINE);
        limitKindBox.setEnabled(enabled && !searchRunning);
        depthSpinner.setEnabled(enabled && !searchRunning
            && limitKindBox.getSelectedItem() == GameController.LimitKind.DEPTH);
        movetimeSpinner.setEnabled(enabled && !searchRunning
            && limitKindBox.getSelectedItem() == GameController.LimitKind.MOVETIME);
        threadsSpinner.setEnabled(enabled && !searchRunning);
        boolean engines = modeBox.getSelectedItem() == GameController.GameMode.ENGINE_VS_ENGINE;
        evaluatorBox.setEnabled(enabled && !engines); evaluatorBox.setVisible(!engines); evaluatorLabel.setVisible(!engines);
        engineSetup.setVisible(engines);
        whiteEngine.setEditable(enabled); blackEngine.setEditable(enabled);
        newGameButton.setText(engines ? "Start Game" : "New Game");
        newGameButton.setName(engines ? "startGame" : "newGame");
        newGameButton.setEnabled(enabled && (!engines || whiteEngine.validSelection() && blackEngine.validSelection()));
    }

    private void refreshPlaySetup() {
        if (whiteEngine == null || blackEngine == null) return;
        setControlsEnabled(!closing && !evaluatorChanging); revalidate();
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
