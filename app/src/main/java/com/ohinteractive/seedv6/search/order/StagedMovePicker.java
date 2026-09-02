package com.ohinteractive.seedv6.search.order;

import java.util.Arrays;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.See;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;

/**
 * Allocation-free-after-construction staged picker over authoritative V6 legal
 * generation. One instance is intended to be confined to one search worker.
 *
 * <p>The stages are: exact legal hash move; non-losing tactical moves; two
 * exact legal quiet killers; remaining quiet moves; losing tactical moves.
 * Checked positions are generated once through {@link Gen#genEvasion} and then
 * classified into those stages. Every score lives in sidecar storage and the
 * complete move value remains opaque and unchanged.</p>
 */
public final class StagedMovePicker {

    /** Existing production search buffers use this complete legal-move capacity. */
    public static final int MAX_MOVES = 256;
    public static final long NO_MOVE = 0L;

    private static final byte EMITTED = -1;
    private static final byte UNSCORED_TACTICAL = 0;
    private static final byte NON_LOSING_TACTICAL = 1;
    private static final byte QUIET = 2;
    private static final byte LOSING_TACTICAL = 3;

    private static final int STAGE_HASH = 0;
    private static final int STAGE_SCORE_TACTICALS = 1;
    private static final int STAGE_NON_LOSING_TACTICAL = 2;
    private static final int STAGE_FIRST_KILLER = 3;
    private static final int STAGE_SECOND_KILLER = 4;
    private static final int STAGE_QUIET = 5;
    private static final int STAGE_LOSING_TACTICAL = 6;
    private static final int STAGE_DONE = 7;

    private final MoveOrdering ordering;
    private final int maxPly;
    private final long[][] moves;
    private final int[][] primaryScores;
    private final int[][] secondaryScores;
    private final byte[][] categories;
    private final int[] moveCounts;
    private final int[] stages;
    private final int[] seeCounts;
    private final long[] hashMoves;
    private final long[] firstKillers;
    private final long[] secondKillers;
    private final long[][] preparedBoards;
    private final boolean[] prepared;
    private final boolean[] checked;
    private final long[] generationBuffer = new long[MAX_MOVES];
    private final long[] generationScratch = new long[Board.MAX_BITBOARDS];

    StagedMovePicker(MoveOrdering ordering, int maxPly) {
        this.ordering = ordering;
        this.maxPly = maxPly;
        moves = new long[maxPly][MAX_MOVES];
        primaryScores = new int[maxPly][MAX_MOVES];
        secondaryScores = new int[maxPly][MAX_MOVES];
        categories = new byte[maxPly][MAX_MOVES];
        moveCounts = new int[maxPly];
        stages = new int[maxPly];
        seeCounts = new int[maxPly];
        hashMoves = new long[maxPly];
        firstKillers = new long[maxPly];
        secondKillers = new long[maxPly];
        preparedBoards = new long[maxPly][];
        prepared = new boolean[maxPly];
        checked = new boolean[maxPly];
    }

    /** Generate and minimally classify one node. No hash hint is legality proof. */
    public int prepare(long[] board, int ply, long hashMove) {
        MoveOrdering.requireBoard(board);
        final int status = Math.toIntExact(board[Board.STATUS]);
        return prepare(board, ply, hashMove, computeCheckers(board, status), true);
    }

    /**
     * Generate the qsearch move set: every legal evasion when checked, otherwise
     * legal tactical moves only. The supplied checker set must have been
     * computed for this exact board before any stand-pat decision.
     */
    public int prepareQuiescence(long[] board, int ply, long hashMove, long checkers) {
        return prepare(board, ply, hashMove, checkers, false);
    }

    /**
     * Release one reusable per-ply picker frame after a consumer completes or
     * unwinds. History and killer state owned by {@link MoveOrdering} is not
     * changed.
     */
    public void clearPly(int ply) {
        requirePly(ply);
        moveCounts[ply] = 0;
        stages[ply] = STAGE_DONE;
        hashMoves[ply] = NO_MOVE;
        firstKillers[ply] = NO_MOVE;
        secondKillers[ply] = NO_MOVE;
        preparedBoards[ply] = null;
        prepared[ply] = false;
        checked[ply] = false;
    }

