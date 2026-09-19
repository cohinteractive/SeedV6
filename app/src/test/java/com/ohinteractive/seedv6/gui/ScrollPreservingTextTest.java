package com.ohinteractive.seedv6.gui;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.junit.jupiter.api.Test;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.edt;
import static org.junit.jupiter.api.Assertions.*;

class ScrollPreservingTextTest {
    @Test void topMiddleBottomAndShorteningSurviveRepeatedDocumentReplacement() throws Exception {
        exercise(null);
    }

    @Test void nativeSwingLayoutAlsoPreservesTopMiddleBottomAndClamps() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        JFrame frame = edt(() -> new JFrame("SeedV6 validation scroll test"));
        try { exercise(frame); }
        finally { edt(frame::dispose); }
    }

    private static void exercise(JFrame frame) throws Exception {
        Fixture f = edt(() -> new Fixture(frame));
        for (int mode = 0; mode < 3; mode++) {
            int anchor = mode;
            int position = edt(() -> {
                f.bar().setValue(anchor == 0 ? 0 : anchor == 1 ? f.bottom() / 2 : f.bottom() - 1);
                return f.bar().getValue();
            });
            for (int update = 0; update < 6; update++) {
                String changed = lines(100 + update * 8, "Refresh " + update);
                edt(() -> f.writer.setText(changed));
                settle();
                edt(() -> assertEquals(anchor == 2 ? f.bottom() : position, f.bar().getValue(), 2,
                        "Refresh must preserve the user's anchor"));
            }
        }
        edt(() -> { f.bar().setValue(f.bottom() / 2); f.writer.setText(lines(20, "Shorter")); });
        settle();
        edt(() -> assertEquals(f.bottom(), f.bar().getValue()));
        edt(() -> f.writer.setText("Fits"));
        settle();
        edt(() -> assertEquals(0, f.bar().getValue()));
        edt(() -> f.writer.setText(lines(100, "Grows from top")));
        settle();
        edt(() -> assertEquals(0, f.bar().getValue(), "A previously fitting document stays at the top"));
        assertThrows(IllegalStateException.class, () -> f.writer.setText("off EDT"));
    }

    @Test void unchangedTextDoesNotRewriteDocumentAndPendingRefreshDoesNotUndoManualScrolling() throws Exception {
        Fixture f = edt(() -> new Fixture(null));
        AtomicInteger mutations = new AtomicInteger();
        edt(() -> {
            f.text.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { mutations.incrementAndGet(); }
                public void removeUpdate(DocumentEvent e) { mutations.incrementAndGet(); }
                public void changedUpdate(DocumentEvent e) { mutations.incrementAndGet(); }
            });
            f.bar().setValue(200);
            f.writer.setText(f.text.getText());
            assertEquals(0, mutations.get());
            f.writer.setText(lines(120, "Changed"));
            f.bar().setValue(350); // User scroll after replacement, before its deferred restoration.
        });
        settle();
        edt(() -> assertEquals(350, f.bar().getValue()));
        edt(() -> {
            f.writer.setText(lines(130, "Older queued refresh"));
            f.bar().setValue(450);
            f.writer.setText(lines(150, "Newest refresh"));
        });
        settle();
        edt(() -> assertEquals(450, f.bar().getValue()));
        edt(() -> {
            f.writer.setText(lines(160, "While dragging"));
            f.bar().setValueIsAdjusting(true);
            f.bar().setValue(550);
        });
        settle();
        edt(() -> { assertEquals(550, f.bar().getValue()); f.bar().setValueIsAdjusting(false); });
    }

    private static void settle() throws Exception { edt(() -> {}); edt(() -> {}); }
    private static String lines(int count, String prefix) { return (prefix + " - wrapped status fields and telemetry\n").repeat(count); }

    private static final class Fixture {
        final JTextArea text = new JTextArea();
        final JScrollPane pane = new JScrollPane(text);
        final ScrollPreservingText writer = new ScrollPreservingText(text, pane);
        Fixture(JFrame frame) {
            text.setEditable(false); text.setLineWrap(true); text.setWrapStyleWord(true);
            pane.setPreferredSize(new Dimension(360, 180));
            pane.setSize(360, 180);
            if (frame != null) { frame.setContentPane(pane); frame.pack(); frame.setVisible(true); }
            writer.setText(lines(100, "Initial"));
            assertTrue(bottom() > 500);
        }
        JScrollBar bar() { return pane.getVerticalScrollBar(); }
        int bottom() { return bar().getMaximum() - bar().getVisibleAmount(); }
    }
}
