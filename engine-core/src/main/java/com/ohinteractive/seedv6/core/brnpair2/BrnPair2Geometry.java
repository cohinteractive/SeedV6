package com.ohinteractive.seedv6.core.brnpair2;

import java.util.ArrayList;

/** Shared immutable relation geometry and category encoding for compilation and inference. */
final class BrnPair2Geometry {
    private static final int[][] SHAPES={{0,0,1,0,2,0},{0,0,0,1,0,2},{0,0,1,1,2,2},
            {0,0,-1,1,-2,2},{0,0,1,0,0,1},{0,0,-1,0,0,1},{0,0,2,0,0,2},{0,0,3,0,0,3}};
    private static final int[][] CELLS;
    private static final int[] SHAPE;
    static {
        var cells=new ArrayList<int[]>();var shapes=new ArrayList<Integer>();
        for(int k=0;k<SHAPES.length;k++)for(int y=0;y<8;y++)for(int x=0;x<8;x++) {
            int[] row=new int[3];boolean valid=true;
            for(int j=0;j<3;j++){int a=x+SHAPES[k][2*j],b=y+SHAPES[k][2*j+1];valid&=a>=0&&a<8&&b>=0&&b<8;row[j]=8*b+a;}
            if(valid){cells.add(row);shapes.add(k);}
        }
        CELLS=cells.toArray(int[][]::new);SHAPE=shapes.stream().mapToInt(Integer::intValue).toArray();
        if(CELLS.length!=327)throw new AssertionError("C01 geometry count");
    }
    static final int TEMPLATES=CELLS.length;
    static final double NORM=1/Math.sqrt(2*TEMPLATES);
    static int cell(int template,int endpoint){return CELLS[template][endpoint];}
    static int shape(int template){return SHAPE[template];}
    static int shapeCount(){return SHAPES.length;}
    static void requireOrder(int order){if(order!=2)throw new IllegalArgumentException("BRE-Pair 2 requires order 2");}
    static int category(int code,int perspective){return code==0?0:BrnPair2Features.channel(code^(perspective<<3))+1;}
    static int state(int[] codes,int template,int order){int value=0;for(int j=order-1;j>=0;j--)value=13*value+codes[CELLS[template][j]];return value;}
    private BrnPair2Geometry() {}
}
