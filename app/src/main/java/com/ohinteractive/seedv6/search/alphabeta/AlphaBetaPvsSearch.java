package com.ohinteractive.seedv6.search.alphabeta;

import java.util.Arrays;
import java.util.Objects;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.rules.DrawAdjudicator;
import com.ohinteractive.seedv6.rules.DrawAdjudicator.RuleDraw;
import com.ohinteractive.seedv6.rules.SearchLineHistory;
import com.ohinteractive.seedv6.search.common.SearchControl;
import com.ohinteractive.seedv6.search.common.SearchObserver;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.common.WindowedSearch;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnostics;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot;
import com.ohinteractive.seedv6.search.order.MoveOrdering;
import com.ohinteractive.seedv6.search.order.StagedMovePicker;
import com.ohinteractive.seedv6.search.quiescence.QuiescenceSearch;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;
import com.ohinteractive.seedv6.search.tt.TranspositionTable;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.Bound;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.Cacheability;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.Probe;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.StoreOutcome;

/**
 * Worker-owned, single-threaded, non-selective fail-soft negamax alpha-beta
 * with optional principal-variation search.
 *
 * <p>Depth is remaining main-search plies at the current node. Depth zero is
 * the accepted score-only quiescence leaf. All node, board, move-picker, TT
 * probe and PV storage is allocated once per worker; a call allocates only its
 * private history snapshot and immutable exported result/PV.</p>
 */
public final class AlphaBetaPvsSearch implements WindowedSearch {

    /** The established mate, absolute-ply and PV boundary. */
    public static final int MAX_SUPPORTED_DEPTH = TranspositionScores.MAX_MATE_PLY;
    public static final int MAX_PV_MOVES = TranspositionScores.MAX_MATE_PLY;

    public AlphaBetaPvsSearch() {
        this(new TranspositionTable());
    }

    public AlphaBetaPvsSearch(TranspositionTable table) {
        this(table, Configuration.production());
    }

    AlphaBetaPvsSearch(TranspositionTable table, Configuration configuration) {
        this.table = Objects.requireNonNull(table, "table");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        ordering = new MoveOrdering(MAX_SUPPORTED_DEPTH + 1);
        picker = ordering.picker();
        quiescence = new QuiescenceSearch(ordering);
        for(int ply = 0; ply <= MAX_SUPPORTED_DEPTH; ply ++) {
            probes[ply] = new Probe();
        }
    }

    @Override
    public SearchResult search(SearchRequest request) {
        return search(request, NEGATIVE_INFINITY, POSITIVE_INFINITY);
    }

    /** Package-visible fail-soft window entry for focused correctness tests. */
    SearchResult search(SearchRequest request, int alpha, int beta) {
        beginTopLevelSearch();
        try {
            return searchWindow(request, alpha, beta);
        } finally {
            endTopLevelSearch();
        }
    }

    @Override
    public void beginTopLevelSearch() {
        if(active || topLevelActive) {
            throw new IllegalStateException("AlphaBetaPvsSearch already has an active top-level search.");
        }
        if(newGamePending) {
            table.newGame();
            ordering.reset();
            newGamePending = false;
        }
        if(configuration.transpositionTable()) table.advanceGeneration();
        diagnosticsScopeInitialized = false;
        diagnostics = null;
        topLevelActive = true;
    }

