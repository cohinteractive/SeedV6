package com.ohinteractive.seedv6.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.util.Piece;

class PieceRendererTest {

    @Test
    void bothPieceSetsContainTwelveConsistentTransparentPngs() throws IOException {
        for(PieceRenderer.PieceSet pieceSet : PieceRenderer.PieceSet.values()) {
            final PieceRenderer renderer = new PieceRenderer(pieceSet);
            int expectedWidth = -1;
            int expectedHeight = -1;
            for(Map.Entry<Integer, String> entry : expectedMappings().entrySet()) {
                final int piece = entry.getKey();
                final String expectedPath = resourceRoot(pieceSet) + entry.getValue();
                assertEquals(expectedPath, PieceRenderer.resourcePath(pieceSet, piece));
                try(InputStream input = PieceRenderer.class.getResourceAsStream(expectedPath)) {
                    assertNotNull(input, expectedPath);
                    final BufferedImage decoded = ImageIO.read(input);
                    assertNotNull(decoded, expectedPath);
                    assertTrue(decoded.getColorModel().hasAlpha(), expectedPath);
                    assertTrue(hasVisiblePixel(decoded), expectedPath);
                    assertTransparentBorder(decoded, expectedPath);
                    assertEquals(256, decoded.getWidth(), expectedPath);
                    assertEquals(256, decoded.getHeight(), expectedPath);
                    if(expectedWidth < 0) {
                        expectedWidth = decoded.getWidth();
                        expectedHeight = decoded.getHeight();
                    }
                    assertEquals(expectedWidth, decoded.getWidth(), expectedPath);
                    assertEquals(expectedHeight, decoded.getHeight(), expectedPath);
                }
                assertNotNull(renderer.imageForPiece(piece), expectedPath);
            }
        }
    }

    @Test
    void originalArtworkRetainsWhiteAndDarkPieceFills() {
        final PieceRenderer renderer = new PieceRenderer(PieceRenderer.PieceSet.ORIGINAL);
        for(int type = Piece.KING; type <= Piece.PAWN; type ++) {
            assertTrue(countVisiblePixelsWithLuminance(renderer.imageForPiece(type), 220, true) > 100);
            assertTrue(countVisiblePixelsWithLuminance(
                renderer.imageForPiece(type | BLACK), 80, false
            ) > 100);
        }
    }

    @Test
    void originalIsTheDefaultAndMappingsUseTheExpectedColourAndTypeFilenames() {
        for(PieceRenderer.PieceSet pieceSet : PieceRenderer.PieceSet.values()) {
            for(int type = Piece.KING; type <= Piece.PAWN; type ++) {
                final String white = PieceRenderer.resourcePath(pieceSet, type);
                final String black = PieceRenderer.resourcePath(pieceSet, type | BLACK);
                assertTrue(white.endsWith("/w" + suffix(type) + ".png"));
                assertTrue(black.endsWith("/b" + suffix(type) + ".png"));
                assertNotEquals(white, black);
            }
        }
        for(Map.Entry<Integer, String> entry : expectedMappings().entrySet()) {
            assertEquals(
                RESOURCE_ROOT + "original/" + entry.getValue(),
                PieceRenderer.resourcePath(entry.getKey())
            );
        }
    }

    @Test
    void imagesAreDecodedOnceAndReusedAcrossRepaints() {
        final AtomicInteger loads = new AtomicInteger();
        final BufferedImage image = solidImage(16, 16, Color.CYAN);
        final PieceRenderer renderer = new PieceRenderer(path -> {
            loads.incrementAndGet();
            return image;
        });
        assertEquals(12, loads.get());

        final BufferedImage canvas = solidImage(200, 200, Color.MAGENTA);
        final Graphics2D graphics = canvas.createGraphics();
        try {
            for(int repeat = 0; repeat < 3; repeat ++) {
                for(int piece : expectedMappings().keySet()) {
                    renderer.paint(graphics, piece, 0, 0, 64, 64);
                    assertSame(image, renderer.imageForPiece(piece));
                }
            }
        } finally {
            graphics.dispose();
        }
        assertEquals(12, loads.get());
    }

    @Test
    void imageScalingPreservesAspectRatioAndCentersTheArtwork() {
        final PieceRenderer renderer = new PieceRenderer(
            path -> solidImage(20, 10, Color.GREEN)
        );
        final BufferedImage canvas = solidImage(100, 100, Color.MAGENTA);
        final Graphics2D graphics = canvas.createGraphics();
        try {
            renderer.paint(graphics, Piece.QUEEN, 0, 0, 100, 100);
        } finally {
            graphics.dispose();
        }
        assertEquals(Color.MAGENTA.getRGB(), canvas.getRGB(50, 20));
        assertEquals(Color.GREEN.getRGB(), canvas.getRGB(50, 50));
        assertEquals(Color.MAGENTA.getRGB(), canvas.getRGB(50, 80));
    }

