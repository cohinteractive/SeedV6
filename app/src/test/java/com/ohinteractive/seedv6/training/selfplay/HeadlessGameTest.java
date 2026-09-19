package com.ohinteractive.seedv6.training.selfplay;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Value;
import static org.junit.jupiter.api.Assertions.*;

class HeadlessGameTest {
    @Test
    void bothCheckmatesAndStalematePrecedeFiftyMoveRule() {
        assertTerminal("7k/6Q1/6K1/8/8/8/8/8 b - - 100 1",
                GameTermination.WHITE_CHECKMATES_BLACK, GameResult.WHITE_WIN);
        assertTerminal("8/8/8/8/8/6k1/6q1/7K w - - 100 1",
                GameTermination.BLACK_CHECKMATES_WHITE, GameResult.BLACK_WIN);
        assertTerminal("7k/5Q2/6K1/8/8/8/8/8 b - - 100 1",
                GameTermination.STALEMATE, GameResult.DRAW);
    }

    @Test
    void rulesDetectFiftyMoveAndConservativeMaterialDraws() {
        assertTerminal("4k3/8/8/8/8/8/8/R3K3 w - - 100 1",
                GameTermination.FIFTY_MOVE_RULE, GameResult.DRAW);
        assertTerminal("4k3/8/8/8/8/8/8/4K3 w - - 0 1",
                GameTermination.INSUFFICIENT_MATERIAL, GameResult.DRAW);
        HeadlessGame game = game("4k3/8/8/8/8/8/8/R3K3 w - - 99 1", 1);
        play(game, "a1a2");
        assertEquals(GameTermination.FIFTY_MOVE_RULE, game.termination(), "Genuine result wins over cap.");
        assertEquals(1, game.trajectory().positions().size());
    }

    @Test
    void formalThreefoldRequiresThreeOccurrencesAndKeepsHistoryCoherent() {
        HeadlessGame game = new HeadlessGame(Board.startingPosition(), 20);
        for (int cycle = 0; cycle < 2; cycle++) {
            for (String move : new String[] {"g1f3", "g8f6", "f3g1", "f6g8"}) {
                play(game, move);
                game.historySnapshot().requireCurrent(game.boardSnapshot());
            }
            if (cycle == 0) assertTrue(game.active());
        }
        assertEquals(GameTermination.THREEFOLD_REPETITION, game.termination());
        GameTrajectory trajectory = game.trajectory();
        assertEquals(8, trajectory.playedPlies());
        assertEquals(9, trajectory.history().size());
        assertTrue(trajectory.history().isFormalThreefold(trajectory.finalBoard()));
        replay(trajectory);
    }

    @Test
    void trajectoryIsBeforeEachMoveAndExcludesTerminalBoard() {
        HeadlessGame game = foolsMate();
        GameTrajectory trajectory = game.trajectory();
        assertEquals(GameTermination.BLACK_CHECKMATES_WHITE, trajectory.termination());
        assertEquals(4, trajectory.positions().size());
        assertArrayEquals(Board.startingPosition(), trajectory.positions().getFirst().board());
        assertFalse(Arrays.equals(trajectory.finalBoard(), trajectory.positions().getLast().board()));
        for (int i = 0; i < 4; i++) {
            assertEquals(i, trajectory.positions().get(i).ply());
            assertEquals(i % 2, trajectory.positions().get(i).sideToMove());
        }
        assertEquals(5, trajectory.history().size());
        replay(trajectory);
        assertThrows(IllegalStateException.class, () -> game.play(0));
    }

    @Test
    void boardAndTrajectorySnapshotsDoNotAliasCallerOrSession() {
        long[] root = Board.startingPosition();
        HeadlessGame game = new HeadlessGame(root, 1);
        root[0] = 0;
        long[] exposed = game.boardSnapshot();
        exposed[1] = 0;
        play(game, "e2e4");
        GameTrajectory trajectory = game.trajectory();
        assertArrayEquals(Board.startingPosition(), trajectory.positions().getFirst().board());
        long[] recorded = trajectory.positions().getFirst().board();
        recorded[2] = 0;
        assertArrayEquals(Board.startingPosition(), trajectory.positions().getFirst().board());
        long[] end = trajectory.finalBoard();
        end[0] = 0;
        trajectory.history().requireCurrent(trajectory.finalBoard());
        assertThrows(UnsupportedOperationException.class, () -> trajectory.positions().clear());
    }

