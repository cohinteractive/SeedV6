package com.ohinteractive.seedv6.uci;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.search.manage.SearchLimits;
import com.ohinteractive.seedv6.search.manage.TimeManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoCommandParserTest {

    @Test
    void parsesDepthNodesTimeAndTheirCoherentCombinations() {
        assertEquals(new SearchLimits(4, -1L, -1L, false), parse("go depth 4", white()));
        assertEquals(new SearchLimits(0, 0L, -1L, false), parse("go nodes 0", white()));
        assertEquals(new SearchLimits(0, Long.MAX_VALUE, -1L, false),
            parse("go nodes 9223372036854775807", white()));
        assertEquals(new SearchLimits(0, -1L, 0L, false), parse("go movetime 0", white()));
        assertEquals(new SearchLimits(0, -1L, Long.MAX_VALUE - TimeManager.ENGINE_OVERHEAD_MILLIS, false),
            parse("go movetime 9223372036854775807", white()));
        assertEquals(new SearchLimits(3, 420L, 90L, false),
            parse("go nodes 420 movetime 100 depth 3", white()));
        assertTrue(parse("go infinite", white()).infinite());
    }

    @Test
    void selectsOnlyRootSideClockAndIncrement() {
        final SearchLimits white = parse(
            "go wtime 3000 btime 9000 winc 100 binc 900 movestogo 30", white()
        );
        final SearchLimits black = parse(
            "go wtime 3000 btime 9000 winc 100 binc 900 movestogo 30", black()
        );

        assertEquals(TimeManager.allocateClockMillis(3_000L, 100L, 30), white.timeMillis());
        assertEquals(TimeManager.allocateClockMillis(9_000L, 900L, 30), black.timeMillis());
    }

    @Test
    void missingIncrementIsZeroAndDefaultMovesToGoIsThirty() {
        assertEquals(
            TimeManager.allocateClockMillis(3_000L, 0L, TimeManager.DEFAULT_MOVES_TO_GO),
            parse("go wtime 3000", white()).timeMillis()
        );
    }

    @Test
    void explicitMovetimeOverridesValidOrdinaryClockFields() {
        final SearchLimits limits = parse(
            "go movetime 100 wtime 1 btime 2 winc 3 binc 4 movestogo 5", white()
        );
        assertEquals(90L, limits.timeMillis());
    }

    @Test
    void rejectsMalformedNegativeOverflowMissingAndContradictoryForms() {
        assertInvalid("go");
        assertInvalid("go depth 0");
        assertInvalid("go depth 65");
        assertInvalid("go nodes -1");
        assertInvalid("go nodes 9223372036854775808");
        assertInvalid("go movetime nope");
        assertInvalid("go movetime -1");
        assertInvalid("go wtime -1");
        assertInvalid("go wtime 100", black());
        assertInvalid("go btime 100", white());
        assertInvalid("go movestogo 0 wtime 100");
        assertInvalid("go depth 1 depth 2");
        assertInvalid("go infinite nodes 1");
        assertInvalid("go ponder");
    }

    private static SearchLimits parse(String command, long[] board) {
        return GoCommandParser.parse(command.split("\\s+"), (int) board[Board.STATUS]);
    }

    private static void assertInvalid(String command) {
        assertInvalid(command, white());
    }

    private static void assertInvalid(String command, long[] board) {
        assertThrows(IllegalArgumentException.class, () -> parse(command, board));
    }

    private static long[] white() {
        return Board.startingPosition();
    }

    private static long[] black() {
        return Board.fromFen("4k3/8/8/8/8/8/8/4K3 b - - 0 1");
    }
}
