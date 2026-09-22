package com.ohinteractive.seedv6.training.validation;

import java.util.*;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.training.selfplay.TrajectorySampler.Sample;
import static org.junit.jupiter.api.Assertions.*;

class HeldOutLossTest {
    private final List<Sample> samples = List.of(new Sample(Board.startingPosition(), 1), new Sample(Board.startingPosition(), -1));
    @Test void lowerLossPromotesWorseRetainsAndTiesRetain() {
        assertEquals(PromotionPolicy.Decision.PROMOTE, new HeldOutLoss.Comparison(12, .1, .2).decision());
        assertEquals(PromotionPolicy.Decision.RETAIN_INCUMBENT, new HeldOutLoss.Comparison(12, .2, .1).decision());
        assertEquals(PromotionPolicy.Decision.RETAIN_INCUMBENT, new HeldOutLoss.Comparison(12, .1, .1).decision());
        assertEquals(PromotionPolicy.Decision.RETAIN_INCUMBENT, new HeldOutLoss.Comparison(12, -0.0, 0.0).decision());
    }
    @Test void bothActorsSeeExactlyTheSameBoardsInTheSameOrderAndUseHalfSquaredError() {
        var candidateBoards = new ArrayList<long[]>(); var bestBoards = new ArrayList<long[]>();
        var result = HeldOutLoss.compare(board -> { candidateBoards.add(board); return .5; },
                board -> { bestBoards.add(board); return 0; }, samples);
        assertEquals(.625, result.candidateLoss()); assertEquals(.5, result.bestLoss());
        assertEquals(2, candidateBoards.size());
        for (int i = 0; i < samples.size(); i++) { assertSame(candidateBoards.get(i), bestBoards.get(i)); assertArrayEquals(samples.get(i).board(), bestBoards.get(i)); }
    }
    @Test void invalidOrTinyEvidenceCannotPromote() {
        assertThrows(IllegalArgumentException.class, () -> new HeldOutLoss.Comparison(1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new HeldOutLoss.Comparison(4, Double.NaN, 1));
        assertThrows(IllegalArgumentException.class, () -> new HeldOutLoss.Comparison(4, 1, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> new HeldOutLoss.Comparison(4, -.1, .2));
    }
}
