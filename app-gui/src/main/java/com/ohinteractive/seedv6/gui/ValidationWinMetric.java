package com.ohinteractive.seedv6.gui;

import java.awt.*;
import javax.swing.*;

/** Paint-only pulse: fixed label geometry, one bounded EDT timer per win counter. */
final class ValidationWinMetric extends JLabel {
    private final Color normal;
    private long pulseStarted;
    private double pulse;
    private final Timer animation = new Timer(16, e -> tick());

    ValidationWinMetric(String name, Color normal) {
        super("\u2014", SwingConstants.CENTER);
        this.normal = normal; setName(name); setForeground(normal); setFont(SeedTheme.font(32, Font.BOLD));
        setPreferredSize(new Dimension(SeedTheme.scale(90), SeedTheme.scale(47)));
        animation.setCoalesce(true);
    }
    void showCount(Integer count, boolean animate) {
        setText(count == null ? "\u2014" : count.toString());
        if (animate && isShowing()) { pulseStarted = System.nanoTime(); pulse = 1; animation.restart(); repaint(); }
    }
    private void tick() {
        double remaining = 1 - (System.nanoTime() - pulseStarted) / 420_000_000.0;
        if (remaining <= 0 || !isShowing()) { stopPulse(); return; }
        pulse = remaining * remaining; repaint();
    }
    void stopPulse() { animation.stop(); pulse = 0; repaint(); }
    @Override public void removeNotify() { stopPulse(); super.removeNotify(); }
    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        double scale = 1 + .10 * pulse;
        g.translate(getWidth() / 2.0, getHeight() / 2.0); g.scale(scale, scale);
        g.setFont(getFont()); FontMetrics fm = g.getFontMetrics();
        double bright = .85 * pulse;
        g.setColor(new Color((int)(normal.getRed() + (255 - normal.getRed()) * bright),
                (int)(normal.getGreen() + (255 - normal.getGreen()) * bright),
                (int)(normal.getBlue() + (255 - normal.getBlue()) * bright)));
        g.drawString(getText(), -fm.stringWidth(getText()) / 2, (fm.getAscent() - fm.getDescent()) / 2);
        g.dispose();
    }
}
