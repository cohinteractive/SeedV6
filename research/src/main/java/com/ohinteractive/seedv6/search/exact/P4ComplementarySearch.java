package com.ohinteractive.seedv6.search.exact;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntFunction;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.diagnostics.RootSearchObservation;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.manage.ClockPolicy;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;

/** IC-C3 Complementary-depth helpers, a research-only copy of exact96 IndependentSearch.
 * OWNER retains adjacent ADAPTIVE16 iterations, selection and unchanged BOUNDARY2.
 * Helpers visit alternating depth parities: workers1/3 even, worker2 odd atW4.
 * Skipped depths are never claimed complete; the existing aspiration adjacency
 * guard gives helpers full windows across their gaps. Shared TT remains heuristic,
 * not independent confidence. Global hard stops, accounting and joins are unchanged. */
public final class P4ComplementarySearch implements IterativeSearch {
    private final Worker[] workers;
    private final ExecutorService pool;
    private final Future<?>[] futures;
    private final TTable table;
    private final AtomicBoolean abort = new AtomicBoolean();
    private final AtomicReference<Throwable> failure = new AtomicReference<>();
    private final AtomicInteger participating = new AtomicInteger();
    private final AtomicLong unlimitedAdmissions = new AtomicLong();
    private SearchRequest request;
    private SearchControl control;
    private long[] root;
    private int generation;
    private boolean active, closed;
    private SearchResult lastCompleted;
    private SearchDiagnosticsSnapshot lastDiagnostics = SearchDiagnosticsSnapshot.disabled();

    public static P4ComplementarySearch constructionResearch(int count, SearchEvaluation evaluation,
            TTable table, FlatSearchConfig config, AspirationSearch.Policy aspiration, ClockPolicy clocks) {
        return new P4ComplementarySearch(count, evaluation, table, config, aspiration, clocks, null);
    }

    /** Package-local evaluator witnesses; each worker owns a distinct mutable evaluator. */
    P4ComplementarySearch(int count, SearchEvaluation evaluation, TTable table, FlatSearchConfig config,
            AspirationSearch.Policy aspiration, ClockPolicy clocks, IntFunction<ExactEvaluator> evaluators) {
        if(count < 2 || count > 16) throw new IllegalArgumentException("Independent SMP requires 2..16 workers");
        Objects.requireNonNull(evaluation); Objects.requireNonNull(config); Objects.requireNonNull(aspiration);
        Objects.requireNonNull(clocks); this.table = Objects.requireNonNull(table);
        workers = new Worker[count]; futures = new Future<?>[count-1];
        for(int i=0;i<count;i++) workers[i] = new Worker(i,
                evaluators == null ? ExactEvaluator.from(evaluation) : evaluators.apply(i), config, aspiration, clocks);
        pool = Executors.newFixedThreadPool(count-1, task -> {
            var thread = new Thread(task, "seedv6-independent-search"); thread.setDaemon(true); return thread;
        });
    }

    private final class Worker implements Runnable, SingleDepthSearch {
        final int index;
        final ExactSearch exact;
        final SingleDepthSearch iterativeWindow;
        final long[] board = new long[Board.MAX_BITBOARDS];
        long nodes, qnodes, evaluations, attempts, lows, highs, fallbacks;
        int maximumPly, maximumQply, attemptedDepth, completedDepth;
        boolean incomplete, target, terminal;
        SearchResult completed;

        Worker(int index, ExactEvaluator evaluator, FlatSearchConfig config,
                AspirationSearch.Policy aspiration, ClockPolicy clocks) {
            this.index = index; Objects.requireNonNull(evaluator);
            exact = ExactSearch.constructionResearch(new ExactEvaluator() {
                @Override public void initialize(long[] position) { evaluator.initialize(position); }
                @Override public void child(long[] parent, long[] child, int ply) { child(parent,child,ply,-1); }
                @Override public void child(long[] parent, long[] child, int ply, int qply) {
                    if(!admit()) return;
                    nodes++;
                    if(qply >= 0) { qnodes++; maximumQply = Math.max(maximumQply,qply); }
                    maximumPly = Math.max(maximumPly,ply+1);
                    evaluator.child(parent,child,ply,qply);
                }
                @Override public void reenter(int ply) {
                    if(!admit()) return;
                    nodes++; maximumPly = Math.max(maximumPly,ply); evaluator.reenter(ply);
                }
                @Override public int evaluate(long[] position, int ply) {
                    evaluations++; return evaluator.evaluate(position,ply);
                }
            }, table, config);
            var resolved = AspirationSearch.researchWindows(this,this::window,aspiration);
            iterativeWindow = index == 0 ? clocks.apply(resolved) : resolved;
        }

        void reset() {
            nodes=qnodes=evaluations=attempts=lows=highs=fallbacks=0;
            maximumPly=maximumQply=attemptedDepth=completedDepth=0;
            incomplete=target=terminal=false; completed=null;
        }

        private boolean admit() {
            if(!control.tryEnterNode()) return false;
            if(control.isUnlimited()) unlimitedAdmissions.incrementAndGet();
            return true;
        }

