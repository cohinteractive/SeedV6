package com.ohinteractive.seedv6.training.data;

/** Raw label interpretation, independent of the bytes carrying it and of the student. */
public enum LabelProfile {
    STOCKFISH_CP_MATE_V1,
    BT4_Q_V1;

    public static LabelProfile established(DataSource.Format format) {
        if (format == DataSource.Format.STOCKFISH_BINPACK_ZSTD)
            throw new IllegalArgumentException("Stockfish BINP/Zstd requires an explicit label profile (BT4_Q_V1)");
        return STOCKFISH_CP_MATE_V1;
    }
}
