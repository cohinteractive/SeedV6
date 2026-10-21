package com.ohinteractive.seedv6.core.brnnext;

import java.io.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.core.brnnext.BrnNextArchitecture.*;

class BrnQuadraticProjectionTest {
    static byte[] bytes(BrnNextTrainer trainer) throws IOException {
        var out=new ByteArrayOutputStream();trainer.write(out);return out.toByteArray();
    }
    @ParameterizedTest @EnumSource(value=BrnNextArchitecture.class,names={"GEN32_CUBIC","GEN32_CUBIC_R4"})
    void projectionRetainsAllSurvivingOptimizerStateAndIndependentMaskedUpdates(BrnNextArchitecture kind) throws Exception {
        var target=kind==GEN32_CUBIC?GEN32_QUADRATIC:GEN32_QUADRATIC_R4;
        var source=new BrnNextTrainer(kind,71,.001);
        var boards=BrnNextMathTest.BOARDS;double[] targets={.6,-.4,.2};
        for(int i=0;i<8;i++)source.trainBatch(boards,targets,3);
        byte[] before=bytes(source);var projected=source.projectQuadratic();
        assertArrayEquals(before,bytes(source));assertEquals(source.step(),projected.step());
        assertEquals(source.rate(),projected.rate());assertEquals(target,projected.architecture());
        double[][] a={source.weights,source.first,source.second},b={projected.weights,projected.first,projected.second};
        for(int i=0;i<3;i++)assertArrayEquals(Arrays.copyOf(a[i],target.parameters()),b[i]);
        var masked=BrnNextTrainer.read(kind,new ByteArrayInputStream(before));
        for(int update=0;update<8;update++) {
            for(var array:new double[][]{masked.weights,masked.first,masked.second})
                Arrays.fill(array,target.parameters(),array.length,0);
            for(var board:boards)assertEquals(masked.predictPawns(board),projected.predictPawns(board));
            assertEquals(masked.trainBatch(boards,targets,3),projected.trainBatch(boards,targets,3));
            a=new double[][]{masked.weights,masked.first,masked.second};
            for(int i=0;i<3;i++)assertArrayEquals(Arrays.copyOf(a[i],target.parameters()),b[i]);
        }
        var restored=BrnNextTrainer.read(target,new ByteArrayInputStream(bytes(projected)));
        projected.trainBatch(boards,targets,3);restored.trainBatch(boards,targets,3);
        assertArrayEquals(bytes(projected),bytes(restored));assertArrayEquals(before,bytes(source));
        assertThrows(IOException.class,()->BrnNextTrainer.read(kind,new ByteArrayInputStream(bytes(projected))));
        for(var other:BrnNextArchitecture.values())if(!other.frozenCubic()) {
            var unsupported=new BrnNextTrainer(other,1,.001);var saved=bytes(unsupported);
            assertThrows(IllegalArgumentException.class,unsupported::projectQuadratic);
            assertArrayEquals(saved,bytes(unsupported));
        }
    }
}
