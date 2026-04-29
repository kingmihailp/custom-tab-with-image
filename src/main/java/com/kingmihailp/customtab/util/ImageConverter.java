package com.kingmihailp.customtab.util;

import com.kingmihailp.customtab.CustomTabMod;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts a raster image into a Minecraft text Component by mapping each
 * scaled pixel to a full-block Unicode character (█ U+2588) with the nearest
 * 24-bit RGB colour applied as a hex text color.
 *
 * Transparent pixels (alpha < 128) are rendered as a plain space so the
 * background shows through.
 */
public final class ImageConverter {

    /** The "full block" character used for every opaque pixel. */
    private static final String BLOCK = "█";
    private static final String SPACE = " ";

    private ImageConverter() {}

    /**
     * Load {@code imageFile}, scale it to {@code width × height} characters and
     * convert to a multi-line Component.  Returns {@code null} when the file
     * cannot be read.
     */
    public static Component convert(File imageFile, int width, int height) {
        BufferedImage src;
        try {
            src = ImageIO.read(imageFile);
        } catch (IOException e) {
            CustomTabMod.LOGGER.error("Failed to read image {}: {}", imageFile.getPath(), e.getMessage());
            return null;
        }
        if (src == null) {
            CustomTabMod.LOGGER.error("Unsupported image format: {}", imageFile.getPath());
            return null;
        }

        // Scale to target dimensions
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, width, height, null);
        g.dispose();

        // Build component row by row
        MutableComponent root = Component.empty();
        for (int row = 0; row < height; row++) {
            if (row > 0) root.append(Component.literal("\n"));
            root.append(buildRow(scaled, row, width));
        }
        return root;
    }

    private static Component buildRow(BufferedImage img, int row, int width) {
        MutableComponent line = Component.empty();
        TextColor lastColor = null;
        StringBuilder run = new StringBuilder();

        for (int col = 0; col < width; col++) {
            int argb = img.getRGB(col, row);
            int alpha = (argb >>> 24) & 0xFF;

            if (alpha < 128) {
                // Transparent — flush current run then add a space with no colour
                if (run.length() > 0) {
                    line.append(Component.literal(run.toString())
                            .withStyle(Style.EMPTY.withColor(lastColor)));
                    run.setLength(0);
                    lastColor = null;
                }
                line.append(Component.literal(SPACE));
                continue;
            }

            int rgb = argb & 0x00FFFFFF;
            TextColor color = TextColor.fromRgb(rgb);

            if (!color.equals(lastColor)) {
                if (run.length() > 0) {
                    line.append(Component.literal(run.toString())
                            .withStyle(Style.EMPTY.withColor(lastColor)));
                    run.setLength(0);
                }
                lastColor = color;
            }
            run.append(BLOCK);
        }

        if (run.length() > 0) {
            line.append(Component.literal(run.toString())
                    .withStyle(Style.EMPTY.withColor(lastColor)));
        }
        return line;
    }

    /**
     * Returns a human-readable description of the image dimensions after scaling,
     * useful for admin feedback messages.
     */
    public static String dimensionInfo(int width, int height) {
        return width + "×" + height + " characters";
    }
}
