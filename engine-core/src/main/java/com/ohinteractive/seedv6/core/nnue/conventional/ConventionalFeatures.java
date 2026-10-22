package com.ohinteractive.seedv6.core.nnue.conventional;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;

/** Bullet ChessBucketsMirrored, schema S6CNU001. Both kings are input features. */
public final class ConventionalFeatures {
    public static final int COUNT = 7680;
    private static final int[] BUCKETS = {
        0, 1, 2, 3, 4, 4, 5, 5, 6, 6, 6, 6, 7, 7, 7, 7,
        8, 8, 8, 8, 8, 8, 8, 8, 9, 9, 9, 9, 9, 9, 9, 9
    };
    private ConventionalFeatures() {}

    public static int context(int perspective, int kingSquare) {
        if ((perspective != Value.WHITE && perspective != Value.BLACK) || kingSquare < 0 || kingSquare >= 64)
            throw new IllegalArgumentException("Invalid perspective/king square");
        int king = kingSquare ^ (perspective == Value.BLACK ? 56 : 0);
        int mirror = (king & 7) > 3 ? 7 : 0;
        return BUCKETS[(king >>> 3) * 4 + ((king ^ mirror) & 7)] * 2 + (mirror == 0 ? 0 : 1);
    }

    public static int index(int perspective, int kingSquare, int piece, int square) {
        int type = piece & Piece.TYPE;
        if (square < 0 || square >= 64 || type < Piece.KING || type > Piece.PAWN || piece < 0 || piece > 14)
            throw new IllegalArgumentException("Invalid piece/square");
        int context = context(perspective, kingSquare);
        int channel = 6 - type + ((piece >>> Board.PLAYER_SHIFT) == perspective ? 0 : 6);
        int oriented = square ^ (perspective == Value.BLACK ? 56 : 0) ^ ((context & 1) == 0 ? 0 : 7);
        return (context >>> 1) * 768 + channel * 64 + oriented;
    }

    static int king(long[] board, int perspective) {
        long kings = board[0] & ~board[1] & ~board[2];
        return Long.numberOfTrailingZeros(kings & (perspective == Value.WHITE ? ~board[3] : board[3]));
    }

    static void requireBoard(long[] board) {
        if (board == null || board.length < Board.MAX_BITBOARDS) throw new IllegalArgumentException("Invalid board length");
        long occupied = board[0] | board[1] | board[2];
        long kings = board[0] & ~board[1] & ~board[2];
        if ((board[3] & ~occupied) != 0 || (board[0] & board[1] & board[2]) != 0
                || Long.bitCount(kings & ~board[3]) != 1 || Long.bitCount(kings & board[3]) != 1)
            throw new IllegalArgumentException("Invalid pieces or king count");
    }
}