    private int prepare(
        long[] board, int ply, long hashMove, long checkers, boolean includeQuiets
    ) {
        MoveOrdering.requireBoard(board);
        requirePly(ply);

        final long[] nodeMoves = moves[ply];
        final int status = Math.toIntExact(board[Board.STATUS]);
        final boolean inCheck = checkers != 0L;
        final int tacticalCount;
        final int moveCount;
        if(inCheck) {
            moveCount = Gen.genEvasion(
                board[0], board[1], board[2], board[3], status, board[Board.KEY], true,
                checkers, nodeMoves, generationScratch
            );
            tacticalCount = -1;
        } else {
            tacticalCount = Gen.genTactical(
                board[0], board[1], board[2], board[3], status, board[Board.KEY], true,
                nodeMoves, generationScratch
            );
            final int quietCount = includeQuiets ? Gen.genQuiet(
                board[0], board[1], board[2], board[3], status, board[Board.KEY], true,
                generationBuffer, generationScratch
            ) : 0;
            if(tacticalCount + quietCount > MAX_MOVES) {
                throw new IllegalStateException(
                    "Authoritative legal move count exceeds picker capacity " + MAX_MOVES
                        + ": tactical=" + tacticalCount + ", quiet=" + quietCount
                );
            }
            System.arraycopy(generationBuffer, 0, nodeMoves, tacticalCount, quietCount);
            moveCount = tacticalCount + quietCount;
        }

        for(int index = 0; index < moveCount; index ++) {
            final long move = nodeMoves[index];
            final boolean tactical = inCheck
                ? MoveOrdering.isTactical(board, move)
                : index < tacticalCount;
            if(tactical) {
                categories[ply][index] = UNSCORED_TACTICAL;
            } else {
                primaryScores[ply][index] = ordering.historyScore(move);
                secondaryScores[ply][index] = 0;
                categories[ply][index] = QUIET;
            }
        }

        moveCounts[ply] = moveCount;
        stages[ply] = STAGE_HASH;
        seeCounts[ply] = 0;
        hashMoves[ply] = hashMove;
        firstKillers[ply] = ordering.killer(ply, 0);
        secondKillers[ply] = ordering.killer(ply, 1);
        preparedBoards[ply] = board;
        checked[ply] = inCheck;
        prepared[ply] = true;
        return moveCount;
    }

    /** Return the next complete move value, or {@link #NO_MOVE} after exhaustion. */
    public long next(int ply) {
        requirePrepared(ply);
        while(true) {
            switch(stages[ply]) {
                case STAGE_HASH: {
                    stages[ply] = STAGE_SCORE_TACTICALS;
                    final long move = emitExact(ply, hashMoves[ply], (byte) 0);
                    if(move != NO_MOVE) return move;
                    break;
                }
                case STAGE_SCORE_TACTICALS:
                    prepareTacticalScores(ply);
                    stages[ply] = STAGE_NON_LOSING_TACTICAL;
                    break;
                case STAGE_NON_LOSING_TACTICAL: {
                    final long move = emitBest(ply, NON_LOSING_TACTICAL);
                    if(move != NO_MOVE) return move;
                    stages[ply] = STAGE_FIRST_KILLER;
                    break;
                }
                case STAGE_FIRST_KILLER: {
                    stages[ply] = STAGE_SECOND_KILLER;
                    final long move = emitExact(ply, firstKillers[ply], QUIET);
                    if(move != NO_MOVE) return move;
                    break;
                }
                case STAGE_SECOND_KILLER: {
                    stages[ply] = STAGE_QUIET;
                    final long move = emitExact(ply, secondKillers[ply], QUIET);
                    if(move != NO_MOVE) return move;
                    break;
                }
                case STAGE_QUIET: {
                    final long move = emitBest(ply, QUIET);
                    if(move != NO_MOVE) return move;
                    stages[ply] = STAGE_LOSING_TACTICAL;
                    break;
                }
                case STAGE_LOSING_TACTICAL: {
                    final long move = emitBest(ply, LOSING_TACTICAL);
                    if(move != NO_MOVE) return move;
                    stages[ply] = STAGE_DONE;
                    break;
                }
                case STAGE_DONE:
                    return NO_MOVE;
                default:
                    throw new IllegalStateException("Unexpected move-picker stage: " + stages[ply]);
            }
        }
    }

    public int moveCount(int ply) {
        requirePrepared(ply);
        return moveCounts[ply];
    }

    /** SEE calls made by the current or most recently cleared preparation. */
    public int seeEvaluationCount(int ply) {
        requirePly(ply);
        return seeCounts[ply];
    }

    public boolean inCheck(int ply) {
        requirePrepared(ply);
        return checked[ply];
    }

    /** Diagnostics-only exact membership check over this authoritative legal set. */
    public boolean containsLegalMove(int ply, long move) {
        requirePrepared(ply);
        if(move == NO_MOVE) return false;
        final int count = moveCounts[ply];
        for(int index = 0; index < count; index ++) {
            if(moves[ply][index] == move) return true;
        }
        return false;
    }

    void reset() {
        Arrays.fill(moveCounts, 0);
        Arrays.fill(stages, STAGE_DONE);
        Arrays.fill(seeCounts, 0);
        Arrays.fill(hashMoves, NO_MOVE);
        Arrays.fill(firstKillers, NO_MOVE);
        Arrays.fill(secondKillers, NO_MOVE);
        Arrays.fill(preparedBoards, null);
        Arrays.fill(prepared, false);
        Arrays.fill(checked, false);
    }

