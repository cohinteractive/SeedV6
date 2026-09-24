package com.ohinteractive.seedv6.gui;

import java.util.Objects;
import java.util.function.Consumer;
import java.nio.file.Path;
import java.util.concurrent.*;

import javax.swing.SwingUtilities;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.alphabeta.RootParallelSearch;
import com.ohinteractive.seedv6.search.common.IterationSnapshot;
import com.ohinteractive.seedv6.search.common.SearchObserver;
import com.ohinteractive.seedv6.search.common.SearchTermination;
import com.ohinteractive.seedv6.search.manage.ManagedSearchResult;
import com.ohinteractive.seedv6.search.manage.SearchLifecycleService;
import com.ohinteractive.seedv6.search.manage.SearchLimits;

/**
 * UI adapter around fixed per-participant managed lifecycle services. Search and
 * single-thread search execution remain wholly owned by {@link SearchLifecycleService}.
 */
final class EngineSearchAdapter implements SearchGateway {

    EngineSearchAdapter(int rootWorkers) {
        this(rootWorkers, SwingUtilities::invokeLater, SearchLifecycleService::new);
    }

    EngineSearchAdapter(
        int rootWorkers, Consumer<Runnable> edtQueue, LifecycleFactory lifecycleFactory
    ) {
        this.edtQueue = Objects.requireNonNull(edtQueue, "edtQueue");
        this.lifecycleFactory = Objects.requireNonNull(lifecycleFactory, "lifecycleFactory");
        validateWorkerCount(rootWorkers);
        workerCount = rootWorkers;
        service = lifecycleFactory.create(rootWorkers);
        blackService = service;
    }

    @Override
    public void start(
        long[] board, GameHistory history, SearchLimits limits,
        Object uiToken, Listener listener
    ) {
        requireEdt();
        ensureOpen();
        Objects.requireNonNull(uiToken, "uiToken");
        Objects.requireNonNull(listener, "listener");
        final SearchLifecycleService owner = Board.player((int) board[Board.STATUS]) == Value.WHITE ? service : blackService;
        if (current != null && current.owner != owner) current.owner.invalidate(SearchTermination.POSITION_CHANGED);
        final Request request = new Request(owner, uiToken, listener);
        current = request;
        try {
            request.generation = owner.start(
                board, history, limits,
                new SearchObserver() {
                    @Override
                    public void onIterationCompleted(IterationSnapshot snapshot) {
                        edtQueue.accept(() -> deliverIteration(request, snapshot));
                    }
                },
                result -> edtQueue.accept(() -> deliverResult(request, result))
            );
        } catch(RuntimeException exception) {
            if(current == request) current = null;
            throw exception;
        }
    }

    @Override
    public void stop() {
        requireEdt();
        ensureOpen();
        if (current != null) current.owner.stop();
    }

    @Override
    public void invalidate(SearchTermination reason) {
        requireEdt();
        ensureOpen();
        current = null;
        service.invalidate(reason);
        if (blackService != service) blackService.invalidate(reason);
    }

    @Override
    public boolean isSearching() {
        requireEdt();
        return current != null;
    }

    @Override public boolean isBusy() {
        requireEdt();
        return transitioning || current != null || (service != null && service.isWorking())
                || (blackService != null && blackService.isWorking());
    }

    @Override public PlayEvaluator evaluator() { return participants.white(); }
    @Override public PlayParticipants participants() { return participants; }

    @Override public void changeEvaluator(PlayEvaluator.Mode mode, Path root, Consumer<String> completed) {
        changeParticipants(mode, root, PlayParticipants.Selection.BEST, completed);
    }

