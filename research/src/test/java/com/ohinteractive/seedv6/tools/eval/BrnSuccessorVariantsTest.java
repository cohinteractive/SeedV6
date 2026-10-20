package com.ohinteractive.seedv6.tools.eval;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.Brn3Model;
import com.ohinteractive.seedv6.core.brnnext.*;
import com.ohinteractive.seedv6.core.brnpair2.BrnPair2Features;
import java.util.SplittableRandom;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class BrnSuccessorVariantsTest {
    @ParameterizedTest @EnumSource(value=BrnNextArchitecture.class, names={"GEN32_GATED","GEN32_CUBIC"})
    void oldGainControlCannotSilentlyRescaleOnlyTheCorrection(BrnNextArchitecture kind) {
        var model=new BrnNextModel(kind,new double[kind.parameters()]);
        assertSame(model,BrnSuccessorVariants.gain(model,.25));
        assertThrows(IllegalArgumentException.class,()->BrnSuccessorVariants.gain(model,.5));
    }
    @ParameterizedTest @EnumSource(value=BrnNextArchitecture.class, names={"GATED_PAIRS","CUBIC","POOLED"})
    void outputScalingChangesOnlyResidualGainAndPreservesSource(BrnNextArchitecture kind) {
        var random=new SplittableRandom(913);double[] weights=new double[kind.parameters()];
        for(int i=0;i<weights.length;i++)weights[i]=random.nextDouble(-.4,.4);
        var source=new BrnNextModel(kind,weights);
        for(double gain:new double[]{0,.25,.5,1}) {
            var variant=BrnSuccessorVariants.gain(source,gain);
            for(String fen:new String[]{"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                    "r3k2r/8/8/3pP3/8/8/8/R3K2R b KQkq - 0 1","4k3/P7/8/8/8/8/7p/4K3 w - - 0 1"}) {
                var board=Board.fromFen(fen);double material=BrnPair2Features.material(board);
                double expected=material+gain*(source.rawPawns(board,source.newWorkspace())-material);
                assertEquals(Brn3Model.score(expected),variant.score(board,variant.newWorkspace()));
                double raw=variant.rawPawns(board,variant.newWorkspace());
                assertEquals(expected,material+.25*(raw-material),1e-12);
            }
        }
        for(int i=0;i<weights.length;i++)assertEquals(weights[i],source.weight(i));
        assertThrows(IllegalArgumentException.class,()->BrnSuccessorVariants.gain(source,Double.NaN));
        assertThrows(IllegalArgumentException.class,()->BrnSuccessorVariants.gain(source,1.01));
    }
}
