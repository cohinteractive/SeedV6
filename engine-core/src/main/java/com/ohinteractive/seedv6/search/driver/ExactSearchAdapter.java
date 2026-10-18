package com.ohinteractive.seedv6.search.driver;

import java.util.Objects;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.exact.ExactEvaluator;
import com.ohinteractive.seedv6.search.exact.ExactSearch;
import com.ohinteractive.seedv6.search.exact.IterationReuse;
import com.ohinteractive.seedv6.search.exact.NullMovePolicy;
import com.ohinteractive.seedv6.search.exact.InternalSearchPolicy;
import com.ohinteractive.seedv6.search.exact.SeePolicy;
import com.ohinteractive.seedv6.search.exact.FutilityPolicy;
import com.ohinteractive.seedv6.search.exact.LmrDiagnostics;
import com.ohinteractive.seedv6.search.exact.LmpDiagnostics;
import com.ohinteractive.seedv6.search.exact.CheckExtensionDiagnostics;
import com.ohinteractive.seedv6.search.tt.TTable;

/**
 * Worker-confined production boundary for one fixed-depth Search invocation.
 * The calibrated HCE default composes SR-018 and SR-019; neural and explicitly supplied
 * evaluator constructors retain static leaves. Single-worker Pair-2 production selects
 * QXRAY qsearch, native EFFORT, quiet LMR and protected narrow-window LMP at its factory.
 * The recursive algorithm uses the same
 * evaluator interface, without evaluator-type tests in its hot path.
 * Owns evaluator and Search TTable state, reusing both across iterations and
 * requests. Production uses TTable's existing default size; injected null is TT-off.
 * Consumer nodes count admitted children (not roots), as SearchControl requires;
 * the independent ExactSearch result continues to count roots as well.
 */
public final class ExactSearchAdapter implements SingleDepthSearch {
    private final ExactSearch exact;
    private final long[] root = new long[Board.MAX_BITBOARDS];
    private final long[] scratch = new long[Board.MAX_BITBOARDS];
    private final long[] rootMoves = new long[512];
    private SearchControl control;
    private long nodes;
    private long evaluations;
    private long qnodes;
    private int maximumQply;
    private int maximumPly;
    private volatile boolean active;

    public ExactSearchAdapter() { this(SearchEvaluation.handcrafted()); }

    public ExactSearchAdapter(SearchEvaluation evaluation) { this(evaluation, new TTable()); }

    public ExactSearchAdapter(ExactEvaluator evaluator) { this(evaluator, new TTable()); }

    /** Explicit small table or null is useful for bounded/headless comparisons. */
    public ExactSearchAdapter(SearchEvaluation evaluation, TTable table) {
        this(ExactEvaluator.from(evaluation), table, evaluation == SearchEvaluation.handcrafted() && table != null);
    }

    public ExactSearchAdapter(ExactEvaluator evaluator, TTable table) {
        this(evaluator, table, false);
    }

    private ExactSearchAdapter(ExactEvaluator evaluator, TTable table, boolean calibratedPruning) {
        this(evaluator, table, calibratedPruning, -1);
    }

    /** Explicit Phase 2 single-worker research; ordinary constructors stay static. */
    public static ExactSearchAdapter quiescenceResearch(SearchEvaluation evaluation, int policy) {
        return new ExactSearchAdapter(ExactEvaluator.from(evaluation), new TTable(), false, policy);
    }

    /** Confirmed SR-001 composition; affected nominal proofs and all qTT stores stay disabled. */
    public static ExactSearchAdapter unprunedQuiescence(SearchEvaluation evaluation) {
        return quiescenceResearch(evaluation, ExactSearch.QSEARCH_BASELINE);
    }

    /** Confirmed SR-029 composition; omitted nominal work never becomes ordinary TT proof. */
    public static ExactSearchAdapter quietLateMoveReductions(SearchEvaluation evaluation) {
        return lateMoveReductionResearch(evaluation, ExactSearch.LMR_QUIET, null);
    }

