package com.ohinteractive.seedv6.gui;

import java.awt.GraphicsEnvironment;

import javax.swing.SwingUtilities;

/** Explicit native-GUI entry point; ordinary Main startup remains UCI. */
public final class SwingLauncher {

    public static void launch() {
        if(GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException("SeedV6 Swing requires a graphical desktop.");
        }
        SwingUtilities.invokeLater(() -> new ChessFrame().setVisible(true));
    }

    public static void main(String[] args) {
        launch();
    }

    private SwingLauncher() {}
}
