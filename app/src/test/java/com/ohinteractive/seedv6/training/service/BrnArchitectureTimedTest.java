package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.training.data.DataFiles;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class BrnArchitectureTimedTest {
    @TempDir Path temp;

    static List<BrnResearchData.Example> examples(int size) {
        var result = new ArrayList<BrnResearchData.Example>();
        for (int i = 0; i < size; i++) result.add(new BrnResearchData.Example(Board.startingPosition(), i - 65, 78, 0));
        return result;
    }
    @Test void budgetStopsAtBatchBoundaryAndPreservesExactShuffledCursor() {
        var data = examples(130); var cursor = new BrnArchitectureTimed.Cursor(130, 71);
        int[] first = BrnResearchMain.order(130, 71); var seen = new ArrayList<Double>();
        var sizes = new ArrayList<Integer>(); var clock = new AtomicLong();
        BrnArchitectureTimed.Batch trainer = (boards, targets, size) -> {
            sizes.add(size); for (int j = 0; j < size; j++) seen.add(targets[j]);
        };
        var timing = BrnArchitectureTimed.trainTo(trainer, cursor, data, 0, 5, () -> clock.addAndGet(3));
        assertEquals(6, timing.nanos()); assertEquals(3, timing.maximumBatchNanos()); assertEquals(2, timing.batches());
        assertEquals(List.of(128, 2), sizes); assertEquals(130, cursor.samples);
        assertEquals(2, cursor.epoch); assertEquals(0, cursor.offset);
        for (int i = 0; i < first.length; i++) assertEquals(data.get(first[i]).outcome(), seen.get(i), 0);
        var copy = cursor.copy();
        timing = BrnArchitectureTimed.trainTo(trainer, cursor, data, timing.nanos(), 7, () -> clock.addAndGet(3));
        assertEquals(9, timing.nanos()); assertEquals(258, cursor.samples); assertEquals(128, cursor.offset);
        assertEquals(130, copy.samples); assertEquals(0, copy.offset);
        assertArrayEquals(BrnResearchMain.order(130, 72), cursor.order);
        assertThrows(IllegalStateException.class, () -> BrnArchitectureTimed.trainTo(trainer, copy, data, 0, 2, () -> 10));
        assertThrows(IllegalArgumentException.class, () -> BrnArchitectureTimed.trainTo(trainer, copy, data, 3, 2, () -> 10));
        assertArrayEquals(new double[]{15, 30, 60, 120}, BrnArchitectureTimed.budgets("15,30,60,120"));
        for (String invalid : List.of("", "0", "NaN", "Infinity", "1,1", "2,1", "1201", "1,"))
            assertThrows(IllegalArgumentException.class, () -> BrnArchitectureTimed.budgets(invalid));
    }

    @Test void everyAdapterHasExactScheduledResumeAndLoadableModel() throws Exception {
        var data = examples(128);
        for (String family : BrnArchitectureTimed.FAMILIES) {
            var learner = new BrnArchitectureTimed.Learner(family, 71);
            var cursor = new BrnArchitectureTimed.Cursor(128, 71);
            long[][] boards = new long[128][]; double[] targets = new double[128];
            int size = cursor.fill(data, boards, targets); learner.train(boards, targets, size); cursor.advance(size);
            Path out = temp.resolve(family); Files.createDirectory(out); learner.save(out, "selected");
            String hash = BrnResearchComparison.digest(out.resolve("selected.model"));
            DataFiles.write(out.resolve("result.json"), Map.of("kind", learner.kind(), "complete", true, "selectedModelSha256", hash));
            double before = learner.view().pawns().applyAsDouble(boards[0]);
            BrnArchitectureTimed.verifyResume(learner, out, cursor, data);
            assertEquals(1, learner.step(), family); assertEquals(128, cursor.samples, family);
            assertEquals(before, learner.view().pawns().applyAsDouble(boards[0]), 0, family);
            var loaded = BrnArchitectureControls.loadView(out);
            assertEquals(before, loaded.pawns().applyAsDouble(boards[0]), 0, family);
            learner.save(out, "after-restore");
            assertEquals(-1, Files.mismatch(out.resolve("selected.state"), out.resolve("after-restore.state")), family);
            assertEquals(hash, BrnResearchComparison.digest(out.resolve("after-restore.model")), family);
        }
    }

    @Test void logicTerminalRefitDoesNotChangeTheContinuingPrefix() throws Exception {
        var boards = new long[][]{Board.startingPosition()}; var targets = new double[]{.4};
        for (String family : List.of("logic", "logic-fixed")) {
            var prefix = new BrnArchitectureTimed.Learner(family, 71); prefix.train(boards, targets, 1);
            Path out = temp.resolve(family); Files.createDirectory(out); prefix.save(out, "prefix");
            var branch = new BrnArchitectureTimed.Learner(family, 71); branch.restore(out.resolve("prefix.state"));
            branch.finalPhase(); assertTrue(branch.logic.hardened()); assertEquals(.0003, branch.logic.hardRate(), 0);
            assertEquals(family.equals("logic-fixed"), prefix.logic.hardened());
            if (family.equals("logic-fixed")) assertEquals(.003, prefix.logic.hardRate(), 0);
            branch.train(boards, targets, 1); prefix.save(out, "unchanged");
            assertEquals(-1, Files.mismatch(out.resolve("prefix.state"), out.resolve("unchanged.state")));
            assertEquals(1, prefix.step()); assertEquals(2, branch.step());
        }
    }
}
