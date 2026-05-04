package com.kingmihailp.customtab.handler;

import com.kingmihailp.customtab.CustomTabMod;
import com.kingmihailp.customtab.config.TabConfig;
import com.kingmihailp.customtab.util.ColorUtil;
import com.kingmihailp.customtab.util.ImageConverter;
import com.kingmihailp.customtab.util.PlaceholderUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.io.File;

public class TabListHandler {

    private MinecraftServer server;
    private int tickCounter = 0;

    // TPS tracking — ring buffer of the last 20 tick durations (nanoseconds)
    private static final int TPS_SAMPLES = 20;
    private final long[] tickNanos = new long[TPS_SAMPLES];
    private int tpsIdx = 0;
    private long lastTickNano = System.nanoTime();
    private float currentTps = 20.0f;

    // Cached image component — rebuilt only when the image path/dimensions change
    private String    cachedImagePath = null;
    private int       cachedImageW    = -1;
    private int       cachedImageH    = -1;
    private Component cachedImage     = null;

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        server = event.getServer();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        server = null;
        cachedImage = null;
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        // --- TPS measurement ---
        long now = System.nanoTime();
        tickNanos[tpsIdx % TPS_SAMPLES] = now - lastTickNano;
        lastTickNano = now;
        tpsIdx++;

        if (tpsIdx >= TPS_SAMPLES) {
            long sum = 0;
            for (long t : tickNanos) sum += t;
            long avgNs = sum / TPS_SAMPLES;
            currentTps = avgNs > 0
                    ? Math.min(20.0f, (float) (1_000_000_000.0 / avgNs))
                    : 20.0f;
        }

        // --- Tab update ---
        if (server == null) return;
        if (!TabConfig.ENABLED.get()) return;

        tickCounter++;
        if (tickCounter < TabConfig.UPDATE_INTERVAL.get()) return;
        tickCounter = 0;

        Component image = getOrBuildImage();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Component header = buildHeader(player, image);
            Component footer = buildFooter(player);
            player.connection.send(
                    new net.minecraft.network.protocol.game.ClientboundTabListPacket(header, footer));
        }
    }

    /** Send updated header/footer immediately when a player joins. */
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (server == null) return;
        if (!TabConfig.ENABLED.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        Component image  = getOrBuildImage();
        Component header = buildHeader(player, image);
        Component footer = buildFooter(player);
        player.connection.send(
                new net.minecraft.network.protocol.game.ClientboundTabListPacket(header, footer));
    }

    // ------------------------------------------------------------------

    /** Force the image cache to be rebuilt on next tick (called after /customtab reload). */
    public void invalidateImageCache() {
        cachedImagePath = null;
    }

    private Component buildHeader(ServerPlayer player, Component image) {
        MutableComponent header = Component.empty();

        String headerRaw = TabConfig.getHeaderText();

        if (image != null && TabConfig.IMAGE_ABOVE_HEADER.get()) {
            header.append(image);
            if (!headerRaw.isBlank()) header.append(Component.literal("\n"));
        }

        if (!headerRaw.isBlank()) {
            String resolved = PlaceholderUtil.apply(headerRaw, server, player, currentTps);
            header.append(ColorUtil.parse(resolved));
        }

        if (image != null && !TabConfig.IMAGE_ABOVE_HEADER.get()) {
            if (!headerRaw.isBlank()) header.append(Component.literal("\n"));
            header.append(image);
        }

        return header;
    }

    private Component buildFooter(ServerPlayer player) {
        String raw = TabConfig.getFooterText();
        if (raw == null || raw.isBlank()) return Component.empty();
        String resolved = PlaceholderUtil.apply(raw, server, player, currentTps);
        return ColorUtil.parse(resolved);
    }

    private Component getOrBuildImage() {
        if (!TabConfig.IMAGE_ENABLED.get()) return null;

        String path = TabConfig.IMAGE_PATH.get();
        int    w    = TabConfig.IMAGE_WIDTH.get();
        int    h    = TabConfig.IMAGE_HEIGHT.get();

        if (path.equals(cachedImagePath) && w == cachedImageW && h == cachedImageH) {
            return cachedImage;
        }

        File file = new File(path);
        if (!file.isAbsolute()) file = new File(server.getServerDirectory().toFile(), path);

        if (!file.exists()) {
            CustomTabMod.LOGGER.warn("Image not found: {}", file.getAbsolutePath());
            cachedImage = null;
        } else {
            cachedImage = ImageConverter.convert(file, w, h);
        }

        cachedImagePath = path;
        cachedImageW    = w;
        cachedImageH    = h;
        return cachedImage;
    }
}
