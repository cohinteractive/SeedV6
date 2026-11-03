package com.ohinteractive.seedv6.uci;

import java.io.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.search.driver.BaselineEngine;
import com.ohinteractive.seedv6.search.manage.ClockPolicy;
import static org.junit.jupiter.api.Assertions.*;

class FormulationUciTest {
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
                assertTrue(text.contains("id name SeedV6 Construction V1 NNUE CP64 "));
                assertEquals(2, text.lines().filter(s -> s.equals("uciok")).count());
                assertEquals(1, text.lines().filter(s -> s.equals("readyok")).count());
                assertEquals(2, text.lines().filter(s -> s.startsWith("info string formulation snapshot:")).count());
                assertTrue(text.contains(core.configurationId()));
                var changed = configuration.withClockPolicy(ClockPolicy.FACTUAL).formulation(4, 1);
                assertTrue(text.contains(changed.configurationId()));
                assertEquals(core.searchName(), changed.searchName());
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
