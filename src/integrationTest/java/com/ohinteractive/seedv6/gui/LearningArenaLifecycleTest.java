package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.search.exact.ParallelSearch;
import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.service.*;
import java.nio.file.*;
import java.util.*;
import java.util.prefs.Preferences;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;
import static com.ohinteractive.seedv6.training.service.LearningArenaConfig.*;
import static com.ohinteractive.seedv6.training.service.LearningArenaState.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real saved states and Swing actions, without rendering or training campaigns. */
@Timeout(45)
class LearningArenaLifecycleTest {
    @TempDir Path temporary;
    private Preferences preferences;
    private DataSource firstSource, secondSource;
    private final List<LearningArenaPanel> panels = new ArrayList<>();

    @BeforeEach void setup() throws Exception {
        preferences = Preferences.userRoot().node("seedv6-arena-lifecycle-" + UUID.randomUUID());
        preferences.put("baseTrainingRoot", temporary.resolve("library").toString());
        firstSource = DataSource.register("First default source", Files.writeString(temporary.resolve("first.jsonl"), SourceReadersTest.line(100)), 1);
        secondSource = DataSource.register("Second campaign source", Files.writeString(temporary.resolve("second.jsonl"), SourceReadersTest.line(200)), 3);
        var library = new TrainingDataLibrary(new TrainingFolders(preferences).base());
        library.register(firstSource); library.register(secondSource);
    }
    @AfterEach void cleanup() throws Exception {
        for (var panel : panels) edt(panel::beginShutdown).run();
        preferences.removeNode(); preferences.flush();
    }

    @Test void laterSessionRestoresEverySavedFieldAndCopiedModelWithoutStartingWork() throws Exception {
        var config = custom("Persisted A", secondSource);
        Path root = saveStarted("campaign-a", config);
        byte[] before = Files.readAllBytes(root.resolve("campaign.json"));
        var first = panel(); open(first, root); assertSetup(first, config);
        assertReadOnly(first);
        edt(first::beginShutdown).run(); panels.remove(first);
        preferences.flush();
        assertEquals(root, new TrainingFolders(Preferences.userRoot().node(preferences.absolutePath())).arenaCampaign());
        var restarted = panel(); awaitCampaign(restarted, config.name());
        assertSetup(restarted, config); assertReadOnly(restarted);
        assertArrayEquals(before, Files.readAllBytes(root.resolve("campaign.json")), "Open/startup must not rewrite execution evidence");
        assertEquals(0, LearningArenaState.read(root).current().number());
        assertFalse(Files.exists(root.resolve("B")), "Restoration must not start the uninitialized competitor");
    }

    @Test void openBThenReopenAAndReopenSameHashAlwaysHydrates() throws Exception {
        var a = custom("Campaign A", secondSource);
        var b = new LearningArenaConfig("Campaign B", new Competitor("B first", TrainingArchitecture.NNUE, 5, 7),
                new Competitor("B second", TrainingArchitecture.BRN3, 6, 9, .008, null), firstSource,
                19, 2, 4, 123, new Arena(2, Limit.DEPTH, 2, 800, 0, 0, 2, 40, TrainerConfig.STANDARD_START));
        Path rootA = saveStarted("a", a), rootB = saveStarted("b", b);
        byte[] beforeA = Files.readAllBytes(rootA.resolve("campaign.json")), beforeB = Files.readAllBytes(rootB.resolve("campaign.json"));
        var panel = panel(); open(panel, rootA); assertSetup(panel, a);
        open(panel, rootB); assertSetup(panel, b);
        open(panel, rootA); assertSetup(panel, a);
        // Deliberately simulate stale controls; users cannot edit this read-only view.
        edt(() -> {
            named(panel, "arenaName", JTextField.class).setText("Stale draft");
            named(panel, "arenaArchitectureA", JComboBox.class).setSelectedItem(TrainingArchitecture.NNUE_MATERIAL);
            named(panel, "arenaEpochs", JSpinner.class).setValue(1);
        });
        open(panel, rootA); assertSetup(panel, a); assertReadOnly(panel);
        assertEquals(rootA, new TrainingFolders(preferences).arenaCampaign());
        assertArrayEquals(beforeA, Files.readAllBytes(rootA.resolve("campaign.json")));
        assertArrayEquals(beforeB, Files.readAllBytes(rootB.resolve("campaign.json")));
    }

