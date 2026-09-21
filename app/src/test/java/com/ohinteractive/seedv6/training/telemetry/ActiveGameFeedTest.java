package com.ohinteractive.seedv6.training.telemetry;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.training.selfplay.HeadlessGame;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.training.telemetry.ActiveGameSnapshot.Role.*;

public class ActiveGameFeedTest {
    public static long move(HeadlessGame game, String coordinate) {
        return java.util.Arrays.stream(game.legalMoves()).filter(m -> Move.coordinate(m).equals(coordinate)).findFirst().orElseThrow();
    }
    @Test void immutableRealBoardLastMovePerspectiveNewGamesAndGenerations() {
        var feed = new ActiveGameFeed(); assertNull(feed.latest());
        feed.selfPlay(4, "latest-3");
        var game = new HeadlessGame(Board.startingPosition(), 20); feed.start(game, 1, 0);
        var start = feed.latest(); assertEquals(Value.WHITE, start.sideToMove()); assertNull(start.evaluation());
        assertEquals(LATEST_TRAINING, start.white().role()); assertEquals(start.white(), start.black());
        assertEquals("latest-3", start.white().checkpointId()); assertEquals(-1, start.lastFrom());
        long first = move(game, "e2e4"); game.play(first);
        feed.moved(game, first, new SearchResult(first, true, 125, 3, 50, 20, true));
        var after = feed.latest(); assertArrayEquals(Board.fromFen("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"), after.board());
        assertEquals(Value.BLACK, after.sideToMove()); assertEquals(12, after.lastFrom()); assertEquals(28, after.lastTo());
        assertEquals(125, after.evaluation().whiteScore()); assertEquals(start.positionKey(), after.evaluation().sourcePositionKey());
        assertEquals(Value.WHITE, after.evaluation().searchingSide()); assertEquals(3, after.evaluation().depth());
        long second = move(game, "e7e5"); game.play(second);
        feed.moved(game, second, new SearchResult(second, true, 80, 2, 30, 20, true));
        assertEquals(-80, feed.latest().evaluation().whiteScore()); assertEquals(Value.BLACK, feed.latest().evaluation().searchingSide());
        assertEquals(after.positionKey(), feed.latest().evaluation().sourcePositionKey());
        long[] copy = after.board(); copy[0] = 0; assertArrayEquals(Board.startingPosition(), start.board());
        assertNotEquals(0, after.board()[0]); assertTrue(feed.latest().version() > after.version());
        long third = move(game, "g1f3"); game.play(third); feed.moved(game, third, null);
        assertNull(feed.latest().evaluation(), "An unsearched move cannot retain the preceding score");
        feed.clear(); assertNull(feed.latest()); feed.start(new HeadlessGame(Board.startingPosition(), 8), 2, 0);
        assertTrue(feed.latest().gameId() > start.gameId()); assertEquals(2, feed.latest().gameOrdinal());
        assertEquals(0, feed.latest().playedPlies()); assertNull(feed.latest().evaluation());
        feed.selfPlay(5, "latest-4"); assertNull(feed.latest()); feed.start(game, 1, 0);
        assertEquals(5, feed.latest().generation()); assertEquals("latest-4", feed.latest().white().checkpointId());
    }
    @Test void completionCancellationAndValidationSwapClearOrReplace() {
        var feed = new ActiveGameFeed(); feed.validation(6, "candidate", "incumbent");
        var game = new HeadlessGame(Board.startingPosition(), 1); feed.start(game, 1, 1);
        assertEquals(CANDIDATE, feed.latest().white().role()); assertEquals(BEST, feed.latest().black().role());
        long move = move(game, "e2e4"); game.play(move); feed.moved(game, move, null); assertNull(feed.latest());
        game = new HeadlessGame(Board.startingPosition(), 8); feed.start(game, 2, 2);
        assertEquals(BEST, feed.latest().white().role()); assertEquals("incumbent", feed.latest().white().checkpointId());
        assertEquals(CANDIDATE, feed.latest().black().role()); assertEquals("candidate", feed.latest().black().checkpointId());
        new com.ohinteractive.seedv6.training.validation.ValidationControl(feed).cancel(); assertNull(feed.latest());
        feed.start(game, 3, 1); assertNull(feed.latest(), "Late worker publications cannot revive a closed feed");
        feed.moved(game, move, null); assertNull(feed.latest());
    }
}
