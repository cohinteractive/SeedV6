package com.ohinteractive.seedv6.core;

import com.ohinteractive.seedv6.core.util.Bitboard;
import com.ohinteractive.seedv6.core.util.Magic;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;

public class Gen {
    
    public static int genAll(long board0, long board1, long board2, long board3, int status, long key, boolean legal, long[] movesBuffer, long[] boardBuffer) {
        final int player = status & Board.PLAYER_BIT;
        final int playerBit = player << Board.PLAYER_SHIFT;
        final long allOccupancy = board0 | board1 | board2;
        final long colorMask = ~(-(player) ^ board3);
        final long otherOccupancy = allOccupancy & ~colorMask;
        int moveListLength = 0;
        moveListLength = getKingMoves(board0, board1, board2, board3, colorMask, status, movesBuffer, Piece.KING | playerBit, moveListLength, player, allOccupancy, otherOccupancy);
        moveListLength = getQueenMoves(board0, board1, board2, board3, colorMask, movesBuffer, Piece.QUEEN | playerBit, moveListLength, allOccupancy, otherOccupancy);
        moveListLength = getPawnMoves(board0, board1, board2, board3, colorMask, status, movesBuffer, Piece.PAWN | playerBit, moveListLength, player, allOccupancy, otherOccupancy);
        moveListLength = getRookMoves(board0, board1, board2, board3, colorMask, movesBuffer, Piece.ROOK | playerBit, moveListLength, allOccupancy, otherOccupancy);
        moveListLength = getBishopMoves(board0, board1, board2, board3, colorMask, movesBuffer, Piece.BISHOP | playerBit, moveListLength, allOccupancy, otherOccupancy);
        moveListLength = getKnightMoves(board0, board1, board2, board3, colorMask, movesBuffer, Piece.KNIGHT | playerBit, moveListLength, allOccupancy, otherOccupancy);
        if(legal) moveListLength = purgeIllegalMoves(board0, board1, board2, board3, status, key, movesBuffer, player, moveListLength, boardBuffer);
        return moveListLength;
    }

    public static int genEvasion(long board0, long board1, long board2, long board3, int status, long key, boolean legal, long checkers, long[] movesBuffer, long[] boardBuffer) {
        final int player = status & Board.PLAYER_BIT;
        final int playerBit = player << Board.PLAYER_SHIFT;
        final long allOccupancy = board0 | board1 | board2;
        final long colorMask = ~(-(player) ^ board3);
        final long playerOccupancy = allOccupancy & colorMask;
        final long otherOccupancy = allOccupancy & ~colorMask;
        final long kingBitboard = board0 & ~board1 & ~board2 & colorMask;
        final int[] lsb = LSB;
        final int kingSquare = lsb[(int) (((kingBitboard & -kingBitboard) * DB) >>> 58)];
        int moveListLength = 0;
        moveListLength = getKingEvasions(board0, board1, board2, board3, movesBuffer, Piece.KING | playerBit, moveListLength, kingSquare, playerOccupancy);
        if((checkers & (checkers - 1L)) != 0L) {
            if(legal) moveListLength = purgeIllegalMoves(board0, board1, board2, board3, status, key, movesBuffer, player, moveListLength, boardBuffer);
            return moveListLength;
        }
        final int checkerSquare = lsb[(int) (((checkers & -checkers) * DB) >>> 58)];
        final long checkerBit = 1L << checkerSquare;
        final long checkersSliders = (((board1 ^ board2) & ~(board0 & board2))) & checkerBit;
        final long evasionMask = checkerBit | (BETWEEN[kingSquare | (checkerSquare << 6)] & -((checkersSliders | -checkersSliders) >>> 63));
        moveListLength = getQueenEvasions(board0, board1, board2, board3, colorMask, movesBuffer, Piece.QUEEN | playerBit, moveListLength, allOccupancy, evasionMask);
        moveListLength = getRookEvasions(board0, board1, board2, board3, colorMask, movesBuffer, Piece.ROOK | playerBit, moveListLength, allOccupancy, evasionMask);
        moveListLength = getBishopEvasions(board0, board1, board2, board3, colorMask, movesBuffer, Piece.BISHOP | playerBit, moveListLength, allOccupancy, evasionMask);
        moveListLength = getKnightEvasions(board0, board1, board2, board3, colorMask, movesBuffer, Piece.KNIGHT | playerBit, moveListLength, evasionMask);
        moveListLength = getPawnEvasions(board0, board1, board2, board3, colorMask, status, movesBuffer, Piece.PAWN | playerBit, moveListLength, player, allOccupancy, otherOccupancy, evasionMask);
        if(legal) moveListLength = purgeIllegalMoves(board0, board1, board2, board3, status, key, movesBuffer, player, moveListLength, boardBuffer);
        return moveListLength;
    }