    @Override public void changeParticipants(PlayEvaluator.Mode mode, Path root,
                                             PlayParticipants.Selection selection, Consumer<String> completed) {
        requireEdt();
        ensureOpen();
        if (transitioning) throw new IllegalStateException("An evaluator change is already pending.");
        transitioning = true;
        current = null;
        SearchLifecycleService previous = service;
        SearchLifecycleService previousBlack = blackService;
        previous.invalidate(SearchTermination.POSITION_CHANGED);
        if (previousBlack != previous) previousBlack.invalidate(SearchTermination.POSITION_CHANGED);
        PlayParticipants prior = participants;
        int workers = workerCount;
        lifecycleIo.execute(() -> {
            String error = null;
            try {
                // Retire the previous worker/search/evaluator state before constructing a replacement.
                closeServices(previous, previousBlack);
                service = null; blackService = null;
                PlayParticipants next = PlayParticipants.load(mode, root, selection);
                if (!closing) {
                    installServices(workers, next);
                    participants = next;
                }
            } catch (Exception failure) {
                error = "Evaluator change failed: " + TrainingController.concise(failure);
                if (service == null && !closing) {
                    try { installServices(workers, prior); }
                    catch (RuntimeException restoreFailure) { error += "; search unavailable: " + TrainingController.concise(restoreFailure); }
                }
                error += ". Previous game and networks are unchanged; game is paused.";
            }
            String outcome = error;
            SwingUtilities.invokeLater(() -> {
                transitioning = false;
                if (!closing) completed.accept(outcome);
            });
        });
    }

    private SearchLifecycleService createLifecycle(int workers, PlayEvaluator binding) {
        return lifecycleFactory.create(workers, binding);
    }

    private void installServices(int workers, PlayParticipants bindings) {
        SearchLifecycleService white = createLifecycle(workers, bindings.white());
        try {
            SearchLifecycleService black = bindings.black() == bindings.white() ? white : createLifecycle(workers, bindings.black());
            service = white; blackService = black;
        } catch (RuntimeException failure) { white.close(); throw failure; }
    }

    private static void closeServices(SearchLifecycleService white, SearchLifecycleService black) {
        try { if (white != null) white.close(); }
        finally { if (black != null && black != white) black.close(); }
        if (white != null && !white.isTerminated() || black != null && !black.isTerminated())
            throw new IllegalStateException("Previous search worker has not terminated. Wait before changing evaluator.");
    }

    @Override public void listNetworks(Path root, Consumer<CheckpointStore.AvailableCheckpoints> completed,
                                       Consumer<String> failed) {
        requireEdt();
        if (closing) return;
        lifecycleIo.execute(() -> {
            try {
                var all = CheckpointStore.availableCheckpoints(root);
                var available = new CheckpointStore.AvailableCheckpoints(all.checkpoints().stream()
                        .filter(m -> m.architecture() == com.ohinteractive.seedv6.training.model.TrainingArchitecture.NNUE)
                        .toList(), all.diagnostics());
                SwingUtilities.invokeLater(() -> { if (!closing) completed.accept(available); });
            } catch (Exception failure) {
                String message = TrainingController.concise(failure);
                SwingUtilities.invokeLater(() -> { if (!closing) failed.accept(message); });
            }
        });
    }

    @Override
    public int workerCount() {
        return workerCount;
    }

    @Override
    public void replaceWorkerCount(int requestedWorkers) {
        requireEdt();
        ensureOpen();
        validateWorkerCount(requestedWorkers);
        if(requestedWorkers == workerCount) return;
        if(isBusy()) {
            throw new IllegalStateException("Threads cannot be changed while a result is pending.");
        }
        current = null;
        closeServices(service, blackService);
        installServices(requestedWorkers, participants);
        workerCount = requestedWorkers;
    }

    @Override
    public Runnable beginShutdown() {
        requireEdt();
        if(closing) return this::awaitShutdown;
        closing = true;
        current = null;
        if (service != null) service.invalidate(SearchTermination.POSITION_CHANGED);
        if (blackService != null && blackService != service) blackService.invalidate(SearchTermination.POSITION_CHANGED);
        shutdown = lifecycleIo.submit(() -> {
            closeServices(service, blackService);
        });
        lifecycleIo.shutdown();
        return this::awaitShutdown;
    }

    private void awaitShutdown() {
        try {
            shutdown.get(35, TimeUnit.SECONDS);
            if (!lifecycleIo.awaitTermination(1, TimeUnit.SECONDS)) throw new IllegalStateException("Search cleanup is still running.");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt(); throw new IllegalStateException("Interrupted while closing search.", interrupted);
        } catch (ExecutionException | TimeoutException failure) {
            throw new IllegalStateException("Search shutdown failed: " + TrainingController.concise(failure), failure);
        }
    }

