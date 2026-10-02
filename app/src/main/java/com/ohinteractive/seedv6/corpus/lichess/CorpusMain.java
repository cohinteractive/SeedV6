package com.ohinteractive.seedv6.corpus.lichess;

import java.nio.file.Path;
import java.util.*;
import com.ohinteractive.seedv6.corpus.*;

/** Independent headless main, available through :app:corpus; never starts UCI or Swing. */
public final class CorpusMain {
    private CorpusMain() {}
    public static void main(String[] args) throws Exception {
        if (args.length == 0 || args[0].equals("help") || args[0].equals("--help")) {
            System.out.println("Seed corpus schema 1\n"
                    + "import-lichess --input FILE.jsonl.zst --corpus ROOT [--max-records N] [--shard-size N] [--progress-every N]\n"
                    + "validate --corpus ROOT [--sample N]\ninspect --corpus ROOT [--sample N]\n"
                    + "Defaults: max-records=0 (unbounded), shard-size=100000, progress-every=100000, sample=3.\n"
                    + "Restart rescans from byte zero; completed data is deduplicated, not efficiently seek-resumed.");
            return;
        }
        String command = args[0];
        Set<String> allowed = switch (command) {
            case "import-lichess" -> Set.of("--input", "--corpus", "--max-records", "--shard-size", "--progress-every");
            case "validate", "inspect" -> Set.of("--corpus", "--sample");
            default -> throw new IllegalArgumentException("Unknown corpus command: " + command);
        };
        Map<String, String> flags = new HashMap<>();
        for (int i = 1; i < args.length; i += 2) {
            if (!allowed.contains(args[i]) || i + 1 >= args.length || flags.putIfAbsent(args[i], args[i + 1]) != null)
                throw new IllegalArgumentException("Unknown, missing or duplicate argument: " + args[i]);
        }
        Path root = Path.of(required(flags, "--corpus"));
        if (command.equals("import-lichess")) {
            LichessImporter.run(new LichessImporter.Options(Path.of(required(flags, "--input")), root,
                    Long.parseLong(flags.getOrDefault("--max-records", "0")),
                    Integer.parseInt(flags.getOrDefault("--shard-size", "100000")),
                    Long.parseLong(flags.getOrDefault("--progress-every", "100000"))), System.out);
        } else {
            int samples = Integer.parseInt(flags.getOrDefault("--sample", "3"));
            if (samples < 0 || samples > 1000) throw new IllegalArgumentException("Sample must be 0..1000");
            try (CorpusReader reader = new CorpusReader(root)) {
                System.out.println(new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(reader.manifest()));
                if (command.equals("validate")) System.out.println("Integrity OK: " + reader.validate());
                long[] visited = {0};
                // Samples are emitted during the logical scan; inspect/validate explicitly read all
                // current records, so they also report an independently observed readable count.
                reader.forEach(r -> {
                    if (visited[0]++ < samples) System.out.println("sample source=" + r.sourceId() + " line=" + r.sourceRecord()
                            + " fen=" + r.position().fen() + " halfmove-known=" + r.position().halfmoveKnown()
                            + " kind=" + (r.targetKind() == CorpusRecord.CP ? "cp" : "mate") + " target=" + r.target()
                            + " perspective=" + r.perspective() + " depth=" + r.depth() + " work=" + r.work() + " unit=" + r.workUnit());
                });
                System.out.println("Readable logical records=" + visited[0]);
            }
        }
    }
    private static String required(Map<String, String> flags, String name) {
        String value = flags.get(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Required argument: " + name);
        return value;
    }
}
