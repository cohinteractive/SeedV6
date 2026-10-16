package com.ohinteractive.seedv6.search.exact;

import static com.ohinteractive.seedv6.search.exact.ExactSearch.*;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BooleanSupplier;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.See;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.rules.DrawAdjudicator;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;

/**
 * SR-002 experiment only: flat equivalent of production TT-enabled staged/lazy PVS.
 * No production consumer selects this class. ExactSearch remains the recursive control
 * and TT-off oracle. All node traversal is in one loop; helpers never descend.
 *
 * Current-node values stay in locals. Seven int arrays and one long array retain
 * suspended parent frames (36 bytes/ply of payload). Depth is rootDepth - ply;
 * board, move and PV storage use the same preallocated representation as ExactSearch.
 * The optional local-leaf variant avoids spilling a frame for children that cannot
 * descend. Its leaf helper is nonrecursive and preserves every entry/checkpoint.
 * No frame allocation, board undo/copy, or new policy is introduced.
 */
public final class FlatExactSearch {
    private static final int MAX_MOVES = 512;
    private static final int ENTER = 0, NEXT = 1, RETURN = 2, LEAF_RETURN = 3;
    private static final int QUIET_PENDING = 1, FULL_SEARCH = 2;

    private final ExactEvaluator evaluator;
    private final TTable table;
    private final boolean localLeaves;
    private final TTable.TEntry entry = new TTable.TEntry();
    private final long[][] boards = new long[MAX_DEPTH + 1][Board.MAX_BITBOARDS];
    private final long[][] moves = new long[MAX_DEPTH + 1][MAX_MOVES];
    private final long[][] pv = new long[MAX_DEPTH + 1][MAX_DEPTH];
    private final int[] pvLength = new int[MAX_DEPTH + 1];
    private final int[] selectionKeys = new int[MAX_DEPTH * MAX_MOVES];
    private final int[] quietHistory = new int[QuietHistory.SIZE];
    private final long[] generatorScratch = new long[Board.MAX_BITBOARDS];
    private final long[] quietScratch = new long[MAX_MOVES];
    private final long[] historyKeys = new long[MAX_DEPTH + 1];
    private final int[] frameAlpha = new int[MAX_DEPTH];
    private final int[] frameBeta = new int[MAX_DEPTH];
    private final int[] frameOriginalAlpha = new int[MAX_DEPTH];
    private final int[] frameCount = new int[MAX_DEPTH];
    private final int[] frameIndex = new int[MAX_DEPTH];
    private final int[] frameBest = new int[MAX_DEPTH];
    private final int[] frameFlags = new int[MAX_DEPTH];
    private final long[] frameKey = new long[MAX_DEPTH];
    private SearchLineHistory history;
    private BooleanSupplier cancelled;
    private long nodes;
    private int generation;
    private boolean active, requestActive;

    /** Exclusive fresh table ownership, identical to ExactSearch; this experiment requires TT. */
    public FlatExactSearch(ExactEvaluator evaluator, TTable table) {
        this(evaluator, table, true);
    }

    /** false retains the first all-frames SR-002 layout for reproducible comparison. */
    public FlatExactSearch(ExactEvaluator evaluator, TTable table, boolean localLeaves) {
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
        this.table = Objects.requireNonNull(table, "table");
        this.localLeaves = localLeaves;
    }

    public void beginRequest() {
        if(active || requestActive) throw new IllegalStateException("Search request already active.");
        advanceGeneration();
        requestActive = true;
    }

    public void endRequest() {
        if(active || !requestActive) throw new IllegalStateException("No idle Search request.");
        requestActive = false;
    }

    public void newGame() {
        if(active || requestActive) throw new IllegalStateException("Search request active.");
        table.clear();
    }

    private void advanceGeneration() {
        table.advanceGeneration();
        generation = (generation + 1) & 255;
    }

    public ExactSearchResult search(long[] board, int depth) {
        return search(board, GameHistory.initial(board), depth, NEVER_CANCELLED);
    }

    public ExactSearchResult search(long[] board, GameHistory game, int depth, BooleanSupplier cancelled) {
        return searchWindow(board, game, depth, -INFINITY, INFINITY, cancelled);
    }

