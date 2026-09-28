package com.ohinteractive.seedv6.tools.search;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExactSearchHarnessTest {
    @Test void quiescenceComparisonReportsSeparateTreesAndAllowsSemanticDifferences() {
        String output = run("--leaves=both", "--position=start,tactical,evasion", "--depth=1",
                "--warmups=1", "--repetitions=2", "--tt=off");
        assertEquals(6, output.lines().filter(l -> l.startsWith("qsearch ")).count());
        assertTrue(output.contains("ordering=QSEARCH_BASELINE"));
        assertTrue(output.contains("ordering=CONTROL"));
        for(String field : new String[] {"normal_nodes=", "qnodes=", "total_nodes=", "max_qply=", "completed_normally=true"})
            assertTrue(output.contains(field));
        String limited = run("--leaves=qsearch", "--position=tactical", "--depth=0",
                "--node-limit=2", "--warmups=0", "--repetitions=2");
        assertTrue(limited.contains("completed=-1 best=none"));
        assertTrue(limited.contains("completed_normally=false"));
        for(String arg : new String[] {"--tt=on", "--search=pvs", "--ordering=see-material",
                "--mechanics=staged-lazy", "--frames=both", "--node-limit=0"})
            assertThrows(IllegalArgumentException.class, () -> run("--leaves=both", arg));
        assertThrows(IllegalArgumentException.class, () -> run("--leaves=unknown"));
        assertThrows(IllegalArgumentException.class, () -> run("--node-limit=2"));
    }

    @Test void flatComparisonChecksExactTreesAndReportsNoiseAllocationAndGc() {
        String output=run("--frames=both","--position=start,mate","--depths=1,2",
                "--warmups=1","--repetitions=3","--tt-mib=1");
        assertEquals(4,output.lines().filter(l->l.startsWith("equivalence ")).count());
        assertEquals(8,output.lines().filter(l->l.startsWith("sample ")).count());
        for(String field : new String[] {"same_pv=true","oracle=true","every_pv_prefix=true",
                "identical_nodes=","wall_ratio=","throughput_ratio=","q1_ms=","q3_ms=",
                "allocated_bytes_median=","gc=","compilation_ms=","paired_median=","aggregate cases=4"})
            assertTrue(output.contains(field),field);
        assertThrows(IllegalArgumentException.class,()->run("--frames=both","--tt=off"));
        assertThrows(IllegalArgumentException.class,()->run("--frames=both","--search=alpha-beta"));
        assertThrows(IllegalArgumentException.class,()->run("--frames=both","--depths=257"));
        assertThrows(IllegalArgumentException.class,()->run("--frames=both","--tt-mib=0"));
    }

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
            assertTrue(output.contains(mode.equals("on") ? "ordering=SEE_MATERIAL_QUIET_HISTORY/STAGED_LAZY/PVS" : "ordering=CONTROL"));
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

    @Test void captureHistoryComparisonKeepsMaterialAsBaselineAndResetsRepeatedSearches() {
        for(String tt : new String[] {"off", "on"}) {
            String output = run("--position=ordering", "--depth=3", "--warmups=1", "--repetitions=2",
                    "--ordering=see-material,see-material-history", "--tt=" + tt);
            for(String name : new String[] {"SEE_MATERIAL", "SEE_MATERIAL_CAPTURE_HISTORY"})
                assertEquals(6, output.lines().filter(l -> l.endsWith("ordering=" + name)).count());
            assertTrue(output.contains("baseline=SEE_MATERIAL candidate=SEE_MATERIAL_CAPTURE_HISTORY"));
        }
        assertTrue(run("--position=mate", "--depth=2", "--warmups=0", "--repetitions=1", "--ordering=see-material-history")
                .contains("ordering=SEE_MATERIAL_CAPTURE_HISTORY"));
    }

    @Test void quietHistoryComparisonIsIndependentAndStartsEachRepetitionEmpty() {
        for(String tt : new String[] {"off", "on"}) {
            String output = run("--position=ordering", "--depth=3", "--warmups=1", "--repetitions=2",
                    "--ordering=see-material,see-material-quiet-history", "--tt=" + tt);
            for(String name : new String[] {"SEE_MATERIAL", "SEE_MATERIAL_QUIET_HISTORY"})
                assertEquals(6, output.lines().filter(l -> l.endsWith("ordering=" + name)).count());
            assertTrue(output.contains("baseline=SEE_MATERIAL candidate=SEE_MATERIAL_QUIET_HISTORY"));
            assertFalse(output.contains("ordering=SEE_MATERIAL_CAPTURE_HISTORY"));
        }
        assertTrue(run("--position=mate", "--depth=2", "--warmups=0", "--repetitions=1", "--ordering=see-material-quiet-history")
                .contains("ordering=SEE_MATERIAL_QUIET_HISTORY"));
    }

    @Test void continuationPairUsesMainHistoryBaselineAndRemainsIndependentlySelectable() {
        for(String tt : new String[] {"off", "on"}) {
            String output = run("--position=ordering", "--depth=3", "--warmups=1", "--repetitions=2",
                    "--ordering=see-material-quiet-history,see-material-continuation-history", "--tt=" + tt);
            for(String name : new String[] {"SEE_MATERIAL_QUIET_HISTORY", "SEE_MATERIAL_CONTINUATION_HISTORY"})
                assertEquals(6, output.lines().filter(l -> l.endsWith("ordering=" + name)).count());
            assertTrue(output.contains("baseline=SEE_MATERIAL_QUIET_HISTORY candidate=SEE_MATERIAL_CONTINUATION_HISTORY"));
        }
        assertTrue(run("--position=mate", "--depth=2", "--warmups=0", "--repetitions=1", "--ordering=see-material-continuation-history")
                .contains("ordering=SEE_MATERIAL_CONTINUATION_HISTORY"));
    }

    @Test void killerPairUsesMainHistoryWithoutContinuationAndIsIndependentlySelectable() {
        for(String tt : new String[] {"off", "on"}) {
            String output = run("--position=ordering", "--depth=3", "--warmups=1", "--repetitions=2",
                    "--ordering=see-material-quiet-history,see-material-quiet-history-killers", "--tt=" + tt);
            for(String name : new String[] {"SEE_MATERIAL_QUIET_HISTORY", "SEE_MATERIAL_QUIET_HISTORY_KILLERS"})
                assertEquals(6, output.lines().filter(l -> l.endsWith("ordering=" + name)).count());
            assertTrue(output.contains("baseline=SEE_MATERIAL_QUIET_HISTORY candidate=SEE_MATERIAL_QUIET_HISTORY_KILLERS"));
            assertFalse(output.contains("ordering=SEE_MATERIAL_CONTINUATION_HISTORY"));
        }
        assertTrue(run("--position=mate", "--depth=2", "--warmups=0", "--repetitions=1", "--ordering=see-material-quiet-history-killers")
                .contains("ordering=SEE_MATERIAL_QUIET_HISTORY_KILLERS"));
    }

    @Test void countermovePairUsesMainHistoryAndRemainsIndependentlySelectable() {
        for(String tt : new String[] {"off", "on"}) {
            String output = run("--position=ordering", "--depth=3", "--warmups=1", "--repetitions=2",
                    "--ordering=see-material-quiet-history,see-material-quiet-history-countermove", "--tt=" + tt);
            for(String name : new String[] {"SEE_MATERIAL_QUIET_HISTORY", "SEE_MATERIAL_QUIET_HISTORY_COUNTERMOVE"})
                assertEquals(6, output.lines().filter(l -> l.endsWith("ordering=" + name)).count());
            assertTrue(output.contains("baseline=SEE_MATERIAL_QUIET_HISTORY candidate=SEE_MATERIAL_QUIET_HISTORY_COUNTERMOVE"));
            assertFalse(output.contains("ordering=SEE_MATERIAL_CONTINUATION_HISTORY"));
            assertFalse(output.contains("ordering=SEE_MATERIAL_QUIET_HISTORY_KILLERS"));
        }
        assertTrue(run("--position=mate", "--depth=2", "--warmups=0", "--repetitions=1", "--ordering=see-material-quiet-history-countermove")
                .contains("ordering=SEE_MATERIAL_QUIET_HISTORY_COUNTERMOVE"));
    }

    @Test void mechanicsRequireExactIdentityAndDoNotReportNodeChangeAsImprovement() {
        for(String tt : new String[] {"off", "on"}) {
            String output = run("--position=ordering", "--depth=3", "--warmups=1", "--repetitions=2",
                    "--ordering=see-material-quiet-history", "--mechanics=all", "--tt=" + tt);
            for(String name : new String[] {"CURRENT_INSERTION", "HANDCRAFTED_FULL_SORT_24", "LAZY_SELECTION"})
                assertEquals(6, output.lines().filter(l -> l.endsWith("ordering=" + name)).count());
            assertFalse(output.contains("nodes_pct=")); assertTrue(output.contains("identical_nodes="));
            assertEquals(2, output.lines().filter(l -> l.startsWith("comparison aggregate")).count());
        }
        String thresholds = run("--position=start", "--depth=2", "--warmups=0", "--repetitions=1",
                "--ordering=see-material-quiet-history", "--mechanics=current-insertion,handcrafted-sort", "--sort-crossovers=8,16,24,32,512");
        for(int threshold : new int[] {8, 16, 24, 32, 512}) assertTrue(thresholds.contains("ordering=HANDCRAFTED_FULL_SORT_" + threshold));
        assertThrows(IllegalArgumentException.class, () -> run("--mechanics=all"));
        assertThrows(IllegalArgumentException.class, () -> run("--ordering=see-material-quiet-history", "--mechanics=unknown"));
        assertThrows(IllegalArgumentException.class, () -> run("--sort-crossovers=0"));
    }

    @Test void stagingSeparatesIdenticalLeafTreesFromDeferredHistoryTrees() {
        for(String tt : new String[] {"off", "on"}) {
            String output = run("--position=ordering", "--depth=3", "--warmups=1", "--repetitions=2",
                    "--ordering=see-material-quiet-history", "--mechanics=staging", "--tt=" + tt);
            for(String mode : new String[] {"FULL_LAZY", "LEAF_STAGED_LAZY", "STAGED_LAZY"})
                assertEquals(6, output.lines().filter(l -> l.endsWith("ordering=" + mode)).count());
            assertEquals(3, output.lines().filter(l -> l.startsWith("comparison aggregate")).count());
            assertTrue(output.lines().anyMatch(l -> l.contains("baseline=FULL_LAZY candidate=LEAF_STAGED_LAZY identical_nodes=")));
            assertTrue(output.lines().anyMatch(l -> l.contains("baseline=LEAF_STAGED_LAZY candidate=STAGED_LAZY nodes_pct=")));
        }
        for(String mode : new String[] {"full-lazy", "leaf-staged-lazy", "staged-lazy"})
            assertTrue(run("--position=start", "--depth=1", "--warmups=0", "--repetitions=1",
                    "--ordering=see-material-quiet-history", "--mechanics=" + mode).contains("ordering=" + mode.toUpperCase().replace('-', '_')));
    }

    @Test void pvsTraversalIsOrthogonalAndPairedVerificationAllowsNodeChanges() {
        for(String tt : new String[] {"off","on"}) {
            String output=run("--position=ordering","--depth=3","--warmups=1","--repetitions=2",
                    "--ordering=see-material-quiet-history","--mechanics=staged-lazy","--search=both","--tt="+tt);
            for(String mode : new String[] {"ORDERED_ALPHA_BETA","PVS"})
                assertEquals(6,output.lines().filter(l->l.endsWith("ordering=STAGED_LAZY/"+mode)).count());
            assertTrue(output.contains("nodes_pct="));
            assertEquals(6,output.lines().filter(l->l.startsWith("semantics ") && l.endsWith("every_pv_prefix_verified=true")).count());
        }
        for(String mode : new String[] {"alpha-beta","pvs"})
            assertTrue(run("--position=start","--depth=1","--warmups=0","--repetitions=1","--search="+mode)
                    .contains("ordering=CONTROL/"+(mode.equals("pvs") ? "PVS" : "ORDERED_ALPHA_BETA")));
        assertThrows(IllegalArgumentException.class,()->run("--search=unknown"));
        assertThrows(IllegalArgumentException.class,()->run("--search=both","--ordering=both"));
    }

    private static String run(String... args) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try(PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            ExactSearchHarness.run(args, out);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
