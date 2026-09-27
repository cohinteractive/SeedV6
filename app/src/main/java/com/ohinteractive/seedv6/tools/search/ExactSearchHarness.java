package com.ohinteractive.seedv6.tools.search;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.exact.ExactSearch;
import com.ohinteractive.seedv6.search.exact.ExactEvaluator;
import com.ohinteractive.seedv6.search.exact.ExactSearchResult;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;
import com.ohinteractive.seedv6.tools.perft.DefaultPerftPositionLibrary;
import com.ohinteractive.seedv6.tools.perft.PerftPosition;

/** Dedicated fixed-depth HCE baseline; does not invoke production Search or perft counting. */
public final class ExactSearchHarness {
    public record Position(String name, String fen) {}

    public static List<Position> positions() {
        List<Position> positions = new ArrayList<>();
        for(PerftPosition p : new DefaultPerftPositionLibrary().positions()) {
            switch(p.id()) {
                case "initial-position" -> positions.add(new Position("start", p.fen()));
                case "kiwipete" -> positions.add(new Position("kiwipete", p.fen()));
                case "rook-and-pawn-endgame" -> positions.add(new Position("endgame", p.fen()));
                default -> { }
            }
        }
        positions.add(new Position("mate", "7k/8/5KQ1/8/8/8/8/8 w - - 0 1"));
        positions.add(new Position("checkmate", "7k/6Q1/5K2/8/8/8/8/8 b - - 0 1"));
        positions.add(new Position("stalemate", "7k/5K2/6Q1/8/8/8/8/8 b - - 0 1"));
        return List.copyOf(positions);
    }

    public static void main(String[] args) { run(args, System.out); }

    /** Fixed SR-014/015 comparison set; leaves the established six-position oracle set intact. */
    public static List<Position> orderingPositions() {
        List<Position> selected = new ArrayList<>();
        selected.add(positions().get(0)); // Quiet opening.
        selected.add(positions().get(1)); // High branching: Kiwipete.
        selected.add(new Position("tactical", "4k3/8/2p1p3/3q4/2P1P3/3Q4/8/4K3 w - - 0 1"));
        for(PerftPosition p : new DefaultPerftPositionLibrary().positions()) {
            if(p.id().equals("promotion-and-castling-tactics")) selected.add(new Position("evasion", p.fen()));
            if(p.id().equals("complex-middlegame")) selected.add(new Position("middlegame", p.fen()));
        }
        selected.add(new Position("transposition-pawns", "4k3/pp6/8/8/8/8/PP6/4K3 w - - 0 1"));
        return List.copyOf(selected);
    }

