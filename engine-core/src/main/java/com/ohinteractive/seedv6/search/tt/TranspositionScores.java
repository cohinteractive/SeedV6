package com.ohinteractive.seedv6.search.tt;

/**
 * Root-ply-independent transposition-score conversion for SeedV6 search.
 *
 * <p>SeedV6 represents checkmate as {@code +/-32768} adjusted by the terminal
 * root ply. A positive mate score is moved closer to {@link #MATE_SCORE} when
 * stored and restored away from it when probed; negative mate scores use the
 * opposite arithmetic. Ordinary position scores in the inclusive range
 * {@code [-MAX_NORMAL_SCORE, MAX_NORMAL_SCORE]} are unchanged; the adjacent
 * mate bands begin at {@code +/-MATE_THRESHOLD}. Values are validated rather
 * than clamped.</p>
 */
public final class TranspositionScores {

    public static final int MATE_SCORE = 32768;
    public static final int MAX_MATE_PLY = 256;
    public static final int MATE_THRESHOLD = MATE_SCORE - MAX_MATE_PLY;
    public static final int MAX_NORMAL_SCORE = MATE_THRESHOLD - 1;

    public static int toTableScore(int score, int ply) {
        requireScore(score);
        requirePly(ply);
        final long converted;
        if(score >= MATE_THRESHOLD) {
            converted = (long) score + ply;
        } else if(score <= -MATE_THRESHOLD) {
            converted = (long) score - ply;
        } else {
            return score;
        }
        if(converted < -MATE_SCORE || converted > MATE_SCORE) {
            throw new IllegalArgumentException(
                "Mate score and ply do not form a valid SeedV6 search score: score="
                    + score + ", ply=" + ply
            );
        }
        return (int) converted;
    }

    public static int fromTableScore(int storedScore, int ply) {
        requireScore(storedScore);
        requirePly(ply);
        if(storedScore >= MATE_THRESHOLD) return storedScore - ply;
        if(storedScore <= -MATE_THRESHOLD) return storedScore + ply;
        return storedScore;
    }

    public static boolean isMateScore(int score) {
        requireScore(score);
        return score >= MATE_THRESHOLD || score <= -MATE_THRESHOLD;
    }

    static void requirePly(int ply) {
        if(ply < 0 || ply > MAX_MATE_PLY) {
            throw new IllegalArgumentException(
                "Ply must be between 0 and " + MAX_MATE_PLY + ": " + ply
            );
        }
    }

    private static void requireScore(int score) {
        if(score < -MATE_SCORE || score > MATE_SCORE) {
            throw new IllegalArgumentException(
                "Score must be between -" + MATE_SCORE + " and " + MATE_SCORE + ": " + score
            );
        }
    }

    private TranspositionScores() {}
}
