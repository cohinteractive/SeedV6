package com.ohinteractive.seedv6.core;

import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Bitboard;
import com.ohinteractive.seedv6.core.util.Fen;
import com.ohinteractive.seedv6.core.util.Magic;
import com.ohinteractive.seedv6.core.util.Pext;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.core.util.Zobrist;

public class BoardMoveType {

    public static final int STATUS = 4;
    public static final int MAX_BITBOARDS = 6;
    public static final int KEY = 5;
    public static final int PLAYER_BIT = 1;

    public static int player(int status) {
        return status & PLAYER_BIT;
    }

    public static final int WHITE_KINGSIDE_BIT = 0b1;
    public static final int BLACK_KINGSIDE_BIT = 0b100;
    public static final int WHITE_KINGSIDE_BIT_UNSHIFTED = 0b10;
    public static final int BLACK_KINGSIDE_BIT_UNSHIFTED = 0b1000;

    public static boolean kingSide(int status, int player) {
        return 
        (status & 
        ((WHITE_KINGSIDE_BIT_UNSHIFTED & ~(-player)) | (BLACK_KINGSIDE_BIT_UNSHIFTED & -player))
        ) != 0L;
    }

    public static final int WHITE_QUEENSIDE_BIT = 0b10;
    public static final int BLACK_QUEENSIDE_BIT = 0b1000;
    public static final int WHITE_QUEENSIDE_BIT_UNSHIFTED = 0b100;
    public static final int BLACK_QUEENSIDE_BIT_UNSHIFTED = 0b10000;

    public static boolean queenSide(int status, int player) {
        return 
        (status & 
        ((WHITE_QUEENSIDE_BIT_UNSHIFTED & ~(-player)) | (BLACK_QUEENSIDE_BIT_UNSHIFTED & -player))
        ) != 0L;
    }

    public static final int ESQUARE_SHIFT = 5;
    public static final int SQUARE_BITS = 0b111111;
    public static final long WHITE_ENPASSANT_SQUARES = 0x0000ff0000000000L;
    public static final long BLACK_ENPASSANT_SQUARES = 0x0000000000ff0000L;

    public static int enPassantSquare(int status) {
        int player = status & PLAYER_BIT;
        int eSquare = status >>> ESQUARE_SHIFT & SQUARE_BITS;
        return ((1L << eSquare) & ((WHITE_ENPASSANT_SQUARES & ~(-player)) | (BLACK_ENPASSANT_SQUARES & -player))) != 0L ? eSquare : Value.INVALID;
    }

    public static final int HALF_MOVE_CLOCK_SHIFT = 11;
    public static final int HALF_MOVE_CLOCK_BITS = 0b1111111;

    public static int halfMoveClock(int status) {
        return status >>> HALF_MOVE_CLOCK_SHIFT & HALF_MOVE_CLOCK_BITS;
    }

    public static final int FULL_MOVE_NUMBER_SHIFT = 18;
    public static final int FULL_MOVE_NUMBER_BITS = 0b1111111111;

    public static int fullMoveNumber(int status) {
        return status >>> FULL_MOVE_NUMBER_SHIFT & FULL_MOVE_NUMBER_BITS;
    }

    public static final String FEN_STARTING_POSITION = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    public static long[] startingPosition() {
        return fromFen(FEN_STARTING_POSITION);
    }

    public static final int CASTLING_SHIFT = 1;
    public static final int CASTLING_BITS = 0b1111;
    public static final int OCCUPANCY_BIT = 0b1000;
    public static final int SQUARE_A1 = 0;
    public static final int SQUARE_A8 = 56;
    public static final int SQUARE_H1 = 7;
    public static final int SQUARE_H8 = 63;