    public ExactSearchResult searchWindow(long[] board, GameHistory game, int depth,
                                          int alpha, int beta, BooleanSupplier cancelled) {
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(game, "gameHistory");
        Objects.requireNonNull(cancelled, "cancelled");
        if(board.length < Board.MAX_BITBOARDS) throw new IllegalArgumentException("Incomplete board.");
        if(depth < 0 || depth > MAX_DEPTH) throw new IllegalArgumentException("Depth must be 0.." + MAX_DEPTH);
        if(alpha < -INFINITY || beta > INFINITY || alpha >= beta) throw new IllegalArgumentException("Invalid window.");
        if(active) throw new IllegalStateException("FlatExactSearch cannot be re-entered.");
        game.requireCurrent(board);
        if(!requestActive) advanceGeneration();
        long start = System.nanoTime();
        active = true;
        nodes = 0;
        this.cancelled = cancelled;
        try {
            Arrays.fill(quietHistory, 0);
            checkpoint();
            System.arraycopy(board, 0, boards[0], 0, Board.MAX_BITBOARDS);
            history = new SearchLineHistory(game, depth);
            historyKeys[0] = SearchKey.rootHistory(board, game);
            evaluator.initialize(boards[0]);
            int score = traverse(depth, alpha, beta);
            checkpoint();
            if(depth > 0) store(SearchKey.key(boards[0], historyKeys[0]), depth, 0,
                    alpha, beta, score, pvLength[0] == 0 ? 0 : pv[0][0]);
            long[] line = Arrays.copyOf(pv[0], pvLength[0]);
            return new ExactSearchResult(depth, true, line.length == 0 ? 0 : line[0], score,
                    line, nodes, System.nanoTime() - start);
        } catch(Aborted ignored) {
            Arrays.fill(quietHistory, 0);
            return new ExactSearchResult(depth, false, 0, Value.INVALID, new long[0],
                    nodes, System.nanoTime() - start);
        } finally {
            // Recursive finally blocks pop on exceptional unwind; there is no Java
            // call stack here. Discard all real pushes even on evaluator exceptions.
            if(history != null) history.restoreRoot();
            history = null;
            this.cancelled = null;
            active = false;
        }
    }

