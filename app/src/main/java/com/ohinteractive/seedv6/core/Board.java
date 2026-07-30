package com.ohinteractive.seedv6.core;

import com.ohinteractive.seedv6.core.util.Bitboard;
import com.ohinteractive.seedv6.core.util.Fen;
import com.ohinteractive.seedv6.core.util.Magic;
import com.ohinteractive.seedv6.core.util.Pext;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.core.util.Zobrist;

public class Board {

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
     * private final long[][] boardStack = new long[MAX_PLY + 1][Board.MAX_BITBOARDS];
     * Each ply then reuses its board array after the initial allocation.
     */
    public static void makeMoveInto(long board0, long board1, long board2, long board3, int status, long key, long move, long[] newBoard) {
        // No arraycopy is needed; every newBoard slot is written before return.
        int castling = status >>> CASTLING_SHIFT & CASTLING_BITS;
        final int player = status & PLAYER_BIT;
        final int other = 1 ^ player;
        final long blackMask = -player;
        final long whiteMask = ~blackMask;
        int eSquare = status >>> ESQUARE_SHIFT & SQUARE_BITS;
        eSquare = ((1L << eSquare) & ((WHITE_ENPASSANT_SQUARES & ~(-player)) | (BLACK_ENPASSANT_SQUARES & -player))) != 0L ? eSquare : Value.INVALID;
        final int originalESquare = eSquare;
        int halfMoveClock = (status >>> HALF_MOVE_CLOCK_SHIFT & HALF_MOVE_CLOCK_BITS) + 1;
        int fullMoveNumber = status >>> FULL_MOVE_NUMBER_SHIFT & FULL_MOVE_NUMBER_BITS;
        final int startSquare = (int) move & SQUARE_BITS;
        final int startPiece = (int) move >>> START_PIECE_SHIFT & PIECE_BITS;
        final int startPieceType = startPiece & Piece.TYPE;
        final int targetSquare = (int) move >>> TARGET_SQUARE_SHIFT & SQUARE_BITS;
        final int targetPiece = (int) move >>> TARGET_PIECE_SHIFT & PIECE_BITS;
        final int targetPieceType = targetPiece & Piece.TYPE;
        final long targetSquareBit = 1L << targetSquare;
        final int squareDiff = startSquare - targetSquare;
        final int squareDiffSign = squareDiff >> 31;
        final int squareDiffAbs = (squareDiff ^ squareDiffSign) - squareDiffSign;
        if(eSquare != Value.INVALID) {
            key ^= Zobrist.ENPASSANT_FILE[eSquare & Value.FILE];
            eSquare = Value.INVALID;
        }
        if(targetPiece != Value.NONE) {
            halfMoveClock = 0;
            board0 ^= -(targetPiece & 1) & targetSquareBit;
            board1 ^= -(targetPiece >>> 1 & 1) & targetSquareBit;
            board2 ^= -(targetPiece >>> 2 & 1) & targetSquareBit;
            board3 ^= whiteMask & targetSquareBit;
            key ^= Zobrist.PIECE[targetPieceType][other][targetSquare];
        }
        switch(startPieceType) {
            case Piece.QUEEN:
            case Piece.BISHOP:
            case Piece.KNIGHT: {
                final long pieceMoveBits = (1L << startSquare) | targetSquareBit;
                board0 ^= -(startPiece & 1) & pieceMoveBits;
                board1 ^= -(startPiece >>> 1 & 1) & pieceMoveBits;
                board2 ^= -(startPiece >>> 2 & 1) & pieceMoveBits;
                board3 ^= blackMask & pieceMoveBits;
                key ^= Zobrist.PIECE[startPieceType][player][startSquare]
                    ^  Zobrist.PIECE[startPieceType][player][targetSquare];
                break;
            }
            case Piece.KING: {
                final long pieceMoveBits = (1L << startSquare) | targetSquareBit;
                board0 ^= pieceMoveBits;
                board3 ^= blackMask & pieceMoveBits;
                key ^= Zobrist.PIECE[Piece.KING][player][startSquare]
                    ^  Zobrist.PIECE[Piece.KING][player][targetSquare];
                final boolean playerKingSideCastling  = (castling & ((WHITE_KINGSIDE_BIT  & whiteMask) | (BLACK_KINGSIDE_BIT  & blackMask))) != 0;
                final boolean playerQueenSideCastling = (castling & ((WHITE_QUEENSIDE_BIT & whiteMask) | (BLACK_QUEENSIDE_BIT & blackMask))) != 0;
                if(playerKingSideCastling || playerQueenSideCastling) {
                    key ^= (playerKingSideCastling  ? Zobrist.CASTLING[player][Value.KING_SIDE]  : 0)
                        ^  (playerQueenSideCastling ? Zobrist.CASTLING[player][Value.QUEEN_SIDE] : 0);
                    castling &= ~((WHITE_CASTLING_BITS & whiteMask) | (BLACK_CASTLING_BITS & blackMask));
                }
                if(squareDiffAbs == 2) {
                    final long rookMoveBits;
                    if((targetSquare & Value.FILE) == Value.FILE_G) {
                        rookMoveBits = (1L << (targetSquare + 1)) | (1L << (targetSquare - 1));
                        key ^= Zobrist.PIECE[Piece.ROOK][player][targetSquare + 1]
                            ^  Zobrist.PIECE[Piece.ROOK][player][targetSquare - 1];
                    } else {
                        rookMoveBits = (1L << (targetSquare - 2)) | (1L << (targetSquare + 1));
                        key ^= Zobrist.PIECE[Piece.ROOK][player][targetSquare - 2]
                            ^  Zobrist.PIECE[Piece.ROOK][player][targetSquare + 1];
                    }
                    board0 ^= rookMoveBits;
                    board1 ^= rookMoveBits;
                    board3 ^= blackMask & rookMoveBits;
                }
                break;
            }
            case Piece.ROOK: {
                final long pieceMoveBits = (1L << startSquare) | targetSquareBit;
                board0 ^= pieceMoveBits;
                board1 ^= pieceMoveBits;
                board3 ^= blackMask & pieceMoveBits;
                key ^= Zobrist.PIECE[Piece.ROOK][player][startSquare]
                    ^  Zobrist.PIECE[Piece.ROOK][player][targetSquare];
                final long kingSideBit = Value.KINGSIDE_BIT[player];
                final long queenSideBit = Value.QUEENSIDE_BIT[player];
                if((castling & kingSideBit) != Value.NONE) {
                    if(startSquare == ((SQUARE_H1 & whiteMask) | (SQUARE_H8 & blackMask))) {
                        castling ^= kingSideBit;
                        key ^= Zobrist.CASTLING[player][Value.KING_SIDE];
                    }
                }
                if((castling & queenSideBit) != Value.NONE) {
                    if(startSquare == ((SQUARE_A1 & whiteMask) | (SQUARE_A8 & blackMask))) {
                        castling ^= queenSideBit;
                        key ^= Zobrist.CASTLING[player][Value.QUEEN_SIDE];
                    }
                }
                break;
            }
            case Piece.PAWN: {
                final int promotePiece = (int) move >>> PROMOTE_PIECE_SHIFT & PIECE_BITS;
                halfMoveClock = 0;
                if(promotePiece == Value.NONE) {
                    final long pieceMoveBits = (1L << startSquare) | targetSquareBit;
                    board1 ^= pieceMoveBits;
                    board2 ^= pieceMoveBits;
                    board3 ^= blackMask & pieceMoveBits;
                    key ^= Zobrist.PIECE[startPieceType][player][startSquare]
                        ^  Zobrist.PIECE[startPieceType][player][targetSquare];
                } else {
                    final long startSquareBit = 1L << startSquare;
                    board0 ^= -(promotePiece & 1) & targetSquareBit;
                    board1 ^= startSquareBit | (-(promotePiece >>> 1 & 1) & targetSquareBit);
                    board2 ^= startSquareBit | (-(promotePiece >>> 2 & 1) & targetSquareBit);
                    board3 ^= blackMask & (startSquareBit | targetSquareBit);
                    key ^= Zobrist.PIECE[startPieceType][player][startSquare]
                        ^  Zobrist.PIECE[promotePiece & Piece.TYPE][player][targetSquare];
                }
                if(targetSquare == originalESquare) {
                    final int captureSquare = targetSquare + (int) ((-8 & whiteMask) | (8 & blackMask));
                    final long captureSquareBit = 1L << captureSquare;
                    board1 ^= captureSquareBit;
                    board2 ^= captureSquareBit;
                    board3 ^= whiteMask & captureSquareBit;
                    key ^= Zobrist.PIECE[Piece.PAWN][other][captureSquare];
                }
                if(squareDiffAbs == 16) {
                    eSquare = startSquare + (int) ((8 & whiteMask) | (-8 & blackMask));
                    key ^= Zobrist.ENPASSANT_FILE[eSquare & Value.FILE];
                }
                break;
            }
            default: break;
        }
        if(targetPieceType == Piece.ROOK) {
            if((castling & ((WHITE_KINGSIDE_BIT & blackMask) | (BLACK_KINGSIDE_BIT & whiteMask))) != Value.NONE) {
                if(targetSquare == ((SQUARE_H1 & blackMask) | (SQUARE_H8 & whiteMask))) {
                    castling ^= ((WHITE_KINGSIDE_BIT & blackMask) | (BLACK_KINGSIDE_BIT & whiteMask));
                    key ^= Zobrist.CASTLING[other][Value.KING_SIDE];
                }
            }
            if((castling & ((WHITE_QUEENSIDE_BIT & blackMask) | (BLACK_QUEENSIDE_BIT & whiteMask))) != Value.NONE) {
                if(targetSquare == ((SQUARE_A1 & blackMask) | (SQUARE_A8 & whiteMask))) {
                    castling ^= ((WHITE_QUEENSIDE_BIT & blackMask) | (BLACK_QUEENSIDE_BIT & whiteMask));
                    key ^= Zobrist.CASTLING[other][Value.QUEEN_SIDE];
                }
            }
        }
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
        for(int i = Board.SQUARE_A1; i <= Board.SQUARE_H8; i ++) {
            int square = i ^ 0x38;
            int piece = Board.getSquare(board0, board1, board2, board3, square);
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

    public static final int[] LSB = {
        0,  1, 48,  2, 57, 49, 28,  3,
		61, 58, 50, 42, 38, 29, 17,  4,
		62, 55, 59, 36, 53, 51, 43, 22,
		45, 39, 33, 30, 24, 18, 12,  5,
		63, 47, 56, 27, 60, 41, 37, 16,
		54, 35, 52, 21, 44, 32, 23, 11,
		46, 26, 40, 15, 34, 20, 31, 10,
		25, 14, 19,  9, 13,  8,  7,  6
    };

	public static final long DB = 0x03f79d71b4cb0a89L;

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

    private Board() {}
    

}