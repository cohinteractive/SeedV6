package com.ohinteractive.seedv6.core.util;

import java.util.Objects;

public class Fen {

    public static int[] getPieces(String fen) {
        final String fenPieces = fields(fen)[0];
        final String[] ranks = fenPieces.split("/", -1);
        if(ranks.length != 8) throw invalid("piece placement must contain eight ranks");
        int[] squares = new int[64];
        for(int rankIndex = 0; rankIndex < ranks.length; rankIndex ++) {
            int file = 0;
            for(char c : ranks[rankIndex].toCharArray()) {
                if(c >= '1' && c <= '8') {
                    file += c - '0';
                } else {
                    final int piece = PIECE_STRING.indexOf(c);
                    if(piece <= 0) throw invalid("unknown piece: " + c);
                    if(file >= 8) throw invalid("rank contains more than eight squares");
                    squares[(7 - rankIndex) * 8 + file] = piece;
                    file ++;
                }
                if(file > 8) throw invalid("rank contains more than eight squares");
            }
            if(file != 8) throw invalid("each rank must contain eight squares");
        }
        return squares;
    }

    public static boolean getWhiteToMove(String fen) {
        final String side = fields(fen)[1];
        if(!side.equals("w") && !side.equals("b")) throw invalid("side to move must be w or b");
        return side.equals("w");
    }

    public static int getCastling(String fen) {
        final String fenCastlingString = fields(fen)[2];
        if(fenCastlingString.equals("-")) return 0;
        if(fenCastlingString.isEmpty() || fenCastlingString.length() > 4) {
            throw invalid("invalid castling field");
        }
        int castling = 0;
        for(char c : fenCastlingString.toCharArray()) {
            int castlingCharIndex = CASTLING_STRING.indexOf(c);
            if(castlingCharIndex == -1) throw invalid("invalid castling right: " + c);
            final int right = 1 << castlingCharIndex;
            if((castling & right) != 0) throw invalid("duplicate castling right: " + c);
            castling |= right;
        }
        return castling;
    }

    public static int getEnPassantSquare(String fen) {
        final String enPassant = fields(fen)[3];
        if(enPassant.equals("-")) return -1;
        if(enPassant.length() != 2) throw invalid("invalid en-passant square");
        int file = FILE_STRING.indexOf(enPassant.charAt(0));
        if(file == -1) throw invalid("invalid en-passant file");
        int rank = enPassant.charAt(1) - '1';
        if(rank < 0 || rank > 7) throw invalid("invalid en-passant rank");
        int eSquare = rank << 3 | file;
        int playerToMove = getWhiteToMove(fen) ? 0 : 1;
        if((playerToMove == 0 && rank != 5) || (playerToMove == 1 && rank != 2)) {
            throw invalid("en-passant rank is inconsistent with side to move");
        }
        return eSquare;
    }

    public static int getHalfMoveClock(String fen) {
        return nonNegativeInteger(fields(fen)[4], "halfmove clock", 0);
    }

    public static int getFullMoveNumber(String fen) {
        return nonNegativeInteger(fields(fen)[5], "fullmove number", 1);
    }

    private static final String PIECE_STRING = " KQRBNP  kqrbnp";
    private static final String CASTLING_STRING = "KQkq";
    private static final String FILE_STRING = "abcdefgh";

    private Fen() {}

    private static String[] fields(String fen) {
        final String value = Objects.requireNonNull(fen, "fen").trim();
        final String[] fields = value.isEmpty() ? new String[0] : value.split("\\s+");
        if(fields.length != 6) throw invalid("FEN must contain exactly six fields");
        return fields;
    }

    private static int nonNegativeInteger(String value, String name, int minimum) {
        final int parsed;
        try {
            parsed = Integer.parseInt(value);
        } catch(NumberFormatException exception) {
            throw invalid(name + " must be an integer");
        }
        if(parsed < minimum) throw invalid(name + " must be at least " + minimum);
        return parsed;
    }

    private static IllegalArgumentException invalid(String detail) {
        return new IllegalArgumentException("Invalid FEN: " + detail);
    }

}