    private int traverse(int rootDepth, int alpha, int beta) {
        int ply = 0, state = ENTER, score = 0;
        int originalAlpha = alpha, count = 0, index = 0, best = -INFINITY, flags = 0;
        long key = 0;
        search: while(true) {
            if(state == RETURN || state == LEAF_RETURN) {
                if(state == RETURN) {
                    if(ply == 0) return score;
                    --ply;
                    alpha = frameAlpha[ply];
                    beta = frameBeta[ply];
                    originalAlpha = frameOriginalAlpha[ply];
                    count = frameCount[ply];
                    index = frameIndex[ply];
                    best = frameBest[ply];
                    flags = frameFlags[ply];
                    key = frameKey[ply];
                }
                score = -score;
                if((flags & FULL_SEARCH) == 0 && score > alpha && score < beta) {
                    if(state == LEAF_RETURN) {
                        flags |= FULL_SEARCH;
                        score = leaf(ply + 1);
                        continue;
                    }
                    // Same nominal child depth and same board/evaluator/history slot.
                    // Re-enter with a fresh original window; do not make/push twice.
                    frameFlags[ply] = flags | FULL_SEARCH;
                    int childAlpha = -beta;
                    beta = -alpha;
                    alpha = childAlpha;
                    ++ply;
                    state = ENTER;
                    continue;
                }
                history.popRealPosition();
                long move = moves[ply][index];
                if(score > best) {
                    best = score;
                    if((flags & FULL_SEARCH) != 0 || score >= beta) {
                        pv[ply][0] = move;
                        int length = (flags & FULL_SEARCH) != 0 ? pvLength[ply + 1] : 0;
                        System.arraycopy(pv[ply + 1], 0, pv[ply], 1, length);
                        pvLength[ply] = 1 + length;
                    }
                }
                if(score >= beta) {
                    checkpoint();
                    QuietHistory.recordCutoff(quietHistory, moves[ply], index,
                            Board.enPassantSquare((int) boards[ply][Board.STATUS]), rootDepth - ply);
                    checkpoint();
                    if(ply != 0) store(key, rootDepth - ply, ply, originalAlpha, beta, score, move);
                    state = RETURN;
                    // Parent resumes on the next loop; no partial node evidence.
                    continue;
                }
                if(score > alpha) alpha = score;
                ++index;
                state = NEXT;
            }
            long[] board = boards[ply];
            long[] legalMoves = moves[ply];
            int status = (int) board[Board.STATUS];
            int depth = rootDepth - ply;
            if(state == ENTER) {
                checkpoint();
                nodes++;
                pvLength[ply] = 0;
                flags = 0;
                long checkers = checkers(board, status);
                if(checkers != 0) {
                    count = Gen.genEvasion(board[0], board[1], board[2], board[3], status,
                            board[Board.KEY], true, checkers, legalMoves, generatorScratch);
                } else {
                    count = Gen.genTactical(board[0], board[1], board[2], board[3], status,
                            board[Board.KEY], true, legalMoves, generatorScratch);
                    if(count == 0) count = Gen.genQuiet(board[0], board[1], board[2], board[3], status,
                            board[Board.KEY], true, legalMoves, generatorScratch);
                    else flags = QUIET_PENDING;
                }
                state = RETURN;
                if(count == 0) {
                    score = Board.isPlayerInCheck(board[0], board[1], board[2], board[3], Board.player(status))
                            ? -MATE_SCORE + ply : 0;
                    continue;
                }
                if(DrawAdjudicator.adjudicateNonTerminal(board, history) != DrawAdjudicator.RuleDraw.NONE) {
                    score = 0;
                    continue;
                }
                if(depth <= 0) {
                    score = evaluator.evaluate(board, ply);
                    if(score < -MAX_STATIC_SCORE || score > MAX_STATIC_SCORE)
                        throw new IllegalArgumentException("Static score enters the reserved mate band: " + score);
                    continue;
                }
                originalAlpha = alpha;
                key = SearchKey.key(board, historyKeys[ply]);
                long hashMove = 0;
                if(table.probe(key, entry)) {
                    hashMove = entry.hashMove;
                    if((flags & QUIET_PENDING) != 0 && hashMove != 0
                            && !CaptureHistory.isTactical(hashMove, Board.enPassantSquare(status))) {
                        count = appendQuiets(board, legalMoves, count);
                        flags = 0;
                    }
                    long data = entry.data;
                    if((data & 255) == depth && ((data >>> 10) & 255) == generation) {
                        score = TranspositionScores.fromTableScore((int) (data >> 32), ply);
                        int type = (int) (data >>> 8) & 3;
                        if(type == TTable.TYPE_EXACT || (type == TTable.TYPE_LOWER && score >= beta)
                                || (type == TTable.TYPE_UPPER && score <= alpha)) {
                            if(hashMove != 0) for(int i = 0; i < count; i++) {
                                if(legalMoves[i] == hashMove) {
                                    pv[ply][0] = hashMove;
                                    pvLength[ply] = 1;
                                    break;
                                }
                            }
                            if(ply != 0 || pvLength[ply] != 0) continue search;
                            hashMove = 0;
                        }
                    }
                }
                snapshotOrdering(board, legalMoves, count, hashMove, ply * MAX_MOVES);
                best = -INFINITY;
                index = 0;
                state = NEXT;
            }
            if(index < count || (flags & QUIET_PENDING) != 0) {
                checkpoint();
                if(index == count) {
                    int from = count;
                    count = appendQuiets(board, legalMoves, count);
                    flags &= ~QUIET_PENDING;
                    for(int j = from; j < count; j++) {
                        int evidence = quietHistory[QuietHistory.index(legalMoves[j])];
                        selectionKeys[ply * MAX_MOVES + j] = ((evidence + QuietHistory.LIMIT) << 9) | (511 - j);
                    }
                }
                if(index < count) {
                    Sort.next(legalMoves, selectionKeys, ply * MAX_MOVES, index, count);
                    long move = legalMoves[index];
                    long[] child = boards[ply + 1];
                    Board.makeMoveInto(board[0], board[1], board[2], board[3], status, board[Board.KEY], move, child);
                    evaluator.child(board, child, ply);
                    history.pushRealPosition(child);
                    historyKeys[ply + 1] = SearchKey.childHistory(historyKeys[ply],
                            history.currentKey(), (int) child[Board.STATUS]);
                    if(localLeaves && depth == 1) {
                        flags = (flags & QUIET_PENDING) | (index == 0 ? FULL_SEARCH : 0);
                        score = leaf(ply + 1);
                        state = LEAF_RETURN;
                        continue;
                    }
                    frameAlpha[ply] = alpha;
                    frameBeta[ply] = beta;
                    frameOriginalAlpha[ply] = originalAlpha;
                    frameCount[ply] = count;
                    frameIndex[ply] = index;
                    frameBest[ply] = best;
                    frameFlags[ply] = (flags & QUIET_PENDING) | (index == 0 ? FULL_SEARCH : 0);
                    frameKey[ply] = key;
                    int childAlpha = index == 0 ? -beta : -alpha - 1;
                    beta = -alpha;
                    alpha = childAlpha;
                    ++ply;
                    state = ENTER;
                    continue;
                }
            }
            checkpoint();
            score = best;
            if(ply != 0) store(key, depth, ply, originalAlpha, beta, score, pv[ply][0]);
            state = RETURN;
        }
    }

