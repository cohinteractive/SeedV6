package com.ohinteractive.seedv6.tools.eval;

import com.ohinteractive.seedv6.core.brnnext.*;
import java.util.SplittableRandom;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class BrnCubicVectorPrototypeTest {
    @ParameterizedTest @EnumSource(value=BrnNextArchitecture.class,names={"GEN32_CUBIC","GEN32_CUBIC_R4"})
    void laneUpdatesRetainScalarOrderAcrossRefreshesAndSideChanges(BrnNextArchitecture kind) {
        var random=new SplittableRandom(971);double[] weights=new double[kind.parameters()];
        for(int i=0;i<weights.length;i++)weights[i]=random.nextDouble(-1,1);
        var model=new BrnNextModel(kind,weights);
        var scalar=new BrnCubicDeltaAccumulator(model);var vector=new BrnCubicVectorPrototype(model);
        var copiedScalar=new BrnCubicVectorPrototype(model,false);
        var canonical=model.newWorkspace();var boards=BrnSuccessorAudit.boards();
        for(int i=0;i<20000;i++) {
            var board=boards.get(random.nextInt(boards.size()));
            assertEquals(Double.doubleToLongBits(scalar.rawPawns(board)),Double.doubleToLongBits(vector.rawPawns(board)));
            assertEquals(Double.doubleToLongBits(scalar.rawPawns(board)),Double.doubleToLongBits(copiedScalar.rawPawns(board)));
            assertEquals(model.score(board,canonical),vector.score(board));
            assertEquals(scalar.score(board),vector.score(board));
        }
        for(int i=0;i<weights.length;i++)assertEquals(weights[i],model.weight(i));
        assertEquals(scalar.primitiveBytes()+8L*weights.length,vector.primitiveBytes());
    }
}
