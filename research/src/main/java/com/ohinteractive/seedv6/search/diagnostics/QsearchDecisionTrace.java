package com.ohinteractive.seedv6.search.diagnostics;

import java.util.*;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.order.MoveOrdering;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;

/**
 * Explicit diagnostic-only, single-worker observer. No method supplies a search decision.
 * The shadow owns a one-position state, rebuilt from a private board copy. In particular it
 * never receives the driver's accumulator, TT, history, picker, PV, or SearchControl.
 */
public final class QsearchDecisionTrace {
    public enum Reason { STAND_PAT_BETA, CHILD_BETA, MOVES_EXHAUSTED, NO_TACTICAL_MOVES,
        CHECKMATE, STALEMATE, RULE_DRAW, SOFT_QPLY_LIMIT, ABORTED, EXCEPTION }
    public enum Cutoff { BOTH, DRIVER_ONLY, SHADOW_ONLY, NEITHER }
    public enum Window { BELOW_ALPHA, AT_ALPHA, INSIDE_WINDOW, AT_OR_ABOVE_BETA }
    private static final int MAX_DIFFERENCE = 2 * (TranspositionScores.MATE_THRESHOLD - 1);
    private static final int FIRST_PER_CLASS = 8, PERIOD = 4096, PERIODIC_LIMIT = 128, OUTLIERS = 16;
    private static final int CHILD_EXAMPLES = 8;
    private final SearchEvaluation.State shadow;
    private final int stride;
    private final Frame[] stack = new Frame[TranspositionScores.MAX_MATE_PLY + 1];
    private final long[] scratch = new long[Board.MAX_BITBOARDS];
    private final Metrics total = new Metrics(true);
    private final Map<Integer, Metrics> byPly = new TreeMap<>();
    private final Map<String, Metrics> byCheck = new TreeMap<>(), byIncoming = new TreeMap<>();
    private final Map<Cutoff, Metrics> byClass = new EnumMap<>(Cutoff.class);
    private final Histogram driverAdjacent = new Histogram(), shadowAdjacent = new Histogram(),
            driverAdjacentShadowOnly = new Histogram(), shadowAdjacentShadowOnly = new Histogram(),
            childReturnDelta = new Histogram();
    private final List<Map<String, Object>> examples = new ArrayList<>();
    private final Map<String, Integer> retained = new TreeMap<>();
    private final PriorityQueue<Map<String, Object>> outliers = new PriorityQueue<>(
            Comparator.<Map<String, Object>>comparingInt(m -> (int) m.get("absoluteDifference"))
                    .thenComparingLong(m -> -(long) m.get("ordinal")));
    private long ordinal, staticOrdinal, observedChildEntries, coveredDescendants;
    private int iteration, attempt, periodic;
    private final int positionStride, positionLimit;
    private final List<PositionSample> positions = new ArrayList<>();

    /** Detached diagnostic snapshots. No evaluator runs here; consumers inspect them after search. */
    public record PositionSample(long ordinal, int iteration, int attempt, int ply, int qply,
            long[] board, boolean check, int alpha, int beta, Integer driver, Integer shadow,
            boolean standPatAllowed, Cutoff cutoff, Reason reason, long[] parentBoard, Integer parentDriver) {}

    public QsearchDecisionTrace(SearchEvaluation shadow, int stride) {
        this(shadow, stride, 1, 0);
    }

    public QsearchDecisionTrace(SearchEvaluation shadow, int stride, int positionStride, int positionLimit) {
        this.shadow = Objects.requireNonNull(shadow, "shadow").newState(1);
        if (stride < 1) throw new IllegalArgumentException("Shadow stride must be positive.");
        if (positionStride < 1 || positionLimit < 0 || positionLimit > 20000)
            throw new IllegalArgumentException("Invalid bounded position sampling.");
        this.stride = stride;
        this.positionStride = positionStride;
        this.positionLimit = positionLimit;
        for (int i = 0; i < stack.length; i++) stack[i] = new Frame();
    }

    public void reset() {
        ordinal = staticOrdinal = observedChildEntries = coveredDescendants = 0;
        iteration = attempt = periodic = 0;
        total.clear(); byPly.clear(); byCheck.clear(); byIncoming.clear(); byClass.clear();
        examples.clear(); retained.clear(); outliers.clear();
        positions.clear();
        for (var h : List.of(driverAdjacent, shadowAdjacent, driverAdjacentShadowOnly, shadowAdjacentShadowOnly, childReturnDelta)) h.clear();
    }

    public void beginAttempt(int depth) { iteration = depth; attempt++; }

