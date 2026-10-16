package com.ohinteractive.seedv6.uci;

import com.ohinteractive.seedv6.core.move.MoveIntent;
import com.ohinteractive.seedv6.core.move.MoveIntent.Promotion;

final class UciMoveParser {

    static MoveIntent parse(String coordinate) {
        if(coordinate == null || (coordinate.length() != 4 && coordinate.length() != 5)) {
            throw new IllegalArgumentException("A UCI move must contain four coordinates and an optional promotion.");
        }
        final int from = square(coordinate.charAt(0), coordinate.charAt(1));
        final int to = square(coordinate.charAt(2), coordinate.charAt(3));
        final Promotion promotion = coordinate.length() == 4
            ? Promotion.NONE
            : switch(Character.toLowerCase(coordinate.charAt(4))) {
                case 'q' -> Promotion.QUEEN;
                case 'r' -> Promotion.ROOK;
                case 'b' -> Promotion.BISHOP;
                case 'n' -> Promotion.KNIGHT;
                default -> throw new IllegalArgumentException("Unsupported UCI promotion suffix.");
            };
        return new MoveIntent(from, to, promotion);
    }

    private static int square(char file, char rank) {
        if(file < 'a' || file > 'h' || rank < '1' || rank > '8') {
            throw new IllegalArgumentException("Invalid UCI square.");
        }
        return (rank - '1') * 8 + file - 'a';
    }

    private UciMoveParser() {}
}
