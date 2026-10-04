package com.ohinteractive.seedv6.search.common;

/**
 * Non-chess lifecycle reason for ending a controlled search. A normal exact
 * search leaves its control at {@link #NONE}; its returned SearchResult is the
 * authority for exact-depth completion.
 */
public enum SearchTermination {
    NONE,
    COMPLETED,
    /** A separate root game-outcome decision; no searched depth or mate distance is implied. */
    TABLEBASE,
    /** A legal repertoire choice; no searched score or depth is implied. */
    BOOK,
    NODE_LIMIT,
    TIME_LIMIT,
    /** A clock-managed move decision finished before its hard deadline. */
    TIME_ALLOCATION,
    STOPPED,
    REPLACED,
    POSITION_CHANGED,
    NEW_GAME,
    SHUTDOWN,
    FAILURE
}
