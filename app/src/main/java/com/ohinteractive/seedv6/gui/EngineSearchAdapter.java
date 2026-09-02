package com.ohinteractive.seedv6.gui;

import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.alphabeta.RootParallelSearch;
import com.ohinteractive.seedv6.search.common.IterationSnapshot;
import com.ohinteractive.seedv6.search.common.SearchObserver;
import com.ohinteractive.seedv6.search.common.SearchTermination;
import com.ohinteractive.seedv6.search.manage.ManagedSearchResult;
import com.ohinteractive.seedv6.search.manage.SearchLifecycleService;
import com.ohinteractive.seedv6.search.manage.SearchLimits;

/**
 * UI adapter around the single current managed lifecycle service. Search and
 * root execution remain wholly owned by {@link SearchLifecycleService}.
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
        final SearchLifecycleService owner = service;
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
        service.stop();
    }

    @Override
    public void invalidate(SearchTermination reason) {
        requireEdt();
        ensureOpen();
        current = null;
        service.invalidate(reason);
    }

    @Override
    public boolean isSearching() {
        requireEdt();
        return current != null;
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
        if(current != null || service.isSearching()) {
            throw new IllegalStateException("Threads cannot be changed while a result is pending.");
        }
        final SearchLifecycleService previous = service;
        current = null;
        previous.close();
        service = lifecycleFactory.create(requestedWorkers);
        workerCount = requestedWorkers;
    }

    @Override
    public Runnable beginShutdown() {
        requireEdt();
        if(closing) return () -> {};
        closing = true;
        current = null;
        service.invalidate(SearchTermination.POSITION_CHANGED);
        final SearchLifecycleService ownedService = service;
        return ownedService::close;
    }

    private final Consumer<Runnable> edtQueue;
    private final LifecycleFactory lifecycleFactory;
    private SearchLifecycleService service;
    private Request current;
    private int workerCount;
    private boolean closing;

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
            && service == request.owner
            && request.generation == generation
            && request.owner.generation() == generation;
    }

    private void ensureOpen() {
        if(closing) throw new IllegalStateException("Search adapter is closing.");
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

    int workerCount();

    void replaceWorkerCount(int requestedWorkers);

    /** Marks callback delivery closed on the EDT and returns the potentially blocking cleanup. */
    Runnable beginShutdown();
}