    private final Consumer<Runnable> edtQueue;
    private final LifecycleFactory lifecycleFactory;
    private volatile SearchLifecycleService service;
    private volatile SearchLifecycleService blackService;
    private volatile PlayParticipants participants = PlayParticipants.shared(PlayEvaluator.handcrafted());
    private final ExecutorService lifecycleIo = Executors.newSingleThreadExecutor(r -> new Thread(r, "seedv6-ui-search-io"));
    private Future<?> shutdown;
    private boolean transitioning;
    private Request current;
    private int workerCount;
    private volatile boolean closing;

    private void deliverIteration(Request request, IterationSnapshot snapshot) {
        requireEdt();
        if(isCurrent(request, request.generation)) {
            request.listener.onIteration(request.uiToken, snapshot);
        }
    }

    private void deliverResult(Request request, ManagedSearchResult result) {
        requireEdt();
        if(!isCurrent(request, result.generation())) return;
        current = null;
        request.listener.onComplete(request.uiToken, result);
    }

    private boolean isCurrent(Request request, long generation) {
        return !closing
            && current == request
            && (service == request.owner || blackService == request.owner)
            && request.generation == generation
            && request.owner.generation() == generation;
    }

    private void ensureOpen() {
        if(closing) throw new IllegalStateException("Search adapter is closing.");
        if (service == null || transitioning) throw new IllegalStateException("Search evaluator is unavailable or changing.");
    }

    private static void validateWorkerCount(int rootWorkers) {
        if(rootWorkers < RootParallelSearch.MIN_WORKERS
            || rootWorkers > RootParallelSearch.MAX_WORKERS) {
            throw new IllegalArgumentException(
                "Threads must be in " + RootParallelSearch.MIN_WORKERS
                    + ".." + RootParallelSearch.MAX_WORKERS + ": " + rootWorkers
            );
        }
    }

    private static void requireEdt() {
        if(!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Search adapter access must occur on the EDT.");
        }
    }

    @FunctionalInterface
    interface LifecycleFactory {
        SearchLifecycleService create(int rootWorkers);
        default SearchLifecycleService create(int rootWorkers, PlayEvaluator binding) {
            return binding.mode() == PlayEvaluator.Mode.HANDCRAFTED ? create(rootWorkers)
                    : new SearchLifecycleService(rootWorkers, binding.evaluation());
        }
    }

    private static final class Request {
        private final SearchLifecycleService owner;
        private final Object uiToken;
        private final Listener listener;
        private long generation = -1L;

        private Request(
            SearchLifecycleService owner, Object uiToken, Listener listener
        ) {
            this.owner = owner;
            this.uiToken = uiToken;
            this.listener = listener;
        }
    }
}

/** Small UI-owned port permitting deterministic controller tests without an engine lifecycle clone. */
interface SearchGateway {

    interface Listener {
        void onIteration(Object uiToken, IterationSnapshot snapshot);
        void onComplete(Object uiToken, ManagedSearchResult result);
    }

    void start(
        long[] board, GameHistory history, SearchLimits limits,
        Object uiToken, Listener listener
    );

    void stop();

    void invalidate(SearchTermination reason);

    boolean isSearching();

    default boolean isBusy() { return isSearching(); }

    default PlayEvaluator evaluator() { return PlayEvaluator.handcrafted(); }

    default PlayParticipants participants() { return PlayParticipants.shared(evaluator()); }

    default void changeParticipants(PlayEvaluator.Mode mode, Path root, PlayParticipants.Selection selection,
                                    Consumer<String> completed) {
        if (!selection.equals(PlayParticipants.Selection.BEST))
            throw new UnsupportedOperationException("Per-side evaluator selection is not supported by this search gateway.");
        changeEvaluator(mode, root, completed);
    }

    default void listNetworks(Path root, Consumer<CheckpointStore.AvailableCheckpoints> completed, Consumer<String> failed) {
        completed.accept(new CheckpointStore.AvailableCheckpoints(java.util.List.of(), java.util.List.of()));
    }

    default void changeEvaluator(PlayEvaluator.Mode mode, Path root, Consumer<String> completed) {
        throw new UnsupportedOperationException("Evaluator selection is not supported by this search gateway.");
    }

    int workerCount();

    void replaceWorkerCount(int requestedWorkers);

    /** Marks callback delivery closed on the EDT and returns the potentially blocking cleanup. */
    Runnable beginShutdown();
}
