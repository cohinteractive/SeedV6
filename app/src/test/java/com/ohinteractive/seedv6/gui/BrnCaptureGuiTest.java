package com.ohinteractive.seedv6.gui;

import java.nio.file.Path;
import java.util.*;
import java.util.prefs.Preferences;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.service.*;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;
import static org.junit.jupiter.api.Assertions.*;

class BrnCaptureGuiTest {
    @TempDir Path temp;
    TrainingSettings settings() {
        return new TrainingSettings(temp.resolve("student"),1,1,4,0,0,1,1,1,2,71,8,1,NetworkArchitecture.BRN2)
                .withSupervision(BrnSupervision.blended(.5)).withSource(TrainingSource.HANDCRAFTED);
    }
    @Test void brnDefaultRoundTripAndLifecycleLocksWithNnuePanelUnchanged() throws Exception {
        var options=settings();var panel=edt(()->new TrainingPanel(options));
        until(()->edt(()->named(panel,"brn2CaptureConsistencyLambda",JSpinner.class).isEnabled()
                && named(panel,"brnTrainingSource",JComboBox.class).isEnabled()));
        var controller=edt(()->new TrainingController(options,new TrainingController.Backend(),s->{},panel::showState));
        try {
            edt(()->{
                panel.bind(controller);var field=named(panel,"brn2CaptureConsistencyLambda",JSpinner.class);
                assertEquals(0.0,field.getValue());field.setValue(2.0);assertTrue(panel.applySettings());
                assertEquals(2,controller.state().settings().config(TrainerConfig.DepthChange.REQUIRE_SAME).captureConsistency().lambda());
                for(var phase:TrainingController.Phase.values()) {
                    boolean active=List.of(TrainingController.Phase.STARTING,TrainingController.Phase.CONFIRM_DEPTH,
                            TrainingController.Phase.RUNNING,TrainingController.Phase.STOPPING).contains(phase);
                    panel.showState(new TrainingController.ViewState(controller.state().settings(),phase,null,"",active,!active,false,1,""));
                    if(active||phase==TrainingController.Phase.CLOSING) assertFalse(field.isEnabled(),phase.name());
                }
                named(panel,"networkArchitecture",JComboBox.class).setSelectedItem(NetworkArchitecture.NNUE);
                assertFalse(named(panel,"brn2Configuration",JPanel.class).isVisible());assertFalse(field.isEnabled());
            });
        } finally { edt(controller::beginShutdown).run(); }
    }
    @Test void preferencesAndDurableRootRestoreExactLambdaAndValidateTypedInput() throws Exception {
        var prefs=Preferences.userRoot().node("seedv6-capture-test-"+UUID.randomUUID());
        try {
            for(double value:new double[]{0,2,.125}) {
                var chosen=settings().withCaptureConsistency(new BrnCaptureConsistency(value));chosen.save(prefs);
                assertEquals(chosen,TrainingSettings.load(prefs));
            }
            prefs.remove("brn2Capture.lambda");assertNull(TrainingSettings.load(prefs).captureConsistency());
            assertEquals(BrnCaptureConsistency.OFF,new TrainingController.Backend().resolveSource(TrainingSettings.load(prefs)).captureConsistency());
            try(var store=new CheckpointStore(settings().root(),TrainingArchitecture.BRN2)) {store.writeBrnCaptureConsistency(new BrnCaptureConsistency(2));}
            var restored=new TrainingController.Backend().resolveSource(settings());assertEquals(2,restored.captureConsistency().lambda());
            var panel=edt(()->new Brn2ConfigurationPanel(settings()));
            edt(()->panel.selectRoot(settings().root().toString(),NetworkArchitecture.BRN2));until(()->edt(panel::ready));
            assertEquals(new BrnCaptureConsistency(2),edt(panel::readCaptureConsistency));
            for(double value:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY}) {
                edt(()->named(panel,"brn2CaptureConsistencyLambda",JSpinner.class).setValue(value));
                assertThrows(Exception.class,()->edt(panel::readCaptureConsistency));
            }
            edt(()->named(panel,"brn2CaptureConsistencyLambda",JSpinner.class).setValue(.125));
            assertEquals(new BrnCaptureConsistency(.125),edt(panel::readCaptureConsistency));
            settings().withCaptureConsistency(new BrnCaptureConsistency(.125)).save(prefs);
            TrainingSettings.defaults().save(prefs);
            assertNull(TrainingSettings.load(prefs).captureConsistency());
            assertEquals(.125,prefs.getDouble("brn2Capture.lambda",-1)); // NNUE save never writes BRN settings.
        } finally {prefs.removeNode();}
    }
}
