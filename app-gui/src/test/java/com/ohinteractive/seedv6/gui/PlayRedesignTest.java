package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.move.MoveIntent.Promotion;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.manage.*;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;
import static org.junit.jupiter.api.Assertions.*;

class PlayRedesignTest {
    @Test void preparationStartFenCancellationAndTermination() throws Exception {
        edt(() -> {
            Harness h = new Harness();
            h.controller.setHumanSide(GameController.HumanSide.BLACK);
            h.controller.initialize(); h.controller.newGame();
            assertEquals(0, h.search.starts); assertFalse(h.view.position.humanInput());
            assertFalse(h.controller.canResign()); assertFalse(h.controller.canStopGame());
            h.controller.startGame(); h.controller.startGame();
            assertEquals(1, h.search.starts);
            Object stale = h.search.token;
            long oldMove = exact(h.controller.boardSnapshot(), "e2e4");
            h.controller.newGame();
            h.controller.onComplete(stale, result(oldMove, 20, SearchTermination.COMPLETED));
            assertTrue(h.controller.displayedMoves().isEmpty());
            assertTrue(h.controller.loadFen("4k3/8/8/8/4P3/8/8/4K3 b - - 0 17"));
            assertEquals(1, h.search.starts); assertFalse(h.view.position.humanInput());
            h.controller.squarePressed(square("e8")); h.controller.squareReleased(square("d8"));
            assertTrue(h.controller.displayedMoves().isEmpty());
            h.controller.startGame(); assertTrue(h.view.position.humanInput());
            assertTrue(h.controller.canResign()); h.controller.resign();
            assertEquals(PositionStatus.Outcome.BLACK_RESIGNED, h.controller.positionStatus().outcome());
            assertFalse(h.view.position.humanInput()); assertFalse(h.controller.canStartGame());
        });
    }

    @Test void completedScoresArePositionBoundWhiteRelativeAndImmutable() throws Exception {
        edt(() -> {
            Harness h = new Harness(); h.controller.startGame();
            h.move("e2", "e4");
            assertNull(h.view.position.playedMoves().getFirst().score());
            long e5 = exact(h.controller.boardSnapshot(), "e7e5");
            long searchedKey = h.controller.boardSnapshot()[Board.KEY];
            h.complete(result(e5, 35, SearchTermination.COMPLETED));
            var human = h.view.position.playedMoves().getFirst();
            var engine = h.view.position.playedMoves().getLast();
            assertEquals("e4", human.san()); assertEquals("e5", engine.san());
            assertEquals(-35, human.score().whiteValue()); assertEquals("-0.35", engine.score().text());
            assertEquals(searchedKey, human.score().positionKey());
            h.move("g1", "f3");
            long nc6 = exact(h.controller.boardSnapshot(), "b8c6");
            h.complete(result(nc6, -TranspositionScores.MATE_SCORE + 5, SearchTermination.COMPLETED));
            assertEquals("+M3", h.view.position.playedMoves().getLast().score().text());
            assertEquals(human, h.view.position.playedMoves().getFirst());
            assertEquals("Nf3", h.view.position.playedMoves().get(2).san());
            assertEquals("Nc6", h.view.position.playedMoves().get(3).san());
        });
    }

    @Test void bookFallbackMismatchAndStaleSearchNeverInventScores() throws Exception {
        edt(() -> {
            Harness h = new Harness(); h.controller.startGame(); h.move("e2", "e4");
            long e5 = exact(h.controller.boardSnapshot(), "e7e5");
            h.complete(new ManagedSearchResult(1, e5, true, null, SearchTermination.BOOK, 0, null));
            assertTrue(h.view.position.playedMoves().stream().allMatch(m -> m.score() == null));
            h.move("g1", "f3");
            long nc6 = exact(h.controller.boardSnapshot(), "b8c6");
            long nf6 = exact(h.controller.boardSnapshot(), "g8f6");
            h.complete(new ManagedSearchResult(2, nc6, true, result(nf6, 99, SearchTermination.COMPLETED).lastCompletedResult(),
                    SearchTermination.STOPPED, 20, null));
            assertTrue(h.view.position.playedMoves().stream().allMatch(m -> m.score() == null));
            h.move("f1", "c4"); Object stale = h.search.token;
            long next = exact(h.controller.boardSnapshot(), "g8f6");
            h.controller.stopGame(); h.controller.onComplete(stale, result(next, 100, SearchTermination.COMPLETED));
            assertEquals(5, h.view.position.playedMoves().size());
            assertNull(h.view.position.playedMoves().getLast().score());
        });
    }

