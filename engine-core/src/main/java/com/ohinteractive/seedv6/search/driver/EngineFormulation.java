package com.ohinteractive.seedv6.search.driver;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import com.ohinteractive.seedv6.search.exact.ConstructionSearch;
import com.ohinteractive.seedv6.search.exact.FlatSearchConfig;
import com.ohinteractive.seedv6.search.exact.FlatSearchConfig.Feature;
import com.ohinteractive.seedv6.search.manage.ClockPolicy;

/** Read-only identity of an already resolved binding. No model loading or Search allocation.
 * Names require exact mechanism/parameter equality; selected profile names are not evidence.
 * Build provenance remains ApplicationVersion's responsibility. */
public record EngineFormulation(ConstructionSearch search, String evaluator, String evaluatorIdentity,
                                ClockPolicy clockPolicy) {
    public EngineFormulation {
        Objects.requireNonNull(search);
        Objects.requireNonNull(evaluator);
        Objects.requireNonNull(evaluatorIdentity);
    }

    public static String searchName(ConstructionSearch search) {
        var actual = search.mechanisms();
        for (var profile : BaselineEngine.SearchProfile.values()) {
            var expected = BaselineEngine.configuration(search.workers(), search.hashMiB(), profile);
            if (actual.equals(expected.mechanisms()) && search.rootScheduling() == expected.rootScheduling())
                return switch (profile) {
                    case TC_P1 -> "TC-P1 — HL-G1 + bounded capture ProbCut";
                    case HL_G1 -> "HL-G1 — retained Search learning and history-guided reductions";
                    case LEGACY -> "LEGACY — pre-HL Search configuration";
                    case MW_C4_W4 -> "MW-C4 — eager root scheduling + guarded TT memory over TC-P1";
                };
        }
        if (actual.equals(FlatSearchConfig.candidate())) return "Construction V1";
        if (actual.equals(FlatSearchConfig.exact())) return "Exact flat PVS control";
        return "Custom flat PVS configuration";
    }

    public String searchName() { return searchName(search); }

    /** Full enabled set for diagnostics, independently of any familiar formulation label. */
    public List<Feature> enabledMechanisms() {
        return Stream.of(Feature.values()).filter(search.mechanisms()::has).toList();
    }

    /** Deliberately selected principal mechanisms; complete flags/configuration remain available. */
    public String principalMechanisms() {
        var labels = new java.util.ArrayList<String>();
        labels.add("PVS");
        for (Feature feature : enabledMechanisms()) {
            String label = switch (feature) {
                case QXRAY -> "QXRAY quiescence";
                case LMR -> "late-move reductions";
                case LMP -> "late-move pruning";
                case EFFORT -> "effort reuse";
                case COMPLETED_HINTS -> "completed TT hints";
                case CHECK_EXTENSION -> "check extensions";
                case RECAPTURE_EXTENSION -> "recapture extensions";
                case FUTILITY -> "futility";
                case REVERSE_FUTILITY -> "reverse futility";
                case NOMINAL_FUTILITY -> "nominal-depth futility";
                case VERIFIED_NULL -> "verified null move";
                case HEURISTIC_TT_MEMORY -> "heuristic TT memory";
                case AGED_HEURISTIC_TT_MEMORY -> "aged TT memory";
                case TT_INFORMATION -> "TT information";
                case REQUEST_LEARNING -> "retained Search learning";
                case HISTORY_EFFORT -> "history-guided reductions";
                case EVAL_CORRECTION -> "evaluation correction";
                case CORRECTED_QSEARCH -> "corrected qsearch";
                case RETAINED_CORRECTION -> "retained correction";
                case TACTICAL_VERIFICATION -> "bounded capture ProbCut";
                default -> null;
            };
            if (label != null) labels.add(label);
        }
        return String.join(", ", labels);
    }

    /** Complete reproducible binding identity; excludes consumer, display labels and build time. */
    public String configurationIdentity() {
        return search.identity() + "\n" + evaluatorIdentity + "\nclockPolicy="
                + (clockPolicy == null ? "NONE" : clockPolicy.identity());
    }

    /** Short SHA-256 fingerprint, not a replacement for the full identity or source provenance. */
    public String configurationId() {
        try {
            return "cfg-" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(configurationIdentity().getBytes(StandardCharsets.UTF_8))).substring(0, 16);
        } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    public String runtimeSummary() {
        return "flags=" + search.mechanisms().flags() + " · W" + search.workers() + " · Hash " + search.hashMiB()
                + " MiB · aspiration " + (search.aspiration() == null ? "OFF" : search.aspiration())
                + " · parallel " + search.parallelIdentity()
                + " · clock " + (clockPolicy == null ? "NONE" : clockPolicy.name()) + " · " + configurationId();
    }

    public String summary() {
        return searchName() + "\nEnabled: " + principalMechanisms() + "\nEvaluator: " + evaluator
                + "\n" + runtimeSummary();
    }
}
