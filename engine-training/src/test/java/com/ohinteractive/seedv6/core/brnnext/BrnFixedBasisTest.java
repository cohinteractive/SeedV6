package com.ohinteractive.seedv6.core.brnnext;

import java.io.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.core.brnnext.BrnNextArchitecture.*;

class BrnFixedBasisTest {
    static byte[] bytes(BrnNextTrainer trainer) throws IOException {
        var out=new ByteArrayOutputStream();trainer.write(out);return out.toByteArray();
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void newHeadOptimizerPreservesAnchorAndMatchesIndependentMaskedUpdates(boolean cubic) throws Exception {
        var source=new BrnNextTrainer(GEN32_QUADRATIC_R4,71,.001);
        var boards=BrnNextMathTest.BOARDS;double[] targets={.6,-.4,.2};
        for(int i=0;i<16;i++)source.trainBatch(boards,targets,3);
        byte[] original=bytes(source);var fixed=source.freezeQuadraticBasis(cubic);var kind=fixed.architecture();
        assertEquals(cubic?GEN32_FIXED_CUBIC_R4:GEN32_FIXED_QUADRATIC_R4,kind);
        assertEquals(0,fixed.step());assertEquals(source.rate(),fixed.rate());
        assertArrayEquals(source.weights,Arrays.copyOf(fixed.weights,source.weights.length));
        assertTrue(Arrays.stream(fixed.first).allMatch(x->x==0));assertTrue(Arrays.stream(fixed.second).allMatch(x->x==0));
        for(var board:boards)assertEquals(source.predictPawns(board),fixed.predictPawns(board));
        var ordinary=new BrnNextTrainer(cubic?GEN32_CUBIC_R4:GEN32_QUADRATIC_R4,0,.001);
        System.arraycopy(fixed.weights,0,ordinary.weights,0,fixed.weights.length);
        double[] anchor=Arrays.copyOf(fixed.weights,kind.trainableStart());
        for(int i=0;i<32;i++) {
            assertEquals(ordinary.trainBatch(boards,targets,3),fixed.trainBatch(boards,targets,3));
            System.arraycopy(anchor,0,ordinary.weights,0,anchor.length);
            Arrays.fill(ordinary.first,0,anchor.length,0);Arrays.fill(ordinary.second,0,anchor.length,0);
            assertArrayEquals(ordinary.weights,fixed.weights);assertArrayEquals(ordinary.first,fixed.first);
            assertArrayEquals(ordinary.second,fixed.second);assertArrayEquals(anchor,Arrays.copyOf(fixed.weights,anchor.length));
        }
        assertTrue(Arrays.stream(fixed.first,kind.trainableStart(),kind.parameters()).anyMatch(x->x!=0));
        assertEquals(4,kind.parameters()-kind.trainableStart());
        var restored=BrnNextTrainer.read(kind,new ByteArrayInputStream(bytes(fixed)));
        fixed.trainBatch(boards,targets,3);restored.trainBatch(boards,targets,3);assertArrayEquals(bytes(fixed),bytes(restored));
        var runtime=new ByteArrayOutputStream();BrnNextCodec.writeModel(fixed.snapshot(),runtime);
        var decoded=BrnNextCodec.readModel(kind,new ByteArrayInputStream(runtime.toByteArray()));
        for(var board:boards)assertEquals(fixed.predictPawns(board),decoded.rawPawns(board,decoded.newWorkspace()));
        assertThrows(IOException.class,()->BrnNextTrainer.read(ordinary.architecture(),new ByteArrayInputStream(bytes(fixed))));
        restored.first[0]=.1;
        assertThrows(IOException.class,()->BrnNextTrainer.read(kind,new ByteArrayInputStream(bytes(restored))));
        assertArrayEquals(original,bytes(source));
        assertThrows(IllegalArgumentException.class,()->fixed.freezeQuadraticBasis(cubic));
    }
}