    @Test void engineVersusEngineContinuesButStoppedSearchDoesNotStartAnother() throws Exception {
        edt(() -> {
            Harness h = new Harness(); h.controller.setGameMode(GameController.GameMode.ENGINE_VS_ENGINE);
            h.controller.newGame(); assertEquals(0, h.search.starts);
            h.controller.startGame();
            h.controller.startEngineGame(PlayParticipants.Selection.BEST); // Duplicate must not pause self-play.
            h.complete(result(exact(h.controller.boardSnapshot(), "e2e4"), 45, SearchTermination.COMPLETED));
            assertEquals(2, h.search.starts); assertEquals(45, h.view.position.playedMoves().getFirst().score().whiteValue());
            h.controller.stopSearch();
            h.complete(result(exact(h.controller.boardSnapshot(), "e7e5"), 12, SearchTermination.STOPPED));
            assertEquals(2, h.search.starts); assertEquals(-12, h.view.position.playedMoves().getLast().score().whiteValue());
            assertFalse(h.view.position.humanInput()); h.controller.stopGame();
            assertEquals(PositionStatus.Outcome.STOPPED, h.controller.positionStatus().outcome());
        });
    }

    @Test void evaluatorLoadingStartsOnceAndFailureLeavesFenPrepared() throws Exception {
        edt(() -> {
            class DelayedSearch extends FakeSearch {
                java.util.function.Consumer<String> loaded;
                public void changeParticipants(PlayEvaluator.Mode mode, java.nio.file.Path root,
                        PlayParticipants.Selection selection, java.util.function.Consumer<String> completed) { loaded = completed; }
            }
            DelayedSearch search = new DelayedSearch();
            TestView view = new TestView() { public void showError(String title, String message) {} };
            GameController controller = new GameController(search, view);
            controller.setHumanSide(GameController.HumanSide.BLACK);
            controller.loadFen("4k3/8/8/8/4P3/8/8/4K3 w - - 0 8");
            long[] prepared = controller.boardSnapshot();
            controller.startGame(PlayEvaluator.Mode.POLICY4_PAIR2, PlayParticipants.Selection.BEST);
            controller.startGame(); assertEquals(0, search.starts); assertFalse(controller.canStartGame());
            search.loaded.accept("Checkpoint unavailable");
            assertTrue(controller.canStartGame()); assertArrayEquals(prepared, controller.boardSnapshot());
            controller.startGame(PlayEvaluator.Mode.POLICY4_PAIR2, PlayParticipants.Selection.BEST);
            search.loaded.accept(null); assertEquals(1, search.starts); assertArrayEquals(prepared, controller.boardSnapshot());
            controller.startGame(); assertEquals(1, search.starts);
        });
    }

    @Test void dragClickIllegalOutsideStaleAndPromotionUseOneMovePath() throws Exception {
        edt(() -> {
            Harness h = new Harness(); h.controller.setGameMode(GameController.GameMode.HUMAN_VS_HUMAN);
            h.drag("e2", "e4"); assertTrue(h.controller.displayedMoves().isEmpty());
            h.controller.startGame();
            h.drag("e2", "e5"); assertTrue(h.controller.displayedMoves().isEmpty());
            h.press("e2"); h.motion(-20, -20); h.release(-20, -20);
            assertTrue(h.controller.displayedMoves().isEmpty());
            h.drag("e2", "e4"); assertEquals(List.of("e2e4"), h.controller.displayedMoves());
            h.click("e7"); h.click("e5"); assertEquals(2, h.controller.displayedMoves().size());
            h.press("g1"); h.motion(10, 10); h.controller.newGame(); h.release("f3");
            assertTrue(h.controller.displayedMoves().isEmpty());
            h.controller.loadFen("7k/P7/8/8/8/8/8/7K w - - 0 1"); h.controller.startGame();
            h.drag("a7", "a8"); assertEquals("a8=Q+", h.view.position.playedMoves().getFirst().san());
            assertEquals(1, h.view.promotions);
        });
    }

    @Test void capturesAndPromotionsUseCommittedHistoryAndActualMaterial() {
        GameSession s = GameSession.fromFen("1r5k/P7/8/8/8/8/8/7K w - - 0 1");
        assertEquals(-4, BoardMaterial.balance(s.boardSnapshot(), 0));
        s.applyIntent(square("a7"), square("b8"), Promotion.QUEEN);
        assertEquals(9, BoardMaterial.balance(s.boardSnapshot(), 0));
        assertEquals(1, s.capturedPieces().count(Piece.ROOK | 8));
        assertFalse(s.capturedPieces().fromStartingPosition());
        assertEquals(0, s.capturedPieces().count(Piece.PAWN | 8));
        GameSession ep = GameSession.fromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1");
        ep.applyIntent(square("e5"), square("d6"), Promotion.NONE);
        assertEquals(1, ep.capturedPieces().count(Piece.PAWN | 8));
        assertEquals("exd6", ep.playedMoves().getFirst().san());
    }

