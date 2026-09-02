package com.ohinteractive.seedv6.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import com.ohinteractive.seedv6.core.move.MoveIntent.Promotion;
import com.ohinteractive.seedv6.search.alphabeta.RootParallelSearch;

/** Modest testing-oriented native Swing window for the V6 engine. */
final class ChessFrame extends JFrame implements GameController.View {

    ChessFrame() {
        super("SeedV6 Engine Harness");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(920, 680));
        setLayout(new BorderLayout(8, 8));

        add(boardPanel, BorderLayout.CENTER);
        add(createControlPanel(), BorderLayout.EAST);
        add(statusLabel, BorderLayout.SOUTH);
        ((javax.swing.JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        controller = new GameController(
            new EngineSearchAdapter(RootParallelSearch.DEFAULT_WORKERS), this
        );
        boardPanel.setInputListener(controller);
        installActions();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                closeWindow();
            }
        });
        pack();
        setLocationByPlatform(true);
        controller.initialize();
    }

    @Override
    public void showPosition(GameController.PositionView position) {
        requireEdt();
        boardPanel.showPosition(position);
        statusLabel.setText(position.status().displayText());
        final StringBuilder moves = new StringBuilder();
        final List<String> coordinates = position.moves();
        for(int index = 0; index < coordinates.size(); index += 2) {
            moves.append(index / 2 + 1).append(". ").append(coordinates.get(index));
            if(index + 1 < coordinates.size()) moves.append("  ").append(coordinates.get(index + 1));
            moves.append(System.lineSeparator());
        }
        moveArea.setText(moves.toString());
        moveArea.setCaretPosition(moveArea.getDocument().getLength());
    }

    @Override
    public void showSearch(GameController.SearchInfo search) {
        requireEdt();
        final String nps = search.nps() < 0L ? "—" : Long.toString(search.nps());
        analysisArea.setText(
            "State: " + search.state() + System.lineSeparator()
                + "Depth: " + (search.depth() == 0 ? "—" : search.depth()) + System.lineSeparator()
                + "Score: " + search.score() + System.lineSeparator()
                + "Nodes: " + search.nodes() + System.lineSeparator()
                + "NPS: " + nps + System.lineSeparator()
                + "Termination: " + search.termination() + System.lineSeparator()
                + "PV: " + search.pv()
        );
    }

    @Override
    public void setSearchRunning(boolean running) {
        requireEdt();
        searchRunning = running;
        stopButton.setEnabled(running);
        threadsSpinner.setEnabled(!running);
        limitKindBox.setEnabled(!running);
        depthSpinner.setEnabled(!running && limitKindBox.getSelectedItem() == GameController.LimitKind.DEPTH);
        movetimeSpinner.setEnabled(!running && limitKindBox.getSelectedItem() == GameController.LimitKind.MOVETIME);
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
    private final JTextArea moveArea = textArea(10, 26);
    private final JTextArea analysisArea = textArea(8, 26);
    private final JButton newGameButton = new JButton("New Game");
    private final JButton loadFenButton = new JButton("Load FEN");
    private final JButton stopButton = new JButton("Stop Search");
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
    private boolean searchRunning;
    private boolean closing;

    private JPanel createControlPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Harness controls"));
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.insets = new Insets(3, 4, 3, 4);

        final JPanel gameButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        gameButtons.add(newGameButton);
        gameButtons.add(loadFenButton);
        panel.add(gameButtons, constraints);
        addRow(panel, constraints, "Mode", modeBox);
        addRow(panel, constraints, "Human side", humanSideBox);
        addRow(panel, constraints, "Limit", limitKindBox);
        addRow(panel, constraints, "Depth", depthSpinner);
        addRow(panel, constraints, "Movetime ms", movetimeSpinner);
        addRow(panel, constraints, "Threads", threadsSpinner);

        constraints.gridy ++;
        constraints.gridwidth = 2;
        stopButton.setEnabled(false);
        panel.add(stopButton, constraints);

        constraints.gridy ++;
        constraints.weighty = 0.45;
        constraints.fill = GridBagConstraints.BOTH;
        panel.add(titledScroll("Move history", moveArea), constraints);
        constraints.gridy ++;
        constraints.weighty = 0.55;
        panel.add(titledScroll("Search", analysisArea), constraints);
        panel.setPreferredSize(new Dimension(300, 640));
        return panel;
    }

    private void installActions() {
        newGameButton.addActionListener(event -> controller.newGame());
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
        depthSpinner.setEnabled(!searchRunning && depth);
        movetimeSpinner.setEnabled(!searchRunning && !depth);
    }

    private void closeWindow() {
        if(closing) return;
        closing = true;
        setControlsEnabled(false);
        final Runnable cleanup = controller.beginShutdown();
        final Thread shutdown = new Thread(() -> {
            try {
                cleanup.run();
            } finally {
                SwingUtilities.invokeLater(this::dispose);
            }
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
    }

    private static void addRow(
        JPanel panel, GridBagConstraints constraints, String label, java.awt.Component component
    ) {
        constraints.gridy ++;
        constraints.gridwidth = 1;
        constraints.gridx = 0;
        constraints.weightx = 0.0;
        panel.add(new JLabel(label), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1.0;
        panel.add(component, constraints);
        constraints.gridx = 0;
        constraints.gridwidth = 2;
    }

    private static JScrollPane titledScroll(String title, JTextArea area) {
        final JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createTitledBorder(title));
        return scroll;
    }

    private static JTextArea textArea(int rows, int columns) {
        final JTextArea area = new JTextArea(rows, columns);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }

    private static void requireEdt() {
        if(!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Swing view mutation must occur on the EDT.");
        }
    }
}
