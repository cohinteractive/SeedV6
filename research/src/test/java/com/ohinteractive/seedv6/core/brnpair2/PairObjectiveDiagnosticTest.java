package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import java.io.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PairObjectiveDiagnosticTest {
    @Test void analyticSparseGradientsMatchIndependentFiniteDifferences() {
        var trainer=new BrnPair2Trainer(.01);var aligned=new PairObjectiveDiagnostic.Aligned(trainer);
        long[][] boards={Board.startingPosition(),Board.fromFen("4k3/8/8/3pP3/8/8/8/4K3 b - - 0 1")};double[] targets={.7,.2};
        aligned.gradients(boards,targets);int tested=0;
        for(int i=0;i<trainer.weights.length;i++)if(aligned.touched[i]&&(i%23==0||i==trainer.bias)) {
            double old=trainer.weights[i],eps=1e-5;trainer.weights[i]=old+eps;double plus=loss(trainer,boards,targets);
            trainer.weights[i]=old-eps;double minus=loss(trainer,boards,targets);trainer.weights[i]=old;
            assertEquals((plus-minus)/(2*eps),aligned.gradient[i],2e-10);tested++;
        }
        assertTrue(tested>10);
    }
    static double loss(BrnPair2Trainer t,long[][] b,double[] y) {
        double sum=0;for(int i=0;i<b.length;i++)sum+=PairObjectiveDiagnostic.objective(t.predictPawns(b[i]),BrnPair2Features.material(b[i]),y[i])[0];return sum/b.length;
    }
    @Test void compiledReadoutAndExactOptimizerContinuationMatch()throws Exception {
        var t=new BrnPair2Trainer(.01);long[][] b={Board.startingPosition()};double[] y={.8};var a=new PairObjectiveDiagnostic.Aligned(t);
        double initial=loss(t,b,y);for(int i=0;i<8;i++)a.batch(b,y);assertTrue(loss(t,b,y)<initial);
        var bytes=new ByteArrayOutputStream();t.write(bytes);var restored=BrnPair2Trainer.read(new ByteArrayInputStream(bytes.toByteArray()));
        a.batch(b,y);new PairObjectiveDiagnostic.Aligned(restored).batch(b,y);
        var left=new ByteArrayOutputStream();var right=new ByteArrayOutputStream();t.write(left);restored.write(right);assertArrayEquals(left.toByteArray(),right.toByteArray());
        double raw=t.predictPawns(b[0]),m=BrnPair2Features.material(b[0]);
        assertEquals(m+.25*(raw-m),new BrnPair2Model(t.snapshot()).newWorkspace().evaluatePawns(b[0],.25),1e-6);
    }
    @Test void wholeEpochResumePreservesShuffleAndBytes()throws Exception {
        var d=new PairObjectiveDiagnostic.Data(new long[][]{Board.startingPosition(),Board.fromFen("4k3/8/8/8/8/8/P7/4K3 b - - 0 1")},new double[]{.7,.4});
        var a=new BrnPair2Trainer(.01);PairObjectiveDiagnostic.epoch(a,d,"aligned",71,1);
        var bytes=new ByteArrayOutputStream();a.write(bytes);var b=BrnPair2Trainer.read(new ByteArrayInputStream(bytes.toByteArray()));
        PairObjectiveDiagnostic.epoch(a,d,"aligned",71,2);PairObjectiveDiagnostic.epoch(b,d,"aligned",71,2);
        var x=new ByteArrayOutputStream();var y=new ByteArrayOutputStream();a.write(x);b.write(y);assertArrayEquals(x.toByteArray(),y.toByteArray());
    }
}
