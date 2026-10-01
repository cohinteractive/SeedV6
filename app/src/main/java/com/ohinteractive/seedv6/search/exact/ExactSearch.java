package com.ohinteractive.seedv6.search.exact;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BooleanSupplier;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.See;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.rules.DrawAdjudicator;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;
import com.ohinteractive.seedv6.search.tt.TTable;

/**
 * Exact recursive negamax: TT-off CONTROL ordered alpha-beta is the independent
 * oracle; TT-on uses SEE/material/main-history ordering, staged lazy generation
 * and PVS with exact mate-domain bounds. Ordinary constructors remain exact.
 * Explicit selective factories enable calibrated static-null and isolated null-move predictions; the production
 * adapter opts in only for HCE. Worker-confined, single threaded; no time policy.
 *
 * Nodes count every entered position including the root, terminal positions and
 * static leaves; an entry refused by cancellation does not count. Input board
 * and immutable game history are preserved. Board.makeMoveInto writes six longs
 * into reusable child storage; no board copies/objects are created per node.
 */
public final class ExactSearch {
    public static final int MATE_SCORE = TranspositionScores.MATE_SCORE;
    public static final int MAX_DEPTH = TranspositionScores.MAX_MATE_PLY;
    public static final int MAX_STATIC_SCORE = TranspositionScores.MAX_NORMAL_SCORE;
    public static final int INFINITY = MATE_SCORE + 1;
    public static final BooleanSupplier NEVER_CANCELLED = () -> false;
    private static final int MAX_MOVES = 512;
    public static final int CONTROL = 0;
    public static final int SEE_TIERED = 1;
    public static final int SEE_TACTICAL = 2;
    public static final int SEE_MATERIAL = 3;
    public static final int SEE_MATERIAL_LVA = 4;
    public static final int SEE_MATERIAL_CAPTURE_HISTORY = 5;
    public static final int SEE_MATERIAL_QUIET_HISTORY = 6;
    public static final int SEE_MATERIAL_CONTINUATION_HISTORY = 7;
    public static final int SEE_MATERIAL_QUIET_HISTORY_KILLERS = 8;
    public static final int SEE_MATERIAL_QUIET_HISTORY_COUNTERMOVE = 9;
    public static final int CURRENT_INSERTION = 0;
    public static final int HANDCRAFTED_FULL_SORT = 1;
    public static final int LAZY_SELECTION = 2;
    public static final int FULL_LAZY = LAZY_SELECTION;
    public static final int LEAF_STAGED_LAZY = 3;
    public static final int STAGED_LAZY = 4;
    public static final int SORT_CROSSOVER = 24;
    public static final int ORDERED_ALPHA_BETA = 0;
    public static final int PVS = 1;
    public static final int QSEARCH_BASELINE = 0;
    public static final int SEE_ALL = 1;
    public static final int SEE_PROMO_SAFE = 2;
    public static final int SEE_CHECK_PROMO_SAFE = 3;
    public static final int QDEPTH_4 = 4;
    public static final int QDEPTH_8 = 8;
    public static final int QDEPTH_12 = 12;
    public static final int QSEARCH_QTT = 16;
    public static final int QSEARCH_QUIET_CHECKS = 32;
    public static final int QCHECK_INITIAL_ONLY = 33;
    public static final int QCHECK_MAX_ONE = 34;
    public static final int QCHECK_MAX_TWO = 35;
    public static final int QCHECK_FORCED_ONE = 36;
    public static final int QCHECK_INITIAL_PLUS_ONE = 37;
    public static final int QCHECK_INITIAL_PLUS_TWO = 38;
    private static final int STATIC_NULL_MARGIN = 960;

    private final boolean reverseFutility;
    private final boolean nullMovePruning;
    private final boolean mateDistancePruning;
    private boolean nullProbeActive;
    private long selectiveEpoch;
    private final ExactEvaluator evaluator;
    private final TTable table;
    private final int ordering;
    private final int mechanics;
    private final int sortCrossover;
    private final int traversal;
    private final boolean quiescence;
    private final int qsearchPruning;
    private final int qdepthLimit;
    private final TTable qtable;
    private final TTable.TEntry qentry;
    // Optional SR-001D diagnostics only: probes, hits, exact, lower/upper cutoffs, save attempts.
    private final long[] qttCounters;
    private final boolean quietChecks;
    private final int quietCheckLimit; // -1 = initial qnode only; otherwise per-path allowance.
    private final int quietCheckReplies; // SR-001G only; zero disables forcingness classification.
    private final boolean broadInitialChecks;
    private final long[] evasionScratch;
    private final long[] forcingBoard; // Only needed to classify at the absolute final board slot.
    // Optional G diagnostics: classified, 0/1/2/3+ replies, required calls/moves,
    // broad-initial diagnostic-only calls/moves, maximum ordinary checks used on a path.
    private final long[] forcingCounters;
    // Optional SR-001E diagnostics: nodes with checks, searched checks, check cutoffs, longest chain.
    // SR-001F adds first/second checks searched, allowance-closed nodes, max used, generated checks.
    private final long[] quietCheckCounters;
    private final int[] quietCheckRuns;
    // Lazy keys survive child recursion. Full sorting reuses materialScores instead.
    private final int[] selectionKeys;
    private final TTable.TEntry entry;
    private final long[] historyKeys;
    private int generation;
    private boolean requestActive;
    private final long[][] boards = new long[MAX_DEPTH + 1][Board.MAX_BITBOARDS];
    private final long[][] moves = new long[MAX_DEPTH + 1][MAX_MOVES];
    private final long[][] pv = new long[MAX_DEPTH + 1][MAX_DEPTH];
    private final int[] pvLength = new int[MAX_DEPTH + 1];
    // Generator and ordering scratch are used only before descending to children.
    private final long[] generatorScratch = new long[Board.MAX_BITBOARDS];
    private final long[] quietScratch = new long[MAX_MOVES];
    private final long[] badTacticalScratch;
    private final int[] materialScores;
    private final int[] captureHistory;
    private final int[] quietHistory;
    private final short[] continuationHistory;
    private final long[] killers;
    private final long[] countermoves;
    private SearchLineHistory history;
    private BooleanSupplier cancelled;
    private long nodes;
    private long qnodes;
    private int maximumQply;
    private boolean active;
    private boolean rootDomainBound;

    /**
     * SR-019 selective execution, with ordinary mathematical TT entries only.
     * The caller must supply an evaluator for which this numerical policy has
     * been validated. Current adoption is HCE only; ordinary constructors and
     * the independently invocable TT-off oracle never enable this mechanism.
     */
    public static ExactSearch withStaticNullPruning(ExactEvaluator evaluator, TTable table) {
        return new ExactSearch(evaluator, Objects.requireNonNull(table), SEE_MATERIAL_QUIET_HISTORY,
                STAGED_LAZY, SORT_CROSSOVER, PVS, false, QSEARCH_BASELINE, false, true, false, true);
    }

    /**
     * Calibrated HCE SR-018 policy composed with the unchanged SR-019 policy.
     * As with the static-null factory, calibrated evaluation is a caller
     * precondition; unknown/neural definitions must use ordinary constructors.
     */
    public static ExactSearch withCalibratedPruning(ExactEvaluator evaluator, TTable table) {
        return new ExactSearch(evaluator, Objects.requireNonNull(table), SEE_MATERIAL_QUIET_HISTORY,
                STAGED_LAZY, SORT_CROSSOVER, PVS, false, QSEARCH_BASELINE, false, true, true, true);
    }

    public ExactSearch() { this(SearchEvaluation.handcrafted()); }

    public ExactSearch(SearchEvaluation evaluation) { this(ExactEvaluator.from(evaluation)); }

