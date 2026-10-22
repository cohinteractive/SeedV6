package com.ohinteractive.seedv6.gui;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.manage.*;

/** Also runnable against relocated distribution jars, without JUnit or a checkout. */
public final class ConventionalGuiProbe {
    static <T> T edt(Callable<T> call) throws Exception {
        var result = new AtomicReference<T>(); var failure = new AtomicReference<Throwable>();
        SwingUtilities.invokeAndWait(() -> { try { result.set(call.call()); } catch (Throwable t) { failure.set(t); } });
        if (failure.get() != null) throw new AssertionError(failure.get());
        return result.get();
    }
    static String bind(EngineSearchAdapter adapter, PlayEvaluator.Mode mode, PlayParticipants.Selection selection) throws Exception {
        var loaded = new CompletableFuture<String>();
        edt(() -> { adapter.changeParticipants(mode, null, selection, loaded::complete); return null; });
        return loaded.get(20, TimeUnit.SECONDS);
    }
    static ManagedSearchResult search(EngineSearchAdapter adapter, long[] board, SearchLimits limits, boolean stop) throws Exception {
        var result = new CompletableFuture<ManagedSearchResult>();
        // Parity probes must exercise search, not the GUI's optional opening repertoire.
        for (String name : new String[]{"service", "blackService"}) {
            var field = EngineSearchAdapter.class.getDeclaredField(name); field.setAccessible(true);
            ((SearchLifecycleService) field.get(adapter)).setBookEnabled(false);
        }
        edt(() -> {
            adapter.start(board, GameHistory.initial(board), limits, new Object(), new SearchGateway.Listener() {
                public void onIteration(Object token, IterationSnapshot iteration) {}
                public void onComplete(Object token, ManagedSearchResult value) { result.complete(value); }
            });
            if (stop) adapter.stop();
            return null;
        });
        return result.get(20, TimeUnit.SECONDS);
    }
    static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        var adapter = edt(() -> new EngineSearchAdapter(1));
        try {
            String error = bind(adapter, PlayEvaluator.Mode.CONVENTIONAL_CP64, PlayParticipants.Selection.BEST);
            if (args.length > 0 && args[0].equals("failure")) {
                require(error != null && error.contains("checkpoint-64 NNUE"), "Expected clear network failure: " + error);
                require(adapter.evaluator().mode() == PlayEvaluator.Mode.HANDCRAFTED, "Failed selection replaced the old engine");
                System.out.println(error);
                require(bind(adapter, PlayEvaluator.Mode.POLICY4_PAIR2, PlayParticipants.Selection.BEST) == null, "Other engine unavailable");
            } else {
                require(error == null, error);
                System.out.println(adapter.evaluator().identity(adapter.workerCount()));
            }
            String fen = args.length > 0 && !args[0].equals("failure") ? args[0] : Board.FEN_STARTING_POSITION;
            var board = Board.fromFen(fen);
            var result = search(adapter, board, new SearchLimits(5, -1, -1, false), false);
            require(result.failure() == null && result.hasMove(), "Search failed: " + result.failure());
            var session = GameSession.fromFen(fen);
            require(java.util.Arrays.stream(session.legalMoves()).anyMatch(m -> m == result.bestMove()), "Illegal move");
            var value = result.lastCompletedResult();
            System.out.println("RESULT score=" + value.score() + " nodes=" + value.nodes()
                    + " move=" + com.ohinteractive.seedv6.core.move.Move.coordinate(result.bestMove())
                    + " pv=" + String.join(" ", java.util.Arrays.stream(value.principalVariation())
                            .mapToObj(com.ohinteractive.seedv6.core.move.Move::coordinate).toList()));
        } finally { edt(adapter::beginShutdown).run(); }
    }
    private ConventionalGuiProbe() {}
}
