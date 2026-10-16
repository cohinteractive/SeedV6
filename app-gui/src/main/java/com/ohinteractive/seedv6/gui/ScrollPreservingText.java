package com.ohinteractive.seedv6.gui;

import javax.swing.*;
import javax.swing.text.DefaultCaret;

/** EDT-only replacement of a read-only status document, without caret-driven scrolling. */
final class ScrollPreservingText {
    private static final int BOTTOM_TOLERANCE = 2;
    private final JTextArea text;
    private final JScrollPane pane;
    private long revision;

    ScrollPreservingText(JTextArea text, JScrollPane pane) {
        this.text = text;
        this.pane = pane;
        ((DefaultCaret) text.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
    }

    void setText(String next) {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Status refresh requires EDT.");
        if (next.equals(text.getText())) return;
        JScrollBar bar = pane.getVerticalScrollBar();
        int position = bar.getValue();
        // Top takes precedence when all text fits; it must not start following later growth.
        boolean bottom = position > bar.getMinimum() && maximum(bar) - position <= BOTTOM_TOLERANCE;
        long update = ++revision;
        text.setText(next);
        layout();
        restore(bar, position, bottom);
        int restored = bar.getValue();
        // Revalidation is deferred by Swing. Do not let an older refresh undo a newer one,
        // or override a user scroll that occurred before this callback reached the EDT.
        SwingUtilities.invokeLater(() -> {
            if (update != revision || bar.getValueIsAdjusting() || bar.getValue() != restored) return;
            layout();
            restore(bar, position, bottom);
        });
    }

    private void layout() {
        pane.validate();
        pane.doLayout();
        pane.getViewport().doLayout();
    }

    private static int maximum(JScrollBar bar) { return Math.max(bar.getMinimum(), bar.getMaximum() - bar.getVisibleAmount()); }
    private static void restore(JScrollBar bar, int position, boolean bottom) {
        bar.setValue(bottom ? maximum(bar) : Math.max(bar.getMinimum(), Math.min(position, maximum(bar))));
    }
}
