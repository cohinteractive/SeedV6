package com.ohinteractive.seedv6.gui;

import java.awt.*;
import javax.swing.*;
import static com.ohinteractive.seedv6.gui.TrainingDashboardModel.*;

/** EDT-only operational cards over immutable trainer publications. */
final class TrainingDashboard extends JPanel implements Scrollable {
    private final JLabel title = label("Training", 21, SeedTheme.TEXT);
    private final JLabel subtitle = label("No active run", 12, SeedTheme.SECONDARY);
    private final JLabel state = label("IDLE", 12, SeedTheme.GREEN), elapsed = label("00:00:00", 17, SeedTheme.TEXT);
    private final JLabel generationElapsed = label("\u2014", 17, SeedTheme.TEXT);
    private final CampaignDivider campaign = new CampaignDivider();
    private final JLabel runProgress = label("", 13, SeedTheme.TEXT), timeLimit = label("", 12, SeedTheme.SECONDARY);
    private final JLabel positionMethod = label("Positions: awaiting run", 12, SeedTheme.TEXT);
    private final JLabel validationMethod = label("Validation: awaiting run", 12, SeedTheme.TEXT);
    private final PhaseProgress selfPlay = new PhaseProgress("Position generation / self-play", "selfPlayProgress");
    private final PhaseProgress training = new PhaseProgress("Network training", "networkTrainingProgress");
    private final PhaseProgress validation = new PhaseProgress("Validation", "validationPairProgress");
    private final JLabel matchTitle = label("Candidate vs Best", 14, SeedTheme.TEXT);
    private final JLabel leftCaption = label("Candidate", 12, SeedTheme.SECONDARY), rightCaption = label("Best", 12, SeedTheme.SECONDARY);
    private final JLabel leftMetric = metric("\u2014", SeedTheme.GREEN), rightMetric = metric("\u2014", SeedTheme.TEXT);
    private final JLabel direction = label("Awaiting validation", 12, SeedTheme.SECONDARY);
    private final JLabel decision = label("Awaiting validation", 15, SeedTheme.TEXT);
    private final ValidationWinMetric candidateWins = new ValidationWinMetric("candidateWins", SeedTheme.GREEN);
    private final ValidationWinMetric draws = new ValidationWinMetric("validationDraws", SeedTheme.SECONDARY);
    private final ValidationWinMetric bestWins = new ValidationWinMetric("bestWins", SeedTheme.TEXT);
    private JPanel rightCounterColumn, winsColumn;
    private final JPanel winCounts = panel(new GridLayout(1, 3, SeedTheme.scale(12), 0));
    private final WinIncreases winIncreases = new WinIncreases();
    private final JLabel previousDuration = label("Previous generation \u00b7 unavailable", 11, SeedTheme.SECONDARY);
    private final JLabel[] comparison = new JLabel[5], configuration = new JLabel[6];
    private final JLabel recentNote = label("Last 25 plotted \u00b7 latest validation regime only \u00b7 green = promoted", 11, SeedTheme.SECONDARY);
    private final TrainingHistory.Records recent = new TrainingHistory.Records();
    private final TrainingHistory history = new TrainingHistory();
    private final HistoryChart recentScores = new HistoryChart(true, 160), recentDurations = new HistoryChart(false, 160);
    private com.ohinteractive.seedv6.training.history.HistoryRepository.Snapshot lastHistory;

