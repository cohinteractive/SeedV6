package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.rules.DrawAdjudicator.RuleDraw;

/** Immutable controller-facing adjudication of the current authoritative position. */
record PositionStatus(
    int sideToMove,
    boolean inCheck,
    Outcome outcome,
    int checkedKingSquare
) {

    enum Outcome {
        ACTIVE,
        CHECKMATE,
        STALEMATE,
        FIFTY_MOVE_DRAW,
        THREEFOLD_DRAW,
        INSUFFICIENT_MATERIAL_DRAW
    }

    boolean terminal() {
        return outcome != Outcome.ACTIVE;
    }

    String displayText() {
        final String side = sideToMove == Value.WHITE ? "White" : "Black";
        return switch(outcome) {
            case ACTIVE -> side + " to move" + (inCheck ? " — check" : "");
            case CHECKMATE -> "Checkmate — " + (sideToMove == Value.WHITE ? "Black" : "White") + " wins";
            case STALEMATE -> "Draw — stalemate";
            case FIFTY_MOVE_DRAW -> "Draw — 50-move rule";
            case THREEFOLD_DRAW -> "Draw — threefold repetition";
            case INSUFFICIENT_MATERIAL_DRAW -> "Draw — insufficient material";
        };
    }

    static Outcome fromRuleDraw(RuleDraw draw) {
        return switch(draw) {
            case NONE -> Outcome.ACTIVE;
            case FIFTY_MOVE -> Outcome.FIFTY_MOVE_DRAW;
            case FORMAL_THREEFOLD -> Outcome.THREEFOLD_DRAW;
            case INSUFFICIENT_MATERIAL -> Outcome.INSUFFICIENT_MATERIAL_DRAW;
        };
    }
}