    public ExactSearch(ExactEvaluator evaluator) { this(evaluator, null); }

    public ExactSearch(SearchEvaluation evaluation, TTable table) { this(ExactEvaluator.from(evaluation), table); }

    /**
     * Transfers exclusive ownership of a fresh table (generation zero), or null
     * for TT-off. The evaluator must give the same board the same static value;
     * ply selects its stack slot, not a different evaluation function.
     */
    public ExactSearch(ExactEvaluator evaluator, TTable table) {
        this(evaluator, table, table == null ? CONTROL : SEE_MATERIAL_QUIET_HISTORY,
                table == null ? CURRENT_INSERTION : STAGED_LAZY, SORT_CROSSOVER,
                table == null ? ORDERED_ALPHA_BETA : PVS,
                false, QSEARCH_BASELINE, false, false, false, table != null);
    }

    /** Explicit research/control seam; retains full generation and ordered alpha-beta. */
    public ExactSearch(ExactEvaluator evaluator, TTable table, int ordering) {
        this(evaluator, table, ordering, CURRENT_INSERTION);
    }

    public ExactSearch(ExactEvaluator evaluator, TTable table, int ordering, int mechanics) {
        this(evaluator, table, ordering, mechanics, SORT_CROSSOVER);
    }

    /** Experimental SR-017 seam; alternate mechanics apply only to the accepted history policy. */
    public ExactSearch(ExactEvaluator evaluator, TTable table, int ordering, int mechanics, int sortCrossover) {
        this(evaluator, table, ordering, mechanics, sortCrossover, ORDERED_ALPHA_BETA);
    }

    /** Explicit research traversal without SR-011, preserving independent mechanical controls.
     * Normal TT-enabled construction selects PVS and mate-distance pruning above. */
    public ExactSearch(ExactEvaluator evaluator, TTable table, int ordering, int mechanics, int sortCrossover,
                       int traversal) {
        this(evaluator, table, ordering, mechanics, sortCrossover, traversal, false, QSEARCH_BASELINE, false);
    }

    /**
     * SR-001A semantic reference only. Normal nodes retain TT-off CONTROL ordered
     * alpha-beta; leaves use unpruned, TT-free qsearch. No production caller opts in.
     */
    public static ExactSearch quiescenceResearch(ExactEvaluator evaluator) {
        return quiescenceResearch(evaluator, QSEARCH_BASELINE);
    }

    /** Independent SR-001 research alternatives; no combinations or production opt-in. */
    public static ExactSearch quiescenceResearch(ExactEvaluator evaluator, int pruning) {
        return quiescenceResearch(evaluator, pruning, false);
    }

    /** Optional qTT/quiet-check research diagnostics are excluded from clean timing runs. */
    public static ExactSearch quiescenceResearch(ExactEvaluator evaluator, int pruning, boolean diagnostics) {
        if((pruning < QSEARCH_BASELINE || pruning > SEE_CHECK_PROMO_SAFE)
                && pruning != QDEPTH_4 && pruning != QDEPTH_8 && pruning != QDEPTH_12
                && pruning != QSEARCH_QTT && (pruning < QSEARCH_QUIET_CHECKS || pruning > QCHECK_INITIAL_PLUS_TWO))
            throw new IllegalArgumentException("Unknown qsearch pruning candidate.");
        if(diagnostics && pruning != QSEARCH_QTT && (pruning < QSEARCH_QUIET_CHECKS || pruning > QCHECK_INITIAL_PLUS_TWO))
            throw new IllegalArgumentException("Diagnostics require qTT or quiet-check research.");
        return new ExactSearch(evaluator, null, CONTROL, CURRENT_INSERTION, SORT_CROSSOVER,
                ORDERED_ALPHA_BETA, true, pruning, diagnostics);
    }

    private ExactSearch(ExactEvaluator evaluator, TTable table, int ordering, int mechanics, int sortCrossover,
                        int traversal, boolean quiescence, int qsearchPruning, boolean diagnostics) {
        this(evaluator, table, ordering, mechanics, sortCrossover, traversal, quiescence, qsearchPruning, diagnostics, false);
    }

    private ExactSearch(ExactEvaluator evaluator, TTable table, int ordering, int mechanics, int sortCrossover,
                        int traversal, boolean quiescence, int qsearchPruning, boolean diagnostics, boolean reverseFutility) {
        this(evaluator, table, ordering, mechanics, sortCrossover, traversal, quiescence,
                qsearchPruning, diagnostics, reverseFutility, false);
    }

    private ExactSearch(ExactEvaluator evaluator, TTable table, int ordering, int mechanics, int sortCrossover,
                        int traversal, boolean quiescence, int qsearchPruning, boolean diagnostics,
                        boolean reverseFutility, boolean nullMovePruning) {
        this(evaluator, table, ordering, mechanics, sortCrossover, traversal, quiescence,
                qsearchPruning, diagnostics, reverseFutility, nullMovePruning, false);
    }

    private ExactSearch(ExactEvaluator evaluator, TTable table, int ordering, int mechanics, int sortCrossover,
                        int traversal, boolean quiescence, int qsearchPruning, boolean diagnostics,
                        boolean reverseFutility, boolean nullMovePruning, boolean mateDistancePruning) {
        this.reverseFutility = reverseFutility;
        this.nullMovePruning = nullMovePruning;
        this.mateDistancePruning = mateDistancePruning;
        if(ordering != CONTROL && ordering != SEE_TIERED && ordering != SEE_TACTICAL
                && ordering != SEE_MATERIAL && ordering != SEE_MATERIAL_LVA
                && ordering != SEE_MATERIAL_CAPTURE_HISTORY && ordering != SEE_MATERIAL_QUIET_HISTORY
                && ordering != SEE_MATERIAL_CONTINUATION_HISTORY && ordering != SEE_MATERIAL_QUIET_HISTORY_KILLERS
                && ordering != SEE_MATERIAL_QUIET_HISTORY_COUNTERMOVE)
            throw new IllegalArgumentException("Unknown ordering mode.");
        if(mechanics < CURRENT_INSERTION || mechanics > STAGED_LAZY
                || (mechanics != CURRENT_INSERTION && ordering != SEE_MATERIAL_QUIET_HISTORY))
            throw new IllegalArgumentException("Mechanics require see-material-quiet-history.");
        if(sortCrossover < 2 || sortCrossover > MAX_MOVES)
            throw new IllegalArgumentException("Sort crossover must be 2..512.");
        if(traversal != ORDERED_ALPHA_BETA && traversal != PVS)
            throw new IllegalArgumentException("Unknown search traversal.");
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
        this.table = table;
        this.ordering = ordering;
        this.mechanics = mechanics;
        this.sortCrossover = sortCrossover;
        this.traversal = traversal;
        this.quiescence = quiescence;
        this.qsearchPruning = qsearchPruning <= SEE_CHECK_PROMO_SAFE ? qsearchPruning : QSEARCH_BASELINE;
        qdepthLimit = qsearchPruning == QDEPTH_4 || qsearchPruning == QDEPTH_8 || qsearchPruning == QDEPTH_12
                ? qsearchPruning : Integer.MAX_VALUE;
        // Physically separate value domain; accepted default 64 MiB requested substrate.
        qtable = quiescence && qsearchPruning == QSEARCH_QTT ? new TTable() : null;
        qentry = qtable == null ? null : new TTable.TEntry();
        qttCounters = diagnostics && qtable != null ? new long[6] : null;
        quietChecks = quiescence && qsearchPruning >= QSEARCH_QUIET_CHECKS && qsearchPruning <= QCHECK_INITIAL_PLUS_TWO;
        quietCheckLimit = qsearchPruning == QCHECK_INITIAL_ONLY ? -1
                : qsearchPruning == QCHECK_MAX_ONE ? 1 : qsearchPruning == QCHECK_MAX_TWO ? 2 : Integer.MAX_VALUE;
        quietCheckReplies = qsearchPruning == QCHECK_INITIAL_PLUS_TWO ? 2
                : qsearchPruning == QCHECK_FORCED_ONE || qsearchPruning == QCHECK_INITIAL_PLUS_ONE ? 1 : 0;
        broadInitialChecks = qsearchPruning == QCHECK_INITIAL_PLUS_ONE || qsearchPruning == QCHECK_INITIAL_PLUS_TWO;
        evasionScratch = quietCheckReplies == 0 ? null : new long[MAX_MOVES];
        forcingBoard = quietCheckReplies == 0 ? null : new long[Board.MAX_BITBOARDS];
        forcingCounters = diagnostics && quietCheckReplies != 0 ? new long[10] : null;
        quietCheckCounters = diagnostics && quietChecks ? new long[qsearchPruning >= QCHECK_INITIAL_ONLY && qsearchPruning <= QCHECK_MAX_TWO ? 9 : 4] : null;
        quietCheckRuns = quietCheckCounters == null ? null : new int[MAX_DEPTH + 1];
        selectionKeys = mechanics >= LAZY_SELECTION || quiescence ? new int[MAX_DEPTH * MAX_MOVES] : null;
        badTacticalScratch = mechanics == CURRENT_INSERTION && ordering != CONTROL ? new long[MAX_MOVES] : null;
        materialScores = ordering >= SEE_MATERIAL && mechanics <= HANDCRAFTED_FULL_SORT ? new int[MAX_MOVES] : null;
        captureHistory = ordering == SEE_MATERIAL_CAPTURE_HISTORY ? new int[CaptureHistory.SIZE] : null;
        quietHistory = ordering == SEE_MATERIAL_QUIET_HISTORY || ordering == SEE_MATERIAL_CONTINUATION_HISTORY
                || ordering == SEE_MATERIAL_QUIET_HISTORY_KILLERS || ordering == SEE_MATERIAL_QUIET_HISTORY_COUNTERMOVE
                ? new int[QuietHistory.SIZE] : null;
        continuationHistory = ordering == SEE_MATERIAL_CONTINUATION_HISTORY ? new short[ContinuationHistory.SIZE] : null;
        killers = ordering == SEE_MATERIAL_QUIET_HISTORY_KILLERS ? new long[(MAX_DEPTH + 1) * KillerMoves.SLOTS] : null;
        countermoves = ordering == SEE_MATERIAL_QUIET_HISTORY_COUNTERMOVE ? new long[Countermoves.SIZE] : null;
        entry = table == null ? null : new TTable.TEntry();
        historyKeys = table == null && qtable == null ? null : new long[MAX_DEPTH + 1];
    }

