package com.ohinteractive.seedv6.search.driver;

import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.search.exact.ConstructionSearch;
import com.ohinteractive.seedv6.search.exact.FlatSearchConfig;
import com.ohinteractive.seedv6.search.exact.FlatSearchConfig.Feature;
import com.ohinteractive.seedv6.search.manage.ClockPolicy;
import static org.junit.jupiter.api.Assertions.*;

class EngineFormulationTest {
    @Test void defaultAndExplicitProfilesDescribeTheirActualFlags() throws Exception {
        String previous = System.getProperty(BaselineEngine.SearchProfile.PROPERTY);
        try {
            System.clearProperty(BaselineEngine.SearchProfile.PROPERTY);
            var normal = BaselineEngine.load().formulation(1, 64);
            assertEquals(1177812991L, normal.search().mechanisms().flags());
            assertEquals("TC-P1 — HL-G1 + bounded capture ProbCut", normal.searchName());
            assertEquals(BaselineEngine.productionFormulation(), normal.searchName());
            for (var profile : BaselineEngine.SearchProfile.values()) {
                var explicit = BaselineEngine.load(BaselineEngine.Evaluator.CONVENTIONAL_CP64,
                        ClockPolicy.BOUNDARY2, profile).formulation(1, 64);
                assertEquals(profile == BaselineEngine.SearchProfile.TC_P1,
                        explicit.principalMechanisms().contains("ProbCut"));
                assertEquals(profile != BaselineEngine.SearchProfile.LEGACY,
                        explicit.principalMechanisms().contains("retained Search learning"));
                if (profile == BaselineEngine.SearchProfile.TC_P1)
                    assertEquals(normal.configurationId(), explicit.configurationId());
            }
        } finally { restore(previous); }
    }

    @Test void customAndExperimentalFlagsCannotBorrowAName() {
        var guided = BaselineEngine.configuration(1, 64, BaselineEngine.SearchProfile.HL_G1).mechanisms();
        var corrected = guided.with(Feature.EVAL_CORRECTION, true).with(Feature.CORRECTED_QSEARCH, true)
                .with(Feature.RETAINED_CORRECTION, true);
        var custom = formulation(corrected);
        assertEquals("Custom flat PVS configuration", custom.searchName());
        assertTrue(custom.principalMechanisms().contains("retained correction"));
        assertFalse(custom.principalMechanisms().contains("ProbCut"));
        assertTrue(formulation(corrected.with(Feature.TACTICAL_VERIFICATION, true)).principalMechanisms().contains("ProbCut"));
        assertEquals("Custom flat PVS configuration", formulation(new FlatSearchConfig(guided.flags(), 101, 200)).searchName());
        for (var config : new FlatSearchConfig[]{FlatSearchConfig.exact(), FlatSearchConfig.policy4(),
                FlatSearchConfig.ordered(), FlatSearchConfig.candidate(), corrected}) {
            var identity = formulation(config);
            for (Feature feature : Feature.values()) assertEquals(config.has(feature), identity.enabledMechanisms().contains(feature));
            assertFalse(identity.searchName().startsWith("TC-P1"));
        }
    }

    @Test void removingProbcutFromTcP1ReportsHlG1RegardlessOfRequestedLabel() {
        var tc = BaselineEngine.configuration(4, 64, BaselineEngine.SearchProfile.TC_P1);
        var identity = formulation(tc.mechanisms().with(Feature.TACTICAL_VERIFICATION, false));
        assertTrue(identity.searchName().startsWith("HL-G1"));
        assertFalse(identity.summary().contains("ProbCut"));
    }

    @Test void alternativesHaveTheirOwnEvaluatorAndConstructionIdentity() throws Exception {
        var hce = BaselineEngine.load(BaselineEngine.Evaluator.HCE, ClockPolicy.FACTUAL,
                BaselineEngine.SearchProfile.TC_P1).formulation(4, 1);
        var pair = BaselineEngine.load(BaselineEngine.Evaluator.BREP2_32).formulation(4, 1);
        for (var identity : new EngineFormulation[]{hce, pair}) {
            assertEquals(262143, identity.search().mechanisms().flags());
            assertEquals("Construction V1", identity.searchName());
            assertFalse(identity.summary().contains("ProbCut"));
        }
        assertTrue(hce.evaluator().contains("HCE"));
        assertTrue(pair.evaluator().contains("BREP2-32"));
        assertTrue(pair.evaluatorIdentity().contains("networkSha256="));
        assertNotEquals(hce.configurationId(), pair.configurationId());
    }

    @Test void immutableSelectionSurvivesPropertyAndResourceChanges() throws Exception {
        String previous = System.getProperty(BaselineEngine.SearchProfile.PROPERTY);
        try {
            System.setProperty(BaselineEngine.SearchProfile.PROPERTY, "HL_G1");
            var binding = BaselineEngine.load();
            var before = binding.formulation(1, 64);
            System.setProperty(BaselineEngine.SearchProfile.PROPERTY, "TC_P1");
            var after = binding.withClockPolicy(ClockPolicy.FACTUAL).formulation(16, 1);
            assertEquals(before.searchName(), after.searchName());
            assertEquals(before.search().mechanisms(), after.search().mechanisms());
            assertEquals(before.evaluatorIdentity(), after.evaluatorIdentity());
            assertNotEquals(before.configurationId(), after.configurationId());
            assertTrue(after.configurationIdentity().contains("workers=16;hashMiB=1"));
            assertEquals(ClockPolicy.FACTUAL, after.clockPolicy());
            System.setProperty(BaselineEngine.SearchProfile.PROPERTY, "invalid");
            assertThrows(IllegalArgumentException.class, BaselineEngine::load);
            assertEquals(before, binding.formulation(1, 64));
        } finally { restore(previous); }
    }

    private static EngineFormulation formulation(FlatSearchConfig config) {
        return new EngineFormulation(new ConstructionSearch(config, 1, 64, AspirationSearch.Policy.ADAPTIVE16),
                "Test evaluator", "evaluator=test", null);
    }
    private static void restore(String previous) {
        if (previous == null) System.clearProperty(BaselineEngine.SearchProfile.PROPERTY);
        else System.setProperty(BaselineEngine.SearchProfile.PROPERTY, previous);
    }
}
