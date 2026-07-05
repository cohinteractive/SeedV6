package com.ohinteractive.seedv6.core.move;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.util.Bitboard;
import com.ohinteractive.seedv6.core.util.Magic;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;

public class Move {
    
    public static String string(long move) {
        int promotePiece = (int) move >>> Board.PROMOTE_PIECE_SHIFT & Board.PIECE_BITS;
        return Board.squareToString((int) move & Board.SQUARE_BITS) + Board.squareToString((int) move >>> Board.TARGET_SQUARE_SHIFT & Board.SQUARE_BITS)
                + (promotePiece == Value.NONE ? "" : Piece.SHORT_STRING[promotePiece].toUpperCase());
    }

    public static String notation(long[] board, long move) {
        final long board0 = board[0];
        final long board1 = board[1];
        final long board2 = board[2];
        final long board3 = board[3];
        final int status = (int) board[Board.STATUS];
        int startSquare = (int) move & Board.SQUARE_BITS;
        int startFile = startSquare & Value.FILE;
        int startRank = startSquare >>> 3;
        int targetSquare = (int) move >>> Board.TARGET_SQUARE_SHIFT & Board.SQUARE_BITS;
        int targetFile = targetSquare & Value.FILE;
        int targetRank = targetSquare >>> 3;
        int startPiece = (int) move >>> Board.START_PIECE_SHIFT & Board.PIECE_BITS;
        int player = startPiece >>> 3;
        final long colorMask = ~(-(player & 1) ^ board3);
        long pieceBitboard = (-(startPiece & 1) & board0) & (-(startPiece >>> 1 & 1) & board1) & (-(startPiece >>> 2 & 1) & board2) & colorMask;
        int startType = startPiece & Piece.TYPE;
        int targetPiece = (int) move >>> Board.TARGET_PIECE_SHIFT & Board.PIECE_BITS;
        int promotePiece = (int) move >>> Board.PROMOTE_PIECE_SHIFT & Board.PIECE_BITS;
        long allOccupancy = board0 | board1 | board2;
        String notation = "";
        switch (startType) {
            case Piece.KING: {
                if (Math.abs(startSquare - targetSquare) == 2) {
                    return "O-O" + (targetFile == Value.FILE_G ? "" : "-O");
                }
                notation = "K";
                break;
            }
            case Piece.QUEEN: {
                notation = "Q";
                long queensAttackTargetSquare = Magic.queenMoves(targetSquare, allOccupancy) & pieceBitboard;
                if (queensAttackTargetSquare > 1L) {
                    int queensOnFile = Long.bitCount(queensAttackTargetSquare & Bitboard.BB[Bitboard.FILE][targetFile]);
                    int queensOnRank = Long.bitCount(queensAttackTargetSquare & Bitboard.BB[Bitboard.RANK][targetRank]);
                    int queensOnDiagonals = Long
                            .bitCount(queensAttackTargetSquare & (Bitboard.BB[Bitboard.DIAGONAL_ATTACKS][targetSquare]));
                    if (queensOnRank > 1) {
                        notation += Value.FILE_STRING.charAt(startFile);
                    }
                    if (queensOnFile > 1) {
                        notation += Integer.toString(startRank + 1);
                    }
                    if (notation.length() == 1 && queensOnDiagonals > 1) {
                        notation += Value.FILE_STRING.charAt(startFile);
                    }
                }
                break;
            }
            case Piece.ROOK: {
                notation = "R";
                long rooksAttackTargetSquare = Magic.rookMoves(targetSquare, allOccupancy) & pieceBitboard;
                if (rooksAttackTargetSquare > 1L) {
                    int rooksOnFile = Long.bitCount(rooksAttackTargetSquare & Bitboard.BB[Bitboard.FILE][targetFile]);
                    int rooksOnRank = Long.bitCount(rooksAttackTargetSquare & Bitboard.BB[Bitboard.RANK][targetRank]);
                    if (rooksOnRank > 1) {
                        notation += Value.FILE_STRING.charAt(startFile);
                    }
                    if (rooksOnFile > 1) {
                        notation += Integer.toString(startRank + 1);
                    }
                }
                break;
            }
            case Piece.BISHOP: {
                notation = "B";
                long bishopsAttackTargetSquare = Magic.bishopMoves(targetSquare, allOccupancy) & pieceBitboard;
                if (bishopsAttackTargetSquare > 1L) {
                    int bishopsOnFile = Long.bitCount(bishopsAttackTargetSquare & Bitboard.BB[Bitboard.FILE][targetFile]);
                    int bishopsOnRank = Long.bitCount(bishopsAttackTargetSquare & Bitboard.BB[Bitboard.RANK][targetRank]);
                    int bishopsOnDiagonals = Long
                            .bitCount(bishopsAttackTargetSquare & (Bitboard.BB[Bitboard.DIAGONAL_ATTACKS][targetSquare]));
                    if (bishopsOnRank > 1) {
                        notation += Value.FILE_STRING.charAt(startFile);
                    }
                    if (bishopsOnFile > 1) {
                        notation += Integer.toString(startRank + 1);
                    }
                    if (notation.length() == 1 && bishopsOnDiagonals > 1) {
                        notation += Value.FILE_STRING.charAt(startFile);
                    }
                }
                break;
            }
            case Piece.KNIGHT: {
                notation = "N";
                if (Long.bitCount(Bitboard.BB[Bitboard.LEAP_ATTACKS][targetSquare] & pieceBitboard) > 1) {
                    if (Long.bitCount(Bitboard.BB[Bitboard.RANK][startRank] & pieceBitboard) > 1) {
                        notation += Value.FILE_STRING.charAt(startFile);
                    }
                    if (Long.bitCount(Bitboard.BB[Bitboard.FILE][startFile] & pieceBitboard) > 1) {
                        notation += Integer.toString(startRank + 1);
                    }
                }
                break;
            }
            case Piece.PAWN:
            default: {
                notation = "";
                break;
            }
        }
        if (targetPiece != Value.NONE || targetSquare == Board.enPassantSquare(status)) {
            if (startType == Piece.PAWN) {
                notation += Value.FILE_STRING.charAt(startFile);
            }
            notation += "x";
        }
        notation += Board.squareToString(targetSquare);
        if (promotePiece != Value.NONE) {
            notation += "=";
            switch (promotePiece & Piece.TYPE) {
                case Piece.QUEEN:
                    notation += "Q";
                    break;
                case Piece.ROOK:
                    notation += "R";
                    break;
                case Piece.BISHOP:
                    notation += "B";
                    break;
                case Piece.KNIGHT:
                    notation += "N";
                    break;
            }
        }
        final long[] tempBoard = new long[Board.MAX_BITBOARDS];
        Board.makeMoveInto(board[0], board[1], board[2], board[3], status, board[Board.KEY], move, tempBoard);
        if (Board.isPlayerInCheck(tempBoard[0], tempBoard[1], tempBoard[2], tempBoard[3], 1 ^ player)) {
            int moveCount = Gen.genAll(tempBoard[0], tempBoard[1], tempBoard[2], tempBoard[3], status, tempBoard[Board.KEY], true, new long[256], new long[Board.MAX_BITBOARDS]);
            if (moveCount == 0) {
                notation += "#";
            } else {
                notation += "+";
            }
        }
        return notation;
    }

}
