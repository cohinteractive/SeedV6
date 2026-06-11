package com.ohinteractive.seedv6.core;

import com.ohinteractive.seedv6.core.util.Value;

public class Eval {

    public static void main(String[] args) {
        long[] board = Board.fromFen("rnb1kbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        System.out.printf("Eval = %d%n", eval(board[0], board[1], board[2], board[3], (int) board[4], board[5]));
    }

    public static int eval(long board0, long board1, long board2, long board3, int status, long key) {
        final long whiteOccupancy = ~board3;
        final long blackOccupancy = board3;
        final long queensBitboard = ~board0 & board1 & ~board2;
        final long rooksBitboard = board0 & board1 & ~board2;
        final long bishopsBitboard = ~board0 & ~board1 & board2;
        final long knightsBitboard = board0 & ~board1 & board2;
        final long pawnsBitboard = ~board0 & board1 & board2;
        final long whiteQueens = queensBitboard & whiteOccupancy;
        final long whiteRooks = rooksBitboard & whiteOccupancy;
        final long whiteBishops = bishopsBitboard & whiteOccupancy;
        final long whiteKnights = knightsBitboard & whiteOccupancy;
        final long whitePawns = pawnsBitboard & whiteOccupancy;
        final long blackQueens = queensBitboard & blackOccupancy;
        final long blackRooks = rooksBitboard & blackOccupancy;
        final long blackBishops = bishopsBitboard & blackOccupancy;
        final long blackKnights = knightsBitboard & blackOccupancy;
        final long blackPawns = pawnsBitboard & blackOccupancy;
        final int whiteQueenCount = Long.bitCount(whiteQueens);
        final int whiteRookCount = Long.bitCount(whiteRooks);
        final int whiteBishopCount = Long.bitCount(whiteBishops);
        final int whiteKnightCount = Long.bitCount(whiteKnights);
        final int whitePawnCount = Long.bitCount(whitePawns);
        final int blackQueenCount = Long.bitCount(blackQueens);
        final int blackRookCount = Long.bitCount(blackRooks);
        final int blackBishopCount = Long.bitCount(blackBishops);
        final int blackKnightCount = Long.bitCount(blackKnights);
        final int blackPawnCount = Long.bitCount(blackPawns);
        int score =
            (whiteQueenCount - blackQueenCount) * QUEEN_VALUE +
            (whiteRookCount - blackRookCount) * ROOK_VALUE +
            (whiteBishopCount - blackBishopCount) * BISHOP_VALUE +
            (whiteKnightCount - blackKnightCount) * KNIGHT_VALUE +
            (whitePawnCount - blackPawnCount) * PAWN_VALUE;
        if(whiteBishopCount >= 2) score += 30;
        if(blackBishopCount >= 2) score -= 30;
        if(isInsufficientMaterial(
            whiteQueenCount, whiteRookCount, whiteBishopCount, whiteKnightCount, whitePawnCount,
            blackQueenCount, blackRookCount, blackBishopCount, blackKnightCount, blackPawnCount
        )) score = 0;
        final int player = status & Board.PLAYER_BIT;
        return player == Value.WHITE ? score : -score;
    }

    private static final int QUEEN_VALUE = 900;
    private static final int ROOK_VALUE = 500;
    private static final int BISHOP_VALUE = 330;
    private static final int KNIGHT_VALUE = 320;
    private static final int PAWN_VALUE = 100;

    private Eval() {}

    private static boolean isInsufficientMaterial(
        int whiteQueens, int whiteRooks, int whiteBishops, int whiteKnights, int whitePawns,
        int blackQueens, int blackRooks, int blackBishops, int blackKnights, int blackPawns
    ) {
        if(whitePawns != 0 || blackPawns != 0 || whiteRooks != 0 || blackRooks != 0 || whiteQueens != 0 || blackQueens != 0) return false;
        return (whiteKnights + whiteBishops + blackKnights + blackBishops) <= 1;
    }
    
}
