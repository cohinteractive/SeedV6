package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.text.ParseException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import com.ohinteractive.seedv6.training.service.BrnSupervision;
import com.ohinteractive.seedv6.training.service.BrnRunSeeds;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointInspection;
import javax.swing.*;
import com.ohinteractive.seedv6.core.brn.BrnAdamConfig;
import static com.ohinteractive.seedv6.gui.TrainingDashboard.*;

/** BRN-2 has online sparse Adam, with no minibatch or epoch controls in this milestone. */
final class Brn2ConfigurationPanel extends JPanel {
    private final JSpinner learningRate;
    private final JTextField dataSeed = new JTextField();
    private final Map<Path, String> seedDrafts = new HashMap<>();
    private BrnRunSeeds storedSeeds;
    private final JComboBox<BrnSupervision.Mode> supervision = new JComboBox<>(BrnSupervision.Mode.values());
    private final JSpinner teacherWeight = new JSpinner(new SpinnerNumberModel(50.0, 0.0, 100.0, 1.0));
    private final JPanel weightRow = panel(new BorderLayout(8, 0));
    private final JLabel contribution = label("", 12, SeedTheme.SECONDARY);
    private final JLabel objectiveNote = label("", 11, SeedTheme.SECONDARY);
    private final Map<Path, BrnSupervision> drafts = new HashMap<>();
    private Runnable changed = () -> {};
    private Path selectedRoot;
    private BrnSupervision storedValue = BrnSupervision.WDL;
    private boolean editable = true, ready = true, lineage, seedLineage, updating;
    private String error = "";
    private long request;
    private record Selection(BrnSupervision supervision, boolean lineage, boolean seedLineage, BrnRunSeeds seeds) {}

