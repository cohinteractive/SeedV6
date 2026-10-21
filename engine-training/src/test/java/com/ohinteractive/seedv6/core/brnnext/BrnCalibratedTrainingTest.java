package com.ohinteractive.seedv6.core.brnnext;

import java.io.*;
import java.util.*;
import com.ohinteractive.seedv6.core.brn3.Brn3Objective;
import com.ohinteractive.seedv6.training.nnue.NnueCorpusTargets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.core.brnnext.BrnNextArchitecture.*;

class BrnCalibratedTrainingTest {
    static byte[] bytes(BrnNextTrainer trainer) throws IOException {
        var out=new ByteArrayOutputStream();trainer.write(out);return out.toByteArray();
    }
    @ParameterizedTest @EnumSource(value=BrnNextArchitecture.class,names={"GEN32_CAL_AFFINE_R4","GEN32_CAL_QUADRATIC_R4","GEN32_CAL_CUBIC_R4"})
    void fixedSourceObjectiveGradientAndResumeAreExplicit(BrnNextArchitecture kind) throws Exception {
        var q=new BrnNextTrainer(GEN32_QUADRATIC_R4,31,.001);
        var boards=BrnNextMathTest.BOARDS;double[] targets={.6,-.4,.2};
        for(int i=0;i<16;i++)q.trainBatch(boards,targets,3);
        var source=q.freezeQuadraticBasis(true);
        for(int i=0;i<8;i++)source.trainBatch(boards,targets,3);
        byte[] original=bytes(source);var t=source.calibrateFixedBasis(kind);
        assertEquals(0,t.step());assertArrayEquals(Arrays.copyOf(source.weights,3845),Arrays.copyOf(t.weights,3845));
        assertTrue(Arrays.stream(t.weights,3845,t.weights.length).allMatch(x->x==0));
        assertTrue(Arrays.stream(t.first).allMatch(x->x==0));assertTrue(Arrays.stream(t.second).allMatch(x->x==0));
        for(var board:boards) {
            double g=BrnNextBase.model().newWorkspace().evaluatePawns(board),m=com.ohinteractive.seedv6.core.brnpair2.BrnPair2Features.material(board);
            assertEquals(g+.5*(q.predictPawns(board)-g),t.predictPawns(board),1e-12);
            assertEquals(m+.25*(t.predictPawns(board)-m),t.objectivePawns(board));
            assertEquals(Brn3Objective.outcome(t.objectivePawns(board),board),t.predictOutcome(board));
        }
        // Finite differences of the *calibrated objective*, not the implementation's backward routine.
        var finite=new double[t.weights.length];
        for(int j=kind.trainableStart();j<t.weights.length;j++) {
            double sum=0,h=1e-5,old=t.weights[j];
            for(int i=0;i<boards.length;i++) {
                t.weights[j]=old+h;double plus=Brn3Objective.crossEntropy(t.objectivePawns(boards[i]),NnueCorpusTargets.material(boards[i]),targets[i])[0];
                t.weights[j]=old-h;double minus=Brn3Objective.crossEntropy(t.objectivePawns(boards[i]),NnueCorpusTargets.material(boards[i]),targets[i])[0];
                sum+=(plus-minus)/(2*h*boards.length);
            }
            t.weights[j]=old;finite[j]=sum;
        }
        var anchor=Arrays.copyOf(t.weights,3845);t.trainBatch(boards,targets,3);
        for(int j=3845;j<t.weights.length;j++) {
            assertEquals(finite[j],t.gradient[j]/boards.length,2e-8,"gradient "+j);
            assertEquals(-.001*finite[j]/(Math.abs(finite[j])+1e-8),t.weights[j],2e-7,"Adam "+j);
        }
        for(int i=0;i<16;i++)t.trainBatch(boards,targets,3);
        assertArrayEquals(anchor,Arrays.copyOf(t.weights,3845));
        assertTrue(Arrays.stream(t.first,0,3845).allMatch(x->x==0));assertTrue(Arrays.stream(t.second,0,3845).allMatch(x->x==0));
        var restored=BrnNextTrainer.read(kind,new ByteArrayInputStream(bytes(t)));
        t.trainBatch(boards,targets,3);restored.trainBatch(boards,targets,3);assertArrayEquals(bytes(t),bytes(restored));
        var encoded=new ByteArrayOutputStream();BrnNextCodec.writeModel(t.snapshot(),encoded);
        var model=BrnNextCodec.readModel(kind,new ByteArrayInputStream(encoded.toByteArray()));
        var heldOut=com.ohinteractive.seedv6.training.validation.HeldOutLoss.predictor(new com.ohinteractive.seedv6.training.model.NetworkModel.BrnNext(model));
        for(var board:boards) {
            assertEquals(t.predictPawns(board),model.rawPawns(board,model.newWorkspace()));
            assertEquals(t.objectivePawns(board),model.searchPawns(board,model.newWorkspace()));
            assertEquals(t.predictOutcome(board),heldOut.applyAsDouble(board));
        }
        restored.first[0]=.1;assertThrows(IOException.class,()->BrnNextTrainer.read(kind,new ByteArrayInputStream(bytes(restored))));
        assertThrows(IOException.class,()->BrnNextCodec.readModel(GEN32_FIXED_CUBIC_R4,new ByteArrayInputStream(encoded.toByteArray())));
        assertArrayEquals(original,bytes(source));
    }
}
