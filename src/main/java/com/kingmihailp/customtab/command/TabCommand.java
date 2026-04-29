package com.kingmihailp.customtab.command;

import com.kingmihailp.customtab.CustomTabMod;
import com.kingmihailp.customtab.config.TabConfig;
import com.kingmihailp.customtab.handler.TabListHandler;
import com.kingmihailp.customtab.util.ColorUtil;
import com.kingmihailp.customtab.util.ImageConverter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.io.File;

/**
 * /customtab — admin command tree (requires operator level 2).
 *
 * Sub-commands:
 *   /customtab reload              — hot-reload config from disk
 *   /customtab enable              — enable the custom tab
 *   /customtab disable             — disable the custom tab
 *   /customtab header <text>       — set header text (saved to config)
 *   /customtab footer <text>       — set footer text (saved to config)
 *   /customtab image set <path>    — point to an image file on the server
 *   /customtab image enable        — enable image display
 *   /customtab image disable       — disable image display
 *   /customtab image resize <w> <h>— set image render dimensions
 *   /customtab image info          — show current image settings
 *   /customtab preview             — send the current header/footer to the caller only
 */
public final class TabCommand {

    private static TabListHandler handlerRef;

    private TabCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("customtab")
                .requires(src -> src.hasPermission(2))

                // --- reload ---
                .then(Commands.literal("reload")
                    .executes(ctx -> reload(ctx.getSource())))

                // --- enable / disable ---
                .then(Commands.literal("enable")
                    .executes(ctx -> setEnabled(ctx.getSource(), true)))
                .then(Commands.literal("disable")
                    .executes(ctx -> setEnabled(ctx.getSource(), false)))

