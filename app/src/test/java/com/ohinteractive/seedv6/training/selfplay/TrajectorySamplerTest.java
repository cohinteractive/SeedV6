package com.ohinteractive.seedv6.training.selfplay;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import static org.junit.jupiter.api.Assertions.*;

class TrajectorySamplerTest {
    @Test
    void emptyShortEqualAndLongSamplingIsBoundedEvenUniqueAndDeterministic() {
        assertArrayEquals(new int[0], TrajectorySampler.indexes(0, 32));
        assertArrayEquals(new int[] {0, 1, 2}, TrajectorySampler.indexes(3, 32));
        assertArrayEquals(new int[] {0, 1, 2}, TrajectorySampler.indexes(3, 3));
        assertArrayEquals(new int[] {0, 49, 99, 149}, TrajectorySampler.indexes(150, 4));
        assertArrayEquals(new int[] {74}, TrajectorySampler.indexes(150, 1));
        for (int length : new int[] {1, 2, 31, 32, 33, 150, 1024}) {
            int[] indexes = TrajectorySampler.indexes(length, 32);
            assertEquals(Math.min(length, 32), indexes.length);
            assertEquals(indexes.length, Arrays.stream(indexes).distinct().count());
            assertEquals(0, indexes[0]);
            assertEquals(length - 1, indexes[indexes.length - 1]);
            assertArrayEquals(indexes, TrajectorySampler.indexes(length, 32));
        }
        assertThrows(IllegalArgumentException.class, () -> TrajectorySampler.indexes(-1, 2));
        assertThrows(IllegalArgumentException.class, () -> TrajectorySampler.indexes(1, 0));
    }

    @Test
    void rejectsIncompleteAndAcceptsEmptyCompletedWithoutInventingSamples() {
        HeadlessGame capped = new HeadlessGame(Board.startingPosition(), 1);
        capped.play(capped.legalMoves()[0]);
        assertThrows(IllegalArgumentException.class, () -> TrajectorySampler.sample(capped.trajectory(), 32));
        GameTrajectory empty = HeadlessGameTest.game("4k3/8/8/8/8/8/8/4K3 w - - 0 1", 20).trajectory();
        assertTrue(TrajectorySampler.sample(empty, 32).isEmpty());
    }

    @Test
    void actualTrajectoryTargetsFollowSideToMoveAndSnapshotsAreOwned() {
        GameTrajectory trajectory = HeadlessGameTest.foolsMate().trajectory();
        var samples = TrajectorySampler.sample(trajectory, 32);
        assertEquals(4, samples.size());
        for (int i = 0; i < samples.size(); i++) {
            assertEquals(i % 2 == 0 ? -1.0 : 1.0, samples.get(i).target());
            long[] board = samples.get(i).board();
            board[0] = 0;
            assertArrayEquals(trajectory.positions().get(i).board(), samples.get(i).board());
        }
        var bounded = TrajectorySampler.sample(trajectory, 2);
        assertArrayEquals(trajectory.positions().getFirst().board(), bounded.getFirst().board());
        assertArrayEquals(trajectory.positions().getLast().board(), bounded.getLast().board());
        assertThrows(UnsupportedOperationException.class, samples::clear);
        assertThrows(IllegalArgumentException.class, () -> new TrajectorySampler.Sample(Board.startingPosition(), 0.5));
    }
}
