package com.ohinteractive.seedv6.search.driver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Protects the shipped boundary, including references beyond constructor sites. */
class ProductionSearchBoundaryTest {
    @Test void productionCannotReferToVerificationClasses() throws Exception {
        Path main = Path.of("src/main/java/com/ohinteractive/seedv6");
        var forbidden = Pattern.compile("com\\.ohinteractive\\.seedv6\\.(?:tools\\.|search\\.(?:alphabeta|flat|iterative|quiescence|order)\\.|search\\.tt\\.TranspositionTable\\b|search\\.common\\.WindowedSearch\\b|search\\.diagnostics\\.(?:SearchDiagnostics|QsearchDecisionTrace)\\b|core\\.(?:BoardMoveType|GenMoveType|GenRewrite)\\b)");
        try(var files = Files.walk(main)) {
            for(Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                assertFalse(forbidden.matcher(Files.readString(file)).find(), "Verification dependency in " + file);
            }
        }
        assertTrue(Files.isRegularFile(main.resolve("search/exact/ExactSearch.java")));
        assertTrue(Files.isRegularFile(main.resolve("search/tt/TTable.java")));
    }

    @Test void applicationJarContainsOnlyProductionClassesAndResources() throws Exception {
        Path jar = Path.of("build/libs/app.jar"); // test depends on installDist, which builds this JAR
        assertTrue(Files.isRegularFile(jar));
        Set<String> packages = Set.of("tools/", "search/alphabeta/", "search/flat/", "search/iterative/",
                "search/quiescence/", "search/order/");
        try(var archive = new JarFile(jar.toFile())) {
            var names = archive.stream().map(e -> e.getName()).toList();
            String base = "com/ohinteractive/seedv6/";
            for(String pkg : packages)
                assertTrue(names.stream().noneMatch(n -> n.startsWith(base + pkg)), "Packaged verification directory " + pkg);
            for(String type : Set.of("search/tt/TranspositionTable", "search/exact/FlatExactSearch",
                    "core/BoardMoveType", "core/GenMoveType", "core/util/MagicGenerator",
                    "training/service/FrozenWdlReplay", "training/service/BrnDiagnosticTraining"))
                assertTrue(names.stream().noneMatch(n -> n.startsWith(base + type)), "Packaged verification class " + type);
            assertNotNull(archive.getJarEntry(base + "Main.class"));
            assertNotNull(archive.getJarEntry(base + "search/exact/ExactSearch.class"));
            assertNotNull(archive.getJarEntry(base + "search/tt/TTable.class"));
            assertNotNull(archive.getJarEntry(base + "gui/pieces/original/wk.png"));
        }
    }
}