    static void run(String[] args, PrintStream out) {
        int depth = 3;
        int warmups = 3;
        int repetitions = 5;
        String names = "start,kiwipete,endgame";
        String fen = null;
        boolean named = false;
        boolean tt = false;
        int[] orderings = {ExactSearch.CONTROL};
        for(String arg : args) {
            if(arg.equals("--help")) {
                out.println("--position=start,kiwipete,endgame|all|ordering --fen=<six-field FEN> --depth=0..256 --warmups=3 --repetitions=5 --tt=off|on --ordering=control|see-tiered|see-tactical|both|control,see-tactical");
                out.println("both retains CONTROL versus SEE_TIERED; control,see-tactical compares CONTROL versus SEE_TACTICAL.");
                out.println("Also: --ordering=see-material|see-material-lva|see-tactical,see-material,see-material-lva");
                out.println("Capture history: --ordering=see-material-history|see-material,see-material-history");
                out.println("Main quiet history: --ordering=see-material-quiet-history|see-material,see-material-quiet-history");
                return;
            }
            if(arg.startsWith("--depth=")) depth = Integer.parseInt(arg.substring(8));
            else if(arg.startsWith("--warmups=")) warmups = Integer.parseInt(arg.substring(10));
            else if(arg.startsWith("--repetitions=")) repetitions = Integer.parseInt(arg.substring(14));
            else if(arg.startsWith("--position=")) { names = arg.substring(11); named = true; }
            else if(arg.startsWith("--fen=")) fen = arg.substring(6);
            else if(arg.equals("--tt=on")) tt = true;
            else if(arg.equals("--tt=off")) tt = false;
            else if(arg.equals("--ordering=control")) orderings = new int[] {ExactSearch.CONTROL};
            else if(arg.equals("--ordering=see-tiered")) orderings = new int[] {ExactSearch.SEE_TIERED};
            else if(arg.equals("--ordering=see-tactical")) orderings = new int[] {ExactSearch.SEE_TACTICAL};
            else if(arg.equals("--ordering=see-material")) orderings = new int[] {ExactSearch.SEE_MATERIAL};
            else if(arg.equals("--ordering=see-material-lva")) orderings = new int[] {ExactSearch.SEE_MATERIAL_LVA};
            else if(arg.equals("--ordering=see-material-history")) orderings = new int[] {ExactSearch.SEE_MATERIAL_CAPTURE_HISTORY};
            else if(arg.equals("--ordering=see-material-quiet-history")) orderings = new int[] {ExactSearch.SEE_MATERIAL_QUIET_HISTORY};
            else if(arg.equals("--ordering=see-material,see-material-quiet-history"))
                orderings = new int[] {ExactSearch.SEE_MATERIAL, ExactSearch.SEE_MATERIAL_QUIET_HISTORY};
            else if(arg.equals("--ordering=see-material,see-material-history"))
                orderings = new int[] {ExactSearch.SEE_MATERIAL, ExactSearch.SEE_MATERIAL_CAPTURE_HISTORY};
            else if(arg.equals("--ordering=both")) orderings = new int[] {ExactSearch.CONTROL, ExactSearch.SEE_TIERED};
            else if(arg.equals("--ordering=control,see-tactical")) orderings = new int[] {ExactSearch.CONTROL, ExactSearch.SEE_TACTICAL};
            else if(arg.equals("--ordering=see-tactical,see-material,see-material-lva"))
                orderings = new int[] {ExactSearch.SEE_TACTICAL, ExactSearch.SEE_MATERIAL, ExactSearch.SEE_MATERIAL_LVA};
            else throw new IllegalArgumentException("Unknown argument: " + arg);
        }
        if(depth < 0 || depth > ExactSearch.MAX_DEPTH || warmups < 0 || repetitions < 1) {
            throw new IllegalArgumentException("Invalid depth/warmup/repetition count.");
        }
        if(fen != null && named) throw new IllegalArgumentException("Select either FEN or named positions.");
        List<Position> selected = new ArrayList<>();
        if(fen != null) selected.add(new Position("fen", fen));
        else if(names.equals("all")) selected.addAll(positions());
        else if(names.equals("ordering")) selected.addAll(orderingPositions());
        else for(String name : names.split(",", -1)) {
            Position found = null;
            for(Position p : positions()) if(p.name().equals(name)) found = p;
            for(Position p : orderingPositions()) if(p.name().equals(name)) found = p;
            if(found == null) throw new IllegalArgumentException("Unknown position: " + name);
            selected.add(found);
        }
        out.printf(Locale.ROOT, "exact-search evaluator=HCE threads=1 java=%s vm=%s os=%s/%s depth=%d warmups=%d repetitions=%d%n",
                System.getProperty("java.version"), System.getProperty("java.vm.name"),
                System.getProperty("os.name"), System.getProperty("os.arch"), depth, warmups, repetitions);
        out.println("Time is search wall time (setup included, worker construction/FEN parsing excluded); upper median measured sample.");
        out.printf("tt=%s table=%s%n", tt ? "on" : "off", tt ? "cold/cleared before each request; explicit harness 4 MiB" : "none");
        if(orderings.length > 1) out.println("Compared modes rotate execution order each warmup/measured round; semantic verification is outside timing.");
        long[] totalNodes = new long[orderings.length];
        long[] totalNanos = new long[orderings.length];
        for(Position position : selected) {
            long[] board = Board.fromFen(position.fen());
            GameHistory history = GameHistory.initial(board);
            TTable[] tables = new TTable[orderings.length];
            ExactSearch[] searches = new ExactSearch[orderings.length];
            for(int mode = 0; mode < orderings.length; mode++) {
                tables[mode] = tt ? new TTable(4) : null;
                searches[mode] = new ExactSearch(ExactEvaluator.from(SearchEvaluation.handcrafted()), tables[mode], orderings[mode]);
            }
            ExactSearchResult[] expected = new ExactSearchResult[orderings.length];
            ExactSearchResult[][] measured = new ExactSearchResult[orderings.length][repetitions];
            for(int round = 0; round < warmups + repetitions; round++) {
                for(int turn = 0; turn < orderings.length; turn++) {
                    int mode = (round + turn) % orderings.length;
                    if(tables[mode] != null) tables[mode].clear();
                    ExactSearchResult result = searches[mode].search(board, history, depth, ExactSearch.NEVER_CANCELLED);
                    requireRepeatable(expected[mode], result);
                    expected[mode] = result;
                    if(round >= warmups) measured[mode][round - warmups] = result;
                }
                for(int mode = 1; mode < orderings.length; mode++) {
                    if(expected[0].score() != expected[mode].score())
                        throw new IllegalStateException("Ordering changed the fixed-depth score at " + position.name());
                }
            }
            ExactSearchResult[] medians = new ExactSearchResult[orderings.length];
            for(int mode = 0; mode < orderings.length; mode++) {
                Arrays.sort(measured[mode], Comparator.comparingLong(ExactSearchResult::elapsedNanos));
                ExactSearchResult median = measured[mode][repetitions / 2];
                medians[mode] = median;
                totalNodes[mode] += median.nodes();
                totalNanos[mode] += median.elapsedNanos();
                out.printf(Locale.ROOT, "position=%s requested=%d completed=%d best=%s score=%d pv=[%s] nodes=%d median_ms=%.3f nps=%d ordering=%s%n",
                    position.name(), median.requestedDepth(), median.completedDepth(),
                    median.hasMove() ? Move.coordinate(median.bestMove()) : "none", median.score(),
                    pvText(median.principalVariation()), median.nodes(), median.elapsedNanos() / 1_000_000.0, median.nps(),
                    orderingName(orderings[mode]));
            }
            if(orderings.length > 1) {
                ExactSearch reference = new ExactSearch();
                for(ExactSearchResult result : medians) verifyBestAndPv(board, history, depth, result, reference);
                for(int mode = 1; mode < orderings.length; mode++)
                    comparison(out, "position=" + position.name() + " depth=" + depth
                            + " baseline=" + orderingName(orderings[0]) + " candidate=" + orderingName(orderings[mode]),
                            medians[0].nodes(), medians[mode].nodes(), medians[0].elapsedNanos(), medians[mode].elapsedNanos());
            }
        }
        for(int mode = 1; mode < orderings.length; mode++)
            comparison(out, "aggregate depth=" + depth + " positions=" + selected.size()
                    + " statistic=sum-of-position-medians baseline=" + orderingName(orderings[0])
                    + " candidate=" + orderingName(orderings[mode]),
                    totalNodes[0], totalNodes[mode], totalNanos[0], totalNanos[mode]);
    }

