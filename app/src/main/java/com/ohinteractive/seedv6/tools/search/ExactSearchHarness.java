package com.ohinteractive.seedv6.tools.search;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.exact.ExactSearch;
import com.ohinteractive.seedv6.search.exact.ExactSearchResult;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;
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

    static void run(String[] args, PrintStream out) {
        int depth = 3;
        int warmups = 3;
        int repetitions = 5;
        String names = "start,kiwipete,endgame";
        String fen = null;
        boolean named = false;
        boolean tt = false;
        for(String arg : args) {
            if(arg.equals("--help")) {
                out.println("--position=start,kiwipete,endgame|all --fen=<six-field FEN> --depth=0..256 --warmups=3 --repetitions=5 --tt=off|on");
                return;
            }
            if(arg.startsWith("--depth=")) depth = Integer.parseInt(arg.substring(8));
            else if(arg.startsWith("--warmups=")) warmups = Integer.parseInt(arg.substring(10));
            else if(arg.startsWith("--repetitions=")) repetitions = Integer.parseInt(arg.substring(14));
            else if(arg.startsWith("--position=")) { names = arg.substring(11); named = true; }
            else if(arg.startsWith("--fen=")) fen = arg.substring(6);
            else if(arg.equals("--tt=on")) tt = true;
            else if(arg.equals("--tt=off")) tt = false;
            else throw new IllegalArgumentException("Unknown argument: " + arg);
        }
        if(depth < 0 || depth > ExactSearch.MAX_DEPTH || warmups < 0 || repetitions < 1) {
            throw new IllegalArgumentException("Invalid depth/warmup/repetition count.");
        }
        if(fen != null && named) throw new IllegalArgumentException("Select either FEN or named positions.");
        List<Position> selected = new ArrayList<>();
        if(fen != null) selected.add(new Position("fen", fen));
        else if(names.equals("all")) selected.addAll(positions());
        else for(String name : names.split(",", -1)) {
            Position found = null;
            for(Position p : positions()) if(p.name().equals(name)) found = p;
            if(found == null) throw new IllegalArgumentException("Unknown position: " + name);
            selected.add(found);
        }
        out.printf(Locale.ROOT, "exact-search evaluator=HCE threads=1 java=%s vm=%s os=%s/%s depth=%d warmups=%d repetitions=%d%n",
                System.getProperty("java.version"), System.getProperty("java.vm.name"),
                System.getProperty("os.name"), System.getProperty("os.arch"), depth, warmups, repetitions);
        out.println("Time is search wall time (setup included, worker construction/FEN parsing excluded); upper median measured sample.");
        out.printf("tt=%s table=%s%n", tt ? "on" : "off", tt ? "cold/cleared before each request; explicit harness 4 MiB" : "none");
        for(Position position : selected) {
            long[] board = Board.fromFen(position.fen());
            GameHistory history = GameHistory.initial(board);
            TTable table = tt ? new TTable(4) : null;
            ExactSearch search = new ExactSearch(SearchEvaluation.handcrafted(), table);
            ExactSearchResult expected = null;
            for(int i = 0; i < warmups; i++) {
                if(table != null) table.clear();
                ExactSearchResult result = search.search(board, history, depth, ExactSearch.NEVER_CANCELLED);
                requireRepeatable(expected, result);
                expected = result;
            }
            ExactSearchResult[] measured = new ExactSearchResult[repetitions];
            for(int i = 0; i < repetitions; i++) {
                if(table != null) table.clear();
                measured[i] = search.search(board, history, depth, ExactSearch.NEVER_CANCELLED);
                requireRepeatable(expected, measured[i]);
                expected = measured[i];
            }
            Arrays.sort(measured, Comparator.comparingLong(ExactSearchResult::elapsedNanos));
            ExactSearchResult median = measured[repetitions / 2];
            out.printf(Locale.ROOT, "position=%s requested=%d completed=%d best=%s score=%d pv=[%s] nodes=%d median_ms=%.3f nps=%d%n",
                    position.name(), median.requestedDepth(), median.completedDepth(),
                    median.hasMove() ? Move.coordinate(median.bestMove()) : "none", median.score(),
                    pvText(median.principalVariation()), median.nodes(), median.elapsedNanos() / 1_000_000.0, median.nps());
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
