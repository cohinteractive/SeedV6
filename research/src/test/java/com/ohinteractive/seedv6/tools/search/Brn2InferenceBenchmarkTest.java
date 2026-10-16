package com.ohinteractive.seedv6.tools.search;

import com.ohinteractive.seedv6.core.brn2.Brn2Model;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Brn2InferenceBenchmarkTest {
    @Test void measuredPathsAgreeOnEveryCorpusChildIncludingRepeatedSiblingUpdates() {
        var workload = Brn2InferenceBenchmark.workload(new Brn2Model(), NnueNetwork.initialized(1));
        assertEquals(233, workload.size());
        assertEquals(7, workload.operations().size());
        for (int round = 0; round < 2; round++) for (int i = 0; i < workload.size(); i++) {
            double full = workload.operations().get(0).applyAsDouble(i);
            for (int op = 1; op < 4; op++) assertEquals(full, workload.operations().get(op).applyAsDouble(i), 1e-12);
            double nnueFull = workload.operations().get(4).applyAsDouble(i);
            for (int op = 5; op < 7; op++) assertEquals(nnueFull, workload.operations().get(op).applyAsDouble(i), 1e-6);
        }
    }
}