    private static String orderingName(int ordering) {
        return switch(ordering) {
            case ExactSearch.CONTROL -> "CONTROL";
            case ExactSearch.SEE_TIERED -> "SEE_TIERED";
            case ExactSearch.SEE_TACTICAL -> "SEE_TACTICAL";
            case ExactSearch.SEE_MATERIAL -> "SEE_MATERIAL";
            case ExactSearch.SEE_MATERIAL_LVA -> "SEE_MATERIAL_LVA";
            case ExactSearch.SEE_MATERIAL_CAPTURE_HISTORY -> "SEE_MATERIAL_CAPTURE_HISTORY";
            case ExactSearch.SEE_MATERIAL_QUIET_HISTORY -> "SEE_MATERIAL_QUIET_HISTORY";
            default -> throw new IllegalArgumentException("Unknown ordering mode.");
        };
    }

    private static void comparison(PrintStream out, String label, long controlNodes, long candidateNodes,
                                   long controlNanos, long candidateNanos) {
        out.printf(Locale.ROOT, "comparison %s nodes_pct=%.3f wall_pct=%.3f throughput_pct=%.3f%n", label,
                100.0 * (candidateNodes / (double) controlNodes - 1),
                100.0 * (candidateNanos / (double) controlNanos - 1),
                100.0 * ((candidateNodes / (double) candidateNanos) / (controlNodes / (double) controlNanos) - 1));
    }

    /** Re-search the best child and PV endpoint through TT-off CONTROL, allowing equal-valued ties. */
    private static void verifyBestAndPv(long[] board, GameHistory game, int depth,
                                        ExactSearchResult result, ExactSearch reference) {
        long[] legal = new long[512];
        long[] scratch = new long[Board.MAX_BITBOARDS];
        var history = GameHistory.builder(game);
        int ply = 0;
        long[] pv = result.principalVariation();
        for(long move : pv) {
            int count = Gen.genAll(board[0], board[1], board[2], board[3], (int) board[Board.STATUS],
                    board[Board.KEY], true, legal, scratch);
            boolean found = false;
            for(int i = 0; i < count; i++) if(legal[i] == move) found = true;
            if(!found) throw new IllegalStateException("Illegal PV move.");
            long[] child = new long[Board.MAX_BITBOARDS];
            Board.makeMoveInto(board[0], board[1], board[2], board[3], (int) board[Board.STATUS], board[Board.KEY], move, child);
            board = child;
            history.appendPosition(board);
            ply++;
            if(ply == 1 || ply == pv.length) {
                int score = reference.search(board, history.snapshot(), depth - ply, ExactSearch.NEVER_CANCELLED).score();
                if(score >= TranspositionScores.MATE_THRESHOLD) score -= ply;
                else if(score <= -TranspositionScores.MATE_THRESHOLD) score += ply;
                if((ply % 2 == 0 ? score : -score) != result.score())
                    throw new IllegalStateException("Best move/PV does not realize the exact root value.");
            }
        }
    }

    private static void requireRepeatable(ExactSearchResult expected, ExactSearchResult result) {
        if(!result.completed()) throw new IllegalStateException("Fixed-depth search aborted; no benchmark result.");
        if(expected != null && (expected.bestMove() != result.bestMove() || expected.score() != result.score()
                || expected.nodes() != result.nodes() || !Arrays.equals(expected.principalVariation(), result.principalVariation()))) {
            throw new IllegalStateException("Non-deterministic fixed-depth search.");
        }
    }

    private static String pvText(long[] pv) {
        StringBuilder text = new StringBuilder();
        for(long move : pv) {
            if(!text.isEmpty()) text.append(' ');
            text.append(Move.coordinate(move));
        }
        return text.toString();
    }

    private ExactSearchHarness() {}
}
