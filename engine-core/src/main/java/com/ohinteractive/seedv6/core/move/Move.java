package com.ohinteractive.seedv6.core.move;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;

public class Move {

    public static final int QUIET = 0b000;
    public static final int PAWN_PUSH = 0b001;
    public static final int PAWN_DOUBLE_PUSH = 0b010;
    public static final int CASTLE = 0b011;
    public static final int CAPTURE = 0b100;
    public static final int EN_PASSANT = 0b101;
    public static final int PROMOTION = 0b110;
    public static final int CAPTURE_PROMOTION = 0b111;

    public static final int MOVE_TYPE_SHIFT = 24;
    public static final long MOVE_TYPE_MASK = 0b111L << MOVE_TYPE_SHIFT;
    public static final long QUIET_BITS = (long) QUIET << MOVE_TYPE_SHIFT;
    public static final long PAWN_PUSH_BITS = (long) PAWN_PUSH << MOVE_TYPE_SHIFT;
    public static final long PAWN_DOUBLE_PUSH_BITS = (long) PAWN_DOUBLE_PUSH << MOVE_TYPE_SHIFT;
    public static final long CASTLE_BITS = (long) CASTLE << MOVE_TYPE_SHIFT;
    public static final long CAPTURE_BITS = (long) CAPTURE << MOVE_TYPE_SHIFT;
    public static final long EN_PASSANT_BITS = (long) EN_PASSANT << MOVE_TYPE_SHIFT;
    public static final long PROMOTION_BITS = (long) PROMOTION << MOVE_TYPE_SHIFT;
    public static final long CAPTURE_PROMOTION_BITS = (long) CAPTURE_PROMOTION << MOVE_TYPE_SHIFT;

    public static final int UNUSED_MOVE_BIT_SHIFT = 27;
    public static final long UNUSED_MOVE_BIT_MASK = 1L << UNUSED_MOVE_BIT_SHIFT;

    /*
     * Castling-change bits use the same order as Board's unshifted castling rights:
     * bit 28 white kingside, bit 29 white queenside,
     * bit 30 black kingside, bit 31 black queenside.
     * A set bit removes that right; it never supplies replacement rights.
     */
    public static final int CASTLING_CHANGE_SHIFT = 28;
    public static final long CASTLING_CHANGE_MASK = 0b1111L << CASTLING_CHANGE_SHIFT;
    public static final long WHITE_KINGSIDE_CHANGE_MASK = 0b0001L << CASTLING_CHANGE_SHIFT;
    public static final long WHITE_QUEENSIDE_CHANGE_MASK = 0b0010L << CASTLING_CHANGE_SHIFT;
    public static final long BLACK_KINGSIDE_CHANGE_MASK = 0b0100L << CASTLING_CHANGE_SHIFT;
    public static final long BLACK_QUEENSIDE_CHANGE_MASK = 0b1000L << CASTLING_CHANGE_SHIFT;
    public static final long WHITE_CASTLING_CHANGE_MASK =
        WHITE_KINGSIDE_CHANGE_MASK | WHITE_QUEENSIDE_CHANGE_MASK;
    public static final long BLACK_CASTLING_CHANGE_MASK =
        BLACK_KINGSIDE_CHANGE_MASK | BLACK_QUEENSIDE_CHANGE_MASK;
    public static final long EXPERIMENTAL_METADATA_MASK = MOVE_TYPE_MASK | CASTLING_CHANGE_MASK;

    public static int moveType(long move) {
        return (int) (move >>> MOVE_TYPE_SHIFT) & 0b111;
    }

    public static int castlingChange(long move) {
        return (int) (move >>> CASTLING_CHANGE_SHIFT) & 0b1111;
    }

    public static long withoutExperimentalMetadata(long move) {
        return move & ~EXPERIMENTAL_METADATA_MASK;
    }

    public static int fromSquare(long move) {
        return (int) move & Board.SQUARE_BITS;
    }

    public static int toSquare(long move) {
        return (int) (move >>> Board.TARGET_SQUARE_SHIFT) & Board.SQUARE_BITS;
    }

