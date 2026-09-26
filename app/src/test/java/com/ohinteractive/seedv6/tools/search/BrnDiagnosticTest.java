package com.ohinteractive.seedv6.tools.search;

import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.prefs.*;
import com.google.gson.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn2.*;
import com.ohinteractive.seedv6.core.util.Fen;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.diagnostics.RootSearchObservation;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(90)
class BrnDiagnosticTest {
    @TempDir Path temporary;
    static final String MATE = "7k/5Q2/6K1/8/8/8/8/8 w - - 0 1";
    static final long SEED = 20260926;

    @Test void seedsAndCodecGiveStableActualParameterIdentity() throws Exception {
        var first = new Brn2Model(SEED);
        String fingerprint = BrnDiagnosticModel.fingerprint(first);
        assertEquals(fingerprint, BrnDiagnosticModel.fingerprint(new Brn2Model(SEED)));
        assertNotEquals(fingerprint, BrnDiagnosticModel.fingerprint(new Brn2Model(SEED + 1)));
        assertEquals(fingerprint, BrnDiagnosticModel.fingerprint(Brn2Codec.decodeModel(Brn2Codec.encodeModel(first))));
        assertArrayEquals(new Brn2Model().copyWeights(), new Brn2Model(Brn2Model.INITIALIZATION_SEED).copyWeights());
        double[] changed = first.copyWeights(); changed[changed.length - 1] = -0.0;
        assertNotEquals(fingerprint, BrnDiagnosticModel.fingerprint(new Brn2Model(changed)), "Signed zero is part of the bit identity");
    }

    @Test void fenRoundTripsCorpusAndActualMoveStates() {
        for (var position : Brn2DiagnosticCorpus.POSITIONS) {
            var board = Board.fromFen(position.fen());
            assertArrayEquals(board, Board.fromFen(Fen.fromBoard(board)), position.fen());
            for (long move : Brn2Diagnostics.legalMoves(board)) {
                var child = Brn2Diagnostics.play(board, move);
                assertArrayEquals(child, Board.fromFen(Fen.fromBoard(child)), Fen.fromBoard(child));
            }
        }
        for (String fen : List.of("4k3/8/8/3pP3/8/8/8/4K3 w - d6 99 43", "4k3/8/8/8/3Pp3/8/8/4K3 b - d3 0 43"))
            assertEquals(fen, Fen.fromBoard(Board.fromFen(fen)));
    }

    @Test void parsingIsStrictAndAllProductionSettingsAreExplicit() {
        var o = options("train", "--workers=6", "--generations", "16", "--games=256", "--validation=held-out", "--shuffle=false");
        var c = o.config(temporary.resolve("store"));
        assertEquals(6, c.selfPlay().threads()); assertEquals(16, c.maximumGenerations());
        assertEquals(256, c.selfPlay().games()); assertFalse(c.training().shuffle());
        assertEquals(1, c.training().epochs()); assertEquals(1, c.training().minibatchSize());
        assertEquals(SEED, c.masterSeed()); assertEquals(SEED, c.runSeeds().dataSeed());
        for (String[] args : List.of(new String[]{"train"}, new String[]{"replay", "--seed=1", "--unknown=x"},
                new String[]{"replay", "--seed=1", "--seed=2"}, new String[]{"train", "--seed=1", "--model=x"},
                new String[]{"train", "--seed=1", "--engine=legacy"}, new String[]{"replay", "--seed=1", "--workers=0"},
                new String[]{"train", "--seed=1", "--generations=0"}, new String[]{"replay", "--seed=1", "--outlier-ms=NaN"}))
            assertThrows(IllegalArgumentException.class, () -> new BrnDiagnosticOptions(args));
    }

    @Test void observedProductionSearchPreservesMoveScoreNodesAndScope() {
        var evaluation = SearchEvaluation.brn2(new Brn2Model(SEED));
        var request = new SearchRequest(Board.fromFen(MATE), 2);
        var observed = new ArrayList<SearchDriverOutcome>();
        assertNull(RootSearchObservation.current());
        try (var normal = new SearchDriver(new ExactSearchAdapter(evaluation, new TTable(1)));
             var traced = new SearchDriver(new ExactSearchAdapter(evaluation, new TTable(1)))) {
            var expected = normal.search(request);
            try (var scope = RootSearchObservation.observe(new RootSearchObservation.Listener() {
                public void started(SearchRequest r) { assertTrue(r.diagnosticsEnabled()); }
                public void finished(SearchRequest r, SearchDriverOutcome result, long nanos) { observed.add(result); assertTrue(nanos > 0); }
            })) { traced.search(request); }
            assertEquals(1, observed.size());
            var actual = observed.getFirst().lastCompletedResult(); var plain = expected.lastCompletedResult();
            assertEquals(plain.bestMove(), actual.bestMove()); assertEquals(plain.score(), actual.score());
            assertEquals(plain.nodes(), actual.nodes()); assertEquals(actual.nodes(), actual.diagnostics().worker().nodes().mainNodes());
            assertEquals(0, actual.diagnostics().worker().nodes().qNodes());
        }
        assertNull(RootSearchObservation.current());
    }

