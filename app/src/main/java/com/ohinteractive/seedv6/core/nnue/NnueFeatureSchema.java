package com.ohinteractive.seedv6.core.nnue;

import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;

/** Primitive king/piece/square vocabulary. Changing its meaning requires a new version. */
public final class NnueFeatureSchema {

    public static final int VERSION = 1;
    public static final String ID = "seedv6.nnue.halfkp.v1";
    public static final int SQUARE_COUNT = 64;
    public static final int PIECE_CHANNEL_COUNT = 12;
    public static final int FEATURE_COUNT = SQUARE_COUNT * PIECE_CHANNEL_COUNT * SQUARE_COUNT;

    public static final int FRIENDLY_PAWN = 0;
    public static final int FRIENDLY_KNIGHT = 1;
    public static final int FRIENDLY_BISHOP = 2;
    public static final int FRIENDLY_ROOK = 3;
    public static final int FRIENDLY_QUEEN = 4;
    public static final int FRIENDLY_KING = 5;
    public static final int ENEMY_PAWN = 6;
    public static final int ENEMY_KNIGHT = 7;
    public static final int ENEMY_BISHOP = 8;
    public static final int ENEMY_ROOK = 9;
    public static final int ENEMY_QUEEN = 10;
    public static final int ENEMY_KING = 11;

    private NnueFeatureSchema() {}

    /** Colours use Value.WHITE/BLACK; pieceType uses Piece constants, never schema ordinals. */
    public static int relativePieceChannel(int perspective, int pieceColour, int pieceType) {
        requireColour(perspective);
        requireColour(pieceColour);
        int friendlyChannel = switch (pieceType) {
            case Piece.PAWN -> FRIENDLY_PAWN;
            case Piece.KNIGHT -> FRIENDLY_KNIGHT;
            case Piece.BISHOP -> FRIENDLY_BISHOP;
            case Piece.ROOK -> FRIENDLY_ROOK;
            case Piece.QUEEN -> FRIENDLY_QUEEN;
            case Piece.KING -> FRIENDLY_KING;
            default -> throw new IllegalArgumentException("Unknown SeedV6 piece type: " + pieceType);
        };
        return friendlyChannel + (pieceColour == perspective ? 0 : ENEMY_PAWN);
    }

    /** White retains a1=0; Black flips ranks, preserving files. */
    public static int orientSquare(int perspective, int square) {
        requireColour(perspective);
        if (square < 0 || square >= SQUARE_COUNT) {
            throw new IllegalArgumentException("Square must be in [0, 63]: " + square);
        }
        return perspective == Value.WHITE ? square : square ^ 56;
    }

    /** Both square arguments are in the board's ordinary a1=0 orientation. Includes both kings. */
    public static int featureIndex(int perspective, int perspectiveKingSquare,
                                   int pieceColour, int pieceType, int pieceSquare) {
        int king = orientSquare(perspective, perspectiveKingSquare);
        int channel = relativePieceChannel(perspective, pieceColour, pieceType);
        int square = orientSquare(perspective, pieceSquare);
        return ((king * PIECE_CHANNEL_COUNT + channel) * SQUARE_COUNT) + square;
    }

    static void requireColour(int colour) {
        if (colour != Value.WHITE && colour != Value.BLACK) {
            throw new IllegalArgumentException("Colour must be Value.WHITE or Value.BLACK: " + colour);
        }
    }
}
