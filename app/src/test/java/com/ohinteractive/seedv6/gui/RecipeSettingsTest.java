package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.training.service.*;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.*;
import java.util.prefs.Preferences;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;
import javax.swing.*;

class RecipeSettingsTest {
    @TempDir Path root;
    @Test void ratesRoundTripAndCopiesPreserveThemForEveryArchitecture() throws Exception {
        var prefs = Preferences.userRoot().node("seedv6-tests/recipe-" + UUID.randomUUID());
        try {
            for (var architecture : NetworkArchitecture.values()) {
                var s = TrainingSettings.defaults(root, architecture).withLearningRate(.007)
                        .withTimeLimit(9).withValidationMethod(ValidationMethod.HELD_OUT)
                        .withCorpus("", null).withTeacherStore(null).withRunSeeds(null).withSupervision(null).withCaptureConsistency(null);
                assertEquals(.007, s.recipeLearningRate()); assertEquals(.007, s.config(TrainerConfig.DepthChange.REQUIRE_SAME).training().learningRate());
                assertEquals(s, LineageConfiguration.decode(LineageConfiguration.encode(s), root, architecture));
                s.save(prefs); assertEquals(.007, TrainingSettings.load(prefs).recipeLearningRate());
                s.withLearningRate(null).save(prefs); assertNull(TrainingSettings.load(prefs).recipeLearningRate());
            }
        } finally { prefs.removeNode(); }
    }
    @Test void v3ConfigurationAndNewManagedDefaultsStayDistinct() throws Exception {
        var old = TrainingSettings.defaults(root, NetworkArchitecture.NNUE);
        byte[] bytes = Base64.getDecoder().decode(LineageConfiguration.encode(old));
        ByteBuffer.wrap(bytes).putInt(3); bytes = Arrays.copyOf(bytes, bytes.length - 1);
        var decoded = LineageConfiguration.decode(Base64.getEncoder().encodeToString(bytes), root, old.architecture());
        assertEquals(old, decoded); assertNull(decoded.config(TrainerConfig.DepthChange.REQUIRE_SAME).training().learningRate());
        for (var architecture : List.of(NetworkArchitecture.NNUE, NetworkArchitecture.NNUE_MATERIAL, NetworkArchitecture.BRN3)) {
            var created = TrainingLineages.read(TrainingLineages.create(root, architecture, "Recipe " + architecture));
            assertEquals(architecture == NetworkArchitecture.BRN3 ? .003 : .001, created.settings().recipeLearningRate());
        }
    }
    @Test void sharedRateEditorPreservesLegacyAbsenceAndExplicitValues() throws Exception {
        edt(() -> {
            var panel = new RecipeLearningRatePanel(TrainingSettings.defaults(root, NetworkArchitecture.NNUE));
            var inherit = named(panel, "inheritLearningRate", JCheckBox.class);
            var rate = named(panel, "recipeLearningRate", JSpinner.class);
            assertTrue(inherit.isSelected()); assertFalse(rate.isEnabled()); assertNull(assertDoesNotThrow(panel::read));
            inherit.doClick(); rate.setValue(.008); assertEquals(.008, assertDoesNotThrow(panel::read));
            panel.setEditable(false); assertFalse(inherit.isEnabled()); assertFalse(rate.isEnabled());
            panel.load(TrainingSettings.defaults(root, NetworkArchitecture.BRN3).withLearningRate(.005));
            assertFalse(inherit.isSelected()); assertEquals(.005, assertDoesNotThrow(panel::read));
        });
    }
}
