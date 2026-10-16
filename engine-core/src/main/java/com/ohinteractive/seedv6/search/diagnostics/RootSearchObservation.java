package com.ohinteractive.seedv6.search.diagnostics;

import java.util.Objects;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.driver.SearchDriverOutcome;

/** Opt-in, caller-thread scope. No worker/node-loop access, synchronization, or production logging. */
public final class RootSearchObservation implements AutoCloseable {
    public interface Listener {
        void started(SearchRequest request);
        void finished(SearchRequest request, SearchDriverOutcome outcome, long elapsedNanos);
    }
    private static final ThreadLocal<Listener> CURRENT = new ThreadLocal<>();
    private final Thread owner = Thread.currentThread();
    private final Listener previous;
    private boolean closed;
    private RootSearchObservation(Listener listener) {
        previous = CURRENT.get();
        CURRENT.set(Objects.requireNonNull(listener));
    }
    public static RootSearchObservation observe(Listener listener) { return new RootSearchObservation(listener); }
    public static Listener current() { return CURRENT.get(); }
    @Override public void close() {
        if (Thread.currentThread() != owner || closed) throw new IllegalStateException("Invalid observation scope close.");
        closed = true;
        if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
    }
}
