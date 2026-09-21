package com.ohinteractive.seedv6.training.telemetry;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Value;

/** Immutable presentation only. Never training samples, search state or durable evidence. */
public record ActiveGameSnapshot(Phase phase, long generation, long gameId, long version,
                                 int gameOrdinal, int gameInPair, int playedPlies,
                                 Participant white, Participant black, long[] board, long lastMove,
                                 MoveEvaluation evaluation, long startedNanos, long publishedNanos) {
    public enum Phase { SELF_PLAY, VALIDATION }
    public enum Role { LATEST_TRAINING, CANDIDATE, BEST }
    public record Participant(Role role, String checkpointId) {}

    /**
     * Final completed root search that selected lastMove, BEFORE that move was applied.
     * Sign normalized to White; NNUE mapping units are uncalibrated (mate bands remain mate).
     * Not a fresh evaluation of the resulting board, a probability, or a validation match score.
     * sourcePositionKey and searchingSide tie it to the previous position and actual actor.
     */
    public record MoveEvaluation(int whiteScore, int searchingSide, int depth, long sourcePositionKey) {}

    public ActiveGameSnapshot { board = board.clone(); }
    @Override public long[] board() { return board.clone(); }
    public int sideToMove() { return Board.player((int) board[Board.STATUS]); }
    public long positionKey() { return board[Board.KEY]; }
    public int lastFrom() { return lastMove == 0 ? -1 : Move.fromSquare(lastMove); }
    public int lastTo() { return lastMove == 0 ? -1 : Move.toSquare(lastMove); }
    public Participant searchingParticipant() {
        return evaluation == null ? null : evaluation.searchingSide() == Value.WHITE ? white : black;
    }
}
