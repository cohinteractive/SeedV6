package com.ohinteractive.seedv6.training.selfplay;

import java.util.Optional;

public enum GameTermination {
    ACTIVE(null),
    WHITE_CHECKMATES_BLACK(GameResult.WHITE_WIN),
    BLACK_CHECKMATES_WHITE(GameResult.BLACK_WIN),
    STALEMATE(GameResult.DRAW),
    THREEFOLD_REPETITION(GameResult.DRAW),
    FIFTY_MOVE_RULE(GameResult.DRAW),
    INSUFFICIENT_MATERIAL(GameResult.DRAW),
    PLY_CAP(null), CANCELLED(null), SEARCH_FAILURE(null), INFRASTRUCTURE_FAILURE(null);

    private final GameResult result;

    GameTermination(GameResult result) { this.result = result; }

    public Optional<GameResult> result() { return Optional.ofNullable(result); }
    public boolean completed() { return result != null; }
    public boolean aborted() { return this != ACTIVE && !completed(); }
}