    public static long[] fromFen(String fen) {
        long[] board = new long[MAX_BITBOARDS];
        int[] pieces = Fen.getPieces(fen);
        long board0 = 0;
        long board1 = 0;
        long board2 = 0;
        long board3 = 0;
        for(int square = SQUARE_A1; square <= SQUARE_H8; square ++) {
            int piece = pieces[square];
            if(piece != Value.NONE) {
                long squareBit = 1L << square;
                board0 |= -(piece & 1) & squareBit;
                board1 |= -(piece >>> 1 & 1) & squareBit;
                board2 |= -(piece >>> 2 & 1) & squareBit;
                board3 |= -(piece >>> 3 & 1) & squareBit;
            }
        }
        board[0] = board0;
        board[1] = board1;
        board[2] = board2;
        board[3] = board3;
        int playerToMove = Fen.getWhiteToMove(fen) ? 0 : 1;
        int castling = Fen.getCastling(fen);
        long status = playerToMove ^ castling << CASTLING_SHIFT;
        int eSquare = Fen.getEnPassantSquare(fen);
        if(eSquare != -1) {
            status ^= eSquare << ESQUARE_SHIFT;
        } else {
            eSquare = 0; // Must be 0 here; Zobrist.getKey relies on it for branchless eSquare handling.
        }
        board[STATUS] = status ^ Fen.getHalfMoveClock(fen) << HALF_MOVE_CLOCK_SHIFT ^ Fen.getFullMoveNumber(fen) << FULL_MOVE_NUMBER_SHIFT;
        board[KEY] = Zobrist.getKey(pieces, playerToMove, castling, eSquare);
        return board;
    }

    public static final int PLAYER_SHIFT = 3;
    public static final int START_PIECE_SHIFT = 16;
    public static final int PIECE_BITS = 0b1111;
    public static final int TARGET_SQUARE_SHIFT = 6;
    public static final int TARGET_PIECE_SHIFT = 20;
    public static final int PROMOTE_PIECE_SHIFT = 12;
    public static final int WHITE_CASTLING_BITS = WHITE_KINGSIDE_BIT | WHITE_QUEENSIDE_BIT;
    public static final int BLACK_CASTLING_BITS = BLACK_KINGSIDE_BIT | BLACK_QUEENSIDE_BIT;

    /*
     * Caller supplies newBoard to avoid per-move allocation.
     * Search callers can allocate one board array per ply, for example:
     * private final long[][] boardStack = new long[MAX_PLY + 1][BoardMoveType.MAX_BITBOARDS];
     * Each ply then reuses its board array after the initial allocation.
     */
    public static void makeMoveInto(long board0, long board1, long board2, long board3, int status, long key, long move, long[] newBoard) {
        switch(Move.moveType(move)) {
            case Move.QUIET:
                makeQuietMoveInto(board0, board1, board2, board3, status, key, move, newBoard);
                return;
            case Move.PAWN_PUSH:
                makePawnPushInto(board0, board1, board2, board3, status, key, move, newBoard);
                return;
            case Move.PAWN_DOUBLE_PUSH:
                makePawnDoublePushInto(board0, board1, board2, board3, status, key, move, newBoard);
                return;
            case Move.CASTLE:
                makeCastleMoveInto(board0, board1, board2, board3, status, key, move, newBoard);
                return;
            case Move.CAPTURE:
                makeCaptureMoveInto(board0, board1, board2, board3, status, key, move, newBoard);
                return;
            case Move.EN_PASSANT:
                makeEnPassantMoveInto(board0, board1, board2, board3, status, key, move, newBoard);
                return;
            case Move.PROMOTION:
                makePromotionMoveInto(board0, board1, board2, board3, status, key, move, newBoard);
                return;
            case Move.CAPTURE_PROMOTION:
                makeCapturePromotionMoveInto(board0, board1, board2, board3, status, key, move, newBoard);
                return;
            default:
                return;
        }
    }

