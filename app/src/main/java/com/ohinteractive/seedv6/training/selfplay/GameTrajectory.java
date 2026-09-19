package com.ohinteractive.seedv6.training.selfplay;

import java.util.List;
import java.util.Optional;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;

/** Immutable stopped game. Positions precede successfully played moves; the final board is separate. */
public final class GameTrajectory {
    public record Position(long[] board, int ply, long playedMove) {
        public Position {
            if (board.length != Board.MAX_BITBOARDS || ply < 0) {
                throw new IllegalArgumentException("Invalid trajectory position.");
            }
            board = board.clone();
        }
        @Override public long[] board() { return board.clone(); }
        public int sideToMove() { return Board.player((int) board[Board.STATUS]); }
    }

    private final List<Position> positions;
    private final long[] finalBoard;
    private final GameHistory history;
    private final GameTermination termination;
    private final String failure;

    GameTrajectory(List<Position> positions, long[] finalBoard, GameHistory history,
                   GameTermination termination, String failure) {
        if (termination == GameTermination.ACTIVE) throw new IllegalStateException("Game is still active.");
        this.positions = List.copyOf(positions);
        this.finalBoard = finalBoard.clone();
        history.requireCurrent(finalBoard);
        this.history = history;
        this.termination = termination;
        this.failure = failure;
    }

    public List<Position> positions() { return positions; }
    public long[] finalBoard() { return finalBoard.clone(); }
    public GameHistory history() { return history; }
    public int playedPlies() { return positions.size(); }
    public GameTermination termination() { return termination; }
    public Optional<GameResult> result() { return termination.result(); }
    public Optional<String> failure() { return Optional.ofNullable(failure); }
}
