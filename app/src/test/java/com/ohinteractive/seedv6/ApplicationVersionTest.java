package com.ohinteractive.seedv6;

import com.google.gson.JsonParser;
import java.io.*;
import java.math.BigInteger;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ApplicationVersionTest {
    @TempDir Path temp;

    @Test void canonicalNumbersAndEmbeddedDevelopmentIdentityAgree() throws Exception {
        var json = JsonParser.parseString(Files.readString(Path.of(System.getProperty("seedv6.versionState")))).getAsJsonObject();
        assertEquals(Set.of("major", "minor", "patch", "build"), json.keySet());
        for (var field : json.entrySet()) {
            assertTrue(field.getValue().isJsonPrimitive() && field.getValue().getAsJsonPrimitive().isNumber());
            assertTrue(field.getValue().toString().matches("0|[1-9][0-9]*"));
        }
        var loaded = ApplicationVersion.load();
        assertEquals(json.get("major").getAsBigInteger(), loaded.major());
        assertEquals(json.get("minor").getAsBigInteger(), loaded.minor());
        assertEquals(json.get("patch").getAsBigInteger(), loaded.patch());
        assertEquals(json.get("build").getAsBigInteger(), loaded.build());
        assertEquals(loaded.version() + " (build " + loaded.build() + ")", loaded.displayVersion());
        assertEquals("Development run", loaded.desktopBuild());
    }

    @Test void aboutContentCanBeConstructedWithoutAGui() {
        var info = ApplicationVersion.from(version(), new Properties());
        assertEquals("4.5.6 (build 17)", info.displayVersion());
        assertEquals("""
                SeedV6

                Version: 4.5.6
                Build: 17
                Version updated: 2026-09-20
                Source revision: abcdef123456
                Desktop build: Development run
                Platform: Test OS / test-arch""", info.aboutText());
        var desktop = new Properties(); desktop.setProperty("builtAt", "2026-09-26T10:00:00Z");
        assertEquals("2026-09-26T10:00:00Z", ApplicationVersion.from(version(), desktop).desktopBuild());
    }

    @Test void invalidEmbeddedNumbersFailClearly() {
        for (String invalid : List.of("-1", "1.5", "true", "1e3", "")) {
            var values = version(); values.setProperty("build", invalid);
            assertThrows(IllegalStateException.class, () -> ApplicationVersion.from(values, new Properties()));
        }
        var values = version(); values.setProperty("build", "18446744073709551616");
        assertEquals(new BigInteger("18446744073709551616"), ApplicationVersion.from(values, new Properties()).build());
        values.remove("major");
        assertThrows(IllegalStateException.class, () -> ApplicationVersion.from(values, new Properties()));
    }

    @Test void isolatedPackagedJarKeepsItsIdentityWhenCheckoutChanges() throws Exception {
        Path jar = temp.resolve("old-application.jar");
        try (var output = new JarOutputStream(Files.newOutputStream(jar))) {
            String className = "com/ohinteractive/seedv6/ApplicationVersion.class";
            output.putNextEntry(new JarEntry(className));
            try (var input = ApplicationVersion.class.getResourceAsStream("/" + className)) { input.transferTo(output); }
            output.closeEntry();
            put(output, "application-version.properties", version());
            var desktop = new Properties(); desktop.setProperty("builtAt", "2026-09-26T10:00:00Z");
            put(output, "desktop-build.properties", desktop);
        }
        Files.writeString(temp.resolve("VERSION_STATE.txt"), "{\"major\":9,\"minor\":9,\"patch\":9,\"build\":9999}");
        try (var loader = new URLClassLoader(new java.net.URL[]{jar.toUri().toURL()}, null)) {
            Class<?> type = loader.loadClass(ApplicationVersion.class.getName());
            Object loaded = type.getMethod("load").invoke(null);
            assertEquals("4.5.6 (build 17)", type.getMethod("displayVersion").invoke(loaded));
            assertEquals("2026-09-26T10:00:00Z", type.getMethod("desktopBuild").invoke(loaded));
            assertEquals("abcdef123456", type.getMethod("sourceRevision").invoke(loaded));
        }
    }

    private static void put(JarOutputStream output, String name, Properties values) throws IOException {
        output.putNextEntry(new JarEntry("com/ohinteractive/seedv6/" + name));
        var text = new StringWriter(); values.store(text, null);
        output.write(text.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        output.closeEntry();
    }

    private static Properties version() {
        var values = new Properties();
        values.setProperty("major", "4"); values.setProperty("minor", "5");
        values.setProperty("patch", "6"); values.setProperty("build", "17");
        values.setProperty("versionUpdated", "2026-09-20");
        values.setProperty("sourceRevision", "abcdef123456");
        values.setProperty("platform", "Test OS / test-arch");
        return values;
    }
}
