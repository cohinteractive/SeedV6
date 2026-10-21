package com.ohinteractive.seedv6.core.brnnext;

/** Immutable successor layouts, including explicitly bound frozen-base extensions. Wire IDs are explicit and never enum ordinals. */
public enum BrnNextArchitecture {
    GATED_PAIRS(1, 4), CUBIC(2, 16), POOLED(3, 16),
    GEN32_GATED(4, 4), GEN32_CUBIC(5, 16), GEN32_CUBIC_R4(6, 4),
    GEN32_QUADRATIC(7, 16), GEN32_QUADRATIC_R4(8, 4),
    GEN32_FIXED_QUADRATIC_R4(9, 4), GEN32_FIXED_CUBIC_R4(10, 4),
    GEN32_CAL_AFFINE_R4(11, 4), GEN32_CAL_QUADRATIC_R4(12, 4), GEN32_CAL_CUBIC_R4(13, 4);

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
    public boolean calibratedHeads() { return this == GEN32_CAL_AFFINE_R4 || this == GEN32_CAL_QUADRATIC_R4 || this == GEN32_CAL_CUBIC_R4; }
    public int affineHead() { if (!calibratedHeads()) throw new IllegalStateException("Calibrated layout required"); return head()+width; }
    public int extraHead() { return affineHead()+4; }
    public boolean frozenCubic() { return this == GEN32_CUBIC || this == GEN32_CUBIC_R4 || this == GEN32_FIXED_CUBIC_R4 || this == GEN32_CAL_CUBIC_R4; }
    public boolean quadratic() { return this == GEN32_QUADRATIC || this == GEN32_QUADRATIC_R4 || this == GEN32_FIXED_QUADRATIC_R4 || this == GEN32_CAL_AFFINE_R4 || this == GEN32_CAL_QUADRATIC_R4; }
    public boolean fixedBasis() { return this == GEN32_FIXED_QUADRATIC_R4 || this == GEN32_FIXED_CUBIC_R4 || calibratedHeads(); }
    /** Fixed-basis families explicitly exclude the anchor from Adam updates. */
    public int trainableStart() { return calibratedHeads() ? affineHead() : fixedBasis() ? head() + (this == GEN32_FIXED_CUBIC_R4 ? width : 0) : 0; }
    public boolean frozenBase() { return this == GEN32_GATED || frozenCubic() || quadratic(); }
    /** Degree2 is an additive pairwise control, not a higher-order successor claim. */
    public boolean polynomial() { return correctionKind() == CUBIC || quadratic(); }
    public BrnNextArchitecture correctionKind() {
        return switch(this) { case GEN32_GATED -> GATED_PAIRS; case GEN32_CUBIC, GEN32_CUBIC_R4, GEN32_FIXED_CUBIC_R4, GEN32_CAL_CUBIC_R4 -> CUBIC; default -> this; };
    }
    public int parameters() { return calibratedHeads() ? extraHead()+(this==GEN32_CAL_AFFINE_R4?0:width) : head() + width * (correctionKind() == GATED_PAIRS ? 1 + PAIR_STATES : quadratic() ? 1 : 2); }
    public String configurationId() {
        return name() + "_W" + width + "_PS768_MATERIAL_QUARTER_V1"
                + (frozenBase() ? "_BASE_" + BrnNextBase.SHA256 : "")
                + (calibratedHeads() ? "_HALF_ANCHOR_AFFINE_SEARCH_OBJECTIVE_V1" : "");
    }
}
