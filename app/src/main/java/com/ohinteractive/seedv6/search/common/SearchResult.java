package com.ohinteractive.seedv6.search.common;

import java.util.Arrays;

import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot;

public record SearchResult(
    long bestMove,
    boolean hasMove,
    int score,
    int depth,
    long nodes,
    int legalRootMoves,
    boolean completed,
    long[] principalVariation,
    SearchDiagnosticsSnapshot diagnostics
) {

    public SearchResult {
        if(depth < 0) {
            throw new IllegalArgumentException("Search result depth must not be negative: " + depth);
        }
        if(legalRootMoves < 0) {
            throw new IllegalArgumentException("Legal root move count must not be negative: " + legalRootMoves);
        }
        if(!hasMove && bestMove != 0L) {
            throw new IllegalArgumentException("A result without a move must use bestMove 0.");
        }
        principalVariation = principalVariation == null
            ? new long[0] : principalVariation.clone();
        diagnostics = diagnostics == null
            ? SearchDiagnosticsSnapshot.disabled() : diagnostics;
        if(principalVariation.length > 256) {
            throw new IllegalArgumentException(
                "Principal variation exceeds the 256-ply search boundary: "
                    + principalVariation.length
            );
        }
        if(principalVariation.length != 0 && !hasMove) {
            throw new IllegalArgumentException("A result without a move cannot contain a PV.");
        }
        if(principalVariation.length != 0 && principalVariation[0] != bestMove) {
            throw new IllegalArgumentException("The first PV move must equal bestMove.");
        }
    }

    /** Compatibility constructor for exact facilities that do not own PV data. */
    public SearchResult(
        long bestMove, boolean hasMove, int score, int depth, long nodes,
        int legalRootMoves, boolean completed
    ) {
        this(
            bestMove, hasMove, score, depth, nodes, legalRootMoves, completed,
            new long[0], SearchDiagnosticsSnapshot.disabled()
        );
    }

    /** Compatibility constructor for callers supplying PV but no diagnostics. */
    public SearchResult(
        long bestMove, boolean hasMove, int score, int depth, long nodes,
        int legalRootMoves, boolean completed, long[] principalVariation
    ) {
        this(
            bestMove, hasMove, score, depth, nodes, legalRootMoves, completed,
            principalVariation, SearchDiagnosticsSnapshot.disabled()
        );
    }

    /** Returns an independently owned copy; reusable workers cannot mutate this result. */
    @Override
    public long[] principalVariation() {
        return principalVariation.clone();
    }

    public int principalVariationLength() {
        return principalVariation.length;
    }

    public long principalVariationMove(int index) {
        return principalVariation[index];
    }

    @Override
    public boolean equals(Object candidate) {
        if(this == candidate) return true;
        if(!(candidate instanceof SearchResult other)) return false;
        return bestMove == other.bestMove
            && hasMove == other.hasMove
            && score == other.score
            && depth == other.depth
            && nodes == other.nodes
            && legalRootMoves == other.legalRootMoves
            && completed == other.completed
            && Arrays.equals(principalVariation, other.principalVariation)
            && diagnostics.equals(other.diagnostics);
    }

    @Override
    public int hashCode() {
        int hash = Long.hashCode(bestMove);
        hash = 31 * hash + Boolean.hashCode(hasMove);
        hash = 31 * hash + score;
        hash = 31 * hash + depth;
        hash = 31 * hash + Long.hashCode(nodes);
        hash = 31 * hash + legalRootMoves;
        hash = 31 * hash + Boolean.hashCode(completed);
        hash = 31 * hash + Arrays.hashCode(principalVariation);
        return 31 * hash + diagnostics.hashCode();
    }

    @Override
    public String toString() {
        return "SearchResult[bestMove=" + bestMove
            + ", hasMove=" + hasMove
            + ", score=" + score
            + ", depth=" + depth
            + ", nodes=" + nodes
            + ", legalRootMoves=" + legalRootMoves
            + ", completed=" + completed
            + ", principalVariation=" + Arrays.toString(principalVariation)
            + ", diagnostics=" + diagnostics + "]";
    }

}
