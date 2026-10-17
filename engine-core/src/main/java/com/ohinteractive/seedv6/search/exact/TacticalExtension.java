package com.ohinteractive.seedv6.search.exact;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;

/** SR-032 boundary hypotheses; explicit research controls, never production defaults.
 * One added main ply per real path; no score threshold or check-extension policy. */
public enum TacticalExtension {
    OFF, RECAPTURE, PROMOTION, UNIQUE;

    boolean eligible(long move,long previous,boolean complete,int legalCount) {
        return switch(this) {
            case OFF -> false;
            case RECAPTURE -> previous!=0
                    && ((previous>>>Board.TARGET_PIECE_SHIFT)&Board.PIECE_BITS)!=0
                    && ((move>>>Board.TARGET_PIECE_SHIFT)&Board.PIECE_BITS)!=0
                    && ((move>>>Board.PROMOTE_PIECE_SHIFT)&Board.PIECE_BITS)==0
                    && Move.toSquare(move)==Move.toSquare(previous);
            case PROMOTION -> ((move>>>Board.PROMOTE_PIECE_SHIFT)&Board.PIECE_BITS)!=0;
            case UNIQUE -> complete&&legalCount==1;
        };
    }
}
