package com.ohinteractive.seedv6.core.util;

public class Value {
    
    public static final int INVALID = Integer.MIN_VALUE;
    public static final int NONE = 0;
    public static final int WHITE = 0;
    public static final int BLACK = 1;
    public static final int FILE = 7;
    public static final int KING_SIDE = 0;
    public static final int QUEEN_SIDE = 1;
    public static final int FILE_C = 2;
    public static final int FILE_G = 6;
    public static final int[] KINGSIDE_BIT = { 0b1, 0b100 };
    public static final int[] QUEENSIDE_BIT = { 0b10, 0b1000 };

    public static final String FILE_STRING = "abcdefgh";
}