    public void enter(long[] board, int ply, int qply, int alpha, int beta, boolean check, boolean counted) {
        Frame f = stack[ply];
        f.reset();
        f.ordinal = ++ordinal; f.ply = ply; f.qply = qply; f.alpha = alpha; f.beta = beta;
        f.check = check; f.counted = counted;
        f.covered = counted && ply > 0 && (stack[ply - 1].covered || stack[ply - 1].cutoff == Cutoff.SHADOW_ONLY);
        if (f.covered) coveredDescendants++;
        f.incoming = !counted ? "qsearch-root" : stack[ply - 1].moveCategory;
        System.arraycopy(board, 0, f.board, 0, Board.MAX_BITBOARDS);
        if (counted) observedChildEntries++;
    }

    public void prepared(int ply, int count) { stack[ply].generated = count; }

    /** Called only where the driver actually evaluates. Check/terminal nodes are not evaluated for fiction. */
    public void staticScore(int ply, int driver, boolean standPatAllowed) {
        Frame f = stack[ply];
        f.driver = driver; f.hasStatic = true; f.allowed = standPatAllowed;
        f.driverCutoff = standPatAllowed && driver >= f.beta;
        f.raised = standPatAllowed && !f.driverCutoff && driver > f.alpha;
        Frame parent = f.counted ? stack[ply - 1] : null;
        if (parent != null && parent.hasStatic) {
            f.parentDriverDelta = -driver - parent.driver;
            driverAdjacent.add(Math.abs(f.parentDriverDelta));
            if (parent.cutoff == Cutoff.SHADOW_ONLY) driverAdjacentShadowOnly.add(Math.abs(f.parentDriverDelta));
        }
        if (staticOrdinal++ % stride != 0) return;
        System.arraycopy(f.board, 0, scratch, 0, Board.MAX_BITBOARDS);
        shadow.initialize(scratch, 0);
        f.shadow = shadow.evaluate(scratch, 0);
        if (!Arrays.equals(f.board, scratch)) throw new IllegalStateException("Shadow changed its copied board.");
        f.paired = true;
        if (parent != null && parent.paired) {
            f.parentShadowDelta = -f.shadow - parent.shadow;
            shadowAdjacent.add(Math.abs(f.parentShadowDelta));
            if (parent.cutoff == Cutoff.SHADOW_ONLY) shadowAdjacentShadowOnly.add(Math.abs(f.parentShadowDelta));
        }
        f.difference = Math.abs(driver - f.shadow);
        if (f.difference > MAX_DIFFERENCE) throw new IllegalStateException("Static score outside normal band.");
        if (standPatAllowed) f.cutoff = classify(driver, f.shadow, f.beta);
    }

    public void moveStarted(int ply, long move, int alpha, boolean evasion) {
        Frame f = stack[ply];
        f.searched++; f.move = move; f.moveAlpha = alpha;
        f.moveCategory = category(f.board, move, evasion);
    }

    /** childScore is child's side-to-move score; its negation is used by the driver at this node. */
    public void moveReturned(int ply, int childScore) {
        Frame f = stack[ply];
        f.returned++;
        int slot = Math.min(f.returned - 1, CHILD_EXAMPLES - 1);
        f.moves[slot] = f.move; f.childScores[slot] = childScore; f.childAlphas[slot] = f.moveAlpha;
        f.childCategories[slot] = f.moveCategory;
        f.childRanks[slot] = f.searched;
        if (f.hasStatic) {
            int delta = Math.abs(-childScore - f.driver);
            f.maxChildStaticDelta = Math.max(f.maxChildStaticDelta, delta);
            childReturnDelta.add(delta);
        }
    }

    public void end(int ply, Reason reason, Integer score) {
        Frame f = stack[ply];
        f.reason = reason; f.score = score; f.descendants = ordinal - f.ordinal;
    }

