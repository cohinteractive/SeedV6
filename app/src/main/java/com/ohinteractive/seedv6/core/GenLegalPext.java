package com.ohinteractive.seedv6.core;

import com.ohinteractive.seedv6.core.util.Bitboard;
import com.ohinteractive.seedv6.core.util.Pext;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;

/*
 * Experimental direct-legal counterpart to Gen.
 *
 * The public signatures deliberately match Gen so callers can compare the two
 * implementations without changing move buffers or move encoding. The legal,
 * key, and boardBuffer arguments remain for that compatibility; this class
 * never makes each candidate move to determine legality.
 */
public class GenLegalPext {

    public static int genAll(long board0, long board1, long board2, long board3, int status, long key, boolean legal, long[] movesBuffer, long[] boardBuffer) {
        return generate(board0, board1, board2, board3, status, 0L, false, movesBuffer, GEN_ALL);
    }

    public static int genEvasion(long board0, long board1, long board2, long board3, int status, long key, boolean legal, long checkers, long[] movesBuffer, long[] boardBuffer) {
        return generate(board0, board1, board2, board3, status, checkers, true, movesBuffer, GEN_ALL);
    }

    public static int genTactical(long board0, long board1, long board2, long board3, int status, long key, boolean legal, long[] movesBuffer, long[] boardBuffer) {
        return generate(board0, board1, board2, board3, status, 0L, false, movesBuffer, GEN_TACTICAL);
    }

    public static int genQuiet(long board0, long board1, long board2, long board3, int status, long key, boolean legal, long[] movesBuffer, long[] boardBuffer) {
        return generate(board0, board1, board2, board3, status, 0L, false, movesBuffer, GEN_QUIET);
    }

    private GenLegalPext() {}

    private static final int GEN_ALL = 0;
    private static final int GEN_TACTICAL = 1;
    private static final int GEN_QUIET = 2;

    private static int generate(long board0, long board1, long board2, long board3, int status, long suppliedCheckers, boolean useSuppliedCheckers, long[] moves, int generationType) {
        final int player = status & Board.PLAYER_BIT;
        final int playerBit = player << Board.PLAYER_SHIFT;
        final long colorMask = ~(-(player) ^ board3);
        final long allOccupancy = board0 | board1 | board2;
        final long playerOccupancy = allOccupancy & colorMask;
        final long otherOccupancy = allOccupancy & ~colorMask;

        final long playerKing = board0 & ~board1 & ~board2 & colorMask;
        final long otherKing = board0 & ~board1 & ~board2 & ~colorMask;
        final long otherQueens = ~board0 & board1 & ~board2 & ~colorMask;
        final long otherRooks = board0 & board1 & ~board2 & ~colorMask;
        final long otherBishops = ~board0 & ~board1 & board2 & ~colorMask;
        final long otherKnights = board0 & ~board1 & board2 & ~colorMask;
        final long otherPawns = ~board0 & board1 & board2 & ~colorMask;

        final int kingSquare = bitScan(playerKing);
        final long checkers = useSuppliedCheckers
            ? suppliedCheckers
            : Board.getCheckersPext(board0, board1, board2, board3, colorMask, player, kingSquare, allOccupancy);
        final boolean multipleCheck = (checkers & (checkers - 1L)) != 0L;

        long responseMask = ~0L;
        if(checkers != 0L) {
            if(multipleCheck) {
                responseMask = 0L;
            } else {
                final int checkerSquare = bitScan(checkers);
                final long checkerBit = 1L << checkerSquare;
                final long slidingChecker = checkerBit & (otherQueens | otherRooks | otherBishops);
                responseMask = checkerBit;
                if(slidingChecker != 0L) responseMask |= BETWEEN[kingSquare | (checkerSquare << 6)];
            }
        }

        final long rookBlockers = Pext.rookMoves(kingSquare, allOccupancy) & playerOccupancy;
        final long bishopBlockers = Pext.bishopMoves(kingSquare, allOccupancy) & playerOccupancy;
        final long rookPinners = Pext.rookMoves(kingSquare, allOccupancy ^ rookBlockers) & (otherQueens | otherRooks);
        final long bishopPinners = Pext.bishopMoves(kingSquare, allOccupancy ^ bishopBlockers) & (otherQueens | otherBishops);
        long pinned = 0L;
        long pinners = rookPinners | bishopPinners;
        while(pinners != 0L) {
            final long pinner = pinners & -pinners;
            pinners ^= pinner;
            pinned |= BETWEEN[kingSquare | (bitScan(pinner) << 6)] & playerOccupancy;
        }

        int moveListLength = 0;
        moveListLength = getKingMoves(
            board0, board1, board2, board3,
            status, moves, Piece.KING | playerBit, moveListLength,
            player, kingSquare, playerOccupancy, otherOccupancy, allOccupancy,
            checkers, generationType,
            otherKing, otherQueens, otherRooks, otherBishops, otherKnights, otherPawns
        );
        if(multipleCheck) return moveListLength;

        moveListLength = getQueenMoves(
            board0, board1, board2, board3, colorMask, moves,
            Piece.QUEEN | playerBit, moveListLength, allOccupancy, otherOccupancy,
            kingSquare, pinned, rookPinners, bishopPinners, responseMask, generationType
        );
        moveListLength = getRookMoves(
            board0, board1, board2, board3, colorMask, moves,
            Piece.ROOK | playerBit, moveListLength, allOccupancy, otherOccupancy,
            kingSquare, pinned, rookPinners, responseMask, generationType
        );
        moveListLength = getBishopMoves(
            board0, board1, board2, board3, colorMask, moves,
            Piece.BISHOP | playerBit, moveListLength, allOccupancy, otherOccupancy,
            kingSquare, pinned, bishopPinners, responseMask, generationType
        );
        moveListLength = getKnightMoves(
            board0, board1, board2, board3, colorMask, moves,
            Piece.KNIGHT | playerBit, moveListLength, allOccupancy, otherOccupancy,
            pinned, responseMask, generationType
        );
        return getPawnMoves(
            board0, board1, board2, board3, colorMask, status, moves,
            Piece.PAWN | playerBit, moveListLength, player, allOccupancy, otherOccupancy,
            kingSquare, pinned, rookPinners, bishopPinners, responseMask, checkers,
            generationType,
            otherKing, otherQueens, otherRooks, otherBishops, otherKnights, otherPawns
        );
    }

