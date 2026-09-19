package com.ohinteractive.seedv6.training.selfplay;

import java.util.ArrayList;
import java.util.List;
import com.ohinteractive.seedv6.core.Board;

/** Result-independent, evenly spaced samples including both ends (middle for a one-sample limit). */
public final class TrajectorySampler {
    public static final int DEFAULT_MAXIMUM_SAMPLES = 32;

    /** One six-long copy per retained sample; no evaluator/search metadata is a target source. */
    public record Sample(long[] board, double target) {
        public Sample {
            if (board.length != Board.MAX_BITBOARDS || (target != -1 && target != 0 && target != 1)) {
                throw new IllegalArgumentException("Expected a six-long board and exact W/D/L target.");
            }
            board = board.clone();
        }
        @Override public long[] board() { return board.clone(); }
        void copyBoardInto(long[] destination) { System.arraycopy(board, 0, destination, 0, board.length); }
    }

    public static List<Sample> sample(GameTrajectory game, int maximumSamples) {
        GameResult result = game.result().orElseThrow(
                () -> new IllegalArgumentException("Incomplete games cannot supply targets."));
        int[] indexes = indexes(game.positions().size(), maximumSamples);
        List<Sample> samples = new ArrayList<>(indexes.length);
        for (int index : indexes) {
            GameTrajectory.Position position = game.positions().get(index);
            samples.add(new Sample(position.board(), result.target(position.sideToMove())));
        }
        return List.copyOf(samples);
    }

    public static int[] indexes(int positions, int maximumSamples) {
        if (positions < 0 || maximumSamples < 1) throw new IllegalArgumentException("Invalid sample bounds.");
        int count = Math.min(positions, maximumSamples);
        int[] indexes = new int[count];
        for (int i = 0; i < count; i++) {
            indexes[i] = count == 1 ? (positions - 1) / 2 : (int) ((long) i * (positions - 1) / (count - 1));
        }
        return indexes;
    }

    private TrajectorySampler() {}
}
