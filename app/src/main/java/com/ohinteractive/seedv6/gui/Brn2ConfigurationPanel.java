package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.text.ParseException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import com.ohinteractive.seedv6.training.service.BrnSupervision;
import com.ohinteractive.seedv6.training.service.BrnCaptureConsistency;
import com.ohinteractive.seedv6.training.service.BrnRunSeeds;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointInspection;
import javax.swing.*;
import com.ohinteractive.seedv6.core.brn.BrnAdamConfig;
import static com.ohinteractive.seedv6.gui.TrainingDashboard.*;

/** BRN-2 has online sparse Adam, with no minibatch or epoch controls in this milestone. */
final class Brn2ConfigurationPanel extends JPanel {
    private final JSpinner captureLambda = new JSpinner(new SpinnerNumberModel(0.0, 0.0, Double.MAX_VALUE, .5));
    private final Map<Path, BrnCaptureConsistency> captureDrafts = new HashMap<>();
    private boolean captureArchitecture;
    private final JSpinner learningRate;
    final JPanel sourceSlot = panel(new BorderLayout());
    private final JTextField teacherStore = new JTextField();
    private final JButton teacherBrowse = new JButton("Browse...");
    private final JPanel teacherRow = panel(new GridBagLayout());
    private final Map<Path, String> teacherDrafts = new HashMap<>();
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
    private record Selection(BrnSupervision supervision, boolean lineage, boolean seedLineage, BrnRunSeeds seeds, String teacherStore, BrnCaptureConsistency capture) {}

