package com.ohinteractive.seedv6.core.brn3;

import static com.ohinteractive.seedv6.core.brn3.Brn3Layout.*;
import com.ohinteractive.seedv6.core.*;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BrnSplineTrainerTest {
    @Test void independentKnotsInterpolationTailAndLinearSubmodel() {
        double[] values={1,-2,4,1,2,3,4,5,6,7,8,9,10,11,12,14};
        assertEquals(0,BrnSplineTrainer.curve(values,0,0),0);
        assertEquals(.25,BrnSplineTrainer.curve(values,0,.25),0);
        assertEquals(.5,BrnSplineTrainer.curve(values,0,.5),0);
        assertEquals(-.25,BrnSplineTrainer.curve(values,0,.75),0);
        assertEquals(7,BrnSplineTrainer.curve(values,0,8),0);
        assertEquals(7.5,BrnSplineTrainer.curve(values,0,8.25),0);
        assertEquals(-3,(BrnSplineTrainer.curve(values,0,.5000001)-BrnSplineTrainer.curve(values,0,.5))/1e-7,1e-8);
        var linear=new BrnEnergyTrainer(0,71,.003);var spline=new BrnSplineTrainer(16,71,.003);
        for(int i=0;i<DENSE;i++)assertEquals(linear.weights[i],spline.weights[i],0);
        var random=new Random(71);
        for(int j=0;j<192;j++){double w=random.nextDouble()-.5;linear.weights[DENSE+j]=w;for(int k=1;k<=16;k++)spline.weights[DENSE+j*16+k-1]=k*w;}
        for(var board:BrnEnergyTrainerTest.examples())assertEquals(linear.predictPawns(board),spline.predictPawns(board),1e-12);
        for(int j=0;j<192;j++)for(double x:new double[]{0,.1,.5,1,7.3,8,12})assertEquals(linear.weights[DENSE+j]*x,BrnSplineTrainer.curve(spline.weights,DENSE+j*16,x),1e-14);
        for(int j=0;j<192;j++)for(int k=2;k<=16;k++)spline.weights[DENSE+j*16+k-1]=random.nextDouble()-.5;
        var snapshot=spline.snapshot();float[] before=snapshot.weights.clone();var ablated=snapshot.linearized();
        for(var board:BrnEnergyTrainerTest.examples())assertEquals(linear.predictPawns(board),ablated.newWorkspace().evaluatePawns(board),1e-6);
        assertArrayEquals(before,snapshot.weights);
    }
    @Test void independentForwardAndAllBlockGradientsIncludingExtrapolation() {
        var board=BrnEnergyTrainerTest.examples()[0];
        for(double spacing:new double[]{.5,.125})for(double scale:new double[]{1,100}) {
            var model=new BrnSplineTrainer(16,97,.003,spacing);for(int i=0;i<DENSE;i++)model.weights[i]*=scale;
            for(int i=DENSE;i<model.output;i++)model.weights[i]=.13*Math.sin(i-DENSE+.7);
            double actual=model.predictPawns(board),expected=Brn3Features.material(board)+model.weights[model.output];
            for(int j=0;j<192;j++) {
                double x=model.pooled[j/96][j%96];int upper=1;while(upper<16&&x>upper*spacing)upper++;
                double lo=upper==1?0:spacing*model.weights[DENSE+j*16+upper-2],hi=spacing*model.weights[DENSE+j*16+upper-1];
                expected+=lo+(hi-lo)*(x-(upper-1)*spacing)/spacing;
            }
            assertEquals(expected,actual,1e-12);if(scale>1)assertTrue(model.pooledAboveGrid()>0);
            model.resetGradient();model.backward(1);double[] g=model.gradient.clone();int[] bounds={0,NODES,DENSE,model.output,model.modelParameters,g.length};
            for(int block=0;block<bounds.length-1;block++) {
                int chosen=0;for(int i=bounds[block];i<bounds[block+1]&&chosen<12;i++)if(Math.abs(g[i])>1e-10) {
                    double prior=model.weights[i];model.weights[i]=prior+1e-6;double plus=model.predictPawns(board);
                    model.weights[i]=prior-1e-6;double minus=model.predictPawns(board);model.weights[i]=prior;
                    assertEquals(g[i],(plus-minus)/2e-6,2e-7,"parameter "+i);chosen++;
                }
                assertTrue(chosen>0,"Unobserved block "+block);
            }
        }
    }
    @Test void tinyFitInformationAndColorSymmetry() {
        for(double spacing:new double[]{.5,.125})verifyTinyFit(spacing);
    }
    private void verifyTinyFit(double spacing) {
        var model=new BrnSplineTrainer(16,71,.003,spacing);var boards=BrnEnergyTrainerTest.examples();double[] targets={.2,-.3,-.4};
        assertEquals(Brn3Features.material(boards[0]),model.predictPawns(boards[0]),0);
        // Fixed diagnostic extension: retain the same .025 fit criterion and inspect
        // convergence at1000/2000/4000, rather than relaxing a failed fine-grid gate.
        for(int i=1;i<=4000;i++) {
            model.trainBatch(boards,targets,3);
            if(i==1000||i==2000||i==4000) {
                double[] outputs=new double[3];for(int j=0;j<3;j++)outputs[j]=Brn3Objective.smoothOutcome(model.predictPawns(boards[j]),com.ohinteractive.seedv6.training.nnue.NnueCorpusTargets.material(boards[j]))[0];
                System.out.println("E01 tiny spacing="+spacing+" updates="+i+" outcomes="+Arrays.toString(outputs));
            }
        }
        for(int i=0;i<3;i++)assertEquals(targets[i],Brn3Objective.smoothOutcome(model.predictPawns(boards[i]),com.ohinteractive.seedv6.training.nnue.NnueCorpusTargets.material(boards[i]))[0],.025);
        var altered=boards[0].clone();altered[4]=(altered[4]&Board.PLAYER_BIT)|(~altered[4]&~Board.PLAYER_BIT);altered[5]^=54321;
        assertEquals(model.predictPawns(boards[0]),model.predictPawns(altered),0);
        assertEquals(model.predictPawns(boards[0]),model.predictPawns(Board.fromFen("rnbqkb1r/pppp1ppp/5n2/4p3/4P3/2N5/PPPP1PPP/R1BQKBNR b KQkq - 2 3")),1e-10);
    }
    @Test void exactContinuationCrcImmutabilityAndIncrementalParity()throws Exception {
        for(double spacing:new double[]{.5,.125})verifyContinuation(spacing);
    }
    private void verifyContinuation(double spacing)throws Exception {
        var model=new BrnSplineTrainer(16,71,.003,spacing);var boards=BrnEnergyTrainerTest.examples();double[] targets={.2,-.3,-.4};
        for(int i=0;i<6;i++)model.trainBatch(boards,targets,3);
        var bytes=new ByteArrayOutputStream();model.write(bytes);byte[] state=bytes.toByteArray();var restored=BrnSplineTrainer.read(new ByteArrayInputStream(state));
        assertEquals(spacing,restored.spacing(),0);
        if(spacing==.5){var old=BrnSplineTrainer.read(new ByteArrayInputStream(legacy(state)));assertArrayEquals(model.weights,old.weights);assertArrayEquals(model.first,old.first);assertArrayEquals(model.second,old.second);assertEquals(model.step(),old.step());assertEquals(.5,old.spacing(),0);}
        model.trainBatch(boards,targets,3);restored.trainBatch(boards,targets,3);assertArrayEquals(model.weights,restored.weights);assertArrayEquals(model.first,restored.first);assertArrayEquals(model.second,restored.second);assertEquals(model.step(),restored.step());
        state[state.length/2]^=1;assertThrows(IOException.class,()->BrnSplineTrainer.read(new ByteArrayInputStream(state)));
        bytes.reset();model.snapshot().write(bytes);byte[] payload=bytes.toByteArray();var snapshot=BrnSplineTrainer.Model.read(new ByteArrayInputStream(payload));var work=snapshot.newWorkspace();
        assertEquals(spacing,snapshot.spacing,0);
        if(spacing==.5){var old=BrnSplineTrainer.Model.read(new ByteArrayInputStream(legacy(payload)));assertArrayEquals(snapshot.weights,old.weights);assertEquals(.5,old.spacing,0);}
        payload[payload.length/2]^=1;assertThrows(IOException.class,()->BrnSplineTrainer.Model.read(new ByteArrayInputStream(payload)));assertThrows(IOException.class,()->BrnSplineTrainer.Model.read(new ByteArrayInputStream(Arrays.copyOf(payload,24))));
        long[] moves=new long[512];var random=new Random(12741);var board=Board.startingPosition();
        String[] special={"r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1","4k3/P7/8/8/8/8/7p/4K3 w - - 0 1"};
        for(String fen:special) {
            var parent=Board.fromFen(fen);int n=Gen.genAll(parent[0],parent[1],parent[2],parent[3],(int)parent[4],parent[5],true,moves,new long[6]);
            for(int i=0;i<n;i++){var child=new long[6];Board.makeMoveInto(parent[0],parent[1],parent[2],parent[3],(int)parent[4],parent[5],moves[i],child);
                assertEquals(model.predictPawns(parent),work.evaluatePawns(parent),1e-4);assertEquals(model.predictPawns(child),work.evaluatePawns(child),1e-4);assertEquals(model.predictPawns(parent),work.evaluatePawns(parent),1e-4);}
        }
        for(int ply=0;ply<800;ply++) {
            assertEquals(model.predictPawns(board),work.evaluatePawns(board),1e-4);assertEquals(snapshot.newWorkspace().evaluatePawns(board),work.evaluatePawns(board),1e-10);
            int n=Gen.genAll(board[0],board[1],board[2],board[3],(int)board[4],board[5],true,moves,new long[6]);
            if(n==0||ply%99==98){board=Board.startingPosition();continue;}
            var child=new long[6];Board.makeMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],moves[random.nextInt(n)],child);board=child;
        }
        double frozen=work.evaluatePawns(board);Arrays.fill(model.weights,123);assertEquals(frozen,work.evaluatePawns(board),0);
    }
    /** V1 had the same payload but fixed .5 spacing and no stored spacing field. */
    private static byte[] legacy(byte[] current) {
        byte[] old=new byte[current.length-8];System.arraycopy(current,0,old,0,12);old[7]--;
        System.arraycopy(current,20,old,12,current.length-24);var crc=new java.util.zip.CRC32();crc.update(old,0,old.length-4);
        java.nio.ByteBuffer.wrap(old,old.length-4,4).putInt((int)crc.getValue());return old;
    }
}