    @Test void scoresheetHasAlignedBorderlessColumnsAndBlackFenNumbering() throws Exception {
        edt(() -> {
            Harness h = new Harness(); h.controller.setGameMode(GameController.GameMode.HUMAN_VS_HUMAN);
            h.controller.loadFen("4k3/8/8/8/4P3/8/8/4K3 b - - 0 17"); h.controller.startGame(); h.move("e8", "d8");
            MoveScoresheet sheet = new MoveScoresheet(); sheet.showPosition(h.view.position);
            JTable table = find(sheet, JTable.class);
            assertNull(table.getTableHeader()); assertFalse(table.getShowHorizontalLines()); assertFalse(table.getShowVerticalLines());
            assertEquals("17.", table.getValueAt(0, 0)); assertEquals("", table.getValueAt(0, 1));
            assertEquals("Kd8", ((PlayedMove) table.getValueAt(0, 3)).san()); assertEquals("", table.getValueAt(0, 4));
        });
    }

    static int square(String s) { return (s.charAt(1) - '1') * 8 + s.charAt(0) - 'a'; }
    static long exact(long[] board, String coordinate) {
        return new com.ohinteractive.seedv6.core.move.LegalMoveResolver().resolve(board,
                new com.ohinteractive.seedv6.core.move.MoveIntent(square(coordinate.substring(0, 2)), square(coordinate.substring(2, 4)), Promotion.NONE));
    }
    static ManagedSearchResult result(long move, int score, SearchTermination termination) {
        return new ManagedSearchResult(1, move, true, new SearchResult(move, true, score, 3, 20, 1, true), termination, 20, null);
    }
    static void edt(Runnable task) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> { try { task.run(); } catch (Throwable t) { failure.set(t); } });
        if (failure.get() != null) throw new AssertionError(failure.get());
    }
    static <T> T find(Container container, Class<T> type) {
        for (Component c : container.getComponents()) {
            if (type.isInstance(c)) return type.cast(c);
            if (c instanceof Container child) { T result = find(child, type); if (result != null) return result; }
        }
        return null;
    }
    static class Harness {
        final FakeSearch search = new FakeSearch(); final TestView view = new TestView();
        final GameController controller = new GameController(search, view);
        Harness() { view.board.setSize(640, 640); view.board.setInputListener(controller); controller.initialize(); }
        void move(String from, String to) { controller.squarePressed(square(from)); controller.squareReleased(square(to)); }
        void complete(ManagedSearchResult result) { search.running = false; controller.onComplete(search.token, result); }
        Point point(String square) {
            Rectangle b = view.board.boardBounds(); int s = square(square);
            return new Point(b.x + ((s & 7) * 2 + 1) * b.width / 16, b.y + ((7 - (s >>> 3)) * 2 + 1) * b.height / 16);
        }
        void event(int id, int x, int y) { view.board.dispatchEvent(new MouseEvent(view.board, id, 1,
                id == MouseEvent.MOUSE_DRAGGED ? MouseEvent.BUTTON1_DOWN_MASK : 0, x, y, 1, false,
                id == MouseEvent.MOUSE_DRAGGED ? MouseEvent.NOBUTTON : MouseEvent.BUTTON1)); }
        void press(String s) { Point p = point(s); event(MouseEvent.MOUSE_PRESSED, p.x, p.y); }
        void release(String s) { Point p = point(s); release(p.x, p.y); }
        void release(int x, int y) { event(MouseEvent.MOUSE_RELEASED, x, y); }
        void motion(int x, int y) { event(MouseEvent.MOUSE_DRAGGED, x, y); }
        void click(String s) { press(s); release(s); }
        void drag(String from, String to) { press(from); Point p = point(to); motion(p.x, p.y); release(to); }
    }
    static class TestView implements GameController.View {
        final BoardPanel board = new BoardPanel(); GameController.PositionView position; int promotions;
        public void showPosition(GameController.PositionView value) { position = value; board.showPosition(value); }
        public void showSearch(GameController.SearchInfo info) {}
        public void setSearchRunning(boolean running) {}
        public Promotion choosePromotion(List<Promotion> choices) { promotions++; return Promotion.QUEEN; }
        public void showError(String title, String message) { fail(title + ": " + message); }
    }
    static class FakeSearch implements SearchGateway {
        Object token; int starts; boolean running;
        public void start(long[] board, GameHistory history, SearchLimits limits, Object token, Listener listener) {
            history.requireCurrent(board); this.token = token; starts++; running = true;
        }
        public void stop() {}
        public void invalidate(SearchTermination reason) { running = false; }
        public boolean isSearching() { return running; }
        public int workerCount() { return 1; }
        public void replaceWorkerCount(int count) {}
        public Runnable beginShutdown() { return () -> {}; }
    }
}
