package com.ohinteractive.seedv6.core.move;

import java.util.Objects;

public record MoveIntent(int fromSquare, int toSquare, Promotion promotion) {

    public enum Promotion {
        NONE,
        QUEEN,
        ROOK,
        BISHOP,
        KNIGHT
    }

    public MoveIntent(int fromSquare, int toSquare) {
        this(fromSquare, toSquare, Promotion.NONE);
    }

    public MoveIntent {
        if(fromSquare < 0 || fromSquare > 63) {
            throw new IllegalArgumentException("Source square must be in 0..63: " + fromSquare);
        }
        if(toSquare < 0 || toSquare > 63) {
            throw new IllegalArgumentException("Destination square must be in 0..63: " + toSquare);
        }
        Objects.requireNonNull(promotion, "promotion");
    }

}
