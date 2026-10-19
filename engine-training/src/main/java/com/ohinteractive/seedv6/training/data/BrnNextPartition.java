package com.ohinteractive.seedv6.training.data;

import java.nio.charset.StandardCharsets;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.corpus.CorpusPosition;

/** Stable, label-independent split for new BRN successor lineages only.
 * Placement aliases (including STM/rule-state, color/rank and file reflection)
 * never cross partitions, even across generations, sources or different candidates.
 * No game-disjoint or distribution-independent claim is implied. */
public final class BrnNextPartition {
    public static final String ID = "brn-next-geometry-sha256-8train-1validation-1sealed-v1";
    public enum Part { TRAIN, VALIDATION, SEALED }
    public static String groupKey(CorpusPosition board) {
        String best = null;
        for (int color = 0; color < 2; color++) for (int mirror = 0; mirror < 2; mirror++) {
            char[] key = new char[64];
            for (int square = 0; square < 64; square++) {
                int code = Board.getSquare(board.plane0(), board.plane1(), board.plane2(), board.plane3(), square);
                key[square ^ (color * 56) ^ (mirror * 7)] = (char) ('A' + ((code & 7) == 0 ? 0 : code ^ (color * 8)));
            }
            String value = new String(key); if (best == null || value.compareTo(best) < 0) best = value;
        }
        return best;
    }
    public static Part part(CorpusPosition board) {
        byte[] hash = DataFiles.digest().digest((ID + ":" + groupKey(board)).getBytes(StandardCharsets.US_ASCII));
        int bucket = (((hash[0] & 255) << 8) | (hash[1] & 255)) % 10;
        return bucket == 0 ? Part.SEALED : bucket == 1 ? Part.VALIDATION : Part.TRAIN;
    }
    private BrnNextPartition() {}
}
