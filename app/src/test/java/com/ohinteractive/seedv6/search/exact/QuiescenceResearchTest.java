package com.ohinteractive.seedv6.search.exact;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.See;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(20)
class QuiescenceResearchTest {
    private static final ExactEvaluator HCE = (board, ply) -> Eval.evaluate(board);
    private static final String CAPTURE = "4k3/8/8/3q4/4P3/8/8/4K3 w - - 0 1";
    private static final String EVASION = "4r1k1/8/8/8/8/8/8/4K3 w - - 0 1";
    private static final String EP = "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 2";
    private static final String PROMOTION = "7k/P7/8/8/8/8/8/7K w - - 0 1";
    private static final String CAPTURE_PROMOTION = "1r5k/P7/8/8/8/8/8/7K w - - 0 1";
    private static final String UNDERPROMOTION = "8/k1P5/2K5/8/8/8/8/8 w - - 0 1";
    private static final String MATE_CAPTURE = "7k/6p1/5KQ1/8/8/8/8/8 w - - 0 1";

    @Test void quietLeavesEqualStaticAndDoNotAdmitQuietChecks() {
        for(String fen : new String[] {
                "4k3/8/8/8/8/8/P7/4K3 w - - 0 1",
                "4k3/8/8/8/8/8/P7/4K3 b - - 0 1",
                "7k/8/5KQ1/8/8/8/8/8 w - - 0 1"}) {
            long[] board = Board.fromFen(fen);
            ExactSearchResult q = search(board, 0);
            assertEquals(new ExactSearch().search(board, 0).score(), q.score());
            assertEquals(1, q.nodes());
            assertEquals(1, q.qnodes());
            assertEquals(0, q.normalNodes());
            assertEquals(0, q.maximumQply());
            assertFalse(q.hasMove());
            assertEquals(0, q.principalVariation().length);
        }
    }

    @Test void profitableCaptureExtendsThePvAndChangesTheHorizon() {
        long[] board = Board.fromFen(CAPTURE);
        ExactSearchResult q = compare(board, GameHistory.initial(board), 0, HCE);
        assertTrue(q.score() > Eval.evaluate(board));
        assertEquals("e4d5", Move.coordinate(q.bestMove()));
        assertTrue(q.principalVariation().length > q.requestedDepth());
        assertTrue(q.qnodes() > 1);
        long[] sequence = Board.fromFen("7k/8/1p6/q7/8/Q7/8/R6K w - - 0 1");
        ExactSearchResult exchange = compare(sequence, GameHistory.initial(sequence), 0, (b, p) -> material(b));
        assertTrue(exchange.score() > material(sequence));
        assertEquals(3, exchange.principalVariation().length);
    }

    @Test void negativeSeeCapturesAreVisitedAlthoughStandPatWins() {
        long[] board = Board.fromFen("3rk3/8/8/3p4/8/8/8/3QK3 w - - 0 1");
        long capture = move(board, "d1d5");
        assertFalse(See.atLeastGeneratedLegal(board, capture, 0));
        List<Long> children = new ArrayList<>();
        ExactEvaluator tracking = new ExactEvaluator() {
            @Override public int evaluate(long[] b, int ply) { return Eval.evaluate(b); }
            @Override public void child(long[] parent, long[] child, int parentPly) {
                if(parentPly == 0) children.add(child[Board.KEY]);
            }
        };
        ExactSearchResult q = compare(board, GameHistory.initial(board), 0, tracking);
        assertTrue(children.contains(ExhaustiveOracle.child(board, capture)[Board.KEY]));
        assertEquals(Eval.evaluate(board), q.score());
        assertEquals(0, q.principalVariation().length);
        assertTrue(q.maximumQply() >= 2);
    }

    @Test void enPassantAndEveryPromotionAreAdmitted() {
        for(String fen : new String[] {EP, PROMOTION, CAPTURE_PROMOTION}) {
            long[] board = Board.fromFen(fen);
            List<Long> rootChildren = new ArrayList<>();
            ExactEvaluator tracking = new ExactEvaluator() {
                @Override public int evaluate(long[] b, int ply) { return material(b); }
                @Override public void child(long[] parent, long[] child, int parentPly) {
                    if(parentPly == 0) rootChildren.add(child[Board.KEY]);
                }
            };
            ExactSearchResult q = compare(board, GameHistory.initial(board), 0, tracking);
            long[] admitted = Arrays.stream(ExhaustiveOracle.legalMoves(board))
                    .filter(m -> QuiescenceOracle.tactical(board, m)).toArray();
            assertEquals(admitted.length, rootChildren.size());
            for(long m : admitted) assertTrue(rootChildren.contains(ExhaustiveOracle.child(board, m)[Board.KEY]));
            assertEquals(fen.equals(EP) ? "e5d6" : fen.equals(PROMOTION) ? "a7a8q" : "a7b8q",
                    Move.coordinate(q.bestMove()));
        }
    }

