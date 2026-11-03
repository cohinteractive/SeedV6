package com.ohinteractive.seedv6.gui;

import java.util.*;
import java.util.concurrent.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.BaselineEngine;
import com.ohinteractive.seedv6.search.exact.ExactSearch;
import com.ohinteractive.seedv6.search.manage.*;
import static com.ohinteractive.seedv6.gui.ConventionalGuiProbe.*;

/** Headless EDT/Play lifecycle probe compiled separately against actual GUI distribution jars. */
public final class HlG1ProductionProbe {
    public static void main(String[] args) throws Exception {
        long flags = switch (BaselineEngine.SearchProfile.configured()) {
            case TC_P1 -> 1177812991; case HL_G1 -> 104071167; case LEGACY -> 3407871;
        };
        var adapter = edt(() -> new EngineSearchAdapter(1));
        int requests = 0;
        try {
            require(bind(adapter, PlayEvaluator.Mode.CONVENTIONAL_CP64, PlayParticipants.Selection.BEST) == null, "CP64 binding");
            for (int workers : new int[]{1, 4, 8, 16}) {
                edt(() -> { adapter.replaceWorkerCount(workers); return null; });
                require(adapter.workerCount() == workers, "worker setting");
                require(adapter.evaluator().identity(workers).contains("flags=" + flags + " "), "effective profile");
                System.out.println(adapter.evaluator().identity(workers));
                for (int cycle = 0; cycle < 2; cycle++) {
                    edt(() -> { adapter.invalidate(SearchTermination.NEW_GAME); return null; });
                    var ordinary = run(adapter, GameSession.startingPosition(), new SearchLimits(5, -1, -1, false), false);
                    require(ordinary.termination() == SearchTermination.COMPLETED && ordinary.lastCompletedResult().depth() == 5, "depth completion");
                    require(Math.abs(ordinary.lastCompletedResult().score()) < ExactSearch.MATE_SCORE - 1000, "ordinary score domain");
                    long start = System.nanoTime();
                    var timed = run(adapter, GameSession.startingPosition(), new SearchLimits(100, -1, 150, false), false);
                    require(timed.termination() == SearchTermination.TIME_LIMIT, "movetime reason");
                    require((System.nanoTime() - start) / 1_000_000 < 5000, "movetime drain bound");
                    require(timed.lastCompletedResult() == null || timed.lastCompletedResult().depth() < 100, "truthful timed depth");
                    var stopped = run(adapter, GameSession.startingPosition(), new SearchLimits(100, -1, -1, false), true);
                    require(stopped.termination() == SearchTermination.STOPPED, "stop reason");
                    requests += 3;
                }
                String[] fens = {"7k/6Q1/5K2/8/8/8/8/8 b - - 100 1", "7k/5Q2/6K1/8/8/8/8/8 b - - 100 1",
                        "7k/8/5KQ1/8/8/8/8/8 w - - 0 1"};
                int[] scores = {-ExactSearch.MATE_SCORE, 0, ExactSearch.MATE_SCORE - 1};
                for (int i = 0; i < fens.length; i++) {
                    var terminal = run(adapter, GameSession.fromFen(fens[i]), new SearchLimits(4, -1, -1, false), false);
                    require(terminal.lastCompletedResult().score() == scores[i], "mate/stalemate score");
                    requests++;
                }
                var repetition = GameSession.startingPosition();
                for (int cycle = 0; cycle < 2; cycle++) {
                    for (String coordinate : List.of("g1f3", "g8f6", "f3g1", "f6g8"))
                        repetition.applyGeneratedMove(Arrays.stream(repetition.legalMoves())
                                .filter(m -> Move.coordinate(m).equals(coordinate)).findFirst().orElseThrow());
                    var result = run(adapter, repetition, new SearchLimits(3, -1, -1, false), false);
                    require(result.lastCompletedResult() != null, "history result");
                    if (cycle == 0) require(result.lastCompletedResult().hasMove(), "twofold is not a terminal repetition");
                    else require(result.lastCompletedResult().score() == 0 && !result.lastCompletedResult().hasMove(), "real threefold");
                    requests++;
                }
            }
        } finally { edt(adapter::beginShutdown).run(); }
        for (String name : List.of("service", "blackService")) require(service(adapter, name).isTerminated(), "shutdown drain");
        System.out.println("PASS GUI Play flags=" + flags + " requests=" + requests + " workers=1,4,8,16 depth/movetime/stop/history/mate/PV/restore/reuse/drain");
    }
    private static SearchLifecycleService service(EngineSearchAdapter adapter, String name) throws Exception {
        var f = EngineSearchAdapter.class.getDeclaredField(name); f.setAccessible(true); return (SearchLifecycleService) f.get(adapter);
    }
    private static ManagedSearchResult run(EngineSearchAdapter adapter, GameSession session, SearchLimits limits, boolean stop) throws Exception {
        for (String name : List.of("service", "blackService")) service(adapter, name).setBookEnabled(false);
        var board = session.boardSnapshot(); var before = board.clone(); var history = session.historySnapshot();
        var result = new CompletableFuture<ManagedSearchResult>();
        edt(() -> {
            adapter.start(board, history, limits, new Object(), new SearchGateway.Listener() {
                public void onIteration(Object token, IterationSnapshot iteration) { }
                public void onComplete(Object token, ManagedSearchResult value) { result.complete(value); }
            });
            if (stop) adapter.stop();
            return null;
        });
        var done = result.get(20, TimeUnit.SECONDS);
        require(done.failure() == null, "search failure: " + done.failure());
        require(Arrays.equals(before, board), "board restoration"); history.requireCurrent(board);
        require(!edt(adapter::isBusy) && edt(adapter::activeSearchThreads) == 0, "completion before drain");
        if (done.hasMove()) require(Arrays.stream(session.legalMoves()).anyMatch(m -> m == done.bestMove()), "legal move");
        if (done.lastCompletedResult() != null) {
            require(done.lastCompletedResult().completed(), "incomplete publication");
            var replay = GameSession.fromFen(com.ohinteractive.seedv6.core.util.Fen.fromBoard(board));
            for (long move : done.lastCompletedResult().principalVariation()) replay.applyGeneratedMove(move);
        }
        System.out.println("request termination=" + done.termination() + " nodes=" + done.nodes()
                + " depth=" + (done.lastCompletedResult() == null ? 0 : done.lastCompletedResult().depth()));
        return done;
    }
    private HlG1ProductionProbe() { }
}