    @Override
    public SearchResult searchWindow(SearchRequest request, int alpha, int beta) {
        if(!topLevelActive) {
            throw new IllegalStateException("A top-level search sequence has not been started.");
        }
        Objects.requireNonNull(request, "request");
        validateWindow(alpha, beta);
        initializeDiagnostics(request.diagnosticsEnabled());
        final int requestedDepth = request.depth();
        if(requestedDepth > MAX_SUPPORTED_DEPTH) {
            throw new IllegalArgumentException(
                "Unsupported search depth: " + requestedDepth
                    + " (maximum " + MAX_SUPPORTED_DEPTH + ")"
            );
        }
        if(active) throw new IllegalStateException("AlphaBetaPvsSearch is already active.");

        final SearchLineHistory history = new SearchLineHistory(request.gameHistory());
        active = true;
        final int initialHistorySize = history.size();
        final long searchStartNanos = System.nanoTime();
        final SearchObserver observer = request.observer();
        try {
            request.copyBoardInto(boardStack[0]);
            resetInvocation(request, history);
            if(diagnostics != null) diagnostics.recordRoot();
            final int rootMoveCount = Gen.genAll(
                boardStack[0][0], boardStack[0][1], boardStack[0][2], boardStack[0][3],
                Math.toIntExact(boardStack[0][Board.STATUS]), boardStack[0][Board.KEY], true,
                rootMoves, generatorScratch
            );
            legalRootMoves = rootMoveCount;
            rootFallback = rootMoveCount == 0 ? StagedMovePicker.NO_MOVE : rootMoves[0];
            observer.onSearchStarted(
                requestedDepth, Eval.evaluate(boardStack[0]), rootMoveCount
            );

            if(!control.checkpoint()) {
                aborted = true;
                return finish(observer, requestedDepth, searchStartNanos, false, 0);
            }

            final int score = requestedDepth == 0
                ? searchRootQuiescence(history, alpha, beta)
                : searchNode(boardStack[0], requestedDepth, 0, history, alpha, beta);
            return finish(
                observer, requestedDepth, searchStartNanos, !aborted,
                aborted ? 0 : score
            );
        } finally {
            history.restoreRoot();
            lastHistoryStartSize = initialHistorySize;
            lastHistoryEndSize = history.size();
            control = null;
            active = false;
        }
    }

    @Override
    public void endTopLevelSearch() {
        if(active) {
            throw new IllegalStateException("Cannot end a top-level search while an exact attempt is active.");
        }
        if(!topLevelActive) {
            throw new IllegalStateException("No top-level search sequence is active.");
        }
        topLevelActive = false;
        diagnostics = null;
    }

    @Override
    public int maxSupportedDepth() {
        return MAX_SUPPORTED_DEPTH;
    }

    @Override
    public void newGame() {
        newGamePending = true;
    }

    public MoveOrdering ordering() {
        return ordering;
    }

    Statistics statistics() {
        return new Statistics(
            mainChildEntries, qsearchChildEntries, pvsNullWindowSearches,
            pvsResearches, pvsFailLows, pvsFailHighs,
            pvsRootNullWindows, pvsInteriorNullWindows,
            pvsRootResearches, pvsInteriorResearches,
            pvsRootFailHighs, pvsInteriorFailHighs,
            firstMoveFullWindowSearches, ttCutoffs, ttStores, quietCutoffUpdates,
            lastHistoryStartSize, lastHistoryEndSize
        );
    }

    void resetHeuristics() {
        if(active) throw new IllegalStateException("Cannot reset an active search.");
        ordering.reset();
    }

    private static final int NEGATIVE_INFINITY = -TranspositionScores.MATE_SCORE - 1;
    private static final int POSITIVE_INFINITY = TranspositionScores.MATE_SCORE + 1;
    private static final int SAFE_TT_REPETITION_DEPTH = 8;
    private static final int MAX_MOVES = StagedMovePicker.MAX_MOVES;

    private final TranspositionTable table;
    private final Configuration configuration;
    private final MoveOrdering ordering;
    private final StagedMovePicker picker;
    private final QuiescenceSearch quiescence;
    private final SearchDiagnostics diagnosticsAccumulator = new SearchDiagnostics();
    private final long[][] boardStack =
        new long[MAX_SUPPORTED_DEPTH + 1][Board.MAX_BITBOARDS];
    private final long[][] unorderedMoves = new long[MAX_SUPPORTED_DEPTH + 1][MAX_MOVES];
    private final int[] unorderedIndices = new int[MAX_SUPPORTED_DEPTH + 1];
    private final Probe[] probes = new Probe[MAX_SUPPORTED_DEPTH + 1];
    private final long[][] pv = new long[MAX_SUPPORTED_DEPTH + 1][MAX_PV_MOVES];
    private final int[] pvLengths = new int[MAX_SUPPORTED_DEPTH + 1];
    private final int[] bestScores = new int[MAX_SUPPORTED_DEPTH + 1];
    private final long[] bestMoves = new long[MAX_SUPPORTED_DEPTH + 1];
    private final boolean[] pathDependent = new boolean[MAX_SUPPORTED_DEPTH + 1];
    private final long[] rootMoves = new long[MAX_MOVES];
    private final long[] generatorScratch = new long[Board.MAX_BITBOARDS];

