package com.ohinteractive.seedv6.search.exact;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Pext;
import com.ohinteractive.seedv6.core.util.Piece;

/** Explicit SR-024 experiments over cumulative Search; OFF preserves production. */
public enum SeePolicy {
    OFF,
    /** Omit negative nonchecking captures in qsearch; retain promotions and EP. */
    QSAFE,
    /** As QSAFE, but retain every move at the first qsearch layer. */
    QDEEP,
    /** Preserve original full-window qsearch calls; prune only original scouts. */
    QSCOUT,
    /** Retain exchanges losing at most one material pawn, including minor trades. */
    QPAWN,
    /** Preserve captures uncovering another friendly slider's attack on a piece. */
    QXRAY,
    /** Protected later negative captures at nominal scout depths one through three. */
    MAIN,
    /** Exchange-negative later non-pawn/non-king quiets at scout depths two to four. */
    QUIET,
    /** Retain bad captures, ordering them after quiets in nominal Search. */
    ORDER;

    public boolean prunesQuiescence(int qply) {
        return this == QSAFE || this == QSCOUT || this == QPAWN || this == QXRAY || this == QDEEP && qply > 0;
    }

    /** Geometric clearance safeguard, not a legal continuation or score bound.
     * Caller supplies an ordinary capture and excludes promotions, EP and checks. */
    static boolean opensSliderThreat(long[] parent, long[] child, long move) {
        long occupied = child[0] | child[1] | child[2];
        long ours = occupied & (((int) parent[Board.STATUS] & Board.PLAYER_BIT) == 0 ? ~child[3] : child[3]);
        long pawns = child[0] & ~child[1] & ~child[2];
        long kings = ~child[0] & child[1] & child[2];
        long targets = occupied & ~ours & ~(pawns | kings);
        if(targets == 0) return false;
        int from = (int) move & Board.SQUARE_BITS;
        int to = (int) (move >>> Board.TARGET_SQUARE_SHIFT) & Board.SQUARE_BITS;
        long queens = ~child[0] & child[1] & ~child[2];
        long rooks = child[0] & child[1] & ~child[2];
        long bishops = ~child[0] & ~child[1] & child[2];
        long sliders = ours & ~(1L << to)
                & ((Pext.bishopMoves(from, occupied) & (bishops | queens))
                | (Pext.rookMoves(from, occupied) & (rooks | queens)));
        long beforeOccupied = parent[0] | parent[1] | parent[2];
        while(sliders != 0) {
            int square = Long.numberOfTrailingZeros(sliders); sliders &= sliders - 1;
            int type = Board.getSquare(child[0], child[1], child[2], child[3], square) & Piece.TYPE;
            long before = 0, after = 0;
            if(type != Piece.ROOK) {before |= Pext.bishopMoves(square, beforeOccupied);after |= Pext.bishopMoves(square, occupied);}
            if(type != Piece.BISHOP) {before |= Pext.rookMoves(square, beforeOccupied);after |= Pext.rookMoves(square, occupied);}
            if((after & ~before & targets) != 0) return true;
        }
        return false;
    }

    /** Optional worker-private diagnostic sink; never shared across workers. */
    public static class Diagnostics {
        public long qCandidates, qOmitted, mainCandidates, mainOmitted, checksRetained, demoted;
        public final long[] omittedAtDepth = new long[ExactSearch.MAX_DEPTH + 1];
        public void omitted(long[] board, long[] child, long move, int ply, int depth,
                int qply, int searched, int searchedQuiets, int alpha, int beta) {}
    }
}
