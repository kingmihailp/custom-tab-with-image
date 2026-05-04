package com.kingmihailp.customtab.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class TabConfig {

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;

    // Header / footer — each entry is one line; joined with \n when sent
    public static final ModConfigSpec.ConfigValue<List<? extends String>> HEADER_LINES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> FOOTER_LINES;

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
        HEADER_LINES = builder
                .comment(
                        "Tab header lines. Each list entry is one line.",
                        "Formatting codes:  &a &b &c … &r &l &o &n &m &k",
                        "Hex colors:        &#RRGGBB  (e.g. &#FF5500)",
                        "Global placeholders:",
                        "  {online}       — players online",
                        "  {max_players}  — server player limit",
                        "  {server_name}  — server name",
                        "  {time}         — HH:mm:ss",
                        "  {date}         — yyyy-MM-dd",
                        "  {tps}          — server TPS",
                        "  {cpu}          — JVM CPU load (e.g. 42.3%)",
                        "  {ram_used}     — used heap MB",
                        "  {ram_max}      — max heap MB",
                        "Per-player placeholders (unique per recipient):",
                        "  {player}       — player's in-game name",
                        "  {ping}         — player's ping in ms",
                        "Leave the list empty ([]) to disable the header."
                )
                .defineList("lines",
                        List.of(
                                "&lWelcome to &#FF5500{server_name}&r",
                                "&7Players: &a{online}&7/&c{max_players} &8| &7TPS: &a{tps} &8| &7{time}"
                        ),
                        o -> o instanceof String);
        IMAGE_ABOVE_HEADER = builder
                .comment("If true, the image is placed ABOVE the header text; otherwise BELOW it.")
                .define("image_above_header", true);
        builder.pop();

        builder.push("footer");
        FOOTER_LINES = builder
                .comment(
                        "Tab footer lines. Same placeholders and formatting as the header.",
                        "Leave the list empty ([]) to disable the footer."
                )
                .defineList("lines",
                        List.of(
                                "&7Hello, &e{player}&7! Your ping: &a{ping} ms",
                                "&7CPU: &b{cpu} &8| &7RAM: &b{ram_used}&7/&b{ram_max} &7MB"
                        ),
                        o -> o instanceof String);
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

    /** Joins header lines with newlines, returning empty string if the list is empty. */
    public static String getHeaderText() {
        return String.join("\n", HEADER_LINES.get());
    }

    /** Joins footer lines with newlines, returning empty string if the list is empty. */
    public static String getFooterText() {
        return String.join("\n", FOOTER_LINES.get());
    }
}
