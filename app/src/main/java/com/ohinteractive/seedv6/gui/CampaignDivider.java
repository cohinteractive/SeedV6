package com.ohinteractive.seedv6.gui;

import java.awt.*;
import javax.swing.*;

/** Thin content-aligned campaign separator; no fractional within-generation progress. */
final class CampaignDivider extends JComponent {
    private double fraction;
    CampaignDivider() { setName("campaignProgressDivider"); setPreferredSize(new Dimension(1, SeedTheme.scale(3))); }
    void showProgress(double value, String description) {
        fraction = value; setToolTipText(description);
        getAccessibleContext().setAccessibleName(description); repaint();
    }
    @Override public javax.accessibility.AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) accessibleContext = new AccessibleJComponent() {};
        return accessibleContext;
    }
    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(SeedTheme.LINE); g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        g.setColor(SeedTheme.SELECTED); g.fillRoundRect(0, 0, (int)Math.round(getWidth() * fraction), getHeight(), getHeight(), getHeight());
        g.dispose();
    }
}