    @Test
    void missingOrUndecodableImagesUseTheVisibleFallbackWithoutRepeatedLoads() {
        final AtomicInteger loads = new AtomicInteger();
        final PieceRenderer renderer = new PieceRenderer(path -> {
            loads.incrementAndGet();
            return null;
        });
        assertEquals(12, loads.get());

        for(int piece : expectedMappings().keySet()) {
            final BufferedImage canvas = solidImage(96, 96, Color.MAGENTA);
            final Graphics2D graphics = canvas.createGraphics();
            try {
                renderer.paint(graphics, piece, 0, 0, 96, 96);
                renderer.paint(graphics, piece, 0, 0, 96, 96);
            } finally {
                graphics.dispose();
            }
            assertTrue(hasChangedPixel(canvas, Color.MAGENTA.getRGB()), "piece=" + piece);
        }
        assertEquals(12, loads.get());
    }

    private static final int BLACK = 8;
    private static final String RESOURCE_ROOT = "/com/ohinteractive/seedv6/gui/pieces/";

    private static String resourceRoot(PieceRenderer.PieceSet pieceSet) {
        return RESOURCE_ROOT + switch(pieceSet) {
            case ORIGINAL -> "original/";
            case LEGACY -> "legacy/";
        };
    }

    private static Map<Integer, String> expectedMappings() {
        final Map<Integer, String> mappings = new LinkedHashMap<>();
        mappings.put(Piece.KING, "wk.png");
        mappings.put(Piece.QUEEN, "wq.png");
        mappings.put(Piece.ROOK, "wr.png");
        mappings.put(Piece.BISHOP, "wb.png");
        mappings.put(Piece.KNIGHT, "wn.png");
        mappings.put(Piece.PAWN, "wp.png");
        mappings.put(Piece.KING | BLACK, "bk.png");
        mappings.put(Piece.QUEEN | BLACK, "bq.png");
        mappings.put(Piece.ROOK | BLACK, "br.png");
        mappings.put(Piece.BISHOP | BLACK, "bb.png");
        mappings.put(Piece.KNIGHT | BLACK, "bn.png");
        mappings.put(Piece.PAWN | BLACK, "bp.png");
        return mappings;
    }

    private static String suffix(int type) {
        return switch(type) {
            case Piece.KING -> "k";
            case Piece.QUEEN -> "q";
            case Piece.ROOK -> "r";
            case Piece.BISHOP -> "b";
            case Piece.KNIGHT -> "n";
            case Piece.PAWN -> "p";
            default -> throw new AssertionError(type);
        };
    }

    private static BufferedImage solidImage(int width, int height, Color color) {
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(color);
            graphics.fillRect(0, 0, width, height);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static boolean hasChangedPixel(BufferedImage image, int original) {
        for(int y = 0; y < image.getHeight(); y ++) {
            for(int x = 0; x < image.getWidth(); x ++) {
                if(image.getRGB(x, y) != original) return true;
            }
        }
        return false;
    }

    private static boolean hasVisiblePixel(BufferedImage image) {
        for(int y = 0; y < image.getHeight(); y ++) {
            for(int x = 0; x < image.getWidth(); x ++) {
                if((image.getRGB(x, y) >>> 24) != 0) return true;
            }
        }
        return false;
    }

    private static void assertTransparentBorder(BufferedImage image, String message) {
        for(int x = 0; x < image.getWidth(); x ++) {
            assertEquals(0, image.getRGB(x, 0) >>> 24, message);
            assertEquals(0, image.getRGB(x, image.getHeight() - 1) >>> 24, message);
        }
        for(int y = 0; y < image.getHeight(); y ++) {
            assertEquals(0, image.getRGB(0, y) >>> 24, message);
            assertEquals(0, image.getRGB(image.getWidth() - 1, y) >>> 24, message);
        }
    }

    private static int countVisiblePixelsWithLuminance(
        BufferedImage image, int threshold, boolean atLeast
    ) {
        int count = 0;
        for(int y = 0; y < image.getHeight(); y ++) {
            for(int x = 0; x < image.getWidth(); x ++) {
                final int argb = image.getRGB(x, y);
                if((argb >>> 24) < 200) continue;
                final int red = (argb >>> 16) & 0xff;
                final int green = (argb >>> 8) & 0xff;
                final int blue = argb & 0xff;
                final int luminance = (299 * red + 587 * green + 114 * blue) / 1000;
                if(atLeast ? luminance >= threshold : luminance <= threshold) count ++;
            }
        }
        return count;
    }
}
