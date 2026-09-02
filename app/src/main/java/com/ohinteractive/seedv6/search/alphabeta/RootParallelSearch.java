package com.ohinteractive.seedv6.search.alphabeta;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.rules.DrawAdjudicator;
import com.ohinteractive.seedv6.rules.DrawAdjudicator.RuleDraw;
import com.ohinteractive.seedv6.rules.SearchLineHistory;
import com.ohinteractive.seedv6.search.alphabeta.AlphaBetaPvsSearch.Configuration;
import com.ohinteractive.seedv6.search.alphabeta.AlphaBetaPvsSearch.RootChildResult;
import com.ohinteractive.seedv6.search.common.SearchControl;
import com.ohinteractive.seedv6.search.common.SearchObserver;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.common.SearchTermination;
import com.ohinteractive.seedv6.search.common.WindowedSearch;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnostics;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot;
import com.ohinteractive.seedv6.search.order.MoveOrdering;
import com.ohinteractive.seedv6.search.order.StagedMovePicker;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;
import com.ohinteractive.seedv6.search.tt.TranspositionTable;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.Bound;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.Cacheability;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.Probe;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.StoreOutcome;

/**
 * Conservative root-only parallel search over the accepted WS13 recursive
 * search. A fixed reusable context is submitted once per configured worker;
 * contexts claim exact root indexes from one bounded source and search every
 * child against the attempt's stable window. Reduction is always root-index
 * order, never completion order.
 */
public final class RootParallelSearch implements WindowedSearch {

    public static final int MIN_WORKERS = 1;
    public static final int MAX_WORKERS = 16;
    public static final int DEFAULT_WORKERS = 1;

    public RootParallelSearch(int workerCount) {
        this(new TranspositionTable(), SelectiveSearchPolicy.production(), workerCount);
    }

    public RootParallelSearch(TranspositionTable table, int workerCount) {
        this(table, SelectiveSearchPolicy.production(), workerCount);
    }

    public RootParallelSearch(
        TranspositionTable table, SelectiveSearchPolicy selectiveSearchPolicy,
        int workerCount
    ) {
        this(table, selectiveSearchPolicy, workerCount, RootWorkerHook.NONE);
    }

    RootParallelSearch(
        TranspositionTable table, SelectiveSearchPolicy selectiveSearchPolicy,
        int workerCount, RootWorkerHook workerHook
    ) {
        if(workerCount < MIN_WORKERS || workerCount > MAX_WORKERS) {
            throw new IllegalArgumentException(
                "Root workers must be in " + MIN_WORKERS + ".." + MAX_WORKERS
                    + ": " + workerCount
            );
        }
        this.table = Objects.requireNonNull(table, "table");
        Objects.requireNonNull(selectiveSearchPolicy, "selectiveSearchPolicy");
        this.workerCount = workerCount;
        this.workerHook = Objects.requireNonNull(workerHook, "workerHook");
        final Configuration configuration = new Configuration(
            true, true, true, selectiveSearchPolicy
        );
        singleThread = new AlphaBetaPvsSearch(table, configuration, true);
        workers = new AlphaBetaPvsSearch[workerCount == 1 ? 0 : workerCount];
        workerRootCounts = new int[workers.length];
        workerNodeCounts = new long[workers.length];
        for(int index = 0; index < workers.length; index ++) {
            workers[index] = new AlphaBetaPvsSearch(table, configuration, false);
        }
        executor = workerCount == 1 ? null : Executors.newFixedThreadPool(
            workerCount, new RootThreadFactory()
        );
        if(executor != null) ((ThreadPoolExecutor) executor).prestartAllCoreThreads();
    }

    public int workerCount() {
        return workerCount;
    }

    /** Last completed/aborted attempt's cheap per-context load observation. */
    public RootLoad lastLoad() {
        return new RootLoad(workerRootCounts, workerNodeCounts);
    }

