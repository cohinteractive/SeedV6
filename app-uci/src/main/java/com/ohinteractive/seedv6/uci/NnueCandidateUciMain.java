package com.ohinteractive.seedv6.uci;

import java.io.InputStream;
import java.nio.file.Path;
import com.ohinteractive.seedv6.ApplicationVersion;
import com.ohinteractive.seedv6.core.nnue.conventional.ConventionalNetwork;
import com.ohinteractive.seedv6.search.driver.ConventionalNnueEngine;
import com.ohinteractive.seedv6.search.driver.ExactSearchAdapter;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.manage.SearchLifecycleService;

/** Dedicated trial candidate. The ordinary Gen32 default and research launcher remain unchanged. */
public final class NnueCandidateUciMain {
    static final String NETWORK = ConventionalNnueEngine.NETWORK;
    static final String SHA256 = ConventionalNnueEngine.SHA256;
    static final String PROFILE = ConventionalNnueEngine.PROFILE;

    static ConventionalNetwork readPinned(InputStream input) throws Exception {
        return ConventionalNnueEngine.readPinned(input);
    }
    static ConventionalNetwork load(Path override) throws Exception {
        return ConventionalNnueEngine.load(override).model();
    }
    static ExactSearchAdapter search(SearchEvaluation evaluation, int hashMiB) {
        return ConventionalNnueEngine.search(evaluation, hashMiB);
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
        var engine = ConventionalNnueEngine.load(override);
        var version = ApplicationVersion.load();
        return new UciConfiguration() {
            public SearchLifecycleService service(int workers, int hashMiB) {
                return engine.service(workers, hashMiB);
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
                return "candidate=nnue-cp64-e008-rc1 " + engine.identity(hashMiB)
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
