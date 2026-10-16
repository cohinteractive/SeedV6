package com.ohinteractive.seedv6.core;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvalTest {

    @Test
    void phaseHasExplicitOpeningMiddleEndAndPromotedMaterialBoundaries() {
        assertEquals(0, Eval.phase(2, 4, 4, 4));
        assertEquals(12, Eval.phase(1, 2, 2, 2));
        assertEquals(24, Eval.phase(0, 0, 0, 0));
        assertEquals(0, Eval.phase(18, 20, 20, 20));

        assertEquals(0, Eval.breakdown(Board.startingPosition()).phase());
        assertEquals(24, Eval.breakdown(board("4k3/8/8/8/8/8/8/4K3 w - - 0 1")).phase());

        long[] promoted = board(
                "rrrrkrrr/rrr5/8/8/8/8/RRR5/RRRRKRRR w - - 0 1");
        assertEquals(0, Eval.breakdown(promoted).phase());
        assertEquals(0, Eval.evaluate(promoted));
    }

    @Test
    void scoreIsSideToMoveRelativeAndColourMirrorInvariant() {
        long[] white = board("4k3/8/8/8/3Q4/8/8/4K3 w - - 0 1");
        long[] black = board("4k3/8/8/8/3Q4/8/8/4K3 b - - 0 1");
        assertEquals(1_154, Eval.evaluate(white));
        assertEquals(-1_154, Eval.evaluate(black));

        long[] original = board(
                "r3k2r/pppq1ppp/2npbn2/3Np3/2B1P3/2N2Q1P/PPP2PP1/R3K2R w KQkq - 4 12");
        long[] colourMirror = board(
                "r3k2r/ppp2pp1/2n2q1p/2b1p3/3nP3/2NPBN2/PPPQ1PPP/R3K2R b KQkq - 4 12");
        assertEquals(-69, Eval.evaluate(original));
        assertEquals(Eval.evaluate(original), Eval.evaluate(colourMirror));
    }

    @Test
    void productionAndBreakdownAreIdenticalAndBoardIsNeverMutated() {
        for (String fen : DONOR_EQUIVALENT_CORPUS) {
            long[] position = board(fen);
            long[] before = position.clone();
            int production = Eval.evaluate(position);
            Eval.Breakdown breakdown = Eval.breakdown(position);

            assertEquals(production, breakdown.total(), fen);
            assertEquals(production, Eval.eval(position[0], position[1], position[2],
                    position[3], Math.toIntExact(position[Board.STATUS]), position[Board.KEY]), fen);
            assertArrayEquals(before, position, fen);
        }
    }

    @Test
    void ruleDrawStateIsOutsideStaticEvaluation() {
        long[] atZero = board("4k3/8/8/8/3Q4/8/8/4K3 b - - 0 1");
        long[] atHundred = board("4k3/8/8/8/3Q4/8/8/4K3 b - - 100 1");

        assertEquals(-1_154, Eval.evaluate(atZero));
        assertEquals(Eval.evaluate(atZero), Eval.evaluate(atHundred));
        assertEquals(0, Eval.evaluate(board("4k3/8/8/8/8/8/8/4K3 w - - 100 1")));
    }

    @Test
    void materialLadderBishopPairAndCorrectedKnightPawnTermAreVisible() {
        Eval.SideBreakdown queen = Eval.breakdown(
                board("4k3/8/8/8/3Q4/8/8/4K3 w - - 0 1")).white();
        Eval.SideBreakdown rook = Eval.breakdown(
                board("4k3/8/8/8/3R4/8/8/4K3 w - - 0 1")).white();
        Eval.SideBreakdown bishop = Eval.breakdown(
                board("4k3/8/8/8/3B4/8/8/4K3 w - - 0 1")).white();
        Eval.SideBreakdown knight = Eval.breakdown(
                board("4k3/8/8/8/3N4/8/8/4K3 w - - 0 1")).white();
        Eval.SideBreakdown pawn = Eval.breakdown(
                board("4k3/8/3p4/8/2P5/8/4K3/8 w - - 0 1")).white();

        assertTrue(queen.material() > rook.material());
        assertTrue(rook.material() > bishop.material());
        assertTrue(bishop.material() > pawn.material());
        assertTrue(knight.material() > pawn.material());

        Eval.SideBreakdown pair = Eval.breakdown(
                board("4k3/8/8/8/2B2B2/8/8/4K3 w - - 0 1")).white();
        assertTrue(pair.minorStructure() > bishop.minorStructure());

        long[] correctedKnightPawn = board(
                "4k3/8/8/8/3N4/2P5/8/4K3 w - - 0 1");
        assertEquals(650, Eval.evaluate(correctedKnightPawn));
        assertEquals(-16, Eval.breakdown(correctedKnightPawn).white().minorStructure());
    }

    @Test
    void promotedAttackSafetySaturatesWithoutEightBitWrap() {
        Eval.Breakdown promotedQueens = Eval.breakdown(board(
                "6k1/QQQQQQQQ/Q7/8/8/8/8/4K3 w - - 0 1"));

        assertEquals(0, promotedQueens.phase());
        assertEquals(-500, promotedQueens.black().kingSafety());
        assertTrue(Math.abs(promotedQueens.total()) <= Eval.MAX_STATIC_SCORE);
    }

    @Test
    void pawnMasksCoverDoubledWeakIsolatedPassedPhalanxAndEdgeFiles() {
        Eval.SideBreakdown isolated = Eval.breakdown(
                board("4k3/8/8/8/2P5/8/4K3/8 w - - 0 1")).white();
        Eval.SideBreakdown doubled = Eval.breakdown(
                board("4k3/8/8/8/2P5/2P5/4K3/8 w - - 0 1")).white();
        Eval.SideBreakdown phalanx = Eval.breakdown(
                board("4k3/8/8/8/2PP4/8/4K3/8 w - - 0 1")).white();
        Eval.SideBreakdown blocked = Eval.breakdown(
                board("4k3/8/8/2p5/2P5/8/4K3/8 w - - 0 1")).white();

        assertTrue(isolated.pawnStructure() < 0);
        assertTrue(doubled.pawnStructure() < isolated.pawnStructure());
        assertEquals(0, phalanx.pawnStructure());
        assertTrue(isolated.passedPawns() > 0);
        assertEquals(0, blocked.passedPawns());
        assertTrue(phalanx.passedPawns() > isolated.passedPawns());

        int aFile = Eval.evaluate(board("4k3/8/8/8/P7/8/4K3/8 w - - 0 1"));
        int hFile = Eval.evaluate(board("3k4/8/8/8/7P/8/3K4/8 w - - 0 1"));
        assertEquals(aFile, hFile);
    }

    @Test
    void correctedPassedPawnRaceIsColourSymmetric() {
        long[] whiteRunner = board("4k3/8/8/8/P7/8/4K3/8 w - - 0 1");
        long[] blackRunnerMirror = board("8/4k3/8/p7/8/8/8/4K3 b - - 0 1");

        assertEquals(Eval.evaluate(whiteRunner), Eval.evaluate(blackRunnerMirror));
        assertTrue(Eval.breakdown(whiteRunner).white().passedPawns() > 0);
    }

    @Test
    void kingShelterPawnStormAndKingSafetyAreVisible() {
        Eval.SideBreakdown sheltered = Eval.breakdown(board(
                "6k1/8/8/8/8/8/5PPP/6K1 w - - 0 1")).white();
        Eval.SideBreakdown exposed = Eval.breakdown(board(
                "6k1/8/8/8/8/8/8/6K1 w - - 0 1")).white();
        Eval.SideBreakdown stormed = Eval.breakdown(board(
                "6k1/8/8/8/5ppp/8/5PPP/6K1 w - - 0 1")).white();

        assertTrue(sheltered.kingShelter() > exposed.kingShelter());
        assertTrue(stormed.kingShelter() < sheltered.kingShelter());

        Eval.Breakdown attacked = Eval.breakdown(
                board("4k3/8/8/8/3Q4/8/8/4K3 w - - 0 1"));
        assertTrue(attacked.black().kingSafety() < 0);
        assertEquals(0, attacked.white().kingSafety());
    }

    @Test
    void mobilityRookFilesDevelopmentOutpostsProtectorsAndBishopComplexAreVisible() {
        Eval.SideBreakdown openRook = Eval.breakdown(board(
                "4k3/7p/8/8/8/3R4/P7/4K3 w - - 0 1")).white();
        Eval.SideBreakdown semiOpenRook = Eval.breakdown(board(
                "4k3/3p4/8/8/8/3R4/P7/4K3 w - - 0 1")).white();
        Eval.SideBreakdown closedRook = Eval.breakdown(board(
                "4k3/3p4/8/8/8/3R4/3P4/4K3 w - - 0 1")).white();
        assertTrue(openRook.rookStructure() > semiOpenRook.rookStructure());
        assertTrue(semiOpenRook.rookStructure() > closedRook.rookStructure());

        Eval.SideBreakdown queenFile = Eval.breakdown(board(
                "4k3/3q4/8/8/8/3R4/P7/4K3 w - - 0 1")).white();
        assertTrue(queenFile.rookStructure() > openRook.rookStructure());

        Eval.SideBreakdown outpost = Eval.breakdown(board(
                "4k3/8/8/4N3/3P4/8/8/4K3 w - - 0 1")).white();
        Eval.SideBreakdown unsupported = Eval.breakdown(board(
                "4k3/8/8/4N3/8/3P4/8/4K3 w - - 0 1")).white();
        assertTrue(outpost.minorStructure() > unsupported.minorStructure());
        assertNotEquals(0, outpost.distance());

        Eval.SideBreakdown sameComplex = Eval.breakdown(board(
                "4k3/8/8/3B4/2P1P3/8/8/4K3 w - - 0 1")).white();
        Eval.SideBreakdown mixedComplex = Eval.breakdown(board(
                "4k3/8/8/3B4/2PP4/8/8/4K3 w - - 0 1")).white();
        assertNotEquals(sameComplex.minorStructure(), mixedComplex.minorStructure());

        Eval.SideBreakdown developedQueen = Eval.breakdown(board(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPQPPPP/RNB1KBNR w KQkq - 0 1")).white();
        assertNotEquals(0, developedQueen.development());
    }

    @Test
    void variedLegalPositionsAreDeterministicImmutableAndInRange() {
        Random random = new Random(0x5eed_0005L);
        long[] moves = new long[256];
        long[] scratch = new long[256];

        for (int game = 0; game < 24; game++) {
            long[] position = Board.startingPosition();
            for (int ply = 0; ply < 100; ply++) {
                long[] before = position.clone();
                int first = Eval.evaluate(position);
                int second = Eval.evaluate(position);
                Eval.Breakdown breakdown = Eval.breakdown(position);
                assertEquals(first, second);
                assertEquals(first, breakdown.total());
                assertTrue(Math.abs(first) <= Eval.MAX_STATIC_SCORE);
                assertArrayEquals(before, position);

                int count = Gen.genAll(position[0], position[1], position[2], position[3],
                        Math.toIntExact(position[Board.STATUS]), position[Board.KEY], true,
                        moves, scratch);
                if (count == 0) {
                    break;
                }
                long[] next = new long[Board.MAX_BITBOARDS];
                Board.makeMoveInto(position[0], position[1], position[2], position[3],
                        Math.toIntExact(position[Board.STATUS]), position[Board.KEY],
                        moves[random.nextInt(count)], next);
                position = next;
            }
        }
    }

    @Test
    void invalidKingPresenceFailsExplicitly() {
        assertThrows(IllegalArgumentException.class,
                () -> Eval.evaluate(board("8/8/8/8/8/8/8/4K3 w - - 0 1")));
        assertThrows(IllegalArgumentException.class,
                () -> Eval.evaluate(board("4k3/8/8/8/8/8/8/8 w - - 0 1")));
    }

    private static long[] board(String fen) {
        return Board.fromFen(fen);
    }

    static final String[] DONOR_EQUIVALENT_CORPUS = {
        "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
        "r3k2r/pppq1ppp/2npbn2/3Np3/2B1P3/2N2Q1P/PPP2PP1/R3K2R w KQkq - 4 12",
        "2kr3r/ppp2ppp/2n1bn2/2bpp3/4P3/2NP1N2/PPP1BPPP/R2R2K1 b - - 2 14",
        "4k3/8/8/8/3Q4/8/8/4K3 w - - 0 1",
        "4k3/8/8/8/3R4/8/8/4K3 w - - 0 1",
        "4k3/8/8/8/2B2B2/8/8/4K3 w - - 0 1",
        "4k3/8/8/8/3N4/2P5/8/4K3 w - - 0 1",
        "4k3/8/8/3p4/2P5/8/4K3/8 w - - 0 1",
        "4k3/8/8/8/2P5/2P5/4K3/8 w - - 0 1",
        "4k3/8/8/8/2PP4/8/4K3/8 w - - 0 1",
        "4k3/8/3p4/8/2P5/8/4K3/8 w - - 0 1",
        "4k3/8/8/8/8/8/3P4/R3K3 w - - 0 1",
        "4k3/3q4/8/8/8/8/3R4/4K3 w - - 0 1",
        "4k3/8/5p2/4N3/3P4/8/8/4K3 w - - 0 1",
        "4k3/8/8/3B4/2P1P3/8/8/4K3 w - - 0 1",
        "4k3/8/8/3B4/2PP4/8/8/4K3 w - - 0 1"
    };
}