    public static int genTactical(long board0, long board1, long board2, long board3, int status, long key, boolean legal, long[] movesBuffer, long[] boardBuffer) {
        final int player = status & Board.PLAYER_BIT;
        final int playerBit = player << Board.PLAYER_SHIFT;
        final long allOccupancy = board0 | board1 | board2;
        final long colorMask = ~(-(player) ^ board3);
        final long playerOccupancy = allOccupancy & colorMask;
        final long otherOccupancy = allOccupancy & ~colorMask;
        int moveListLength = 0;
        moveListLength = getKingTactical(board0, board1, board2, board3, colorMask, movesBuffer, Piece.KING | playerBit, moveListLength, otherOccupancy);
        moveListLength = getQueenTactical(board0, board1, board2, board3, colorMask, movesBuffer, Piece.QUEEN | playerBit, moveListLength, allOccupancy, otherOccupancy);
        moveListLength = getRookTactical(board0, board1, board2, board3, colorMask, movesBuffer, Piece.ROOK | playerBit, moveListLength, allOccupancy, otherOccupancy);
        moveListLength = getBishopTactical(board0, board1, board2, board3, colorMask, movesBuffer, Piece.BISHOP | playerBit, moveListLength, allOccupancy, otherOccupancy);
        moveListLength = getKnightTactical(board0, board1, board2, board3, colorMask, movesBuffer, Piece.KNIGHT | playerBit, moveListLength, otherOccupancy);
        return moveListLength;
    }

    private Gen() {}

    private static int purgeIllegalMoves(long board0, long board1, long board2, long board3, int status, long key, long[] moves, int player, int moveListLength, long[] boardBuffer) {
        int legalMoveCount = 0;
        for(int i = 0; i < moveListLength; i ++) {
            final long move = moves[i];
            Board.makeMoveInto(board0, board1, board2, board3, status, key, move, boardBuffer);
            if(!Board.isPlayerInCheck(boardBuffer[0], boardBuffer[1], boardBuffer[2], boardBuffer[3], player)) moves[legalMoveCount ++] = move;
        }
        return legalMoveCount;
    }

    private static final int WHITE_KINGSIDE_CASTLING_BIT_UNSHIFTED = 0b10;
    private static final int WHITE_QUEENSIDE_CASTLING_BIT_UNSHIFTED = 0b100;
    private static final int BLACK_KINGSIDE_CASTLING_BIT_UNSHIFTED = 0b1000;
    private static final int BLACK_QUEENSIDE_CASTLING_BIT_UNSHIFTED = 0b10000;
    private static final long WHITE_KINGSIDE_CASTLING_INTERMEDIATE_SQUARES = 0x0000000000000060L;
    private static final long WHITE_QUEENSIDE_CASTLING_INTERMEDIATE_SQUARES = 0x000000000000000eL;
    private static final long BLACK_KINGSIDE_CASTLING_INTERMEDIATE_SQUARES = 0x6000000000000000L;
    private static final long BLACK_QUEENSIDE_CASTLING_INTERMEDIATE_SQUARES = 0x0e00000000000000L;

    private static int getKingMoves(long board0, long board1, long board2, long board3, long colorMask, int status, long[] moves, int piece, int moveListLength, int player, long allOccupancy, long otherOccupancy) {
        final long pieceBitboard = board0 & ~board1 & ~board2 & colorMask;
        final int[] lsb = LSB;
        final int square = lsb[(int) (((pieceBitboard & -pieceBitboard) * DB) >>> 58)];
        final long kingAttacks = KING_ATTACKS[square];
        long moveBitboard = kingAttacks & otherOccupancy;
        final int moveInfo = square | (piece << Board.START_PIECE_SHIFT);
        while(moveBitboard != 0L) {
            final long b = moveBitboard & -moveBitboard;
            moveBitboard ^= b;
            final int targetSquare = lsb[(int) ((b * DB) >>> 58)];
            moves[moveListLength ++] = moveInfo | (targetSquare << Board.TARGET_SQUARE_SHIFT) | ((int) (((board3 >>> targetSquare & 1) << 3) | ((board2 >>> targetSquare & 1) << 2) | ((board1 >>> targetSquare & 1) << 1) | (board0 >>> targetSquare & 1)) << Board.TARGET_PIECE_SHIFT);
        }
        moveBitboard = kingAttacks & ~allOccupancy;
        while(moveBitboard != 0L) {
            final long b = moveBitboard & -moveBitboard;
            moveBitboard ^= b;
            moves[moveListLength ++] = moveInfo | (lsb[(int) ((b * DB) >>> 58)] << Board.TARGET_SQUARE_SHIFT);
        }
        final int blackCastlingBitsMask = -player;
        final int whiteCastlingBitsMask = ~blackCastlingBitsMask;
        final boolean kingSide = (status & ((WHITE_KINGSIDE_CASTLING_BIT_UNSHIFTED & whiteCastlingBitsMask) | (BLACK_KINGSIDE_CASTLING_BIT_UNSHIFTED & blackCastlingBitsMask))) != Value.NONE;
        final boolean queenSide = (status & ((WHITE_QUEENSIDE_CASTLING_BIT_UNSHIFTED & whiteCastlingBitsMask) | (BLACK_QUEENSIDE_CASTLING_BIT_UNSHIFTED & blackCastlingBitsMask))) != Value.NONE;
        if(kingSide || queenSide) {
            final int other = 1 ^ player;
            if(!Board.isSquareAttackedByPlayer(board0, board1, board2, board3, square, other)) {
                if(kingSide) {
                    if((allOccupancy & ((WHITE_KINGSIDE_CASTLING_INTERMEDIATE_SQUARES  & whiteCastlingBitsMask) | (BLACK_KINGSIDE_CASTLING_INTERMEDIATE_SQUARES  & blackCastlingBitsMask))) == 0L && !Board.isSquareAttackedByPlayer(board0, board1, board2, board3, square + 1, other))
                        moves[moveListLength ++] = moveInfo | ((square + 2) << Board.TARGET_SQUARE_SHIFT);
                }
                if(queenSide) {
                    if((allOccupancy & ((WHITE_QUEENSIDE_CASTLING_INTERMEDIATE_SQUARES & whiteCastlingBitsMask) | (BLACK_QUEENSIDE_CASTLING_INTERMEDIATE_SQUARES & blackCastlingBitsMask))) == 0L && !Board.isSquareAttackedByPlayer(board0, board1, board2, board3, square - 1, other))
                        moves[moveListLength ++] = moveInfo | ((square - 2) << Board.TARGET_SQUARE_SHIFT);
                }
            }
        }
        return moveListLength;
    }

