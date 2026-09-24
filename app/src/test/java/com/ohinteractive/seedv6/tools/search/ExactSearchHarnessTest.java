package com.ohinteractive.seedv6.tools.search;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExactSearchHarnessTest {
    @Test void namedRunReportsCompletedSearchAndTiming() {
        String output = run("--position=start,mate", "--depth=1", "--warmups=1", "--repetitions=2");
        assertTrue(output.contains("evaluator=HCE threads=1"));
        assertTrue(output.contains("position=start requested=1 completed=1 best="));
        assertTrue(output.contains("position=mate requested=1 completed=1"));
        assertTrue(output.contains("score=32767"));
        for(String field : new String[] {"pv=[", "nodes=", "median_ms=", "nps="}) assertTrue(output.contains(field));
    }

    @Test void arbitraryFenAndZeroDepthAreSupported() {
        String output = run("--fen=7k/6Q1/5K2/8/8/8/8/8 b - - 0 1", "--depth=0", "--warmups=0", "--repetitions=1");
        assertTrue(output.contains("position=fen requested=0 completed=0 best=none score=-32768 pv=[] nodes=1"));
    }

    @Test void invalidOptionsFailRatherThanSilentlyRunningSomethingElse() {
        for(String argument : new String[] {"--position=missing", "--depth=-1", "--depth=257", "--warmups=-1", "--repetitions=0", "--threads=2"}) {
            assertThrows(IllegalArgumentException.class, () -> run(argument));
        }
        assertThrows(IllegalArgumentException.class, () -> run("--position=start", "--fen=unused"));
    }

    private static String run(String... args) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try(PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            ExactSearchHarness.run(args, out);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
