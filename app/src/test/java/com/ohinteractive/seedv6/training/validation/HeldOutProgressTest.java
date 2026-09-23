package com.ohinteractive.seedv6.training.validation;

import java.util.*;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.training.selfplay.TrajectorySampler;
import static org.junit.jupiter.api.Assertions.*;

class HeldOutProgressTest {
    @Test void reportsOnlyRealCompletedChunksAndPreservesExactArithmeticForEveryObjective() {
        var samples = Collections.nCopies(600, new TrajectorySampler.Sample(Board.startingPosition(), 1));
        for (double weight : new double[]{0, .75, 1}) {
            var progress = new ArrayList<Integer>();
            java.util.function.ToDoubleFunction<TrajectorySampler.Sample> target = s -> (1 - weight) * s.target() + weight * .25;
            var expected = HeldOutLoss.compare(b -> .5, b -> .3, samples, target);
            var actual = HeldOutLoss.compare(b -> .5, b -> .3, samples, target, progress::add);
            assertEquals(expected, actual); assertEquals(List.of(256, 512, 600), progress);
            assertEquals(600, actual.samples());
        }
    }
    @Test void aFastTinyHoldoutStillReportsItsTrueCompletedCount() {
        var samples = Collections.nCopies(2, new TrajectorySampler.Sample(Board.startingPosition(), 0));
        var progress = new ArrayList<Integer>();
        HeldOutLoss.compare(b -> 0, b -> 0, samples, TrajectorySampler.Sample::target, progress::add);
        assertEquals(List.of(2), progress);
    }
}
