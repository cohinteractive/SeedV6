package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.training.data.DataFiles;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class BrnArchitectureQualityTest {
    @TempDir Path temp;
    @Test @SuppressWarnings("unchecked") void expectedScoreBinsIncludeBothExtremesAndWeightByPopulation() {
        var calibration = new BrnArchitectureQuality.Reliability();
        calibration.add(-1, -1); calibration.add(0, 1); calibration.add(0, -1); calibration.add(1, 0);
        var report = calibration.report(); var bins = (List<Map<String,Object>>) report.get("bins");
        assertEquals(4, report.get("positions")); assertEquals(1, bins.get(0).get("positions"));
        assertEquals(2, bins.get(5).get("positions")); assertEquals(1, bins.get(9).get("positions"));
        assertEquals(.5, bins.get(5).get("meanPredictedExpectedScore"));
        assertEquals(.5, bins.get(5).get("meanTeacherExpectedScore"));
        assertEquals(.125, (double) report.get("weightedAbsoluteBinGap"), 0);
        assertThrows(IllegalArgumentException.class, () -> calibration.add(Double.NaN, 0));
        assertThrows(IllegalArgumentException.class, () -> calibration.add(0, 1.01));
        assertThrows(IllegalStateException.class, () -> new BrnArchitectureQuality.Reliability().report());
    }
    @Test void alteredOrUnfrozenPlanFailsBeforeAccessingDatasets() throws Exception {
        Path plan = temp.resolve("plan.json");
        DataFiles.write(plan, Map.of("schema", "brn-architecture-quality-plan-v1", "finalDecisionsFrozen", false,
                "entries", Map.of("missing", Map.of("partition", "test", "run", "nonexistent", "data", "nonexistent"))));
        String hash = BrnResearchComparison.digest(plan);
        var failure = assertThrows(IOException.class, () -> BrnArchitectureQuality.entry(plan, hash, "missing", "sealed-final-v1"));
        assertTrue(failure.getMessage().contains("decisions"));
        failure = assertThrows(IOException.class, () -> BrnArchitectureQuality.entry(plan, "altered", "missing", "validation-v1"));
        assertTrue(failure.getMessage().contains("changed"));
        failure = assertThrows(IOException.class, () -> BrnArchitectureQuality.entry(plan, hash, "missing", "validation-v1"));
        assertTrue(failure.getMessage().contains("Partition"));
    }
}
