package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import com.ohinteractive.seedv6.core.Board;

/** Coordinate-notation scoresheet. It follows new moves only while already at the bottom. */
final class MoveScoresheet extends JPanel {
    private List<String> moves = List.of();
    private int firstSide, firstNumber = 1;
    private long revision;
    private final AbstractTableModel model = new AbstractTableModel() {
        public int getRowCount() { return moves.isEmpty() ? 0 : (moves.size() + firstSide + 1) / 2; }
        public int getColumnCount() { return 3; }
        public String getColumnName(int column) { return new String[] {"#", "White", "Black"}[column]; }
        public Object getValueAt(int row, int column) {
            if (column == 0) return firstNumber + row;
            int ply = row * 2 + column - 1 - firstSide;
            return ply < 0 || ply >= moves.size() ? "" : moves.get(ply);
        }
    };
    private final JTable table = new JTable(model);
    private final JScrollPane scroll = new JScrollPane(table);
    private final JLabel count = SeedTheme.label("Coordinate notation", 11, SeedTheme.MUTED);

    MoveScoresheet() {
        super(new BorderLayout()); setOpaque(false);
        table.setName("moveHistory"); table.setFillsViewportHeight(true);
        table.setRowHeight(SeedTheme.scale(27)); table.setFont(SeedTheme.font(13, Font.PLAIN));
        table.setRowSelectionAllowed(false); table.setCellSelectionEnabled(false);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setMaxWidth(SeedTheme.scale(65));
        table.getColumnModel().getColumn(0).setPreferredWidth(SeedTheme.scale(50));
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int row, int column) {
                super.getTableCellRendererComponent(t, value, false, false, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, SeedTheme.scale(14), 0, SeedTheme.scale(8)));
                boolean latest = !moves.isEmpty() && row == model.getRowCount() - 1;
                setBackground(latest ? SeedTheme.SELECTED : row % 2 == 0 ? SeedTheme.INSET : SeedTheme.PANEL);
                setForeground(latest && column > 0 && row * 2 + column - 1 - firstSide == moves.size() - 1
                        ? SeedTheme.GREEN : column == 0 ? SeedTheme.SECONDARY : SeedTheme.TEXT);
                return this;
            }
        });
        table.getAccessibleContext().setAccessibleDescription("Played moves in coordinate notation; current move highlighted. Read-only scoresheet.");
        scroll.setPreferredSize(new Dimension(0, SeedTheme.scale(240)));
        scroll.setMinimumSize(new Dimension(0, SeedTheme.scale(80)));
        JPanel body = SeedTheme.panel(new BorderLayout());
        body.add(scroll, BorderLayout.CENTER); SeedTheme.padding(count, 6, 14, 6, 14); body.add(count, BorderLayout.SOUTH);
        add(SeedTheme.card("Moves", null, body));
    }

    void showPosition(GameController.PositionView position) {
        List<String> next = position.moves();
        if (next.isEmpty()) {
            long[] board = position.board();
            firstSide = position.status().sideToMove(); firstNumber = Board.fullMoveNumber((int) board[Board.STATUS]);
        }
        if (moves.equals(next)) return; // Square selection must not disturb the reader's scroll.
        JScrollBar bar = scroll.getVerticalScrollBar();
        boolean follow = bar.getValue() + bar.getVisibleAmount() >= bar.getMaximum() - 2;
        int oldValue = bar.getValue();
        moves = next; model.fireTableDataChanged();
        count.setText("Coordinate notation  ·  " + moves.size() + " plies");
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
}