    @Test void startNewResetsEveryControlAndBothModelsToConstructorDefaults() throws Exception {
        var panel = panel(); awaitDraft(panel);
        var defaults = edt(() -> controls(panel));
        assertEquals(TrainingArchitecture.NNUE_MATERIAL, defaults.get("arenaArchitectureA"));
        assertEquals(TrainingArchitecture.BRN3, defaults.get("arenaArchitectureB"));
        var config = custom("Customized", secondSource); Path root = saveStarted("custom", config);
        byte[] before = Files.readAllBytes(root.resolve("campaign.json"));
        open(panel, root); assertSetup(panel, config);
        edt(() -> named(panel, "arenaStart", JButton.class).doClick(0));
        awaitDraft(panel);
        assertEquals(defaults, edt(() -> controls(panel)));
        assertNull(new TrainingFolders(preferences).arenaCampaign());
        edt(() -> {
            assertFalse(named(panel, "arenaResume", JButton.class).isEnabled());
            assertEquals(0, named(panel, "arenaHistory", JTable.class).getRowCount());
            assertTrue(named(panel, "arenaName", JTextField.class).isEnabled());
            assertEquals(0, named(panel, "arenaViews", JTabbedPane.class).getSelectedIndex());
            named(panel, "arenaName", JTextField.class).setText("Deliberate next draft");
            named(panel, "arenaEpochs", JSpinner.class).setValue(3);
        });
        assertArrayEquals(before, Files.readAllBytes(root.resolve("campaign.json")), "Draft edits cannot mutate the old campaign");
        edt(() -> named(panel, "arenaStart", JButton.class).doClick(0)); awaitDraft(panel);
        assertEquals(defaults, edt(() -> controls(panel)), "Start new also resets an abandoned draft");
    }

    @Test void missingCorruptAndInvalidRememberedCampaignsFallBackWithoutSubstitution() throws Exception {
        var good = custom("Unrelated existing campaign", firstSource);
        saveStarted("unrelated", good);
        Path corrupt = temporary.resolve("corrupt"); Files.createDirectories(corrupt);
        Files.writeString(corrupt.resolve("campaign.json"), "{invalid campaign}");
        for (String path : List.of(temporary.resolve("missing").toString(), corrupt.toString(), "invalid|campaign")) {
            preferences.put("lastArenaCampaign", path);
            var panel = panel(); awaitDraft(panel);
            edt(() -> {
                assertEquals("Learning campaign", named(panel, "arenaName", JTextField.class).getText());
                assertFalse(named(panel, "arenaResume", JButton.class).isEnabled());
                assertTrue(named(panel, "arenaName", JTextField.class).isEnabled());
                assertEquals(TrainingArchitecture.NNUE_MATERIAL, named(panel, "arenaArchitectureA", JComboBox.class).getSelectedItem());
            });
            assertNull(new TrainingFolders(preferences).arenaCampaign());
            assertNull(preferences.get("lastArenaCampaign", null));
            edt(panel::beginShutdown).run(); panels.remove(panel);
        }
    }

    @Test void campaignSourceRemainsExactWhenTheLibraryHasAnotherDescriptorForItsIdentity() throws Exception {
        var renamed = secondSource.withDisplay("Catalog display name", 1);
        new TrainingDataLibrary(new TrainingFolders(preferences).base()).register(renamed);
        var config = custom("Frozen source", secondSource);
        var panel = panel(); open(panel, saveStarted("frozen", config));
        assertSetup(panel, config);
        assertEquals(secondSource, edt(() -> named(panel, "arenaSourceLibrary", TrainingDataSelector.class).selected()));
        Files.delete(secondSource.path());
        open(panel, temporary.resolve("frozen"));
        assertSetup(panel, config);
        assertFalse(edt(() -> named(panel, "arenaSourceLibrary", TrainingDataSelector.class).ready()));
    }

    @Test void resumeRehydratesSavedConfigurationEvenAfterSameCampaignWasAlreadyShown() throws Exception {
        var config = new LearningArenaConfig("Resume fixture", new Competitor("First", TrainingArchitecture.BRN3, 71, 2),
                new Competitor("Second", TrainingArchitecture.BRN3, 97, 2), firstSource, 2, 1, 1, 42,
                new Arena(2, Limit.DEPTH, 1, 100, 1, 0, 0, 4, "7k/5K2/6Q1/8/8/8/8/8 w - - 0 1"));
        Path root = temporary.resolve("resume");
        try (var owner = LearningArenaService.create(root, config, ignored -> {})) { assertEquals(config, owner.state().config()); }
        var panel = panel(); open(panel, root);
        edt(() -> {
            named(panel, "arenaName", JTextField.class).setText("Stale controls");
            named(panel, "arenaArchitectureA", JComboBox.class).setSelectedItem(TrainingArchitecture.BRN_PAIR2);
            named(panel, "arenaResume", JButton.class).doClick(0);
            named(panel, "arenaPause", JButton.class).doClick(0);
        });
        awaitCampaign(panel, config.name()); assertSetup(panel, config); assertReadOnly(panel);
        assertEquals(config, LearningArenaState.read(root).config());
        assertEquals(config.identity(), LearningArenaState.read(root).binding());
    }

