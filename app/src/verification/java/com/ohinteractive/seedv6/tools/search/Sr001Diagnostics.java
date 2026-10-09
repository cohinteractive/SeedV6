package com.ohinteractive.seedv6.tools.search;

import java.nio.file.*;
import java.io.PrintWriter;
import java.util.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;

/** Explicit frozen-model, whole-request SR-001 diagnostics; never a game/strength runner. */
public final class Sr001Diagnostics {
    static final String CHECKPOINT = "g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304";
    static final String HASH = "5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7";
    static final String[] NAMES = {"static", "unpruned", "see-promo", "see-check", "q4", "recapture", "see-initial-checks", "check-only", "q2"};
    static final int[] POLICIES = {-1, 0, 2, 3, 4, 64, 65, 66, 67};
    static final String[][] POSITIONS = {
        {"start", "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"},
        {"kiwipete", "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1"},
        {"endgame", "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1"},
        {"exchange", "4k3/8/2p1p3/3q4/2P1P3/3Q4/8/4K3 w - - 0 1"},
        {"promotion", "8/k1P5/2K5/8/8/8/8/8 w - - 0 1"},
        {"evasion", "4r1k1/8/8/8/8/8/8/4K3 w - - 0 1"},
        {"clearance", "b1q4k/8/8/2p5/N7/8/8/R3K3 w - - 0 1"}
    };
    public static void main(String[] args) throws Exception {
        if(args.length != 2) throw new IllegalArgumentException("store new-output.csv");
        var snapshot = CheckpointStore.readSnapshot(Path.of(args[0]), CHECKPOINT);
        if(!snapshot.manifest().networkSha256().equals(HASH)) throw new IllegalArgumentException("Frozen network mismatch");
        var evaluation = snapshot.model().evaluation(NnueScoreMapping.V1);
        var drivers = new SearchDriver[POLICIES.length];
        try {
            for(int i = 0; i < drivers.length; i++) {
                drivers[i] = new SearchDriver(POLICIES[i] < 0 ? ProductionSearch.createStaticControl(1, evaluation)
                        : ExactSearchAdapter.quiescenceResearch(evaluation, POLICIES[i]));
                for(int warm = 0; warm < 3; warm++) {
                    drivers[i].newGame();
                    drivers[i].search(new SearchRequest(Board.startingPosition(), 4));
                }
            }
            try(var out = new PrintWriter(Files.newBufferedWriter(Path.of(args[1]), StandardOpenOption.CREATE_NEW))) {
                out.println("position,budget,repetition,policy,completedDepth,attemptedDepth,targetComplete,wholeNodes,mainNodes,qNodes,evaluations,maxPly,maxQply,elapsedNanos,score,best,pv,termination");
                for(int rep = 0; rep < 5; rep++) for(var position : POSITIONS) for(int budget : new int[] {3, 4, 20000}) {
                    for(int offset = 0; offset < drivers.length; offset++) {
                        int i = (offset + rep) % drivers.length;
                        var board = Board.fromFen(position[1]);
                        var original = board.clone();
                        drivers[i].newGame();
                        long start = System.nanoTime();
                        var control = SearchControl.controlled(budget == 20000 ? budget : 1_000_000,
                                start, -1, TimeSource.SYSTEM);
                        var r = drivers[i].search(new SearchRequest(board, GameHistory.initial(board),
                                budget == 20000 ? 64 : budget, SearchObserver.NONE, control, true));
                        long elapsed = System.nanoTime() - start;
                        var last = r.lastCompletedResult(); var nodes = r.diagnostics().worker().nodes();
                        if(!Arrays.equals(board, original) || r.nodes() != nodes.totalEnteredNodes())
                            throw new AssertionError("State/accounting mismatch");
                        var line = new StringJoiner(" ");
                        if(last != null) for(long move : last.principalVariation()) line.add(Move.coordinate(move));
                        out.println(String.join(",", position[0], budget == 20000 ? "nodes20000" : "depth"+budget,
                                ""+rep, NAMES[i], ""+(last == null ? 0 : last.depth()), ""+r.attemptedDepth(),
                                ""+r.targetDepthCompleted(), ""+r.nodes(), ""+nodes.mainNodes(), ""+nodes.qNodes(),
                                ""+nodes.evaluationCalls(), ""+nodes.maximumAbsolutePly(), ""+nodes.maximumQply(),
                                ""+elapsed, ""+(last == null ? "" : last.score()),
                                last != null && last.hasMove() ? Move.coordinate(last.bestMove()) : "", line.toString(),
                                control.termination().toString()));
                        out.flush();
                    }
                }
            }
        } finally { for(var driver : drivers) if(driver != null) driver.close(); }
    }
}
