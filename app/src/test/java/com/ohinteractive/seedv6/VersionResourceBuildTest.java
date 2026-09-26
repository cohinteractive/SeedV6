package com.ohinteractive.seedv6;

import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Real resource/JAR tasks in disposable source archives; no application windows. */
class VersionResourceBuildTest {
    @TempDir Path temp;
    private static final String VALID = "{\n  \"major\": 4,\n  \"minor\": 5,\n  \"patch\": 6,\n  \"build\": 17\n}\n";
    private static final String RESOURCE = "com/ohinteractive/seedv6/application-version.properties";

    private void fixture() throws Exception {
        Files.writeString(temp.resolve("settings.gradle"), "rootProject.name = 'version-fixture'\n");
        Files.copy(Path.of(System.getProperty("seedv6.versionBuildScript")), temp.resolve("version.gradle"));
        Files.writeString(temp.resolve("build.gradle"), "plugins { id 'java' }\napply from: 'version.gradle'\n");
        Files.writeString(temp.resolve("VERSION_STATE.txt"), VALID);
    }

    private GradleRunner runner(String... tasks) {
        var arguments = new ArrayList<>(List.of(tasks));
        arguments.addAll(List.of("--configuration-cache", "--console=plain", "--max-workers=2"));
        return GradleRunner.create().withProjectDir(temp.toFile()).withArguments(arguments);
    }

    @Test void repeatedBuildsEmbedResourcesWithoutMutatingAuthorityOrInventingDates() throws Exception {
        fixture();
        byte[] original = Files.readAllBytes(temp.resolve("VERSION_STATE.txt"));
        runner("build").build();
        Path resource = temp.resolve("build/resources/main").resolve(RESOURCE);
        var properties = new Properties();
        try (var reader = Files.newBufferedReader(resource)) { properties.load(reader); }
        assertEquals("17", properties.getProperty("build"));
        assertEquals("Unknown (Git history unavailable)", properties.getProperty("versionUpdated"));
        assertEquals("Unknown (Git unavailable)", properties.getProperty("sourceRevision"));
        assertFalse(Files.exists(resource.resolveSibling("desktop-build.properties")));
        runner("build").build();
        assertArrayEquals(original, Files.readAllBytes(temp.resolve("VERSION_STATE.txt")));
        try (var jar = new JarFile(temp.resolve("build/libs/version-fixture.jar").toFile())) {
            assertNotNull(jar.getEntry(RESOURCE));
            assertNull(jar.getEntry("com/ohinteractive/seedv6/desktop-build.properties"));
        }
        Files.writeString(temp.resolve("VERSION_STATE.txt"), VALID.replace("\"build\": 17", "\"build\": 18"));
        runner("processResources").build();
        try (var reader = Files.newBufferedReader(resource)) { properties.load(reader); }
        assertEquals("18", properties.getProperty("build"), "Configuration cache must not freeze version inputs");
        try (var oldJar = new JarFile(temp.resolve("build/libs/version-fixture.jar").toFile())) {
            var old = new Properties(); old.load(oldJar.getInputStream(oldJar.getEntry(RESOURCE)));
            assertEquals("17", old.getProperty("build"), "An already-built JAR retains its original identity");
        }
    }

    @Test void invalidCanonicalJsonIsRejectedAndNeverRewritten() throws Exception {
        fixture();
        for (String invalid : List.of(
                VALID.replace("\"build\": 17", "\"build\": -1"),
                VALID.replace("\"build\": 17", "\"build\": true"),
                VALID.replace("\"build\": 17", "\"build\": 1.5"),
                VALID.replace("\"build\": 17", "\"build\": 17, \"build\": 18"),
                VALID.replace("\"build\": 17", "\"other\": 17"))) {
            Files.writeString(temp.resolve("VERSION_STATE.txt"), invalid);
            runner("processResources").buildAndFail();
            assertEquals(invalid, Files.readString(temp.resolve("VERSION_STATE.txt")));
        }
    }

    @Test void gitVersionDateTracksCommittedStateAndMarksUncommittedChanges() throws Exception {
        fixture();
        Files.writeString(temp.resolve(".gitignore"), ".gradle/\nbuild/\n");
        git("init", "-b", "main");
        git("add", ".");
        git("-c", "commit.gpgsign=false", "-c", "core.hooksPath=" + temp.resolve("no-hooks"), "commit", "-m", "Version fixture");
        runner("processResources").build();
        var properties = new Properties();
        Path resource = temp.resolve("build/resources/main").resolve(RESOURCE);
        try (var reader = Files.newBufferedReader(resource)) { properties.load(reader); }
        assertEquals("2026-09-20", properties.getProperty("versionUpdated"));
        assertEquals(git("rev-parse", "--short=12", "HEAD"), properties.getProperty("sourceRevision"));
        String changed = VALID.replace("\"build\": 17", "\"build\": 18446744073709551616");
        Files.writeString(temp.resolve("VERSION_STATE.txt"), changed);
        runner("processResources").build();
        try (var reader = Files.newBufferedReader(resource)) { properties.load(reader); }
        assertEquals("Uncommitted version state (date unavailable)", properties.getProperty("versionUpdated"));
        assertTrue(properties.getProperty("sourceRevision").endsWith(" (uncommitted changes)"));
        assertEquals("18446744073709551616", properties.getProperty("build"));
        assertEquals(changed, Files.readString(temp.resolve("VERSION_STATE.txt")));
    }

    private String git(String... arguments) throws Exception {
        var command = new ArrayList<>(List.of("git")); command.addAll(List.of(arguments));
        var process = new ProcessBuilder(command).directory(temp.toFile()).redirectErrorStream(true);
        process.environment().putAll(Map.of("GIT_AUTHOR_NAME", "Version fixture", "GIT_COMMITTER_NAME", "Version fixture",
                "GIT_AUTHOR_EMAIL", "fixture@example.invalid", "GIT_COMMITTER_EMAIL", "fixture@example.invalid",
                "GIT_AUTHOR_DATE", "2026-09-20T12:00:00Z", "GIT_COMMITTER_DATE", "2026-09-20T12:00:00Z"));
        var child = process.start();
        String output = new String(child.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(0, child.waitFor(), output);
        return output.trim();
    }
}