    /** One driver request may contain several fixed-depth invocations. */
    public void beginRequest() {
        if(active || requestActive) throw new IllegalStateException("Search request already active.");
        advanceGeneration();
        requestActive = true;
    }

    public void endRequest() {
        if(active || !requestActive) throw new IllegalStateException("No idle Search request.");
        requestActive = false;
    }

    public void newGame() {
        if(active || requestActive) throw new IllegalStateException("Search request active.");
        if(table != null) table.clear();
    }

    private void advanceGeneration() {
        if(table != null) {
            table.advanceGeneration();
            generation = (generation + 1) & 255;
        }
    }

    /** A FEN/standalone board has only its initial history; callers with game history must supply it. */
    public ExactSearchResult search(long[] board, int depth) {
        return search(board, GameHistory.initial(board), depth, NEVER_CANCELLED);
    }

    /** Worker-thread observation for an external research node-budget cancellation supplier. */
    public long visitedNodes() { return nodes; }

    /** Research snapshot, outside recursion: probes, hits, EXACT, LOWER, UPPER, save attempts. */
    public long[] qttDiagnostics() {
        if(qttCounters == null) throw new IllegalStateException("qTT diagnostics are disabled.");
        return qttCounters.clone();
    }

    /** Research-only counters. The chain counts added quiet checks separated only by quiet evasions.
     * F's closed-node count is eligibility evidence, not a counterfactual count of useful omitted moves. */
    public long[] quietCheckDiagnostics() {
        if(quietCheckCounters == null) throw new IllegalStateException("Quiet-check diagnostics are disabled.");
        return quietCheckCounters.clone();
    }

    /** SR-001G only; returned snapshots are outside recursive Search. */
    public long[] forcingDiagnostics() {
        if(forcingCounters == null) throw new IllegalStateException("Forcingness diagnostics are disabled.");
        return forcingCounters.clone();
    }

    /**
     * Cancellation may come from any thread via a safely published supplier,
     * e.g. () -> !searchControl.checkpoint(). Thread interruption is also honored
     * without clearing the flag. No time-management policy is owned here.
     */
    public ExactSearchResult search(long[] board, GameHistory gameHistory, int depth, BooleanSupplier cancelled) {
        return searchWindow(board, gameHistory, depth, -INFINITY, INFINITY, cancelled);
    }

    public ExactSearchResult searchWindow(long[] board, GameHistory gameHistory, int depth,
                                          int alpha, int beta, BooleanSupplier cancelled) {
        return searchWindow(board, gameHistory, depth, alpha, beta, cancelled, -1, 0);
    }

    /** Research PV validation only: retain consumed qplies when re-searching a qline suffix.
     * Scores/mate distance and returned node/ply statistics are relative to this new root. */
    public ExactSearchResult searchQuiescenceSuffix(long[] board, GameHistory gameHistory, int consumedQplies,
                                                   BooleanSupplier cancelled) {
        return searchQuiescenceSuffix(board, gameHistory, consumedQplies, 0, cancelled);
    }

    /** SR-001F suffix validation also retains the ordinary quiet checks already used on this path. */
    public ExactSearchResult searchQuiescenceSuffix(long[] board, GameHistory gameHistory, int consumedQplies,
                                                   int quietChecksUsed, BooleanSupplier cancelled) {
        if(!quiescence || consumedQplies < 0 || consumedQplies > MAX_DEPTH)
            throw new IllegalArgumentException("Requires qsearch and consumed qplies in 0..256.");
        if(quietChecksUsed < 0 || quietChecksUsed > consumedQplies)
            throw new IllegalArgumentException("Quiet checks used must be in 0..consumed qplies.");
        return searchWindow(board, gameHistory, 0, -INFINITY, INFINITY, cancelled, consumedQplies, quietChecksUsed);
    }