    @Test void rookUnderpromotionAvoidsQueenStalemate() {
        long[] board = Board.fromFen(UNDERPROMOTION);
        long[] queen = ExhaustiveOracle.child(board, move(board, "c7c8q"));
        assertEquals(0, ExhaustiveOracle.legalMoves(queen).length);
        assertFalse(inCheck(queen));
        ExactSearchResult q = compare(board, GameHistory.initial(board), 0, (b, p) -> material(b));
        assertEquals("c7c8r", Move.coordinate(q.bestMove()));
        assertTrue(q.score() > 0);
    }

    @Test void checkedNodesSearchAllQuietEvasionsWithoutStandPat() throws Exception {
        long[] board = Board.fromFen(EVASION);
        assertTrue(inCheck(board));
        long[] legal = ExhaustiveOracle.legalMoves(board);
        assertTrue(legal.length > 0);
        assertTrue(Arrays.stream(legal).noneMatch(m -> QuiescenceOracle.tactical(board, m)));
        List<Long> children = new ArrayList<>();
        ExactEvaluator evaluator = new ExactEvaluator() {
            @Override public int evaluate(long[] b, int ply) {
                assertFalse(inCheck(b), "Stand-pat in check");
                return 37;
            }
            @Override public void child(long[] parent, long[] child, int parentPly) {
                if(parentPly == 0) children.add(child[Board.KEY]);
            }
        };
        ExactSearch search = ExactSearch.quiescenceResearch(evaluator);
        ExactSearchResult q = search.search(board, 0);
        assertEquals(-37, q.score());
        assertEquals(legal.length, children.size());
        for(long m : legal) assertTrue(children.contains(ExhaustiveOracle.child(board, m)[Board.KEY]));
        assertNull(field(search, "quietHistory")); // No normal history can be published by this path.
        assertNull(field(search, "table"));
        assertNull(field(search, "entry"));
        assertNull(field(search, "historyKeys"));
        assertEquals(ExactSearch.ORDERED_ALPHA_BETA, field(search, "traversal"));
        compare(board, GameHistory.initial(board), 0, evaluator);
    }

    @Test void quietBlockingEvasionMayGiveCheckAndResolveAnotherCheckedQnode() {
        long[] board = Board.fromFen("4r3/8/7k/8/8/8/8/2B1K3 w - - 0 1");
        long block = move(board, "c1e3");
        long[] reply = ExhaustiveOracle.child(board, block);
        assertTrue(inCheck(board));
        assertTrue(inCheck(reply));
        assertFalse(QuiescenceOracle.tactical(board, block));
        List<Long> children = new ArrayList<>();
        ExactEvaluator tracking = new ExactEvaluator() {
            @Override public int evaluate(long[] b, int ply) {
                assertFalse(inCheck(b));
                return material(b);
            }
            @Override public void child(long[] parent, long[] child, int parentPly) {
                if(parentPly == 0) children.add(child[Board.KEY]);
            }
        };
        ExactSearchResult q = compare(board, GameHistory.initial(board), 0, tracking);
        assertTrue(children.contains(reply[Board.KEY]));
        assertEquals(ExhaustiveOracle.legalMoves(board).length, children.size());
        assertTrue(q.maximumQply() >= 2);
    }

