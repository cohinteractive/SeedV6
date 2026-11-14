package com.ohinteractive.seedv6.core.nnue.conventional;

import com.ohinteractive.seedv6.core.Board;

/** Independently specified local-file pawn pairs, without a king bucket. */
public final class Phase2Features {
    public static final int PAIRS=4560;
    private Phase2Features() {}
    public static long pawns(long[] board) { return board[1]&board[2]&~board[0]; }
    public static int pair(long[] board,int perspective,int king,int a,int b) {
        if(a==b || a<8 || a>=56 || b<8 || b>=56 || Math.abs(a%8-b%8)>1)
            throw new IllegalArgumentException("Invalid local-file pawn pair");
        int flip=(perspective==1?56:0)^((king&7)>3?7:0);
        int ca=(int)(board[3]>>>a)&1,cb=(int)(board[3]>>>b)&1;
        int x=(ca^perspective)*48+(a^flip)-8,y=(cb^perspective)*48+(b^flip)-8;
        int hi=Math.max(x,y),lo=Math.min(x,y);
        return ConventionalFeatures.COUNT+hi*(hi-1)/2+lo;
    }
    public static int[] all(long[] board,int perspective,boolean includePairs) {
        var indices=new java.util.ArrayList<Integer>();int king=ConventionalFeatures.king(board,perspective);
        for(int s=0;s<64;s++) {
            int piece=Board.getSquare(board[0],board[1],board[2],board[3],s);
            if(piece!=0)indices.add(ConventionalFeatures.index(perspective,king,piece,s));
        }
        if(includePairs) {
            long outer=pawns(board);
            while(outer!=0) {
                int a=Long.numberOfTrailingZeros(outer);outer&=outer-1;long inner=outer;
                while(inner!=0) {
                    int b=Long.numberOfTrailingZeros(inner);inner&=inner-1;
                    if(Math.abs(a%8-b%8)<=1)indices.add(pair(board,perspective,king,a,b));
                }
            }
        }
        return indices.stream().sorted().mapToInt(Integer::intValue).toArray();
    }
}
