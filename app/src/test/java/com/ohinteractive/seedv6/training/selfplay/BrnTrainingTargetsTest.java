package com.ohinteractive.seedv6.training.selfplay;

import java.util.List;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn.*;
import com.ohinteractive.seedv6.core.util.Value;
import static org.junit.jupiter.api.Assertions.*;

class BrnTrainingTargetsTest {
    @Test void existingTerminalTargetsAreBoundedMonotoneAndCorrectForBothPerspectives() {
        for (int side : new int[] {Value.WHITE, Value.BLACK}) {
            assertEquals(side == Value.WHITE ? 1 : -1, GameResult.WHITE_WIN.target(side));
            assertEquals(0, GameResult.DRAW.target(side));
            assertEquals(side == Value.WHITE ? -1 : 1, GameResult.BLACK_WIN.target(side));
        }
        assertTrue(GameResult.BLACK_WIN.target(Value.WHITE) < GameResult.DRAW.target(Value.WHITE));
        assertTrue(GameResult.DRAW.target(Value.WHITE) < GameResult.WHITE_WIN.target(Value.WHITE));
        for (var result : GameResult.values()) for (int side : new int[]{Value.WHITE, Value.BLACK})
            assertTrue(Math.abs(result.target(side)) <= 1);
        for (double bad : new double[]{Double.NaN, Double.POSITIVE_INFINITY, -2000, 2000, -1.1, 1.1})
            assertThrows(IllegalArgumentException.class, () -> new TrajectorySampler.Sample(Board.startingPosition(), bad));
    }

    @Test void realTrajectoryFeedsExactExistingTargetsToOnlineAdamWithoutScoreConversion() throws Exception {
        var samples = TrajectorySampler.sample(HeadlessGameTest.foolsMate().trajectory(), 32);
        assertEquals(List.of(-1.0, 1.0, -1.0, 1.0), samples.stream().map(TrajectorySampler.Sample::target).toList());
        var expected = new BrnTrainer(0.001); var actual = new BrnTrainer(0.001);
        for (var sample : samples) expected.train(sample.board(), sample.target());
        var stats = BrnSelfPlayTraining.trainSamples(actual, samples, new SelfPlayTraining.Config(1, 1, false, 0),
                new SelfPlayControl(), ignored -> {}).orElseThrow();
        assertEquals(4, stats.samplesTrained()); assertEquals(4, stats.optimizerUpdates());
        assertArrayEquals(BrnCodec.encodeTraining(expected), BrnCodec.encodeTraining(actual));
        assertNotEquals(0, actual.snapshot().weight(BrnFeatureSchema.BIAS));
    }

    @Test void cancellationOnlyStopsBetweenSuccessfulOnlineUpdatesAndCodecContinuesExactly() throws Exception {
        var samples = TrajectorySampler.sample(HeadlessGameTest.foolsMate().trajectory(), 32);
        var trainer = new BrnTrainer(0.002); var control = new SelfPlayControl();
        var result = BrnSelfPlayTraining.trainSamples(trainer, samples, new SelfPlayTraining.Config(1, 1, false, 0),
                control, progress -> control.cancel()).orElseThrow();
        assertTrue(result.cancelled()); assertEquals(1, trainer.optimizer().step());
        var restored = BrnCodec.decodeTraining(BrnCodec.encodeTraining(trainer));
        for (var sample : samples.subList(1, samples.size())) {
            trainer.train(sample.board(), sample.target()); restored.train(sample.board(), sample.target());
        }
        assertArrayEquals(BrnCodec.encodeTraining(trainer), BrnCodec.encodeTraining(restored));
        assertTrue(BrnSelfPlayTraining.trainSamples(restored, List.of(), new SelfPlayTraining.Config(1, 1, true, 2),
                new SelfPlayControl(), ignored -> {}).isEmpty());
    }
}
