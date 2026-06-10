package com.ohinteractive.seedv6.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BoardTest {

    @Test
    void playerReturns0() {
        long[] board = new long[Board.MAX_BITBOARDS];
        board[Board.STATUS] = 0b10000010010;
        int player = Board.player((int) board[Board.STATUS]);
        assertEquals(0, player);
    }

    @Test
    void playerReturns1() {
        long[] board = new long[Board.MAX_BITBOARDS];
        board[Board.STATUS] = 0b10000010011;
        int player = Board.player((int) board[Board.STATUS]);
        assertEquals(1, player);
    }

    @Test
    void whiteKingSideReturnsTrue() {
        long[] board = new long[Board.MAX_BITBOARDS];
        board[Board.STATUS] = 0b10000010011;
        boolean isKingSide = Board.kingSide((int) board[Board.STATUS], 0);
        assertEquals(true, isKingSide);
    }

    @Test
    void whiteQueenSideReturnsTrue() {
        long[] board = new long[Board.MAX_BITBOARDS];
        board[Board.STATUS] = 0b10000010111;
        boolean isQueenSide = Board.queenSide((int) board[Board.STATUS], 0);
        assertEquals(true, isQueenSide);
    }

    @Test
    void blackKingSideReturnsTrue() {
        long[] board = new long[Board.MAX_BITBOARDS];
        board[Board.STATUS] = 0b10000011011;
        boolean isKingSide = Board.kingSide((int) board[Board.STATUS], 1);
        assertEquals(true, isKingSide);
    }

    @Test
    void blackQueenSideReturnsTrue() {
        long[] board = new long[Board.MAX_BITBOARDS];
        board[Board.STATUS] = 0b10000010111;
        boolean isQueenSide = Board.queenSide((int) board[Board.STATUS], 1);
        assertEquals(true, isQueenSide);
    }

    @Test
    void validWhiteEnPassantSquare() {
        long[] board = new long[Board.MAX_BITBOARDS];
        board[Board.STATUS] = 0b10100010110;
        int eSquare = Board.enPassantSquare((int) board[Board.STATUS]);
        assertEquals(40, eSquare);
    }

    @Test
    void invalidWhiteEnPassantSquare() {
        long[] board = new long[Board.MAX_BITBOARDS];
        board[Board.STATUS] = 0b10100010111;
        int eSquare = Board.enPassantSquare((int) board[Board.STATUS]);
        assertEquals(Integer.MIN_VALUE, eSquare);
    }

    
}