    private static void makeQuietMoveInto(long board0, long board1, long board2, long board3, int status, long key, long move, long[] newBoard) {
        final int player = status & PLAYER_BIT;
        final int startSquare = (int) move & SQUARE_BITS;
        final int targetSquare = (int) move >>> TARGET_SQUARE_SHIFT & SQUARE_BITS;
        final int startPiece = (int) move >>> START_PIECE_SHIFT & PIECE_BITS;
        final int startPieceType = startPiece & Piece.TYPE;
        final long pieceMoveBits = (1L << startSquare) | (1L << targetSquare);
        board0 ^= -(startPiece & 1) & pieceMoveBits;
        board1 ^= -(startPiece >>> 1 & 1) & pieceMoveBits;
        board2 ^= -(startPiece >>> 2 & 1) & pieceMoveBits;
        board3 ^= -player & pieceMoveBits;
        key = clearEnPassantHash(status, key)
            ^ Zobrist.PIECE[startPieceType][player][startSquare]
            ^ Zobrist.PIECE[startPieceType][player][targetSquare];
        final int oldCastling = status >>> CASTLING_SHIFT & CASTLING_BITS;
        final int castling = oldCastling & ~Move.castlingChange(move);
        key = updateCastlingHash(key, oldCastling, castling);
        writeMoveResult(
            board0, board1, board2, board3, player, castling, Value.INVALID,
            (status >>> HALF_MOVE_CLOCK_SHIFT & HALF_MOVE_CLOCK_BITS) + 1,
            status >>> FULL_MOVE_NUMBER_SHIFT & FULL_MOVE_NUMBER_BITS, key, newBoard
        );
    }

    private static void makePawnPushInto(long board0, long board1, long board2, long board3, int status, long key, long move, long[] newBoard) {
        final int player = status & PLAYER_BIT;
        final int startSquare = (int) move & SQUARE_BITS;
        final int targetSquare = (int) move >>> TARGET_SQUARE_SHIFT & SQUARE_BITS;
        final long pieceMoveBits = (1L << startSquare) | (1L << targetSquare);
        board1 ^= pieceMoveBits;
        board2 ^= pieceMoveBits;
        board3 ^= -player & pieceMoveBits;
        key = clearEnPassantHash(status, key)
            ^ Zobrist.PIECE[Piece.PAWN][player][startSquare]
            ^ Zobrist.PIECE[Piece.PAWN][player][targetSquare];
        final int oldCastling = status >>> CASTLING_SHIFT & CASTLING_BITS;
        final int castling = oldCastling & ~Move.castlingChange(move);
        key = updateCastlingHash(key, oldCastling, castling);
        writeMoveResult(
            board0, board1, board2, board3, player, castling, Value.INVALID, 0,
            status >>> FULL_MOVE_NUMBER_SHIFT & FULL_MOVE_NUMBER_BITS, key, newBoard
        );
    }

    private static void makePawnDoublePushInto(long board0, long board1, long board2, long board3, int status, long key, long move, long[] newBoard) {
        final int player = status & PLAYER_BIT;
        final int startSquare = (int) move & SQUARE_BITS;
        final int targetSquare = (int) move >>> TARGET_SQUARE_SHIFT & SQUARE_BITS;
        final int eSquare = (startSquare + targetSquare) >>> 1;
        final long pieceMoveBits = (1L << startSquare) | (1L << targetSquare);
        board1 ^= pieceMoveBits;
        board2 ^= pieceMoveBits;
        board3 ^= -player & pieceMoveBits;
        key = clearEnPassantHash(status, key)
            ^ Zobrist.PIECE[Piece.PAWN][player][startSquare]
            ^ Zobrist.PIECE[Piece.PAWN][player][targetSquare]
            ^ Zobrist.ENPASSANT_FILE[eSquare & Value.FILE];
        final int oldCastling = status >>> CASTLING_SHIFT & CASTLING_BITS;
        final int castling = oldCastling & ~Move.castlingChange(move);
        key = updateCastlingHash(key, oldCastling, castling);
        writeMoveResult(
            board0, board1, board2, board3, player, castling, eSquare, 0,
            status >>> FULL_MOVE_NUMBER_SHIFT & FULL_MOVE_NUMBER_BITS, key, newBoard
        );
    }

