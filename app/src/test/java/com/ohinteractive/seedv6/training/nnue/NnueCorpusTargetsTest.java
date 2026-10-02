package com.ohinteractive.seedv6.training.nnue;

import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.corpus.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import static org.junit.jupiter.api.Assertions.*;

class NnueCorpusTargetsTest {
    static CorpusRecord record(boolean black, int kind, int value, int perspective) {
        return new CorpusRecord(CorpusPosition.fromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR "
                + (black ? "b" : "w") + " KQkq - 0 1"), kind, value, perspective, 20, -1, 0, 1, 1);
    }
    static double target(CorpusRecord r) { return NnueCorpusTargets.target(r, r.position().toBoard(0)); }

    @Test void officialSf171GoldenCoefficientsNormalizationAndPermilleResults() {
        assertEquals("STOCKFISH_WDL_V1", NnueCorpusTargets.ID);
        assertEquals("03e27488f3d21d8ff4dbf3065603afa21dbd0ef3", NnueCorpusTargets.SOURCE_COMMIT);
        assertEquals(78, NnueCorpusTargets.material(Board.startingPosition()));
        // Golden results calculated separately from the immutable official uci.cpp coefficients.
        assertEquals(new NnueCorpusTargets.Wdl(28, 944, 28), NnueCorpusTargets.wdl(0, 78));
        assertEquals(new NnueCorpusTargets.Wdl(145, 850, 5), NnueCorpusTargets.wdl(50, 78));
        assertEquals(new NnueCorpusTargets.Wdl(500, 499, 1), NnueCorpusTargets.wdl(100, 78));
        assertEquals(new NnueCorpusTargets.Wdl(972, 28, 0), NnueCorpusTargets.wdl(200, 78));
        assertEquals(new NnueCorpusTargets.Wdl(500, 500, 0), NnueCorpusTargets.wdl(100, 58));
        assertEquals(new NnueCorpusTargets.Wdl(49, 951, 0), NnueCorpusTargets.wdl(50, 17));
        assertEquals(.499, target(record(false, CorpusRecord.CP, 100, CorpusRecord.WHITE)));
    }
    @Test void perspectiveIsNormalizedExactlyOnceIncludingBlackStmLabels() {
        assertEquals(.499, target(record(false, CorpusRecord.CP, 100, CorpusRecord.WHITE)));
        assertEquals(-.499, target(record(true, CorpusRecord.CP, 100, CorpusRecord.WHITE)));
        assertEquals(.499, target(record(true, CorpusRecord.CP, 100, CorpusRecord.SIDE_TO_MOVE)));
        assertEquals(-.499, target(record(true, CorpusRecord.CP, -100, CorpusRecord.SIDE_TO_MOVE)));
        assertEquals(0, target(record(true, CorpusRecord.CP, 0, CorpusRecord.WHITE)));
    }
    @Test void mateSignAndPerspectiveUseCertainOutcomeWithoutDistanceScaling() {
        for (int distance : new int[]{1, 20, Integer.MAX_VALUE}) {
            assertEquals(1, target(record(false, CorpusRecord.MATE, distance, CorpusRecord.WHITE)));
            assertEquals(-1, target(record(true, CorpusRecord.MATE, distance, CorpusRecord.WHITE)));
            assertEquals(1, target(record(true, CorpusRecord.MATE, distance, CorpusRecord.SIDE_TO_MOVE)));
            assertEquals(-1, target(record(true, CorpusRecord.MATE, -distance, CorpusRecord.SIDE_TO_MOVE)));
        }
        assertEquals(1, target(record(true, CorpusRecord.MATE, Integer.MIN_VALUE, CorpusRecord.WHITE)));
    }
    @Test void boundedOddModelHasExplicitMaterialClampAndSupportsAllIntegerCp() {
        for (int material : new int[]{0, 17, 58, 78, 120}) {
            for (long cp : new long[]{0, 1, 50, 100, 200, 1000, Integer.MAX_VALUE, 2147483648L}) {
                double positive = NnueCorpusTargets.wdl(cp, material).target();
                assertTrue(positive >= 0 && positive <= 1);
                assertEquals(positive == 0 ? 0 : -positive, NnueCorpusTargets.wdl(-cp, material).target());
            }
        }
        assertEquals(NnueCorpusTargets.wdl(50, 17), NnueCorpusTargets.wdl(50, 0));
        assertEquals(NnueCorpusTargets.wdl(50, 78), NnueCorpusTargets.wdl(50, 120));
        assertEquals(1, target(record(true, CorpusRecord.CP, Integer.MIN_VALUE, CorpusRecord.WHITE)));
    }
    @Test void unsupportedPerspectiveAndUnsignedMateZeroAreDeterministicRejections() {
        var raw = record(false, CorpusRecord.CP, 20, CorpusRecord.RAW_SOURCE);
        var zero = record(false, CorpusRecord.MATE, 0, CorpusRecord.WHITE);
        assertEquals("unsupported-perspective", NnueCorpusTargets.rejection(raw));
        assertEquals("mate-zero", NnueCorpusTargets.rejection(zero));
        assertThrows(IllegalArgumentException.class, () -> target(raw));
        assertThrows(IllegalArgumentException.class, () -> target(zero));
    }
    @Test void generatedSampleContractAndTerminalPerspectiveRemainExact() {
        long[] board = Board.startingPosition();
        assertThrows(IllegalArgumentException.class, () -> new TrajectorySampler.Sample(board, .499));
        assertEquals(1, GameResult.WHITE_WIN.target(0));
        assertEquals(-1, GameResult.WHITE_WIN.target(1));
        assertEquals(0, GameResult.DRAW.target(1));
        assertEquals(1, GameResult.BLACK_WIN.target(1));
        var r = record(true, CorpusRecord.CP, 100, CorpusRecord.WHITE);
        byte[] before = r.encode(); target(r); assertArrayEquals(before, r.encode());
    }
}
