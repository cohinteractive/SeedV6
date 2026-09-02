package com.ohinteractive.seedv6.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Stable results from an out-of-tree execution of SeedV3 HEAD 0432147 with only
 * its pre-existing KNIGHT_PAWN load correction applied. Donor draw/cache results
 * and the corrected V6 passed-pawn race are classified separately below.
 */
class EvalDonorComparisonTest {

    private static final int[] CORRECTED_DONOR_SCORES = {
        0, -69, 402, 1_154, 737, 824, 650, -2,
        714, 930, 16, 916, -385, 392, 1_182, 1_277
    };

    @Test
    void auditedProductionSemanticsMatchCorrectedDonorCorpusExactly() {
        assertEquals(CORRECTED_DONOR_SCORES.length,
                EvalTest.DONOR_EQUIVALENT_CORPUS.length);
        for (int index = 0; index < CORRECTED_DONOR_SCORES.length; index++) {
            String fen = EvalTest.DONOR_EQUIVALENT_CORPUS[index];
            assertEquals(CORRECTED_DONOR_SCORES[index],
                    Eval.evaluate(Board.fromFen(fen)), fen);
        }
    }

    @Test
    void committedKnightPawnDefectIsNotPreserved() {
        String fen = "4k3/8/8/8/3N4/2P5/8/4K3 w - - 0 1";

        // Corrected donor working tree and V6: 650. SeedV3 HEAD's erroneous
        // ROOK_PAWN lookup would add 38 and produce 688.
        assertEquals(650, Eval.evaluate(Board.fromFen(fen)));
    }

    @Test
    void ruleDrawAndCacheCouplingIsAnIntentionalDiscrepancy() {
        String atZero = "4k3/8/8/8/3Q4/8/8/4K3 b - - 0 1";
        String atHundred = "4k3/8/8/8/3Q4/8/8/4K3 b - - 100 1";

        // A fresh donor process returns 0 at halfmove 100. If the halfmove-99
        // position is evaluated first, its position-key cache instead returns
        // -1154 at 100. V6 deliberately returns the position-only static score.
        assertEquals(-1_154, Eval.evaluate(Board.fromFen(atZero)));
        assertEquals(-1_154, Eval.evaluate(Board.fromFen(atHundred)));
    }

    @Test
    void passedPawnRaceAndPromotedPhaseFixesAreIntentionalDiscrepancies() {
        String edgeRunner = "4k3/8/8/8/P6p/8/4K3/8 w - - 0 1";
        // Corrected donor reference: -56. Its colour-asymmetric promotion-distance
        // formula misses this white runner; the symmetric V6 king-catch test is 294.
        assertEquals(294, Eval.evaluate(Board.fromFen(edgeRunner)));

        String twentyPromotedRooks =
                "rrrrkrrr/rrr5/8/8/8/8/RRR5/RRRRKRRR w - - 0 1";
        // Donor PHASE_VALUE[count] cannot index 20 rooks. V6 weights the count
        // directly, clamps to the opening endpoint, and evaluates symmetrically.
        assertEquals(0, Eval.evaluate(Board.fromFen(twentyPromotedRooks)));
        assertEquals(0, Eval.breakdown(Board.fromFen(twentyPromotedRooks)).phase());
    }

    @Test
    void pawnStormSignIsAnIntentionalDiscrepancy() {
        String storm = "6k1/8/8/8/5ppp/8/5PPP/6K1 w - - 0 1";

        // The retained PAWN_STORM values are negative. SeedV3 subtracts them and
        // scores -8 here; V6 applies them as signed penalties and scores -50.
        Eval.Breakdown breakdown = Eval.breakdown(Board.fromFen(storm));
        assertEquals(-50, breakdown.total());
        assertEquals(-6, breakdown.white().kingShelter());
    }
}
