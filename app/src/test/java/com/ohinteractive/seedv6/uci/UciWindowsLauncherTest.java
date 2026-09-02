package com.ohinteractive.seedv6.uci;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@EnabledOnOs(OS.WINDOWS)
class UciWindowsLauncherTest {

    @Test
    void installedLauncherKeepsStdoutProtocolCleanEvenWithAmbientDebug() throws Exception {
        final Path launcher = Path.of(System.getProperty("seedv6.windowsLauncher"));
        assertTrue(java.nio.file.Files.isRegularFile(launcher), launcher.toString());
        final ProcessBuilder builder = new ProcessBuilder(
            "cmd.exe", "/d", "/c", launcher.toString()
        );
        builder.environment().put("DEBUG", "1");
        final Process process = builder.start();
        try {
            process.getOutputStream().write("uci\nisready\nquit\n".getBytes(StandardCharsets.UTF_8));
            process.getOutputStream().close();
            if(!process.waitFor(10L, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                fail("Installed SeedV6 launcher did not exit within ten seconds.");
            }
            final List<String> stdout = new String(
                process.getInputStream().readAllBytes(), StandardCharsets.UTF_8
            ).lines().toList();
            final String stderr = new String(
                process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8
            );

            assertEquals(0, process.exitValue());
            assertEquals(List.of(
                "id name SeedV6", "id author Charles Clark", "uciok", "readyok"
            ), stdout);
            assertEquals("", stderr);
        } finally {
            if(process.isAlive()) process.destroyForcibly();
        }
    }
}
