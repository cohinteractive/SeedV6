package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import java.io.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DenseAdamAttackFieldTrainerTest {
    @Test void denseStepMatchesFullArrayEquationsIncludingUntouchedHistoricalMoments() {
        var dense=new DenseAdamAttackFieldTrainer(true,.01,71);var masked=new DenseAdamAttackFieldTrainer(false,.01,71);
        var batch=new AttackFieldNetwork.Features[]{new AttackFieldNetwork.Workspace().encode(Board.startingPosition(),true)};double[] targets={.75};
        var model=dense.model;model.gradients(batch,targets);int absent=-1;
        for(int i=0;i<model.weights.length;i++)if(model.stamps[i]!=model.serial){absent=i;break;}
        assertTrue(absent>=0);model.first[absent]=masked.model.first[absent]=.02;model.second[absent]=masked.model.second[absent]=.004;model.step=masked.model.step=5;
        double[] expected=model.weights.clone(),first=model.first.clone(),second=model.second.clone();double c1=1-Math.pow(.9,6),c2=1-Math.pow(.999,6);
        for(int i=0;i<expected.length;i++) {
            double g=model.stamps[i]==model.serial?model.gradient[i]:0;
            first[i]=.9*first[i]+.1*g;second[i]=.999*second[i]+.001*g*g;
            expected[i]=Math.max(-.99,Math.min(.99,expected[i]*(1-.01*.01)-.01*(first[i]/c1)/(Math.sqrt(second[i]/c2)+1e-8)));
        }
        dense.train(batch,targets);masked.train(batch,targets);
        assertArrayEquals(expected,model.weights,1e-15);assertArrayEquals(first,model.first);assertArrayEquals(second,model.second);assertEquals(6,model.step);
        assertEquals(.02,masked.model.first[absent]);assertEquals(.004,masked.model.second[absent]);assertEquals(.018,model.first[absent],1e-17);assertNotEquals(masked.model.weights[absent],model.weights[absent]);
        for(double value:model.weights)assertTrue(Double.isFinite(value)&&Math.abs(value)<=.99);
        model.first[absent]=Double.NaN;assertThrows(ArithmeticException.class,()->dense.train(batch,targets));
    }
    @Test void maskedControlAndDenseSerializedContinuationAreExactIncludingTailBatch()throws Exception {
        var data=new PairCoverageDiagnostic.Data(257);
        for(int i=0;i<data.rows;i++){long[] b=i%2==0?Board.startingPosition():Board.fromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1");System.arraycopy(b,0,data.boards,6*i,6);data.targets[i]=(i%5)/4.0;}
        var legacy=new AttackFieldNetwork(true,.01,71);var masked=new DenseAdamAttackFieldTrainer(false,.01,71);
        for(int epoch=1;epoch<=3;epoch++) {
            AttackFieldDiagnostic.epoch(legacy,data,71,epoch);DenseAdamAttackFieldDiagnostic.epoch(masked,data,71,epoch);var a=new ByteArrayOutputStream();var b=new ByteArrayOutputStream();legacy.write(a);masked.model.write(b);assertArrayEquals(a.toByteArray(),b.toByteArray());
        }
        var dense=new DenseAdamAttackFieldTrainer(true,.01,71);DenseAdamAttackFieldDiagnostic.epoch(dense,data,71,1);var bytes=new ByteArrayOutputStream();dense.write(bytes);var restored=DenseAdamAttackFieldTrainer.read(new ByteArrayInputStream(bytes.toByteArray()));assertTrue(restored.dense);
        DenseAdamAttackFieldDiagnostic.epoch(dense,data,71,2);DenseAdamAttackFieldDiagnostic.epoch(restored,data,71,2);var a=new ByteArrayOutputStream();var b=new ByteArrayOutputStream();dense.write(a);restored.write(b);assertArrayEquals(a.toByteArray(),b.toByteArray());
        byte[] corrupt=a.toByteArray();corrupt[11]^=1;assertThrows(IOException.class,()->DenseAdamAttackFieldTrainer.read(new ByteArrayInputStream(corrupt)));
    }
}