    /** Static-only entry, never descending or touching TT; repeated for a PVS re-search. */
    private int leaf(int ply) {
        checkpoint();
        nodes++;
        pvLength[ply] = 0;
        long[] board = boards[ply], legalMoves = moves[ply];
        int status = (int) board[Board.STATUS];
        long checkers = checkers(board, status);
        int count;
        if(checkers != 0) {
            count = Gen.genEvasion(board[0], board[1], board[2], board[3], status,
                    board[Board.KEY], true, checkers, legalMoves, generatorScratch);
        } else {
            count = Gen.genTactical(board[0], board[1], board[2], board[3], status,
                    board[Board.KEY], true, legalMoves, generatorScratch);
            if(count == 0) count = Gen.genQuiet(board[0], board[1], board[2], board[3], status,
                    board[Board.KEY], true, legalMoves, generatorScratch);
        }
        if(count == 0) return Board.isPlayerInCheck(board[0], board[1], board[2], board[3], Board.player(status))
                ? -MATE_SCORE + ply : 0;
        if(DrawAdjudicator.adjudicateNonTerminal(board, history) != DrawAdjudicator.RuleDraw.NONE) return 0;
        int score = evaluator.evaluate(board, ply);
        if(score < -MAX_STATIC_SCORE || score > MAX_STATIC_SCORE)
            throw new IllegalArgumentException("Static score enters the reserved mate band: " + score);
        return score;
    }

    private static long checkers(long[] board, int status) {
        int player = Board.player(status);
        long color = ~(-(long) player ^ board[3]);
        return Board.getCheckersPext(board[0], board[1], board[2], board[3], color, player,
                Long.numberOfTrailingZeros(board[0] & ~board[1] & ~board[2] & color), board[0] | board[1] | board[2]);
    }

    private int appendQuiets(long[] board, long[] list, int count) {
        int quiet = Gen.genQuiet(board[0], board[1], board[2], board[3], (int) board[Board.STATUS],
                board[Board.KEY], true, quietScratch, generatorScratch);
        System.arraycopy(quietScratch, 0, list, count, quiet);
        return count + quiet;
    }

    private void snapshotOrdering(long[] board, long[] list, int count, long hashMove, int base) {
        int ep = Board.enPassantSquare((int) board[Board.STATUS]);
        for(int i = 0; i < count; i++) {
            long move = list[i];
            if(move == hashMove) { selectionKeys[base + i] = Integer.MAX_VALUE - i; continue; }
            int group = 0;
            int evidence;
            if(CaptureHistory.isTactical(move, ep)) {
                group = See.atLeastGeneratedLegal(board, move, 0) ? 2 : 1;
                evidence = ExactSearch.tacticalMaterialValue(move, ep);
            } else evidence = quietHistory[QuietHistory.index(move)];
            selectionKeys[base + i] = (group << 25) | ((evidence + QuietHistory.LIMIT) << 9) | (511 - i);
        }
    }

    private void store(long key, int depth, int ply, int alpha, int beta, int score, long move) {
        if(depth > 255) return;
        int type = score <= alpha ? TTable.TYPE_UPPER : score >= beta ? TTable.TYPE_LOWER : TTable.TYPE_EXACT;
        table.save(key, depth, type, TranspositionScores.toTableScore(score, ply), move);
    }

    private void checkpoint() {
        if(Thread.currentThread().isInterrupted() || cancelled.getAsBoolean()) throw ABORTED;
    }

    private static final Aborted ABORTED = new Aborted();
    private static final class Aborted extends RuntimeException {
        private Aborted() { super(null, null, false, false); }
    }
}