    private static void makeCastleMoveInto(long board0, long board1, long board2, long board3, int status, long key, long move, long[] newBoard) {
        final int player = status & PLAYER_BIT;
        final int startSquare = (int) move & SQUARE_BITS;
        final int targetSquare = (int) move >>> TARGET_SQUARE_SHIFT & SQUARE_BITS;
        final long kingMoveBits = (1L << startSquare) | (1L << targetSquare);
        final int rookStartSquare;
        final int rookTargetSquare;
        if((targetSquare & Value.FILE) == Value.FILE_G) {
            rookStartSquare = targetSquare + 1;
            rookTargetSquare = targetSquare - 1;
        } else {
            rookStartSquare = targetSquare - 2;
            rookTargetSquare = targetSquare + 1;
        }
        final long rookMoveBits = (1L << rookStartSquare) | (1L << rookTargetSquare);
        board0 ^= kingMoveBits | rookMoveBits;
        board1 ^= rookMoveBits;
        board3 ^= -player & (kingMoveBits | rookMoveBits);
        key = clearEnPassantHash(status, key)
            ^ Zobrist.PIECE[Piece.KING][player][startSquare]
            ^ Zobrist.PIECE[Piece.KING][player][targetSquare]
            ^ Zobrist.PIECE[Piece.ROOK][player][rookStartSquare]
            ^ Zobrist.PIECE[Piece.ROOK][player][rookTargetSquare];
        final int oldCastling = status >>> CASTLING_SHIFT & CASTLING_BITS;
        final int castling = oldCastling & ~Move.castlingChange(move);
        key = updateCastlingHash(key, oldCastling, castling);
        writeMoveResult(
            board0, board1, board2, board3, player, castling, Value.INVALID,
            (status >>> HALF_MOVE_CLOCK_SHIFT & HALF_MOVE_CLOCK_BITS) + 1,
            status >>> FULL_MOVE_NUMBER_SHIFT & FULL_MOVE_NUMBER_BITS, key, newBoard
        );
    }

    private static void makeCaptureMoveInto(long board0, long board1, long board2, long board3, int status, long key, long move, long[] newBoard) {
        final int player = status & PLAYER_BIT;
        final int other = 1 ^ player;
        final int startSquare = (int) move & SQUARE_BITS;
        final int targetSquare = (int) move >>> TARGET_SQUARE_SHIFT & SQUARE_BITS;
        final int startPiece = (int) move >>> START_PIECE_SHIFT & PIECE_BITS;
        final int targetPiece = (int) move >>> TARGET_PIECE_SHIFT & PIECE_BITS;
        final int startPieceType = startPiece & Piece.TYPE;
        final long targetSquareBit = 1L << targetSquare;
        board0 ^= -(targetPiece & 1) & targetSquareBit;
        board1 ^= -(targetPiece >>> 1 & 1) & targetSquareBit;
        board2 ^= -(targetPiece >>> 2 & 1) & targetSquareBit;
        board3 ^= ~(-player) & targetSquareBit;
        final long pieceMoveBits = (1L << startSquare) | targetSquareBit;
        board0 ^= -(startPiece & 1) & pieceMoveBits;
        board1 ^= -(startPiece >>> 1 & 1) & pieceMoveBits;
        board2 ^= -(startPiece >>> 2 & 1) & pieceMoveBits;
        board3 ^= -player & pieceMoveBits;
        key = clearEnPassantHash(status, key)
            ^ Zobrist.PIECE[targetPiece & Piece.TYPE][other][targetSquare]
            ^ Zobrist.PIECE[startPieceType][player][startSquare]
            ^ Zobrist.PIECE[startPieceType][player][targetSquare];
        final int oldCastling = status >>> CASTLING_SHIFT & CASTLING_BITS;
        final int castling = oldCastling & ~Move.castlingChange(move);
        key = updateCastlingHash(key, oldCastling, castling);
        writeMoveResult(
            board0, board1, board2, board3, player, castling, Value.INVALID, 0,
            status >>> FULL_MOVE_NUMBER_SHIFT & FULL_MOVE_NUMBER_BITS, key, newBoard
        );
    }

