package com.ohinteractive.seedv6.core;

import java.util.Objects;

import com.ohinteractive.seedv6.core.util.Bitboard;
import com.ohinteractive.seedv6.core.util.Pext;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;

/**
 * Exact static exchange evaluation for a legal V6 capture or promotion.
 *
 * <p>The integer result is the material gain for the side making {@code move},
 * using {@link Eval#exchangeValue(int)}. Positive is favourable, zero is an
 * equal exchange, and negative is unfavourable. The supplied board is not
 * modified. After the candidate move, both sides choose optimally between
 * stopping and making a legal capture onto the candidate destination square.
 * Ordinary quiet moves and castling are outside this contract; non-capturing
 * promotions are included.</p>
 */
public final class See {

    private static final long[] KNIGHT_ATTACKS = Bitboard.BB[Bitboard.LEAP_ATTACKS];
    private static final long[] KING_ATTACKS = Bitboard.BB[Bitboard.KING_ATTACKS];
    private static final long[][] PAWN_ATTACKS = {
        Bitboard.BB[Bitboard.PAWN_ATTACKS_PLAYER0],
        Bitboard.BB[Bitboard.PAWN_ATTACKS_PLAYER1]
    };

    /** Evaluate one legal capture, en-passant capture, or promotion. */
    public static int evaluate(long[] board, long move) {
        Objects.requireNonNull(board, "board");
        if (board.length < Board.MAX_BITBOARDS) {
            throw new IllegalArgumentException(
                    "Board must contain at least " + Board.MAX_BITBOARDS + " longs.");
        }
        if (move == 0L) {
            throw new IllegalArgumentException("SEE requires a non-zero move.");
        }

        long board0 = board[0];
        long board1 = board[1];
        long board2 = board[2];
        long board3 = board[3];
        int status = Math.toIntExact(board[Board.STATUS]);
        int mover = status & Board.PLAYER_BIT;
        int fromSquare = (int) move & Board.SQUARE_BITS;
        int targetSquare = (int) (move >>> Board.TARGET_SQUARE_SHIFT) & Board.SQUARE_BITS;
        if (fromSquare == targetSquare) {
            throw new IllegalArgumentException("SEE move must change squares.");
        }

        int movingPiece = Board.getSquare(board0, board1, board2, board3, fromSquare);
        int encodedMovingPiece = (int) (move >>> Board.START_PIECE_SHIFT) & Board.PIECE_BITS;
        if (movingPiece == Value.NONE || movingPiece != encodedMovingPiece
                || (movingPiece >>> Board.PLAYER_SHIFT) != mover) {
            throw new IllegalArgumentException("SEE move does not encode the moving piece.");
        }

        int movingType = movingPiece & Piece.TYPE;
        int targetPiece = Board.getSquare(board0, board1, board2, board3, targetSquare);
        int encodedTargetPiece = (int) (move >>> Board.TARGET_PIECE_SHIFT) & Board.PIECE_BITS;
        if (targetPiece != encodedTargetPiece) {
            throw new IllegalArgumentException("SEE move does not encode the target piece.");
        }

        int capturedType = Value.NONE;
        boolean enPassant = false;
        if (targetPiece != Value.NONE) {
            if ((targetPiece >>> Board.PLAYER_SHIFT) == mover
                    || (targetPiece & Piece.TYPE) == Piece.KING) {
                throw new IllegalArgumentException("SEE target is not a capturable enemy piece.");
            }
            capturedType = targetPiece & Piece.TYPE;
        } else if (movingType == Piece.PAWN
                && targetSquare == Board.enPassantSquare(status)) {
            int captureSquare = targetSquare + (mover == Value.WHITE ? -8 : 8);
            int capturedPiece = Board.getSquare(board0, board1, board2, board3, captureSquare);
            if ((capturedPiece & Piece.TYPE) != Piece.PAWN
                    || (capturedPiece >>> Board.PLAYER_SHIFT) == mover) {
                throw new IllegalArgumentException("SEE move has no en-passant pawn to capture.");
            }
            capturedType = Piece.PAWN;
            enPassant = true;
        }

        int promotePiece = (int) (move >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS;
        int promotionType = promotePiece & Piece.TYPE;
        boolean promotion = promotePiece != Value.NONE;
        if (promotion) {
            if (movingType != Piece.PAWN
                    || (promotePiece >>> Board.PLAYER_SHIFT) != mover
                    || !isPromotionType(promotionType)
                    || !isPromotionRank(targetSquare, mover)) {
                throw new IllegalArgumentException("SEE move has an invalid promotion.");
            }
        } else if (movingType == Piece.PAWN && isPromotionRank(targetSquare, mover)) {
            throw new IllegalArgumentException("SEE move omits a required promotion piece.");
        }

        if (capturedType == Value.NONE && !promotion) {
            throw new IllegalArgumentException(
                    "SEE supports captures and promotions, not ordinary quiet moves.");
        }
        if (enPassant && promotion) {
            throw new IllegalArgumentException("En passant cannot also be a promotion.");
        }

        long[] after = new long[Board.MAX_BITBOARDS];
        Board.makeMoveInto(board0, board1, board2, board3, status, board[Board.KEY], move, after);
        if (Board.isPlayerInCheckPext(after[0], after[1], after[2], after[3], mover)) {
            throw new IllegalArgumentException("SEE move is not legal for the supplied position.");
        }

        int pieceOnTargetType = promotion ? promotionType : movingType;
        int actualTargetType = Board.getSquare(after[0], after[1], after[2], after[3], targetSquare)
                & Piece.TYPE;
        if (actualTargetType != pieceOnTargetType) {
            throw new IllegalArgumentException("SEE move does not produce the encoded target piece.");
        }

        int initialGain = exchangeValueOrZero(capturedType)
                + (promotion ? Eval.exchangeValue(promotionType)
                        - Eval.exchangeValue(Piece.PAWN) : 0);
        int reply = bestContinuation(after[0], after[1], after[2], after[3],
                targetSquare, 1 ^ mover, pieceOnTargetType);
        return initialGain - reply;
    }

    /**
     * Evaluate a tactical move supplied by the current authoritative direct-legal
     * generator without allocating or constructing full Board state.
     *
     * <p>This is a search-internal production entry, not a general validation
     * API. The caller must supply a legal generated capture, en-passant capture,
     * or promotion for {@code board}, plus reusable scratch with at least four
     * elements. Slots zero through three are overwritten with the exact
     * post-candidate piece planes; status and key state are deliberately not
     * constructed because the continuation reads only those planes.</p>
     */
    public static int evaluateGeneratedLegal(long[] board, long move, long[] piecePlaneScratch) {
        final long board0 = board[0];
        final long board1 = board[1];
        final long board2 = board[2];
        final long board3 = board[3];
        final int status = Math.toIntExact(board[Board.STATUS]);
        final int mover = status & Board.PLAYER_BIT;
        final int fromSquare = (int) move & Board.SQUARE_BITS;
        final int targetSquare = (int) (move >>> Board.TARGET_SQUARE_SHIFT)
                & Board.SQUARE_BITS;
        final int movingType = (int) (move >>> Board.START_PIECE_SHIFT)
                & Board.PIECE_BITS & Piece.TYPE;
        int capturedType = (int) (move >>> Board.TARGET_PIECE_SHIFT)
                & Board.PIECE_BITS & Piece.TYPE;
        final int promotionType = (int) (move >>> Board.PROMOTE_PIECE_SHIFT)
                & Board.PIECE_BITS & Piece.TYPE;
        final boolean promotion = promotionType != Value.NONE;
        final int pieceOnTargetType = promotion ? promotionType : movingType;

        final long targetBit = 1L << targetSquare;
        long cleared = (1L << fromSquare) | targetBit;
        if (capturedType == Value.NONE && movingType == Piece.PAWN
                && targetSquare == Board.enPassantSquare(status)) {
            final int captureSquare = targetSquare + (mover == Value.WHITE ? -8 : 8);
            cleared |= 1L << captureSquare;
            capturedType = Piece.PAWN;
        }

        piecePlaneScratch[0] = (board0 & ~cleared)
                | (-(long) (pieceOnTargetType & 1) & targetBit);
        piecePlaneScratch[1] = (board1 & ~cleared)
                | (-(long) (pieceOnTargetType >>> 1 & 1) & targetBit);
        piecePlaneScratch[2] = (board2 & ~cleared)
                | (-(long) (pieceOnTargetType >>> 2 & 1) & targetBit);
        piecePlaneScratch[3] = (board3 & ~cleared)
                | (mover == Value.BLACK ? targetBit : 0L);

        final int initialGain = exchangeValueOrZero(capturedType)
                + (promotion ? Eval.exchangeValue(promotionType)
                        - Eval.exchangeValue(Piece.PAWN) : 0);
        final int reply = bestContinuation(
                piecePlaneScratch[0], piecePlaneScratch[1], piecePlaneScratch[2],
                piecePlaneScratch[3], targetSquare, 1 ^ mover, pieceOnTargetType);
        return initialGain - reply;
    }

    /**
     * Whether a generated legal tactical move has SEE >= {@code threshold}.
     * Accepts captures, en passant and all promotions, including non-capturing
     * underpromotions; ordinary quiet moves and castling are outside the contract.
     * Like {@link #evaluateGeneratedLegal}, this entry trusts the generator rather
     * than validating the initial move. The board is unchanged and no scratch or
     * heap allocation is needed. Every int threshold is supported.
     *
     * <p>Both sides may stop the exchange for zero, even when checked, as in the
     * numeric SEE contract. Only legal recaptures onto this destination compete
     * with stopping. This is local exchange accounting, not a search of evasions.</p>
     */
    public static boolean atLeastGeneratedLegal(long[] board, long move, int threshold) {
        final int status = (int) board[Board.STATUS];
        final int mover = status & Board.PLAYER_BIT;
        final int fromSquare = (int) move & Board.SQUARE_BITS;
        final int targetSquare = (int) (move >>> Board.TARGET_SQUARE_SHIFT) & Board.SQUARE_BITS;
        final int movingType = (int) (move >>> Board.START_PIECE_SHIFT) & Piece.TYPE;
        int capturedType = (int) (move >>> Board.TARGET_PIECE_SHIFT) & Piece.TYPE;
        final int promotionType = (int) (move >>> Board.PROMOTE_PIECE_SHIFT) & Piece.TYPE;
        final int resultingType = promotionType == Value.NONE ? movingType : promotionType;
        final long targetBit = 1L << targetSquare;
        long cleared = (1L << fromSquare) | targetBit;
        if (capturedType == Value.NONE && movingType == Piece.PAWN
                && targetSquare == Board.enPassantSquare(status)) {
            cleared |= 1L << (targetSquare + (mover == Value.WHITE ? -8 : 8));
            capturedType = Piece.PAWN;
        }
        final int gain = exchangeValueOrZero(capturedType)
                + (promotionType == Value.NONE ? 0
                : Eval.exchangeValue(promotionType) - Eval.exchangeValue(Piece.PAWN));
        // The opponent can decline every recapture, so SEE cannot exceed gain.
        if (gain < threshold) return false;

        final long next0 = (board[0] & ~cleared) | (-(long) (resultingType & 1) & targetBit);
        final long next1 = (board[1] & ~cleared) | (-(long) (resultingType >>> 1 & 1) & targetBit);
        final long next2 = (board[2] & ~cleared) | (-(long) (resultingType >>> 2 & 1) & targetBit);
        final long next3 = (board[3] & ~cleared) | (mover == Value.BLACK ? targetBit : 0L);
        // gain - reply >= threshold iff reply < gain - threshold + 1.
        // Widen before subtraction, including for Integer.MIN_VALUE thresholds.
        return !continuationAtLeast(next0, next1, next2, next3, targetSquare,
                1 ^ mover, resultingType, (long) gain - threshold + 1);
    }

    /**
     * E = max(0, gain - E(child)) over all legal recaptures. For integer scores,
     * a branch reaches t iff its child cannot reach gain - t + 1. Each capture
     * removes one piece, so this boolean recurrence terminates without needing
     * an exact exchange value or a greedy least-valuable-attacker assumption.
     */
    private static boolean continuationAtLeast(long board0, long board1, long board2, long board3,
                                               int targetSquare, int player, int targetType,
                                               long threshold) {
        if (threshold <= 0) return true; // Optional stopping.
        if (targetType == Piece.KING) return false; // Kings cannot be captured.
        final int captureValue = Eval.exchangeValue(targetType);
        final boolean promotionRank = isPromotionRank(targetSquare, player);
        final int maximumGain = captureValue + (promotionRank
                ? Eval.exchangeValue(Piece.QUEEN) - Eval.exchangeValue(Piece.PAWN) : 0);
        if (threshold > maximumGain) return false; // Child exchange gain is nonnegative.

        final long allOccupancy = board0 | board1 | board2;
        final long playerPieces = allOccupancy & (player == Value.WHITE ? ~board3 : board3);
        final long queens = ~board0 & board1 & ~board2 & playerPieces;
        final long rooks = board0 & board1 & ~board2 & playerPieces;
        final long bishops = ~board0 & ~board1 & board2 & playerPieces;
        long attackers = (KNIGHT_ATTACKS[targetSquare] & board0 & ~board1 & board2 & playerPieces)
                | (PAWN_ATTACKS[1 ^ player][targetSquare] & ~board0 & board1 & board2 & playerPieces)
                | (KING_ATTACKS[targetSquare] & board0 & ~board1 & ~board2 & playerPieces)
                | (Pext.bishopMoves(targetSquare, allOccupancy) & (bishops | queens))
                | (Pext.rookMoves(targetSquare, allOccupancy) & (rooks | queens));
        while (attackers != 0L) {
            final long fromBit = attackers & -attackers;
            attackers ^= fromBit;
            final int attackerType = Board.getSquare(board0, board1, board2, board3, bitScan(fromBit))
                    & Piece.TYPE;
            if (attackerType == Piece.PAWN && promotionRank) {
                final int baseGain = captureValue - Eval.exchangeValue(Piece.PAWN);
                if (captureAtLeast(board0, board1, board2, board3, fromBit, targetSquare,
                        player, Piece.QUEEN, baseGain + Eval.exchangeValue(Piece.QUEEN), threshold)
                        || captureAtLeast(board0, board1, board2, board3, fromBit, targetSquare,
                        player, Piece.ROOK, baseGain + Eval.exchangeValue(Piece.ROOK), threshold)
                        || captureAtLeast(board0, board1, board2, board3, fromBit, targetSquare,
                        player, Piece.BISHOP, baseGain + Eval.exchangeValue(Piece.BISHOP), threshold)
                        || captureAtLeast(board0, board1, board2, board3, fromBit, targetSquare,
                        player, Piece.KNIGHT, baseGain + Eval.exchangeValue(Piece.KNIGHT), threshold)) {
                    return true;
                }
            } else if (captureAtLeast(board0, board1, board2, board3, fromBit, targetSquare,
                    player, attackerType, captureValue, threshold)) {
                return true;
            }
        }
        return false;
    }

    private static boolean captureAtLeast(long board0, long board1, long board2, long board3,
                                         long fromBit, int targetSquare, int player,
                                         int resultingType, int gain, long threshold) {
        if (gain < threshold) return false;
        final long targetBit = 1L << targetSquare;
        final long changed = fromBit | targetBit;
        final long next0 = (board0 & ~changed) | (-(long) (resultingType & 1) & targetBit);
        final long next1 = (board1 & ~changed) | (-(long) (resultingType >>> 1 & 1) & targetBit);
        final long next2 = (board2 & ~changed) | (-(long) (resultingType >>> 2 & 1) & targetBit);
        final long next3 = (board3 & ~changed) | (player == Value.BLACK ? targetBit : 0L);
        // Recheck after occupancy changes: pins, discovered checks and king captures.
        if (Board.isPlayerInCheckPext(next0, next1, next2, next3, player)) return false;
        return !continuationAtLeast(next0, next1, next2, next3, targetSquare,
                1 ^ player, resultingType, gain - threshold + 1);
    }

    /**
     * Return the best optional exchange gain for {@code player}. Each recursive
     * capture removes one piece, so valid chess positions bound the recursion.
     */
    private static int bestContinuation(long board0, long board1, long board2, long board3,
                                        int targetSquare, int player, int targetType) {
        if (targetType == Piece.KING) {
            return 0;
        }

        long allOccupancy = board0 | board1 | board2;
        long playerPieces = allOccupancy & (player == Value.WHITE ? ~board3 : board3);
        long queens = ~board0 & board1 & ~board2 & playerPieces;
        long rooks = board0 & board1 & ~board2 & playerPieces;
        long bishops = ~board0 & ~board1 & board2 & playerPieces;
        long attackers = (KNIGHT_ATTACKS[targetSquare]
                & board0 & ~board1 & board2 & playerPieces)
                | (PAWN_ATTACKS[1 ^ player][targetSquare]
                & ~board0 & board1 & board2 & playerPieces)
                | (KING_ATTACKS[targetSquare]
                & board0 & ~board1 & ~board2 & playerPieces)
                | (Pext.bishopMoves(targetSquare, allOccupancy) & (bishops | queens))
                | (Pext.rookMoves(targetSquare, allOccupancy) & (rooks | queens));

        int best = 0;
        while (attackers != 0L) {
            long fromBit = attackers & -attackers;
            attackers ^= fromBit;
            int fromSquare = bitScan(fromBit);
            int attackerType = Board.getSquare(board0, board1, board2, board3, fromSquare)
                    & Piece.TYPE;
            if (attackerType == Piece.PAWN && isPromotionRank(targetSquare, player)) {
                best = Math.max(best, captureGain(board0, board1, board2, board3,
                        fromBit, targetSquare, player, targetType, attackerType, Piece.QUEEN));
                best = Math.max(best, captureGain(board0, board1, board2, board3,
                        fromBit, targetSquare, player, targetType, attackerType, Piece.ROOK));
                best = Math.max(best, captureGain(board0, board1, board2, board3,
                        fromBit, targetSquare, player, targetType, attackerType, Piece.BISHOP));
                best = Math.max(best, captureGain(board0, board1, board2, board3,
                        fromBit, targetSquare, player, targetType, attackerType, Piece.KNIGHT));
            } else {
                best = Math.max(best, captureGain(board0, board1, board2, board3,
                        fromBit, targetSquare, player, targetType, attackerType, attackerType));
            }
        }
        return best;
    }

    private static int captureGain(long board0, long board1, long board2, long board3,
                                   long fromBit, int targetSquare, int player,
                                   int targetType, int attackerType, int resultingType) {
        long targetBit = 1L << targetSquare;
        long changed = fromBit | targetBit;
        long next0 = (board0 & ~changed) | (-(long) (resultingType & 1) & targetBit);
        long next1 = (board1 & ~changed) | (-(long) (resultingType >>> 1 & 1) & targetBit);
        long next2 = (board2 & ~changed) | (-(long) (resultingType >>> 2 & 1) & targetBit);
        long next3 = (board3 & ~changed) | (player == Value.BLACK ? targetBit : 0L);
        if (Board.isPlayerInCheckPext(next0, next1, next2, next3, player)) {
            return Integer.MIN_VALUE;
        }

        int promotionGain = attackerType == Piece.PAWN && resultingType != Piece.PAWN
                ? Eval.exchangeValue(resultingType) - Eval.exchangeValue(Piece.PAWN)
                : 0;
        return Eval.exchangeValue(targetType) + promotionGain
                - bestContinuation(next0, next1, next2, next3,
                        targetSquare, 1 ^ player, resultingType);
    }

    private static int exchangeValueOrZero(int pieceType) {
        return pieceType == Value.NONE ? 0 : Eval.exchangeValue(pieceType);
    }

    private static boolean isPromotionType(int pieceType) {
        return pieceType == Piece.QUEEN || pieceType == Piece.ROOK
                || pieceType == Piece.BISHOP || pieceType == Piece.KNIGHT;
    }

    private static boolean isPromotionRank(int square, int player) {
        return (square >>> 3) == (player == Value.WHITE ? 7 : 0);
    }

    private static int bitScan(long singleBit) {
        return Board.LSB[(int) ((singleBit * Board.DB) >>> 58)];
    }

    private See() {}
}