    public static MoveIntent.Promotion promotion(long move) {
        final int promotePiece = (int) (move >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS;
        return switch (promotePiece & Piece.TYPE) {
            case Value.NONE -> MoveIntent.Promotion.NONE;
            case Piece.QUEEN -> MoveIntent.Promotion.QUEEN;
            case Piece.ROOK -> MoveIntent.Promotion.ROOK;
            case Piece.BISHOP -> MoveIntent.Promotion.BISHOP;
            case Piece.KNIGHT -> MoveIntent.Promotion.KNIGHT;
            default -> throw new IllegalArgumentException("Unsupported promotion piece in move: " + promotePiece);
        };
    }

    public static String coordinate(long move) {
        final String suffix = switch (promotion(move)) {
            case NONE -> "";
            case QUEEN -> "q";
            case ROOK -> "r";
            case BISHOP -> "b";
            case KNIGHT -> "n";
        };
        return Board.squareToString(fromSquare(move)) + Board.squareToString(toSquare(move)) + suffix;
    }

    public static String string(long move) {
        int promotePiece = (int) move >>> Board.PROMOTE_PIECE_SHIFT & Board.PIECE_BITS;
        return Board.squareToString((int) move & Board.SQUARE_BITS) + Board.squareToString((int) move >>> Board.TARGET_SQUARE_SHIFT & Board.SQUARE_BITS)
                + (promotePiece == Value.NONE ? "" : Piece.SHORT_STRING[promotePiece].toUpperCase());
    }

    /** SAN using authoritative legal moves (pinned alternatives never disambiguate). */
    public static String notation(long[] board, long move) {
        long[] legal = new long[256], child = new long[Board.MAX_BITBOARDS];
        int count = Gen.genAll(board[0], board[1], board[2], board[3],
                (int) board[Board.STATUS], board[Board.KEY], true, legal, child);
        Board.makeMoveInto(board[0], board[1], board[2], board[3],
                (int) board[Board.STATUS], board[Board.KEY], move, child);
        boolean check = Board.isPlayerInCheckPext(child[0], child[1], child[2], child[3],
                Board.player((int) child[Board.STATUS]));
        boolean mate = check && Gen.genAll(child[0], child[1], child[2], child[3],
                (int) child[Board.STATUS], child[Board.KEY], true,
                new long[256], new long[Board.MAX_BITBOARDS]) == 0;
        return notation(move, legal, count, check, mate);
    }

    /** Reuses a caller's legal source moves and adjudicated child; no generation or board mutation. */
    public static String notation(long move, long[] legal, int count, boolean check, boolean mate) {
        int from = fromSquare(move), to = toSquare(move);
        int piece = (int) (move >>> Board.START_PIECE_SHIFT) & Board.PIECE_BITS;
        int type = piece & Piece.TYPE;
        StringBuilder san = new StringBuilder(12);
        if (type == Piece.KING && Math.abs(from - to) == 2) {
            san.append((to & 7) == 6 ? "O-O" : "O-O-O");
        } else {
            if (type != Piece.PAWN) {
                san.append(Piece.SHORT_STRING[piece].toUpperCase(java.util.Locale.ROOT));
                boolean alternative = false, sameFile = false, sameRank = false;
                for (int i = 0; i < count; i++) {
                    long other = legal[i]; int source = fromSquare(other);
                    if (source == from || toSquare(other) != to
                            || ((other >>> Board.START_PIECE_SHIFT) & Board.PIECE_BITS) != piece) continue;
                    alternative = true;
                    sameFile |= (source & 7) == (from & 7);
                    sameRank |= (source >>> 3) == (from >>> 3);
                }
                if (alternative) {
                    if (!sameFile || sameRank) san.append((char) ('a' + (from & 7)));
                    if (sameFile) san.append((char) ('1' + (from >>> 3)));
                }
            }
            boolean capture = ((move >>> Board.TARGET_PIECE_SHIFT) & Board.PIECE_BITS) != 0
                    || type == Piece.PAWN && (from & 7) != (to & 7);
            if (capture) {
                if (type == Piece.PAWN) san.append((char) ('a' + (from & 7)));
                san.append('x');
            }
            san.append(Board.squareToString(to));
            int promoted = (int) (move >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS;
            if (promoted != 0) san.append('=').append(Piece.SHORT_STRING[promoted].toUpperCase(java.util.Locale.ROOT));
        }
        if (mate) san.append('#'); else if (check) san.append('+');
        return san.toString();
    }
}
