package com.ohinteractive.seedv6.uci;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brnpair2.BrnPair2Model;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.driver.ProductionSearch;
import com.ohinteractive.seedv6.search.driver.ExactSearchAdapter;
import com.ohinteractive.seedv6.search.driver.SearchDriver;
import com.ohinteractive.seedv6.search.driver.AspirationSearch;
import com.ohinteractive.seedv6.search.exact.ExactSearch;
import com.ohinteractive.seedv6.search.exact.IterationReuse;
import com.ohinteractive.seedv6.search.exact.NullMovePolicy;
import com.ohinteractive.seedv6.search.manage.SearchLifecycleService;
import com.ohinteractive.seedv6.search.common.TimeSource;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.ohinteractive.seedv6.training.model.TrainingArchitecture;

/** Explicit, immutable process configuration. No preferences, promotion, or fallback. */
record UciEvaluation(SearchEvaluation evaluation, String identity, int qpolicy, int lmrPolicy, String aspirationPolicy, int lmpPolicy, int checkPolicy, int reusePolicy, NullMovePolicy nullPolicy) {
    UciEvaluation(SearchEvaluation evaluation, String identity, int qpolicy, int lmrPolicy,
            String aspirationPolicy, int lmpPolicy, int checkPolicy, int reusePolicy) {
        this(evaluation, identity, qpolicy, lmrPolicy, aspirationPolicy, lmpPolicy, checkPolicy, reusePolicy, NullMovePolicy.OFF);
    }
    UciEvaluation(SearchEvaluation evaluation, String identity, int qpolicy, int lmrPolicy,
            String aspirationPolicy, int lmpPolicy, int checkPolicy) {
        this(evaluation, identity, qpolicy, lmrPolicy, aspirationPolicy, lmpPolicy, checkPolicy, REUSE_PRODUCTION);
    }
    UciEvaluation(SearchEvaluation evaluation, String identity, int qpolicy, int lmrPolicy,
            String aspirationPolicy, int lmpPolicy) {
        this(evaluation, identity, qpolicy, lmrPolicy, aspirationPolicy, lmpPolicy, ExactSearch.CHECK_OFF);
    }
    UciEvaluation(SearchEvaluation evaluation, String identity, int qpolicy, int lmrPolicy) {
        this(evaluation, identity, qpolicy, lmrPolicy, "off");
    }
    UciEvaluation(SearchEvaluation evaluation, String identity, int qpolicy, int lmrPolicy, String aspirationPolicy) {
        this(evaluation, identity, qpolicy, lmrPolicy, aspirationPolicy, LMP_PRODUCTION);
    }
    static final String PREFIX = "seedv6.uci.";
    static final int PRODUCTION = -1;
    static final int STATIC_CONTROL = -2;
    static final int LMR_PRODUCTION = -1;
    static final int LMP_PRODUCTION = -1;
    static final int REUSE_PRODUCTION = -1;