    public static ExactSearchAdapter lateMoveReductionResearch(SearchEvaluation evaluation,
            int policy, LmrDiagnostics diagnostics) {
        return new ExactSearchAdapter(ExactEvaluator.from(evaluation), new TTable(), false,
                ExactSearch.QSEARCH_BASELINE, policy, diagnostics);
    }

    /** Confirmed SR-022 composition; the measured core and conservative proof suppression are unchanged. */
    public static ExactSearchAdapter quietMovePruning(SearchEvaluation evaluation) {
        return lateMovePruningResearch(evaluation, ExactSearch.LMP_WINDOW, null);
    }

    public static ExactSearchAdapter lateMovePruningResearch(SearchEvaluation evaluation,
            int policy, LmpDiagnostics diagnostics) {
        return new ExactSearchAdapter(ExactEvaluator.from(evaluation), new TTable(), false,
                ExactSearch.QSEARCH_BASELINE, ExactSearch.LMR_QUIET, null, policy, diagnostics);
    }

    public static ExactSearchAdapter checkExtensionResearch(SearchEvaluation evaluation,
            int policy, int lmr, int lmp, CheckExtensionDiagnostics diagnostics) {
        return new ExactSearchAdapter(ExactEvaluator.from(evaluation), new TTable(), false,
                ExactSearch.QSEARCH_BASELINE, lmr, null, lmp, null, policy, diagnostics);
    }

    public static ExactSearchAdapter iterationReuseResearch(SearchEvaluation evaluation, int policy,
            IterationReuse.Diagnostics diagnostics) {
        return iterationReuseResearch(evaluation, policy, diagnostics, new TTable());
    }

    private static ExactSearchAdapter iterationReuseResearch(SearchEvaluation evaluation, int policy,
            IterationReuse.Diagnostics diagnostics, TTable table) {
        return new ExactSearchAdapter(ExactEvaluator.from(evaluation), table, false,
                ExactSearch.QSEARCH_BASELINE, ExactSearch.LMR_QUIET, null, ExactSearch.LMP_WINDOW, null,
                ExactSearch.CHECK_OFF, null, policy, diagnostics);
    }

    public static ExactSearchAdapter nullMoveResearch(SearchEvaluation evaluation, NullMovePolicy policy,
            NullMovePolicy.Diagnostics diagnostics) {
        if(!evaluation.isBrnPair2()) throw new IllegalArgumentException("Null research requires Pair-2");
        return new ExactSearchAdapter(ExactEvaluator.from(evaluation), new TTable(), false,
                ExactSearch.QSEARCH_BASELINE, ExactSearch.LMR_QUIET, null, ExactSearch.LMP_WINDOW, null,
                ExactSearch.CHECK_OFF, null, IterationReuse.EFFORT, null, policy, diagnostics);
    }

    /** SR-024 experiments preserve native EFFORT and all cumulative recursive policies. */
    public static ExactSearchAdapter seeResearch(SearchEvaluation evaluation, SeePolicy policy,
            SeePolicy.Diagnostics diagnostics) {
        return seeResearch(evaluation, policy, diagnostics, new TTable());
    }

    /** The table is exclusively owned by this search; policy and evaluator arithmetic are unchanged. */
    public static ExactSearchAdapter seeResearch(SearchEvaluation evaluation, SeePolicy policy,
            SeePolicy.Diagnostics diagnostics, TTable table) {
        var adapter = iterationReuseResearch(evaluation, IterationReuse.EFFORT, null, table);
        adapter.exact.withSeeResearch(policy, diagnostics);
        return adapter;
    }

    public static ExactSearchAdapter futilityResearch(SearchEvaluation evaluation, FutilityPolicy policy,
            FutilityPolicy.Diagnostics diagnostics) {
        if(!evaluation.isBrnPair2()) throw new IllegalArgumentException("Futility research requires Pair-2");
        var adapter = seeResearch(evaluation, SeePolicy.QXRAY, null);
        adapter.exact.withFutilityResearch(policy, diagnostics);
        return adapter;
    }

