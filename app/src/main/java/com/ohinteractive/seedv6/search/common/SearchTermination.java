package com.ohinteractive.seedv6.search.common;

/**
 * Non-chess lifecycle reason for ending a controlled search. A normal exact
 * search leaves its control at {@link #NONE}; its returned SearchResult is the
 * authority for exact-depth completion.
 */
public enum SearchTermination {
    NONE,
    COMPLETED,
    NODE_LIMIT,
    TIME_LIMIT,
    STOPPED,
    REPLACED,
    POSITION_CHANGED,
    NEW_GAME,
    SHUTDOWN,
    FAILURE
}