    @Test void createAndRunCommitsDeliberateDraftEditsAndRemembersTheNewCampaign() throws Exception {
        var panel = panel(); awaitDraft(panel);
        edt(() -> {
            named(panel, "arenaName", JTextField.class).setText("New committed campaign");
            named(panel, "arenaArchitectureA", JComboBox.class).setSelectedItem(TrainingArchitecture.BRN3);
            named(panel, "arenaPositions", JSpinner.class).setValue(2);
            named(panel, "arenaEpochs", JSpinner.class).setValue(1);
            named(panel, "arenaRounds", JSpinner.class).setValue(1);
            named(panel, "arenaGames", JSpinner.class).setValue(2);
            named(panel, "arenaDepth", JSpinner.class).setValue(1);
            named(panel, "arenaOpeningMax", JSpinner.class).setValue(0);
            named(panel, "arenaPlyCap", JSpinner.class).setValue(4);
            named(panel, "arenaFen", JTextField.class).setText("7k/5K2/6Q1/8/8/8/8/8 w - - 0 1");
            named(panel, "arenaCreate", JButton.class).doClick(0);
            named(panel, "arenaPause", JButton.class).doClick(0);
        });
        awaitCampaign(panel, "New committed campaign");
        Path root = new TrainingFolders(preferences).arenaCampaign(); assertNotNull(root);
        var config = LearningArenaState.read(root).config();
        assertEquals("New committed campaign", config.name()); assertEquals(2, config.positionsPerRound());
        assertEquals(TrainingArchitecture.BRN3, config.a().architecture());
        assertSetup(panel, config); assertReadOnly(panel);
    }

    @Test void legacyOptionalFieldsAndPair2TokenRemainCompatible() throws Exception {
        var config = new LearningArenaConfig("Legacy", new Competitor("Legacy NNUE", TrainingArchitecture.NNUE, 11, 12),
                new Competitor("Legacy pair model", TrainingArchitecture.BRN_PAIR2, 13, 14), firstSource, 30, 2, 3, 45,
                new Arena(2, Limit.DEPTH, 1, 100, 1, 0, 0, 10, TrainerConfig.STANDARD_START));
        Path root = saveStarted("legacy", config);
        String before = Files.readString(root.resolve("campaign.json"));
        assertFalse(before.contains("learningRate")); assertFalse(before.contains("initialModel")); assertFalse(before.contains("nnueObjective"));
        assertTrue(before.contains("BRN_PAIR2"));
        var panel = panel(); open(panel, root); assertSetup(panel, config);
        edt(() -> {
            var selector = named(panel, "arenaArchitectureB", JComboBox.class);
            assertEquals("BRE-Pair 2", ((JLabel) selector.getRenderer().getListCellRendererComponent(new JList<>(), selector.getSelectedItem(), 0, false, false)).getText());
        });
        assertEquals(before, Files.readString(root.resolve("campaign.json")));
    }