    private void prepareTacticalScores(int ply) {
        final long[] board = preparedBoards[ply];
        final int status = Math.toIntExact(board[Board.STATUS]);
        final int count = moveCounts[ply];
        int evaluated = 0;
        for(int index = 0; index < count; index ++) {
            if(categories[ply][index] != UNSCORED_TACTICAL) continue;

            final long move = moves[ply][index];
            final int see = See.evaluate(board, move);
            primaryScores[ply][index] = see;
            secondaryScores[ply][index] = tacticalTieScore(move, status);
            categories[ply][index] = see >= 0
                ? NON_LOSING_TACTICAL : LOSING_TACTICAL;
            evaluated ++;
        }
        seeCounts[ply] = evaluated;
    }

    private long emitExact(int ply, long target, byte requiredCategory) {
        if(target == NO_MOVE) return NO_MOVE;
        final int count = moveCounts[ply];
        for(int index = 0; index < count; index ++) {
            final byte category = categories[ply][index];
            if(category != EMITTED && (requiredCategory == 0 || category == requiredCategory)
                && moves[ply][index] == target) {
                categories[ply][index] = EMITTED;
                return target;
            }
        }
        return NO_MOVE;
    }

    private long emitBest(int ply, byte requiredCategory) {
        int best = -1;
        final int count = moveCounts[ply];
        for(int index = 0; index < count; index ++) {
            if(categories[ply][index] != requiredCategory) continue;
            if(best == -1 || comesBefore(ply, index, best)) best = index;
        }
        if(best == -1) return NO_MOVE;
        categories[ply][best] = EMITTED;
        return moves[ply][best];
    }

    private boolean comesBefore(int ply, int candidate, int incumbent) {
        final int primaryComparison = Integer.compare(
            primaryScores[ply][candidate], primaryScores[ply][incumbent]
        );
        if(primaryComparison != 0) return primaryComparison > 0;
        final int secondaryComparison = Integer.compare(
            secondaryScores[ply][candidate], secondaryScores[ply][incumbent]
        );
        if(secondaryComparison != 0) return secondaryComparison > 0;
        return Long.compareUnsigned(moves[ply][candidate], moves[ply][incumbent]) < 0;
    }

    private static int tacticalTieScore(long move, int status) {
        final int promotionType = (int) (move >>> Board.PROMOTE_PIECE_SHIFT)
            & Board.PIECE_BITS & Piece.TYPE;
        final int promotionGain = promotionType == Value.NONE ? 0
            : Eval.exchangeValue(promotionType) - Eval.exchangeValue(Piece.PAWN);

        final int targetType = (int) (move >>> Board.TARGET_PIECE_SHIFT)
            & Board.PIECE_BITS & Piece.TYPE;
        final int victimValue;
        if(targetType != Value.NONE) {
            victimValue = Eval.exchangeValue(targetType);
        } else if(isEnPassant(move, status)) {
            victimValue = Eval.exchangeValue(Piece.PAWN);
        } else {
            victimValue = 0;
        }
        final int attackerType = (int) (move >>> Board.START_PIECE_SHIFT)
            & Board.PIECE_BITS & Piece.TYPE;

        // Lexicographic promotion gain, victim value, then lower attacker value.
        // V6 piece type codes already increase from king/queen through pawn.
        return promotionGain * 16_384 + victimValue * 8 + attackerType;
    }

    private static boolean isEnPassant(long move, int status) {
        final int startPiece = (int) (move >>> Board.START_PIECE_SHIFT) & Board.PIECE_BITS;
        return (startPiece & Piece.TYPE) == Piece.PAWN
            && ((int) (move >>> Board.TARGET_PIECE_SHIFT) & Board.PIECE_BITS) == Value.NONE
            && ((int) (move >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS) == Value.NONE
            && ((int) move & Value.FILE) != ((int) (move >>> Board.TARGET_SQUARE_SHIFT) & Value.FILE)
            && ((int) (move >>> Board.TARGET_SQUARE_SHIFT) & Board.SQUARE_BITS)
                == Board.enPassantSquare(status);
    }

    private static long computeCheckers(long[] board, int status) {
        final int player = status & Board.PLAYER_BIT;
        final long allOccupancy = board[0] | board[1] | board[2];
        final long colorMask = ~(-(long) player ^ board[3]);
        final long king = board[0] & ~board[1] & ~board[2] & colorMask;
        final int kingSquare = Long.numberOfTrailingZeros(king);
        return Board.getCheckersPext(
            board[0], board[1], board[2], board[3], colorMask, player,
            kingSquare, allOccupancy
        );
    }

    private void requirePrepared(int ply) {
        requirePly(ply);
        if(!prepared[ply]) {
            throw new IllegalStateException("Move picker has not been prepared at ply " + ply);
        }
    }

    private void requirePly(int ply) {
        if(ply < 0 || ply >= maxPly) {
            throw new IndexOutOfBoundsException(
                "Ply out of range: " + ply + " (configured maximum " + maxPly + ")"
            );
        }
    }
}
