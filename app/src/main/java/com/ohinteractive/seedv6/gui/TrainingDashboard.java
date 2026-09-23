package com.ohinteractive.seedv6.gui;

import java.awt.*;
import javax.swing.*;
import static com.ohinteractive.seedv6.gui.TrainingDashboardModel.*;

/** EDT-only operational cards. No trainer ownership, filesystem reads or history accumulation. */
final class TrainingDashboard extends JPanel implements Scrollable {
    private final JLabel title = label("Training", 21, SeedTheme.TEXT);
    private final JLabel subtitle = label("Candidate not published → Best not loaded", 12, SeedTheme.SECONDARY);
    private final JLabel state = label("IDLE", 12, SeedTheme.GREEN), elapsed = label("00:00:00", 17, SeedTheme.TEXT);
    private final PhaseProgress selfPlay = new PhaseProgress("Self-play", "selfPlayProgress");
    private final PhaseProgress training = new PhaseProgress("Network training", "networkTrainingProgress");
    private final PhaseProgress validation = new PhaseProgress("Validation", "validationPairProgress");
    private final JLabel matchTitle = label("Candidate vs Best", 14, SeedTheme.TEXT);
    private final JLabel candidate = label("Candidate —", 12, SeedTheme.TEXT), incumbent = label("Best —", 12, SeedTheme.TEXT);
    private final JLabel wins = metric("—", SeedTheme.GREEN), draws = metric("—", SeedTheme.SECONDARY), losses = metric("—", SeedTheme.ERROR);
    private final JLabel score = metric("—", SeedTheme.GREEN);
    private final JTextArea decision = text("Awaiting validation", 13, SeedTheme.TEXT);
    private final JTextArea decisionDetail = text("Decision follows all configured game slots", 11, SeedTheme.SECONDARY);
    private final JPanel comparisonCards = panel(new CardLayout());
    private final JTextArea bootstrapDecision = text("Waiting for held-out WDL validation", 12, SeedTheme.TEXT);
    private final JLabel matchNote = label("Candidate score against the incumbent Best in this match · valid pairs only", 11, SeedTheme.SECONDARY);
    private final JLabel best = label("Not loaded", 14, SeedTheme.TEXT), currentCandidate = label("Not published", 14, SeedTheme.TEXT);
    private final JLabel promotions = label("0 this run", 14, SeedTheme.TEXT);
    private final JLabel depth = label("—", 13, SeedTheme.TEXT), games = label("—", 13, SeedTheme.TEXT);
    private final JLabel pairs = label("—", 13, SeedTheme.TEXT), threads = label("—", 13, SeedTheme.TEXT);
    private final JLabel recentNote = label("No completed validation yet", 11, SeedTheme.SECONDARY);
    private final TrainingHistory.Records recent = new TrainingHistory.Records();
    private final TrainingHistory history = new TrainingHistory();
    private final HistoryChart recentScores = new HistoryChart(true, 80), recentDurations = new HistoryChart(false, 80);
    private com.ohinteractive.seedv6.training.history.HistoryRepository.Snapshot lastHistory;