    @Test
    void illegalMoveDoesNotMutateAnythingAndCapIsNotDraw() {
        HeadlessGame game = new HeadlessGame(Board.startingPosition(), 1);
        long[] root = game.boardSnapshot();
        assertThrows(IllegalArgumentException.class, () -> game.play(0));
        assertArrayEquals(root, game.boardSnapshot());
        assertEquals(1, game.historySnapshot().size());
        assertEquals(0, game.playedPlies());
        assertThrows(IllegalStateException.class, game::trajectory);
        play(game, "e2e4");
        assertEquals(GameTermination.PLY_CAP, game.termination());
        assertTrue(game.trajectory().result().isEmpty());
        assertTrue(game.termination().aborted());
    }

    @Test
    void exactSpecialMovesUseBoardAndKeepHistoryInSync() {
        String[][] fixtures = {
            {"r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1", "e1g1"},
            {"r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 0 1", "e8c8"},
            {"4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 2", "e5d6"},
            {"1r5k/P7/8/8/8/8/8/7K w - - 0 1", "a7b8n"},
            {"7k/8/8/8/8/8/p7/1R5K b - - 0 1", "a2b1q"}
        };
        for (String[] fixture : fixtures) {
            HeadlessGame game = game(fixture[0], 1);
            play(game, fixture[1]);
            assertEquals(2, game.historySnapshot().size());
            replay(game.trajectory());
        }
    }

    @Test
    void sixResultSideCombinationsAreExact() {
        assertEquals(1, GameResult.WHITE_WIN.whiteResult());
        assertEquals(-1, GameResult.BLACK_WIN.whiteResult());
        assertEquals(0, GameResult.DRAW.whiteResult());
        assertEquals(1.0, GameResult.WHITE_WIN.target(Value.WHITE));
        assertEquals(-1.0, GameResult.WHITE_WIN.target(Value.BLACK));
        assertEquals(-1.0, GameResult.BLACK_WIN.target(Value.WHITE));
        assertEquals(1.0, GameResult.BLACK_WIN.target(Value.BLACK));
        assertEquals(0.0, GameResult.DRAW.target(Value.WHITE));
        assertEquals(0.0, GameResult.DRAW.target(Value.BLACK));
        assertThrows(IllegalArgumentException.class, () -> GameResult.DRAW.target(2));
    }

    static HeadlessGame foolsMate() {
        HeadlessGame game = new HeadlessGame(Board.startingPosition(), 20);
        for (String move : new String[] {"f2f3", "e7e5", "g2g4", "d8h4"}) play(game, move);
        return game;
    }

    static HeadlessGame game(String fen, int cap) { return new HeadlessGame(Board.fromFen(fen), cap); }

    static void play(HeadlessGame game, String coordinate) { game.play(find(game, coordinate)); }

    static long find(HeadlessGame game, String coordinate) {
        return Arrays.stream(game.legalMoves()).filter(move -> Move.coordinate(move).equals(coordinate))
                .findFirst().orElseThrow(() -> new AssertionError("Missing legal move " + coordinate));
    }

    static void replay(GameTrajectory trajectory) {
        if (trajectory.positions().isEmpty()) return;
        HeadlessGame replay = new HeadlessGame(trajectory.positions().getFirst().board(), Integer.MAX_VALUE);
        for (GameTrajectory.Position position : trajectory.positions()) {
            assertTrue(replay.active());
            assertArrayEquals(replay.boardSnapshot(), position.board());
            replay.play(position.playedMove());
        }
        assertArrayEquals(trajectory.finalBoard(), replay.boardSnapshot());
        trajectory.history().requireCurrent(replay.boardSnapshot());
        assertEquals(replay.historySnapshot().size(), trajectory.history().size());
        for (int i = 0; i < trajectory.history().size(); i++) {
            assertEquals(replay.historySnapshot().keyAt(i), trajectory.history().keyAt(i));
        }
    }

    private static void assertTerminal(String fen, GameTermination reason, GameResult result) {
        HeadlessGame game = game(fen, 20);
        assertFalse(game.active());
        assertEquals(reason, game.termination());
        assertEquals(result, game.trajectory().result().orElseThrow());
        assertTrue(game.trajectory().positions().isEmpty(), "Terminal initial boards supply no positions.");
        game.trajectory().history().requireCurrent(game.trajectory().finalBoard());
    }
}
