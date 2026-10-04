package com.ohinteractive.seedv6.training.data;

import com.ohinteractive.seedv6.corpus.CorpusPosition;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class BinpackDecoderTest {
    @Test void upstreamOracleCoversContinuationScoresCastlesEpPromotionsClockAndResult() throws Exception {
        List<String> expected;
        try (var stream = getClass().getResourceAsStream("/binpack/oracle.tsv")) { expected = new String(stream.readAllBytes(), StandardCharsets.UTF_8).lines().toList(); }
        try (var input = getClass().getResourceAsStream("/binpack/oracle.binpack")) {
            var decoder = new BinpackDecoder(BinpackDecoder.readChunk(input)); int index = 0;
            for (BinpackDecoder.Record record; (record = decoder.next()) != null; index++) {
                String[] fields = expected.get(index).split("\t");
                assertEquals(CorpusPosition.fromFen(fields[0]), record.position(), "position " + index);
                assertEquals(Integer.parseInt(fields[1]), record.target(), "score " + index);
                assertEquals(Integer.parseInt(fields[2]), record.ply()); assertEquals(Integer.parseInt(fields[3]), record.result());
                assertEquals(Integer.parseInt(fields[4]), record.move().from()); assertEquals(Integer.parseInt(fields[5]), record.move().to());
                assertEquals(Integer.parseInt(fields[6]), record.move().type());
            }
            assertEquals(expected.size(), index); assertTrue(index > 60); assertNull(BinpackDecoder.readChunk(input));
        }
    }
    @Test void baseStateAndSentinelAreLosslessAndTruncationIsNeverEof() throws Exception {
        String fen = "r3k2r/8/8/3pP3/8/8/8/R3K2R w KQkq d6 205 3";
        var record = new BinpackDecoder(BinpackFixtures.record(fen, 32002)).next();
        assertEquals(CorpusPosition.fromFen(fen), record.position()); assertEquals(32002, record.target()); assertEquals(1, record.result()); assertEquals(37, record.ply());
        byte[] bytes = BinpackFixtures.chunk(10);
        for (int n = 1; n < bytes.length; n++) {
            byte[] prefix = Arrays.copyOf(bytes, n);
            assertThrows(IOException.class, () -> BinpackDecoder.readChunk(new ByteArrayInputStream(prefix)));
        }
        byte[] raw = BinpackFixtures.record(BinpackFixtures.FEN, 3); raw[33] = 1;
        var broken = new BinpackDecoder(raw); broken.next(); assertThrows(IOException.class, broken::next);
        byte[] oracle;
        try (var in = getClass().getResourceAsStream("/binpack/oracle.binpack")) { oracle = BinpackDecoder.readChunk(in); }
        var truncated = new BinpackDecoder(Arrays.copyOf(oracle, oracle.length - 1));
        assertThrows(IOException.class, () -> { while (truncated.next() != null) {} });
        byte[] tooMany = BinpackFixtures.record(BinpackFixtures.FEN, 3); Arrays.fill(tooMany, 0, 8, (byte) 255);
        assertThrows(IOException.class, () -> new BinpackDecoder(tooMany).next());
    }
}
