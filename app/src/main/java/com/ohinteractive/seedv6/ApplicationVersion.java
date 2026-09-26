package com.ohinteractive.seedv6;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Properties;

/** Identity of these application resources; never consults an external checkout. */
public record ApplicationVersion(BigInteger major, BigInteger minor, BigInteger patch, BigInteger build,
                                 String versionUpdated, String sourceRevision, String desktopBuild, String platform) {
    private static final String ROOT = "/com/ohinteractive/seedv6/";

    public static ApplicationVersion load() {
        try {
            return from(read("application-version.properties", true), read("desktop-build.properties", false));
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot read embedded application version", failure);
        }
    }

    static ApplicationVersion from(Properties version, Properties desktop) {
        String builtAt = desktop.getProperty("builtAt");
        if (builtAt != null) Instant.parse(builtAt);
        else if (!desktop.isEmpty()) throw new IllegalStateException("Missing desktop build timestamp");
        return new ApplicationVersion(number(version, "major"), number(version, "minor"),
                number(version, "patch"), number(version, "build"),
                required(version, "versionUpdated"), required(version, "sourceRevision"),
                builtAt == null ? "Development run" : builtAt, required(version, "platform"));
    }

    public String version() { return major + "." + minor + "." + patch; }
    public String displayVersion() { return version() + " (build " + build + ")"; }

    public String aboutText() {
        return "SeedV6\n\nVersion: " + version() + "\nBuild: " + build
                + "\nVersion updated: " + versionUpdated + "\nSource revision: " + sourceRevision
                + "\nDesktop build: " + desktopBuild + "\nPlatform: " + platform;
    }

    private static Properties read(String name, boolean required) throws IOException {
        Properties result = new Properties();
        try (InputStream stream = ApplicationVersion.class.getResourceAsStream(ROOT + name)) {
            if (stream == null) {
                if (required) throw new IllegalStateException("Missing embedded " + name + "; build application resources first.");
            } else {
                result.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            }
        }
        return result;
    }

    private static BigInteger number(Properties properties, String key) {
        String value = required(properties, key);
        if (!value.matches("0|[1-9][0-9]*")) throw new IllegalStateException("Invalid embedded version field: " + key);
        return new BigInteger(value);
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing embedded version field: " + key);
        return value;
    }
}
