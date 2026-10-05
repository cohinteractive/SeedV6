package com.ohinteractive.seedv6.gui;

import java.nio.file.Path;
import java.util.prefs.Preferences;

/** Play naming/lifecycle adapter for the common application model selector. */
final class PlayEnginePanel extends ModelSelectionPanel {
    PlayEnginePanel(String side, Path fallback, Preferences preferences, Runnable changed) {
        this(side, side.equals("white") ? "White Engine" : "Black Engine", fallback, preferences, changed);
    }
    PlayEnginePanel(String side, String title, Path fallback, Preferences preferences, Runnable changed) {
        this(side, title, fallback, new TrainingFolders(TrainingSettings.defaults(fallback, NetworkArchitecture.NNUE)), preferences, changed);
    }
    PlayEnginePanel(String side, String title, Path fallback, TrainingFolders folders, Preferences preferences, Runnable changed) {
        super(side, title, fallback, folders, preferences, changed);
    }
}
