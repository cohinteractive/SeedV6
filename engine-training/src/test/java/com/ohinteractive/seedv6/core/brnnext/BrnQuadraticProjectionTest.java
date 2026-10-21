package com.ohinteractive.seedv6.core.brnnext;

import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.core.brnnext.BrnNextArchitecture.*;

class BrnQuadraticProjectionTest {
    static byte[] bytes(BrnNextTrainer trainer) throws IOException {
        var out=new ByteArrayOutputStream();trainer.write(out);return out.toByteArray();
    }
    @Test void projectionRetainsAllSurvivingOptimizerStateAndIndependentMaskedUpdates() throws Exception {
        var source=new BrnNextTrainer(GEN32_CUBIC,71,.001);
        var boards=BrnNextMathTest.BOARDS;double[] targets={.6,-.4,.2};
        for(int i=0;i<8;i++)source.trainBatch(boards,targets,3);
        byte[] before=bytes(source);var projected=source.projectQuadratic();
        assertArrayEquals(before,bytes(source));assertEquals(source.step(),projected.step());
        assertEquals(source.rate(),projected.rate());assertEquals(GEN32_QUADRATIC,projected.architecture());
        double[][] a={source.weights,source.first,source.second},b={projected.weights,projected.first,projected.second};
        for(int i=0;i<3;i++)assertArrayEquals(Arrays.copyOf(a[i],GEN32_QUADRATIC.parameters()),b[i]);
        var masked=BrnNextTrainer.read(GEN32_CUBIC,new ByteArrayInputStream(before));
        for(int update=0;update<8;update++) {
            for(var array:new double[][]{masked.weights,masked.first,masked.second})
                Arrays.fill(array,GEN32_QUADRATIC.parameters(),array.length,0);
            for(var board:boards)assertEquals(masked.predictPawns(board),projected.predictPawns(board));
            assertEquals(masked.trainBatch(boards,targets,3),projected.trainBatch(boards,targets,3));
            a=new double[][]{masked.weights,masked.first,masked.second};
            for(int i=0;i<3;i++)assertArrayEquals(Arrays.copyOf(a[i],GEN32_QUADRATIC.parameters()),b[i]);
        }
        var restored=BrnNextTrainer.read(GEN32_QUADRATIC,new ByteArrayInputStream(bytes(projected)));
        projected.trainBatch(boards,targets,3);restored.trainBatch(boards,targets,3);
        assertArrayEquals(bytes(projected),bytes(restored));assertArrayEquals(before,bytes(source));
        assertThrows(IOException.class,()->BrnNextTrainer.read(GEN32_CUBIC,new ByteArrayInputStream(bytes(projected))));
        for(var kind:BrnNextArchitecture.values())if(kind!=GEN32_CUBIC) {
            var unsupported=new BrnNextTrainer(kind,1,.001);var saved=bytes(unsupported);
            assertThrows(IllegalArgumentException.class,unsupported::projectQuadratic);
            assertArrayEquals(saved,bytes(unsupported));
        }
    }
}
