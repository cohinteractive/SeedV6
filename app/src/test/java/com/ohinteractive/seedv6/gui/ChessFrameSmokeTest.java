package com.ohinteractive.seedv6.gui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class ChessFrameSmokeTest {

    @Test
    void createsShowsAndClosesARealWindowWhenADesktopIsAvailable() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        final CountDownLatch closed = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            final ChessFrame frame = new ChessFrame();
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent event) {
                    closed.countDown();
                }
            });
            frame.setVisible(true);
            frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
        });
        assertTrue(closed.await(10L, TimeUnit.SECONDS));
    }
}