    /** Phase 3 opt-in O2 controls over the accepted one-worker cumulative engine. */
    public static ExactSearchAdapter historyResearch(SearchEvaluation evaluation, boolean retain,
            boolean nominalOnly, com.ohinteractive.seedv6.search.exact.HistoryLearning.Diagnostics diagnostics) {
        if(!evaluation.isBrnPair2()) throw new IllegalArgumentException("History research requires Pair-2");
        var adapter=seeResearch(evaluation,SeePolicy.QXRAY,null);
        adapter.exact.withHistoryResearch(retain,nominalOnly,diagnostics);
        return adapter;
    }

    /** Explicit O1/O2 composition; never selected by the production factory. */
    public static ExactSearchAdapter informationResearch(SearchEvaluation evaluation, boolean hints,
            boolean retain, boolean nominalOnly, com.ohinteractive.seedv6.search.exact.MoveHintDiagnostics diagnostics) {
        var adapter=historyResearch(evaluation,retain,nominalOnly,null);
        adapter.exact.withCompletedHints(hints,diagnostics);
        return adapter;
    }

    public static ExactSearchAdapter depthFeedbackResearch(SearchEvaluation evaluation,
            com.ohinteractive.seedv6.search.exact.DepthFeedback policy,
            com.ohinteractive.seedv6.search.exact.DepthFeedback.Diagnostics diagnostics) {
        if(!evaluation.isBrnPair2()) throw new IllegalArgumentException("Depth feedback requires Pair-2");
        var adapter = seeResearch(evaluation, SeePolicy.QXRAY, null);
        adapter.exact.withDepthFeedback(policy, diagnostics);
        return adapter;
    }

    public static ExactSearchAdapter tacticalExtensionResearch(SearchEvaluation evaluation,
            com.ohinteractive.seedv6.search.exact.TacticalExtension policy,
            com.ohinteractive.seedv6.search.exact.TacticalExtensionDiagnostics diagnostics) {
        if(!evaluation.isBrnPair2())throw new IllegalArgumentException("Tactical extensions require Pair-2");
        var adapter=seeResearch(evaluation,SeePolicy.QXRAY,null);
        adapter.exact.withTacticalExtension(policy,diagnostics);
        return adapter;
    }

    public static ExactSearchAdapter internalSearchResearch(SearchEvaluation evaluation, InternalSearchPolicy policy,
            InternalSearchPolicy.Diagnostics diagnostics) {
        if(!evaluation.isBrnPair2()) throw new IllegalArgumentException("Internal research requires Pair-2");
        return new ExactSearchAdapter(ExactEvaluator.from(evaluation), new TTable(), false,
                ExactSearch.QSEARCH_BASELINE, ExactSearch.LMR_QUIET, null, ExactSearch.LMP_WINDOW, null,
                ExactSearch.CHECK_OFF, null, IterationReuse.EFFORT, null, null, null, policy, diagnostics);
    }

    private ExactSearchAdapter(ExactEvaluator evaluator, TTable table, boolean calibratedPruning, int qpolicy) {
        this(evaluator, table, calibratedPruning, qpolicy, ExactSearch.LMR_OFF, null);
    }

    private ExactSearchAdapter(ExactEvaluator evaluator, TTable table, boolean calibratedPruning,
            int qpolicy, int lmrPolicy, LmrDiagnostics lmrDiagnostics) {
        this(evaluator, table, calibratedPruning, qpolicy, lmrPolicy, lmrDiagnostics, ExactSearch.LMP_OFF, null);
    }

    private ExactSearchAdapter(ExactEvaluator evaluator, TTable table, boolean calibratedPruning,
            int qpolicy, int lmrPolicy, LmrDiagnostics lmrDiagnostics, int lmpPolicy, LmpDiagnostics lmpDiagnostics) {
        this(evaluator, table, calibratedPruning, qpolicy, lmrPolicy, lmrDiagnostics, lmpPolicy, lmpDiagnostics,
                ExactSearch.CHECK_OFF, null);
    }