    Brn2ConfigurationPanel(TrainingSettings settings) {
        super(new BorderLayout()); setOpaque(false); setName("brn2Configuration");
        learningRate = new JSpinner(new SpinnerNumberModel(settings.brn2LearningRate(), Double.MIN_VALUE, Double.MAX_VALUE, 0.0001));
        learningRate.setEditor(new JSpinner.NumberEditor(learningRate, "0.##########"));
        learningRate.setName("brn2LearningRate");
        learningRate.setToolTipText("Used only when bootstrapping a new BRN-2 store. Resume restores the exact stored learning rate and Adam state.");
        JPanel body = padded(new BorderLayout(0, SeedTheme.scale(10)), 14);
        JPanel fields = panel(new GridBagLayout());
        TrainingPanel.row(fields, 0, "Initial learning rate", learningRate);
        supervision.setName("brn2Supervision"); teacherWeight.setName("brn2TeacherWeight");
        teacherWeight.setEditor(new JSpinner.NumberEditor(teacherWeight, "0.##########"));
        contribution.setName("brn2WdlContribution"); objectiveNote.setName("brn2SupervisionStatus");
        weightRow.add(teacherWeight); weightRow.add(contribution, BorderLayout.EAST);
        TrainingPanel.row(fields, 1, "Supervision", supervision);
        JPanel blend = panel(new GridBagLayout());
        TrainingPanel.row(blend, 0, "NNUE teacher weight (%)", weightRow);
        // Keep the weight label and field together when WDL hides the entire row.
        JPanel selection = panel(new BorderLayout()); selection.add(blend); selection.add(objectiveNote, BorderLayout.SOUTH);
        dataSeed.setName("brn2DataSeed");
        dataSeed.setToolTipText("Optional signed 64-bit self-play seed. Blank preserves Model / run seed behavior. An explicit value locks both seeds for the new lineage.");
        TrainingPanel.row(fields, 2, "Self-play data seed (optional)", dataSeed);
        dataSeed.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { rememberSeed(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { rememberSeed(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { rememberSeed(); }
        });
        if (settings.runSeeds() != null) seedDrafts.put(settings.root(), Long.toString(settings.runSeeds().dataSeed()));
        var constraints = new GridBagConstraints(); constraints.gridy = 3; constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL; constraints.weightx = 1; fields.add(selection, constraints);
        supervision.addActionListener(e -> { if (!updating) { remember(); refresh(); } });
        teacherWeight.addChangeListener(e -> { if (!updating) { remember(); refresh(); } });
        if (settings.architecture() == NetworkArchitecture.BRN2 && settings.supervision() != null)
            drafts.put(settings.root(), settings.supervision());
        refresh();
        body.add(fields, BorderLayout.NORTH);
        JTextArea explanation = text("""
                Fresh stores use deterministic randomized weights and fresh Adam state. Each generation makes one shuffled online pass. Resume restores the exact optimizer.
                WDL is the default. NNUE blended requires Bootstrap with NNUE and uses the pinned Generator's normalized static value from each sample's side to move.
                An explicit data seed changes only self-play openings; shuffle and hold-out streams keep Model / run seed. Both seeds then lock for Resume.
                Supervision is fixed for the lineage. Use a fresh store to change it. Lower configured held-out loss selects Best; component losses are descriptive.
                """, 12, SeedTheme.SECONDARY);
        explanation.setName("brn2TrainingInformation"); explanation.setRows(13); body.add(explanation);
        add(card("BRN-2 Configuration", null, body));
    }

    double read() throws ParseException {
        learningRate.commitEdit();
        double value = ((Number) learningRate.getValue()).doubleValue();
        new BrnAdamConfig(value);
        return value;
    }
    void onChange(Runnable changed) { this.changed = changed; }
    boolean ready() { return ready; }
    void selectRoot(String path, NetworkArchitecture architecture) {
        long ticket = ++request; selectedRoot = null; storedSeeds = null; error = "";
        if (architecture != NetworkArchitecture.BRN2) { ready = true; refresh(); return; }
        ready = false; lineage = false; seedLineage = false; refresh();
        if (path.isBlank()) { ready = true; apply(BrnSupervision.WDL); return; }
        final Path root;
        try { root = Path.of(path).toAbsolutePath().normalize(); }
        catch (RuntimeException invalid) { error = invalid.getMessage(); ready = true; refresh(); return; }
        new SwingWorker<Selection, Void>() {
            protected Selection doInBackground() throws Exception {
                boolean fresh = CheckpointInspection.freshRoot(root, architecture.trainingArchitecture());
                var stored = CheckpointStore.readBrnSupervision(root);
                var seeds = CheckpointStore.readBrnRunSeeds(root);
                return new Selection(stored.orElse(BrnSupervision.WDL), !fresh || stored.isPresent(), !fresh || seeds.isPresent(), seeds.orElse(null));
            }
            protected void done() {
                if (ticket != request) return;
                selectedRoot = root; ready = true;
                try {
                    var selection = get(); lineage = selection.lineage(); seedLineage = selection.seedLineage(); storedSeeds = selection.seeds();
                    updating = true;
                    dataSeed.setText(storedSeeds != null ? Long.toString(storedSeeds.dataSeed()) : seedLineage ? "" : seedDrafts.getOrDefault(root, ""));
                    updating = false;
                    apply(lineage ? selection.supervision() : drafts.getOrDefault(root, BrnSupervision.WDL));
                } catch (Exception invalid) { error = TrainingController.concise(invalid); refresh(); }
            }
        }.execute();
    }
    BrnRunSeeds storedRunSeeds() { return storedSeeds; }
    BrnRunSeeds readRunSeeds(long masterSeed) {
        if (!ready) throw new IllegalStateException("Reading stored run seeds; wait before Start.");
        if (!error.isEmpty()) throw new IllegalArgumentException(error);
        if (seedLineage) return storedSeeds;
        String text = dataSeed.getText().trim();
        return text.isEmpty() ? null : new BrnRunSeeds(masterSeed, Long.parseLong(text));
    }
    private void rememberSeed() {
        if (!updating && ready && !seedLineage && selectedRoot != null) seedDrafts.put(selectedRoot, dataSeed.getText());
    }
    BrnSupervision readSupervision() throws ParseException {
        if (!ready) return null; // Backend resolves durable state before Start.
        if (!error.isEmpty()) throw new IllegalArgumentException(error);
        if (lineage) return storedValue; // Do not round-trip a stored binary64 weight through percent display.
        teacherWeight.commitEdit(); return selection();
    }
    private BrnSupervision selection() {
        return supervision.getSelectedItem() == BrnSupervision.Mode.WDL ? BrnSupervision.WDL
                : BrnSupervision.blended(((Number) teacherWeight.getValue()).doubleValue() / 100.0);
    }
    private void remember() { if (ready && !lineage && selectedRoot != null) drafts.put(selectedRoot, selection()); }
    private void apply(BrnSupervision value) {
        storedValue = value;
        updating = true; supervision.setSelectedItem(value.mode());
        teacherWeight.setValue(value.blended() ? value.teacherWeight() * 100 : 50.0);
        updating = false; refresh();
    }
    private void refresh() {
        boolean blended = supervision.getSelectedItem() == BrnSupervision.Mode.NNUE_BLENDED;
        weightRow.getParent().setVisible(blended);
        contribution.setText(String.format(java.util.Locale.ROOT, "WDL: %.2f%%", 100 - ((Number) teacherWeight.getValue()).doubleValue()));
        boolean enabled = editable && ready && !lineage && error.isEmpty();
        dataSeed.setEnabled(editable && ready && !seedLineage && error.isEmpty());
        supervision.setEnabled(enabled); teacherWeight.setEnabled(enabled && blended);
        objectiveNote.setText(!ready ? "Reading stored supervision..." : !error.isEmpty() ? error : lineage
                ? "Supervision locked to this lineage." + (storedSeeds == null ? "" : " Run seeds locked too.")
                : "WDL remains the default; blended supervision is experimental.");
        changed.run(); revalidate();
    }
    void setEditable(boolean editable) { this.editable = editable; learningRate.setEnabled(editable); refresh(); }
}
