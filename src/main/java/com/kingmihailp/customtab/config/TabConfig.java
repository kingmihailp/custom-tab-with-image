package com.kingmihailp.customtab.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class TabConfig {

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;

    // Header / footer text
    public static final ModConfigSpec.ConfigValue<String> HEADER_TEXT;
    public static final ModConfigSpec.ConfigValue<String> FOOTER_TEXT;

    // Image settings
    public static final ModConfigSpec.BooleanValue IMAGE_ENABLED;
    public static final ModConfigSpec.ConfigValue<String> IMAGE_PATH;
    public static final ModConfigSpec.IntValue IMAGE_WIDTH;
    public static final ModConfigSpec.IntValue IMAGE_HEIGHT;
    public static final ModConfigSpec.BooleanValue IMAGE_ABOVE_HEADER;

    // Update interval in ticks (20 = 1 second)
    public static final ModConfigSpec.IntValue UPDATE_INTERVAL;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("CustomTab — tab list customization").push("general");
        ENABLED = builder
                .comment("Enable or disable the custom tab list entirely.")
                .define("enabled", true);
        UPDATE_INTERVAL = builder
                .comment("How often to update the tab header/footer, in ticks (20 = 1 second).")
                .defineInRange("update_interval", 40, 1, 1200);
        builder.pop();

        builder.push("header");
        HEADER_TEXT = builder
                .comment(
                        "Tab header text. Supports:",
                        "  • Legacy color codes: &a, &b, &c … &r, &l, &o, &n, &m, &k",
                        "  • Hex colors:         &#RRGGBB  (e.g. &#FF5500)",
                        "  • Newlines:           \\n",
                        "  • Placeholders:       {online}, {max_players}, {server_name}, {time}",
                        "Leave empty to disable the header."
                )
                .define("text", "&lWelcome to &#FF5500{server_name}&r\\n&7Players online: &a{online}&7/&c{max_players}");
        IMAGE_ABOVE_HEADER = builder
                .comment("If true, the image is placed ABOVE the header text; otherwise BELOW it.")
                .define("image_above_header", true);
        builder.pop();

        builder.push("footer");
        FOOTER_TEXT = builder
                .comment(
                        "Tab footer text. Same formatting rules as the header.",
                        "Leave empty to disable the footer."
                )
                .define("text", "&7Server time: &e{time}");
        builder.pop();

        builder.push("image");
        IMAGE_ENABLED = builder
                .comment("Enable the image in the tab header.")
                .define("enabled", false);
        IMAGE_PATH = builder
                .comment(
                        "Path to the image file (PNG/JPG/GIF).",
                        "Relative paths are resolved from the server root directory.",
                        "Example: config/customtab/header.png"
                )
                .define("path", "config/customtab/header.png");
        IMAGE_WIDTH = builder
                .comment("Width of the rendered image in characters. Each character ≈ 1 pixel column.")
                .defineInRange("width", 60, 1, 200);
        IMAGE_HEIGHT = builder
                .comment("Height of the rendered image in character rows.")
                .defineInRange("height", 10, 1, 80);
        builder.pop();

        SPEC = builder.build();
    }
}