    @Test void tacticalVisitOrderReusesSeeClassMaterialAndGeneratedTies() {
        long[] board = Board.fromFen("4k3/8/2p1p3/3q4/2P1P3/3Q4/8/4K3 w - - 0 1");
        List<Long> expected = new ArrayList<>();
        for(long m : ExhaustiveOracle.legalMoves(board)) if(QuiescenceOracle.tactical(board, m)) expected.add(m);
        // List.sort is stable; this independent test ranking preserves generated ties.
        expected.sort((a, b) -> {
            int byClass = Boolean.compare(See.atLeastGeneratedLegal(board, b, 0), See.atLeastGeneratedLegal(board, a, 0));
            return byClass != 0 ? byClass : Integer.compare(ExactSearch.tacticalMaterialValue(b, -1), ExactSearch.tacticalMaterialValue(a, -1));
        });
        List<Long> actual = new ArrayList<>();
        ExactSearch q = ExactSearch.quiescenceResearch(new ExactEvaluator() {
            @Override public int evaluate(long[] b, int ply) { return material(b); }
            @Override public void child(long[] parent, long[] child, int parentPly) {
                if(parentPly == 0) actual.add(child[Board.KEY]);
            }
        });
        assertTrue(q.search(board, 0).completed());
        assertEquals(expected.stream().map(m -> ExhaustiveOracle.child(board, m)[Board.KEY]).toList(), actual);
    }

    @Test void terminalAndDrawPrecedenceNeverCallsEvaluator() {
        ExactSearch q = ExactSearch.quiescenceResearch((b, p) -> { throw new AssertionError("Terminal evaluated"); });
        for(int depth : new int[] {0, 1, ExactSearch.MAX_DEPTH}) {
            assertEquals(-32768, q.search(Board.fromFen("7k/6Q1/5K2/8/8/8/8/8 b - - 100 1"), depth).score());
            for(String fen : new String[] {
                    "7k/5K2/6Q1/8/8/8/8/8 b - - 0 1",
                    "4k3/8/8/8/8/8/8/R3K3 w - - 100 1",
                    "4k3/8/8/8/8/8/8/4K3 w - - 0 1"}) {
                ExactSearchResult result = q.search(Board.fromFen(fen), depth);
                assertEquals(0, result.score());
                assertEquals(1, result.nodes());
                assertFalse(result.hasMove());
            }
        }
    }

    @Test void quietEvasionCanCompleteFormalThreefoldUsingRealGameAndQline() {
        long[] board = Board.fromFen("r6k/8/8/8/8/8/8/1K6 b - - 1 1");
        GameHistory.Builder game = GameHistory.builder(board);
        String[] cycle = {"a8b8", "b1a1", "b8a8", "a1b1"};
        for(int i = 0; i < 7; i++) {
            board = ExhaustiveOracle.child(board, move(board, cycle[i % 4]));
            game.appendPosition(board);
        }
        assertTrue(inCheck(board));
        assertEquals(2, game.snapshot().currentOccurrences(board));
        ExactSearchResult q = compare(board, game.snapshot(), 0, (b, p) -> 37);
        assertEquals(0, q.score());
        assertEquals("a1b1", Move.coordinate(q.bestMove()));
        assertTrue(q.maximumQply() > 0);
        // Without the preceding game cycles, the same qline is not a repetition draw.
        assertEquals(-37, ExactSearch.quiescenceResearch((b, p) -> 37).search(board, 0).score());
    }

    @Test void ruleFiftyIsReachedByQuietEvasionAndResetByCaptures() {
        long[] evasion = Board.fromFen(EVASION.replace("0 1", "99 1"));
        ExactSearchResult draw = compare(evasion, GameHistory.initial(evasion), 0, (b, p) -> {
            throw new AssertionError("Checked root or drawn child evaluated");
        });
        assertEquals(0, draw.score());
        assertTrue(draw.hasMove());
        long[] capture = Board.fromFen(CAPTURE.replace("0 1", "99 1"));
        ExactSearchResult q = compare(capture, GameHistory.initial(capture), 0, HCE);
        assertTrue(q.score() > 0);
        assertEquals(0, Board.halfMoveClock((int) ExhaustiveOracle.child(capture, q.bestMove())[Board.STATUS]));
    }

    @Test void mateDistanceUsesAbsolutePlyAndCheckedChildrenResolve() {
        long[] board = Board.fromFen(MATE_CAPTURE);
        for(int depth : new int[] {0, 1, 2}) {
            ExactSearchResult q = compare(board, GameHistory.initial(board), depth, HCE);
            assertEquals(32767, q.score());
            assertEquals("g6g7", Move.coordinate(q.bestMove()));
        }
    }