        @Override public void beginRequest() { exact.beginSharedRequest(generation); }
        @Override public void endRequest() { exact.endRequest(); }
        @Override public int maxSupportedDepth() { return ExactSearch.MAX_DEPTH; }
        @Override public SearchResult search(SearchRequest next) {
            return window(next,-ExactSearch.INFINITY,ExactSearch.INFINITY);
        }

        private NodeMetrics metrics() {
            return new NodeMetrics(nodes-qnodes,qnodes,maximumPly,maximumQply,evaluations);
        }

        SearchResult window(SearchRequest next, int alpha, int beta) {
            var before=metrics(); next.copyBoardInto(board);
            var result=exact.searchWindow(board,next.gameHistory(),next.depth(),alpha,beta,P4ComplementarySearch.this::cancelled);
            if(Thread.currentThread().isInterrupted()) control.request(SearchTermination.STOPPED);
            var diagnostics=SearchDiagnosticsSnapshot.disabled();
            if(next.diagnosticsEnabled()) {
                var empty=SearchDiagnosticsSnapshot.enabledEmpty(); var w=empty.worker();
                diagnostics=new SearchDiagnosticsSnapshot(true,new WorkerMetrics(
                        new NodeMetrics(nodes-qnodes-before.mainNodes(),qnodes-before.qNodes(),
                                maximumPly,maximumQply,evaluations-before.evaluationCalls()),
                        w.transpositionTable(),w.moveOrder(),w.qsearch(),w.selective()),empty.iteration());
            }
            return new SearchResult(result.bestMove(),result.hasMove(),result.score(),next.depth(),
                    nodes-before.totalEnteredNodes(),legalRootMoves,result.completed(),result.principalVariation(),
                    diagnostics,result.selective());
        }

        @Override public void run() {
            participating.incrementAndGet();
            try {
                int firstDepth=Math.min(request.depth(),index==0?1:1+(index&1));
                int stride=index==0?1:2;
                for(int depth=firstDepth;depth<=request.depth();depth+=stride) {
                    if(Thread.currentThread().isInterrupted()) control.request(SearchTermination.STOPPED);
                    if(cancelled() || !control.checkpointNodeBudget()) break;
                    attemptedDepth=depth;
                    var result=iterativeWindow.search(new SearchRequest(root,request.gameHistory(),depth,
                            SearchObserver.NONE,control,request.diagnosticsEnabled()));
                    if(request.diagnosticsEnabled()) {
                        var m=result.diagnostics().iteration(); attempts+=m.aspirationAttempts();
                        lows+=m.failLowResearches(); highs+=m.failHighResearches(); fallbacks+=m.fullWindowFallbacks();
                    }
                    incomplete=!result.completed();
                    if(incomplete) break;
                    completedDepth=depth;
                    completed=copy(result,nodes,SearchDiagnosticsSnapshot.disabled());
                    if(index==0) {
                        // Nodes are the exact global admission snapshot. Live helper
                        // diagnostics are explicitly unavailable until join, rather
                        // than publishing racing private counters as global totals.
                        lastCompleted=copy(result,admissions(),SearchDiagnosticsSnapshot.disabled());
                        long elapsed=control.isUnlimited()?SearchControl.elapsedNanos(System.nanoTime(),started)
                                :control.elapsedNanos();
                        request.observer().onIterationCompleted(IterationSnapshot.from(lastCompleted,elapsed));
                    }
                    terminal=!result.hasMove(); target=terminal || depth==request.depth();
                    if(target) break;
                    if(index==0 && control.isClockManaged() && (result.legalRootMoves()==1
                            || result.score()==TranspositionScores.MATE_SCORE-1
                            || iterativeWindow.clockAllocationComplete(completed,
                                    control.elapsedNanos(),control.timeBudgetNanos()))) {
                        control.completeClockAllocation(); break;
                    }
                }
            } catch(Throwable problem) { fail(problem); }
            finally { participating.decrementAndGet(); }
        }
    }

    private long started;
    private int legalRootMoves;
    private boolean cancelled() { return abort.get() || !control.checkpoint(); }
    private long admissions() { return control.isUnlimited()?unlimitedAdmissions.get():control.nodes(); }
    private void fail(Throwable problem) { failure.compareAndSet(null,problem); abort.set(true); control.fail(); }
    private static SearchResult copy(SearchResult r,long nodes,SearchDiagnosticsSnapshot diagnostics) {
        return new SearchResult(r.bestMove(),r.hasMove(),r.score(),r.depth(),nodes,r.legalRootMoves(),
                r.completed(),r.principalVariation(),diagnostics,r.selective());
    }

    @Override public SearchDriverOutcome search(SearchRequest next) {
        Objects.requireNonNull(next); ensureIdle();
        if(next.depth()<1 || next.depth()>maxSupportedDepth()) throw new IllegalArgumentException("Unsupported iterative depth");
        var observation=RootSearchObservation.current();
        if(observation!=null) {
            var position=new long[Board.MAX_BITBOARDS]; next.copyBoardInto(position);
            next=new SearchRequest(position,next.gameHistory(),next.depth(),next.observer(),next.control(),true);
            observation.started(next);
        }
        long observationStart=System.nanoTime();
        var outcome=execute(next);
        if(observation!=null) observation.finished(next,outcome,System.nanoTime()-observationStart);
        return outcome;
    }

