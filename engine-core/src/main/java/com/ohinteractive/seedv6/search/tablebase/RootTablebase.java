package com.ohinteractive.seedv6.search.tablebase;

import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.SearchControl;

/**
 * Optional root-only game-outcome facility. Implementations must establish the
 * winning decision for the supplied real history/rules and preserve the inputs.
 * Null means no usable decision, never a draw. No result is ordinary TT proof.
 */
@FunctionalInterface
public interface RootTablebase {
    RootTablebase NONE = (board, history, control) -> null;
    TablebaseWin probeWin(long[] board, GameHistory history, SearchControl control);
}