    @Test void tinyReplayIsReproducibleAndOutliersNeverStopIt() throws Exception {
        var a = run(options("replay", "--fen=" + MATE, "--fen=" + MATE, "--outlier-ms=0"));
        var b = run(options("replay", "--fen=" + MATE, "--fen=" + MATE, "--workers=6"));
        var searches = rows(a, "search"); assertEquals(2, searches.size()); assertEquals(2, rows(a, "search_outlier").size());
        for (int i = 0; i < 2; i++) for (String field : List.of("fen", "best_move", "score", "nodes", "qnodes", "model_fingerprint"))
            assertEquals(searches.get(i).get(field), rows(b, "search").get(i).get(field), field);
        assertTrue(rows(b, "run_metadata").getFirst().get("worker_count_ignored").getAsBoolean());
        assertEquals(1, rows(b, "run_metadata").getFirst().get("effective_workers").getAsInt());
        assertEquals(2, searches.getLast().get("position_id").getAsInt());
        assertEquals("completed", rows(a, "run_end").getFirst().get("status").getAsString());
    }

    @Test void legacyReplayActuallyUsesRootWorkersAndReportsQsearch() throws Exception {
        var path = run(options("replay", "--engine=legacy", "--workers=2", "--depth=1", "--fen=" + MATE));
        var metadata = rows(path, "run_metadata").getFirst();
        assertEquals(2, metadata.get("effective_workers").getAsInt()); assertFalse(metadata.get("worker_count_ignored").getAsBoolean());
        assertFalse(rows(path, "search").getFirst().get("tt_probes").isJsonNull());
        assertEquals("completed", rows(path, "run_end").getFirst().get("status").getAsString());
    }

    @Test void newOutputCannotOverwriteOrEscapeBuild() throws Exception {
        assertThrows(IOException.class, () -> BrnDiagnostic.allocate(temporary, "production-store"));
        assertThrows(IOException.class, () -> BrnDiagnostic.allocate(temporary, "build/../production-store"));
        var path = BrnDiagnostic.allocate(temporary, "build/a space");
        Files.writeString(path.resolve("sentinel"), "keep");
        assertThrows(FileAlreadyExistsException.class, () -> BrnDiagnostic.allocate(temporary, "build/a space"));
        assertEquals("keep", Files.readString(path.resolve("sentinel")));
    }

