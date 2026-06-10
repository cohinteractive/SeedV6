package com.ohinteractive.seedv6.core.util;

public class Fen {
    
    public static int[] getPieces(String fen) {
        String fenPieces = fen.indexOf(" ") != -1 ? fen.substring(0, fen.indexOf(" ")) : fen;
        if(fenPieces.length() - fenPieces.replace("/", "").length() != 7) return new int[0];
        int[] squares = new int[64];
        int square = 56;
        for(char c : fenPieces.toCharArray()) {
            if(c == '/') { square = square - 16; continue; }
            if(Character.isDigit(c)) { square += (c - '0'); continue; }
            squares[square ++] = PIECE_STRING.indexOf(c);
        }
        return squares;
    }

    public static boolean getWhiteToMove(String fen) {
        return fen.charAt(getSpaceIndex(fen, 1) + 1) == 'w';
    }

    public static int getCastling(String fen) {
        String fenCastlingString = fen.substring(getSpaceIndex(fen, 2) + 1, getSpaceIndex(fen, 3));
        int castling = 0;
        for(char c : fenCastlingString.toCharArray()) {
            if(c == '-') return 0;
            int castlingCharIndex = CASTLING_STRING.indexOf(c);
            if(castlingCharIndex == -1) return -1;
            castling |= 1 << castlingCharIndex;
        }
        return castling;
    }

    public static int getEnPassantSquare(String fen) {
        int eIndex = getSpaceIndex(fen, 3) + 1;
        if(eIndex == 0) return -1;
        if(fen.charAt(eIndex) == '-') return -1;
        int file = FILE_STRING.indexOf(fen.charAt(eIndex));
        if(file == -1) return -1;
        int rank = Character.valueOf(fen.charAt(eIndex + 1)) - 49;
        if(rank == -1) return -1;
        int eSquare = rank << 3 | file;
        int playerToMove = getWhiteToMove(fen) ? 0 : 1;
        return (playerToMove == 0 && eSquare > 39 && eSquare < 48) || (playerToMove == 1 && eSquare > 15 && eSquare < 24) ? eSquare : -1;
    }

    public static int getHalfMoveClock(String fen) {
        return getSpaceIndex(fen, 4) + 1 == -1 ? -1 : Integer.parseInt(fen.substring(getSpaceIndex(fen, 4) + 1, getSpaceIndex(fen, 5)));
    }

    public static int getFullMoveNumber(String fen) {
        return Integer.parseInt(fen.substring(getSpaceIndex(fen, 5) + 1));
    }

    private static final String PIECE_STRING = " KQRBNP  kqrbnp";
    private static final String CASTLING_STRING = "KQkq";
    private static final String FILE_STRING = "abcdefgh";

    private Fen() {}

    private static int getSpaceIndex(String string, int spaceIndex) {
        int index = 0;
        for(int i = 0; i < spaceIndex; i ++) {
            index = string.indexOf(32, index + 1);
            if(index == -1) return -1;
        }
        return index;
    }

}
