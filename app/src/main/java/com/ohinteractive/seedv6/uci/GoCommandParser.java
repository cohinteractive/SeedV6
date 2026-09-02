package com.ohinteractive.seedv6.uci;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.search.flat.FlatNegamax;
import com.ohinteractive.seedv6.search.manage.SearchLimits;
import com.ohinteractive.seedv6.search.manage.TimeManager;

/** Strict, deterministic parser/normalizer for the supported WS4 go subset. */
final class GoCommandParser {

    static SearchLimits parse(String[] tokens, int rootStatus) {
        if(tokens.length < 2 || !tokens[0].equals("go")) {
            throw new IllegalArgumentException("A supported go limit is required.");
        }

        int depth = SearchLimits.NO_DEPTH;
        long nodes = SearchLimits.NO_LIMIT;
        long movetime = SearchLimits.NO_LIMIT;
        long whiteTime = SearchLimits.NO_LIMIT;
        long blackTime = SearchLimits.NO_LIMIT;
        long whiteIncrement = SearchLimits.NO_LIMIT;
        long blackIncrement = SearchLimits.NO_LIMIT;
        int movesToGo = 0;
        boolean infinite = false;
        int seen = 0;

        for(int index = 1; index < tokens.length; index ++) {
            final String token = tokens[index];
            final int bit;
            switch(token) {
                case "depth" -> bit = DEPTH;
                case "nodes" -> bit = NODES;
                case "movetime" -> bit = MOVETIME;
                case "wtime" -> bit = WTIME;
                case "btime" -> bit = BTIME;
                case "winc" -> bit = WINC;
                case "binc" -> bit = BINC;
                case "movestogo" -> bit = MOVESTOGO;
                case "infinite" -> bit = INFINITE;
                default -> throw new IllegalArgumentException("Unsupported go token: " + token);
            }
            if((seen & bit) != 0) throw new IllegalArgumentException("Duplicate go token: " + token);
            seen |= bit;
            if(bit == INFINITE) {
                infinite = true;
                continue;
            }
            if(++ index >= tokens.length) throw new IllegalArgumentException("Missing go value for " + token);
            final long value = parseNonNegative(tokens[index], token);
            switch(bit) {
                case DEPTH -> {
                    if(value < 1L || value > FlatNegamax.MAX_SUPPORTED_DEPTH) {
                        throw new IllegalArgumentException("Depth is outside the supported range.");
                    }
                    depth = (int) value;
                }
                case NODES -> nodes = value;
                case MOVETIME -> movetime = value;
                case WTIME -> whiteTime = value;
                case BTIME -> blackTime = value;
                case WINC -> whiteIncrement = value;
                case BINC -> blackIncrement = value;
                case MOVESTOGO -> {
                    if(value < 1L || value > Integer.MAX_VALUE) {
                        throw new IllegalArgumentException("movestogo must be a positive integer.");
                    }
                    movesToGo = (int) value;
                }
                default -> throw new IllegalStateException();
            }
        }

        final boolean hasFinite = depth != SearchLimits.NO_DEPTH
            || nodes != SearchLimits.NO_LIMIT || movetime != SearchLimits.NO_LIMIT
            || (seen & (WTIME | BTIME | WINC | BINC | MOVESTOGO)) != 0;
        if(infinite) {
            if(hasFinite) throw new IllegalArgumentException("infinite cannot be combined with another limit.");
            return new SearchLimits(SearchLimits.NO_DEPTH, SearchLimits.NO_LIMIT, SearchLimits.NO_LIMIT, true);
        }

        final long timeBudget;
        if(movetime != SearchLimits.NO_LIMIT) {
            // Explicit movetime takes precedence over otherwise valid clock fields.
            timeBudget = TimeManager.movetimeBudgetMillis(movetime);
        } else if((seen & (WTIME | BTIME | WINC | BINC | MOVESTOGO)) != 0) {
            final boolean black = (rootStatus & Board.PLAYER_BIT) != 0;
            final long remaining = black ? blackTime : whiteTime;
            if(remaining == SearchLimits.NO_LIMIT) {
                throw new IllegalArgumentException("The side to move has no remaining clock value.");
            }
            final long selectedIncrement = black ? blackIncrement : whiteIncrement;
            final long increment = selectedIncrement == SearchLimits.NO_LIMIT ? 0L : selectedIncrement;
            timeBudget = TimeManager.allocateClockMillis(
                remaining, increment,
                movesToGo == 0 ? TimeManager.DEFAULT_MOVES_TO_GO : movesToGo
            );
        } else {
            timeBudget = SearchLimits.NO_LIMIT;
        }

        return new SearchLimits(depth, nodes, timeBudget, false);
    }

    private static final int DEPTH = 1;
    private static final int NODES = 1 << 1;
    private static final int MOVETIME = 1 << 2;
    private static final int WTIME = 1 << 3;
    private static final int BTIME = 1 << 4;
    private static final int WINC = 1 << 5;
    private static final int BINC = 1 << 6;
    private static final int MOVESTOGO = 1 << 7;
    private static final int INFINITE = 1 << 8;

    private GoCommandParser() {}

    private static long parseNonNegative(String value, String name) {
        final long parsed;
        try {
            parsed = Long.parseLong(value);
        } catch(NumberFormatException exception) {
            throw new IllegalArgumentException("Malformed " + name + '.', exception);
        }
        if(parsed < 0L) throw new IllegalArgumentException(name + " must not be negative.");
        return parsed;
    }
}
