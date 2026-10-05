package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.*;

/** Symmetric 100% columns from finalized paired scores; empty slots are deliberately unscored. */
final class ArenaScoreChart extends JComponent implements Scrollable {
    static final Color FIRST = SeedTheme.GREEN, SECOND = new Color(0x7b98dc);
    private List<ArenaRoundSummary> rounds = List.of();
    ArenaScoreChart() { setName("arenaScoreChart"); setToolTipText(""); setFont(SeedTheme.font(11, Font.PLAIN)); }
    void showRounds(List<ArenaRoundSummary> values) {
        rounds = List.copyOf(values); revalidate(); repaint();
        getAccessibleContext().setAccessibleDescription("100% stacked round scores. Green lower share, blue upper share. 50% parity. Only completed valid pairs score; empty columns are pending or unscored.");
    }
    @Override public javax.accessibility.AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) accessibleContext = new AccessibleJComponent() {};
        return accessibleContext;
    }
    @Override public Dimension getPreferredSize() { return new Dimension(Math.max(600, 60 + rounds.size() * 70), SeedTheme.scale(230)); }
    private int slot() { return Math.max(1, (getWidth() - 65) / Math.max(1, rounds.size())); }
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics); var g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int left = 48, top = 15, height = Math.max(1, getHeight() - 52), width = getWidth() - left - 15;
            g.setColor(SeedTheme.INSET); g.fillRect(left, top, width, height);
            g.setFont(getFont()); g.setColor(SeedTheme.SECONDARY);
            g.drawString("100%", 6, top + 5); g.drawString("50%", 12, top + height / 2 + 4); g.drawString("0%", 20, top + height);
            if (rounds.isEmpty()) { g.drawString("Round scores appear here", left + 18, top + 25); return; }
            for (int i = 0; i < rounds.size(); i++) {
                var round = rounds.get(i); int span = slot(), bar = Math.min(54, Math.max(12, span - 16));
                int x = left + span * i + (span - bar) / 2;
                if (round.firstScore() == null) {
                    g.setColor(SeedTheme.LINE); g.drawRect(x, top, bar, height);
                    g.setColor(SeedTheme.SECONDARY); centered(g, round.complete() ? "N/A" : "...", x, bar, top + height / 2 - 8);
                } else {
                    int lower = (int) Math.round(height * round.firstScore()), upper = height - lower;
                    g.setColor(SECOND); g.fillRect(x, top, bar, upper); g.setColor(FIRST); g.fillRect(x, top + upper, bar, lower);
                    g.setColor(SeedTheme.BACKGROUND);
                    if (lower >= 25) centered(g, round.score(true), x, bar, top + upper + lower / 2 + (upper == 0 ? -8 : 4));
                    if (upper >= 25) centered(g, round.score(false), x, bar, top + upper / 2 + (lower == 0 ? -8 : 4));
                }
                g.setColor(SeedTheme.TEXT); centered(g, Integer.toString(round.round()), x, bar, top + height + 20);
            }
            g.setColor(SeedTheme.TEXT); g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[]{6, 4}, 0));
            g.drawLine(left, top + height / 2, left + width, top + height / 2);
            g.setColor(SeedTheme.SECONDARY); g.drawString("Round", 5, top + height + 20);
        } finally { g.dispose(); }
    }
    private static void centered(Graphics2D g, String text, int x, int width, int y) { g.drawString(text, x + (width - g.getFontMetrics().stringWidth(text)) / 2, y); }
    @Override public String getToolTipText(MouseEvent event) {
        int index = (event.getX() - 48) / slot();
        return event.getX() < 48 || index < 0 || index >= rounds.size() ? null : rounds.get(index).description();
    }
    public Dimension getPreferredScrollableViewportSize() { return new Dimension(600, SeedTheme.scale(230)); }
    public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 70; }
    public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return Math.max(70, r.width - 70); }
    public boolean getScrollableTracksViewportWidth() { return getParent() instanceof JViewport p && p.getWidth() >= getPreferredSize().width; }
    public boolean getScrollableTracksViewportHeight() { return true; }
}
