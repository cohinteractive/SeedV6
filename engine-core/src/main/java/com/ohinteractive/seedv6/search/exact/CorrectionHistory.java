package com.ohinteractive.seedv6.search.exact;

import java.util.Arrays;
import com.ohinteractive.seedv6.core.Board;

/** CH-C1: bounded private pawn-context prediction, never a proof/TT score.
 * Values have four fractional bits; separate side-to-move tables generalize
 * across nonpawn moves. Hash collisions are deliberate heuristic sharing.
 * Lifetime follows REQUEST_LEARNING, including its conservative abort clearing. */
final class CorrectionHistory {
    static final int SIZE=16384, SCALE=16, LIMIT=256*SCALE;
    final short[] values=new short[SIZE*2];
    Diagnostics diagnostics;

    static int index(long[] board) {
        long pawns=~board[0]&board[1]&board[2];
        long white=pawns&~board[3],black=pawns&board[3];
        long key=white*0x9e3779b97f4a7c15L+Long.rotateLeft(black,31)*0xbf58476d1ce4e5b9L;
        key=(key^(key>>>30))*0xbf58476d1ce4e5b9L;
        key=(key^(key>>>27))*0x94d049bb133111ebL;
        return ((int)(key^(key>>>31))&(SIZE-1))+Board.player((int)board[Board.STATUS])*SIZE;
    }
    void clear() { Arrays.fill(values,(short)0); }
    int apply(long[] board,int raw) {
        int correction=values[index(board)]/SCALE;
        int result=Math.max(-ExactSearch.MAX_STATIC_SCORE,Math.min(ExactSearch.MAX_STATIC_SCORE,raw+correction));
        if(diagnostics!=null) {
            diagnostics.reads++;if(result!=raw)diagnostics.changed++;
            diagnostics.absoluteCorrection+=Math.abs(result-raw);
            diagnostics.maxCorrection=Math.max(diagnostics.maxCorrection,Math.abs(result-raw));
        }
        return result;
    }
    /** Upper/lower selective outcomes update only in the supported direction.
     * The depth-weighted EMA targets bounded search-minus-RAW evaluation, while
     * eligibility compares to the WORKING evaluation actually used at entry. */
    void update(long[] board,int depth,int alpha,int beta,int raw,int working,int score) {
        int bound=score<=alpha?0:score>=beta?2:1;
        if((bound==0 && score>=working)||(bound==2 && score<=working)) {
            if(diagnostics!=null)diagnostics.directionRejected++;
            return;
        }
        int slot=index(board),target=Math.max(-LIMIT,Math.min(LIMIT,(score-raw)*SCALE));
        int before=values[slot],weight=Math.min(depth,8);
        values[slot]=(short)(before+(target-before)*weight/32);
        if(diagnostics!=null) {
            diagnostics.updates[bound]++;if(values[slot]!=before)diagnostics.changedUpdates++;
            if(Math.abs(values[slot])>=LIMIT-16)diagnostics.nearSaturation++;
        }
    }
    /** Optional research observation survives table resets; never changes policy. */
    static final class Diagnostics {
        long reads,changed,absoluteCorrection,maxCorrection,directionRejected,changedUpdates,nearSaturation;
        long guardRejected,ruleRejected,improvingChanged;
        final long[] updates=new long[3]; // selective upper, window-interior, lower
    }
}
