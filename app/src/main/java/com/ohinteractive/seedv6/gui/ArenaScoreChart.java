package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.*;

/** Fixed, left-packed 100% bars. A stays below B; neither order nor color follows the winner. */
final class ArenaScoreChart extends JComponent implements Scrollable {
    static final Color FIRST = SeedTheme.GREEN, SECOND = new Color(0xe0a45e);
    private List<ArenaRoundSummary> rounds = List.of();
    ArenaScoreChart() { setName("arenaScoreChart"); setToolTipText(""); setFont(SeedTheme.font(13, Font.BOLD)); }
    void showRounds(List<ArenaRoundSummary> values) {
        rounds = values.stream().filter(ArenaRoundSummary::complete).toList(); revalidate(); repaint();
        getAccessibleContext().setAccessibleDescription("Completed round scores. A green lower share, B amber upper share; stable identities, valid pairs only. "
                + String.join(". ", rounds.stream().map(ArenaRoundSummary::description).toList()));
    }
    @Override public javax.accessibility.AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) accessibleContext = new AccessibleJComponent() {};
        return accessibleContext;
    }
    static int barWidth() { return SeedTheme.scale(144); }
    static int gap() { return SeedTheme.scale(32); }
    static int barX(int index) { return SeedTheme.scale(52) + index * (barWidth() + gap()); }
    @Override public Dimension getPreferredSize() { return new Dimension(Math.max(600, barX(rounds.size())), SeedTheme.scale(280)); }
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics); var g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int left = barX(0), top = SeedTheme.scale(42), height = Math.max(1, getHeight() - SeedTheme.scale(102));
            g.setColor(SeedTheme.INSET); g.fillRect(left, top, Math.max(0, getWidth() - left), height);
            g.setFont(SeedTheme.font(12, Font.PLAIN)); g.setColor(SeedTheme.SECONDARY);
            g.drawString("100%", 5, top + 5); g.drawString("50%", 10, top + height / 2 + 4); g.drawString("0%", 18, top + height);
            g.setColor(SeedTheme.LINE); g.drawLine(left, top + height / 2, getWidth(), top + height / 2);
            if (rounds.isEmpty()) { g.setColor(SeedTheme.SECONDARY); g.drawString("Completed round scores appear here", left + 18, top + 25); return; }
            for (int i = 0; i < rounds.size(); i++) {
                var round = rounds.get(i); int x = barX(i), bar = barWidth();
                g.setFont(getFont());
                if (round.firstScore() == null) {
                    g.setColor(SeedTheme.SECONDARY); g.drawRect(x, top, bar, height);
                    centered(g, "Unscored", x, bar, top + height / 2);
                } else {
                    int lower = (int)Math.round(height * round.firstScore()), upper = height - lower;
                    segment(g, x, top, bar, upper, SECOND);
                    segment(g, x, top + upper, bar, lower, FIRST);
                    caption(g, round.second(), round.score(false), x, bar, top, upper, top - SeedTheme.scale(24), SECOND);
                    caption(g, round.first(), round.score(true), x, bar, top + upper, lower, top + height + SeedTheme.scale(18), FIRST);
                }
                g.setFont(SeedTheme.font(12, Font.PLAIN)); g.setColor(SeedTheme.TEXT);
                centered(g, "Round " + round.round(), x, bar, top + height + SeedTheme.scale(53));
            }
        } finally { g.dispose(); }
    }
    private static void segment(Graphics2D g, int x, int y, int width, int height, Color color) {
        g.setColor(color); g.fillRect(x, y, width, height); g.setColor(SeedTheme.BACKGROUND);
        g.setStroke(new BasicStroke(2)); g.drawRect(x, y, width, height);
    }
    private static void caption(Graphics2D g, String name, String score, int x, int width, int y, int height, int outside, Color color) {
        boolean inside = height >= SeedTheme.scale(44);
        g.setColor(inside ? SeedTheme.BACKGROUND : color);
        int baseline = inside ? y + height / 2 - SeedTheme.scale(3) : outside;
        centered(g, name, x, width, baseline); centered(g, score, x, width, baseline + SeedTheme.scale(17));
    }
    private static void centered(Graphics2D g, String text, int x, int width, int y) { g.drawString(text, x + (width - g.getFontMetrics().stringWidth(text)) / 2, y); }
    @Override public String getToolTipText(MouseEvent event) {
        if (event.getX() < barX(0)) return null;
        int index = (event.getX() - barX(0)) / (barWidth() + gap());
        return index >= rounds.size() || event.getX() >= barX(index) + barWidth() ? null : rounds.get(index).description();
    }
    public Dimension getPreferredScrollableViewportSize() { return new Dimension(600, SeedTheme.scale(280)); }
    public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return barWidth() + gap(); }
    public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return Math.max(barWidth() + gap(), r.width - barWidth()); }
    public boolean getScrollableTracksViewportWidth() { return getParent() instanceof JViewport p && p.getWidth() >= getPreferredSize().width; }
    public boolean getScrollableTracksViewportHeight() { return true; }
}