    private static int getKingMoves(
        long board0, long board1, long board2, long board3,
        int status, long[] moves, int piece, int moveListLength,
        int player, int square, long playerOccupancy, long otherOccupancy, long allOccupancy,
        long checkers, int generationType,
        long otherKing, long otherQueens, long otherRooks, long otherBishops, long otherKnights, long otherPawns
    ) {
        final long kingAttacks = KING_ATTACKS[square];
        final int moveInfo = square | (piece << Board.START_PIECE_SHIFT);
        if(generationType != GEN_QUIET) {
            long moveBitboard = kingAttacks & otherOccupancy;
            while(moveBitboard != 0L) {
                final long b = moveBitboard & -moveBitboard;
                moveBitboard ^= b;
                final int targetSquare = bitScan(b);
                if(isKingDestinationSafe(
                    square, targetSquare, player, allOccupancy,
                    otherKing, otherQueens, otherRooks, otherBishops, otherKnights, otherPawns
                )) {
                    moves[moveListLength ++] = moveInfo
                        | (targetSquare << Board.TARGET_SQUARE_SHIFT)
                        | (getTargetPiece(board0, board1, board2, board3, targetSquare) << Board.TARGET_PIECE_SHIFT);
                }
            }
        }
        if(generationType != GEN_TACTICAL) {
            long moveBitboard = kingAttacks & ~allOccupancy;
            while(moveBitboard != 0L) {
                final long b = moveBitboard & -moveBitboard;
                moveBitboard ^= b;
                final int targetSquare = bitScan(b);
                if(isKingDestinationSafe(
                    square, targetSquare, player, allOccupancy,
                    otherKing, otherQueens, otherRooks, otherBishops, otherKnights, otherPawns
                )) {
                    moves[moveListLength ++] = moveInfo | (targetSquare << Board.TARGET_SQUARE_SHIFT);
                }
            }
            if(checkers == 0L) {
                final int blackMask = -player;
                final int whiteMask = ~blackMask;
                final int expectedKingSquare = player == 0 ? 4 : 60;
                final int kingSideRookSquare = player == 0 ? Board.SQUARE_H1 : Board.SQUARE_H8;
                final int queenSideRookSquare = player == 0 ? Board.SQUARE_A1 : Board.SQUARE_A8;
                final int rook = Piece.ROOK | (player << Board.PLAYER_SHIFT);
                final boolean kingSide = square == expectedKingSquare
                    && getTargetPiece(board0, board1, board2, board3, kingSideRookSquare) == rook
                    && (status & ((WHITE_KINGSIDE_CASTLING_BIT_UNSHIFTED & whiteMask) | (BLACK_KINGSIDE_CASTLING_BIT_UNSHIFTED & blackMask))) != Value.NONE;
                final boolean queenSide = square == expectedKingSquare
                    && getTargetPiece(board0, board1, board2, board3, queenSideRookSquare) == rook
                    && (status & ((WHITE_QUEENSIDE_CASTLING_BIT_UNSHIFTED & whiteMask) | (BLACK_QUEENSIDE_CASTLING_BIT_UNSHIFTED & blackMask))) != Value.NONE;
                if(kingSide) {
                    final long intermediateSquares = (WHITE_KINGSIDE_CASTLING_INTERMEDIATE_SQUARES & whiteMask) | (BLACK_KINGSIDE_CASTLING_INTERMEDIATE_SQUARES & blackMask);
                    if((allOccupancy & intermediateSquares) == 0L
                        && isKingDestinationSafe(
                            square, square + 1, player, allOccupancy,
                            otherKing, otherQueens, otherRooks, otherBishops, otherKnights, otherPawns
                        )
                        && isKingDestinationSafe(
                            square, square + 2, player, allOccupancy,
                            otherKing, otherQueens, otherRooks, otherBishops, otherKnights, otherPawns
                        )) {
                        moves[moveListLength ++] = moveInfo | ((square + 2) << Board.TARGET_SQUARE_SHIFT);
                    }
                }
                if(queenSide) {
                    final long intermediateSquares = (WHITE_QUEENSIDE_CASTLING_INTERMEDIATE_SQUARES & whiteMask) | (BLACK_QUEENSIDE_CASTLING_INTERMEDIATE_SQUARES & blackMask);
                    if((allOccupancy & intermediateSquares) == 0L
                        && isKingDestinationSafe(
                            square, square - 1, player, allOccupancy,
                            otherKing, otherQueens, otherRooks, otherBishops, otherKnights, otherPawns
                        )
                        && isKingDestinationSafe(
                            square, square - 2, player, allOccupancy,
                            otherKing, otherQueens, otherRooks, otherBishops, otherKnights, otherPawns
                        )) {
                        moves[moveListLength ++] = moveInfo | ((square - 2) << Board.TARGET_SQUARE_SHIFT);
                    }
                }
            }
        }
        return moveListLength;
    }

