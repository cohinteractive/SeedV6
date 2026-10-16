package com.ohinteractive.seedv6.gui;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.OptionalInt;

/** Pure white-at-bottom mapping for a centered, resizable square board. */
public final class SquareMapper {

    public Rectangle boardBounds(int componentWidth, int componentHeight) {
        final int size = Math.max(0, Math.min(componentWidth, componentHeight));
        return new Rectangle(
            Math.max(0, (componentWidth - size) / 2),
            Math.max(0, (componentHeight - size) / 2),
            size,
            size
        );
    }

    public OptionalInt squareAt(int x, int y, int componentWidth, int componentHeight) {
        final Rectangle board = boardBounds(componentWidth, componentHeight);
        if(board.width == 0 || x < board.x || y < board.y
            || x >= board.x + board.width || y >= board.y + board.height) {
            return OptionalInt.empty();
        }
        final int file = (int) ((long) (x - board.x) * BOARD_DIMENSION / board.width);
        final int displayRank = (int) ((long) (y - board.y) * BOARD_DIMENSION / board.height);
        return OptionalInt.of((BOARD_DIMENSION - 1 - displayRank) * BOARD_DIMENSION + file);
    }

    public Rectangle squareBounds(
        int square, int componentWidth, int componentHeight
    ) {
        requireSquare(square);
        final Rectangle board = boardBounds(componentWidth, componentHeight);
        final int file = square & 7;
        final int displayRank = BOARD_DIMENSION - 1 - (square >>> 3);
        final int left = board.x + file * board.width / BOARD_DIMENSION;
        final int right = board.x + (file + 1) * board.width / BOARD_DIMENSION;
        final int top = board.y + displayRank * board.height / BOARD_DIMENSION;
        final int bottom = board.y + (displayRank + 1) * board.height / BOARD_DIMENSION;
        return new Rectangle(left, top, right - left, bottom - top);
    }

    public Point squareCenter(int square, int componentWidth, int componentHeight) {
        final Rectangle bounds = squareBounds(square, componentWidth, componentHeight);
        return new Point(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
    }

    private static final int BOARD_DIMENSION = 8;

    private static void requireSquare(int square) {
        if(square < 0 || square >= BOARD_DIMENSION * BOARD_DIMENSION) {
            throw new IllegalArgumentException("Square must be in 0..63: " + square);
        }
    }
}
