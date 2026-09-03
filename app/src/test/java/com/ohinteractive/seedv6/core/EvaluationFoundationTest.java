package com.ohinteractive.seedv6.core;

import com.ohinteractive.seedv6.tools.eval.EvaluationCorpus;
import com.ohinteractive.seedv6.tools.eval.EvaluatorBenchmark;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvaluationFoundationTest {

    private static final String BASELINE =
            "9981efef51dd5d7be3ea82d97a49add2653d4caf";
    private static final String CORPUS_SHA256 =
            "dad39174f075e77941e6e4e5d579e0c46a294b6652e22d15d614b9bd0a07f697";

    @Test
    void corpusIdentityMetadataAndSamplingCompositionAreStable() {
        EvaluationCorpus.Metadata metadata = EvaluationCorpus.metadata();

        assertEquals(BASELINE, EvaluationCorpus.BASELINE_COMMIT);
        assertEquals(BASELINE, FrozenEvalReference.BASELINE_COMMIT);
        assertEquals(CORPUS_SHA256, EvaluationCorpus.rawSha256());
        assertEquals(256, EvaluationCorpus.SAMPLE_STRIDE);
        assertEquals(8, EvaluationCorpus.SUPPORT_SAMPLES_PER_STREAM);
        assertEquals(3_182, metadata.sampleCount());
        assertEquals(3_020, metadata.weightedSampleCount());
        assertEquals(773_261L, metadata.totalCalls());
        assertEquals(81_411L,
                metadata.calls(EvaluationCorpus.Source.MAIN_SEARCH));
        assertEquals(691_850L,
                metadata.calls(EvaluationCorpus.Source.QSEARCH));
        assertEquals(26_338L, metadata.endgameCalls());
        assertEquals(19_102L, metadata.promotedMaterialCalls());

        assertEquals(List.of(
                "opening-start", "middlegame-kiwipete", "quiet-endgame",
                "tactical-queen", "quiet-pawn", "check-evasion",
                "transposition-knights", "qsearch-exchanges", "promotion-race",
                "en-passant", "checkmate-terminal", "stalemate-terminal"),
                metadata.rootNames());
        long[][] expectedCalls = {
                {10_823, 43_069}, {60_815, 626_819}, {262, 601}, {196, 339},
                {480, 858}, {18, 14}, {3_456, 6_767}, {3_820, 9_231},
                {995, 3_190}, {544, 962}, {1, 0}, {1, 0}
        };
        long[][] actualCalls = metadata.callCounts();
        assertEquals(expectedCalls.length, actualCalls.length);
        for (int root = 0; root < expectedCalls.length; root++) {
            assertArrayEquals(expectedCalls[root], actualCalls[root]);
        }

        assertArrayEquals(new long[] {
                68_639, 74_938, 132_702, 97_306, 41_074,
                64_015, 108_931, 60_181, 34_590, 18_346,
                13_819, 9_157, 6_648, 4_991, 3_075,
                1_405, 7_106, 422, 681, 506,
                8_869, 0, 10_343, 321, 5_196
        }, metadata.phaseHistogram());
        assertArrayEquals(new long[] {
                20_932, 8_998, 1_497, 3_662, 14_338,
                37_118, 120_274, 299_440, 267_002
        }, metadata.whitePawnHistogram());
        assertArrayEquals(new long[] {
                15_354, 5_512, 13_833, 13_475, 36_311,
                78_535, 170_170, 272_033, 168_038
        }, metadata.blackPawnHistogram());

        int support = 0;
        int weighted = 0;
        int main = 0;
        int qsearch = 0;
        int pawnHeavy = 0;
        int sliderHeavy = 0;
        int promoted = 0;
        int endgame = 0;
        for (EvaluationCorpus.Entry entry : EvaluationCorpus.entries()) {
            if (entry.kind() == EvaluationCorpus.SampleKind.SUPPORT) support++;
            else weighted++;
            if (entry.source() == EvaluationCorpus.Source.MAIN_SEARCH) main++;
            else qsearch++;
            if (entry.whitePawnCount() + entry.blackPawnCount() >= 12) pawnHeavy++;
            if (entry.sliderCount() >= 6) sliderHeavy++;
            if (entry.promotedMaterial()) promoted++;
            if (entry.endgame()) endgame++;
        }
        assertEquals(162, support);
        assertEquals(3_020, weighted);
        assertEquals(401, main);
        assertEquals(2_781, qsearch);
        assertEquals(2_503, pawnHeavy);
        assertEquals(2_870, sliderHeavy);
        assertEquals(71, promoted);
        assertEquals(234, endgame);
    }

    @Test
    void corpusBoardsAreValidImmutableAndExactlyMatchFrozenReference() {
        for (EvaluationCorpus.Entry entry : EvaluationCorpus.entries()) {
            long[] board = entry.board();
            long[] before = board.clone();
            long occupancy = board[0] | board[1] | board[2];
            long kings = board[0] & ~board[1] & ~board[2];

            assertEquals(Board.MAX_BITBOARDS, board.length);
            assertEquals(0L, board[3] & ~occupancy,
                    "Colour bits outside occupancy at ordinal " + entry.ordinal());
            assertEquals(1, Long.bitCount(kings & ~board[3]),
                    "White king count at ordinal " + entry.ordinal());
            assertEquals(1, Long.bitCount(kings & board[3]),
                    "Black king count at ordinal " + entry.ordinal());
            Math.toIntExact(board[Board.STATUS]);

            int production = Eval.evaluate(board);
            int reference = FrozenEvalReference.evaluate(board);
            assertEquals(reference, production,
                    "Evaluator mismatch at root " + entry.rootIndex()
                            + " ordinal " + entry.ordinal());
            assertEquals(production, Eval.evaluate(board));
            assertEquals(reference, FrozenEvalReference.evaluate(board));
            assertTrue(Math.abs(production) <= Eval.MAX_STATIC_SCORE);
            assertArrayEquals(before, board,
                    "Evaluation mutated root " + entry.rootIndex()
                            + " ordinal " + entry.ordinal());
        }
    }

    @Test
    void frozenReferenceMatchesRepresentativeFixturesAndLegalPositions() {
        for (String fen : EvalTest.DONOR_EQUIVALENT_CORPUS) {
            assertReferenceEquality(Board.fromFen(fen), fen);
        }
        String[] specialFixtures = {
                "r3k2r/ppp2pp1/2n2q1p/2b1p3/3nP3/2NPBN2/PPPQ1PPP/R3K2R b KQkq - 4 12",
                "4k3/8/8/8/3Q4/8/8/4K3 b - - 100 1",
                "rrrrkrrr/rrr5/8/8/8/8/RRR5/RRRRKRRR w - - 0 1",
                "6k1/QQQQQQQQ/Q7/8/8/8/8/4K3 w - - 0 1",
                "6k1/8/8/8/5ppp/8/5PPP/6K1 w - - 0 1",
                "4k3/3p4/8/8/8/3R4/P7/4K3 w - - 0 1",
                "8/4k3/8/p7/8/8/8/4K3 b - - 0 1"
        };
        for (String fen : specialFixtures) {
            assertReferenceEquality(Board.fromFen(fen), fen);
        }

        Random random = new Random(0x5eed_5001L);
        long[] moves = new long[256];
        long[] scratch = new long[256];
        for (int game = 0; game < 12; game++) {
            long[] position = Board.startingPosition();
            for (int ply = 0; ply < 80; ply++) {
                assertReferenceEquality(position, "game=" + game + " ply=" + ply);
                int count = Gen.genAll(position[0], position[1], position[2], position[3],
                        Math.toIntExact(position[Board.STATUS]), position[Board.KEY], true,
                        moves, scratch);
                if (count == 0) break;
                long[] next = new long[Board.MAX_BITBOARDS];
                Board.makeMoveInto(position[0], position[1], position[2], position[3],
                        Math.toIntExact(position[Board.STATUS]), position[Board.KEY],
                        moves[random.nextInt(count)], next);
                position = next;
            }
        }
    }

    @Test
    void frozenTuningDataHasIndependentExactIntegrityContract() {
        Eval.TuningSummary production = Eval.tuningSummary();
        FrozenEvalReference.TuningSummary frozen = FrozenEvalReference.tuningSummary();

        assertEquals(125, frozen.materialEntries());
        assertEquals(19_200, frozen.pieceSquareEntries());
        assertEquals(7_861, frozen.criteriaEntries());
        assertEquals("1d647057aa7d3b6953f45aa07de337536eb3239a0bba526b3a8aeb2c31e85373",
                frozen.materialSha256());
        assertEquals("71789a6d196416672e4dc50acab3236006ece49d124fb90fc065f0bf0cd17f9b",
                frozen.pieceSquareSha256());
        assertEquals("fd780b7b1598722f454aee543ebf02bb8900c1dcb81d3741f1824eba4c4bbbfe",
                frozen.criteriaSha256());

        assertEquals(frozen.materialEntries(), production.materialEntries());
        assertEquals(frozen.pieceSquareEntries(), production.pieceSquareEntries());
        assertEquals(frozen.criteriaEntries(), production.criteriaEntries());
        assertEquals(frozen.materialSha256(), production.materialSha256());
        assertEquals(frozen.pieceSquareSha256(), production.pieceSquareSha256());
        assertEquals(frozen.criteriaSha256(), production.criteriaSha256());
    }

    @Test
    void corpusReconstructionAndBenchmarkChecksumAreDeterministic() {
        List<EvaluationCorpus.Entry> entries = EvaluationCorpus.entries();
        assertSame(entries, EvaluationCorpus.entries());
        assertThrows(UnsupportedOperationException.class, () -> entries.add(entries.get(0)));
        for (EvaluationCorpus.Entry entry : entries) {
            assertArrayEquals(entry.board(), entry.board());
        }

        long[][] boards = EvaluatorBenchmark.productionWeightedBoards();
        long[][] before = Arrays.stream(boards).map(long[]::clone).toArray(long[][]::new);
        long first = EvaluatorBenchmark.checksum(boards, 10_000);
        long second = EvaluatorBenchmark.checksum(boards, 10_000);
        assertEquals(0x57649cb252d7d14fL, first);
        assertEquals(first, second);
        for (int index = 0; index < boards.length; index++) {
            assertArrayEquals(before[index], boards[index]);
        }
    }

    private static void assertReferenceEquality(long[] board, String context) {
        long[] before = board.clone();
        assertEquals(FrozenEvalReference.evaluate(board), Eval.evaluate(board), context);
        assertArrayEquals(before, board, context);
    }
}
