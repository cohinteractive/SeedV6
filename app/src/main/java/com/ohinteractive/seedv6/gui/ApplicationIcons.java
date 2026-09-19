package com.ohinteractive.seedv6.gui;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

/** Canonical application artwork, decoded once from packaged resources. */
final class ApplicationIcons {

    private static final String ROOT =
            "/com/ohinteractive/seedv6/gui/icons/";
    private static final int[] SIZES =
            {16, 20, 24, 32, 40, 48, 64, 96, 128, 256};
    private static final List<Image> WINDOW_IMAGES = load();

    static List<Image> windowImages() {
        return WINDOW_IMAGES;
    }

    private static List<Image> load() {
        final List<Image> images = new ArrayList<>(SIZES.length);
        for (int size : SIZES) {
            final String path = ROOT + "seedv6-" + size + ".png";
            try (InputStream input =
                    ApplicationIcons.class.getResourceAsStream(path)) {
                if (input == null) {
                    throw new IllegalStateException(
                            "Missing application icon: " + path);
                }
                final BufferedImage image = ImageIO.read(input);
                if (image == null
                        || image.getWidth() != size
                        || image.getHeight() != size) {
                    throw new IllegalStateException(
                            "Invalid application icon: " + path);
                }
                images.add(image);
            } catch (IOException exception) {
                throw new IllegalStateException(
                        "Cannot load application icon: " + path, exception);
            }
        }
        return List.copyOf(images);
    }

    private ApplicationIcons() {}
}