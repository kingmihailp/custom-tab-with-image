package com.kingmihailp.customtab.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client-side config — stored in .minecraft/config/customtab-client.toml.
 * Each player configures this independently; no server involvement.
 */
public class TabClientConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue IMAGE_ENABLED;
    public static final ModConfigSpec.ConfigValue<String> IMAGE_PATH;

    /** Display width in pixels on screen (not texture pixels). */
    public static final ModConfigSpec.IntValue IMAGE_DISPLAY_WIDTH;
    /** Display height in pixels on screen. */
    public static final ModConfigSpec.IntValue IMAGE_DISPLAY_HEIGHT;

    /** Horizontal offset from the screen center (negative = left, positive = right). */
    public static final ModConfigSpec.IntValue IMAGE_X_OFFSET;
    /** Vertical offset from the top of the screen (pixels from y=0). */
    public static final ModConfigSpec.IntValue IMAGE_Y_OFFSET;

    /** Image opacity, 0.0 (transparent) to 1.0 (opaque). */
    public static final ModConfigSpec.DoubleValue IMAGE_ALPHA;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("CustomTab client — image settings for the local player").push("image");

        IMAGE_ENABLED = builder
                .comment("Show the local image overlay in the tab list.")
                .define("enabled", false);

        IMAGE_PATH = builder
                .comment(
                        "Path to the image file (PNG / JPG / GIF — first frame for GIF).",
                        "Relative paths are resolved from the game directory (.minecraft/).",
                        "Example:  config/customtab/header.png"
                )
                .define("path", "config/customtab/header.png");

        IMAGE_DISPLAY_WIDTH = builder
                .comment("Width of the rendered image in screen pixels.")
                .defineInRange("display_width", 200, 1, 1920);

        IMAGE_DISPLAY_HEIGHT = builder
                .comment("Height of the rendered image in screen pixels.")
                .defineInRange("display_height", 40, 1, 1080);

        IMAGE_X_OFFSET = builder
                .comment("Horizontal offset from screen center (pixels). Negative = left.")
                .defineInRange("x_offset", 0, -2000, 2000);

        IMAGE_Y_OFFSET = builder
                .comment("Vertical offset from the top of the screen (pixels).")
                .defineInRange("y_offset", 2, 0, 1080);

        IMAGE_ALPHA = builder
                .comment("Image opacity: 0.0 = invisible, 1.0 = fully opaque.")
                .defineInRange("alpha", 1.0, 0.0, 1.0);

        builder.pop();
        SPEC = builder.build();
    }
}
