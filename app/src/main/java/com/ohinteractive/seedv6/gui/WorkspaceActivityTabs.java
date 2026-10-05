package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.util.Arrays;
import java.util.stream.Collectors;
import javax.swing.*;

/** Shared low-cost tab animation; its single EDT timer stops when every workspace is idle. */
final class WorkspaceActivityTabs implements AutoCloseable {
    private static final Color CLEAR = new Color(66, 189, 123, 0);
    private static final Color ACCENT = new Color(66, 189, 123, 55);
    private static final Color WASH = new Color(66, 189, 123, 16);
    private static final Color EDGE = new Color(66, 189, 123, 105);
    private final Header[] headers;
    private final Timer timer;
    private float phase;
    private boolean closed;

    WorkspaceActivityTabs(JTabbedPane tabs) {
        headers = new Header[tabs.getTabCount()];
        for (int i = 0; i < headers.length; i++) {
            headers[i] = new Header(tabs.getTitleAt(i), tabs.getFont());
            tabs.setTabComponentAt(i, headers[i]);
        }
        timer = new Timer(80, event -> {
            phase = (System.nanoTime() % 3_000_000_000L) / 3_000_000_000F;
            for (var header : headers) if (header.activity.active()) header.repaint();
        });
    }

    void update(int tab, WorkspaceActivity activity) {
        requireEdt();
        if (closed) return;
        var header = headers[tab];
        if (!header.activity.equals(activity)) {
            header.activity = activity;
            phase = (System.nanoTime() % 3_000_000_000L) / 3_000_000_000F;
            header.setToolTipText(activity.active() ? header.getText() + " — " + activity.detail() : null);
            header.repaint();
        }
        if (Arrays.stream(headers).anyMatch(h -> h.activity.active())) {
            if (!timer.isRunning()) timer.start();
        } else timer.stop();
    }

    String status(String idleText) {
        String active = Arrays.stream(headers).filter(h -> h.activity.active())
                .map(h -> h.getText() + " — " + h.activity.detail()).collect(Collectors.joining("   |   "));
        return active.isEmpty() ? idleText : active;
    }

    boolean animating() { return timer.isRunning(); }
    @Override public void close() { requireEdt(); closed = true; timer.stop(); }

    private final class Header extends JLabel {
        private WorkspaceActivity activity = WorkspaceActivity.idle();
        Header(String title, Font font) {
            super(title);
            setFont(font);
            setForeground(SeedTheme.TEXT);
            setOpaque(false);
            SeedTheme.padding(this, 3, 8, 3, 8);
        }
        @Override protected void paintComponent(Graphics graphics) {
            if (activity.active() && getWidth() > 0) {
                Graphics2D g = (Graphics2D) graphics.create();
                try {
                    float band = Math.max(24, getWidth() * .8F);
                    float x = phase * (getWidth() + band) - band;
                    // A quiet persistent edge makes activity legible even between passing accents.
                    g.setColor(WASH);
                    g.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                    g.setColor(EDGE);
                    g.fillRect(4, getHeight() - 2, Math.max(0, getWidth() - 8), 2);
                    g.setPaint(new LinearGradientPaint(x, 0, x + band, 0,
                            new float[]{0, .5F, 1}, new Color[]{CLEAR, ACCENT, CLEAR}));
                    g.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                } finally { g.dispose(); }
            }
            super.paintComponent(graphics);
        }
    }

    private static void requireEdt() {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Workspace activity requires EDT");
    }
}
