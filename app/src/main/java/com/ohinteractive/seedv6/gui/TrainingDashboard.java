package com.ohinteractive.seedv6.gui;

import java.awt.*;
import javax.swing.*;
import static com.ohinteractive.seedv6.gui.TrainingDashboardModel.*;

/** EDT-only operational cards over immutable trainer publications. */
final class TrainingDashboard extends JPanel implements Scrollable {
    private final JLabel title = label("Training", 21, SeedTheme.TEXT);
    private final JLabel subtitle = label("No active run", 12, SeedTheme.SECONDARY);
    private final JLabel state = label("IDLE", 12, SeedTheme.GREEN), elapsed = label("00:00:00", 17, SeedTheme.TEXT);
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
    private final JLabel[] comparison = new JLabel[5], configuration = new JLabel[7];
    private final JLabel recentNote = label("Last 25 plotted \u00b7 latest validation regime only \u00b7 green = promoted", 11, SeedTheme.SECONDARY);
    private final TrainingHistory.Records recent = new TrainingHistory.Records();
    private final TrainingHistory history = new TrainingHistory();
    private final HistoryChart recentScores = new HistoryChart(true, 120), recentDurations = new HistoryChart(false, 120);
    private com.ohinteractive.seedv6.training.history.HistoryRepository.Snapshot lastHistory;

    TrainingDashboard() {
        super(new GridBagLayout()); setOpaque(false); setName("trainingDashboard");
        title.setFont(SeedTheme.font(21, Font.BOLD)); title.setName("trainingHeadline"); state.setName("trainingState");
        state.setOpaque(true); state.setBackground(SeedTheme.SELECTED); SeedTheme.padding(state, 8, 12, 8, 12);
        JPanel heading = new SeedTheme.Card(); heading.setLayout(new BorderLayout(0, SeedTheme.scale(8)));
        JPanel headingBody = padded(new BorderLayout(SeedTheme.scale(10), 0), 10);
        JPanel titles = panel(new BorderLayout(0, SeedTheme.scale(3))); titles.add(title); titles.add(subtitle, BorderLayout.SOUTH);
        headingBody.add(titles); JPanel badges = panel(new FlowLayout(FlowLayout.RIGHT, SeedTheme.scale(14), 0));
        badges.add(state); badges.add(value("Run elapsed", elapsed)); headingBody.add(badges, BorderLayout.EAST); heading.add(headingBody);
        JPanel run = padded(new GridLayout(2, 2, SeedTheme.scale(12), SeedTheme.scale(7)), 10);
        run.add(runProgress); run.add(timeLimit); run.add(positionMethod); run.add(validationMethod); heading.add(run, BorderLayout.SOUTH);
        runProgress.setName("activeRunProgress"); timeLimit.setName("activeRunTimeLimit");
        positionMethod.setName("effectivePositionMethod"); validationMethod.setName("effectiveValidationMethod");

        JPanel phases = padded(new GridLayout(1, 3, SeedTheme.scale(16), 0), 10);
        phases.add(selfPlay); phases.add(training); phases.add(validation);
        JPanel comparisonBody = padded(new BorderLayout(0, SeedTheme.scale(8)), 12);
        JPanel metrics = panel(new GridLayout(1, 2, SeedTheme.scale(15), 0));
        metrics.add(counter(leftCaption, leftMetric, "")); metrics.add(counter(rightCaption, rightMetric, ""));
        comparisonBody.add(metrics, BorderLayout.NORTH);
        JPanel evidence = panel(new GridLayout(7, 1, 0, SeedTheme.scale(5)));
        evidence.add(direction); evidence.add(decision);
        for (int i = 0; i < comparison.length; i++) { comparison[i] = label("\u2014", 11, SeedTheme.SECONDARY); evidence.add(comparison[i]); }
        comparisonBody.add(evidence);
        leftMetric.setName("candidateScore"); decision.setName("promotionDecision");
        JPanel config = padded(new GridLayout(configuration.length, 1, 0, SeedTheme.scale(8)), 12);
        config.setName("effectiveTrainingConfiguration");
        for (int i = 0; i < configuration.length; i++) { configuration[i] = label("\u2014", 12, SeedTheme.TEXT); config.add(configuration[i]); }
        JPanel comparisons = panel(new GridBagLayout());
        addCell(comparisons, card(null, matchTitle, comparisonBody), 0, .6);
        addCell(comparisons, card("Effective Configuration", null, config), 1, .4);

        JPanel previews = panel(new GridBagLayout());
        addCell(previews, card("Comparison trend \u00b7 latest validation regime", null, recentScores), 0, .62);
        addCell(previews, card("Generation duration", null, recentDurations), 1, .38);
        JPanel latest = padded(new BorderLayout(0, SeedTheme.scale(6)), 8); latest.add(recentNote, BorderLayout.NORTH);
        JScrollPane recentScroll = scroll(TrainingHistory.table(recent, "recentTrainingHistory"));
        recentScroll.setPreferredSize(new Dimension(1, SeedTheme.scale(320))); latest.add(recentScroll);
        JComponent[] cards = {heading, card("Current Generation", null, phases), comparisons, previews, card("Recent History", null, latest)};
        GridBagConstraints c = new GridBagConstraints(); c.gridx = 0; c.weightx = 1; c.fill = GridBagConstraints.BOTH;
        for (int i = 0; i < cards.length; i++) {
            c.gridy = i; c.insets = new Insets(0, 0, SeedTheme.scale(10), 0);
            cards[i].setMinimumSize(new Dimension(0, cards[i].getPreferredSize().height)); add(cards[i], c);
        }
        c.gridy = cards.length; c.weighty = 1; add(Box.createVerticalGlue(), c);
    }
    JScrollPane historyView() { return scroll(history); }

