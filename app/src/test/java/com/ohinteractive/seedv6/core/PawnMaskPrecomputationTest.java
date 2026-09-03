package com.ohinteractive.seedv6.core;

import com.ohinteractive.seedv6.core.util.Bitboard;
import com.ohinteractive.seedv6.core.util.Value;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PawnMaskPrecomputationTest {

    private static final long[] FILE = Bitboard.BB[Bitboard.FILE];
    private static final long[] RANK = Bitboard.BB[Bitboard.RANK];
    private static final long[][] FORWARD_RANKS = {
        Bitboard.BB[Bitboard.FORWARD_RANKS_PLAYER0],
        Bitboard.BB[Bitboard.FORWARD_RANKS_PLAYER1]
    };

    @Test
    void squareOnlyMasksExactlyMatchOldDynamicExpressionsForEverySquare() {
        for (int square = 0; square < 64; square++) {
            int file = square & Value.FILE;
            int rank = square >>> 3;
            long adjacentFiles = (file > 0 ? FILE[file - 1] : 0L)
                    | (file < 7 ? FILE[file + 1] : 0L);

            assertEquals(adjacentFiles, Eval.adjacentFileMask(square),
                    context("adjacent-file", square, -1));
            assertEquals(adjacentFiles & RANK[rank], Eval.phalanxMask(square),
                    context("phalanx", square, -1));
        }
    }

    @Test
    void sideAndSquareMasksExactlyMatchOldDynamicExpressionsExhaustively() {
        for (int player = Value.WHITE; player <= Value.BLACK; player++) {
            for (int square = 0; square < 64; square++) {
                int file = square & Value.FILE;
                int rank = square >>> 3;
                long adjacentFiles = (file > 0 ? FILE[file - 1] : 0L)
                        | (file < 7 ? FILE[file + 1] : 0L);
                long weakSupport = adjacentFiles
                        & (FORWARD_RANKS[1 ^ player][rank] | RANK[rank]);
                long passedPawn = (FILE[file] | adjacentFiles)
                        & FORWARD_RANKS[player][rank];
                long promotionPath = FORWARD_RANKS[player][rank] & FILE[file];

                assertEquals(weakSupport, Eval.weakPawnSupportMask(player, square),
                        context("weak-support", square, player));
                assertEquals(passedPawn, Eval.passedPawnMask(player, square),
                        context("passed-pawn", square, player));
                assertEquals(promotionPath, Eval.promotionPathMask(player, square),
                        context("promotion-path", square, player));
            }
        }
    }

    private static String context(String mask, int square, int player) {
        int file = square & Value.FILE;
        int rank = square >>> 3;
        return mask + " player=" + player + " square=" + square
                + " file=" + file + " rank=" + rank;
    }
}