    private static int getKingTactical(long board0, long board1, long board2, long board3, long colorMask, long[] moves, int piece, int moveListLength, long otherOccupancy) {
        final long pieceBitboard = board0 & ~board1 & ~board2 & colorMask;
        final int[] lsb = LSB;
        final int square = lsb[(int) (((pieceBitboard & -pieceBitboard) * DB) >>> 58)];
        final long kingAttacks = KING_ATTACKS[square];
        long moveBitboard = kingAttacks & otherOccupancy;
        final int moveInfo = square | (piece << Board.START_PIECE_SHIFT);
        while(moveBitboard != 0L) {
            final long b = moveBitboard & -moveBitboard;
            moveBitboard ^= b;
            final int targetSquare = lsb[(int) ((b * DB) >>> 58)];
            moves[moveListLength ++] = moveInfo | (targetSquare << Board.TARGET_SQUARE_SHIFT) | ((int) (((board3 >>> targetSquare & 1) << 3) | ((board2 >>> targetSquare & 1) << 2) | ((board1 >>> targetSquare & 1) << 1) | (board0 >>> targetSquare & 1)) << Board.TARGET_PIECE_SHIFT);
        }
        return moveListLength;
    }

    private static int getKnightMoves(long board0, long board1, long board2, long board3, long colorMask, long[] moves, int piece, int moveListLength, long allOccupancy, long otherOccupancy) {
        long pieceBitboard = board0 & ~board1 & board2 & colorMask;
        final int[] lsb = LSB;
        final long[] leapAttacks = LEAP_ATTACKS;
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            final long knightAttacks = leapAttacks[square];
            long moveBitboard = knightAttacks & otherOccupancy;
            final int moveInfo = square | (piece << Board.START_PIECE_SHIFT);
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                moves[moveListLength ++] = moveInfo | (targetSquare << Board.TARGET_SQUARE_SHIFT) | ((int) (((board3 >>> targetSquare & 1) << 3) | ((board2 >>> targetSquare & 1) << 2) | ((board1 >>> targetSquare & 1) << 1) | (board0 >>> targetSquare & 1)) << Board.TARGET_PIECE_SHIFT);
            }
            moveBitboard = knightAttacks & ~allOccupancy;
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                moves[moveListLength ++] = moveInfo | (lsb[(int) ((b2 * DB) >>> 58)] << Board.TARGET_SQUARE_SHIFT);
            }
        }
        return moveListLength;
    }

    private static int getKnightTactical(long board0, long board1, long board2, long board3, long colorMask, long[] moves, int piece, int moveListLength, long otherOccupancy) {
        long pieceBitboard = board0 & ~board1 & board2 & colorMask;
        final int[] lsb = LSB;
        final long[] leapAttacks = LEAP_ATTACKS;
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            final long knightAttacks = leapAttacks[square];
            long moveBitboard = knightAttacks & otherOccupancy;
            final int moveInfo = square | (piece << Board.START_PIECE_SHIFT);
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                moves[moveListLength ++] = moveInfo | (targetSquare << Board.TARGET_SQUARE_SHIFT) | ((int) (((board3 >>> targetSquare & 1) << 3) | ((board2 >>> targetSquare & 1) << 2) | ((board1 >>> targetSquare & 1) << 1) | (board0 >>> targetSquare & 1)) << Board.TARGET_PIECE_SHIFT);
            }
        }
        return moveListLength;
    }

    private static int getPawnMoves(long board0, long board1, long board2, long board3, long colorMask, int status, long[] moves, int piece, int moveListLength, int player, long allOccupancy, long otherOccupancy) {
        long pieceBitboard = ~board0 & board1 & board2 & colorMask;
        final int[] lsb = LSB;
        final int playerBit = player << Board.PLAYER_SHIFT;
        final int promotionRank = 7 & ~(-player);
        final int eSquare = status >>> Board.ESQUARE_SHIFT & Board.SQUARE_BITS;
        otherOccupancy |= (eSquare > 0 ? (1L << eSquare) : 0L);
        final long[] pawnAttacks = PAWN_ATTACKS[player];
        final long[] pawnAdvanceSingle = PAWN_ADVANCE_SINGLE[player];
        final long[] pawnAdvanceDouble = PAWN_ADVANCE_DOUBLE[player];
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            long moveBitboard = pawnAttacks[square] & otherOccupancy;
            final int moveInfo = square | (piece << Board.START_PIECE_SHIFT); 
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                final int targetRank = targetSquare >>> 3;
                final int promoteInfo = moveInfo | (targetSquare << Board.TARGET_SQUARE_SHIFT) | ((int) (((board3 >>> targetSquare & 1) << 3) | ((board2 >>> targetSquare & 1) << 2) | ((board1 >>> targetSquare & 1) << 1) | (board0 >>> targetSquare & 1)) << Board.TARGET_PIECE_SHIFT);
                if(targetRank == promotionRank) {
                    moves[moveListLength++] = promoteInfo | ((Piece.QUEEN | playerBit) << Board.PROMOTE_PIECE_SHIFT);
                    moves[moveListLength++] = promoteInfo | ((Piece.ROOK | playerBit) << Board.PROMOTE_PIECE_SHIFT);
                    moves[moveListLength++] = promoteInfo | ((Piece.BISHOP | playerBit) << Board.PROMOTE_PIECE_SHIFT);
                    moves[moveListLength++] = promoteInfo | ((Piece.KNIGHT | playerBit) << Board.PROMOTE_PIECE_SHIFT);
                } else {
                    moves[moveListLength ++] = promoteInfo;
                }
            }
            final long singlePush = pawnAdvanceSingle[square] & ~allOccupancy;
            if(singlePush == 0L) continue;

            final int targetSquare = lsb[(int) ((singlePush * DB) >>> 58)];
            final int targetRank = targetSquare >>> 3;
            final int moveInfoWithTarget = moveInfo | (targetSquare << Board.TARGET_SQUARE_SHIFT);

            if(targetRank == promotionRank) {
                moves[moveListLength++] = moveInfoWithTarget | ((Piece.QUEEN  | playerBit) << Board.PROMOTE_PIECE_SHIFT);
                moves[moveListLength++] = moveInfoWithTarget | ((Piece.ROOK   | playerBit) << Board.PROMOTE_PIECE_SHIFT);
                moves[moveListLength++] = moveInfoWithTarget | ((Piece.BISHOP | playerBit) << Board.PROMOTE_PIECE_SHIFT);
                moves[moveListLength++] = moveInfoWithTarget | ((Piece.KNIGHT | playerBit) << Board.PROMOTE_PIECE_SHIFT);
                continue;
            }
            moves[moveListLength ++] = moveInfoWithTarget;
            final long doublePush = pawnAdvanceDouble[square] & ~allOccupancy;
            if(doublePush != 0L) moves[moveListLength ++] = moveInfo | (lsb[(int) ((doublePush * DB) >>> 58)] << Board.TARGET_SQUARE_SHIFT);
        }
        return moveListLength;
    }

    private static int getQueenMoves(long board0, long board1, long board2,long board3, long colorMask, long[] moves, int piece, int moveListLength, long allOccupancy, long otherOccupancy) {
        long pieceBitboard = ~board0 & board1 & ~board2 & colorMask;
        final int[] lsb = LSB;
        final long[][] rookMoves = Magic.ROOK_MOVES;
        final long[][] bishopMoves = Magic.BISHOP_MOVES;
        final long[] rookMovement = Magic.ROOK_MOVEMENT;
        final long[] bishopMovement = Magic.BISHOP_MOVEMENT;
        final long[] rookMagics = Magic.ROOK_MAGIC_NUMBER;
        final long[] bishopMagics = Magic.BISHOP_MAGIC_NUMBER;
        final int[] rookShifts = Magic.ROOK_SHIFT;
        final int[] bishopShifts = Magic.BISHOP_SHIFT;
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            final long magic = 
                rookMoves[square][(int)   ((allOccupancy & rookMovement[square])   * rookMagics[square]   >>> rookShifts[square])] |
                bishopMoves[square][(int) ((allOccupancy & bishopMovement[square]) * bishopMagics[square] >>> bishopShifts[square])];
            long moveBitboard = magic & otherOccupancy;
            final int moveInfo = square | (piece << Board.START_PIECE_SHIFT);
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                moves[moveListLength ++] = moveInfo | (targetSquare << Board.TARGET_SQUARE_SHIFT) | ((int) (((board3 >>> targetSquare & 1) << 3) | ((board2 >>> targetSquare & 1) << 2) | ((board1 >>> targetSquare & 1) << 1) | (board0 >>> targetSquare & 1)) << Board.TARGET_PIECE_SHIFT);
            }
            moveBitboard = magic & ~allOccupancy;
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                moves[moveListLength ++] = moveInfo | (lsb[(int) ((b2 * DB) >>> 58)] << Board.TARGET_SQUARE_SHIFT);
            }
        }
        return moveListLength;
    }

    private static int getQueenTactical(long board0, long board1, long board2, long board3, long colorMask, long[] moves, int piece, int moveListLength, long allOccupancy, long otherOccupancy) {
        long pieceBitboard = ~board0 & board1 & ~board2 & colorMask;
        final int[] lsb = LSB;
        final long[][] rookMoves = Magic.ROOK_MOVES;
        final long[][] bishopMoves = Magic.BISHOP_MOVES;
        final long[] rookMovement = Magic.ROOK_MOVEMENT;
        final long[] bishopMovement = Magic.BISHOP_MOVEMENT;
        final long[] rookMagics = Magic.ROOK_MAGIC_NUMBER;
        final long[] bishopMagics = Magic.BISHOP_MAGIC_NUMBER;
        final int[] rookShifts = Magic.ROOK_SHIFT;
        final int[] bishopShifts = Magic.BISHOP_SHIFT;
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            final long magic = 
                rookMoves[square][(int)   ((allOccupancy & rookMovement[square])   * rookMagics[square]   >>> rookShifts[square])] |
                bishopMoves[square][(int) ((allOccupancy & bishopMovement[square]) * bishopMagics[square] >>> bishopShifts[square])];
            long moveBitboard = magic & otherOccupancy;
            final int moveInfo = square | (piece << Board.START_PIECE_SHIFT);
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                moves[moveListLength ++] = moveInfo | (targetSquare << Board.TARGET_SQUARE_SHIFT) | ((int) (((board3 >>> targetSquare & 1) << 3) | ((board2 >>> targetSquare & 1) << 2) | ((board1 >>> targetSquare & 1) << 1) | (board0 >>> targetSquare & 1)) << Board.TARGET_PIECE_SHIFT);
            }
        }
        return moveListLength;
    }

    private static int getRookMoves(long board0, long board1, long board2, long board3, long colorMask, long[] moves, int piece, int moveListLength, long allOccupancy, long otherOccupancy) {
        long pieceBitboard = board0 & board1 & ~board2 & colorMask;
        final int[] lsb = LSB;
        final long[][] rookMoves = Magic.ROOK_MOVES;
        final long[] rookMovement = Magic.ROOK_MOVEMENT;
        final long[] rookMagics = Magic.ROOK_MAGIC_NUMBER;
        final int[] rookShifts = Magic.ROOK_SHIFT;
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            final long magic = rookMoves[square][(int) ((allOccupancy & rookMovement[square]) * rookMagics[square] >>> rookShifts[square])];
            long moveBitboard = magic & otherOccupancy;
            final int moveInfo = square | (piece << Board.START_PIECE_SHIFT);
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                moves[moveListLength ++] = moveInfo | (targetSquare << Board.TARGET_SQUARE_SHIFT) | ((int) (((board3 >>> targetSquare & 1) << 3) | ((board2 >>> targetSquare & 1) << 2) | ((board1 >>> targetSquare & 1) << 1) | (board0 >>> targetSquare & 1)) << Board.TARGET_PIECE_SHIFT);
            }
            moveBitboard = magic & ~allOccupancy;
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                moves[moveListLength ++] = moveInfo | (lsb[(int) ((b2 * DB) >>> 58)] << Board.TARGET_SQUARE_SHIFT);
            }
        }
        return moveListLength;
    }

    private static int getRookTactical(long board0, long board1, long board2, long board3, long colorMask, long[] moves, int piece, int moveListLength, long allOccupancy, long otherOccupancy) {
        long pieceBitboard = board0 & board1 & ~board2 & colorMask;
        final int[] lsb = LSB;
        final long[][] rookMoves = Magic.ROOK_MOVES;
        final long[] rookMovement = Magic.ROOK_MOVEMENT;
        final long[] rookMagics = Magic.ROOK_MAGIC_NUMBER;
        final int[] rookShifts = Magic.ROOK_SHIFT;
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            final long magic = 
                rookMoves[square][(int)   ((allOccupancy & rookMovement[square])   * rookMagics[square]   >>> rookShifts[square])];
            long moveBitboard = magic & otherOccupancy;
            final int moveInfo = square | (piece << Board.START_PIECE_SHIFT);
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                moves[moveListLength ++] = moveInfo | (targetSquare << Board.TARGET_SQUARE_SHIFT) | ((int) (((board3 >>> targetSquare & 1) << 3) | ((board2 >>> targetSquare & 1) << 2) | ((board1 >>> targetSquare & 1) << 1) | (board0 >>> targetSquare & 1)) << Board.TARGET_PIECE_SHIFT);
            }
        }
        return moveListLength;
    }

    private static int getBishopMoves(long board0, long board1, long board2, long board3, long colorMask, long[] moves, int piece, int moveListLength, long allOccupancy, long otherOccupancy) {
        long pieceBitboard = ~board0 & ~board1 & board2 & colorMask;
        final int[] lsb = LSB;
        final long[][] bishopMoves = Magic.BISHOP_MOVES;
        final long[] bishopMovement = Magic.BISHOP_MOVEMENT;
        final long[] bishopMagics = Magic.BISHOP_MAGIC_NUMBER;
        final int[] bishopShifts = Magic.BISHOP_SHIFT;
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            final long magic = bishopMoves[square][(int) ((allOccupancy & bishopMovement[square]) * bishopMagics[square] >>> bishopShifts[square])];
            long moveBitboard = magic & otherOccupancy;
            final int moveInfo = square | (piece << Board.START_PIECE_SHIFT);
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                moves[moveListLength ++] = moveInfo | (targetSquare << Board.TARGET_SQUARE_SHIFT) | ((int) (((board3 >>> targetSquare & 1) << 3) | ((board2 >>> targetSquare & 1) << 2) | ((board1 >>> targetSquare & 1) << 1) | (board0 >>> targetSquare & 1)) << Board.TARGET_PIECE_SHIFT);
            }
            moveBitboard = magic & ~allOccupancy;
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                moves[moveListLength ++] = moveInfo | (lsb[(int) ((b2 * DB) >>> 58)] << Board.TARGET_SQUARE_SHIFT);
            }
        }
        return moveListLength;
    }

    private static int getBishopTactical(long board0, long board1, long board2, long board3, long colorMask, long[] moves, int piece, int moveListLength, long allOccupancy, long otherOccupancy) {
        long pieceBitboard = ~board0 & ~board1 & board2 & colorMask;
        final int[] lsb = LSB;
        final long[][] bishopMoves = Magic.BISHOP_MOVES;
        final long[] bishopMovement = Magic.BISHOP_MOVEMENT;
        final long[] bishopMagics = Magic.BISHOP_MAGIC_NUMBER;
        final int[] bishopShifts = Magic.BISHOP_SHIFT;
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            final long magic = 
                bishopMoves[square][(int) ((allOccupancy & bishopMovement[square]) * bishopMagics[square] >>> bishopShifts[square])];
            long moveBitboard = magic & otherOccupancy;
            final int moveInfo = square | (piece << Board.START_PIECE_SHIFT);
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                moves[moveListLength ++] = moveInfo | (targetSquare << Board.TARGET_SQUARE_SHIFT) | ((int) (((board3 >>> targetSquare & 1) << 3) | ((board2 >>> targetSquare & 1) << 2) | ((board1 >>> targetSquare & 1) << 1) | (board0 >>> targetSquare & 1)) << Board.TARGET_PIECE_SHIFT);
            }
        }
        return moveListLength;
    }

    private static int getKingEvasions(long board0, long board1, long board2, long board3, long[] moves, int piece, int moveListLength, int square, long playerOccupancy) {
        final int[] lsb = LSB;
        final long kingAttacks = KING_ATTACKS[square];
        long moveBitboard = kingAttacks & ~playerOccupancy;
        final int moveInfo = square | (piece << Board.START_PIECE_SHIFT);
        while(moveBitboard != 0L) {
            final long b = moveBitboard & -moveBitboard;
            moveBitboard ^= b;
            final int targetSquare = lsb[(int) ((b * DB) >>> 58)];
            moves[moveListLength ++] = moveInfo | (targetSquare << Board.TARGET_SQUARE_SHIFT) | ((int) (((board3 >>> targetSquare & 1) << 3) | ((board2 >>> targetSquare & 1) << 2) | ((board1 >>> targetSquare & 1) << 1) | (board0 >>> targetSquare & 1)) << Board.TARGET_PIECE_SHIFT);
        }
        return moveListLength;
    }

    private static int getKnightEvasions(long board0, long board1, long board2, long board3, long colorMask, long[] moves, int piece, int moveListLength, long evasionMask) {
        long pieceBitboard = board0 & ~board1 & board2 & colorMask;
        final int[] lsb = LSB;
        final long[] leapAttacks = LEAP_ATTACKS;
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            final long knightAttacks = leapAttacks[square];
            long moveBitboard = knightAttacks & evasionMask;
            final int moveInfo = square | (piece << Board.START_PIECE_SHIFT);
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                moves[moveListLength ++] = moveInfo | (targetSquare << Board.TARGET_SQUARE_SHIFT) | ((int) (((board3 >>> targetSquare & 1) << 3) | ((board2 >>> targetSquare & 1) << 2) | ((board1 >>> targetSquare & 1) << 1) | (board0 >>> targetSquare & 1)) << Board.TARGET_PIECE_SHIFT);
            }
        }
        return moveListLength;
    }

    private static int getPawnEvasions(long board0, long board1, long board2, long board3, long colorMask, int status, long[] moves, int piece, int moveListLength, int player, long allOccupancy, long otherOccupancy, long evasionMask) {
        long pieceBitboard = ~board0 & board1 & board2 & colorMask;
        final int[] lsb = LSB;
        final int playerBit = player << Board.PLAYER_SHIFT;
        final int promotionRank = 7 & ~(-player);
        final int eSquare = status >>> Board.ESQUARE_SHIFT & Board.SQUARE_BITS;
        final long[] pawnAttacks = PAWN_ATTACKS[player];
        final long[] pawnAdvanceSingle = PAWN_ADVANCE_SINGLE[player];
        final long[] pawnAdvanceDouble = PAWN_ADVANCE_DOUBLE[player];
        final long epPresentMask = (eSquare | -eSquare ) >> 31;
        final long epBit = (1L << eSquare) & epPresentMask;
        final long playerMask = -((long) player);
        final long capturedEpPawnBit = ((epBit >>> 8) & ~playerMask) | ((epBit << 8) & playerMask);
        final long epCapturesChecker = capturedEpPawnBit & evasionMask;
        final long eSquareEvasionMask = epBit & ((epCapturesChecker | -epCapturesChecker) >> 63);
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            long moveBitboard = pawnAttacks[square] & ((evasionMask & otherOccupancy) | eSquareEvasionMask); 
            final int moveInfo = square | (piece << Board.START_PIECE_SHIFT);
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                final int targetRank = targetSquare >>> 3;
                final int promoteInfo = moveInfo | (targetSquare << Board.TARGET_SQUARE_SHIFT) | ((int) (((board3 >>> targetSquare & 1) << 3) | ((board2 >>> targetSquare & 1) << 2) | ((board1 >>> targetSquare & 1) << 1) | (board0 >>> targetSquare & 1)) << Board.TARGET_PIECE_SHIFT);
                if(targetRank == promotionRank) {
                    moves[moveListLength ++] = promoteInfo | ((Piece.QUEEN | playerBit) << Board.PROMOTE_PIECE_SHIFT);
                    moves[moveListLength ++] = promoteInfo | ((Piece.ROOK | playerBit) << Board.PROMOTE_PIECE_SHIFT);
                    moves[moveListLength ++] = promoteInfo | ((Piece.BISHOP | playerBit) << Board.PROMOTE_PIECE_SHIFT);
                    moves[moveListLength ++] = promoteInfo | ((Piece.KNIGHT | playerBit) << Board.PROMOTE_PIECE_SHIFT);
                } else {
                    moves[moveListLength ++] = promoteInfo;
                }
            }
            final long singlePushValid = pawnAdvanceSingle[square] & ~allOccupancy;
            if(singlePushValid == 0L) continue;
            final long singlePush = singlePushValid & evasionMask;
            if(singlePush != 0L) {
                final int targetSquare = lsb[(int) ((singlePush * DB) >>> 58)];
                final int targetRank = targetSquare >>> 3;
                final int moveInfoWithTarget = moveInfo | (targetSquare << Board.TARGET_SQUARE_SHIFT);
                if(targetRank == promotionRank) {
                    moves[moveListLength++] = moveInfoWithTarget | ((Piece.QUEEN  | playerBit) << Board.PROMOTE_PIECE_SHIFT);
                    moves[moveListLength++] = moveInfoWithTarget | ((Piece.ROOK  | playerBit) << Board.PROMOTE_PIECE_SHIFT);
                    moves[moveListLength++] = moveInfoWithTarget | ((Piece.BISHOP  | playerBit) << Board.PROMOTE_PIECE_SHIFT);
                    moves[moveListLength++] = moveInfoWithTarget | ((Piece.KNIGHT  | playerBit) << Board.PROMOTE_PIECE_SHIFT);
                    continue;
                }
                moves[moveListLength ++] = moveInfoWithTarget;
            }
            final long doublePush = pawnAdvanceDouble[square] & ~allOccupancy & evasionMask;
            if(doublePush != 0L) moves[moveListLength ++] = moveInfo | (lsb[(int) ((doublePush * DB) >>> 58)] << Board.TARGET_SQUARE_SHIFT);
        }
        return moveListLength;
    }

    private static int getQueenEvasions(long board0, long board1, long board2, long board3, long colorMask, long[] moves, int piece, int moveListLength, long allOccupancy, long evasionMask) {
        long pieceBitboard = ~board0 & board1 & ~board2 & colorMask;
        final int[] lsb = LSB;
        final long[][] rookMoves = Magic.ROOK_MOVES;
        final long[][] bishopMoves = Magic.BISHOP_MOVES;
        final long[] rookMovement = Magic.ROOK_MOVEMENT;
        final long[] bishopMovement = Magic.BISHOP_MOVEMENT;
        final long[] rookMagics = Magic.ROOK_MAGIC_NUMBER;
        final long[] bishopMagics = Magic.BISHOP_MAGIC_NUMBER;
        final int[] rookShifts = Magic.ROOK_SHIFT;
        final int[] bishopShifts = Magic.BISHOP_SHIFT;
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            final long magic = 
                rookMoves[square][(int)   ((allOccupancy & rookMovement[square])   * rookMagics[square]   >>> rookShifts[square])] |
                bishopMoves[square][(int) ((allOccupancy & bishopMovement[square]) * bishopMagics[square] >>> bishopShifts[square])];
            long moveBitboard = magic & evasionMask;
            final int moveInfo = square | (piece << Board.START_PIECE_SHIFT);
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                moves[moveListLength ++] = moveInfo | (targetSquare << Board.TARGET_SQUARE_SHIFT) | ((int) (((board3 >>> targetSquare & 1) << 3) | ((board2 >>> targetSquare & 1) << 2) | ((board1 >>> targetSquare & 1) << 1) | (board0 >>> targetSquare & 1)) << Board.TARGET_PIECE_SHIFT);
            }
        }
        return moveListLength;
    }

    private static int getRookEvasions(long board0, long board1, long board2, long board3, long colorMask, long[] moves, int piece, int moveListLength, long allOccupancy, long evasionMask) {
        long pieceBitboard = board0 & board1 & ~board2 & colorMask;
        final int[] lsb = LSB;
        final long[][] rookMoves = Magic.ROOK_MOVES;
        final long[] rookMovement = Magic.ROOK_MOVEMENT;
        final long[] rookMagics = Magic.ROOK_MAGIC_NUMBER;
        final int[] rookShifts = Magic.ROOK_SHIFT;
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            final long magic = 
                rookMoves[square][(int)   ((allOccupancy & rookMovement[square])   * rookMagics[square]   >>> rookShifts[square])];
            long moveBitboard = magic & evasionMask;
            final int moveInfo = square | (piece << Board.START_PIECE_SHIFT);
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                moves[moveListLength ++] = moveInfo | (targetSquare << Board.TARGET_SQUARE_SHIFT) | ((int) (((board3 >>> targetSquare & 1) << 3) | ((board2 >>> targetSquare & 1) << 2) | ((board1 >>> targetSquare & 1) << 1) | (board0 >>> targetSquare & 1)) << Board.TARGET_PIECE_SHIFT);
            }
        }
        return moveListLength;
    }

    private static int getBishopEvasions(long board0, long board1, long board2, long board3, long colorMask, long[] moves, int piece, int moveListLength, long allOccupancy, long evasionMask) {
        long pieceBitboard = ~board0 & ~board1 & board2 & colorMask;
        final int[] lsb = LSB;
        final long[][] bishopMoves = Magic.BISHOP_MOVES;
        final long[] bishopMovement = Magic.BISHOP_MOVEMENT;
        final long[] bishopMagics = Magic.BISHOP_MAGIC_NUMBER;
        final int[] bishopShifts = Magic.BISHOP_SHIFT;
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            final long magic = 
                bishopMoves[square][(int) ((allOccupancy & bishopMovement[square]) * bishopMagics[square] >>> bishopShifts[square])];
            long moveBitboard = magic & evasionMask;
            final int moveInfo = square | (piece << Board.START_PIECE_SHIFT);
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                moves[moveListLength ++] = moveInfo | (targetSquare << Board.TARGET_SQUARE_SHIFT) | ((int) (((board3 >>> targetSquare & 1) << 3) | ((board2 >>> targetSquare & 1) << 2) | ((board1 >>> targetSquare & 1) << 1) | (board0 >>> targetSquare & 1)) << Board.TARGET_PIECE_SHIFT);
            }
        }
        return moveListLength;
    }

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

    /*
	 * To get the LSB from a long, use:
	 * int lsbIndex = LSB[(int) (((someLong & -someLong) * DB) >>> 58)];
	 */
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