    private static void makeEnPassantMoveInto(long board0, long board1, long board2, long board3, int status, long key, long move, long[] newBoard) {
        final int player = status & PLAYER_BIT;
        final int other = 1 ^ player;
        final int startSquare = (int) move & SQUARE_BITS;
        final int targetSquare = (int) move >>> TARGET_SQUARE_SHIFT & SQUARE_BITS;
        final int captureSquare = targetSquare + (player == 0 ? -8 : 8);
        final long pieceMoveBits = (1L << startSquare) | (1L << targetSquare);
        final long captureSquareBit = 1L << captureSquare;
        board1 ^= pieceMoveBits | captureSquareBit;
        board2 ^= pieceMoveBits | captureSquareBit;
        board3 ^= (-player & pieceMoveBits) | (~(-player) & captureSquareBit);
        key = clearEnPassantHash(status, key)
            ^ Zobrist.PIECE[Piece.PAWN][player][startSquare]
            ^ Zobrist.PIECE[Piece.PAWN][player][targetSquare]
            ^ Zobrist.PIECE[Piece.PAWN][other][captureSquare];
        final int oldCastling = status >>> CASTLING_SHIFT & CASTLING_BITS;
        final int castling = oldCastling & ~Move.castlingChange(move);
        key = updateCastlingHash(key, oldCastling, castling);
        writeMoveResult(
            board0, board1, board2, board3, player, castling, Value.INVALID, 0,
            status >>> FULL_MOVE_NUMBER_SHIFT & FULL_MOVE_NUMBER_BITS, key, newBoard
        );
    }

    private static void makePromotionMoveInto(long board0, long board1, long board2, long board3, int status, long key, long move, long[] newBoard) {
        final int player = status & PLAYER_BIT;
        final int startSquare = (int) move & SQUARE_BITS;
        final int targetSquare = (int) move >>> TARGET_SQUARE_SHIFT & SQUARE_BITS;
        final int promotePiece = (int) move >>> PROMOTE_PIECE_SHIFT & PIECE_BITS;
        final long startSquareBit = 1L << startSquare;
        final long targetSquareBit = 1L << targetSquare;
        board0 ^= -(promotePiece & 1) & targetSquareBit;
        board1 ^= startSquareBit | (-(promotePiece >>> 1 & 1) & targetSquareBit);
        board2 ^= startSquareBit | (-(promotePiece >>> 2 & 1) & targetSquareBit);
        board3 ^= -player & (startSquareBit | targetSquareBit);
        key = clearEnPassantHash(status, key)
            ^ Zobrist.PIECE[Piece.PAWN][player][startSquare]
            ^ Zobrist.PIECE[promotePiece & Piece.TYPE][player][targetSquare];
        final int oldCastling = status >>> CASTLING_SHIFT & CASTLING_BITS;
        final int castling = oldCastling & ~Move.castlingChange(move);
        key = updateCastlingHash(key, oldCastling, castling);
        writeMoveResult(
            board0, board1, board2, board3, player, castling, Value.INVALID, 0,
            status >>> FULL_MOVE_NUMBER_SHIFT & FULL_MOVE_NUMBER_BITS, key, newBoard
        );
    }

