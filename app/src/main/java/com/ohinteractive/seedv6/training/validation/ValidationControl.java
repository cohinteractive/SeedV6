package com.ohinteractive.seedv6.training.validation;

import com.ohinteractive.seedv6.search.common.SearchControl;
import com.ohinteractive.seedv6.search.common.SearchTermination;
import com.ohinteractive.seedv6.search.common.TimeSource;

/** One arena caller and an optional cancelling thread; cancellation also interrupts active search. */
public final class ValidationControl {
    private final com.ohinteractive.seedv6.training.telemetry.ActiveGameFeed presentation;
    public ValidationControl() { this(null); }
    public ValidationControl(com.ohinteractive.seedv6.training.telemetry.ActiveGameFeed presentation) { this.presentation = presentation; }
    public com.ohinteractive.seedv6.training.telemetry.ActiveGameFeed presentation() { return presentation; }
    private volatile boolean cancelled;
    private SearchControl active;
    private java.util.List<ValidationResult.Pair> savedPairs = java.util.List.of();
    private java.util.List<ValidationResult.Pair> resumePairs = java.util.List.of();
    public java.util.List<ValidationResult.Pair> savedPairs() { return savedPairs; }
    public void savedPairs(java.util.List<ValidationResult.Pair> value) { savedPairs = resumePairs = java.util.List.copyOf(value); }
    java.util.List<ValidationResult.Pair> takeSavedPairs() { var result = resumePairs; resumePairs = java.util.List.of(); return result; }
    void recordPairs(java.util.List<ValidationResult.Pair> value) { savedPairs = java.util.List.copyOf(value); }

    public synchronized void cancel() {
        cancelled = true;
        if (presentation != null) presentation.close();
        if (active != null) active.request(SearchTermination.STOPPED);
    }
    public boolean cancelled() { return cancelled; }

    /** Enter one synchronous fixed-depth search; shared by validation and the read-only strength arena. */
    public synchronized SearchControl beginSearch() {
        if (active != null) throw new IllegalStateException("Control is already in use.");
        active = SearchControl.controlled(-1, System.nanoTime(), -1, TimeSource.SYSTEM);
        if (cancelled) active.request(SearchTermination.STOPPED);
        return active;
    }
    public synchronized void endSearch() { active = null; }
}
