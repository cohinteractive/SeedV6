package com.ohinteractive.seedv6.gui;

import java.util.Locale;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;

/** Immutable scoresheet entry. Score provenance is captured once, outside the search hot path. */
record PlayedMove(String san, int side, int number, boolean human, long resultingKey, Score score) {
    PlayedMove withScore(Score value) {
        return score == null ? new PlayedMove(san, side, number, human, resultingKey, value) : this;
    }

    record Score(int whiteValue, int depth, int searchingSide, long positionKey,
                 String evaluator, boolean neural) {
        static Score from(SearchResult result, int side, long key, PlayEvaluator evaluator) {
            return new Score(side == Value.WHITE ? result.score() : -result.score(), result.depth(),
                    side, key, evaluator.description(), evaluator.neural());
        }
        String text() {
            if (TranspositionScores.isMateScore(whiteValue)) {
                int distance = (TranspositionScores.MATE_SCORE - Math.abs(whiteValue) + 1) / 2;
                return (whiteValue > 0 ? "+M" : "−M") + distance;
            }
            return neural ? String.format(Locale.ROOT, "%+d", whiteValue)
                    : String.format(Locale.ROOT, "%+.2f", whiteValue / 100.0);
        }
        String description() {
            return "Completed search · White perspective · depth " + depth + " · " + evaluator
                    + (neural ? " · uncalibrated network units" : " · pawns");
        }
    }
}
