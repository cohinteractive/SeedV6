package com.ohinteractive.seedv6.search.driver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.HashSet;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Keeps historical Search experiments explicit and out of normal application execution. */
class ProductionSearchBoundaryTest {
    @Test void onlyNamedLegacyReferenceToolsAndLegacyImplementationConstructOldSearch() throws Exception {
        Map<String, String> referenceOnly = Map.of(
                "search/alphabeta/RootParallelSearch.java", "legacy implementation internals",
                "tools/SearchSmoke.java", "independent flat-negamax reference smoke",
                "tools/search/SearchBenchmark.java", "legacy TT/selectivity/parallel comparison benchmark",
                "tools/search/BrnRemediationBenchmark.java", "legacy qsearch-remediation comparison",
                "tools/search/Brn2Diagnostics.java", "legacy qsearch decision tracing and calibration",
                "tools/search/BrnDiagnostic.java", "explicit legacy replay option only; production diagnostic training stays on SearchDriver",
                "tools/nnue/NnuePerformanceBenchmark.java", "legacy NNUE main/qsearch worker performance reference");
        var construction = Pattern.compile("\\bnew\\s+(?:[\\w]+\\.)*(?:AlphaBetaPvsSearch|RootParallelSearch|IterativeDeepeningSearch|FlatNegamax)\\s*\\(|\\b(?:AlphaBetaPvsSearch|RootParallelSearch|IterativeDeepeningSearch|FlatNegamax)\\s*::\\s*new");
        Path root = Path.of("src/main/java/com/ohinteractive/seedv6");
        var observed = new HashSet<String>();
        try(var files = Files.walk(root)) {
            for(Path path : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(path);
                if(!construction.matcher(source).find()) continue;
                String relative = root.relativize(path).toString().replace('\\', '/');
                observed.add(relative);
                assertTrue(referenceOnly.containsKey(relative), "Active legacy Search construction in " + relative);
            }
        }
        assertEquals(referenceOnly.keySet(), observed, "Update the explicit reference inventory when removing legacy tools.");
    }
}