    TrainingDashboard() {
        super(new GridBagLayout()); setOpaque(false); setName("trainingDashboard");
        title.setFont(SeedTheme.font(21, Font.BOLD)); title.setName("trainingHeadline"); state.setName("trainingState");
        state.setOpaque(true); state.setBackground(SeedTheme.SELECTED); SeedTheme.padding(state, 8, 12, 8, 12);
        JPanel heading = new SeedTheme.Card(); heading.setLayout(new BorderLayout());
        JPanel headingContent = padded(new BorderLayout(0, SeedTheme.scale(6)), 8); heading.add(headingContent);
        JPanel headingBody = panel(new BorderLayout(SeedTheme.scale(10), 0));
        JPanel titles = panel(new BorderLayout(0, SeedTheme.scale(3))); titles.add(title, BorderLayout.NORTH); titles.add(subtitle);
        headingBody.add(titles); JPanel badges = panel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        badges.add(state); badges.add(Box.createHorizontalStrut(SeedTheme.scale(14))); badges.add(value("Campaign elapsed", elapsed));
        badges.add(Box.createHorizontalStrut(SeedTheme.scale(14))); badges.add(value("Generation elapsed", generationElapsed));
        headingBody.add(badges, BorderLayout.EAST); headingContent.add(headingBody, BorderLayout.NORTH);
        headingContent.add(campaign);
        JPanel run = panel(new GridLayout(2, 2, SeedTheme.scale(12), SeedTheme.scale(7)));
        run.add(runProgress); run.add(timeLimit); run.add(positionMethod); run.add(validationMethod);
        timeLimit.setHorizontalAlignment(SwingConstants.RIGHT); validationMethod.setHorizontalAlignment(SwingConstants.RIGHT);
        int halfHeight = Math.max(headingBody.getPreferredSize().height, run.getPreferredSize().height);
        headingBody.setPreferredSize(new Dimension(1, halfHeight)); run.setPreferredSize(new Dimension(1, halfHeight));
        headingContent.add(run, BorderLayout.SOUTH);
        elapsed.setName("campaignElapsed"); generationElapsed.setName("generationElapsed"); previousDuration.setName("previousGenerationDuration");
        runProgress.setName("activeRunProgress"); timeLimit.setName("activeRunTimeLimit");
        positionMethod.setName("effectivePositionMethod"); validationMethod.setName("effectiveValidationMethod");

        JPanel phases = padded(new GridLayout(1, 3, SeedTheme.scale(16), 0), 6);
        phases.add(selfPlay); phases.add(training); phases.add(validation);
        JPanel comparisonBody = padded(new BorderLayout(0, SeedTheme.scale(3)), 6);
        winCounts.add(winCounter("Candidate", candidateWins)); winCounts.add(winCounter("Draws", draws)); winCounts.add(winCounter("Best", bestWins));
        JPanel mainMetrics = panel(new GridBagLayout());
        addCell(mainMetrics, counter(leftCaption, leftMetric, ""), 0, .25);
        addCell(mainMetrics, counter(rightCaption, rightMetric, ""), 1, .25);
        rightCounterColumn = (JPanel) mainMetrics.getComponent(1);
        addCell(mainMetrics, winCounts, 2, .75); winsColumn = (JPanel) mainMetrics.getComponent(2);
        comparisonBody.add(mainMetrics, BorderLayout.NORTH);
        JPanel evidence = panel(new GridBagLayout());
        JPanel verdict = panel(new FlowLayout(FlowLayout.LEFT, SeedTheme.scale(6), 0));
        verdict.add(decision); verdict.add(direction); stack(evidence, verdict, 0, 0);
        for (int i = 0; i < comparison.length; i++) {
            comparison[i] = label("", 11, SeedTheme.SECONDARY); stack(evidence, comparison[i], i + 1, 0);
        }
        stack(evidence, previousDuration, 6, 0);
        comparisonBody.add(evidence);
        leftMetric.setName("candidateScore"); decision.setName("promotionDecision");
        JPanel config = padded(new BorderLayout(), 8);
        JPanel configRows = panel(new GridBagLayout()); config.add(configRows, BorderLayout.NORTH);
        config.setName("effectiveTrainingConfiguration");
        for (int i = 0; i < configuration.length; i++) { configuration[i] = label("\u2014", 12, SeedTheme.TEXT); stack(configRows, configuration[i], i, 4); }
        JPanel comparisons = new ComparisonRow();
        addCell(comparisons, card(null, matchTitle, comparisonBody), 0, .6);
        addCell(comparisons, card("Effective Configuration", null, config), 1, .4);

        JPanel previews = panel(new GridBagLayout()); previews.setName("runtimeGraphs");
        recentScores.setName("comparisonTrend"); recentDurations.setName("generationDuration");
        addCell(previews, card("Comparison trend \u00b7 latest validation regime", null, recentScores), 0, .62);
        addCell(previews, card("Generation duration", null, recentDurations), 1, .38);
        JPanel latest = padded(new BorderLayout(0, SeedTheme.scale(6)), 8); latest.add(recentNote, BorderLayout.NORTH);
        JScrollPane recentScroll = scroll(TrainingHistory.table(recent, "recentTrainingHistory"));
        recentScroll.setPreferredSize(new Dimension(1, SeedTheme.scale(320))); latest.add(recentScroll);
        JComponent[] cards = {heading, card("Current Generation", null, phases), comparisons, previews, card("Recent History", null, latest)};
        GridBagConstraints c = new GridBagConstraints(); c.gridx = 0; c.weightx = 1; c.fill = GridBagConstraints.BOTH;
        for (int i = 0; i < cards.length; i++) {
            c.gridy = i; c.insets = new Insets(0, 0, SeedTheme.scale(6), 0);
            add(widthIndependent(cards[i]), c);
        }
        c.gridy = cards.length; c.weighty = 1; add(Box.createVerticalGlue(), c);
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0) {
                winIncreases.reset(); candidateWins.stopPulse(); bestWins.stopPulse();
            }
        });
    }
    JScrollPane historyView() { return scroll(history); }

    void showState(TrainingController.ViewState view) {
        var s = view.snapshot(); var r = s == null ? null : s.run().orElse(null);
        title.setText(generationTitle(s)); title.setToolTipText(title.getText()); subtitle.setText(r == null ? view.message() : (view.active() ? r.action() : view.startAction()) + " \u00b7 Candidate "
                + network(s.candidateId()) + " \u00b7 Best " + network(s.bestId()));
        subtitle.setToolTipText(view.message());
        state.setText(phase(view)); state.setForeground(view.phase() == TrainingController.Phase.FAILED ? SeedTheme.ERROR : SeedTheme.GREEN);
        elapsed.setText(timer(s == null ? 0 : s.elapsed().toSeconds()));
        generationElapsed.setText(s == null ? "\u2014" : s.generationElapsed(System.nanoTime()).map(d -> timer(d.toSeconds())).orElse("\u2014"));
        runProgress.setText(runLabel(s)); timeLimit.setText(timeLabel(s));
        campaign.showProgress(campaignFraction(s), runLabel(s));
        positionMethod.setText("Positions: " + (r == null ? "awaiting effective configuration" : TrainingComparison.positionMethod(r)));
        validationMethod.setText("Validation: " + (r == null ? "awaiting effective configuration" : TrainingComparison.method(r)));
        selfPlay.show(selfPlay(s, view.settings())); training.show(training(s)); validation.show(validation(s, view.settings()));
        showComparison(s);
        var prior = previousGeneration(view.history().records(), s == null ? 0 : s.generation());
        previousDuration.setText("Previous generation \u00b7 " + (prior == null ? "unavailable" : "Gen " + prior.generation()
                + " \u00b7 " + (prior.totalNanos() == null ? "duration unavailable" : timer(prior.totalNanos() / 1_000_000_000))));
        var pulse = winIncreases.update(s, view.active() && isShowing());
        var m = match(s);
        candidateWins.showCount(m == null ? null : m.wins(), pulse.candidate());
        draws.showCount(m == null ? null : m.draws(), false); bestWins.showCount(m == null ? null : m.losses(), pulse.best());
        String[] config = r == null ? new String[]{"Effective settings appear when the generation starts"}
                : !r.generationSettingsKnown() ? new String[]{"Recovering a legacy Candidate", NetworkArchitecture.valueOf(r.effective().architecture().name()) + " \u00b7 Recorded depth: " + s.trainingDepth(), "Original generation settings were not recorded",
                "Current validation: " + r.effective().validation().openingPairs() + " pairs",
                "Validation depth: " + r.effective().validation().depth() + " \u00b7 Threads: " + r.effective().validation().threads()}
                : new String[]{
                NetworkArchitecture.valueOf(r.effective().architecture().name()) + " \u00b7 Depth: " + r.effective().selfPlay().depth() + " \u00b7 Workers: " + r.effective().selfPlay().threads(),
                count(r.effective().selfPlay().games()) + " games \u00b7 up to " + r.effective().selfPlay().maximumSamplesPerGame() + " samples/game",
                "Epochs: " + r.effective().training().epochs() + " \u00b7 Batch: " + r.effective().training().minibatchSize() + " \u00b7 Openings: " + r.effective().selfPlay().minimumOpeningPlies() + "\u2014" + r.effective().selfPlay().maximumOpeningPlies(),
                r.effective().heldOut(r.source()) ? "Holdout: whole games, about 20% (at least 2)" : "Validation: " + r.effective().validation().openingPairs()
                        + " pairs \u00b7 Depth " + r.effective().validation().depth() + " \u00b7 Threads " + r.effective().validation().threads(),
                r.effective().heldOut(r.source()) ? "Strictly lower loss wins \u00b7 Cap " + r.effective().selfPlay().maximumPlies() : "Promotion margin: " + score(r.effective().validation().policy().requiredMargin()) + " \u00b7 Cap " + r.effective().selfPlay().maximumPlies(),
                r.supervision().blended() ? "Targets: NNUE " + score(r.supervision().teacherWeight()) + " \u00b7 WDL " + score(1 - r.supervision().teacherWeight())
                        : r.effective().heldOut(r.source()) ? "Targets: terminal WDL" : ""};
        if (r != null && r.generationSettingsKnown()
                && r.effective().architecture() == com.ohinteractive.seedv6.training.model.TrainingArchitecture.BRN2)
            config[5] += " | " + r.effective().effectiveCaptureConsistency().description();
        for (int i = 0; i < configuration.length; i++) set(configuration[i], i < config.length ? config[i] : "");
        history.showHistory(view.history(), view.historyWarning());
        if (lastHistory != view.history()) {
            lastHistory = view.history(); var records = lastHistory.records();
            var preview = records.subList(Math.max(0, records.size() - 25), records.size());
            recentScores.showRecords(preview); recentDurations.showRecords(preview);
            recent.show(records.subList(Math.max(0, records.size() - 5), records.size()));
        }
        recentScores.showLive(view.active() ? liveComparison(s) : null);
        recentNote.setText(view.historyWarning().isBlank() && view.history().warnings().isEmpty()
                ? "C = candidate · B = incumbent Best · Δ = C − B · ↓ lower loss is better · ↑ higher score is better"
                : "History warning \u00b7 see History / Diagnostics");
    }
    private void showComparison(com.ohinteractive.seedv6.training.service.TrainerSnapshot s) {
        for (var line : comparison) set(line, "");
        var b = s == null ? null : s.bootstrapValidation().orElse(null);
        var m = match(s);
        boolean heldOut = b != null || s != null && s.run().map(r -> r.effective().heldOut(r.source())).orElse(false);
        winCounts.setVisible(!heldOut); winsColumn.setVisible(!heldOut); rightCounterColumn.setVisible(heldOut);
        matchTitle.setText("Candidate vs Best" + (s != null && ((b != null && !b.candidateId().equals(s.candidateId())) || m != null && !m.current()) ? " \u00b7 latest result" : ""));
        if (heldOut) {
            leftCaption.setText("Candidate loss"); rightCaption.setText("Incumbent Best loss");
            leftMetric.setText(b == null ? "\u2014" : TrainingComparison.loss(b.evidence().comparison().candidateLoss()));
            rightMetric.setText(b == null ? "\u2014" : TrainingComparison.loss(b.evidence().comparison().bestLoss()));
            direction.setText(b == null ? "↓ Lower loss is better" : TrainingComparison.lossName(b.evidence().supervision()) + " ↓ · Δ " + TrainingComparison.delta(b.evidence().comparison()));
            decision.setText(b == null ? "Awaiting validation" : b.evidence().comparison().decision() == com.ohinteractive.seedv6.training.validation.PromotionPolicy.Decision.PROMOTE
                    ? s.bestId().equals(b.candidateId()) ? "PROMOTED" : "Promotion publication pending" : "BEST RETAINED");
            if (b != null) {
                var e = b.evidence();
                set(comparison[0], e.comparison().samples() + " held-out samples \u00b7 strict lower configured loss wins; ties retain Best");
                if (e.wdlLoss() != null) {
                    set(comparison[1], TrainingComparison.pair("WDL", e.wdlLoss()));
                    set(comparison[2], TrainingComparison.pair("NNUE", e.teacherLoss()));
                    set(comparison[3], "Decision uses configured target loss above \u00b7 " + e.supervision().description());
                }
                set(comparison[4], "Candidate " + network(b.candidateId()) + " \u00b7 Incumbent " + network(b.incumbentId()));
                comparison[4].setToolTipText(b.candidateId() + " / " + b.incumbentId());
            }
        } else {
            leftCaption.setText("Score"); rightCaption.setText("Promotion lower bound");
            leftMetric.setText(m == null ? "\u2014" : m.score());
            rightMetric.setText(s == null || s.assessment().isEmpty() ? "\u2014" : score(s.assessment().get().lowerBound()));
            direction.setText("\u2191 Higher is better \u00b7 valid pairs only");
            decision.setText(m == null ? "Awaiting validation" : m.decision());
            if (m != null) {
                long finishedGames = s.validation().map(v -> v.terminations().entrySet().stream()
                        .filter(e -> e.getKey().completed() || e.getKey() == com.ohinteractive.seedv6.training.selfplay.GameTermination.PLY_CAP)
                        .mapToLong(java.util.Map.Entry::getValue).sum()).orElseGet(() -> (long) s.validationProgress().orElseThrow().completedGames());
                set(comparison[1], "Games: " + finishedGames + " / " + (2 * m.configuredPairs()) + " \u00b7 Valid pairs: " + m.pairs() + " / " + m.configuredPairs());
                set(comparison[2], m.detail());
                set(comparison[3], "Promotion lower bound: " + rightMetric.getText());
                set(comparison[0], "Candidate Gen " + m.candidate() + " \u00b7 Best Gen " + m.incumbent());
                comparison[0].setToolTipText(m.candidateId() + " / " + m.incumbentId());
            }
        }
        decision.setForeground(decision.getText().equals("PROMOTED") ? SeedTheme.GREEN : SeedTheme.TEXT);
    }
    private static void set(JLabel label, String text) { label.setText(text); label.setToolTipText(text); label.setVisible(!text.isEmpty()); }
    private static void stack(JPanel parent, JComponent child, int row, int gap) {
        GridBagConstraints c = new GridBagConstraints(); c.gridx = 0; c.gridy = row; c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL; c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(0, 0, SeedTheme.scale(gap), 0); parent.add(child, c);
    }
    static JPanel card(String title, JLabel heading, JComponent body) {
        JPanel card = new SeedTheme.Card(); card.setLayout(new BorderLayout());
        JPanel header = panel(new BorderLayout());
        JLabel label = heading == null ? label(title, 14, SeedTheme.TEXT) : heading;
        label.setFont(SeedTheme.font(14, Font.BOLD)); header.add(label);
        header.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, SeedTheme.LINE),
                BorderFactory.createEmptyBorder(SeedTheme.scale(3), SeedTheme.scale(12), SeedTheme.scale(3), SeedTheme.scale(12))));
        card.add(header, BorderLayout.NORTH); card.add(body); return card;
    }
    static JPanel padded(LayoutManager layout, int pad) { JPanel p = panel(layout); SeedTheme.padding(p, pad, pad, pad, pad); return p; }
    static JPanel panel(LayoutManager layout) { return SeedTheme.panel(layout); }
    static JLabel label(String text, int size, Color color) { return SeedTheme.label(text, size, color); }
    static JTextArea text(String value, int size, Color color) {
        JTextArea text = new JTextArea(value); text.setEditable(false); text.setOpaque(false); text.setLineWrap(true); text.setWrapStyleWord(true);
        text.setFont(SeedTheme.font(size, Font.PLAIN)); text.setForeground(color); text.setFocusable(false); text.setBorder(null);
        // These are passive labels, not log editors. Caret updates must never scroll the dashboard.
        ((javax.swing.text.DefaultCaret) text.getCaret()).setUpdatePolicy(javax.swing.text.DefaultCaret.NEVER_UPDATE);
        return text;
    }
    static JScrollPane scroll(JComponent body) {
        JScrollPane scroll = new JScrollPane(body); scroll.setBorder(BorderFactory.createEmptyBorder());
        if (body instanceof JTable table) scroll.setColumnHeaderView(table.getTableHeader());
        scroll.setOpaque(false); scroll.getViewport().setOpaque(false); scroll.getVerticalScrollBar().setUnitIncrement(SeedTheme.scale(24)); return scroll;
    }
    private static JPanel value(String caption, JLabel value) {
        JPanel p = panel(new BorderLayout(0, SeedTheme.scale(3))); p.add(label(caption, 11, SeedTheme.SECONDARY), BorderLayout.NORTH); p.add(value); return p;
    }
    private static JPanel winCounter(String caption, JLabel metric) {
        JLabel title = label(caption, 12, metric.getForeground()); title.setHorizontalAlignment(SwingConstants.CENTER);
        metric.getAccessibleContext().setAccessibleName(caption + (caption.equals("Draws") ? "" : " wins"));
        JPanel p = panel(new BorderLayout()); p.add(title, BorderLayout.NORTH); p.add(metric); return p;
    }
    private static JLabel metric(String value, Color color) { JLabel l = label(value, 32, color); l.setFont(SeedTheme.font(32, Font.BOLD)); return l; }
    private static JPanel counter(JLabel title, JLabel metric, String caption) {
        JPanel p = panel(new BorderLayout(0, SeedTheme.scale(2))); p.add(title, BorderLayout.NORTH); p.add(metric); if (!caption.isEmpty()) p.add(label(caption, 10, SeedTheme.SECONDARY), BorderLayout.SOUTH); return p;
    }
    private static void addCell(JPanel parent, JComponent child, int column, double weight) {
        GridBagConstraints c = new GridBagConstraints(); c.gridx = column; c.weightx = weight; c.weighty = 1; c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, column == 0 ? 0 : SeedTheme.scale(10), 0, 0); child.setMinimumSize(new Dimension(0, 0));
        parent.add(widthIndependent(child), c);
    }
    // Long labels must not force GridBagLayout to shrink vertical content at enlarged display scales.
    private static JPanel widthIndependent(JComponent child) {
        JPanel columnPanel = new JPanel(new BorderLayout()) {
            @Override public Dimension getPreferredSize() { return new Dimension(1, super.getPreferredSize().height); }
            @Override public Dimension getMinimumSize() { return new Dimension(0, getPreferredSize().height); }
        };
        columnPanel.setOpaque(false); columnPanel.add(child); return columnPanel;
    }
    /** At enlarged display scales the two detail cards stack instead of clipping their values. */
    private static final class ComparisonRow extends JPanel {
        private boolean stacked;
        ComparisonRow() { super(new GridBagLayout()); setOpaque(false); }
        private boolean updateColumns() {
            int width = getWidth() > 0 ? getWidth() : getParent() == null ? 0 : getParent().getWidth();
            boolean next = width > 0 && width < SeedTheme.scale(700);
            if (next == stacked) return false;
            stacked = next; var layout = (GridBagLayout) getLayout();
            for (int i = 0; i < getComponentCount(); i++) {
                var c = layout.getConstraints(getComponent(i));
                c.gridx = stacked ? 0 : i; c.gridy = stacked ? i : 0;
                c.weightx = stacked ? 1 : i == 0 ? .6 : .4;
                c.insets = new Insets(stacked && i > 0 ? SeedTheme.scale(6) : 0, stacked || i == 0 ? 0 : SeedTheme.scale(10), 0, 0);
                layout.setConstraints(getComponent(i), c);
            }
            return true;
        }
        @Override public Dimension getPreferredSize() { updateColumns(); return super.getPreferredSize(); }
        @Override public void doLayout() { if (updateColumns()) revalidate(); super.doLayout(); }
    }

    private static final class PhaseProgress extends JPanel {
        private final JLabel value = label("—", 12, SeedTheme.TEXT), detail = label("—", 10, SeedTheme.SECONDARY);
        private final JProgressBar bar = new JProgressBar(0, 100);
        private final JLabel percent = label("—", 10, SeedTheme.SECONDARY);
        PhaseProgress(String title, String name) {
            super(new BorderLayout(0, SeedTheme.scale(3))); setOpaque(false);
            JPanel labels = panel(new GridBagLayout());
            stack(labels, label(title, 12, SeedTheme.TEXT), 0, 2); stack(labels, value, 1, 2); stack(labels, detail, 2, 0); add(labels);
            JPanel progress = panel(new BorderLayout(SeedTheme.scale(8), 0)); bar.setName(name); bar.setForeground(SeedTheme.GREEN);
            bar.setPreferredSize(new Dimension(1, SeedTheme.scale(8))); progress.add(bar); progress.add(percent, BorderLayout.EAST); add(progress, BorderLayout.SOUTH);
        }
        void show(Progress p) {
            value.setText(p.value()); detail.setText(p.detail()); value.setToolTipText(p.value()); detail.setToolTipText(p.detail());
            bar.setValue(Math.max(0, p.percent())); bar.setVisible(p.percent() >= 0); percent.setText(p.percent() < 0 ? "Counted at optimizer boundaries" : p.percent() + "%");
            bar.getAccessibleContext().setAccessibleName(p.value());
        }
    }
    public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return SeedTheme.scale(24); }
    public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return Math.max(1, r.height - SeedTheme.scale(24)); }
    public boolean getScrollableTracksViewportWidth() { return true; }
    public boolean getScrollableTracksViewportHeight() { return getParent() != null && getParent().getHeight() >= getPreferredSize().height; }
}