    TrainingDashboard() {
        super(new GridBagLayout()); setOpaque(false); setName("trainingDashboard");
        title.setFont(SeedTheme.font(21, Font.BOLD)); title.setName("trainingHeadline"); state.setName("trainingState");
        state.setOpaque(true); state.setBackground(SeedTheme.SELECTED); SeedTheme.padding(state, 8, 12, 8, 12);
        JPanel heading = new SeedTheme.Card(); heading.setLayout(new BorderLayout(SeedTheme.scale(10), 0));
        JPanel headingBody = padded(new BorderLayout(SeedTheme.scale(10), 0), 10);
        JPanel titles = panel(new BorderLayout(0, SeedTheme.scale(3))); titles.add(title); titles.add(subtitle, BorderLayout.SOUTH);
        headingBody.add(titles); JPanel badges = panel(new FlowLayout(FlowLayout.RIGHT, SeedTheme.scale(14), 0));
        badges.add(state); badges.add(value("Run elapsed", elapsed)); headingBody.add(badges, BorderLayout.EAST); heading.add(headingBody);

        JPanel phases = padded(new GridLayout(1, 3, SeedTheme.scale(16), 0), 8);
        phases.add(selfPlay); phases.add(training); phases.add(validation);
        training.setToolTipText("Applied optimizer updates and sample visits. Loss measures fit to terminal targets, not playing strength.");

        JPanel comparison = padded(new BorderLayout(0, SeedTheme.scale(8)), 8);
        JPanel columns = panel(new GridBagLayout());
        JPanel records = panel(new GridLayout(1, 3, SeedTheme.scale(12), 0));
        records.add(counter(candidate, wins, "wins")); records.add(counter(label("Draws", 12, SeedTheme.SECONDARY), draws, "valid-pair games"));
        records.add(counter(incumbent, losses, "wins"));
        addCell(columns, records, 0, .46); addCell(columns, counter(label("Candidate score", 11, SeedTheme.SECONDARY), score, "relative to incumbent"), 1, .23);
        JPanel verdict = panel(new BorderLayout(0, SeedTheme.scale(6)));
        verdict.add(label("Promotion decision", 11, SeedTheme.SECONDARY), BorderLayout.NORTH);
        verdict.add(decision); verdict.add(decisionDetail, BorderLayout.SOUTH); addCell(columns, verdict, 2, .31);
        comparison.add(columns); comparison.add(matchNote, BorderLayout.SOUTH);
        comparisonCards.add(comparison, "games");
        bootstrapDecision.setName("bootstrapValidationDecision"); bootstrapDecision.setRows(8);
        JPanel bootstrapBody = padded(new BorderLayout(), 8); bootstrapBody.add(bootstrapDecision);
        comparisonCards.add(bootstrapBody, "loss");
        score.setName("candidateScore"); decision.setName("promotionDecision");

        JPanel networks = padded(new GridBagLayout(), 8);
        addCell(networks, value("Current Best", best), 0, .22);
        addCell(networks, value("Current Candidate", currentCandidate), 1, .22);
        addCell(networks, value("Promotions", promotions), 2, .18);
        JPanel regime = panel(new GridLayout(2, 2, SeedTheme.scale(10), SeedTheme.scale(4)));
        regime.add(depth); regime.add(games); regime.add(pairs); regime.add(threads);
        addCell(networks, regime, 3, .38);

        JPanel previews = panel(new GridBagLayout());
        addCell(previews, card("Candidate score vs incumbent Best", null, recentScores), 0, .62);
        JComponent durationPreview = card("Generation duration", null, recentDurations);
        durationPreview.setToolTipText("Generation duration · active processing time");
        addCell(previews, durationPreview, 1, .38);

        JPanel latest = panel(new BorderLayout());
        JPanel latestBody = padded(new BorderLayout(0, SeedTheme.scale(6)), 8); latestBody.add(recentNote, BorderLayout.NORTH);
        JScrollPane recentScroll = scroll(TrainingHistory.table(recent, "recentTrainingHistory"));
        recentScroll.setPreferredSize(new Dimension(1, SeedTheme.scale(102)));
        latestBody.add(recentScroll); latest.add(latestBody);

        JComponent[] cards = {heading, card("Current Generation", null, phases), card(null, matchTitle, comparisonCards),
                card("Best / Candidate Status · Current regime", null, networks), previews, card("Recent History", null, latest)};
        GridBagConstraints c = new GridBagConstraints(); c.gridx = 0; c.weightx = 1; c.fill = GridBagConstraints.BOTH;
        for (int i = 0; i < cards.length; i++) {
            c.gridy = i; c.insets = new Insets(0, 0, i == cards.length - 1 ? 0 : SeedTheme.scale(10), 0);
            cards[i].setMinimumSize(new Dimension(0, cards[i].getPreferredSize().height)); add(cards[i], c);
        }
        c.gridy = cards.length; c.weighty = 1; add(Box.createVerticalGlue(), c);
    }

    JScrollPane historyView() { return scroll(history); }