    private ExactSearchResult searchWindow(long[] board, GameHistory gameHistory, int depth,
                                           int alpha, int beta, BooleanSupplier cancelled, int consumedQplies, int quietChecksUsed) {
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(gameHistory, "gameHistory");
        Objects.requireNonNull(cancelled, "cancelled");
        if(board.length < Board.MAX_BITBOARDS) throw new IllegalArgumentException("Incomplete board.");
        if(depth < 0 || depth > MAX_DEPTH) throw new IllegalArgumentException("Depth must be 0.." + MAX_DEPTH);
        if(alpha < -INFINITY || beta > INFINITY || alpha >= beta) throw new IllegalArgumentException("Invalid window.");
        if(active) throw new IllegalStateException("ExactSearch cannot be re-entered.");
        gameHistory.requireCurrent(board);
        if(!requestActive) advanceGeneration();
        long start = System.nanoTime();
        active = true;
        nodes = 0;
        rootDomainBound = false;
        selectiveEpoch = 0;
        nullProbeActive = false;
        qnodes = 0;
        maximumQply = 0;
        this.cancelled = cancelled;
        try {
            // Invocation-local history, even within one driver request. Broader lifecycle remains OPEN.
            if(captureHistory != null) Arrays.fill(captureHistory, 0);
            if(quietHistory != null) Arrays.fill(quietHistory, 0);
            if(continuationHistory != null) Arrays.fill(continuationHistory, (short) 0);
            if(killers != null) Arrays.fill(killers, 0);
            if(countermoves != null) Arrays.fill(countermoves, 0);
            if(qttCounters != null) Arrays.fill(qttCounters, 0);
            if(quietCheckCounters != null) Arrays.fill(quietCheckCounters, 0);
            if(forcingCounters != null) Arrays.fill(forcingCounters, 0);
            checkpoint();
            // No reuse across fixed-depth invocations, even inside one driver request.
            if(qtable != null) qtable.clear();
            System.arraycopy(board, 0, boards[0], 0, Board.MAX_BITBOARDS);
            history = new SearchLineHistory(gameHistory, quiescence ? MAX_DEPTH : depth);
            if(historyKeys != null) historyKeys[0] = SearchKey.rootHistory(board, gameHistory);
            evaluator.initialize(boards[0]);
            int score = consumedQplies < 0 ? negamax(depth, 0, alpha, beta, 0, false)
                    : quiescence(0, consumedQplies, alpha, beta, quietChecksUsed);
            checkpoint();
            // Conservative provenance: even a prediction later made irrelevant
            // prevents claiming this completed invocation as entirely exact.
            boolean selective = selectiveEpoch != 0;
            if(table != null && depth > 0 && !selective && !rootDomainBound) store(SearchKey.key(boards[0], historyKeys[0]), depth, 0,
                    alpha, beta, score, pvLength[0] == 0 ? 0 : pv[0][0]);
            long[] line = Arrays.copyOf(pv[0], pvLength[0]);
            return new ExactSearchResult(depth, true, line.length == 0 ? 0 : line[0], score,
                    line, nodes, System.nanoTime() - start, qnodes, Math.max(0, maximumQply - Math.max(0, consumedQplies)), selective);
        } catch(Aborted ignored) {
            if(quietHistory != null) Arrays.fill(quietHistory, 0);
            if(continuationHistory != null) Arrays.fill(continuationHistory, (short) 0);
            if(killers != null) Arrays.fill(killers, 0);
            if(countermoves != null) Arrays.fill(countermoves, 0);
            return new ExactSearchResult(depth, false, 0, Value.INVALID, new long[0],
                    nodes, System.nanoTime() - start, qnodes, Math.max(0, maximumQply - Math.max(0, consumedQplies)));
        } finally {
            history = null;
            this.cancelled = null;
            active = false;
        }
    }