    void showState(TrainingController.ViewState view) {
        var s = view.snapshot(); var r = s == null ? null : s.run().orElse(null);
        title.setText(generationTitle(s)); subtitle.setText(r == null ? view.message() : (view.active() ? r.action() : view.startAction()) + " \u00b7 Candidate "
                + network(s.candidateId()) + " \u00b7 Best " + network(s.bestId()));
        subtitle.setToolTipText(view.message());
        state.setText(phase(view)); state.setForeground(view.phase() == TrainingController.Phase.FAILED ? SeedTheme.ERROR : SeedTheme.GREEN);
        elapsed.setText(timer(s == null ? 0 : s.elapsed().toSeconds()));
        runProgress.setText(runLabel(s)); timeLimit.setText(timeLabel(s));
        positionMethod.setText("Positions: " + (r == null ? "awaiting effective configuration" : TrainingComparison.positionMethod(r)));
        validationMethod.setText("Validation: " + (r == null ? "awaiting effective configuration" : TrainingComparison.method(r.source(), r.supervision())));
        selfPlay.show(selfPlay(s, view.settings())); training.show(training(s)); validation.show(validation(s, view.settings()));
        showComparison(s);
        String[] config = r == null ? new String[]{"Effective settings appear when the generation starts"}
                : !r.generationSettingsKnown() ? new String[]{"Recovering a legacy Candidate", "Architecture: " + NetworkArchitecture.valueOf(r.effective().architecture().name()),
                "Recorded training depth: " + s.trainingDepth(), "Original generation settings were not recorded",
                "Current validation: " + r.effective().validation().openingPairs() + " pairs",
                "Validation depth: " + r.effective().validation().depth() + " \u00b7 Threads: " + r.effective().validation().threads()}
                : new String[]{
                "Architecture: " + NetworkArchitecture.valueOf(r.effective().architecture().name()),
                "Search depth: " + r.effective().selfPlay().depth() + " \u00b7 Workers: " + r.effective().selfPlay().threads(),
                "Games: " + count(r.effective().selfPlay().games()) + " \u00b7 Samples/game: up to " + r.effective().selfPlay().maximumSamplesPerGame(),
                "Epochs: " + r.effective().training().epochs() + " \u00b7 Batch: " + r.effective().training().minibatchSize(),
                "Opening plies: " + r.effective().selfPlay().minimumOpeningPlies() + "\u2014" + r.effective().selfPlay().maximumOpeningPlies()
                        + " \u00b7 Game cap: " + r.effective().selfPlay().maximumPlies(),
                r.source().bootstrap() ? "Holdout: whole games, about 20% (at least 2)" : "Validation: " + r.effective().validation().openingPairs()
                        + " pairs \u00b7 Depth " + r.effective().validation().depth() + " \u00b7 Threads " + r.effective().validation().threads(),
                r.source().bootstrap() ? r.supervision().description() : "Promotion margin: " + score(r.effective().validation().policy().requiredMargin())};
        for (int i = 0; i < configuration.length; i++) set(configuration[i], i < config.length ? config[i] : "");
        history.showHistory(view.history(), view.historyWarning());
        if (lastHistory != view.history()) {
            lastHistory = view.history(); var records = lastHistory.records();
            var preview = records.subList(Math.max(0, records.size() - 25), records.size());
            recentScores.showRecords(preview); recentDurations.showRecords(preview);
            recent.show(records.subList(Math.max(0, records.size() - 5), records.size()));
        }
        recentNote.setText(view.historyWarning().isBlank() && view.history().warnings().isEmpty()
                ? "C = candidate · B = incumbent Best · Δ = C − B · ↓ lower loss is better · ↑ higher score is better"
                : "History warning \u00b7 see History / Diagnostics");
    }
    private void showComparison(com.ohinteractive.seedv6.training.service.TrainerSnapshot s) {
        for (var line : comparison) set(line, "");
        var b = s == null ? null : s.bootstrapValidation().orElse(null);
        var m = match(s);
        boolean heldOut = b != null || s != null && s.run().map(r -> r.source().bootstrap()).orElse(false);
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
            leftCaption.setText("Candidate score"); rightCaption.setText("Promotion lower bound");
            leftMetric.setText(m == null ? "\u2014" : m.score());
            rightMetric.setText(s == null || s.assessment().isEmpty() ? "\u2014" : score(s.assessment().get().lowerBound()));
            direction.setText("↑ Higher score and lower confidence bound are better");
            decision.setText(m == null ? "Awaiting validation" : m.decision());
            if (m != null) {
                set(comparison[0], "W–D–L: " + m.wins() + "–" + m.draws() + "–" + m.losses());
                long finishedGames = s.validation().map(v -> v.terminations().entrySet().stream()
                        .filter(e -> e.getKey().completed() || e.getKey() == com.ohinteractive.seedv6.training.selfplay.GameTermination.PLY_CAP)
                        .mapToLong(java.util.Map.Entry::getValue).sum()).orElseGet(() -> (long) s.validationProgress().orElseThrow().completedGames());
                set(comparison[1], "Games finished: " + finishedGames + " / " + (2 * m.configuredPairs()) + " \u00b7 Valid pairs: " + m.pairs() + " / " + m.configuredPairs());
                set(comparison[2], m.detail());
                set(comparison[4], "Candidate " + m.candidate() + " \u00b7 Incumbent " + m.incumbent());
                comparison[4].setToolTipText(m.candidateId() + " / " + m.incumbentId());
            }
        }
        decision.setForeground(decision.getText().equals("PROMOTED") ? SeedTheme.GREEN : SeedTheme.TEXT);
    }
    private static void set(JLabel label, String text) { label.setText(text); label.setToolTipText(text); }
    static JPanel card(String title, JLabel heading, JComponent body) {
        JPanel card = new SeedTheme.Card(); card.setLayout(new BorderLayout());
        JPanel header = panel(new BorderLayout());
        JLabel label = heading == null ? label(title, 14, SeedTheme.TEXT) : heading;
        label.setFont(SeedTheme.font(14, Font.BOLD)); header.add(label);
        header.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, SeedTheme.LINE),
                BorderFactory.createEmptyBorder(SeedTheme.scale(5), SeedTheme.scale(12), SeedTheme.scale(5), SeedTheme.scale(12))));
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
        JPanel p = panel(new GridLayout(2, 1, 0, SeedTheme.scale(3))); p.add(label(caption, 11, SeedTheme.SECONDARY)); p.add(value); return p;
    }
    private static JLabel metric(String value, Color color) { JLabel l = label(value, 32, color); l.setFont(SeedTheme.font(32, Font.BOLD)); return l; }
    private static JPanel counter(JLabel title, JLabel metric, String caption) {
        JPanel p = panel(new BorderLayout(0, SeedTheme.scale(2))); p.add(title, BorderLayout.NORTH); p.add(metric); p.add(label(caption, 10, SeedTheme.SECONDARY), BorderLayout.SOUTH); return p;
    }
    private static void addCell(JPanel parent, JComponent child, int column, double weight) {
        GridBagConstraints c = new GridBagConstraints(); c.gridx = column; c.weightx = weight; c.weighty = 1; c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, column == 0 ? 0 : SeedTheme.scale(10), 0, 0); child.setMinimumSize(new Dimension(0, 0));
        // Equal preferred widths let weights, rather than long text, determine the column proportions.
        child.setPreferredSize(new Dimension(1, child.getPreferredSize().height)); parent.add(child, c);
    }
    private static final class PhaseProgress extends JPanel {
        private final JLabel value = label("—", 12, SeedTheme.TEXT), detail = label("—", 10, SeedTheme.SECONDARY);
        private final JProgressBar bar = new JProgressBar(0, 100);
        private final JLabel percent = label("—", 10, SeedTheme.SECONDARY);
        PhaseProgress(String title, String name) {
            super(new BorderLayout(0, SeedTheme.scale(5))); setOpaque(false);
            JPanel labels = panel(new GridLayout(3, 1, 0, SeedTheme.scale(3)));
            labels.add(label(title, 12, SeedTheme.TEXT)); labels.add(value); labels.add(detail); add(labels);
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