    private static int getQueenMoves(
        long board0, long board1, long board2, long board3, long colorMask,
        long[] moves, int piece, int moveListLength, long allOccupancy, long otherOccupancy,
        int kingSquare, long pinned, long rookPinners, long bishopPinners,
        long responseMask, int generationType
    ) {
        long pieceBitboard = ~board0 & board1 & ~board2 & colorMask;
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = bitScan(b);
            long moveBitboard = Pext.queenMoves(square, allOccupancy) & responseMask;
            if((b & pinned) != 0L) moveBitboard &= getPinRay(kingSquare, square, rookPinners | bishopPinners);
            moveListLength = addPieceMoves(
                board0, board1, board2, board3, moves, piece, moveListLength,
                square, moveBitboard, allOccupancy, otherOccupancy, generationType
            );
        }
        return moveListLength;
    }

    private static int getRookMoves(
        long board0, long board1, long board2, long board3, long colorMask,
        long[] moves, int piece, int moveListLength, long allOccupancy, long otherOccupancy,
        int kingSquare, long pinned, long rookPinners, long responseMask, int generationType
    ) {
        long pieceBitboard = board0 & board1 & ~board2 & colorMask;
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = bitScan(b);
            long moveBitboard = Pext.rookMoves(square, allOccupancy) & responseMask;
            if((b & pinned) != 0L) moveBitboard &= getPinRay(kingSquare, square, rookPinners);
            moveListLength = addPieceMoves(
                board0, board1, board2, board3, moves, piece, moveListLength,
                square, moveBitboard, allOccupancy, otherOccupancy, generationType
            );
        }
        return moveListLength;
    }

    private static int getBishopMoves(
        long board0, long board1, long board2, long board3, long colorMask,
        long[] moves, int piece, int moveListLength, long allOccupancy, long otherOccupancy,
        int kingSquare, long pinned, long bishopPinners, long responseMask, int generationType
    ) {
        long pieceBitboard = ~board0 & ~board1 & board2 & colorMask;
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = bitScan(b);
            long moveBitboard = Pext.bishopMoves(square, allOccupancy) & responseMask;
            if((b & pinned) != 0L) moveBitboard &= getPinRay(kingSquare, square, bishopPinners);
            moveListLength = addPieceMoves(
                board0, board1, board2, board3, moves, piece, moveListLength,
                square, moveBitboard, allOccupancy, otherOccupancy, generationType
            );
        }
        return moveListLength;
    }

    private static int getKnightMoves(
        long board0, long board1, long board2, long board3, long colorMask,
        long[] moves, int piece, int moveListLength, long allOccupancy, long otherOccupancy,
        long pinned, long responseMask, int generationType
    ) {
        long pieceBitboard = board0 & ~board1 & board2 & colorMask & ~pinned;
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = bitScan(b);
            final long moveBitboard = LEAP_ATTACKS[square] & responseMask;
            moveListLength = addPieceMoves(
                board0, board1, board2, board3, moves, piece, moveListLength,
                square, moveBitboard, allOccupancy, otherOccupancy, generationType
            );
        }
        return moveListLength;
    }

    private static int addPieceMoves(
        long board0, long board1, long board2, long board3,
        long[] moves, int piece, int moveListLength, int square,
        long destinations, long allOccupancy, long otherOccupancy, int generationType
    ) {
        final int moveInfo = square | (piece << Board.START_PIECE_SHIFT);
        if(generationType != GEN_QUIET) {
            long moveBitboard = destinations & otherOccupancy;
            while(moveBitboard != 0L) {
                final long b = moveBitboard & -moveBitboard;
                moveBitboard ^= b;
                final int targetSquare = bitScan(b);
                moves[moveListLength ++] = moveInfo
                    | (targetSquare << Board.TARGET_SQUARE_SHIFT)
                    | (getTargetPiece(board0, board1, board2, board3, targetSquare) << Board.TARGET_PIECE_SHIFT);
            }
        }
        if(generationType != GEN_TACTICAL) {
            long moveBitboard = destinations & ~allOccupancy;
            while(moveBitboard != 0L) {
                final long b = moveBitboard & -moveBitboard;
                moveBitboard ^= b;
                moves[moveListLength ++] = moveInfo | (bitScan(b) << Board.TARGET_SQUARE_SHIFT);
            }
        }
        return moveListLength;
    }

    private static int getPawnMoves(
        long board0, long board1, long board2, long board3, long colorMask, int status,
        long[] moves, int piece, int moveListLength, int player,
        long allOccupancy, long otherOccupancy,
        int kingSquare, long pinned, long rookPinners, long bishopPinners,
        long responseMask, long checkers, int generationType,
        long otherKing, long otherQueens, long otherRooks, long otherBishops, long otherKnights, long otherPawns
    ) {
        long pieceBitboard = ~board0 & board1 & board2 & colorMask;
        final int playerBit = player << Board.PLAYER_SHIFT;
        final int promotionRank = 7 & ~(-player);
        final int eSquare = Board.enPassantSquare(status);
        final long epBit = eSquare == Value.INVALID ? 0L : 1L << eSquare;
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = bitScan(b);
            final int moveInfo = square | (piece << Board.START_PIECE_SHIFT);
            final long pinRay = (b & pinned) == 0L
                ? ~0L
                : getPinRay(kingSquare, square, rookPinners | bishopPinners);

            if(generationType != GEN_QUIET) {
                long moveBitboard = PAWN_ATTACKS[player][square] & otherOccupancy & responseMask & pinRay;
                while(moveBitboard != 0L) {
                    final long b2 = moveBitboard & -moveBitboard;
                    moveBitboard ^= b2;
                    final int targetSquare = bitScan(b2);
                    final int targetPiece = getTargetPiece(board0, board1, board2, board3, targetSquare);
                    final int promoteInfo = moveInfo
                        | (targetSquare << Board.TARGET_SQUARE_SHIFT)
                        | (targetPiece << Board.TARGET_PIECE_SHIFT);
                    if((targetSquare >>> 3) == promotionRank) {
                        moveListLength = addPromotions(moves, moveListLength, promoteInfo, playerBit);
                    } else {
                        moves[moveListLength ++] = promoteInfo;
                    }
                }

                final long epDestination = PAWN_ATTACKS[player][square] & epBit & pinRay;
                if(epDestination != 0L) {
                    final int targetSquare = bitScan(epDestination);
                    final int capturedSquare = targetSquare + (player == 0 ? -8 : 8);
                    final long capturedPawnBit = 1L << capturedSquare;
                    final boolean checkResponse = checkers == 0L
                        || (epDestination & responseMask) != 0L
                        || (capturedPawnBit & checkers) != 0L;
                    if((otherPawns & capturedPawnBit) != 0L && checkResponse) {
                        final long occupancyAfter = (allOccupancy ^ b ^ capturedPawnBit) | epDestination;
                        if(!isSquareAttacked(
                            kingSquare, player, occupancyAfter,
                            otherKing, otherQueens, otherRooks, otherBishops, otherKnights, otherPawns & ~capturedPawnBit
                        )) {
                            moves[moveListLength ++] = moveInfo | (targetSquare << Board.TARGET_SQUARE_SHIFT);
                        }
                    }
                }

                final long singlePush = PAWN_ADVANCE_SINGLE[player][square] & ~allOccupancy;
                if(singlePush != 0L && (singlePush & pinRay & responseMask) != 0L) {
                    final int targetSquare = bitScan(singlePush);
                    if((targetSquare >>> 3) == promotionRank) {
                        final int promoteInfo = moveInfo | (targetSquare << Board.TARGET_SQUARE_SHIFT);
                        moveListLength = addPromotions(moves, moveListLength, promoteInfo, playerBit);
                    }
                }
            }

            if(generationType != GEN_TACTICAL) {
                final long singlePush = PAWN_ADVANCE_SINGLE[player][square] & ~allOccupancy;
                if(singlePush == 0L) continue;
                final int targetSquare = bitScan(singlePush);
                if((targetSquare >>> 3) == promotionRank) continue;
                if((singlePush & pinRay & responseMask) != 0L) {
                    moves[moveListLength ++] = moveInfo | (targetSquare << Board.TARGET_SQUARE_SHIFT);
                }
                final long doublePush = PAWN_ADVANCE_DOUBLE[player][square] & ~allOccupancy & pinRay & responseMask;
                if(doublePush != 0L) {
                    moves[moveListLength ++] = moveInfo | (bitScan(doublePush) << Board.TARGET_SQUARE_SHIFT);
                }
            }
        }
        return moveListLength;
    }

    private static int addPromotions(long[] moves, int moveListLength, int moveInfo, int playerBit) {
        moves[moveListLength ++] = moveInfo | ((Piece.QUEEN | playerBit) << Board.PROMOTE_PIECE_SHIFT);
        moves[moveListLength ++] = moveInfo | ((Piece.ROOK | playerBit) << Board.PROMOTE_PIECE_SHIFT);
        moves[moveListLength ++] = moveInfo | ((Piece.BISHOP | playerBit) << Board.PROMOTE_PIECE_SHIFT);
        moves[moveListLength ++] = moveInfo | ((Piece.KNIGHT | playerBit) << Board.PROMOTE_PIECE_SHIFT);
        return moveListLength;
    }

    private static long getPinRay(int kingSquare, int pieceSquare, long pinners) {
        final long pieceBit = 1L << pieceSquare;
        while(pinners != 0L) {
            final long pinner = pinners & -pinners;
            pinners ^= pinner;
            final int pinnerSquare = bitScan(pinner);
            final long between = BETWEEN[kingSquare | (pinnerSquare << 6)];
            if((between & pieceBit) != 0L) return between | pinner;
        }
        return 0L;
    }

    private static boolean isKingDestinationSafe(
        int kingSquare, int targetSquare, int player, long allOccupancy,
        long otherKing, long otherQueens, long otherRooks, long otherBishops, long otherKnights, long otherPawns
    ) {
        final long targetBit = 1L << targetSquare;
        final long occupancyAfter = (allOccupancy & ~(1L << kingSquare) & ~targetBit) | targetBit;
        return !isSquareAttacked(
            targetSquare, player, occupancyAfter,
            otherKing & ~targetBit,
            otherQueens & ~targetBit,
            otherRooks & ~targetBit,
            otherBishops & ~targetBit,
            otherKnights & ~targetBit,
            otherPawns & ~targetBit
        );
    }

    private static boolean isSquareAttacked(
        int square, int player, long allOccupancy,
        long otherKing, long otherQueens, long otherRooks, long otherBishops, long otherKnights, long otherPawns
    ) {
        if((LEAP_ATTACKS[square] & otherKnights) != 0L) return true;
        if((PAWN_ATTACKS[player][square] & otherPawns) != 0L) return true;
        if((KING_ATTACKS[square] & otherKing) != 0L) return true;
        if((Pext.rookMoves(square, allOccupancy) & (otherQueens | otherRooks)) != 0L) return true;
        return (Pext.bishopMoves(square, allOccupancy) & (otherQueens | otherBishops)) != 0L;
    }

    private static int getTargetPiece(long board0, long board1, long board2, long board3, int square) {
        return (int) (((board3 >>> square & 1) << 3)
            | ((board2 >>> square & 1) << 2)
            | ((board1 >>> square & 1) << 1)
            | (board0 >>> square & 1));
    }

    private static int bitScan(long bitboard) {
        return LSB[(int) (((bitboard & -bitboard) * DB) >>> 58)];
    }

    private static final int WHITE_KINGSIDE_CASTLING_BIT_UNSHIFTED = 0b10;
    private static final int WHITE_QUEENSIDE_CASTLING_BIT_UNSHIFTED = 0b100;
    private static final int BLACK_KINGSIDE_CASTLING_BIT_UNSHIFTED = 0b1000;
    private static final int BLACK_QUEENSIDE_CASTLING_BIT_UNSHIFTED = 0b10000;
    private static final long WHITE_KINGSIDE_CASTLING_INTERMEDIATE_SQUARES = 0x0000000000000060L;
    private static final long WHITE_QUEENSIDE_CASTLING_INTERMEDIATE_SQUARES = 0x000000000000000eL;
    private static final long BLACK_KINGSIDE_CASTLING_INTERMEDIATE_SQUARES = 0x6000000000000000L;
    private static final long BLACK_QUEENSIDE_CASTLING_INTERMEDIATE_SQUARES = 0x0e00000000000000L;

    private static final long[] KING_ATTACKS = new long[64];
    private static final long[] LEAP_ATTACKS = new long[64];
    private static final long[][] PAWN_ATTACKS = new long[2][64];
    private static final long[][] PAWN_ADVANCE_SINGLE = new long[2][64];
    private static final long[][] PAWN_ADVANCE_DOUBLE = new long[2][64];
    private static final long[] BETWEEN = new long[64 * 64];
    static {
        for(int square = 0; square < 64; square ++) {
            KING_ATTACKS[square] = Bitboard.BB[Bitboard.KING_ATTACKS][square];
            LEAP_ATTACKS[square] = Bitboard.BB[Bitboard.LEAP_ATTACKS][square];
            for(int player = 0; player < 2; player ++) {
                PAWN_ATTACKS[player][square] = Bitboard.BB[Bitboard.PAWN_ATTACKS_PLAYER0 + player][square];
                PAWN_ADVANCE_SINGLE[player][square] = Bitboard.BB[Bitboard.PAWN_ADVANCE_1_PLAYER0 + player][square];
                PAWN_ADVANCE_DOUBLE[player][square] = Bitboard.BB[Bitboard.PAWN_ADVANCE_2_PLAYER0 + player][square];
            }
            for(int target = 0; target < 64; target ++) {
                BETWEEN[square | (target << 6)] = Bitboard.BB[Bitboard.BETWEEN][square | (target << 6)];
            }
        }
    }

    private static final int[] LSB = {
        0,  1, 48,  2, 57, 49, 28,  3,
        61, 58, 50, 42, 38, 29, 17,  4,
        62, 55, 59, 36, 53, 51, 43, 22,
        45, 39, 33, 30, 24, 18, 12,  5,
        63, 47, 56, 27, 60, 41, 37, 16,
        54, 35, 52, 21, 44, 32, 23, 11,
        46, 26, 40, 15, 34, 20, 31, 10,
        25, 14, 19,  9, 13,  8,  7,  6
    };

    private static final long DB = 0x03f79d71b4cb0a89L;

}
