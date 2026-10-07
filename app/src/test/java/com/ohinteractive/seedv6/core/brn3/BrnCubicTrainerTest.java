package com.ohinteractive.seedv6.core.brn3;

import static com.ohinteractive.seedv6.core.brn3.Brn3Layout.*;
import com.ohinteractive.seedv6.core.*;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BrnCubicTrainerTest {
    @Test void exactQuadraticSubmodelAndExistingGradients() {
        var quadratic=new BrnEnergyTrainer(8,71,.003);var cubic=new BrnCubicTrainer(8,71,.003);
        for(int i=0;i<quadratic.output;i++)assertEquals(quadratic.weights[i],cubic.weights[i],0);
        Arrays.fill(quadratic.weights,DENSE,quadratic.factors,.13);Arrays.fill(quadratic.weights,quadratic.alpha,quadratic.output,.17);
        Arrays.fill(quadratic.weights,quadratic.modelParameters,quadratic.trainingParameters,.002);quadratic.weights[quadratic.output]=-.09;
        System.arraycopy(quadratic.weights,0,cubic.weights,0,quadratic.output);cubic.weights[cubic.output]=quadratic.weights[quadratic.output];
        System.arraycopy(quadratic.weights,quadratic.modelParameters,cubic.weights,cubic.modelParameters,RELATIVE_PARAMETERS);
        var board=BrnEnergyTrainerTest.examples()[0];assertEquals(quadratic.predictPawns(board),cubic.predictPawns(board),0);
        quadratic.resetGradient();cubic.resetGradient();quadratic.backward(1);cubic.backward(1);
        for(int i=0;i<quadratic.output;i++)assertEquals(quadratic.gradient[i],cubic.gradient[i],1e-14,"shared block "+i);
        for(int i=0;i<RELATIVE_PARAMETERS;i++)assertEquals(quadratic.gradient[quadratic.modelParameters+i],cubic.gradient[cubic.modelParameters+i],1e-14);
        for(int i=cubic.third;i<cubic.beta;i++)assertEquals(0,cubic.gradient[i],0);
        assertEquals(quadratic.gradient[quadratic.output],cubic.gradient[cubic.output],0);
    }
    @Test void explicitPolynomialAndAllBlockFiniteDifferences() {
        var model=new BrnCubicTrainer(8,97,.003);var board=BrnEnergyTrainerTest.examples()[0];
        Arrays.fill(model.weights,DENSE,model.factors,.13);Arrays.fill(model.weights,model.alpha,model.third,.17);Arrays.fill(model.weights,model.beta,model.output,.11);
        double actual=model.predictPawns(board),expected=Brn3Features.material(board)+model.weights[model.output];
        double[] x=new double[192];for(int p=0;p<2;p++)for(int c=0;c<96;c++)x[p*96+c]=model.pooled[p][c];
        for(int i=0;i<192;i++)expected+=model.weights[DENSE+i]*x[i];
        // Expand quadratic and cubic monomials using independent scalar loops.
        for(int r=0;r<8;r++)for(int i=0;i<192;i++)for(int j=0;j<192;j++) {
            int base=model.factors+2*r*192;double uv=model.weights[base+i]*model.weights[base+192+j]*x[i]*x[j];
            expected+=model.weights[model.alpha+r]*uv;
            double third=0;for(int k=0;k<192;k++)third+=model.weights[model.third+r*192+k]*x[k];
            expected+=model.weights[model.beta+r]*uv*third;
        }
        assertEquals(expected,actual,2e-11);
        var snapshot=model.snapshot();var ablated=snapshot.withoutCubic();Arrays.fill(model.weights,model.beta,model.output,0);
        assertEquals(model.predictPawns(board),ablated.newWorkspace().evaluatePawns(board),1e-5);
        Arrays.fill(model.weights,model.beta,model.output,.11);assertEquals(actual,snapshot.newWorkspace().evaluatePawns(board),1e-5);
        model.predictPawns(board);
        model.resetGradient();model.backward(1);double[] g=model.gradient.clone();
        int[] bounds={0,NODES,DENSE,model.factors,model.alpha,model.third,model.beta,model.output,model.modelParameters,g.length};
        for(int block=0;block<bounds.length-1;block++) {
            int chosen=0;
            for(int i=bounds[block];i<bounds[block+1]&&chosen<10;i++)if(Math.abs(g[i])>1e-10) {
                double prior=model.weights[i];model.weights[i]=prior+1e-6;double plus=model.predictPawns(board);
                model.weights[i]=prior-1e-6;double minus=model.predictPawns(board);model.weights[i]=prior;
                assertEquals(g[i],(plus-minus)/2e-6,2e-7,"parameter "+i);chosen++;
            }
            assertTrue(chosen>0,"Unobserved parameter block "+block);
        }
    }
    @Test void tinyFitSymmetryAndInformationIsolation() {
        var model=new BrnCubicTrainer(8,71,.003);var boards=BrnEnergyTrainerTest.examples();double[] targets={.2,-.3,-.4};
        for(int i=0;i<1000;i++)model.trainBatch(boards,targets,3);
        for(int i=0;i<3;i++)assertEquals(targets[i],Brn3Objective.smoothOutcome(model.predictPawns(boards[i]),com.ohinteractive.seedv6.training.nnue.NnueCorpusTargets.material(boards[i]))[0],.025);
        var altered=boards[0].clone();altered[4]=(altered[4]&Board.PLAYER_BIT)|(~altered[4]&~Board.PLAYER_BIT);altered[5]^=98765;
        assertEquals(model.predictPawns(boards[0]),model.predictPawns(altered),0);
        var reversed=Board.fromFen("rnbqkb1r/pppp1ppp/5n2/4p3/4P3/2N5/PPPP1PPP/R1BQKBNR b KQkq - 2 3");
        assertEquals(model.predictPawns(boards[0]),model.predictPawns(reversed),1e-10);
    }
    @Test void exactContinuationCrcImmutableFoldAndIncrementalParity()throws Exception {
        var model=new BrnCubicTrainer(8,71,.003);var boards=BrnEnergyTrainerTest.examples();double[] targets={.2,-.3,-.4};
        for(int i=0;i<6;i++)model.trainBatch(boards,targets,3);
        var bytes=new ByteArrayOutputStream();model.write(bytes);byte[] state=bytes.toByteArray();
        var restored=BrnCubicTrainer.read(new ByteArrayInputStream(state));model.trainBatch(boards,targets,3);restored.trainBatch(boards,targets,3);
        assertArrayEquals(model.weights,restored.weights);assertArrayEquals(model.first,restored.first);assertArrayEquals(model.second,restored.second);assertEquals(model.step(),restored.step());
        state[state.length/2]^=1;assertThrows(IOException.class,()->BrnCubicTrainer.read(new ByteArrayInputStream(state)));
        bytes.reset();model.snapshot().write(bytes);byte[] payload=bytes.toByteArray();var snapshot=BrnCubicTrainer.Model.read(new ByteArrayInputStream(payload));var work=snapshot.newWorkspace();
        payload[payload.length/2]^=1;assertThrows(IOException.class,()->BrnCubicTrainer.Model.read(new ByteArrayInputStream(payload)));
        assertThrows(IOException.class,()->BrnCubicTrainer.Model.read(new ByteArrayInputStream(Arrays.copyOf(payload,24))));
        long[] moves=new long[512];var random=new Random(9241);var board=Board.startingPosition();
        String[] special={"r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1","4k3/P7/8/8/8/8/7p/4K3 w - - 0 1"};
        for(String fen:special) {
            var parent=Board.fromFen(fen);int n=Gen.genAll(parent[0],parent[1],parent[2],parent[3],(int)parent[4],parent[5],true,moves,new long[6]);
            for(int i=0;i<n;i++){var child=new long[6];Board.makeMoveInto(parent[0],parent[1],parent[2],parent[3],(int)parent[4],parent[5],moves[i],child);
                assertEquals(model.predictPawns(parent),work.evaluatePawns(parent),1e-4);assertEquals(model.predictPawns(child),work.evaluatePawns(child),1e-4);assertEquals(model.predictPawns(parent),work.evaluatePawns(parent),1e-4);}
        }
        for(int ply=0;ply<800;ply++) {
            assertEquals(model.predictPawns(board),work.evaluatePawns(board),1e-4);
            assertEquals(snapshot.newWorkspace().evaluatePawns(board),work.evaluatePawns(board),1e-10);
            int n=Gen.genAll(board[0],board[1],board[2],board[3],(int)board[4],board[5],true,moves,new long[6]);
            if(n==0||ply%99==98){board=Board.startingPosition();continue;}
            var child=new long[6];Board.makeMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],moves[random.nextInt(n)],child);board=child;
        }
        double frozen=work.evaluatePawns(board);Arrays.fill(model.weights,123);assertEquals(frozen,work.evaluatePawns(board),0);
    }
}
