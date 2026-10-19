package com.ohinteractive.seedv6.core.brnnext;

/** Immutable wave-one layouts. Wire IDs are explicit and never enum ordinals. */
public enum BrnNextArchitecture {
    GATED_PAIRS(1, 4), CUBIC(2, 16), POOLED(3, 16);

    public static final int FEATURES = 12 * 64;
    public static final int UNARY = 1, EMBEDDING = UNARY + FEATURES;
    public static final int PAIR_STATES = 4 * 13 * 13;
    public static final double PIECE_NORM = 1 / Math.sqrt(32);
    public static final double PAIR_NORM = 1 / Math.sqrt(210);
    public static final double DEGREE2_NORM = 1 / Math.sqrt(496);
    public static final double DEGREE3_NORM = 1 / Math.sqrt(4960);
    private final int wireId, width;

    BrnNextArchitecture(int wireId, int width) { this.wireId = wireId; this.width = width; }
    public int wireId() { return wireId; }
    public int width() { return width; }
    public int head() { return EMBEDDING + FEATURES * width; }
    public int parameters() { return head() + width * (this == GATED_PAIRS ? 1 + PAIR_STATES : 2); }
    public String configurationId() { return name() + "_W" + width + "_PS768_MATERIAL_QUARTER_V1"; }
}