    private static void makeCapturePromotionMoveInto(long board0, long board1, long board2, long board3, int status, long key, long move, long[] newBoard) {
        final int player = status & PLAYER_BIT;
        final int other = 1 ^ player;
        final int startSquare = (int) move & SQUARE_BITS;
        final int targetSquare = (int) move >>> TARGET_SQUARE_SHIFT & SQUARE_BITS;
        final int targetPiece = (int) move >>> TARGET_PIECE_SHIFT & PIECE_BITS;
        final int promotePiece = (int) move >>> PROMOTE_PIECE_SHIFT & PIECE_BITS;
        final long startSquareBit = 1L << startSquare;
        final long targetSquareBit = 1L << targetSquare;
        board0 ^= -(targetPiece & 1) & targetSquareBit;
        board1 ^= -(targetPiece >>> 1 & 1) & targetSquareBit;
        board2 ^= -(targetPiece >>> 2 & 1) & targetSquareBit;
        board3 ^= ~(-player) & targetSquareBit;
        board0 ^= -(promotePiece & 1) & targetSquareBit;
        board1 ^= startSquareBit | (-(promotePiece >>> 1 & 1) & targetSquareBit);
        board2 ^= startSquareBit | (-(promotePiece >>> 2 & 1) & targetSquareBit);
        board3 ^= -player & (startSquareBit | targetSquareBit);
        key = clearEnPassantHash(status, key)
            ^ Zobrist.PIECE[targetPiece & Piece.TYPE][other][targetSquare]
            ^ Zobrist.PIECE[Piece.PAWN][player][startSquare]
            ^ Zobrist.PIECE[promotePiece & Piece.TYPE][player][targetSquare];
        final int oldCastling = status >>> CASTLING_SHIFT & CASTLING_BITS;
        final int castling = oldCastling & ~Move.castlingChange(move);
        key = updateCastlingHash(key, oldCastling, castling);
        writeMoveResult(
            board0, board1, board2, board3, player, castling, Value.INVALID, 0,
            status >>> FULL_MOVE_NUMBER_SHIFT & FULL_MOVE_NUMBER_BITS, key, newBoard
        );
    }

    private static long clearEnPassantHash(int status, long key) {
        final int eSquare = enPassantSquare(status);
        return eSquare == Value.INVALID ? key : key ^ Zobrist.ENPASSANT_FILE[eSquare & Value.FILE];
    }

    private static long updateCastlingHash(long key, int oldCastling, int newCastling) {
        if(oldCastling == newCastling) return key;
        final int removed = oldCastling & ~newCastling;
        if((removed & WHITE_KINGSIDE_BIT) != 0) key ^= Zobrist.CASTLING[0][Value.KING_SIDE];
        if((removed & WHITE_QUEENSIDE_BIT) != 0) key ^= Zobrist.CASTLING[0][Value.QUEEN_SIDE];
        if((removed & BLACK_KINGSIDE_BIT) != 0) key ^= Zobrist.CASTLING[1][Value.KING_SIDE];
        if((removed & BLACK_QUEENSIDE_BIT) != 0) key ^= Zobrist.CASTLING[1][Value.QUEEN_SIDE];
        return key;
    }

    private static void writeMoveResult(
        long board0, long board1, long board2, long board3,
        int player, int castling, int eSquare, int halfMoveClock, int fullMoveNumber,
        long key, long[] newBoard
    ) {
        newBoard[0] = board0;
        newBoard[1] = board1;
        newBoard[2] = board2;
        newBoard[3] = board3;
        newBoard[STATUS] =
            (1 ^ player) |
            (castling << CASTLING_SHIFT) |
            (eSquare != Value.INVALID ? (eSquare << ESQUARE_SHIFT) : 0) |
            (halfMoveClock << HALF_MOVE_CLOCK_SHIFT) |
            ((fullMoveNumber + player) << FULL_MOVE_NUMBER_SHIFT);
        newBoard[KEY] = key ^ Zobrist.WHITEMOVE;
    }

    public static final long ENPASSANT_RESET_BITS = ~(SQUARE_BITS << ESQUARE_SHIFT);

