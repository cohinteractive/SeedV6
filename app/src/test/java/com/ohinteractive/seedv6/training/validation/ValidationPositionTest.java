package com.ohinteractive.seedv6.training.validation;

import java.util.ArrayList;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.common.TimeSource;
import com.ohinteractive.seedv6.training.selfplay.HeadlessGame;
import com.ohinteractive.seedv6.training.telemetry.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.training.validation.ValidationArenaTest.*;
import static com.ohinteractive.seedv6.training.telemetry.ActiveGameSnapshot.Role.BEST;

class ValidationPositionTest {
    @Test void actualArenaUsesReversedActorsActualPositionsAndLastMoverScore() {
        var feed = new ActiveGameFeed(); feed.validation(2, "candidate", "best");
        var seen = new ArrayList<ActiveGameSnapshot>();
        var root = Board.startingPosition(); var cfg = config(2, 2, 2, 4);
        var result = new ValidationArena().validate(CANDIDATE, INCUMBENT, cfg, root, GameHistory.initial(root),
                new ValidationControl(feed), (network, configuration) -> new ValidationArena.Player() {
                    SearchResult completed;
                    public SearchResult lastResult() { return completed; }
                    public long move(SearchRequest request) {
                        var live = feed.latest(); assertNotNull(live); seen.add(live);
                        long[] board = new long[Board.MAX_BITBOARDS]; request.copyBoardInto(board);
                        assertArrayEquals(board, live.board());
                        var actor = live.sideToMove() == Value.WHITE ? live.white() : live.black();
                        assertEquals(network == CANDIDATE ? CANDIDATE_ROLE : BEST, actor.role());
                        assertEquals(live.gameInPair() == 1 ? CANDIDATE_ROLE : BEST, live.white().role());
                        assertEquals(live.gameInPair() == 1 ? BEST : CANDIDATE_ROLE, live.black().role());
                        if (live.playedPlies() == 0) { assertNull(live.evaluation()); assertEquals(-1, live.lastFrom()); }
                        else {
                            assertEquals(live.sideToMove() == Value.WHITE ? -123 : 123, live.evaluation().whiteScore());
                            assertEquals(1, live.evaluation().depth());
                        }
                        long move = new HeadlessGame(board, 20).legalMoves()[0];
                        completed = new SearchResult(move, true, 123, 1, 10, 1, true); return move;
                    }
                }, p -> { if (!p.gameActive()) assertNull(feed.latest()); }, TimeSource.SYSTEM);
        assertNull(feed.latest()); assertEquals(4, result.statistics().incompletePairs() * 2);
        assertEquals(4, seen.stream().map(ActiveGameSnapshot::gameId).distinct().count());
        assertEquals(16, seen.size());
    }
    private static final ActiveGameSnapshot.Role CANDIDATE_ROLE = ActiveGameSnapshot.Role.CANDIDATE;

    @Test void realNnueValidationEvidenceIsIdenticalWithPositionsEnabled() {
        var feed = new ActiveGameFeed(); feed.validation(1, "candidate", "best");
        var arena = new ValidationArena(); var cfg = config(1, 1, 1, 4);
        var off = arena.validate(CANDIDATE, INCUMBENT, cfg, new ValidationControl());
        var on = arena.validate(CANDIDATE, INCUMBENT, cfg, new ValidationControl(feed));
        assertEquals(off, on); assertEquals(off.assess(PromotionPolicy.DEFAULT), on.assess(PromotionPolicy.DEFAULT));
        assertNull(feed.latest());
    }
    @Test void factorySearchAndCloseFailuresCannotLeaveActiveGame() {
        for (int mode = 0; mode < 3; mode++) {
            int scenario = mode; var feed = new ActiveGameFeed(); feed.validation(1, "candidate", "best");
            var root = Board.startingPosition();
            new ValidationArena().validate(CANDIDATE, INCUMBENT, config(1, 0, 0, 2), root, GameHistory.initial(root),
                    new ValidationControl(feed), (network, cfg) -> {
                        if (scenario == 0) throw new IllegalStateException("factory failure");
                        return new ValidationArena.Player() {
                            public long move(SearchRequest request) {
                                if (scenario == 1) throw new IllegalStateException("search failure");
                                long[] b = new long[Board.MAX_BITBOARDS]; request.copyBoardInto(b);
                                return new HeadlessGame(b, 20).legalMoves()[0];
                            }
                            public void close() { throw new IllegalStateException("close failure"); }
                        };
                    });
            assertNull(feed.latest());
        }
    }
}