    @Test void realTrainingIsIsolatedHeadlessAndCapturedFensReplayWithSavedModel() throws Exception {
        // A separate JVM proves Main does not load any GUI class or consult preferences, and
        // isolates fake platform-default user stores from the test runner and the real user.
        Files.writeString(temporary.resolve("settings.gradle"), ""); Files.createDirectory(temporary.resolve("app"));
        Path home = Files.createDirectory(temporary.resolve("home")), local = Files.createDirectory(temporary.resolve("local"));
        Path normal = Files.createDirectories(home.resolve(".seedv6-nnue/training"));
        Path windows = Files.createDirectories(local.resolve("SeedV6-NNUE/training"));
        Files.writeString(normal.resolve("sentinel"), "never read or write"); Files.writeString(windows.resolve("sentinel"), "never read or write");
        var before = files(home, local);
        Path log = temporary.resolve("classes.log");
        var command = new ArrayList<>(List.of(java(), "-Xmx1024m", "-Djava.awt.headless=true", "-Duser.home=" + home,
                "-Djava.util.prefs.PreferencesFactory=" + NoPreferences.class.getName(),
                "-Xlog:class+load=info:file=classes.log", "-cp", System.getProperty("java.class.path"), "com.ohinteractive.seedv6.Main",
                "brn-diagnostic", "train", "--seed=" + SEED, "--generations=1", "--games=1", "--depth=1", "--workers=6",
                "--opening-max=0", "--max-plies=4", "--samples=2", "--validation-pairs=1", "--starting-fen=" + MATE,
                "--output=build/isolated"));
        var process = new ProcessBuilder(command).directory(temporary.toFile()).redirectErrorStream(true)
                .redirectOutput(temporary.resolve("process.txt").toFile());
        process.environment().put("LOCALAPPDATA", local.toString());
        var child = process.start();
        try { assertTrue(child.waitFor(75, TimeUnit.SECONDS)); }
        finally { if (child.isAlive()) child.destroyForcibly(); }
        assertEquals(0, child.exitValue(), Files.readString(temporary.resolve("process.txt")));
        assertEquals(before, files(home, local));
        assertFalse(Files.readString(log).contains("com.ohinteractive.seedv6.gui."), "No GUI classes may initialize");
        Path output = temporary.resolve("build/isolated");
        assertEquals(2, rows(output, "generation_end").size(), "Only generation zero and settled generation one");
        var generation = rows(output, "generation_end").getLast();
        assertEquals(1, generation.get("completed_games").getAsInt());
        assertTrue(generation.get("generated_samples").getAsInt() > 0);
        assertFalse(generation.get("training_loss").isJsonNull());
        assertNotEquals(rows(output, "run_metadata").getFirst().get("initial_fingerprint"), generation.get("candidate_fingerprint"));
        var searches = rows(output, "search");
        assertTrue(searches.stream().anyMatch(s -> s.get("phase").getAsString().equals("validation")));
        var search = searches.getFirst();
        Path corpus = temporary.resolve("captured.fen"); Files.writeString(corpus, search.get("fen").getAsString() + "\n");
        var replay = run(options("replay", "--depth=1", "--fen-file=" + corpus, "--model=" + output.resolve(search.get("model_file").getAsString())));
        var replayed = rows(replay, "search").getFirst();
        for (String field : List.of("fen", "best_move", "score", "nodes", "model_fingerprint")) assertEquals(search.get(field), replayed.get(field), field);
        long nodes = searches.stream().mapToLong(s -> s.get("nodes").getAsLong()).sum();
        assertEquals(nodes, rows(output, "run_end").getFirst().get("nodes").getAsLong());
    }

    @Test void heldOutProductionTrainingRecordsSeparateCandidateBestAndLosses() throws Exception {
        var output = run(options("train", "--games=4", "--generations=1", "--depth=1", "--opening-max=0",
                "--max-plies=4", "--samples=2", "--validation=held-out", "--starting-fen=" + MATE));
        var end = rows(output, "generation_end").getLast();
        assertEquals(2, rows(output, "generation_end").size());
        assertFalse(end.get("held_out").isJsonNull()); assertEquals(4, end.get("completed_games").getAsInt());
        assertNotNull(end.get("promotion_outcome")); assertEquals(0, end.get("qnodes").getAsLong());
    }

    static BrnDiagnosticOptions options(String mode, String... args) {
        var result = new ArrayList<>(List.of(mode, "--seed=" + SEED)); result.addAll(List.of(args));
        return new BrnDiagnosticOptions(result.toArray(String[]::new));
    }
    Path run(BrnDiagnosticOptions options) throws Exception {
        return BrnDiagnostic.run(options, temporary, new PrintStream(OutputStream.nullOutputStream()));
    }
    static List<JsonObject> rows(Path output, String type) throws IOException {
        var rows = new ArrayList<JsonObject>(); long sequence = 0;
        for (String line : Files.readAllLines(output.resolve("report.jsonl"))) {
            var row = JsonParser.parseString(line).getAsJsonObject();
            assertEquals(BrnDiagnostic.SCHEMA, row.get("schema").getAsString());
            assertEquals(++sequence, row.get("sequence").getAsLong());
            if (row.get("record_type").getAsString().equals(type)) rows.add(row);
            if (row.get("record_type").getAsString().equals("search")) for (String field : List.of("position_id", "generation",
                    "game_index", "ply", "fen", "side_to_move", "depth", "best_move", "score", "nodes", "qnodes", "total_nodes",
                    "elapsed_ns", "elapsed_ms", "nps", "model_fingerprint", "completed")) assertTrue(row.has(field), field);
        }
        return rows;
    }
    static Map<String, String> files(Path... roots) throws IOException {
        var result = new TreeMap<String, String>();
        for (Path root : roots) try (var paths = Files.walk(root)) {
            for (Path file : paths.filter(Files::isRegularFile).toList()) result.put(file.toString(), Files.readString(file) + Files.getLastModifiedTime(file));
        }
        return result;
    }
    static String java() { return Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString(); }
    public static final class NoPreferences implements PreferencesFactory {
        public Preferences userRoot() { throw new AssertionError("Diagnostics must not read configured production preferences"); }
        public Preferences systemRoot() { throw new AssertionError("Diagnostics must not read production preferences"); }
    }
}
