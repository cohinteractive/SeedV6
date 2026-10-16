package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.training.model.*;
import java.awt.*;
import java.nio.file.Path;
import javax.swing.*;

/** A lineage role that pins its accepted Best at each generation, using the common model browser. */
final class BestLineageField extends JPanel {
    private final JLabel identity = new JLabel("Select legacy NNUE lineage");
    private final JButton choose = new JButton("Select lineage...");
    private final TrainingFolders folders;
    private final Runnable changed;
    private String root = "";
    private long ticket;
    BestLineageField(String name, TrainingFolders folders, Runnable changed) {
        super(new BorderLayout(8, 0)); this.folders = folders; this.changed = changed; setOpaque(false); setName(name);
        identity.setName(name + "Identity"); choose.setName(name + "Choose");
        identity.setMinimumSize(new Dimension(80, 24)); identity.setPreferredSize(new Dimension(260, 24));
        add(identity); add(choose, BorderLayout.EAST); choose.addActionListener(e -> select());
    }
    String root() { return root; }
    void root(String value) {
        root = value == null ? "" : value; identity.setToolTipText(root); long expected = ++ticket;
        if (root.isBlank()) { identity.setText("Select legacy NNUE lineage"); changed.run(); return; }
        String selected = root; identity.setText("Reading lineage...");
        new SwingWorker<String, Void>() {
            protected String doInBackground() throws Exception {
                var entry = ModelLibrary.identify(Path.of(selected));
                if (entry.architecture() != TrainingArchitecture.NNUE) throw new java.io.IOException("Requires legacy NNUE (no material prior)");
                var snapshot = ModelLibrary.browse(entry);
                return entry.name() + " · Best " + snapshot.best().filter(g -> g.checkpoint().materialized()).map(g -> "Gen " + g.number()).orElse("unavailable");
            }
            protected void done() {
                if (ticket != expected) return;
                try { identity.setText(get()); }
                catch (Exception invalid) { identity.setText("Lineage unavailable"); identity.setToolTipText(selected + " · " + TrainingController.concise(invalid)); }
            }
        }.execute(); changed.run();
    }
    private void select() {
        Path initial;
        try { initial = root.isBlank() ? null : Path.of(root); }
        catch (java.nio.file.InvalidPathException invalid) { initial = null; }
        var browser = new ModelSelectionPanel(getName(), "Accepted Best is pinned independently at each generation",
                initial, folders, null, () -> {}, NetworkArchitecture.NNUE, true);
        browser.setPreferredSize(new Dimension(620, 325));
        try {
            while (JOptionPane.showConfirmDialog(this, browser, "Select model lineage", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
                if (!browser.validSelection()) { JOptionPane.showMessageDialog(this, "Choose a legacy NNUE lineage with an available Best generation."); continue; }
                root(browser.selectedRoot().toString()); return;
            }
        } finally { browser.dispose(); }
    }
    @Override public void setEnabled(boolean enabled) { super.setEnabled(enabled); if (choose != null) choose.setEnabled(enabled); }
}
