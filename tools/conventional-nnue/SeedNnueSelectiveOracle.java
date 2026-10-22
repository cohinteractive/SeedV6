import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.conventional.ConventionalNetwork;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;

/** Bounded independent-inference check of the separately frozen research composition. */
public final class SeedNnueSelectiveOracle {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("NETWORK EXISTING_ROOTS_FENS");
        if (System.getenv("SEED_NNUE_OPERATION") == null)
            throw new IllegalStateException("Use nnue-control.cmd run");
        var net = ConventionalNetwork.load(Path.of(args[0]));
        var roots = new ArrayList<>(Files.readAllLines(Path.of(args[1])));
        roots.addAll(Arrays.asList(
            Board.FEN_STARTING_POSITION,
            "r3k2r/pppq1ppp/2n1bn2/3pp3/3PP3/2N1BN2/PPPQ1PPP/R3K2R w KQkq - 0 1",
            "4k3/8/8/3n4/4P3/8/8/4K3 w - - 0 1",
            "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1",
            "4k2r/8/8/3pP3/8/8/8/R3K3 w - d6 0 1",
            "7k/P7/8/8/8/8/8/7K w - - 0 1"));
        for (String fen : roots) {
            if (fen.isBlank()) continue;
            var board = Board.fromFen(fen);
            var request = new SearchRequest(board, GameHistory.initial(board), 5,
                SearchObserver.NONE, SearchControl.unlimited(), true);
            long start = System.nanoTime();
            try (var fast = SeedNnueSelectiveResearch.search(SearchEvaluation.conventional(net), 4);
                 var reference = SeedNnueSelectiveResearch.search(SearchEvaluation.conventionalFullRebuild(net), 4)) {
                var a = fast.search(request); var b = reference.search(request);
                long aq = a.diagnostics().worker().nodes().qNodes();
                long bq = b.diagnostics().worker().nodes().qNodes();
                if (!a.completed() || !b.completed() || a.depth() != 5 || b.depth() != 5
                    || a.score() != b.score() || a.bestMove() != b.bestMove()
                    || a.nodes() != b.nodes() || aq != bq || a.selective() != b.selective()
                    || !Arrays.equals(a.principalVariation(), b.principalVariation()))
                    throw new AssertionError("Independent inference/search mismatch: " + fen);
                System.out.printf("{\"status\":\"PASS\",\"depth\":5,\"score\":%d,\"nodes\":%d,\"qnodes\":%d,\"selective\":%s,\"elapsed_ns\":%d,\"fen\":\"%s\"}%n",
                    a.score(), a.nodes(), aq, a.selective(), System.nanoTime()-start, fen);
            }
        }
    }
}
