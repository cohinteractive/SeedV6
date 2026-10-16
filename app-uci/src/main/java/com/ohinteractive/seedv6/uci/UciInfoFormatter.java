package com.ohinteractive.seedv6.uci;

import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.search.common.IterationSnapshot;

/** UCI-only rendering of immutable completed-iteration data. */
final class UciInfoFormatter {

    static String format(IterationSnapshot snapshot) {
        final StringBuilder line = new StringBuilder(128)
            .append("info depth ").append(snapshot.depth())
            .append(" score ").append(UciScore.fromInternal(snapshot.score()).fields())
            .append(" nodes ").append(snapshot.nodes())
            .append(" time ").append(snapshot.elapsedMillis());
        if(snapshot.hasNps()) line.append(" nps ").append(snapshot.nps());
        final long[] pv = snapshot.principalVariation();
        if(pv.length != 0) {
            line.append(" pv");
            for(long move : pv) line.append(' ').append(Move.coordinate(move));
        }
        return line.toString();
    }

    private UciInfoFormatter() {}
}
