package com.kingmihailp.customtab.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;

import java.awt.Color;

/**
 * Converts strings with &-codes, &#RRGGBB hex codes, and [font:NAME] tags
 * into styled Components.
 *
 * Colour / formatting:
 *   &#RRGGBB        — 24-bit hex colour
 *   &0-9 a-f        — legacy colour codes
 *   &k l m n o r   — legacy formatting codes (obfuscated / bold / strike / under / italic / reset)
 *
 * Font tags (case-insensitive, apply to all following text until next [font:…]):
 *   [font:default]   — standard Minecraft font  (reset)
 *   [font:uniform]   — uniform-width font        (cleaner, monospaced feel)
 *   [font:alt]       — Standard Galactic Alphabet (enchantment-table symbols)
 *   [font:illageralt] — Illager rune font         (woodland mansion symbols)
 *   [font:namespace:path] — any resource-pack font by full resource location
 *
 * Other:
 *   \n — newline
 */
public final class ColorUtil {

    private static final ResourceLocation FONT_DEFAULT    = ResourceLocation.fromNamespaceAndPath("minecraft", "default");
    private static final ResourceLocation FONT_UNIFORM    = ResourceLocation.fromNamespaceAndPath("minecraft", "uniform");
    private static final ResourceLocation FONT_ALT        = ResourceLocation.fromNamespaceAndPath("minecraft", "alt");
    private static final ResourceLocation FONT_ILLAGERALT = ResourceLocation.fromNamespaceAndPath("minecraft", "illageralt");

    private static final String FONT_TAG_OPEN = "[font:";

    private ColorUtil() {}

    /** Parse a raw config string into a fully styled Component. */
    public static Component parse(String raw) {
        String text = raw.replace("\\n", "\n");

        MutableComponent root = Component.empty();
        Style currentStyle = Style.EMPTY;

        int i = 0;
        StringBuilder plain = new StringBuilder();

        while (i < text.length()) {

            // ── [font:NAME] ────────────────────────────────────────────────
            if (text.regionMatches(true, i, FONT_TAG_OPEN, 0, FONT_TAG_OPEN.length())) {
                int end = text.indexOf(']', i + FONT_TAG_OPEN.length());
                if (end > i + FONT_TAG_OPEN.length()) {
                    String fontName = text.substring(i + FONT_TAG_OPEN.length(), end);
                    flushPlain(root, plain, currentStyle);
                    plain = new StringBuilder();
                    currentStyle = currentStyle.withFont(resolveFont(fontName));
                    i = end + 1;
                    continue;
                }
            }

            // ── &#RRGGBB ───────────────────────────────────────────────────
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

            // ── &code ──────────────────────────────────────────────────────
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

    // ------------------------------------------------------------------

    /**
     * Maps a font name string to a ResourceLocation.
     * Accepts short aliases or a full "namespace:path" resource location.
     */
    private static ResourceLocation resolveFont(String name) {
        return switch (name.toLowerCase()) {
            case "default"              -> FONT_DEFAULT;
            case "uniform"              -> FONT_UNIFORM;
            case "alt", "galactic",
                 "enchanting",
                 "enchantment"          -> FONT_ALT;
            case "illageralt", "illager",
                 "rune", "runes"        -> FONT_ILLAGERALT;
            default -> {
                // Allow fully-qualified "namespace:path" for resource-pack fonts
                if (name.contains(":")) {
                    try {
                        yield ResourceLocation.parse(name);
                    } catch (Exception e) {
                        yield FONT_DEFAULT;
                    }
                }
                // Fall back: treat as a minecraft sub-font
                yield ResourceLocation.fromNamespaceAndPath("minecraft", name.toLowerCase());
            }
        };
    }

    private static void flushPlain(MutableComponent root, StringBuilder plain, Style style) {
        if (plain.length() == 0) return;
        root.append(Component.literal(plain.toString()).withStyle(style));
    }
}
