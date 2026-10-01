package com.ohinteractive.seedv6.search.exact;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.tt.TTable;

/** SR-004 research only. The unchanged SearchDriver owns requests/publication.
 * All policies use the same adapter accounting and unchanged default ExactSearch.
 * Each call resets quiet history inside ExactSearch; only the TT spans calls.
 */
final class MtdResearchSearch implements SingleDepthSearch {
    enum Policy { CONTROL, MTD_PREV, MTD_ORACLE, MTD_TWO_PASS, MTD_BRACKETED }
    record TtStats(long probes, long hits, long exact, long lower, long upper, long saves) {}
    /** Mechanical observations only: a hit is NOT necessarily depth-applicable
     * or a cutoff. Used in separate correctness/diagnostic runs, never timing. */
    static final class ObservedTable extends TTable {
        long probes, hits, exact, lower, upper, saves;
        @Override public boolean probe(long key, TEntry entry) {
            probes++;
            boolean hit = super.probe(key, entry);
            if(hit) {
                hits++;
                switch((int) (entry.data >>> 8) & 3) {
                    case TYPE_EXACT -> exact++;
                    case TYPE_LOWER -> lower++;
                    case TYPE_UPPER -> upper++;
                    default -> throw new AssertionError("Not Search evidence");
                }
            }
            return hit;
        }
        @Override public void save(long key, int depth, int type, int score, long move) {
            saves++; super.save(key, depth, type, score, move);
        }
        void resetCounters() { probes = hits = exact = lower = upper = saves = 0; }
        TtStats snapshot() { return new TtStats(probes, hits, exact, lower, upper, saves); }
    }
    record Attempt(String phase, int alpha, int beta, String classification,
                   int lower, int upper, SearchResult result, long visitedNodes, long nanos, TtStats tt) {}
    record Iteration(int depth, Integer guess, List<Attempt> attempts, SearchResult result, long nanos) {}

    final List<Iteration> iterations = new ArrayList<>();
    private final Policy policy;
    private final IntUnaryOperator oracle;
    private final ExactSearch exact;
    private final ObservedTable observed;
    private final long[] root = new long[Board.MAX_BITBOARDS];
    private final long[] scratch = new long[Board.MAX_BITBOARDS];
    private final long[] moves = new long[512];
    private SearchControl control;
    private SearchResult previous;
    private long nodes, evaluations;
    private int maximumPly;

    MtdResearchSearch(Policy policy, ExactEvaluator evaluator, TTable table, IntUnaryOperator oracle) {
        if(table == null || policy == null || policy == Policy.MTD_ORACLE && oracle == null)
            throw new IllegalArgumentException("TT and research policy/oracle required");
        this.policy = policy;
        this.oracle = oracle;
        observed = table instanceof ObservedTable t ? t : null;
        exact = new ExactSearch(new ExactEvaluator() {
            @Override public void initialize(long[] b) { evaluator.initialize(b); }
            @Override public int evaluate(long[] b, int ply) { evaluations++; return evaluator.evaluate(b, ply); }
            @Override public void child(long[] parent, long[] child, int ply) {
                // ExactSearchAdapter's admitted-child convention, including PVS re-entry.
                if(!control.tryEnterNode()) return;
                nodes++;
                maximumPly = Math.max(maximumPly, ply + 1);
                evaluator.child(parent, child, ply);
            }
        }, table);
    }

    // Thresholds are integers, not encoded mate distances. The ordinary band and
    // both mate bands are adjacent; infinities +/-32769 are thresholds only.
    // Guard BEFORE arithmetic: neither infinity nor Value.INVALID is a score.
    static int predecessor(int score) { requireScore(score); return score - 1; }
    static int successor(int score) { requireScore(score); return score + 1; }
    static int midpointBeta(int lower, int upper) {
        requireInterval(lower, upper);
        return lower + (upper - lower + 1) / 2;
    }
    static int outwardBeta(int lower, int upper, int direction, int distance) {
        requireInterval(lower, upper);
        if((direction != 1 && direction != -1) || distance < 1 || distance > 2 * ExactSearch.MATE_SCORE)
            throw new IllegalArgumentException("Invalid outward step");
        // Start each step at the latest SEARCH-PROVEN bound, including fail-soft
        // leaps. A threshold itself is never added to the evidence interval.
        return direction > 0 ? (int) Math.min(upper, (long) lower + distance)
                : (int) Math.max((long) lower + 1, (long) upper - distance + 1);
    }
    private static void requireInterval(int lower, int upper) {
        requireScore(lower); requireScore(upper);
        if(lower >= upper) throw new IllegalArgumentException("No unresolved interval");
    }
    static boolean zeroPhase(String phase) {
        return phase.equals("zero") || phase.equals("bracket") || phase.equals("bisect");
    }
    private static void requireScore(int score) {
        if(score < -ExactSearch.MATE_SCORE || score > ExactSearch.MATE_SCORE)
            throw new IllegalArgumentException("Not a Search score: " + score);
    }

    @Override public void beginRequest() { previous = null; iterations.clear(); exact.beginRequest(); }
    @Override public void endRequest() { exact.endRequest(); }
    @Override public void newGame() { exact.newGame(); }
    @Override public int maxSupportedDepth() { return ExactSearch.MAX_DEPTH; }

