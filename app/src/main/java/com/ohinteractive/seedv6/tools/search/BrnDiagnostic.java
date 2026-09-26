package com.ohinteractive.seedv6.tools.search;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn.BrnAdamConfig;
import com.ohinteractive.seedv6.core.brn2.*;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Fen;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.alphabeta.RootParallelSearch;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.iterative.IterativeDeepeningSearch;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.ohinteractive.seedv6.training.service.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.telemetry.ActiveGameSnapshot;
import static com.ohinteractive.seedv6.tools.search.DiagnosticReport.*;

/** Cross-host, isolated diagnostics. No GUI, user preferences, normal store discovery or resume. */
public final class BrnDiagnostic {
    static final String SCHEMA = "seedv6-brn-diagnostic-v1";
    static final String PRODUCTION = "SearchDriver/ExactSearch; sequential games; configured workers ignored; no qsearch";
    static final String LEGACY = "supplemental IterativeDeepeningSearch/RootParallelSearch/AlphaBetaPvsSearch with qsearch";
    private static final int LEGACY_TT_ENTRIES = 1 << 18;

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && (args[0].equals("--help") || args[0].equals("help"))) {
            System.out.print(BrnDiagnosticOptions.HELP); return;
        }
        System.setProperty("java.awt.headless", "true");
        run(new BrnDiagnosticOptions(args), repository(), System.out);
    }

    private static Path repository() throws IOException {
        Path current = Path.of("").toAbsolutePath().normalize();
        for (Path p = current; p != null; p = p.getParent())
            if (Files.exists(p.resolve("settings.gradle")) && Files.isDirectory(p.resolve("app"))) return p.toRealPath();
        throw new IOException("Run brn-diagnostic from the SeedV6 repository (or use its Gradle task).");
    }

    static Path run(BrnDiagnosticOptions options, Path repository, PrintStream console) throws Exception {
        long runStarted = System.nanoTime();
        List<String> positions = new ArrayList<>(options.fens);
        if (options.values.containsKey("fen-file")) for (String line : Files.readAllLines(Path.of(options.values.get("fen-file")), StandardCharsets.UTF_8))
            if (!line.isBlank() && !line.stripLeading().startsWith("#")) positions.add(line.strip());
        if (positions.isEmpty() && !options.values.containsKey("fen-file"))
            positions.addAll(Brn2DiagnosticCorpus.POSITIONS.stream().map(SearchBenchmark.Position::fen).toList());
        if (options.mode.equals("replay") && positions.isEmpty()) throw new IllegalArgumentException("Empty FEN corpus.");
        positions.forEach(Board::fromFen);
        Brn2Model initial = options.values.containsKey("model")
                ? Brn2Codec.decodeModel(Files.readAllBytes(Path.of(options.values.get("model")))) : new Brn2Model(options.seed());
        Path directory = allocate(repository, options.values.get("output"));
        console.println("BRN diagnostic report: " + directory.resolve("report.jsonl"));
        try (var report = new Report(options, directory, console, runStarted)) {
            String fingerprint = report.saveModel(initial);
            console.println("Initial BRN-2 parameter SHA-256: " + fingerprint);
            console.println(options.legacy() ? LEGACY : PRODUCTION);
            var config = options.config(directory.resolve("training-store"));
            var metadata = fields("mode", options.mode, "engine", options.legacy() ? "legacy" : "production",
                    "search_path", options.legacy() ? LEGACY : PRODUCTION,
                    "configured_workers", options.workers(), "effective_workers", options.legacy() ? options.workers() : 1,
                    "worker_count_ignored", !options.legacy(), "architecture", "BRN-2", "feature_schema", Brn2Features.VERSION,
                    "codec_version", Brn2Codec.VERSION, "parameter_count", Brn2Model.PARAMETER_COUNT,
                    "brn_score_scale", com.ohinteractive.seedv6.search.evaluation.BrnScoreMapping.SCALE,
                    "base_seed", options.seed(), "initial_fingerprint", fingerprint,
                    "model_source", options.get("model", "virgin-seeded-initialization"),
                    "fingerprint_format", "sha256:DataOutputStream UTF(v1,BRN-2),int(codec,schema,dimensions),long(Double.doubleToLongBits),big-endian",
                    "os.name", System.getProperty("os.name"), "os.version", System.getProperty("os.version"),
                    "os.arch", System.getProperty("os.arch"), "java.version", System.getProperty("java.version"),
                    "java.vendor", System.getProperty("java.vendor"), "java.runtime.name", System.getProperty("java.runtime.name"),
                    "java.runtime.version", System.getProperty("java.runtime.version"), "java.vm.name", System.getProperty("java.vm.name"),
                    "jvm_arguments", ManagementFactory.getRuntimeMXBean().getInputArguments(),
                    "available_processors", Runtime.getRuntime().availableProcessors(), "maximum_heap_bytes", Runtime.getRuntime().maxMemory(),
                    "started_utc", Instant.now().toString(), "settings", structured(config),
                    "requested_depth", options.depth(), "time_limit", null, "node_limit", null,
                    "outlier_ms", options.decimal("outlier-ms", 1000), "outlier_qratio", options.decimal("outlier-qratio", 100),
                    "outlier_nodes", options.longValue("outlier-nodes", 1000000), "legacy_tt_entries", options.legacy() ? LEGACY_TT_ENTRIES : null,
                    "production_tt_requested_mb", options.legacy() ? null : 192,
                    "initialization_algorithm", "java.util.Random(seed), original BRN-2 distributions; StrictMath.sqrt; zero biases",
                    "adam", structured(new BrnAdamConfig(config.brnLearningRate())),
                    "corpus", options.mode.equals("replay") ? positions : List.of(),
                    "node_definition", "ordinary admitted child nodes and qsearch admitted child nodes; root excluded; cumulative across depth iterations",
                    "replay_state", "fresh search/TT per FEN; singleton repetition history",
                    "training_state", "production reuse within game/colour; full repetition history; indexed game/shuffle seeds; sequential updates",
                    "nondeterminism", options.legacy() && options.workers() > 1
                            ? "shared TT and worker scheduling can alter work/order; no forced deterministic parallel semantics"
                            : "no scheduling-dependent training updates in this checkout; timings/JIT/GC/host load vary; compare actual fingerprints/scores/nodes");
            metadata.putAll(git(repository));
            report.emit("run_metadata", metadata);
            report.initialFingerprint = fingerprint;
            report.fingerprints.add(fields("generation", 0, "candidate_fingerprint", fingerprint, "best_fingerprint", fingerprint));
            try {
                if (options.mode.equals("replay")) replay(options, positions, initial, fingerprint, report);
                else BrnDiagnosticTraining.run(config, new Brn2Trainer(initial, new BrnAdamConfig(config.brnLearningRate())), report.training(config));
                report.finish("completed", null);
            } catch (Exception failure) {
                report.finish("failed", failure.toString());
                throw failure;
            }
        } finally { console.println("BRN diagnostic report: " + directory.resolve("report.jsonl")); }
        return directory;
    }

    /** Reject existing outputs and aliases escaping build before opening any store. */
    static Path allocate(Path repository, String output) throws IOException {
        Path root = repository.toRealPath(), build = root.resolve("build");
        if (Files.exists(build) && !build.toRealPath().equals(build)) throw new IOException("Diagnostic build directory is an alias.");
        Path destination = output == null ? build.resolve("brn-diagnostics") : root.resolve(output).normalize().toAbsolutePath();
        if (!destination.startsWith(build) || destination.equals(build)) throw new IOException("Diagnostic output must be beneath repository build/.");
        for (Path p = destination; p != null && p.startsWith(build); p = p.getParent()) {
            if (Files.exists(p) && !p.toRealPath().equals(p)) throw new IOException("Diagnostic output contains an alias: " + p);
        }
        if (output == null) {
            Files.createDirectories(destination);
            return Files.createTempDirectory(destination, "run-");
        }
        Files.createDirectories(destination.getParent());
        return Files.createDirectory(destination); // never reuse, resume or overwrite even an empty existing output
    }

    private static void replay(BrnDiagnosticOptions options, List<String> fens, Brn2Model model, String fingerprint, Report report) throws IOException {
        Files.write(report.directory.resolve("corpus.fen"), fens, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        int index = 0;
        for (String fen : fens) {
            long[] board = Board.fromFen(fen);
            var request = new SearchRequest(board, GameHistory.initial(board), options.depth(), SearchObserver.NONE, SearchControl.unlimited(), true);
            var identity = fields("generation", 0, "game_index", null, "ply", 0, "phase", "replay", "position_id", ++index,
                    "model_fingerprint", fingerprint, "model_file", "models/" + fingerprint + ".brn2");
            report.start(identity, request);
            var evaluation = SearchEvaluation.brn2(model);
            if (options.legacy()) {
                try (var driver = new IterativeDeepeningSearch(new RootParallelSearch(options.workers(), evaluation, LEGACY_TT_ENTRIES))) {
                    long start = System.nanoTime();
                    var outcome = driver.search(request);
                    report.search(identity, request, outcome.lastCompletedResult(), outcome.diagnostics(), System.nanoTime() - start, outcome.targetDepthCompleted());
                }
            } else {
                try (var driver = new SearchDriver(evaluation)) {
                    long start = System.nanoTime();
                    var outcome = driver.search(request);
                    report.search(identity, request, outcome.lastCompletedResult(), outcome.diagnostics(), System.nanoTime() - start, outcome.targetDepthCompleted());
                }
            }
        }
    }

    private static Map<String, Object> git(Path root) {
        var result = fields("git_commit", null, "git_dirty", null, "git_status", null);
        try {
            String commit = gitCommand(root, "rev-parse", "HEAD").strip();
            String status = gitCommand(root, "status", "--porcelain=v1", "--untracked-files=normal");
            result.put("git_commit", commit); result.put("git_dirty", !status.isBlank()); result.put("git_status", status);
        } catch (Exception unavailable) { result.put("git_error", unavailable.toString()); }
        return result;
    }
    private static String gitCommand(Path root, String... args) throws Exception {
        var command = new ArrayList<>(List.of("git", "-C", root.toString())); command.addAll(List.of(args));
        var process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.waitFor() != 0) throw new IOException(output.strip());
        return output;
    }

    /** Structured settings/metrics, with unavailable/nonfinite loss values encoded as JSON null. */
    static Object structured(Object value) {
        if (value == null) return null;
        if (value instanceof Double d && !Double.isFinite(d)) return null;
        if (value instanceof Enum<?> e) return e.name();
        if (value instanceof Path) return value.toString();
        if (value.getClass().isRecord()) {
            var result = new LinkedHashMap<String, Object>();
            for (var component : value.getClass().getRecordComponents()) try {
                result.put(component.getName(), structured(component.getAccessor().invoke(value)));
            } catch (ReflectiveOperationException problem) { throw new IllegalStateException(problem); }
            return result;
        }
        if (value instanceof Map<?, ?> map) {
            var result = new LinkedHashMap<String, Object>(); map.forEach((k,v) -> result.put(k.toString(), structured(v))); return result;
        }
        return value;
    }

    private static final class Totals {
        long searches, nodes, qnodes, searchNanos, outliers, maximumNodes, maximumQnodes, slowestNanos;
        Map<String, Object> worst;
        void add(Map<String, Object> row, long main, long q, long nanos, boolean outlier) {
            searches++; nodes += main; qnodes += q; searchNanos += nanos; if (outlier) outliers++;
            maximumNodes = Math.max(maximumNodes, main + q); maximumQnodes = Math.max(maximumQnodes, q);
            if (worst == null || nanos > slowestNanos) { slowestNanos = nanos; worst = new LinkedHashMap<>(row); }
        }
        Map<String, Object> fields() {
            return DiagnosticReport.fields("total_searches", searches, "nodes", nodes, "qnodes", qnodes, "total_nodes", nodes + qnodes,
                    "qnode_percentage", nodes + qnodes == 0 ? 0 : 100.0 * qnodes / (nodes + qnodes),
                    "search_elapsed_ns", searchNanos, "nps", nps(nodes + qnodes, searchNanos), "outliers", outliers,
                    "maximum_root_nodes", maximumNodes, "maximum_root_qnodes", maximumQnodes,
                    "slowest_root_ns", slowestNanos, "worst_search", worst);
        }
    }
    private static double nps(long nodes, long nanos) { return nanos == 0 ? 0 : nodes * 1e9 / nanos; }

    private static final class Report implements AutoCloseable {
        final BrnDiagnosticOptions options;
        final Path directory;
        final PrintStream console;
        final PrintWriter writer;
        final long started;
        final Totals total = new Totals();
        Totals generation = new Totals();
        final List<Map<String, Object>> fingerprints = new ArrayList<>();
        final Map<String, String> checkpointFingerprints = new HashMap<>();
        String initialFingerprint;
        long sequence, currentGeneration;
        int positionId;
        Report(BrnDiagnosticOptions options, Path directory, PrintStream console, long started) throws IOException {
            this.options = options; this.directory = directory; this.console = console;
            this.started = started;
            writer = new PrintWriter(Files.newBufferedWriter(directory.resolve("report.jsonl"), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW));
        }
        void emit(String type, Map<String, Object> data) {
            var row = fields("schema", SCHEMA, "record_type", type, "sequence", ++sequence);
            row.putAll(data); writer.println(json(row)); writer.flush();
            if (writer.checkError()) throw new UncheckedIOException(new IOException("Cannot write diagnostic report"));
        }
        String saveModel(Brn2Model model) throws IOException {
            String fingerprint = BrnDiagnosticModel.fingerprint(model);
            Path models = directory.resolve("models"); Files.createDirectories(models);
            Path file = models.resolve(fingerprint + ".brn2");
            if (!Files.exists(file)) try (var out = new BufferedOutputStream(Files.newOutputStream(file, StandardOpenOption.CREATE_NEW))) { Brn2Codec.writeModel(model, out); }
            return fingerprint;
        }
        String checkpoint(String id) {
            if (id.equals("HANDCRAFTED")) return null;
            return checkpointFingerprints.computeIfAbsent(id, key -> {
                try {
                    var model = CheckpointStore.inspect(directory.resolve("training-store/checkpoints").resolve(key)).model();
                    return saveModel(((com.ohinteractive.seedv6.training.model.NetworkModel.Brn2) model).model());
                } catch (IOException error) { throw new UncheckedIOException(error); }
            });
        }
        Map<String, Object> identity(ActiveGameSnapshot game) {
            if (game == null) throw new IllegalStateException("Missing production game identity at root boundary");
            String id = (game.sideToMove() == 0 ? game.white() : game.black()).checkpointId();
            String fingerprint = checkpoint(id);
            return fields("generation", game.generation(), "game_index", game.gameOrdinal() - 1, "ply", game.playedPlies(),
                    "phase", game.phase().name().toLowerCase(Locale.ROOT), "position_id", positionId,
                    "actor_checkpoint", id, "model_fingerprint", fingerprint,
                    "model_file", fingerprint == null ? null : "models/" + fingerprint + ".brn2");
        }
        Map<String, Object> position(Map<String, Object> identity, SearchRequest request) {
            var row = new LinkedHashMap<>(identity);
            long[] board = new long[Board.MAX_BITBOARDS]; request.copyBoardInto(board);
            row.putAll(fields("fen", Fen.fromBoard(board), "side_to_move", Board.player((int)board[Board.STATUS]) == 0 ? "w" : "b",
                    "move_number", Board.fullMoveNumber((int)board[Board.STATUS]), "depth", request.depth(),
                    "history_size", request.gameHistory().size()));
            return row;
        }
        void start(Map<String, Object> identity, SearchRequest request) {
            emit("search_start", position(identity, request)); // preserve the FEN even if a search never returns
        }
        void search(Map<String, Object> identity, SearchRequest request, SearchResult result, SearchDiagnosticsSnapshot diagnostics,
                    long nanos, boolean completed) {
            var row = position(identity, request);
            var metrics = diagnostics.worker();
            long nodes = metrics.nodes().mainNodes(), qnodes = metrics.nodes().qNodes();
            boolean elapsedOutlier = nanos / 1e6 >= options.decimal("outlier-ms", 1000);
            boolean ratioOutlier = qnodes > 0 && qnodes / (double)Math.max(1, nodes) >= options.decimal("outlier-qratio", 100);
            boolean nodesOutlier = nodes + qnodes >= options.longValue("outlier-nodes", 1000000);
            boolean outlier = elapsedOutlier || ratioOutlier || nodesOutlier;
            var reasons = new ArrayList<String>();
            if (elapsedOutlier) reasons.add("elapsed_ms"); if (ratioOutlier) reasons.add("qnodes_per_ordinary_node"); if (nodesOutlier) reasons.add("total_nodes");
            row.putAll(fields("best_move", result != null && result.hasMove() ? Move.coordinate(result.bestMove()) : null,
                    "score", result == null ? null : result.score(), "completed", completed, "nodes", nodes, "qnodes", qnodes,
                    "total_nodes", nodes + qnodes, "elapsed_ns", nanos, "elapsed_ms", nanos / 1e6, "nps", nps(nodes + qnodes, nanos),
                    "maximum_qply", metrics.nodes().maximumQply(), "stand_pat_beta_cutoffs", metrics.qsearch().standPatCutoffs(),
                    "stand_pat_attempts", null, "qsearch_beta_cutoffs", null,
                    "tt_probes", options.legacy() ? metrics.transpositionTable().probes() : null,
                    "tt_hits", options.legacy() ? metrics.transpositionTable().keyMatches() : null,
                    "tt_cutoffs", options.legacy() ? metrics.transpositionTable().usableBoundCutoffs() : null,
                    "outlier", outlier, "outlier_reasons", reasons));
            // Decimal strings avoid losing 64-bit repetition identities in JavaScript JSON consumers.
            var history = new ArrayList<String>();
            for (int i = 0; i < request.gameHistory().size(); i++) history.add(Long.toUnsignedString(request.gameHistory().keyAt(i)));
            row.put("history_keys_u64", history);
            emit("search", row);
            total.add(row, nodes, qnodes, nanos, outlier); generation.add(row, nodes, qnodes, nanos, outlier);
            if (outlier) { emit("search_outlier", row); console.println("Outlier position " + identity.get("position_id") + ": " + row.get("elapsed_ms") + " ms; " + row.get("fen")); }
            if (!completed) throw new IllegalStateException("Fixed-depth diagnostic search did not complete.");
        }
        BrnDiagnosticTraining.Listener training(TrainerConfig config) {
            return new BrnDiagnosticTraining.Listener() {
                public void phase(TrainerSnapshot snapshot) {
                    if (snapshot.state() == TrainerSnapshot.State.RECOVERING && snapshot.generation() == 0 && !snapshot.bestId().isEmpty()) {
                        checkpointFingerprints.put(snapshot.bestId(), initialFingerprint);
                        emit("generation_end", fields("generation", 0, "initialization_only", true,
                                "candidate_fingerprint", initialFingerprint, "best_fingerprint", initialFingerprint));
                    } else if (snapshot.state() == TrainerSnapshot.State.GENERATING_SELF_PLAY) {
                        generation = new Totals(); currentGeneration = snapshot.generation();
                        var row = fields("generation", snapshot.generation(), "candidate_fingerprint", checkpoint(snapshot.latestTrainingId()),
                                "best_fingerprint", checkpoint(snapshot.bestId()));
                        for (var domain : TrainerConfig.SeedDomain.values()) row.put(domain.name().toLowerCase(Locale.ROOT) + "_seed", config.seed(snapshot.generation(), domain));
                        emit("generation_start", row);
                    } else if (snapshot.state() == TrainerSnapshot.State.RECORDING_DECISION && snapshot.generation() > 0
                            && snapshot.totals().completedGenerations() == snapshot.generation()) {
                        var row = generation.fields();
                        var identity = fields("generation", snapshot.generation(), "candidate_fingerprint", checkpoint(snapshot.candidateId()),
                                "best_fingerprint", checkpoint(snapshot.bestId()));
                        row.putAll(identity); fingerprints.add(identity);
                        row.putAll(fields("wall_ns", snapshot.generationElapsed(System.nanoTime()).orElseThrow().toNanos(), "completed_games", snapshot.selfPlay().completedGames(),
                                "generated_positions", snapshot.selfPlay().rawTrajectoryPositions(), "generated_samples", snapshot.selfPlay().sampledPositions(),
                                "games", structured(snapshot.selfPlay()), "training_loss", snapshot.training().map(BrnDiagnostic::structured).orElse(null),
                                "candidate_id", snapshot.candidateId(), "best_id", snapshot.bestId(),
                                "promotion_outcome", snapshot.bootstrapValidation().map(v -> v.evidence().comparison().decision().name())
                                        .orElseGet(() -> snapshot.assessment().orElseThrow().decision().name()),
                                "assessment", snapshot.assessment().map(BrnDiagnostic::structured).orElse(null),
                                "held_out", snapshot.bootstrapValidation().map(BrnDiagnostic::structured).orElse(null),
                                "validation", snapshot.validation().map(BrnDiagnostic::structured).orElse(null),
                                "cumulative_nodes", total.nodes, "cumulative_qnodes", total.qnodes));
                        emit("generation_end", row);
                        console.println("Generation " + snapshot.generation() + ": " + snapshot.selfPlay().completedGames() + " completed games; Candidate " + identity.get("candidate_fingerprint") + "; Best " + identity.get("best_fingerprint"));
                    }
                }
                public void game(SelfPlayBatch.Progress progress) {
                    emit("game_end", fields("generation", currentGeneration, "game", structured(progress.lastGame()), "statistics", structured(progress.statistics())));
                }
                public void started(ActiveGameSnapshot game, SearchRequest request) { positionId++; start(identity(game), request); }
                public void searched(ActiveGameSnapshot game, SearchRequest request, SearchDriverOutcome outcome, long nanos) {
                    search(identity(game), request, outcome.lastCompletedResult(), outcome.diagnostics(), nanos, outcome.targetDepthCompleted());
                }
            };
        }
        void finish(String status, String error) throws IOException {
            long wall = System.nanoTime() - started;
            var row = total.fields(); row.putAll(fields("status", status, "error", error, "wall_ns", wall,
                    "wall_nps", nps(total.nodes + total.qnodes, wall), "generation_fingerprints", fingerprints)); emit("run_end", row);
            StringBuilder summary = new StringBuilder("SeedV6 BRN diagnostic: ").append(status).append('\n')
                    .append(options.legacy() ? LEGACY : PRODUCTION).append('\n')
                    .append("Seed: ").append(options.seed()).append("; initial fingerprint: ").append(initialFingerprint).append('\n')
                    .append("Configured workers: ").append(options.workers()).append("; effective workers: ").append(options.legacy() ? options.workers() : 1).append('\n')
                    .append(String.format(Locale.ROOT, "Wall: %.3f s; searches: %d; nodes: %d; qnodes: %d (%.3f%%); search NPS: %.1f; wall NPS: %.1f; outliers: %d%n",
                            wall / 1e9, total.searches, total.nodes, total.qnodes, (double)row.get("qnode_percentage"), nps(total.nodes + total.qnodes, total.searchNanos), nps(total.nodes + total.qnodes, wall), total.outliers));
            if (total.worst != null) summary.append("Slowest: ").append(total.slowestNanos / 1e6).append(" ms; ").append(total.worst.get("fen")).append('\n');
            for (var item : fingerprints) summary.append("Generation ").append(item.get("generation")).append(" Candidate ").append(item.get("candidate_fingerprint")).append(" Best ").append(item.get("best_fingerprint")).append('\n');
            if (error != null) summary.append("Failure: ").append(error).append('\n');
            Files.writeString(directory.resolve("summary.txt"), summary, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            console.print(summary);
        }
        @Override public void close() { writer.close(); }
    }
    private BrnDiagnostic() {}
}
