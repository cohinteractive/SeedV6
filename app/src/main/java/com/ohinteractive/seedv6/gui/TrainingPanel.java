package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import com.ohinteractive.seedv6.search.exact.ParallelSearch;
import com.ohinteractive.seedv6.training.validation.PromotionPolicy;
import com.ohinteractive.seedv6.training.service.ValidationMethod;
import com.ohinteractive.seedv6.training.service.RunTermination;
import static com.ohinteractive.seedv6.gui.TrainingDashboard.*;

/** EDT-only workspace over the existing controller and immutable trainer publications. */
final class TrainingPanel extends JPanel {
    private final JTextField root = new JTextField(20), seed = new JTextField();
    private final JTextField baseRoot = new JTextField(20);
    private final JComboBox<TrainingLineages.Entry> lineageSelector = new JComboBox<>();
    private final JButton newLineage = new JButton("New Lineage..."), importLineage = new JButton("Import...");
    private final JButton scheduledStop = new JButton("Stop after Generation");
    private final JButton browseGenerations = new JButton("Generations, notes & provenance...");
    private boolean rebinding;
    private TrainingLineages.Selection displayedLineage;
    private final JTextArea configurationOrigin = text("Create or import a lineage. Recorded initialization and prior recipes are available in Generations, notes & provenance.", 11, SeedTheme.SECONDARY);
    private final JSpinner depth = spinner(4, 1, 256), threads = ThreadSelection.spinner(1);
    private final JSpinner games = spinner(64, 1, 100_000), pairs = spinner(64, 1, 100_000);
    private final JSpinner min, max, samples, plies, generations, runMinutes;
    private final JComboBox<RunTermination.Kind> termination = new JComboBox<>(RunTermination.Kind.values());
    private final JComboBox<NetworkArchitecture> architecture = new JComboBox<>(NetworkArchitecture.values());
    private final JComboBox<ValidationMethod> validationMethod = new JComboBox<>(ValidationMethod.values());
    private final JPanel validationEntry = panel(new BorderLayout());
    private final JPanel architectureCards = new JPanel(new CardLayout()) {
        @Override public Dimension getPreferredSize() {
            for (Component child : getComponents()) if (child.isVisible()) return child.getPreferredSize();
            return super.getPreferredSize();
        }
    };
    private final NnueConfigurationPanel nnue;
    private final TrainingRecipePanel recipe;
    private final BrnConfigurationPanel brn;
    private final Brn1ConfigurationPanel brn1;
    private final Brn2ConfigurationPanel brn2;
    private final Brn3ConfigurationPanel brn3;
    private final BrnTrainingSourcePanel trainingSource;
    private final JButton browse = new JButton("Browse…"), apply = new JButton("Apply settings");
    private final JButton start = new JButton("Start Training"), stop = new JButton("Stop Now");
    private final JTextArea progress = new JTextArea(17, 32), validation = new JTextArea(12, 32);
    private final JScrollPane trainingBlock = new JScrollPane(progress), validationBlock = new JScrollPane(validation);
    private final ScrollPreservingText trainingText = new ScrollPreservingText(progress, trainingBlock);
    private final ScrollPreservingText validationText = new ScrollPreservingText(validation, validationBlock);
    private final JSplitPane outputs = new JSplitPane(JSplitPane.VERTICAL_SPLIT) {
        @Override public void doLayout() {
            if (getTopComponent() != null && getBottomComponent() != null) {
                int minimum = getInsets().top + getTopComponent().getMinimumSize().height;
                int maximum = getHeight() - getInsets().bottom - getDividerSize() - getBottomComponent().getMinimumSize().height;
                // Retain a user's usable divider position; recover it when resizing would hide a log.
                if (maximum >= minimum && (getDividerLocation() < minimum || getDividerLocation() > maximum))
                    setDividerLocation(minimum + (maximum - minimum) / 2);
            }
            super.doLayout();
        }
    };
    private final TrainingProgressView optimization = new TrainingProgressView("trainingOptimization", true);
    private final List<JComponent> editors = new ArrayList<>();
    private final JTabbedPane tabs = new JTabbedPane();
    private final TrainingDashboard dashboard = new TrainingDashboard();
    private final JScrollPane dashboardScroll;
    private final JLabel status = label("IDLE", 11, SeedTheme.SECONDARY);
    private final JTextArea providerSummary = text("", 12, SeedTheme.SECONDARY);
    private TrainingController controller;
    private boolean confirming, applying, wasActive, validationChoiceEdited;
    private final TrainingFolders folders;
    private NetworkArchitecture displayedArchitecture;

    TrainingPanel(TrainingSettings settings) {
        this(settings, new TrainingFolders(settings));
    }