    private ExactSearchAdapter(ExactEvaluator evaluator, TTable table, boolean calibratedPruning,
            int qpolicy, int lmrPolicy, LmrDiagnostics lmrDiagnostics, int lmpPolicy, LmpDiagnostics lmpDiagnostics,
            int checkPolicy, CheckExtensionDiagnostics checkDiagnostics) {
        this(evaluator, table, calibratedPruning, qpolicy, lmrPolicy, lmrDiagnostics, lmpPolicy, lmpDiagnostics,
                checkPolicy, checkDiagnostics, IterationReuse.OFF, null);
    }

    private ExactSearchAdapter(ExactEvaluator evaluator, TTable table, boolean calibratedPruning,
            int qpolicy, int lmrPolicy, LmrDiagnostics lmrDiagnostics, int lmpPolicy, LmpDiagnostics lmpDiagnostics,
            int checkPolicy, CheckExtensionDiagnostics checkDiagnostics, int reuse, IterationReuse.Diagnostics reuseDiagnostics) {
        this(evaluator, table, calibratedPruning, qpolicy, lmrPolicy, lmrDiagnostics, lmpPolicy, lmpDiagnostics,
                checkPolicy, checkDiagnostics, reuse, reuseDiagnostics, null, null);
    }

    private ExactSearchAdapter(ExactEvaluator evaluator, TTable table, boolean calibratedPruning,
            int qpolicy, int lmrPolicy, LmrDiagnostics lmrDiagnostics, int lmpPolicy, LmpDiagnostics lmpDiagnostics,
            int checkPolicy, CheckExtensionDiagnostics checkDiagnostics, int reuse, IterationReuse.Diagnostics reuseDiagnostics,
            NullMovePolicy nullPolicy, NullMovePolicy.Diagnostics nullDiagnostics) {
        this(evaluator, table, calibratedPruning, qpolicy, lmrPolicy, lmrDiagnostics, lmpPolicy, lmpDiagnostics,
                checkPolicy, checkDiagnostics, reuse, reuseDiagnostics, nullPolicy, nullDiagnostics, null, null);
    }

    private ExactSearchAdapter(ExactEvaluator evaluator, TTable table, boolean calibratedPruning,
            int qpolicy, int lmrPolicy, LmrDiagnostics lmrDiagnostics, int lmpPolicy, LmpDiagnostics lmpDiagnostics,
            int checkPolicy, CheckExtensionDiagnostics checkDiagnostics, int reuse, IterationReuse.Diagnostics reuseDiagnostics,
            NullMovePolicy nullPolicy, NullMovePolicy.Diagnostics nullDiagnostics,
            InternalSearchPolicy internalPolicy, InternalSearchPolicy.Diagnostics internalDiagnostics) {
        Objects.requireNonNull(evaluator, "evaluator");
        ExactEvaluator observed = new ExactEvaluator() {
            @Override public void initialize(long[] board) { evaluator.initialize(board); }
            @Override public void reenter(int ply) {
                if(!control.tryEnterNode()) return;
                nodes++;
                maximumPly = Math.max(maximumPly, ply);
                evaluator.reenter(ply);
            }
            @Override public int evaluate(long[] board, int ply) {
                evaluations++;
                return evaluator.evaluate(board, ply);
            }
            @Override public void child(long[] parent, long[] child, int parentPly) {
                child(parent, child, parentPly, -1);
            }
            @Override public void child(long[] parent, long[] child, int parentPly, int qply) {
                // ExactSearch prepares each legal or synthetic child before its
                // next cancellation checkpoint. Refusing admission sets the
                // control reason; that checkpoint aborts without using child state.
                // Exhausting the budget at the FINAL admitted child does not
                // cancel: a complete iteration is still allowed to unwind.
                // PVS re-search reuses this prepared child/evaluator slot; this
                // hook counts prepared transitions (including probe work), not
                // repeated PVS entries. Synthetic work also consumes the budget.
                if(!control.tryEnterNode()) return;
                nodes++;
                if(qply >= 0) {
                    qnodes++;
                    maximumQply = Math.max(maximumQply, qply);
                }
                maximumPly = Math.max(maximumPly, parentPly + 1);
                evaluator.child(parent, child, parentPly, qply);
            }
        };
        exact = internalPolicy != null ? ExactSearch.internalSearchResearch(observed, table, internalPolicy, internalDiagnostics)
                : nullPolicy != null ? ExactSearch.nullMoveResearch(observed, table, nullPolicy, nullDiagnostics)
                : reuse != IterationReuse.OFF ? ExactSearch.iterationReuseResearch(observed, table, reuse, true, reuseDiagnostics)
                : checkPolicy != ExactSearch.CHECK_OFF || checkDiagnostics != null
                ? ExactSearch.checkExtensionResearch(observed, table, checkPolicy, lmrPolicy, lmpPolicy, checkDiagnostics)
                : lmpPolicy != ExactSearch.LMP_OFF || lmpDiagnostics != null
                ? ExactSearch.lateMovePruningResearch(observed, table, lmpPolicy, lmpDiagnostics)
                : lmrPolicy != ExactSearch.LMR_OFF || lmrDiagnostics != null
                ? ExactSearch.lateMoveReductionResearch(observed, table, lmrPolicy, lmrDiagnostics)
                : qpolicy >= 0 ? ExactSearch.quiescencePvsResearch(observed, table, qpolicy)
                : calibratedPruning ? ExactSearch.withCalibratedPruning(observed, table) : new ExactSearch(observed, table);
    }