    private SearchControl control;
    private SearchObserver observer;
    private SearchDiagnostics diagnostics;
    private int legalRootMoves;
    private long rootFallback;
    private long nodes;
    private boolean aborted;
    private boolean active;
    private boolean topLevelActive;
    private boolean diagnosticsScopeInitialized;
    private boolean diagnosticsScopeEnabled;
    private volatile boolean newGamePending;

    private long mainChildEntries;
    private long qsearchChildEntries;
    private long pvsNullWindowSearches;
    private long pvsResearches;
    private long pvsFailLows;
    private long pvsFailHighs;
    private long pvsRootNullWindows;
    private long pvsInteriorNullWindows;
    private long pvsRootResearches;
    private long pvsInteriorResearches;
    private long pvsRootFailHighs;
    private long pvsInteriorFailHighs;
    private long firstMoveFullWindowSearches;
    private long ttCutoffs;
    private long ttStores;
    private long quietCutoffUpdates;
    private int lastHistoryStartSize;
    private int lastHistoryEndSize;

    private void resetInvocation(SearchRequest request, SearchLineHistory history) {
        control = request.control();
        observer = request.observer();
        nodes = 0L;
        aborted = false;
        legalRootMoves = 0;
        rootFallback = StagedMovePicker.NO_MOVE;
        Arrays.fill(pvLengths, 0);
        Arrays.fill(bestScores, NEGATIVE_INFINITY);
        Arrays.fill(bestMoves, StagedMovePicker.NO_MOVE);
        Arrays.fill(pathDependent, false);
        mainChildEntries = 0L;
        qsearchChildEntries = 0L;
        pvsNullWindowSearches = 0L;
        pvsResearches = 0L;
        pvsFailLows = 0L;
        pvsFailHighs = 0L;
        pvsRootNullWindows = 0L;
        pvsInteriorNullWindows = 0L;
        pvsRootResearches = 0L;
        pvsInteriorResearches = 0L;
        pvsRootFailHighs = 0L;
        pvsInteriorFailHighs = 0L;
        firstMoveFullWindowSearches = 0L;
        ttCutoffs = 0L;
        ttStores = 0L;
        quietCutoffUpdates = 0L;
        lastHistoryStartSize = history.size();
        lastHistoryEndSize = history.size();
    }

    private int searchRootQuiescence(
        SearchLineHistory history, int alpha, int beta
    ) {
        final QuiescenceSearch.Result leaf = quiescence.searchLeaf(
            boardStack[0], history, control, 0, alpha, beta, diagnostics
        );
        nodes += leaf.nodes();
        qsearchChildEntries += leaf.nodes();
        if(!leaf.completed()) {
            aborted = true;
            return 0;
        }
        pathDependent[0] = leaf.pathDependent();
        if(rootFallback != StagedMovePicker.NO_MOVE) bestMoves[0] = rootFallback;
        return leaf.score();
    }

