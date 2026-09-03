package com.ohinteractive.seedv6.core;

import com.ohinteractive.seedv6.core.util.Piece;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MaterialFactReuseTest {

    @Test
    void currentPhaseFactsExactlyMatchOldExpressionsAcrossLegalPromotionRanges() {
        EvalTuning tuning = EvalTuning.INSTANCE;
        long cases = 0L;
        long promotedCases = 0L;

        for (int phase = 0; phase <= EvalTuning.MAX_PHASE; phase++) {
            int queenUnit = tuning.material(Piece.QUEEN, phase);
            int rookUnit = tuning.material(Piece.ROOK, phase);
            int bishopUnit = tuning.material(Piece.BISHOP, phase);
            int knightUnit = tuning.material(Piece.KNIGHT, phase);
            int pawnUnit = tuning.material(Piece.PAWN, phase);

            for (int pawnCount = 0; pawnCount <= 8; pawnCount++) {
                for (int queenCount = 0; queenCount <= 9; queenCount++) {
                    for (int rookCount = 0; rookCount <= 10; rookCount++) {
                        for (int bishopCount = 0; bishopCount <= 10; bishopCount++) {
                            for (int knightCount = 0; knightCount <= 10; knightCount++) {
                                int promotions = Math.max(0, queenCount - 1)
                                        + Math.max(0, rookCount - 2)
                                        + Math.max(0, bishopCount - 2)
                                        + Math.max(0, knightCount - 2);
                                if (promotions > 8 - pawnCount) {
                                    continue;
                                }

                                int queenComponent = queenCount * queenUnit;
                                int rookComponent = rookCount * rookUnit;
                                int bishopComponent = bishopCount * bishopUnit;
                                int knightComponent = knightCount * knightUnit;
                                int pawnComponent = pawnCount * pawnUnit;
                                int nonPawnMaterial = queenComponent + rookComponent
                                        + bishopComponent + knightComponent;

                                requireEqual(queenCount * tuning.material(Piece.QUEEN, phase),
                                        queenComponent, "queen", phase, pawnCount, queenCount,
                                        rookCount, bishopCount, knightCount);
                                requireEqual(rookCount * tuning.material(Piece.ROOK, phase),
                                        rookComponent, "rook", phase, pawnCount, queenCount,
                                        rookCount, bishopCount, knightCount);
                                requireEqual(bishopCount * tuning.material(Piece.BISHOP, phase),
                                        bishopComponent, "bishop", phase, pawnCount, queenCount,
                                        rookCount, bishopCount, knightCount);
                                requireEqual(knightCount * tuning.material(Piece.KNIGHT, phase),
                                        knightComponent, "knight", phase, pawnCount, queenCount,
                                        rookCount, bishopCount, knightCount);
                                requireEqual(pawnCount * tuning.material(Piece.PAWN, phase),
                                        pawnComponent, "pawn", phase, pawnCount, queenCount,
                                        rookCount, bishopCount, knightCount);
                                requireEqual(
                                        queenCount * tuning.material(Piece.QUEEN, phase)
                                                + rookCount * tuning.material(Piece.ROOK, phase)
                                                + bishopCount * tuning.material(Piece.BISHOP, phase)
                                                + knightCount * tuning.material(Piece.KNIGHT, phase),
                                        nonPawnMaterial, "non-pawn-total", phase, pawnCount,
                                        queenCount, rookCount, bishopCount, knightCount);

                                cases++;
                                if (promotions != 0) {
                                    promotedCases++;
                                }
                            }
                        }
                    }
                }
            }
        }

        assertEquals(217_350L, cases);
        assertEquals(205_200L, promotedCases);
    }

    private static void requireEqual(int expected, int actual, String component,
                                     int phase, int pawnCount, int queenCount,
                                     int rookCount, int bishopCount, int knightCount) {
        if (expected != actual) {
            throw new AssertionError(component + " phase=" + phase
                    + " pawns=" + pawnCount + " queens=" + queenCount
                    + " rooks=" + rookCount + " bishops=" + bishopCount
                    + " knights=" + knightCount + " expected=" + expected
                    + " actual=" + actual);
        }
    }
}