    /** Finally hook, including exceptional unwinds. No state flows back into search. */
    public void leave(int ply) {
        Frame f = stack[ply];
        if ((f.ordinal - 1) % positionStride == 0 && (f.ordinal - 1) / positionStride < positionLimit) {
            Frame parent = f.counted && ply > 0 && stack[ply - 1].hasStatic ? stack[ply - 1] : null;
            positions.add(new PositionSample(f.ordinal, iteration, attempt, f.ply, f.qply, f.board.clone(),
                    f.check, f.alpha, f.beta, f.hasStatic ? f.driver : null, f.paired ? f.shadow : null,
                    f.allowed, f.cutoff, f.reason, parent == null ? null : parent.board.clone(),
                    parent == null ? null : parent.driver));
        }
        total.add(f);
        byPly.computeIfAbsent(f.qply, k -> new Metrics(false)).add(f);
        byCheck.computeIfAbsent(f.check ? "check" : "non-check", k -> new Metrics(false)).add(f);
        byIncoming.computeIfAbsent(f.incoming, k -> new Metrics(false)).add(f);
        if (f.cutoff != null) byClass.computeIfAbsent(f.cutoff, k -> new Metrics(true)).add(f);
        String key = f.cutoff == null ? f.reason.name() : f.cutoff.name();
        boolean first = retained.getOrDefault(key, 0) < FIRST_PER_CLASS;
        boolean periodicSample = f.ordinal % PERIOD == 0 && periodic < PERIODIC_LIMIT;
        boolean extreme = f.paired && (outliers.size() < OUTLIERS
                || f.difference > (int) outliers.peek().get("absoluteDifference"));
        if (first || periodicSample || extreme) {
            Map<String, Object> sample = sample(f);
            if (first || periodicSample) {
                examples.add(sample);
                if (first) retained.merge(key, 1, Integer::sum);
                if (periodicSample) periodic++;
            }
            if (extreme) { if (outliers.size() == OUTLIERS) outliers.remove(); outliers.add(sample); }
        }
    }

    public static Cutoff classify(int driver, int shadow, int beta) {
        return driver >= beta ? (shadow >= beta ? Cutoff.BOTH : Cutoff.DRIVER_ONLY)
                : (shadow >= beta ? Cutoff.SHADOW_ONLY : Cutoff.NEITHER);
    }

    public static Window window(int score, int alpha, int beta) {
        return score >= beta ? Window.AT_OR_ABOVE_BETA : score < alpha ? Window.BELOW_ALPHA
                : score == alpha ? Window.AT_ALPHA : Window.INSIDE_WINDOW;
    }