    void showState(TrainingController.ViewState view) {
        var s = view.snapshot(); var m = match(s); var settings = view.settings();
        title.setText(s == null ? "Training" : "Training · Generation " + s.generation());
        subtitle.setText(s == null ? "Candidate not published → Best not loaded"
                : "Candidate " + (s.candidateId().isEmpty() ? "pending" : network(s.candidateId())) + " → Best " + network(s.bestId()));
        state.setText(phase(view)); state.setForeground(view.phase() == TrainingController.Phase.FAILED || s != null && s.failed() ? SeedTheme.ERROR : SeedTheme.GREEN);
        elapsed.setText(timer(s == null ? 0 : s.elapsed().toSeconds()));
        selfPlay.show(selfPlay(s, settings)); training.show(training(s)); validation.show(validation(s, settings));
        matchTitle.setText(m == null || m.current() ? "Candidate vs Best" : "Candidate vs Best · Latest match");
        candidate.setText(m == null ? "Candidate —" : "Candidate " + m.candidate()); incumbent.setText(m == null ? "Best —" : "Best " + m.incumbent());
        candidate.setToolTipText(m == null ? null : m.candidateId()); incumbent.setToolTipText(m == null ? null : m.incumbentId());
        wins.setText(m == null ? "—" : Integer.toString(m.wins())); draws.setText(m == null ? "—" : Integer.toString(m.draws())); losses.setText(m == null ? "—" : Integer.toString(m.losses()));
        score.setText(m == null ? "—" : m.score()); decision.setText(m == null ? "Awaiting validation" : m.decision());
        decision.setForeground(m != null && m.failed() ? SeedTheme.ERROR : m != null && m.decision().equals("PROMOTED") ? SeedTheme.GREEN : SeedTheme.TEXT);
        decisionDetail.setText(m == null ? "No Candidate match available" : m.detail());
        best.setText(s == null ? "Not loaded" : network(s.bestId()) + (s.bestId().equals(view.bootstrapId()) ? " · bootstrap" : ""));
        currentCandidate.setText(s == null ? "Not published" : network(s.candidateId()));
        best.setToolTipText(s == null ? null : s.bestId()); currentCandidate.setToolTipText(s == null ? null : s.candidateId());
        promotions.setText((s == null ? 0 : s.totals().promotions()) + " this run");
        depth.setText("Depth " + settings.depth()); games.setText("Games " + count(settings.games())); pairs.setText("Pairs " + count(settings.validationPairs())); threads.setText("Threads " + settings.threads());
        boolean lossMode = s != null && s.bootstrapValidation().isPresent() || settings.source() != null && settings.source().bootstrap();
        ((CardLayout) comparisonCards.getLayout()).show(comparisonCards, lossMode ? "loss" : "games");
        if (lossMode) {
            matchTitle.setText("Candidate vs Best - held-out loss"); pairs.setText("Held-out loss");
            bootstrapDecision.setText(s != null && s.bootstrapValidation().isPresent() ? TrainingProgress.bootstrapSummary(s)
                    : "NNUE generates games; the BRN student learns its configured target.\nCandidate and Best will be compared on the same held-out games.\nLower prediction loss selects Best; this is not a game-strength result.");
        }
        history.showHistory(view.history(), view.historyWarning());
        if (lastHistory != view.history()) {
            lastHistory = view.history();
            var records = lastHistory.records();
            var preview = records.subList(Math.max(0, records.size() - 25), records.size());
            recentScores.showRecords(preview); recentDurations.showRecords(preview);
            recent.show(records.subList(Math.max(0, records.size() - 5), records.size()));
        }
        recentNote.setText(view.historyWarning().isBlank() && view.history().warnings().isEmpty()
                ? lossMode ? "Bootstrap promotions use configured held-out loss; no game score is plotted"
                : "Last 25 plotted · green = promoted · opponent may change; not absolute strength"
                : "History warning · see History / Diagnostics");
        recentNote.setToolTipText("Last 25 completed generations; green diamonds = promotion, × = unavailable measurement. Each score is against that generation's incumbent Best.");

    }

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