    private int negamax(int depth, int ply, int alpha, int beta, long previousMove, boolean scout) {
        // The boundary position belongs to qsearch exactly once, including terminal leaves.
        if(quiescence && depth <= 0) return quiescence(ply, 0, alpha, beta);
        checkpoint();
        nodes++;
        pvLength[ply] = 0;
        long[] board = boards[ply];
        int status = (int) board[Board.STATUS];
        long[] legalMoves = moves[ply];
        int count;
        boolean quietPending = false;
        boolean checked = false;
        if(mechanics < LEAF_STAGED_LAZY || (mechanics == LEAF_STAGED_LAZY && depth > 0)) {
            count = Gen.genAll(board[0], board[1], board[2], board[3], status,
                    board[Board.KEY], true, legalMoves, generatorScratch);
        } else {
            long checkers = checkers(board, status);
            checked = checkers != 0;
            if(checkers != 0) {
                // Complete legal evasions: no fragile partial in-check terminal decision.
                count = Gen.genEvasion(board[0], board[1], board[2], board[3], status,
                        board[Board.KEY], true, checkers, legalMoves, generatorScratch);
            } else if(depth <= 0) {
                // Static leaves consume only existence, never the move list.
                count = Gen.hasLegalMoveNotInCheck(board[0], board[1], board[2], board[3], status) ? 1 : 0;
            } else {
                count = Gen.genTactical(board[0], board[1], board[2], board[3], status,
                        board[Board.KEY], true, legalMoves, generatorScratch);
                if(count == 0) {
                    // An empty tactical subset is not evidence of stalemate.
                    count = Gen.genQuiet(board[0], board[1], board[2], board[3], status,
                            board[Board.KEY], true, legalMoves, generatorScratch);
                } else quietPending = true;
            }
        }
        // Mate/stalemate precede claimable rule draws and the nominal horizon.
        if(count == 0) {
            return Board.isPlayerInCheck(board[0], board[1], board[2], board[3], Board.player(status))
                    ? -MATE_SCORE + ply : 0;
        }
        if(DrawAdjudicator.adjudicateNonTerminal(board, history) != DrawAdjudicator.RuleDraw.NONE) return 0;
        // R007: the current static boundary resolves before any Search TT evidence.
        if(depth <= 0) {
            int score = evaluator.evaluate(board, ply);
            if(score < -MAX_STATIC_SCORE || score > MAX_STATIC_SCORE) {
                throw new IllegalArgumentException("Static score enters the reserved mate band: " + score);
            }
            return score;
        }
        final int originalAlpha = alpha;
        final int originalBeta = beta;
        long key = 0;
        long hashMove = 0;
        if(table != null && !nullProbeActive) {
            key = SearchKey.key(board, historyKeys[ply]);
            if(table.probe(key, entry)) {
                hashMove = entry.hashMove; // Candidate only; validate when a PV or ordering uses it.
                if(quietPending && hashMove != 0
                        && !CaptureHistory.isTactical(hashMove, Board.enPassantSquare(status))) {
                    // A quiet hint forces complete exact membership validation, even for a TT PV prefix.
                    count = appendQuiets(board, legalMoves, count);
                    quietPending = false;
                }
                long data = entry.data;
                if((data & 255) == depth && ((data >>> 10) & 255) == generation) {
                    int score = TranspositionScores.fromTableScore((int) (data >> 32), ply);
                    int type = (int) (data >>> 8) & 3;
                    if(type == TTable.TYPE_EXACT || (type == TTable.TYPE_LOWER && score >= beta)
                            || (type == TTable.TYPE_UPPER && score <= alpha)) {
                        // Ordinary TT root resolution requires a validated move prefix.
                        // SR-011's separate narrow domain-only return is handled below.
                        if(hashMove != 0) {
                            for(int i = 0; i < count; i++) {
                                if(legalMoves[i] == hashMove) {
                                    pv[ply][0] = hashMove;
                                    pvLength[ply] = 1;
                                    break;
                                }
                            }
                        }
                        if(ply != 0 || pvLength[ply] != 0) return score;
                        hashMove = 0; // The positive-depth root still needs a legal best move.
                    }
                }
            }
        }
        // SR-011: post-terminal, positive static-depth score domain. TT-off stays
        // the independent unmodified oracle; qsearch research also has no ordinary table.
        // P + D <= MAX_DEPTH keeps every reachable mate outside the ordinary band.
        if(mateDistancePruning) {
            int upper = MATE_SCORE - ply - 1;
            int lower = depth == 1 ? -MAX_STATIC_SCORE : -MATE_SCORE + ply + 2;
            if(upper <= originalAlpha) {
                if(ply == 0) rootDomainBound = true;
                return upper; // UPPER, no searched move/PV and no position-specific TT store.
            }
            if(lower >= originalBeta) {
                if(ply == 0) rootDomainBound = true;
                return lower; // LOWER; a numeric mate-band threshold is not an exact mate distance.
            }
            if(alpha < lower) alpha = lower;
            if(beta > upper) beta = upper;
            // Non-cutting original-window TT bounds are deliberately not reconsidered.
            // Normal searched results below still classify/store against originalAlpha/Beta.
        }
        // Legal existence/draws, static leaves and applicable exact TT evidence
        // have resolved first. No positive-depth raw E was naturally available.
        final long entryEpoch = selectiveEpoch;
        // Eligibility is separate from aggression. A scout is actual call
        // provenance, not an inferred node role. Exclude the rule-50 horizon.
        // Original caller windows preserve calibration after exact domain tightening.
        // The upper-window guard also avoids an E call that cannot possibly cut.
        if(reverseFutility && !nullProbeActive && scout && depth == 2 && !checked
                && Board.halfMoveClock(status) < DrawAdjudicator.FIFTY_MOVE_HALFMOVES - 2
                && originalAlpha >= -MAX_STATIC_SCORE && originalBeta <= MAX_STATIC_SCORE - STATIC_NULL_MARGIN) {
            int staticEval = evaluator.evaluate(board, ply);
            if(staticEval < -MAX_STATIC_SCORE || staticEval > MAX_STATIC_SCORE)
                throw new IllegalArgumentException("Static score enters the reserved mate band: " + staticEval);
            if(staticEval - STATIC_NULL_MARGIN >= originalBeta) {
                checkpoint();
                selectiveEpoch++;
                // Predict only the threshold; neither E nor an omitted PV is
                // a searched value. pvLength[ply] is still zero.
                return originalBeta;
            }
        }
        // SR-018: actual scouts only, above SR-019, within measured depth bands.
        // The pass and its entire subtree have no ordinary TT participation.
        if(nullMovePruning && !nullProbeActive && scout && depth >= 4 && depth <= 6 && !checked
                && Board.halfMoveClock(status) + depth < DrawAdjudicator.FIFTY_MOVE_HALFMOVES
                && originalAlpha >= -MAX_STATIC_SCORE && originalBeta <= MAX_STATIC_SCORE - 512
                && hasNonPawnMaterial(board, status)) {
            int staticEval = evaluator.evaluate(board, ply);
            if(staticEval < -MAX_STATIC_SCORE || staticEval > MAX_STATIC_SCORE)
                throw new IllegalArgumentException("Static score enters the reserved mate band: " + staticEval);
            if(staticEval - originalBeta >= 512) {
                long[] synthetic = boards[ply + 1];
                syntheticPass(board, synthetic);
                evaluator.child(board, synthetic, ply);
                history.enterSyntheticPosition(synthetic);
                nullProbeActive = true;
                int prediction;
                try {
                    // One pass ply plus fixed deliberate reduction R=2.
                    prediction = -negamax(depth - 3, ply + 1, -originalBeta, -originalBeta + 1, 0, false);
                } finally {
                    nullProbeActive = false;
                    history.leaveSyntheticPosition();
                }
                checkpoint();
                if(prediction >= originalBeta) {
                    selectiveEpoch++;
                    return originalBeta; // No synthetic PV or real-position fail-soft score.
                }
            }
        }
        int continuationContext = continuationHistory == null ? -1 : ContinuationHistory.context(previousMove);
        int countermoveContext = countermoves == null ? -1 : Countermoves.context(previousMove);
        int keyBase = ply * MAX_MOVES;
        if(mechanics == CURRENT_INSERTION) {
            if(ordering == CONTROL) orderTacticalFirst(legalMoves, count, status);
            else orderSeeClassified(board, legalMoves, count, status, hashMove, continuationContext, ply, countermoveContext);
            if(hashMove != 0) promoteHashMove(legalMoves, count, hashMove);
        } else if(mechanics == HANDCRAFTED_FULL_SORT) {
            snapshotOrdering(board, legalMoves, count, hashMove, materialScores, 0);
            Sort.full(legalMoves, materialScores, 0, count, sortCrossover);
        } else {
            snapshotOrdering(board, legalMoves, count, hashMove, selectionKeys, keyBase);
        }
        int best = -INFINITY;
        long[] child = boards[ply + 1];
        for(int i = 0; i < count || quietPending; i++) {
            checkpoint();
            if(i == count) {
                int from = count;
                count = appendQuiets(board, legalMoves, count);
                quietPending = false;
                // Deliberately sample history now, after tactical siblings. No history rollback/copy.
                for(int j = from; j < count; j++) {
                    int evidence = quietHistory[QuietHistory.index(legalMoves[j])];
                    selectionKeys[keyBase + j] = ((evidence + QuietHistory.LIMIT) << 9) | (511 - j);
                }
                if(i == count) break;
            }
            if(mechanics >= LAZY_SELECTION) Sort.next(legalMoves, selectionKeys, keyBase, i, count);
            long move = legalMoves[i];
            Board.makeMoveInto(board[0], board[1], board[2], board[3], status, board[Board.KEY], move, child);
            evaluator.child(board, child, ply);
            history.pushRealPosition(child);
            if(historyKeys != null && !nullProbeActive) historyKeys[ply + 1] = SearchKey.childHistory(
                    historyKeys[ply], history.currentKey(), (int) child[Board.STATUS]);
            int score;
            boolean fullSearch = true;
            try {
                if(traversal == PVS && i > 0) {
                    // Validated windows stay within +/-32769; alpha < beta implies
                    // alpha + 1 <= beta. Neither addition nor negation can overflow,
                    // including in the reserved mate band. Searched scores are not clamped.
                    fullSearch = false;
                    score = -negamax(depth - 1, ply + 1, -alpha - 1, -alpha, move, true);
                    if(score > alpha && score < beta) {
                        // Reported scout improvement requires a full re-search.
                        // Selective descendants do not provide mathematical proof.
                        // Keep the same board, evaluator slot and real-history push.
                        fullSearch = true;
                        score = -negamax(depth - 1, ply + 1, -beta, -alpha, move, false);
                    }
                } else {
                    score = -negamax(depth - 1, ply + 1, -beta, -alpha, move, false);
                }
            } finally {
                history.popRealPosition();
            }
            if(score > best) {
                best = score;
                // Fail-low scout bounds may improve a fail-low return, but never
                // replace its discovered line. A scout cutoff exposes only its
                // legal move prefix, not an allegedly exact full-window child PV.
                if(fullSearch || score >= beta) {
                    pv[ply][0] = move;
                    int length = fullSearch ? pvLength[ply + 1] : 0;
                    System.arraycopy(pv[ply + 1], 0, pv[ply], 1, length);
                    pvLength[ply] = 1 + length;
                }
            }
            if(score >= beta) {
                if(quietHistory != null && !nullProbeActive) {
                    checkpoint(); // A final child may have raised cancellation, even with TT off.
                    QuietHistory.recordCutoff(quietHistory, legalMoves, i, Board.enPassantSquare(status), depth);
                    if(continuationHistory != null) ContinuationHistory.recordCutoff(continuationHistory,
                            continuationContext, legalMoves, i, Board.enPassantSquare(status), depth);
                    if(killers != null) KillerMoves.recordCutoff(killers, ply, move, Board.enPassantSquare(status));
                    if(countermoves != null) Countermoves.recordCutoff(countermoves, countermoveContext, move,
                            Board.enPassantSquare(status));
                }
                if(captureHistory != null) CaptureHistory.recordCutoff(captureHistory, legalMoves, i,
                        Board.enPassantSquare(status), depth);
                return completed(key, depth, ply, originalAlpha, originalBeta, score, move, entryEpoch);
            }
            if(score > alpha) alpha = score;
        }
        return completed(key, depth, ply, originalAlpha, originalBeta, best, pv[ply][0], entryEpoch);
    }

    /**
     * SR-001A: stand-pat is an abstract stop option, never a real/pass move.
     * Checked nodes use every evasion in generated order, without history effects.
     * Non-check nodes use exactly legal tacticals and the accepted SR-015 lazy keys.
     * SR-001E/F/G append eligible legal quiet checks in generated order after those tacticals.
     */
    private int quiescence(int ply, int qply, int alpha, int beta) {
        return quiescence(ply, qply, alpha, beta, 0);
    }