    @Override public SearchResult search(SearchRequest request) {
        if(request.diagnosticsEnabled()) throw new IllegalArgumentException("Clean research adapter only");
        long start = System.nanoTime();
        var attempts = new ArrayList<Attempt>();
        Integer guess = null;
        SearchResult result;
        if(policy == Policy.CONTROL || request.depth() == 1) {
            var a = invoke(request, "full", -ExactSearch.INFINITY, ExactSearch.INFINITY,
                    -ExactSearch.MATE_SCORE, ExactSearch.MATE_SCORE);
            attempts.add(a);
            result = a.result();
        } else {
            if(previous == null || previous.depth() != request.depth() - 1)
                throw new IllegalStateException("MTD requires the preceding completed iteration");
            guess = policy == Policy.MTD_ORACLE ? oracle.applyAsInt(request.depth()) : previous.score();
            requireScore(guess);
            int lower = -ExactSearch.MATE_SCORE, upper = ExactSearch.MATE_SCORE, g = guess;
            int completedPasses = 0, direction = 0, distance = 1;
            boolean bracketed = false;
            long totalNodes = 0;
            result = incomplete(request, 0, previous.legalRootMoves());
            while(lower < upper && (policy != Policy.MTD_TWO_PASS || completedPasses < 2)
                    && mayContinue(request.control())) {
                // lower < beta <= upper guarantees strict interval shrink for
                // either completed fail-soft outcome. The guess is never proof.
                int beta = g == lower ? successor(g) : g;
                String phase = "zero";
                if(policy == Policy.MTD_BRACKETED) {
                    phase = bracketed ? "bisect" : "bracket";
                    if(completedPasses > 0) beta = bracketed ? midpointBeta(lower, upper)
                            : outwardBeta(lower, upper, direction, distance);
                }
                if(beta <= lower || beta > upper) throw new AssertionError("Invalid MTD threshold");
                var a = invoke(request, phase, predecessor(beta), beta, lower, upper);
                attempts.add(a);
                totalNodes += a.result().nodes();
                result = incomplete(request, totalNodes, a.result().legalRootMoves());
                if(!a.result().completed()) break;
                g = a.result().score(); lower = a.lower(); upper = a.upper();
                completedPasses++;
                if(policy == Policy.MTD_BRACKETED && !bracketed) {
                    int provenDirection = a.classification().equals("fail-high") ? 1 : -1;
                    if(direction == 0) direction = provenDirection;
                    else if(direction != provenDirection) bracketed = true;
                    else distance = Math.min(2 * ExactSearch.MATE_SCORE, distance * 2);
                }
            }
            boolean fallback = policy == Policy.MTD_TWO_PASS && completedPasses == 2 && lower < upper;
            if((lower == upper || fallback) && mayContinue(request.control())) {
                // Full-window fallback is itself authoritative, with no extra
                // materialization. Otherwise V is inside the smallest integer
                // window: bound PVs alone cannot establish an exact line.
                // Existing exact TT prefixes remain permitted in both paths.
                var a = invoke(request, fallback ? "fallback" : "materialize",
                        fallback ? -ExactSearch.INFINITY : predecessor(lower),
                        fallback ? ExactSearch.INFINITY : successor(lower), lower, upper);
                attempts.add(a);
                totalNodes += a.result().nodes();
                var r = a.result();
                if(r.completed() && (r.score() < lower || r.score() > upper || !a.classification().equals("exact")))
                    throw new AssertionError("Exact result disagrees with proven bounds");
                result = new SearchResult(r.bestMove(), r.hasMove(), r.score(), request.depth(), totalNodes,
                        r.legalRootMoves(), r.completed(), r.principalVariation());
            }
        }
        if(result.completed()) previous = result;
        iterations.add(new Iteration(request.depth(), guess, List.copyOf(attempts), result, System.nanoTime() - start));
        return result;
    }

    private static boolean mayContinue(SearchControl control) {
        if(Thread.currentThread().isInterrupted()) control.request(SearchTermination.STOPPED);
        return !Thread.currentThread().isInterrupted() && control.checkpoint() && control.checkpointNodeBudget();
    }
    private static SearchResult incomplete(SearchRequest request, long nodes, int legal) {
        return new SearchResult(0, false, Value.INVALID, request.depth(), nodes, legal, false, new long[0]);
    }

    private Attempt invoke(SearchRequest request, String phase, int alpha, int beta, int lower, int upper) {
        long start = System.nanoTime();
        control = request.control();
        nodes = evaluations = 0; maximumPly = 0;
        try {
            request.copyBoardInto(root);
            int count = Gen.genAll(root[0], root[1], root[2], root[3], (int) root[Board.STATUS],
                    root[Board.KEY], true, moves, scratch);
            if(observed != null) observed.resetCounters();
            var r = exact.searchWindow(root, request.gameHistory(), request.depth(), alpha, beta, () -> !control.checkpoint());
            if(!r.completed() && Thread.currentThread().isInterrupted()) control.request(SearchTermination.STOPPED);
            var converted = new SearchResult(r.bestMove(), r.hasMove(), r.score(), request.depth(), nodes,
                    count, r.completed(), r.principalVariation());
            String classification = !r.completed() ? "incomplete" : r.score() <= alpha ? "fail-low"
                    : r.score() >= beta ? "fail-high" : "exact";
            if(zeroPhase(phase) && r.completed()) {
                int oldLower = lower, oldUpper = upper;
                if(classification.equals("fail-high")) lower = Math.max(lower, r.score());
                else if(classification.equals("fail-low")) upper = Math.min(upper, r.score());
                else throw new AssertionError("Integer zero window returned interior score");
                if(lower > upper || lower == oldLower && upper == oldUpper)
                    throw new AssertionError("Contradictory or non-progressing Search evidence");
            }
            if(r.completed() && (maximumPly > request.depth() || evaluations > r.nodes()))
                throw new AssertionError("Adapter accounting");
            return new Attempt(phase, alpha, beta, classification, lower, upper, converted, r.nodes(),
                    System.nanoTime() - start, observed == null ? null : observed.snapshot());
        } finally { control = null; }
    }
}
