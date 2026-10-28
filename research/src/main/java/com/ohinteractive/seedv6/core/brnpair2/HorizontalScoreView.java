package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;

/** Placement/STM score views only: never pass these views to search or move generation. */
final class HorizontalScoreView {
    static void copy(long[] board,long[] out) {
        if(board==out||board.length<Board.MAX_BITBOARDS||out.length<Board.MAX_BITBOARDS)throw new IllegalArgumentException("Distinct complete score views required");
        System.arraycopy(board,0,out,0,Board.MAX_BITBOARDS);
    }
    static void reflect(long[] board,long[] out) {
        copy(board,out);for(int i=0;i<4;i++)out[i]=Long.reverse(Long.reverseBytes(board[i]));
    }
    static boolean canonical(long[] board,long[] out) {
        int stm=Board.player((int)board[4]);long kings=board[0]&~board[1]&~board[2],own=kings&(stm==0?~board[3]:board[3]);
        if(Long.bitCount(own)!=1)throw new IllegalArgumentException("Own king count");
        boolean flip=(Long.numberOfTrailingZeros(own)&7)>3;if(flip)reflect(board,out);else copy(board,out);return flip;
    }
    private HorizontalScoreView(){}
}
