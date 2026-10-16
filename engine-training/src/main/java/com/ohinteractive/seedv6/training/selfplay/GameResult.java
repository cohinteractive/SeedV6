package com.ohinteractive.seedv6.training.selfplay;

import com.ohinteractive.seedv6.core.util.Value;

/** Completed chess result in a fixed White perspective; administrative stops have no result. */
public enum GameResult {
    WHITE_WIN(1), DRAW(0), BLACK_WIN(-1);

    private final int whiteResult;

    GameResult(int whiteResult) { this.whiteResult = whiteResult; }

    public int whiteResult() { return whiteResult; }

    public double target(int sideToMove) {
        if (sideToMove != Value.WHITE && sideToMove != Value.BLACK) {
            throw new IllegalArgumentException("Invalid side to move.");
        }
        return whiteResult == 0 ? 0 : sideToMove == Value.WHITE ? whiteResult : -whiteResult;
    }
}