    @Override public SearchResult search(SearchRequest request) {
        return searchWindow(request, -ExactSearch.INFINITY, ExactSearch.INFINITY);
    }

    /** Invocation result only: a completed narrow return may still be a bound. */
    SearchResult searchWindow(SearchRequest request, int alpha, int beta) {
        Objects.requireNonNull(request, "request");
        if(active) throw new IllegalStateException("ExactSearchAdapter cannot be re-entered.");
        if(request.depth() > maxSupportedDepth()) throw new IllegalArgumentException("Unsupported depth.");
        active = true;
        control = request.control();
        nodes = evaluations = qnodes = 0;
        maximumPly = maximumQply = 0;
        try {
            request.copyBoardInto(root);
            int count = Gen.genAll(root[0], root[1], root[2], root[3], (int) root[Board.STATUS],
                    root[Board.KEY], true, rootMoves, scratch);
            var result = exact.searchWindow(root, request.gameHistory(), request.depth(), alpha, beta,
                    () -> !control.checkpoint());
            // Thread interruption is also an ExactSearch cancellation source.
            if(!result.completed() && Thread.currentThread().isInterrupted()) control.request(SearchTermination.STOPPED);
            var converted = new SearchResult(result.bestMove(), result.hasMove(), result.score(),
                    request.depth(), nodes, count, result.completed(), result.principalVariation(),
                    diagnostics(request.diagnosticsEnabled()), result.selective());
            request.observer().onSearchFinished(converted, result.elapsedNanos());
            return converted;
        } finally {
            control = null;
            active = false;
        }
    }

    private SearchDiagnosticsSnapshot diagnostics(boolean enabled) {
        if(!enabled) return SearchDiagnosticsSnapshot.disabled();
        var empty = SearchDiagnosticsSnapshot.enabledEmpty();
        var worker = empty.worker();
        return new SearchDiagnosticsSnapshot(true, new WorkerMetrics(
                new NodeMetrics(nodes - qnodes, qnodes, maximumPly, maximumQply, evaluations), worker.transpositionTable(),
                worker.moveOrder(), worker.qsearch(), worker.selective()), empty.iteration());
    }

    @Override public int maxSupportedDepth() { return ExactSearch.MAX_DEPTH; }
    @Override public int activeSearchThreads() { return active ? 1 : 0; }
    @Override public void beginRequest() { exact.beginRequest(); }
    @Override public void endRequest() { exact.endRequest(); }
    @Override public void newGame() { exact.newGame(); }
}
