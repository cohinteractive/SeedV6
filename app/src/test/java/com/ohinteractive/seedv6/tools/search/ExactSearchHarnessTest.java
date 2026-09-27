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
        assertTrue(output.contains("ordering=CONTROL"));
        for(String field : new String[] {"pv=[", "nodes=", "median_ms=", "nps="}) assertTrue(output.contains(field));
    }

    @Test void arbitraryFenAndZeroDepthAreSupported() {
        String output = run("--fen=7k/6Q1/5K2/8/8/8/8/8 b - - 0 1", "--depth=0", "--warmups=0", "--repetitions=1");
        assertTrue(output.contains("position=fen requested=0 completed=0 best=none score=-32768 pv=[] nodes=1"));
    }

    @Test void explicitTtModesAreRepeatableAndCold() {
        for(String mode : new String[] {"off", "on"}) {
            String output = run("--tt=" + mode, "--position=start,mate", "--depth=4", "--warmups=1", "--repetitions=2");
            assertTrue(output.contains("tt=" + mode));
            assertTrue(output.contains("best=b1c3 score=-4"));
            assertTrue(output.contains("best=g6g7 score=32767"));
            assertTrue(output.contains(mode.equals("on") ? "cold/cleared" : "table=none"));
        }
        assertThrows(IllegalArgumentException.class, () -> run("--tt=maybe"));
    }

    @Test void invalidOptionsFailRatherThanSilentlyRunningSomethingElse() {
        for(String argument : new String[] {"--position=missing", "--depth=-1", "--depth=257", "--warmups=-1", "--repetitions=0", "--threads=2"}) {
            assertThrows(IllegalArgumentException.class, () -> run(argument));
        }
        assertThrows(IllegalArgumentException.class, () -> run("--position=start", "--fen=unused"));
        assertThrows(IllegalArgumentException.class, () -> run("--ordering=unknown"));
    }

    @Test void pairedOrderingExperimentReportsBothModesAndVerifiesValues() {
        for(String tt : new String[] {"off", "on"}) {
            String output = run("--position=ordering", "--depth=2", "--warmups=1", "--repetitions=2", "--ordering=both", "--tt=" + tt);
            assertEquals(6, ExactSearchHarness.orderingPositions().size());
            assertEquals(6, output.split("ordering=CONTROL", -1).length - 1);
            assertEquals(6, output.split("ordering=SEE_TIERED", -1).length - 1);
            assertTrue(output.contains("comparison aggregate depth=2 positions=6"));
        }
        assertTrue(run("--position=mate", "--depth=2", "--warmups=0", "--repetitions=1", "--ordering=see-tiered")
                .contains("ordering=SEE_TIERED"));
    }

    @Test void tacticalOnlyComparisonSelectsTheNewCandidateWithoutRedefiningBoth() {
        for(String tt : new String[] {"off", "on"}) {
            String output = run("--position=ordering", "--depth=2", "--warmups=1", "--repetitions=2",
                    "--ordering=control,see-tactical", "--tt=" + tt);
            assertEquals(6, output.split("ordering=CONTROL", -1).length - 1);
            assertEquals(6, output.split("ordering=SEE_TACTICAL", -1).length - 1);
            assertFalse(output.contains("ordering=SEE_TIERED"));
            assertTrue(output.contains("comparison aggregate depth=2 positions=6"));
        }
        assertTrue(run("--position=mate", "--depth=2", "--warmups=0", "--repetitions=1", "--ordering=see-tactical")
                .contains("ordering=SEE_TACTICAL"));
    }

    @Test void materialComparisonRotatesThreeModesAndReportsEachAgainstTacticalBaseline() {
        for(String tt : new String[] {"off", "on"}) {
            String output = run("--position=ordering", "--depth=2", "--warmups=1", "--repetitions=2",
                    "--ordering=see-tactical,see-material,see-material-lva", "--tt=" + tt);
            for(String name : new String[] {"SEE_TACTICAL", "SEE_MATERIAL", "SEE_MATERIAL_LVA"})
                assertEquals(6, output.lines().filter(l -> l.endsWith("ordering=" + name)).count());
            assertEquals(2, output.lines().filter(l -> l.startsWith("comparison aggregate depth=2 positions=6")
                    && l.contains("baseline=SEE_TACTICAL")).count());
        }
        for(String mode : new String[] {"see-material", "see-material-lva"})
            assertTrue(run("--position=mate", "--depth=2", "--warmups=0", "--repetitions=1", "--ordering=" + mode)
                    .contains("ordering=" + mode.toUpperCase().replace('-', '_')));
    }

    private static String run(String... args) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try(PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            ExactSearchHarness.run(args, out);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
