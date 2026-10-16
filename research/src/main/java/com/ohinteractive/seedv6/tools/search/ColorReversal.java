package com.ohinteractive.seedv6.tools.search;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Zobrist;

/** Diagnostic snapshot symmetry, never a search move or a production feature transform. */
final class ColorReversal {
    static long[] transform(long[] board) {
        if (board.length != Board.MAX_BITBOARDS) throw new IllegalArgumentException("Expected complete board.");
        long[] result = board.clone();
        long occupied = board[0] | board[1] | board[2];
        if ((board[3] & ~occupied) != 0 || (board[0] & board[1] & board[2]) != 0)
            throw new IllegalArgumentException("Invalid piece encoding.");
        for (int i = 0; i < 3; i++) result[i] = Long.reverseBytes(board[i]);
        result[3] = Long.reverseBytes(board[3] ^ occupied);
        long status = board[Board.STATUS];
        int rights = (int) (status >>> Board.CASTLING_SHIFT) & Board.CASTLING_BITS;
        int reversedRights = ((rights & 3) << 2) | (rights >>> 2);
        int ep = (int) (status >>> Board.ESQUARE_SHIFT) & Board.SQUARE_BITS;
        if (ep != 0 && Board.enPassantSquare((int) status) != ep)
            throw new IllegalArgumentException("Noncanonical en-passant state.");
        int reversedEp = ep == 0 ? 0 : ep ^ 56;
        long mask = ((long) Board.CASTLING_BITS << Board.CASTLING_SHIFT)
                | ((long) Board.SQUARE_BITS << Board.ESQUARE_SHIFT);
        // Preserve halfmove clock, fullmove label and every other status bit exactly.
        // Fullmove numbering increments after Black: snapshot symmetry does not commute
        // with that bookkeeping convention when a mapped move is played.
        result[Board.STATUS] = ((status ^ Board.PLAYER_BIT) & ~mask)
                | ((long) reversedRights << Board.CASTLING_SHIFT) | ((long) reversedEp << Board.ESQUARE_SHIFT);
        int[] pieces = new int[64];
        for (int square = 0; square < 64; square++)
            pieces[square] = Board.getSquare(result[0], result[1], result[2], result[3], square);
        result[Board.KEY] = Zobrist.getKey(pieces, Board.player((int) result[Board.STATUS]), reversedRights, reversedEp);
        return result;
    }

    static String coordinate(String move) {
        char[] chars = move.toCharArray();
        chars[1] = (char) ('9' - chars[1] + '0');
        chars[3] = (char) ('9' - chars[3] + '0');
        return new String(chars);
    }

    private ColorReversal() {}
}
