package com.ohinteractive.seedv6.search.evaluation;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Brn3SearchCalibrationTest {
    @Test void calibrationRetainsMaterialAndCodecWeightsWhileChangingOnlyTheResidual() throws Exception {
        var weights=new float[Brn3Model.PARAMETER_COUNT];weights[Brn3Layout.OUTPUT_BIAS]=8;
        var model=new Brn3Model(weights);var bytes=Brn3Codec.encodeModel(model);
        var restored=Brn3Codec.decodeModel(bytes);var raw=restored.newWorkspace();
        var production=SearchEvaluation.brn3(restored).newState(4);
        var original=SearchEvaluation.brn3Research(restored,1).newState(4);
        for(String side:new String[]{"w","b"}) {
            var b=Board.fromFen("4k3/8/8/8/8/8/PNBRQ3/4K3 "+side+" - - 0 1");
            double material=side.equals("w")?21.5:-21.5;
            assertEquals(material+8,raw.evaluatePawns(b),1e-12);
            assertEquals(Brn3Model.score(material+2),production.evaluate(b,0));
            assertEquals(Brn3Model.score(material+8),original.evaluate(b,0));
            assertEquals(material,raw.evaluatePawns(b,0),1e-12);
            var forbidden=b.clone();forbidden[4]=(b[4]&Board.PLAYER_BIT)|(~b[4]&~Board.PLAYER_BIT);forbidden[5]=~b[5];
            assertEquals(production.evaluate(b,0),production.evaluate(forbidden,0));
        }
        assertArrayEquals(bytes,Brn3Codec.encodeModel(restored));
        assertThrows(IllegalArgumentException.class,()->SearchEvaluation.brn3Research(model,Double.NaN));
        assertThrows(IllegalArgumentException.class,()->SearchEvaluation.brn3Research(model,-.1));
    }
    @Test void freshMaterialAndLegacyPayloadInferenceRemainReproducible() throws Exception {
        var trainer=new Brn3Trainer(71);var restored=Brn3Codec.decodeTraining(Brn3Codec.encodeTraining(trainer));
        var board=Board.fromFen("4k3/8/8/8/8/8/PNBRQ3/4K3 w - - 0 1");
        var model=restored.snapshot();
        assertEquals(2150,SearchEvaluation.brn3(model).newState(4).evaluate(board,0));
        assertEquals(trainer.predictPawns(board),model.newWorkspace().evaluatePawns(board),1e-12);
    }
}
