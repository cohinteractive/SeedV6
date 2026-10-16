package com.ohinteractive.seedv6.search.driver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Protects the shipped boundary, including references beyond constructor sites. */
class ProductionSearchBoundaryTest {
    @Test void productionCannotReferToVerificationClasses() throws Exception {
        Path main = Path.of("src/main/java/com/ohinteractive/seedv6");
        Path core = Path.of("../engine-core/src/main/java/com/ohinteractive/seedv6");
        Path training = Path.of("../engine-training/src/main/java/com/ohinteractive/seedv6");
        Path gui = Path.of("../app-gui/src/main/java/com/ohinteractive/seedv6");
        Path uci = Path.of("../app-uci/src/main/java/com/ohinteractive/seedv6");
        var forbidden = Pattern.compile("com\\.ohinteractive\\.seedv6\\.(?:tools\\.|search\\.(?:alphabeta|flat|iterative|quiescence|order)\\.|search\\.tt\\.TranspositionTable\\b|search\\.common\\.WindowedSearch\\b|search\\.diagnostics\\.(?:SearchDiagnostics|QsearchDecisionTrace)\\b|core\\.(?:BoardMoveType|GenMoveType|GenRewrite)\\b)");
        for (Path owner : List.of(main, core, training, gui, uci)) try(var files = Files.walk(owner)) {
            for(Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                assertFalse(forbidden.matcher(Files.readString(file)).find(), "Verification dependency in " + file);
            }
        }
        assertTrue(Files.isRegularFile(core.resolve("search/exact/ExactSearch.java")));
        assertTrue(Files.isRegularFile(core.resolve("search/tt/TTable.java")));
        assertFalse(Files.exists(main.resolve("search/exact/ExactSearch.java")));
        assertFalse(Files.exists(main.resolve("search/tt/TTable.java")));
        assertTrue(Files.isRegularFile(training.resolve("training/checkpoint/CheckpointStore.java")));
        assertFalse(Files.exists(main.resolve("training/checkpoint/CheckpointStore.java")));
        assertTrue(Files.isRegularFile(gui.resolve("gui/SwingLauncher.java")));
        try (var files = Files.walk(main)) {
            assertTrue(files.noneMatch(p -> p.toString().endsWith(".java") && p.startsWith(main.resolve("gui"))));
        }
        try (var files = Files.walk(gui)) {
            var legacy = Pattern.compile("com\\.ohinteractive\\.seedv6\\.(?:uci\\.|Main\\b)");
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList())
                assertFalse(legacy.matcher(Files.readString(file)).find(), "Legacy application dependency in " + file);
        }
        assertFalse(Files.exists(main.resolve("uci/UciEngine.java")));
        assertTrue(Files.isRegularFile(uci.resolve("uci/UciEngine.java")));
        try (var files = Files.walk(uci)) {
            var forbiddenUci = Pattern.compile("(?:com\\.ohinteractive\\.seedv6\\.(?:gui|training|tools)\\.|javax\\.swing\\.|java\\.awt\\.)");
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList())
                assertFalse(forbiddenUci.matcher(Files.readString(file)).find(), "Non-headless dependency " + file);
        }
        var applicationDependency = Pattern.compile("(?:com\\.ohinteractive\\.seedv6\\.(?:gui|uci)\\.|javax\\.swing\\.|java\\.awt\\.)");
        try (var files = Files.walk(training)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList())
                assertFalse(applicationDependency.matcher(Files.readString(file)).find(), "Application dependency in " + file);
        }
    }

    @Test void applicationJarContainsOnlyProductionClassesAndResources() throws Exception {
        Path jar = Path.of("build/libs/app.jar"); // test depends on installDist, which builds this JAR
        assertTrue(Files.isRegularFile(jar));
        Set<String> packages = Set.of("tools/", "search/alphabeta/", "search/flat/", "search/iterative/",
                "search/quiescence/", "search/order/");
        Set<String> classes = new HashSet<>();
        Path core = Path.of("build/install/seedv6/lib/engine-core.jar");
        assertTrue(Files.isRegularFile(core), "Distribution must include its engine dependency");
        Path training = Path.of("build/install/seedv6/lib/engine-training.jar");
        assertTrue(Files.isRegularFile(training), "Distribution must include its training dependency");
        Path gui = Path.of("build/install/seedv6/lib/app-gui.jar");
        assertTrue(Files.isRegularFile(gui), "Distribution must include its desktop dependency");
        Path uci = Path.of("build/install/seedv6/lib/app-uci.jar");
        assertTrue(Files.isRegularFile(uci));
        for (Path owner : List.of(jar, core, training, gui, uci)) try(var archive = new JarFile(owner.toFile())) {
            // Empty source directories can survive relocation; ownership concerns payload files.
            var names = archive.stream().filter(e -> !e.isDirectory()).map(e -> e.getName()).toList();
            for (String name : names) if (name.endsWith(".class"))
                assertTrue(classes.add(name), "Duplicate production class " + name);
            String base = "com/ohinteractive/seedv6/";
            for(String pkg : packages)
                assertTrue(names.stream().noneMatch(n -> n.startsWith(base + pkg)), "Packaged verification directory " + pkg);
            for(String type : Set.of("search/tt/TranspositionTable", "search/exact/FlatExactSearch",
                    "core/BoardMoveType", "core/GenMoveType", "core/util/MagicGenerator",
                    "training/service/FrozenWdlReplay", "training/service/BrnDiagnosticTraining"))
                assertTrue(names.stream().noneMatch(n -> n.startsWith(base + type)), "Packaged verification class " + type);
            if (owner.equals(jar)) {
                assertNotNull(archive.getJarEntry(base + "Main.class"));
                assertNull(archive.getJarEntry(base + "uci/UciEngine.class"));
                assertTrue(names.stream().noneMatch(n -> n.startsWith(base + "gui/")));
                assertNull(archive.getJarEntry(base + "search/exact/ExactSearch.class"));
                assertNull(archive.getJarEntry(base + "training/checkpoint/CheckpointStore.class"));
            } else if (owner.equals(core)) {
                assertNotNull(archive.getJarEntry(base + "search/exact/ExactSearch.class"));
                assertNotNull(archive.getJarEntry(base + "search/tt/TTable.class"));
                assertNotNull(archive.getJarEntry(base + "book/opening-book.txt"));
                assertNotNull(archive.getJarEntry(base + "search/tablebase/small-tables.sha256"));
                for (String name : names) {
                    assertFalse(name.startsWith(base + "gui/") || name.startsWith(base + "training/")
                            || name.startsWith(base + "uci/"), "Non-engine package " + name);
                    assertFalse(name.matches(".*(?:Trainer|TrainingCodec|Adam[^/]*)\\$?.*\\.class"),
                            "Training implementation in core: " + name);
                }
            } else if (owner.equals(training)) {
                assertNotNull(archive.getJarEntry(base + "training/checkpoint/CheckpointStore.class"));
                assertNotNull(archive.getJarEntry(base + "core/brnpair2/BrnPair2Trainer.class"));
                assertNotNull(archive.getJarEntry(base + "training/data/binpack-NOTICE.txt"));
                for (String name : names)
                    assertFalse(name.startsWith(base + "gui/") || name.startsWith(base + "uci/")
                            || name.startsWith(base + "search/"), "Non-training package " + name);
            } else if (owner.equals(uci)) {
                assertNotNull(archive.getJarEntry(base + "uci/UciMain.class"));
                assertNotNull(archive.getJarEntry(base + "uci/UciEngine.class"));
                assertNull(archive.getJarEntry(base + "uci/UciEvaluation.class"));
                for (String name : names) if (name.endsWith(".class"))
                    assertTrue(name.startsWith(base + "uci/"), "Non-UCI implementation " + name);
            } else {
                assertNotNull(archive.getJarEntry(base + "gui/SwingLauncher.class"));
                assertNotNull(archive.getJarEntry(base + "gui/pieces/original/wk.png"));
                assertNotNull(archive.getJarEntry(base + "gui/FlatDarkLaf.properties"));
                for (String name : names) if (name.endsWith(".class"))
                    assertTrue(name.startsWith(base + "gui/"), "Non-GUI implementation " + name);
            }
        }
    }
}
