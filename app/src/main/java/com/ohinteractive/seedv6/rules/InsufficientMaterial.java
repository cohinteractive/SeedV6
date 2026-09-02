package com.ohinteractive.seedv6.rules;

/**
 * Conservative material-only automatic-draw detector. This is deliberately
 * not a complete FIDE dead-position solver.
 */
public final class InsufficientMaterial {

    public static boolean isAutomaticDraw(long board0, long board1, long board2, long board3) {
        final long queens = ~board0 & board1 & ~board2;
        final long rooks = board0 & board1 & ~board2;
        final long bishops = ~board0 & ~board1 & board2;
        final long knights = board0 & ~board1 & board2;
        final long pawns = ~board0 & board1 & board2;
        if((queens | rooks | pawns) != 0L) return false;

        final int minorCount = Long.bitCount(bishops | knights);
        if(minorCount <= 1) return true;

        // Any number of bishops is dead only when every bishop is confined to
        // the same square-colour complex and there are no knights.
        return knights == 0L
            && ((bishops & LIGHT_SQUARES) == 0L || (bishops & ~LIGHT_SQUARES) == 0L);
    }

    private static final long LIGHT_SQUARES = 0x55AA55AA55AA55AAL;

    private InsufficientMaterial() {}
}
