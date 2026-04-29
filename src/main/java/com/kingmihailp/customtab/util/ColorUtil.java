package com.kingmihailp.customtab.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts strings with &-codes and &#RRGGBB hex codes into Adventure Components.
 *
 * Supported syntax:
 *   &#RRGGBB  — 24-bit hex colour
 *   &0-9a-f  — legacy colour codes
 *   &k l m n o r — legacy formatting codes
 *   \n — newline
 */
public final class ColorUtil {

    private static final Pattern HEX_PATTERN   = Pattern.compile("&#([0-9A-Fa-f]{6})");
    private static final Pattern AMP_PATTERN   = Pattern.compile("&([0-9a-fk-orA-FK-OR])");

    private ColorUtil() {}

    /** Parse a raw config string into a fully styled Component. */
    public static Component parse(String raw) {
        // Replace literal \n with real newlines first
        String text = raw.replace("\\n", "\n");

        MutableComponent root = Component.empty();
        Style currentStyle = Style.EMPTY;

        // We iterate token by token: either a colour/format code or plain text.
        int i = 0;
        StringBuilder plain = new StringBuilder();

        while (i < text.length()) {
            // Check for hex colour &#RRGGBB
            if (i + 7 < text.length() && text.charAt(i) == '&' && text.charAt(i + 1) == '#') {
                String hex = text.substring(i + 2, i + 8);
                if (hex.matches("[0-9A-Fa-f]{6}")) {
                    flushPlain(root, plain, currentStyle);
                    plain = new StringBuilder();
                    Color c = Color.decode("#" + hex);
                    currentStyle = currentStyle.withColor(
                            net.minecraft.network.chat.TextColor.fromRgb(c.getRGB() & 0xFFFFFF));
                    i += 8;
                    continue;
                }
            }

            // Check for legacy & code
            if (i + 1 < text.length() && text.charAt(i) == '&') {
                char code = text.charAt(i + 1);
                ChatFormatting fmt = ChatFormatting.getByCode(code);
                if (fmt != null) {
                    flushPlain(root, plain, currentStyle);
                    plain = new StringBuilder();
                    if (fmt == ChatFormatting.RESET) {
                        currentStyle = Style.EMPTY;
                    } else if (fmt.isColor()) {
                        currentStyle = Style.EMPTY.applyFormat(fmt);
                    } else {
                        currentStyle = currentStyle.applyFormat(fmt);
                    }
                    i += 2;
                    continue;
                }
            }

            plain.append(text.charAt(i));
            i++;
        }
        flushPlain(root, plain, currentStyle);
        return root;
    }

    private static void flushPlain(MutableComponent root, StringBuilder plain, Style style) {
        if (plain.length() == 0) return;
        root.append(Component.literal(plain.toString()).withStyle(style));
    }
}