    TrainingPanel(TrainingSettings settings, TrainingFolders folders) {
        super(new BorderLayout(0, SeedTheme.scale(10))); setOpaque(false);
        this.folders = folders;
        browseGenerations.setName("browseTrainingGenerations");
        browseGenerations.addActionListener(e -> {
            if (root.getText().isBlank()) return;
            var browser = new ModelSelectionPanel("trainingBrowser", "Training continues from this lineage's Latest checkpoint",
                    Path.of(root.getText()), folders, null, () -> {});
            browser.lockLineage(); browser.setPreferredSize(new Dimension(SeedTheme.scale(620), SeedTheme.scale(330)));
            try { JOptionPane.showMessageDialog(this, browser, "Lineage generations", JOptionPane.PLAIN_MESSAGE); }
            finally { browser.dispose(); }
        });
        displayedArchitecture = settings.architecture();
        root.setName("trainingRoot"); depth.setName("trainingDepth"); threads.setName("trainingThreads");
        games.setName("trainingGames"); pairs.setName("trainingPairs"); progress.setName("trainingProgress");
        start.setName("startTraining"); stop.setName("stopTraining"); apply.setName("applyTrainingSettings");
        status.setName("trainingLifecycleStatus");
        root.setText(folders.root(displayedArchitecture)); root.setToolTipText(root.getText());
        depth.setValue(settings.depth()); ThreadSelection.setChoice(threads, settings.threads()); games.setValue(settings.games()); pairs.setValue(settings.validationPairs());
        min = spinner(settings.openingMin(), 0, 100_000); max = spinner(settings.openingMax(), 0, 100_000);
        samples = spinner(settings.samples(), 1, 100_000); plies = spinner(settings.maximumPlies(), 1, 100_000);
        generations = new JSpinner(new SpinnerNumberModel(settings.maximumGenerations(), 0L, Long.MAX_VALUE, 1L));
        runMinutes = new JSpinner(new SpinnerNumberModel(settings.maximumRunMinutes(), 0L, 5256000L, 1L));
        runMinutes.setName("trainingRunMinutes"); generations.setName("trainingGenerations");
        termination.setName("trainingTermination"); termination.setSelectedItem(settings.termination().kind());
        termination.setToolTipText("Time budgets finish the current generation. Stop Now separately saves resumable partial work.");
        termination.addActionListener(e -> {
            if (!rebinding) {
                var kind = (RunTermination.Kind) termination.getSelectedItem();
                if ((kind == RunTermination.Kind.GENERATIONS || kind == RunTermination.Kind.COMBINED) && ((Number) generations.getValue()).longValue() == 0) generations.setValue(1L);
                if ((kind == RunTermination.Kind.TIME_BUDGET || kind == RunTermination.Kind.COMBINED) && ((Number) runMinutes.getValue()).longValue() == 0) runMinutes.setValue(60L);
            }
            updateTerminationControls();
        });
        validationMethod.setName("trainingValidationMethod"); validationMethod.setSelectedItem(settings.generatedValidation());
        validationEntry.add(validationMethod);
        validationMethod.addActionListener(e -> { validationChoiceEdited = true; sourceChanged(); });
        seed.setText(Long.toString(settings.seed()));
        seed.setName("trainingSeed"); samples.setName("trainingSamples");
        seed.setToolTipText("One seed for deterministic run streams and fresh network initialization. Resume restores the stored model and optimizer.");
        architecture.setName("networkArchitecture"); architecture.setSelectedItem(settings.architecture());
        architectureCards.setName("architectureConfiguration"); architectureCards.setOpaque(false);
        recipe = new TrainingRecipePanel(settings);
        nnue = new NnueConfigurationPanel(settings); architectureCards.add(nnue, NetworkArchitecture.NNUE.name());
        brn = new BrnConfigurationPanel(settings); architectureCards.add(brn, NetworkArchitecture.BRN.name());
        brn1 = new Brn1ConfigurationPanel(settings); architectureCards.add(brn1, NetworkArchitecture.BRN1.name());
        brn2 = new Brn2ConfigurationPanel(settings, folders); architectureCards.add(brn2, NetworkArchitecture.BRN2.name());
        brn3 = new Brn3ConfigurationPanel(settings); architectureCards.add(brn3, NetworkArchitecture.BRN3.name());
        architectureCards.add(new BrePair2ConfigurationPanel(), NetworkArchitecture.BRN_PAIR2.name());
        trainingSource = new BrnTrainingSourcePanel(settings, folders, this::sourceChanged);
        seed.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { changed(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { changed(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { changed(); }
            private void changed() { if (!rebinding) try { trainingSource.selectionSeedChanged(Long.parseLong(seed.getText().trim())); } catch (NumberFormatException incomplete) { /* Apply validates. */ } }
        });
        brn2.onChange(() -> {
            if (brn2.storedRunSeeds() != null) seed.setText(Long.toString(brn2.storedRunSeeds().masterSeed()));
            sourceChanged();
        });
        architecture.addActionListener(event -> {
            if (rebinding) return;
            var requested = selectedArchitecture();
            if (controller == null) {
                displayedArchitecture = requested; placeSource();
                ((CardLayout) architectureCards.getLayout()).show(architectureCards, requested.nnueFamily() ? NetworkArchitecture.NNUE.name() : requested.name());
                return;
            }
            rebinding = true; architecture.setSelectedItem(displayedArchitecture); rebinding = false;
            selectCatalog(folders.base(), requested, null);
        });
        ((CardLayout) architectureCards.getLayout()).show(architectureCards, settings.architecture().nnueFamily() ? NetworkArchitecture.NNUE.name() : settings.architecture().name());
        JPanel selection = padded(new BorderLayout(SeedTheme.scale(12), 0), 10);
        JLabel architectureLabel = label("Network Architecture", 12, SeedTheme.SECONDARY);
        architectureLabel.setLabelFor(architecture); selection.add(architectureLabel, BorderLayout.WEST); selection.add(architecture);
        JPanel lineageRow = panel(new BorderLayout(SeedTheme.scale(8), 0));
        lineageRow.add(label("Training Lineage", 12, SeedTheme.SECONDARY), BorderLayout.WEST);
        lineageRow.add(lineageSelector);
        JPanel manage = panel(new FlowLayout(FlowLayout.RIGHT, SeedTheme.scale(6), 0));
        manage.add(newLineage); manage.add(importLineage); lineageRow.add(manage, BorderLayout.EAST);
        selection.add(lineageRow, BorderLayout.SOUTH);
        lineageSelector.setName("trainingLineage"); newLineage.setName("newTrainingLineage"); importLineage.setName("importTrainingLineage");
        baseRoot.setName("baseTrainingRoot"); baseRoot.setText(folders.base().toString()); baseRoot.setEditable(false);
        root.setEditable(false); scheduledStop.setName("scheduleTrainingStop");
        add(selection, BorderLayout.NORTH);
        editors.addAll(List.of(lineageSelector, newLineage, importLineage, baseRoot));
        editors.addAll(List.of(root, browse, depth, threads, games, pairs, min, max, samples, plies, generations, runMinutes, seed, architecture, validationMethod, apply));
        editors.add(termination); editors.add(browseGenerations);
        tabs.setName("trainingViews"); tabs.putClientProperty("JTabbedPane.tabAreaAlignment", "leading");
        dashboardScroll = scroll(dashboard); dashboardScroll.setName("trainingDashboardScroll");
        tabs.addTab("Dashboard", dashboardScroll); tabs.addTab("History", dashboard.historyView());
        tabs.addTab("Validation & run", configuration()); tabs.addTab("Diagnostics", diagnostics());
        tabs.addTab("Recipe & lineage", networkConfiguration());
        JPanel exposure = new ConfigurationCards();
        JPanel exposureFields = padded(new GridBagLayout(), 12);
        row(exposureFields, 0, "Positions / generation", trainingSource.positionsControl());
        row(exposureFields, 1, "Generated games / generation", games); row(exposureFields, 2, "Samples / generated game", samples);
        row(exposureFields, 3, "Run / shuffle seed", seed);
        addCard(exposure, card("Training run and exposure", null, exposureFields), 0); addCard(exposure, trainingSource, 1);
        tabs.addTab("Data & exposure", scroll(exposure));
        add(tabs);
        JPanel actions = panel(new BorderLayout(SeedTheme.scale(8), 0)); actions.add(status);
        JPanel buttons = panel(new FlowLayout(FlowLayout.RIGHT, SeedTheme.scale(8), 0)); buttons.add(start);
        JPanel stopping = panel(new BorderLayout()); stopping.setName("trainingStopControl");
        stopping.add(scheduledStop); stopping.add(stop, BorderLayout.EAST); buttons.add(stopping); actions.add(buttons, BorderLayout.EAST);
        start.putClientProperty("FlatLaf.style", "background: #218f59; foreground: #ffffff; hoverBackground: #29a568; pressedBackground: #187547");
        add(actions, BorderLayout.SOUTH); stop.setEnabled(false); scheduledStop.setVisible(false); stop.setVisible(false);
        root.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { changed(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { changed(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { changed(); }
            private void changed() { if (!rebinding) { trainingSource.selectRoot(root.getText(), displayedArchitecture); brn2.selectRoot(root.getText(), displayedArchitecture); } }
        });
        trainingSource.selectRoot(root.getText(), displayedArchitecture); brn2.selectRoot(root.getText(), displayedArchitecture);
        browse.addActionListener(event -> {
            FilePickers.choose(this, FilePickers.Purpose.TRAINING_STORAGE, "Open", baseRoot.getText())
                    .ifPresent(path -> selectCatalog(path, displayedArchitecture, null));
        });
        lineageSelector.addActionListener(event -> {
            if (rebinding || controller == null) return;
            var entry = (TrainingLineages.Entry) lineageSelector.getSelectedItem();
            if (entry == null) return;
            rebinding = true; lineageSelector.setSelectedItem(displayedLineage == null ? null : displayedLineage.entry()); rebinding = false;
            selectCatalog(folders.base(), displayedArchitecture, () -> entry);
        });
        newLineage.addActionListener(event -> {
            String name = JOptionPane.showInputDialog(this, "Training lineage name", "New Lineage", JOptionPane.PLAIN_MESSAGE);
            if (name != null) selectCatalog(folders.base(), displayedArchitecture,
                    () -> TrainingLineages.create(folders.base(), displayedArchitecture, name));
        });
        importLineage.addActionListener(event -> {
            FilePickers.choose(this, FilePickers.Purpose.ADOPT_CHECKPOINT,
                    "Adopt existing checkpoint store in place (no files moved)", folders.base().toString())
                    .ifPresent(path -> selectCatalog(folders.base(), displayedArchitecture,
                            () -> new TrainingLineages.Entry(path, displayedArchitecture, path.getFileName().toString())));
        });
        scheduledStop.addActionListener(event -> controller.toggleScheduledStop());
        apply.addActionListener(event -> applySettings());
        start.addActionListener(event -> { if (applySettings()) controller.start(); });
        stop.addActionListener(event -> controller.stop());
        if (settings.validationMethod() == null) restoreLegacyValidation(settings);
    }

    /** One-time preference interpretation, never a listener coupling the two selectors. */
    private void restoreLegacyValidation(TrainingSettings initial) {
        new SwingWorker<ValidationMethod, Void>() {
            protected ValidationMethod doInBackground() throws Exception {
                return new TrainingController.Backend().resolveSource(initial).selectedValidation();
            }
            protected void done() {
                if (validationChoiceEdited || displayedArchitecture != initial.architecture()
                        || !root.getText().equals(initial.root().toString())) return;
                try { validationMethod.setSelectedItem(get()); }
                catch (Exception invalidStore) { /* Normal startup inspection reports the store error. */ }
            }
        }.execute();
    }

    void showDiagnostics() { tabs.setSelectedIndex(3); }
    // First activation only: tab focus/layout may otherwise reveal a lower child on a short viewport.
    // Later polls and workspace switches preserve the user's chosen position.
    void showDashboardTop() { SwingUtilities.invokeLater(() -> dashboardScroll.getViewport().setViewPosition(new Point())); }
    void bind(TrainingController controller) { this.controller = controller; showState(controller.state()); }

    /** Production startup loads saved lineage state before Start is made available. */
    void loadInitialLineage() { controller.requireLineageSelection(); selectCatalog(folders.base(), displayedArchitecture, null); }

    void selectCatalog(Path base, NetworkArchitecture selected, java.util.concurrent.Callable<TrainingLineages.Entry> requested) {
        if (controller == null || controller.state().active() || controller.state().loading()) return;
        var choices = new java.util.concurrent.atomic.AtomicReference<List<TrainingLineages.Entry>>();
        Path normalized = base.toAbsolutePath().normalize();
        controller.selectLineage(() -> {
            var entry = requested == null ? null : requested.call();
            var discovered = new ArrayList<>(TrainingLineages.discover(normalized, selected, folders.adopted(normalized, selected)));
            if (entry == null) {
                String preferred = folders.root(selected);
                entry = discovered.stream().filter(e -> e.root().toString().equals(preferred)).findFirst()
                        .orElse(discovered.isEmpty() ? null : discovered.getFirst());
            }
            var loaded = entry == null ? null : TrainingLineages.read(entry);
            if (loaded != null) {
                discovered.removeIf(e -> e.root().equals(loaded.entry().root())); discovered.add(loaded.entry());
                discovered.sort(java.util.Comparator.comparing(TrainingLineages.Entry::name, String.CASE_INSENSITIVE_ORDER));
            }
            choices.set(List.copyOf(discovered));
            return loaded;
        }, TrainingSettings.defaults(normalized.resolve(selected.folderName()).resolve("unselected"), selected), failure -> {
            if (failure != null) {
                JOptionPane.showMessageDialog(this, "Could not load training lineage: " + TrainingController.concise(failure),
                        "Training lineage", JOptionPane.ERROR_MESSAGE);
                return;
            }
            folders.base(normalized); folders.select(selected); baseRoot.setText(normalized.toString());
            var loaded = controller.state().lineage();
            if (loaded != null) {
                folders.register(normalized, selected, loaded.entry().root());
                folders.remember(selected, loaded.entry().root().toString());
            }
            rebinding = true;
            lineageSelector.removeAllItems(); choices.get().forEach(lineageSelector::addItem);
            lineageSelector.setSelectedItem(loaded == null ? null : loaded.entry());
            rebinding = false;
            loadSettings(controller.state()); showState(controller.state());
        });
    }

    private void loadSettings(TrainingController.ViewState state) {
        rebinding = true;
        try {
            var s = state.settings(); displayedLineage = state.lineage(); displayedArchitecture = s.architecture();
            architecture.setSelectedItem(displayedArchitecture); placeSource();
            if (displayedLineage != null) {
                boolean listed = false;
                for (int i = 0; i < lineageSelector.getItemCount(); i++) if (lineageSelector.getItemAt(i).equals(displayedLineage.entry())) listed = true;
                if (!listed) lineageSelector.addItem(displayedLineage.entry());
            }
            lineageSelector.setSelectedItem(displayedLineage == null ? null : displayedLineage.entry());
            lineageSelector.setToolTipText(displayedLineage == null ? "Create or import a training lineage"
                    : displayedLineage.lineage().name() + " | " + displayedLineage.lineage().id());
            ((CardLayout) architectureCards.getLayout()).show(architectureCards, displayedArchitecture.nnueFamily() ? NetworkArchitecture.NNUE.name() : displayedArchitecture.name());
            root.setText(displayedLineage == null ? "" : s.root().toString()); root.setToolTipText(root.getText());
            configurationOrigin.setText(displayedLineage == null ? "Create or import a training lineage."
                    : displayedLineage.lineage().configurationOrigin());
            depth.setValue(s.depth()); ThreadSelection.setChoice(threads, s.threads()); games.setValue(s.games()); pairs.setValue(s.validationPairs());
            min.setValue(s.openingMin()); max.setValue(s.openingMax()); samples.setValue(s.samples()); plies.setValue(s.maximumPlies());
            generations.setValue(s.maximumGenerations()); runMinutes.setValue(s.maximumRunMinutes()); seed.setText(Long.toString(s.seed()));
            termination.setSelectedItem(s.termination().kind()); updateTerminationControls();
            validationChoiceEdited = true; validationMethod.setSelectedItem(s.generatedValidation());
            nnue.load(s); trainingSource.load(s);
            recipe.load(s);
            brn2.load(s, displayedLineage != null && displayedLineage.seedLocked());
        } finally { rebinding = false; }
    }

    Runnable beginCorpusShutdown() { return () -> {}; }

    void showStorageSettings() {
        JPanel storage = panel(new BorderLayout(8, 8));
        storage.add(label("Base Training Root (this machine)", 12, SeedTheme.SECONDARY), BorderLayout.NORTH);
        storage.add(baseRoot); storage.add(browse, BorderLayout.EAST);
        storage.add(label("Select a root to discover its lineages. Existing files are kept in place.", 12, SeedTheme.SECONDARY), BorderLayout.SOUTH);
        JOptionPane.showMessageDialog(this, storage, "Training storage settings", JOptionPane.PLAIN_MESSAGE);
    }

    boolean applySettings() {
        if (controller == null || applying) return false;
        applying = true;
        try {
            for (JSpinner spinner : List.of(depth, threads, games, pairs, min, max, samples, plies, generations, runMinutes)) spinner.commitEdit();
            if (root.getText().isBlank()) throw new IllegalArgumentException("Select a checkpoint folder.");
            var previous = controller.state().settings();
            var options = recipe.read();
            double rate = previous.brnLearningRate();
            double rate1 = previous.brn1LearningRate();
            double rate2 = previous.brn2LearningRate();
            final long masterSeed;
            try { masterSeed = Long.parseLong(seed.getText().trim()); }
            catch (NumberFormatException invalid) { throw new IllegalArgumentException("Run / shuffle seed must be a signed 64-bit integer.", invalid); }
            validationChoiceEdited = true;
            var runLimit = RunTermination.selected((RunTermination.Kind) termination.getSelectedItem(),
                    ((Number) generations.getValue()).longValue(), java.time.Duration.ofMinutes(((Number) runMinutes.getValue()).longValue()).toMillis());
            TrainingSettings edited = new TrainingSettings(Path.of(root.getText()), value(depth), value(threads), value(games),
                    value(min), value(max), value(samples), options.minibatch(), options.epochs(), value(pairs), masterSeed,
                    value(plies), runLimit.generations(), selectedArchitecture(), rate, rate1, rate2,
                    selectedArchitecture().nnueFamily() && !trainingSource.corpus() && !previous.corpusSelected() && previous.source() == null ? null : trainingSource.read(), trainingSource.generatorStore(),
                    selectedArchitecture() == NetworkArchitecture.BRN2 ? brn2.readSupervision() : null)
                    .withCaptureConsistency(selectedArchitecture() == NetworkArchitecture.BRN2 ? brn2.readCaptureConsistency() : null)
                    .withTeacherStore(selectedArchitecture() == NetworkArchitecture.BRN2 ? brn2.readTeacherStore() : null)
                    .withRunSeeds(selectedArchitecture() == NetworkArchitecture.BRN2 ? brn2.readRunSeeds(masterSeed) : null)
                    .withTimeLimit(java.time.Duration.ofMillis(runLimit.millis()).toMinutes())
                    .withValidationMethod((ValidationMethod) validationMethod.getSelectedItem())
                    .withCorpus(selectedArchitecture().supportsTrainingData() && trainingSource.corpus() ? trainingSource.corpusRoot() : previous.corpusRoot(),
                            selectedArchitecture().supportsTrainingData() ? trainingSource.readCorpusConfig() : previous.corpusTraining())
                    .withLearningRate(options.learningRate());
            trainingSource.saveSources();
            controller.setSettings(edited); root.setToolTipText(edited.root().toString());
            folders.remember(displayedArchitecture, root.getText()); folders.select(displayedArchitecture);
            return true;
        } catch (Exception invalid) {
            tabs.setSelectedIndex(2);
            JOptionPane.showMessageDialog(this, "Check training settings: " + TrainingController.concise(invalid), "Invalid training settings", JOptionPane.ERROR_MESSAGE);
            return false;
        } finally { applying = false; }
    }

    void showState(TrainingController.ViewState state) {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Training view requires EDT.");
        if (displayedLineage != state.lineage()) loadSettings(state);
        boolean stopped = wasActive && !state.active(); wasActive = state.active();
        if (stopped) {
            brn2.selectRoot(root.getText(), displayedArchitecture);
            if (displayedArchitecture.supportsTrainingData()) trainingSource.selectRoot(root.getText(), displayedArchitecture);
        }
        boolean editable = !state.active() && !state.loading() && state.phase() != TrainingController.Phase.CLOSING;
        editors.forEach(component -> component.setEnabled(editable));
        apply.setEnabled(editable && !root.getText().isBlank());
        browseGenerations.setEnabled(editable && !root.getText().isBlank());
        brn2.setEditable(editable);
        recipe.setEditable(editable);
        trainingSource.setEditable(editable);
        updateCorpusControls(editable);
        updateTerminationControls();
        seed.setEnabled(editable && (displayedArchitecture != NetworkArchitecture.BRN2 || (brn2.ready() && brn2.storedRunSeeds() == null)));
        start.setEnabled(state.canStart() && trainingSource.ready() && brn2.ready()); start.setText(state.startAction());
        start.setVisible(!state.active()); scheduledStop.setVisible(state.active()); stop.setVisible(state.active());
        scheduledStop.setText(state.scheduledStopGeneration() > 0 ? "Cancel Scheduled Stop" : "Stop after Generation "
                + (state.snapshot() == null || state.snapshot().generation() == 0 ? "..." : state.snapshot().generation()));
        scheduledStop.setEnabled(state.phase() == TrainingController.Phase.RUNNING && state.snapshot() != null
                && state.snapshot().running() && !state.snapshot().stopping()
                && state.snapshot().run().map(r -> !r.timeLimitReached()).orElse(true)
                && state.snapshot().state() != com.ohinteractive.seedv6.training.service.TrainerSnapshot.State.RECOVERING
                && state.snapshot().state() != com.ohinteractive.seedv6.training.service.TrainerSnapshot.State.ACQUIRING_TRAINING_DATA
                && state.snapshot().generation() > 0);
        stop.setEnabled(state.active() && state.phase() != TrainingController.Phase.STOPPING && state.phase() != TrainingController.Phase.CLOSING);
        dashboard.showState(state);
        status.setText(TrainingDashboardModel.phase(state) + (!state.active() ? " - " + state.startAction()
                : state.snapshot() != null && state.snapshot().state() == com.ohinteractive.seedv6.training.service.TrainerSnapshot.State.ACQUIRING_TRAINING_DATA
                ? " - " + state.message() : state.message().contains("Restarted unfinished generation")
                ? " - unfinished generation restarted from settled checkpoint (see Diagnostics)" : ""));
        status.setToolTipText(state.message());
        status.setForeground(state.phase() == TrainingController.Phase.FAILED ? SeedTheme.ERROR : SeedTheme.SECONDARY);
        // Both bounded documents retain all previous diagnostics. No full-log reconstruction or caret jump.
        optimization.showTraining(state);
        trainingText.setText(TrainingProgress.format(state) + "\n\nHistory: " + state.settings().root().resolve(com.ohinteractive.seedv6.training.history.HistoryRepository.FILE)
                + "\n" + String.join("\n", state.history().warnings()) + "\n" + state.historyWarning());
        progress.setForeground(state.phase() == TrainingController.Phase.FAILED ? SeedTheme.ERROR : SeedTheme.TEXT);
        String rendered = TrainingProgress.validation(state, System.nanoTime());
        validationText.setText(rendered);
        validation.setForeground(rendered.startsWith("Validation failed") ? SeedTheme.ERROR
                : rendered.contains("INCONCLUSIVE") || rendered.contains("Warning:") ? SeedTheme.WARNING : SeedTheme.TEXT);
        if (state.phase() == TrainingController.Phase.CONFIRM_DEPTH && !confirming) {
            confirming = true;
            SwingUtilities.invokeLater(() -> {
                try {
                    if (controller.state().phase() == TrainingController.Phase.CONFIRM_DEPTH) {
                        controller.confirmDepth(JOptionPane.showConfirmDialog(this, state.message(), "Confirm training depth change",
                                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION);
                    }
                } finally { confirming = false; }
            });
        }
    }

    private void placeSource() {
        revalidate();
    }
    private JScrollPane networkConfiguration() {
        JPanel body = new ConfigurationCards();
        addCard(body, card("Default training recipe", null, recipe), 0);
        addCard(body, architectureCards, 1);
        JPanel setup = padded(new GridBagLayout(), 14);
        GridBagConstraints detail = new GridBagConstraints(); detail.gridx = 0; detail.gridy = 1; detail.gridwidth = 2;
        detail.fill = GridBagConstraints.HORIZONTAL; detail.weightx = 1; detail.insets = new Insets(12, 0, 0, 0);
        setup.add(configurationOrigin, detail); addCard(body, card("Lineage provenance", null, setup), 2);
        addCard(body, browseGenerations, 3);
        GridBagConstraints filler = new GridBagConstraints(); filler.gridy = 4; filler.weighty = 1; body.add(Box.createVerticalGlue(), filler);
        JScrollPane scroll = scroll(body); scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); return scroll;
    }
    private JScrollPane configuration() {
        JPanel content = new ConfigurationCards();
        providerSummary.setName("trainingProviderSummary"); addCard(content, providerSummary, 0);
        root.setToolTipText("Selected lineage storage; use Training Lineage above to switch.");
        configurationOrigin.setName("lineageConfigurationOrigin");
        JPanel protocol = padded(new GridLayout(1, 2, SeedTheme.scale(20), 0), 14);
        JPanel left = panel(new GridBagLayout()), right = panel(new GridBagLayout());
        row(left, 0, "Candidate validation", validationEntry); row(right, 0, "Validation pairs", pairs);
        row(left, 1, "Search depth (plies)", depth); row(right, 1, "Search threads", threads);
        row(left, 2, "Opening min. plies", min); row(right, 2, "Opening max. plies", max);
        row(left, 3, "Game ply cap", plies);
        var information = validationInformation(); information.setPreferredSize(new Dimension(1, SeedTheme.scale(340)));
        var rules = new JButton("Validation rules"); rules.setName("validationRules");
        rules.addActionListener(event -> information.scrollRectToVisible(new Rectangle(0, 0, information.getWidth(), information.getHeight())));
        row(right, 3, "Scoring and promotion", rules);
        protocol.add(left); protocol.add(right);
        addCard(content, card("Validation and generated-game protocol", null, protocol), 1);
        var semantics = text("Search depth, threads, openings and ply cap apply to generated games and game-pair validation. Held-out validation uses reserved samples. Paired games reverse colours; promotion keeps its existing rules.", 12, SeedTheme.SECONDARY);
        semantics.setRows(3); addCard(content, semantics, 2);
        JPanel stopping = padded(new GridBagLayout(), 14);
        row(stopping, 0, "Stop policy", termination); row(stopping, 1, "Generations this run", generations);
        row(stopping, 2, "Time budget (minutes)", runMinutes);
        addCard(content, card("Run termination - finish the current generation", null, stopping), 3);
        updateTerminationControls();
        JPanel commit = padded(new BorderLayout(SeedTheme.scale(10), 0), 12);
        JTextArea help = text("Stop before editing. Resume with unchanged settings continues exactly. Changed generation settings restart unfinished work from the last settled checkpoint. Best changes only through the existing promotion rules.", 12, SeedTheme.SECONDARY);
        help.setRows(3); commit.add(help); commit.add(apply, BorderLayout.EAST); addCard(content, commit, 4);
        addCard(content, information, 5);
        GridBagConstraints filler = new GridBagConstraints(); filler.gridy = 6; filler.weighty = 1; content.add(Box.createVerticalGlue(), filler);
        JScrollPane scroll = scroll(content); scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); return scroll;
    }

    private JPanel diagnostics() {
        for (JTextArea area : List.of(progress, validation)) {
            area.setEditable(false); area.setLineWrap(true); area.setWrapStyleWord(true);
            area.setMargin(new Insets(SeedTheme.scale(8), SeedTheme.scale(10), SeedTheme.scale(8), SeedTheme.scale(10)));
            area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, SeedTheme.scale(12))); area.setBackground(SeedTheme.INSET);
        }
        validation.setName("validationProgress"); validation.setToolTipText("Scores count valid pairs only. Search and promotion rules are in Validation & run.");
        trainingBlock.setBorder(BorderFactory.createTitledBorder("Trainer snapshot")); validationBlock.setBorder(BorderFactory.createTitledBorder("Candidate validation"));
        trainingBlock.setMinimumSize(new Dimension(100, 80)); validationBlock.setMinimumSize(new Dimension(100, 80));
        outputs.setName("trainingOutputs"); outputs.setTopComponent(trainingBlock); outputs.setBottomComponent(validationBlock);
        outputs.setBorder(BorderFactory.createEmptyBorder()); outputs.setContinuousLayout(true); outputs.setResizeWeight(.4); outputs.setDividerSize(SeedTheme.scale(8));
        outputs.setDividerLocation(SeedTheme.scale(300));
        JPanel body = panel(new BorderLayout(0, SeedTheme.scale(8)));
        JPanel details = panel(new BorderLayout()); details.add(root, BorderLayout.SOUTH);
        details.add(optimization, BorderLayout.NORTH);
        body.add(details, BorderLayout.NORTH); body.add(outputs); return body;
    }

    static JScrollPane validationInformation() {
        JTextArea explanation = new JTextArea("""
                BRN-3 Training Data
                BRN-3 starts from a fixed material prior and trains its relational residual from CP or compatible BT4 Q-labeled Training Data. It does not generate training games. Minibatch size and epochs are in Recipe & lineage; positions are in Data & exposure, and validation is in Validation & run. Held-out validation reserves separate records and uses the frozen outcome adapter for Candidate and Best. Strictly lower prediction loss promotes; game-pair validation is a separate strength test.

                Earlier BRN position generation and supervision
                BRN-2 defaults to Handcrafted position generation and WDL targets. NNUE generation and NNUE blended supervision independently pin accepted NNUE Best checkpoints per generation. Handcrafted scores never enter targets. BRN-2 can select NNUE blended supervision in its architecture configuration; mode and weight apply at safe campaign boundaries. A seeded whole-game split holds out about 20%% of completed sampled games (at least two; at least two training games). Candidate and Best use the same held-out positions. Strictly lower mean half-squared error promotes; ties keep Best. This measures prediction loss, not game strength. Position generation and candidate validation are independent. Game pairs evaluate any resulting Candidate. Held-out validation reserves whole games before training, including for network self-play. Position generation can change between campaigns in the same compatible store. While stopped, changing other generation settings restarts unfinished work from the last settled checkpoint; unchanged settings preserve exact Resume.

                Games and scoring
                Each pair uses the same randomized opening with reversed colours. Opening length is sampled within the configured range, using uniformly selected legal moves and seeded random streams.
                The game cap and current-game plies count moves after the opening. Only pairs with two chess-complete games count toward Candidate/Best wins, draws and score. Capped, cancelled or failed pairs are incomplete and excluded from scoring.
                Games/Overall track configured game slots; cancelled slots can include unplayed games. Capped counts games, not pairs.

                Promotion
                Pair score is the mean of its two game scores (win = 1, draw = 0.5, loss = 0). Candidate score is the mean over valid pairs.
                Hoeffding lower = mean - sqrt(-ln(alpha) / (2*n)), where n is valid pairs. Promotion requires the configured minimum valid pairs and lower > 0.5 + required margin; equality keeps Best.
                The GUI requires all configured Validation pairs to be valid (default %d), with alpha %s and required margin %s. Assessment follows all configured game slots; there is no early threshold stopping.
                Too few valid pairs is inconclusive and keeps Best. Search/infrastructure failure blocks promotion regardless of score. A completed game run can briefly show assessment pending before its decision is available.
                """.formatted(TrainingSettings.defaults().validationPairs(), PromotionPolicy.DEFAULT.alpha(), PromotionPolicy.DEFAULT.requiredMargin()), 14, 58);
        explanation.setName("validationInformation");
        explanation.setEditable(false);
        explanation.setLineWrap(true); explanation.setWrapStyleWord(true);
        explanation.setFont(new Font(Font.MONOSPACED, Font.PLAIN, explanation.getFont().getSize()));
        explanation.setMargin(new Insets(6, 6, 6, 6));
        explanation.setCaretPosition(0);
        JScrollPane section = new JScrollPane(explanation);
        section.setBorder(BorderFactory.createTitledBorder("Validation"));
        return section;
    }

    private static JSpinner spinner(int value, int min, int max) { return new JSpinner(new SpinnerNumberModel(value, min, max, 1)); }
    private static int value(JSpinner spinner) { return ((Number) spinner.getValue()).intValue(); }
    private void updateTerminationControls() {
        var kind = (RunTermination.Kind) termination.getSelectedItem();
        fieldVisible(generations, kind == RunTermination.Kind.GENERATIONS || kind == RunTermination.Kind.COMBINED);
        fieldVisible(runMinutes, kind == RunTermination.Kind.TIME_BUDGET || kind == RunTermination.Kind.COMBINED);
    }
    private NetworkArchitecture selectedArchitecture() { return (NetworkArchitecture) architecture.getSelectedItem(); }
    private void sourceChanged() {
        if (rebinding) return;
        if (trainingSource == null) return;
        boolean editable = controller == null || (!controller.state().active() && !controller.state().loading() && controller.state().phase() != TrainingController.Phase.CLOSING);
        updateCorpusControls(editable);
        seed.setEnabled(editable && (displayedArchitecture != NetworkArchitecture.BRN2 || (brn2.ready() && brn2.storedRunSeeds() == null)));
        start.setEnabled(trainingSource.ready() && brn2.ready() && (controller == null || controller.state().canStart()));
    }
    private void updateCorpusControls(boolean editable) {
        boolean corpus = trainingSource.corpus();
        providerSummary.setText(trainingSource.summary());
        brn2.setCorpus(corpus);
        games.setEnabled(editable && !corpus); samples.setEnabled(editable && !corpus);
        fieldVisible(games, !corpus); fieldVisible(samples, !corpus);
        fieldVisible(trainingSource.positionsControl(), corpus);
        validationMethod.setEnabled(editable);
        boolean gameValidation = validationMethod.getSelectedItem() == ValidationMethod.GAME_PAIRS;
        for (var field : List.of(depth, threads, min, max, plies)) { field.setEnabled(editable && (!corpus || gameValidation)); fieldVisible(field, !corpus || gameValidation); }
        fieldVisible(pairs, gameValidation);
        seed.setToolTipText("Controls deterministic run/shuffle streams. Fresh NNUE/BRN-3 also use it to initialize weights; BRN-1/2 use their fixed architecture initializer seeds. Recorded initialization provenance stays immutable; Resume restores the model. Training Data advances sequentially.");
        validationMethod.setToolTipText(null);
        pairs.setEnabled(editable && gameValidation);
    }

    static void fieldVisible(JComponent field, boolean visible) {
        field.setVisible(visible);
        if (field.getParent() != null) for (Component child : field.getParent().getComponents())
            if (child instanceof JLabel label && label.getLabelFor() == field) label.setVisible(visible);
    }

    static void row(JPanel panel, int row, String title, JComponent field) {
        GridBagConstraints c = new GridBagConstraints(); c.gridy = row; c.gridx = 0; c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(SeedTheme.scale(5), 0, SeedTheme.scale(5), SeedTheme.scale(10));
        JLabel label = label(title, 12, SeedTheme.SECONDARY); label.setLabelFor(field); panel.add(label, c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(SeedTheme.scale(5), 0, SeedTheme.scale(5), 0);
        Dimension preferred = field.getPreferredSize();
        field.setPreferredSize(new Dimension(preferred.width, Math.max(preferred.height, SeedTheme.scale(30))));
        field.setMinimumSize(new Dimension(SeedTheme.scale(75), SeedTheme.scale(30))); panel.add(field, c);
    }
    private static void addCard(JPanel parent, JComponent card, int row) {
        card.setMinimumSize(new Dimension(0, card.getPreferredSize().height));
        GridBagConstraints c = new GridBagConstraints(); c.gridx = 0; c.gridy = row; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, SeedTheme.scale(10), 0); parent.add(card, c);
    }
    private static final class ConfigurationCards extends JPanel implements Scrollable {
        ConfigurationCards() { super(new GridBagLayout()); setOpaque(false); }
        @Override public void doLayout() {
            for (Component child : getComponents()) if (child instanceof JComponent component)
                component.setMinimumSize(new Dimension(0, component.getPreferredSize().height));
            super.doLayout();
        }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return SeedTheme.scale(24); }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height - SeedTheme.scale(24); }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
        @Override public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize(); size.height = Math.max(size.height, getMinimumSize().height); return size;
        }
    }
}
