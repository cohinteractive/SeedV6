package com.ohinteractive.seedv6.uci;

import java.nio.file.Path;
import com.ohinteractive.seedv6.ApplicationVersion;
import com.ohinteractive.seedv6.search.common.TimeSource;
import com.ohinteractive.seedv6.search.driver.AspirationSearch;
import com.ohinteractive.seedv6.search.driver.PinnedSearch;
import com.ohinteractive.seedv6.search.manage.SearchLifecycleService;

/** BotLi-compatible explicit experimental executable. No research properties or option injection. */
public final class ConstructionUciMain {
    static UciConfiguration configuration(String[] args) throws Exception {
        Path model = null;
        AspirationSearch.Policy aspiration = null;
        for (String key : System.getProperties().stringPropertyNames())
            if (key.startsWith("seedv6.construction.") || key.startsWith("seedv6.uci."))
                throw new IllegalArgumentException("Research/production property is not a construction-v1 launcher option: " + key);
        for (int i = 0; i < args.length; i += 2) {
            if (i + 1 >= args.length) throw new IllegalArgumentException("Expected --model <file> or --aspiration OFF|WIDEN16");
            switch (args[i]) {
                case "--model" -> model = Path.of(args[i + 1]);
                case "--aspiration" -> aspiration = switch (args[i + 1]) {
                    case "OFF" -> null;
                    case "WIDEN16" -> AspirationSearch.Policy.WIDEN16;
                    default -> throw new IllegalArgumentException("Aspiration must be OFF or WIDEN16");
                };
                default -> throw new IllegalArgumentException("Unknown construction-v1 argument: " + args[i]);
            }
        }
        var binding = PinnedSearch.load(PinnedSearch.Policy.CONSTRUCTION_V1, model, aspiration);
        var version = ApplicationVersion.load();
        return new UciConfiguration() {
            public SearchLifecycleService service(int workers, int hashMiB) {
                return new SearchLifecycleService(TimeSource.SYSTEM, () -> binding.create(workers, hashMiB),
                        com.ohinteractive.seedv6.search.tablebase.RootTablebase.NONE,
                        com.ohinteractive.seedv6.book.Book.bundled());
            }
            public boolean standalone() { return true; }
            public boolean acceptsWorkers(int workers) { return workers >= 1 && workers <= 16; }
            public String name() { return "SeedV6 construction-v1 experimental " + version.displayVersion(); }
            public String identity(int workers) { return identity(workers, 64); }
            public String identity(int workers, int hashMiB) {
                return binding.identity(workers, hashMiB) + " build=" + version.build() + " source=" + version.sourceRevision();
            }
        };
    }
    public static void main(String[] args) {
        try (var engine = new UciEngine(System.in, System.out, System.err, configuration(args))) {
            var shutdown = new Thread(engine::close, "seedv6-construction-shutdown");
            Runtime.getRuntime().addShutdownHook(shutdown);
            try { engine.run(); }
            finally {
                try { Runtime.getRuntime().removeShutdownHook(shutdown); }
                catch (IllegalStateException shuttingDown) { /* Hook drains workers. */ }
            }
        } catch (Exception | OutOfMemoryError failure) {
            System.err.println("SeedV6 construction-v1 startup/runtime error: " + failure.getMessage());
            System.exit(1);
        }
    }
    private ConstructionUciMain() {}
}