    MoveOrdering rootOrderingForTest() {
        return singleThread.ordering();
    }

    MoveOrdering workerOrderingForTest(int index) {
        return workers[index].ordering();
    }

    @Override
    public SearchResult search(SearchRequest request) {
        beginTopLevelSearch();
        try {
            return searchWindow(request, NEGATIVE_INFINITY, POSITIVE_INFINITY);
        } finally {
            endTopLevelSearch();
        }
    }

    @Override
    public void beginTopLevelSearch() {
        if(closed) throw new IllegalStateException("RootParallelSearch is closed.");
        if(active || topLevelActive) {
            throw new IllegalStateException("RootParallelSearch already has an active search.");
        }
        singleThread.beginTopLevelSearch();
        int begunWorkers = 0;
        try {
            for(; begunWorkers < workers.length; begunWorkers ++) {
                workers[begunWorkers].beginTopLevelSearch();
            }
        } catch(Throwable failure) {
            for(int index = begunWorkers - 1; index >= 0; index --) {
                workers[index].endTopLevelSearch();
            }
            singleThread.endTopLevelSearch();
            throw failure;
        }
        rootDiagnostics.reset();
        diagnosticsScopeInitialized = false;
        topLevelActive = true;
    }

    @Override
    public SearchResult searchWindow(SearchRequest request, int alpha, int beta) {
        if(workerCount == 1) return singleThread.searchWindow(request, alpha, beta);
        if(!topLevelActive) {
            throw new IllegalStateException("A top-level search sequence has not been started.");
        }
        Objects.requireNonNull(request, "request");
        validateWindow(alpha, beta);
        if(request.depth() < 0 || request.depth() > maxSupportedDepth()) {
            throw new IllegalArgumentException("Unsupported search depth: " + request.depth());
        }
        if(active) throw new IllegalStateException("RootParallelSearch is already active.");
        if(request.depth() == 0) return singleThread.searchWindow(request, alpha, beta);

        active = true;
        currentControl = request.control();
        final long startNanos = System.nanoTime();
        final SearchObserver observer = request.observer();
        try {
            initializeDiagnostics(request.diagnosticsEnabled());
            request.copyBoardInto(rootBoard);
            rootDiagnostics.recordRoot();
            table.probe(rootBoard[Board.KEY], request.depth(), alpha, beta, 0, rootProbe);
            if(request.diagnosticsEnabled()) rootDiagnostics.recordTtProbe(rootProbe.outcome());
            final long hashMove = rootProbe.keyMatches()
                ? rootProbe.move() : StagedMovePicker.NO_MOVE;
            final StagedMovePicker rootPicker = singleThread.ordering().picker();
            final int rootMoveCount = rootPicker.prepare(rootBoard, 0, hashMove);
            try {
                if(request.diagnosticsEnabled()
                    && rootPicker.containsLegalMove(0, hashMove)) {
                    rootDiagnostics.recordHashMoveAvailable();
                }
                for(int index = 0; index < rootMoveCount; index ++) {
                    rootMoves[index] = rootPicker.next(0);
                }
            } finally {
                rootPicker.clearPly(0);
            }
            observer.onSearchStarted(
                request.depth(), Eval.evaluate(rootBoard), rootMoveCount
            );

            if(rootMoveCount == 0) {
                final int score = inCheck(rootBoard)
                    ? -TranspositionScores.MATE_SCORE : 0;
                storeRoot(
                    request.depth(), score, StagedMovePicker.NO_MOVE,
                    Cacheability.POSITION_ONLY, alpha, beta
                );
                return finish(
                    observer, new SearchResult(
                        0L, false, score, request.depth(), 0L, 0, true,
                        new long[0], mergedDiagnostics(request.diagnosticsEnabled())
                    ), startNanos
                );
            }

            final SearchLineHistory rootHistory = new SearchLineHistory(request.gameHistory());
            final RuleDraw draw = DrawAdjudicator.adjudicateNonTerminal(rootBoard, rootHistory);
            if(draw != RuleDraw.NONE) {
                final Cacheability cacheability = draw == RuleDraw.INSUFFICIENT_MATERIAL
                    ? Cacheability.POSITION_ONLY : Cacheability.PATH_DEPENDENT;
                storeRoot(request.depth(), 0, rootMoves[0], cacheability, alpha, beta);
                return finish(
                    observer, new SearchResult(
                        rootMoves[0], true, 0, request.depth(), 0L, rootMoveCount,
                        true, new long[] {rootMoves[0]},
                        mergedDiagnostics(request.diagnosticsEnabled())
                    ), startNanos
                );
            }

            if(!request.control().checkpoint()) {
                return incomplete(observer, request, rootMoveCount, startNanos);
            }
            for(int index = 0; index < rootMoveCount; index ++) {
                observer.onRootMoveStarted(index + 1, rootMoveCount, rootMoves[index]);
            }
            Arrays.fill(rootResults, 0, rootMoveCount, null);
            Arrays.fill(workerRootCounts, 0);
            Arrays.fill(workerNodeCounts, 0L);
            final AtomicInteger nextRootIndex = new AtomicInteger();
            final CompletionService<Void> completions =
                new ExecutorCompletionService<>(executor);
            for(int workerIndex = 0; workerIndex < workers.length; workerIndex ++) {
                final int contextIndex = workerIndex;
                completions.submit(() -> {
                    searchPartition(
                        contextIndex, workers[contextIndex], request, alpha, beta,
                        rootMoveCount, nextRootIndex
                    );
                    return null;
                });
            }

            Throwable workerFailure = null;
            boolean interrupted = false;
            for(int completed = 0; completed < workers.length; completed ++) {
                try {
                    completions.take().get();
                } catch(InterruptedException failure) {
                    interrupted = true;
                    request.control().fail();
                    if(workerFailure == null) workerFailure = failure;
                    Thread.interrupted();
                    completed --;
                } catch(ExecutionException failure) {
                    request.control().fail();
                    if(workerFailure == null) workerFailure = failure.getCause();
                }
            }
            if(interrupted) Thread.currentThread().interrupt();
            if(workerFailure != null) throwWorkerFailure(workerFailure);

            for(int index = 0; index < rootMoveCount; index ++) {
                if(rootResults[index] == null || !rootResults[index].completed()) {
                    return incomplete(observer, request, rootMoveCount, startNanos);
                }
            }
            if(!request.control().checkpoint()) {
                return incomplete(observer, request, rootMoveCount, startNanos);
            }

            final int bestIndex = selectBestIndex(rootResults, rootMoveCount);
            final RootChildResult best = rootResults[bestIndex];
            long attemptNodes = 0L;
            boolean pathDependent = false;
            int priorBestScore = NEGATIVE_INFINITY;
            for(int index = 0; index < rootMoveCount; index ++) {
                final RootChildResult candidate = rootResults[index];
                attemptNodes = saturatedAdd(attemptNodes, candidate.nodes());
                pathDependent |= candidate.pathDependent();
                final boolean improved = candidate.score() > priorBestScore;
                if(improved) priorBestScore = candidate.score();
                observer.onRootMoveFinished(
                    index + 1, rootMoveCount, candidate.move(), candidate.score(), improved,
                    candidate.nodes(), rootElapsedNanos[index]
                );
            }
            if(best.score() >= beta) {
                singleThread.ordering().recordQuietCutoff(
                    rootBoard, 0, best.move(), request.depth()
                );
            }
            storeRoot(
                request.depth(), best.score(), best.move(),
                pathDependent ? Cacheability.PATH_DEPENDENT : Cacheability.POSITION_ONLY,
                alpha, beta
            );
            return finish(
                observer, new SearchResult(
                    best.move(), true, best.score(), request.depth(), attemptNodes,
                    rootMoveCount, true, best.principalVariation(),
                    mergedDiagnostics(request.diagnosticsEnabled())
                ), startNanos
            );
        } finally {
            currentControl = null;
            active = false;
        }
    }

