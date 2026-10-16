package com.ohinteractive.seedv6.training.selfplay;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.training.telemetry.ActiveGameFeed;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.training.selfplay.SelfPlayRunnerTest.*;
import static com.ohinteractive.seedv6.training.telemetry.ActiveGameFeedTest.move;

class SelfPlayPositionTest {
    @Test void realLoopPublishesStartAndActualMovesAndClearsCompletionFailureAndCancellation() {
        for (int mode = 0; mode < 3; mode++) {
            int scenario = mode;
            var feed = new ActiveGameFeed(); feed.selfPlay(1, "actor");
            var control = new SelfPlayControl(feed); var game = new HeadlessGame(Board.startingPosition(), 3);
            int[] calls = {0};
            var result = SelfPlayRunner.drive(game, config(1, 1, 0, 0, 3), 0, control, request -> {
                var live = feed.latest(); assertNotNull(live); assertEquals(calls[0], live.playedPlies());
                long[] requested = new long[Board.MAX_BITBOARDS]; request.copyBoardInto(requested);
                assertArrayEquals(requested, live.board());
                assertArrayEquals(game.boardSnapshot(), live.board());
                if (calls[0]++ == 1) {
                    assertEquals(12, live.lastFrom()); assertEquals(28, live.lastTo());
                    if (scenario == 1) throw new IllegalStateException("search failure");
                    if (scenario == 2) { control.cancel(); assertNull(feed.latest()); }
                }
                return move(game, calls[0] == 1 ? "e2e4" : calls[0] == 2 ? "e7e5" : "g1f3");
            });
            assertNull(feed.latest());
            assertEquals(mode == 0 ? GameTermination.PLY_CAP : mode == 1 ? GameTermination.SEARCH_FAILURE : GameTermination.CANCELLED, result.termination());
        }
    }
    @Test void actualNnueSearchWithAndWithoutPublicationHasIdenticalTrajectory() {
        var cfg = config(1, 812, 2, 2, 8); var board = Board.startingPosition();
        var off = SelfPlayRunner.play(NETWORK, cfg, 0, board, new SelfPlayControl());
        var feed = new ActiveGameFeed(); feed.selfPlay(1, "actor");
        var on = SelfPlayRunner.play(NETWORK, cfg, 0, board, new SelfPlayControl(feed));
        assertSameTrajectory(off, on); assertNull(feed.latest());
    }
}
