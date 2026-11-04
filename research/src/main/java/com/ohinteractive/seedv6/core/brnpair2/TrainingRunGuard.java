package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;

/** Managed runs require the existing Python supervisor's live per-run lease.
 * Legacy configurations retain their original behavior; no historical adoption.
 */
final class TrainingRunGuard {
    static void check(String configName) throws Exception {
        Path configPath = Path.of(configName);
        var config = JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();
        Path out = Path.of(config.get("output").getAsString()).toAbsolutePath().normalize();
        Path marker = out.resolve("training-run.json");
        if (!config.has("training_finalization")) {
            if (Files.exists(marker)) throw new IOException("Managed run requires original opt-in config");
            return;
        }
        if (!config.get("training_finalization").getAsString().equals("explicit-v1"))
            throw new IOException("Unsupported training lifecycle");
        var run = JsonParser.parseString(Files.readString(marker)).getAsJsonObject();
        String token = System.getenv("SEED_TRAINING_TOKEN");
        if (!run.get("status").getAsString().equals("running") || token == null ||
                !token.equals(run.get("token").getAsString()) ||
                !PairObjectiveDiagnostic.sha(configPath).equals(run.get("config_sha256").getAsString()))
            throw new IOException("Use the managed training runner; completed/finalized runs cannot resume");
        try (var channel = FileChannel.open(out.resolve("training-run.lock"), StandardOpenOption.WRITE)) {
            try (var lock = channel.tryLock(0, 1, false)) {
                if (lock != null) throw new IOException("Training supervisor lease is absent");
            }
        }
    }
    private TrainingRunGuard() {}
}
