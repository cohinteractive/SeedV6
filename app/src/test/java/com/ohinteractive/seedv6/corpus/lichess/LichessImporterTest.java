package com.ohinteractive.seedv6.corpus.lichess;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.github.luben.zstd.ZstdOutputStream;
import com.ohinteractive.seedv6.corpus.*;

class LichessImporterTest {
    @TempDir Path temp;
    private static final String FEN = "4k3/8/8/8/8/8/8/4K3 b - -";
    private static String json(String fen, String evals) {
        return "{\"fen\":\"" + fen + "\",\"evals\":" + evals + "}";
    }
    private static String evaluation(int depth, String work, String pvs) {
        return "{\"depth\":" + depth + (work == null ? "" : ",\"knodes\":" + work) + ",\"pvs\":" + pvs + "}";
    }
    private static String cp(int value) { return "[{\"cp\":" + value + ",\"line\":\"e8e7\"}]"; }

    @Test void highestDepthThenWorkThenFirstEvaluationAndFirstPvNotLargestCp() {
        String evals = "[" + evaluation(10, "999", cp(100)) + "," + evaluation(11, "20", cp(7)) + ","
                + evaluation(11, "21", "[{\"cp\":-14},{\"cp\":200}]") + "," + evaluation(11, "21", cp(99)) + "]";
        CorpusRecord r = LichessDecoder.decode(json(FEN, evals), 1, 6);
        assertEquals(-14, r.target()); assertEquals(CorpusRecord.WHITE, r.perspective());
        assertEquals(11, r.depth()); assertEquals(21, r.work()); assertEquals(CorpusRecord.KILONODES, r.workUnit());
        assertEquals(6, r.sourceRecord()); assertFalse(r.position().halfmoveKnown());
        r = LichessDecoder.decode(json(FEN, "[" + evaluation(95, "589893", "[{\"mate\":-15}]") + "]"), 1, 1);
        assertEquals(CorpusRecord.MATE, r.targetKind()); assertEquals(-15, r.target());
        r = LichessDecoder.decode(json(FEN + " 23 42", "[" + evaluation(20, null, cp(5)) + "]"), 1, 1);
        assertEquals(-1, r.work()); assertEquals(CorpusRecord.UNKNOWN_WORK, r.workUnit());
        assertEquals(23, r.position().halfmove());
    }

    @Test void malformedTargetsAndFenAreRejectedWhileValidSiblingSurvives() {
        for (String line : List.of("not json", "{}", "{\"fen\":9}", json("bad", "[]"),
                json(FEN, "[" + evaluation(10, "3", "[{\"cp\":2,\"mate\":1}]") + "]"),
                json(FEN, "[" + evaluation(10, "3.5", cp(2)) + "]"),
                json(FEN, "[" + evaluation(10, "3", cp(2)).replace("\"cp\":2", "\"cp\":999999999999") + "]")))
            assertThrows(IllegalArgumentException.class, () -> LichessDecoder.decode(line, 1, 1), line);
        String evals = "[" + evaluation(99, "3", "[]") + "," + evaluation(10, "3", cp(2)) + "]";
        assertEquals(2, LichessDecoder.decode(json(FEN, evals), 1, 1).target());
    }

    @Test void boundedCompressedImportRerunReopenAndUnknownState() throws Exception {
        Path input = temp.resolve("positions.jsonl.zst"), root = temp.resolve("corpus");
        String low = json(FEN, "[" + evaluation(10, "1", cp(2)) + "]");
        String high = json(FEN, "[" + evaluation(11, "1", cp(3)) + "]");
        String known = json(FEN + " 0 1", "[" + evaluation(10, "1", cp(4)) + "]");
        write(input, low + "\ninvalid\n" + high + "\n" + known + "\n" + json(FEN + " 1 2", "[" + evaluation(10, "1", cp(5)) + "]") + "\n");
        var options = new LichessImporter.Options(input, root, 4, 2, 2);
        ByteArrayOutputStream logs = new ByteArrayOutputStream();
        var first = LichessImporter.run(options, new PrintStream(logs));
        assertEquals(4, first.stats().read()); assertEquals(3, first.stats().accepted());
        assertEquals(1, first.stats().rejected()); assertEquals(1, first.stats().duplicates());
        assertEquals(2, first.stats().added()); assertEquals(1, first.stats().upgraded()); assertEquals(2, first.corpusTotal());
        var second = LichessImporter.run(options, new PrintStream(logs));
        assertEquals(2, second.corpusTotal()); assertEquals(0, second.stats().added());
        assertEquals(3, second.stats().duplicates()); assertEquals(0, second.stats().persisted());
        try (CorpusReader reader = new CorpusReader(root)) {
            assertEquals(2, reader.validate().positions());
            List<CorpusRecord> records = new ArrayList<>(); reader.forEach(records::add);
            assertEquals(1, records.stream().filter(r -> !r.position().halfmoveKnown()).count());
            assertEquals(3, records.stream().filter(r -> !r.position().halfmoveKnown()).findFirst().orElseThrow().target());
            assertEquals(8, reader.manifest().sources().getFirst().recordsRead());
        }
        var full = LichessImporter.run(new LichessImporter.Options(input, root, 0, 2, 100), new PrintStream(logs));
        assertEquals(5, full.stats().read()); assertEquals(3, full.corpusTotal());
        assertTrue(logs.toString().contains("corpus-total=2"));
    }

    @Test void giantInvalidLineHasBoundedRetentionAndNextLineStillImports() throws Exception {
        Path input = temp.resolve("giant.zst"), root = temp.resolve("corpus");
        write(input, "x".repeat(1048577) + "\n" + json(FEN, "[" + evaluation(10, "1", cp(3)) + "]"));
        var summary = LichessImporter.run(new LichessImporter.Options(input, root, 0, 1, 100), new PrintStream(OutputStream.nullOutputStream()));
        assertEquals(2, summary.stats().read()); assertEquals(1, summary.stats().rejected()); assertEquals(1, summary.corpusTotal());
    }

    @Test void truncatedZstandardAbortsRatherThanPretendingSuccessfulEof() throws Exception {
        Path input = temp.resolve("truncated.zst"), root = temp.resolve("corpus");
        write(input, (json(FEN, "[" + evaluation(10, "1", cp(3)) + "]") + "\n").repeat(2000));
        byte[] compressed = Files.readAllBytes(input);
        Files.write(input, Arrays.copyOf(compressed, compressed.length - 5));
        assertThrows(IOException.class, () -> LichessImporter.run(new LichessImporter.Options(input, root, 0, 10, 100),
                new PrintStream(OutputStream.nullOutputStream())));
        try (CorpusReader r = new CorpusReader(root)) { r.validate(); }
    }
    private static void write(Path path, String lines) throws IOException {
        try (var out = new ZstdOutputStream(Files.newOutputStream(path))) { out.write(lines.getBytes(StandardCharsets.UTF_8)); }
    }
}
