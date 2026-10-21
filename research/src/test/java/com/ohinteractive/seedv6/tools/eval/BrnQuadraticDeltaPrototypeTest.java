package com.ohinteractive.seedv6.tools.eval;

import com.ohinteractive.seedv6.core.brnnext.*;
import java.util.SplittableRandom;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class BrnQuadraticDeltaPrototypeTest {
    @ParameterizedTest @EnumSource(value=BrnNextArchitecture.class,names={"GEN32_CUBIC","GEN32_CUBIC_R4"})
    void zeroHeadsPermitExactOmissionButNonzeroHeadsAreRejected(BrnNextArchitecture kind) {
        var random=new SplittableRandom(15917);double[] weights=new double[kind.parameters()];
        for(int i=0;i<kind.head()+kind.width();i++)weights[i]=random.nextDouble(-1,1);
        var model=new BrnNextModel(kind,weights);
        var scalar=new BrnCubicDeltaAccumulator(model);var quadratic=new BrnQuadraticDeltaPrototype(model);
        var canonical=model.newWorkspace();var boards=BrnSuccessorAudit.boards();
        for(int i=0;i<20000;i++) {
            var board=boards.get(random.nextInt(boards.size()));
            assertEquals(Double.doubleToLongBits(scalar.rawPawns(board)),Double.doubleToLongBits(quadratic.rawPawns(board)));
            assertEquals(model.score(board,canonical),quadratic.score(board));
            assertEquals(scalar.score(board),quadratic.score(board));
        }
        for(int i=0;i<weights.length;i++)assertEquals(weights[i],model.weight(i));
        assertEquals(scalar.primitiveBytes()-16L*kind.width(),quadratic.primitiveBytes());
        for(int r=0;r<kind.width();r++) {
            weights[kind.head()+kind.width()+r]=Double.MIN_VALUE;
            var unsupported=new BrnNextModel(kind,weights);
            assertThrows(IllegalArgumentException.class,()->new BrnQuadraticDeltaPrototype(unsupported));
            weights[kind.head()+kind.width()+r]=0;
        }
    }
}
