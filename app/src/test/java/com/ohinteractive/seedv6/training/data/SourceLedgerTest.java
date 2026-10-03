package com.ohinteractive.seedv6.training.data;

import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SourceLedgerTest {
    @TempDir Path temporary;
    static final String A = "a".repeat(64), B = "b".repeat(64), HASH = "c".repeat(64);
    @Test void independentLineagesSourcesReservationsCompletionAndAbandonment() throws Exception {
        var ledger = new SourceLedger(temporary.resolve("one"));
        var ranges = List.of(new SourceLedger.Range(A, 0, 10, 8, 2, 0), new SourceLedger.Range(B, 0, 5, 4, 1, 0));
        var reserved = ledger.reserve(1, "attempt", "mix", ranges, HASH, HASH);
        var reopened = new SourceLedger(temporary.resolve("one")); assertEquals(reserved, reopened.active().orElseThrow());
        assertEquals(10, reopened.next(A)); assertEquals(5, reopened.next(B));
        assertEquals(0, new SourceLedger(temporary.resolve("two")).next(A));
        var activeLedger = reopened;
        assertThrows(java.io.IOException.class, () -> activeLedger.reserve(2, "another", "mix", ranges, HASH, HASH));
        reopened.complete(1); reopened.complete(1); assertEquals(SourceLedger.Status.COMPLETED, new SourceLedger(temporary.resolve("one")).generation(1).orElseThrow().status());
        reopened.reserve(2, "next", "mix", List.of(new SourceLedger.Range(A, 10, 16, 4, 2, 0)), HASH, HASH);
        SourceLedger.abandonForRestart(temporary.resolve("one"), 2, "restart"); SourceLedger.abandonForRestart(temporary.resolve("one"), 2, "restart");
        reopened = new SourceLedger(temporary.resolve("one")); assertEquals(16, reopened.next(A)); assertEquals(5, reopened.next(B));
        assertEquals(SourceLedger.Status.ABANDONED, reopened.state().reservations().getLast().status());
        Files.writeString(temporary.resolve("one/training-data/cursors.json"), "{}");
        assertThrows(java.io.IOException.class, () -> new SourceLedger(temporary.resolve("one")));
    }
    @Test void failedAtomicPublicationCannotAdvanceInMemoryOrDurableCursor() throws Exception {
        Path root = temporary.resolve("publication"); var ledger = new SourceLedger(root);
        Path target = root.resolve("training-data/cursors.json"); Files.createDirectories(target);
        Files.writeString(target.resolve("blocker"), "preserved");
        assertThrows(java.io.IOException.class, () -> ledger.reserve(1, "attempt", "mix",
                List.of(new SourceLedger.Range(A, 0, 6, 4, 2, 0)), HASH, HASH));
        assertEquals(0, ledger.next(A)); assertTrue(ledger.state().reservations().isEmpty());
        assertEquals("preserved", Files.readString(target.resolve("blocker")));
        try (var files = Files.list(target.getParent())) { assertEquals(1, files.count()); }
    }
    @Test void deterministicWeightedAllocationPreservesConfiguredTieOrder() {
        var sources = new DataSources(1, List.of(new DataSource("A", DataSource.Format.LICHESS_JSONL, "a", A, 8), new DataSource("B", DataSource.Format.LICHESS_JSONL, "b", B, 2)), false);
        assertArrayEquals(new int[]{8000, 2000}, sources.allocate(10000)); assertArrayEquals(new int[]{2, 1}, sources.allocate(3));
        assertArrayEquals(new int[]{0, 0}, sources.allocate(0));
    }
}
