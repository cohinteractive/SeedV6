import java.nio.file.*;
import java.util.*;
import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.exact.*;

/** Bounded read-only score/transition probe. Input TSV: name, FEN, optional independent expected CP. */
public final class BaselineCalibrationProbe {
    public static void main(String[] args) throws Exception {
        var nnue = BaselineEngine.load();
        var model = nnue.conventional().model();
        var evaluations = new SearchEvaluation[]{BaselineEngine.load(BaselineEngine.Evaluator.HCE).evaluation(),
                BaselineEngine.load(BaselineEngine.Evaluator.BREP2_32).evaluation(), nnue.evaluation()};
        var states = Arrays.stream(evaluations).map(e -> e.newState(3)).toList();
        System.out.println("name\thead\thce\tbrep2\tnnueCp\tconstructionDepth4\twithoutMarginsDepth4\tchildrenChecked\theadCrossings\tkingContextCrossings\tmaximumHeadDelta\tfen");
        for (String line : Files.readAllLines(Path.of(args[0]))) {
            if (line.isBlank() || line.startsWith("#")) continue;
            var fields = line.split("\t"); var board = Board.fromFen(fields[1]);
            int[] scores = new int[3];
            for (int i = 0; i < 3; i++) { states.get(i).initialize(board, 0); scores[i] = states.get(i).evaluate(board, 0); }
            if (scores[2] != model.fullRebuild(board)) throw new AssertionError("Rebuild: " + line);
            if (fields.length > 2 && scores[2] != Integer.parseInt(fields[2])) throw new AssertionError("Independent parity: " + line);
            long[] legal = new long[256];
            int count = Gen.genAll(board[0], board[1], board[2], board[3], (int) board[4], board[5], true, legal, new long[6]);
            int checked = 0, headCrossings = 0, kingCrossings = 0, maximumHeadDelta = 0;
            for (int index = 0; index < count; index++) {
                long move = legal[index];
                // Board.makeMove is the same legal transition used by search; rebuild is independent of deltas.
                var child = board.clone();
                Board.makeMoveInto(board[0], board[1], board[2], board[3], (int) board[4], board[5], move, child);
                states.get(2).child(board, child, 0);
                if (states.get(2).evaluate(child, 1) != model.fullRebuild(child)) throw new AssertionError("Child parity: " + line);
                if (states.get(2).evaluate(board, 0) != scores[2]) throw new AssertionError("Parent changed");
                if (head(board) != head(child)) {
                    headCrossings++;
                    maximumHeadDelta = Math.max(maximumHeadDelta, Math.abs(-states.get(2).evaluate(child, 1) - scores[2]));
                }
                if (context(board, 0) != context(child, 0) || context(board, 1) != context(child, 1)) kingCrossings++;
                checked++;
            }
            String full = "", control = "";
            if (fields[0].startsWith("case")) {
                full = Integer.toString(search(nnue.evaluation(), board, FlatSearchConfig.candidate()));
                control = Integer.toString(search(nnue.evaluation(), board, FlatSearchConfig.candidate()
                        .with(FlatSearchConfig.Feature.FUTILITY, false).with(FlatSearchConfig.Feature.REVERSE_FUTILITY, false)));
            }
            System.out.println(fields[0] + "\t" + ((Long.bitCount(board[0] | board[1] | board[2]) - 2) / 4)
                    + "\t" + scores[0] + "\t" + scores[1] + "\t" + scores[2] + "\t" + full + "\t" + control + "\t" + checked
                    + "\t" + headCrossings + "\t" + kingCrossings + "\t" + maximumHeadDelta + "\t" + fields[1]);
        }
    }
    private static int head(long[] board) { return (Long.bitCount(board[0] | board[1] | board[2]) - 2) / 4; }
    private static int context(long[] board, int side) {
        long kings = board[0] & ~board[1] & ~board[2];
        int square = Long.numberOfTrailingZeros(kings & (side == 0 ? ~board[3] : board[3]));
        return com.ohinteractive.seedv6.core.nnue.conventional.ConventionalFeatures.context(side, square);
    }
    private static int search(SearchEvaluation evaluation, long[] board, FlatSearchConfig config) throws Exception {
        try (var search = new ConstructionSearch(config, 1, 8, AspirationSearch.Policy.WIDEN16).create(evaluation)) {
            search.beginRequest();
            try {
                SearchResult result = null;
                for (int depth = 1; depth <= 4; depth++) result = search.search(new SearchRequest(board,
                        GameHistory.initial(board), depth, SearchObserver.NONE, SearchControl.unlimited(), true));
                if (!result.completed()) throw new AssertionError("Incomplete calibration search");
                return result.score();
            } finally { search.endRequest(); }
        }
    }
}
