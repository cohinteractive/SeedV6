package com.ohinteractive.seedv6.core.brnnext;

import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.core.brnnext.BrnNextArchitecture.*;

class BrnCubicProjectionTest {
    static final int[] COMPONENTS={2,3,5,9};
    static byte[] bytes(BrnNextTrainer t) throws IOException {
        var out=new ByteArrayOutputStream();t.write(out);return out.toByteArray();
    }
    @Test void projectionPreservesAllRetainedStateAndMatchesMaskedTraining() throws Exception {
        var source=new BrnNextTrainer(GEN32_CUBIC,71,.001);
        double[] targets={.6,-.4,.2};var boards=BrnNextMathTest.BOARDS;
        for(int i=0;i<8;i++)source.trainBatch(boards,targets,3);
        byte[] original=bytes(source);var compact=source.projectCubicFour(COMPONENTS);
        assertArrayEquals(original,bytes(source));assertEquals(source.step(),compact.step());
        assertEquals(source.rate(),compact.rate());assertEquals(GEN32_CUBIC_R4,compact.architecture());
        double[][] a={source.weights,source.first,source.second},b={compact.weights,compact.first,compact.second};
        for(int k=0;k<3;k++) {
            assertArrayEquals(Arrays.copyOf(a[k],EMBEDDING),Arrays.copyOf(b[k],EMBEDDING));
            for(int f=0;f<FEATURES;f++)for(int r=0;r<4;r++)
                assertEquals(a[k][EMBEDDING+f*16+COMPONENTS[r]],b[k][EMBEDDING+f*4+r]);
            for(int d=0;d<2;d++)for(int r=0;r<4;r++)
                assertEquals(a[k][GEN32_CUBIC.head()+d*16+COMPONENTS[r]],b[k][GEN32_CUBIC_R4.head()+d*4+r]);
        }
        // Independent rank16 control: zero omitted factors, heads and moments so
        // their gradients stay zero. Retained factors follow the same objective.
        var masked=BrnNextTrainer.read(GEN32_CUBIC,new ByteArrayInputStream(original));
        for(var values:new double[][]{masked.weights,masked.first,masked.second})for(int r=0;r<16;r++) {
            if(Arrays.binarySearch(COMPONENTS,r)>=0)continue;
            for(int f=0;f<FEATURES;f++)values[EMBEDDING+f*16+r]=0;
            values[GEN32_CUBIC.head()+r]=values[GEN32_CUBIC.head()+16+r]=0;
        }
        for(int i=0;i<8;i++) {
            for(var board:boards)assertEquals(masked.predictPawns(board),compact.predictPawns(board),1e-12);
            assertEquals(masked.trainBatch(boards,targets,3),compact.trainBatch(boards,targets,3),1e-12);
        }
        var restored=BrnNextTrainer.read(GEN32_CUBIC_R4,new ByteArrayInputStream(bytes(compact)));
        compact.trainBatch(boards,targets,3);restored.trainBatch(boards,targets,3);
        assertArrayEquals(bytes(compact),bytes(restored));assertArrayEquals(original,bytes(source));
        assertThrows(IOException.class,()->BrnNextTrainer.read(GEN32_CUBIC,new ByteArrayInputStream(bytes(compact))));
    }
    @Test void invalidMappingsCannotSilentlyChangeOrAliasTheSource() throws Exception {
        var source=new BrnNextTrainer(GEN32_CUBIC,71,.001);byte[] before=bytes(source);
        for(int[] indices:new int[][]{{2,3,5},{2,3,5,16},{2,2,5,9},{3,2,5,9},{-1,3,5,9}})
            assertThrows(IllegalArgumentException.class,()->source.projectCubicFour(indices));
        assertThrows(IllegalArgumentException.class,()->new BrnNextTrainer(GEN32_GATED,1,.001).projectCubicFour(COMPONENTS));
        int[] indices=COMPONENTS.clone();var projected=source.projectCubicFour(indices);
        byte[] projectedBytes=bytes(projected);indices[0]=0;
        assertArrayEquals(projectedBytes,bytes(projected));assertArrayEquals(before,bytes(source));
    }
}