    Brn2ConfigurationPanel(TrainingSettings settings) {
        super(new BorderLayout()); setOpaque(false); setName("brn2Configuration");
        learningRate = new JSpinner(new SpinnerNumberModel(settings.brn2LearningRate(), Double.MIN_VALUE, Double.MAX_VALUE, 0.0001));
        learningRate.setEditor(new JSpinner.NumberEditor(learningRate, "0.##########"));
        learningRate.setName("brn2LearningRate");
        learningRate.setToolTipText("Used only when bootstrapping a new BRN-2 store. Resume restores the exact stored learning rate and Adam state.");
        JPanel body = padded(new BorderLayout(0, SeedTheme.scale(10)), 14);
        JPanel fields = panel(new GridBagLayout());
        teacherStore.setName("nnueTeacherStore"); teacherBrowse.setName("browseNnueTeacher");
        teacherStore.setText(settings.teacherStore() == null ? settings.generatorStore() : settings.teacherStore());
        if (!teacherStore.getText().isBlank()) teacherDrafts.put(settings.root(), teacherStore.getText());
        JPanel teacherEntry = panel(new BorderLayout(8, 0)); teacherEntry.add(teacherStore); teacherEntry.add(teacherBrowse, BorderLayout.EAST);
        TrainingPanel.row(teacherRow, 0, "NNUE Teacher Store", teacherEntry);
        teacherStore.setToolTipText("Accepted NNUE Best supplies static normalized targets. Each generation pins this teacher independently of position generation.");
        teacherBrowse.addActionListener(e -> {
            var chooser = new JFileChooser(teacherStore.getText()); chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) teacherStore.setText(chooser.getSelectedFile().toString());
        });
        teacherStore.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { rememberTeacher(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { rememberTeacher(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { rememberTeacher(); }
        });
        TrainingPanel.row(fields, 0, "Initial learning rate", learningRate);
        supervision.setName("brn2Supervision"); teacherWeight.setName("brn2TeacherWeight");
        teacherWeight.setEditor(new JSpinner.NumberEditor(teacherWeight, "0.##########"));
        contribution.setName("brn2WdlContribution"); objectiveNote.setName("brn2SupervisionStatus");
        weightRow.add(teacherWeight); weightRow.add(contribution, BorderLayout.EAST);
        TrainingPanel.row(fields, 1, "Supervision", supervision);
        JPanel blend = panel(new GridBagLayout());
        TrainingPanel.row(blend, 0, "NNUE teacher weight (%)", weightRow);
        // Keep the weight label and field together when WDL hides the entire row.
        JPanel selection = panel(new BorderLayout()); selection.add(teacherRow, BorderLayout.NORTH); selection.add(blend); selection.add(objectiveNote, BorderLayout.SOUTH);
        dataSeed.setName("brn2DataSeed");
        dataSeed.setToolTipText("Optional signed 64-bit self-play seed. Blank uses Model / run seed for self-play. Both effective seeds lock for a new lineage.");
        TrainingPanel.row(fields, 2, "Self-play data seed (optional)", dataSeed);
        dataSeed.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { rememberSeed(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { rememberSeed(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { rememberSeed(); }
        });
        if (settings.runSeeds() != null) seedDrafts.put(settings.root(), Long.toString(settings.runSeeds().dataSeed()));
        captureArchitecture = settings.architecture() == NetworkArchitecture.BRN2;
        captureLambda.setName("brn2CaptureConsistencyLambda");
        captureLambda.setEditor(new JSpinner.NumberEditor(captureLambda, "0.#################"));
        captureLambda.setToolTipText("Experimental; 0 is OFF. Requires NNUE blended supervision at 50%. Changing lambda restarts unfinished work from its settled parent.");
        TrainingPanel.row(fields, 3, "Capture consistency lambda (experimental)", captureLambda);
        if (captureArchitecture && settings.captureConsistency() != null) {
            captureDrafts.put(settings.root(), settings.captureConsistency());
            captureLambda.setValue(settings.captureConsistency().lambda());
        }
        captureLambda.addChangeListener(e -> {
            if (!updating && ready && selectedRoot != null) {
                double value = ((Number) captureLambda.getValue()).doubleValue();
                if (Double.isFinite(value) && value >= 0) captureDrafts.put(selectedRoot, new BrnCaptureConsistency(value));
            }
            changed.run();
        });
        var constraints = new GridBagConstraints(); constraints.gridy = 4; constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL; constraints.weightx = 1; fields.add(selection, constraints);
        supervision.addActionListener(e -> { if (!updating) { remember(); refresh(); } });
        teacherWeight.addChangeListener(e -> { if (!updating) { remember(); refresh(); } });
        if (settings.architecture() == NetworkArchitecture.BRN2 && settings.supervision() != null)
            drafts.put(settings.root(), settings.supervision());
        refresh();
        JPanel controls = panel(new BorderLayout(0, 8)); controls.add(sourceSlot, BorderLayout.NORTH); controls.add(fields);
        body.add(controls, BorderLayout.NORTH);
        JTextArea explanation = text("""
                Fresh stores use deterministic randomized weights and fresh Adam state. Each generation makes one shuffled online pass. Resume restores the exact optimizer.
                Handcrafted positions and WDL supervision are the defaults. Generation and supervision are independent. NNUE blended uses a separately pinned teacher's normalized static value, never the generator's search score.
                An explicit data seed changes only self-play openings; shuffle and hold-out streams keep Model / run seed. Both effective seeds lock for new lineages, including a blank data-seed field.
                Supervision and validation apply to the next campaign. Changed settings restart unfinished work from its settled parent. Component losses are descriptive.
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
        captureArchitecture = architecture == NetworkArchitecture.BRN2;
        long ticket = ++request; selectedRoot = null; storedSeeds = null; error = "";
        if (architecture != NetworkArchitecture.BRN2) { ready = true; refresh(); return; }
        ready = false; lineage = false; seedLineage = false; refresh();
        if (path.isBlank()) { ready = true; captureLambda.setValue(0.0); apply(BrnSupervision.WDL); return; }
        final Path root;
        try { root = Path.of(path).toAbsolutePath().normalize(); }
        catch (RuntimeException invalid) { error = invalid.getMessage(); ready = true; refresh(); return; }
        new SwingWorker<Selection, Void>() {
            protected Selection doInBackground() throws Exception {
                boolean fresh = CheckpointInspection.freshRoot(root, architecture.trainingArchitecture());
                var stored = CheckpointStore.readBrnSupervision(root);
                var seeds = CheckpointStore.readBrnRunSeeds(root);
                return new Selection(stored.orElse(BrnSupervision.WDL), !fresh || stored.isPresent(), !fresh || seeds.isPresent(), seeds.orElse(null), CheckpointStore.readBrnTeacherStore(root).orElse(null), CheckpointStore.readBrnCaptureConsistency(root));
            }
            protected void done() {
                if (ticket != request) return;
                selectedRoot = root; ready = true;
                try {
                    var selection = get(); lineage = selection.lineage(); seedLineage = selection.seedLineage(); storedSeeds = selection.seeds();
                    updating = true;
                    captureLambda.setValue(captureDrafts.getOrDefault(root, selection.capture()).lambda());
                    dataSeed.setText(storedSeeds != null ? Long.toString(storedSeeds.dataSeed()) : seedLineage ? "" : seedDrafts.getOrDefault(root, ""));
                    teacherStore.setText(teacherDrafts.getOrDefault(root, selection.teacherStore() == null ? "" : selection.teacherStore()));
                    updating = false;
                    apply(drafts.getOrDefault(root, selection.supervision()));
                } catch (Exception invalid) { error = TrainingController.concise(invalid); refresh(); }
            }
        }.execute();
    }
    BrnCaptureConsistency readCaptureConsistency() throws ParseException {
        if (!captureArchitecture) return null;
        if (!ready) return null;
        if (!error.isEmpty()) throw new IllegalArgumentException(error);
        captureLambda.commitEdit();
        return new BrnCaptureConsistency(((Number) captureLambda.getValue()).doubleValue());
    }
    String readTeacherStore() {
        if (!ready) return null;
        if (!error.isEmpty()) throw new IllegalArgumentException(error);
        return supervision.getSelectedItem() == BrnSupervision.Mode.NNUE_BLENDED ? teacherStore.getText().trim() : null;
    }
    private void rememberTeacher() {
        if (!updating && ready && selectedRoot != null) teacherDrafts.put(selectedRoot, teacherStore.getText());
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
        if (storedValue != null && supervision.getSelectedItem() == storedValue.mode()
                && (!storedValue.blended() || ((Number) teacherWeight.getValue()).doubleValue() == storedValue.teacherWeight() * 100))
            return storedValue; // Preserve the exact binary64 value when unedited.
        teacherWeight.commitEdit(); return selection();
    }
    private BrnSupervision selection() {
        return supervision.getSelectedItem() == BrnSupervision.Mode.WDL ? BrnSupervision.WDL
                : BrnSupervision.blended(((Number) teacherWeight.getValue()).doubleValue() / 100.0);
    }
    private void remember() { if (ready && selectedRoot != null) drafts.put(selectedRoot, selection()); }
    private void apply(BrnSupervision value) {
        storedValue = value;
        updating = true; supervision.setSelectedItem(value.mode());
        teacherWeight.setValue(value.blended() ? value.teacherWeight() * 100 : 50.0);
        updating = false; refresh();
    }
    private void refresh() {
        boolean blended = supervision.getSelectedItem() == BrnSupervision.Mode.NNUE_BLENDED;
        weightRow.getParent().setVisible(blended); teacherRow.setVisible(blended);
        contribution.setText(String.format(java.util.Locale.ROOT, "WDL: %.2f%%", 100 - ((Number) teacherWeight.getValue()).doubleValue()));
        boolean enabled = editable && ready && error.isEmpty();
        captureLambda.setEnabled(enabled && captureArchitecture);
        dataSeed.setEnabled(editable && ready && !seedLineage && error.isEmpty());
        teacherStore.setEnabled(enabled && blended); teacherBrowse.setEnabled(enabled && blended);
        supervision.setEnabled(enabled); teacherWeight.setEnabled(enabled && blended);
        objectiveNote.setText(!ready ? "Reading stored supervision..." : !error.isEmpty() ? error : "Next campaign objective; WDL is the default. Blended supervision is experimental.");
        changed.run(); revalidate();
    }
    void setEditable(boolean editable) { this.editable = editable; learningRate.setEnabled(editable); refresh(); }
}