    private int searchNode(
        long[] board, int depth, int ply, SearchLineHistory history,
        int alpha, int beta
    ) {
        pvLengths[ply] = 0;
        bestScores[ply] = NEGATIVE_INFINITY;
        bestMoves[ply] = StagedMovePicker.NO_MOVE;
        pathDependent[ply] = false;
        if(!control.checkpoint()) {
            aborted = true;
            return 0;
        }
        if(depth <= 0) return searchQuiescenceLeaf(board, history, ply, alpha, beta);

        final int originalAlpha = alpha;
        final int originalBeta = beta;
        final Probe probe = probes[ply];
        long hashMove = StagedMovePicker.NO_MOVE;
        if(configuration.transpositionTable()) {
            table.probe(board[Board.KEY], depth, alpha, beta, ply, probe);
            if(diagnostics != null) diagnostics.recordTtProbe(probe.outcome());
            if(probe.keyMatches()) hashMove = probe.move();
        }

        boolean pickerPrepared = false;
        try {
            final int moveCount;
            if(configuration.moveOrdering()) {
                moveCount = picker.prepare(board, ply, hashMove);
                pickerPrepared = true;
                if(diagnostics != null && picker.containsLegalMove(ply, hashMove)) {
                    diagnostics.recordHashMoveAvailable();
                }
            } else {
                moveCount = Gen.genAll(
                    board[0], board[1], board[2], board[3],
                    Math.toIntExact(board[Board.STATUS]), board[Board.KEY], true,
                    unorderedMoves[ply], generatorScratch
                );
                unorderedIndices[ply] = 0;
            }

            if(!control.checkpoint()) {
                aborted = true;
                return 0;
            }

            if(moveCount == 0) {
                final int terminal = isInCheck(board)
                    ? -TranspositionScores.MATE_SCORE + ply : 0;
                store(board, depth, terminal, ply, StagedMovePicker.NO_MOVE,
                    Cacheability.POSITION_ONLY, originalAlpha, originalBeta);
                bestScores[ply] = terminal;
                return terminal;
            }

            final RuleDraw draw = DrawAdjudicator.adjudicateNonTerminal(board, history);
            if(draw != RuleDraw.NONE) {
                final boolean dependsOnPath = draw == RuleDraw.FIFTY_MOVE
                    || draw == RuleDraw.FORMAL_THREEFOLD;
                pathDependent[ply] = dependsOnPath;
                if(ply == 0) {
                    bestMoves[0] = rootFallback;
                    pv[0][0] = rootFallback;
                    pvLengths[0] = 1;
                }
                store(
                    board, depth, 0, ply,
                    ply == 0 ? rootFallback : StagedMovePicker.NO_MOVE,
                    dependsOnPath ? Cacheability.PATH_DEPENDENT
                        : Cacheability.POSITION_ONLY,
                    originalAlpha, originalBeta
                );
                bestScores[ply] = 0;
                return 0;
            }

            if(configuration.transpositionTable() && ply != 0
                && probe.scoreUsable() && ttContextSafe(board, depth, history)) {
                ttCutoffs ++;
                if(diagnostics != null) diagnostics.recordTtCutoff(probe.outcome());
                bestScores[ply] = probe.score();
                return probe.score();
            }

            int searchedMoves = 0;
            while(searchedMoves < moveCount) {
                final long move = configuration.moveOrdering()
                    ? picker.next(ply)
                    : unorderedMoves[ply][unorderedIndices[ply] ++];
                if(move == StagedMovePicker.NO_MOVE) {
                    throw new IllegalStateException(
                        "Move source ended before its authoritative count at ply " + ply
                    );
                }
                requireChildCapacity(ply);
                if(!control.tryEnterNode()) {
                    aborted = true;
                    return 0;
                }
                boolean hashMoveSource = false;
                boolean tacticalMove = false;
                boolean killerContribution = false;
                boolean historyContribution = false;
                if(diagnostics != null) {
                    diagnostics.recordMainNode(ply + 1);
                    diagnostics.recordLegalMoveSearched();
                    tacticalMove = MoveOrdering.isTactical(board, move);
                    hashMoveSource = configuration.moveOrdering()
                        && hashMove != StagedMovePicker.NO_MOVE && move == hashMove;
                    if(configuration.moveOrdering() && !hashMoveSource && !tacticalMove) {
                        killerContribution = move == ordering.killer(ply, 0)
                            || move == ordering.killer(ply, 1);
                        historyContribution = !killerContribution && ordering.historyScore(move) > 0;
                    }
                }

                final long rootMoveStartNodes = nodes;
                final long rootMoveStartNanos = ply == 0 ? System.nanoTime() : 0L;
                if(ply == 0) {
                    observer.onRootMoveStarted(searchedMoves + 1, legalRootMoves, move);
                }

                final long[] child = boardStack[ply + 1];
                Board.makeMoveInto(
                    board[0], board[1], board[2], board[3],
                    Math.toIntExact(board[Board.STATUS]), board[Board.KEY], move, child
                );
                nodes ++;
                mainChildEntries ++;
                history.pushRealPosition(child);
                int score;
                boolean childPathDependent;
                try {
                    if(searchedMoves == 0 || !configuration.pvs()) {
                        if(searchedMoves == 0) firstMoveFullWindowSearches ++;
                        score = -searchNode(
                            child, depth - 1, ply + 1, history, -beta, -alpha
                        );
                        childPathDependent = pathDependent[ply + 1];
                    } else {
                        pvsNullWindowSearches ++;
                        if(ply == 0) pvsRootNullWindows ++;
                        else pvsInteriorNullWindows ++;
                        score = -searchNode(
                            child, depth - 1, ply + 1, history, -alpha - 1, -alpha
                        );
                        childPathDependent = pathDependent[ply + 1];
                        if(!aborted) {
                            if(score <= alpha) {
                                pvsFailLows ++;
                            } else if(score >= beta) {
                                pvsFailHighs ++;
                                if(ply == 0) pvsRootFailHighs ++;
                                else pvsInteriorFailHighs ++;
                            } else {
                                pvsResearches ++;
                                if(ply == 0) pvsRootResearches ++;
                                else pvsInteriorResearches ++;
                                score = -searchNode(
                                    child, depth - 1, ply + 1, history, -beta, -alpha
                                );
                                childPathDependent |= pathDependent[ply + 1];
                            }
                        }
                    }
                } finally {
                    history.popRealPosition();
                }
                if(aborted) return 0;
                if(!control.checkpoint()) {
                    aborted = true;
                    return 0;
                }
                pathDependent[ply] |= childPathDependent;

                final boolean improvedBest = score > bestScores[ply];
                if(improvedBest) {
                    bestScores[ply] = score;
                    bestMoves[ply] = move;
                    prependChildPv(ply, move);
                }
                if(ply == 0) {
                    observer.onRootMoveFinished(
                        searchedMoves + 1, legalRootMoves, move, score, improvedBest,
                        nodes - rootMoveStartNodes, System.nanoTime() - rootMoveStartNanos
                    );
                    if(!control.checkpoint()) {
                        aborted = true;
                        return 0;
                    }
                }
                searchedMoves ++;

                if(score >= beta) {
                    if(diagnostics != null) {
                        diagnostics.recordBetaCutoff(
                            searchedMoves, hashMoveSource, tacticalMove,
                            killerContribution, historyContribution
                        );
                    }
                    if(configuration.moveOrdering()
                        && ordering.recordQuietCutoff(board, ply, move, depth)) {
                        quietCutoffUpdates ++;
                    }
                    store(
                        board, depth, score, ply, bestMoves[ply],
                        pathDependent[ply] ? Cacheability.PATH_DEPENDENT
                            : Cacheability.POSITION_ONLY,
                        originalAlpha, originalBeta
                    );
                    return score;
                }
                if(score > alpha) alpha = score;
            }

            final int result = bestScores[ply];
            if(!control.checkpoint()) {
                aborted = true;
                return 0;
            }
            store(
                board, depth, result, ply, bestMoves[ply],
                pathDependent[ply] ? Cacheability.PATH_DEPENDENT
                    : Cacheability.POSITION_ONLY,
                originalAlpha, originalBeta
            );
            return result;
        } finally {
            if(pickerPrepared) picker.clearPly(ply);
        }
    }

