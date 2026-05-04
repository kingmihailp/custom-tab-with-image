package com.kingmihailp.customtab.command;

import com.kingmihailp.customtab.CustomTabMod;
import com.kingmihailp.customtab.config.TabConfig;
import com.kingmihailp.customtab.handler.TabListHandler;
import com.kingmihailp.customtab.util.ColorUtil;
import com.kingmihailp.customtab.util.ImageConverter;
import com.kingmihailp.customtab.util.PlaceholderUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * /customtab — admin command tree (requires operator level 2).
 *
 * Sub-commands:
 *   /customtab reload
 *   /customtab enable / disable
 *   /customtab header set <text>       — replace all lines with one line
 *   /customtab header add <text>       — append a new line
 *   /customtab header clear            — remove all lines
 *   /customtab header list             — show current lines
 *   /customtab footer set <text>
 *   /customtab footer add <text>
 *   /customtab footer clear
 *   /customtab footer list
 *   /customtab image set/enable/disable/resize/info
 *   /customtab preview
 */
public final class TabCommand {

    private static TabListHandler handlerRef;

    private TabCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("customtab")
                .requires(src -> src.hasPermission(2))

                .then(Commands.literal("reload")
                    .executes(ctx -> reload(ctx.getSource())))

                .then(Commands.literal("enable")
                    .executes(ctx -> setEnabled(ctx.getSource(), true)))
                .then(Commands.literal("disable")
                    .executes(ctx -> setEnabled(ctx.getSource(), false)))

                // --- header ---
                .then(Commands.literal("header")
                    .then(Commands.literal("set")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                            .executes(ctx -> headerSet(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "text")))))
                    .then(Commands.literal("add")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                            .executes(ctx -> headerAdd(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "text")))))
                    .then(Commands.literal("clear")
                        .executes(ctx -> headerClear(ctx.getSource())))
                    .then(Commands.literal("list")
                        .executes(ctx -> headerList(ctx.getSource()))))

                // --- footer ---
                .then(Commands.literal("footer")
                    .then(Commands.literal("set")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                            .executes(ctx -> footerSet(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "text")))))
                    .then(Commands.literal("add")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                            .executes(ctx -> footerAdd(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "text")))))
                    .then(Commands.literal("clear")
                        .executes(ctx -> footerClear(ctx.getSource())))
                    .then(Commands.literal("list")
                        .executes(ctx -> footerList(ctx.getSource()))))

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
    // General
    // ------------------------------------------------------------------

    private static int reload(CommandSourceStack src) {
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

    // ------------------------------------------------------------------
    // Header
    // ------------------------------------------------------------------

    private static int headerSet(CommandSourceStack src, String text) {
        TabConfig.HEADER_LINES.set(List.of(text));
        TabConfig.SPEC.save();
        src.sendSuccess(() -> Component.literal("§aHeader set to: ").append(ColorUtil.parse(text)), true);
        return 1;
    }

    private static int headerAdd(CommandSourceStack src, String text) {
        List<String> lines = new ArrayList<>(TabConfig.HEADER_LINES.get());
        lines.add(text);
        TabConfig.HEADER_LINES.set(lines);
        TabConfig.SPEC.save();
        src.sendSuccess(() -> Component.literal(
                "§aAdded header line " + lines.size() + ": ").append(ColorUtil.parse(text)), true);
        return 1;
    }

    private static int headerClear(CommandSourceStack src) {
        TabConfig.HEADER_LINES.set(List.of());
        TabConfig.SPEC.save();
        src.sendSuccess(() -> Component.literal("§aHeader cleared."), true);
        return 1;
    }

    private static int headerList(CommandSourceStack src) {
        List<? extends String> lines = TabConfig.HEADER_LINES.get();
        if (lines.isEmpty()) {
            src.sendSuccess(() -> Component.literal("§7Header is empty."), false);
            return 1;
        }
        StringBuilder sb = new StringBuilder("§6Header lines:\n");
        for (int i = 0; i < lines.size(); i++) {
            sb.append("§7  [").append(i + 1).append("] §f").append(lines.get(i)).append('\n');
        }
        String msg = sb.toString().stripTrailing();
        src.sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    // ------------------------------------------------------------------
    // Footer
    // ------------------------------------------------------------------

    private static int footerSet(CommandSourceStack src, String text) {
        TabConfig.FOOTER_LINES.set(List.of(text));
        TabConfig.SPEC.save();
        src.sendSuccess(() -> Component.literal("§aFooter set to: ").append(ColorUtil.parse(text)), true);
        return 1;
    }

    private static int footerAdd(CommandSourceStack src, String text) {
        List<String> lines = new ArrayList<>(TabConfig.FOOTER_LINES.get());
        lines.add(text);
        TabConfig.FOOTER_LINES.set(lines);
        TabConfig.SPEC.save();
        src.sendSuccess(() -> Component.literal(
                "§aAdded footer line " + lines.size() + ": ").append(ColorUtil.parse(text)), true);
        return 1;
    }

    private static int footerClear(CommandSourceStack src) {
        TabConfig.FOOTER_LINES.set(List.of());
        TabConfig.SPEC.save();
        src.sendSuccess(() -> Component.literal("§aFooter cleared."), true);
        return 1;
    }

    private static int footerList(CommandSourceStack src) {
        List<? extends String> lines = TabConfig.FOOTER_LINES.get();
        if (lines.isEmpty()) {
            src.sendSuccess(() -> Component.literal("§7Footer is empty."), false);
            return 1;
        }
        StringBuilder sb = new StringBuilder("§6Footer lines:\n");
        for (int i = 0; i < lines.size(); i++) {
            sb.append("§7  [").append(i + 1).append("] §f").append(lines.get(i)).append('\n');
        }
        String msg = sb.toString().stripTrailing();
        src.sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    // ------------------------------------------------------------------
    // Image
    // ------------------------------------------------------------------

    private static int setImagePath(CommandSourceStack src, String path) {
        File file = new File(path);
        if (!file.isAbsolute()) {
            file = new File(src.getServer().getServerDirectory().toFile(), path);
        }
        if (!file.exists()) {
            final File finalFile = file;
            src.sendFailure(Component.literal("§cFile not found: " + finalFile.getAbsolutePath()));
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

    // ------------------------------------------------------------------
    // Preview
    // ------------------------------------------------------------------

    private static int preview(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("§cThis command can only be run by a player."));
            return 0;
        }
        MinecraftServer server = src.getServer();
        String headerRaw = TabConfig.getHeaderText();
        String footerRaw = TabConfig.getFooterText();

        Component header = headerRaw.isBlank() ? Component.empty()
                : ColorUtil.parse(PlaceholderUtil.apply(headerRaw, server, player, 20.0f));
        Component footer = footerRaw.isBlank() ? Component.empty()
                : ColorUtil.parse(PlaceholderUtil.apply(footerRaw, server, player, 20.0f));

        player.connection.send(
                new net.minecraft.network.protocol.game.ClientboundTabListPacket(header, footer));
        src.sendSuccess(() -> Component.literal(
                "§aTab preview sent (text only; image requires a full tick)."), false);
        return 1;
    }

    /** Called from the handler to register itself so commands can invalidate its cache. */
    public static void setHandlerRef(TabListHandler handler) {
        handlerRef = handler;
    }
}
