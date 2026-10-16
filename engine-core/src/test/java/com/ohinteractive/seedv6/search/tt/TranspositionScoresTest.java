package com.ohinteractive.seedv6.search.tt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranspositionScoresTest {

    @Test
    void winningAndLosingMatesRoundTripAtSameAndDifferentPlies() {
        final int winning = TranspositionScores.MATE_SCORE - 8;
        final int storedWinning = TranspositionScores.toTableScore(winning, 3);
        assertEquals(TranspositionScores.MATE_SCORE - 5, storedWinning);
        assertEquals(winning, TranspositionScores.fromTableScore(storedWinning, 3));
        assertEquals(winning - 3, TranspositionScores.fromTableScore(storedWinning, 6));

        final int losing = -TranspositionScores.MATE_SCORE + 8;
        final int storedLosing = TranspositionScores.toTableScore(losing, 3);
        assertEquals(-TranspositionScores.MATE_SCORE + 5, storedLosing);
        assertEquals(losing, TranspositionScores.fromTableScore(storedLosing, 3));
        assertEquals(losing + 3, TranspositionScores.fromTableScore(storedLosing, 6));
    }

    @Test
    void normalizedMatesRetainShorterAndLongerOrderingAcrossRootPlies() {
        final int shortWinStored = TranspositionScores.toTableScore(
            TranspositionScores.MATE_SCORE - 9, 7
        );
        final int longWinStored = TranspositionScores.toTableScore(
            TranspositionScores.MATE_SCORE - 10, 2
        );
        assertTrue(
            TranspositionScores.fromTableScore(shortWinStored, 4)
                > TranspositionScores.fromTableScore(longWinStored, 4)
        );

        final int shortLossStored = TranspositionScores.toTableScore(
            -TranspositionScores.MATE_SCORE + 9, 7
        );
        final int longLossStored = TranspositionScores.toTableScore(
            -TranspositionScores.MATE_SCORE + 10, 2
        );
        assertTrue(
            TranspositionScores.fromTableScore(shortLossStored, 4)
                < TranspositionScores.fromTableScore(longLossStored, 4)
        );
    }

    @Test
    void mateBandBoundaryIsExplicitAndNearMateOrdinaryScoresAreUnchanged() {
        final int threshold = TranspositionScores.MATE_THRESHOLD;
        assertTrue(TranspositionScores.isMateScore(threshold));
        assertTrue(TranspositionScores.isMateScore(-threshold));
        assertFalse(TranspositionScores.isMateScore(threshold - 1));
        assertFalse(TranspositionScores.isMateScore(-threshold + 1));

        assertEquals(threshold + 1, TranspositionScores.toTableScore(threshold, 1));
        assertEquals(-threshold - 1, TranspositionScores.toTableScore(-threshold, 1));
        assertEquals(threshold - 1, TranspositionScores.toTableScore(threshold - 1, 37));
        assertEquals(-threshold + 1, TranspositionScores.toTableScore(-threshold + 1, 37));
    }

    @Test
    void deterministicCrossPlyCorpusIsRoundTripStable() {
        final int[] ordinary = {
            -TranspositionScores.MAX_NORMAL_SCORE,
            -1,
            0,
            1,
            TranspositionScores.MAX_NORMAL_SCORE
        };
        for(int ply = 0; ply <= 64; ply ++) {
            for(int score : ordinary) {
                assertEquals(
                    score,
                    TranspositionScores.fromTableScore(
                        TranspositionScores.toTableScore(score, ply), ply
                    )
                );
            }
            for(int terminalPly = ply; terminalPly <= TranspositionScores.MAX_MATE_PLY;
                terminalPly += 17) {
                final int winning = TranspositionScores.MATE_SCORE - terminalPly;
                final int losing = -TranspositionScores.MATE_SCORE + terminalPly;
                assertEquals(
                    winning,
                    TranspositionScores.fromTableScore(
                        TranspositionScores.toTableScore(winning, ply), ply
                    )
                );
                assertEquals(
                    losing,
                    TranspositionScores.fromTableScore(
                        TranspositionScores.toTableScore(losing, ply), ply
                    )
                );
            }
        }
    }

    @Test
    void invalidScoresPliesAndImpossibleMatePairsAreRejectedWithoutClamping() {
        assertThrows(
            IllegalArgumentException.class,
            () -> TranspositionScores.toTableScore(TranspositionScores.MATE_SCORE + 1, 0)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> TranspositionScores.fromTableScore(-TranspositionScores.MATE_SCORE - 1, 0)
        );
        assertThrows(IllegalArgumentException.class, () -> TranspositionScores.toTableScore(0, -1));
        assertThrows(
            IllegalArgumentException.class,
            () -> TranspositionScores.fromTableScore(0, TranspositionScores.MAX_MATE_PLY + 1)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> TranspositionScores.toTableScore(TranspositionScores.MATE_SCORE, 1)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> TranspositionScores.toTableScore(-TranspositionScores.MATE_SCORE, 1)
        );
    }
}