    public static void nullMoveInto(long board0, long board1, long board2, long board3, int status, long key, long[] newBoard) {
        int eSquare = status >>> ESQUARE_SHIFT & SQUARE_BITS;
        if(eSquare > 0) key ^= Zobrist.ENPASSANT_FILE[eSquare & Value.FILE];
        key ^= Zobrist.WHITEMOVE;
        newBoard[0] = board0;
        newBoard[1] = board1;
        newBoard[2] = board2;
        newBoard[3] = board3;
        newBoard[STATUS] = (status ^ PLAYER_BIT) & ENPASSANT_RESET_BITS;
        newBoard[KEY] = key;
    }

    public static int getSquare(long board0, long board1, long board2, long board3, int square) {
        return (int) (((board3 >>> square & 1) << 3) |
                      ((board2 >>> square & 1) << 2) |
                      ((board1 >>> square & 1) << 1) |
                       (board0 >>> square & 1));
    }

    public static boolean isSquareAttackedByPlayer(long board0, long board1, long board2, long board3, int square, int player) {
        final int other = 1 ^ player;
        final long colorMask = -(other) ^ board3;
        if((LEAP_ATTACKS[square] & board0 & ~board1 & board2 & colorMask) != 0) return true;
        if((PAWN_ATTACKS[other][square] & ~board0 & board1 & board2 & colorMask) != 0L) return true;
        if((KING_ATTACKS[square] & board0 & ~board1 & ~board2 & colorMask) != 0L) return true;
        final long allOccupancy = board0 | board1| board2;
        if((Magic.bishopMoves(square, allOccupancy) & ~board0 & (board1 ^ board2) & colorMask) != 0L) return true;
        if((Magic.rookMoves  (square, allOccupancy) & board1 & ~board2 & colorMask) != 0L) return true;
        return false;
    }

    public static boolean isPlayerInCheck(long board0, long board1, long board2, long board3, int player) {
        final long colorMask = -(player) ^ board3;
        final long bitboard = board0 & ~board1 & ~board2 & ~colorMask;
        final int square = LSB[(int) (((bitboard & -bitboard) * DB) >>> 58)];
        if((LEAP_ATTACKS[square] & board0 & ~board1 & board2 & colorMask) != 0L) return true;
        if((PAWN_ATTACKS[player][square] & ~board0 & board1 & board2 & colorMask) != 0L) return true;
        if((KING_ATTACKS[square] & board0 & ~board1 & ~board2 & colorMask) != 0L) return true;
        final long allOccupancy = board0 | board1| board2;
        if((Magic.bishopMoves(square, allOccupancy) & ~board0 & (board1 ^ board2) & colorMask) != 0L) return true;
        if((Magic.rookMoves  (square, allOccupancy) & board1 & ~board2 & colorMask) != 0L) return true;
        return false;
    }

    public static boolean isPlayerInCheckPext(long board0, long board1, long board2, long board3, int player) {
        final long colorMask = -(player) ^ board3;
        final long bitboard = board0 & ~board1 & ~board2 & ~colorMask;
        final int square = LSB[(int) (((bitboard & -bitboard) * DB) >>> 58)];
        if((LEAP_ATTACKS[square] & board0 & ~board1 & board2 & colorMask) != 0L) return true;
        if((PAWN_ATTACKS[player][square] & ~board0 & board1 & board2 & colorMask) != 0L) return true;
        if((KING_ATTACKS[square] & board0 & ~board1 & ~board2 & colorMask) != 0L) return true;
        final long allOccupancy = board0 | board1| board2;
        if((Pext.bishopMoves(square, allOccupancy) & ~board0 & (board1 ^ board2) & colorMask) != 0L) return true;
        if((Pext.rookMoves  (square, allOccupancy) & board1 & ~board2 & colorMask) != 0L) return true;
        return false;
    }

