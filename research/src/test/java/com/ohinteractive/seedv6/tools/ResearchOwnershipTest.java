package com.ohinteractive.seedv6.tools;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResearchOwnershipTest {
    @Test void referencesAndCorpusLoadWithoutApplications() throws Exception {
        var loader = getClass().getClassLoader();
        for (String name : new String[] {"Main", "gui.SwingLauncher", "uci.UciEngine", "uci.UciMain",
                "uci.LegacyUciEvaluation", "integration.ResearchUciMain"})
            assertThrows(ClassNotFoundException.class, () -> loader.loadClass("com.ohinteractive.seedv6." + name));
        for (String name : new String[] {"search.flat.FlatNegamax", "search.exact.FlatExactSearch",
                "search.exact.ExhaustiveOracle", "search.exact.QuiescenceOracle", "core.SeeLegalOracle",
                "training.service.BrnArchitectureNativeReference", "uci.UciEvaluation"})
            assertNotNull(loader.loadClass("com.ohinteractive.seedv6." + name));
        try (var corpus = loader.getResourceAsStream("com/ohinteractive/seedv6/tools/eval/opt5-m1-eval-corpus-v1.b64")) {
            assertNotNull(corpus);
            assertTrue(corpus.readAllBytes().length > 0);
        }
    }
}
