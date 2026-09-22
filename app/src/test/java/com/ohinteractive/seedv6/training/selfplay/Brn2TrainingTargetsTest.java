package com.ohinteractive.seedv6.training.selfplay;

import java.util.List;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn2.*;
import com.ohinteractive.seedv6.core.util.Value;
import static org.junit.jupiter.api.Assertions.*;

class Brn2TrainingTargetsTest {
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
        var expected = new Brn2Trainer(0.001); var actual = new Brn2Trainer(0.001);
        for (var sample : samples) expected.train(sample.board(), sample.target());
        var stats = Brn2SelfPlayTraining.trainSamples(actual, samples, new SelfPlayTraining.Config(1, 1, false, 0),
                new SelfPlayControl(), ignored -> {}).orElseThrow();
        assertEquals(4, stats.samplesTrained()); assertEquals(4, stats.optimizerUpdates());
        assertArrayEquals(Brn2Codec.encodeTraining(expected), Brn2Codec.encodeTraining(actual));
        assertNotEquals(0, actual.snapshot().weight(Brn2Model.OUTPUT_BIAS));
    }

    @Test void cancellationOnlyStopsBetweenSuccessfulOnlineUpdatesAndCodecContinuesExactly() throws Exception {
        var samples = TrajectorySampler.sample(HeadlessGameTest.foolsMate().trajectory(), 32);
        var trainer = new Brn2Trainer(0.002); var control = new SelfPlayControl();
        var result = Brn2SelfPlayTraining.trainSamples(trainer, samples, new SelfPlayTraining.Config(1, 1, false, 0),
                control, progress -> control.cancel()).orElseThrow();
        assertTrue(result.cancelled()); assertEquals(1, trainer.optimizer().step());
        var restored = Brn2Codec.decodeTraining(Brn2Codec.encodeTraining(trainer));
        for (var sample : samples.subList(1, samples.size())) {
            trainer.train(sample.board(), sample.target()); restored.train(sample.board(), sample.target());
        }
        assertArrayEquals(Brn2Codec.encodeTraining(trainer), Brn2Codec.encodeTraining(restored));
        assertTrue(Brn2SelfPlayTraining.trainSamples(restored, List.of(), new SelfPlayTraining.Config(1, 1, true, 2),
                new SelfPlayControl(), ignored -> {}).isEmpty());
    }
}
