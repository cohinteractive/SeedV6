package com.ohinteractive.seedv6.core.brn3;

import static com.ohinteractive.seedv6.core.brn3.Brn3Layout.*;
import com.ohinteractive.seedv6.core.*;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BrnEnergyTrainerTest {
    static final String FEN="r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3";
    static long[][] examples(){return new long[][]{Board.fromFen(FEN),Board.fromFen("4k3/8/8/3p4/3P4/8/4N3/4K3 w - - 0 1"),Board.fromFen("4k3/8/8/4q3/8/8/4R3/4K3 b - - 0 1")};}
    @Test void encoderMatchesOriginalAndEnergyMatchesExplicitQuadraticMatrix() {
        var b=Board.fromFen(FEN);var original=new Brn3Trainer(71);var energy=new BrnEnergyTrainer(8,71,.003);
        for(int i=0;i<DENSE;i++)assertEquals(original.weight(i),energy.weights[i],0);
        assertEquals(Brn3Features.material(b),energy.predictPawns(b),0);
        Arrays.fill(energy.weights,DENSE,energy.factors,.13);Arrays.fill(energy.weights,energy.alpha,energy.output,.17);energy.weights[energy.output]=-.09;
        double actual=energy.predictPawns(b),expected=Brn3Features.material(b)-.09;double[] x=new double[192];
        // Independent local encoder over original BRN3 weights, directed incident sums.
        int stm=Board.player((int)b[4]);
        for(int p=0;p<2;p++) {
            var ids=new ArrayList<Integer>();int perspective=stm^p;
            for(int sq=0;sq<64;sq++){int code=Board.getSquare(b[0],b[1],b[2],b[3],sq);if(code!=0)ids.add(Brn3Features.channel(code^(perspective<<3))*64+(sq^(perspective*56)));}
            for(int a:ids)for(int c=0;c<8;c++) {
                double local=original.weight(NODES+a*8+c);
                for(int other:ids)if(other!=a)local+=original.weight(Brn3Layout.edge(a,other)+c)/Math.sqrt(ids.size()-1);
                x[p*96+a/64*8+c]+=Math.max(0,local)/Math.sqrt(ids.size());
            }
        }
        for(int i=0;i<192;i++){assertEquals(x[i],energy.pooled[i/96][i%96],2e-15);expected+=energy.weights[DENSE+i]*x[i];}
        for(int i=0;i<192;i++)for(int j=0;j<192;j++) {
            double q=0;for(int r=0;r<8;r++){int base=energy.factors+2*r*192;q+=energy.weights[energy.alpha+r]*energy.weights[base+i]*energy.weights[base+192+j];}
            expected+=q*x[i]*x[j];
        }
        assertEquals(expected,actual,1e-12);
    }
    @Test void allParameterFamiliesHaveIndependentFiniteDifferenceGradients() {
        var energy=new BrnEnergyTrainer(8,97,.003);var b=examples()[0];
        Arrays.fill(energy.weights,DENSE,energy.factors,.13);Arrays.fill(energy.weights,energy.alpha,energy.output,.17);
        energy.predictPawns(b);energy.resetGradient();energy.backward(1);double[] g=energy.gradient.clone();
        var selected=new ArrayList<Integer>();
        for(int lo:new int[]{0,NODES,DENSE,energy.factors,energy.alpha,energy.output,energy.modelParameters}) {
            int hi=lo==0?NODES:lo==NODES?DENSE:lo==DENSE?energy.factors:lo==energy.factors?energy.alpha:lo==energy.alpha?energy.output:lo==energy.output?energy.output+1:g.length;
            int chosen=0;for(int i=lo;i<hi&&chosen<12;i++)if(Math.abs(g[i])>1e-9){selected.add(i);chosen++;}
            assertTrue(chosen>0,"No observable gradient in block "+lo);
        }
        for(int i:selected){double prior=energy.weights[i];energy.weights[i]=prior+1e-6;double plus=energy.predictPawns(b);energy.weights[i]=prior-1e-6;double minus=energy.predictPawns(b);energy.weights[i]=prior;
            assertEquals(g[i],(plus-minus)/2e-6,2e-7,"parameter "+i);}
    }
    @Test void tinyOverfitSymmetryAndInformationBoundary() {
        var boards=examples();double[] targets={.2,-.3,-.4};
        var model=new BrnEnergyTrainer(8,71,.003);
        for(int step=0;step<1000;step++)model.trainBatch(boards,targets,3);
        for(int i=0;i<3;i++)assertEquals(targets[i],Brn3Objective.smoothOutcome(model.predictPawns(boards[i]),com.ohinteractive.seedv6.training.nnue.NnueCorpusTargets.material(boards[i]))[0],.025);
        var a=boards[0];var altered=a.clone();altered[4]=(a[4]&Board.PLAYER_BIT)|(~a[4]&~Board.PLAYER_BIT);altered[5]^=1234567;
        assertEquals(model.predictPawns(a),model.predictPawns(altered),0);
        var reversed=Board.fromFen("rnbqkb1r/pppp1ppp/5n2/4p3/4P3/2N5/PPPP1PPP/R1BQKBNR b KQkq - 2 3");
        assertEquals(model.predictPawns(a),model.predictPawns(reversed),1e-10);
    }
    @Test void exactResumeAndIndependentFoldedCacheAcrossLegalTransitions()throws Exception {
        for(int rank:new int[]{0,8,16}) {
            var model=new BrnEnergyTrainer(rank,71,.003);var boards=examples();double[] targets={.2,-.3,-.4};
            for(int step=0;step<5;step++)model.trainBatch(boards,targets,3);
            var bytes=new ByteArrayOutputStream();model.write(bytes);var restored=BrnEnergyTrainer.read(new ByteArrayInputStream(bytes.toByteArray()));
            model.trainBatch(boards,targets,3);restored.trainBatch(boards,targets,3);
            assertArrayEquals(model.weights,restored.weights);assertArrayEquals(model.first,restored.first);assertArrayEquals(model.second,restored.second);assertEquals(model.step(),restored.step());
            bytes.reset();model.snapshot().write(bytes);var snapshot=BrnEnergyTrainer.Model.read(new ByteArrayInputStream(bytes.toByteArray()));var work=snapshot.newWorkspace();
            byte[] corrupt=bytes.toByteArray();corrupt[corrupt.length/2]^=1;assertThrows(IOException.class,()->BrnEnergyTrainer.Model.read(new ByteArrayInputStream(corrupt)));
            assertThrows(IOException.class,()->BrnEnergyTrainer.Model.read(new ByteArrayInputStream(Arrays.copyOf(corrupt,24))));
            var random=new SplittableRandom(7241);var board=Board.startingPosition();long[] moves=new long[512];
            for(int ply=0;ply<600;ply++) {
                assertEquals(model.predictPawns(board),work.evaluatePawns(board),1e-4);
                assertEquals(snapshot.newWorkspace().evaluatePawns(board),work.evaluatePawns(board),1e-10);
                int n=Gen.genAll(board[0],board[1],board[2],board[3],(int)board[4],board[5],true,moves,new long[6]);
                if(n==0||ply%99==98){board=Board.startingPosition();continue;}
                var child=new long[6];Board.makeMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],moves[random.nextInt(n)],child);board=child;
            }
            double frozen=work.evaluatePawns(board);Arrays.fill(model.weights,123);assertEquals(frozen,work.evaluatePawns(board),0);
        }
    }
}