    static UciEvaluation load(Properties properties) {
        String store = properties.getProperty(PREFIX + "pair2Store", "");
        String checkpoint = properties.getProperty(PREFIX + "checkpoint", "");
        String hash = properties.getProperty(PREFIX + "networkSha256", "");
        String warmup = properties.getProperty(PREFIX + "warmupDepth", "0");
        String leaves = properties.getProperty(PREFIX + "qsearchResearch", "production");
        int policy = researchPolicy(leaves);
        String lmr = properties.getProperty(PREFIX + "lmrResearch", "production");
        int reduction = reductionPolicy(lmr);
        String aspiration = properties.getProperty(PREFIX + "aspirationResearch", "off");
        int pruning = pruningPolicy(properties.getProperty(PREFIX + "lmpResearch", "production"));
        int reuse = reusePolicy(properties.getProperty(PREFIX + "reuseResearch", "production"));
        NullMovePolicy nullPolicy = NullMovePolicy.valueOf(properties.getProperty(PREFIX + "nmpResearch", "off").toUpperCase(java.util.Locale.ROOT));
        if(nullPolicy != NullMovePolicy.OFF && (policy != PRODUCTION || reduction != LMR_PRODUCTION
                || pruning != LMP_PRODUCTION || reuse != REUSE_PRODUCTION || !aspiration.equals("off")
                || !properties.getProperty(PREFIX + "checkResearch", "off").equals("off")))
            throw new IllegalArgumentException("Null research requires complete cumulative EFFORT production");
        int extension = extensionPolicy(properties.getProperty(PREFIX + "checkResearch", "off"));
        if(reuse > IterationReuse.OFF && (policy != PRODUCTION || reduction != LMR_PRODUCTION
                || pruning != LMP_PRODUCTION || !aspiration.equals("off") || extension != ExactSearch.CHECK_OFF))
            throw new IllegalArgumentException("Reuse research requires full-window cumulative production");
        if(extension != ExactSearch.CHECK_OFF && (policy != PRODUCTION || reduction != LMR_PRODUCTION
                || pruning != LMP_PRODUCTION || !aspiration.equals("off")))
            throw new IllegalArgumentException("Check research requires full-window cumulative production");
        if(pruning > ExactSearch.LMP_OFF && (policy != PRODUCTION || reduction != LMR_PRODUCTION || !aspiration.equals("off")))
            throw new IllegalArgumentException("LMP research requires full-window cumulative production");
        if(!aspiration.equals("off")) {
            aspirationPolicy(aspiration);
            if(policy != PRODUCTION || reduction != LMR_PRODUCTION)
                throw new IllegalArgumentException("Aspiration research requires cumulative production");
        }
        if(reduction > ExactSearch.LMR_OFF && policy != PRODUCTION && policy != ExactSearch.QSEARCH_BASELINE)
            throw new IllegalArgumentException("LMR research requires unpruned qsearch");
        if (store.isBlank()) {
            if (!checkpoint.isEmpty() || !hash.isEmpty() || !warmup.equals("0") || policy >= 0 || reduction > ExactSearch.LMR_OFF || !aspiration.equals("off") || pruning > ExactSearch.LMP_OFF || extension != ExactSearch.CHECK_OFF || reuse > IterationReuse.OFF || nullPolicy != NullMovePolicy.OFF)
                throw new IllegalArgumentException("Explicit UCI evaluator options require pair2Store");
            return new UciEvaluation(SearchEvaluation.handcrafted(), "", policy, reduction, aspiration, pruning, extension, reuse, nullPolicy);
        }
        if (!hash.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("Pair-2 UCI requires the expected networkSha256 (lowercase hex)");
        int depth = Integer.parseInt(warmup);
        if (depth < 0 || depth > 4) throw new IllegalArgumentException("UCI warmupDepth must be 0..4");
        try {
            var selected = checkpoint.isEmpty() ? CheckpointStore.readBestSnapshot(Path.of(store))
                    : CheckpointStore.readSnapshot(Path.of(store), checkpoint);
            if (selected.manifest().architecture() != TrainingArchitecture.BRN_PAIR2)
                throw new IllegalArgumentException("UCI pair2Store is not BRE-Pair 2");
            if (!hash.equals(selected.manifest().networkSha256()))
                throw new IllegalArgumentException("UCI networkSha256 mismatch");
            var definition = selected.model().evaluation(NnueScoreMapping.V1);
            // Untimed, bounded JVM warm-up; private scratch/TT discarded before play.
            // Three complete depth-limited production requests, outside game clocks.
            if (depth > 0) try (var driver = new SearchDriver(search(definition, policy, reduction, aspiration, pruning, extension, reuse, nullPolicy, 1))) {
                var board = Board.startingPosition();
                for (int i = 0; i < 3; i++) {
                    driver.newGame();
                    if (!driver.search(new SearchRequest(board, GameHistory.initial(board), depth)).targetDepthCompleted())
                        throw new IllegalStateException("Incomplete UCI warm-up");
                }
            }
            var version = com.ohinteractive.seedv6.ApplicationVersion.load();
            return new UciEvaluation(definition, "evaluator=BRN_PAIR2 checkpoint=" + selected.manifest().id()
                    + " sha256=" + hash + " mapping=" + BrnPair2Model.CALIBRATION_ID
                    + " warmup=" + (depth == 0 ? "off" : "3x" + depth)
                    + " ttMiB=64 nmp=off reverseFutility=off"
                    + (policy == PRODUCTION ? "" : " leaves=" + leaves + " mode=control")
                    + (policy >= 0 ? " qTT=off nominalQProof=off" : "")
                    + " build=" + version.build() + " source=" + version.sourceRevision(), policy, reduction, aspiration, pruning, extension, reuse, nullPolicy);
        } catch (IOException invalid) {
            throw new IllegalArgumentException("Cannot load requested BRE-Pair 2 checkpoint", invalid);
        }
    }

    SearchLifecycleService service(int workers) {
        if(nullPolicy != NullMovePolicy.OFF) return new SearchLifecycleService(TimeSource.SYSTEM,
                () -> search(evaluation, qpolicy, lmrPolicy, aspirationPolicy, lmpPolicy, checkPolicy, reusePolicy, nullPolicy, workers));
        if(reusePolicy > IterationReuse.OFF) return new SearchLifecycleService(TimeSource.SYSTEM,
                () -> search(evaluation, qpolicy, lmrPolicy, aspirationPolicy, lmpPolicy, checkPolicy, reusePolicy, nullPolicy, workers));
        if(checkPolicy != ExactSearch.CHECK_OFF) return new SearchLifecycleService(TimeSource.SYSTEM,
                () -> search(evaluation, qpolicy, lmrPolicy, aspirationPolicy, lmpPolicy, checkPolicy, reusePolicy, nullPolicy, workers));
        if(lmpPolicy > ExactSearch.LMP_OFF) return new SearchLifecycleService(TimeSource.SYSTEM,
                () -> search(evaluation, qpolicy, lmrPolicy, aspirationPolicy, lmpPolicy, checkPolicy, reusePolicy, nullPolicy, workers));
        if(!aspirationPolicy.equals("off")) return new SearchLifecycleService(TimeSource.SYSTEM,
                () -> search(evaluation, qpolicy, lmrPolicy, aspirationPolicy, lmpPolicy, checkPolicy, reusePolicy, nullPolicy, workers));
        if(lmrPolicy > ExactSearch.LMR_OFF) return new SearchLifecycleService(TimeSource.SYSTEM,
                () -> search(evaluation, qpolicy, lmrPolicy, aspirationPolicy, lmpPolicy, checkPolicy, reusePolicy, nullPolicy, workers));
        if(qpolicy == PRODUCTION) return lmrPolicy == LMR_PRODUCTION
                ? (lmpPolicy == ExactSearch.LMP_OFF
                        ? SearchLifecycleService.quietLmrControl(workers, evaluation)
                        : (reusePolicy == IterationReuse.OFF
                                ? SearchLifecycleService.iterationReuseControl(workers, evaluation)
                                : new SearchLifecycleService(workers, evaluation)))
                : SearchLifecycleService.unprunedControl(workers, evaluation);
        if(qpolicy == STATIC_CONTROL) return SearchLifecycleService.staticControl(workers, evaluation);
        return new SearchLifecycleService(TimeSource.SYSTEM,
                () -> search(evaluation, qpolicy, lmrPolicy, aspirationPolicy, lmpPolicy, checkPolicy, reusePolicy, nullPolicy, workers));
    }

    private static com.ohinteractive.seedv6.search.common.SingleDepthSearch search(
            SearchEvaluation evaluation, int policy, int lmrPolicy, String aspiration, int lmpPolicy, int checkPolicy, int reusePolicy, NullMovePolicy nullPolicy, int workers) {
        if(nullPolicy != NullMovePolicy.OFF) {
            if(workers != 1 || !evaluation.isBrnPair2())
                throw new IllegalArgumentException("Null research requires single-worker Pair-2");
            return ExactSearchAdapter.nullMoveResearch(evaluation, nullPolicy, null);
        }
        if(reusePolicy > IterationReuse.OFF) {
            if(workers != 1 || !evaluation.isBrnPair2())
                throw new IllegalArgumentException("Reuse research requires single-worker Pair-2");
            return ExactSearchAdapter.iterationReuseResearch(evaluation, reusePolicy, null);
        }
        if(checkPolicy != ExactSearch.CHECK_OFF) {
            if(workers != 1 || !evaluation.isBrnPair2())
                throw new IllegalArgumentException("Check research requires single-worker Pair-2");
            return ExactSearchAdapter.checkExtensionResearch(evaluation, checkPolicy,
                    ExactSearch.LMR_QUIET, ExactSearch.LMP_WINDOW, null);
        }
        if(lmpPolicy > ExactSearch.LMP_OFF) {
            if(workers != 1) throw new IllegalArgumentException("LMP research requires one worker");
            return ExactSearchAdapter.lateMovePruningResearch(evaluation, lmpPolicy, null);
        }
        if(!aspiration.equals("off")) {
            if(workers != 1) throw new IllegalArgumentException("Aspiration research requires one worker");
            return new AspirationSearch(ExactSearchAdapter.quietLateMoveReductions(evaluation), aspirationPolicy(aspiration));
        }
        if(lmrPolicy > ExactSearch.LMR_OFF) {
            if(workers != 1) throw new IllegalArgumentException("LMR research requires one worker");
            return ExactSearchAdapter.lateMoveReductionResearch(evaluation, lmrPolicy, null);
        }
        if(policy == PRODUCTION) return lmrPolicy == LMR_PRODUCTION
                ? (lmpPolicy == ExactSearch.LMP_OFF
                        ? ProductionSearch.createQuietLmrControl(workers, evaluation)
                        : (reusePolicy == IterationReuse.OFF
                                ? ProductionSearch.createIterationReuseControl(workers, evaluation)
                                : ProductionSearch.create(workers, evaluation)))
                : ProductionSearch.createUnprunedControl(workers, evaluation);
        if(policy == STATIC_CONTROL) return ProductionSearch.createStaticControl(workers, evaluation);
        if(workers != 1) throw new IllegalArgumentException("Qsearch research requires one worker");
        return ExactSearchAdapter.quiescenceResearch(evaluation, policy);
    }

    /** Report the actual composition, including after a drained Threads reconfiguration. */
    String identity(int workers) {
        if(identity.isEmpty()) return identity;
        boolean qsearch = workers == 1 && evaluation.isBrnPair2();
        int actualLmr = lmrPolicy == LMR_PRODUCTION
                ? (qpolicy == PRODUCTION && qsearch ? ExactSearch.LMR_QUIET : ExactSearch.LMR_OFF) : lmrPolicy;
        String reductions = switch(actualLmr) {
            case ExactSearch.LMR_OFF -> "off";
            case ExactSearch.LMR_FIXED -> "fixed";
            case ExactSearch.LMR_SCALED -> "scaled";
            case ExactSearch.LMR_SCOUT -> "scout";
            case ExactSearch.LMR_QUIET -> "quiet";
            default -> throw new IllegalStateException("Unknown LMR identity");
        };
        int actualLmp = lmpPolicy == LMP_PRODUCTION
                ? (qsearch && qpolicy == PRODUCTION && lmrPolicy == LMR_PRODUCTION
                        && aspirationPolicy.equals("off") ? ExactSearch.LMP_WINDOW : ExactSearch.LMP_OFF)
                : lmpPolicy;
        return identity.replace("nmp=off", "nmp=" + nullPolicy.name().toLowerCase(java.util.Locale.ROOT)) + " reuse=" + reuseName(actualReuse(workers)) + " check=" + extensionName(checkPolicy) + " lmp=" + pruningName(actualLmp) + " aspiration=" + aspirationPolicy + " lmr=" + reductions + " workers=" + workers + (qpolicy == PRODUCTION
                ? " leaves=" + (qsearch ? "unpruned" : "static") + " mode=" + (lmrPolicy == LMR_PRODUCTION && lmpPolicy != ExactSearch.LMP_OFF ? "production" : "control")
                    + (qsearch ? " qTT=off nominalQProof=off" : "")
                : "") + (qpolicy >= 0 || lmrPolicy > 0 || !aspirationPolicy.equals("off") || lmpPolicy > ExactSearch.LMP_OFF || checkPolicy != ExactSearch.CHECK_OFF || reusePolicy > IterationReuse.OFF || nullPolicy != NullMovePolicy.OFF ? " experimental=true" : "")
                + (actualLmr > 0 ? " nominalLmrProof=off" : "")
                + (actualLmp > ExactSearch.LMP_OFF ? " nominalLmpProof=off" : "");
    }

    private int actualReuse(int workers) {
        if(reusePolicy != REUSE_PRODUCTION) return reusePolicy;
        return workers == 1 && evaluation.isBrnPair2() && qpolicy == PRODUCTION
                && lmrPolicy == LMR_PRODUCTION && lmpPolicy == LMP_PRODUCTION
                && aspirationPolicy.equals("off") && checkPolicy == ExactSearch.CHECK_OFF
                ? IterationReuse.EFFORT : IterationReuse.OFF;
    }

    static int reusePolicy(String name) {
        return switch(name) {
            case "production" -> REUSE_PRODUCTION;
            case "off" -> IterationReuse.OFF;
            case "observe" -> IterationReuse.OBSERVE;
            case "root" -> IterationReuse.ROOT;
            case "pv" -> IterationReuse.PV;
            case "score" -> IterationReuse.SCORE;
            case "effort" -> IterationReuse.EFFORT;
            case "history" -> IterationReuse.HISTORY;
            case "pvhistory" -> IterationReuse.PV_HISTORY;
            default -> throw new IllegalArgumentException("Unknown reuse policy: " + name);
        };
    }
    static String reuseName(int policy) {
        return switch(policy) {
            case 0 -> "off"; case 1 -> "observe"; case 2 -> "root"; case 3 -> "pv";
            case 4 -> "score"; case 5 -> "effort"; case 6 -> "history"; case 7 -> "pvhistory";
            default -> throw new IllegalArgumentException("Unknown reuse policy: " + policy);
        };
    }

    static int extensionPolicy(String name) {
        return switch(name) {
            case "off" -> ExactSearch.CHECK_OFF;
            case "boundary" -> ExactSearch.CHECK_BOUNDARY;
            case "giving" -> ExactSearch.CHECK_GIVING;
            case "evasion" -> ExactSearch.CHECK_EVASION;
            case "forced" -> ExactSearch.CHECK_FORCED;
            case "leaf" -> ExactSearch.CHECK_LEAF;
            case "repeat" -> ExactSearch.CHECK_REPEAT;
            default -> throw new IllegalArgumentException("Unknown check policy: " + name);
        };
    }

    static String extensionName(int policy) {
        return switch(policy) {
            case ExactSearch.CHECK_OFF -> "off";
            case ExactSearch.CHECK_BOUNDARY -> "boundary";
            case ExactSearch.CHECK_GIVING -> "giving";
            case ExactSearch.CHECK_EVASION -> "evasion";
            case ExactSearch.CHECK_FORCED -> "forced";
            case ExactSearch.CHECK_LEAF -> "leaf";
            case ExactSearch.CHECK_REPEAT -> "repeat";
            default -> throw new IllegalStateException("Unknown check policy");
        };
    }

    static int pruningPolicy(String name) {
        return switch(name) {
            case "production" -> LMP_PRODUCTION;
            case "off" -> ExactSearch.LMP_OFF;
            case "shallow" -> ExactSearch.LMP_SHALLOW;
            case "lmr" -> ExactSearch.LMP_LMR;
            case "combined" -> ExactSearch.LMP_COMBINED;
            case "guarded" -> ExactSearch.LMP_GUARDED;
            case "d2" -> ExactSearch.LMP_DEPTH_TWO;
            case "window" -> ExactSearch.LMP_WINDOW;
            case "history-protect" -> ExactSearch.LMP_HISTORY_PROTECT;
            case "history-extra" -> ExactSearch.LMP_HISTORY_EXTRA;
            case "history-replace" -> ExactSearch.LMP_HISTORY_REPLACE;
            case "history-deep" -> ExactSearch.LMP_HISTORY_DEEP;
            case "history-gate" -> ExactSearch.LMP_HISTORY_GATE;
            case "history-leaf" -> ExactSearch.LMP_HISTORY_LEAF;
            default -> throw new IllegalArgumentException("Unknown LMP policy: " + name);
        };
    }
    static String pruningName(int policy) {
        return switch(policy) {
            case LMP_PRODUCTION -> "production";
            case ExactSearch.LMP_OFF -> "off";
            case ExactSearch.LMP_SHALLOW -> "shallow";
            case ExactSearch.LMP_LMR -> "lmr";
            case ExactSearch.LMP_COMBINED -> "combined";
            case ExactSearch.LMP_GUARDED -> "guarded";
            case ExactSearch.LMP_DEPTH_TWO -> "d2";
            case ExactSearch.LMP_WINDOW -> "window";
            case ExactSearch.LMP_HISTORY_PROTECT -> "history-protect";
            case ExactSearch.LMP_HISTORY_EXTRA -> "history-extra";
            case ExactSearch.LMP_HISTORY_REPLACE -> "history-replace";
            case ExactSearch.LMP_HISTORY_DEEP -> "history-deep";
            case ExactSearch.LMP_HISTORY_GATE -> "history-gate";
            case ExactSearch.LMP_HISTORY_LEAF -> "history-leaf";
            default -> throw new IllegalStateException("Unknown LMP identity");
        };
    }

    static AspirationSearch.Policy aspirationPolicy(String name) {
        return switch(name) {
            case "one16" -> AspirationSearch.Policy.ONE16;
            case "wide64" -> AspirationSearch.Policy.WIDE64;
            case "widen16" -> AspirationSearch.Policy.WIDEN16;
            case "adaptive16" -> AspirationSearch.Policy.ADAPTIVE16;
            case "alpha16" -> AspirationSearch.Policy.ALPHA16;
            case "parity16" -> AspirationSearch.Policy.PARITY16;
            default -> throw new IllegalArgumentException("Unknown aspiration policy: " + name);
        };
    }

    static int researchPolicy(String name) {
        return switch(name) {
            case "production" -> PRODUCTION;
            case "static" -> STATIC_CONTROL;
            case "unpruned" -> ExactSearch.QSEARCH_BASELINE;
            case "see-promo" -> ExactSearch.SEE_PROMO_SAFE;
            case "see-check" -> ExactSearch.SEE_CHECK_PROMO_SAFE;
            case "q4" -> ExactSearch.QDEPTH_4;
            case "q8" -> ExactSearch.QDEPTH_8;
            case "q12" -> ExactSearch.QDEPTH_12;
            case "initial-checks" -> ExactSearch.QCHECK_INITIAL_ONLY;
            case "recapture" -> ExactSearch.Q_RECAPTURE;
            case "see-initial-checks" -> ExactSearch.Q_SEE_INITIAL_CHECKS;
            case "check-only" -> ExactSearch.Q_CHECK_ONLY;
            case "q2" -> ExactSearch.Q_DEPTH_TWO;
            default -> throw new IllegalArgumentException("Unknown qsearch research policy: " + name);
        };
    }

    static int reductionPolicy(String name) {
        return switch(name) {
            case "production" -> LMR_PRODUCTION;
            case "off" -> ExactSearch.LMR_OFF;
            case "fixed" -> ExactSearch.LMR_FIXED;
            case "scaled" -> ExactSearch.LMR_SCALED;
            case "scout" -> ExactSearch.LMR_SCOUT;
            case "quiet" -> ExactSearch.LMR_QUIET;
            default -> throw new IllegalArgumentException("Unknown LMR research policy: " + name);
        };
    }
}
