package com.kingmihailp.customtab.client;

import com.kingmihailp.customtab.config.TabClientConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.io.File;

/**
 * Client-only commands — no operator permission required, affect only this player.
 *
 *   /ctclient image set <path>         — set image path in client config
 *   /ctclient image enable             — enable client image
 *   /ctclient image disable            — disable client image
 *   /ctclient image size <w> <h>       — set display size (pixels)
 *   /ctclient image offset <x> <y>     — set position offset
 *   /ctclient image alpha <0.0-1.0>    — set opacity
 *   /ctclient image reload             — force texture reload
 *   /ctclient image info               — show current settings
 */
@EventBusSubscriber(modid = com.kingmihailp.customtab.CustomTabMod.MODID,
        bus = EventBusSubscriber.Bus.GAME,
        value = Dist.CLIENT)
public final class ClientTabCommand {

    private ClientTabCommand() {}

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("ctclient")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("image")
                    .then(Commands.literal("set")
                        .then(Commands.argument("path", StringArgumentType.greedyString())
                            .executes(ctx -> setPath(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "path")))))
                    .then(Commands.literal("enable")
                        .executes(ctx -> setEnabled(ctx.getSource(), true)))
                    .then(Commands.literal("disable")
                        .executes(ctx -> setEnabled(ctx.getSource(), false)))
                    .then(Commands.literal("size")
                        .then(Commands.argument("width", IntegerArgumentType.integer(1, 1920))
                            .then(Commands.argument("height", IntegerArgumentType.integer(1, 1080))
                                .executes(ctx -> setSize(
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "width"),
                                        IntegerArgumentType.getInteger(ctx, "height"))))))
                    .then(Commands.literal("offset")
                        .then(Commands.argument("x", IntegerArgumentType.integer(-2000, 2000))
                            .then(Commands.argument("y", IntegerArgumentType.integer(0, 1080))
                                .executes(ctx -> setOffset(
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "x"),
                                        IntegerArgumentType.getInteger(ctx, "y"))))))
                    .then(Commands.literal("alpha")
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 1.0))
                            .executes(ctx -> setAlpha(ctx.getSource(),
                                    DoubleArgumentType.getDouble(ctx, "value")))))
                    .then(Commands.literal("reload")
                        .executes(ctx -> reload(ctx.getSource())))
                    .then(Commands.literal("info")
                        .executes(ctx -> info(ctx.getSource()))))
        );
    }

    // ------------------------------------------------------------------

    private static int setPath(CommandSourceStack src, String path) {
        File file = new File(path);
        if (!file.isAbsolute()) {
            file = new File(Minecraft.getInstance().gameDirectory, path);
        }
        if (!file.exists()) {
            final File f = file;
            src.sendFailure(Component.literal("§cFile not found: " + f.getAbsolutePath()));
            return 0;
        }
        TabClientConfig.IMAGE_PATH.set(path);
        TabClientConfig.SPEC.save();
        ClientTabImageRenderer.invalidate();
        src.sendSuccess(() -> Component.literal("§aClient image path set to: §f" + path), false);
        return 1;
    }

    private static int setEnabled(CommandSourceStack src, boolean enabled) {
        TabClientConfig.IMAGE_ENABLED.set(enabled);
        TabClientConfig.SPEC.save();
        src.sendSuccess(() -> Component.literal(
                enabled ? "§aClient image enabled." : "§cClient image disabled."), false);
        return 1;
    }

    private static int setSize(CommandSourceStack src, int w, int h) {
        TabClientConfig.IMAGE_DISPLAY_WIDTH.set(w);
        TabClientConfig.IMAGE_DISPLAY_HEIGHT.set(h);
        TabClientConfig.SPEC.save();
        src.sendSuccess(() -> Component.literal("§aClient image size set to §f" + w + "×" + h + "§a px."), false);
        return 1;
    }

    private static int setOffset(CommandSourceStack src, int x, int y) {
        TabClientConfig.IMAGE_X_OFFSET.set(x);
        TabClientConfig.IMAGE_Y_OFFSET.set(y);
        TabClientConfig.SPEC.save();
        src.sendSuccess(() -> Component.literal("§aClient image offset set to §f" + x + ", " + y + "§a."), false);
        return 1;
    }

    private static int setAlpha(CommandSourceStack src, double alpha) {
        TabClientConfig.IMAGE_ALPHA.set(alpha);
        TabClientConfig.SPEC.save();
        src.sendSuccess(() -> Component.literal("§aClient image alpha set to §f" + alpha + "§a."), false);
        return 1;
    }

    private static int reload(CommandSourceStack src) {
        ClientTabImageRenderer.invalidate();
        src.sendSuccess(() -> Component.literal("§aClient image texture reloaded."), false);
        return 1;
    }

    private static int info(CommandSourceStack src) {
        boolean enabled = TabClientConfig.IMAGE_ENABLED.get();
        String  path    = TabClientConfig.IMAGE_PATH.get();
        int     w       = TabClientConfig.IMAGE_DISPLAY_WIDTH.get();
        int     h       = TabClientConfig.IMAGE_DISPLAY_HEIGHT.get();
        int     xOff    = TabClientConfig.IMAGE_X_OFFSET.get();
        int     yOff    = TabClientConfig.IMAGE_Y_OFFSET.get();
        double  alpha   = TabClientConfig.IMAGE_ALPHA.get();
        src.sendSuccess(() -> Component.literal(
                "§6Client image settings:\n" +
                "§7  Enabled: "     + (enabled ? "§atrue" : "§cfalse") + "\n" +
                "§7  Path: §f"      + path + "\n" +
                "§7  Size: §f"      + w + "×" + h + " px\n" +
                "§7  Offset: §f"    + xOff + ", " + yOff + "\n" +
                "§7  Alpha: §f"     + alpha), false);
        return 1;
    }
}
