package com.ohinteractive.seedv6.uci;

/** Direct headless entry point for Arena, BotLi and ordinary standard-stream clients. */
public final class UciMain {
    public static void main(String[] args) {
        try {
            for (String key : System.getProperties().stringPropertyNames())
                if (key.startsWith("seedv6.uci.") && !key.equals("seedv6.uci.model"))
                    throw new IllegalArgumentException("Legacy/research property is not a standalone production option: " + key);
            if (args.length != 0 && (args.length != 2 || !args[0].equals("--model")))
                throw new IllegalArgumentException("Usage: seedv6-uci [--model <pinned baseline.brn-pair2>]");
            String selected = args.length == 2 ? args[1] : System.getProperty("seedv6.uci.model");
            var configuration = ProductionUciConfiguration.load(ProductionModel.resolve(selected));
            try (var engine = new UciEngine(System.in, System.out, System.err, configuration)) {
                var shutdown = new Thread(engine::close, "seedv6-uci-shutdown");
                Runtime.getRuntime().addShutdownHook(shutdown);
                try { engine.run(); }
                finally {
                    try { Runtime.getRuntime().removeShutdownHook(shutdown); }
                    catch (IllegalStateException shuttingDown) { /* JVM hook owns cooperative drain. */ }
                }
            }
        } catch (Exception | OutOfMemoryError failure) {
            System.err.println("SeedV6 UCI startup/runtime error: " + failure.getMessage());
            System.exit(1);
        }
    }
    private UciMain() {}
}