    private static String category(long[] board, long move, boolean evasion) {
        boolean promotion = ((move >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS) != 0;
        boolean capture = ((move >>> Board.TARGET_PIECE_SHIFT) & Board.PIECE_BITS) != 0
                || !promotion && MoveOrdering.isTactical(board, move);
        return (evasion ? "evasion/" : "tactical/") + (capture ? "capture" : "non-capture")
                + (promotion ? "/promotion" : "");
    }

    public Map<String, Object> summary() {
        var result = map("observed", ordinal, "observedCountedQChildren", observedChildEntries,
                "observedQRoots", ordinal - observedChildEntries,
                "shadowOnlyAncestorDescendants", coveredDescendants,
                "all", total.snapshot(), "byQply", groups(byPly), "byCheck", groups(byCheck),
                "byIncomingMove", groups(byIncoming), "byCutoffClass", groups(byClass));
        result.put("adjacentStaticAbsoluteDelta", map("driver", driverAdjacent.snapshot(), "shadow", shadowAdjacent.snapshot(),
                "driverBelowShadowOnlyParent", driverAdjacentShadowOnly.snapshot(),
                "shadowBelowShadowOnlyParent", shadowAdjacentShadowOnly.snapshot()));
        result.put("childReturnMinusParentStaticAbsolute", childReturnDelta.snapshot());
        result.put("retainedExamples", samples().size());
        return result;
    }

    public Map<String, Object> policy() {
        return map("shadowStaticStride", stride, "shadowSelection", "zero-based actual-static-evaluation ordinal modulo stride == 0",
                "shadowState", "private board copy; fresh production root rebuild per selected position",
                "perspective", "both static scores and windows are current side-to-move; child return is negated for parent",
                "firstCompletedPerClass", FIRST_PER_CLASS, "periodicNodeOrdinal", PERIOD,
                "periodicLimit", PERIODIC_LIMIT, "largestAbsoluteDifferenceExamples", OUTLIERS,
                "childExamples", "first seven and last returned child, with actual ranks",
                "staticEligibility", "actual driver evaluations only; checks, mates and draws have no fabricated stand-pat",
                "counterfactual", "local observed window only; not a substituted downstream tree",
                "quantiles", "exact integer histogram, interpolation at (n-1)*p",
                "adjacentDelta", "abs(-childStatic-parentStatic); only qsearch edges with actual statics at both ends; shadow requires both sampled",
                "descendants", "observed nodes strictly below a shadow-only ancestor within each qsearch tree; union counts nested cases once; not predicted savings",
                "pruningMargins", "none in qsearch; SEE orders but does not reject moves");
    }

    public List<Map<String, Object>> samples() {
        Map<Long, Map<String, Object>> unique = new TreeMap<>();
        for (var s : examples) unique.put((long) s.get("ordinal"), s);
        for (var s : outliers) unique.put((long) s.get("ordinal"), s);
        return List.copyOf(unique.values());
    }

    public List<PositionSample> positionSamples() {
        return positions.stream().sorted(Comparator.comparingLong(PositionSample::ordinal)).toList();
    }

    private Map<String, Object> sample(Frame f) {
        List<Map<String, Object>> children = new ArrayList<>();
        for (int i = 0; i < Math.min(f.returned, CHILD_EXAMPLES); i++) children.add(map(
                "rank", f.childRanks[i], "move", Move.coordinate(f.moves[i]), "category", f.childCategories[i],
                "alphaBeforeChild", f.childAlphas[i], "childScoreSideToMove", f.childScores[i],
                "scoreParentPerspective", -f.childScores[i]));
        return map("ordinal", f.ordinal, "iterationDepth", iteration, "attempt", attempt,
                "absolutePly", f.ply, "qply", f.qply, "countedQChild", f.counted,
                "boardLongsHex", Arrays.stream(f.board).mapToObj(Long::toHexString).toList(),
                "sideToMove", Board.player((int) f.board[Board.STATUS]) == 0 ? "white" : "black",
                "inCheck", f.check, "incomingMoveCategory", f.incoming, "alphaEntry", f.alpha, "betaEntry", f.beta,
                "driverStatic", f.hasStatic ? f.driver : null, "shadowStatic", f.paired ? f.shadow : null,
                "driverMinusShadow", f.paired ? f.driver - f.shadow : null,
                "childMinusParentDriverStatic", f.parentDriverDelta, "childMinusParentShadowStatic", f.parentShadowDelta,
                "absoluteDifference", f.paired ? f.difference : null, "standPatAllowed", f.allowed,
                "standPatRaisedAlpha", f.raised, "standPatBetaCutoff", f.driverCutoff,
                "driverWindow", f.hasStatic ? window(f.driver, f.alpha, f.beta).name() : null,
                "shadowWindow", f.paired ? window(f.shadow, f.alpha, f.beta).name() : null,
                "cutoffClass", f.cutoff == null ? null : f.cutoff.name(), "movesGenerated", f.generated,
                "movesSearched", f.searched, "childrenReturned", f.returned, "childExamples", children,
                "descendantObservedNodes", f.descendants, "reason", f.reason.name(), "returnedScore", f.score);
    }

    private static Map<String, Object> groups(Map<?, Metrics> groups) {
        Map<String, Object> result = new LinkedHashMap<>();
        groups.forEach((k, v) -> result.put(k.toString(), v.snapshot()));
        return result;
    }

    private static Map<String, Object> map(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
        return result;
    }

    private static final class Frame {
        final long[] board = new long[Board.MAX_BITBOARDS], moves = new long[CHILD_EXAMPLES];
        final int[] childScores = new int[CHILD_EXAMPLES], childAlphas = new int[CHILD_EXAMPLES], childRanks = new int[CHILD_EXAMPLES];
        final String[] childCategories = new String[CHILD_EXAMPLES];
        long ordinal, descendants, move;
        int ply, qply, alpha, beta, driver, shadow, difference, generated, searched, returned, moveAlpha, maxChildStaticDelta;
        boolean check, counted, hasStatic, allowed, paired, driverCutoff, raised, covered;
        String incoming, moveCategory;
        Reason reason;
        Cutoff cutoff;
        Integer score, parentDriverDelta, parentShadowDelta;
        void reset() {
            generated = -1; searched = returned = maxChildStaticDelta = 0;
            hasStatic = allowed = paired = driverCutoff = raised = covered = false;
            cutoff = null; score = parentDriverDelta = parentShadowDelta = null; reason = Reason.EXCEPTION; descendants = 0;
        }
    }

    private static final class Metrics {
        long nodes, paired, allowed, driverCutoffs, raised, narrow, close100, close1000, sumDifference,
                generated, searched, returned, descendantSum, childStaticDeltaSum, frontierNodes, frontierDescendants;
        int maxDifference, maxChildStaticDelta;
        final long[] classes = new long[Cutoff.values().length], reasons = new long[Reason.values().length];
        final long[] differences;
        Metrics(boolean histogram) { differences = histogram ? new long[MAX_DIFFERENCE + 1] : null; }
        void clear() {
            nodes = paired = allowed = driverCutoffs = raised = narrow = close100 = close1000 = sumDifference = 0;
            generated = searched = returned = descendantSum = childStaticDeltaSum = 0;
            frontierNodes = frontierDescendants = 0;
            maxDifference = maxChildStaticDelta = 0;
            Arrays.fill(classes, 0); Arrays.fill(reasons, 0);
            if (differences != null) Arrays.fill(differences, 0);
        }
        void add(Frame f) {
            nodes++; reasons[f.reason.ordinal()]++;
            if (f.allowed) allowed++;
            if (f.driverCutoff) driverCutoffs++;
            if (f.raised) raised++;
            generated += Math.max(0, f.generated); searched += f.searched; returned += f.returned;
            descendantSum += f.descendants;
            if (f.cutoff == Cutoff.SHADOW_ONLY && !f.covered) { frontierNodes++; frontierDescendants += f.descendants; }
            childStaticDeltaSum += f.maxChildStaticDelta;
            maxChildStaticDelta = Math.max(maxChildStaticDelta, f.maxChildStaticDelta);
            if (!f.paired) return;
            paired++; sumDifference += f.difference; maxDifference = Math.max(maxDifference, f.difference);
            if (differences != null) differences[f.difference]++;
            if (f.beta - f.alpha == 1) narrow++;
            if (f.difference <= 100) close100++;
            if (f.difference <= 1000) close1000++;
            if (f.cutoff != null) classes[f.cutoff.ordinal()]++;
        }
        Map<String, Object> snapshot() {
            Map<String, Object> reasonCounts = new LinkedHashMap<>();
            for (Reason r : Reason.values()) reasonCounts.put(r.name(), reasons[r.ordinal()]);
            return map("nodes", nodes, "shadowEvaluated", paired, "standPatAllowed", allowed,
                    "driverStandPatBetaCutoffs", driverCutoffs, "standPatRaisedAlpha", raised,
                    "shadowLocalBetaCutoffs", classes[Cutoff.BOTH.ordinal()] + classes[Cutoff.SHADOW_ONLY.ordinal()],
                    "both", classes[Cutoff.BOTH.ordinal()], "driverOnly", classes[Cutoff.DRIVER_ONLY.ordinal()],
                    "shadowOnly", classes[Cutoff.SHADOW_ONLY.ordinal()], "neither", classes[Cutoff.NEITHER.ordinal()],
                    "absoluteDifference", map("count", paired, "mean", paired == 0 ? null : (double) sumDifference / paired,
                            "median", quantile(.5), "p95", quantile(.95), "max", paired == 0 ? null : maxDifference),
                    "pairedNullWindow", narrow, "differenceAtMost100", close100, "differenceAtMost1000", close1000,
                    "movesGenerated", generated, "movesSearched", searched, "childrenReturned", returned,
                    "overlappingDescendantSum", descendantSum,
                    "shadowOnlyFrontierNodes", frontierNodes, "shadowOnlyFrontierDescendants", frontierDescendants,
                    "maxChildReturnMinusStatic", maxChildStaticDelta,
                    "sumPerNodeMaxChildReturnMinusStatic", childStaticDeltaSum, "reasons", reasonCounts);
        }
        Double quantile(double p) {
            if (differences == null || paired == 0) return null;
            double rank = (paired - 1) * p;
            return at((long) rank) * (1 - (rank % 1)) + at((long) Math.ceil(rank)) * (rank % 1);
        }
        int at(long rank) {
            long cumulative = 0;
            for (int i = 0; i < differences.length; i++) { cumulative += differences[i]; if (rank < cumulative) return i; }
            throw new IllegalStateException("Histogram accounting mismatch.");
        }
    }

    /** Includes backed-up mate returns as well as normal-band adjacent static differences. */
    private static final class Histogram {
        private final long[] bins = new long[2 * TranspositionScores.MATE_SCORE + 1];
        private long count, sum;
        private int max;
        void clear() { Arrays.fill(bins, 0); count = sum = 0; max = 0; }
        void add(int value) { bins[value]++; count++; sum += value; max = Math.max(max, value); }
        Map<String, Object> snapshot() {
            return map("count", count, "mean", count == 0 ? null : (double) sum / count,
                    "median", quantile(.5), "p95", quantile(.95), "max", count == 0 ? null : max);
        }
        Double quantile(double p) {
            if (count == 0) return null;
            double rank = (count - 1) * p;
            return at((long) rank) * (1 - rank % 1) + at((long) Math.ceil(rank)) * (rank % 1);
        }
        int at(long rank) {
            long cumulative = 0;
            for (int i = 0; i < bins.length; i++) { cumulative += bins[i]; if (rank < cumulative) return i; }
            throw new IllegalStateException("Histogram accounting mismatch.");
        }
    }
}
