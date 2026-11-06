package com.ohinteractive.seedv6.search.driver;

import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.exact.FlatSearchConfig;
import com.ohinteractive.seedv6.search.manage.ClockPolicy;
import static org.junit.jupiter.api.Assertions.*;

class BaselineProfileTest {
    @Test void historicalSelectorAndQualifiedW4ConfigurationRemainExact() {
        for (int workers = 1; workers <= 16; workers++) {
            var unified = BaselineEngine.configuration(workers, 64, BaselineEngine.SearchProfile.MW_C4);
            var historical = BaselineEngine.configuration(workers, 64, BaselineEngine.SearchProfile.MW_C4_W4);
            assertEquals(1182007295L, unified.mechanisms().flags());
            if (workers == 4) assertEquals(historical, unified);
            else assertEquals(BaselineEngine.configuration(workers, 64, BaselineEngine.SearchProfile.TC_P1), historical);
        }
    }
    @Test void unifiedAdoptionAndRollbackReconstructBothSchedulingAndFlags() throws Exception {
        String property = BaselineEngine.SearchProfile.PROPERTY;
        String previous = System.getProperty(property);
        try {
            System.clearProperty(property);
            var normal = BaselineEngine.load();
            var rollback = BaselineEngine.load(BaselineEngine.Evaluator.CONVENTIONAL_CP64,
                    ClockPolicy.BOUNDARY2, BaselineEngine.SearchProfile.TC_P1);
            System.setProperty(property, "LEGACY");
            for (var binding : new BaselineEngine[]{normal, rollback}) {
                for (int workers : new int[]{4, 1, 2, 3, 4, 5, 8, 16, 4}) {
                    boolean c4 = binding == normal;
                    for (int hash : new int[]{1, 64}) {
                        var rebound = binding.withClockPolicy(hash == 1 ? ClockPolicy.FACTUAL : ClockPolicy.BOUNDARY2);
                        var config = rebound.formulation(workers, hash);
                        assertEquals(c4 ? 3329490943L : 1177812991L, config.search().mechanisms().flags());
                        assertEquals(workers == 1 ? "OWNER" : c4 ? "EAGER_ROOT" : "ROOT_REUSE", config.search().parallelIdentity());
                        assertTrue(config.searchName().startsWith(c4 ? "CLV-C1" : "TC-P1"));
                        var search = rebound.create(workers, hash);
                        try (var driver = new SearchDriver(search)) {
                            Object aspiration = hash == 64 ? field(search, "owner") : search;
                            Object owner = field(aspiration, "owner");
                            if (workers > 1) {
                                assertEquals("ParallelSearch", owner.getClass().getSimpleName());
                                assertEquals(c4, field(owner, "eagerFiniteRoots"));
                                assertEquals("ROOT_REUSE", field(owner, "policy").toString());
                            }
                            var kernel = workers == 1 ? field(owner, "exact") : owner;
                            assertEquals(config.search().mechanisms(), field(kernel, "construction"));
                            assertTrue(driver.search(new SearchRequest(Board.startingPosition(), 3)).targetDepthCompleted());
                            assertEquals(0, driver.activeSearchThreads());
                        }
                    }
                }
            }
        } finally {
            if (previous == null) System.clearProperty(property); else System.setProperty(property, previous);
        }
    }

    @Test void rollbackPropertyIsSnapshottedAndExplicitSelectionWins() throws Exception {
        String property = BaselineEngine.SearchProfile.PROPERTY;
        String previous = System.getProperty(property);
        try {
            System.clearProperty(property);
            var promoted = BaselineEngine.load();
            System.setProperty(property, "legacy");
            var rollback = BaselineEngine.load();
            var explicit = BaselineEngine.load(BaselineEngine.Evaluator.CONVENTIONAL_CP64,
                    ClockPolicy.BOUNDARY2, BaselineEngine.SearchProfile.HL_G1);
            for (int workers = 1; workers <= 16; workers++) {
                assertTrue(promoted.identity(workers, 1).contains("searchProfile=CLV_C1"));
                assertTrue(explicit.identity(workers, 1).contains("flags=104071167"));
                assertTrue(rollback.identity(workers, 1).contains("searchProfile=LEGACY"));
                assertTrue(rollback.withClockPolicy(ClockPolicy.FACTUAL).identity(workers, 1).contains("flags=3407871"));
                assertEquals(3407871, BaselineEngine.configuration(workers, 1).mechanisms().flags());
            }
            System.setProperty(property, "HL_G1");
            // Existing bindings retain their actual kernels after a property/clock-policy change.
            for (var binding : new BaselineEngine[]{promoted, rollback, explicit}) {
                var clock = binding.create(1, 1);
                try (var driver = new SearchDriver(clock)) {
                    Object aspiration = field(clock, "owner");
                    Object kernel = field(field(aspiration, "owner"), "exact");
                    assertEquals(binding == rollback ? 3407871 : binding == promoted ? 3329490943L : 104071167,
                            ((FlatSearchConfig) field(kernel, "construction")).flags());
                    assertTrue(driver.search(new SearchRequest(Board.startingPosition(), 4)).targetDepthCompleted());
                    assertEquals(0, driver.activeSearchThreads());
                }
            }
            System.setProperty(property, "typo");
            assertThrows(IllegalArgumentException.class, BaselineEngine::load);
            assertEquals(3407871, BaselineEngine.legacyConfiguration(4, 1).mechanisms().flags());
            assertTrue(BaselineEngine.load(BaselineEngine.Evaluator.HCE).identity(4, 1).contains("flags=262143"));
            assertNotNull(BaselineEngine.load(BaselineEngine.Evaluator.CONVENTIONAL_CP64,
                    ClockPolicy.FACTUAL, BaselineEngine.SearchProfile.LEGACY));
        } finally {
            if (previous == null) System.clearProperty(property); else System.setProperty(property, previous);
        }
    }
    private static Object field(Object value, String name) throws Exception {
        var field = value.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(value);
    }
}
