package com.ohinteractive.seedv6.search.exact;

import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;

/** Clean, warmed whole-request cost comparison. No omission observer in timed searches.
 * Thread allocation includes request/driver result work, excludes engine construction.
 * Compilation/GC are process-wide indicators, not causal per-policy attribution. */
public final class Sr022Cost {
    public static void main(String[] args) throws Exception {
        if(args.length != 4) throw new IllegalArgumentException("store corpus.tsv new-output.jsonl POLICY");
        var snapshot = CheckpointStore.readSnapshot(Path.of(args[0]),
                "g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304");
        if(!snapshot.manifest().networkSha256().equals("5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7"))
            throw new AssertionError("Wrong network");
        var definition = snapshot.model().evaluation(NnueScoreMapping.V1);
        var allocation = (com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        if(!allocation.isThreadAllocatedMemorySupported()) throw new AssertionError("Allocation unavailable");
        allocation.setThreadAllocatedMemoryEnabled(true);
        long thread = Thread.currentThread().threadId();
        var gson = new GsonBuilder().create();
        try(var off = new SearchDriver(ExactSearchAdapter.quietLateMoveReductions(definition));
                var quiet = new SearchDriver(ExactSearchAdapter.lateMovePruningResearch(definition, Integer.parseInt(args[3]), null));
                var output = Files.newBufferedWriter(Path.of(args[2]), StandardOpenOption.CREATE_NEW)) {
            SearchDriver[] drivers = {off, quiet};
            for(var driver : drivers) for(int i=0; i<3; i++) {
                driver.newGame(); long start = System.nanoTime();
                var board = Board.startingPosition();
                driver.search(new SearchRequest(board, GameHistory.initial(board), 20, SearchObserver.NONE,
                        SearchControl.controlled(100_000, start, 15_000_000_000L, TimeSource.SYSTEM), true));
            }
            int observations = 0;
            for(int round=0; round<2; round++) {
                int rootIndex = 0;
                for(String line : Files.readAllLines(Path.of(args[1]))) {
                    if(line.isBlank() || line.startsWith("#")) continue;
                    var fields = line.split("\t", -1);
                    var board = Board.fromFen(fields[1]); var builder = GameHistory.builder(board);
                    if(fields.length > 2 && !fields[2].isBlank()) for(String uci : fields[2].split(" ")) {
                        long move = Arrays.stream(ExhaustiveOracle.legalMoves(board))
                                .filter(m -> Move.coordinate(m).equals(uci)).findFirst().orElseThrow();
                        board = ExhaustiveOracle.child(board, move); builder.appendPosition(board);
                    }
                    var history = builder.snapshot();
                    for(long budget : new long[] {0, 100_000, 1_000_000}) for(int offset=0; offset<2; offset++) {
                        int policyIndex = (rootIndex + round + offset) % 2;
                        var driver = drivers[policyIndex]; driver.newGame();
                        long compiled = ManagementFactory.getCompilationMXBean().getTotalCompilationTime();
                        long collections = gcCount();
                        long allocated = allocation.getThreadAllocatedBytes(thread); long start = System.nanoTime();
                        var control = SearchControl.controlled(budget == 0 ? 5_000_000 : budget, start, 15_000_000_000L, TimeSource.SYSTEM);
                        var result = driver.search(new SearchRequest(board, history, budget == 0 ? 6 : 20, SearchObserver.NONE, control, true));
                        long elapsed = System.nanoTime() - start;
                        long bytes = allocation.getThreadAllocatedBytes(thread) - allocated;
                        if(result.nodes() != result.diagnostics().totalEnteredNodes()) throw new AssertionError("Accounting mismatch");
                        if(control.termination() == SearchTermination.TIME_LIMIT || (budget != 0 && result.nodes() > budget))
                            throw new AssertionError("Timed out or exceeded node budget");
                        Phase2QuiescenceTest.legalPv(board, result.lastCompletedResult().principalVariation());
                        var row = new LinkedHashMap<String,Object>();
                        row.put("id", fields[0]); row.put("round", round); row.put("policy", policyIndex == 0 ? "off" : args[3]);
                        row.put("budget", budget); row.put("elapsedNanos", elapsed); row.put("threadAllocatedBytes", bytes);
                        row.put("processCompilationMillis", ManagementFactory.getCompilationMXBean().getTotalCompilationTime()-compiled);
                        row.put("processGcCollections", gcCount()-collections); row.put("outcome", result);
                        row.put("termination", control.termination());
                        output.write(gson.toJson(row)); output.newLine(); output.flush(); observations++;
                    }
                    rootIndex++;
                }
                System.out.println("completed clean cost round " + round);
            }
            output.write(gson.toJson(Map.of("status", "passed", "observations", observations))); output.newLine();
        }
    }

    private static long gcCount() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(b -> Math.max(0, b.getCollectionCount())).sum();
    }
}
