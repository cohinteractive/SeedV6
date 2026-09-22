package com.ohinteractive.seedv6.training.validation;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn2.*;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.model.NetworkModel;
import com.ohinteractive.seedv6.training.telemetry.*;
import static org.junit.jupiter.api.Assertions.*;

class Brn2ValidationWiringTest {
    @Test void realArenaUsesDistinctPinnedBrnValuesForBothColoursAndNeverFallsBack() {
        double[] weights = new double[Brn2Model.PARAMETER_COUNT]; weights[Brn2Model.OUTPUT_BIAS] = .01;
        var candidate = new NetworkModel.Brn2(new Brn2Model(weights));
        var incumbent = new NetworkModel.Brn2(new Brn2Model(new double[Brn2Model.PARAMETER_COUNT]));
        var config = new ValidationConfig(1, 12, 0, 0, 1, 1, NnueScoreMapping.V1, 3);
        var feed = new ActiveGameFeed(); feed.validation(1, "brn-candidate", "brn-best");
        var seen = new ArrayList<ActiveGameSnapshot>();
        var board = Board.startingPosition();
        var result = new ValidationArena().validate(candidate, incumbent, config, board, GameHistory.initial(board),
                new ValidationControl(feed), progress -> {
                    var live = feed.latest();
                    if (live != null && live.evaluation() != null) seen.add(live);
                });
        assertFalse(seen.isEmpty()); assertNull(feed.latest());
        assertEquals(1, result.statistics().incompletePairs());
        for (var live : seen) {
            boolean isCandidate = live.searchingParticipant().role() == ActiveGameSnapshot.Role.CANDIDATE;
            assertEquals(isCandidate ? "brn-candidate" : "brn-best", live.searchingParticipant().checkpointId());
            assertEquals(isCandidate ? 325 : 0, Math.abs(live.evaluation().whiteScore()));
        }
        assertTrue(seen.stream().anyMatch(s -> s.gameInPair() == 1 && s.searchingParticipant().role() == ActiveGameSnapshot.Role.CANDIDATE));
        assertTrue(seen.stream().anyMatch(s -> s.gameInPair() == 2 && s.searchingParticipant().role() == ActiveGameSnapshot.Role.CANDIDATE));
        assertTrue(seen.stream().anyMatch(s -> s.searchingParticipant().role() == ActiveGameSnapshot.Role.BEST));
    }
}