                // --- header ---
                .then(Commands.literal("header")
                    .then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(ctx -> setHeader(ctx.getSource(),
                                StringArgumentType.getString(ctx, "text")))))

                // --- footer ---
                .then(Commands.literal("footer")
                    .then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(ctx -> setFooter(ctx.getSource(),
                                StringArgumentType.getString(ctx, "text")))))

                // --- image ---
                .then(Commands.literal("image")
                    .then(Commands.literal("set")
                        .then(Commands.argument("path", StringArgumentType.greedyString())
                            .executes(ctx -> setImagePath(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "path")))))
                    .then(Commands.literal("enable")
                        .executes(ctx -> setImageEnabled(ctx.getSource(), true)))
                    .then(Commands.literal("disable")
                        .executes(ctx -> setImageEnabled(ctx.getSource(), false)))
                    .then(Commands.literal("resize")
                        .then(Commands.argument("width", IntegerArgumentType.integer(1, 200))
                            .then(Commands.argument("height", IntegerArgumentType.integer(1, 80))
                                .executes(ctx -> resizeImage(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "width"),
                                        IntegerArgumentType.getInteger(ctx, "height"))))))
                    .then(Commands.literal("info")
                        .executes(ctx -> imageInfo(ctx.getSource()))))

                // --- preview ---
                .then(Commands.literal("preview")
                    .executes(ctx -> preview(ctx.getSource())))
        );
    }

    // ------------------------------------------------------------------

    private static int reload(CommandSourceStack src) {
        // NeoForge reloads server configs from disk automatically; we just
        // invalidate our image cache so the new path/dimensions take effect.
        if (handlerRef != null) handlerRef.invalidateImageCache();
        src.sendSuccess(() -> Component.literal("§aCustomTab config reloaded."), true);
        return 1;
    }

    private static int setEnabled(CommandSourceStack src, boolean enabled) {
        TabConfig.ENABLED.set(enabled);
        TabConfig.SPEC.save();
        src.sendSuccess(() -> Component.literal(
                enabled ? "§aCustomTab enabled." : "§cCustomTab disabled."), true);
        return 1;
    }

    private static int setHeader(CommandSourceStack src, String text) {
        TabConfig.HEADER_TEXT.set(text);
        TabConfig.SPEC.save();
        src.sendSuccess(() -> Component.literal("§aHeader updated. Preview: ")
                .append(ColorUtil.parse(text)), true);
        return 1;
    }

    private static int setFooter(CommandSourceStack src, String text) {
        TabConfig.FOOTER_TEXT.set(text);
        TabConfig.SPEC.save();
        src.sendSuccess(() -> Component.literal("§aFooter updated. Preview: ")
                .append(ColorUtil.parse(text)), true);
        return 1;
    }

    private static int setImagePath(CommandSourceStack src, String path) {
        // Validate that the file exists
        File file = new File(path);
        if (!file.isAbsolute()) {
            file = new File(src.getServer().getServerDirectory().toFile(), path);
        }
        if (!file.exists()) {
            final File finalFile = file;
            src.sendFailure(Component.literal(
                    "§cFile not found: " + finalFile.getAbsolutePath()));
            return 0;
        }
        TabConfig.IMAGE_PATH.set(path);
        TabConfig.SPEC.save();
        if (handlerRef != null) handlerRef.invalidateImageCache();
        src.sendSuccess(() -> Component.literal("§aImage path set to: §f" + path), true);
        return 1;
    }

    private static int setImageEnabled(CommandSourceStack src, boolean enabled) {
        TabConfig.IMAGE_ENABLED.set(enabled);
        TabConfig.SPEC.save();
        src.sendSuccess(() -> Component.literal(
                enabled ? "§aImage display enabled." : "§cImage display disabled."), true);
        return 1;
    }

    private static int resizeImage(CommandSourceStack src, int w, int h) {
        TabConfig.IMAGE_WIDTH.set(w);
        TabConfig.IMAGE_HEIGHT.set(h);
        TabConfig.SPEC.save();
        if (handlerRef != null) handlerRef.invalidateImageCache();
        src.sendSuccess(() -> Component.literal(
                "§aImage size set to " + ImageConverter.dimensionInfo(w, h) + "."), true);
        return 1;
    }

    private static int imageInfo(CommandSourceStack src) {
        boolean enabled = TabConfig.IMAGE_ENABLED.get();
        String  path    = TabConfig.IMAGE_PATH.get();
        int     w       = TabConfig.IMAGE_WIDTH.get();
        int     h       = TabConfig.IMAGE_HEIGHT.get();
        src.sendSuccess(() -> Component.literal(
                "§6Image settings:\n" +
                "§7  Enabled: " + (enabled ? "§atrue" : "§cfalse") + "\n" +
                "§7  Path: §f" + path + "\n" +
                "§7  Size: §f" + ImageConverter.dimensionInfo(w, h)), false);
        return 1;
    }

    private static int preview(CommandSourceStack src) {
        if (!(src.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) {
            src.sendFailure(Component.literal("§cThis command can only be run by a player."));
            return 0;
        }
        MinecraftServer server = src.getServer();
        Component header = buildHeaderForServer(server);
        Component footer = buildFooterForServer(server);
        player.connection.send(
                new net.minecraft.network.protocol.game.ClientboundTabListPacket(header, footer));
        src.sendSuccess(() -> Component.literal("§aTab preview sent (text only, image requires a full tick)."), false);
        return 1;
    }

    private static Component buildHeaderForServer(MinecraftServer server) {
        String raw = TabConfig.HEADER_TEXT.get();
        if (raw == null || raw.isBlank()) return Component.empty();
        return ColorUtil.parse(
                com.kingmihailp.customtab.util.PlaceholderUtil.apply(raw, server));
    }

    private static Component buildFooterForServer(MinecraftServer server) {
        String raw = TabConfig.FOOTER_TEXT.get();
        if (raw == null || raw.isBlank()) return Component.empty();
        return ColorUtil.parse(
                com.kingmihailp.customtab.util.PlaceholderUtil.apply(raw, server));
    }

    /** Called from the handler to register itself so commands can invalidate its cache. */
    public static void setHandlerRef(TabListHandler handler) {
        handlerRef = handler;
    }
}