    private SearchDriverOutcome execute(SearchRequest next) {
        active=true; request=next; control=next.control(); started=System.nanoTime();
        abort.set(false); failure.set(null); unlimitedAdmissions.set(0); lastCompleted=null;
        lastDiagnostics=next.diagnosticsEnabled()?SearchDiagnosticsSnapshot.enabledEmpty():SearchDiagnosticsSnapshot.disabled();
        root=new long[Board.MAX_BITBOARDS]; next.copyBoardInto(root);
        for(var worker:workers) worker.reset();
        int begun=0; boolean interrupted=false;
        try {
            var scratch=new long[Board.MAX_BITBOARDS]; var moves=new long[512];
            legalRootMoves=Gen.genAll(root[0],root[1],root[2],root[3],(int)root[Board.STATUS],root[Board.KEY],true,moves,scratch);
            table.advanceGeneration(); generation=(generation+1)&255;
            for(var worker:workers) { worker.iterativeWindow.beginRequest(); begun++; }
            for(int i=1;i<workers.length;i++) futures[i-1]=pool.submit(workers[i]);
            workers[0].run();
        } catch(Throwable problem) { fail(problem); }
        finally {
            // Local stop preserves COMPLETED and the real global stop reason.
            // Never cancel Future as a substitute for waiting for its worker.
            abort.set(true);
            for(int i=0;i<futures.length;i++) if(futures[i]!=null) {
                for(;;) try { futures[i].get(); break; }
                catch(InterruptedException e) { interrupted=true; control.request(SearchTermination.STOPPED); }
                catch(ExecutionException e) { fail(e.getCause()); break; }
                futures[i]=null;
            }
            for(int i=0;i<begun;i++) try { workers[i].iterativeWindow.endRequest(); }
            catch(Throwable problem) { fail(problem); }
            updateDiagnostics(); active=false;
            if(interrupted) Thread.currentThread().interrupt();
        }
        long nodes=0; for(var worker:workers) nodes+=worker.nodes;
        if(nodes!=admissions()) fail(new IllegalStateException("Independent request callback accounting mismatch"));
        var problem=failure.get();
        if(problem!=null) {
            if(problem instanceof Error error) throw error;
            if(problem instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("Independent Search worker failed",problem);
        }
        var owner=workers[0];
        return new SearchDriverOutcome(lastCompleted,owner.target,owner.terminal,owner.attemptedDepth,
                owner.incomplete,nodes,lastDiagnostics);
    }

    private void updateDiagnostics() {
        if(!request.diagnosticsEnabled()) return;
        var total=SearchDiagnosticsSnapshot.enabledEmpty();
        long attempts=0,lows=0,highs=0,fallbacks=0;
        for(var worker:workers) {
            var e=SearchDiagnosticsSnapshot.enabledEmpty(); var w=e.worker();
            total=total.mergeWorkers(new SearchDiagnosticsSnapshot(true,new WorkerMetrics(worker.metrics(),
                    w.transpositionTable(),w.moveOrder(),w.qsearch(),w.selective()),e.iteration()));
            attempts+=worker.attempts; lows+=worker.lows; highs+=worker.highs; fallbacks+=worker.fallbacks;
        }
        int depth=workers[0].completedDepth;
        lastDiagnostics=total.withIteration(new IterationMetrics(depth,attempts,lows,highs,fallbacks,depth));
    }

    /** Quiescent actual worker progress for research; not a voting result or proof. */
    public record WorkerProgress(int worker,long nodes,int attemptedDepth,int completedDepth,boolean incomplete,
                                 SearchResult completedResult) {}
    public WorkerProgress[] workerProgress() {
        ensureIdle(); var result=new WorkerProgress[workers.length];
        for(int i=0;i<workers.length;i++) {
            var w=workers[i]; result[i]=new WorkerProgress(i,w.nodes,w.attemptedDepth,w.completedDepth,w.incomplete,w.completed);
        }
        return result;
    }
    private void ensureIdle() {
        if(closed || active) throw new IllegalStateException("Independent Search is active or closed");
    }
    @Override public int maxSupportedDepth() { return ExactSearch.MAX_DEPTH; }
    @Override public int activeSearchThreads() { return participating.get(); }
    @Override public SearchResult lastCompletedResult() { return lastCompleted; }
    @Override public SearchDiagnosticsSnapshot lastDiagnostics() { return lastDiagnostics; }
    @Override public void newGame() {
        ensureIdle(); table.clear(); for(var worker:workers) worker.exact.clearRetainedCorrection();
        lastCompleted=null; lastDiagnostics=SearchDiagnosticsSnapshot.disabled();
    }
    @Override public void close() {
        if(closed) return; ensureIdle(); closed=true; pool.shutdown();
        boolean interrupted=false;
        for(;;) try { if(pool.awaitTermination(1,TimeUnit.SECONDS)) break; }
        catch(InterruptedException e) { interrupted=true; }
        if(interrupted) Thread.currentThread().interrupt();
    }
}
