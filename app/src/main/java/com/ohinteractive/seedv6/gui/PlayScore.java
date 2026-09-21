package com.ohinteractive.seedv6.gui;

import java.util.Locale;
import com.ohinteractive.seedv6.core.util.Value;

/** Presentation mapping only. NNUE V1 units must never be described as centipawns/probability. */
record PlayScore(String text, double whiteFraction, boolean available) {
    static PlayScore from(GameController.SearchInfo search, boolean nnue) {
        String[] parts = search.score().split(" ");
        if (parts.length != 2) return new PlayScore("—", 0.5, false);
        try {
            int raw = Integer.parseInt(parts[1]);
            int white = search.scoreSide() == Value.WHITE ? raw : -raw;
            if (parts[0].equals("mate")) {
                boolean whiteWins = raw == 0 ? search.scoreSide() != Value.WHITE : white > 0;
                return new PlayScore((whiteWins ? "+M" : "−M") + Math.abs(raw), whiteWins ? 1 : 0, true);
            }
            if (!parts[0].equals("cp")) return new PlayScore("—", 0.5, false);
            String text = nnue ? String.format(Locale.ROOT, "%+d", white)
                    : String.format(Locale.ROOT, "%+.2f", white / 100.0);
            // Bounded visual advantage, deliberately not a win probability or calibrated NNUE score.
            double scale = nnue ? 2000.0 : 400.0;
            return new PlayScore(text, 0.5 + 0.48 * Math.tanh(white / scale), true);
        } catch (NumberFormatException invalid) { return new PlayScore("—", 0.5, false); }
    }
}
