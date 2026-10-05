package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.training.telemetry.ActiveGameSnapshot;
import static com.ohinteractive.seedv6.gui.TrainingDashboardModel.network;

/** Shared read-only participant and move-score semantics; no evaluation or match policy. */
final class MatchPresentation {
    static String participant(ActiveGameSnapshot.Participant p) {
        if (p.model() != null) return p.model().description();
        return switch (p.role()) {
            case LATEST_TRAINING -> "Latest Training ";
            case CANDIDATE -> "Candidate ";
            case BEST -> "Best ";
            case MODEL -> "Model ";
        } + network(p.checkpointId());
    }
    static NetworkArchitecture architecture(ActiveGameSnapshot game, NetworkArchitecture fallback) {
        var actor = game.searchingParticipant();
        return actor != null && actor.model() != null ? NetworkArchitecture.valueOf(actor.model().architecture().name()) : fallback;
    }
    static PlayScore score(ActiveGameSnapshot game, NetworkArchitecture fallback) {
        var e = game.evaluation();
        if (e == null) return new PlayScore("\u2014", .5, false);
        int raw = e.whiteScore();
        if (com.ohinteractive.seedv6.search.tt.TranspositionScores.isMateScore(raw)) {
            int moves = (com.ohinteractive.seedv6.search.tt.TranspositionScores.MATE_SCORE - Math.abs(raw) + 1) / 2;
            return new PlayScore((raw > 0 ? "+M" : "\u2212M") + moves, raw > 0 ? 1 : 0, true);
        }
        return new PlayScore(String.format(java.util.Locale.ROOT, "%+d", raw), architecture(game, fallback).evaluationBar(raw), true);
    }
    static String evaluationCaption(ActiveGameSnapshot game, NetworkArchitecture fallback) {
        var e = game.evaluation();
        if (e == null) return "Evaluation unavailable \u00b7 no completed search for this move";
        return "Last move search \u00b7 White " + score(game, fallback).text() + " \u00b7 " + architecture(game, fallback).evaluationUnits() + "\n"
                + participant(game.searchingParticipant()) + " (" + (e.searchingSide() == Value.WHITE ? "White" : "Black")
                + ") \u00b7 depth " + e.depth() + " \u00b7 before " + Move.coordinate(game.lastMove());
    }
    private MatchPresentation() {}
}
