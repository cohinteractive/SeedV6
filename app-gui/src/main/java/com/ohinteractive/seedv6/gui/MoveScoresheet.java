package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import com.ohinteractive.seedv6.core.Board;

/** SAN scoresheet with immutable searched scores. It follows new moves only while already at the bottom. */
final class MoveScoresheet extends JPanel {
    private List<PlayedMove> moves = List.of();
    private int firstSide, firstNumber = 1;
    private long revision;
    private final AbstractTableModel model = new AbstractTableModel() {
        public int getRowCount() { return moves.isEmpty() ? 0 : (moves.size() + firstSide + 1) / 2; }
        public int getColumnCount() { return 5; }
        public Object getValueAt(int row, int column) {
            if (column == 0) return (firstNumber + row) + ".";
            int ply = row * 2 + (column - 1) / 2 - firstSide;
            if (ply < 0 || ply >= moves.size()) return "";
            PlayedMove move = moves.get(ply);
            return column % 2 == 1 ? move : move.score() == null ? "" : "(" + move.score().text() + ")";
        }
    };
    private final JTable table = new JTable(model);
    private final JScrollPane scroll = new JScrollPane(table);
    private final JLabel count = SeedTheme.label("SAN · scores from White's perspective", 11, SeedTheme.MUTED);

    MoveScoresheet() {
        super(new BorderLayout()); setOpaque(false);
        table.setName("moveHistory"); table.setFillsViewportHeight(true);
        table.setRowHeight(SeedTheme.scale(27)); table.setFont(SeedTheme.font(13, Font.PLAIN));
        table.setRowSelectionAllowed(false); table.setCellSelectionEnabled(false);
        table.setTableHeader(null); table.setShowGrid(false); table.setIntercellSpacing(new Dimension(0, 0));
        table.setBorder(BorderFactory.createEmptyBorder());
        table.getColumnModel().getColumn(0).setMinWidth(SeedTheme.scale(38));
        table.getColumnModel().getColumn(0).setMaxWidth(SeedTheme.scale(52));
        for (int column : new int[]{2, 4}) {
            table.getColumnModel().getColumn(column).setMinWidth(SeedTheme.scale(78));
            table.getColumnModel().getColumn(column).setMaxWidth(SeedTheme.scale(100));
            table.getColumnModel().getColumn(column).setPreferredWidth(SeedTheme.scale(85));
        }
        for (int column : new int[]{1, 3}) {
            table.getColumnModel().getColumn(column).setMinWidth(SeedTheme.scale(105));
            table.getColumnModel().getColumn(column).setMaxWidth(SeedTheme.scale(105));
        }
        table.getColumnModel().getColumn(4).setMaxWidth(Integer.MAX_VALUE);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.setDefaultRenderer(Object.class, new SanRenderer());
        table.getAccessibleContext().setAccessibleDescription("SAN moves with piece artwork and completed search scores in White perspective. Read-only scoresheet.");
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(new Dimension(0, SeedTheme.scale(240)));
        scroll.setMinimumSize(new Dimension(0, SeedTheme.scale(80)));
        JPanel body = SeedTheme.panel(new BorderLayout());
        body.add(scroll, BorderLayout.CENTER); SeedTheme.padding(count, 6, 14, 6, 14); body.add(count, BorderLayout.SOUTH);
        add(SeedTheme.card("Moves", null, body));
    }

    void showPosition(GameController.PositionView position) {
        List<PlayedMove> next = position.playedMoves();
        if (next.isEmpty()) {
            long[] board = position.board();
            firstSide = position.status().sideToMove(); firstNumber = Board.fullMoveNumber((int) board[Board.STATUS]);
        }
        if (!next.isEmpty()) { firstSide = next.getFirst().side(); firstNumber = next.getFirst().number(); }
        if (moves.equals(next)) return; // Square selection must not disturb the reader's scroll.
        JScrollBar bar = scroll.getVerticalScrollBar();
        boolean follow = bar.getValue() + bar.getVisibleAmount() >= bar.getMaximum() - 2;
        int oldValue = bar.getValue();
        moves = next; model.fireTableDataChanged();
        count.setText("SAN · White perspective · " + moves.size() + " plies");
        scroll.validate(); scroll.doLayout(); scroll.getViewport().doLayout();
        if (follow && model.getRowCount() > 0) table.scrollRectToVisible(table.getCellRect(model.getRowCount() - 1, 0, true));
        else bar.setValue(oldValue);
        int restored = bar.getValue();
        long update = ++revision;
        SwingUtilities.invokeLater(() -> {
            if (update != revision || bar.getValueIsAdjusting() || bar.getValue() != restored) return;
            if (follow && model.getRowCount() > 0) table.scrollRectToVisible(table.getCellRect(model.getRowCount() - 1, 0, true));
            else bar.setValue(oldValue);
        });
    }
    private static final PieceRenderer PIECES = new PieceRenderer(PieceRenderer.PieceSet.LEGACY);
    private final class SanRenderer extends DefaultTableCellRenderer {
        private PlayedMove move;
        @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                                  boolean focus, int row, int column) {
            move = value instanceof PlayedMove m ? m : null;
            super.getTableCellRendererComponent(t, move == null ? value : move.san(), false, false, row, column);
            setHorizontalAlignment(LEFT);
            setBorder(BorderFactory.createEmptyBorder(0, SeedTheme.scale(column == 0 ? 12 : 3), 0, 0));
            setBackground(SeedTheme.PANEL); setForeground(column == 0 ? SeedTheme.SECONDARY : SeedTheme.TEXT);
            int ply = row * 2 + (column - 1) / 2 - firstSide;
            PlayedMove entry = column == 0 || ply < 0 || ply >= moves.size() ? null : moves.get(ply);
            setToolTipText(entry == null ? null : entry.san() + (entry.score() == null
                    ? " · Search score unavailable" : " · " + entry.score().description()));
            return this;
        }
        @Override protected void paintComponent(Graphics graphics) {
            if (move == null) { super.paintComponent(graphics); return; }
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setColor(getBackground()); g.fillRect(0, 0, getWidth(), getHeight());
                g.setFont(getFont()); g.setColor(getForeground());
                int x = getInsets().left, size = SeedTheme.scale(19);
                int baseline = (getHeight() - g.getFontMetrics().getHeight()) / 2 + g.getFontMetrics().getAscent();
                for (int i = 0; i < move.san().length(); i++) {
                    char ch = move.san().charAt(i);
                    int type = switch (ch) {
                        case 'N' -> com.ohinteractive.seedv6.core.util.Piece.KNIGHT;
                        case 'B' -> com.ohinteractive.seedv6.core.util.Piece.BISHOP;
                        case 'R' -> com.ohinteractive.seedv6.core.util.Piece.ROOK;
                        case 'Q' -> com.ohinteractive.seedv6.core.util.Piece.QUEEN;
                        case 'K' -> com.ohinteractive.seedv6.core.util.Piece.KING;
                        default -> 0;
                    };
                    if (type != 0) {
                        PIECES.paint(g, type | (move.side() << Board.PLAYER_SHIFT), x, (getHeight() - size) / 2, size, size);
                        x += size;
                    } else {
                        g.drawString(String.valueOf(ch), x, baseline); x += g.getFontMetrics().charWidth(ch);
                    }
                }
            } finally { g.dispose(); }
        }
    }

}