    private int searchQuiescenceLeaf(
        long[] board, SearchLineHistory history, int ply, int alpha, int beta
    ) {
        final QuiescenceSearch.Result leaf = quiescence.searchLeaf(
            board, history, control, ply, alpha, beta, diagnostics
        );
        nodes += leaf.nodes();
        qsearchChildEntries += leaf.nodes();
        if(!leaf.completed()) {
            aborted = true;
            return 0;
        }
        pathDependent[ply] = leaf.pathDependent();
        bestScores[ply] = leaf.score();
        return leaf.score();
    }

    private void prependChildPv(int ply, long move) {
        final int childLength = pvLengths[ply + 1];
        final int maximumChildLength = MAX_PV_MOVES - ply - 1;
        if(childLength < 0 || childLength > maximumChildLength) {
            throw new MainSearchCapacityException(
                "Invalid child PV length " + childLength + " at absolute ply " + ply
            );
        }
        pv[ply][0] = move;
        if(childLength != 0) {
            System.arraycopy(pv[ply + 1], 0, pv[ply], 1, childLength);
        }
        pvLengths[ply] = childLength + 1;
    }

    private void store(
        long[] board, int depth, int score, int ply, long move,
        Cacheability cacheability, int originalAlpha, int originalBeta
    ) {
        if(!configuration.transpositionTable() || aborted) return;
        final StoreOutcome outcome = table.store(
            board[Board.KEY], depth, classify(score, originalAlpha, originalBeta),
            score, ply, move, cacheability
        );
        if(outcome == StoreOutcome.STORED) ttStores ++;
        if(outcome == StoreOutcome.STORED && diagnostics != null) diagnostics.recordTtStore();
    }

