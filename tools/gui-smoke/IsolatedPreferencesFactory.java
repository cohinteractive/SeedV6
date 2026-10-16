package com.ohinteractive.seedv6.validation;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.prefs.*;

/** Opt-in test/desktop fixture; never reads or writes the host preference registry. */
public final class IsolatedPreferencesFactory implements PreferencesFactory {
    private static final Path FILE = Path.of(Objects.requireNonNull(
            System.getProperty("seedv6.validation.preferences"), "Explicit disposable preferences file required"));
    private static final Properties DATA = new Properties();
    static {
        try {
            if (Files.exists(FILE)) try (var input = Files.newInputStream(FILE)) { DATA.load(input); }
        } catch (IOException error) { throw new ExceptionInInitializerError(error); }
    }
    private final Preferences user = new Node(null, "", "user");
    private final Preferences system = new Node(null, "", "system");
    public Preferences userRoot() { return user; }
    public Preferences systemRoot() { return system; }

    private static synchronized void save() {
        try {
            Files.createDirectories(FILE.toAbsolutePath().getParent());
            Path pending = FILE.resolveSibling(FILE.getFileName() + ".pending");
            try (var output = Files.newOutputStream(pending)) { DATA.store(output, "Disposable SeedV6 GUI validation only"); }
            Files.move(pending, FILE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException error) { throw new UncheckedIOException(error); }
    }
    private static final class Node extends AbstractPreferences {
        private final String kind;
        Node(Node parent, String name, String kind) {
            super(parent, name); this.kind = kind;
            synchronized (DATA) { DATA.setProperty(kind + ":node:" + absolutePath(), ""); }
        }
        private String prefix() { return kind + ":value:" + absolutePath() + "\0"; }
        protected void putSpi(String key, String value) { synchronized (DATA) { DATA.setProperty(prefix() + key, value); save(); } }
        protected String getSpi(String key) { synchronized (DATA) { return DATA.getProperty(prefix() + key); } }
        protected void removeSpi(String key) { synchronized (DATA) { DATA.remove(prefix() + key); save(); } }
        protected void removeNodeSpi() {
            synchronized (DATA) {
                DATA.keySet().removeIf(k -> k.toString().startsWith(prefix()) || k.equals(kind + ":node:" + absolutePath()));
                save();
            }
        }
        protected String[] keysSpi() {
            synchronized (DATA) { return DATA.stringPropertyNames().stream().filter(k -> k.startsWith(prefix()))
                    .map(k -> k.substring(prefix().length())).sorted().toArray(String[]::new); }
        }
        protected String[] childrenNamesSpi() {
            String prefix = kind + ":node:" + (absolutePath().equals("/") ? "/" : absolutePath() + "/");
            synchronized (DATA) { return DATA.stringPropertyNames().stream().filter(k -> k.startsWith(prefix))
                    .map(k -> k.substring(prefix.length())).filter(k -> !k.isEmpty())
                    .map(k -> k.split("/", 2)[0]).distinct().sorted().toArray(String[]::new); }
        }
        protected AbstractPreferences childSpi(String name) { return new Node(this, name, kind); }
        protected void syncSpi() { /* One JVM owns each disposable file. */ }
        protected void flushSpi() { synchronized (DATA) { save(); } }
    }
}
