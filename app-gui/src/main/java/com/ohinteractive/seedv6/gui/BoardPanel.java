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
import com.ohinteractive.seedv6.core.move.CapturedPieces;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.training.telemetry.ActiveGameSnapshot;

/** Resizable board rendering and mouse intent capture; contains no chess rules. */
final class BoardPanel extends JPanel {

    interface InputListener {
        void squarePressed(int square);
        void squareReleased(int square);
        default void cancelInteraction() {}
    }

    BoardPanel() {
        setBackground(SeedTheme.PANEL);
        setName("chessBoard");
        getAccessibleContext().setAccessibleName("Chess board, White at bottom");
        setFocusable(true);
        final MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                if (!javax.swing.SwingUtilities.isLeftMouseButton(event) || !humanInput) return;
                cancelDrag();
                int square = squareAt(event).orElse(-1);
                if (square < 0) return;
                gestureRevision = revision;
                gesture = true;
                listener.squarePressed(square);
                if (selectedSquare == square && legalTargets.length > 0) {
                    dragSource = square;
                    dragPiece = Board.getSquare(board[0], board[1], board[2], board[3], square);
                    dragSize = boardBounds().width / 8;
                    pressX = dragX = event.getX(); pressY = dragY = event.getY();
                }
            }

            @Override public void mouseDragged(MouseEvent event) {
                if (!gesture || dragSource < 0 || !humanInput || gestureRevision != revision) return;
                int oldX = dragX, oldY = dragY;
                boolean wasDragging = dragging;
                dragX = event.getX(); dragY = event.getY();
                if (Math.abs(dragX - pressX) + Math.abs(dragY - pressY) >= SeedTheme.scale(5)) dragging = true;
                if (dragging) {
                    if (!wasDragging) repaint(squareBounds(dragSource));
                    repaint(oldX - dragSize / 2 - 1, oldY - dragSize / 2 - 1, dragSize + 2, dragSize + 2);
                    repaint(dragX - dragSize / 2 - 1, dragY - dragSize / 2 - 1, dragSize + 2, dragSize + 2);
                }
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                if (!javax.swing.SwingUtilities.isLeftMouseButton(event) || !gesture) return;
                boolean valid = humanInput && gestureRevision == revision;
                boolean wasDragging = dragging;
                int square = squareAt(event).orElse(-1);
                boolean legal = false;
                for (int target : legalTargets) if (target == square) legal = true;
                cancelDrag();
                if (!valid) return;
                if (square < 0 || wasDragging && !legal) listener.cancelInteraction();
                else listener.squareReleased(square);
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    void setInputListener(InputListener listener) {
        this.listener = listener == null ? NO_INPUT : listener;
    }

    void showPosition(GameController.PositionView position) {
        if (revision != position.revision() || !position.humanInput()) cancelDrag();
        revision = position.revision(); humanInput = position.humanInput();
        unavailable = null;
        board = position.board();
        captured = position.captured();
        whiteMaterial = BoardMaterial.balance(board, Value.WHITE);
        candidateSide = -1;
        trainingPosition = false;
        selectedSquare = position.selectedSquare();
        legalTargets = position.legalTargets();
        lastFrom = position.lastFrom();
        lastTo = position.lastTo();
        checkedKing = position.checkedKingSquare();
        repaint();
    }

    /** Read-only training renderer. The only board copy on the EDT happens for a new publication. */
    void showTrainingPosition(com.ohinteractive.seedv6.training.telemetry.ActiveGameSnapshot position, PlayScore evaluation, String description) {
        cancelDrag(); humanInput = false;
        unavailable = null;
        board = position.board();
        captured = position.captured();
        whiteMaterial = BoardMaterial.balance(board, Value.WHITE);
        candidateSide = candidateSide(position);
        selectedSquare = checkedKing = -1;
        legalTargets = new int[0];
        lastFrom = position.lastFrom(); lastTo = position.lastTo();
        score = evaluation;
        trainingPosition = true;
        setToolTipText(description);
        getAccessibleContext().setAccessibleDescription(description);
        repaint();
    }

    long[] displayedBoard() { return board == null ? null : board.clone(); }
    boolean positionUnavailable() { return unavailable != null; }
    CapturedPieces displayedCaptures() { return captured; }
    int displayedCandidateSide() { return candidateSide; }

    static int candidateSide(ActiveGameSnapshot position) {
        if (position.phase() != ActiveGameSnapshot.Phase.VALIDATION) return -1;
        return position.white().role() == ActiveGameSnapshot.Role.CANDIDATE ? Value.WHITE
                : position.black().role() == ActiveGameSnapshot.Role.CANDIDATE ? Value.BLACK : -1;
    }

    /** Explicit empty presentation; never carries another workspace's game or a made-up position. */
    void showUnavailablePosition(String message) {
        cancelDrag(); humanInput = false;
        board = new long[Board.MAX_BITBOARDS];
        captured = CapturedPieces.empty(false);
        whiteMaterial = 0;
        candidateSide = -1;
        selectedSquare = lastFrom = lastTo = checkedKing = -1;
        legalTargets = new int[0];
        unavailable = message;
        score = new PlayScore("—", 0.5, false);
        setToolTipText(message);
        getAccessibleContext().setAccessibleDescription(message);
        repaint();
    }

    void showScore(PlayScore score, String units) {
        this.score = score;
        setToolTipText("Searched evaluation: " + units + ", White perspective. Bar is not a win probability.");
        Rectangle bounds = boardBounds();
        repaint(bounds.x + bounds.width, 0, getWidth() - bounds.x - bounds.width, getHeight());
    }

    Rectangle boardBounds() {
        Rectangle bounds = mapper.boardBounds(boardWidth(), boardHeight());
        bounds.translate(SeedTheme.scale(24), topInset());
        return bounds;
    }

    private int boardWidth() { return Math.max(0, getWidth() - SeedTheme.scale(70)); }
    private int boardHeight() { return Math.max(0, getHeight() - (playPresentation
            ? blackNameHeight + whiteNameHeight + SeedTheme.scale(106) : SeedTheme.scale(94))); }
    private int topInset() { return playPresentation ? blackNameHeight + SeedTheme.scale(40) : SeedTheme.scale(44); }
    private Rectangle squareBounds(int square) {
        Rectangle bounds = mapper.squareBounds(square, boardWidth(), boardHeight());
        bounds.translate(SeedTheme.scale(24), topInset());
        return bounds;
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
                final Rectangle bounds = squareBounds(square);
                if (!g.hitClip(bounds.x, bounds.y, bounds.width, bounds.height)) continue;
                final int file = square & 7;
                final int rank = square >>> 3;
                g.setColor(((file + rank) & 1) != 0 ? LIGHT_SQUARE : DARK_SQUARE);
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
                final Rectangle selected = squareBounds(selectedSquare);
                g.setStroke(new BasicStroke(Math.max(2F, selected.width / 18F)));
                g.setColor(SELECTED);
                g.drawRect(selected.x + 2, selected.y + 2,
                    Math.max(0, selected.width - 4), Math.max(0, selected.height - 4));
            }
            for(int target : legalTargets) {
                final Rectangle bounds = squareBounds(target);
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
                if(piece == 0 || dragging && square == dragSource) continue;
                final Rectangle bounds = squareBounds(square);
                if (!g.hitClip(bounds.x, bounds.y, bounds.width, bounds.height)) continue;
                pieceRenderer.paint(g, piece, bounds.x, bounds.y, bounds.width, bounds.height, (piece >>> 3) == candidateSide);
            }
            paintCoordinatesAndEvaluation(g);
            if (dragging) {
                pieceRenderer.paint(g, dragPiece, dragX - dragSize / 2, dragY - dragSize / 2, dragSize, dragSize);
            }
            if (unavailable == null) {
                Rectangle bounds = boardBounds();
                if (playPresentation) {
                    paintCaptured(g, Value.BLACK, bounds.x, bounds.y - SeedTheme.scale(29), bounds.width);
                    paintCaptured(g, Value.WHITE, bounds.x, bounds.y + bounds.height + SeedTheme.scale(25) + whiteNameHeight, bounds.width);
                } else {
                    paintMaterial(g, Value.BLACK, bounds.x, bounds.y - SeedTheme.scale(32), bounds.width);
                    paintMaterial(g, Value.WHITE, bounds.x, bounds.y + bounds.height + SeedTheme.scale(23), bounds.width);
                }
            }
            if (unavailable != null) {
                Rectangle b = boardBounds();
                g.setColor(new Color(17, 24, 29, 205)); g.fillRect(b.x, b.y, b.width, b.height);
                g.setFont(SeedTheme.font(16, java.awt.Font.BOLD)); g.setColor(SeedTheme.TEXT);
                g.drawString("No live training position", b.x + (b.width - g.getFontMetrics().stringWidth("No live training position")) / 2, b.y + b.height / 2);
                g.setFont(SeedTheme.font(11, java.awt.Font.PLAIN)); g.setColor(SeedTheme.SECONDARY);
                String hint = "Waiting for an active training game.";
                g.drawString(hint, b.x + (b.width - g.getFontMetrics().stringWidth(hint)) / 2, b.y + b.height / 2 + SeedTheme.scale(25));
            }
        } finally {
            g.dispose();
        }
    }

    private static final InputListener NO_INPUT = new InputListener() {
        @Override public void squarePressed(int square) {}
        @Override public void squareReleased(int square) {}
    };
    private static final Color LIGHT_SQUARE = new Color(0xe4cfaa);
    private static final Color DARK_SQUARE = new Color(0xa88a67);
    private static final Color LAST_MOVE = new Color(181, 202, 77, 145);
    private static final Color SELECTED = SeedTheme.GREEN;
    private static final Color LEGAL_TARGET = new Color(20, 83, 51, 160);
    private static final Color CHECKED_KING = new Color(215, 50, 50, 150);

    private final SquareMapper mapper = new SquareMapper();
    // The existing rounded set matches the accepted workstation reference.
    private static final PieceRenderer pieceRenderer = new PieceRenderer(PieceRenderer.PieceSet.LEGACY);
    private InputListener listener = NO_INPUT;
    private long revision, gestureRevision;
    private boolean humanInput, gesture, dragging;
    private int dragSource = -1, dragPiece, dragSize, pressX, pressY, dragX, dragY;

    private void cancelDrag() { gesture = dragging = false; dragSource = -1; repaint(); }
    private long[] board;
    private CapturedPieces captured = CapturedPieces.empty(false);
    private int whiteMaterial;
    private int candidateSide = -1;
    private String unavailable;
    private boolean trainingPosition;
    private int selectedSquare = -1;
    private int[] legalTargets = new int[0];
    private int lastFrom = -1;
    private int lastTo = -1;
    private int checkedKing = -1;
    private PlayScore score = new PlayScore("—", 0.5, false);

    private void paintCoordinatesAndEvaluation(Graphics2D g) {
        Rectangle b = boardBounds();
        g.setFont(SeedTheme.font(12, java.awt.Font.PLAIN)); g.setColor(SeedTheme.SECONDARY);
        for (int i = 0; i < 8; i++) {
            Rectangle rank = squareBounds(i * 8), file = squareBounds(i);
            g.drawString(Integer.toString(i + 1), b.x - SeedTheme.scale(17), rank.y + rank.height / 2 + SeedTheme.scale(4));
            String letter = Character.toString('a' + i);
            g.drawString(letter, file.x + (file.width - g.getFontMetrics().stringWidth(letter)) / 2,
                    b.y + b.height + SeedTheme.scale(17));
        }
        if (unavailable != null) return;
        int x = b.x + b.width + SeedTheme.scale(12), width = SeedTheme.scale(15);
        int whiteHeight = (int) Math.round(b.height * score.whiteFraction());
        g.setColor(SeedTheme.INSET); g.fillRect(x, b.y, width, b.height);
        if (!trainingPosition || score.available()) {
            g.setColor(new Color(0xd7dddf)); g.fillRect(x, b.y + b.height - whiteHeight, width, whiteHeight);
            g.setColor(SeedTheme.GREEN); g.fillRect(x, b.y + b.height - whiteHeight - 1, width, SeedTheme.scale(3));
            g.setColor(SeedTheme.MUTED); g.drawLine(x - 2, b.y + b.height / 2, x + width + 2, b.y + b.height / 2);
        }
        g.setColor(SeedTheme.TEXT); g.setFont(SeedTheme.font(10, java.awt.Font.PLAIN));
        g.drawString(score.text(), x + (width - g.getFontMetrics().stringWidth(score.text())) / 2, b.y - SeedTheme.scale(6));
    }

    private OptionalInt squareAt(MouseEvent event) {
        return mapper.squareAt(event.getX() - SeedTheme.scale(24), event.getY() - topInset(), boardWidth(), boardHeight());
    }

    private boolean playPresentation;
    private final javax.swing.JTextArea blackName = playerName(), whiteName = playerName();
    private int blackNameHeight = SeedTheme.scale(40), whiteNameHeight = SeedTheme.scale(40);

    void showPlayers(String white, String black, String whiteIdentity, String blackIdentity) {
        if (!playPresentation) {
            playPresentation = true; setLayout(null); add(blackName); add(whiteName);
            blackName.setName("blackPlayerNetwork"); whiteName.setName("whitePlayerNetwork");
        }
        boolean changed = !whiteName.getText().equals(white) || !blackName.getText().equals(black);
        if (!whiteName.getText().equals(white)) whiteName.setText(white);
        if (!blackName.getText().equals(black)) blackName.setText(black);
        whiteName.setToolTipText(whiteIdentity); blackName.setToolTipText(blackIdentity);
        if (changed) { revalidate(); repaint(); }
    }

    private static javax.swing.JTextArea playerName() {
        var text = new javax.swing.JTextArea(); text.setEditable(false); text.setFocusable(false);
        text.setOpaque(false); text.setLineWrap(true); text.setWrapStyleWord(true);
        text.setBorder(javax.swing.BorderFactory.createEmptyBorder()); text.setMargin(new java.awt.Insets(0, 0, 0, 0));
        text.setFont(SeedTheme.font(14, java.awt.Font.BOLD)); text.setForeground(SeedTheme.TEXT);
        return text;
    }

    @Override public void doLayout() {
        if (!playPresentation) return;
        // Measure only during layout, never during mouse motion or search telemetry.
        for (int pass = 0; pass < 3; pass++) {
            Rectangle b = boardBounds();
            blackName.setSize(Math.max(1, b.width), Short.MAX_VALUE);
            whiteName.setSize(Math.max(1, b.width), Short.MAX_VALUE);
            blackNameHeight = blackName.getPreferredSize().height;
            whiteNameHeight = whiteName.getPreferredSize().height;
        }
        Rectangle b = boardBounds();
        blackName.setBounds(b.x, b.y - SeedTheme.scale(33) - blackNameHeight, b.width, blackNameHeight);
        whiteName.setBounds(b.x, b.y + b.height + SeedTheme.scale(23), b.width, whiteNameHeight);
    }

    private void paintCaptured(Graphics2D g, int side, int x, int y, int width) {
        int advantage = side == Value.WHITE ? whiteMaterial : -whiteMaterial;
        int size = SeedTheme.scale(23), gap = SeedTheme.scale(7), step = SeedTheme.scale(9);
        int groups = 0, extras = 0;
        for (int type : BoardMaterial.ORDER) {
            int count = captured.count(type | ((side ^ 1) << Board.PLAYER_SHIFT));
            if (count > 0) { groups++; extras += count - 1; }
        }
        g.setFont(SeedTheme.font(17, java.awt.Font.BOLD));
        String balance = advantage > 0 ? "+" + advantage : "";
        int scoreWidth = balance.isEmpty() ? 0 : g.getFontMetrics().stringWidth(balance) + gap;
        if (extras > 0) step = Math.max(1, Math.min(step, (width - groups * (size + gap) - scoreWidth) / extras));
        int left = x;
        for (int type : BoardMaterial.ORDER) {
            int piece = type | ((side ^ 1) << Board.PLAYER_SHIFT), count = captured.count(piece);
            if (count == 0) continue;
            // A quiet light backing keeps the original black artwork readable on the dark panel.
            g.setColor(SeedTheme.SECONDARY);
            g.fillRoundRect(left, y, size + (count - 1) * step, size, SeedTheme.scale(4), SeedTheme.scale(4));
            for (int i = 0; i < count; i++) pieceRenderer.paint(g, piece, left + i * step, y, size, size);
            left += size + (count - 1) * step + gap;
        }
        g.setColor(SeedTheme.TEXT);
        g.drawString(balance, left, y + SeedTheme.scale(18));
    }

    private void paintMaterial(Graphics2D g, int side, int x, int y, int width) {
        if (width <= 0) return;
        g.setFont(SeedTheme.font(11, java.awt.Font.PLAIN));
        g.setColor(side == candidateSide ? PieceRenderer.CANDIDATE_ACCENT : SeedTheme.SECONDARY);
        String name = side == Value.WHITE ? "White" : "Black";
        String scope = captured.fromStartingPosition() ? "" : "*";
        String label = name + " captures" + scope;
        String balance = BoardMaterial.signed(side == Value.WHITE ? whiteMaterial : -whiteMaterial);
        int right = x + width - g.getFontMetrics().stringWidth(balance);
        int size = SeedTheme.scale(20);
        int groups = 0, annotations = 0;
        for (int type : BoardMaterial.ORDER) {
            int count = captured.count(type | ((side ^ 1) << Board.PLAYER_SHIFT));
            if (count > 0) {
                groups++;
                annotations += g.getFontMetrics().stringWidth(count > 1 ? "×" + count : "") + SeedTheme.scale(4);
            }
        }
        if (g.getFontMetrics().stringWidth(label) + groups * size + annotations + SeedTheme.scale(14) > right - x) {
            label = name + scope;
            if (groups > 0) size = Math.max(SeedTheme.scale(12), Math.min(size,
                    (right - x - g.getFontMetrics().stringWidth(label) - annotations - SeedTheme.scale(14)) / groups));
        }
        g.drawString(label, x, y + SeedTheme.scale(18));
        int left = x + g.getFontMetrics().stringWidth(label) + SeedTheme.scale(6);
        for (int type : BoardMaterial.ORDER) {
            int piece = type | ((side ^ 1) << Board.PLAYER_SHIFT);
            int count = captured.count(piece);
            if (count == 0) continue;
            String quantity = count > 1 ? "×" + count : "";
            int group = size + g.getFontMetrics().stringWidth(quantity) + SeedTheme.scale(4);
            pieceRenderer.paint(g, piece, left, y, size, size);
            g.drawString(quantity, left + size, y + SeedTheme.scale(18));
            left += group;
        }
        g.drawString(balance, right, y + SeedTheme.scale(18));
    }

    @Override public String getToolTipText(MouseEvent event) {
        String scope = captured.fromStartingPosition() ? "Captures from committed game moves."
                : "* Captures since the loaded/opening position; earlier captures are unknown.";
        return (super.getToolTipText(event) == null ? "" : super.getToolTipText(event) + " ")
                + scope + " Material: P=1, N/B=3, R=5, Q=9; balance from each side's perspective.";
    }
}