    private LearningArenaPanel panel() throws Exception {
        var panel = edt(() -> { SeedTheme.initialize(); return new LearningArenaPanel(new TrainingFolders(preferences)); });
        panels.add(panel); return panel;
    }
    private void open(LearningArenaPanel panel, Path root) throws Exception {
        edt(() -> panel.openCampaign(root)); awaitCampaign(panel, LearningArenaState.read(root).config().name());
    }
    private void awaitCampaign(LearningArenaPanel panel, String name) throws Exception {
        until(() -> edt(() -> { panel.poll(); return named(panel, "arenaOpen", JButton.class).isEnabled()
                && name.equals(named(panel, "arenaName", JTextField.class).getText())
                && !named(panel, "arenaSourceDetails", JTextArea.class).getText().startsWith("Checking"); }));
    }
    private void awaitDraft(LearningArenaPanel panel) throws Exception {
        until(() -> edt(() -> { panel.poll(); return named(panel, "arenaOpen", JButton.class).isEnabled()
                && named(panel, "arenaCreate", JButton.class).isEnabled(); }));
    }
    private LearningArenaConfig custom(String name, DataSource source) {
        var model = new InitialModel(temporary.resolve("original-model").toString(), UUID.randomUUID().toString(), "Saved checkpoint model", "g000007-s000000123-" + "f".repeat(64), 7);
        return new LearningArenaConfig(name, new Competitor("Custom pair model", TrainingArchitecture.BRN_PAIR2, 1234, 17, null, model),
                new Competitor("Custom NNUE", TrainingArchitecture.NNUE, 4321, 23, .007, null), source, 37, 3, 5, 8765,
                new Arena(6, Limit.TIME, 7, 2345, ParallelSearch.MAX_WORKERS, 2, 6, 77, "7k/5K2/6Q1/8/8/8/8/8 w - - 0 1"));
    }
    private Path saveStarted(String directory, LearningArenaConfig config) throws Exception {
        Path root = temporary.resolve(directory);
        LearningArenaState state;
        try (var owner = LearningArenaService.create(root, config, ignored -> {})) { state = owner.state(); }
        // A durable initialized-A receipt makes this a started experiment; no training is needed for UI tests.
        var round = new Round(0, "", 0, new Endpoint("g000000-s000000000-" + "a".repeat(64), 0, 0, 0), null, List.of(), false);
        DataFiles.write(root.resolve("campaign.json"), new LearningArenaState(1, state.id(), state.binding(), state.config(), List.of(round), Status.PAUSED, "Fixture paused"));
        return root;
    }
    private void assertReadOnly(LearningArenaPanel panel) throws Exception {
        edt(() -> {
            for (String key : controls(panel).keySet()) {
                var component = named(panel, key, JComponent.class);
                if (component != null && !(component instanceof JLabel)) assertFalse(component.isEnabled(), key);
            }
            for (String side : List.of("A", "B")) {
                assertFalse(named(panel, "arenaSelectModel" + side, JButton.class).isEnabled());
                assertFalse(named(panel, "arenaFreshModel" + side, JButton.class).isEnabled());
            }
            assertFalse(named(panel, "arenaCreate", JButton.class).isEnabled());
            assertTrue(named(panel, "arenaStart", JButton.class).isEnabled());
        });
    }
    private void assertSetup(LearningArenaPanel panel, LearningArenaConfig c) throws Exception {
        var expected = new LinkedHashMap<String, Object>();
        expected.put("arenaName", c.name()); expected.put("arenaSource", c.source()); expected.put("arenaFen", c.arena().startingFen());
        expected.put("arenaPositions", c.positionsPerRound()); expected.put("arenaEpochs", c.epochs()); expected.put("arenaRounds", c.rounds()); expected.put("arenaSeed", Long.toString(c.seed()));
        var v = c.arena(); expected.put("arenaGames", v.games()); expected.put("arenaLimit", v.limit()); expected.put("arenaDepth", v.depth());
        expected.put("arenaMillis", (int) v.millis()); expected.put("arenaThreads", v.threads()); expected.put("arenaOpeningMin", v.openingMin());
        expected.put("arenaOpeningMax", v.openingMax()); expected.put("arenaPlyCap", v.maximumPlies());
        expectedCompetitor(expected, "A", c.a()); expectedCompetitor(expected, "B", c.b());
        assertEquals(expected, edt(() -> controls(panel)));
    }
    private static void expectedCompetitor(Map<String, Object> expected, String side, Competitor c) {
        expected.put("arenaName" + side, c.name()); expected.put("arenaSeed" + side, Long.toString(c.seed())); expected.put("arenaArchitecture" + side, c.architecture());
        expected.put("arenaBatch" + side, c.minibatch()); expected.put("arenaInheritRate" + side, c.learningRate() == null);
        expected.put("arenaLearningRate" + side, c.learningRate() == null ? TrainingRecipe.defaults(c.architecture()).learningRate() : c.learningRate());
        var model = c.initialModel(); expected.put("arenaInitialModel" + side, model == null ? "Fresh initialization" : model.name() + " - Gen " + model.generation());
        expected.put("modelBinding" + side, model == null ? null : model.root() + " / " + model.checkpoint());
    }
    private static Map<String, Object> controls(LearningArenaPanel panel) {
        var values = new LinkedHashMap<String, Object>();
        for (String name : List.of("arenaName", "arenaFen", "arenaSeed", "arenaNameA", "arenaSeedA", "arenaNameB", "arenaSeedB"))
            values.put(name, named(panel, name, JTextField.class).getText());
        for (String name : List.of("arenaPositions", "arenaEpochs", "arenaRounds", "arenaGames", "arenaDepth", "arenaMillis", "arenaThreads", "arenaOpeningMin", "arenaOpeningMax", "arenaPlyCap", "arenaBatchA", "arenaBatchB", "arenaLearningRateA", "arenaLearningRateB"))
            values.put(name, named(panel, name, JSpinner.class).getValue());
        for (String name : List.of("arenaSource", "arenaLimit", "arenaArchitectureA", "arenaArchitectureB"))
            values.put(name, named(panel, name, JComboBox.class).getSelectedItem());
        for (String side : List.of("A", "B")) {
            values.put("arenaInheritRate" + side, named(panel, "arenaInheritRate" + side, JCheckBox.class).isSelected());
            var label = named(panel, "arenaInitialModel" + side, JLabel.class);
            values.put(label.getName(), label.getText()); values.put("modelBinding" + side, label.getToolTipText());
        }
        return values;
    }
}
