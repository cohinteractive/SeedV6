package com.ohinteractive.seedv6.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.OptionalInt;

import javax.swing.JPanel;

import com.ohinteractive.seedv6.core.Board;

/** Resizable board rendering and mouse intent capture; contains no chess rules. */
final class BoardPanel extends JPanel {

    interface InputListener {
        void squarePressed(int square);
        void squareReleased(int square);
    }

    BoardPanel() {
        setBackground(new Color(42, 46, 51));
        setFocusable(true);
        final MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                squareAt(event).ifPresent(square -> listener.squarePressed(square));
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                squareAt(event).ifPresent(square -> listener.squareReleased(square));
            }
        };
        addMouseListener(mouse);
    }

    void setInputListener(InputListener listener) {
        this.listener = listener == null ? NO_INPUT : listener;
    }

    void showPosition(GameController.PositionView position) {
        board = position.board();
        selectedSquare = position.selectedSquare();
        legalTargets = position.legalTargets();
        lastFrom = position.lastFrom();
        lastTo = position.lastTo();
        checkedKing = position.checkedKingSquare();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(640, 640);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if(board == null) return;
        final Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for(int square = 0; square < 64; square ++) {
                final Rectangle bounds = mapper.squareBounds(square, getWidth(), getHeight());
                final int file = square & 7;
                final int rank = square >>> 3;
                g.setColor(((file + rank) & 1) == 0 ? LIGHT_SQUARE : DARK_SQUARE);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
                if(square == lastFrom || square == lastTo) {
                    g.setColor(LAST_MOVE);
                    g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
                }
                if(square == checkedKing) {
                    g.setColor(CHECKED_KING);
                    g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
                }
            }

            if(selectedSquare >= 0) {
                final Rectangle selected = mapper.squareBounds(selectedSquare, getWidth(), getHeight());
                g.setStroke(new BasicStroke(Math.max(2F, selected.width / 18F)));
                g.setColor(SELECTED);
                g.drawRect(selected.x + 2, selected.y + 2,
                    Math.max(0, selected.width - 4), Math.max(0, selected.height - 4));
            }
            for(int target : legalTargets) {
                final Rectangle bounds = mapper.squareBounds(target, getWidth(), getHeight());
                final int diameter = Math.max(6, Math.min(bounds.width, bounds.height) / 4);
                g.setColor(LEGAL_TARGET);
                g.fillOval(
                    bounds.x + (bounds.width - diameter) / 2,
                    bounds.y + (bounds.height - diameter) / 2,
                    diameter,
                    diameter
                );
            }

            for(int square = 0; square < 64; square ++) {
                final int piece = Board.getSquare(
                    board[0], board[1], board[2], board[3], square
                );
                if(piece == 0) continue;
                final Rectangle bounds = mapper.squareBounds(square, getWidth(), getHeight());
                pieceRenderer.paint(g, piece, bounds.x, bounds.y, bounds.width, bounds.height);
            }
        } finally {
            g.dispose();
        }
    }

    private static final InputListener NO_INPUT = new InputListener() {
        @Override public void squarePressed(int square) {}
        @Override public void squareReleased(int square) {}
    };
    private static final Color LIGHT_SQUARE = new Color(238, 216, 180);
    private static final Color DARK_SQUARE = new Color(126, 164, 118);
    private static final Color LAST_MOVE = new Color(246, 221, 80, 125);
    private static final Color SELECTED = new Color(29, 111, 210);
    private static final Color LEGAL_TARGET = new Color(20, 83, 51, 160);
    private static final Color CHECKED_KING = new Color(215, 50, 50, 150);

    private final SquareMapper mapper = new SquareMapper();
    private final PieceRenderer pieceRenderer = new PieceRenderer();
    private InputListener listener = NO_INPUT;
    private long[] board;
    private int selectedSquare = -1;
    private int[] legalTargets = new int[0];
    private int lastFrom = -1;
    private int lastTo = -1;
    private int checkedKing = -1;

    private OptionalInt squareAt(MouseEvent event) {
        return mapper.squareAt(event.getX(), event.getY(), getWidth(), getHeight());
    }
}