    public static long getCheckers(long board0, long board1, long board2, long board3, long colorMask, int player, int kingSquare, long allOccupancy) {
        final long otherKing = board0 & ~board1 & ~board2 & ~colorMask;
        final long otherQueens = ~board0 & board1 & ~board2 & ~colorMask;
        final long otherRooks = board0 & board1 & ~board2 & ~colorMask;
        final long otherBishops = ~board0 & ~board1 & board2 & ~colorMask;
        final long otherKnights = board0 & ~board1 & board2 & ~colorMask;
        final long otherPawns = ~board0 & board1 & board2 & ~colorMask;
        return  (LEAP_ATTACKS[kingSquare] & otherKnights) |
                (PAWN_ATTACKS[player][kingSquare] & otherPawns) |
                (KING_ATTACKS[kingSquare] & otherKing) |
                (Magic.rookMoves(kingSquare, allOccupancy) & (otherQueens | otherRooks)) |
                (Magic.bishopMoves(kingSquare, allOccupancy) & (otherQueens | otherBishops));
    }

    public static long getCheckersPext(long board0, long board1, long board2, long board3, long colorMask, int player, int kingSquare, long allOccupancy) {
        final long otherKing = board0 & ~board1 & ~board2 & ~colorMask;
        final long otherQueens = ~board0 & board1 & ~board2 & ~colorMask;
        final long otherRooks = board0 & board1 & ~board2 & ~colorMask;
        final long otherBishops = ~board0 & ~board1 & board2 & ~colorMask;
        final long otherKnights = board0 & ~board1 & board2 & ~colorMask;
        final long otherPawns = ~board0 & board1 & board2 & ~colorMask;
        return  (LEAP_ATTACKS[kingSquare] & otherKnights) |
                (PAWN_ATTACKS[player][kingSquare] & otherPawns) |
                (KING_ATTACKS[kingSquare] & otherKing) |
                (Pext.rookMoves(kingSquare, allOccupancy) & (otherQueens | otherRooks)) |
                (Pext.bishopMoves(kingSquare, allOccupancy) & (otherQueens | otherBishops));
    }

    public static String squareToString(int square) {
        return Value.FILE_STRING.charAt(square & Value.FILE) + Integer.toString((square >>> 3) + 1);
    }

    public static String boardString(long board0, long board1, long board2, long board3) {
        StringBuilder boardString = new StringBuilder();
        for(int i = BoardMoveType.SQUARE_A1; i <= BoardMoveType.SQUARE_H8; i ++) {
            int square = i ^ 0x38;
            int piece = BoardMoveType.getSquare(board0, board1, board2, board3, square);
            boardString.append((piece != Value.NONE ? Piece.SHORT_STRING[piece] : ".")).append((i & 7) == 7 ? "\n" : " ");
        }
        return boardString.toString();
    }

    public static boolean equals(long[] board1, long[] board2, boolean includeCounters) {
        for(int i = 0; i < 4; i ++) {
            if(board1[i] != board2[i]) return false;
        }
        int status1 = (int) board1[STATUS];
        int status2 = (int) board2[STATUS];
        if(includeCounters & (status1 != status2)) return false;
        if((status1 & 0b11111111111) != (status2 & 0b11111111111)) return false;
        return true;
    }

    // Share the production lookup table instead of duplicating its mutable array.
    public static final int[] LSB = Board.LSB;

	public static final long DB = Board.DB;

    private static final long[] LEAP_ATTACKS = new long[64];
    private static final long[] KING_ATTACKS = new long[64];
    static {
        for(int square = SQUARE_A1; square <= SQUARE_H8; square ++) {
            LEAP_ATTACKS[square] = Bitboard.BB[Bitboard.LEAP_ATTACKS][square];
            KING_ATTACKS[square] = Bitboard.BB[Bitboard.KING_ATTACKS][square];
        }
    }
    private static final long[][] PAWN_ATTACKS = new long[2][64];
    static {
        for(int player = 0; player < 2; player ++) {
            for(int square = 0; square < 64; square ++) {
                PAWN_ATTACKS[player][square] = Bitboard.BB[Bitboard.PAWN_ATTACKS + player][square];
            }
        }
    }

    private BoardMoveType() {}
    

}