    private int quiescence(int ply, int qply, int alpha, int beta, int quietChecksUsed) {
        checkpoint();
        nodes++;
        qnodes++;
        maximumQply = Math.max(maximumQply, qply);
        if(quietCheckRuns != null && qply == 0) quietCheckRuns[ply] = 0;
        pvLength[ply] = 0;
        long[] board = boards[ply];
        int status = (int) board[Board.STATUS];
        long[] legalMoves = moves[ply];
        long checking = checkers(board, status);
        boolean checked = checking != 0;
        int count = checked
                ? Gen.genEvasion(board[0], board[1], board[2], board[3], status,
                        board[Board.KEY], true, checking, legalMoves, generatorScratch)
                : Gen.genTactical(board[0], board[1], board[2], board[3], status,
                        board[Board.KEY], true, legalMoves, generatorScratch);
        // Same precedence as static ExactSearch: mate/stalemate, then rule draws.
        // Quiets establish existence; SR-001E may later consume their checking subset.
        int quietCount = -1;
        if(count == 0) {
            if(checked) return -MATE_SCORE + ply;
            quietCount = Gen.genQuiet(board[0], board[1], board[2], board[3], status,
                    board[Board.KEY], true, quietScratch, generatorScratch);
            if(quietCount == 0) return 0;
        }
        if(DrawAdjudicator.adjudicateNonTerminal(board, history) != DrawAdjudicator.RuleDraw.NONE) return 0;
        final boolean allowQuietChecks = quietChecks && (quietCheckLimit < 0 ? qply == 0 : quietChecksUsed < quietCheckLimit);
        if(quietCheckCounters != null && !checked) {
            // Diagnostic run only: count existence even when stand-pat/tacticals will cut.
            // Its extra make/check work is intentionally excluded from clean timing runs.
            if(!allowQuietChecks) quietCheckCounters[6]++; // Nonterminal noncheck nodes whose allowance is closed.
            else {
                if(quietCount < 0) quietCount = Gen.genQuiet(board[0], board[1], board[2], board[3], status,
                        board[Board.KEY], true, quietScratch, generatorScratch);
                if(quietCheckCounters.length == 4) {
                    if(hasQuietCheck(board, quietCount)) quietCheckCounters[0]++;
                } else {
                    int checks = countQuietChecks(board, quietCount);
                    if(checks != 0) quietCheckCounters[0]++;
                    quietCheckCounters[8] += checks;
                }
            }
        }
        final int originalAlpha = alpha;
        final int originalBeta = beta;
        long key = 0;
        if(qtable != null) {
            key = SearchKey.key(board, historyKeys[ply]);
            if(qttCounters != null) qttCounters[0]++;
            if(qtable.probe(key, qentry)) {
                if(qttCounters != null) qttCounters[1]++;
                long data = qentry.data;
                // Constant depth zero belongs only to this isolated unlimited-qsearch domain.
                if((data & 255) == 0 && ((data >>> 10) & 255) == 0) {
                    int score = TranspositionScores.fromTableScore((int)(data >> 32), ply);
                    int type = (int)(data >>> 8) & 3;
                    if(type == TTable.TYPE_EXACT || (type == TTable.TYPE_LOWER && score >= beta)
                            || (type == TTable.TYPE_UPPER && score <= alpha)) {
                        if(qttCounters != null) qttCounters[2 + type]++;
                        // Honest empty suffix: no hash move is consumed or fabricated continuation exposed.
                        return score;
                    }
                }
                // Non-cutting bounds neither tighten the window nor change ordering.
            }
        }
        int best = -INFINITY;
        if(!checked) {
            best = evaluator.evaluate(board, ply);
            if(best < -MAX_STATIC_SCORE || best > MAX_STATIC_SCORE)
                throw new IllegalArgumentException("Static score enters the reserved mate band: " + best);
            // SR-001C: nominal qhorizon, after legal-existence/draw resolution.
            // Generation above is only adjudication at this boundary: no ordering,
            // child preparation or tactical recursion follows. Checks cannot stop here.
            if(best >= beta || (count == 0 && !allowQuietChecks) || qply >= qdepthLimit)
                return qcompleted(key, ply, originalAlpha, originalBeta, best, 0);
            if(best > alpha) alpha = best;
        }
        // Existing absolute storage/mate-domain boundary, NOT a score-producing qdepth cap.
        // Resolved terminals and stand-pat proofs above are valid; unresolved move search
        // at the last slot must unwind through the ordinary incomplete-result path.
        if(ply == MAX_DEPTH) {
            if(allowQuietChecks && !checked && count == 0
                    && !(quietCheckReplies == 0 ? hasQuietCheck(board, quietCount)
                        : hasForcingQuietCheck(board, quietCount, qply))) return best;
            throw ABORTED;
        }
        int keyBase = ply * MAX_MOVES;
        if(!checked) snapshotOrdering(board, legalMoves, count, 0, selectionKeys, keyBase);
        final int tacticalCount = count;
        boolean quietPending = allowQuietChecks && !checked;
        if(quietPending && count == 0) {
            // No child has touched the legal-existence scratch yet.
            System.arraycopy(quietScratch, 0, legalMoves, 0, quietCount);
            count = quietCount;
            quietPending = false;
        }
        long[] child = boards[ply + 1];
        for(int i = 0; i < count || quietPending; i++) {
            checkpoint();
            if(i == count) {
                count = appendQuiets(board, legalMoves, count);
                quietPending = false;
                if(i == count) break;
            }
            boolean quietCheck = !checked && i >= tacticalCount;
            if(!checked && !quietCheck) Sort.next(legalMoves, selectionKeys, keyBase, i, tacticalCount);
            long move = legalMoves[i];
            // Reuse the snapshotted SR-015 class (1 = SEE-negative, 2 = SEE-nonnegative).
            // No second SEE call, magnitude margin, or checked-node pruning.
            boolean suspect = !checked && qsearchPruning != QSEARCH_BASELINE
                    && (selectionKeys[keyBase + i] >>> 25) == 1
                    && (qsearchPruning == SEE_ALL
                        || ((move >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS) == 0);
            if(suspect && qsearchPruning != SEE_CHECK_PROMO_SAFE) continue;
            Board.makeMoveInto(board[0], board[1], board[2], board[3], status, board[Board.KEY], move, child);
            // Only a negative non-promotion needs this check test. The reusable child
            // board is kept if searched; a rejected move never touches evaluator/history.
            long childChecking = suspect || quietCheck ? checkers(child, (int) child[Board.STATUS]) : 0;
            if((suspect || quietCheck) && childChecking == 0) continue;
            if(quietCheck && quietCheckReplies != 0 && !forcingCheckEligible(child, qply, childChecking)) continue;
            if(quietCheck && forcingCounters != null)
                forcingCounters[9] = Math.max(forcingCounters[9], quietChecksUsed + 1);
            if(quietCheckCounters != null) {
                if(quietCheck) quietCheckCounters[1]++;
                if(quietCheck && quietCheckCounters.length > 4) {
                    quietCheckCounters[quietChecksUsed == 0 ? 4 : 5]++;
                    quietCheckCounters[7] = Math.max(quietCheckCounters[7], quietChecksUsed + 1);
                }
                int run = quietCheck ? quietCheckRuns[ply] + 1
                        : checked && !CaptureHistory.isTactical(move, Board.enPassantSquare(status)) ? quietCheckRuns[ply] : 0;
                quietCheckRuns[ply + 1] = run;
                quietCheckCounters[3] = Math.max(quietCheckCounters[3], run);
            }
            evaluator.child(board, child, ply);
            history.pushRealPosition(child);
            if(qtable != null) historyKeys[ply + 1] = SearchKey.childHistory(
                    historyKeys[ply], history.currentKey(), (int) child[Board.STATUS]);
            int score;
            try { score = -quiescence(ply + 1, qply + 1, -beta, -alpha, quietChecksUsed + (quietCheck ? 1 : 0)); }
            finally { history.popRealPosition(); }
            if(score > best) {
                best = score;
                pv[ply][0] = move;
                // A cutoff supplies a searched legal prefix, not a proven exact continuation.
                int length = score >= beta ? 0 : pvLength[ply + 1];
                System.arraycopy(pv[ply + 1], 0, pv[ply], 1, length);
                pvLength[ply] = 1 + length;
            }
            if(score >= beta) {
                if(quietCheck && quietCheckCounters != null) quietCheckCounters[2]++;
                return qcompleted(key, ply, originalAlpha, originalBeta, score, move);
            }
            if(score > alpha) alpha = score;
        }
        return qcompleted(key, ply, originalAlpha, originalBeta, best, pvLength[ply] == 0 ? 0 : pv[ply][0]);
    }

    /** Uses only the caller's generated legal quiet list and reusable scratch; never searches a child. */
    private boolean hasQuietCheck(long[] board, int count) {
        for(int i = 0; i < count; i++) {
            checkpoint();
            Board.makeMoveInto(board[0], board[1], board[2], board[3], (int)board[Board.STATUS],
                    board[Board.KEY], quietScratch[i], generatorScratch);
            if(checkers(generatorScratch, (int)generatorScratch[Board.STATUS]) != 0) return true;
        }
        return false;
    }

    /** Full legal generation keeps Board/Gen unchanged. Its list is intentionally not
     * reused by child qsearch; classification performs no evaluation or history mutation. */
    private boolean forcingCheckEligible(long[] child, int qply, long checking) {
        boolean required = !broadInitialChecks || qply != 0;
        if(!required && forcingCounters == null) return true;
        checkpoint();
        int status = (int) child[Board.STATUS];
        int replies = Gen.genEvasion(child[0], child[1], child[2], child[3], status, child[Board.KEY],
                true, checking, evasionScratch, generatorScratch);
        if(forcingCounters != null) {
            forcingCounters[0]++;
            forcingCounters[1 + Math.min(3, replies)]++;
            forcingCounters[required ? 5 : 7]++;
            forcingCounters[required ? 6 : 8] += replies;
        }
        return !required || replies <= quietCheckReplies;
    }

    /** At ply 256, excluded checks need no child slot; eligible unresolved moves still abort. */
    private boolean hasForcingQuietCheck(long[] board, int count, int qply) {
        for(int i = 0; i < count; i++) {
            checkpoint();
            Board.makeMoveInto(board[0], board[1], board[2], board[3], (int)board[Board.STATUS],
                    board[Board.KEY], quietScratch[i], forcingBoard);
            long checking = checkers(forcingBoard, (int)forcingBoard[Board.STATUS]);
            if(checking != 0 && forcingCheckEligible(forcingBoard, qply, checking)) return true;
        }
        return false;
    }

    /** Full existence count only in SR-001F diagnostic runs, outside clean timing. */
    private int countQuietChecks(long[] board, int count) {
        int checks = 0;
        for(int i = 0; i < count; i++) {
            checkpoint();
            Board.makeMoveInto(board[0], board[1], board[2], board[3], (int)board[Board.STATUS],
                    board[Board.KEY], quietScratch[i], generatorScratch);
            if(checkers(generatorScratch, (int)generatorScratch[Board.STATUS]) != 0) checks++;
        }
        return checks;
    }

    private int qcompleted(long key, int ply, int alpha, int beta, int score, long move) {
        if(qtable != null) {
            checkpoint(); // No evidence from an incomplete node, including final-child cancellation.
            int type = score <= alpha ? TTable.TYPE_UPPER : score >= beta ? TTable.TYPE_LOWER : TTable.TYPE_EXACT;
            qtable.save(key, 0, type, TranspositionScores.toTableScore(score, ply), move);
            if(qttCounters != null) qttCounters[5]++; // save is void; accepted/rejected writes are not observable cheaply.
        }
        return score;
    }

    private static long checkers(long[] board, int status) {
        int player = Board.player(status);
        long color = ~(-(long) player ^ board[3]);
        return Board.getCheckersPext(board[0], board[1], board[2], board[3], color, player,
                Long.numberOfTrailingZeros(board[0] & ~board[1] & ~board[2] & color), board[0] | board[1] | board[2]);
    }

    /** Shared scratch is safe: generated quiets are copied to this ply before recursion. */
    private int appendQuiets(long[] board, long[] list, int count) {
        int quiet = Gen.genQuiet(board[0], board[1], board[2], board[3], (int) board[Board.STATUS],
                board[Board.KEY], true, quietScratch, generatorScratch);
        System.arraycopy(quietScratch, 0, list, count, quiet);
        return count + quiet;
    }

    private int completed(long key, int depth, int ply, int alpha, int beta, int score, long move, long entryEpoch) {
        // A prediction anywhere in the searched subtree taints this result.
        // Suppress ancestor writes too, including otherwise EXACT outcomes.
        // Unaffected subtrees remain eligible for normal exact TT reuse.
        if(table != null && !nullProbeActive && entryEpoch == selectiveEpoch) {
            checkpoint(); // Includes cancellation raised by the final evaluated child.
            // R004 has eight depth bits; depth 256 must never alias depth zero.
            if(ply != 0) store(key, depth, ply, alpha, beta, score, move);
        }
        return score;
    }

    private static boolean hasNonPawnMaterial(long[] board, int status) {
        long occupied = board[0] | board[1] | board[2];
        long kings = board[0] & ~board[1] & ~board[2];
        long pawns = ~board[0] & board[1] & board[2];
        long side = Board.player(status) == 0 ? ~board[3] : board[3];
        return (occupied & ~kings & ~pawns & side) != 0;
    }

    private static void syntheticPass(long[] board, long[] child) {
        int status = (int) board[Board.STATUS];
        Board.nullMoveInto(board[0], board[1], board[2], board[3], status, board[Board.KEY], child);
        // Local speculative clocks: unchanged pieces/castling, expired EP.
        int clock = Math.min(Board.MAX_HALF_MOVE_CLOCK, Board.halfMoveClock(status) + 1);
        child[Board.STATUS] = ((int) child[Board.STATUS]
                & ~((Board.HALF_MOVE_CLOCK_BITS << Board.HALF_MOVE_CLOCK_SHIFT)
                    | (Board.FULL_MOVE_NUMBER_BITS << Board.FULL_MOVE_NUMBER_SHIFT)))
                | (clock << Board.HALF_MOVE_CLOCK_SHIFT)
                | ((Board.fullMoveNumber(status) + Board.player(status)) << Board.FULL_MOVE_NUMBER_SHIFT);
    }

    private void store(long key, int depth, int ply, int alpha, int beta, int score, long move) {
        if(depth > 255) return;
        int type = score <= alpha ? TTable.TYPE_UPPER : score >= beta ? TTable.TYPE_LOWER : TTable.TYPE_EXACT;
        table.save(key, depth, type, TranspositionScores.toTableScore(score, ply), move);
    }

    /** Validate against generated legal moves and stably promote in the same pass. */
    private static void promoteHashMove(long[] moves, int count, long hashMove) {
        for(int i = 0; i < count; i++) {
            if(moves[i] == hashMove) {
                System.arraycopy(moves, 0, moves, 1, i);
                moves[0] = hashMove;
                return;
            }
        }
    }

    /** Stable two-way partition: captures (including EP) and promotions, then quiets. */
    private void orderTacticalFirst(long[] legalMoves, int count, int status) {
        int tactical = 0;
        int quiet = 0;
        int ep = Board.enPassantSquare(status);
        for(int i = 0; i < count; i++) {
            long move = legalMoves[i];
            boolean isTactical = ((move >>> Board.TARGET_PIECE_SHIFT) & Board.PIECE_BITS) != 0
                    || ((move >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS) != 0
                    || (((move >>> Board.START_PIECE_SHIFT) & Piece.TYPE) == Piece.PAWN && Move.toSquare(move) == ep);
            if(isTactical) legalMoves[tactical++] = move;
            else quietScratch[quiet++] = move;
        }
        System.arraycopy(quietScratch, 0, legalMoves, tactical, quiet);
    }

    /** Stable full-generation partition; scratch is finished before descending. */
    private void orderSeeClassified(long[] board, long[] legalMoves, int count, int status, long hashMove,
                                    int continuationContext, int ply, int countermoveContext) {
        int good = 0;
        int quiet = 0;
        int bad = 0;
        int hashIndex = -1;
        int ep = Board.enPassantSquare(status);
        for(int i = 0; i < count; i++) {
            long move = legalMoves[i];
            // Exact membership validates the hint. Promotion below puts it first;
            // its SEE class is irrelevant and every other class retains its order.
            if(move == hashMove) {
                hashIndex = good;
                legalMoves[good++] = move;
                continue;
            }
            boolean isTactical = ((move >>> Board.TARGET_PIECE_SHIFT) & Board.PIECE_BITS) != 0
                    || ((move >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS) != 0
                    || (((move >>> Board.START_PIECE_SHIFT) & Piece.TYPE) == Piece.PAWN && Move.toSquare(move) == ep);
            if(!isTactical) quietScratch[quiet++] = move;
            else if(See.atLeastGeneratedLegal(board, move, 0)) legalMoves[good++] = move;
            else badTacticalScratch[bad++] = move;
        }
        if(ordering >= SEE_MATERIAL) {
            // Move the matched hash outside both ranked ranges, without scoring it.
            if(hashIndex >= 0) {
                System.arraycopy(legalMoves, 0, legalMoves, 1, hashIndex);
                legalMoves[0] = hashMove;
            }
            boolean lva = ordering == SEE_MATERIAL_LVA;
            rankTacticals(legalMoves, hashIndex >= 0 ? 1 : 0, good, ep, lva);
            rankTacticals(badTacticalScratch, 0, bad, ep, lva);
        }
        if(ordering != SEE_TIERED) {
            if(quietHistory != null) rankQuiets(quietScratch, quiet, continuationContext);
            if(killers != null) {
                // Reuse stable exact-match promotion on the generated quiet range only.
                // Secondary first, then primary: distinct classes, no composite history score.
                int base = ply * KillerMoves.SLOTS;
                if(killers[base + 1] != 0) promoteHashMove(quietScratch, quiet, killers[base + 1]);
                if(killers[base] != 0) promoteHashMove(quietScratch, quiet, killers[base]);
            }
            if(countermoveContext >= 0) {
                long reply = countermoves[countermoveContext];
                if(reply != 0) promoteHashMove(quietScratch, quiet, reply);
            }
            System.arraycopy(badTacticalScratch, 0, legalMoves, good, bad);
            System.arraycopy(quietScratch, 0, legalMoves, good + bad, quiet);
        } else {
            // Retain the earlier SEE_TIERED experiment: bad tacticals after quiets.
            System.arraycopy(quietScratch, 0, legalMoves, good, quiet);
            System.arraycopy(badTacticalScratch, 0, legalMoves, good + quiet, bad);
        }
    }

    /** Snapshot the currently generated phase; deferred quiets are sampled separately when needed. */
    private void snapshotOrdering(long[] board, long[] list, int count, long hashMove, int[] keys, int base) {
        int ep = Board.enPassantSquare((int) board[Board.STATUS]);
        for(int i = 0; i < count; i++) {
            long move = list[i];
            if(move == hashMove) { keys[base + i] = Integer.MAX_VALUE - i; continue; }
            int group = 0;
            int evidence;
            if(CaptureHistory.isTactical(move, ep)) {
                group = See.atLeastGeneratedLegal(board, move, 0) ? 2 : 1;
                evidence = tacticalMaterialValue(move, ep);
            } else evidence = quietHistory[QuietHistory.index(move)];
            // group dominates 16-bit shifted evidence; lower generation ordinal wins ties.
            // Quiet evidence is [-16384,16384]; tactical material is [0,1850].
            keys[base + i] = (group << 25) | ((evidence + QuietHistory.LIMIT) << 9) | (511 - i);
        }
    }

    /** Immediate material only, for a generated legal tactical; EP's packed victim is empty. */
    static int tacticalMaterialValue(long move, int ep) {
        int captured = (int) (move >>> Board.TARGET_PIECE_SHIFT) & Piece.TYPE;
        int moving = (int) (move >>> Board.START_PIECE_SHIFT) & Piece.TYPE;
        int promoted = (int) (move >>> Board.PROMOTE_PIECE_SHIFT) & Piece.TYPE;
        if(captured == 0 && moving == Piece.PAWN && Move.toSquare(move) == ep) captured = Piece.PAWN;
        return (captured == 0 ? 0 : Eval.exchangeValue(captured))
                + (promoted == 0 ? 0 : Eval.exchangeValue(promoted) - Eval.exchangeValue(Piece.PAWN));
    }

    /** Experimental stable insertion pass on one tactical class, shared by material policies. */
    private void rankTacticals(long[] list, int from, int end, int ep, boolean lva) {
        // King is the largest exchange value. This scale makes primary evidence dominate
        // every attacker tie-break; the maximum legal key (1850 * 20001) fits in int.
        int scale = lva ? Eval.exchangeValue(Piece.KING) + 1 : 1;
        // Signed history spans [-LIMIT, LIMIT]; a one-unit material difference dominates it.
        // The maximum legal composite (1850 * 32769 + 16384) also fits in int.
        if(captureHistory != null) scale = 2 * CaptureHistory.LIMIT + 1;
        for(int i = from; i < end; i++) {
            long move = list[i];
            int score = tacticalMaterialValue(move, ep) * scale;
            if(lva) score -= Eval.exchangeValue((int) (move >>> Board.START_PIECE_SHIFT) & Piece.TYPE);
            if(captureHistory != null) score += captureHistory[CaptureHistory.index(move, ep)];
            int j = i;
            while(j > from && materialScores[j - 1] < score) {
                list[j] = list[j - 1];
                materialScores[j] = materialScores[j - 1];
                j--;
            }
            list[j] = move;
            materialScores[j] = score;
        }
    }

    /** Experimental stable insertion over quiets only; does not decide SR-017 mechanics.
     * Tactical ranking has finished, so its primitive score scratch is reusable here. */
    private void rankQuiets(long[] list, int end, int continuationContext) {
        for(int i = 0; i < end; i++) {
            long move = list[i];
            int score = quietHistory[QuietHistory.index(move)];
            if(continuationContext >= 0) score += continuationHistory[ContinuationHistory.index(continuationContext, move)];
            int j = i;
            while(j > 0 && materialScores[j - 1] < score) {
                list[j] = list[j - 1];
                materialScores[j] = materialScores[j - 1];
                j--;
            }
            list[j] = move;
            materialScores[j] = score;
        }
    }

    private void checkpoint() {
        if(Thread.currentThread().isInterrupted() || cancelled.getAsBoolean()) throw ABORTED;
    }

    private static final Aborted ABORTED = new Aborted();
    private static final class Aborted extends RuntimeException {
        private Aborted() { super(null, null, false, false); }
    }
}
