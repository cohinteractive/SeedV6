package com.ohinteractive.seedv6.corpus;

import java.io.IOException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Worker-owned, throttled progress and cooperative cancellation for view preparation only. */
public final class CorpusPreparation {
    public record Progress(String stage, long completed, long total) {}
    public static final CorpusPreparation NONE = new CorpusPreparation(() -> false, p -> {});
    public static final class Cancelled extends IOException {
        private Cancelled() { super("Corpus preparation stopped safely"); }
    }
    private final BooleanSupplier cancelled;
    private final Consumer<Progress> observer;
    private String stage = "";
    private long lastPublication;

    public CorpusPreparation(BooleanSupplier cancelled, Consumer<Progress> observer) {
        this.cancelled = java.util.Objects.requireNonNull(cancelled);
        this.observer = java.util.Objects.requireNonNull(observer);
    }
    public void checkCancelled() throws Cancelled {
        if (this != NONE && cancelled.getAsBoolean()) throw new Cancelled();
    }
    public void report(String stage, long completed, long total) throws Cancelled {
        checkCancelled();
        if (this == NONE) return;
        long now = System.nanoTime();
        if (!this.stage.equals(stage) || completed == total || now - lastPublication >= 100_000_000L) {
            this.stage = stage; lastPublication = now;
            observer.accept(new Progress(stage, completed, total));
            checkCancelled();
        }
    }
}