    private SearchResult finish(
        SearchObserver searchObserver, int requestedDepth, long searchStartNanos,
        boolean completed, int score
    ) {
        final boolean hasMove;
        final long bestMove;
        final long[] exportedPv;
        if(requestedDepth == 0) {
            hasMove = rootFallback != StagedMovePicker.NO_MOVE;
            bestMove = hasMove ? rootFallback : StagedMovePicker.NO_MOVE;
            exportedPv = new long[0];
        } else {
            hasMove = bestMoves[0] != StagedMovePicker.NO_MOVE;
            bestMove = hasMove ? bestMoves[0] : StagedMovePicker.NO_MOVE;
            exportedPv = hasMove
                ? Arrays.copyOf(pv[0], pvLengths[0]) : new long[0];
        }
        final int resultScore = completed
            ? score
            : bestScores[0] == NEGATIVE_INFINITY ? 0 : bestScores[0];
        final SearchResult result = new SearchResult(
            bestMove, hasMove, resultScore, requestedDepth, nodes,
            legalRootMoves, completed, exportedPv,
            diagnostics == null ? SearchDiagnosticsSnapshot.disabled() : diagnostics.snapshot()
        );
        searchObserver.onSearchFinished(result, System.nanoTime() - searchStartNanos);
        return result;
    }

    private static boolean ttContextSafe(
        long[] board, int depth, SearchLineHistory history
    ) {
        // Raw Board.KEY excludes the halfmove clock and line history. A reset
        // clock plus fewer than eight main plies cannot reach formal threefold
        // from its sole reversible-window occurrence; qsearch has no TT policy.
        return Board.halfMoveClock(Math.toIntExact(board[Board.STATUS])) == 0
            && depth < SAFE_TT_REPETITION_DEPTH
            && history.currentOccurrences(board) == 1;
    }

    private void initializeDiagnostics(boolean enabled) {
        if(!diagnosticsScopeInitialized) {
            diagnosticsScopeInitialized = true;
            diagnosticsScopeEnabled = enabled;
            if(enabled) {
                diagnosticsAccumulator.reset();
                diagnostics = diagnosticsAccumulator;
            }
            return;
        }
        if(enabled != diagnosticsScopeEnabled) {
            throw new IllegalArgumentException(
                "Every exact attempt in one top-level search must use the same diagnostics mode."
            );
        }
    }

    private static Bound classify(int score, int originalAlpha, int originalBeta) {
        if(score >= originalBeta) return Bound.LOWER;
        if(score <= originalAlpha) return Bound.UPPER;
        return Bound.EXACT;
    }

    private static boolean isInCheck(long[] board) {
        final int player = Math.toIntExact(board[Board.STATUS]) & Board.PLAYER_BIT;
        return Board.isPlayerInCheckPext(
            board[0], board[1], board[2], board[3], player
        );
    }

    private static void validateWindow(int alpha, int beta) {
        if(alpha < NEGATIVE_INFINITY || beta > POSITIVE_INFINITY || alpha >= beta) {
            throw new IllegalArgumentException(
                "Search window must satisfy " + NEGATIVE_INFINITY + " <= alpha < beta <= "
                    + POSITIVE_INFINITY + ": alpha=" + alpha + ", beta=" + beta
            );
        }
    }

    static void requireChildCapacity(int ply) {
        if(ply < 0 || ply >= MAX_SUPPORTED_DEPTH) {
            throw new MainSearchCapacityException(
                "Main search cannot enter a child from absolute ply " + ply
                    + " (maximum child ply " + MAX_SUPPORTED_DEPTH + ")"
            );
        }
    }

    record Configuration(
        boolean pvs, boolean transpositionTable, boolean moveOrdering
    ) {
        static Configuration production() {
            return new Configuration(true, true, true);
        }
    }

    record Statistics(
        long mainChildEntries,
        long qsearchChildEntries,
        long pvsNullWindowSearches,
        long pvsResearches,
        long pvsFailLows,
        long pvsFailHighs,
        long pvsRootNullWindows,
        long pvsInteriorNullWindows,
        long pvsRootResearches,
        long pvsInteriorResearches,
        long pvsRootFailHighs,
        long pvsInteriorFailHighs,
        long firstMoveFullWindowSearches,
        long ttCutoffs,
        long ttStores,
        long quietCutoffUpdates,
        int historyStartSize,
        int historyEndSize
    ) {}

    /** Absolute-ply exhaustion is a controlled lifecycle failure, not a score. */
    public static final class MainSearchCapacityException extends IllegalStateException {
        public MainSearchCapacityException(String message) {
            super(message);
        }
    }
}
