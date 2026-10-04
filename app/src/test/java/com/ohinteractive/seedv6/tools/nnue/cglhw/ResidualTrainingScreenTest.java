package com.ohinteractive.seedv6.tools.nnue.cglhw;

import com.ohinteractive.seedv6.core.Board;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResidualTrainingScreenTest {
    @Test void heldOutPurgesTrainingFeatureAliasesAndDuplicatesWithoutLookingAtTargets() {
        var a=Board.startingPosition();var same=a.clone();same[4]^=1L<<10;
        var other=a.clone();other[4]^=1;
        var train=new ResidualTrainingScreen.Data(new long[][]{a},new double[]{1});
        var held=new ResidualTrainingScreen.Data(new long[][]{same,other,other.clone()},new double[]{-1,.3,-.8});
        var filtered=ResidualTrainingScreen.disjointHeld(train,held);
        assertEquals(1,filtered.boards().length);assertArrayEquals(other,filtered.boards()[0]);
        assertEquals(.3,filtered.targets()[0]);
    }
}