    @Override
    public void endTopLevelSearch() {
        if(active) throw new IllegalStateException("Cannot end an active root attempt.");
        if(!topLevelActive) throw new IllegalStateException("No top-level search is active.");
        Throwable failure = null;
        for(int index = workers.length - 1; index >= 0; index --) {
            try {
                workers[index].endTopLevelSearch();
            } catch(Throwable candidate) {
                if(failure == null) failure = candidate;
                else failure.addSuppressed(candidate);
            }
        }
        try {
            singleThread.endTopLevelSearch();
        } catch(Throwable candidate) {
            if(failure == null) failure = candidate;
            else failure.addSuppressed(candidate);
        }
        topLevelActive = false;
        if(failure != null) throwWorkerFailure(failure);
    }

    @Override
    public int maxSupportedDepth() {
        return singleThread.maxSupportedDepth();
    }

    @Override
    public void newGame() {
        singleThread.newGame();
        for(AlphaBetaPvsSearch worker : workers) worker.newGame();
    }

    @Override
    public void close() {
        if(closed) return;
        closed = true;
        final SearchControl control = currentControl;
        if(control != null) control.request(SearchTermination.SHUTDOWN);
        if(executor == null) return;
        executor.shutdownNow();
        boolean interrupted = false;
        try {
            if(!executor.awaitTermination(SHUTDOWN_MILLIS, TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("Root worker executor did not terminate.");
            }
        } catch(InterruptedException failure) {
            interrupted = true;
        } finally {
            if(interrupted) Thread.currentThread().interrupt();
        }
    }

    private static final int NEGATIVE_INFINITY = -TranspositionScores.MATE_SCORE - 1;
    private static final int POSITIVE_INFINITY = TranspositionScores.MATE_SCORE + 1;
    private static final int MAX_MOVES = StagedMovePicker.MAX_MOVES;
    private static final long SHUTDOWN_MILLIS = 2_000L;

    private final TranspositionTable table;
    private final int workerCount;
    private final AlphaBetaPvsSearch singleThread;
    private final AlphaBetaPvsSearch[] workers;
    private final ExecutorService executor;
    private final RootWorkerHook workerHook;
    private final Probe rootProbe = new Probe();
    private final SearchDiagnostics rootDiagnostics = new SearchDiagnostics();
    private final long[] rootBoard = new long[Board.MAX_BITBOARDS];
    private final long[] rootMoves = new long[MAX_MOVES];
    private final RootChildResult[] rootResults = new RootChildResult[MAX_MOVES];
    private final long[] rootElapsedNanos = new long[MAX_MOVES];
    private final int[] workerRootCounts;
    private final long[] workerNodeCounts;
    private volatile SearchControl currentControl;
    private boolean active;
    private boolean topLevelActive;
    private boolean diagnosticsScopeInitialized;
    private boolean diagnosticsEnabled;
    private volatile boolean closed;

    private void searchPartition(
        int workerIndex, AlphaBetaPvsSearch worker, SearchRequest request, int alpha, int beta,
        int rootMoveCount, AtomicInteger nextRootIndex
    ) throws Exception {
        final SearchLineHistory history = new SearchLineHistory(request.gameHistory());
        worker.workerDiagnostics(request.diagnosticsEnabled());
        while(request.control().checkpoint()) {
            final int index = nextRootIndex.getAndIncrement();
            if(index >= rootMoveCount) return;
            final long move = rootMoves[index];
            workerHook.beforeRootMove(index, move);
            final long startNanos = System.nanoTime();
            rootResults[index] = worker.searchRootMove(request, history, move, alpha, beta);
            rootElapsedNanos[index] = System.nanoTime() - startNanos;
            workerRootCounts[workerIndex] ++;
            workerNodeCounts[workerIndex] = saturatedAdd(
                workerNodeCounts[workerIndex], rootResults[index].nodes()
            );
            if(!rootResults[index].completed()) return;
        }
    }

    private SearchResult incomplete(
        SearchObserver observer, SearchRequest request, int rootMoveCount, long startNanos
    ) {
        long nodes = 0L;
        for(int index = 0; index < rootMoveCount; index ++) {
            if(rootResults[index] != null) {
                nodes = saturatedAdd(nodes, rootResults[index].nodes());
            }
        }
        return finish(
            observer, new SearchResult(
                0L, false, 0, request.depth(), nodes, rootMoveCount, false,
                new long[0], mergedDiagnostics(request.diagnosticsEnabled())
            ), startNanos
        );
    }

    private SearchDiagnosticsSnapshot mergedDiagnostics(boolean enabled) {
        if(!enabled) return SearchDiagnosticsSnapshot.disabled();
        SearchDiagnosticsSnapshot merged = rootDiagnostics.snapshot();
        for(AlphaBetaPvsSearch worker : workers) {
            merged = merged.mergeWorkers(worker.workerDiagnostics(true));
        }
        return merged;
    }

    private void initializeDiagnostics(boolean enabled) {
        if(!diagnosticsScopeInitialized) {
            diagnosticsScopeInitialized = true;
            diagnosticsEnabled = enabled;
            return;
        }
        if(enabled != diagnosticsEnabled) {
            throw new IllegalArgumentException(
                "Every exact attempt in one top-level search must use the same diagnostics mode."
            );
        }
    }

    private void storeRoot(
        int depth, int score, long move, Cacheability cacheability,
        int alpha, int beta
    ) {
        final StoreOutcome outcome = table.store(
            rootBoard[Board.KEY], depth, classify(score, alpha, beta), score, 0,
            move, cacheability
        );
        if(diagnosticsEnabled && outcome == StoreOutcome.STORED) {
            rootDiagnostics.recordTtStore();
        }
    }

    private static SearchResult finish(
        SearchObserver observer, SearchResult result, long startNanos
    ) {
        observer.onSearchFinished(result, System.nanoTime() - startNanos);
        return result;
    }

    private static Bound classify(int score, int alpha, int beta) {
        if(score >= beta) return Bound.LOWER;
        if(score <= alpha) return Bound.UPPER;
        return Bound.EXACT;
    }

    private static boolean inCheck(long[] board) {
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

    private static long saturatedAdd(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    static int selectBestIndex(RootChildResult[] results, int count) {
        if(count < 1 || count > results.length) {
            throw new IllegalArgumentException("Invalid completed root-result count: " + count);
        }
        int best = 0;
        for(int index = 1; index < count; index ++) {
            if(results[index].score() > results[best].score()) best = index;
        }
        return best;
    }

    private static void throwWorkerFailure(Throwable failure) {
        if(failure instanceof RuntimeException runtime) throw runtime;
        if(failure instanceof Error error) throw error;
        throw new RootWorkerException("Root worker failed.", failure);
    }

    @FunctionalInterface
    interface RootWorkerHook {
        RootWorkerHook NONE = (index, move) -> {};

        void beforeRootMove(int rootIndex, long move) throws Exception;
    }

    public static final class RootWorkerException extends RuntimeException {
        RootWorkerException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public record RootLoad(int[] rootMoves, long[] nodes) {
        public RootLoad {
            rootMoves = rootMoves.clone();
            nodes = nodes.clone();
        }

        @Override
        public int[] rootMoves() {
            return rootMoves.clone();
        }

        @Override
        public long[] nodes() {
            return nodes.clone();
        }
    }

    private static final class RootThreadFactory implements ThreadFactory {
        private final AtomicInteger sequence = new AtomicInteger();

        @Override
        public Thread newThread(Runnable task) {
            return new Thread(task, "seedv6-root-worker-" + sequence.incrementAndGet());
        }
    }
}
