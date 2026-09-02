package com.ohinteractive.seedv6.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;

/** Cached classpath-image renderer with a dependency-free Java2D fallback. */
final class PieceRenderer {

    PieceRenderer() {
        this(DEFAULT_PIECE_SET, PieceRenderer::loadClasspathImage);
    }

    PieceRenderer(ImageLoader loader) {
        this(DEFAULT_PIECE_SET, loader);
    }

    PieceRenderer(PieceSet pieceSet) {
        this(pieceSet, PieceRenderer::loadClasspathImage);
    }

    PieceRenderer(PieceSet pieceSet, ImageLoader loader) {
        for(int piece = 0; piece < RESOURCE_FILENAMES.length; piece ++) {
            final String path = resourcePath(pieceSet, piece);
            if(path != null) images[piece] = loader.load(path);
        }
    }

    void paint(Graphics2D graphics, int piece, int x, int y, int width, int height) {
        final int type = piece & Piece.TYPE;
        if(type < Piece.KING || type > Piece.PAWN || width <= 0 || height <= 0) return;

        final BufferedImage image = imageForPiece(piece);
        if(image != null) {
            paintImage(graphics, image, x, y, width, height);
        } else {
            paintFallback(graphics, piece, x, y, width, height);
        }
    }

    BufferedImage imageForPiece(int piece) {
        return piece >= 0 && piece < images.length ? images[piece] : null;
    }

    static String resourcePath(int piece) {
        return resourcePath(DEFAULT_PIECE_SET, piece);
    }

    static String resourcePath(PieceSet pieceSet, int piece) {
        if(piece < 0 || piece >= RESOURCE_FILENAMES.length) return null;
        final String filename = RESOURCE_FILENAMES[piece];
        return filename == null ? null : RESOURCE_ROOT + pieceSet.directory + "/" + filename;
    }

    @FunctionalInterface
    interface ImageLoader {
        BufferedImage load(String resourcePath);
    }

    enum PieceSet {
        ORIGINAL("original"),
        LEGACY("legacy");

        private PieceSet(String directory) {
            this.directory = directory;
        }

        private final String directory;
    }

    private static final PieceSet DEFAULT_PIECE_SET = PieceSet.ORIGINAL;
    private static final String RESOURCE_ROOT = "/com/ohinteractive/seedv6/gui/pieces/";
    private static final String[] RESOURCE_FILENAMES = {
        null,
        "wk.png",
        "wq.png",
        "wr.png",
        "wb.png",
        "wn.png",
        "wp.png",
        null,
        null,
        "bk.png",
        "bq.png",
        "br.png",
        "bb.png",
        "bn.png",
        "bp.png"
    };

    private final BufferedImage[] images = new BufferedImage[RESOURCE_FILENAMES.length];

    private static BufferedImage loadClasspathImage(String resourcePath) {
        try(InputStream input = PieceRenderer.class.getResourceAsStream(resourcePath)) {
            return input == null ? null : ImageIO.read(input);
        } catch(IOException | RuntimeException ignored) {
            return null;
        }
    }

    private static void paintImage(
        Graphics2D graphics, BufferedImage image,
        int x, int y, int width, int height
    ) {
        final double scale = Math.min(
            (double) width / image.getWidth(),
            (double) height / image.getHeight()
        );
        final int targetWidth = Math.max(1, (int) Math.round(image.getWidth() * scale));
        final int targetHeight = Math.max(1, (int) Math.round(image.getHeight() * scale));
        final int left = x + (width - targetWidth) / 2;
        final int top = y + (height - targetHeight) / 2;
        final Graphics2D copy = (Graphics2D) graphics.create();
        try {
            copy.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );
            copy.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            copy.drawImage(image, left, top, targetWidth, targetHeight, null);
        } finally {
            copy.dispose();
        }
    }

    private static void paintFallback(
        Graphics2D graphics, int piece, int x, int y, int width, int height
    ) {
        final Graphics2D copy = (Graphics2D) graphics.create();
        try {
            paintFallbackInto(copy, piece, x, y, width, height);
        } finally {
            copy.dispose();
        }
    }

    private static void paintFallbackInto(
        Graphics2D graphics, int piece, int x, int y, int width, int height
    ) {
        final int type = piece & Piece.TYPE;

        final int side = piece >>> 3;
        final int diameter = Math.max(1, Math.min(width, height) * 4 / 5);
        final int left = x + (width - diameter) / 2;
        final int top = y + (height - diameter) / 2;
        final Color fill = side == Value.WHITE ? WHITE_FILL : BLACK_FILL;
        final Color ink = side == Value.WHITE ? BLACK_FILL : WHITE_FILL;

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(fill);
        graphics.fill(new Ellipse2D.Double(left, top, diameter, diameter));
        graphics.setStroke(new BasicStroke(Math.max(1F, diameter / 28F)));
        graphics.setColor(ink);
        graphics.draw(new Ellipse2D.Double(left, top, diameter, diameter));

        final String label = LABELS[type];
        final Font font = new Font(Font.SANS_SERIF, Font.BOLD, Math.max(9, diameter * 11 / 20));
        graphics.setFont(font);
        final FontMetrics metrics = graphics.getFontMetrics();
        graphics.drawString(
            label,
            x + (width - metrics.stringWidth(label)) / 2,
            y + (height - metrics.getHeight()) / 2 + metrics.getAscent()
        );

    }

    private static final Color WHITE_FILL = new Color(250, 250, 246);
    private static final Color BLACK_FILL = new Color(38, 43, 48);
    private static final String[] LABELS = {"", "K", "Q", "R", "B", "N", "P"};
}
