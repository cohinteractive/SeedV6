package com.ohinteractive.seedv6.gui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.util.UIScale;

/** Small desktop palette and composition helpers, shared by both workspaces. */
final class SeedTheme {
    static final Color BACKGROUND = new Color(0x11181d);
    static final Color PANEL = new Color(0x1c242a);
    static final Color INSET = new Color(0x151d23);
    static final Color LINE = new Color(0x303b42);
    static final Color TEXT = new Color(0xeeefeb);
    static final Color SECONDARY = new Color(0xaeb8bf);
    static final Color MUTED = new Color(0x77828a);
    static final Color GREEN = new Color(0x42bd7b);
    static final Color SELECTED = new Color(0x244b3e);
    static final Color ERROR = new Color(0xe47772);
    static final Color WARNING = new Color(0xd8b978);
    static final int GAP = 12, PAD = 16;
    private static boolean installed;

    // Evaluated before JFrame constructs its children (also for direct test construction).
    static String initialize() {
        if (!installed) {
            FlatLaf.registerCustomDefaultsSource("com.ohinteractive.seedv6.gui");
            if (!FlatDarkLaf.setup()) throw new IllegalStateException("Cannot initialize SeedV6 desktop theme.");
            UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
            JFrame.setDefaultLookAndFeelDecorated(true);
            JDialog.setDefaultLookAndFeelDecorated(true);
            installed = true;
        }
        return "SeedV6";
    }

    static int scale(int value) { return UIScale.scale(value); }
    static Font font(int size, int style) { return UIManager.getFont("Label.font").deriveFont(style, (float) scale(size)); }
    static JPanel panel(LayoutManager layout) {
        JPanel result = new JPanel(layout); result.setOpaque(false); return result;
    }
    static JLabel label(String text, int size, Color color) {
        JLabel result = new JLabel(text); result.setFont(font(size, Font.PLAIN)); result.setForeground(color); return result;
    }
    static void padding(JComponent component, int top, int left, int bottom, int right) {
        component.setBorder(BorderFactory.createEmptyBorder(scale(top), scale(left), scale(bottom), scale(right)));
    }
    static JPanel card(String title, JComponent action, JComponent content) {
        JPanel card = new Card();
        card.setLayout(new BorderLayout());
        JPanel header = panel(new BorderLayout(scale(12), 0));
        JLabel heading = label(title, 16, TEXT); heading.setFont(font(16, Font.BOLD));
        header.add(heading, BorderLayout.WEST);
        if (action != null) header.add(action, BorderLayout.EAST);
        header.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, LINE),
                BorderFactory.createEmptyBorder(scale(10), scale(PAD), scale(10), scale(PAD))));
        card.add(header, BorderLayout.NORTH); card.add(content, BorderLayout.CENTER);
        return card;
    }
    static final class Card extends JPanel {
        Card() {
            setOpaque(false);
            setBorder(new AbstractBorder() {
                @Override public Insets getBorderInsets(Component c) { return new Insets(1, 1, 1, 1); }
                @Override public Insets getBorderInsets(Component c, Insets i) { i.set(1, 1, 1, 1); return i; }
            });
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(PANEL); g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, scale(10), scale(10));
            g.setColor(LINE); g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, scale(10), scale(10));
            g.dispose();
        }
    }
    private SeedTheme() {}
}