    @Test void failSoftStandPatAndTacticalCutoffsRespectBounds() {
        long[] board = Board.fromFen(CAPTURE);
        int stand = Eval.evaluate(board);
        ExactSearch search = ExactSearch.quiescenceResearch(HCE);
        GameHistory game = GameHistory.initial(board);
        ExactSearchResult stop = search.searchWindow(board, game, 0, stand - 20, stand - 10, ExactSearch.NEVER_CANCELLED);
        assertEquals(stand, stop.score());
        assertEquals(1, stop.nodes());
        assertFalse(stop.hasMove());
        int exact = new QuiescenceOracle(HCE).score(board, new SearchLineHistory(game), 0, 0);
        ExactSearchResult high = search.searchWindow(board, game, 0, stand, stand + 1, ExactSearch.NEVER_CANCELLED);
        assertTrue(high.score() > stand + 1 && high.score() <= exact);
        assertEquals(1, high.principalVariation().length);
        for(String fen : new String[] {EP, CAPTURE_PROMOTION, EVASION, MATE_CAPTURE}) {
            long[] b = Board.fromFen(fen);
            GameHistory h = GameHistory.initial(b);
            int value = new QuiescenceOracle(HCE).score(b, new SearchLineHistory(h), 0, 0);
            int lower = search.searchWindow(b, h, 0, value - 21, value - 1, ExactSearch.NEVER_CANCELLED).score();
            int upper = search.searchWindow(b, h, 0, value + 1, Math.min(ExactSearch.INFINITY, value + 21), ExactSearch.NEVER_CANCELLED).score();
            assertTrue(lower >= value - 1 && lower <= value, fen);
            assertTrue(upper <= value + 1 && upper >= value, fen);
        }
    }

    @Test void fullWindowOracleCoversNormalToQsearchTransitionAndRepeatability() {
        int comparisons = 0;
        for(String fen : new String[] {CAPTURE, EP, PROMOTION, CAPTURE_PROMOTION, UNDERPROMOTION, EVASION}) {
            long[] board = Board.fromFen(fen);
            long[] original = board.clone();
            for(int depth = 0; depth <= 2; depth++) {
                ExactSearchResult expected = compare(board, GameHistory.initial(board), depth, HCE);
                ExactSearch q = ExactSearch.quiescenceResearch(HCE);
                for(int repeat = 0; repeat < 3; repeat++) same(expected, q.search(board, depth));
                assertArrayEquals(original, board);
                comparisons++;
            }
        }
        assertEquals(18, comparisons);
    }

