package com.ohinteractive.seedv6.uci;

import java.io.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.ApplicationVersion;
import com.ohinteractive.seedv6.search.driver.BaselineEngine;
import com.ohinteractive.seedv6.search.manage.ClockPolicy;
import static org.junit.jupiter.api.Assertions.*;

class FormulationUciTest {
    @Test void inspectionButtonReportsCurrentBindingWithoutAnotherHandshakeOrReconstruction() throws Exception {
        String previous = System.getProperty(BaselineEngine.SearchProfile.PROPERTY);
        try {
            System.clearProperty(BaselineEngine.SearchProfile.PROPERTY);
            var configuration = BaselineUciConfiguration.load();
            System.setProperty(BaselineEngine.SearchProfile.PROPERTY, "LEGACY");
            var commands = new StringBuilder("uci\n");
            for (int workers : new int[]{1, 2, 3, 4, 5, 8, 16}) {
                commands.append("setoption name Threads value ").append(workers).append("\n");
                commands.append("setoption name Hash value 1\nsetoption name ClockPolicy value FACTUAL\n");
                commands.append("setoption name Show Search Configuration\nisready\n");
            }
            commands.append("quit\n");
            var output = new ByteArrayOutputStream(); var error = new ByteArrayOutputStream();
            try (var engine = new UciEngine(new ByteArrayInputStream(commands.toString().getBytes(StandardCharsets.UTF_8)),
                    output, error, configuration)) { engine.run(); }
            String text = output.toString(StandardCharsets.UTF_8);
            assertEquals("", error.toString(StandardCharsets.UTF_8));
            assertTrue(text.contains("option name Show Search Configuration type button"));
            assertEquals(1, text.lines().filter(s -> s.startsWith("id name SeedV6-")).count());
            assertEquals(8, text.lines().filter(s -> s.startsWith("info string formulation snapshot:")).count());
            var identities = text.lines().filter(s -> s.contains("searchProfile=")).toList();
            assertEquals(8, identities.size());
            for (int workers : new int[]{1, 2, 3, 4, 5, 8, 16}) {
                String identity = identities.stream().filter(s -> s.contains("workers=" + workers + ";hashMiB=1;")).findFirst().orElseThrow();
                assertTrue(identity.contains("searchProfile=CLV_C1 "));
                assertTrue(identity.contains("flags=3329490943"));
                assertTrue(identity.contains("parallel=" + (workers == 1 ? "OWNER" : "EAGER_ROOT")));
                assertTrue(identity.contains("evaluator=CONVENTIONAL_NNUE checkpoint=64"));
                assertTrue(identity.contains("clockPolicy=" + ClockPolicy.FACTUAL.identity()));
                assertTrue(text.contains(configuration.withClockPolicy(ClockPolicy.FACTUAL).formulation(workers, 1).configurationId()));
            }
        } finally {
            if (previous == null) System.clearProperty(BaselineEngine.SearchProfile.PROPERTY);
            else System.setProperty(BaselineEngine.SearchProfile.PROPERTY, previous);
        }
    }
    @Test void handshakeKeepsNameAndOptionsAndReportsCurrentSnapshot() throws Exception {
        String previous = System.getProperty(BaselineEngine.SearchProfile.PROPERTY);
        try {
            for (var profile : BaselineEngine.SearchProfile.values()) {
                System.setProperty(BaselineEngine.SearchProfile.PROPERTY, profile.name());
                var configuration = BaselineUciConfiguration.load();
                var core = BaselineEngine.load().formulation(1, 64);
                assertEquals(core, configuration.formulation(1, 64));
                // Changing the property cannot change an existing binding during reconstruction.
                System.setProperty(BaselineEngine.SearchProfile.PROPERTY, "TC_P1");
                String commands = "uci\nsetoption name Threads value 4\nsetoption name Hash value 1\n"
                        + "setoption name ClockPolicy value FACTUAL\nisready\nuci\nquit\n";
                var output = new ByteArrayOutputStream(); var error = new ByteArrayOutputStream();
                try (var engine = new UciEngine(new ByteArrayInputStream(commands.getBytes(StandardCharsets.UTF_8)),
                        output, error, configuration)) { engine.run(); }
                String text = output.toString(StandardCharsets.UTF_8);
                assertEquals("", error.toString(StandardCharsets.UTF_8));
                String expectedName = "id name SeedV6-" + ApplicationVersion.load().build();
                assertEquals(java.util.List.of(expectedName, expectedName),
                        text.lines().filter(s -> s.startsWith("id name ")).toList());
                assertEquals(2, text.lines().filter(s -> s.equals("uciok")).count());
                assertEquals(1, text.lines().filter(s -> s.equals("readyok")).count());
                assertEquals(2, text.lines().filter(s -> s.startsWith("info string formulation snapshot:")).count());
                assertTrue(text.contains(core.configurationId()));
                var changed = configuration.withClockPolicy(ClockPolicy.FACTUAL).formulation(4, 1);
                assertTrue(text.contains(changed.configurationId()));
                if (profile == BaselineEngine.SearchProfile.MW_C4_W4) {
                    assertTrue(changed.searchName().startsWith("MW-C4"));
                    assertTrue(text.contains("parallel=EAGER_ROOT"));
                } else assertEquals(core.searchName(), changed.searchName());
                assertTrue(text.contains("flags=" + core.search().mechanisms().flags()));
                assertTrue(text.contains("option name Threads type spin default 1 min 1 max 16"));
                assertTrue(text.contains("option name Hash type spin default 64 min 1 max 1024"));
                assertTrue(text.lines().allMatch(s -> s.startsWith("id ") || s.startsWith("option ")
                        || s.startsWith("info string ") || s.equals("uciok") || s.equals("readyok")));
            }
        } finally {
            if (previous == null) System.clearProperty(BaselineEngine.SearchProfile.PROPERTY);
            else System.setProperty(BaselineEngine.SearchProfile.PROPERTY, previous);
        }
    }
}
