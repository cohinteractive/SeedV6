package com.ohinteractive.seedv6.search.driver;

import com.ohinteractive.seedv6.core.Board;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class Wdl25ProductionTest {
    @Test void defaultAndRollbackHaveDistinctVerifiedWeightsAndIdenticalProductionSearch() throws Exception {
        var normal = BaselineEngine.load();
        var rollback = BaselineEngine.load(BaselineEngine.Evaluator.CONVENTIONAL_CP64);
        assertEquals(BaselineEngine.Evaluator.CONVENTIONAL_WDL25, normal.evaluator());
        assertEquals(ConventionalNnueEngine.Model.WDL25, normal.conventional().selection());
        assertEquals(ConventionalNnueEngine.Model.CP64, rollback.conventional().selection());
        assertNotEquals(normal.conventional().model().payloadSha256(), rollback.conventional().model().payloadSha256());
        for (int workers : new int[]{1, 4, 16}) {
            assertEquals(normal.formulation(workers, 8).search(), rollback.formulation(workers, 8).search());
            assertEquals(3329490943L, normal.formulation(workers, 8).search().mechanisms().flags());
            assertTrue(normal.identity(workers, 8).contains("model=WDL25"));
            assertTrue(rollback.identity(workers, 8).contains("model=CP64"));
        }
        // Historical E008 callers still load the exact old asset.
        assertEquals(ConventionalNnueEngine.Model.CP64, ConventionalNnueEngine.load().selection());
    }

    @Test void neitherPinAcceptsMissingCorruptOrOtherModelBytes() throws Exception {
        for (var pin : ConventionalNnueEngine.Model.values()) {
            byte[] bytes;
            try (var input = getClass().getResourceAsStream(pin.resource())) { bytes = input.readAllBytes(); }
            assertEquals(pin.payloadSha256(), ConventionalNnueEngine.readPinned(pin, new ByteArrayInputStream(bytes)).payloadSha256());
            assertThrows(IOException.class, () -> ConventionalNnueEngine.readPinned(pin, null));
            var other = pin == ConventionalNnueEngine.Model.CP64 ? ConventionalNnueEngine.Model.WDL25 : ConventionalNnueEngine.Model.CP64;
            assertThrows(IOException.class, () -> ConventionalNnueEngine.readPinned(other, new ByteArrayInputStream(bytes)));
            bytes[bytes.length - 1] ^= 1;
            assertThrows(IOException.class, () -> ConventionalNnueEngine.readPinned(pin, new ByteArrayInputStream(bytes)));
        }
    }

    @Test void productionMatchesIndependentResearchIntegerGoldenScores() throws Exception {
        // Selected rows from strength-20261114 WDL25 Python arbitrary-precision export.
        // File SHA256 a25e76fcdfe1b854452685bec87bc759bed2ed166312579e024410f0903517ab.
        String[] fens = {
            "8/5r2/7R/8/4P3/5k2/1K6/8 w - - 43 1",
            "8/2k5/8/8/8/5K1Q/1R6/8 b - - 84 1",
            "8/8/6k1/3r4/8/5R2/8/4K3 w - - 21 1",
            "8/8/4k3/6B1/2K3P1/8/8/8 w - - 3 1",
            "8/8/8/p7/P7/2k5/6q1/1K6 b - - 27 1",
            "8/8/P2k4/1n3r2/1P6/4R3/8/7K b - - 11 1",
            "2k5/1p4r1/p1p2n2/P3p1p1/1P6/3P1B2/2PK1P2/R7 w - - 8 1",
            "2r1r1k1/1p3p1p/8/3b2p1/1B4P1/P3PP2/3P1KP1/R4B2 b - - 0 1",
            "r3k2r/2pbqppp/P4n2/8/PbQ1p3/N7/1P1PPPPP/R1B1KB1R b KQkq - 0 1",
            "r2q1rk1/pb1p1pbp/np2pnp1/2p5/2PP4/1P2PN1P/PB2BPP1/RN1Q1RK1 w - - 5 1",
            "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1"};
        int[] expected = {-50, -5732, -6, 936, 4257, -200, 80, -375, -137, 70, -6};
        var model = BaselineEngine.load().conventional().model();
        var accumulator = model.newAccumulator();
        for (int i = 0; i < fens.length; i++) {
            var board = Board.fromFen(fens[i]); accumulator.rebuild(board);
            assertEquals(expected[i], accumulator.evaluate(board), fens[i]);
            assertEquals(expected[i], model.fullRebuild(board), fens[i]);
        }
    }
}