    @Test void cancellationBudgetAndThreadInterruptionPublishNoPartialValue() {
        long[] board = Board.fromFen(CAPTURE_PROMOTION);
        GameHistory game = GameHistory.initial(board);
        ExactSearch q = ExactSearch.quiescenceResearch(HCE);
        aborted(q.search(board, game, 0, () -> true));
        ExactSearchResult limited = q.search(board, game, 0, () -> q.visitedNodes() >= 3);
        aborted(limited);
        assertEquals(3, limited.qnodes());
        AtomicBoolean cancelled = new AtomicBoolean();
        ExactSearch during = ExactSearch.quiescenceResearch((b, p) -> {
            if(p > 0) cancelled.set(true);
            return Eval.evaluate(b);
        });
        aborted(during.search(board, game, 0, cancelled::get));
        assertTrue(cancelled.get());
        try {
            Thread.currentThread().interrupt();
            aborted(q.search(board, 0));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally { Thread.interrupted(); }
        same(search(board, 0), q.search(board, 0));
    }

    @Test void absoluteStorageBoundaryAbortsUnresolvedCheckAndKeepsMatePly() throws Exception {
        // Place a checked node in the existing last slot directly: no fabricated 256-ply game,
        // smaller production limit or score-producing test policy is introduced.
        ExactSearch q = ExactSearch.quiescenceResearch((b, p) -> { throw new AssertionError("Checked evaluation"); });
        long[][] boards = (long[][]) field(q, "boards");
        long[] checked = Board.fromFen(EVASION);
        System.arraycopy(checked, 0, boards[256], 0, Board.MAX_BITBOARDS);
        set(q, "history", new SearchLineHistory(GameHistory.initial(checked)));
        set(q, "cancelled", ExactSearch.NEVER_CANCELLED);
        Method call = ExactSearch.class.getDeclaredMethod("quiescence", int.class, int.class, int.class, int.class);
        call.setAccessible(true);
        InvocationTargetException error = assertThrows(InvocationTargetException.class,
                () -> call.invoke(q, 256, 3, -32769, 32769));
        assertEquals("Aborted", error.getCause().getClass().getSimpleName());
        long[] tactical = Board.fromFen(CAPTURE);
        // Use HCE for this second boundary case; an unresolved tactical cannot fabricate a static result.
        ExactSearch nonCheck = ExactSearch.quiescenceResearch(HCE);
        System.arraycopy(tactical, 0, ((long[][]) field(nonCheck, "boards"))[256], 0, Board.MAX_BITBOARDS);
        set(nonCheck, "history", new SearchLineHistory(GameHistory.initial(tactical)));
        set(nonCheck, "cancelled", ExactSearch.NEVER_CANCELLED);
        InvocationTargetException pending = assertThrows(InvocationTargetException.class,
                () -> call.invoke(nonCheck, 256, 0, -32769, 32769));
        assertEquals("Aborted", pending.getCause().getClass().getSimpleName());
        assertEquals(0, ((int[]) field(nonCheck, "pvLength"))[256]);
        long[] mate = Board.fromFen("7k/6Q1/5K2/8/8/8/8/8 b - - 0 1");
        System.arraycopy(mate, 0, boards[256], 0, Board.MAX_BITBOARDS);
        assertEquals(-32768 + 256, call.invoke(q, 256, 3, -32769, 32769));
    }

    private static ExactSearchResult compare(long[] board, GameHistory game, int depth, ExactEvaluator evaluator) {
        QuiescenceOracle oracle = new QuiescenceOracle(evaluator);
        int expected = oracle.score(board, new SearchLineHistory(game), depth, 0);
        ExactSearchResult result = ExactSearch.quiescenceResearch(evaluator).search(board, game, depth, ExactSearch.NEVER_CANCELLED);
        assertTrue(result.completed());
        assertEquals(expected, result.score(), "depth=" + depth + " board=" + Arrays.toString(board));
        assertEquals(result.nodes(), result.normalNodes() + result.qnodes());
        assertTrue(result.nodes() <= oracle.nodes);
        SearchLineHistory history = new SearchLineHistory(game);
        int ply = 0;
        for(long m : result.principalVariation()) {
            assertTrue(Arrays.stream(ExhaustiveOracle.legalMoves(board)).anyMatch(legal -> legal == m));
            if(ply >= depth && !inCheck(board)) assertTrue(QuiescenceOracle.tactical(board, m));
            board = ExhaustiveOracle.child(board, m);
            history.pushRealPosition(board);
            ply++;
            int suffix = new QuiescenceOracle(evaluator).score(board, history, depth - ply, ply);
            assertEquals(expected, ply % 2 == 0 ? suffix : -suffix, "Every PV prefix realizes root value");
        }
        return result;
    }

    private static ExactSearchResult search(long[] board, int depth) {
        return ExactSearch.quiescenceResearch(HCE).search(board, depth);
    }

    private static long move(long[] board, String coordinate) {
        return Arrays.stream(ExhaustiveOracle.legalMoves(board)).filter(m -> Move.coordinate(m).equals(coordinate)).findFirst().orElseThrow();
    }

    private static boolean inCheck(long[] board) {
        return Board.isPlayerInCheck(board[0], board[1], board[2], board[3], Board.player((int) board[Board.STATUS]));
    }

    /** Fixture evaluator: isolates promotion choice from HCE's advanced-pawn bonuses. */
    private static int material(long[] board) {
        int white = 0;
        for(int square = 0; square < 64; square++) {
            int type = (int) (((board[0] >>> square) & 1) | (((board[1] >>> square) & 1) << 1)
                    | (((board[2] >>> square) & 1) << 2));
            int value = type == 0 || type == Piece.KING ? 0 : Eval.exchangeValue(type);
            white += ((board[3] >>> square) & 1) == 0 ? value : -value;
        }
        return Board.player((int) board[Board.STATUS]) == 0 ? white : -white;
    }

    private static void same(ExactSearchResult a, ExactSearchResult b) {
        assertEquals(a.completed(), b.completed());
        assertEquals(a.score(), b.score());
        assertEquals(a.bestMove(), b.bestMove());
        assertEquals(a.nodes(), b.nodes());
        assertEquals(a.qnodes(), b.qnodes());
        assertEquals(a.maximumQply(), b.maximumQply());
        assertArrayEquals(a.principalVariation(), b.principalVariation());
    }

    private static void aborted(ExactSearchResult result) {
        assertFalse(result.completed());
        assertEquals(-1, result.completedDepth());
        assertEquals(Value.INVALID, result.score());
        assertFalse(result.hasMove());
        assertEquals(0, result.principalVariation().length);
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = ExactSearch.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = ExactSearch.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
