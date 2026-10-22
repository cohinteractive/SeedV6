package com.ohinteractive.seedv6.uci;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import com.ohinteractive.seedv6.ApplicationVersion;
import com.ohinteractive.seedv6.book.Book;
import com.ohinteractive.seedv6.core.nnue.conventional.ConventionalNetwork;
import com.ohinteractive.seedv6.search.common.TimeSource;
import com.ohinteractive.seedv6.search.driver.ExactSearchAdapter;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.exact.SeePolicy;
import com.ohinteractive.seedv6.search.manage.SearchLifecycleService;
import com.ohinteractive.seedv6.search.tablebase.RootTablebase;
import com.ohinteractive.seedv6.search.tt.TTable;

/** Dedicated trial candidate. The ordinary Gen32 default and research launcher remain unchanged. */
public final class NnueCandidateUciMain {
    static final String NETWORK = "/models/conventional-cp64.cnnue";
    static final int NETWORK_BYTES = 3_940_952;
    static final String SHA256 = "8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b";
    static final String PROFILE = "E008_QXRAY_LMR_WINDOW_EFFORT_HINTS";

    static ConventionalNetwork readPinned(InputStream input) throws Exception {
        if (input == null) throw new IOException("Missing required checkpoint-64 NNUE asset " + NETWORK);
        // Bound reads before decoding, and verify the entire export, including its header.
        byte[] bytes = input.readNBytes(NETWORK_BYTES + 1);
        if (bytes.length != NETWORK_BYTES || !SHA256.equals(HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(bytes))))
            throw new IOException("Required checkpoint-64 NNUE length/SHA-256 mismatch");
        var model = ConventionalNetwork.read(new ByteArrayInputStream(bytes));
        if (!model.schema().equals("S6CNU002") || model.outputBuckets() != 8)
            throw new IOException("Required S6CNU002 eight-head network is incompatible");
        return model;
    }

    static ConventionalNetwork load(Path override) throws Exception {
        try (var input = override == null ? NnueCandidateUciMain.class.getResourceAsStream(NETWORK)
                : Files.newInputStream(override)) {
            return readPinned(input);
        }
    }

    /** Byte-for-byte algorithm/composition equivalent to the frozen E008 research factory. */
    static ExactSearchAdapter search(SearchEvaluation evaluation, int hashMiB) {
        if (!evaluation.isConventional()) throw new IllegalArgumentException("Conventional NNUE required");
        if (hashMiB < 1 || hashMiB > 1024) throw new IllegalArgumentException("Hash must be 1..1024 MiB");
        return ExactSearchAdapter.seeResearch(evaluation, SeePolicy.QXRAY, null, new TTable(hashMiB))
                .withCompletedHints(true, null);
    }

    static UciConfiguration configuration(String[] args) throws Exception {
        Path override = null;
        for (String key : System.getProperties().stringPropertyNames())
            if (key.startsWith("seedv6.uci.") || key.startsWith("seedv6.construction."))
                throw new IllegalArgumentException("Evaluator/search overrides are not candidate options: " + key);
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--scalar" -> System.setProperty("seed.nnue.scalar", "true");
                case "--network" -> {
                    if (override != null || ++i == args.length)
                        throw new IllegalArgumentException("Expected one --network <identical checkpoint-64 file>");
                    override = Path.of(args[i]);
                }
                default -> throw new IllegalArgumentException("Usage: seedv6-nnue-cp64 [--scalar] [--network <identical checkpoint-64 file>]");
            }
        }
        var model = load(override);
        var evaluation = SearchEvaluation.conventional(model);
        var version = ApplicationVersion.load();
        return new UciConfiguration() {
            public SearchLifecycleService service(int workers, int hashMiB) {
                if (workers != 1) throw new IllegalArgumentException("Checkpoint-64 E008 candidate requires Threads=1");
                return new SearchLifecycleService(TimeSource.SYSTEM, () -> search(evaluation, hashMiB),
                        RootTablebase.NONE, Book.bundled());
            }
            public boolean standalone() { return true; }
            public boolean acceptsWorkers(int workers) { return workers == 1; }
            public int maximumWorkers() { return 1; }
            // python-chess expects no unsolicited output while configuring an engine.
            // A fresh 'uci' query always reports the actual current resources.
            public boolean announcesResourceChanges() { return false; }
            public String name() { return "SeedV6 NNUE cp64 E008 RC1 " + version.displayVersion(); }
            public String identity(int workers) { return identity(workers, 64); }
            public String identity(int workers, int hashMiB) {
                return "candidate=nnue-cp64-e008-rc1 evaluator=CONVENTIONAL_NNUE checkpoint=64"
                        + " schema=" + model.schema() + " heads=" + model.outputBuckets()
                        + " networkSha256=" + SHA256 + " payload=" + model.payloadSha256()
                        + " head=" + model.headBackend() + " search=" + PROFILE
                        + " execution=recursive-pvs qsearch=QXRAY lmr=QUIET lmp=WINDOW reuse=EFFORT"
                        + " hints=completed_same_key history=invocation learning=all_searched"
                        + " aspiration=OFF futility=OFF reverseFutility=OFF nullMove=OFF extensions=OFF"
                        + " workers=" + workers + " hashMiB=" + hashMiB
                        + " build=" + version.build() + " source=" + version.sourceRevision();
            }
        };
    }

    public static void main(String[] args) {
        try (var engine = new UciEngine(System.in, System.out, System.err, configuration(args))) {
            var shutdown = new Thread(engine::close, "seedv6-nnue-candidate-shutdown");
            Runtime.getRuntime().addShutdownHook(shutdown);
            try { engine.run(); }
            finally {
                try { Runtime.getRuntime().removeShutdownHook(shutdown); }
                catch (IllegalStateException shuttingDown) { /* Hook drains the search owner. */ }
            }
        } catch (Exception | OutOfMemoryError failure) {
            System.err.println("SeedV6 NNUE candidate startup/runtime error: " + failure.getMessage());
            System.exit(1);
        }
    }
    private NnueCandidateUciMain() {}
}
